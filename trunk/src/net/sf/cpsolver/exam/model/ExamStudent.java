package net.sf.cpsolver.exam.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Value;
/**
 * A student. 
 * <br><br>
 * 
 * @version
 * ExamTT 1.1 (Examination Timetabling)<br>
 * Copyright (C) 2007 Tomas Muller<br>
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
public class ExamStudent extends Constraint implements Comparable {
    private HashSet[] iTable;
    private HashSet[] iDayTable;
    private boolean iAllowDirectConflicts = true;
    
    /**
     * Constructor
     * @param model examination timetabling model
     * @param id student unique id
     */
    public ExamStudent(ExamModel model, long id) {
        super();
        iAllowDirectConflicts = model.getProperties().getPropertyBoolean("Student.AllowDirectConflicts", iAllowDirectConflicts);
        iAssignedVariables = null;
        iId = id;
        iTable = new HashSet[model.getNrPeriods()];
        for (int i=0;i<iTable.length;i++) iTable[i]=new HashSet();
        iDayTable = new HashSet[model.getNrDays()];
        for (int i=0;i<iDayTable.length;i++) iDayTable[i]=new HashSet();
    }
    
    /**
     * True if direct student conflicts are allowed for this student
     */
    public boolean isAllowDirectConflicts() {
        return iAllowDirectConflicts;
    }
    
    /**
     * Set whether direct student conflicts are allowed for this student
     */
    public void setAllowDirectConflicts(boolean allowDirectConflicts) {
        iAllowDirectConflicts = allowDirectConflicts;
    }
    /**
     * True if the given two exams can have a direct student conflict with this
     * student, i.e., they can be placed at the same period. 
     * @param ex1 an exam 
     * @param ex2 an exam
     * @return {@link ExamStudent#isAllowDirectConflicts()} and {@link Exam#isAllowDirectConflicts()} for
     * both exams
     */
    public boolean canConflict(Exam ex1, Exam ex2) {
        return isAllowDirectConflicts() && ex1.isAllowDirectConflicts() && ex2.isAllowDirectConflicts();
    }
    
    /**
     * Exam(s) enrolled by the student that are scheduled in the given period
     */
    public Set getExams(ExamPeriod period) { return iTable[period.getIndex()]; }
    /**
     * Exam(s) enrolled by the student that are scheduled in the given day
     */
    public Set getExamsADay(ExamPeriod period) { return iDayTable[period.getDay()]; }
    /**
     * Exam(s) enrolled by the student that are scheduled in the given day
     */
    public Set getExamsADay(int day) { return iDayTable[day]; }
    
    /**
     * Compute conflicts between the given assignment of an exam and all the current assignments (of this student).
     * Only not-allowed conflicts (see {@link ExamStudent#canConflict(Exam, Exam)} are considered. 
     */
    public void computeConflicts(Value value, Set conflicts) {
        ExamPlacement p = (ExamPlacement)value;
        Exam ex = (Exam)p.variable();
        for (Iterator i=iTable[p.getPeriod().getIndex()].iterator();i.hasNext();) {
            Exam exam = (Exam)i.next();
            if (!canConflict(ex,exam)) conflicts.add(exam.getAssignment());
        }
    }
    
    /**
     * Check whether there is a conflict between the given assignment of an exam and all the current assignments (of this student).
     * Only not-allowed conflicts (see {@link ExamStudent#canConflict(Exam, Exam)} are considered. 
     */
    public boolean inConflict(Value value) {
        ExamPlacement p = (ExamPlacement)value;
        Exam ex = (Exam)p.variable();
        for (Iterator i=iTable[p.getPeriod().getIndex()].iterator();i.hasNext();) {
            Exam exam = (Exam)i.next();
            if (!canConflict(ex,exam)) return true;
        }
        return false;
    }
    
    /**
     * True if the given exams can conflict (see {@link ExamStudent#canConflict(Exam, Exam)}), 
     * or if they are placed at different periods.
     */
    public boolean isConsistent(Value value1, Value value2) {
        ExamPlacement p1 = (ExamPlacement)value1;
        ExamPlacement p2 = (ExamPlacement)value2;
        return (p1.getPeriod()!=p2.getPeriod() || canConflict((Exam)p1.variable(), (Exam)p2.variable()));
    }
    
    /**
     * An exam was assigned, update student assignment table
     */
    public void assigned(long iteration, Value value) {
        super.assigned(iteration, value);
        ExamPlacement p = (ExamPlacement)value;
        iTable[p.getPeriod().getIndex()].add(value.variable());
        iDayTable[p.getPeriod().getDay()].add(value.variable());
    }
    
    /**
     * An exam was unassigned, update student assignment table
     */
    public void unassigned(long iteration, Value value) {
        super.unassigned(iteration, value);
        ExamPlacement p = (ExamPlacement)value;
        iTable[p.getPeriod().getIndex()].remove(value.variable());
        iDayTable[p.getPeriod().getDay()].remove(value.variable());
    }
    
    /**
     * Compare two student for equality
     */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof ExamStudent)) return false;
        ExamStudent s = (ExamStudent)o;
        return getId()==s.getId();
    }
    
    /**
     * Hash code
     */
    public int hashCode() {
        return (int)(getId() ^ (getId() >>> 32));
    }
    
    /**
     * Student unique id
     */
    public String toString() {
        return String.valueOf(getId());
    }
    
    /**
     * Compare two students (by student ids)
     */
    public int compareTo(Object o) {
        return toString().compareTo(o.toString());
    }
    
    /**
     * Constraint is hard if {@link ExamStudent#isAllowDirectConflicts()} is false.
     */
    public boolean isHard() {
        return !iAllowDirectConflicts;
    }
}
