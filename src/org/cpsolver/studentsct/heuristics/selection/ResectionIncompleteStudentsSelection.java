package org.cpsolver.studentsct.heuristics.selection;

import org.apache.logging.log4j.Logger;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.studentsct.heuristics.studentord.StudentOrder;
import org.cpsolver.studentsct.heuristics.studentord.StudentRandomOrder;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Student;


/**
 * Resection incomplete studends. An extension of {@link BranchBoundSelection},
 * where only students that are not complete ({@link Student#isComplete(Assignment)} is
 * false) and that are sectioned somewhere ({@link Student#nrAssignedRequests(Assignment)}
 * is greater then zero) are resectioned.
 * 
 * <br>
 * <br>
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2014 Tomas Muller<br>
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

public class ResectionIncompleteStudentsSelection extends BranchBoundSelection {
    private static Logger sLog = org.apache.logging.log4j.LogManager.getLogger(ResectionIncompleteStudentsSelection.class);

    public ResectionIncompleteStudentsSelection(DataProperties properties) {
        super(properties);
        iOrder = new StudentRandomOrder(properties);
        if (properties.getProperty("Neighbour.ResectionIncompleteStudentsOrder") != null) {
            try {
                iOrder = (StudentOrder) Class.forName(
                        properties.getProperty("Neighbour.ResectionIncompleteStudentsOrder")).getConstructor(
                        new Class[] { DataProperties.class }).newInstance(new Object[] { properties });
            } catch (Exception e) {
                sLog.error("Unable to set student order, reason:" + e.getMessage(), e);
            }
        }
    }

    @Override
    public void init(Solver<Request, Enrollment> solver) {
        init(solver, "Resection incomplete students...");
    }

    /**
     * Select neighbour. All students with an incomplete and non-empty schedule
     * are taken, one by one in a random order. For each student a branch &amp; 
     * bound search is employed.
     */
    @Override
    public Neighbour<Request, Enrollment> selectNeighbour(Solution<Request, Enrollment> solution) {
        Student student = null;
        while ((student = nextStudent()) != null) {
            Progress.getInstance(solution.getModel()).incProgress();
            if (student.nrAssignedRequests(solution.getAssignment()) == 0 || student.isComplete(solution.getAssignment()))
                continue;
            Neighbour<Request, Enrollment> neighbour = getSelection(solution.getAssignment(), student).select();
            if (neighbour != null)
                return neighbour;
        }
        return null;
    }

}
