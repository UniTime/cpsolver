package net.sf.cpsolver.coursett.preference;

/**
 * Sum preference combination. <br>
 * <ul>
 * <li>If at least one preference is required -> required
 * <li>If at least one preference is prohibited -> prohibited
 * <li>Otherwise, sum of the given preferences is returned
 * </ul>
 * 
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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 */
public class SumPreferenceCombination extends PreferenceCombination {
    int iPreference = 0;

    @Override
    public void addPreferenceInt(int intPref) {
        super.addPreferenceInt(intPref);
        iPreference += intPref;
    }

    @Override
    public int getPreferenceInt() {
        return iPreference;
    }
}
