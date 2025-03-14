package org.cpsolver.studentsct.reservation;

import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Student;

/**
 * Group restriction. Students are matched based on their course requests.  
 * <br>
 * <br>
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2020 Tomas Muller<br>
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
public class CourseRestriction extends Restriction {
    private Course iCourse;
    
    /**
     * Constructor
     * @param id restriction unique id
     * @param course course offering on which the restriction is set
     */
    public CourseRestriction(long id, Course course) {
        super(id, course.getOffering());
        iCourse = course;
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