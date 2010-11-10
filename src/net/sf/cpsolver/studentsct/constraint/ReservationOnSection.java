package net.sf.cpsolver.studentsct.constraint;

import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Section;

/**
 * Abstract single section reservation.
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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 */
public abstract class ReservationOnSection extends Reservation {
    private Section iSection = null;

    /**
     * Constructor
     * 
     * @param section
     *            section on which the reservation is set
     */
    public ReservationOnSection(Section section) {
        super();
        iSection = section;
    }

    /** Return section on which the reservation is set */
    public Section getSection() {
        return iSection;
    }

    /**
     * True, if the enrollment contains the section on which this reservation is
     * set. See {@link Reservation#isApplicable(Enrollment)} for details.
     */
    @Override
    public boolean isApplicable(Enrollment enrollment) {
        return enrollment.getAssignments().contains(iSection);
    }

    @Override
    public String toString() {
        return "Reservation on " + iSection.getLongName();
    }
}
