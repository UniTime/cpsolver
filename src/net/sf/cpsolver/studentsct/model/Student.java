package net.sf.cpsolver.studentsct.model;

import java.util.Enumeration;
import java.util.Vector;

import net.sf.cpsolver.ifs.multi.MultiVariable;

public class Student extends MultiVariable {
    private long iId;
    private Vector iRequests = new Vector();

    public Student(long id) {
        iId = id;
    }
    
    public long getId() {
        return iId;
    }

    public Vector getRequests() {
        return iRequests;
    }
    
    public int nrRequests() {
        int ret = 0;
        for (Enumeration e=iRequests.elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (request instanceof CourseRequest && !request.isAlternative()) ret++;
        }
        return ret;
    }
    
    public boolean canAssign(Request request) {
        if (!request.isAlternative() || request.getAssignment()!=null) return true;
        int alt = 0;
        for (Enumeration e=iRequests.elements();e.hasMoreElements();) {
            Request r  = (Request)e.nextElement();
            if (r.equals(request)) continue;
            if (r.isAlternative()) {
                if (r.getAssignment()!=null || (r instanceof CourseRequest && ((CourseRequest)r).isWaitlist())) alt--;
            } else {
                if (r instanceof CourseRequest && !((CourseRequest)r).isWaitlist() && r.getAssignment()==null) alt++;
            }
        }
        return (alt>0);
    }
    
    public Vector variables() {
        return getRequests();
    }
    
    public String toString() {
        return "Student "+getId();
    }
}
