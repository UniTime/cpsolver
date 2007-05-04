package net.sf.cpsolver.studentsct.model;

import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.coursett.model.TimeLocation;

/**
 * Time and room assignment. This can be either {@link Section} or {@link FreeTimeRequest}.
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
public interface Assignment {
    /** Time assignment */
    public TimeLocation getTime();
    /** Room assignment 
     * @return list of {@link net.sf.cpsolver.coursett.model.RoomLocation}
     */
    public Vector getRooms();
    /** Number of rooms in which a section meets */
    public int getNrRooms();
    
    /** True, if this assignment is overlapping in time and space with the given assignment. */
    public boolean isOverlapping(Assignment assignment);
    /** True, if this assignment is overlapping in time and space with the given set of assignments. */
    public boolean isOverlapping(Set assignments);
    
    /** Enrollment with this assignmnet was assigned to a {@link Request}. */
    public void assigned(Enrollment enrollment);
    /** Enrollment with this assignmnet was unassigned from a {@link Request}. */
    public void unassigned(Enrollment enrollment);
    /** Return the list of assigned enrollments that contains this assignment.*/
    public Set getEnrollments();
}
