package org.cpsolver.ifs.dbt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.extension.Extension;
import org.cpsolver.ifs.extension.ViolatedInitials;
import org.cpsolver.ifs.heuristics.GeneralValueSelection;
import org.cpsolver.ifs.heuristics.ValueSelection;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;


/**
 * Selection of a value for dynamic backtracking. <br>
 * <br>
 * <ul>
 * <li>Returns null if all values of the selected variable are nogood.
 * <li>Selected the best good value (according to the parameters) of the selected variable.
 * </ul>
 * <br>
 * It is based on a weighted sum of several criteria. <br>
 * <br>
 * This IFS solver value selection heuristics is to be used only in case of
 * dynamic backtracking and it has the following parameters: <br>
 * <table border='1'><caption>Related Solver Parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>General.MPP</td>
 * <td>{@link Boolean}</td>
 * <td>Minimal Perturbation Problem</td>
 * </tr>
 * <tr>
 * <td>Value.MPPLimit</td>
 * <td>{@link Integer}</td>
 * <td>Limit on the number of perturbations (only in case of MPP, i.e., when
 * General.MPP=true). MPP limit is decreased when a complete solution is found.
 * If set to -1, it is no used</td>
 * </tr>
 * <tr>
 * <td>Value.InitialSelectionProb</td>
 * <td>{@link Double}</td>
 * <td>Probability of selection of initial value (only in case of MPP)</td>
 * </tr>
 * <tr>
 * <td>Value.WeightDeltaInitialAssignments</td>
 * <td>{@link Double}</td>
 * <td>Weight of difference in the number of assignments of initial values in
 * case of selection of the value(only in case of MPP)</td>
 * </tr>
 * <tr>
 * <td>Value.RandomWalkProb</td>
 * <td>{@link Double}</td>
 * <td>Probability of random selection of a good value</td>
 * </tr>
 * <tr>
 * <td>Value.WeightNrAssignments</td>
 * <td>{@link Double}</td>
 * <td>Weight of the number of previous assignments of the value</td>
 * </tr>
 * <tr>
 * <td>Value.WeightValue</td>
 * <td>{@link Double}</td>
 * <td>Weight of the value itself (e.g., for minCSP)</td>
 * </tr>
 * </table>
 * <br>
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
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
 * @param <V> Variable
 * @param <T> Value
 */
public class DbtValueSelection<V extends Variable<V, T>, T extends Value<V, T>> implements ValueSelection<V, T> {
    private static org.apache.logging.log4j.Logger sLogger = org.apache.logging.log4j.LogManager.getLogger(GeneralValueSelection.class);
    private double iRandomWalkProb = 0.0;
    private double iInitialSelectionProb = 0.0;
    private int iMPPLimit = -1;

    private double iWeightDeltaInitialAssignment = 0.0;
    private double iWeightValue = 0.0;

    private boolean iMPP = false;
    private DbtPropagation<V, T> iProp = null;
    private ViolatedInitials<V, T> iViolatedInitials = null;

    public DbtValueSelection(DataProperties properties) {
        iMPP = properties.getPropertyBoolean("General.MPP", false);

        if (iMPP) {
            iMPPLimit = properties.getPropertyInt("Value.MPPLimit", -1);
            iInitialSelectionProb = properties.getPropertyDouble("Value.InitialSelectionProb", 0.75);
            iWeightDeltaInitialAssignment = properties.getPropertyDouble("Value.WeightDeltaInitialAssignments", 0.0);
        }

        iRandomWalkProb = properties.getPropertyDouble("Value.RandomWalkProb", 0.0);
        iWeightValue = properties.getPropertyDouble("Value.WeightValue", 0.0);
    }

    /**
     * Heuristics initialization
     * 
     * @see ValueSelection#init(Solver)
     */
    @Override
    public void init(Solver<V, T> solver) {
        for (Extension<V, T> extension : solver.getExtensions()) {
            if (DbtPropagation.class.isInstance(extension)) {
                iProp = (DbtPropagation<V, T>) extension;
            }
            if (ViolatedInitials.class.isInstance(extension)) {
                iViolatedInitials = (ViolatedInitials<V, T>) extension;
            }
        }
    }

