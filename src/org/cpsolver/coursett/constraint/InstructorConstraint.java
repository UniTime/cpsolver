package org.cpsolver.coursett.constraint;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.criteria.BackToBackInstructorPreferences;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;
import org.cpsolver.ifs.util.DistanceMetric;


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

public class InstructorConstraint extends ConstraintWithContext<Lecture, Placement, InstructorConstraint.InstructorConstraintContext> {
    private Long iResourceId;
    private String iName;
    private String iPuid;
    private List<Placement> iUnavailabilities = null;
    private boolean iIgnoreDistances = false;
    private Long iType = null;

    /**
     * Constructor
     * 
     * @param id instructor id
     * @param puid instructor external id
     * @param name instructor name
     * @param ignDist true if distance conflicts are to be ignored 
     */
    public InstructorConstraint(Long id, String puid, String name, boolean ignDist) {
        iResourceId = id;
        iName = name;
        iPuid = puid;
        iIgnoreDistances = ignDist;
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
            if (c.variable().getId() < 0 && lecture.getDepartment() != null && c.variable().getDepartment() != null
                    && !c.variable().getDepartment().equals(lecture.getDepartment())) continue;
            if (c.getTimeLocation().hasIntersection(time) && !lecture.canShareRoom(c.variable())) return false;
        }
        return true;
    }
    
    protected DistanceMetric getDistanceMetric() {
        return ((TimetableModel)getModel()).getDistanceMetric();
    }

    public boolean isAvailable(Lecture lecture, Placement placement) {
        if (iUnavailabilities == null) return true;
        TimeLocation t1 = placement.getTimeLocation();
        for (Placement c: iUnavailabilities) {
            if (c.variable().getId() < 0 && lecture.getDepartment() != null && c.variable().getDepartment() != null
                    && !c.variable().getDepartment().equals(lecture.getDepartment())) continue;
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
        for (int i = 0; i < available.length; i++)
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

    /** Back-to-back preference of two placements (3 means prohibited) 
     * @param p1 first placement
     * @param p2 second placement
     * @return distance preference between the two placements
     **/
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

    /** Resource id 
     * @return instructor unique id
     **/
    public Long getResourceId() {
        return iResourceId;
    }

    /** Resource name */
    @Override
    public String getName() {
        return iName;
    }

    @Override
    public void computeConflicts(Assignment<Lecture, Placement> assignment, Placement placement, Set<Placement> conflicts) {
        Lecture lecture = placement.variable();
        Placement current = assignment.getValue(lecture);
        BitSet weekCode = placement.getTimeLocation().getWeekCode();
        InstructorConstraintContext context = getContext(assignment);

        for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
            int slot = e.nextElement();
            for (Placement p : context.getPlacements(slot)) {
                if (!p.equals(current) && p.getTimeLocation().shareWeeks(weekCode)) {
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
                    for (Placement c : context.getPlacements(prevSlot, placement)) {
                        if (lecture.equals(c.variable())) continue;
                        if (c.canShareRooms(placement) && c.sameRooms(placement)) continue;
                        if (Placement.getDistanceInMeters(getDistanceMetric(), placement, c) > getDistanceMetric().getInstructorProhibitedLimit())
                            conflicts.add(c);
                    }
                }
                int nextSlot = startSlot + placement.getTimeLocation().getLength();
                if ((nextSlot / Constants.SLOTS_PER_DAY) == (startSlot / Constants.SLOTS_PER_DAY)) {
                    for (Placement c : context.getPlacements(nextSlot, placement)) {
                        if (lecture.equals(c.variable())) continue;
                        if (c.canShareRooms(placement) && c.sameRooms(placement)) continue;
                        if (Placement.getDistanceInMeters(getDistanceMetric(), placement, c) > getDistanceMetric().getInstructorProhibitedLimit())
                            conflicts.add(c);
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
                                conflicts.add(otherPlacement);
                        } else if (t2.getStartSlot() + t2.getLength() < t1.getStartSlot()) {
                            if (Placement.getDistanceInMinutes(getDistanceMetric(), placement, otherPlacement) >  t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t1.getStartSlot() - t2.getStartSlot() - t2.getLength()))
                                conflicts.add(otherPlacement);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean inConflict(Assignment<Lecture, Placement> assignment, Placement placement) {
        Lecture lecture = placement.variable();
        Placement current = assignment.getValue(lecture);
        BitSet weekCode = placement.getTimeLocation().getWeekCode();
        InstructorConstraintContext context = getContext(assignment);
        
        for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
            int slot = e.nextElement();
            for (Placement p : context.getPlacements(slot)) {
                if (!p.equals(current) && p.getTimeLocation().shareWeeks(weekCode)) {
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
                    for (Placement c : context.getPlacements(prevSlot, placement)) {
                        if (lecture.equals(c.variable())) continue;
                        if (c.canShareRooms(placement) && c.sameRooms(placement)) continue;
                        if (Placement.getDistanceInMeters(getDistanceMetric(), placement, c) > getDistanceMetric().getInstructorProhibitedLimit())
                            return true;
                    }
                }
                int nextSlot = startSlot + placement.getTimeLocation().getLength();
                if ((nextSlot / Constants.SLOTS_PER_DAY) == (startSlot / Constants.SLOTS_PER_DAY)) {
                    for (Placement c : context.getPlacements(nextSlot, placement)) {
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

    @Override
    public boolean isConsistent(Placement p1, Placement p2) {
        if (p1.canShareRooms(p2) && p1.sameRooms(p2))
            return true;
        if (p1.getTimeLocation().hasIntersection(p2.getTimeLocation()))
            return false;
        return getDistancePreference(p1, p2) != Constants.sPreferenceLevelProhibited;
    }

    @Override
    public String toString() {
        return "Instructor " + getName();
    }

    /** Back-to-back preference of the given placement 
     * @param assignment current assignment
     * @param value placement under consideration
     * @return distance preference for the given placement 
     **/
    public int getPreference(Assignment<Lecture, Placement> assignment, Placement value) {
        Lecture lecture = value.variable();
        Placement placement = value;
        int pref = 0;
        HashSet<Placement> checked = new HashSet<Placement>();
        InstructorConstraintContext context = getContext(assignment);
        
        for (Enumeration<Integer> e = placement.getTimeLocation().getStartSlots(); e.hasMoreElements();) {
            int startSlot = e.nextElement();
            
            int prevSlot = startSlot - 1;
            if (prevSlot >= 0 && (prevSlot / Constants.SLOTS_PER_DAY) == (startSlot / Constants.SLOTS_PER_DAY)) {
                for (Placement c : context.getPlacements(prevSlot, placement)) {
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
                for (Placement c : context.getPlacements(nextSlot, placement)) {
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
        }
            
        if (getDistanceMetric().doComputeDistanceConflictsBetweenNonBTBClasses()) {
            TimeLocation t1 = placement.getTimeLocation();
            Placement before = null, after = null;
            for (Lecture other: variables()) {
                Placement otherPlacement = assignment.getValue(other);
                if (otherPlacement == null || other.equals(placement.variable())) continue;
                TimeLocation t2 = otherPlacement.getTimeLocation();
                if (t1 == null || t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) continue;
                if (t1.getStartSlot() + t1.getLength() < t2.getStartSlot()) {
                    int distanceInMinutes = Placement.getDistanceInMinutes(getDistanceMetric(), placement, otherPlacement);
                    if (distanceInMinutes > t1.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t2.getStartSlot() - t1.getStartSlot() - t1.getLength()))
                        pref += (iIgnoreDistances ? Constants.sPreferenceLevelStronglyDiscouraged : Constants.sPreferenceLevelProhibited);
                    else if (distanceInMinutes > Constants.SLOT_LENGTH_MIN * (t2.getStartSlot() - t1.getStartSlot() - t1.getLength()))
                        pref += Constants.sPreferenceLevelDiscouraged;
                } else if (t2.getStartSlot() + t2.getLength() < t1.getStartSlot()) {
                    int distanceInMinutes = Placement.getDistanceInMinutes(getDistanceMetric(), placement, otherPlacement);
                    if (distanceInMinutes >  t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t1.getStartSlot() - t2.getStartSlot() - t2.getLength()))
                        pref += (iIgnoreDistances ? Constants.sPreferenceLevelStronglyDiscouraged : Constants.sPreferenceLevelProhibited);
                    else if (distanceInMinutes > Constants.SLOT_LENGTH_MIN * (t1.getStartSlot() - t2.getStartSlot() - t2.getLength()))
                        pref += Constants.sPreferenceLevelDiscouraged;
                }
                if (t1.getStartSlot() + t1.getLength() <= t2.getStartSlot()) {
                    if (after == null || t2.getStartSlot() < after.getTimeLocation().getStartSlot())
                        after = otherPlacement;
                } else if (t2.getStartSlot() + t2.getLength() <= t1.getStartSlot()) {
                    if (before == null || before.getTimeLocation().getStartSlot() < t2.getStartSlot())
                        before = otherPlacement;
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
        return pref;
    }
    
    public int getPreference(Assignment<Lecture, Placement> assignment) {
        return getContext(assignment).getPreference();
    }

    public int getPreferenceCombination(Assignment<Lecture, Placement> assignment, Placement value) {
        Lecture lecture = value.variable();
        Placement placement = value;
        int pref = 0;
        HashSet<Placement> checked = new HashSet<Placement>();
        InstructorConstraintContext context = getContext(assignment);
        
        for (Enumeration<Integer> e = placement.getTimeLocation().getStartSlots(); e.hasMoreElements();) {
            int startSlot = e.nextElement();
            
            int prevSlot = startSlot - 1;
            if (prevSlot >= 0 && (prevSlot / Constants.SLOTS_PER_DAY) == (startSlot / Constants.SLOTS_PER_DAY)) {
                for (Placement c : context.getPlacements(prevSlot, placement)) {
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
                for (Placement c : context.getPlacements(nextSlot, placement)) {
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
                for (Lecture other: variables()) {
                    Placement otherPlacement = assignment.getValue(other);
                    if (otherPlacement == null || other.equals(placement.variable())) continue;
                    TimeLocation t2 = otherPlacement.getTimeLocation();
                    if (t1 == null || t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) continue;
                    if (t1.getStartSlot() + t1.getLength() < t2.getStartSlot()) {
                        int distanceInMinutes = Placement.getDistanceInMinutes(getDistanceMetric(), placement, otherPlacement);
                        if (distanceInMinutes > t1.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t2.getStartSlot() - t1.getStartSlot() - t1.getLength()))
                            pref = Math.max(pref, (iIgnoreDistances ? Constants.sPreferenceLevelStronglyDiscouraged : Constants.sPreferenceLevelProhibited));
                        else if (distanceInMinutes > Constants.SLOT_LENGTH_MIN * (t2.getStartSlot() - t1.getStartSlot() - t1.getLength()))
                            pref = Math.max(pref, Constants.sPreferenceLevelDiscouraged);
                    } else if (t2.getStartSlot() + t2.getLength() < t1.getStartSlot()) {
                        int distanceInMinutes = Placement.getDistanceInMinutes(getDistanceMetric(), placement, otherPlacement);
                        if (distanceInMinutes >  t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t1.getStartSlot() - t2.getStartSlot() - t2.getLength()))
                            pref = Math.max(pref, (iIgnoreDistances ? Constants.sPreferenceLevelStronglyDiscouraged : Constants.sPreferenceLevelProhibited));
                        else if (distanceInMinutes > Constants.SLOT_LENGTH_MIN * (t1.getStartSlot() - t2.getStartSlot() - t2.getLength()))
                            pref = Math.max(pref, Constants.sPreferenceLevelDiscouraged);
                    }
                    if (t1.getStartSlot() + t1.getLength() <= t2.getStartSlot()) {
                        if (after == null || t2.getStartSlot() < after.getTimeLocation().getStartSlot())
                            after = otherPlacement;
                    } else if (t2.getStartSlot() + t2.getLength() <= t1.getStartSlot()) {
                        if (before == null || before.getTimeLocation().getStartSlot() < t2.getStartSlot())
                            before = otherPlacement;
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

    /** Worst back-to-back preference of this instructor 
     * @return worst possible distance preference 
     **/
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
    
    @Override
    public InstructorConstraintContext createAssignmentContext(Assignment<Lecture, Placement> assignment) {
        return new InstructorConstraintContext(assignment);
    }

    public class InstructorConstraintContext implements AssignmentConstraintContext<Lecture, Placement> {
        public int iPreference = 0;
        protected List<Placement>[] iResource;
 
        @SuppressWarnings("unchecked")
        public InstructorConstraintContext(Assignment<Lecture, Placement> assignment) {
            iResource = new List[Constants.SLOTS_PER_DAY * Constants.DAY_CODES.length];
            for (int i = 0; i < iResource.length; i++)
                iResource[i] = new ArrayList<Placement>(3);
            for (Lecture lecture: variables()) {
                Placement placement = assignment.getValue(lecture);
                if (placement != null) {
                    for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
                        int slot = e.nextElement();
                        iResource[slot].add(placement);
                    }
                }
            }
            iPreference = countPreference(assignment);
            getModel().getCriterion(BackToBackInstructorPreferences.class).inc(assignment, iPreference);
        }

        @Override
        public void assigned(Assignment<Lecture, Placement> assignment, Placement placement) {
            for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
                int slot = e.nextElement();
                iResource[slot].add(placement);
            }
            getModel().getCriterion(BackToBackInstructorPreferences.class).inc(assignment, -iPreference);
            iPreference = countPreference(assignment);
            getModel().getCriterion(BackToBackInstructorPreferences.class).inc(assignment, iPreference);
        }
        
        @Override
        public void unassigned(Assignment<Lecture, Placement> assignment, Placement placement) {
            for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
                int slot = e.nextElement();
                iResource[slot].remove(placement);
            }
            getModel().getCriterion(BackToBackInstructorPreferences.class).inc(assignment, -iPreference);
            iPreference = countPreference(assignment);
            getModel().getCriterion(BackToBackInstructorPreferences.class).inc(assignment, iPreference);
        }
        
        public List<Placement> getPlacements(int slot) { return iResource[slot]; }
        
        public Placement getPlacement(int slot, int day) {
            for (Placement p : iResource[slot]) {
                if (p.getTimeLocation().hasDay(day))
                    return p;
            }
            return null;
        }
        
        public List<Placement> getPlacements(int slot, BitSet weekCode) {
            List<Placement> placements = new ArrayList<Placement>(iResource[slot].size());
            for (Placement p : iResource[slot]) {
                if (p.getTimeLocation().shareWeeks(weekCode))
                    placements.add(p);
            }
            return placements;
        }
        
        public List<Placement> getPlacements(int slot, Placement placement) {
            return getPlacements(slot, placement.getTimeLocation().getWeekCode());
        }
        
        public int getNrSlots() { return iResource.length; }
        
        public Placement[] getResourceOfWeek(int startDay) {
            Placement[] ret = new Placement[iResource.length];
            for (int i = 0; i < iResource.length; i++) {
                ret[i] = getPlacement(i, startDay + (i / Constants.SLOTS_PER_DAY));
            }
            return ret;
        }

        /** Overall back-to-back preference of this instructor 
         * @return current distance preference
         **/
        public int getPreference() {
            return iPreference;
        }
        
        public int countPreference(Assignment<Lecture, Placement> assignment) {
            int pref = 0;
            HashSet<Placement> checked = new HashSet<Placement>();

            for (int slot = 1; slot < getNrSlots(); slot++) {
                if ((slot % Constants.SLOTS_PER_DAY) == 0) continue;
                for (Placement placement : getPlacements(slot)) {
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
                for (Lecture v1: variables()) {
                    Placement p1 = assignment.getValue(v1);
                    TimeLocation t1 = (p1 == null ? null : p1.getTimeLocation());
                    if (t1 == null) continue;
                    Placement before = null;
                    for (Lecture l2: variables()) {
                        Placement p2 = assignment.getValue(l2);
                        if (p2 == null || l2.equals(v1)) continue;
                        TimeLocation t2 = p2.getTimeLocation();
                        if (t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) continue;
                        if (t2.getStartSlot() + t2.getLength() < t1.getStartSlot()) {
                            int distanceInMinutes = Placement.getDistanceInMinutes(getDistanceMetric(), p1, p2);
                            if (distanceInMinutes >  t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t1.getStartSlot() - t2.getStartSlot() - t2.getLength()))
                                pref += (iIgnoreDistances ? Constants.sPreferenceLevelStronglyDiscouraged : Constants.sPreferenceLevelProhibited);
                            else if (distanceInMinutes > Constants.SLOT_LENGTH_MIN * (t1.getStartSlot() - t2.getStartSlot() - t2.getLength()))
                                pref += Constants.sPreferenceLevelDiscouraged;
                        }
                        if (t2.getStartSlot() + t2.getLength() <= t1.getStartSlot()) {
                            if (before == null || before.getTimeLocation().getStartSlot() < t2.getStartSlot())
                                before = p2;
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
                    if (before != null && Placement.getDistanceInMinutes(getDistanceMetric(), before, p1) > getDistanceMetric().getInstructorLongTravelInMinutes())
                        pref += Constants.sPreferenceLevelStronglyDiscouraged;
                }
            }

            return pref;
        }
    }
}
