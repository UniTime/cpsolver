package org.cpsolver.ifs.perturbations;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.extension.Extension;
import org.cpsolver.ifs.extension.ViolatedInitials;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Default computation of perturbation penalty (minimal perturbation problem). <br>
 * <br>
 * A distance function can be defined with the help of perturbations. A
 * perturbation is a variable that has a different value in the solutions of the
 * initial and the new problem. Some perturbations must be present in each new
 * solution. So called input perturbation means that a variable must have
 * different values in the initial and changed problem because of some input
 * changes (e.g., a course must be scheduled at a different time in the changed
 * problem). The distance function can be defined as the number of additional
 * perturbations. They are given by subtraction of the final number of
 * perturbations and the number of input perturbations (variables without
 * initial assignments). <br>
 * <br>
 * This implementation is easily extendable. It disassemble all the available
 * cases into a comparison of the initial and the assigned value different each
 * other. So, the only method which is needed to be changed is
 * {@link DefaultPerturbationsCounter#getPenalty(Assignment, Value, Value)}. Its current
 * implementation is:
 * <pre>
 * <code>
 * protected double getPenalty(Value assignedValue, Value initialValue) {
 * &nbsp;&nbsp;&nbsp;&nbsp;return 1.0;
 * }
 * </code>
 * </pre>
 * It is called only when assignedValue is different to initialValue.
 * 
 * @see Solver
 * @see Solution
 * @see Variable
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
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 *
 * @param <V> Variable
 * @param <T> Value
 */
public class DefaultPerturbationsCounter<V extends Variable<V, T>, T extends Value<V, T>> implements PerturbationsCounter<V, T> {
    private ViolatedInitials<V, T> iViolatedInitials = null;
    protected static java.text.DecimalFormat sDoubleFormat = new java.text.DecimalFormat("0.00", new java.text.DecimalFormatSymbols(Locale.US));

    /**
     * Constructor
     * 
     * @param properties
     *            input configuration
     */
    public DefaultPerturbationsCounter(DataProperties properties) {
    }

    /** Initialization */
    @Override
    public void init(Solver<V, T> solver) {
        for (Extension<V, T> extension : solver.getExtensions()) {
            if (ViolatedInitials.class.isInstance(extension))
                iViolatedInitials = (ViolatedInitials<V, T>) extension;
        }
    }

    @Override
    public double getPerturbationPenalty(Assignment<V, T> assignment, Model<V, T> model) {
        double penalty = 0.0;
        for (V variable : model.variablesWithInitialValue()) {
            T value = assignment.getValue(variable);
            if (value != null && variable.getInitialAssignment() != null && !value.equals(variable.getInitialAssignment()))
                penalty += getPenaltyD(assignment, value, variable.getInitialAssignment());
        }
        return penalty;
    }

    @Override
    public double getPerturbationPenalty(Assignment<V, T> assignment, Model<V, T> model, Collection<V> variables) {
        double penalty = 0.0;
        for (V variable : variables) {
            T value = assignment.getValue(variable);
            if (value != null && variable.getInitialAssignment() != null && !value.equals(variable.getInitialAssignment()))
                penalty += getPenaltyD(assignment, value, variable.getInitialAssignment());
        }
        return penalty;
    }

    protected ViolatedInitials<V, T> getViolatedInitials() {
        return iViolatedInitials;
    }

    /**
     * Computes perturbation penalty between assigned and initial value of the
     * same lecture. It is called only when assignedValue is different to
     * initialValue.
     * 
     * @param assignment current assignment
     * @param assignedValue
     *            value assigned to a varuable (null when variable is
     *            unassigned)
     * @param initialValue
     *            initial value of the same varaible (always not null)
     * @return penalty
     */
    protected double getPenalty(Assignment<V, T> assignment, T assignedValue, T initialValue) {
        return 1.0;
    }

    /**
     * Case A: initial value of a different unassigned variable cannot be
     * assigned (computed by {@link ViolatedInitials})
     * 
     * @param assignment current assignment
     * @param selectedValue
     *            value which is going to be assigned to its variable
     * @param initialValue
     *            value of a different variable, which is currently assigned but
     *            which need to be unassifned Different variable, which is
     *            unassigned and whose initial value is in conflict with the
     *            selected value.
     * @return penalty
     */
    protected double getPenaltyA(Assignment<V, T> assignment, T selectedValue, T initialValue) {
        return getPenalty(assignment, null, initialValue);
    }

    /**
     * Case B: initial value is unassigned from a conflicting variable.
     * 
     * @param assignment current assignment
     * @param selectedValue
     *            value which is going to be unassigned to its variable
     * @param assignedValue
     *            value currently assigned to a conflicting variable (different
     *            from the one of selectedVariable)
     * @param initialValue
     *            initial value of the conflicting variable of assignedValue
     * @return penalty
     */
    protected double getPenaltyB(Assignment<V, T> assignment, T selectedValue, T assignedValue, T initialValue) {
        return getPenalty(assignment, assignedValue, initialValue);
    }

    /**
     * Case C: non-initial value is unassigned from a conflicting variable.
     * 
     * @param assignment current assignment
     * @param selectedValue
     *            value which is going to be unassigned to its variable
     * @param assignedValue
     *            value currently assigned to a conflicting variable (different
     *            from the one of selectedVariable)
     * @param initialValue
     *            initial value of the conflicting variable of assignedValue
     * @return penalty
     */
    protected double getPenaltyC(Assignment<V, T> assignment, T selectedValue, T assignedValue, T initialValue) {
        return -getPenalty(assignment, assignedValue, initialValue);
    }

    /**
     * Case D: different than initial value is assigned to the variable
     * 
     * @param assignment current assignment
     * @param selectedValue
     *            value which is going to be unassigned to its variable
     * @param initialValue
     *            initial value of the same variable
     * @return penalty
     */
    protected double getPenaltyD(Assignment<V, T> assignment, T selectedValue, T initialValue) {
        return getPenalty(assignment, selectedValue, initialValue);
    }

    @Override
    public double getPerturbationPenalty(Assignment<V, T> assignment, Model<V, T> model, T selectedValue, Collection<T> conflicts) {
        double penalty = 0;
        Set<T> violations = (getViolatedInitials() == null ? null : getViolatedInitials().getViolatedInitials(selectedValue));
        if (violations != null)
            for (T aValue : violations) {
                if (assignment.getValue(aValue.variable()) == null)
                    penalty += getPenaltyA(assignment, selectedValue, aValue);
            }
        if (conflicts != null) {
            for (T conflictValue : conflicts) {
                T initialValue = conflictValue.variable().getInitialAssignment();
                if (initialValue != null) {
                    if (initialValue.equals(conflictValue))
                        penalty += getPenaltyB(assignment, selectedValue, conflictValue, initialValue);
                    else {
                        if (violations == null || !violations.contains(initialValue))
                            penalty += getPenaltyC(assignment, selectedValue, conflictValue, initialValue);
                    }
                }
            }
        }
        if (selectedValue.variable().getInitialAssignment() != null && !selectedValue.equals(selectedValue.variable().getInitialAssignment()))
            penalty += getPenaltyD(assignment, selectedValue, selectedValue.variable().getInitialAssignment());
        return penalty;
    }

    @Override
    public void getInfo(Assignment<V, T> assignment, Model<V, T> model, Map<String, String> info) {
        if (model.variablesWithInitialValue().size() > 0)
            info.put("Perturbations: Total penalty", sDoubleFormat.format(getPerturbationPenalty(assignment, model)));
    }

    @Override
    public void getInfo(Assignment<V, T> assignment, Model<V, T> model, Map<String, String> info, Collection<V> variables) {
        if (model.variablesWithInitialValue().size() > 0)
            info.put("Perturbations: Total penalty", sDoubleFormat.format(getPerturbationPenalty(assignment, model, variables)));
    }
}
