package org.cpsolver.ifs.model;

import org.cpsolver.ifs.assignment.Assignment;

/**
 * Interface of a constraint that weakens with the time.
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
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
public interface WeakeningConstraint<V extends Variable<V, T>, T extends Value<V, T>> {
    /**
     * Weaken the constraint. This method is called if the constraint
     * participates in an unassigned of a variable.
     * @param assignment current assignment
     */
    public void weaken(Assignment<V, T> assignment);
    
    
    /**
     * Weaken the constraint enough so that it can assign the given
     * value.
     * @param assignment current assignment
     * @param value a conflicting value to be assigned
     */
    public void weaken(Assignment<V, T> assignment, T value);
}
