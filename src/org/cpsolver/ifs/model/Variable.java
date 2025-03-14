package org.cpsolver.ifs.model;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.DefaultParallelAssignment;
import org.cpsolver.ifs.assignment.DefaultSingleAssignment;
import org.cpsolver.ifs.assignment.EmptyAssignment;
import org.cpsolver.ifs.assignment.context.CanHoldContext;
import org.cpsolver.ifs.util.IdGenerator;


/**
 * Generic variable. <br>
 * <br>
 * Besides a domain of values, a variable also contains information about
 * assigned value, the value assigned in the best ever found solution and also
 * the initial value (minimal perturbations problem). It also knows what
 * constraints are associated with this variable and has a unique id.
 * 
 * @see Value
 * @see Model
 * @see org.cpsolver.ifs.solver.Solver
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
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
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 * 
 * @param <V> Variable 
 * @param <T> Value
 */
public class Variable<V extends Variable<V, T>, T extends Value<V, T>> implements Comparable<V> {
    private static IdGenerator iIdGenerator = new IdGenerator();

    protected long iId = -1;
    private Model<V, T> iModel = null;

    private T iInitialValue = null; // initial value
    /** Assigned value */
    protected T iValue = null; // assigned value
    @SuppressWarnings("unchecked")
    private Value<V, T>[] iAssignedValues = (Value<V, T>[])Array.newInstance(Value.class, CanHoldContext.sMaxSize); // assigned values
    private T iBestValue = null; // best value
    private long iBestAssignmentIteration = 0;
    private List<T> iValues = null;

    private T iRecentlyRemovedValue = null;
    private long iLastIteration = 0;
    private Object iExtra = null;

    private List<Constraint<V, T>> iConstraints = new ArrayList<Constraint<V, T>>();
    private List<Constraint<V, T>> iHardConstraints = new ArrayList<Constraint<V, T>>();
    private List<Constraint<V, T>> iSoftConstraints = new ArrayList<Constraint<V, T>>();
    private List<VariableListener<T>> iVariableListeners = null;

    private Map<V, List<Constraint<V, T>>> iConstraintVariables = null;
    protected int iIndex = -1;

    /** Constructor */
    public Variable() {
        this(null);
    }

    /**
     * Constructor
     * 
     * @param initialValue
     *            initial value (minimal-perturbation problem)
     */
    public Variable(T initialValue) {
        iId = iIdGenerator.newId();
        setInitialAssignment(initialValue);
    }

    /** Model, the variable belong to 
     * @return problem model
     **/
    public Model<V, T> getModel() {
        return iModel;
    }

    /** Set the model to which the variable belongs to 
     * @param model problem model
     **/
    public void setModel(Model<V, T> model) {
        iModel = model;
    }
    
    /** Variable's domain, use {@link Variable#values(Assignment)} instead. 
     * @return all possible values of this variable
     **/
    @Deprecated
    public List<T> values() {
        return values(new EmptyAssignment<V, T>());
    }

    /** Variable's domain 
     * @param assignment current assignment (if the domain is dependent on the current assignment)
     * @return all possible values of this variable
     **/
    public List<T> values(Assignment<V, T> assignment) {
        return iValues;
    }

    /** Sets the domain 
     * @param values variable's domain to cache 
     **/
    protected void setValues(List<T> values) {
        iValues = values;
    }

    /** True, if the variable's domain is not empty 
     * @return true if there is at least one value in the domain 
     **/
    @Deprecated
    public boolean hasValues() {
        return !values().isEmpty();
    }

    /**
     * Returns current assignment.
     * Use {@link Assignment#getValue(Variable)} or {@link Variable#getAssignment(Assignment)} instead.
     * @return currently assigned value
     **/
    @Deprecated
    public T getAssignment() {
        return iValue;
    }

    /**
     * Returns true if the variable is assigned.
     * Use {@link Variable#hasAssignment(Assignment)} instead.
     * @return true if currently assigned
     **/
    @Deprecated
    public boolean hasAssignment() {
        return iValue != null;
    }
    
    /** Returns current assignment 
     * @param assignment current assignment
     * @return currently assigned value
     **/
    @SuppressWarnings("unchecked")
    public T getAssignment(Assignment<V, T> assignment) {
        return assignment.getValue((V) this);
    }
    
    /** Returns true if the variable is assigned
     * @param assignment current assignment
     * @return true if currently assigned
     **/
    public boolean hasAssignment(Assignment<V, T> assignment) {
        return getAssignment(assignment) != null;
    }
    
    /**
     * Sets current assignment.
     * BEWARE: Do not use outside of {@link DefaultSingleAssignment}.
     * @param value current assignment
     **/
    @Deprecated
    public void setAssignment(T value) {
        iValue = value;
    }
    
