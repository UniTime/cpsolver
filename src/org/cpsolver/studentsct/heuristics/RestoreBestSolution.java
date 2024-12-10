package org.cpsolver.studentsct.heuristics;

import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;

/**
 * A simple step that checks whether the best solution has improved since the last check.
 * If there is no improvement, restore the best solution (reset the search to start from the best
 * solution again). The checking is done in the seletion's initialization phase,
 * no neighbors are actually computed.
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
public class RestoreBestSolution implements NeighbourSelection<Request, Enrollment> {
    private Double iBestValue = null;
    
    public RestoreBestSolution(DataProperties config) {
    }

    @Override
    public void init(Solver<Request, Enrollment> solver) {
        Progress.getInstance(solver.currentSolution().getModel()).setPhase("Restore best...", 1);
        if (solver.currentSolution().getBestInfo() == null) return; // no best saved yet
        if (iBestValue == null || iBestValue > solver.currentSolution().getBestValue()) {
            Progress.getInstance(solver.currentSolution().getModel()).debug("best value marked");
            iBestValue = solver.currentSolution().getBestValue();
        } else {
            Progress.getInstance(solver.currentSolution().getModel()).debug("best solution restored");
            solver.currentSolution().restoreBest();
        }
    }


    @Override
    public Neighbour<Request, Enrollment> selectNeighbour(Solution<Request, Enrollment> solution) {
        return null;
    }

}
