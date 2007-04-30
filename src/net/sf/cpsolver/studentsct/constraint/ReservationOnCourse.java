package net.sf.cpsolver.studentsct.constraint;

import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.Enrollment;

public abstract class ReservationOnCourse extends Reservation {
    private Course iCourse = null;
    
    public ReservationOnCourse(Course course) {
        super();
        iCourse = course;
    }
    
    public Course getCourse() {
        return iCourse;
    }
    
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
