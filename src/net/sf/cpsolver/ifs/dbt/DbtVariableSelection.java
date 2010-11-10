package net.sf.cpsolver.ifs.dbt;

import net.sf.cpsolver.ifs.extension.*;
import net.sf.cpsolver.ifs.heuristics.*;
import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.solution.*;
import net.sf.cpsolver.ifs.solver.*;
import net.sf.cpsolver.ifs.util.*;

/**
 * Selection of a variable for dynamic backtracking. <br>
 * <br>
 * <li>Returns null if all variables are assigned. <li>Checks if there is a
 * varaible with all values marked as nogood (and pick it if there is any). <li>
 * Returns the first unassigned variable. <br>
 * <br>
 * This IFS solver variable selection heuristics is to be used only in case of
 * dynamic backtracking and it has no parameters.
 * 
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
public class DbtVariableSelection<V extends Variable<V, T>, T extends Value<V, T>> implements VariableSelection<V, T> {
    private DbtPropagation<V, T> iProp = null;

    public DbtVariableSelection(DataProperties properties) {
    }

    /**
     * Heuristics initialization
     * 
     * @see VariableSelection#init(Solver)
     */
    public void init(Solver<V, T> solver) {
        for (Extension<V, T> extension : solver.getExtensions()) {
            if (extension instanceof DbtPropagation<?, ?>) {
                iProp = (DbtPropagation<V, T>) extension;
            }
        }
    }

    /**
     * Variable selection
     * 
     * @see VariableSelection#selectVariable(Solution)
     */
    public V selectVariable(Solution<V, T> solution) {
        if (solution.getModel().nrUnassignedVariables() == 0) {
            return null;
        }
        if (iProp != null) {
            for (V variable : solution.getModel().unassignedVariables()) {
                if (iProp.goodValues(variable).isEmpty()) {
                    return variable;
                }
            }
        }
        return solution.getModel().unassignedVariables().iterator().next();
    }

}
