package net.sf.cpsolver.coursett.constraint;

import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Value;

/**
 * Class limit constraint.
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

public class ClassLimitConstraint extends Constraint {
	private int iClassLimit = 0;
	private Lecture iParent = null;
	private String iName = null;
	boolean iEnabled = true;
	
	private int iClassLimitDelta = 0;
	
	public ClassLimitConstraint(int classLimit, String name) {
		iClassLimit = classLimit;
		iName = name;
	}
	public ClassLimitConstraint(Lecture parent, String name) {
		iParent = parent;
		iName = name;
	}
	
	public int getClassLimitDelta() { return iClassLimitDelta; }
	public void setClassLimitDelta(int classLimitDelta) { iClassLimitDelta = classLimitDelta; }
	
	public int classLimit() { 
		return (iParent==null?iClassLimit+iClassLimitDelta:iParent.minClassLimit()+iClassLimitDelta);
	}
	public Lecture getParentLecture() {
		return iParent;
	}
	public int currentClassLimit(Value value, Set conflicts) {
		int limit = 0;
		for (Enumeration e=variables().elements();e.hasMoreElements();) {
			Lecture lecture = (Lecture)e.nextElement();
			limit += lecture.classLimit((Placement)value, conflicts);
		}
		return limit;
	}
	
	public void computeConflicts(Value value, Set conflicts) {
		if (!iEnabled) return;
		int currentLimit = currentClassLimit(value, conflicts);
		int classLimit = classLimit();
		if (currentLimit<classLimit) {
			//System.out.println(getName()+"> "+currentLimit+"<"+classLimit+" ("+value+")");
			TreeSet adepts = new TreeSet(new ClassLimitComparator());
			computeAdepts(adepts, variables(), value, conflicts);
			addParentAdepts(adepts, iParent, value, conflicts);
			//System.out.println(" -- found "+adepts.size()+" adepts");
			for (Iterator i=adepts.iterator();i.hasNext();) {
				Placement adept = (Placement)i.next();
				//System.out.println("   -- selected "+adept);
				conflicts.add(adept);
				currentLimit = currentClassLimit(value, conflicts);
				//System.out.println("   -- new current limit "+currentLimit);
				if (currentLimit>=classLimit) break;
			}
			//System.out.println(" -- done (currentLimit="+currentLimit+", classLimit="+classLimit+")");
		}
		
		if (currentLimit<classLimit) conflicts.add(value);

		if (iParent!=null && iParent.getClassLimitConstraint()!=null)
			iParent.getClassLimitConstraint().computeConflicts(value, conflicts);
	}
	
	public void computeAdepts(Collection adepts, Vector variables, Value value, Set conflicts) {
		for (Enumeration e=variables.elements();e.hasMoreElements();) {
			Lecture lecture = (Lecture)e.nextElement();
			Placement placement = (Placement)lecture.getAssignment();
			if (placement!=null && !placement.equals(value) && !conflicts.contains(placement)) {
				adepts.add(placement);
			}
			if (lecture.hasAnyChildren()) {
				for (Enumeration f=lecture.getChildrenSubpartIds();f.hasMoreElements();) {
					Long subpartId = (Long)f.nextElement();
					computeAdepts(adepts, lecture.getChildren(subpartId), value, conflicts);
				}
			}
				
		}
	}
	
	public void addParentAdepts(Collection adepts, Lecture parent, Value value, Set conflicts) {
		if (parent==null) return;
		Placement placement = (Placement)parent.getAssignment();
		if (placement!=null && !placement.equals(value) && !conflicts.contains(placement)) {
			adepts.add(placement);
		}
		addParentAdepts(adepts, parent.getParent(), value, conflicts);
	}
	
	public boolean inConflict(Value value) {
		if (!iEnabled) return false;
		int currentLimit = currentClassLimit(value, null);
		int classLimit = classLimit();
		if (currentLimit<classLimit) return true;
		
		if (iParent!=null && iParent.getClassLimitConstraint()!=null)
			return iParent.getClassLimitConstraint().inConflict(value);
		
		return false;
	}
	
	public String getName() { return iName; }
	
	private static class ClassLimitComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			Placement p1 = (Placement)o1;
			Lecture l1 = (Lecture)p1.variable();
			Placement p2 = (Placement)o2;
			Lecture l2 = (Lecture)p2.variable();
			int cl1 = Math.min(l1.maxClassLimit(),(int)Math.ceil(p1.minRoomSize()/l1.roomToLimitRatio()));
			int cl2 = Math.min(l2.maxClassLimit(),(int)Math.ceil(p2.minRoomSize()/l2.roomToLimitRatio()));
			int cmp = -Double.compare(l1.maxAchievableClassLimit()-cl1,l2.maxAchievableClassLimit()-cl2);
			if (cmp!=0) return cmp;
			return l1.getClassId().compareTo(l2.getClassId());
		}
	}
	
    public void setEnabled(boolean enabled) { 
    	iEnabled = enabled;
    }
    public boolean isEnabled() { return iEnabled; }
    
    public String toString() {
    	return "Class-limit "+getName(); 
    }
	
}
