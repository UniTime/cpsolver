package net.sf.cpsolver.studentsct;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.EnumerableHashSet;
import net.sf.cpsolver.studentsct.constraint.SectionLimit;
import net.sf.cpsolver.studentsct.constraint.StudentConflict;
import net.sf.cpsolver.studentsct.extension.DistanceConflict;
import net.sf.cpsolver.studentsct.model.Config;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Offering;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;
import net.sf.cpsolver.studentsct.model.Subpart;

/**
 * Student sectioning model.
 * 
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
public class StudentSectioningModel extends Model {
    private Vector iStudents = new Vector();
    private Vector iOfferings = new Vector();
    private HashSet iCompleteStudents = new HashSet();
    private double iTotalValue = 0.0;
    private DataProperties iProperties;
    private DistanceConflict iDistanceConflict = null;
    
    /**
     * Constructor
     * @param properties configuration
     */
    public StudentSectioningModel(DataProperties properties) {
        super();
        iAssignedVariables = new EnumerableHashSet();
        iUnassignedVariables = new EnumerableHashSet();
        iPerturbVariables = new EnumerableHashSet();
        addGlobalConstraint(new SectionLimit(properties));
        iProperties = properties;
    }
    
    /**
     * Students
     */
    public Vector getStudents() {
        return iStudents;
    }
    
    /**
     * Students with complete schedules (see {@link Student#isComplete()})
     */
    public Set getCompleteStudents() {
        return iCompleteStudents;
    }
    
    /**
     * Add a student into the model
     */
    public void addStudent(Student student) {
        iStudents.addElement(student);
        StudentConflict conflict = new StudentConflict();
        for (Enumeration e=student.getRequests().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            conflict.addVariable(request);
            addVariable(request);
        }
        addConstraint(conflict);
        if (student.isComplete())
            iCompleteStudents.add(student);
    }
    
    /**
     * List of offerings
     */
    public Vector getOfferings() {
        return iOfferings;
    }

    /**
     * Add an offering into the model
     */
    public void addOffering(Offering offering) {
        iOfferings.add(offering);
    }
    
    /**
     * Number of students with complete schedule
     */
    public int nrComplete() {
        return getCompleteStudents().size();
    }
    
    /**
     * Model info
     */
    public Hashtable getInfo() {
        Hashtable info = super.getInfo();
        info.put("Students with complete schedule" , 
                sDoubleFormat.format(100.0*nrComplete()/getStudents().size())+"% ("+nrComplete()+"/"+getStudents().size()+")");
        if (getDistanceConflict()!=null)
            info.put("Student distance conflicts", sDoubleFormat.format(getDistanceConflict().getTotalNrConflicts()));
        return info;
    }
    
    /**
     * Overall solution value
     */
    public double getTotalValue() {
        return iTotalValue;
    }
    
    /**
     * Called after an enrollment was assigned to a request. The list of complete students 
     * and the overall solution value are updated.
     */
    public void afterAssigned(long iteration, Value value) {
        super.afterAssigned(iteration, value);
        Enrollment enrollment = (Enrollment)value;
        Student student = enrollment.getStudent();
        if (student.isComplete())
            iCompleteStudents.add(student);
        iTotalValue += value.toDouble();
    }
    
    /**
     * Called before an enrollment was unassigned from a request. The list of complete students 
     * and the overall solution value are updated.
     */
    public void afterUnassigned(long iteration, Value value) {
        super.afterUnassigned(iteration, value);
        Enrollment enrollment = (Enrollment)value;
        Student student = enrollment.getStudent();
        if (iCompleteStudents.contains(student) && !student.isComplete())
            iCompleteStudents.remove(student);
        iTotalValue -= value.toDouble();
    }
    
    /**
     * Configuration
     */
    public DataProperties getProperties() {
        return iProperties;
    }
    
    /**
     * Empty online student sectioning infos for all sections (see {@link Section#getSpaceExpected()} and {@link Section#getSpaceHeld()}). 
     */
    public void clearOnlineSectioningInfos() {
        for (Enumeration e=iOfferings.elements();e.hasMoreElements();) {
            Offering offering = (Offering)e.nextElement();
            for (Enumeration f=offering.getConfigs().elements();f.hasMoreElements();) {
                Config config = (Config)f.nextElement();
                for (Enumeration g=config.getSubparts().elements();g.hasMoreElements();) {
                    Subpart subpart = (Subpart)g.nextElement();
                    for (Enumeration h=subpart.getSections().elements();h.hasMoreElements();) {
                        Section section = (Section)h.nextElement();
                        section.setSpaceExpected(0);
                        section.setSpaceHeld(0);
                    }
                }
            }
        }
    }
    
    /**
     * Compute online student sectioning infos for all sections (see {@link Section#getSpaceExpected()} and {@link Section#getSpaceHeld()}). 
     */
    public void computeOnlineSectioningInfos() {
        clearOnlineSectioningInfos();
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            Student student = (Student)e.nextElement();
            if (!student.isDummy()) continue;
            for (Enumeration f=student.getRequests().elements();f.hasMoreElements();) {
                Request request = (Request)f.nextElement();
                if (!(request instanceof CourseRequest)) continue;
                CourseRequest courseRequest = (CourseRequest)request;
                Enrollment enrollment = (Enrollment)courseRequest.getAssignment();
                if (enrollment!=null) {
                    for (Iterator i=enrollment.getAssignments().iterator();i.hasNext();) {
                        Section section = (Section)i.next();
                        section.setSpaceHeld(courseRequest.getWeight()+section.getSpaceHeld());
                    }
                }
                Vector feasibleEnrollments = new Vector();
                for (Enumeration g=courseRequest.values().elements();g.hasMoreElements();) {
                    Enrollment enrl = (Enrollment)g.nextElement();
                    boolean overlaps = false;
                    for (Enumeration h=student.getRequests().elements();h.hasMoreElements();) {
                        CourseRequest otherCourseRequest = (CourseRequest)h.nextElement();
                        if (otherCourseRequest.equals(courseRequest)) continue;
                        Enrollment otherErollment = (Enrollment)otherCourseRequest.getAssignment();
                        if (otherErollment==null) continue;
                        if (enrl.isOverlapping(otherErollment)) {
                            overlaps = true; break;
                        }
                    }
                    if (!overlaps)
                        feasibleEnrollments.add(enrl);
                }
                double increment = courseRequest.getWeight() / feasibleEnrollments.size();
                for (Enumeration g=feasibleEnrollments.elements();g.hasMoreElements();) {
                    Enrollment feasibleEnrollment = (Enrollment)g.nextElement();
                    for (Iterator i=feasibleEnrollment.getAssignments().iterator();i.hasNext();) {
                        Section section = (Section)i.next();
                        section.setSpaceExpected(section.getSpaceExpected()+increment);
                    }
                }
            }
        }
    }
    
    /**
     * Sum of weights of all requests that are not assigned (see {@link Request#getWeight()}).
     */
    public double getUnassignedRequestWeight() {
        double weight = 0.0;
        for (Enumeration e=unassignedVariables().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            weight += request.getWeight();
        }
        return weight;
    }

    /**
     * Sum of weights of all requests (see {@link Request#getWeight()}).
     */
    public double getTotalRequestWeight() {
        double weight = 0.0;
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            weight += request.getWeight();
        }
        return weight;
    }
    
    /**
     * Set distance conflict extension 
     */
    public void setDistanceConflict(DistanceConflict dc) {
        iDistanceConflict = dc;
    }

    /**
     * Return distance conflict extension
     */
    public DistanceConflict getDistanceConflict() {
        return iDistanceConflict;
    }
    
    /**
     * Average priority of unassigned requests (see {@link Request#getPriority()})
     */
    public double avgUnassignPriority() {
        double totalPriority = 0.0;  
        for (Enumeration e=unassignedVariables().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (request.isAlternative()) continue;
            totalPriority += request.getPriority();
        }
        return 1.0 + totalPriority / unassignedVariables().size();
    }
    
    /**
     * Average number of requests per student (see {@link Student#getRequests()})
     */
    public double avgNrRequests() {
        double totalRequests = 0.0;  
        int totalStudents = 0;
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            Student student = (Student)e.nextElement();
            if (student.nrRequests()==0) continue;
            totalRequests += student.nrRequests();
            totalStudents ++;
        }
        return totalRequests / totalStudents;
    }
    
    /** Number of last like ({@link Student#isDummy()} equals true) students. */
    public int getNrLastLikeStudents() {
        int nrLastLikeStudents = 0;
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            Student student = (Student)e.nextElement();
            if (student.isDummy()) nrLastLikeStudents++;
        }
        return nrLastLikeStudents;
    }
    
    /** Number of real ({@link Student#isDummy()} equals false) students. */
    public int getNrRealStudents() {
        int nrRealStudents = 0;
        for (Enumeration e=getStudents().elements();e.hasMoreElements();) {
            Student student = (Student)e.nextElement();
            if (!student.isDummy()) nrRealStudents++;
        }
        return nrRealStudents;
    }

    /** Number of last like ({@link Student#isDummy()} equals true) students with a complete schedule ({@link Student#isComplete()} equals true). */
    public int getNrCompleteLastLikeStudents() {
        int nrLastLikeStudents = 0;
        for (Iterator i=getCompleteStudents().iterator();i.hasNext();) {
            Student student = (Student)i.next();
            if (student.isDummy()) nrLastLikeStudents++;
        }
        return nrLastLikeStudents;
    }
    
    /** Number of real ({@link Student#isDummy()} equals false) students with a complete schedule ({@link Student#isComplete()} equals true). */
    public int getNrCompleteRealStudents() {
        int nrRealStudents = 0;
        for (Iterator i=getCompleteStudents().iterator();i.hasNext();) {
            Student student = (Student)i.next();
            if (!student.isDummy()) nrRealStudents++;
        }
        return nrRealStudents;
    }

    /** Number of requests from last-like ({@link Student#isDummy()} equals true) students. */
    public int getNrLastLikeRequests() {
        int nrLastLikeRequests = 0;
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (request.getStudent().isDummy()) nrLastLikeRequests++;
        }
        return nrLastLikeRequests;
    }
    
    /** Number of requests from real ({@link Student#isDummy()} equals false) students. */
    public int getNrRealRequests() {
        int nrRealRequests = 0;
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (!request.getStudent().isDummy()) nrRealRequests++;
        }
        return nrRealRequests;
    }

    /** Number of requests from last-like ({@link Student#isDummy()} equals true) students that are assigned. */
    public int getNrAssignedLastLikeRequests() {
        int nrLastLikeRequests = 0;
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (request.getStudent().isDummy()) nrLastLikeRequests++;
        }
        return nrLastLikeRequests;
    }
    
    /** Number of requests from real ({@link Student#isDummy()} equals false) students that are assigned. */
    public int getNrAssignedRealRequests() {
        int nrRealRequests = 0;
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (!request.getStudent().isDummy()) nrRealRequests++;
        }
        return nrRealRequests;
    }
    
    /**
     * Model extended info. Some more information (that is more expensive to compute) is added to an ordinary {@link Model#getInfo()}.
     */
    public Hashtable getExtendedInfo() {
        Hashtable info = getInfo();
        int nrLastLikeStudents = getNrLastLikeStudents();
        if (nrLastLikeStudents!=0 && nrLastLikeStudents!=getStudents().size()) {
            int nrRealStudents = getStudents().size() - nrLastLikeStudents;
            int nrLastLikeCompleteStudents = getNrCompleteLastLikeStudents();
            int nrRealCompleteStudents = getNrCompleteRealStudents();
            info.put("Last-like students with complete schedule" ,
                    sDoubleFormat.format(100.0*nrLastLikeCompleteStudents/nrLastLikeStudents)+"% ("+nrLastLikeCompleteStudents+"/"+nrLastLikeStudents+")");
            info.put("Real students with complete schedule" ,
                    sDoubleFormat.format(100.0*nrRealCompleteStudents/nrRealStudents)+"% ("+nrRealCompleteStudents+"/"+nrRealStudents+")");
            int nrRealRequests = getNrRealRequests();
            int nrLastLikeRequests = variables().size() - nrRealRequests;
            int nrRealAssignedRequests = getNrAssignedRealRequests();
            int nrLastLikeAssignedRequests = assignedVariables().size() - nrRealAssignedRequests;
            info.put("Last-like assigned requests" ,
                    sDoubleFormat.format(100.0*nrLastLikeAssignedRequests/nrLastLikeRequests)+"% ("+nrLastLikeAssignedRequests+"/"+nrLastLikeRequests+")");
            info.put("Real assigned requests" ,
                    sDoubleFormat.format(100.0*nrRealAssignedRequests/nrRealRequests)+"% ("+nrRealAssignedRequests+"/"+nrRealRequests+")");
        }
        info.put("Average unassigned priority", sDoubleFormat.format(avgUnassignPriority()));
        info.put("Average number of requests", sDoubleFormat.format(avgNrRequests()));
        info.put("Unassigned request weight", sDoubleFormat.format(getUnassignedRequestWeight())+" / "+sDoubleFormat.format(getTotalRequestWeight()));
        return info;
    }

}
