package org.cpsolver.studentsct;

import org.apache.log4j.Logger;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.Callback;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;


/**
 * Abstract student sectioning loader class.
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
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

public abstract class StudentSectioningLoader implements Runnable {
    private StudentSectioningModel iModel = null;
    private Assignment<Request, Enrollment> iAssignment = null;
    private Callback iCallback = null;

    /**
     * Constructor
     * 
     * @param model
     *            an empty instance of timetable model
     */
    public StudentSectioningLoader(StudentSectioningModel model, Assignment<Request, Enrollment> assignment) {
        iModel = model;
        iAssignment = assignment;
    }

    /**
     * Returns provided model.
     * 
     * @return provided model
     */
    protected StudentSectioningModel getModel() {
        return iModel;
    }
    
    /**
     * Returns provided assignment.
     * 
     * @return provided assignment
     */
    protected Assignment<Request, Enrollment> getAssignment() {
        return iAssignment;
    }

    /**
     * Load the model.
     */
    public abstract void load() throws Exception;

    /**
     * Sets callback class
     * 
     * @param callback
     *            method {@link Callback#execute()} is executed when load is
     *            done
     */
    public void setCallback(Callback callback) {
        iCallback = callback;
    }

    @Override
    public void run() {
        try {
            load();
        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e.getMessage(), e);
        } finally {
            if (iCallback != null)
                iCallback.execute();
        }
    }

}
