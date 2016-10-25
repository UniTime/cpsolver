package org.cpsolver.studentsct.model;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.cpsolver.coursett.model.TimeLocation;


/**
 * Student choice. Students have a choice of availabe time (but not room) and
 * instructor(s).
 * 
 * Choices of subparts that have the same instrutional type are also merged
 * together. For instance, a student have a choice of a time/instructor of a
 * Lecture and of a Recitation.
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
public class Choice {
    private Offering iOffering = null;
    private String iInstructionalType = null;
    private TimeLocation iTime = null;
    private List<Instructor> iInstructors = null;
    private int iHashCode;

    /**
     * Constructor
     * 
     * @param offering
     *            instructional offering to which the choice belongs
     * @param instructionalType
     *            instructional type to which the choice belongs (e.g., Lecture,
     *            Recitation or Laboratory)
     * @param time
     *            time assignment
     * @param instructors
     *            instructor(s)
     */
    public Choice(Offering offering, String instructionalType, TimeLocation time, List<Instructor> instructors) {
        iOffering = offering;
        iInstructionalType = instructionalType;
        iTime = time;
        iInstructors = instructors;
        iHashCode = getId().hashCode();
    }
    
    @Deprecated
    public Choice(Offering offering, String instructionalType, TimeLocation time, String instructorIds, String instructorNames) {
        this(offering, instructionalType, time, Instructor.toInstructors(instructorIds, instructorNames));
    }
    
    /**
     * Constructor
     * @param section section to base the choice on
     */
    public Choice(Section section) {
        this(section.getSubpart().getConfig().getOffering(), section.getSubpart().getInstructionalType(), section.getTime(), section.getInstructors());
    }

    /**
     * Constructor
     * 
     * @param offering
     *            instructional offering to which the choice belongs
     * @param choiceId
     *            choice id is in format instructionalType|time|instructorIds
     *            where time is of format dayCode:startSlot:length:datePatternId
     */
    public Choice(Offering offering, String choiceId) {
        iOffering = offering;
        iInstructionalType = choiceId.substring(0, choiceId.indexOf('|'));
        choiceId = choiceId.substring(choiceId.indexOf('|') + 1);
        String timeId = null;
        if (choiceId.indexOf('|') < 0) {
            timeId = choiceId;
        } else {
            timeId = choiceId.substring(0, choiceId.indexOf('|'));
            String instructorIds = choiceId.substring(choiceId.indexOf('|') + 1);
            if (!instructorIds.isEmpty()) {
                iInstructors = new ArrayList<Instructor>();
                for (String id: instructorIds.split(":"))
                    if (!id.isEmpty()) iInstructors.add(new Instructor(Long.parseLong(id)));
            }
        }
        if (timeId != null && timeId.length() > 0) {
            StringTokenizer s = new StringTokenizer(timeId, ":");
            int dayCode = Integer.parseInt(s.nextToken());
            int startSlot = Integer.parseInt(s.nextToken());
            int length = Integer.parseInt(s.nextToken());
            Long datePatternId = (s.hasMoreElements() ? Long.valueOf(s.nextToken()) : null);
            iTime = new TimeLocation(dayCode, startSlot, length, 0, 0, datePatternId, "N/A", new BitSet(), 0);
        }
        iHashCode = getId().hashCode();
    }

    /** Instructional offering to which this choice belongs 
     * @return instructional offering
     **/
    public Offering getOffering() {
        return iOffering;
    }

    /**
     * Instructional type (e.g., Lecture, Recitation or Laboratory) to which
     * this choice belongs
     * @return instructional type
     */
    public String getInstructionalType() {
        return iInstructionalType;
    }

    /** Time location of the choice
     * @return selected time
     **/
    public TimeLocation getTime() {
        return iTime;
    }
    
    /**
     * Return true if the given choice has the same instructional type and time
     * return true if the two choices have the same ime
     */
    public boolean sameTime(Choice choice) {
        return getInstructionalType().equals(choice.getInstructionalType()) &&
                (getTime() == null ? choice.getTime() == null : getTime().equals(choice.getTime()));
    }

    /**
     * Instructor(s) id of the choice, can be null if the section has no
     * instructor assigned
     * @return selected instructors
     */
    @Deprecated
    public String getInstructorIds() {
        if (hasInstructors()) {
            StringBuffer sb = new StringBuffer();
            for (Iterator<Instructor> i = getInstructors().iterator(); i.hasNext(); ) {
                Instructor instructor = i.next();
                sb.append(instructor.getId());
                if (i.hasNext()) sb.append(":");
            }
            return sb.toString();
        }
        return null;
    }

    /**
     * Instructor(s) name of the choice, can be null if the section has no
     * instructor assigned
     * @return selected instructors
     */
    @Deprecated
    public String getInstructorNames() {
        if (hasInstructors()) {
            StringBuffer sb = new StringBuffer();
            for (Iterator<Instructor> i = getInstructors().iterator(); i.hasNext(); ) {
                Instructor instructor = i.next();
                if (instructor.getName() != null)
                    sb.append(instructor.getName());
                else if (instructor.getExternalId() != null)
                    sb.append(instructor.getExternalId());
                if (i.hasNext()) sb.append(":");
            }
            return sb.toString();
        }
        return null;
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
     * Choice id combined from instructionalType, time and instructorIds in the
     * following format: instructionalType|time|instructorIds where time is of
     * format dayCode:startSlot:length:datePatternId
     * @return choice id
     */
    public String getId() {
        String ret = getInstructionalType() + "|";
        if (getTime() != null)
            ret += getTime().getDayCode() + ":" + getTime().getStartSlot() + ":" + getTime().getLength() + (getTime().getDatePatternId() == null ? "" : ":" + getTime().getDatePatternId());
        if (hasInstructors())
            ret += "|" + getInstructorIds();
        return ret;
    }

    /** Compare two choices, based on {@link Choice#getId()} */
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Choice))
            return false;
        return ((Choice) o).getId().equals(getId());
    }

    /** Choice hash id, based on {@link Choice#getId()} */
    @Override
    public int hashCode() {
        return iHashCode;
    }

    /**
     * List of sections of the instructional offering which represent this
     * choice. Note that there can be multiple sections with the same choice
     * (e.g., only if the room location differs).
     * @return set of sections for matching this choice
     */
    public Set<Section> getSections() {
        Set<Section> sections = new HashSet<Section>();
        for (Config config : getOffering().getConfigs()) {
            for (Subpart subpart : config.getSubparts()) {
                if (!subpart.getInstructionalType().equals(getInstructionalType()))
                    continue;
                for (Section section : subpart.getSections()) {
                    if (this.sameChoice(section))
                        sections.add(section);
                }
            }
        }
        return sections;
    }

    /**
     * List of parent sections of sections of the instructional offering which
     * represent this choice. Note that there can be multiple sections with the
     * same choice (e.g., only if the room location differs).
     * @return set of parent sections
     */
    public Set<Section> getParentSections() {
        Set<Section> parentSections = new HashSet<Section>();
        for (Config config : getOffering().getConfigs()) {
            for (Subpart subpart : config.getSubparts()) {
                if (!subpart.getInstructionalType().equals(getInstructionalType()))
                    continue;
                if (subpart.getParent() == null)
                    continue;
                for (Section section : subpart.getSections()) {
                    if (this.sameChoice(section) && section.getParent() != null)
                        parentSections.add(section.getParent());
                }
            }
        }
        return parentSections;
    }

    /**
     * Choice name: name of the appropriate subpart + long name of time +
     * instructor(s) name
     * @return choice name
     */
    public String getName() {
        return (getOffering().getSubparts(getInstructionalType()).iterator().next()).getName()
                + " "
                + (getTime() == null ? "" : getTime().getLongName(true))
                + (hasInstructors() ? " " + getInstructorNames(",") : "");
    }
    
    /** True if the instructional type is the same */
    public boolean sameInstructionalType(Section section) {
        return getInstructionalType().equals(section.getSubpart().getInstructionalType());
    }

    /** True if the time assignment is the same */
    public boolean sameTime(Section section) {
        return getTime() == null ? section.getTime() == null : getTime().equals(section.getTime());
    }
    
    /** True if the section contains all instructors of this choice */
    public boolean sameInstructors(Section section) {
        return !hasInstructors() || (section.hasInstructors() && section.getInstructors().containsAll(getInstructors()));
    }
    
    /** True if the time assignment as well as the instructor(s) are the same */
    public boolean sameChoice(Section section) {
        return sameInstructionalType(section) && sameTime(section) && sameInstructors(section);
    }

    @Override
    public String toString() {
        return getName();
    }
    
    /** Instructors of this choice 
     * @return list of instructors
     **/
    public List<Instructor> getInstructors() {
        return iInstructors;
    }
    
    /**
     * Has any instructors
     * @return return true if there is at least one instructor in this choice
     */
    public boolean hasInstructors() {
        return iInstructors != null && !iInstructors.isEmpty();
    }
    
    /**
     * Return number of instructors of this choice
     * @return number of instructors of this choice
     */
    public int nrInstructors() {
        return iInstructors == null ? 0 : iInstructors.size();
    }
}
