package net.sf.cpsolver.coursett.criteria;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.coursett.constraint.JenrlConstraint;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.coursett.model.TimetableModel;
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
    
    public StudentConflict() {
        iValueUpdateType = ValueUpdateType.BeforeUnassignedBeforeAssigned;
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return null;
    }

    @Override
    public double getValue() {
        return Math.round(super.getValue());
    }
    
    public DistanceMetric getMetrics() {
        return ((TimetableModel)getModel()).getDistanceMetric();
    }

    public static boolean overlaps(Placement p1, Placement p2) {
        return p1 != null && p2 != null && p1.getTimeLocation().hasIntersection(p2.getTimeLocation());
    }
    
    public static boolean distance(DistanceMetric m, Placement p1, Placement p2) {
        if (p1 == null || p2 == null || m == null) return false;
        TimeLocation t1 = p1.getTimeLocation(), t2 = p2.getTimeLocation();
        if (!t1.shareDays(t2) || !t1.shareWeeks(t2)) return false;
        if (t1.getStartSlot() != t2.getStartSlot() + t2.getNrSlotsPerMeeting() && t2.getStartSlot() != t1.getStartSlot() + t1.getNrSlotsPerMeeting()) return false;
        if (t1.getStartSlot() + t1.getLength() == t2.getStartSlot()) {
            return Placement.getDistanceInMinutes(m, p1, p2) > t1.getBreakTime();
        } else if (t2.getStartSlot() + t2.getLength() == t1.getStartSlot()) {
            return Placement.getDistanceInMinutes(m, p1, p2) > t2.getBreakTime();
        }
        return false;
    }
    
    public static boolean committed(Placement p1, Placement p2) {
        return p1 != null && p2 != null && committed(p1.variable(), p2.variable());
    }
    
    public static boolean committed(Lecture l1, Lecture l2) {
        return l1 != null && l2 != null && (l1.isCommitted() || l2.isCommitted());
    }
    
    public static boolean hard(Placement p1, Placement p2) {
        return p1 != null && p2 != null && hard(p1.variable(), p2.variable());
    }
    
    public static boolean hard(Lecture l1, Lecture l2) {
        return l1 != null && l2 != null && l1.isSingleSection() && l2.isSingleSection();
    }
    
    public boolean inConflict(Placement p1, Placement p2) {
        return !committed(p1, p2) && (overlaps(p1, p2) || distance(getMetrics(), p1, p2));
    }

    @Override
    public double getValue(Placement value, Set<Placement> conflicts) {
        double ret = 0.0;
        for (JenrlConstraint jenrl: value.variable().jenrlConstraints()) {
            Placement another = jenrl.another(value.variable()).getAssignment();
            if (another == null) continue;
            if (conflicts != null && conflicts.contains(another)) continue;
            if (inConflict(value, another))
                ret += jenrl.jenrl();
        }
        /*
        if (conflicts != null)
            for (Placement conflict: conflicts) {
                for (JenrlConstraint jenrl: conflict.variable().jenrlConstraints()) {
                    Placement another = jenrl.another(conflict.variable()).getAssignment();
                    if (another == null || another.variable().equals(value.variable())) continue;
                    if (conflicts != null && conflicts.contains(another)) continue;
                    if (inConflict(conflict, another))
                        ret -= jenrl.jenrl();
                }
            }
            */
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
                Placement another = jenrl.another(lect).getAssignment();
                if (another == null) continue;
                if (inConflict(lect.getAssignment(), another))
                    ret += jenrl.jenrl();
            }
        }
        return Math.round(ret);
    }

    @Override
    public double[] getBounds() {
        double[] bounds = { 0.0, 0.0 };
        for (JenrlConstraint jenrl: ((TimetableModel)getModel()).getJenrlConstraints())
            bounds[0] += jenrl.jenrl();
        return bounds;
    }
    
    @Override
    public double[] getBounds(Collection<Lecture> variables) {
        double[] bounds = { 0.0, 0.0 };
        Set<JenrlConstraint> constraints = new HashSet<JenrlConstraint>();
        for (Lecture lect: variables) {
            if (lect.getAssignment() == null) continue;
            for (JenrlConstraint jenrl: lect.jenrlConstraints()) {
                if (constraints.add(jenrl))
                    bounds[0] += jenrl.jenrl();
            }
        }
        return bounds;
    }
    
    public void incJenrl(JenrlConstraint jenrl, double studentWeight) {
        if (inConflict(jenrl.first().getAssignment(), jenrl.second().getAssignment()))
            iValue += studentWeight;
    }
}
