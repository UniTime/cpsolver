package org.cpsolver.instructor.criteria;

import java.util.Collection;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.instructor.model.Instructor;
import org.cpsolver.instructor.model.InstructorSchedulingModel;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;

/**
 * Back to Back. This criterion counts how well are the back-to-back preferences that are set on an {@link Instructor} met
 * (counting {@link Instructor#countBackToBacks(Assignment, TeachingAssignment, double, double)}).
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
public class BackToBack extends InstructorSchedulingCriterion {
    private double iDiffRoomWeight = 0.8, iDiffTypeWeight = 0.5;

    public BackToBack() {
        setValueUpdateType(ValueUpdateType.NoUpdate);
    }
    
    @Override
    public void configure(DataProperties properties) {   
        super.configure(properties);
        iDiffRoomWeight = properties.getPropertyDouble("BackToBack.DifferentRoomWeight", 0.8);
        iDiffTypeWeight = properties.getPropertyDouble("BackToBack.DifferentTypeWeight", 0.5);
    }

    @Override
    public double getWeightDefault(DataProperties config) {
        return 1.0;
    }
    
    /**
     * Different room weight
     * @return penalty for teaching two back-to-back that are in different rooms
     */
    public double getDifferentRoomWeight() { return iDiffRoomWeight; }

    /**
     * Different instructional type weight
     * @return penalty for teaching two back-to-back that are of different instructional type
     */
    public double getDifferentTypeWeight() { return iDiffTypeWeight; }

    @Override
    public double getValue(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        return value.getInstructor().countBackToBacks(assignment, value, iDiffRoomWeight, iDiffTypeWeight);
    }
    
    @Override
    protected double[] computeBounds(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (Instructor instructor: ((InstructorSchedulingModel)getModel()).getInstructors()) {
            bounds[1] += Math.abs(instructor.getBackToBackPreference());
        }
        return bounds;
    }
    
    @Override
    public double[] getBounds(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Collection<TeachingRequest.Variable> variables) {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (Instructor instructor: getInstructors(assignment, variables)) {
            bounds[1] += Math.abs(instructor.getBackToBackPreference());
        }
        return bounds;
    }

    @Override
    public double getValue(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Collection<TeachingRequest.Variable> variables) {
        double value = 0.0;
        for (Instructor instructor: getAssignedInstructors(assignment, variables))
            value += instructor.getContext(assignment).countBackToBackPreference(iDiffRoomWeight, iDiffTypeWeight);
        return value;
    }

    @Override
    public String getAbbreviation() {
        return "Back2Back";
    }
}
