package org.cpsolver.ifs.assignment.context;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.InheritedAssignment;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;

/**
 * An additional interface that can be implemented by the {@link HasAssignmentContext} class.
 * The inherited assignment context holder (see {@link InheritedAssignmentContextHolder}) can
 * than use this interface to inherit an assignment context from the parent assignment context.
 * If a {@link HasAssignmentContext} class implements this interface, {@link InheritedAssignment}
 * will use the {@link CanInheritContext#inheritAssignmentContext(Assignment, AssignmentContext)} method 
 * instead of the {@link HasAssignmentContext#createAssignmentContext(Assignment)} method to create
 * a new context.   
 * 
 * @see HasAssignmentContext
 * @see InheritedAssignment
 * @see InheritedAssignmentContextHolder
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Iterative Forward Search)<br>
 *          Copyright (C) 2014 Tomas Muller<br>
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
 *          License along with this library; if not see <a href='http://www.gnu.org/licenses'>http://www.gnu.org/licenses</a>.
 * @param <V> Variable
 * @param <T> Value
 * @param <C> Assignment Context
 **/
public interface CanInheritContext <V extends Variable<V, T>, T extends Value<V, T>, C extends AssignmentContext> {
    
    /**
     * Create a new assignment context for the given assignment. This method can be used to
     * clone the parent context if desired.
     * @param assignment an assignment for which there needs to be an assignment context
     * @param parentContext context of the parent assignment
     * @return a new instance of the assignment context, filled according to the given assignment
     */
    public C inheritAssignmentContext(Assignment<V,T> assignment, C parentContext);

}
