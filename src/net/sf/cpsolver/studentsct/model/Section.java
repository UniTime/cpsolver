package net.sf.cpsolver.studentsct.model;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.RoomLocation;
import net.sf.cpsolver.coursett.model.TimeLocation;

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
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 */
public class Section implements Assignment, Comparable<Section> {
    private static DecimalFormat sDF = new DecimalFormat("0.000");
    private long iId = -1;
    private String iName = null;
    private Subpart iSubpart = null;
    private Section iParent = null;
    private Placement iPlacement = null;
    private int iLimit = 0;
    private Set<Enrollment> iEnrollments = new HashSet<Enrollment>();
    private Choice iChoice = null;
    private double iPenalty = 0.0;
    private double iEnrollmentWeight = 0.0;
    private double iMaxEnrollmentWeight = 0.0;
    private double iMinEnrollmentWeight = 0.0;
    private double iSpaceExpected = 0.0;
    private double iSpaceHeld = 0.0;

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
     * @param instructorIds
     *            instructor(s) id -- needed for {@link Section#getChoice()}
     * @param instructorNames
     *            instructor(s) name -- needed for {@link Section#getChoice()}
     * @param parent
     *            parent section -- if there is a parent section defined, a
     *            student that is enrolled in this section has to be enrolled in
     *            the parent section as well. Also, the same relation needs to
     *            be defined between subpart of this section and the subpart of
     *            the parent section
     */
    public Section(long id, int limit, String name, Subpart subpart, Placement placement, String instructorIds,
            String instructorNames, Section parent) {
        iId = id;
        iLimit = limit;
        iName = name;
        iSubpart = subpart;
        iSubpart.getSections().add(this);
        iPlacement = placement;
        iParent = parent;
        iChoice = new Choice(getSubpart().getConfig().getOffering(), getSubpart().getInstructionalType(), getTime(),
                instructorIds, instructorNames);
    }

    /** Section id */
    public long getId() {
        return iId;
    }

    /**
     * Section limit. This is defines the maximal number of students that can be
     * enrolled into this section at the same time. It is -1 in the case of an
     * unlimited section
     */
    public int getLimit() {
        return iLimit;
    }

    /** Set section limit */
    public void setLimit(int limit) {
        iLimit = limit;
    }

    /** Section name */
    public String getName() {
        return iName;
    }
    
    /** Set section name */
    public void setName(String name) {
        iName = name;
    }

    /** Scheduling subpart to which this section belongs */
    public Subpart getSubpart() {
        return iSubpart;
    }

    /**
     * Parent section of this section (can be null). If there is a parent
     * section defined, a student that is enrolled in this section has to be
     * enrolled in the parent section as well. Also, the same relation needs to
     * be defined between subpart of this section and the subpart of the parent
     * section.
     */
    public Section getParent() {
        return iParent;
    }

    /**
     * Time/room placement of the section. This can be null, for arranged
     * sections.
     */
    public Placement getPlacement() {
        return iPlacement;
    }
    
    /**
     * Set time/room placement of the section. This can be null, for arranged
     * sections.
     */
    public void setPlacement(Placement placement) {
        iPlacement = placement;
    }

    /** Time placement of the section. */
    public TimeLocation getTime() {
        return (iPlacement == null ? null : iPlacement.getTimeLocation());
    }

    /** Number of rooms in which the section meet. */
    public int getNrRooms() {
        return (iPlacement == null ? 0 : iPlacement.getNrRooms());
    }

    /**
     * Room placement -- list of
     * {@link net.sf.cpsolver.coursett.model.RoomLocation}
     */
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
    public boolean isOverlapping(Assignment assignment) {
        if (getTime() == null || assignment.getTime() == null)
            return false;
        return getTime().hasIntersection(assignment.getTime());
    }

    /**
     * True, if this section overlaps with one of the given set of assignments
     * in time and space
     */
    public boolean isOverlapping(Set<? extends Assignment> assignments) {
        if (getTime() == null || assignments == null)
            return false;
        for (Assignment assignment : assignments) {
            if (assignment.getTime() == null)
                continue;
            if (getTime().hasIntersection(assignment.getTime()))
                return true;
        }
        return false;
    }

    /** Called when an enrollment with this section is assigned to a request */
    public void assigned(Enrollment enrollment) {
        if (iEnrollments.isEmpty()) {
            iMinEnrollmentWeight = iMaxEnrollmentWeight = enrollment.getRequest().getWeight();
        } else {
            iMaxEnrollmentWeight = Math.max(iMaxEnrollmentWeight, enrollment.getRequest().getWeight());
            iMinEnrollmentWeight = Math.min(iMinEnrollmentWeight, enrollment.getRequest().getWeight());
        }
        iEnrollments.add(enrollment);
        iEnrollmentWeight += enrollment.getRequest().getWeight();
    }

    /** Called when an enrollment with this section is unassigned from a request */
    public void unassigned(Enrollment enrollment) {
        iEnrollments.remove(enrollment);
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

    /** Set of assigned enrollments */
    public Set<Enrollment> getEnrollments() {
        return iEnrollments;
    }

    /**
     * Enrollment weight -- weight of all requests which have an enrollment that
     * contains this section, excluding the given one. See
     * {@link Request#getWeight()}.
     */
    public double getEnrollmentWeight(Request excludeRequest) {
        double weight = iEnrollmentWeight;
        if (excludeRequest != null && excludeRequest.getAssignment() != null
                && iEnrollments.contains(excludeRequest.getAssignment()))
            weight -= excludeRequest.getWeight();
        return weight;
    }

    /**
     * Maximal weight of a single enrollment in the section
     */
    public double getMaxEnrollmentWeight() {
        return iMaxEnrollmentWeight;
    }

    /**
     * Minimal weight of a single enrollment in the section
     */
    public double getMinEnrollmentWeight() {
        return iMinEnrollmentWeight;
    }

    /** Long name: subpart name + time long name + room names + instructor names */
    public String getLongName() {
        return getSubpart().getName() + (getTime() == null ? "" : " " + getTime().getLongName())
                + (getNrRooms() == 0 ? "" : " " + getPlacement().getRoomName(","))
                + (getChoice().getInstructorNames() == null ? "" : " " + getChoice().getInstructorNames());
    }

    @Override
    public String toString() {
        return getName() + (getTime() == null ? "" : " " + getTime().getLongName())
                + (getNrRooms() == 0 ? "" : " " + getPlacement().getRoomName(","))
                + (getChoice().getInstructorNames() == null ? "" : " " + getChoice().getInstructorNames()) + " (L:"
                + (getLimit() < 0 ? "unlimited" : "" + getLimit())
                + (getPenalty() == 0.0 ? "" : ",P:" + sDF.format(getPenalty())) + ")";
    }

    /** A (student) choice representing this section. */
    public Choice getChoice() {
        return iChoice;
    }

    /**
     * Return penalty which is added to an enrollment that contains this
     * section.
     */
    public double getPenalty() {
        return iPenalty;
    }

    /** Set penalty which is added to an enrollment that contains this section. */
    public void setPenalty(double penalty) {
        iPenalty = penalty;
    }

    /**
     * Compare two sections, prefer sections with lower penalty and more open
     * space
     */
    public int compareTo(Section s) {
        int cmp = Double.compare(getPenalty(), s.getPenalty());
        if (cmp != 0)
            return cmp;
        cmp = Double.compare(getLimit() - getEnrollmentWeight(null), s.getLimit() - s.getEnrollmentWeight(null));
        if (cmp != 0)
            return cmp;
        return Double.compare(getId(), s.getId());
    }

    /**
     * Return the amount of space of this section that is held for incoming
     * students. This attribute is computed during the batch sectioning (it is
     * the overall weight of dummy students enrolled in this section) and it is
     * being updated with each incomming student during the online sectioning.
     */
    public double getSpaceHeld() {
        return iSpaceHeld;
    }

    /**
     * Set the amount of space of this section that is held for incoming
     * students. See {@link Section#getSpaceHeld()} for more info.
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
     * Also, this value is being updated with each incomming student during the
     * online sectioning.
     */
    public double getSpaceExpected() {
        return iSpaceExpected;
    }

    /**
     * Set the amount of space of this section that is expected to be taken by
     * incoming students. See {@link Section#getSpaceExpected()} for more info.
     */
    public void setSpaceExpected(double spaceExpected) {
        iSpaceExpected = spaceExpected;
    }

    /**
     * Online sectioning penalty.
     */
    public double getOnlineSectioningPenalty() {
        if (getLimit() <= 0)
            return 0.0;

        double available = getLimit() - getEnrollmentWeight(null);

        double penalty = (getSpaceExpected() - available) / getLimit();

        return Math.max(-1.0, Math.min(1.0, penalty));
    }

}
