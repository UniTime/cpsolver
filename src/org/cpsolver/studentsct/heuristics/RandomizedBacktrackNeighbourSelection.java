package org.cpsolver.studentsct.heuristics;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.BacktrackNeighbourSelection;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;


/**
 * Randomized backtracking-based neighbour selection. This class extends
 * {@link RandomizedBacktrackNeighbourSelection}, however, only a randomly
 * selected subset of enrollments of each request is considered (
 * {@link CourseRequest#computeRandomEnrollments(Assignment, int)} with the given limit is
 * used).
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
 * <td>Neighbour.MaxValues</td>
 * <td>{@link Integer}</td>
 * <td>Limit on the number of enrollments to be visited of each
 * {@link CourseRequest}.</td>
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
public class RandomizedBacktrackNeighbourSelection extends BacktrackNeighbourSelection<Request, Enrollment> {
    private int iMaxValues = 100;
    private boolean iPreferPriorityStudents = true;

    /**
     * Constructor
     * 
     * @param properties
     *            configuration
     * @throws Exception thrown when the initialization fails
     */
    public RandomizedBacktrackNeighbourSelection(DataProperties properties) throws Exception {
        super(properties);
        iMaxValues = properties.getPropertyInt("Neighbour.MaxValues", iMaxValues);
        iPreferPriorityStudents = properties.getPropertyBoolean("Sectioning.PriorityStudentsFirstSelection.AllIn", true);
    }

    /**
     * List of values of a variable.
     * {@link CourseRequest#computeRandomEnrollments(Assignment, int)} with the provided
     * limit is used for a {@link CourseRequest}.
     */
    @Override
    protected Iterator<Enrollment> values(BacktrackNeighbourSelection<Request, Enrollment>.BacktrackNeighbourSelectionContext context, Request variable) {
        if (variable instanceof CourseRequest) {
            final CourseRequest request = (CourseRequest)variable;
            final StudentSectioningModel model = (StudentSectioningModel)context.getModel();
            final Assignment<Request, Enrollment> assignment = context.getAssignment();
            final Enrollment current = assignment.getValue(request);
            List<Enrollment> values = (iMaxValues > 0 ? request.computeRandomEnrollments(assignment, iMaxValues) : request.computeEnrollments(assignment));
            Collections.sort(values, new Comparator<Enrollment>() {
                private HashMap<Enrollment, Double> iValues = new HashMap<Enrollment, Double>();
                private Double value(Enrollment e) {
                    Double value = iValues.get(e);
                    if (value == null) {
                        if (model.getStudentQuality() != null)
                            value = model.getStudentWeights().getWeight(assignment, e, model.getStudentQuality().conflicts(e));
                        else
                            value = model.getStudentWeights().getWeight(assignment, e,
                                    (model.getDistanceConflict() == null ? null : model.getDistanceConflict().conflicts(e)),
                                    (model.getTimeOverlaps() == null ? null : model.getTimeOverlaps().conflicts(e)));
                        iValues.put(e, value);
                    }
                    return value;
                }
                @Override
                public int compare(Enrollment e1, Enrollment e2) {
                    if (e1.equals(e2)) return 0;
                    if (e1.equals(current)) return -1;
                    if (e2.equals(current)) return 1;
                    Double v1 = value(e1), v2 = value(e2);
                    return v1.equals(v2) ? e1.compareTo(assignment, e2) : v2.compareTo(v1);
                }
            });
            return values.iterator();
        } else {
            return variable.computeEnrollments(context.getAssignment()).iterator();
        }
    }
    
    /**
     * Check if the given conflicting enrollment can be unassigned
     * @param conflict given enrollment
     * @return if running MPP, do not unassign initial enrollments
     */
    public boolean canUnassign(Enrollment enrollment, Enrollment conflict, Assignment<Request, Enrollment> assignment) {
        if (conflict.getRequest().isMPP() && conflict.equals(conflict.getRequest().getInitialAssignment()) && 
                !enrollment.equals(enrollment.getRequest().getInitialAssignment())) return false;
        if (conflict.getRequest() instanceof CourseRequest && ((CourseRequest)conflict.getRequest()).getFixedValue() != null) return false;
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
    
    @Override
    protected boolean checkBound(List<Request> variables2resolve, int idx, int depth, Enrollment value, Set<Enrollment> conflicts) {
        for (Enrollment conflict: conflicts)
            if (!canUnassign(value, conflict, getContext().getAssignment())) return false;
        return super.checkBound(variables2resolve, idx, depth, value, conflicts);
    }
}
