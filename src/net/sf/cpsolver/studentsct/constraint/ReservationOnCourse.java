package net.sf.cpsolver.studentsct.constraint;

import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.Enrollment;

/**
 * Abstract course reservation.
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
public abstract class ReservationOnCourse extends Reservation {
    private Course iCourse = null;

    /**
     * Constructor
     * 
     * @param course
     *            course on which the reservation is set
     */
    public ReservationOnCourse(Course course) {
        super();
        iCourse = course;
    }

    /** Return course, on which the reservation is set */
    public Course getCourse() {
        return iCourse;
    }

    /**
     * True, if the enrollment contains the course on which this reservation is
     * set. See {@link Reservation#isApplicable(Enrollment)} for details.
     */
    @Override
    public boolean isApplicable(Enrollment enrollment) {
        if (enrollment.getConfig() == null)
            return false;

        // Course check -- is it needed?
        Course courseThisEnrollment = enrollment.getConfig().getOffering().getCourse(enrollment.getStudent());
        if (courseThisEnrollment == null || !courseThisEnrollment.equals(iCourse))
            return false;

        return true;
    }

    @Override
    public String toString() {
        return "Reservation on " + iCourse.getName();
    }
}
