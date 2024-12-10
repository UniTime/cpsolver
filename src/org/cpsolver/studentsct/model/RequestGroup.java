package org.cpsolver.studentsct.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AbstractClassWithContext;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.CanInheritContext;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.studentsct.reservation.Reservation;

/**
 * Representation of a group of students requesting the same course that
 * should be scheduled in the same set of sections.<br>
 * <br>
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2015 Tomas Muller<br>
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
public class RequestGroup extends AbstractClassWithContext<Request, Enrollment, RequestGroup.RequestGroupContext>
    implements CanInheritContext<Request, Enrollment, RequestGroup.RequestGroupContext>{
    private long iId = -1; 
    private String iName = null;
    private Course iCourse;
    private Set<CourseRequest> iRequests = new HashSet<CourseRequest>();
    private double iTotalWeight = 0.0;
    
    /**
     * Creates request group. Pair (id, course) must be unique.
     * @param id identification of the group
     * @param name group name
     * @param course course for which the group is created (only course requests for this course can be of this group)
     */
    public RequestGroup(long id, String name, Course course) {
        iId = id;
        iName = name;
        iCourse = course;
        iCourse.getRequestGroups().add(this);
    }
    
    /**
     * Add course request to the group. It has to contain the course of this group {@link RequestGroup#getCourse()}.
     * This is done automatically by {@link CourseRequest#addRequestGroup(RequestGroup)}.
     * @param request course request to be added to this group
     */
    public void addRequest(CourseRequest request) {
        if (iRequests.add(request))
            iTotalWeight += request.getWeight();
    }
    
    /**
     * Remove course request from the group. This is done automatically by {@link CourseRequest#removeRequestGroup(RequestGroup)}.
     * @param request course request to be removed from this group
     */
    public void removeRequest(CourseRequest request) {
        if (iRequests.remove(request))
            iTotalWeight -= request.getWeight();
    }
    
    /**
     * Return the set of course requests that are associated with this group.
     * @return course requests of this group
     */
    public Set<CourseRequest> getRequests() {
        return iRequests;
    }
    
    /**
     * Total weight (using {@link CourseRequest#getWeight()}) of the course requests of this group
     * @return total weight of course requests in this group
     */
    public double getTotalWeight() {
        return iTotalWeight;
    }

    /**
     * Request group id
     * @return request group id
     */
    public long getId() {
        return iId;
    }
    
    /**
     * Request group name
     * @return request group name
     */
    public String getName() {
        return iName;
    }
    
    /**
     * Course associated with this group. Only course requests for this course can be of this group.
     * @return course of this request group
     */
    public Course getCourse() {
        return iCourse;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof RequestGroup)) return false;
        return getId() == ((RequestGroup)o).getId() && getCourse().getId() == ((RequestGroup)o).getCourse().getId();
    }
    
    @Override
    public int hashCode() {
        return (int) (iId ^ (iCourse.getId() >>> 32));
    }
    
    /** Called when an enrollment is assigned to a request of this request group */
    public void assigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        getContext(assignment).assigned(assignment, enrollment);
    }

    /** Called when an enrollment is unassigned from a request of this request group */
    public void unassigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        getContext(assignment).unassigned(assignment, enrollment);
    }    
    
    /**
     * Enrollment weight -- weight of all requests which have an enrollment that
     * is of this request group, excluding the given one. See
     * {@link Request#getWeight()}.
     * @param assignment current assignment
     * @param excludeRequest course request to ignore, if any
     * @return enrollment weight
     */
    public double getEnrollmentWeight(Assignment<Request, Enrollment> assignment, Request excludeRequest) {
        return getContext(assignment).getEnrollmentWeight(assignment, excludeRequest);
    }
    
    /**
     * Section weight -- weight of all requests which have an enrollment that
     * is of this request group and that includes the given section, excluding the given one. See
     * {@link Request#getWeight()}.
     * @param assignment current assignment
     * @param section section in question
     * @param excludeRequest course request to ignore, if any
     * @return enrollment weight
     */
    public double getSectionWeight(Assignment<Request, Enrollment> assignment, Section section, Request excludeRequest) {
        return getContext(assignment).getSectionWeight(assignment, section, excludeRequest);
    }
    
    /**
     * Return how much is the given enrollment similar to other enrollments of this group.
     * @param assignment current assignment 
     * @param enrollment enrollment in question
     * @param bestRatio how much of the weight should be used on estimation of the enrollment potential
     *        (considering that students of this group that are not yet enrolled can take the same enrollment)
     * @param fillRatio how much of the weight should be used in estimation how well are the sections of this enrollments going to be filled
     *        (bestRatio + fillRatio &lt;= 1.0)
     * @return 1.0 if all enrollments have the same sections as the given one, 0.0 if there is no match at all 
     */
    public double getEnrollmentSpread(Assignment<Request, Enrollment> assignment, Enrollment enrollment, double bestRatio, double fillRatio) {
        return getContext(assignment).getEnrollmentSpread(assignment, enrollment, bestRatio, fillRatio);
    }
    
    /**
     * Return average section spread of this group. It reflects the probability of two students of this group
     * being enrolled in the same section. 
     * @param assignment current assignment 
     * @return 1.0 if all enrollments have the same sections as the given one, 0.0 if there is no match at all 
     */
    public double getAverageSpread(Assignment<Request, Enrollment> assignment) {
        return getContext(assignment).getAverageSpread();
    }
    
    /**
     * Return section spread of this group. It reflects the probability of two students of this group
     * being enrolled in this section. 
     * @param assignment current assignment 
     * @param section given section
     * @return 1.0 if all enrollments have the same sections as the given one, 0.0 if there is no match at all 
     */
    public double getSectionSpread(Assignment<Request, Enrollment> assignment, Section section) {
        return getContext(assignment).getSectionSpread(section);
    }

    public class RequestGroupContext implements AssignmentConstraintContext<Request, Enrollment> {
        private Set<Enrollment> iEnrollments = null;
        private double iEnrollmentWeight = 0.0;
        private Map<Long, Double> iSectionWeight = null; 
        private boolean iReadOnly = false;

        public RequestGroupContext(Assignment<Request, Enrollment> assignment) {
            iEnrollments = new HashSet<Enrollment>();
            iSectionWeight = new HashMap<Long, Double>();
            for (CourseRequest request: getCourse().getRequests()) {
                if (request.getRequestGroups().contains(RequestGroup.this)) {
                    Enrollment enrollment = assignment.getValue(request);
                    if (enrollment != null && getCourse().equals(enrollment.getCourse()))
                        assigned(assignment, enrollment);
                }
            }
        }
        
        public RequestGroupContext(RequestGroupContext parent) {
            iEnrollmentWeight = parent.iEnrollmentWeight;
            iEnrollments = parent.iEnrollments;
            iSectionWeight = parent.iSectionWeight;
            iReadOnly = true;
        }

        /** Called when an enrollment is assigned to a request of this request group */
        @Override
        public void assigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
            if (iReadOnly) {
                iEnrollments = new HashSet<Enrollment>(iEnrollments);
                iSectionWeight = new HashMap<Long, Double>(iSectionWeight);
                iReadOnly = false;
            }
            if (iEnrollments.add(enrollment)) {
                iEnrollmentWeight += enrollment.getRequest().getWeight();
                for (Section section: enrollment.getSections()) {
                    Double weight = iSectionWeight.get(section.getId());
                    iSectionWeight.put(section.getId(), enrollment.getRequest().getWeight() + (weight == null ? 0.0 : weight.doubleValue()));
                }
            }
        }

        /** Called when an enrollment is unassigned from a request of this request group */
        @Override
        public void unassigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
            if (iReadOnly) {
                iEnrollments = new HashSet<Enrollment>(iEnrollments);
                iSectionWeight = new HashMap<Long, Double>(iSectionWeight);
                iReadOnly = false;
            }
            if (iEnrollments.remove(enrollment)) {
                iEnrollmentWeight -= enrollment.getRequest().getWeight();
                for (Section section: enrollment.getSections()) {
                    Double weight = iSectionWeight.get(section.getId());
                    iSectionWeight.put(section.getId(), weight - enrollment.getRequest().getWeight());
                }
            }
        }
        
        /** Set of assigned enrollments 
         * @return assigned enrollments of this request group
         **/
        public Set<Enrollment> getEnrollments() {
            return iEnrollments;
        }
        
        /**
         * Enrollment weight -- weight of all requests which have an enrollment that
         * is of this request group, excluding the given one. See
         * {@link Request#getWeight()}.
         * @param assignment current assignment
         * @param excludeRequest course request to ignore, if any
         * @return enrollment weight
         */
        public double getEnrollmentWeight(Assignment<Request, Enrollment> assignment, Request excludeRequest) {
            double weight = iEnrollmentWeight;
            if (excludeRequest != null) {
                Enrollment enrollment = assignment.getValue(excludeRequest);
                if (enrollment!= null && iEnrollments.contains(enrollment))
                    weight -= excludeRequest.getWeight();
            }
            return weight;
        }
        
        /**
         * Section weight -- weight of all requests which have an enrollment that
         * is of this request group and that includes the given section, excluding the given one. See
         * {@link Request#getWeight()}.
         * @param assignment current assignment
         * @param section section in question
         * @param excludeRequest course request to ignore, if any
         * @return enrollment weight
         */
        public double getSectionWeight(Assignment<Request, Enrollment> assignment, Section section, Request excludeRequest) {
            Double weight = iSectionWeight.get(section.getId());
            if (excludeRequest != null && weight != null) {
                Enrollment enrollment = assignment.getValue(excludeRequest);
                if (enrollment!= null && iEnrollments.contains(enrollment) && enrollment.getSections().contains(section))
                    weight -= excludeRequest.getWeight();
            }
            return (weight == null ? 0.0 : weight.doubleValue());
        }
        
        /**
         * Return space available in the given section
         * @param assignment current assignment
         * @param section section to check
         * @param enrollment enrollment in question
         * @return check section reservations, if present; use unreserved space otherwise
         */
        private double getAvailableSpace(Assignment<Request, Enrollment> assignment, Section section, Enrollment enrollment) {
            Reservation reservation = enrollment.getReservation();
            Set<Section> sections = (reservation == null ? null : reservation.getSections(section.getSubpart()));
            if (reservation != null && sections != null && sections.contains(section) && !reservation.isExpired()) {
                double sectionAvailable = (section.getLimit() < 0 ? Double.MAX_VALUE : section.getLimit() - section.getEnrollmentWeight(assignment, enrollment.getRequest()));
                double reservationAvailable = reservation.getReservedAvailableSpace(assignment, enrollment.getConfig(), enrollment.getRequest());
                return Math.min(sectionAvailable, reservationAvailable) + (reservation.mustBeUsed() ? 0.0 : section.getUnreservedSpace(assignment, enrollment.getRequest()));
            } else {
                return section.getUnreservedSpace(assignment, enrollment.getRequest());
            }
        }
        
        /**
         * Return how much is the given enrollment (which is not part of the request group) creating an issue for this request group
         * @param assignment current assignment 
         * @param enrollment enrollment in question
         * @param bestRatio how much of the weight should be used on estimation of the enrollment potential
         *      (considering that students of this group that are not yet enrolled can take the same enrollment) 
         * @param fillRatio how much of the weight should be used in estimation how well are the sections of this enrollments going to be filled 
         *      (bestRatio + fillRatio &lt;= 1.0)
         * @return 1.0 if all enrollments have the same sections as the given one, 0.0 if there is no match at all 
         */
        public double getEnrollmentSpread(Assignment<Request, Enrollment> assignment, Enrollment enrollment, double bestRatio, double fillRatio) {
            if (iTotalWeight <= 1.0) return 1.0;
            
            // enrollment weight (excluding the given enrollment)
            double totalEnrolled = getEnrollmentWeight(assignment, enrollment.getRequest());
            double totalRemaining = iTotalWeight - totalEnrolled;
            
            // section weight (also excluding the given enrollment)
            Enrollment e = assignment.getValue(enrollment.getRequest());
            double enrollmentPairs = 0.0, bestPairs = 0.0, fill = 0.0;
            for (Section section: enrollment.getSections()) {
                double potential = Math.max(Math.min(totalRemaining, getAvailableSpace(assignment, section, enrollment)), enrollment.getRequest().getWeight());
                Double enrolled = iSectionWeight.get(section.getId());
                if (enrolled != null) {
                    if (e != null && e.getSections().contains(section))
                        enrolled -= enrollment.getRequest().getWeight();
                    potential += enrolled;
                    enrollmentPairs += enrolled * (enrolled + 1.0);  
                }
                bestPairs += potential * (potential - 1.0);
                if (section.getLimit() > potential) {
                    fill += potential / section.getLimit();
                } else {
                    fill += 1.0;
                }
            }
            double pEnrl = (totalEnrolled < 1.0 ? 0.0 : (enrollmentPairs / enrollment.getSections().size()) / (totalEnrolled * (totalEnrolled + 1.0)));
            double pBest = (bestPairs / enrollment.getSections().size()) / (iTotalWeight * (iTotalWeight - 1.0));
            double pFill = fill / enrollment.getSections().size();
            
            return (1.0 - bestRatio - fillRatio) * pEnrl + bestRatio * pBest + fillRatio * pFill;
        }
        
        /**
         * Return average section spread of this group. It reflects the probability of two students of this group
         * being enrolled in the same section. 
         * @return 1.0 if all enrollments have the same sections as the given one, 0.0 if there is no match at all 
         */
        public double getAverageSpread() {
            // none or just one enrollment -> all the same
            if (iEnrollmentWeight <= 1.0) return 1.0;
            
            double weight = 0.0;
            for (Config config: getCourse().getOffering().getConfigs()) {
                double pairs = 0.0;
                for (Subpart subpart: config.getSubparts())
                    for (Section section: subpart.getSections()) {
                        Double enrollment = iSectionWeight.get(section.getId());
                        if (enrollment != null && enrollment > 1.0)
                            pairs += enrollment * (enrollment - 1);
                    }
                weight += (pairs / config.getSubparts().size()) / (iEnrollmentWeight * (iEnrollmentWeight - 1.0));
            }
            return weight;
        }
        
        /**
         * Return section spread of this group. It reflects the probability of two students of this group
         * being enrolled in this section. 
         * @param section given section
         * @return 1.0 if all enrollments have the same sections as the given one, 0.0 if there is no match at all 
         */
        public double getSectionSpread(Section section) {
            Double w = iSectionWeight.get(section.getId());
            if (w != null && w > 1.0) {
                return (w * (w - 1.0)) / (iEnrollmentWeight * (iEnrollmentWeight - 1.0));
            } else {
                return 0.0;
            }
        }
    }

    @Override
    public RequestGroupContext createAssignmentContext(Assignment<Request, Enrollment> assignment) {
        return new RequestGroupContext(assignment);
    }

    @Override
    public RequestGroupContext inheritAssignmentContext(Assignment<Request, Enrollment> assignment, RequestGroupContext parentContext) {
        return new RequestGroupContext(parentContext);
    }

    @Override
    public Model<Request, Enrollment> getModel() {
        return getCourse().getModel();
    }
}
