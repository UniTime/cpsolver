package org.cpsolver.coursett.constraint;

import java.util.HashSet;
import java.util.Set;

import org.cpsolver.coursett.criteria.StudentConflict;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.Student;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.BinaryConstraintWithContext;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.WeakeningConstraint;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.DistanceMetric;
import org.cpsolver.ifs.util.ToolBox;


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
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
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
    private double iJenrlMaxConflicts = 1.0;
    private double iJenrlMaxConflictsWeaken = 0.001;

    /**
     * Constructor
     */
    public JenrlConstraint() {
        super();
    }
    
    @Override
    public void setModel(Model<Lecture, Placement> model) {
        super.setModel(model);
        if (model != null && model instanceof TimetableModel) {
            DataProperties config = ((TimetableModel)model).getProperties();
            iJenrlMaxConflicts = config.getPropertyDouble("General.JenrlMaxConflicts", 1.0);
            iJenrlMaxConflictsWeaken = config.getPropertyDouble("General.JenrlMaxConflictsWeaken", 0.001);
        }
    }
    
    @Override
    public void computeConflicts(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        if (!getContext(assignment).isOverLimit() || value.variable().isCommitted()) return;
        Lecture other = another(value.variable());
        if (other == null) return;
        Placement otherPlacement = assignment.getValue(other);
        if (otherPlacement != null && !other.isCommitted() && isInConflict(value, otherPlacement, getDistanceMetric(), getWorkDayLimit()))
            conflicts.add(otherPlacement);
    }

    @Override
    public boolean inConflict(Assignment<Lecture, Placement> assignment, Placement value) {
        if (!getContext(assignment).isOverLimit() || value.variable().isCommitted()) return false;
        Lecture other = another(value.variable());
        if (other == null) return false;
        Placement otherPlacement = assignment.getValue(other);
        return otherPlacement != null && !other.isCommitted() && isInConflict(value, otherPlacement, getDistanceMetric(), getWorkDayLimit());
    }

    /**
     * Returns true if the given placements are overlapping or they are
     * back-to-back and too far for students.
     * @param p1 first placement
     * @param p2 second placement
     * @param m distance metrics
     * @param workDayLimit limit on the work-day
     * @return true if there is a student conflict between the two placements
     */
    public static boolean isInConflict(Placement p1, Placement p2, DistanceMetric m, int workDayLimit) {
        return p1 != null && p2 != null && !StudentConflict.ignore(p1.variable(), p2.variable()) && (StudentConflict.distance(m, p1, p2) || StudentConflict.overlaps(p1, p2) || StudentConflict.workday(workDayLimit, p1, p2));
    }

    /**
     * Number of joined enrollments if the given value is assigned to the given
     * variable
     * @param assignment current assignment
     * @param variable a class
     * @param value class placement under consideration
     * @return number of student conflicts caused by this constraint if assigned
     */
    public long jenrl(Assignment<Lecture, Placement> assignment, Lecture variable, Placement value) {
        Lecture other = (first().equals(variable) ? second() : first());
        Placement otherPlacement = (other == null ? null : assignment.getValue(other));
        return (otherPlacement != null && isInConflict(value, otherPlacement, getDistanceMetric(), getWorkDayLimit()) ? Math.round(iJenrl) : 0);
    }
    
    private DistanceMetric getDistanceMetric() {
        return (getModel() == null ? null : ((TimetableModel)getModel()).getDistanceMetric());
    }
    
    private int getWorkDayLimit() {
        return (getModel() == null ? -1 : ((TimetableModel)getModel()).getStudentWorkDayLimit());
    }

    /** True if the given two lectures overlap in time 
     * @param assignment current assignment
     * @return true if this constraint is conflicting
     **/
    public boolean isInConflict(Assignment<Lecture, Placement> assignment) {
        return getContext(assignment).isConflicting();
    }

    /**
     * Increment the number of joined enrollments (during student final
     * sectioning)
     * @param assignment current assignment
     * @param student student added in between the two classes of this constraint
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
     * @param assignment current assignment
     * @param student student removed from between the two classes of this constraint
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

    /** Number of joined enrollments (during student final sectioning) 
     * @return number of joined enrollments
     **/
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
    
    public boolean areStudentConflictsWorkday(Assignment<Lecture, Placement> assignment, Placement value) {
        return StudentConflict.workday(getWorkDayLimit(), value, assignment.getValue(another(value.variable())));
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
     * @return true if this constraint is to be ignored
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
            if (p1 != null && p2 != null && isInConflict(p1, p2, getDistanceMetric(), getWorkDayLimit())) {
                iAdded = true;
                first().addActiveJenrl(assignment, JenrlConstraint.this);
                second().addActiveJenrl(assignment, JenrlConstraint.this);
            }
            if (iJenrlMaxConflicts >= 0.0 && iJenrlMaxConflicts < 1.0)
                iJenrlLimit = Math.min(first().maxClassLimit(), second().maxClassLimit()) * iJenrlMaxConflicts;
        }

        @Override
        public void assigned(Assignment<Lecture, Placement> assignment, Placement value) {
            Lecture other = another(value.variable());
            if (other == null) return;
            Placement otherValue = assignment.getValue(other);
            if (!iAdded && otherValue != null && isInConflict(value, otherValue, getDistanceMetric(), getWorkDayLimit())) {
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
                double maxConflicts = iJenrlMaxConflicts + iTwiggle;
                iTwiggle = (iJenrl + 0.00001) / Math.min(first().maxClassLimit(), second().maxClassLimit()) - maxConflicts;
                if (maxConflicts + iTwiggle >= 0.0 && maxConflicts + iTwiggle < 1.0) {
                    iJenrlLimit = Math.min(first().maxClassLimit(), second().maxClassLimit()) * (maxConflicts + iTwiggle);
                } else {
                    iJenrlLimit = null;
                }
            }
        }
        
        public void weaken() {
            iTwiggle += iJenrlMaxConflictsWeaken;
            double maxConflicts = iJenrlMaxConflicts + iTwiggle;
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
            double maxConflicts = iJenrlMaxConflicts + iTwiggle;
            if (maxConflicts >= 0.0 && maxConflicts < 1.0) {
                iJenrlLimit = Math.max(Math.min(first().maxClassLimit(), second().maxClassLimit()) * maxConflicts, iJenrlLimit - weight);
            } else {
                iJenrlLimit = null;
            }
        }
    }
}
