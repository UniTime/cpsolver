package org.cpsolver.studentsct.online.selection;

import java.util.Hashtable;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.studentsct.heuristics.selection.BranchBoundSelection;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.FreeTimeRequest;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.online.OnlineSectioningModel;
import org.cpsolver.studentsct.online.expectations.OverExpectedCriterion;

/**
 * Online student sectioning interface to be implemented by any sectioning algorithm.
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
public interface OnlineSectioningSelection {
    /**
     * Set online sectioning model
     * @param model online sectioning model
     */
    public void setModel(OnlineSectioningModel model);
    
    /**
     * Set preferred sections
     * @param preferredSections preferred sections for each course request
     */
    public void setPreferredSections(Hashtable<CourseRequest, Set<Section>> preferredSections);
    
    /**
     * Set required sections
     * @param requiredSections required sections for each course request
     */
    public void setRequiredSections(Hashtable<CourseRequest, Set<Section>> requiredSections);
    
    /**
     * Set required free times
     * @param requiredFreeTimes required free times
     */
    public void setRequiredFreeTimes(Set<FreeTimeRequest> requiredFreeTimes);
    
    /**
     * Set course requests that are to be left unassigned
     * @param requiredUnassignedRequests course requests that are required to be left unassigned
     */
    public void setRequiredUnassinged(Set<CourseRequest> requiredUnassignedRequests);
    
    /**
     * Compute student schedule
     * @param assignment current assignment
     * @param student student in question
     * @return student schedule
     */
    public BranchBoundSelection.BranchBoundNeighbour select(Assignment<Request, Enrollment> assignment, Student student);
    
    /**
     * Set hard limit on the {@link OverExpectedCriterion} penalty.
     * @param maxOverExpected max over-expected limit
     */
    public void setMaxOverExpected(double maxOverExpected);
}
