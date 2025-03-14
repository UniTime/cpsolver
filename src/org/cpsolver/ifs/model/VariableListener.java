package org.cpsolver.ifs.model;

import org.cpsolver.ifs.assignment.Assignment;

/**
 * IFS variable listener.
 * 
 * @see Variable
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
 * @param <T> Value
 */
public interface VariableListener<T extends Value<?, T>> {
    /**
     * Called by the variable when a value is assigned to it
     * 
     * @param assignment current assignment
     * @param iteration
     *            current iteration
     * @param value
     *            assigned to the variable
     */
    public void variableAssigned(Assignment<?, T> assignment, long iteration, T value);

    /**
     * Called by the variable when a value is unassigned from it
     * 
     * @param assignment current assignment
     * @param iteration
     *            current iteration
     * @param value
     *            unassigned from the variable
     */
    public void variableUnassigned(Assignment<?, T> assignment, long iteration, T value);

    /**
     * Called by the variable when a value is permanently removed from its
     * domain
     * 
     * @param iteration
     *            current iteration
     * @param value
     *            removed from the variable's domain
     */
    public void valueRemoved(long iteration, T value);
}
