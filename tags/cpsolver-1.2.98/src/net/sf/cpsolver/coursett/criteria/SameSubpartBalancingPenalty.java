package net.sf.cpsolver.coursett.criteria;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.coursett.constraint.SpreadConstraint;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;

import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Same subpart balancing penalty. This criterion tries to spread classes of each
 * scheduling subpart in time. It also includes all other spread in time distribution
 * constraints. This criterion is counted by {@link SpreadConstraint}. 
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
public class SameSubpartBalancingPenalty extends TimetablingCriterion {
    
    public SameSubpartBalancingPenalty() {
        iValueUpdateType = ValueUpdateType.NoUpdate;
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 12.0 * config.getPropertyDouble("Comparator.SpreadPenaltyWeight", 1.0);
    }

    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.SpreadPenaltyWeight";
    }

    @Override
    public double getValue(Placement value, Set<Placement> conflicts) {
        double ret = 0.0;
        for (SpreadConstraint sc: value.variable().getSpreadConstraints())
            ret += sc.getPenalty(value);
        return ret / 12.0;
    }
    
    @Override
    public double getValue() {
        return iValue / 12.0;
    }
    
    @Override
    public double getValue(Collection<Lecture> variables) {
        double ret = 0;
        Set<SpreadConstraint> constraints = new HashSet<SpreadConstraint>();
        for (Lecture lect: variables) {
            for (SpreadConstraint sc: lect.getSpreadConstraints()) {
                if (!constraints.add(sc)) continue;
                ret += sc.getPenalty();
            }
        }
        return ret / 12.0;
    }
    
    @Override
    public double[] getBounds() {
        return new double[] { 0.0, 0.0 };
    }
    
    @Override
    public double[] getBounds(Collection<Lecture> variables) {
        return new double[] { 0.0, 0.0 };
    }
}
