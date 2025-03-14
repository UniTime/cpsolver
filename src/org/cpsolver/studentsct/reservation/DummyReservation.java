package org.cpsolver.studentsct.reservation;

import org.cpsolver.studentsct.model.Offering;
import org.cpsolver.studentsct.model.Student;

/**
 * Dummy reservation. Use to fill remaining space for offerings that are marked as by reservation only.
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
public class DummyReservation extends Reservation {
    
    /**
     * Dummy reservation has low priority
     */
    public static final int DEFAULT_PRIORITY = 600;
    /**
     * Dummy reservation does not need to be used
     */
    public static final boolean DEFAULT_MUST_BE_USED = false;
    /**
     * Dummy reservations can not assign over the limit.
     */
    public static final boolean DEFAULT_CAN_ASSIGN_OVER_LIMIT = false;
    /**
     * Overlaps are not allowed for dummy reservations. 
     */
    public static final boolean DEFAULT_ALLOW_OVERLAP = false;
    
    
    /**
     * Constructor
     * @param offering offering on which the reservation is set
     */
    public DummyReservation(Offering offering) {
        super(offering.getId(), offering, DEFAULT_PRIORITY, DEFAULT_MUST_BE_USED, DEFAULT_CAN_ASSIGN_OVER_LIMIT, DEFAULT_ALLOW_OVERLAP);
    }

    /**
     * Dummy reservation is unlimited
     */
    @Override
    public double getReservationLimit() {
        return -1;
    }

    /**
     * Dummy reservation is not applicable to any students
     */
    @Override
    public boolean isApplicable(Student student) {
        return false;
    }

}
