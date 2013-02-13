package net.sf.cpsolver.coursett.constraint;

import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.coursett.criteria.StudentConflict;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.Student;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.criteria.Criterion;
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
    private double iPriority = 0.0;
    private Set<Student> iStudents = new HashSet<Student>();
    private boolean iAdded = false;

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
            iAdded = false;
            (first()).removeActiveJenrl(this);
            (second()).removeActiveJenrl(this);
        }
    }

    /**
     * Returns true if the given placements are overlapping or they are
     * back-to-back and too far for students.
     */
    public static boolean isInConflict(Placement p1, Placement p2, DistanceMetric m) {
        return StudentConflict.distance(m, p1, p2) || StudentConflict.overlaps(p1, p2);
    }

    @Override
    public void assigned(long iteration, Placement value) {
        super.assigned(iteration, value);
        if (second() == null || first().getAssignment() == null || second().getAssignment() == null)
            return;
        if (isInConflict(first().getAssignment(), second().getAssignment(), getDistanceMetric())) {
            iAdded = true;
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

    /**
     * Increment the number of joined enrollments (during student final
     * sectioning)
     */
    public void incJenrl(Student student) {
        double jenrlWeight = student.getJenrlWeight(first(), second());
        iJenrl += jenrlWeight;
        Double conflictPriority = student.getConflictingPriorty(first(), second());
        if (conflictPriority != null) iPriority += conflictPriority * jenrlWeight;
        iStudents.add(student);
        for (Criterion<Lecture, Placement> criterion: getModel().getCriteria())
            if (criterion instanceof StudentConflict)
                ((StudentConflict)criterion).incJenrl(this, jenrlWeight, conflictPriority);
    }

    public double getJenrlWeight(Student student) {
        return student.getJenrlWeight(first(), second());
    }

    /**
     * Decrement the number of joined enrollments (during student final
     * sectioning)
     */
    public void decJenrl(Student student) {
        double jenrlWeight = student.getJenrlWeight(first(), second());
        iJenrl -= jenrlWeight;
        Double conflictPriority = student.getConflictingPriorty(first(), second());
        if (conflictPriority != null) iPriority -= conflictPriority * jenrlWeight;
        iStudents.remove(student);
        for (Criterion<Lecture, Placement> criterion: getModel().getCriteria())
            if (criterion instanceof StudentConflict)
                ((StudentConflict)criterion).incJenrl(this, -jenrlWeight, conflictPriority);
    }

    /** Number of joined enrollments (during student final sectioning) */
    public long getJenrl() {
        return Math.round(iJenrl);
    }

    public double jenrl() {
        return iJenrl;
    }

    public double priority() {
        return iPriority;
    }

    public int getNrStudents() {
        return iStudents.size();
    }
    
    public Set<Student> getStudents() {
        return iStudents;
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
        return StudentConflict.hard(first(), second());
    }

    public boolean areStudentConflictsDistance() {
        return StudentConflict.distance(getDistanceMetric(), first().getAssignment(), second().getAssignment());
    }

    public boolean areStudentConflictsCommitted() {
        return StudentConflict.committed(first(), second());
    }

    public boolean areStudentConflictsDistance(Placement value) {
        return StudentConflict.distance(getDistanceMetric(), value, another(value.variable()).getAssignment());
    }

    public boolean isOfTheSameProblem() {
        return ToolBox.equals(first().getSolverGroupId(), second().getSolverGroupId());
    }

}
