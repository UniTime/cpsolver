package net.sf.cpsolver.coursett.constraint;

import java.util.Set;

import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.RoomSharingModel;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Discouraged room constraint. This constraint is based on
 * {@link RoomConstraint}, however, it tries to minimize the usage of the room
 * as much as possible.
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
public class DiscouragedRoomConstraint extends RoomConstraint {
    int iUsage = 0;
    int iLimit = 0;
    boolean iEnabled = false;

    private int iUnassignmentsToWeaken = 1000;
    private long iUnassignment = 0;

    public DiscouragedRoomConstraint(DataProperties config, Long id, String name, Long buildingId, int capacity,
            RoomSharingModel roomSharingModel, Double x, Double y, boolean ignoreTooFar, boolean constraint) {
        super(id, name, buildingId, capacity, roomSharingModel, x, y, ignoreTooFar, constraint);
        iUnassignmentsToWeaken = config.getPropertyInt("DiscouragedRoom.Unassignments2Weaken", iUnassignmentsToWeaken);
    }

    public int getLimit() {
        return iLimit;
    }

    public int getUsage() {
        return iUsage;
    }

    public boolean isOverLimit(Placement value) {
        if (!iEnabled)
            return false; // not enabled
        if (iUnassignmentsToWeaken == 0)
            return false; // not working
        Placement placement = value;
        if (!placement.hasRoomLocation(getResourceId()))
            return false; // different room
        Lecture lecture = placement.variable();
        if (lecture.roomLocations().size() == lecture.getNrRooms())
            return false; // required room
        if (lecture.isCommitted())
            return false; // commited class
        if (lecture.getAssignment() != null && (lecture.getAssignment()).hasRoomLocation(getResourceId()))
            return false; // already assigned in this room
        if (iUsage + 1 <= iLimit)
            return false; // under the limit
        return true;
    }

    @Override
    public void computeConflicts(Placement value, Set<Placement> conflicts) {
        if (!getConstraint())
            return;
        super.computeConflicts(value, conflicts);
        if (isOverLimit(value))
            conflicts.add(value);
    }

    @Override
    public boolean inConflict(Placement value) {
        if (!getConstraint())
            return false;
        if (isOverLimit(value))
            return true;
        return super.inConflict(value);
    }

    @Override
    public boolean isConsistent(Placement value1, Placement value2) {
        if (!getConstraint())
            return true;
        if (isOverLimit(value1) || isOverLimit(value2))
            return false;
        return super.isConsistent(value1, value2);
    }

    @Override
    public void assigned(long iteration, Placement value) {
        super.assigned(iteration, value);
        Placement placement = value;
        if (!placement.hasRoomLocation(getResourceId()))
            return;
        Lecture lecture = placement.variable();
        if (lecture.isCommitted())
            return;
        iUsage++;
    }

    @Override
    public void unassigned(long iteration, Placement value) {
        super.unassigned(iteration, value);
        Placement placement = value;
        if (!placement.hasRoomLocation(getResourceId())) {
            iUnassignment++;
            if (iUnassignmentsToWeaken > 0 && iUnassignment % iUnassignmentsToWeaken == 0)
                iLimit++;
        } else {
            iUsage--;
        }
    }

    @Override
    public String getName() {
        return "discouraged " + super.getName();
    }

    @Override
    public String toString() {
        return "Discouraged " + super.toString();
    }

    public void setEnabled(boolean enabled) {
        iEnabled = enabled;
        iLimit = Math.max(iUsage, iLimit);
    }

    public boolean isEnabled() {
        return iEnabled;
    }
}
