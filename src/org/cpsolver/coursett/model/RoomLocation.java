package org.cpsolver.coursett.model;

import java.util.HashMap;
import java.util.Map;

import org.cpsolver.coursett.constraint.RoomConstraint;
import org.cpsolver.ifs.util.DistanceMetric;

/**
 * Room part of placement. <br>
 * <br>
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

public class RoomLocation implements Comparable<RoomLocation> {
    private int iPreference;
    private String iName;
    private Long iId;
    private Long iBldgId;
    private int iRoomSize;
    private Double iPosX = null, iPosY = null;
    private RoomConstraint iRoomConstraint = null;
    private boolean iIgnoreTooFar = false;
    private Map<Integer, Integer> iPreferenceByIndex = null;

    /**
     * Constructor
     * 
     * @param id
     *            room id
     * @param name
     *            room name
     * @param bldgId
     *            building id
     * @param preference
     *            soft preference
     * @param size
     *            room size
     * @param x
     *            x-position of the building
     * @param y
     *            y-position of the building
     * @param ignoreTooFar true if distance conflicts are to be ignored
     * @param rc related room constraint
     */
    public RoomLocation(Long id, String name, Long bldgId, int preference, int size, Double x, Double y,
            boolean ignoreTooFar, RoomConstraint rc) {
        iId = id;
        iName = name;
        iPreference = preference;
        iRoomSize = size;
        iPosX = x;
        iPosY = y;
        iBldgId = bldgId;
        iRoomConstraint = rc;
        iIgnoreTooFar = ignoreTooFar;
    }

    /** Room id 
     * @return room unique id
     **/
    public Long getId() {
        return iId;
    }

    /** Building id 
     * @return building unique id
     **/
    public Long getBuildingId() {
        return iBldgId;
    }

    /** Room name 
     * @return room name
     **/
    public String getName() {
        return iName;
    }

    /** Room preference 
     * @return room preference
     **/
    public int getPreference() {
        return iPreference;
    }

    /** 
     * Set room preference
     * @param preference room preferences
     */
    public void setPreference(int preference) {
        iPreference = preference;
    }

    /** Room size 
     * @return room size
     **/
    public int getRoomSize() {
        return iRoomSize;
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

    public RoomConstraint getRoomConstraint() {
        return iRoomConstraint;
    }

    @Override
    public String toString() {
        return "Room{name=" + iName + ", pref=" + iPreference + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof RoomLocation))
            return false;
        return getId().equals(((RoomLocation) o).getId());
    }

    public double getDistanceInMeters(DistanceMetric m, RoomLocation roomLocation) {
        if (getId().equals(roomLocation.getId()))
            return 0.0;
        if (getIgnoreTooFar() || roomLocation.getIgnoreTooFar())
            return 0.0;
        return m.getDistanceInMeters(getId(), getPosX(), getPosY(), roomLocation.getId(), roomLocation.getPosX(), roomLocation.getPosY());
    }

    public int getDistanceInMinutes(DistanceMetric m, RoomLocation roomLocation) {
        if (getId().equals(roomLocation.getId()))
            return 0;
        if (getIgnoreTooFar() || roomLocation.getIgnoreTooFar())
            return 0;
        return  m.getDistanceInMinutes(getId(), getPosX(), getPosY(), roomLocation.getId(), roomLocation.getPosX(), roomLocation.getPosY());
    }

    @Override
    public int compareTo(RoomLocation o) {
        int cmp = -(Long.valueOf(getRoomSize())).compareTo(Long.valueOf(o.getRoomSize()));
        if (cmp != 0)
            return cmp;
        return getName().compareTo((o).getName());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    /**
     * Support for multiple rooms with different preferences: override default room preference for the given index
     */
    public int getPreference(int roomIndex) {
        if (iPreferenceByIndex == null) return iPreference;
        Integer pref = iPreferenceByIndex.get(roomIndex);
        return (pref == null ? iPreference : pref);
    }
    /**
     * Support for multiple rooms with different preferences: override default room preference for the given index
     */
    public void setPreference(int roomIndex, int preference) {
        if (iPreferenceByIndex == null) iPreferenceByIndex = new HashMap<Integer, Integer>();
        iPreferenceByIndex.put(roomIndex, preference);
    }
    /**
     * Support for multiple rooms with different preferences: has preference overrides for particular rooms
     */
    public boolean hasPreferenceByIndex() { return iPreferenceByIndex != null && !iPreferenceByIndex.isEmpty(); }
    /**
     * Support for multiple rooms with different preferences: return preference overrides
     */
    public Map<Integer, Integer> getPreferenceByIndex() { return iPreferenceByIndex; }
    
    /**
     * Support for multiple rooms with different preferences: return min preference
     */
    public int getMinPreference() {
        int ret = getPreference();
        if (iPreferenceByIndex != null)
            for (Integer pref: iPreferenceByIndex.values())
                if (pref < ret) ret = pref;
        return ret;
    }
    /**
     * Support for multiple rooms with different preferences: return max preference
     */
    public int getMaxPreference() {
        int ret = getPreference();
        if (iPreferenceByIndex != null)
            for (Integer pref: iPreferenceByIndex.values())
                if (pref > ret) ret = pref;
        return ret;
    }
}
