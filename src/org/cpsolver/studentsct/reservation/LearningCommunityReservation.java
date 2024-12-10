package org.cpsolver.studentsct.reservation;

import java.util.Collection;

import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Student;

/**
 * Learning Community reservation. This is a combination of {@link GroupReservation}
 * and {@link CourseReservation}. Space is reserved for students of a group but only when
 * they enroll into the offering through the given course.
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
public class LearningCommunityReservation extends GroupReservation {
    private Course iCourse;
    
    /**
     * Learning Community reservations are just a below student group reservations.
     */
    public static final int DEFAULT_PRIORITY = 250;
    /**
     * Learning Community reservation must be used (unless it is expired)
     */
    public static final boolean DEFAULT_MUST_BE_USED = true;
    /**
     * Learning Community reservations cannot be assigned over the limit.
     */
    public static final boolean DEFAULT_CAN_ASSIGN_OVER_LIMIT = false;
    /**
     * Overlaps are not allowed for Learning Community reservations. 
     */
    public static final boolean DEFAULT_ALLOW_OVERLAP = false;

    /**
     * Constructor
     * @param id unique id
     * @param limit reservation limit (-1 for unlimited)
     * @param course course offering for which the reservation is
     * @param studentIds one or more students
     */
    public LearningCommunityReservation(long id, double limit, Course course, Long... studentIds) {
        super(id, limit, course.getOffering(), DEFAULT_PRIORITY, DEFAULT_MUST_BE_USED, DEFAULT_CAN_ASSIGN_OVER_LIMIT, DEFAULT_ALLOW_OVERLAP, studentIds);
        iCourse = course;
    }
    
    /**
     * Constructor
     * @param id unique id
     * @param limit reservation limit (-1 for unlimited)
     * @param course course offering for which the reservation is
     * @param studentIds one or more students
     */
    public LearningCommunityReservation(long id, double limit, Course course, Collection<Long> studentIds) {
        super(id, limit, course.getOffering(), DEFAULT_PRIORITY, DEFAULT_MUST_BE_USED, DEFAULT_CAN_ASSIGN_OVER_LIMIT, DEFAULT_ALLOW_OVERLAP, studentIds);
        iCourse = course;
    }

    /**
     * Reservation limit (-1 for unlimited)
     */
    @Override
    public double getReservationLimit() {
        if (super.getReservationLimit() < 0.0)
            return iCourse.getLimit(); // no group limit >> return course limit
        else if (iCourse.getLimit()  < 0.0)
            return super.getReservationLimit(); // course unlimited >> return group limit
        else
            return Math.min(super.getReservationLimit(), iCourse.getLimit()); // return smaller of the two limits
    }
    
    /**
     * Course offering
     * @return course offering
     */
    public Course getCourse() {
        return iCourse;
    }
    
    /**
     * Check the student group and the course
     */
    @Override
    public boolean isApplicable(Student student) {
        if (!super.isApplicable(student)) return false;
        for (Request r: student.getRequests()) {
            if (r instanceof CourseRequest) {
                for (Course course: ((CourseRequest) r).getCourses()) {
                    if (course.equals(getCourse())) return true;
                }
            }
        }
        return false;
    }
}
