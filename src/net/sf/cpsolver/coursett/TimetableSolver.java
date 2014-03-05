package net.sf.cpsolver.coursett;

import net.sf.cpsolver.coursett.heuristics.NeighbourSelectionWithSuggestions;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.JProf;
import net.sf.cpsolver.ifs.util.Progress;

/**
 * University course timetabling solver. <br>
 * <br>
 * When a complete solution is found, it is improved by limited depth
 * backtracking search. This way it is ensured that the fund solution is at
 * least locally optimal.
 * 
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
    protected void onAssigned(double startTime) {
        if (iCurrentSolution.getModel().nrUnassignedVariables() == 0) {
            // complete solution was found
            if (iCompleteSolutionFixInterval < 0) {
             // feature disabled
                return;
            } else if (iCompleteSolutionFixInterval == 0) {
                // only run first time a complete solution is found
                if (iLastCompleteSolutionFixIteration >= 0) return;
            } else {
                // run first time and if not run for a given number of iterations
                if (iLastCompleteSolutionFixIteration >= 0 && iCurrentSolution.getIteration() - iLastCompleteSolutionFixIteration < iCompleteSolutionFixInterval) return;
            }
            if (getSolutionComparator().isBetterThanBestSolution(iCurrentSolution)) {
                fixCompleteSolution(startTime);
                iLastCompleteSolutionFixIteration = iCurrentSolution.getIteration();
            }
        } else if (iCurrentSolution.getBestInfo() == null) {
            // complete solution has not been found yet
            if (iIncompleteSolutionFixInterval < 0) {
                // feature disabled
                   return;
            } else if (iIncompleteSolutionFixInterval == 0) {
                // only run first time a complete solution is found
                if (iLastIncompleteSolutionFixIteration >= 0) return;
            } else {
                // run first time and if not run for a given number of iterations
                if (iLastIncompleteSolutionFixIteration >= 0 && iCurrentSolution.getIteration() - iLastIncompleteSolutionFixIteration < iIncompleteSolutionFixInterval) return;
            }
            if (getSolutionComparator().isBetterThanBestSolution(iCurrentSolution)) {
                fixCompleteSolution(startTime);
                iLastIncompleteSolutionFixIteration = iCurrentSolution.getIteration();
            }
        }
    }

    /**
     * Try to improve existing solution by backtracking search of very limited
     * depth. See {@link NeighbourSelectionWithSuggestions} for more details.
     */
    protected void fixCompleteSolution(double startTime) {
        Progress progress = Progress.getInstance(currentSolution().getModel());

        TimetableModel model = (TimetableModel) iCurrentSolution.getModel();
        iCurrentSolution.saveBest();
        progress.save();
        double solutionValue = 0.0, newSolutionValue = model.getTotalValue();
        do {
            solutionValue = newSolutionValue;
            progress.setPhase("Fixing solution", model.variables().size());
            for (Lecture variable : model.variables()) {
                Placement bestValue = null;
                double bestVal = 0.0;
                Placement currentValue = variable.getAssignment();
                if (currentValue == null)
                    continue;
                double currentVal = currentValue.toDouble();
                for (Placement value : variable.values()) {
                    if (value.equals(currentValue))
                        continue;
                    if (model.conflictValues(value).isEmpty()) {
                        double val = value.toDouble();
                        if (bestValue == null || val < bestVal) {
                            bestValue = value;
                            bestVal = val;
                        }
                    }
                }
                if (bestValue != null && bestVal < currentVal)
                    variable.assign(0, bestValue);
                iCurrentSolution.update(JProf.currentTimeSec() - startTime);
                progress.incProgress();
                if (iStop)
                    break;
            }
            newSolutionValue = model.getTotalValue();
            if (newSolutionValue < solutionValue) {
                progress.debug("New solution value is  " + newSolutionValue);
            }
        } while (!iStop && newSolutionValue < solutionValue && getTerminationCondition().canContinue(iCurrentSolution));
        progress.restore();

        if (!iCurrentSolution.getModel().unassignedVariables().isEmpty())
            return;
        progress.save();
        try {
            progress.setPhase("Fixing solution [2]", model.variables().size());
            NeighbourSelectionWithSuggestions ns = new NeighbourSelectionWithSuggestions(this);
            for (Lecture lecture : model.variables()) {
                Neighbour<Lecture, Placement> n = ns.selectNeighbourWithSuggestions(iCurrentSolution, lecture, 2);
                if (n != null && n.value() <= 0.0)
                    n.assign(0);
                iCurrentSolution.update(JProf.currentTimeSec() - startTime);
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
