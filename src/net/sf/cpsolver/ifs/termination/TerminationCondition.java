package net.sf.cpsolver.ifs.termination;

import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;

/**
 * Termination condition. <br>
 * <br>
 * The termination condition determines when the algorithm should finish. For
 * example, the solver should terminate when the maximal number of iterations or
 * some other given timeout value is reached. Moreover, it can stop the search
 * process when the current solution is good enough, e.g., all variables are
 * assigned and/or some other solution parameters are in the required ranges.
 * For example, the solver can stop when all variables are assigned and less
 * than 10% of the soft constraints are violated. Termination of the process by
 * the user can also be a part of the termination condition.
 * 
 * @see net.sf.cpsolver.ifs.solver.Solver
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
 **/
public interface TerminationCondition<V extends Variable<V, T>, T extends Value<V, T>> {
    /**
     * Returns true when the solver can continue with the next iteration
     * 
     * @param currentSolution
     *            current solution
     */
    public boolean canContinue(Solution<V, T> currentSolution);
}
