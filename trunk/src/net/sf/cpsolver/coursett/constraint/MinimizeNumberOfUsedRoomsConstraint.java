package net.sf.cpsolver.coursett.constraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.coursett.Constants;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.RoomLocation;
import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Minimize number of used rooms within the set of classes. <br>
 * <br>
 * This constraint implements the following distribution/group constraint: <br>
 * <br>
 * MIN_ROOM_USE (Minimize Number Of Rooms Used)<br>
 * Minimize number of rooms used by the given set of classes.
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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 */

public class MinimizeNumberOfUsedRoomsConstraint extends Constraint<Lecture, Placement> implements WeakeningConstraint {
    private int iUnassignmentsToWeaken = 250;
    private long iUnassignment = 0;
    private int iLimit = 1;
    private Hashtable<RoomLocation, Set<Lecture>> iUsedRooms = new Hashtable<RoomLocation, Set<Lecture>>();
    boolean iEnabled = false;

    public MinimizeNumberOfUsedRoomsConstraint(DataProperties config) {
        iUnassignmentsToWeaken = config.getPropertyInt("MinimizeNumberOfUsedRooms.Unassignments2Weaken",
                iUnassignmentsToWeaken);
    }

    public boolean isOverLimit(Placement placement) {
        return getOverLimit(placement) > 0;
    }

    public int getOverLimit(Placement placement) {
        if (!iEnabled)
            return 0; // not enabled
        if (iUnassignmentsToWeaken == 0)
            return 0; // not working

        Lecture lecture = placement.variable();

        if (lecture.getNrRooms() <= 0)
            return 0; // no room
        if (lecture.roomLocations().size() == lecture.getNrRooms())
            return 0; // required room
        if (lecture.isCommitted())
            return 0; // commited class

        int usage = iUsedRooms.size();
        if (usage + lecture.getNrRooms() <= iLimit)
            return 0; // under the limit, quick check

        if (placement.isMultiRoom()) {
            HashSet<RoomLocation> assignedRooms = new HashSet<RoomLocation>();
            if (lecture.getAssignment() != null)
                assignedRooms.addAll(lecture.getAssignment().getRoomLocations());
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
            RoomLocation assignedRoom = (lecture.getAssignment() != null && !lecture.getAssignment().equals(placement) ? lecture
                    .getAssignment().getRoomLocation()
                    : null);
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

    @Override
    public void computeConflicts(Placement placement, Set<Placement> conflicts) {
        int overLimit = getOverLimit(placement);
        if (overLimit > 0) {
            List<List<Placement>> adepts = new ArrayList<List<Placement>>();
            for (Enumeration<Set<Lecture>> e = iUsedRooms.elements(); e.hasMoreElements();) {
                Set<Lecture> lects = e.nextElement();
                List<Placement> placementsToUnassign = new ArrayList<Placement>();
                boolean canUnassign = true;
                for (Lecture l : lects) {
                    if (l.isCommitted()) {
                        canUnassign = false;
                        break;
                    }
                    if (!conflicts.contains(l.getAssignment()))
                        placementsToUnassign.add(l.getAssignment());
                }
                if (!canUnassign)
                    continue;
                adepts.add(placementsToUnassign);
            }
            if (adepts.size() < overLimit) {
                conflicts.add(placement);
            } else {
                Collections.sort(adepts, new Comparator<List<Placement>>() {
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
    public boolean inConflict(Placement placeement) {
        return isOverLimit(placeement);
    }

    @Override
    public boolean isConsistent(Placement value1, Placement value2) {
        return (isOverLimit(value1) || isOverLimit(value2));
    }

    @Override
    public void assigned(long iteration, Placement placement) {
        super.assigned(iteration, placement);
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
    public void unassigned(long iteration, Placement placement) {
        super.unassigned(iteration, placement);
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

    public void weaken() {
        iUnassignment++;
        if (iUnassignmentsToWeaken > 0 && iUnassignment % iUnassignmentsToWeaken == 0)
            iLimit++;
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
        double histogram[][] = new double[Constants.SLOTS_PER_DAY][Constants.NR_DAYS_WEEK];
        for (int i = 0; i < Constants.SLOTS_PER_DAY_NO_EVENINGS; i++)
            for (int j = 0; j < Constants.NR_DAYS_WEEK; j++)
                histogram[i][j] = 0.0;
        for (Lecture lecture : variables()) {
            if (lecture.getNrRooms() == 0)
                continue;
            List<Placement> values = lecture.values();
            for (Placement p : lecture.values()) {
                int firstSlot = p.getTimeLocation().getStartSlot();
                if (firstSlot > Constants.DAY_SLOTS_LAST)
                    continue;
                int endSlot = firstSlot + p.getTimeLocation().getNrSlotsPerMeeting() - 1;
                if (endSlot < Constants.DAY_SLOTS_FIRST)
                    continue;
                for (int i = firstSlot; i <= endSlot; i++) {
                    int dayCode = p.getTimeLocation().getDayCode();
                    for (int j = 0; j < Constants.NR_DAYS_WEEK; j++) {
                        if ((dayCode & Constants.DAY_CODES[j]) != 0) {
                            histogram[i][j] += ((double) lecture.getNrRooms()) / values.size();
                        }
                    }
                }
            }
        }
        int maxAverageRooms = 0;
        for (int i = 0; i < Constants.SLOTS_PER_DAY_NO_EVENINGS; i++)
            for (int j = 0; j < Constants.NR_DAYS_WEEK; j++)
                maxAverageRooms = Math.max(maxAverageRooms, (int) Math.ceil(histogram[i][j]));
        return Math.max(1, Math.max(mandatoryRooms.size(), maxAverageRooms));
    }

    public void setEnabled(boolean enabled) {
        iEnabled = enabled;
        iLimit = Math.max(iUsedRooms.size(), estimateLimit());
    }

    public boolean isEnabled() {
        return iEnabled;
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
}
