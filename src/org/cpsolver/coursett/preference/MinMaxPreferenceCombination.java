package org.cpsolver.coursett.preference;

/**
 * Min-max preference combination. <br>
 * <ul>
 * <li>If at least one preference is required &rarr; required
 * <li>If at least one preference is prohibited &rarr; prohibited
 * <li>If max&gt;-min &rarr; max
 * <li>If -min&gt;max &rarr; min
 * <li>Otherwise &rarr; 0
 * </ul>
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
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
 */
public class MinMaxPreferenceCombination extends PreferenceCombination {
    int iPreferenceMin = 0;
    int iPreferenceMax = 0;
    
    public MinMaxPreferenceCombination() {}
    
    public MinMaxPreferenceCombination(MinMaxPreferenceCombination c) {
        super(c);
        iPreferenceMin = c.iPreferenceMin;
        iPreferenceMax = c.iPreferenceMax;
    }

    @Override
    public void addPreferenceInt(int intPref) {
        super.addPreferenceInt(intPref);
        iPreferenceMax = Math.max(iPreferenceMax, intPref);
        iPreferenceMin = Math.min(iPreferenceMin, intPref);
    }

    @Override
    public int getPreferenceInt() {
        return (iPreferenceMax > -iPreferenceMin ? iPreferenceMax : -iPreferenceMin > iPreferenceMax ? iPreferenceMin
                : iPreferenceMax);
    }
    
    @Override
    public PreferenceCombination clonePreferenceCombination() { return new MinMaxPreferenceCombination(this); }
}
