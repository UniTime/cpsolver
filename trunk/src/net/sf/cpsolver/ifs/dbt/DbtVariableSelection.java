package net.sf.cpsolver.ifs.dbt;

import java.util.*;

import net.sf.cpsolver.ifs.extension.*;
import net.sf.cpsolver.ifs.heuristics.*;
import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.solution.*;
import net.sf.cpsolver.ifs.solver.*;
import net.sf.cpsolver.ifs.util.*;

/**
 * Selection of a variable for dynamic backtracking.
 * <br><br>
 * <li> Returns null if all variables are assigned.
 * <li> Checks if there is a varaible with all values marked as nogood (and pick it if there is any).
 * <li> Returns the first unassigned variable.
 * <br><br>
 * This IFS solver variable selection heuristics is to be used only in case of dynamic backtracking and it has no parameters.
 *
 * 
 * @version
 * IFS 1.0 (Iterative Forward Search)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@ktiml.mff.cuni.cz">muller@ktiml.mff.cuni.cz</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
public class DbtVariableSelection implements VariableSelection {
    private DbtPropagation iProp = null;

    public DbtVariableSelection(DataProperties properties) {}

    /** 
     * Heuristics initialization
     *
     * @see VariableSelection#init(Solver)
     */
    public void init(Solver solver) {
        for (Enumeration i = solver.getExtensions().elements(); i.hasMoreElements();) {
            Extension extension = (Extension) i.nextElement();

            if (extension instanceof DbtPropagation) {
                iProp = (DbtPropagation) extension;
            }
        }
    }
    
    /** 
     * Variable selection 
     *
     * @see VariableSelection#selectVariable(Solution)
     */
    public Variable selectVariable(Solution solution) {
        if (solution.getModel().unassignedVariables().isEmpty()) {
            return null;
        }
        if (iProp != null) { 
            for (Enumeration i1 = solution.getModel().unassignedVariables().elements(); i1.hasMoreElements();) {
                Variable variable = (Variable) i1.nextElement();

                if (iProp.goodValues(variable).isEmpty()) {
                    return variable;
                }
            }
        }
        return (Variable) solution.getModel().unassignedVariables().firstElement();
    }
    
}
