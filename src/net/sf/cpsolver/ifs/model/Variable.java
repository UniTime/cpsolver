package net.sf.cpsolver.ifs.model;

import java.util.*;

import net.sf.cpsolver.ifs.util.*;

/**
 * Generic variable.
 * <br><br>
 * Besides a domain of values, a variable also contains information about assigned value,
 * the value assigned in the best ever found solution and also the initial value (minimal
 * perturbations problem). It also knows what constraints are associated with this variable and 
 * has a unique id.
 *
 * @see Value
 * @see Model
 * @see net.sf.cpsolver.ifs.solver.Solver
 *
 * @version
 * IFS 1.1 (Iterative Forward Search)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@ktiml.mff.cuni.cz">muller@ktiml.mff.cuni.cz</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
public class Variable implements Comparable {
    private static IdGenerator iIdGenerator = new IdGenerator();

    protected long iId = -1;
    private Model iModel = null;

    private Value iInitialValue = null; // initial value
    /** Assigned value */
    protected Value iValue = null; // assigned value
    private Value iBestValue = null; // best value
    private long iBestAssignmentIteration = 0;
    private Vector iValues = null;
    
    private Value iRecentlyRemovedValue = null;

    private long iAssignmentCounter = 0;
    private long iLastAssignmentIteration = -1;
    private long iLastUnassignmentIteration = -1;
    private Object iExtra = null;
    
    private Vector iConstraints = new FastVector();
    private Vector iHardConstraints = new FastVector();
    private Vector iSoftConstraints = new FastVector();
    private Vector iVariableListeners = null;
    
    private Hashtable iConstraintVariables = null;
    
    /** Constructor */
    public Variable() {
        this(null);
    }
    
    /** Constructor
     * @param initialValue initial value (minimal-perturbation problem)
     */
    public Variable(Value initialValue) {
        iId = iIdGenerator.newId();
        setInitialAssignment(initialValue);
    }
    
    /** Model, the variable belong to */
    public Model getModel() { return iModel; }
    /** Set the model to which the variable belongs to */
    public void setModel(Model model) { iModel = model; }
        
    /** Domain */
    public Vector values() {
        return iValues;
    }
    /** Sets the domain */
    protected void setValues(Vector values) {
        iValues = values;
    }
    /** True, if the variable's domain is not empty */
    public boolean hasValues() {
    	return !values().isEmpty();
    }
    
    /** Returns current assignment */
    public Value getAssignment() { return iValue; }
    /** Returns true if the variable is assigned */
    public boolean hasAssignment() { return iValue!=null; }
    /** Returns initial assignment */
    public Value getInitialAssignment() { return iInitialValue; }
    /** Sets initial assignment */
    public void setInitialAssignment(Value initialValue) { 
        iInitialValue = initialValue; 
        if (iInitialValue!=null && iInitialValue.variable()==null) iInitialValue.setVariable(this);
        if (iModel!=null)
        	iModel.invalidateVariablesWithInitialValueCache();
    }
    /** Returns true if the variable has an initial assignment */
    public boolean hasInitialAssignment() { return iInitialValue!=null; }
    
    /** Assign value to this variable. If the variable has already assigned another value, it is 
     * unassigned first. Also, all conflicting values are unassigned before the given value is assigned
     * to this variable.
     * @param iteration current iteration
     * @param value the value to be assigned
     */
    public void assign(long iteration, Value value) {
    	if (getModel()!=null)
    		getModel().beforeAssigned(iteration,value);
        iLastAssignmentIteration = iteration;
        if (iValue!=null) unassign(iteration);
        if (iRecentlyRemovedValue!=null && iRecentlyRemovedValue.equals(value)) {
            iRecentlyRemovedValue = null;
            return;
        }
        if (value==null) return;
        iValue = value;
        for (Enumeration e=iConstraints.elements(); e.hasMoreElements();) {
            Constraint constraint = (Constraint)e.nextElement();
            constraint.assigned(iteration, value);
        }
        iAssignmentCounter++;
        value.assigned(iteration);
        if (iVariableListeners!=null) 
            for (Enumeration e=iVariableListeners.elements(); e.hasMoreElements();)
                ((VariableListener)e.nextElement()).variableAssigned(iteration, value);
    	if (getModel()!=null)
    		getModel().afterAssigned(iteration,value);
    }
    
    /** Unassign value from this variable.
     * @param iteration current iteration
     */
    public void unassign(long iteration) {
        if (iValue==null) return;
    	if (getModel()!=null)
    		getModel().beforeUnassigned(iteration,iValue);
        iLastUnassignmentIteration = iteration;
        Value oldValue = iValue;
        iValue = null;
        for (Enumeration e=iConstraints.elements(); e.hasMoreElements();) {
            Constraint constraint = (Constraint)e.nextElement();
            constraint.unassigned(iteration, oldValue);
        }
        oldValue.unassigned(iteration);
        if (iVariableListeners!=null)
            for (Enumeration e=iVariableListeners.elements(); e.hasMoreElements();) 
                ((VariableListener)e.nextElement()).variableUnassigned(iteration, oldValue);
    	if (getModel()!=null)
    		getModel().afterUnassigned(iteration,oldValue);
    }
    /** Return how many times was this variable assigned in the past. */
    public long countAssignments() { return iAssignmentCounter; }
    
    /** Adds a constraint. Called automatically when the constraint is added to the model, i.e.,
     * {@link Model#addConstraint(Constraint)} is called.
     * @param constraint added constraint
     */
    public void addContstraint(Constraint constraint) {  
        iConstraints.addElement(constraint); 
        if (constraint.isHard()) {
            iHardConstraints.addElement(constraint);
            iConstraintVariables = null;
        } else iSoftConstraints.addElement(constraint);
    }

    /** Removes a constraint. Called automatically when the constraint is removed from the model, i.e.,
     * {@link Model#removeConstraint(Constraint)} is called.
     * @param constraint added constraint
     */
    public void removeContstraint(Constraint constraint) { 
        iConstraints.removeElement(constraint); 
        if (iHardConstraints.contains(constraint)) {
            iHardConstraints.removeElement(constraint);
            iConstraintVariables = null; 
        } else iSoftConstraints.removeElement(constraint);
    }
    
    /** Return the list of constraints associated with this variable */
    public Vector constraints() { return iConstraints; }
    /** Return the list of hard constraints associated with this variable */
    public Vector hardConstraints() { return iHardConstraints; }
    /** Return the list of soft constraints associated with this variable */
    public Vector softConstraints() { return iSoftConstraints; }
    
    public String toString() {
        return "Variable{name="+getName()+", initial="+getInitialAssignment()+", current="+getAssignment()+", values="+values().size()+", constraints="+iConstraints.size()+"}";
    }
    
    /** Unique id */
    public long getId() { return iId;}
    
    public int hashCode() { return (int)iId; }
    /** Variable's name -- for printing purposes */
    public String getName() { return String.valueOf(iId); }
    /** Variable's description -- for printing purposes */
    public String getDescription() { return null; }

    /** Sets variable's value of the best ever found solution. Called when {@link Model#saveBest()} is called. */
    public void setBestAssignment(Value value) { iBestValue = value; iBestAssignmentIteration = (value==null?0l:value.lastAssignmentIteration()); }
    /** Returns the value from the best ever found soultion. */
    public Value getBestAssignment() { return iBestValue; }
    /** Returns the iteration when the best value was assigned */
    public long getBestAssignmentIteration() { return iBestAssignmentIteration; }

    /** Returns the iteration when the variable was assigned for the last time (-1 if never) */
    public long lastAssignmentIteration() { return iLastAssignmentIteration; }
    /** Returns the iteration when the variable was unassigned for the last time (-1 if never) */    
    public long lastUnassignmentIteration() { return iLastUnassignmentIteration; }

    public int compareTo(Object o) {
        if (o==null || !(o instanceof Variable)) return -1;
        Variable v = (Variable)o;
        return getName().compareTo(v.getName());
    }
    
    public boolean equals(Object o) {
        try {
            Variable v = (Variable)o;
            return getId()==v.getId();
        } catch (Exception e) {
            return false;
        }
    }

    /** Adds variable listener */
    public void addVariableListener(VariableListener listener) { 
        if (iVariableListeners==null) iVariableListeners=new FastVector();
        iVariableListeners.addElement(listener); 
    }
    /** Removes variable listener */
    public void removeVariableListener(VariableListener listener) { 
        if (iVariableListeners==null) iVariableListeners=new FastVector();
        iVariableListeners.removeElement(listener); 
    }
    /** Return variable listeners */
    public Vector getVariableListeners() { return iVariableListeners; }
    
    /** Extra information to which can be used by an extension (see {@link net.sf.cpsolver.ifs.extension.Extension}). */
    public void setExtra(Object object) { iExtra = object; }
    /** Extra information to which can be used by an extension (see {@link net.sf.cpsolver.ifs.extension.Extension}). */
    public Object getExtra() { return iExtra; }

    /** Permanently remove a value from variables domain. */
    public void removeValue(long iteration, Value value) {
        if (iValue!=null && iValue.equals(value)) unassign(iteration);
        if (iValues==null) return;
        iValues.remove(value);
        if (iInitialValue!=null && iInitialValue.equals(value)) {
        	iInitialValue=null;
        	if (iModel!=null) iModel.invalidateVariablesWithInitialValueCache();
        }
        if (iVariableListeners!=null)
            for (Enumeration e=iVariableListeners.elements(); e.hasMoreElements();)
                ((VariableListener)e.nextElement()).valueRemoved(iteration, value);
        iRecentlyRemovedValue = value;
    }
    
    /** Returns a table of all variables linked with this variable by a constraint. 
     * @return table (variable, constraint)
     */
    public Hashtable constraintVariables() {
        if (iConstraintVariables == null) {
            iConstraintVariables = new Hashtable();
            for (Enumeration e1=constraints().elements();e1.hasMoreElements();) {
                Constraint constraint = (Constraint)e1.nextElement();
                for (Enumeration e2=constraint.variables().elements();e2.hasMoreElements();) {
                    Variable variable = (Variable)e2.nextElement();
                    if (!variable.equals(this)) {
                        Vector constraints = (Vector)iConstraintVariables.get(variable);
                        if (constraints==null) {
                            constraints = new FastVector(1,10);
                            iConstraintVariables.put(variable, constraints);
                        }
                        constraints.add(constraint);
                    }
                }
            }
        }
        return iConstraintVariables;
    }

    /** Permanently remove the initial value from the variable's domain -- for testing MPP */
    public void removeInitialValue() {
        if (iInitialValue==null) return;
        if (iValues==null) return;
        if (getAssignment()!=null && getAssignment().equals(iInitialValue)) unassign(0);
        iValues.remove(iInitialValue);
        if (iModel!=null)
        	iModel.invalidateVariablesWithInitialValueCache();
        iInitialValue=null;
    }
}