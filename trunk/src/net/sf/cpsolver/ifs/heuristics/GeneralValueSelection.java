package net.sf.cpsolver.ifs.heuristics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.ifs.extension.ConflictStatistics;
import net.sf.cpsolver.ifs.extension.Extension;
import net.sf.cpsolver.ifs.extension.MacPropagation;
import net.sf.cpsolver.ifs.extension.ViolatedInitials;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * General implementation of value selection criterion. <br>
 * <br>
 * Value selection criterion is based on weighted sum of various criteria. It
 * also allows random walk technique and tabu search. <br>
 * Parameters: <br>
 * <table border='1'>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>General.MPP</td>
 * <td>{@link Boolean}</td>
 * <td>if true, MPP is being solved</td>
 * </tr>
 * <tr>
 * <td>Value.MPPLimit</td>
 * <td>{@link Integer}</td>
 * <td>MPP: limitation of the number of allowed perturbations. If a solution
 * within this limit is gound, it is decreased.</td>
 * </tr>
 * <tr>
 * <td>Value.InitialSelectionProb</td>
 * <td>{@link Double}</td>
 * <td>MPP: probability of selection of the initial value</td>
 * </tr>
 * <tr>
 * <td>Value.RandomWalkProb</td>
 * <td>{@link Double}</td>
 * <td>Random Walk: probability of selection of a value randomly among all the
 * values</td>
 * </tr>
 * <tr>
 * <td>Value.Tabu</td>
 * <td>{@link Integer}</td>
 * <td>Tabu Search: length of the tabu-list</td>
 * </tr>
 * <tr>
 * <td>Value.GoodSelectionProb</td>
 * <td>{@link Double}</td>
 * <td>In case of {@link MacPropagation}, with this probability (1.0 means
 * always), the selection is made only among good values (not removed from the
 * domain).</td>
 * </tr>
 * </table>
 * <br>
 * Following weights are used in the weighted sum (computed for all values). The
 * value with the lowest weighted sum is selected. If there are more than one of
 * such values, one of them is selected randomly. <br>
 * <table border='1'>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>Value.WeightDeltaInitialAssignments</td>
 * <td>{@link Double}</td>
 * <td>MPP: Difference in the number of assigned initial values if the value is
 * assigned to the variable (weighted by this
 * Value.WeightDeltaInitialAssignments): -1 if the value is initial, 0
 * otherwise, increased by the number of initial values assigned to variables
 * with hard conflicts with the value</td>
 * </tr>
 * <tr>
 * <td>Value.WeightWeightedConflicts</td>
 * <td>{@link Double}</td>
 * <td>When {@link ConflictStatistics} is used: weighted number of conflicting
 * variables</td>
 * </tr>
 * <tr>
 * <td>Value.WeightPotentialConflicts</td>
 * <td>{@link Double}</td>
 * <td>When {@link ConflictStatistics} is used: weighted number of potentially
 * conflicting variables</td>
 * </tr>
 * <tr>
 * <td>Value.WeightConflicts</td>
 * <td>{@link Double}</td>
 * <td>Number of conflicting variables {@link Model#conflictValues(Value)}.</td>
 * </tr>
 * <tr>
 * <td>Value.WeightNrAssignments</td>
 * <td>{@link Double}</td>
 * <td>Number of previous assignments of the value</td>
 * </tr>
 * <tr>
 * <td>Value.WeightValue</td>
 * <td>{@link Double}</td>
 * <td>Value {@link Value#toDouble()}</td>
 * </tr>
 * </table>
 * 
 * @see VariableSelection
 * @see Solver
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 **/

public class GeneralValueSelection<V extends Variable<V, T>, T extends Value<V, T>> implements ValueSelection<V, T> {
    private double iRandomWalkProb = 0.0;
    private double iInitialSelectionProb = 0.0;
    private double iGoodSelectionProb = 0.0;
    private int iMPPLimit = -1;

    private double iWeightDeltaInitialAssignment = 0.0;
    private double iWeightPotentialConflicts = 0.0;
    private double iWeightWeightedCoflicts = 0.0;
    private double iWeightCoflicts = 1.0;
    private double iWeightNrAssignments = 0.5;
    private double iWeightValue = 0.0;

    protected int iTabuSize = 0;
    protected ArrayList<T> iTabu = null;
    protected int iTabuPos = 0;

    private boolean iMPP = false;
    private ConflictStatistics<V, T> iStat = null;
    private MacPropagation<V, T> iProp = null;
    private ViolatedInitials<V, T> iViolatedInitials = null;

    public GeneralValueSelection() {
    }

    /**
     * Constructor
     * 
     * @param properties
     *            input configuration
     */
    public GeneralValueSelection(DataProperties properties) {
        iMPP = properties.getPropertyBoolean("General.MPP", false);
        if (iMPP) {
            iMPPLimit = properties.getPropertyInt("Value.MPPLimit", -1);
            iInitialSelectionProb = properties.getPropertyDouble("Value.InitialSelectionProb", 0.75);
            iWeightDeltaInitialAssignment = properties.getPropertyDouble("Value.WeightDeltaInitialAssignments", 0.0);
        }
        iGoodSelectionProb = properties.getPropertyDouble("Value.GoodSelectionProb", 0.00);
        iWeightWeightedCoflicts = properties.getPropertyDouble("Value.WeightWeightedConflicts", 1.0);
        iWeightPotentialConflicts = properties.getPropertyDouble("Value.WeightPotentialConflicts", 0.0);

        iRandomWalkProb = properties.getPropertyDouble("Value.RandomWalkProb", 0.0);
        iWeightCoflicts = properties.getPropertyDouble("Value.WeightConflicts", 1.0);
        iWeightNrAssignments = properties.getPropertyDouble("Value.WeightNrAssignments", 0.5);
        iWeightValue = properties.getPropertyDouble("Value.WeightValue", 0.0);
        iTabuSize = properties.getPropertyInt("Value.Tabu", 0);
        if (iTabuSize > 0)
            iTabu = new ArrayList<T>(iTabuSize);
    }

    /** Initialization */
    @Override
    public void init(Solver<V, T> solver) {
        for (Extension<V, T> extension : solver.getExtensions()) {
            if (ConflictStatistics.class.isInstance(extension))
                iStat = (ConflictStatistics<V, T>) extension;
            if (MacPropagation.class.isInstance(extension))
                iProp = (MacPropagation<V, T>) extension;
            if (ViolatedInitials.class.isInstance(extension))
                iViolatedInitials = (ViolatedInitials<V, T>) extension;
        }
    }

    /** Value selection */
    @Override
    public T selectValue(Solution<V, T> solution, V selectedVariable) {
        if (iMPP) {
            if (selectedVariable.getInitialAssignment() != null) {
                if (solution.getModel().nrUnassignedVariables() == 0) {
                    if (solution.getModel().perturbVariables().size() <= iMPPLimit)
                        iMPPLimit = solution.getModel().perturbVariables().size() - 1;
                }
                if (iMPPLimit >= 0 && solution.getModel().perturbVariables().size() > iMPPLimit)
                    return selectedVariable.getInitialAssignment();
                if (selectedVariable.getInitialAssignment() != null && ToolBox.random() <= iInitialSelectionProb)
                    return selectedVariable.getInitialAssignment();
            }
        }

        List<T> values = selectedVariable.values();
        if (ToolBox.random() <= iRandomWalkProb)
            return ToolBox.random(values);
        if (iProp != null && selectedVariable.getAssignment() == null && ToolBox.random() <= iGoodSelectionProb) {
            Collection<T> goodValues = iProp.goodValues(selectedVariable);
            if (!goodValues.isEmpty())
                values = new ArrayList<T>(goodValues);
        }
        if (values.size() == 1)
            return values.get(0);

        List<T> bestValues = null;
        double bestWeightedSum = 0;

        for (T value : values) {
            if (iTabu != null && iTabu.contains(value))
                continue;
            if (selectedVariable.getAssignment() != null && selectedVariable.getAssignment().equals(value))
                continue;

            Collection<T> conf = solution.getModel().conflictValues(value);
            if (conf.contains(value))
                continue;

            double weightedConflicts = (iStat == null || iWeightWeightedCoflicts == 0.0 ? 0.0 : iStat.countRemovals(
                    solution.getIteration(), conf, value));
            double potentialConflicts = (iStat == null || iWeightPotentialConflicts == 0.0 ? 0.0 : iStat
                    .countPotentialConflicts(solution.getIteration(), value, 3));

            long deltaInitialAssignments = 0;
            if (iMPP && iWeightDeltaInitialAssignment != 0.0) {
                if (iViolatedInitials != null) {
                    Set<T> violations = iViolatedInitials.getViolatedInitials(value);
                    if (violations != null) {
                        for (T aValue : violations) {
                            if (aValue.variable().getAssignment() == null
                                    || aValue.variable().getAssignment().equals(aValue))
                                deltaInitialAssignments += 2;
                        }
                    }
                }
                for (Iterator<T> it1 = conf.iterator(); it1.hasNext();) {
                    T aValue = it1.next();
                    if (aValue.variable().getInitialAssignment() != null)
                        deltaInitialAssignments--;
                }
                if (selectedVariable.getInitialAssignment() != null
                        && !selectedVariable.getInitialAssignment().equals(value)) {
                    deltaInitialAssignments++;
                }
                if (iMPPLimit >= 0
                        && (solution.getModel().perturbVariables().size() + deltaInitialAssignments) > iMPPLimit)
                    continue;
            }

            double weightedSum = (iWeightDeltaInitialAssignment * deltaInitialAssignments)
                    + (iWeightPotentialConflicts * potentialConflicts) + (iWeightWeightedCoflicts * weightedConflicts)
                    + (iWeightCoflicts * conf.size()) + (iWeightNrAssignments * value.countAssignments())
                    + (iWeightValue * value.toDouble());

            if (bestValues == null || bestWeightedSum > weightedSum) {
                bestWeightedSum = weightedSum;
                if (bestValues == null)
                    bestValues = new ArrayList<T>();
                else
                    bestValues.clear();
                bestValues.add(value);
            } else {
                if (bestWeightedSum == weightedSum)
                    bestValues.add(value);
            }
        }

        T selectedValue = (bestValues == null ? null : ToolBox.random(bestValues));
        if (selectedValue == null)
            selectedValue = ToolBox.random(values);
        if (iTabu != null) {
            if (iTabu.size() == iTabuPos)
                iTabu.add(selectedValue);
            else
                iTabu.set(iTabuPos, selectedValue);
            iTabuPos = (iTabuPos + 1) % iTabuSize;
        }
        return (bestValues == null ? null : selectedValue);
    }

}
