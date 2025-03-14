package org.cpsolver.studentsct.heuristics.selection;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.studentsct.heuristics.studentord.StudentChoiceOrder;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Request.RequestPriority;

/**
 * This selection is very much like {@link BranchBoundSelection}, but only critical
 * course requests are assigned (see {@link CourseRequest#isCritical()}.
 * Students that do not have any unassigned critical courses are skipped.
 * 
 * <br>
 * <br>
 * Parameters: <br>
 * <table border='1'><caption>Related Solver Parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>Neighbour.CriticalCoursesBranchAndBoundTimeout</td>
 * <td>{@link Integer}</td>
 * <td>Timeout for each neighbour selection (in milliseconds).</td>
 * </tr>
 * <tr>
 * <td>Neighbour.BranchAndBoundMinimizePenalty</td>
 * <td>{@link Boolean}</td>
 * <td>If true, section penalties (instead of section values) are minimized:
 * overall penalty is minimized together with the maximization of the number of
 * assigned requests and minimization of distance conflicts -- this variant is
 * to better mimic the case when students can choose their sections (section
 * times).</td>
 * </tr>
 * </table>
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
public class CriticalCoursesBranchAndBoundSelection extends BranchBoundSelection {
    protected boolean iMPP = false;
    private RequestPriority iPriority;
    
    public CriticalCoursesBranchAndBoundSelection(DataProperties properties, RequestPriority priority) {
        super(properties);
        iMPP = properties.getPropertyBoolean("General.MPP", false);
        iTimeout = properties.getPropertyInt("Neighbour.CriticalCoursesBranchAndBoundTimeout", 10000);
        iPriority = priority;
        if (iOrder instanceof StudentChoiceOrder) {
            ((StudentChoiceOrder)iOrder).setCriticalOnly(true);
            ((StudentChoiceOrder)iOrder).setRequestPriority(iPriority);
        }
    }
    
    public CriticalCoursesBranchAndBoundSelection(DataProperties properties) {
        this(properties, RequestPriority.Critical);
    }
    
    @Override
    public void init(Solver<Request, Enrollment> solver) {
        init(solver, iPriority.name() + " Courses B&B" + (iFilter == null ? "" : " (" + iFilter.getName().toLowerCase() + " students)") + "...");
    }
    
    @Override
    public Neighbour<Request, Enrollment> selectNeighbour(Solution<Request, Enrollment> solution) {
        Student student = null;
        while ((student = nextStudent()) != null) {
            Progress.getInstance(solution.getModel()).incProgress();
            if (student.hasUnassignedCritical(solution.getAssignment(), iPriority)) {
                // only consider students that have some unassigned critical course requests
                Neighbour<Request, Enrollment> neighbour = getSelection(solution.getAssignment(), student).select();
                if (neighbour != null) return neighbour;
            }
        }
        return null;
    }
    
    @Override
    public Selection getSelection(Assignment<Request, Enrollment> assignment, Student student) {
        return new CriticalCoursesSelection(student, assignment);
    }
    
    public class CriticalCoursesSelection extends Selection {
        
        public CriticalCoursesSelection(Student student, Assignment<Request, Enrollment> assignment) {
            super(student, assignment);
        }
        
        public boolean isCritical(int idx) {
            for (int i = idx; i < iStudent.getRequests().size(); i++) {
                Request r = iStudent.getRequests().get(i);
                if (!r.isAlternative() && iPriority.isCritical(r)) return true;
            }
            return false;
        }
        
        @Override
        public void backTrack(int idx) {
            if (!isCritical(idx)) {
                if (iMinimizePenalty) {
                    if (getBestAssignment() == null || (getNrAssigned() > getBestNrAssigned() || (getNrAssigned() == getBestNrAssigned() && getPenalty() < getBestValue())))
                        saveBest();
                } else {
                    if (getBestAssignment() == null || getValue() < getBestValue())
                        saveBest();
                }
                return;
            }
            if (idx < iAssignment.length && !iPriority.isCritical(iStudent.getRequests().get(idx)) && (!iMPP || iStudent.getRequests().get(idx).getInitialAssignment() == null)) {
                // not done yet && not critical && not initial >> leave unassigned
                backTrack(idx + 1);
            } else {
                super.backTrack(idx);
            }
        }
    }
}
