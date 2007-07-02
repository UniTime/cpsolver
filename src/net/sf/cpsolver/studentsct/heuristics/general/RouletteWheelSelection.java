package net.sf.cpsolver.studentsct.heuristics.general;

import java.util.Enumeration;
import java.util.Vector;

import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * A general roulette wheel selection.
 * An object is selected randomly, proportionaly to the provided weight.
 * This class also supports multiple selections (it implements {@link Enumeration} interface).
 *
 * <br><br>
 * 
 * @version
 * StudentSct 1.1 (Student Sectioning)<br>
 * Copyright (C) 2007 Tomas Muller<br>
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

public class RouletteWheelSelection implements Enumeration {
    private Vector iAdepts = new Vector(), iPoints = new Vector();
    private double iTotalPoints = 0;
    private int iFirst = 0;
    
    /** 
     * Add an adept to the selection
     * @param adept an object
     * @param points object weight (more points, better chance to be selected)
     */
    public void add(Object adept, double points) {
        iAdepts.add(adept);
        iPoints.add(new Double(points));
        iTotalPoints+=points;
    }
    
    private void swap(int idx1, int idx2) {
        Object a1 = iAdepts.elementAt(idx1);
        Object a2 = iAdepts.elementAt(idx2);
        iAdepts.setElementAt(a2, idx1);
        iAdepts.setElementAt(a1, idx2);
        Object p1 = iPoints.elementAt(idx1);
        Object p2 = iPoints.elementAt(idx2);
        iPoints.setElementAt(p2, idx1);
        iPoints.setElementAt(p1, idx2);
    }
    
    /** Are there still some adepts that have not been yet selected */
    public boolean hasMoreElements() {
        return iFirst<iAdepts.size();
    }
    
    /** Perform selection. An object is selected randomly with the probability proportional to the 
     * provided weight. Each object can be selected only once. 
     */
    public Object nextElement() {
        if (!hasMoreElements()) return null;
        double rx = ToolBox.random()*iTotalPoints;
        
        int iIdx = iFirst; rx -= ((Double)iPoints.elementAt(iIdx)).doubleValue();
        while (rx>0 && iIdx+1<iAdepts.size()) {
            iIdx++;
            rx -= ((Double)iPoints.elementAt(iIdx)).doubleValue();
        }
        
        Object selectedObject = iAdepts.elementAt(iIdx);
        iTotalPoints -= ((Double)iPoints.elementAt(iIdx)).doubleValue();
        swap(iFirst, iIdx);
        iFirst++;
        
        return selectedObject;
    }
    
    /** Number of objects in the set */
    public int size() {
        return iAdepts.size();
    }
}
