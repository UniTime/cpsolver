package org.cpsolver.ifs.assignment.context;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;

/**
 * An abstract implementation of an assignment context holding class.
 * This works much like {@link ConstraintWithContext} or {@link VariableWithContext}, however,
 * it is not tight to a particular class type. Instead {@link AbstractClassWithContext#getModel()}
 * needs to be implemented.
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
public abstract class AbstractClassWithContext<V extends Variable<V, T>, T extends Value<V, T>, C extends AssignmentContext> implements HasAssignmentContext<V, T, C>, CanHoldContext {
    private AssignmentContextReference<V, T, C> iContextReference = null;
    private AssignmentContext[] iContext = new AssignmentContext[CanHoldContext.sMaxSize];
    private C iSingleContextWhenNoModel = null;
  
    /**
     * Returns an assignment context associated with this object. If there is no 
     * assignment context associated with this object yet, one is created using the
     * {@link ConstraintWithContext#createAssignmentContext(Assignment)} method.
     * @param assignment given assignment
     * @return assignment context associated with this object and the given assignment
     */
    @Override
    public C getContext(Assignment<V, T> assignment) {
        if (getModel() == null) {
            if (iSingleContextWhenNoModel == null)
                iSingleContextWhenNoModel = createAssignmentContext(assignment);
            return iSingleContextWhenNoModel;
        }
        return AssignmentContextHelper.getContext(this, assignment);
    }
    
    @Override
    public synchronized AssignmentContextReference<V, T, C> getAssignmentContextReference() {
        if (iContextReference == null)
            iContextReference = getModel().createReference(this);
        return iContextReference;
    }

    @Override
    public void setAssignmentContextReference(AssignmentContextReference<V, T, C> reference) { iContextReference = reference; }

    @Override
    public AssignmentContext[] getContext() { return iContext; }

    /**
     * Get the model. This is used to create an assignment context if needed.
     * @return model
     */
    public abstract Model<V,T> getModel();
}
