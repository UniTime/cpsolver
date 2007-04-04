package net.sf.cpsolver.studentsct.model;

import java.util.Iterator;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Value;

public class Enrollment extends Value implements Comparable {
    private Request iRequest = null;
    private Config iConfig = null;
    private Set iAssignments = null;
    
    public static double sPriorityWeight = 0.90;
    public static double sAlterativeWeight = 0.1;
    public static double sSelectedWeight = 1.1;
    public static double sWaitlistedWeight = 1.01;
    
    public Enrollment(Request request, double value, Config config, Set assignments) {
        super(request);
        iRequest = request;
        iConfig = config;
        iAssignments = assignments;
        iValue = value;
    }
    
    public Student getStudent() {
        return iRequest.getStudent();
    }
    
    public Request getRequest() {
        return iRequest;
    }
    
    public boolean isCourseRequest() {
        return iConfig!=null;
    }
    
    public Offering getOffering() {
        return iConfig.getOffering();
    }
    
    public Config getConfig() {
        return iConfig;
    }
    
    public Set getAssignments() {
        return iAssignments;
    }
    
    public boolean isOverlapping(Enrollment enrl) {
        for (Iterator i=enrl.getAssignments().iterator();i.hasNext();) {
            Assignment assignment = (Assignment)i.next();
            if (assignment.isOverlapping(getAssignments())) return true;
        }
        return false;
    }
    
    public double percentWaitlisted() {
        if (!isCourseRequest()) return 0.0;
        CourseRequest courseRequest = (CourseRequest)getRequest();
        int nrWaitlisted = 0;
        for (Iterator i=getAssignments().iterator();i.hasNext();) {
            Section section = (Section)i.next();
            if (courseRequest.isWaitlisted(section))
                nrWaitlisted++;
        }
        return ((double)nrWaitlisted)/getAssignments().size();
    }

    public double percentSelected() {
        if (!isCourseRequest()) return 0.0;
        CourseRequest courseRequest = (CourseRequest)getRequest();
        int nrSelected = 0;
        for (Iterator i=getAssignments().iterator();i.hasNext();) {
            Section section = (Section)i.next();
            if (courseRequest.isSelected(section))
                nrSelected++;
        }
        return ((double)nrSelected)/getAssignments().size();
    }
    
    public boolean isWaitlisted() {
        if (!isCourseRequest()) return false;
        CourseRequest courseRequest = (CourseRequest)getRequest();
        for (Iterator i=getAssignments().iterator();i.hasNext();) {
            Section section = (Section)i.next();
            if (!courseRequest.isWaitlisted(section)) return false;
        }
        return true;
    }
    
    public boolean isSelected() {
        if (!isCourseRequest()) return false;
        CourseRequest courseRequest = (CourseRequest)getRequest();
        for (Iterator i=getAssignments().iterator();i.hasNext();) {
            Section section = (Section)i.next();
            if (!courseRequest.isSelected(section)) return false;
        }
        return true;
    }

    public double toDouble() {
        return 
            -iValue * 
            Math.pow(sPriorityWeight,getRequest().getPriority()) * 
            (getRequest().isAlternative()?sAlterativeWeight:1.0) *
            Math.pow(sSelectedWeight,percentSelected()) * 
            Math.pow(sWaitlistedWeight,percentWaitlisted());
    }
    
    public String getName() {
        String ret = (getConfig()==null?"":getConfig().getName());
        for (Iterator i=getAssignments().iterator();i.hasNext();) {
            Assignment assignment = (Assignment)i.next();
            ret+="\n  "+assignment.toString()+(i.hasNext()?",":"");
        }
        return ret;
    }
    
    public String toString() {
        return getName()+",\n  value="+toDouble();
    }
    
    public boolean equals(Object o) {
        if (o==null || !(o instanceof Enrollment)) return false;
        Enrollment e = (Enrollment)o;
        if (!getConfig().equals(e.getConfig())) return false;
        if (!getRequest().equals(e.getRequest())) return false;
        if (!getAssignments().equals(e.getAssignments())) return false;
        return true;
    }
}