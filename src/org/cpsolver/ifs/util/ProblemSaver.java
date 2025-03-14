package org.cpsolver.ifs.util;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.termination.TerminationCondition;

/**
 * Abstract problem saver class.
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2016 Tomas Muller<br>
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
public abstract class ProblemSaver<V extends Variable<V, T>, T extends Value<V, T>, M extends Model<V,T>> implements Runnable {
    private Solver<V, T> iSolver = null;
    private Callback iCallback = null;
    private TerminationCondition<V, T> iTermination = null;

    /**
     * Constructor
     * @param solver current solver
     */
    public ProblemSaver(Solver<V, T> solver) {
        iSolver = solver;
    }

    /** Solver 
     * @return current solver
     **/
    public Solver<V, T> getSolver() {
        return iSolver;
    }
    
    /** Solution to be saved 
     * @return current solution
     **/
    protected Solution<V, T> getSolution() {
        return iSolver.currentSolution();
    }

    /** Model of the solution 
     * @return problem model
     **/
    @SuppressWarnings("unchecked")
    public M getModel() {
        return (M)iSolver.currentSolution().getModel();
    }

    /** Current assignment 
     * @return current assignment
     **/
    public Assignment<V, T> getAssignment() {
        return getSolution().getAssignment();
    }

    /** Save the solution 
     * @throws Exception thrown when save fails
     **/
    public abstract void save() throws Exception;

    /**
     * Sets callback class
     * 
     * @param callback
     *            method {@link Callback#execute()} is executed when save is
     *            done
     */
    public void setCallback(Callback callback) {
        iCallback = callback;
    }
    
    /**
     * Provide termination condition so that the save process can be stopped if needed (optional).
     */
    public void setTerminationCondition(TerminationCondition<V, T> termination) {
        iTermination = termination;
    }
    
    /**
     * Return termination condition so that the save process can be stopped if needed.
     */
    public TerminationCondition<V, T> getTerminationCondition() {
        return iTermination;
    }

    @Override
    public void run() {
        try {
            save();
        } catch (Exception e) {
            org.apache.logging.log4j.LogManager.getLogger(this.getClass()).error(e.getMessage(), e);
        } finally {
            if (iCallback != null)
                iCallback.execute();
        }
    }
}