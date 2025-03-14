package org.cpsolver.coursett.constraint;

import java.util.Set;

import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.RoomSharingModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.WeakeningConstraint;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Discouraged room constraint. This constraint is based on
 * {@link RoomConstraint}, however, it tries to minimize the usage of the room
 * as much as possible.
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
public class DiscouragedRoomConstraint extends RoomConstraint implements WeakeningConstraint<Lecture, Placement> {
    private int iUnassignmentsToWeaken = 1000;

    public DiscouragedRoomConstraint(DataProperties config, Long id, String name, Long buildingId, int capacity,
            RoomSharingModel roomSharingModel, Double x, Double y, boolean ignoreTooFar, boolean constraint) {
        super(id, name, buildingId, capacity, roomSharingModel, x, y, ignoreTooFar, constraint);
        iUnassignmentsToWeaken = config.getPropertyInt("DiscouragedRoom.Unassignments2Weaken", iUnassignmentsToWeaken);
    }

    @Override
    public void computeConflicts(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        if (!getConstraint() || !value.hasRoomLocation(getResourceId())) return;
        super.computeConflicts(assignment, value, conflicts);
        if (((DiscouragedRoomConstraintContext)getContext(assignment)).isOverLimit(assignment, value))
            conflicts.add(value);
    }

    @Override
    public boolean inConflict(Assignment<Lecture, Placement> assignment, Placement value) {
        if (!getConstraint() || !value.hasRoomLocation(getResourceId())) return false;
        return ((DiscouragedRoomConstraintContext)getContext(assignment)).isOverLimit(assignment, value) || super.inConflict(assignment, value);
    }

    @Override
    public String getName() {
        return "discouraged " + super.getName();
    }

    @Override
    public String toString() {
        return "Discouraged " + super.toString();
    }

    @Override
    public void weaken(Assignment<Lecture, Placement> assignment) {
        ((DiscouragedRoomConstraintContext)getContext(assignment)).weaken();
    }

    @Override
    public void weaken(Assignment<Lecture, Placement> assignment, Placement value) {
        ((DiscouragedRoomConstraintContext)getContext(assignment)).weaken(assignment, value);
    }
    
    @Override
    public RoomConstraintContext createAssignmentContext(Assignment<Lecture, Placement> assignment) {
        return new DiscouragedRoomConstraintContext(assignment);
    }

    @Override
    public void unassigned(Assignment<Lecture, Placement> assignment, long iteration, Placement placement) {
        super.unassigned(assignment, iteration, placement);
        if (!placement.hasRoomLocation(getResourceId()))
            ((DiscouragedRoomConstraintContext)getContext(assignment)).weaken();
    }

    public class DiscouragedRoomConstraintContext extends RoomConstraintContext {
        int iUsage = 0;
        int iLimit = 0;
        private long iUnassignment = 0;

        public DiscouragedRoomConstraintContext(Assignment<Lecture, Placement> assignment) {
            super(assignment);
        }

        @Override
        public void assigned(Assignment<Lecture, Placement> assignment, Placement placement) {
            super.assigned(assignment, placement);
            if (placement.hasRoomLocation(getResourceId()) && !placement.variable().isCommitted())
                iUsage ++;
        }
        
        @Override
        public void unassigned(Assignment<Lecture, Placement> assignment, Placement placement) {
            super.unassigned(assignment, placement);
            if (placement.hasRoomLocation(getResourceId()) && !placement.variable().isCommitted())
                iUsage --;
        }

        public int getLimit() {
            return iLimit;
        }

        public int getUsage() {
            return iUsage;
        }

        public boolean isOverLimit(Assignment<Lecture, Placement> assignment, Placement value) {
            if (iUnassignmentsToWeaken == 0)
                return false; // not working
            if (!value.hasRoomLocation(getResourceId()))
                return false; // different room
            Lecture lecture = value.variable();
            if (lecture.roomLocations().size() == lecture.getNrRooms())
                return false; // required room
            if (lecture.isCommitted())
                return false; // committed class
            Placement current = assignment.getValue(lecture);
            if (current != null && current.hasRoomLocation(getResourceId()))
                return false; // already assigned in this room
            if (iUsage + 1 <= iLimit)
                return false; // under the limit
            return true;
        }
        
        public void weaken() {
            if (iUnassignmentsToWeaken == 0) return;
            iUnassignment++;
            if (iUnassignment % iUnassignmentsToWeaken == 0)
                iLimit ++;
        }
        
        public void weaken(Assignment<Lecture, Placement> assignment, Placement value) {
            while (isOverLimit(assignment, value))
                iLimit ++;
        }
    }
}
