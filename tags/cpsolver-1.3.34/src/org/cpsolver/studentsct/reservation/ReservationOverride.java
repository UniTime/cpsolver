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
    private boolean iMustBeUsed = false;
    private boolean iAllowOverlap = false;
    private boolean iAllowOverLimit = false;
    
    /**
     * Constructor
     * @param id unique id
     * @param offering offering for which the reservation is
     * @param studentIds one or more students
     */
    public ReservationOverride(long id, Offering offering, Long... studentIds) {
        super(id, offering, studentIds);
    }
    
    /**
     * Constructor
     * @param id unique id
     * @param offering offering for which the reservation is
     * @param studentIds one or more students
     */
    public ReservationOverride(long id, Offering offering, Collection<Long> studentIds) {
        super(id, offering, studentIds);
    }

    /**
     * Set if the override must be used
     * @param mustBeUsed true if the override must be used (if not expired)
     */
    public void setMustBeUsed(boolean mustBeUsed) { iMustBeUsed = mustBeUsed; }

    /**
     * Return if the override must be used (unless it is expired)
     * @return true if must be used and not expired
     */
    @Override
    public boolean mustBeUsed() {
        return iMustBeUsed && !isExpired();
    }
    
    /**
     * Set if the override allows for time conflicts
     * @param allowOverlap true if time overlaps are allowed
     */
    public void setAllowOverlap(boolean allowOverlap) {
        iAllowOverlap = allowOverlap;
    }
    
    /**
     * Overlaps are allowed for individual reservations. 
     */
    @Override
    public boolean isAllowOverlap() {
        return iAllowOverlap;
    }

    /**
     * Set if the override allows for over the limit assignment
     * @param allowOverLimit true if the student can get into the course, configuration, or class over the limit
     */
    public void setCanAssignOverLimit(boolean allowOverLimit) {
        iAllowOverLimit = allowOverLimit;
    }
    
    /**
     * Individual reservations are the only reservations that can be assigned over the limit.
     */
    @Override
    public boolean canAssignOverLimit() {
        return iAllowOverLimit;
    }
    
    /**
     * Overrides comes just after individual and group reservations
     */
    @Override
    public int getPriority() {
        return 300;
    }
}
