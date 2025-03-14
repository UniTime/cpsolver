package org.cpsolver.coursett.heuristics;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentContext;
import org.cpsolver.ifs.assignment.context.NeighbourSelectionWithContext;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.SimpleNeighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.Progress;


/**
 * University course timetabling neighbour selection. <br>
 * <br>
 * When a complete solution is found, it is improved by limited depth
 * backtracking search. This way it is ensured that the fund solution is at
 * least locally optimal.
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
public class FixCompleteSolutionNeighbourSelection extends NeighbourSelectionWithContext<Lecture, Placement, FixCompleteSolutionNeighbourSelection.FixCompleteSolutionNeighbourContext> {
    private NeighbourSelection<Lecture, Placement> iParent = null;
    private long iLastCompleteSolutionFixIteration = -1;
    private long iLastIncompleteSolutionFixIteration = -1;
    private long iCompleteSolutionFixInterval = 1;
    private long iIncompleteSolutionFixInterval = 5000;
    private NeighbourSelectionWithSuggestions iSuggestions = null;
    private Progress iProgress = null;
    private Solver<Lecture, Placement> iSolver = null;

    public FixCompleteSolutionNeighbourSelection(DataProperties config, NeighbourSelection<Lecture, Placement> parent) throws Exception {
        iParent = parent;
        iSuggestions = new NeighbourSelectionWithSuggestions(config);
    }
    
    public FixCompleteSolutionNeighbourSelection(DataProperties config) throws Exception {
        this(config, new NeighbourSelectionWithSuggestions(config));
    }
    
    @Override
    public void init(Solver<Lecture, Placement> solver) {
        super.init(solver);
        iCompleteSolutionFixInterval = solver.getProperties().getPropertyLong("General.CompleteSolutionFixInterval", iCompleteSolutionFixInterval);
        iIncompleteSolutionFixInterval = solver.getProperties().getPropertyLong("General.IncompleteSolutionFixInterval", iIncompleteSolutionFixInterval);
        iSuggestions.init(solver);
        iParent.init(solver);
        iProgress = Progress.getInstance(solver.currentSolution().getModel());
        iLastIncompleteSolutionFixIteration = -1;
        iLastCompleteSolutionFixIteration = -1;
        iSolver = solver;
    }

    /**
     * Try to improve existing solution by backtracking search of very limited
     * depth. See {@link NeighbourSelectionWithSuggestions} for more details.
     */
    @Override
    public Neighbour<Lecture, Placement> selectNeighbour(Solution<Lecture, Placement> solution) {
        FixCompleteSolutionNeighbourContext context = getContext(solution.getAssignment());
        if (context.getPhase() == 0) {
            if (solution.getModel().nrUnassignedVariables(solution.getAssignment()) == 0) {
                // complete solution was found
                if (iCompleteSolutionFixInterval < 0) {
                    // feature disabled
                    return iParent.selectNeighbour(solution);
                } else if (iCompleteSolutionFixInterval == 0) {
                    // only run first time a complete solution is found
                    if (iLastCompleteSolutionFixIteration >= 0) return iParent.selectNeighbour(solution);
                } else {
                    // run first time and if not run for a given number of iterations
                    if (iLastCompleteSolutionFixIteration >= 0 && solution.getIteration() - iLastCompleteSolutionFixIteration < iCompleteSolutionFixInterval)
                        return iParent.selectNeighbour(solution);
                }
                if (solution.getBestIteration() == solution.getIteration())
                    context.incPhase(solution);
            } else if (!solution.isBestComplete()) {
                // complete solution has not been found yet
                if (iIncompleteSolutionFixInterval < 0) {
                    // feature disabled
                    return iParent.selectNeighbour(solution);
                } else if (iIncompleteSolutionFixInterval == 0) {
                    // only run first time a complete solution is found
                    if (iLastIncompleteSolutionFixIteration >= 0)
                        return iParent.selectNeighbour(solution);
                } else {
                    // run first time and if not run for a given number of iterations
                    if (solution.getIteration() - iLastIncompleteSolutionFixIteration < iIncompleteSolutionFixInterval)
                        return iParent.selectNeighbour(solution);
                }
                if (solution.getBestIteration() == solution.getIteration())
                    context.incPhase(solution);
            }
        }
        
        while (context.getPhase() > 0 && !iSolver.isStop()) {
            if (context.hasMoreElements()) {
                Lecture variable = context.nextElement();
                // iProgress.incProgress();
                if (context.getPhase() == 1) {
                    Placement bestValue = null;
                    double bestVal = 0.0;
                    Placement currentValue = solution.getAssignment().getValue(variable);
                    if (currentValue == null)
                        continue;
                    double currentVal = currentValue.toDouble(solution.getAssignment());
                    for (Placement value : variable.values(solution.getAssignment())) {
                        if (value.equals(currentValue))
                            continue;
                        if (solution.getModel().conflictValues(solution.getAssignment(), value).isEmpty()) {
                            double val = value.toDouble(solution.getAssignment());
                            if (bestValue == null || val < bestVal) {
                                bestValue = value;
                                bestVal = val;
                            }
                        }
                    }
                    if (bestValue != null && bestVal < currentVal)
                        return new SimpleNeighbour<Lecture, Placement>(variable, bestValue);                    
                } else {
                    Neighbour<Lecture, Placement> n = iSuggestions.selectNeighbourWithSuggestions(solution, variable, 2);
                    if (n != null && n.value(solution.getAssignment()) <= solution.getModel().getTotalValue(solution.getAssignment()))
                        return n;
                }
            } else {
                context.incPhase(solution);
            }
        }
        return iParent.selectNeighbour(solution);
    }
    
    @Override
    public FixCompleteSolutionNeighbourContext createAssignmentContext(Assignment<Lecture, Placement> assignment) {
        return new FixCompleteSolutionNeighbourContext(assignment);
    }

    public class FixCompleteSolutionNeighbourContext implements AssignmentContext, Enumeration<Lecture> {
        private int iPhase = 0;
        private Iterator<Lecture> iLectures = null;
        
        public FixCompleteSolutionNeighbourContext(Assignment<Lecture, Placement> assignment) {}
        
        public int getPhase() { return iPhase; }
        
        public void incPhase(Solution<Lecture, Placement> solution) {
            // Progress progress = Progress.getInstance(solution.getModel());                
            if (iPhase == 0) {
                // iSolver.setUpdateProgress(false);
                iPhase = 1;
                solution.saveBest();
                // progress.setPhase("Fixing solution", solution.getModel().countVariables());
                iProgress.info("[" + Thread.currentThread().getName() + "] Fixing solution...");
                iLectures = new ArrayList<Lecture>(solution.getModel().variables()).iterator();
            } else if (iPhase == 1 && solution.isComplete()) {
                iPhase = 2;
                iProgress.info("[" + Thread.currentThread().getName() + "] Fixing complete solution...");
                // progress.setPhase("Fixing solution [2]", solution.getModel().countVariables());
                iLectures = new ArrayList<Lecture>(solution.getModel().variables()).iterator();
            } else {
                if (iPhase == 1)
                    iLastIncompleteSolutionFixIteration = solution.getIteration();
                else 
                    iLastCompleteSolutionFixIteration = solution.getIteration();
                // iSolver.setUpdateProgress(true);
                // if (solution.isBestComplete())
                //     iProgress.setPhase("Improving found solution ...");
                // else
                //     iProgress.setPhase("Searching for initial solution ...", solution.getModel().countVariables());
                iPhase = 0;
                // progress.restore();
                iLectures = null;
            }
        }

        @Override
        public boolean hasMoreElements() {
            return iLectures != null && iLectures.hasNext();
        }

        @Override
        public Lecture nextElement() {
            return iLectures.next();
        }
        
    }
}
