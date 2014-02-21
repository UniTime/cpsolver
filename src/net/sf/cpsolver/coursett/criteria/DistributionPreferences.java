package net.sf.cpsolver.coursett.criteria;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.coursett.constraint.GroupConstraint;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Distribution preferences. This criterion counts how well the soft distribution preferences
 * are met. This is a sum of {@link GroupConstraint#getPreference()} of all soft distribution 
 * (group) constraints.
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
public class DistributionPreferences extends TimetablingCriterion {
    
    public DistributionPreferences() {
        iValueUpdateType = ValueUpdateType.NoUpdate;
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.ContrPreferenceWeight", 1.0);
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.ConstrPreferenceWeight";
    }


    @Override
    public double getValue(Placement value, Set<Placement> conflicts) {
        double ret = 0.0;
        for (GroupConstraint gc : value.variable().groupConstraints())
            ret += gc.getCurrentPreference(value);
        return ret;
    }
    
    @Override
    public double getValue(Collection<Lecture> variables) {
        double ret = 0;
        Set<GroupConstraint> constraints = new HashSet<GroupConstraint>();
        for (Lecture lect: variables) {
            for (GroupConstraint gc: lect.groupConstraints()) {
                if (!constraints.add(gc)) continue;
                ret += gc.getCurrentPreference();
            }
        }
        return ret;
    }
        
    @Override
    protected double[] computeBounds() {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (GroupConstraint gc: ((TimetableModel)getModel()).getGroupConstraints()) {
            if (!gc.isHard()) {
                bounds[0] -= Math.abs(gc.getPreference());
                bounds[1] += Math.abs(gc.getPreference()) * (gc.variables().size() * (gc.variables().size() - 1)) / 2;
            }
        }
        return bounds;
    }
    
    @Override
    public double[] getBounds(Collection<Lecture> variables) {
        double[] bounds = new double[] { 0.0, 0.0 };
        Set<GroupConstraint> constraints = new HashSet<GroupConstraint>();
        for (Lecture lect: variables) {
            for (GroupConstraint gc: lect.groupConstraints()) {
                if (!gc.isHard() && constraints.add(gc)) {
                    bounds[0] -= Math.abs(gc.getPreference());
                    bounds[1] += Math.abs(gc.getPreference()) * (gc.variables().size() * (gc.variables().size() - 1)) / 2;
                }
            }
        }
        return bounds;
    }
}
