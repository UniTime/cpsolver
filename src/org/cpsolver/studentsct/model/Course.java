package org.cpsolver.studentsct.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AbstractClassWithContext;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.CanInheritContext;
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
public class Course extends AbstractClassWithContext<Request, Enrollment, Course.CourseContext> implements CanInheritContext<Request, Enrollment, Course.CourseContext> {
    private long iId = -1;
    private String iSubjectArea = null;
    private String iCourseNumber = null;
    private Offering iOffering = null;
    private int iLimit = 0, iProjected = 0;
    private Set<CourseRequest> iRequests = Collections.synchronizedSet(new HashSet<CourseRequest>());
    private String iNote = null;
    private Set<RequestGroup> iRequestGroups = new HashSet<RequestGroup>();
    private String iCredit = null;
    private Float iCreditValue = null;
    private String iTitle = null;
    private String iType = null;

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
    
    /**
     * Maximal weight of a single enrollment in the course
     * @param assignment current assignment
     * @return maximal enrollment weight
     */
    public double getMaxEnrollmentWeight(Assignment<Request, Enrollment> assignment) {
        return getContext(assignment).getMaxEnrollmentWeight();
    }

    /**
     * Minimal weight of a single enrollment in the course
     * @param assignment current assignment
     * @return minimal enrollment weight
     */
    public double getMinEnrollmentWeight(Assignment<Request, Enrollment> assignment) {
        return getContext(assignment).getMinEnrollmentWeight();
    }
    
    /**
     * Add request group of this course. This is automatically called 
     * by the constructor of the {@link RequestGroup}.
     * @param group request group to be added
     */
    public void addRequestGroup(RequestGroup group) {
        iRequestGroups.add(group);
    }
    
    /**
     * Remove request group from this course.
     * @param group request group to be removed
     */
    public void removeRequestGroup(RequestGroup group) {
        iRequestGroups.remove(group);
    }
    
    /**
     * Lists all the request groups of this course
     * @return all request groups of this course
     */
    public Set<RequestGroup> getRequestGroups() {
        return iRequestGroups;
    }
    
    /**
     * Set credit (Online Student Scheduling only)
     * @param credit scheduling course credit
     */
    public void setCredit(String credit) {
        iCredit = credit;
        if (iCreditValue == null && credit != null) {
            int split = credit.indexOf('|');
            String abbv = null;
            if (split >= 0) {
                abbv = credit.substring(0, split);
            } else {
                abbv = credit;
            }
            Matcher m = Pattern.compile("(^| )(\\d+\\.?\\d*)([,-]?(\\d+\\.?\\d*))?($| )").matcher(abbv);
            if (m.find())
                iCreditValue = Float.parseFloat(m.group(2));
        }
    }
    
    /**
     * Get credit (Online Student Scheduling only)
     * @return scheduling course credit
     */
    public String getCredit() { return iCredit; }
    
    /**
     * True if this course has a credit value defined
     * @return true if a credit value is set
     */
    public boolean hasCreditValue() { return iCreditValue != null; }
    
    /**
     * Set course credit value (null if not set)
     * @param creditValue course credit value
     */
    public void setCreditValue(Float creditValue) { iCreditValue = creditValue; }
    
    /**
     * Get course credit value (null if not set)
     * return course credit value
     */
    public Float getCreditValue() { return iCreditValue; }

    @Override
    public CourseContext createAssignmentContext(Assignment<Request, Enrollment> assignment) {
        return new CourseContext(assignment);
    }
    

    @Override
    public CourseContext inheritAssignmentContext(Assignment<Request, Enrollment> assignment, CourseContext parentContext) {
        return new CourseContext(parentContext);
    }
    
    public class CourseContext implements AssignmentConstraintContext<Request, Enrollment> {
        private double iEnrollmentWeight = 0.0;
        private Set<Enrollment> iEnrollments = null;
        private double iMaxEnrollmentWeight = 0.0;
        private double iMinEnrollmentWeight = 0.0;
        private boolean iReadOnly = false;

        public CourseContext(Assignment<Request, Enrollment> assignment) {
            iEnrollments = new HashSet<Enrollment>();
            for (CourseRequest request: getRequests()) {
                Enrollment enrollment = assignment.getValue(request);
                if (enrollment != null && Course.this.equals(enrollment.getCourse()))
                    assigned(assignment, enrollment);
            }
        }
        
        public CourseContext(CourseContext parent) {
            iEnrollmentWeight = parent.iEnrollmentWeight;
            iMinEnrollmentWeight = parent.iMinEnrollmentWeight;
            iMaxEnrollmentWeight = parent.iMaxEnrollmentWeight;
            iEnrollments = parent.iEnrollments;
            iReadOnly = true;
        }

