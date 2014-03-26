package net.sf.cpsolver.coursett.heuristics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.coursett.criteria.TimetablingCriterion;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.criteria.Criterion;
import net.sf.cpsolver.ifs.extension.Extension;
import net.sf.cpsolver.ifs.extension.MacPropagation;
import net.sf.cpsolver.ifs.heuristics.ValueSelection;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Placement (value) selection. <br>
 * <br>
 * We have implemented a hierarchical handling of the value selection criteria
 * (see {@link HeuristicSelector}). <br>
 * <br>
 * The value selection heuristics also allow for random selection of a value
 * with a given probability (random walk, e.g., 2%) and, in the case of MPP, to
 * select the initial value (if it exists) with a given probability (e.g., 70%). <br>
 * <br>
 * Parameters (general):
 * <table border='1'>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>Placement.RandomWalkProb</td>
 * <td>{@link Double}</td>
 * <td>Random walk probability</td>
 * </tr>
 * <tr>
 * <td>Placement.GoodSelectionProb</td>
 * <td>{@link Double}</td>
 * <td>Good value (not removed from domain) selection probability (MAC related)</td>
 * </tr>
 * <tr>
 * <td>Placement.TabuLength</td>
 * <td>{@link Integer}</td>
 * <td>Tabu-list length (-1 means do not use tabu-list)</td>
 * </tr>
 * <tr>
 * <td>Placement.MPP_InitialProb</td>
 * <td>{@link Double}</td>
 * <td>MPP initial selection probability</td>
 * </tr>
 * <tr>
 * <td>Placement.MPP_Limit</td>
 * <td>{@link Integer}</td>
 * <td>MPP: limit on the number of perturbations (-1 for no limit)</td>
 * </tr>
 * <tr>
 * <td>Placement.MPP_PenaltyLimit</td>
 * <td>{@link Double}</td>
 * <td>MPP: limit on the perturbations penalty (-1 for no limit)</td>
 * </tr>
 * </table>
 * <br>
 * Parameters (for each level of selection):
 * <table border='1'>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>Placement.NrAssignmentsWeight1<br>
 * Placement.NrAssignmentsWeight2<br>
 * Placement.NrAssignmentsWeight3</td>
 * <td>{@link Double}</td>
 * <td>Number of previous assignments of the value weight</td>
 * </tr>
 * <tr>
 * <td>Placement.NrConflictsWeight1,2,3</td>
 * <td>{@link Double}</td>
 * <td>Number of conflicts weight</td>
 * </tr>
 * <tr>
 * <td>Placement.WeightedConflictsWeight1,2,3</td>
 * <td>{@link Double}</td>
 * <td>Weighted conflicts weight (Conflict-based Statistics related)</td>
 * </tr>
 * <tr>
 * <td>Placement.NrPotentialConflictsWeight1,2,3</td>
 * <td>{@link Double}</td>
 * <td>Number of potential conflicts weight (Conflict-based Statistics related)</td>
 * </tr>
 * <tr>
 * <td>Placement.MPP_DeltaInitialAssignmentWeight1,2,3</td>
 * <td>{@link Double}</td>
 * <td>Delta initial assigments weight (MPP, violated initials related)</td>
 * </tr>
 * <tr>
 * <td>Placement.NrHardStudConfsWeight1,2,3</td>
 * <td>{@link Double}</td>
 * <td>Hard student conflicts weight (student conflicts between single-section
 * classes)</td>
 * </tr>
 * <tr>
 * <td>Placement.NrStudConfsWeight1,2,3</td>
 * <td>{@link Double}</td>
 * <td>Student conflicts weight</td>
 * </tr>
 * <tr>
 * <td>Placement.TimePreferenceWeight1,2,3</td>
 * <td>{@link Double}</td>
 * <td>Time preference weight</td>
 * </tr>
 * <tr>
 * <td>Placement.DeltaTimePreferenceWeight1,2,3</td>
 * <td>{@link Double}</td>
 * <td>Time preference delta weight (difference between before and after
 * assignemnt of the value)</td>
 * </tr>
 * <tr>
 * <td>Placement.ConstrPreferenceWeight1,2,3</td>
 * <td>{@link Double}</td>
 * <td>Constraint preference weight</td>
 * </tr>
 * <tr>
 * <td>Placement.RoomPreferenceWeight1,2,3</td>
 * <td>{@link Double}</td>
 * <td>Room preference weight</td>
 * </tr>
 * <tr>
 * <td>Placement.UselessSlotsWeight1,2,3</td>
 * <td>{@link Double}</td>
 * <td>Useless slot weight</td>
 * </tr>
 * <tr>
 * <td>Placement.TooBigRoomWeight1,2,3</td>
 * <td>{@link Double}</td>
 * <td>Too big room weight</td>
 * </tr>
 * <tr>
 * <td>Placement.DistanceInstructorPreferenceWeight1,2,3</td>
 * <td>{@link Double}</td>
 * <td>Distance (of the rooms of the back-to-back classes) based instructor
 * preferences weight</td>
 * </tr>
 * <tr>
 * <td>Placement.DeptSpreadPenaltyWeight1,2,3</td>
 * <td>{@link Double}</td>
 * <td>Department spreading: penalty of when a slot over initial allowance is
 * used</td>
 * </tr>
 * <tr>
 * <td>Placement.ThresholdKoef1,2</td>
 * <td>{@link Double}</td>
 * <td>Threshold koeficient of the level</td>
 * </tr>
 * </table>
 * 
 * @see PlacementSelection
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          <a href="http://muller.unitime.org">http://muller.unitime.org</a><br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 3 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */

