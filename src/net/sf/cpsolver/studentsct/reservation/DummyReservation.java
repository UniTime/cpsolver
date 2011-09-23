package net.sf.cpsolver.studentsct.reservation;

import net.sf.cpsolver.studentsct.model.Offering;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * Dummy reservation. Use to fill remaining space for offerings that are marked as by reservation only.
 * 
 * <br>
 * <br>
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
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
     * Constructor
     * @param offering offering on which the reservation is set
     */
    public DummyReservation(Offering offering) {
        super(offering.getId(), offering);
    }

    /**
     * Dummy reservation is unlimited
     */
    @Override
    public double getReservationLimit() {
        return -1;
    }

    /**
     * Dummy reservation has low priority
     */
    @Override
    public int getPriority() {
        return 4;
    }

    /**
     * Dummy reservation is not applicable to any students
     */
    @Override
    public boolean isApplicable(Student student) {
        return false;
    }

    /**
     * Dummy reservation cannot go over the limit
     */
    @Override
    public boolean canAssignOverLimit() {
        return false;
    }

    
    /**
     * Dummy reservation do not need to be used
     */
    @Override
    public boolean mustBeUsed() {
        return false;
    }

}
