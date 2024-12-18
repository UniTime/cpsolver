package org.cpsolver.instructor.criteria;

import java.util.Collection;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.instructor.constraints.GroupConstraint;
import org.cpsolver.instructor.constraints.GroupConstraint.Distribution;
import org.cpsolver.instructor.model.Instructor;
import org.cpsolver.instructor.model.InstructorSchedulingModel;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;

/**
 * Distributions criterion. Counting violated soft group constraints. See {@link GroupConstraint}
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Instructor Sectioning)<br>
 *          Copyright (C) 2016 Tomas Muller<br>
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
public class Distributions extends InstructorSchedulingCriterion {
    
    public Distributions() {
        setValueUpdateType(ValueUpdateType.NoUpdate);
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 1.0;
    }

    @Override
    public double getValue(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        double ret = 0.0;
        for (Distribution d: value.getInstructor().getDistributions()) {
            if (!d.isHard()) {
                ret += Math.abs(d.getPenalty()) * d.getType().getValue(d, assignment, value.getInstructor(), value);
            }
        }
        return ret;
    }

    @Override
    public double getValue(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Collection<TeachingRequest.Variable> variables) {
        double value = 0.0;
        for (Instructor instructor: getAssignedInstructors(assignment, variables))
            for (Distribution d: instructor.getDistributions())
                if (!d.isHard())
                    value += Math.abs(d.getPenalty()) * d.getType().getValue(d, assignment, instructor, null);
        return value;
    }
    
    @Override
    protected double[] computeBounds(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (Instructor instructor: ((InstructorSchedulingModel)getModel()).getInstructors()) {
            for (Distribution d: instructor.getDistributions())
                if (!d.isHard())
                    bounds[1] += Math.abs(d.getPenalty());
        }
        return bounds;
    }
    
    @Override
    public double[] getBounds(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Collection<TeachingRequest.Variable> variables) {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (Instructor instructor: getInstructors(assignment, variables)) {
            for (Distribution d: instructor.getDistributions())
                if (!d.isHard())
                    bounds[1] += Math.abs(d.getPenalty());
        }
        return bounds;
    }
}
