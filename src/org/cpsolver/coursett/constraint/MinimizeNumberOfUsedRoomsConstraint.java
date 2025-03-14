package org.cpsolver.coursett.constraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;
import org.cpsolver.ifs.model.WeakeningConstraint;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Minimize number of used rooms within the set of classes. <br>
 * <br>
 * This constraint implements the following distribution/group constraint: <br>
 * <br>
 * MIN_ROOM_USE (Minimize Number Of Rooms Used)<br>
 * Minimize number of rooms used by the given set of classes.
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

public class MinimizeNumberOfUsedRoomsConstraint extends ConstraintWithContext<Lecture, Placement, MinimizeNumberOfUsedRoomsConstraint.MinimizeNumberOfUsedRoomsConstraintContext> implements WeakeningConstraint<Lecture, Placement> {
    private int iUnassignmentsToWeaken = 250;
    private int iFirstDaySlot, iLastDaySlot, iFirstWorkDay, iLastWorkDay;

    public MinimizeNumberOfUsedRoomsConstraint(DataProperties config) {
        iUnassignmentsToWeaken = config.getPropertyInt("MinimizeNumberOfUsedRooms.Unassignments2Weaken", iUnassignmentsToWeaken);
        iFirstDaySlot = config.getPropertyInt("General.FirstDaySlot", Constants.DAY_SLOTS_FIRST);
        iLastDaySlot = config.getPropertyInt("General.LastDaySlot", Constants.DAY_SLOTS_LAST);
        iFirstWorkDay = config.getPropertyInt("General.FirstWorkDay", 0);
        iLastWorkDay = config.getPropertyInt("General.LastWorkDay", Constants.NR_DAYS_WEEK - 1);
        if (iLastWorkDay < iFirstWorkDay) iLastWorkDay += 7;
    }

    @Override
    public void computeConflicts(Assignment<Lecture, Placement> assignment, Placement placement, Set<Placement> conflicts) {
        MinimizeNumberOfUsedRoomsConstraintContext context = getContext(assignment);
        int overLimit = context.getOverLimit(assignment, placement);
        if (overLimit > 0) {
            List<List<Placement>> adepts = new ArrayList<List<Placement>>();
            for (Set<Lecture> lects: context.getUsedRooms().values()) {
                List<Placement> placementsToUnassign = new ArrayList<Placement>();
                boolean canUnassign = true;
                for (Lecture l : lects) {
                    if (l.isCommitted()) {
                        canUnassign = false;
                        break;
                    }
                    Placement p = assignment.getValue(l);
                    if (!conflicts.contains(p))
                        placementsToUnassign.add(p);
                }
                if (!canUnassign)
                    continue;
                adepts.add(placementsToUnassign);
            }
            if (adepts.size() < overLimit) {
                conflicts.add(placement);
            } else {
                Collections.sort(adepts, new Comparator<List<Placement>>() {
                    @Override
                    public int compare(List<Placement> c1, List<Placement> c2) {
                        return Double.compare(c1.size(), c2.size());
                    }
                });
                for (int i = 0; i < overLimit; i++) {
                    conflicts.addAll(adepts.get(i));
                }
            }
        }
    }

    @Override
    public boolean inConflict(Assignment<Lecture, Placement> assignment, Placement placement) {
        return getContext(assignment).isOverLimit(assignment, placement);
    }

        @Override
    public String getName() {
        return "Minimize number of used rooms";
    }

    public int estimateLimit() {
        HashSet<RoomLocation> mandatoryRooms = new HashSet<RoomLocation>();
        for (Lecture lecture : variables()) {
            if (lecture.getNrRooms() == 0)
                continue;
            if (lecture.isCommitted() || lecture.roomLocations().size() == 1)
                mandatoryRooms.addAll(lecture.roomLocations());
        }
        double histogram[][] = new double[iLastDaySlot - iFirstDaySlot + 1][iLastWorkDay - iFirstWorkDay + 1];
        for (int i = 0; i < iLastDaySlot - iFirstDaySlot + 1; i++)
            for (int j = 0; j < iLastWorkDay - iFirstWorkDay + 1; j++)
                histogram[i][j] = 0.0;
        for (Lecture lecture : variables()) {
            if (lecture.getNrRooms() == 0)
                continue;
            List<Placement> values = lecture.values(null);
            for (Placement p : lecture.values(null)) {
                int firstSlot = p.getTimeLocation().getStartSlot();
                if (firstSlot > iLastDaySlot)
                    continue;
                int endSlot = firstSlot + p.getTimeLocation().getNrSlotsPerMeeting() - 1;
                if (endSlot < iFirstDaySlot)
                    continue;
                for (int i = Math.max(firstSlot, iFirstDaySlot); i <= Math.min(endSlot, iLastDaySlot); i++) {
                    int dayCode = p.getTimeLocation().getDayCode();
                    for (int j = iFirstWorkDay; j <= iLastWorkDay; j++) {
                        if ((dayCode & Constants.DAY_CODES[j%7]) != 0) {
                            histogram[i - iFirstDaySlot][j - iFirstWorkDay] += ((double) lecture.getNrRooms()) / values.size();
                        }
                    }
                }
            }
        }
        int maxAverageRooms = 0;
        for (int i = 0; i < iLastDaySlot - iFirstDaySlot + 1; i++)
            for (int j = 0; j < iLastWorkDay - iFirstWorkDay + 1; j++)
                maxAverageRooms = Math.max(maxAverageRooms, (int) Math.ceil(histogram[i][j]));
        return Math.max(1, Math.max(mandatoryRooms.size(), maxAverageRooms));
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Minimize Number Of Rooms Used between ");
        for (Iterator<Lecture> e = variables().iterator(); e.hasNext();) {
            Lecture v = e.next();
            sb.append(v.getName());
            if (e.hasNext())
                sb.append(", ");
        }
        return sb.toString();
    }
    
