package net.sf.cpsolver.studentsct.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.coursett.model.TimeLocation;

/**
 * Representation of a request of a student for free time.
 * This class directly implements {@link Assignment} API, with the appropriate free time. 
 * <br><br>
 * 
 * @version
 * StudentSct 1.1 (Student Sectioning)<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
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
public class FreeTimeRequest extends Request implements Assignment {
    private TimeLocation iTime = null;
    private HashSet iEnrollments = new HashSet();
    
    /** Constructor
     * @param id request unique id
     * @param priority request priority
     * @param alternative true if the request is alternative (alternative request can be assigned instead of a non-alternative course requests, if it is left unassigned)
     * @param student appropriate student
     * @param time appropriate time location that is requested to be free
     */
    public FreeTimeRequest(long id, int priority, boolean alternative, Student student, TimeLocation time) {
        super(id, priority, alternative, student);
        iTime = time;
    }
    
    /** Return requested time to be free */
    public TimeLocation getTime() {
        return iTime;
    }

    /** Assignment API: free time request has no rooms */
    public int getNrRooms() {
        return 0;
    }
    
    /** Assignment API: free time request has no rooms */
    public Vector getRooms() {
        return new Vector(0);
    }
    
    /** True, if this assignment is overlapping in time and space with the given assignment. */
    public boolean isOverlapping(Assignment assignment) {
        if (getTime()==null || assignment.getTime()==null) return false;
        return getTime().hasIntersection(assignment.getTime());
    }
    
    /** True, if this assignment is overlapping in time and space with the given set of assignments. */
    public boolean isOverlapping(Set assignments) {
        if (getTime()==null) return false;
        for (Iterator i=assignments.iterator();i.hasNext();) {
            Assignment assignment = (Assignment)i.next();
            if (assignment.getTime()==null) continue;
            if (getTime().hasIntersection(assignment.getTime())) return true;
        }
        return false;
    }
    
    /** Create enrollment of this request */
    public Enrollment createEnrollment() {
        HashSet assignments = new HashSet();
        assignments.add(this);
        return new Enrollment(this, 1.0, null, assignments);
    }
    
    /** Create all possible enrollments of this request -- there is only one possible enrollment: {@link FreeTimeRequest#createEnrollment()}*/
    public Vector computeEnrollments() {
        Vector enrollments = new Vector(1);
        enrollments.add(createEnrollment());
        return enrollments;
    }
    
    /** Enrollment with this assignmnet was assigned to a {@link Request}. */
    public void assigned(Enrollment enrollment) {
        iEnrollments.add(enrollment);
    }
    
    /** Enrollment with this assignmnet was unassigned from a {@link Request}. */
    public void unassigned(Enrollment enrollment) {
        iEnrollments.remove(enrollment);
    }
    
    /** Return the list of assigned enrollments that contains this assignment.*/
    public Set getEnrollments() {
        return iEnrollments;
    }
    
    /** Request name: A for alternative, 1 + priority, Free Time, long name of requested time */
    public String getName() {
        return (isAlternative()?"A":"")+(1+getPriority()+(isAlternative()?-getStudent().nrRequests():0))+". Free Time "+getTime().getLongName();
    }

    public String toString() {
        return getName();
    }
    
    /** Estimated bound for this request */
    public double getBound() {
        return - Math.pow(Enrollment.sPriorityWeight,getPriority()) * (isAlternative()?Enrollment.sAlterativeWeight:1.0);
    }
}
