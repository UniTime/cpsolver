package org.cpsolver.ifs.dbt;

import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.extension.MacPropagation;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.solver.SolverListener;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Maintenance of arc consistency in dynamic backtracking. <br>
 * <br>
 * The difference between {@link MacPropagation} and this DBT propagation is
 * that all not-assigned values of an assigned variable are marked as nogood.
 * Also, when a dead end is reached, unassignment or failure takes place. <br>
 * <br>
 * This IFS solver extension is to be used only in case of dynamic backtracking
 * and it has no parameters.
 * 
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          <a href="http://muller.unitime.org">http://muller.unitime.org</a><br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 3 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not see <a href='http://www.gnu.org/licenses'>http://www.gnu.org/licenses</a>.
 * @param <V> Variable
 * @param <T> Value
 * 
 */
public class DbtPropagation<V extends Variable<V, T>, T extends Value<V, T>> extends MacPropagation<V, T> implements SolverListener<V, T> {
    private static org.apache.logging.log4j.Logger sLogger = org.apache.logging.log4j.LogManager.getLogger(DbtPropagation.class);

    /**
     * Constructor. No parameter is taken from properties.
     * @param solver current solver
     * @param properties solver configuration
     */
    public DbtPropagation(Solver<V, T> solver, DataProperties properties) {
        super(solver, properties);
        solver.addSolverListener(this);
    }

    /**
     * Propagation takes place every time a value is assigned to a variable. <br>
     * <br>
     * <ul>
     * <li>Prints a warning if the value is nogood (should not never happen),
     * <li>sets all other values of the variable to nogood (explanation is the assigned value itself),
     * <li>runs propagation.
     * </ul>
     * 
     * @see MacPropagation#propagate(Assignment, Variable)
     */
    @Override
    public void afterAssigned(Assignment<V, T> assignment, long iteration, T value) {
        iIteration = iteration;
        if (!isGood(assignment, value)) {
            sLogger.warn(value.variable().getName() + " = " + value.getName() + " -- not good value assigned (noGood:" + noGood(assignment, value) + ")");
            setGood(assignment, value);
        }

        Set<T> noGood = new HashSet<T>(1);

        noGood.add(value);
        for (T anotherValue : value.variable().values(assignment)) {
            if (anotherValue.equals(value) || !isGood(assignment, anotherValue))
                continue;
            setNoGood(assignment, anotherValue, noGood);
        }
        propagate(assignment, value.variable());
    }

    /**
     * Undo propagation when a value is unassigned. <br>
     * <br>
     * <ul>
     * <li>Prints an error if the value is nogood (should not never happen),
     * <li>runs propagation undo.
     * </ul>
     * 
     * @see MacPropagation#undoPropagate(Assignment, Variable)
     */
    @Override
    public void afterUnassigned(Assignment<V, T> assignment, long iteration, T value) {
        iIteration = iteration;
        if (!isGood(assignment, value)) {
            sLogger.error(value.variable().getName() + " = " + value.getName()
                    + " -- not good value unassigned (noGood:" + noGood(assignment, value) + ")");
        }
        undoPropagate(assignment, value.variable());
    }

    /**
     * If no variable is selected (all variables are assinged), unassign the
     * last assigned variable. <br>
     * <br>
     * Do not allow to select an assigned variable. <br>
     * <br>
     * If no variable is selected (because all variables are assigned, see
     * {@link DbtVariableSelection}):
     * <ul>
     * <li>find the last assigned variable and
     * <li>unassign it (explanation is a union of assignments of all other
     * variables).
     * </ul>
     * 
     * @see DbtVariableSelection#selectVariable(Solution)
     */
    @Override
    public boolean variableSelected(Assignment<V, T> assignment, long iteration, V variable) {
        if (variable == null) {
            sLogger.debug("No variable selected -> backtrack.");
            V lastVariable = null;

            for (V var : assignment.assignedVariables()) {
                if (lastVariable == null || assignment.getIteration(lastVariable) < assignment.getIteration(var)) {
                    lastVariable = var;
                }
            }
            if (lastVariable == null) {
                sLogger.error("No assignment -> fail");
                getSolver().stopSolver();
                return false;
            }
            sLogger.debug("Unassign:" + lastVariable.getName());
            Set<T> noGoods = new HashSet<T>();

            for (V var : assignment.assignedVariables()) {
                if (!var.equals(lastVariable)) {
                    noGoods.add(assignment.getValue(var));
                }
            }
            T value = assignment.getValue(lastVariable);

            assignment.unassign(iteration, lastVariable);
            setNoGood(assignment, value, noGoods);
            return false;
        }
        if (assignment.getValue(variable) != null) {
            sLogger.error("Assigned value selected -- not supported by DBT.");
            return false;
        }
        return true;
    }

    /**
     * If no value is selected (because of a dead end), make some unassignments. <br>
     * <br>
     * If no value is selected (e.g., because the selected variable has all
     * values marked as nogood, see {@link DbtValueSelection}),
     * <ul>
     * <li>compute a union of explanations of all values,
     * <ul>
     * <li>if it is empty fail (inconsistency is found),
     * </ul>
     * <li>otherwise pick the last assigned variable from the computed union of
     * explanation and unassign it
     * <pre>
     * (explanation for that is the computed union of explanations without the last assignment).
     * </pre>
     * </ul>
     * 
     * @see DbtVariableSelection#selectVariable(Solution)
     */
    @Override
    public boolean valueSelected(Assignment<V, T> assignment, long iteration, V variable, T value) {
        if (variable != null && value == null) {
            Set<T> noGoods = new HashSet<T>();

            for (T val : variable.values(assignment)) {
                if (noGood(assignment, val) != null) {
                    noGoods.addAll(noGood(assignment, val));
                }
            }
            if (noGoods.isEmpty()) {
                sLogger.debug("Fail");
                getSolver().stopSolver();
                return false;
            }
            V lastVariable = null;

            for (T val : noGoods) {
                V var = val.variable();

                if (lastVariable == null || assignment.getIteration(lastVariable) < assignment.getIteration(var)) {
                    lastVariable = var;
                }
            }
            T current = assignment.getValue(lastVariable);

            noGoods.remove(current);
            assignment.unassign(iteration, lastVariable);
            setNoGood(assignment, current, noGoods);
        }
        return true;
    }

    @Override
    public boolean neighbourSelected(Assignment<V, T> assignment, long iteration, Neighbour<V, T> neighbour) {
        return true;
    }

    @Override
    public void neighbourFailed(Assignment<V, T> assignment, long iteration, Neighbour<V, T> neighbour) {
    }
}
