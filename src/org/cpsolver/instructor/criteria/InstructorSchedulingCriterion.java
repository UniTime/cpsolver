package org.cpsolver.instructor.criteria;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.AbstractCriterion;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.instructor.model.Instructor;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;

/**
 * Abstract instructor scheduling criterion. Implementing {@link Criterion#getInfo(Assignment, Map)} and
 * a few other methods.
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
public abstract class InstructorSchedulingCriterion extends AbstractCriterion<TeachingRequest.Variable, TeachingAssignment> {

    @Override
    public void getInfo(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Map<String, String> info) {
        double val = getValue(assignment);
        double[] bounds = getBounds(assignment);
        if (bounds[0] <= val && val <= bounds[1] && bounds[0] < bounds[1])
            info.put(getName(), getPerc(val, bounds[0], bounds[1]) + "% (" + sDoubleFormat.format(val) + ")");
        else if (bounds[1] <= val && val <= bounds[0] && bounds[1] < bounds[0])
            info.put(getName(), getPercRev(val, bounds[1], bounds[0]) + "% (" + sDoubleFormat.format(val) + ")");
        else if (bounds[0] != val || val != bounds[1])
            info.put(getName(), sDoubleFormat.format(val));
    }
    
    @Override
    public void getInfo(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Map<String, String> info, Collection<TeachingRequest.Variable> variables) {
        double val = getValue(assignment, variables);
        double[] bounds = getBounds(assignment, variables);
        if (bounds[0] <= val && val <= bounds[1] && bounds[0] < bounds[1])
            info.put(getName(), getPerc(val, bounds[0], bounds[1]) + "% (" + sDoubleFormat.format(val) + ")");
        else if (bounds[1] <= val && val <= bounds[0] && bounds[1] < bounds[0])
            info.put(getName(), getPercRev(val, bounds[1], bounds[0]) + "% (" + sDoubleFormat.format(val) + ")");
        else if (bounds[0] != val || val != bounds[1])
            info.put(getName(), sDoubleFormat.format(val));
    }
    
    /**
     * Instructor of a sub-problem
     * @param assignment current instructors
     * @param variables sub-problem
     * @return instructors that can be used by the given teaching requests
     */
    public Set<Instructor> getInstructors(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Collection<TeachingRequest.Variable> variables) {
        Set<Instructor> instructors = new HashSet<Instructor>();
        for (TeachingRequest.Variable req: variables)
            for (TeachingAssignment ta: req.values(assignment))
                instructors.add(ta.getInstructor());
        return instructors;
    }
    
    /**
     * Assigned instructors of a sub-problem
     * @param assignment current instructors
     * @param variables sub-problem
     * @return instructors that can be used by the given teaching requests
     */
    public Set<Instructor> getAssignedInstructors(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Collection<TeachingRequest.Variable> variables) {
        Set<Instructor> instructors = new HashSet<Instructor>();
        for (TeachingRequest.Variable req: variables) {
            TeachingAssignment ta = assignment.getValue(req);
            if (ta != null)
                instructors.add(ta.getInstructor());
        }
        return instructors;
    }
}
