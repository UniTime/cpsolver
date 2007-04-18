package net.sf.cpsolver.coursett.model;

import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Locale;

import net.sf.cpsolver.coursett.Constants;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Time part of placement.
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

public class TimeLocation {
	private static SimpleDateFormat sDateFormatShort = new SimpleDateFormat("MM/dd", Locale.US);
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
    
    /** Constructor
     * @param dayCode days (combination of 1 for Monday, 2 for Tuesday, ...)
     * @param startTime start slot
     * @param length number of slots
     * @param pref time preference
     */
    public TimeLocation(int dayCode, int startTime, int length, int pref, double normPref, Long datePatternId, String datePatternName, BitSet weekCode, int breakTime) {
        iPreference = pref;
        iNormalizedPreference = normPref;
        iStartSlot = startTime;
        iDayCode = dayCode;
        iLength = length;
        iBreakTime = breakTime;
        iNrMeetings = 0;
        for (int i=0;i<Constants.DAY_CODES.length;i++) {
            if ((iDayCode & Constants.DAY_CODES[i])==0) continue;
            iNrMeetings++;
        }
        iHashCode = combine(combine(iDayCode, iStartSlot),iLength);
        iDatePatternName = datePatternName;
        iWeekCode = weekCode;
        iDatePatternId = datePatternId;
        if (iDatePatternName==null) iDatePatternName = "not set";
        if (iWeekCode==null) {
        	iWeekCode = new BitSet(366);
        	for (int i=0;i<=365;i++)
        		iWeekCode.set(i);
        }
    }
    
    /** Number of meetings */
    public int getNrMeetings() {
        return iNrMeetings;
    }
    
    public int getBreakTime() {
        return iBreakTime;
    }

    private static int combine(int a, int b) {
        int ret = 0;
        for (int i=0;i<15;i++) ret = ret | ((a & (1<<i))<<i) | ((b & (1<<i))<<(i+1));
        return ret;
    }
    
    /** Days (combination of 1 for Monday, 2 for Tuesday, ...) */
    public int getDayCode() { return iDayCode; }
    /** Days for printing purposes */
    public String getDayHeader() { 
        StringBuffer sb = new StringBuffer();
        for (int i=0;i<Constants.DAY_CODES.length;i++)
            if ((iDayCode & Constants.DAY_CODES[i])!=0)
                sb.append(Constants.DAY_NAMES_SHORT[i]);
        return sb.toString(); 
    }
    /** Start time for printing purposes */
    public String getStartTimeHeader() { 
    	int min = iStartSlot * Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN;
        int h = min/60;
        int m = min%60;
        return (h>12?h-12:h)+":"+(m<10?"0":"")+m+(h>=12?"p":"a");
    }
    /** End time for printing purposes */
    public String getEndTimeHeader() { 
    	int min = (iStartSlot + iLength) * Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN - getBreakTime();
        int m = min % 60;
        int h = min / 60;
        return (h>12?h-12:h)+":"+(m<10?"0":"")+m+(h>=12?"p":"a");
    }
    /** End time for printing purposes */
    public String getEndTimeHeaderNoAdj() { 
    	int min = (iStartSlot + iLength) * Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN;
        int m = min % 60;
        int h = min / 60;
        return (h>12?h-12:h)+":"+(m<10?"0":"")+m+(h>=12?"p":"a");
    }
    /** Start slot */
    public int getStartSlot() { return iStartSlot; }
    /** Used slots in a day (combination of 1..first, 2..second,...) */
    
    /** true if days overlap */
    public boolean shareDays(TimeLocation anotherLocation) {
        return ((iDayCode & anotherLocation.iDayCode)!=0);
    }
    /** number of overlapping days */
    public int nrSharedDays(TimeLocation anotherLocation) {
        int ret=0;
        for (int i=0;i<Constants.NR_DAYS;i++) {
        	if ((iDayCode & Constants.DAY_CODES[i])==0) continue;
        	if ((anotherLocation.iDayCode & Constants.DAY_CODES[i])==0) continue;
        	ret++;
        }
        return ret;
    }
    /** true if hours overlap */
    public boolean shareHours(TimeLocation anotherLocation) {
    	return (iStartSlot+iLength > anotherLocation.iStartSlot) && (anotherLocation.iStartSlot+anotherLocation.iLength > iStartSlot);
    }
    /** number of overlapping days */
    public int nrSharedHours(TimeLocation anotherLocation) {
    	int end = Math.min(iStartSlot+iLength, anotherLocation.iStartSlot+anotherLocation.iLength);
    	int start = Math.max(iStartSlot, anotherLocation.iStartSlot);
    	return (end<start?0:end-start);
    }
    /** true if weeks overlap */
    public boolean shareWeeks(TimeLocation anotherLocation) {
        return iWeekCode.intersects(anotherLocation.iWeekCode);
    }
    /** true if weeks overlap */
    public boolean shareWeeks(BitSet weekCode) {
        return iWeekCode.intersects(weekCode);
    }
    public boolean hasDay(int day) {
    	return iWeekCode.get(day);
    }
    /** true if overlap */
    public boolean hasIntersection(TimeLocation anotherLocation) {
        return shareDays(anotherLocation) && shareHours(anotherLocation) && shareWeeks(anotherLocation);
    }

