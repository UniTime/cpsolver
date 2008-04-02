package net.sf.cpsolver.exam.model;

/**
 * Representation of a period placement of an exam. It contains a period {@link ExamPeriod} and a penalty
 * associated with a placement of an exam into the given period.  
 * <br><br>
 * 
 * @version
 * ExamTT 1.1 (Examination Timetabling)<br>
 * Copyright (C) 2008 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
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
public class ExamPeriodPlacement implements Comparable {
    private ExamPeriod iPeriod;
    private int iPenalty;
    
    /**
     * Constructor 
     * @param period examination period that is available for an exam and that is of enough length
     * @param penalty period penalty for given exam
     */
    public ExamPeriodPlacement(ExamPeriod period, int penalty) {
        iPeriod = period;
        iPenalty = penalty;
    }
    
    /** Examination period */
    public ExamPeriod getPeriod() { return iPeriod; }
    
    /** Examination period id */
    public Long getId() { return getPeriod().getId(); }
    
    /** Examination period index */
    public int getIndex() { return getPeriod().getIndex(); }
    
    /** Examination period penalty (for an assignment of this period to the given exam {@link Exam#getPeriodPlacements()})
     * @return given penalty plus global period penalty {@link ExamPeriod#getPenalty()}    
     */
    public int getPenalty() { return iPenalty + iPeriod.getPenalty(); }
    
    /**
     * Hash code
     */
    public int hashCode() {
        return getPeriod().hashCode();
    }
    
    public String toString() {
        return getPeriod().toString()+(getPenalty()==0?"":"/"+getPenalty());
    }
    
    /** Compare two room placements for equality */
    public boolean equals(Object o) {
        if (o==null) return false;
        if (o instanceof ExamPeriodPlacement) {
            return getPeriod().equals(((ExamPeriodPlacement)o).getPeriod());
        } else if (o instanceof ExamPeriod) {
            return getPeriod().equals(o);
        }
        return false;
    }

    /** Compare two period placements */
    public int compareTo(Object o) {
        if (o==null || !(o instanceof ExamPeriodPlacement)) return -1;
        return getPeriod().compareTo(((ExamPeriodPlacement)o).getPeriod());
    }
}
