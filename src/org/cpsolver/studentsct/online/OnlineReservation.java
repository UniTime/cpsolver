package org.cpsolver.studentsct.online;

import org.cpsolver.studentsct.model.Offering;
import org.cpsolver.studentsct.model.Student;

/**
 * An online reservation. A simple class modeling any reservation of a student. This class is particularly useful when a model containing only the given
 * student is constructed (to provide him/her with a schedule or suggestions).
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2014 Tomas Muller<br>
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
 *          License along with this library; if not see <a href='http://www.gnu.org/licenses'>http://www.gnu.org/licenses</a>.
 * 
 */
public class OnlineReservation extends org.cpsolver.studentsct.reservation.Reservation {
    private int iType;
    private int iLimit;
    private boolean iApply;
    private boolean iOverride;
    
    /**
     * Constructor 
     * @param type reservation type
     * @param id reservation unique id
     * @param offering reservation offering
     * @param priority reservation priority
     * @param over true when the reservation allows the student to be assigned over the limit
     * @param limit reservation limit
     * @param apply true if the reservation applies to the given student
     * @param mustUse true if the reservation must be used
     * @param allowOverlap true if the reservation allows for time overlaps
     * @param expired true if the reservation is expired
     * @param override true if the reservation is reservation override
     */
    public OnlineReservation(int type, long id, Offering offering, int priority, boolean over, int limit, boolean apply, boolean mustUse, boolean allowOverlap, boolean expired, boolean override) {
            super(id, offering, priority, mustUse, over, allowOverlap);
            iType = type;
            iLimit = limit;
            iApply = apply;
            iOverride = override;
            setExpired(expired);
    }
    
    /**
     * Constructor 
     * @param type reservation type
     * @param id reservation unique id
     * @param offering reservation offering
     * @param priority reservation priority
     * @param over true when the reservation allows the student to be assigned over the limit
     * @param limit reservation limit
     * @param apply true if the reservation applies to the given student
     * @param mustUse true if the reservation must be used
     * @param allowOverlap true if the reservation allows for time overlaps
     * @param expired true if the reservation is expired
     */
    public OnlineReservation(int type, long id, Offering offering, int priority, boolean over, int limit, boolean apply, boolean mustUse, boolean allowOverlap, boolean expired) {
        this(type, id, offering, priority, over, limit, apply, mustUse, allowOverlap, expired, false);
    }
    
    public int getType() {
            return iType;
    }
    
    @Override
    public double getReservationLimit() {
            return iLimit;
    }

    @Override
    public boolean isApplicable(Student student) {
            return iApply;
    }
    
    public boolean isOverride() {
        return iOverride;
    }
    
    @Override
    public boolean mustBeUsed() {
        if (iOverride)
            return mustBeUsedIgnoreExpiration();
        return super.mustBeUsed();
    }
    
    @Override
    public double getLimitCap() {
        return getReservationLimit();
    }
}
