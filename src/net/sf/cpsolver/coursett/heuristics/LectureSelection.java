package net.sf.cpsolver.coursett.heuristics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import net.sf.cpsolver.coursett.constraint.GroupConstraint;
import net.sf.cpsolver.coursett.constraint.InstructorConstraint;
import net.sf.cpsolver.coursett.constraint.JenrlConstraint;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.ifs.extension.Extension;
import net.sf.cpsolver.ifs.extension.MacPropagation;
import net.sf.cpsolver.ifs.heuristics.VariableSelection;
import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.EnumerableCollection;
import net.sf.cpsolver.ifs.util.FastVector;
import net.sf.cpsolver.ifs.util.ToolBox;


/**
 * Lecture (variable) selection.
 * <br><br>
 * If there are one or more variables unassigned, the variable selection criterion picks one of them randomly. We have 
 * tried several approaches using domain sizes, number of previous assignments, numbers of constraints in which the 
 * variable participates, etc., but there was no significant improvement in this timetabling problem towards the random 
 * selection of an unassigned variable. The reason is, that it is easy to go back when a wrong variable is picked - 
 * such a variable is unassigned when there is a conflict with it in some of the subsequent iterations. 
 * <br><br>
 * When all variables are assigned, an evaluation is made for each variable according to the above described weights. The 
 * variable with the worst evaluation is selected. This variable promises the best improvement in optimization.
 * <br><br>
 * Parameters (selection among unassigned lectures):
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>Lecture.RouletteWheelSelection</td><td>{@link Boolean}</td><td>Roulette wheel selection</td></tr>
 * <tr><td>Lecture.RandomWalkProb</td><td>{@link Double}</td><td>Random walk probability</td></tr>
 * <tr><td>Lecture.DomainSizeWeight</td><td>{@link Double}</td><td>Domain size weight</td></tr>
 * <tr><td>Lecture.NrAssignmentsWeight</td><td>{@link Double}</td><td>Number of assignments weight</td></tr>
 * <tr><td>Lecture.InitialAssignmentWeight</td><td>{@link Double}</td><td>Initial assignment weight</td></tr>
 * <tr><td>Lecture.NrConstraintsWeight</td><td>{@link Double}</td><td>Number of constraint weight</td></tr>
 * </table>
 * <br>
 * Parameters (selection among assigned lectures, when the solution is complete):
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>Lecture.HardStudentConflictWeight</td><td>{@link Double}</td><td>Hard student conflict weight</td></tr>
 * <tr><td>Lecture.StudentConflictWeight</td><td>{@link Double}</td><td>Student conflict weight</td></tr>
 * <tr><td>Lecture.TimePreferenceWeight</td><td>{@link Double}</td><td>Time preference weight</td></tr>
 * <tr><td>Lecture.ContrPreferenceWeight</td><td>{@link Double}</td><td>Group constraint preference weight</td></tr>
 * <tr><td>Lecture.RoomPreferenceWeight</td><td>{@link Double}</td><td>Room preference weight</td></tr>
 * <tr><td>Lecture.UselessSlotWeight</td><td>{@link Double}</td><td>Useless slot weight</td></tr>
 * <tr><td>Lecture.TooBigRoomWeight</td><td>{@link Double}</td><td>Too big room weight</td></tr>
 * <tr><td>Lecture.DistanceInstructorPreferenceWeight</td><td>{@link Double}</td><td>Distance (of the rooms of the back-to-back classes) based instructor preferences weight</td></tr>
 * <tr><td>Lecture.DeptSpreadPenaltyWeight</td><td>{@link Double}</td><td>Department balancing penalty (see {@link net.sf.cpsolver.coursett.constraint.DepartmentSpreadConstraint})</td></tr>
 * </table>
 * <br>
 * Parameters (selection among subset of lectures (faster)):
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>Lecture.SelectionSubSet</td><td>{@link Boolean}</td><td>Selection among subset of lectures (faster)</td></tr>
 * <tr><td>Lecture.SelectionSubSetMinSize</td><td>{@link Double}</td><td>Minimal subset size</td></tr>
 * <tr><td>Lecture.SelectionSubSetPart</td><td>{@link Double}</td><td>Subset size in percentage of all lectures available for selection</td></tr>
 * </table>
 * 
 * @see PlacementSelection
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
public class LectureSelection implements VariableSelection {
    private double iRandomWalkProb;
    private double iDomainSizeWeight;
    private double iGoodValuesWeight;
    private double iNrAssignmentsWeight;
    private double iConstraintsWeight;
    private double iInitialAssignmentWeight;
    private boolean iRouletteWheelSelection;
    private boolean iUnassignWhenNotGood;
    
    private double iEmptySingleSlotWeight;
    private double iTooBigRoomWeight;
    private double iTimePreferencesWeight;
    private double iStudentConflictWeight;
    private double iRoomPreferencesWeight;
    private double iConstrPreferencesWeight;
    private double iHardStudentConflictWeight;
    private double iCommitedStudentConflictWeight;
    private double iDistanceInstructorPreferenceWeight;
    private double iDeptSpreadPenaltyWeight;
    private double iSpreadPenaltyWeight;
    
    private boolean iSubSetSelection;
    private double iSelectionSubSetPart;
    private int iSelectionSubSetMinSize;
    private boolean iInteractiveMode;
    
    private boolean iRW = false;
    private boolean iMPP = false;
    private boolean iSwitchStudents = false;
    
    private MacPropagation iProp = null;

    private int       iTabuSize                  = 0;
    private ArrayList iTabu                      = null;
    private int       iTabuPos                   = 0;
    
    private int iVariableChanceIteration = 1000;
    private double iVariableChanceProb = 0.05;

    public LectureSelection(DataProperties properties) {
        iRouletteWheelSelection  = properties.getPropertyBoolean("Lecture.RouletteWheelSelection",true);
        iUnassignWhenNotGood     = properties.getPropertyBoolean("Lecture.UnassignWhenNotGood",false);
        iRW                      = properties.getPropertyBoolean("General.RandomWalk", true);
        iRandomWalkProb          = (!iRW?0.0:properties.getPropertyDouble("Lecture.RandomWalkProb",1.00));
        iGoodValuesWeight        = properties.getPropertyDouble("Lecture.GoodValueProb",1.0);
        iDomainSizeWeight        = properties.getPropertyDouble("Lecture.DomainSizeWeight",30.0);
        
        iInteractiveMode         = properties.getPropertyBoolean("General.InteractiveMode", false);
        
        iNrAssignmentsWeight     = properties.getPropertyDouble("Lecture.NrAssignmentsWeight",10.0);
        iConstraintsWeight       = properties.getPropertyDouble("Lecture.NrConstraintsWeight",0.0);
        iMPP                     = properties.getPropertyBoolean("General.MPP", false);
        iInitialAssignmentWeight = (!iMPP?0.0:properties.getPropertyDouble("Lecture.InitialAssignmentWeight",20.0));

        iEmptySingleSlotWeight   = properties.getPropertyDouble("Lecture.UselessSlotWeight",properties.getPropertyDouble("Comparator.UselessSlotWeight",0.0));
        iTooBigRoomWeight        = properties.getPropertyDouble("Lecture.TooBigRoomWeight",properties.getPropertyDouble("Comparator.TooBigRoomWeight",0.0));
        iTimePreferencesWeight   = properties.getPropertyDouble("Lecture.TimePreferenceWeight",properties.getPropertyDouble("Comparator.TimePreferenceWeight",1.0));
        iStudentConflictWeight   = properties.getPropertyDouble("Lecture.StudentConflictWeight",properties.getPropertyDouble("Comparator.StudentConflictWeight",0.2));
        iCommitedStudentConflictWeight   = properties.getPropertyDouble("Lecture.CommitedStudentConflictWeight",properties.getPropertyDouble("Comparator.CommitedStudentConflictWeight",1.0));
        iRoomPreferencesWeight   = properties.getPropertyDouble("Lecture.RoomPreferenceWeight",properties.getPropertyDouble("Comparator.RoomPreferenceWeight",0.1));
        iConstrPreferencesWeight = properties.getPropertyDouble("Lecture.ContrPreferenceWeight",properties.getPropertyDouble("Comparator.ContrPreferenceWeight",1.0));
        
        iSwitchStudents            = properties.getPropertyBoolean("General.SwitchStudents",true);
        iHardStudentConflictWeight = (!iSwitchStudents?0.0:properties.getPropertyDouble("Lecture.HardStudentConflictWeight",properties.getPropertyDouble("Comparator.HardStudentConflictWeight",1.0)));
        iDistanceInstructorPreferenceWeight = properties.getPropertyDouble("Lecture.DistanceInstructorPreferenceWeight",properties.getPropertyDouble("Comparator.DistanceInstructorPreferenceWeight",1.0));
        iDeptSpreadPenaltyWeight = properties.getPropertyDouble("Lecture.DeptSpreadPenaltyWeight",properties.getPropertyDouble("Comparator.DeptSpreadPenaltyWeight",1.0));
        iSpreadPenaltyWeight = properties.getPropertyDouble("Lecture.SpreadPenaltyWeight",properties.getPropertyDouble("Comparator.DeptSpreadPenaltyWeight",1.0));
        
        iSubSetSelection           = properties.getPropertyBoolean("Lecture.SelectionSubSet",true);
        iSelectionSubSetMinSize    = properties.getPropertyInt("Lecture.SelectionSubSetMinSize",10);
        iSelectionSubSetPart       = properties.getPropertyDouble("Lecture.SelectionSubSetPart", 0.2);
        
        iTabuSize    			   = properties.getPropertyInt("Lecture.TabuSize",20);
        if (iTabuSize>0) iTabu=new ArrayList(iTabuSize);
        
        iVariableChanceIteration = properties.getPropertyInt("Lecture.VariableChanceIteration",1000);
        iVariableChanceProb = properties.getPropertyDouble("Lecture.VariableChanceProb", 0.05);
    }
    
    public static Collection parameters() {
        Vector ret = new FastVector();
        
        //ParameterDefinition.Dependency mppDep = new ParameterDefinition.Dependency("General.MPP","true");
        //ParameterDefinition.Dependency rwDep = new ParameterDefinition.Dependency("General.RandomWalk","true");
        //ParameterDefinition.Dependency propDep = new ParameterDefinition.Dependency("General.MAC","true");
        //ParameterDefinition.Dependency swDep = new ParameterDefinition.Dependency("General.SwitchStudents","true");
        //ParameterDefinition.Dependency deptSpreadDep = new ParameterDefinition.Dependency("General.DeptBalancing","false");
        
        //ret.addElement(new ParameterDefinition("Lecture Selection","Lecture.RouletteWheelSelection", "Roulette-wheel selection", ParameterDefinition.TYPE_BOOLEAN, "true"));
        //ret.addElement(new ParameterDefinition("Lecture Selection","Lecture.UnassignWhenNotGood", "Unassign when no good", ParameterDefinition.TYPE_BOOLEAN, "false").addDependency(propDep));
        //ret.addElement(new ParameterDefinition("Lecture Selection","Lecture.RandomWalkProb", "Random walk probability", ParameterDefinition.TYPE_DOUBLE, "0.02").addDependency(rwDep));
        //ret.addElement(new ParameterDefinition("Lecture Selection","Lecture.GoodValueProb", "Good value selection", ParameterDefinition.TYPE_DOUBLE, "1.00").addDependency(propDep));

        //ret.addElement(new ParameterDefinition("Lecture Selection - Weights","Lecture.DomainSizeWeight", "Domain size", ParameterDefinition.TYPE_DOUBLE, "30.0"));
        //ret.addElement(new ParameterDefinition("Lecture Selection - Weights","Lecture.NrAssignmentsWeight", "Number of assignments", ParameterDefinition.TYPE_DOUBLE, "10.0"));
        //ret.addElement(new ParameterDefinition("Lecture Selection - Weights","Lecture.NrConstraintsWeight", "Number of constraints", ParameterDefinition.TYPE_DOUBLE, "0.0"));
        //ret.addElement(new ParameterDefinition("Lecture Selection - Weights","Lecture.InitialAssignmentWeight", "Initial assignment", ParameterDefinition.TYPE_DOUBLE, "20.0").addDependency(mppDep));
        
        //ret.addElement(new ParameterDefinition("Lecture Selection - Weights (a complete solution found)","Lecture.UselessSlotWeight", "Useless slots", ParameterDefinition.TYPE_DOUBLE, "0.00"));
        //ret.addElement(new ParameterDefinition("Lecture Selection - Weights (a complete solution found)","Lecture.TimePreferenceWeight", "Time preferences", ParameterDefinition.TYPE_DOUBLE, "1.00"));
        //ret.addElement(new ParameterDefinition("Lecture Selection - Weights (a complete solution found)","Lecture.StudentConflictWeight", "Student conflicts", ParameterDefinition.TYPE_DOUBLE, "0.10"));
        //ret.addElement(new ParameterDefinition("Lecture Selection - Weights (a complete solution found)","Lecture.RoomPreferenceWeight", "Room preferences", ParameterDefinition.TYPE_DOUBLE, "0.10"));
        //ret.addElement(new ParameterDefinition("Lecture Selection - Weights (a complete solution found)","Lecture.ContrPreferenceWeight", "Group constraint preferences", ParameterDefinition.TYPE_DOUBLE, "1.00"));
        //ret.addElement(new ParameterDefinition("Lecture Selection - Weights (a complete solution found)","Lecture.HardStudentConflictWeight", "Hard student conflicts", ParameterDefinition.TYPE_DOUBLE, "1.00"));
        
        //ret.addElement(new ParameterDefinition("Lecture Selection - Weights (a complete solution found)","Lecture.DistanceInstructorPreferenceWeight", "Distance Instructor Preference", ParameterDefinition.TYPE_DOUBLE, "0.10"));
        //ret.addElement(new ParameterDefinition("Lecture Selection - Weights (a complete solution found)","Lecture.DeptSpreadPenaltyWeight", "Deparment balancing -- penalty a slot over initial allowance", ParameterDefinition.TYPE_DOUBLE, "0.0").addDependency(deptSpreadDep));

        //ret.addElement(new ParameterDefinition("Lecture Selection","Lecture.SelectionSubSet", "Subset selection", ParameterDefinition.TYPE_BOOLEAN, "true"));
        //ParameterDefinition.Dependency subSetDep = new ParameterDefinition.Dependency("Lecture.SelectionSubSet", "true");
        //ret.addElement(new ParameterDefinition("Lecture Selection","Lecture.SelectionSubSetMinSize", "Subset selection -- subset minimum size", ParameterDefinition.TYPE_INTEGER, "10").addDependency(subSetDep));
        //ret.addElement(new ParameterDefinition("Lecture Selection","Lecture.SelectionSubSetPart", "Subset selection -- part", ParameterDefinition.TYPE_DOUBLE, "0.20").addDependency(subSetDep));

        return ret;
    }

    public void init(Solver solver) {
        for (Enumeration i=solver.getExtensions().elements();i.hasMoreElements();) {
            Extension extension = (Extension)i.nextElement();
            if (extension instanceof MacPropagation)
                iProp = (MacPropagation)extension;
        }
    }

    public Variable selectVariable(Solution solution) {
    	EnumerableCollection unassignedVariables = solution.getModel().unassignedVariables();
    	if (iInteractiveMode) {
    		//remove variables that have no values
    		unassignedVariables = new FastVector(unassignedVariables.size());
    		for (Enumeration e=solution.getModel().unassignedVariables().elements();e.hasMoreElements();) {
    			Variable variable = (Variable)e.nextElement();
    			if (!variable.values().isEmpty()) unassignedVariables.addElement(variable);
    		}
    	}
    	
        if (unassignedVariables.isEmpty()) {
            EnumerableCollection variables = solution.getModel().perturbVariables();
            if (variables.isEmpty()) variables = solution.getModel().assignedVariables();
            
            if (iRW && ToolBox.random()<=iRandomWalkProb) return (Variable)ToolBox.random(variables);
            
            Vector selectionVariables = null;
            int worstTimePreference = 0;
            int worstRoomConstrPreference = 0;
            
            for (Iterator i1=(iSubSetSelection?ToolBox.subSet(variables,iSelectionSubSetPart,iSelectionSubSetMinSize):variables).iterator();i1.hasNext();) {
                Variable selectedVariable = (Variable) i1.next();
                
                if (iTabu!=null && iTabu.contains(selectedVariable)) continue;
                
                Value value = (Value)selectedVariable.getAssignment();
                
                int sumStudentConflicts = (iStudentConflictWeight!=0.0?((Lecture)selectedVariable).countStudentConflicts(value):0);
                boolean haveAlternative = !((Lecture)selectedVariable).isSingleSection();
                int constrPreference = 0;
                int emptySingleHalfHours = 0;
                int sumHardStudentConflicts = 0;
                int distanceInstructorPreferences = 0;
                int tooBig = ((Placement)value).getTooBigRoomPreference();
                int sumCommitedStudentConflicts = 0;
                
                for (Enumeration i2=selectedVariable.constraints().elements();i2.hasMoreElements();) {
                    Constraint constraint = (Constraint)i2.nextElement();
                    if (iCommitedStudentConflictWeight!=0.0) {
                    	sumCommitedStudentConflicts += ((Lecture)selectedVariable).getCommitedConflicts((Placement)value);
                    }
                    if (iDistanceInstructorPreferenceWeight!=0.0 && constraint instanceof InstructorConstraint) {
                        distanceInstructorPreferences += ((InstructorConstraint)constraint).getPreference(value);
                    }
                    if (iHardStudentConflictWeight!=0.0 && !haveAlternative && constraint instanceof JenrlConstraint) {
                        JenrlConstraint jenrl = (JenrlConstraint)constraint;
                        if (jenrl.areStudentConflictsHard())
                            sumHardStudentConflicts += ((JenrlConstraint)constraint).jenrl(selectedVariable, value);
                    } else if (iConstrPreferencesWeight!=0.0 && constraint instanceof GroupConstraint) {
                        GroupConstraint gc = (GroupConstraint)constraint;
                        constrPreference += gc.getCurrentPreference();
                    }
                }
                
                if (iEmptySingleSlotWeight!=0.0) {
                	emptySingleHalfHours = ((Placement)value).nrUselessHalfHours();
                }
                
                int roomPreference = ((Placement)value).sumRoomPreference();
                double deptSpreadPenalty = (iDeptSpreadPenaltyWeight==0.0 || ((Lecture)selectedVariable).getDeptSpreadConstraint()==null?0:((Lecture)selectedVariable).getDeptSpreadConstraint().getPenalty());
                double spreadPenalty = (iSpreadPenaltyWeight==0.0 ? 0.0 : ((Lecture)selectedVariable).getSpreadPenalty());
                int timePreference = (int)(100.0*(
                    (iStudentConflictWeight*sumStudentConflicts)+
                    (iHardStudentConflictWeight*sumHardStudentConflicts)+
                    (iTimePreferencesWeight*((Placement)value).getTimeLocation().getNormalizedPreference())+
                    (iEmptySingleSlotWeight*emptySingleHalfHours)+
                    (iRoomPreferencesWeight*roomPreference)+
                    (iTooBigRoomWeight*tooBig)+
                    (iConstrPreferencesWeight*constrPreference)+
                    (iDistanceInstructorPreferenceWeight*distanceInstructorPreferences)+
                    (iDeptSpreadPenaltyWeight*deptSpreadPenalty)+
                    (iSpreadPenaltyWeight*spreadPenalty)+
                    (iCommitedStudentConflictWeight*sumCommitedStudentConflicts)
                ));

                if (selectionVariables==null || timePreference>worstTimePreference || (timePreference==worstTimePreference && roomPreference+constrPreference>worstRoomConstrPreference)) {
                    if (selectionVariables==null) selectionVariables=new FastVector(); else selectionVariables.clear();
                    selectionVariables.addElement(selectedVariable);
                    worstTimePreference=timePreference;
                    worstRoomConstrPreference=roomPreference+constrPreference;
                } else if (timePreference==worstTimePreference && roomPreference+constrPreference==worstRoomConstrPreference) {
                    selectionVariables.addElement(selectedVariable);
                }
            }
            
            Variable selectedVariable = (Variable)ToolBox.random(selectionVariables);
            
            if (selectedVariable==null)
            	selectedVariable = (Variable)ToolBox.random(variables);
            
            if (selectedVariable!=null && iTabu!=null) {
                if (iTabu.size()==iTabuPos)
                    iTabu.add(selectedVariable);
                else
                    iTabu.set(iTabuPos, selectedVariable);
                iTabuPos = (iTabuPos + 1) % iTabuSize;
            }
            
            return selectedVariable;
        } else {
        	if (iVariableChanceIteration>0) {
        		Vector variablesWithChance = new Vector(unassignedVariables.size());
        		for (Enumeration e=unassignedVariables.elements();e.hasMoreElements();) {
        			Variable v = (Variable)e.nextElement();
        			if (v.countAssignments()>iVariableChanceIteration) continue;
        			variablesWithChance.add(v);
        		}
        	
        		if (variablesWithChance.isEmpty() && ToolBox.random()<=iVariableChanceProb && !solution.getModel().assignedVariables().isEmpty())
        			return (Variable)ToolBox.random(solution.getModel().assignedVariables());
        	
        		if (ToolBox.random()<=iRandomWalkProb) {
        			if (!variablesWithChance.isEmpty())
        				return (Variable)ToolBox.random(variablesWithChance);
        			else
        				return (Variable)ToolBox.random(unassignedVariables);
        		}
        	} else {
        		if (ToolBox.random()<=iRandomWalkProb) return (Variable)ToolBox.random(unassignedVariables);
        	}
            
            if (iProp!=null && iUnassignWhenNotGood) {
                Vector noGoodVariables = new FastVector();
                for (Iterator i1=ToolBox.subSet(unassignedVariables,iSelectionSubSetPart,iSelectionSubSetMinSize).iterator();i1.hasNext(); ){
                    Variable variable = (Variable) i1.next();
                    if (iProp.goodValues(variable).isEmpty())
                        noGoodVariables.addElement(variable);
                }
                if (!noGoodVariables.isEmpty()) {
                    if (ToolBox.random()<0.02) return (Variable)ToolBox.random(solution.getModel().assignedVariables());
                    for (int attempt=0;attempt<10;attempt++) {
                        Variable noGoodVariable = (Variable)ToolBox.random(noGoodVariables);
                        Value noGoodValue = (Value)ToolBox.random(noGoodVariable.values());
                        if (!iProp.noGood(noGoodValue).isEmpty()) return ((Value)ToolBox.random(iProp.noGood(noGoodValue))).variable();
                    }
                }
            }
                
            if (iRouletteWheelSelection) {
                int iMaxDomainSize=0;
                int iMaxGoodDomainSize=0;
                int iMaxConstraints=0;
                long iMaxNrAssignments=0;
                Collection variables = (iSubSetSelection?ToolBox.subSet(unassignedVariables,iSelectionSubSetPart,iSelectionSubSetMinSize):unassignedVariables);
                for (Iterator i=variables.iterator();i.hasNext(); ){
                    Variable variable = (Variable) i.next();
                    
                    if (iTabu!=null && iTabu.contains(variable)) continue;
                    
                    iMaxDomainSize=Math.max(iMaxDomainSize,variable.values().size());
                    iMaxGoodDomainSize=(iProp==null?0:Math.max(iMaxGoodDomainSize,iProp.goodValues(variable).size()));
                    iMaxConstraints=Math.max(iMaxConstraints,variable.constraints().size());
                    iMaxNrAssignments=Math.max(iMaxNrAssignments,variable.countAssignments());
                }

                Vector points = new FastVector();
                int totalPoints = 0;
                
                for (Iterator i=variables.iterator();i.hasNext(); ){
                    Variable variable = (Variable) i.next();
                    
                    long pointsThisVariable = Math.round(
                        iDomainSizeWeight*(((double)(iMaxDomainSize-variable.values().size()))/((double)iMaxDomainSize))+
                        (iProp==null?0.0:iGoodValuesWeight*(((double)(iMaxGoodDomainSize-iProp.goodValues(variable).size()))/((double)iMaxGoodDomainSize)))+
                        iNrAssignmentsWeight*(((double)variable.countAssignments())/((double)iMaxNrAssignments))+
                        iConstraintsWeight*(((double)(iMaxConstraints-variable.constraints().size()))/((double)iMaxConstraints))+
                        iInitialAssignmentWeight*(variable.getInitialAssignment()!=null?solution.getModel().conflictValues(variable.getInitialAssignment()).size():0.0)
                        );
                    if (pointsThisVariable>0) {
                        totalPoints += pointsThisVariable;
                        points.addElement(new Integer(totalPoints));
                    }
                }
                
                if (totalPoints>0) {
                    int rndPoints = ToolBox.random(totalPoints);
                    Iterator x=variables.iterator();
                    for (int i=0;x.hasNext() && i<points.size();i++){
                        Variable variable = (Variable)x.next();
                        int tp = ((Integer)points.elementAt(i)).intValue();
                        if (tp>rndPoints) {
                            if (variable!=null && iTabu!=null) {
                                if (iTabu.size()==iTabuPos)
                                    iTabu.add(variable);
                                else
                                    iTabu.set(iTabuPos, variable);
                                iTabuPos = (iTabuPos + 1) % iTabuSize;
                            }
                        	return variable;
                        }
                    }
                }
                
            } else {
                
                Vector selectionVariables = null;
                long bestGood = 0;
                for (Iterator i=ToolBox.subSet(unassignedVariables,iSelectionSubSetPart,iSelectionSubSetMinSize).iterator();i.hasNext(); ){
                    Variable variable = (Variable) i.next();
                    
                    if (iTabu!=null && iTabu.contains(variable)) continue;
                    
                    long good = (long)(
                    iDomainSizeWeight*variable.values().size()
                    + iGoodValuesWeight*(iProp==null?0:iProp.goodValues(variable).size())
                    + iNrAssignmentsWeight*variable.countAssignments()
                    + iConstraintsWeight*variable.constraints().size()
                    + iInitialAssignmentWeight*(variable.getInitialAssignment()!=null?solution.getModel().conflictValues(variable.getInitialAssignment()).size():0.0)
                    );
                    if (selectionVariables==null || bestGood>good) {
                        if (selectionVariables==null) selectionVariables = new FastVector(); else selectionVariables.clear();
                        bestGood=good;
                        selectionVariables.addElement(variable);
                    } else if (good==bestGood) {
                        selectionVariables.addElement(variable);
                    }
                }
                
                if (!selectionVariables.isEmpty()) {
                	Variable selectedVariable = (Variable)ToolBox.random(selectionVariables);

                    if (selectedVariable!=null && iTabu!=null) {
                        if (iTabu.size()==iTabuPos)
                            iTabu.add(selectedVariable);
                        else
                            iTabu.set(iTabuPos, selectedVariable);
                        iTabuPos = (iTabuPos + 1) % iTabuSize;
                    }
                    
                    return selectedVariable;
                }
            }
            
            Variable selectedVariable = (Variable)ToolBox.random(unassignedVariables);
            
            if (selectedVariable!=null && iTabu!=null) {
                if (iTabu.size()==iTabuPos)
                    iTabu.add(selectedVariable);
                else
                    iTabu.set(iTabuPos, selectedVariable);
                iTabuPos = (iTabuPos + 1) % iTabuSize;
            }
            
            return selectedVariable;
        }
    }
    
}
