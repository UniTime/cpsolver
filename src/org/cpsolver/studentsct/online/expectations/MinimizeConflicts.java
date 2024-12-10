package org.cpsolver.studentsct.online.expectations;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;

/**
 * A class is considered over-expected when it is not available (limit is zero) or when 
 * there is a time conflict with some other enrollment of the student (despite the
 * reservation allowing for the conflict). 
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
public class MinimizeConflicts implements OverExpectedCriterion, OverExpectedCriterion.HasContext {
    private boolean iTimeConflicts = true, iSpaceConflicts = true;
    private OverExpectedCriterion iParent;
    private double iParentWeight = 0.5;
    
    public MinimizeConflicts(DataProperties config) {
        this(config, null);
    }

    public MinimizeConflicts(DataProperties config, OverExpectedCriterion parent) {
        iParent = parent;
        iTimeConflicts = config.getPropertyBoolean("OverExpected.TimeConflicts", iTimeConflicts);
        iSpaceConflicts = config.getPropertyBoolean("OverExpected.SpaceConflicts", iSpaceConflicts);
        iParentWeight = config.getPropertyDouble("OverExpected.ParentWeight", iParentWeight);
    }

    @Override
    public double getOverExpected(Assignment<Request, Enrollment> assignment, Section section, Request request) {
        double penalty = 0.0;
        if (iSpaceConflicts && section.getLimit() == 0) {
            // no space in the section >> avoid
            penalty += 1.0;
        }
        
        if (iTimeConflicts && !section.isAllowOverlap()) {
            for (Request r: request.getStudent().getRequests()) {
                if (request.equals(r)) break;
                Enrollment e = assignment.getValue(r);
                if (e != null && e.isCourseRequest() && section.isOverlapping(e.getAssignments())) {
                    // time conflict with some other already assigned section >> avoid
                    penalty += 1.0;
                }
            }
        }
        
        if (penalty > 0) return penalty / section.getSubpart().getConfig().getSubparts().size();
        return (iParent == null ? 0.0 : iParentWeight * iParent.getOverExpected(assignment, section, request));
    }
    
    @Override
    public Integer getExpected(int sectionLimit, double expectedSpace) {
        return (iParent == null ? null : iParent.getExpected(sectionLimit, expectedSpace)); 
    }

    @Override
    public String toString() {
        return "min-conflict" + (iParent == null ? "" : "/" + iParent);
    }

    @Override
    public double getOverExpected(Assignment<Request, Enrollment> assignment, Enrollment selection, Enrollment value, Set<Enrollment> conflicts) {
        if (selection == null || !selection.isCourseRequest()) return 0.0;
        
        double penalty = 0;
        if (iSpaceConflicts) {
            for (Section section: selection.getSections()) {
                if (section.getLimit() == 0) {
                    // no space in the section >> avoid
                    penalty += 1.0 / selection.getSections().size();
                }
            }
        }
        
        Request request = selection.getRequest();
        // only count time conflicts on the other requests
        if (iTimeConflicts && !request.equals(value.getRequest())) {
            for (Section section: selection.getSections()) {
                if (!section.isAllowOverlap()) {
                    for (Request r: request.getStudent().getRequests()) {
                        if (request.equals(r)) break;
                        Enrollment e = (value.variable().equals(r) ? value : assignment.getValue(r));
                        if (e != null && e.isCourseRequest() && (conflicts == null || !conflicts.contains(e)) && section.isOverlapping(e.getAssignments())) {
                            // time conflict with some other already assigned section >> avoid
                            penalty += 1.0 / selection.getSections().size();
                        }
                    }
                }
            }
        }
        // because time conflicts are only counted on other request, consider the case when the currently selected value is at the back 
        if (iTimeConflicts && request.getPriority() < value.getRequest().getPriority() && value.isCourseRequest()) {
            for (Section section: value.getSections()) {
                if (!section.isAllowOverlap()) {
                    if (section.isOverlapping(selection.getSections())) {
                        // time conflict with some other already assigned section >> avoid
                        penalty += 1.0 / value.getSections().size();
                    }
                }
            }
        }

        /*
        if (iTimeConflicts && !section.isAllowOverlap()) {
            // only count time conflicts on the other requests
            for (Request r: request.getStudent().getRequests()) {
                if (request.equals(r)) break;
                Enrollment e = (value.variable().equals(r) ? value : assignment.getValue(r));
                if (e != null && e.isCourseRequest() && (conflicts == null || !conflicts.contains(e)) && section.isOverlapping(e.getAssignments())) {
                    // time conflict with some other already assigned section >> avoid
                    penalty += 1.0;
                }
            }
        }
        */
        
        if (penalty > 0) return penalty;
        if (iParent == null) return 0.0;
        
        for (Section section: selection.getSections())
            penalty += iParentWeight * iParent.getOverExpected(assignment, section, request);
        return penalty;
    }

    @Override
    public double getOverExpected(Assignment<Request, Enrollment> assignment, Enrollment[] enrollment, int index, Section section, Request request) {
        double penalty = 0;
        if (iSpaceConflicts && section.getLimit() == 0) {
            // no space in the section >> avoid
            //return 1.0 / section.getSubpart().getConfig().getSubparts().size();
            penalty += 1.0;
        }
        
        if (iTimeConflicts && !section.isAllowOverlap()) {
            for (int i = 0; i < index; i++) {
                if (enrollment[i] == null || enrollment[i].getSections() == null || !enrollment[i].isCourseRequest()) continue;
                if (section.isOverlapping(enrollment[i].getSections())) {
                    // time conflict with some other already assigned section >> avoid
                    penalty += 1.0;
                }
            }
        }
        
        if (penalty > 0) return penalty / section.getSubpart().getConfig().getSubparts().size();
        return (iParent == null ? 0.0 : iParentWeight * iParent.getOverExpected(assignment, section, request));
    }
}