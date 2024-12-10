package org.cpsolver.ifs.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;


/**
 * A neighbour consisting of a change (either assignment or unassignment) of a
 * single variable.
 * 
 * @see org.cpsolver.ifs.heuristics.NeighbourSelection
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
public class SimpleNeighbour<V extends Variable<V, T>, T extends Value<V, T>> implements Neighbour<V, T> {
    private V iVariable = null;
    private T iValue = null;
    private Set<T> iConflicts = null;

    /**
     * Model
     * 
     * @param variable
     *            variable to be assigned
     * @param value
     *            value to be assigned to the given variable, null if the
     *            variable should be unassigned
     */
    public SimpleNeighbour(V variable, T value) {
        iVariable = variable;
        iValue = value;
    }
    
    public SimpleNeighbour(V variable, T value, Set<T> conflicts) {
        iVariable = variable;
        iValue = value;
        iConflicts = conflicts;
    }

    /** Selected variable 
     * @return a variable 
     **/
    public V getVariable() {
        return iVariable;
    }

    /** Selected value
     * @return a value, null if the variable is to be unassigned
     **/
    public T getValue() {
        return iValue;
    }

    /** Perform assignment */
    @Override
    public void assign(Assignment<V, T> assignment, long iteration) {
        if (iVariable == null)
            return;
        if (iConflicts != null)
            for (T conflict: iConflicts)
                assignment.unassign(iteration, conflict.variable());
        if (iValue != null)
            assignment.assign(iteration, iValue);
        else
            assignment.unassign(iteration, iVariable);
    }

    /** Improvement in the solution value if this neighbour is accepted. */
    @Override
    public double value(Assignment<V, T> assignment) {
        T old = assignment.getValue(iVariable);
        // assigning an unassigned variable is always an improving move
        if (iValue != null && old == null && (iConflicts == null || iConflicts.isEmpty())) return -1;
        double val = (iValue == null ? 0 : iValue.toDouble(assignment)) - (iVariable == null || old == null ? 0 : old.toDouble(assignment));
        if (iConflicts != null)
            for (T conflict: iConflicts)
                val -= conflict.toDouble(assignment);
        return val;
    }

    @Override
    public String toString() {
        return iVariable.getName() + " := " + (iValue == null ? "null" : iValue.getName());
    }

    @Override
    public Map<V, T> assignments() {
        HashMap<V, T> ret = new HashMap<V, T>();
        if (iVariable != null)
            ret.put(iVariable, iValue);
        if (iConflicts != null)
            for (T conflict: iConflicts)
                ret.put(conflict.variable(), null);
        return ret;
    }
}