public class PlacementSelection implements ValueSelection<Lecture, Placement> {
    static final int NR_LEVELS = 3;
    private static final double PRECISION = 1.0;
    private static boolean USE_THRESHOLD = true;
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

    private double[] iThresholdKoef = new double[NR_LEVELS];
    public static final String NR_THRESHOLD_KOEF = "Placement.ThresholdKoef";

    private int iTabuSize = 0;
    private ArrayList<Placement> iTabu = null;
    private int iTabuPos = 0;
    public static final String TABU_LENGTH = "Placement.TabuLength";

    private MacPropagation<Lecture, Placement> iProp = null;

    private boolean iRW = false;
    private boolean iMPP = false;

    private boolean iCanUnassingSingleton = false;

    @Override
    public void init(Solver<Lecture, Placement> solver) {
        for (Extension<Lecture, Placement> extension : solver.getExtensions()) {
            if (MacPropagation.class.isInstance(extension))
                iProp = (MacPropagation<Lecture, Placement>) extension;
        }
    }

    public PlacementSelection(DataProperties properties) {
        iMPP = properties.getPropertyBoolean("General.MPP", false);
        iRW = properties.getPropertyBoolean("General.RandomWalk", true);
        iCanUnassingSingleton = properties.getPropertyBoolean("Placement.CanUnassingSingleton", iCanUnassingSingleton);
        iRandomWalkProb = (iRW ? properties.getPropertyDouble(RW_SELECTION_PROB, 0.00) : 0.0);
        iGoodSelectionProb = properties.getPropertyDouble(GOOD_SELECTION_PROB, 1.00);
        iInitialSelectionProb = (iMPP ? properties.getPropertyDouble(INITIAL_SELECTION_PROB, 0.75) : 0.0);
        iMPPLimit = (iMPP ? properties.getPropertyInt(NR_MPP_LIMIT, -1) : -1);
        iMPPPenaltyLimit = (iMPP ? properties.getPropertyDouble(NR_MPP_PENALTY_LIMIT, -1.0) : -1.0);
        iTabuSize = properties.getPropertyInt(TABU_LENGTH, -1);
        if (iTabuSize > 0)
            iTabu = new ArrayList<Placement>(iTabuSize);
        iUseThreshold = properties.getPropertyBoolean("Placement.UseThreshold", USE_THRESHOLD);
        for (int level = 0; level < NR_LEVELS; level++)
            iThresholdKoef[level] = (USE_THRESHOLD ? properties.getPropertyDouble(NR_THRESHOLD_KOEF + (level + 1), (level == 0 ? 0.1 : 0.0)) : 0.0);
    }

