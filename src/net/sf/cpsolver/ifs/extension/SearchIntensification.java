package net.sf.cpsolver.ifs.extension;

import java.util.*;

import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.solution.*;
import net.sf.cpsolver.ifs.solver.*;
import net.sf.cpsolver.ifs.util.*;

/**
 * Go back to the best known solution when no better solution is found within the given amount of iterations.
 *
 * @version
 * IFS 1.1 (Iterative Forward Search)<br>
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
public class SearchIntensification extends Extension implements SolutionListener {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(SearchIntensification.class);
    private long iInitialIterationLimit = 100; 
    private long iIterationLimit = 100; 
    private long iLastReturn = 0;
    private ConflictStatistics iCBS = null;
    private int iResetInterval = 5;
    private int iResetCounter = 0;
    private int iMux = 2;
    private int iMuxInterval = 2;
    private int iMuxCounter = 0;
    
    public SearchIntensification(Solver solver, DataProperties properties) {
        super(solver, properties);
        iInitialIterationLimit = properties.getPropertyLong("SearchIntensification.IterationLimit", iInitialIterationLimit);
        iResetInterval = properties.getPropertyInt("SearchIntensification.ResetInterval", iResetInterval);
        iMuxInterval = properties.getPropertyInt("SearchIntensification.MultiplyInterval", iMuxInterval);
        iMux = properties.getPropertyInt("SearchIntensification.Multiply", iMux);
    }
    
    public boolean init(Solver solver) {
        if (iResetInterval>0) {
            for (Enumeration e=solver.getExtensions().elements();e.hasMoreElements();) {
                Extension ex = (Extension)e.nextElement();
                if (ex instanceof ConflictStatistics) {
                    iCBS = (ConflictStatistics)ex;
                    break;
                }
            }
        }
        return super.init(solver);
    }
    
    public void register(Model model) {
        super.register(model);
        getSolver().currentSolution().addSolutionListener(this);
    }
    
    public void unregister(Model model) {
        super.unregister(model);
        getSolver().currentSolution().removeSolutionListener(this);
    }
    
    public void afterAssigned(long iteration, Value value) {
        if (iIterationLimit>0 && iteration>0) {
            Solution solution = getSolver().currentSolution();
            if (solution.getBestIteration()<0 || !solution.isBestComplete()) return;
            long bestIt = Math.max(solution.getBestIteration(), iLastReturn);
            long currIt = solution.getIteration();
            if (currIt - bestIt > iIterationLimit) {
                iLastReturn = currIt; iResetCounter++; iMuxCounter++;
                sLogger.debug("Going back to the best known solution...");
                solution.restoreBest();
                sLogger.debug("Reset counter set to "+iResetCounter);
                if (iMuxInterval>0 && (iMuxCounter%iMuxInterval)==0) {
                    iIterationLimit *= iMux;
                    sLogger.debug("Iteration limit increased to "+iIterationLimit);
                }
                if (iCBS!=null && iResetInterval>0 && (iResetCounter%iResetInterval)==0) {
                    sLogger.debug("Reseting CBS...");
                    iCBS.reset();
                }
            }
        }
    }
    public void bestSaved(Solution solution) {
        if (iResetCounter>0)
            sLogger.debug("Reset counter set to zero.");
        iResetCounter = 0; iMuxCounter = 0; iIterationLimit = iInitialIterationLimit;
    }
    public void solutionUpdated(Solution solution) {
    }
    public void bestCleared(Solution solution) {
    }
    public void bestRestored(Solution solution) {
    }
    public void getInfo(Solution solution, Dictionary info) {
    }
    public void getInfo(Solution solution, Dictionary info, Vector variables) {
    }
}
