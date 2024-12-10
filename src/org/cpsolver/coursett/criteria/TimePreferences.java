package org.cpsolver.coursett.criteria;

import java.util.Collection;
import java.util.Set;

import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Time preferences. This criterion counts how well the time preferences are met. This is
 * a sum of {@link TimeLocation#getNormalizedPreference()} of the assigned classes.
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
public class TimePreferences extends TimetablingCriterion {

    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.TimePreferenceWeight", 1.0);
    }

    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.TimePreferenceWeight";
    }
    
    private double preference(Placement value) {
        return value.getTimePenalty();
    }

    @Override
    public double getValue(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        if (value.variable().isCommitted()) return 0.0;
        double ret = value.variable().getWeight() * preference(value);
        if (conflicts != null)
            for (Placement conflict: conflicts)
                ret -= conflict.variable().getWeight() * preference(conflict);
        return ret;
    }
        
    @Override
    public double[] getBounds(Assignment<Lecture, Placement> assignment, Collection<Lecture> variables) {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (Lecture lect: variables) {
            if (lect.isCommitted()) continue;
            double[] p = lect.getMinMaxTimePreference();
            bounds[1] += lect.getWeight() * (p[1] - p[0]);
        }
        return bounds;
    }

}
