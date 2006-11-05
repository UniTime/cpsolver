package net.sf.cpsolver.ifs.heuristics;

import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.solution.*;
import net.sf.cpsolver.ifs.solver.*;

/**
 * Value selection criterion.
 * <br><br>
 * After a variable is selected, we need to find a value to be assigned to the variable. This problem is usually called
 * "value selection" in constraint programming. Typically, the most useful advice is to select the best-fit value.
 * So, we are looking for a value which is the most preferred for the variable and which causes the least trouble as well.
 * This means that we need to find a value with the minimal potential for future conflicts with other variables.
 * For example, a value which violates the smallest number of soft constraints can be selected among those with
 * the smallest number of hard conflicts.
 * <br><br>
 * The task of this criterion is to select a value of the given variable which will be assigned to this variable.
 *
 * @see Solver
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
public interface ValueSelection {
    /** Initialization */
    public void init(Solver solver);
    /** Value selection
     * @param solution current solution
     * @param selectedVariable selected variable
     */
    public Value selectValue(Solution solution, Variable selectedVariable);
}
