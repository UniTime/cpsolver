package org.cpsolver.instructor.criteria;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.AbstractCriterion;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.instructor.constraints.InstructorConstraint;
import org.cpsolver.instructor.model.Instructor;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;

/**
 * Different Lecture. If an instructor is teaching two or more assignments of the same course, this criterion counts cases when these
 * assignments do not share a common part (e.g., have a different lecture, counting {@link Instructor#differentLectures(Assignment, TeachingAssignment)}).
 * 
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
public class DifferentLecture extends AbstractCriterion<TeachingRequest, TeachingAssignment> {

    public DifferentLecture() {
        setValueUpdateType(ValueUpdateType.NoUpdate);
    }

    @Override
    public double getWeightDefault(DataProperties config) {
        return 1000.0;
    }

    @Override
    public double getValue(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        return value.getInstructor().differentLectures(assignment, value);
    }

    @Override
    public double getValue(Assignment<TeachingRequest, TeachingAssignment> assignment, Collection<TeachingRequest> variables) {
        double value = 0.0;
        Set<Instructor> instructors = new HashSet<Instructor>();
        for (Constraint<TeachingRequest, TeachingAssignment> c : getModel().constraints()) {
            if (c instanceof InstructorConstraint) {
                InstructorConstraint ic = (InstructorConstraint)c;
                if (instructors.add(ic.getInstructor())) {
                    value += ic.getContext(assignment).countDifferentLectures();
                }
            }
        }
        return value;
    }
    
    @Override
    protected double[] computeBounds(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (Constraint<TeachingRequest, TeachingAssignment> c: getModel().constraints()) {
            if (c instanceof InstructorConstraint)
                bounds[1] ++;
        }
        return bounds;
    }
    
    @Override
    public double[] getBounds(Assignment<TeachingRequest, TeachingAssignment> assignment, Collection<TeachingRequest> variables) {
        double[] bounds = new double[] { 0.0, 0.0 };
        Set<Constraint<TeachingRequest, TeachingAssignment>> constraints = new HashSet<Constraint<TeachingRequest, TeachingAssignment>>();
        for (TeachingRequest req: variables) {
            for (Constraint<TeachingRequest, TeachingAssignment> c : req.constraints()) {
                if (c instanceof InstructorConstraint && constraints.add(c))
                    bounds[1] ++;
            }
        }
        return bounds;
    }
    
    @Override
    public String getAbbreviation() {
        return "DL";
    }
}
