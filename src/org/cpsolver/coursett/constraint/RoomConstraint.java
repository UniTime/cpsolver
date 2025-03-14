package org.cpsolver.coursett.constraint;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.criteria.BrokenTimePatterns;
import org.cpsolver.coursett.criteria.UselessHalfHours;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.RoomSharingModel;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Room constraint. <br>
 * Classes with the same room can not overlap in time.
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

public class RoomConstraint extends ConstraintWithContext<Lecture, Placement, RoomConstraint.RoomConstraintContext> {
    private Long iResourceId;
    private String iName;
    private Long iBuildingId;
    private int iCapacity = 0;
    private List<Placement>[] iAvailable = null;
    private boolean iConstraint = true;

    private Double iPosX = null, iPosY = null;
    private boolean iIgnoreTooFar = false;

    private RoomSharingModel iRoomSharingModel = null;

    private Long iType = null;
    private int iDayOfWeekOffset = 0;
    
    private RoomConstraint iParentRoom;
    private List<RoomConstraint> iPartitions;

    /**
     * Constructor
     * @param id room unique id
     * @param name room name
     * @param buildingId building unique id
     * @param capacity room size
     * @param roomSharingModel room sharing model
     * @param x X-coordinate (latitude)
     * @param y Y-coordinate (longitude)
     * @param ignoreTooFar ignore distances if set to true
     * @param constraint hard constraint if true (classes cannot overlap in this room)
     */
    public RoomConstraint(Long id, String name, Long buildingId, int capacity, RoomSharingModel roomSharingModel,
            Double x, Double y, boolean ignoreTooFar, boolean constraint) {
        iResourceId = id;
        iName = name;
        iBuildingId = buildingId;
        iCapacity = capacity;
        iConstraint = constraint;
        iRoomSharingModel = roomSharingModel;
        iPosX = x;
        iPosY = y;
        iIgnoreTooFar = ignoreTooFar;
    }
    
    @Override
    public void setModel(Model<Lecture, Placement> model) {
        super.setModel(model);
        if (model != null) {
            DataProperties config = ((TimetableModel)model).getProperties();
            iDayOfWeekOffset = config.getPropertyInt("DatePattern.DayOfWeekOffset", 0);
        }
    }

