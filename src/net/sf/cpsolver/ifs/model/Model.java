package net.sf.cpsolver.ifs.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sf.cpsolver.ifs.assignment.Assignment;
import net.sf.cpsolver.ifs.assignment.DefaultSingleAssignment;
import net.sf.cpsolver.ifs.assignment.EmptyAssignment;
import net.sf.cpsolver.ifs.assignment.context.AssignmentContext;
import net.sf.cpsolver.ifs.assignment.context.AssignmentContextReference;
import net.sf.cpsolver.ifs.assignment.context.HasAssignmentContext;
import net.sf.cpsolver.ifs.criteria.Criterion;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Generic model (definition of a problem). <br>
 * <br>
 * It consists of variables and constraints. It has also capability of
 * memorizing the current and the best ever found assignment. <br>
 * <br>
 * Example usage:<br>
 * <ul>
 * <code>
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
 * </code>
 * </ul>
 * 
 * @see Variable
 * @see Constraint
 * @see net.sf.cpsolver.ifs.solution.Solution
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

public class Model<V extends Variable<V, T>, T extends Value<V, T>> {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(Model.class);
    protected static java.text.DecimalFormat sTimeFormat = new java.text.DecimalFormat("0.00",
            new java.text.DecimalFormatSymbols(Locale.US));
    protected static java.text.DecimalFormat sDoubleFormat = new java.text.DecimalFormat("0.00",
            new java.text.DecimalFormatSymbols(Locale.US));
    protected static java.text.DecimalFormat sPercentageFormat = new java.text.DecimalFormat("0.00",
            new java.text.DecimalFormatSymbols(Locale.US));

    private List<V> iVariables = new ArrayList<V>();
    private List<Constraint<V, T>> iConstraints = new ArrayList<Constraint<V, T>>();
    private List<GlobalConstraint<V, T>> iGlobalConstraints = new ArrayList<GlobalConstraint<V, T>>();
    private Collection<V> iVariablesWithInitialValueCache = null;

    private List<ModelListener<V, T>> iModelListeners = new ArrayList<ModelListener<V, T>>();
    private List<InfoProvider<V, T>> iInfoProviders = new ArrayList<InfoProvider<V, T>>();
    private HashMap<String, Criterion<V, T>> iCriteria = new HashMap<String, Criterion<V,T>>();

    private int iBestUnassignedVariables = -1;
    private int iBestPerturbations = 0;
    private double iBestValue = 0.0;
    private int iNextReferenceId = 0;
    private int iNextVariableIndex = 0;
    @Deprecated
    private Assignment<V, T> iAssignment = null;
    private Assignment<V, T> iEmptyAssignment = null;
    private Map<Integer, AssignmentContextReference<V, T, ? extends AssignmentContext>> iAssignmentContextReferences = new HashMap<Integer, AssignmentContextReference<V, T, ? extends AssignmentContext>>();

    /** Constructor */
    public Model() {
    }

    /** The list of variables in the model */
    public List<V> variables() {
        return iVariables;
    }

    /** The number of variables in the model */
    public int countVariables() {
        return iVariables.size();
    }

    /** Adds a variable to the model */
    @SuppressWarnings("unchecked")
    public void addVariable(V variable) {
        variable.setModel(this);
        variable.setIndex(iNextVariableIndex++);
        iVariables.add(variable);
        if (variable instanceof InfoProvider<?, ?>)
            iInfoProviders.add((InfoProvider<V, T>) variable);
        for (ModelListener<V, T> listener : iModelListeners)
            listener.variableAdded(variable);
        invalidateVariablesWithInitialValueCache();
    }

    /** Removes a variable from the model */
    public void removeVariable(V variable) {
        variable.setModel(null);
        iVariables.remove(variable);
        if (variable instanceof InfoProvider<?, ?>)
            iInfoProviders.remove(variable);
        for (ModelListener<V, T> listener : iModelListeners)
            listener.variableRemoved(variable);
        invalidateVariablesWithInitialValueCache();
    }

    /** The list of constraints in the model */
    public List<Constraint<V, T>> constraints() {
        return iConstraints;
    }

    /** The number of constraints in the model */
    public int countConstraints() {
        return iConstraints.size();
    }

    /** Adds a constraint to the model */
    @SuppressWarnings("unchecked")
    public void addConstraint(Constraint<V, T> constraint) {
        constraint.setModel(this);
        iConstraints.add(constraint);
        if (constraint instanceof InfoProvider<?, ?>)
            iInfoProviders.add((InfoProvider<V, T>) constraint);
        for (ModelListener<V, T> listener : iModelListeners)
            listener.constraintAdded(constraint);
    }

