package net.sf.cpsolver.studentsct.reservation;

import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * Course reservation. Students are matched based on their course requests.  
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
public class CourseReservation extends Reservation {
    private Course iCourse;
    
    /**
     * Constructor
     * @param id unique id
     * @param course course offering on which the reservation is set
     */
    public CourseReservation(long id, Course course) {
        super(id, course.getOffering());
        iCourse = course;
    }

    /**
     * Curriculum reservation cannot go over the limit
     */
    @Override
    public boolean canAssignOverLimit() {
        return false;
    }

    /**
     * Reservation limit (-1 for unlimited)
     */
    @Override
    public double getLimit() {
        return iCourse.getLimit();
    }

    /**
     * Reservation priority (lower than individual and group reservations)
     */
    @Override
    public int getPriority() {
        return 2;
    }
    
    /**
     * Course offering
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
