package net.sf.cpsolver.ifs.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.cpsolver.ifs.util.IdGenerator;

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
 * @see net.sf.cpsolver.ifs.solver.Solver
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
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
 */
public class Variable<V extends Variable<V, T>, T extends Value<V, T>> implements Comparable<V> {
    private static IdGenerator iIdGenerator = new IdGenerator();

    protected long iId = -1;
    private Model<V, T> iModel = null;

    private T iInitialValue = null; // initial value
    /** Assigned value */
    protected T iValue = null; // assigned value
    private T iBestValue = null; // best value
    private long iBestAssignmentIteration = 0;
    private List<T> iValues = null;

    private T iRecentlyRemovedValue = null;

    private long iAssignmentCounter = 0;
    private long iLastAssignmentIteration = -1;
    private long iLastUnassignmentIteration = -1;
    private Object iExtra = null;

    private List<Constraint<V, T>> iConstraints = new ArrayList<Constraint<V, T>>();
    private List<Constraint<V, T>> iHardConstraints = new ArrayList<Constraint<V, T>>();
    private List<Constraint<V, T>> iSoftConstraints = new ArrayList<Constraint<V, T>>();
    private List<VariableListener<T>> iVariableListeners = null;

    private Map<V, List<Constraint<V, T>>> iConstraintVariables = null;

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

    /** Model, the variable belong to */
    public Model<V, T> getModel() {
        return iModel;
    }

    /** Set the model to which the variable belongs to */
    public void setModel(Model<V, T> model) {
        iModel = model;
    }

    /** Domain */
    public List<T> values() {
        return iValues;
    }

    /** Sets the domain */
    protected void setValues(List<T> values) {
        iValues = values;
    }

    /** True, if the variable's domain is not empty */
    public boolean hasValues() {
        return !values().isEmpty();
    }

    /** Returns current assignment */
    public T getAssignment() {
        return iValue;
    }

    /** Returns true if the variable is assigned */
    public boolean hasAssignment() {
        return iValue != null;
    }

    /** Returns initial assignment */
    public T getInitialAssignment() {
        return iInitialValue;
    }

    /** Sets initial assignment */
    public void setInitialAssignment(T initialValue) {
        iInitialValue = initialValue;
        if (iInitialValue != null && iInitialValue.variable() == null)
            iInitialValue.setVariable(this);
        if (iModel != null)
            iModel.invalidateVariablesWithInitialValueCache();
    }

    /** Returns true if the variable has an initial assignment */
    public boolean hasInitialAssignment() {
        return iInitialValue != null;
    }

    /**
     * Assign value to this variable. If the variable has already assigned
     * another value, it is unassigned first. Also, all conflicting values are
     * unassigned before the given value is assigned to this variable.
     * 
     * @param iteration
     *            current iteration
     * @param value
     *            the value to be assigned
     */
    public void assign(long iteration, T value) {
        if (getModel() != null)
            getModel().beforeAssigned(iteration, value);
        iLastAssignmentIteration = iteration;
        if (iValue != null)
            unassign(iteration);
        if (iRecentlyRemovedValue != null && iRecentlyRemovedValue.equals(value)) {
            iRecentlyRemovedValue = null;
            return;
        }
        if (value == null)
            return;
        iValue = value;
        for (Constraint<?, T> constraint : iConstraints) {
            constraint.assigned(iteration, value);
        }
        if (getModel() != null)
            for (GlobalConstraint<?, T> constraint : getModel().globalConstraints()) {
                constraint.assigned(iteration, value);
            }
        iAssignmentCounter++;
        value.assigned(iteration);
        if (iVariableListeners != null)
            for (VariableListener<T> listener : iVariableListeners) {
                listener.variableAssigned(iteration, value);
            }
        if (getModel() != null)
            getModel().afterAssigned(iteration, value);
    }

