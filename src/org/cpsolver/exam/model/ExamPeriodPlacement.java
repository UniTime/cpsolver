package org.cpsolver.exam.model;

/**
 * Representation of a period placement of an exam. It contains a period
 * {@link ExamPeriod} and a penalty associated with a placement of an exam into
 * the given period. <br>
 * <br>
 * 
 * @author  Tomas Muller
 * @version ExamTT 1.3 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2014 Tomas Muller<br>
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
public class ExamPeriodPlacement implements Comparable<ExamPeriodPlacement> {
    private ExamPeriod iPeriod;
    private int iPenalty;

    /**
     * Constructor
     * 
     * @param period
     *            examination period that is available for an exam and that is
     *            of enough length
     * @param penalty
     *            period penalty for given exam
     */
    public ExamPeriodPlacement(ExamPeriod period, int penalty) {
        iPeriod = period;
        iPenalty = penalty;
    }

    /** Examination period 
     * @return period
     **/
    public ExamPeriod getPeriod() {
        return iPeriod;
    }

    /** Examination period id 
     * @return period unique id
     **/
    public Long getId() {
        return getPeriod().getId();
    }

    /** Examination period index 
     * @return period index
     **/
    public int getIndex() {
        return getPeriod().getIndex();
    }

    /**
     * Examination period penalty (for an assignment of this period to the given
     * exam {@link Exam#getPeriodPlacements()})
     * 
     * @return given penalty plus global period penalty
     *         {@link ExamPeriod#getPenalty()}
     */
    public int getPenalty() {
        return 2 * iPenalty + iPeriod.getPenalty();
    }
    
    /**
     * Period penalty for given exam
     * @return period penalty
     */
    public int getExamPenalty() {
        return iPenalty;
    }

    /**
     * Hash code
     */
    @Override
    public int hashCode() {
        return getPeriod().hashCode();
    }

    @Override
    public String toString() {
        return getPeriod().toString() + (getPenalty() == 0 ? "" : "/" + getPenalty());
    }

    /** Compare two room placements for equality */
    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o instanceof ExamPeriodPlacement) {
            return getPeriod().equals(((ExamPeriodPlacement) o).getPeriod());
        } else if (o instanceof ExamPeriod) {
            return getPeriod().equals(o);
        }
        return false;
    }

    /** Compare two period placements */
    @Override
    public int compareTo(ExamPeriodPlacement o) {
        return getPeriod().compareTo(o.getPeriod());
    }
}
