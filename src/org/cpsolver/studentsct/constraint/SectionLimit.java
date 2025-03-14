package org.cpsolver.studentsct.constraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.constraint.ConfigLimit.Adepts;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.reservation.Reservation;


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
 * This constraint also check section reservations.
 * 
 * <br>
 * <br>
 * Parameters:
 * <table border='1'><caption>Related Solver Parameters</caption>
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
public class SectionLimit extends GlobalConstraint<Request, Enrollment> {
    private static double sNominalWeight = 0.00001;
    private boolean iPreferDummyStudents = false;
    private boolean iPreferPriorityStudents = true;

    /**
     * Constructor
     * 
     * @param cfg
     *            solver configuration
     */
    public SectionLimit(DataProperties cfg) {
        super();
        iPreferDummyStudents = cfg.getPropertyBoolean("SectionLimit.PreferDummyStudents", false);
        iPreferPriorityStudents = cfg.getPropertyBoolean("Sectioning.PriorityStudentsFirstSelection.AllIn", true);
    }

    /**
     * Enrollment weight of a section if the given request is assigned. In order
     * to overcome rounding problems with last-like students ( e.g., 5 students
     * are projected to two sections of limit 2 -- each section can have up to 3
     * of these last-like students), the weight of the request with the highest
     * weight in the section is changed to a small nominal weight.
     * 
     * @param assignment current assignment
     * @param section
     *            a section that is of concern
     * @param request
     *            a request of a student to be assigned containing the given
     *            section
     * @return section's new weight
     */
    public static double getEnrollmentWeight(Assignment<Request, Enrollment> assignment, Section section, Request request) {
        return section.getEnrollmentWeight(assignment, request) + request.getWeight() - Math.max(section.getMaxEnrollmentWeight(assignment), request.getWeight()) + sNominalWeight;
    }
    
    /**
     * Remaining unreserved space in a section if the given request is assigned. In order
     * to overcome rounding problems with last-like students ( e.g., 5 students
     * are projected to two sections of limit 2 -- each section can have up to 3
     * of these last-like students), the weight of the request with the highest
     * weight in the section is changed to a small nominal weight.
     * 
     * @param assignment current assignment
     * @param section
     *            a section that is of concern
     * @param request
     *            a request of a student to be assigned containing the given
     *            section
     * @return section's new unreserved space
     */
    public static double getUnreservedSpace(Assignment<Request, Enrollment> assignment, Section section, Request request) {
        return section.getUnreservedSpace(assignment, request) - request.getWeight() + Math.max(section.getMaxEnrollmentWeight(assignment), request.getWeight()) - sNominalWeight;
    }

    
    /**
     * True if the enrollment has reservation for this section.
     * Everything else is checked in the {@link ReservationLimit} constraint.
     **/
    private boolean hasSectionReservation(Enrollment enrollment, Section section) {
        Reservation reservation = enrollment.getReservation();
        if (reservation == null) return false;
        Set<Section> sections = reservation.getSections(section.getSubpart());
        return sections != null && sections.contains(section);
    }

