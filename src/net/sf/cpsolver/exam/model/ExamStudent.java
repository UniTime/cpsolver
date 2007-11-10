package net.sf.cpsolver.exam.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Value;

public class ExamStudent extends Constraint implements Comparable {
    private HashSet[] iTable;
    private HashSet[] iDayTable;
    private String iId;
    private boolean iAllowDirectConflicts = true;
    
    public ExamStudent(ExamModel model, String id) {
        super();
        iAssignedVariables = null;
        iId = id;
        iTable = new HashSet[model.getNrPeriods()];
        for (int i=0;i<iTable.length;i++) iTable[i]=new HashSet();
        iDayTable = new HashSet[model.getNrDays()];
        for (int i=0;i<iDayTable.length;i++) iDayTable[i]=new HashSet();
    }
    
    public boolean isAllowDirectConflicts() {
        return iAllowDirectConflicts;
    }
    
    public void setAllowDirectConflicts(boolean allowDirectConflicts) {
        iAllowDirectConflicts = allowDirectConflicts;
    }
    
    private boolean canConflict(Exam ex1, Exam ex2) {
        return isAllowDirectConflicts() && ex1.isAllowDirectConflicts() && ex2.isAllowDirectConflicts();
    }
    
    public String getStudentId() { return iId; }
    public Set getExams(ExamPeriod period) { return iTable[period.getIndex()]; }
    public Set getExamsADay(ExamPeriod period) { return iDayTable[period.getDay()]; }
    public Set getExamsADay(int day) { return iDayTable[day]; }
    
    public void computeConflicts(Value value, Set conflicts) {
        ExamPlacement p = (ExamPlacement)value;
        Exam ex = (Exam)p.variable();
        for (Iterator i=iTable[p.getPeriod().getIndex()].iterator();i.hasNext();) {
            Exam exam = (Exam)i.next();
            if (!canConflict(ex,exam)) conflicts.add(exam.getAssignment());
        }
    }
    
    public boolean inConflict(Value value) {
        ExamPlacement p = (ExamPlacement)value;
        Exam ex = (Exam)p.variable();
        for (Iterator i=iTable[p.getPeriod().getIndex()].iterator();i.hasNext();) {
            Exam exam = (Exam)i.next();
            if (!canConflict(ex,exam)) return true;
        }
        return false;
    }
    
    public boolean isConsistent(Value value1, Value value2) {
        ExamPlacement p1 = (ExamPlacement)value1;
        ExamPlacement p2 = (ExamPlacement)value2;
        return (p1.getPeriod()!=p2.getPeriod() || canConflict((Exam)p1.variable(), (Exam)p2.variable()));
    }
    
    public void assigned(long iteration, Value value) {
        super.assigned(iteration, value);
        ExamPlacement p = (ExamPlacement)value;
        iTable[p.getPeriod().getIndex()].add(value.variable());
        iDayTable[p.getPeriod().getDay()].add(value.variable());
    }
    
    public void unassigned(long iteration, Value value) {
        super.unassigned(iteration, value);
        ExamPlacement p = (ExamPlacement)value;
        iTable[p.getPeriod().getIndex()].remove(value.variable());
        iDayTable[p.getPeriod().getDay()].remove(value.variable());
    }
    
    public boolean equals(Object o) {
        if (o==null || !(o instanceof ExamStudent)) return false;
        ExamStudent s = (ExamStudent)o;
        return getStudentId().equals(s.getStudentId());
    }
    
    public int hashCode() {
        return getStudentId().hashCode();
    }
    
    public String toString() {
        return getStudentId();
    }
    
    public int compareTo(Object o) {
        return toString().compareTo(o.toString());
    }
    
    public boolean isHard() {
        return !iAllowDirectConflicts;
    }
}
