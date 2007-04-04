package net.sf.cpsolver.studentsct.model;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Vector;

public class Subpart implements Comparable {
    private long iId = -1;
    private String iInstructionalType = null;
    private String iName = null;
    private Vector iSections = new Vector();
    private Config iConfig = null;
    private Subpart iParent = null;

    public Subpart(long id, String itype, String name, Config config) {
        iId = id;
        iInstructionalType = itype;
        iName = name;
        iConfig = config;
        iConfig.getSubparts().add(this);
    }
    
    public long getId() {
        return iId;
    }
    
    public String getInstructionalType() {
        return iInstructionalType;
    }
    
    public String getName() {
        return iName;
    }
    
    public Config getConfig() {
        return iConfig;
    }

    public Vector getSections() {
        return iSections;
    }
    
    public Subpart getParent() {
        return iParent;
    }
    
    public void setParent(Subpart parent) {
        iParent = parent;
    }
    
    public String toString() {
        return getName();
    }
    
    public boolean isParentOf(Subpart subpart) {
        if (subpart.getParent()==null) return false;
        if (subpart.getParent().equals(this)) return true;
        return isParentOf(subpart.getParent());
    }
    
    public int compareTo(Object o) {
        if (o==null || !(o instanceof Subpart)) {
            return -1;
        }
        Subpart s = (Subpart)o;
        if (isParentOf(s)) return -1;
        if (s.isParentOf(this)) return 1;
        return Double.compare(getId(),s.getId());
    }
    
    public HashSet getChoices() {
        HashSet choices = new HashSet();
        for (Enumeration e=getSections().elements();e.hasMoreElements();) {
            choices.add(((Section)e.nextElement()).getChoice());
        }
        return choices;
    }
}
