package org.cpsolver.ifs.model;

import java.util.Map;

import org.cpsolver.ifs.assignment.Assignment;


/**
 * IFS neighbour.
 * 
 * @see org.cpsolver.ifs.heuristics.NeighbourSelection
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
public interface Neighbour<V extends Variable<V, T>, T extends Value<V, T>> {
    /** Perform assignment 
     * @param assignment current assignment
     * @param iteration current iteration
     **/
    public void assign(Assignment<V, T> assignment, long iteration);

    /** Difference in the evaluation function, if this neighnour is assigned.
     * @param assignment current assignment
     * @return difference in the solution value when assigned
     **/
    public double value(Assignment<V, T> assignment);
    
    /** Return assignments to be done.
     * @return list of assignments (including unassignments) of this neighbour
     **/
    public Map<V, T> assignments(); 
}
