package org.cpsolver.instructor.criteria;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.instructor.model.Instructor;
import org.cpsolver.instructor.model.InstructorSchedulingModel;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;

/**
 * Different Lecture. If an instructor is teaching two or more assignments of the same course, this criterion counts cases when these
 * assignments do not share a common part (e.g., have a different lecture, counting {@link Instructor#differentLectures(Assignment, TeachingAssignment)}).
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
public class DifferentLecture extends InstructorSchedulingCriterion {

    public DifferentLecture() {
        setValueUpdateType(ValueUpdateType.NoUpdate);
    }

    @Override
    public double getWeightDefault(DataProperties config) {
        return 1000.0;
    }

    @Override
    public double getValue(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        return value.getInstructor().differentLectures(assignment, value);
    }

    @Override
    public double getValue(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Collection<TeachingRequest.Variable> variables) {
        double value = 0.0;
        for (Instructor instructor: getAssignedInstructors(assignment, variables)) {
            value += instructor.getContext(assignment).countDifferentLectures();
        }
        return value;
    }
    
    @Override
    protected double[] computeBounds(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) {
        return new double[] { 0.0, ((InstructorSchedulingModel)getModel()).getInstructors().size() };
    }
    
    @Override
    public double[] getBounds(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Collection<TeachingRequest.Variable> variables) {
        return new double[] { 0.0, getInstructors(assignment, variables).size() };
    }
    
    @Override
    public String getAbbreviation() {
        return "DiffLecture";
    }
    
    @Override
    public void getInfo(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Map<String, String> info) {
        double val = getValue(assignment);
        double[] bounds = getBounds(assignment);
        if (bounds[0] <= val && val <= bounds[1] && bounds[0] < bounds[1])
            info.put("Same Lecture", getPerc(val, bounds[0], bounds[1]) + "% (" + sDoubleFormat.format(bounds[1] - val) + ")");
    }
    
    @Override
    public void getInfo(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Map<String, String> info, Collection<TeachingRequest.Variable> variables) {
        double val = getValue(assignment, variables);
        double[] bounds = getBounds(assignment, variables);
        if (bounds[0] <= val && val <= bounds[1] && bounds[0] < bounds[1])
            info.put("Same Lecture", getPerc(val, bounds[0], bounds[1]) + "% (" + sDoubleFormat.format(bounds[1] - val) + ")");
    }
}
