package net.sf.cpsolver.studentsct.constraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.ifs.model.GlobalConstraint;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;

/**
 * Section limit constraint. This global constraint ensures that a limit of each
 * section is not exceeded. This means that the total sum of weights of course
 * requests (see {@link Request#getWeight()}) enrolled into a section is below
 * the section's limit (see {@link Section#getLimit()}).
 * 
 * <br>
 * <br>
 * Sections with negative limit are considered unlimited, and therefore
 * completely ignored by this constraint.
 * 
 * <br>
 * <br>
 * Parameters:
 * <table border='1'>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>SectionLimit.PreferDummyStudents</td>
 * <td>{@link Boolean}</td>
 * <td>If true, requests of dummy (last-like) students are preferred to be
 * selected as conflicting.</td>
 * </tr>
 * </table>
 * <br>
 * <br>
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 */
public class SectionLimit extends GlobalConstraint<Request, Enrollment> {
    private static double sNominalWeight = 0.00001;
    private boolean iPreferDummyStudents = false;

    /**
     * Constructor
     * 
     * @param cfg
     *            solver configuration
     */
    public SectionLimit(DataProperties cfg) {
        super();
        iPreferDummyStudents = cfg.getPropertyBoolean("SectionLimit.PreferDummyStudents", false);
    }

    /**
     * Enrollment weight of a section if the given request is assigned. In order
     * to overcome rounding problems with last-like students ( e.g., 5 students
     * are projected to two sections of limit 2 -- each section can have up to 3
     * of these last-like students), the weight of the request with the highest
     * weight in the section is changed to a small nominal weight.
     * 
     * @param section
     *            a section that is of concern
     * @param request
     *            a request of a student to be assigned containing the given
     *            section
     * @return section's new weight
     */
    public static double getEnrollmentWeight(Section section, Request request) {
        return section.getEnrollmentWeight(request) + request.getWeight()
                - Math.max(section.getMaxEnrollmentWeight(), request.getWeight()) + sNominalWeight;
    }

    /**
     * A given enrollment is conflicting, if there is a section which limit
     * (computed by {@link SectionLimit#getEnrollmentWeight(Section, Request)})
     * exceeds the section limit. <br>
     * For each of such sections, one or more existing enrollments are
     * (randomly) selected as conflicting until the overall weight is under the
     * limit.
     * 
     * @param enrollment
     *            {@link Enrollment} that is being considered
     * @param conflicts
     *            all computed conflicting requests are added into this set
     */
    @Override
    public void computeConflicts(Enrollment enrollment, Set<Enrollment> conflicts) {
        // exclude free time requests
        if (!enrollment.isCourseRequest())
            return;

        // for each section
        for (Section section : enrollment.getSections()) {

            // unlimited section
            if (section.getLimit() < 0)
                continue;

            // new enrollment weight
            double enrlWeight = getEnrollmentWeight(section, enrollment.getRequest());

            // below limit -> ok
            if (enrlWeight <= section.getLimit())
                continue;

            // above limit -> compute adepts (current assignments that are not
            // yet conflicting)
            // exclude all conflicts as well
            List<Enrollment> dummyAdepts = new ArrayList<Enrollment>(section.getEnrollments().size());
            List<Enrollment> adepts = new ArrayList<Enrollment>(section.getEnrollments().size());
            for (Enrollment e : section.getEnrollments()) {
                if (e.getRequest().equals(enrollment.getRequest()))
                    continue;
                if (conflicts.contains(e))
                    enrlWeight -= e.getRequest().getWeight();
                else if (iPreferDummyStudents && e.getStudent().isDummy())
                    dummyAdepts.add(e);
                else
                    adepts.add(e);
            }

            // while above limit -> pick an adept and make it conflicting
            while (enrlWeight > section.getLimit()) {
                // pick adept (prefer dummy students), decrease enrollment
                // weight, make conflict
                if (iPreferDummyStudents && !dummyAdepts.isEmpty()) {
                    Enrollment conflict = ToolBox.random(dummyAdepts);
                    dummyAdepts.remove(conflict);
                    enrlWeight -= conflict.getRequest().getWeight();
                    conflicts.add(conflict);
                } else if (!adepts.isEmpty()) {
                    Enrollment conflict = ToolBox.random(adepts);
                    adepts.remove(conflict);
                    enrlWeight -= conflict.getRequest().getWeight();
                    conflicts.add(conflict);
                } else {
                    // no adepts -> enrollment cannot be assigned
                    conflicts.add(enrollment);
                    break;
                }
            }
        }
    }

    /**
     * A given enrollment is conflicting, if there is a section which
     * limit(computed by
     * {@link SectionLimit#getEnrollmentWeight(Section, Request)}) exceeds the
     * section limit.
     * 
     * @param enrollment
     *            {@link Enrollment} that is being considered
     * @return true, if there is a section which will exceed its limit when the
     *         given enrollment is assigned
     */
    @Override
    public boolean inConflict(Enrollment enrollment) {
        // exclude free time requests
        if (!enrollment.isCourseRequest())
            return false;

        // for each section
        for (Section section : enrollment.getSections()) {

            // unlimited section
            if (section.getLimit() < 0)
                continue;

            // new enrollment weight
            double enrlWeight = getEnrollmentWeight(section, enrollment.getRequest());

            // above limit -> conflict
            if (enrlWeight > section.getLimit())
                return true;
        }

        // no conflicting section -> no conflict
        return false;
    }

    @Override
    public String toString() {
        return "SectioningLimit";
    }
}