    /**
     * A given enrollment is conflicting, if there is a section which limit
     * (computed by {@link SectionLimit#getEnrollmentWeight(Assignment, Section, Request)})
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
    public void computeConflicts(Assignment<Request, Enrollment> assignment, Enrollment enrollment, Set<Enrollment> conflicts) {
        // check reservation can assign over the limit
        if (enrollment.getReservation() != null && enrollment.getReservation().canBatchAssignOverLimit())
            return;

        // exclude free time requests
        if (!enrollment.isCourseRequest())
            return;

        // for each section
        for (Section section : enrollment.getSections()) {

            // no reservation -- check the space in the unreserved space in the section
            if (enrollment.getConfig().getOffering().hasReservations() && !hasSectionReservation(enrollment, section)) {
                // section is fully reserved by section reservations
                if (section.getTotalUnreservedSpace() < enrollment.getRequest().getWeight()) {
                    conflicts.add(enrollment);
                    return;
                }
                
                double unreserved = getUnreservedSpace(assignment, section, enrollment.getRequest()); 
                
                if (unreserved < 0.0) {
                    // no unreserved space available -> cannot be assigned
                    // try to unassign some other enrollments that also do not have reservation
                    
                    List<Enrollment> adepts = new ArrayList<Enrollment>(section.getEnrollments(assignment).size());
                    for (Enrollment e : section.getEnrollments(assignment)) {
                        if (e.getRequest().equals(enrollment.getRequest()))
                            continue;
                        if (e.getReservation() != null && e.getReservation().canBatchAssignOverLimit())
                            continue;
                        if (hasSectionReservation(e, section))
                            continue;
                        if (conflicts.contains(e))
                            unreserved += e.getRequest().getWeight();
                        else
                            adepts.add(e);
                    }
                    
                    while (unreserved < 0.0) {
                        if (adepts.isEmpty()) {
                            // no adepts -> enrollment cannot be assigned
                            conflicts.add(enrollment);
                            return;
                        }
                        
                        // pick adept (prefer dummy students), decrease unreserved space,
                        // make conflict
                        Enrollment conflict = new Adepts(iPreferDummyStudents, iPreferPriorityStudents, adepts, assignment).get();
                        adepts.remove(conflict);
                        unreserved += conflict.getRequest().getWeight();
                        conflicts.add(conflict);
                    }
                }
            }

            // unlimited section
            if (section.getLimit() < 0)
                continue;

            // new enrollment weight
            double enrlWeight = getEnrollmentWeight(assignment, section, enrollment.getRequest());

            // below limit -> ok
            if (enrlWeight <= section.getLimit())
                continue;

            // above limit -> compute adepts (current assignments that are not
            // yet conflicting) exclude all conflicts as well
            List<Enrollment> adepts = new ArrayList<Enrollment>(section.getEnrollments(assignment).size());
            for (Enrollment e : section.getEnrollments(assignment)) {
                if (e.getRequest().equals(enrollment.getRequest()))
                    continue;
                if (conflicts.contains(e))
                    enrlWeight -= e.getRequest().getWeight();
                else
                    adepts.add(e);
            }

            // while above limit -> pick an adept and make it conflicting
            while (enrlWeight > section.getLimit()) {
                if (adepts.isEmpty()) {
                    // no adepts -> enrollment cannot be assigned
                    conflicts.add(enrollment);
                    return;
                }
                
                // pick adept (prefer dummy students & students w/o reservation), decrease enrollment
                // weight, make conflict
                Enrollment conflict = new Adepts(iPreferDummyStudents, iPreferPriorityStudents, adepts, assignment).get();
                adepts.remove(conflict);
                enrlWeight -= conflict.getRequest().getWeight();
                conflicts.add(conflict);
            }
        }
    }

    /**
     * A given enrollment is conflicting, if there is a section which
     * limit(computed by
     * {@link SectionLimit#getEnrollmentWeight(Assignment, Section, Request)}) exceeds the
     * section limit.
     * 
     * @param enrollment
     *            {@link Enrollment} that is being considered
     * @return true, if there is a section which will exceed its limit when the
     *         given enrollment is assigned
     */
    @Override
    public boolean inConflict(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        // check reservation can assign over the limit
        if (enrollment.getReservation() != null && enrollment.getReservation().canBatchAssignOverLimit())
            return false;

        // exclude free time requests
        if (!enrollment.isCourseRequest())
            return false;

        // for each section
        for (Section section : enrollment.getSections()) {

            // not have reservation -> check unreserved space
            if (enrollment.getConfig().getOffering().hasReservations() &&
                !hasSectionReservation(enrollment, section) && (
                section.getTotalUnreservedSpace() < enrollment.getRequest().getWeight() ||
                getUnreservedSpace(assignment, section, enrollment.getRequest()) < 0.0))
                return true;

            // unlimited section
            if (section.getLimit() < 0)
                continue;
            
            // new enrollment weight
            double enrlWeight = getEnrollmentWeight(assignment, section, enrollment.getRequest());

            // above limit -> conflict
            if (enrlWeight > section.getLimit())
                return true;
        }

        // no conflicting section -> no conflict
        return false;
    }

    @Override
    public String toString() {
        return "SectionLimit";
    }
}
