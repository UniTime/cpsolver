package org.cpsolver.studentsct.model;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.AssignmentComparable;
import org.cpsolver.ifs.assignment.context.AbstractClassWithContext;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.CanInheritContext;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.studentsct.reservation.Reservation;


/**
 * Representation of a class. Each section contains id, name, scheduling
 * subpart, time/room placement, and a limit. Optionally, parent-child relation
 * between sections can be defined. <br>
 * <br>
 * Each student requesting a course needs to be enrolled in a class of each
 * subpart of a selected configuration. In the case of parent-child relation
 * between classes, if a student is enrolled in a section that has a parent
 * section defined, he/she has to be enrolled in the parent section as well. If
 * there is a parent-child relation between two sections, the same relation is
 * defined on their subparts as well, i.e., if section A is a parent section B,
 * subpart of section A isa parent of subpart of section B. <br>
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
public class Section extends AbstractClassWithContext<Request, Enrollment, Section.SectionContext> implements SctAssignment, AssignmentComparable<Section, Request, Enrollment>, CanInheritContext<Request, Enrollment, Section.SectionContext>{
    private static DecimalFormat sDF = new DecimalFormat("0.000");
    private long iId = -1;
    private String iName = null;
    private Map<Long, String> iNameByCourse = null;
    private Subpart iSubpart = null;
    private Section iParent = null;
    private Placement iPlacement = null;
    private int iLimit = 0;
    private List<Instructor> iInstructors = null;
    private double iPenalty = 0.0;
    private double iSpaceExpected = 0.0;
    private double iSpaceHeld = 0.0;
    private String iNote = null;
    private Set<Long> iIgnoreConflictsWith = null;
    private boolean iCancelled = false, iEnabled = true, iOnline = false, iPast = false;
    private List<Unavailability> iUnavailabilities = new ArrayList<Unavailability>();

    /**
     * Constructor
     * 
     * @param id
     *            section unique id
     * @param limit
     *            section limit, i.e., the maximal number of students that can
     *            be enrolled in this section at the same time
     * @param name
     *            section name
     * @param subpart
     *            subpart of this section
     * @param placement
     *            time/room placement
     * @param instructors
     *            assigned instructor(s)
     * @param parent
     *            parent section -- if there is a parent section defined, a
     *            student that is enrolled in this section has to be enrolled in
     *            the parent section as well. Also, the same relation needs to
     *            be defined between subpart of this section and the subpart of
     *            the parent section
     */
    public Section(long id, int limit, String name, Subpart subpart, Placement placement, List<Instructor> instructors, Section parent) {
        iId = id;
        iLimit = limit;
        iName = name;
        iSubpart = subpart;
        if (iSubpart != null)
            iSubpart.getSections().add(this);
        iPlacement = placement;
        iParent = parent;
        iInstructors = instructors;
    }
    
    /**
     * Constructor
     * 
     * @param id
     *            section unique id
     * @param limit
     *            section limit, i.e., the maximal number of students that can
     *            be enrolled in this section at the same time
     * @param name
     *            section name
     * @param subpart
     *            subpart of this section
     * @param placement
     *            time/room placement
     * @param instructors
     *            assigned instructor(s)
     * @param parent
     *            parent section -- if there is a parent section defined, a
     *            student that is enrolled in this section has to be enrolled in
     *            the parent section as well. Also, the same relation needs to
     *            be defined between subpart of this section and the subpart of
     *            the parent section
     */
    public Section(long id, int limit, String name, Subpart subpart, Placement placement, Section parent, Instructor... instructors) {
        this(id, limit, name, subpart, placement, Arrays.asList(instructors), parent);
    }
    
    @Deprecated
    public Section(long id, int limit, String name, Subpart subpart, Placement placement, String instructorIds, String instructorNames, Section parent) {
        this(id, limit, name, subpart, placement, Instructor.toInstructors(instructorIds, instructorNames), parent);
    }

    /** Section id */
    @Override
    public long getId() {
        return iId;
    }

    /**
     * Section limit. This is defines the maximal number of students that can be
     * enrolled into this section at the same time. It is -1 in the case of an
     * unlimited section
     * @return class limit
     */
    public int getLimit() {
        return iLimit;
    }

    /** Set section limit 
     * @param limit class limit
     **/
    public void setLimit(int limit) {
        iLimit = limit;
    }

    /** Section name 
     * @return class name
     **/
    public String getName() {
        return iName;
    }
    
    /** Set section name 
     * @param name class name
     **/
    public void setName(String name) {
        iName = name;
    }

    /** Scheduling subpart to which this section belongs 
     * @return scheduling subpart
     **/
    public Subpart getSubpart() {
        return iSubpart;
    }

    /**
     * Parent section of this section (can be null). If there is a parent
     * section defined, a student that is enrolled in this section has to be
     * enrolled in the parent section as well. Also, the same relation needs to
     * be defined between subpart of this section and the subpart of the parent
     * section.
     * @return parent class
     */
    public Section getParent() {
        return iParent;
    }

    /**
     * Time/room placement of the section. This can be null, for arranged
     * sections.
     * @return time and room assignment of this class
     */
    public Placement getPlacement() {
        return iPlacement;
    }
    
    /**
     * Set time/room placement of the section. This can be null, for arranged
     * sections.
     * @param placement time and room assignment of this class
     */
    public void setPlacement(Placement placement) {
        iPlacement = placement;
    }

    /** Time placement of the section. */
    @Override
    public TimeLocation getTime() {
        return (iPlacement == null ? null : iPlacement.getTimeLocation());
    }
    
    /** Check if the class has a time assignment (is not arranged hours) */
    public boolean hasTime() {
        return iPlacement != null && iPlacement.getTimeLocation() != null && iPlacement.getTimeLocation().getDayCode() != 0;
    }
    
    /** True if the instructional type is the same */
    public boolean sameInstructionalType(Section section) {
        return getSubpart().getInstructionalType().equals(section.getSubpart().getInstructionalType());
    }

    /** True if the time assignment is the same */
    public boolean sameTime(Section section) {
        return getTime() == null ? section.getTime() == null : getTime().equals(section.getTime());
    }
    
    /** True if the instructor(s) are the same */
    public boolean sameInstructors(Section section) {
        if (nrInstructors() != section.nrInstructors()) return false;
        return !hasInstructors() || getInstructors().containsAll(section.getInstructors());
    }
    
    /** True if the time assignment as well as the instructor(s) are the same */
    public boolean sameChoice(Section section) {
        return sameInstructionalType(section) && sameTime(section) && sameInstructors(section);
    }

    /** Number of rooms in which the section meet. */
    @Override
    public int getNrRooms() {
        return (iPlacement == null ? 0 : iPlacement.getNrRooms());
    }

    /**
     * Room placement -- list of
     * {@link org.cpsolver.coursett.model.RoomLocation}
     */
    @Override
    public List<RoomLocation> getRooms() {
        if (iPlacement == null)
            return null;
        if (iPlacement.getRoomLocations() == null && iPlacement.getRoomLocation() != null) {
            List<RoomLocation> ret = new ArrayList<RoomLocation>(1);
            ret.add(iPlacement.getRoomLocation());
            return ret;
        }
        return iPlacement.getRoomLocations();
    }

    /**
     * True, if this section overlaps with the given assignment in time and
     * space
     */
    @Override
    public boolean isOverlapping(SctAssignment assignment) {
        if (isAllowOverlap() || assignment.isAllowOverlap()) return false;
        if (getTime() == null || assignment.getTime() == null) return false;
        if (assignment instanceof Section && isToIgnoreStudentConflictsWith(assignment.getId())) return false;
        return getTime().hasIntersection(assignment.getTime());
    }

    /**
     * True, if this section overlaps with one of the given set of assignments
     * in time and space
     */
    @Override
    public boolean isOverlapping(Set<? extends SctAssignment> assignments) {
        if (isAllowOverlap()) return false;
        if (getTime() == null || assignments == null)
            return false;
        for (SctAssignment assignment : assignments) {
            if (assignment.isAllowOverlap())
                continue;
            if (assignment.getTime() == null)
                continue;
            if (assignment instanceof Section && isToIgnoreStudentConflictsWith(assignment.getId()))
                continue;
            if (getTime().hasIntersection(assignment.getTime()))
                return true;
        }
        return false;
    }

    /** Called when an enrollment with this section is assigned to a request */
    @Override
    public void assigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        getContext(assignment).assigned(assignment, enrollment);
    }

    /** Called when an enrollment with this section is unassigned from a request */
    @Override
    public void unassigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        getContext(assignment).unassigned(assignment, enrollment);
    }

    /** Long name: subpart name + time long name + room names + instructor names
     * @param useAmPm use 12-hour format 
     * @return long name
     **/
    public String getLongName(boolean useAmPm) {
        return getSubpart().getName() + " " + getName() + " " + (getTime() == null ? "" : " " + getTime().getLongName(useAmPm))
                + (getNrRooms() == 0 ? "" : " " + getPlacement().getRoomName(","))
                + (hasInstructors() ? " " + getInstructorNames(",") : "");
    }
    
    @Deprecated
    public String getLongName() {
        return getLongName(true);
    }

    @Override
    public String toString() {
        return getSubpart().getConfig().getOffering().getName() + " " + getSubpart().getName() + " " + getName()
                + (getTime() == null ? "" : " " + getTime().getLongName(true))
                + (getNrRooms() == 0 ? "" : " " + getPlacement().getRoomName(","))
                + (hasInstructors() ? " " + getInstructorNames(",") : "") + " (L:"
                + (getLimit() < 0 ? "unlimited" : "" + getLimit())
                + (getPenalty() == 0.0 ? "" : ",P:" + sDF.format(getPenalty())) + ")";
    }

    /** Instructors assigned to this section 
     * @return list of instructors
     **/
    public List<Instructor> getInstructors() {
        return iInstructors;
    }
    
    /**
     * Has any instructors assigned
     * @return return true if there is at least one instructor assigned
     */
    public boolean hasInstructors() {
        return iInstructors != null && !iInstructors.isEmpty();
    }
    
    /**
     * Return number of instructors of this section
     * @return number of assigned instructors
     */
    public int nrInstructors() {
        return iInstructors == null ? 0 : iInstructors.size();
    }
    
    /**
     * Instructor names
     * @param delim delimiter
     * @return instructor names
     */
    public String getInstructorNames(String delim) {
        if (iInstructors == null || iInstructors.isEmpty()) return "";
        StringBuffer sb = new StringBuffer();
        for (Iterator<Instructor> i = iInstructors.iterator(); i.hasNext(); ) {
            Instructor instructor = i.next();
            sb.append(instructor.getName() != null ? instructor.getName() : instructor.getExternalId() != null ? instructor.getExternalId() : "I" + instructor.getId());
            if (i.hasNext()) sb.append(delim);
        }
        return sb.toString();
    }

    /**
     * Return penalty which is added to an enrollment that contains this
     * section.
     * @return online penalty
     */
    public double getPenalty() {
        return iPenalty;
    }

    /** Set penalty which is added to an enrollment that contains this section. 
     * @param penalty online penalty
     **/
    public void setPenalty(double penalty) {
        iPenalty = penalty;
    }

    /**
     * Compare two sections, prefer sections with lower penalty and more open
     * space
     */
    @Override
    public int compareTo(Assignment<Request, Enrollment> assignment, Section s) {
        int cmp = Double.compare(getPenalty(), s.getPenalty());
        if (cmp != 0)
            return cmp;
        cmp = Double.compare(
                getLimit() < 0 ? getContext(assignment).getEnrollmentWeight(assignment, null) : getContext(assignment).getEnrollmentWeight(assignment, null) - getLimit(),
                s.getLimit() < 0 ? s.getContext(assignment).getEnrollmentWeight(assignment, null) : s.getContext(assignment).getEnrollmentWeight(assignment, null) - s.getLimit());
        if (cmp != 0)
            return cmp;
        return Double.compare(getId(), s.getId());
    }
    
    /**
     * Compare two sections, prefer sections with lower penalty
     */
    @Override
    public int compareTo(Section s) {
        int cmp = Double.compare(getPenalty(), s.getPenalty());
        if (cmp != 0)
            return cmp;
        return Double.compare(getId(), s.getId());
    }

    /**
     * Return the amount of space of this section that is held for incoming
     * students. This attribute is computed during the batch sectioning (it is
     * the overall weight of dummy students enrolled in this section) and it is
     * being updated with each incomming student during the online sectioning.
     * @return space held
     */
    public double getSpaceHeld() {
        return iSpaceHeld;
    }

    /**
     * Set the amount of space of this section that is held for incoming
     * students. See {@link Section#getSpaceHeld()} for more info.
     * @param spaceHeld space held
     */
    public void setSpaceHeld(double spaceHeld) {
        iSpaceHeld = spaceHeld;
    }

    /**
     * Return the amount of space of this section that is expected to be taken
     * by incoming students. This attribute is computed during the batch
     * sectioning (for each dummy student that can attend this section (without
     * any conflict with other enrollments of that student), 1 / x where x is
     * the number of such sections of this subpart is added to this value).
     * Also, this value is being updated with each incoming student during the
     * online sectioning.
     * @return space expected
     */
    public double getSpaceExpected() {
        return iSpaceExpected;
    }

    /**
     * Set the amount of space of this section that is expected to be taken by
     * incoming students. See {@link Section#getSpaceExpected()} for more info.
     * @param spaceExpected space expected
     */
    public void setSpaceExpected(double spaceExpected) {
        iSpaceExpected = spaceExpected;
    }

    /**
     * Online sectioning penalty.
     * @param assignment current assignment
     * @return online sectioning penalty
     */
    public double getOnlineSectioningPenalty(Assignment<Request, Enrollment> assignment) {
        if (getLimit() <= 0)
            return 0.0;

        double available = getLimit() - getContext(assignment).getEnrollmentWeight(assignment, null);

        double penalty = (getSpaceExpected() - available) / getLimit();

        return Math.max(-1.0, Math.min(1.0, penalty));
    }

    /**
     * Return true if overlaps are allowed, but the number of overlapping slots should be minimized.
     * This can be changed on the subpart, using {@link Subpart#setAllowOverlap(boolean)}.
     **/
    @Override
    public boolean isAllowOverlap() {
        return iSubpart.isAllowOverlap();
    }
    
    /** Sections first, then by {@link FreeTimeRequest#getId()} */
    @Override
    public int compareById(SctAssignment a) {
        if (a instanceof Section) {
            return Long.valueOf(getId()).compareTo(((Section)a).getId());
        } else {
            return -1;
        }
    }

    /**
     * Available space in the section that is not reserved by any section reservation
     * @param assignment current assignment
     * @param excludeRequest excluding given request (if not null)
     * @return unreserved space in this class
     **/
    public double getUnreservedSpace(Assignment<Request, Enrollment> assignment, Request excludeRequest) {
        // section is unlimited -> there is unreserved space unless there is an unlimited reservation too 
        // (in which case there is no unreserved space)
        if (getLimit() < 0) {
            // exclude reservations that are not directly set on this section
            for (Reservation r: getSectionReservations()) {
                // ignore expired reservations
                if (r.isExpired()) continue;
                // there is an unlimited reservation -> no unreserved space
                if (r.getLimit() < 0) return 0.0;
            }
            return Double.MAX_VALUE;
        }
        
        double available = getLimit() - getContext(assignment).getEnrollmentWeight(assignment, excludeRequest);
        // exclude reservations that are not directly set on this section
        for (Reservation r: getSectionReservations()) {
            // ignore expired reservations
            if (r.isExpired()) continue;
            // unlimited reservation -> all the space is reserved
            if (r.getLimit() < 0.0) return 0.0;
            // compute space that can be potentially taken by this reservation
            double reserved = r.getContext(assignment).getReservedAvailableSpace(assignment, excludeRequest);
            // deduct the space from available space
            available -= Math.max(0.0, reserved);
        }
        
        return available;
    }
    
    /**
     * Total space in the section that cannot be used by any section reservation
     * @return total unreserved space in this class
     **/
    public synchronized double getTotalUnreservedSpace() {
        if (iTotalUnreservedSpace == null)
            iTotalUnreservedSpace = getTotalUnreservedSpaceNoCache();
        return iTotalUnreservedSpace;
    }
    private Double iTotalUnreservedSpace = null;
    private double getTotalUnreservedSpaceNoCache() {
        // section is unlimited -> there is unreserved space unless there is an unlimited reservation too 
        // (in which case there is no unreserved space)
        if (getLimit() < 0) {
            // exclude reservations that are not directly set on this section
            for (Reservation r: getSectionReservations()) {
                // ignore expired reservations
                if (r.isExpired()) continue;
                // there is an unlimited reservation -> no unreserved space
                if (r.getLimit() < 0) return 0.0;
            }
            return Double.MAX_VALUE;
        }
        
        // we need to check all reservations linked with this section
        double available = getLimit(), reserved = 0, exclusive = 0;
        Set<Section> sections = new HashSet<Section>();
        reservations: for (Reservation r: getSectionReservations()) {
            // ignore expired reservations
            if (r.isExpired()) continue;
            // unlimited reservation -> no unreserved space
            if (r.getLimit() < 0) return 0.0;
            for (Section s: r.getSections(getSubpart())) {
                if (s.equals(this)) continue;
                if (s.getLimit() < 0) continue reservations;
                if (sections.add(s))
                    available += s.getLimit();
            }
            reserved += r.getLimit();
            if (r.getSections(getSubpart()).size() == 1)
                exclusive += r.getLimit();
        }
        
        return Math.min(available - reserved, getLimit() - exclusive);
    }
    
    
    /**
     * Get reservations for this section
     * @return reservations that can use this class
     */
    public synchronized List<Reservation> getReservations() {
        if (iReservations == null) {
            iReservations = new ArrayList<Reservation>();
            for (Reservation r: getSubpart().getConfig().getOffering().getReservations()) {
                if (r.getSections(getSubpart()) == null || r.getSections(getSubpart()).contains(this))
                    iReservations.add(r);
            }
        }
        return iReservations;
    }
    private List<Reservation> iReservations = null;
    
    /**
     * Get reservations that require this section
     * @return reservations that must use this class
     */
    public synchronized List<Reservation> getSectionReservations() {
        if (iSectionReservations == null) {
            iSectionReservations = new ArrayList<Reservation>();
            for (Reservation r: getSubpart().getSectionReservations()) {
                if (r.getSections(getSubpart()).contains(this))
                    iSectionReservations.add(r);
            }
        }
        return iSectionReservations;
    }
    private List<Reservation> iSectionReservations = null;

    /**
     * Clear reservation information that was cached on this section
     */
    public synchronized void clearReservationCache() {
        iReservations = null;
        iSectionReservations = null;
        iTotalUnreservedSpace = null;
    }
    
    /**
     * Return course-dependent section name
     * @param courseId course offering unique id
     * @return course dependent class name
     */
    public String getName(long courseId) {
        if (iNameByCourse == null) return getName();
        String name = iNameByCourse.get(courseId);
        return (name == null ? getName() : name);
    }
    
    /**
     * Set course-dependent section name
     * @param courseId course offering unique id
     * @param name course dependent class name
     */
    public void setName(long courseId, String name) {
        if (iNameByCourse == null) iNameByCourse = new HashMap<Long, String>();
        iNameByCourse.put(courseId, name);
    }

    /**
     * Return course-dependent section names
     * @return map of course-dependent class names
     */
    public Map<Long, String> getNameByCourse() { return iNameByCourse; }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Section)) return false;
        return getId() == ((Section)o).getId();
    }
    
    @Override
    public int hashCode() {
        return (int) (iId ^ (iId >>> 32));
    }
    
    /**
     * Section note
     * @return scheduling note
     */
    public String getNote() { return iNote; }
    
    /**
     * Section note
     * @param note scheduling note
     */
    public void setNote(String note) { iNote = note; }
    
    /**
     * Add section id of a section that student conflicts are to be ignored with
     * @param sectionId class unique id
     */
    public void addIgnoreConflictWith(long sectionId) {
        if (iIgnoreConflictsWith == null) iIgnoreConflictsWith = new HashSet<Long>();
        iIgnoreConflictsWith.add(sectionId);
    }
    
    /**
     * Returns true if student conflicts between this section and the given one are to be ignored
     * @param sectionId class unique id
     * @return true if student conflicts between these two sections are to be ignored
     */
    public boolean isToIgnoreStudentConflictsWith(long sectionId) {
        return iIgnoreConflictsWith != null && iIgnoreConflictsWith.contains(sectionId);
    }
    
    /**
     * Returns a set of ids of sections that student conflicts are to be ignored with (between this section and the others)
     * @return set of class unique ids of the sections that student conflicts are to be ignored with 
     */
    public Set<Long> getIgnoreConflictWithSectionIds() {
        return iIgnoreConflictsWith;
    }
    
    /** Set of assigned enrollments */
    @Override
    public Set<Enrollment> getEnrollments(Assignment<Request, Enrollment> assignment) {
        return getContext(assignment).getEnrollments();
    }
    
    /**
     * Enrollment weight -- weight of all requests which have an enrollment that
     * contains this section, excluding the given one. See
     * {@link Request#getWeight()}.
     * @param assignment current assignment
     * @param excludeRequest course request to ignore, if any
     * @return enrollment weight
     */
    public double getEnrollmentWeight(Assignment<Request, Enrollment> assignment, Request excludeRequest) {
        return getContext(assignment).getEnrollmentWeight(assignment, excludeRequest);
    }
    
    /**
     * Enrollment weight including over the limit enrollments.
     * That is enrollments that have reservation with {@link Reservation#canBatchAssignOverLimit()} set to true.
     * @param assignment current assignment
     * @param excludeRequest course request to ignore, if any
     * @return enrollment weight
     */
    public double getEnrollmentTotalWeight(Assignment<Request, Enrollment> assignment, Request excludeRequest) {
        return getContext(assignment).getEnrollmentTotalWeight(assignment, excludeRequest);
    }
    
    /**
     * Maximal weight of a single enrollment in the section
     * @param assignment current assignment
     * @return maximal enrollment weight
     */
    public double getMaxEnrollmentWeight(Assignment<Request, Enrollment> assignment) {
        return getContext(assignment).getMaxEnrollmentWeight();
    }

    /**
     * Minimal weight of a single enrollment in the section
     * @param assignment current assignment
     * @return minimal enrollment weight
     */
    public double getMinEnrollmentWeight(Assignment<Request, Enrollment> assignment) {
        return getContext(assignment).getMinEnrollmentWeight();
    }
    
    /**
     * Return cancelled flag of the class.
     * @return true if the class is cancelled
     */
    public boolean isCancelled() { return iCancelled; }
    
    /**
     * Set cancelled flag of the class.
     * @param cancelled true if the class is cancelled
     */
    public void setCancelled(boolean cancelled) { iCancelled = cancelled; }

    /**
     * Return past flag of the class.
     * @return true if the class is in the past and should be avoided when possible
     */
    public boolean isPast() { return iPast; }
    
    /**
     * Set past flag of the class.
     * @param past true if the class is in the past and should be avoided when possible
     */
    public void setPast(boolean past) { iPast = past; }

    /**
     * Return enabled flag of the class.
     * @return true if the class is enabled for student scheduling
     */
    public boolean isEnabled() { return iEnabled; }
    
    /**
     * Set enabled flag of the class.
     * @param enabled true if the class is enabled for student scheduling
     */
    public void setEnabled(boolean enabled) { iEnabled = enabled; }
    
    /**
     * Return whether the class is online.
     * @return true if the class is online
     */
    public boolean isOnline() { return iOnline; }
    
    /**
     * Set whether the class is online.
     * @param online true if the class is online
     */
    public void setOnline(boolean online) { iOnline = online; }
    
    @Override
    public Model<Request, Enrollment> getModel() {
        return getSubpart().getConfig().getOffering().getModel();
    }

    @Override
    public SectionContext createAssignmentContext(Assignment<Request, Enrollment> assignment) {
        return new SectionContext(assignment);
    }
    
    @Override
    public SectionContext inheritAssignmentContext(Assignment<Request, Enrollment> assignment, SectionContext parentContext) {
        return new SectionContext(parentContext);
    }
    
    public class SectionContext implements AssignmentConstraintContext<Request, Enrollment> {
        private Set<Enrollment> iEnrollments = null;
        private double iEnrollmentWeight = 0.0;
        private double iEnrollmentTotalWeight = 0.0;
        private double iMaxEnrollmentWeight = 0.0;
        private double iMinEnrollmentWeight = 0.0;
        private boolean iReadOnly = false;

        public SectionContext(Assignment<Request, Enrollment> assignment) {
            iEnrollments = new HashSet<Enrollment>();
            for (Course course: getSubpart().getConfig().getOffering().getCourses()) {
                for (CourseRequest request: course.getRequests()) {
                    Enrollment enrollment = assignment.getValue(request);
                    if (enrollment != null && enrollment.getSections().contains(Section.this))
                        assigned(assignment, enrollment);
                }
            }
        }
        
        public SectionContext(SectionContext parent) {
            iEnrollmentWeight = parent.iEnrollmentWeight;
            iEnrollmentTotalWeight = parent.iEnrollmentTotalWeight;
            iMaxEnrollmentWeight = parent.iMaxEnrollmentWeight;
            iMinEnrollmentWeight = parent.iMinEnrollmentWeight;
            iEnrollments = parent.iEnrollments;
            iReadOnly = true;
        }

        /** Called when an enrollment with this section is assigned to a request */
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
            if (iEnrollments.add(enrollment)) {
                iEnrollmentTotalWeight += enrollment.getRequest().getWeight();
                if (enrollment.getReservation() == null || !enrollment.getReservation().canBatchAssignOverLimit())
                    iEnrollmentWeight += enrollment.getRequest().getWeight();
            }
        }

        /** Called when an enrollment with this section is unassigned from a request */
        @Override
        public void unassigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
            if (iReadOnly) {
                iEnrollments = new HashSet<Enrollment>(iEnrollments);
                iReadOnly = false;
            }
            if (iEnrollments.remove(enrollment)) {
                iEnrollmentTotalWeight -= enrollment.getRequest().getWeight();
                if (enrollment.getReservation() == null || !enrollment.getReservation().canBatchAssignOverLimit())
                    iEnrollmentWeight -= enrollment.getRequest().getWeight();
            }
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
        
        /** Set of assigned enrollments 
         * @return assigned enrollments of this section
         **/
        public Set<Enrollment> getEnrollments() {
            return iEnrollments;
        }
        
        /**
         * Enrollment weight -- weight of all requests which have an enrollment that
         * contains this section, excluding the given one. See
         * {@link Request#getWeight()}.
         * @param assignment current assignment
         * @param excludeRequest course request to ignore, if any
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
        
        /**
         * Enrollment weight including over the limit enrollments.
         *  That is enrollments that have reservation with {@link Reservation#canBatchAssignOverLimit()} set to true.
         * @param assignment current assignment
         * @param excludeRequest course request to ignore, if any
         * @return enrollment weight
         */
        public double getEnrollmentTotalWeight(Assignment<Request, Enrollment> assignment, Request excludeRequest) {
            double weight = iEnrollmentTotalWeight;
            if (excludeRequest != null) {
                Enrollment enrollment = assignment.getValue(excludeRequest);
                if (enrollment!= null && iEnrollments.contains(enrollment))
                    weight -= excludeRequest.getWeight();
            }
            return weight;
        }
        
        /**
         * Maximal weight of a single enrollment in the section
         * @return maximal enrollment weight
         */
        public double getMaxEnrollmentWeight() {
            return iMaxEnrollmentWeight;
        }

        /**
         * Minimal weight of a single enrollment in the section
         * @return minimal enrollment weight
         */
        public double getMinEnrollmentWeight() {
            return iMinEnrollmentWeight;
        }
    }
    
    /**
     * Choice matching this section
     * @return choice matching this section
     */
    public Choice getChoice() {
        return new Choice(this);
    }
    
    /**
     * List of student unavailabilities
     * @return student unavailabilities
     */
    public List<Unavailability> getUnavailabilities() { return iUnavailabilities; }
}
