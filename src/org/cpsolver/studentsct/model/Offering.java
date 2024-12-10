package org.cpsolver.studentsct.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.studentsct.reservation.Reservation;
import org.cpsolver.studentsct.reservation.Restriction;



/**
 * Representation of an instructional offering. An offering contains id, name,
 * the list of course offerings, and the list of possible configurations. See
 * {@link Config} and {@link Course}.
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
public class Offering {
    private long iId = -1;
    private String iName = null;
    private Model<Request, Enrollment> iModel = null;
    private List<Config> iConfigs = new ArrayList<Config>();
    private List<Course> iCourses = new ArrayList<Course>();
    private List<Reservation> iReservations = new ArrayList<Reservation>();
    private List<Restriction> iRestrictions = new ArrayList<Restriction>();
    private boolean iDummy = false;

    /**
     * Constructor
     * 
     * @param id
     *            instructional offering unique id
     * @param name
     *            instructional offering name (this is usually the name of the
     *            controlling course)
     */
    public Offering(long id, String name) {
        iId = id;
        iName = name;
    }

    /** Offering id 
     * @return instructional offering unique id
     **/
    public long getId() {
        return iId;
    }

    /** Offering name 
     * @return instructional offering name
     **/
    public String getName() {
        return iName;
    }

    /** Possible configurations 
     * @return instructional offering configurations
     **/
    public List<Config> getConfigs() {
        return iConfigs;
    }

    /**
     * List of courses. One instructional offering can contain multiple courses
     * (names under which it is offered)
     * @return list of course offerings
     */
    public List<Course> getCourses() {
        return iCourses;
    }

    /**
     * Return section of the given id, if it is part of one of this offering
     * configurations.
     * @param sectionId class unique id
     * @return section of the given id
     */
    public Section getSection(long sectionId) {
        for (Config config : getConfigs()) {
            for (Subpart subpart : config.getSubparts()) {
                for (Section section : subpart.getSections()) {
                    if (section.getId() == sectionId)
                        return section;
                }
            }
        }
        return null;
    }

    /** Return course, under which the given student enrolls into this offering. 
     * @param student given student
     * @return course of this offering requested by the student
     **/
    public Course getCourse(Student student) {
        if (getCourses().isEmpty())
            return null;
        if (getCourses().size() == 1)
            return getCourses().get(0);
        for (Request request : student.getRequests()) {
            if (request instanceof CourseRequest) {
                for (Course course : ((CourseRequest) request).getCourses()) {
                    if (getCourses().contains(course))
                        return course;
                }
            }
        }
        return getCourses().get(0);
    }

    /** Return set of instructional types, union over all configurations. 
     * @return set of instructional types
     **/
    public Set<String> getInstructionalTypes() {
        Set<String> instructionalTypes = new HashSet<String>();
        for (Config config : getConfigs()) {
            for (Subpart subpart : config.getSubparts()) {
                instructionalTypes.add(subpart.getInstructionalType());
            }
        }
        return instructionalTypes;
    }

    /**
     * Return the list of all possible choices of the given instructional type
     * for this offering.
     * @param instructionalType instructional type
     * @return set of choices of the given instructional type
     */
    public Set<Choice> getChoices(String instructionalType) {
        Set<Choice> choices = new HashSet<Choice>();
        for (Config config : getConfigs()) {
            for (Subpart subpart : config.getSubparts()) {
                if (!instructionalType.equals(subpart.getInstructionalType()))
                    continue;
                choices.addAll(subpart.getChoices());
            }
        }
        return choices;
    }

    /**
     * Return list of all subparts of the given isntructional type for this
     * offering.
     * @param instructionalType instructional type
     * @return subpart of the given instructional type
     */
    public Set<Subpart> getSubparts(String instructionalType) {
        Set<Subpart> subparts = new HashSet<Subpart>();
        for (Config config : getConfigs()) {
            for (Subpart subpart : config.getSubparts()) {
                if (instructionalType.equals(subpart.getInstructionalType()))
                    subparts.add(subpart);
            }
        }
        return subparts;
    }

    /** Minimal penalty from {@link Config#getMinPenalty()} 
     * @return minimal penalty
     **/
    public double getMinPenalty() {
        double min = Double.MAX_VALUE;
        for (Config config : getConfigs()) {
            min = Math.min(min, config.getMinPenalty());
        }
        return (min == Double.MAX_VALUE ? 0.0 : min);
    }

    /** Maximal penalty from {@link Config#getMaxPenalty()} 
     * @return maximal penalty
     **/
    public double getMaxPenalty() {
        double max = Double.MIN_VALUE;
        for (Config config : getConfigs()) {
            max = Math.max(max, config.getMaxPenalty());
        }
        return (max == Double.MIN_VALUE ? 0.0 : max);
    }

    @Override
    public String toString() {
        return iName;
    }
    
    /** Reservations associated with this offering 
     * @return reservations for this offering
     **/
    public List<Reservation> getReservations() { return iReservations; }
    
    /** True if there are reservations for this offering 
     * @return true if there is at least one reservation
     **/
    public boolean hasReservations() { return !iReservations.isEmpty(); }
    
    /** Restrictions associated with this offering 
     * @return restrictions for this offering
     **/
    public List<Restriction> getRestrictions() { return iRestrictions; }
    
    /** True if there are restrictions for this offering 
     * @return true if there is at least one restriction
     **/
    public boolean hasRestrictions() { return !iRestrictions.isEmpty(); }
    
    /**
     * Total space in the offering that is not reserved by any reservation 
     * @return total unreserved space in the offering
     **/
    public synchronized double getTotalUnreservedSpace() {
        if (iTotalUnreservedSpace == null)
            iTotalUnreservedSpace = getTotalUnreservedSpaceNoCache();
        return iTotalUnreservedSpace;
    }
    Double iTotalUnreservedSpace = null;
    private double getTotalUnreservedSpaceNoCache() {
        // compute overall available space
        double available = 0.0;
        for (Config config: getConfigs()) {
            available += config.getLimit();
            // offering is unlimited -> there is unreserved space unless there is an unlimited reservation too 
            // (in which case there is no unreserved space)
            if (config.getLimit() < 0) {
                for (Reservation r: getReservations()) {
                    // ignore expired reservations
                    if (r.isExpired()) continue;
                    // skip reservations that have restrictions that are not inclusive (these are only checked on the restricted sections/configs)
                    if (!r.areRestrictionsInclusive() && (!r.getConfigs().isEmpty() || !r.getSections().isEmpty())) continue;
                    // there is an unlimited reservation -> no unreserved space
                    if (r.getLimit(config) < 0) return 0.0;
                }
                return Double.MAX_VALUE;
            }
        }
        
        // compute maximal reserved space (out of the available space)
        double reserved = 0;
        for (Reservation r: getReservations()) {
            // ignore expired reservations
            if (r.isExpired()) continue;
            // skip reservations that have restrictions that are not inclusive (these are only checked on the restricted sections/configs)
            if (!r.areRestrictionsInclusive() && (!r.getConfigs().isEmpty() || !r.getSections().isEmpty())) continue;
            // unlimited reservation -> no unreserved space
            if (r.getLimit() < 0) return 0.0;
            reserved += r.getLimit();
        }
        
        return Math.max(0.0, available - reserved);
    }

    /**
     * Available space in the offering that is not reserved by any reservation 
     * @param assignment current request
     * @param excludeRequest excluding given request (if not null)
     * @return remaining unreserved space in the offering
     **/
    public double getUnreservedSpace(Assignment<Request, Enrollment> assignment, Request excludeRequest) {
        // compute available space
        double available = 0.0;
        for (Config config: getConfigs()) {
            available += config.getLimit() - config.getContext(assignment).getEnrollmentWeight(assignment, excludeRequest);
            // offering is unlimited -> there is unreserved space unless there is an unlimited reservation too 
            // (in which case there is no unreserved space)
            if (config.getLimit() < 0) {
                for (Reservation r: getReservations()) {
                    // ignore expired reservations
                    if (r.isExpired()) continue;
                    // skip reservations that have restrictions that are not inclusive (these are only checked on the restricted sections/configs)
                    if (!r.areRestrictionsInclusive() && (!r.getConfigs().isEmpty() || !r.getSections().isEmpty())) continue;
                    // there is an unlimited reservation -> no unreserved space
                    if (r.getLimit(config) < 0) return 0.0;
                }
                return Double.MAX_VALUE;
            }
        }
        
        // compute reserved space (out of the available space)
        double reserved = 0;
        for (Reservation r: getReservations()) {
            // ignore expired reservations
            if (r.isExpired()) continue;
            // skip reservations that have restrictions that are not inclusive (these are only checked on the restricted sections/configs)
            if (!r.areRestrictionsInclusive() && (!r.getConfigs().isEmpty() || !r.getSections().isEmpty())) continue;
            // unlimited reservation -> no unreserved space
            if (r.getLimit() < 0) return 0.0;
            reserved += Math.max(0.0, r.getContext(assignment).getReservedAvailableSpace(assignment, excludeRequest));
        }
        
        return available - reserved;
    }

    
    /**
     * Clear reservation information that was cached on this offering or below
     */
    public synchronized void clearReservationCache() {
        for (Config c: getConfigs())
            c.clearReservationCache();
        for (Course c: getCourses())
            for (CourseRequest r: c.getRequests())
                r.clearReservationCache();
        iTotalUnreservedSpace = null;
    }
    
    /**
     * Clear restriction information that was cached on this offering or below
     */
    public synchronized void clearRestrictionCache() {
        for (Course c: getCourses())
            for (CourseRequest r: c.getRequests())
                r.clearRestrictionCache();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Offering)) return false;
        return getId() == ((Offering)o).getId();
    }
    
    @Override
    public int hashCode() {
        return (int) (iId ^ (iId >>> 32));
    }
    
    public Model<Request, Enrollment> getModel() { return iModel; }
    public void setModel(Model<Request, Enrollment> model) { iModel = model; }
    
    /**
     * Dummy courses that should show on the solver dashboard
     * (e.g., because they are loaded due to an other session unavailability)
     * @return true if the course is "dummy"
     */
    public boolean isDummy() { return iDummy; }
    
    /**
     * Dummy courses that should show on the solver dashboard
     * (e.g., because they are loaded due to an other session unavailability)
    */
    public void setDummy(boolean dummy) { iDummy = dummy; }
}