    @SuppressWarnings("unchecked")
    public void setNotAvailable(Placement placement) {
        if (iAvailable == null) {
            iAvailable = new List[Constants.SLOTS_PER_DAY * Constants.NR_DAYS];
            for (int i = 0; i < iAvailable.length; i++)
                iAvailable[i] = null;
        }
        for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
            int slot = e.nextElement();
            if (iAvailable[slot] == null)
                iAvailable[slot] = new ArrayList<Placement>(1);
            iAvailable[slot].add(placement);
        }
        for (Lecture lecture: variables())
            lecture.clearValueCache();
    }

    public boolean isAvailable(int slot) {
        if (getConstraint() && iAvailable != null && iAvailable[slot] != null && !iAvailable[slot].isEmpty())
            return false;
        if (getSharingModel() != null && getSharingModel().isNotAvailable(slot))
            return false;
        return true;
    }

    public boolean isAvailable(Lecture lecture, TimeLocation time, Long scheduler) {
        if (iAvailable != null && getConstraint()) {
            for (Enumeration<Integer> e = time.getSlots(); e.hasMoreElements();) {
                int slot = e.nextElement();
                if (iAvailable[slot] != null) {
                    for (Placement p : iAvailable[slot]) {
                        if (lecture.canShareRoom(p.variable()))
                            continue;
                        if (time.shareWeeks(p.getTimeLocation()))
                            return false;
                    }
                }
            }
        }

        // room partition checking
        if (getParentRoom() != null && getParentRoom().getConstraint() && getParentRoom().iAvailable != null) { // check parent room's availability
            for (Enumeration<Integer> e = time.getSlots(); e.hasMoreElements();) {
                int slot = e.nextElement();
                if (getParentRoom().iAvailable[slot] != null) {
                    for (Placement p : getParentRoom().iAvailable[slot]) {
                        if (lecture.canShareRoom(p.variable()))
                            continue;
                        if (time.shareWeeks(p.getTimeLocation()))
                            return false;
                    }
                }
            }
        }
        if (getPartitions() != null) { // check partitions for availability
            for (RoomConstraint partition: getPartitions()) {
                if (partition.iAvailable != null && partition.getConstraint()) {
                    for (Enumeration<Integer> e = time.getSlots(); e.hasMoreElements();) {
                        int slot = e.nextElement();
                        if (partition.iAvailable[slot] != null) {
                            for (Placement p : partition.iAvailable[slot]) {
                                if (lecture.canShareRoom(p.variable()))
                                    continue;
                                if (time.shareWeeks(p.getTimeLocation()))
                                    return false;
                            }
                        }
                    }
                }
            }
        }
        
        if (getSharingModel() != null && !getSharingModel().isAvailable(time, scheduler))
            return false;
        return true;
    }

    public List<Placement>[] getAvailableArray() {
        return iAvailable;
    }

    public RoomSharingModel getSharingModel() {
        return iRoomSharingModel;
    }
    
    /**
     * Add partition of this room. This room is unavailable at a time when one of the partition 
     * is not available and vice versa.
     * @param room room partition
     */
    public void addPartition(RoomConstraint room) {
        room.iParentRoom = this;
        if (iPartitions == null) iPartitions = new ArrayList<RoomConstraint>();
        iPartitions.add(room);
    }
    
    /**
     * If this room is a partition of some other room, returns the parent room (which is partitioned).
     * @return parent room
     */
    public RoomConstraint getParentRoom() { return iParentRoom; }
    
    /**
     * If this room is partitioned into multiple rooms, return room partitions
     * @return room partitions
     */
    public List<RoomConstraint> getPartitions() { return iPartitions; }

    /** Room id 
     * @return room unique id
     **/
    public Long getResourceId() {
        return iResourceId;
    }

    /** Building id 
     * @return building unique id
     **/
    public Long getBuildingId() {
        return iBuildingId;
    }

    /** Room name */
    @Override
    public String getName() {
        return iName;
    }

    public String getRoomName() {
        return iName;
    }

    /** Capacity 
     * @return room size
     **/
    public int getCapacity() {
        return iCapacity;
    }

    @Override
    public void computeConflicts(Assignment<Lecture, Placement> assignment, Placement placement, Set<Placement> conflicts) {
        if (!getConstraint())
            return;
        if (!placement.hasRoomLocation(getResourceId()))
            return;

        // room partition checking
        if (getParentRoom() != null && getParentRoom().getConstraint()) { // check parent room's availability
            Lecture lecture = placement.variable();
            Placement current = assignment.getValue(lecture);
            Set<Placement> shared = null;
            BitSet weekCode = placement.getTimeLocation().getWeekCode();
            RoomConstraintContext context = getParentRoom().getContext(assignment);
            for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
                int slot = e.nextElement();
                for (Placement confPlacement : context.getPlacements(slot)) {
                    if (!confPlacement.getTimeLocation().shareWeeks(weekCode))
                        continue;
                    if (confPlacement.equals(current))
                        continue;
                    if (shared != null && shared.contains(confPlacement))
                        continue;
                    if (confPlacement.canShareRooms(placement) && checkRoomSize(placement, shared, confPlacement)) {
                        if (shared == null) shared = new HashSet<Placement>();
                        shared.add(confPlacement);
                        continue;
                    }
                    conflicts.add(confPlacement);
                }
            }
        }
        if (getPartitions() != null) { // check partitions for availability
            Lecture lecture = placement.variable();
            Placement current = assignment.getValue(lecture);
            Set<Placement> shared = null;
            BitSet weekCode = placement.getTimeLocation().getWeekCode();
            for (RoomConstraint partition: iPartitions) {
                if (!partition.getConstraint()) continue;
                RoomConstraintContext context = partition.getContext(assignment);
                for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
                    int slot = e.nextElement();
                    for (Placement confPlacement : context.getPlacements(slot)) {
                        if (!confPlacement.getTimeLocation().shareWeeks(weekCode))
                            continue;
                        if (confPlacement.equals(current))
                            continue;
                        if (shared != null && shared.contains(confPlacement))
                            continue;
                        if (confPlacement.canShareRooms(placement) && checkRoomSize(placement, shared, confPlacement)) {
                            if (shared == null) shared = new HashSet<Placement>();
                            shared.add(confPlacement);
                            continue;
                        }
                        conflicts.add(confPlacement);
                    }
                }
            }
        }
        
        Lecture lecture = placement.variable();
        Placement current = assignment.getValue(lecture);
        Set<Placement> shared = null;
        BitSet weekCode = placement.getTimeLocation().getWeekCode();
        RoomConstraintContext context = getContext(assignment);

        for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
            int slot = e.nextElement();
            for (Placement confPlacement : context.getPlacements(slot)) {
                if (!confPlacement.getTimeLocation().shareWeeks(weekCode))
                    continue;
                if (confPlacement.equals(current))
                    continue;
                if (shared != null && shared.contains(confPlacement))
                    continue;
                if (confPlacement.canShareRooms(placement) && checkRoomSize(placement, shared, confPlacement)) {
                    if (shared == null) shared = new HashSet<Placement>();
                    shared.add(confPlacement);
                    continue;
                }
                conflicts.add(confPlacement);
            }
        }
    }
    
    public boolean checkRoomSize(Placement placement, Collection<Placement> other, Placement extra) {
        int day = -1;
        TimeLocation t1 = placement.getTimeLocation();
        while ((day = t1.getWeekCode().nextSetBit(1 + day)) >= 0) {
            int dow = (day + iDayOfWeekOffset) % 7;
            if ((t1.getDayCode() & Constants.DAY_CODES[dow]) == 0) continue;
            if (extra != null) {
                TimeLocation t2 = extra.getTimeLocation();
                if (!t2.hasDay(day) || (t2.getDayCode() & Constants.DAY_CODES[dow]) == 0) continue;
            }
            for (int i = 0; i < t1.getLength(); i++) {
                int slot = t1.getStartSlot() + i;
                int size = placement.variable().maxRoomUse();
                if (extra != null) {
                    TimeLocation t2 = extra.getTimeLocation();
                    if (t2.getStartSlot() <= slot && slot < t2.getStartSlot() + t2.getLength())
                        size += extra.variable().maxRoomUse();
                    else
                        continue;
                }
                if (other != null)
                    for (Placement p: other) {
                        TimeLocation t2 = p.getTimeLocation();
                        if (t2.hasDay(day) && (t2.getDayCode() & Constants.DAY_CODES[dow]) != 0 && t2.getStartSlot() <= slot && slot < t2.getStartSlot() + t2.getLength())
                            size += p.variable().maxRoomUse();
                    }
                if (size > getCapacity()) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean checkRoomSize(Placement placement, Collection<Placement> other) {
        return checkRoomSize(placement, other, null);
    }

    @Override
    public boolean inConflict(Assignment<Lecture, Placement> assignment, Placement placement) {
        if (!getConstraint())
            return false;
        if (!placement.hasRoomLocation(getResourceId()))
            return false;
        
        // room partition checking
        if (getParentRoom() != null && getParentRoom().getConstraint()) { // check parent room's availability
            Lecture lecture = placement.variable();
            Placement current = assignment.getValue(lecture);
            Set<Placement> shared = null;
            BitSet weekCode = placement.getTimeLocation().getWeekCode();
            RoomConstraintContext context = getParentRoom().getContext(assignment);
            for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
                int slot = e.nextElement();
                for (Placement confPlacement : context.getPlacements(slot)) {
                    if (!confPlacement.getTimeLocation().shareWeeks(weekCode))
                        continue;
                    if (confPlacement.equals(current))
                        continue;
                    if (shared != null && shared.contains(confPlacement))
                        continue;
                    if (confPlacement.canShareRooms(placement) && checkRoomSize(placement, shared, confPlacement)) {
                        if (shared == null) shared = new HashSet<Placement>();
                        shared.add(confPlacement);
                        continue;
                    }
                    return true;
                }
            }
        }
        if (getPartitions() != null) { // check partitions for availability
            Lecture lecture = placement.variable();
            Placement current = assignment.getValue(lecture);
            Set<Placement> shared = null;
            BitSet weekCode = placement.getTimeLocation().getWeekCode();
            for (RoomConstraint partition: iPartitions) {
                if (!partition.getConstraint()) continue;
                RoomConstraintContext context = partition.getContext(assignment);
                for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
                    int slot = e.nextElement();
                    for (Placement confPlacement : context.getPlacements(slot)) {
                        if (!confPlacement.getTimeLocation().shareWeeks(weekCode))
                            continue;
                        if (confPlacement.equals(current))
                            continue;
                        if (shared != null && shared.contains(confPlacement))
                            continue;
                        if (confPlacement.canShareRooms(placement) && checkRoomSize(placement, shared, confPlacement)) {
                            if (shared == null) shared = new HashSet<Placement>();
                            shared.add(confPlacement);
                            continue;
                        }
                        return true;
                    }
                }
            }
        }

        Lecture lecture = placement.variable();
        Placement current = assignment.getValue(lecture);
        Set<Placement> shared = null;
        BitSet weekCode = placement.getTimeLocation().getWeekCode();
        RoomConstraintContext context = getContext(assignment);

        for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
            int slot = e.nextElement();
            for (Placement confPlacement : context.getPlacements(slot)) {
                if (!confPlacement.getTimeLocation().shareWeeks(weekCode))
                    continue;
                if (confPlacement.equals(current))
                    continue;
                if (shared != null && shared.contains(confPlacement))
                    continue;
                if (confPlacement.canShareRooms(placement) && checkRoomSize(placement, shared, confPlacement)) {
                    if (shared == null) shared = new HashSet<Placement>();
                    shared.add(confPlacement);
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isConsistent(Placement p1, Placement p2) {
        if (!getConstraint())
            return true;
        if (getParentRoom() != null) { // partition checking -- one of the placement is with the parent room
            if ((p1.hasRoomLocation(getResourceId()) && p2.hasRoomLocation(getParentRoom().getResourceId())) ||
                (p2.hasRoomLocation(getResourceId()) && p1.hasRoomLocation(getParentRoom().getResourceId()))) {
                if (p1.getTimeLocation().hasIntersection(p2.getTimeLocation())) {
                    if (!p1.canShareRooms(p2) || (p1.variable()).maxRoomUse() + (p2.variable()).maxRoomUse() > getCapacity())
                        return true;
                }
            }
        }
        if (!p1.hasRoomLocation(getResourceId()))
            return false;
        if (!p2.hasRoomLocation(getResourceId()))
            return false;
        if (p1.getTimeLocation().hasIntersection(p2.getTimeLocation())) {
            if (!p1.canShareRooms(p2) || (p1.variable()).maxRoomUse() + (p2.variable()).maxRoomUse() > getCapacity())
                return true;
        }
        return false;
    }

    @Override
    public void assigned(Assignment<Lecture, Placement> assignment, long iteration, Placement placement) {
        if (placement.hasRoomLocation(getResourceId()))
            super.assigned(assignment, iteration, placement);
    }

    @Override
    public void unassigned(Assignment<Lecture, Placement> assignment, long iteration, Placement placement) {
        if (placement.hasRoomLocation(getResourceId()))
            super.unassigned(assignment, iteration, placement);
    }

    /**
     * Lookup table getResource()[slot] &rarr; lecture using this room placed in the
     * given time slot (null if empty)
     * @param assignment current assignment
     * @param slot time slot
     * @return list of placements in the room in the given time
     */
    public List<Placement> getResource(Assignment<Lecture, Placement> assignment, int slot) {
        return getContext(assignment).getPlacements(slot);
    }

    public Placement[] getResourceOfWeek(Assignment<Lecture, Placement> assignment, int startDay) {
        return getContext(assignment).getResourceOfWeek(startDay);
    }
    
    @Override
    public String toString() {
        return "Room " + getName();
    }

    /** Position of the building 
     * @param x X-coordinate (latitude)
     * @param y Y-coordinate (longitude)
     **/
    public void setCoordinates(Double x, Double y) {
        iPosX = x;
        iPosY = y;
    }

    /** X-position of the building 
     * @return X-coordinate (latitude)
     **/
    public Double getPosX() {
        return iPosX;
    }

    /** Y-position of the building 
     * @return Y-coordinate (longitude)
     **/
    public Double getPosY() {
        return iPosY;
    }

    public boolean getIgnoreTooFar() {
        return iIgnoreTooFar;
    }

    public boolean getConstraint() {
        return iConstraint;
    }

    public Long getType() {
        return iType;
    }

    public void setType(Long type) {
        iType = type;
    }
    
    @Override
    public RoomConstraintContext createAssignmentContext(Assignment<Lecture, Placement> assignment) {
        return new RoomConstraintContext(assignment);
    }

    public class RoomConstraintContext implements AssignmentConstraintContext<Lecture, Placement> {
        private List<Placement>[] iResource;
        private int iLastUselessHalfHours = 0;
        private double iLastBrokenTimePatterns = 0;
 
        @SuppressWarnings("unchecked")
        public RoomConstraintContext(Assignment<Lecture, Placement> assignment) {
            iResource = new List[Constants.SLOTS_PER_DAY * Constants.NR_DAYS];
            for (int i = 0; i < iResource.length; i++)
                iResource[i] = new ArrayList<Placement>(3);
            for (Lecture lecture: variables()) {
                Placement placement = assignment.getValue(lecture);
                if (placement != null && placement.hasRoomLocation(getResourceId())) {
                    for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
                        int slot = e.nextElement();
                        iResource[slot].add(placement);
                    }
                }
            }
            iLastUselessHalfHours = UselessHalfHours.countUselessSlotsHalfHours(this);
            getModel().getCriterion(UselessHalfHours.class).inc(assignment, iLastUselessHalfHours);
            iLastBrokenTimePatterns = BrokenTimePatterns.countUselessSlotsBrokenTimePatterns(this) / 6.0;
            getModel().getCriterion(BrokenTimePatterns.class).inc(assignment, iLastBrokenTimePatterns);
        }

        @Override
        public void assigned(Assignment<Lecture, Placement> assignment, Placement placement) {
            if (!placement.hasRoomLocation(getResourceId()))
                return;
            for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
                int slot = e.nextElement();
                iResource[slot].add(placement);
            }
            getModel().getCriterion(UselessHalfHours.class).inc(assignment, -iLastUselessHalfHours);
            iLastUselessHalfHours = UselessHalfHours.countUselessSlotsHalfHours(this);
            getModel().getCriterion(UselessHalfHours.class).inc(assignment, iLastUselessHalfHours);
            getModel().getCriterion(BrokenTimePatterns.class).inc(assignment, -iLastBrokenTimePatterns);
            iLastBrokenTimePatterns = BrokenTimePatterns.countUselessSlotsBrokenTimePatterns(this) / 6.0;
            getModel().getCriterion(BrokenTimePatterns.class).inc(assignment, iLastBrokenTimePatterns);
        }
        
        @Override
        public void unassigned(Assignment<Lecture, Placement> assignment, Placement placement) {
            if (!placement.hasRoomLocation(getResourceId()))
                return;
            for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
                int slot = e.nextElement();
                iResource[slot].remove(placement);
            }
            getModel().getCriterion(UselessHalfHours.class).inc(assignment, -iLastUselessHalfHours);
            iLastUselessHalfHours = UselessHalfHours.countUselessSlotsHalfHours(this);
            getModel().getCriterion(UselessHalfHours.class).inc(assignment, iLastUselessHalfHours);
            getModel().getCriterion(BrokenTimePatterns.class).inc(assignment, -iLastBrokenTimePatterns);
            iLastBrokenTimePatterns = BrokenTimePatterns.countUselessSlotsBrokenTimePatterns(this) / 6.0;
            getModel().getCriterion(BrokenTimePatterns.class).inc(assignment, iLastBrokenTimePatterns);
        }
        
        public List<Placement> getPlacements(int slot) { return iResource[slot]; }
        
        public Placement getPlacement(int slot, int day) {
            for (Placement p : iResource[slot]) {
                if (p.getTimeLocation().hasDay(day))
                    return p;
            }
            return null;
        }
        
        public Placement[] getResourceOfWeek(int startDay) {
            Placement[] ret = new Placement[iResource.length];
            for (int i = 0; i < iResource.length; i++) {
                ret[i] = getPlacement(i, startDay + (i / Constants.SLOTS_PER_DAY));
            }
            return ret;
        }
        
        public boolean inConflict(Lecture lecture, TimeLocation time) {
            for (Enumeration<Integer> e = time.getSlots(); e.hasMoreElements();) {
                int slot = e.nextElement();
                for (Placement confPlacement : getPlacements(slot)) {
                    if (!confPlacement.getTimeLocation().shareWeeks(time.getWeekCode())) continue;
                    if (confPlacement.variable().equals(lecture)) continue;
                    if (!confPlacement.variable().canShareRoom(lecture)) return true;
                }
            }
            return false;
        }

    }
}
