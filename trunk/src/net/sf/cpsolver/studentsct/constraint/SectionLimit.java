package net.sf.cpsolver.studentsct.constraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.ifs.model.GlobalConstraint;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.reservation.Reservation;

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
        // check reservation can assign over the limit
        if (((StudentSectioningModel)getModel()).getReservationCanAssignOverTheLimit() &&
            enrollment.getReservation() != null && enrollment.getReservation().canAssignOverLimit())
            return;

        // exclude free time requests
        if (!enrollment.isCourseRequest())
            return;

        // for each section
        for (Section section : enrollment.getSections()) {
            
            // no reservation -- check the space in the unreserved space in the section
            if (!hasSectionReservation(enrollment, section)) {
                // section is fully reserved by section reservations
                if (section.getTotalUnreservedSpace() < enrollment.getRequest().getWeight()) {
                    conflicts.add(enrollment);
                    return;
                }
                
                double unreserved = section.getUnreservedSpace(enrollment.getRequest()); 
                
                if (unreserved < enrollment.getRequest().getWeight()) {
                    // no unreserved space available -> cannot be assigned
                    // try to unassign some other enrollments that also do not have reservation
                    
                    List<Enrollment> adepts = new ArrayList<Enrollment>(section.getEnrollments().size());
                    for (Enrollment e : section.getEnrollments()) {
                        if (e.getRequest().equals(enrollment.getRequest()))
                            continue;
                        if (hasSectionReservation(e, section))
                            continue;
                        if (conflicts.contains(e))
                            unreserved += e.getRequest().getWeight();
                        else
                            adepts.add(e);
                    }
                    
                    while (unreserved < enrollment.getRequest().getWeight()) {
                        if (adepts.isEmpty()) {
                            // no adepts -> enrollment cannot be assigned
                            conflicts.add(enrollment);
                            return;
                        }
                        
                        // pick adept (prefer dummy students), decrease unreserved space,
                        // make conflict
                        List<Enrollment> best = new ArrayList<Enrollment>();
                        boolean bestDummy = false;
                        double bestValue = 0;
                        for (Enrollment adept: adepts) {
                            boolean dummy = adept.getStudent().isDummy();
                            double value = adept.toDouble();
                            
                            if (iPreferDummyStudents && dummy != bestDummy) {
                                if (dummy) {
                                    best.clear();
                                    best.add(adept);
                                    bestDummy = dummy;
                                    bestValue = value;
                                }
                                continue;
                            }
                            
                            if (best.isEmpty() || value > bestValue) {
                                if (best.isEmpty()) best.clear();
                                best.add(adept);
                                bestDummy = dummy;
                                bestValue = value;
                            } else if (bestValue == value) {
                                best.add(adept);
                            }
                        }
                        
                        Enrollment conflict = ToolBox.random(best);
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
            double enrlWeight = getEnrollmentWeight(section, enrollment.getRequest());

            // below limit -> ok
            if (enrlWeight <= section.getLimit())
                continue;

            // above limit -> compute adepts (current assignments that are not
            // yet conflicting) exclude all conflicts as well
            List<Enrollment> adepts = new ArrayList<Enrollment>(section.getEnrollments().size());
            for (Enrollment e : section.getEnrollments()) {
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
                List<Enrollment> best = new ArrayList<Enrollment>();
                boolean bestDummy = false;
                double bestValue = 0;
                boolean bestRes = true;
                for (Enrollment adept: adepts) {
                    boolean dummy = adept.getStudent().isDummy();
                    double value = adept.toDouble();
                    boolean res = hasSectionReservation(adept, section);
                    
                    if (iPreferDummyStudents && dummy != bestDummy) {
                        if (dummy) {
                            best.clear();
                            best.add(adept);
                            bestDummy = dummy;
                            bestValue = value;
                            bestRes = res;
                        }
                        continue;
                    }
                    
                    if (bestRes != res) {
                        if (!res) {
                            best.clear();
                            best.add(adept);
                            bestDummy = dummy;
                            bestValue = value;
                            bestRes = res;
                        }
                        continue;
                    }

                    if (best.isEmpty() || value > bestValue) {
                        if (best.isEmpty()) best.clear();
                        best.add(adept);
                        bestDummy = dummy;
                        bestValue = value;
                        bestRes = res;
                    } else if (bestValue == value) {
                        best.add(adept);
                    }
                }
                
                Enrollment conflict = ToolBox.random(best);
                adepts.remove(conflict);
                enrlWeight -= conflict.getRequest().getWeight();
                conflicts.add(conflict);
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
        // check reservation can assign over the limit
        if (((StudentSectioningModel)getModel()).getReservationCanAssignOverTheLimit() &&
            enrollment.getReservation() != null && enrollment.getReservation().canAssignOverLimit())
            return false;

        // exclude free time requests
        if (!enrollment.isCourseRequest())
            return false;

        // for each section
        for (Section section : enrollment.getSections()) {

            // unlimited section
            if (section.getLimit() < 0)
                continue;
            
            // not have reservation -> check unreserved space
            if (!hasSectionReservation(enrollment, section) && (
                section.getTotalUnreservedSpace() < enrollment.getRequest().getWeight() ||
                section.getUnreservedSpace(enrollment.getRequest()) < enrollment.getRequest().getWeight()))
                return true;

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
        return "SectionLimit";
    }
}
