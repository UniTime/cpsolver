package net.sf.cpsolver.studentsct.model;

import java.util.Enumeration;
import java.util.Vector;

/**
 * Representation of a student. Each student contains id, and a list of requests. 
 * <br><br>
 * Last-like semester students are mark as dummy. Dummy students have lower value
 * and generally should not block "real" students from getting requested courses.
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
 */public class Student {
    private long iId;
    private boolean iDummy = false;
    private Vector iRequests = new Vector();

    public static double sDummyStudentWeight = 0.5;

    /** Constructor
     * @param id student unique id
     */
    public Student(long id) {
        iId = id;
    }
    
    /** Constructor
     * @param id student unique id
     * @param dummy dummy flag
     */
    public Student(long id, boolean dummy) {
        iId = id;
        iDummy = dummy;
    }

    /** Student unique id */
    public long getId() {
        return iId;
    }

    /** Student's course and free time requests */
    public Vector getRequests() {
        return iRequests;
    }
    
    /** Number of requests (alternative requests are ignored) */
    public int nrRequests() {
        int ret = 0;
        for (Enumeration e=iRequests.elements();e.hasMoreElements();) {
            Request r  = (Request)e.nextElement();
            if (!r.isAlternative()) ret++;
        }
        return ret;
    }
    
    /** Number of alternative requests */
    public int nrAlternativeRequests() {
        int ret = 0;
        for (Enumeration e=iRequests.elements();e.hasMoreElements();) {
            Request r  = (Request)e.nextElement();
            if (r.isAlternative()) ret++;
        }
        return ret;
    }

    /** 
     * True if the given request can be assigned to the student.
     * A request cannot be assigned to a student when the student already has the 
     * desired number of requests assigned (i.e., number of non-alternative course
     * requests).  
     **/
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
    
    /** 
     * True if the student has assigned the desired number of requests (i.e., number of non-alternative course
     * requests). 
     */
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
    
    /** Number of assigned COURSE requests */
    public int nrAssignedRequests() {
        int nrAssignedRequests = 0;
        for (Enumeration e=iRequests.elements();e.hasMoreElements();) {
            Request r  = (Request)e.nextElement();
            if (!(r instanceof CourseRequest)) continue; //ignore free times
            if (r.getAssignment()!=null) nrAssignedRequests++;
        }
        return nrAssignedRequests;
    }
    
    public String toString() {
        return (isDummy()?"D":"")+"S["+getId()+"]";
    }
    
    /** 
     * Student's dummy flag. Dummy students have lower value
     * and generally should not block "real" students from getting requested courses.
     */
    public boolean isDummy() {
        return iDummy;
    }
    
    /** 
     * Set student's dummy flag. Dummy students have lower value
     * and generally should not block "real" students from getting requested courses.
     */
    public void setDummy(boolean dummy) {
        iDummy = dummy;
    }
}