    /**
     * Unassign value from this variable.
     * 
     * @param iteration
     *            current iteration
     */
    public void unassign(long iteration) {
        if (iValue == null)
            return;
        if (getModel() != null)
            getModel().beforeUnassigned(iteration, iValue);
        iLastUnassignmentIteration = iteration;
        T oldValue = iValue;
        iValue = null;
        for (Constraint<?, T> constraint : iConstraints) {
            constraint.unassigned(iteration, oldValue);
        }
        if (getModel() != null)
            for (GlobalConstraint<?, T> constraint : getModel().globalConstraints()) {
                constraint.unassigned(iteration, oldValue);
            }
        oldValue.unassigned(iteration);
        if (iVariableListeners != null)
            for (VariableListener<T> listener : iVariableListeners)
                listener.variableUnassigned(iteration, oldValue);
        if (getModel() != null)
            getModel().afterUnassigned(iteration, oldValue);
    }

    /** Return how many times was this variable assigned in the past. */
    public long countAssignments() {
        return iAssignmentCounter;
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

    /** Return the list of constraints associated with this variable */
    public List<Constraint<V, T>> constraints() {
        return iConstraints;
    }

    /** Return the list of hard constraints associated with this variable */
    public List<Constraint<V, T>> hardConstraints() {
        return iHardConstraints;
    }

    /** Return the list of soft constraints associated with this variable */
    public List<Constraint<V, T>> softConstraints() {
        return iSoftConstraints;
    }

    @Override
    public String toString() {
        return "Variable{name=" + getName() + ", initial=" + getInitialAssignment() + ", current=" + getAssignment()
                + ", values=" + values().size() + ", constraints=" + iConstraints.size() + "}";
    }

    /** Unique id */
    public long getId() {
        return iId;
    }

    @Override
    public int hashCode() {
        return (int) iId;
    }

    /** Variable's name -- for printing purposes */
    public String getName() {
        return String.valueOf(iId);
    }

    /** Variable's description -- for printing purposes */
    public String getDescription() {
        return null;
    }

    /**
     * Sets variable's value of the best ever found solution. Called when
     * {@link Model#saveBest()} is called.
     */
    public void setBestAssignment(T value) {
        iBestValue = value;
        iBestAssignmentIteration = (value == null ? 0l : value.lastAssignmentIteration());
    }

    /** Returns the value from the best ever found soultion. */
    public T getBestAssignment() {
        return iBestValue;
    }

    /** Returns the iteration when the best value was assigned */
    public long getBestAssignmentIteration() {
        return iBestAssignmentIteration;
    }

    /**
     * Returns the iteration when the variable was assigned for the last time
     * (-1 if never)
     */
    public long lastAssignmentIteration() {
        return iLastAssignmentIteration;
    }

    /**
     * Returns the iteration when the variable was unassigned for the last time
     * (-1 if never)
     */
    public long lastUnassignmentIteration() {
        return iLastUnassignmentIteration;
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

    /** Adds variable listener */
    public void addVariableListener(VariableListener<T> listener) {
        if (iVariableListeners == null)
            iVariableListeners = new ArrayList<VariableListener<T>>();
        iVariableListeners.add(listener);
    }

    /** Removes variable listener */
    public void removeVariableListener(VariableListener<T> listener) {
        if (iVariableListeners != null)
            iVariableListeners.remove(listener);
    }

    /** Return variable listeners */
    public List<VariableListener<T>> getVariableListeners() {
        return iVariableListeners;
    }

    /**
     * Extra information to which can be used by an extension (see
     * {@link net.sf.cpsolver.ifs.extension.Extension}).
     */
    public void setExtra(Object object) {
        iExtra = object;
    }

    /**
     * Extra information to which can be used by an extension (see
     * {@link net.sf.cpsolver.ifs.extension.Extension}).
     */
    public Object getExtra() {
        return iExtra;
    }

    /** Permanently remove a value from variables domain. */
    public void removeValue(long iteration, T value) {
        if (iValue != null && iValue.equals(value))
            unassign(iteration);
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
        if (getAssignment() != null && getAssignment().equals(iInitialValue))
            unassign(0);
        iValues.remove(iInitialValue);
        if (iModel != null)
            iModel.invalidateVariablesWithInitialValueCache();
        iInitialValue = null;
    }
}