    /** Removes a constraint from the model */
    public void removeConstraint(Constraint<V, T> constraint) {
        constraint.setModel(null);
        iConstraints.remove(constraint);
        if (constraint instanceof InfoProvider<?, ?>)
            iInfoProviders.remove(constraint);
        for (ModelListener<V, T> listener : iModelListeners)
            listener.constraintRemoved(constraint);
    }

    /** The list of global constraints in the model */
    public List<GlobalConstraint<V, T>> globalConstraints() {
        return iGlobalConstraints;
    }

    /** The number of global constraints in the model */
    public int countGlobalConstraints() {
        return iGlobalConstraints.size();
    }

    /** Adds a global constraint to the model */
    @SuppressWarnings("unchecked")
    public void addGlobalConstraint(GlobalConstraint<V, T> constraint) {
        constraint.setModel(this);
        iGlobalConstraints.add(constraint);
        if (constraint instanceof InfoProvider<?, ?>)
            iInfoProviders.add((InfoProvider<V, T>) constraint);
        for (ModelListener<V, T> listener : iModelListeners)
            listener.constraintAdded(constraint);
    }

    /** Removes a global constraint from the model */
    public void removeGlobalConstraint(GlobalConstraint<V, T> constraint) {
        constraint.setModel(null);
        iGlobalConstraints.remove(constraint);
        if (constraint instanceof InfoProvider<?, ?>)
            iInfoProviders.remove(constraint);
        for (ModelListener<V, T> listener : iModelListeners)
            listener.constraintRemoved(constraint);
    }

    /**
     * The list of unassigned variables in the model.
     * Use {@link Model#unassignedVariables(Assignment)} or {@link Assignment#unassignedVariables(Model)} instead.
     **/
    @Deprecated
    public Collection<V> unassignedVariables() {
        return unassignedVariables(getDefaultAssignment());
    }

    /** The list of unassigned variables in the model */
    public Collection<V> unassignedVariables(Assignment<V, T> assignment) {
        return assignment.unassignedVariables(this);
    }

    /**
     * Number of unassigned variables.
     * Use {@link Model#nrUnassignedVariables(Assignment)} or {@link Assignment#nrUnassignedVariables(Model)} instead.
     **/
    @Deprecated
    public int nrUnassignedVariables() {
        return nrUnassignedVariables(getDefaultAssignment());
    }

    /** Number of unassigned variables */
    public int nrUnassignedVariables(Assignment<V, T> assignment) {
        return assignment.nrUnassignedVariables(this);
    }

    /**
     * The list of assigned variables in the model.
     * Use {@link Model#assignedVariables(Assignment)} or {@link Assignment#assignedVariables()} instead.
     **/
    @Deprecated
    public Collection<V> assignedVariables() {
        return assignedVariables(getDefaultAssignment());
    }
    
    /** The list of assigned variables in the model */
    public Collection<V> assignedVariables(Assignment<V, T> assignment) {
        return assignment.assignedVariables();
    }

    /**
     * Number of assigned variables.
     * Use {@link Model#nrAssignedVariables(Assignment)} or {@link Assignment#nrAssignedVariables()} instead.
     **/
    @Deprecated
    public int nrAssignedVariables() {
        return nrAssignedVariables(getDefaultAssignment());
    }
    
    /** Number of assigned variables */
    public int nrAssignedVariables(Assignment<V, T> assignment) {
        return assignment.nrAssignedVariables();
    }

    /**
     * The list of perturbation variables in the model, i.e., the variables
     * which has an initial value but which are not assigned with this value.
     * Use {@link Model#perturbVariables(Assignment)} instead.
     */
    @Deprecated
    public Collection<V> perturbVariables() {
        return perturbVariables(getDefaultAssignment(), variablesWithInitialValue());
    }
    
    /**
     * The list of perturbation variables in the model, i.e., the variables
     * which has an initial value but which are not assigned with this value.
     */
    public Collection<V> perturbVariables(Assignment<V, T> assignment) {
        return perturbVariables(assignment, variablesWithInitialValue());
    }
    
    /**
     * The list of perturbation variables in the model, i.e., the variables
     * which has an initial value but which are not assigned with this value.
     * Only variables from the given set are considered.
     * Use {@link Model#perturbVariables(Assignment, Collection)} instead.
     */
    @Deprecated
    public List<V> perturbVariables(Collection<V> variables) {
        return perturbVariables(getDefaultAssignment(), variables);
    }

