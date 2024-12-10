package org.cpsolver.ifs.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.ValueSelection;
import org.cpsolver.ifs.heuristics.VariableSelection;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.ConstraintListener;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Conflict-based statistics. <br>
 * <br>
 * The idea behind it is to memorize conflicts and to avoid their potential
 * repetition. When a value v0 is assigned to a variable V0, hard conflicts with
 * previously assigned variables (e.g., V1 = v1, V2 = v2, ... Vm = vm) may
 * occur. These variables V1,...,Vm have to be unassigned before the value v0 is
 * assigned to the variable V0. These unassignments, together with the reason
 * for their unassignment (i.e., the assignment V0 = v0), and a counter tracking
 * how many times such an event occurred in the past, is stored in memory. <br>
 * <br>
 * Later, if a variable is selected for assignment again, the stored information
 * about repetition of past hard conflicts can be taken into account, e.g., in
 * the value selection heuristics. Assume that the variable V0 is selected for
 * an assignment again (e.g., because it became unassigned as a result of a
 * later assignment), we can weight the number of hard conflicts created in the
 * past for each possible value of this variable. In the above example, the
 * existing assignment V1 = v1 can prohibit the selection of value v0 for
 * variable V0 if there is again a conflict with the assignment V1 = v1. <br>
 * <br>
 * Conflict-based statistics are a data structure which memorizes the number of
 * hard conflicts that have occurred during the search (e.g., that assignment V0
 * = v0 resulted c1 times in an unassignment of V1 = v1, c2 times of V2 = v2, .
 * . . and cm times of Vm = vm). More precisely, they form an array
 * <pre><code>
 * CBS[Va = va, Vb != vb] = cab,
 * </code></pre>
 * stating that the assignment Va = va caused the unassignment of Vb = vb a
 * total of cab times in the past. Note that in case of n-ary constraints (where
 * n &gt; 2), this does not imply that the assignments Va = va and Vb = vb cannot
 * be used together. The proposed conflict-based statistics do not actually work
 * with any constraint, they only memorize unassignments and the assignment that
 * caused them. Let us consider a variable Va selected by the
 * {@link VariableSelection#selectVariable(Solution)} function and a value va
 * selected by {@link ValueSelection#selectValue(Solution, Variable)}. Once the
 * assignment Vb = vb is selected by {@link Model#conflictValues(Assignment, Value)} to be
 * unassigned, the array cell CBS[Va = va, Vb != vb] is incremented by one. <br>
 * <br>
 * The data structure is implemented as a hash table, storing information for
 * conflict-based statistics. A counter is maintained for the tuple A = a and B
 * != b. This counter is increased when the value a is assigned to the variable
 * A and b is unassigned from B. The example of this structure
 * <pre><code>
 * A = a &nbsp;&nbsp;&nbsp; &#8594; &nbsp;&nbsp;&nbsp; 3 x B != b, &nbsp; 4 x B
 * != c, &nbsp; 2 x C != a, &nbsp; 120 x D != a
 * </code></pre>
 * expresses that variable B lost its assignment b three times and its
 * assignment c four times, variable C lost its assignment a two times, and D
 * lost its assignment a 120 times, all because of later assignments of value a
 * to variable A. This structure is being used in the value selection heuristics
 * to evaluate existing conflicts with the assigned variables. For example, if
 * there is a variable A selected and if the value a is in conflict with the
 * assignment B = b, we know that a similar problem has already occurred 3x in
 * the past, and hence the conflict A = a is weighted with the number 3. <br>
 * <br>
 * Then, a min-conflict value selection criterion, which selects a value with
 * the minimal number of conflicts with the existing assignments, can be easily
 * adapted to a weighted min-conflict criterion. The value with the smallest sum
 * of the number of conflicts multiplied by their frequencies is selected.
 * Stated in another way, the weighted min-conflict approach helps the value
 * selection heuristics to select a value that might cause more conflicts than
 * another value, but these conflicts occurred less frequently, and therefore
 * they have a lower weighted sum. <br>
 * <br>
 * The conflict-based statistics has also implemented the following extensions:
 * <ul>
 * <li>If a variable is selected for an assignment, the above presented
 * structure can also tell how many potential conflicts a value can cause in the
 * future. In the above example, we already know that four times a later
 * assignment of A=a caused that value c was unassigned from B. We can try to
 * minimize such future conflicts by selecting a different value of the variable
 * B while A is still unbound.
 * <li>The memorized conflicts can be aged according to how far they have
 * occurred in the past. For example, a conflict which occurred 1000 iterations
 * ago can have half the weight of a conflict which occurred during the last
 * iteration or it can be forgotten at all.
 * </ul>
 * Furthermore, the presented conflict-based statistics can be used not only
 * inside the solving mechanism. The constructed "implications" together with
 * the information about frequency of their occurrences can be easily accessed
 * by users or by some add-on deductive engine to identify inconsistencies1
 * and/or hard parts of the input problem. The user can then modify the input
 * requirements in order to eliminate problems found and let the solver continue
 * the search with this modified input problem. <br>
 * <br>
 * Parameters: <br>
 * <table border='1'><caption>Related Solver Parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>ConflictStatistics.Ageing</td>
 * <td>{@link Double}</td>
 * <td>Ageing of the conflict-based statistics. Every memorized conflict is aged
 * (multiplited) by this factor for every iteration which passed from the time
 * it was memorized. For instance, if there was a conflict 10 iterations ago,
 * its value is ageing^10 (default is 1.0 -- no ageing).</td>
 * </tr>
 * <tr>
 * <td>ConflictStatistics.AgeingHalfTime</td>
 * <td>{@link Integer}</td>
 * <td>Another way how to express ageing: number of iterations to decrease a
 * conflict to 1/2 (default is 0 -- no ageing)</td>
 * </tr>
 * </table>
 * 
 * @see Solver
 * @see Model
 * @see ValueSelection
 * @see VariableSelection
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
public class ConflictStatistics<V extends Variable<V, T>, T extends Value<V, T>> extends Extension<V, T> implements ConstraintListener<V, T> {
    private static final String PARAM_AGEING = "ConflictStatistics.Ageing";
    private static final String PARAM_HALF_AGE = "ConflictStatistics.AgeingHalfTime";
    private static final String PARAM_PRINT = "ConflictStatistics.Print";

    private double iAgeing = 1.0;
    private boolean iPrint = false;

    private Map<AssignedValue<T>, List<AssignedValue<T>>> iAssignments = new HashMap<AssignedValue<T>, List<AssignedValue<T>>>();
    private Map<V, List<AssignedValue<T>>> iUnassignedVariables = new HashMap<V, List<AssignedValue<T>>>();
    private Map<AssignedValue<T>, List<AssignedValue<T>>> iNoGoods = new HashMap<AssignedValue<T>, List<AssignedValue<T>>>();
    
    private final ReentrantReadWriteLock iLock = new ReentrantReadWriteLock();

    public ConflictStatistics(Solver<V, T> solver, DataProperties properties) {
        super(solver, properties);
        iAgeing = properties.getPropertyDouble(PARAM_AGEING, iAgeing);
        int halfAge = properties.getPropertyInt(PARAM_HALF_AGE, 0);
        if (halfAge > 0)
            iAgeing = Math.exp(Math.log(0.5) / (halfAge));
        iPrint = properties.getPropertyBoolean(PARAM_PRINT, iPrint);
    }

    @Override
    public void register(Model<V, T> model) {
        super.register(model);
    }

    @Override
    public void unregister(Model<V, T> model) {
        super.unregister(model);
    }

    private void variableUnassigned(long iteration, T unassignedValue, AssignedValue<T> noGood) {
        if (iteration <= 0) return;
        iLock.writeLock().lock();
        try {
            AssignedValue<T> unass = new AssignedValue<T>(iteration, unassignedValue, iAgeing);
            List<AssignedValue<T>> noGoodsForUnassignment = iNoGoods.get(unass);
            if (noGoodsForUnassignment != null) {
                if (noGoodsForUnassignment.contains(noGood)) {
                    (noGoodsForUnassignment.get(noGoodsForUnassignment.indexOf(noGood))).incCounter(iteration);
                } else {
                    noGoodsForUnassignment.add(noGood);
                }
            } else {
                noGoodsForUnassignment = new ArrayList<AssignedValue<T>>();
                noGoodsForUnassignment.add(noGood);
                iNoGoods.put(unass, noGoodsForUnassignment);
            }
        } finally {
            iLock.writeLock().unlock();
        }
    }

    public void reset() {
        iLock.writeLock().lock();
        try {
            iUnassignedVariables.clear();
            iAssignments.clear();
        } finally {
            iLock.writeLock().unlock();
        }
    }

    public Map<AssignedValue<T>, List<AssignedValue<T>>> getNoGoods() {
        return iNoGoods;
    }

    public void variableUnassigned(long iteration, T unassignedValue, T assignedValue) {
        if (iteration <= 0) return;
        AssignedValue<T> ass = new AssignedValue<T>(iteration, assignedValue, iAgeing);
        AssignedValue<T> unass = new AssignedValue<T>(iteration, unassignedValue, iAgeing);
        iLock.writeLock().lock();
        try {
            if (iAssignments.containsKey(unass)) {
                List<AssignedValue<T>> asss = iAssignments.get(unass);
                if (asss.contains(ass)) {
                    asss.get(asss.indexOf(ass)).incCounter(iteration);
                } else {
                    asss.add(ass);
                }
            } else {
                List<AssignedValue<T>> asss = new ArrayList<AssignedValue<T>>();
                asss.add(ass);
                iAssignments.put(unass, asss);
            }
            if (iUnassignedVariables.containsKey(unassignedValue.variable())) {
                List<AssignedValue<T>> asss = iUnassignedVariables.get(unassignedValue.variable());
                if (asss.contains(ass)) {
                    (asss.get(asss.indexOf(ass))).incCounter(iteration);
                } else {
                    asss.add(ass);
                }
            } else {
                List<AssignedValue<T>> asss = new ArrayList<AssignedValue<T>>();
                asss.add(ass);
                iUnassignedVariables.put(unassignedValue.variable(), asss);
            }
        } finally {
            iLock.writeLock().unlock();
        }
    }

    /**
     * Counts number of unassignments of the given conflicting values caused by
     * the assignment of the given value.
     * @param iteration current iteration
     * @param conflictValues values conflicting with the given value
     * @param value given value
     * @return number of unassignments
     */
    public double countRemovals(long iteration, Collection<T> conflictValues, T value) {
        long ret = 0;
        for (T conflictValue : conflictValues) {
            ret += countRemovals(iteration, conflictValue, value);
            // tady bylo +1
        }
        return ret;
    }

    /**
     * Counts number of unassignments of the given conflicting value caused by
     * the assignment of the given value.
     * @param iteration current iteration
     * @param conflictValue value conflicting with the given value
     * @param value given value
     * @return number of unassignments
     */
    public double countRemovals(long iteration, T conflictValue, T value) {
        iLock.readLock().lock();
        try {
            List<AssignedValue<T>> asss = iUnassignedVariables.get(conflictValue.variable());
            if (asss == null)
                return 0;
            AssignedValue<T> ass = new AssignedValue<T>(iteration, value, iAgeing);
            int idx = asss.indexOf(ass);
            if (idx < 0)
                return 0;
            return (asss.get(idx)).getCounter(iteration);
        } finally {
            iLock.readLock().unlock();
        }
    }

    /**
     * Counts potential number of unassignments of if the given value is
     * selected.
     * @param assignment current assignment
     * @param iteration current iteration
     * @param value given value
     * @param limit conflict limit
     * @return number of potential unassignments
     */
    public long countPotentialConflicts(Assignment<V, T> assignment, long iteration, T value, int limit) {
        iLock.readLock().lock();
        try {
            List<AssignedValue<T>> asss = iAssignments.get(new AssignedValue<T>(iteration, value, iAgeing));
            if (asss == null)
                return 0;
            long count = 0;
            for (AssignedValue<T> ass : asss) {
                if (ass.getValue().variable().getAssignment(assignment) == null) {
                    if (limit >= 0) {
                        count += ass.getCounter(iteration) * Math.max(0, 1 + limit - value.variable().getModel().conflictValues(assignment, ass.getValue()).size());
                    } else {
                        count += ass.getCounter(iteration);
                    }
                }
            }
            return count;            
        } finally {
            iLock.readLock().unlock();
        }
    }
    
    /**
     * Count the number of past assignments of a variable
     * @param variable given variable
     * @return total number of past assignments
     */
    public long countAssignments(V variable) {
        iLock.readLock().lock();
        try {
            List<AssignedValue<T>> assignments = iUnassignedVariables.get(variable);
            if (assignments == null || assignments.isEmpty()) return 0;
            double ret = 0;
            for (AssignedValue<T> assignment: assignments) {
                ret += assignment.getCounter(0);
            }
            return Math.round(ret);
        } finally {
            iLock.readLock().unlock();
        }
    }

    @Override
    public String toString() {
        iLock.readLock().lock();
        try {
            if (iPrint) {
                StringBuffer sb = new StringBuffer("Statistics{");
                TreeSet<AssignedValue<T>> sortedUnassignments = new TreeSet<AssignedValue<T>>(new Comparator<AssignedValue<T>>() {
                    @Override
                    public int compare(AssignedValue<T> x1, AssignedValue<T> x2) {
                        int c1 = 0, c2 = 0;
                        for (AssignedValue<T> y: iNoGoods.get(x1))
                            c1 += y.getCounter(0);
                        for (AssignedValue<T> y: iNoGoods.get(x2))
                            c2 += y.getCounter(0);
                        int cmp = Double.compare(c1, c2);
                        if (cmp != 0)
                            return -cmp;
                        return x1.compareTo(0, x2);
                    }
                });
                sortedUnassignments.addAll(iNoGoods.keySet());
                int printedUnassignments = 0;
                for (AssignedValue<T> x : sortedUnassignments) {
                    int c = 0;
                    for (AssignedValue<T> y: iNoGoods.get(x))
                        c += y.getCounter(0);
                    sb.append("\n    ").append(c + "x ").append(x.toString(0, false)).append(" <= {");
                    TreeSet<AssignedValue<T>> sortedAssignments = new TreeSet<AssignedValue<T>>(new Comparator<AssignedValue<T>>() {
                        @Override
                        public int compare(AssignedValue<T> x1, AssignedValue<T> x2) {
                            int cmp = Double.compare(x1.getCounter(0), x2.getCounter(0));
                            if (cmp != 0)
                                return -cmp;
                            return x1.compareTo(0, x2);
                        }
                    });
                    sortedAssignments.addAll(iNoGoods.get(x));
                    int printedAssignments = 0;
                    for (AssignedValue<T> y : sortedAssignments) {
                        sb.append("\n        ").append(y.toString(0, true));
                        if (++printedAssignments == 20) {
                            sb.append("\n        ...");
                            break;
                        }
                    }
                    sb.append("\n      }");
                    if (++printedUnassignments == 100) {
                        sb.append("\n     ...");
                        break;
                    }
                }
                sb.append("\n    }");
                return sb.toString();            
            } else {
                StringBuffer sb = new StringBuffer("Statistics{");
                TreeSet<V> sortedUnassignedVariables = new TreeSet<V>(new Comparator<V>() {
                    @Override
                    public int compare(V v1, V v2) {
                        int cmp = Double.compare(countAssignments(v1), countAssignments(v2));
                        if (cmp != 0)
                            return -cmp;
                        return v1.compareTo(v2);
                    }
                });
                sortedUnassignedVariables.addAll(iUnassignedVariables.keySet());
                int printedVariables = 0;
                for (V variable : sortedUnassignedVariables) {
                    sb.append("\n      ").append(countAssignments(variable) + "x ").append(variable.getName()).append(" <= {");
                    TreeSet<AssignedValue<T>> sortedAssignments = new TreeSet<AssignedValue<T>>(new Comparator<AssignedValue<T>>() {
                        @Override
                        public int compare(AssignedValue<T> x1, AssignedValue<T> x2) {
                            int cmp = Double.compare(x1.getCounter(0), x2.getCounter(0));
                            if (cmp != 0)
                                return -cmp;
                            return x1.compareTo(0, x2);
                        }
                    });
                    sortedAssignments.addAll(iUnassignedVariables.get(variable));
                    int printedAssignments = 0;
                    for (AssignedValue<T> x : sortedAssignments) {
                        sb.append("\n        ").append(x.toString(0, true));
                        if (++printedAssignments == 20) {
                            sb.append("\n        ...");
                            break;
                        }
                    }
                    sb.append("\n      }");
                    if (++printedVariables == 100) {
                        sb.append("\n      ...");
                        break;
                    }
                }
                sb.append("\n    }");
                return sb.toString();            
            }
        } finally {
            iLock.readLock().unlock();
        }
    }

    @Override
    public void constraintBeforeAssigned(Assignment<V, T> assignment, long iteration, Constraint<V, T> constraint, T assigned, Set<T> unassigned) {
    }

    /** Increments appropriate counters when there is a value unassigned */
    @Override
    public void constraintAfterAssigned(Assignment<V, T> assignment, long iteration, Constraint<V, T> constraint, T assigned, Set<T> unassigned) {
        if (iteration <= 0)
            return;
        if (unassigned == null || unassigned.isEmpty())
            return;
        if (iPrint) {
            // AssignmentSet noGoods =
            // AssignmentSet.createAssignmentSet(iteration,unassigned, iAgeing);
            // noGoods.addAssignment(iteration, assigned, iAgeing);
            // noGoods.setConstraint(constraint);
            AssignedValue<T> noGood = new AssignedValue<T>(iteration, assigned, iAgeing);
            noGood.setConstraint(constraint);
            for (T unassignedValue : unassigned) {
                variableUnassigned(iteration, unassignedValue, noGood);
                variableUnassigned(iteration, unassignedValue, assigned);
            }
        } else {
            for (T unassignedValue : unassigned) {
                variableUnassigned(iteration, unassignedValue, assigned);
            }
        }
    }

    @Override
    public void constraintAdded(Constraint<V, T> constraint) {
        constraint.addConstraintListener(this);
    }

    @Override
    public void constraintRemoved(Constraint<V, T> constraint) {
        constraint.removeConstraintListener(this);
    }
}
