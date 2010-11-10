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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
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
    private List<Placement>[] iAvailable = null;
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

    @SuppressWarnings("unchecked")
    public void setNotAvailable(Placement placement) {
        if (iAvailable == null) {
            iAvailable = new List[Constants.SLOTS_PER_DAY * Constants.DAY_CODES.length];
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
        if (iAvailable == null)
            return true;
        return (iAvailable[slot] == null || iAvailable[slot].isEmpty());
    }

    public boolean isAvailable(Lecture lecture, TimeLocation time) {
        if (iAvailable == null)
            return true;
        for (Enumeration<Integer> e = time.getSlots(); e.hasMoreElements();) {
            int slot = e.nextElement().intValue();
            if (iAvailable[slot] != null) {
                for (Placement p : iAvailable[slot]) {
                    if (lecture.canShareRoom(p.variable()))
                        continue;
                    if (time.shareWeeks(p.getTimeLocation()))
                        return false;
                }
            }
        }
        return true;
    }
    
    private DistanceMetric getDistanceMetric() {
        return ((TimetableModel)getModel()).getDistanceMetric();
    }

    public boolean isAvailable(Lecture lecture, Placement placement) {
        if (iAvailable == null)
            return true;
        for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
            int slot = e.nextElement();
            if (iAvailable[slot] != null) {
                for (Placement p : iAvailable[slot]) {
                    if (lecture.canShareRoom(p.variable()) && placement.sameRooms(p))
                        continue;
                    if (placement.getTimeLocation().shareWeeks(p.getTimeLocation()))
                        return false;
                }
            }
        }
        if (!iIgnoreDistances) {
            for (Enumeration<Integer> e = placement.getTimeLocation().getStartSlots(); e.hasMoreElements();) {
                int startSlot = e.nextElement();
                int prevSlot = startSlot - 1;
                if (prevSlot >= 0 && (prevSlot / Constants.SLOTS_PER_DAY) == (startSlot / Constants.SLOTS_PER_DAY)) {
                    if (iAvailable[prevSlot] != null) {
                        for (Placement p : iAvailable[prevSlot]) {
                            if (lecture.canShareRoom(p.variable()) && placement.sameRooms(p))
                                continue;
                            if (placement.getTimeLocation().shareWeeks(p.getTimeLocation())
                                    && Placement.getDistanceInMeters(getDistanceMetric(), p, placement) > getDistanceMetric().getInstructorProhibitedLimit())
                                return false;
                        }
                    }
                }
                int nextSlot = startSlot + placement.getTimeLocation().getLength();
                if ((nextSlot / Constants.SLOTS_PER_DAY) == (startSlot / Constants.SLOTS_PER_DAY)) {
                    if (iAvailable[nextSlot] != null) {
                        for (Placement p : iAvailable[nextSlot]) {
                            if (lecture.canShareRoom(p.variable()) && placement.sameRooms(p))
                                continue;
                            if (placement.getTimeLocation().shareWeeks(p.getTimeLocation())
                                    && Placement.getDistanceInMeters(getDistanceMetric(), p, placement) > getDistanceMetric().getInstructorProhibitedLimit())
                                return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public List<Placement>[] getAvailableArray() {
        return iAvailable;
    }

    /** Back-to-back preference of two placements (3 means prohibited) */
    public int getDistancePreference(Placement p1, Placement p2) {
        if (!p1.getTimeLocation().shareDays(p2.getTimeLocation()))
            return 0;
        if (!p1.getTimeLocation().shareWeeks(p2.getTimeLocation()))
            return 0;
        int s1 = p1.getTimeLocation().getStartSlot() % Constants.SLOTS_PER_DAY;
        int s2 = p2.getTimeLocation().getStartSlot() % Constants.SLOTS_PER_DAY;
        if (s1 + p1.getTimeLocation().getLength() != s2 && s2 + p2.getTimeLocation().getLength() != s1)
            return 0;
        double distance = Placement.getDistanceInMeters(getDistanceMetric(), p1, p2);
        if (distance <= getDistanceMetric().getInstructorNoPreferenceLimit())
            return Constants.sPreferenceLevelNeutral;
        if (distance <= getDistanceMetric().getInstructorDiscouragedLimit())
            return Constants.sPreferenceLevelDiscouraged;
        if (iIgnoreDistances || distance <= getDistanceMetric().getInstructorProhibitedLimit())
            return Constants.sPreferenceLevelStronglyDiscouraged;
        return Constants.sPreferenceLevelProhibited;
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
                    List<Placement> conf = getPlacements(prevSlot, placement);
                    for (Placement c : conf) {
                        if (lecture.equals(c.variable()))
                            continue;
                        if (Placement.getDistanceInMeters(getDistanceMetric(), placement, c) > getDistanceMetric().getInstructorProhibitedLimit()) {
                            if (c.canShareRooms(placement) && c.sameRooms(placement))
                                continue;
                            conflicts.add(c);
                        }
                    }
                }
                int nextSlot = startSlot + placement.getTimeLocation().getLength();
                if ((nextSlot / Constants.SLOTS_PER_DAY) == (startSlot / Constants.SLOTS_PER_DAY)) {
                    List<Placement> conf = getPlacements(nextSlot, placement);
                    for (Placement c : conf) {
                        if (lecture.equals(c.variable()))
                            continue;
                        if (Placement.getDistanceInMeters(getDistanceMetric(), placement, c) > getDistanceMetric().getInstructorProhibitedLimit()) {
                            if (c.canShareRooms(placement) && c.sameRooms(placement))
                                continue;
                            conflicts.add(c);
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
                    List<Placement> conf = getPlacements(prevSlot, placement);
                    for (Placement c : conf) {
                        if (lecture.equals(c.variable()))
                            continue;
                        if (Placement.getDistanceInMeters(getDistanceMetric(), placement, c) > getDistanceMetric().getInstructorProhibitedLimit()) {
                            if (c.canShareRooms(placement) && c.sameRooms(placement))
                                continue;
                            return true;
                        }
                    }
                }
                int nextSlot = startSlot + placement.getTimeLocation().getLength();
                if ((nextSlot / Constants.SLOTS_PER_DAY) == (startSlot / Constants.SLOTS_PER_DAY)) {
                    List<Placement> conf = getPlacements(nextSlot, placement);
                    for (Placement c : conf) {
                        if (lecture.equals(c.variable()))
                            continue;
                        if (Placement.getDistanceInMeters(getDistanceMetric(), placement, c) > getDistanceMetric().getInstructorProhibitedLimit()) {
                            if (c.canShareRooms(placement) && c.sameRooms(placement))
                                continue;
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
        iPreference += getPreference(placement);
        for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
            int slot = e.nextElement();
            iResource[slot].add(placement);
        }
    }

    @Override
    public void unassigned(long iteration, Placement placement) {
        super.unassigned(iteration, placement);
        iPreference -= getPreference(placement);
        for (Enumeration<Integer> e = placement.getTimeLocation().getSlots(); e.hasMoreElements();) {
            int slot = e.nextElement();
            iResource[slot].remove(placement);
        }
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
        HashSet<Placement> used = new HashSet<Placement>();
        for (Enumeration<Integer> e = placement.getTimeLocation().getStartSlots(); e.hasMoreElements();) {
            int startSlot = e.nextElement();
            int prevSlot = startSlot - 1;
            if (prevSlot >= 0 && (prevSlot / Constants.SLOTS_PER_DAY) == (startSlot / Constants.SLOTS_PER_DAY)) {
                List<Placement> conf = getPlacements(prevSlot, placement);
                for (Placement c : conf) {
                    if (lecture.equals(c.variable()))
                        continue;
                    if (!used.add(c))
                        continue;
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
                List<Placement> conf = getPlacements(nextSlot, placement);
                for (Placement c : conf) {
                    if (lecture.equals(c.variable()))
                        continue;
                    if (!used.add(c))
                        continue;
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
        return pref;
    }

    public int getPreferenceCombination(Placement value) {
        Lecture lecture = value.variable();
        Placement placement = value;
        int pref = 0;
        HashSet<Placement> used = new HashSet<Placement>();
        for (Enumeration<Integer> e = placement.getTimeLocation().getStartSlots(); e.hasMoreElements();) {
            int startSlot = e.nextElement();
            int prevSlot = startSlot - 1;
            if (prevSlot >= 0 && (prevSlot / Constants.SLOTS_PER_DAY) == (startSlot / Constants.SLOTS_PER_DAY)) {
                List<Placement> conf = getPlacements(prevSlot, placement);
                for (Placement c : conf) {
                    if (lecture.equals(c.variable()))
                        continue;
                    if (!used.add(c))
                        continue;
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
                List<Placement> conf = getPlacements(nextSlot, placement);
                for (Placement c : conf) {
                    if (lecture.equals(c.variable()))
                        continue;
                    if (!used.add(c))
                        continue;
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
        HashSet<Placement> used = new HashSet<Placement>();
        for (int slot = 1; slot < iResource.length; slot++) {
            if ((slot % Constants.SLOTS_PER_DAY) == 0)
                continue;
            for (Placement placement : iResource[slot]) {
                List<Placement> prevPlacements = getPlacements(slot - 1, placement);
                for (Placement prevPlacement : prevPlacements) {
                    if (!used.add(prevPlacement))
                        continue;
                    double dist = Placement.getDistanceInMeters(getDistanceMetric(), prevPlacement, placement);
                    if (dist > getDistanceMetric().getInstructorNoPreferenceLimit() && dist <= getDistanceMetric().getInstructorDiscouragedLimit())
                        pref += Constants.sPreferenceLevelDiscouraged;
                    if (dist > getDistanceMetric().getInstructorDiscouragedLimit())
                        pref += Constants.sPreferenceLevelStronglyDiscouraged;
                }
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
