package net.sf.cpsolver.coursett.criteria.placement;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.ifs.extension.ConflictStatistics;
import net.sf.cpsolver.ifs.extension.Extension;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solution.SolutionListener;
import net.sf.cpsolver.ifs.solver.Solver;

/**
 * Hard conflicts weighted by the conflict-based statistics (past occurrences).
 * <br>
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2011 Tomas Muller<br>
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
public class WeightedHardConflicts extends PlacementSelectionCriterion implements SolutionListener<Lecture, Placement> {
    protected ConflictStatistics<Lecture, Placement> iStat = null;
    protected long iIteration = 0;
    
    @Override
    public boolean init(Solver<Lecture, Placement> solver) {
        super.init(solver);
        for (Extension<Lecture, Placement> extension : solver.getExtensions()) {
            if (ConflictStatistics.class.isInstance(extension))
                iStat = (ConflictStatistics<Lecture, Placement>) extension;
        }
        solver.currentSolution().addSolutionListener(this);
        return true;
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.NrConflictsWeight";
    }

    @Override
    public double getValue(Placement value, Set<Placement> conflicts) {
        return (iStat == null || conflicts == null ? 0.0 : iStat.countRemovals(iIteration, conflicts, value));
    }

    @Override
    public double getPlacementSelectionWeightDefault(int level) {
        return (level == 0 ? 3.0 : 0.0);
    }

    @Override
    public void solutionUpdated(Solution<Lecture, Placement> solution) {
        iIteration = solution.getIteration();
    }

    @Override
    public void getInfo(Solution<Lecture, Placement> solution, Map<String, String> info) {
    }

    @Override
    public void getInfo(Solution<Lecture, Placement> solution, Map<String, String> info, Collection<Lecture> variables) {
    }

    @Override
    public void bestCleared(Solution<Lecture, Placement> solution) {
        iIteration = solution.getIteration();        
    }

    @Override
    public void bestSaved(Solution<Lecture, Placement> solution) {
        iIteration = solution.getIteration();
    }

    @Override
    public void bestRestored(Solution<Lecture, Placement> solution) {
        iIteration = solution.getIteration();
    }

}
