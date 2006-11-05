package net.sf.cpsolver.ifs.perturbations;

import java.util.*;

import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;

/**
 * Counter of perturbation penalty (minimal perturbation problem).
 * <br><br>
 * Many real-life problems are dynamic, with changes in the problem definition occurring after a solution to the initial 
 * formulation has been reached. A minimal perturbation problem incorporates these changes, along with the initial 
 * solution, as a new problem whose solution must be as close as possible to the initial solution. The iterative forward 
 * search algorithm is also made to solve minimal perturbation problems. 
 * <br><br>
 * To define the minimal perturbation problem, we will consider an initial (original) problem, its solution, a new 
 * problem, and some distance function which allows us to compare solutions of the initial and the new problem. 
 * Subsequently we look for a solution of the new problem with minimal distance from the initial solution. This
 * distance is expressed by this PerturbationCounter
 *
 * @see Solver
 * @see Solution
 * @see Variable
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
public interface PerturbationsCounter {
    /** Initialization */
    public void init(Solver solver);
    
    /** Returns perturbation penalty, i.e., the distance between current solution and the solution of the initial 
     * problem (see {@link Variable#getInitialAssignment()}).
     * @param solution current solution
     */
    public double getPerturbationPenalty(Solution solution);
    
    /** Returns perturbation penalty of the solution which become from the current solution when given conflicting 
     * values are unassigned and the selected value is assigned. Since this penalty is used for comparison of 
     * different candidate values in the value selection criterion, it is fully acceptable to just return a difference
     * between current and the altered solution (which might be easied for computation that the whole perturbation penalty).
     * @param solution current solution
     * @param selectedValue value to be selected in the next iteration
     * @param conflicts conflicting values to be unassigned in the next iteration
     */
    public double getPerturbationPenalty(Solution solution, Value selectedValue, Collection conflicts);
    
    /** Some (perturbation) information about the solution might be returned here.
     * @param info resultant info table
     * @param solution current solution
     */
    public void getInfo(Dictionary info, Solution solution);
}