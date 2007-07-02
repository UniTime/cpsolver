package net.sf.cpsolver.ifs.model;

import java.util.*;

import net.sf.cpsolver.ifs.util.*;

/**
 * Generic value.
 * <br><br>
 * Every value has a notion about the variable it belongs to. It has also a unique id.
 * By default, every Value has an integer value which is used in general heuristics,
 * the task is than to minimimize the total value of assigned values in the solution.
 *
 * @see Variable
 * @see Model
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
public class Value implements Comparable {
    private static IdGenerator sIdGenerator = new IdGenerator();
    
    private long iId;
    private Variable iVariable = null;
    
    private long iAssignmentCounter = 0;
    private long iLastAssignmentIteration = -1;
    private long iLastUnassignmentIteration = -1;
    
    /** Integer value */
    protected double iValue = 0;
    /** Extra information which can be used by an IFS extension (see {@link net.sf.cpsolver.ifs.extension.Extension})*/
    private Object iExtra = null;
    
    /** Constructor
     * @param variable variable which the value belongs to
     */
    public Value(Variable variable) {
        iId = sIdGenerator.newId();
        iVariable = variable;
    }

    /** Constructor
     * @param variable variable which the value belongs to
     * @param value integer value
     */
    public Value(Variable variable, double value) {
        iId = sIdGenerator.newId();
        iVariable = variable;
        iValue = value;
    }
    
    /** Returns the variable which this value belongs to */
    public Variable variable() { return iVariable; }
    /** Sets the variable which this value belongs to */
    public void setVariable(Variable variable) { iVariable = variable; }
    
    /** Notification (called by variable) that this value is assigned
     * @param iteration current iteration
     */
    public void assigned(long iteration) {
        iAssignmentCounter++; iLastAssignmentIteration = iteration;
    }
    
    /** Notification (called by variable) that this value is unassigned
     * @param iteration current iteration
     */
    public void unassigned(long iteration) { iLastUnassignmentIteration = iteration; }
    
    /** Returns the iteration when the value was assigned at last (-1 if never).*/
    public long lastAssignmentIteration() { return iLastAssignmentIteration; }
    /** Returns the iteration when the value was unassigned at last (-1 if never).*/
    public long lastUnassignmentIteration() { return iLastUnassignmentIteration; }
    /** Returns the number of assignments of this value to its variable.*/
    public long countAssignments() { return iAssignmentCounter; }
    
    /** Unique id */
    public long getId() { return iId;}
    
    /** Values name -- for printing purposes (E.g., Monday 7:30)*/
    public String getName() { return String.valueOf(iId); }
    
    /** Values description -- for printing purposes*/
    public String getDescription() { return null; }
    
    /** Dobouble representaion. This allows us to have generic optimization criteria. The task
     * is than to minimize total value of assigned variables of a solution.
     */
    public double toDouble() { return iValue; }
    
    public String toString() { return getName(); }
    
    public int hashCode() { return (int)iId; }
    
    /** Comparison of two values which is based only on the value (not appropriate variable etc.). toDouble() is compared by default. */
    public boolean valueEquals(Value value) {
        if (value==null) return false;
        return toDouble()==value.toDouble();
    }
    
    public int compareTo(Object o) {
        if (o==null || !(o instanceof Value)) return -1;
        int cmp = Double.compare(toDouble(),((Value)o).toDouble());
        if (cmp!=0) return cmp;
        return Double.compare(getId(),((Value)o).getId());
    }
    
    /** By default, comparison is made on unique ids */
    public boolean equals(Object o) {
        try {
            return getId()==((Value)o).getId();
        } catch (Exception e) { return false; }
    }
    
    /** Extra information to which can be used by an extension (see {@link net.sf.cpsolver.ifs.extension.Extension}). */
    public Object getExtra() { return iExtra; }
    /** Extra information to which can be used by an extension (see {@link net.sf.cpsolver.ifs.extension.Extension}). */
    public void setExtra(Object object) { iExtra = object; }

    /** True, if the value is consistent with the given value */
    public boolean isConsistent(Value value) {
        for (Enumeration e1=variable().constraints().elements();e1.hasMoreElements();) {
        	Constraint constraint = (Constraint)e1.nextElement();
        	if (!constraint.isConsistent(this, value))
        		return false;
        }
        for (Enumeration e1=variable().getModel().globalConstraints().elements();e1.hasMoreElements();) {
            Constraint constraint = (Constraint)e1.nextElement();
            if (!constraint.isConsistent(this, value))
                return false;
        }
        return true;
    }

    /** Returns a set of conflicting values with this value. When empty, the value is consistent with the existing assignment. */
    public java.util.Set conflicts() {
        HashSet conflicts = new HashSet();
        for (Enumeration e1=variable().constraints().elements();e1.hasMoreElements();) {
            Constraint constraint = (Constraint)e1.nextElement();
            constraint.computeConflicts(this, conflicts);
        }
        if (!conflicts.isEmpty()) return conflicts;
        return null;
    }
}
