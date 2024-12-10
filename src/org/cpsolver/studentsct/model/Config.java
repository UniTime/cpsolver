package org.cpsolver.studentsct.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AbstractClassWithContext;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.CanInheritContext;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.studentsct.reservation.Reservation;




/**
 * Representation of a configuration of an offering. A configuration contains
 * id, name, an offering and a list of subparts. <br>
 * <br>
 * Each instructional offering (see {@link Offering}) contains one or more
 * configurations. Each configuration contain one or more subparts. Each student
 * has to take a class of each subpart of one of the possible configurations.
 * 
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
public class Config extends AbstractClassWithContext<Request, Enrollment, Config.ConfigContext> implements CanInheritContext<Request, Enrollment, Config.ConfigContext> {
    private long iId = -1;
    private String iName = null;
    private Offering iOffering = null;
    private int iLimit = -1;
    private List<Subpart> iSubparts = new ArrayList<Subpart>();
    private Long iInstrMethodId;
    private String iInstrMethodName;
    private String iInstrMethodReference;

    /**
     * Constructor
     * 
     * @param id
     *            instructional offering configuration unique id
     * @param limit
     *            configuration limit (-1 for unlimited)
     * @param name
     *            configuration name
     * @param offering
     *            instructional offering to which this configuration belongs
     */
    public Config(long id, int limit, String name, Offering offering) {
        iId = id;
        iLimit = limit;
        iName = name;
        iOffering = offering;
        iOffering.getConfigs().add(this);
    }

    /** Configuration id 
     * @return instructional offering configuration unique id
     **/
    public long getId() {
        return iId;
    }
    
    /**
     * Configuration limit. This is defines the maximal number of students that can be
     * enrolled into this configuration at the same time. It is -1 in the case of an
     * unlimited configuration
     * @return configuration limit
     */
    public int getLimit() {
        return iLimit;
    }

    /** Set configuration limit 
     * @param limit configuration limit, -1 if unlimited
     **/
    public void setLimit(int limit) {
        iLimit = limit;
    }



    /** Configuration name 
     * @return configuration name
     **/
    public String getName() {
        return iName;
    }

    /** Instructional offering to which this configuration belongs. 
     * @return instructional offering
     **/
    public Offering getOffering() {
        return iOffering;
    }

    /** List of subparts 
     * @return scheduling subparts
     **/
    public List<Subpart> getSubparts() {
        return iSubparts;
    }
    
    /**
     * Return instructional method id
     * @return instructional method id
     */
    public Long getInstructionalMethodId() { return iInstrMethodId; }
    
    /**
     * Set instructional method id
     * @param instrMethodId instructional method id
     */
    public void setInstructionalMethodId(Long instrMethodId) { iInstrMethodId = instrMethodId; }
    
    /**
     * Return instructional method name
     * @return instructional method name
     */
    public String getInstructionalMethodName() { return iInstrMethodName; }
    
    /**
     * Set instructional method name
     * @param instrMethodName instructional method name
     */
    public void setInstructionalMethodName(String instrMethodName) { iInstrMethodName = instrMethodName; }
    
    /**
     * Return instructional method reference
     * @return instructional method reference
     */
    public String getInstructionalMethodReference() { return iInstrMethodReference; }
    
    /**
     * Set instructional method reference
     * @param instrMethodReference instructional method reference
     */
    public void setInstructionalMethodReference(String instrMethodReference) { iInstrMethodReference = instrMethodReference; }

    @Override
    public String toString() {
        return getName();
    }

    /** Average minimal penalty from {@link Subpart#getMinPenalty()} 
     * @return minimal penalty
     **/
    public double getMinPenalty() {
        double min = 0.0;
        for (Subpart subpart : getSubparts()) {
            min += subpart.getMinPenalty();
        }
        return min / getSubparts().size();
    }

    /** Average maximal penalty from {@link Subpart#getMaxPenalty()} 
     * @return maximal penalty
     **/
    public double getMaxPenalty() {
        double max = 0.0;
        for (Subpart subpart : getSubparts()) {
            max += subpart.getMinPenalty();
        }
        return max / getSubparts().size();
    }
    
    /**
     * Available space in the configuration that is not reserved by any config reservation
     * @param assignment current assignment
     * @param excludeRequest excluding given request (if not null)
     * @return available space
     **/
    public double getUnreservedSpace(Assignment<Request, Enrollment> assignment, Request excludeRequest) {
        // configuration is unlimited -> there is unreserved space unless there is an unlimited reservation too 
        // (in which case there is no unreserved space)
        if (getLimit() < 0) {
            // exclude reservations that are not directly set on this section
            for (Reservation r: getConfigReservations()) {
                // ignore expired reservations
                if (r.isExpired()) continue;
                // there is an unlimited reservation -> no unreserved space
                if (r.getLimit(this) < 0) return 0.0;
            }
            return Double.MAX_VALUE;
        }
        
        double available = getLimit() - getContext(assignment).getEnrollmentWeight(assignment, excludeRequest);
        // exclude reservations that are not directly set on this section
        for (Reservation r: getConfigReservations()) {
            // ignore expired reservations
            if (r.isExpired()) continue;
            // unlimited reservation -> all the space is reserved
            if (r.getLimit(this) < 0.0) return 0.0;
            // compute space that can be potentially taken by this reservation
            double reserved = r.getContext(assignment).getReservedAvailableSpace(assignment, this, excludeRequest);
            // deduct the space from available space
            available -= Math.max(0.0, reserved);
        }
        
        return available;
    }
    
    /**
     * Total space in the configuration that cannot be reserved by any config reservation
     * @return total unreserved space
     **/
    public synchronized double getTotalUnreservedSpace() {
        if (iTotalUnreservedSpace == null)
            iTotalUnreservedSpace = getTotalUnreservedSpaceNoCache();
        return iTotalUnreservedSpace;
    }
    private Double iTotalUnreservedSpace = null;
    private double getTotalUnreservedSpaceNoCache() {
        // configuration is unlimited -> there is unreserved space unless there is an unlimited reservation too 
        // (in which case there is no unreserved space)
        if (getLimit() < 0) {
            // exclude reservations that are not directly set on this section
            for (Reservation r: getConfigReservations()) {
                // ignore expired reservations
                if (r.isExpired()) continue;
                // there is an unlimited reservation -> no unreserved space
                if (r.getLimit(this) < 0) return 0.0;
            }
            return Double.MAX_VALUE;
        }
        
        // we need to check all reservations linked with this section
        double available = getLimit(), reserved = 0, exclusive = 0;
        Set<Config> configs = new HashSet<Config>();
        reservations: for (Reservation r: getConfigReservations()) {
            // ignore expired reservations
            if (r.isExpired()) continue;
            // unlimited reservation -> no unreserved space
            if (r.getLimit(this) < 0) return 0.0;
            for (Config s: r.getConfigs()) {
                if (s.equals(this)) continue;
                if (s.getLimit() < 0) continue reservations;
                if (configs.add(s))
                    available += s.getLimit();
            }
            reserved += r.getLimit(this);
            if (r.getConfigs().size() == 1)
                exclusive += r.getLimit(this);
        }
        
        return Math.min(available - reserved, getLimit() - exclusive);
    }
    
    /**
     * Get reservations for this configuration
     * @return related reservations
     */
    public synchronized List<Reservation> getReservations() {
        if (iReservations == null) {
            iReservations = new ArrayList<Reservation>();
            for (Reservation r: getOffering().getReservations()) {
                if (r.getConfigs().isEmpty() || r.getConfigs().contains(this))
                    iReservations.add(r);
            }
        }
        return iReservations;
    }
    List<Reservation> iReservations = null;
    
    /**
     * Get reservations that require this configuration
     * @return related reservations
     */
    public synchronized List<Reservation> getConfigReservations() {
        if (iConfigReservations == null) {
            iConfigReservations = new ArrayList<Reservation>();
            for (Reservation r: getOffering().getReservations()) {
                if (!r.getConfigs().isEmpty() && r.getConfigs().contains(this))
                    iConfigReservations.add(r);
            }
        }
        return iConfigReservations;
    }
    List<Reservation> iConfigReservations = null;
    
    /**
     * Clear reservation information that was cached on this configuration or below
     */
    public synchronized void clearReservationCache() {
        for (Subpart s: getSubparts())
            s.clearReservationCache();
        iReservations = null;
        iConfigReservations = null;
        iTotalUnreservedSpace = null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Config)) return false;
        return getId() == ((Config)o).getId();
    }
    
    @Override
    public int hashCode() {
        return Long.valueOf(getId()).hashCode();
    }
    
    @Override
    public Model<Request, Enrollment> getModel() {
        return getOffering().getModel();
    }
    
    /** Set of assigned enrollments 
     * @param assignment current assignment
     * @return enrollments in this configuration
     **/
    public Set<Enrollment> getEnrollments(Assignment<Request, Enrollment> assignment) {
        return getContext(assignment).getEnrollments();
    }
    
    /**
     * Enrollment weight -- weight of all requests which have an enrollment that
     * contains this config, excluding the given one. See
     * {@link Request#getWeight()}.
     * @param assignment current assignment
     * @param excludeRequest request to exclude, null if all requests are to be included
     * @return enrollment weight
     */
    public double getEnrollmentWeight(Assignment<Request, Enrollment> assignment, Request excludeRequest) {
        return getContext(assignment).getEnrollmentWeight(assignment, excludeRequest);
    }
    
    /**
     * Enrollment weight including over the limit enrollments.
     * That is enrollments that have reservation with {@link Reservation#canBatchAssignOverLimit()} set to true.
     * {@link Request#getWeight()}.
     * @param assignment current assignment
     * @param excludeRequest request to exclude, null if all requests are to be included
     * @return enrollment weight
     */
    public double getEnrollmentTotalWeight(Assignment<Request, Enrollment> assignment, Request excludeRequest) {
        return getContext(assignment).getEnrollmentTotalWeight(assignment, excludeRequest);
    }
    
    /**
     * Maximal weight of a single enrollment in the config
     * @param assignment current assignment
     * @return maximal enrollment weight
     */
    public double getMaxEnrollmentWeight(Assignment<Request, Enrollment> assignment) {
        return getContext(assignment).getMaxEnrollmentWeight();
    }

    /**
     * Minimal weight of a single enrollment in the config
     * @param assignment current assignment
     * @return minimal enrollment weight
     */
    public double getMinEnrollmentWeight(Assignment<Request, Enrollment> assignment) {
        return getContext(assignment).getMinEnrollmentWeight();
    }

    @Override
    public ConfigContext createAssignmentContext(Assignment<Request, Enrollment> assignment) {
        return new ConfigContext(assignment);
    }
    

    @Override
    public ConfigContext inheritAssignmentContext(Assignment<Request, Enrollment> assignment, ConfigContext parentContext) {
        return new ConfigContext(parentContext);
    }
    
    public class ConfigContext implements AssignmentConstraintContext<Request, Enrollment> {
        private double iEnrollmentWeight = 0.0;
        private double iEnrollmentTotalWeight = 0.0;
        private double iMaxEnrollmentWeight = 0.0;
        private double iMinEnrollmentWeight = 0.0;
        private Set<Enrollment> iEnrollments = null;
        private boolean iReadOnly = false;

        public ConfigContext(Assignment<Request, Enrollment> assignment) {
            iEnrollments = new HashSet<Enrollment>();
            for (Course course: getOffering().getCourses()) {
                for (CourseRequest request: course.getRequests()) {
                    Enrollment enrollment = assignment.getValue(request);
                    if (enrollment != null && Config.this.equals(enrollment.getConfig()))
                        assigned(assignment, enrollment);
                }
            }
        }
        
        public ConfigContext(ConfigContext parent) {
            iEnrollmentWeight = parent.iEnrollmentWeight;
            iEnrollmentTotalWeight = parent.iEnrollmentTotalWeight;
            iMaxEnrollmentWeight = parent.iMaxEnrollmentWeight;
            iMinEnrollmentWeight = parent.iMinEnrollmentWeight;
            iEnrollments = parent.iEnrollments;
            iReadOnly = true;
        }

        /** Called when an enrollment with this config is assigned to a request */
        @Override
        public void assigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
            if (iReadOnly) {
                iEnrollments = new HashSet<Enrollment>(iEnrollments);
                iReadOnly = false;
            }
            if (iEnrollments.isEmpty()) {
                iMinEnrollmentWeight = iMaxEnrollmentWeight = enrollment.getRequest().getWeight();
            } else {
                iMaxEnrollmentWeight = Math.max(iMaxEnrollmentWeight, enrollment.getRequest().getWeight());
                iMinEnrollmentWeight = Math.min(iMinEnrollmentWeight, enrollment.getRequest().getWeight());
            }
            if (iEnrollments.add(enrollment)) {
                iEnrollmentTotalWeight += enrollment.getRequest().getWeight();
                if (enrollment.getReservation() == null || !enrollment.getReservation().canBatchAssignOverLimit())
                    iEnrollmentWeight += enrollment.getRequest().getWeight();
            }
        }

        /** Called when an enrollment with this config is unassigned from a request */
        @Override
        public void unassigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
            if (iReadOnly) {
                iEnrollments = new HashSet<Enrollment>(iEnrollments);
                iReadOnly = false;
            }
            if (iEnrollments.remove(enrollment)) {
                iEnrollmentTotalWeight -= enrollment.getRequest().getWeight();
                if (enrollment.getReservation() == null || !enrollment.getReservation().canBatchAssignOverLimit())
                    iEnrollmentWeight -= enrollment.getRequest().getWeight();
            }
            if (iEnrollments.isEmpty()) {
                iMinEnrollmentWeight = iMaxEnrollmentWeight = 0;
            } else if (iMinEnrollmentWeight != iMaxEnrollmentWeight) {
                if (iMinEnrollmentWeight == enrollment.getRequest().getWeight()) {
                    double newMinEnrollmentWeight = Double.MAX_VALUE;
                    for (Enrollment e : iEnrollments) {
                        if (e.getRequest().getWeight() == iMinEnrollmentWeight) {
                            newMinEnrollmentWeight = iMinEnrollmentWeight;
                            break;
                        } else {
                            newMinEnrollmentWeight = Math.min(newMinEnrollmentWeight, e.getRequest().getWeight());
                        }
                    }
                    iMinEnrollmentWeight = newMinEnrollmentWeight;
                }
                if (iMaxEnrollmentWeight == enrollment.getRequest().getWeight()) {
                    double newMaxEnrollmentWeight = Double.MIN_VALUE;
                    for (Enrollment e : iEnrollments) {
                        if (e.getRequest().getWeight() == iMaxEnrollmentWeight) {
                            newMaxEnrollmentWeight = iMaxEnrollmentWeight;
                            break;
                        } else {
                            newMaxEnrollmentWeight = Math.max(newMaxEnrollmentWeight, e.getRequest().getWeight());
                        }
                    }
                    iMaxEnrollmentWeight = newMaxEnrollmentWeight;
                }
            }
        }
        
        /**
         * Enrollment weight -- weight of all requests which have an enrollment that
         * contains this config, excluding the given one. See
         * {@link Request#getWeight()}.
         * @param assignment current assignment
         * @param excludeRequest request to exclude
         * @return enrollment weight
         */
        public double getEnrollmentWeight(Assignment<Request, Enrollment> assignment, Request excludeRequest) {
            double weight = iEnrollmentWeight;
            if (excludeRequest != null) {
                Enrollment enrollment = assignment.getValue(excludeRequest);
                if (enrollment != null && iEnrollments.contains(enrollment) && (enrollment.getReservation() == null || !enrollment.getReservation().canBatchAssignOverLimit()))
                    weight -= excludeRequest.getWeight();
            }
            return weight;
        }
        
        /**
         * Enrollment weight including over the limit enrollments.
         * That is enrollments that have reservation with {@link Reservation#canBatchAssignOverLimit()} set to true.
         * @param assignment current assignment
         * @param excludeRequest request to exclude
         * @return enrollment weight
         */
        public double getEnrollmentTotalWeight(Assignment<Request, Enrollment> assignment, Request excludeRequest) {
            double weight = iEnrollmentTotalWeight;
            if (excludeRequest != null) {
                Enrollment enrollment = assignment.getValue(excludeRequest);
                if (enrollment != null && iEnrollments.contains(enrollment))
                    weight -= excludeRequest.getWeight();
            }
            return weight;
        }
        
        /** Set of assigned enrollments 
         * @return assigned enrollments (using this configuration)
         **/
        public Set<Enrollment> getEnrollments() {
            return iEnrollments;
        }

        /**
         * Maximal weight of a single enrollment in the config
         * @return maximal enrollment weight
         */
        public double getMaxEnrollmentWeight() {
            return iMaxEnrollmentWeight;
        }

        /**
         * Minimal weight of a single enrollment in the config
         * @return minimal enrollment weight
         */
        public double getMinEnrollmentWeight() {
            return iMinEnrollmentWeight;
        }
    }
    
    /**
     * True if at least one subpart of this config has a credit value set
     * @return true if a there is a subpart credit
     */
    public boolean hasCreditValue() {
        for (Subpart subpart: getSubparts())
            if (subpart.hasCreditValue()) return true;
        return false;
    }
    
    /**
     * Sum of subpart credit of this config
     * return config credit value
     */
    public Float getCreditValue() {
        float credit = 0f; boolean hasCredit = false;
        for (Subpart subpart: getSubparts())
            if (subpart.hasCreditValue()) {
                hasCredit = true;
                credit += subpart.getCreditValue();
            }
        return (hasCredit ? Float.valueOf(credit) : null);
    }
    
    public int getNrOnline() {
        int online = 0;
        for (Subpart subpart: getSubparts())
            if (subpart.isOnline()) online ++;
        return online;
    }
    
    public int getNrArrHours() {
        int arrHrs = 0;
        for (Subpart subpart: getSubparts())
            if (!subpart.hasTime()) arrHrs ++;
        return arrHrs;
    }
    
    public int getNrPast() {
        int past = 0;
        for (Subpart subpart: getSubparts())
            if (subpart.isPast()) past ++;
        return past;
    }
}
