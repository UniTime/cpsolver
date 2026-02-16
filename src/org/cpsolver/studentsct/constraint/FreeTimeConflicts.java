package org.cpsolver.studentsct.constraint;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.FreeTimeRequest;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Student;

/**
 * A simple global constraint that does not allow an overlap with a higher
 * priority free time request, even when it is not assigned.
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2026 Tomas Muller<br>
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
public class FreeTimeConflicts extends GlobalConstraint<Request, Enrollment> {

    @Override
    public void computeConflicts(Assignment<Request, Enrollment> assignment, Enrollment value, Set<Enrollment> conflicts) {
        if (inConflict(assignment, value)) conflicts.add(value);
    }
    
    @Override
    public boolean inConflict(Assignment<Request, Enrollment> assignment, Enrollment value) {
        if (value.isAllowOverlap()) return false;
        Student student = value.getRequest().getStudent();
        for (Request request: student.getRequests()) {
            if (request.equals(value.getRequest())) break;
            if (request instanceof FreeTimeRequest) {
                FreeTimeRequest ft = (FreeTimeRequest)request;
                if (ft.isOverlapping(value.getSections())) return true;
            }
        }
        return false;
    }

}
