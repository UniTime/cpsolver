package net.sf.cpsolver.studentsct.model;

import java.util.TreeSet;

public class Config {
    private long iId = -1;
    private String iName = null;
    private Offering iOffering = null;
    private TreeSet iSubparts = new TreeSet();
    
    public Config(long id, String name, Offering offering) {
        iId = id;
        iName = name;
        iOffering = offering;
        iOffering.getConfigs().add(this);
    }
    
    public long getId() {
        return iId;
    }
    
    public String getName() {
        return iName;
    }
    
    public Offering getOffering() {
        return iOffering;
    }
    
    public TreeSet getSubparts() {
        return iSubparts;
    }
}
