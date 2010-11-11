package net.sf.cpsolver.studentsct.heuristics;

import java.util.ArrayList;
import java.util.Iterator;

import net.sf.cpsolver.ifs.heuristics.BacktrackNeighbourSelection;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;

/**
 * Randomized backtracking-based neighbour selection. This class extends
 * {@link RandomizedBacktrackNeighbourSelection}, however, only a randomly
 * selected subset of enrollments of each request is considered (
 * {@link CourseRequest#computeRandomEnrollments(int)} with the given limit is
 * used).
 * 
 * <br>
 * <br>
 * Parameters: <br>
 * <table border='1'>
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
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
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
     * @throws Exception
     */
    public RandomizedBacktrackNeighbourSelection(DataProperties properties) throws Exception {
        super(properties);
        iMaxValues = properties.getPropertyInt("Neighbour.MaxValues", iMaxValues);
    }

    /**
     * List of values of a variable.
     * {@link CourseRequest#computeRandomEnrollments(int)} with the provided
     * limit is used for a {@link CourseRequest}.
     */
    @Override
    protected Iterator<Enrollment> values(Request variable) {
        if (iMaxValues > 0 && variable instanceof CourseRequest) {
            return new ArrayList<Enrollment>(((CourseRequest) variable).computeRandomEnrollments(iMaxValues))
                    .iterator();
        }
        return variable.values().iterator();
    }
}
