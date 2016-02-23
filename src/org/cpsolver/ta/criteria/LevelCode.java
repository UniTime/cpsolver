package org.cpsolver.ta.criteria;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.AbstractCriterion;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ta.model.TeachingRequest;
import org.cpsolver.ta.model.TeachingAssignment;

public class LevelCode extends AbstractCriterion<TeachingRequest, TeachingAssignment> {

    @Override
    public double getWeightDefault(DataProperties config) {
        return 1000.0;
    }

    @Override
    public double getValue(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        if (value.getStudent().getLevel() == null) return 0;
        Integer pref = value.variable().getLevels().get(value.getStudent().getLevel());
        return (pref == null ? 0 : -pref);
    }

    @Override
    public String toString(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        return "Lev:" + sDoubleFormat.format(-getValue(assignment));
    }

}
