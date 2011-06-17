package net.sf.cpsolver.coursett.constraint;

import java.util.Set;

import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.Student;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.model.BinaryConstraint;
import net.sf.cpsolver.ifs.util.DistanceMetric;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Join student enrollment constraint. <br>
 * This constraint is placed between all pairs of classes where there is at
 * least one student attending both classes. It represents a number of student
 * conflicts (number of joined enrollments), if the given two classes overlap in
 * time. <br>
 * Also, it dynamically maintains the counter of all student conflicts. It is a
 * soft constraint.
 * 
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
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

public class JenrlConstraint extends BinaryConstraint<Lecture, Placement> {
    private double iJenrl = 0.0;
    private int iNrStrudents = 0;
    private boolean iAdded = false;
    private boolean iAddedDistance = false;

    /**
     * Constructor
     */
    public JenrlConstraint() {
        super();
    }

    @Override
    public void computeConflicts(Placement value, Set<Placement> conflicts) {
    }

    @Override
    public boolean inConflict(Placement value) {
        return false;
    }

    @Override
    public boolean isConsistent(Placement value1, Placement value2) {
        return true;
    }

    @Override
    public void unassigned(long iteration, Placement value) {
        super.unassigned(iteration, value);
        if (iAdded) {
            ((TimetableModel) getModel()).getViolatedStudentConflictsCounter().dec(Math.round(iJenrl));
            if (areStudentConflictsHard())
                ((TimetableModel) getModel()).getViolatedHardStudentConflictsCounter().dec(Math.round(iJenrl));
            if (areStudentConflictsCommitted())
                ((TimetableModel) getModel()).getViolatedCommitttedStudentConflictsCounter().dec(Math.round(iJenrl));
            iAdded = false;
            (first()).removeActiveJenrl(this);
            (second()).removeActiveJenrl(this);
        }
        if (iAddedDistance) {
            ((TimetableModel) getModel()).getViolatedDistanceStudentConflictsCounter().dec(Math.round(iJenrl));
            iAddedDistance = false;
        }
    }

    /**
     * Returns true if the given placements are overlapping or they are
     * back-to-back and too far for students.
     */
    public static boolean isInConflict(Placement p1, Placement p2, DistanceMetric m) {
        if (p1 == null || p2 == null)
            return false;
        TimeLocation t1 = p1.getTimeLocation(), t2 = p2.getTimeLocation();
        if (!t1.shareDays(t2))
            return false;
        if (!t1.shareWeeks(t2))
            return false;
        if (t1.shareHours(t2))
            return true;
        if (m == null)
            return false;
        int s1 = t1.getStartSlot(), s2 = t2.getStartSlot();
        if (s1 + t1.getNrSlotsPerMeeting() != s2 && s2 + t2.getNrSlotsPerMeeting() != s1)
            return false;
        int distance = Placement.getDistanceInMinutes(m, p1, p2);
        if (s1 + t1.getLength() == s2)
            return distance > t1.getBreakTime();
        else
            return distance > t2.getBreakTime();
    }

    @Override
    public void assigned(long iteration, Placement value) {
        super.assigned(iteration, value);
        if (second() == null || first().getAssignment() == null || second().getAssignment() == null)
            return;
        // if (v1.getInitialAssignment()!=null &&
        // v2.getInitialAssignment()!=null &&
        // v1.getAssignment().equals(v1.getInitialAssignment()) &&
        // v2.getAssignment().equals(v2.getInitialAssignment())) return;
        if (isInConflict(first().getAssignment(), second().getAssignment(), getDistanceMetric())) {
            iAdded = true;
            ((TimetableModel) getModel()).getViolatedStudentConflictsCounter().inc(Math.round(iJenrl));
            if (areStudentConflictsHard())
                ((TimetableModel) getModel()).getViolatedHardStudentConflictsCounter().inc(Math.round(iJenrl));
            if (areStudentConflictsCommitted())
                ((TimetableModel) getModel()).getViolatedCommitttedStudentConflictsCounter().inc(Math.round(iJenrl));
            if (areStudentConflictsDistance()) {
                ((TimetableModel) getModel()).getViolatedDistanceStudentConflictsCounter()
                        .inc(Math.round(iJenrl));
                iAddedDistance = true;
            }
            (first()).addActiveJenrl(this);
            (second()).addActiveJenrl(this);
        }
    }

    /**
     * Number of joined enrollments if the given value is assigned to the given
     * variable
     */
    public long jenrl(Lecture variable, Placement value) {
        Lecture anotherLecture = (first().equals(variable) ? second() : first());
        if (anotherLecture.getAssignment() == null)
            return 0;
        return (isInConflict(anotherLecture.getAssignment(), value, getDistanceMetric()) ? Math.round(iJenrl) : 0);
    }
    
