package org.cpsolver.coursett.criteria.placement;

import java.util.Set;

import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.ifs.assignment.Assignment;


/**
 * Use conflict-based statistics to compute potential hard conflicts.
 * <br>
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
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
public class PotentialHardConflicts extends WeightedHardConflicts {
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.NrPotentialConflictsWeight";
    }

    @Override
    public double getValue(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        if (iStat != null && conflicts != null && !conflicts.isEmpty()) {
            return iStat.countPotentialConflicts(assignment, ((IterationContext)getContext(assignment)).getIteration(), value, 3);
        } else {
            return 0.0;
        }
    }
    
    @Override
    public double getPlacementSelectionWeightDefault(int level) {
        return 0.0;
    }

}
