package net.sf.cpsolver.ifs.solution;

import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * General implementation of solution comparator.
 * <br><br>
 * The solution is better than the best ever found solution when it has more variables assigned. In the case, when both
 * solutions have the same number of assigned variables, the better solution is the one with smaller total value, i.e.,
 * the sum of {@link net.sf.cpsolver.ifs.model.Value#toInt()} over all assigned variables.
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
public class GeneralSolutionComparator implements SolutionComparator {
    
    public GeneralSolutionComparator() {}
    /** No parameters are used so far. */
    public GeneralSolutionComparator(DataProperties properties) {
    }
    
    public boolean isBetterThanBestSolution(Solution currentSolution) {
        if (currentSolution.getBestInfo()==null) return true;
        int unassigned = currentSolution.getModel().unassignedVariables().size();
        if (currentSolution.getModel().getBestUnassignedVariables()!=unassigned)
            return currentSolution.getModel().getBestUnassignedVariables()>unassigned;
        return currentSolution.getModel().getTotalValue()<currentSolution.getBestValue();
    }
    
}
