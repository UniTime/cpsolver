package org.cpsolver.coursett.criteria;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.cpsolver.coursett.constraint.DepartmentSpreadConstraint;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Department balancing penalty. This criterion tries to balance classes of each
 * department evenly. This means that for instance each department gets a fair
 * amount of unpopular times like early morning or late afternoon. This criterion
 * is counted by {@link DepartmentSpreadConstraint}. 
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
public class DepartmentBalancingPenalty extends SameSubpartBalancingPenalty {

    @Override
    public double getWeightDefault(DataProperties config) {
        return 12.0 * config.getPropertyDouble("Comparator.DeptSpreadPenaltyWeight", 1.0);
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.DeptSpreadPenaltyWeight";
    }

    @Override
    public double getValue(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        return (value.variable().getDeptSpreadConstraint() == null ? 0.0 : value.variable().getDeptSpreadConstraint().getPenalty(assignment, value)) / 12.0;
    }
    
    @Override
    public double getValue(Assignment<Lecture, Placement> assignment, Collection<Lecture> variables) {
        double ret = 0;
        Set<DepartmentSpreadConstraint> constraints = new HashSet<DepartmentSpreadConstraint>();
        for (Lecture lect: variables)
            if (lect.getDeptSpreadConstraint() != null && constraints.add(lect.getDeptSpreadConstraint()))
                ret += lect.getDeptSpreadConstraint().getPenalty(assignment);
        return ret / 12.0;
    }
}
