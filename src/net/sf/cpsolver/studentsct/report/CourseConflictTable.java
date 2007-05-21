package net.sf.cpsolver.studentsct.report;

import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.constraint.SectionLimit;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;

public class CourseConflictTable {
    private static org.apache.log4j.Logger sLog = org.apache.log4j.Logger.getLogger(CourseConflictTable.class);
    private static DecimalFormat sDF = new DecimalFormat("0.000");
    
    private StudentSectioningModel iModel = null;
    
    public CourseConflictTable(StudentSectioningModel model) {
        iModel = model;
    }
    
    public StudentSectioningModel getModel() {
        return iModel;
    }

    private boolean areInHardConfict(Request r1, Request r2) {
        for (Enumeration e=r1.values().elements();e.hasMoreElements();) {
            Enrollment e1 = (Enrollment)e.nextElement();
            for (Enumeration f=r2.values().elements();f.hasMoreElements();) {
                Enrollment e2 = (Enrollment)f.nextElement();
                if (!e1.isOverlapping(e2)) return false;
            }
        }
        return true;
    }
    
    private HashSet explanations(Enrollment enrl, Enrollment conflict) {
        HashSet expl = new HashSet();
        for (Iterator i=enrl.getAssignments().iterator();i.hasNext();) {
            Section s1 = (Section)i.next();
            for (Iterator j=conflict.getAssignments().iterator();j.hasNext();) {
                Section s2 = (Section)j.next();
                if (s1.isOverlapping(s2))
                    expl.add(s1.getSubpart().getName()+" "+s1.getTime().getLongName()+" vs "+s2.getSubpart().getName()+" "+s2.getTime().getLongName());
            }
        }
        for (Iterator i=enrl.getAssignments().iterator();i.hasNext();) {
            Section s1 = (Section)i.next();
            if (conflict.getAssignments().contains(s1) && s1.getEnrollmentWeight(enrl.getRequest()) + SectionLimit.getWeight(enrl.getRequest())>s1.getLimit()) {
                expl.add(s1.getSubpart().getName()+" n/a");
            }
        }
        return expl;
    }
    
