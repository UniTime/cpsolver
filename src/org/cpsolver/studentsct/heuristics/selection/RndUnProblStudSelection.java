package org.cpsolver.studentsct.heuristics.selection;

import java.util.Set;

import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.heuristics.RoundRobinNeighbourSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Request.RequestPriority;
import org.cpsolver.studentsct.model.Student;


/**
 * Random unassignment of some problematic students. Problematic students are to
 * be provided by a neighbour selection that was used before this one by
 * {@link RoundRobinNeighbourSelection}.
 * 
 * <br>
 * <br>
 * In each step a problematic student is randomly selected with the given
 * probabilty. Null is returned otherwise (the controll is passed to the next
 * {@link NeighbourSelection}).
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
 * <td>Neighbour.RandomUnassignmentOfProblemStudentProb</td>
 * <td>{@link Double}</td>
 * <td>Probability of a random selection of a student from the given set of
 * problematic students.</td>
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
public class RndUnProblStudSelection extends RandomUnassignmentSelection {
    private ProblemStudentsProvider iProblemStudentsProvider = null;
    private Set<Student> iProblemStudents = null;

    /**
     * Constructor
     * 
     * @param properties
     *            configuration
     * @param psp
     *            a class that provides the set of problematic students
     */
    public RndUnProblStudSelection(DataProperties properties, ProblemStudentsProvider psp) {
        super(properties);
        iProblemStudentsProvider = psp;
        iRandom = properties.getPropertyDouble("Neighbour.RandomUnassignmentOfProblemStudentProb", 0.9);
    }

    /**
     * Initialization -- {@link ProblemStudentsProvider#getProblemStudents()} is
     * called
     */
    @Override
    public void init(Solver<Request, Enrollment> solver) {
        iProblemStudents = iProblemStudentsProvider.getProblemStudents();
        Progress.getInstance(solver.currentSolution().getModel()).setPhase(
                "Random unassignment of problematic students...", 1);
    }

    /**
     * With the given probabilty, a problematic student is randomly selected to
     * be unassigned. Null is returned otherwise.
     */
    @Override
    public synchronized Neighbour<Request, Enrollment> selectNeighbour(Solution<Request, Enrollment> solution) {
        if (Math.random() < iRandom) {
            while (!iProblemStudents.isEmpty()) {
                Student student = ToolBox.random(iProblemStudents);
                iProblemStudents.remove(student);
                if (student.hasMinCredit() && student.getAssignedCredit(solution.getAssignment()) < student.getMinCredit()) continue;
                return new UnassignStudentNeighbour(student, solution.getAssignment(), RequestPriority.Important);
            }
        }
        Progress.getInstance(solution.getModel()).incProgress();
        return null;
    }
}
