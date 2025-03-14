package org.cpsolver.studentsct.constraint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Request.RequestPriority;
import org.cpsolver.studentsct.model.Student.StudentPriority;


/**
 * Configuration limit constraint. This global constraint ensures that a limit of each
 * configuration is not exceeded. This means that the total sum of weights of course
 * requests (see {@link Request#getWeight()}) enrolled into a configuration is below
 * the configuration's limit (see {@link Config#getLimit()}).
 * 
 * <br>
 * <br>
 * Configurations with negative limit are considered unlimited, and therefore
 * completely ignored by this constraint.
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
 * <td>ConfigLimit.PreferDummyStudents</td>
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
public class ConfigLimit extends GlobalConstraint<Request, Enrollment> {
    
    private static double sNominalWeight = 0.00001;
    private boolean iPreferDummyStudents = false;
    private boolean iPreferPriorityStudents = true;

    /**
     * Constructor
     * 
     * @param cfg
     *            solver configuration
     */
    public ConfigLimit(DataProperties cfg) {
        super();
        iPreferDummyStudents = cfg.getPropertyBoolean("ConfigLimit.PreferDummyStudents", false);
        iPreferPriorityStudents = cfg.getPropertyBoolean("Sectioning.PriorityStudentsFirstSelection.AllIn", true);
    }


    /**
     * Enrollment weight of a config if the given request is assigned. In order
     * to overcome rounding problems with last-like students ( e.g., 5 students
     * are projected to two configs of limit 2 -- each section can have up to 3
     * of these last-like students), the weight of the request with the highest
     * weight in the config is changed to a small nominal weight.
     * 
     * @param assignment current assignment
     * @param config
     *            a config that is of concern
     * @param request
     *            a request of a student to be assigned containing the given
     *            section
     * @return section's new weight
     */
    public static double getEnrollmentWeight(Assignment<Request, Enrollment> assignment, Config config, Request request) {
        return config.getEnrollmentWeight(assignment, request) + request.getWeight() - Math.max(config.getMaxEnrollmentWeight(assignment), request.getWeight()) + sNominalWeight;
    }

    /**
     * A given enrollment is conflicting, if the config's enrollment
     * (computed by {@link ConfigLimit#getEnrollmentWeight(Assignment, Config, Request)})
     * exceeds the limit. <br>
     * If the limit is breached, one or more existing enrollments are
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
        
        // enrollment's config
        Config config = enrollment.getConfig();

        // exclude free time requests
        if (config == null)
            return;

        // exclude empty enrollmens
        if (enrollment.getSections() == null || enrollment.getSections().isEmpty())
            return;

        // unlimited config
        if (config.getLimit() < 0)
            return;
        
        // new enrollment weight
        double enrlWeight = getEnrollmentWeight(assignment, config, enrollment.getRequest());

        // below limit -> ok
        if (enrlWeight <= config.getLimit())
            return;

        // above limit -> compute adepts (current assignments that are not
        // yet conflicting)
        // exclude all conflicts as well
        ArrayList<Enrollment> adepts = new ArrayList<Enrollment>(config.getEnrollments(assignment).size());
        for (Enrollment e : config.getEnrollments(assignment)) {
            if (e.getRequest().equals(enrollment.getRequest()))
                continue;
            if (e.getReservation() != null && e.getReservation().canBatchAssignOverLimit())
                continue;
            if (conflicts.contains(e))
                enrlWeight -= e.getRequest().getWeight();
            else
                adepts.add(e);
        }

        // while above limit -> pick an adept and make it conflicting
        while (enrlWeight > config.getLimit()) {
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
        // check reservation can assign over the limit
        if (enrollment.getReservation() != null && enrollment.getReservation().canBatchAssignOverLimit())
            return false;

        // enrollment's config
        Config config = enrollment.getConfig();

        // exclude free time requests
        if (config == null)
            return false;
        
        // exclude empty enrollmens
        if (enrollment.getSections() == null || enrollment.getSections().isEmpty())
            return false;

        // unlimited config
        if (config.getLimit() < 0)
            return false;

        // new enrollment weight
        double enrlWeight = getEnrollmentWeight(assignment, config, enrollment.getRequest());
        
        // above limit -> conflict
        return (enrlWeight > config.getLimit());
    }
    
    @Override
    public String toString() {
        return "ConfigLimit";
    }
    
    /**
     * Support class to pick an adept (prefer dummy students &amp; students w/o reservation)
     */
    static class Adepts {
        private ArrayList<Enrollment> iEnrollments;
        private double iValue;
        private boolean iDummy;
        private StudentPriority iPriority;
        private RequestPriority iRequestPriority;
        private boolean iReservation;
        private boolean iConsiderDummy;
        private boolean iPriorityFirst;
        
        public Adepts(boolean preferDummy, boolean priorityFirst) {
            iConsiderDummy = preferDummy;
            iPriorityFirst = priorityFirst;
            iEnrollments = new ArrayList<Enrollment>();
        }
        
        public Adepts(boolean preferDummy, boolean priorityFirst, int size) {
            iConsiderDummy = preferDummy;
            iPriorityFirst = priorityFirst;
            iEnrollments = new ArrayList<Enrollment>(size);
        }
        
        public Adepts(boolean preferDummy, boolean priorityFirst, Collection<Enrollment> adepts, Assignment<Request, Enrollment> assignment) {
            iConsiderDummy = preferDummy;
            iPriorityFirst = priorityFirst;
            iEnrollments = new ArrayList<Enrollment>(adepts.size());
            for (Enrollment adept: adepts)
                add(adept, assignment);
        }
        
        public void add(Enrollment enrollment, Assignment<Request, Enrollment> assignment) {
            double value = enrollment.toDouble(assignment, false);
            boolean dummy = enrollment.getStudent().isDummy();
            StudentPriority priority = enrollment.getStudent().getPriority();
            boolean reservation = (enrollment.getReservation() != null);
            RequestPriority rp = enrollment.getRequest().getRequestPriority();
            if (iEnrollments.isEmpty()) { // no adepts yet
                iValue = value; iDummy = dummy; iPriority = priority; iRequestPriority = rp; iReservation = reservation;
                iEnrollments.add(enrollment);
                return;
            }
            if (iConsiderDummy && iDummy != dummy) { // different dummy
                if (!dummy) return; // ignore not-dummy students
                iEnrollments.clear();
                iValue = value; iDummy = dummy; iPriority = priority; iRequestPriority = rp; iReservation = reservation;
                iEnrollments.add(enrollment);
                return;
            }
            if (iPriorityFirst) {
                if (iPriority != priority) { // different priority
                    if (priority.ordinal() < iPriority.ordinal()) return; // ignore more priority students
                    iEnrollments.clear();
                    iValue = value; iDummy = dummy; iPriority = priority; iRequestPriority = rp; iReservation = reservation;
                    iEnrollments.add(enrollment);
                    return;
                }
                if (iRequestPriority != rp) { // different request priority
                    if (rp.ordinal() < iRequestPriority.ordinal()) return; // ignore more critical courses
                    iEnrollments.clear();
                    iValue = value; iDummy = dummy; iPriority = priority; iRequestPriority = rp; iReservation = reservation;
                    iEnrollments.add(enrollment);
                    return;
                }
            } else {
                if (iRequestPriority != rp) { // different request priority
                    if (rp.ordinal() < iRequestPriority.ordinal()) return; // ignore more critical courses
                    iEnrollments.clear();
                    iValue = value; iDummy = dummy; iPriority = priority; iRequestPriority = rp; iReservation = reservation;
                    iEnrollments.add(enrollment);
                    return;
                }
                if (iPriority != priority) { // different priority
                    if (priority.ordinal() < iPriority.ordinal()) return; // ignore more priority students
                    iEnrollments.clear();
                    iValue = value; iDummy = dummy; iPriority = priority; iRequestPriority = rp; iReservation = reservation;
                    iEnrollments.add(enrollment);
                    return;
                }
            }
            if (iReservation != reservation) { // different reservation
                if (reservation) return; // ignore students with reservation
                iEnrollments.clear();
                iValue = value; iDummy = dummy; iPriority = priority; iRequestPriority = rp; iReservation = reservation;
                iEnrollments.add(enrollment);
                return;
            }
            // prefer requests with higher value (lower priority)
            if (value > iValue) {
                iEnrollments.clear();
                iValue = value; iDummy = dummy; iPriority = priority; iRequestPriority = rp; iReservation = reservation;
                iEnrollments.add(enrollment);
            } else if (value == iValue) {
                iEnrollments.add(enrollment);
            }
        }
        
        public Enrollment get() {
            if (iEnrollments.isEmpty()) return null;
            return ToolBox.random(iEnrollments);
        }
        
        public boolean isEmpty() {
            return iEnrollments.isEmpty();
        }
    }
}
