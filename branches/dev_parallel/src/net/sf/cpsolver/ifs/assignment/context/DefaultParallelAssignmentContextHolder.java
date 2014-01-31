package net.sf.cpsolver.ifs.assignment.context;

import java.util.Arrays;

import net.sf.cpsolver.ifs.assignment.Assignment;
import net.sf.cpsolver.ifs.assignment.DefaultParallelAssignment;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;

/**
 * A simple assignment context holder implementation used by the {@link DefaultParallelAssignment} class.
 * {@link CanHoldContext} are used when possible, storing contexts in arrays, on the
 * {@link DefaultParallelAssignment#getIndex()} position.
 * 
 * @see AssignmentContext
 * @see AssignmentContextReference
 * @see AssignmentContextHolder
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 **/
public class DefaultParallelAssignmentContextHolder<V extends Variable<V, T>, T extends Value<V, T>> extends AssignmentContextHolderMap<V, T> {
    private int iIndex = -1;

    public DefaultParallelAssignmentContextHolder(int threadIndex) {
        iIndex = threadIndex;
    }
    
    /**
     * Returns contexts as an existing, big enough array.
     */
    protected AssignmentContext[] getContexts(CanHoldContext holder) {
        synchronized (holder) {
            AssignmentContext[] contexts = holder.getContext();
            if (contexts == null) {
                contexts = new AssignmentContext[Math.max(10, 1 + iIndex)];
                holder.setContext(contexts);
            } else if (contexts.length <= iIndex) {
                contexts = Arrays.copyOf(contexts, iIndex + 10);
                holder.setContext(contexts);
            }
            return contexts;
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <U extends AssignmentContext> U getAssignmentContext(Assignment<V, T> assignment, AssignmentContextReference<V, T, U> reference) {
        if (iIndex >= 0 && reference.getParent() instanceof CanHoldContext) {
            AssignmentContext[] contexts = getContexts((CanHoldContext)reference.getParent());
            AssignmentContext context = contexts[iIndex];
            if (context != null) return (U) context;
            
            context = reference.getParent().createAssignmentContext(assignment);
            contexts[iIndex] = context;
            
            return (U) context;
        } else {
            return super.getAssignmentContext(assignment, reference);
        }
    }
}