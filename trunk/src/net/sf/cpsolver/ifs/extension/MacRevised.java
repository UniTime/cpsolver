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
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 */

public class MacRevised<V extends Variable<V, T>, T extends Value<V, T>> extends Extension<V, T> {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(MacRevised.class);
    private boolean iDbt = false;
    private Progress iProgress;

    /** List of constraints on which arc-consistency is to be maintained */
    protected List<Constraint<V, T>> iConstraints = null;
    /** Current iteration */
    protected long iIteration = 0;

    /** Constructor */
    public MacRevised(Solver<V, T> solver, DataProperties properties) {
        super(solver, properties);
        iDbt = properties.getPropertyBoolean("MacRevised.Dbt", false);
    }

    /** Adds a constraint on which arc-consistency is to be maintained */
    public void addConstraint(Constraint<V, T> constraint) {
        if (iConstraints == null)
            iConstraints = new ArrayList<Constraint<V, T>>();
        iConstraints.add(constraint);
    }

    /**
     * Returns true, if arc-consistency is to be maintained on the given
     * constraint
     */
    public boolean contains(Constraint<V, T> constraint) {
        if (iConstraints == null)
            return true;
        return iConstraints.contains(constraint);
    }

    /**
     * Before a value is unassigned: until the value is inconsistent with the
     * current solution, an assignment from its explanation is picked and
     * unassigned.
     */
    @Override
    public void beforeAssigned(long iteration, T value) {
        if (value == null)
            return;
        sLogger.debug("Before assign " + value.variable().getName() + " = " + value.getName());
        iIteration = iteration;
        while (!isGood(value) && !noGood(value).isEmpty()) {
            if (iDbt)
                sLogger.warn("Going to assign a no-good value " + value + " (noGood:" + noGood(value) + ").");
            T noGoodValue = noGood(value).iterator().next();
            noGoodValue.variable().unassign(iteration);
        }
        if (!isGood(value)) {
            sLogger.warn("Going to assign a bad value " + value + " with empty no-good.");
        }
    }

    /**
     * After a value is assigned: explanations of other values of the value's
     * variable are reset (to contain only the assigned value), propagation over
     * the assigned variable takes place.
     */
    @Override
    public void afterAssigned(long iteration, T value) {
        sLogger.debug("After assign " + value.variable().getName() + " = " + value.getName());
        iIteration = iteration;
        if (!isGood(value)) {
            sLogger.warn(value.variable().getName() + " = " + value.getName() + " -- not good value assigned (noGood:"
                    + noGood(value) + ")");
            setGood(value);
        }

        Set<T> noGood = new HashSet<T>(1);
        noGood.add(value);
        List<T> queue = new ArrayList<T>();
        for (Iterator<T> i = value.variable().values().iterator(); i.hasNext();) {
            T anotherValue = i.next();
            if (anotherValue.equals(value) || !isGood(anotherValue))
                continue;
            setNoGood(anotherValue, noGood);
            queue.add(anotherValue);
        }
        propagate(queue);
    }

    /**
     * After a value is unassigned: explanations of all values of unassigned
     * variable are recomputed ({@link Value#conflicts()}), propagation undo
     * over the unassigned variable takes place.
     */
    @Override
    public void afterUnassigned(long iteration, T value) {
        sLogger.debug("After unassign " + value.variable().getName() + " = " + value.getName());
        iIteration = iteration;
        if (!isGood(value))
            sLogger.error(value.variable().getName() + " = " + value.getName()
                    + " -- not good value unassigned (noGood:" + noGood(value) + ")");

        List<T> back = new ArrayList<T>(supportValues(value.variable()));
        for (T aValue : back) {
            if (aValue.variable().getAssignment() != null) {
                Set<T> noGood = new HashSet<T>(1);
                noGood.add(aValue.variable().getAssignment());
                setNoGood(aValue, noGood);
            } else
                setGood(aValue);
        }

        List<T> queue = new ArrayList<T>();
        for (T aValue : back) {
            if (!isGood(aValue) || revise(aValue))
                queue.add(aValue);
        }

        propagate(queue);
    }

