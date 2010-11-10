package net.sf.cpsolver.coursett.model;

import net.sf.cpsolver.coursett.constraint.RoomConstraint;
import net.sf.cpsolver.ifs.util.DistanceMetric;

/**
 * Room part of placement. <br>
 * <br>
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

public class RoomLocation implements Comparable<RoomLocation> {
    private int iPreference;
    private String iName;
    private Long iId;
    private Long iBldgId;
    private int iRoomSize;
    private Double iPosX = null, iPosY = null;
    private RoomConstraint iRoomConstraint = null;
    private boolean iIgnoreTooFar = false;

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

    /** Room id */
    public Long getId() {
        return iId;
    }

    /** Building id */
    public Long getBuildingId() {
        return iBldgId;
    }

    /** Room name */
    public String getName() {
        return iName;
    }

    /** Room preference */
    public int getPreference() {
        return iPreference;
    }

    public void setPreference(int preference) {
        iPreference = preference;
    }

    /** Room size */
    public int getRoomSize() {
        return iRoomSize;
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
        if (getPosX() == null || getPosY() == null || roomLocation.getPosX() == null || roomLocation.getPosY() == null)
            return 10000.0;
        return m.getDistanceInMeters(getPosX(), getPosY(), roomLocation.getPosX(), roomLocation.getPosY());
    }

    public int getDistanceInMinutes(DistanceMetric m, RoomLocation roomLocation) {
        if (getId().equals(roomLocation.getId()))
            return 0;
        if (getIgnoreTooFar() || roomLocation.getIgnoreTooFar())
            return 0;
        if (getPosX() == null || getPosY() == null || roomLocation.getPosX() == null || roomLocation.getPosY() == null)
            return 60;
        return m.getDistanceInMinutes(getPosX(), getPosY(), roomLocation.getPosX(), roomLocation.getPosY());
    }

    public int compareTo(RoomLocation o) {
        int cmp = -(new Long(getRoomSize())).compareTo(new Long(o.getRoomSize()));
        if (cmp != 0)
            return cmp;
        return getName().compareTo((o).getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}
