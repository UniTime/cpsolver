package net.sf.cpsolver.studentsct.constraint;

import java.util.Enumeration;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Student;

public abstract class Reservation extends Constraint {
    public static int CAN_ENROLL_NO = 0;
    public static int CAN_ENROLL_YES = 1;
    public static int CAN_ENROLL_INSTEAD = 2;

    public abstract int canEnroll(Student student);
    public abstract boolean canEnrollInstead(Student student, Student insteadOfStudent);
    
    public abstract boolean isApplicable(Enrollment enrollment);
    
    public void computeConflicts(Value value, Set conflicts) {
        Enrollment enrollment = (Enrollment)value;
        
        if (!isApplicable(enrollment)) return; 
        
        int ce = canEnroll(enrollment.getStudent());
        
        if (ce==CAN_ENROLL_YES) return;
        
        if (ce==CAN_ENROLL_NO) {
            // Unable to bump out some other student
            conflicts.add(enrollment);
            return;
        }
        
        // Try other bump out some other student
        Vector conflictEnrollments = null;
        double conflictValue = 0;
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            CourseRequest request = (CourseRequest) e.nextElement();
            if (canEnrollInstead(enrollment.getStudent(),request.getStudent())) {
                if (conflictEnrollments==null || conflictValue>enrollment.toDouble()) {
                    if (conflictEnrollments==null)
                        conflictEnrollments = new Vector();
                    else
                        conflictEnrollments.clear();
                    conflictEnrollments.add(request.getAssignment());
                    conflictValue = request.getAssignment().toDouble();
                }
            }
        }
        if (conflictEnrollments!=null && !conflictEnrollments.isEmpty()) {
            conflicts.add(ToolBox.random(conflictEnrollments));
            return;
        }

        // Unable to bump out some other student
        conflicts.add(enrollment);
    }
    
    public boolean inConflict(Value value) {
        Enrollment enrollment = (Enrollment)value;
        
        if (!isApplicable(enrollment)) return false;

        return canEnroll(enrollment.getStudent())!=CAN_ENROLL_YES;
    }
    
}