    public void propagate(List<T> queue) {
        int idx = 0;
        while (queue.size() > idx) {
            T value = queue.get(idx++);
            sLogger.debug("  -- propagate " + value.variable().getName() + " = " + value.getName() + " (noGood:"
                    + noGood(value) + ")");
            if (goodValues(value.variable()).isEmpty()) {
                sLogger.info("Empty domain detected for variable " + value.variable().getName());
                continue;
            }
            for (Constraint<V, T> constraint : value.variable().hardConstraints()) {
                if (!contains(constraint))
                    continue;
                propagate(constraint, value, queue);
            }
        }
    }

    public void propagate(Constraint<V, T> constraint, T noGoodValue, List<T> queue) {
        for (V aVariable : constraint.variables()) {
            if (aVariable.equals(noGoodValue.variable()))
                continue;
            for (Iterator<T> j = aVariable.values().iterator(); j.hasNext();) {
                T aValue = j.next();
                if (isGood(aValue) && constraint.isConsistent(noGoodValue, aValue)
                        && !hasSupport(constraint, aValue, noGoodValue.variable())) {
                    setNoGood(aValue, explanation(constraint, aValue, noGoodValue.variable()));
                    queue.add(aValue);
                }
            }
        }
    }

    public boolean revise(T value) {
        sLogger.debug("  -- revise " + value.variable().getName() + " = " + value.getName());
        for (Constraint<V, T> constraint : value.variable().hardConstraints()) {
            if (!contains(constraint))
                continue;
            if (revise(constraint, value))
                return true;
        }
        return false;
    }

    public boolean revise(Constraint<V, T> constraint, T value) {
        for (V aVariable : constraint.variables()) {
            if (aVariable.equals(value.variable()))
                continue;
            if (!hasSupport(constraint, value, aVariable)) {
                setNoGood(value, explanation(constraint, value, aVariable));
                return true;
            }
        }
        return false;
    }

    public Set<T> explanation(Constraint<V, T> constraint, T value, V variable) {
        Set<T> expl = new HashSet<T>();
        for (T aValue : variable.values()) {
            if (constraint.isConsistent(aValue, value)) {
                expl.addAll(noGood(aValue));
            }
        }
        return expl;
    }

    public Set<T> supports(Constraint<V, T> constraint, T value, V variable) {
        Set<T> sup = new HashSet<T>();
        for (T aValue : variable.values()) {
            if (!isGood(aValue))
                continue;
            if (!constraint.isConsistent(aValue, value))
                continue;
            sup.add(aValue);
        }
        return sup;
    }

    public boolean hasSupport(Constraint<V, T> constraint, T value, V variable) {
        for (T aValue : variable.values()) {
            if (isGood(aValue) && constraint.isConsistent(aValue, value)) {
                // sLogger.debug("    -- "+variable.getName()+" = "+aValue.getName()+" supports "
                // +
                // value.variable().getName()+" = "+value.getName()+" (constraint:"+constraint.getName()+")");
                return true;
            }
        }
        // sLogger.debug("    -- value "+value.variable().getName()+" = " +
        // value.getName()+" has no support from values of variable "+variable.getName()+" (constraint:"+constraint.getName()+")");
        /*
         * for (Enumeration e=variable.values().elements();e.hasMoreElements();)
         * { T aValue = (T)e.nextElement(); if
         * (constraint.isConsistent(aValue,value)) {
         * //sLogger.debug("      -- support "
         * +aValue.getName()+" is not good: "+expl2str(noGood(aValue))); } }
         */
        return false;
    }

    /**
     * Initialization. Enforce arc-consistency over the current (initial)
     * solution. AC3 algorithm is used.
     */
    @Override
    public boolean init(Solver<V, T> solver) {
        boolean isOk = true;
        iProgress = Progress.getInstance(getModel());
        iProgress.save();
        iProgress.setPhase("Initializing propagation:", getModel().variables().size());
        for (Iterator<V> i = getModel().variables().iterator(); i.hasNext();) {
            V aVariable = i.next();
            supportValues(aVariable).clear();
            goodValues(aVariable).clear();
        }
        List<T> queue = new ArrayList<T>();
        for (Iterator<V> i = getModel().variables().iterator(); i.hasNext();) {
            V aVariable = i.next();
            for (Iterator<T> j = aVariable.values().iterator(); j.hasNext();) {
                T aValue = j.next();
                initNoGood(aValue, null);
                goodValues(aVariable).add(aValue);
                if (revise(aValue)) {
                    queue.add(aValue);
                } else if (aVariable.getAssignment() != null && !aValue.equals(aVariable.getAssignment())) {
                    Set<T> noGood = new HashSet<T>();
                    noGood.add(aVariable.getAssignment());
                    setNoGood(aValue, noGood);
                    queue.add(aValue);
                }
            }
            iProgress.incProgress();
        }
        propagate(queue);
        iProgress.restore();
        return isOk;
    }

