package org.cpsolver.instructor.constraints;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.instructor.criteria.BackToBack;
import org.cpsolver.instructor.criteria.DifferentLecture;
import org.cpsolver.instructor.criteria.TimeOverlaps;
import org.cpsolver.instructor.model.Instructor;
import org.cpsolver.instructor.model.Section;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;

/**
 * Instructor Constraint. This is the main constraint of the problem, ensuring that an instructor gets a consistent list of 
 * assignments. It checks for instructor availability, maximal load, time conflicts, and whether the given assignments are of the same
 * course (if desired).
 * 
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
public class InstructorConstraint extends ConstraintWithContext<TeachingRequest, TeachingAssignment, InstructorConstraint.Context> {
    private Instructor iInstructor;
    
    /**
     * Constructor
     * @param instructor instructor for which the constraint is created
     */
    public InstructorConstraint(Instructor instructor) {
        iInstructor = instructor;
    }
    
    /**
     * Instructor of the constraint
     * @return instructor for which the constraint is created
     */
    public Instructor getInstructor() { return iInstructor; }
    
    @Override
    public void computeConflicts(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        if (value.getInstructor().equals(getInstructor())) {
            Context context = getContext(assignment);
            
            // Check availability
            /*
            if (getInstructor().getTimePreference(value.variable()).isProhibited()) {
                conflicts.add(value);
                return;
            }
            */

            // Check for overlaps
            for (TeachingAssignment ta : context.getAssignments()) {
                if (ta.variable().equals(value.variable()) || conflicts.contains(ta))
                    continue;

                if (ta.variable().overlaps(value.variable()))
                    conflicts.add(ta);
            }

            // Same course and/or common
            if (value.variable().getCourse().isExclusive()) {
                boolean sameCommon = value.variable().getCourse().isSameCommon();
                for (TeachingAssignment ta : context.getAssignments()) {
                    if (ta.variable().equals(value.variable()) || conflicts.contains(ta))
                        continue;

                    if (!ta.variable().sameCourse(value.variable()) || (sameCommon && !ta.variable().sameCommon(value.variable())))
                        conflicts.add(ta);
                }
            } else if (value.variable().getCourse().isSameCommon()) {
                for (TeachingAssignment ta : context.getAssignments()) {
                    if (ta.variable().equals(value.variable()) || conflicts.contains(ta))
                        continue;
                    if (ta.variable().sameCourse(value.variable()) && !ta.variable().sameCommon(value.variable()))
                        conflicts.add(ta);
                }
            }
            
            // Check load
            float load = value.variable().getLoad();
            List<TeachingAssignment> adepts = new ArrayList<TeachingAssignment>();
            for (TeachingAssignment ta : context.getAssignments()) {
                if (ta.variable().equals(value.variable()) || conflicts.contains(ta))
                    continue;

                adepts.add(ta);
                load += ta.variable().getLoad();
            }
            while (load > getInstructor().getMaxLoad()) {
                if (adepts.isEmpty()) {
                    conflicts.add(value);
                    break;
                }
                TeachingAssignment conflict = ToolBox.random(adepts);
                load -= conflict.variable().getLoad();
                adepts.remove(conflict);
                conflicts.add(conflict);
            }
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof InstructorConstraint)) return false;
        InstructorConstraint ic = (InstructorConstraint) o;
        return getInstructor().equals(ic.getInstructor());
    }

    @Override
    public int hashCode() {
        return getInstructor().hashCode();
    }
    
    @Override
    public Context createAssignmentContext(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        return new Context(assignment);
    }

    /**
     * Instructor Constraint Context. It keeps the list of current assignments of an instructor.
     */
    public class Context implements AssignmentConstraintContext<TeachingRequest, TeachingAssignment> {
        private HashSet<TeachingAssignment> iAssignments = new HashSet<TeachingAssignment>();
        private int iTimeOverlaps;
        private double iBackToBacks;
        private double iDifferentLectures;
        
        /**
         * Constructor
         * @param assignment current assignment
         */
        public Context(Assignment<TeachingRequest, TeachingAssignment> assignment) {
            for (TeachingRequest request: variables()) {
                TeachingAssignment value = assignment.getValue(request);
                if (value != null)
                    iAssignments.add(value);
            }
            if (!iAssignments.isEmpty())
                updateCriteria(assignment);
        }

        @Override
        public void assigned(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value) {
            if (value.getInstructor().equals(getInstructor())) {
                iAssignments.add(value);
                updateCriteria(assignment);
            }
        }
        
        @Override
        public void unassigned(Assignment<TeachingRequest, TeachingAssignment> assignment, TeachingAssignment value) {
            if (value.getInstructor().equals(getInstructor())) {
                iAssignments.remove(value);
                updateCriteria(assignment);
            }
        }
        
        /**
         * Update optimization criteria
         * @param assignment current assignment
         */
        private void updateCriteria(Assignment<TeachingRequest, TeachingAssignment> assignment) {
            // update back-to-backs
            BackToBack b2b = (BackToBack)getModel().getCriterion(BackToBack.class);
            if (b2b != null) {
                b2b.inc(assignment, -iBackToBacks);
                iBackToBacks = countBackToBackPreference(b2b.getDifferentRoomWeight(), b2b.getDifferentTypeWeight());
                b2b.inc(assignment, iBackToBacks);
            }
            
            // update time overlaps
            Criterion<TeachingRequest, TeachingAssignment> overlaps = getModel().getCriterion(TimeOverlaps.class);
            if (overlaps != null) {
                overlaps.inc(assignment, -iTimeOverlaps);
                iTimeOverlaps = countTimeOverlaps();
                overlaps.inc(assignment, iTimeOverlaps);
            }
            
            // update same lectures
            Criterion<TeachingRequest, TeachingAssignment> diff = getModel().getCriterion(DifferentLecture.class);
            if (diff != null) {
                diff.inc(assignment, -iDifferentLectures);
                iDifferentLectures = countDifferentLectures();
                diff.inc(assignment, iDifferentLectures);
            }

        }
        
        /**
         * Current assignments of this instructor
         * @return current teaching assignments
         */
        public Set<TeachingAssignment> getAssignments() { return iAssignments; }
        
        /**
         * Current load of this instructor
         * @return current load
         */
        public float getLoad() {
            float load = 0;
            for (TeachingAssignment assignment : iAssignments)
                load += assignment.variable().getLoad();
            return load;
        }
        
        /**
         * If there are classes that allow for overlap, the number of such overlapping slots of this instructor
         * @return current time overlaps (number of overlapping slots)
         */
        public int countTimeOverlaps() {
            int share = 0;
            for (TeachingAssignment a1 : iAssignments) {
                for (TeachingAssignment a2 : iAssignments) {
                    if (a1.getId() < a2.getId())
                        share += a1.variable().share(a2.variable());
                }
                share += getInstructor().share(a1.variable());
            }
            return share;
        }

        /**
         * Number of teaching assignments that have a time assignment of this instructor
         * @return current number of teaching assignments that have a time
         */
        public int countAssignmentsWithTime() {
            int ret = 0;
            a1: for (TeachingAssignment a1 : iAssignments) {
                for (Section s1: a1.variable().getSections())
                    if (s1.hasTime()) {
                        ret++; continue a1;
                    }
            }
            return ret;
        }
        
        /**
         * Percentage of common sections that are not same for the instructor (using {@link TeachingRequest#nrSameLectures(TeachingRequest)})
         * @return percentage of pairs of common sections that are not the same
         */
        public double countDifferentLectures() {
            double same = 0;
            int pairs = 0;
            for (TeachingAssignment a1 : iAssignments) {
                for (TeachingAssignment a2 : iAssignments) {
                    if (a1.getId() < a2.getId()) {
                        same += a1.variable().nrSameLectures(a2.variable());
                        pairs++;
                    }
                }
            }
            return (pairs == 0 ? 0.0 : (pairs - same) / pairs);
        }
        
        /**
         * Current back-to-back preference of the instructor (using {@link TeachingRequest#countBackToBacks(TeachingRequest, double, double)})
         * @param diffRoomWeight different room weight
         * @param diffTypeWeight different instructional type weight
         * @return current back-to-back preference
         */
        public double countBackToBackPreference(double diffRoomWeight, double diffTypeWeight) {
            double b2b = 0;
            if (getInstructor().isBackToBackPreferred() || getInstructor().isBackToBackDiscouraged())
                for (TeachingAssignment a1 : iAssignments) {
                    for (TeachingAssignment a2 : iAssignments) {
                        if (a1.getId() >= a2.getId()) continue;
                        if (getInstructor().getBackToBackPreference() < 0) { // preferred
                            b2b += (a1.variable().countBackToBacks(a2.variable(), diffRoomWeight, diffTypeWeight) - 1.0) * getInstructor().getBackToBackPreference();
                        } else {
                            b2b += a1.variable().countBackToBacks(a2.variable(), diffRoomWeight, diffTypeWeight) * getInstructor().getBackToBackPreference();
                        }
                    }
                }
            return b2b;
        }
        
        /**
         * Current back-to-back percentage for this instructor
         * @return percentage of assignments that are back-to-back
         */
        public double countBackToBackPercentage() {
            BackToBack c = (BackToBack)getModel().getCriterion(BackToBack.class);
            if (c == null) return 0.0;
            double b2b = 0.0;
            int pairs = 0;
            for (TeachingAssignment a1 : iAssignments) {
                for (TeachingAssignment a2 : iAssignments) {
                    if (a1.getId() >= a2.getId()) continue;
                    b2b += a1.variable().countBackToBacks(a2.variable(), c.getDifferentRoomWeight(), c.getDifferentTypeWeight());
                    pairs ++;
                }
            }
            return (pairs == 0 ? 0.0 : b2b / pairs);
        }
    }
    
    @Override
    public String toString() {
        return getInstructor().getName();
    }
}
