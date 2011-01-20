package net.sf.cpsolver.studentsct.reservation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.studentsct.model.Offering;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * Individual reservation. A reservation for a particular student (or students).
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
public class IndividualReservation extends Reservation {
    private Set<Long> iStudentIds = new HashSet<Long>();
    
    /**
     * Constructor
     * @param id unique id
     * @param offering offering for which the reservation is
     * @param studentIds one or more students
     */
    public IndividualReservation(long id, Offering offering, Long... studentIds) {
        super(id, offering);
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
    public IndividualReservation(long id, Offering offering, Collection<Long> studentIds) {
        super(id, offering);
        iStudentIds.addAll(studentIds);
    }

    /**
     * Individual reservations are the only reservations that can be assigned over the limit.
     */
    @Override
    public boolean canAssignOverLimit() {
        return true;
    }

    /**
     * Individual reservations are of the top priority
     */
    @Override
    public int getPriority() {
        return 0;
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
     */
    public Set<Long> getStudentIds() {
        return iStudentIds;
    }

    /**
     * Reservation limit == number of students in the reservation
     */
    @Override
    public double getLimit() {
        return iStudentIds.size();
    }

}
