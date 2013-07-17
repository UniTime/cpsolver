package net.sf.cpsolver.coursett.criteria;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.coursett.Constants;
import net.sf.cpsolver.coursett.constraint.JenrlConstraint;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.Student;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DistanceMetric;

/**
 * Student conflicts. This criterion counts student conflicts between classes. A conflict
 * occurs when two classes that are attended by the same student (or students) are overlapping
 * in time or place back-to-back in rooms that are too far a part. The combinations of classes
 * that share students are maintained by {@link JenrlConstraint}.  
 * <br>
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2011 Tomas Muller<br>
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
        iValueUpdateType = ValueUpdateType.BeforeUnassignedBeforeAssigned;
    }
    
    @Override
    public boolean init(Solver<Lecture, Placement> solver) {
        iIncludeConflicts = solver.getProperties().getPropertyBoolean("StudentConflict.IncludeConflicts", false);
        return super.init(solver);
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return null;
    }

    @Override
    public double getValue() {
        return super.getValue();
    }
    
    public DistanceMetric getMetrics() {
        return (getModel() == null ? null : ((TimetableModel)getModel()).getDistanceMetric());
    }

    public static boolean overlaps(Placement p1, Placement p2) {
        return p1 != null && p2 != null && p1.getTimeLocation().hasIntersection(p2.getTimeLocation()) && (!p1.variable().isCommitted() || !p2.variable().isCommitted());
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
    
    public static boolean ignore(Placement p1, Placement p2) {
        return p1 != null && p2 != null && p1.variable().isToIgnoreStudentConflictsWith(p2.variable());
    }
    
    public static boolean ignore(Lecture l1, Lecture l2) {
        return l1 != null && l2 != null && l1.isToIgnoreStudentConflictsWith(l2);
    }
    
    public static boolean committed(Placement p1, Placement p2) {
        return p1 != null && p2 != null && committed(p1.variable(), p2.variable());
    }
    
    public static boolean committed(Lecture l1, Lecture l2) {
        return l1 != null && l2 != null && (l1.isCommitted() || l2.isCommitted()) && (!l1.isCommitted() || !l2.isCommitted());
    }
    
    public static boolean applicable(Placement p1, Placement p2) {
        return p1 != null && p2 != null && applicable(p1.variable(), p2.variable());
    }
    
    public static boolean applicable(Lecture l1, Lecture l2) {
        return l1 != null && l2 != null && (!l1.isCommitted() || !l2.isCommitted());
    }

    public static boolean hard(Placement p1, Placement p2) {
        return p1 != null && p2 != null && hard(p1.variable(), p2.variable());
    }
    
    public static boolean hard(Lecture l1, Lecture l2) {
        return l1 != null && l2 != null && l1.isSingleSection() && l2.isSingleSection() && (!l1.isCommitted() || !l2.isCommitted());
    }
    
    public boolean isApplicable(Lecture l1, Lecture l2) {
        return !ignore(l1, l2) && applicable(l1, l2) && !committed(l1, l2); // exclude committed and outside student conflicts
    }

    public boolean inConflict(Placement p1, Placement p2) {
        return !ignore(p1, p2) && (overlaps(p1, p2) || distance(getMetrics(), p1, p2)) && isApplicable(p1.variable(), p2.variable());
    }
    
    @Override
    public double getValue(Placement value, Set<Placement> conflicts) {
        double ret = 0.0;
        for (JenrlConstraint jenrl: value.variable().jenrlConstraints()) {
            Placement another = jenrl.another(value.variable()).getAssignment();
            if (another == null) continue;
            if (conflicts != null && conflicts.contains(another)) continue;
            if (inConflict(value, another))
                ret += jointEnrollment(jenrl);
        }
        if (iIncludeConflicts && conflicts != null)
            for (Placement conflict: conflicts) {
                for (JenrlConstraint jenrl: conflict.variable().jenrlConstraints()) {
                    Placement another = jenrl.another(conflict.variable()).getAssignment();
                    if (another == null || another.variable().equals(value.variable())) continue;
                    if (conflicts != null && conflicts.contains(another)) continue;
                    if (inConflict(conflict, another))
                        ret -= jointEnrollment(jenrl);
                }
            }
        return ret;
    }
    
    @Override
    public double getValue(Collection<Lecture> variables) {
        double ret = 0.0;
        Set<JenrlConstraint> constraints = new HashSet<JenrlConstraint>();
        for (Lecture lect: variables) {
            if (lect.getAssignment() == null) continue;
            for (JenrlConstraint jenrl: lect.jenrlConstraints()) {
                if (!constraints.add(jenrl)) continue;
                if (!jenrl.another(lect).isCommitted() && !variables.contains(jenrl.another(lect))) continue;
                if (inConflict(lect.getAssignment(), jenrl.another(lect).getAssignment()))
                    ret += jointEnrollment(jenrl);
            }
        }
        return ret;
    }

    @Override
    public double[] getBounds() {
        double[] bounds = { 0.0, 0.0 };
        for (JenrlConstraint jenrl: ((TimetableModel)getModel()).getJenrlConstraints())
            if (isApplicable(jenrl.first(), jenrl.second()))
                bounds[0] += jointEnrollment(jenrl);
        return bounds;
    }
    
    @Override
    public double[] getBounds(Collection<Lecture> variables) {
        double[] bounds = { 0.0, 0.0 };
        Set<JenrlConstraint> constraints = new HashSet<JenrlConstraint>();
        for (Lecture lect: variables) {
            if (lect.getAssignment() == null) continue;
            for (JenrlConstraint jenrl: lect.jenrlConstraints()) {
                if (isApplicable(jenrl.first(), jenrl.second()) && constraints.add(jenrl) && (jenrl.another(lect).isCommitted() || variables.contains(jenrl.another(lect))))
                    bounds[0] += jointEnrollment(jenrl);
            }
        }
        return bounds;
    }
    
    public void incJenrl(JenrlConstraint jenrl, double studentWeight, Double conflictPriority, Student student) {
        if (inConflict(jenrl.first().getAssignment(), jenrl.second().getAssignment()))
            iValue += studentWeight;
    }
    
    @Override
    public void bestRestored() {
        super.bestRestored();
        iValue = getValue(getModel().variables());
    }
}
