package org.cpsolver.studentsct.model;

import java.util.List;
import java.util.Set;

import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;


/**
 * Time and room assignment. This can be either {@link Section} or
 * {@link FreeTimeRequest}. <br>
 * <br>
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2014 Tomas Muller<br>
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
public interface SctAssignment {
    /** Time assignment 
     * @return time assignment
     **/
    public TimeLocation getTime();

    /**
     * Room assignment
     * 
     * @return list of {@link org.cpsolver.coursett.model.RoomLocation}
     */
    public List<RoomLocation> getRooms();

    /** Number of rooms in which a section meets 
     * @return number of rooms in which a section meets
     **/
    public int getNrRooms();

    /**
     * True, if this assignment is overlapping in time and space with the given
     * assignment.
     * @param assignment another assignment
     * @return true if there is an overlap
     */
    public boolean isOverlapping(SctAssignment assignment);

    /**
     * True, if this assignment is overlapping in time and space with the given
     * set of assignments.
     * @param assignments a set of assignments
     * @return true if one of them overlaps
     */
    public boolean isOverlapping(Set<? extends SctAssignment> assignments);

    /** Enrollment with this assignment was assigned to a {@link Request}. 
     * @param assignment current assignment
     * @param enrollment an enrollment that was just assigned
     **/
    public void assigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment);

    /** Enrollment with this assignment was unassigned from a {@link Request}. 
     * @param assignment current assignment
     * @param enrollment an enrollment that was just unassigned
     **/
    public void unassigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment);

    /** Return the list of assigned enrollments that contains this assignment. 
     * @param assignment current assignments
     * @return a list of assigned enrollments (of this request) that contains this assignment
     **/
    public Set<Enrollment> getEnrollments(Assignment<Request, Enrollment> assignment);
    
    /** Return true if overlaps are allowed, but the number of overlapping slots should be minimized. 
     * @return true, if overlaps are allowed 
     **/
    public boolean isAllowOverlap();
    
    /** Unique id 
     * @return section unique id
     **/
    public long getId();

    /** Compare assignments by unique ids. 
     * @param a another section assignment
     * @return comparison
     **/
    public int compareById(SctAssignment a);
}
