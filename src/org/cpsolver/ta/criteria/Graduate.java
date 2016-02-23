package org.cpsolver.ta.criteria;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.AbstractCriterion;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ta.model.TeachingRequest;
import org.cpsolver.ta.model.TeachingAssignment;

public class Graduate extends AbstractCriterion<TeachingRequest, TeachingAssignment> {

    @Override
    public double getWeightDefault(DataProperties config) {
        return 1.0;
    }

    @Override
    public double getValue(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        return (value.getStudent().isGrad() ? 0 : value.variable().getId() < 0 ? value.variable().getLoad() : 10.0 * value.variable().getLoad());
    }

    @Override
    public String toString(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        return "Grad:" + sDoubleFormat.format(getValue(assignment));
    }

}
