package net.sf.cpsolver.coursett.heuristics;

import java.util.ArrayList;
import java.util.List;


/**
 * General hierarchical selection. <br>
 * <br>
 * We have implemented a hierarchical handling of the value selection criteria.
 * There are three levels of comparison. At each level a weighted sum of the
 * criteria described below is computed. Only solutions with the smallest sum
 * are considered in the next level. The weights express how quickly a complete
 * solution should be found. Only hard constraints are satisfied in the first
 * level sum. Distance from the initial solution (MPP), and a weighting of major
 * preferences (including time, classroom requirements and student conflicts),
 * are considered in the next level. In the third level, other minor criteria
 * are considered. In general, a criterion can be used in more than one level,
 * e.g., with different weights. <br>
 * <br>
 * The above sums order the values lexicographically: the best value having the
 * smallest first level sum, the smallest second level sum among values with the
 * smallest first level sum, and the smallest third level sum among these
 * values. As mentioned above, this allows diversification between the
 * importance of individual criteria. <br>
 * <br>
 * Furthermore, the value selection heuristics also support some limits (e.g.,
 * that all values with a first level sum smaller than a given percentage Pth
 * above the best value [typically 10%] will go to the second level comparison
 * and so on). This allows for the continued feasibility of a value near to the
 * best that may yet be much better in the next level of comparison. If there is
 * more than one solution after these three levels of comparison, one is
 * selected randomly. This approach helped us to significantly improve the
 * quality of the resultant solutions. <br>
 * <br>
 * In general, there can be more than three levels of these weighted sums,
 * however three of them seem to be sufficient for spreading weights of various
 * criteria for our problem.
 * 
 * @see PlacementSelection
 * @version CourseTT 1.2 (University Course Timetabling)<br>
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
public class HeuristicSelector<E> {
    private double[] iThreshKoef;
    private List<Element> iElements = new ArrayList<Element>();
    private double iBestValueZero = 0.0;

    /**
     * Constructor
     * 
     * @param threshKoef
     *            limit for each level, e.g., new double[] {0.1, 0.1, 0.1} for
     *            three level selection with 10% limit on each level
     */
    public HeuristicSelector(double[] threshKoef) {
        iThreshKoef = threshKoef;
    }

    /**
     * Adds an object to selection
     * 
     * @param values
     *            weighted sum for each level
     * @param object
     *            object to be returned if selected
     * @return true if added (it is not added if it is obvious that it cannot be
     *         selected)
     */
    public boolean add(double[] values, E object) {
        if (iElements.isEmpty() || values[0] < iBestValueZero) {
            iBestValueZero = values[0];
            iElements.add(new Element(values, object));
            return true;
        } else if (values[0] <= iBestValueZero * (iBestValueZero < 0.0 ? 1.0 - iThreshKoef[0] : 1.0 + iThreshKoef[0])) {
            iElements.add(new Element(values, object));
            return true;
        }
        return false;
    }

    public Double firstLevelThreshold() {
        return (iElements.isEmpty() ? null : new Double(iBestValueZero
                * (iBestValueZero < 0.0 ? 1.0 - iThreshKoef[0] : 1.0 + iThreshKoef[0])));
    }

    /**
     * Do the selection.
     * 
     * @return inserted objects which met the criteria
     */
    public List<Element> selection() {
        List<Element> selection = iElements;
        double bestValue = iBestValueZero;
        for (int level = 0; level < iThreshKoef.length; level++) {
            List<Element> x = new ArrayList<Element>(selection.size());
            double threshold = (bestValue < 0.0 ? 1.0 - iThreshKoef[level] : 1.0 + iThreshKoef[level]) * bestValue;
            // System.out.println("B"+(level+1)+": "+bestValue);
            // System.out.println("T"+(level+1)+": "+threshold);
            double nextBestValue = 0.0;
            boolean lastLevel = (level + 1 == iThreshKoef.length);
            for (Element element : selection) {
                if (element.getValue(level) <= threshold) {
                    if (!lastLevel && (x.isEmpty() || element.getValue(level + 1) < nextBestValue))
                        nextBestValue = element.getValue(level + 1);
                    x.add(element);
                }
            }
            selection = x;
            bestValue = nextBestValue;
        }
        return selection;
    }

    /** An element in heuristical selection */
    public class Element {
        private double[] iValues;
        private E iObject;

        private Element(double[] values, E object) {
            iValues = values;
            iObject = object;
        }

        /** weighted sum in each level */
        public double[] getValues() {
            return iValues;
        }

        /** weighted sum in the given level */
        public double getValue(int level) {
            return iValues[level];
        }

        /** given object */
        public E getObject() {
            return iObject;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < iValues.length; i++)
                sb.append(i == 0 ? "" : ",").append(iValues[i]);
            return "[" + sb + "]:" + iObject;
        }
    }
}
