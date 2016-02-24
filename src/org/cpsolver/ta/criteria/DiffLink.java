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

public class DiffLink extends AbstractCriterion<TeachingRequest, TeachingAssignment> {

    public DiffLink() {
        setValueUpdateType(ValueUpdateType.NoUpdate);
    }

    @Override
    public double getWeightDefault(DataProperties config) {
        return 10.0;
    }

    @Override
    public double getValue(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        return value.getStudent().diffLinks(assignment, value);
        /*
        if (value.variable().getAssignmentId() < 0 || value.variable().getLink() == null)
            return 0;
        int links = 0;
        for (TeachingRequest clazz : value.getStudent().variables()) {
            TeachingAssignment other = assignment.getValue(clazz);
            if (clazz.equals(value.variable()) || other == null || clazz.getAssignmentId() < 0 || clazz.getLink() == null || !other.getStudent().equals(value.getStudent()))
                continue;
            if (!value.variable().getLink().equals(clazz.getLink()))
                links++;
        }
        return links;
        */
    }

    @Override
    public double getValue(Assignment<TeachingRequest, TeachingAssignment> assignment, Collection<TeachingRequest> variables) {
        double value = 0.0;
        Set<Student> students = new HashSet<Student>();
        for (Constraint<TeachingRequest, TeachingAssignment> c : getModel().constraints()) {
            if (c instanceof Student) {
                Student s = (Student) c;
                if (students.add(s)) {
                    value += s.getContext(assignment).countDiffLinks();
                }
            }
        }
        return value;
    }

    @Override
    public String toString(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        return "Link:" + sDoubleFormat.format(getValue(assignment));
    }

}
