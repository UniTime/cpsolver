package net.sf.cpsolver.ifs.termination;

import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * General implementation of termination condition.
 * <br><br>
 * Solver stops when the solution is complete (all varaibles are assigned) or when a timeout
 * is reached (expressed either by the number of iterations or by a time).
 * <br><br>
 * Parameters:
 * <br>
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>Termination.StopWhenComplete</td><td>{@link Double}</td><td>if true, solver stops when a complete solution is found</td></tr>
 * <tr><td>Termination.MaxIters</td><td>{@link Integer}</td><td>if zero or positive, solver stops when the given number of iteration is reached</td></tr>
 * <tr><td>Termination.TimeOut</td><td>{@link Double}</td><td>if zero or positive, solver stops when the given timeout (given in seconds) is reached</td></tr>
 * </table>
 *
 * @see net.sf.cpsolver.ifs.solver.Solver
 *
 * @version
 * IFS 1.0 (Iterative Forward Search)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@ktiml.mff.cuni.cz">muller@ktiml.mff.cuni.cz</a><br>
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
 **/
public class GeneralTerminationCondition implements TerminationCondition {
    protected static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(GeneralTerminationCondition.class);
    private int iMaxIter;
    private double iTimeOut;
    private boolean iStopWhenComplete;
    
    public GeneralTerminationCondition(DataProperties properties) {
        iMaxIter = properties.getPropertyInt("Termination.MaxIters",-1);
        iTimeOut = properties.getPropertyDouble("Termination.TimeOut",  -1.0);
        iStopWhenComplete = properties.getPropertyBoolean("Termination.StopWhenComplete", false);
    }
    
    public boolean canContinue(Solution currentSolution) {
        if (iMaxIter>=0 && currentSolution.getIteration()>=iMaxIter) {
            sLogger.info("Maximum number of iteration reached.");
            return false;
        }
        if (iTimeOut>=0 && currentSolution.getTime()>iTimeOut) {
            sLogger.info("Timeout reached.");
            return false;
        }
        if (iStopWhenComplete || (iMaxIter<0 && iTimeOut<0)) {
            boolean ret = (!currentSolution.getModel().unassignedVariables().isEmpty());
            if (!ret) sLogger.info("Complete solution found.");
            return ret;
        }
        return true;
    }
}
