package org.cpsolver.studentsct.online.expectations;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;

/**
 * Over-expected criterion interface. Various implementations can be provided.
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2014 Tomas Muller<br>
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
 *          License along with this library; if not see <a href='http://www.gnu.org/licenses'>http://www.gnu.org/licenses</a>.
 * 
 */
public interface OverExpectedCriterion {
    /**
     * Expectation penalty, to be minimized
     * @param assignment current assignment
     * @param section section in question
     * @param request student course request
     * @return expectation penalty (typically 1.0 / number of subparts when over-expected, 0.0 otherwise)
     */
    public double getOverExpected(Assignment<Request, Enrollment> assignment, Section section, Request request);
    
    /**
     * Expected space, for printing purposes
     * @param sectionLimit section limit, see {@link Section#getLimit()}
     * @param expectedSpace expectation, see {@link Section#getSpaceExpected()}
     * @return expected space to be printed (null if there are no expectations)
     */
    public Integer getExpected(int sectionLimit, double expectedSpace);
    
    public static interface HasContext {
        /**
         * Expectation penalty, to be minimized.
         * A variant of the {@link OverExpectedCriterion#getOverExpected(Assignment, Section, Request)} method that can be called from {@link Constraint#computeConflicts(Assignment, Value, Set)}.
         * @param assignment current assignment
         * @param selection selected enrollment question
         * @param value an enrollment to be assigned
         * @param conflicts enrollments that have been already identified as conflicting
         * @return expectation penalty (typically 1.0 / number of subparts when over-expected, 0.0 otherwise)
         */
        public double getOverExpected(Assignment<Request, Enrollment> assignment, Enrollment selection, Enrollment value, Set<Enrollment> conflicts);
        
        /**
         * Expectation penalty, to be minimized
         * @param assignment current assignment
         * @param enrollment current assignment of the student
         * @param index only use enrollments 0 .. index - 1 from the assignment array
         * @param section section in question
         * @param request student course request
         * @return expectation penalty (typically 1.0 / number of subparts when over-expected, 0.0 otherwise)
         */
        public double getOverExpected(Assignment<Request, Enrollment> assignment, Enrollment[] enrollment, int index, Section section, Request request);
    }
}