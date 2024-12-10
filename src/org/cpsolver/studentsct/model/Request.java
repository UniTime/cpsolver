package org.cpsolver.studentsct.model;

import java.util.List;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentContext;
import org.cpsolver.ifs.assignment.context.VariableWithContext;
import org.cpsolver.studentsct.StudentSectioningModel;


/**
 * Representation of a request of a student for a course(s) or a free time. This
 * can be either {@link CourseRequest} or {@link FreeTimeRequest}. Each request
 * contains id, priority, weight, and a student. A request can be also marked as
 * alternative. <br>
 * <br>
 * For each student, all non-alternative requests should be satisfied (an
 * enrollment is assigned to a request). If not, an alternative request can be
 * assigned instead of a non-alternative course request. In the case when only
 * one of two requests can be assigned, the one with the lowest priority is
 * preferred. <br>
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
public abstract class Request extends VariableWithContext<Request, Enrollment, Request.RequestContext> {
    private long iId = -1;
    private int iPriority = 0;
    private boolean iAlternative = false;
    private Student iStudent = null;
    private double iWeight = 1.0;
    /** True means that method {@link Request#values()} will cache its results. */
    public static boolean sCacheValues = false;

    /**
     * Constructor
     * 
     * @param id
     *            course/free time request unique id
     * @param priority
     *            request priority -- if there is a choice, request with lower
     *            priority is more preferred to be assigned
     * @param alternative
     *            true if the request is alternative (alternative request can be
     *            assigned instead of a non-alternative course requests, if it
     *            is left unassigned)
     * @param student
     *            student to which this request belongs
     */
    public Request(long id, int priority, boolean alternative, Student student) {
        iId = id;
        iPriority = priority;
        iAlternative = alternative;
        iStudent = student;
        iStudent.getRequests().add(this);
    }

    /** Request id */
    @Override
    public long getId() {
        return iId;
    }

    /**
     * Request priority -- if there is a choice, request with lower priority is
     * more preferred to be assigned
     * @return request priority
     */
    public int getPriority() {
        return iPriority;
    }

    /** Set request priority 
     * @param priority request priority
     **/
    public void setPriority(int priority) {
        iPriority = priority;
    }

    /**
     * True, if the request is alternative (alternative request can be assigned
     * instead of a non-alternative course requests, if it is left unassigned)
     * @return is alternative request
     */
    public boolean isAlternative() {
        return iAlternative;
    }

    /** Student to which this request belongs 
     * @return student
     **/
    public Student getStudent() {
        return iStudent;
    }

    /**
     * Compare to requests, non-alternative requests go first, otherwise use
     * priority (a request with lower priority goes first)
     */
    @Override
    public int compareTo(Request r) {
        if (getStudent().getId() == r.getStudent().getId())
            return (isAlternative() != r.isAlternative() ? isAlternative() ? 1 : -1 : getPriority() < r.getPriority() ? -1 : 1);
        else
            return getStudent().compareTo(r.getStudent());
    }

    /** Compute available enrollments 
     * @param assignment current assignment
     * @return list of all enrollments
     **/
    public abstract List<Enrollment> computeEnrollments(Assignment<Request, Enrollment> assignment);

    /**
     * Domain of this variable -- list of available enrollments. Method
     * {@link Request#computeEnrollments(Assignment)} is used.
     */
    @Override
    public List<Enrollment> values(Assignment<Request, Enrollment> assignment) {
        List<Enrollment> values = super.values(assignment);
        if (values != null)
            return values;
        values = computeEnrollments(assignment);
        if (sCacheValues)
            setValues(values);
        return values;
    }

    /**
     * Assign given enrollment to this request. This method also calls
     * {@link SctAssignment#assigned(Assignment, Enrollment)} on for all the assignments of the
     * enrollment.
     */
    @Override
    public void variableAssigned(Assignment<Request, Enrollment> assignment, long iteration, Enrollment enrollment) {
        super.variableAssigned(assignment, iteration, enrollment);
        for (SctAssignment a : enrollment.getAssignments()) {
            a.assigned(assignment, enrollment);
        }
        if (enrollment.getConfig() != null)
            enrollment.getConfig().getContext(assignment).assigned(assignment, enrollment);
        if (enrollment.getCourse() != null)
            enrollment.getCourse().assigned(assignment, enrollment);
        if (enrollment.getReservation() != null)
            enrollment.getReservation().getContext(assignment).assigned(assignment, enrollment);
    }

    /**
     * Unassign currently assigned enrollment from this request. This method
     * also calls {@link SctAssignment#unassigned(Assignment, Enrollment)} on for all the
     * assignments of the current enrollment.
     */
    @Override
    public void variableUnassigned(Assignment<Request, Enrollment> assignment, long iteration, Enrollment enrollment) {
        super.variableUnassigned(assignment, iteration, enrollment);
        for (SctAssignment a : enrollment.getAssignments()) {
            a.unassigned(assignment, enrollment);
        }
        if (enrollment.getConfig() != null)
            enrollment.getConfig().getContext(assignment).unassigned(assignment, enrollment);
        if (enrollment.getCourse() != null)
            enrollment.getCourse().unassigned(assignment, enrollment);
        if (enrollment.getReservation() != null)
            enrollment.getReservation().getContext(assignment).unassigned(assignment, enrollment);
    }

    /** Get bound, i.e., the value of the best possible enrollment 
     * @return uppper bound
     **/
    public abstract double getBound();

    /**
     * Request weight, set by default to 1.0, defines the amount of space which
     * will be taken in the section by this request.
     * @return request weight
     */
    public double getWeight() {
        return iWeight;
    }

    /**
     * Set request weight. It defines the amount of space which will be taken in
     * the section by this request.
     * @param weight request weight
     */
    public void setWeight(double weight) {
        iWeight = weight;
    }

    /** Return true if request is assigned. 
     * @param assignment current assignment
     * @return true if this request is assigned 
     **/
    public boolean isAssigned(Assignment<Request, Enrollment> assignment) {
        return assignment.getValue(this) != null;
    }
    
    @Override
    public int hashCode() {
        return (int) (iId ^ (iId >>> 32) ^ getStudent().getId() ^ (getStudent().getId() >>> 32));
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Request)) return false;
        return getId() == ((Request)o).getId() && getStudent().getId() == ((Request)o).getStudent().getId();
    }
    
    public class RequestContext implements AssignmentContext {
        private Double iWeight = null;
        
        public RequestContext(Assignment<Request, Enrollment> assignment) {
            Enrollment enrollment = assignment.getValue(Request.this);
            if (enrollment != null)
                setLastWeight(enrollment.getRequest().getWeight() * ((StudentSectioningModel)getModel()).getStudentWeights().getWeight(assignment, enrollment));
        }
        
        public Double getLastWeight() { return iWeight; }
        public void setLastWeight(Double weight) { iWeight = weight; }
    }
    

    @Override
    public RequestContext createAssignmentContext(Assignment<Request, Enrollment> assignment) {
        return new RequestContext(assignment);
    }
    
    /**
     * Return true if this request can track MPP
     * @return true if the request is course request and it either has an initial enrollment or some selected choices.
     */
    public boolean isMPP() {
        return false;
    }
    
    /**
     * Return true if this request has any selection
     * @return true if the request is course request and has some selected choices.
     */
    public boolean hasSelection() {
        return false;
    }
    
    /**
     * Smallest credit provided by this request
     */
    public abstract float getMinCredit();
    
    /**
     * Is this request critical for the student to progress towards his/her degree
     */
    @Deprecated
    public boolean isCritical() {
        return getRequestPriority() != null && getRequestPriority().isCritical();
    }
    
    /**
     * Importance of the request. Higher priority requests are assigned before lower priority requests.
     */
    public abstract RequestPriority getRequestPriority();
    
    /**
     * Importance of the request for the student to progress towards his/her degree.
     * The request priority is used to re-order course priorities (if desired),
     * and to assign requests of a higher priority first (before requests of a lower priority).
     */
    public static enum RequestPriority {
        LC("l", 1.0),
        Critical("c", 1.0),
        Vital("v", 0.8),
        Important("i", 0.5),
        Normal("", null), // this is the default priority
        VisitingF2F("f", null), // low priority for face-to-face courses of visiting students
        ;
        
        String iAbbv;
        Double iBoost;
        RequestPriority(String abbv, Double boost) {
            iAbbv = abbv;
            iBoost = boost;
        }
        public String getAbbreviation() { return iAbbv; }
        public Double getBoost() { return iBoost; }
        public boolean isCritical(Request r) {
            return r.getRequestPriority().ordinal() <= ordinal();
        }
        public boolean isHigher(Request r) {
            return ordinal() < r.getRequestPriority().ordinal();
        }
        public boolean isSame(Request r) {
            return ordinal() == r.getRequestPriority().ordinal();
        }
        public boolean isCritical() {
            return ordinal() < Normal.ordinal();
        }
        public int compareCriticalsTo(RequestPriority rp) {
            if (!isCritical() && !rp.isCritical()) return 0; // do not compare non-critical levels
            return compareTo(rp);
        }
    }
}
