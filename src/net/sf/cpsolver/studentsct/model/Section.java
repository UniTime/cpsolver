package net.sf.cpsolver.studentsct.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimeLocation;

public class Section implements Assignment {
    private long iId = -1;
    private String iName = null;
    private Subpart iSubpart = null;
    private Section iParent = null;
    private Placement iPlacement = null;
    private int iLimit = 0;
    private HashSet iEnrollments = new HashSet();
    private Choice iChoice = null;
    
    public Section(long id, int limit, String name, Subpart subpart, Placement placement, String instructorIds, String instructorNames) {
        iId = id;
        iLimit = limit;
        iName = name;
        iSubpart = subpart; 
        iSubpart.getSections().add(this);
        iPlacement = placement;
        iChoice = new Choice(getSubpart().getConfig().getOffering(),getSubpart().getInstructionalType(), getTime(), instructorIds, instructorNames);
    }
    
    public long getId() {
        return iId;
    }
    
    public int getLimit() {
        return iLimit;
    }
    
    public String getName() {
        return iName;
    }
    
    public Subpart getSubpart() {
        return iSubpart;
    }

    public Section getParent() {
        return iParent;
    }
    
    public void setParent(Section parent) {
        iParent = parent;
    }

    public Placement getPlacement() {
        return iPlacement;
    }
    
    public TimeLocation getTime() {
        return (iPlacement==null?null:iPlacement.getTimeLocation());
    }
    
    public int getNrRooms() {
        return (iPlacement==null?0:iPlacement.getNrRooms());
    }
    
    public Vector getRooms() {
        if (iPlacement==null) return null;
        if (iPlacement.getRoomLocations()==null && iPlacement.getRoomLocation()!=null) {
            Vector ret = new Vector(1);
            ret.add(iPlacement.getRoomLocation());
            return ret;
        }
        return iPlacement.getRoomLocations();
    }
    
    public boolean isOverlapping(Assignment assignment) {
        if (getTime()==null || assignment.getTime()==null) return false;
        return getTime().hasIntersection(assignment.getTime());
    }

    public boolean isOverlapping(Set assignments) {
        if (getTime()==null) return false;
        for (Iterator i=assignments.iterator();i.hasNext();) {
            Assignment assignment = (Assignment)i.next();
            if (assignment.getTime()==null) continue;
            if (getTime().hasIntersection(assignment.getTime())) return true;
        }
        return false;
    }
    
    public void assigned(Enrollment enrollment) {
        iEnrollments.add(enrollment);
    }
    
    public void unassigned(Enrollment enrollment) {
        iEnrollments.remove(enrollment);
    }
    
    public Set getEnrollments() {
        return iEnrollments;
    }
    
    public String toString() {
        return getName()+
            (getTime()==null?"":" "+getTime().getLongName())+
            (getNrRooms()==0?"":" "+getPlacement().getRoomName(","))+
            (getChoice().getInstructorNames()==null?"":" "+getChoice().getInstructorNames());
    }
    
    public Choice getChoice() {
        return iChoice;
    }
}
