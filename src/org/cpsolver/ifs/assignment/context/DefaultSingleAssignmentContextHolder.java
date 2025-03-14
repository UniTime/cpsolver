package org.cpsolver.ifs.assignment.context;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.DefaultSingleAssignment;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;

/**
 * A simple assignment context holder implementation used by the {@link DefaultSingleAssignment} class.
 * {@link CanHoldContext} are used when possible, storing contexts in arrays of length 1 (one context per
 * {@link HasAssignmentContext} class).
 * 
 * @see AssignmentContext
 * @see AssignmentContextReference
 * @see AssignmentContextHolder
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
public class DefaultSingleAssignmentContextHolder<V extends Variable<V, T>, T extends Value<V, T>> extends AssignmentContextHolderMap<V, T> {

    public DefaultSingleAssignmentContextHolder() {
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <U extends AssignmentContext> U getAssignmentContext(Assignment<V, T> assignment, AssignmentContextReference<V, T, U> reference) {
        if (reference.getParent() instanceof CanHoldContext) {
            AssignmentContext[] contexts = ((CanHoldContext)reference.getParent()).getContext();
            if (contexts[0] == null)
                contexts[0] = reference.getParent().createAssignmentContext(assignment);
            return (U)contexts[0];
        } else {
            return super.getAssignmentContext(assignment, reference);
        }
    }
    
    @Override
    public <C extends AssignmentContext> void clearContext(AssignmentContextReference<V, T, C> reference) {
        if (reference.getParent() instanceof CanHoldContext) {
            AssignmentContext[] contexts = ((CanHoldContext)reference.getParent()).getContext();
            contexts[0] = null;
        } else {
            super.clearContext(reference);
        }
    }
}