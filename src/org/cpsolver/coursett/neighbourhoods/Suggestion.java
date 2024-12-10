package org.cpsolver.coursett.neighbourhoods;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cpsolver.coursett.heuristics.NeighbourSelectionWithSuggestions;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.SimpleNeighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;

/**
 * A simple neighbor selection based on {@link NeighbourSelectionWithSuggestions},
 * only triggering when there is an unassigned class. The selection picks a random unassigned 
 * class and tries to do a limited-depth search up to the Neighbour.SuggestionDepth
 * depth. If successful, the returned suggestion is always considered improving as
 * it finds an assignment that increases the number of assigned classes.
 * <br>
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Iterative Forward Search)<br>
 *          Copyright (C) 2014 Tomas Muller<br>
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
public class Suggestion extends NeighbourSelectionWithSuggestions {
    private boolean iAllowUnassignments = false;

    public Suggestion(DataProperties properties) throws Exception {
        super(properties);
        iAllowUnassignments = properties.getPropertyBoolean("Suggestion.AllowUnassignments", iAllowUnassignments);
    }
    
    @Override
    public Neighbour<Lecture, Placement> selectNeighbour(Solution<Lecture, Placement> solution) {
        Collection<Lecture> unassigned = solution.getModel().unassignedVariables(solution.getAssignment());
        if (!unassigned.isEmpty()) {
            Lecture lecture = ToolBox.random(unassigned);
            int depth = Math.max(2, ToolBox.random(iSuggestionDepth));
            Neighbour<Lecture, Placement> neigbour = selectNeighbourWithSuggestions(solution, lecture, depth);
            if (neigbour != null)
                return new SuggestionNeighbour(neigbour);
            Placement placement = ToolBox.random(lecture.values(solution.getAssignment()));
            if (placement != null) {
                Set<Placement> conflicts = new HashSet<Placement>();
                if (iStat != null)
                    for (Map.Entry<Constraint<Lecture, Placement>, Set<Placement>> entry: solution.getModel().conflictConstraints(solution.getAssignment(), placement).entrySet()) {
                        iStat.constraintAfterAssigned(solution.getAssignment(), solution.getIteration(), entry.getKey(), placement, entry.getValue());
                        conflicts.addAll(entry.getValue());
                    }
                else
                    conflicts = solution.getModel().conflictValues(solution.getAssignment(), placement);
                if (!conflicts.contains(placement) && (iAllowUnassignments || conflicts.size() <= 1))
                    return new SimpleNeighbour<Lecture, Placement>(lecture, placement, conflicts);
            }
        }
        return null;
    }
    
    /** Simple @{link Neighbour} wrapper always returning -1 as the value */
    static class SuggestionNeighbour implements Neighbour<Lecture, Placement> {
        Neighbour<Lecture, Placement> iNeigbour;
        
        SuggestionNeighbour(Neighbour<Lecture, Placement> neigbour) {
            iNeigbour = neigbour;
        }

        @Override
        public void assign(Assignment<Lecture, Placement> assignment, long iteration) {
            iNeigbour.assign(assignment, iteration);
        }

        @Override
        public double value(Assignment<Lecture, Placement> assignment) {
            return -1;
        }

        @Override
        public Map<Lecture, Placement> assignments() {
            return iNeigbour.assignments();
        }
    }
}
