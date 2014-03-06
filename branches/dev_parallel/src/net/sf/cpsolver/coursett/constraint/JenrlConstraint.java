package net.sf.cpsolver.coursett.constraint;

import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.coursett.criteria.StudentConflict;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.Student;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.assignment.Assignment;
import net.sf.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import net.sf.cpsolver.ifs.assignment.context.BinaryConstraintWithContext;
import net.sf.cpsolver.ifs.criteria.Criterion;
import net.sf.cpsolver.ifs.model.WeakeningConstraint;
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

public class JenrlConstraint extends BinaryConstraintWithContext<Lecture, Placement, JenrlConstraint.JenrlConstraintContext> implements WeakeningConstraint<Lecture, Placement> {
    private double iJenrl = 0.0;
    private double iPriority = 0.0;
    private Set<Student> iStudents = new HashSet<Student>();
    private Set<Student> iInstructors = new HashSet<Student>();

    /**
     * Constructor
     */
    public JenrlConstraint() {
        super();
    }
    
    @Override
    public void computeConflicts(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        if (!getContext(assignment).isOverLimit() || value.variable().isCommitted()) return;
        Lecture other = another(value.variable());
        if (other == null) return;
        Placement otherPlacement = assignment.getValue(other);
        if (otherPlacement != null && !other.isCommitted() && isInConflict(value, otherPlacement, getDistanceMetric()))
            conflicts.add(otherPlacement);
    }

    @Override
    public boolean inConflict(Assignment<Lecture, Placement> assignment, Placement value) {
        if (!getContext(assignment).isOverLimit() || value.variable().isCommitted()) return false;
        Lecture other = another(value.variable());
        if (other == null) return false;
        Placement otherPlacement = assignment.getValue(other);
        return otherPlacement != null && !other.isCommitted() && isInConflict(value, otherPlacement, getDistanceMetric());
    }

    /**
     * Returns true if the given placements are overlapping or they are
     * back-to-back and too far for students.
     */
    public static boolean isInConflict(Placement p1, Placement p2, DistanceMetric m) {
        return !StudentConflict.ignore(p1, p2) && (StudentConflict.distance(m, p1, p2) || StudentConflict.overlaps(p1, p2));
    }

    /**
     * Number of joined enrollments if the given value is assigned to the given
     * variable
     */
    public long jenrl(Assignment<Lecture, Placement> assignment, Lecture variable, Placement value) {
        Lecture other = (first().equals(variable) ? second() : first());
        Placement otherPlacement = (other == null ? null : assignment.getValue(other));
        return (otherPlacement != null && isInConflict(value, otherPlacement, getDistanceMetric()) ? Math.round(iJenrl) : 0);
    }
    
    private DistanceMetric getDistanceMetric() {
        return (getModel() == null ? null : ((TimetableModel)getModel()).getDistanceMetric());
    }

    /** True if the given two lectures overlap in time */
    public boolean isInConflict(Assignment<Lecture, Placement> assignment) {
        return getContext(assignment).isConflicting();
    }

    /**
     * Increment the number of joined enrollments (during student final
     * sectioning)
     */
    public void incJenrl(Assignment<Lecture, Placement> assignment, Student student) {
        JenrlConstraintContext context = getContext(assignment);
        boolean hard = context.isOverLimit();
        double jenrlWeight = student.getJenrlWeight(first(), second());
        iJenrl += jenrlWeight;
        Double conflictPriority = student.getConflictingPriorty(first(), second());
        if (conflictPriority != null) iPriority += conflictPriority * jenrlWeight;
        iStudents.add(student);
        if (student.getInstructor() != null && (student.getInstructor().variables().contains(first()) ||
                student.getInstructor().variables().contains(second())))
            iInstructors.add(student);
        for (Criterion<Lecture, Placement> criterion: getModel().getCriteria())
            if (criterion instanceof StudentConflict)
                ((StudentConflict)criterion).incJenrl(assignment, this, jenrlWeight, conflictPriority, student);
        if (!hard && context.isOverLimit() && isInConflict(assignment)) {
            context.incLimit(jenrlWeight);
        }
    }

    public double getJenrlWeight(Student student) {
        return student.getJenrlWeight(first(), second());
    }