    /**
     * The list of perturbation variables in the model, i.e., the variables
     * which has an initial value but which are not assigned with this value.
     * Only variables from the given set are considered.
     */
    public List<V> perturbVariables(Assignment<V, T> assignment, Collection<V> variables) {
        List<V> perturbances = new ArrayList<V>();
        for (V variable : variables) {
            if (variable.getInitialAssignment() == null)
                continue;
            T value = assignment.getValue(variable);
            if (value != null) {
                if (!variable.getInitialAssignment().equals(value))
                    perturbances.add(variable);
            } else {
                boolean hasPerturbance = false;
                for (Constraint<V, T> constraint : variable.hardConstraints()) {
                    if (constraint.inConflict(assignment, variable.getInitialAssignment())) {
                        hasPerturbance = true;
                        break;
                    }
                }
                if (!hasPerturbance)
                    for (GlobalConstraint<V, T> constraint : globalConstraints()) {
                        if (constraint.inConflict(assignment, variable.getInitialAssignment())) {
                            hasPerturbance = true;
                            break;
                        }
                    }
                if (hasPerturbance)
                    perturbances.add(variable);
            }
        }
        return perturbances;
    }

    /**
     * Returns the set of conflicting variables with this value, if it is
     * assigned to its variable
     * Use {@link Model#conflictValues(Assignment, Value)} instead.
     */
    @Deprecated
    public Set<T> conflictValues(T value) {
        return conflictValues(getDefaultAssignment(), value);
    }
    
    /**
     * Returns the set of conflicting variables with this value, if it is
     * assigned to its variable
     */
    public Set<T> conflictValues(Assignment<V, T> assignment, T value) {
        Set<T> conflictValues = new HashSet<T>();
        for (Constraint<V, T> constraint : value.variable().hardConstraints())
            constraint.computeConflicts(assignment, value, conflictValues);
        for (GlobalConstraint<V, T> constraint : globalConstraints())
            constraint.computeConflicts(assignment, value, conflictValues);
        return conflictValues;
    }

    /**
     * Return true if the given value is in conflict with a hard constraint
     * Use {@link Model#inConflict(Assignment, Value)} instead.
     **/
    @Deprecated
    public boolean inConflict(T value) {
        return inConflict(getDefaultAssignment(), value);
    }

    /** Return true if the given value is in conflict with a hard constraint */
    public boolean inConflict(Assignment<V, T> assignment, T value) {
        for (Constraint<V, T> constraint : value.variable().hardConstraints())
            if (constraint.inConflict(assignment, value))
                return true;
        for (GlobalConstraint<V, T> constraint : globalConstraints())
            if (constraint.inConflict(assignment, value))
                return true;
        return false;
    }

    /** The list of variables without initial value */
    public Collection<V> variablesWithInitialValue() {
        if (iVariablesWithInitialValueCache != null)
            return iVariablesWithInitialValueCache;
        iVariablesWithInitialValueCache = new ArrayList<V>();
        for (V variable : iVariables) {
            if (variable.getInitialAssignment() != null)
                iVariablesWithInitialValueCache.add(variable);
        }
        return iVariablesWithInitialValueCache;
    }

    /** Invalidates cache containing all variables that possess an initial value */
    protected void invalidateVariablesWithInitialValueCache() {
        iVariablesWithInitialValueCache = null;
    }
    
    /** Called before a value is assigned to its variable */
    @Deprecated
    public void beforeAssigned(long iteration, T value) {
    }

    /** Called before a value is assigned to its variable */
    @SuppressWarnings("deprecation")
    public void beforeAssigned(Assignment<V, T> assignment, long iteration, T value) {
        beforeAssigned(iteration, value);
        for (ModelListener<V, T> listener : iModelListeners)
            listener.beforeAssigned(assignment, iteration, value);
    }

    /** Called before a value is unassigned from its variable */
    @Deprecated
    public void beforeUnassigned(long iteration, T value) {
    }

    /** Called before a value is unassigned from its variable */
    @SuppressWarnings("deprecation")
    public void beforeUnassigned(Assignment<V, T> assignment, long iteration, T value) {
        beforeUnassigned(iteration, value);
        for (ModelListener<V, T> listener : iModelListeners)
            listener.beforeUnassigned(assignment, iteration, value);
    }

    /** Called after a value is assigned to its variable */
    @Deprecated
    public void afterAssigned(long iteration, T value) {
    }

