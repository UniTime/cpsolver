package org.cpsolver.studentsct.online.selection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.JProf;
import org.cpsolver.studentsct.constraint.LinkedSections;
import org.cpsolver.studentsct.heuristics.selection.OnlineSelection;
import org.cpsolver.studentsct.heuristics.selection.BranchBoundSelection.BranchBoundNeighbour;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.FreeTimeRequest;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Subpart;
import org.cpsolver.studentsct.online.OnlineSectioningModel;

/**
 * Online student sectioning algorithm using multi-criteria selection. It is very
 * similar to the {@link SuggestionSelection}, however, the {link {@link OnlineSelection}
 * is used to compare two solutions and for branching.
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2014 Tomas Muller<br>
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
 *          License along with this library; if not see <a
 *          href='http://www.gnu.org/licenses'>http://www.gnu.org/licenses</a>.
 * 
 */
public class MultiCriteriaBranchAndBoundSelection implements OnlineSectioningSelection {
    protected int iTimeout = 1000;
    protected OnlineSectioningModel iModel = null;
    protected Assignment<Request, Enrollment> iAssignment = null;
    protected SelectionCriterion iComparator = null;
    private boolean iPriorityWeighting = true;
    protected boolean iBranchWhenSelectedHasNoConflict = false;
    private double iMaxOverExpected = -1.0;

    /** Student */
    protected Student iStudent;
    /** Start time */
    protected long iT0;
    /** End time */
    protected long iT1;
    /** Was timeout reached */
    protected boolean iTimeoutReached;
    /** Current assignment */
    protected Enrollment[] iCurrentAssignment;
    /** Best assignment */
    protected Enrollment[] iBestAssignment;
    /** Value cache */
    protected HashMap<CourseRequest, List<Enrollment>> iValues;

    private Set<FreeTimeRequest> iRequiredFreeTimes;
    private Hashtable<CourseRequest, Set<Section>> iPreferredSections;
    private Hashtable<CourseRequest, Config> iRequiredConfig = new Hashtable<CourseRequest, Config>();
    private Hashtable<CourseRequest, Hashtable<Subpart, Section>> iRequiredSection = new Hashtable<CourseRequest, Hashtable<Subpart, Section>>();
    private Set<CourseRequest> iRequiredUnassinged = null;

    public MultiCriteriaBranchAndBoundSelection(DataProperties config) {
        iTimeout = config.getPropertyInt("Neighbour.BranchAndBoundTimeout", iTimeout);
        iPriorityWeighting = config.getPropertyBoolean("StudentWeights.PriorityWeighting", iPriorityWeighting);
        iBranchWhenSelectedHasNoConflict = config.getPropertyBoolean("Students.BranchWhenSelectedHasNoConflict", iBranchWhenSelectedHasNoConflict);
    }

    @Override
    public void setModel(OnlineSectioningModel model) {
        iModel = model;
    }

    @Override
    public void setPreferredSections(Hashtable<CourseRequest, Set<Section>> preferredSections) {
        iPreferredSections = preferredSections;
    }

    public void setTimeout(int timeout) {
        iTimeout = timeout;
    }

    @Override
    public void setRequiredSections(Hashtable<CourseRequest, Set<Section>> requiredSections) {
        if (requiredSections != null) {
            for (Map.Entry<CourseRequest, Set<Section>> entry : requiredSections.entrySet()) {
                Hashtable<Subpart, Section> subSection = new Hashtable<Subpart, Section>();
                iRequiredSection.put(entry.getKey(), subSection);
                for (Section section : entry.getValue()) {
                    if (subSection.isEmpty())
                        iRequiredConfig.put(entry.getKey(), section.getSubpart().getConfig());
                    subSection.put(section.getSubpart(), section);
                }
            }
        }
    }

    @Override
    public void setRequiredFreeTimes(Set<FreeTimeRequest> requiredFreeTimes) {
        iRequiredFreeTimes = requiredFreeTimes;
    }

    public BranchBoundNeighbour select(Assignment<Request, Enrollment> assignment, Student student,
            SelectionCriterion comparator) {
        iStudent = student;
        iComparator = comparator;
        iAssignment = assignment;
        return select();
    }

    @Override
    public BranchBoundNeighbour select(Assignment<Request, Enrollment> assignment, Student student) {
        SelectionCriterion comparator = null;
        if (iPriorityWeighting)
            comparator = new OnlineSectioningCriterion(student, iModel, assignment, iPreferredSections);
        else
            comparator = new EqualWeightCriterion(student, iModel, assignment, iPreferredSections);
        return select(assignment, student, comparator);
    }

