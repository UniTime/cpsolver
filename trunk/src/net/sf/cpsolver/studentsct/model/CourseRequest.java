package net.sf.cpsolver.studentsct.model;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

public class CourseRequest extends Request {
    private static DecimalFormat sDF = new DecimalFormat("0.000");
    private Vector iCourses = null;
    private Set iWaitlistedChoices = new HashSet();
    private Set iSelectedChoices = new HashSet();
    private boolean iWaitlist = false;
    private Double iCachedBound = null;
    
    public static double sAltValue = 0.5;
    
    public CourseRequest(long id, int priority, boolean alternative, Student student, Vector courses, boolean waitlist) {
        super(id, priority, alternative, student);
        iCourses = courses;
        iWaitlist = waitlist;
    }

    public Vector getCourses() {
        return iCourses;
    }
    
    public Enrollment createEnrollment(Set sections) {
        if (sections==null || sections.isEmpty()) return null;
        Config config = ((Section)sections.iterator().next()).getSubpart().getConfig();
        return new Enrollment(this, Math.pow(sAltValue, iCourses.indexOf(config.getOffering().getCourse(getStudent()))), config, sections);
    }
    
    public Vector computeEnrollments() {
        Vector ret = new Vector();
        int idx = 0;
        for (Enumeration e=iCourses.elements();e.hasMoreElements();idx++) {
            Course course = (Course)e.nextElement();
            for (Enumeration f=course.getOffering().getConfigs().elements();f.hasMoreElements();) {
                Config config = (Config)f.nextElement();
                computeEnrollments(ret, Math.pow(sAltValue, idx), 0, config, new HashSet(), 0, false, false, false, false, -1);
            }
        }
        return ret;
    }
    
    public Vector computeRandomEnrollments(int limitEachConfig) {
        Vector ret = new Vector();
        int idx = 0;
        for (Enumeration e=iCourses.elements();e.hasMoreElements();idx++) {
            Course course = (Course)e.nextElement();
            for (Enumeration f=course.getOffering().getConfigs().elements();f.hasMoreElements();) {
                Config config = (Config)f.nextElement();
                computeEnrollments(ret, Math.pow(sAltValue, idx), 0, config, new HashSet(), 0, false, false, false, true, (limitEachConfig<=0?limitEachConfig:ret.size()+limitEachConfig));
            }
        }
        return ret;
    }

    private void computeEnrollments(Collection enrollments, double value, double penalty, Config config, HashSet sections, int idx, boolean avaiableOnly, boolean skipSameTime, boolean selectedOnly, boolean random, int limit) {
        if (limit>0 && enrollments.size()>=limit) return;
        if (config.getSubparts().size()==idx) {
            enrollments.add(new Enrollment(this, value, config, new HashSet(sections)));
        } else {
            Subpart subpart = (Subpart)config.getSubparts().elementAt(idx);
            HashSet times = (skipSameTime?new HashSet():null);
            Vector sectionsThisSubpart = subpart.getSections();
            if (random) {
                sectionsThisSubpart = new Vector(subpart.getSections());
                Collections.shuffle(sectionsThisSubpart);
            } else if (skipSameTime) {
                sectionsThisSubpart = new Vector(subpart.getSections());
                Collections.sort(sectionsThisSubpart);
            }
            for (Enumeration e=sectionsThisSubpart.elements();e.hasMoreElements();) {
                Section section = (Section)e.nextElement();
                if (section.getParent()!=null && !sections.contains(section.getParent())) continue;
                if (section.isOverlapping(sections)) continue;
                if (avaiableOnly && section.getLimit()>=0 && section.getEnrollmentWeight(this)>=section.getLimit()) continue;
                if (selectedOnly && !isSelected(section)) continue;
                if (skipSameTime && section.getTime()!=null && !times.add(section.getTime()) && !isSelected(section) && !isWaitlisted(section)) continue;
                sections.add(section);
                computeEnrollments(enrollments, value, penalty+section.getPenalty(), config, sections, idx+1, avaiableOnly, skipSameTime, selectedOnly, random, limit);
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
                computeEnrollments(ret, Math.pow(sAltValue, idx), 0, config, new HashSet(), 0, true, false, false, false, -1);
            }
        }
        return ret;
    }
    
