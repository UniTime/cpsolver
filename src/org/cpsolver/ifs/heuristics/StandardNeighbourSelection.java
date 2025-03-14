package org.cpsolver.ifs.heuristics;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.extension.ConflictStatistics;
import org.cpsolver.ifs.extension.Extension;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.SimpleNeighbour;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.model.WeakeningConstraint;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.solver.SolverListener;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Standard neighbour selection criterion. <br>
 * <br>
 * This criterion is using the provided variable and value selection criteria.
 * In each step, a variable is selected first using the
 * {@link VariableSelection}. Then, a value is selected to the selected
 * variable, using the {@link ValueSelection}. A {@link SimpleNeighbour}
 * containing the selected value is returned. <br>
 * <br>
 * Note: the use of neighbour select criteria extends the former implementation
 * of the IFS algorithm which was only able to use variable and value selection
 * criteria and therefore only one value was assigned in each iteration. <br>
 * <br>
 * Parameters: <br>
 * <table border='1'><caption>Related Solver Parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>Value.Class</td>
 * <td>{@link String}</td>
 * <td>Fully qualified class name of the value selection criterion (see
 * {@link ValueSelection}, e.g. {@link GeneralValueSelection})</td>
 * </tr>
 * <tr>
 * <td>Variable.Class</td>
 * <td>{@link String}</td>
 * <td>Fully qualified class name of the variable selection criterion (see
 * {@link VariableSelection}, e.g. {@link GeneralVariableSelection})</td>
 * </tr>
 * </table>
 * 
 * @see Solver
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
 *
 * @param <V> Variable
 * @param <T> Value
 **/
public class StandardNeighbourSelection<V extends Variable<V, T>, T extends Value<V, T>> implements NeighbourSelection<V, T> {
    protected static org.apache.logging.log4j.Logger sLogger = org.apache.logging.log4j.LogManager.getLogger(StandardNeighbourSelection.class);

    private ValueSelection<V, T> iValueSelection = null;
    private VariableSelection<V, T> iVariableSelection = null;
    protected Solver<V, T> iSolver = null;
    protected ConflictStatistics<V, T> iStat = null;

    /** Sets value selection criterion 
     * @param valueSelection value selection criterion
     **/
    public void setValueSelection(ValueSelection<V, T> valueSelection) {
        iValueSelection = valueSelection;
    }

    /** Sets variable selection criterion 
     * @param variableSelection variable selection criterion
     **/
    public void setVariableSelection(VariableSelection<V, T> variableSelection) {
        iVariableSelection = variableSelection;
    }

    /** Returns value selection criterion 
     * @return value selection criterion
     **/
    public ValueSelection<V, T> getValueSelection() {
        return iValueSelection;
    }

    /** Returns variable selection criterion
     * @return variable selection criterion
     **/
    public VariableSelection<V, T> getVariableSelection() {
        return iVariableSelection;
    }

    /**
     * Constructor
     * 
     * @param properties
     *            configuration
     * @throws Exception thrown when initialization fails
     */
    @SuppressWarnings("unchecked")
    public StandardNeighbourSelection(DataProperties properties) throws Exception {
        String valueSelectionClassName = properties.getProperty("Value.Class", "org.cpsolver.ifs.heuristics.GeneralValueSelection");
        sLogger.info("Using " + valueSelectionClassName);
        Class<?> valueSelectionClass = Class.forName(valueSelectionClassName);
        Constructor<?> valueSelectionConstructor = valueSelectionClass.getConstructor(new Class[] { DataProperties.class });
        setValueSelection((ValueSelection<V, T>) valueSelectionConstructor.newInstance(new Object[] { properties }));

        String variableSelectionClassName = properties.getProperty("Variable.Class", "org.cpsolver.ifs.heuristics.GeneralVariableSelection");
        sLogger.info("Using " + variableSelectionClassName);
        Class<?> variableSelectionClass = Class.forName(variableSelectionClassName);
        Constructor<?> variableSelectionConstructor = variableSelectionClass.getConstructor(new Class[] { DataProperties.class });
        setVariableSelection((VariableSelection<V, T>) variableSelectionConstructor.newInstance(new Object[] { properties }));
    }

