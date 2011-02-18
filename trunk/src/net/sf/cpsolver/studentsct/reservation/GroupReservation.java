package net.sf.cpsolver.studentsct.reservation;

import java.util.Collection;

import net.sf.cpsolver.studentsct.model.Offering;

/**
 * Group reservation. This is basically a {@link IndividualReservation}, but
 * students cannot be assigned over the limit and the priority is lower than on
 * individual reservations. Also, a different limit than the number of students
 * in the group can be provided.
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
public class GroupReservation extends IndividualReservation {
    private double iLimit;

    /**
     * Constructor
     * @param id unique id
     * @param limit reservation limit (-1 for unlimited)
     * @param offering offering for which the reservation is
     * @param studentIds one or more students
     */
    public GroupReservation(long id, double limit, Offering offering, Long... studentIds) {
        super(id, offering, studentIds);
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
        super(id, offering, studentIds);
        iLimit = limit;
    }

    /**
     * Group reservations are of the second highest priority
     */
    @Override
    public int getPriority() {
        return 1;
    }

    /**
     * Group reservations can not be assigned over the limit.
     */
    @Override
    public boolean canAssignOverLimit() {
        return false;
    }

    /**
     * Reservation limit
     */
    @Override
    public double getLimit() {
        return iLimit;
    }

}
