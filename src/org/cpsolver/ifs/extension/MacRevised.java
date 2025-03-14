package org.cpsolver.ifs.extension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentContext;
import org.cpsolver.ifs.assignment.context.ExtensionWithContext;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.Progress;


/**
 * Another implementation of MAC propagation.
 * 
 * @see MacPropagation
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
 * @param <V> Variable
 * @param <T> Value
 */

public class MacRevised<V extends Variable<V, T>, T extends Value<V, T>> extends ExtensionWithContext<V, T, MacRevised<V, T>.NoGood> {
    private static org.apache.logging.log4j.Logger sLogger = org.apache.logging.log4j.LogManager.getLogger(MacRevised.class);
    private boolean iDbt = false;
    private Progress iProgress;

    /** List of constraints on which arc-consistency is to be maintained */
    protected List<Constraint<V, T>> iConstraints = null;
    /** Current iteration */
    protected long iIteration = 0;

    /** Constructor 
     * @param solver current solver
     * @param properties solver configuration
     **/
    public MacRevised(Solver<V, T> solver, DataProperties properties) {
        super(solver, properties);
        iDbt = properties.getPropertyBoolean("MacRevised.Dbt", false);
    }

    /** Adds a constraint on which arc-consistency is to be maintained 
     * @param constraint a hard constraint to be added
     **/
    public void addConstraint(Constraint<V, T> constraint) {
        if (iConstraints == null)
            iConstraints = new ArrayList<Constraint<V, T>>();
        iConstraints.add(constraint);
    }

    /**
     * Returns true, if arc-consistency is to be maintained on the given
     * constraint
     * @param constraint a constraint
     * @return true if the constraint is in the set
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
    public void beforeAssigned(Assignment<V, T> assignment, long iteration, T value) {
        if (value == null)
            return;
        sLogger.debug("Before assign " + value.variable().getName() + " = " + value.getName());
        iIteration = iteration;
        NoGood context = getContext(assignment);
        while (!context.isGood(value) && !context.noGood(value).isEmpty()) {
            if (iDbt)
                sLogger.warn("Going to assign a no-good value " + value + " (noGood:" + context.noGood(value) + ").");
            T noGoodValue = context.noGood(value).iterator().next();
            assignment.unassign(iteration, noGoodValue.variable());
        }
        if (!context.isGood(value)) {
            sLogger.warn("Going to assign a bad value " + value + " with empty no-good.");
        }
    }

    /**
     * After a value is assigned: explanations of other values of the value's
     * variable are reset (to contain only the assigned value), propagation over
     * the assigned variable takes place.
     */
    @Override
    public void afterAssigned(Assignment<V, T> assignment, long iteration, T value) {
        sLogger.debug("After assign " + value.variable().getName() + " = " + value.getName());
        iIteration = iteration;
        NoGood context = getContext(assignment);
        if (!context.isGood(value)) {
            sLogger.warn(value.variable().getName() + " = " + value.getName() + " -- not good value assigned (noGood:" + context.noGood(value) + ")");
            context.setGood(value);
        }

        Set<T> noGood = new HashSet<T>(1);
        noGood.add(value);
        List<T> queue = new ArrayList<T>();
        for (Iterator<T> i = value.variable().values(assignment).iterator(); i.hasNext();) {
            T anotherValue = i.next();
            if (anotherValue.equals(value) || !context.isGood(anotherValue))
                continue;
            context.setNoGood(anotherValue, noGood);
            queue.add(anotherValue);
        }
        context.propagate(assignment, queue);
    }

    /**
     * After a value is unassigned: explanations of all values of unassigned
     * variable are recomputed ({@link Value#conflicts(Assignment)}), propagation undo
     * over the unassigned variable takes place.
     */
    @Override
    public void afterUnassigned(Assignment<V, T> assignment, long iteration, T value) {
        sLogger.debug("After unassign " + value.variable().getName() + " = " + value.getName());
        iIteration = iteration;
        NoGood context = getContext(assignment);
        if (!context.isGood(value))
            sLogger.error(value.variable().getName() + " = " + value.getName() + " -- not good value unassigned (noGood:" + context.noGood(value) + ")");

        List<T> back = new ArrayList<T>(context.supportValues(value.variable()));
        for (T aValue : back) {
            T current = assignment.getValue(aValue.variable());
            if (current != null) {
                Set<T> noGood = new HashSet<T>(1);
                noGood.add(current);
                context.setNoGood(aValue, noGood);
            } else
                context.setGood(aValue);
        }

        List<T> queue = new ArrayList<T>();
        for (T aValue : back) {
            if (!context.isGood(aValue) || context.revise(assignment, aValue))
                queue.add(aValue);
        }

        context.propagate(assignment, queue);
    }

