package org.cpsolver.studentsct.online;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.RouletteWheelSelection;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.online.expectations.OverExpectedCriterion;

/**
 * A global constraint limiting the over-expected penalization for a student.
 * Only to be used during online scheduling (with the {@link OnlineSectioningModel}).
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
public class MaxOverExpectedConstraint extends GlobalConstraint<Request, Enrollment> {
    private double iMaxOverExpected;

    public MaxOverExpectedConstraint(double limit) {
        iMaxOverExpected = limit;
    }

    @Override
    public void computeConflicts(Assignment<Request, Enrollment> assignment, Enrollment value, Set<Enrollment> conflicts) {
        if (!value.isCourseRequest()) return;
        CourseRequest cr = (CourseRequest) value.variable();
        OnlineSectioningModel model = (OnlineSectioningModel) getModel();

        double basePenalty = model.getOverExpected(assignment, value, value, conflicts);

        if (basePenalty > iMaxOverExpected) {
            conflicts.add(value);
            return;
        }

        RouletteWheelSelection<Enrollment> selection = new RouletteWheelSelection<Enrollment>();

        for (Request r : cr.getStudent().getRequests()) {
            Enrollment e = assignment.getValue(r);
            if (e != null && !r.equals(value.variable()) && !conflicts.contains(e) && e.isCourseRequest()) {
                double penalty = model.getOverExpected(assignment, e, value, conflicts);
                if (penalty > 0.0)
                    selection.add(e, penalty);
            }
        }

        while (selection.getRemainingPoints() + basePenalty > iMaxOverExpected && selection.hasMoreElements()) {
            conflicts.add(selection.nextElement());
        }
    }

    @Override
    public boolean inConflict(Assignment<Request, Enrollment> assignment, Enrollment value) {
        if (!value.isCourseRequest()) return false;
        OverExpectedCriterion over = ((OnlineSectioningModel)getModel()).getOverExpectedCriterion();
        if (over == null) return false;

        double penalty = 0.0;
        for (Request r : value.getRequest().getStudent().getRequests()) {
            Enrollment e = (r.equals(value.variable()) ? value : assignment.getValue(r));
            if (e != null && e.isCourseRequest())
                for (Section s : e.getSections())
                    penalty += over.getOverExpected(assignment, s, e.getRequest());
        }
        return penalty > iMaxOverExpected;
    }

}