package net.sf.cpsolver.coursett.criteria;

import java.util.Collection;
import java.util.Set;

import net.sf.cpsolver.coursett.Constants;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;

import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Room preferences. This criterion counts how well the room preferences are met. This is
 * a sum of {@link Placement#getRoomPreference()} of the assigned classes.
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
public class RoomPreferences extends TimetablingCriterion {

    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.RoomPreferenceWeight", 1.0);
    }

    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.RoomPreferenceWeight";
    }
    
    private int preference(Placement value) {
        int pref = value.getRoomPreference();
        if (pref < Constants.sPreferenceLevelRequired / 2)
            return value.variable().getMinMaxRoomPreference()[0];
        else if (pref > Constants.sPreferenceLevelProhibited / 2)
            return value.variable().getMinMaxRoomPreference()[1];
        else
            return pref;
    }
    
    @Override
    public double getValue(Placement value, Set<Placement> conflicts) {
        if (value.variable().isCommitted()) return 0.0;
        double ret = value.variable().getWeight() * preference(value);
        if (conflicts != null)
            for (Placement conflict: conflicts)
                ret -= conflict.variable().getWeight() * preference(conflict);
        return ret;
    }

    @Override
    public double[] getBounds(Collection<Lecture> variables) {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (Lecture lect: variables) {
            if (lect.isCommitted()) continue;
            int[] p = lect.getMinMaxRoomPreference();
            bounds[0] += lect.getWeight() * p[0];
            bounds[1] += lect.getWeight() * p[1];
        }
        return bounds;
    }

}
