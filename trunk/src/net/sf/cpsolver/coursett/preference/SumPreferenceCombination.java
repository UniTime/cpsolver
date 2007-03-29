package net.sf.cpsolver.coursett.preference;

/**
 * Sum preference combination.
 * <br>
 * <ul>
 * <li>If at least one preference is required -> required
 * <li>If at least one preference is prohibited -> prohibited
 * <li>Otherwise, sum of the given preferences is returned
 * </ul>
 *
 * @version
 * CourseTT 1.1 (University Course Timetabling)<br>
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
public class SumPreferenceCombination extends PreferenceCombination {
    int iPreference = 0;
    
    public void addPreferenceInt(int intPref) {
    	super.addPreferenceInt(intPref);
    	iPreference += intPref;
    }
    
    public int getPreferenceInt() {
    	return iPreference;
    }
}
