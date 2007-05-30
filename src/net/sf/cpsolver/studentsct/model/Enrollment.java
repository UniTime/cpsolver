package net.sf.cpsolver.studentsct.model;

import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.extension.DistanceConflict;

/**
 * Representation of an enrollment of a student into a course. A student needs to 
 * be enrolled in a section of each subpart of a selected configuration. When 
 * parent-child relation is defined among sections, if a student is enrolled
 * in a section that has a parent section defined, he/she has be enrolled in 
 * the parent section as well. Also, the selected sections cannot overlap in time.
 * <br><br>
 * 
 * @version
 * StudentSct 1.1 (Student Sectioning)<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@ktiml.mff.cuni.cz">muller@ktiml.mff.cuni.cz</a><br>
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

public class Enrollment extends Value {
    private static DecimalFormat sDF = new DecimalFormat("0.000");
    private Request iRequest = null;
    private Config iConfig = null;
    private Set iAssignments = null;
    private Double iCachedPenalty = null;
    private Double iCachedDoubleValue = null;

    public static double sPriorityWeight = 0.90;
    public static double sAlterativeWeight = 1.0;
    public static double sInitialWeight = 1.2;
    public static double sSelectedWeight = 1.1;
    public static double sWaitlistedWeight = 1.01;
    public static double sMinWeight = 0.0001;
    public static double sNormPenalty = 5.0;
    public static double sDistConfWeight = 0.95;

    /** Constructor
     * @param request course / free time request
     * @param value value (1.0 for primary course, 0.5 for the first alternative, etc.)
     * @param config selected configuration
     * @param assignments valid list of sections
     */
    public Enrollment(Request request, double value, Config config, Set assignments) {
        super(request);
        iRequest = request;
        iConfig = config;
        iAssignments = assignments;
        iValue = value;
    }
    
    /** Student */
    public Student getStudent() {
        return iRequest.getStudent();
    }
    
    /** Request */
    public Request getRequest() {
        return iRequest;
    }
    
    /** True if the request is course request */
    public boolean isCourseRequest() {
        return iConfig!=null;
    }
    
    /** Offering of the course request */
    public Offering getOffering() {
        return (iConfig==null?null:iConfig.getOffering());
    }
    
    /** Config of the course request */
    public Config getConfig() {
        return iConfig;
    }
    
    /** List of assignments (selected sections) */
    public Set getAssignments() {
        return iAssignments;
    }
    
    /** True when this enrollment is overlapping with the given enrollment */
    public boolean isOverlapping(Enrollment enrl) {
        for (Iterator i=enrl.getAssignments().iterator();i.hasNext();) {
            Assignment assignment = (Assignment)i.next();
            if (assignment.isOverlapping(getAssignments())) return true;
        }
        return false;
    }
    
    /** Percent of sections that are wait-listed */
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

    /** Percent of sections that are selected */
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
    
    /** Percent of sections that are initial */
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

    /** True if all the sections are wait-listed */
    public boolean isWaitlisted() {
        if (!isCourseRequest()) return false;
        CourseRequest courseRequest = (CourseRequest)getRequest();
        for (Iterator i=getAssignments().iterator();i.hasNext();) {
            Section section = (Section)i.next();
            if (!courseRequest.isWaitlisted(section)) return false;
        }
        return true;
    }
    
    /** True if all the sections are selected */
    public boolean isSelected() {
        if (!isCourseRequest()) return false;
        CourseRequest courseRequest = (CourseRequest)getRequest();
        for (Iterator i=getAssignments().iterator();i.hasNext();) {
            Section section = (Section)i.next();
            if (!courseRequest.isSelected(section)) return false;
        }
        return true;
    }
    
    /** Enrollment penalty -- sum of section penalties (see {@link Section#getPenalty()}) */
    public double getPenalty() {
        if (iCachedPenalty==null) {
            double penalty = 0.0;
            if (isCourseRequest()) {
                for (Iterator i=getAssignments().iterator();i.hasNext();) {
                    Section section = (Section)i.next();
                    penalty += section.getPenalty();
                }
            }
            iCachedPenalty = new Double(penalty/getAssignments().size());
        }
        return iCachedPenalty.doubleValue();
    }
    
    /** Normalized enrollment penalty -- to be used in {@link Enrollment#toDouble()} */
    public static double normalizePenalty(double penalty) {
        return sNormPenalty/(sNormPenalty+penalty);
    }

    /** Enrollment value */
    public double toDouble() {
        return toDouble(nrDistanceConflicts());
    }

    /** Enrollment value */
    public double toDouble(double nrDistanceConflicts) {
        if (iCachedDoubleValue==null) {
            iCachedDoubleValue = new Double(
                    -iValue * 
                    Math.pow(sPriorityWeight,getRequest().getPriority()) * 
                    (getRequest().isAlternative()?sAlterativeWeight:1.0) *
                    Math.pow(sInitialWeight,percentInitial()) *
                    Math.pow(sSelectedWeight,percentSelected()) * 
                    Math.pow(sWaitlistedWeight,percentWaitlisted()) *
                    //Math.max(sMinWeight,getRequest().getWeight()) * 
                    (getStudent().isDummy()?Student.sDummyStudentWeight:1.0) *
                    normalizePenalty(getPenalty())
            );
        }
        return iCachedDoubleValue.doubleValue() * 
            Math.pow(sDistConfWeight,nrDistanceConflicts);
    }
    
    /** Enrollment name */
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
                Section assignment = (Section)i.next();
                ret+="\n  "+assignment.getLongName()+(i.hasNext()?",":"");
            }
            return ret;
        } else if (getRequest() instanceof FreeTimeRequest) {
            return "Free Time "+((FreeTimeRequest)getRequest()).getTime().getLongName();
        } else {
            String ret = "";
            for (Iterator i=getAssignments().iterator();i.hasNext();) {
                Assignment assignment = (Section)i.next();
                ret+=assignment.toString()+(i.hasNext()?",":"");
                if (i.hasNext()) ret+="\n  ";
            }
            return ret;
        }
    }
    
    public String toString() {
        String ret = sDF.format(toDouble())+"/"+sDF.format(getRequest().getBound())+(getPenalty()==0.0?"":"/"+sDF.format(getPenalty()));
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
    
    /** Number of distance conflicts, in which this enrollment is involved. */
    public double nrDistanceConflicts() {
        if (!isCourseRequest()) return 0;
        if (getRequest().getModel() instanceof StudentSectioningModel) {
            DistanceConflict dc = ((StudentSectioningModel)getRequest().getModel()).getDistanceConflict();
            if (dc==null) return 0;
            return dc.nrAllConflicts(this);
        } else return 0;
    }
}