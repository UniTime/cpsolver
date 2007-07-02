package net.sf.cpsolver.coursett.heuristics;


import java.util.*;

import net.sf.cpsolver.coursett.constraint.*;
import net.sf.cpsolver.coursett.model.*;
import net.sf.cpsolver.ifs.extension.*;
import net.sf.cpsolver.ifs.heuristics.*;
import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.perturbations.*;
import net.sf.cpsolver.ifs.solution.*;
import net.sf.cpsolver.ifs.solver.*;
import net.sf.cpsolver.ifs.util.*;

/**
 * Placement (value) selection.
 * <br><br>
 * We have implemented a hierarchical handling of the value selection criteria (see {@link HeuristicSelector}).
 * <br><br>
 * The value selection heuristics also allow for random selection of a value with a given probability 
 * (random walk, e.g., 2%) and, in the case of MPP, to select the initial value (if it exists) with a given probability (e.g., 70%).
 * <br><br>
 * Parameters (general):
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>Placement.RandomWalkProb</td><td>{@link Double}</td><td>Random walk probability</td></tr>
 * <tr><td>Placement.GoodSelectionProb</td><td>{@link Double}</td><td>Good value (not removed from domain) selection probability (MAC related)</td></tr>
 * <tr><td>Placement.TabuLength</td><td>{@link Integer}</td><td>Tabu-list length (-1 means do not use tabu-list)</td></tr>
 * <tr><td>Placement.MPP_InitialProb</td><td>{@link Double}</td><td>MPP initial selection probability </td></tr>
 * <tr><td>Placement.MPP_Limit</td><td>{@link Integer}</td><td>MPP: limit on the number of perturbations (-1 for no limit)</td></tr>
 * <tr><td>Placement.MPP_PenaltyLimit</td><td>{@link Double}</td><td>MPP: limit on the perturbations penalty (-1 for no limit)</td></tr>
 * </table>
 * <br>
 * Parameters (for each level of selection):
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>Placement.NrAssignmentsWeight1<br>Placement.NrAssignmentsWeight2<br>Placement.NrAssignmentsWeight3</td><td>{@link Double}</td><td>Number of previous assignments of the value weight</td></tr>
 * <tr><td>Placement.NrConflictsWeight1,2,3</td><td>{@link Double}</td><td>Number of conflicts weight</td></tr>
 * <tr><td>Placement.WeightedConflictsWeight1,2,3</td><td>{@link Double}</td><td>Weighted conflicts weight (Conflict-based Statistics related)</td></tr>
 * <tr><td>Placement.NrPotentialConflictsWeight1,2,3</td><td>{@link Double}</td><td>Number of potential conflicts weight (Conflict-based Statistics related)</td></tr>
 * <tr><td>Placement.MPP_DeltaInitialAssignmentWeight1,2,3</td><td>{@link Double}</td><td>Delta initial assigments weight (MPP, violated initials related)</td></tr>
 * <tr><td>Placement.NrHardStudConfsWeight1,2,3</td><td>{@link Double}</td><td>Hard student conflicts weight (student conflicts between single-section classes)</td></tr>
 * <tr><td>Placement.NrStudConfsWeight1,2,3</td><td>{@link Double}</td><td>Student conflicts weight</td></tr>
 * <tr><td>Placement.TimePreferenceWeight1,2,3</td><td>{@link Double}</td><td>Time preference weight</td></tr>
 * <tr><td>Placement.DeltaTimePreferenceWeight1,2,3</td><td>{@link Double}</td><td>Time preference delta weight (difference between before and after assignemnt of the value)</td></tr>
 * <tr><td>Placement.ConstrPreferenceWeight1,2,3</td><td>{@link Double}</td><td>Constraint preference weight</td></tr>
 * <tr><td>Placement.RoomPreferenceWeight1,2,3</td><td>{@link Double}</td><td>Room preference weight</td></tr>
 * <tr><td>Placement.UselessSlotsWeight1,2,3</td><td>{@link Double}</td><td>Useless slot weight</td></tr>
 * <tr><td>Placement.TooBigRoomWeight1,2,3</td><td>{@link Double}</td><td>Too big room weight</td></tr>
 * <tr><td>Placement.DistanceInstructorPreferenceWeight1,2,3</td><td>{@link Double}</td><td>Distance (of the rooms of the back-to-back classes) based instructor preferences weight</td></tr>
 * <tr><td>Placement.DeptSpreadPenaltyWeight1,2,3</td><td>{@link Double}</td><td>Department spreading: penalty of when a slot over initial allowance is used</td></tr>
 * <tr><td>Placement.ThresholdKoef1,2</td><td>{@link Double}</td><td>Threshold koeficient of the level</td></tr>
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

public class PlacementSelection implements ValueSelection {
    static final int NR_LEVELS = 3;
    private static final double PRECISION = 1.0;
    private static final boolean USE_THRESHOLD = true;
    private boolean iUseThreshold = USE_THRESHOLD;
    
    private double iGoodSelectionProb;
    public static final String GOOD_SELECTION_PROB = "Placement.GoodSelectionProb";
    private double iRandomWalkProb;
    public static final String RW_SELECTION_PROB = "Placement.RandomWalkProb";
    private double iInitialSelectionProb;
    public static final String INITIAL_SELECTION_PROB = "Placement.MPP_InitialProb";
    private int iMPPLimit;
    public static final String NR_MPP_LIMIT = "Placement.MPP_Limit";
    private double iMPPPenaltyLimit;
    public static final String NR_MPP_PENALTY_LIMIT = "Placement.MPP_PenaltyLimit";
    
    private double[] iNrConflictsWeight = new double[NR_LEVELS];
    public static final String NR_CONFLICTS_WEIGHT = "Placement.NrConflictsWeight";
    private double[] iNrPotentialConflictsWeight = new double[NR_LEVELS];
    public static final String NR_POTENTIAL_CONFLICTS_WEIGHT = "Placement.NrPotentialConflictsWeight";
    private double[] iNrWeightedConflictsWeight = new double[NR_LEVELS];
    public static final String WEIGHTED_CONFLICTS_WEIGHT = "Placement.WeightedConflictsWeight";
    private double[] iDeltaTimePreferenceWeight = new double[NR_LEVELS];
    public static final String DELTA_TIME_PREFERENCE_WEIGHT = "Placement.DeltaTimePreferenceWeight";
    private double[] iPerturbationPenaltyWeight = new double[NR_LEVELS];
    public static final String DELTA_INITIAL_ASSIGNMENT_WEIGHT = "Placement.MPP_DeltaInitialAssignmentWeight";
    private double[] iNrStudentConflictsWeight = new double[NR_LEVELS];
    public static final String NR_STUDENT_CONF_WEIGHT = "Placement.NrStudConfsWeight";
    private double[] iNrHardStudentConflictsWeight = new double[NR_LEVELS];
    public static final String NR_HARD_STUDENT_CONF_WEIGHT = "Placement.NrHardStudConfsWeight";
    private double[] iNrCommitedStudentConflictsWeight = new double[NR_LEVELS];
    public static final String NR_COMMITED_STUDENT_CONF_WEIGHT = "Placement.NrCommitedStudConfsWeight";
    private double[] iUselessSlotsWeight = new double[NR_LEVELS];
    public static final String USELESS_SLOTS_WEIGHT = "Placement.UselessSlotsWeight";
    private double[] iSumConstrPreferencesWeight = new double[NR_LEVELS];
    public static final String SUM_CONSTR_PREFERENCE_WEIGHT = "Placement.ConstrPreferenceWeight";
    private double[] iSumRoomPreferencesWeight = new double[NR_LEVELS];
    public static final String SUM_ROOM_PREFERENCE_WEIGHT = "Placement.RoomPreferenceWeight";
    private double[] iSumTimePreferencesWeight = new double[NR_LEVELS];
    public static final String SUM_TIME_PREFERENCE_WEIGHT = "Placement.TimePreferenceWeight";
    private double[] iNrAssignmentsWeight = new double[NR_LEVELS];
    public static final String NR_ASSIGNMENTS_WEIGHT = "Placement.NrAssignmentsWeight";
    private double[] iThresholdKoef = new double[NR_LEVELS];
    public static final String NR_THRESHOLD_KOEF = "Placement.ThresholdKoef";
    private double[] iTooBigRoomWeight = new double[NR_LEVELS];
    public static final String TOO_BIG_ROOM_WEIGHT = "Placement.TooBigRoomWeight";
    private double[] iDeptSpreadWeight = new double[NR_LEVELS];
    public static final String DEPT_SPREAD_WEIGHT = "Placement.DeptSpreadPenaltyWeight";
    private double[] iDistanceInstructorPreferenceWeight = new double[NR_LEVELS];
    public static final String DISTANCE_INSTRUCTOR_PREFERENCE_WEIGHT = "Placement.DistanceInstructorPreferenceWeight";
    private double[] iSpreadWeight = new double[NR_LEVELS];
    public static final String SPREAD_WEIGHT = "Placement.SpreadPenaltyWeight";

    private int       iTabuSize                  = 0;
    private ArrayList iTabu                      = null;
    private int       iTabuPos                   = 0;
    public static final String TABU_LENGTH       = "Placement.TabuLength";

    private ConflictStatistics iStat = null;
    private MacPropagation iProp = null;
    private ViolatedInitials iViolatedInitials = null;
    private PerturbationsCounter iPerturbationsCounter = null;
    
    private boolean iRW = false;
    private boolean iMPP = false;
    private boolean iSW = false;
    
    private boolean iCanUnassingSingleton = false;

    public void init(Solver solver) {
        for (Enumeration i=solver.getExtensions().elements();i.hasMoreElements();) {
            Extension extension = (Extension)i.nextElement();
            if (extension instanceof ConflictStatistics) 
                iStat = (ConflictStatistics) extension;
            if (extension instanceof MacPropagation)
                iProp = (MacPropagation)extension;
            if (extension instanceof ViolatedInitials)
                iViolatedInitials = (ViolatedInitials)extension;
        }
        iPerturbationsCounter = solver.getPerturbationsCounter();
    }

    public PlacementSelection(DataProperties properties) {
        iMPP                     = properties.getPropertyBoolean("General.MPP", false);
        iRW                      = properties.getPropertyBoolean("General.RandomWalk", true);
        iSW                      = properties.getPropertyBoolean("General.SwitchStudents",true);
        iCanUnassingSingleton    = properties.getPropertyBoolean("Placement.CanUnassingSingleton", iCanUnassingSingleton);
        boolean autoconfigure = properties.getPropertyBoolean("Placement.AutoConfigure", false);
        iRandomWalkProb = (iRW?properties.getPropertyDouble(RW_SELECTION_PROB,0.00):0.0);
        iGoodSelectionProb = properties.getPropertyDouble(GOOD_SELECTION_PROB,1.00);
        iInitialSelectionProb = (iMPP?properties.getPropertyDouble(INITIAL_SELECTION_PROB, 0.75):0.0);
        iMPPLimit = (iMPP?properties.getPropertyInt(NR_MPP_LIMIT, -1):-1);
        iMPPPenaltyLimit = (iMPP?properties.getPropertyDouble(NR_MPP_PENALTY_LIMIT, -1.0):-1.0);
        iTabuSize = properties.getPropertyInt(TABU_LENGTH, -1);
        if (iTabuSize>0) iTabu=new ArrayList(iTabuSize);
        iUseThreshold = properties.getPropertyBoolean("Placement.UseThreshold", USE_THRESHOLD);
        
        for (int level=0; level<NR_LEVELS; level++) {
            iNrConflictsWeight[level] = properties.getPropertyDouble(NR_CONFLICTS_WEIGHT+(level+1),(level==0?1.0:0.0));
            iNrPotentialConflictsWeight[level] = properties.getPropertyDouble(NR_POTENTIAL_CONFLICTS_WEIGHT+(level+1),0.0);
            iNrWeightedConflictsWeight[level] = properties.getPropertyDouble(WEIGHTED_CONFLICTS_WEIGHT+(level+1),(level==0?1.0:0.0));
            iDeltaTimePreferenceWeight[level] = properties.getPropertyDouble(DELTA_TIME_PREFERENCE_WEIGHT+(level+1), (level==0?0.5:0.0));
            iPerturbationPenaltyWeight[level] = (iMPP?properties.getPropertyDouble(DELTA_INITIAL_ASSIGNMENT_WEIGHT+(level+1), (level==0?0.5:level==1?1.0:0.0)):0.0);
            iNrStudentConflictsWeight[level] = properties.getPropertyDouble(NR_STUDENT_CONF_WEIGHT+(level+1),(level==0?0.1:(level==1?0.2:0.0)));
            iNrHardStudentConflictsWeight[level] = (iSW?properties.getPropertyDouble(NR_HARD_STUDENT_CONF_WEIGHT+(level+1),(level==0?0.5:level==1?1.0:0.0)):0.0);
            iNrCommitedStudentConflictsWeight[level] = properties.getPropertyDouble(NR_COMMITED_STUDENT_CONF_WEIGHT+(level+1),(level==0?0.2:level==1?1.0:0.0));
            iUselessSlotsWeight[level] = properties.getPropertyDouble(USELESS_SLOTS_WEIGHT+(level+1), 0.0);
            iSumConstrPreferencesWeight[level] = properties.getPropertyDouble(SUM_CONSTR_PREFERENCE_WEIGHT+(level+1), (level==0?0.5:level==1?1.0:0.0));
            iSumRoomPreferencesWeight[level] = properties.getPropertyDouble(SUM_ROOM_PREFERENCE_WEIGHT+(level+1), (level==1?0.1:0.0));
            iSumTimePreferencesWeight[level] = properties.getPropertyDouble(SUM_TIME_PREFERENCE_WEIGHT+(level+1), (level==1?1.0:0.0));
            iNrAssignmentsWeight[level] = properties.getPropertyDouble(NR_ASSIGNMENTS_WEIGHT+(level+1), 0.0);
            iThresholdKoef[level] = (USE_THRESHOLD?properties.getPropertyDouble(NR_THRESHOLD_KOEF+(level+1), (level==0?0.1:0.0)):0.0);
            iTooBigRoomWeight[level] = properties.getPropertyDouble(TOO_BIG_ROOM_WEIGHT+(level+1), 0.0);
            iDeptSpreadWeight[level] = properties.getPropertyDouble(DEPT_SPREAD_WEIGHT+(level+1), (level==0?0.5:level==1?1.0:0.0));
            iSpreadWeight[level] = properties.getPropertyDouble(SPREAD_WEIGHT+(level+1), (level==0?0.5:level==1?1.0:0.0));
            iDistanceInstructorPreferenceWeight[level] = properties.getPropertyDouble(DISTANCE_INSTRUCTOR_PREFERENCE_WEIGHT+(level+1), (level==0?0.1:level==1?1.0:0.0));
        }
        
        if (autoconfigure) {
            iNrConflictsWeight[0] = 3.0;
            iNrPotentialConflictsWeight[0] = 0.0;
            iNrWeightedConflictsWeight[0] = 3.0;
            iDeltaTimePreferenceWeight[0] = properties.getPropertyDouble("Comparator.TimePreferenceWeight",1.0)/2.0;
            iNrAssignmentsWeight[0] = 0.0;
            iThresholdKoef[0] = 0.1;
            
            iNrStudentConflictsWeight[0] = properties.getPropertyDouble("Comparator.StudentConflictWeight",0.2);
            iNrHardStudentConflictsWeight[0] = properties.getPropertyDouble("Comparator.HardStudentConflictWeight",1.0);
            iNrCommitedStudentConflictsWeight[0] = properties.getPropertyDouble("Comparator.CommitedStudentConflictWeight",1.0);
            iUselessSlotsWeight[0] = properties.getPropertyDouble("Comparator.UselessSlotWeight",0.0);
            iSumConstrPreferencesWeight[0] = properties.getPropertyDouble("Comparator.ContrPreferenceWeight",1.0);
            iSumRoomPreferencesWeight[0] = properties.getPropertyDouble("Comparator.RoomPreferenceWeight",0.1);
            iSumTimePreferencesWeight[0] = properties.getPropertyDouble("Comparator.TimePreferenceWeight",1.0);
            iTooBigRoomWeight[0] = properties.getPropertyDouble("Comparator.TooBigRoomWeight",0.0);
            iDeptSpreadWeight[0] = properties.getPropertyDouble("Comparator.DeptSpreadPenaltyWeight",1.0);
            iSpreadWeight[0] = properties.getPropertyDouble("Comparator.SpreadPenaltyWeight",1.0);
            iDistanceInstructorPreferenceWeight[0] = properties.getPropertyDouble("Comparator.DistanceInstructorPreferenceWeight",1.0);
            iPerturbationPenaltyWeight[0] = (iMPP?properties.getPropertyDouble("Comparator.PerturbationPenaltyWeight",1.0):0.0);

            iNrConflictsWeight[1] = 0.0;
            iNrPotentialConflictsWeight[1] = 0.0;
            iNrWeightedConflictsWeight[1] = 0.0;
            iDeltaTimePreferenceWeight[1] = 0.0;
            iNrAssignmentsWeight[1] = 0.0;
            iThresholdKoef[1] = 0.0;
            
            iNrStudentConflictsWeight[1] = properties.getPropertyDouble("Comparator.StudentConflictWeight",0.2);
            iNrHardStudentConflictsWeight[1] = properties.getPropertyDouble("Comparator.HardStudentConflictWeight",1.0);
            iNrCommitedStudentConflictsWeight[1] = properties.getPropertyDouble("Comparator.CommitedStudentConflictWeight",1.0);
            iUselessSlotsWeight[1] = properties.getPropertyDouble("Comparator.UselessSlotWeight",0.0);
            iSumConstrPreferencesWeight[1] = properties.getPropertyDouble("Comparator.ContrPreferenceWeight",1.0);
            iSumRoomPreferencesWeight[1] = properties.getPropertyDouble("Comparator.RoomPreferenceWeight",0.1);
            iSumTimePreferencesWeight[1] = properties.getPropertyDouble("Comparator.TimePreferenceWeight",1.0);
            iTooBigRoomWeight[1] = properties.getPropertyDouble("Comparator.TooBigRoomWeight",0.0);
            iDeptSpreadWeight[1] = properties.getPropertyDouble("Comparator.DeptSpreadPenaltyWeight",1.0);
            iSpreadWeight[1] = properties.getPropertyDouble("Comparator.SpreadPenaltyWeight",1.0);
            iDistanceInstructorPreferenceWeight[1] = properties.getPropertyDouble("Comparator.DistanceInstructorPreferenceWeight",1.0);
            iPerturbationPenaltyWeight[1] = (iMPP?properties.getPropertyDouble("Comparator.PerturbationPenaltyWeight",1.0):0.0);

            iNrConflictsWeight[2] = 0.0;
            iNrPotentialConflictsWeight[2] = 0.0;
            iNrWeightedConflictsWeight[2] = 0.0;
            iDeltaTimePreferenceWeight[2] = 0.0;
            iPerturbationPenaltyWeight[2] = 0.0;
            iNrStudentConflictsWeight[2] = 0.0;
            iNrHardStudentConflictsWeight[2] = 0.0;
            iNrCommitedStudentConflictsWeight[2] = 0.0;
            iUselessSlotsWeight[2] = 0.0;
            iSumConstrPreferencesWeight[2] = 0.0;
            iSumRoomPreferencesWeight[2] = 0.0;
            iSumTimePreferencesWeight[2] = 0.0;
            iNrAssignmentsWeight[2] = 0.0;
            iThresholdKoef[2] = 0.0;
            iTooBigRoomWeight[2] = 0.0;
            iDeptSpreadWeight[2] = 0.0;
            iSpreadWeight[2] = 0.0;
            iDistanceInstructorPreferenceWeight[2] = 0.0;
        }
    }
    
    public Value selectValue(Solution solution, Variable selectedVariable) {
    	if (selectedVariable==null) return null;
    	TimetableModel model = (TimetableModel)solution.getModel();
        /*if (iMPPLimit>=0 && solution.getModel().unassignedVariables().isEmpty() && iMPPLimit>solution.getModel().perturbVariables().size()) {
            ToolBox.print("A complete solution with "+solution.getModel().perturbVariables().size()+" perturbances found");
            iMPPLimit = solution.getModel().perturbVariables().size()-1;
            ToolBox.print("MPP limit decreased to "+iMPPLimit);
        }*/
        if (selectedVariable.getInitialAssignment()!=null) {
            if (iMPPLimit>=0 && model.perturbVariables().size()>=iMPPLimit) {
            	if (!containsItselfSingletonOrCommited(model, model.conflictValues(selectedVariable.getInitialAssignment()),selectedVariable.getInitialAssignment()))
            		return checkValue(selectedVariable.getInitialAssignment());
            } else if (iMPPPenaltyLimit>=0.0 && solution.getPerturbationsCounter()!=null && solution.getPerturbationsCounter().getPerturbationPenalty(model)>iMPPPenaltyLimit) {
            	if (!containsItselfSingletonOrCommited(model, model.conflictValues(selectedVariable.getInitialAssignment()),selectedVariable.getInitialAssignment()))
            		return checkValue(selectedVariable.getInitialAssignment());
            } else if (selectedVariable.getInitialAssignment()!=null && ToolBox.random()<=iInitialSelectionProb) {
            	if (!containsItselfSingletonOrCommited(model, model.conflictValues(selectedVariable.getInitialAssignment()),selectedVariable.getInitialAssignment()))
            		return checkValue(selectedVariable.getInitialAssignment());
            }
        }
        
        Vector values = selectedVariable.values();
        if (iRW && ToolBox.random()<=iRandomWalkProb) {
        	for (int i=0;i<5;i++) {
        		Value ret = (Value)ToolBox.random(values);
        		if (!containsItselfSingletonOrCommited(model, model.conflictValues(ret), ret))
        			return checkValue(ret);
        	}
        }
        if (iProp!=null && selectedVariable.getAssignment()==null && ToolBox.random()<=iGoodSelectionProb) {
            Collection goodValues = iProp.goodValues(selectedVariable);
            if (!goodValues.isEmpty()) values=new FastVector(goodValues);
        }
        if (values.size()==1) {
        	Value ret = (Value)values.firstElement();
        	if (!containsItselfSingletonOrCommited(model, model.conflictValues(ret), ret))
        		return checkValue(ret);
        }
        
        long[] bestCost = new long[NR_LEVELS];
        Vector selectionValues = null;
        
        
        HeuristicSelector selector = (iUseThreshold?new HeuristicSelector(iThresholdKoef):null);
        for (Enumeration i1=values.elements();i1.hasMoreElements();) {
            Value value = (Value) i1.nextElement();
            if (iTabu!=null && iTabu.contains(value)) continue;
            if (selectedVariable.getAssignment()!=null && selectedVariable.getAssignment().equals(value)) continue;
            
            ParamRetriever paramRetriever = new ParamRetriever(solution, (Lecture)selectedVariable, (Placement)value);
            if (containsItselfSingletonOrCommited(model, paramRetriever.conflicts(), value)) continue;
            
            if (iUseThreshold) {
            	Double flt = selector.firstLevelThreshold();
                double[] costs = new double[NR_LEVELS];
                for (int level=0;level<NR_LEVELS;level++) {
                    costs[level] = getCost(paramRetriever, level, flt);
                    if (level==0 && flt!=null && costs[0]>flt.doubleValue()) {
                    	break;
                    }
                }
                if (flt!=null && costs[0]>flt.doubleValue()) continue;
                selector.add(costs, value);
            } else {
            	Double flt = (selectionValues==null?null:new Double(0.5+bestCost[0]));
                boolean fail = false;
                boolean best = false;
                for (int level=0;!fail && level<1;level++) {
                    long cost = Math.round( PRECISION * getCost(paramRetriever, level, flt));
                    if (selectionValues!=null && !best) {
                        if (cost>bestCost[level]) { fail=true; }
                        if (cost<bestCost[level]) { bestCost[level]=cost; selectionValues.clear(); best=true; }
                    } else {
                        bestCost[level]=cost;
                    }
                }
                if (selectionValues==null) selectionValues = new FastVector(values.size());
                if (!fail) selectionValues.addElement(value);
            }
        }
        //ToolBox.print("Best "+selectionValues.size()+" locations for variable "+selectedVariable.getId()+" have "+bestConflicts+" conflicts ("+bestRemovals+" weighted) and "+bestStudentConflicts+" ("+bestOriginalStudentConflicts+" * "+bestKoef+" + "+bestPenalty+") preference.");
        Value selectedValue = null;
        if (iUseThreshold) {
            selectionValues = selector.selection();
            
            if (selectedVariable.getInitialAssignment()!=null) {
                for (Enumeration e=selectionValues.elements();e.hasMoreElements();) {
                    Value value = (Value)((HeuristicSelector.Element) e.nextElement()).getObject();
                    if (value.equals(selectedVariable.getInitialAssignment())) {
                        selectedValue = value;
                        break;
                    }
                }
                //&& selectionValues.contains(selectedVariable.getInitialAssignment())) return selectedVariable.getInitialAssignment();
            } 
            
            if (selectedValue==null) {
                HeuristicSelector.Element selection = (HeuristicSelector.Element)ToolBox.random(selectionValues);
                selectedValue = (Value)(selection==null?null:selection.getObject());
            }
        } else {
            if (selectedVariable.getInitialAssignment()!=null && selectionValues.contains(selectedVariable.getInitialAssignment())) return checkValue(selectedVariable.getInitialAssignment());
            selectedValue = (Value)ToolBox.random(selectionValues);
        }
        if (selectedValue!=null && iTabu!=null) {
            if (iTabu.size()==iTabuPos)
                iTabu.add(selectedValue);
            else
                iTabu.set(iTabuPos, selectedValue);
            iTabuPos = (iTabuPos + 1) % iTabuSize;
        }
        return checkValue(selectedValue);
    }
    
    public boolean containsItselfSingletonOrCommited(TimetableModel model, Collection values, Value selectedValue) {
    	if (values.contains(selectedValue)) return true;
    	if (model.hasConstantVariables()) {
        	for (Iterator i=values.iterator();i.hasNext();) {
        		Placement placement = (Placement)i.next();
        		Lecture lecture = (Lecture)placement.variable();
        		if (lecture.isCommitted()) return true;
        		if (!iCanUnassingSingleton && lecture.isSingleton()) return true;
        	}
        	return false;
    	} else {
    		if (iCanUnassingSingleton) return false;
    		for (Iterator i=values.iterator();i.hasNext();) {
    			Placement placement = (Placement)i.next();
    			Lecture lecture = (Lecture)placement.variable();
    			if (lecture.isSingleton()) return true;
    		}
    		return false;
    	}
    }
    
    private Value checkValue(Value aValue) {
        if (aValue==null) return null;
        for (Iterator i=((Lecture)aValue.variable()).getWeakeningConstraints().iterator();i.hasNext();) {
        	Constraint c = (Constraint)i.next();
        	if (c.inConflict(aValue)) ((WeakeningConstraint)c).weaken();
        }
        return aValue;
    }
    
    public ParamRetriever getParameters(Solution solution, Lecture lecture, Placement placement) {
        return new ParamRetriever(solution, lecture, placement);
    }
    
    public double getCost(ParamRetriever paramRetriever, int level, Double flt) {
    	if (level==0 && flt!=null) {
            double cost =
                (iNrConflictsWeight[level]==0.0?0.0:iNrConflictsWeight[level]*paramRetriever.nrContlicts())+
                (iNrWeightedConflictsWeight[level]==0.0?0.0:iNrWeightedConflictsWeight[level]*paramRetriever.weightedConflicts())+
                (iNrPotentialConflictsWeight[level]==0.0?0.0:iNrPotentialConflictsWeight[level]*paramRetriever.potentialConflicts(3))+
                (iDeltaTimePreferenceWeight[level]==0.0?0.0:iDeltaTimePreferenceWeight[level]*paramRetriever.deltaTimePreference())+
                (iSumConstrPreferencesWeight[level]==0.0?0.0:iSumConstrPreferencesWeight[level]*paramRetriever.constrPreference())+
                (iSumRoomPreferencesWeight[level]==0.0?0.0:iSumRoomPreferencesWeight[level]*paramRetriever.roomPreference())+
                (iSumTimePreferencesWeight[level]==0.0?0.0:iSumTimePreferencesWeight[level]*paramRetriever.timePreference())+
                (iNrAssignmentsWeight[level]==0.0?0.0:iNrAssignmentsWeight[level]*paramRetriever.nrAssignments())
                ;     
            if (cost>flt.doubleValue()) return cost;
            cost += 
                (iUselessSlotsWeight[level]==0.0?0.0:iUselessSlotsWeight[level]*paramRetriever.emptySingleHalfHours())+
                (iTooBigRoomWeight[level]==0.0?0.0:iTooBigRoomWeight[level]*paramRetriever.tooBig())
                ;
            if (cost>flt.doubleValue()) return cost;
            cost +=
                (iPerturbationPenaltyWeight[level]==0.0?0.0:iPerturbationPenaltyWeight[level]*paramRetriever.perturbationsPenalty())
                ;
            if (cost>flt.doubleValue()) return cost;
            cost += 
                (iDistanceInstructorPreferenceWeight[level]==0.0?0.0:iDistanceInstructorPreferenceWeight[level]*paramRetriever.distanceInstructorPreference())
                ;
            if (cost>flt.doubleValue()) return cost;
            cost +=
                (iDeptSpreadWeight[level]==0.0?0.0:iDeptSpreadWeight[level]*paramRetriever.deptSpread())+
                (iSpreadWeight[level]==0.0?0.0:iSpreadWeight[level]*paramRetriever.spread())
                ;
            if (cost>flt.doubleValue()) return cost;
            cost += 
                (iNrStudentConflictsWeight[level]==0.0?0.0:iNrStudentConflictsWeight[level]*paramRetriever.sumStudentConflicts())+
                (iNrHardStudentConflictsWeight[level]==0.0?0.0:iNrHardStudentConflictsWeight[level]*paramRetriever.sumHardStudentConflicts())+
                (iNrCommitedStudentConflictsWeight[level]==0.0?0.0:iNrCommitedStudentConflictsWeight[level]*paramRetriever.sumCommitedStudentConflicts())
                ;
            return cost;
    	}
        double cost =
                    (iNrConflictsWeight[level]==0.0?0.0:iNrConflictsWeight[level]*paramRetriever.nrContlicts())+
                    (iNrWeightedConflictsWeight[level]==0.0?0.0:iNrWeightedConflictsWeight[level]*paramRetriever.weightedConflicts())+
                    (iNrPotentialConflictsWeight[level]==0.0?0.0:iNrPotentialConflictsWeight[level]*paramRetriever.potentialConflicts(3))+
                    (iDeltaTimePreferenceWeight[level]==0.0?0.0:iDeltaTimePreferenceWeight[level]*paramRetriever.deltaTimePreference())+
                    (iPerturbationPenaltyWeight[level]==0.0?0.0:iPerturbationPenaltyWeight[level]*paramRetriever.perturbationsPenalty())+
                    (iNrStudentConflictsWeight[level]==0.0?0.0:iNrStudentConflictsWeight[level]*paramRetriever.sumStudentConflicts())+
                    (iNrHardStudentConflictsWeight[level]==0.0?0.0:iNrHardStudentConflictsWeight[level]*paramRetriever.sumHardStudentConflicts())+
                    (iUselessSlotsWeight[level]==0.0?0.0:iUselessSlotsWeight[level]*paramRetriever.emptySingleHalfHours())+
                    (iSumConstrPreferencesWeight[level]==0.0?0.0:iSumConstrPreferencesWeight[level]*paramRetriever.constrPreference())+
                    (iSumRoomPreferencesWeight[level]==0.0?0.0:iSumRoomPreferencesWeight[level]*paramRetriever.roomPreference())+
                    (iSumTimePreferencesWeight[level]==0.0?0.0:iSumTimePreferencesWeight[level]*paramRetriever.timePreference())+
                    (iNrAssignmentsWeight[level]==0.0?0.0:iNrAssignmentsWeight[level]*paramRetriever.nrAssignments())+
                    (iTooBigRoomWeight[level]==0.0?0.0:iTooBigRoomWeight[level]*paramRetriever.tooBig())+
                    (iDeptSpreadWeight[level]==0.0?0.0:iDeptSpreadWeight[level]*paramRetriever.deptSpread())+
                    (iSpreadWeight[level]==0.0?0.0:iSpreadWeight[level]*paramRetriever.spread())+
                    (iDistanceInstructorPreferenceWeight[level]==0.0?0.0:iDistanceInstructorPreferenceWeight[level]*paramRetriever.distanceInstructorPreference())+
                    (iNrCommitedStudentConflictsWeight[level]==0.0?0.0:iNrCommitedStudentConflictsWeight[level]*paramRetriever.sumCommitedStudentConflicts())
                    ;     
        return cost;
    }
    
    public class ParamRetriever {
        private Lecture iLecture;
        private Placement iPlacement;
        private Solution iSolution;
        private ParamRetriever(Solution solution, Lecture lecture, Placement placement) {
            iSolution = solution;
            iLecture = lecture;
            iPlacement = placement;
        }
        
        Collection iConf = null;
        public Collection conflicts() {
            if (iConf == null) iConf = iSolution.getModel().conflictValues(iPlacement);
            return iConf;
        }
        
        public long nrContlicts() {
            return conflicts().size();
        }
        
        private Double iWeightedConflicts = null;
        public double weightedConflicts() {
            if (iWeightedConflicts==null) 
                iWeightedConflicts = new Double(iStat==null?0.0:iStat.countRemovals(iSolution.getIteration(), conflicts(), iPlacement));
            return iWeightedConflicts.doubleValue();
        }
        
        private Double iPotentialConflicts = null;
        public double potentialConflicts(int limit) {
            if (iPotentialConflicts==null)
                iPotentialConflicts = new Double(iStat==null?0.0:iStat.countPotentialConflicts(iSolution.getIteration(),iPlacement, limit));
            return iPotentialConflicts.doubleValue();
        }
         
        Double iDeltaTimePreference = null;
        public double deltaTimePreference() {
            if (iDeltaTimePreference==null) {
                double deltaTimePreference = 0;
                for (Iterator it1=conflicts().iterator(); it1.hasNext(); ) {
                    Placement placement = (Placement)it1.next();
                    double timePref = placement.getTimeLocation().getNormalizedPreference();
                    deltaTimePreference -= timePref - ((Lecture)placement.variable()).getBestTimePreference();
                }
                deltaTimePreference += iPlacement.getTimeLocation().getNormalizedPreference() - iLecture.getBestTimePreference();
                iDeltaTimePreference = new Double(deltaTimePreference);
            }
            return iDeltaTimePreference.doubleValue();
        }
        
        Double iPerturbationsPenalty = null;
        public double perturbationsPenalty() {
            if (iPerturbationsCounter==null) return 0.0;
            if (iPerturbationsPenalty==null) {
                iPerturbationsPenalty = new Double(iPerturbationsCounter.getPerturbationPenalty(iSolution.getModel(),iPlacement,conflicts()));
            }
            return iPerturbationsPenalty.doubleValue();
        }
        
        public void countStudentConflicts() {
        	long all = 0;
        	int hard = 0;
        	if (!iLecture.isSingleSection()) {
        		for (Enumeration i=iLecture.jenrlConstraints();i.hasMoreElements();) {
        			JenrlConstraint jc = (JenrlConstraint)i.nextElement();
        			all += jc.jenrl(iLecture,iPlacement);
        		}
        	} else {
        		for (Enumeration i=iLecture.jenrlConstraints();i.hasMoreElements();) {
        			JenrlConstraint jc = (JenrlConstraint)i.nextElement();
        			long jenrl = jc.jenrl(iLecture,iPlacement);
        			if (((Lecture)jc.another(iLecture)).isSingleSection()) hard += jenrl;
        			all += jenrl;
        		}
        	}
        	iSumStudentConflicts = new Integer((int)all);
        	iSumHardStudentConflicts = new Integer(hard);
        }
        
        Integer iSumStudentConflicts = null;
        public int sumStudentConflicts() {
            if (iSumStudentConflicts==null) countStudentConflicts();
            return iSumStudentConflicts.intValue();
        }
        
        Integer iConstrPreference = null;
        public int constrPreference() {
            if (iConstrPreference==null) {
                int constrPreference = 0;
                for (Iterator i2=iLecture.groupConstraints().iterator();i2.hasNext();) {
                	GroupConstraint gc = (GroupConstraint)i2.next();
                	constrPreference += gc.getCurrentPreference(iPlacement);
                }
                iConstrPreference = new Integer(constrPreference);
            }
            return iConstrPreference.intValue();
        }
        
        Integer iEmptySingleHalfHours = null;
        public int emptySingleHalfHours() {
            if (iEmptySingleHalfHours==null) {
            	iEmptySingleHalfHours = new Integer(iPlacement.nrUselessHalfHours());
            }
            return iEmptySingleHalfHours.intValue();
        }
        
        Integer iSumHardStudentConflicts = null;
        public int sumHardStudentConflicts() {
            if (iSumHardStudentConflicts==null) countStudentConflicts();
            return iSumHardStudentConflicts.intValue();
        }
        
        public int sumCommitedStudentConflicts() {
        	return iLecture.getCommitedConflicts(iPlacement);
        }

        public int roomPreference() {
        	return iPlacement.sumRoomPreference();
        }
        
        public long nrAssignments() {
            return iPlacement.countAssignments();
        }
        
        public double timePreference() {
            return iPlacement.getTimeLocation().getNormalizedPreference();
        }
        
        public int tooBig() {
        	return iPlacement.getTooBigRoomPreference();
        }
        
        Integer iDeptSpreadCache = null;
        public int deptSpread() {
            if (iLecture.getDeptSpreadConstraint()==null) return 0;
            if (iDeptSpreadCache==null) 
                iDeptSpreadCache = new Integer(iLecture.getDeptSpreadConstraint().getPenalty(iPlacement));
            return iDeptSpreadCache.intValue();
        }
        
        Integer iSpreadCache = null;
        public int spread() {
            if (iLecture.getSpreadConstraints().isEmpty()) return 0;
            if (iSpreadCache==null) {
            	iSpreadCache = new Integer(iPlacement.getSpreadPenalty());
            }
            return iSpreadCache.intValue();
        }

        Integer iDistanceInstructorPreferenceCache = null;
        public int distanceInstructorPreference() {
            if (iDistanceInstructorPreferenceCache==null) {
                int pref = 0;
                for (Enumeration i2=iLecture.constraints().elements();i2.hasMoreElements();) {
                	Constraint constraint = (Constraint)i2.nextElement();
                    if (constraint instanceof InstructorConstraint) {
                    	pref += ((InstructorConstraint)constraint).getPreference(iPlacement);
                    }
                }
                iDistanceInstructorPreferenceCache = new Integer(pref);
            }
            return iDistanceInstructorPreferenceCache.intValue();
        }
    }
    
    public PerturbationsCounter getPerturbationsCounter() {
        return iPerturbationsCounter;
    }
}
