package org.cpsolver.studentsct.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AbstractClassWithContext;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.model.Model;


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
public class Course extends AbstractClassWithContext<Request, Enrollment, Course.CourseContext> {
    private long iId = -1;
    private String iSubjectArea = null;
    private String iCourseNumber = null;
    private Offering iOffering = null;
    private int iLimit = 0, iProjected = 0;
    private Set<CourseRequest> iRequests = Collections.synchronizedSet(new HashSet<CourseRequest>());
    private String iNote = null;

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

    /** Course offering unique id 
     * @return coure offering unqiue id
     **/
    public long getId() {
        return iId;
    }

    /** Subject area 
     * @return subject area abbreviation
     **/
    public String getSubjectArea() {
        return iSubjectArea;
    }

    /** Course number 
     * @return course number
     **/
    public String getCourseNumber() {
        return iCourseNumber;
    }

    /** Course offering name: subject area + course number 
     * @return course name
     **/
    public String getName() {
        return iSubjectArea + " " + iCourseNumber;
    }

    @Override
    public String toString() {
        return getName();
    }

    /** Instructional offering which is offered under this course offering. 
     * @return instructional offering
     **/
    public Offering getOffering() {
        return iOffering;
    }

    /** Course offering limit 
     * @return course offering limit, -1 if unlimited
     **/
    public int getLimit() {
        return iLimit;
    }

    /** Set course offering limit 
     * @param limit course offering limit, -1 if unlimited
     **/
    public void setLimit(int limit) {
        iLimit = limit;
    }

    /** Course offering projected number of students 
     * @return course projection
     **/
    public int getProjected() {
        return iProjected;
    }
    
    /** Called when an enrollment with this course is assigned to a request 
     * @param assignment current assignment
     * @param enrollment assigned enrollment
     **/
    public void assigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        getContext(assignment).assigned(assignment, enrollment);
    }

    /** Called when an enrollment with this course is unassigned from a request
     * @param assignment current assignment
     * @param enrollment unassigned enrollment
     */
    public void unassigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        getContext(assignment).unassigned(assignment, enrollment);
    }
    
    /** Set of course requests requesting this course 
     * @return request for this course
     **/
    public Set<CourseRequest> getRequests() {
        return iRequests;
    }
    
    /**
     * Course note
     * @return course note
     */
    public String getNote() { return iNote; }
    
    /**
     * Course note
     * @param note course note
     */
    public void setNote(String note) { iNote = note; }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Course)) return false;
        return getId() == ((Course)o).getId();
    }
    
    @Override
    public int hashCode() {
        return (int) (iId ^ (iId >>> 32));
    }
    
    @Override
    public Model<Request, Enrollment> getModel() {
        return getOffering().getModel();
    }
    
    /**
     * Enrollment weight -- weight of all requests that are enrolled into this course,
     * excluding the given one. See
     * {@link Request#getWeight()}.
     * @param assignment current assignment
     * @param excludeRequest request to exclude
     * @return enrollment weight
     */
    public double getEnrollmentWeight(Assignment<Request, Enrollment> assignment, Request excludeRequest) {
        return getContext(assignment).getEnrollmentWeight(assignment, excludeRequest);
    }
    
    /** Set of assigned enrollments 
     * @param assignment current assignment
     * @return assigned enrollments for this course offering
     **/
    public Set<Enrollment> getEnrollments(Assignment<Request, Enrollment> assignment) {
        return getContext(assignment).getEnrollments();
    }

    @Override
    public CourseContext createAssignmentContext(Assignment<Request, Enrollment> assignment) {
        return new CourseContext(assignment);
    }
    
    public class CourseContext implements AssignmentConstraintContext<Request, Enrollment> {
        private double iEnrollmentWeight = 0.0;
        private Set<Enrollment> iEnrollments = new HashSet<Enrollment>();

        public CourseContext(Assignment<Request, Enrollment> assignment) {
            for (CourseRequest request: getRequests()) {
                Enrollment enrollment = assignment.getValue(request);
                if (enrollment != null && Course.this.equals(enrollment.getCourse()))
                    assigned(assignment, enrollment);
            }
        }

        @Override
        public void assigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
            if (iEnrollments.add(enrollment))
                iEnrollmentWeight += enrollment.getRequest().getWeight();
        }

        @Override
        public void unassigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
            if (iEnrollments.remove(enrollment))
                iEnrollmentWeight -= enrollment.getRequest().getWeight();
        }
        
        /**
         * Enrollment weight -- weight of all requests that are enrolled into this course,
         * excluding the given one. See
         * {@link Request#getWeight()}.
         * @param assignment current assignment
         * @param excludeRequest request to exclude
         * @return enrollment weight
         */
        public double getEnrollmentWeight(Assignment<Request, Enrollment> assignment, Request excludeRequest) {
            double weight = iEnrollmentWeight;
            if (excludeRequest != null && assignment.getValue(excludeRequest) != null && iEnrollments.contains(assignment.getValue(excludeRequest)))
                weight -= excludeRequest.getWeight();
            return weight;
        }
        
        /** Set of assigned enrollments 
         * @return assigned enrollments for this course offering
         **/
        public Set<Enrollment> getEnrollments() {
            return iEnrollments;
        }
    }
}
