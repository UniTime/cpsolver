package org.cpsolver.ifs.algorithms;

import java.util.Collection;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentContext;
import org.cpsolver.ifs.assignment.context.NeighbourSelectionWithContext;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.SimpleNeighbour;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solution.SolutionListener;
import org.cpsolver.ifs.solver.ParallelSolver;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;


/**
 * A simple neighbourhood selection extension suitable for the {@link ParallelSolver}
 * during the construction phase. If the best ever found solution was found by a different
 * thread ({@link Assignment#getIndex()} does not match) and the current solution has
 * a smaller number of variables assigned for at least the given number of iterations
 * (parameter ParallelConstruction.MaxIdle, defaults to 100), start assigning unassigned variables
 * to their best values. Otherwise, pass the selection to the provided neighbourhood selection.
 *
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
public class ParallelConstruction<V extends Variable<V, T>, T extends Value<V, T>> extends NeighbourSelectionWithContext<V, T, ParallelConstruction<V, T>.ConstructionContext> implements SolutionListener<V, T> {
    protected static Logger sLog = org.apache.logging.log4j.LogManager.getLogger(ParallelConstruction.class);
    protected NeighbourSelection<V, T> iParent = null;
    protected Double iBestValue = null;
    protected int iBestIndex = -1, iBestAssigned = 0;
    protected int iMaxIdle = 100;
    
    public ParallelConstruction(DataProperties config, NeighbourSelection<V, T> parent) {
        iParent = parent;
        iMaxIdle = config.getPropertyInt("ParallelConstruction.MaxIdle", iMaxIdle);
    }

    @Override
    public void init(Solver<V, T> solver) {
        super.init(solver);
        iParent.init(solver);
        solver.currentSolution().addSolutionListener(this);
    }


    @Override
    public void solutionUpdated(Solution<V, T> solution) {
    }

    @Override
    public void getInfo(Solution<V, T> solution, Map<String, String> info) {
    }

    @Override
    public void getInfo(Solution<V, T> solution, Map<String, String> info, Collection<V> variables) {
    }

    @Override
    public void bestCleared(Solution<V, T> solution) {
    }

    @Override
    public void bestSaved(Solution<V, T> solution) {
        iBestValue = solution.getBestValue();
        iBestIndex = solution.getAssignment().getIndex();
        iBestAssigned = solution.getAssignment().nrAssignedVariables();
        getContext(solution.getAssignment()).reset();
    }

    @Override
    public void bestRestored(Solution<V, T> solution) {
    }

    @Override
    public Neighbour<V, T> selectNeighbour(Solution<V, T> solution) {
        if (iBestValue != null && iBestIndex != solution.getAssignment().getIndex()) {
            ConstructionContext context = getContext(solution.getAssignment());
            if (solution.getAssignment().nrAssignedVariables() == iBestAssigned) {
                context.reset();
            } else if (solution.getAssignment().nrAssignedVariables() < iBestAssigned && context.inc() >= iMaxIdle) {
                Model<V, T> model = solution.getModel();
                Assignment<V, T> assignment = solution.getAssignment();
                int idx = ToolBox.random(model.countVariables());
                for (int i = 0; i < model.countVariables(); i++) {
                    V variable = model.variables().get((i + idx) % model.countVariables());
                    T value = assignment.getValue(variable);
                    T best = variable.getBestAssignment();
                    if (value == null && best != null)
                        return new SimpleNeighbour<V, T>(variable, best, model.conflictValues(solution.getAssignment(), best));
                }
            }
        }
        return iParent.selectNeighbour(solution);
    }

    @Override
    public ConstructionContext createAssignmentContext(Assignment<V, T> assignment) {
        return new ConstructionContext(assignment);
    }

    public class ConstructionContext implements AssignmentContext {
        private int iCounter = 0;
        
        public ConstructionContext(Assignment<V, T> assignment) {
        }
        
        public int inc() { return iCounter++; }
        public void reset() { iCounter = 0; }
        
    }

}
