package net.sf.cpsolver.studentsct.check;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.model.Assignment;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

public class OverlapCheck {
    private static org.apache.log4j.Logger sLog = org.apache.log4j.Logger.getLogger(OverlapCheck.class);
    private StudentSectioningModel iModel;
    
    public OverlapCheck(StudentSectioningModel model) {
        iModel = model;
    }
    
    public StudentSectioningModel getModel() {
        return iModel;
    }
    
    public boolean check() {
        sLog.info("Checking for overlaps...");
        boolean ret = true;
        for (Enumeration e=getModel().getStudents().elements();e.hasMoreElements();) {
            Student student = (Student)e.nextElement();
            Hashtable times = new Hashtable();
            for (Enumeration f=student.getRequests().elements();f.hasMoreElements();) {
                Request request = (Request)f.nextElement();
                Enrollment enrollment = (Enrollment)request.getAssignment();
                if (enrollment==null) continue;
                for (Iterator g=enrollment.getAssignments().iterator();g.hasNext();) {
                    Assignment assignment = (Assignment)g.next();
                    if (assignment.getTime()==null) continue;
                    for (Enumeration h=times.keys();h.hasMoreElements();) {
                        TimeLocation time = (TimeLocation)h.nextElement();
                        if (time.hasIntersection(assignment.getTime())) {
                            sLog.error("Student "+student+" assignment "+assignment+" overlaps with "+times.get(time));
                            ret = false;
                        }
                    }
                    times.put(assignment.getTime(),assignment);
                }
            }
        }
        return ret;
    }

}
