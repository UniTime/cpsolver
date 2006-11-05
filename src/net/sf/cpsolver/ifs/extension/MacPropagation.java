package net.sf.cpsolver.ifs.extension;


import java.util.*;

import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.solver.*;
import net.sf.cpsolver.ifs.util.*;

/**
 * MAC propagation.
 * <br><br>
 * During the arc consistency maintenance, when a value is deleted from a variable’s domain, the reason (forming an 
 * explanation) can be computed and attached to the deleted value. Once a variable (say Vx with the assigned value vx) 
 * is unassigned during the search, all deleted values which contain a pair Vx = vx in their explanations need to be 
 * recomputed. Such value can be either still inconsistent with the current (partial) solution (a different explanation 
 * is attached to it in this case) or it can be returned back to its variable's domain. Arc consistency is maintained 
 * after each iteration step, i.e., the selected assignment is propagated into the not yet assigned variables. When a 
 * value vx is assigned to a variable Vx, an explanation Vx != vx' &#8592; Vx = vx is attached to all values vx' of the 
 * variable Vx, different from vx.
 * <br><br>
 * In the case of forward checking (only constraints going from assigned variables to unassigned variables are revised), 
 * computing explanations is rather easy. A value vx is deleted from the domain of the variable Vx only if there is a 
 * constraint which prohibits the assignment Vx=vx because of the existing assignments (e.g., Vy = vy, … Vz = vz). 
 * An explanation for the deletion of this value vx is then Vx != vx &#8592; (Vy = vy & ... Vz = vz), where Vy = vy & ... Vz = vz 
 * are assignments contained in the prohibiting constraint. In case of arc consistency, a value vx is deleted from 
 * the domain of the variable Vx if there is a constraint which does not permit the assignment Vx = vx with other 
 * possible assignments of the other variables in the constraint. This means that there is no support value (or 
 * combination of values) for the value vx of the variable Vx in the constraint. An explanation is then a union of 
 * explanations of all possible support values for the assignment Vx = vx of this constraint which were deleted. 
 * The reason is that if one of these support values is returned to its variable's domain, this value vx may 
 * be returned as well (i.e., the reason for its deletion has vanished, a new reason needs to be computed). 
 * <br><br>
 * As for the implementation, we only need to enforce arc consistency of the initial solution and to extend unassign 
 * and assign methods. Procedure {@link MacPropagation#afterAssigned(long, Value)} enforces arc consistency of the 
 * solution with the selected assignment variable = value and the procedure {@link MacPropagation#afterUnassigned(long, Value)}
 * "undoes" the assignment variable = value. It means that explanations of all values which were deleted and which 
 * contain assignment variable = value in their explanations need to be recomputed. This can be done via returning 
 * all these values into their variables’ domains followed by arc consistency maintenance over their variables.
 * <br><br>
 * Parameters:
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>MacPropagation.JustForwardCheck</td><td>{@link Boolean}</td><td>If true, only forward checking instead of full arc consistency is maintained during the search.</td></tr>
 * </table>
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
public class MacPropagation extends Extension {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(MacPropagation.class);
    private boolean iJustForwardCheck = false;
    
    /** List of constraints on which arc-consistency is to be maintained */
    protected Vector iConstraints = null;
    /** Current iteration */
    protected long iIteration = 0;
    
    /** Constructor */
    public MacPropagation(Solver solver, DataProperties properties) {
        super(solver, properties);
        iJustForwardCheck = properties.getPropertyBoolean("MacPropagation.JustForwardCheck", false);
    }
    
    /** Adds a constraint on which arc-consistency is to be maintained */
    public void addConstraint(Constraint constraint) {
        if (iConstraints == null) iConstraints = new FastVector();
        iConstraints.addElement(constraint);
    }
    
    /** Returns true, if arc-consistency is to be maintained on the given constraint */
    public boolean contains(Constraint constraint) {
        if (iConstraints == null) return true;
        return iConstraints.contains(constraint);
    }
    
    /** Before a value is unassigned: until the value is inconsistent with the current solution, 
     * an assignment from its explanation is picked and unassigned. 
     */
    public void beforeAssigned(long iteration, Value value) {
        iIteration = iteration;
        if (value == null) return;
        if (!isGood(value)) {
            while (!isGood(value) && !noGood(value).isEmpty()) {
                Value noGoodValue = (Value)noGood(value).iterator().next();
                noGoodValue.variable().unassign(iteration);
            }
        }
        if (!isGood(value)) {
            sLogger.warn("Going to assign a bad value "+value+" with empty no-good.");
        }
    }
    
    /** After a value is assigned: explanations of other values of the value's variable are reset (to contain only the assigned value), 
     * propagation over the assigned variable takes place.
     */
    public void afterAssigned(long iteration, Value value) {
        iIteration = iteration;
        if (!isGood(value)) {
            sLogger.warn(value.variable().getName() + " = " + value.getName() + " -- not good value assigned (noGood:" + noGood(value) + ")");
            setGood(value);
        }
        
        HashSet noGood = new HashSet(1);
        noGood.add(value);
        for (Enumeration i = value.variable().values().elements(); i.hasMoreElements(); ) {
            Value anotherValue = (Value)i.nextElement();
            if (anotherValue.equals(value)) continue;
            setNoGood(anotherValue, noGood);
        }
        propagate(value.variable());
    }
    
    /** After a value is unassigned: explanations of all values of unassigned variable are recomputed ({@link Value#conflicts()}), propagation
     * undo over the unassigned variable takes place.
     */
    public void afterUnassigned(long iteration, Value value) {
        iIteration = iteration;
        if (!isGood(value))
            sLogger.error(value.variable().getName() + " = " + value.getName() + " -- not good value unassigned (noGood:" + noGood(value) + ")");
        for (Enumeration i = value.variable().values().elements(); i.hasMoreElements(); ) {
            Value anotherValue = (Value)i.nextElement();
            if (!isGood(anotherValue)) {
                Set noGood = anotherValue.conflicts();
                if (noGood == null)
                    setGood(anotherValue);
                else
                    setNoGood(anotherValue, noGood);
            }
        }
        undoPropagate(value.variable());
    }
    
    /** Initialization. Enforce arc-consistency over the current (initial) solution. AC3 algorithm is used. */
    public boolean init(Solver solver) {
        boolean isOk = true;
        Progress.getInstance().save();
        Progress.getInstance().setPhase("Initializing propagation:", 3 * getModel().variables().size());
        for (Enumeration i1 = getModel().variables().elements(); i1.hasMoreElements(); ) {
            Variable aVariable = (Variable)i1.nextElement();
            supportValues(aVariable).clear();
            goodValues(aVariable).clear();
        }
        for (Enumeration i1 = getModel().variables().elements(); i1.hasMoreElements(); ) {
            Variable aVariable = (Variable)i1.nextElement();
            for (Enumeration i2 = aVariable.values().elements(); i2.hasMoreElements(); ) {
                Value aValue = (Value)i2.nextElement();
                Set noGood = aValue.conflicts();
                initNoGood(aValue, noGood);
                if (noGood == null) {
                    goodValues(aVariable).add(aValue);
                }
                else {
                }
            }
            Progress.getInstance().incProgress();
        }
        net.sf.cpsolver.ifs.util.Queue queue = new net.sf.cpsolver.ifs.util.Queue(getModel().variables().size() + 1);
        for (Enumeration i1 = getModel().variables().elements(); i1.hasMoreElements(); ) {
            Variable aVariable = (Variable)i1.nextElement();
            for (Enumeration i2 = aVariable.hardConstraints().elements(); i2.hasMoreElements(); ) {
                Constraint constraint = (Constraint)i2.nextElement();
                propagate(constraint, aVariable, queue);
            }
            Progress.getInstance().incProgress();
        }
        if (!iJustForwardCheck)
            propagate(queue);
        for (Enumeration i1 = getModel().variables().elements(); i1.hasMoreElements(); ) {
            Variable aVariable = (Variable)i1.nextElement();
            Vector values2delete = new FastVector();
            for (Enumeration i2 = aVariable.values().elements(); i2.hasMoreElements(); ) {
                Value aValue = (Value)i2.nextElement();
                if (!isGood(aValue) && noGood(aValue).isEmpty()) {
                    values2delete.addElement(aValue);
                }
            }
            Object[] vals = values2delete.toArray();
            for (int i = 0; i < vals.length; i++)
                aVariable.removeValue(0, (Value)vals[i]);
            if (aVariable.values().isEmpty()) {
                sLogger.error(aVariable.getName() + " has empty domain!");
                isOk = false;
            }
            Progress.getInstance().incProgress();
        }
        Progress.getInstance().restore();
        return isOk;
    }
    
    /** Propagation over the given variable. */
    protected void propagate(Variable variable) {
        net.sf.cpsolver.ifs.util.Queue queue = new net.sf.cpsolver.ifs.util.Queue(variable.getModel().variables().size() + 1);
        if (variable.getAssignment() != null) {
            for (Enumeration i = variable.hardConstraints().elements(); i.hasMoreElements(); ) {
                Constraint constraint = (Constraint)i.nextElement();
                if (contains(constraint))
                    propagate(constraint, variable.getAssignment(), queue);
            }
        }
        else {
            for (Enumeration i = variable.hardConstraints().elements(); i.hasMoreElements(); ) {
                Constraint constraint = (Constraint)i.nextElement();
                if (contains(constraint))
                    propagate(constraint, variable, queue);
            }
        }
        if (!iJustForwardCheck && !queue.isEmpty())
            propagate(queue);
    }
    
    /** Propagation over the queue of variables. */
    protected void propagate(net.sf.cpsolver.ifs.util.Queue queue) {
        while (!queue.isEmpty()) {
            Variable aVariable = (Variable)queue.get();
            for (Enumeration i = aVariable.hardConstraints().elements(); i.hasMoreElements(); ) {
                Constraint constraint = (Constraint)i.nextElement();
                if (contains(constraint))
                    propagate(constraint, aVariable, queue);
            }
        }
    }
    
    /** Propagation undo over the given variable. All values having given variable in thair explanations needs to be
     * recomputed. This is done in two phases: 
     * 1) values that contain this variable in explanation are returned back to domains (marked as good)
     * 2) propagation over variables which contains a value that was marked as good takes place
     */
    public void undoPropagate(Variable variable) {
        Hashtable undoVars = new Hashtable();
        while (!supportValues(variable).isEmpty()) {
            Value value = (Value)supportValues(variable).iterator().next();
            Set noGood = value.conflicts();
            if (noGood == null) {
                setGood(value);
                Vector values = (Vector)undoVars.get(value.variable());
                if (values == null) {
                    values = new FastVector();
                    undoVars.put(value.variable(), values);
                }
                values.addElement(value);
            }
            else {
                setNoGood(value, noGood);
                if (noGood.isEmpty())
                    ((Variable)value.variable()).removeValue(iIteration, value);
            }
        }
        
        net.sf.cpsolver.ifs.util.Queue queue = new net.sf.cpsolver.ifs.util.Queue(variable.getModel().variables().size() + 1);
        for (Enumeration e = undoVars.keys(); e.hasMoreElements();) {
            Variable aVariable = (Variable)e.nextElement();
            Vector values = (Vector)undoVars.get(aVariable);
            boolean add = false;
            for (Enumeration e1 = aVariable.constraintVariables().keys(); e1.hasMoreElements(); )
                if (propagate((Variable)e1.nextElement(), aVariable, values))
                    add = true;
            if (add)
                queue.put(aVariable);
        }
        for (Enumeration e1 = variable.constraintVariables().keys(); e1.hasMoreElements(); )
            if (propagate((Variable)e1.nextElement(), variable) && !queue.contains(variable))
                queue.put(variable);
        if (!iJustForwardCheck)
            propagate(queue);
    }
    
    protected boolean propagate(Variable aVariable, Variable anotherVariable, Vector adepts) {
        if (goodValues(aVariable).isEmpty())
            return false;
        boolean ret = false;
        Vector conflicts = null;
        for (Enumeration i = ((Vector)anotherVariable.constraintVariables().get(aVariable)).elements(); i.hasMoreElements();) {
            Constraint constraint = (Constraint)i.nextElement();
            for (Iterator i1 = goodValues(aVariable).iterator(); i1.hasNext();) {
                Value aValue = (Value)i1.next();
                if (conflicts == null)
                    conflicts = conflictValues(constraint, aValue, adepts);
                else
                    conflicts = conflictValues(constraint, aValue, conflicts);
                if (conflicts == null || conflicts.isEmpty())
                    break;
            }
            if (conflicts != null && !conflicts.isEmpty())
                for (Enumeration i1 = conflicts.elements(); i1.hasMoreElements();) {
                    Value conflictValue = (Value)i1.nextElement();
                    Set reason = reason(constraint, aVariable, conflictValue);
                    //sLogger.debug("  "+conflictValue+" become nogood (c:"+constraint.getName()+", r:"+reason+")");
                    setNoGood(conflictValue, reason);
                    adepts.removeElement(conflictValue);
                    if (reason.isEmpty())
                        ((Variable)conflictValue.variable()).removeValue(iIteration,conflictValue);
                    ret = true;
                }
        }
        return ret;
    }
    
    protected boolean propagate(Variable aVariable, Variable anotherVariable) {
        if (goodValues(anotherVariable).isEmpty())
            return false;
        return propagate(aVariable, anotherVariable, new FastVector(goodValues(anotherVariable)));
    }
    
    /** support values of a variable */
    private Set supportValues(Variable variable) {
        Set[] ret = (Set[])variable.getExtra();
        if (ret == null) {
            ret = new HashSet[] { new HashSet(1000), new HashSet()};
            variable.setExtra(ret);
        }
        return ret[0];
    }
    
    /** good values of a variable (values not removed from variables domain)*/
    public Set goodValues(Variable variable) {
        Set[] ret = (Set[])variable.getExtra();
        if (ret == null) {
            ret = new HashSet[] { new HashSet(1000), new HashSet()};
            variable.setExtra(ret);
        }
        return ret[1];
    }
    
    /** notification that a nogood value becomes good or vice versa */
    private void goodnessChanged(Value value) {
        if (isGood(value)) {
            goodValues(value.variable()).add(value);
        }
        else {
            goodValues(value.variable()).remove(value);
        }
    }
    /** removes support of a variable */
    private void removeSupport(Variable variable, Value value) {
        supportValues(variable).remove(value);
    }
    /** adds support of a variable */
    private void addSupport(Variable variable, Value value) {
        supportValues(variable).add(value);
    }
    
    /** variables explanation */
    public Set noGood(Value value) {
        return (Set)value.getExtra();
    }
    /** is variable good */
    public boolean isGood(Value value) {
        return (value.getExtra() == null);
    }
    /** sets value to be good */
    protected void setGood(Value value) {
        Set noGood = noGood(value);
        if (noGood != null)
            for (Iterator i = noGood.iterator(); i.hasNext();)
                removeSupport(((Value)i.next()).variable(), value);
        value.setExtra(null);
        goodnessChanged(value);
    }
    /** sets values explanation (initialization) */
    private void initNoGood(Value value, Set reason) {
        value.setExtra(reason);
    }
    /** sets value's explanation*/
    public void setNoGood(Value value, Set reason) {
        Set noGood = noGood(value);
        if (noGood != null)
            for (Iterator i = noGood.iterator(); i.hasNext();)
                removeSupport(((Value)i.next()).variable(), value);
        value.setExtra(reason);
        for (Iterator i = reason.iterator(); i.hasNext();) {
            Value aValue = (Value)i.next();
            addSupport(aValue.variable(), value);
        }
        goodnessChanged(value);
    }
    
    /** propagation over a constraint */
    private void propagate(Constraint constraint, Value anAssignedValue, net.sf.cpsolver.ifs.util.Queue queue) {
        HashSet reason = new HashSet(1);
        reason.add(anAssignedValue);
        Collection conflicts = conflictValues(constraint, anAssignedValue);
        if (conflicts != null && !conflicts.isEmpty())
            for (Iterator i1 = conflicts.iterator(); i1.hasNext();) {
                Value conflictValue = (Value)i1.next();
                //sLogger.debug("  "+conflictValue+" become nogood (c:"+constraint.getName()+", r:"+reason+")");
                setNoGood(conflictValue, reason);
                if (!queue.contains(conflictValue.variable()))
                    queue.put(conflictValue.variable());
            }
    }
    
    /** propagation over a constraint */
    private void propagate(Constraint constraint, Variable aVariable, net.sf.cpsolver.ifs.util.Queue queue) {
        if (goodValues(aVariable).isEmpty())
            return;
        Vector conflicts = conflictValues(constraint, aVariable);
        
        if (conflicts != null && !conflicts.isEmpty()) {
            for (Enumeration i1 = conflicts.elements(); i1.hasMoreElements(); ) {
                Value conflictValue = (Value)i1.nextElement();
                if (!queue.contains(conflictValue.variable()))
                    queue.put(conflictValue.variable());
                Set reason = reason(constraint, aVariable, conflictValue);
                //sLogger.debug("  "+conflictValue+" become nogood (c:"+constraint.getName()+", r:"+reason+")");
                setNoGood(conflictValue, reason);
                if (reason.isEmpty())
                    ((Variable)conflictValue.variable()).removeValue(iIteration, conflictValue);
            }
        }
    }
    
    private Vector conflictValues(Constraint constraint, Value aValue) {
        Vector ret = new FastVector();
        
        for (Enumeration i1 = constraint.variables().elements(); i1.hasMoreElements();) {
            Variable variable = (Variable)i1.nextElement();
            if (variable.equals(aValue.variable()))
                continue;
            if (variable.getAssignment() != null)
                continue;
            
            for (Iterator i2 = goodValues(variable).iterator();i2.hasNext();) {
                Value value = (Value)i2.next();
                if (!constraint.isConsistent(aValue, value))
                    ret.addElement(value);
            }
        }
        return ret;
    }
    
    private Vector conflictValues(Constraint constraint, Value aValue, Vector values) {
        Vector ret = new FastVector(values.size());
        
        for (Enumeration i1 = values.elements(); i1.hasMoreElements();) {
            Value value = (Value)i1.nextElement();
            if (!constraint.isConsistent(aValue, value))
                ret.addElement(value);
        }
        return ret;
    }
    
    private Vector conflictValues(Constraint constraint, Variable aVariable) {
        Vector conflicts = null;
        for (Iterator i1 = goodValues(aVariable).iterator(); i1.hasNext();) {
            Value aValue = (Value)i1.next();
            if (conflicts == null)
                conflicts = conflictValues(constraint, aValue);
            else
                conflicts = conflictValues(constraint, aValue, conflicts);
            if (conflicts == null || conflicts.isEmpty())
                return null;
        }
        return conflicts;
    }
    
    private HashSet reason(Constraint constraint, Variable aVariable, Value aValue) {
        HashSet ret = new HashSet();
        for (Enumeration i1 = aVariable.values().elements(); i1.hasMoreElements();) {
            Value value = (Value)i1.nextElement();
            if (constraint.isConsistent(aValue, value)) {
                if (noGood(value) == null)
                    sLogger.error("Something went wrong: value " + value + " cannot participate in a reason.");
                else
                    ret.addAll(noGood(value));
            }
        }
        return ret;
    }
    
}
