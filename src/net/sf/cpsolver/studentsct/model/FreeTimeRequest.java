package net.sf.cpsolver.studentsct.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.coursett.model.RoomLocation;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.studentsct.StudentSectioningModel;


/**
 * Representation of a request of a student for free time. This class directly
 * implements {@link Assignment} API, with the appropriate free time. <br>
 * <br>
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
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
public class FreeTimeRequest extends Request implements Assignment {
    private TimeLocation iTime = null;
    private HashSet<Enrollment> iEnrollments = new HashSet<Enrollment>();

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
    public boolean isOverlapping(Assignment assignment) {
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
    public boolean isOverlapping(Set<? extends Assignment> assignments) {
        if (isAllowOverlap())
            return false;
        if (getTime() == null)
            return false;
        for (Assignment assignment : assignments) {
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

    /** Create enrollment of this request */
    public Enrollment createEnrollment() {
        HashSet<Assignment> assignments = new HashSet<Assignment>();
        assignments.add(this);
        return new Enrollment(this, 0, null, assignments);
    }

    /**
     * Create all possible enrollments of this request -- there is only one
     * possible enrollment: {@link FreeTimeRequest#createEnrollment()}
     */
    @Override
    public List<Enrollment> computeEnrollments() {
        List<Enrollment> enrollments = new ArrayList<Enrollment>(1);
        enrollments.add(createEnrollment());
        return enrollments;
    }

    /** Enrollment with this assignment was assigned to a {@link Request}. */
    @Override
    public void assigned(Enrollment enrollment) {
        iEnrollments.add(enrollment);
    }

    /** Enrollment with this assignment was unassigned from a {@link Request}. */
    @Override
    public void unassigned(Enrollment enrollment) {
        iEnrollments.remove(enrollment);
    }

    /** Return the list of assigned enrollments that contains this assignment. */
    @Override
    public Set<Enrollment> getEnrollments() {
        return iEnrollments;
    }

    /**
     * Request name: A for alternative, 1 + priority, Free Time, long name of
     * requested time
     */
    @Override
    public String getName() {
        return (isAlternative() ? "A" : "") + (1 + getPriority() + (isAlternative() ? -getStudent().nrRequests() : 0))
                + ". Free Time " + getTime().getDayHeader() + " " + getTime().getStartTimeHeader() + " - " + getTime().getEndTimeHeader();
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
        return false;
    }
    
    /** Sections first, then by {@link FreeTimeRequest#getId()} */
    @Override
    public int compareById(Assignment a) {
        if (a instanceof FreeTimeRequest) {
            return new Long(getId()).compareTo(((FreeTimeRequest)a).getId());
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
        return super.equals(o) && (o instanceof CourseRequest) && getTime().equals(((FreeTimeRequest)o).getTime());
    }

}
