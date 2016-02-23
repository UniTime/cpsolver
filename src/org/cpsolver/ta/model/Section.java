package org.cpsolver.ta.model;

import java.util.Collection;

import org.cpsolver.coursett.model.TimeLocation;

public class Section {
    private Long iId;
    private String iName;
    private TimeLocation iTime;
    private String iRoom;
    private boolean iAllowOverlap;
    
    public Section(Long id, String name, TimeLocation time, String room, boolean allowOverlap) {
        iId = id;
        iName = name;
        iTime = time;
        iRoom = room;
        iAllowOverlap = allowOverlap;
    }
    
    public Long getSectionId() { return iId; }
    public String getSectionName() { return iName; }
    public TimeLocation getTime() { return iTime; }
    public boolean hasTime() { return getTime() != null && getTime().getDayCode() != 0; }
    public String getRoom() { return iRoom; }
    public boolean hasRoom() { return iRoom != null && !iRoom.isEmpty(); }
    public boolean isAllowOverlap() { return iAllowOverlap; }
    
    public boolean isOverlapping(Section section) {
        if (isAllowOverlap() || section.isAllowOverlap()) return false;
        if (getTime() == null || section.getTime() == null) return false;
        return getTime().hasIntersection(section.getTime());
    }
    
    public boolean isOverlapping(Collection<Section> sections) {
        if (isAllowOverlap()) return false;
        if (getTime() == null) return false;
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
    
    public boolean isBackToBack(Collection<Section> sections) {
        for (Section section : sections)
            if (isBackToBack(section)) return true;
        return false;
    }
    
    public boolean isSameRoom(Section section) {
        return hasRoom() == section.hasRoom() && (!hasRoom() || getRoom().equals(section.getRoom()));
    }
    
    public boolean isBackToBackSameRoom(Section section) {
        if (getTime() == null || section.getTime() == null) return false;
        return getTime().shareWeeks(section.getTime()) && getTime().shareDays(section.getTime()) && isSameRoom(section) &&
                (getTime().getStartSlot() + getTime().getLength() == section.getTime().getStartSlot() || section.getTime().getStartSlot() + section.getTime().getLength() == getTime().getStartSlot());
    }
    
    public boolean isBackToBackSameRoom(Collection<Section> sections) {
        for (Section section : sections)
            if (isBackToBackSameRoom(section)) return true;
        return false;
    }

    public int share(Section section) {
        if (getTime() != null && section.getTime() != null && (isAllowOverlap() || section.isAllowOverlap()) && getTime().hasIntersection(section.getTime()))
            return getTime().nrSharedDays(section.getTime()) * getTime().nrSharedHours(section.getTime());
        else
            return 0;
    }
    
    public int share(Collection<Section> sections) {
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
        return ((getSectionName() == null ? "" : getSectionName() + " ") +
                (hasTime() ? getTime().getDayHeader() + " " + getTime().getStartTimeHeader(true) + " - " + getTime().getEndTimeHeader(true) + " " : "") +
                (hasRoom() ? getRoom() : "")).trim();
    }
}
