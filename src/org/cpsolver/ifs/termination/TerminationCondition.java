package org.cpsolver.ifs.termination;

import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;

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
 * @see org.cpsolver.ifs.solver.Solver
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
 *          License along with this library; if not see <a href='http://www.gnu.org/licenses'>http://www.gnu.org/licenses</a>.
 *
 * @param <V> Variable
 * @param <T> Value
 **/
public interface TerminationCondition<V extends Variable<V, T>, T extends Value<V, T>> {
    /**
     * Returns true when the solver can continue with the next iteration
     * 
     * @param currentSolution
     *            current solution
     * @return true if the solver can continue with the next iteration
     */
    public boolean canContinue(Solution<V, T> currentSolution);
}
