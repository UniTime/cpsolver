package net.sf.cpsolver.exam.heuristics;

import java.util.Enumeration;
import java.util.Vector;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.heuristics.VariableSelection;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Unassigned variable selection. The "biggest" variable (using {@link Variable#compareTo(Object)})
 * unassigned variable is selected. One is selected randomly if there are more than one of
 * such variables.
 *  
 * @version
 * ExamTT 1.1 (Examination Timetabling)<br>
 * Copyright (C) 2008 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
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
public class ExamUnassignedVariableSelection implements VariableSelection {
    private static Logger sLog = Logger.getLogger(ExamUnassignedVariableSelection.class);
    private Vector iVariables = null;
    private long iIteration = -1;
    private boolean iRandomSelection = true;
    
    /** Constructor */
    public ExamUnassignedVariableSelection(DataProperties properties) {
        iRandomSelection = properties.getPropertyBoolean("ExamUnassignedVariableSelection.random", iRandomSelection);
    }
    
    /** Initialization */
    public void init(Solver solver) {}
    
    /** Variable selection */
    public Variable selectVariable(Solution solution) {
        Model model = solution.getModel();
        if (model.nrUnassignedVariables()==0) return null;
        if (iRandomSelection) return (Variable)ToolBox.random(model.unassignedVariables());
        Variable variable = null;
        for (Enumeration e=model.unassignedVariables().elements();e.hasMoreElements();) {
            Variable v = (Variable)e.nextElement();
            if (variable==null || v.compareTo(variable)<0) variable = v;
        }
        return variable;
    }
}
