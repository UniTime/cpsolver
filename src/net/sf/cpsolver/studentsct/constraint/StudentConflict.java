package net.sf.cpsolver.studentsct.constraint;

import java.util.Enumeration;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;

public class StudentConflict extends Constraint {
    
    public void computeConflicts(Value value, Set conflicts) {
        //get enrollment
        Enrollment enrollment = (Enrollment)value;

        //for all assigned course requests -> if overlapping with this enrollment -> conflict 
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (request.equals(enrollment.getRequest())) continue;
            if (enrollment.isOverlapping((Enrollment)request.getAssignment()))
                conflicts.add(request.getAssignment());
        }
        
        //if this enrollment cannot be assigned (student already has a full schedule) -> unassignd a lowest priority request
        if (!enrollment.getStudent().canAssign(enrollment.getRequest())) {
            Enrollment lowestPriorityEnrollment = null;
            int lowestPriority = -1;
            for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
                Request request = (Request)e.nextElement();
                if (request.equals(enrollment.getRequest())) continue;
                if (lowestPriority<request.getPriority()) {
                    lowestPriority = request.getPriority();
                    lowestPriorityEnrollment = (Enrollment)request.getAssignment();
                }
            }
            if (lowestPriorityEnrollment!=null)
                conflicts.add(lowestPriorityEnrollment);
        }
    }
    
    public boolean isConsistent(Value value1, Value value2) {
        Enrollment e1 = (Enrollment)value1;
        Enrollment e2 = (Enrollment)value2;
        return !e1.isOverlapping(e2);
    }
    
    public boolean inConflict(Value value) {
        //get enrollment
        Enrollment enrollment = (Enrollment)value;

        //for all assigned course requests -> if overlapping with this enrollment -> conflict 
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (request.equals(enrollment.getRequest())) continue;
            if (enrollment.isOverlapping((Enrollment)request.getAssignment()))
                return true;
        }
        
        //if this enrollment cannot be assigned (student already has a full schedule) -> conflict
        if (!enrollment.getStudent().canAssign(enrollment.getRequest()))
            return true;
        
        //nothing above -> no conflict
        return false;
    }
    
    public String toString() {
        return "StudentConflicts";
    }
}
