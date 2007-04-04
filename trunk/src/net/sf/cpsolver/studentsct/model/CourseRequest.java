package net.sf.cpsolver.studentsct.model;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

public class CourseRequest extends Request {
    public Vector iCourses = null;
    public Set iWaitlistedChoices = new HashSet();
    public Set iSelectedChoices = new HashSet();
    public boolean iWaitlist = false;
    
    public static double sAltValue = 0.1;
    
    public CourseRequest(long id, int priority, boolean alternative, Student student, Vector courses, boolean waitlist) {
        super(id, priority, alternative, student);
        iCourses = courses;
        iWaitlist = waitlist;
    }

    public Vector getCourses() {
        return iCourses;
    }
    
    public Vector computeEnrollments() {
        Vector ret = new Vector();
        int idx = 0;
        for (Enumeration e=iCourses.elements();e.hasMoreElements();idx++) {
            Course course = (Course)e.nextElement();
            for (Enumeration f=course.getOffering().getConfigs().elements();f.hasMoreElements();) {
                Config config = (Config)f.nextElement();
                computeEnrollments(ret, Math.pow(sAltValue, idx), config, new HashSet(), 0, false, false);
            }
        }
        return ret;
    }
    
    private void computeEnrollments(Collection enrollments, double value, Config config, HashSet sections, int idx, boolean avaiableOnly, boolean skipSameTime) {
        if (config.getSubparts().size()==idx) {
            enrollments.add(new Enrollment(this, value, config, new HashSet(sections)));
        } else {
            Subpart subpart = (Subpart)config.getSubparts().toArray()[idx];
            HashSet times = (skipSameTime?new HashSet():null);
            for (Enumeration e=subpart.getSections().elements();e.hasMoreElements();) {
                Section section = (Section)e.nextElement();
                if (section.getParent()!=null && !sections.contains(section.getParent())) continue;
                if (section.isOverlapping(sections)) continue;
                if (avaiableOnly && section.getEnrollments().size()>=section.getLimit()) continue;
                if (skipSameTime && section.getTime()!=null && !times.add(section.getTime()) && !isSelected(section) && !isWaitlisted(section)) continue;
                sections.add(section);
                computeEnrollments(enrollments, value, config, sections, idx+1, avaiableOnly, skipSameTime);
                sections.remove(section);
            }
        }
    }
    
    public Vector getAvaiableEnrollments() {
        Vector ret = new Vector();
        int idx = 0;
        for (Enumeration e=iCourses.elements();e.hasMoreElements();idx++) {
            Course course = (Course)e.nextElement();
            for (Enumeration f=course.getOffering().getConfigs().elements();f.hasMoreElements();) {
                Config config = (Config)f.nextElement();
                computeEnrollments(ret, Math.pow(sAltValue, idx), config, new HashSet(), 0, true, false);
            }
        }
        return ret;
    }

    private TreeSet iAvaiableEnrollmentsSkipSameTime = null;
    public TreeSet getAvaiableEnrollmentsSkipSameTime() {
        if (iAvaiableEnrollmentsSkipSameTime!=null) return iAvaiableEnrollmentsSkipSameTime;
        iAvaiableEnrollmentsSkipSameTime = new TreeSet();
        int idx = 0;
        for (Enumeration e=iCourses.elements();e.hasMoreElements();idx++) {
            Course course = (Course)e.nextElement();
            for (Enumeration f=course.getOffering().getConfigs().elements();f.hasMoreElements();) {
                Config config = (Config)f.nextElement();
                computeEnrollments(iAvaiableEnrollmentsSkipSameTime, Math.pow(sAltValue, idx), config, new HashSet(), 0, true, true);
            }
        }
        return iAvaiableEnrollmentsSkipSameTime;
    }
    
    public Set getWaitlistedChoices() {
        return iWaitlistedChoices;
    }
    
    public boolean isWaitlisted(Section section) {
        return iWaitlistedChoices.contains(section.getChoice());
    }
    
    public Set getSelectedChoices() {
        return iSelectedChoices;
    }
    
    public boolean isSelected(Section section) {
        return iSelectedChoices.contains(section.getChoice());
    }
    
    public String getName() {
        String ret = (isAlternative()?"A":"")+(1+getPriority())+". "+(isWaitlist()?"(w) ":"");
        int idx = 0;
        for (Enumeration e=iCourses.elements();e.hasMoreElements();idx++) {
            Course course = (Course)e.nextElement();
            if (idx==0)
                ret+=course.getName();
            else
                ret+=", "+idx+". alt "+course.getName();
        }
        return ret;
    }
    
    public boolean isWaitlist() {
        return iWaitlist;
    }

    public String toString() {
        return getName();
    }
    
    public Course getCourse(long courseId) {
        for (Enumeration e=getCourses().elements();e.hasMoreElements();) {
            Course course = (Course)e.nextElement();
            if (course.getId()==courseId) return course;
        }
        return null;
    }

    public Config getConfig(long configId) {
        for (Enumeration e=getCourses().elements();e.hasMoreElements();) {
            Course course = (Course)e.nextElement();
            for (Enumeration f=course.getOffering().getConfigs().elements();f.hasMoreElements();) {
                Config config = (Config)f.nextElement();
                if (config.getId()==configId) return config;
            }
        }
        return null;
    }
    
    public double getBound() {
        return - Math.pow(Enrollment.sPriorityWeight,getPriority()) * 
            (isAlternative()?Enrollment.sAlterativeWeight:1.0) *
            Math.pow(Enrollment.sSelectedWeight,(iSelectedChoices.isEmpty()?0:1)) * 
            Math.pow(Enrollment.sWaitlistedWeight,(iWaitlistedChoices.isEmpty()?0:1));
    }
}
