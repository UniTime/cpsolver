package org.cpsolver.instructor.criteria;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.instructor.model.Instructor;
import org.cpsolver.instructor.model.Section;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;

/**
 * Time overlaps. This criterion counts slots during which an instructor has to teach (or attend) two things that are overlapping in time
 * (using {@link Instructor#share(TeachingRequest)}). The time overlaps must be allowed in this case (see {@link Section#isAllowOverlap()}).
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
public class TimeOverlaps extends InstructorSchedulingCriterion {

    public TimeOverlaps() {
        setValueUpdateType(ValueUpdateType.NoUpdate);
    }

    @Override
    public double getWeightDefault(DataProperties config) {
        return 1000.0;
    }

    @Override
    public double getValue(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        return value.getInstructor().share(assignment, value);
    }

    @Override
    public double getValue(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Collection<TeachingRequest.Variable> variables) {
        double value = 0.0;
        for (Instructor instructor: getAssignedInstructors(assignment, variables)) {
            value += instructor.getContext(assignment).countTimeOverlaps();
        }
        return value;
    }
    
    @Override
    public String getAbbreviation() {
        return "Overlaps";
    }
    
    @Override
    public void getInfo(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Map<String, String> info) {
        double val = getValue(assignment);
        if (val != 0)
            info.put(getName(), sDoubleFormat.format(val / 12.0) + " h");
    }

    @Override
    public void getInfo(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Map<String, String> info, Collection<TeachingRequest.Variable> variables) {
        double val = getValue(assignment, variables);
        if (val != 0)
            info.put(getName(), sDoubleFormat.format(val / 12.0) + " h");
    }
}
