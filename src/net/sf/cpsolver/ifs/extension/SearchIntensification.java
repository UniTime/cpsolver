package net.sf.cpsolver.ifs.extension;

import java.util.*;

import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.solution.*;
import net.sf.cpsolver.ifs.solver.*;
import net.sf.cpsolver.ifs.util.*;

/**
 * Go back to the best known solution when no better solution is found within
 * the given amount of iterations.
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 */
public class SearchIntensification<V extends Variable<V, T>, T extends Value<V, T>> extends Extension<V, T> implements
        SolutionListener<V, T> {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(SearchIntensification.class);
    private long iInitialIterationLimit = 100;
    private long iIterationLimit = 100;
    private long iLastReturn = 0;
    private ConflictStatistics<V, T> iCBS = null;
    private int iResetInterval = 5;
    private int iResetCounter = 0;
    private int iMux = 2;
    private int iMuxInterval = 2;
    private int iMuxCounter = 0;

    public SearchIntensification(Solver<V, T> solver, DataProperties properties) {
        super(solver, properties);
        iInitialIterationLimit = properties.getPropertyLong("SearchIntensification.IterationLimit",
                iInitialIterationLimit);
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
    public void afterAssigned(long iteration, T value) {
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

    public void solutionUpdated(Solution<V, T> solution) {
    }

    public void bestCleared(Solution<V, T> solution) {
    }

    public void bestRestored(Solution<V, T> solution) {
    }

    public void getInfo(Solution<V, T> solution, Map<String, String> info) {
    }

    public void getInfo(Solution<V, T> solution, Map<String, String> info, List<V> variables) {
    }
}