    /**
     * Returns current assignments.
     * BEWARE: Do not use outside of {@link DefaultParallelAssignment}.
     * @return currently assigned values
     **/
    @Deprecated
    public Value<V, T>[] getAssignments() {
        return iAssignedValues;
    }

    /** Returns initial assignment 
     * @return initial assignment (for the minimal perturbation problem)
     **/
    public T getInitialAssignment() {
        return iInitialValue;
    }

    /** Sets initial assignment
     * @param initialValue initial assignment
     **/
    public void setInitialAssignment(T initialValue) {
        iInitialValue = initialValue;
        if (iInitialValue != null && iInitialValue.variable() == null)
            iInitialValue.setVariable(this);
        if (iModel != null)
            iModel.invalidateVariablesWithInitialValueCache();
    }

    /** Returns true if the variable has an initial assignment 
     * @return true if this variable has an initial assignment  
     **/
    public boolean hasInitialAssignment() {
        return iInitialValue != null;
    }

    /**
     * Assign value to this variable. If the variable has already assigned
     * another value, it is unassigned first. Also, all conflicting values are
     * unassigned before the given value is assigned to this variable.
     * Use {@link Assignment#assign(long, Value)} instead.
     * 
     * @param iteration
     *            current iteration
     * @param value
     *            the value to be assigned
     */
    @Deprecated
    public void assign(int iteration, T value) {
        if (iRecentlyRemovedValue != null && iRecentlyRemovedValue.equals(value)) {
            iRecentlyRemovedValue = null;
            return;
        }
        getModel().getDefaultAssignment().assign(iteration, value);
    }
    
    /**
     * Unassign value from this variable.
     * Use {@link Assignment#unassign(long, Variable)} instead.
     * 
     * @param iteration
     *            current iteration
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public void unassign(int iteration) {
        getModel().getDefaultAssignment().unassign(iteration, (V) this);
    }

    
    /**
     * Returns iteration of the last assignment.
     * Use {@link Assignment#getIteration(Variable)} instead.
     * @return iteration of the last assignment 
     **/
    @Deprecated
    public long getLastIteration() {
        return iLastIteration;
    }
    
    /**
     * Sets iteration of the last assignment.
     * BEWARE: Do not use outside of {@link DefaultSingleAssignment}.
     * @param iteration current iteration
     **/
    @Deprecated
    public void setLastIteration(long iteration) {
        iLastIteration = iteration;
    }

    /**
     * A value was assigned to this variable
     * @param assignment current assignment
     * @param iteration current iteration
     * @param value assigned value
     */
    public void variableAssigned(Assignment<V, T> assignment, long iteration, T value) {
        if (iVariableListeners != null)
            for (VariableListener<T> listener : iVariableListeners) {
                listener.variableAssigned(assignment, iteration, value);
            }
    }

    /**
     * A value was unassigned from this variable
     * @param assignment current assignment
     * @param iteration current iteration
     * @param oldValue unassigned value
     */
    public void variableUnassigned(Assignment<V, T> assignment, long iteration, T oldValue) {
        if (iVariableListeners != null)
            for (VariableListener<T> listener : iVariableListeners)
                listener.variableUnassigned(assignment, iteration, oldValue);
    }

    /**
     * Adds a constraint. Called automatically when the constraint is added to
     * the model, i.e., {@link Model#addConstraint(Constraint)} is called.
     * 
     * @param constraint
     *            added constraint
     */
    public void addContstraint(Constraint<V, T> constraint) {
        iConstraints.add(constraint);
        if (constraint.isHard()) {
            iHardConstraints.add(constraint);
            iConstraintVariables = null;
        } else
            iSoftConstraints.add(constraint);
    }

    /**
     * Removes a constraint. Called automatically when the constraint is removed
     * from the model, i.e., {@link Model#removeConstraint(Constraint)} is
     * called.
     * 
     * @param constraint
     *            added constraint
     */
    public void removeContstraint(Constraint<V, T> constraint) {
        iConstraints.remove(constraint);
        if (iHardConstraints.contains(constraint)) {
            iHardConstraints.remove(constraint);
            iConstraintVariables = null;
        } else
            iSoftConstraints.remove(constraint);
    }

    /** Return the list of constraints associated with this variable 
     * @return list of constraints associated with this variable
     **/
    public List<Constraint<V, T>> constraints() {
        return iConstraints;
    }

    /** Return the list of hard constraints associated with this variable 
     * @return list of hard constraints associated with this variable
     **/
    public List<Constraint<V, T>> hardConstraints() {
        return iHardConstraints;
    }

    /** Return the list of soft constraints associated with this variable 
     * @return list of soft (not hard) constraints associated with this variable
     **/
    public List<Constraint<V, T>> softConstraints() {
        return iSoftConstraints;
    }

    @Override
    public String toString() {
        return getName();
    }

    /** Unique id 
     * @return variable id
     **/
    public long getId() {
        return iId;
    }

