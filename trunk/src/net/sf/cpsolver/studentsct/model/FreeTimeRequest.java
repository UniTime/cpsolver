package net.sf.cpsolver.studentsct.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.coursett.model.TimeLocation;

public class FreeTimeRequest extends Request implements Assignment {
    private TimeLocation iTime = null;
    private HashSet iEnrollments = new HashSet();
    
    public FreeTimeRequest(long id, int priority, boolean alternative, Student student, TimeLocation time) {
        super(id, priority, alternative, student);
        iTime = time;
    }
    
    public TimeLocation getTime() {
        return iTime;
    }

    public int getNrRooms() {
        return 0;
    }
    
    public Vector getRooms() {
        return new Vector(0);
    }
    
    public boolean isOverlapping(Assignment assignment) {
        if (getTime()==null || assignment.getTime()==null) return false;
        return getTime().hasIntersection(assignment.getTime());
    }
    
    public boolean isOverlapping(Set assignments) {
        if (getTime()==null) return false;
        for (Iterator i=assignments.iterator();i.hasNext();) {
            Assignment assignment = (Assignment)i.next();
            if (assignment.getTime()==null) continue;
            if (getTime().hasIntersection(assignment.getTime())) return true;
        }
        return false;
    }
    
    public Enrollment createEnrollment() {
        HashSet assignments = new HashSet();
        assignments.add(this);
        Vector enrollments = new Vector(1);
        return new Enrollment(this, 1.0, null, assignments);
    }
    
    public Vector computeEnrollments() {
        Vector enrollments = new Vector(1);
        enrollments.add(createEnrollment());
        return enrollments;
    }
    
    public void assigned(Enrollment enrollment) {
        iEnrollments.add(enrollment);
    }
    
    public void unassigned(Enrollment enrollment) {
        iEnrollments.remove(enrollment);
    }
    
    public Set getEnrollments() {
        return iEnrollments;
    }
    
    public String getName() {
        return (isAlternative()?"A":"")+(1+getPriority()+(isAlternative()?-getStudent().nrRequests():0))+". Free Time "+getTime().getLongName();
    }

    public String toString() {
        return getName();
    }
    
    public double getBound() {
        return - Math.pow(Enrollment.sPriorityWeight,getPriority()) * (isAlternative()?Enrollment.sAlterativeWeight:1.0);
    }
}
