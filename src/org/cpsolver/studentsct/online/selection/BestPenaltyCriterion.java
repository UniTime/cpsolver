package org.cpsolver.studentsct.online.selection;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.FreeTimeRequest;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.online.OnlineSectioningModel;
import org.cpsolver.studentsct.online.selection.MultiCriteriaBranchAndBoundSelection.SelectionCriterion;

/**
 * A simple weighting multi-criteria selection criterion only optimizing on the
 * over-expected penalty. This criterion is used to find a bound on the
 * over-expected penalty to ensure no suggestion given to the student.
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
public class BestPenaltyCriterion implements SelectionCriterion {
    private Student iStudent;
    private OnlineSectioningModel iModel;

    public BestPenaltyCriterion(Student student, OnlineSectioningModel model) {
        iStudent = student;
        iModel = model;
    }

    private Request getRequest(int index) {
        return (index < 0 || index >= iStudent.getRequests().size() ? null : iStudent.getRequests().get(index));
    }

    private boolean isFreeTime(int index) {
        Request r = getRequest(index);
        return r != null && r instanceof FreeTimeRequest;
    }

    @Override
    public int compare(Assignment<Request, Enrollment> assignment, Enrollment[] current, Enrollment[] best) {
        if (best == null)
            return -1;

        // 0. best priority & alternativity ignoring free time requests
        for (int idx = 0; idx < current.length; idx++) {
            if (isFreeTime(idx))
                continue;
            if (best[idx] != null && best[idx].getAssignments() != null) {
                if (current[idx] == null || current[idx].getSections() == null)
                    return 1; // higher priority request assigned
                if (best[idx].getTruePriority() < current[idx].getTruePriority())
                    return 1; // less alternative request assigned
                if (best[idx].getTruePriority() > current[idx].getTruePriority())
                    return -1; // more alternative request assigned
            } else {
                if (current[idx] != null && current[idx].getAssignments() != null)
                    return -1; // higher priority request assigned
            }
        }

        // 1. minimize number of penalties
        double bestPenalties = 0, currentPenalties = 0;
        for (int idx = 0; idx < current.length; idx++) {
            if (best[idx] != null && best[idx].getAssignments() != null && best[idx].isCourseRequest()) {
                for (Section section : best[idx].getSections())
                    bestPenalties += iModel.getOverExpected(assignment, best, idx, section, best[idx].getRequest());
                for (Section section : current[idx].getSections())
                    currentPenalties += iModel.getOverExpected(assignment, current, idx, section, current[idx].getRequest());
            }
        }
        if (currentPenalties < bestPenalties)
            return -1;
        if (bestPenalties < currentPenalties)
            return 1;

        return 0;
    }

    @Override
    public boolean canImprove(Assignment<Request, Enrollment> assignment, int maxIdx, Enrollment[] current,
            Enrollment[] best) {
        // 0. best priority & alternativity ignoring free time requests
        int alt = 0;
        for (int idx = 0; idx < current.length; idx++) {
            if (isFreeTime(idx))
                continue;
            Request request = getRequest(idx);
            if (idx < maxIdx) {
                if (best[idx] != null) {
                    if (current[idx] == null)
                        return false; // higher priority request assigned
                    if (best[idx].getTruePriority() < current[idx].getTruePriority())
                        return false; // less alternative request assigned
                    if (best[idx].getTruePriority() > current[idx].getTruePriority())
                        return true; // more alternative request assigned
                    if (request.isAlternative())
                        alt--;
                } else {
                    if (current[idx] != null)
                        return true; // higher priority request assigned
                    if (!request.isAlternative())
                        alt++;
                }
            } else {
                if (best[idx] != null) {
                    if (best[idx].getTruePriority() > 0)
                        return true; // alternativity can be improved
                } else {
                    if (!request.isAlternative() || alt > 0)
                        return true; // priority can be improved
                }
            }
        }

        // 1. maximize number of penalties
        double bestPenalties = 0, currentPenalties = 0;
        for (int idx = 0; idx < current.length; idx++) {
            if (best[idx] != null) {
                for (Section section : best[idx].getSections())
                    bestPenalties += iModel.getOverExpected(assignment, best, idx, section, best[idx].getRequest());
            }
            if (current[idx] != null && idx < maxIdx) {
                for (Section section : current[idx].getSections())
                    currentPenalties += iModel.getOverExpected(assignment, current, idx, section, current[idx].getRequest());
            }
        }
        if (currentPenalties < bestPenalties)
            return true;
        if (bestPenalties < currentPenalties)
            return false;

        return false;
    }

    @Override
    public double getTotalWeight(Assignment<Request, Enrollment> assignment, Enrollment[] enrollments) {
        return 0.0;
    }

    @Override
    public int compare(Assignment<Request, Enrollment> assignment, Enrollment e1, Enrollment e2) {
        // 1. alternativity
        if (e1.getTruePriority() < e2.getTruePriority())
            return -1;
        if (e1.getTruePriority() > e2.getTruePriority())
            return 1;

        // 2. maximize number of penalties
        double p1 = 0, p2 = 0;
        for (Section section : e1.getSections())
            p1 += iModel.getOverExpected(assignment, section, e1.getRequest());
        for (Section section : e2.getSections())
            p2 += iModel.getOverExpected(assignment, section, e2.getRequest());
        if (p1 < p2)
            return -1;
        if (p2 < p1)
            return 1;

        return 0;
    }
}
