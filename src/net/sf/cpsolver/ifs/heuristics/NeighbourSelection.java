package net.sf.cpsolver.ifs.heuristics;

import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;

/**
 * Neighbour selection criterion. <br>
 * <br>
 * In each iteration of the solver, a neighbour is selected and assigned (by
 * default {@link StandardNeighbourSelection} is employed).
 * 
 * @see Solver
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 **/
public interface NeighbourSelection<V extends Variable<V, T>, T extends Value<V, T>> {
    /** Criterion initialization */
    public void init(Solver<V, T> solver);

    /**
     * select a neighbour of a given solution
     * 
     * @param solution
     *            given solution
     * @return a neighbour assignment
     */
    public Neighbour<V, T> selectNeighbour(Solution<V, T> solution);

}
