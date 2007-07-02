package net.sf.cpsolver.coursett.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.coursett.Constants;
import net.sf.cpsolver.coursett.constraint.ClassLimitConstraint;
import net.sf.cpsolver.coursett.constraint.DepartmentSpreadConstraint;
import net.sf.cpsolver.coursett.constraint.GroupConstraint;
import net.sf.cpsolver.coursett.constraint.InstructorConstraint;
import net.sf.cpsolver.coursett.constraint.JenrlConstraint;
import net.sf.cpsolver.coursett.constraint.RoomConstraint;
import net.sf.cpsolver.coursett.constraint.SpreadConstraint;
import net.sf.cpsolver.coursett.constraint.WeakeningConstraint;
import net.sf.cpsolver.ifs.constant.ConstantVariable;
import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.util.FastVector;


/**
 * Lecture (variable).
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

public class Lecture extends Variable implements ConstantVariable {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(Lecture.class);
    private Long iClassId;
    private Long iSolverGroupId;
    private Long iSchedulingSubpartId;
    private String iName;
    private Long iDept;
    private Long iScheduler;
    private Vector iTimeLocations;
    private Vector iRoomLocations;
    private String iNote = null;
    
    private int iMinClassLimit;
    private int iMaxClassLimit;
    private double iRoomToLimitRatio;
    private int iNrRooms;
    private int iOrd;
    
    private Set iStudents = new HashSet();
    private DepartmentSpreadConstraint iDeptSpreadConstraint = null;
    private Set iSpreadConstraints = new HashSet();
    private Set iWeakeningConstraints = new HashSet();
    private Vector iInstructorConstraints = new Vector();
    private ClassLimitConstraint iClassLimitConstraint = null;
    
    private Lecture iParent = null;
    private Hashtable iChildren = null;
    private Vector  iSameSubpartLectures = null;
    private Configuration iParentConfiguration = null;

    private Hashtable iSameStudents = new Hashtable(10);
    private Set iActiveJenrls = new HashSet();
    private Vector iJenrlConstraints = new FastVector();
    private Hashtable iJenrlConstraintsHash = new Hashtable();
    private Hashtable iCommitedConflicts = new Hashtable();
    private Set iGroupConstraints = new HashSet();
    private Set iHardGroupSoftConstraints = new HashSet();
    private Set iCanShareRoomGroupConstraints = new HashSet();
    
    public boolean iCommitted = false;
    
    public static boolean sSaveMemory = false;
    public static boolean sAllowBreakHard = false;
    
    private Integer iCacheMinRoomSize = null;
    private Integer iCacheMaxRoomSize = null;
    private Integer iCacheMaxAchievableClassLimit = null;
    
    /** Constructor
     * @param id unique identification
     * @param name class name
     * @param timeLocations set of time locations
     * @param roomLocations set of room location
     * @param initialPlacement initial placement
     */
    public Lecture(Long id, Long solverGroupId, Long schedulingSubpartId, String name, Vector timeLocations, Vector roomLocations, int nrRooms, Placement initialPlacement, int minClassLimit, int maxClassLimit, double room2limitRatio) {
        super(initialPlacement);
        iClassId = id;
        iSchedulingSubpartId = schedulingSubpartId;
        iTimeLocations = timeLocations;
        iRoomLocations = roomLocations;
        iName = name;
        iMinClassLimit = minClassLimit;
        iMaxClassLimit = maxClassLimit;
        iRoomToLimitRatio = room2limitRatio;
        iNrRooms = nrRooms;
        iSolverGroupId = solverGroupId;
    }
    
    public Lecture(Long id, Long solverGroupId, String name) {
    	super(null);
    	iClassId = id;
    	iSolverGroupId = solverGroupId;
    	iName = name;
    }
    
    public Long getSolverGroupId() {
    	return iSolverGroupId;
    }
    
    /** Add active jenrl constraint (active mean that there is at least one student between its classes) */
    public void addActiveJenrl(JenrlConstraint constr) { iActiveJenrls.add(constr); }
    /** Active jenrl constraints (active mean that there is at least one student between its classes) */
    public Set activeJenrls() { return iActiveJenrls; }
    /** Remove active jenrl constraint (active mean that there is at least one student between its classes) */
    public void removeActiveJenrl(JenrlConstraint constr) { iActiveJenrls.remove(constr); }
    
    /** Class id */
    public Long getClassId() { return iClassId; }
    public Long getSchedulingSubpartId() { return iSchedulingSubpartId; }
    /** Class name */
    public String getName() { return iName; }
    /** Class id */
    public long getId() { return iClassId.longValue(); }
    /** Instructor name */
    public Vector getInstructorNames() { 
    	Vector ret = new Vector();
    	for (Enumeration e=iInstructorConstraints.elements();e.hasMoreElements();) {
    		InstructorConstraint ic = (InstructorConstraint)e.nextElement();
    		ret.addElement(ic.getName());
    	}
    	return ret;
    }
    public String getInstructorName() { 
    	StringBuffer sb = new StringBuffer();
    	for (Enumeration e=iInstructorConstraints.elements();e.hasMoreElements();) {
    		InstructorConstraint ic = (InstructorConstraint)e.nextElement();
    		sb.append(ic.getName());
    		if (e.hasMoreElements())
    			sb.append(", ");
    	}
    	return sb.toString();
    }
    
    /** List of enrolled students */
    public Set students() { return iStudents; }
    
    public double nrWeightedStudents() {
    	double w = 0.0;
    	for (Iterator i=iStudents.iterator();i.hasNext();) {
    		Student s = (Student)i.next();
    		w+=s.getOfferingWeight(getConfiguration());
    	}
    	return w;
    }
    
    /** Add an enrolled student */
    public void addStudent(Student student) {
    	if (iStudents.contains(student)) return;
    	if (getAssignment()!=null && getModel()!=null)
    		((TimetableModel)getModel()).getCommittedStudentConflictsCounter().inc(student.countConflictPlacements((Placement)getAssignment()));
        iStudents.add(student);
        iSameStudents.clear();
        iCommitedConflicts.clear();
    }
    public void removeStudent(Student student) {
    	if (getAssignment()!=null && getModel()!=null)
    		((TimetableModel)getModel()).getCommittedStudentConflictsCounter().dec(student.countConflictPlacements((Placement)getAssignment()));
        iStudents.remove(student);
        iSameStudents.clear();
        iCommitedConflicts.clear();
    }
    /** Returns true if the given student is enrolled */
    public boolean hasStudent(Student student) { return iStudents.contains(student);}
    /** Set of lectures of the same class (only section is different) */
    public void setSameSubpartLectures(Vector sameSubpartLectures) {  iSameSubpartLectures = sameSubpartLectures; }
    /** Set of lectures of the same class (only section is different) */
    public Vector sameSubpartLectures() { return iSameSubpartLectures; }
    /** List of students enrolled in this class as well as in the given class */
    public Set sameStudents(Lecture lecture) {
        if (iSameStudents.containsKey(lecture)) return (Set)iSameStudents.get(lecture);
        Set ret = new HashSet(students());
        ret.retainAll(lecture.students());
        iSameStudents.put(lecture, ret);
        return ret;
    }
    /** List of students of this class in conflict with the given assignment */
    public Set conflictStudents(Value value) {
        if (value==null) return new HashSet();
        if (value.equals(getAssignment())) return conflictStudents();
        Set ret = new HashSet();
        for (Enumeration i1=jenrlConstraints(); i1.hasMoreElements();) { //constraints()
        	JenrlConstraint jenrl = (JenrlConstraint)i1.nextElement();// constraint;
            if (jenrl.jenrl(this, value)>0)
            	ret.addAll(sameStudents((Lecture)jenrl.another(this)));
        }
        return ret;
    }

    /** List of students of this class which are in conflict with any other assignment */
    public Set conflictStudents() {
        Set ret = new HashSet();
        if (getAssignment()==null) return ret;
        for (Iterator i1=activeJenrls().iterator(); i1.hasNext();) {
            JenrlConstraint jenrl = (JenrlConstraint) i1.next();
            ret.addAll(sameStudents((Lecture)jenrl.another(this)));
        }
    	Placement placement = (Placement)getAssignment();
    	for (Iterator i1=students().iterator();i1.hasNext();) {
    		Student student = (Student)i1.next();
    		if (student.countConflictPlacements(placement)>0)
    			ret.add(student);
    	}
        return ret;
    }
    
    /** Lectures different from this one, where it is student conflict of the given student between this and the lecture */
    public Vector conflictLectures(Student student) {
        Vector ret = new FastVector();
        if (getAssignment()==null) return ret;
        for (Iterator it=activeJenrls().iterator();it.hasNext();) {
            JenrlConstraint jenrl = (JenrlConstraint) it.next();
            Lecture lect = (Lecture)jenrl.another(this);
            if (lect.students().contains(student)) ret.addElement(lect);
        }
        return ret;
    }
    
    /** True if this lecture is in a student conflict with the given student */
    public int isInConflict(Student student) {
        if (getAssignment()==null) return 0;
        int ret = 0;
        for (Iterator it=activeJenrls().iterator();it.hasNext();) {
            JenrlConstraint jenrl = (JenrlConstraint) it.next();
            Lecture lect = (Lecture)jenrl.another(this);
            if (lect.students().contains(student)) ret++;
        }
        return ret;
    }
    
    private void computeValues(Vector values, boolean allowBreakHard, TimeLocation timeLocation, Vector roomLocations, int idx) {
    	if (roomLocations.size()==iNrRooms) {
    		Placement p = new Placement(this,timeLocation, roomLocations);
    		p.setVariable(this);
    		if (sSaveMemory && !isValid(p)) return;
    		if (getInitialAssignment()!=null && p.equals(getInitialAssignment())) setInitialAssignment(p);
    		if (getAssignment()!=null && getAssignment().equals(p)) iValue=getAssignment();
    		if (getBestAssignment()!=null && getBestAssignment().equals(p)) setBestAssignment(p);
    		values.addElement(p);
    		return;
    	}
    	for (int i=idx;i<iRoomLocations.size();i++) {
    		RoomLocation roomLocation = (RoomLocation)iRoomLocations.elementAt(i);
    		if (!allowBreakHard && Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(roomLocation.getPreference()))) continue;
    		
    		if (roomLocation.getRoomConstraint()!=null && !roomLocation.getRoomConstraint().isAvailable(this,timeLocation, getScheduler())) continue;
    		roomLocations.addElement(roomLocation);
    		computeValues(values, allowBreakHard, timeLocation, roomLocations, i+1);
    		roomLocations.removeElementAt(roomLocations.size()-1);
    	}
    }
    
    /** Domain -- all combinations of room and time locations */
    public Vector computeValues(boolean allowBreakHard) {
        Vector values = new FastVector(iRoomLocations.size()*iTimeLocations.size());
        for (Enumeration i1=iTimeLocations.elements();i1.hasMoreElements();) {
            TimeLocation timeLocation = (TimeLocation)i1.nextElement();
            if (!allowBreakHard && Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(timeLocation.getPreference()))) continue;
            if (timeLocation.getPreference()>500) continue;
            boolean notAvailable=false;
           	for (Enumeration e=getInstructorConstraints().elements();e.hasMoreElements();) {
           		InstructorConstraint ic = (InstructorConstraint)e.nextElement();
           		if (!ic.isAvailable(this, timeLocation)) {
           			notAvailable = true; break;
           		}
           	}
           	if (notAvailable) continue;
            if (iNrRooms==0) {
        		Placement p = new Placement(this,timeLocation,(RoomLocation)null);
               	for (Enumeration e=getInstructorConstraints().elements();e.hasMoreElements();) {
               		InstructorConstraint ic = (InstructorConstraint)e.nextElement();
               		if (!ic.isAvailable(this, p)) {
               			notAvailable = true; break;
               		}
               	}
               	if (notAvailable) continue;
        		p.setVariable(this);
        		if (sSaveMemory && !isValid(p)) continue;
        		if (getInitialAssignment()!=null && p.equals(getInitialAssignment())) setInitialAssignment(p);
        		if (getAssignment()!=null && getAssignment().equals(p)) iValue=getAssignment();
        		if (getBestAssignment()!=null && getBestAssignment().equals(p)) setBestAssignment(p);
        		values.addElement(p);
            } else if (iNrRooms==1) {
            	for (Enumeration i2=iRoomLocations.elements();i2.hasMoreElements();) {
            		RoomLocation roomLocation = (RoomLocation)i2.nextElement();
            		if (!allowBreakHard && Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(roomLocation.getPreference()))) continue;
            		if (roomLocation.getPreference()>500) continue;
            		if (roomLocation.getRoomConstraint()!=null && !roomLocation.getRoomConstraint().isAvailable(this,timeLocation, getScheduler())) continue;
            		Placement p = new Placement(this,timeLocation, roomLocation);
            		p.setVariable(this);
            		if (sSaveMemory && !isValid(p)) continue;
            		if (getInitialAssignment()!=null && p.equals(getInitialAssignment())) setInitialAssignment(p);
            		if (getAssignment()!=null && getAssignment().equals(p)) iValue=getAssignment();
            		if (getBestAssignment()!=null && getBestAssignment().equals(p)) setBestAssignment(p);
            		values.addElement(p);
            	}
            } else {
            	computeValues(values, allowBreakHard, timeLocation, new Vector(iNrRooms), 0);
            }
        }
        return values;
    }
    
    /** All values */
    public Vector values() {
    	if (super.values()==null) {
            if (getInitialAssignment()!=null && iTimeLocations.size()==1 && iRoomLocations.size()==getNrRooms()) {
            	Vector values = new Vector(1);
            	values.addElement(getInitialAssignment());
            	setValues(values);
            } else {
            	if (isCommitted() || !sSaveMemory)
            		setValues(computeValues(sAllowBreakHard));
            }
    	}
    	if (isCommitted())
    		return super.values();
        if (sSaveMemory) {
            return computeValues(sAllowBreakHard);            
        } else
            return super.values();
    }
    
    public boolean equals(Object o) {
        try {
            return getClassId().equals(((Lecture)o).getClassId());
        } catch (Exception e) {
            return false;
        }
    }
    
    /** Best time preference of this lecture */
    private Double iBestTimePreferenceCache = null;
    public double getBestTimePreference() {
    	if (iBestTimePreferenceCache==null) {
    		double ret = Double.MAX_VALUE;
    		for (Enumeration e=iTimeLocations.elements();e.hasMoreElements();) {
    			TimeLocation time = (TimeLocation)e.nextElement();
    			ret = Math.min(ret,time.getNormalizedPreference());
    		}
    		iBestTimePreferenceCache = new Double(ret);
    	}
    	return iBestTimePreferenceCache.doubleValue();
    }
    
    /** Best room preference of this lecture */
    public int getBestRoomPreference() { 
    	int ret = Integer.MAX_VALUE;
    	for (Enumeration e=iRoomLocations.elements();e.hasMoreElements();) {
    		RoomLocation room = (RoomLocation)e.nextElement();
    		ret = Math.min(ret,room.getPreference());
    	}
    	return ret;
    }
    
    /** Number of student conflicts caused by the given assignment of this lecture */
    public int countStudentConflicts(Value value) {
        int studentConflictsSum = 0;
        for (Enumeration i=jenrlConstraints();i.hasMoreElements();) {
            JenrlConstraint jenrl = (JenrlConstraint)i.nextElement();
            studentConflictsSum += jenrl.jenrl(this, value);
        }
        return studentConflictsSum;
    }

    public int countStudentConflictsOfTheSameProblem(Value value) {
        int studentConflictsSum = 0;
        for (Enumeration i=jenrlConstraints();i.hasMoreElements();) {
            JenrlConstraint jenrl = (JenrlConstraint)i.nextElement();
            if (!jenrl.isOfTheSameProblem()) continue;
            studentConflictsSum += jenrl.jenrl(this, value);
        }
        return studentConflictsSum;
    }

    public int countHardStudentConflicts(Value value) {
        int studentConflictsSum = 0;
        if (!isSingleSection()) return 0;
        for (Enumeration i=jenrlConstraints();i.hasMoreElements();) {
            JenrlConstraint jenrl = (JenrlConstraint)i.nextElement();
            if (!jenrl.areStudentConflictsHard()) continue;
            studentConflictsSum += jenrl.jenrl(this, value);
        }
        return studentConflictsSum;
    }
    
    public int countHardStudentConflictsOfTheSameProblem(Value value) {
        int studentConflictsSum = 0;
        for (Enumeration i=jenrlConstraints();i.hasMoreElements();) {
            JenrlConstraint jenrl = (JenrlConstraint)i.nextElement();
            if (!jenrl.isOfTheSameProblem()) continue;
            if (!jenrl.areStudentConflictsHard()) continue;
            studentConflictsSum += jenrl.jenrl(this, value);
        }
        return studentConflictsSum;
    }

    public int countDistanceStudentConflicts(Value value) {
        int studentConflictsSum = 0;
        for (Enumeration i=jenrlConstraints();i.hasMoreElements();) {
            JenrlConstraint jenrl = (JenrlConstraint)i.nextElement();
            if (!jenrl.areStudentConflictsDistance(value)) continue;
            studentConflictsSum += jenrl.jenrl(this, value);
        }
        return studentConflictsSum;
    }

    public int countDistanceStudentConflictsOfTheSameProblem(Value value) {
        int studentConflictsSum = 0;
        for (Enumeration i=jenrlConstraints();i.hasMoreElements();) {
            JenrlConstraint jenrl = (JenrlConstraint)i.nextElement();
            if (!jenrl.isOfTheSameProblem()) continue;
            if (!jenrl.areStudentConflictsDistance(value)) continue;
            studentConflictsSum += jenrl.jenrl(this, value);
        }
        return studentConflictsSum;
    }

    /** Number of student conflicts caused by the initial assignment of this lecture */
    public int countInitialStudentConflicts() {
        Value value = getInitialAssignment();
        if (value==null) return 0;
        int studentConflictsSum = 0;
        for (Enumeration i=jenrlConstraints();i.hasMoreElements();) {
            JenrlConstraint jenrl = (JenrlConstraint)i.nextElement();
            Lecture another = (Lecture)jenrl.another(this);
            if (another.getInitialAssignment()!=null)
                if (JenrlConstraint.isInConflict((Placement)value,(Placement)another.getInitialAssignment()))
                    studentConflictsSum += jenrl.getJenrl();
        }
        return studentConflictsSum;
    }

    /** Table of student conflicts caused by the initial assignment of this lecture in format (another lecture, number)*/
    public Hashtable getInitialStudentConflicts() {
        Value value = getInitialAssignment();
        if (value==null) return null;
        Hashtable ret = new Hashtable();
        for (Enumeration i=jenrlConstraints();i.hasMoreElements();) {
            JenrlConstraint jenrl = (JenrlConstraint)i.nextElement();
            Lecture another = (Lecture)jenrl.another(this);
            if (another.getInitialAssignment()!=null)
                if (JenrlConstraint.isInConflict((Placement)value,(Placement)another.getInitialAssignment()))
                    ret.put(another,new Long(jenrl.getJenrl()));
        }
        return ret;
    }

    /** List of student conflicts caused by the initial assignment of this lecture */
    public Set initialStudentConflicts() {
        Value value = getInitialAssignment();
        if (value==null) return null;
        HashSet ret = new HashSet();
        for (Enumeration i=jenrlConstraints();i.hasMoreElements();) {
            JenrlConstraint jenrl = (JenrlConstraint)i.nextElement();
            Lecture another = (Lecture)jenrl.another(this);
            if (another.getInitialAssignment()!=null)
                if (JenrlConstraint.isInConflict((Placement)value,(Placement)another.getInitialAssignment()))
                    ret.addAll(sameStudents(another));
        }
        return ret;
    }
    
    public void addContstraint(Constraint constraint) {
    	super.addContstraint(constraint);
    	
    	if (constraint instanceof WeakeningConstraint)
        	iWeakeningConstraints.add(constraint);
    	
        if (constraint instanceof JenrlConstraint) {
        	JenrlConstraint jenrl = (JenrlConstraint)constraint;
        	Lecture another = (Lecture)jenrl.another(this);
        	if (another!=null) {
        		iJenrlConstraints.add(jenrl);
        		another.iJenrlConstraints.add(jenrl);
        		iJenrlConstraintsHash.put(another,constraint);
        		another.iJenrlConstraintsHash.put(this,constraint);
        	}
        } else if (constraint instanceof DepartmentSpreadConstraint)
            iDeptSpreadConstraint = (DepartmentSpreadConstraint)constraint;
        else if (constraint instanceof SpreadConstraint)
            iSpreadConstraints.add(constraint);
        else if (constraint instanceof InstructorConstraint) {
        	InstructorConstraint ic = (InstructorConstraint)constraint;
        	if (ic.getResourceId()!=null && ic.getResourceId().intValue()>0)
        		iInstructorConstraints.add(ic);
        } else if (constraint instanceof ClassLimitConstraint)
        	iClassLimitConstraint = (ClassLimitConstraint)constraint;
        else if (constraint instanceof GroupConstraint) {
        	GroupConstraint gc = (GroupConstraint)constraint;
        	if (GroupConstraint.canShareRooms(gc.getType())) {
        		iCanShareRoomGroupConstraints.add(constraint);
        	} else {
        		iGroupConstraints.add(constraint);
        		if (Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(gc.getPreference())) ||
                        Constants.sPreferenceRequired.equals(Constants.preferenceLevel2preference(gc.getPreference())))
        				iHardGroupSoftConstraints.add(constraint);
        	}
        }
    }
    public void removeContstraint(Constraint constraint) {
        super.removeContstraint(constraint);
        
    	if (constraint instanceof WeakeningConstraint)
        	iWeakeningConstraints.remove(constraint);
        
        if (constraint instanceof JenrlConstraint) {
        	JenrlConstraint jenrl = (JenrlConstraint)constraint;
        	Lecture another = (Lecture)jenrl.another(this);
        	if (another!=null) {
        		iJenrlConstraints.remove(jenrl);
        		another.iJenrlConstraints.remove(jenrl);
        		iJenrlConstraintsHash.remove(another);
        		another.iJenrlConstraintsHash.remove(this);
        	}
        } else if (constraint instanceof GroupConstraint) {
       		iCanShareRoomGroupConstraints.remove(constraint);
        	iHardGroupSoftConstraints.remove(constraint);
        	iGroupConstraints.remove(constraint);
        } else if (constraint instanceof DepartmentSpreadConstraint)
        	iDeptSpreadConstraint = null;
        else if (constraint instanceof SpreadConstraint)
            iSpreadConstraints.remove(constraint);
        else if (constraint instanceof InstructorConstraint)
            iInstructorConstraints.remove(constraint);
        else if (constraint instanceof ClassLimitConstraint)
        	iClassLimitConstraint = null;
    }
    
    /** All JENRL constraints of this lecture */
    public JenrlConstraint jenrlConstraint(Lecture another) {
    	/*
    	for (Enumeration e=iJenrlConstraints.elements();e.hasMoreElements();) {
    		JenrlConstraint jenrl = (JenrlConstraint)e.nextElement();
    		if (jenrl.another(this).equals(another)) return jenrl;
    	}
    	return null;
    	*/
        return (JenrlConstraint)iJenrlConstraintsHash.get(another);
    }
    public Enumeration jenrlConstraints() {
        return iJenrlConstraints.elements();
    }
    
    public int minClassLimit() {
        return iMinClassLimit;
    }
    
    public int maxClassLimit() {
        return iMaxClassLimit;
    }
    
    public int maxAchievableClassLimit() {
    	//if (iCacheMaxAchievableClassLimit!=null) return iCacheMaxAchievableClassLimit.intValue();
    	
    	int maxAchievableClassLimit = Math.min(maxClassLimit(),(int)Math.floor(maxRoomSize()/roomToLimitRatio()));
    	
    	if (hasAnyChildren()) {
    		
    		for (Enumeration e1=getChildrenSubpartIds();e1.hasMoreElements();) {
    			Long subpartId = (Long) e1.nextElement();
    			int maxAchievableChildrenLimit = 0;
    			
    			for (Enumeration e2=getChildren(subpartId).elements();e2.hasMoreElements();) {
    				Lecture child = (Lecture)e2.nextElement();
    				maxAchievableChildrenLimit += child.maxAchievableClassLimit();
    			}
    		
    			maxAchievableClassLimit = Math.min(maxAchievableClassLimit, maxAchievableChildrenLimit);
    		}
    	}
    	
    	maxAchievableClassLimit = Math.max(minClassLimit(),maxAchievableClassLimit);
    	iCacheMaxAchievableClassLimit = new Integer(maxAchievableClassLimit);
    	return maxAchievableClassLimit;
    }
    
    public int classLimit() {
    	if (minClassLimit()==maxClassLimit()) return minClassLimit();
    	return classLimit(null, null);
    }

    public int classLimit(Placement assignment, Set conflicts) {
    	Placement a = (Placement)getAssignment();
    	if (assignment!=null && assignment.variable().equals(this))
    		a = assignment;
    	if (conflicts!=null && a!=null && conflicts.contains(a))
    		a = null;
    	int classLimit = (a==null?maxAchievableClassLimit():Math.min(maxClassLimit(),(int)Math.floor(a.minRoomSize()/roomToLimitRatio())));
    	
    	if (!hasAnyChildren()) return classLimit;
    	
		for (Enumeration e1=getChildrenSubpartIds();e1.hasMoreElements();) {
			Long subpartId = (Long) e1.nextElement();
			int childrenClassLimit = 0;
			
			for (Enumeration e2=getChildren(subpartId).elements();e2.hasMoreElements();) {
				Lecture child = (Lecture)e2.nextElement();
				childrenClassLimit += child.classLimit(assignment, conflicts);
			}
		
			classLimit = Math.min(classLimit, childrenClassLimit);
		}
    	
    	return Math.max(minClassLimit(),classLimit);
    }
    
    public double roomToLimitRatio() {
    	return iRoomToLimitRatio;
    }
    
    public int minRoomUse() {
    	return (int)Math.ceil(iMinClassLimit*iRoomToLimitRatio);
    }

    public int maxRoomUse() {
    	return (int)Math.ceil(iMaxClassLimit*iRoomToLimitRatio);
    }
    
    public String toString() {
        return getName();
    }
    
    public String getValuesString() {
        StringBuffer sb = new StringBuffer();
        for (Enumeration e=values().elements();e.hasMoreElements();) {
            Placement p = (Placement)e.nextElement();
            sb.append(p.getName()).append(e.hasMoreElements()?", ":"");
        }
        return sb.toString();
    }

    /** Controlling Course Offering Department */
    public Long getDepartment() { return iDept;}
    /** Controlling Course Offering Department */
    public void setDepartment(Long dept) { iDept=dept; }
    /** Scheduler (Managing Department) */
    public Long getScheduler() { return iScheduler;}
    /** Scheduler (Managing Department) */
    public void setScheduler(Long scheduler) { iScheduler=scheduler; }
    /** Departmental spreading constraint */
    public DepartmentSpreadConstraint getDeptSpreadConstraint() { return iDeptSpreadConstraint; }
    /** Instructor constraint */
    public Vector getInstructorConstraints() { return iInstructorConstraints; }
    public ClassLimitConstraint getClassLimitConstraint() { return iClassLimitConstraint; }
    public Set getSpreadConstraints() { return iSpreadConstraints; }
    public Set getWeakeningConstraints() { return iWeakeningConstraints; }

    /** All room locations */
    public Vector roomLocations() { return iRoomLocations; }
    /** All time locations */
    public Vector timeLocations() { return iTimeLocations; }
    public int nrTimeLocations() {
    	int ret = 0;
    	for (Enumeration e=iTimeLocations.elements();e.hasMoreElements();) {
    		TimeLocation time = (TimeLocation)e.nextElement();
    		if (!Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(time.getPreference())))
    			ret++;
    	}
    	return ret;
    }
    public int nrRoomLocations() {
    	int ret = 0;
    	for (Enumeration e=iRoomLocations.elements();e.hasMoreElements();) {
    		RoomLocation room = (RoomLocation)e.nextElement();
    		if (!Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(room.getPreference())))
    			ret++;
    	}
    	return ret;
    }
    public int nrValues() {
    	int ret = 0;
    	for (Enumeration e=values().elements();e.hasMoreElements();) {
    		Placement placement = (Placement)e.nextElement();
    		if (!Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(placement.getRoomPreference())) &&
    			!Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(placement.getTimeLocation().getPreference())))
    			ret++;
    	}
    	return ret;
    }
    public int nrValues(TimeLocation time) {
    	int ret = 0;
    	for (Enumeration e=iRoomLocations.elements();e.hasMoreElements();) {
    		RoomLocation room = (RoomLocation)e.nextElement();
    		if (!Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(room.getPreference())) && (room.getRoomConstraint()==null || room.getRoomConstraint().isAvailable(this, time, getScheduler())))
    			ret++;
    	}
    	return ret;
    }
    public int nrValues(RoomLocation room) {
    	int ret = 0;
    	for (Enumeration e=iTimeLocations.elements();e.hasMoreElements();) {
    		TimeLocation time = (TimeLocation)e.nextElement();
    		if (!Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(time.getPreference())) && (room.getRoomConstraint()==null || room.getRoomConstraint().isAvailable(this, time, getScheduler())))
    			ret++;
    	}
    	return ret;
    }
    public int nrValues(Vector rooms) {
    	int ret = 0;
    	for (Enumeration e=iTimeLocations.elements();e.hasMoreElements();) {
    		TimeLocation time = (TimeLocation)e.nextElement();
    		boolean available = true;
    		for (Enumeration f=rooms.elements();available && f.hasMoreElements();) {
    			RoomLocation room = (RoomLocation)f.nextElement();
    			if (Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(time.getPreference())) || (room.getRoomConstraint()!=null && !room.getRoomConstraint().isAvailable(this, time, getScheduler())))
    				available = false;
    		}
    		if (available) ret++;
    	}
    	return ret;
    }
    public boolean allowBreakHard() { return sAllowBreakHard; }
    public int getNrRooms() { return iNrRooms; }
    public Lecture getParent() { return iParent; }
    public void setParent(Lecture parent) { iParent = parent; iParent.addChild(this); }
    public boolean hasParent() { return (iParent!=null);}
    public boolean hasChildren(Long subpartId) { return (iChildren!=null && iChildren.get(subpartId)!=null && !((Vector)iChildren.get(subpartId)).isEmpty()); }
    public boolean hasAnyChildren() { return (iChildren!=null && !iChildren.isEmpty()); }
    public Vector getChildren(Long subpartId) { return (Vector)iChildren.get(subpartId); }
    public Enumeration getChildrenSubpartIds() { return (iChildren==null?null:iChildren.keys()); }
    private void addChild(Lecture child) {
    	if (iChildren==null) iChildren = new Hashtable();
    	Vector childrenThisSubpart = (Vector)iChildren.get(child.getSchedulingSubpartId());
    	if (childrenThisSubpart==null) {
    		childrenThisSubpart = new FastVector();
    		iChildren.put(child.getSchedulingSubpartId(),childrenThisSubpart);
    	}
    	childrenThisSubpart.addElement(child);
    }
    public boolean isSingleSection() {
    	if (iParent==null) 
    		return (iSameSubpartLectures==null || iSameSubpartLectures.size()<=1);
    	return (iParent.getChildren(getSchedulingSubpartId()).size()<=1);
    }
    public boolean areStudentConflictsHard(Lecture lecture) {
    	return isSingleSection() && lecture.isSingleSection();
    }
    public Vector sameStudentsLectures() {
    	//return (hasParent()?getParent().getChildren():sameSubpartLectures());
    	return (hasParent()?getParent().getChildren(getSchedulingSubpartId()):sameSubpartLectures());
    }
    public Lecture getChild(Student student, Long subpartId) {
    	if (!hasAnyChildren()) return null;
        Vector children = getChildren(subpartId);
        if (children==null) return null;
    	for (Enumeration e=children.elements();e.hasMoreElements();) {
    		Lecture child = (Lecture)e.nextElement();
    		if (child.students().contains(student)) 
    			return child;
    	}
    	return null;
    }
    
    public int getCommitedConflicts(Placement placement) {
    	Integer ret = (Integer)iCommitedConflicts.get(placement);
    	if (ret==null) {
    		ret = new Integer(placement.getCommitedConflicts());
    		iCommitedConflicts.put(placement,ret);
    	}
    	return ret.intValue();
    }
    
    public void assign(long iteration, Value value) {
    	super.assign(iteration, value);
    	if (value!=null && getModel()!=null) {
    		((TimetableModel)getModel()).getCommittedStudentConflictsCounter().inc(getCommitedConflicts((Placement)value));
    	}
    }
    
    public void unassign(long iteration) {
    	if (getAssignment()!=null && isCommitted())
    		throw new RuntimeException("Unable to unassign committed variable ("+getName()+" "+getAssignment().getName()+")");
    	if (getAssignment()!=null && getModel()!=null) {
    		((TimetableModel)getModel()).getCommittedStudentConflictsCounter().dec(getCommitedConflicts((Placement)getAssignment()));
    	}
    	super.unassign(iteration);
    }
    
    public Set hardGroupSoftConstraints() {
    	return iHardGroupSoftConstraints;
    }
    public Set groupConstraints() {
    	return iGroupConstraints;
    }
    
    public int minRoomSize() {
    	if (iCacheMinRoomSize!=null) return iCacheMinRoomSize.intValue();
    	if (getNrRooms()<=1) {
    		int min = Integer.MAX_VALUE;
    		for (Enumeration e=roomLocations().elements();e.hasMoreElements();) {
    			RoomLocation r = (RoomLocation)e.nextElement();
    			if (Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(r.getPreference()))) continue;
    			min = Math.min(min, r.getRoomSize());
    		}
    		iCacheMinRoomSize = new Integer(min);
    		return min;
    	} else {
    		Vector rl = new Vector(roomLocations());
    		Collections.sort(rl);
    		int min = 0; int i = 0;
    		for (Enumeration e=rl.elements();e.hasMoreElements() && i<getNrRooms();) {
    			RoomLocation r = (RoomLocation)e.nextElement();
    			if (Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(r.getPreference()))) continue;
    			min += r.getRoomSize(); i++;
    		}
    		iCacheMinRoomSize = new Integer(min);
    		return min;
    	}
    }
    
    public int maxRoomSize() {
    	if (iCacheMaxRoomSize!=null) return iCacheMaxRoomSize.intValue();
    	if (getNrRooms()<=1) {
    		int max = Integer.MIN_VALUE;
    		for (Enumeration e=roomLocations().elements();e.hasMoreElements();) {
    			RoomLocation r = (RoomLocation)e.nextElement();
    			if (Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(r.getPreference()))) continue;
    			max = Math.max(max, r.getRoomSize());
    		}
    		iCacheMaxRoomSize = new Integer(max);
    		return max;
    	} else {
    		Vector rl = new Vector(roomLocations());
    		Collections.sort(rl, Collections.reverseOrder());
    		int max = 0; int i = 0;
    		for (Enumeration e=rl.elements();e.hasMoreElements() && i<getNrRooms();) {
    			RoomLocation r = (RoomLocation)e.nextElement();
    			if (Constants.sPreferenceProhibited.equals(Constants.preferenceLevel2preference(r.getPreference()))) continue;
    			max += r.getRoomSize(); i++;
    		}
    		iCacheMaxRoomSize = new Integer(max);
    		return max;
    	}
    }    
    
    public long getDiscouragedRoomSize() {
        return Math.round(1.25 * minRoomSize());
    }
    public long getStronglyDiscouragedRoomSize() {
    	return Math.round(1.5 * minRoomSize());
    }
    
    public boolean canShareRoom() {
    	return (!iCanShareRoomGroupConstraints.isEmpty());
    }
    
    public boolean canShareRoom(Lecture other) {
        if (other.equals(this)) return true;
    	for (Iterator i=iCanShareRoomGroupConstraints.iterator();i.hasNext();) {
    		GroupConstraint gc = (GroupConstraint)i.next();
    		if (gc.variables().contains(other)) return true;
    	}
    	return false;
    }
    
    public Set canShareRoomConstraints() {
    	return iCanShareRoomGroupConstraints;
    }
    
    public boolean isSingleton() {
    	return values().size()==1;
    }
    
    public boolean isValid(Placement placement) {
    	TimetableModel model = (TimetableModel)getModel();
    	if (model==null) return true;
    	if (model.hasConstantVariables()) {
       		for (Iterator i=model.conflictValues(placement).iterator();i.hasNext();) {
       			Placement confPlacement = (Placement)i.next();
       			Lecture lecture = (Lecture)confPlacement.variable();
       			if (lecture.isCommitted()) return false;
       			if (confPlacement.equals(placement)) return false;
        	}
    	} else {
    		if (model.conflictValues(placement).contains(placement))
    			return false;
    	}
    	return true;
    }
    
    public String getNotValidReason(Placement placement) {
    	TimetableModel model = (TimetableModel)getModel();
    	if (model==null) return "no model for class "+getName();
   		Hashtable conflictConstraints = model.conflictConstraints(placement);
   		for (Iterator i=conflictConstraints.entrySet().iterator();i.hasNext();) {
   			Map.Entry entry = (Map.Entry)i.next();
   			Constraint constraint = (Constraint)entry.getKey();
   			Collection conflicts = (Collection)entry.getValue();
   			String cname = constraint.getName();
			if (constraint instanceof RoomConstraint) {
				cname = "Room "+constraint.getName();
			} else if (constraint instanceof InstructorConstraint) {
				cname = "Instructor "+constraint.getName();
			} else if (constraint instanceof GroupConstraint) {
				cname = "Distribution "+constraint.getName();
			} else if (constraint instanceof DepartmentSpreadConstraint) {
				cname = "Balancing of department "+constraint.getName();
			} else if (constraint instanceof SpreadConstraint) {
				cname = "Same subpart spread "+constraint.getName();
			} else if (constraint instanceof ClassLimitConstraint) {
				cname = "Class limit "+constraint.getName();
			}
       		for (Iterator j=conflicts.iterator();j.hasNext();) {
       			Placement confPlacement = (Placement)j.next();
       			Lecture lecture = (Lecture)confPlacement.variable();
       			if (lecture.isCommitted()) {
       				return placement.getLongName()+" conflicts with "+lecture.getName()+" "+confPlacement.getLongName()+" due to constraint "+cname;
       			}
       			if (confPlacement.equals(placement)) {
       				return placement.getLongName()+" is not valid due to constraint "+cname; 
       			}
        	}
    	}
    	return null;
    }
    
    public void purgeInvalidValues(boolean interactiveMode) {
    	if (isCommitted() || Lecture.sSaveMemory) return;
    	TimetableModel model = (TimetableModel)getModel();
    	if (model==null) return;
    	if (!model.hasConstantVariables()) return;
    	Vector newValues = new FastVector(values().size());
    	for (Enumeration e=values().elements();e.hasMoreElements();) {
    		Placement placement = (Placement)e.nextElement();
    		if (placement.isValid())
    			newValues.addElement(placement);
    	}
    	if (!interactiveMode && newValues.size()!=values().size()) {
    		for (Iterator i=timeLocations().iterator();i.hasNext();) {
    			TimeLocation timeLocation = (TimeLocation)i.next();
    			boolean hasPlacement = false;
    			for (Enumeration e=newValues.elements();e.hasMoreElements();) {
    				Placement placement = (Placement)e.nextElement();
    				if (timeLocation.equals(placement.getTimeLocation())) {
    					hasPlacement = true; break;
    				}
    			}
    			if (!hasPlacement) i.remove();
    		}
    		for (Iterator i=roomLocations().iterator();i.hasNext();) {
    			RoomLocation roomLocation = (RoomLocation)i.next();
    			boolean hasPlacement = false;
    			for (Enumeration e=newValues.elements();e.hasMoreElements();) {
    				Placement placement = (Placement)e.nextElement();
    				if (placement.isMultiRoom()) {
    					if (placement.getRoomLocations().contains(roomLocation)) {
    						hasPlacement = true; break;
    					}
    				} else {
    					if (roomLocation.equals(placement.getRoomLocation())) {
    						hasPlacement = true; break;
    					}
    				}
    			}
    			if (!hasPlacement) i.remove();
    		}
    	}
    	setValues(newValues);
    }
    
    public void setCommitted(boolean committed) {
    	iCommitted = committed;
    }
    public boolean isCommitted() { return iCommitted; }
    public boolean isConstant() { return iCommitted; }
    
    public int getSpreadPenalty() {
    	int spread = 0;
    	for (Iterator i=getSpreadConstraints().iterator();i.hasNext();) {
    		SpreadConstraint sc = (SpreadConstraint)i.next();
    		spread += sc.getPenalty();
    	}
    	return spread;
    }
    public int hashCode() {
    	return getClassId().hashCode();
    }

    public Configuration getConfiguration() {
    	Lecture lecture = this;
    	while (lecture.getParent()!=null) lecture = lecture.getParent();
    	return lecture.iParentConfiguration;
    }
    
    public void setConfiguration(Configuration configuration) {
    	Lecture lecture = this;
    	while (lecture.getParent()!=null) lecture = lecture.getParent();
    	lecture.iParentConfiguration = configuration;
    	configuration.addTopLecture(lecture);
    }
    
    private int[] iMinMaxRoomPreference = null;
    public int[] getMinMaxRoomPreference() {
    	if (iMinMaxRoomPreference==null) {
    		if (getNrRooms()<=0 || roomLocations().isEmpty()) {
    			iMinMaxRoomPreference = new int[] {0,0};
    		} else {
    			int minRoomPref = Integer.MAX_VALUE;
    			int maxRoomPref = Integer.MIN_VALUE;
    			for (Enumeration e=roomLocations().elements();e.hasMoreElements();) {
    				RoomLocation r = (RoomLocation)e.nextElement();
    				int pref = r.getPreference();
    				if (pref>Constants.sPreferenceLevelRequired/2)
    					minRoomPref = Math.min(minRoomPref,pref);
    				if (pref<Constants.sPreferenceLevelProhibited/2)
    					maxRoomPref = Math.max(maxRoomPref,pref);
    			}
    			iMinMaxRoomPreference = new int[] {minRoomPref, maxRoomPref};
    		}
    	}
    	return iMinMaxRoomPreference;
    }

    private double[] iMinMaxTimePreference = null;
    public double[] getMinMaxTimePreference() {
    	if (iMinMaxTimePreference==null) {
        	double minTimePref = Double.MAX_VALUE; 
        	double maxTimePref = -Double.MAX_VALUE;
        	for (Enumeration e=timeLocations().elements();e.hasMoreElements();) {
        		TimeLocation t = (TimeLocation)e.nextElement();
        		double pref = t.getNormalizedPreference();
        		if (pref>Constants.sPreferenceLevelRequired/2)
        			minTimePref = Math.min(minTimePref,pref);
        		if (pref<Constants.sPreferenceLevelProhibited/2)
        			maxTimePref = Math.max(maxTimePref,pref);
        	}
        	iMinMaxTimePreference = new double[] {minTimePref, maxTimePref};
    	}
    	return iMinMaxTimePreference;
    }
    
    public void setOrd(int ord) { iOrd = ord; }
    public int getOrd() { return iOrd; }
    public int compareTo(Object o) {
    	if (o==null || !(o instanceof Lecture)) return -1;
    	int cmp = Double.compare(getOrd(),((Lecture)o).getOrd());
    	if (cmp!=0) return cmp;
    	return super.compareTo(o);
    }
    
    public String getNote() { return iNote; }
    public void setNote(String note) { iNote = note; }
}