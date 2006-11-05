package net.sf.cpsolver.ifs.solution;

/**
 * IFS solution comparator.
 * <br><br>
 * The solution comparator compares two solutions: the current solution and the best solution found. This comparison can
 * be based on several criteria. For example, it can lexicographically order solutions according to the number of 
 * unassigned variables (smaller number is better) and the number of violated soft constraints.
 *
 * @see Solution
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
 */
public interface SolutionComparator {
    /** Compares two solutions. Returns true if the given solution is better than its best ever found solution (see {@link Solution#saveBest()} and {@link Solution#restoreBest()}).
     * @param currentSolution given solution
     * @return true if the given solution is better than the best ever found solution
     */
    public boolean isBetterThanBestSolution(Solution currentSolution);
}