    /** support values of a variable */
    @SuppressWarnings("unchecked")
    private Set<T> supportValues(V variable) {
        Set<T>[] ret = (Set<T>[]) variable.getExtra();
        if (ret == null) {
            ret = new Set[] { new HashSet<T>(1000), new HashSet<T>() };
            variable.setExtra(ret);
        }
        return ret[0];
    }

    /** good values of a variable (values not removed from variables domain) */
    @SuppressWarnings("unchecked")
    public Set<T> goodValues(V variable) {
        Set<T>[] ret = (Set<T>[]) variable.getExtra();
        if (ret == null) {
            ret = new Set[] { new HashSet<T>(1000), new HashSet<T>() };
            variable.setExtra(ret);
        }
        return ret[1];
    }

    /** notification that a nogood value becomes good or vice versa */
    private void goodnessChanged(T value) {
        if (isGood(value)) {
            goodValues(value.variable()).add(value);
        } else {
            goodValues(value.variable()).remove(value);
        }
    }

    /** removes support of a variable */
    private void removeSupport(V variable, T value) {
        supportValues(variable).remove(value);
    }

    /** adds support of a variable */
    private void addSupport(V variable, T value) {
        supportValues(variable).add(value);
    }

    /** variables explanation */
    @SuppressWarnings("unchecked")
    public Set<T> noGood(T value) {
        return (Set<T>) value.getExtra();
    }

    /** is variable good */
    public boolean isGood(T value) {
        return (value.getExtra() == null);
    }

    /** sets value to be good */
    protected void setGood(T value) {
        sLogger.debug("    -- set good " + value.variable().getName() + " = " + value.getName());
        Set<T> noGood = noGood(value);
        if (noGood != null)
            for (T v : noGood)
                removeSupport(v.variable(), value);
        value.setExtra(null);
        goodnessChanged(value);
    }

    /** sets values explanation (initialization) */
    private void initNoGood(T value, Set<T> reason) {
        value.setExtra(reason);
    }

    private String expl2str(Set<T> expl) {
        StringBuffer sb = new StringBuffer("[");
        for (Iterator<T> i = expl.iterator(); i.hasNext();) {
            T value = i.next();
            sb.append(value.variable().getName() + "=" + value.getName());
            if (i.hasNext())
                sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    private void checkExpl(Set<T> expl) {
        sLogger.debug("    -- checking explanation: " + expl2str(expl));
        for (Iterator<T> i = expl.iterator(); i.hasNext();) {
            T value = i.next();
            if (!value.equals(value.variable().getAssignment())) {
                if (value.variable().getAssignment() == null)
                    sLogger.warn("      -- variable " + value.variable().getName() + " unassigned");
                else
                    sLogger.warn("      -- variable " + value.variable().getName() + " assigned to a different value "
                            + value.variable().getAssignment().getName());
            }
        }
    }

    private void printAssignments() {
        sLogger.debug("    -- printing assignments: ");
        for (Iterator<V> i = getModel().variables().iterator(); i.hasNext();) {
            V variable = i.next();
            if (variable.getAssignment() != null)
                sLogger.debug("      -- " + variable.getName() + " = " + variable.getAssignment().getName());
        }
    }

    /** sets value's explanation */
    public void setNoGood(T value, Set<T> reason) {
        sLogger.debug("    -- set nogood " + value.variable().getName() + " = " + value.getName() + "(expl:"
                + expl2str(reason) + ")");
        if (value.equals(value.variable().getAssignment())) {
            try {
                throw new Exception("An assigned value " + value.variable().getName() + " = " + value.getName()
                        + " become no good (noGood:" + reason + ")!!");
            } catch (Exception e) {
                sLogger.warn(e.getMessage(), e);
            }
            checkExpl(reason);
            printAssignments();
        }
        Set<T> noGood = noGood(value);
        if (noGood != null)
            for (T v : noGood)
                removeSupport(v.variable(), value);
        value.setExtra(reason);
        for (T aValue : reason) {
            addSupport(aValue.variable(), value);
        }
        goodnessChanged(value);
    }
}