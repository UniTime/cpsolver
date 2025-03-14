package org.cpsolver.exam.model;

/**
 * Representation of an examination period. Examination timetabling model
 * contains a list of non-overlapping examination periods. Each period has a
 * day, starting time and a length (in minutes) defined. Each exam is to be
 * assigned to one period that is available for the exam and that is of the same
 * of greater length than the exam. <br>
 * <br>
 * A penalty weight ({@link ExamPeriod#getPenalty()}) can be assigned to each
 * period. It is used to penalize unpopular examination times (e.g., evening or
 * last-day). <br>
 * <br>
 * A list of periods is to be defined using
 * {@link ExamModel#addPeriod(Long, String, String, int, int)}, inserting
 * periods in the order of increasing days and times. <br>
 * <br>
 * 
 * @author  Tomas Muller
 * @version ExamTT 1.3 (Examination Timetabling)<br>
 *          Copyright (C) 2007 - 2014 Tomas Muller<br>
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
public class ExamPeriod implements Comparable<ExamPeriod> {
    private int iIndex = -1;
    private Long iId = null;
    private String iTimeStr;
    private String iDayStr;
    private int iLength;
    private int iDay, iTime;
    private int iPenalty;
    private Integer iStart = null;
    private ExamPeriod iPrev, iNext;

    /**
     * Constructor
     * 
     * @param id
     *            period unique identifier
     * @param day
     *            day (e.g., 07/12/10)
     * @param time
     *            (e.g., 8:00am-10:00am)
     * @param length
     *            length of period in minutes
     * @param penalty
     *            penalization of using this period
     */
    public ExamPeriod(Long id, String day, String time, int length, int penalty) {
        iId = id;
        iDayStr = day;
        iTimeStr = time;
        iLength = length;
        iPenalty = penalty;
    }

    /** Period unique identifier 
     * @return period unique id
     **/
    public Long getId() {
        return iId;
    }

    /** Period unique identifier 
     * @param id period unique id
     **/
    public void setId(Long id) {
        iId = id;
    }

    /**
     * Day string, e.g., 07/12/10
     * @return day of the period
     */
    public String getDayStr() {
        return iDayStr;
    }

    /**
     * Day index
     * 
     * @return index of the day within all days that are used for examination
     */
    public int getDay() {
        return iDay;
    }

    /**
     * Time string, e.g., 8:00am-10:00am
     * @return time of the period
     */
    public String getTimeStr() {
        return iTimeStr;
    }

    /**
     * Time index
     * 
     * @return index of the time within all time that are used for examination
     *         on the same day
     */
    public int getTime() {
        return iTime;
    }

    /**
     * Length of period in minutes
     * 
     * @return period length
     */
    public int getLength() {
        return iLength;
    }

    /**
     * Period index
     * 
     * @return index of the period within all examination periods
     */
    public int getIndex() {
        return iIndex;
    }

    /**
     * Period weight to be used to penalize unpopular periods
     * 
     * @return period weight
     */
    public int getPenalty() {
        return iPenalty;
    }

    /**
     * Previous period
     * 
     * @return period with index equal to index-1, null if this is the first
     *         period
     */
    public ExamPeriod prev() {
        return iPrev;
    }

    /**
     * Next period
     * 
     * @return period with index equal to index+1, null if this is the last
     *         period
     */
    public ExamPeriod next() {
        return iNext;
    }

    /**
     * Set priod indexes (only to be used by
     * {@link ExamModel#addPeriod(Long, String, String, int, int)})
     * 
     * @param index
     *            period index
     * @param day
     *            day index
     * @param time
     *            time index
     */
    public void setIndex(int index, int day, int time) {
        iIndex = index;
        iDay = day;
        iTime = time;
    }

    /**
     * Set previous period (only to be used by
     * {@link ExamModel#addPeriod(Long, String, String, int, int)})
     * 
     * @param prev
     *            previous period
     */
    public void setPrev(ExamPeriod prev) {
        iPrev = prev;
    }

    /**
     * Set next period (only to be used by
     * {@link ExamModel#addPeriod(Long, String, String, int, int)})
     * 
     * @param next
     *            next period
     */
    public void setNext(ExamPeriod next) {
        iNext = next;
    }

    /**
     * String representation
     * 
     * @return day string time string
     */
    @Override
    public String toString() {
        return getDayStr() + " " + getTimeStr();
    }

    /**
     * String representation for debuging purposes
     * 
     * @return day string time string (idx: index, day: day index, time: time
     *         index, weight: period penalty, prev: previous period, next: next
     *         period)
     */
    public String toDebugString() {
        return getDayStr() + " " + getTimeStr() + " (idx:" + getIndex() + ", day:" + getDay() + ", time:" + getTime()
                + ", penalty:" + getPenalty()
                + (prev() == null ? "" : ", prev:" + prev().getDayStr() + " " + prev().getTimeStr() + ")")
                + (next() == null ? "" : ", next:" + next().getDayStr() + " " + next().getTimeStr() + ")");
    }

    @Override
    public int hashCode() {
        return iIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ExamPeriod))
            return false;
        return getIndex() == ((ExamPeriod) o).getIndex();
    }

    @Override
    public int compareTo(ExamPeriod p) {
        return Double.compare(getIndex(), p.getIndex());
    }
    
    public Integer getStartTime() { return iStart; }
    public void setStartTime(Integer startTime) { iStart = startTime; }
    
    /**
     * Check if this period overlaps with the given period
     * @param p other period
     * @return true if the two periods overlap in time (considering period lengths)
     */
    public boolean hasIntersection(ExamPeriod p) {
        if (getIndex() == p.getIndex() || getStartTime() == null || p.getStartTime() == null) return false;
        return getDay() == p.getDay() && (getStartTime() + getLength() > p.getStartTime()) && (p.getStartTime() + p.getLength() > getStartTime());
    }
    
    /**
     * Check if the first exam assigned to this period overlaps with the second exam assigned in the given period period
     * @param x1 exam of this period
     * @param x2 the other exam (of the period p)
     * @param p other period
     * @return true if the two periods overlap in time (considering examination lengths)
     */
    public boolean hasIntersection(Exam x1, Exam x2, ExamPeriod p) {
        if (getIndex() == p.getIndex() || getStartTime() == null || p.getStartTime() == null) return false;
        return getDay() == p.getDay() && (getStartTime() + x1.getLength() > p.getStartTime()) && (p.getStartTime() + x2.getLength() > getStartTime());
    }
}
