package net.sf.cpsolver.studentsct.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.studentsct.reservation.Reservation;



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
public class Config {
    private long iId = -1;
    private String iName = null;
    private Offering iOffering = null;
    private int iLimit = -1;
    private List<Subpart> iSubparts = new ArrayList<Subpart>();
    private double iEnrollmentWeight = 0.0;
    private double iMaxEnrollmentWeight = 0.0;
    private double iMinEnrollmentWeight = 0.0;
    private Set<Enrollment> iEnrollments = new HashSet<Enrollment>();

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

    /** Configuration id */
    public long getId() {
        return iId;
    }
    
    /**
     * Configuration limit. This is defines the maximal number of students that can be
     * enrolled into this configuration at the same time. It is -1 in the case of an
     * unlimited configuration
     */
    public int getLimit() {
        return iLimit;
    }

    /** Set configuration limit */
    public void setLimit(int limit) {
        iLimit = limit;
    }



    /** Configuration name */
    public String getName() {
        return iName;
    }

    /** Instructional offering to which this configuration belongs. */
    public Offering getOffering() {
        return iOffering;
    }

    /** List of subparts */
    public List<Subpart> getSubparts() {
        return iSubparts;
    }

    @Override
    public String toString() {
        return getName();
    }

    /** Average minimal penalty from {@link Subpart#getMinPenalty()} */
    public double getMinPenalty() {
        double min = 0.0;
        for (Subpart subpart : getSubparts()) {
            min += subpart.getMinPenalty();
        }
        return min / getSubparts().size();
    }

    /** Average maximal penalty from {@link Subpart#getMaxPenalty()} */
    public double getMaxPenalty() {
        double max = 0.0;
        for (Subpart subpart : getSubparts()) {
            max += subpart.getMinPenalty();
        }
        return max / getSubparts().size();
    }
    
    /** Called when an enrollment with this config is assigned to a request */
    public void assigned(Enrollment enrollment) {
        if (iEnrollments.isEmpty()) {
            iMinEnrollmentWeight = iMaxEnrollmentWeight = enrollment.getRequest().getWeight();
        } else {
            iMaxEnrollmentWeight = Math.max(iMaxEnrollmentWeight, enrollment.getRequest().getWeight());
            iMinEnrollmentWeight = Math.min(iMinEnrollmentWeight, enrollment.getRequest().getWeight());
        }
        iEnrollments.add(enrollment);
        iEnrollmentWeight += enrollment.getRequest().getWeight();
    }

    /** Called when an enrollment with this config is unassigned from a request */
    public void unassigned(Enrollment enrollment) {
        iEnrollments.remove(enrollment);
        iEnrollmentWeight -= enrollment.getRequest().getWeight();
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
     */
    public double getEnrollmentWeight(Request excludeRequest) {
        double weight = iEnrollmentWeight;
        if (excludeRequest != null && excludeRequest.getAssignment() != null
                && iEnrollments.contains(excludeRequest.getAssignment()))
            weight -= excludeRequest.getWeight();
        return weight;
    }
    
    /** Set of assigned enrollments */
    public Set<Enrollment> getEnrollments() {
        return iEnrollments;
    }

    /**
     * Maximal weight of a single enrollment in the config
     */
    public double getMaxEnrollmentWeight() {
        return iMaxEnrollmentWeight;
    }

    /**
     * Minimal weight of a single enrollment in the config
     */
    public double getMinEnrollmentWeight() {
        return iMinEnrollmentWeight;
    }
    
    /**
     * Available space in the configuration that is not reserved by any config reservation
     * @param excludeRequest excluding given request (if not null)
     **/
    public double getUnreservedSpace(Request excludeRequest) {
        // configuration is unlimited -> there is unreserved space unless there is an unlimited reservation too 
        // (in which case there is no unreserved space)
        if (getLimit() < 0) {
            // exclude reservations that are not directly set on this section
            for (Reservation r: getConfigReservations()) {
                if (r.getLimit() < 0) return 0.0;
            }
            return Double.MAX_VALUE;
        }
        
        double available = getLimit() - getEnrollmentWeight(excludeRequest);
        // exclude reservations that are not directly set on this section
        for (Reservation r: getConfigReservations()) {
            // unlimited reservation -> all the space is reserved
            if (r.getLimit() < 0.0) return 0.0;
            // compute space that can be potentially taken by this reservation
            double reserved = r.getReservedAvailableSpace(excludeRequest);
            // deduct the space from available space
            available -= Math.max(0.0, reserved);
        }
        
        return Math.max(0.0, available);
    }
    
    /**
     * Total space in the configuration that cannot be reserved by any config reservation
     **/
    public double getTotalUnreservedSpace() {
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
                if (r.getLimit() < 0) return 0.0;
            }
            return Double.MAX_VALUE;
        }
        
        // we need to check all reservations linked with this section
        double available = getLimit(), reserved = 0, exclusive = 0;
        Set<Config> configs = new HashSet<Config>();
        reservations: for (Reservation r: getConfigReservations()) {
            // unlimited reservation -> no unreserved space
            if (r.getLimit() < 0) return 0.0;
            for (Config s: r.getConfigs()) {
                if (s.equals(this)) continue;
                if (s.getLimit() < 0) continue reservations;
                if (configs.add(s))
                    available += s.getLimit();
            }
            reserved += r.getLimit();
            if (r.getConfigs().size() == 1)
                exclusive += r.getLimit();
        }
        
        return Math.min(available - reserved, getLimit() - exclusive);
    }
    
    /**
     * Get reservations for this configuration
     */
    public List<Reservation> getReservations() {
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
     */
    public List<Reservation> getConfigReservations() {
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
    public void clearReservationCache() {
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
        return new Long(getId()).hashCode();
    }
}
