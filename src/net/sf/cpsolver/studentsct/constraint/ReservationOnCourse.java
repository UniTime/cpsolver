package net.sf.cpsolver.studentsct.constraint;

import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.Enrollment;

/**
 * Abstract course reservation. 
 * 
 * <br><br>
 * 
 * @version
 * StudentSct 1.1 (Student Sectioning)<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@ktiml.mff.cuni.cz">muller@ktiml.mff.cuni.cz</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
public abstract class ReservationOnCourse extends Reservation {
    private Course iCourse = null;
    
    /** 
     * Constructor
     * @param course course on which the reservation is set
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
     * True, if the enrollment contains the course on which this reservation is set.
     * See {@link Reservation#isApplicable(Enrollment)} for details.
     */
    public boolean isApplicable(Enrollment enrollment) {
        if (enrollment.getConfig()==null) return false;
        
        // Course check -- is it needed?
        Course courseThisEnrollment = enrollment.getConfig().getOffering().getCourse(enrollment.getStudent());
        if (courseThisEnrollment==null || !courseThisEnrollment.equals(iCourse)) return false;
        
        return true;
    }
    
    public String toString() {
        return "Reservation on "+iCourse.getName();
    }
}
