package net.sf.cpsolver.ifs.algorithms.neighbourhoods;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.SimpleNeighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.JProf;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Try to assign a variable with a new value. A variable is selected randomly, a
 * different value is randomly selected for the variable -- the variable is
 * assigned with the new value.  If there is a conflict, it tries to resolve these
 * conflicts by assigning conflicting variables to other values as well.
 * <br>
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
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
        double total = model.getTotalValue();
        int varIdx = ToolBox.random(model.variables().size());
        for (int i = 0; i < model.variables().size(); i++) {
            V variable = model.variables().get((i + varIdx) % model.variables().size());
            List<T> values = variable.values();
            if (values.isEmpty()) continue;
            int valIdx = ToolBox.random(values.size());
            T old = variable.getAssignment();
            
            int attempts = 0;
            long startTime = JProf.currentTimeMillis();
            for (int j = 0; j < values.size(); j++) {
                T value = values.get((j + valIdx) % values.size());
                if (value.equals(old)) continue;
                
                Set<T> conflicts = model.conflictValues(value);
                if (conflicts.contains(value)) continue;
                if (conflicts.isEmpty()) {
                    SimpleNeighbour<V, T> n = new SimpleNeighbour<V, T>(variable, value);
                    if (!iHC || n.value() <= 0) return n;
                    else continue;
                }
                
                Map<V, T> assignments = new HashMap<V, T>();
                assignments.put(variable, value);
                
                for (T conflict: conflicts)
                    conflict.variable().unassign(solution.getIteration());
                variable.assign(solution.getIteration(), value);
                
                Double v = resolve(solution, total, startTime, assignments, new ArrayList<T>(conflicts), 0);
                if (!conflicts.isEmpty())
                    attempts ++;
                
                variable.unassign(solution.getIteration());
                for (T conflict: conflicts)
                    conflict.variable().assign(solution.getIteration(), conflict);
                if (old != null) variable.assign(solution.getIteration(), old);
                
                if (v != null)
                    return new SwapNeighbour(assignments.values(), v);
                
                if (attempts >= iMaxAttempts) break;
            }
        }
        return null;
    }
    
    /**
     * Return true if the time limit was reached, number of attempts are limited to 1 in such a case.
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
     * @param assignments re-assignments to be made
     * @param conflicts list of conflicts to resolve
     * @param index index in the list of conflicts
     * @return value of the modified solution, null if cannot be resolved
     */
    public Double resolve(Solution<V, T> solution, double total, long startTime, Map<V, T> assignments, List<T> conflicts, int index) {
        if (index == conflicts.size()) return solution.getModel().getTotalValue() - total;
        T conflict = conflicts.get(index);
        V variable = conflict.variable();
        
        List<T> values = variable.values();
        if (values.isEmpty()) return null;
        
        int valIdx = ToolBox.random(values.size());
        int attempts = 0;
        for (int i = 0; i < values.size(); i++) {
            T value = values.get((i + valIdx) % values.size());
            if (value.equals(conflict) || solution.getModel().inConflict(value)) continue;
            
            variable.assign(solution.getIteration(), value);
            Double v = resolve(solution, total, startTime, assignments, conflicts, 1 + index);
            variable.unassign(solution.getIteration());
            attempts ++;
            
            if (v != null && (!iHC || v <= 0)) {
                assignments.put(variable, value);
                return v;
            }
            if (attempts >= iMaxAttempts || isTimeLimitReached(startTime)) break;
        }
            
        return null;
    }
    
    public class SwapNeighbour extends Neighbour<V, T> {
        private double iValue = 0;
        private Collection<T> iAssignments = null;

        public SwapNeighbour(Collection<T> assignments, double value) {
            iAssignments = assignments; iValue = value;
        }

        @Override
        public double value() {
            return iValue;
        }

        @Override
        public void assign(long iteration) {
            for (T placement: iAssignments)
                if (placement.variable().getAssignment() != null)
                    placement.variable().unassign(iteration);
            for (T placement: iAssignments)
                placement.variable().assign(iteration, placement);
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
    }
}