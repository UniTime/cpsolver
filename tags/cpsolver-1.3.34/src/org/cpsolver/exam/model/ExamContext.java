package org.cpsolver.exam.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;


public class ExamContext implements AssignmentConstraintContext<Exam, ExamPlacement> {
    private Map<ExamStudent, Set<Exam>>[] iStudentTable;
    private Map<ExamStudent, Set<Exam>>[] iStudentDayTable;
    private Map<ExamInstructor, Set<Exam>>[] iInstructorTable;
    private Map<ExamInstructor, Set<Exam>>[] iInstructorDayTable;
    
    @SuppressWarnings("unchecked")
    public ExamContext(ExamModel model, Assignment<Exam, ExamPlacement> assignment) {
        iStudentTable = new Map[model.getNrPeriods()];
        for (int i = 0; i < iStudentTable.length; i++)
            iStudentTable[i] = new HashMap<ExamStudent, Set<Exam>>();
        iStudentDayTable = new Map[model.getNrDays()];
        for (int i = 0; i < iStudentDayTable.length; i++)
            iStudentDayTable[i] = new HashMap<ExamStudent, Set<Exam>>();
        iInstructorTable = new Map[model.getNrPeriods()];
        for (int i = 0; i < iInstructorTable.length; i++)
            iInstructorTable[i] = new HashMap<ExamInstructor, Set<Exam>>();
        iInstructorDayTable = new Map[model.getNrDays()];
        for (int i = 0; i < iInstructorDayTable.length; i++)
            iInstructorDayTable[i] = new HashMap<ExamInstructor, Set<Exam>>();
        for (Exam exam: model.variables()) {
            ExamPlacement placement = assignment.getValue(exam);
            if (placement != null)
                assigned(assignment, placement);
        }
    }

    @Override
    public void assigned(Assignment<Exam, ExamPlacement> assignment, ExamPlacement placement) {
        int period = placement.getPeriod().getIndex();
        int day = placement.getPeriod().getDay();
        for (ExamStudent student: placement.variable().getStudents()) {
            Set<Exam> examsThisPeriod = iStudentTable[period].get(student);
            if (examsThisPeriod == null) {
                examsThisPeriod = new HashSet<Exam>();
                iStudentTable[period].put(student, examsThisPeriod);
            }
            examsThisPeriod.add(placement.variable());
            Set<Exam> examsThisDay = iStudentDayTable[day].get(student);
            if (examsThisDay == null) {
                examsThisDay = new HashSet<Exam>();
                iStudentDayTable[day].put(student, examsThisDay);
            }
            examsThisDay.add(placement.variable());
        }
        for (ExamInstructor instructor: placement.variable().getInstructors()) {
            Set<Exam> examsThisPeriod = iInstructorTable[period].get(instructor);
            if (examsThisPeriod == null) {
                examsThisPeriod = new HashSet<Exam>();
                iInstructorTable[period].put(instructor, examsThisPeriod);
            }
            examsThisPeriod.add(placement.variable());
            Set<Exam> examsThisDay = iInstructorDayTable[day].get(instructor);
            if (examsThisDay == null) {
                examsThisDay = new HashSet<Exam>();
                iInstructorDayTable[day].put(instructor, examsThisDay);
            }
            examsThisDay.add(placement.variable());
        }
    }
    
    @Override
    public void unassigned(Assignment<Exam, ExamPlacement> assignment, ExamPlacement placement) {
        int period = placement.getPeriod().getIndex();
        int day = placement.getPeriod().getDay();
        for (ExamStudent student: placement.variable().getStudents()) {
            Set<Exam> examsThisPeriod = iStudentTable[period].get(student);
            examsThisPeriod.remove(placement.variable());
            if (examsThisPeriod.isEmpty())
                iStudentTable[period].remove(student);
            Set<Exam> examsThisDay = iStudentDayTable[day].get(student);
            examsThisDay.remove(placement.variable());
            if (examsThisDay.isEmpty())
                iStudentDayTable[day].remove(student);
        }for (ExamInstructor instructor: placement.variable().getInstructors()) {
            Set<Exam> examsThisPeriod = iInstructorTable[period].get(instructor);
            examsThisPeriod.remove(placement.variable());
            if (examsThisPeriod.isEmpty())
                iInstructorTable[period].remove(instructor);
            Set<Exam> examsThisDay = iInstructorDayTable[day].get(instructor);
            examsThisDay.remove(placement.variable());
            if (examsThisDay.isEmpty())
                iInstructorDayTable[day].remove(instructor);
        }
    }
    
    public Map<ExamStudent, Set<Exam>> getStudentsOfPeriod(int period) { return iStudentTable[period]; }
    
    public Map<ExamStudent, Set<Exam>> getStudentsOfDay(int day) { return iStudentDayTable[day]; }
    
    public Map<ExamInstructor, Set<Exam>> getInstructorsOfPeriod(int period) { return iInstructorTable[period]; }
    
    public Map<ExamInstructor, Set<Exam>> getInstructorsOfDay(int day) { return iInstructorDayTable[day]; }
}
