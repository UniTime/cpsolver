package org.cpsolver.ifs.extension;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.ModelListener;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;

/**
 * Generic extension of IFS solver. <br>
 * <br>
 * All extensions should extend this class. <br>
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
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
 * @param <V> Variable
 * @param <T> Value
 */
public class Extension<V extends Variable<V, T>, T extends Value<V, T>> implements ModelListener<V, T> {
    private Model<V, T> iModel = null;
    private Solver<V, T> iSolver = null;
    private DataProperties iProperties = null;

    /**
     * Constructor
     * 
     * @param solver
     *            IFS solver
     * @param properties
     *            input configuration
     */
    public Extension(Solver<V, T> solver, DataProperties properties) {
        iSolver = solver;
        iProperties = properties;
    }

    /** Registration of a model. This is called by the solver before start. 
     * @param model problem model
     **/
    public void register(Model<V, T> model) {
        iModel = model;
        iModel.addModelListener(this);
    }

    /**
     * Unregistration of a model. This is called by the solver when extension is
     * removed.
     * @param model problem model
     */
    public void unregister(Model<V, T> model) {
        iModel.removeModelListener(this);
        iModel = null;
    }

    /**
     * Returns true if there is a model registered to this extension, i.e., when
     * extension is registered.
     * @return true if registered with a solver
     */
    public boolean isRegistered() {
        return iModel != null;
    }

    /** Returns the model
     * @return problem model 
     **/
    public Model<V, T> getModel() {
        return iModel;
    }

    /** Returns the solver 
     * @return current solver
     **/
    public Solver<V, T> getSolver() {
        return iSolver;
    }

    /** Returns input configuration
     * @return solver configuration
     **/
    public DataProperties getProperties() {
        return iProperties;
    }

    /** Called after a value is assigned to a variable */
    @Override
    public void afterAssigned(Assignment<V, T> assignment, long iteration, T value) {
    }

    /** Called after a value is unassigned from a variable */
    @Override
    public void afterUnassigned(Assignment<V, T> assignment, long iteration, T value) {
    }

    /** Called before a value is assigned to a variable */
    @Override
    public void beforeAssigned(Assignment<V, T> assignment, long iteration, T value) {
    }

    /** Called after a value is unassigned from a variable */
    @Override
    public void beforeUnassigned(Assignment<V, T> assignment, long iteration, T value) {
    }

    /** Called when a constraint is added to the model */
    @Override
    public void constraintAdded(Constraint<V, T> constraint) {
    }

    /** Called when a constraint is removed from the model */
    @Override
    public void constraintRemoved(Constraint<V, T> constraint) {
    }

    /** Called when a variable is added to the model */
    @Override
    public void variableAdded(V variable) {
    }

    /** Called when a variable is removed from the model */
    @Override
    public void variableRemoved(V variable) {
    }

    /** Initialization -- called before the solver is started */
    @Override
    public boolean init(Solver<V, T> solver) {
        return true;
    }
}