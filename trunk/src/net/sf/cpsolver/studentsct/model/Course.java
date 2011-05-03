package net.sf.cpsolver.studentsct.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Representation of a course offering. A course offering contains id, subject
 * area, course number and an instructional offering. <br>
 * <br>
 * Each instructional offering (see {@link Offering}) is offered under one or
 * more course offerings.
 * 
 * <br>
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
public class Course {
    private long iId = -1;
    private String iSubjectArea = null;
    private String iCourseNumber = null;
    private Offering iOffering = null;
    private int iLimit = 0, iProjected = 0;
    private double iEnrollmentWeight = 0.0;
    private Set<Enrollment> iEnrollments = new HashSet<Enrollment>();
    private Set<CourseRequest> iRequests = Collections.synchronizedSet(new HashSet<CourseRequest>());

    /**
     * Constructor
     * 
     * @param id
     *            course offering unique id
     * @param subjectArea
     *            subject area (e.g., MA, CS, ENGL)
     * @param courseNumber
     *            course number under the given subject area
     * @param offering
     *            instructional offering which is offered under this course
     *            offering
     */
    public Course(long id, String subjectArea, String courseNumber, Offering offering) {
        iId = id;
        iSubjectArea = subjectArea;
        iCourseNumber = courseNumber;
        iOffering = offering;
        iOffering.getCourses().add(this);
    }

    /**
     * Constructor
     * 
     * @param id
     *            course offering unique id
     * @param subjectArea
     *            subject area (e.g., MA, CS, ENGL)
     * @param courseNumber
     *            course number under the given subject area
     * @param offering
     *            instructional offering which is offered under this course
     *            offering
     * @param limit
     *            course offering limit (-1 for unlimited)
     * @param projected
     *            projected demand
     */
    public Course(long id, String subjectArea, String courseNumber, Offering offering, int limit, int projected) {
        iId = id;
        iSubjectArea = subjectArea;
        iCourseNumber = courseNumber;
        iOffering = offering;
        iOffering.getCourses().add(this);
        iLimit = limit;
        iProjected = projected;
    }

    /** Course offering unique id */
    public long getId() {
        return iId;
    }

    /** Subject area */
    public String getSubjectArea() {
        return iSubjectArea;
    }

    /** Course number */
    public String getCourseNumber() {
        return iCourseNumber;
    }

    /** Course offering name: subject area + course number */
    public String getName() {
        return iSubjectArea + " " + iCourseNumber;
    }

    @Override
    public String toString() {
        return getName();
    }

    /** Instructional offering which is offered under this course offering. */
    public Offering getOffering() {
        return iOffering;
    }

    /** Course offering limit */
    public int getLimit() {
        return iLimit;
    }

    /** Set course offering limit */
    public void setLimit(int limit) {
        iLimit = limit;
    }

    /** Course offering projected number of students */
    public int getProjected() {
        return iProjected;
    }
    
    /** Called when an enrollment with this course is assigned to a request */
    public void assigned(Enrollment enrollment) {
        iEnrollments.add(enrollment);
        iEnrollmentWeight += enrollment.getRequest().getWeight();
    }

    /** Called when an enrollment with this course is unassigned from a request */
    public void unassigned(Enrollment enrollment) {
        iEnrollments.remove(enrollment);
        iEnrollmentWeight -= enrollment.getRequest().getWeight();
    }
    
    /**
     * Enrollment weight -- weight of all requests that are enrolled into this course,
     * excluding the given one. See
     * {@link Request#getWeight()}.
     */
    public double getEnrollmentWeight(Request excludeRequest) {
        double weight = iEnrollmentWeight;
        if (excludeRequest != null && excludeRequest.getAssignment() != null
                && iEnrollments.contains(excludeRequest.getAssignment()))
            weight -= excludeRequest.getWeight();
        return weight;
    }
    
    /** Set of assigned enrollments */
    public Set<Enrollment> getEnrollments() {
        return iEnrollments;
    }
    
    /** Set of course requests requesting this course */
    public Set<CourseRequest> getRequests() {
        return iRequests;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Course)) return false;
        return getId() == ((Course)o).getId();
    }
    
    @Override
    public int hashCode() {
        return (int) (iId ^ (iId >>> 32));
    }
}