    /**
     * Initialization -- methods
     * {@link org.cpsolver.ifs.heuristics.VariableSelection#init(Solver)} and
     * {@link org.cpsolver.ifs.heuristics.ValueSelection#init(Solver)} are
     * called.
     */
    @Override
    public void init(Solver<V, T> solver) {
        getValueSelection().init(solver);
        getVariableSelection().init(solver);
        iSolver = solver;
        for (Extension<V, T> ext: solver.getExtensions())
            if (ext instanceof ConflictStatistics)
                iStat = (ConflictStatistics<V, T>)ext;
    }

    /** Use the provided variable selection criterion to select a variable 
     * @param solution current solution
     * @return selected variable
     **/
    public V selectVariable(Solution<V, T> solution) {
        // Variable selection
        V variable = getVariableSelection().selectVariable(solution);
        for (SolverListener<V, T> listener : iSolver.getSolverListeners())
            if (!listener.variableSelected(solution.getAssignment(), solution.getIteration(), variable))
                return null;
        if (variable == null)
            sLogger.debug("No variable selected.");

        if (variable != null && variable.values(solution.getAssignment()).isEmpty()) {
            sLogger.debug("Variable " + variable.getName() + " has no values.");
            return null;
        }
        return variable;
    }

    /**
     * Use the provided value selection criterion to select a value to the
     * selected variable
     * @param solution current solution
     * @param variable selected variable
     * @return selected value
     */
    @SuppressWarnings("unchecked")
    public T selectValue(Solution<V, T> solution, V variable) {
        // Value selection
        T value = getValueSelection().selectValue(solution, variable);
        for (SolverListener<V, T> listener : iSolver.getSolverListeners())
            if (!listener.valueSelected(solution.getAssignment(), solution.getIteration(), variable, value))
                return null;

        if (value == null) {
            sLogger.debug("No value selected for variable " + variable + ".");
            for (Constraint<V, T> constraint: variable.hardConstraints()) {
                if (constraint.isHard() && constraint instanceof WeakeningConstraint)
                    ((WeakeningConstraint<V, T>)constraint).weaken(solution.getAssignment());
            }
            for (Constraint<V, T> constraint: solution.getModel().globalConstraints()) {
                if (constraint.isHard() && constraint instanceof WeakeningConstraint)
                    ((WeakeningConstraint<V, T>)constraint).weaken(solution.getAssignment());
            }
            return null;
        } else {
            for (Constraint<V, T> constraint: variable.hardConstraints()) {
                if (constraint instanceof WeakeningConstraint && constraint.inConflict(solution.getAssignment(), value))
                    ((WeakeningConstraint<V, T>)constraint).weaken(solution.getAssignment());
            }
            for (Constraint<V, T> constraint: solution.getModel().globalConstraints()) {
                if (constraint instanceof WeakeningConstraint && constraint.inConflict(solution.getAssignment(), value))
                    ((WeakeningConstraint<V, T>)constraint).weaken(solution.getAssignment());
            }
        }
        return value;
    }

    /**
     * Select neighbour. A value is selected to the selected variable.
     */
    @Override
    public Neighbour<V, T> selectNeighbour(Solution<V, T> solution) {
        V variable = selectVariable(solution);
        if (variable == null)
            return null;
        T value = selectValue(solution, variable);
        if (value == null)
            return null;
        if (iSolver.hasSingleSolution()) {
            Set<T> conflicts = solution.getModel().conflictValues(solution.getAssignment(), value);
            if (iStat != null)
                for (Map.Entry<Constraint<V, T>, Set<T>> entry: solution.getModel().conflictConstraints(solution.getAssignment(), value).entrySet())
                    iStat.constraintAfterAssigned(solution.getAssignment(), solution.getIteration(), entry.getKey(), value, entry.getValue());
                // for (T conflict: conflicts)
                //      iStat.variableUnassigned(solution.getIteration(), conflict, value);
            return new SimpleNeighbour<V, T>(variable, value, conflicts);
        } else {
            return new SimpleNeighbour<V, T>(variable, value);
        }
    }
}
