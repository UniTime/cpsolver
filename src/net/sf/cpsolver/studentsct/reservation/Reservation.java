package net.sf.cpsolver.studentsct.reservation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.studentsct.model.Config;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Offering;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;
import net.sf.cpsolver.studentsct.model.Subpart;


/**
 * Abstract reservation. This abstract class allow some section, courses,
 * and other parts to be reserved to particular group of students. A reservation
 * can be unlimited (any number of students of that particular group can attend
 * a course, section, etc.) or with a limit (only given number of seats is
 * reserved to the students of the particular group).
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
public abstract class Reservation implements Comparable<Reservation> {
    /** Reservation unique id */
    private long iId = 0;
    
    /** Instructional offering on which the reservation is set, required */
    private Offering iOffering;

    /** One or more configurations, if applicable */ 
    private Set<Config> iConfigs = new HashSet<Config>();
    
    /** One or more sections, if applicable */
    private Map<Subpart, Set<Section>> iSections = new HashMap<Subpart, Set<Section>>();
    
    /** Enrollments included in this reservation */
    private Set<Enrollment> iEnrollments = new HashSet<Enrollment>();
    
    /** Used part of the limit */
    private double iUsed = 0;
    
    /**
     * Constructor
     * @param id reservation unique id
     * @param offering instructional offering on which the reservation is set
     */
    public Reservation(long id, Offering offering) {
        iId = id;
        iOffering = offering;
        iOffering.getReservations().add(this);
        iOffering.clearReservationCache();
    }
    
    /**
     * Reservation  id
     */
    public long getId() { return iId; }
    
    /**
     * Reservation limit
     */
    public abstract double getLimit();
    
    
    /** Reservation priority (e.g., individual reservations first) */
    public abstract int getPriority();
    
    /**
     * Returns true if the student is applicable for the reservation
     * @param student a student 
     * @return true if student can use the reservation to get into the course / configuration / section
     */
    public abstract boolean isApplicable(Student student);

    /**
     * Instructional offering on which the reservation is set.
     */
    public Offering getOffering() { return iOffering; }
    
    /**
     * One or more configurations on which the reservation is set (optional).
     */
    public Set<Config> getConfigs() { return iConfigs; }
    
    /**
     * Add a configuration (of the offering {@link Reservation#getOffering()}) to this reservation
     */
    public void addConfig(Config config) { iConfigs.add(config); }
    
    /**
     * One or more sections on which the reservation is set (optional).
     */
    public Map<Subpart, Set<Section>> getSections() { return iSections; }
    
    /**
     * One or more sections on which the reservation is set (optional).
     */
    public Set<Section> getSections(Subpart subpart) {
        return iSections.get(subpart);
    }
    
    /**
     * Add a section (of the offering {@link Reservation#getOffering()}) to this reservation.
     * This will also add all parent sections and the appropriate configuration to the offering.
     */
    public void addSection(Section section) {
        addConfig(section.getSubpart().getConfig());
        while (section != null) {
            Set<Section> sections = iSections.get(section.getSubpart());
            if (sections == null) {
                sections = new HashSet<Section>();
                iSections.put(section.getSubpart(), sections);
            }
            sections.add(section);
            section = section.getParent();
        }
    }
    
    /**
     * Return true if the given enrollment meets the reservation.
     */
    public boolean isIncluded(Enrollment enrollment) {
        // Free time request are never included
        if (enrollment.getConfig() == null) return false;
        
        // Check the offering
        if (!iOffering.equals(enrollment.getConfig().getOffering())) return false;
        
        // If there are configurations, check the configuration
        if (!iConfigs.isEmpty() && !iConfigs.contains(enrollment.getConfig())) return false;
        
        // Check all the sections of the enrollment
        for (Section section: enrollment.getSections()) {
            Set<Section> sections = iSections.get(section.getSubpart());
            if (sections != null && !sections.contains(section))
                return false;
        }
        
        return true;
    }
    
    /**
     * True if the enrollment can be done using this configuration
     */
    public boolean canEnroll(Enrollment enrollment) {
        // Check if student can use this reservation
        if (!isApplicable(enrollment.getStudent())) return false;
        
        // Check if the enrollment meets the reservation
        if (!isIncluded(enrollment)) return false;

        // Check the limit
        return getLimit() < 0 || getUsedSpace() + enrollment.getRequest().getWeight() <= getLimit();
    }
    
    /** Notify reservation about an unassignment */
    public void assigned(Enrollment enrollment) {
        iEnrollments.add(enrollment);
        iUsed += enrollment.getRequest().getWeight();
    }

    /** Notify reservation about an assignment */
    public void unassigned(Enrollment enrollment) {
        iEnrollments.remove(enrollment);
        iUsed -= enrollment.getRequest().getWeight();
    }
    
    /** Enrollments assigned using this reservation */
    public Set<Enrollment> getEnrollments() {
        return iEnrollments;
    }
    
    /** Used space */
    public double getUsedSpace() {
        return iUsed;
    }

    /**
     * Available reserved space
     * @param excludeRequest excluding given request (if not null)
     **/
    public double getReservedAvailableSpace(Request excludeRequest) {
        // Unlimited
        if (getLimit() < 0) return Float.MAX_VALUE;
        
        double reserved = getLimit() - getUsedSpace();
        if (excludeRequest != null && excludeRequest.getAssignment() != null &&
            iEnrollments.contains(excludeRequest.getAssignment()))
            reserved += excludeRequest.getWeight();
        
        return reserved;
    }
    
    /**
     * True if can go over the course / config / section limit. Only to be used in the online sectioning. 
      */
    public abstract boolean canAssignOverLimit();
    
    /**
     * Reservation restrictivity (estimated percentage of enrollments that include this reservation, 1.0 reservation on the whole offering)
     */
    public double getRestrictivity() {
        if (iCachedRestrictivity == null) {
            if (getConfigs().isEmpty()) return 1.0;
            int nrChoices = 0, nrMatchingChoices = 0;
            for (Config config: getOffering().getConfigs()) {
                int x[] = nrChoices(config, 0, new HashSet<Section>(), getConfigs().contains(config));
                nrChoices += x[0];
                nrMatchingChoices += x[1];
            }
            iCachedRestrictivity = ((double)nrMatchingChoices) / nrChoices;
        }
        return iCachedRestrictivity;
    }
    private Double iCachedRestrictivity = null;
    
    
    /** Number of choices and number of chaing choices in the given sub enrollment */
    private int[] nrChoices(Config config, int idx, HashSet<Section> sections, boolean matching) {
        if (config.getSubparts().size() == idx) {
            return new int[]{1, matching ? 1 : 0};
        } else {
            Subpart subpart = config.getSubparts().get(idx);
            Set<Section> matchingSections = getSections(subpart);
            int choicesThisSubpart = 0;
            int matchingChoicesThisSubpart = 0;
            for (Section section : subpart.getSections()) {
                if (section.getParent() != null && !sections.contains(section.getParent()))
                    continue;
                if (section.isOverlapping(sections))
                    continue;
                sections.add(section);
                boolean m = matching && (matchingSections == null || matchingSections.contains(section));
                int[] x = nrChoices(config, 1 + idx, sections, m);
                choicesThisSubpart += x[0];
                matchingChoicesThisSubpart += x[1];
                sections.remove(section);
            }
            return new int[] {choicesThisSubpart, matchingChoicesThisSubpart};
        }
    }
    
    /**
     * Priority first, than restrictivity (more restrictive first), than availability (more available first), than id 
     */
    @Override
    public int compareTo(Reservation r) {
        if (getPriority() != r.getPriority()) {
            return (getPriority() < r.getPriority() ? -1 : 1);
        }
        int cmp = Double.compare(getRestrictivity(), r.getRestrictivity());
        if (cmp != 0) return cmp;
        cmp = - Double.compare(getReservedAvailableSpace(null), r.getReservedAvailableSpace(null));
        if (cmp != 0) return cmp;
        return new Long(getId()).compareTo(r.getId());
    }
}
