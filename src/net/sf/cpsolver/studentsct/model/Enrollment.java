package net.sf.cpsolver.studentsct.model;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.util.ToolBox;

public class Enrollment extends Value implements Comparable {
    private Request iRequest = null;
    private Config iConfig = null;
    private Set iAssignments = null;
    
    public static double sPriorityWeight = 0.90;
    public static double sAlterativeWeight = 1.0;
    public static double sInitialWeight = 1.2;
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
        return (iConfig==null?null:iConfig.getOffering());
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
    
    public double percentInitial() {
        if (getRequest().getInitialAssignment()==null) return 0.0;
        Enrollment inital = (Enrollment)getRequest().getInitialAssignment();
        int nrInitial = 0;
        for (Iterator i=getAssignments().iterator();i.hasNext();) {
            Section section = (Section)i.next();
            if (inital.getAssignments().contains(section))
                nrInitial++;
        }
        return ((double)nrInitial)/getAssignments().size();
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
            Math.pow(sInitialWeight,percentInitial()) *
            Math.pow(sSelectedWeight,percentSelected()) * 
            Math.pow(sWaitlistedWeight,percentWaitlisted()) *
            getRequest().getWeight() * 
            (getStudent().isDummy()?Student.sDummyStudentWeight:1.0);
    }
    
    public String getName() {
        if (getRequest() instanceof CourseRequest) {
            Course course = null;
            CourseRequest courseRequest = (CourseRequest)getRequest();
            for (Enumeration e=courseRequest.getCourses().elements();e.hasMoreElements();) {
                Course c = (Course)e.nextElement();
                if (c.getOffering().getConfigs().contains(getConfig())) {
                    course = c; break;
                }
            }
            String ret = (course==null?getConfig()==null?"":getConfig().getName():course.getName());
            for (Iterator i=getAssignments().iterator();i.hasNext();) {
                Assignment assignment = (Assignment)i.next();
                ret+="\n  "+assignment.toString()+(i.hasNext()?",":"");
            }
            return ret;
        } else if (getRequest() instanceof FreeTimeRequest) {
            return "Free Time "+((FreeTimeRequest)getRequest()).getTime().getLongName();
        } else {
            String ret = "";
            for (Iterator i=getAssignments().iterator();i.hasNext();) {
                Assignment assignment = (Assignment)i.next();
                ret+=assignment.toString()+(i.hasNext()?",":"");
                if (i.hasNext()) ret+="\n  ";
            }
            return ret;
        }
    }
    
    public String toString() {
        String ret = getStudent()+" "+toDouble()+"/"+getRequest();
        if (getRequest() instanceof CourseRequest) {
            ret+=" ";
            for (Iterator i=getAssignments().iterator();i.hasNext();) {
                Assignment assignment = (Assignment)i.next();
                ret+=assignment+(i.hasNext()?", ":"");
            }
        }        
        return ret;
    }
    
    public boolean equals(Object o) {
        if (o==null || !(o instanceof Enrollment)) return false;
        Enrollment e = (Enrollment)o;
        if (!ToolBox.equals(getConfig(), e.getConfig())) return false;
        if (!ToolBox.equals(getRequest(),e.getRequest())) return false;
        if (!ToolBox.equals(getAssignments(),e.getAssignments())) return false;
        return true;
    }
    
    public Enrollment bestSwap(Enrollment enrl, Set problematicStudents) {
        Enrollment bestEnrollment = null;
        for (Iterator i=getRequest().values().iterator();i.hasNext();) {
            Enrollment enrollment = (Enrollment)i.next();
            if (enrollment.equals(this)) continue;
            if (!enrl.isConsistent(enrollment)) continue;
            if (getStudent().getModel().conflictValues(enrollment).isEmpty()) {
                if (bestEnrollment==null || bestEnrollment.toDouble()>enrollment.toDouble())
                    bestEnrollment = enrollment;
            }
        }
        if (bestEnrollment==null && problematicStudents!=null) {
            for (Iterator i=getRequest().values().iterator();i.hasNext();) {
                Enrollment enrollment = (Enrollment)i.next();
                if (enrollment.equals(this)) continue;
                if (!enrl.isConsistent(enrollment)) continue;
                Set conflicts = getStudent().getModel().conflictValues(enrollment);
                for (Iterator j=conflicts.iterator();j.hasNext();) {
                    Enrollment conflict = (Enrollment)j.next();
                    if (!enrl.getStudent().equals(conflict.getStudent()) && !getStudent().equals(conflict.getStudent()))
                        problematicStudents.add(conflict.getStudent());
                }
            }
        }
        return bestEnrollment;
    }
}