package org.cpsolver.studentsct.heuristics.selection;

import java.util.ArrayList;
import java.util.List;


import org.apache.logging.log4j.Logger;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.StudentPreferencePenalties;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Subpart;

/**
 * Section given student using branch &amp; bound algorithm with no unassignments
 * allowed.
 * 
 * <br>
 * <br>
 * Parameters: <br>
 * <table border='1'><caption>Related Solver Parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>Sectioning.UseStudentPreferencePenalties</td>
 * <td>{@link Boolean}</td>
 * <td>If true, {@link StudentPreferencePenalties} are used</td>
 * </tr>
 * <tr>
 * <td>Sectioning.Distribution</td>
 * <td>{@link Integer}</td>
 * <td>When student preference penalties are used, defines which distribution is
 * to be used (one of {@link StudentPreferencePenalties#sDistTypePreference},
 * {@link StudentPreferencePenalties#sDistTypePreferenceQuadratic},
 * {@link StudentPreferencePenalties#sDistTypePreferenceReverse},
 * {@link StudentPreferencePenalties#sDistTypeUniform})</td>
 * </tr>
 * <tr>
 * <td>Sectioning.UseOnlinePenalties</td>
 * <td>{@link Boolean}</td>
 * <td>If true, online sectioning penalties computed based on held/expected
 * space are used.</td>
 * </tr>
 * <tr>
 * <td>Sectioning.Epsilon</td>
 * <td>{@link Double}</td>
 * <td>When both online penalties and student preference penalties are used: a
 * solution based on online penalties is computed first, this solution (and the
 * given epsilon) is then used to setup bounds on online penalties for the
 * solution that minimizes student preference penalties. Limit on online penalty
 * is computed as (1+Section.Epsilon) {@link BranchBoundSelection.Selection#getPenalty()}, i.e., only
 * sections with penalty equal or below this limit can be used -- among these
 * the solution that minimizes student preference penalties is computed.</td>
 * </tr>
 * </table>
 * <br>
 * <br>
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2014 Tomas Muller<br>
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

public class OnlineSelection extends BranchBoundSelection {
    private static Logger sLog = org.apache.logging.log4j.LogManager.getLogger(OnlineSelection.class);
    private int iDistributionType = -1;
    private double iEpsilon = 0.05;
    private boolean iUsePenalties = true;
    private boolean iUseStudentPrefPenalties = false;
    private BranchBoundSelection iBranchBound = null;

    /**
     * Constructor
     * 
     * @param properties
     *            configuration
     */
    public OnlineSelection(DataProperties properties) {
        super(properties);
        iUseStudentPrefPenalties = properties.getPropertyBoolean("Sectioning.UseStudentPreferencePenalties",
                iUseStudentPrefPenalties);
        iDistributionType = properties.getPropertyInt("Sectioning.Distribution",
                StudentPreferencePenalties.sDistTypePreference);
        iEpsilon = properties.getPropertyDouble("Sectioning.Epsilon", iEpsilon);
        iUsePenalties = properties.getPropertyBoolean("Sectioning.UseOnlinePenalties", iUsePenalties);
        if (iUseStudentPrefPenalties && !properties.containsPropery("Sectioning.UseOnlinePenalties"))
            iUsePenalties = false;
        if (iUsePenalties || !iUseStudentPrefPenalties)
            iBranchBound = new BranchBoundSelection(properties);
        iMinimizePenalty = true;
    }

    @Override
    public void init(Solver<Request, Enrollment> solver) {
        init(solver, "Online...");
        if (iBranchBound != null)
            iBranchBound.init(solver, "Online...");
    }

    /** Use student preference penalties 
     * @return true if student preference penalties are to be used
     **/
    public boolean isUseStudentPrefPenalties() {
        return iUseStudentPrefPenalties;
    }

    /** Use online penalties 
     * @return true if online penalties are to be used
     **/
    public boolean isUsePenalties() {
        return iUsePenalties;
    }

    /**
     * Set online sectioning penalties to all sections of all courses of the
     * given student
     */
    private static void setPenalties(Assignment<Request, Enrollment> assignment, Student student) {
        for (Request request : student.getRequests()) {
            if (!(request instanceof CourseRequest))
                continue;
            CourseRequest courseRequest = (CourseRequest) request;
            for (Course course : courseRequest.getCourses()) {
                for (Config config : course.getOffering().getConfigs()) {
                    for (Subpart subpart : config.getSubparts()) {
                        for (Section section : subpart.getSections()) {
                            section.setPenalty(section.getOnlineSectioningPenalty(assignment));
                        }
                    }
                }
            }
            courseRequest.clearCache();
        }
    }

    /** Update online sectioning info after the given student is sectioned 
     * @param assignment current assignment
     * @param student student in question
     **/
    public void updateSpace(Assignment<Request, Enrollment> assignment, Student student) {
        for (Request request : student.getRequests()) {
            if (!(request instanceof CourseRequest))
                continue;
            CourseRequest courseRequest = (CourseRequest) request;
            Enrollment enrollment = assignment.getValue(courseRequest);
            if (enrollment == null)
                return; // not enrolled --> no update
            for (Section section : enrollment.getSections()) {
                section.setSpaceHeld(section.getSpaceHeld() - courseRequest.getWeight());
                // sLog.debug("  -- space held for "+section+" decreased by 1 (to "+section.getSpaceHeld()+")");
            }
            List<Enrollment> feasibleEnrollments = new ArrayList<Enrollment>();
            for (Enrollment enrl : courseRequest.values(assignment)) {
                boolean overlaps = false;
                for (Request otherRequest : courseRequest.getStudent().getRequests()) {
                    if (otherRequest.equals(courseRequest) || !(otherRequest instanceof CourseRequest))
                        continue;
                    Enrollment otherErollment = assignment.getValue(otherRequest);
                    if (otherErollment == null)
                        continue;
                    if (enrl.isOverlapping(otherErollment)) {
                        overlaps = true;
                        break;
                    }
                }
                if (!overlaps)
                    feasibleEnrollments.add(enrl);
            }
            double decrement = courseRequest.getWeight() / feasibleEnrollments.size();
            for (Enrollment feasibleEnrollment : feasibleEnrollments) {
                for (Section section : feasibleEnrollment.getSections()) {
                    section.setSpaceExpected(section.getSpaceExpected() - decrement);
                    // sLog.debug("  -- space expected for "+section+" decreased by "+decrement+" (to "+section.getSpaceExpected()+")");
                }
            }
        }
    }

    /**
     * Branch &amp; bound selection for a student
     */
    @Override
    public Selection getSelection(Assignment<Request, Enrollment> assignment, Student student) {
        if (iUsePenalties)
            setPenalties(assignment, student);
        Selection selection = null;
        if (iBranchBound != null)
            selection = iBranchBound.getSelection(assignment, student);
        if (iUseStudentPrefPenalties)
            selection = new EpsilonSelection(student, assignment, selection);
        return selection;
    }

    /**
     * Branch &amp; bound selection for a student
     */
    public class EpsilonSelection extends BranchBoundSelection.Selection {
        private StudentPreferencePenalties iPenalties = null;
        private Selection iSelection = null;

        /**
         * Constructor
         * 
         * @param student
         *            selected student
         * @param assignment current assignment
         * @param selection selection
         */
        public EpsilonSelection(Student student, Assignment<Request, Enrollment> assignment, Selection selection) {
            super(student, assignment);
            iPenalties = new StudentPreferencePenalties(iDistributionType);
            iSelection = selection;
        }

        /**
         * Execute branch &amp; bound, return the best found schedule for the
         * selected student.
         */
        @Override
        public BranchBoundNeighbour select() {
            BranchBoundNeighbour onlineSelection = null;
            if (iSelection != null) {
                onlineSelection = iSelection.select();
                if (sDebug)
                    sLog.debug("Online: " + onlineSelection);
            }
            BranchBoundNeighbour neighbour = super.select();
            if (neighbour != null)
                return neighbour;
            if (onlineSelection != null)
                return onlineSelection;
            return null;
        }

        /** Assignment penalty */
        @Override
        protected double getAssignmentPenalty(int i) {
            return iPenalties.getPenalty(iAssignment[i]) + iDistConfWeight * getDistanceConflicts(i).size();
        }

        public boolean isAllowed(int idx, Enrollment enrollment) {
            if (iSelection == null || iSelection.getBestAssignment() == null
                    || iSelection.getBestAssignment()[idx] == null)
                return true;
            double bestPenalty = iSelection.getBestAssignment()[idx].getPenalty();
            double limit = (iEpsilon < 0 ? Math.max(0, bestPenalty) : (bestPenalty < 0 ? 1 - iEpsilon : 1 + iEpsilon)
                    * bestPenalty);
            if (enrollment.getPenalty() > limit) {
                if (sDebug)
                    sLog.debug("  -- enrollment " + enrollment + " was filtered out " + "(penalty="
                            + enrollment.getPenalty() + ", best=" + bestPenalty + ", limit=" + limit + ")");
                return false;
            }
            return true;
        }

        /** First conflicting enrollment */
        @Override
        public Enrollment firstConflict(int idx, Enrollment enrollment) {
            Enrollment conflict = super.firstConflict(idx, enrollment);
            if (conflict != null)
                return conflict;
            return (isAllowed(idx, enrollment) ? null : enrollment);
        }

        /** Student preference penalties 
         * @return student preference penalties
         **/
        public StudentPreferencePenalties getPenalties() {
            return iPenalties;
        }
    }
}
