package net.sf.cpsolver.coursett;

/**
 * Course Timetabling common constants. <br>
 * <br>
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 */
public class Constants extends net.sf.cpsolver.ifs.Constants {
    /** Number of slots per day */
    public static final int SLOTS_PER_DAY = 288;

    /** Day codes to combine several days into one int */
    public static int DAY_CODES[] = new int[] { 64, 32, 16, 8, 4, 2, 1 };
    /** All days */
    public static int DAY_CODE_ALL = 127;
    /** All week days */
    public static int DAY_CODE_WEEK = 124;

    /** Length of a single slot in minutes */
    public static int SLOT_LENGTH_MIN = 5;

    /** Start time of the first slot in minutes (from midnight) */
    public static int FIRST_SLOT_TIME_MIN = 0;

    /** Number of slots per day */
    public static int DAY_SLOTS_FIRST = (7 * 60 + 30) / 5; // day starts at 7:30

    /** Number of slots per day */
    public static int DAY_SLOTS_LAST = (17 * 60 + 30) / 5 - 1; // day ends at
                                                               // 17:30

    /** Number of slots per day w/o evening hours */
    public static int SLOTS_PER_DAY_NO_EVENINGS = DAY_SLOTS_LAST - DAY_SLOTS_FIRST + 1;

    /** Day names in short format M, T, W, Th, F, Sa, Su */
    public static String DAY_NAMES_SHORT[] = new String[] { "M", "T", "W", "Th", "F", "S", "Su" };

    /** Number of days */
    public static int NR_DAYS = DAY_CODES.length;

    /** Number of days of week (excludes weekend) */
    public static int NR_DAYS_WEEK = 5;

    /** Preference: prohibited */
    public static final String sPreferenceProhibited = "P";
    /** Preference: required */
    public static final String sPreferenceRequired = "R";
    /** Preference: strongly discouraged */
    public static final String sPreferenceStronglyDiscouraged = "2";
    /** Preference: discouraged */
    public static final String sPreferenceDiscouraged = "1";
    /** Preference: preferred */
    public static final String sPreferencePreferred = "-1";
    /** Preference: strongly preferred */
    public static final String sPreferenceStronglyPreferred = "-2";
    /** Preference: neutral */
    public static final String sPreferenceNeutral = "0";

    /** Preference level: prohibited */
    public static final int sPreferenceLevelProhibited = 100;
    /** Preference level: required */
    public static final int sPreferenceLevelRequired = -100;
    /** Preference level: strongly discouraged */
    public static final int sPreferenceLevelStronglyDiscouraged = 4;
    /** Preference level: discouraged */
    public static final int sPreferenceLevelDiscouraged = 1;
    /** Preference level: preferred */
    public static final int sPreferenceLevelPreferred = -1;
    /** Preference level: strongly preferred */
    public static final int sPreferenceLevelStronglyPreferred = -4;
    /** Preference level: neutral */
    public static final int sPreferenceLevelNeutral = 0;

    /** Convert preference to preference level */
    public static int preference2preferenceLevel(String prologPref) {
        if (sPreferenceRequired.equals(prologPref))
            return sPreferenceLevelRequired;
        if (sPreferenceStronglyPreferred.equals(prologPref))
            return sPreferenceLevelStronglyPreferred;
        if (sPreferencePreferred.equals(prologPref))
            return sPreferenceLevelPreferred;
        if (sPreferenceDiscouraged.equals(prologPref))
            return sPreferenceLevelDiscouraged;
        if (sPreferenceStronglyDiscouraged.equals(prologPref))
            return sPreferenceLevelStronglyDiscouraged;
        if (sPreferenceProhibited.equals(prologPref))
            return sPreferenceLevelProhibited;
        return sPreferenceLevelNeutral;
    }

    /** Convert preference level to preference */
    public static String preferenceLevel2preference(int intPref) {
        if (intPref >= sPreferenceLevelProhibited / 2)
            return sPreferenceProhibited;
        if (intPref >= sPreferenceLevelStronglyDiscouraged)
            return sPreferenceStronglyDiscouraged;
        if (intPref > sPreferenceLevelNeutral)
            return sPreferenceDiscouraged;
        if (intPref <= sPreferenceLevelRequired / 2)
            return sPreferenceRequired;
        if (intPref <= sPreferenceLevelStronglyPreferred)
            return sPreferenceStronglyPreferred;
        if (intPref < sPreferenceLevelNeutral)
            return sPreferencePreferred;
        return sPreferenceNeutral;
    }

    /** Convert time (hour:minute) to time slot */
    public static int time2slot(int hour, int min) {
        return (hour * 60 + min - FIRST_SLOT_TIME_MIN) / SLOT_LENGTH_MIN;
    }
}
