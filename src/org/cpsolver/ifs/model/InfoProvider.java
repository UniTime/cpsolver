package org.cpsolver.ifs.model;

import java.util.Collection;
import java.util.Map;

import org.cpsolver.ifs.assignment.Assignment;


/**
 * A class providing INFO table.
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
public interface InfoProvider<V extends Variable<V, T>, T extends Value<V, T>> {
    /** Adds some information into the table with information about the solution
     * @param assignment current assignment
     * @param info info table
     **/
    public void getInfo(Assignment<V, T> assignment, Map<String, String> info);

    /**
     * Adds some information into the table with information about the solution,
     * only consider variables from the given set
     * @param assignment current assignment
     * @param info info table 
     * @param variables sub-problem
     */
    public void getInfo(Assignment<V, T> assignment, Map<String, String> info, Collection<V> variables);
}