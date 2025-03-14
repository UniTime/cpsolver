package org.cpsolver.studentsct.reservation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.cpsolver.studentsct.model.Offering;
import org.cpsolver.studentsct.model.Student;


/**
 * Individual reservation. A reservation for a particular student (or students).
 * 
 * <br>
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
public class IndividualReservation extends Reservation {
    private Set<Long> iStudentIds = new HashSet<Long>();
    
    /**
     * Individual reservations are of the top priority
     */
    public static final int DEFAULT_PRIORITY = 100;
    /**
     * Individual or group reservation must be used (unless it is expired)
     */
    public static final boolean DEFAULT_MUST_BE_USED = true;
    /**
     * Individual reservations are the only reservations that can be assigned over the limit.
     */
    public static final boolean DEFAULT_CAN_ASSIGN_OVER_LIMIT = true;
    /**
     * Overlaps are allowed for individual reservations. 
     */
    public static final boolean DEFAULT_ALLOW_OVERLAP = true;
    
    /**
     * Constructor
     * @param id reservation unique id
     * @param offering instructional offering on which the reservation is set
     * @param priority reservation priority
     * @param mustBeUsed must this reservation be used
     * @param canAssignOverLimit can assign over class / configuration / course limit
     * @param allowOverlap does this reservation allow for overlaps
     * @param studentIds one or more students
     */
    protected IndividualReservation(long id, Offering offering, int priority, boolean mustBeUsed, boolean canAssignOverLimit, boolean allowOverlap, Long... studentIds) {
        super(id, offering, priority, mustBeUsed, canAssignOverLimit, allowOverlap);
        for (Long studentId: studentIds) {
            iStudentIds.add(studentId);
        }
    }

    /**
     * Constructor
     * @param id unique id
     * @param offering offering for which the reservation is
     * @param studentIds one or more students
     */
    public IndividualReservation(long id, Offering offering, Long... studentIds) {
        this(id, offering, DEFAULT_PRIORITY, DEFAULT_MUST_BE_USED, DEFAULT_CAN_ASSIGN_OVER_LIMIT, DEFAULT_ALLOW_OVERLAP, studentIds);
    }
    
    /**
     * Constructor
     * @param id reservation unique id
     * @param offering instructional offering on which the reservation is set
     * @param priority reservation priority
     * @param mustBeUsed must this reservation be used
     * @param canAssignOverLimit can assign over class / configuration / course limit
     * @param allowOverlap does this reservation allow for overlaps
     * @param studentIds one or more students
     */
    protected IndividualReservation(long id, Offering offering, int priority, boolean mustBeUsed, boolean canAssignOverLimit, boolean allowOverlap, Collection<Long> studentIds) {
        super(id, offering, priority, mustBeUsed, canAssignOverLimit, allowOverlap);
        iStudentIds.addAll(studentIds);
    }


    /**
     * Constructor
     * @param id unique id
     * @param offering offering for which the reservation is
     * @param studentIds one or more students
     */
    public IndividualReservation(long id, Offering offering, Collection<Long> studentIds) {
        this(id, offering, DEFAULT_PRIORITY, DEFAULT_MUST_BE_USED, DEFAULT_CAN_ASSIGN_OVER_LIMIT, DEFAULT_ALLOW_OVERLAP, studentIds);
    }

    /**
     * Reservation is applicable for all students in the reservation
     */
    @Override
    public boolean isApplicable(Student student) {
        return iStudentIds.contains(student.getId());
    }
    
    /**
     * Students in the reservation
     * @return set of student ids associated with this reservation
     */
    public Set<Long> getStudentIds() {
        return iStudentIds;
    }

    /**
     * Reservation limit == number of students in the reservation
     */
    @Override
    public double getReservationLimit() {
        return iStudentIds.size();
    }
    
}