    private DistanceMetric getDistanceMetric() {
        return ((TimetableModel)getModel()).getDistanceMetric();
    }

    /** True if the given two lectures overlap in time */
    public boolean isInConflict() {
        return iAdded;
    }

    /** True if the given two lectures overlap in time */
    public boolean isInConflictPrecise() {
        return isInConflict(first().getAssignment(), second().getAssignment(), getDistanceMetric());
    }

    /**
     * Increment the number of joined enrollments (during student final
     * sectioning)
     */
    public void incJenrl(Student student) {
        if (iAdded) {
            ((TimetableModel) getModel()).getViolatedStudentConflictsCounter().dec(Math.round(iJenrl));
            if (areStudentConflictsHard())
                ((TimetableModel) getModel()).getViolatedHardStudentConflictsCounter().dec(Math.round(iJenrl));
            if (areStudentConflictsCommitted())
                ((TimetableModel) getModel()).getViolatedCommitttedStudentConflictsCounter().dec(Math.round(iJenrl));
            if (iAddedDistance)
                ((TimetableModel) getModel()).getViolatedDistanceStudentConflictsCounter()
                        .dec(Math.round(iJenrl));
        }
        iJenrl += student.getJenrlWeight(first(), second());
        iNrStrudents++;
        if (iAdded) {
            ((TimetableModel) getModel()).getViolatedStudentConflictsCounter().inc(Math.round(iJenrl));
            if (areStudentConflictsHard())
                ((TimetableModel) getModel()).getViolatedHardStudentConflictsCounter().inc(Math.round(iJenrl));
            if (areStudentConflictsCommitted())
                ((TimetableModel) getModel()).getViolatedCommitttedStudentConflictsCounter().inc(Math.round(iJenrl));
            if (iAddedDistance)
                ((TimetableModel) getModel()).getViolatedDistanceStudentConflictsCounter()
                        .inc(Math.round(iJenrl));
        }
    }

    public double getJenrlWeight(Student student) {
        return student.getJenrlWeight(first(), second());
    }

    /**
     * Decrement the number of joined enrollments (during student final
     * sectioning)
     */
    public void decJenrl(Student student) {
        if (iAdded) {
            ((TimetableModel) getModel()).getViolatedStudentConflictsCounter().dec(Math.round(iJenrl));
            if (areStudentConflictsHard())
                ((TimetableModel) getModel()).getViolatedHardStudentConflictsCounter().dec(Math.round(iJenrl));
            if (areStudentConflictsCommitted())
                ((TimetableModel) getModel()).getViolatedCommitttedStudentConflictsCounter().dec(Math.round(iJenrl));
            if (iAddedDistance)
                ((TimetableModel) getModel()).getViolatedDistanceStudentConflictsCounter()
                        .dec(Math.round(iJenrl));
        }
        iJenrl -= student.getJenrlWeight(first(), second());
        iNrStrudents--;
        if (iAdded) {
            ((TimetableModel) getModel()).getViolatedStudentConflictsCounter().inc(Math.round(iJenrl));
            if (areStudentConflictsHard())
                ((TimetableModel) getModel()).getViolatedHardStudentConflictsCounter().inc(Math.round(iJenrl));
            if (areStudentConflictsCommitted())
                ((TimetableModel) getModel()).getViolatedCommitttedStudentConflictsCounter().inc(Math.round(iJenrl));
            if (iAddedDistance)
                ((TimetableModel) getModel()).getViolatedDistanceStudentConflictsCounter()
                        .inc(Math.round(iJenrl));
        }
    }

    /** Number of joined enrollments (during student final sectioning) */
    public long getJenrl() {
        return Math.round(iJenrl);
    }

    public int getNrStudents() {
        return iNrStrudents;
    }

    @Override
    public boolean isHard() {
        return false;
    }

    @Override
    public String toString() {
        return "Joint Enrollment between " + first().getName() + " and " + second().getName();
    }

    public boolean areStudentConflictsHard() {
        return (first()).areStudentConflictsHard(second());
    }

    public boolean areStudentConflictsDistance() {
        return !(first().getAssignment()).getTimeLocation().hasIntersection(
                (second().getAssignment()).getTimeLocation());
    }

    public boolean areStudentConflictsCommitted() {
        return first().isCommitted() || second().isCommitted();
    }

    public boolean areStudentConflictsDistance(Placement value) {
        Placement first = (first().equals(value.variable()) ? value : first().getAssignment());
        Placement second = (second().equals(value.variable()) ? value : second().getAssignment());
        if (first == null || second == null)
            return false;
        return !first.getTimeLocation().hasIntersection(second.getTimeLocation());
    }

    public boolean isOfTheSameProblem() {
        Lecture first = first();
        Lecture second = second();
        return ToolBox.equals(first.getSolverGroupId(), second.getSolverGroupId());
    }

}
