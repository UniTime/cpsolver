package org.cpsolver.studentsct.constraint;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Student;


/**
 * This constraints ensures that a student is not enrolled into sections that
 * are overlapping in time.
 * 
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
public class StudentConflict extends Constraint<Request, Enrollment> {
    
    /**
     * Constructor
     * @param student student for which the constraint is created
     */
    public StudentConflict(Student student) {
        super();
        for (Request request : student.getRequests())
            addVariable(request);
    }
    
    /**
     * True if the given enrollment can be assigned to the student. A new enrollment
     * cannot be assigned to a student when the student already has the desired
     * number of requests assigned (i.e., number of non-alternative course
     * requests).
     * @param assignment current assignment
     * @param enrollment given enrollment
     * @return true if the given request can be assigned
     **/
    public static boolean canAssign(Assignment<Request, Enrollment> assignment, Enrollment enrollment, Set<Enrollment> conflicts) {
        int alt = 0;
        float credit = 0;
        boolean found = false;
        for (Request r : enrollment.getStudent().getRequests()) {
            Enrollment e = (r.equals(enrollment.getRequest()) ? enrollment : r.getAssignment(assignment));
            if (r.equals(enrollment.getRequest())) {
                found = true;
            } else if (e != null && conflicts != null && conflicts.contains(e)) {
                e = null;
            }
            boolean assigned = (e != null || r.equals(enrollment.getRequest()));
            boolean course = (r instanceof CourseRequest);
            boolean waitlist = (course && ((CourseRequest) r).isWaitlist());
            if (r.isAlternative()) {
                if (assigned || (!found && waitlist))
                    alt--;
            } else {
                if (course && !waitlist && !assigned)
                    alt++;
            }
            if (e != null) credit += e.getCredit();
        }
        return (alt >= 0 && credit <= enrollment.getStudent().getMaxCredit());
    }

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
    public void computeConflicts(Assignment<Request, Enrollment> assignment, Enrollment enrollment, Set<Enrollment> conflicts) {
        // for all assigned course requests -> if overlapping with this
        // enrollment -> conflict
        for (Request request : variables()) {
            if (request.equals(enrollment.getRequest()))
                continue;
            Enrollment e = assignment.getValue(request);
            if (e == null)
                continue;
            if (enrollment.isOverlapping(e))
                conflicts.add(e);
        }

        // if this enrollment cannot be assigned (student already has a full
        // schedule) -> unassignd a lowest priority request
        while (!enrollment.getAssignments().isEmpty() && !canAssign(assignment, enrollment, conflicts)) {
            Enrollment lowestPriorityEnrollment = null;
            int lowestPriority = -1;
            for (Request request : variables()) {
                if (request.equals(enrollment.getRequest()))
                    continue;
                if (!(request instanceof CourseRequest) || ((CourseRequest)request).isWaitlist())
                    continue;
                Enrollment e = assignment.getValue(request);
                if (e == null || conflicts.contains(e))
                    continue;
                if (lowestPriority < request.getPriority()) {
                    lowestPriority = request.getPriority();
                    lowestPriorityEnrollment = e;
                }
            }
            if (lowestPriorityEnrollment != null) {
                conflicts.add(lowestPriorityEnrollment);
            } else {
                conflicts.add(enrollment); // there are only alternatives or wait-listed courses
                break;
            }
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
    public boolean inConflict(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        // for all assigned course requests -> if overlapping with this
        // enrollment -> conflict
        for (Request request : variables()) {
            if (request.equals(enrollment.getRequest()))
                continue;
            Enrollment e = assignment.getValue(request);
            if (e == null)
                continue;
            if (enrollment.isOverlapping(e))
                return true;
        }

        // if this enrollment cannot be assigned (student already has a full
        // schedule) -> conflict
        if (!canAssign(assignment, enrollment, null))
            return true;

        // nothing above -> no conflict
        return false;
    }

    @Override
    public String toString() {
        return "StudentConflicts";
    }
}
