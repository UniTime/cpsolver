package org.cpsolver.ifs.algorithms.neighbourhoods;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.cpsolver.coursett.heuristics.NeighbourSelectionWithSuggestions;
import org.cpsolver.ifs.algorithms.HillClimber;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.constant.ConstantModel;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.JProf;
import org.cpsolver.ifs.util.ToolBox;


/**
 * Suggestion move. A variable is selected randomly. A limited depth backtracking
 * is used to find a possible change (much like the suggestions in UniTime when
 * the interactive solver is used). Unlike in {@link NeighbourSelectionWithSuggestions},
 * the very first found suggestion is returned. The depth is limited by 
 * SuggestionMove.Depth parameter (defaults to 3). Also, instead of using a time limit, 
 * the width of the search is limited by the number of values that are tried for each
 * variable (parameter SuggestionMove.MaxAttempts, defaults to 10). When used in
 * {@link HillClimber}, the first suggestion that does not worsen the solution is returned.
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
public class SuggestionMove<V extends Variable<V, T>, T extends Value<V, T>> extends RandomSwapMove<V, T> {
    protected int iSuggestionDepth = 3;
    protected int iTimeLimit = 200;
    
    public SuggestionMove(DataProperties config) throws Exception {
        super(config);
        iMaxAttempts = config.getPropertyInt("SuggestionMove.MaxAttempts", iMaxAttempts);
        iSuggestionDepth = config.getPropertyInt("SuggestionMove.Depth", iSuggestionDepth);
        iTimeLimit = config.getPropertyInt("SuggestionMove.TimeLimit", iTimeLimit);
    }

    @Override
    public void init(Solver<V, T> solver) {
        super.init(solver);
    }
    
    @Override
    public Neighbour<V, T> selectNeighbour(Solution<V, T> solution) {
        Lock lock = solution.getLock().writeLock();
        lock.lock();
        try {
            V variable = null;
            if (solution.getModel().nrUnassignedVariables(solution.getAssignment()) > 0)
                variable = ToolBox.random(solution.getModel().unassignedVariables(solution.getAssignment()));
            else
                variable = ToolBox.random(solution.getModel().variables());
            return backtrack(
                    solution, solution.getModel().getTotalValue(solution.getAssignment()),
                    solution.getModel().nrUnassignedVariables(solution.getAssignment()),
                    JProf.currentTimeMillis(), variable, new HashMap<V, T>(), new HashMap<V, T>(), iSuggestionDepth);
        } finally {
            lock.unlock();
        }
    }
    
    private boolean containsCommited(Solution<V, T> solution, Collection<T> values) {
        if (solution.getModel() instanceof ConstantModel) {
            ConstantModel<V, T> model = (ConstantModel<V, T>)solution.getModel();
            if (model.hasConstantVariables())
                for (T value: values)
                    if (model.isConstant(value.variable())) return true;
        }
        return false;
    }

    private SwapNeighbour backtrack(Solution<V, T> solution, double total, int un, long startTime, V initial, Map<V, T> resolvedVariables, HashMap<V, T> conflictsToResolve, int depth) {
        Model<V, T> model = solution.getModel();
        Assignment<V, T> assignment = solution.getAssignment();
        int nrUnassigned = conflictsToResolve.size();
        if (initial == null && nrUnassigned == 0) {
            if (model.nrUnassignedVariables(assignment) > un) return null;
            double value = model.getTotalValue(assignment) - total;
            if (!iHC || value <= 0)
                return new SwapNeighbour(new ArrayList<T>(resolvedVariables.values()),
                        un > model.nrUnassignedVariables(assignment) ? -1 : value);
            else
                return null;
        }
        if (depth <= 0) return null;

        V variable = initial;
        if (variable == null) {
            for (V l: conflictsToResolve.keySet()) {
                if (resolvedVariables.containsKey(l)) continue;
                variable = l; break;
            }
            if (variable == null) return null;
        } else {
            if (resolvedVariables.containsKey(variable)) return null;
        }
        
        List<T> values = variable.values(solution.getAssignment());
        if (values.isEmpty()) return null;
        
        int idx = ToolBox.random(values.size());
        int nrAttempts = 0;
        values: for (int i = 0; i < values.size(); i++) {
            if (nrAttempts >= iMaxAttempts || isTimeLimitReached(startTime)) break;
            T value = values.get((i + idx) % values.size());

            T cur = assignment.getValue(variable);
            if (value.equals(cur)) continue;
            
            Set<T> conflicts = model.conflictValues(assignment, value);
            if (nrUnassigned + conflicts.size() > depth) continue;
            if (conflicts.contains(value)) continue;
            if (containsCommited(solution, conflicts)) continue;
            for (T c: conflicts)
                if (resolvedVariables.containsKey(c.variable()))
                    continue values;
            
            for (T c: conflicts) assignment.unassign(solution.getIteration(), c.variable());
            if (cur != null) assignment.unassign(solution.getIteration(), variable);
            
            assignment.assign(solution.getIteration(), value);
            for (T c: conflicts)
                conflictsToResolve.put(c.variable(), c);

            T resolvedConf = conflictsToResolve.remove(variable);
            resolvedVariables.put(variable, value);
            
            SwapNeighbour n = backtrack(solution, total, un, startTime, null, resolvedVariables, conflictsToResolve, depth - 1);
            nrAttempts ++;
            
            resolvedVariables.remove(variable);
            if (cur == null) assignment.unassign(solution.getIteration(), variable);
            else assignment.assign(solution.getIteration(), cur);
            for (T c: conflicts) {
                assignment.assign(solution.getIteration(), c);
                conflictsToResolve.remove(c.variable());
            }
            if (resolvedConf != null)
                conflictsToResolve.put(variable, resolvedConf);
            
            if (n != null) return n;
        }
        
        return null;
    }

}
