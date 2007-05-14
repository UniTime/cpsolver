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
import net.sf.cpsolver.studentsct.constraint.SectionLimit;
import net.sf.cpsolver.studentsct.constraint.StudentConflict;
import net.sf.cpsolver.studentsct.model.Config;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Offering;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;
import net.sf.cpsolver.studentsct.model.Subpart;

public class StudentSectioningModel extends Model {
    private Vector iStudents = new Vector();
    private Vector iOfferings = new Vector();
    private HashSet iCompleteStudents = new HashSet();
    private double iTotalValue = 0.0;
    private DataProperties iProperties;
    
    public StudentSectioningModel(DataProperties properties) {
        super();
        addGlobalConstraint(new SectionLimit());
        iProperties = properties;
    }
    
    public Vector getStudents() {
        return iStudents;
    }
    
    public Set getCompleteStudents() {
        return iCompleteStudents;
    }
    
    public void addStudent(Student student) {
        iStudents.addElement(student);
        student.setModel(this);
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
    
    public Vector getOfferings() {
        return iOfferings;
    }
    
    public void addOffering(Offering offering) {
        iOfferings.add(offering);
    }
    
    public int nrComplete() {
        return getCompleteStudents().size();
        /*
        int nrComplete = 0;
        for (Enumeration e=iStudents.elements();e.hasMoreElements();) {
            Student student = (Student)e.nextElement();
            if (student.isComplete()) nrComplete++;
        }
        return nrComplete;
        */
    }
    
    public Hashtable getInfo() {
        Hashtable info = super.getInfo();
        info.put("Students with complete schedule" , 
                sDoubleFormat.format(100.0*nrComplete()/getStudents().size())+"% ("+nrComplete()+"/"+getStudents().size()+")");
        return info;
    }
    
    public double getTotalValue() {
        return iTotalValue;
        //return super.getTotalValue();
    }
    
    public void afterAssigned(long iteration, Value value) {
        super.afterAssigned(iteration, value);
        Enrollment enrollment = (Enrollment)value;
        Student student = enrollment.getStudent();
        if (student.isComplete())
            iCompleteStudents.add(student);
        iTotalValue += value.toDouble();
    }
    
    public void afterUnassigned(long iteration, Value value) {
        super.afterUnassigned(iteration, value);
        Enrollment enrollment = (Enrollment)value;
        Student student = enrollment.getStudent();
        if (iCompleteStudents.contains(student) && !student.isComplete())
            iCompleteStudents.remove(student);
        iTotalValue -= value.toDouble();
    }
    
    public DataProperties getProperties() {
        return iProperties;
    }
    
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
    
    public double getUnassignedRequestWeight() {
        double weight = 0.0;
        for (Enumeration e=unassignedVariables().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            weight += request.getWeight();
        }
        return weight;
    }

    public double getTotalRequestWeight() {
        double weight = 0.0;
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            weight += request.getWeight();
        }
        return weight;
    }
}
