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

public class TimeOverlaps extends AbstractCriterion<TeachingRequest, TeachingAssignment> {

    public TimeOverlaps() {
        setValueUpdateType(ValueUpdateType.NoUpdate);
    }

    @Override
    public double getWeightDefault(DataProperties config) {
        return 1000.0;
    }

    @Override
    public double getValue(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        return value.getInstructor().share(assignment, value);
    }

    @Override
    public double getValue(Assignment<TeachingRequest, TeachingAssignment> assignment, Collection<TeachingRequest> variables) {
        double value = 0.0;
        Set<Instructor> instructors = new HashSet<Instructor>();
        for (Constraint<TeachingRequest, TeachingAssignment> c : getModel().constraints()) {
            if (c instanceof InstructorConstraint) {
                InstructorConstraint ic = (InstructorConstraint)c;
                if (instructors.add(ic.getInstructor())) {
                    value += ic.getContext(assignment).countTimeOverlaps();
                }
            }
        }
        return value;
    }
    
    @Override
    public String getAbbreviation() {
        return "O";
    }
}
