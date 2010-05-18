package net.sf.cpsolver.ifs.model;

/**
 * A neighbour consisting of a change (either assignment or unassignment) of a
 * single variable.
 * 
 * @see net.sf.cpsolver.ifs.heuristics.NeighbourSelection
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 */

public class SimpleNeighbour<V extends Variable<V, T>, T extends Value<V, T>> extends Neighbour<V, T> {
    private V iVariable = null;
    private T iValue = null;

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

    /** Selected variable */
    public V getVariable() {
        return iVariable;
    }

    /** Selected value */
    public T getValue() {
        return iValue;
    }

    /** Perform assignment */
    @Override
    public void assign(long iteration) {
        if (iVariable == null)
            return;
        if (iValue != null)
            iVariable.assign(iteration, iValue);
        else
            iVariable.unassign(iteration);
    }

    /** Improvement in the solution value if this neighbour is accepted. */
    @Override
    public double value() {
        return (iValue == null ? 0 : iValue.toDouble())
                - (iVariable == null || iVariable.getAssignment() == null ? 0 : iVariable.getAssignment().toDouble());
    }

    @Override
    public String toString() {
        return iVariable.getName() + " "
                + (iVariable.getAssignment() == null ? "null" : iVariable.getAssignment().getName()) + " -> "
                + (iValue == null ? "null" : iValue.getName());
    }
}
