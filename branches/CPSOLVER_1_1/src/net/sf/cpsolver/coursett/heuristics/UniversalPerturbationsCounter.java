package net.sf.cpsolver.coursett.heuristics;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.coursett.Constants;
import net.sf.cpsolver.coursett.constraint.InstructorConstraint;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.perturbations.DefaultPerturbationsCounter;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Perturbation penalty computation.
 * <br><br>
 * In practise, the strategy for computing perturbations needs to be extended. For example, a change in time is usually 
 * much worse than a movement to a different classroom. The number of enrolled/involved students should also be taken 
 * into account. Another factor is whether the solution has already been published or not.
 * <br>
 * The priorities for evaluating perturbations are as follows. Before publishing timetable:<ul>
 * <li>minimize number of classes with time changes,
 * <li>minimize number of student conflicts,
 * <li>optimize satisfaction of problem soft constraints.
 * </ul><br>
 * After publishing the timetable (class time changes are not allowed):<ul>
 * <li>minimize number of additional (new) student conflicts,
 * <li>minimize number of students with time changes,
 * <li>minimize number of classes with time changes,
 * <li>optimize satisfaction of problem soft constraints.
 * </ul>
 * In both cases, the number of classes with room change is not significant at all. Before the timetable is published, 
 * minimizing the number of classes with time changes is the most important criteria for the MPP as long as it does not 
 * create too many additional student conflicts in the process. Therefore, as a compromise, the cost (in equivalent 
 * conflicts) of changing the time assigned to a class equals a number like 5% of the students enrolled in that class. 
 * Otherwise none of our other criteria would have any importance. 
 * <br><br>
 * Similar properties apply between other criteria as well. To fulfil all these needs we have crated a function (called 
 * perturbations penalty) which can be computed over a partial solution. This is a weighted sum of various perturbations 
 * criteria like the number of classes with time changes or the number of additional student conflicts. This 
 * perturbation penalty is added as an extra optimization criterion to the solution comparator and to value selection 
 * criterion, so we can also setup the weights between this perturbation penalty and other (initial) soft constraints.
 * <br><br>
 * Parameters:
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>Perturbations.DifferentPlacement</td><td>{@link Double}</td><td>Different value than initial is assigned</td></tr>
 * <tr><td>Perturbations.AffectedStudentWeight</td><td>{@link Double}</td><td>Number of students which are enrolled in a class which is placed to a different location than initial (a student can be included twice or more)</td></tr>
 * <tr><td>Perturbations.AffectedInstructorWeight</td><td>{@link Double}</td><td>Number of instructors which are assigned to classes which are placed to different locations than initial (an instructor can be included twice or more)</td></tr>
 * <tr><td>Perturbations.DifferentRoomWeight</td><td>{@link Double}</td><td>Number of classes which are placed to a different room than initial</td></tr>
 * <tr><td>Perturbations.DifferentBuildingWeight</td><td>{@link Double}</td><td>Number of classes which are placed to a different building than initial</td></tr>
 * <tr><td>Perturbations.DifferentTimeWeight</td><td>{@link Double}</td><td>Number of classes which are placed in a different time than initial</td></tr>
 * <tr><td>Perturbations.DifferentDayWeight</td><td>{@link Double}</td><td>Number of classes which are placed in a different days than initial</td></tr>
 * <tr><td>Perturbations.DifferentHourWeight</td><td>{@link Double}</td><td>Number of classes which are placed in a different hours than initial</td></tr>
 * <tr><td>Perturbations.DeltaStudentConflictsWeight</td><td>{@link Double}</td><td>Difference of student conflicts of classes assigned to current placements instead of initial placements. It is a difference between number of students conflicts which are in the initial solution and the current one. Student conflicts created by classes without initial placement are not taken into account</td></tr>
 * <tr><td>Perturbations.NewStudentConflictsWeight</td><td>{@link Double}</td><td>New created student conflicts -- particular students are taken into account. Student conflicts created by classes without initial placement are not taken into account</td></tr>
 * <tr><td>Perturbations.TooFarForInstructorsWeight</td><td>{@link Double}</td><td>New placement of a class is too far from the intial placement (instructor-wise). It is computed only when the class has an instructor assigned, moreover:<ul><li>0 < distance(currentPlacement,initialPlacement) <= 5  .. weight is taken once<li>5 < distance(currentPlacement,initialPlacement) <= 20 .. weight is taken twice<li>20 < distance(currentPlacement,initialPlacement)       .. weight is taken ten times</ul></td></tr>
 * <tr><td>Perturbations.TooFarForStudentsWeight</td><td>{@link Double}</td><td>New placement of a class is too far from the intial placement (instructor-student). It is weighted by the number of students enrolled in the class when distance(currentPlacement,initialPlacement) > 67</td></tr>
 * <tr><td>Perturbations.DeltaInstructorDistancePreferenceWeight</td><td>{@link Double}</td><td>Difference between number of instructor distance preferences of the initial (but maybe inconsistent) solution and the current solution. Instructor distance preferences of classes without initial placement are not taken into account</td></tr>
 * <tr><td>Perturbations.DeltaRoomPreferenceWeight</td><td>{@link Double}</td><td>Difference between room preferences of the initial and the current solution. Room preferences of classes without initial placement are not taken into account</td></tr>
 * <tr><td>Perturbations.DeltaTimePreferenceWeight</td><td>{@link Double}</td><td>Difference between time preferences of the initial and the current solution. Time preferences of classes without initial placement are not taken into account</td></tr>
 * <tr><td>Perturbations.AffectedStudentByTimeWeight</td><td>{@link Double}</td><td>Number of students which are enrolled in a class which is placed to a different time than initial </td></tr>
 * <tr><td>Perturbations.AffectedInstructorByTimeWeight</td><td>{@link Double}</td><td>Number of instructors which are assigned to classes which are placed to different time than initial</td></tr>
 * <tr><td>Perturbations.AffectedStudentByRoomWeight</td><td>{@link Double}</td><td>Number of students which are enrolled in a class which is placed to a different room than initial </td></tr>
 * <tr><td>Perturbations.AffectedInstructorByRoomWeight</td><td>{@link Double}</td><td>Number of instructors which are assigned to classes which are placed to different room than initial </td></tr>
 * <tr><td>Perturbations.AffectedStudentByBldgWeight</td><td>{@link Double}</td><td>Number of students which are enrolled in a class which is placed to a different building than initial</td></tr>
 * <tr><td>Perturbations.AffectedInstructorByBldgWeight</td><td>{@link Double}</td><td>Number of instructors which are assigned to classes which are placed to different building than initial </td></tr>
 * </table>
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

