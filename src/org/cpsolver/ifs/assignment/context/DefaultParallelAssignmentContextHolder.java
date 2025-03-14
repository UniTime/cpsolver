package org.cpsolver.ifs.assignment.context;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.DefaultParallelAssignment;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;


/**
 * A simple assignment context holder implementation used by the {@link DefaultParallelAssignment} class.
 * {@link CanHoldContext} are used when possible, storing contexts in arrays, on the
 * {@link DefaultParallelAssignment#getIndex()} position.
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
public class DefaultParallelAssignmentContextHolder<V extends Variable<V, T>, T extends Value<V, T>> extends AssignmentContextHolderMap<V, T> {
    protected int iIndex = -1;

    public DefaultParallelAssignmentContextHolder(int threadIndex) {
        iIndex = threadIndex;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <U extends AssignmentContext> U getAssignmentContext(Assignment<V, T> assignment, AssignmentContextReference<V, T, U> reference) {
        if (iIndex >= 0 && iIndex < CanHoldContext.sMaxSize && reference.getParent() instanceof CanHoldContext) {
            AssignmentContext[] contexts = ((CanHoldContext)reference.getParent()).getContext();
            U context = (U)contexts[iIndex];
            
            if (context == null) {
                context = reference.getParent().createAssignmentContext(assignment);
                contexts[iIndex] = context;
            }
            
            return context;
        } else {
            return super.getAssignmentContext(assignment, reference);
        }
    }
    
    @Override
    public <C extends AssignmentContext> void clearContext(AssignmentContextReference<V, T, C> reference) {
        if (iIndex >= 0 && iIndex < CanHoldContext.sMaxSize && reference.getParent() instanceof CanHoldContext) {
            AssignmentContext[] contexts = ((CanHoldContext)reference.getParent()).getContext();
            contexts[iIndex] = null;
        } else {
            super.clearContext(reference);
        }
    }
}