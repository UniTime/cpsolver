package net.sf.cpsolver.studentsct.heuristics.selection;

import java.util.List;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * Random unassignment of some (randomly selected) students.
 * 
 * <br>
 * <br>
 * In each step a student is randomly selected with the given probabilty. Null
 * is returned otherwise (controll is passed to the following
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
 * <td>Neighbour.RandomUnassignmentProb</td>
 * <td>{@link Double}</td>
 * <td>Probability of a random selection of a student.</td>
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
public class RandomUnassignmentSelection implements NeighbourSelection<Request, Enrollment> {
    private List<Student> iStudents = null;
    protected double iRandom = 0.5;

    /**
     * Constructor
     * 
     * @param properties
     *            configuration
     */
    public RandomUnassignmentSelection(DataProperties properties) {
        iRandom = properties.getPropertyDouble("Neighbour.RandomUnassignmentProb", iRandom);
    }

    /**
     * Initialization
     */
    @Override
    public void init(Solver<Request, Enrollment> solver) {
        iStudents = ((StudentSectioningModel) solver.currentSolution().getModel()).getStudents();
        Progress.getInstance(solver.currentSolution().getModel()).setPhase("Random unassignment...", 1);
    }

    /**
     * With the given probabilty, a student is randomly selected to be
     * unassigned. Null is returned otherwise.
     */
    @Override
    public Neighbour<Request, Enrollment> selectNeighbour(Solution<Request, Enrollment> solution) {
        if (Math.random() < iRandom) {
            Student student = ToolBox.random(iStudents);
            return new UnassignStudentNeighbour(student);
        }
        Progress.getInstance(solution.getModel()).incProgress();
        return null;
    }

    /** Unassignment of all requests of a student */
    public static class UnassignStudentNeighbour extends Neighbour<Request, Enrollment> {
        private Student iStudent = null;

        /**
         * Constructor
         * 
         * @param student
         *            a student to be unassigned
         */
        public UnassignStudentNeighbour(Student student) {
            iStudent = student;
        }

        @Override
        public double value() {
            double val = 0;
            for (Request request : iStudent.getRequests()) {
                if (request.getAssignment() != null)
                    val -= request.getAssignment().toDouble();
            }
            return val;
        }

        /** All requests of the given student are unassigned */
        @Override
        public void assign(long iteration) {
            for (Request request : iStudent.getRequests()) {
                if (request.getAssignment() != null)
                    request.unassign(iteration);
            }
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer("Un{");
            sb.append(" " + iStudent);
            sb.append(" }");
            return sb.toString();
        }

    }

}
