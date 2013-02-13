package net.sf.cpsolver.ifs.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.ifs.util.IdGenerator;

/**
 * Generic constraint. <br>
 * <br>
 * Like in other traditional Constraint Logic Programming (CLP) frameworks, the
 * input problem consists of variables, values and constraints. Each constraint
 * is defined over a subset of the problem variables and it prohibits some
 * combinations of values which these variables can simultaneously take. In
 * usual CSP problems, all constraints are binary (or the problem is transformed
 * into an equivalent problem with only binary constrains before the search is
 * started) since most of the consistency and filtering techniques are designed
 * only for binary constraints. In such a case, the procedure computing
 * conflicting variables is rather simple and it returns an unambiguous set of
 * variables. It enumerates all the constraints which contain the selected
 * variable and which are not consistent with the selected value. It returns all
 * the variables of such constraints, different from the selected variable. <br>
 * <br>
 * On the other hand, most of real problems have plenty of multi-variable
 * constraints, like, for instance, resource constraint in timetabling. Such
 * resource constraint enforces the rule that none of the variables which are
 * using the given resource can be overlapping in time (if the resource has
 * capacity one) or that the amount of the resource used at a time does not
 * exceed its capacity. It is not very useful to replace such resource
 * constraint by a set of binary constraints (e.g., prohibiting two overlapping
 * placements in time of two particular events using the same resource), since
 * this approach usually ends up with thousands of constraints. Also, there is
 * usually a much more effective consistency and/or filtering technique working
 * with the original constraint (for instance, "cumulative" constraint is
 * usually used for modelling resource constraints in CLP). <br>
 * <br>
 * Using multi-variable constraints, the set of conflicting variables returned
 * by the procedure computing conflicting variables can differ according to its
 * implementation. For instance, we can have a constraint A+B=C where A and C is
 * already assigned to A=3 and C=5. Then if the assignment B=3 is selected,
 * either A or B or both A and B can be unassigned to make the problem {A=3,
 * B=3, C=5} consistent with the constraint A+B=C. Intuitively, there should be
 * minimal number of variables unassigned in each iteration step (we are trying
 * to increase the number of the assigned variables during the search). Also,
 * for many constraints, it is possible to find inconsistencies even when not
 * all variables of the constraint are yet assigned. For instance, if there are
 * two lectures using the same room at the same time, we know that one of them
 * needs to be unassigned even when there are unassigned lectures which will
 * also need to be placed in that room. <br>
 * <br>
 * In the current implementation, each hard constraint needs to implement the
 * procedure {@link Constraint#computeConflicts(Value, Set)} which returns all
 * the already assigned values that are incompatible we the selected assignment
 * (value which is to be assigned to its variable). This procedure is called for
 * all constraints which contain the selected variable in an ordered manner.
 * Furthermore, this order can be changed during the search. Moreover, the
 * computed set of conflicting variables is passed to this
 * {@link Constraint#computeConflicts(Value, Set)} procedure as a parameter, so
 * the constraint can "see" what variables are already selected for unassignment
 * by previously processed constraints. This way, we are not computing the very
 * minimal set of conflicting variables, however, we allow for computing this
 * set in an efficient way. It can be also tuned for a particular problem by
 * changing the order of constraints. <br>
 * <br>
 * Also note that each constraint can keep its notion about the assigned
 * variables. For instance, the resource constraint of a particular room can
 * memorize a look-up table stating what lecture is assigned in what time
 * slot(s), so for the computation of the conflicting lectures it only looks
 * through the appropriate fields of this table. The implementation is based on
 * {@link Constraint#assigned(long,Value)} and
 * {@link Constraint#unassigned(long,Value)} methods that are responsible to
 * keeping the problem consistent with the constraint. Also note that this
 * default consistency technique is defined on a problem level and it can be
 * changed by a more dedicated one, implemented for a particular problem.
 * 
 * @see Variable
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

public abstract class Constraint<V extends Variable<V, T>, T extends Value<V, T>> implements
        Comparable<Constraint<V, T>> {
    private static IdGenerator iIdGenerator = new IdGenerator();

    protected long iId = -1;

    private List<V> iVariables = new ArrayList<V>();
    protected Collection<V> iAssignedVariables = new HashSet<V>();
    private Model<V, T> iModel = null;
    protected List<ConstraintListener<T>> iConstraintListeners = null;

    /** Constructor */
    public Constraint() {
        iId = iIdGenerator.newId();
    }

    /** The model which the constraint belongs to */
    public Model<V, T> getModel() {
        return iModel;
    }

    /** Sets the model which the constraint belongs to */
    public void setModel(Model<V, T> model) {
        iModel = model;
    }

    /** The list of variables of this constraint */
    public List<V> variables() {
        return iVariables;
    }

    /** The list of variables of this constraint that are assigned */
    public Collection<V> assignedVariables() {
        return iAssignedVariables;
    }

    /** The number of variables of this constraint */
    public int countVariables() {
        return variables().size();
    }

    /** The number of variables of this constraint that are assigned */
    public int countAssignedVariables() {
        return assignedVariables().size();
    }

    /** Add a variable to this constraint */
    public void addVariable(V variable) {
        iVariables.add(variable);
        variable.addContstraint(this);
        if (variable.getAssignment() != null) {
            assigned(0, variable.getAssignment());
            if (iAssignedVariables != null)
                iAssignedVariables.add(variable);
        }
    }

    /** Remove a variable from this constraint */
    public void removeVariable(V variable) {
        if (variable.getAssignment() != null) {
            unassigned(0, variable.getAssignment());
        }
        variable.removeContstraint(this);
        iVariables.remove(variable);
        if (iAssignedVariables != null && iAssignedVariables.contains(variable))
            iAssignedVariables.remove(variable);
    }

    /**
     * The only method which has to be implemented by any constraint. It returns
     * the values which needs to be unassigned in order to make this constraint
     * consistent with the given value if it is assigned to its variable. The
     * computed list of conflicting values is added to the given set of
     * conflicts.
     * 
     * @param value
     *            value to be assigned to its varaible
     * @param conflicts
     *            resultant set of conflicting values
     */
    public abstract void computeConflicts(T value, Set<T> conflicts);

    /**
     * Returns true if the given assignments are consistent respecting this
     * constraint. This method is used by MAC (see
     * {@link net.sf.cpsolver.ifs.extension.MacPropagation}).
     */
    public boolean isConsistent(T value1, T value2) {
        return true;
    }

    /**
     * Returns true if the given assignment is inconsistent with the existing
     * assignments respecting this constraint. This method is used by MAC (see
     * {@link net.sf.cpsolver.ifs.extension.MacPropagation}).
     */
    public boolean inConflict(T value) {
        Set<T> conflicts = new HashSet<T>();
        computeConflicts(value, conflicts);
        return !conflicts.isEmpty();
    }

    /**
     * Given value is to be assigned to its varable. In this method, the
     * constraint should unassigns all varaibles which are in conflict with the
     * given assignment because of this constraint.
     */
    public void assigned(long iteration, T value) {
        Set<T> conf = null;
        if (isHard()) {
            conf = new HashSet<T>();
            computeConflicts(value, conf);
        }
        if (iConstraintListeners != null)
            for (ConstraintListener<T> listener : iConstraintListeners)
                listener.constraintBeforeAssigned(iteration, this, value, conf);
        if (conf != null) {
            for (T conflictValue : conf) {
                conflictValue.variable().unassign(iteration);
            }
        }
        if (iAssignedVariables != null)
            iAssignedVariables.add(value.variable());
        if (iConstraintListeners != null)
            for (ConstraintListener<T> listener : iConstraintListeners)
                listener.constraintAfterAssigned(iteration, this, value, conf);
    }

    /**
     * Given value is unassigned from its variable.
     */
    public void unassigned(long iteration, T value) {
        if (iAssignedVariables != null)
            iAssignedVariables.remove(value.variable());
    }

    /** Adds a constraint listener */
    public void addConstraintListener(ConstraintListener<T> listener) {
        if (iConstraintListeners == null)
            iConstraintListeners = new ArrayList<ConstraintListener<T>>();
        iConstraintListeners.add(listener);
    }

    /** Removes a constraint listener */
    public void removeConstraintListener(ConstraintListener<T> listener) {
        if (iConstraintListeners != null)
            iConstraintListeners.remove(listener);
    }

    /** Returns the list of registered constraint listeners */
    public List<ConstraintListener<T>> constraintListeners() {
        return iConstraintListeners;
    }

    /** Unique id */
    public long getId() {
        return iId;
    }

    /** Constraint's name -- for printing purposes */
    public String getName() {
        return String.valueOf(iId);
    }

    /** Constraint's description -- for printing purposes */
    public String getDescription() {
        return null;
    }

    @Override
    public int hashCode() {
        return (int) iId;
    }

    /**
     * Returns true if the constraint is hard. Only hard constraints are allowed
     * to unassign a variable when there is a conflict with a value that is
     * being assigned
     */
    public boolean isHard() {
        return true;
    }

    /**
     * Compare two constraints for equality ({@link Constraint#getId()} is used)
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Constraint<?, ?>))
            return false;
        return getId() == ((Constraint<?, ?>) o).getId();
    }

    @Override
    public int compareTo(Constraint<V, T> c) {
        return Double.compare(getId(), c.getId());
    }
}
