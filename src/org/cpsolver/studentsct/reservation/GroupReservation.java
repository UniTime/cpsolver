package org.cpsolver.studentsct.reservation;

import java.util.Collection;

import org.cpsolver.studentsct.model.Offering;


/**
 * Group reservation. This is basically a {@link IndividualReservation}, but
 * students cannot be assigned over the limit and the priority is lower than on
 * individual reservations. Also, a different limit than the number of students
 * in the group can be provided.
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
public class GroupReservation extends IndividualReservation {
    private double iLimit;
    
    /**
     * Group reservations are of the second highest priority
     */
    public static final int DEFAULT_PRIORITY = 200;
    /**
     * Individual or group reservation must be used (unless it is expired)
     */
    public static final boolean DEFAULT_MUST_BE_USED = true;
    /**
     * Group reservations cannot be assigned over the limit.
     */
    public static final boolean DEFAULT_CAN_ASSIGN_OVER_LIMIT = false;
    /**
     * Overlaps are not allowed for group reservations. 
     */
    public static final boolean DEFAULT_ALLOW_OVERLAP = false;

    /**
     * Constructor
     * @param id unique id
     * @param limit reservation limit (-1 for unlimited)
     * @param offering offering for which the reservation is
     * @param studentIds one or more students
     */
    public GroupReservation(long id, double limit, Offering offering, Long... studentIds) {
        super(id, offering, DEFAULT_PRIORITY, DEFAULT_MUST_BE_USED, DEFAULT_CAN_ASSIGN_OVER_LIMIT, DEFAULT_ALLOW_OVERLAP, studentIds);
        iLimit = limit;
    }
    
    /**
     * Constructor
     * @param id unique id
     * @param limit reservation limit (-1 for unlimited)
     * @param offering offering for which the reservation is
     * @param studentIds one or more students
     */
    public GroupReservation(long id, double limit, Offering offering, Collection<Long> studentIds) {
        super(id, offering, DEFAULT_PRIORITY, DEFAULT_MUST_BE_USED, DEFAULT_CAN_ASSIGN_OVER_LIMIT, DEFAULT_ALLOW_OVERLAP, studentIds);
        iLimit = limit;
    }
    
    /**
     * Constructor
     * @param id reservation unique id
     * @param limit reservation limit (-1 for unlimited)
     * @param offering instructional offering on which the reservation is set
     * @param priority reservation priority
     * @param mustBeUsed must this reservation be used
     * @param canAssignOverLimit can assign over class / configuration / course limit
     * @param allowOverlap does this reservation allow for overlaps
     * @param studentIds one or more students
     */
    protected GroupReservation(long id, double limit, Offering offering, int priority, boolean mustBeUsed, boolean canAssignOverLimit, boolean allowOverlap, Long... studentIds) {
        super(id, offering, priority, mustBeUsed, canAssignOverLimit, allowOverlap, studentIds);
        iLimit = limit;
    }
    
    /**
     * Constructor
     * @param id reservation unique id
     * @param limit reservation limit (-1 for unlimited)
     * @param offering instructional offering on which the reservation is set
     * @param priority reservation priority
     * @param mustBeUsed must this reservation be used
     * @param canAssignOverLimit can assign over class / configuration / course limit
     * @param allowOverlap does this reservation allow for overlaps
     * @param studentIds one or more students
     */
    protected GroupReservation(long id, double limit, Offering offering, int priority, boolean mustBeUsed, boolean canAssignOverLimit, boolean allowOverlap, Collection<Long> studentIds) {
        super(id, offering, priority, mustBeUsed, canAssignOverLimit, allowOverlap, studentIds);
        iLimit = limit;
    }

    /**
     * Reservation limit
     */
    @Override
    public double getReservationLimit() {
        return iLimit;
    }
    
    /**
     * Set reservation limit (-1 for unlimited)
     * @param limit reservation limit, -1 if unlimited
     */
    public void setReservationLimit(double limit) {
        iLimit = limit;
    }
}
