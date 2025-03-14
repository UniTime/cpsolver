package org.cpsolver.instructor.model;

import java.util.Collection;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.instructor.criteria.DifferentLecture;

/**
 * Section. A section (part of a teaching request that needs an instructor) has an id, a name, a time, a room.
 * A section may be allowed to overlap in time (in which case the overlapping time is to be minimized) and/or
 * marked as common. This is, for instance, to be able to ensure that all assignments of a course that are
 * given to a single instructor share the same lecture.  
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Instructor Sectioning)<br>
 *          Copyright (C) 2016 Tomas Muller<br>
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
public class Section {
    private Long iId;
    private String iExternalId;
    private String iType;
    private String iName;
    private TimeLocation iTime;
    private String iRoom;
    private boolean iAllowOverlap;
    private boolean iCommon;
    
    /**
     * Constructor
     * @param id section unique id
     * @param externalId section external id
     * @param type instructional type
     * @param name section name
     * @param time section time assignment
     * @param room section room assignment
     * @param allowOverlap can this section overlap with some other section
     * @param common is this a common part of the course
     */
    public Section(long id, String externalId, String type, String name, TimeLocation time, String room, boolean allowOverlap, boolean common) {
        iId = id;
        iExternalId = externalId;
        iType = type;
        iName = name;
        iTime = time;
        iRoom = room;
        iAllowOverlap = allowOverlap;
        iCommon = common;
    }
    
    /**
     * Section unique id that was provided in the constructor
     * @return section unique id
     */
    public Long getSectionId() { return iId; }
    
    /**
     * Section external unique id that was provided in the constructor
     * @return section external id
     */
    public String getExternalId() { return iExternalId; }
    
    /**
     * Section instructional type (e.g., Lecture) that was provided in the constructor
     * @return section instructional type
     */
    public String getSectionType() { return iType; }
    
    /**
     * Has section type filled in?
     * @return true, if there is a section instructional type filled in
     */
    public boolean hasSectionType() { return iType != null && !iType.isEmpty(); }
    
    /**
     * Section name that was provided in the constructor
     * @return section name
     */
    public String getSectionName() { return iName; }
    
    /**
     * Section time that was provided in the constructor
     * @return section time
     */
    public TimeLocation getTime() { return iTime; }
    
    /**
     * Has section time filled in?
     * @return true if section is assigned in time
     */
    public boolean hasTime() { return getTime() != null && getTime().getDayCode() != 0; }
    
    /**
     * Section room (or rooms)
     * @return section room
     */
    public String getRoom() { return iRoom; }
    
    /**
     * Has section room filled in?
     * @return true if section is assigned in space
     */
    public boolean hasRoom() { return iRoom != null && !iRoom.isEmpty(); }
    
    /**
     * Are time overlaps with other sections and with prohibited time preferences allowed? If true, the number of overlapping time slots should be minimized instead.
     * @return true if other sections can overlap with this section or if the student can teach this section even when he/she is unavailable
     */
    public boolean isAllowOverlap() { return iAllowOverlap; }
    
    /**
     * Is common part of the course (e.g., a lecture)? It is possible to either require all assignments of a course that are given to the same instructor to have
     * to share the common part (when {@link TeachingRequest#getSameCommonPreference()} is true) or to minimize the different sections that are not shared among all the assignments
     * (using {@link DifferentLecture} criterion and {@link TeachingRequest#nrSameLectures(TeachingRequest)}). 
     * @return true if this section forms a common part of the course
     */
    public boolean isCommon() { return iCommon; }
    
    /**
     * Check if this section overlaps in time with some other section
     * @param section the other section
     * @return true, if neither of the two sections allow for overlap and they are assigned in overlapping times (see {@link TimeLocation#hasIntersection(TimeLocation)})
     */
    public boolean isOverlapping(Section section) {
        if (isAllowOverlap() || section.isAllowOverlap()) return false;
        if (getTime() == null || section.getTime() == null) return false;
        return getTime().hasIntersection(section.getTime());
    }
    
    /**
     * Check if this section overlaps in time with at least one of the given sections
     * @param sections the other sections
     * @return true, if there is a section among the sections that overlaps in time with this section (see {@link Section#isOverlapping(Section)})
     */
    public boolean isOverlapping(Collection<Section> sections) {
        if (isAllowOverlap()) return false;
        if (getTime() == null) return false;
        if (sections.contains(this)) return false;
        for (Section section : sections) {
            if (section.isAllowOverlap()) continue;
            if (section.getTime() == null) continue;
            if (getTime().hasIntersection(section.getTime())) return true;
        }
        return false;
    }
    
    /**
     * Check if this section is back to back with some other section
     * @param section the other section
     * @return true, if this section is back-to-back with the other section
     */
    public boolean isBackToBack(Section section) {
        if (getTime() == null || section.getTime() == null) return false;
        return getTime().shareWeeks(section.getTime()) && getTime().shareDays(section.getTime()) && 
                (getTime().getStartSlot() + getTime().getLength() == section.getTime().getStartSlot() || section.getTime().getStartSlot() + section.getTime().getLength() == getTime().getStartSlot());
    }
    
    /**
     * Check if this section has the same days as some other section
     * @param section the other section
     * @return 0 if all the days are different, 1 if all the days are the same
     */
    public double percSameDays(Section section) {
        if (getTime() == null || section.getTime() == null) return 0;
        double ret = 0.0;
        for (int dayCode: Constants.DAY_CODES)
            if ((getTime().getDayCode() & dayCode) != 0 && (section.getTime().getDayCode() & dayCode) != 0) ret ++;
        return ret / Math.min(getTime().getNrMeetings(), section.getTime().getNrMeetings());
    }
    
    /**
     * Check if this section is placed in the same room as the other section
     * @param section the other section
     * @return true, if both sections have no room or if they have the same room
     */
    public boolean isSameRoom(Section section) {
        return hasRoom() == section.hasRoom() && (!hasRoom() || getRoom().equals(section.getRoom()));
    }

    /**
     * Check if this section has the same instructional type as the other section
     * @param section the other section
     * @return true, if both sections have no instructional type or if they have the same instructional type
     */
    public boolean isSameSectionType(Section section) {
        return hasSectionType() == section.hasSectionType() && (!hasSectionType() || getSectionType().equals(section.getSectionType()));
    }
    
    /**
     * Check if this section is back-to-back with some other section in the list
     * @param sections the other sections
     * @param diffRoomWeight different room penalty (should be between 1 and 0)
     * @param diffTypeWeight different instructional type penalty (should be between 1 and 0)
     * @return 1.0 if there is a section in the list that is back-to-back and in the same room and with the same type, 0.0 if there is no back-to-back section in the list, etc.
     * If there are multiple back-to-back sections, the best back-to-back value is returned.
     */
    public double countBackToBacks(Collection<Section> sections, double diffRoomWeight, double diffTypeWeight) {
        if (sections.contains(this)) return 0.0;
        double btb = 0;
        for (Section section : sections)
            if (isBackToBack(section)) {
                double w = 1.0;
                if (!isSameRoom(section)) w *= diffRoomWeight;
                if (!isSameSectionType(section)) w *= diffTypeWeight;
                if (w > btb) btb = w;
            }
        return btb;
    }
    
    /**
     * Check if this section has the same days with some other section in the list
     * @param sections the other sections
     * @param diffRoomWeight different room penalty (should be between 1 and 0)
     * @param diffTypeWeight different instructional type penalty (should be between 1 and 0)
     * @return 1.0 if there is a section in the list that has the same days and in the same room and with the same type, 0.0 if there is no same days section in the list, etc.
     * If there are multiple same days sections, the best same days value is returned.
     */
    public double countSameDays(Collection<Section> sections, double diffRoomWeight, double diffTypeWeight) {
        if (sections.contains(this)) return 0.0;
        double sd = 0;
        for (Section section : sections) {
            double w = percSameDays(section);
            if (!isSameRoom(section)) w *= diffRoomWeight;
            if (!isSameSectionType(section)) w *= diffTypeWeight;
            if (w > sd) sd = w;
        }
        return sd;
    }
    
    /**
     * Check if this section has the same room with some other section in the list
     * @param sections the other sections
     * @param diffTypeWeight different instructional type penalty (should be between 1 and 0)
     * @return 1.0 if there is a section in the list that has the same room and with the same type, 0.0 if there is no same room section in the list, etc.
     * If there are multiple same room sections, the best same room value is returned.
     */
    public double countSameRooms(Collection<Section> sections, double diffTypeWeight) {
        if (sections.contains(this)) return 0.0;
        double sr = 0;
        for (Section section : sections) {
            if (isSameRoom(section)) {
                double w = 1.0;
                if (!isSameSectionType(section)) w *= diffTypeWeight;
                if (w > sr) sr = w;
            }
        }
        return sr;
    }
    
    /**
     * If this section can overlap in time with the other section, compute the number of overlapping time slots
     * @param section the other section
     * @return number of shared days times number of shared slots (a day)
     */
    public int share(Section section) {
        if (getTime() != null && section.getTime() != null && (isAllowOverlap() || section.isAllowOverlap()) && getTime().hasIntersection(section.getTime()))
            return getTime().nrSharedDays(section.getTime()) * getTime().nrSharedHours(section.getTime());
        else
            return 0;
    }
    
    /**
     * Compute the number of overlapping time slots between this section and the given time
     * @param time the given time
     * @return number of shared days times number of shared slots (a day)
     */
    public int share(TimeLocation time) {
        if (getTime() != null && time != null && getTime().hasIntersection(time))
            return getTime().nrSharedDays(time) * getTime().nrSharedHours(time);
        else
            return 0;
    }
    
    /**
     * If this section can overlap in time with any of the given section, compute the number of overlapping time slots
     * @param sections the other sections
     * @return number of shared days times number of shared slots (a day)
     */
    public int share(Collection<Section> sections) {
        if (sections.contains(this)) return 0;
        int ret = 0;
        for (Section section : sections)
            ret += share(section);
        return ret;
    }
    
    /**
     * Format the time statement
     * @param useAmPm use 12-hours or 24-hours format
     * @return &lt;Days of week&gt; &lt;Start time&gt; - &lt;End time&gt;
     */
    public String getTimeName(boolean useAmPm) {
        if (getTime() == null || getTime().getDayCode() == 0) return "-";
        return getTime().getDayHeader() + " " + getTime().getStartTimeHeader(useAmPm) + " - " + getTime().getEndTimeHeader(useAmPm);
    }
    
    @Override
    public String toString() {
        return (getExternalId() != null ? (getSectionType() == null ? "" : getSectionType() + " ") + getExternalId() :
            getSectionName() != null ? getSectionName() : getTime() != null ? getTime().getName(true) : "S" + getSectionId());
    }
    
    @Override
    public int hashCode() {
        return getSectionId().hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Section)) return false;
        Section s = (Section)o;
        return getSectionId().equals(s.getSectionId());
    }
}
