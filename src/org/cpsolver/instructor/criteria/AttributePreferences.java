package org.cpsolver.instructor.criteria;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.AbstractCriterion;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;

public class AttributePreferences extends AbstractCriterion<TeachingRequest, TeachingAssignment> {

    @Override
    public double getWeightDefault(DataProperties config) {
        return 1000.0;
    }

    @Override
    public double getValue(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        return value.getAttributePreference();
    }

    @Override
    public String getAbbreviation() {
        return "A";
    }
}
