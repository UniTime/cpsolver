package org.cpsolver.ifs.assignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cpsolver.ifs.assignment.context.AssignmentContext;
import org.cpsolver.ifs.assignment.context.AssignmentContextHolder;
import org.cpsolver.ifs.assignment.context.AssignmentContextReference;
import org.cpsolver.ifs.assignment.context.HasAssignmentContext;
import org.cpsolver.ifs.constant.ConstantVariable;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;


/**
 * An abstract implementation of an {@link Assignment} object. It contains an instance of
 * a given assignment context holder (see {@link AssignmentContextHolder}) and 
 * implements the assignment logic. But the actual process of storing and retrieving values
 * is left on the assignment implementation.
 * 
 * @see Assignment
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
public abstract class AssignmentAbstract<V extends Variable<V, T>, T extends Value<V, T>> implements Assignment<V, T> {
    protected AssignmentContextHolder<V, T> iContexts;
    protected boolean iHasInitialzedContext = false;
    
    /**
     * Constructor
     * @param contexts an instance of the assignment context holder
     */
    public AssignmentAbstract(AssignmentContextHolder<V, T> contexts) {
        iContexts = contexts;
    }
    
    /**
     * Checks if the variable is {@link ConstantVariable}, returns {@link AssignmentAbstract#getValueInternal(Variable)}
     * if the variable is not a constant.
     */
    @Override
    @SuppressWarnings("unchecked")
    public T getValue(V variable) {
        if (variable instanceof ConstantVariable<?> && ((ConstantVariable<?>)variable).isConstant())
            return ((ConstantVariable<T>)variable).getConstantValue();
        return getValueInternal(variable);
    }

    /**
     * Returns assignment of a variable, null if not assigned. To be implemented.
     * @param variable a variable in question
     * @return assigned value
     **/
    protected abstract T getValueInternal(V variable);
    
    /**
     * Sets an assignment to a variable (unassigns a variable if the given value is null). To be implemented.
     * @param iteration current iteration
     * @param variable a variable to be assigned
     * @param value new assignment, null if to be unassigned
     **/
    protected abstract void setValueInternal(long iteration, V variable, T value);
    
    /** Assigns a variable with the given value. All the appropriate classes are notified about the change.
     * It is using {@link AssignmentAbstract#setValueInternal(long, Variable, Value)} to store the new 
     * assignment.
     * @param iteration current iteration
     * @param variable a variable
     * @param value one of its values, null if the variable is to be unassigned
     * @return previous assignment of the variable 
     **/
    @SuppressWarnings("unchecked")
    protected T assign(long iteration, V variable, T value) {
        if (variable instanceof ConstantVariable<?> && ((ConstantVariable<?>)variable).isConstant())
            return ((ConstantVariable<T>)variable).getConstantValue();

        assert variable.getModel() != null && (value == null || variable.equals(value.variable()));
        Model<V, T> model = variable.getModel();
        
        // ensure all model, criterion, and constraint assignment contexts are initialized before changing the assignment value
        ensureInitializedContext(variable);
        
        // unassign old value, if assigned
        T old = getValueInternal(variable);
        if (old != null) {
            if (old.equals(value)) return old;
            if (model != null)
                model.beforeUnassigned(this, iteration, old);
            setValueInternal(iteration, variable, null);
            for (Constraint<V, T> constraint : variable.constraints())
                constraint.unassigned(this, iteration, old);
            if (model != null)
                for (GlobalConstraint<V, T> constraint : model.globalConstraints())
                    constraint.unassigned(this, iteration, old);
            variable.variableUnassigned(this, iteration, old);
            if (model != null)
                model.afterUnassigned(this, iteration, old);
        }
        
        // assign new value, if provided
        if (value != null) {
            if (model != null)
                model.beforeAssigned(this, iteration, value);
            setValueInternal(iteration, variable, value);
            for (Constraint<V, T> constraint : variable.constraints())
                constraint.assigned(this, iteration, value);
            if (model != null)
                for (GlobalConstraint<V, T> constraint : model.globalConstraints())
                    constraint.assigned(this, iteration, value);
            variable.variableAssigned(this, iteration, value);
            if (model != null)
                model.afterAssigned(this, iteration, value);
        }
        
        // return old value
        return old;
    }
    
    @Override
    public T assign(long iteration, T value) {
        return assign(iteration, value.variable(), value);
    }
    
    @Override
    public T unassign(long iteration, V variable) {
        return assign(iteration, variable, null);
    }
    
    @Override
    public T unassign(long iteration, V variable, T value) {
        T current = getValue(variable);
        if (current == null && value == null) {
            return current;
        } else if (current != null && current.equals(value)) {
            return current;
        } else {
            return assign(iteration, variable, null);
        }
    }
    
    @Override
    public int nrAssignedVariables() {
        return assignedVariables().size();
    }
    
    @Override
    public Collection<T> assignedValues() {
        List<T> values = new ArrayList<T>();
        for (V variable: assignedVariables())
            values.add(getValueInternal(variable));
        return values;
    }

    @Override
    public Collection<V> unassignedVariables(Model<V, T> model) {
        List<V> unassigned = new ArrayList<V>();
        for (V variable: model.variables())
            if (getValue(variable) == null)
                unassigned.add(variable);
        return unassigned;
    }

    @Override
    public int nrUnassignedVariables(Model<V, T> model) {
        return model.variables().size() - nrAssignedVariables();
    }

    @Override
    public <C extends AssignmentContext> C getAssignmentContext(AssignmentContextReference<V, T, C> reference) {
        return iContexts.getAssignmentContext(this, reference);
    }
    
    @Override
    public <C extends AssignmentContext> void clearContext(AssignmentContextReference<V, T, C> reference) {
        iContexts.clearContext(reference);
    }
    
    @Override
    public int getIndex() {
        return -1;
    }
    
    /**
     * Ensure that the model, all criteria, all global constraints and all the related constraints have their assignment contexts initialized.
     * @param variable a variable to be changed
     */
    @SuppressWarnings("unchecked")
    protected void ensureInitializedContext(V variable) {
        if (!iHasInitialzedContext && variable.getModel() != null) {
            if (variable.getModel() instanceof HasAssignmentContext)
                iContexts.getAssignmentContext(this, ((HasAssignmentContext<V, T, ?>)variable.getModel()).getAssignmentContextReference());
            for (Criterion<V, T> criterion: variable.getModel().getCriteria())
                if (criterion instanceof HasAssignmentContext)
                    iContexts.getAssignmentContext(this, ((HasAssignmentContext<V, T, ?>)criterion).getAssignmentContextReference());
            for (GlobalConstraint<V, T> constraint: variable.getModel().globalConstraints())
                if (constraint instanceof HasAssignmentContext)
                    iContexts.getAssignmentContext(this, ((HasAssignmentContext<V, T, ?>)constraint).getAssignmentContextReference());
            iHasInitialzedContext = true;
        }
        for (Constraint<V, T> constraint: variable.constraints())
            if (constraint instanceof HasAssignmentContext)
                iContexts.getAssignmentContext(this, ((HasAssignmentContext<V, T, ?>)constraint).getAssignmentContextReference());
    }
}
