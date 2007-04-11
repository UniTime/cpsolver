package net.sf.cpsolver.studentsct.model;

import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import net.sf.cpsolver.coursett.model.TimeLocation;

public class Choice {
    private Offering iOffering = null;
    private String iInstructionalType = null;
    private TimeLocation iTime = null;
    private String iInstructorIds = null;
    private String iInstructorNames = null;
    private int iHashCode;
    
    public Choice(Offering offering, String instructionalType, TimeLocation time, String instructorIds, String instructorNames) {
        iOffering = offering;
        iInstructionalType = instructionalType;
        iTime = time;
        iInstructorIds = instructorIds;
        iInstructorNames = instructorNames;
        iHashCode = getId().hashCode();
    }
    
    public Choice(Offering offering, String choiceId) {
        iOffering = offering;
        iInstructionalType = choiceId.substring(0, choiceId.indexOf('|'));
        choiceId = choiceId.substring(choiceId.indexOf('|')+1);
        String timeId = null;
        if (choiceId.indexOf('|')<0) {
            timeId = choiceId;
        } else {
            timeId = choiceId.substring(0, choiceId.indexOf('|'));
            iInstructorIds = choiceId.substring(choiceId.indexOf('|')+1);
        }
        if (timeId!=null && timeId.length()>0) {
            StringTokenizer s = new StringTokenizer(timeId,":");
            int dayCode = Integer.parseInt(s.nextToken());
            int startSlot = Integer.parseInt(s.nextToken());
            int length = Integer.parseInt(s.nextToken());
            Long datePatternId = (s.hasMoreElements()?Long.valueOf(s.nextToken()):null);
            iTime = new TimeLocation(dayCode, startSlot, length, 0, 0, datePatternId, "N/A", new BitSet(), 0);
        }
        iHashCode = getId().hashCode();
    }
    
    public Offering getOffering() {
        return iOffering;
    }
    
    public String getInstructionalType() {
        return iInstructionalType;
    }
    
    public TimeLocation getTime() {
        return iTime;
    }
    
    public String getInstructorIds() {
        return iInstructorIds;
    }
    
    public String getInstructorNames() {
        return iInstructorNames;
    }
    
    public String getId() {
        String ret = getInstructionalType()+"|";
        if (getTime()!=null)
            ret += getTime().getDayCode()+":"+
                getTime().getStartSlot()+":"+
                getTime().getLength()+
                (getTime().getDatePatternId()==null?"":":"+getTime().getDatePatternId());
        if (getInstructorIds()!=null)
            ret += "|"+getInstructorIds();
        return ret;
    }
    
    public boolean equals(Object o) {
        if (o==null || !(o instanceof Choice)) return false;
        return ((Choice)o).getId().equals(getId());
    }
    
    public int hashCode() {
        return iHashCode;
    }
    
    public HashSet getSections() {
        HashSet sections = new HashSet();
        for (Enumeration e=getOffering().getConfigs().elements();e.hasMoreElements();) {
            Config config = (Config)e.nextElement();
            for (Iterator f=config.getSubparts().iterator();f.hasNext();) {
                Subpart subpart = (Subpart)f.next();
                if (!subpart.getInstructionalType().equals(getInstructionalType())) continue;
                for (Enumeration g=subpart.getSections().elements();g.hasMoreElements();) {
                    Section section = (Section)g.nextElement();
                    if (section.getChoice().equals(this))
                        sections.add(section);
                }
            }
        }
        return sections;
    }
    
    public HashSet getParentSections() {
        HashSet parentSections = new HashSet();
        for (Enumeration e=getOffering().getConfigs().elements();e.hasMoreElements();) {
            Config config = (Config)e.nextElement();
            for (Iterator f=config.getSubparts().iterator();f.hasNext();) {
                Subpart subpart = (Subpart)f.next();
                if (!subpart.getInstructionalType().equals(getInstructionalType())) continue;
                if (subpart.getParent()==null) continue;
                for (Enumeration g=subpart.getSections().elements();g.hasMoreElements();) {
                    Section section = (Section)g.nextElement();
                    if (section.getChoice().equals(this) && section.getParent()!=null)
                        parentSections.add(section.getParent());
                }
            }
        }
        return parentSections;
    }
    
    public String getName() {
        return 
            ((Subpart)getOffering().getSubparts(getInstructionalType()).iterator().next()).getName()+" "+
            (getTime()==null?"":getTime().getLongName())+
            (getInstructorIds()==null?"":getInstructorNames()!=null?" "+getInstructorNames():" "+getInstructorIds());
    }
    
    public String toString() {
        return getName();
    }
}
