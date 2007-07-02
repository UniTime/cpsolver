package net.sf.cpsolver.coursett.model;

import net.sf.cpsolver.coursett.constraint.RoomConstraint;

/**
 * Room part of placement.
 * <br><br>
 *
 * @version
 * CourseTT 1.1 (University Course Timetabling)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

public class RoomLocation implements Comparable {
    private int iPreference;
    private String iName;
    private Long iId;
    private Long iBldgId;
    private int iRoomSize;
    private int iPosX = 0, iPosY = 0;
    private RoomConstraint iRoomConstraint = null;
    private boolean iIgnoreTooFar = false;
    
    /** Constructor
     * @param id room id
     * @param name room name
     * @param bldgId building id
     * @param preference soft preference
     * @param size room size
     * @param x x-position of the building
     * @param y y-position of the building
     */
    public RoomLocation(Long id, String name, Long bldgId, int preference, int size, int x, int y, boolean ignoreTooFar, RoomConstraint rc) {
        iId = id;
        iName = name;
        iPreference = preference;
        iRoomSize = size;
        iPosX = x; iPosY = y;
        iBldgId = bldgId;
        iRoomConstraint = rc;
        iIgnoreTooFar = ignoreTooFar;
    }
    
    /** Room id */
    public Long getId() { return iId; }
    /** Building id */
    public Long getBuildingId() { return iBldgId; }
    /** Room name */
    public String getName() { return iName; }
    /** Room preference */
    public int getPreference() { return iPreference; }
    public void setPreference(int preference) { iPreference = preference; }
    /** Room size */
    public int getRoomSize() { return iRoomSize; }
    /** Position of the building */
    public void setCoordinates(int x, int y) { iPosX=x; iPosY=y; }
    /** X-position of the building */
    public int getPosX() { return iPosX; }
    /** Y-position of the building */
    public int getPosY() { return iPosY; }
    public boolean getIgnoreTooFar() { return iIgnoreTooFar; }
    public RoomConstraint getRoomConstraint() { return iRoomConstraint; }
    
    public String toString() {
        return "Room{name="+iName+", pref="+iPreference+"}";
    }
    public boolean equals(Object o) {
    	if (o==null || !(o instanceof RoomLocation)) return false;
    	return getId().equals(((RoomLocation)o).getId());
    }
    public double getDistance(RoomLocation roomLocation) {
    	if (getId().equals(roomLocation.getId())) return 0.0;
    	if (getIgnoreTooFar() || roomLocation.getIgnoreTooFar()) return 0.0;
    	if (getPosX()<0 || getPosY()<0 || roomLocation.getPosX()<0 || roomLocation.getPosY()<0) return 10000.0;
		long x = getPosX()-roomLocation.getPosX();
		long y = getPosY()-roomLocation.getPosY();
		return Math.sqrt((x*x)+(y*y));
    }
    
    public int compareTo(Object o) {
    	int cmp = -(new Long(getRoomSize())).compareTo(new Long(((RoomLocation)o).getRoomSize()));
    	if (cmp!=0) return cmp;
    	return getName().compareTo(((RoomLocation)o).getName());
    }
    
    public int hashCode() {
    	return getName().hashCode();
    }
}
