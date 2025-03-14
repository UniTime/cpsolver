package org.cpsolver.studentsct.heuristics;

import java.util.ArrayList;
import java.util.List;

import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Student;


/**
 * Two-phase (Batch) student sectioning neighbour selection. It is based on
 * {@link StudentSctNeighbourSelection}, however in the first round, only real
 * students are sectioned. All dummy students are removed from the problem
 * during initialization of this neighbour selection, they are returned into the
 * problem after the first round of {@link StudentSctNeighbourSelection}.
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

public class TwoPhaseStudentSctNeighbourSelection extends StudentSctNeighbourSelection {
    private int iNrRounds = 7;

    public TwoPhaseStudentSctNeighbourSelection(DataProperties properties) throws Exception {
        super(properties);
        iNrRounds = properties.getPropertyInt("TwoPhaseSectioning.NrRoundsFirstPhase", iNrRounds);
    }

    /** Initialization -- also remove all the dummy students from the problem */
    @Override
    public void init(Solver<Request, Enrollment> solver) {
        super.init(solver);
        if (removeDummyStudents(solver.currentSolution()))
            registerSelection(new RestoreDummyStudents());
    }

    List<Student> iDummyStudents = null;

    private boolean removeDummyStudents(Solution<Request, Enrollment> solution) {
        StudentSectioningModel model = (StudentSectioningModel) solution.getModel();
        if (model.getNrLastLikeStudents(false) == 0 || model.getNrRealStudents(false) == 0)
            return false;
        iDummyStudents = new ArrayList<Student>();
        for (Student student : new ArrayList<Student>(model.getStudents())) {
            if (student.isDummy()) {
                iDummyStudents.add(student);
                model.removeStudent(student);
            }
        }
        return true;
    }

    private boolean addDummyStudents(Solution<Request, Enrollment> solution) {
        if (iDummyStudents == null || iDummyStudents.isEmpty())
            return false;
        iNrRounds--;
        if (iNrRounds > 0)
            return false;
        solution.restoreBest();
        StudentSectioningModel model = (StudentSectioningModel) solution.getModel();
        for (Student student : iDummyStudents) {
            model.addStudent(student);
        }
        iDummyStudents = null;
        solution.saveBest();
        return true;
    }

    /**
     * Return all dummy students into the problem, executed as the last phase of
     * the first round
     */
    protected class RestoreDummyStudents implements NeighbourSelection<Request, Enrollment> {
        public RestoreDummyStudents() {
        }

        @Override
        public void init(Solver<Request, Enrollment> solver) {
        }

        /** Return all (removed) dummy students into the problem */
        @Override
        public Neighbour<Request, Enrollment> selectNeighbour(Solution<Request, Enrollment> solution) {
            addDummyStudents(solution);
            return null;
        }
    }
}
