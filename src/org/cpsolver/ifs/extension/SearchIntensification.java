package org.cpsolver.ifs.extension;

import java.util.Collection;
import java.util.Map;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentContext;
import org.cpsolver.ifs.assignment.context.ExtensionWithContext;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solution.SolutionListener;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Go back to the best known solution when no better solution is found within
 * the given amount of iterations.
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
public class SearchIntensification<V extends Variable<V, T>, T extends Value<V, T>> extends ExtensionWithContext<V, T, SearchIntensification<V, T>.Context> implements SolutionListener<V, T> {
    private static org.apache.logging.log4j.Logger sLogger = org.apache.logging.log4j.LogManager.getLogger(SearchIntensification.class);
    private long iInitialIterationLimit = 100;
    private ConflictStatistics<V, T> iCBS = null;
    private int iResetInterval = 5;
    private int iMux = 2;
    private int iMuxInterval = 2;

    public SearchIntensification(Solver<V, T> solver, DataProperties properties) {
        super(solver, properties);
        iInitialIterationLimit = properties.getPropertyLong("SearchIntensification.IterationLimit", iInitialIterationLimit);
        iResetInterval = properties.getPropertyInt("SearchIntensification.ResetInterval", iResetInterval);
        iMuxInterval = properties.getPropertyInt("SearchIntensification.MultiplyInterval", iMuxInterval);
        iMux = properties.getPropertyInt("SearchIntensification.Multiply", iMux);
    }

    @Override
    public boolean init(Solver<V, T> solver) {
        if (iResetInterval > 0) {
            for (Extension<V, T> ex : solver.getExtensions()) {
                if (ConflictStatistics.class.isInstance(ex)) {
                    iCBS = (ConflictStatistics<V, T>) ex;
                    break;
                }
            }
        }
        return super.init(solver);
    }

    @Override
    public void register(Model<V, T> model) {
        super.register(model);
        getSolver().currentSolution().addSolutionListener(this);
    }

    @Override
    public void unregister(Model<V, T> model) {
        super.unregister(model);
        getSolver().currentSolution().removeSolutionListener(this);
    }

    @Override
    public void afterAssigned(Assignment<V, T> assignment, long iteration, T value) {
        getContext(assignment).afterAssigned(assignment, iteration, value);
    }

    @Override
    public void bestSaved(Solution<V, T> solution) {
        Context context = getContext(solution.getAssignment());
        context.bestSaved(solution);
    }

    @Override
    public void solutionUpdated(Solution<V, T> solution) {
    }

    @Override
    public void bestCleared(Solution<V, T> solution) {
    }

    @Override
    public void bestRestored(Solution<V, T> solution) {
    }

    @Override
    public void getInfo(Solution<V, T> solution, Map<String, String> info) {
    }

    @Override
    public void getInfo(Solution<V, T> solution, Map<String, String> info, Collection<V> variables) {
    }
    
    /**
     * Assignment context
     */
    public class Context implements AssignmentContext {
        private int iResetCounter = 0;
        private int iMuxCounter = 0;
        private long iLastReturn = 0;
        private long iIterationLimit = 100;
        
        public Context(Assignment<V, T> assignment) {
            iIterationLimit = iInitialIterationLimit;
        }
        
        public void afterAssigned(Assignment<V, T> assignment, long iteration, T value) {
            if (iIterationLimit > 0 && iteration > 0) {
                Solution<V, T> solution = getSolver().currentSolution();
                if (solution.getBestIteration() < 0 || !solution.isBestComplete())
                    return;
                long bestIt = Math.max(solution.getBestIteration(), iLastReturn);
                long currIt = solution.getIteration();
                if (currIt - bestIt > iIterationLimit) {
                    iLastReturn = currIt;
                    iResetCounter++;
                    iMuxCounter++;
                    sLogger.debug("Going back to the best known solution...");
                    solution.restoreBest();
                    sLogger.debug("Reset counter set to " + iResetCounter);
                    if (iMuxInterval > 0 && (iMuxCounter % iMuxInterval) == 0) {
                        iIterationLimit *= iMux;
                        sLogger.debug("Iteration limit increased to " + iIterationLimit);
                    }
                    if (iCBS != null && iResetInterval > 0 && (iResetCounter % iResetInterval) == 0) {
                        sLogger.debug("Reseting CBS...");
                        iCBS.reset();
                    }
                }
            }
        }

        public void bestSaved(Solution<V, T> solution) {
            if (iResetCounter > 0)
                sLogger.debug("Reset counter set to zero.");
            iResetCounter = 0;
            iMuxCounter = 0;
            iIterationLimit = iInitialIterationLimit;
        }
    }

    @Override
    public Context createAssignmentContext(Assignment<V, T> assignment) {
        return new Context(assignment);
    }
}
