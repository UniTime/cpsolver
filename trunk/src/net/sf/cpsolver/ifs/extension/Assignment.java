package net.sf.cpsolver.ifs.extension;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Value;

/**
 * This class describing an assignment of a value to a variable together with a counter (used by CBS).
 *
 * Counter also supports ageing: the counter is multiplied by aging factor for each iteration.
 *
 * @version
 * IFS 1.1 (Iterative Forward Search)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@ktiml.mff.cuni.cz">muller@ktiml.mff.cuni.cz</a><br>
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
public class Assignment {
    private Value iValue;
    private double iCounter = 1.0;
    private long iLastRevision;
    private double iAgeing = 1.0;
    private Constraint iConstraint = null;
    
    /** Constructor
     * @param iteration current iteration
     * @param value value
     * @param ageing ageing factor
     */
    public Assignment(long iteration, Value value, double ageing) {
        iValue = value;
        iLastRevision = iteration;
        iAgeing = ageing;
    }
    
    /** Returns value */
    public Value getValue() {
        return iValue;
    }
    
    /** Increments counter
     * @param iteration current iteration
     */
    public void incCounter(long iteration) {
        revise(iteration);
        iCounter += 1.0;
    }
    
    /** Set counter
     * @param cnt new value
     */
    public void setCounter(double cnt) {
        iCounter = cnt;
    }
    
    /** Get counter
     * @param iteration current iteration
     */
    public double getCounter(long iteration) {
        if (iteration == 0l)
            return iCounter;
        if (iAgeing == 1.0)
            return iCounter;
        return iCounter * Math.pow(iAgeing, iteration - iLastRevision);
    }
    
    /** Returns constraint */
    public Constraint getConstraint() {
        return iConstraint;
    }
    /** Sets constraint */
    public void setConstraint(Constraint constraint) {
        iConstraint = constraint;
    }
    
    /** Revise counter. If ageing is used, counter is adopted to the current iteration: it is multiplited by ageing factor powered by the number of iterations since last revision.
     */
    public synchronized void revise(long iteration) {
        if (iAgeing == 1.0)
            return;
        iCounter *= Math.pow(iAgeing, iteration - iLastRevision);
        iLastRevision = iteration;
    }
    
    /** Combine two integers (for hash code)
     */
    public static int combine(int a, int b) {
        int ret = 0;
        for (int i = 0; i < 15; i++)
            ret = ret | ((a & (1 << i)) << i) | ((b & (1 << i)) << (i + 1));
        return ret;
    }
    
    public int hashCode() {
        return iValue.hashCode();
    }
    
    public boolean equals(Object o) {
        try {
            Assignment a = (Assignment)o;
            return a.getValue().getId() == getValue().getId();
        }
        catch (Exception e) {
            return false;
        }
    }
    
    /** Returns comparator of assignments */
    public static java.util.Comparator getComparator(long iteration) {
        return new AssignmentComparator(iteration);
    }
    
    /** String representation */
    public String toString() {
        return toString(0l, true);
    }
    
    /** String representation (e.g., 10x A := a)*/
    public String toString(long iteration, boolean assignment) {
        return (assignment ? getCounter(iteration) + "x " : "")
            + getValue().variable().getName()
            + (assignment ? " := " : " != ")
            + getValue().getName();
    }
    
    /** Compare two assignments (their counters) */
    public int compareTo(long iteration, Assignment a) {
        int cmp =
        getValue().variable().getName().compareTo(
        a.getValue().variable().getName());
        if (cmp != 0)
            return cmp;
        if (getCounter(iteration) != a.getCounter(iteration))
            return (getCounter(iteration) < a.getCounter(iteration) ? 1 : -1);
            return getValue().getName().compareTo(a.getValue().getName());
    }
    
    /** Assignment comparator */
    public static class AssignmentComparator implements java.util.Comparator {
        private long iIteration;
        public AssignmentComparator(long iteration) {
            iIteration = iteration;
        }
        public int compare(Object o1, Object o2) {
            if (o1 == null
            || o2 == null
            || !(o1 instanceof Assignment)
            || !(o2 instanceof Assignment))
                return 0;
            return ((Assignment)o1).compareTo(iIteration, (Assignment)o2);
        }
    }
}
