package org.cpsolver.instructor.criteria;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.instructor.model.Instructor;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;

/**
 * Unused Instructor Load. If an instructor is being used (has at least one teaching assignment),
 * this criterion can penalize the remaining (unused) load of the instructor. That is the difference
 * between instructor maximal load and the assigned load of the instructor.
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
public class UnusedInstructorLoad extends InstructorSchedulingCriterion {
    
    public UnusedInstructorLoad() {
        setValueUpdateType(ValueUpdateType.NoUpdate);
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 100000.0;
    }

    @Override
    public double getValue(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        Instructor.Context cx = value.getInstructor().getContext(assignment);
        
        float load = cx.getLoad();
        if (!cx.getAssignments().contains(value)) load += value.variable().getRequest().getLoad();
        if (conflicts != null)
            for (TeachingAssignment ta: conflicts)
                if (ta.getInstructor().equals(value.getInstructor())) load -= ta.variable().getRequest().getLoad();
        
        return Math.max(0, value.getInstructor().getMaxLoad() - load);
    }
    
    @Override
    public double getValue(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Collection<TeachingRequest.Variable> variables) {
        double unused = 0.0;
        Set<Instructor> instructors = new HashSet<Instructor>();
        for (TeachingRequest.Variable req: variables) {
            TeachingAssignment ta = assignment.getValue(req);
            if (ta != null &&instructors.add(ta.getInstructor())) {
                unused += ta.getInstructor().getMaxLoad() - ta.getInstructor().getContext(assignment).getLoad();
            }
        }
        return unused;
    }

    @Override
    public String getAbbreviation() {
        return "UnusedLoad";
    }
}
