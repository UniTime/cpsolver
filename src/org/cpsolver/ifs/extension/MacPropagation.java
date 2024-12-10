package org.cpsolver.ifs.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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
 * MAC propagation. <br>
 * <br>
 * During the arc consistency maintenance, when a value is deleted from a
 * variable's domain, the reason (forming an explanation) can be computed and
 * attached to the deleted value. Once a variable (say Vx with the assigned
 * value vx) is unassigned during the search, all deleted values which contain a
 * pair Vx = vx in their explanations need to be recomputed. Such value can be
 * either still inconsistent with the current (partial) solution (a different
 * explanation is attached to it in this case) or it can be returned back to its
 * variable's domain. Arc consistency is maintained after each iteration step,
 * i.e., the selected assignment is propagated into the not yet assigned
 * variables. When a value vx is assigned to a variable Vx, an explanation Vx !=
 * vx' &#8592; Vx = vx is attached to all values vx' of the variable Vx,
 * different from vx. <br>
 * <br>
 * In the case of forward checking (only constraints going from assigned
 * variables to unassigned variables are revised), computing explanations is
 * rather easy. A value vx is deleted from the domain of the variable Vx only if
 * there is a constraint which prohibits the assignment Vx=vx because of the
 * existing assignments (e.g., Vy = vy, Vz = vz). An explanation for the
 * deletion of this value vx is then Vx != vx &#8592; (Vy = vy &amp; ... Vz = vz),
 * where Vy = vy &amp; ... Vz = vz are assignments contained in the prohibiting
 * constraint. In case of arc consistency, a value vx is deleted from the domain
 * of the variable Vx if there is a constraint which does not permit the
 * assignment Vx = vx with other possible assignments of the other variables in
 * the constraint. This means that there is no support value (or combination of
 * values) for the value vx of the variable Vx in the constraint. An explanation
 * is then a union of explanations of all possible support values for the
 * assignment Vx = vx of this constraint which were deleted. The reason is that
 * if one of these support values is returned to its variable's domain, this
 * value vx may be returned as well (i.e., the reason for its deletion has
 * vanished, a new reason needs to be computed). <br>
 * <br>
 * As for the implementation, we only need to enforce arc consistency of the
 * initial solution and to extend unassign and assign methods. Procedure
 * {@link MacPropagation#afterAssigned(Assignment, long, Value)} enforces arc consistency of
 * the solution with the selected assignment variable = value and the procedure
 * {@link MacPropagation#afterUnassigned(Assignment, long, Value)} "undoes" the assignment
 * variable = value. It means that explanations of all values which were deleted
 * and which contain assignment variable = value in their explanations need to
 * be recomputed. This can be done via returning all these values into their
 * variables' domains followed by arc consistency maintenance over their
 * variables. <br>
 * <br>
 * Parameters:
 * <table border='1'><caption>Related Solver Parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>MacPropagation.JustForwardCheck</td>
 * <td>{@link Boolean}</td>
 * <td>If true, only forward checking instead of full arc consistency is
 * maintained during the search.</td>
 * </tr>
 * </table>
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
public class MacPropagation<V extends Variable<V, T>, T extends Value<V, T>> extends ExtensionWithContext<V, T, MacPropagation<V, T>.NoGood> {
    private static org.apache.logging.log4j.Logger sLogger = org.apache.logging.log4j.LogManager.getLogger(MacPropagation.class);
    private boolean iJustForwardCheck = false;
    private Progress iProgress;

    /** List of constraints on which arc-consistency is to be maintained */
    protected List<Constraint<V, T>> iConstraints = null;
    /** Current iteration */
    protected long iIteration = 0;

    /** Constructor 
     * @param solver current solver 
     * @param properties solver configuration
     **/
    public MacPropagation(Solver<V, T> solver, DataProperties properties) {
        super(solver, properties);
        iJustForwardCheck = properties.getPropertyBoolean("MacPropagation.JustForwardCheck", false);
    }

    /** Adds a constraint on which arc-consistency is to be maintained 
     * @param constraint a hard constraint
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
     * @return true if in the set
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
        iIteration = iteration;
        if (value == null)
            return;
        if (!isGood(assignment, value)) {
            while (!isGood(assignment, value) && !noGood(assignment, value).isEmpty()) {
                T noGoodValue = noGood(assignment, value).iterator().next();
                assignment.unassign(iteration, noGoodValue.variable());
            }
        }
        if (!isGood(assignment, value)) {
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
        iIteration = iteration;
        if (!isGood(assignment, value)) {
            sLogger.warn(value.variable().getName() + " = " + value.getName() + " -- not good value assigned (noGood:" + noGood(assignment, value) + ")");
            setGood(assignment, value);
        }

        HashSet<T> noGood = new HashSet<T>(1);
        noGood.add(value);
        for (Iterator<T> i = value.variable().values(assignment).iterator(); i.hasNext();) {
            T anotherValue = i.next();
            if (anotherValue.equals(value))
                continue;
            setNoGood(assignment, anotherValue, noGood);
        }
        getContext(assignment).propagate(assignment, value.variable());
    }

    /**
     * After a value is unassigned: explanations of all values of unassigned
     * variable are recomputed ({@link Value#conflicts(Assignment)}), propagation undo
     * over the unassigned variable takes place.
     */
    @Override
    public void afterUnassigned(Assignment<V, T> assignment, long iteration, T value) {
        iIteration = iteration;
        if (!isGood(assignment, value))
            sLogger.error(value.variable().getName() + " = " + value.getName()
                    + " -- not good value unassigned (noGood:" + noGood(assignment, value) + ")");
        for (Iterator<T> i = value.variable().values(assignment).iterator(); i.hasNext();) {
            T anotherValue = i.next();
            if (!isGood(assignment, anotherValue)) {
                Set<T> noGood = anotherValue.conflicts(assignment);
                if (noGood == null)
                    setGood(assignment, anotherValue);
                else
                    setNoGood(assignment, anotherValue, noGood);
            }
        }
        getContext(assignment).undoPropagate(assignment, value.variable());
    }

    /** good values of a variable (values not removed from variables domain) 
     * @param assignment current assignment
     * @param variable given variable
     * @return set of good values 
     **/
    public Set<T> goodValues(Assignment<V, T> assignment, V variable) {
        return getContext(assignment).goodValues(variable);
    }

    /** notification that a nogood value becomes good or vice versa */
    private void goodnessChanged(Assignment<V, T> assignment, T value) {
        if (isGood(assignment, value)) {
            goodValues(assignment, value.variable()).add(value);
        } else {
            goodValues(assignment, value.variable()).remove(value);
        }
    }

    /** removes support of a variable */
    private void removeSupport(Assignment<V, T> assignment, V variable, T value) {
        getContext(assignment).supportValues(variable).remove(value);
    }

    /** adds support of a variable */
    private void addSupport(Assignment<V, T> assignment, V variable, T value) {
        getContext(assignment).supportValues(variable).add(value);
    }

    /** variables explanation 
     * @param assignment current assignment 
     * @param value given value
     * @return no-good for the value
     **/
    public Set<T> noGood(Assignment<V, T> assignment, T value) {
        return getContext(assignment).getNoGood(value);
    }

    /** is variable good
     * @param assignment current assignment
     * @param value given value
     * @return true if there is no no-good set for the value
     **/
    public boolean isGood(Assignment<V, T> assignment, T value) {
        return getContext(assignment).getNoGood(value) == null;
    }

    /** sets value to be good 
     * @param assignment current assignment
     * @param value given value
     **/
    protected void setGood(Assignment<V, T> assignment, T value) {
        Set<T> noGood = getContext(assignment).getNoGood(value);
        if (noGood != null)
            for (T v : noGood)
                removeSupport(assignment, v.variable(), value);
        getContext(assignment).setNoGood(value, null);
        goodnessChanged(assignment, value);
    }

    /** sets value's explanation 
     * @param assignment current assignment
     * @param value given value
     * @param reason no-good set for the value
     **/
    public void setNoGood(Assignment<V, T> assignment, T value, Set<T> reason) {
        Set<T> noGood = noGood(assignment, value);
        if (noGood != null)
            for (T v : noGood)
                removeSupport(assignment, v.variable(), value);
        getContext(assignment).setNoGood(value, reason);
        for (T aValue : reason)
            addSupport(assignment, aValue.variable(), value);
        goodnessChanged(assignment, value);
    }

    private Set<T> reason(Assignment<V, T> assignment, Constraint<V, T> constraint, V aVariable, T aValue) {
        Set<T> ret = new HashSet<T>();

        for (Iterator<T> i = aVariable.values(assignment).iterator(); i.hasNext();) {
            T value = i.next();
            if (constraint.isConsistent(aValue, value)) {
                if (noGood(assignment, value) == null)
                    sLogger.error("Something went wrong: value " + value + " cannot participate in a reason.");
                else
                    ret.addAll(noGood(assignment, value));
            }
        }
        return ret;
    }
    
    @Override
    public NoGood createAssignmentContext(Assignment<V, T> assignment) {
        return new NoGood(assignment);
    }
    
    /** Propagation over the given variable. 
     * @param assignment current assignment
     * @param variable given variable
     **/
    protected void propagate(Assignment<V, T> assignment, V variable) {
        getContext(assignment).propagate(assignment, variable);
    }
    
    /**
     * Propagation undo over the given variable. All values having given
     * variable in their explanations needs to be recomputed. This is done in
     * two phases: 1) values that contain this variable in explanation are
     * returned back to domains (marked as good) 2) propagation over variables
     * which contains a value that was marked as good takes place
     * @param assignment current assignment
     * @param variable given variable
     */
    public void undoPropagate(Assignment<V, T> assignment, V variable) {
        getContext(assignment).undoPropagate(assignment, variable);
    }

    /**
     * Assignment context
     */
    public class NoGood implements AssignmentContext {
        private Map<V, Set<T>[]> iNoGood = new HashMap<V, Set<T>[]>();
        private Map<V, Map<T, Set<T>>> iNoGoodVal = new HashMap<V, Map<T, Set<T>>>();
        
        /**
         * Initialization. Enforce arc-consistency over the current (initial)
         * solution. AC3 algorithm is used.
         */
        public NoGood(Assignment<V, T> assignment) {
            iProgress = Progress.getInstance(getModel());
            iProgress.save();
            iProgress.setPhase("Initializing propagation:", 3 * getModel().variables().size());
            for (Iterator<V> i = getModel().variables().iterator(); i.hasNext();) {
                V aVariable = i.next();
                supportValues(aVariable).clear();
                goodValues(aVariable).clear();
            }
            for (Iterator<V> i = getModel().variables().iterator(); i.hasNext();) {
                V aVariable = i.next();
                for (Iterator<T> j = aVariable.values(assignment).iterator(); j.hasNext();) {
                    T aValue = j.next();
                    Set<T> noGood = aValue.conflicts(assignment);
                    setNoGood(aValue, noGood);
                    if (noGood == null) {
                        goodValues(aVariable).add(aValue);
                    } else {
                    }
                }
                iProgress.incProgress();
            }
            Queue<V> queue = new LinkedList<V>();
            for (Iterator<V> i = getModel().variables().iterator(); i.hasNext();) {
                V aVariable = i.next();
                for (Constraint<V, T> constraint : aVariable.hardConstraints()) {
                    propagate(assignment, constraint, aVariable, queue);
                }
                iProgress.incProgress();
            }
            if (!iJustForwardCheck)
                propagate(assignment, queue);
            for (Iterator<V> i = getModel().variables().iterator(); i.hasNext();) {
                V aVariable = i.next();
                List<T> values2delete = new ArrayList<T>();
                for (Iterator<T> j = aVariable.values(assignment).iterator(); j.hasNext();) {
                    T aValue = j.next();
                    if (getNoGood(aValue) != null && getNoGood(aValue).isEmpty()) {
                        values2delete.add(aValue);
                    }
                }
                for (T val : values2delete)
                    aVariable.removeValue(0, val);
                if (aVariable.values(assignment).isEmpty()) {
                    sLogger.error(aVariable.getName() + " has empty domain!");
                }
                iProgress.incProgress();
            }
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
         * @return set of good values 
         **/
        @SuppressWarnings("unchecked")
        private Set<T> goodValues(V variable) {
            Set<T>[] ret = getNoGood(variable);
            if (ret == null) {
                ret = new Set[] { new HashSet<T>(1000), new HashSet<T>() };
                setNoGood(variable, ret);
            }
            return ret[1];
        }
        
        /** propagation over a constraint */
        private void propagate(Assignment<V, T> assignment, Constraint<V, T> constraint, V aVariable, Queue<V> queue) {
            if (goodValues(aVariable).isEmpty())
                return;
            List<T> conflicts = conflictValues(assignment, constraint, aVariable);

            if (conflicts != null && !conflicts.isEmpty()) {
                for (T conflictValue : conflicts) {
                    if (!queue.contains(conflictValue.variable()))
                        queue.add(conflictValue.variable());
                    Set<T> reason = reason(assignment, constraint, aVariable, conflictValue);
                    // sLogger.debug("  "+conflictValue+" become nogood (c:"+constraint.getName()+", r:"+reason+")");
                    setNoGood(conflictValue, reason);
                    if (reason.isEmpty())
                        (conflictValue.variable()).removeValue(iIteration, conflictValue);
                }
            }
        }
        
        protected boolean propagate(Assignment<V, T> assignment, V aVariable, V anotherVariable, List<T> adepts) {
            if (goodValues(aVariable).isEmpty())
                return false;
            boolean ret = false;
            List<T> conflicts = null;
            for (Constraint<V, T> constraint : anotherVariable.constraintVariables().get(aVariable)) {
                for (T aValue : goodValues(aVariable)) {
                    if (conflicts == null)
                        conflicts = conflictValues(constraint, aValue, adepts);
                    else
                        conflicts = conflictValues(constraint, aValue, conflicts);
                    if (conflicts == null || conflicts.isEmpty())
                        break;
                }
                if (conflicts != null && !conflicts.isEmpty())
                    for (T conflictValue : conflicts) {
                        Set<T> reason = reason(assignment, constraint, aVariable, conflictValue);
                        // sLogger.debug("  "+conflictValue+" become nogood (c:"+constraint.getName()+", r:"+reason+")");
                        setNoGood(conflictValue, reason);
                        adepts.remove(conflictValue);
                        if (reason.isEmpty())
                            (conflictValue.variable()).removeValue(iIteration, conflictValue);
                        ret = true;
                    }
            }
            return ret;
        }

        protected boolean propagate(Assignment<V, T> assignment, V aVariable, V anotherVariable) {
            if (goodValues(anotherVariable).isEmpty())
                return false;
            return propagate(assignment, aVariable, anotherVariable, new ArrayList<T>(goodValues(anotherVariable)));
        }

        /** Propagation over the given variable. 
         * @param assignment current assignment
         * @param variable given variable
         **/
        protected void propagate(Assignment<V, T> assignment, V variable) {
            Queue<V> queue = new LinkedList<V>();
            if (assignment.getValue(variable) != null) {
                for (Constraint<V, T> constraint : variable.hardConstraints()) {
                    if (contains(constraint))
                        propagate(assignment, constraint, assignment.getValue(variable), queue);
                }
            } else {
                for (Constraint<V, T> constraint : variable.hardConstraints()) {
                    if (contains(constraint))
                        propagate(assignment, constraint, variable, queue);
                }
            }
            if (!iJustForwardCheck && !queue.isEmpty())
                propagate(assignment, queue);
        }

        /** Propagation over the queue of variables.
         * @param assignment current assignment
         * @param queue variable queue
         **/
        protected void propagate(Assignment<V, T> assignment, Queue<V> queue) {
            while (!queue.isEmpty()) {
                V aVariable = queue.poll();
                for (Constraint<V, T> constraint : aVariable.hardConstraints()) {
                    if (contains(constraint))
                        propagate(assignment, constraint, aVariable, queue);
                }
            }
        }
        
        /** propagation over a constraint */
        private void propagate(Assignment<V, T> assignment, Constraint<V, T> constraint, T anAssignedValue, Queue<V> queue) {
            Set<T> reason = new HashSet<T>(1);
            reason.add(anAssignedValue);
            Collection<T> conflicts = conflictValues(assignment, constraint, anAssignedValue);
            if (conflicts != null && !conflicts.isEmpty())
                for (T conflictValue : conflicts) {
                    // sLogger.debug("  "+conflictValue+" become nogood (c:"+constraint.getName()+", r:"+reason+")");
                    setNoGood(conflictValue, reason);
                    if (!queue.contains(conflictValue.variable()))
                        queue.add(conflictValue.variable());
                }
        }
        
        /**
         * Propagation undo over the given variable. All values having given
         * variable in thair explanations needs to be recomputed. This is done in
         * two phases: 1) values that contain this variable in explanation are
         * returned back to domains (marked as good) 2) propagation over variables
         * which contains a value that was marked as good takes place
         * @param assignment current assignment
         * @param variable given variable
         */
        public void undoPropagate(Assignment<V, T> assignment, V variable) {
            Map<V, List<T>> undoVars = new HashMap<V, List<T>>();
            NoGood context = getContext(assignment);
            while (!context.supportValues(variable).isEmpty()) {
                T value = context.supportValues(variable).iterator().next();
                Set<T> noGood = value.conflicts(assignment);
                if (noGood == null) {
                    setGood(assignment, value);
                    List<T> values = undoVars.get(value.variable());
                    if (values == null) {
                        values = new ArrayList<T>();
                        undoVars.put(value.variable(), values);
                    }
                    values.add(value);
                } else {
                    setNoGood(value, noGood);
                    if (noGood.isEmpty())
                        (value.variable()).removeValue(iIteration, value);
                }
            }

            Queue<V> queue = new LinkedList<V>();
            for (V aVariable : undoVars.keySet()) {
                List<T> values = undoVars.get(aVariable);
                boolean add = false;
                for (V x : aVariable.constraintVariables().keySet()) {
                    if (propagate(assignment, x, aVariable, values))
                        add = true;
                }
                if (add)
                    queue.add(aVariable);
            }
            for (V x : variable.constraintVariables().keySet()) {
                if (propagate(assignment, x, variable) && !queue.contains(variable))
                    queue.add(variable);
            }
            if (!iJustForwardCheck)
                propagate(assignment, queue);
        }
        
        private List<T> conflictValues(Assignment<V, T> assignment, Constraint<V, T> constraint, T aValue) {
            List<T> ret = new ArrayList<T>();

            for (V variable : constraint.variables()) {
                if (variable.equals(aValue.variable()))
                    continue;
                if (assignment.getValue(variable) != null)
                    continue;

                for (T value : goodValues(variable))
                    if (!constraint.isConsistent(aValue, value))
                        ret.add(value);
            }
            return ret;
        }

        private List<T> conflictValues(Constraint<V, T> constraint, T aValue, List<T> values) {
            List<T> ret = new ArrayList<T>(values.size());
            for (T value : values)
                if (!constraint.isConsistent(aValue, value))
                    ret.add(value);
            return ret;
        }

        private List<T> conflictValues(Assignment<V, T> assignment, Constraint<V, T> constraint, V aVariable) {
            List<T> conflicts = null;
            for (T aValue : goodValues(aVariable)) {
                if (conflicts == null)
                    conflicts = conflictValues(assignment, constraint, aValue);
                else
                    conflicts = conflictValues(constraint, aValue, conflicts);
                if (conflicts == null || conflicts.isEmpty())
                    return null;
            }
            return conflicts;
        }        
    }

}
