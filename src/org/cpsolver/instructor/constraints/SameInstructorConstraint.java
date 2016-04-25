package org.cpsolver.instructor.constraints;

import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;
import org.cpsolver.instructor.criteria.SameInstructor;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;

public class SameInstructorConstraint extends ConstraintWithContext<TeachingRequest, TeachingAssignment, SameInstructorConstraint.Context> {
    private Long iId;
    private String iName;
    private int iPreference = 0;
    private boolean iRequired = false, iProhibited = false;
    
    public SameInstructorConstraint(Long id, String name, String preference) {
        iId = id;
        iName = name;
        iPreference = Constants.preference2preferenceLevel(preference);
        if (Constants.sPreferenceRequired.equals(preference)) {
            iRequired = true;
        } else if (Constants.sPreferenceProhibited.equals(preference)) {
            iProhibited = true;
        }
    }
    
    public Long getConstraintId() { return iId; }
    @Override
    public String getName() { return iName; }
    
    public boolean isRequired() { return iRequired; }
    
    public boolean isProhibited() { return iProhibited; }
    
    public int getPreference() { return iPreference; }
    
    @Override
    public boolean isHard() {
        return isRequired() || isProhibited();
    }
    
    public boolean isSatisfiedPair(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment a1, TeachingAssignment a2) {
        if (isRequired() || (!isProhibited() && getPreference() <= 0))
            return a1.getInstructor().equals(a2.getInstructor());
        else if (isProhibited() || (!isRequired() && getPreference() > 0))
            return !a1.getInstructor().equals(a2.getInstructor());
        return true;
    }

    @Override
    public void computeConflicts(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        if (isHard()) {
            for (TeachingRequest request: variables()) {
                if (request.equals(value.variable())) continue;
                
                TeachingAssignment conflict = assignment.getValue(request);
                if (conflict == null) continue;
                
                if (!isSatisfiedPair(assignment, value, conflict))
                    conflicts.add(conflict);
            }
        }
    }
    
    public int getCurrentPreference(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value) {
        if (isHard()) return 0; // no preference
        if (countAssignedVariables(assignment) + (assignment.getValue(value.variable()) == null ? 1 : 0) < 2) return 0; // not enough variables
        int nrViolatedPairsAfter = 0;
        int nrViolatedPairsBefore = 0;
        for (TeachingRequest v1 : variables()) {
            for (TeachingRequest v2 : variables()) {
                if (v1.getId() >= v2.getId()) continue;
                TeachingAssignment p1 = (v1.equals(value.variable()) ? null : assignment.getValue(v1));
                TeachingAssignment p2 = (v2.equals(value.variable()) ? null : assignment.getValue(v2));
                if (p1 != null && p2 != null && !isSatisfiedPair(assignment, p1, p2))
                    nrViolatedPairsBefore ++;
                if (v1.equals(value.variable())) p1 = value;
                if (v2.equals(value.variable())) p2 = value;
                if (p1 != null && p2 != null && !isSatisfiedPair(assignment, p1, p2))
                    nrViolatedPairsAfter ++;
            }
        }
        return (nrViolatedPairsAfter > 0 ? Math.abs(iPreference) * nrViolatedPairsAfter : 0) - (nrViolatedPairsBefore > 0 ? Math.abs(iPreference) * nrViolatedPairsBefore : 0);
    }
    
    public int getCurrentPreference(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        if (isHard()) return 0; // no preference
        if (countAssignedVariables(assignment) < 2) return 0; // not enough variable
        int nrViolatedPairs = 0;
        for (TeachingRequest v1 : variables()) {
            TeachingAssignment p1 = assignment.getValue(v1);
            if (p1 == null) continue;
            for (TeachingRequest v2 : variables()) {
                TeachingAssignment p2 = assignment.getValue(v2);
                if (p2 == null || v1.getId() >= v2.getId()) continue;
                if (!isSatisfiedPair(assignment, p1, p2)) nrViolatedPairs++;
            }
        }
        return (nrViolatedPairs > 0 ? Math.abs(iPreference) * nrViolatedPairs : 0);
    }
    
    @Override
    public Context createAssignmentContext(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        return new Context(assignment);
    }

    public class Context implements AssignmentConstraintContext<TeachingRequest, TeachingAssignment> {
        private int iLastPreference = 0;
        
        public Context(Assignment<TeachingRequest, TeachingAssignment> assignment) {
            updateCriterion(assignment);
        }

        @Override
        public void assigned(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value) {
            updateCriterion(assignment);
        }

        @Override
        public void unassigned(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value) {
            updateCriterion(assignment);
        }
        
        private void updateCriterion(Assignment<TeachingRequest, TeachingAssignment> assignment) {
            if (!isHard()) {
                getModel().getCriterion(SameInstructor.class).inc(assignment, -iLastPreference);
                iLastPreference = getCurrentPreference(assignment);
                getModel().getCriterion(SameInstructor.class).inc(assignment, iLastPreference);
            }
        }
        
        public int getPreference() { return iLastPreference; }
    }
}
