package net.sf.cpsolver.studentsct.constraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * Abstract reservation constraint. This constraint allow some section, courses,
 * and other parts to be reserved to particular group of students. A reservation
 * can be unlimited (any number of students of that particular group can attend
 * a course, section, etc.) or with a limit (only given number of seats is
 * reserved to the students of the particular group).
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
public abstract class Reservation extends Constraint<Request, Enrollment> {
    /** Student cannot be enrolled */
    public static int CAN_ENROLL_NO = 0;
    /** Student can be enrolled */
    public static int CAN_ENROLL_YES = 1;
    /**
     * Student can be enrolled, however, some other student has to dropped out
     * of the section, course, etc.
     */
    public static int CAN_ENROLL_INSTEAD = 2;

    /**
     * Check, whether a student can be enrolled into the given section, course,
     * etc.
     * 
     * @param student
     *            given student
     * @return {@link Reservation#CAN_ENROLL_YES},
     *         {@link Reservation#CAN_ENROLL_NO}, or
     *         {@link Reservation#CAN_ENROLL_INSTEAD}
     */
    public abstract int canEnroll(Student student);

    /**
     * Check whether the given student can be enrolled instead of another
     * student
     * 
     * @param student
     *            student that is to be enrolled in the particular course,
     *            section, etc.
     * @param insteadOfStudent
     *            student, that is currently enrolled, which is going to be
     *            bumped out of the course, section, etc. in order to enroll
     *            given studen
     * @return true, if the move is permitted
     */
    public abstract boolean canEnrollInstead(Student student, Student insteadOfStudent);

    /**
     * Check whether the reservation is applicable to the given enrollment. This
     * means that the given enrollment contains the particular section, course,
     * etc.
     * 
     * @param enrollment
     *            enrollment of a student that is being considered
     * @return true, if the reservation applies to the enrollment
     */
    public abstract boolean isApplicable(Enrollment enrollment);

    /**
     * Implementation of the constraint primitives. See
     * {@link Constraint#computeConflicts(Value, Set)} for more details.
     */
    @Override
    public void computeConflicts(Enrollment enrollment, Set<Enrollment> conflicts) {
        if (!isApplicable(enrollment))
            return;

        int ce = canEnroll(enrollment.getStudent());

        if (ce == CAN_ENROLL_YES)
            return;

        if (ce == CAN_ENROLL_NO) {
            // Unable to bump out some other student
            conflicts.add(enrollment);
            return;
        }

        // Try other bump out some other student
        List<Enrollment> conflictEnrollments = null;
        double conflictValue = 0;
        for (Request request : assignedVariables()) {
            if (canEnrollInstead(enrollment.getStudent(), request.getStudent())) {
                if (conflictEnrollments == null || conflictValue > enrollment.toDouble()) {
                    if (conflictEnrollments == null)
                        conflictEnrollments = new ArrayList<Enrollment>();
                    else
                        conflictEnrollments.clear();
                    conflictEnrollments.add(request.getAssignment());
                    conflictValue = request.getAssignment().toDouble();
                }
            }
        }
        if (conflictEnrollments != null && !conflictEnrollments.isEmpty()) {
            conflicts.add(ToolBox.random(conflictEnrollments));
            return;
        }

        // Unable to bump out some other student
        conflicts.add(enrollment);
    }

    /**
     * Implementation of the constraint primitives. See
     * {@link Constraint#inConflict(Value)} for more details.
     */
    @Override
    public boolean inConflict(Enrollment enrollment) {
        if (!isApplicable(enrollment))
            return false;

        return canEnroll(enrollment.getStudent()) != CAN_ENROLL_YES;
    }

}
