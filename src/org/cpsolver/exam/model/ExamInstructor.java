package org.cpsolver.exam.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;


/**
 * An instructor. <br>
 * <br>
 * 
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
public class ExamInstructor extends Constraint<Exam, ExamPlacement> {
    private String iName;
    private boolean iAllowDirectConflicts = true;
    private List<ExamOwner> iOwners = new ArrayList<ExamOwner>();
    private boolean[] iAvailable = null;

    public ExamInstructor(ExamModel model, long id, String name) {
        super();
        iAllowDirectConflicts = model.getProperties().getPropertyBoolean("Instructor.AllowDirectConflicts", iAllowDirectConflicts);
        iId = id;
        iName = name;
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
    public Set<Exam> getExams(Assignment<Exam, ExamPlacement> assignment, ExamPeriod period) {
        Set<Exam> exams = ((ExamModel)getModel()).getInstructorsOfPeriod(assignment, period).get(this);
        return (exams != null ? exams : new HashSet<Exam>());
        // return getContext(assignment).getExams(period.getIndex());
    }

    /**
     * Exam(s) enrolled by the instructor that are scheduled in the given day
     */
    public Set<Exam> getExamsADay(Assignment<Exam, ExamPlacement> assignment, ExamPeriod period) {
        Set<Exam> exams = ((ExamModel)getModel()).getInstructorsOfPeriod(assignment, period).get(this);
        return (exams != null ? exams : new HashSet<Exam>());
        // return getContext(assignment).getExamsOfDay(period.getDay());
    }

    /**
     * Exam(s) enrolled by the instructor that are scheduled in the given day
     */
    public Set<Exam> getExamsADay(Assignment<Exam, ExamPlacement> assignment, int day) {
        Set<Exam> exams = ((ExamModel)getModel()).getInstructorsOfDay(assignment, day).get(this);
        return (exams != null ? exams : new HashSet<Exam>());
        // return getContext(assignment).getExamsOfDay(day);
    }

    /**
     * Compute conflicts between the given assignment of an exam and all the
     * current assignments (of this instructor). Only not-allowed conflicts (see
     * {@link ExamInstructor#isAllowDirectConflicts()}) are considered.
     */
    @Override
    public void computeConflicts(Assignment<Exam, ExamPlacement> assignment, ExamPlacement p, Set<ExamPlacement> conflicts) {
        if (isAllowDirectConflicts())
            return;
        Set<Exam> exams = ((ExamModel)getModel()).getInstructorsOfPeriod(assignment, p.getPeriod()).get(this);
        if (exams != null)
            for (Exam exam : exams) {
                conflicts.add(assignment.getValue(exam));
            }
    }

    /**
     * Check whether there is a conflict between the given assignment of an exam
     * and all the current assignments (of this instructor). Only not-allowed
     * conflicts (see {@link ExamInstructor#isAllowDirectConflicts()}) are
     * considered.
     */
    @Override
    public boolean inConflict(Assignment<Exam, ExamPlacement> assignment, ExamPlacement p) {
        if (isAllowDirectConflicts())
            return false;
        Set<Exam> exams = ((ExamModel)getModel()).getInstructorsOfPeriod(assignment, p.getPeriod()).get(this);
        return exams != null && !exams.isEmpty();
    }

    /**
     * True if the given exams can conflict (see
     * {@link ExamInstructor#isAllowDirectConflicts()}), or if they are placed
     * at different periods.
     */
    @Override
    public boolean isConsistent(ExamPlacement p1, ExamPlacement p2) {
        if (isAllowDirectConflicts())
            return true;
        return (p1.getPeriod() != p2.getPeriod());
    }

    /**
     * Compare two instructors for equality
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ExamInstructor))
            return false;
        ExamInstructor s = (ExamInstructor) o;
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
     * Instructor name
     */
    @Override
    public String getName() {
        return hasName() ? iName : String.valueOf(getId());
    }

    /**
     * Instructor name
     */
    public boolean hasName() {
        return (iName != null && iName.length() > 0);
    }

    /**
     * Instructor name
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * Compare two instructors (by instructor ids)
     */
    public int compareTo(ExamInstructor o) {
        return toString().compareTo(o.toString());
    }

    /**
     * Courses and/or sections that this instructor is enrolled to
     * 
     * @return list of {@link ExamOwner}
     */
    public List<ExamOwner> getOwners() {
        return iOwners;
    }

    @Override
    public boolean isHard() {
        return !isAllowDirectConflicts();
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
