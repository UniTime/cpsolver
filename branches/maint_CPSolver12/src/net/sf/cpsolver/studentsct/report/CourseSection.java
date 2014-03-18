package net.sf.cpsolver.studentsct.report;

import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.Section;

/**
 * A simple class containing reference to a (course, class) pair. Used in some of the reports.
 * Provides sorting capabilities.
 * 
 * <br>
 * <br>
 *  
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2013 Tomas Muller<br>
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
public class CourseSection implements Comparable<CourseSection> {
    private Course iCourse;
    private Section iSection;
    
    /**
     * Constructor
     */
    public CourseSection(Course course, Section section) {
        iCourse = course;
        iSection = section;
    }
    
    /**
     * Course
     */
    public Course getCourse() { return iCourse; }
    
    /**
     * Class
     */
    public Section getSection() { return iSection; }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof CourseSection)) return false;
        CourseSection cs = (CourseSection)o;
        return getCourse().equals(cs.getCourse()) && getSection().equals(cs.getSection());
    }
    
    @Override
    public int hashCode() {
        long h1 = getCourse().hashCode();
        long h2 = getSection().hashCode();
        return (int)(h1 ^ (h2 >>> 32));
    }
    
    @Override
    public int compareTo(CourseSection other) {
        int cmp = getCourse().getName().compareTo(other.getCourse().getName());
        if (cmp != 0) return cmp;
        cmp = getSection().getSubpart().getInstructionalType().compareTo(other.getSection().getSubpart().getInstructionalType());
        if (cmp != 0) return cmp;
        // cmp = getSection().getName().compareTo(other.getSection().getName());
        // if (cmp != 0) return cmp;
        return getSection().getId() < other.getSection().getId() ? -1 : getSection().getId() == other.getSection().getId() ? 0 : 1;
    }
}