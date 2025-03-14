package org.cpsolver.coursett.criteria;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.constraint.JenrlConstraint;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.Student;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.DistanceMetric;


/**
 * Student conflicts. This criterion counts student conflicts between classes. A conflict
 * occurs when two classes that are attended by the same student (or students) are overlapping
 * in time or place back-to-back in rooms that are too far a part. The combinations of classes
 * that share students are maintained by {@link JenrlConstraint}.  
 * <br>
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
public class StudentConflict extends TimetablingCriterion {
    protected boolean iIncludeConflicts = false;
    
    public StudentConflict() {
        setValueUpdateType(ValueUpdateType.BeforeUnassignedBeforeAssigned);
    }
    
    @Override
    public void configure(DataProperties properties) {   
        super.configure(properties);
        iIncludeConflicts = properties.getPropertyBoolean("StudentConflict.IncludeConflicts", false);
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return null;
    }

    public DistanceMetric getMetrics() {
        return (getModel() == null ? null : ((TimetableModel)getModel()).getDistanceMetric());
    }

    public static boolean overlaps(Placement p1, Placement p2) {
        return p1 != null && p2 != null && p1.getTimeLocation().hasIntersection(p2.getTimeLocation()) && (!p1.variable().isCommitted() || !p2.variable().isCommitted());
    }
    
    protected double jointEnrollment(JenrlConstraint jenrl, Placement p1, Placement p2) {
        return jointEnrollment(jenrl);
    }
    
    protected double jointEnrollment(JenrlConstraint jenrl) {
        return jenrl.jenrl();
    }
    
    public static boolean distance(DistanceMetric m, Placement p1, Placement p2) {
        if (m == null && p1 != null) m = ((TimetableModel)p1.variable().getModel()).getDistanceMetric();
        if (m == null && p2 != null) m = ((TimetableModel)p2.variable().getModel()).getDistanceMetric();
        if (p1 == null || p2 == null || m == null) return false;
        if (p1.variable().isCommitted() && p2.variable().isCommitted()) return false;
        TimeLocation t1 = p1.getTimeLocation(), t2 = p2.getTimeLocation();
        if (!t1.shareDays(t2) || !t1.shareWeeks(t2)) return false;
        if (m.doComputeDistanceConflictsBetweenNonBTBClasses()) {
            if (t1.getStartSlot() + t1.getLength() <= t2.getStartSlot()) {
                return Placement.getDistanceInMinutes(m, p1, p2) > t1.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t2.getStartSlot() - t1.getStartSlot() - t1.getLength());
            } else if (t2.getStartSlot() + t2.getLength() <= t1.getStartSlot()) {
                return Placement.getDistanceInMinutes(m, p1, p2) > t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t1.getStartSlot() - t2.getStartSlot() - t2.getLength());
            }
        } else {
            if (t1.getStartSlot() + t1.getLength() == t2.getStartSlot()) {
                return Placement.getDistanceInMinutes(m, p1, p2) > t1.getBreakTime();
            } else if (t2.getStartSlot() + t2.getLength() == t1.getStartSlot()) {
                return Placement.getDistanceInMinutes(m, p1, p2) > t2.getBreakTime();
            }
        }
        return false;
    }
    
    public static int slots(Placement p1, Placement p2) {
        if (p1 == null || p2 == null) return 0;
        TimeLocation t1 = p1.getTimeLocation(), t2 = p2.getTimeLocation();
        if (t1 == null || t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) return 0;
        return Math.max(t1.getStartSlot() + t1.getLength(), t2.getStartSlot() + t2.getLength()) - Math.min(t1.getStartSlot(), t2.getStartSlot());
    }
    
    public static boolean workday(int slotsLimit, Placement p1, Placement p2) {
        if (slotsLimit <= 0) return false;
        return p1 != null && p2 != null && slots(p1, p2) > slotsLimit;
    }
    
    public static boolean ignore(Lecture l1, Lecture l2) {
        return l1 != null && l2 != null && l1.isToIgnoreStudentConflictsWith(l2);
    }
    
    public static boolean committed(Lecture l1, Lecture l2) {
        return l1 != null && l2 != null && (l1.isCommitted() || l2.isCommitted()) && (!l1.isCommitted() || !l2.isCommitted());
    }
    
    public static boolean uncommitted(Lecture l1, Lecture l2) {
        return l1 != null && l2 != null && !l1.isCommitted() && !l2.isCommitted();
    }
    
    public static boolean applicable(Lecture l1, Lecture l2) {
        return l1 != null && l2 != null && (!l1.isCommitted() || !l2.isCommitted());
    }

    public static boolean hard(Lecture l1, Lecture l2) {
        return l1 != null && l2 != null && l1.isSingleSection() && l2.isSingleSection() && (!l1.isCommitted() || !l2.isCommitted());
    }
    
    public boolean isApplicable(Lecture l1, Lecture l2) {
        return l1 != null && l2 != null && !ignore(l1, l2) && uncommitted(l1, l2); // exclude committed and outside student conflicts
    }
    
    public boolean isApplicable(Student student, Lecture l1, Lecture l2) {
        return isApplicable(l1, l2);
    }
    
    public boolean inConflict(Placement p1, Placement p2) {
        return overlaps(p1, p2) || distance(getMetrics(), p1, p2);
    }
    
    @Override
    public double getValue(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        double ret = 0.0;
        for (JenrlConstraint jenrl: value.variable().jenrlConstraints()) {
            Lecture other = jenrl.another(value.variable());
            if (!isApplicable(value.variable(), other)) continue;
            Placement another = assignment.getValue(other);
            if (another == null) continue;
            if (conflicts != null && conflicts.contains(another)) continue;
            if (inConflict(value, another))
                ret += jointEnrollment(jenrl, value, another);
        }
        if (iIncludeConflicts && conflicts != null)
            for (Placement conflict: conflicts) {
                for (JenrlConstraint jenrl: conflict.variable().jenrlConstraints()) {
                    Lecture other = jenrl.another(conflict.variable());
                    if (!isApplicable(conflict.variable(), other)) continue;
                    Placement another = assignment.getValue(other);
                    if (another == null || another.variable().equals(value.variable())) continue;
                    if (conflicts != null && conflicts.contains(another)) continue;
                    if (inConflict(conflict, another))
                        ret -= jointEnrollment(jenrl, conflict, another);
                }
            }
        return ret;
    }
    
    @Override
    public double getValue(Assignment<Lecture, Placement> assignment, Collection<Lecture> variables) {
        double ret = 0.0;
        Set<JenrlConstraint> constraints = new HashSet<JenrlConstraint>();
        for (Lecture lect: variables) {
            Placement plac = assignment.getValue(lect);
            if (plac == null) continue;
            for (JenrlConstraint jenrl: lect.jenrlConstraints()) {
                if (!constraints.add(jenrl)) continue;
                Lecture other = jenrl.another(lect);
                if (!other.isCommitted() && !variables.contains(other)) continue;
                if (!isApplicable(lect, other)) continue;
                if (inConflict(plac, assignment.getValue(other)))
                    ret += jointEnrollment(jenrl, plac, assignment.getValue(other));
            }
        }
        return ret;
    }

    @Override
    public double[] getBounds(Assignment<Lecture, Placement> assignment) {
        double[] bounds = { 0.0, 0.0 };
        for (JenrlConstraint jenrl: ((TimetableModel)getModel()).getJenrlConstraints())
            if (isApplicable(jenrl.first(), jenrl.second()))
                bounds[0] += jointEnrollment(jenrl);
        return bounds;
    }
    
    @Override
    public double[] getBounds(Assignment<Lecture, Placement> assignment, Collection<Lecture> variables) {
        double[] bounds = { 0.0, 0.0 };
        Set<JenrlConstraint> constraints = new HashSet<JenrlConstraint>();
        for (Lecture lect: variables) {
            if (assignment.getValue(lect) == null) continue;
            for (JenrlConstraint jenrl: lect.jenrlConstraints()) {
                if (isApplicable(jenrl.first(), jenrl.second()) && constraints.add(jenrl) && (jenrl.another(lect).isCommitted() || variables.contains(jenrl.another(lect))))
                    bounds[0] += jointEnrollment(jenrl);
            }
        }
        return bounds;
    }
    
    public void incJenrl(Assignment<Lecture, Placement> assignment, JenrlConstraint jenrl, double studentWeight, Double conflictPriority, Student student) {
        if (isApplicable(jenrl.first(), jenrl.second()) && inConflict(assignment.getValue(jenrl.first()), assignment.getValue(jenrl.second())))
            super.inc(assignment, studentWeight);
    }
    
    @Override
    public void bestRestored(Assignment<Lecture, Placement> assignment) {
        super.bestRestored(assignment);
        getContext(assignment).setTotal(getValue(assignment, getModel().variables()));
    }
}
