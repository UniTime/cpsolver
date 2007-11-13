package net.sf.cpsolver.exam.model;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;
/**
 * Representation of a group of rooms.
 * Each room belongs to one or more room groups. 
 * Each exam has one or more room groups associated with. 
 * Only rooms of room groups that are associated with an exam can be used 
 * by the exam. 
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
public class ExamRoomGroup {
    private String iId;
    private Vector iRooms = new Vector();
    
    /**
     * Constructor
     * @param id room group unique id
     */
    public ExamRoomGroup(String id) {
        iId = id; 
    }
    
    /**
     * Room group unique id
     */
    public String getId() {
        return iId;
    }

    /**
     * Add room to the room group
     * @param room a room
     */
    public void addRoom(ExamRoom room) {
        iRooms.add(room);
    }
    
    /**
     * Return list of rooms in the room group
     * @return list of {@link ExamRoom}
     */
    public Vector getRooms() {
        return iRooms;
    }
    
    /**
     * Size of the smallest room in the group (using {@link ExamRoom#getSize()}) 
     */
    public int getMinSize() {
        if (iRooms.isEmpty()) return 0;
        int min = Integer.MAX_VALUE;
        for (Enumeration e=iRooms.elements();e.hasMoreElements();) {
            ExamRoom room = (ExamRoom)e.nextElement();
            min = Math.min(min, room.getSize());
        }
        return min;
    }

    /**
     * Size of the largest room in the group (using {@link ExamRoom#getSize()}) 
     */
    public int getMaxSize() {
        if (iRooms.isEmpty()) return 0;
        int max = Integer.MIN_VALUE;
        for (Enumeration e=iRooms.elements();e.hasMoreElements();) {
            ExamRoom room = (ExamRoom)e.nextElement();
            max = Math.max(max, room.getSize());
        }
        return max;
    }

    /**
     * Average size of the room in the group (using {@link ExamRoom#getSize()}) 
     */
    public int getAvgSize() {
        if (iRooms.isEmpty()) return 0;
        int size = 0;
        for (Enumeration e=iRooms.elements();e.hasMoreElements();) {
            ExamRoom room = (ExamRoom)e.nextElement();
            size += room.getSize();
        }
        return size / iRooms.size();
    }
    
    /**
     * Total size of all rooms in the group (using {@link ExamRoom#getSize()}) 
     */
    public int getSpace() {
        if (iRooms.isEmpty()) return 0;
        int space = 0;
        for (Enumeration e=iRooms.elements();e.hasMoreElements();) {
            ExamRoom room = (ExamRoom)e.nextElement();
            space += room.getSize();
        }
        return space;
    }
    
    /**
     * Size of the median room in the group (using {@link ExamRoom#getSize()}) 
     */
    public int getMedSize() {
        if (iRooms.isEmpty()) return 0;
        Vector sizes = new Vector();
        for (Enumeration e=iRooms.elements();e.hasMoreElements();) {
            ExamRoom room = (ExamRoom)e.nextElement();
            sizes.add(new Integer(room.getSize()));
        }
        Collections.sort(sizes);
        return ((Integer)sizes.elementAt(sizes.size()/2)).intValue();
    }
    
    /**
     * Size of the smallest room in the group (using {@link ExamRoom#getAltSize()}) 
     */
    public int getMinAltSize() {
        if (iRooms.isEmpty()) return 0;
        int min = Integer.MAX_VALUE;
        for (Enumeration e=iRooms.elements();e.hasMoreElements();) {
            ExamRoom room = (ExamRoom)e.nextElement();
            min = Math.min(min, room.getAltSize());
        }
        return min;
    }

    /**
     * Size of the largest room in the group (using {@link ExamRoom#getAltSize()}) 
     */
    public int getMaxAltSize() {
        if (iRooms.isEmpty()) return 0;
        int max = Integer.MIN_VALUE;
        for (Enumeration e=iRooms.elements();e.hasMoreElements();) {
            ExamRoom room = (ExamRoom)e.nextElement();
            max = Math.max(max, room.getAltSize());
        }
        return max;
    }

    /**
     * Average size of the room in the group (using {@link ExamRoom#getAltSize()}) 
     */
    public int getAvgAltSize() {
        if (iRooms.isEmpty()) return 0;
        int size = 0;
        for (Enumeration e=iRooms.elements();e.hasMoreElements();) {
            ExamRoom room = (ExamRoom)e.nextElement();
            size += room.getAltSize();
        }
        return size / iRooms.size();
    }
    
    /**
     * Total size of all rooms in the group (using {@link ExamRoom#getAltSize()}) 
     */
    public int getAltSpace() {
        if (iRooms.isEmpty()) return 0;
        int space = 0;
        for (Enumeration e=iRooms.elements();e.hasMoreElements();) {
            ExamRoom room = (ExamRoom)e.nextElement();
            space += room.getAltSize();
        }
        return space;
    }

    /**
     * Size of the median room in the group (using {@link ExamRoom#getAltSize()}) 
     */
    public int getMedAltSize() {
        if (iRooms.isEmpty()) return 0;
        Vector sizes = new Vector();
        for (Enumeration e=iRooms.elements();e.hasMoreElements();) {
            ExamRoom room = (ExamRoom)e.nextElement();
            sizes.add(new Integer(room.getAltSize()));
        }
        Collections.sort(sizes);
        return ((Integer)sizes.elementAt(sizes.size()/2)).intValue();
    }
}
