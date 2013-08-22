package net.sf.cpsolver.coursett.constraint;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.coursett.Constants;
import net.sf.cpsolver.coursett.criteria.BackToBackInstructorPreferences;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.util.DistanceMetric;

/**
 * Instructor constraint. <br>
 * Classes with this instructor can not overlap in time. Also, for back-to-back
 * classes, there is the following reasoning:
 * <ul>
 * <li>if the distance is equal or below
 * {@link DistanceMetric#getInstructorNoPreferenceLimit()} .. no preference
 * <li>if the distance is above
 * {@link DistanceMetric#getInstructorNoPreferenceLimit()} and below
 * {@link DistanceMetric#getInstructorDiscouragedLimit()} .. constraint is
 * discouraged (soft, preference = 1)
 * <li>if the distance is above
 * {@link DistanceMetric#getInstructorDiscouragedLimit()} and below
 * {@link DistanceMetric#getInstructorProhibitedLimit()} .. constraint is
 * strongly discouraged (soft, preference = 2)
 * <li>if the distance is above
 * {@link DistanceMetric#getInstructorProhibitedLimit()} .. constraint is
 * prohibited (hard)
 * </ul>
 * <br>
 * When {@link InstructorConstraint#isIgnoreDistances()} is set to true, the
 * constraint never prohibits two back-to-back classes (but it still tries to
 * minimize the above back-to-back preferences).
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

public class InstructorConstraint extends Constraint<Lecture, Placement> {

    public int iPreference = 0;

    /**
     * table iResource[slot] = lecture using this resource placed in the given
     * time slot (null if empty)
     */
    protected List<Placement>[] iResource;
    private Long iResourceId;
    private String iName;
    private String iPuid;
    private List<Placement> iUnavailabilities = null;
    private boolean iIgnoreDistances = false;
    private Long iType = null;

    /**
     * Constructor
     * 
     * @param id
     *            instructor id
     * @param name
     *            instructor name
     */
    @SuppressWarnings("unchecked")
    public InstructorConstraint(Long id, String puid, String name, boolean ignDist) {
        iResourceId = id;
        iName = name;
        iPuid = puid;
        iIgnoreDistances = ignDist;
        iResource = new List[Constants.SLOTS_PER_DAY * Constants.DAY_CODES.length];
        for (int i = 0; i < iResource.length; i++)
            iResource[i] = new ArrayList<Placement>(3);
    }

    public List<Placement> getPlacements(int slot, Placement placement) {
        return getPlacements(slot, placement.getTimeLocation().getWeekCode());
    }

    public List<Placement> getPlacements(int slot, BitSet weekCode) {
        List<Placement> placements = new ArrayList<Placement>(iResource[slot].size());
        for (Placement p : iResource[slot]) {
            if (p.getTimeLocation().shareWeeks(weekCode))
                placements.add(p);
        }
        return placements;
    }

    public Placement getPlacement(int slot, int day) {
        for (Placement p : iResource[slot]) {
            if (p.getTimeLocation().hasDay(day))
                return p;
        }
        return null;
    }
    
    public void setNotAvailable(Placement placement) {
        if (iUnavailabilities == null)
            iUnavailabilities = new ArrayList<Placement>();
        iUnavailabilities.add(placement);
        for (Lecture lecture: variables())
            lecture.clearValueCache();
    }

    public boolean isAvailable(Lecture lecture, TimeLocation time) {
        if (iUnavailabilities == null) return true;
        for (Placement c: iUnavailabilities) {
            if (c.getTimeLocation().hasIntersection(time) && !lecture.canShareRoom(c.variable())) return false;
        }
        return true;
    }
    
    private DistanceMetric getDistanceMetric() {
        return ((TimetableModel)getModel()).getDistanceMetric();
    }

    public boolean isAvailable(Lecture lecture, Placement placement) {
        if (iUnavailabilities == null) return true;
        TimeLocation t1 = placement.getTimeLocation();
        for (Placement c: iUnavailabilities) {
            if (c.getTimeLocation().hasIntersection(placement.getTimeLocation()) && (!lecture.canShareRoom(c.variable()) || !placement.sameRooms(c)))
                return false;
            if (!iIgnoreDistances) {
                TimeLocation t2 = c.getTimeLocation();
                if (t1.shareDays(t2) && t1.shareWeeks(t2)) {
                    if (t1.getStartSlot() + t1.getLength() == t2.getStartSlot() || t2.getStartSlot() + t2.getLength() == t1.getStartSlot()) {
                        if (Placement.getDistanceInMeters(getDistanceMetric(), placement, c) > getDistanceMetric().getInstructorProhibitedLimit())
                            return false;
                    } else if (getDistanceMetric().doComputeDistanceConflictsBetweenNonBTBClasses()) {
                        if (t1.getStartSlot() + t1.getLength() < t2.getStartSlot()) {
                            if (Placement.getDistanceInMinutes(getDistanceMetric(), placement, c) > t1.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t2.getStartSlot() - t1.getStartSlot() - t1.getLength()))
                                return false;
                        } else if (t2.getStartSlot() + t2.getLength() < t1.getStartSlot()) {
                            if (Placement.getDistanceInMinutes(getDistanceMetric(), placement, c) > t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t1.getStartSlot() - t2.getStartSlot() - t2.getLength()))
                                return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public List<Placement> getUnavailabilities() {
        return iUnavailabilities;
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    public List<Placement>[] getAvailableArray() {
        if (iUnavailabilities == null) return null;
        List<Placement>[] available = new List[Constants.SLOTS_PER_DAY * Constants.DAY_CODES.length];
        for (int i = 0; i < iResource.length; i++)
            available[i] = null;
        for (Placement p: iUnavailabilities) {
            for (Enumeration<Integer> e = p.getTimeLocation().getSlots(); e.hasMoreElements();) {
                int slot = e.nextElement();
                if (available[slot] == null)
                    available[slot] = new ArrayList<Placement>(1);
                available[slot].add(p);
            }
        }
        return available;
    }

    /** Back-to-back preference of two placements (3 means prohibited) */
    public int getDistancePreference(Placement p1, Placement p2) {
        TimeLocation t1 = p1.getTimeLocation();
        TimeLocation t2 = p2.getTimeLocation();
        if (t1 == null || t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2))
            return Constants.sPreferenceLevelNeutral;
        if (t1.getStartSlot() + t1.getLength() == t2.getStartSlot() || t2.getStartSlot() + t2.getLength() == t1.getStartSlot()) {
            double distance = Placement.getDistanceInMeters(getDistanceMetric(), p1, p2);
            if (distance <= getDistanceMetric().getInstructorNoPreferenceLimit())
                return Constants.sPreferenceLevelNeutral;
            if (distance <= getDistanceMetric().getInstructorDiscouragedLimit())
                return Constants.sPreferenceLevelDiscouraged;
            if (iIgnoreDistances || distance <= getDistanceMetric().getInstructorProhibitedLimit())
                return Constants.sPreferenceLevelStronglyDiscouraged;
            return Constants.sPreferenceLevelProhibited;
        } else if (getDistanceMetric().doComputeDistanceConflictsBetweenNonBTBClasses()) {
            if (t1.getStartSlot() + t1.getLength() < t2.getStartSlot()) {
                int distanceInMinutes = Placement.getDistanceInMinutes(getDistanceMetric(), p1, p2);
                if (distanceInMinutes > t1.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t2.getStartSlot() - t1.getStartSlot() - t1.getLength())) // too far apart
                    return (iIgnoreDistances ? Constants.sPreferenceLevelStronglyDiscouraged : Constants.sPreferenceLevelProhibited);
                if (distanceInMinutes >= getDistanceMetric().getInstructorLongTravelInMinutes()) // long travel
                    return Constants.sPreferenceLevelStronglyDiscouraged;
                if (distanceInMinutes > Constants.SLOT_LENGTH_MIN * (t2.getStartSlot() - t1.getStartSlot() - t1.getLength())) // too far if no break time
                    return Constants.sPreferenceLevelDiscouraged;
            } else if (t2.getStartSlot() + t2.getLength() < t1.getStartSlot()) {
                int distanceInMinutes = Placement.getDistanceInMinutes(getDistanceMetric(), p1, p2);
                if (distanceInMinutes > t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t1.getStartSlot() - t2.getStartSlot() - t2.getLength())) // too far apart
                    return (iIgnoreDistances ? Constants.sPreferenceLevelStronglyDiscouraged : Constants.sPreferenceLevelProhibited);
                if (distanceInMinutes >= getDistanceMetric().getInstructorLongTravelInMinutes()) // long travel
                    return Constants.sPreferenceLevelStronglyDiscouraged;
                if (distanceInMinutes > Constants.SLOT_LENGTH_MIN * (t1.getStartSlot() - t2.getStartSlot() - t2.getLength())) // too far if no break time
                    return Constants.sPreferenceLevelDiscouraged;
            }
        } 
        return Constants.sPreferenceLevelNeutral; 
    }

    /** Resource id */
    public Long getResourceId() {
        return iResourceId;
    }

    /** Resource name */
    @Override
    public String getName() {
        return iName;
    }

    @Override
    public void computeConflicts(Placement placement, Set<Placement> conflicts) {
        Lecture lecture = placement.variable();
        BitSet weekCode = placement.getTimeLocation().getWeekCode();

        for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
            int slot = e.nextElement();
            for (Placement p : iResource[slot]) {
                if (!p.equals(lecture.getAssignment()) && p.getTimeLocation().shareWeeks(weekCode)) {
                    if (p.canShareRooms(placement) && p.sameRooms(placement))
                        continue;
                    conflicts.add(p);
                }
            }
        }
        if (!iIgnoreDistances) {
            for (Enumeration<Integer> e = placement.getTimeLocation().getStartSlots(); e.hasMoreElements();) {
                int startSlot = e.nextElement();

                int prevSlot = startSlot - 1;
                if (prevSlot >= 0 && (prevSlot / Constants.SLOTS_PER_DAY) == (startSlot / Constants.SLOTS_PER_DAY)) {
                    for (Placement c : getPlacements(prevSlot, placement)) {
                        if (lecture.equals(c.variable())) continue;
                        if (c.canShareRooms(placement) && c.sameRooms(placement)) continue;
                        if (Placement.getDistanceInMeters(getDistanceMetric(), placement, c) > getDistanceMetric().getInstructorProhibitedLimit())
                            conflicts.add(c);
                    }
                }
                int nextSlot = startSlot + placement.getTimeLocation().getLength();
                if ((nextSlot / Constants.SLOTS_PER_DAY) == (startSlot / Constants.SLOTS_PER_DAY)) {
                    for (Placement c : getPlacements(nextSlot, placement)) {
                        if (lecture.equals(c.variable())) continue;
                        if (c.canShareRooms(placement) && c.sameRooms(placement)) continue;
                        if (Placement.getDistanceInMeters(getDistanceMetric(), placement, c) > getDistanceMetric().getInstructorProhibitedLimit())
                            conflicts.add(c);
                    }
                }
                
                if (getDistanceMetric().doComputeDistanceConflictsBetweenNonBTBClasses()) {
                    TimeLocation t1 = placement.getTimeLocation();
                    for (Lecture other: assignedVariables()) {
                        if (other.getAssignment() == null || other.equals(placement.variable())) continue;
                        TimeLocation t2 = other.getAssignment().getTimeLocation();
                        if (t1 == null || t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) continue;
                        if (t1.getStartSlot() + t1.getLength() < t2.getStartSlot()) {
                            if (Placement.getDistanceInMinutes(getDistanceMetric(), placement, other.getAssignment()) > t1.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t2.getStartSlot() - t1.getStartSlot() - t1.getLength()))
                                conflicts.add(other.getAssignment());
                        } else if (t2.getStartSlot() + t2.getLength() < t1.getStartSlot()) {
                            if (Placement.getDistanceInMinutes(getDistanceMetric(), placement, other.getAssignment()) >  t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t1.getStartSlot() - t2.getStartSlot() - t2.getLength()))
                                conflicts.add(other.getAssignment());
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean inConflict(Placement placement) {
        Lecture lecture = placement.variable();
        BitSet weekCode = placement.getTimeLocation().getWeekCode();
        for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
            int slot = e.nextElement();
            for (Placement p : iResource[slot]) {
                if (!p.equals(lecture.getAssignment()) && p.getTimeLocation().shareWeeks(weekCode)) {
                    if (p.canShareRooms(placement) && p.sameRooms(placement))
                        continue;
                    return true;
                }
            }
        }
        if (!iIgnoreDistances) {
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
                    for (Lecture other: assignedVariables()) {
                        if (other.getAssignment() == null || other.equals(placement.variable())) continue;
                        TimeLocation t2 = other.getAssignment().getTimeLocation();
                        if (t1 == null || t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) continue;
                        if (t1.getStartSlot() + t1.getLength() < t2.getStartSlot()) {
                            if (Placement.getDistanceInMinutes(getDistanceMetric(), placement, other.getAssignment()) > t1.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t2.getStartSlot() - t1.getStartSlot() - t1.getLength()))
                                return true;
                        } else if (t2.getStartSlot() + t2.getLength() < t1.getStartSlot()) {
                            if (Placement.getDistanceInMinutes(getDistanceMetric(), placement, other.getAssignment()) >  t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t1.getStartSlot() - t2.getStartSlot() - t2.getLength()))
                                return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isConsistent(Placement p1, Placement p2) {
        if (p1.canShareRooms(p2) && p1.sameRooms(p2))
            return true;
        if (p1.getTimeLocation().hasIntersection(p2.getTimeLocation()))
            return false;
        return getDistancePreference(p1, p2) != Constants.sPreferenceLevelProhibited;
    }

    @Override
    public void assigned(long iteration, Placement placement) {
        super.assigned(iteration, placement);
        // iPreference += getPreference(placement);
        for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
            int slot = e.nextElement();
            iResource[slot].add(placement);
        }
        getModel().getCriterion(BackToBackInstructorPreferences.class).inc(-iPreference);
        iPreference = countPreference();
        getModel().getCriterion(BackToBackInstructorPreferences.class).inc(iPreference);
    }

    @Override
    public void unassigned(long iteration, Placement placement) {
        super.unassigned(iteration, placement);
        // iPreference -= getPreference(placement);
        for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
            int slot = e.nextElement();
            iResource[slot].remove(placement);
        }
        getModel().getCriterion(BackToBackInstructorPreferences.class).inc(-iPreference);
        iPreference = countPreference();
        getModel().getCriterion(BackToBackInstructorPreferences.class).inc(iPreference);
    }

    /**
     * Lookup table getResource()[slot] -> lecture using this resource placed in
     * the given time slot (null if empty)
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

    /** Number of useless slots for this resource */
    public int countUselessSlots() {
        int ret = 0;
        for (int d = 0; d < Constants.DAY_CODES.length; d++) {
            for (int s = 1; s < Constants.SLOTS_PER_DAY - 1; s++) {
                int slot = d * Constants.SLOTS_PER_DAY + s;
                if (iResource[slot - 1] != null && iResource[slot] == null && iResource[slot + 1] != null)
                    ret++;
            }
        }
        return ret;
    }

    /** Resource usage usage */
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
        return "Instructor " + getName();
    }

    /** Back-to-back preference of the given placement */
    public int getPreference(Placement value) {
        Lecture lecture = value.variable();
        Placement placement = value;
        int pref = 0;
        HashSet<Placement> checked = new HashSet<Placement>();
        
        for (Enumeration<Integer> e = placement.getTimeLocation().getStartSlots(); e.hasMoreElements();) {
            int startSlot = e.nextElement();
            
            int prevSlot = startSlot - 1;
            if (prevSlot >= 0 && (prevSlot / Constants.SLOTS_PER_DAY) == (startSlot / Constants.SLOTS_PER_DAY)) {
                for (Placement c : getPlacements(prevSlot, placement)) {
                    if (lecture.equals(c.variable()) || !checked.add(c)) continue;
                    double dist = Placement.getDistanceInMeters(getDistanceMetric(), placement, c);
                    if (dist > getDistanceMetric().getInstructorNoPreferenceLimit() && dist <= getDistanceMetric().getInstructorDiscouragedLimit())
                        pref += Constants.sPreferenceLevelDiscouraged;
                    if (dist > getDistanceMetric().getInstructorDiscouragedLimit()
                            && (dist <= getDistanceMetric().getInstructorProhibitedLimit() || iIgnoreDistances))
                        pref += Constants.sPreferenceLevelStronglyDiscouraged;
                    if (!iIgnoreDistances && dist > getDistanceMetric().getInstructorProhibitedLimit())
                        pref += Constants.sPreferenceLevelProhibited;
                }
            }
            int nextSlot = startSlot + placement.getTimeLocation().getLength();
            if ((nextSlot / Constants.SLOTS_PER_DAY) == (startSlot / Constants.SLOTS_PER_DAY)) {
                for (Placement c : getPlacements(nextSlot, placement)) {
                    if (lecture.equals(c.variable()) || !checked.add(c)) continue;
                    double dist = Placement.getDistanceInMeters(getDistanceMetric(), placement, c);
                    if (dist > getDistanceMetric().getInstructorNoPreferenceLimit() && dist <= getDistanceMetric().getInstructorDiscouragedLimit())
                        pref += Constants.sPreferenceLevelDiscouraged;
                    if (dist > getDistanceMetric().getInstructorDiscouragedLimit()
                            && (dist <= getDistanceMetric().getInstructorProhibitedLimit() || iIgnoreDistances))
                        pref += Constants.sPreferenceLevelStronglyDiscouraged;
                    if (!iIgnoreDistances && dist > getDistanceMetric().getInstructorProhibitedLimit())
                        pref = Constants.sPreferenceLevelProhibited;
                }
            }
            
            if (getDistanceMetric().doComputeDistanceConflictsBetweenNonBTBClasses()) {
                TimeLocation t1 = placement.getTimeLocation();
                Placement before = null, after = null;
                for (Lecture other: assignedVariables()) {
                    if (other.getAssignment() == null || other.equals(placement.variable())) continue;
                    TimeLocation t2 = other.getAssignment().getTimeLocation();
                    if (t1 == null || t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) continue;
                    if (t1.getStartSlot() + t1.getLength() < t2.getStartSlot()) {
                        int distanceInMinutes = Placement.getDistanceInMinutes(getDistanceMetric(), placement, other.getAssignment());
                        if (distanceInMinutes > t1.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t2.getStartSlot() - t1.getStartSlot() - t1.getLength()))
                            pref += (iIgnoreDistances ? Constants.sPreferenceLevelStronglyDiscouraged : Constants.sPreferenceLevelProhibited);
                        else if (distanceInMinutes > Constants.SLOT_LENGTH_MIN * (t2.getStartSlot() - t1.getStartSlot() - t1.getLength()))
                            pref += Constants.sPreferenceLevelDiscouraged;
                    } else if (t2.getStartSlot() + t2.getLength() < t1.getStartSlot()) {
                        int distanceInMinutes = Placement.getDistanceInMinutes(getDistanceMetric(), placement, other.getAssignment());
                        if (distanceInMinutes >  t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t1.getStartSlot() - t2.getStartSlot() - t2.getLength()))
                            pref += (iIgnoreDistances ? Constants.sPreferenceLevelStronglyDiscouraged : Constants.sPreferenceLevelProhibited);
                        else if (distanceInMinutes > Constants.SLOT_LENGTH_MIN * (t1.getStartSlot() - t2.getStartSlot() - t2.getLength()))
                            pref += Constants.sPreferenceLevelDiscouraged;
                    }
                    if (t1.getStartSlot() + t1.getLength() <= t2.getStartSlot()) {
                        if (after == null || t2.getStartSlot() < after.getTimeLocation().getStartSlot())
                            after = other.getAssignment();
                    } else if (t2.getStartSlot() + t2.getLength() <= t1.getStartSlot()) {
                        if (before == null || before.getTimeLocation().getStartSlot() < t2.getStartSlot())
                            before = other.getAssignment();
                    }
                }
                if (iUnavailabilities != null) {
                    for (Placement c: iUnavailabilities) {
                        TimeLocation t2 = c.getTimeLocation();
                        if (t1 == null || t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) continue;
                        if (t1.getStartSlot() + t1.getLength() <= t2.getStartSlot()) {
                            if (after == null || t2.getStartSlot() < after.getTimeLocation().getStartSlot())
                                after = c;
                        } else if (t2.getStartSlot() + t2.getLength() <= t1.getStartSlot()) {
                            if (before == null || before.getTimeLocation().getStartSlot() < t2.getStartSlot())
                                before = c;
                        }
                    }
                }
                if (before != null && Placement.getDistanceInMinutes(getDistanceMetric(), before, placement) > getDistanceMetric().getInstructorLongTravelInMinutes())
                    pref += Constants.sPreferenceLevelStronglyDiscouraged;
                if (after != null && Placement.getDistanceInMinutes(getDistanceMetric(), after, placement) > getDistanceMetric().getInstructorLongTravelInMinutes())
                    pref += Constants.sPreferenceLevelStronglyDiscouraged;
                if (before != null && after != null && Placement.getDistanceInMinutes(getDistanceMetric(), before, after) > getDistanceMetric().getInstructorLongTravelInMinutes())
                    pref -= Constants.sPreferenceLevelStronglyDiscouraged;
            }
        }
        return pref;
    }

    public int getPreferenceCombination(Placement value) {
        Lecture lecture = value.variable();
        Placement placement = value;
        int pref = 0;
        HashSet<Placement> checked = new HashSet<Placement>();
        
        for (Enumeration<Integer> e = placement.getTimeLocation().getStartSlots(); e.hasMoreElements();) {
            int startSlot = e.nextElement();
            
            int prevSlot = startSlot - 1;
            if (prevSlot >= 0 && (prevSlot / Constants.SLOTS_PER_DAY) == (startSlot / Constants.SLOTS_PER_DAY)) {
                for (Placement c : getPlacements(prevSlot, placement)) {
                    if (lecture.equals(c.variable()) || !checked.add(c)) continue;
                    double dist = Placement.getDistanceInMeters(getDistanceMetric(), placement, c);
                    if (dist > getDistanceMetric().getInstructorNoPreferenceLimit() && dist <= getDistanceMetric().getInstructorDiscouragedLimit())
                        pref = Math.max(pref, Constants.sPreferenceLevelDiscouraged);
                    if (dist > getDistanceMetric().getInstructorDiscouragedLimit()
                            && (dist <= getDistanceMetric().getInstructorProhibitedLimit() || iIgnoreDistances))
                        pref = Math.max(pref, Constants.sPreferenceLevelStronglyDiscouraged);
                    if (!iIgnoreDistances && dist > getDistanceMetric().getInstructorProhibitedLimit())
                        pref = Math.max(pref, Constants.sPreferenceLevelProhibited);
                }
            }
            int nextSlot = startSlot + placement.getTimeLocation().getLength();
            if ((nextSlot / Constants.SLOTS_PER_DAY) == (startSlot / Constants.SLOTS_PER_DAY)) {
                for (Placement c : getPlacements(nextSlot, placement)) {
                    if (lecture.equals(c.variable()) || !checked.add(c)) continue;
                    double dist = Placement.getDistanceInMeters(getDistanceMetric(), placement, c);
                    if (dist > getDistanceMetric().getInstructorNoPreferenceLimit() && dist <= getDistanceMetric().getInstructorDiscouragedLimit())
                        pref = Math.max(pref, Constants.sPreferenceLevelDiscouraged);
                    if (dist > getDistanceMetric().getInstructorDiscouragedLimit()
                            && (dist <= getDistanceMetric().getInstructorProhibitedLimit() || iIgnoreDistances))
                        pref = Math.max(pref, Constants.sPreferenceLevelStronglyDiscouraged);
                    if (!iIgnoreDistances && dist > getDistanceMetric().getInstructorProhibitedLimit())
                        pref = Constants.sPreferenceLevelProhibited;
                }
            }
            
            if (getDistanceMetric().doComputeDistanceConflictsBetweenNonBTBClasses()) {
                TimeLocation t1 = placement.getTimeLocation();
                Placement before = null, after = null;
                for (Lecture other: assignedVariables()) {
                    if (other.getAssignment() == null || other.equals(placement.variable())) continue;
                    TimeLocation t2 = other.getAssignment().getTimeLocation();
                    if (t1 == null || t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) continue;
                    if (t1.getStartSlot() + t1.getLength() < t2.getStartSlot()) {
                        int distanceInMinutes = Placement.getDistanceInMinutes(getDistanceMetric(), placement, other.getAssignment());
                        if (distanceInMinutes > t1.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t2.getStartSlot() - t1.getStartSlot() - t1.getLength()))
                            pref = Math.max(pref, (iIgnoreDistances ? Constants.sPreferenceLevelStronglyDiscouraged : Constants.sPreferenceLevelProhibited));
                        else if (distanceInMinutes > Constants.SLOT_LENGTH_MIN * (t2.getStartSlot() - t1.getStartSlot() - t1.getLength()))
                            pref = Math.max(pref, Constants.sPreferenceLevelDiscouraged);
                    } else if (t2.getStartSlot() + t2.getLength() < t1.getStartSlot()) {
                        int distanceInMinutes = Placement.getDistanceInMinutes(getDistanceMetric(), placement, other.getAssignment());
                        if (distanceInMinutes >  t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t1.getStartSlot() - t2.getStartSlot() - t2.getLength()))
                            pref = Math.max(pref, (iIgnoreDistances ? Constants.sPreferenceLevelStronglyDiscouraged : Constants.sPreferenceLevelProhibited));
                        else if (distanceInMinutes > Constants.SLOT_LENGTH_MIN * (t1.getStartSlot() - t2.getStartSlot() - t2.getLength()))
                            pref = Math.max(pref, Constants.sPreferenceLevelDiscouraged);
                    }
                    if (t1.getStartSlot() + t1.getLength() <= t2.getStartSlot()) {
                        if (after == null || t2.getStartSlot() < after.getTimeLocation().getStartSlot())
                            after = other.getAssignment();
                    } else if (t2.getStartSlot() + t2.getLength() <= t1.getStartSlot()) {
                        if (before == null || before.getTimeLocation().getStartSlot() < t2.getStartSlot())
                            before = other.getAssignment();
                    }
                }
                if (iUnavailabilities != null) {
                    for (Placement c: iUnavailabilities) {
                        TimeLocation t2 = c.getTimeLocation();
                        if (t1 == null || t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) continue;
                        if (t1.getStartSlot() + t1.getLength() <= t2.getStartSlot()) {
                            if (after == null || t2.getStartSlot() < after.getTimeLocation().getStartSlot())
                                after = c;
                        } else if (t2.getStartSlot() + t2.getLength() <= t1.getStartSlot()) {
                            if (before == null || before.getTimeLocation().getStartSlot() < t2.getStartSlot())
                                before = c;
                        }
                    }
                }
                int tooLongTravel = 0;
                if (before != null && Placement.getDistanceInMinutes(getDistanceMetric(), before, placement) > getDistanceMetric().getInstructorLongTravelInMinutes())
                    tooLongTravel++;
                if (after != null && Placement.getDistanceInMinutes(getDistanceMetric(), after, placement) > getDistanceMetric().getInstructorLongTravelInMinutes())
                    tooLongTravel++;
                if (tooLongTravel > 0)
                    pref += Math.max(pref, Constants.sPreferenceLevelStronglyDiscouraged);
            }
        }
        return pref;
    }

    /** Overall back-to-back preference of this instructor */
    public int getPreference() {
        /*
         * if (iPreference!=countPreference()) {System.err.println(
         * "InstructorConstraint.getPreference() is not working properly"); }
         */
        return iPreference;
    }

    public int countPreference() {
        int pref = 0;
        HashSet<Placement> checked = new HashSet<Placement>();
        
        for (int slot = 1; slot < iResource.length; slot++) {
            if ((slot % Constants.SLOTS_PER_DAY) == 0) continue;
            for (Placement placement : iResource[slot]) {
                for (Placement c : getPlacements(slot - 1, placement)) {
                    if (placement.variable().equals(c.variable()) || !checked.add(c)) continue;
                    double dist = Placement.getDistanceInMeters(getDistanceMetric(), c, placement);
                    if (dist > getDistanceMetric().getInstructorNoPreferenceLimit() && dist <= getDistanceMetric().getInstructorDiscouragedLimit())
                        pref += Constants.sPreferenceLevelDiscouraged;
                    if (dist > getDistanceMetric().getInstructorDiscouragedLimit())
                        pref += Constants.sPreferenceLevelStronglyDiscouraged;
                }
            }
        }
        
        if (getDistanceMetric().doComputeDistanceConflictsBetweenNonBTBClasses()) {
            for (Lecture p1: assignedVariables()) {
                TimeLocation t1 = (p1.getAssignment() == null ? null : p1.getAssignment().getTimeLocation());
                if (t1 == null) continue;
                Placement before = null;
                for (Lecture p2: assignedVariables()) {
                    if (p2.getAssignment() == null || p2.equals(p1)) continue;
                    TimeLocation t2 = p2.getAssignment().getTimeLocation();
                    if (t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) continue;
                    if (t2.getStartSlot() + t2.getLength() < t1.getStartSlot()) {
                        int distanceInMinutes = Placement.getDistanceInMinutes(getDistanceMetric(), p1.getAssignment(), p2.getAssignment());
                        if (distanceInMinutes >  t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t1.getStartSlot() - t2.getStartSlot() - t2.getLength()))
                            pref += (iIgnoreDistances ? Constants.sPreferenceLevelStronglyDiscouraged : Constants.sPreferenceLevelProhibited);
                        else if (distanceInMinutes > Constants.SLOT_LENGTH_MIN * (t1.getStartSlot() - t2.getStartSlot() - t2.getLength()))
                            pref += Constants.sPreferenceLevelDiscouraged;
                    }
                    if (t2.getStartSlot() + t2.getLength() <= t1.getStartSlot()) {
                        if (before == null || before.getTimeLocation().getStartSlot() < t2.getStartSlot())
                            before = p2.getAssignment();
                    }
                }
                if (iUnavailabilities != null) {
                    for (Placement c: iUnavailabilities) {
                        TimeLocation t2 = c.getTimeLocation();
                        if (t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) continue;
                        if (t2.getStartSlot() + t2.getLength() <= t1.getStartSlot()) {
                            if (before == null || before.getTimeLocation().getStartSlot() < t2.getStartSlot())
                                before = c;
                        }
                    }
                }
                if (before != null && Placement.getDistanceInMinutes(getDistanceMetric(), before, p1.getAssignment()) > getDistanceMetric().getInstructorLongTravelInMinutes())
                    pref += Constants.sPreferenceLevelStronglyDiscouraged;
            }
        }

        return pref;
    }

    /** Worst back-to-back preference of this instructor */
    public int getWorstPreference() {
        return Constants.sPreferenceLevelStronglyDiscouraged * (variables().size() - 1);
    }

    public String getPuid() {
        return iPuid;
    }

    public boolean isIgnoreDistances() {
        return iIgnoreDistances;
    }
    
    public void setIgnoreDistances(boolean ignDist) {
        iIgnoreDistances = ignDist;
    }

    public Long getType() {
        return iType;
    }

    public void setType(Long type) {
        iType = type;
    }
}
