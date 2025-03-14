package org.cpsolver.ifs.assignment.context;

import java.util.Arrays;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;


/**
 * A simple assignment context holder implementation using array to store all the assignment contexts.
 * {@link AssignmentContextReference#getIndex()} are used to index the array.
 * 
 * @see AssignmentContext
 * @see AssignmentContextReference
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
public class AssignmentContextHolderArray<V extends Variable<V, T>, T extends Value<V, T>> implements AssignmentContextHolder<V, T> {
    private AssignmentContext[] iContexts = new AssignmentContext[1000];

    public AssignmentContextHolderArray() {
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <U extends AssignmentContext> U getAssignmentContext(Assignment<V, T> assignment, AssignmentContextReference<V, T, U> reference) {
        try {
            AssignmentContext context = iContexts[reference.getIndex()];
            if (context != null) return (U) context;
        } catch (ArrayIndexOutOfBoundsException e) {}

        U context = reference.getParent().createAssignmentContext(assignment);
        if (iContexts.length <= reference.getIndex())
            iContexts = Arrays.copyOf(iContexts, reference.getIndex() + 1000);
        iContexts[reference.getIndex()] = context;
        return context;
    }
    
    @Override
    public <C extends AssignmentContext> void clearContext(AssignmentContextReference<V, T, C> reference) {
        try {
            iContexts[reference.getIndex()] = null;
        } catch (ArrayIndexOutOfBoundsException e) {}
    }
}