package org.cpsolver.ifs.assignment.context;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;

/**
 * A reference to an assignment context. A reference is created by a class requiring an assignment
 * context {@link HasAssignmentContext} by calling {@link Model#createReference(HasAssignmentContext)}.
 * This reference can be stored with the class implementing the {@link HasAssignmentContext}
 * interface in place of the assignment context {@link AssignmentContext}. Assignment context
 * can be than retrieved from an assignment by calling {@link Assignment#getAssignmentContext(AssignmentContextReference)}.
 * This method also create a new context if there is none associated with the assignment yet for
 * the given reference.
 * 
 * <br><br>
 * 
 * Method {@link AssignmentContextReference#getIndex()} can be used to index stored contexts. 
 * A new assignment context can be created for a reference by calling {@link HasAssignmentContext#createAssignmentContext(Assignment)}
 * on the {@link AssignmentContextReference#getParent()}.
 * 
 * @see AssignmentContext
 * @see AssignmentContextReference
 * @see HasAssignmentContext
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
public class AssignmentContextReference <V extends Variable<V, T>, T extends Value<V, T>, C extends AssignmentContext> {
    private HasAssignmentContext<V, T, C> iParent;
    private int iIndex;

    /**
     * Create a reference context
     * @param parent a class implementing the {@link HasAssignmentContext} interface
     * @param index a unique index associated with the reference by the {@link Model}
     **/
    public AssignmentContextReference(HasAssignmentContext<V, T, C> parent, int index) {
        iParent = parent;
        iIndex = index;
    }
    
    /**
     * Return index, a unique number associated with the reference by the {@link Model}.
     * @return an index
     */
    public int getIndex() {
        return iIndex;
    }
    
    @Override
    public int hashCode() {
        return iIndex;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof AssignmentContextReference<?,?,?>))
            return false;
        return getIndex() == ((AssignmentContextReference<?,?,?>)o).getIndex();
    }
    
    /**
     * Return parent class, i.e., the class implementing the {@link HasAssignmentContext} interface.
     * @return parent
     */
    public HasAssignmentContext<V,T,C> getParent() {
        return iParent;
    }
}
