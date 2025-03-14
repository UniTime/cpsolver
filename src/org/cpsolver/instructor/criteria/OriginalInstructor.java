package org.cpsolver.instructor.criteria;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;

/**
 * Original Instructor. This criterion penalizes teaching assignments that are not given
 * to the initial instructor (i.e., {@link TeachingRequest} is not assigned to {@link Variable#getInitialAssignment()}).
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
public class OriginalInstructor extends InstructorSchedulingCriterion {
    private boolean iMPP = false;

    public OriginalInstructor() {
    }
    
    @Override
    public void configure(DataProperties properties) {   
        super.configure(properties);
        iMPP = properties.getPropertyBoolean("General.MPP", iMPP);
    }

    @Override
    public double getWeightDefault(DataProperties config) {
        return 100.0;
    }

    @Override
    public double getValue(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        if (iMPP && value.variable().getInitialAssignment() != null) {
            return (value.getInstructor().equals(value.variable().getInitialAssignment().getInstructor()) ? 0.0 : 1.0);
        } else {
            return 0.0;
        }
    }
    
    @Override
    public String getAbbreviation() {
        return "Original";
    }
    
    @Override
    public void getInfo(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Map<String, String> info) {
        double val = getValue(assignment);
        double[] bounds = getBounds(assignment);
        if (bounds[0] <= val && val <= bounds[1] && bounds[0] < bounds[1])
            info.put(getName(), getPerc(val, bounds[0], bounds[1]) + "% (" + sDoubleFormat.format(bounds[1] - val) + ")");
    }
    
    @Override
    public void getInfo(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Map<String, String> info, Collection<TeachingRequest.Variable> variables) {
        double val = getValue(assignment, variables);
        double[] bounds = getBounds(assignment, variables);
        if (bounds[0] <= val && val <= bounds[1] && bounds[0] < bounds[1])
            info.put(getName(), getPerc(val, bounds[0], bounds[1]) + "% (" + sDoubleFormat.format(bounds[1] - val) + ")");
    }
}