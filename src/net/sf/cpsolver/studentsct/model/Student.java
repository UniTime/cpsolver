package net.sf.cpsolver.studentsct.model;

import java.util.Enumeration;
import java.util.Vector;

import net.sf.cpsolver.ifs.multi.MultiVariable;

public class Student extends MultiVariable {
    private long iId;
    private boolean iDummy = false;
    private Vector iRequests = new Vector();

    public static double sDummyStudentWeight = 0.5;

    public Student(long id) {
        iId = id;
    }
    
    public Student(long id, boolean dummy) {
        iId = id;
        iDummy = dummy;
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
            Request r  = (Request)e.nextElement();
            if (!r.isAlternative()) ret++;
        }
        return ret;
    }
    
    public int nrAlternativeRequests() {
        int ret = 0;
        for (Enumeration e=iRequests.elements();e.hasMoreElements();) {
            Request r  = (Request)e.nextElement();
            if (r.isAlternative()) ret++;
        }
        return ret;
    }

    public boolean canAssign(Request request) {
        if (request.getAssignment()!=null) return true;
        int alt = 0;
        boolean found = false;
        for (Enumeration e=iRequests.elements();e.hasMoreElements();) {
            Request r  = (Request)e.nextElement();
            if (r.equals(request)) found = true;
            boolean assigned = (r.getAssignment()!=null || r.equals(request));
            boolean course = (r instanceof CourseRequest);
            boolean waitlist = (course && ((CourseRequest)r).isWaitlist());
            if (r.isAlternative()) {
                if (assigned || (!found && waitlist)) alt--;
            } else {
                if (course && !waitlist && !assigned) alt++;
            }
        }
        return (alt>=0);
    }
    
    public boolean isComplete() {
        int nrRequests = 0;
        int nrAssignedRequests = 0;
        for (Enumeration e=iRequests.elements();e.hasMoreElements();) {
            Request r  = (Request)e.nextElement();
            if (!(r instanceof CourseRequest)) continue; //ignore free times
            if (!r.isAlternative()) nrRequests++;
            if (r.getAssignment()!=null) nrAssignedRequests++;
        }
        return nrAssignedRequests==nrRequests;
    }
    
    public Vector variables() {
        return getRequests();
    }
    
    public String toString() {
        return (isDummy()?"D":"")+"S["+getId()+"]";
    }
    
    public boolean isDummy() {
        return iDummy;
    }
    
    public void setDummy(boolean dummy) {
        iDummy = dummy;
    }
}