    @Override
    public int hashCode() {
        return (int) iId;
    }

    /** Variable's name -- for printing purposes 
     * @return variable name
     **/
    public String getName() {
        return String.valueOf(iId);
    }

    /** Variable's description -- for printing purposes 
     * @return variable description
     **/
    public String getDescription() {
        return null;
    }

    /**
     * Sets variable's value of the best ever found solution. Called when
     * {@link Model#saveBest(Assignment)} is called.
     * @param value a value
     * @param iteration value's assignment iteration
     */
    public void setBestAssignment(T value, long iteration) {
        iBestValue = value;
        iBestAssignmentIteration = iteration;
    }

    /** Returns the value from the best ever found solution. 
     * @return best assignment 
     **/
    public T getBestAssignment() {
        return iBestValue;
    }

    /** Returns the iteration when the best value was assigned
     * @return iteration of the best assignment
     **/
    public long getBestAssignmentIteration() {
        return iBestAssignmentIteration;
    }

    @Override
    public int compareTo(V variable) {
        if (variable == null)
            return -1;
        int cmp = getName().compareTo(variable.getName());
        if (cmp != 0)
            return cmp;
        return Double.compare(getId(), variable.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Variable<?, ?>))
            return false;
        return getId() == ((Variable<?, ?>) o).getId();
    }

    /** Adds variable listener 
     * @param listener a variable listener
     **/
    public void addVariableListener(VariableListener<T> listener) {
        if (iVariableListeners == null)
            iVariableListeners = new ArrayList<VariableListener<T>>();
        iVariableListeners.add(listener);
    }

    /** Removes variable listener 
     * @param listener a variable listener
     **/
    public void removeVariableListener(VariableListener<T> listener) {
        if (iVariableListeners != null)
            iVariableListeners.remove(listener);
    }

    /** Return variable listeners
     * @return list of variable listeners 
     **/
    public List<VariableListener<T>> getVariableListeners() {
        return iVariableListeners;
    }

    /**
     * Extra information to which can be used by an extension (see
     * {@link org.cpsolver.ifs.extension.Extension}).
     * @param object extra object
     */
    public <X> void setExtra(X object) {
        iExtra = object;
    }

    /**
     * Extra information to which can be used by an extension (see
     * {@link org.cpsolver.ifs.extension.Extension}).
     * @return extra object
     */
    @SuppressWarnings("unchecked")
    public <X> X getExtra() {
        try {
            return (X) iExtra;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Permanently remove a value from variable's domain.
     * The variable should not have this value assigned in any existing assignment.
     * @param iteration current iteration
     * @param value value to be removed from this variable's domain
     **/
    @SuppressWarnings({ "deprecation", "unchecked" })
    public void removeValue(long iteration, T value) {
    	if (value.equals(getModel().getDefaultAssignment().getValue((V) this)))
    		getModel().getDefaultAssignment().unassign(iteration, (V) this);
        if (iValues == null)
            return;
        iValues.remove(value);
        if (iInitialValue != null && iInitialValue.equals(value)) {
            iInitialValue = null;
            if (iModel != null)
                iModel.invalidateVariablesWithInitialValueCache();
        }
        if (iVariableListeners != null)
            for (VariableListener<T> listener : iVariableListeners)
                listener.valueRemoved(iteration, value);
        iRecentlyRemovedValue = value;
    }

    /**
     * Returns a table of all variables linked with this variable by a
     * constraint.
     * 
     * @return table (variable, constraint)
     */
    public Map<V, List<Constraint<V, T>>> constraintVariables() {
        if (iConstraintVariables == null) {
            iConstraintVariables = new HashMap<V, List<Constraint<V, T>>>();
            for (Constraint<V, T> constraint : constraints()) {
                for (V variable : constraint.variables()) {
                    if (!variable.equals(this)) {
                        List<Constraint<V, T>> constraints = iConstraintVariables.get(variable);
                        if (constraints == null) {
                            constraints = new ArrayList<Constraint<V, T>>();
                            iConstraintVariables.put(variable, constraints);
                        }
                        constraints.add(constraint);
                    }
                }
            }
        }
        return iConstraintVariables;
    }

    /**
     * Permanently remove the initial value from the variable's domain -- for
     * testing MPP
     */
    public void removeInitialValue() {
        if (iInitialValue == null)
            return;
        if (iValues == null)
            return;
        iValues.remove(iInitialValue);
        if (iModel != null)
            iModel.invalidateVariablesWithInitialValueCache();
        iInitialValue = null;
    }
 
    /**
     * Unique index of a variable, only to be assigned by {@link Model#addVariable(Variable)}.
     * @param index an index
     */
    public void setIndex(int index) { iIndex = index; }

    /**
     * Unique index of a variable that was assigned by {@link Model#addVariable(Variable)}.
     * @return -1 if not in a model
     */
    public int getIndex() { return iIndex; }
}