    /** Called after a value is assigned to its variable */
    @SuppressWarnings("deprecation")
    public void afterAssigned(Assignment<V, T> assignment,  long iteration, T value) {
        afterAssigned(iteration, value);
        for (ModelListener<V, T> listener : iModelListeners)
            listener.afterAssigned(assignment, iteration, value);
    }
    
    /** Called after a value is unassigned from its variable */
    @Deprecated
    public void afterUnassigned(long iteration, T value) {
    }

    /** Called after a value is unassigned from its variable */
    @SuppressWarnings("deprecation")
    public void afterUnassigned(Assignment<V, T> assignment, long iteration, T value) {
        afterUnassigned(iteration, value);
        for (ModelListener<V, T> listener : iModelListeners)
            listener.afterUnassigned(assignment, iteration, value);
    }

    @Override
    public String toString() {
        return "Model{\n    variables=" + ToolBox.col2string(variables(), 2) + ",\n    constraints=" + ToolBox.col2string(constraints(), 2) + ",\n  }";
    }

    protected String getPerc(double value, double min, double max) {
        if (max == min)
            return sPercentageFormat.format(100.0);
        return sPercentageFormat.format(100.0 - 100.0 * (value - min) / (max - min));
    }

    protected String getPercRev(double value, double min, double max) {
        if (max == min)
            return sPercentageFormat.format(0.0);
        return sPercentageFormat.format(100.0 * (value - min) / (max - min));
    }
    
    /**
     * Returns information about the current solution. Information from all
     * model listeners and constraints is also included.
     * Use {@link Model#getInfo(Assignment)} instead.
     */
    @Deprecated
    public Map<String, String> getInfo() {
        return getInfo(getDefaultAssignment());
    }

    /**
     * Returns information about the current solution. Information from all
     * model listeners and constraints is also included.
     */
    public Map<String, String> getInfo(Assignment<V, T> assignment) {
        Map<String, String> ret = new HashMap<String, String>();
        ret.put("Assigned variables", getPercRev(assignment.nrAssignedVariables(), 0, variables().size()) + "% (" + assignment.nrAssignedVariables() + "/" + variables().size() + ")");
        int nrVarsWithInitialValue = variablesWithInitialValue().size();
        if (nrVarsWithInitialValue > 0) {
            Collection<V> pv = perturbVariables(assignment);
            ret.put("Perturbation variables", getPercRev(pv.size(), 0, nrVarsWithInitialValue) + "% (" + pv.size() + " + " + (variables().size() - nrVarsWithInitialValue) + ")");
        }
        ret.put("Overall solution value", sDoubleFormat.format(getTotalValue(assignment)));
        for (InfoProvider<V, T> provider : iInfoProviders)
            provider.getInfo(assignment, ret);
        return ret;
    }
    
    /**
     * Extended information about current solution. Similar to
     * {@link Model#getInfo(Assignment)}, but some more information (that is more
     * expensive to compute) might be added.
     * Use {@link Model#getExtendedInfo(Assignment)} instead.
     */
    @Deprecated
    public Map<String, String> getExtendedInfo() {
        return getExtendedInfo(getDefaultAssignment());
    }

    /**
     * Extended information about current solution. Similar to
     * {@link Model#getInfo(Assignment)}, but some more information (that is more
     * expensive to compute) might be added.
     */
    public Map<String, String> getExtendedInfo(Assignment<V, T> assignment) {
        Map<String, String> ret = getInfo(assignment);
        for (InfoProvider<V, T> provider : iInfoProviders)
            if (provider instanceof ExtendedInfoProvider)
                ((ExtendedInfoProvider<V, T>)provider).getExtendedInfo(assignment, ret);
        return ret;
    }
    
    /**
     * Returns information about the current solution. Information from all
     * model listeners and constraints is also included. Only variables from the
     * given set are considered.
     * Use {@link Model#getInfo(Assignment, Collection)} instead.
     **/
    @Deprecated
    public Map<String, String> getInfo(Collection<V> variables) {
        return getInfo(getDefaultAssignment(), variables);
    }

