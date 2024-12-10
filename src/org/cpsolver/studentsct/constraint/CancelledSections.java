package org.cpsolver.studentsct.constraint;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;


/**
 * Cancelled sections constraint. This global constraint ensures that no enrollment
 * containing a cancelled sections (using {@link Section#isCancelled()}) is used.
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
public class CancelledSections extends GlobalConstraint<Request, Enrollment> {
    
    /**
     * A given enrollment is conflicting, if there is a section that
     * is cancelled.
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
     * is cancelled.
     * 
     * @param enrollment {@link Enrollment} that is being considered
     * @return true, if the enrollment does not follow a reservation that must be used 
     */
    @Override
    public boolean inConflict(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        // exclude free time requests
        if (enrollment.getConfig() == null) return false;

        // check all sections of the given enrollment
        for (Section section: enrollment.getSections())
            if (section.isCancelled()) return true;
        
        return false;
    }
    
    @Override
    public String toString() {
        return "CancelledSections";
    }
}
