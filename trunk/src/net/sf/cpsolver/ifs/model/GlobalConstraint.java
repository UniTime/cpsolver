package net.sf.cpsolver.ifs.model;

import net.sf.cpsolver.ifs.util.Collection;
import net.sf.cpsolver.ifs.util.HashSet;
import net.sf.cpsolver.ifs.util.List;

/**
 * Generic global constraint. <br>
 * <br>
 * Global constraint is a {@link Constraint} that applies to all variables.
 * 
 * @see Variable
 * @see Model
 * @see Constraint
 * @see net.sf.cpsolver.ifs.solver.Solver
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 */

public abstract class GlobalConstraint<V extends Variable<V, T>, T extends Value<V, T>> extends Constraint<V, T> {

    /** The list of variables of this constraint */
    @Override
    public List<V> variables() {
        return getModel().variables();
    }

    /** The list of variables of this constraint that are assigned */
    @Override
    public Collection<V> assignedVariables() {
        return getModel().assignedVariables();
    }

    /** Add a variable to this constraint */
    @Override
    public void addVariable(V variable) {
        throw new RuntimeException("A variable cannot be added to a global constraint.");
    }

    /** Remove a variable from this constraint */
    @Override
    public void removeVariable(V variable) {
        throw new RuntimeException("A variable cannot be removed from a global constraint.");
    }

    /**
     * Given value is to be assigned to its varable. In this method, the
     * constraint should unassigns all varaibles which are in conflict with the
     * given assignment because of this constraint.
     */
    @Override
    public void assigned(long iteration, T value) {
        HashSet<T> conf = null;
        if (isHard()) {
            conf = new HashSet<T>();
            computeConflicts(value, conf);
        }
        if (constraintListeners() != null)
            for (ConstraintListener<T> listener : iConstraintListeners)
                listener.constraintBeforeAssigned(iteration, this, value, conf);
        if (conf != null) {
            for (T conflictValue : conf) {
                conflictValue.variable().unassign(iteration);
            }
        }
        if (constraintListeners() != null)
            for (ConstraintListener<T> listener : iConstraintListeners)
                listener.constraintAfterAssigned(iteration, this, value, conf);
    }

    /**
     * Given value is unassigned from its varable.
     */
    @Override
    public void unassigned(long iteration, T value) {
    }
}
