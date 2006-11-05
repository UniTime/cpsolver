package net.sf.cpsolver.ifs.model;

import java.util.*;

import net.sf.cpsolver.ifs.solver.*;
import net.sf.cpsolver.ifs.util.*;

/**
 * Generic model (definition of a problem).
 * <br><br>
 * It consists of variables and constraints. It has also capability of memorizing the current
 * and the best ever found assignment.
 * <br><br>
 * Example usage:<br><ul><code>
 * MyModel model = new MyModel();<br>
 * Variable a = new MyVariable("A");<br>
 * model.addVariable(a);<br>
 * Variable b = new MyVariable("B");<br>
 * model.addVariable(b);<br>
 * Variable c = new MyVariable("C");<br>
 * model.addVariable(c);<br>
 * Constraint constr = MyConstraint("all-different");<br>
 * model.addConstraint(constr);<br>
 * constr.addVariable(a);<br>
 * constr.addVariable(b);<br>
 * constr.addVariable(c);<br>
 * solver.setInitialSolution(model);
 * </code></ul>
 * 
 * @see Variable
 * @see Constraint
 * @see net.sf.cpsolver.ifs.solution.Solution
 * @see net.sf.cpsolver.ifs.solver.Solver
 *
 * @version
 * IFS 1.0 (Iterative Forward Search)<br>
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

public class Model {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(Model.class);
    private static java.text.DecimalFormat sTimeFormat = new java.text.DecimalFormat("0.00",new java.text.DecimalFormatSymbols(Locale.US));
    
    private Vector iVariables = new FastVector();
    private Vector iConstraints = new FastVector();
    private Vector iUnassignedVariables = new FastVector();
    private Vector iAssignedVariables = new FastVector();
    
    private Vector iPerturbVariables = null;
    private Vector iConflictVariables = null;
    private Vector iVariablesWithoutInitialValue = null;
    
    private int iBestUnassignedVariables = -1;
    private int iBestPerturbations = 0;
    
    /** Constructor */
    public Model() {
    }
    
    /** The list of variables in the model */
    public Vector variables() { return iVariables; }
    /** The number of variables in the model */
    public int countVariables() { return iVariables.size(); }
    /** Adds a variable to the model */
    public void addVariable(Variable variable) {
        variable.setModel(this);
        iVariables.addElement(variable);
        if (variable.getAssignment()==null) iUnassignedVariables.addElement(variable);
        else iAssignedVariables.addElement(variable);
        if (variable.getAssignment()!=null) variable.assign(0L,variable.getAssignment());
        for (Enumeration e=iModelListeners.elements();e.hasMoreElements();)
            ((ModelListener)e.nextElement()).variableAdded(variable);
    }
    /** Removes a variable from the model */
    public void removeVariable(Variable variable) {
        variable.setModel(null);
        iVariables.removeElement(variable);
        if (iUnassignedVariables.contains(variable)) iUnassignedVariables.removeElement(variable);
        if (iAssignedVariables.contains(variable)) iAssignedVariables.removeElement(variable);
        for (Enumeration e=iModelListeners.elements();e.hasMoreElements();)
            ((ModelListener)e.nextElement()).variableRemoved(variable);
    }
    
    /** The list of constraints in the model */
    public Vector constraints() { return iConstraints; }
    /** The number of constraints in the model */
    public int countConstraints() { return iConstraints.size(); }
    /** Adds a constraint to the model */
    public void addConstraint(Constraint constraint) {
        constraint.setModel(this);
        iConstraints.addElement(constraint);
        for (Enumeration e=iModelListeners.elements();e.hasMoreElements();)
            ((ModelListener)e.nextElement()).constraintAdded(constraint);
    }
    /** Removes a constraint from the model */
    public void removeConstraint(Constraint constraint) {
        constraint.setModel(null);
        iConstraints.removeElement(constraint);
        for (Enumeration e=iModelListeners.elements();e.hasMoreElements();)
            ((ModelListener)e.nextElement()).constraintRemoved(constraint);
    }
    /** The list of unassigned variables in the model */
    public Vector unassignedVariables() { return iUnassignedVariables; }
    /** The list of assigned variables in the model */
    public Vector assignedVariables() { return iAssignedVariables; }
    /** The list of perturbation variables in the model, i.e., the variables which has an initial value but which are not 
     * assigned with this value.
     */
    public Vector perturbVariables() {
        if (iPerturbVariables!=null) return iPerturbVariables;
        Vector perturbances = new FastVector();
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            Variable variable = (Variable)e.nextElement();
            if (variable.getInitialAssignment()!=null) {
                if (variable.getAssignment()!=null) {
                    if (!variable.getInitialAssignment().equals(variable.getAssignment())) perturbances.addElement(variable);
                } else {
                    boolean hasPerturbance = false;
                    for (Enumeration x=variable.hardConstraints().elements();!hasPerturbance && x.hasMoreElements();) {
                        Constraint constraint = (Constraint)x.nextElement();
                        if (constraint.variables().contains(variable) && constraint.inConflict(variable.getInitialAssignment()))
                            hasPerturbance=true;
                    }
                    if (hasPerturbance) perturbances.addElement(variable);
                }
            }
        }
        iPerturbVariables = perturbances;
        return perturbances;
    }
    
    /** Returns the set of confliction variables with this value, if it is assigned to its variable */
    public Set conflictValues(Value value) {
        HashSet conflictValues = new HashSet();
        for (Enumeration c=value.variable().hardConstraints().elements(); c.hasMoreElements();)
            ((Constraint)c.nextElement()).computeConflicts(value, conflictValues);
        return conflictValues;
    }
    
    /** The list of variales without initial value */
    public Vector variablesWithoutInitialValue() {
        if (iVariablesWithoutInitialValue!=null) return iVariablesWithoutInitialValue;
        Vector variables = new FastVector();
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            Variable variable = (Variable)e.nextElement();
            if (variable.getInitialAssignment()==null) variables.addElement(variable);
        }
        iVariablesWithoutInitialValue=variables;
        return variables;
    }
    
    /** Called before a value is assigned to its variable */
    public void beforeAssigned(long iteration, Value value) {
        for (Enumeration e=iModelListeners.elements();e.hasMoreElements();)
            ((ModelListener)e.nextElement()).beforeAssigned(iteration, value);
    }
    
    /** Called before a value is unassigned from its variable */
    public void beforeUnassigned(long iteration, Value value) {
        for (Enumeration e=iModelListeners.elements();e.hasMoreElements();)
            ((ModelListener)e.nextElement()).beforeUnassigned(iteration, value);
    }
    
    /** Called after a value is assigned to its variable */
    public void afterAssigned(long iteration, Value value) {
        iUnassignedVariables.removeElement(value.variable());
        iAssignedVariables.addElement(value.variable());
        iPerturbVariables = null;
        iConflictVariables = null;
        for (Enumeration e=iModelListeners.elements();e.hasMoreElements();)
            ((ModelListener)e.nextElement()).afterAssigned(iteration, value);
    }
    
    /** Called after a value is unassigned from its variable */
    public void afterUnassigned(long iteration, Value value) {
        iUnassignedVariables.addElement(value.variable());
        iAssignedVariables.removeElement(value.variable());
        iPerturbVariables = null;
        iConflictVariables = null;
        for (Enumeration e=iModelListeners.elements();e.hasMoreElements();)
            ((ModelListener)e.nextElement()).afterUnassigned(iteration, value);
    }
    
    public String toString() {
        Collections.sort(variables(), new Comparator() {
            public int compare(Object o1, Object o2) {
                Variable v1 = (Variable)o1;
                Variable v2 = (Variable)o2;
                return (v1!=null?v1.getName().compareTo(v2.getName()):(int)(v1.getId()-v2.getId()));
            }
        });
        return "Model{\n    variables="+ToolBox.col2string(variables(),2)+
        ",\n    constraints="+ToolBox.col2string(constraints(),2)+
        ",\n    #unassigned="+unassignedVariables().size()+
        ",\n    unassigned="+ToolBox.col2string(unassignedVariables(),2)+
        ",\n    #perturbations="+perturbVariables().size()+"+"+variablesWithoutInitialValue().size()+
        ",\n    perturbations="+ToolBox.col2string(perturbVariables(),2)+
        ",\n    info="+getInfo()+
        (variablesWithoutInitialValue().size()<variables().size()?",\n    withoutInitial="+ToolBox.col2string(variablesWithoutInitialValue(),2):"")+
        "\n  }";
    }
    
    /** Returns information about the current solution. Information from all model listeners and constraints is also included.
     */
    public java.util.Hashtable getInfo() {
        java.util.Hashtable ret = new java.util.Hashtable();
        ret.put("Unassigned variables", unassignedVariables().size()+" / "+variables().size());
        ret.put("Perturbation variables (with + without initial value)", perturbVariables().size()+" + "+variablesWithoutInitialValue().size());
        ret.put("Total value", new Integer(getTotalValue()));
        for (Enumeration e=iModelListeners.elements();e.hasMoreElements();)
            ((ModelListener)e.nextElement()).getInfo(ret);
        for (Enumeration e=iConstraints.elements();e.hasMoreElements();)
            ((Constraint)e.nextElement()).getInfo(ret);
        return ret;
    }
    
    /** Returns the number of unassigned variables in the best ever found solution */
    public int getBestUnassignedVariables() { return iBestUnassignedVariables; }
    /** Returns the number of perturbation variables in the best ever found solution */
    public int getBestPerturbations() { return iBestPerturbations; }
    /** Save the current assignment as the best ever found assignment */
    public void saveBest() {
        iBestUnassignedVariables = unassignedVariables().size();
        iBestPerturbations = perturbVariables().size();
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            Variable variable = (Variable)e.nextElement();
            variable.setBestAssignment(variable.getAssignment());
        }
    }
    /** Restore the best ever found assignment into the current assignment*/
    public void restoreBest() {
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            Variable variable = (Variable)e.nextElement();
            variable.unassign(0);
        }
        HashSet problems = new HashSet();
        for (Enumeration e=ToolBox.sortEnumeration(variables().elements(), new BestAssignmentComparator());e.hasMoreElements();) {
            Variable variable = (Variable)e.nextElement();
            if (variable.getBestAssignment()!=null) {
                Set confs = conflictValues(variable.getBestAssignment());
                if (!confs.isEmpty()) {
                    sLogger.error("restore best problem: assignment "+variable.getName()+" = "+variable.getBestAssignment().getName());
                    for (Enumeration en=variable.hardConstraints().elements();en.hasMoreElements();) {
                        Constraint c=(Constraint)en.nextElement();
                        Set x = new HashSet();
                        c.computeConflicts(variable.getBestAssignment(),x);
                        if (!x.isEmpty()) {
                            sLogger.error("  constraint "+c.getName()+" causes the following conflicts "+x);
                        }
                    }
                    problems.add(variable.getBestAssignment());
                } else variable.assign(0,variable.getBestAssignment());
            }
        }
        int attempt = 0;
        while (!problems.isEmpty() && attempt<=100) {
            attempt++;
            Value value = (Value)ToolBox.random(problems); problems.remove(value);
            Variable variable = value.variable();            
            Set confs = conflictValues(value);
            if (!confs.isEmpty()) {
                sLogger.error("restore best problem (again, att="+attempt+"): assignment "+variable.getName()+" = "+variable.getBestAssignment().getName());
                for (Enumeration en=variable.hardConstraints().elements();en.hasMoreElements();) {
                    Constraint c=(Constraint)en.nextElement();
                    Set x = new HashSet();
                    c.computeConflicts(value,x);
                    if (!x.isEmpty()) sLogger.error("  constraint "+c.getName()+" causes the following conflicts "+x);
                }
                problems.addAll(confs);
            }
            variable.assign(0,value);
        }
    }
    
    /** The list of unassigned variables in the best ever found solution*/
    public Vector bestUnassignedVariables() {
        if (iBestUnassignedVariables<0) return unassignedVariables();
        Vector ret = new FastVector(variables().size());
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            Variable variable = (Variable)e.nextElement();
            if (variable.getBestAssignment()==null) ret.addElement(variable);
        }
        return ret;
    }
    
    /** Value of the current solution. It is the sum of all assigned values, i.e., {@link Value#toInt()}.*/
    public int getTotalValue() {
        int valCurrent = 0;
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();)
            valCurrent += ((Variable)e.nextElement()).getAssignment().toInt();
        return valCurrent;
    }
    
    private Vector iModelListeners = new FastVector();
    /** Adds a model listener */
    public void addModelListener(ModelListener listener) {
        iModelListeners.addElement(listener);
        for (Enumeration e=iConstraints.elements();e.hasMoreElements();)
            listener.constraintAdded((Constraint)e.nextElement());
        for (Enumeration e=iVariables.elements();e.hasMoreElements();)
            listener.variableAdded((Variable)e.nextElement());
    }
    /** Removes a model listener */
    public void removeModelListener(ModelListener listener) {
        for (Enumeration e=iVariables.elements();e.hasMoreElements();)
            listener.variableRemoved((Variable)e.nextElement());
        for (Enumeration e=iConstraints.elements();e.hasMoreElements();)
            listener.constraintRemoved((Constraint)e.nextElement());
        iModelListeners.removeElement(listener);
    }
    
    /** Model initialization */
    public boolean init(Solver solver) {
        boolean ok = true;
        for (Enumeration e=iModelListeners.elements();ok && e.hasMoreElements();)
            ok = ((ModelListener)e.nextElement()).init(solver);
        return ok;
    }
    /** The list of model listeners */
    public Vector getModelListeners() { return iModelListeners; }
    /** The list of model listeners that are of the given class*/
    public ModelListener modelListenerOfType(Class type) {
        for (Enumeration e=iModelListeners.elements();e.hasMoreElements();) {
            ModelListener listener = (ModelListener)e.nextElement();
            if (listener.getClass() == type) return listener;
        }
        return null;
    }
    /** The list of constraints which are in a conflict with the given value if it is assigned to its variable.
     * This means the constraints, which adds a value into the set of conflicting values in {@link Constraint#computeConflicts(Value, Set)}.
     */
    public Hashtable conflictConstraints(Value value) {
        Hashtable conflictConstraints = new Hashtable();
        for (Enumeration c=value.variable().hardConstraints().elements(); c.hasMoreElements();) {
            Constraint constraint = (Constraint)c.nextElement();
            HashSet conflicts = new HashSet();
            constraint.computeConflicts(value, conflicts);
            if (conflicts!=null && !conflicts.isEmpty()) {
                conflictConstraints.put(constraint,conflicts);
            }
        }
        return conflictConstraints;
    }
    /** The list of hard constraints which contain at least one variable that is not assigned. */
    public Vector unassignedHardConstraints() {
        Vector ret = new FastVector();
        for (Enumeration c=constraints().elements();c.hasMoreElements();) {
            Constraint constraint = (Constraint)c.nextElement();
            if (!constraint.isHard()) continue;
            boolean assigned = true;
            for (Enumeration v=constraint.variables().elements();assigned && v.hasMoreElements();)
                if (((Variable)v.nextElement()).getAssignment()==null) assigned=false;
            if (!assigned)
                ret.addElement(constraint);
        }
        return ret;
    }
    
    private class BestAssignmentComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            Variable v1=(Variable)o1;
            Variable v2=(Variable)o2;
            return (int)(v1.getBestAssignmentIteration()-v2.getBestAssignmentIteration());
        }
    }
}