public class UniversalPerturbationsCounter extends DefaultPerturbationsCounter {
    private double iDifferentPlacement = 1.0;
    private double iAffectedStudentWeight = 0.0;
    private double iAffectedInstructorWeight = 0.0;
    private double iAffectedStudentByTimeWeight = 0.0;
    private double iAffectedInstructorByTimeWeight = 0.0;
    private double iAffectedStudentByRoomWeight = 0.0;
    private double iAffectedInstructorByRoomWeight = 0.0;
    private double iAffectedStudentByBldgWeight = 0.0;
    private double iAffectedInstructorByBldgWeight = 0.0;
    private double iDifferentRoomWeight = 0.0;
    private double iDifferentBuildingWeight = 0.0;
    private double iDifferentTimeWeight = 0.0;
    private double iDifferentDayWeight = 0.0;
    private double iDifferentHourWeight = 0.0;
    private double iNewStudentConflictsWeight = 0.0;
    private double iDeltaStudentConflictsWeight = 0.0;
    private double iTooFarForInstructorsWeight = 0.0;
    private double iTooFarForStudentsWeight = 0.0;
    private double iDeltaInstructorDistancePreferenceWeight = 0.0;
    private double iDeltaRoomPreferenceWeight = 0.0;
    private double iDeltaTimePreferenceWeight = 0.0;
    private boolean iMPP = false;
    
    public UniversalPerturbationsCounter(DataProperties properties) {
        super(properties);
        iMPP = properties.getPropertyBoolean("General.MPP",false);
        iDifferentPlacement = properties.getPropertyDouble("Perturbations.DifferentPlacement",iDifferentPlacement);
        iAffectedStudentWeight = properties.getPropertyDouble("Perturbations.AffectedStudentWeight",iAffectedStudentWeight);
        iAffectedInstructorWeight = properties.getPropertyDouble("Perturbations.AffectedInstructorWeight",iAffectedInstructorWeight);
        iAffectedStudentByTimeWeight = properties.getPropertyDouble("Perturbations.AffectedStudentByTimeWeight",iAffectedStudentByTimeWeight);
        iAffectedInstructorByTimeWeight = properties.getPropertyDouble("Perturbations.AffectedInstructorByTimeWeight",iAffectedInstructorByTimeWeight);
        iAffectedStudentByRoomWeight = properties.getPropertyDouble("Perturbations.AffectedStudentByRoomWeight",iAffectedStudentByRoomWeight);
        iAffectedInstructorByRoomWeight = properties.getPropertyDouble("Perturbations.AffectedInstructorByRoomWeight",iAffectedInstructorByRoomWeight);
        iAffectedStudentByBldgWeight = properties.getPropertyDouble("Perturbations.AffectedStudentByBldgWeight",iAffectedStudentByBldgWeight);
        iAffectedInstructorByBldgWeight = properties.getPropertyDouble("Perturbations.AffectedInstructorByBldgWeight",iAffectedInstructorByBldgWeight);
        iDifferentRoomWeight = properties.getPropertyDouble("Perturbations.DifferentRoomWeight",iDifferentRoomWeight);
        iDifferentBuildingWeight = properties.getPropertyDouble("Perturbations.DifferentBuildingWeight",iDifferentBuildingWeight);
        iDifferentTimeWeight = properties.getPropertyDouble("Perturbations.DifferentTimeWeight",iDifferentTimeWeight);
        iDifferentDayWeight = properties.getPropertyDouble("Perturbations.DifferentDayWeight",iDifferentDayWeight);
        iDifferentHourWeight = properties.getPropertyDouble("Perturbations.DifferentHourWeight",iDifferentHourWeight);
        iDeltaStudentConflictsWeight = properties.getPropertyDouble("Perturbations.DeltaStudentConflictsWeight",iDeltaStudentConflictsWeight);
        iNewStudentConflictsWeight = properties.getPropertyDouble("Perturbations.NewStudentConflictsWeight",iNewStudentConflictsWeight);
        iTooFarForInstructorsWeight = properties.getPropertyDouble("Perturbations.TooFarForInstructorsWeight",iTooFarForInstructorsWeight);
        iTooFarForStudentsWeight = properties.getPropertyDouble("Perturbations.TooFarForStudentsWeight",iTooFarForStudentsWeight);
        iDeltaInstructorDistancePreferenceWeight = properties.getPropertyDouble("Perturbations.DeltaInstructorDistancePreferenceWeight",iDeltaInstructorDistancePreferenceWeight);
        iDeltaRoomPreferenceWeight = properties.getPropertyDouble("Perturbations.DeltaRoomPreferenceWeight",iDeltaRoomPreferenceWeight);
        iDeltaTimePreferenceWeight = properties.getPropertyDouble("Perturbations.DeltaTimePreferenceWeight",iDeltaTimePreferenceWeight);
    }
    
