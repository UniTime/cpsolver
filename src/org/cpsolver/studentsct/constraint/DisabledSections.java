package org.cpsolver.studentsct.constraint;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.reservation.Reservation;


/**
 * Disabled sections constraint. This global constraint ensures that no enrollment
 * containing a disabled sections (using {@link Section#isEnabled()}) is used, unless
 * there is a reservation allowing for the use of disabled sections
 * (using {@link Reservation#isAllowDisabled()}).
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
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */
public class DisabledSections extends GlobalConstraint<Request, Enrollment> {
    
    /**
     * A given enrollment is conflicting, if there is a section that
     * is disabled and there is not a matching reservation that would allow for that.
     * 
     * @param enrollment {@link Enrollment} that is being considered
     * @param conflicts all computed conflicting requests are added into this set
     */
    @Override
    public void computeConflicts(Assignment<Request, Enrollment> assignment, Enrollment enrollment, Set<Enrollment> conflicts) {
        if (inConflict(assignment, enrollment))
            conflicts.add(enrollment);
    }
    
    /**
     * A given enrollment is conflicting, if there is a section that
     * is disabled and there is not a matching reservation that would allow for that.
     * 
     * @param enrollment {@link Enrollment} that is being considered
     * @return true, if the enrollment does not follow a reservation that must be used 
     */
    @Override
    public boolean inConflict(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        // enrollment's config
        Config config = enrollment.getConfig();

        // exclude free time requests
        if (config == null) return false;
        
        // student allows for disabled sections >> no problem
        if (enrollment.getStudent().isAllowDisabled()) return false;
        
        boolean hasDisabledSection = false;
        // check all sections of the given enrollment
        for (Section section: enrollment.getSections())
            if (!section.isEnabled(enrollment.getStudent())) {
                hasDisabledSection = true;
                break;
            }
        
        // no disabled section >> no conflict
        if (!hasDisabledSection) return false;
        
        // no reservations >> conflict not allowed
        if (!config.getOffering().hasReservations())
            return true;
        
        // enrollment's reservation
        Reservation reservation = enrollment.getReservation();
        
        // already has a reservation that all for disabled sections
        if (reservation != null && reservation.isAllowDisabled()) return false;
        
        // if a there is some other reservation that allows for disabled sections >> also fine
        for (Reservation r: config.getOffering().getReservations())
            if (r.isAllowDisabled() && r.isApplicable(enrollment.getStudent()) && r.isIncluded(enrollment))
                return false;
        
        return true;
    }
    
    @Override
    public String toString() {
        return "DisabledSections";
    }
}
