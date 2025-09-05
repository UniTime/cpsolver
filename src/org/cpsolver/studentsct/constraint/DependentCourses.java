package org.cpsolver.studentsct.constraint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.studentsct.constraint.LinkedSections.EnrollmentAssignment;
import org.cpsolver.studentsct.heuristics.selection.BranchBoundSelection;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Student;

/**
 * Global constraint for dependent courses. If a student requests both a course and its
 * parent (identified by {@link Course#getParent()}), the student cannot get the dependent
 * course without also having the parent course assigned.
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.4 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2025 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          <a href="http://muller.unitime.org">http://muller.unitime.org</a><br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 3 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */

public class DependentCourses extends GlobalConstraint<Request, Enrollment> {

    @Override
    public void computeConflicts(Assignment<Request, Enrollment> assignment, Enrollment value, Set<Enrollment> conflicts) {
        if (value != null && value.getCourse() != null) {
            // check if assigned course has a parent
            Course parent = value.getCourse().getParent();
            if (parent != null) {
                // has parent -> check if student has the parent course assigned
                for (Request request: value.getStudent().getRequests()) {
                    if (request.hasCourse(parent)) { // this request has the parent course
                        if (request.equals(value.variable())) {
                            // same request >> problem
                            conflicts.add(value);
                            break;
                        }
                        Enrollment enrollment = assignment.getValue(request);
                        // not assigned or assigned to a different offering -> conflict
                        if (enrollment == null || enrollment.getCourse() == null || !parent.equals(enrollment.getCourse())) {
                            conflicts.add(value);
                            break;
                        }
                    }
                }
            }
            // check the previous enrollment of the given request
            Enrollment original = assignment.getValue(value.variable());
            if (original != null && original.getCourse() != null && original.getCourse().hasChildren() && !original.getCourse().equals(value.getCourse())) {
                // moving to another course, check that the original offering is not a parent to an assigned course
                for (Request r: original.getStudent().getRequests()) {
                    Enrollment e = assignment.getValue(r);
                    Course p = (e == null || e.getCourse() == null ? null : e.getCourse().getParent());
                    if (p != null && original.getCourse().equals(p))
                        conflicts.add(e);
                }
            }            
        }
        
        if (!conflicts.contains(value)) {
            List<Enrollment> additionalConflicts = null;
            // check the conflicts 
            for (Iterator<Enrollment> i = conflicts.iterator(); i.hasNext(); ) {
                Enrollment conf = i.next();
                if (conf != null && conf.getCourse() != null && conf.getCourse().hasChildren()) {
                    for (Request r: conf.getStudent().getRequests()) {
                        Enrollment e = assignment.getValue(r);
                        if (r.equals(value.getRequest())) e = value;
                        Course p = (e == null || e.getCourse() == null ? null : e.getCourse().getParent());
                        if (p != null && conf.getCourse().equals(p)) {
                            if (additionalConflicts == null) additionalConflicts = new ArrayList<Enrollment>();
                            additionalConflicts.add(e);
                        }
                    }
                }
            }
            if (additionalConflicts != null) conflicts.addAll(additionalConflicts);
        }
    }
    
    /**
     * Conflict check. There is a conflict when a student has an assigned course that has 
     * a parent course, the student also has the parent course requested,
     * but does not have the parent course assigned.
     */
    @Override
    public boolean inConflict(Assignment<Request, Enrollment> assignment, Enrollment value) {
        if (value == null || value.getCourse() == null) return false;
        
        // check if assigned course has a parent
        Course parent = value.getCourse().getParent();
        if (parent != null) {
            // has parent -> check if student has the parent course assigned
            for (Request request: value.getStudent().getRequests()) {
                if (request.hasCourse(parent)) { // this request has the parent course
                    if (request.equals(value.variable())) return true; // same request >> problem 
                    Enrollment enrollment = assignment.getValue(request);
                    // not assigned or assigned to a different offering -> conflict
                    if (enrollment == null || enrollment.getCourse() == null || !parent.equals(enrollment.getCourse())) {
                        return true;
                    }
                }
            }
        }
        
        // check the previous enrollment of the given request
        Enrollment original = assignment.getValue(value.variable());
        if (original != null && original.getCourse() != null && original.getCourse().hasChildren() && !original.getCourse().equals(value.getCourse())) {
            // moving to another course, check that the original offering is not a parent to an assigned course
            for (Request r: original.getStudent().getRequests()) {
                Enrollment e = assignment.getValue(r);
                Course p = (e == null || e.getCourse() == null ? null : e.getCourse().getParent());
                if (p != null && original.getCourse().equals(p))
                     return true;
            }
        }

        return false;
    }
    
