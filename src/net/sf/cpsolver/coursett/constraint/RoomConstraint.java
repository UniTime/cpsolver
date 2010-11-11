package net.sf.cpsolver.coursett.constraint;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.coursett.Constants;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.RoomSharingModel;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.ifs.model.Constraint;

/**
 * Room constraint. <br>
 * Classes with the same room can not overlap in time.
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

public class RoomConstraint extends Constraint<Lecture, Placement> {
    private List<Placement>[] iResource;
    private Long iResourceId;
    private String iName;
    private long iPreference = 0;
    private Long iBuildingId;
    private int iCapacity = 0;
    private List<Placement>[] iAvailable = null;
    private boolean iConstraint = true;

    private Double iPosX = null, iPosY = null;
    private boolean iIgnoreTooFar = false;

    private Integer iCacheUselessSlots = null;
    private Integer iCacheUselessSlotsHalfHours = null;
    private Integer iCacheBrokenTimePatterns = null;

    private RoomSharingModel iRoomSharingModel = null;

    private Long iType = null;

    /**
     * Constructor
     */
    @SuppressWarnings("unchecked")
    public RoomConstraint(Long id, String name, Long buildingId, int capacity, RoomSharingModel roomSharingModel,
            Double x, Double y, boolean ignoreTooFar, boolean constraint) {
        iResourceId = id;
        iName = name;
        iResource = new List[Constants.SLOTS_PER_DAY * Constants.NR_DAYS];
        iBuildingId = buildingId;
        iCapacity = capacity;
        iConstraint = constraint;
        for (int i = 0; i < iResource.length; i++)
            iResource[i] = new ArrayList<Placement>(3);
        iRoomSharingModel = roomSharingModel;
        iPosX = x;
        iPosY = y;
        iIgnoreTooFar = ignoreTooFar;
    }

    @SuppressWarnings("unchecked")
    public void setNotAvailable(Placement placement) {
        if (iAvailable == null) {
            iAvailable = new List[Constants.SLOTS_PER_DAY * Constants.NR_DAYS];
            for (int i = 0; i < iResource.length; i++)
                iAvailable[i] = null;
        }
        for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
            int slot = e.nextElement();
            if (iAvailable[slot] == null)
                iAvailable[slot] = new ArrayList<Placement>(1);
            iAvailable[slot].add(placement);
        }
    }

    public boolean isAvailable(int slot) {
        if (iAvailable != null && iAvailable[slot] != null && !iAvailable[slot].isEmpty())
            return false;
        if (getSharingModel() != null && getSharingModel().isNotAvailable(slot))
            return false;
        return true;
    }

    public boolean isAvailable(Lecture lecture, TimeLocation time, Long scheduler) {
        if (iAvailable != null) {
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

    /** Room id */
    public Long getResourceId() {
        return iResourceId;
    }

    /** Building id */
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

    /** Capacity */
    public int getCapacity() {
        return iCapacity;
    }

    public Placement getPlacement(int slot, int day) {
        for (Placement p : iResource[slot]) {
            if (p.getTimeLocation().hasDay(day))
                return p;
        }
        return null;
    }

    @Override
    public void computeConflicts(Placement placement, Set<Placement> conflicts) {
        if (!getConstraint())
            return;
        if (!placement.hasRoomLocation(getResourceId()))
            return;
        Lecture lecture = placement.variable();
        boolean canShareRoom = lecture.canShareRoom();
        int size = lecture.maxRoomUse();
        HashSet<Placement> skipPlacements = null;
        BitSet weekCode = placement.getTimeLocation().getWeekCode();

        for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
            int slot = e.nextElement();
            for (Placement confPlacement : iResource[slot]) {
                if (!confPlacement.getTimeLocation().shareWeeks(weekCode))
                    continue;
                if (confPlacement.equals(lecture.getAssignment()))
                    continue;
                Lecture confLecture = confPlacement.variable();
                if (skipPlacements != null && skipPlacements.contains(confPlacement))
                    continue;
                if (canShareRoom && confPlacement.canShareRooms(placement)
                        && confLecture.maxRoomUse() + size <= getCapacity()) {
                    size += confLecture.maxRoomUse();
                    if (skipPlacements == null)
                        skipPlacements = new HashSet<Placement>();
                    skipPlacements.add(confPlacement);
                    continue;
                }
                conflicts.add(confPlacement);
            }
        }
    }

    @Override
    public boolean inConflict(Placement placement) {
        if (!getConstraint())
            return false;
        Lecture lecture = placement.variable();
        if (!placement.hasRoomLocation(getResourceId()))
            return false;
        int size = lecture.maxRoomUse();
        HashSet<Placement> skipPlacements = null;
        BitSet weekCode = placement.getTimeLocation().getWeekCode();

        for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
            int slot = e.nextElement();
            for (Placement confPlacement : iResource[slot]) {
                if (!confPlacement.getTimeLocation().shareWeeks(weekCode))
                    continue;
                if (confPlacement.equals(lecture.getAssignment()))
                    continue;
                Lecture confLecture = confPlacement.variable();
                if (skipPlacements != null && skipPlacements.contains(confPlacement))
                    continue;
                if (confPlacement.canShareRooms(placement) && confLecture.maxRoomUse() + size <= getCapacity()) {
                    size += confLecture.maxRoomUse();
                    if (skipPlacements == null)
                        skipPlacements = new HashSet<Placement>();
                    skipPlacements.add(confPlacement);
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
    public void assigned(long iteration, Placement placement) {
        super.assigned(iteration, placement);
        if (!placement.hasRoomLocation(getResourceId()))
            return;
        clearCache();
        iPreference += placement.getRoomLocation(getResourceId()).getPreference();
        for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
            int slot = e.nextElement();
            iResource[slot].add(placement);
        }
    }

    @Override
    public void unassigned(long iteration, Placement placement) {
        super.unassigned(iteration, placement);
        if (!placement.hasRoomLocation(getResourceId()))
            return;
        clearCache();
        iPreference -= placement.getRoomLocation(getResourceId()).getPreference();
        for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
            int slot = e.nextElement();
            iResource[slot].remove(placement);
        }
    }

    /**
     * Lookup table getResource()[slot] -> lecture using this room placed in the
     * given time slot (null if empty)
     */
    public List<Placement> getResource(int slot) {
        return iResource[slot];
    }

    public Placement[] getResourceOfWeek(int startDay) {
        Placement[] ret = new Placement[iResource.length];
        for (int i = 0; i < iResource.length; i++) {
            ret[i] = getPlacement(i, startDay + (i / Constants.SLOTS_PER_DAY));
        }
        return ret;
    }

    boolean isUseless(int slot) {
        int s = slot % Constants.SLOTS_PER_DAY;
        if (s - 1 < 0 || s + 6 >= Constants.SLOTS_PER_DAY)
            return false;
        return (!iResource[slot - 1].isEmpty() && iResource[slot + 0].isEmpty() && iResource[slot + 1].isEmpty()
                && iResource[slot + 2].isEmpty() && iResource[slot + 3].isEmpty() && iResource[slot + 4].isEmpty()
                && iResource[slot + 5].isEmpty() && !iResource[slot + 6].isEmpty());
    }

    boolean isUselessBefore(int slot) {
        int s = slot % Constants.SLOTS_PER_DAY;
        if (s - 1 < 0 || s + 6 >= Constants.SLOTS_PER_DAY)
            return false;
        return (!iResource[slot - 1].isEmpty() && iResource[slot + 0].isEmpty() && iResource[slot + 1].isEmpty()
                && iResource[slot + 2].isEmpty() && iResource[slot + 3].isEmpty() && iResource[slot + 4].isEmpty() && iResource[slot + 5]
                .isEmpty());
    }

    boolean isUselessAfter(int slot) {
        int s = slot % Constants.SLOTS_PER_DAY;
        if (s - 1 < 0 || s + 6 >= Constants.SLOTS_PER_DAY)
            return false;
        return (iResource[slot + 0].isEmpty() && iResource[slot + 1].isEmpty() && iResource[slot + 2].isEmpty()
                && iResource[slot + 3].isEmpty() && iResource[slot + 4].isEmpty() && iResource[slot + 5].isEmpty() && !iResource[slot + 6]
                .isEmpty());
    }

    /** Number of useless slots for this room */
    public int countUselessSlots() {
        if (iCacheUselessSlots != null)
            return iCacheUselessSlots.intValue();
        float ret = 0;
        float factor = 1.0f / 6.0f;
        for (int d = 0; d < Constants.NR_DAYS; d++) {
            for (int s = 0; s < Constants.SLOTS_PER_DAY; s++) {
                int slot = d * Constants.SLOTS_PER_DAY + s;
                if (iResource[slot].isEmpty()) {
                    if (isUseless(slot))
                        ret += Constants.sPreferenceLevelStronglyDiscouraged;
                    switch (d) {
                        case 0:
                            if (!iResource[2 * Constants.SLOTS_PER_DAY + s].isEmpty()
                                    && !iResource[4 * Constants.SLOTS_PER_DAY + s].isEmpty())
                                ret += factor * Constants.sPreferenceLevelDiscouraged;
                            break;
                        case 1:
                            if (!iResource[3 * Constants.SLOTS_PER_DAY + s].isEmpty())
                                ret += factor * Constants.sPreferenceLevelDiscouraged;
                            break;
                        case 2:
                            if (!iResource[0 * Constants.SLOTS_PER_DAY + s].isEmpty()
                                    && !iResource[4 * Constants.SLOTS_PER_DAY + s].isEmpty())
                                ret += factor * Constants.sPreferenceLevelDiscouraged;
                            break;
                        case 3:
                            if (!iResource[1 * Constants.SLOTS_PER_DAY + s].isEmpty())
                                ret += factor * Constants.sPreferenceLevelDiscouraged;
                            break;
                        case 4:
                            if (!iResource[0 * Constants.SLOTS_PER_DAY + s].isEmpty()
                                    && !iResource[2 * Constants.SLOTS_PER_DAY + s].isEmpty())
                                ret += factor * Constants.sPreferenceLevelDiscouraged;
                            break;
                    }
                }
            }
        }
        iCacheUselessSlots = new Integer(Math.round(ret));
        return iCacheUselessSlots.intValue();
    }

    /** Number of useless slots for this room */
    public int countUselessSlotsHalfHours() {
        if (iCacheUselessSlotsHalfHours != null)
            return iCacheUselessSlotsHalfHours.intValue();
        int ret = 0;
        for (int d = 0; d < Constants.NR_DAYS; d++) {
            for (int s = 0; s < Constants.SLOTS_PER_DAY; s++) {
                int slot = d * Constants.SLOTS_PER_DAY + s;
                if (isUseless(slot))
                    ret++;
            }
        }
        iCacheUselessSlotsHalfHours = new Integer(ret);
        return ret;
    }

    /** Number of useless slots for this room */
    public int countUselessSlotsBrokenTimePatterns() {
        if (iCacheBrokenTimePatterns != null)
            return iCacheBrokenTimePatterns.intValue();
        int ret = 0;
        for (int d = 0; d < Constants.NR_DAYS; d++) {
            for (int s = 0; s < Constants.SLOTS_PER_DAY; s++) {
                int slot = d * Constants.SLOTS_PER_DAY + s;
                if (iResource[slot].isEmpty()) {
                    switch (d) {
                        case 0:
                            if (!iResource[2 * Constants.SLOTS_PER_DAY + s].isEmpty()
                                    && !iResource[4 * Constants.SLOTS_PER_DAY + s].isEmpty())
                                ret++;
                            break;
                        case 1:
                            if (!iResource[3 * Constants.SLOTS_PER_DAY + s].isEmpty())
                                ret++;
                            break;
                        case 2:
                            if (!iResource[0 * Constants.SLOTS_PER_DAY + s].isEmpty()
                                    && !iResource[4 * Constants.SLOTS_PER_DAY + s].isEmpty())
                                ret++;
                            break;
                        case 3:
                            if (!iResource[1 * Constants.SLOTS_PER_DAY + s].isEmpty())
                                ret++;
                            break;
                        case 4:
                            if (!iResource[0 * Constants.SLOTS_PER_DAY + s].isEmpty()
                                    && !iResource[2 * Constants.SLOTS_PER_DAY + s].isEmpty())
                                ret++;
                            break;
                    }
                }
            }
        }
        iCacheBrokenTimePatterns = new Integer(Math.round((1.0f / 6.0f) * ret));
        return iCacheBrokenTimePatterns.intValue();
    }

    public void clearCache() {
        iCacheBrokenTimePatterns = null;
        iCacheUselessSlots = null;
        iCacheUselessSlotsHalfHours = null;
    }

    private static int sDaysMWF = Constants.DAY_CODES[0] + Constants.DAY_CODES[2] + Constants.DAY_CODES[4];
    private static int sDaysTTh = Constants.DAY_CODES[1] + Constants.DAY_CODES[3];

    /** Number of useless slots for this room */
    public int countUselessSlots(Placement placement) {
        float ret = 0;
        float factor = 1.0f / 6.0f;
        int slot = placement.getTimeLocation().getStartSlot() % Constants.SLOTS_PER_DAY;
        int days = placement.getTimeLocation().getDayCode();
        for (int d = 0; d < Constants.NR_DAYS; d++) {
            if ((Constants.DAY_CODES[d] & days) == 0)
                continue;
            if (isUselessBefore(d * Constants.SLOTS_PER_DAY + slot - 6))
                ret += Constants.sPreferenceLevelStronglyDiscouraged;
            if (isUselessAfter(d * Constants.SLOTS_PER_DAY + slot + placement.getTimeLocation().getNrSlotsPerMeeting()))
                ret += Constants.sPreferenceLevelStronglyDiscouraged;
        }
        if ((days & sDaysMWF) != 0 && (days & sDaysMWF) != sDaysMWF) {
            for (int s = slot; s < slot + placement.getTimeLocation().getLength(); s++) {
                int nrEmpty = 0;
                if ((Constants.DAY_CODES[0] & days) == 0 && iResource[0 * Constants.SLOTS_PER_DAY + s].isEmpty())
                    nrEmpty++;
                if ((Constants.DAY_CODES[2] & days) == 0 && iResource[2 * Constants.SLOTS_PER_DAY + s].isEmpty())
                    nrEmpty++;
                if ((Constants.DAY_CODES[4] & days) == 0 && iResource[4 * Constants.SLOTS_PER_DAY + s].isEmpty())
                    nrEmpty++;
                if (nrEmpty > 0)
                    ret += factor * Constants.sPreferenceLevelDiscouraged;
            }
        }
        if ((days & sDaysTTh) != 0 && (days & sDaysTTh) != sDaysTTh) {
            for (int s = slot; s < slot + placement.getTimeLocation().getLength(); s++) {
                int nrEmpty = 0;
                if ((Constants.DAY_CODES[1] & days) == 0 && iResource[1 * Constants.SLOTS_PER_DAY + s].isEmpty())
                    nrEmpty++;
                if ((Constants.DAY_CODES[3] & days) == 0 && iResource[3 * Constants.SLOTS_PER_DAY + s].isEmpty())
                    nrEmpty++;
                if (nrEmpty > 0)
                    ret += factor * Constants.sPreferenceLevelDiscouraged;
            }
        }
        return Math.round(ret);
    }

    /** Room usage */
    protected void printUsage(StringBuffer sb) {
        for (int slot = 0; slot < iResource.length; slot++) {
            for (Placement p : iResource[slot]) {
                int day = slot / Constants.SLOTS_PER_DAY;
                int time = slot * Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN;
                int h = time / 60;
                int m = time % 60;
                String d = Constants.DAY_NAMES_SHORT[day];
                int slots = p.getTimeLocation().getLength();
                time += (30 * slots);
                int h2 = time / 60;
                int m2 = time % 60;
                sb.append(sb.length() == 0 ? "" : ",\n        ").append(
                        "[" + d + (h > 12 ? h - 12 : h) + ":" + (m < 10 ? "0" : "") + m + (h >= 12 ? "p" : "a") + "-"
                                + (h2 > 12 ? h2 - 12 : h2) + ":" + (m2 < 10 ? "0" : "") + m2 + (h2 >= 12 ? "p" : "a")
                                + "]=").append(p.variable().getName());
                slot += slots - 1;
                // sb.append(sb.length()==0?"":", ").append("s"+(slot+1)+"=").append(((Lecture)getResource()[slot]).getName());
            }
        }
    }

    @Override
    public String toString() {
        return "Room " + getName();
    }

    /** Position of the building */
    public void setCoordinates(Double x, Double y) {
        iPosX = x;
        iPosY = y;
    }

    /** X-position of the building */
    public Double getPosX() {
        return iPosX;
    }

    /** Y-position of the building */
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
}
