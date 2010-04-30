package net.sf.cpsolver.exam.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Value;

/**
 * An instructor. 
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
public class ExamInstructor extends Constraint {
    private HashSet[] iTable;
    private HashSet[] iDayTable;
    private String iName;
    private boolean iAllowDirectConflicts = true;
    private Vector iOwners = new Vector();
    private boolean[] iAvailable = null;

    public ExamInstructor(ExamModel model, long id, String name) {
        super();
        iAllowDirectConflicts = model.getProperties().getPropertyBoolean("Instructor.AllowDirectConflicts", iAllowDirectConflicts);
        iAssignedVariables = null;
        iId = id;
        iName = name;
        iTable = new HashSet[model.getNrPeriods()];
        for (int i=0;i<iTable.length;i++) iTable[i]=new HashSet();
        iDayTable = new HashSet[model.getNrDays()];
        for (int i=0;i<iDayTable.length;i++) iDayTable[i]=new HashSet();
    }
    
    /**
     * True when direct instructor conflicts are not allowed. 
     */
    public boolean isAllowDirectConflicts() {
        return iAllowDirectConflicts;
    }
    
    /**
     * Set to true when direct instructor conflicts are not allowed. 
     */
    public void setAllowDirectConflicts(boolean allowDirectConflicts) {
        iAllowDirectConflicts = allowDirectConflicts;
    }
    
    /**
     * Exam(s) enrolled by the instructor that are scheduled in the given period
     */
    public Set getExams(ExamPeriod period) { return iTable[period.getIndex()]; }
    /**
     * Exam(s) enrolled by the instructor that are scheduled in the given day
     */
    public Set getExamsADay(ExamPeriod period) { return iDayTable[period.getDay()]; }
    /**
     * Exam(s) enrolled by the instructor that are scheduled in the given day
     */
    public Set getExamsADay(int day) { return iDayTable[day]; }
    
    /**
     * Compute conflicts between the given assignment of an exam and all the current assignments (of this instructor).
     * Only not-allowed conflicts (see {@link ExamInstructor#isAllowDirectConflicts()}) are considered. 
     */
    public void computeConflicts(Value value, Set conflicts) {
        if (isAllowDirectConflicts()) return;
        ExamPlacement p = (ExamPlacement)value;
        for (Iterator i=iTable[p.getPeriod().getIndex()].iterator();i.hasNext();) {
            Exam exam = (Exam)i.next();
            conflicts.add(exam.getAssignment());
        }
    }
    
    /**
     * Check whether there is a conflict between the given assignment of an exam and all the current assignments (of this instructor).
     * Only not-allowed conflicts (see {@link ExamInstructor#isAllowDirectConflicts()}) are considered. 
     */
    public boolean inConflict(Value value) {
        if (isAllowDirectConflicts()) return false;
        ExamPlacement p = (ExamPlacement)value;
        return !iTable[p.getPeriod().getIndex()].isEmpty();
    }
    
    /**
     * True if the given exams can conflict (see {@link ExamInstructor#isAllowDirectConflicts()}), 
     * or if they are placed at different periods.
     */
    public boolean isConsistent(Value value1, Value value2) {
        if (isAllowDirectConflicts()) return true;
        ExamPlacement p1 = (ExamPlacement)value1;
        ExamPlacement p2 = (ExamPlacement)value2;
        return (p1.getPeriod()!=p2.getPeriod());
    }
    
    /**
     * An exam was assigned, update instructor assignment table
     */
    public void afterAssigned(long iteration, Value value) {
        ExamPlacement p = (ExamPlacement)value;
        iTable[p.getPeriod().getIndex()].add(value.variable());
        iDayTable[p.getPeriod().getDay()].add(value.variable());
    }
    
    /**
     * An exam was unassigned, update instructor assignment table
     */
    public void afterUnassigned(long iteration, Value value) {
        ExamPlacement p = (ExamPlacement)value;
        iTable[p.getPeriod().getIndex()].remove(value.variable());
        iDayTable[p.getPeriod().getDay()].remove(value.variable());
    }
    
    /**
     * Compare two instructors for equality
     */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof ExamInstructor)) return false;
        ExamInstructor s = (ExamInstructor)o;
        return getId()==s.getId();
    }
    
    /**
     * Hash code
     */
    public int hashCode() {
        return (int)(getId() ^ (getId() >>> 32));
    }
    
    /**
     * Instructor name
     */
    public String getName() {
        return hasName()?iName:String.valueOf(getId());
    }

    /**
     * Instructor name
     */
    public boolean hasName() {
        return (iName!=null && iName.length()>0);
    }
    
    /**
     * Instructor name
     */
    public String toString() {
        return getName();
    }
    
    /**
     * Compare two instructors (by instructor ids)
     */
    public int compareTo(Object o) {
        return toString().compareTo(o.toString());
    }
    
    /**
     * Courses and/or sections that this instructor is enrolled to
     * @return list of {@link ExamOwner} 
     */
    public Vector getOwners() {
        return iOwners;
    }
    
    public boolean isHard() {
        return !isAllowDirectConflicts();
    }
    
    /**
     * True if the student is available (for examination timetabling) during the given period
     * @param period a period
     * @return true if a student can attend an exam at the given period, false if otherwise
     */
    public boolean isAvailable(ExamPeriod period) { 
        return (iAvailable==null?true:iAvailable[period.getIndex()]); 
    }
    
    /**
     * Set whether the student is available (for examination timetabling) during the given period
     * @param period a period
     * @param available true if a student can attend an exam at the given period, false if otherwise
     */
    public void setAvailable(int period, boolean available) {
        if (iAvailable==null) {
            iAvailable = new boolean[iTable.length];
            for (int i=0;i<iTable.length;i++) iAvailable[i]=true;
        }
        iAvailable[period]=available; 
    }
}
