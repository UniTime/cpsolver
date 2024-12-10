package org.cpsolver.ifs.extension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Computation of violated initial values (minimal perturbation problem). <br>
 * <br>
 * It is using {@link Constraint#isConsistent(Value, Value)} to find out what
 * initial values (of different variables) cannot be assigned when an arbitrary
 * value is assigned to a variable. This information is computed in advance,
 * before the solver is executed. It is used for better estimation of
 * perturbation penalty (see
 * {@link org.cpsolver.ifs.perturbations.PerturbationsCounter}) when a value
 * is to be assigned to a variable.
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
 * @param <V> Variable
 * @param <T> Value
 */
public class ViolatedInitials<V extends Variable<V, T>, T extends Value<V, T>> extends Extension<V, T> {
    private static org.apache.logging.log4j.Logger sLogger = org.apache.logging.log4j.LogManager.getLogger(ViolatedInitials.class);
    private Map<T, Set<T>> iViolatedInitials = new HashMap<T, Set<T>>();

    public ViolatedInitials(Solver<V, T> solver, DataProperties properties) {
        super(solver, properties);
    }

    /** Compute the violations between any value and all other initial values 
     * @param assignment current assignment
     * @return true if initialized properly
     **/
    public boolean init(Assignment<V, T> assignment) {
        sLogger.info("Computation of violated initials enabled.");
        for (V variable : getModel().variables()) {
            if (variable.getInitialAssignment() == null)
                continue;
            for (Constraint<V, T> constraint : variable.hardConstraints()) {
                for (T value : conflictValues(assignment, constraint, variable.getInitialAssignment())) {
                    addViolatedInitial(value, variable.getInitialAssignment());
                }
            }
        }
        return true;
    }

    /** Initial values that cannot be assigned when the given value is assigned 
     * @param value given value
     * @return list of initial values that cannot be assigned due to the given value
     *
     **/
    public Set<T> getViolatedInitials(T value) {
        return iViolatedInitials.get(value);
    }

    private void addViolatedInitial(T value, T anotherValue) {
        Set<T> violations = iViolatedInitials.get(value);
        if (violations == null) {
            violations = new HashSet<T>();
            iViolatedInitials.put(value, violations);
        }
        violations.add(anotherValue);
    }

    private List<T> conflictValues(Assignment<V, T> assignment, Constraint<V, T> constraint, T aValue) {
        List<T> ret = new ArrayList<T>();
        for (V variable : constraint.variables()) {
            if (variable.equals(aValue.variable()))
                continue;
            if (assignment.getValue(variable) != null)
                continue;
            for (T value : variable.values(assignment)) {
                if (!constraint.isConsistent(aValue, value))
                    ret.add(value);
            }
        }
        return ret;
    }
}
