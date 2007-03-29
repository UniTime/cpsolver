package net.sf.cpsolver.coursett.model;


import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Locale;
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
import net.sf.cpsolver.coursett.heuristics.TimetableComparator;
import net.sf.cpsolver.coursett.heuristics.UniversalPerturbationsCounter;
import net.sf.cpsolver.ifs.constant.ConstantModel;
import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.perturbations.PerturbationsCounter;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.Counter;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.FastVector;


/**
 * Timetable model.
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

public class TimetableModel extends ConstantModel {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(TimetableModel.class);
    private static java.text.DecimalFormat sDoubleFormat = new java.text.DecimalFormat("0.00",new java.text.DecimalFormatSymbols(Locale.US));
    private long iGlobalRoomPreference = 0;
    private double iGlobalTimePreference = 0;
    private long iMinRoomPreference = 0;
    private long iMaxRoomPreference = 0;
    private long iBestInstructorDistancePreference = 0;
    private double iMinTimePreference = 0;
    private double iMaxTimePreference = 0;
    private int iBestDepartmentSpreadPenalty = 0;
    private int iBestSpreadPenalty = 0;
    private int iBestCommitedStudentConflicts = 0;
    private int iMaxGroupConstraintPreference = 0;
    private int iMinGroupConstraintPreference = 0;
    private Counter iGlobalGroupConstraintPreference = new Counter();
    private Counter iViolatedStudentConflicts = new Counter();
    private Counter iViolatedHardStudentConflicts = new Counter();
    private Counter iViolatedDistanceStudentConflicts = new Counter();
    private Counter iCommittedStudentConflictsCounter = new Counter();
    private FastVector iInstructorConstraints = new FastVector();
    private FastVector iJenrlConstraints = new FastVector();
    private FastVector iRoomConstraints = new FastVector();
    private FastVector iDepartmentSpreadConstraints = new FastVector();
    private FastVector iSpreadConstraints = new FastVector();
    private FastVector iGroupConstraints = new FastVector();
    private FastVector iClassLimitConstraints = new FastVector();
    private DataProperties iProperties = null;
    private TimetableComparator iCmp = null;
    private UniversalPerturbationsCounter iPertCnt = null;
    private int iYear = -1;
    
    private HashSet iAllStudents = new HashSet();
    
    /** Back-to-back classes: maximal distance for no prefernce */
	private double iInstructorNoPreferenceLimit = 0.0;
    /** Back-to-back classes: maximal distance for discouraged prefernce */
	private double iInstructorDiscouragedLimit = 5.0;
    /** Back-to-back classes: maximal distance for strongly discouraged prefernce (everything above is prohibited) */
	private double iInstructorProhibitedLimit = 20.0;
	
	private double iStudentDistanceLimit = 67.0;
	private double iStudentDistanceLimit75min = 100.0;
    
    public TimetableModel(DataProperties properties) {
        super();
        iProperties = properties;
        iInstructorNoPreferenceLimit = iProperties.getPropertyDouble("Instructor.NoPreferenceLimit", iInstructorNoPreferenceLimit);
        iInstructorDiscouragedLimit = iProperties.getPropertyDouble("Instructor.DiscouragedLimit",iInstructorDiscouragedLimit);
        iInstructorProhibitedLimit = iProperties.getPropertyDouble("Instructor.ProhibitedLimit",iInstructorProhibitedLimit);
        iStudentDistanceLimit = iProperties.getPropertyDouble("Student.DistanceLimit",iStudentDistanceLimit);
        iStudentDistanceLimit75min = iProperties.getPropertyDouble("Student.DistanceLimit75min",iStudentDistanceLimit75min);
        iCmp = new TimetableComparator(properties);
        iPertCnt = new UniversalPerturbationsCounter(properties);
    }
    
    public double getInstructorNoPreferenceLimit() {
    	return iInstructorNoPreferenceLimit;
    }
    
    public double getInstructorDiscouragedLimit() {
    	return iInstructorDiscouragedLimit;
    }
    
    public double getInstructorProhibitedLimit() {
    	return iInstructorProhibitedLimit;
    }
    
    public double getStudentDistanceLimit() {
    	return iStudentDistanceLimit;
    }
    
    public double getStudentDistanceLimit75min() {
    	return iStudentDistanceLimit75min;
    }
    
    public boolean init(Solver solver) {
    	iCmp = new TimetableComparator(solver.getProperties());
        iPertCnt = new UniversalPerturbationsCounter(solver.getProperties());
    	return super.init(solver);
    }
    
    public void addVariable(Variable variable) {
    	super.addVariable(variable);
    	Lecture lecture = (Lecture)variable;
    	if (lecture.isCommitted()) return;
    	double[] minMaxTimePref = lecture.getMinMaxTimePreference();
    	iMinTimePreference += minMaxTimePref[0];
    	iMaxTimePreference += minMaxTimePref[1];
    	int[] minMaxRoomPref = lecture.getMinMaxRoomPreference();
    	iMinRoomPreference += minMaxRoomPref[0];
    	iMaxRoomPreference += minMaxRoomPref[1];
    }
    
    public void removeVariable(Variable variable) {
    	super.removeVariable(variable);
    	Lecture lecture = (Lecture)variable;
    	if (lecture.isCommitted()) return;
    	double[] minMaxTimePref = lecture.getMinMaxTimePreference();
    	iMinTimePreference -= minMaxTimePref[0];
    	iMaxTimePreference -= minMaxTimePref[1];
    	int[] minMaxRoomPref = lecture.getMinMaxRoomPreference();
    	iMinRoomPreference -= minMaxRoomPref[0];
    	iMaxRoomPreference -= minMaxRoomPref[1];
    }
    
    
    public DataProperties getProperties() { return iProperties; }

    /** Overall room preference */
    public long getGlobalRoomPreference() { return iGlobalRoomPreference; }
    /** Overall time preference */
    public double getGlobalTimePreference() { return iGlobalTimePreference; }
    /** Number of student conflicts */
    public long getViolatedStudentConflicts() { return iViolatedStudentConflicts.get(); }
    /** Number of student conflicts */
    public long countViolatedStudentConflicts() {
    	long studentConflicts = 0;
        for (Enumeration it1=iJenrlConstraints.elements();it1.hasMoreElements();) {
            JenrlConstraint jenrl = (JenrlConstraint)it1.nextElement();
            Lecture a = (Lecture)jenrl.first();
            Lecture b = (Lecture)jenrl.second();
            //if (a.getAssignment()!=null && b.getAssignment()!=null && JenrlConstraint.isInConflict((Placement)a.getAssignment(),(Placement)b.getAssignment()))
            if (jenrl.isInConflict())
                studentConflicts+=jenrl.getJenrl();
        }
        return studentConflicts;
    }
    /** Number of student conflicts */
    public Counter getViolatedStudentConflictsCounter() { return iViolatedStudentConflicts; }
    public Counter getViolatedHardStudentConflictsCounter() { return iViolatedHardStudentConflicts; }
    public Counter getViolatedDistanceStudentConflictsCounter() { return iViolatedDistanceStudentConflicts; }
    
    /** Overall group constraint preference */
    public long getGlobalGroupConstraintPreference() { return iGlobalGroupConstraintPreference.get(); }
    /** Overall group constraint preference */
    public Counter getGlobalGroupConstraintPreferenceCounter() { return iGlobalGroupConstraintPreference; }
    /** Overall instructor distance (back-to-back) preference */
    public long getInstructorDistancePreference() {
        long pref = 0;
        for (Enumeration it1=iInstructorConstraints.elements();it1.hasMoreElements();) {
            InstructorConstraint constraint = (InstructorConstraint)it1.nextElement();
            pref+=constraint.getPreference();
        }
        return pref;
    }
    /** The worst instructor distance (back-to-back) preference */
    public long getInstructorWorstDistancePreference() {
        long pref = 0;
        for (Enumeration it1=iInstructorConstraints.elements();it1.hasMoreElements();) {
            InstructorConstraint constraint = (InstructorConstraint)it1.nextElement();
            pref+=constraint.getWorstPreference();
        }
        return pref;
    }
    /** Overall number of useless time slots */
    public long getUselessSlots() {
        long uselessSlots = 0;
        for (Enumeration it1=iRoomConstraints.elements();it1.hasMoreElements();) {
            RoomConstraint constraint = (RoomConstraint)it1.nextElement();
            uselessSlots+=((RoomConstraint)constraint).countUselessSlots();
        }
        return uselessSlots;
    }
    /** Overall number of useless time slots */
    public long getUselessHalfHours() {
        long uselessSlots = 0;
        for (Enumeration it1=iRoomConstraints.elements();it1.hasMoreElements();) {
            RoomConstraint constraint = (RoomConstraint)it1.nextElement();
            uselessSlots+=((RoomConstraint)constraint).countUselessSlotsHalfHours();
        }
        return uselessSlots;
    }
    /** Overall number of useless time slots */
    public long getBrokenTimePatterns() {
        long uselessSlots = 0;
        for (Enumeration it1=iRoomConstraints.elements();it1.hasMoreElements();) {
            RoomConstraint constraint = (RoomConstraint)it1.nextElement();
            uselessSlots+=((RoomConstraint)constraint).countUselessSlotsBrokenTimePatterns();
        }
        return uselessSlots;
    }
    /** Overall number of student conflicts caused by distancies (back-to-back classes are too far)*/
    public long getStudentDistanceConflicts() {
    	/*
    	if (iViolatedDistanceStudentConflicts.get()!=countStudentDistanceConflicts()) {
    		System.err.println("TimetableModel.getStudentDistanceConflicts() is not working properly");
    	}
    	*/
    	return iViolatedDistanceStudentConflicts.get();
    }
    public long countStudentDistanceConflicts() {
        long nrConflicts = 0;
        for (Enumeration it1=iJenrlConstraints.elements();it1.hasMoreElements();) {
            JenrlConstraint jenrl = (JenrlConstraint)it1.nextElement();
            if (jenrl.isInConflict() && 
                    !((Placement)jenrl.first().getAssignment()).getTimeLocation().hasIntersection(((Placement)jenrl.second().getAssignment()).getTimeLocation()))
                    nrConflicts += jenrl.getJenrl();
        }
        return nrConflicts;
    }
    /** Overall hard student conflicts (student conflict between single section classes) */ 
    public long getHardStudentConflicts() {
    	/*
    	if (iViolatedHardStudentConflicts.get()!=countHardStudentConflicts()) {
    		System.err.println("TimetableModel.getHardStudentConflicts() is not working properly");
    	}
    	*/
    	return iViolatedHardStudentConflicts.get(); 
    }
    public long countHardStudentConflicts() {
        long hardStudentConflicts = 0;
        for (Enumeration it1=iJenrlConstraints.elements();it1.hasMoreElements();) {
            JenrlConstraint jenrl = (JenrlConstraint)it1.nextElement();
            if (jenrl.isInConflict()) {
                Lecture l1 = (Lecture)jenrl.first();
                Lecture l2 = (Lecture)jenrl.second();
                if (l1.areStudentConflictsHard(l2))
                    hardStudentConflicts+=jenrl.getJenrl();
            }
        }
        return hardStudentConflicts;
    }
    public Counter getCommittedStudentConflictsCounter() { return iCommittedStudentConflictsCounter; }
    public int getCommitedStudentConflicts() {
    	/*
    	if (iCommittedStudentConflictsCounter.get()!=countCommitedStudentConflicts()) {
    		System.err.println("TimetableModel.getCommitedStudentConflicts() is not working properly");
    	}
    	*/
    	return (int)iCommittedStudentConflictsCounter.get(); 
    }
    public int countCommitedStudentConflicts() {
    	int commitedStudentConflicts = 0;
    	for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
    		Lecture lecture = (Lecture)e.nextElement();
    		commitedStudentConflicts+=lecture.getCommitedConflicts((Placement)lecture.getAssignment());
    	}
    	return commitedStudentConflicts;
    }
    
    /** When a value is assigned to a variable -- update gloval preferences */
    public void afterAssigned(long iteration, Value value) {
        super.afterAssigned(iteration, value);
        if (value==null) return;
        Placement placement = (Placement)value;
        iGlobalRoomPreference += placement.sumRoomPreference();
        iGlobalTimePreference += placement.getTimeLocation().getNormalizedPreference();
    }
    /** When a value is unassigned from a variable -- update gloval preferences */
    public void afterUnassigned(long iteration, Value value) {
        super.afterUnassigned(iteration, value);
        if (value==null) return;
        Placement placement = (Placement)value;
        iGlobalRoomPreference -= placement.sumRoomPreference();
        iGlobalTimePreference -= placement.getTimeLocation().getNormalizedPreference();
    }

    /** Student final sectioning (switching students between sections of the same class in order to minimize overall number of student conflicts) */
    public void switchStudents() {
    	FinalSectioning sect = new FinalSectioning(this);
    	sect.run();
    }

    public String toString() {
        return "TimetableModel{"+
        "\n  super="+super.toString()+
        "\n  studentConflicts="+iViolatedStudentConflicts.get()+
//        "\n  studentConflicts(violated room distance)="+iViolatedRoomDistanceStudentConflicts.get()+
//        "\n  studentPreferences="+iRoomDistanceStudentPreference.get()+
        "\n  roomPreferences="+iGlobalRoomPreference+
        "\n  timePreferences="+iGlobalTimePreference+
        "\n  groupConstraintPreferences="+iGlobalGroupConstraintPreference.get()+
        "\n}";
    }
    
    /** Overall number of too big rooms (rooms with more than 3/2 seats than needed) */
    public int countTooBigRooms() {
        int tooBigRooms=0;
        for (Enumeration it1=assignedVariables().elements();it1.hasMoreElements();) {
            Lecture lecture = (Lecture)it1.nextElement();
            if (lecture.getAssignment()==null) continue;
            Placement placement = (Placement)lecture.getAssignment();
            tooBigRooms += placement.getTooBigRoomPreference();
        }
        return tooBigRooms;
    }

    /** Overall departmental spread penalty */
    public int getDepartmentSpreadPenalty() {
        if (iDepartmentSpreadConstraints.isEmpty()) return 0;
        int penalty = 0;
        for (Enumeration e=iDepartmentSpreadConstraints.elements();e.hasMoreElements();) {
            DepartmentSpreadConstraint c = (DepartmentSpreadConstraint)e.nextElement();
            penalty += ((DepartmentSpreadConstraint)c).getPenalty();
        }
        return penalty;
    }
    
    /** Overall spread penalty */
    public int getSpreadPenalty() {
        if (iSpreadConstraints.isEmpty()) return 0;
        int penalty = 0;
        for (Enumeration e=iSpreadConstraints.elements();e.hasMoreElements();) {
            SpreadConstraint c = (SpreadConstraint)e.nextElement();
            penalty += ((SpreadConstraint)c).getPenalty();
        }
        return penalty;
    }
    
    /** Global info */
    public java.util.Hashtable getInfo() {
        Hashtable ret = super.getInfo();
        ret.put("Memory usage", getMem());
        ret.put("Room preferences", getPerc(iGlobalRoomPreference,iMinRoomPreference,iMaxRoomPreference)+"% ("+iGlobalRoomPreference+")");
        ret.put("Time preferences", getPerc(iGlobalTimePreference,iMinTimePreference,iMaxTimePreference)+"% ("+sDoubleFormat.format(iGlobalTimePreference)+")");
        ret.put("Distribution preferences", getPerc(iGlobalGroupConstraintPreference.get(),iMinGroupConstraintPreference,iMaxGroupConstraintPreference)+"% ("+iGlobalGroupConstraintPreference.get()+")");
        int commitedStudentConflicts = getCommitedStudentConflicts(); 
        ret.put("Student conflicts", (commitedStudentConflicts+getViolatedStudentConflicts())+" [committed:"+commitedStudentConflicts+", hard:"+getHardStudentConflicts()+"]");
        if (getProperties().getPropertyBoolean("General.UseDistanceConstraints",false)) {
        	ret.put("Student conflicts", (commitedStudentConflicts+getViolatedStudentConflicts())+" [committed:"+commitedStudentConflicts+", distance:"+getStudentDistanceConflicts()+", hard:"+getHardStudentConflicts()+"]");
            ret.put("Back-to-back instructor preferences", getPerc(getInstructorDistancePreference(),0,getInstructorWorstDistancePreference())+"% ("+getInstructorDistancePreference()+")");
        }
        if (getProperties().getPropertyBoolean("General.DeptBalancing", false)) {
            ret.put("Department balancing penalty", sDoubleFormat.format(((double)getDepartmentSpreadPenalty())/12.0));
        }
        ret.put("Same subpart balancing penalty", sDoubleFormat.format(((double)getSpreadPenalty())/12.0));
        ret.put("Too big rooms", getPercRev(countTooBigRooms(),0,Constants.sPreferenceLevelStronglyDiscouraged*variables().size())+"% ("+countTooBigRooms()+")");
        ret.put("Useless half-hours", getPercRev(getUselessSlots(),0,Constants.sPreferenceLevelStronglyDiscouraged*getRoomConstraints().size()*Constants.SLOTS_PER_DAY_NO_EVENINGS*Constants.NR_DAYS_WEEK)+"% ("+getUselessHalfHours()+" + "+getBrokenTimePatterns()+")");
        return ret;
    }
    
    public java.util.Hashtable getInfo(Vector variables) {
        Hashtable ret = super.getInfo(variables);
        ret.put("Memory usage", getMem());
        
        int roomPref = 0, minRoomPref = 0, maxRoomPref = 0;
        double timePref = 0, minTimePref = 0, maxTimePref = 0;
        double grPref = 0, minGrPref = 0, maxGrPref = 0;
        long allSC = 0, hardSC = 0, distSC = 0;
        int instPref = 0, worstInstrPref = 0;
        int spreadPen = 0, deptSpreadPen = 0;
        int tooBigRooms = 0;
        int rcs = 0, uselessSlots = 0, uselessSlotsHH = 0, uselessSlotsBTP = 0;
        
        HashSet used = new HashSet();
        
        for (Enumeration e=variables.elements();e.hasMoreElements();) {
        	Lecture lecture = (Lecture)e.nextElement();
        	if (lecture.isCommitted()) continue;
        	Placement placement = (Placement)lecture.getAssignment();
        	
        	int[] minMaxRoomPref = lecture.getMinMaxRoomPreference();
        	minRoomPref += minMaxRoomPref[0];
        	maxRoomPref += minMaxRoomPref[1];
        	
        	double[] minMaxTimePref = lecture.getMinMaxTimePreference();
        	minTimePref += minMaxTimePref[0];
        	maxTimePref += minMaxTimePref[1];
        	
        	if (placement!=null) {
        		roomPref += placement.getRoomPreference();
        		timePref += placement.getTimeLocation().getNormalizedPreference();
        		tooBigRooms += placement.getTooBigRoomPreference();
        	}
        	
        	for (Enumeration f=lecture.constraints().elements();f.hasMoreElements();) {
        		Constraint c = (Constraint)f.nextElement();
        		if (!used.add(c)) continue;
        		
        		if (c instanceof InstructorConstraint) {
        			InstructorConstraint ic = (InstructorConstraint)c;
        			instPref += ic.getPreference();
        			worstInstrPref += ic.getWorstPreference();
        		}
        		
        		if (c instanceof DepartmentSpreadConstraint) {
        			DepartmentSpreadConstraint dsc = (DepartmentSpreadConstraint)c;
        			deptSpreadPen += dsc.getPenalty();
        		} else if (c instanceof SpreadConstraint) {
        			SpreadConstraint sc = (SpreadConstraint)c;
        			spreadPen += sc.getPenalty();
        		}
        		
        		if (c instanceof GroupConstraint) {
        			GroupConstraint gc = (GroupConstraint)c;
        			if (gc.isHard()) continue;
            		minGrPref += Math.min(gc.getPreference(),0);
            		maxGrPref += Math.max(gc.getPreference(),0);
            		grPref += gc.getCurrentPreference();
        		}
        		
        		if (c instanceof JenrlConstraint) {
        			JenrlConstraint jc = (JenrlConstraint)c;
        			if (!jc.isInConflict() || !jc.isOfTheSameProblem()) continue;
            		Lecture l1 = (Lecture)jc.first();
            		Lecture l2 = (Lecture)jc.second();
            		allSC += jc.getJenrl();
            		if (l1.areStudentConflictsHard(l2))
            			hardSC += jc.getJenrl();
            		Placement p1 = (Placement)l1.getAssignment();
            		Placement p2 = (Placement)l2.getAssignment();
            		if (!p1.getTimeLocation().hasIntersection(p2.getTimeLocation()))
            			distSC += jc.getJenrl();
        		}
        		
        		if (c instanceof RoomConstraint) {
        			RoomConstraint rc = (RoomConstraint)c;
        			uselessSlots+=rc.countUselessSlots();
        			uselessSlotsHH+=rc.countUselessSlotsHalfHours();
        			uselessSlotsBTP+=rc.countUselessSlotsBrokenTimePatterns();
        			rcs ++;
        		}
        	}
        }
        
        
        ret.put("Room preferences", getPerc(roomPref,minRoomPref,maxRoomPref)+"% ("+roomPref+")");
        ret.put("Time preferences", getPerc(timePref,minTimePref,maxTimePref)+"% ("+sDoubleFormat.format(timePref)+")");
        ret.put("Distribution preferences", getPerc(grPref,minGrPref,maxGrPref)+"% ("+grPref+")");
        ret.put("Student conflicts", allSC+" [committed:"+0+", hard:"+hardSC+"]");
        if (getProperties().getPropertyBoolean("General.UseDistanceConstraints",false)) {
        	ret.put("Student conflicts", allSC+" [committed:"+0+", distance:"+distSC+", hard:"+hardSC+"]");
            ret.put("Back-to-back instructor preferences", getPerc(instPref,0,worstInstrPref)+"% ("+instPref+")");
        }
        if (getProperties().getPropertyBoolean("General.DeptBalancing", false)) {
            ret.put("Department balancing penalty", sDoubleFormat.format(((double)deptSpreadPen)/12.0));
        }
        ret.put("Same subpart balancing penalty", sDoubleFormat.format(((double)spreadPen)/12.0));
        ret.put("Too big rooms", getPercRev(tooBigRooms,0,Constants.sPreferenceLevelStronglyDiscouraged*variables.size())+"% ("+tooBigRooms+")");
        ret.put("Useless half-hours", getPercRev(uselessSlots,0,Constants.sPreferenceLevelStronglyDiscouraged*rcs*Constants.SLOTS_PER_DAY_NO_EVENINGS*Constants.NR_DAYS_WEEK)+"% ("+uselessSlotsHH+" + "+uselessSlotsBTP+")");

        return ret;
    }

    private int iBestTooBigRooms;
    private long iBestUselessSlots;
    private double iBestGlobalTimePreference;
    private long iBestGlobalRoomPreference;
    private long iBestGlobalGroupConstraintPreference;
    private long iBestViolatedStudentConflicts;
    private long iBestHardStudentConflicts;

    /** Overall number of too big rooms of the best solution ever found */
    public int bestTooBigRooms() { return iBestTooBigRooms; }
    /** Overall number of useless slots of the best solution ever found */
    public long bestUselessSlots() { return iBestUselessSlots;}
    /** Overall time preference of the best solution ever found */
    public double bestGlobalTimePreference() { return iBestGlobalTimePreference;}
    /** Overall room preference of the best solution ever found */
    public long bestGlobalRoomPreference() { return iBestGlobalRoomPreference;}
    /** Overall group constraint preference of the best solution ever found */
    public long bestGlobalGroupConstraintPreference() { return iBestGlobalGroupConstraintPreference;}
    /** Overall number of student conflicts of the best solution ever found */
    public long bestViolatedStudentConflicts() { return iBestViolatedStudentConflicts;}
    /** Overall number of student conflicts between single section classes of the best solution ever found */
    public long bestHardStudentConflicts() { return iBestHardStudentConflicts;}
    /** Overall instructor distance preference of the best solution ever found */
    public long bestInstructorDistancePreference() { return iBestInstructorDistancePreference; }
    /** Overall departmental spread penalty of the best solution ever found */
    public int bestDepartmentSpreadPenalty() { return iBestDepartmentSpreadPenalty; }
    public int bestSpreadPenalty() { return iBestSpreadPenalty; }
    public int bestCommitedStudentConflicts() { return iBestCommitedStudentConflicts; }
    
    public void saveBest() {
        super.saveBest();
        iBestTooBigRooms = countTooBigRooms();
        iBestUselessSlots = getUselessSlots();
        iBestGlobalTimePreference = getGlobalTimePreference();
        iBestGlobalRoomPreference = getGlobalRoomPreference();
        iBestGlobalGroupConstraintPreference = getGlobalGroupConstraintPreference();
        iBestViolatedStudentConflicts = getViolatedStudentConflicts();
        iBestHardStudentConflicts = getHardStudentConflicts();
        iBestInstructorDistancePreference = getInstructorDistancePreference();
        iBestDepartmentSpreadPenalty = getDepartmentSpreadPenalty();
        iBestSpreadPenalty = getSpreadPenalty();
        iBestCommitedStudentConflicts = getCommitedStudentConflicts();
    }

    public void addConstraint(Constraint constraint) {
        super.addConstraint(constraint);
        if (constraint instanceof InstructorConstraint) {
            iInstructorConstraints.addElement(constraint);
        } else if (constraint instanceof JenrlConstraint) {
            iJenrlConstraints.addElement(constraint);
        } else if (constraint instanceof RoomConstraint) {
            iRoomConstraints.addElement(constraint);
        } else if (constraint instanceof DepartmentSpreadConstraint) {
            iDepartmentSpreadConstraints.addElement(constraint);
        } else if (constraint instanceof SpreadConstraint) {
            iSpreadConstraints.addElement(constraint);
        } else if (constraint instanceof ClassLimitConstraint) {
        	iClassLimitConstraints.addElement(constraint);
        } else if (constraint instanceof GroupConstraint) {
            iGroupConstraints.addElement(constraint);
            if (!constraint.isHard()) {
            	GroupConstraint gc = (GroupConstraint)constraint;
            	iMinGroupConstraintPreference += Math.min(gc.getPreference(),0);
            	iMaxGroupConstraintPreference += Math.max(gc.getPreference(),0);
            }
        }
    }
    public void removeConstraint(Constraint constraint) {
        super.removeConstraint(constraint);
        if (constraint instanceof InstructorConstraint) {
            iInstructorConstraints.removeElement(constraint);
        } else if (constraint instanceof JenrlConstraint) {
            iJenrlConstraints.removeElement(constraint);
        } else if (constraint instanceof RoomConstraint) {
            iRoomConstraints.removeElement(constraint);
        } else if (constraint instanceof DepartmentSpreadConstraint) {
            iDepartmentSpreadConstraints.removeElement(constraint);
        } else if (constraint instanceof SpreadConstraint) {
            iSpreadConstraints.removeElement(constraint);
        } else if (constraint instanceof ClassLimitConstraint) {
        	iClassLimitConstraints.removeElement(constraint);
        } else if (constraint instanceof GroupConstraint) {
            iGroupConstraints.removeElement(constraint);
        }
    }

    /** The list of all instructor constraints */
    public Vector getInstructorConstraints() { return iInstructorConstraints; }
    /** The list of all group constraints */
    public Vector getGroupConstraints() { return iGroupConstraints; }
    /** The list of all jenrl constraints */
    public Vector getJenrlConstraints() { return iJenrlConstraints; }
    /** The list of all room constraints */
    public Vector getRoomConstraints() { return iRoomConstraints; }
    /** The list of all departmental spread constraints */
    public Vector getDepartmentSpreadConstraints() { return iDepartmentSpreadConstraints;  }
    public Vector getSpreadConstraints() { return iSpreadConstraints;  }
    public Vector getClassLimitConstraints() { return iClassLimitConstraints; }
    /** Max capacity for too big rooms (3/2 of the number of students) */
    public double getTotalValue() {
    	return iCmp.currentValue(this,iPertCnt);
    }
    public double getTotalValue(Vector variables) {
    	return iCmp.currentValue(this,iPertCnt, variables);
    }
    
    public int getYear() { return iYear; }
    public void setYear(int year) { iYear=year; }
    
    public TimetableComparator getTimetableComparator() { return iCmp;}
    public PerturbationsCounter getPerturbationsCounter() { return iPertCnt; }
    
    public Set getAllStudents() {
    	return iAllStudents;
    }
    public void addStudent(Student student) {
    	iAllStudents.add(student);
    }
    public void removeStudent(Student student) {
    	iAllStudents.remove(student);
    }
    
    /** Returns amount of allocated memory.
     * @return amount of allocated memory to be written in the log
     */
    public static synchronized String getMem() {
        Runtime rt = Runtime.getRuntime();
        return sDoubleFormat.format(((double)(rt.totalMemory()-rt.freeMemory()))/1048576)+"M";
    }
}
