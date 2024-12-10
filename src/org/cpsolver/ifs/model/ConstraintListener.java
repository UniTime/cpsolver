package org.cpsolver.ifs.model;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;


/**
 * IFS constraint listener.
 * 
 * @see Constraint
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
public interface ConstraintListener<V extends Variable<V, T>, T extends Value<V, T>> {
    /**
     * Called by the constraint, before a value is assigned to its variable.
     * 
     * @param assignment current assignment
     * @param iteration
     *            current iteration
     * @param constraint
     *            source constraint
     * @param assigned
     *            value which will be assigned to its variable (
     *            {@link Value#variable()})
     * @param unassigned
     *            set of conflicting values which will be unassigned by the
     *            constraint before it assigns the given value
     */
    public void constraintBeforeAssigned(Assignment<V, T> assignment, long iteration, Constraint<V, T> constraint, T assigned, Set<T> unassigned);

    /**
     * Called by the constraint, after a value is assigned to its variable.
     * 
     * @param assignment current assignment
     * @param iteration
     *            current iteration
     * @param constraint
     *            source constraint
     * @param assigned
     *            value which was assigned to its variable (
     *            {@link Value#variable()})
     * @param unassigned
     *            set of conflicting values which were unassigned by the
     *            constraint before it assigned the given value
     */
    public void constraintAfterAssigned(Assignment<V, T> assignment, long iteration, Constraint<V, T> constraint, T assigned, Set<T> unassigned);
}