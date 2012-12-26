package net.sf.cpsolver.studentsct.model;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import net.sf.cpsolver.coursett.model.TimeLocation;

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
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
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
    private String iInstructorIds = null;
    private String iInstructorNames = null;
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
     * @param instructorIds
     *            instructor(s) id
     * @param instructorNames
     *            instructor(s) name
     */
    public Choice(Offering offering, String instructionalType, TimeLocation time, String instructorIds,
            String instructorNames) {
        iOffering = offering;
        iInstructionalType = instructionalType;
        iTime = time;
        iInstructorIds = instructorIds;
        iInstructorNames = instructorNames;
        iHashCode = getId().hashCode();
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
            iInstructorIds = choiceId.substring(choiceId.indexOf('|') + 1);
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

    /** Instructional offering to which this choice belongs */
    public Offering getOffering() {
        return iOffering;
    }

    /**
     * Instructional type (e.g., Lecture, Recitation or Laboratory) to which
     * this choice belongs
     */
    public String getInstructionalType() {
        return iInstructionalType;
    }

    /** Time location of the choice */
    public TimeLocation getTime() {
        return iTime;
    }

    /**
     * Instructor(s) id of the choice, can be null if the section has no
     * instructor assigned
     */
    public String getInstructorIds() {
        return iInstructorIds;
    }

    /**
     * Instructor(s) name of the choice, can be null if the section has no
     * instructor assigned
     */
    public String getInstructorNames() {
        return iInstructorNames;
    }
    
    /**
     * Set instructor(s) id and name of the choice, can be null if the section has no
     * instructor assigned
     */
    public void setInstructor(String instructorIds, String instructorNames) {
        iInstructorIds = instructorIds;
        iInstructorNames = instructorNames;
    }

    /**
     * Choice id combined from instructionalType, time and instructorIds in the
     * following format: instructionalType|time|instructorIds where time is of
     * format dayCode:startSlot:length:datePatternId
     */
    public String getId() {
        String ret = getInstructionalType() + "|";
        if (getTime() != null)
            ret += getTime().getDayCode() + ":" + getTime().getStartSlot() + ":" + getTime().getLength()
                    + (getTime().getDatePatternId() == null ? "" : ":" + getTime().getDatePatternId());
        if (getInstructorIds() != null)
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
     */
    public Set<Section> getSections() {
        Set<Section> sections = new HashSet<Section>();
        for (Config config : getOffering().getConfigs()) {
            for (Subpart subpart : config.getSubparts()) {
                if (!subpart.getInstructionalType().equals(getInstructionalType()))
                    continue;
                for (Section section : subpart.getSections()) {
                    if (section.getChoice().equals(this))
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
                    if (section.getChoice().equals(this) && section.getParent() != null)
                        parentSections.add(section.getParent());
                }
            }
        }
        return parentSections;
    }

    /**
     * Choice name: name of the appropriate subpart + long name of time +
     * instructor(s) name
     */
    public String getName() {
        return (getOffering().getSubparts(getInstructionalType()).iterator().next()).getName()
                + " "
                + (getTime() == null ? "" : getTime().getLongName())
                + (getInstructorIds() == null ? "" : getInstructorNames() != null ? " " + getInstructorNames() : " "
                        + getInstructorIds());
    }

    @Override
    public String toString() {
        return getName();
    }
}
