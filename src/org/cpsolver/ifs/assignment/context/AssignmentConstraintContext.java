package org.cpsolver.ifs.assignment.context;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;

/**
 * An extension of the simple {@link AssignmentContext} which is used by the
 * {@link ConstraintWithContext} class. The notification methods assigned and unassigned 
 * are automatically called by the constraint whenever a variable of the constraint
 * is assigned or unassigned respectively.
 * 
 * @see ConstraintWithContext
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
 **/
public interface AssignmentConstraintContext<V extends Variable<V, T>, T extends Value<V, T>> extends AssignmentContext {
    
    /**
     * Called when {@link ConstraintWithContext#assigned(Assignment, long, Value)} is called to update
     * the content of the context.
     * @param assignment current assignment (with which this context is associated)
     * @param value assigned value
     */
    public void assigned(Assignment<V,T> assignment, T value);
    
    /**
     * Called when {@link ConstraintWithContext#unassigned(Assignment, long, Value)} is called to update
     * the content of the context.
     * @param assignment current assignment (with which this context is associated)
     * @param value unassigned value
     */
    public void unassigned(Assignment<V,T> assignment, T value);

}