    /**
     * Initialization. Enforce arc-consistency over the current (initial)
     * solution. AC3 algorithm is used.
     */
    @Override
    public boolean init(Solver<V, T> solver) {
        return true;
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

    private void checkExpl(Assignment<V, T> assignment, Set<T> expl) {
        sLogger.debug("    -- checking explanation: " + expl2str(expl));
        for (Iterator<T> i = expl.iterator(); i.hasNext();) {
            T value = i.next();
            T current = assignment.getValue(value.variable());
            if (!value.equals(current)) {
                if (current == null)
                    sLogger.warn("      -- variable " + value.variable().getName() + " unassigned");
                else
                    sLogger.warn("      -- variable " + value.variable().getName() + " assigned to a different value " + current.getName());
            }
        }
    }

    private void printAssignments(Assignment<V, T> assignment) {
        sLogger.debug("    -- printing assignments: ");
        for (Iterator<V> i = getModel().variables().iterator(); i.hasNext();) {
            V variable = i.next();
            T value = assignment.getValue(variable);
            if (value != null)
                sLogger.debug("      -- " + variable.getName() + " = " + value.getName());
        }
    }

    @Override
    public NoGood createAssignmentContext(Assignment<V, T> assignment) {
        return new NoGood(assignment);
    }

    /**
     * Assignment context
     */
    public class NoGood implements AssignmentContext {
        private Map<V, Set<T>[]> iNoGood = new HashMap<V, Set<T>[]>();
        private Map<V, Map<T, Set<T>>> iNoGoodVal = new HashMap<V, Map<T, Set<T>>>();
        
        public NoGood(Assignment<V, T> assignment) {
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
                for (Iterator<T> j = aVariable.values(assignment).iterator(); j.hasNext();) {
                    T aValue = j.next();
                    initNoGood(aValue, null);
                    goodValues(aVariable).add(aValue);
                    T current = assignment.getValue(aVariable);
                    if (revise(assignment, aValue)) {
                        queue.add(aValue);
                    } else if (current != null && !aValue.equals(current)) {
                        Set<T> noGood = new HashSet<T>();
                        noGood.add(current);
                        setNoGood(aValue, noGood);
                        queue.add(aValue);
                    }
                }
                iProgress.incProgress();
            }
            propagate(assignment, queue);
            iProgress.restore();
        }
        
        public Set<T>[] getNoGood(V variable) {
            return iNoGood.get(variable);
        }
        
        public void setNoGood(V variable, Set<T>[] noGood) {
            if (noGood == null)
                iNoGood.remove(variable);
            else
                iNoGood.put(variable, noGood);
        }
        
        public Set<T> getNoGood(T value) {
            Map<T, Set<T>> ng = iNoGoodVal.get(value.variable());
            if (ng == null) return null;
            return ng.get(value);
        }
        
        public void setNoGood(T value, Set<T> noGood) {
            Map<T, Set<T>> ng = iNoGoodVal.get(value.variable());
            if (ng == null) {
                ng = new HashMap<T, Set<T>>();
                iNoGoodVal.put(value.variable(), ng);
            }
            if (noGood == null)
                ng.remove(value);
            else
                ng.put(value, noGood);
        }
        
        /** support values of a variable */
        @SuppressWarnings("unchecked")
        private Set<T> supportValues(V variable) {
            Set<T>[] ret = getNoGood(variable);
            if (ret == null) {
                ret = new Set[] { new HashSet<T>(1000), new HashSet<T>() };
                setNoGood(variable, ret);
            }
            return ret[0];
        }

        /** good values of a variable (values not removed from variables domain) 
         * @param variable given variable
         * @return good values for the variable (i.e., values from the domain of the variable that have no no-good set)
         **/
        @SuppressWarnings("unchecked")
        public Set<T> goodValues(V variable) {
            Set<T>[] ret = getNoGood(variable);
            if (ret == null) {
                ret = new Set[] { new HashSet<T>(1000), new HashSet<T>() };
                setNoGood(variable, ret);
            }
            return ret[1];
        }

        /** notification that a no-good value becomes good or vice versa */
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

        /** variables explanation 
         * @param value given value
         * @return no-good set for the value 
         **/
        public Set<T> noGood(T value) {
            return getNoGood(value);
        }

        /** is variable good 
         * @param value given value
         * @return true if good, i.e., the value has no no-good set 
         **/
        public boolean isGood(T value) {
            return (getNoGood(value) == null);
        }

        /** sets value to be good 
         * @param value given value
         **/
        protected void setGood(T value) {
            sLogger.debug("    -- set good " + value.variable().getName() + " = " + value.getName());
            Set<T> noGood = noGood(value);
            if (noGood != null)
                for (T v : noGood)
                    removeSupport(v.variable(), value);
            setNoGood(value, null);
            goodnessChanged(value);
        }

        /** sets values explanation (initialization) */
        private void initNoGood(T value, Set<T> reason) {
            setNoGood(value, reason);
        }
        
        public void propagate(Assignment<V, T> assignment, List<T> queue) {
            int idx = 0;
            while (queue.size() > idx) {
                T value = queue.get(idx++);
                sLogger.debug("  -- propagate " + value.variable().getName() + " = " + value.getName() + " (noGood:" + noGood(value) + ")");
                if (goodValues(value.variable()).isEmpty()) {
                    sLogger.info("Empty domain detected for variable " + value.variable().getName());
                    continue;
                }
                for (Constraint<V, T> constraint : value.variable().hardConstraints()) {
                    if (!contains(constraint))
                        continue;
                    propagate(assignment, constraint, value, queue);
                }
            }
        }

        public void propagate(Assignment<V, T> assignment, Constraint<V, T> constraint, T noGoodValue, List<T> queue) {
            for (V aVariable : constraint.variables()) {
                if (aVariable.equals(noGoodValue.variable()))
                    continue;
                for (Iterator<T> j = aVariable.values(assignment).iterator(); j.hasNext();) {
                    T aValue = j.next();
                    if (isGood(aValue) && constraint.isConsistent(noGoodValue, aValue)
                            && !hasSupport(assignment, constraint, aValue, noGoodValue.variable())) {
                        setNoGood(aValue, explanation(assignment, constraint, aValue, noGoodValue.variable()));
                        queue.add(aValue);
                    }
                }
            }
        }

        public boolean revise(Assignment<V, T> assignment, T value) {
            sLogger.debug("  -- revise " + value.variable().getName() + " = " + value.getName());
            for (Constraint<V, T> constraint : value.variable().hardConstraints()) {
                if (!contains(constraint))
                    continue;
                if (revise(assignment, constraint, value))
                    return true;
            }
            return false;
        }

        public boolean revise(Assignment<V, T> assignment, Constraint<V, T> constraint, T value) {
            for (V aVariable : constraint.variables()) {
                if (aVariable.equals(value.variable()))
                    continue;
                if (!hasSupport(assignment, constraint, value, aVariable)) {
                    setNoGood(value, explanation(assignment, constraint, value, aVariable));
                    return true;
                }
            }
            return false;
        }

        public Set<T> explanation(Assignment<V, T> assignment, Constraint<V, T> constraint, T value, V variable) {
            Set<T> expl = new HashSet<T>();
            for (T aValue : variable.values(assignment)) {
                if (constraint.isConsistent(aValue, value)) {
                    expl.addAll(noGood(aValue));
                }
            }
            return expl;
        }

        public Set<T> supports(Assignment<V, T> assignment, Constraint<V, T> constraint, T value, V variable) {
            Set<T> sup = new HashSet<T>();
            for (T aValue : variable.values(assignment)) {
                if (!isGood(aValue))
                    continue;
                if (!constraint.isConsistent(aValue, value))
                    continue;
                sup.add(aValue);
            }
            return sup;
        }

        public boolean hasSupport(Assignment<V, T> assignment, Constraint<V, T> constraint, T value, V variable) {
            for (T aValue : variable.values(assignment)) {
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
        
        /** sets value's explanation 
         * @param assignment current assignment
         * @param value a value
         * @param reason no-good set for the value
         **/
        public void setNoGood(Assignment<V, T> assignment, T value, Set<T> reason) {
            sLogger.debug("    -- set nogood " + value.variable().getName() + " = " + value.getName() + "(expl:" + expl2str(reason) + ")");
            if (value.equals(assignment.getValue(value.variable()))) {
                try {
                    throw new Exception("An assigned value " + value.variable().getName() + " = " + value.getName() + " become no good (noGood:" + reason + ")!!");
                } catch (Exception e) {
                    sLogger.warn(e.getMessage(), e);
                }
                checkExpl(assignment, reason);
                printAssignments(assignment);
            }
            Set<T> noGood = noGood(value);
            if (noGood != null)
                for (T v : noGood)
                    removeSupport(v.variable(), value);
            setNoGood(value, reason);
            for (T aValue : reason) {
                addSupport(aValue.variable(), value);
            }
            goodnessChanged(value);
        }
    }
}