    protected double getPenalty(Value assignedValue, Value initialValue) {
        // assigned and initial value of the same lecture
        // assigned might be null
        Lecture lecture = (Lecture)initialValue.variable();
        Placement assignedPlacement = (assignedValue==null?null:(Placement)assignedValue);
        Placement initialPlacement = (Placement)initialValue;
        double penalty = 0.0;
        if (iDifferentPlacement!=0.0)
            penalty += iDifferentPlacement;
        if (iAffectedStudentWeight!=0.0)
            penalty += iAffectedStudentWeight*lecture.classLimit();
        if (iAffectedInstructorWeight!=0.0)
            penalty += iAffectedInstructorWeight*lecture.getInstructorConstraints().size();
        if (assignedValue!=null) {
            if ((iDifferentRoomWeight!=0.0 || iAffectedInstructorByRoomWeight!=0.0 || iAffectedStudentByRoomWeight!=0.0)) {
            	int nrDiff = initialPlacement.nrDifferentRooms(assignedPlacement);
            	penalty += nrDiff*iDifferentRoomWeight;
            	penalty += nrDiff*iAffectedInstructorByRoomWeight*lecture.getInstructorConstraints().size();
            	penalty += nrDiff*iAffectedStudentByRoomWeight * lecture.classLimit();
            } if ((iDifferentBuildingWeight!=0.0 || iAffectedInstructorByBldgWeight!=0.0 || iAffectedStudentByBldgWeight!=0.0)) {
            	int nrDiff = initialPlacement.nrDifferentBuildings(assignedPlacement);
            	penalty += nrDiff*iDifferentBuildingWeight;
            	penalty += nrDiff*iAffectedInstructorByBldgWeight*lecture.getInstructorConstraints().size();
            	penalty += nrDiff*iAffectedStudentByBldgWeight * lecture.classLimit();
            } if ((iDifferentTimeWeight!=0.0 || iAffectedInstructorByTimeWeight!=0.0 || iAffectedStudentByTimeWeight!=0.0) && !initialPlacement.getTimeLocation().equals(assignedPlacement.getTimeLocation())) {
                penalty += iDifferentTimeWeight;
                penalty += iAffectedInstructorByTimeWeight*lecture.getInstructorConstraints().size();
                penalty += iAffectedStudentByTimeWeight * lecture.classLimit();
            } if (iDifferentDayWeight!=0.0 && initialPlacement.getTimeLocation().getDayCode()!=assignedPlacement.getTimeLocation().getDayCode())
                penalty += iDifferentDayWeight;
            if (iDifferentHourWeight!=0.0 && initialPlacement.getTimeLocation().getStartSlot()!=assignedPlacement.getTimeLocation().getStartSlot())
                penalty += iDifferentHourWeight;
            if ((iTooFarForInstructorsWeight!=0.0 || iTooFarForStudentsWeight!=0.0) && !initialPlacement.getTimeLocation().equals(assignedPlacement.getTimeLocation())) {
                double distance = Placement.getDistance(initialPlacement, assignedPlacement);
                if (!lecture.getInstructorConstraints().isEmpty() && iTooFarForInstructorsWeight!=0.0) {
                    if (distance>0.0 && distance<=5.0) {
                        penalty += Constants.sPreferenceLevelDiscouraged*iTooFarForInstructorsWeight*lecture.getInstructorConstraints().size();
                    } else if (distance>5.0 && distance<=20.0) {
                        penalty += Constants.sPreferenceLevelStronglyDiscouraged*iTooFarForInstructorsWeight*lecture.getInstructorConstraints().size();
                    } else if (distance>20.0) {
                        penalty += Constants.sPreferenceLevelProhibited*iTooFarForInstructorsWeight*lecture.getInstructorConstraints().size();
                    }
                }
                if (iTooFarForStudentsWeight!=0.0 && distance > ((TimetableModel)lecture.getModel()).getStudentDistanceLimit())
                	penalty += iTooFarForStudentsWeight*lecture.classLimit();
            }
            if (iDeltaStudentConflictsWeight!=0.0) {
                int newStudentConflicts = lecture.countStudentConflicts(assignedValue);
                int oldStudentConflicts = lecture.countInitialStudentConflicts();
                penalty += iDeltaStudentConflictsWeight * (newStudentConflicts-oldStudentConflicts);
            }
            if (iNewStudentConflictsWeight!=0.0) {
                Set newStudentConflicts = lecture.conflictStudents(assignedValue);
                Set initialStudentConflicts = lecture.initialStudentConflicts();
                for (Iterator i=newStudentConflicts.iterator();i.hasNext();)
                    if (!initialStudentConflicts.contains(i.next())) penalty += iNewStudentConflictsWeight;
            }
            if (iDeltaTimePreferenceWeight!=0.0) {
                penalty += iDeltaTimePreferenceWeight * (assignedPlacement.getTimeLocation().getNormalizedPreference() - initialPlacement.getTimeLocation().getNormalizedPreference());
            }
            if (iDeltaRoomPreferenceWeight!=0.0) {
            	penalty += iDeltaRoomPreferenceWeight * (assignedPlacement.sumRoomPreference() - initialPlacement.sumRoomPreference());
            }
            if (iDeltaInstructorDistancePreferenceWeight!=0.0) {
            	for (Enumeration e=lecture.getInstructorConstraints().elements();e.hasMoreElements();) {
            		InstructorConstraint ic = (InstructorConstraint)e.nextElement();
            		for (Enumeration f=ic.variables().elements();f.hasMoreElements();) {
            			Lecture lect = (Lecture)f.nextElement();
            			if (lect.equals(lecture)) continue;
            			int initialPreference = (lect.getInitialAssignment()==null?Constants.sPreferenceLevelNeutral:ic.getDistancePreference(initialPlacement,(Placement)lect.getInitialAssignment()));
            			int assignedPreference = (lect.getAssignment()==null?Constants.sPreferenceLevelNeutral:ic.getDistancePreference(assignedPlacement,(Placement)lect.getAssignment()));
            			penalty += iDeltaInstructorDistancePreferenceWeight * (assignedPreference - initialPreference);
            		}
                }
            }
        }
        return penalty;
    }
    
