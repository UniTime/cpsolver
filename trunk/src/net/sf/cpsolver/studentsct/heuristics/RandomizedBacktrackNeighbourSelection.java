package net.sf.cpsolver.studentsct.heuristics;

import java.util.Enumeration;

import net.sf.cpsolver.ifs.heuristics.BacktrackNeighbourSelection;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.model.CourseRequest;

/**
 * Randomized backtracking-based neighbour selection. 
 * This class extends {@link RandomizedBacktrackNeighbourSelection}, however,
 * only a randomly selected subset of enrollments of each request is considered
 * ({@link CourseRequest#computeRandomEnrollments(int)} with the given limit is used).
 * 
 * <br><br>
 * Parameters:
 * <br>
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>Neighbour.MaxValues</td><td>{@link Integer}</td><td>Limit on the number of enrollments to be visited of each {@link CourseRequest}.</td></tr>
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
public class RandomizedBacktrackNeighbourSelection extends BacktrackNeighbourSelection {
    private int iMaxValues = 100;
    
    /**
     * Constructor
     * @param properties configuration
     * @throws Exception
     */
    public RandomizedBacktrackNeighbourSelection(DataProperties properties) throws Exception {
        super(properties);
        iMaxValues = properties.getPropertyInt("Neighbour.MaxValues", iMaxValues);
    }
    
    /** 
     * List of values of a variable.
     * {@link CourseRequest#computeRandomEnrollments(int)} with the provided limit is used 
     * for a {@link CourseRequest}.  
     */
    protected Enumeration values(Variable variable) {
        if (iMaxValues>0 && variable instanceof CourseRequest) {
            return ((CourseRequest)variable).computeRandomEnrollments(iMaxValues).elements();
        } 
        return variable.values().elements();
    }
}
