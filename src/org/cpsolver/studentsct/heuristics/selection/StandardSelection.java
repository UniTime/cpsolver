package org.cpsolver.studentsct.heuristics.selection;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.heuristics.ValueSelection;
import org.cpsolver.ifs.heuristics.VariableSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.SimpleNeighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;


/**
 * Use the provided variable and value selection for some time. The provided
 * variable and value selection is used for the number of iterations equal to
 * the number of all variables in the problem. If a complete solution is found,
 * the neighbour selection is stopped (it returns null).
 * 
 * <br>
 * <br>
 * Parameters: <br>
 * <table border='1' summary='Related Solver Parameters'>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>Neighbour.StandardIterations</td>
 * <td>{@link Long}</td>
 * <td>Number of iterations to perform. If -1, number of iterations is set to
 * the number of unassigned variables.</td>
 * </tr>
 * </table>
 * <br>
 * <br>
 * 
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2014 Tomas Muller<br>
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
public class StandardSelection implements NeighbourSelection<Request, Enrollment> {
    private long iIteration = 0;
    private ValueSelection<Request, Enrollment> iValueSelection = null;
    private VariableSelection<Request, Enrollment> iVariableSelection = null;
    protected long iNrIterations = -1;
    private boolean iPreferPriorityStudents = true;

    /**
     * Constructor (variable and value selection are expected to be already
     * initialized).
     * 
     * @param properties
     *            configuration
     * @param variableSelection
     *            variable selection
     * @param valueSelection
     *            value selection
     */
    public StandardSelection(DataProperties properties, VariableSelection<Request, Enrollment> variableSelection,
            ValueSelection<Request, Enrollment> valueSelection) {
        iVariableSelection = variableSelection;
        iValueSelection = valueSelection;
        iPreferPriorityStudents = properties.getPropertyBoolean("Sectioning.PriorityStudentsFirstSelection.AllIn", true);
    }

    /** Initialization */
    @Override
    public void init(Solver<Request, Enrollment> solver) {
        init(solver, "Ifs...");
    }
    
    protected void init(Solver<Request, Enrollment> solver, String name) {
        iVariableSelection.init(solver);
        iValueSelection.init(solver);
        iIteration = 0;
        iNrIterations = solver.getProperties().getPropertyLong("Neighbour.StandardIterations", -1);
        if (iNrIterations < 0)
            iNrIterations = solver.currentSolution().getModel().nrUnassignedVariables(solver.currentSolution().getAssignment());
        Progress.getInstance(solver.currentSolution().getModel()).setPhase(name, iNrIterations);
    }
    
    /**
     * Check if the given conflicting enrollment can be unassigned
     * @param conflict given enrollment
     * @return if running MPP, do not unassign initial enrollments
     */
    public boolean canUnassign(Enrollment enrollment, Enrollment conflict, Assignment<Request, Enrollment> assignment) {
        if (conflict.getRequest().isMPP() && conflict.equals(conflict.getRequest().getInitialAssignment())) return false;
        if (conflict.getRequest().getStudent().hasMinCredit()) {
            float credit = conflict.getRequest().getStudent().getAssignedCredit(assignment) - conflict.getCredit();
            if (credit < conflict.getRequest().getStudent().getMinCredit()) return false;
        }
        if (!conflict.getRequest().isAlternative() && conflict.getRequest().getRequestPriority().isHigher(enrollment.getRequest())) return false;
        if (iPreferPriorityStudents || conflict.getRequest().getRequestPriority().isSame(enrollment.getRequest())) {
            if (!enrollment.getStudent().isPriority() && conflict.getStudent().isPriority()) return false;
        }
        return true;
    }

    /**
     * Employ the provided {@link VariableSelection} and {@link ValueSelection}
     * and return the selected value as {@link SimpleNeighbour}. The selection
     * is stopped (null is returned) after the number of iterations equal to the
     * number of variables in the problem or when a complete solution is found.
     */
    @Override
    public Neighbour<Request, Enrollment> selectNeighbour(Solution<Request, Enrollment> solution) {
        if (solution.getModel().unassignedVariables(solution.getAssignment()).isEmpty() || ++iIteration >= iNrIterations)
            return null;
        Progress.getInstance(solution.getModel()).incProgress();
        attempts: for (int i = 0; i < 10; i++) {
            Request request = iVariableSelection.selectVariable(solution);
            if (request == null) continue;
            Enrollment enrollment = iValueSelection.selectValue(solution, request);
            if (enrollment == null) continue;
            Set<Enrollment> conflicts = enrollment.variable().getModel().conflictValues(solution.getAssignment(), enrollment);
            if (conflicts.contains(enrollment)) continue;
            for (Enrollment conflict: conflicts)
                if (!canUnassign(enrollment, conflict, solution.getAssignment())) continue attempts;
            return new SimpleNeighbour<Request, Enrollment>(request, enrollment, conflicts);
        }
        return null;
    }

}
