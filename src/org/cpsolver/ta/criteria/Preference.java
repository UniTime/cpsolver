package org.cpsolver.ta.criteria;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.AbstractCriterion;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ta.model.TeachingRequest;
import org.cpsolver.ta.model.TeachingAssignment;

public class Preference extends AbstractCriterion<TeachingRequest, TeachingAssignment> {

    @Override
    public double getWeightDefault(DataProperties config) {
        return 1.0;
    }

    @Override
    public double getValue(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        switch (value.getStudent().getPreference(value.variable())) {
            case 0:
                return -1.0;
            case 1:
                return -0.8;
            case 2:
                return -0.5;
            default:
                return 0;
        }
    }

    @Override
    public String toString(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        return "Pref:" + sDoubleFormat.format(-getValue(assignment));
    }

}
