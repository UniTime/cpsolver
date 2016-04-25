package org.cpsolver.instructor.constraints;

import java.util.HashSet;
import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;
import org.cpsolver.instructor.criteria.SameLink;
import org.cpsolver.instructor.model.Instructor;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;

public class SameLinkConstraint extends ConstraintWithContext<TeachingRequest, TeachingAssignment, SameLinkConstraint.Context> {
    private Long iId;
    private String iName;
    private int iPreference = 0;
    private boolean iRequired = false, iProhibited = false;
    
    public SameLinkConstraint(Long id, String name, String preference) {
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
    
    @Override
    public void computeConflicts(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        if (isHard()) {
            InstructorConstraint.Context context = value.getInstructor().getConstraint().getContext(assignment);
            for (TeachingAssignment ta : context.getAssignments()) {
                if (ta.variable().equals(value.variable()) || conflicts.contains(ta))
                    continue;
                if (isRequired() && !variables().contains(ta.variable()))
                    conflicts.add(ta);
                if (isProhibited() && variables().contains(ta.variable()))
                    conflicts.add(ta);
            }
        }
    }
    
    public int getCurrentPreference(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value) {
        if (isHard()) return 0; // no preference
        TeachingAssignment current = assignment.getValue(value.variable());
        if (current != null && current.getInstructor().equals(value.getInstructor())) return 0;
        int ret = 0;
        if (getPreference() < 0) { // preferred
            for (TeachingAssignment other : value.getInstructor().getConstraint().getContext(assignment).getAssignments()) {
                if (!variables().equals(value.variable()) && !variables().contains(other.variable())) {
                    ret++;
                }
            }
            if (current != null) {
                for (TeachingAssignment other : current.getInstructor().getConstraint().getContext(assignment).getAssignments()) {
                    if (!variables().equals(value.variable()) && !variables().contains(other.variable())) {
                        ret--;
                    }
                }
            }
        } else if (getPreference() > 0) {
            for (TeachingAssignment other : value.getInstructor().getConstraint().getContext(assignment).getAssignments()) {
                if (!variables().equals(value.variable()) && variables().contains(other.variable()))
                    ret++;
            }
            if (current != null) {
                for (TeachingAssignment other : current.getInstructor().getConstraint().getContext(assignment).getAssignments()) {
                    if (!variables().equals(value.variable()) && variables().contains(other.variable())) {
                        ret--;
                    }
                }
            }
        }
        return ret;
    }
    
    public int getCurrentPreference(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        if (isHard()) return 0; // no preference
        if (getPreference() < 0) { // preferred
            int ret = 0;
            Set<Instructor> checked = new HashSet<Instructor>();
            for (TeachingRequest tr: variables()) {
                TeachingAssignment ta = assignment.getValue(tr);
                if (ta == null || !checked.add(ta.getInstructor())) continue;
                InstructorConstraint.Context context = ta.getInstructor().getConstraint().getContext(assignment);
                for (TeachingAssignment other : context.getAssignments()) {
                    if (!variables().contains(other.variable())) {
                        ret++;
                    }
                }
            }
            return ret;
        } else if (getPreference() > 0) {
            int ret = 0;
            Set<Instructor> checked = new HashSet<Instructor>();
            for (TeachingRequest tr: variables()) {
                TeachingAssignment ta = assignment.getValue(tr);
                if (ta == null || !checked.add(ta.getInstructor())) continue;
                InstructorConstraint.Context context = ta.getInstructor().getConstraint().getContext(assignment);
                for (TeachingAssignment other : context.getAssignments()) {
                    if (!variables().equals(tr) && variables().contains(other.variable())) {
                        ret++;
                    }
                }
            }
            return ret;
        } else {
            return 0;
        }
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
                getModel().getCriterion(SameLink.class).inc(assignment, -iLastPreference);
                iLastPreference = getCurrentPreference(assignment);
                getModel().getCriterion(SameLink.class).inc(assignment, iLastPreference);
            }
        }
        
        public int getPreference() { return iLastPreference; }
    }
}