    /**
     * Returns information about the current solution. Information from all
     * model listeners and constraints is also included. Only variables from the
     * given set are considered.
     */
    public Map<String, String> getInfo(Assignment<V, T> assignment, Collection<V> variables) {
        Map<String, String> ret = new HashMap<String, String>();
        int assigned = 0, perturb = 0, nrVarsWithInitialValue = 0;
        for (V variable : variables) {
            T value = assignment.getValue(variable);
            if (value != null)
                assigned++;
            if (variable.getInitialAssignment() != null) {
                nrVarsWithInitialValue++;
                if (value != null) {
                    if (!variable.getInitialAssignment().equals(value))
                        perturb++;
                } else {
                    boolean hasPerturbance = false;
                    for (Constraint<V, T> constraint : variable.hardConstraints()) {
                        if (constraint.inConflict(assignment, variable.getInitialAssignment())) {
                            hasPerturbance = true;
                            break;
                        }
                    }
                    if (!hasPerturbance)
                        for (GlobalConstraint<V, T> constraint : globalConstraints()) {
                            if (constraint.inConflict(assignment, variable.getInitialAssignment())) {
                                hasPerturbance = true;
                                break;
                            }
                        }
                    if (hasPerturbance)
                        perturb++;
                }
            }
        }
        ret.put("Assigned variables", getPercRev(assigned, 0, variables.size()) + "% (" + assigned + "/" + variables.size() + ")");
        if (nrVarsWithInitialValue > 0) {
            ret.put("Perturbation variables", getPercRev(perturb, 0, nrVarsWithInitialValue) + "% (" + perturb + " + " + (variables.size() - nrVarsWithInitialValue) + ")");
        }
        ret.put("Overall solution value", sDoubleFormat.format(getTotalValue(assignment, variables)));
        for (InfoProvider<V, T> provider : iInfoProviders)
            provider.getInfo(assignment, ret, variables);
        return ret;
    }

    /**
     * Returns the number of unassigned variables in the best ever found
     * solution
     */
    public int getBestUnassignedVariables() {
        return iBestUnassignedVariables;
    }

    /**
     * Returns the number of perturbation variables in the best ever found
     * solution
     */
    public int getBestPerturbations() {
        return iBestPerturbations;
    }
    
    /**
     * Total value of the best ever found solution -- sum of all assigned values
     * (see {@link Value#toDouble()}).
     */
    public double getBestValue() {
        return iBestValue;
    }

    /** Set total value of the best ever found solution */
    public void setBestValue(double bestValue) {
        iBestValue = bestValue;
    }
    
    /**
     * Save the current assignment as the best ever found assignment
     * Use {@link Model#saveBest(Assignment)} instead.
     **/
    @Deprecated
    public void saveBest() {
        saveBest(getDefaultAssignment());
    }

    /** Save the current assignment as the best ever found assignment */
    public void saveBest(Assignment<V, T> assignment) {
        iBestUnassignedVariables = iVariables.size() - assignment.nrAssignedVariables();
        iBestPerturbations = perturbVariables(assignment).size();
        iBestValue = getTotalValue(assignment);
        for (V variable : iVariables) {
            variable.setBestAssignment(assignment.getValue(variable), assignment.getIteration(variable));
        }
        for (Criterion<V, T> criterion: getCriteria()) {
            criterion.bestSaved(assignment);
        }
    }

    /** Clear the best ever found assignment */
    public void clearBest() {
        iBestUnassignedVariables = -1;
        iBestPerturbations = 0;
        iBestValue = 0;
        for (V variable : iVariables) {
            variable.setBestAssignment(null, 0);
        }
    }

    /**
     * Restore the best ever found assignment into the current assignment
     * Use {@link Model#restoreBest(Assignment)} instead.
     **/
    @Deprecated
    protected void restoreBest() {
        restoreBest(getDefaultAssignment());
    }