    public void getInfo(Dictionary info, Model model) {
    	getInfo(info, model, null);
    }
    
    public void getInfo(Dictionary info, Model model, Vector variables) {
    	if (variables==null)
    		super.getInfo(info, model);
    	else
    		super.getInfo(info, model, variables);
        if (!iMPP) return;
        int perts = 0;
        long affectedStudents=0;
        int affectedInstructors=0;
        long affectedStudentsByTime=0;
        int affectedInstructorsByTime=0;
        long affectedStudentsByRoom=0;
        int affectedInstructorsByRoom=0;
        long affectedStudentsByBldg=0;
        int affectedInstructorsByBldg=0;
        int differentRoom=0;
        int differentBuilding=0;
        int differentTime=0;
        int differentDay=0;
        int differentHour=0;
        int tooFarForInstructors=0;
        int tooFarForStudents=0;
        int deltaStudentConflicts=0;
        int newStudentConflicts=0;
        double deltaTimePreferences=0;
        int deltaRoomPreferences=0;
        int deltaInstructorDistancePreferences=0;
        for (Enumeration e=(variables==null?model.perturbVariables():model.perturbVariables(variables)).elements();e.hasMoreElements();) {
            Variable variable = (Variable)e.nextElement();
            if (variable.getAssignment()==null || variable.getInitialAssignment()==null || variable.getAssignment().equals(variable.getInitialAssignment())) continue;
            perts++;
            Lecture lecture = (Lecture)variable ;
            Placement assignedPlacement = (Placement)variable.getAssignment();
            Placement initialPlacement = (Placement)variable.getInitialAssignment();
            affectedStudents += lecture.classLimit();
            affectedInstructors += lecture.getInstructorConstraints().size();
            
            int nrDiff = initialPlacement.nrDifferentRooms(assignedPlacement);
        	differentRoom += nrDiff;
            affectedInstructorsByRoom += nrDiff*lecture.getInstructorConstraints().size();
            affectedStudentsByRoom += nrDiff*lecture.classLimit();
            
            nrDiff = initialPlacement.nrDifferentBuildings(assignedPlacement);
            differentBuilding += nrDiff;
            affectedInstructorsByBldg += nrDiff*lecture.getInstructorConstraints().size();
            affectedStudentsByBldg += nrDiff*lecture.classLimit();
            
            deltaRoomPreferences += assignedPlacement.sumRoomPreference() - initialPlacement.sumRoomPreference();

            if (!initialPlacement.getTimeLocation().equals(assignedPlacement.getTimeLocation())) {
                differentTime++;
                affectedInstructorsByTime+=lecture.getInstructorConstraints().size();
                affectedStudentsByTime += lecture.classLimit();
            }
            if (initialPlacement.getTimeLocation().getDayCode()!=assignedPlacement.getTimeLocation().getDayCode()) differentDay++;
            if (initialPlacement.getTimeLocation().getStartSlot()!=assignedPlacement.getTimeLocation().getStartSlot()) differentHour++;
            if (!initialPlacement.getTimeLocation().equals(assignedPlacement.getTimeLocation())) {
                double distance = Placement.getDistance(initialPlacement, assignedPlacement);
                if (!lecture.getInstructorConstraints().isEmpty()) {
    	            if (distance>0.0 && distance<=5.0) {
    	                tooFarForInstructors+=Constants.sPreferenceLevelDiscouraged*lecture.getInstructorConstraints().size();
    	            } else if (distance>5.0 && distance<=20.0) {
    	                tooFarForInstructors+=Constants.sPreferenceLevelStronglyDiscouraged*lecture.getInstructorConstraints().size();
    	            } else if (distance>20.0) {
    	                tooFarForInstructors+=Constants.sPreferenceLevelProhibited*lecture.getInstructorConstraints().size();
    	            }
                }
                if (distance > ((TimetableModel)lecture.getModel()).getStudentDistanceLimit()) tooFarForStudents+=lecture.classLimit();
            }
            deltaStudentConflicts += lecture.countStudentConflicts(assignedPlacement) - lecture.countInitialStudentConflicts();
            Set newStudentConflictsSet = lecture.conflictStudents(assignedPlacement);
            Set initialStudentConflicts = lecture.initialStudentConflicts();
            for (Iterator e1=newStudentConflictsSet.iterator();e1.hasNext();)
                if (!initialStudentConflicts.contains(e1.next())) newStudentConflicts++;
            deltaTimePreferences += assignedPlacement.getTimeLocation().getNormalizedPreference() - initialPlacement.getTimeLocation().getNormalizedPreference();
            for (Enumeration e1=lecture.getInstructorConstraints().elements();e1.hasMoreElements();) {
            	InstructorConstraint ic = (InstructorConstraint)e1.nextElement();
            	for (Enumeration e2=ic.variables().elements();e2.hasMoreElements();) {
            		Lecture lect = (Lecture)e2.nextElement();
            		if (lect.equals(lecture)) continue;
            		int initialPreference = (lect.getInitialAssignment()==null?Constants.sPreferenceLevelNeutral:ic.getDistancePreference(initialPlacement,(Placement)lect.getInitialAssignment()));
            		int assignedPreference = (lect.getAssignment()==null?Constants.sPreferenceLevelNeutral:ic.getDistancePreference(assignedPlacement,(Placement)lect.getAssignment()));
            		deltaInstructorDistancePreferences += assignedPreference - initialPreference;
            	}
            }
        }
        if (perts!=0)
        	info.put("Perturbations: Different placement", String.valueOf(perts)+" (weighted "+sDoubleFormat.format(iDifferentPlacement*perts)+")");
        if (affectedStudents!=0)
        	info.put("Perturbations: Number of affected students", String.valueOf(affectedStudents)+" (weighted "+sDoubleFormat.format(iAffectedStudentWeight*affectedStudents)+")");
        if (affectedInstructors!=0)
        	info.put("Perturbations: Number of affected instructors", String.valueOf(affectedInstructors)+" (weighted "+sDoubleFormat.format(iAffectedInstructorWeight*affectedInstructors)+")");
        if (affectedStudentsByTime!=0)
        	info.put("Perturbations: Number of affected students [time]", String.valueOf(affectedStudentsByTime)+" (weighted "+sDoubleFormat.format(iAffectedStudentByTimeWeight*affectedStudentsByTime)+")");
        if (affectedInstructorsByTime!=0)
        	info.put("Perturbations: Number of affected instructors [time]", String.valueOf(affectedInstructorsByTime)+" (weighted "+sDoubleFormat.format(iAffectedInstructorByTimeWeight*affectedInstructorsByTime)+")");
        if (affectedStudentsByRoom!=0)
        	info.put("Perturbations: Number of affected students [room]", String.valueOf(affectedStudentsByRoom)+" (weighted "+sDoubleFormat.format(iAffectedStudentByRoomWeight*affectedStudentsByRoom)+")");
        if (affectedInstructorsByRoom!=0)
        	info.put("Perturbations: Number of affected instructors [room]", String.valueOf(affectedInstructorsByRoom)+" (weighted "+sDoubleFormat.format(iAffectedInstructorByRoomWeight*affectedInstructorsByRoom)+")");
        if (affectedStudentsByBldg!=0)
        	info.put("Perturbations: Number of affected students [bldg]", String.valueOf(affectedStudentsByBldg)+" (weighted "+sDoubleFormat.format(iAffectedStudentByBldgWeight*affectedStudentsByBldg)+")");
        if (affectedInstructorsByBldg!=0)
        	info.put("Perturbations: Number of affected instructors [bldg]", String.valueOf(affectedInstructorsByBldg)+" (weighted "+sDoubleFormat.format(iAffectedInstructorByBldgWeight*affectedInstructorsByBldg)+")");
        if (differentRoom!=0)
        	info.put("Perturbations: Different room", String.valueOf(differentRoom)+" (weighted "+sDoubleFormat.format(iDifferentRoomWeight*differentRoom)+")");
        if (differentBuilding!=0)
        	info.put("Perturbations: Different building", String.valueOf(differentBuilding)+" (weighted "+sDoubleFormat.format(iDifferentBuildingWeight*differentBuilding)+")");
        if (differentTime!=0)
        	info.put("Perturbations: Different time", String.valueOf(differentTime)+" (weighted "+sDoubleFormat.format(iDifferentTimeWeight*differentTime)+")");
        if (differentDay!=0)
        	info.put("Perturbations: Different day", String.valueOf(differentDay)+" (weighted "+sDoubleFormat.format(iDifferentDayWeight*differentDay)+")");
        if (differentHour!=0)
        	info.put("Perturbations: Different hour", String.valueOf(differentHour)+" (weighted "+sDoubleFormat.format(iDifferentHourWeight*differentHour)+")");
        if (tooFarForInstructors!=0)
        	info.put("Perturbations: New placement too far from initial [instructors]", String.valueOf(tooFarForInstructors)+" (weighted "+sDoubleFormat.format(iTooFarForInstructorsWeight*tooFarForInstructors)+")");
        if (tooFarForStudents!=0)
        	info.put("Perturbations: New placement too far from initial [students]", String.valueOf(tooFarForStudents)+" (weighted "+sDoubleFormat.format(iTooFarForStudentsWeight*tooFarForStudents)+")");
        if (deltaStudentConflicts!=0)
        	info.put("Perturbations: Delta student conflicts", String.valueOf(deltaStudentConflicts)+" (weighted "+sDoubleFormat.format(iDeltaStudentConflictsWeight*deltaStudentConflicts)+")");
        if (newStudentConflicts!=0)
        	info.put("Perturbations: New student conflicts", String.valueOf(newStudentConflicts)+" (weighted "+sDoubleFormat.format(iNewStudentConflictsWeight*newStudentConflicts)+")");
        if (deltaTimePreferences!=0)
        	info.put("Perturbations: Delta time preferences", String.valueOf(deltaTimePreferences)+" (weighted "+sDoubleFormat.format(iDeltaTimePreferenceWeight*deltaTimePreferences)+")");
        if (deltaRoomPreferences!=0)
        	info.put("Perturbations: Delta room preferences", String.valueOf(deltaRoomPreferences)+" (weighted "+sDoubleFormat.format(iDeltaRoomPreferenceWeight*deltaRoomPreferences)+")");
        if (deltaInstructorDistancePreferences!=0)
        	info.put("Perturbations: Delta instructor distance preferences", String.valueOf(deltaInstructorDistancePreferences)+" (weighted "+sDoubleFormat.format(iDeltaInstructorDistancePreferenceWeight*deltaInstructorDistancePreferences)+")");
    }
    
