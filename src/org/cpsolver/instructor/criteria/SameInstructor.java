package org.cpsolver.instructor.criteria;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.instructor.constraints.SameInstructorConstraint;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;

/**
 * Same Instructor. This criterion counts how well are the soft Same Instructor Constraints (see {@link SameInstructorConstraint}) met.
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
public class SameInstructor extends InstructorSchedulingCriterion {

    public SameInstructor() {
        setValueUpdateType(ValueUpdateType.NoUpdate);
    }

    @Override
    public double getWeightDefault(DataProperties config) {
        return 10.0;
    }

    @Override
    public double getValue(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        double ret = 0.0;
        for (Constraint<TeachingRequest.Variable, TeachingAssignment> c : value.variable().constraints())
            if (c instanceof SameInstructorConstraint)
                ret += ((SameInstructorConstraint)c).getCurrentPreference(assignment, value);
        return ret;
    }

    @Override
    public double getValue(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Collection<TeachingRequest.Variable> variables) {
        double ret = 0;
        Set<Constraint<TeachingRequest.Variable, TeachingAssignment>> constraints = new HashSet<Constraint<TeachingRequest.Variable, TeachingAssignment>>();
        for (TeachingRequest.Variable req: variables) {
            for (Constraint<TeachingRequest.Variable, TeachingAssignment> c : req.constraints()) {
                if (c instanceof SameInstructorConstraint && constraints.add(c))
                    ret += ((SameInstructorConstraint)c).getContext(assignment).getPreference();
            }
        }
        return ret;
    }
    
    @Override
    protected double[] computeBounds(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (Constraint<TeachingRequest.Variable, TeachingAssignment> c: getModel().constraints()) {
            if (c instanceof SameInstructorConstraint && !c.isHard())
                bounds[1] += Math.abs(((SameInstructorConstraint)c).getPreference()) * (1 + (c.variables().size() * (c.variables().size() - 1)) / 2);
        }
        return bounds;
    }
    
    @Override
    public double[] getBounds(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Collection<TeachingRequest.Variable> variables) {
        double[] bounds = new double[] { 0.0, 0.0 };
        Set<Constraint<TeachingRequest.Variable, TeachingAssignment>> constraints = new HashSet<Constraint<TeachingRequest.Variable, TeachingAssignment>>();
        for (TeachingRequest.Variable req: variables) {
            for (Constraint<TeachingRequest.Variable, TeachingAssignment> c : req.constraints()) {
                if (c instanceof SameInstructorConstraint && !c.isHard() && constraints.add(c))
                    bounds[1] += Math.abs(((SameInstructorConstraint)c).getPreference()) * ((c.variables().size() * (c.variables().size() - 1)) / 2);
            }
        }
        return bounds;
    }

    @Override
    public String getAbbreviation() {
        return "SameInstructor";
    }
}