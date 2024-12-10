package org.cpsolver.studentsct.reservation;

import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Student;

/**
 * Course reservation. Students are matched based on their course requests.  
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
public class CourseReservation extends Reservation {
    private Course iCourse;
    
    /**
     * Reservation priority (lower than individual and group reservations)
     */
    public static final int DEFAULT_PRIORITY = 400;
    /**
     * Course reservation does not need to be used
     */
    public static final boolean DEFAULT_MUST_BE_USED = false;
    /**
     * Course reservations can not assign over the limit.
     */
    public static final boolean DEFAULT_CAN_ASSIGN_OVER_LIMIT = false;
    /**
     * Overlaps are not allowed for course reservations. 
     */
    public static final boolean DEFAULT_ALLOW_OVERLAP = false;
    
    /**
     * Constructor
     * @param id unique id
     * @param course course offering on which the reservation is set
     */
    public CourseReservation(long id, Course course) {
        super(id, course.getOffering(), DEFAULT_PRIORITY, DEFAULT_MUST_BE_USED, DEFAULT_CAN_ASSIGN_OVER_LIMIT, DEFAULT_ALLOW_OVERLAP);
        iCourse = course;
    }

    /**
     * Reservation limit (-1 for unlimited)
     */
    @Override
    public double getReservationLimit() {
        return iCourse.getLimit();
    }
    
    /**
     * Course offering
     * @return course offering
     */
    public Course getCourse() {
        return iCourse;
    }
    
    /**
     * Check the area, classifications and majors
     */
    @Override
    public boolean isApplicable(Student student) {
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
