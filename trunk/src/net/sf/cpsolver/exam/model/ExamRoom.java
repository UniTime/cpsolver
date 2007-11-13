package net.sf.cpsolver.exam.model;

import java.util.Set;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Value;

/**
 * A room. Only one exam can use a room at a time (period). 
 * <br><br>
 * 
 * @version
 * ExamTT 1.1 (Examination Timetabling)<br>
 * Copyright (C) 2007 Tomas Muller<br>
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
public class ExamRoom extends Constraint implements Comparable {
    private ExamPlacement[] iTable;
    private boolean[] iAvailable;
    private String iId;
    private int iSize, iAltSize;
    private int iCoordX, iCoordY;
    private int iHashCode;

    /**
     * Constructor
     * @param model examination timetabling model
     * @param id unique id
     * @param size room (normal) seating capacity
     * @param altSize room alternating seating capacity (to be used when {@link Exam#hasAltSeating()} is true)
     * @param coordX x coordinate
     * @param coordY y coordinate
     */
    public ExamRoom(ExamModel model, String id, int size, int altSize, int coordX, int coordY) {
        super();
        iAssignedVariables = null;
        iId = id;
        iHashCode = id.hashCode();
        iCoordX = coordX; iCoordY = coordY; 
        iSize = size; iAltSize = altSize;
        iTable = new ExamPlacement[model.getNrPeriods()];
        iAvailable = new boolean[model.getNrPeriods()];
        for (int i=0;i<iTable.length;i++) {
            iTable[i]=null;
            iAvailable[i]=true;
        }
    }
    
    /**
     * Distance between two rooms. It is computed as Euclidean distance using the room coordinates,
     * 1 unit equals to 10 meters.
     * @param other another room
     * @return distance between this and the given room
     */
    public int getDistance(ExamRoom other) {
        if (getCoordX()<0 || getCoordY()<0 || other.getCoordX()<0 || other.getCoordY()<0) return 10000;
        int dx = getCoordX()-other.getCoordX();
        int dy = getCoordY()-other.getCoordY();
        return (int)Math.sqrt(dx*dx+dy*dy);
    }
    
    /**
     * Room unique id
     */
    public String getRoomId() { return iId; }
    /**
     * Normal seating capacity (to be used when {@link Exam#hasAltSeating()} is false)
     */
    public int getSize() { return iSize; }
    /**
     * Alternating seating capacity (to be used when {@link Exam#hasAltSeating()} is true)
     */
    public int getAltSize() { return iAltSize; }
    /**
     * X coordinate
     */
    public int getCoordX() { return iCoordX; }
    /**
     * Y coordinate
     */
    public int getCoordY() { return iCoordY; }
    /**
     * An exam placed at the given period
     * @param period a period
     * @return a placement of an exam in this room at the given period, null if unused 
     */
    public ExamPlacement getPlacement(ExamPeriod period) { return iTable[period.getIndex()]; }
    /**
     * True if the room is available (for examination timetabling) during the given period
     * @param period a period
     * @return true if an exam can be scheduled into this room at the given period, false if otherwise
     */
    public boolean isAvailable(ExamPeriod period) { return iAvailable[period.getIndex()]; }
    /**
     * Set whether the room is available (for examination timetabling) during the given period
     * @param period a period
     * @param available true if an exam can be scheduled into this room at the given period, false if otherwise
     */
    public void setAvailable(int period, boolean available) { iAvailable[period]=available; }
    
    /**
     * Compute conflicts between the given assignment of an exam and all the current assignments (of this room)
     */
    public void computeConflicts(Value value, Set conflicts) {
        ExamPlacement p = (ExamPlacement)value;
        if (!p.getRooms().contains(this)) return; 
        if (iTable[p.getPeriod().getIndex()]!=null && !iTable[p.getPeriod().getIndex()].variable().equals(value.variable()))
            conflicts.add(iTable[p.getPeriod().getIndex()]);
    }
    
    /**
     * Checks whether there is a conflict between the given assignment of an exam and all the current assignments (of this room)
     */
    public boolean inConflict(Value value) {
        ExamPlacement p = (ExamPlacement)value;
        if (!p.getRooms().contains(this)) return false; 
        return iTable[p.getPeriod().getIndex()]!=null && !iTable[p.getPeriod().getIndex()].variable().equals(value.variable());
    }
    
    /**
     * False if the given two assignments are using this room at the same period 
     */
    public boolean isConsistent(Value value1, Value value2) {
        ExamPlacement p1 = (ExamPlacement)value1;
        ExamPlacement p2 = (ExamPlacement)value2;
        return (p1.getPeriod()!=p2.getPeriod() || !p1.getRooms().contains(this) || !p2.getRooms().contains(this));
    }
    
    /**
     * An exam was assigned, update room assignment table
     */
    public void assigned(long iteration, Value value) {
        super.assigned(iteration, value);
        ExamPlacement p = (ExamPlacement)value;
        if (!p.getRooms().contains(this)) return;
            iTable[p.getPeriod().getIndex()] = p;
    }
    
    /**
     * An exam was unassigned, update room assignment table
     */
    public void unassigned(long iteration, Value value) {
        super.unassigned(iteration, value);
        ExamPlacement p = (ExamPlacement)value;
        if (!p.getRooms().contains(this)) return;
            iTable[p.getPeriod().getIndex()] = null;
    }
    
    /**
     * Checks two rooms for equality
     */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof ExamRoom)) return false;
        ExamRoom r = (ExamRoom)o;
        return getRoomId().equals(r.getRoomId());
    }
    
    /**
     * Hash code
     */
    public int hashCode() {
        return iHashCode;
    }
    
    /**
     * Room unique id
     */
    public String toString() {
        return getRoomId();
    }
    
    /**
     * Compare two rooms (by unique id)
     */
    public int compareTo(Object o) {
        return toString().compareTo(o.toString());
    }
}
