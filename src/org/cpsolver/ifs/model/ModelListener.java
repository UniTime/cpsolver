package org.cpsolver.ifs.model;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.solver.Solver;

/**
 * IFS model listener.
 * 
 * @see Model
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
 *
 * @param <V> Variable 
 * @param <T> Value
 */
public interface ModelListener<V extends Variable<V, T>, T extends Value<V, T>> {
    /**
     * Variable is added to the model
     * 
     * @param variable
     *            added variable
     */
    public void variableAdded(V variable);

    /**
     * Variable is removed from the model
     * 
     * @param variable
     *            removed variable
     */
    public void variableRemoved(V variable);

    /**
     * Constraint is added to the model
     * 
     * @param constraint
     *            added constraint
     */
    public void constraintAdded(Constraint<V, T> constraint);

    /**
     * Constraint is removed from the model
     * 
     * @param constraint
     *            removed constraint
     */
    public void constraintRemoved(Constraint<V, T> constraint);

    /**
     * Called before a value is assigned to its variable (
     * {@link Value#variable()}).
     * 
     * @param assignment current assignment
     * @param iteration
     *            current iteration
     * @param value
     *            value to be assigned
     */
    public void beforeAssigned(Assignment<V, T> assignment, long iteration, T value);

    /**
     * Called before a value is unassigned from its variable (
     * {@link Value#variable()}).
     * 
     * @param assignment current assignment
     * @param iteration
     *            current iteration
     * @param value
     *            value to be unassigned
     */
    public void beforeUnassigned(Assignment<V, T> assignment, long iteration, T value);

    /**
     * Called after a value is assigned to its variable (
     * {@link Value#variable()}).
     * 
     * @param assignment current assignment
     * @param iteration
     *            current iteration
     * @param value
     *            value to be assigned
     */
    public void afterAssigned(Assignment<V, T> assignment,  long iteration, T value);

    /**
     * Called after a value is unassigned from its variable (
     * {@link Value#variable()}).
     * 
     * @param assignment current assignment
     * @param iteration
     *            current iteration
     * @param value
     *            value to be unassigned
     */
    public void afterUnassigned(Assignment<V, T> assignment, long iteration, T value);

    /**
     * Notification that the model was initialized by the solver.
     * 
     * @param solver
     *            IFS solver
     * @return true if successfully initialized
     */
    public boolean init(Solver<V, T> solver);
}
