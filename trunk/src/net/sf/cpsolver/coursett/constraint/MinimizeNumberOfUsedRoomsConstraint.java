package net.sf.cpsolver.coursett.constraint;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.coursett.Constants;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.RoomLocation;
import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.util.DataProperties;

/** Minimize number of used rooms within the set of classes.
 * <br><br>
 * This constraint implements the following distribution/group constraint:
 * <br><br>
 * MIN_ROOM_USE (Minimize Number Of Rooms Used)<br> 
 * Minimize number of rooms used by the given set of classes.  
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

public class MinimizeNumberOfUsedRoomsConstraint extends Constraint implements WeakeningConstraint {
	private int iUnassignmentsToWeaken = 250;
	private long iUnassignment = 0;
	private int iLimit = 1;
	private Hashtable iUsedRooms = new Hashtable();
	boolean iEnabled = false;
	
	public MinimizeNumberOfUsedRoomsConstraint(DataProperties config) {
		iUnassignmentsToWeaken = config.getPropertyInt("MinimizeNumberOfUsedRooms.Unassignments2Weaken", iUnassignmentsToWeaken);
	}
	
	public boolean isOverLimit(Value value) {
		return getOverLimit(value)>0;
	}
	
    public int getOverLimit(Value value) {
    	if (!iEnabled) return 0; //not enabled
    	if (iUnassignmentsToWeaken==0) return 0; //not working
    	
		Placement placement = (Placement) value;
		Lecture lecture = (Lecture)placement.variable();
		
		if (lecture.getNrRooms()<=0) return 0; //no room
		if (lecture.roomLocations().size()==lecture.getNrRooms()) return 0; //required room
		if (lecture.isCommitted()) return 0; //commited class
		
		int usage = iUsedRooms.size();
		if (usage+lecture.getNrRooms()<=iLimit) return 0; //under the limit, quick check
		
		if (placement.isMultiRoom()) {
			HashSet assignedRooms = new HashSet();
			if (lecture.getAssignment()!=null)
				assignedRooms.addAll(((Placement)lecture.getAssignment()).getRoomLocations());
			for (Enumeration e=placement.getRoomLocations().elements();e.hasMoreElements();) {
				RoomLocation r = (RoomLocation)e.nextElement();
				if (assignedRooms.remove(r)) continue;
				Integer usageThisRoom = (Integer)iUsedRooms.get(r);
				if (!iUsedRooms.containsKey(r)) usage++; 
			}
			for (Iterator i=assignedRooms.iterator();i.hasNext();) {
				RoomLocation r = (RoomLocation)i.next();
				Set lects = (Set)iUsedRooms.get(r);
				if (lects!=null && lects.size()==1) usage--;
			}
		} else {
			RoomLocation assignedRoom = (lecture.getAssignment()!=null && !lecture.getAssignment().equals(placement)? ((Placement)lecture.getAssignment()).getRoomLocation() : null);
			RoomLocation room = placement.getRoomLocation();
			if (!room.equals(assignedRoom)) {
				if (!iUsedRooms.containsKey(room)) usage++;
				if (assignedRoom!=null) {
					Set lects = (Set)iUsedRooms.get(assignedRoom);
					if (lects!=null && lects.size()==1) usage--;
				}
			}
		}
		if (usage<=iUsedRooms.size()) return 0; //number of used rooms not changed
		if (usage<=iLimit) return 0; //under the limit
    	return usage-iLimit;
    }
    
    public void computeConflicts(Value value, Set conflicts) {
    	int overLimit = getOverLimit(value);
    	if (overLimit>0) {
    		Vector adepts = new Vector();
    		for (Enumeration e=iUsedRooms.elements();e.hasMoreElements();) {
    			Set lects = (Set)e.nextElement();
    			Vector placementsToUnassign = new Vector();
    			boolean canUnassign = true;
    			for (Iterator i=lects.iterator();i.hasNext();) {
    				Lecture l = (Lecture)i.next();
    				if (l.isCommitted()) {
    					canUnassign = false; break;
    				}
    				if (!conflicts.contains(l.getAssignment()))
    					placementsToUnassign.addElement(l.getAssignment());
    			}
    			if (!canUnassign) continue;
    			adepts.addElement(placementsToUnassign);
    		}
    		if (adepts.size()<overLimit) { 
    			conflicts.add(value);
    		} else {
    			Collections.sort(adepts, new Comparator() {
    				public int compare(Object o1, Object o2) {
    					Collection c1 = (Collection)o1;
    					Collection c2 = (Collection)o2;
    					return Double.compare(c1.size(), c2.size());
    				}
    			});
    			for (int i=0;i<overLimit;i++) {
    				conflicts.addAll((Collection)adepts.elementAt(i));
    			}
    		}
    	}
    }
    
    public boolean inConflict(Value value) {
    	return isOverLimit(value);
    }
    
    public boolean isConsistent(Value value1, Value value2) {
    	return (isOverLimit(value1) || isOverLimit(value2));
    }
    
    public void assigned(long iteration, Value value) {
    	super.assigned(iteration, value);
    	Placement placement = (Placement) value;
        Lecture lecture = (Lecture)placement.variable();
        if (lecture.getNrRooms()<=0) return;
        if (placement.isMultiRoom()) {
        	for (Enumeration e=placement.getRoomLocations().elements();e.hasMoreElements();) {
        		RoomLocation r = (RoomLocation)e.nextElement();
        		Set lects = (Set)iUsedRooms.get(r);
        		if (lects==null) {
        			lects = new HashSet();
        			iUsedRooms.put(r, lects);
        		}
        		lects.add(lecture);
        	}
        } else {
        	RoomLocation r = placement.getRoomLocation();
    		Set lects = (Set)iUsedRooms.get(r);
    		if (lects==null) {
    			lects = new HashSet();
    			iUsedRooms.put(r, lects);
    		}
    		lects.add(lecture);
        }
    }
    
    public void unassigned(long iteration, Value value) {
        super.unassigned(iteration, value);
        Placement placement = (Placement) value;
        Lecture lecture = (Lecture)placement.variable();
        if (lecture.getNrRooms()<=0) return;
        if (placement.isMultiRoom()) {
        	for (Enumeration e=placement.getRoomLocations().elements();e.hasMoreElements();) {
        		RoomLocation r = (RoomLocation)e.nextElement();
        		Set lects = (Set)iUsedRooms.get(r);
        		if (lects!=null) {
        			lects.remove(lecture);
        			if (lects.isEmpty())
        				iUsedRooms.remove(r);
        		}
        	}
        } else {
        	RoomLocation r = placement.getRoomLocation();
    		Set lects = (Set)iUsedRooms.get(r);
    		if (lects!=null) {
    			lects.remove(lecture);
    			if (lects.isEmpty())
    				iUsedRooms.remove(r);
    		}
        }
    }
    
    public void weaken() {
    	iUnassignment++;
    	if (iUnassignmentsToWeaken>0 && iUnassignment%iUnassignmentsToWeaken==0) iLimit++;
    }
    
    public String getName() { return "Minimize number of used rooms"; }
    
    public int estimateLimit() {
    	HashSet mandatoryRooms = new HashSet();
    	for (Enumeration e=variables().elements();e.hasMoreElements();) {
    		Lecture lecture = (Lecture)e.nextElement();
    		if (lecture.getNrRooms()==0) continue;
    		if (lecture.isCommitted() || lecture.roomLocations().size()==1)
    			mandatoryRooms.addAll(lecture.roomLocations());
    	}
        double histogram[][] = new double[Constants.SLOTS_PER_DAY][Constants.NR_DAYS_WEEK];
        for (int i=0;i<Constants.SLOTS_PER_DAY_NO_EVENINGS;i++)
            for (int j=0;j<Constants.NR_DAYS_WEEK;j++)
            	histogram[i][j]=0.0;
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            Lecture lecture = (Lecture)e.nextElement();
            if (lecture.getNrRooms()==0) continue;
            Vector values = lecture.values();
            Placement firstPlacement = (values.isEmpty()?null:(Placement)values.firstElement());
            for (Enumeration e2=lecture.values().elements();e2.hasMoreElements();) {
                Placement p = (Placement)e2.nextElement();
                int firstSlot = p.getTimeLocation().getStartSlot();
                if (firstSlot>Constants.DAY_SLOTS_LAST) continue;
                int endSlot = firstSlot+p.getTimeLocation().getNrSlotsPerMeeting()-1;
                if (endSlot<Constants.DAY_SLOTS_FIRST) continue;
                for (int i=firstSlot;i<=endSlot;i++) {
                    int dayCode = p.getTimeLocation().getDayCode();
                    for (int j=0;j<Constants.NR_DAYS_WEEK;j++) {
                        if ((dayCode & Constants.DAY_CODES[j])!=0) {
                            histogram[i][j] += ((double)lecture.getNrRooms()) / values.size();
                        }
                    }
                }
            }
        }
        int maxAverageRooms = 0;
        for (int i=0;i<Constants.SLOTS_PER_DAY_NO_EVENINGS;i++)
            for (int j=0;j<Constants.NR_DAYS_WEEK;j++)
            	maxAverageRooms = Math.max(maxAverageRooms, (int)Math.ceil(histogram[i][j]));
         return Math.max(1, Math.max(mandatoryRooms.size(), maxAverageRooms));
    }
    
    public void setEnabled(boolean enabled) { 
    	iEnabled = enabled;
    	iLimit = Math.max(iUsedRooms.size(), estimateLimit());
    }
    public boolean isEnabled() { return iEnabled; }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Minimize Number Of Rooms Used between ");
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            Variable v = (Variable)e.nextElement();
            sb.append(v.getName());
            if (e.hasMoreElements()) sb.append(", ");
        }
        return sb.toString();
    }
}
