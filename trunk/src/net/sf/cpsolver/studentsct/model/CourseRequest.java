package net.sf.cpsolver.studentsct.model;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.constraint.SectionLimit;

/**
 * Representation of a request of a student for one or more course. A student requests one of the given courses, 
 * preferably the first one.
 * <br><br>
 * 
 * @version
 * StudentSct 1.1 (Student Sectioning)<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
public class CourseRequest extends Request {
    private static DecimalFormat sDF = new DecimalFormat("0.000");
    private Vector iCourses = null;
    private Set iWaitlistedChoices = new HashSet();
    private Set iSelectedChoices = new HashSet();
    private boolean iWaitlist = false;
    private Double iCachedBound = null, iCachedMinPenalty = null, iCachedMaxPenalty = null;
    /** Enrollment value: value * sAltValue ^ index, where index is zero for the first course, one for the second course etc. */
    public static double sAltValue = 0.5;
    public static boolean sSameTimePrecise = false;
    
    /** Constructor
     * @param id request unique id
     * @param priority request priority
     * @param alternative true if the request is alternative (alternative request can be assigned instead of a non-alternative course requests, if it is left unassigned)
     * @param student appropriate student
     * @param courses list of requested courses (in the correct order -- first is the requested course, second is the first alternative, etc.)
     * @param waitlist true if the student can be put on a waitlist (no alternative course request will be given instead)
     */
    public CourseRequest(long id, int priority, boolean alternative, Student student, Vector courses, boolean waitlist) {
        super(id, priority, alternative, student);
        iCourses = courses;
        iWaitlist = waitlist;
    }

    /** 
     * List of requested courses (in the correct order -- first is the requested course, second is the first alternative, etc.) 
     */
    public Vector getCourses() {
        return iCourses;
    }
    
    /** 
     * Create enrollment for the given list of sections. The list of sections needs to be correct, i.e., a section for each subpart of a 
     * configuration of one of the requested courses.
     */  
    public Enrollment createEnrollment(Set sections) {
        if (sections==null || sections.isEmpty()) return null;
        Config config = ((Section)sections.iterator().next()).getSubpart().getConfig();
        return new Enrollment(this, Math.pow(sAltValue, iCourses.indexOf(config.getOffering().getCourse(getStudent()))), config, sections);
    }
    
    /** 
     * Return all possible enrollments.
     */
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
    
    /**
     * Return a subset of all enrollments -- randomly select only up to limitEachConfig enrollments of each config. 
     */
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
    
    /** Return true if the both sets of sections contain sections of the same subparts, and each pair of sections of the same subpart is offered at the same time. */ 
    private boolean sameTimes(Set sections1, Set sections2) {
        for (Iterator i1=sections1.iterator();i1.hasNext();) {
            Section s1 = (Section)i1.next();
            Section s2 = null;
            for (Iterator i2=sections2.iterator();i2.hasNext();) {
                Section s = (Section)i2.next();
                if (s.getSubpart().equals(s1.getSubpart())) {
                    s2 = s; break;
                }
            }
            if (s2==null) return false;
            if (!ToolBox.equals(s1.getTime(), s2.getTime())) return false;
        }
        return true;
    }
    
    /**
     * Recursive computation of enrollments 
     * @param enrollments list of enrollments to be returned
     * @param value value of the selected sections
     * @param penalty penalty of the selected sections
     * @param config selected configurations
     * @param sections sections selected so far
     * @param idx index of the subparts (a section of 0..idx-1 subparts has been already selected)
     * @param avaiableOnly only use available sections
     * @param skipSameTime for each possible times, pick only one section
     * @param selectedOnly select only sections that are selected ({@link CourseRequest#isSelected(Section)} is true)
     * @param random pick sections in a random order (useful when limit is used)
     * @param limit when above zero, limit the number of selected enrollments to this limit
     */
    private void computeEnrollments(Collection enrollments, double value, double penalty, Config config, HashSet sections, int idx, boolean avaiableOnly, boolean skipSameTime, boolean selectedOnly, boolean random, int limit) {
        if (limit>0 && enrollments.size()>=limit) return;
        if (config.getSubparts().size()==idx) {
            if (skipSameTime && sSameTimePrecise) {
                boolean waitListedOrSelected = false;
                if (!getSelectedChoices().isEmpty() || !getWaitlistedChoices().isEmpty()) { 
                    for (Iterator i=sections.iterator();i.hasNext();) {
                        Section section = (Section)i.next();
                        if (isWaitlisted(section) || isSelected(section)) { waitListedOrSelected = true; break; }
                    }
                }
                if (!waitListedOrSelected) {
                    for (Iterator i=enrollments.iterator();i.hasNext();) {
                        Enrollment enrollment = (Enrollment)i.next();
                        if (sameTimes(enrollment.getAssignments(), sections)) return; 
                    }
                }
            }
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
            boolean hasChildren = !subpart.getChildren().isEmpty();
            for (Enumeration e=sectionsThisSubpart.elements();e.hasMoreElements();) {
                Section section = (Section)e.nextElement();
                if (section.getParent()!=null && !sections.contains(section.getParent())) continue;
                if (section.isOverlapping(sections)) continue;
                if (avaiableOnly && section.getLimit()>=0 && section.getEnrollmentWeight(this)+SectionLimit.getWeight(this)>section.getLimit()) continue;
                if (selectedOnly && !isSelected(section)) continue;
                if (skipSameTime && section.getTime()!=null && !hasChildren && !times.add(section.getTime()) && !isSelected(section) && !isWaitlisted(section)) continue;
                sections.add(section);
                computeEnrollments(enrollments, value, penalty+section.getPenalty(), config, sections, idx+1, avaiableOnly, skipSameTime, selectedOnly, random, limit);
                sections.remove(section);
            }
        }
    }
    
    /** Return all enrollments that are available */ 
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
    
    /** Return all enrollments that are selected ({@link CourseRequest#isSelected(Section)} is true)
     * @param availableOnly pick only available sections
     */
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

    /** Return all enrollments that are available, pick only the first section of the sections with the same time (of each subpart, {@link Section} comparator is used) */ 
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
    
    /** 
     * Return all possible enrollments.
     */
    public Vector getEnrollmentsSkipSameTime() {
        Vector ret = new Vector();
        int idx = 0;
        for (Enumeration e=iCourses.elements();e.hasMoreElements();idx++) {
            Course course = (Course)e.nextElement();
            for (Enumeration f=course.getOffering().getConfigs().elements();f.hasMoreElements();) {
                Config config = (Config)f.nextElement();
                computeEnrollments(ret, Math.pow(sAltValue, idx), 0, config, new HashSet(), 0, false, true, false, false, -1);
            }
        }
        return ret;
    }
    
    
    /** Wait-listed choices */
    public Set getWaitlistedChoices() {
        return iWaitlistedChoices;
    }
    
    /** Return true when the given section is wait-listed (i.e., its choice is among wait-listed choices) */
    public boolean isWaitlisted(Section section) {
        return iWaitlistedChoices.contains(section.getChoice());
    }
    
    /** Selected choices */
    public Set getSelectedChoices() {
        return iSelectedChoices;
    }
    
    /** Return true when the given section is selected (i.e., its choice is among selected choices) */
    public boolean isSelected(Section section) {
        return iSelectedChoices.contains(section.getChoice());
    }
    
    /** Request name: A for alternative, 1 + priority, (w) when waitlist, list of course names */  
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
    
    /** True if the student can be put on a waitlist (no alternative course request will be given instead) */
    public boolean isWaitlist() {
        return iWaitlist;
    }

    public String toString() {
        return getName()+(getWeight()!=1.0?" (W:"+sDF.format(getWeight())+")":"");
    }
    
    /** Return course of the requested courses with the given id */
    public Course getCourse(long courseId) {
        for (Enumeration e=getCourses().elements();e.hasMoreElements();) {
            Course course = (Course)e.nextElement();
            if (course.getId()==courseId) return course;
        }
        return null;
    }

    /** Return config of the requested courses with the given id */
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
    
    /** Return subpart of the requested courses with the given id */
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

    /** Return section of the requested courses with the given id */
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
    
    /** Minimal penalty (minimum of {@link Offering#getMinPenalty()} among requested courses) */
    public double getMinPenalty() {
        if (iCachedMinPenalty==null) {
            double min = Double.MAX_VALUE;
            for (Enumeration e=getCourses().elements();e.hasMoreElements();) {
                Course course = (Course)e.nextElement();
                min = Math.min(min, course.getOffering().getMinPenalty());
            }
            iCachedMinPenalty = new Double(min);
        }
        return iCachedMinPenalty.doubleValue();
    }
    
    /** Maximal penalty (maximum of {@link Offering#getMaxPenalty()} among requested courses) */
    public double getMaxPenalty() {
        if (iCachedMaxPenalty==null) {
            double max = Double.MIN_VALUE;
            for (Enumeration e=getCourses().elements();e.hasMoreElements();) {
                Course course = (Course)e.nextElement();
                max = Math.max(max, course.getOffering().getMaxPenalty());
            }
            iCachedMaxPenalty = new Double(max);
        }
        return iCachedMaxPenalty.doubleValue();
    }
    
    /** Clear cached min/max penalties and cached bound */
    public void clearCache() {
        iCachedMaxPenalty = null;
        iCachedMinPenalty = null;
        iCachedBound = null;
    }

    /** Estimated bound for this request -- it estimates the smallest value among all possible enrollments */
    public double getBound() {
        if (iCachedBound==null) {
            iCachedBound = new Double(
                    - Math.pow(Enrollment.sPriorityWeight,getPriority()) * 
                    (isAlternative()?Enrollment.sAlterativeWeight:1.0) *
                    Math.pow(Enrollment.sInitialWeight,(getInitialAssignment()==null?0:1)) *
                    Math.pow(Enrollment.sSelectedWeight,(iSelectedChoices.isEmpty()?0:1)) * 
                    Math.pow(Enrollment.sWaitlistedWeight,(iWaitlistedChoices.isEmpty()?0:1)) *
                    //Math.max(Enrollment.sMinWeight,getWeight()) * 
                    (getStudent().isDummy()?Student.sDummyStudentWeight:1.0) *
                    Enrollment.normalizePenalty(getMinPenalty())
             );
        }
        return iCachedBound.doubleValue();
    }
}
