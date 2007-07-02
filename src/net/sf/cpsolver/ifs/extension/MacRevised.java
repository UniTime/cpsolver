package net.sf.cpsolver.ifs.extension;


import java.util.*;

import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.solver.*;
import net.sf.cpsolver.ifs.util.*;

/**
 * Another implementation of MAC propagation.
 * 
 * @see MacPropagation
 *
 * @version
 * IFS 1.1 (Iterative Forward Search)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
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

public class MacRevised extends Extension {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(MacRevised.class);
    private boolean iDbt = false;
    private Progress iProgress;
    
    /** List of constraints on which arc-consistency is to be maintained */
    protected Vector iConstraints = null;
    /** Current iteration */
    protected long iIteration = 0;
    
    /** Constructor */
    public MacRevised(Solver solver, DataProperties properties) {
        super(solver, properties);
        iProgress = Progress.getInstance(getModel());
        iDbt = properties.getPropertyBoolean("MacRevised.Dbt", false);
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
        sLogger.debug("Before assign "+value.variable().getName() + " = " + value.getName());
        iIteration = iteration;
        if (value == null) return;
        while (!isGood(value) && !noGood(value).isEmpty()) {
            if (iDbt) sLogger.warn("Going to assign a no-good value "+value+" (noGood:" + noGood(value) + ").");
            Value noGoodValue = (Value)noGood(value).iterator().next();
            noGoodValue.variable().unassign(iteration);
        }
        if (!isGood(value)) {
            sLogger.warn("Going to assign a bad value "+value+" with empty no-good.");
        }
    }
    
    /** After a value is assigned: explanations of other values of the value's variable are reset (to contain only the assigned value), 
     * propagation over the assigned variable takes place.
     */
    public void afterAssigned(long iteration, Value value) {
        sLogger.debug("After assign "+value.variable().getName() + " = " + value.getName());
        iIteration = iteration;
        if (!isGood(value)) {
            sLogger.warn(value.variable().getName() + " = " + value.getName() + " -- not good value assigned (noGood:" + noGood(value) + ")");
            setGood(value);
        }
        
        HashSet noGood = new HashSet(1); noGood.add(value);
        Vector queue = new Vector();
        for (Enumeration i = value.variable().values().elements(); i.hasMoreElements(); ) {
            Value anotherValue = (Value)i.nextElement();
            if (anotherValue.equals(value) || !isGood(anotherValue)) continue;
            setNoGood(anotherValue, noGood);
            queue.add(anotherValue);
        }
        propagate(queue);
    }
    
    /** After a value is unassigned: explanations of all values of unassigned variable are recomputed ({@link Value#conflicts()}), propagation
     * undo over the unassigned variable takes place.
     */
    public void afterUnassigned(long iteration, Value value) {
        sLogger.debug("After unassign "+value.variable().getName() + " = " + value.getName());
        iIteration = iteration;
        if (!isGood(value))
            sLogger.error(value.variable().getName() + " = " + value.getName() + " -- not good value unassigned (noGood:" + noGood(value) + ")");

        Vector back = new Vector(supportValues(value.variable()));
        for (Enumeration e=back.elements();e.hasMoreElements();) {
            Value aValue = (Value)e.nextElement();
            if (aValue.variable().getAssignment()!=null) {
                HashSet noGood = new HashSet(1); noGood.add(aValue.variable().getAssignment());
                setNoGood(aValue, noGood);
            } else
                setGood(aValue);
        }
        
        Vector queue = new Vector();
        for (Enumeration e=back.elements();e.hasMoreElements();) {
            Value aValue = (Value)e.nextElement();
            if (!isGood(aValue) || revise(aValue))
                queue.add(aValue);
        }

        propagate(queue);
    }
    
    public void propagate(Vector queue) {
        int idx = 0;
        while (queue.size()>idx) {
            Value value = (Value)queue.elementAt(idx++);
            sLogger.debug("  -- propagate "+value.variable().getName()+" = " + value.getName()+" (noGood:" + noGood(value) + ")");
            if (goodValues(value.variable()).isEmpty()) {
                sLogger.info("Empty domain detected for variable "+value.variable().getName());
                continue;
            }
            for (Enumeration e=value.variable().hardConstraints().elements(); e.hasMoreElements(); ) {
                Constraint constraint = (Constraint)e.nextElement();
                if (!contains(constraint)) continue;
                propagate(constraint, value, queue);
            }
        }
    }
    
    public void propagate(Constraint constraint, Value noGoodValue, Vector queue) {
        for (Enumeration e1 = constraint.variables().elements();e1.hasMoreElements();) {
            Variable aVariable = (Variable)e1.nextElement();
            if (aVariable.equals(noGoodValue.variable())) continue;
            for (Enumeration e2=aVariable.values().elements(); e2.hasMoreElements();) {
                Value aValue = (Value)e2.nextElement();
                if (isGood(aValue) && constraint.isConsistent(noGoodValue,aValue) && !hasSupport(constraint, aValue, noGoodValue.variable())) {
                    setNoGood(aValue, explanation(constraint, aValue, noGoodValue.variable()));
                    queue.addElement(aValue);
                }
            }
        }
    }
    
    public boolean revise(Value value) {
        sLogger.debug("  -- revise "+value.variable().getName()+" = " + value.getName());
        for (Enumeration e=value.variable().hardConstraints().elements(); e.hasMoreElements(); ) {
            Constraint constraint = (Constraint)e.nextElement();
            if (!contains(constraint)) continue;
            if (revise(constraint, value)) return true;
        }
        return false;
    }
    
    public boolean revise(Constraint constraint, Value value) {
        for (Enumeration e1 = constraint.variables().elements();e1.hasMoreElements();) {
            Variable aVariable = (Variable)e1.nextElement();
            if (aVariable.equals(value.variable())) continue;
            if (!hasSupport(constraint, value, aVariable)) {
                setNoGood(value, explanation(constraint, value, aVariable));
                return true;
            }
        }
        return false;
    }
    
    public HashSet explanation(Constraint constraint, Value value, Variable variable) {
        HashSet expl = new HashSet();
        for (Enumeration e=variable.values().elements();e.hasMoreElements();) {
            Value aValue = (Value)e.nextElement();
            if (constraint.isConsistent(aValue,value)) {
                expl.addAll(noGood(aValue));
            }
        }
        return expl;
    }
    
    public HashSet supports(Constraint constraint, Value value, Variable variable) {
        HashSet sup = new HashSet();
        for (Enumeration e=variable.values().elements();e.hasMoreElements();) {
            Value aValue = (Value)e.nextElement();
            if (!isGood(aValue)) continue;
            if (!constraint.isConsistent(aValue,value)) continue;
            sup.add(aValue);
        }
        return sup;
    }    
    
    public boolean hasSupport(Constraint constraint, Value value, Variable variable) {
        for (Enumeration e=variable.values().elements();e.hasMoreElements();) {
            Value aValue = (Value)e.nextElement();
            if (isGood(aValue) && constraint.isConsistent(aValue,value)) {
                //sLogger.debug("    -- "+variable.getName()+" = "+aValue.getName()+" supports " + value.variable().getName()+" = "+value.getName()+" (constraint:"+constraint.getName()+")");
                return true;
            }
        }
        //sLogger.debug("    -- value "+value.variable().getName()+" = " + value.getName()+" has no support from values of variable "+variable.getName()+" (constraint:"+constraint.getName()+")");
        /*
        for (Enumeration e=variable.values().elements();e.hasMoreElements();) {
            Value aValue = (Value)e.nextElement();
            if (constraint.isConsistent(aValue,value)) {
                //sLogger.debug("      -- support "+aValue.getName()+" is not good: "+expl2str(noGood(aValue)));
            }
        }
         */
        return false;
    }
    
    
    /** Initialization. Enforce arc-consistency over the current (initial) solution. AC3 algorithm is used. */
    public boolean init(Solver solver) {
        boolean isOk = true;
        for (Enumeration e=getModel().constraints().elements();e.hasMoreElements();) {
            Constraint constraint = (Constraint)e.nextElement();
            sLogger.debug("Constraint "+constraint.getName());
        }
        iProgress.save();
        iProgress.setPhase("Initializing propagation:", getModel().variables().size());
        for (Enumeration i1 = getModel().variables().elements(); i1.hasMoreElements(); ) {
            Variable aVariable = (Variable)i1.nextElement();
            supportValues(aVariable).clear();
            goodValues(aVariable).clear();
        }
        Vector queue = new Vector();
        for (Enumeration i1 = getModel().variables().elements(); i1.hasMoreElements(); ) {
            Variable aVariable = (Variable)i1.nextElement();
            for (Enumeration i2 = aVariable.values().elements(); i2.hasMoreElements(); ) {
                Value aValue = (Value)i2.nextElement();
                initNoGood(aValue, null);
                goodValues(aVariable).add(aValue);
                if (revise(aValue)) {
                    queue.add(aValue);
                } else if (aVariable.getAssignment()!=null && !aValue.equals(aVariable.getAssignment())) {
                    HashSet noGood = new HashSet(); noGood.add(aVariable.getAssignment());
                    setNoGood(aValue, noGood);
                    queue.add(noGood);
                }
            }
            iProgress.incProgress();
        }
        propagate(queue);
        iProgress.restore();
        return isOk;
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
        sLogger.debug("    -- set good "+value.variable().getName()+" = " + value.getName());
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
    
    private static String expl2str(Set expl) {
        StringBuffer sb = new StringBuffer("[");
        for (Iterator i=expl.iterator();i.hasNext();) {
            Value value = (Value)i.next();
            sb.append(value.variable().getName()+"=" + value.getName());
            if (i.hasNext()) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
    
    private static void checkExpl(Set expl) {
        sLogger.debug("    -- checking explanation: "+expl2str(expl));
        for (Iterator i=expl.iterator();i.hasNext();) {
            Value value = (Value)i.next();
            if (!value.equals(value.variable().getAssignment())) {
                if (value.variable().getAssignment()==null)
                    sLogger.warn("      -- variable "+value.variable().getName()+" unassigned");
                else
                    sLogger.warn("      -- variable "+value.variable().getName()+" assigned to a different value "+value.variable().getAssignment().getName());
            }
        }
    }
    
    private void printAssignments() {
        sLogger.debug("    -- printing assignments: ");
        for (Enumeration e=getModel().variables().elements();e.hasMoreElements();) {
            Variable variable = (Variable)e.nextElement();
            if (variable.getAssignment()!=null)
                sLogger.debug("      -- "+variable.getName()+" = "+variable.getAssignment().getName());
        }
    }

    /** sets value's explanation*/
    public void setNoGood(Value value, Set reason) {
        sLogger.debug("    -- set nogood "+value.variable().getName()+" = " + value.getName()+"(expl:"+expl2str(reason)+")");
        if (value.equals(value.variable().getAssignment())) {
            try {
                throw new Exception("An assigned value "+value.variable().getName() + " = " + value.getName() + " become no good (noGood:" + reason + ")!!");
            } catch (Exception e) {
                sLogger.warn(e.getMessage(),e);
            }
            checkExpl(reason);
            printAssignments();
        }
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
}