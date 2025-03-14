package org.cpsolver.studentsct.online.selection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.FreeTimeRequest;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.online.OnlineSectioningModel;
import org.cpsolver.studentsct.online.expectations.OverExpectedCriterion;
import org.cpsolver.studentsct.online.selection.MultiCriteriaBranchAndBoundSelection.SelectionComparator;

/**
 * Computation of suggestions using a limited depth branch and bound.
 * For a given schedule and a selected section, compute
 * possible changes that will be displayed to the student as suggestions. The number
 * of suggestions is limited by Suggestions.MaxSuggestions parameter (defaults to 20).
 * Time is limited by Suggestions.Timeout (defaults to 5000 ms), search depth is limited
 * by Suggestions.MaxDepth parameter (default to 4).
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
public class SuggestionsBranchAndBound {
    private Hashtable<CourseRequest, Set<Section>> iRequiredSections = null;
    private Set<FreeTimeRequest> iRequiredFreeTimes = null;
    private Hashtable<CourseRequest, Set<Section>> iPreferredSections = null;
    private Request iSelectedRequest = null;
    private Section iSelectedSection = null;
    private Student iStudent = null;
    private TreeSet<Suggestion> iSuggestions = new TreeSet<Suggestion>();
    private int iMaxDepth = 4;
    private long iTimeout = 5000;
    private int iMaxSuggestions = 20;
    private long iT0, iT1;
    private boolean iTimeoutReached = false;
    private int iNrSolutionsSeen = 0;
    private OnlineSectioningModel iModel;
    private Assignment<Request, Enrollment> iAssignment;
    private Hashtable<Request, List<Enrollment>> iValues = new Hashtable<Request, List<Enrollment>>();
    private long iLastSuggestionId = 0;
    private SuggestionFilter iFilter = null;
    protected SelectionComparator iComparator = null;
    protected int iMatched = 0;
    protected double iMaxSectionsWithPenalty = 0;

    /**
     * Constructor 
     * @param properties configuration
     * @param student given student
     * @param assignment current assignment
     * @param requiredSections required sections
     * @param requiredFreeTimes required free times (free time requests that must be assigned)
     * @param preferredSections preferred sections
     * @param selectedRequest selected request
     * @param selectedSection selected section
     * @param filter section filter
     * @param maxSectionsWithPenalty maximal number of sections that have a positive over-expectation penalty {@link OverExpectedCriterion#getOverExpected(Assignment, Section, Request)}
     */
    public SuggestionsBranchAndBound(DataProperties properties, Student student,
            Assignment<Request, Enrollment> assignment, Hashtable<CourseRequest, Set<Section>> requiredSections,
            Set<FreeTimeRequest> requiredFreeTimes, Hashtable<CourseRequest, Set<Section>> preferredSections,
            Request selectedRequest, Section selectedSection, SuggestionFilter filter, double maxSectionsWithPenalty) {
        iRequiredSections = requiredSections;
        iRequiredFreeTimes = requiredFreeTimes;
        iPreferredSections = preferredSections;
        iSelectedRequest = selectedRequest;
        iSelectedSection = selectedSection;
        iStudent = student;
        iModel = (OnlineSectioningModel) selectedRequest.getModel();
        iAssignment = assignment;
        iMaxDepth = properties.getPropertyInt("Suggestions.MaxDepth", iMaxDepth);
        iTimeout = properties.getPropertyLong("Suggestions.Timeout", iTimeout);
        iMaxSuggestions = properties.getPropertyInt("Suggestions.MaxSuggestions", iMaxSuggestions);
        iMaxSectionsWithPenalty = maxSectionsWithPenalty;
        iFilter = filter;
        iComparator = new SelectionComparator() {
            private HashMap<Enrollment, Double> iValues = new HashMap<Enrollment, Double>();

            private Double value(Enrollment e) {
                Double value = iValues.get(e);
                if (value == null) {
                    if (iModel.getStudentQuality() != null)
                        value = iModel.getStudentWeights().getWeight(iAssignment, e, iModel.getStudentQuality().conflicts(e));
                    else
                        value = iModel.getStudentWeights().getWeight(iAssignment, e,
                                (iModel.getDistanceConflict() == null ? null : iModel.getDistanceConflict().conflicts(e)),
                                (iModel.getTimeOverlaps() == null ? null : iModel.getTimeOverlaps().conflicts(e)));
                    iValues.put(e, value);
                }
                return value;
            }

            @Override
            public int compare(Assignment<Request, Enrollment> a, Enrollment e1, Enrollment e2) {
                return value(e2).compareTo(value(e1));
            }
        };
    }

    /**
     * Return search time
     * @return search time
     */
    public long getTime() {
        return iT1 - iT0;
    }

    /**
     * Was time limit reached
     * @return true if reached
     */
    public boolean isTimeoutReached() {
        return iTimeoutReached;
    }

    /**
     * Number of possible suggestions visited
     * @return a number of solutions seen
     */
    public int getNrSolutionsSeen() {
        return iNrSolutionsSeen;
    }

    /**
     * Perform the search
     * @return an ordered set of possible suggestions
     */
    public TreeSet<Suggestion> computeSuggestions() {
        iT0 = System.currentTimeMillis();
        iTimeoutReached = false;
        iNrSolutionsSeen = 0;
        iSuggestions.clear();

        ArrayList<Request> requests2resolve = new ArrayList<Request>();
        requests2resolve.add(iSelectedRequest);
        TreeSet<Request> altRequests2resolve = new TreeSet<Request>();

        for (Map.Entry<CourseRequest, Set<Section>> entry : iPreferredSections.entrySet()) {
            CourseRequest request = entry.getKey();
            Set<Section> sections = entry.getValue();
            if (!sections.isEmpty() && sections.size() == sections.iterator().next().getSubpart().getConfig().getSubparts().size())
                iAssignment.assign(0, request.createEnrollment(iAssignment, sections));
            else if (!request.equals(iSelectedRequest)) {
                if (sections.isEmpty())
                    altRequests2resolve.add(request);
                else
                    requests2resolve.add(request);
            }
        }

        for (Request request : iStudent.getRequests()) {
            if (iAssignment.getValue(request) == null && request instanceof FreeTimeRequest) {
                FreeTimeRequest ft = (FreeTimeRequest) request;
                Enrollment enrollment = ft.createEnrollment();
                if (iModel.conflictValues(iAssignment, enrollment).isEmpty())
                    iAssignment.assign(0, enrollment);
            }
        }

        for (Request request : iStudent.getRequests()) {
            request.setInitialAssignment(iAssignment.getValue(request));
        }

        backtrack(requests2resolve, altRequests2resolve, 0, iMaxDepth, false);

        iT1 = System.currentTimeMillis();
        return iSuggestions;
    }

    /**
     * Main branch and bound rutine
     * @param requests2resolve remaining requests to assign
     * @param altRequests2resolve alternative requests to assign
     * @param idx current depth
     * @param depth remaining depth
     * @param alt can leave a request unassigned
     */
    protected void backtrack(ArrayList<Request> requests2resolve, TreeSet<Request> altRequests2resolve, int idx,
            int depth, boolean alt) {
        if (!iTimeoutReached && iTimeout > 0 && System.currentTimeMillis() - iT0 > iTimeout)
            iTimeoutReached = true;
        int nrUnassigned = requests2resolve.size() - idx;
        if (nrUnassigned == 0) {
            List<FreeTimeRequest> okFreeTimes = new ArrayList<FreeTimeRequest>();
            double sectionsWithPenalty = 0;
            for (Request r : iStudent.getRequests()) {
                Enrollment e = iAssignment.getValue(r);
                if (iMaxSectionsWithPenalty >= 0 && e != null && r instanceof CourseRequest) {
                    for (Section s : e.getSections())
                        sectionsWithPenalty += iModel.getOverExpected(iAssignment, s, r);
                }
                if (e == null && r instanceof FreeTimeRequest) {
                    FreeTimeRequest ft = (FreeTimeRequest) r;
                    Enrollment enrollment = ft.createEnrollment();
                    if (iModel.conflictValues(iAssignment, enrollment).isEmpty()) {
                        iAssignment.assign(0, enrollment);
                        okFreeTimes.add(ft);
                    }
                }
                if (e != null && e.isCourseRequest() && e.getSections().isEmpty()) {
                    Double minPenalty = null;
                    for (Enrollment other : values(e.getRequest())) {
                        if (!isAllowed(other)) continue;
                        if (e.equals(other)) continue;
                        double penalty = 0.0;
                        for (Section s: other.getSections())
                            penalty += iModel.getOverExpected(iAssignment, s, other.getRequest());
                        if (minPenalty == null || minPenalty > penalty) minPenalty = penalty;
                        if (minPenalty == 0.0) break;
                    }
                    if (minPenalty != null) sectionsWithPenalty += minPenalty;
                }
            }
            if (iMaxSectionsWithPenalty >= 0 && sectionsWithPenalty > iMaxSectionsWithPenalty)
                return;
            Suggestion s = new Suggestion(requests2resolve);
            if (iSuggestions.size() >= iMaxSuggestions && iSuggestions.last().compareTo(s) <= 0)
                return;
            if (iMatched != 1) {
                for (Iterator<Suggestion> i = iSuggestions.iterator(); i.hasNext();) {
                    Suggestion x = i.next();
                    if (x.sameSelectedSection()) {
                        if (x.compareTo(s) <= 0) return;
                        i.remove();
                    }
                }
            }
            s.init();
            iSuggestions.add(s);
            if (iSuggestions.size() > iMaxSuggestions)
                iSuggestions.remove(iSuggestions.last());
            for (FreeTimeRequest ft : okFreeTimes)
                iAssignment.unassign(0, ft);
            return;
        }
        if (!canContinue(requests2resolve, idx, depth))
            return;
        Request request = requests2resolve.get(idx);
        for (Enrollment enrollment : values(request)) {
            if (!canContinueEvaluation())
                break;
            if (!isAllowed(enrollment))
                continue;
            if (enrollment.equals(iAssignment.getValue(request)))
                continue;
            if (enrollment.getAssignments().isEmpty() && alt)
                continue;
            Set<Enrollment> conflicts = iModel.conflictValues(iAssignment, enrollment);
            if (!checkBound(requests2resolve, idx, depth, enrollment, conflicts))
                continue;
            Enrollment current = iAssignment.getValue(request);
            ArrayList<Request> newVariables2resolve = new ArrayList<Request>(requests2resolve);
            for (Iterator<Enrollment> i = conflicts.iterator(); i.hasNext();) {
                Enrollment conflict = i.next();
                iAssignment.unassign(0, conflict.variable());
                if (!newVariables2resolve.contains(conflict.variable()))
                    newVariables2resolve.add(conflict.variable());
            }
            if (current != null)
                iAssignment.unassign(0, current.variable());
            iAssignment.assign(0, enrollment);
            if (enrollment.getAssignments().isEmpty()) {
                if (altRequests2resolve != null && !altRequests2resolve.isEmpty()) {
                    Suggestion lastBefore = (iSuggestions.isEmpty() ? null : iSuggestions.last());
                    int sizeBefore = iSuggestions.size();
                    for (Request r : altRequests2resolve) {
                        newVariables2resolve.add(r);
                        backtrack(newVariables2resolve, null, idx + 1, depth, true);
                        newVariables2resolve.remove(r);
                    }
                    Suggestion lastAfter = (iSuggestions.isEmpty() ? null : iSuggestions.last());
                    int sizeAfter = iSuggestions.size();
                    // did not succeeded with an alternative -> try without it
                    if (sizeBefore == sizeAfter && (sizeAfter < iMaxSuggestions || sizeAfter == 0 || lastAfter.compareTo(lastBefore) == 0))
                        backtrack(newVariables2resolve, altRequests2resolve, idx + 1, depth - 1, alt);
                } else {
                    backtrack(newVariables2resolve, altRequests2resolve, idx + 1, depth - 1, alt);
                }
            } else {
                backtrack(newVariables2resolve, altRequests2resolve, idx + 1, depth - 1, alt);
            }
            if (current == null)
                iAssignment.unassign(0, request);
            else
                iAssignment.assign(0, current);
            for (Iterator<Enrollment> i = conflicts.iterator(); i.hasNext();) {
                Enrollment conflict = i.next();
                iAssignment.assign(0, conflict);
            }
        }
    }

    /**
     * Domain of a request    
     * @param request given request
     * @return possible enrollments (meeting the filter etc)
     */
    protected List<Enrollment> values(final Request request) {
        if (!request.equals(iSelectedRequest) && !request.getStudent().canAssign(iAssignment, request)) {
            if (canLeaveUnassigned(request)) {
                List<Enrollment> values = new ArrayList<Enrollment>();
                Config config = null;
                if (request instanceof CourseRequest)
                    config = ((CourseRequest) request).getCourses().get(0).getOffering().getConfigs().get(0);
                values.add(new Enrollment(request, 0, config, new HashSet<Section>(), iAssignment));
                return values;
            }
            return new ArrayList<Enrollment>();
        }
        List<Enrollment> values = iValues.get(request);
        if (values != null)
            return values;
        if (request instanceof CourseRequest) {
            CourseRequest cr = (CourseRequest) request;
            values = (cr.equals(iSelectedRequest) ? cr.getAvaiableEnrollments(iAssignment) : cr.getAvaiableEnrollmentsSkipSameTime(iAssignment));
            if (cr.equals(iSelectedRequest)) {
                Collections.sort(values, new Comparator<Enrollment>() {
                    @Override
                    public int compare(Enrollment e1, Enrollment e2) {
                        int s1 = 0;
                        for (Section s: e1.getSections())
                            if (((CourseRequest)iSelectedRequest).isSelected(s)) s1++;
                        int s2 = 0;
                        for (Section s: e2.getSections())
                            if (((CourseRequest)iSelectedRequest).isSelected(s)) s2++;
                        if (s1 != s2) return (s1 > s2 ? -1 : 1);
                        
                        if (e1.getRequest().getInitialAssignment() != null) {
                            Enrollment original = e1.getRequest().getInitialAssignment();
                            int x1 = 0;
                            if (original.getCourse().equals(e1.getCourse())) x1 += 100;
                            if (original.getConfig().equals(e1.getConfig())) {
                                x1 += 10;
                                for (Section section: original.getSections()) {
                                    for (Section s: e1.getSections()) {
                                        if (s.getSubpart().getId() == section.getSubpart().getId()) {
                                            if (ToolBox.equals(section.getTime(), s.getTime()) && ToolBox.equals(section.getRooms(), s.getRooms()))
                                                x1 ++;
                                            break;
                                        }
                                    }
                                }
                            }
                            int x2 = 0;
                            if (original.getCourse().equals(e2.getCourse())) x2 += 100;
                            if (original.getConfig().equals(e2.getConfig())) {
                                x2 += 10;
                                for (Section section: original.getSections()) {
                                    for (Section s: e2.getSections()) {
                                        if (s.getSubpart().getId() == section.getSubpart().getId()) {
                                            if (ToolBox.equals(section.getTime(), s.getTime()) && ToolBox.equals(section.getRooms(), s.getRooms()))
                                                x2 ++;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (x1 != x2) {
                                return (x1 > x2 ? -1 : 1);
                            }
                        }
                        
                        return iComparator.compare(iAssignment, e1, e2);
                    }
                });
            } else {
                Collections.sort(values, new Comparator<Enrollment>() {
                    @Override
                    public int compare(Enrollment e1, Enrollment e2) {
                        return iComparator.compare(iAssignment, e1, e2);
                    }
                });
            }
        } else {
            values = new ArrayList<Enrollment>();
            values.add(((FreeTimeRequest) request).createEnrollment());
        }
        if (canLeaveUnassigned(request)) {
            Config config = null;
            if (request instanceof CourseRequest)
                config = ((CourseRequest) request).getCourses().get(0).getOffering().getConfigs().get(0);
            values.add(new Enrollment(request, 0, config, new HashSet<Section>(), iAssignment));
        }
        iValues.put(request, values);
        if (request.equals(iSelectedRequest) && iFilter != null && request instanceof CourseRequest) {
            for (Iterator<Enrollment> i = values.iterator(); i.hasNext();) {
                Enrollment enrollment = i.next();
                if (enrollment.getAssignments() != null && !enrollment.getAssignments().isEmpty()) {
                    boolean match = false;
                    for (Iterator<Section> j = enrollment.getSections().iterator(); j.hasNext();) {
                        Section section = j.next();
                        if (iSelectedSection != null) {
                            if (section.getSubpart().getId() == iSelectedSection.getSubpart().getId()) {
                                if (iFilter.match(enrollment.getCourse(), section)) {
                                    match = true;
                                    break;
                                }
                            }
                            if (section.getSubpart().getConfig().getId() != iSelectedSection.getSubpart().getConfig().getId() &&
                                    section.getSubpart().getInstructionalType().equals(iSelectedSection.getSubpart().getInstructionalType())) {
                                if (iFilter.match(enrollment.getCourse(), section)) {
                                    match = true;
                                    break;
                                }
                            }
                        } else {
                            if (iFilter.match(enrollment.getCourse(), section)) {
                                match = true;
                                break;
                            }
                        }
                    }
                    if (!match)
                        i.remove();
                }
            }
        }
        if (request.equals(iSelectedRequest))
            iMatched = values.size();
        return values;
    }

    /**
     * Termination criterion
     * @param requests2resolve request to resolve
     * @param idx current depth
     * @param depth remaining depth
     * @return true if the search can continue
     */
    protected boolean canContinue(ArrayList<Request> requests2resolve, int idx, int depth) {
        if (depth <= 0)
            return false;
        if (iTimeoutReached)
            return false;
        return true;
    }

    /**
     * Can continue evaluation of possible enrollments
     * @return false if the timeout was reached
     */
    protected boolean canContinueEvaluation() {
        return !iTimeoutReached;
    }

    /**
     * Check bound
     * @param requests2resolve requests to resolve
     * @param idx current depth
     * @param depth remaining depth
     * @param value enrollment in question
     * @param conflicts conflicting enrollments
     * @return false if the branch can be cut
     */
    protected boolean checkBound(ArrayList<Request> requests2resolve, int idx, int depth, Enrollment value,
            Set<Enrollment> conflicts) {
        if (iMaxSectionsWithPenalty < 0.0 && idx > 0 && !conflicts.isEmpty()) return false;
        int nrUnassigned = requests2resolve.size() - idx;
        if ((nrUnassigned + conflicts.size() > depth)) {
            return false;
        }
        for (Enrollment conflict : conflicts) {
            int confIdx = requests2resolve.indexOf(conflict.variable());
            if (confIdx >= 0 && confIdx <= idx)
                return false;
        }
        if (iMaxSectionsWithPenalty >= 0) {
            double sectionsWithPenalty = 0;
            for (Request r : iStudent.getRequests()) {
                Enrollment e = iAssignment.getValue(r);
                if (r.equals(value.variable())) {
                    e = value;
                } else if (conflicts.contains(e)) {
                    e = null;
                }
                if (e != null && e.isCourseRequest()) {
                    sectionsWithPenalty += iModel.getOverExpected(iAssignment, e, value, conflicts);
                }
            }
            if (sectionsWithPenalty > iMaxSectionsWithPenalty)
                return false;
        }
        return true;
    }

    /**
     * Check required sections
     * @param enrollment given enrollment
     * @return true if  the given enrollment allowed
     */
    public boolean isAllowed(Enrollment enrollment) {
        if (iRequiredSections != null && enrollment.getRequest() instanceof CourseRequest) {
            // Obey required sections
            Set<Section> required = iRequiredSections.get(enrollment.getRequest());
            if (required != null && !required.isEmpty()) {
                if (enrollment.getAssignments() == null)
                    return false;
                for (Section r : required)
                    if (!enrollment.getAssignments().contains(r))
                        return false;
            }
        }
        if (enrollment.getRequest().equals(iSelectedRequest)) {
            // Selected request must be assigned
            if (enrollment.getAssignments() == null || enrollment.getAssignments().isEmpty())
                return false;
            // Selected section must be assigned differently
            if (iSelectedSection != null && enrollment.getAssignments().contains(iSelectedSection))
                return false;
        }
        return true;
    }

    /**
     * Can this request be left unassigned
     * @param request given request
     * @return true if can be left unassigned (there is no requirement)
     */
    public boolean canLeaveUnassigned(Request request) {
        if (request instanceof CourseRequest) {
            if (iRequiredSections != null) {
                // Request with required section must be assigned
                Set<Section> required = iRequiredSections.get(request);
                if (required != null && !required.isEmpty())
                    return false;
            }
        } else {
            // Free time is required
            if (iRequiredFreeTimes.contains(request))
                return false;
        }
        // Selected request must be assigned
        if (request.equals(iSelectedRequest))
            return false;
        return true;
    }

    /**
     * Compare two suggestions
     * @param assignment current assignment
     * @param s1 first suggestion
     * @param s2 second suggestion
     * @return true if s1 is better than s2
     */
    protected int compare(Assignment<Request, Enrollment> assignment, Suggestion s1, Suggestion s2) {
        return Double.compare(s1.getValue(), s2.getValue());
    }

    /**
     * Suggestion
     */
    public class Suggestion implements Comparable<Suggestion> {
        private double iValue = 0.0;
        private int iNrUnassigned = 0;
        private int iUnassignedPriority = 0;
        private int iNrChanges = 0;

        private long iId = iLastSuggestionId++;
        private Enrollment[] iEnrollments;
        private Section iSelectedEnrollment = null;
        private boolean iSelectedEnrollmentChangeTime = false;
        private TreeSet<Section> iSelectedSections = new TreeSet<Section>(new EnrollmentSectionComparator());
        private int iSelectedChoice = 0;

        /**
         * Create suggestion
         * @param resolvedRequests assigned requests
         */
        public Suggestion(ArrayList<Request> resolvedRequests) {
            for (Request request : resolvedRequests) {
                Enrollment enrollment = iAssignment.getValue(request);
                if (enrollment.getAssignments().isEmpty()) {
                    iNrUnassigned++;
                    iUnassignedPriority += request.getPriority();
                }
                iValue += (enrollment.getAssignments() == null || enrollment.getAssignments().isEmpty() ? 0.0 : enrollment.toDouble(iAssignment, false));
                if (request.getInitialAssignment() != null && enrollment.isCourseRequest()) {
                    Enrollment original = request.getInitialAssignment();
                    for (Iterator<Section> i = enrollment.getSections().iterator(); i.hasNext();) {
                        Section section = i.next();
                        Section originalSection = null;
                        for (Iterator<Section> j = original.getSections().iterator(); j.hasNext();) {
                            Section x = j.next();
                            if (x.getSubpart().getId() == section.getSubpart().getId()) {
                                originalSection = x;
                                break;
                            }
                        }
                        if (originalSection == null || !ToolBox.equals(section.getTime(), originalSection.getTime())
                                || !ToolBox.equals(section.getRooms(), originalSection.getRooms()))
                            iNrChanges++;
                    }
                    if (!enrollment.getCourse().equals(request.getInitialAssignment().getCourse()))
                        iNrChanges += 100 * (1 + enrollment.getTruePriority());
                    if (!enrollment.getConfig().equals(request.getInitialAssignment().getConfig()))
                        iNrChanges += 10;
                }
            }
            if (iSelectedRequest != null && iSelectedSection != null) {
                Enrollment enrollment = iAssignment.getValue(iSelectedRequest);
                if (enrollment.getAssignments() != null && !enrollment.getAssignments().isEmpty()) {
                    for (Iterator<Section> i = enrollment.getSections().iterator(); i.hasNext();) {
                        Section section = i.next();
                        if (section.getSubpart().getId() == iSelectedSection.getSubpart().getId()) {
                            iSelectedEnrollment = section;
                            break;
                        }
                        if (section.getSubpart().getConfig().getId() != iSelectedSection.getSubpart().getConfig()
                                .getId()
                                && section.getSubpart().getInstructionalType()
                                        .equals(iSelectedSection.getSubpart().getInstructionalType())) {
                            iSelectedEnrollment = section;
                            break;
                        }
                    }
                }
            }
            if (iSelectedEnrollment != null)
                iSelectedEnrollmentChangeTime = !ToolBox.equals(iSelectedEnrollment.getTime(),
                        iSelectedSection.getTime());
            if (iSelectedRequest != null) {
                Enrollment enrollment = iAssignment.getValue(iSelectedRequest);
                if (enrollment.isCourseRequest() && enrollment.getAssignments() != null
                        && !enrollment.getAssignments().isEmpty()) {
                    iSelectedSections.addAll(enrollment.getSections());
                    iSelectedChoice = ((CourseRequest)iSelectedRequest).getCourses().indexOf(enrollment.getCourse());
                }
            }
        }

        /** initialization */
        public void init() {
            iEnrollments = new Enrollment[iStudent.getRequests().size()];
            for (int i = 0; i < iStudent.getRequests().size(); i++) {
                Request r = iStudent.getRequests().get(i);
                iEnrollments[i] = iAssignment.getValue(r);
                if (iEnrollments[i] == null) {
                    Config c = null;
                    if (r instanceof CourseRequest)
                        c = ((CourseRequest) r).getCourses().get(0).getOffering().getConfigs().get(0);
                    iEnrollments[i] = new Enrollment(r, 0, c, null, iAssignment);
                }
            }
        }

        /**
         * Current assignment for the student
         * @return schedule
         */
        public Enrollment[] getEnrollments() {
            return iEnrollments;
        }

        /**
         * Current value
         * @return assignment value
         */
        public double getValue() {
            return iValue;
        }

        /**
         * Number of unassigned requests
         * @return number of unassigned requests
         */
        public int getNrUnassigned() {
            return iNrUnassigned;
        }

        /**
         * Average unassigned priority
         * @return average priority of unassinged requests
         */
        public double getAverageUnassignedPriority() {
            return ((double) iUnassignedPriority) / iNrUnassigned;
        }

        /**
         * Number of changes in this schedule (comparing to the original one)
         * @return number of changes
         */
        public int getNrChanges() {
            return iNrChanges;
        }

        /**
         * Is the same section selected (as in the current assignment)
         * @return true the same section is selected
         */
        public boolean sameSelectedSection() {
            if (iSelectedRequest != null && iSelectedEnrollment != null) {
                Enrollment enrollment = iAssignment.getValue(iSelectedRequest);
                if (enrollment != null && enrollment.getAssignments().contains(iSelectedEnrollment))
                    return true;
                if (iSelectedEnrollmentChangeTime && iSelectedSection.getSubpart().getSections().size() > iMaxSuggestions) {
                    Section selectedEnrollment = null;
                    for (Iterator<Section> i = enrollment.getSections().iterator(); i.hasNext();) {
                        Section section = i.next();
                        if (section.getSubpart().getId() == iSelectedSection.getSubpart().getId()) {
                            selectedEnrollment = section;
                            break;
                        }
                        if (section.getSubpart().getConfig().getId() != iSelectedSection.getSubpart().getConfig().getId() &&
                                section.getSubpart().getInstructionalType().equals(iSelectedSection.getSubpart().getInstructionalType())) {
                            selectedEnrollment = section;
                            break;
                        }
                    }
                    if (selectedEnrollment != null && ToolBox.equals(selectedEnrollment.getTime(), iSelectedEnrollment.getTime()))
                        return true;
                }
            }
            return false;
        }

        @Override
        public int compareTo(Suggestion suggestion) {
            int cmp = Double.compare(getNrUnassigned(), suggestion.getNrUnassigned());
            if (cmp != 0)
                return cmp;
            if (getNrUnassigned() > 0) {
                cmp = Double.compare(suggestion.getAverageUnassignedPriority(), getAverageUnassignedPriority());
                if (cmp != 0)
                    return cmp;
            }
            
            if (iSelectedRequest != null && iSelectedRequest instanceof CourseRequest) {
                int s1 = 0;
                for (Section s: iSelectedSections)
                    if (((CourseRequest)iSelectedRequest).isSelected(s)) s1++;
                int s2 = 0;
                for (Section s: suggestion.iSelectedSections)
                    if (((CourseRequest)iSelectedRequest).isSelected(s)) s2++;
                if (s1 != s2) {
                    return (s1 > s2 ? -1 : 1);
                }
            }
            
            cmp = Double.compare(getNrChanges(), suggestion.getNrChanges());
            if (cmp != 0)
                return cmp;
            
            cmp = Double.compare(iSelectedChoice, suggestion.iSelectedChoice);
            if (cmp != 0)
                return cmp;

            Iterator<Section> i1 = iSelectedSections.iterator();
            Iterator<Section> i2 = suggestion.iSelectedSections.iterator();
            SectionAssignmentComparator c = new SectionAssignmentComparator();
            while (i1.hasNext() && i2.hasNext()) {
                cmp = c.compare(i1.next(), i2.next());
                if (cmp != 0)
                    return cmp;
            }

            cmp = compare(iAssignment, this, suggestion);
            if (cmp != 0)
                return cmp;

            return Double.compare(iId, suggestion.iId);
        }
    }

    /**
     * Enrollment comparator (used to sort enrollments in a domain).
     * Selected sections go first.
     */
    public class EnrollmentSectionComparator implements Comparator<Section> {
        /**
         * Is section s1 parent of section s2 (or a parent of a parent...)
         * @param s1 a section
         * @param s2 a section
         * @return if there is a parent-child relation between the two sections
         */
        public boolean isParent(Section s1, Section s2) {
            Section p1 = s1.getParent();
            if (p1 == null)
                return false;
            if (p1.equals(s2))
                return true;
            return isParent(p1, s2);
        }

        @Override
        public int compare(Section a, Section b) {
            if (iSelectedSection != null && iSelectedSection.getSubpart().getId() == a.getSubpart().getId())
                return -1;
            if (iSelectedSection != null && iSelectedSection.getSubpart().getId() == b.getSubpart().getId())
                return 1;

            if (isParent(a, b))
                return 1;
            if (isParent(b, a))
                return -1;

            int cmp = a.getSubpart().getInstructionalType().compareToIgnoreCase(b.getSubpart().getInstructionalType());
            if (cmp != 0)
                return cmp;

            return Double.compare(a.getId(), b.getId());
        }
    }

    /**
     * Section comparator. Order section by time first, then by room.
     */
    public class SectionAssignmentComparator implements Comparator<Section> {
        @Override
        public int compare(Section a, Section b) {
            TimeLocation t1 = (a == null ? null : a.getTime());
            TimeLocation t2 = (b == null ? null : b.getTime());
            if (t1 != null && t2 != null) {
                for (int i = 0; i < Constants.DAY_CODES.length; i++) {
                    if ((t1.getDayCode() & Constants.DAY_CODES[i]) != 0) {
                        if ((t2.getDayCode() & Constants.DAY_CODES[i]) == 0)
                            return -1;
                    } else if ((t2.getDayCode() & Constants.DAY_CODES[i]) != 0) {
                        return 1;
                    }
                }
                int cmp = Double.compare(t1.getStartSlot(), t2.getStartSlot());
                if (cmp != 0)
                    return cmp;
            }
            String r1 = (a == null || a.getRooms() == null ? null : a.getRooms().toString());
            String r2 = (b == null || b.getRooms() == null ? null : b.getRooms().toString());
            if (r1 != null && r2 != null) {
                int cmp = r1.compareToIgnoreCase(r2);
                if (cmp != 0)
                    return cmp;
            }

            return 0;
        }
    }

    /** 
     * Number of possible enrollments of the selected request
     * @return number of possible enrollment of the selected request (meeting the given filter etc.)
     */
    public int getNrMatched() {
        return iMatched;
    }

    /**
     * Suggestion filter.
     */
    public static interface SuggestionFilter {
        /**
         * Match the given section 
         * @param course given course
         * @param section given section
         * @return true if matching the filter (can be used)
         */
        public boolean match(Course course, Section section);
    }

}