    /**
     * Decrement the number of joined enrollments (during student final
     * sectioning)
     */
    public void decJenrl(Assignment<Lecture, Placement> assignment, Student student) {
        JenrlConstraintContext context = getContext(assignment);
        boolean hard = context.isOverLimit();
        double jenrlWeight = student.getJenrlWeight(first(), second());
        iJenrl -= jenrlWeight;
        Double conflictPriority = student.getConflictingPriorty(first(), second());
        if (conflictPriority != null) iPriority -= conflictPriority * jenrlWeight;
        iStudents.remove(student);
        iInstructors.remove(student);
        for (Criterion<Lecture, Placement> criterion: getModel().getCriteria())
            if (criterion instanceof StudentConflict)
                ((StudentConflict)criterion).incJenrl(assignment, this, -jenrlWeight, conflictPriority, student);
        if (hard && !context.isOverLimit())
            context.decLimit(jenrlWeight);
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
    
    public int getNrInstructors() {
        return iInstructors.size();
    }
    
    public Set<Student> getInstructors() {
        return iInstructors;
    }

    @Override
    public boolean isHard() {
        return true;
    }
    
    @Override
    public String getName() {
        return "Join Enrollment";
    }

    @Override
    public String toString() {
        return "Join Enrollment between " + first().getName() + " and " + second().getName();
    }

    public boolean areStudentConflictsHard() {
        return StudentConflict.hard(first(), second());
    }

    public boolean areStudentConflictsDistance(Assignment<Lecture, Placement> assignment) {
        return StudentConflict.distance(getDistanceMetric(), assignment.getValue(first()), assignment.getValue(second()));
    }

    public boolean areStudentConflictsCommitted() {
        return StudentConflict.committed(first(), second());
    }

    public boolean areStudentConflictsDistance(Assignment<Lecture, Placement> assignment, Placement value) {
        return StudentConflict.distance(getDistanceMetric(), value, assignment.getValue(another(value.variable())));
    }

    public boolean isOfTheSameProblem() {
        return ToolBox.equals(first().getSolverGroupId(), second().getSolverGroupId());
    }

    @Override
    public void weaken(Assignment<Lecture, Placement> assignment) {
        getContext(assignment).weaken();
    }

    @Override
    public void weaken(Assignment<Lecture, Placement> assignment, Placement value) {
        getContext(assignment).weaken(assignment, value);
    }
    
    /**
     * Returns true if there is {@link IgnoreStudentConflictsConstraint} between the two lectures.
     */
    public boolean isToBeIgnored() {
        return first().isToIgnoreStudentConflictsWith(second());
    }
    
    @Override
    public JenrlConstraintContext createAssignmentContext(Assignment<Lecture, Placement> assignment) {
        return new JenrlConstraintContext(assignment);
    }

    public class JenrlConstraintContext implements AssignmentConstraintContext<Lecture, Placement> {
        private boolean iAdded = false;
        private Double iJenrlLimit = null;
        private double iTwiggle = 0.0;

        public JenrlConstraintContext(Assignment<Lecture, Placement> assignment) {
            Placement p1 = assignment.getValue(first());
            Placement p2 = assignment.getValue(second());
            if (p1 != null && p2 != null && isInConflict(p1, p2, getDistanceMetric())) {
                iAdded = true;
                first().addActiveJenrl(assignment, JenrlConstraint.this);
                second().addActiveJenrl(assignment, JenrlConstraint.this);
            }
            double maxConflicts = ((TimetableModel)first().getModel()).getProperties().getPropertyDouble("General.JenrlMaxConflicts", 1.0);
            if (maxConflicts >= 0.0 && maxConflicts < 1.0)
                iJenrlLimit = Math.min(first().maxClassLimit(), second().maxClassLimit()) * maxConflicts;
        }

        @Override
        public void assigned(Assignment<Lecture, Placement> assignment, Placement value) {
            Lecture other = another(value.variable());
            if (other == null) return;
            Placement otherValue = assignment.getValue(other);
            if (!iAdded && otherValue != null && isInConflict(value, otherValue, getDistanceMetric())) {
                iAdded = true;
                first().addActiveJenrl(assignment, JenrlConstraint.this);
                second().addActiveJenrl(assignment, JenrlConstraint.this);
            }
        }

        @Override
        public void unassigned(Assignment<Lecture, Placement> assignment, Placement value) {
            if (iAdded) {
                iAdded = false;
                first().removeActiveJenrl(assignment, JenrlConstraint.this);
                second().removeActiveJenrl(assignment, JenrlConstraint.this);
            }
        }
        
        public boolean isConflicting() {
            return iAdded;
        }
        
        public void weaken(Assignment<Lecture, Placement> assignment, Placement value) {
            if (inConflict(assignment, value)) {
                double maxConflicts = ((TimetableModel)second().getModel()).getProperties().getPropertyDouble("General.JenrlMaxConflicts", 1.0) + iTwiggle;
                iTwiggle = (iJenrl + 0.00001) / Math.min(first().maxClassLimit(), second().maxClassLimit()) - maxConflicts;
                if (maxConflicts + iTwiggle >= 0.0 && maxConflicts + iTwiggle < 1.0) {
                    iJenrlLimit = Math.min(first().maxClassLimit(), second().maxClassLimit()) * (maxConflicts + iTwiggle);
                } else {
                    iJenrlLimit = null;
                }
            }
        }
        
        public void weaken() {
            iTwiggle += ((TimetableModel)second().getModel()).getProperties().getPropertyDouble("General.JenrlMaxConflictsWeaken", 0.001);
            double maxConflicts = ((TimetableModel)second().getModel()).getProperties().getPropertyDouble("General.JenrlMaxConflicts", 1.0) + iTwiggle;
            if (maxConflicts >= 0.0 && maxConflicts < 1.0) {
                iJenrlLimit = Math.min(first().maxClassLimit(), second().maxClassLimit()) * maxConflicts;
            } else {
                iJenrlLimit = null;
            }
        }
        
        public boolean isOverLimit() {
            return iJenrlLimit != null && iJenrl > iJenrlLimit;
        }
        
        public void incLimit(double weight) {
            if (iJenrlLimit != null) iJenrlLimit += weight;
        }
        
        public void decLimit(double weight) {
            double maxConflicts = ((TimetableModel)second().getModel()).getProperties().getPropertyDouble("General.JenrlMaxConflicts", 1.0) + iTwiggle;
            if (maxConflicts >= 0.0 && maxConflicts < 1.0) {
                iJenrlLimit = Math.max(Math.min(first().maxClassLimit(), second().maxClassLimit()) * maxConflicts, iJenrlLimit - weight);
            } else {
                iJenrlLimit = null;
            }
        }
    }
}