        @Override
        public void assigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
            if (iReadOnly) {
                iEnrollments = new HashSet<Enrollment>(iEnrollments);
                iReadOnly = false;
            }
            if (iEnrollments.isEmpty()) {
                iMinEnrollmentWeight = iMaxEnrollmentWeight = enrollment.getRequest().getWeight();
            } else {
                iMaxEnrollmentWeight = Math.max(iMaxEnrollmentWeight, enrollment.getRequest().getWeight());
                iMinEnrollmentWeight = Math.min(iMinEnrollmentWeight, enrollment.getRequest().getWeight());
            }
            if (iEnrollments.add(enrollment) && (enrollment.getReservation() == null || !enrollment.getReservation().canBatchAssignOverLimit()))
                iEnrollmentWeight += enrollment.getRequest().getWeight();
        }

        @Override
        public void unassigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
            if (iReadOnly) {
                iEnrollments = new HashSet<Enrollment>(iEnrollments);
                iReadOnly = false;
            }
            if (iEnrollments.remove(enrollment) && (enrollment.getReservation() == null || !enrollment.getReservation().canBatchAssignOverLimit()))
                iEnrollmentWeight -= enrollment.getRequest().getWeight();
            if (iEnrollments.isEmpty()) {
                iMinEnrollmentWeight = iMaxEnrollmentWeight = 0;
            } else if (iMinEnrollmentWeight != iMaxEnrollmentWeight) {
                if (iMinEnrollmentWeight == enrollment.getRequest().getWeight()) {
                    double newMinEnrollmentWeight = Double.MAX_VALUE;
                    for (Enrollment e : iEnrollments) {
                        if (e.getRequest().getWeight() == iMinEnrollmentWeight) {
                            newMinEnrollmentWeight = iMinEnrollmentWeight;
                            break;
                        } else {
                            newMinEnrollmentWeight = Math.min(newMinEnrollmentWeight, e.getRequest().getWeight());
                        }
                    }
                    iMinEnrollmentWeight = newMinEnrollmentWeight;
                }
                if (iMaxEnrollmentWeight == enrollment.getRequest().getWeight()) {
                    double newMaxEnrollmentWeight = Double.MIN_VALUE;
                    for (Enrollment e : iEnrollments) {
                        if (e.getRequest().getWeight() == iMaxEnrollmentWeight) {
                            newMaxEnrollmentWeight = iMaxEnrollmentWeight;
                            break;
                        } else {
                            newMaxEnrollmentWeight = Math.max(newMaxEnrollmentWeight, e.getRequest().getWeight());
                        }
                    }
                    iMaxEnrollmentWeight = newMaxEnrollmentWeight;
                }
            }
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
            if (excludeRequest != null) {
                Enrollment enrollment = assignment.getValue(excludeRequest);
                if (enrollment!= null && iEnrollments.contains(enrollment) && (enrollment.getReservation() == null || !enrollment.getReservation().canBatchAssignOverLimit()))
                    weight -= excludeRequest.getWeight();
            }
            return weight;
        }
        
        /** Set of assigned enrollments 
         * @return assigned enrollments for this course offering
         **/
        public Set<Enrollment> getEnrollments() {
            return iEnrollments;
        }
        
        /**
         * Maximal weight of a single enrollment in the course
         * @return maximal enrollment weight
         */
        public double getMaxEnrollmentWeight() {
            return iMaxEnrollmentWeight;
        }

        /**
         * Minimal weight of a single enrollment in the course
         * @return minimal enrollment weight
         */
        public double getMinEnrollmentWeight() {
            return iMinEnrollmentWeight;
        }
    }
    
    public double getOnlineBound() {
        double bound = 1.0; 
        for (Config config: getOffering().getConfigs()) {
            int online = config.getNrOnline();
            if (online == 0) return 0.0;
            double factor = ((double)online) / config.getSubparts().size();
            if (factor < bound) bound = factor;
        }
        return bound;
    }
    
    public double getArrHrsBound() {
        double bound = 1.0; 
        for (Config config: getOffering().getConfigs()) {
            int arrHrs = config.getNrArrHours();
            if (arrHrs == 0) return 0.0;
            double factor = ((double)arrHrs) / config.getSubparts().size();
            if (factor < bound) bound = factor;
        }
        return bound;
    }
    
    public double getPastBound() {
        double bound = 1.0; 
        for (Config config: getOffering().getConfigs()) {
            int past = config.getNrPast();
            if (past == 0) return 0.0;
            double factor = ((double)past) / config.getSubparts().size();
            if (factor < bound) bound = factor;
        }
        return bound;
    }
    
    /**
     * Course title
     */
    public String getTitle() {
        return iTitle;
    }

    /**
     * Course title
     */
    public void setTitle(String title) {
        iTitle = title;
    }
    
    /**
     * Course type
     */
    public String getType() {
        return iType;
    }

    /**
     * Course type
     */
    public void setType(String type) {
        iType = type;
    }
}
