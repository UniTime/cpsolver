package org.cpsolver.studentsct.heuristics.selection;

import java.util.ConcurrentModificationException;
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
import org.cpsolver.studentsct.filter.StudentFilter;
import org.cpsolver.studentsct.heuristics.AssignmentCheck;
import org.cpsolver.studentsct.heuristics.EnrollmentSelection;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Request.RequestPriority;


/**
 * Use the provided variable and value selection for some time. The provided
 * variable and value selection is used for the number of iterations equal to
 * the number of all variables in the problem. If a complete solution is found,
 * the neighbour selection is stopped (it returns null).
 * 
 * <br>
 * <br>
 * Parameters: <br>
 * <table border='1'><caption>Related Solver Parameters</caption>
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
 * @author  Tomas Muller
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
public class StandardSelection implements NeighbourSelection<Request, Enrollment>, AssignmentCheck<Request, Enrollment> {
    private long iIteration = 0;
    protected ValueSelection<Request, Enrollment> iValueSelection = null;
    protected VariableSelection<Request, Enrollment> iVariableSelection = null;
    protected long iNrIterations = -1;
    protected boolean iPreferPriorityStudents = true;
    protected long iConflictTimeOut = -7200;
    protected long iWorsenTimeOut = -7200;
    protected long iTimeOut = -3600;
    protected boolean iCanConflict = true;
    protected boolean iCanWorsen = true;
    protected boolean iCanWorsenCritical = false;
    protected boolean iCanHigherPriorityConflict = true;

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
        iTimeOut = properties.getPropertyLong("Neighbour.StandardTimeOut", 0);
        if (iTimeOut < 0)
            iTimeOut = Math.max(0, properties.getPropertyLong("Termination.TimeOut", -1l) + iTimeOut);
        iConflictTimeOut = properties.getPropertyLong("Neighbour.StandardConflictTimeOut", - properties.getPropertyLong("Termination.TimeOut", 0) / 10);
        if (iConflictTimeOut < 0)
            iConflictTimeOut = Math.max(0, properties.getPropertyLong("Termination.TimeOut", -1l) + iConflictTimeOut);
        iWorsenTimeOut = properties.getPropertyLong("Neighbour.StandardWorsenTimeOut", - 3 * properties.getPropertyLong("Termination.TimeOut", 0) / 10);
        if (iWorsenTimeOut < 0)
            iWorsenTimeOut = Math.max(0, properties.getPropertyLong("Termination.TimeOut", -1l) + iWorsenTimeOut);
        iCanConflict = properties.getPropertyBoolean("Neighbour.StandardCanConflict", true);
        iCanWorsen = properties.getPropertyBoolean("Neighbour.StandardCanWorsen", true);
        iCanHigherPriorityConflict = properties.getPropertyBoolean("Neighbour.StandardCanHigherPriorityConflict", true);
        iCanWorsenCritical = properties.getPropertyBoolean("Neighbour.CriticalStandardCanWorsen", false);
    }

    /** Initialization */
    @Override
    public void init(Solver<Request, Enrollment> solver) {
        StudentFilter filter = null;
        if (iVariableSelection instanceof UnassignedRequestSelection)
            filter = ((UnassignedRequestSelection)iVariableSelection).getFilter();
        init(solver, "Ifs" + (filter == null ? "" : " (" + filter.getName().toLowerCase() + " students)") + "...");
    }
    
    protected void init(Solver<Request, Enrollment> solver, String name) {
        iVariableSelection.init(solver);
        iValueSelection.init(solver);
        iIteration = 0;
        iNrIterations = solver.getProperties().getPropertyLong("Neighbour.StandardIterations", -1);
        if (iNrIterations < 0)
            iNrIterations = solver.currentSolution().getModel().nrUnassignedVariables(solver.currentSolution().getAssignment());
        if (iTimeOut > 0 && solver.currentSolution().getTime() > iTimeOut)
            iNrIterations = 0;
        iCanConflict = solver.getProperties().getPropertyBoolean("Neighbour.StandardCanConflict", true);
        if (iCanConflict && iConflictTimeOut > 0 && solver.currentSolution().getTime() > iConflictTimeOut)
            iCanConflict = false;
        if (iCanWorsen && iWorsenTimeOut > 0 && solver.currentSolution().getTime() > iWorsenTimeOut)
            iCanWorsen = false;
        if (!iCanConflict)
            name = "No-Conf " + name;
        else if (!iCanWorsen)
            name = "Improving " + name;
        if (iNrIterations > 0)
            Progress.getInstance(solver.currentSolution().getModel()).setPhase(name, iNrIterations);
    }
    
    /**
     * Check if the given conflicting enrollment can be unassigned
     * @param conflict given enrollment
     * @return if running MPP, do not unassign initial enrollments
     */
    @Override
    public boolean canUnassign(Enrollment enrollment, Enrollment conflict, Assignment<Request, Enrollment> assignment) {
        if (!iCanConflict) return false;
        if (!iCanHigherPriorityConflict && conflict.getRequest().getPriority() < enrollment.getRequest().getPriority()) return false;
        if (conflict.getRequest().isMPP() && conflict.equals(conflict.getRequest().getInitialAssignment())) return false;
        if (conflict.getRequest().getStudent().hasMinCredit()) {
            float credit = conflict.getRequest().getStudent().getAssignedCredit(assignment) - conflict.getCredit();
            if (credit < conflict.getRequest().getStudent().getMinCredit()) return false;
        }
        if (!conflict.getRequest().isAlternative() && conflict.getRequest().getRequestPriority().isHigher(enrollment.getRequest())) return false;
        if (iPreferPriorityStudents || conflict.getRequest().getRequestPriority().isSame(enrollment.getRequest())) {
            if (conflict.getStudent().getPriority().isHigher(enrollment.getStudent())) return false;
        }
        return true;
    }
    
    /**
     * Check whether the given neighbors can be returned
     * @return by default, any neighbors is accepted
     */
    public boolean accept(SimpleNeighbour<Request, Enrollment> n, Solution<Request, Enrollment> solution) {
        if (!iCanWorsenCritical && RequestPriority.Important.isCritical(n.getVariable()))
            return n.value(solution.getAssignment()) <= 0.0;
        if (iCanWorsen) return true;
        return n.value(solution.getAssignment()) <= 0.0;
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
            try {
                Request request = iVariableSelection.selectVariable(solution);
                if (request == null) continue;
                Enrollment enrollment =
                        (iValueSelection instanceof EnrollmentSelection)
                                ? ((EnrollmentSelection)iValueSelection).selectValue(solution, request, this)
                                : iValueSelection.selectValue(solution, request);
                if (enrollment == null) continue;
                Set<Enrollment> conflicts = enrollment.variable().getModel().conflictValues(solution.getAssignment(), enrollment);
                if (conflicts.contains(enrollment)) continue;
                for (Enrollment conflict: conflicts)
                    if (!canUnassign(enrollment, conflict, solution.getAssignment())) continue attempts;
                SimpleNeighbour<Request, Enrollment> n = new SimpleNeighbour<Request, Enrollment>(request, enrollment, conflicts);
                if (accept(n, solution)) return n;
            } catch (ConcurrentModificationException e) {}
        }
        return null;
    }

}
