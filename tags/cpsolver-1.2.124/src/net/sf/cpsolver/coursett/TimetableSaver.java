package net.sf.cpsolver.coursett;

import org.apache.log4j.Logger;

import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.Callback;

/**
 * Abstract timetable saver class.
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
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

public abstract class TimetableSaver implements Runnable {
    private Solver<Lecture, Placement> iSolver = null;
    private Callback iCallback = null;

    /**
     * Constructor
     */
    public TimetableSaver(Solver<Lecture, Placement> solver) {
        iSolver = solver;
    }

    /** Solver */
    public Solver<Lecture, Placement> getSolver() {
        return iSolver;
    }

    /** Solution to be saved */
    protected Solution<Lecture, Placement> getSolution() {
        return iSolver.currentSolution();
    }

    /** Model of the solution */
    protected TimetableModel getModel() {
        return (TimetableModel) iSolver.currentSolution().getModel();
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
