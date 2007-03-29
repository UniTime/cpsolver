package net.sf.cpsolver.coursett.model;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.ifs.util.FastVector;

/**
 * Configuration. Each course can have multiple configurations. 
 * A student needs to be enrolled into classes of one of the configurations.
 * 
 * @version
 * CourseTT 1.1 (University Course Timetabling)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@ktiml.mff.cuni.cz">muller@ktiml.mff.cuni.cz</a><br>
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

public class Configuration {
	private Long iConfigId = null;
	private Long iOfferingId = null;
	private Hashtable iTopLectures = new Hashtable();
	private Vector iAltConfigurations = null;
	private int iLimit = -1;
	
	public Configuration(Long offeringId, Long configId, int limit) {
		iOfferingId = offeringId;
		iConfigId = configId;
		iLimit = limit;
	}
	
	public Long getOfferingId() { return iOfferingId; }
	public Long getConfigId() { return iConfigId; }
	
	public void addTopLecture(Lecture lecture) {
		Set lectures = (Set)iTopLectures.get(lecture.getSchedulingSubpartId());
		if (lectures==null) {
			lectures = new HashSet();
			iTopLectures.put(lecture.getSchedulingSubpartId(), lectures);
		}
		lectures.add(lecture);
	}
	
	public Enumeration getTopSubpartIds() {
		return iTopLectures.keys();
	}
	
	public Set getTopLectures(Long subpartId) {
		return (Set)iTopLectures.get(subpartId);
	}
	
	public void setAltConfigurations(Vector altConfigurations) {
		iAltConfigurations = altConfigurations;
	}
	
	public void addAltConfiguration(Configuration configuration) {
		if (iAltConfigurations==null)
			iAltConfigurations = new FastVector();
		iAltConfigurations.addElement(configuration);
	}

	
	public Vector getAltConfigurations() {
		return iAltConfigurations;
	}
	
	public Set students() {
		Set students = new HashSet();
		for (Enumeration e=iTopLectures.elements();e.hasMoreElements();) {
			HashSet lectures = (HashSet)e.nextElement();
			for (Iterator i=lectures.iterator();i.hasNext();) {
				Lecture l = (Lecture)i.next();
				students.addAll(l.students());
			}
		}
		return students;
	}

	public boolean hasConflict(Student student) {
		for (Iterator i=student.getLectures().iterator();i.hasNext();) {
        	Lecture lecture = (Lecture)i.next();
        	if (lecture.getAssignment()==null || !this.equals(lecture.getConfiguration())) continue;
        	if (student.countConflictPlacements((Placement)lecture.getAssignment())>0) return true;
        	for (Iterator j=student.getLectures().iterator();j.hasNext();) {
        		Lecture x = (Lecture)j.next();
        		if (x.getAssignment()==null || x.equals(lecture)) continue;
        		if (lecture.jenrlConstraint(x).isInConflict()) return true;
        	}
		}
		return false;
	}
	
	public int getLimit() {
        if (iLimit<0) {
            double totalWeight = 0.0;
            for (Iterator i=students().iterator();i.hasNext();) {
                Student s = (Student)i.next();
                totalWeight += s.getOfferingWeight(getOfferingId());
            }
            iLimit = (int)Math.round(totalWeight);
        }
		return iLimit;
	}
	
	public int hashCode() { return getConfigId().hashCode(); }
	public boolean equals(Object o) {
		if (o==null || !(o instanceof Configuration)) return false;
		return getConfigId().equals(((Configuration)o).getConfigId());
	}
}