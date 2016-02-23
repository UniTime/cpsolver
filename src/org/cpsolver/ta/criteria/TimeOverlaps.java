package org.cpsolver.ta.criteria;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.AbstractCriterion;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ta.constraints.Student;
import org.cpsolver.ta.model.TeachingAssignment;
import org.cpsolver.ta.model.TeachingRequest;

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
        return value.getStudent().share(assignment, value);
    }

    @Override
    public double getValue(Assignment<TeachingRequest, TeachingAssignment> assignment, Collection<TeachingRequest> variables) {
        double value = 0.0;
        Set<Student> students = new HashSet<Student>();
        for (Constraint<TeachingRequest, TeachingAssignment> c : getModel().constraints()) {
            if (c instanceof Student) {
                Student s = (Student) c;
                if (students.add(s)) {
                    value += s.getContext(assignment).countTimeOverlaps();
                }
            }
        }
        return value;
    }

    @Override
    public String toString(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        return "Overlap:" + sDoubleFormat.format(getValue(assignment));
    }

}
