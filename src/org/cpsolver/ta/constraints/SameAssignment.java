package org.cpsolver.ta.constraints;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ta.model.TeachingRequest;
import org.cpsolver.ta.model.TeachingAssignment;

public class SameAssignment extends Constraint<TeachingRequest, TeachingAssignment> {
    private Long iAssignmentId = null;

    public SameAssignment(Long id) {
        super();
        iAssignmentId = id;
    }

    public Long getAssignmentId() {
        return iAssignmentId;
    }

    @Override
    public void computeConflicts(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment ta, Set<TeachingAssignment> conflicts) {
        for (TeachingRequest clazz : variables()) {
            if (clazz.equals(ta.variable()))
                continue;
            TeachingAssignment a = assignment.getValue(clazz);
            if (a != null && !ta.getStudent().equals(a.getStudent()))
                conflicts.add(a);
        }
    }

    @Override
    public boolean inConflict(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment ta) {
        for (TeachingRequest clazz : variables()) {
            if (clazz.equals(ta.variable()))
                continue;
            TeachingAssignment a = assignment.getValue(clazz);
            if (a != null && !ta.getStudent().equals(a.getStudent()))
                return true;
        }
        return false;
    }

    @Override
    public boolean isConsistent(TeachingAssignment a1, TeachingAssignment a2) {
        return !a1.variable().getAssignmentId().equals(a2.variable().getAssignmentId()) || a1.getStudent().equals(a2.getStudent());
    }

    @Override
    public String toString() {
        return "SameAssignment{" + getAssignmentId() + "}";
    }
}
