package org.cpsolver.studentsct.reservation;

import java.util.Collection;

import org.cpsolver.studentsct.model.Offering;

/**
 * Reservation override. A special instance of the {@link IndividualReservation}
 * for a particular student (or students) that allows for defining whether
 * it must be used, whether time conflicts are allowed, and whether it
 * is allowed to assign the student over the limit. This class is intended to be used
 * to model student overrides.
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
public class ReservationOverride extends IndividualReservation {
    /**
     * Reservation priority (lower than individual and group reservations)
     */
    public static final int DEFAULT_PRIORITY = 300;
    /**
     * Reservation override does not need to be used by default
     */
    public static final boolean DEFAULT_MUST_BE_USED = false;
    /**
     * Reservation override cannot be assigned over the limit by default.
     */
    public static final boolean DEFAULT_CAN_ASSIGN_OVER_LIMIT = false;
    /**
     * Overlaps are not allowed for group reservation overrides by default. 
     */
    public static final boolean DEFAULT_ALLOW_OVERLAP = false;
    
    /**
     * Constructor
     * @param id unique id
     * @param offering offering for which the reservation is
     * @param studentIds one or more students
     */
    public ReservationOverride(long id, Offering offering, Long... studentIds) {
        super(id, offering, DEFAULT_PRIORITY, DEFAULT_MUST_BE_USED, DEFAULT_CAN_ASSIGN_OVER_LIMIT, DEFAULT_ALLOW_OVERLAP, studentIds);
    }
    
    /**
     * Constructor
     * @param id unique id
     * @param offering offering for which the reservation is
     * @param studentIds one or more students
     */
    public ReservationOverride(long id, Offering offering, Collection<Long> studentIds) {
        super(id, offering, DEFAULT_PRIORITY, DEFAULT_MUST_BE_USED, DEFAULT_CAN_ASSIGN_OVER_LIMIT, DEFAULT_ALLOW_OVERLAP, studentIds);
    }
    
    @Override
    /**
     * Override reservation ignore expiration date when checking if they must be used.
     */
    public boolean mustBeUsed() {
        return mustBeUsedIgnoreExpiration();
    }
}