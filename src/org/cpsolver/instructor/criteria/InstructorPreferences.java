package org.cpsolver.instructor.criteria;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.AbstractCriterion;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;

public class InstructorPreferences extends AbstractCriterion<TeachingRequest, TeachingAssignment> {

    @Override
    public double getWeightDefault(DataProperties config) {
        return 0.1;
    }

    @Override
    public double getValue(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        return value.getInstructorPreference();
    }

    @Override
    public String getAbbreviation() {
        return "P";
    }
}