    /**
     * Execute branch &amp; bound, return the best found schedule for the selected
     * student.
     */
    public BranchBoundNeighbour select() {
        iT0 = JProf.currentTimeMillis();
        iTimeoutReached = false;
        iCurrentAssignment = new Enrollment[iStudent.getRequests().size()];
        iBestAssignment = null;

        int i = 0;
        for (Request r : iStudent.getRequests())
            iCurrentAssignment[i++] = iAssignment.getValue(r);
        saveBest();
        for (int j = 0; j < iCurrentAssignment.length; j++)
            iCurrentAssignment[j] = null;

        iValues = new HashMap<CourseRequest, List<Enrollment>>();
        backTrack(0);
        iT1 = JProf.currentTimeMillis();
        if (iBestAssignment == null)
            return null;

        return new BranchBoundNeighbour(iStudent, iComparator.getTotalWeight(iAssignment, iBestAssignment),
                iBestAssignment);
    }

    /** Was timeout reached */
    public boolean isTimeoutReached() {
        return iTimeoutReached;
    }

    /** Time (in milliseconds) the branch &amp; bound did run */
    public long getTime() {
        return iT1 - iT0;
    }

    /** Save the current schedule as the best */
    public void saveBest() {
        if (iBestAssignment == null)
            iBestAssignment = new Enrollment[iCurrentAssignment.length];
        for (int i = 0; i < iCurrentAssignment.length; i++)
            iBestAssignment[i] = iCurrentAssignment[i];
    }

    /** True if the enrollment is conflicting */
    public boolean inConflict(final int idx, final Enrollment enrollment) {
        for (GlobalConstraint<Request, Enrollment> constraint : enrollment.variable().getModel().globalConstraints())
            if (constraint.inConflict(iAssignment, enrollment))
                return true;
        for (LinkedSections linkedSections : iStudent.getLinkedSections()) {
            if (linkedSections.inConflict(enrollment, new LinkedSections.EnrollmentAssignment() {
                @Override
                public Enrollment getEnrollment(Request request, int index) {
                    return (index == idx ? enrollment : iCurrentAssignment[index]);
                }
            }) != null)
                return true;
        }
        float credit = enrollment.getCredit();
        for (int i = 0; i < iCurrentAssignment.length; i++) {
            if (iCurrentAssignment[i] != null && i != idx) {
                credit += iCurrentAssignment[i].getCredit();
                if (credit > iStudent.getMaxCredit() || iCurrentAssignment[i].isOverlapping(enrollment))
                    return true;
            }
        }
        if (iMaxOverExpected >= 0.0) {
            double penalty = 0.0;
            for (int i = 0; i < idx; i++) {
                if (iCurrentAssignment[i] != null && iCurrentAssignment[i].getAssignments() != null && iCurrentAssignment[i].isCourseRequest())
                    for (Section section: iCurrentAssignment[i].getSections())
                        penalty += iModel.getOverExpected(iAssignment, iCurrentAssignment, i, section, iCurrentAssignment[i].getRequest());
            }
            if (enrollment.isCourseRequest())
                for (Section section: enrollment.getSections())
                    penalty += iModel.getOverExpected(iAssignment, iCurrentAssignment, idx, section, enrollment.getRequest());
            if (penalty > iMaxOverExpected) return true;
        }
        return !isAllowed(idx, enrollment);
    }

    /** True if the given request can be assigned */
    public boolean canAssign(Request request, int idx) {
        if (iCurrentAssignment[idx] != null)
            return true;
        int alt = 0;
        int i = 0;
        float credit = 0;
        for (Iterator<Request> e = iStudent.getRequests().iterator(); e.hasNext(); i++) {
            Request r = e.next();
            if (r.equals(request))
                credit += r.getMinCredit();
            else if (iCurrentAssignment[i] != null)
                credit += iCurrentAssignment[i].getCredit();
            if (r.equals(request))
                continue;
            if (r.isAlternative()) {
                if (iCurrentAssignment[i] != null || (r instanceof CourseRequest && ((CourseRequest) r).isWaitlist()))
                    alt--;
            } else {
                if (r instanceof CourseRequest && !((CourseRequest) r).isWaitlist() && iCurrentAssignment[i] == null)
                    alt++;
            }
        }
        return (!request.isAlternative() || alt > 0) && (credit <= request.getStudent().getMaxCredit());
    }

    public boolean isAllowed(int idx, Enrollment enrollment) {
        if (enrollment.isCourseRequest()) {
            CourseRequest request = (CourseRequest) enrollment.getRequest();
            if (iRequiredUnassinged != null && iRequiredUnassinged.contains(request)) return false;
            Config reqConfig = iRequiredConfig.get(request);
            if (reqConfig != null) {
                if (!reqConfig.equals(enrollment.getConfig()))
                    return false;
                Hashtable<Subpart, Section> reqSections = iRequiredSection.get(request);
                for (Section section : enrollment.getSections()) {
                    Section reqSection = reqSections.get(section.getSubpart());
                    if (reqSection == null)
                        continue;
                    if (!section.equals(reqSection))
                        return false;
                }
            }
        } else if (iRequiredFreeTimes.contains(enrollment.getRequest())) {
            if (enrollment.getAssignments() == null || enrollment.getAssignments().isEmpty())
                return false;
        }
        return true;
    }

