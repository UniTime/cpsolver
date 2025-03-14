package org.cpsolver.coursett;

import org.cpsolver.coursett.heuristics.FixCompleteSolutionNeighbourSelection;
import org.cpsolver.coursett.heuristics.NeighbourSelectionWithSuggestions;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.JProf;
import org.cpsolver.ifs.util.Progress;

/**
 * University course timetabling solver. <br>
 * <br>
 * When a complete solution is found, it is improved by limited depth
 * backtracking search. This way it is ensured that the fund solution is at
 * least locally optimal.<br>
 * <br>
 * Deprecated: use {@link FixCompleteSolutionNeighbourSelection} instead.
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
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
 */

@Deprecated
public class TimetableSolver extends Solver<Lecture, Placement> {
    private long iLastCompleteSolutionFixIteration = -1;
    private long iLastIncompleteSolutionFixIteration = -1;
    private long iCompleteSolutionFixInterval = 1;
    private long iIncompleteSolutionFixInterval = 5000;

    public TimetableSolver(DataProperties properties) {
        super(properties);
    }
    
    @Override
    public void initSolver() {
        super.initSolver();
        iCompleteSolutionFixInterval = getProperties().getPropertyLong("General.CompleteSolutionFixInterval", iCompleteSolutionFixInterval);
        iIncompleteSolutionFixInterval = getProperties().getPropertyLong("General.IncompleteSolutionFixInterval", iIncompleteSolutionFixInterval);
    }

    @Override
    protected void onAssigned(double startTime, Solution<Lecture, Placement> solution) {
        if (solution.getModel().nrUnassignedVariables(solution.getAssignment()) == 0) {
            // complete solution was found
            if (iCompleteSolutionFixInterval < 0) {
             // feature disabled
                return;
            } else if (iCompleteSolutionFixInterval == 0) {
                // only run first time a complete solution is found
                if (iLastCompleteSolutionFixIteration >= 0) return;
            } else {
                // run first time and if not run for a given number of iterations
                if (iLastCompleteSolutionFixIteration >= 0 && solution.getIteration() - iLastCompleteSolutionFixIteration < iCompleteSolutionFixInterval) return;
            }
            if (getSolutionComparator().isBetterThanBestSolution(solution)) {
                fixCompleteSolution(solution, startTime);
                iLastCompleteSolutionFixIteration = solution.getIteration();
            }
        } else if (solution.getBestInfo() == null) {
            // complete solution has not been found yet
            if (iIncompleteSolutionFixInterval < 0) {
                // feature disabled
                   return;
            } else if (iIncompleteSolutionFixInterval == 0) {
                // only run first time a complete solution is found
                if (iLastIncompleteSolutionFixIteration >= 0) return;
            } else {
                // run first time and if not run for a given number of iterations
                if (iLastIncompleteSolutionFixIteration >= 0 && solution.getIteration() - iLastIncompleteSolutionFixIteration < iIncompleteSolutionFixInterval) return;
            }
            if (getSolutionComparator().isBetterThanBestSolution(solution)) {
                fixCompleteSolution(solution, startTime);
                iLastIncompleteSolutionFixIteration = solution.getIteration();
            }
        }
    }

    /**
     * Try to improve existing solution by backtracking search of very limited
     * depth. See {@link NeighbourSelectionWithSuggestions} for more details.
     * @param solution current solution
     * @param startTime start time
     */
    protected void fixCompleteSolution(Solution<Lecture, Placement> solution, double startTime) {
        Progress progress = Progress.getInstance(solution.getModel());

        TimetableModel model = (TimetableModel) solution.getModel();
        Assignment<Lecture, Placement> assignment = solution.getAssignment();
        solution.saveBest();
        progress.save();
        double solutionValue = 0.0, newSolutionValue = model.getTotalValue(assignment);
        do {
            solutionValue = newSolutionValue;
            progress.setPhase("Fixing solution", model.variables().size());
            for (Lecture variable : model.variables()) {
                Placement bestValue = null;
                double bestVal = 0.0;
                Placement currentValue = assignment.getValue(variable);
                if (currentValue == null)
                    continue;
                double currentVal = currentValue.toDouble(assignment);
                for (Placement value : variable.values()) {
                    if (value.equals(currentValue))
                        continue;
                    if (model.conflictValues(assignment, value).isEmpty()) {
                        double val = value.toDouble(assignment);
                        if (bestValue == null || val < bestVal) {
                            bestValue = value;
                            bestVal = val;
                        }
                    }
                }
                if (bestValue != null && bestVal < currentVal)
                    assignment.assign(0, bestValue);
                solution.update(JProf.currentTimeSec() - startTime);
                progress.incProgress();
                if (iStop)
                    break;
            }
            newSolutionValue = model.getTotalValue(assignment);
            if (newSolutionValue < solutionValue) {
                progress.debug("New solution value is  " + newSolutionValue);
            }
        } while (!iStop && newSolutionValue < solutionValue && getTerminationCondition().canContinue(solution));
        progress.restore();

        if (!solution.getModel().unassignedVariables(assignment).isEmpty())
            return;
        progress.save();
        try {
            progress.setPhase("Fixing solution [2]", model.variables().size());
            NeighbourSelectionWithSuggestions ns = new NeighbourSelectionWithSuggestions(this);
            for (Lecture lecture : model.variables()) {
                Neighbour<Lecture, Placement> n = ns.selectNeighbourWithSuggestions(solution, lecture, 2);
                if (n != null && n.value(assignment) <= 0.0)
                    n.assign(assignment, 0);
                solution.update(JProf.currentTimeSec() - startTime);
                progress.incProgress();
                if (iStop)
                    break;
            }
        } catch (Exception e) {
            sLogger.debug(e.getMessage(), e);
        } finally {
            progress.restore();
        }
    }
}
