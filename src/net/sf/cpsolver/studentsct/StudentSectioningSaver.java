package net.sf.cpsolver.studentsct;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.Callback;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;

/**
 * Abstract student sectioning saver class.
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 */

public abstract class StudentSectioningSaver implements Runnable {
    private Solver<Request, Enrollment> iSolver = null;
    private Callback iCallback = null;

    /**
     * Constructor
     */
    public StudentSectioningSaver(Solver<Request, Enrollment> solver) {
        iSolver = solver;
    }

    /** Solver */
    public Solver<Request, Enrollment> getSolver() {
        return iSolver;
    }

    /** Solution to be saved */
    protected Solution<Request, Enrollment> getSolution() {
        return iSolver.currentSolution();
    }

    /** Model of the solution */
    protected StudentSectioningModel getModel() {
        return (StudentSectioningModel) iSolver.currentSolution().getModel();
    }

    /** Save the solution */
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
