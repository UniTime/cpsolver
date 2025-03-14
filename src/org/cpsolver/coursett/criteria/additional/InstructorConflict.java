package org.cpsolver.coursett.criteria.additional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cpsolver.coursett.constraint.InstructorConstraint;
import org.cpsolver.coursett.constraint.SoftInstructorConstraint;
import org.cpsolver.coursett.criteria.TimetablingCriterion;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;

/**
 * Instructor conflict counting criterion. Used by the {@link SoftInstructorConstraint}.
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
public class InstructorConflict extends TimetablingCriterion {
    
    public InstructorConflict() {
        setValueUpdateType(ValueUpdateType.NoUpdate);
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.InstructorConflictWeight", 100.0);
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.InstructorConflictWeight";
    }
    
    protected int penalty(Assignment<Lecture, Placement> assignment, Placement value) {
        int ret = 0;
        for (InstructorConstraint ic: value.variable().getInstructorConstraints()) {
            if (ic instanceof SoftInstructorConstraint)
                ret += ((SoftInstructorConstraint)ic).getConflicts(assignment, value);
        }
        return ret;
    }
    
    @Override
    public double getValue(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        double ret = penalty(assignment, value);
        if (conflicts != null)
            for (Placement conflict: conflicts)
                ret -= penalty(assignment, conflict);
        return ret;
    }

    @Override
    public double getValue(Assignment<Lecture, Placement> assignment, Collection<Lecture> variables) {
        double ret = 0;
        Set<InstructorConstraint> constraints = new HashSet<InstructorConstraint>();
        for (Lecture lect: variables) {
            for (InstructorConstraint ic: lect.getInstructorConstraints()) {
                if (!constraints.add(ic)) continue;
                if (ic instanceof SoftInstructorConstraint)
                    ret += ((SoftInstructorConstraint)ic).getConflicts(assignment);
            }
        }
        return ret;
    }
    
    @Override
    protected double[] computeBounds(Assignment<Lecture, Placement> assignment) {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (InstructorConstraint ic: ((TimetableModel)getModel()).getInstructorConstraints())
            if (ic instanceof SoftInstructorConstraint)
                bounds[1] += ((SoftInstructorConstraint)ic).getWorstConflicts();
        return bounds;
    }
    
    @Override
    public double[] getBounds(Assignment<Lecture, Placement> assignment, Collection<Lecture> variables) {
        double[] bounds = new double[] { 0.0, 0.0 };
        Set<InstructorConstraint> constraints = new HashSet<InstructorConstraint>();
        for (Lecture lect: variables) {
            for (InstructorConstraint ic: lect.getInstructorConstraints()) {
                if (!constraints.add(ic)) continue;
                if (ic instanceof SoftInstructorConstraint)
                    bounds[1] += ((SoftInstructorConstraint)ic).getWorstConflicts();
            }
        }
        return bounds;
    }
    
    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info) {
        super.getInfo(assignment, info);
        double conf = getValue(assignment);
        if (conf > 0.0)
            info.put("Instructor conflicts", sDoubleFormat.format(conf));
    }
    
    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info, Collection<Lecture> variables) {
        super.getInfo(assignment, info, variables);
        double conf = getValue(assignment, variables);
        if (conf > 0.0)
            info.put("Instructor conflicts", sDoubleFormat.format(conf));
    }
}
