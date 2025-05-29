package org.cpsolver.studentsct.constraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.constraint.ConfigLimit.Adepts;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.reservation.Reservation;


/**
 * Reservation limit constraint. This global constraint ensures that a limit of each
 * reservation is not exceeded. This means that the total sum of weights of course
 * requests (see {@link Request#getWeight()}) enrolled into a reservation is below
 * the reservation's limit (see {@link Reservation#getLimit()}). It also ensures that
 * the desired space is reserved in the enrollment's offering and configuration. 
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
 * <td>ReservationLimit.PreferDummyStudents</td>
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
public class ReservationLimit extends GlobalConstraint<Request, Enrollment> {
    private static double sNominalWeight = 0.00001;
    private boolean iPreferDummyStudents = false;
    private boolean iPreferPriorityStudents = true;

    /**
     * Constructor
     * 
     * @param cfg
     *            solver configuration
     */
    public ReservationLimit(DataProperties cfg) {
        super();
        iPreferDummyStudents = cfg.getPropertyBoolean("ReservationLimit.PreferDummyStudents", false);
        iPreferPriorityStudents = cfg.getPropertyBoolean("Sectioning.PriorityStudentsFirstSelection.AllIn", true);
    }

    
    /**
     * Remaining unreserved space in a config if the given request is assigned. In order
     * to overcome rounding problems with last-like students ( e.g., 5 students
     * are projected to two sections of limit 2 -- each section can have up to 3
     * of these last-like students), the weight of the request with the highest
     * weight in the section is changed to a small nominal weight.
     * 
     * @param assignment current assignment
     * @param config
     *            a config that is of concern
     * @param request
     *            a request of a student to be assigned containing the given
     *            section
     * @param hasReservation
     *            true if the enrollment in question has a reservation (only not matching the given configuration) 
     * @return config's new unreserved space
     */
    public static double getUnreservedSpace(Assignment<Request, Enrollment> assignment, Config config, Request request, boolean hasReservation) {
        if (hasReservation) // only check the config's unreserved space
            return config.getUnreservedSpace(assignment, request) - request.getWeight() + Math.max(config.getMaxEnrollmentWeight(assignment), request.getWeight()) - sNominalWeight;
        else // no reservation -- also check offering's unreserved space
            return Math.min(config.getUnreservedSpace(assignment, request), config.getOffering().getUnreservedSpace(assignment, request))
                    - request.getWeight() + Math.max(config.getMaxEnrollmentWeight(assignment), request.getWeight()) - sNominalWeight;
    }


    /**
     * A given enrollment is conflicting, if the reservation's remaining available space
     * (computed by {@link Reservation#getReservedAvailableSpace(Assignment, Request)})
     * is below the requests weight {@link Request#getWeight()}. <br>
     * If the limit is breached, one or more existing enrollments are
     * selected as conflicting until there is enough space in the reservation.
     * Similarly, if the enrollment does not have the reservation, it is checked
     * that there is enough unreserved space in the desired configuration.
     * 
     * @param enrollment
     *            {@link Enrollment} that is being considered
     * @param conflicts
     *            all computed conflicting requests are added into this set
     */
    @Override
    public void computeConflicts(Assignment<Request, Enrollment> assignment, Enrollment enrollment, Set<Enrollment> conflicts) {
        // enrollment's config
        Config config = enrollment.getConfig();

        // exclude free time requests
        if (config == null)
            return;
        
        // exclude empty enrollmens
        if (enrollment.getSections() == null || enrollment.getSections().isEmpty())
            return;
        
        // no reservations
        if (!config.getOffering().hasReservations())
            return;
        
        // enrollment's reservation
        Reservation reservation = enrollment.getReservation();
        
        // check space in the reservation reservation
        if (reservation != null) {
            // check reservation too
            double reserved = reservation.getReservedAvailableSpace(assignment, enrollment.getRequest());
            
            if (reservation.getLimit() >= 0 && reserved < enrollment.getRequest().getWeight()) {
                // reservation is not unlimited and there is not enough space in it
                
                // try to free some space in the reservation
                List<Enrollment> adepts = new ArrayList<Enrollment>(config.getEnrollments(assignment).size());
                for (Enrollment e : config.getEnrollments(assignment)) {
                    if (e.getRequest().equals(enrollment.getRequest()))
                        continue;
                    if (!reservation.equals(e.getReservation()))
                        continue;
                    if (conflicts.contains(e))
                        reserved += e.getRequest().getWeight();
                    else
                        adepts.add(e);
                }
                
                while (reserved < enrollment.getRequest().getWeight()) {
                    if (adepts.isEmpty()) {
                        // no adepts -> enrollment cannot be assigned
                        conflicts.add(enrollment);
                        return;
                    }
                    
                    // pick adept (prefer dummy students), decrease unreserved space,
                    // make conflict
                    Enrollment conflict = new Adepts(iPreferDummyStudents, iPreferPriorityStudents, adepts, assignment).get();
                    adepts.remove(conflict);
                    reserved += conflict.getRequest().getWeight();
                    conflicts.add(conflict);
                }
            }

            // if not configuration reservation -> check configuration unavailable space
            if (!hasConfigReservation(enrollment)) {
                // check reservation can assign over the limit
                if (reservation.canBatchAssignOverLimit())
                    return;

                // check the total first (basically meaning that there will never be enough space in 
                // the section for an enrollment w/o configuration reservation
                if (config.getTotalUnreservedSpace() < enrollment.getRequest().getWeight()) {
                    conflicts.add(enrollment);
                    return;
                }

                double unreserved = getUnreservedSpace(assignment, config, enrollment.getRequest(), true);

                if (unreserved < 0.0) {
                    // no unreserved space available -> cannot be assigned
                    // try to unassign some other enrollments that also do not have config reservation
                    
                    List<Enrollment> adepts = new ArrayList<Enrollment>(config.getEnrollments(assignment).size());
                    for (Enrollment e : config.getEnrollments(assignment)) {
                        if (e.getRequest().equals(enrollment.getRequest()))
                            continue;
                        if (hasConfigReservation(e))
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
        } else { // no reservation at all
            // check the total first (basically meaning that there will never be enough space in
            // the section for an enrollment w/o reservation
            if (config.getOffering().getTotalUnreservedSpace() < enrollment.getRequest().getWeight() || 
                config.getTotalUnreservedSpace() < enrollment.getRequest().getWeight()) {
                conflicts.add(enrollment);
                return;
            }
                
            // check configuration unavailable space too
            double unreserved = getUnreservedSpace(assignment, config, enrollment.getRequest(), false);
                
            if (unreserved < 0.0) {
                // no unreserved space available -> cannot be assigned
                // try to unassign some other enrollments that also do not have reservation
                
                List<Enrollment> adepts = new ArrayList<Enrollment>(config.getEnrollments(assignment).size());
                for (Enrollment e : config.getEnrollments(assignment)) {
                    if (e.getRequest().equals(enrollment.getRequest()))
                        continue;
                    if (e.getReservation() != null)
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
    }

    /**
     * True if the enrollment has reservation for this configuration.
     **/
    private boolean hasConfigReservation(Enrollment enrollment) {
        if (enrollment.getConfig() == null) return false;
        Reservation reservation = enrollment.getReservation();
        if (reservation == null) return false;
        return reservation.getConfigs().contains(enrollment.getConfig());
    }

    
    /**
     * A given enrollment is conflicting, if the config's enrollment (computed by
     * {@link ConfigLimit#getEnrollmentWeight(Assignment, Config, Request)}) exceeds the
     * limit.
     * 
     * @param enrollment
     *            {@link Enrollment} that is being considered
     * @return true, if the enrollment cannot be assigned without exceeding the limit
     */
    @Override
    public boolean inConflict(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        // enrollment's config
        Config config = enrollment.getConfig();

        // exclude free time requests
        if (config == null)
            return false;
        
        // exclude empty enrollmens
        if (enrollment.getSections() == null || enrollment.getSections().isEmpty())
            return false;
        
        // enrollment's reservation
        Reservation reservation = enrollment.getReservation();
        
        // check reservation
        if (reservation != null) {
            // reservation is not unlimited and there is not enough space in it
            if (reservation.getLimit() >= 0 &&
                    reservation.getReservedAvailableSpace(assignment, enrollment.getRequest()) < enrollment.getRequest().getWeight())
                return true;
            
            // check reservation can assign over the limit
            if (reservation.canBatchAssignOverLimit())
                return false;

            // if not configuration reservation, check configuration unreserved space too
            return (!hasConfigReservation(enrollment) && (
                    config.getTotalUnreservedSpace() < enrollment.getRequest().getWeight() ||
                    getUnreservedSpace(assignment, config, enrollment.getRequest(), true) < 0.0));
        } else {
            // check unreserved space;
            return config.getOffering().getTotalUnreservedSpace() < enrollment.getRequest().getWeight() || 
                   config.getTotalUnreservedSpace() < enrollment.getRequest().getWeight() ||
                   getUnreservedSpace(assignment, config, enrollment.getRequest(), false) < 0.0;
        }
    }
    
    @Override
    public String toString() {
        return "ReservationLimit";
    }

}
