package net.sf.cpsolver.coursett.constraint;

import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.coursett.Constants;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.util.FastVector;

/**
 * Instructor constraint.
 * <br>
 * Classes with this instructor can not overlap in time. Also, for back-to-back classes, 
 * there is the following reasoning:<ul>
 * <li>if the distance is equal or below {@link TimetableModel#getInstructorNoPreferenceLimit()} .. no preference
 * <li>if the distance is above {@link TimetableModel#getInstructorNoPreferenceLimit()} and below {@link TimetableModel#getInstructorDiscouragedLimit()} .. constraint is discouraged (soft, preference = 1)
 * <li>if the distance is above {@link TimetableModel#getInstructorDiscouragedLimit()} and below {@link TimetableModel#getInstructorProhibitedLimit()} .. constraint is strongly discouraged (soft, preference = 2)
 * <li>if the distance is above {@link TimetableModel#getInstructorProhibitedLimit()} .. constraint is prohibited (hard)
 * </ul>
 * <br>
 * When {@link InstructorConstraint#isIgnoreDistances()} is set to true, the constraint never prohibits two back-to-back classes (but it still tries to minimize the above back-to-back preferences).
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

public class InstructorConstraint extends Constraint {
    
    public int iPreference = 0;
    
    /** table iResource[slot] = lecture using this resource placed in the given time slot (null if empty) */
    protected Vector[] iResource;
    private Long iResourceId;
    private String iName;
    private String iPuid;
    private Vector[] iAvailable = null;
    private boolean iIgnoreDistances = false;
    private Integer iType = null;
    
    
    /** Constructor
     * @param id instructor id
     * @param name instructor name
     */
    public InstructorConstraint(Long id, String puid, String name, boolean ignDist) {
        iResourceId = id;
        iName = name;
        iPuid = puid;
        iIgnoreDistances = ignDist;
        iResource = new Vector[Constants.SLOTS_PER_DAY * Constants.DAY_CODES.length];
        for (int i=0;i<iResource.length;i++)
            iResource[i]=new FastVector(3);
    }

    public Vector getPlacements(int slot, Placement placement) {
    	return getPlacements(slot, placement.getTimeLocation().getWeekCode());
    }
    
    public Vector getPlacements(int slot, BitSet weekCode) {
    	Vector placements = new FastVector(iResource[slot].size());
    	for (Enumeration e=iResource[slot].elements();e.hasMoreElements();) {
    		Placement p = (Placement)e.nextElement();
    		if (p.getTimeLocation().shareWeeks(weekCode))
    			placements.addElement(p);
    	}
    	return placements;
    }

    public Placement getPlacement(int slot, int day) {
    	for (Enumeration e=iResource[slot].elements();e.hasMoreElements();) {
    		Placement p = (Placement)e.nextElement();
    		if (p.getTimeLocation().hasDay(day))
    			return p;
    	}
    	return null;
    }

    public void setNotAvailable(Placement placement) {
    	if (iAvailable==null) {
    		iAvailable = new Vector[Constants.SLOTS_PER_DAY * Constants.DAY_CODES.length];
    		for (int i=0;i<iResource.length;i++)
    			iAvailable[i]=null;
    	}
		for (Enumeration e=placement.getTimeLocation().getSlots();e.hasMoreElements();) {
			int slot = ((Integer)e.nextElement()).intValue();
            if (iAvailable[slot]==null)
                iAvailable[slot] = new Vector(1);
			iAvailable[slot].addElement(placement);
		}
    }
    public boolean isAvailable(int slot) {
    	if (iAvailable==null) return true;
    	return (iAvailable[slot]==null || iAvailable[slot].isEmpty());
    }
    public boolean isAvailable(Lecture lecture, TimeLocation time) {
    	if (iAvailable==null) return true;
    	for (Enumeration e=time.getSlots();e.hasMoreElements();) {
    		int slot = ((Integer)e.nextElement()).intValue();
    		if (iAvailable[slot]!=null) {
                for (Enumeration f=iAvailable[slot].elements();f.hasMoreElements();) {
                    Placement p = (Placement)f.nextElement();
                    if (lecture.canShareRoom((Lecture)p.variable())) continue;
                    if (time.shareWeeks(p.getTimeLocation())) return false;
                }
            }
    	}
    	return true;
    }
    public boolean isAvailable(Lecture lecture, Placement placement) {
    	if (iAvailable==null) return true;
    	for (Enumeration e=placement.getTimeLocation().getSlots();e.hasMoreElements();) {
    		int slot = ((Integer)e.nextElement()).intValue();
            if (iAvailable[slot]!=null) {
                for (Enumeration f=iAvailable[slot].elements();f.hasMoreElements();) {
                    Placement p = (Placement)f.nextElement();
                    if (lecture.canShareRoom((Lecture)p.variable()) && placement.sameRooms(p)) continue;
                    if (placement.getTimeLocation().shareWeeks(p.getTimeLocation())) return false;
                }
            }
    	}
        if (!iIgnoreDistances) {
            TimetableModel m = (TimetableModel)getModel();
            for (Enumeration e=placement.getTimeLocation().getStartSlots();e.hasMoreElements();) {
                int startSlot = ((Integer)e.nextElement()).intValue();
                int prevSlot = startSlot-1;
                if (prevSlot>=0 && (prevSlot/Constants.SLOTS_PER_DAY) == (startSlot/Constants.SLOTS_PER_DAY)) {
                    if (iAvailable[prevSlot]!=null) {
                        for (Enumeration f=iAvailable[prevSlot].elements();f.hasMoreElements();) {
                            Placement p = (Placement)f.nextElement();
                            if (lecture.canShareRoom((Lecture)p.variable()) && placement.sameRooms(p)) continue;
                            if (placement.getTimeLocation().shareWeeks(p.getTimeLocation()) && Placement.getDistance(p,placement)>m.getInstructorProhibitedLimit())
                                return false;
                        }
                    }
                }
                int nextSlot = startSlot+placement.getTimeLocation().getLength();
                if ((nextSlot/Constants.SLOTS_PER_DAY) == (startSlot/Constants.SLOTS_PER_DAY)) {
                    if (iAvailable[nextSlot]!=null) {
                        for (Enumeration f=iAvailable[nextSlot].elements();f.hasMoreElements();) {
                            Placement p = (Placement)f.nextElement();
                            if (lecture.canShareRoom((Lecture)p.variable()) && placement.sameRooms(p)) continue;
                            if (placement.getTimeLocation().shareWeeks(p.getTimeLocation()) && Placement.getDistance(p,placement)>m.getInstructorProhibitedLimit())
                                return false;
                        }
                    }
                }
            }
        }
    	return true;
    }
    public Vector[] getAvailableArray() {
    	return iAvailable;
    }
    
    /** Back-to-back preference of two placements (3 means prohibited) */
    public int getDistancePreference(Placement p1, Placement p2) {
        if (!p1.getTimeLocation().shareDays(p2.getTimeLocation())) return 0;
        if (!p1.getTimeLocation().shareWeeks(p2.getTimeLocation())) return 0;
        int s1 = p1.getTimeLocation().getStartSlot() % Constants.SLOTS_PER_DAY;
        int s2 = p2.getTimeLocation().getStartSlot() % Constants.SLOTS_PER_DAY;
        if (s1+p1.getTimeLocation().getLength()!=s2 && s2+p2.getTimeLocation().getLength()!=s1) return 0;
        double distance = Placement.getDistance(p1,p2);
    	TimetableModel m = (TimetableModel)p1.variable().getModel();
        if (distance<=m.getInstructorNoPreferenceLimit()) return Constants.sPreferenceLevelNeutral;
        if (distance<=m.getInstructorDiscouragedLimit()) return Constants.sPreferenceLevelDiscouraged;
        if (iIgnoreDistances || distance<=m.getInstructorProhibitedLimit()) return Constants.sPreferenceLevelStronglyDiscouraged;
        return Constants.sPreferenceLevelProhibited;
    }
    
    /** Resource id */
    public Long getResourceId() { return iResourceId; }
    /** Resource name */
    public String getName() { return iName; }

    public void computeConflicts(Value value, Set conflicts) {
        Lecture lecture = (Lecture) value.variable();
        Placement placement = (Placement) value;
        BitSet weekCode = placement.getTimeLocation().getWeekCode();
        
        for (Enumeration e=placement.getTimeLocation().getSlots();e.hasMoreElements();) {
        	int slot = ((Integer)e.nextElement()).intValue();
        	for (Enumeration f=iResource[slot].elements();f.hasMoreElements();) {
        		Placement p = (Placement)f.nextElement();
        		if (!p.equals(lecture.getAssignment()) && p.getTimeLocation().shareWeeks(weekCode)) {
        			if (p.canShareRooms(placement) && p.sameRooms(placement)) continue;
        			conflicts.add(p);
        		}
        	}
        }
        if (!iIgnoreDistances) {
            TimetableModel m = (TimetableModel)getModel();
            for (Enumeration e=placement.getTimeLocation().getStartSlots();e.hasMoreElements();) {
                int startSlot = ((Integer)e.nextElement()).intValue();
                int prevSlot = startSlot-1;
                if (prevSlot>=0 && (prevSlot/Constants.SLOTS_PER_DAY) == (startSlot/Constants.SLOTS_PER_DAY)) {
                    Vector conf = getPlacements(prevSlot,placement);
                    for (Enumeration i=conf.elements();i.hasMoreElements();) {
                        Placement c = (Placement)i.nextElement();
                        if (lecture.equals(c.variable())) continue;
                        if (Placement.getDistance(placement,c)>m.getInstructorProhibitedLimit()) {
                            if (c.canShareRooms(placement) && c.sameRooms(placement)) continue;
                            conflicts.add(c);
                        }
                    }
                }
                int nextSlot = startSlot+placement.getTimeLocation().getLength();
                if ((nextSlot/Constants.SLOTS_PER_DAY) == (startSlot/Constants.SLOTS_PER_DAY)) {
                    Vector conf = getPlacements(nextSlot,placement);
                    for (Enumeration i=conf.elements();i.hasMoreElements();) {
                        Placement c = (Placement)i.nextElement();
                        if (lecture.equals(c.variable())) continue;
                        if (Placement.getDistance(placement,c)>m.getInstructorProhibitedLimit()) {
                            if (c.canShareRooms(placement) && c.sameRooms(placement)) continue;
                            conflicts.add(c);
                        }
                    }
                }
            }
        }
    }
    
    public boolean inConflict(Value value) {
        Lecture lecture = (Lecture) value.variable();
        Placement placement = (Placement) value;
        BitSet weekCode = placement.getTimeLocation().getWeekCode();
        for (Enumeration e=placement.getTimeLocation().getSlots();e.hasMoreElements();) {
        	int slot = ((Integer)e.nextElement()).intValue();
        	for (Enumeration f=iResource[slot].elements();f.hasMoreElements();) {
        		Placement p = (Placement)f.nextElement();
        		if (!p.equals(lecture.getAssignment()) && p.getTimeLocation().shareWeeks(weekCode)) {
        			if (p.canShareRooms(placement) && p.sameRooms(placement)) continue;
        			return true;
        		}
        	}
        }
        if (!iIgnoreDistances) {
            TimetableModel m = (TimetableModel)getModel();
            for (Enumeration e=placement.getTimeLocation().getStartSlots();e.hasMoreElements();) {
                int startSlot = ((Integer)e.nextElement()).intValue();
                int prevSlot = startSlot-1;
                if (prevSlot>=0 && (prevSlot/Constants.SLOTS_PER_DAY) == (startSlot/Constants.SLOTS_PER_DAY)) {
                    Vector conf = getPlacements(prevSlot,placement);
                    for (Enumeration i=conf.elements();i.hasMoreElements();) {
                        Placement c = (Placement)i.nextElement();
                        if (lecture.equals(c.variable())) continue;
                        if (Placement.getDistance(placement,c)>m.getInstructorProhibitedLimit()) {
                            if (c.canShareRooms(placement) && c.sameRooms(placement)) continue;
                            return true;
                        }
                    }
                }
                int nextSlot = startSlot+placement.getTimeLocation().getLength();
                if ((nextSlot/Constants.SLOTS_PER_DAY) == (startSlot/Constants.SLOTS_PER_DAY)) {
                    Vector conf = getPlacements(nextSlot,placement);
                    for (Enumeration i=conf.elements();i.hasMoreElements();) {
                        Placement c = (Placement)i.nextElement();
                        if (lecture.equals(c.variable())) continue;
                        if (Placement.getDistance(placement,c)>m.getInstructorProhibitedLimit()) {
                            if (c.canShareRooms(placement) && c.sameRooms(placement)) continue;
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    public boolean isConsistent(Value value1, Value value2) {
        Placement p1 = (Placement) value1;
        Placement p2 = (Placement) value2;
        if (p1.canShareRooms(p2) && p1.sameRooms(p2)) return true;
        if (p1.getTimeLocation().hasIntersection(p2.getTimeLocation())) return false;
        return getDistancePreference(p1,p2)!=Constants.sPreferenceLevelProhibited;
    }
    
    public void assigned(long iteration, Value value) {
        super.assigned(iteration, value);
        Placement placement = (Placement) value;
        iPreference += getPreference(value);
        for (Enumeration e=placement.getTimeLocation().getSlots();e.hasMoreElements();) {
        	int slot = ((Integer)e.nextElement()).intValue();
            iResource[slot].addElement(placement);
        }
    }
    
    public void unassigned(long iteration, Value value) {
        super.unassigned(iteration, value);
        Placement placement = (Placement) value;
        iPreference -= getPreference(value);
        for (Enumeration e=placement.getTimeLocation().getSlots();e.hasMoreElements();) {
        	int slot = ((Integer)e.nextElement()).intValue();
            iResource[slot].removeElement(placement);
        }
    }
    
    /** Lookup table getResource()[slot] -> lecture using this resource placed in the given time slot (null if empty) */
    public Vector getResource(int slot) { return iResource[slot]; }
    public Placement[] getResourceOfWeek(int startDay) {
    	Placement[] ret = new Placement[iResource.length];
    	for (int i=0;i<iResource.length;i++) {
    		ret[i]=getPlacement(i,startDay+(i/Constants.SLOTS_PER_DAY));
    	}
    	return ret;
    }

    /** Number of useless slots for this resource */
    public int countUselessSlots() {
        int ret = 0;
        for (int d=0;d<Constants.DAY_CODES.length;d++) {
            for (int s=1;s<Constants.SLOTS_PER_DAY-1;s++) {
                int slot = d*Constants.SLOTS_PER_DAY+s;
                if (iResource[slot-1]!=null && iResource[slot]==null && iResource[slot+1]!=null)
                    ret++;
            }
        }
        return ret;
    }
    
    /** Resource usage usage */
    protected void printUsage(StringBuffer sb) {
        for (int slot=0;slot<iResource.length;slot++) {
            for (Enumeration e=iResource[slot].elements();e.hasMoreElements();) {
            	Placement p = (Placement)e.nextElement();
                int day = slot / Constants.SLOTS_PER_DAY;
                int time = slot * Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN;
                int h = time / 60;
                int m = time % 60;
                String d = Constants.DAY_NAMES_SHORT[day];
                int slots = p.getTimeLocation().getLength();
                time += (30*slots);
                int h2 = time / 60;
                int m2 = time % 60;
                sb.append(sb.length()==0?"":",\n        ").append("["+d+(h>12?h-12:h)+":"+(m<10?"0":"")+m+(h>=12?"p":"a")+"-"+(h2>12?h2-12:h2)+":"+(m2<10?"0":"")+m2+(h2>=12?"p":"a")+"]=").append(p.variable().getName());
                slot+=slots-1;
                //sb.append(sb.length()==0?"":", ").append("s"+(slot+1)+"=").append(((Lecture)getResource()[slot]).getName());
            }
        }
    }

    public String toString() {
        return "Instructor "+getName();
    }    
    
    /** Back-to-back preference of the given placement */
    public int getPreference(Value value) {
        Lecture lecture = (Lecture)value.variable();
        Placement placement = (Placement)value;
        int pref = 0;
        TimetableModel m = (TimetableModel)getModel();
        HashSet used = new HashSet();
        for (Enumeration e=placement.getTimeLocation().getStartSlots();e.hasMoreElements();) {
        	int startSlot = ((Integer)e.nextElement()).intValue();
            int prevSlot = startSlot-1;
            if (prevSlot>=0 && (prevSlot/Constants.SLOTS_PER_DAY) == (startSlot/Constants.SLOTS_PER_DAY)) {
                Vector conf = getPlacements(prevSlot,placement);
                for (Enumeration i=conf.elements();i.hasMoreElements();) {
                	Placement c = (Placement)i.nextElement();
                	if (lecture.equals(c.variable())) continue;
                	if (!used.add(c)) continue; 
                	double dist = Placement.getDistance(placement,c);
                	if (dist>m.getInstructorNoPreferenceLimit() && dist<=m.getInstructorDiscouragedLimit()) pref+=Constants.sPreferenceLevelDiscouraged;
                	if (dist>m.getInstructorDiscouragedLimit() && (dist<=m.getInstructorProhibitedLimit() || iIgnoreDistances)) pref+=Constants.sPreferenceLevelStronglyDiscouraged;
                	if (!iIgnoreDistances && dist>m.getInstructorProhibitedLimit()) pref += Constants.sPreferenceLevelProhibited;
                }
            }
            int nextSlot = startSlot+placement.getTimeLocation().getLength();
            if ((nextSlot/Constants.SLOTS_PER_DAY) == (startSlot/Constants.SLOTS_PER_DAY)) {
            	Vector conf = getPlacements(nextSlot,placement);
                for (Enumeration i=conf.elements();i.hasMoreElements();) {
                	Placement c = (Placement)i.nextElement();
                	if (lecture.equals(c.variable())) continue;
                	if (!used.add(c)) continue; 
                	double dist = Placement.getDistance(placement,c);
                	if (dist>m.getInstructorNoPreferenceLimit() && dist<=m.getInstructorDiscouragedLimit()) pref+=Constants.sPreferenceLevelDiscouraged;
                	if (dist>m.getInstructorDiscouragedLimit() && (dist<=m.getInstructorProhibitedLimit() || iIgnoreDistances)) pref+=Constants.sPreferenceLevelStronglyDiscouraged;
                	if (!iIgnoreDistances && dist>m.getInstructorProhibitedLimit()) pref = Constants.sPreferenceLevelProhibited;
                }
            }
        }
        return pref;
    }

    public int getPreferenceCombination(Value value) {
        Lecture lecture = (Lecture)value.variable();
        Placement placement = (Placement)value;
        int pref = 0;
        HashSet used = new HashSet();
        TimetableModel m = (TimetableModel)getModel();
        for (Enumeration e=placement.getTimeLocation().getStartSlots();e.hasMoreElements();) {
        	int startSlot = ((Integer)e.nextElement()).intValue();
            int prevSlot = startSlot-1;
            if (prevSlot>=0 && (prevSlot/Constants.SLOTS_PER_DAY) == (startSlot/Constants.SLOTS_PER_DAY)) {
                Vector conf = getPlacements(prevSlot,placement);
                for (Enumeration i=conf.elements();i.hasMoreElements();) {
                	Placement c = (Placement)i.nextElement();
                	if (lecture.equals(c.variable())) continue;
                	if (!used.add(c)) continue; 
                	double dist = Placement.getDistance(placement,c);
                	if (dist>m.getInstructorNoPreferenceLimit() && dist<=m.getInstructorDiscouragedLimit()) pref = Math.max(pref,Constants.sPreferenceLevelDiscouraged);
                	if (dist>m.getInstructorDiscouragedLimit() && (dist<=m.getInstructorProhibitedLimit() || iIgnoreDistances)) pref = Math.max(pref,Constants.sPreferenceLevelStronglyDiscouraged);
                	if (!iIgnoreDistances && dist>m.getInstructorProhibitedLimit()) pref = Math.max(pref,Constants.sPreferenceLevelProhibited);
                }
            }
            int nextSlot = startSlot+placement.getTimeLocation().getLength();
            if ((nextSlot/Constants.SLOTS_PER_DAY) == (startSlot/Constants.SLOTS_PER_DAY)) {
            	Vector conf = getPlacements(nextSlot,placement);
                for (Enumeration i=conf.elements();i.hasMoreElements();) {
                	Placement c = (Placement)i.nextElement();
                	if (lecture.equals(c.variable())) continue;
                	if (!used.add(c)) continue;
                	double dist = Placement.getDistance(placement,c);
                	if (dist>m.getInstructorNoPreferenceLimit() && dist<=m.getInstructorDiscouragedLimit()) pref = Math.max(pref,Constants.sPreferenceLevelDiscouraged);
                	if (dist>m.getInstructorDiscouragedLimit() && (dist<=m.getInstructorProhibitedLimit() || iIgnoreDistances)) pref = Math.max(pref,Constants.sPreferenceLevelStronglyDiscouraged);
                	if (!iIgnoreDistances && dist>m.getInstructorProhibitedLimit()) pref = Math.max(pref,Constants.sPreferenceLevelProhibited);
                }
            }
        }
        return pref;
    }

    /** Overall back-to-back preference of this instructor */
    public int getPreference() {
    	/*
    	if (iPreference!=countPreference()) {
    		System.err.println("InstructorConstraint.getPreference() is not working properly");
    	}
    	*/
    	return iPreference;
    }
    public int countPreference() {
        int pref = 0;
        HashSet used = new HashSet();
        TimetableModel m = (TimetableModel)getModel();
        for (int slot=1;slot<iResource.length;slot++) {
            if ((slot%Constants.SLOTS_PER_DAY)==0) continue;
            for (Enumeration e=iResource[slot].elements();e.hasMoreElements();) {
            	Placement placement = (Placement)e.nextElement();
            	Vector prevPlacements = getPlacements(slot-1,placement);
                for (Enumeration i=prevPlacements.elements();i.hasMoreElements();) {
                	Placement prevPlacement = (Placement)i.nextElement();
            		if (!used.add(prevPlacement)) continue; 
            		double dist = Placement.getDistance(prevPlacement,placement);
            		if (dist>m.getInstructorNoPreferenceLimit() && dist<=m.getInstructorDiscouragedLimit()) pref+=Constants.sPreferenceLevelDiscouraged;
            		if (dist>m.getInstructorDiscouragedLimit()) pref+=Constants.sPreferenceLevelStronglyDiscouraged;
            	}
            }
        }
        return pref;
    }
    
    /** Worst back-to-back preference of this instructor */
    public int getWorstPreference() {
        return Constants.sPreferenceLevelStronglyDiscouraged*(variables().size()-1);
    }
    
    public String getPuid() {
    	return iPuid;
    }
    
    public boolean isIgnoreDistances() {
        return iIgnoreDistances;
    }

    public Integer getType() { return iType; }
    public void setType(Integer type) { iType = type; }
}
