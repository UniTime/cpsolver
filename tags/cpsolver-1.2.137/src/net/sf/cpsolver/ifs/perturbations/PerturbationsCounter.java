package net.sf.cpsolver.ifs.perturbations;

import java.util.Collection;
import java.util.Map;

import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;

/**
 * Counter of perturbation penalty (minimal perturbation problem). <br>
 * <br>
 * Many real-life problems are dynamic, with changes in the problem definition
 * occurring after a solution to the initial formulation has been reached. A
 * minimal perturbation problem incorporates these changes, along with the
 * initial solution, as a new problem whose solution must be as close as
 * possible to the initial solution. The iterative forward search algorithm is
 * also made to solve minimal perturbation problems. <br>
 * <br>
 * To define the minimal perturbation problem, we will consider an initial
 * (original) problem, its solution, a new problem, and some distance function
 * which allows us to compare solutions of the initial and the new problem.
 * Subsequently we look for a solution of the new problem with minimal distance
 * from the initial solution. This distance is expressed by this
 * PerturbationCounter
 * 
 * @see Solver
 * @see Solution
 * @see Variable
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
public interface PerturbationsCounter<V extends Variable<V, T>, T extends Value<V, T>> {
    /** Initialization */
    public void init(Solver<V, T> solver);

    /**
     * Returns perturbation penalty, i.e., the distance between current solution
     * and the solution of the initial problem (see
     * {@link Variable#getInitialAssignment()}).
     * 
     * @param model
     *            current model
     */
    public double getPerturbationPenalty(Model<V, T> model);

    /**
     * Returns perturbation penalty, i.e., the distance between current solution
     * and the solution of the initial (only include variables from the given
     * set) problem (see {@link Variable#getInitialAssignment()}).
     * 
     * @param model
     *            current model
     */
    public double getPerturbationPenalty(Model<V, T> model, Collection<V> variables);

    /**
     * Returns perturbation penalty of the solution which become from the
     * current solution when given conflicting values are unassigned and the
     * selected value is assigned. Since this penalty is used for comparison of
     * different candidate values in the value selection criterion, it is fully
     * acceptable to just return a difference between current and the altered
     * solution (which might be easied for computation that the whole
     * perturbation penalty).
     * 
     * @param model
     *            current model
     * @param selectedValue
     *            value to be selected in the next iteration
     * @param conflicts
     *            conflicting values to be unassigned in the next iteration
     */
    public double getPerturbationPenalty(Model<V, T> model, T selectedValue, Collection<T> conflicts);

    /**
     * Some (perturbation) information about the solution might be returned
     * here.
     * 
     * @param info
     *            resultant info table
     * @param model
     *            current model
     */
    public void getInfo(Map<String, String> info, Model<V, T> model);

    /**
     * Some (perturbation) information about the solution might be returned here
     * (only include variables from the given set).
     * 
     * @param info
     *            resultant info table
     * @param model
     *            current model
     */
    public void getInfo(Map<String, String> info, Model<V, T> model, Collection<V> variables);
}