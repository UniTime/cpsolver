package net.sf.cpsolver.ifs.heuristics;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * A general roulette wheel selection. An object is selected randomly,
 * proportionaly to the provided weight. This class also supports multiple
 * selections (it implements {@link Enumeration} interface).
 * 
 * <br>
 * <br>
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
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

public class RouletteWheelSelection<E> implements Enumeration<E> {
    private List<E> iAdepts = new ArrayList<E>();
    private List<Double> iPoints = new ArrayList<Double>();
    private double iTotalPoints = 0, iUsedPoints = 0;
    private int iFirst = 0;

    /**
     * Add an adept to the selection
     * 
     * @param adept
     *            an object
     * @param points
     *            object weight (more points, better chance to be selected)
     */
    public void add(E adept, double points) {
        iAdepts.add(adept);
        iPoints.add(points);
        iTotalPoints += points;
    }

    private void swap(int idx1, int idx2) {
        E a1 = iAdepts.get(idx1);
        E a2 = iAdepts.get(idx2);
        iAdepts.set(idx1, a2);
        iAdepts.set(idx2, a1);
        Double p1 = iPoints.get(idx1);
        Double p2 = iPoints.get(idx2);
        iPoints.set(idx1, p2);
        iPoints.set(idx2, p1);
    }

    /** Are there still some adepts that have not been yet selected */
    public boolean hasMoreElements() {
        return iFirst < iAdepts.size();
    }

    /**
     * Perform selection. An object is selected randomly with the probability
     * proportional to the provided weight. Each object can be selected only
     * once.
     */
    public E nextElement() {
        if (!hasMoreElements())
            return null;
        double rx = ToolBox.random() * iTotalPoints;

        int iIdx = iFirst;
        rx -= iPoints.get(iIdx);
        while (rx > 0 && iIdx + 1 < iAdepts.size()) {
            iIdx++;
            rx -= iPoints.get(iIdx);
        }

        E selectedObject = iAdepts.get(iIdx);
        double points = iPoints.get(iIdx);
        iTotalPoints -= points;
        iUsedPoints += points;
        swap(iFirst, iIdx);
        iFirst++;

        return selectedObject;
    }

    /** Number of objects in the set */
    public int size() {
        return iAdepts.size();
    }

    /** Total value of objects that were already returned by the selection. */
    public double getUsedPoints() {
        return iUsedPoints;
    }

    /** Total value of objects that are still in the selection. */
    public double getRemainingPoints() {
        return iTotalPoints;
    }

    /** Total value of objects that were added into the selection. */
    public double getTotalPoints() {
        return iTotalPoints + iUsedPoints;
    }
}
