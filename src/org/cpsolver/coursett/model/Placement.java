package org.cpsolver.coursett.model;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.constraint.GroupConstraint;
import org.cpsolver.coursett.constraint.InstructorConstraint;
import org.cpsolver.coursett.constraint.SpreadConstraint;
import org.cpsolver.coursett.preference.PreferenceCombination;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.util.DistanceMetric;
import org.cpsolver.ifs.util.ToolBox;


/**
 * Placement (value). <br>
 * <br>
 * It combines room and time location
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

public class Placement extends Value<Lecture, Placement> {
    private TimeLocation iTimeLocation;
    private RoomLocation iRoomLocation;
    private List<RoomLocation> iRoomLocations = null;
    private Long iAssignmentId = null;
    private int iHashCode = 0;
    private Double iTimePenalty = null;
    private Integer iRoomPenalty = null;

    /**
     * Constructor
     * 
     * @param lecture
     *            lecture
     * @param timeLocation
     *            time location
     * @param roomLocation
     *            room location
     */
    public Placement(Lecture lecture, TimeLocation timeLocation, RoomLocation roomLocation) {
        super(lecture);
        iTimeLocation = timeLocation;
        iRoomLocation = roomLocation;
        if (iRoomLocation == null) {
            iRoomLocations = new ArrayList<RoomLocation>(0);
        }
        iHashCode = getName().hashCode();
    }

    public Placement(Lecture lecture, TimeLocation timeLocation, java.util.List<RoomLocation> roomLocations) {
        super(lecture);
        iTimeLocation = timeLocation;
        iRoomLocation = (roomLocations.isEmpty() ? null : (RoomLocation) roomLocations.get(0));
        if (roomLocations.size() != 1) {
            iRoomLocations = new ArrayList<RoomLocation>(roomLocations);
        }
        if (iRoomLocations != null && iRoomLocations.size() > 1) {
            boolean hasPreferenceByIndex = false;
            for (RoomLocation r: iRoomLocations)
                if (r.hasPreferenceByIndex()) { hasPreferenceByIndex = true; break; }
            if (hasPreferenceByIndex)
                fixRoomOrder(0, roomLocations, new RoomLocation[iRoomLocations.size()], PreferenceCombination.getDefault(), null);
        }
        iHashCode = getName().hashCode();
    }
    
    private Integer fixRoomOrder(int idx, List<RoomLocation> rooms, RoomLocation[] current, PreferenceCombination preference, Integer bestSoFar) {
        if (idx == current.length) {
            if (bestSoFar == null || preference.getPreferenceInt() < bestSoFar) {
                iRoomLocations.clear();
                for (RoomLocation r: current)
                    iRoomLocations.add(r);
                return preference.getPreferenceInt();
            }
        } else {
            r: for (RoomLocation r: rooms) {
                for (int i = 0; i < idx; i++)
                    if (r.equals(current[i])) continue r;
                PreferenceCombination pc = preference.clonePreferenceCombination();
                pc.addPreferenceInt(r.getPreference(idx));
                if (!pc.isProhibited()) {
                    current[idx] = r;
                    bestSoFar = fixRoomOrder(idx + 1, rooms, current, pc, bestSoFar);
                }
            }
        }
        return bestSoFar;
    }

    /** Time location 
     * @return time of this placement
     **/
    public TimeLocation getTimeLocation() {
        return iTimeLocation;
    }

    /** Room location 
     * @return room of this placement
     **/
    public RoomLocation getRoomLocation() {
        return iRoomLocation;
    }

    /** Room locations (multi-room placement) 
     * @return rooms of this placement (if there are more than one)
     **/
    public List<RoomLocation> getRoomLocations() {
        return iRoomLocations;
    }

    public List<Long> getBuildingIds() {
        if (isMultiRoom()) {
            List<Long> ret = new ArrayList<Long>(iRoomLocations.size());
            for (RoomLocation r : iRoomLocations) {
                ret.add(r.getBuildingId());
            }
            return ret;
        } else {
            List<Long> ret = new ArrayList<Long>(1);
            ret.add(iRoomLocation.getBuildingId());
            return ret;
        }
    }

    public List<Long> getRoomIds() {
        if (isMultiRoom()) {
            List<Long> ret = new ArrayList<Long>(iRoomLocations.size());
            for (RoomLocation r : iRoomLocations) {
                ret.add(r.getId());
            }
            return ret;
        } else {
            List<Long> ret = new ArrayList<Long>(1);
            ret.add(iRoomLocation.getId());
            return ret;
        }
    }

    public List<String> getRoomNames() {
        if (isMultiRoom()) {
            List<String> ret = new ArrayList<String>(iRoomLocations.size());
            for (RoomLocation r : iRoomLocations) {
                ret.add(r.getName());
            }
            return ret;
        } else {
            List<String> ret = new ArrayList<String>(1);
            if (iRoomLocation != null)
                ret.add(iRoomLocation.getName());
            return ret;
        }
    }

    public List<Integer> getRoomPrefs() {
        if (isMultiRoom()) {
            List<Integer> ret = new ArrayList<Integer>(iRoomLocations.size());
            int roomIndex = 0;
            for (RoomLocation r : iRoomLocations) {
                ret.add(r.getPreference(roomIndex++));
            }
            return ret;
        } else {
            List<Integer> ret = new ArrayList<Integer>(1);
            if (iRoomLocation != null)
                ret.add(iRoomLocation.getPreference());
            return ret;
        }
    }

    public boolean isMultiRoom() {
        return (iRoomLocations != null && iRoomLocations.size() != 1);
    }

    public RoomLocation getRoomLocation(Long roomId) {
        if (isMultiRoom()) {
            for (RoomLocation r : iRoomLocations) {
                if (r.getId().equals(roomId))
                    return r;
            }
        } else if (iRoomLocation != null && iRoomLocation.getId().equals(roomId))
            return iRoomLocation;
        return null;
    }
    
    public int getRoomLocationIndex(Long roomId) {
        if (isMultiRoom()) {
            int idx = 0;
            for (RoomLocation r : iRoomLocations) {
                if (r.getId().equals(roomId))
                    return idx;
                idx ++;
            }
        } else if (iRoomLocation != null && iRoomLocation.getId().equals(roomId))
            return 0;
        return -1;
    }

    public boolean hasRoomLocation(Long roomId) {
        if (isMultiRoom()) {
            for (RoomLocation r : iRoomLocations) {
                if (r.getId().equals(roomId))
                    return true;
            }
            return false;
        } else
            return iRoomLocation != null && iRoomLocation.getId().equals(roomId);
    }

    public String getRoomName(String delim) {
        if (isMultiRoom()) {
            StringBuffer sb = new StringBuffer();
            for (RoomLocation r : iRoomLocations) {
                if (sb.length() > 0)
                    sb.append(delim);
                sb.append(r.getName());
            }
            return sb.toString();
        } else {
            return (getRoomLocation() == null ? "" : getRoomLocation().getName());
        }
    }

    @Override
    public String getName() {
        return getName(true);
    }
    
    public String getName(boolean useAmPm) {
        Lecture lecture = variable();
        return getTimeLocation().getName(useAmPm) + " " + getRoomName(", ")
                + (lecture != null && lecture.getInstructorName() != null ? " " + lecture.getInstructorName() : "");
    }

    public String getLongName(boolean useAmPm) {
        Lecture lecture = variable();
        if (isMultiRoom()) {
            StringBuffer sb = new StringBuffer();
            for (RoomLocation r : iRoomLocations) {
                if (sb.length() > 0)
                    sb.append(", ");
                sb.append(r.getName());
            }
            return getTimeLocation().getLongName(useAmPm) + " " + sb
                    + (lecture != null &&  lecture.getInstructorName() != null ? " " + lecture.getInstructorName() : "");
        } else
            return getTimeLocation().getLongName(useAmPm)
                    + (getRoomLocation() == null ? "" : " " + getRoomLocation().getName())
                    + (lecture != null && lecture.getInstructorName() != null ? " " + lecture.getInstructorName() : "");
    }
    
    @Deprecated
    public String getLongName() {
        return getLongName(true);
    }

    public boolean sameRooms(Placement placement) {
        if (placement.isMultiRoom() != isMultiRoom())
            return false;
        if (isMultiRoom()) {
            if (placement.getRoomLocations().size() != getRoomLocations().size())
                return false;
            return placement.getRoomLocations().containsAll(getRoomLocations());
        } else {
            if (placement.getRoomLocation() == null)
                return getRoomLocation() == null;
            return placement.getRoomLocation().equals(getRoomLocation());
        }
    }

    public boolean shareRooms(Placement placement) {
        if (isMultiRoom()) {
            if (placement.isMultiRoom()) {
                for (RoomLocation rl : getRoomLocations()) {
                    if (rl.getRoomConstraint() == null || !rl.getRoomConstraint().getConstraint())
                        continue;
                    if (placement.getRoomLocations().contains(rl))
                        return true;
                }
                return false;
            } else {
                if (placement.getRoomLocation().getRoomConstraint() == null || !placement.getRoomLocation().getRoomConstraint().getConstraint())
                    return false;
                return getRoomLocations().contains(placement.getRoomLocation());
            }
        } else {
            if (getRoomLocation().getRoomConstraint() == null || !getRoomLocation().getRoomConstraint().getConstraint())
                return false;
            if (placement.isMultiRoom()) {
                return placement.getRoomLocations().contains(getRoomLocation());
            } else {
                return getRoomLocation().equals(placement.getRoomLocation());
            }
        }
    }

    public int nrDifferentRooms(Placement placement) {
        if (isMultiRoom()) {
            int ret = 0;
            for (RoomLocation r : getRoomLocations()) {
                if (!placement.getRoomLocations().contains(r))
                    ret++;
            }
            return ret;
        } else {
            return (placement.getRoomLocation().equals(getRoomLocation()) ? 0 : 1);
        }
    }

    public int nrDifferentBuildings(Placement placement) {
        if (isMultiRoom()) {
            int ret = 0;
            for (RoomLocation r : getRoomLocations()) {
                boolean contains = false;
                for (RoomLocation q : placement.getRoomLocations()) {
                    if (ToolBox.equals(r.getBuildingId(), q.getBuildingId()))
                        contains = true;
                }
                if (!contains)
                    ret++;
            }
            return ret;
        } else {
            return (ToolBox.equals(placement.getRoomLocation().getBuildingId(), getRoomLocation().getBuildingId()) ? 0
                    : 1);
        }
    }

    public int sumRoomPreference() {
        if (isMultiRoom()) {
            int ret = 0;
            int roomIndex = 0;
            for (RoomLocation r : getRoomLocations()) {
                ret += r.getPreference(roomIndex ++);
            }
            return ret;
        } else {
            return getRoomLocation().getPreference();
        }
    }

    public int getRoomPreference() {
        if (isMultiRoom()) {
            PreferenceCombination p = PreferenceCombination.getDefault();
            int roomIndex = 0;
            for (RoomLocation r : getRoomLocations()) {
                p.addPreferenceInt(r.getPreference(roomIndex++));
            }
            return p.getPreferenceInt();
        } else {
            return getRoomLocation().getPreference();
        }
    }

    public int getRoomSize() {
        if (isMultiRoom()) {
            if (getRoomLocations().isEmpty()) return 0;
            if (variable() != null && variable().isSplitAttendance()) {
                int roomSize = 0;
                for (RoomLocation r : getRoomLocations())
                    roomSize += r.getRoomSize();
                return roomSize;
            } else {
                int roomSize = Integer.MAX_VALUE;
                for (RoomLocation r : getRoomLocations()) {
                    roomSize = Math.min(roomSize, r.getRoomSize());
                }
                return roomSize;
            }
        } else {
            return getRoomLocation().getRoomSize();
        }
    }

    public boolean isHard(Assignment<Lecture, Placement> assignment) {
        if (Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(getTimeLocation().getPreference())))
            return true;
        if (isRoomProhibited()) return true;
        Lecture lecture = variable();
        for (GroupConstraint gc : lecture.hardGroupSoftConstraints()) {
            if (gc.isSatisfied(assignment))
                continue;
            if (Constants.sPreferenceProhibited.equals(gc.getPrologPreference()))
                return true;
            if (Constants.sPreferenceRequired.equals(gc.getPrologPreference()))
                return true;
        }
        return false;
    }
    
    public boolean isRoomProhibited() {
        if (isMultiRoom()) {
            int roomIndex = 0;
            for (RoomLocation r : getRoomLocations()) {
                if (Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(r.getPreference(roomIndex++))))
                    return true;
            }
        } else {
            if (getRoomLocation() != null && Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(getRoomLocation().getPreference())))
                return true;
        }
        return false;
    }

    public boolean sameTime(Placement placement) {
        return placement.getTimeLocation().equals(getTimeLocation());
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || !(object instanceof Placement))
            return false;
        Placement placement = (Placement) object;
        if (placement.getId() == getId())
            return true; // quick check
        Lecture lecture = placement.variable();
        Lecture thisLecture = variable();
        if (lecture != null && thisLecture != null && !lecture.getClassId().equals(thisLecture.getClassId()))
            return false;
        if (!sameRooms(placement))
            return false;
        if (!sameTime(placement))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return iHashCode;
    }

    @Override
    public String toString() {
        return variable().getName() + " " + getName();
    }

    /** Distance between two placements 
     * @param m distance matrix
     * @param p1 first placement
     * @param p2 second placement
     * @return maximal distance in meters between the two placement
     **/
    public static double getDistanceInMeters(DistanceMetric m, Placement p1, Placement p2) {
        if (p1.isMultiRoom()) {
            if (p2.isMultiRoom()) {
                double dist = 0.0;
                for (RoomLocation r1 : p1.getRoomLocations()) {
                    for (RoomLocation r2 : p2.getRoomLocations()) {
                        dist = Math.max(dist, r1.getDistanceInMeters(m, r2));
                    }
                }
                return dist;
            } else {
                if (p2.getRoomLocation() == null)
                    return 0.0;
                double dist = 0.0;
                for (RoomLocation r1 : p1.getRoomLocations()) {
                    dist = Math.max(dist, r1.getDistanceInMeters(m, p2.getRoomLocation()));
                }
                return dist;
            }
        } else if (p2.isMultiRoom()) {
            if (p1.getRoomLocation() == null)
                return 0.0;
            double dist = 0.0;
            for (RoomLocation r2 : p2.getRoomLocations()) {
                dist = Math.max(dist, p1.getRoomLocation().getDistanceInMeters(m, r2));
            }
            return dist;
        } else {
            if (p1.getRoomLocation() == null || p2.getRoomLocation() == null)
                return 0.0;
            return p1.getRoomLocation().getDistanceInMeters(m, p2.getRoomLocation());
        }
    }
    
    /** Distance between two placements 
     * @param m distance matrix
     * @param p1 first placement
     * @param p2 second placement
     * @return maximal distance in minutes between the two placement
     **/
    public static int getDistanceInMinutes(DistanceMetric m, Placement p1, Placement p2) {
        if (p1.isMultiRoom()) {
            if (p2.isMultiRoom()) {
                int dist = 0;
                for (RoomLocation r1 : p1.getRoomLocations()) {
                    for (RoomLocation r2 : p2.getRoomLocations()) {
                        dist = Math.max(dist, r1.getDistanceInMinutes(m, r2));
                    }
                }
                return dist;
            } else {
                if (p2.getRoomLocation() == null)
                    return 0;
                int dist = 0;
                for (RoomLocation r1 : p1.getRoomLocations()) {
                    dist = Math.max(dist, r1.getDistanceInMinutes(m, p2.getRoomLocation()));
                }
                return dist;
            }
        } else if (p2.isMultiRoom()) {
            if (p1.getRoomLocation() == null)
                return 0;
            int dist = 0;
            for (RoomLocation r2 : p2.getRoomLocations()) {
                dist = Math.max(dist, p1.getRoomLocation().getDistanceInMinutes(m, r2));
            }
            return dist;
        } else {
            if (p1.getRoomLocation() == null || p2.getRoomLocation() == null)
                return 0;
            return p1.getRoomLocation().getDistanceInMinutes(m, p2.getRoomLocation());
        }
    }

    public int getCommitedConflicts() {
        int ret = 0;
        Lecture lecture = variable();
        for (Student student : lecture.students()) {
            ret += student.countConflictPlacements(this);
        }
        return ret;
    }

    public Long getAssignmentId() {
        return iAssignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        iAssignmentId = assignmentId;
    }

    public boolean canShareRooms(Placement other) {
        return (variable()).canShareRoom(other.variable());
    }

    public boolean isValid() {
        Lecture lecture = variable();
        if (!lecture.isValid(this))
            return false;
        for (InstructorConstraint ic : lecture.getInstructorConstraints()) {
            if (!ic.isAvailable(lecture, this) && ic.isHard())
                return false;
        }
        if (lecture.getNrRooms() > 0) {
            if (isMultiRoom()) {
                for (RoomLocation roomLocation : getRoomLocations()) {
                    if (roomLocation.getRoomConstraint() != null
                            && !roomLocation.getRoomConstraint().isAvailable(lecture, getTimeLocation(),
                                    lecture.getScheduler()))
                        return false;
                }
            } else {
                if (getRoomLocation().getRoomConstraint() != null
                        && !getRoomLocation().getRoomConstraint().isAvailable(lecture, getTimeLocation(),
                                lecture.getScheduler()))
                    return false;
            }
        }
        return true;
    }

    public String getNotValidReason(Assignment<Lecture, Placement> assignment, boolean useAmPm) {
        Lecture lecture = variable();
        String reason = lecture.getNotValidReason(assignment, this, useAmPm);
        if (reason != null)
            return reason;
        for (InstructorConstraint ic : lecture.getInstructorConstraints()) {
            if (!ic.isAvailable(lecture, this) && ic.isHard()) {
                if (!ic.isAvailable(lecture, getTimeLocation())) {
                    for (Placement c: ic.getUnavailabilities()) {
                        if (c.variable().getId() < 0 && lecture.getDepartment() != null && c.variable().getDepartment() != null
                                && !c.variable().getDepartment().equals(lecture.getDepartment())) continue;
                        if (c.getTimeLocation().hasIntersection(getTimeLocation()) && !lecture.canShareRoom(c.variable()))
                            return "instructor " + ic.getName() + " not available at " + getTimeLocation().getLongName(useAmPm) + " due to " + c.variable().getName();
                    }
                    return "instructor " + ic.getName() + " not available at " + getTimeLocation().getLongName(useAmPm);
                } else
                    return "placement " + getTimeLocation().getLongName(useAmPm) + " " + getRoomName(", ") + " is too far for instructor " + ic.getName();
            }
        }
        if (lecture.getNrRooms() > 0) {
            if (isMultiRoom()) {
                for (RoomLocation roomLocation : getRoomLocations()) {
                    if (roomLocation.getRoomConstraint() != null && !roomLocation.getRoomConstraint().isAvailable(lecture, getTimeLocation(), lecture.getScheduler())) {
                        if (roomLocation.getRoomConstraint().getAvailableArray() != null) {
                            for (Enumeration<Integer> e = getTimeLocation().getSlots(); e.hasMoreElements();) {
                                int slot = e.nextElement();
                                if (roomLocation.getRoomConstraint().getAvailableArray()[slot] != null) {
                                    for (Placement c : roomLocation.getRoomConstraint().getAvailableArray()[slot]) {
                                        if (c.getTimeLocation().hasIntersection(getTimeLocation()) && !lecture.canShareRoom(c.variable())) {
                                            return "room " + roomLocation.getName() + " not available at " + getTimeLocation().getLongName(useAmPm) + " due to " + c.variable().getName();
                                        }
                                    }
                                }
                            }
                        }
                        return "room " + roomLocation.getName() + " not available at " + getTimeLocation().getLongName(useAmPm);
                    }
                }
            } else {
                if (getRoomLocation().getRoomConstraint() != null && !getRoomLocation().getRoomConstraint().isAvailable(lecture, getTimeLocation(), lecture.getScheduler()))
                    if (getRoomLocation().getRoomConstraint().getAvailableArray() != null) {
                        for (Enumeration<Integer> e = getTimeLocation().getSlots(); e.hasMoreElements();) {
                            int slot = e.nextElement();
                            if (getRoomLocation().getRoomConstraint().getAvailableArray()[slot] != null) {
                                for (Placement c : getRoomLocation().getRoomConstraint().getAvailableArray()[slot]) {
                                    if (c.getTimeLocation().hasIntersection(getTimeLocation()) && !lecture.canShareRoom(c.variable())) {
                                        return "room " + getRoomLocation().getName() + " not available at " + getTimeLocation().getLongName(useAmPm) + " due to " + c.variable().getName();
                                    }
                                }
                            }
                        }
                    }
                    return "room " + getRoomLocation().getName() + " not available at " + getTimeLocation().getLongName(useAmPm);
            }
        }
        return reason;
    }
    
    @Deprecated
    public String getNotValidReason(Assignment<Lecture, Placement> assignment) {
        return getNotValidReason(assignment, true);
    }

    public int getNrRooms() {
        if (iRoomLocations != null)
            return iRoomLocations.size();
        return (iRoomLocation == null ? 0 : 1);
    }

    public int getSpreadPenalty(Assignment<Lecture, Placement> assignment) {
        int spread = 0;
        for (SpreadConstraint sc : variable().getSpreadConstraints()) {
            spread += sc.getPenalty(assignment, this);
        }
        return spread;
    }

    public int getMaxSpreadPenalty(Assignment<Lecture, Placement> assignment) {
        int spread = 0;
        for (SpreadConstraint sc : variable().getSpreadConstraints()) {
            spread += sc.getMaxPenalty(assignment, this);
        }
        return spread;
    }

    @Override
    public double toDouble(Assignment<Lecture, Placement> assignment) {
        double ret = 0.0;
        for (Criterion<Lecture, Placement> criterion: variable().getModel().getCriteria())
            ret += criterion.getWeightedValue(assignment, this, null);
        return ret;
    }

    private transient Object iAssignment = null;

    public Object getAssignment() {
        return iAssignment;
    }

    public void setAssignment(Object assignment) {
        iAssignment = assignment;
    }

    public double getTimePenalty() {
        if (iTimeLocation == null) return 0.0;
        if (iTimePenalty == null) {
            double[] bounds = variable().getMinMaxTimePreference();
            double npref = iTimeLocation.getNormalizedPreference();
            if (iTimeLocation.getPreference() < Constants.sPreferenceLevelRequired / 2) npref = bounds[0];
            else if (iTimeLocation.getPreference() > Constants.sPreferenceLevelProhibited / 2) npref = bounds[1];
            iTimePenalty = npref - bounds[0];
        }
        return iTimePenalty;
    }

    public int getRoomPenalty() {
        if (getNrRooms() == 0) return 0;
        if (iRoomPenalty == null) {
            int pref = getRoomPreference();
            int[] bounds = variable().getMinMaxRoomPreference();
            if (pref < Constants.sPreferenceLevelRequired / 2) pref = bounds[0];
            if (pref > Constants.sPreferenceLevelProhibited / 2) pref = bounds[1];
            iRoomPenalty = pref - bounds[0];
        }
        return iRoomPenalty;
    }
}