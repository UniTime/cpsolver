package org.cpsolver.instructor.constraints;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.instructor.model.Instructor.Context;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;

/**
 * Instructor Constraint. This is the main constraint of the problem, ensuring that an instructor gets a consistent list of 
 * assignments. It checks for instructor availability, maximal load, time conflicts, and whether the given assignments are of the same
 * course (if desired).
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
public class InstructorConstraint extends GlobalConstraint<TeachingRequest.Variable, TeachingAssignment> {
    
    /**
     * Constructor
     */
    public InstructorConstraint() {}
    
    @Override
    public void computeConflicts(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        Context context = value.getInstructor().getContext(assignment);

        // Check availability
        if (context.getInstructor().getTimePreference(value.variable().getRequest()).isProhibited()) {
            conflicts.add(value);
            return;
        }

        // Check for overlaps
        for (TeachingAssignment ta : context.getAssignments()) {
            if (ta.variable().equals(value.variable()) || conflicts.contains(ta))
                continue;

            if (ta.variable().getRequest().overlaps(value.variable().getRequest()))
                conflicts.add(ta);
        }

        // Same course and/or common
        for (TeachingAssignment ta : context.getAssignments()) {
            if (ta.variable().equals(value.variable()) || conflicts.contains(ta))
                continue;
            if (ta.variable().getRequest().isSameCourseViolated(value.variable().getRequest()) || ta.variable().getRequest().isSameCommonViolated(value.variable().getRequest()))
                conflicts.add(ta);
        }
        
        // Check load
        float load = value.variable().getRequest().getLoad();
        List<TeachingAssignment> adepts = new ArrayList<TeachingAssignment>();
        for (TeachingAssignment ta : context.getAssignments()) {
            if (ta.variable().equals(value.variable()) || conflicts.contains(ta))
                continue;

            adepts.add(ta);
            load += ta.variable().getRequest().getLoad();
        }
        while (load > context.getInstructor().getMaxLoad()) {
            if (adepts.isEmpty()) {
                conflicts.add(value);
                break;
            }
            TeachingAssignment conflict = ToolBox.random(adepts);
            load -= conflict.variable().getRequest().getLoad();
            adepts.remove(conflict);
            conflicts.add(conflict);
        }
    }
    
    @Override
    public String getName() {
        return "Instructor Constraint";
    }
    
    @Override
    public String toString() {
        return getName();
    }
}