    /**
     * A special check for the branch-and-bound routine (such as {@link BranchBoundSelection}) which
     * constructs a full schedule for a student.
     * There is a conflict when there is a dependent course assigned within the already constructed 
     * schedule (up to the given index) and the already constructed schedule also contains the parent 
     * course but it is not assigned.
     * @param student a student
     * @param assignment current assigned up to the given index
     * @param index index of the variable that is being assigned
     * @return true if there is no conflict
     */
    public boolean isPartialScheduleInConflict(Student student, EnrollmentAssignment assignment, int index) {
        /*
        // check all requests up to index
        for (int i = 0; i <= index; i++) {
            Request request = student.getRequests().get(i);
            Enrollment enrollment = assignment.getEnrollment(request, i);
            if (enrollment == null || enrollment.getCourse() == null) continue;
            Course parent = enrollment.getCourse().getParent();
            if (parent != null) {
                // has assigned a dependent offering >> check if there is a parent
                for (int j = 0; j <= index; j++) {
                    Request other = student.getRequests().get(j);
                    if (other.hasCourse(parent)) {
                        Enrollment otherEnrollment = assignment.getEnrollment(other, j);
                        // parent request is not assigned or assigned to a different offering > conflict
                        if (otherEnrollment == null || otherEnrollment.getCourse() == null ||
                            !parent.equals(otherEnrollment.getCourse())) return true;
                    }
                }
            }
        }
        */
        Request request = student.getRequests().get(index);
        Enrollment enrollment = assignment.getEnrollment(request, index);
        Course parent = (enrollment == null || enrollment.getCourse() == null ? null : enrollment.getCourse().getParent());
        if (parent != null) {
            // current-level enrollment has a parent > check if the parent is already assigned
            for (int j = 0; j <= index; j++) {
                Request other = student.getRequests().get(j);
                if (other.hasCourse(parent)) {
                    Enrollment otherEnrollment = assignment.getEnrollment(request, j);
                    // parent request is not assigned or assigned to a different offering > conflict
                    if (otherEnrollment == null || otherEnrollment.getCourse() == null ||
                        !parent.equals(otherEnrollment.getCourse())) {
                        return true;
                    }
                }
            }
        }
        if (request.hasChildren()) {
            for (int j = 0; j <= index; j++) {
                Request other = student.getRequests().get(j);
                Enrollment otherEnrollment = assignment.getEnrollment(other, j);
                Course otherParent = (otherEnrollment == null || otherEnrollment.getCourse() == null ? null : otherEnrollment.getCourse().getParent());
                // other is assigned and has a parent that is included in the current-level request > must be assigned
                if (otherParent != null && request.hasCourse(otherParent)) {
                    if (enrollment == null || !otherParent.equals(enrollment.getCourse())) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * A special check for the branch-and-bound routine (such as {@link BranchBoundSelection}) which
     * constructs a full schedule for a student.
     * The parent request cannot be left unassigned if it contains a parent course for any of the already
     * assigned dependent course.
     * @param student a student
     * @param assignment current assigned up to the given index
     * @param parentRequest parent request (that is being checked if it can be left unassigned)
     * @return true if the parent request can be left unassigned (it is not a parent course for an assigned dependent course)
     */
    public boolean canLeaveUnassigned(Student student, EnrollmentAssignment assignment, Request parentRequest) {
        if (!parentRequest.hasChildren()) return false;
        // request has children > check if assigned
        for (int i = 0; i < student.getRequests().size(); i++) {
            Request request = student.getRequests().get(i);
            if (parentRequest.equals(request)) break;
            Enrollment enrollment = assignment.getEnrollment(request, i);
            Course parent = (enrollment == null || enrollment.getCourse() == null ? null : enrollment.getCourse().getParent());
            // assigned to a dependent course >> check if the given request has the parent
            if (parent != null && parentRequest.hasCourse(parent)) {
                return false;
            }
        }
        return true;
    }
}
