package net.sf.cpsolver.studentsct.constraint;

import java.util.Set;

import net.sf.cpsolver.ifs.model.GlobalConstraint;
import net.sf.cpsolver.studentsct.model.Config;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.reservation.Reservation;

/**
 * Required reservation constraint. This global constraint ensures that reservations
 * with {@link Reservation#mustBeUsed()} flags are used. That is, an enrollment
 * is conflicting when there is a reservation for the student that must be used,
 * but the given enrollment does not use it. 
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
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
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */
public class RequiredReservation extends GlobalConstraint<Request, Enrollment> {
    
    /**
     * A given enrollment is conflicting, if there is a reservation that
     * the student must use, but the given enrollment does not use it.
     * 
     * @param enrollment {@link Enrollment} that is being considered
     * @param conflicts all computed conflicting requests are added into this set
     */
    @Override
    public void computeConflicts(Enrollment enrollment, Set<Enrollment> conflicts) {
        if (inConflict(enrollment))
            conflicts.add(enrollment);
    }
    
    /**
     * A given enrollment is conflicting, if there is a reservation that
     * the student must use, but the given enrollment does not use it.
     * 
     * @param enrollment {@link Enrollment} that is being considered
     * @return true, if the enrollment does not follow a reservation that must be used 
     */
    @Override
    public boolean inConflict(Enrollment enrollment) {
        // enrollment's config
        Config config = enrollment.getConfig();

        // exclude free time requests
        if (config == null) return false;
        
        // no reservations
        if (!config.getOffering().hasReservations())
            return false;
        
        // enrollment's reservation
        Reservation reservation = enrollment.getReservation();
        
        // already has a reservation that must be used
        if (reservation != null && reservation.mustBeUsed()) return false;
        
        // if a reservation is required for the student, fail
        for (Reservation r: config.getOffering().getReservations())
            if (r.mustBeUsed() && r.isApplicable(enrollment.getStudent()))
                return true;
        
        return false;
    }
    
    @Override
    public String toString() {
        return "RequiredReservation";
    }
}
