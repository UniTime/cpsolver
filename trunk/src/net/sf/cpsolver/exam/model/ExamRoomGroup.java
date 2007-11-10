package net.sf.cpsolver.exam.model;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

public class ExamRoomGroup {
    private String iId;
    private Vector iRooms = new Vector();
    
    public ExamRoomGroup(String id) {
        iId = id; 
    }
    
    public String getId() {
        return iId;
    }
    
    public void addRoom(ExamRoom room) {
        iRooms.add(room);
    }
    
    public Vector getRooms() {
        return iRooms;
    }
    
    public int getMinSize() {
        if (iRooms.isEmpty()) return 0;
        int min = Integer.MAX_VALUE;
        for (Enumeration e=iRooms.elements();e.hasMoreElements();) {
            ExamRoom room = (ExamRoom)e.nextElement();
            min = Math.min(min, room.getSize());
        }
        return min;
    }

    public int getMaxSize() {
        if (iRooms.isEmpty()) return 0;
        int max = Integer.MIN_VALUE;
        for (Enumeration e=iRooms.elements();e.hasMoreElements();) {
            ExamRoom room = (ExamRoom)e.nextElement();
            max = Math.max(max, room.getSize());
        }
        return max;
    }

    public int getAvgSize() {
        if (iRooms.isEmpty()) return 0;
        int size = 0;
        for (Enumeration e=iRooms.elements();e.hasMoreElements();) {
            ExamRoom room = (ExamRoom)e.nextElement();
            size += room.getSize();
        }
        return size / iRooms.size();
    }
    
    public int getSpace() {
        if (iRooms.isEmpty()) return 0;
        int space = 0;
        for (Enumeration e=iRooms.elements();e.hasMoreElements();) {
            ExamRoom room = (ExamRoom)e.nextElement();
            space += room.getSize();
        }
        return space;
    }
    
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
    
    public int getMinAltSize() {
        if (iRooms.isEmpty()) return 0;
        int min = Integer.MAX_VALUE;
        for (Enumeration e=iRooms.elements();e.hasMoreElements();) {
            ExamRoom room = (ExamRoom)e.nextElement();
            min = Math.min(min, room.getAltSize());
        }
        return min;
    }

    public int getMaxAltSize() {
        if (iRooms.isEmpty()) return 0;
        int max = Integer.MIN_VALUE;
        for (Enumeration e=iRooms.elements();e.hasMoreElements();) {
            ExamRoom room = (ExamRoom)e.nextElement();
            max = Math.max(max, room.getAltSize());
        }
        return max;
    }

    public int getAvgAltSize() {
        if (iRooms.isEmpty()) return 0;
        int size = 0;
        for (Enumeration e=iRooms.elements();e.hasMoreElements();) {
            ExamRoom room = (ExamRoom)e.nextElement();
            size += room.getAltSize();
        }
        return size / iRooms.size();
    }
    
    public int getAltSpace() {
        if (iRooms.isEmpty()) return 0;
        int space = 0;
        for (Enumeration e=iRooms.elements();e.hasMoreElements();) {
            ExamRoom room = (ExamRoom)e.nextElement();
            space += room.getAltSize();
        }
        return space;
    }

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
