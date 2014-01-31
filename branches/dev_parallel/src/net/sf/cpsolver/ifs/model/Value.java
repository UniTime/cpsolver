package net.sf.cpsolver.ifs.model;

import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.ifs.assignment.Assignment;
import net.sf.cpsolver.ifs.assignment.ValueComparator;
import net.sf.cpsolver.ifs.assignment.context.ExtensionWithContext;
import net.sf.cpsolver.ifs.util.IdGenerator;

/**
 * Generic value. <br>
 * <br>
 * Every value has a notion about the variable it belongs to. It has also a
 * unique id. By default, every Value has an integer value which is used in
 * general heuristics, the task is than to minimimize the total value of
 * assigned values in the solution.
 * 
 * @see Variable
 * @see Model
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
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */
public class Value<V extends Variable<V, T>, T extends Value<V, T>> implements Comparable<T> {
    private static IdGenerator sIdGenerator = new IdGenerator();

    private long iId;
    private V iVariable = null;

    /** Integer value */
    protected double iValue = 0;
    /**
     * Extra information which can be used by an IFS extension (see
     * {@link net.sf.cpsolver.ifs.extension.Extension})
     */
    private Object iExtra = null;

    /**
     * Constructor
     * 
     * @param variable
     *            variable which the value belongs to
     */
    public Value(V variable) {
        iId = sIdGenerator.newId();
        iVariable = variable;
    }

    /**
     * Constructor
     * 
     * @param variable
     *            variable which the value belongs to
     * @param value
     *            integer value
     */
    public Value(V variable, double value) {
        iId = sIdGenerator.newId();
        iVariable = variable;
        iValue = value;
    }

    /** Returns the variable which this value belongs to */
    public V variable() {
        return iVariable;
    }

    /** Sets the variable which this value belongs to */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setVariable(Variable variable) {
        iVariable = (V) variable;
    }

    /** Unique id */
    public long getId() {
        return iId;
    }

    /** Values name -- for printing purposes (E.g., Monday 7:30) */
    public String getName() {
        return String.valueOf(iId);
    }

    /** Values description -- for printing purposes */
    public String getDescription() {
        return null;
    }

    /**
     * Double representation. This allows us to have generic optimization
     * criteria. The task is than to minimize total value of assigned variables
     * of a solution.
     */
    public double toDouble(Assignment<V, T> assignment) {
        return iValue;
    }
    
    /**
     * Double representation. This allows us to have generic optimization
     * criteria. The task is than to minimize total value of assigned variables
     * of a solution.
     * If the value may depend on other values of the assignment, use {@link Value#toDouble(Assignment)} instead.
     */
    @Deprecated
    public double toDouble() {
        return iValue;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int hashCode() {
        return (int) iId;
    }

    /**
     * Comparison of two values which is based only on the value (not
     * appropriate variable etc.). {@link Value#toDouble(Assignment)} is compared by default.
     */
    public boolean valueEquals(Assignment<V, T> assignment, T value) {
        if (value == null)
            return false;
        return toDouble(assignment) == value.toDouble(assignment);
    }

    /**
     * Comparison of two values which is based only on the value (not
     * appropriate variable etc.). {@link Value#toDouble(Assignment)} is compared by default.
     * Use {@link Value#valueEquals(Assignment, Value)} instead.
     */
    @Deprecated
    public boolean valueEquals(T value) {
    	return valueEquals(variable().getModel().getDefaultAssignment(), value);
    }

    /**
     * Compare two values by their value
     */
    public int compareTo(Assignment<V, T> assignment, T value) {
        if (value == null)
            return -1;
        int cmp = Double.compare(toDouble(assignment), value.toDouble(assignment));
        if (cmp != 0)
            return cmp;
        return Double.compare(getId(), value.getId());
    }
    
    /**
     * Compare two values by their value.
     * Use {@link Value#compareTo(Assignment, Value)} and {@link ValueComparator} instead.
     */
    @Override
    @Deprecated
    public int compareTo(T value) {
    	return compareTo(variable().getModel().getDefaultAssignment(), value);
    }

    /** By default, comparison is made on unique ids */
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Value<?, ?>))
            return false;
        return getId() == ((Value<?, ?>) o).getId();
    }

    /**
     * Extra information to which can be used by an extension (see
     * {@link net.sf.cpsolver.ifs.extension.Extension}).
     * Use {@link ExtensionWithContext} instead.
     */
    @Deprecated
    public Object getExtra() {
        return iExtra;
    }

    /**
     * Extra information to which can be used by an extension (see
     * {@link net.sf.cpsolver.ifs.extension.Extension}).
     * Use {@link ExtensionWithContext} instead.
     */
    @Deprecated
    public void setExtra(Object object) {
        iExtra = object;
    }

    /** True, if the value is consistent with the given value */
    @SuppressWarnings("unchecked")
    public boolean isConsistent(T value) {
        for (Constraint<V, T> constraint : iVariable.constraints()) {
            if (!constraint.isConsistent((T) this, value))
                return false;
        }
        for (Constraint<V, T> constraint : iVariable.getModel().globalConstraints()) {
            if (!constraint.isConsistent((T) this, value))
                return false;
        }
        return true;
    }

    /**
     * Returns a set of conflicting values with this value. When empty, the
     * value is consistent with the existing assignment.
     */
    @SuppressWarnings("unchecked")
    public Set<T> conflicts(Assignment<V, T> assignment) {
        HashSet<T> conflicts = new HashSet<T>();
        for (Constraint<V, T> constraint : iVariable.constraints()) {
            constraint.computeConflicts(assignment, (T) this, conflicts);
        }
        for (Constraint<V, T> constraint : iVariable.getModel().globalConstraints()) {
            constraint.computeConflicts(assignment, (T) this, conflicts);
        }
        if (!conflicts.isEmpty())
            return conflicts;
        return null;
    }
    
    /**
     * Returns a set of conflicting values with this value. When empty, the
     * value is consistent with the existing assignment.
     * Use {@link Value#conflicts(Assignment)} instead.
     */
    @Deprecated
    public Set<T> conflicts() {
    	return conflicts(variable().getModel().getDefaultAssignment());
    }
}
