package net.sf.cpsolver.coursett.preference;

import net.sf.cpsolver.coursett.Constants;

/**
 * Preference combination. <br>
 * <br>
 * A preference can be:
 * <ul>
 * <li>R .. required
 * <li>P .. prohibited
 * <li>number .. soft preference (smaller value is better)
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
public abstract class PreferenceCombination {
    boolean iIsRequired = false;
    boolean iIsProhibited = false;

    /** Add preference a preference */
    public void addPreferenceProlog(String prologPref) {
        addPreferenceInt(Constants.preference2preferenceLevel(prologPref));
    }

    /** Returns combined preference from the given preferences */
    public void addPreferenceInt(int intPref) {
        String prologPref = Constants.preferenceLevel2preference(intPref);
        if (Constants.sPreferenceRequired.equals(prologPref))
            iIsRequired = true;
        if (Constants.sPreferenceProhibited.equals(prologPref))
            iIsProhibited = true;
    }

    public boolean isRequired() {
        return iIsRequired && !iIsProhibited;
    }

    public boolean isProhibited() {
        return iIsProhibited;
    }

    public abstract int getPreferenceInt();

    public String getPreferenceProlog() {
        if (iIsProhibited)
            return Constants.sPreferenceProhibited;
        if (iIsRequired)
            return Constants.sPreferenceRequired;
        return Constants.preferenceLevel2preference(getPreferenceInt());
    }

    public static PreferenceCombination getDefault() {
        return new SumPreferenceCombination();
    }
}
