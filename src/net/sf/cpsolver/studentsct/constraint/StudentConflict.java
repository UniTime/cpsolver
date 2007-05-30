package net.sf.cpsolver.studentsct.constraint;

import java.util.Enumeration;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;

/**
 * This constraints ensures that a student is not enrolled into sections that are
 * overlapping in time. 
 * 
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
public class StudentConflict extends Constraint {
    
    /**
     * A given enrollment is conflicting when the student is enrolled into
     * another course / free time request that has an assignment that is 
     * overlapping with one or more assignments of the given section.
     * See {@link Enrollment#isOverlapping(Enrollment)} for more details.
     * All such overlapping enrollments are added into the provided set of conflicts.
     * @param value {@link Enrollment} that is being considered
     * @param conflicts resultant list of conflicting enrollments
     */
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
    
    /** Two enrollments are consistnet if they are not overlapping in time */
    public boolean isConsistent(Value value1, Value value2) {
        Enrollment e1 = (Enrollment)value1;
        Enrollment e2 = (Enrollment)value2;
        return !e1.isOverlapping(e2);
    }
    
    /**
     * A given enrollment is conflicting when the student is enrolled into
     * another course / free time request that has an assignment that is 
     * overlapping with one or more assignments of the given section.
     * See {@link Enrollment#isOverlapping(Enrollment)} for more details.
     * @param value {@link Enrollment} that is being considered
     * @return true, if the student is enrolled into another enrollment of a
     * different request that is overlapping in time with the given enrollment  
     */
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
