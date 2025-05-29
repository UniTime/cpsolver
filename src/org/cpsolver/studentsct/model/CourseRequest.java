package org.cpsolver.studentsct.model;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.AssignmentComparator;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.constraint.ConfigLimit;
import org.cpsolver.studentsct.constraint.CourseLimit;
import org.cpsolver.studentsct.constraint.LinkedSections;
import org.cpsolver.studentsct.constraint.SectionLimit;
import org.cpsolver.studentsct.reservation.Reservation;
import org.cpsolver.studentsct.reservation.Restriction;


/**
 * Representation of a request of a student for one or more course. A student
 * requests one of the given courses, preferably the first one. <br>
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
public class CourseRequest extends Request {
    private static DecimalFormat sDF = new DecimalFormat("0.000");
    private List<Course> iCourses = null;
    private Set<Choice> iWaitlistedChoices = new HashSet<Choice>();
    private Set<Choice> iSelectedChoices = new HashSet<Choice>();
    private Set<Choice> iRequiredChoices = new HashSet<Choice>();
    private boolean iWaitlist = false;
    private Long iTimeStamp = null;
    private Double iCachedMinPenalty = null, iCachedMaxPenalty = null;
    public static boolean sSameTimePrecise = false;
    private Set<RequestGroup> iRequestGroups = new HashSet<RequestGroup>();
    private RequestPriority iPriority = RequestPriority.Normal;
    private Enrollment iFixed = null;

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
     * @param courses
     *            list of requested courses (in the correct order -- first is
     *            the requested course, second is the first alternative, etc.)
     * @param waitlist
     *            time stamp of the request if the student can be put on a wait-list (no alternative
     *            course request will be given instead)
     * @param critical
     *            is the course request is critical for the student in order to move forward in their degree 
     * @param timeStamp request time stamp
     */
    public CourseRequest(long id, int priority, boolean alternative, Student student, java.util.List<Course> courses, boolean waitlist, boolean critical, Long timeStamp) {
        super(id, priority, alternative, student);
        iCourses = new ArrayList<Course>(courses);
        for (Course course: iCourses)
            course.getRequests().add(this);
        iWaitlist = waitlist;
        iPriority = (critical ? RequestPriority.Critical : RequestPriority.Normal);
        iTimeStamp = timeStamp;
    }
    
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
     * @param courses
     *            list of requested courses (in the correct order -- first is
     *            the requested course, second is the first alternative, etc.)
     * @param waitlist
     *            time stamp of the request if the student can be put on a wait-list (no alternative
     *            course request will be given instead)
     * @param importance
     *            request priority 
     * @param timeStamp request time stamp
     */
    public CourseRequest(long id, int priority, boolean alternative, Student student, java.util.List<Course> courses, boolean waitlist, RequestPriority importance, Long timeStamp) {
        super(id, priority, alternative, student);
        iCourses = new ArrayList<Course>(courses);
        for (Course course: iCourses)
            course.getRequests().add(this);
        iWaitlist = waitlist;
        iPriority = importance;
        iTimeStamp = timeStamp;
    }
    
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
     * @param courses
     *            list of requested courses (in the correct order -- first is
     *            the requested course, second is the first alternative, etc.)
     * @param waitlist
     *            time stamp of the request if the student can be put on a wait-list (no alternative
     *            course request will be given instead)
     * @param timeStamp request time stamp
     */
    public CourseRequest(long id, int priority, boolean alternative, Student student, java.util.List<Course> courses, boolean waitlist, Long timeStamp) {
        this(id, priority, alternative, student, courses, waitlist, false, timeStamp);
    }

    /**
     * List of requested courses (in the correct order -- first is the requested
     * course, second is the first alternative, etc.)
     * @return requested course offerings
     */
    public List<Course> getCourses() {
        return iCourses;
    }

    /**
     * Create enrollment for the given list of sections. The list of sections
     * needs to be correct, i.e., a section for each subpart of a configuration
     * of one of the requested courses.
     * @param sections selected sections
     * @param reservation selected reservation
     * @return enrollment
     */
    public Enrollment createEnrollment(Set<? extends SctAssignment> sections, Reservation reservation) {
        if (sections == null || sections.isEmpty())
            return null;
        Config config = ((Section) sections.iterator().next()).getSubpart().getConfig();
        Course course = null;
        for (Course c: iCourses) {
            if (c.getOffering().getConfigs().contains(config)) {
                course = c;
                break;
            }
        }
        return new Enrollment(this, iCourses.indexOf(course), course, config, sections, reservation);
    }
    
    /**
     * Create enrollment for the given list of sections. The list of sections
     * needs to be correct, i.e., a section for each subpart of a configuration
     * of one of the requested courses.
     * @param course selected course
     * @param sections selected sections
     * @param reservation selected reservation
     * @return enrollment
     */
    public Enrollment createEnrollment(Course course, Set<? extends SctAssignment> sections, Reservation reservation) {
        if (sections == null || sections.isEmpty())
            return null;
        Config config = ((Section) sections.iterator().next()).getSubpart().getConfig();
        return new Enrollment(this, iCourses.indexOf(course), course, config, sections, reservation);
    }

    /**
     * Create enrollment for the given list of sections. The list of sections
     * needs to be correct, i.e., a section for each subpart of a configuration
     * of one of the requested courses.
     * @param assignment current assignment (to guess the reservation)
     * @param sections selected sections
     * @return enrollment
     */
    public Enrollment createEnrollment(Assignment<Request, Enrollment> assignment, Set<? extends SctAssignment> sections) {
        Enrollment ret = createEnrollment(sections, null);
        ret.guessReservation(assignment, true);
        return ret;
        
    }
    
    /**
     * Maximal domain size (i.e., number of enrollments of a course request), -1 if there is no limit.
     * @return maximal domain size, -1 if unlimited
     */
    protected int getMaxDomainSize() {
        StudentSectioningModel model = (StudentSectioningModel) getModel();
        return model == null ? -1 : model.getMaxDomainSize();
    }
    
    /**
     * Return all possible enrollments.
     */
    @Override
    public List<Enrollment> computeEnrollments(Assignment<Request, Enrollment> assignment) {
        List<Enrollment> ret = new ArrayList<Enrollment>();
        if (isFixed()) {
            ret.add(getFixedValue());
            return ret;
        }
        if (getInitialAssignment() != null && getModel() != null && ((StudentSectioningModel)getModel()).isMPP() && ((StudentSectioningModel)getModel()).getKeepInitialAssignments()) {
            ret.add(getInitialAssignment());
            return ret;
        }
        int idx = 0;
        for (Course course : iCourses) {
            for (Config config : course.getOffering().getConfigs()) {
                computeEnrollments(assignment, ret, idx, 0, course, config, new HashSet<Section>(), 0, false, false,
                        false, false, getMaxDomainSize() <= 0 ? -1 : ret.size() + getMaxDomainSize());
            }
            idx++;
        }
        return ret;
    }

    /**
     * Return a subset of all enrollments -- randomly select only up to
     * limitEachConfig enrollments of each config.
     * @param assignment current assignment
     * @param limitEachConfig maximal number of enrollments in each configuration
     * @return computed enrollments
     */
    public List<Enrollment> computeRandomEnrollments(Assignment<Request, Enrollment> assignment, int limitEachConfig) {
        List<Enrollment> ret = new ArrayList<Enrollment>();
        if (isFixed()) {
            ret.add(getFixedValue());
            return ret;
        }
        if (getInitialAssignment() != null && getModel() != null && ((StudentSectioningModel)getModel()).isMPP() && ((StudentSectioningModel)getModel()).getKeepInitialAssignments()) {
            ret.add(getInitialAssignment());
            return ret;
        }
        int idx = 0;
        for (Course course : iCourses) {
            for (Config config : course.getOffering().getConfigs()) {
                computeEnrollments(assignment, ret, idx, 0, course, config, new HashSet<Section>(), 0, false, false,
                        false, true, (limitEachConfig <= 0 ? limitEachConfig : ret.size() + limitEachConfig));
            }
            idx++;
        }
        return ret;
    }

    /**
     * Return true if the both sets of sections contain sections of the same
     * subparts, and each pair of sections of the same subpart is offered at the
     * same time.
     */
    private boolean sameTimes(Set<Section> sections1, Set<Section> sections2) {
        for (Section s1 : sections1) {
            Section s2 = null;
            for (Section s : sections2) {
                if (s.getSubpart().equals(s1.getSubpart())) {
                    s2 = s;
                    break;
                }
            }
            if (s2 == null)
                return false;
            if (!ToolBox.equals(s1.getTime(), s2.getTime()))
                return false;
        }
        return true;
    }

    /**
     * Recursive computation of enrollments
     * 
     * @param enrollments
     *            list of enrollments to be returned
     * @param priority
     *            zero for the course, one for the first alternative, two for the second alternative
     * @param penalty
     *            penalty of the selected sections
     * @param course
     *            selected course
     * @param config
     *            selected configuration
     * @param sections
     *            sections selected so far
     * @param idx
     *            index of the subparts (a section of 0..idx-1 subparts has been
     *            already selected)
     * @param availableOnly
     *            only use available sections
     * @param skipSameTime
     *            for each possible times, pick only one section
     * @param selectedOnly
     *            select only sections that are selected (
     *            {@link CourseRequest#isSelected(Section)} is true)
     * @param random
     *            pick sections in a random order (useful when limit is used)
     * @param limit
     *            when above zero, limit the number of selected enrollments to
     *            this limit
     * @param ignoreDisabled
     *            are sections that are disabled for student scheduling allowed to be used
     * @param reservations
     *            list of applicable reservations
     */
    private void computeEnrollments(Assignment<Request, Enrollment> assignment, Collection<Enrollment> enrollments, int priority, double penalty, Course course, Config config,
            HashSet<Section> sections, int idx, boolean availableOnly, boolean skipSameTime, boolean selectedOnly,
            boolean random, int limit) {
        if (limit > 0 && enrollments.size() >= limit)
            return;
        if (idx == 0) { // run only once for each configuration
            if (isNotAllowed(course, config)) return;
            boolean canOverLimit = false;
            if (availableOnly) {
                for (Reservation r: getReservations(course)) {
                    if (!r.canBatchAssignOverLimit()) continue;
                    if (r.neverIncluded()) continue;
                    if (!r.getConfigs().isEmpty() && !r.getConfigs().contains(config)) continue;
                    if (r.getReservedAvailableSpace(assignment, this) < getWeight()) continue;
                    if (r.getReservedAvailableSpace(assignment, config, this) < getWeight()) continue;
                    canOverLimit = true; break;
                }
            }
            if (!canOverLimit) {
                if (availableOnly && config.getLimit() >= 0 && ConfigLimit.getEnrollmentWeight(assignment, config, this) > config.getLimit())
                    return;
                if (availableOnly && course.getLimit() >= 0 && CourseLimit.getEnrollmentWeight(assignment, course, this) > course.getLimit())
                    return;
                if (config.getOffering().hasReservations()) {
                    boolean hasReservation = false, hasConfigReservation = false, reservationMustBeUsed = false;
                    for (Reservation r: getReservations(course)) {
                        if (r.mustBeUsed()) reservationMustBeUsed = true;
                        if (availableOnly && r.getReservedAvailableSpace(assignment, this) < getWeight()) continue;
                        if (availableOnly && r.getReservedAvailableSpace(assignment, config, this) < getWeight()) continue;
                        if (r.neverIncluded()) {
                        } else if (r.getConfigs().isEmpty()) {
                            hasReservation = true;
                        } else if (r.getConfigs().contains(config)) {
                            hasReservation = true;
                            hasConfigReservation = true;
                        } else if (!r.areRestrictionsInclusive()) {
                            hasReservation = true;
                        }
                    }
                    if (!hasConfigReservation && config.getTotalUnreservedSpace() < getWeight())
                        return;
                    if (!hasReservation && config.getOffering().getTotalUnreservedSpace() < getWeight())
                        return;
                    if (availableOnly && !hasReservation && config.getOffering().getUnreservedSpace(assignment, this) < getWeight())
                        return;
                    if (availableOnly && !hasConfigReservation && config.getUnreservedSpace(assignment, this) < getWeight())
                        return;
                    if (!hasReservation && reservationMustBeUsed)
                        return;
                }
            }
        }
        if (config.getSubparts().size() == idx) {
            if (skipSameTime && sSameTimePrecise) {
                boolean waitListedOrSelected = false;
                if (!getSelectedChoices().isEmpty() || !getWaitlistedChoices().isEmpty()) {
                    for (Section section : sections) {
                        if (isWaitlisted(section) || isSelected(section)) {
                            waitListedOrSelected = true;
                            break;
                        }
                    }
                }
                if (!waitListedOrSelected) {
                    for (Enrollment enrollment : enrollments) {
                        if (sameTimes(enrollment.getSections(), sections))
                            return;
                    }
                }
            }
            Enrollment e = new Enrollment(this, priority, course, config, new HashSet<SctAssignment>(sections), null);
            if (isNotAllowed(e)) {
            } else if (!config.getOffering().hasReservations()) {
                enrollments.add(e);
            } else {
                boolean mustHaveReservation = config.getOffering().getTotalUnreservedSpace() < getWeight();
                boolean mustHaveConfigReservation = config.getTotalUnreservedSpace() < getWeight();
                boolean mustHaveSectionReservation = false;
                boolean containDisabledSection = false;
                for (Section s: sections) {
                    if (s.getTotalUnreservedSpace() < getWeight()) {
                        mustHaveSectionReservation = true;
                    }
                    if (!getStudent().isAllowDisabled() && !s.isEnabled(getStudent())) {
                        containDisabledSection = true;
                    }
                }
                boolean canOverLimit = false;
                if (availableOnly) {
                    for (Reservation r: getReservations(course)) {
                        if (!r.canBatchAssignOverLimit() || !r.isIncluded(e)) continue;
                        if (r.getReservedAvailableSpace(assignment, config, this) < getWeight()) continue;
                        if (containDisabledSection && !r.isAllowDisabled()) continue;
                        enrollments.add(new Enrollment(this, priority, null, config, new HashSet<SctAssignment>(sections), r));
                        canOverLimit = true;
                    }
                }
                if (!canOverLimit) {
                    boolean reservationMustBeUsed = false;
                    reservations: for (Reservation r: (availableOnly ? getSortedReservations(assignment, course) : getReservations(course))) {
                        if (r.mustBeUsed()) reservationMustBeUsed = true;
                        if (!r.isIncluded(e)) continue;
                        if (availableOnly && r.getReservedAvailableSpace(assignment, config, this) < getWeight()) continue;
                        if (mustHaveConfigReservation && r.getConfigs().isEmpty()) continue;
                        if (mustHaveSectionReservation)
                            for (Section s: sections)
                                if (r.getSections(s.getSubpart()) == null && s.getTotalUnreservedSpace() < getWeight()) continue reservations;
                        if (containDisabledSection && !r.isAllowDisabled()) continue;
                        enrollments.add(new Enrollment(this, priority, null, config, new HashSet<SctAssignment>(sections), r));
                        if (availableOnly) return; // only one available reservation suffice (the best matching one)
                    }
                    // a case w/o reservation
                    if (!(mustHaveReservation || mustHaveConfigReservation || mustHaveSectionReservation) &&
                        !(availableOnly && config.getOffering().getUnreservedSpace(assignment, this) < getWeight()) &&
                        !reservationMustBeUsed && !containDisabledSection) {
                        enrollments.add(new Enrollment(this, priority, !getReservations(course).isEmpty(), null, config, new HashSet<SctAssignment>(sections), null));
                    }
                }
            }
        } else {
            Subpart subpart = config.getSubparts().get(idx);
            HashSet<TimeLocation> times = (skipSameTime ? new HashSet<TimeLocation>() : null);
            List<Section> sectionsThisSubpart = subpart.getSections();
            if (skipSameTime) {
                sectionsThisSubpart = new ArrayList<Section>(subpart.getSections());
                Collections.sort(sectionsThisSubpart, new AssignmentComparator<Section, Request, Enrollment>(assignment));
            }
            List<Section> matchingSectionsThisSubpart = new ArrayList<Section>(subpart.getSections().size());
            boolean hasChildren = !subpart.getChildren().isEmpty();
            for (Section section : sectionsThisSubpart) {
                if (section.isCancelled())
                    continue;
                if (!isRequired(section))
                    continue;
                if (getInitialAssignment() != null && (getModel() != null && ((StudentSectioningModel)getModel()).getKeepInitialAssignments()) &&
                        !getInitialAssignment().getAssignments().contains(section))
                    continue;
                if (isFixed() && !getFixedValue().getAssignments().contains(section))
                    continue;
                if (section.getParent() != null && !sections.contains(section.getParent()))
                    continue;
                if (section.isOverlapping(sections))
                    continue;
                if (selectedOnly && hasSelection(section) && !isSelected(section))
                    continue;
                if (isNotAllowed(course, section))
                    continue;
                if (!getStudent().isAvailable(section)) {
                    boolean canOverlap = false;
                    for (Reservation r: getReservations(course)) {
                        if (!r.isAllowOverlap()) continue;
                        if (r.getSections(subpart) != null && !r.getSections(subpart).contains(section)) continue;
                        if (r.getReservedAvailableSpace(assignment, config, this) < getWeight()) continue;
                        canOverlap = true; break;
                    }
                    if (!canOverlap) continue;
                }
                boolean canOverLimit = false;
                if (availableOnly) {
                    for (Reservation r: getReservations(course)) {
                        if (!r.canBatchAssignOverLimit()) continue;
                        if (r.getSections(subpart) != null && !r.getSections(subpart).contains(section)) continue;
                        if (r.getReservedAvailableSpace(assignment, config, this) < getWeight()) continue;
                        canOverLimit = true; break;
                    }
                }
                if (!canOverLimit) {
                    if (availableOnly && section.getLimit() >= 0
                            && SectionLimit.getEnrollmentWeight(assignment, section, this) > section.getLimit())
                        continue;
                    if (config.getOffering().hasReservations()) {
                        boolean hasReservation = false, hasSectionReservation = false, reservationMustBeUsed = false;
                        for (Reservation r: getReservations(course)) {
                            if (r.mustBeUsed()) reservationMustBeUsed = true;
                            if (availableOnly && r.getReservedAvailableSpace(assignment, config, this) < getWeight()) continue;
                            if (r.getSections(subpart) == null) {
                                hasReservation = true;
                            } else if (r.getSections(subpart).contains(section)) {
                                hasReservation = true;
                                hasSectionReservation = true;
                            }
                        }
                        if (!hasSectionReservation && section.getTotalUnreservedSpace() < getWeight())
                            continue;
                        if (availableOnly && !hasSectionReservation && section.getUnreservedSpace(assignment, this) < getWeight())
                            continue;
                        if (!hasReservation && reservationMustBeUsed)
                            continue;
                    }
                }
                if (!getStudent().isAllowDisabled() && !section.isEnabled(getStudent())) {
                    boolean allowDisabled = false;
                    for (Reservation r: getReservations(course)) {
                        if (!r.isAllowDisabled()) continue;
                        if (r.getSections(subpart) != null && !r.getSections(subpart).contains(section)) continue;
                        if (!r.getConfigs().isEmpty() && !r.getConfigs().contains(config)) continue;
                        allowDisabled = true; break;
                    }
                    if (!allowDisabled) continue;
                }
                if (skipSameTime && section.getTime() != null && !hasChildren && !times.add(section.getTime()) && !isSelected(section) && !isWaitlisted(section) && 
                        (section.getIgnoreConflictWithSectionIds() == null || section.getIgnoreConflictWithSectionIds().isEmpty()))
                    continue;
                matchingSectionsThisSubpart.add(section);
            }
            if (random || limit > 0) {
                sectionsThisSubpart = new ArrayList<Section>(sectionsThisSubpart);
                Collections.shuffle(sectionsThisSubpart);
            }
            int i = 0;
            for (Section section: matchingSectionsThisSubpart) {
                sections.add(section);
                computeEnrollments(assignment, enrollments, priority, penalty + section.getPenalty(), course, config, sections, idx + 1,
                        availableOnly, skipSameTime, selectedOnly, random,
                        limit < 0 ? limit : Math.max(1, limit * (1 + i) / matchingSectionsThisSubpart.size()));
                sections.remove(section);
                i++;
            }
        }
    }

    /** Return all enrollments that are available 
     * @param assignment current assignment
     * @return all available enrollments
     **/
    public List<Enrollment> getAvaiableEnrollments(Assignment<Request, Enrollment> assignment) {
        List<Enrollment> ret = new ArrayList<Enrollment>();
        if (isFixed()) {
            ret.add(getFixedValue());
            return ret;
        }
        if (getInitialAssignment() != null && getModel() != null && ((StudentSectioningModel)getModel()).isMPP() && ((StudentSectioningModel)getModel()).getKeepInitialAssignments()) {
            ret.add(getInitialAssignment());
            return ret;
        }
        int idx = 0;
        for (Course course : iCourses) {
            for (Config config : course.getOffering().getConfigs()) {
                computeEnrollments(assignment, ret, idx, 0, course, config, new HashSet<Section>(), 0, true, false, false, false,
                        getMaxDomainSize() <= 0 ? -1 : ret.size() + getMaxDomainSize());
            }
            idx++;
        }
        return ret;
    }

    /**
     * Return all enrollments of the first course that are selected (
     * {@link CourseRequest#isSelected(Section)} is true)
     * 
     * @param assignment current assignment
     * @param availableOnly
     *            pick only available sections
     * @return selected enrollments
     */
    public List<Enrollment> getSelectedEnrollments(Assignment<Request, Enrollment> assignment, boolean availableOnly) {
        if (getSelectedChoices().isEmpty())
            return null;
        List<Enrollment> enrollments = new ArrayList<Enrollment>();
        if (isFixed()) return enrollments;
        if (getInitialAssignment() != null && getModel() != null && ((StudentSectioningModel)getModel()).isMPP() && ((StudentSectioningModel)getModel()).getKeepInitialAssignments())
            return enrollments;
        for (Course course : iCourses) {
            boolean hasChoice = false;
            for (Choice choice: getSelectedChoices())
                if (course.getOffering().equals(choice.getOffering())) { hasChoice = true; break; }
            if (hasChoice)
                for (Config config : course.getOffering().getConfigs()) {
                    computeEnrollments(assignment, enrollments, 0, 0, course, config, new HashSet<Section>(), 0, availableOnly, false, true, false, -1);
                }
            break;
        }
        return enrollments;
    }

    /**
     * Return all enrollments that are available, pick only the first section of
     * the sections with the same time (of each subpart, {@link Section}
     * comparator is used)
     * @param assignment current assignment
     * @return available enrollments 
     */
    public List<Enrollment> getAvaiableEnrollmentsSkipSameTime(Assignment<Request, Enrollment> assignment) {
        List<Enrollment> ret = new ArrayList<Enrollment>();
        if (isFixed()) {
            ret.add(getFixedValue());
            return ret;
        }
        if (getInitialAssignment() != null) {
            ret.add(getInitialAssignment());
            if (getModel() != null && ((StudentSectioningModel)getModel()).isMPP() && ((StudentSectioningModel)getModel()).getKeepInitialAssignments())
                return ret;
        }
        int idx = 0;
        for (Course course : iCourses) {
            boolean skipSameTime = true;
            for (LinkedSections link: getStudent().getLinkedSections())
                if (link.getOfferings().contains(course.getOffering())) { skipSameTime = false; break; }
            for (Config config : course.getOffering().getConfigs()) {
                computeEnrollments(assignment, ret, idx, 0, course, config, new HashSet<Section>(), 0, true, skipSameTime, false, false,
                        getMaxDomainSize() <= 0 ? -1 : ret.size() + getMaxDomainSize());
            }
            idx++;
        }
        return ret;
    }

    /**
     * Return all possible enrollments, but pick only the first section of
     * the sections with the same time (of each subpart, {@link Section}
     * comparator is used).
     * @param assignment current assignment
     * @return computed enrollments 
     */
    public List<Enrollment> getEnrollmentsSkipSameTime(Assignment<Request, Enrollment> assignment) {
        List<Enrollment> ret = new ArrayList<Enrollment>();
        if (isFixed()) {
            ret.add(getFixedValue());
            return ret;
        }
        if (getInitialAssignment() != null) {
            ret.add(getInitialAssignment());
            if (getModel() != null && ((StudentSectioningModel)getModel()).isMPP() && ((StudentSectioningModel)getModel()).getKeepInitialAssignments())
                return ret;
        }
        int idx = 0;
        for (Course course : iCourses) {
            for (Config config : course.getOffering().getConfigs()) {
                boolean skipSameTime = true;
                for (LinkedSections link: getStudent().getLinkedSections())
                    if (link.getOfferings().contains(course.getOffering())) { skipSameTime = false; break; }
                computeEnrollments(assignment, ret, idx, 0, course, config, new HashSet<Section>(), 0, false, skipSameTime, false, false,
                        getMaxDomainSize() <= 0 ? -1 : ret.size() + getMaxDomainSize());
            }
            idx++;
        }
        return ret;
    }

    /** Wait-listed choices 
     * @return wait-listed choices
     **/
    public Set<Choice> getWaitlistedChoices() {
        return iWaitlistedChoices;
    }

    /**
     * Return true when the given section is wait-listed (i.e., its choice is
     * among wait-listed choices)
     * @param section given section
     * @return true if the given section matches the wait-listed choices
     */
    public boolean isWaitlisted(Section section) {
        for (Choice choice: iWaitlistedChoices)
            if (choice.sameChoice(section)) return true;
        return false;
    }

    /** Selected choices 
     * @return selected choices
     **/
    public Set<Choice> getSelectedChoices() {
        return iSelectedChoices;
    }
    
    /**
     * Return true when the given section is selected (i.e., its choice is among
     * selected choices)
     * @param section given section
     * @return true if the given section matches the selected choices
     */
    public boolean isSelected(Section section) {
        for (Choice choice: iSelectedChoices)
            if (choice.sameSection(section) || choice.sameConfiguration(section)) return true;
        return false;
    }
    
    /**
     * Return true when the given section has a preference (i.e., there is a matching selection),
     * or when there is a section preference for a different configuration
     * @param section given section
     * @return true if the there is a matching choice for the given section that is of the same offering
     */
    public boolean hasSelection(Section section) {
        boolean hasSectionChoices = false, hasSectionChoicesThisConfig = false;
        for (Choice choice: iSelectedChoices) {
            if (choice.sameOffering(section)) {
                if (choice.isMatching(section)) return true;
                if (choice.getSubpartId() != null) {
                    hasSectionChoices = true;
                    for (Subpart subpart: section.getSubpart().getConfig().getSubparts()) {
                        if (choice.getSubpartId().equals(subpart.getId())) { hasSectionChoicesThisConfig = true; }
                    }
                }
            }
        }
        return (hasSectionChoices && !hasSectionChoicesThisConfig);
    }
    
    /**
     * Required choices
     * @return required choices
     */
    public Set<Choice> getRequiredChoices() {
        return iRequiredChoices;
    }
    
    /**
     * Return true when the given section is required (i.e., its choice is among required choices, or there are no requirements)
     * @param section given section
     * @return true if the given section matches the required choices
     */
    public boolean isRequired(Section section) {
        if (iRequiredChoices.isEmpty()) return true;
        boolean hasConfig = false, hasMatchingConfig = false;
        boolean hasSubpart = false, hasMatchingSection = false;
        boolean hasSectionReq = false;
        for (Choice choice: iRequiredChoices) {
            // different offering -> skip
            if (!choice.getOffering().equals(section.getSubpart().getConfig().getOffering())) continue;
            // has config -> check config
            if (choice.getConfigId() != null) {
                hasConfig = true;
                if (choice.sameConfiguration(section)) hasMatchingConfig = true;
            }
            // has section of the matching subpart -> check section
            if (choice.getSubpartId() != null) {
                hasSectionReq = true;
                if (choice.getSubpartId().equals(section.getSubpart().getId())) {
                    hasSubpart = true;
                    if (choice.sameSection(section)) hasMatchingSection = true;
                } else if (!hasMatchingConfig) {
                    for (Subpart subpart: section.getSubpart().getConfig().getSubparts()) {
                        if (choice.getSubpartId().equals(subpart.getId())) {
                            hasMatchingConfig = true;
                            break;
                        }
                    }
                }
            }
        }
        if (hasConfig && !hasMatchingConfig) return false;
        if (hasSubpart && !hasMatchingSection) return false;
        // no match, but there are section requirements for a different config -> not satisfied 
        if (!hasMatchingConfig && !hasMatchingSection && hasSectionReq) return false;
        return true;
    }

    /**
     * Request name: A for alternative, 1 + priority, (w) when wait-list, list of
     * course names
     */
    @Override
    public String getName() {
        String ret = (isAlternative() ? "A" : "")
                + (1 + getPriority() + (isAlternative() ? -getStudent().nrRequests() : 0)) + ". "
                + (getRequestPriority() != RequestPriority.Normal ?
                  (isWaitlist() ? "(" + getRequestPriority().getAbbreviation() + "w) " : "(" + getRequestPriority().getAbbreviation() + ") ")
                  : isWaitlist() ? "(w) " : "");
        int idx = 0;
        for (Course course : iCourses) {
            if (idx == 0)
                ret += course.getName();
            else
                ret += ", " + idx + ". alt " + course.getName();
            idx++;
        }
        return ret;
    }

    /**
     * True if the student can be put on a wait-list (no alternative course
     * request will be given instead)
     * @return true if the request can be wait-listed
     */
    public boolean isWaitlist() {
        return iWaitlist;
    }
    
    /**
     * True if the student can be put on a wait-list (no alternative course
     * request will be given instead)
     * @param waitlist true if the request can be wait-listed
     */
    public void setWaitlist(boolean waitlist) {
        iWaitlist = waitlist;
    }
    
    /**
     * True if the course request is critical for the student in order to move forward in their degree 
     * @param critical true if the request is critical
     */
    @Deprecated
    public void setCritical(boolean critical) {
        iPriority = (critical ? RequestPriority.Critical : RequestPriority.Normal);
    }
    
    /**
     * Time stamp of the request
     * @return request time stamp
     */
    public Long getTimeStamp() {
        return iTimeStamp;
    }

    @Override
    public String toString() {
        return getName() + (getWeight() != 1.0 ? " (W:" + sDF.format(getWeight()) + ")" : "");
    }

    /** Return course of the requested courses with the given id 
     * @param courseId course offering id
     * @return course of the given id
     **/
    public Course getCourse(long courseId) {
        for (Course course : iCourses) {
            if (course.getId() == courseId)
                return course;
        }
        return null;
    }

    /** Return configuration of the requested courses with the given id 
     * @param configId instructional offering configuration unique id
     * @return config of the given id
     **/
    public Config getConfig(long configId) {
        for (Course course : iCourses) {
            for (Config config : course.getOffering().getConfigs()) {
                if (config.getId() == configId)
                    return config;
            }
        }
        return null;
    }

    /** Return subpart of the requested courses with the given id 
     * @param subpartId scheduling subpart unique id
     * @return subpart of the given id
     **/
    public Subpart getSubpart(long subpartId) {
        for (Course course : iCourses) {
            for (Config config : course.getOffering().getConfigs()) {
                for (Subpart subpart : config.getSubparts()) {
                    if (subpart.getId() == subpartId)
                        return subpart;
                }
            }
        }
        return null;
    }

    /** Return section of the requested courses with the given id 
     * @param sectionId class unique id
     * @return section of the given id
     **/
    public Section getSection(long sectionId) {
        for (Course course : iCourses) {
            for (Config config : course.getOffering().getConfigs()) {
                for (Subpart subpart : config.getSubparts()) {
                    for (Section section : subpart.getSections()) {
                        if (section.getId() == sectionId)
                            return section;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Minimal penalty (minimum of {@link Offering#getMinPenalty()} among
     * requested courses)
     * @return minimal penalty
     */
    public double getMinPenalty() {
        if (iCachedMinPenalty == null) {
            double min = Double.MAX_VALUE;
            for (Course course : iCourses) {
                min = Math.min(min, course.getOffering().getMinPenalty());
            }
            iCachedMinPenalty = Double.valueOf(min);
        }
        return iCachedMinPenalty.doubleValue();
    }

    /**
     * Maximal penalty (maximum of {@link Offering#getMaxPenalty()} among
     * requested courses)
     * @return maximal penalty
     */
    public double getMaxPenalty() {
        if (iCachedMaxPenalty == null) {
            double max = Double.MIN_VALUE;
            for (Course course : iCourses) {
                max = Math.max(max, course.getOffering().getMaxPenalty());
            }
            iCachedMaxPenalty = Double.valueOf(max);
        }
        return iCachedMaxPenalty.doubleValue();
    }

    /** Clear cached min/max penalties and cached bound */
    public void clearCache() {
        iCachedMaxPenalty = null;
        iCachedMinPenalty = null;
    }

    /**
     * Estimated bound for this request -- it estimates the smallest value among
     * all possible enrollments
     */
    @Override
    public double getBound() {
        return - getWeight() * ((StudentSectioningModel)getModel()).getStudentWeights().getBound(this);
        /*
        if (iCachedBound == null) {
            iCachedBound = Double.valueOf(-Math.pow(Enrollment.sPriorityWeight, getPriority())
                    * (isAlternative() ? Enrollment.sAlterativeWeight : 1.0)
                    * Math.pow(Enrollment.sInitialWeight, (getInitialAssignment() == null ? 0 : 1))
                    * Math.pow(Enrollment.sSelectedWeight, (iSelectedChoices.isEmpty() ? 0 : 1))
                    * Math.pow(Enrollment.sWaitlistedWeight, (iWaitlistedChoices.isEmpty() ? 0 : 1))
                    *
                    // Math.max(Enrollment.sMinWeight,getWeight()) *
                    (getStudent().isDummy() ? Student.sDummyStudentWeight : 1.0)
                    * Enrollment.normalizePenalty(getMinPenalty()));
        }
        return iCachedBound.doubleValue();
        */
    }

    /** Return true if request is assigned. */
    @Override
    public boolean isAssigned(Assignment<Request, Enrollment> assignment) {
        Enrollment e = assignment.getValue(this);
        return e != null && !e.getAssignments().isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && (o instanceof CourseRequest);
    }
    
    /**
     * Get reservations for this course requests
     * @param course given course
     * @return reservations for this course requests and the given course
     */
    public synchronized List<Reservation> getReservations(Course course) {
        if (iReservations == null)
            iReservations = new HashMap<Course, List<Reservation>>();
        List<Reservation> reservations = iReservations.get(course);
        if (reservations == null) {
            reservations = new ArrayList<Reservation>();
            boolean mustBeUsed = false;
            for (Reservation r: course.getOffering().getReservations()) {
                if (!r.isApplicable(getStudent())) continue;
                if (!mustBeUsed && r.mustBeUsed()) { reservations.clear(); mustBeUsed = true; }
                if (mustBeUsed && !r.mustBeUsed()) continue;
                reservations.add(r);
            }
            iReservations.put(course, reservations);
        }
        return reservations;
    }
    private Map<Course, List<Reservation>> iReservations = null;
    
    /**
     * Get reservations for this course requests ordered using {@link Reservation#compareTo(Assignment, Reservation)}
     * @param course given course
     * @return reservations for this course requests and the given course
     */
    public TreeSet<Reservation> getSortedReservations(Assignment<Request, Enrollment> assignment, Course course) {
        TreeSet<Reservation> reservations = new TreeSet<Reservation>(new AssignmentComparator<Reservation, Request, Enrollment>(assignment));
        reservations.addAll(getReservations(course));
        return reservations;
    }
    
    /**
     * Return true if there is a reservation for a course of this request
     * @return true if there is a reservation for a course of this request
     */
    public boolean hasReservations() {
        for (Course course: getCourses())
            if (!getReservations(course).isEmpty())
                return true;
        return false;
    }
    
    /**
     * Clear reservation information that was cached on this section
     */
    public synchronized void clearReservationCache() {
        if (iReservations != null) iReservations.clear();
    }
    
    /**
     * Get restrictions for this course requests
     * @param course given course
     * @return restrictions for this course requests and the given course
     */
    public synchronized List<Restriction> getRestrictions(Course course) {
        if (iRestrictions == null)
            iRestrictions = new HashMap<Course, List<Restriction>>();
        List<Restriction> restrictions = iRestrictions.get(course);
        if (restrictions == null) {
            restrictions = new ArrayList<Restriction>();
            for (Restriction r: course.getOffering().getRestrictions()) {
                if (r.isApplicable(getStudent()))
                    restrictions.add(r);
            }
            iRestrictions.put(course, restrictions);
        }
        return restrictions;
    }
    private Map<Course, List<Restriction>> iRestrictions = null;
    
    /**
     * Return true if there is a restriction for a course of this request
     * @return true if there is a restriction for a course of this request
     */
    public boolean hasRestrictions(Course course) {
        return !getRestrictions(course).isEmpty();
    }
    
    /**
     * Return true when there are restrictions for a course of this course request and the given config does not meet any of them
     */
    public boolean isNotAllowed(Course course, Config config) {
        List<Restriction> restrictions = getRestrictions(course);
        if (restrictions.isEmpty()) return false;
        for (Restriction r: restrictions)
            if (r.isIncluded(config)) return false;
        return true;
    }
    
    /**
     * Return true when there are restrictions for a course of this course request and the given section does not meet any of them
     */
    public boolean isNotAllowed(Course course, Section section) {
        List<Restriction> restrictions = getRestrictions(course);
        if (restrictions.isEmpty()) return false;
        for (Restriction r: restrictions)
            if (r.isIncluded(section)) return false;
        return true;
    }
    
    /**
     * Return true when there are restrictions for a course of this course request and the given enrollment does not meet any of them
     */
    public boolean isNotAllowed(Enrollment e) {
        List<Restriction> restrictions = getRestrictions(e.getCourse());
        if (restrictions.isEmpty()) return false;
        for (Restriction r: restrictions)
            if (r.isIncluded(e)) return false;
        return true;
    }
    
    /**
     * Clear restriction information that was cached on this request
     */
    public synchronized void clearRestrictionCache() {
        if (iRestrictions != null) iRestrictions.clear();
    }
    
    /**
     * Return true if this request can track MPP
     * @return true if the request is course request and it either has an initial enrollment
     */
    @Override
    public boolean isMPP() {
        StudentSectioningModel model = (StudentSectioningModel) getModel();
        if (model == null || !model.isMPP()) return false;
        return !getStudent().isDummy() && getInitialAssignment() != null; 
    }
    
    /**
     * Return true if this request has any selection
     * @return true if the request is course request and has some selected choices.
     */
    @Override
    public boolean hasSelection() {
        if (getStudent().isDummy() || getSelectedChoices().isEmpty()) return false;
        for (Choice choice: getSelectedChoices())
            if (choice.getSectionId() != null || choice.getConfigId() != null) return true;
        return false;
    }
    
    /**
     * Add request group to this request.
     * @param group request group to be added
     */
    public void addRequestGroup(RequestGroup group) {
        iRequestGroups.add(group);
        group.addRequest(this);
    }
    
    /**
     * Removed request group from this request.
     * @param group request group to be removed
     */
    public void removeRequestGroup(RequestGroup group) {
        iRequestGroups.remove(group);
        group.removeRequest(this);
    }

    /**
     * Lists request groups of this request
     * @return request groups of this course requests
     */
    public Set<RequestGroup> getRequestGroups() {
        return iRequestGroups;
    }
    
    @Override
    public void variableAssigned(Assignment<Request, Enrollment> assignment, long iteration, Enrollment enrollment) {
        super.variableAssigned(assignment, iteration, enrollment);
        for (RequestGroup g: getRequestGroups())
            if (g.getCourse().equals(enrollment.getCourse()))
                g.assigned(assignment, enrollment);
    }

    @Override
    public void variableUnassigned(Assignment<Request, Enrollment> assignment, long iteration, Enrollment enrollment) {
        super.variableUnassigned(assignment, iteration, enrollment);
        for (RequestGroup g: getRequestGroups())
            if (g.getCourse().equals(enrollment.getCourse()))
                g.unassigned(assignment, enrollment);
    }

    @Override
    public float getMinCredit() {
        Float credit = null;
        for (Course course: getCourses()) {
            if (course.hasCreditValue() && (credit == null || credit > course.getCreditValue()))
                    credit = course.getCreditValue();
            for (Config config: course.getOffering().getConfigs()) {
                Float configCredit = config.getCreditValue();
                if (configCredit != null && (credit == null || credit > configCredit))
                        credit = configCredit;
            }
        }
        return (credit == null ? 0 : credit.floatValue());
    }

    @Override
    public RequestPriority getRequestPriority() {
        return iPriority;
    }
    
    public void setRequestPriority(RequestPriority priority) {
        iPriority = priority;
    }

    public boolean isFixed() { return iFixed != null; }
    public Enrollment getFixedValue() { return iFixed; }
    public void setFixedValue(Enrollment constant) { iFixed = constant; }
}
