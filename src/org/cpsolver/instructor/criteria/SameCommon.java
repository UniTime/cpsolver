package org.cpsolver.instructor.criteria;

import java.util.Collection;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.instructor.model.Instructor;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;

/**
 * Same Common Preferences. This criterion counts how well are the same common preferences that are set on a {@link TeachingRequest} met
 * (counting {@link TeachingRequest#getSameCommonPenalty(TeachingRequest)}).
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
public class SameCommon extends InstructorSchedulingCriterion {
    
    public SameCommon() {
        setValueUpdateType(ValueUpdateType.NoUpdate);
    }

    @Override
    public double getWeightDefault(DataProperties config) {
        return 1000000.0;
    }

    @Override
    public double getValue(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        Instructor.Context context = value.getInstructor().getContext(assignment);
        double penalty = 0.0;
        int pairs = 0;
        for (TeachingAssignment ta : context.getAssignments()) {
            if (ta.variable().equals(value.variable()))
                continue;
            penalty += value.variable().getRequest().getSameCommonPenalty(ta.variable().getRequest());
            pairs ++;
        }
        return (pairs == 0 ? 0.0 : penalty / pairs);
    }
    
    @Override
    public double[] getBounds(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Collection<TeachingRequest.Variable> variables) {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (TeachingRequest.Variable req: variables) {
            if (!req.getRequest().isSameCommonProhibited() && !req.getRequest().isSameCommonRequired())
                if (req.getRequest().getSameCommonPreference() < 0) {
                    bounds[0] += req.getRequest().getSameCommonPreference();
                } else {
                    bounds[1] += req.getRequest().getSameCommonPreference();
                }
        }
        return bounds;
    }
    
    @Override
    public double getValue(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Collection<TeachingRequest.Variable> variables) {
        return 0.5 * super.getValue(assignment, variables);
    }

    @Override
    public String getAbbreviation() {
        return "SameCommon";
    }
}
