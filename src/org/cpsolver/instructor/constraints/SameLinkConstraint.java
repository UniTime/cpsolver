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

/**
 * Same Link Constraint. Much like the {@link SameInstructorConstraint}, but it is possible to assign multiple instructors
 * to the teaching requests of the same link. It is, however, prohibited (or discouraged) for an instructor to teach
 * have teaching requests than the ones of the link. In prohibited / discouraged variant, each request must / should get
 * a different instructor. 
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
public class SameLinkConstraint extends ConstraintWithContext<TeachingRequest.Variable, TeachingAssignment, SameLinkConstraint.Context> {
    private Long iId;
    private String iName;
    private int iPreference = 0;
    private boolean iRequired = false, iProhibited = false;
    
    /**
     * Constructor
     * @param id constraint id
     * @param name link name
     * @param preference constraint preference (R for required, etc.)
     */
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
    
    /**
     * Constraint id that was given in the constructor.
     * @return constraint id
     */
    public Long getConstraintId() { return iId; }
    
    @Override
    public String getName() { return iName; }
    
    @Override
    public String toString() { return "Same Link " + getName(); }
    
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
    
    @Override
    public void computeConflicts(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        if (isHard()) {
            Instructor.Context context = value.getInstructor().getContext(assignment);
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
    
    /**
     * Current constraint preference (if soft)
     * @param assignment current assignment
     * @param value proposed change
     * @return change in the current preference value of this constraint
     */
    public int getCurrentPreference(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, TeachingAssignment value) {
        if (isHard()) return 0; // no preference
        TeachingAssignment current = assignment.getValue(value.variable());
        if (current != null && current.getInstructor().equals(value.getInstructor())) return 0;
        int ret = 0;
        if (getPreference() < 0) { // preferred
            for (TeachingAssignment other : value.getInstructor().getContext(assignment).getAssignments()) {
                if (!variables().equals(value.variable()) && !variables().contains(other.variable())) {
                    ret++;
                }
            }
            if (current != null) {
                for (TeachingAssignment other : current.getInstructor().getContext(assignment).getAssignments()) {
                    if (!variables().equals(value.variable()) && !variables().contains(other.variable())) {
                        ret--;
                    }
                }
            }
        } else if (getPreference() > 0) {
            for (TeachingAssignment other : value.getInstructor().getContext(assignment).getAssignments()) {
                if (!variables().equals(value.variable()) && variables().contains(other.variable()))
                    ret++;
            }
            if (current != null) {
                for (TeachingAssignment other : current.getInstructor().getContext(assignment).getAssignments()) {
                    if (!variables().equals(value.variable()) && variables().contains(other.variable())) {
                        ret--;
                    }
                }
            }
        }
        return ret;
    }
    
    /**
     * Current constraint preference (if soft)
     * @param assignment current assignment
     * @return that is number of requests that are not of this link assigned to the instructors that have at least one request of this link if preferred;
     * number of additional requests of this link given to the same instructor if discouraged
     */
    public int getCurrentPreference(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) {
        if (isHard()) return 0; // no preference
        if (getPreference() < 0) { // preferred
            int ret = 0;
            Set<Instructor> checked = new HashSet<Instructor>();
            for (TeachingRequest.Variable tr: variables()) {
                TeachingAssignment ta = assignment.getValue(tr);
                if (ta == null || !checked.add(ta.getInstructor())) continue;
                Instructor.Context context = ta.getInstructor().getContext(assignment);
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
            for (TeachingRequest.Variable tr: variables()) {
                TeachingAssignment ta = assignment.getValue(tr);
                if (ta == null || !checked.add(ta.getInstructor())) continue;
                Instructor.Context context = ta.getInstructor().getContext(assignment);
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
    public Context createAssignmentContext(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) {
        return new Context(assignment);
    }

    /**
     * Same Link Constraint Context. This context keeps the last preference value and updates the {@link SameLink} criterion.
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
                getModel().getCriterion(SameLink.class).inc(assignment, -iLastPreference);
                iLastPreference = getCurrentPreference(assignment);
                getModel().getCriterion(SameLink.class).inc(assignment, iLastPreference);
            }
        }
        
        /**
         * Current preference value (see {@link SameLinkConstraint#getCurrentPreference(Assignment)})
         * @return current preference value
         */
        public int getPreference() { return iLastPreference; }
    }
}
