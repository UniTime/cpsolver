package org.cpsolver.studentsct.constraint;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Student;

/**
 * Student not available. This global constraint ensures that all students are available
 * during their enrollments. That is the student does not have any overlapping unavailabilities
 * that do not allow for overlap. Function {@link Student#isAvailable(Enrollment)} is used.
 * 
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2014 Tomas Muller<br>
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
public class StudentNotAvailable extends GlobalConstraint<Request, Enrollment> {
    
    /**
     * A given enrollment is conflicting, if the student is not available.
     * That is there is at least one unavailability that cannot overlap and that is overlapping with the enrollment.
     * 
     * @param enrollment {@link Enrollment} that is being considered
     * @param conflicts all computed conflicting enrollments are added into this set
     */
    @Override
    public void computeConflicts(Assignment<Request, Enrollment> assignment, Enrollment enrollment, Set<Enrollment> conflicts) {
        if (inConflict(assignment, enrollment))
            conflicts.add(enrollment);
    }
    
    /**
     * A given enrollment is conflicting, if the student is not available.
     * That is there is at least one unavailability that cannot overlap and that is overlapping with the enrollment.
     * 
     * @param enrollment {@link Enrollment} that is being considered
     * @return true, if the student is not available during the given enrollment 
     */
    @Override
    public boolean inConflict(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        return !enrollment.getStudent().isAvailable(enrollment);
    }
    
    @Override
    public String toString() {
        return "StudentNotAvailable";
    }
}