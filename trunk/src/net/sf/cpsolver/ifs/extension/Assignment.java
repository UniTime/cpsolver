package net.sf.cpsolver.ifs.extension;

import java.util.Comparator;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Value;

/**
 * This class describing an assignment of a value to a variable together with a
 * counter (used by CBS).
 * 
 * Counter also supports ageing: the counter is multiplied by aging factor for
 * each iteration.
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
public class Assignment<T extends Value<?, T>> {
    private T iValue;
    private double iCounter = 1.0;
    private long iLastRevision;
    private double iAgeing = 1.0;
    private Constraint<?, T> iConstraint = null;

    /**
     * Constructor
     * 
     * @param iteration
     *            current iteration
     * @param value
     *            value
     * @param ageing
     *            ageing factor
     */
    public Assignment(long iteration, T value, double ageing) {
        iValue = value;
        iLastRevision = iteration;
        iAgeing = ageing;
    }

    /** Returns value */
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
     */
    public double getCounter(long iteration) {
        if (iteration == 0l)
            return iCounter;
        if (iAgeing == 1.0)
            return iCounter;
        return iCounter * Math.pow(iAgeing, iteration - iLastRevision);
    }

    /** Returns constraint */
    public Constraint<?, T> getConstraint() {
        return iConstraint;
    }

    /** Sets constraint */
    public void setConstraint(Constraint<?, T> constraint) {
        iConstraint = constraint;
    }

    /**
     * Revise counter. If ageing is used, counter is adopted to the current
     * iteration: it is multiplited by ageing factor powered by the number of
     * iterations since last revision.
     */
    public synchronized void revise(long iteration) {
        if (iAgeing == 1.0)
            return;
        iCounter *= Math.pow(iAgeing, iteration - iLastRevision);
        iLastRevision = iteration;
    }

    /**
     * Combine two integers (for hash code)
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
        if (o == null || !(o instanceof Assignment<?>))
            return false;
        return ((Assignment<?>) o).getValue().equals(getValue());
    }

    /** String representation */
    @Override
    public String toString() {
        return toString(0l, true);
    }

    /** String representation (e.g., 10x A := a) */
    public String toString(long iteration, boolean assignment) {
        return (assignment ? getCounter(iteration) + "x " : "") + getValue().variable().getName()
                + (assignment ? " := " : " != ") + getValue().getName();
    }

    /** Compare two assignments (their counters) */
    public int compareTo(long iteration, Assignment<T> a) {
        int cmp = getValue().variable().getName().compareTo(a.getValue().variable().getName());
        if (cmp != 0)
            return cmp;
        if (getCounter(iteration) != a.getCounter(iteration))
            return (getCounter(iteration) < a.getCounter(iteration) ? 1 : -1);
        return getValue().getName().compareTo(a.getValue().getName());
    }

    /** Assignment comparator */
    public static class AssignmentComparator<E extends Value<?, E>> implements Comparator<Assignment<E>> {
        private long iIteration;

        public AssignmentComparator(long iteration) {
            iIteration = iteration;
        }

        public int compare(Assignment<E> a1, Assignment<E> a2) {
            return a1.compareTo(iIteration, a2);
        }
    }
}
