package net.sf.cpsolver.coursett.criteria.placement;

import java.util.Set;

import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.ifs.solver.Solver;

/**
 * Difference between proposed and best time assignment. Time assignments
 * that are do not maximize time preference are penalized by the difference
 * in the preference.
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
public class DeltaTimePreference extends PlacementSelectionCriterion {
    private double iLevel1DefaultWeight = 0.0;
    
    @Override
    public boolean init(Solver<Lecture, Placement> solver) {
        iLevel1DefaultWeight = solver.getProperties().getPropertyDouble("Comparator.TimePreferenceWeight", 1.0) / 2.0;
        return super.init(solver);
    }

    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.DeltaTimePreferenceWeight";
    }

    @Override
    public double getValue(Placement value, Set<Placement> conflicts) {
        double ret = value.variable().getWeight() * (value.getTimeLocation().getNormalizedPreference() - value.variable().getBestTimePreference());
        if (conflicts != null)
            for (Placement placement : conflicts) {
                double timePref = placement.getTimeLocation().getNormalizedPreference();
                ret -= placement.variable().getWeight() * (timePref - placement.variable().getBestTimePreference());
            }
        return ret;
    }

    @Override
    public double getPlacementSelectionWeightDefault(int level) {
        return (level == 0 ? iLevel1DefaultWeight : 0.0);
    }
}
