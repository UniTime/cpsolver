package org.cpsolver.exam.heuristics;

import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamModel;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.VariableSelection;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;

/**
 * Unassigned variable selection. The "biggest" variable (using
 * {@link Variable#compareTo(Variable)}) unassigned variable is selected. One is
 * selected randomly if there are more than one of such variables.
 * 
 * @author  Tomas Muller
 * @version ExamTT 1.3 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2014 Tomas Muller<br>
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
public class ExamUnassignedVariableSelection implements VariableSelection<Exam, ExamPlacement> {
    private boolean iRandomSelection = true;

    /** Constructor 
     * @param properties solver configuration
     **/
    public ExamUnassignedVariableSelection(DataProperties properties) {
        iRandomSelection = properties.getPropertyBoolean("ExamUnassignedVariableSelection.random", iRandomSelection);
    }

    /** Initialization */
    @Override
    public void init(Solver<Exam, ExamPlacement> solver) {
    }

    /** Variable selection */
    @Override
    public Exam selectVariable(Solution<Exam, ExamPlacement> solution) {
        ExamModel model = (ExamModel) solution.getModel();
        Assignment<Exam, ExamPlacement> assignment = solution.getAssignment();
        if (model.variables().size() == assignment.nrAssignedVariables())
            return null;
        if (iRandomSelection) {
            int idx = ToolBox.random(model.variables().size() - assignment.nrAssignedVariables());
            for (Exam v: model.variables()) {
                if (assignment.getValue(v) != null) continue;
                if (idx == 0) return v;
                idx --;
            }
        }
        Exam variable = null;
        for (Exam v : model.variables()) {
            if (assignment.getValue(v) != null) continue;
            if (variable == null || v.compareTo(variable) < 0)
                variable = v;
        }
        return variable;
    }
}
