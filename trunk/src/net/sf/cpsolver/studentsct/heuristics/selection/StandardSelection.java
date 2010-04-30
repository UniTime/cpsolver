package net.sf.cpsolver.studentsct.heuristics.selection;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.heuristics.ValueSelection;
import net.sf.cpsolver.ifs.heuristics.VariableSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.SimpleNeighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;

/**
 * Use the provided variable and value selection for some time. 
 * The provided variable and value selection is used for the 
 * number of iterations equal to the number of all 
 * variables in the problem. If a complete solution is found, 
 * the neighbour selection is stopped (it returns null).
 *  
 * <br><br>
 * Parameters:
 * <br>
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>Neighbour.StandardIterations</td><td>{@link Long}</td><td>Number of iterations to perform. If -1, number of iterations is set to the number of unassigned variables.</td></tr>
 * </table>
 * <br><br>
 * 
 * @version
 * StudentSct 1.1 (Student Sectioning)<br>
 * Copyright (C) 2007 Tomas Muller<br>
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
public class StandardSelection implements NeighbourSelection {
    private long iIteration = 0;
    private ValueSelection iValueSelection = null;
    private VariableSelection iVariableSelection = null;
    protected long iNrIterations = -1;
    
    /**
     * Constructor (variable and value selection are expected to be already initialized). 
     * @param properties configuration
     * @param variableSelection variable selection
     * @param valueSelection value selection
     */
    public StandardSelection(DataProperties properties, VariableSelection variableSelection, ValueSelection valueSelection) {
        iVariableSelection = variableSelection;
        iValueSelection = valueSelection;
    }
    
    /** Initialization */
    public void init(Solver solver) {
        iIteration = solver.currentSolution().getIteration();
        iNrIterations = solver.getProperties().getPropertyLong("Neighbour.StandardIterations", -1);
        if (iNrIterations>0)
            Progress.getInstance(solver.currentSolution().getModel()).setPhase("Ifs...", iNrIterations);
    }
    
    /**
     * Employ the provided {@link VariableSelection} and {@link ValueSelection} and return the 
     * selected value as {@link SimpleNeighbour}. The selection is stopped (null is returned) 
     * after the number of iterations equal to the number of variables in the problem
     * or when a complete solution is found.
     */
    public Neighbour selectNeighbour(Solution solution) {
        if (iNrIterations<0) {
            iNrIterations = solution.getModel().unassignedVariables().size();
            Progress.getInstance(solution.getModel()).setPhase("Ifs...", iNrIterations);
        }
        if (solution.getModel().unassignedVariables().isEmpty() || solution.getIteration()>=iIteration+iNrIterations) return null;
        Progress.getInstance(solution.getModel()).incProgress();
        for (int i=0;i<10;i++) {
            Request request = (Request)iVariableSelection.selectVariable(solution);
            Enrollment enrollment = (request==null?null:(Enrollment)iValueSelection.selectValue(solution, request));
            if (enrollment!=null && !enrollment.variable().getModel().conflictValues(enrollment).contains(enrollment))
                return new SimpleNeighbour(request, enrollment);
        }
        return null;
    }

}