    @Override
    public Placement selectValue(Solution<Lecture, Placement> solution, Lecture var) {
        if (var == null)
            return null;
        Lecture selectedVariable = var;

        TimetableModel model = (TimetableModel) solution.getModel();
        if (selectedVariable.getInitialAssignment() != null) {
            if (iMPPLimit >= 0 && model.perturbVariables().size() >= iMPPLimit) {
                if (!containsItselfSingletonOrCommited(model, model.conflictValues(selectedVariable.getInitialAssignment()), selectedVariable.getInitialAssignment()))
                    return selectedVariable.getInitialAssignment();
            } else if (iMPPPenaltyLimit >= 0.0 && solution.getPerturbationsCounter() != null && solution.getPerturbationsCounter().getPerturbationPenalty(model) > iMPPPenaltyLimit) {
                if (!containsItselfSingletonOrCommited(model, model.conflictValues(selectedVariable.getInitialAssignment()), selectedVariable.getInitialAssignment()))
                    return selectedVariable.getInitialAssignment();
            } else if (selectedVariable.getInitialAssignment() != null && ToolBox.random() <= iInitialSelectionProb) {
                if (!containsItselfSingletonOrCommited(model, model.conflictValues(selectedVariable.getInitialAssignment()), selectedVariable.getInitialAssignment()))
                    return selectedVariable.getInitialAssignment();
            }
        }

        List<Placement> values = selectedVariable.values();
        if (iRW && ToolBox.random() <= iRandomWalkProb) {
            for (int i = 0; i < 5; i++) {
                Placement ret = ToolBox.random(values);
                if (!containsItselfSingletonOrCommited(model, model.conflictValues(ret), ret))
                    return ret;
            }
        }
        if (iProp != null && selectedVariable.getAssignment() == null && ToolBox.random() <= iGoodSelectionProb) {
            Collection<Placement> goodValues = iProp.goodValues(selectedVariable);
            if (!goodValues.isEmpty())
                values = new ArrayList<Placement>(goodValues);
        }
        if (values.size() == 1) {
            Placement ret = values.get(0);
            if (!containsItselfSingletonOrCommited(model, model.conflictValues(ret), ret))
                return ret;
        }

        long[] bestCost = new long[NR_LEVELS];
        List<Placement> selectionValues = null;

        HeuristicSelector<Placement> selector = (iUseThreshold ? new HeuristicSelector<Placement>(iThresholdKoef) : null);
        for (Placement value : values) {
            if (iTabu != null && iTabu.contains(value))
                continue;
            if (selectedVariable.getAssignment() != null && selectedVariable.getAssignment().equals(value))
                continue;

            Set<Placement> conflicts = value.variable().getModel().conflictValues(value);
            
            if (containsItselfSingletonOrCommited(model, conflicts, value))
                continue;

            if (iUseThreshold) {
                Double flt = selector.firstLevelThreshold();
                double[] costs = new double[NR_LEVELS];
                for (int level = 0; level < NR_LEVELS; level++) {
                    costs[level] = getCost(level, value, conflicts);
                    if (level == 0 && flt != null && costs[0] > flt.doubleValue()) {
                        break;
                    }
                }
                if (flt != null && costs[0] > flt.doubleValue())
                    continue;
                selector.add(costs, value);
            } else {
                boolean fail = false;
                boolean best = false;
                for (int level = 0; !fail && level < 1; level++) {
                    double val = getCost(level, value, conflicts);
                    long cost = Math.round(PRECISION * val);
                    if (selectionValues != null && !best) {
                        if (cost > bestCost[level]) {
                            fail = true;
                        }
                        if (cost < bestCost[level]) {
                            bestCost[level] = cost;
                            selectionValues.clear();
                            best = true;
                        }
                    } else {
                        bestCost[level] = cost;
                    }
                }
                if (selectionValues == null)
                    selectionValues = new ArrayList<Placement>(values.size());
                if (!fail)
                    selectionValues.add(value);
            }
        }
        // ToolBox.print("Best "+selectionValues.size()+" locations for variable "+selectedVariable.getId()+" have "+bestConflicts+" conflicts ("+bestRemovals+" weighted) and "+bestStudentConflicts+" ("+bestOriginalStudentConflicts+" * "+bestKoef+" + "+bestPenalty+") preference.");
        Placement selectedValue = null;
        if (iUseThreshold) {
            List<HeuristicSelector<Placement>.Element> selectionElements = selector.selection();

            if (selectedVariable.getInitialAssignment() != null) {
                for (HeuristicSelector<Placement>.Element element : selectionElements) {
                    Placement value = element.getObject();
                    if (value.equals(selectedVariable.getInitialAssignment())) {
                        selectedValue = value;
                        break;
                    }
                }
                // &&
                // selectionValues.contains(selectedVariable.getInitialAssignment()))
                // return selectedVariable.getInitialAssignment();
            }

            if (selectedValue == null) {
                HeuristicSelector<Placement>.Element selection = ToolBox.random(selectionElements);
                selectedValue = (selection == null ? null : selection.getObject());
            }
        } else {
            if (selectedVariable.getInitialAssignment() != null
                    && selectionValues.contains(selectedVariable.getInitialAssignment()))
                return selectedVariable.getInitialAssignment();
            selectedValue = ToolBox.random(selectionValues);
        }
        if (selectedValue != null && iTabu != null) {
            if (iTabu.size() == iTabuPos)
                iTabu.add(selectedValue);
            else
                iTabu.set(iTabuPos, selectedValue);
            iTabuPos = (iTabuPos + 1) % iTabuSize;
        }
        return selectedValue;
    }

    public boolean containsItselfSingletonOrCommited(TimetableModel model, Set<Placement> values,
            Placement selectedValue) {
        if (values.contains(selectedValue))
            return true;
        if (model.hasConstantVariables()) {
            for (Placement placement : values) {
                Lecture lecture = placement.variable();
                if (lecture.isCommitted())
                    return true;
                if (!iCanUnassingSingleton && lecture.isSingleton())
                    return true;
            }
            return false;
        } else {
            if (iCanUnassingSingleton)
                return false;
            for (Placement placement : values) {
                Lecture lecture = placement.variable();
                if (lecture.isSingleton())
                    return true;
            }
            return false;
        }
    }

    private double getCost(int level, Placement value, Set<Placement> conflicts) {
        double ret = 0.0;
        for (Criterion<Lecture, Placement> criterion: value.variable().getModel().getCriteria()) {
            if (criterion instanceof TimetablingCriterion) {
                double w = ((TimetablingCriterion)criterion).getPlacementSelectionWeight(level);
                if (w != 0.0)
                    ret += w * criterion.getValue(value, conflicts);
            } else {
                ret += criterion.getWeightedValue(value, conflicts);
            }
        }
        return ret;
    }
    
}
