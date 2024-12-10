package org.cpsolver.ifs.algorithms.neighbourhoods;

import java.util.List;

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
import org.cpsolver.ifs.util.ToolBox;


/**
 * Try to assign a variable with a new value. A variable is selected randomly, a
 * different (available) value is randomly selected for the variable -- the variable is
 * assigned with the new value if there is no conflict.
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
public class RandomMove<V extends Variable<V, T>, T extends Value<V, T>> implements NeighbourSelection<V, T>, HillClimberSelection {
    protected boolean iHC = false;
    
    public RandomMove(DataProperties config) {
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
        int varIdx = ToolBox.random(model.variables().size());
        for (int i = 0; i < model.variables().size(); i++) {
            V variable = model.variables().get((i + varIdx) % model.variables().size());
            List<T> values = variable.values(solution.getAssignment());
            if (values.isEmpty()) continue;
            int valIdx = ToolBox.random(values.size());
            for (int j = 0; j < values.size(); j++) {
                T value = values.get((j + valIdx) % values.size());
                if (!model.inConflict(assignment, value)) {
                    SimpleNeighbour<V, T> n = new SimpleNeighbour<V, T>(variable, value);
                    if (!iHC || n.value(assignment) <= 0) return n;
                }
            }
        }
        return null;
    }
}
