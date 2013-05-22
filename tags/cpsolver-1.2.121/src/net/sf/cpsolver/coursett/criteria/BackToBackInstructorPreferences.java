package net.sf.cpsolver.coursett.criteria;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.coursett.Constants;
import net.sf.cpsolver.coursett.constraint.InstructorConstraint;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimetableModel;

import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Bact-to-back instructor preferences. This criterion counts cases when an instructor
 * has to teach two classes in two rooms that are too far a part. This objective
 * is counter by the {@link InstructorConstraint}
 * (see {@link InstructorConstraint#getDistancePreference(Placement, Placement)}).
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
public class BackToBackInstructorPreferences extends TimetablingCriterion {
    
    public BackToBackInstructorPreferences() {
        iValueUpdateType = ValueUpdateType.NoUpdate;
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return Constants.sPreferenceLevelDiscouraged * config.getPropertyDouble("Comparator.DistanceInstructorPreferenceWeight", 1.0);
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.DistanceInstructorPreferenceWeight";
    }
    
    protected int penalty(Placement value) {
        int ret = 0;
        for (InstructorConstraint ic: value.variable().getInstructorConstraints()) {
            ret += ic.getPreference(value);
        }
        return ret;
    }
    
    @Override
    public double getValue(Placement value, Set<Placement> conflicts) {
        double ret = penalty(value);
        if (conflicts != null)
            for (Placement conflict: conflicts)
                ret -= penalty(conflict);
        return ret;
    }

    @Override
    public double getValue(Collection<Lecture> variables) {
        double ret = 0;
        Set<InstructorConstraint> constraints = new HashSet<InstructorConstraint>();
        for (Lecture lect: variables) {
            for (InstructorConstraint ic: lect.getInstructorConstraints()) {
                if (!constraints.add(ic)) continue;
                ret += ic.getPreference();
            }
        }
        return ret;
    }
    
    @Override
    protected double[] computeBounds() {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (InstructorConstraint ic: ((TimetableModel)getModel()).getInstructorConstraints())
            bounds[1] += ic.getWorstPreference();
        return bounds;
    }
    
    @Override
    public double[] getBounds(Collection<Lecture> variables) {
        double[] bounds = new double[] { 0.0, 0.0 };
        Set<InstructorConstraint> constraints = new HashSet<InstructorConstraint>();
        for (Lecture lect: variables) {
            for (InstructorConstraint ic: lect.getInstructorConstraints()) {
                if (!constraints.add(ic)) continue;
                bounds[1] += ic.getWorstPreference();
            }
        }
        return bounds;
    }
}
