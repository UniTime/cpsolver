package org.cpsolver.studentsct;

import org.apache.log4j.Logger;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.Callback;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;


/**
 * Abstract student sectioning saver class.
 * 
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2014 Tomas Muller<br>
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

public abstract class StudentSectioningSaver implements Runnable {
    private Solver<Request, Enrollment> iSolver = null;
    private Callback iCallback = null;

    /**
     * Constructor
     * @param solver current solver
     */
    public StudentSectioningSaver(Solver<Request, Enrollment> solver) {
        iSolver = solver;
    }

    /** Solver 
     * @return current solver
     **/
    public Solver<Request, Enrollment> getSolver() {
        return iSolver;
    }

    /** Solution to be saved 
     * @return current solution
     **/
    protected Solution<Request, Enrollment> getSolution() {
        return iSolver.currentSolution();
    }

    /** Model of the solution 
     * @return problem model
     **/
    protected StudentSectioningModel getModel() {
        return (StudentSectioningModel) iSolver.currentSolution().getModel();
    }
    
    /** Current assignment 
     * @return current assignment
     **/
    protected Assignment<Request, Enrollment> getAssignment() {
        return iSolver.currentSolution().getAssignment();
    }

    /** Save the solution 
     * @throws Exception thrown when the save fails
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

    @Override
    public void run() {
        try {
            save();
        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e.getMessage(), e);
        } finally {
            if (iCallback != null)
                iCallback.execute();
        }
    }
}
