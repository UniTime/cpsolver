package org.cpsolver.studentsct.model;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.extension.DistanceConflict;
import org.cpsolver.studentsct.extension.StudentQuality;
import org.cpsolver.studentsct.extension.TimeOverlapsCounter;
import org.cpsolver.studentsct.reservation.Reservation;


/**
 * Representation of an enrollment of a student into a course. A student needs
 * to be enrolled in a section of each subpart of a selected configuration. When
 * parent-child relation is defined among sections, if a student is enrolled in
 * a section that has a parent section defined, he/she has be enrolled in the
 * parent section as well. Also, the selected sections cannot overlap in time. <br>
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

public class Enrollment extends Value<Request, Enrollment> {
    private static DecimalFormat sDF = new DecimalFormat("0.000");
    private Request iRequest = null;
    private Config iConfig = null;
    private Course iCourse = null;
    private Set<? extends SctAssignment> iAssignments = null;
    private Double iCachedPenalty = null;
    private int iPriority = 0;
    private boolean iNoReservationPenalty = false;
    private Reservation iReservation = null;
    private Long iTimeStamp = null;
    private String iApproval = null;

    /**
     * Constructor
     * 
     * @param request
     *            course / free time request
     * @param priority
     *            zero for the course, one for the first alternative, two for the second alternative
     * @param noReservationPenalty
     *            when true +1 is added to priority (prefer enrollments with reservations)
     * @param course
     *            selected course
     * @param config
     *            selected configuration
     * @param assignments
     *            valid list of sections
     * @param reservation used reservation
     */
    public Enrollment(Request request, int priority, boolean noReservationPenalty, Course course, Config config, Set<? extends SctAssignment> assignments, Reservation reservation) {
        super(request);
        iRequest = request;
        iConfig = config;
        iAssignments = assignments;
        iPriority = priority;
        iCourse = course;
        iNoReservationPenalty = noReservationPenalty;
        if (iConfig != null && iCourse == null)
            for (Course c: ((CourseRequest)iRequest).getCourses()) {
                if (c.getOffering().getConfigs().contains(iConfig)) {
                    iCourse = c;
                    break;
                }
            }
        iReservation = reservation;
    }
    
    /**
     * Constructor
     * 
     * @param request
     *            course / free time request
     * @param priority
     *            zero for the course, one for the first alternative, two for the second alternative
     * @param course
     *            selected course
     * @param config
     *            selected configuration
     * @param assignments
     *            valid list of sections
     * @param reservation used reservation
     */
    public Enrollment(Request request, int priority, Course course, Config config, Set<? extends SctAssignment> assignments, Reservation reservation) {
        this(request, priority, false, course, config, assignments, reservation);
    }
    
    /**
     * Constructor
     * 
     * @param request
     *            course / free time request
     * @param priority
     *            zero for the course, one for the first alternative, two for the second alternative
     * @param config
     *            selected configuration
     * @param assignments
     *            valid list of sections
     * @param assignment current assignment (to guess the reservation)
     */
    public Enrollment(Request request, int priority, Config config, Set<? extends SctAssignment> assignments, Assignment<Request, Enrollment> assignment) {
        this(request, priority, null, config, assignments, null);
        if (assignments != null && assignment != null)
            guessReservation(assignment, true);
    }
    
    /**
     * Guess the reservation based on the enrollment
     * @param assignment current assignment
     * @param onlyAvailable use only reservation that have some space left in them
     */
    public void guessReservation(Assignment<Request, Enrollment> assignment, boolean onlyAvailable) {
        if (iCourse != null) {
            Reservation best = null;
            for (Reservation reservation: ((CourseRequest)iRequest).getReservations(iCourse)) {
                if (reservation.isIncluded(this)) {
                    if (onlyAvailable && reservation.getContext(assignment).getReservedAvailableSpace(assignment, iConfig, iRequest) < iRequest.getWeight() && !reservation.canBatchAssignOverLimit())
                        continue;
                    if (best == null || best.getPriority() > reservation.getPriority()) {
                        best = reservation;
                    } else if (best.getPriority() == reservation.getPriority() &&
                        best.getContext(assignment).getReservedAvailableSpace(assignment, iConfig, iRequest) < reservation.getContext(assignment).getReservedAvailableSpace(assignment, iConfig, iRequest)) {
                        best = reservation;
                    }
                }
            }
            iReservation = best;
        }
    }
    
    /** Student 
     * @return student
     **/
    public Student getStudent() {
        return iRequest.getStudent();
    }

    /** Request 
     * @return request
     **/
    public Request getRequest() {
        return iRequest;
    }

    /** True if the request is course request 
     * @return true if the request if course request
     **/
    public boolean isCourseRequest() {
        return iConfig != null;
    }

    /** Offering of the course request 
     * @return offering of the course request
     **/
    public Offering getOffering() {
        return (iConfig == null ? null : iConfig.getOffering());
    }

    /** Config of the course request 
     * @return config of the course request
     **/
    public Config getConfig() {
        return iConfig;
    }
    
    /** Course of the course request 
     * @return course of the course request
     **/
    public Course getCourse() {
        return iCourse;
    }

    /** List of assignments (selected sections) 
     * @return assignments (selected sections)
     **/
    @SuppressWarnings("unchecked")
    public Set<SctAssignment> getAssignments() {
        return (Set<SctAssignment>) iAssignments;
    }

    /** List of sections (only for course request) 
     * @return selected sections
     **/
    @SuppressWarnings("unchecked")
    public Set<Section> getSections() {
        if (isCourseRequest())
            return (Set<Section>) iAssignments;
        return new HashSet<Section>();
    }

    /** True when this enrollment is overlapping with the given enrollment 
     * @param enrl other enrollment
     * @return true if there is an overlap 
     **/
    public boolean isOverlapping(Enrollment enrl) {
        if (enrl == null || isAllowOverlap() || enrl.isAllowOverlap())
            return false;
        for (SctAssignment a : getAssignments()) {
            if (a.isOverlapping(enrl.getAssignments()))
                return true;
        }
        return false;
    }

    /** Percent of sections that are wait-listed 
     * @return percent of sections that are wait-listed
     **/
    public double percentWaitlisted() {
        if (!isCourseRequest())
            return 0.0;
        CourseRequest courseRequest = (CourseRequest) getRequest();
        int nrWaitlisted = 0;
        for (Section section : getSections()) {
            if (courseRequest.isWaitlisted(section))
                nrWaitlisted++;
        }
        return ((double) nrWaitlisted) / getAssignments().size();
    }

    /** Percent of sections that are selected 
     * @return percent of sections that are selected
     **/
    public double percentSelected() {
        if (!isCourseRequest())
            return 0.0;
        CourseRequest courseRequest = (CourseRequest) getRequest();
        int nrSelected = 0;
        for (Section section : getSections()) {
            if (courseRequest.isSelected(section))
                nrSelected++;
        }
        return ((double) nrSelected) / getAssignments().size();
    }
    
    /** Percent of sections that are selected 
     * @return percent of sections that are selected
     **/
    public double percentSelectedSameSection() {
        if (!isCourseRequest() || getStudent().isDummy()) return (getRequest().hasSelection() ? 1.0 : 0.0);
        CourseRequest courseRequest = (CourseRequest) getRequest();
        int nrSelected = 0;
        Set<Long> nrMatching = new HashSet<Long>();
        sections: for (Section section : getSections()) {
            for (Choice choice: courseRequest.getSelectedChoices()) {
                if (choice.getSubpartId() != null) nrMatching.add(choice.getSubpartId());
                if (choice.sameSection(section)) {
                    nrSelected ++; continue sections;
                }
            }
        }
        return (nrMatching.isEmpty() ? 1.0 : ((double) nrSelected) / nrMatching.size());
    }
    
    /** Percent of sections that have the same configuration 
     * @return percent of sections that are selected
     **/
    public double percentSelectedSameConfig() {
        if (!isCourseRequest() || getStudent().isDummy() || getConfig() == null) return (getRequest().hasSelection() ? 1.0 : 0.0);
        CourseRequest courseRequest = (CourseRequest) getRequest();
        boolean hasConfigSelection = false;
        for (Choice choice: courseRequest.getSelectedChoices()) {
            if (choice.getConfigId() != null) {
                hasConfigSelection = true;
                if (choice.getConfigId().equals(getConfig().getId())) return 1.0;
            }
        }
        return (hasConfigSelection ? 0.0 : 1.0);
    }

    /** Percent of sections that are initial 
     * @return percent of sections that of the initial enrollment
     **/
    public double percentInitial() {
        if (!isCourseRequest())
            return 0.0;
        if (getRequest().getInitialAssignment() == null)
            return 0.0;
        Enrollment inital = getRequest().getInitialAssignment();
        int nrInitial = 0;
        for (Section section : getSections()) {
            if (inital.getAssignments().contains(section))
                nrInitial++;
        }
        return ((double) nrInitial) / getAssignments().size();
    }
    
    /** Percent of sections that have same time as the initial assignment 
     * @return percent of sections that have same time as the initial assignment
     **/
    public double percentSameTime() {
        if (!isCourseRequest())
            return 0.0;
        Enrollment ie = getRequest().getInitialAssignment();
        if (ie != null) {
            int nrInitial = 0;
            sections: for (Section section : getSections()) {
                for (Section initial: ie.getSections()) {
                    if (section.sameInstructionalType(initial) && section.sameTime(initial)) {
                        nrInitial ++;
                        continue sections;
                    }
                }
            }
            return ((double) nrInitial) / getAssignments().size();
        }
        Set<Choice> selected = ((CourseRequest)getRequest()).getSelectedChoices();
        if (!selected.isEmpty()) {
            int nrInitial = 0;
            sections: for (Section section : getSections()) {
                for (Choice choice: selected) {
                    if (choice.sameOffering(section) && choice.sameInstructionalType(section) && choice.sameTime(section)) {
                        nrInitial ++;
                        continue sections;
                    }
                    
                }
            }
            return ((double) nrInitial) / getAssignments().size();
        }
        return 0.0;
    }

    /** True if all the sections are wait-listed 
     * @return all the sections are wait-listed 
     **/
    public boolean isWaitlisted() {
        if (!isCourseRequest())
            return false;
        CourseRequest courseRequest = (CourseRequest) getRequest();
        for (Iterator<? extends SctAssignment> i = getAssignments().iterator(); i.hasNext();) {
            Section section = (Section) i.next();
            if (!courseRequest.isWaitlisted(section))
                return false;
        }
        return true;
    }

    /** True if all the sections are selected 
     * @return all the sections are selected
     **/
    public boolean isSelected() {
        if (!isCourseRequest())
            return false;
        CourseRequest courseRequest = (CourseRequest) getRequest();
        for (Section section : getSections()) {
            if (!courseRequest.isSelected(section))
                return false;
        }
        return true;
    }
    
    public boolean isRequired() {
        if (!isCourseRequest())
            return false;
        CourseRequest courseRequest = (CourseRequest) getRequest();
        for (Section section : getSections()) {
            if (!courseRequest.isRequired(section))
                return false;
        }
        return true;
    }

    /**
     * Enrollment penalty -- sum of section penalties (see
     * {@link Section#getPenalty()})
     * @return online penalty
     */
    public double getPenalty() {
        if (iCachedPenalty == null) {
            double penalty = 0.0;
            if (isCourseRequest()) {
                for (Section section : getSections()) {
                    penalty += section.getPenalty();
                }
            }
            iCachedPenalty = Double.valueOf(penalty / getAssignments().size());
        }
        return iCachedPenalty.doubleValue();
    }

    /** Enrollment value */
    @Override
    public double toDouble(Assignment<Request, Enrollment> assignment) {
        return toDouble(assignment, true);
    }
    
    /** Enrollment value
     * @param assignment current assignment
     * @param precise if false, distance conflicts and time overlaps are ignored (i.e., much faster, but less precise computation)
     * @return enrollment penalty
     **/
    public double toDouble(Assignment<Request, Enrollment> assignment, boolean precise) {
        if (precise) {
            StudentSectioningModel model = (StudentSectioningModel)variable().getModel();
            if (model.getStudentQuality() != null)
                return - getRequest().getWeight() * model.getStudentWeights().getWeight(assignment, this, studentQualityConflicts(assignment));
            else
                return - getRequest().getWeight() * model.getStudentWeights().getWeight(assignment, this, distanceConflicts(assignment), timeOverlappingConflicts(assignment));
        } else {
            Double value = (assignment == null ? null : variable().getContext(assignment).getLastWeight());
            if (value != null) return - value;
            return - getRequest().getWeight() * ((StudentSectioningModel)variable().getModel()).getStudentWeights().getWeight(assignment, this);
        }
    }
    
    /** Enrollment name */
    @Override
    public String getName() {
        if (getRequest() instanceof CourseRequest) {
            Course course = null;
            CourseRequest courseRequest = (CourseRequest) getRequest();
            for (Course c : courseRequest.getCourses()) {
                if (c.getOffering().getConfigs().contains(getConfig())) {
                    course = c;
                    break;
                }
            }
            String ret = (course == null ? getConfig() == null ? "" : getConfig().getName() : course.getName());
            for (Iterator<? extends SctAssignment> i = getAssignments().iterator(); i.hasNext();) {
                Section assignment = (Section) i.next();
                ret += "\n  " + assignment.getLongName(true) + (i.hasNext() ? "," : "");
            }
            return ret;
        } else if (getRequest() instanceof FreeTimeRequest) {
            return "Free Time " + ((FreeTimeRequest) getRequest()).getTime().getLongName(true);
        } else {
            String ret = "";
            for (Iterator<? extends SctAssignment> i = getAssignments().iterator(); i.hasNext();) {
                SctAssignment assignment = i.next();
                ret += assignment.toString() + (i.hasNext() ? "," : "");
                if (i.hasNext())
                    ret += "\n  ";
            }
            return ret;
        }
    }

    public String toString(Assignment<Request, Enrollment> a) {
        if (getAssignments().isEmpty()) return "not assigned";
        Set<DistanceConflict.Conflict> dc = distanceConflicts(a);
        Set<TimeOverlapsCounter.Conflict> toc = timeOverlappingConflicts(a);
        int share = 0;
        if (toc != null)
            for (TimeOverlapsCounter.Conflict c: toc)
                share += c.getShare();
        String ret = toDouble(a) + "/" + sDF.format(getRequest().getBound())
                + (getPenalty() == 0.0 ? "" : "/" + sDF.format(getPenalty()))
                + (dc == null || dc.isEmpty() ? "" : "/dc:" + dc.size())
                + (share <= 0 ? "" : "/toc:" + share);
        if (getRequest() instanceof CourseRequest) {
            double sameGroup = 0.0; int groupCount = 0;
            for (RequestGroup g: ((CourseRequest)getRequest()).getRequestGroups()) {
                if (g.getCourse().equals(getCourse())) {
                    sameGroup += g.getEnrollmentSpread(a, this, 1.0, 0.0);
                    groupCount ++;
                }
            }
            if (groupCount > 0)
                ret += "/g:" + sDF.format(sameGroup / groupCount);
        }
        if (getRequest() instanceof CourseRequest) {
            ret += " ";
            for (Iterator<? extends SctAssignment> i = getAssignments().iterator(); i.hasNext();) {
                SctAssignment assignment = i.next();
                ret += assignment + (i.hasNext() ? ", " : "");
            }
        }
        if (getReservation() != null) ret = "(r) " + ret;
        return ret;
    }
    
    @Override
    public String toString() {
        if (getAssignments().isEmpty()) return "not assigned";
        String ret = sDF.format(getRequest().getBound()) + (getPenalty() == 0.0 ? "" : "/" + sDF.format(getPenalty()));
        if (getRequest() instanceof CourseRequest) {
            ret += " ";
            for (Iterator<? extends SctAssignment> i = getAssignments().iterator(); i.hasNext();) {
                SctAssignment assignment = i.next();
                ret += assignment + (i.hasNext() ? ", " : "");
            }
        }
        if (getReservation() != null) ret = "(r) " + ret;
        if (getRequest().isMPP()) ret += " [i" + sDF.format(100.0 * percentInitial()) + "/t" + sDF.format(100.0 * percentSameTime()) + "]";
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Enrollment))
            return false;
        Enrollment e = (Enrollment) o;
        if (!ToolBox.equals(getCourse(), e.getCourse()))
            return false;
        if (!ToolBox.equals(getConfig(), e.getConfig()))
            return false;
        if (!ToolBox.equals(getRequest(), e.getRequest()))
            return false;
        if (!ToolBox.equals(getAssignments(), e.getAssignments()))
            return false;
        if (!ToolBox.equals(getReservation(), e.getReservation()))
            return false;
        return true;
    }

    /** Distance conflicts, in which this enrollment is involved. 
     * @param assignment current assignment
     * @return distance conflicts
     **/
    public Set<DistanceConflict.Conflict> distanceConflicts(Assignment<Request, Enrollment> assignment) {
        if (!isCourseRequest())
            return null;
        if (getRequest().getModel() instanceof StudentSectioningModel) {
            DistanceConflict dc = ((StudentSectioningModel) getRequest().getModel()).getDistanceConflict();
            if (dc == null) return null;
            return dc.allConflicts(assignment, this);
        } else
            return null;
    }

    /** Time overlapping conflicts, in which this enrollment is involved. 
     * @param assignment current assignment
     * @return time overlapping conflicts
     **/
    public Set<TimeOverlapsCounter.Conflict> timeOverlappingConflicts(Assignment<Request, Enrollment> assignment) {
        if (getRequest().getModel() instanceof StudentSectioningModel) {
            TimeOverlapsCounter toc = ((StudentSectioningModel) getRequest().getModel()).getTimeOverlaps();
            if (toc == null)
                return null;
            return toc.allConflicts(assignment, this);
        } else
            return null;
    }
    
    public Set<StudentQuality.Conflict> studentQualityConflicts(Assignment<Request, Enrollment> assignment) {
        if (!isCourseRequest())
            return null;
        if (getRequest().getModel() instanceof StudentSectioningModel) {
            StudentQuality sq = ((StudentSectioningModel) getRequest().getModel()).getStudentQuality();
            if (sq == null) return null;
            return sq.allConflicts(assignment, this);
        } else
            return null;
    }

    /** 
     * Return enrollment priority
     * @return zero for the course, one for the first alternative, two for the second alternative
     */
    public int getPriority() {
        return iPriority + (iNoReservationPenalty ? 1 : 0);
    }
    
    /** 
     * Return enrollment priority, ignoring priority bump provided by reservations
     * @return zero for the course, one for the first alternative, two for the second alternative
     */
    public int getTruePriority() {
        return iPriority;
    }
    
    /** 
     * Return adjusted enrollment priority, including priority bump provided by reservations
     * (but ensuring that getting the course without a reservation is still better than getting an alternative) 
     * @return zero for the course, two for the first alternative, four for the second alternative; plus one when the no reservation penalty applies
     */
    public int getAdjustedPriority() {
        return 2 * iPriority + (iNoReservationPenalty ? 1 : 0);
    }
    
    /**
     * Return total number of slots of all sections in the enrollment.
     * @return number of slots used
     */
    public int getNrSlots() {
        int ret = 0;
        for (SctAssignment a: getAssignments()) {
            if (a.getTime() != null) ret += a.getTime().getLength() * a.getTime().getNrMeetings();
        }
        return ret;
    }
    
    /**
     * Return reservation used for this enrollment
     * @return used reservation
     */
    public Reservation getReservation() { return iReservation; }
    
    /**
     * Set reservation for this enrollment
     * @param reservation used reservation
     */
    public void setReservation(Reservation reservation) { iReservation = reservation; }
    
    /**
     * Time stamp of the enrollment
     * @return enrollment time stamp
     */
    public Long getTimeStamp() {
        return iTimeStamp;
    }

    /**
     * Time stamp of the enrollment
     * @param timeStamp enrollment time stamp
     */
    public void setTimeStamp(Long timeStamp) {
        iTimeStamp = timeStamp;
    }

    /**
     * Approval of the enrollment (only used by the online student sectioning)
     * @return consent approval
     */
    public String getApproval() {
        return iApproval;
    }

    /**
     * Approval of the enrollment (only used by the online student sectioning)
     * @param approval consent approval
     */
    public void setApproval(String approval) {
        iApproval = approval;
    }
    
    /**
     * True if this enrollment can overlap with other enrollments of the student.
     * @return can overlap with other enrollments of the student
     */
    public boolean isAllowOverlap() {
        return (getReservation() != null && getReservation().isAllowOverlap());
    }
    
    /**
     * Enrollment limit, i.e., the number of students that would be able to get into the offering using this enrollment (if all the sections are empty)  
     * @return enrollment limit
     */
    public int getLimit() {
        if (!isCourseRequest()) return -1; // free time requests have no limit
        Integer limit = null;
        for (Section section: getSections())
            if (section.getLimit() >= 0) {
                if (limit == null)
                    limit = section.getLimit();
                else
                    limit = Math.min(limit, section.getLimit());
            }
        return (limit == null ? -1 : limit);
    }
    
    /**
     * Credit of this enrollment (using either {@link Course#getCreditValue()} or {@link Subpart#getCreditValue()} when course credit is not present)
     * @return credit of this enrollment
     */
    public float getCredit() {
        if (getCourse() == null) return 0f;
        if (getAssignments().isEmpty()) return 0f;
        if (getCourse().hasCreditValue()) return getCourse().getCreditValue();
        float subpartCredit = 0f;
        for (Subpart subpart: getConfig().getSubparts()) {
            if (subpart.hasCreditValue()) subpartCredit += subpart.getCreditValue();
        }
        return subpartCredit;
    }
    
}