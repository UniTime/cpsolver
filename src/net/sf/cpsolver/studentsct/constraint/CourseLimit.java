package net.sf.cpsolver.studentsct.constraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.ifs.model.GlobalConstraint;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;

/**
 * Course limit constraint. This global constraint ensures that a limit of each
 * course is not exceeded. This means that the total sum of weights of course
 * requests (see {@link Request#getWeight()}) enrolled into a course is below
 * the course's limit (see {@link Course#getLimit()}).
 * 
 * <br>
 * <br>
 * Sections with negative limit are considered unlimited, and therefore
 * completely ignored by this constraint.
 * 
 * <br>
 * <br>
 * Parameters:
 * <table border='1'>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>CourseLimit.PreferDummyStudents</td>
 * <td>{@link Boolean}</td>
 * <td>If true, requests of dummy (last-like) students are preferred to be
 * selected as conflicting.</td>
 * </tr>
 * </table>
 * <br>
 * <br>
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
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
public class CourseLimit extends GlobalConstraint<Request, Enrollment> {
    private boolean iPreferDummyStudents = false;

    /**
     * Constructor
     * 
     * @param cfg
     *            solver configuration
     */
    public CourseLimit(DataProperties cfg) {
        super();
        iPreferDummyStudents = cfg.getPropertyBoolean("CourseLimit.PreferDummyStudents", false);
    }


    /**
     * Enrollment weight of a course if the given request is assigned.
     * 
     * @param course
     *            a course that is of concern
     * @param request
     *            a request of a student to be assigned containing the given
     *            section
     * @return section's new weight
     */
    public static double getEnrollmentWeight(Course course, Request request) {
        return course.getEnrollmentWeight(request) + request.getWeight();
    }

    /**
     * A given enrollment is conflicting, if the course's enrollment
     * (computed by {@link CourseLimit#getEnrollmentWeight(Course, Request)})
     * exceeds the limit. <br>
     * If the limit is breached, one or more existing enrollments are
     * (randomly) selected as conflicting until the overall weight is under the
     * limit.
     * 
     * @param enrollment
     *            {@link Enrollment} that is being considered
     * @param conflicts
     *            all computed conflicting requests are added into this set
     */
    @Override
    public void computeConflicts(Enrollment enrollment, Set<Enrollment> conflicts) {
        // check reservation can assign over the limit
        if (((StudentSectioningModel)getModel()).getReservationCanAssignOverTheLimit() &&
            enrollment.getReservation() != null && enrollment.getReservation().canAssignOverLimit())
            return;

        // enrollment's course
        Course course = enrollment.getCourse();

        // exclude free time requests
        if (course == null)
            return;

        // unlimited course
        if (course.getLimit() < 0)
            return;
        
        // new enrollment weight
        double enrlWeight = getEnrollmentWeight(course, enrollment.getRequest());

        // below limit -> ok
        if (enrlWeight <= course.getLimit())
            return;

        // above limit -> compute adepts (current assignments that are not
        // yet conflicting)
        // exclude all conflicts as well
        List<Enrollment> adepts = new ArrayList<Enrollment>(course.getEnrollments().size());
        for (Enrollment e : course.getEnrollments()) {
            if (e.getRequest().equals(enrollment.getRequest()))
                continue;
            if (conflicts.contains(e))
                enrlWeight -= e.getRequest().getWeight();
            else
                adepts.add(e);
        }

        // while above limit -> pick an adept and make it conflicting
        while (enrlWeight > course.getLimit()) {
            if (adepts.isEmpty()) {
                // no adepts -> enrollment cannot be assigned
                conflicts.add(enrollment);
                break;
            }
            
            // pick adept (prefer dummy students), decrease unreserved space,
            // make conflict
            List<Enrollment> best = new ArrayList<Enrollment>();
            boolean bestDummy = false;
            double bestValue = 0;
            for (Enrollment adept: adepts) {
                boolean dummy = adept.getStudent().isDummy();
                double value = adept.toDouble();
                
                if (iPreferDummyStudents && dummy != bestDummy) {
                    if (dummy) {
                        best.clear();
                        best.add(adept);
                        bestDummy = dummy;
                        bestValue = value;
                    }
                    continue;
                }
                
                if (best.isEmpty() || value > bestValue) {
                    if (best.isEmpty()) best.clear();
                    best.add(adept);
                    bestDummy = dummy;
                    bestValue = value;
                } else if (bestValue == value) {
                    best.add(adept);
                }
            }
            
            Enrollment conflict = ToolBox.random(best);
            adepts.remove(conflict);
            enrlWeight -= conflict.getRequest().getWeight();
            conflicts.add(conflict);
        }
    }

    /**
     * A given enrollment is conflicting, if the course's enrollment (computed by
     * {@link CourseLimit#getEnrollmentWeight(Course, Request)}) exceeds the
     * limit.
     * 
     * @param enrollment
     *            {@link Enrollment} that is being considered
     * @return true, if the enrollment cannot be assigned without exceeding the limit
     */
    @Override
    public boolean inConflict(Enrollment enrollment) {
        // check reservation can assign over the limit
        if (((StudentSectioningModel)getModel()).getReservationCanAssignOverTheLimit() &&
            enrollment.getReservation() != null && enrollment.getReservation().canAssignOverLimit())
            return false;

        // enrollment's course
        Course course = enrollment.getCourse();

        // exclude free time requests
        if (course == null)
            return false;

        // unlimited course
        if (course.getLimit() < 0)
            return false;


        // new enrollment weight
        double enrlWeight = getEnrollmentWeight(course, enrollment.getRequest());
        
        // above limit -> conflict
        return (enrlWeight > course.getLimit());
    }
    
    @Override
    public String toString() {
        return "CourseLimit";
    }

}