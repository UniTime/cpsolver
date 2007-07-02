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

/**
 * Abstract reservation constraint. This constraint allow some section, courses, and other 
 * parts to be reserved to particular group of students. A reservation can be unlimited 
 * (any number of students of that particular group can attend a course, section, etc.) or 
 * with a limit (only given number of seats is reserved to the students of the particular
 * group). 
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
public abstract class Reservation extends Constraint {
    /** Student cannot be enrolled */
    public static int CAN_ENROLL_NO = 0;
    /** Student can be enrolled */
    public static int CAN_ENROLL_YES = 1;
    /** Student can be enrolled, however, some other student has to dropped out of the section, course, etc. */
    public static int CAN_ENROLL_INSTEAD = 2;

    /** 
     * Check, whether a student can be enrolled into the given section, course, etc.
     * @param student given student
     * @return {@link Reservation#CAN_ENROLL_YES}, {@link Reservation#CAN_ENROLL_NO}, or {@link Reservation#CAN_ENROLL_INSTEAD} 
     */ 
    public abstract int canEnroll(Student student);
    
    /**
     * Check whether the given student can be enrolled instead of another student
     * @param student student that is to be enrolled in the particular course, section, etc.
     * @param insteadOfStudent student, that is currently enrolled, which is going to be 
     *  bumped out of the course, section, etc. in order to enroll given studen
     * @return true, if the move is permitted 
     */
    public abstract boolean canEnrollInstead(Student student, Student insteadOfStudent);
    
    /** 
     * Check whether the reservation is applicable to the given enrollment. 
     * This means that the given enrollment contains the particular section, course, etc. 
     * @param enrollment enrollment of a student that is being considered
     * @return true, if the reservation applies to the enrollment
     */ 
    public abstract boolean isApplicable(Enrollment enrollment);
    
    /** 
     * Implementation of the constraint primitives.
     * See {@link Constraint#computeConflicts(Value, Set)} for more details.
     */
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
    
    /** 
     * Implementation of the constraint primitives.
     * See {@link Constraint#inConflict(Value)} for more details.
     */
    public boolean inConflict(Value value) {
        Enrollment enrollment = (Enrollment)value;
        
        if (!isApplicable(enrollment)) return false;

        return canEnroll(enrollment.getStudent())!=CAN_ENROLL_YES;
    }
    
}
