package org.cpsolver.studentsct.online.selection;

import java.util.Hashtable;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.FreeTimeRequest;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.online.OnlineSectioningModel;
import org.cpsolver.studentsct.online.expectations.OverExpectedCriterion;
import org.cpsolver.studentsct.online.selection.MultiCriteriaBranchAndBoundSelection.SelectionCriterion;

/**
 * Computation of suggestions using a limited depth branch and bound, using a
 * multi-criteria selection criterion. Everything is the same, but
 * {@link MultiCriteriaBranchAndBoundSelection.SelectionCriterion#compare(Assignment, Enrollment[], Enrollment[])}
 * is used to compare two suggestions.
 * 
 * @see MultiCriteriaBranchAndBoundSelection.SelectionCriterion
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
 *          License along with this library; if not see <a
 *          href='http://www.gnu.org/licenses'>http://www.gnu.org/licenses</a>.
 * 
 */
public class MultiCriteriaBranchAndBoundSuggestions extends SuggestionsBranchAndBound {

    /**
     * Constructor
     * 
     * @param properties
     *            configuration
     * @param student
     *            given student
     * @param assignment
     *            current assignment
     * @param requiredSections
     *            required sections
     * @param requiredFreeTimes
     *            required free times (free time requests that must be assigned)
     * @param preferredSections
     *            preferred sections
     * @param selectedRequest
     *            selected request
     * @param selectedSection
     *            selected section
     * @param filter
     *            section filter
     * @param maxSectionsWithPenalty
     *            maximal number of sections that have a positive
     *            over-expectation penalty
     *            {@link OverExpectedCriterion#getOverExpected(Assignment, Section, Request)}
     */
    public MultiCriteriaBranchAndBoundSuggestions(DataProperties properties, Student student,
            Assignment<Request, Enrollment> assignment, Hashtable<CourseRequest, Set<Section>> requiredSections,
            Set<FreeTimeRequest> requiredFreeTimes, Hashtable<CourseRequest, Set<Section>> preferredSections,
            Request selectedRequest, Section selectedSection, SuggestionFilter filter, double maxSectionsWithPenalty,
            boolean priorityWeighting) {
        super(properties, student, assignment, requiredSections, requiredFreeTimes, preferredSections, selectedRequest,
                selectedSection, filter, maxSectionsWithPenalty);
        if (priorityWeighting)
            iComparator = new OnlineSectioningCriterion(student, (OnlineSectioningModel) selectedRequest.getModel(),
                    assignment, preferredSections);
        else
            iComparator = new EqualWeightCriterion(student, (OnlineSectioningModel) selectedRequest.getModel(),
                    assignment, preferredSections);
    }

    @Override
    protected int compare(Assignment<Request, Enrollment> assignment, Suggestion s1, Suggestion s2) {
        return ((SelectionCriterion) iComparator).compare(assignment, s1.getEnrollments(), s2.getEnrollments());
    }

}
