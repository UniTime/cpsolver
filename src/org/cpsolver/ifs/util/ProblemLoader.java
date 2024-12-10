package org.cpsolver.ifs.util;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.termination.TerminationCondition;

/**
 * Abstract problem loader class.
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
public abstract class ProblemLoader<V extends Variable<V, T>, T extends Value<V, T>, M extends Model<V,T>> implements Runnable {
    private M iModel = null;
    private Assignment<V, T> iAssignment = null;
    private Callback iCallback = null;
    private TerminationCondition<V, T> iTermination = null;

    /**
     * Constructor
     * 
     * @param model
     *            an empty instance of timetable model
     * @param assignment current assignment
     */
    public ProblemLoader(M model, Assignment<V, T> assignment) {
        iModel = model;
        iAssignment = assignment;
    }

    /**
     * Returns provided model.
     * 
     * @return provided model
     */
    public M getModel() {
        return iModel;
    }
    
    /**
     * Returns provided assignment
     * @return provided assignment
     */
    public Assignment<V, T> getAssignment() {
        return iAssignment;
    }

    /**
     * Load the model.
     * @throws Exception thrown when the load fails
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
            load();
        } catch (Exception e) {
            org.apache.logging.log4j.LogManager.getLogger(this.getClass()).error(e.getMessage(), e);
        } finally {
            if (iCallback != null)
                iCallback.execute();
        }
    }

}