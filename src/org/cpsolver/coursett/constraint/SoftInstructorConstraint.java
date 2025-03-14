package org.cpsolver.coursett.constraint;

import java.util.BitSet;
import java.util.Enumeration;
import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.criteria.additional.InstructorConflict;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;

/**
 * Soft version of the instructor constraint.
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
public class SoftInstructorConstraint extends InstructorConstraint {

    public SoftInstructorConstraint(Long id, String puid, String name, boolean ignDist) {
        super(id, puid, name, ignDist);
    }
    
 
    @Override
    public void computeConflicts(Assignment<Lecture, Placement> assignment, Placement placement, Set<Placement> conflicts) {
        return;
    }
    
    @Override
    public boolean inConflict(Assignment<Lecture, Placement> assignment, Placement placement) {
        return false;
    }

    @Override
    public boolean isConsistent(Placement p1, Placement p2) {
        return true;
    }
    
    @Override
    public boolean isHard() {
        return false;
    }
    
    @Override
    public InstructorConstraintContext createAssignmentContext(Assignment<Lecture, Placement> assignment) {
        return new SoftInstructorConstraintContext(assignment);
    }
    
    public int getConflicts(Assignment<Lecture, Placement> assignment) {
        return ((SoftInstructorConstraintContext)getContext(assignment)).getConflicts();
    }
    
    public int getConflicts(Assignment<Lecture, Placement> assignment, Placement placement) {
        if (((SoftInstructorConstraintContext)getContext(assignment)).inConflict(assignment, placement)) return 1;
        return 0;
    }
    
    public int getWorstConflicts() {
        if (variables().size() < 2) return 0;
        return variables().size() - 1;
    }

    public class SoftInstructorConstraintContext extends InstructorConstraintContext {
        private int iConficts = 0;

        public SoftInstructorConstraintContext(Assignment<Lecture, Placement> assignment) {
            super(assignment);
            iConficts = countConflicts(assignment);
            getModel().getCriterion(InstructorConflict.class).inc(assignment, iConficts);
        }
        
        @Override
        public void assigned(Assignment<Lecture, Placement> assignment, Placement placement) {
            super.assigned(assignment, placement);
            getModel().getCriterion(InstructorConflict.class).inc(assignment, -iConficts);
            iConficts = countConflicts(assignment);
            getModel().getCriterion(InstructorConflict.class).inc(assignment, iConficts);
        }
        
        @Override
        public void unassigned(Assignment<Lecture, Placement> assignment, Placement placement) {
            super.unassigned(assignment, placement);
            getModel().getCriterion(InstructorConflict.class).inc(assignment, -iConficts);
            iConficts = countConflicts(assignment);
            getModel().getCriterion(InstructorConflict.class).inc(assignment, iConficts);
        }
        
        public int getConflicts() { return iConficts; }
        
        protected int countConflicts(Assignment<Lecture, Placement> assignment) {
            int conflicts = 0;
            for (Lecture lecture: variables()) {
                Placement placement = assignment.getValue(lecture);
                if (placement != null && inConflict(assignment, placement))
                    conflicts ++;
            }
            return conflicts;
        }
        
        public boolean inConflict(Assignment<Lecture, Placement> assignment, Placement placement) {
            Lecture lecture = placement.variable();
            Placement current = assignment.getValue(lecture);
            BitSet weekCode = placement.getTimeLocation().getWeekCode();
            
            if (!isAvailable(lecture, placement)) return true;
            
            for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
                int slot = e.nextElement();
                for (Placement p : getPlacements(slot)) {
                    if (!p.equals(current) && p.getTimeLocation().shareWeeks(weekCode)) {
                        if (p.canShareRooms(placement) && p.sameRooms(placement))
                            continue;
                        return true;
                    }
                }
            }
            if (!isIgnoreDistances()) {
                for (Enumeration<Integer> e = placement.getTimeLocation().getStartSlots(); e.hasMoreElements();) {
                    int startSlot = e.nextElement();
                    
                    int prevSlot = startSlot - 1;
                    if (prevSlot >= 0 && (prevSlot / Constants.SLOTS_PER_DAY) == (startSlot / Constants.SLOTS_PER_DAY)) {
                        for (Placement c : getPlacements(prevSlot, placement)) {
                            if (lecture.equals(c.variable())) continue;
                            if (c.canShareRooms(placement) && c.sameRooms(placement)) continue;
                            if (Placement.getDistanceInMeters(getDistanceMetric(), placement, c) > getDistanceMetric().getInstructorProhibitedLimit())
                                return true;
                        }
                    }
                    int nextSlot = startSlot + placement.getTimeLocation().getLength();
                    if ((nextSlot / Constants.SLOTS_PER_DAY) == (startSlot / Constants.SLOTS_PER_DAY)) {
                        for (Placement c : getPlacements(nextSlot, placement)) {
                            if (lecture.equals(c.variable())) continue;
                            if (c.canShareRooms(placement) && c.sameRooms(placement)) continue;
                            if (Placement.getDistanceInMeters(getDistanceMetric(), placement, c) > getDistanceMetric().getInstructorProhibitedLimit())
                                return true;
                        }
                    }
                    
                    if (getDistanceMetric().doComputeDistanceConflictsBetweenNonBTBClasses()) {
                        TimeLocation t1 = placement.getTimeLocation();
                        for (Lecture other: variables()) {
                            Placement otherPlacement = assignment.getValue(other);
                            if (otherPlacement == null || other.equals(placement.variable())) continue;
                            TimeLocation t2 = otherPlacement.getTimeLocation();
                            if (t1 == null || t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) continue;
                            if (t1.getStartSlot() + t1.getLength() < t2.getStartSlot()) {
                                if (Placement.getDistanceInMinutes(getDistanceMetric(), placement, otherPlacement) > t1.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t2.getStartSlot() - t1.getStartSlot() - t1.getLength()))
                                    return true;
                            } else if (t2.getStartSlot() + t2.getLength() < t1.getStartSlot()) {
                                if (Placement.getDistanceInMinutes(getDistanceMetric(), placement, otherPlacement) >  t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t1.getStartSlot() - t2.getStartSlot() - t2.getLength()))
                                    return true;
                            }
                        }
                    }
                }
            }
            return false;
        }
    }
}
