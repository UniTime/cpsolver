package net.sf.cpsolver.coursett.model;

import java.util.Hashtable;

import net.sf.cpsolver.coursett.Constants;

/**
 * Room availability model.
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
public class RoomSharingModel {
    protected Long[][] iPreference = null;
    protected Long[] iDepartmentIds = null;
    protected Hashtable<Long, Integer> iDepartmentIdx = null;

    public static Long sFreeForAllPref = new Long(-1);
    public static Long sNotAvailablePref = new Long(-2);
    public static char sFreeForAllPrefChar = 'F';
    public static char sNotAvailablePrefChar = 'X';

    public static Long sDefaultPref = sFreeForAllPref;
    public static char sDefaultPrefChar = sFreeForAllPrefChar;

    protected RoomSharingModel() {
    }

    public RoomSharingModel(Long[] managerIds, String pattern) {
        iPreference = new Long[getNrDays()][getNrTimes()];
        iDepartmentIds = new Long[managerIds.length];
        iDepartmentIdx = new Hashtable<Long, Integer>();
        for (int i = 0; i < managerIds.length; i++) {
            iDepartmentIds[i] = managerIds[i];
            iDepartmentIdx.put(managerIds[i], i);
        }

        setPreferences(pattern);
    }

    public boolean isFreeForAll(int day, int time) {
        return iPreference[day][time] == sFreeForAllPref;
    }

    public boolean isFreeForAll(int slot) {
        int day = slot / Constants.SLOTS_PER_DAY;
        int time = (slot % Constants.SLOTS_PER_DAY) / 6;
        return iPreference[day][time] == sFreeForAllPref;
    }

    public boolean isNotAvailable(int day, int time) {
        return iPreference[day][time] == sNotAvailablePref;
    }

    public boolean isNotAvailable(int slot) {
        int day = slot / Constants.SLOTS_PER_DAY;
        int time = (slot % Constants.SLOTS_PER_DAY) / 6;
        return iPreference[day][time] == sNotAvailablePref;
    }

    public boolean isAvailable(TimeLocation timeLocation, Long departmentId) {
        for (int d = 0; d < Constants.NR_DAYS; d++) {
            if ((Constants.DAY_CODES[d] & timeLocation.getDayCode()) == 0)
                continue;
            int startTime = timeLocation.getStartSlot() / 6;
            int endTime = (timeLocation.getStartSlot() + timeLocation.getLength() - 1) / 6;
            for (int t = startTime; t <= endTime; t++) {
                Long pref = iPreference[d][t];
                if (pref.equals(sNotAvailablePref))
                    return false;
                if (pref.equals(sFreeForAllPref))
                    continue;
                if (departmentId != null && !departmentId.equals(pref))
                    return false;
            }
        }
        return true;
    }

    public Long getDepartmentId(int day, int time) {
        Long pref = iPreference[day][time];
        if (pref.equals(sFreeForAllPref) || pref.equals(sNotAvailablePref))
            return null;
        return pref;
    }

    public Long getDepartmentId(int slot) {
        int day = slot / Constants.SLOTS_PER_DAY;
        int time = (slot % Constants.SLOTS_PER_DAY) / 6;
        return getDepartmentId(day, time);
    }

    public Long[] getDepartmentIds() {
        return iDepartmentIds;
    }

    public int getNrDepartments() {
        return (iDepartmentIds == null ? 0 : iDepartmentIds.length);
    }

    public int getIndex(Long departmentId) {
        Integer idx = iDepartmentIdx.get(departmentId);
        if (idx == null)
            return -1;
        return idx.intValue();
    }

    public String getPreferences() {
        StringBuffer sb = new StringBuffer();
        for (int d = 0; d < getNrDays(); d++)
            for (int t = 0; t < getNrTimes(); t++) {
                if (iPreference[d][t].equals(sFreeForAllPref))
                    sb.append(sFreeForAllPrefChar);
                else if (iPreference[d][t].equals(sNotAvailablePref))
                    sb.append(sNotAvailablePrefChar);
                else
                    sb.append((char) ('0' + getIndex(iPreference[d][t])));
            }
        return sb.toString();
    }

    public void setPreferences(String pattern) {
        try {
            int idx = 0;
            for (int d = 0; d < getNrDays(); d++)
                for (int t = 0; t < getNrTimes(); t++) {
                    char pref = (pattern != null && idx < pattern.length() ? pattern.charAt(idx) : sDefaultPrefChar);
                    idx++;
                    if (pref == sNotAvailablePrefChar) {
                        iPreference[d][t] = sNotAvailablePref;
                    } else if (pref == sFreeForAllPrefChar) {
                        iPreference[d][t] = sFreeForAllPref;
                    } else {
                        iPreference[d][t] = iDepartmentIds[(pref - '0')];
                    }
                }
        } catch (NullPointerException e) {
        } catch (IndexOutOfBoundsException e) {
        }
    }

    public int getNrDays() {
        return Constants.NR_DAYS;
    }

    public int getNrTimes() {
        return Constants.SLOTS_PER_DAY / 6;
    }
}
