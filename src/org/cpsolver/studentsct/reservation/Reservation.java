package org.cpsolver.studentsct.reservation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.AssignmentComparable;
import org.cpsolver.ifs.assignment.context.AbstractClassWithContext;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.CanInheritContext;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Offering;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Subpart;



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
public abstract class Reservation extends AbstractClassWithContext<Request, Enrollment, Reservation.ReservationContext>
    implements AssignmentComparable<Reservation, Request, Enrollment>, CanInheritContext<Request, Enrollment, Reservation.ReservationContext> {
    /** Reservation unique id */
    private long iId = 0;
    
    /** Is reservation expired? */
    private boolean iExpired;
    
    /** Instructional offering on which the reservation is set, required */
    private Offering iOffering;

    /** One or more configurations, if applicable */ 
    private Set<Config> iConfigs = new HashSet<Config>();
    
    /** One or more sections, if applicable */
    private Map<Subpart, Set<Section>> iSections = new HashMap<Subpart, Set<Section>>();
    
    /** Reservation priority */
    private int iPriority = 100;
    
    /** Must this reservation be used */
    private boolean iMustBeUsed = false;
    
    /** Can assign over class / configuration / course limit */
    private boolean iCanAssignOverLimit = false;
    
    /** Does this reservation allow for overlaps */
    private boolean iAllowOverlap = false;
    
    /** Does this reservation allow for disabled sections */
    private boolean iAllowDisabled = false;
    
    /** No enrollment is matching this reservation when set to true */
    private boolean iNeverIncluded = false;
    
    /** Can break linked-sections constraint */
    private boolean iBreakLinkedSections = false;

    
    /**
     * Constructor
     * @param id reservation unique id
     * @param offering instructional offering on which the reservation is set
     * @param priority reservation priority
     * @param mustBeUsed must this reservation be used
     * @param canAssignOverLimit can assign over class / configuration / course limit
     * @param allowOverlap does this reservation allow for overlaps
     */
    public Reservation(long id, Offering offering, int priority, boolean mustBeUsed, boolean canAssignOverLimit, boolean allowOverlap) {
        iId = id;
        iOffering = offering;
        iOffering.getReservations().add(this);
        iOffering.clearReservationCache();
        iPriority = priority;
        iMustBeUsed = mustBeUsed;
        iCanAssignOverLimit = canAssignOverLimit;
        iAllowOverlap = allowOverlap;
    }
    
    /**
     * Reservation  id
     * @return reservation unique id
     */
    public long getId() { return iId; }
    
    /**
     * Reservation limit
     * @return reservation limit, -1 for unlimited
     */
    public abstract double getReservationLimit();
    
    
    /** Reservation priority (e.g., individual reservations first) 
     * @return reservation priority
     **/
    public int getPriority() {
        return iPriority;
    }
    
    /**
     * Set reservation priority (e.g., individual reservations first) 
     * @param priority reservation priority
     */
    public void setPriority(int priority) {
        iPriority = priority; 
    }
    
    /**
     * Returns true if the student is applicable for the reservation
     * @param student a student 
     * @return true if student can use the reservation to get into the course / configuration / section
     */
    public abstract boolean isApplicable(Student student);

    /**
     * Instructional offering on which the reservation is set.
     * @return instructional offering
     */
    public Offering getOffering() { return iOffering; }
    
    /**
     * One or more configurations on which the reservation is set (optional).
     * @return instructional offering configurations
     */
    public Set<Config> getConfigs() { return iConfigs; }
    
    /**
     * Add a configuration (of the offering {@link Reservation#getOffering()}) to this reservation
     * @param config instructional offering configuration
     */
    public void addConfig(Config config) {
        iConfigs.add(config);
        clearLimitCapCache();
    }
    
    /**
     * One or more sections on which the reservation is set (optional).
     * @return class restrictions
     */
    public Map<Subpart, Set<Section>> getSections() { return iSections; }
    
    /**
     * One or more sections on which the reservation is set (optional).
     * @param subpart scheduling subpart
     * @return class restrictions for the given scheduling subpart
     */
    public Set<Section> getSections(Subpart subpart) {
        return iSections.get(subpart);
    }
    
    /**
     * Add a section (of the offering {@link Reservation#getOffering()}) to this reservation.
     * This will also add all parent sections and the appropriate configuration to the offering.
     * @param section a class restriction
     */
    public void addSection(Section section, boolean inclusive) {
        if (inclusive) {
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
        } else {
            Set<Section> sections = iSections.get(section.getSubpart());
            if (sections == null) {
                sections = new HashSet<Section>();
                iSections.put(section.getSubpart(), sections);
            }
            sections.add(section);
        }
        clearLimitCapCache();
    }
    
    public void addSection(Section section) {
        addSection(section, true);
    }
    
    /**
     * Return true if the given enrollment meets the reservation.
     * @param enrollment given enrollment
     * @return true if the given enrollment meets the reservation
     */
    public boolean isIncluded(Enrollment enrollment) {
        // Never included flag is set -- return false
        if (neverIncluded()) return false;
        
        // Free time request are never included
        if (enrollment.getConfig() == null) return false;
        
        // Check the offering
        if (!iOffering.equals(enrollment.getConfig().getOffering())) return false;
        
        if (areRestrictionsInclusive()) {
            // If there are configurations, check the configuration
            if (!iConfigs.isEmpty() && !iConfigs.contains(enrollment.getConfig())) return false;
            
            // Check all the sections of the enrollment
            for (Section section: enrollment.getSections()) {
                Set<Section> sections = iSections.get(section.getSubpart());
                if (sections != null && !sections.contains(section))
                    return false;
            }
            return true;
        } else {
            // no restrictions -> true
            if (iConfigs.isEmpty() && iSections.isEmpty()) return true;
            
            // configuration match -> true
            if (iConfigs.contains(enrollment.getConfig())) return true;

            // section match -> true
            for (Section section: enrollment.getSections()) {
                Set<Section> sections = iSections.get(section.getSubpart());
                if (sections != null && sections.contains(section))
                    return true;
            }
            
            // no match -> false
            return false;
        }
    }
    
    /**
     * True if the enrollment can be done using this reservation
     * @param assignment current assignment
     * @param enrollment given enrollment
     * @return true if the given enrollment can be assigned
     */
    public boolean canEnroll(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        // Check if student can use this reservation
        if (!isApplicable(enrollment.getStudent())) return false;
        
        // Check if the enrollment meets the reservation
        if (!isIncluded(enrollment)) return false;

        // Check the limit
        return getLimit(enrollment.getConfig()) < 0 || getContext(assignment).getUsedSpace() + enrollment.getRequest().getWeight() <= getLimit(enrollment.getConfig());
    }
    
    /**
     * True if can go over the course / config / section limit. Only to be used in the online sectioning. 
     * @return can assign over class / configuration / course limit
      */
    public boolean canAssignOverLimit() {
        return iCanAssignOverLimit;
    }
    
    /**
     * True if the batch solver can assign the reservation over the course / config / section limit.
     * @return {@link Reservation#canAssignOverLimit()} and {@link StudentSectioningModel#getReservationCanAssignOverTheLimit()}
     */
    public boolean canBatchAssignOverLimit() {
        return canAssignOverLimit() && (iOffering.getModel() == null || ((StudentSectioningModel)iOffering.getModel()).getReservationCanAssignOverTheLimit());
    }
    
    /**
     * Set to true if a student meeting this reservation can go over the course / config / section limit.
     * @param canAssignOverLimit can assign over class / configuration / course limit
     */
    public void setCanAssignOverLimit(boolean canAssignOverLimit) {
        iCanAssignOverLimit = canAssignOverLimit;
    }
    
    /**
     * If true, student must use the reservation (if applicable). Expired reservations do not need to be used. 
     * @return must this reservation be used
     */
    public boolean mustBeUsed() {
        return iMustBeUsed && !isExpired();
    }
    
    /**
     * If true, student must use the reservation (if applicable). Expiration date is ignored. 
     * @return must this reservation be used
     */
    public boolean mustBeUsedIgnoreExpiration() {
        return iMustBeUsed;
    }
    
    /**
     * Set to true if the student must use the reservation (if applicable)
     * @param mustBeUsed must this reservation be used
     */
    public void setMustBeUsed(boolean mustBeUsed) {
        iMustBeUsed = mustBeUsed;
    }
    
    /**
     * Reservation restrictivity (estimated percentage of enrollments that include this reservation, 1.0 reservation on the whole offering)
     * @return computed restrictivity
     */
    public double getRestrictivity() {
        if (iCachedRestrictivity == null) {
            boolean inclusive = areRestrictionsInclusive();
            if (getConfigs().isEmpty() && getSections().isEmpty()) return 1.0;
            int nrChoices = 0, nrMatchingChoices = 0;
            for (Config config: getOffering().getConfigs()) {
                int x[] = nrChoices(config, 0, new HashSet<Section>(), getConfigs().contains(config), inclusive);
                nrChoices += x[0];
                nrMatchingChoices += x[1];
            }
            iCachedRestrictivity = ((double)nrMatchingChoices) / nrChoices;
        }
        return iCachedRestrictivity;
    }
    private Double iCachedRestrictivity = null;
    
    
    /** Number of choices and number of chaing choices in the given sub enrollment */
    private int[] nrChoices(Config config, int idx, HashSet<Section> sections, boolean matching, boolean inclusive) {
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
                boolean m = (inclusive
                        ? matching && (matchingSections == null || matchingSections.contains(section))
                        : matching || (matchingSections != null && matchingSections.contains(section))
                       );
                int[] x = nrChoices(config, 1 + idx, sections, m, inclusive);
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
    public int compareTo(Assignment<Request, Enrollment> assignment, Reservation r) {
        if (mustBeUsed() != r.mustBeUsed()) {
            return (mustBeUsed() ? -1 : 1);
        }
        if (getPriority() != r.getPriority()) {
            return (getPriority() < r.getPriority() ? -1 : 1);
        }
        int cmp = Double.compare(getRestrictivity(), r.getRestrictivity());
        if (cmp != 0) return cmp;
        cmp = - Double.compare(getContext(assignment).getReservedAvailableSpace(assignment, null), r.getContext(assignment).getReservedAvailableSpace(assignment, null));
        if (cmp != 0) return cmp;
        return Long.valueOf(getId()).compareTo(r.getId());
    }
    
    /**
     * Priority first, than restrictivity (more restrictive first), than id 
     */
    @Override
    public int compareTo(Reservation r) {
        if (mustBeUsed() != r.mustBeUsed()) {
            return (mustBeUsed() ? -1 : 1);
        }
        if (getPriority() != r.getPriority()) {
            return (getPriority() < r.getPriority() ? -1 : 1);
        }
        int cmp = Double.compare(getRestrictivity(), r.getRestrictivity());
        if (cmp != 0) return cmp;
        return Long.valueOf(getId()).compareTo(r.getId());
    }
    
    /**
     * Return minimum of two limits where -1 counts as unlimited (any limit is smaller)
     */
    private static double min(double l1, double l2) {
        return (l1 < 0 ? l2 : l2 < 0 ? l1 : Math.min(l1, l2));
    }
    
    /**
     * Add two limits where -1 counts as unlimited (unlimited plus anything is unlimited)
     */
    private static double add(double l1, double l2) {
        return (l1 < 0 ? -1 : l2 < 0 ? -1 : l1 + l2);
    }
    

    /** Limit cap cache */
    private Double iLimitCap = null;
    private Map<Long, Double> iConfigLimitCap = null;

    /**
     * Compute limit cap (maximum number of students that can get into the offering using this reservation)
     * @return reservation limit cap
     */
    public double getLimitCap() {
        if (iLimitCap == null) iLimitCap = getLimitCapNoCache();
        return iLimitCap;
    }
    
    /**
     * Check if restrictions are inclusive (that is for each section, the reservation also contains all its parents and the configuration)
     */
    public boolean areRestrictionsInclusive() {
        for (Map.Entry<Subpart, Set<Section>> entry: getSections().entrySet()) {
            if (getConfigs().contains(entry.getKey().getConfig())) return true;
        }
        return false;
    }

    /**
     * Compute limit cap (maximum number of students that can get into the offering using this reservation)
     */
    private double getLimitCapNoCache() {
        if (getConfigs().isEmpty() && getSections().isEmpty()) return -1; // no config -> can be unlimited
        
        if (canAssignOverLimit()) return -1; // can assign over limit -> no cap
        
        double cap = 0;
        if (areRestrictionsInclusive()) {
            // for each config
            for (Config config: getConfigs()) {
                // config cap
                double configCap = config.getLimit();
            
                for (Map.Entry<Subpart, Set<Section>> entry: getSections().entrySet()) {
                    if (!config.equals(entry.getKey().getConfig())) continue;
                    Set<Section> sections = entry.getValue();
                    
                    // subpart cap
                    double subpartCap = 0;
                    for (Section section: sections)
                        subpartCap = add(subpartCap, section.getLimit());
            
                    // minimize
                    configCap = min(configCap, subpartCap);
                }
                
                // add config cap
                cap = add(cap, configCap);
            }
        } else {
            // for each config
            for (Config config: getConfigs())
               cap = add(cap, config.getLimit());
            // for each subpart
            for (Map.Entry<Subpart, Set<Section>> entry: getSections().entrySet()) {
                Set<Section> sections = entry.getValue();
                // subpart cap
                double subpartCap = 0;
                for (Section section: sections)
                    subpartCap = add(subpartCap, section.getLimit());
                cap = add(cap, subpartCap);
            }
        }
        
        return cap;
    }
    
    /**
     * Compute limit cap (maximum number of students that can get into the offering using this reservation) for a particular configuration
     * @return reservation limit cap
     */
    public double getLimitCap(Config config) {
        Double cap = (iConfigLimitCap == null ? null : iConfigLimitCap.get(config.getId()));
        if (cap == null) {
            cap = getLimitCapNoCache(config);
            if (iConfigLimitCap == null) iConfigLimitCap = new HashMap<Long, Double>();
            iConfigLimitCap.put(config.getId(), cap);
        }
        return cap;
    }
    
    private double getLimitCapNoCache(Config config) {
        if (getConfigs().isEmpty() && getSections().isEmpty()) return -1; // no config -> can be unlimited
        
        if (canAssignOverLimit()) return -1; // can assign over limit -> no cap

        if (areRestrictionsInclusive()) {
            // no restrictions for this configuration -> no limit
            if (!getConfigs().contains(config)) return 0;
            
            // config cap
            double configCap = config.getLimit();
        
            for (Map.Entry<Subpart, Set<Section>> entry: getSections().entrySet()) {
                if (!config.equals(entry.getKey().getConfig())) continue;
                Set<Section> sections = entry.getValue();
                
                // subpart cap
                double subpartCap = 0;
                for (Section section: sections)
                    subpartCap = add(subpartCap, section.getLimit());
        
                // minimize
                configCap = min(configCap, subpartCap);
            }
            
            // add config cap
            return configCap;
        } else {
            double cap = 0;
            
            // config cap
            if (getConfigs().contains(config))
                cap = add(cap, config.getLimit());
            
            // for each subpart
            for (Map.Entry<Subpart, Set<Section>> entry: getSections().entrySet()) {
                if (!config.equals(entry.getKey().getConfig())) continue;
                Set<Section> sections = entry.getValue();

                // subpart cap
                double subpartCap = 0;
                for (Section section: sections)
                    subpartCap = add(subpartCap, section.getLimit());
                cap = add(cap, subpartCap);
            }
            
            return cap;
        }
        
    }
    
    /**
     * Clear limit cap cache
     */
    private void clearLimitCapCache() {
        iLimitCap = null;
        if (iConfigLimitCap != null) iConfigLimitCap.clear();
    }
    
    /**
     * Reservation limit capped the limit cap (see {@link Reservation#getLimitCap()})
     * @return reservation limit, -1 if unlimited
     */
    public double getLimit() {
        return min(getLimitCap(), getReservationLimit());
    }
    
    /**
     * Reservation limit capped the limit cap (see {@link Reservation#getLimitCap(Config)}) for a particular configuration
     * @param config configuration for which the limit is computed (restrictions on other configurations are ignored)
     * @return reservation limit, -1 if unlimited
     */
    public double getLimit(Config config) {
        return min(getLimitCap(config), getReservationLimit());
    }
    
    /**
     * True if holding this reservation allows a student to have attend overlapping class. 
     * @return does this reservation allow for overlaps
     */
    public boolean isAllowOverlap() {
        return iAllowOverlap;
    }
    
    /**
     * Set to true if holding this reservation allows a student to have attend overlapping class.
     * @param allowOverlap does this reservation allow for overlaps
     */
    public void setAllowOverlap(boolean allowOverlap) {
        iAllowOverlap = allowOverlap;
    }
    
    /**
     * True if holding this reservation allows a student to attend a disabled class. 
     * @return does this reservation allow for disabled sections
     */
    public boolean isAllowDisabled() {
        return iAllowDisabled;
    }
    
    /**
     * Set to true if holding this reservation allows a student to attend a disabled class
     * @param allowDisabled does this reservation allow for disabled sections
     */
    public void setAllowDisabled(boolean allowDisabled) {
        iAllowDisabled = allowDisabled;
    }
    
    /**
     * Set reservation expiration. If a reservation is expired, it works as ordinary reservation
     * (especially the flags mutBeUsed and isAllowOverlap), except it does not block other students
     * of getting into the offering / config / section.  
     * @param expired is this reservation expired
     */
    public void setExpired(boolean expired) {
        iExpired = expired;
    }
    
    /**
     * True if the reservation is expired. If a reservation is expired, it works as ordinary reservation
     * (especially the flags mutBeUsed and isAllowOverlap), except it does not block other students
     * of getting into the offering / config / section.
     * @return is this reservation expired
     */
    public boolean isExpired() {
        return iExpired;
    }
    
    /**
     * No enrollment is matching this reservation when set to true
     */
    public boolean neverIncluded() { return iNeverIncluded; }
    
    /**
     * No enrollment is matching this reservation when set to true
     */
    public void setNeverIncluded(boolean neverIncluded) { iNeverIncluded = neverIncluded; }
    
    /**
     * Can break linked-section constraints when set to true
     */
    public boolean canBreakLinkedSections() { return iBreakLinkedSections; }
    
    /**
     * Can break linked-section constraints when set to true
     */
    public void setBreakLinkedSections(boolean breakLinkedSections) { iBreakLinkedSections = breakLinkedSections; }
    
    
    @Override
    public Model<Request, Enrollment> getModel() {
        return getOffering().getModel();
    }
    
    /**
     * Available reserved space
     * @param assignment current assignment
     * @param excludeRequest excluding given request (if not null)
     * @return available reserved space
     **/
    public double getReservedAvailableSpace(Assignment<Request, Enrollment> assignment, Request excludeRequest) {
        return getContext(assignment).getReservedAvailableSpace(assignment, excludeRequest);
    }
    
    /**
     * Available reserved space for a particular config
     * @param assignment current assignment
     * @param excludeRequest excluding given request (if not null)
     * @return available reserved space
     **/
    public double getReservedAvailableSpace(Assignment<Request, Enrollment> assignment, Config config, Request excludeRequest) {
        return getContext(assignment).getReservedAvailableSpace(assignment, config, excludeRequest);
    }
    
    /** Enrollments assigned using this reservation 
     * @param assignment current assignment
     * @return assigned enrollments of this reservation
     **/
    public Set<Enrollment> getEnrollments(Assignment<Request, Enrollment> assignment) {
        return getContext(assignment).getEnrollments();
    }

    @Override
    public ReservationContext createAssignmentContext(Assignment<Request, Enrollment> assignment) {
        return new ReservationContext(assignment);
    }
    

    @Override
    public ReservationContext inheritAssignmentContext(Assignment<Request, Enrollment> assignment, ReservationContext parentContext) {
        return new ReservationContext(parentContext);
    }

    
    public class ReservationContext implements AssignmentConstraintContext<Request, Enrollment> {
        /** Enrollments included in this reservation */
        private Set<Enrollment> iEnrollments = new HashSet<Enrollment>();
        
        /** Used part of the limit */
        private double iUsed = 0;
        private Map<Long, Double> iUsedByConfig = new HashMap<Long, Double>();
        private boolean iReadOnly = false;

        public ReservationContext(Assignment<Request, Enrollment> assignment) {
            for (Course course: getOffering().getCourses())
                for (CourseRequest request: course.getRequests()) {
                    Enrollment enrollment = assignment.getValue(request);
                    if (enrollment != null && Reservation.this.equals(enrollment.getReservation()))
                        assigned(assignment, enrollment);
                }
        }
        
        public ReservationContext(ReservationContext parent) {
            iUsed = parent.iUsed;
            iUsedByConfig = new HashMap<Long, Double>(parent.iUsedByConfig);
            iEnrollments = parent.iEnrollments;
            iReadOnly = true;
        }

        /** Notify reservation about an unassignment */
        @Override
        public void assigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
            if (iReadOnly) {
                iEnrollments = new HashSet<Enrollment>(iEnrollments);
                iReadOnly = false;
            }
            if (iEnrollments.add(enrollment)) {
                iUsed += enrollment.getRequest().getWeight();
                Double used = iUsedByConfig.get(enrollment.getConfig().getId());
                iUsedByConfig.put(enrollment.getConfig().getId(), enrollment.getRequest().getWeight() + (used == null ? 0.0 : used.doubleValue()));
            }
        }

        /** Notify reservation about an assignment */
        @Override
        public void unassigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
            if (iReadOnly) {
                iEnrollments = new HashSet<Enrollment>(iEnrollments);
                iReadOnly = false;
            }
            if (iEnrollments.remove(enrollment)) {
                iUsed -= enrollment.getRequest().getWeight();
                Double used = iUsedByConfig.get(enrollment.getConfig().getId());
                iUsedByConfig.put(enrollment.getConfig().getId(), (used == null ? 0.0 : used.doubleValue()) - enrollment.getRequest().getWeight());
            }
        }
        
        /** Enrollments assigned using this reservation 
         * @return assigned enrollments of this reservation
         **/
        public Set<Enrollment> getEnrollments() {
            return iEnrollments;
        }
        
        /** Used space 
         * @return spaced used of this reservation
         **/
        public double getUsedSpace() {
            return iUsed;
        }
        
        /** Used space in a particular config
         * @return spaced used of this reservation
         **/
        public double getUsedSpace(Config config) {
            Double used = iUsedByConfig.get(config.getId());
            return (used == null ? 0.0 : used.doubleValue());
        }
        
        /**
         * Available reserved space
         * @param assignment current assignment
         * @param excludeRequest excluding given request (if not null)
         * @return available reserved space
         **/
        public double getReservedAvailableSpace(Assignment<Request, Enrollment> assignment, Request excludeRequest) {
            // Unlimited
            if (getLimit() < 0) return Double.MAX_VALUE;
            
            double reserved = getLimit() - getContext(assignment).getUsedSpace();
            if (excludeRequest != null && assignment.getValue(excludeRequest) != null && iEnrollments.contains(assignment.getValue(excludeRequest)))
                reserved += excludeRequest.getWeight();
            
            return reserved;
        }
        
        /**
         * Available reserved space for a particular config
         * @param assignment current assignment
         * @param excludeRequest excluding given request (if not null)
         * @return available reserved space
         **/
        public double getReservedAvailableSpace(Assignment<Request, Enrollment> assignment, Config config, Request excludeRequest) {
            if (config == null) return getReservedAvailableSpace(assignment, excludeRequest);
            
            // Unlimited
            if (getLimit(config) < 0) return Double.MAX_VALUE;
            
            double reserved = getLimit(config) - getContext(assignment).getUsedSpace(config);
            if (excludeRequest != null && assignment.getValue(excludeRequest) != null && assignment.getValue(excludeRequest).getConfig().equals(config) && iEnrollments.contains(assignment.getValue(excludeRequest)))
                reserved += excludeRequest.getWeight();
            
            return reserved;
        }
    }
}