    /** Returns true if the given request can be left unassigned */
    protected boolean canLeaveUnassigned(Request request) {
        if (request instanceof CourseRequest) {
            if (iRequiredConfig.get(request) != null)
                return false;
        } else if (iRequiredFreeTimes.contains(request))
            return false;
        return true;
    }

    /** Returns list of available enrollments for a course request */
    protected List<Enrollment> values(final CourseRequest request) {
        if (iRequiredUnassinged != null && iRequiredUnassinged.contains(request))
            return new ArrayList<Enrollment>();
        List<Enrollment> values = request.getAvaiableEnrollments(iAssignment);
        Collections.sort(values, new Comparator<Enrollment>() {
            @Override
            public int compare(Enrollment o1, Enrollment o2) {
                return iComparator.compare(iAssignment, o1, o2);
            }
        });
        return values;
    }

    /** branch &amp; bound search */
    public void backTrack(int idx) {
        if (iTimeout > 0 && (JProf.currentTimeMillis() - iT0) > iTimeout) {
            iTimeoutReached = true;
            return;
        }
        if (idx == iCurrentAssignment.length) {
            if (iBestAssignment == null || iComparator.compare(iAssignment, iCurrentAssignment, iBestAssignment) < 0)
                saveBest();
            return;
        } else if (iBestAssignment != null
                && !iComparator.canImprove(iAssignment, idx, iCurrentAssignment, iBestAssignment)) {
            return;
        }

        Request request = iStudent.getRequests().get(idx);
        if (!canAssign(request, idx)) {
            backTrack(idx + 1);
            return;
        }

        List<Enrollment> values = null;
        if (request instanceof CourseRequest) {
            CourseRequest courseRequest = (CourseRequest) request;
            if (!courseRequest.getSelectedChoices().isEmpty()) {
                values = courseRequest.getSelectedEnrollments(iAssignment, true);
                if (values != null && !values.isEmpty()) {
                    boolean hasNoConflictValue = false;
                    for (Enrollment enrollment : values) {
                        if (inConflict(idx, enrollment))
                            continue;
                        hasNoConflictValue = true;
                        iCurrentAssignment[idx] = enrollment;
                        backTrack(idx + 1);
                        iCurrentAssignment[idx] = null;
                    }
                    if (hasNoConflictValue && iBranchWhenSelectedHasNoConflict)
                        return;
                }
            }
            values = iValues.get(courseRequest);
            if (values == null) {
                values = values(courseRequest);
                iValues.put(courseRequest, values);
            }
        } else {
            values = request.computeEnrollments(iAssignment);
        }

        boolean hasNoConflictValue = false;
        for (Enrollment enrollment : values) {
            if (inConflict(idx, enrollment))
                continue;
            hasNoConflictValue = true;
            iCurrentAssignment[idx] = enrollment;
            backTrack(idx + 1);
            iCurrentAssignment[idx] = null;
        }

        if (canLeaveUnassigned(request) || (!hasNoConflictValue && request instanceof CourseRequest))
            backTrack(idx + 1);
    }

    /**
     * Enrollment comparator
     */
    public interface SelectionComparator {
        /**
         * Compare two enrollments
         * 
         * @param assignment
         *            current assignment
         * @param e1
         *            first enrollment
         * @param e2
         *            second enrollment
         * @return -1 if the first enrollment is better than the second etc.
         */
        public int compare(Assignment<Request, Enrollment> assignment, Enrollment e1, Enrollment e2);
    }

    /**
     * Multi-criteria selection interface.
     */
    public interface SelectionCriterion extends SelectionComparator {
        /**
         * Compare two solutions
         * 
         * @param assignment
         *            current assignment
         * @param current
         *            current solution
         * @param best
         *            best known solution
         * @return true if current solution is better than the best one
         */
        public int compare(Assignment<Request, Enrollment> assignment, Enrollment[] current, Enrollment[] best);

        /**
         * Bound
         * 
         * @param assignment
         *            current assignment
         * @param idx
         *            current request index
         * @param current
         *            current solution
         * @param best
         *            best known solution
         * @return if the current solution can be extended and possibly improve
         *         upon the best known solution
         */
        public boolean canImprove(Assignment<Request, Enrollment> assignment, int idx, Enrollment[] current,
                Enrollment[] best);

        /**
         * For backward compatibility, return a weighted sum
         * 
         * @param assignment
         *            current assignment
         * @param enrollments
         *            current solution
         * @return solution weight (weighted sum)
         */
        public double getTotalWeight(Assignment<Request, Enrollment> assignment, Enrollment[] enrollments);
    }

    @Override
    public void setRequiredUnassinged(Set<CourseRequest> requiredUnassignedRequests) {
        iRequiredUnassinged = requiredUnassignedRequests;
    }

    @Override
    public void setMaxOverExpected(double maxOverExpected) {
        iMaxOverExpected = maxOverExpected;
    }
}