    /**
     * Value selection
     * 
     * @see ValueSelection#selectValue(Solution, Variable)
     */
    @Override
    public T selectValue(Solution<V, T> solution, V selectedVariable) {
        ArrayList<T> values = null;
        Assignment<V, T> assignment = solution.getAssignment();

        if (iProp != null) {
            values = new ArrayList<T>(iProp.goodValues(assignment, selectedVariable).size());
            for (T value : selectedVariable.values(solution.getAssignment())) {
                if (!iProp.isGood(assignment, value)) {
                    continue;
                }
                Collection<T> conf = solution.getModel().conflictValues(assignment, value);

                if (!conf.isEmpty()) {
                    Set<T> noGood = new HashSet<T>(2 * conf.size());

                    for (T v : conf) {
                        noGood.add(v);
                    }
                    iProp.setNoGood(assignment, value, noGood);
                    sLogger.debug(value + " become nogood (" + noGood + ")");
                } else {
                    if (!solution.isBestComplete() || solution.getModel().getBestValue() > solution.getModel().getTotalValue(assignment) + value.toDouble(assignment)) {
                        values.add(value);
                    }
                }
            }
        } else {
            values = new ArrayList<T>(selectedVariable.values(solution.getAssignment()).size());
            for (T value : selectedVariable.values(solution.getAssignment())) {
                if (solution.getModel().conflictValues(assignment, value).isEmpty()) {
                    if (solution.isBestComplete() && solution.getModel().getBestValue() > solution.getModel().getTotalValue(assignment) + value.toDouble(assignment)) {
                        values.add(value);
                    }
                }
            }
        }
        if (values.isEmpty()) {
            return null;
        }

        if (iMPP) {
            if (iMPPLimit >= 0 && solution.isBestComplete() && solution.getModel().getBestPerturbations() >= 0
                    && solution.getModel().getBestPerturbations() <= iMPPLimit) {
                iMPPLimit = solution.getModel().getBestPerturbations() - 1;
                sLogger.debug("MPP Limit decreased to " + iMPPLimit);
            }

            int nrPerts = solution.getModel().perturbVariables(assignment).size();

            if (iMPPLimit >= 0 && iMPPLimit < nrPerts) {
                return null;
            }
            if (iMPPLimit >= 0 && iMPPLimit == nrPerts && selectedVariable.getInitialAssignment() != null) {
                if (values.contains(selectedVariable.getInitialAssignment())) {
                    return selectedVariable.getInitialAssignment();
                }
                return null;
            }

            if (selectedVariable.getInitialAssignment() != null && ToolBox.random() <= iInitialSelectionProb) {
                if (values.contains(selectedVariable.getInitialAssignment())) {
                    return selectedVariable.getInitialAssignment();
                }
            }
        }

        if (values.size() == 1) {
            return values.get(0);
        }

        if (ToolBox.random() <= iRandomWalkProb) {
            return ToolBox.random(values);
        }

        ArrayList<T> bestValues = null;
        double bestWeightedSum = 0;

        if (iWeightDeltaInitialAssignment == 0.0 && iWeightValue == 0.0) {
            return ToolBox.random(values);
        }

        for (T value : values) {

            long deltaInitialAssignments = 0;

            if (iWeightDeltaInitialAssignment != 0.0) {
                if (iViolatedInitials != null) {
                    Set<T> violations = iViolatedInitials.getViolatedInitials(value);

                    if (violations != null) {
                        for (T aValue : violations) {
                            if (assignment.getValue(aValue.variable()) == null || assignment.getValue(aValue.variable()).equals(aValue)) {
                                deltaInitialAssignments += 2;
                            }
                        }
                    }
                }
                if (selectedVariable.getInitialAssignment() != null
                        && !selectedVariable.getInitialAssignment().equals(value)) {
                    deltaInitialAssignments++;
                }
                if (iMPPLimit >= 0 && (solution.getModel().perturbVariables(assignment).size() + deltaInitialAssignments) > iMPPLimit) {
                    continue;
                }
            }

            double weightedSum = (iWeightDeltaInitialAssignment * deltaInitialAssignments) + (iWeightValue * value.toDouble(assignment));

            if (bestValues == null || bestWeightedSum > weightedSum) {
                bestWeightedSum = weightedSum;
                if (bestValues == null) {
                    bestValues = new ArrayList<T>();
                } else {
                    bestValues.clear();
                }
                bestValues.add(value);
            } else if (bestWeightedSum == weightedSum) {
                bestValues.add(value);
            }
        }
        return (bestValues == null ? null : (T) ToolBox.random(bestValues));
    }
}
