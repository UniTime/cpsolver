package org.cpsolver.coursett.preference;

/**
 * Average preference combination. <br>
 * <ul>
 * <li>If at least one preference is required &rarr; required
 * <li>If at least one preference is prohibited &rarr; prohibited
 * <li>Otherwise, mean value from the given preferences is returned
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
public class AveragePreferenceCombination extends PreferenceCombination {
    int iPreference = 0;
    int iCnt = 0;
    
    public AveragePreferenceCombination() {}
    
    public AveragePreferenceCombination(AveragePreferenceCombination c) {
        super(c);
        iPreference = c.iPreference;
        iCnt = c.iCnt;
    }

    @Override
    public void addPreferenceInt(int intPref) {
        super.addPreferenceInt(intPref);
        iPreference += intPref;
        iCnt++;
    }

    @Override
    public int getPreferenceInt() {
        return Math.round(((float) iPreference) / ((float) iCnt));
    }

    @Override
    public PreferenceCombination clonePreferenceCombination() { return new AveragePreferenceCombination(this); }
}