    @Override
    public void weaken(Assignment<Lecture, Placement> assignment) {
        if (iUnassignmentsToWeaken > 0)
            getContext(assignment).weaken();
    }

    @Override
    public void weaken(Assignment<Lecture, Placement> assignment, Placement value) {
        getContext(assignment).weaken(assignment, value);
    }
    
    @Override
    public MinimizeNumberOfUsedRoomsConstraintContext createAssignmentContext(Assignment<Lecture, Placement> assignment) {
        return new MinimizeNumberOfUsedRoomsConstraintContext(assignment);
    }

    public class MinimizeNumberOfUsedRoomsConstraintContext implements AssignmentConstraintContext<Lecture, Placement> {
        private long iUnassignment = 0;
        private int iLimit = 1;
        private Map<RoomLocation, Set<Lecture>> iUsedRooms = new HashMap<RoomLocation, Set<Lecture>>();

        public MinimizeNumberOfUsedRoomsConstraintContext(Assignment<Lecture, Placement> assignment) {
            for (Lecture lecture: variables()) {
                Placement placement = assignment.getValue(lecture);
                if (placement != null)
                    assigned(assignment, placement);
            }
            iLimit = Math.max(iUsedRooms.size(), estimateLimit());
        }

        @Override
        public void assigned(Assignment<Lecture, Placement> assignment, Placement placement) {
            Lecture lecture = placement.variable();
            if (lecture.getNrRooms() <= 0)
                return;
            if (placement.isMultiRoom()) {
                for (RoomLocation r : placement.getRoomLocations()) {
                    Set<Lecture> lects = iUsedRooms.get(r);
                    if (lects == null) {
                        lects = new HashSet<Lecture>();
                        iUsedRooms.put(r, lects);
                    }
                    lects.add(lecture);
                }
            } else {
                RoomLocation r = placement.getRoomLocation();
                Set<Lecture> lects = iUsedRooms.get(r);
                if (lects == null) {
                    lects = new HashSet<Lecture>();
                    iUsedRooms.put(r, lects);
                }
                lects.add(lecture);
            }
        }

        @Override
        public void unassigned(Assignment<Lecture, Placement> assignment, Placement placement) {
            Lecture lecture = placement.variable();
            if (lecture.getNrRooms() <= 0)
                return;
            if (placement.isMultiRoom()) {
                for (RoomLocation r : placement.getRoomLocations()) {
                    Set<Lecture> lects = iUsedRooms.get(r);
                    if (lects != null) {
                        lects.remove(lecture);
                        if (lects.isEmpty())
                            iUsedRooms.remove(r);
                    }
                }
            } else {
                RoomLocation r = placement.getRoomLocation();
                Set<Lecture> lects = iUsedRooms.get(r);
                if (lects != null) {
                    lects.remove(lecture);
                    if (lects.isEmpty())
                        iUsedRooms.remove(r);
                }
            }
        }
        
        public boolean isOverLimit(Assignment<Lecture, Placement> assignment, Placement placement) {
            return getOverLimit(assignment, placement) > 0;
        }

        public int getOverLimit(Assignment<Lecture, Placement> assignment, Placement placement) {
            if (iUnassignmentsToWeaken == 0)
                return 0; // not working

            Lecture lecture = placement.variable();

            if (lecture.getNrRooms() <= 0)
                return 0; // no room
            if (lecture.roomLocations().size() == lecture.getNrRooms())
                return 0; // required room
            if (lecture.isCommitted())
                return 0; // commited class
            
            Placement current = assignment.getValue(lecture);

            int usage = iUsedRooms.size();
            if (usage + lecture.getNrRooms() <= iLimit)
                return 0; // under the limit, quick check

            if (placement.isMultiRoom()) {
                HashSet<RoomLocation> assignedRooms = new HashSet<RoomLocation>();
                if (current != null)
                    assignedRooms.addAll(current.getRoomLocations());
                for (RoomLocation r : placement.getRoomLocations()) {
                    if (assignedRooms.remove(r))
                        continue;
                    if (!iUsedRooms.containsKey(r))
                        usage++;
                }
                for (RoomLocation r : assignedRooms) {
                    Set<Lecture> lects = iUsedRooms.get(r);
                    if (lects != null && lects.size() == 1)
                        usage--;
                }
            } else {
                RoomLocation assignedRoom = (current != null && !current.equals(placement) ? current.getRoomLocation() : null);
                RoomLocation room = placement.getRoomLocation();
                if (!room.equals(assignedRoom)) {
                    if (!iUsedRooms.containsKey(room))
                        usage++;
                    if (assignedRoom != null) {
                        Set<Lecture> lects = iUsedRooms.get(assignedRoom);
                        if (lects != null && lects.size() == 1)
                            usage--;
                    }
                }
            }
            if (usage <= iUsedRooms.size())
                return 0; // number of used rooms not changed
            if (usage <= iLimit)
                return 0; // under the limit
            return usage - iLimit;
        }
        
        public void weaken(Assignment<Lecture, Placement> assignment, Placement value) {
            if (isOverLimit(assignment, value))
                iLimit += getOverLimit(assignment, value);
        }
        
        public void weaken() {
            iUnassignment++;
            if (iUnassignmentsToWeaken > 0 && iUnassignment % iUnassignmentsToWeaken == 0)
                iLimit++;
        }
        
        public Map<RoomLocation, Set<Lecture>> getUsedRooms() {
            return iUsedRooms;
        }
    }

}
