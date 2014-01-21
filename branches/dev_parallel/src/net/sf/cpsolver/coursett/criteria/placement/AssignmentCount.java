package net.sf.cpsolver.coursett.criteria.placement;

import java.util.Set;

import net.sf.cpsolver.coursett.model.Placement;

/**
 * Count number of past assignments of a value. Avoid repetition by penalizing
 * values that have been assigned too many times already. 
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
public class AssignmentCount extends PlacementSelectionCriterion {
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.NrAssignmentsWeight";
    }

    @Override
    public double getValue(Placement value, Set<Placement> conflicts) {
        return value.countAssignments();
    }
}
