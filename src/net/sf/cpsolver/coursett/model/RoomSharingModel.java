package net.sf.cpsolver.coursett.model;

import java.util.HashMap;

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
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */
public class RoomSharingModel {
	protected int iStep = 1;
    protected Long[][] iPreference = null;
    protected Long[] iDepartmentIds = null;
    protected HashMap<Long, Integer> iDepartmentIdx = null;

    public static Long sFreeForAllPref = new Long(-1);
    public static Long sNotAvailablePref = new Long(-2);
    public static char sFreeForAllPrefChar = '*';
    public static char sNotAvailablePrefChar = '#';

    public static Long sDefaultPref = sFreeForAllPref;
    public static char sDefaultPrefChar = sFreeForAllPrefChar;

    public char iFreeForAllPrefChar = sFreeForAllPrefChar;
    public char iNotAvailablePrefChar = sNotAvailablePrefChar;

    protected RoomSharingModel(int step) {
    	iStep = step;
    }
    
    protected RoomSharingModel() {
        this(6);
    }

    public RoomSharingModel(int step, Long[] managerIds, String pattern, Character freeForAllPrefChar, Character notAvailablePrefChar) {
    	iStep = step;
        iPreference = new Long[getNrDays()][getNrTimes()];
        iDepartmentIds = new Long[managerIds.length];
        iDepartmentIdx = new HashMap<Long, Integer>();
        for (int i = 0; i < managerIds.length; i++) {
            iDepartmentIds[i] = managerIds[i];
            iDepartmentIdx.put(managerIds[i], i);
        }
        if (freeForAllPrefChar != null)
            iFreeForAllPrefChar = freeForAllPrefChar;
        if (notAvailablePrefChar != null)
            iNotAvailablePrefChar = notAvailablePrefChar;

        setPreferences(pattern);
    }
    
    public char getFreeForAllPrefChar() { return iFreeForAllPrefChar; }
    public void setFreeForAllPrefChar(char c) { iFreeForAllPrefChar = c; }

    public char getNotAvailablePrefChar() { return iNotAvailablePrefChar; }
    public void setNotAvailablePrefChar(char c) { iNotAvailablePrefChar = c; }

    public boolean isFreeForAll(int day, int time) {
        return sFreeForAllPref.equals(iPreference[day][time]);
    }

    public boolean isFreeForAll(int slot) {
        int day = slot / Constants.SLOTS_PER_DAY;
        int time = (slot % Constants.SLOTS_PER_DAY) / getStep();
        return sFreeForAllPref.equals(iPreference[day][time]);
    }

    public boolean isNotAvailable(int day, int time) {
        return sNotAvailablePref.equals(iPreference[day][time]);
    }

    public boolean isNotAvailable(int slot) {
        int day = slot / Constants.SLOTS_PER_DAY;
        int time = (slot % Constants.SLOTS_PER_DAY) / getStep();
        return sNotAvailablePref.equals(iPreference[day][time]);
    }

    public boolean isAvailable(TimeLocation timeLocation, Long departmentId) {
        for (int d = 0; d < Constants.NR_DAYS; d++) {
            if ((Constants.DAY_CODES[d] & timeLocation.getDayCode()) == 0)
                continue;
            int startTime = timeLocation.getStartSlot() / getStep();
            int endTime = (timeLocation.getStartSlot() + timeLocation.getLength() - 1) / getStep();
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
        int time = (slot % Constants.SLOTS_PER_DAY) / getStep();
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
                    sb.append(getFreeForAllPrefChar());
                else if (iPreference[d][t].equals(sNotAvailablePref))
                    sb.append(getNotAvailablePrefChar());
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
                    char pref = (pattern != null && idx < pattern.length() ? pattern.charAt(idx) : getFreeForAllPrefChar());
                    idx++;
                    if (pref == getNotAvailablePrefChar()) {
                        iPreference[d][t] = sNotAvailablePref;
                    } else if (pref == getFreeForAllPrefChar()) {
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
        return Constants.SLOTS_PER_DAY / getStep();
    }
    
    public int getStep() {
        return iStep;
    }
}