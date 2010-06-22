package net.sf.cpsolver.ifs.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.Progress;

/**
 * MAC propagation. <br>
 * <br>
 * During the arc consistency maintenance, when a value is deleted from a
 * variable’s domain, the reason (forming an explanation) can be computed and
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
 * existing assignments (e.g., Vy = vy, … Vz = vz). An explanation for the
 * deletion of this value vx is then Vx != vx &#8592; (Vy = vy & ... Vz = vz),
 * where Vy = vy & ... Vz = vz are assignments contained in the prohibiting
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
 * {@link MacPropagation#afterAssigned(long, Value)} enforces arc consistency of
 * the solution with the selected assignment variable = value and the procedure
 * {@link MacPropagation#afterUnassigned(long, Value)} "undoes" the assignment
 * variable = value. It means that explanations of all values which were deleted
 * and which contain assignment variable = value in their explanations need to
 * be recomputed. This can be done via returning all these values into their
 * variables’ domains followed by arc consistency maintenance over their
 * variables. <br>
 * <br>
 * Parameters:
 * <table border='1'>
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
public class MacPropagation<V extends Variable<V, T>, T extends Value<V, T>> extends Extension<V, T> {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(MacPropagation.class);
    private boolean iJustForwardCheck = false;
    private Progress iProgress;

    /** List of constraints on which arc-consistency is to be maintained */
    protected List<Constraint<V, T>> iConstraints = null;
    /** Current iteration */
    protected long iIteration = 0;

    /** Constructor */
    public MacPropagation(Solver<V, T> solver, DataProperties properties) {
        super(solver, properties);
        iJustForwardCheck = properties.getPropertyBoolean("MacPropagation.JustForwardCheck", false);
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
        iIteration = iteration;
        if (value == null)
            return;
        if (!isGood(value)) {
            while (!isGood(value) && !noGood(value).isEmpty()) {
                T noGoodValue = noGood(value).iterator().next();
                noGoodValue.variable().unassign(iteration);
            }
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
        iIteration = iteration;
        if (!isGood(value)) {
            sLogger.warn(value.variable().getName() + " = " + value.getName() + " -- not good value assigned (noGood:"
                    + noGood(value) + ")");
            setGood(value);
        }

        HashSet<T> noGood = new HashSet<T>(1);
        noGood.add(value);
        for (Iterator<T> i = value.variable().values().iterator(); i.hasNext();) {
            T anotherValue = i.next();
            if (anotherValue.equals(value))
                continue;
            setNoGood(anotherValue, noGood);
        }
        propagate(value.variable());
    }

    /**
     * After a value is unassigned: explanations of all values of unassigned
     * variable are recomputed ({@link Value#conflicts()}), propagation undo
     * over the unassigned variable takes place.
     */
    @Override
    public void afterUnassigned(long iteration, T value) {
        iIteration = iteration;
        if (!isGood(value))
            sLogger.error(value.variable().getName() + " = " + value.getName()
                    + " -- not good value unassigned (noGood:" + noGood(value) + ")");
        for (Iterator<T> i = value.variable().values().iterator(); i.hasNext();) {
            T anotherValue = i.next();
            if (!isGood(anotherValue)) {
                Set<T> noGood = anotherValue.conflicts();
                if (noGood == null)
                    setGood(anotherValue);
                else
                    setNoGood(anotherValue, noGood);
            }
        }
        undoPropagate(value.variable());
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
        iProgress.setPhase("Initializing propagation:", 3 * getModel().variables().size());
        for (Iterator<V> i = getModel().variables().iterator(); i.hasNext();) {
            V aVariable = i.next();
            supportValues(aVariable).clear();
            goodValues(aVariable).clear();
        }
        for (Iterator<V> i = getModel().variables().iterator(); i.hasNext();) {
            V aVariable = i.next();
            for (Iterator<T> j = aVariable.values().iterator(); j.hasNext();) {
                T aValue = j.next();
                Set<T> noGood = aValue.conflicts();
                initNoGood(aValue, noGood);
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
                propagate(constraint, aVariable, queue);
            }
            iProgress.incProgress();
        }
        if (!iJustForwardCheck)
            propagate(queue);
        for (Iterator<V> i = getModel().variables().iterator(); i.hasNext();) {
            V aVariable = i.next();
            List<T> values2delete = new ArrayList<T>();
            for (Iterator<T> j = aVariable.values().iterator(); j.hasNext();) {
                T aValue = j.next();
                if (!isGood(aValue) && noGood(aValue).isEmpty()) {
                    values2delete.add(aValue);
                }
            }
            for (T val : values2delete)
                aVariable.removeValue(0, val);
            if (aVariable.values().isEmpty()) {
                sLogger.error(aVariable.getName() + " has empty domain!");
                isOk = false;
            }
            iProgress.incProgress();
        }
        iProgress.restore();
        return isOk;
    }

    /** Propagation over the given variable. */
    protected void propagate(V variable) {
        Queue<V> queue = new LinkedList<V>();
        if (variable.getAssignment() != null) {
            for (Constraint<V, T> constraint : variable.hardConstraints()) {
                if (contains(constraint))
                    propagate(constraint, variable.getAssignment(), queue);
            }
        } else {
            for (Constraint<V, T> constraint : variable.hardConstraints()) {
                if (contains(constraint))
                    propagate(constraint, variable, queue);
            }
        }
        if (!iJustForwardCheck && !queue.isEmpty())
            propagate(queue);
    }

    /** Propagation over the queue of variables. */
    protected void propagate(Queue<V> queue) {
        while (!queue.isEmpty()) {
            V aVariable = queue.poll();
            for (Constraint<V, T> constraint : aVariable.hardConstraints()) {
                if (contains(constraint))
                    propagate(constraint, aVariable, queue);
            }
        }
    }

    /**
     * Propagation undo over the given variable. All values having given
     * variable in thair explanations needs to be recomputed. This is done in
     * two phases: 1) values that contain this variable in explanation are
     * returned back to domains (marked as good) 2) propagation over variables
     * which contains a value that was marked as good takes place
     */
    public void undoPropagate(V variable) {
        Map<V, List<T>> undoVars = new Hashtable<V, List<T>>();
        while (!supportValues(variable).isEmpty()) {
            T value = supportValues(variable).iterator().next();
            Set<T> noGood = value.conflicts();
            if (noGood == null) {
                setGood(value);
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
                if (propagate(x, aVariable, values))
                    add = true;
            }
            if (add)
                queue.add(aVariable);
        }
        for (V x : variable.constraintVariables().keySet()) {
            if (propagate(x, variable) && !queue.contains(variable))
                queue.add(variable);
        }
        if (!iJustForwardCheck)
            propagate(queue);
    }

    protected boolean propagate(V aVariable, V anotherVariable, List<T> adepts) {
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
                    Set<T> reason = reason(constraint, aVariable, conflictValue);
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

    protected boolean propagate(V aVariable, V anotherVariable) {
        if (goodValues(anotherVariable).isEmpty())
            return false;
        return propagate(aVariable, anotherVariable, new ArrayList<T>(goodValues(anotherVariable)));
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

    /** sets value's explanation */
    public void setNoGood(T value, Set<T> reason) {
        Set<T> noGood = noGood(value);
        if (noGood != null)
            for (T v : noGood)
                removeSupport(v.variable(), value);
        value.setExtra(reason);
        for (T aValue : reason)
            addSupport(aValue.variable(), value);
        goodnessChanged(value);
    }

    /** propagation over a constraint */
    private void propagate(Constraint<V, T> constraint, T anAssignedValue, Queue<V> queue) {
        Set<T> reason = new HashSet<T>(1);
        reason.add(anAssignedValue);
        Collection<T> conflicts = conflictValues(constraint, anAssignedValue);
        if (conflicts != null && !conflicts.isEmpty())
            for (T conflictValue : conflicts) {
                // sLogger.debug("  "+conflictValue+" become nogood (c:"+constraint.getName()+", r:"+reason+")");
                setNoGood(conflictValue, reason);
                if (!queue.contains(conflictValue.variable()))
                    queue.add(conflictValue.variable());
            }
    }

    /** propagation over a constraint */
    private void propagate(Constraint<V, T> constraint, V aVariable, Queue<V> queue) {
        if (goodValues(aVariable).isEmpty())
            return;
        List<T> conflicts = conflictValues(constraint, aVariable);

        if (conflicts != null && !conflicts.isEmpty()) {
            for (T conflictValue : conflicts) {
                if (!queue.contains(conflictValue.variable()))
                    queue.add(conflictValue.variable());
                Set<T> reason = reason(constraint, aVariable, conflictValue);
                // sLogger.debug("  "+conflictValue+" become nogood (c:"+constraint.getName()+", r:"+reason+")");
                setNoGood(conflictValue, reason);
                if (reason.isEmpty())
                    (conflictValue.variable()).removeValue(iIteration, conflictValue);
            }
        }
    }

    private List<T> conflictValues(Constraint<V, T> constraint, T aValue) {
        List<T> ret = new ArrayList<T>();

        for (V variable : constraint.variables()) {
            if (variable.equals(aValue.variable()))
                continue;
            if (variable.getAssignment() != null)
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

    private List<T> conflictValues(Constraint<V, T> constraint, V aVariable) {
        List<T> conflicts = null;
        for (T aValue : goodValues(aVariable)) {
            if (conflicts == null)
                conflicts = conflictValues(constraint, aValue);
            else
                conflicts = conflictValues(constraint, aValue, conflicts);
            if (conflicts == null || conflicts.isEmpty())
                return null;
        }
        return conflicts;
    }

    private Set<T> reason(Constraint<V, T> constraint, V aVariable, T aValue) {
        Set<T> ret = new HashSet<T>();

        for (Iterator<T> i = aVariable.values().iterator(); i.hasNext();) {
            T value = i.next();
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
