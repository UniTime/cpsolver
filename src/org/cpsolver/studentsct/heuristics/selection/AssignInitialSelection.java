package org.cpsolver.studentsct.heuristics.selection;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.JProf;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Student;

/**
 * Assign initial enrollments. An extension of {@link BranchBoundSelection},
 * where only initial enrollments (see {@link Request#getInitialAssignment()}) can 
 * be assigned. Students that already has a schedule
 * ({@link Student#nrAssignedRequests(Assignment)} is greater then zero) are ignored.
 * 
 * <br>
 * <br>
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2015 Tomas Muller<br>
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
public class AssignInitialSelection extends BranchBoundSelection {
    
    public AssignInitialSelection(DataProperties properties) {
        super(properties);
    }
    
    @Override
    public void init(Solver<Request, Enrollment> solver) {
        init(solver, "Assign initial enrollments...");
    }
    
    @Override
    public Neighbour<Request, Enrollment> selectNeighbour(Solution<Request, Enrollment> solution) {
        Student student = null;
        while ((student = nextStudent()) != null) {
            Progress.getInstance(solution.getModel()).incProgress();
            if (student.nrAssignedRequests(solution.getAssignment()) > 0) continue;
            Neighbour<Request, Enrollment> neighbour = new InitialSelection(student, solution.getAssignment()).select();
            if (neighbour != null)
                return neighbour;
        }
        return null;
    }
    
    public class InitialSelection extends Selection {
        public InitialSelection(Student student, Assignment<Request, Enrollment> assignment) {
            super(student, assignment);
        }
        
        @Override
        public void backTrack(int idx) {
            if (iTimeout > 0 && (JProf.currentTimeMillis() - iT0) > iTimeout) {
                iTimeoutReached = true;
                return;
            }
            if (getBestAssignment() != null && getBound(idx) >= getBestValue()) {
                return;
            }
            if (idx == iAssignment.length) {
                if (getBestAssignment() == null || getValue() < getBestValue()) {
                    saveBest();
                }
                return;
            }
            Request request = iStudent.getRequests().get(idx);
            if (!canAssign(request, idx)) {
                backTrack(idx + 1);
                return;
            }
            if (request.isMPP()) {
                Enrollment initial = request.getInitialAssignment();
                if (initial != null) {
                    if (!inConflict(idx, initial)) {
                        iAssignment[idx] = initial;
                        backTrack(idx + 1);
                        iAssignment[idx] = null;
                    }
                } else {
                    backTrack(idx + 1);
                }
            } else {
                backTrack(idx + 1);
            }
        }
    }
}