    /** Restore the best ever found assignment into the current assignment */
    @SuppressWarnings("unchecked")
    protected void restoreBest(Assignment<V, T> assignment, Comparator<V> assignmentOrder) {
        TreeSet<V> sortedVariables = new TreeSet<V>(assignmentOrder);
        for (V variable : iVariables) {
            T value = assignment.getValue(variable);
            if (value == null) {
                if (variable.getBestAssignment() != null)
                    sortedVariables.add(variable);
            } else if (!value.equals(variable.getBestAssignment())) {
                assignment.unassign(0, variable);
                if (variable.getBestAssignment() != null)
                    sortedVariables.add(variable);
            }
        }
        Set<T> problems = new HashSet<T>();
        for (V variable : sortedVariables) {
            Set<T> confs = conflictValues(assignment, variable.getBestAssignment());
            if (!confs.isEmpty()) {
                sLogger.error("restore best problem: assignment " + variable.getName() + " = " + variable.getBestAssignment().getName());
                for (Constraint<V, T> c : variable.hardConstraints()) {
                    Set<T> x = new HashSet<T>();
                    c.computeConflicts(assignment, variable.getBestAssignment(), x);
                    if (!x.isEmpty()) {
                        if (c instanceof WeakeningConstraint) {
                            ((WeakeningConstraint<V, T>)c).weaken(assignment, variable.getBestAssignment());
                            sLogger.info("  constraint " + c.getClass().getSimpleName() + " " + c.getName() + " had to be weakened");
                        } else {
                            sLogger.error("  constraint " + c.getClass().getSimpleName() + " " + c.getName() + " causes the following conflicts " + x);
                        }
                    }
                }
                for (GlobalConstraint<V, T> c : globalConstraints()) {
                    Set<T> x = new HashSet<T>();
                    c.computeConflicts(assignment, variable.getBestAssignment(), x);
                    if (!x.isEmpty()) {
                        if (c instanceof WeakeningConstraint) {
                            ((WeakeningConstraint<V, T>)c).weaken(assignment, variable.getBestAssignment());
                            sLogger.info("  constraint " + c.getClass().getSimpleName() + " " + c.getName() + " had to be weakened");
                        } else {
                            sLogger.error("  global constraint " + c.getClass().getSimpleName() + " " + c.getName() + " causes the following conflicts " + x);
                        }
                    }
                }
                problems.add(variable.getBestAssignment());
            } else
                assignment.assign(0, variable.getBestAssignment());
        }
        int attempt = 0, maxAttempts = 3 * problems.size();
        while (!problems.isEmpty() && attempt <= maxAttempts) {
            attempt++;
            T value = ToolBox.random(problems);
            problems.remove(value);
            V variable = value.variable();
            Set<T> confs = conflictValues(assignment, value);
            if (!confs.isEmpty()) {
                sLogger.error("restore best problem (again, att=" + attempt + "): assignment " + variable.getName() + " = " + value.getName());
                for (Constraint<V, T> c : variable.hardConstraints()) {
                    Set<T> x = new HashSet<T>();
                    c.computeConflicts(assignment, value, x);
                    if (!x.isEmpty())
                        sLogger.error("  constraint " + c.getClass().getSimpleName() + " " + c.getName() + " causes the following conflicts " + x);
                }
                for (GlobalConstraint<V, T> c : globalConstraints()) {
                    Set<T> x = new HashSet<T>();
                    c.computeConflicts(assignment, value, x);
                    if (!x.isEmpty())
                        sLogger.error("  constraint " + c.getClass().getSimpleName() + " " + c.getName() + " causes the following conflicts " + x);
                }
                for (T conf : confs)
                    assignment.unassign(0, conf.variable());
                problems.addAll(confs);
            }
            assignment.assign(0, value);
        }
        for (Criterion<V, T> criterion: getCriteria()) {
            criterion.bestRestored(assignment);
        }
    }
    
    /** Restore the best ever found assignment into the current assignment */
    public void restoreBest(Assignment<V, T> assignment) {
        restoreBest(assignment, new Comparator<V>() {
            @Override
            public int compare(V v1, V v2) {
                if (v1.getBestAssignmentIteration() < v2.getBestAssignmentIteration()) return -1;
                if (v1.getBestAssignmentIteration() > v2.getBestAssignmentIteration()) return 1;
                return v1.compareTo(v2);
            }
        });
    }
    
    /**
     * The list of unassigned variables in the best ever found solution.
     * Use {@link Model#bestUnassignedVariables(Assignment)} instead.
     **/
    @Deprecated
    public Collection<V> bestUnassignedVariables() {
        return bestUnassignedVariables(getDefaultAssignment());
    }
    
    /** The list of unassigned variables in the best ever found solution */
    public Collection<V> bestUnassignedVariables(Assignment<V, T> assignment) {
        Collection<V> ret = new ArrayList<V>(variables().size());
        if (iBestUnassignedVariables < 0) {
            for (V variable : variables()) {
                if (assignment.getValue(variable) == null)
                    ret.add(variable);
            }
        } else {
            for (V variable : variables()) {
                if (variable.getBestAssignment() == null)
                    ret.add(variable);
            }
        }
        return ret;
    }

    /**
     * Value of the current solution. It is the sum of all assigned values,
     * i.e., {@link Value#toDouble(Assignment)}.
     * Use {@link Model#getTotalValue(Assignment)} instead.
     */
    @Deprecated
    public double getTotalValue() {
        return getTotalValue(getDefaultAssignment());
    }
    