    public Hashtable getCompactInfo(Model model, boolean includeZero, boolean weighted) {
        Hashtable info = new Hashtable();
        if (!iMPP) return info;
        int perts = 0;
        long affectedStudents=0;
        int affectedInstructors=0;
        long affectedStudentsByTime=0;
        int affectedInstructorsByTime=0;
        long affectedStudentsByRoom=0;
        int affectedInstructorsByRoom=0;
        long affectedStudentsByBldg=0;
        int affectedInstructorsByBldg=0;
        int differentRoom=0;
        int differentBuilding=0;
        int differentTime=0;
        int differentDay=0;
        int differentHour=0;
        int tooFarForInstructors=0;
        int tooFarForStudents=0;
        int deltaStudentConflicts=0;
        int newStudentConflicts=0;
        double deltaTimePreferences=0;
        int deltaRoomPreferences=0;
        int deltaInstructorDistancePreferences=0;
        for (Enumeration e=model.perturbVariables().elements();e.hasMoreElements();) {
            Variable variable = (Variable)e.nextElement();
            if (variable.getAssignment()==null || variable.getInitialAssignment()==null || variable.getAssignment().equals(variable.getInitialAssignment())) continue;
            perts++;
            Lecture lecture = (Lecture)variable ;
            Placement assignedPlacement = (Placement)variable.getAssignment();
            Placement initialPlacement = (Placement)variable.getInitialAssignment();
            affectedStudents += lecture.classLimit();
            affectedInstructors += lecture.getInstructorConstraints().size();

            int nrDiff = initialPlacement.nrDifferentRooms(assignedPlacement);
        	differentRoom += nrDiff;
            affectedInstructorsByRoom += nrDiff*lecture.getInstructorConstraints().size();
            affectedStudentsByRoom += nrDiff*lecture.classLimit();
            
            nrDiff = initialPlacement.nrDifferentBuildings(initialPlacement);
            differentBuilding += nrDiff;
            affectedInstructorsByBldg += nrDiff*lecture.getInstructorConstraints().size();
            affectedStudentsByBldg += nrDiff*lecture.classLimit();
            
            deltaRoomPreferences += assignedPlacement.sumRoomPreference() - initialPlacement.sumRoomPreference();

            if (!initialPlacement.getTimeLocation().equals(assignedPlacement.getTimeLocation())) {
                differentTime++;
                affectedInstructorsByTime+=lecture.getInstructorConstraints().size();
                affectedStudentsByTime += lecture.classLimit();
            }
            if (initialPlacement.getTimeLocation().getDayCode()!=assignedPlacement.getTimeLocation().getDayCode()) differentDay++;
            if (initialPlacement.getTimeLocation().getStartSlot()!=assignedPlacement.getTimeLocation().getStartSlot()) differentHour++;
            if (!initialPlacement.getTimeLocation().equals(assignedPlacement.getTimeLocation())) {
                double distance = Placement.getDistance(initialPlacement, assignedPlacement);
                if (!lecture.getInstructorConstraints().isEmpty()) {
    	            if (distance>0.0 && distance<=5.0) {
    	                tooFarForInstructors+=Constants.sPreferenceLevelDiscouraged;
    	            } else if (distance>5.0 && distance<=20.0) {
    	                tooFarForInstructors+=Constants.sPreferenceLevelStronglyDiscouraged;
    	            } else if (distance>20.0) {
    	                tooFarForInstructors+=Constants.sPreferenceLevelProhibited;
    	            }
                }
                if (distance > ((TimetableModel)lecture.getModel()).getStudentDistanceLimit()) tooFarForStudents+=lecture.classLimit();
            }
            deltaStudentConflicts += lecture.countStudentConflicts(assignedPlacement) - lecture.countInitialStudentConflicts();
            Set newStudentConflictsSet = lecture.conflictStudents(assignedPlacement);
            Set initialStudentConflicts = lecture.initialStudentConflicts();
            for (Iterator e1=newStudentConflictsSet.iterator();e1.hasNext();)
                if (!initialStudentConflicts.contains(e1.next())) newStudentConflicts++;
            deltaTimePreferences += assignedPlacement.getTimeLocation().getNormalizedPreference() - initialPlacement.getTimeLocation().getNormalizedPreference();
        	for (Enumeration e1=lecture.getInstructorConstraints().elements();e1.hasMoreElements();) {
        		InstructorConstraint ic = (InstructorConstraint)e1.nextElement();
        		if (ic!=null) for (Enumeration e2=ic.variables().elements();e2.hasMoreElements();) {
        			Lecture lect = (Lecture)e2.nextElement();
        			if (lect.equals(lecture)) continue;
        			int initialPreference = (lect.getInitialAssignment()==null?Constants.sPreferenceLevelNeutral:ic.getDistancePreference(initialPlacement,(Placement)lect.getInitialAssignment()));
        			int assignedPreference = (lect.getAssignment()==null?Constants.sPreferenceLevelNeutral:ic.getDistancePreference(assignedPlacement,(Placement)lect.getAssignment()));
        			deltaInstructorDistancePreferences += assignedPreference - initialPreference;
        		}
            }
        }
        if (includeZero || iDifferentPlacement!=0.0)
            info.put("Different placement", new Double(weighted?iDifferentPlacement*perts:perts));
        if (includeZero || iAffectedStudentWeight!=0.0)
            info.put("Affected students", new Double(weighted?iAffectedStudentWeight*affectedStudents:affectedStudents));
        if (includeZero || iAffectedInstructorWeight!=0.0)
            info.put("Affected instructors", new Double(weighted?iAffectedInstructorWeight*affectedInstructors:affectedInstructors));
        if (includeZero || iAffectedStudentByTimeWeight!=0.0)
            info.put("Affected students [time]", new Double(weighted?iAffectedStudentByTimeWeight*affectedStudentsByTime:affectedStudentsByTime));
        if (includeZero || iAffectedInstructorByTimeWeight!=0.0)
            info.put("Affected instructors [time]", new Double(weighted?iAffectedInstructorByTimeWeight*affectedInstructorsByTime:affectedInstructorsByTime));
        if (includeZero || iAffectedStudentByRoomWeight!=0.0)
            info.put("Affected students [room]", new Double(weighted?iAffectedStudentByRoomWeight*affectedStudentsByRoom:affectedStudentsByRoom));
        if (includeZero || iAffectedInstructorByRoomWeight!=0.0)
            info.put("Affected instructors [room]", new Double(weighted?iAffectedInstructorByRoomWeight*affectedInstructorsByRoom:affectedInstructorsByRoom));
        if (includeZero || iAffectedStudentByBldgWeight!=0.0)
            info.put("Affected students [bldg]", new Double(weighted?iAffectedStudentByBldgWeight*affectedStudentsByBldg:affectedStudentsByBldg));
        if (includeZero || iAffectedInstructorByBldgWeight!=0.0)
            info.put("Affected instructors [bldg]", new Double(weighted?iAffectedInstructorByBldgWeight*affectedInstructorsByBldg:affectedInstructorsByBldg));
        if (includeZero || iDifferentRoomWeight!=0.0)
            info.put("Different room", new Double(weighted?iDifferentRoomWeight*differentRoom:differentRoom));
        if (includeZero || iDifferentBuildingWeight!=0.0)
            info.put("Different building", new Double(weighted?iDifferentBuildingWeight*differentBuilding:differentBuilding));
        if (includeZero || iDifferentTimeWeight!=0.0)
            info.put("Different time", new Double(weighted?iDifferentTimeWeight*differentTime:differentTime));
        if (includeZero || iDifferentDayWeight!=0.0)
            info.put("Different day", new Double(weighted?iDifferentDayWeight*differentDay:differentDay));
        if (includeZero || iDifferentHourWeight!=0.0)
            info.put("Different hour", new Double(weighted?iDifferentHourWeight*differentHour:differentHour));
        if (includeZero || iTooFarForInstructorsWeight!=0.0)
            info.put("New placement too far for initial [instructors]", new Double(weighted?iTooFarForInstructorsWeight*tooFarForInstructors:tooFarForInstructors));
        if (includeZero || iTooFarForStudentsWeight!=0.0)
            info.put("New placement too far for initial [students]", new Double(weighted?iTooFarForStudentsWeight*tooFarForStudents:tooFarForStudents));
        if (includeZero || iDeltaStudentConflictsWeight!=0.0)
            info.put("Delta student conflicts", new Double(weighted?iDeltaStudentConflictsWeight*deltaStudentConflicts:deltaStudentConflicts));
        if (includeZero || iNewStudentConflictsWeight!=0.0)
            info.put("New student conflicts", new Double(weighted?iNewStudentConflictsWeight*newStudentConflicts:newStudentConflicts));
        if (includeZero || iDeltaTimePreferenceWeight!=0.0)
            info.put("Delta time preferences", new Double(weighted?iDeltaTimePreferenceWeight*deltaTimePreferences:deltaTimePreferences));
        if (includeZero || iDeltaRoomPreferenceWeight!=0.0)
            info.put("Delta room preferences", new Double(weighted?iDeltaRoomPreferenceWeight*deltaRoomPreferences:deltaRoomPreferences));
        if (includeZero || iDeltaInstructorDistancePreferenceWeight!=0.0)
            info.put("Delta instructor distance preferences", new Double(weighted?iDeltaInstructorDistancePreferenceWeight*deltaInstructorDistancePreferences:deltaInstructorDistancePreferences));
        return info;
    }    

