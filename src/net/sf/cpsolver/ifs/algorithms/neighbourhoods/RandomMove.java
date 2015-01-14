package net.sf.cpsolver.ifs.algorithms.neighbourhoods;

import java.util.List;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.SimpleNeighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Try to assign a variable with a new value. A variable is selected randomly, a
 * different (available) value is randomly selected for the variable -- the variable is
 * assigned with the new value if there is no conflict.
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
        int varIdx = ToolBox.random(model.variables().size());
        for (int i = 0; i < model.variables().size(); i++) {
            V variable = model.variables().get((i + varIdx) % model.variables().size());
            List<T> values = variable.values();
            if (values.isEmpty()) continue;
            int valIdx = ToolBox.random(values.size());
            for (int j = 0; j < values.size(); j++) {
                T value = values.get((j + valIdx) % values.size());
                if (!model.inConflict(value)) {
                    SimpleNeighbour<V, T> n = new SimpleNeighbour<V, T>(variable, value);
                    if (!iHC || n.value() <= 0) return n;
                }
            }
        }
        return null;
    }
}
