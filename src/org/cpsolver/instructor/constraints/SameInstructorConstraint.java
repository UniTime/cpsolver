package org.cpsolver.instructor.constraints;

import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;
import org.cpsolver.instructor.criteria.SameInstructor;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;

/**
 * Same Instructor Constraint. Teaching requests linked with this constraint must/should have the same
 * instructor assigned. If discouraged/prohibited, every pair of teaching requests should/must have a different
 * instructor.
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Instructor Sectioning)<br>
 *          Copyright (C) 2016 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          <a href="http://muller.unitime.org">http://muller.unitime.org</a><br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 3 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */
public class SameInstructorConstraint extends ConstraintWithContext<TeachingRequest.Variable, TeachingAssignment, SameInstructorConstraint.Context> {
    private Long iId;
    private String iName;
    private int iPreference = 0;
    private boolean iRequired = false, iProhibited = false;
    
    /**
     * Constructor
     * @param id constraint id
     * @param name constrain (link) name
     * @param preference preference (R for required, P for prohibited, etc.)
     */
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
    
    /**
     * Constraint id that was provided in the constructor
     * @return constraint id
     */
    public Long getConstraintId() { return iId; }
    
    @Override
    public String getName() { return iName; }
    
    @Override
    public String toString() { return "Same Instructor " + getName(); }
    
    /**
     * Is required?
     * @return true if the constraint is required
     */
    public boolean isRequired() { return iRequired; }
    
    /**
     * Is prohibited?
     * @return true if the constraint is prohibited
     */
    public boolean isProhibited() { return iProhibited; }
    
    /**
     * Constraint preference that was provided in the constructor
     * @return constraint preference
     */
    public int getPreference() { return iPreference; }
    
    @Override
    public boolean isHard() {
        return isRequired() || isProhibited();
    }
    
    /**
     * Does a pair of teaching assignments satisfy this constraint?  
     * @param assignment current assignment
     * @param a1 first teaching assignment
     * @param a2 second teaching assignment
     * @return True if the two assignments (of this constraint) have the same instructor (prohibited/preferred case) or a different instructor (prohibited/discouraged case).
     */
    public boolean isSatisfiedPair(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, TeachingAssignment a1, TeachingAssignment a2) {
        if (isRequired() || (!isProhibited() && getPreference() <= 0))
            return a1.getInstructor().equals(a2.getInstructor());
        else if (isProhibited() || (!isRequired() && getPreference() > 0))
            return !a1.getInstructor().equals(a2.getInstructor());
        return true;
    }

    @Override
    public void computeConflicts(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        if (isHard()) {
            for (TeachingRequest.Variable request: variables()) {
                if (request.equals(value.variable())) continue;
                
                TeachingAssignment conflict = assignment.getValue(request);
                if (conflict == null) continue;
                
                if (!isSatisfiedPair(assignment, value, conflict))
                    conflicts.add(conflict);
            }
        }
    }
   
    /**
     * Current constraint preference (if soft)
     * @param assignment current assignment
     * @param value proposed change
     * @return change in the current preference value of this constraint
     */
    public int getCurrentPreference(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, TeachingAssignment value) {
        if (isHard()) return 0; // no preference
        if (countAssignedVariables(assignment) + (assignment.getValue(value.variable()) == null ? 1 : 0) < 2) return 0; // not enough variables
        int nrViolatedPairsAfter = 0;
        int nrViolatedPairsBefore = 0;
        for (TeachingRequest.Variable v1 : variables()) {
            for (TeachingRequest.Variable v2 : variables()) {
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
    
    /**
     * Current constraint preference (if soft)
     * @param assignment current assignment
     * @return number of violated pairs weighted by the absolute value of the preference
     */
    public int getCurrentPreference(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) {
        if (isHard()) return 0; // no preference
        if (countAssignedVariables(assignment) < 2) return 0; // not enough variable
        int nrViolatedPairs = 0;
        for (TeachingRequest.Variable v1 : variables()) {
            TeachingAssignment p1 = assignment.getValue(v1);
            if (p1 == null) continue;
            for (TeachingRequest.Variable v2 : variables()) {
                TeachingAssignment p2 = assignment.getValue(v2);
                if (p2 == null || v1.getId() >= v2.getId()) continue;
                if (!isSatisfiedPair(assignment, p1, p2)) nrViolatedPairs++;
            }
        }
        return (nrViolatedPairs > 0 ? Math.abs(iPreference) * nrViolatedPairs : 0);
    }
    
    @Override
    public Context createAssignmentContext(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) {
        return new Context(assignment);
    }

    /**
     * Same Instructor Constraint Context. This context keeps the last preference value and updates the {@link SameInstructor} criterion.
     *
     */
    public class Context implements AssignmentConstraintContext<TeachingRequest.Variable, TeachingAssignment> {
        private int iLastPreference = 0;
        
        public Context(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) {
            updateCriterion(assignment);
        }

        @Override
        public void assigned(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, TeachingAssignment value) {
            updateCriterion(assignment);
        }

        @Override
        public void unassigned(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, TeachingAssignment value) {
            updateCriterion(assignment);
        }
        
        /**
         * Update the current preference value
         * @param assignment current assignment
         */
        private void updateCriterion(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) {
            if (!isHard()) {
                getModel().getCriterion(SameInstructor.class).inc(assignment, -iLastPreference);
                iLastPreference = getCurrentPreference(assignment);
                getModel().getCriterion(SameInstructor.class).inc(assignment, iLastPreference);
            }
        }
        
        /**
         * Current preference value (see {@link SameInstructorConstraint#getCurrentPreference(Assignment)})
         * @return current preference value
         */
        public int getPreference() { return iLastPreference; }
    }
}