    public Hashtable getCompactInfo(Model model, Placement assignedPlacement, boolean includeZero, boolean weighted) {
        Hashtable info = new Hashtable();
        if (!iMPP) return info;
        Lecture lecture = (Lecture)assignedPlacement.variable();
        Placement initialPlacement = (Placement)lecture.getInitialAssignment();
        if (initialPlacement==null || initialPlacement.equals(assignedPlacement)) return info;
        int perts = 1;
        long affectedStudents = lecture.classLimit();
        int affectedInstructors = lecture.getInstructorConstraints().size();
        long affectedStudentsByTime=(initialPlacement.getTimeLocation().equals(assignedPlacement.getTimeLocation())?0:lecture.classLimit());
        int affectedInstructorsByTime=(initialPlacement.getTimeLocation().equals(assignedPlacement.getTimeLocation())?0:lecture.getInstructorConstraints().size());
        
    	int differentRoom = initialPlacement.nrDifferentRooms(assignedPlacement);
        int affectedInstructorsByRoom = differentRoom*lecture.getInstructorConstraints().size();
        long affectedStudentsByRoom = differentRoom*lecture.classLimit();
        
        int differentBuilding = initialPlacement.nrDifferentBuildings(initialPlacement);
        int affectedInstructorsByBldg = differentBuilding*lecture.getInstructorConstraints().size();
        long affectedStudentsByBldg = differentBuilding*lecture.classLimit();
        
        int deltaRoomPreferences = assignedPlacement.sumRoomPreference() - initialPlacement.sumRoomPreference();

        int differentTime=(initialPlacement.getTimeLocation().equals(assignedPlacement.getTimeLocation())?0:1);
        int differentDay=(initialPlacement.getTimeLocation().getDayCode()!=assignedPlacement.getTimeLocation().getDayCode()?1:0);
        int differentHour=(initialPlacement.getTimeLocation().getStartSlot()!=assignedPlacement.getTimeLocation().getStartSlot()?1:0);
        int tooFarForInstructors=0;
        int tooFarForStudents=0;
        int deltaStudentConflicts=lecture.countStudentConflicts(assignedPlacement)-lecture.countInitialStudentConflicts();
        int newStudentConflicts=0;
        double deltaTimePreferences=(assignedPlacement.getTimeLocation().getNormalizedPreference() - initialPlacement.getTimeLocation().getNormalizedPreference());
        int deltaInstructorDistancePreferences=0;
        
        double distance = Placement.getDistance(initialPlacement, assignedPlacement);
        if (!lecture.getInstructorConstraints().isEmpty()) {
            if (distance>0.0 && distance<=5.0) {
                tooFarForInstructors+=lecture.getInstructorConstraints().size();
            } else if (distance>5.0 && distance<=20.0) {
                tooFarForInstructors+=2*lecture.getInstructorConstraints().size();
            } else if (distance>20.0) {
                tooFarForInstructors+=10*lecture.getInstructorConstraints().size();
            }
        }
        if (distance > ((TimetableModel)lecture.getModel()).getStudentDistanceLimit())
        	tooFarForStudents = (int)lecture.classLimit();
        
        Set newStudentConflictsVect = lecture.conflictStudents(assignedPlacement);
        Set initialStudentConflicts = lecture.initialStudentConflicts();
        for (Iterator e=newStudentConflictsVect.iterator();e.hasNext();)
            if (!initialStudentConflicts.contains(e.next())) newStudentConflicts++;


        for (Enumeration e=lecture.getInstructorConstraints().elements();e.hasMoreElements();) {
        	InstructorConstraint ic = (InstructorConstraint)e.nextElement();
        	for (Enumeration f=ic.variables().elements();f.hasMoreElements();) {
        		Lecture lect = (Lecture)f.nextElement();
        		if (lect.equals(lecture)) continue;
        		int initialPreference = (lect.getInitialAssignment()==null?Constants.sPreferenceLevelNeutral:ic.getDistancePreference(initialPlacement,(Placement)lect.getInitialAssignment()));
        		int assignedPreference = (lect.getAssignment()==null?Constants.sPreferenceLevelNeutral:ic.getDistancePreference(assignedPlacement,(Placement)lect.getAssignment()));
        		deltaInstructorDistancePreferences += (assignedPreference - initialPreference);
        	}
        }
    
        if (includeZero || iDifferentPlacement!=0.0)
            info.put("Different placement", new Double(weighted?iDifferentPlacement*perts:perts));
        if (includeZero || iAffectedStudentWeight!=0.0)
            info.put("Affected students", new Double(weighted?iAffectedStudentWeight*affectedStudents:affectedStudents));
        if (includeZero || iAffectedInstructorWeight!=0.0)
            info.put("Affected instructors", new Double(weighted?iAffectedInstructorWeight*affectedInstructors:affectedInstructors));
        if (includeZero || iAffectedStudentByTimeWeight!=0.0)
            info.put("Affected students [time]", new Double(weighted?iAffectedStudentByTimeWeight*affectedStudentsByTime:affectedStudentsByTime));
        if (includeZero || iAffectedInstructorByTimeWeight!=0.0)
            info.put("Affected instructors [time]", new Double(weighted?iAffectedInstructorByTimeWeight*affectedInstructorsByTime:affectedInstructorsByTime));
        if (includeZero || iAffectedStudentByRoomWeight!=0.0)
            info.put("Affected students [room]", new Double(weighted?iAffectedStudentByRoomWeight*affectedStudentsByRoom:affectedStudentsByRoom));
        if (includeZero || iAffectedInstructorByRoomWeight!=0.0)
            info.put("Affected instructors [room]", new Double(weighted?iAffectedInstructorByRoomWeight*affectedInstructorsByRoom:affectedInstructorsByRoom));
        if (includeZero || iAffectedStudentByBldgWeight!=0.0)
            info.put("Affected students [bldg]", new Double(weighted?iAffectedStudentByBldgWeight*affectedStudentsByBldg:affectedStudentsByBldg));
        if (includeZero || iAffectedInstructorByBldgWeight!=0.0)
            info.put("Affected instructors [bldg]", new Double(weighted?iAffectedInstructorByBldgWeight*affectedInstructorsByBldg:affectedInstructorsByBldg));
        if (includeZero || iDifferentRoomWeight!=0.0)
            info.put("Different room", new Double(weighted?iDifferentRoomWeight*differentRoom:differentRoom));
        if (includeZero || iDifferentBuildingWeight!=0.0)
            info.put("Different building", new Double(weighted?iDifferentBuildingWeight*differentBuilding:differentBuilding));
        if (includeZero || iDifferentTimeWeight!=0.0)
            info.put("Different time", new Double(weighted?iDifferentTimeWeight*differentTime:differentTime));
        if (includeZero || iDifferentDayWeight!=0.0)
            info.put("Different day", new Double(weighted?iDifferentDayWeight*differentDay:differentDay));
        if (includeZero || iDifferentHourWeight!=0.0)
            info.put("Different hour", new Double(weighted?iDifferentHourWeight*differentHour:differentHour));
        if (includeZero || iTooFarForInstructorsWeight!=0.0)
            info.put("New placement too far for initial [instructors]", new Double(weighted?iTooFarForInstructorsWeight*tooFarForInstructors:tooFarForInstructors));
        if (includeZero || iTooFarForStudentsWeight!=0.0)
            info.put("New placement too far for initial [students]", new Double(weighted?iTooFarForStudentsWeight*tooFarForStudents:tooFarForStudents));
        if (includeZero || iDeltaStudentConflictsWeight!=0.0)
            info.put("Delta student conflicts", new Double(weighted?iDeltaStudentConflictsWeight*deltaStudentConflicts:deltaStudentConflicts));
        if (includeZero || iNewStudentConflictsWeight!=0.0)
            info.put("New student conflicts", new Double(weighted?iNewStudentConflictsWeight*newStudentConflicts:newStudentConflicts));
        if (includeZero || iDeltaTimePreferenceWeight!=0.0)
            info.put("Delta time preferences", new Double(weighted?iDeltaTimePreferenceWeight*deltaTimePreferences:deltaTimePreferences));
        if (includeZero || iDeltaRoomPreferenceWeight!=0.0)
            info.put("Delta room preferences", new Double(weighted?iDeltaRoomPreferenceWeight*deltaRoomPreferences:deltaRoomPreferences));
        if (includeZero || iDeltaInstructorDistancePreferenceWeight!=0.0)
            info.put("Delta instructor distance preferences", new Double(weighted?iDeltaInstructorDistancePreferenceWeight*deltaInstructorDistancePreferences:deltaInstructorDistancePreferences));
        return info;
    }
}