    /**
     * Value of the current solution. It is the sum of all assigned values,
     * i.e., {@link Value#toDouble()}.
     */
    public double getTotalValue(Assignment<V, T> assignment) {
        double ret = 0.0;
        for (T t: assignment.assignedValues())
            ret += t.toDouble(assignment);
        return ret;
    }

    /**
     * Value of the current solution. It is the sum of all assigned values,
     * i.e., {@link Value#toDouble(Assignment)}. Only variables from the given set are
     * considered.
     * Use {@link Model#getTotalValue(Assignment, Collection)} instead.
     **/
    @Deprecated
    public double getTotalValue(Collection<V> variables) {
        return getTotalValue(getDefaultAssignment(), variables);
    }
    
    /**
     * Value of the current solution. It is the sum of all assigned values,
     * i.e., {@link Value#toDouble(Assignment)}. Only variables from the given set are
     * considered.
     **/
    public double getTotalValue(Assignment<V, T> assignment, Collection<V> variables) {
        double ret = 0.0;
        for (V v: variables) {
            T t = assignment.getValue(v);
            if (t != null)
                ret += t.toDouble(assignment);
        }
        return ret;
    }

    /** Adds a model listener */
    @SuppressWarnings("unchecked")
    public void addModelListener(ModelListener<V, T> listener) {
        iModelListeners.add(listener);
        if (listener instanceof InfoProvider<?, ?>)
            iInfoProviders.add((InfoProvider<V, T>) listener);
        for (Constraint<V, T> constraint : iConstraints)
            listener.constraintAdded(constraint);
        for (V variable : iVariables)
            listener.variableAdded(variable);
    }

    /** Removes a model listener */
    public void removeModelListener(ModelListener<V, T> listener) {
        if (listener instanceof InfoProvider<?, ?>)
            iInfoProviders.remove(listener);
        for (V variable : iVariables)
            listener.variableRemoved(variable);
        for (Constraint<V, T> constraint : iConstraints)
            listener.constraintRemoved(constraint);
        iModelListeners.remove(listener);
    }

    /** Model initialization */
    public boolean init(Solver<V, T> solver) {
        for (ModelListener<V, T> listener : new ArrayList<ModelListener<V, T>>(iModelListeners)) {
            if (!listener.init(solver))
                return false;
        }
        return true;
    }

    /** The list of model listeners */
    public List<ModelListener<V, T>> getModelListeners() {
        return iModelListeners;
    }

    /** The list of model listeners that are of the given class */
    public ModelListener<V, T> modelListenerOfType(Class<ModelListener<V, T>> type) {
        for (ModelListener<V, T> listener : iModelListeners) {
            if (listener.getClass() == type)
                return listener;
        }
        return null;
    }

    /**
     * The list of constraints which are in a conflict with the given value if
     * it is assigned to its variable. This means the constraints, which adds a
     * value into the set of conflicting values in
     * {@link Constraint#computeConflicts(Assignment, Value, Set)}.
     */
    public Map<Constraint<V, T>, Set<T>> conflictConstraints(Assignment<V, T> assignment, T value) {
        Map<Constraint<V, T>, Set<T>> conflictConstraints = new HashMap<Constraint<V, T>, Set<T>>();
        for (Constraint<V, T> constraint : value.variable().hardConstraints()) {
            Set<T> conflicts = new HashSet<T>();
            constraint.computeConflicts(assignment, value, conflicts);
            if (!conflicts.isEmpty()) {
                conflictConstraints.put(constraint, conflicts);
            }
        }
        for (GlobalConstraint<V, T> constraint : globalConstraints()) {
            Set<T> conflicts = new HashSet<T>();
            constraint.computeConflicts(assignment, value, conflicts);
            if (!conflicts.isEmpty()) {
                conflictConstraints.put(constraint, conflicts);
            }
        }
        return conflictConstraints;
    }

    /**
     * The list of hard constraints which contain at least one variable that is
     * not assigned.
     */
    public List<Constraint<V, T>> unassignedHardConstraints(Assignment<V, T> assignment) {
        List<Constraint<V, T>> ret = new ArrayList<Constraint<V, T>>();
        constraints: for (Constraint<V, T> constraint : constraints()) {
            if (!constraint.isHard())
                continue;
            for (V v : constraint.variables()) {
                if (assignment.getValue(v) == null) {
                    ret.add(constraint);
                    continue constraints;
                }
            }
        }
        if (iVariables.size() > assignment.nrAssignedVariables())
            ret.addAll(globalConstraints());
        return ret;
    }

