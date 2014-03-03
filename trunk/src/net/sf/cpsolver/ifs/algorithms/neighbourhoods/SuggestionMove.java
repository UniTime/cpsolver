package net.sf.cpsolver.ifs.algorithms.neighbourhoods;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.coursett.heuristics.NeighbourSelectionWithSuggestions;
import net.sf.cpsolver.ifs.algorithms.HillClimber;
import net.sf.cpsolver.ifs.constant.ConstantModel;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.JProf;
import net.sf.cpsolver.ifs.util.ToolBox;

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
        V variable = ToolBox.random(solution.getModel().variables());
        SwapNeighbour n = backtrack(solution, solution.getModel().getTotalValue(), solution.getModel().nrUnassignedVariables(), JProf.currentTimeMillis(), variable, new HashMap<V, T>(), new HashMap<V, T>(), iSuggestionDepth);
        return n;
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
        int nrUnassigned = conflictsToResolve.size();
        if (initial == null && nrUnassigned == 0) {
            if (solution.getModel().nrUnassignedVariables() > un) return null;
            double value = solution.getModel().getTotalValue() - total;
            if (!iHC || value <= 0)
                return new SwapNeighbour(new ArrayList<T>(resolvedVariables.values()), value);
            else
                return null;
        }
        if (depth <= 0) return null;

        V lecture = initial;
        if (lecture == null) {
            for (V l: conflictsToResolve.keySet()) {
                if (resolvedVariables.containsKey(l)) continue;
                lecture = l; break;
            }
            if (lecture == null) return null;
        } else {
            if (resolvedVariables.containsKey(lecture)) return null;
        }
        
        List<T> values = lecture.values();
        if (values.isEmpty()) return null;
        
        int idx = ToolBox.random(values.size());
        int nrAttempts = 0;
        values: for (int i = 0; i < values.size(); i++) {
            if (nrAttempts >= iMaxAttempts || isTimeLimitReached(startTime)) break;
            T value = values.get((i + idx) % values.size());
            
            if (value.equals(lecture.getAssignment())) continue;
            
            Set<T> conflicts = solution.getModel().conflictValues(value);
            if (nrUnassigned + conflicts.size() > depth) continue;
            if (conflicts.contains(value)) continue;
            if (containsCommited(solution, conflicts)) continue;
            for (T c: conflicts)
                if (resolvedVariables.containsKey(c.variable()))
                    continue values;
            
            T cur = lecture.getAssignment();
            for (T c: conflicts) c.variable().unassign(0);
            if (cur != null) lecture.unassign(0);
            
            lecture.assign(0, value);
            for (T c: conflicts)
                conflictsToResolve.put(c.variable(), c);

            T resolvedConf = conflictsToResolve.remove(lecture);
            resolvedVariables.put(lecture, value);
            
            SwapNeighbour n = backtrack(solution, total, un, startTime, null, resolvedVariables, conflictsToResolve, depth - 1);
            nrAttempts ++;
            
            resolvedVariables.remove(lecture);
            if (cur == null) lecture.unassign(0);
            else lecture.assign(0, cur);
            for (T c: conflicts) {
                c.variable().assign(0, c);
                conflictsToResolve.remove(c.variable());
            }
            if (resolvedConf != null)
                conflictsToResolve.put(lecture, resolvedConf);
            
            if (n != null) return n;
        }
        
        return null;
    }

}
