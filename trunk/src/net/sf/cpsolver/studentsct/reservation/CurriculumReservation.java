package net.sf.cpsolver.studentsct.reservation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.studentsct.model.AcademicAreaCode;
import net.sf.cpsolver.studentsct.model.Offering;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * Curriculum reservation. Students are matched based on their academic area.
 * If classifications and/or majors are included, student must match on them as well.  
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
public class CurriculumReservation extends Reservation {
    private double iLimit;
    private String iAcadArea;
    private Set<String> iClassifications = new HashSet<String>();
    private Set<String> iMajors = new HashSet<String>();
    
    /**
     * Constructor
     * @param id unique id
     * @param limit reservation limit (-1 for unlimited)
     * @param offering instructional offering on which the reservation is set
     * @param acadArea academic area
     * @param classifications zero or more classifications (classifications must match if not empty)
     * @param majors zero or more majors (majors must match if not empty)
     */
    public CurriculumReservation(long id, double limit, Offering offering, String acadArea, Collection<String> classifications, Collection<String> majors) {
        super(id, offering);
        iLimit = limit;
        iAcadArea = acadArea;
        if (classifications != null)
            iClassifications.addAll(classifications);
        if (majors != null)
            iMajors.addAll(majors);
    }

    /**
     * Curriculum reservation cannot go over the limit
     */
    @Override
    public boolean canAssignOverLimit() {
        return false;
    }

    /**
     * Reservation limit (-1 for unlimited)
     */
    @Override
    public double getLimit() {
        return iLimit;
    }

    /**
     * Set reservation limit (-1 for unlimited)
     */
    public void setLimit(double limit) {
        iLimit = limit;
    }

    /**
     * Reservation priority (lower than individual and group reservations)
     */
    @Override
    public int getPriority() {
        return 3;
    }
    
    /**
     * Academic area
     */
    public String getAcademicArea() {
        return iAcadArea;
    }
    
    /**
     * Majors
     */
    public Set<String> getMajors() {
        return iMajors;
    }
    
    /**
     * Academic classifications
     */
    public Set<String> getClassifications() {
        return iClassifications;
    }

    /**
     * Check the area, classifications and majors
     */
    @Override
    public boolean isApplicable(Student student) {
        boolean match = false;
        if (student.getAcademicAreaClasiffications() == null) return false;
        for (AcademicAreaCode aac: student.getAcademicAreaClasiffications()) {
            if (getAcademicArea().equals(aac.getArea())) {
                if (getClassifications().isEmpty() || getClassifications().contains(aac.getCode())) {
                    match = true; break;
                }
            }
        }
        if (!match) return false;
        for (AcademicAreaCode aac: student.getMajors()) {
            if (getAcademicArea().equals(aac.getArea())) {
                if (getMajors().isEmpty() || getMajors().contains(aac.getCode()))
                    return true;
            }
        }
        return getMajors().isEmpty();
    }
    

}
