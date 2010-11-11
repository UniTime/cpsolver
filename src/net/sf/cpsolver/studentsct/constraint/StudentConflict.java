package net.sf.cpsolver.studentsct.constraint;

import java.util.Set;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;

/**
 * This constraints ensures that a student is not enrolled into sections that
 * are overlapping in time.
 * 
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
public class StudentConflict extends Constraint<Request, Enrollment> {

    /**
     * A given enrollment is conflicting when the student is enrolled into
     * another course / free time request that has an assignment that is
     * overlapping with one or more assignments of the given section. See
     * {@link Enrollment#isOverlapping(Enrollment)} for more details. All such
     * overlapping enrollments are added into the provided set of conflicts.
     * 
     * @param enrollment
     *            {@link Enrollment} that is being considered
     * @param conflicts
     *            resultant list of conflicting enrollments
     */
    @Override
    public void computeConflicts(Enrollment enrollment, Set<Enrollment> conflicts) {
        // for all assigned course requests -> if overlapping with this
        // enrollment -> conflict
        for (Request request : assignedVariables()) {
            if (request.equals(enrollment.getRequest()))
                continue;
            if (enrollment.isOverlapping(request.getAssignment()))
                conflicts.add(request.getAssignment());
        }

        // if this enrollment cannot be assigned (student already has a full
        // schedule) -> unassignd a lowest priority request
        if (!enrollment.getAssignments().isEmpty() && !enrollment.getStudent().canAssign(enrollment.getRequest())) {
            Enrollment lowestPriorityEnrollment = null;
            int lowestPriority = -1;
            for (Request request : assignedVariables()) {
                if (request.equals(enrollment.getRequest()))
                    continue;
                if (lowestPriority < request.getPriority()) {
                    lowestPriority = request.getPriority();
                    lowestPriorityEnrollment = request.getAssignment();
                }
            }
            if (lowestPriorityEnrollment != null)
                conflicts.add(lowestPriorityEnrollment);
        }
    }

    /** Two enrollments are consistent if they are not overlapping in time */
    @Override
    public boolean isConsistent(Enrollment e1, Enrollment e2) {
        return !e1.isOverlapping(e2);
    }

    /**
     * A given enrollment is conflicting when the student is enrolled into
     * another course / free time request that has an assignment that is
     * overlapping with one or more assignments of the given section. See
     * {@link Enrollment#isOverlapping(Enrollment)} for more details.
     * 
     * @param enrollment
     *            {@link Enrollment} that is being considered
     * @return true, if the student is enrolled into another enrollment of a
     *         different request that is overlapping in time with the given
     *         enrollment
     */
    @Override
    public boolean inConflict(Enrollment enrollment) {
        // for all assigned course requests -> if overlapping with this
        // enrollment -> conflict
        for (Request request : assignedVariables()) {
            if (request.equals(enrollment.getRequest()))
                continue;
            if (enrollment.isOverlapping(request.getAssignment()))
                return true;
        }

        // if this enrollment cannot be assigned (student already has a full
        // schedule) -> conflict
        if (!enrollment.getStudent().canAssign(enrollment.getRequest()))
            return true;

        // nothing above -> no conflict
        return false;
    }

    @Override
    public String toString() {
        return "StudentConflicts";
    }
}
