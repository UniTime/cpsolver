package org.cpsolver.ifs.algorithms.neighbourhoods;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.SimpleNeighbour;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.JProf;
import org.cpsolver.ifs.util.ToolBox;


/**
 * Try to assign a variable with a new value. A variable is selected randomly, a
 * different value is randomly selected for the variable -- the variable is
 * assigned with the new value.  If there is a conflict, it tries to resolve these
 * conflicts by assigning conflicting variables to other values as well.
 * <br>
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Iterative Forward Search)<br>
 *          Copyright (C) 2014 Tomas Muller<br>
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
public class RandomSwapMove<V extends Variable<V, T>, T extends Value<V, T>> implements NeighbourSelection<V, T>, HillClimberSelection {
    protected int iMaxAttempts = 3;
    protected boolean iHC = false;
    protected int iTimeLimit = 200;

    public RandomSwapMove(DataProperties config) {
        iMaxAttempts = config.getPropertyInt("RandomSwap.MaxAttempts", iMaxAttempts);
        iTimeLimit = config.getPropertyInt("RandomSwap.TimeLimit", iTimeLimit);
    }
    
    @Override
    public void setHcMode(boolean hcMode) {
        iHC = hcMode;
    }

    @Override
    public void init(Solver<V, T> solver) {
    }

    @Override
    public Neighbour<V, T> selectNeighbour(Solution<V, T> solution) {
        Model<V, T> model = solution.getModel();
        Assignment<V, T> assignment = solution.getAssignment();
        double total = model.getTotalValue(assignment);
        int varIdx = ToolBox.random(model.variables().size());
        for (int i = 0; i < model.variables().size(); i++) {
            V variable = model.variables().get((i + varIdx) % model.variables().size());
            List<T> values = variable.values(solution.getAssignment());
            if (values.isEmpty()) continue;
            int valIdx = ToolBox.random(values.size());
            T old = variable.getAssignment(assignment);
            
            Lock lock = solution.getLock().writeLock();
            lock.lock();
            try {
                int attempts = 0;
                long startTime = JProf.currentTimeMillis();
                for (int j = 0; j < values.size(); j++) {
                    T value = values.get((j + valIdx) % values.size());
                    if (value.equals(old)) continue;
                    
                    Set<T> conflicts = model.conflictValues(assignment, value);
                    if (conflicts.contains(value)) continue;
                    if (conflicts.isEmpty()) {
                        SimpleNeighbour<V, T> n = new SimpleNeighbour<V, T>(variable, value);
                        if (!iHC || n.value(assignment) <= 0) return n;
                        else continue;
                    }
                    
                    Map<V, T> assignments = new HashMap<V, T>();
                    assignments.put(variable, value);
                    
                    for (T conflict: conflicts)
                        assignment.unassign(solution.getIteration(), conflict.variable());
                    assignment.assign(solution.getIteration(), value);
                    
                    Double v = resolve(solution, total, startTime, assignments, new ArrayList<T>(conflicts), 0);
                    if (!conflicts.isEmpty())
                        attempts ++;
                    
                    assignment.unassign(solution.getIteration(), variable);
                    for (T conflict: conflicts)
                        assignment.assign(solution.getIteration(), conflict);
                    if (old != null) assignment.assign(solution.getIteration(), old);
                    
                    if (v != null)
                        return new SwapNeighbour(assignments.values(), old == null ? -1 : v);
                    
                    if (attempts >= iMaxAttempts) break;
                }
            } finally {
                lock.unlock();
            }
        }
        return null;
    }
    
    /**
     * Return true if the time limit was reached, number of attempts are limited to 1 in such a case.
     * @param startTime start time
     * @return true if the given time limit was reached
     */
    protected boolean isTimeLimitReached(long startTime) {
        return iTimeLimit > 0 && (JProf.currentTimeMillis() - startTime) > iTimeLimit;
    }
    
    /**
     * Try to resolve given conflicts. For each conflicting variable it tries to find a 
     * value with no conflict that is compatible with some other assignment
     * of the other conflicting variables. 
     * @param solution current solution
     * @param total original value of the current solution
     * @param startTime starting time
     * @param assignments re-assignments to be made
     * @param conflicts list of conflicts to resolve
     * @param index index in the list of conflicts
     * @return value of the modified solution, null if cannot be resolved
     */
    protected Double resolve(Solution<V, T> solution, double total, long startTime, Map<V, T> assignments, List<T> conflicts, int index) {
        Assignment<V, T> assignment = solution.getAssignment();

        if (index == conflicts.size()) return solution.getModel().getTotalValue(assignment) - total;
        T conflict = conflicts.get(index);
        V variable = conflict.variable();
        
        List<T> values = variable.values(solution.getAssignment());
        if (values.isEmpty()) return null;
        
        int valIdx = ToolBox.random(values.size());
        int attempts = 0;
        for (int i = 0; i < values.size(); i++) {
            T value = values.get((i + valIdx) % values.size());
            if (value.equals(conflict) || solution.getModel().inConflict(assignment, value)) continue;
            
            assignment.assign(solution.getIteration(), value);
            Double v = resolve(solution, total, startTime, assignments, conflicts, 1 + index);
            assignment.unassign(solution.getIteration(), variable);
            attempts ++;
            
            if (v != null && (!iHC || v <= 0)) {
                assignments.put(variable, value);
                return v;
            }
            if (attempts >= iMaxAttempts || isTimeLimitReached(startTime)) break;
        }
            
        return null;
    }
    
    public class SwapNeighbour implements Neighbour<V, T> {
        private double iValue = 0;
        private Collection<T> iAssignments = null;

        public SwapNeighbour(Collection<T> assignments, double value) {
            iAssignments = assignments; iValue = value;
        }

        @Override
        public double value(Assignment<V, T> assignment) {
            return iValue;
        }

        @Override
        public void assign(Assignment<V, T> assignment, long iteration) {
            for (T value: iAssignments)
                assignment.unassign(iteration, value.variable(), value);
            for (T value: iAssignments)
                assignment.assign(iteration, value);
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer("Swap{" + iValue + ": ");
            for (Iterator<T> i = iAssignments.iterator(); i.hasNext(); ) {
                T p = i.next();
                sb.append("\n    " + p.variable().getName() + " " + p.getName() + (i.hasNext() ? "," : ""));
            }
            sb.append("}");
            return sb.toString();
        }

        @Override
        public Map<V, T> assignments() {
            Map<V, T> ret = new HashMap<V, T>();
            for (T value: iAssignments)
                ret.put(value.variable(), value);
            return ret;
        }
    }
}