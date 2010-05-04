package net.sf.cpsolver.ifs.model;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import net.sf.cpsolver.ifs.util.EnumerableCollection;

/**
 * Generic global constraint.
 * <br><br>
 * Global constraint is a {@link Constraint} that applies to all variables.
 *
 * @see Variable
 * @see Model
 * @see Constraint
 * @see net.sf.cpsolver.ifs.solver.Solver
 *
 * @version
 * IFS 1.1 (Iterative Forward Search)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

public abstract class GlobalConstraint extends Constraint {
    
    /** The list of variables of this constraint */
    public Vector variables() { return getModel().variables(); }
    /** The list of variables of this constraint that are assigned */
    public EnumerableCollection assignedVariables() { return getModel().assignedVariables(); }

    /** Add a variable to this constraint */
    public void addVariable(Variable variable) {
        throw new RuntimeException("A variable cannot be added to a global constraint.");
    }
    /** Remove a variable from this constraint */
    public void removeVariable(Variable variable) {
        throw new RuntimeException("A variable cannot be removed from a global constraint.");
    }
    
    /** Given value is to be assigned to its varable. In this method, the constraint should unassigns 
     * all varaibles which are in conflict with the given assignment because of this constraint.
     */
    public void assigned(long iteration, Value value) {
        HashSet conf = null;
        if (isHard()) {
            conf = new HashSet(); computeConflicts(value, conf);
        }
        if (constraintListeners()!=null)
            for (Enumeration e=constraintListeners().elements();e.hasMoreElements();)
                ((ConstraintListener)e.nextElement()).constraintBeforeAssigned(iteration, this, value, conf);
        if (conf!=null) {
            for (Iterator i=conf.iterator(); i.hasNext(); ) {
                Value conflictValue = (Value)i.next();
                conflictValue.variable().unassign(iteration);
            }
        }
        if (constraintListeners()!=null)
            for (Enumeration e=constraintListeners().elements();e.hasMoreElements();)
                ((ConstraintListener)e.nextElement()).constraintAfterAssigned(iteration, this, value, conf);
    }
    
    /** Given value is unassigned from its varable.
     */
    public void unassigned(long iteration, Value value) {
    }
}
