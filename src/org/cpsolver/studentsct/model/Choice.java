package org.cpsolver.studentsct.model;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.util.ToolBox;


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
public class Choice {
    private Long iSectionId = null;
    private Long iSubpartId = null;
    private Long iConfigId = null;
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
        iSectionId = section.getId();
        iSubpartId = section.getSubpart().getId();
        // iConfigId = section.getSubpart().getConfig().getId();
    }
    
    /**
     * Constructor
     * @param config configuration to base the choice on
     */
    public Choice(Config config) {
        this(config.getOffering(), "N/A", null, null);
        iConfigId = config.getId();
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
        String[] choices = choiceId.split("\\|");
        iInstructionalType = choices[0];
        if (choices.length > 1 && !choices[1].isEmpty()) {
            String[] times = choices[1].split(":");
            int dayCode = Integer.parseInt(times[0]);
            int startSlot = Integer.parseInt(times[1]);
            int length = Integer.parseInt(times[2]);
            Long datePatternId = (times.length > 3 ? Long.valueOf(times[3]) : null);
            iTime = new TimeLocation(dayCode, startSlot, length, 0, 0, datePatternId, "N/A", new BitSet(), 0);
        }
        if (choices.length > 2 && !choices[2].isEmpty()) {
            iInstructors = new ArrayList<Instructor>();
            for (String id: choices[2].split(":"))
                iInstructors.add(new Instructor(Long.parseLong(id)));
        }
        if (choices.length > 3 && !choices[3].isEmpty()) {
            String[] ids = choices[3].split(":"); 
            iSectionId = (ids.length < 1 || ids[0].isEmpty() ? null : Long.valueOf(ids[0]));
            iSubpartId = (ids.length < 2 || ids[1].isEmpty() ? null : Long.valueOf(ids[1]));
            iConfigId = (ids.length < 3 || ids[2].isEmpty() ? null : Long.valueOf(ids[2]));
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
     * return true if the two choices have the same time
     */
    public boolean sameTime(Choice choice) {
        return getInstructionalType().equals(choice.getInstructionalType()) && sameTime(getTime(), choice.getTime());
    }
    
    private static boolean sameTime(TimeLocation t1, TimeLocation t2) {
        if (t1 == null) return (t2 == null);
        if (t2 == null) return false;
        if (t1.getStartSlot() != t2.getStartSlot()) return false;
        if (t1.getLength() != t2.getLength()) return false;
        if (t1.getDayCode() != t2.getDayCode()) return false;
        return ToolBox.equals(t1.getDatePatternId(), t2.getDatePatternId());
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
        ret += "|" + (hasInstructors() ? getInstructorIds() : "");
        ret += "|" + (iSectionId == null ? "" : iSectionId) + ":" + (iSubpartId == null ? "" : iSubpartId) + ":" + (iConfigId == null ? "" : iConfigId);
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
        return getInstructionalType() != null && getInstructionalType().equals(section.getSubpart().getInstructionalType());
    }

    /** True if the time assignment is the same */
    public boolean sameTime(Section section) {
        return sameTime(getTime(), section.getTime());
    }
    
    /** True if the section contains all instructors of this choice */
    public boolean sameInstructors(Section section) {
        return !hasInstructors() || (section.hasInstructors() && section.getInstructors().containsAll(getInstructors()));
    }
    
    /** True if the offering is the same */
    public boolean sameOffering(Section section) {
        return iOffering != null && iOffering.equals(section.getSubpart().getConfig().getOffering());
    }
    
    /** True if the time assignment as well as the instructor(s) are the same */
    public boolean sameChoice(Section section) {
        return sameOffering(section) && sameInstructionalType(section) && sameTime(section) && sameInstructors(section);
    }
    
    /** True if the section is the very same */
    public boolean sameSection(Section section) {
        return iSectionId != null && iSectionId.equals(section.getId());
    }
    
    /** True if the subpart is the very same */
    public boolean sameSubart(Section section) {
        return iSubpartId != null && iSubpartId.equals(section.getSubpart().getId());
    }
    
    /** True if the configuration is the very same */
    public boolean sameConfiguration(Section section) {
        return iConfigId != null && iConfigId.equals(section.getSubpart().getConfig().getId()); 
    }
    
    /** True if the configuration is the very same */
    public boolean sameConfiguration(Enrollment enrollment) {
        return iConfigId != null && enrollment.getConfig() != null && iConfigId.equals(enrollment.getConfig().getId()); 
    }
    
    /** True if the configuration is the very same */
    public boolean sameSection(Enrollment enrollment) {
        if (iSectionId == null || !enrollment.isCourseRequest()) return false;
        for (Section section: enrollment.getSections())
            if (iSectionId.equals(section.getId())) return true;
        return false; 
    }
    
    /** True if this choice is applicable to the given section (that is, the choice is a config choice or with the same subpart / instructional type) */
    public boolean isMatching(Section section) {
        if (iConfigId != null) return true;
        if (iSubpartId != null && iSubpartId.equals(section.getSubpart().getId())) return true;
        if (iSubpartId == null && iInstructionalType != null && iInstructionalType.equals(section.getSubpart().getInstructionalType())) return true;
        return false; 
    }
    
    /** section id */
    public Long getSectionId() { return iSectionId; }
    /** subpart id */
    public Long getSubpartId() { return iSubpartId; }
    /** config id */
    public Long getConfigId() { return iConfigId; }

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