    /** Used slots */
    public IntEnumeration getSlots() { return new SlotsEnum(); }
    /** Used start slots (for each meeting) */
    public IntEnumeration getStartSlots() { return new StartSlotsEnum(); }
    /** Days */
    public IntEnumeration getDays() { return new DaysEnum(); }
    public int[] getDaysArray() {
    	int[] days = new int[getNrMeetings()];
    	int i = 0;
    	for (IntEnumeration e=getDays();e.hasMoreElements();)
    		days[i++] = e.nextInt();
    	return days;
    }
    
    /** Text representation */
    public String getName() { return getDayHeader()+" "+getStartTimeHeader(); }
    public String getLongName() { return getDayHeader()+" "+getStartTimeHeader()+" - "+getEndTimeHeader()+" "+getDatePatternName(); }
    public String getLongNameNoAdj() { return getDayHeader()+" "+getStartTimeHeader()+" - "+getEndTimeHeaderNoAdj()+" "+getDatePatternName(); }
    /** Preference */
    public int getPreference() { return iPreference; }
    public void setPreference(int preference) { iPreference = preference; }
    /** Length */
    public int getLength() { return iLength; }
    /** Length */
    public int getNrSlotsPerMeeting() { return iLength; }
    /** Normalized preference */
    public double getNormalizedPreference() { return iNormalizedPreference;}
    public void setNormalizedPreference(double normalizedPreference) { iNormalizedPreference = normalizedPreference; }
    /** Time pattern model (can be null) */
    public Long getTimePatternId() { return iTimePatternId; }
    public Long getDatePatternId() { return iDatePatternId; }
    public void setTimePatternId(Long timePatternId) { iTimePatternId = timePatternId; }
    public BitSet getWeekCode() { return iWeekCode; }
    public String getDatePatternName() { return iDatePatternName; }
    public void setDatePattern(Long datePatternId, String datePatternName, BitSet weekCode) {
        iDatePatternId = datePatternId;
        iDatePatternName = datePatternName;
        iWeekCode = weekCode;
    }
    
    public String toString() { return getName()+" ("+iNormalizedPreference+")"; }
    public int hashCode() {
        return iHashCode;
    }
    
    public interface IntEnumeration extends Enumeration {
    	public int nextInt();
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
    			if (day>=Constants.DAY_CODES.length) return false;
    		} while ((Constants.DAY_CODES[day]&iDayCode)==0);
    		return true;
    	}
    	public boolean hasMoreElements() {
    		return hasNext;
    	}
    	public Object nextElement() {
    		return new Integer(nextInt());
    	}
    	public int nextInt() {
    		int slot = (day*Constants.SLOTS_PER_DAY)+iStartSlot;
    		hasNext = nextDay();
    		return slot;
    	}
    }
    private class DaysEnum extends StartSlotsEnum {
    	private DaysEnum() {
    		super();
    	}
    	public int nextInt() {
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
    		if (pos+1<iLength) {
    			pos++; return true;
    		}
    		if (nextDay()) {
    			pos = 0; return true;
    		}
    		return false;
    	}
    	public int nextInt() {
    		int slot = (day*Constants.SLOTS_PER_DAY)+iStartSlot+pos;
    		hasNext = nextSlot();
    		return slot;
    	}
    }

    public boolean equals(Object o) {
    	if (o==null || !(o instanceof TimeLocation)) return false;
    	TimeLocation t = (TimeLocation)o;
    	if (getStartSlot()!=t.getStartSlot()) return false;
    	if (getLength()!=t.getLength()) return false;
    	if (getDayCode()!=t.getDayCode()) return false;
    	return ToolBox.equals(getTimePatternId(),t.getTimePatternId()) && ToolBox.equals(getDatePatternId(),t.getDatePatternId());
    }
    
    public int getNrWeeks() {
    	return getNrWeeks(0, iWeekCode.size()-1);
    }
    
    public int getNrWeeks(int startDay, int endDay) {
    	/*
    	BitSet x = new BitSet(1+(endDay-startDay)/Constants.NR_DAYS);
    	for (int i=iWeekCode.nextSetBit(startDay); i<=endDay && i>=0; i=iWeekCode.nextSetBit(i+1))
    		x.set((i-startDay)/Constants.NR_DAYS);
    	return x.cardinality();
    	*/
    	int card = iWeekCode.get(startDay, endDay).cardinality();
    	if (card==0) return 0;
    	if (card<=7) return 1;
    	return (5+card)/6;
    }
}
