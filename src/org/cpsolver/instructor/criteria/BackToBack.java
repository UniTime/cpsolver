package org.cpsolver.instructor.criteria;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.AbstractCriterion;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.instructor.constraints.InstructorConstraint;
import org.cpsolver.instructor.model.Instructor;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;

public class BackToBack extends AbstractCriterion<TeachingRequest, TeachingAssignment> {
    private double iDiffRoomWeight = 0.8, iDiffTypeWeight = 0.5;

    public BackToBack() {
        setValueUpdateType(ValueUpdateType.NoUpdate);
    }
    
    @Override
    public boolean init(Solver<TeachingRequest, TeachingAssignment> solver) {
        iDiffRoomWeight = solver.getProperties().getPropertyDouble("BackToBack.DifferentRoomWeight", 0.8);
        iDiffTypeWeight = solver.getProperties().getPropertyDouble("BackToBack.DifferentTypeWeight", 0.5);
        return super.init(solver);
    }

    @Override
    public double getWeightDefault(DataProperties config) {
        return 1.0;
    }
    
    public double getDifferentRoomWeight() { return iDiffRoomWeight; }
    public double getDifferentTypeWeight() { return iDiffTypeWeight; }

    @Override
    public double getValue(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        return value.getInstructor().countBackToBacks(assignment, value, iDiffRoomWeight, iDiffTypeWeight);
    }
    
    @Override
    protected double[] computeBounds(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (Constraint<TeachingRequest, TeachingAssignment> c: getModel().constraints()) {
            if (c instanceof InstructorConstraint) {
                bounds[1] += Math.abs(((InstructorConstraint) c).getInstructor().getBackToBackPreference());
            }
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
                    bounds[1] += Math.abs(((InstructorConstraint) c).getInstructor().getBackToBackPreference());
            }
        }
        return bounds;
    }

    @Override
    public double getValue(Assignment<TeachingRequest, TeachingAssignment> assignment, Collection<TeachingRequest> variables) {
        double value = 0.0;
        Set<Instructor> instructors = new HashSet<Instructor>();
        for (Constraint<TeachingRequest, TeachingAssignment> c : getModel().constraints()) {
            if (c instanceof InstructorConstraint) {
                InstructorConstraint ic = (InstructorConstraint) c;
                if (instructors.add(ic.getInstructor())) {
                    value += ic.getContext(assignment).countBackToBackPreference(iDiffRoomWeight, iDiffTypeWeight);
                }
            }
        }
        return value;
    }

    @Override
    public String getAbbreviation() {
        return "B2B";
    }
}
