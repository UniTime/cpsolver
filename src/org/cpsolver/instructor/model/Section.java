package org.cpsolver.instructor.model;

import java.util.Collection;

import org.cpsolver.coursett.model.TimeLocation;

public class Section {
    private Long iId;
    private String iExternalId;
    private String iType;
    private String iName;
    private TimeLocation iTime;
    private String iRoom;
    private boolean iAllowOverlap;
    private boolean iCommon;
    
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
    
    public Long getSectionId() { return iId; }
    public String getExternalId() { return iExternalId; }
    public String getSectionType() { return iType; }
    public boolean hasSectionType() { return iType != null && !iType.isEmpty(); }
    public String getSectionName() { return iName; }
    public TimeLocation getTime() { return iTime; }
    public boolean hasTime() { return getTime() != null && getTime().getDayCode() != 0; }
    public String getRoom() { return iRoom; }
    public boolean hasRoom() { return iRoom != null && !iRoom.isEmpty(); }
    public boolean isAllowOverlap() { return iAllowOverlap; }
    public boolean isCommon() { return iCommon; }
    
    public boolean isOverlapping(Section section) {
        if (isAllowOverlap() || section.isAllowOverlap()) return false;
        if (getTime() == null || section.getTime() == null) return false;
        return getTime().hasIntersection(section.getTime());
    }
    
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
    
    public boolean isBackToBack(Section section) {
        if (getTime() == null || section.getTime() == null) return false;
        return getTime().shareWeeks(section.getTime()) && getTime().shareDays(section.getTime()) && 
                (getTime().getStartSlot() + getTime().getLength() == section.getTime().getStartSlot() || section.getTime().getStartSlot() + section.getTime().getLength() == getTime().getStartSlot());
    }
    
    public boolean isSameRoom(Section section) {
        return hasRoom() == section.hasRoom() && (!hasRoom() || getRoom().equals(section.getRoom()));
    }

    public boolean isSameSectionType(Section section) {
        return hasSectionType() == section.hasSectionType() && (!hasSectionType() || getSectionType().equals(section.getSectionType()));
    }
    
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
    
    public int share(Section section) {
        if (getTime() != null && section.getTime() != null && (isAllowOverlap() || section.isAllowOverlap()) && getTime().hasIntersection(section.getTime()))
            return getTime().nrSharedDays(section.getTime()) * getTime().nrSharedHours(section.getTime());
        else
            return 0;
    }
    
    public int share(TimeLocation time) {
        if (getTime() != null && time != null && getTime().hasIntersection(time))
            return getTime().nrSharedDays(time) * getTime().nrSharedHours(time);
        else
            return 0;
    }
    
    public int share(Collection<Section> sections) {
        if (sections.contains(this)) return 0;
        int ret = 0;
        for (Section section : sections)
            ret += share(section);
        return ret;
    }
    
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
