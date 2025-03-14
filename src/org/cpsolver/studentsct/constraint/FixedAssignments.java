package org.cpsolver.studentsct.constraint;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;

/**
 * Fixed assignment constraint. This global constraint checks that the 
 * conflicts that have been computed so far (by other global constraints earlier
 * in the list) do not create a conflict with a fixed assignment. This constraint
 * is to be used only when there are fixed course requests and it is to be last global constraint
 * in the list.
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2015 Tomas Muller<br>
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
public class FixedAssignments extends GlobalConstraint<Request, Enrollment> {

    /**
     * If there is a conflict that is fixed (method {@link CourseRequest#isFixed()} returns true)
     * and equal to the initial assignment (returned by {@link CourseRequest#getFixedValue()}),
     * the given enrollment is put into the conflicts.
     */
    @Override
    public void computeConflicts(Assignment<Request, Enrollment> assignment, Enrollment enrollment, Set<Enrollment> conflicts) {
        for (Enrollment conflict: conflicts) {
            if (conflict.getRequest() instanceof CourseRequest) {
                CourseRequest cr = (CourseRequest)conflict.getRequest();
                if (cr.isFixed() && conflict.equals(cr.getFixedValue())) {
                    conflicts.add(enrollment);
                    return;
                }
            }
        }
    }
}
