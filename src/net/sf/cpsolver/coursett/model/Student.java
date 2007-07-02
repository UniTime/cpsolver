package net.sf.cpsolver.coursett.model;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import net.sf.cpsolver.coursett.constraint.JenrlConstraint;

/**
 * Student.
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
public class Student implements Comparable {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(Student.class);
	public static boolean USE_DISTANCE_CACHE = false;
    Long iStudentId = null;
    Hashtable iOfferings = new Hashtable();
    Set iLectures = new HashSet();
    Set iConfigurations = new HashSet();
    Hashtable iCanNotEnrollSections = null;
    Hashtable iDistanceCache = null;
    HashSet iCommitedPlacements = null;
    
    public Student(Long studentId) {
        iStudentId = studentId;
    }
    
    public void addOffering(Long offeringId, double weight) {
    	iOfferings.put(offeringId, new Double(weight));
    }
    public Hashtable getOfferingsMap() {
    	return iOfferings;
    }
    public Set getOfferings() {
    	return iOfferings.keySet();
    }
    public boolean hasOffering(Long offeringId) {
    	return iOfferings.containsKey(offeringId);
    }
    public double getOfferingWeight(Configuration configuration) {
        if (configuration==null) return 1.0;
        return getOfferingWeight(configuration.getOfferingId());
    }
    
    public double getOfferingWeight(Long offeringId) {
    	Double weight = (Double)iOfferings.get(offeringId);
    	return (weight==null?0.0:weight.doubleValue());
    }
    
    public boolean canEnroll(Lecture lecture) {
    	if (iCanNotEnrollSections!=null) {
    		HashSet canNotEnrollLectures = (HashSet)iCanNotEnrollSections.get(lecture.getConfiguration().getOfferingId());
    		return canEnroll(canNotEnrollLectures, lecture, true);
    	}
    	return true;
    }
    
    private boolean canEnroll(HashSet canNotEnrollLectures, Lecture lecture, boolean checkParents) {
    	if (canNotEnrollLectures==null) return true;
    	if (canNotEnrollLectures.contains(lecture)) return false;
    	if (checkParents) {
    		Lecture parent = lecture.getParent();
        	while (parent!=null) {
        		if (canNotEnrollLectures.contains(parent)) return false;
        		parent = parent.getParent();
        	}
    	}
    	if (lecture.hasAnyChildren()) {
    		for (Enumeration e=lecture.getChildrenSubpartIds();e.hasMoreElements();) {
    			Long subpartId = (Long)e.nextElement();
    			boolean canEnrollChild = false;
    			for (Enumeration f=lecture.getChildren(subpartId).elements();f.hasMoreElements();) {
    				Lecture childLecture = (Lecture)f.nextElement();
    				if (canEnroll(canNotEnrollLectures, childLecture, false)) {
    					canEnrollChild = true; break;
    				}
    			}
    			if (!canEnrollChild) return false;
    		}
    	}
    	return true;
    }
    
    public void addCanNotEnroll(Lecture lecture) {
    	if (iCanNotEnrollSections==null)
    		iCanNotEnrollSections = new Hashtable();
        if (lecture.getConfiguration()==null) {
            sLogger.warn("Student.addCanNotEnroll("+lecture+") -- given lecture has no configuration associated with.");
            return;
        }
    	HashSet canNotEnrollLectures = (HashSet)iCanNotEnrollSections.get(lecture.getConfiguration().getOfferingId());
    	if (canNotEnrollLectures==null) {
    		canNotEnrollLectures = new HashSet();
    		iCanNotEnrollSections.put(lecture.getConfiguration().getOfferingId(),canNotEnrollLectures); 
    	}
    	canNotEnrollLectures.add(lecture);
    }
    
    public void addCanNotEnroll(Long offeringId, Collection lectures) {
    	if (lectures==null || lectures.isEmpty()) return;
    	if (iCanNotEnrollSections==null)
    		iCanNotEnrollSections = new Hashtable();
    	HashSet canNotEnrollLectures = (HashSet)iCanNotEnrollSections.get(offeringId);
    	if (canNotEnrollLectures==null) {
    		canNotEnrollLectures = new HashSet();
    		iCanNotEnrollSections.put(offeringId,canNotEnrollLectures); 
    	}
    	canNotEnrollLectures.addAll(lectures);
    }

    public Hashtable canNotEnrollSections() {
    	return iCanNotEnrollSections;
    }

    public void addLecture(Lecture lecture) { iLectures.add(lecture); }
    public void removeLecture(Lecture lecture) { iLectures.remove(lecture); }
    public Set getLectures() { return iLectures; }
    
    public void addConfiguration(Configuration config) { iConfigurations.add(config); }
    public void removeConfiguration(Configuration config) { iConfigurations.remove(config); }
    public Set getConfigurations() { return iConfigurations; }

    public Long getId() { return iStudentId; }
    
    public double getDistance(Student student) {
    	Double dist = (USE_DISTANCE_CACHE && iDistanceCache!=null?(Double)iDistanceCache.get(student):null);
    	if (dist==null) {
            int same = 0;
            for (Iterator i=getOfferings().iterator();i.hasNext();) {
                if (student.getOfferings().contains(i.next())) same++;
            }
            double all = student.getOfferings().size() + getOfferings().size();
            double dif = all - 2.0*same;
            dist = new Double(dif/all);
            if (USE_DISTANCE_CACHE) {
            	if (iDistanceCache == null) iDistanceCache = new Hashtable();
            	iDistanceCache.put(student,dist);
            }
    	}
        return dist.doubleValue();
    }
    
    public void clearDistanceCache() {
    	if (USE_DISTANCE_CACHE && iDistanceCache!=null) iDistanceCache.clear();
    }
    
    public String toString() { return String.valueOf(getId()); }//+"/"+getOfferings(); }
    public int hashCode() { return getId().hashCode(); }
    public int compareTo(Object o) {
    	return getId().compareTo(((Student)o).getId());
    }
    public boolean equals(Object o) {
    	if (o==null || !(o instanceof Student)) return false;
    	return getId().equals(((Student)o).getId());
    }
    
    public void addCommitedPlacement(Placement placement) {
    	if (iCommitedPlacements==null)
    		iCommitedPlacements = new HashSet();
    	iCommitedPlacements.add(placement);
    }
    
    public Set getCommitedPlacements() { return iCommitedPlacements; }
    
    public Set conflictPlacements(Placement placement) {
    	if (iCommitedPlacements==null) return null;
    	Set ret = new HashSet();
    	Lecture lecture = (Lecture)placement.variable();
    	for (Iterator i=iCommitedPlacements.iterator();i.hasNext();) {
    		Placement commitedPlacement = (Placement)i.next();
    		Lecture commitedLecture = (Lecture)commitedPlacement.variable();
    		if (lecture.getSchedulingSubpartId()!=null && lecture.getSchedulingSubpartId().equals(commitedLecture.getSchedulingSubpartId())) continue;
    		if (JenrlConstraint.isInConflict(commitedPlacement, placement))
    			ret.add(commitedPlacement);
    	}
    	return ret;
    }
    
    public int countConflictPlacements(Placement placement) {
    	Set conflicts = conflictPlacements(placement);
    	double w = getOfferingWeight(((Lecture)placement.variable()).getConfiguration());
    	return (int)Math.round(conflicts==null?0:avg(w,1.0)*conflicts.size());
    }
    
    public double getJenrlWeight(Lecture l1, Lecture l2) {
    	return avg(getOfferingWeight(l1.getConfiguration()),getOfferingWeight(l2.getConfiguration()));
    }
    
    public double avg(double w1, double w2) {
    	return Math.sqrt(w1*w2);
    }
}
