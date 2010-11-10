package net.sf.cpsolver.ifs.model;

import java.util.Comparator;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.ArrayList;
import net.sf.cpsolver.ifs.util.Collection;
import net.sf.cpsolver.ifs.util.HashSet;
import net.sf.cpsolver.ifs.util.List;
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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
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
    protected Collection<V> iUnassignedVariables = new HashSet<V>();
    protected Collection<V> iAssignedVariables = new HashSet<V>();
    private Collection<V> iVariablesWithInitialValueCache = null;
    protected Collection<V> iPerturbVariables = null;

    private List<ModelListener<V, T>> iModelListeners = new ArrayList<ModelListener<V, T>>();
    private List<InfoProvider<V>> iInfoProviders = new ArrayList<InfoProvider<V>>();

    private int iBestUnassignedVariables = -1;
    private int iBestPerturbations = 0;
    private int iNrAssignedVariables = 0;

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
        iVariables.add(variable);
        if (variable instanceof InfoProvider<?>)
            iInfoProviders.add((InfoProvider<V>) variable);
        if (variable.getAssignment() == null) {
            if (iUnassignedVariables != null)
                iUnassignedVariables.add(variable);
        } else {
            if (iAssignedVariables != null)
                iAssignedVariables.add(variable);
            iNrAssignedVariables++;
        }
        if (variable.getAssignment() != null)
            variable.assign(0L, variable.getAssignment());
        for (ModelListener<V, T> listener : iModelListeners)
            listener.variableAdded(variable);
        invalidateVariablesWithInitialValueCache();
    }

    /** Removes a variable from the model */
    public void removeVariable(V variable) {
        variable.setModel(null);
        iVariables.remove(variable);
        if (variable instanceof InfoProvider<?>)
            iInfoProviders.remove(variable);
        if (iUnassignedVariables != null && iUnassignedVariables.contains(variable))
            iUnassignedVariables.remove(variable);
        if (iAssignedVariables != null && iAssignedVariables.contains(variable))
            iAssignedVariables.remove(variable);
        if (variable.getAssignment() != null)
            iNrAssignedVariables--;
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
        if (constraint instanceof InfoProvider<?>)
            iInfoProviders.add((InfoProvider<V>) constraint);
        for (ModelListener<V, T> listener : iModelListeners)
            listener.constraintAdded(constraint);
    }

    /** Removes a constraint from the model */
    public void removeConstraint(Constraint<V, T> constraint) {
        constraint.setModel(null);
        iConstraints.remove(constraint);
        if (constraint instanceof InfoProvider<?>)
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
        if (constraint instanceof InfoProvider<?>)
            iInfoProviders.add((InfoProvider<V>) constraint);
        for (ModelListener<V, T> listener : iModelListeners)
            listener.constraintAdded(constraint);
    }

    /** Removes a global constraint from the model */
    public void removeGlobalConstraint(GlobalConstraint<V, T> constraint) {
        constraint.setModel(null);
        iGlobalConstraints.remove(constraint);
        if (constraint instanceof InfoProvider<?>)
            iInfoProviders.remove(constraint);
        for (ModelListener<V, T> listener : iModelListeners)
            listener.constraintRemoved(constraint);
    }

    /** The list of unassigned variables in the model */
    public Collection<V> unassignedVariables() {
        if (iUnassignedVariables != null)
            return iUnassignedVariables;
        Collection<V> un = new ArrayList<V>(iVariables.size());
        for (V variable : iVariables) {
            if (variable.getAssignment() == null)
                un.add(variable);
        }
        return un;
    }

    /** Number of unassigned variables */
    public int nrUnassignedVariables() {
        if (iUnassignedVariables != null)
            return iUnassignedVariables.size();
        return iVariables.size() - iNrAssignedVariables;
    }

    /** The list of assigned variables in the model */
    public Collection<V> assignedVariables() {
        if (iAssignedVariables != null)
            return iAssignedVariables;
        Collection<V> as = new ArrayList<V>(iVariables.size());
        for (V variable : iVariables) {
            if (variable.getAssignment() != null)
                as.add(variable);
        }
        return as;
    }

    /** Number of assigned variables */
    public int nrAssignedVariables() {
        if (iAssignedVariables != null)
            return iAssignedVariables.size();
        return iNrAssignedVariables;
    }

    /**
     * The list of perturbation variables in the model, i.e., the variables
     * which has an initial value but which are not assigned with this value.
     */
    public Collection<V> perturbVariables() {
        if (iPerturbVariables == null)
            iPerturbVariables = perturbVariables(variablesWithInitialValue());
        return iPerturbVariables;
    }

    /**
     * The list of perturbation variables in the model, i.e., the variables
     * which has an initial value but which are not assigned with this value.
     * Only variables from the given set are considered.
     */
    public List<V> perturbVariables(java.util.Collection<V> variables) {
        List<V> perturbances = new ArrayList<V>();
        for (V variable : variables) {
            if (variable.getInitialAssignment() == null)
                continue;
            if (variable.getAssignment() != null) {
                if (!variable.getInitialAssignment().equals(variable.getAssignment()))
                    perturbances.add(variable);
            } else {
                boolean hasPerturbance = false;
                for (Constraint<V, T> constraint : variable.hardConstraints()) {
                    if (constraint.inConflict(variable.getInitialAssignment())) {
                        hasPerturbance = true;
                        break;
                    }
                }
                if (!hasPerturbance)
                    for (GlobalConstraint<V, T> constraint : globalConstraints()) {
                        if (constraint.inConflict(variable.getInitialAssignment())) {
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
     */
    public Set<T> conflictValues(T value) {
        Set<T> conflictValues = new HashSet<T>();
        for (Constraint<V, T> constraint : value.variable().hardConstraints())
            constraint.computeConflicts(value, conflictValues);
        for (GlobalConstraint<V, T> constraint : globalConstraints())
            constraint.computeConflicts(value, conflictValues);
        return conflictValues;
    }

    /** Return true if the given value is in conflict with a hard constraint */
    public boolean inConflict(T value) {
        for (Constraint<V, T> constraint : value.variable().hardConstraints())
            if (constraint.inConflict(value))
                return true;
        for (GlobalConstraint<V, T> constraint : globalConstraints())
            if (constraint.inConflict(value))
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
    public void beforeAssigned(long iteration, T value) {
        for (ModelListener<V, T> listener : iModelListeners)
            listener.beforeAssigned(iteration, value);
    }

    /** Called before a value is unassigned from its variable */
    public void beforeUnassigned(long iteration, T value) {
        for (ModelListener<V, T> listener : iModelListeners)
            listener.beforeUnassigned(iteration, value);
    }

    /** Called after a value is assigned to its variable */
    public void afterAssigned(long iteration, T value) {
        if (iUnassignedVariables != null)
            iUnassignedVariables.remove(value.variable());
        if (iAssignedVariables != null)
            iAssignedVariables.add(value.variable());
        iNrAssignedVariables++;
        iPerturbVariables = null;
        for (ModelListener<V, T> listener : iModelListeners)
            listener.afterAssigned(iteration, value);
    }

    /** Called after a value is unassigned from its variable */
    public void afterUnassigned(long iteration, T value) {
        if (iUnassignedVariables != null)
            iUnassignedVariables.add(value.variable());
        if (iAssignedVariables != null)
            iAssignedVariables.remove(value.variable());
        iNrAssignedVariables--;
        iPerturbVariables = null;
        for (ModelListener<V, T> listener : iModelListeners)
            listener.afterUnassigned(iteration, value);
    }

    @Override
    public String toString() {
        return "Model{\n    variables=" + ToolBox.col2string(variables(), 2) + ",\n    constraints="
                + ToolBox.col2string(constraints(), 2) + ",\n    #unassigned=" + nrUnassignedVariables()
                + ",\n    unassigned=" + ToolBox.col2string(unassignedVariables(), 2) + ",\n    #perturbations="
                + perturbVariables().size() + "+" + (variables().size() - variablesWithInitialValue().size())
                + ",\n    perturbations=" + ToolBox.col2string(perturbVariables(), 2) + ",\n    info=" + getInfo()
                + "\n  }";
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
     */
    public Hashtable<String, String> getInfo() {
        Hashtable<String, String> ret = new Hashtable<String, String>();
        ret.put("Assigned variables", getPercRev(nrAssignedVariables(), 0, variables().size()) + "% ("
                + nrAssignedVariables() + "/" + variables().size() + ")");
        int nrVarsWithInitialValue = variablesWithInitialValue().size();
        if (nrVarsWithInitialValue > 0) {
            ret.put("Perturbation variables", getPercRev(perturbVariables().size(), 0, nrVarsWithInitialValue) + "% ("
                    + perturbVariables().size() + " + " + (variables().size() - nrVarsWithInitialValue) + ")");
        }
        ret.put("Overall solution value", sDoubleFormat.format(getTotalValue()));
        for (InfoProvider<V> provider : iInfoProviders)
            provider.getInfo(ret);
        return ret;
    }

    /**
     * Extended information about current solution. Similar to
     * {@link Model#getInfo()}, but some more information (that is more
     * expensive to compute) might be added.
     */
    public Hashtable<String, String> getExtendedInfo() {
        return getInfo();
    }

    /**
     * Returns information about the current solution. Information from all
     * model listeners and constraints is also included. Only variables from the
     * given set are considered.
     */
    public Hashtable<String, String> getInfo(java.util.Collection<V> variables) {
        Hashtable<String, String> ret = new Hashtable<String, String>();
        int assigned = 0, perturb = 0, nrVarsWithInitialValue = 0;
        for (V variable : variables) {
            if (variable.getAssignment() != null)
                assigned++;
            if (variable.getInitialAssignment() != null) {
                nrVarsWithInitialValue++;
                if (variable.getAssignment() != null) {
                    if (!variable.getInitialAssignment().equals(variable.getAssignment()))
                        perturb++;
                } else {
                    boolean hasPerturbance = false;
                    for (Constraint<V, T> constraint : variable.hardConstraints()) {
                        if (constraint.inConflict(variable.getInitialAssignment())) {
                            hasPerturbance = true;
                            break;
                        }
                    }
                    if (!hasPerturbance)
                        for (GlobalConstraint<V, T> constraint : globalConstraints()) {
                            if (constraint.inConflict(variable.getInitialAssignment())) {
                                hasPerturbance = true;
                                break;
                            }
                        }
                    if (hasPerturbance)
                        perturb++;
                }
            }
        }
        ret.put("Assigned variables", getPercRev(assigned, 0, variables.size()) + "% (" + assigned + "/"
                + variables.size() + ")");
        if (nrVarsWithInitialValue > 0) {
            ret.put("Perturbation variables", getPercRev(perturb, 0, nrVarsWithInitialValue) + "% (" + perturb + " + "
                    + (variables.size() - nrVarsWithInitialValue) + ")");
        }
        ret.put("Overall solution value", sDoubleFormat.format(getTotalValue(variables)));
        for (InfoProvider<V> provider : iInfoProviders)
            provider.getInfo(ret, variables);
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

    /** Save the current assignment as the best ever found assignment */
    public void saveBest() {
        iBestUnassignedVariables = nrUnassignedVariables();// unassignedVariables().size();
        iBestPerturbations = perturbVariables().size();
        for (V variable : iVariables) {
            variable.setBestAssignment(variable.getAssignment());
        }
    }

    /** Clear the best ever found assignment */
    public void clearBest() {
        iBestUnassignedVariables = -1;
        iBestPerturbations = 0;
        for (V variable : iVariables) {
            variable.setBestAssignment(null);
        }
    }

    /** Restore the best ever found assignment into the current assignment */
    public void restoreBest() {
        for (V variable : iVariables) {
            if (variable.getAssignment() != null && !variable.getAssignment().equals(variable.getBestAssignment()))
                variable.unassign(0);
        }
        Set<T> problems = new HashSet<T>();
        TreeSet<V> sortedVariables = new TreeSet<V>(new BestAssignmentComparator());
        sortedVariables.addAll(iVariables);
        for (V variable : sortedVariables) {
            if (variable.getBestAssignment() != null && variable.getAssignment() == null) {
                Set<T> confs = conflictValues(variable.getBestAssignment());
                if (!confs.isEmpty()) {
                    sLogger.error("restore best problem: assignment " + variable.getName() + " = "
                            + variable.getBestAssignment().getName());
                    for (Constraint<V, T> c : variable.hardConstraints()) {
                        Set<T> x = new HashSet<T>();
                        c.computeConflicts(variable.getBestAssignment(), x);
                        if (!x.isEmpty()) {
                            sLogger.error("  constraint " + c.getClass().getName() + " " + c.getName()
                                    + " causes the following conflicts " + x);
                        }
                    }
                    for (GlobalConstraint<V, T> c : globalConstraints()) {
                        Set<T> x = new HashSet<T>();
                        c.computeConflicts(variable.getBestAssignment(), x);
                        if (!x.isEmpty()) {
                            sLogger.error("  global constraint " + c.getClass().getName() + " " + c.getName()
                                    + " causes the following conflicts " + x);
                        }
                    }
                    problems.add(variable.getBestAssignment());
                } else
                    variable.assign(0, variable.getBestAssignment());
            }
        }
        int attempt = 0;
        while (!problems.isEmpty() && attempt <= 100) {
            attempt++;
            T value = ToolBox.random(problems);
            problems.remove(value);
            V variable = value.variable();
            Set<T> confs = conflictValues(value);
            if (!confs.isEmpty()) {
                sLogger.error("restore best problem (again, att=" + attempt + "): assignment " + variable.getName()
                        + " = " + value.getName());
                for (Constraint<V, T> c : variable.hardConstraints()) {
                    Set<T> x = new HashSet<T>();
                    c.computeConflicts(value, x);
                    if (!x.isEmpty())
                        sLogger.error("  constraint " + c.getClass().getName() + " " + c.getName()
                                + " causes the following conflicts " + x);
                }
                for (GlobalConstraint<V, T> c : globalConstraints()) {
                    Set<T> x = new HashSet<T>();
                    c.computeConflicts(value, x);
                    if (!x.isEmpty())
                        sLogger.error("  constraint " + c.getClass().getName() + " " + c.getName()
                                + " causes the following conflicts " + x);
                }
                for (T conf : confs)
                    conf.variable().unassign(0);
                problems.addAll(confs);
            }
            variable.assign(0, value);
        }
    }

    /** The list of unassigned variables in the best ever found solution */
    public Collection<V> bestUnassignedVariables() {
        if (iBestUnassignedVariables < 0)
            return unassignedVariables();
        Collection<V> ret = new ArrayList<V>(variables().size());
        for (V variable : variables()) {
            if (variable.getBestAssignment() == null)
                ret.add(variable);
        }
        return ret;
    }

    /**
     * Value of the current solution. It is the sum of all assigned values,
     * i.e., {@link Value#toDouble()}.
     */
    public double getTotalValue() {
        double valCurrent = 0;
        for (V variable : assignedVariables())
            valCurrent += variable.getAssignment().toDouble();
        return valCurrent;
    }

    /**
     * Value of the current solution. It is the sum of all assigned values,
     * i.e., {@link Value#toDouble()}. Only variables from the given set are
     * considered.
     **/
    public double getTotalValue(java.util.Collection<V> variables) {
        double valCurrent = 0;
        for (V variable : variables) {
            if (variable.getAssignment() != null)
                valCurrent += variable.getAssignment().toDouble();
        }
        return valCurrent;
    }

    /** Adds a model listener */
    @SuppressWarnings("unchecked")
    public void addModelListener(ModelListener<V, T> listener) {
        iModelListeners.add(listener);
        if (listener instanceof InfoProvider<?>)
            iInfoProviders.add((InfoProvider<V>) listener);
        for (Constraint<V, T> constraint : iConstraints)
            listener.constraintAdded(constraint);
        for (V variable : iVariables)
            listener.variableAdded(variable);
    }

    /** Removes a model listener */
    public void removeModelListener(ModelListener<V, T> listener) {
        if (listener instanceof InfoProvider<?>)
            iInfoProviders.remove(listener);
        for (V variable : iVariables)
            listener.variableRemoved(variable);
        for (Constraint<V, T> constraint : iConstraints)
            listener.constraintRemoved(constraint);
        iModelListeners.remove(listener);
    }

    /** Model initialization */
    public boolean init(Solver<V, T> solver) {
        for (ModelListener<V, T> listener : iModelListeners) {
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
     * {@link Constraint#computeConflicts(Value, Set)}.
     */
    public Hashtable<Constraint<V, T>, Set<T>> conflictConstraints(T value) {
        Hashtable<Constraint<V, T>, Set<T>> conflictConstraints = new Hashtable<Constraint<V, T>, Set<T>>();
        for (Constraint<V, T> constraint : value.variable().hardConstraints()) {
            Set<T> conflicts = new HashSet<T>();
            constraint.computeConflicts(value, conflicts);
            if (!conflicts.isEmpty()) {
                conflictConstraints.put(constraint, conflicts);
            }
        }
        for (GlobalConstraint<V, T> constraint : globalConstraints()) {
            Set<T> conflicts = new HashSet<T>();
            constraint.computeConflicts(value, conflicts);
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
    public List<Constraint<V, T>> unassignedHardConstraints() {
        List<Constraint<V, T>> ret = new ArrayList<Constraint<V, T>>();
        constraints: for (Constraint<V, T> constraint : constraints()) {
            if (!constraint.isHard())
                continue;
            for (V v : constraint.variables()) {
                if (v.getAssignment() == null) {
                    ret.add(constraint);
                    continue constraints;
                }
            }
        }
        if (!unassignedVariables().isEmpty())
            ret.addAll(globalConstraints());
        return ret;
    }

    private class BestAssignmentComparator implements Comparator<V> {
        public int compare(V v1, V v2) {
            if (v1.getBestAssignmentIteration() < v2.getBestAssignmentIteration()) return -1;
            if (v1.getBestAssignmentIteration() > v2.getBestAssignmentIteration()) return 1;
            return v1.compareTo(v2);
        }
    }

    /** Registered info providers (see {@link InfoProvider}) */
    protected List<InfoProvider<V>> getInfoProviders() {
        return iInfoProviders;
    }
}
