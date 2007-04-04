package net.sf.cpsolver.studentsct.model;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

public class Offering {
    private long iId = -1;
    private String iName = null;
    private Vector iConfigs = new Vector();
    private Vector iCourses = new Vector();
    
    public Offering(long id, String name) {
        iId = id; iName = name;
    }
    
    public long getId() {
        return iId;
    }
    
    public String getName() {
        return iName;
    }
    
    public Vector getConfigs() {
        return iConfigs;
    }

    public Vector getCourses() {
        return iCourses;
    }
    
    public Section getSection(long sectionId) {
        for (Enumeration e=getConfigs().elements();e.hasMoreElements();) {
            Config config = (Config)e.nextElement();
            for (Iterator f=config.getSubparts().iterator();f.hasNext();) {
                Subpart subpart = (Subpart)f.next();
                for (Enumeration g=subpart.getSections().elements();g.hasMoreElements();) {
                    Section section = (Section)g.nextElement();
                    if (section.getId()==sectionId)
                        return section;
                }
            }
        }
        return null;
    }
    
    public Course getCourse(Student student) {
        if (getCourses().size()==0)
            return null;
        if (getCourses().size()==1)
            return (Course)getCourses().firstElement();
        for (Enumeration e=student.getRequests().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (request instanceof CourseRequest) {
                for (Enumeration f=((CourseRequest)request).getCourses().elements();f.hasMoreElements();) {
                    Course course = (Course)f.nextElement();
                    if (getCourses().contains(course)) return course;
                }
            }
        }
        return (Course)getCourses().firstElement();
    }
    
    public HashSet getInstructionalTypes() {
        HashSet instructionalTypes = new HashSet();
        for (Enumeration e=getConfigs().elements();e.hasMoreElements();) {
            Config config = (Config)e.nextElement();
            for (Iterator f=config.getSubparts().iterator();f.hasNext();) {
                Subpart subpart = (Subpart)f.next();
                instructionalTypes.add(subpart.getInstructionalType());
            }
        }
        return instructionalTypes;
    }
    
    public HashSet getChoices(String instructionalType) {
        HashSet choices = new HashSet();
        for (Enumeration e=getConfigs().elements();e.hasMoreElements();) {
            Config config = (Config)e.nextElement();
            for (Iterator f=config.getSubparts().iterator();f.hasNext();) {
                Subpart subpart = (Subpart)f.next();
                if (!instructionalType.equals(subpart.getInstructionalType())) continue;
                choices.addAll(subpart.getChoices());
            }
        }
        return choices;
    }
    
    public HashSet getSubparts(String instructionalType) {
        HashSet subparts = new HashSet();
        for (Enumeration e=getConfigs().elements();e.hasMoreElements();) {
            Config config = (Config)e.nextElement();
            for (Iterator f=config.getSubparts().iterator();f.hasNext();) {
                Subpart subpart = (Subpart)f.next();
                if (instructionalType.equals(subpart.getInstructionalType())) 
                    subparts.add(subpart);
            }
        }
        return subparts;
    }
}
