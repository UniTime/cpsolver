package net.sf.cpsolver.ifs.extension;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.ModelListener;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Generic extension of IFS solver. <br>
 * <br>
 * All extensions should extend this class. <br>
 * <br>
 * An extension may use extra information associated with a variable or a value
 * (see {@link Variable#setExtra(Object)}, {@link Variable#getExtra()},
 * {@link Value#setExtra(Object)}, {@link Value#getExtra()}) but there can be
 * only one extension using these extra objects used during the search. For
 * instance, {@link MacPropagation} is using these extra objects to memorize
 * explanations.
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

    /** Registration of a model. This is called by the solver before start. */
    public void register(Model<V, T> model) {
        iModel = model;
        iModel.addModelListener(this);
    }

    /**
     * Unregistration of a model. This is called by the solver when extension is
     * removed.
     */
    public void unregister(Model<V, T> model) {
        iModel.removeModelListener(this);
        iModel = null;
    }

    /**
     * Returns true if there is a model registered to this extension, i.e., when
     * extension is registered.
     */
    public boolean isRegistered() {
        return iModel != null;
    }

    /** Returns the model */
    public Model<V, T> getModel() {
        return iModel;
    }

    /** Returns the solver */
    public Solver<V, T> getSolver() {
        return iSolver;
    }

    /** Returns input configuration */
    public DataProperties getProperties() {
        return iProperties;
    }

    /** Called after a value is assigned to a variable */
    public void afterAssigned(long iteration, T value) {
    }

    /** Called after a value is unassigned from a variable */
    public void afterUnassigned(long iteration, T value) {
    }

    /** Called before a value is assigned to a variable */
    public void beforeAssigned(long iteration, T value) {
    }

    /** Called after a value is unassigned from a variable */
    public void beforeUnassigned(long iteration, T value) {
    }

    /** Called when a constraint is added to the model */
    public void constraintAdded(Constraint<V, T> constraint) {
    }

    /** Called when a constraint is removed from the model */
    public void constraintRemoved(Constraint<V, T> constraint) {
    }

    /** Called when a variable is added to the model */
    public void variableAdded(V variable) {
    }

    /** Called when a variable is removed from the model */
    public void variableRemoved(V variable) {
    }

    /** Initialization -- called before the solver is started */
    public boolean init(Solver<V, T> solver) {
        return true;
    }

    /**
     * Should return true when {@link Value#setExtra(Object)},
     * {@link Value#getExtra()} are used by the extension
     */
    public boolean useValueExtra() {
        return false;
    }

    /**
     * Should return true when {@link Variable#setExtra(Object)},
     * {@link Variable#getExtra()} are used by the extension
     */
    public boolean useVariableExtra() {
        return false;
    }
}