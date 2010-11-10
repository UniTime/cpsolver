package net.sf.cpsolver.ifs.heuristics;

import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.solution.*;
import net.sf.cpsolver.ifs.solver.*;

/**
 * Variable selection criterion. <br>
 * <br>
 * The IFS algorithm requires a function that selects a variable to be
 * (re)assigned during the current iteration step. This problem is equivalent to
 * a variable selection criterion in constraint programming. There are several
 * guidelines for selecting a variable. In local search, the variable
 * participating in the largest number of violations is usually selected first.
 * In backtracking-based algorithms, the first-fail principle is often used,
 * i.e., a variable whose instantiation is most complicated is selected first.
 * This could be the variable involved in the largest set of constraints or the
 * variable with the smallest domain, etc. <br>
 * <br>
 * We can split the variable selection criterion into two cases. If some
 * variables remain unassigned, the �worst� variable among them is selected,
 * i.e., first-fail principle is applied. This may, for example, be the variable
 * with the smallest domain or with the highest number of hard and/or soft
 * constraints. <br>
 * <br>
 * The second case occurs when all variables are assigned. Because the algorithm
 * does not need to stop when a complete feasible solution is found, the
 * variable selection criterion for such case has to be considered as well. Here
 * all variables are assigned but the solution is not good enough, e.g., in the
 * sense of violated soft constraints. We choose a variable whose change of a
 * value can introduce the best improvement of the solution. It may, for
 * example, be a variable whose value violates the highest number of soft
 * constraints. <br>
 * <br>
 * It is possible for the solution to become incomplete again after such an
 * iteration because a value which is not consistent with all hard constraints
 * can be selected in the value selection criterion. This can also be taken into
 * account in the variable selection heuristics.
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

public interface VariableSelection<V extends Variable<V, T>, T extends Value<V, T>> {
    /** Initialization */
    public void init(Solver<V, T> solver);

    /**
     * Variable selection
     * 
     * @param solution
     *            current solution
     */
    public V selectVariable(Solution<V, T> solution);
}
