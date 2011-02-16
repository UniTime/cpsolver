package net.sf.cpsolver.studentsct.constraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.ifs.model.GlobalConstraint;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.model.Config;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.reservation.Reservation;

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
 * <table border='1'>
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
public class ReservationLimit extends GlobalConstraint<Request, Enrollment> {
    private boolean iPreferDummyStudents = false;

    /**
     * Constructor
     * 
     * @param cfg
     *            solver configuration
     */
    public ReservationLimit(DataProperties cfg) {
        super();
        iPreferDummyStudents = cfg.getPropertyBoolean("ReservationLimit.PreferDummyStudents", false);
    }


    /**
     * A given enrollment is conflicting, if the reservation's remaning available space
     * (computed by {@link Reservation#getReservedAvailableSpace(Request)})
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
    public void computeConflicts(Enrollment enrollment, Set<Enrollment> conflicts) {
        // enrollment's config
        Config config = enrollment.getConfig();

        // exclude free time requests
        if (config == null)
            return;
        
        // no reservations
        if (!config.getOffering().hasReservations())
            return;
        
        // enrollment's reservation
        Reservation reservation = enrollment.getReservation();
        
        // check space in the reservation reservation
        if (reservation != null) {
            // check reservation too
            double reserved = reservation.getReservedAvailableSpace(enrollment.getRequest());
            
            if (reservation.getLimit() >= 0 && reserved < enrollment.getRequest().getWeight()) {
                // reservation is not unlimited and there is not enough space in it
                
                // try to free some space in the reservation
                List<Enrollment> adepts = new ArrayList<Enrollment>(config.getEnrollments().size());
                for (Enrollment e : config.getEnrollments()) {
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
                    reserved += conflict.getRequest().getWeight();
                    conflicts.add(conflict);
                }
            }

            // if not configuration reservation -> check configuration unavailable space
            if (!hasConfigReservation(enrollment)) {
                // check the total first (basically meaning that there will never be enough space in 
                // the section for an enrollment w/o configuration reservation
                if (config.getTotalUnreservedSpace() < enrollment.getRequest().getWeight()) {
                    conflicts.add(enrollment);
                    return;
                }

                double unreserved = config.getUnreservedSpace(enrollment.getRequest());

                if (unreserved < enrollment.getRequest().getWeight()) {
                    // no unreserved space available -> cannot be assigned
                    // try to unassign some other enrollments that also do not have config reservation
                    
                    List<Enrollment> adepts = new ArrayList<Enrollment>(config.getEnrollments().size());
                    for (Enrollment e : config.getEnrollments()) {
                        if (e.getRequest().equals(enrollment.getRequest()))
                            continue;
                        if (hasConfigReservation(e))
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
        } else { // no reservation at all
            // check the total first (basically meaning that there will never be enough space in
            // the section for an enrollment w/o reservation
            if (config.getOffering().getTotalUnreservedSpace() < enrollment.getRequest().getWeight() || 
                config.getTotalUnreservedSpace() < enrollment.getRequest().getWeight()) {
                conflicts.add(enrollment);
                return;
            }
                
            // check configuration unavailable space too
            double unreserved = Math.min(
                    config.getOffering().getUnreservedSpace(enrollment.getRequest()),
                    config.getUnreservedSpace(enrollment.getRequest()));
                
            if (unreserved < enrollment.getRequest().getWeight()) {
                // no unreserved space available -> cannot be assigned
                // try to unassign some other enrollments that also do not have reservation
                
                List<Enrollment> adepts = new ArrayList<Enrollment>(config.getEnrollments().size());
                for (Enrollment e : config.getEnrollments()) {
                    if (e.getRequest().equals(enrollment.getRequest()))
                        continue;
                    if (e.getReservation() != null)
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
     * {@link ConfigLimit#getEnrollmentWeight(Config, Request)}) exceeds the
     * limit.
     * 
     * @param enrollment
     *            {@link Enrollment} that is being considered
     * @return true, if the enrollment cannot be assigned without exceeding the limit
     */
    @Override
    public boolean inConflict(Enrollment enrollment) {
        // enrollment's config
        Config config = enrollment.getConfig();

        // exclude free time requests
        if (config == null)
            return false;
        
        // enrollment's reservation
        Reservation reservation = enrollment.getReservation();
        
        // check reservation
        if (reservation != null) {
            // unlimited reservation
            if (reservation.getLimit() < 0)
                return false;
            
            // if not configuration reservation, check configuration unreserved space too
            if (!hasConfigReservation(enrollment) &&
                config.getUnreservedSpace(enrollment.getRequest()) < enrollment.getRequest().getWeight())
                return false;
            
            // check remaining space
            return reservation.getReservedAvailableSpace(enrollment.getRequest()) < enrollment.getRequest().getWeight();
        } else {
            // check unreserved space;
            return config.getOffering().getTotalUnreservedSpace() < enrollment.getRequest().getWeight() || 
                   config.getTotalUnreservedSpace() < enrollment.getRequest().getWeight() ||
                   config.getUnreservedSpace(enrollment.getRequest()) < enrollment.getRequest().getWeight() ||
                   config.getOffering().getUnreservedSpace(enrollment.getRequest()) < enrollment.getRequest().getWeight();
        }
    }
    
    @Override
    public String toString() {
        return "ReservationLimit";
    }

}
