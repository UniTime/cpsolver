package org.cpsolver.ta.criteria;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.AbstractCriterion;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ta.constraints.Student;
import org.cpsolver.ta.model.TeachingRequest;
import org.cpsolver.ta.model.TeachingAssignment;

public class SameSections extends AbstractCriterion<TeachingRequest, TeachingAssignment> {

    public SameSections() {
        setValueUpdateType(ValueUpdateType.NoUpdate);
    }

    @Override
    public double getWeightDefault(DataProperties config) {
        return 10.0;
    }

    @Override
    public double getValue(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        return - value.getStudent().sameSections(assignment, value);
    }

    @Override
    public double getValue(Assignment<TeachingRequest, TeachingAssignment> assignment, Collection<TeachingRequest> variables) {
        double value = 0.0;
        Set<Student> students = new HashSet<Student>();
        for (Constraint<TeachingRequest, TeachingAssignment> c : getModel().constraints()) {
            if (c instanceof Student) {
                Student s = (Student) c;
                if (students.add(s)) {
                    value += s.getContext(assignment).countSameSections();
                }
            }
        }
        return - value;
    }

    @Override
    public String toString(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        return "Same:" + sDoubleFormat.format(-getValue(assignment));
    }

}