    public CSVFile createTable(boolean includeLastLikeStudents, boolean includeRealStudents) {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVFile.CSVField[] {
                new CSVFile.CSVField("UnasgnCrs"),
                new CSVFile.CSVField("ConflCrs"),
                new CSVFile.CSVField("NrStud"),
                new CSVFile.CSVField("NoAlt"),
                new CSVFile.CSVField("Reason")
        });
        Hashtable unassignedCourseTable = new Hashtable();
        for (Enumeration e=getModel().unassignedVariables().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (request.getStudent().isDummy() && !includeLastLikeStudents) continue;
            if (!request.getStudent().isDummy() && !includeRealStudents) continue;
            if (request instanceof CourseRequest) {
                CourseRequest courseRequest = (CourseRequest)request;
                if (courseRequest.getStudent().isComplete()) continue;
                
                Vector values = courseRequest.values();
                SectionLimit limitConstraint = new SectionLimit();
                Vector availableValues = new Vector(values.size());
                for (Enumeration f=values.elements();f.hasMoreElements();) {
                    Enrollment enrollment = (Enrollment)f.nextElement();
                    if (!limitConstraint.inConflict(enrollment))
                        availableValues.addElement(enrollment);
                }
                
                if (availableValues.isEmpty()) {
                    Course course = (Course)courseRequest.getCourses().firstElement();
                    Hashtable conflictCourseTable = (Hashtable)unassignedCourseTable.get(course);
                    if (conflictCourseTable==null) {
                        conflictCourseTable = new Hashtable();
                        unassignedCourseTable.put(course, conflictCourseTable);
                    }
                    Object[] weight = (Object[])conflictCourseTable.get(course);
                    double nrStud = (weight==null?0.0:((Double)weight[0]).doubleValue()) + request.getWeight();
                    boolean noAlt = (weight==null?true:((Boolean)weight[1]).booleanValue());
                    HashSet expl = (weight==null?new HashSet():(HashSet)weight[2]);
                    expl.add(course.getName()+" n/a");
                    conflictCourseTable.put(course, new Object[] {new Double(nrStud),new Boolean(noAlt),expl}); 
                }
                
                for (Enumeration f=availableValues.elements();f.hasMoreElements();) {
                    Enrollment enrollment = (Enrollment)f.nextElement();
                    Set conflicts = getModel().conflictValues(enrollment);
                    if (conflicts.isEmpty()) {
                        sLog.warn("Request "+courseRequest+" of student "+courseRequest.getStudent()+" not assigned, however, no conflicts were returned.");
                        courseRequest.assign(0, enrollment);
                        break;
                    }
                    Course course = null;
                    for (Enumeration g=courseRequest.getCourses().elements();g.hasMoreElements();) {
                        Course c = (Course)g.nextElement();
                        if (c.getOffering().equals(enrollment.getConfig().getOffering())) {
                            course = c; break;
                        }
                    }
                    if (course==null) {
                        sLog.warn("Course not found for request "+courseRequest+" of student "+courseRequest.getStudent()+".");
                        continue;
                    }
                    Hashtable conflictCourseTable = (Hashtable)unassignedCourseTable.get(course);
                    if (conflictCourseTable==null) {
                        conflictCourseTable = new Hashtable();
                        unassignedCourseTable.put(course, conflictCourseTable);
                    }
                    for (Iterator i=conflicts.iterator();i.hasNext();) {
                        Enrollment conflict = (Enrollment)i.next();
                        if (conflict.variable() instanceof CourseRequest) {
                            CourseRequest conflictCourseRequest = (CourseRequest)conflict.variable();
                            Course conflictCourse = null;
                            for (Enumeration g=conflictCourseRequest.getCourses().elements();g.hasMoreElements();) {
                                Course c = (Course)g.nextElement();
                                if (c.getOffering().equals(conflict.getConfig().getOffering())) {
                                    conflictCourse = c; break;
                                }
                            }
                            if (conflictCourse==null) {
                                sLog.warn("Course not found for request "+conflictCourseRequest+" of student "+conflictCourseRequest.getStudent()+".");
                                continue;
                            }
                            double weightThisConflict = request.getWeight() / availableValues.size() / conflicts.size();
                            Object[] weight = (Object[])conflictCourseTable.get(conflictCourse);
                            double nrStud = (weight==null?0.0:((Double)weight[0]).doubleValue()) + weightThisConflict;
                            boolean noAlt = (weight==null?areInHardConfict(request, conflict.getRequest()):((Boolean)weight[1]).booleanValue());
                            HashSet expl = (weight==null?new HashSet():(HashSet)weight[2]);
                            expl.addAll(explanations(enrollment, conflict));
                            conflictCourseTable.put(conflictCourse, new Object[] {new Double(nrStud),new Boolean(noAlt),expl}); 
                        }
                    }
                }
            }
        }
        for (Iterator i=unassignedCourseTable.entrySet().iterator();i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            Course unassignedCourse = (Course)entry.getKey();
            Hashtable conflictCourseTable = (Hashtable)entry.getValue();
            for (Iterator j=conflictCourseTable.entrySet().iterator();j.hasNext();) {
                Map.Entry entry2 = (Map.Entry)j.next();
                Course conflictCourse = (Course)entry2.getKey();
                Object[] weight = (Object[])entry2.getValue();
                HashSet expl = (HashSet)weight[2];
                String explStr = "";
                for (Iterator k=new TreeSet(expl).iterator();k.hasNext();)
                    explStr += k.next() + (k.hasNext()?"\n":"");
                csv.addLine(new CSVFile.CSVField[] {
                   new CSVFile.CSVField(unassignedCourse.getName()),
                   new CSVFile.CSVField(conflictCourse.getName()),
                   new CSVFile.CSVField(sDF.format((Double)weight[0])),
                   new CSVFile.CSVField(((Boolean)weight[1]).booleanValue()?"Y":"N"),
                   new CSVFile.CSVField(explStr)
                });
             }
        }
        return csv;
    }
}
