package org.cpsolver.studentsct.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.studentsct.StudentSectioningModel;



/**
 * Representation of a request of a student for free time. This class directly
 * implements {@link SctAssignment} API, with the appropriate free time. <br>
 * <br>
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2014 Tomas Muller<br>
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
public class FreeTimeRequest extends Request implements SctAssignment {
    private TimeLocation iTime = null;

    /**
     * Constructor
     * 
     * @param id
     *            request unique id
     * @param priority
     *            request priority
     * @param alternative
     *            true if the request is alternative (alternative request can be
     *            assigned instead of a non-alternative course requests, if it
     *            is left unassigned)
     * @param student
     *            appropriate student
     * @param time
     *            appropriate time location that is requested to be free
     */
    public FreeTimeRequest(long id, int priority, boolean alternative, Student student, TimeLocation time) {
        super(id, priority, alternative, student);
        iTime = time;
    }

    /** Return requested time to be free */
    @Override
    public TimeLocation getTime() {
        return iTime;
    }

    /** Assignment API: free time request has no rooms */
    @Override
    public int getNrRooms() {
        return 0;
    }

    /** Assignment API: free time request has no rooms */
    @Override
    public List<RoomLocation> getRooms() {
        return new ArrayList<RoomLocation>(0);
    }

    /**
     * True, if this assignment is overlapping in time and space with the given
     * assignment.
     */
    @Override
    public boolean isOverlapping(SctAssignment assignment) {
        if (isAllowOverlap() || assignment.isAllowOverlap()) return false;
        if (getTime() == null || assignment.getTime() == null)
            return false;
        if (assignment instanceof FreeTimeRequest)
            return false;
        return getTime().hasIntersection(assignment.getTime());
    }

    /**
     * True, if this assignment is overlapping in time and space with the given
     * set of assignments.
     */
    @Override
    public boolean isOverlapping(Set<? extends SctAssignment> assignments) {
        if (isAllowOverlap())
            return false;
        if (getTime() == null)
            return false;
        for (SctAssignment assignment : assignments) {
            if (assignment.isAllowOverlap())
                continue;
            if (assignment.getTime() == null)
                continue;
            if (assignment instanceof FreeTimeRequest)
                return false;
            if (getTime().hasIntersection(assignment.getTime()))
                return true;
        }
        return false;
    }

    /** Create enrollment of this request 
     * @return created enrollment
     **/
    public Enrollment createEnrollment() {
        HashSet<SctAssignment> assignments = new HashSet<SctAssignment>();
        assignments.add(this);
        return new Enrollment(this, 0, null, assignments, null);
    }

    /**
     * Create all possible enrollments of this request -- there is only one
     * possible enrollment: {@link FreeTimeRequest#createEnrollment()}
     */
    @Override
    public List<Enrollment> computeEnrollments(Assignment<Request, Enrollment> assignment) {
        List<Enrollment> enrollments = new ArrayList<Enrollment>(1);
        enrollments.add(createEnrollment());
        return enrollments;
    }

    /** Enrollment with this assignment was assigned to a {@link Request}. */
    @Override
    public void assigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        ((FreeTimeRequestContext)getContext(assignment)).assigned(assignment, enrollment);
    }

    /** Enrollment with this assignment was unassigned from a {@link Request}. */
    @Override
    public void unassigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        ((FreeTimeRequestContext)getContext(assignment)).unassigned(assignment, enrollment);
    }

    /** Return the list of assigned enrollments that contains this assignment. */
    @Override
    public Set<Enrollment> getEnrollments(Assignment<Request, Enrollment> assignment) {
        return ((FreeTimeRequestContext)getContext(assignment)).getEnrollments();
    }

    /**
     * Request name: A for alternative, 1 + priority, Free Time, long name of
     * requested time
     */
    @Override
    public String getName() {
        return (isAlternative() ? "A" : "") + (1 + getPriority() + (isAlternative() ? -getStudent().nrRequests() : 0))
                + ". Free Time " + getTime().getDayHeader() + " " + getTime().getStartTimeHeader(true) + " - " + getTime().getEndTimeHeader(true);
    }

    @Override
    public String toString() {
        return getName();
    }

    /** Estimated bound for this request */
    @Override
    public double getBound() {
        return - getWeight() * ((StudentSectioningModel)getModel()).getStudentWeights().getBound(this);
    }

    /** Free time request generally allow overlaps. */
    @Override
    public boolean isAllowOverlap() {
        return (getModel() == null ? true : ((StudentSectioningModel)getModel()).getStudentWeights().isFreeTimeAllowOverlaps());
    }
    
    /** Sections first, then by {@link FreeTimeRequest#getId()} */
    @Override
    public int compareById(SctAssignment a) {
        if (a instanceof FreeTimeRequest) {
            return Long.valueOf(getId()).compareTo(((FreeTimeRequest)a).getId());
        } else {
            return 1;
        }
    }
    
    @Override
    public int hashCode() {
        return super.hashCode() ^ getTime().hashCode();
    }

    
    @Override
    public boolean equals(Object o) {
        return super.equals(o) && (o instanceof FreeTimeRequest) && getTime().equals(((FreeTimeRequest)o).getTime());
    }
    
    public class FreeTimeRequestContext extends RequestContext {
        private HashSet<Enrollment> iEnrollments = new HashSet<Enrollment>();

        public FreeTimeRequestContext(Assignment<Request, Enrollment> assignment) {
            super(assignment);
            Enrollment enrollment = assignment.getValue(FreeTimeRequest.this);
            if (enrollment != null)
                assigned(assignment, enrollment);
        }

        /** Enrollment with this assignment was assigned to a {@link Request}. 
         * @param assignment current assignment
         * @param enrollment assigned enrollment
         **/
        public void assigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
            iEnrollments.add(enrollment);
        }

        /** Enrollment with this assignment was unassigned from a {@link Request}.
         * @param assignment current assignment
         * @param enrollment unassigned enrollment
         */
        public void unassigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
            iEnrollments.remove(enrollment);
        }
        
        /** Return the list of assigned enrollments that contains this assignment. 
         * @return current enrollments
         **/
        public Set<Enrollment> getEnrollments() {
            return iEnrollments;
        }
        
    }
    
    @Override
    public RequestContext createAssignmentContext(Assignment<Request, Enrollment> assignment) {
        return new FreeTimeRequestContext(assignment);
    }

    @Override
    public float getMinCredit() {
        return 0;
    }

    @Override
    public RequestPriority getRequestPriority() {
        return RequestPriority.Normal;
    }
}
