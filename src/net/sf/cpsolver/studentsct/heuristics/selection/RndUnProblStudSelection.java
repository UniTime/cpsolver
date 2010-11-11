package net.sf.cpsolver.studentsct.heuristics.selection;

import java.util.Set;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.heuristics.RoundRobinNeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

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
 * <table border='1'>
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
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
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
    public Neighbour<Request, Enrollment> selectNeighbour(Solution<Request, Enrollment> solution) {
        if (!iProblemStudents.isEmpty() && Math.random() < iRandom) {
            Student student = ToolBox.random(iProblemStudents);
            iProblemStudents.remove(student);
            return new UnassignStudentNeighbour(student);
        }
        Progress.getInstance(solution.getModel()).incProgress();
        return null;
    }
}
