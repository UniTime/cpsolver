package org.cpsolver.exam.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;


/**
 * A student. <br>
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
public class ExamStudent extends Constraint<Exam, ExamPlacement> {
    private boolean iAllowDirectConflicts = true;
    private List<ExamOwner> iOwners = new ArrayList<ExamOwner>();
    private boolean[] iAvailable = null;

    /**
     * Constructor
     * 
     * @param model
     *            examination timetabling model
     * @param id
     *            student unique id
     */
    public ExamStudent(ExamModel model, long id) {
        super();
        iAllowDirectConflicts = model.getProperties().getPropertyBoolean("Student.AllowDirectConflicts", iAllowDirectConflicts);
        iId = id;
        setModel(model);
    }
    
    /**
     * True if direct student conflicts are allowed for this student
     * @return direct conflicts are allowed
     */
    public boolean isAllowDirectConflicts() {
        return iAllowDirectConflicts;
    }

    /**
     * Set whether direct student conflicts are allowed for this student
     * @param allowDirectConflicts direct conflicts are allowed
     */
    public void setAllowDirectConflicts(boolean allowDirectConflicts) {
        iAllowDirectConflicts = allowDirectConflicts;
    }

    /**
     * True if the given two exams can have a direct student conflict with this
     * student, i.e., they can be placed at the same period.
     * 
     * @param ex1
     *            an exam
     * @param ex2
     *            an exam
     * @return {@link ExamStudent#isAllowDirectConflicts()} and
     *         {@link Exam#isAllowDirectConflicts()} for both exams
     */
    public boolean canConflict(Exam ex1, Exam ex2) {
        return isAllowDirectConflicts() && ex1.isAllowDirectConflicts() && ex2.isAllowDirectConflicts();
    }

    /**
     * Exam(s) enrolled by the student that are scheduled in the given period
     * @param assignment current assignment
     * @param period given period
     * @return set of exams that this student is enrolled into and that are placed in the given period
     */
    public Set<Exam> getExams(Assignment<Exam, ExamPlacement> assignment, ExamPeriod period) {
        Set<Exam> exams = ((ExamModel)getModel()).getStudentsOfPeriod(assignment, period).get(this);
        return (exams != null ? exams : new HashSet<Exam>());
        // return getContext(assignment).getExams(period.getIndex());
    }

    /**
     * Exam(s) enrolled by the student that are scheduled in the given day
     * @param assignment current assignment
     * @param period given period
     * @return set of exams that this student is enrolled into and that are placed in the day of the given period
     */
    public Set<Exam> getExamsADay(Assignment<Exam, ExamPlacement> assignment, ExamPeriod period) {
        Set<Exam> exams = ((ExamModel)getModel()).getStudentsOfDay(assignment, period).get(this);
        return (exams != null ? exams : new HashSet<Exam>());
        // return getContext(assignment).getExamsOfDay(period.getDay());
    }

    /**
     * Exam(s) enrolled by the student that are scheduled in the given day
     * @param assignment current assignment
     * @param day given day
     * @return set of exams that this student is enrolled into and that are placed in the given day
     */
    public Set<Exam> getExamsADay(Assignment<Exam, ExamPlacement> assignment, int day) {
        Set<Exam> exams = ((ExamModel)getModel()).getStudentsOfDay(assignment, day).get(this);
        return (exams != null ? exams : new HashSet<Exam>());
        // return getContext(assignment).getExamsOfDay(day);
    }

    /**
     * Compute conflicts between the given assignment of an exam and all the
     * current assignments (of this student). Only not-allowed conflicts (see
     * {@link ExamStudent#canConflict(Exam, Exam)} are considered.
     */
    @Override
    public void computeConflicts(Assignment<Exam, ExamPlacement> assignment, ExamPlacement p, Set<ExamPlacement> conflicts) {
        Exam ex = p.variable();
        Set<Exam> exams = ((ExamModel)getModel()).getStudentsOfPeriod(assignment, p.getPeriod()).get(this);
        if (exams != null)
            for (Exam exam : exams) {
                if (!canConflict(ex, exam))
                    conflicts.add(assignment.getValue(exam));
            }
    }

    /**
     * Check whether there is a conflict between the given assignment of an exam
     * and all the current assignments (of this student). Only not-allowed
     * conflicts (see {@link ExamStudent#canConflict(Exam, Exam)} are
     * considered.
     */
    @Override
    public boolean inConflict(Assignment<Exam, ExamPlacement> assignment, ExamPlacement p) {
        Exam ex = p.variable();
        Set<Exam> exams = ((ExamModel)getModel()).getStudentsOfPeriod(assignment, p.getPeriod()).get(this);
        if (exams != null)
            for (Exam exam : exams) {
                if (!canConflict(ex, exam))
                    return true;
            }
        return false;
    }

    /**
     * True if the given exams can conflict (see
     * {@link ExamStudent#canConflict(Exam, Exam)}), or if they are placed at
     * different periods.
     */
    @Override
    public boolean isConsistent(ExamPlacement p1, ExamPlacement p2) {
        return (p1.getPeriod() != p2.getPeriod() || canConflict(p1.variable(), p2.variable()));
    }

    /**
     * Compare two student for equality
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ExamStudent))
            return false;
        ExamStudent s = (ExamStudent) o;
        return getId() == s.getId();
    }

    /**
     * Hash code
     */
    @Override
    public int hashCode() {
        return (int) (getId() ^ (getId() >>> 32));
    }

    /**
     * Student unique id
     */
    @Override
    public String toString() {
        return String.valueOf(getId());
    }

    /**
     * Compare two students (by student ids)
     */
    @Override
    public int compareTo(Constraint<Exam, ExamPlacement> o) {
        return toString().compareTo(o.toString());
    }

    /**
     * Constraint is hard if {@link ExamStudent#isAllowDirectConflicts()} is
     * false.
     */
    @Override
    public boolean isHard() {
        return !isAllowDirectConflicts();
    }

    /**
     * Courses and/or sections that this student is enrolled to
     * 
     * @return list of {@link ExamOwner}
     */
    public List<ExamOwner> getOwners() {
        return iOwners;
    }

    /**
     * True if the student is available (for examination timetabling) during the
     * given period
     * 
     * @param period
     *            a period
     * @return true if a student can attend an exam at the given period, false
     *         if otherwise
     */
    public boolean isAvailable(ExamPeriod period) {
        return (iAvailable == null ? true : iAvailable[period.getIndex()]);
    }

    /**
     * Set whether the student is available (for examination timetabling) during
     * the given period
     * 
     * @param period
     *            a period
     * @param available
     *            true if a student can attend an exam at the given period,
     *            false if otherwise
     */
    public void setAvailable(int period, boolean available) {
        if (iAvailable == null) {
            iAvailable = new boolean[((ExamModel)getModel()).getNrPeriods()];
            for (int i = 0; i < iAvailable.length; i++)
                iAvailable[i] = true;
        }
        iAvailable[period] = available;
    }

    /*
    @Override
    public Context createAssignmentContext(Assignment<Exam, ExamPlacement> assignment) {
        return new Context(assignment);
    }

    public class Context implements AssignmentConstraintContext<Exam, ExamPlacement> {
        private Set<Exam>[] iTable;
        private Set<Exam>[] iDayTable;
        
        @SuppressWarnings("unchecked")
        public Context(Assignment<Exam, ExamPlacement> assignment) {
            ExamModel model = (ExamModel)getModel();
            iTable = new Set[model.getNrPeriods()];
            for (int i = 0; i < iTable.length; i++)
                iTable[i] = new HashSet<Exam>();
            iDayTable = new Set[model.getNrDays()];
            for (int i = 0; i < iDayTable.length; i++)
                iDayTable[i] = new HashSet<Exam>();
            for (Exam exam: variables()) {
                ExamPlacement placement = assignment.getValue(exam);
                if (placement != null)
                    assigned(assignment, placement);
            }
        }

        @Override
        public void assigned(Assignment<Exam, ExamPlacement> assignment, ExamPlacement placement) {
            iTable[placement.getPeriod().getIndex()].add(placement.variable());
            iDayTable[placement.getPeriod().getDay()].add(placement.variable());
        }
        
        @Override
        public void unassigned(Assignment<Exam, ExamPlacement> assignment, ExamPlacement placement) {
            iTable[placement.getPeriod().getIndex()].remove(placement.variable());
            iDayTable[placement.getPeriod().getDay()].remove(placement.variable());
        }
        
        
        public Set<Exam> getExams(int period) { return iTable[period]; }
        
        public Set<Exam> getExamsOfDay(int day) { return iDayTable[day]; }
        
    }
    */
}