    public Vector getSelectedEnrollments(boolean availableOnly) {
        if (getSelectedChoices().isEmpty()) return null;
        Choice firstChoice = (Choice)getSelectedChoices().iterator().next();
        Vector enrollments = new Vector();
        for (Enumeration e=iCourses.elements();e.hasMoreElements();) {
            Course course = (Course)e.nextElement();
            if (!course.getOffering().equals(firstChoice.getOffering())) continue;
            for (Enumeration f=course.getOffering().getConfigs().elements();f.hasMoreElements();) {
                Config config = (Config)f.nextElement();
                computeEnrollments(enrollments, 1.0, 0, config, new HashSet(), 0, availableOnly, false, true, false, -1);
            }
        }
        return enrollments;
    }

    public TreeSet getAvaiableEnrollmentsSkipSameTime() {
        TreeSet avaiableEnrollmentsSkipSameTime = new TreeSet();
        if (getInitialAssignment()!=null)
            avaiableEnrollmentsSkipSameTime.add(getInitialAssignment());
        int idx = 0;
        for (Enumeration e=iCourses.elements();e.hasMoreElements();idx++) {
            Course course = (Course)e.nextElement();
            for (Enumeration f=course.getOffering().getConfigs().elements();f.hasMoreElements();) {
                Config config = (Config)f.nextElement();
                computeEnrollments(avaiableEnrollmentsSkipSameTime, Math.pow(sAltValue, idx), 0, config, new HashSet(), 0, true, true, false, false, -1);
            }
        }
        return avaiableEnrollmentsSkipSameTime;
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
        String ret = (isAlternative()?"A":"")+(1+getPriority()+(isAlternative()?-getStudent().nrRequests():0))+". "+(isWaitlist()?"(w) ":"");
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
        return getName()+(getWeight()!=1.0?" (W:"+sDF.format(getWeight())+")":"");
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
    
    public Subpart getSubpart(long subpartId) {
        for (Enumeration e=getCourses().elements();e.hasMoreElements();) {
            Course course = (Course)e.nextElement();
            for (Enumeration f=course.getOffering().getConfigs().elements();f.hasMoreElements();) {
                Config config = (Config)f.nextElement();
                for (Enumeration i=config.getSubparts().elements();i.hasMoreElements();) {
                    Subpart subpart = (Subpart)i.nextElement();
                    if (subpart.getId()==subpartId)
                        return subpart;
                }
            }
        }
        return null;
    }

    public Section getSection(long sectionId) {
        for (Enumeration e=getCourses().elements();e.hasMoreElements();) {
            Course course = (Course)e.nextElement();
            for (Enumeration f=course.getOffering().getConfigs().elements();f.hasMoreElements();) {
                Config config = (Config)f.nextElement();
                for (Enumeration i=config.getSubparts().elements();i.hasMoreElements();) {
                    Subpart subpart = (Subpart)i.nextElement();
                    for (Enumeration g=subpart.getSections().elements();g.hasMoreElements();) {
                        Section section = (Section)g.nextElement();
                        if (section.getId()==sectionId) return section;
                    }
                }
            }
        }
        return null;
    }
    
    public double getBound() {
        if (iCachedBound==null) {
            iCachedBound = new Double(
                    - Math.pow(Enrollment.sPriorityWeight,getPriority()) * 
                    (isAlternative()?Enrollment.sAlterativeWeight:1.0) *
                    Math.pow(Enrollment.sInitialWeight,(getInitialAssignment()==null?0:1)) *
                    Math.pow(Enrollment.sSelectedWeight,(iSelectedChoices.isEmpty()?0:1)) * 
                    Math.pow(Enrollment.sWaitlistedWeight,(iWaitlistedChoices.isEmpty()?0:1)) *
                    getWeight() * 
                    (getStudent().isDummy()?Student.sDummyStudentWeight:1.0)
             );
        }
        return iCachedBound.doubleValue();
    }
}
