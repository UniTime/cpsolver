package org.cpsolver.coursett.model;

import java.util.BitSet;
import java.util.Enumeration;

import org.cpsolver.coursett.Constants;
import org.cpsolver.ifs.util.ToolBox;


/**
 * Time part of placement.
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

public class TimeLocation {
    private int iStartSlot;

    private int iPreference;
    private double iNormalizedPreference;

    private Long iTimePatternId = null;
    private int iHashCode;

    private int iDayCode;
    private int iLength;
    private int iNrMeetings;
    private int iBreakTime;

    private BitSet iWeekCode;
    private Long iDatePatternId = null;
    private String iDatePatternName = null;
    private int iDatePreference;

    /**
     * Constructor
     * 
     * @param dayCode
     *            days (combination of 1 for Monday, 2 for Tuesday, ...)
     * @param startTime
     *            start slot
     * @param length
     *            number of slots
     * @param pref
     *            time preference
     * @param normPref normalized preference
     * @param datePatternPreference date pattern preference
     * @param datePatternId date pattern unique id
     * @param datePatternName date pattern name
     * @param weekCode date pattern (binary string with 1 for each day when classes take place)
     * @param breakTime break time in minutes
     */
    public TimeLocation(int dayCode, int startTime, int length, int pref, double normPref, int datePatternPreference,
            Long datePatternId, String datePatternName, BitSet weekCode, int breakTime) {
        iPreference = pref;
        iNormalizedPreference = normPref;
        iStartSlot = startTime;
        iDayCode = dayCode;
        iLength = length;
        iBreakTime = breakTime;
        iNrMeetings = 0;
        for (int i = 0; i < Constants.DAY_CODES.length; i++) {
            if ((iDayCode & Constants.DAY_CODES[i]) == 0)
                continue;
            iNrMeetings++;
        }
        iHashCode = combine(combine(iDayCode, iStartSlot), iLength);
        iDatePatternName = datePatternName;
        iWeekCode = weekCode;
        iDatePatternId = datePatternId;
        if (iDatePatternName == null)
            iDatePatternName = "not set";
        iDatePreference = datePatternPreference;
        if (iWeekCode == null) {
            iWeekCode = new BitSet(366);
            for (int i = 0; i <= 365; i++)
                iWeekCode.set(i);
        }
    }
    
    public TimeLocation(int dayCode, int startTime, int length, int pref, double normPref, Long datePatternId,
            String datePatternName, BitSet weekCode, int breakTime) {
        this(dayCode, startTime, length, pref, normPref, 0, datePatternId, datePatternName, weekCode, breakTime);
    }

    /** Number of meetings 
     * @return number of meetings
     **/
    public int getNrMeetings() {
        return iNrMeetings;
    }

    public int getBreakTime() {
        return iBreakTime;
    }

    public void setBreakTime(int breakTime) {
        iBreakTime = breakTime;
    }

    private static int combine(int a, int b) {
        int ret = 0;
        for (int i = 0; i < 15; i++)
            ret = ret | ((a & (1 << i)) << i) | ((b & (1 << i)) << (i + 1));
        return ret;
    }

    /** Days (combination of 1 for Monday, 2 for Tuesday, ...) 
     * @return days of the week of this time
     **/
    public int getDayCode() {
        return iDayCode;
    }

    /** Days for printing purposes 
     * @return day header (e.g., MWF for Monday - Wednesday - Friday time)
     **/
    public String getDayHeader() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < Constants.DAY_CODES.length; i++)
            if ((iDayCode & Constants.DAY_CODES[i]) != 0)
                sb.append(Constants.DAY_NAMES_SHORT[i]);
        return sb.toString();
    }

    /** Start time for printing purposes
     * @param useAmPm use 12-hour format 
     * @return time header (e.g., 7:30a)
     **/
    public String getStartTimeHeader(boolean useAmPm) {
        int min = iStartSlot * Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN;
        int h = min / 60;
        int m = min % 60;
        if (useAmPm)
            return (h > 12 ? h - 12 : h) + ":" + (m < 10 ? "0" : "") + m + (h >= 12 ? "p" : "a");
        else
            return h + ":" + (m < 10 ? "0" : "") + m;
    }
    
    /** Start time for printing purposes 
     * @return time header (e.g., 7:30a)
     **/
    @Deprecated
    public String getStartTimeHeader() {
        return getStartTimeHeader(true);
    }

    /** End time for printing purposes 
     * @param useAmPm use 12-hour format
     * @return end time (e.g., 8:20a)
     **/
    public String getEndTimeHeader(boolean useAmPm) {
        int min = (iStartSlot + iLength) * Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN - getBreakTime();
        int m = min % 60;
        int h = min / 60;
        if (useAmPm)
            return (h > 12 ? h - 12 : h) + ":" + (m < 10 ? "0" : "") + m + (h >= 12 ? "p" : "a");
        else
            return h + ":" + (m < 10 ? "0" : "") + m;
    }
    
    /** End time for printing purposes 
     * @return end time (e.g., 8:20a)
     **/
    @Deprecated
    public String getEndTimeHeader() {
        return getEndTimeHeader(true);
    }


    /** End time for printing purposes 
     * @param useAmPm use 12-hour format
     * @return end time not counting break time (e.g., 8:30a)
     **/
    public String getEndTimeHeaderNoAdj(boolean useAmPm) {
        int min = (iStartSlot + iLength) * Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN;
        int m = min % 60;
        int h = min / 60;
        if (useAmPm)
            return (h > 12 ? h - 12 : h) + ":" + (m < 10 ? "0" : "") + m + (h >= 12 ? "p" : "a");
        else
            return h + ":" + (m < 10 ? "0" : "") + m;
    }

    /** End time for printing purposes 
     * @return end time not counting break time (e.g., 8:30a)
     **/
    @Deprecated
    public String getEndTimeHeaderNoAdj() {
        return getEndTimeHeaderNoAdj(true);
    }
    
    /** Start slot
     * @return start slot
     **/
    public int getStartSlot() {
        return iStartSlot;
    }

    /** Used slots in a day (combination of 1..first, 2..second,...) */

    /** true if days overlap
     * @param anotherLocation another time
     * @return true if days of the week overlaps
     **/
    public boolean shareDays(TimeLocation anotherLocation) {
        return ((iDayCode & anotherLocation.iDayCode) != 0);
    }

    /** number of overlapping days 
     * @param anotherLocation another time
     * @return number of days of the week that the two times share
     **/
    public int nrSharedDays(TimeLocation anotherLocation) {
        int ret = 0;
        for (int i = 0; i < Constants.NR_DAYS; i++) {
            if ((iDayCode & Constants.DAY_CODES[i]) == 0)
                continue;
            if ((anotherLocation.iDayCode & Constants.DAY_CODES[i]) == 0)
                continue;
            ret++;
        }
        return ret;
    }

    /** true if hours overlap 
     * @param anotherLocation another time
     * @return true if the two times overlap in time (just the time of the day is considered)
     **/
    public boolean shareHours(TimeLocation anotherLocation) {
        return (iStartSlot + iLength > anotherLocation.iStartSlot)
                && (anotherLocation.iStartSlot + anotherLocation.iLength > iStartSlot);
    }

    /** number of overlapping time slots (ignoring days) 
     * @param anotherLocation another time
     * @return number of time slots the two location overlap
     **/
    public int nrSharedHours(TimeLocation anotherLocation) {
        int end = Math.min(iStartSlot + iLength, anotherLocation.iStartSlot + anotherLocation.iLength);
        int start = Math.max(iStartSlot, anotherLocation.iStartSlot);
        return (end < start ? 0 : end - start);
    }

    /** true if weeks overlap
     * @param anotherLocation another time
     * @return true if the date patterns overlap
     */
    public boolean shareWeeks(TimeLocation anotherLocation) {
        return iWeekCode.intersects(anotherLocation.iWeekCode);
    }

    /** true if weeks overlap
     * @param weekCode another date pattern
     * @return true if the date patterns overlap
     */
    public boolean shareWeeks(BitSet weekCode) {
        return iWeekCode.intersects(weekCode);
    }

    public boolean hasDay(int day) {
        return iWeekCode.get(day);
    }

    /** true if overlap 
     * @param anotherLocation another time
     * @return true if the two times overlap, this means that all three checks {@link TimeLocation#shareDays(TimeLocation)}, {@link TimeLocation#shareHours(TimeLocation)} and {@link TimeLocation#shareWeeks(TimeLocation)} are true.
     **/
    public boolean hasIntersection(TimeLocation anotherLocation) {
        return shareDays(anotherLocation) && shareHours(anotherLocation) && shareWeeks(anotherLocation);
    }

    /** Used slots 
     * @return enumeration of used slots
     **/
    public IntEnumeration getSlots() {
        return new SlotsEnum();
    }

    /** Used start slots (for each meeting) 
     * @return enumeration of start slots for each meeting of the time
     **/
    public IntEnumeration getStartSlots() {
        return new StartSlotsEnum();
    }

    /** Days 
     * @return enumeration of days of week of the time
     **/
    public IntEnumeration getDays() {
        return new DaysEnum();
    }

    private int[] iDaysCache = null;
    public int[] getDaysArray() {
        if (iDaysCache == null) {
            iDaysCache = new int[getNrMeetings()];
            int i = 0;
            for (Enumeration<Integer> e = getDays(); e.hasMoreElements();)
                iDaysCache[i++] = e.nextElement();
        }
        return iDaysCache;
    }

    /** Text representation 
     * @param useAmPm 12-hour format
     * @return time name (e.g., MWF 7:30a)
     **/
    public String getName(boolean useAmPm) {
        return getDayHeader() + " " + getStartTimeHeader(useAmPm);
    }
    
    @Deprecated
    public String getName() {
        return getName(true);
    }

    public String getLongName(boolean useAmPm) {
        return getDayHeader() + " " + getStartTimeHeader(useAmPm) + " - " + getEndTimeHeader(useAmPm) + " " + getDatePatternName();
    }
    
    @Deprecated
    public String getLongName() {
        return getLongName(true);
    }

    public String getLongNameNoAdj(boolean useAmPm) {
        return getDayHeader() + " " + getStartTimeHeader(useAmPm) + " - " + getEndTimeHeaderNoAdj(useAmPm) + " " + getDatePatternName();
    }
    
    public String getLongNameNoAdj() {
        return getLongNameNoAdj(true);
    }

    /** Preference 
     * @return time preference
     **/
    public int getPreference() {
        return iPreference;
    }

    public void setPreference(int preference) {
        iPreference = preference;
    }

    /** Length 
     * @return time length (in the number of slots)
     **/
    public int getLength() {
        return iLength;
    }

    /** Length 
     * @return time length (in the number of slots)
     **/
    public int getNrSlotsPerMeeting() {
        return iLength;
    }

    /** Normalized preference 
     * @return normalized preference
     **/
    public double getNormalizedPreference() {
        return iNormalizedPreference;
    }

    public void setNormalizedPreference(double normalizedPreference) {
        iNormalizedPreference = normalizedPreference;
    }

    /** Time pattern model (can be null) 
     * @return time pattern unique id
     **/
    public Long getTimePatternId() {
        return iTimePatternId;
    }

    public Long getDatePatternId() {
        return iDatePatternId;
    }

    public void setTimePatternId(Long timePatternId) {
        iTimePatternId = timePatternId;
    }

    public BitSet getWeekCode() {
        return iWeekCode;
    }

    public String getDatePatternName() {
        return iDatePatternName;
    }

    public void setDatePattern(Long datePatternId, String datePatternName, BitSet weekCode) {
        iDatePatternId = datePatternId;
        iDatePatternName = datePatternName;
        iWeekCode = weekCode;
    }
    
    public int getDatePatternPreference() {
        return iDatePreference;
    }

    @Override
    public String toString() {
        return getName() + " (" + iNormalizedPreference + ")";
    }

    @Override
    public int hashCode() {
        return iHashCode;
    }

    private class StartSlotsEnum implements IntEnumeration {
        int day = -1;
        boolean hasNext = false;

        private StartSlotsEnum() {
            hasNext = nextDay();
        }

        boolean nextDay() {
            do {
                day++;
                if (day >= Constants.DAY_CODES.length)
                    return false;
            } while ((Constants.DAY_CODES[day] & iDayCode) == 0);
            return true;
        }

        @Override
        public boolean hasMoreElements() {
            return hasNext;
        }

        @Override
        public Integer nextElement() {
            int slot = (day * Constants.SLOTS_PER_DAY) + iStartSlot;
            hasNext = nextDay();
            return slot;
        }
        
        @Deprecated
        @Override
        public Integer nextInt() {
            return nextElement();
        }
    }

    private class DaysEnum extends StartSlotsEnum {
        private DaysEnum() {
            super();
        }

        @Override
        public Integer nextElement() {
            int ret = day;
            hasNext = nextDay();
            return ret;
        }
    }

    private class SlotsEnum extends StartSlotsEnum {
        int pos = 0;

        private SlotsEnum() {
            super();
        }

        private boolean nextSlot() {
            if (pos + 1 < iLength) {
                pos++;
                return true;
            }
            if (nextDay()) {
                pos = 0;
                return true;
            }
            return false;
        }

        @Override
        public Integer nextElement() {
            int slot = (day * Constants.SLOTS_PER_DAY) + iStartSlot + pos;
            hasNext = nextSlot();
            return slot;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof TimeLocation))
            return false;
        TimeLocation t = (TimeLocation) o;
        if (getStartSlot() != t.getStartSlot())
            return false;
        if (getLength() != t.getLength())
            return false;
        if (getDayCode() != t.getDayCode())
            return false;
        return ToolBox.equals(getTimePatternId(), t.getTimePatternId())
                && ToolBox.equals(getDatePatternId(), t.getDatePatternId());
    }

    public int getNrWeeks() {
        return getNrWeeks(0, iWeekCode.size() - 1);
    }

    public int getNrWeeks(int startDay, int endDay) {
        /*
         * BitSet x = new BitSet(1+(endDay-startDay)/Constants.NR_DAYS); for
         * (int i=iWeekCode.nextSetBit(startDay); i<=endDay && i>=0;
         * i=iWeekCode.nextSetBit(i+1)) x.set((i-startDay)/Constants.NR_DAYS);
         * return x.cardinality();
         */
        int card = iWeekCode.get(startDay, endDay).cardinality();
        if (card == 0)
            return 0;
        if (card <= 7)
            return 1;
        return (5 + card) / 6;
    }
    
    public interface IntEnumeration extends Enumeration<Integer> {
        @Deprecated
        public Integer nextInt();
    }
    
    private Integer iFirstMeeting = null;
    public int getFirstMeeting(int dayOfWeekOffset) {
        if (iFirstMeeting == null) {
            int idx = -1;
            while ((idx = getWeekCode().nextSetBit(1 + idx)) >= 0) {
                int dow = (idx + dayOfWeekOffset) % 7;
                if ((getDayCode() & Constants.DAY_CODES[dow]) != 0) break;
            }
            iFirstMeeting = idx;
        }
        return iFirstMeeting;
    }
    
    private Integer iLastMeeting = null;
    public int getLastMeeting(int dayOfWeekOffset) {
        if (iLastMeeting == null) {
            int idx = -1;
            while ((idx = getWeekCode().nextSetBit(1 + idx)) >= 0) {
                int dow = (idx + dayOfWeekOffset) % 7;
                if ((getDayCode() & Constants.DAY_CODES[dow]) != 0)
                    iLastMeeting = idx;
            }
        }
        return iLastMeeting;
    }
    
    /** List dates when this time location meets. 
     * @return enumeration of dates of this time (indexes to the {@link TimeLocation#getWeekCode()} for matching days of the week)
     **/
    public IntEnumeration getDates(int dayOfWeekOffset) {
        return new DateEnum(dayOfWeekOffset);
    }
    
    /**
     * Check if the given time location has a particular date
     * @param date a date, expressed as an index to the {@link TimeLocation#getWeekCode()} 
     * @param dayOfWeekOffset day of the week offset for the weeks pattern
     * @return true if this time location is meeting on the given date
     */
    public boolean hasDate(int date, int dayOfWeekOffset) {
        if (date < 0) return false;
        if (getWeekCode().get(date)) {
            int dow = (date + dayOfWeekOffset) % 7;
            if ((getDayCode() & Constants.DAY_CODES[dow]) != 0) return true;
        }
        return false;
    }
    
    /**
     * Check if the time location has the given date, identified by a day of the week and a week pattern
     * @param dayOfWeek day of the week
     * @param week week pattern, or null when only days of the week are to be checked
     * @param dayOfWeekOffset day of the week offset for the weeks pattern
     * @return true if this time location is meeting on the given date
     */
    public boolean hasDate(int dayOfWeek, BitSet week, int dayOfWeekOffset) {
        // check the day of the week
        if ((getDayCode() & Constants.DAY_CODES[dayOfWeek]) == 0) return false;
        if (week == null) {
            // no week -> just day code check is sufficient
            return true;
        } else {
            // has week -> check the week code
            // first date in the week
            int firstDate = week.nextSetBit(0);
            int dow = (firstDate + dayOfWeekOffset) % 7; // 5
            // adjustments the given day of the week
            int adj = (7 - dow + dayOfWeek) % 7;
            return week.get(firstDate + adj) && getWeekCode().get(firstDate + adj);
        }
    }
    
    /**
     * Check if the time location has at least one date from a set identified by a day code and a bit set.
     * Precise computation of individual dates are used instead of just checking whether the day codes and week codes are overlapping.
     * @param dayCode day codes
     * @param weekCode week code
     * @param dayOfWeekOffset day of the week offset for the weeks pattern
     * @return true if there is at least one overlapping date
     */
    public boolean overlaps(int dayCode, BitSet weekCode, int dayOfWeekOffset) {
        // check day code
        if ((getDayCode() & dayCode) == 0) return false;
        if (weekCode == null) {
            // no week -> just day code check is sufficient
            return true;
        } else {
            // has week -> check the week code
            int idx = -1;
            while ((idx = weekCode.nextSetBit(1 + idx)) >= 0) {
                // iterate over all dates of the date pattern
                int dow = (idx + dayOfWeekOffset) % 7;
                if ((dayCode & Constants.DAY_CODES[dow]) == 0) continue;
                // check if this date is in the current time location
                if ((getDayCode() & Constants.DAY_CODES[dow]) != 0 && getWeekCode().get(idx))
                    return true;  
            }
            return false;
        }
    }
    
    /**
     * Count how many times this time location is meeting
     * @param dayOfWeekOffset day of the week offset for the weeks pattern
     * @return number of dates during which this time location is meeting
     */
    public int countDates(int dayOfWeekOffset) {
        int idx = -1;
        int count = 0;
        while ((idx = getWeekCode().nextSetBit(1 + idx)) >= 0) {
            int dow = (idx + dayOfWeekOffset) % 7;
            if ((getDayCode() & Constants.DAY_CODES[dow]) != 0) count++;
        }
        return count;
    }
    
    private class DateEnum implements IntEnumeration {
        int dayOfWeekOffset = 0;
        int nextDate = -1;
        boolean hasNext = false;

        private DateEnum(int dayOfWeekOffset) {
            this.dayOfWeekOffset = dayOfWeekOffset;
            hasNext = nextDate();
        }

        boolean nextDate() {
            while (true) {
                nextDate = getWeekCode().nextSetBit(1 + nextDate);
                if (nextDate < 0) return false;
                int dow = (nextDate + dayOfWeekOffset) % 7;
                if ((getDayCode() & Constants.DAY_CODES[dow]) != 0) return true;
            }
        }

        @Override
        public boolean hasMoreElements() {
            return hasNext;
        }

        @Override
        public Integer nextElement() {
            int ret = nextDate;
            hasNext = nextDate();
            return ret;
        }
        
        @Deprecated
        @Override
        public Integer nextInt() {
            return nextElement();
        }
    }
}
