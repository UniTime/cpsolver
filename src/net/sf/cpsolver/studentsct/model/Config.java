package net.sf.cpsolver.studentsct.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Representation of a configuration of an offering. A configuration contains
 * id, name, an offering and a list of subparts. <br>
 * <br>
 * Each instructional offering (see {@link Offering}) contains one or more
 * configurations. Each configuration contain one or more subparts. Each student
 * has to take a class of each subpart of one of the possible configurations.
 * Some restrictions might be defined using reservations (see
 * {@link net.sf.cpsolver.studentsct.constraint.Reservation}).
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
    
}
