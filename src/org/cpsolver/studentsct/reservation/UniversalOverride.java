package org.cpsolver.studentsct.reservation;

import org.cpsolver.ifs.util.Query;
import org.cpsolver.studentsct.model.AreaClassificationMajor;
import org.cpsolver.studentsct.model.Instructor;
import org.cpsolver.studentsct.model.Offering;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.StudentGroup;

/**
 * Universal reservation override. A reservation override using a student filter.
 * Student filter contains a boolean expression with the following attributes:
 * area, classification, major, minor, group, accommodation, campus,
 * advisor or student external id, and status. For example:
 * major:M1 or major:M2 would match all students with majors M1 or M2.
 * <br>
 * <br>
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2020 Tomas Muller<br>
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
public class UniversalOverride extends Reservation {
    private double iLimit;
    private String iFilter;
    private boolean iOverride;
    private transient Query iStudentQuery;
    
    /**
     * Reservation priority (lower than individual and group reservations)
     */
    public static final int DEFAULT_PRIORITY = 350;
    /**
     * Reservation override does not need to be used by default
     */
    public static final boolean DEFAULT_MUST_BE_USED = false;
    /**
     * Reservation override cannot be assigned over the limit by default.
     */
    public static final boolean DEFAULT_CAN_ASSIGN_OVER_LIMIT = false;
    /**
     * Overlaps are not allowed for group reservation overrides by default. 
     */
    public static final boolean DEFAULT_ALLOW_OVERLAP = false;

    public UniversalOverride(long id, boolean override, double limit, Offering offering, String filter) {
        super(id, offering, DEFAULT_PRIORITY, DEFAULT_MUST_BE_USED, DEFAULT_CAN_ASSIGN_OVER_LIMIT, DEFAULT_ALLOW_OVERLAP);
        iOverride = override;
        iLimit = limit;
        iFilter = filter;
    }
    
    @Override
    /**
     * Override reservation ignore expiration date when checking if they must be used.
     */
    public boolean mustBeUsed() {
        if (iOverride) return mustBeUsedIgnoreExpiration();
        return super.mustBeUsed();
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

    public boolean isOverride() {
        return iOverride;
    }
    
    /**
     * Student filter
     * @return student filter
     */
    public String getFilter() {
        return iFilter;
    }

    /**
     * Check the area, classifications and majors
     */
    @Override
    public boolean isApplicable(Student student) {
        return iFilter != null && !iFilter.isEmpty() && getStudentQuery().match(new StudentMatcher(student));
    }
    
    public Query getStudentQuery() {
        if (iStudentQuery == null)
            iStudentQuery = new Query(iFilter);
        return iStudentQuery;
    }
    
    /**
     * Student matcher matching the student's area, classification, major, minor, group, accommodation, campus,
     * advisor or student external id, or status.
     */
    public static class StudentMatcher implements Query.TermMatcher {
        private Student iStudent;
        
        public StudentMatcher(Student student) {
                iStudent = student;
        }

        public Student student() { return iStudent; }
        
        @Override
        public boolean match(String attr, String term) {
                if (attr == null && term.isEmpty()) return true;
                if ("limit".equals(attr)) return true;
                if ("area".equals(attr)) {
                    for (AreaClassificationMajor acm: student().getAreaClassificationMajors())
                        if (like(acm.getArea(), term)) return true;
                } else if ("clasf".equals(attr) || "classification".equals(attr)) {
                    for (AreaClassificationMajor acm: student().getAreaClassificationMajors())
                        if (like(acm.getClassification(), term)) return true;
                } else if ("campus".equals(attr)) {
                    for (AreaClassificationMajor acm: student().getAreaClassificationMajors())
                        if (like(acm.getCampus(), term)) return true;
                } else if ("major".equals(attr)) {
                    for (AreaClassificationMajor acm: student().getAreaClassificationMajors())
                        if (like(acm.getMajor(), term)) return true;
                } else if ("group".equals(attr)) {
                    for (StudentGroup aac: student().getGroups())
                        if (like(aac.getReference(), term)) return true;
                } else if ("accommodation".equals(attr)) {
                    for (String aac: student().getAccommodations())
                        if (like(aac, term)) return true;
                } else if  ("student".equals(attr)) {
                    return has(student().getName(), term) || eq(student().getExternalId(), term) || eq(student().getName(), term);
                } else if  ("advisor".equals(attr)) {
                    for (Instructor a: student().getAdvisors())
                        if (eq(a.getExternalId(), term)) return true;
                    return false;
                } else if ("minor".equals(attr)) {
                    for (AreaClassificationMajor acm: student().getAreaClassificationMinors())
                        if (like(acm.getMajor(), term)) return true;
                } else if ("status".equals(attr)) {
                    if ("default".equalsIgnoreCase(term) || "Not Set".equalsIgnoreCase(term)) return student().getStatus() == null;
                    return like(student().getStatus(), term);
                } else if ("concentration".equals(attr)) {
                    for (AreaClassificationMajor acm: student().getAreaClassificationMajors())
                        if (like(acm.getConcentration(), term)) return true;
                } else if ("degree".equals(attr)) {
                    for (AreaClassificationMajor acm: student().getAreaClassificationMajors())
                        if (like(acm.getDegree(), term)) return true;
                } else if ("program".equals(attr)) {
                    for (AreaClassificationMajor acm: student().getAreaClassificationMajors())
                        if (like(acm.getProgram(), term)) return true;
                } else if ("primary-area".equals(attr)) {
                    AreaClassificationMajor acm = student().getPrimaryMajor();
                    if (acm != null && like(acm.getArea(), term)) return true;
                } else if ("primary-clasf".equals(attr) || "primary-classification".equals(attr)) {
                    AreaClassificationMajor acm = student().getPrimaryMajor();
                    if (acm != null && like(acm.getClassification(), term)) return true;
                } else if ("primary-major".equals(attr)) {
                    AreaClassificationMajor acm = student().getPrimaryMajor();
                    if (acm != null && like(acm.getMajor(), term)) return true;
                } else if ("primary-concentration".equals(attr)) {
                    AreaClassificationMajor acm = student().getPrimaryMajor();
                    if (acm != null && like(acm.getConcentration(), term)) return true;
                } else if ("primary-degree".equals(attr)) {
                    AreaClassificationMajor acm = student().getPrimaryMajor();
                    if (acm != null && like(acm.getDegree(), term)) return true;
                } else if ("primary-program".equals(attr)) {
                    AreaClassificationMajor acm = student().getPrimaryMajor();
                    if (acm != null && like(acm.getProgram(), term)) return true;
                } else if ("primary-campus".equals(attr)) {
                    AreaClassificationMajor acm = student().getPrimaryMajor();
                    if (acm != null && like(acm.getCampus(), term)) return true;
                } else if (attr != null) {
                    for (StudentGroup aac: student().getGroups())
                        if (eq(aac.getType(), attr.replace('_', ' ')) && like(aac.getReference(), term)) return true;
                }
                return false;
        }
        
        private boolean eq(String name, String term) {
            if (name == null) return false;
            return name.equalsIgnoreCase(term);
        }

        private boolean has(String name, String term) {
            if (name == null) return false;
            if (eq(name, term)) return true;
            for (String t: name.split(" |,"))
                if (t.equalsIgnoreCase(term)) return true;
            return false;
        }
        
        private boolean like(String name, String term) {
            if (name == null) return false;
            if (term.indexOf('%') >= 0) {
                return name.matches("(?i)" + term.replaceAll("%", ".*"));
            } else {
                return name.equalsIgnoreCase(term);
            }
        }
}
}
