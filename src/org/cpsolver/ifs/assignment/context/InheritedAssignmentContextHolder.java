package org.cpsolver.ifs.assignment.context;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.InheritedAssignment;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;

/**
 * A variant of the {@link AssignmentContextHolderMap} that is used by the {@link InheritedAssignment}
 * class. If a {@link HasAssignmentContext} class implements the {@link CanInheritContext} interface, 
 * this class will use the {@link CanInheritContext#inheritAssignmentContext(Assignment, AssignmentContext)} method 
 * instead of the {@link HasAssignmentContext#createAssignmentContext(Assignment)} method to create
 * a new context.   
 * 
 * @see HasAssignmentContext
 * @see InheritedAssignment
 * @see CanInheritContext
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
public class InheritedAssignmentContextHolder<V extends Variable<V, T>, T extends Value<V, T>> extends DefaultParallelAssignmentContextHolder<V, T> {
    private long iVersion;
    
    public InheritedAssignmentContextHolder(int index, long version) {
        super(index);
        iVersion = version;
    }

    /**
     * If the {@link AssignmentContextReference#getParent()} implements the {@link CanInheritContext} interface, this 
     * method will use the {@link CanInheritContext#inheritAssignmentContext(Assignment, AssignmentContext)} instead of the
     * {@link HasAssignmentContext#createAssignmentContext(Assignment)} to create a new context. 
     * @param assignment current assignment (must be {@link InheritedAssignment})
     * @param reference assignment context reference
     * @return assignment context for the given assignment and reference
     */
    @Override
    @SuppressWarnings("unchecked")
    public <U extends AssignmentContext> U getAssignmentContext(Assignment<V, T> assignment, AssignmentContextReference<V, T, U> reference) {
        if (iIndex >= 0 && iIndex < CanHoldContext.sMaxSize && reference.getParent() instanceof CanHoldContext) {
            AssignmentContext[] contexts = ((CanHoldContext)reference.getParent()).getContext();

            VersionedContext<U> context = (VersionedContext<U>)contexts[iIndex];
            if (context == null) {
                context = new VersionedContext<U>();
                if (reference.getParent() instanceof CanInheritContext)
                    context.setContent(((CanInheritContext<V, T, U>)reference.getParent()).inheritAssignmentContext(assignment,
                            ((InheritedAssignment<V, T>)assignment).getParentAssignment().getAssignmentContext(reference)), iVersion);
                else
                    context.setContent(reference.getParent().createAssignmentContext(assignment), iVersion);
                contexts[iIndex] = context;
            } else if (!context.isCurrent(iVersion)) {
                if (reference.getParent() instanceof CanInheritContext)
                    context.setContent(((CanInheritContext<V, T, U>)reference.getParent()).inheritAssignmentContext(assignment,
                            ((InheritedAssignment<V, T>)assignment).getParentAssignment().getAssignmentContext(reference)), iVersion);
                else
                    context.setContent(reference.getParent().createAssignmentContext(assignment), iVersion);
            }
            
            return context.getContent();
        } else {
            U context = (U) iContexts.get(reference.getIndex());
            if (context != null) return context;
            
            if (reference.getParent() instanceof CanInheritContext)
                context = ((CanInheritContext<V, T, U>)reference.getParent()).inheritAssignmentContext(assignment,
                        ((InheritedAssignment<V, T>)assignment).getParentAssignment().getAssignmentContext(reference));
            else
                context = reference.getParent().createAssignmentContext(assignment);
            iContexts.put(reference.getIndex(), context);
            
            return context;
        }
    }
    
    public static class VersionedContext<U extends AssignmentContext> implements AssignmentContext {
        U iContent = null;
        long iContentVersion = -1;
        
        VersionedContext() {}
        
        public U getContent() {
            return iContent;
        }
        
        public void setContent(U content, long version) {
            iContent = content;
            iContentVersion = version;
        }
        
        public boolean isCurrent(long version) {
            return iContentVersion == version;
        }
    }
}
