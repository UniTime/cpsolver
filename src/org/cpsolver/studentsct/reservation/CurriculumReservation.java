package org.cpsolver.studentsct.reservation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cpsolver.studentsct.model.AreaClassificationMajor;
import org.cpsolver.studentsct.model.Offering;
import org.cpsolver.studentsct.model.Student;


/**
 * Curriculum reservation. Students are matched based on their academic area.
 * If classifications and/or majors are included, student must match on them as well.  
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
public class CurriculumReservation extends Reservation {
    private double iLimit;
    private Set<String> iAcadAreas = new HashSet<String>();
    private Set<String> iClassifications = new HashSet<String>();
    private Set<String> iMajors = new HashSet<String>();
    private Set<String> iMinors = new HashSet<String>();
    private Map<String, Set<String>> iConcentrations = null;
    
    /**
     * Reservation priority (lower than individual and group reservations)
     */
    public static final int DEFAULT_PRIORITY = 500;
    /**
     * Curriculum reservation does not need to be used
     */
    public static final boolean DEFAULT_MUST_BE_USED = false;
    /**
     * Curriculum reservations can not assign over the limit.
     */
    public static final boolean DEFAULT_CAN_ASSIGN_OVER_LIMIT = false;
    /**
     * Overlaps are not allowed for curriculum reservations. 
     */
    public static final boolean DEFAULT_ALLOW_OVERLAP = false;
    
    /**
     * Constructor
     * @param id unique id
     * @param limit reservation limit (-1 for unlimited)
     * @param offering instructional offering on which the reservation is set
     * @param acadAreas one or more academic areas
     * @param classifications zero or more classifications (classifications must match if not empty)
     * @param majors zero or more majors (majors must match if not empty)
     * @param minors zero or more majors (minors must match if not empty)
     */
    public CurriculumReservation(long id, double limit, Offering offering, Collection<String> acadAreas, Collection<String> classifications, Collection<String> majors, Collection<String> minors) {
        super(id, offering, DEFAULT_PRIORITY, DEFAULT_MUST_BE_USED, DEFAULT_CAN_ASSIGN_OVER_LIMIT, DEFAULT_ALLOW_OVERLAP);
        iLimit = limit;
        if (acadAreas != null)
            iAcadAreas.addAll(acadAreas);
        if (classifications != null)
            iClassifications.addAll(classifications);
        if (majors != null)
            iMajors.addAll(majors);
        if (minors != null)
            iMinors.addAll(minors);
    }
    
    /**
     * Constructor
     * @param id unique id
     * @param limit reservation limit (-1 for unlimited)
     * @param offering instructional offering on which the reservation is set
     * @param acadArea academic area
     * @param classifications zero or more classifications (classifications must match if not empty)
     * @param majors zero or more majors (majors must match if not empty)
     */
    @Deprecated
    public CurriculumReservation(long id, double limit, Offering offering, String acadArea, Collection<String> classifications, Collection<String> majors) {
        super(id, offering, DEFAULT_PRIORITY, DEFAULT_MUST_BE_USED, DEFAULT_CAN_ASSIGN_OVER_LIMIT, DEFAULT_ALLOW_OVERLAP);
        iLimit = limit;
        iAcadAreas.add(acadArea);
        if (classifications != null)
            iClassifications.addAll(classifications);
        if (majors != null)
            iMajors.addAll(majors);
    }
    
    /**
     * Constructor
     * @param id unique id
     * @param limit reservation limit (-1 for unlimited)
     * @param offering instructional offering on which the reservation is set
     * @param acadAreas one or more academic areas
     * @param classifications zero or more classifications (classifications must match if not empty)
     * @param majors zero or more majors (majors must match if not empty)
     * @param minors zero or more majors (minors must match if not empty)
     * @param priority reservation priority
     * @param mustBeUsed must this reservation be used
     * @param canAssignOverLimit can assign over class / configuration / course limit
     * @param allowOverlap does this reservation allow for overlaps
     */
    protected CurriculumReservation(long id, double limit, Offering offering, Collection<String> acadAreas, Collection<String> classifications, Collection<String> majors, Collection<String> minors,
            int priority, boolean mustBeUsed, boolean canAssignOverLimit, boolean allowOverlap) {
        super(id, offering, priority, mustBeUsed, canAssignOverLimit, allowOverlap);
        iLimit = limit;
        if (acadAreas != null)
            iAcadAreas.addAll(acadAreas);
        if (classifications != null)
            iClassifications.addAll(classifications);
        if (majors != null)
            iMajors.addAll(majors);
        if (minors != null)
            iMinors.addAll(minors);
    }
    
    /**
     * Constructor
     * @param id unique id
     * @param limit reservation limit (-1 for unlimited)
     * @param offering instructional offering on which the reservation is set
     * @param acadArea academic area
     * @param classifications zero or more classifications (classifications must match if not empty)
     * @param majors zero or more majors (majors must match if not empty)
     * @param priority reservation priority
     * @param mustBeUsed must this reservation be used
     * @param canAssignOverLimit can assign over class / configuration / course limit
     * @param allowOverlap does this reservation allow for overlaps
     */
    @Deprecated
    protected CurriculumReservation(long id, double limit, Offering offering, String acadArea, Collection<String> classifications, Collection<String> majors,
            int priority, boolean mustBeUsed, boolean canAssignOverLimit, boolean allowOverlap) {
        super(id, offering, priority, mustBeUsed, canAssignOverLimit, allowOverlap);
        iLimit = limit;
        iAcadAreas.add(acadArea);
        if (classifications != null)
            iClassifications.addAll(classifications);
        if (majors != null)
            iMajors.addAll(majors);
    }

    /**
     * Reservation limit (-1 for unlimited)
     */
    @Override
    public double getReservationLimit() {
        return iLimit;
    }

    /**
     * Set reservation limit (-1 for unlimited)
     * @param limit reservation limit, -1 for unlimited
     */
    public void setReservationLimit(double limit) {
        iLimit = limit;
    }

    
    /**
     * Academic areas
     * @return selected academic areas
     */
    public Set<String> getAcademicAreas() {
        return iAcadAreas;
    }
    
    /**
     * Academic area
     * @return selected academic area
     */
    @Deprecated
    public String getAcademicArea() {
        if (getAcademicAreas().isEmpty()) return "";
        return getAcademicAreas().iterator().next();
    }
    
    /**
     * Majors
     * @return selected majors
     */
    public Set<String> getMajors() {
        return iMajors;
    }
    
    /**
     * Minors
     * @return selected minors
     */
    public Set<String> getMinors() {
        return iMinors;
    }
    
    /**
     * Academic classifications
     * @return selected academic classifications
     */
    public Set<String> getClassifications() {
        return iClassifications;
    }
    
    /** Concentrations for major */
    public Set<String> getConcentrations(String major) {
        return (iConcentrations == null ? null : iConcentrations.get(major));
    }
    
    /** Add concentration for major */
    public void addConcentration(String major, String concentration) {
        if (iConcentrations == null) iConcentrations = new HashMap<String, Set<String>>();
        Set<String> concentrations = iConcentrations.get(major);
        if (concentrations == null) {
            concentrations = new HashSet<String>();
            iConcentrations.put(major, concentrations);
        }
        concentrations.add(concentration);
    }

    /**
     * Check the area, classifications and majors
     */
    @Override
    public boolean isApplicable(Student student) {
        if (!getMajors().isEmpty() || getMinors().isEmpty())
            for (AreaClassificationMajor acm: student.getAreaClassificationMajors())
                if (getAcademicAreas().contains(acm.getArea()) &&
                    (getClassifications().isEmpty() || getClassifications().contains(acm.getClassification())) &&
                    (getMajors().isEmpty() || getMajors().contains(acm.getMajor()))) {
                    Set<String> conc = getConcentrations(acm.getMajor());
                    if (conc != null && !conc.isEmpty()) {
                        return acm.getConcentration() != null && conc.contains(acm.getConcentration());
                    } else {
                        return true;
                    }
                }
        if (!getMinors().isEmpty())
            for (AreaClassificationMajor acm: student.getAreaClassificationMinors())
                if (getAcademicAreas().contains(acm.getArea()) &&
                    (getClassifications().isEmpty() || getClassifications().contains(acm.getClassification())) &&
                    (getMinors().contains(acm.getMajor())))
                        return true;
        return false;
    }
    

}
