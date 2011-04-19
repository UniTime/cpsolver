package net.sf.cpsolver.ifs.solution;

import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * General implementation of solution comparator. <br>
 * <br>
 * The solution is better than the best ever found solution when it has more
 * variables assigned. In the case, when both solutions have the same number of
 * assigned variables, the better solution is the one with smaller total value,
 * i.e., the sum of {@link net.sf.cpsolver.ifs.model.Value#toDouble()} over all
 * assigned variables.
 * 
 * @see Solution
 * @see net.sf.cpsolver.ifs.solver.Solver
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
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */
public class GeneralSolutionComparator<V extends Variable<V, T>, T extends Value<V, T>> implements
        SolutionComparator<V, T> {

    public GeneralSolutionComparator() {
    }

    /** No parameters are used so far. */
    public GeneralSolutionComparator(DataProperties properties) {
    }

    @Override
    public boolean isBetterThanBestSolution(Solution<V, T> currentSolution) {
        if (currentSolution.getBestInfo() == null)
            return true;
        int unassigned = currentSolution.getModel().nrUnassignedVariables();
        if (currentSolution.getModel().getBestUnassignedVariables() != unassigned)
            return currentSolution.getModel().getBestUnassignedVariables() > unassigned;
        return currentSolution.getModel().getTotalValue() < currentSolution.getBestValue();
    }

}
