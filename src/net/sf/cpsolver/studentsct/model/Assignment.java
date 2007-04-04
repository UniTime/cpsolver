package net.sf.cpsolver.studentsct.model;

import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.coursett.model.TimeLocation;

public interface Assignment {
    public TimeLocation getTime();
    public Vector getRooms();
    public int getNrRooms();
    
    public boolean isOverlapping(Assignment assignment);
    public boolean isOverlapping(Set assignments);
    
    public void assigned(Enrollment enrollment);
    public void unassigned(Enrollment enrollment);
    public Set getEnrollments();
}
