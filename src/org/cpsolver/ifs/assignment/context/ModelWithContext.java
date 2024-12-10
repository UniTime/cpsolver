package org.cpsolver.ifs.assignment.context;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;

/**
 * A model with an assignment context. In order to be able to hold multiple assignments in memory
 * it is desired for all the assignment dependent data a constraint may need (to effectively enumerate
 * problem objectives), to store these data in a separate class (implementing the 
 * {@link AssignmentConstraintContext} interface). This context is created by calling
 * {@link ConstraintWithContext#createAssignmentContext(Assignment)} and accessed by
 * {@link ConstraintWithContext#getContext(Assignment)}.
 * 
 * 
 * @see AssignmentContext
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
public abstract class ModelWithContext<V extends Variable<V, T>, T extends Value<V, T>, C extends AssignmentConstraintContext<V, T>> extends Model<V, T> implements HasAssignmentContext<V, T, C>, CanHoldContext {
    
    private AssignmentContextReference<V, T, C> iContextReference = null;
    private AssignmentContext[] iContext = new AssignmentContext[CanHoldContext.sMaxSize];
    
    /**
     * Defines how the context of the model should be automatically updated (i.e., when {@link AssignmentConstraintContext#assigned(Assignment, Value)} and {@link AssignmentConstraintContext#unassigned(Assignment, Value)} are called).
     */
    protected static enum ContextUpdateType {
        /** Update is done before an unassignment and before an assignment. */
        BeforeUnassignedBeforeAssigned,
        /** Update is done after an unassignment and before an assignment. */
        AfterUnassignedBeforeAssigned,
        /** Update is done before an unassignment and after an assignment. */
        BeforeUnassignedAfterAssigned,
        /** Update is done after an unassignment and after an assignment. This is the default. */
        AfterUnassignedAfterAssigned,
        /** Context is to be updated manually. */
        NoUpdate
    }
    private ContextUpdateType iContextUpdateType = ContextUpdateType.BeforeUnassignedAfterAssigned;
    
    public ModelWithContext() {
        super();
        iContextReference = createReference(this);
    }
    
    /**
     * Returns an assignment context associated with this model. If there is no 
     * assignment context associated with this model yet, one is created using the
     * {@link ConstraintWithContext#createAssignmentContext(Assignment)} method. From that time on,
     * this context is kept with the assignment and automatically updated by calling the
     * {@link AssignmentConstraintContext#assigned(Assignment, Value)} and {@link AssignmentConstraintContext#unassigned(Assignment, Value)}
     * whenever a variable is changed.
     * @param assignment given assignment
     * @return assignment context associated with this model and the given assignment
     */
    @Override
    public C getContext(Assignment<V, T> assignment) {
        return AssignmentContextHelper.getContext(this, assignment);
    }

    @Override
    public AssignmentContextReference<V, T, C> getAssignmentContextReference() { return iContextReference; }

    @Override
    public void setAssignmentContextReference(AssignmentContextReference<V, T, C> reference) { iContextReference = reference; }

    @Override
    public AssignmentContext[] getContext() { return iContext; }
    
    @Override
    public void beforeUnassigned(Assignment<V, T> assignment, long iteration, T value) {
        super.beforeUnassigned(assignment, iteration, value);
        switch (getContextUpdateType()) {
            case BeforeUnassignedAfterAssigned:
            case BeforeUnassignedBeforeAssigned:
                getContext(assignment).unassigned(assignment, value);
        }
    }
    
    @Override
    public void afterUnassigned(Assignment<V, T> assignment, long iteration, T value) {
        super.afterUnassigned(assignment, iteration, value);
        switch (getContextUpdateType()) {
            case AfterUnassignedAfterAssigned:
            case AfterUnassignedBeforeAssigned:
                getContext(assignment).unassigned(assignment, value);
        }
    }
    
    @Override
    public void afterAssigned(Assignment<V, T> assignment, long iteration, T value) {
        super.afterAssigned(assignment, iteration, value);
        switch (getContextUpdateType()) {
            case AfterUnassignedAfterAssigned:
            case BeforeUnassignedAfterAssigned:
                getContext(assignment).assigned(assignment, value);
        }
    }
    
    @Override
    public void beforeAssigned(Assignment<V, T> assignment, long iteration, T value) {
        super.beforeAssigned(assignment, iteration, value);
        switch (getContextUpdateType()) {
            case AfterUnassignedBeforeAssigned:
            case BeforeUnassignedBeforeAssigned:
                getContext(assignment).assigned(assignment, value);
        }
    }

    public ContextUpdateType getContextUpdateType() {
        return iContextUpdateType;
    }

    public void setContextUpdateType(ContextUpdateType iContextUpdateType) {
        this.iContextUpdateType = iContextUpdateType;
    }

}
