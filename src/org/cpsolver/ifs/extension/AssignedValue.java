package org.cpsolver.ifs.extension;

import java.util.Comparator;

import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.Value;


/**
 * This class describing an assignment of a value to a variable together with a
 * counter (used by CBS).
 * 
 * Counter also supports aging: the counter is multiplied by aging factor for
 * each iteration.
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
 * @param <T> Value
 */
public class AssignedValue<T extends Value<?, T>> {
    private T iValue;
    private double iCounter = 1.0;
    private long iLastRevision;
    private double iAging = 1.0;
    private Constraint<?, T> iConstraint = null;

    /**
     * Constructor
     * 
     * @param iteration
     *            current iteration
     * @param value
     *            value
     * @param aging
     *            aging factor
     */
    public AssignedValue(long iteration, T value, double aging) {
        iValue = value;
        iLastRevision = iteration;
        iAging = aging;
    }

    /** Returns value 
     * @return value */
    public T getValue() {
        return iValue;
    }

    /**
     * Increments counter
     * 
     * @param iteration
     *            current iteration
     */
    public void incCounter(long iteration) {
        revise(iteration);
        iCounter += 1.0;
    }

    /**
     * Set counter
     * 
     * @param cnt
     *            new value
     */
    public void setCounter(double cnt) {
        iCounter = cnt;
    }

    /**
     * Get counter
     * 
     * @param iteration
     *            current iteration
     * @return counter
     */
    public double getCounter(long iteration) {
        if (iteration == 0l)
            return iCounter;
        if (iAging == 1.0)
            return iCounter;
        return iCounter * Math.pow(iAging, iteration - iLastRevision);
    }

    /** Returns constraint 
     * @return constraint
     **/
    public Constraint<?, T> getConstraint() {
        return iConstraint;
    }

    /** Sets constraint 
     * @param constraint a constraint
     **/
    public void setConstraint(Constraint<?, T> constraint) {
        iConstraint = constraint;
    }

    /**
     * Revise counter. If aging is used, counter is adopted to the current
     * iteration: it is multiplied by aging factor powered by the number of
     * iterations since last revision.
     * @param iteration current iteration
     */
    public synchronized void revise(long iteration) {
        if (iAging == 1.0)
            return;
        iCounter *= Math.pow(iAging, iteration - iLastRevision);
        iLastRevision = iteration;
    }

    /**
     * Combine two integers (for hash code)
     * @param a first integer
     * @param b second integer
     * @return combined hash code
     */
    public static int combine(int a, int b) {
        int ret = 0;
        for (int i = 0; i < 15; i++)
            ret = ret | ((a & (1 << i)) << i) | ((b & (1 << i)) << (i + 1));
        return ret;
    }

    @Override
    public int hashCode() {
        return iValue.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof AssignedValue<?>))
            return false;
        return ((AssignedValue<?>) o).getValue().equals(getValue());
    }

    /** String representation */
    @Override
    public String toString() {
        return toString(0l, true);
    }

    /** String representation (e.g., 10x A := a) 
     * @param iteration current iteration
     * @param assignment true if assignment
     * @return string representation of the assignment
     **/
    public String toString(long iteration, boolean assignment) {
        return (assignment ? getCounter(iteration) + "x " : "") + getValue().variable().getName()
                + (assignment ? " := " : " != ") + getValue().getName() + (getConstraint() != null ? " (" + getConstraint() + ")" : "");
    }

    /** Compare two assignments (their counters)
     * @param iteration current iteration
     * @param a another assignment
     * @return comparison
     **/
    public int compareTo(long iteration, AssignedValue<T> a) {
        int cmp = getValue().variable().getName().compareTo(a.getValue().variable().getName());
        if (cmp != 0)
            return cmp;
        if (getCounter(iteration) != a.getCounter(iteration))
            return (getCounter(iteration) < a.getCounter(iteration) ? 1 : -1);
        return getValue().getName().compareTo(a.getValue().getName());
    }

    /** Assignment comparator 
     * @param <E> a value
     **/
    public static class AssignmentComparator<E extends Value<?, E>> implements Comparator<AssignedValue<E>> {
        private long iIteration;

        public AssignmentComparator(long iteration) {
            iIteration = iteration;
        }

        @Override
        public int compare(AssignedValue<E> a1, AssignedValue<E> a2) {
            return a1.compareTo(iIteration, a2);
        }
    }
}