    /** Registered info providers (see {@link InfoProvider}) */
    protected List<InfoProvider<V, T>> getInfoProviders() {
        return iInfoProviders;
    }
    
    /** Register a new criterion */
    public void addCriterion(Criterion<V,T> criterion) {
        iCriteria.put(criterion.getClass().getName(), criterion);
        criterion.setModel(this);
        addModelListener(criterion);
    }
    
    /** Unregister an existing criterion */
    public void removeCriterion(Criterion<V,T> criterion) {
        iCriteria.remove(criterion.getClass().getName());
        criterion.setModel(null);
        removeModelListener(criterion);
    }
    
    /** Unregister an existing criterion */
    public void removeCriterion(Class<? extends Criterion<V, T>> criterion) {
        Criterion<V,T> c = iCriteria.remove(criterion.getName());
        if (c != null)
            removeModelListener(c);
    }

    /** Return a registered criterion of the given type. */
    public Criterion<V, T> getCriterion(Class<? extends Criterion<V, T>> criterion) {
        return iCriteria.get(criterion.getName());
    }
    
    /** List all registered criteria */
    public Collection<Criterion<V, T>> getCriteria() {
        return iCriteria.values();
    }
    
    /**
     * Weaken all weakening constraint so that the given value can be assigned without
     * them creating a conflict using {@link WeakeningConstraint#weaken(Assignment, Value)}.
     * This method is handy for instance when an existing solution is being loaded
     * into the solver.
     */
    @SuppressWarnings("unchecked")
    public void weaken(Assignment<V, T> assignment, T value) {
        for (Constraint<V, T> constraint : value.variable().hardConstraints()) {
            if (constraint instanceof WeakeningConstraint)
                ((WeakeningConstraint<V,T>)constraint).weaken(assignment, value);
        }
        for (GlobalConstraint<V, T> constraint : globalConstraints()) {
            if (constraint instanceof WeakeningConstraint)
                ((WeakeningConstraint<V,T>)constraint).weaken(assignment, value);
        }
    }
    
    /**
     * Create a reference to an assignment context for a class that is in a need of one. Through this
     * reference an assignment context (see {@link AssignmentContext}) can be accessed using
     * {@link Assignment#getAssignmentContext(AssignmentContextReference)}.
     * @param parent class needing an assignment context
     * @return reference to an assignment context
     */
    public synchronized <C extends AssignmentContext> AssignmentContextReference<V,T,C> createReference(HasAssignmentContext<V, T, C> parent) {
        AssignmentContextReference<V, T, C> ref = new AssignmentContextReference<V, T, C>(parent, iNextReferenceId);
        iAssignmentContextReferences.put(iNextReferenceId, ref);
        iNextReferenceId++;
        return ref;
    }
    
    /**
     * Clear all assignment contexts for the given assignment
     * @param assignment given {@link Assignment}
     */
    public synchronized void clearAssignmentContexts(Assignment<V, T> assignment) {
        for (AssignmentContextReference<V,T,? extends AssignmentContext> ref: iAssignmentContextReferences.values())
            assignment.clearContext(ref);
    }
    
    /**
     * Create all assignment contexts for the given assignment
     * @param assignment given {@link Assignment}
     * @param clear if true {@link Assignment#clearContext(AssignmentContextReference)} is called first
     */
    public synchronized void createAssignmentContexts(Assignment<V, T> assignment, boolean clear) {
        for (AssignmentContextReference<V,T,? extends AssignmentContext> ref: iAssignmentContextReferences.values()) {
            if (clear) assignment.clearContext(ref);
            assignment.getAssignmentContext(ref);
        }
    }

    /**
     * Return default assignment that is using the old {@link Variable#getAssignment()} assignments.
     * @return as instance of {@link DefaultSingleAssignment}
     */
    @Deprecated
    public Assignment<V, T> getDefaultAssignment() {
        if (iAssignment == null)
            iAssignment = new DefaultSingleAssignment<V, T>();
        return iAssignment;
    }
    
    /**
     * Set default assignment 
     */
    @Deprecated
    public void setDefaultAssignment(Assignment<V, T> assignment) {
        iAssignment = assignment;
    }
    
    /**
     * Returns an instance of an empty assignment (using {@link EmptyAssignment})
     */
    public Assignment<V, T> getEmptyAssignment() {
        if (iEmptyAssignment == null)
            iEmptyAssignment = new EmptyAssignment<V, T>();
        return iEmptyAssignment;
    }
}
