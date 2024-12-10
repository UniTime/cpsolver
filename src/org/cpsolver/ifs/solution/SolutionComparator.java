package org.cpsolver.ifs.solution;

import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;

/**
 * IFS solution comparator. <br>
 * <br>
 * The solution comparator compares two solutions: the current solution and the
 * best solution found. This comparison can be based on several criteria. For
 * example, it can lexicographically order solutions according to the number of
 * unassigned variables (smaller number is better) and the number of violated
 * soft constraints.
 * 
 * @see Solution
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
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 * 
 * @param <V> Variable
 * @param <T> Value
 */
public interface SolutionComparator<V extends Variable<V, T>, T extends Value<V, T>> {
    /**
     * Compares two solutions. Returns true if the given solution is better than
     * its best ever found solution (see {@link Solution#saveBest()} and
     * {@link Solution#restoreBest()}).
     * 
     * @param currentSolution
     *            given solution
     * @return true if the given solution is better than the best ever found
     *         solution
     */
    public boolean isBetterThanBestSolution(Solution<V, T> currentSolution);
}
