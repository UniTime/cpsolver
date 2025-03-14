package org.cpsolver.instructor.model;

/**
 * Course of a teaching request. If a course is marked as exclusive, all assignments of an instructor must have
 * the same course. It is also possible to mark the course as same common, which require all assignments that are 
 * given to a single instructor to share the same common part of the course (e.g., the lecture).
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Instructor Sectioning)<br>
 *          Copyright (C) 2016 Tomas Muller<br>
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
public class Course {
    private Long iCourseId;
    private String iCourseName;
    
    /**
     * Constructor
     * @param courseId course id
     * @param courseName course name
     */
    public Course(long courseId, String courseName) {
        iCourseId = courseId; iCourseName = courseName;
    }
    
    /**
     * Course id that was provided in the constructor
     * @return course id
     */
    public Long getCourseId() { return iCourseId; }
    
    /**
     * Course name that was provided in the constructor
     * @return course name
     */
    public String getCourseName() { return iCourseName == null ? "C" + iCourseId : iCourseName; }
    
    @Override
    public int hashCode() {
        return getCourseId().hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Course)) return false;
        Course c = (Course)o;
        return getCourseId().equals(c.getCourseId());            
    }
    
    @Override
    public String toString() { return getCourseName(); }
}
