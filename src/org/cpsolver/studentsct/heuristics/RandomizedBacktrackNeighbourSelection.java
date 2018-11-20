package org.cpsolver.studentsct.heuristics;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
 * <table border='1' summary='Related Solver Parameters'>
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
                    if (e1.equals(assignment.getValue(request))) return -1;
                    if (e2.equals(assignment.getValue(request))) return 1;
                    Double v1 = value(e1), v2 = value(e2);
                    return v1.equals(v2) ? e1.compareTo(assignment, e2) : v2.compareTo(v1);
                }
            });
            return values.iterator();
        } else {
            return variable.computeEnrollments(context.getAssignment()).iterator();
        }
    }
}
