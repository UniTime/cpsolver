package org.cpsolver.instructor.model;

import org.cpsolver.coursett.Constants;

/**
 * A preference. This class encapsulates a time, course, instructor, or attribute preference.
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Instructor Sectioning)<br>
 *          Copyright (C) 2016 Tomas Muller<br>
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
public class Preference<T> {
    protected T iTarget;
    protected int iPreference;

    /**
     * Constructor
     * @param target object for which the preference is given
     * @param preference preference value
     */
    public Preference(T target, int preference) {
        iTarget = target;
        iPreference = preference;
    }
    
    /**
     * Is required?
     * @return true if the preference is required
     */
    public boolean isRequired() { return iPreference < Constants.sPreferenceLevelRequired / 2; }

    /**
     * Is prohibited?
     * @return true if the preference is prohibited
     */
    public boolean isProhibited() { return iPreference > Constants.sPreferenceLevelProhibited / 2; }
    
    /**
     * Preference value
     * @return preference value
     */
    public int getPreference() { return iPreference; }
    
    /**
     * Target object for which the preference is given
     * @return target
     */
    public T getTarget() { return iTarget; }
    
    @Override
    public String toString() { return getTarget() + ": " + (isRequired() ? "R" : isProhibited() ? "P" : getPreference()); }
}
