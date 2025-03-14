package org.cpsolver.studentsct.reservation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.cpsolver.studentsct.model.Offering;
import org.cpsolver.studentsct.model.Student;

/**
 * Individual restriction. A restriction for a particular student (or students).
 * 
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
public class IndividualRestriction extends Restriction {
    private Set<Long> iStudentIds = new HashSet<Long>();
    
    /**
     * Constructor
     * @param id restriction unique id
     * @param offering instructional offering on which the restriction is set
     * @param studentIds one or more students
     */
    public IndividualRestriction(long id, Offering offering, Long... studentIds) {
        super(id, offering);
        for (Long studentId: studentIds) {
            iStudentIds.add(studentId);
        }
    }

    /**
     * Constructor
     * @param id restriction unique id
     * @param offering instructional offering on which the restriction is set
     * @param studentIds one or more students
     */
    public IndividualRestriction(long id, Offering offering, Collection<Long> studentIds) {
        super(id, offering);
        iStudentIds.addAll(studentIds);
    }

    /**
     * Restriction is applicable for all students in the restriction
     */
    @Override
    public boolean isApplicable(Student student) {
        return iStudentIds.contains(student.getId());
    }
    
    /**
     * Students in the restriction
     * @return set of student ids associated with this restriction
     */
    public Set<Long> getStudentIds() {
        return iStudentIds;
    }
}