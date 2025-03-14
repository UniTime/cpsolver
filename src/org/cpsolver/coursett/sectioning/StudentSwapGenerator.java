package org.cpsolver.coursett.sectioning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.cpsolver.coursett.model.Configuration;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.Student;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.ToolBox;

/**
 * Generate student swaps. Rather than generating swaps at random, the class iterates between
 * all classes and all conflicting students of each class and generate a random swap for
 * each such student.
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2017 Tomas Muller<br>
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
public class StudentSwapGenerator implements NeighbourSelection<Lecture, Placement> {
    Iterator<Lecture> iLectures = null;
    Lecture iLecture = null;
    Iterator<Student> iStudents = null;
    
    @Override
    public void init(Solver<Lecture, Placement> solver) {
    }

    @Override
    public Neighbour<Lecture, Placement> selectNeighbour(Solution<Lecture, Placement> solution) {
        TimetableModel model = (TimetableModel)solution.getModel();
        if (model.getAllStudents().isEmpty()) return null;

        boolean next = false;
        while (iLecture == null || iStudents == null || !iStudents.hasNext()) {
            if (iLectures == null || !iLectures.hasNext()) {
                if (next) return null;
                iLectures = model.variables().iterator();
                next = true;
            }
            iLecture = iLectures.next();
            if (iLecture.getConfiguration() == null || (iLecture.getConfiguration().getAltConfigurations().isEmpty() && iLecture.isSingleSection())) continue;
            iStudents = iLecture.conflictStudents(solution.getAssignment()).iterator();
        }
        return generateSwap(model, solution.getAssignment(), iStudents.next(), iLecture.getConfiguration());
    }
    
    public Neighbour<Lecture, Placement> selectNeighbour(Assignment<Lecture, Placement> assignment, Lecture lecture) {
        return generateSwap((TimetableModel)lecture.getModel(), assignment, ToolBox.random(lecture.students()), lecture.getConfiguration());

    }
    
    public Neighbour<Lecture, Placement> generateSwap(TimetableModel model, Assignment<Lecture, Placement> assignment, Student student, Configuration config) {
        if (student == null || config == null) return null;
        if (!config.getAltConfigurations().isEmpty()) {
            int idx = ToolBox.random(config.getAltConfigurations().size() + 2);
            if (idx > 1) config = config.getAltConfigurations().get(idx - 2);
        }
        Long subpartId = ToolBox.random(config.getTopSubpartIds());
        int nrAttempts = ToolBox.random(10);
        for (int i = 0; i < nrAttempts; i++) {
            Lecture lecture = ToolBox.random(config.getTopLectures(subpartId));
            Student other = ToolBox.random(lecture.students());
            if (other != null && !student.equals(other)) {
                StudentSwap swap = new StudentSwap(model, assignment, student, other, config.getOfferingId());
                if (swap.isAllowed()) return swap;
            }
        }
        List<Lecture> lectures = new ArrayList<Lecture>();
        Queue<Collection<Lecture>> queue = new LinkedList<Collection<Lecture>>(config.getTopLectures().values());
        while (!queue.isEmpty()) {
            List<Lecture> adepts = new ArrayList<Lecture>();
            for (Lecture adept: queue.poll())
                if (student.canEnroll(adept)) adepts.add(adept);
            if (adepts.isEmpty()) return null;
            Lecture lect = ToolBox.random(adepts);
            lectures.add(lect);
            if (lect.hasAnyChildren())
                queue.addAll(lect.getChildren().values());
        }
        StudentSwap swap = new StudentSwap(model, assignment, student, lectures);
        return (swap.isAllowed() ? swap : null);
    }

}
