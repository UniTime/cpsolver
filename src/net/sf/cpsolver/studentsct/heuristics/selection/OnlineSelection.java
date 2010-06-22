package net.sf.cpsolver.studentsct.heuristics.selection;

import java.util.ArrayList;
import java.util.List;

import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.StudentPreferencePenalties;
import net.sf.cpsolver.studentsct.model.Config;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;
import net.sf.cpsolver.studentsct.model.Subpart;

import org.apache.log4j.Logger;

/**
 * Section given student using branch & bound algorithm with no unassignments
 * allowed.
 * 
 * <br>
 * <br>
 * Parameters: <br>
 * <table border='1'>
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
 * is computed as (1+Section.Epsilon) {@link Selection#getPenalty}, i.e., only
 * sections with penalty equal or below this limit can be used -- among these
 * the solution that minimizes student preference penalties is computed.</td>
 * </tr>
 * </table>
 * <br>
 * <br>
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 */

public class OnlineSelection extends BranchBoundSelection {
    private static Logger sLog = Logger.getLogger(OnlineSelection.class);
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
    }

    /** Use student preference penalties */
    public boolean isUseStudentPrefPenalties() {
        return iUseStudentPrefPenalties;
    }

    /** Use online penalties */
    public boolean isUsePenalties() {
        return iUsePenalties;
    }

    /**
     * Set online sectioning penalties to all sections of all courses of the
     * given student
     */
    private static void setPenalties(Student student) {
        for (Request request : student.getRequests()) {
            if (!(request instanceof CourseRequest))
                continue;
            CourseRequest courseRequest = (CourseRequest) request;
            for (Course course : courseRequest.getCourses()) {
                for (Config config : course.getOffering().getConfigs()) {
                    for (Subpart subpart : config.getSubparts()) {
                        for (Section section : subpart.getSections()) {
                            section.setPenalty(section.getOnlineSectioningPenalty());
                        }
                    }
                }
            }
            courseRequest.clearCache();
        }
    }

    /** Update online sectioning info after the given student is sectioned */
    public void updateSpace(Student student) {
        for (Request request : student.getRequests()) {
            if (!(request instanceof CourseRequest))
                continue;
            CourseRequest courseRequest = (CourseRequest) request;
            if (courseRequest.getAssignment() == null)
                return; // not enrolled --> no update
            Enrollment enrollment = courseRequest.getAssignment();
            for (Section section : enrollment.getSections()) {
                section.setSpaceHeld(section.getSpaceHeld() - courseRequest.getWeight());
                // sLog.debug("  -- space held for "+section+" decreased by 1 (to "+section.getSpaceHeld()+")");
            }
            List<Enrollment> feasibleEnrollments = new ArrayList<Enrollment>();
            for (Enrollment enrl : courseRequest.values()) {
                boolean overlaps = false;
                for (Request otherRequest : courseRequest.getStudent().getRequests()) {
                    if (otherRequest.equals(courseRequest) || !(otherRequest instanceof CourseRequest))
                        continue;
                    Enrollment otherErollment = otherRequest.getAssignment();
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
     * Branch & bound selection for a student
     */
    @Override
    public Selection getSelection(Student student) {
        if (iUsePenalties)
            setPenalties(student);
        Selection selection = null;
        if (iBranchBound != null)
            selection = iBranchBound.getSelection(student);
        if (iUseStudentPrefPenalties)
            selection = new EpsilonSelection(student, selection);
        return selection;
    }

    /**
     * Branch & bound selection for a student
     */
    public class EpsilonSelection extends BranchBoundSelection.Selection {
        private StudentPreferencePenalties iPenalties = null;
        private Selection iSelection = null;

        /**
         * Constructor
         * 
         * @param student
         *            selected student
         */
        public EpsilonSelection(Student student, Selection selection) {
            super(student);
            iPenalties = new StudentPreferencePenalties(iDistributionType);
            iSelection = selection;
        }

        /**
         * Execute branch & bound, return the best found schedule for the
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
            return iPenalties.getPenalty(iAssignment[i]) + iDistConfWeight * getNrDistanceConflicts(i);
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

        /** Student preference penalties */
        public StudentPreferencePenalties getPenalties() {
            return iPenalties;
        }
    }
}
