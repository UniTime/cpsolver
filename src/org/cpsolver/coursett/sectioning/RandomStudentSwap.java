package org.cpsolver.coursett.sectioning;

import java.util.Iterator;
import java.util.Set;

import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.Student;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.algorithms.neighbourhoods.HillClimberSelection;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;

/**
 * A neihbourhood selection that attempts to swap a student between alternative sections of a course.
 * To be used with the Hill Climber (HC), Great Deluge (GD), or Simulated Annealing (SA) algorithms by
 * adding this class in the AdditionalNeighbours property.
 * Based on {@link StudentSwapGenerator}, but making a random selection of a lecture and of a student.
 * The selection is only available/enabled when the solver is using a single thread
 * ({@link Solver#hasSingleSolution()} is true) as student class assignments are not included in the
 * solution. 
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.4 (University Course Timetabling)<br>
 *          Copyright (C) 2024 Tomas Muller<br>
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
public class RandomStudentSwap extends StudentSwapGenerator implements HillClimberSelection {
    protected boolean iHC = false;
    protected boolean iEnabled = true;

    public RandomStudentSwap(DataProperties config) {
        super();
    }
    
    @Override
    public void init(Solver<Lecture, Placement> solver) {
        super.init(solver);
        iEnabled = solver.hasSingleSolution();
    }
    
    @Override
    public Neighbour<Lecture, Placement> selectNeighbour(Solution<Lecture, Placement> solution) {
        if (!iEnabled) return null; // not enabled
        TimetableModel model = (TimetableModel)solution.getModel();
        if (model.getAllStudents().isEmpty()) return null; // no students
        if (!model.isOnFlySectioningEnabled())
            model.setOnFlySectioningEnabled(true);
        Assignment<Lecture, Placement> assignment = solution.getAssignment();
        // select a random lecture
        Lecture lecture = ToolBox.random(model.variables());
        // get all students
        Set<Student> students = lecture.students();
        // iterate over the students from a random index
        if (students != null && !students.isEmpty()) {
            int stdCnt = students.size();
            Iterator<Student> iterator = students.iterator();
            int stdIdx = ToolBox.random(stdCnt);
            for (int i = 0; i < stdIdx; i++) iterator.next();
            for (int i = 0; i < stdCnt; i++) {
                if (!iterator.hasNext()) iterator = students.iterator();
                Student student = iterator.next();
                Neighbour<Lecture, Placement> n = generateSwap(model, assignment, student, lecture.getConfiguration());
                if (n != null && (!iHC || n.value(assignment) <= 0.0)) return n;
            }
        }
        return null;
    }

    @Override
    public void setHcMode(boolean hcMode) {
        iHC = hcMode;
    }
}
