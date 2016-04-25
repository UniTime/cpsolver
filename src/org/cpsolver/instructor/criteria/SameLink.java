package org.cpsolver.instructor.criteria;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.AbstractCriterion;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.instructor.constraints.SameLinkConstraint;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;

public class SameLink extends AbstractCriterion<TeachingRequest, TeachingAssignment> {

    public SameLink() {
        setValueUpdateType(ValueUpdateType.NoUpdate);
    }

    @Override
    public double getWeightDefault(DataProperties config) {
        return 100.0;
    }

    @Override
    public double getValue(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        double ret = 0.0;
        for (Constraint<TeachingRequest, TeachingAssignment> c : value.variable().constraints())
            if (c instanceof SameLinkConstraint)
                ret += ((SameLinkConstraint)c).getCurrentPreference(assignment, value);
        return ret;
    }

    @Override
    public double getValue(Assignment<TeachingRequest, TeachingAssignment> assignment, Collection<TeachingRequest> variables) {
        double ret = 0;
        Set<Constraint<TeachingRequest, TeachingAssignment>> constraints = new HashSet<Constraint<TeachingRequest, TeachingAssignment>>();
        for (TeachingRequest req: variables) {
            for (Constraint<TeachingRequest, TeachingAssignment> c : req.constraints()) {
                if (c instanceof SameLinkConstraint && constraints.add(c))
                    ret += ((SameLinkConstraint)c).getContext(assignment).getPreference();
            }
        }
        return ret;
    }
    
    @Override
    protected double[] computeBounds(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (Constraint<TeachingRequest, TeachingAssignment> c: getModel().constraints()) {
            if (c instanceof SameLinkConstraint && !c.isHard())
                bounds[1] += Math.abs(((SameLinkConstraint)c).getPreference()) * (c.variables().size() - 1);
        }
        return bounds;
    }
    
    @Override
    public double[] getBounds(Assignment<TeachingRequest, TeachingAssignment> assignment, Collection<TeachingRequest> variables) {
        double[] bounds = new double[] { 0.0, 0.0 };
        Set<Constraint<TeachingRequest, TeachingAssignment>> constraints = new HashSet<Constraint<TeachingRequest, TeachingAssignment>>();
        for (TeachingRequest req: variables) {
            for (Constraint<TeachingRequest, TeachingAssignment> c : req.constraints()) {
                if (c instanceof SameLinkConstraint && !c.isHard() && constraints.add(c))
                    bounds[1] += Math.abs(((SameLinkConstraint)c).getPreference()) * (c.variables().size() - 1);
            }
        }
        return bounds;
    }

    @Override
    public String getAbbreviation() {
        return "Link";
    }
}