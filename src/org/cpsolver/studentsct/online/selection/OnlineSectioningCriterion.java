package org.cpsolver.studentsct.online.selection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.studentsct.extension.DistanceConflict;
import org.cpsolver.studentsct.extension.StudentQuality;
import org.cpsolver.studentsct.extension.TimeOverlapsCounter;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.FreeTimeRequest;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Subpart;
import org.cpsolver.studentsct.model.Unavailability;
import org.cpsolver.studentsct.online.OnlineSectioningModel;
import org.cpsolver.studentsct.online.selection.MultiCriteriaBranchAndBoundSelection.SelectionCriterion;
import org.cpsolver.studentsct.weights.StudentWeights;

/**
* Multi-criteria selection criterion. This provides a lexicographical order of solutions using the
* following criteria:
* <ul>
* <li>best priority &amp; alternativity ignoring free time requests (a better solution has a higher priority course assigned or does not use alternative request if possible)
* <li>avoid or minimize course time overlaps
* <li>minimise use of over-expected classes (this prevents students of getting into classes that we know will be needed later in the process)
* <li>best priority &amp; alternativity including free time requests (this is to prevent students of gaming the system by adding free time requests)
* <li>maximise selection (preferred sections)
* <li>avoid or minimise time overlaps (for classes that are allowed to overlap and for free time requests)
* <li>avoid or minimise distance conflicts
* <li>avoid classes that have no time assignment (arranged hours)
* <li>balance sections
* <li>minimise class penalties (expressing how much a class is over-expected)
* </ul>
* 
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
public class OnlineSectioningCriterion implements SelectionCriterion {
    private Hashtable<CourseRequest, Set<Section>> iPreferredSections = null;
    private List<TimeToAvoid> iTimesToAvoid = null;
    private OnlineSectioningModel iModel;
    private Student iStudent;
    protected double[] iQalityWeights;

    /**
     * Constructor
     * @param student current student
     * @param model online student sectioning model
     * @param assignment current assignment
     * @param preferredSections preferred sections
     */
    public OnlineSectioningCriterion(Student student, OnlineSectioningModel model,
            Assignment<Request, Enrollment> assignment, Hashtable<CourseRequest, Set<Section>> preferredSections) {
        iStudent = student;
        iModel = model;
        iPreferredSections = preferredSections;
        if (model.getProperties().getPropertyBoolean("OnlineStudentSectioning.TimesToAvoidHeuristics", true)) {
            iTimesToAvoid = new ArrayList<TimeToAvoid>();
            for (Request r : iStudent.getRequests()) {
                if (r instanceof CourseRequest) {
                    List<Enrollment> enrollments = ((CourseRequest) r).getAvaiableEnrollmentsSkipSameTime(assignment);
                    if (enrollments.size() <= 5) {
                        int penalty = (7 - enrollments.size()) * (r.isAlternative() ? 1 : 7 - enrollments.size());
                        for (Enrollment enrollment : enrollments)
                            for (Section section : enrollment.getSections())
                                if (section.getTime() != null)
                                    iTimesToAvoid.add(new TimeToAvoid(section.getTime(), penalty, r.getPriority()));
                    }
                } else if (r instanceof FreeTimeRequest) {
                    iTimesToAvoid.add(new TimeToAvoid(((FreeTimeRequest) r).getTime(), 1, Integer.MAX_VALUE));
                }
            }
            for (Unavailability unavailability: iStudent.getUnavailabilities())
                if (unavailability.getTime() != null)
                    iTimesToAvoid.add(new TimeToAvoid(unavailability.getTime(), 1, Integer.MAX_VALUE));
        }
        iQalityWeights = new double[StudentQuality.Type.values().length];
        for (StudentQuality.Type type: StudentQuality.Type.values()) {
            iQalityWeights[type.ordinal()] = model.getProperties().getPropertyDouble(type.getWeightName(), type.getWeightDefault());
        }
    }

    protected OnlineSectioningModel getModel() {
        return iModel;
    }

    protected Student getStudent() {
        return iStudent;
    }

    protected Set<Section> getPreferredSections(Request request) {
        return iPreferredSections.get(request);
    }

    protected List<TimeToAvoid> getTimesToAvoid() {
        return iTimesToAvoid;
    }

    /**
     * Distance conflicts of idx-th assignment of the current schedule
     */
    public Set<DistanceConflict.Conflict> getDistanceConflicts(Enrollment[] assignment, int idx) {
        if (getModel().getDistanceConflict() == null || assignment[idx] == null)
            return null;
        Set<DistanceConflict.Conflict> dist = getModel().getDistanceConflict().conflicts(assignment[idx]);
        for (int x = 0; x < idx; x++)
            if (assignment[x] != null)
                dist.addAll(getModel().getDistanceConflict().conflicts(assignment[x], assignment[idx]));
        return dist;
    }

    /**
     * Time overlapping conflicts of idx-th assignment of the current schedule
     */
    public Set<TimeOverlapsCounter.Conflict> getTimeOverlappingConflicts(Enrollment[] assignment, int idx) {
        if (getModel().getTimeOverlaps() == null || assignment[idx] == null)
            return null;
        Set<TimeOverlapsCounter.Conflict> overlaps = new HashSet<TimeOverlapsCounter.Conflict>();
        for (int x = 0; x < idx; x++)
            if (assignment[x] != null) {
                overlaps.addAll(getModel().getTimeOverlaps().conflicts(assignment[x], assignment[idx]));
            } else if (getStudent().getRequests().get(x) instanceof FreeTimeRequest)
                overlaps.addAll(getModel().getTimeOverlaps().conflicts(
                        ((FreeTimeRequest) getStudent().getRequests().get(x)).createEnrollment(), assignment[idx]));
        overlaps.addAll(getModel().getTimeOverlaps().notAvailableTimeConflicts(assignment[idx]));
        return overlaps;
    }
    
    public Set<StudentQuality.Conflict> getStudentQualityConflicts(Enrollment[] assignment, int idx) {
        if (getModel().getStudentQuality() == null || assignment[idx] == null)
            return null;
        Set<StudentQuality.Conflict> conflicts = new HashSet<StudentQuality.Conflict>();
        for (StudentQuality.Type t: StudentQuality.Type.values()) {
            for (int x = 0; x < idx; x++)
                if (assignment[x] != null)
                    conflicts.addAll(getModel().getStudentQuality().conflicts(t, assignment[x], assignment[idx]));
            conflicts.addAll(getModel().getStudentQuality().conflicts(t, assignment[idx]));
        }
        return conflicts;
    }

    /**
     * Weight of an assignment. Unlike
     * {@link StudentWeights#getWeight(Assignment, Enrollment, Set, Set)}, only count this
     * side of distance conflicts and time overlaps.
     **/
    @Deprecated
    protected double getWeight(Assignment<Request, Enrollment> assignment, Enrollment enrollment,
            Set<DistanceConflict.Conflict> distanceConflicts, Set<TimeOverlapsCounter.Conflict> timeOverlappingConflicts) {
        double weight = -getModel().getStudentWeights().getWeight(assignment, enrollment);
        if (distanceConflicts != null)
            for (DistanceConflict.Conflict c : distanceConflicts) {
                Enrollment other = (c.getE1().equals(enrollment) ? c.getE2() : c.getE1());
                if (other.getRequest().getPriority() <= enrollment.getRequest().getPriority())
                    weight += getModel().getStudentWeights().getDistanceConflictWeight(assignment, c);
            }
        if (timeOverlappingConflicts != null)
            for (TimeOverlapsCounter.Conflict c : timeOverlappingConflicts) {
                weight += getModel().getStudentWeights().getTimeOverlapConflictWeight(assignment, enrollment, c);
            }
        return enrollment.getRequest().getWeight() * weight;
    }
    
    protected double getWeight(Assignment<Request, Enrollment> assignment, Enrollment enrollment, Set<StudentQuality.Conflict> conflicts) {
        double weight = -getModel().getStudentWeights().getWeight(assignment, enrollment);
        if (conflicts != null)
            for (StudentQuality.Conflict c : conflicts) {
                weight += getModel().getStudentWeights().getStudentQualityConflictWeight(assignment, enrollment, c);
            }
        return enrollment.getRequest().getWeight() * weight;
    }

    public Request getRequest(int index) {
        return (index < 0 || index >= getStudent().getRequests().size() ? null : getStudent().getRequests().get(index));
    }

    public boolean isFreeTime(int index) {
        Request r = getRequest(index);
        return r != null && r instanceof FreeTimeRequest;
    }
    
    @Override
    public int compare(Assignment<Request, Enrollment> assignment, Enrollment[] current, Enrollment[] best) {
        if (best == null)
            return -1;

        // 0. best priority & alternativity ignoring free time requests
        boolean ft = false;
        boolean res = false;
        for (int idx = 0; idx < current.length; idx++) {
            if (isFreeTime(idx)) {
                ft = true;
                continue;
            }
            Request request = getRequest(idx);
            if (request instanceof CourseRequest && ((CourseRequest)request).hasReservations()) res = true;
            if (best[idx] != null && best[idx].getAssignments() != null) {
                if (current[idx] == null || current[idx].getSections() == null)
                    return 1; // higher priority request assigned
                if (best[idx].getTruePriority() < current[idx].getTruePriority())
                    return 1; // less alternative request assigned
                if (best[idx].getTruePriority() > current[idx].getTruePriority())
                    return -1; // less alternative request assigned
            } else {
                if (current[idx] != null && current[idx].getAssignments() != null)
                    return -1; // higher priority request assigned
            }
        }
        
        // 0.1. allowed, but not available sections
        int bestNotAvailable = 0, currentNotAvailable = 0;
        for (int idx = 0; idx < current.length; idx++) {
            if (best[idx] != null && best[idx].getAssignments() != null && best[idx].getRequest() instanceof CourseRequest && best[idx].getReservation() != null && best[idx].getReservation().canAssignOverLimit()) {
                for (Section section: best[idx].getSections()) {
                    if (section.getLimit() == 0)
                        bestNotAvailable ++;
                }
            }
            if (current[idx] != null && current[idx].getAssignments() != null && current[idx].getRequest() instanceof CourseRequest && current[idx].getReservation() != null && current[idx].getReservation().canAssignOverLimit()) {
                for (Section section: current[idx].getSections()) {
                    if (section.getLimit() == 0)
                        currentNotAvailable ++;
                }
            }
        }
        if (bestNotAvailable > currentNotAvailable) return -1;
        if (bestNotAvailable < currentNotAvailable) return 1;

        // 0.5. avoid course time overlaps & unavailabilities
        if (getModel().getStudentQuality() != null) {
            int bestTimeOverlaps = 0, currentTimeOverlaps = 0;
            for (int idx = 0; idx < current.length; idx++) {
                if (best[idx] != null && best[idx].getAssignments() != null && best[idx].getRequest() instanceof CourseRequest) {
                    for (int x = 0; x < idx; x++) {
                        if (best[x] != null && best[x].getAssignments() != null && best[x].getRequest() instanceof CourseRequest)
                            bestTimeOverlaps += getModel().getStudentQuality().penalty(StudentQuality.Type.CourseTimeOverlap, best[x], best[idx]);
                    }
                }
                if (current[idx] != null && current[idx].getAssignments() != null && current[idx].getRequest() instanceof CourseRequest) {
                    for (int x = 0; x < idx; x++) {
                        if (current[x] != null && current[x].getAssignments() != null && current[x].getRequest() instanceof CourseRequest)
                            currentTimeOverlaps += getModel().getStudentQuality().penalty(StudentQuality.Type.CourseTimeOverlap, current[x], current[idx]);
                    }
                }
            }
            for (int idx = 0; idx < current.length; idx++) {
                if (best[idx] != null && best[idx].getAssignments() != null && best[idx].isCourseRequest()) {
                    bestTimeOverlaps += getModel().getStudentQuality().penalty(StudentQuality.Type.Unavailability, best[idx]);
                }
                if (current[idx] != null && current[idx].getAssignments() != null && current[idx].isCourseRequest()) {
                    currentTimeOverlaps += getModel().getStudentQuality().penalty(StudentQuality.Type.Unavailability, current[idx]);
                }
            }
            if (currentTimeOverlaps < bestTimeOverlaps)
                return -1;
            if (bestTimeOverlaps < currentTimeOverlaps)
                return 1;
        } else if (getModel().getTimeOverlaps() != null) {
            int bestTimeOverlaps = 0, currentTimeOverlaps = 0;
            for (int idx = 0; idx < current.length; idx++) {
                if (best[idx] != null && best[idx].getAssignments() != null
                		&& best[idx].getRequest() instanceof CourseRequest) {
                    for (int x = 0; x < idx; x++) {
                        if (best[x] != null && best[x].getAssignments() != null
                        		&& best[x].getRequest() instanceof CourseRequest)
                            bestTimeOverlaps += getModel().getTimeOverlaps().nrConflicts(best[x], best[idx]);
                    }
                    for (int x = 0; x < idx; x++) {
                        if (current[x] != null && current[x].getAssignments() != null
                        		&& current[x].getRequest() instanceof CourseRequest)
                            currentTimeOverlaps += getModel().getTimeOverlaps().nrConflicts(current[x], current[idx]);
                    }
                }
            }
            for (int idx = 0; idx < current.length; idx++) {
                if (best[idx] != null && best[idx].getAssignments() != null && best[idx].isCourseRequest()) {
                    bestTimeOverlaps += getModel().getTimeOverlaps().nrNotAvailableTimeConflicts(best[idx]);
                }
                if (current[idx] != null && current[idx].getAssignments() != null && current[idx].isCourseRequest()) {
                    currentTimeOverlaps += getModel().getTimeOverlaps().nrNotAvailableTimeConflicts(current[idx]);
                }
            }
            if (currentTimeOverlaps < bestTimeOverlaps)
                return -1;
            if (bestTimeOverlaps < currentTimeOverlaps)
                return 1;
        }

        // 1. minimize number of penalties
        double bestPenalties = 0, currentPenalties = 0;
        for (int idx = 0; idx < current.length; idx++) {
            if (best[idx] != null && best[idx].getAssignments() != null && best[idx].isCourseRequest()) {
                for (Section section : best[idx].getSections())
                    bestPenalties += getModel().getOverExpected(assignment, best, idx, section, best[idx].getRequest());
                for (Section section : current[idx].getSections())
                    currentPenalties += getModel().getOverExpected(assignment, current, idx, section, current[idx].getRequest());
            }
        }
        if (currentPenalties < bestPenalties)
            return -1;
        if (bestPenalties < currentPenalties)
            return 1;

        // 2. best priority & alternativity including free time requests
        if (ft) {
            for (int idx = 0; idx < current.length; idx++) {
                if (best[idx] != null && best[idx].getAssignments() != null) {
                    if (current[idx] == null || current[idx].getSections() == null)
                        return 1; // higher priority request assigned
                    if (best[idx].getTruePriority() < current[idx].getTruePriority())
                        return 1; // less alternative request assigned
                    if (best[idx].getTruePriority() > current[idx].getTruePriority())
                        return -1; // less alternative request assigned
                } else {
                    if (current[idx] != null && current[idx].getAssignments() != null)
                        return -1; // higher priority request assigned
                }
            }
        }

        // 3. maximize selection
        int bestSelected = 0, currentSelected = 0;
        for (int idx = 0; idx < current.length; idx++) {
            if (best[idx] != null && best[idx].getAssignments() != null && best[idx].isCourseRequest()) {
                Set<Section> preferred = getPreferredSections(best[idx].getRequest());
                if (preferred != null && !preferred.isEmpty()) {
                    for (Section section : best[idx].getSections())
                        if (preferred.contains(section))
                            bestSelected++;
                    for (Section section : current[idx].getSections())
                        if (preferred.contains(section))
                            currentSelected++;
                }
            }
        }
        if (currentSelected > bestSelected)
            return -1;
        if (bestSelected > currentSelected)
            return 1;
        
        // 3.5 maximize preferences
        double bestSelectedConfigs = 0, currentSelectedConfigs = 0;
        double bestSelectedSections = 0, currentSelectedSections = 0;
        for (int idx = 0; idx < current.length; idx++) {
            if (best[idx] != null && best[idx].getAssignments() != null && best[idx].isCourseRequest()) {
                bestSelectedSections += best[idx].percentSelectedSameSection();
                bestSelectedConfigs += best[idx].percentSelectedSameConfig();
            }
            if (current[idx] != null && current[idx].getAssignments() != null && current[idx].isCourseRequest()) {
                currentSelectedSections += current[idx].percentSelectedSameSection();
                currentSelectedConfigs += current[idx].percentSelectedSameConfig();
            }
        }
        if (0.3 * currentSelectedConfigs + 0.7 * currentSelectedSections > 0.3 * bestSelectedConfigs + 0.7 * bestSelectedSections) return -1;
        if (0.3 * bestSelectedConfigs + 0.7 * bestSelectedSections > 0.3 * currentSelectedConfigs + 0.7 * currentSelectedSections) return 1;
        
        // 3.9 maximize selection with penalization for not followed reservations
        if (res) {
            for (int idx = 0; idx < current.length; idx++) {
                if (best[idx] != null && best[idx].getAssignments() != null) {
                    if (current[idx] == null || current[idx].getSections() == null)
                        return 1; // higher priority request assigned
                    if (best[idx].getAdjustedPriority() < current[idx].getAdjustedPriority())
                        return 1; // less alternative request assigned
                    if (best[idx].getAdjustedPriority() > current[idx].getAdjustedPriority())
                        return -1; // less alternative request assigned
                } else {
                    if (current[idx] != null && current[idx].getAssignments() != null)
                        return -1; // higher priority request assigned
                }
            }
        }
        
        // 3.95 avoid past sections
        double bestPast = 0.0, currentPast = 0.0;
        for (int idx = 0; idx < current.length; idx++) {
            if (best[idx] != null && best[idx].getAssignments() != null) {
                for (Section section : best[idx].getSections()) {
                    if (section.isPast())
                        bestPast += 1.0 / best[idx].getSections().size();
                }
            }
            if (current[idx] != null && current[idx].getAssignments() != null) {
                for (Section section : current[idx].getSections()) {
                    if (section.isPast())
                        currentPast += 1.0 / current[idx].getSections().size();
                }
            }
        }
        if (Math.abs(currentPast - bestPast) > 0.0001) {
            if (currentPast < bestPast)
                return -1;
            if (bestPast < currentPast)
                return 1;
        }

        // 4-5. student quality
        if (getModel().getStudentQuality() != null) {
            double bestQuality = 0, currentQuality = 0;
            for (StudentQuality.Type type: StudentQuality.Type.values()) {
                for (int idx = 0; idx < current.length; idx++) {
                    if (best[idx] != null && best[idx].getAssignments() != null) {
                        bestQuality += iQalityWeights[type.ordinal()] * getModel().getStudentQuality().penalty(type, best[idx]);
                        for (int x = 0; x < idx; x++) {
                            if (best[x] != null && best[x].getAssignments() != null)
                                bestQuality += iQalityWeights[type.ordinal()] * getModel().getStudentQuality().penalty(type, best[x], best[idx]);
                        }
                    }
                    if (current[idx] != null && current[idx].getAssignments() != null) {
                        currentQuality += iQalityWeights[type.ordinal()] * getModel().getStudentQuality().penalty(type, current[idx]);
                        for (int x = 0; x < idx; x++) {
                            if (current[x] != null && current[x].getAssignments() != null)
                                currentQuality += iQalityWeights[type.ordinal()] * getModel().getStudentQuality().penalty(type, current[x], current[idx]);
                        }
                    }
                }
            }
            if (currentQuality < bestQuality)
                return -1;
            if (bestQuality < currentQuality)
                return 1;
        } else {
            // 4. avoid time overlaps
            if (getModel().getTimeOverlaps() != null) {
                int bestTimeOverlaps = 0, currentTimeOverlaps = 0;
                for (int idx = 0; idx < current.length; idx++) {
                    if (best[idx] != null && best[idx].getAssignments() != null) {
                        for (int x = 0; x < idx; x++) {
                            if (best[x] != null && best[x].getAssignments() != null)
                                bestTimeOverlaps += getModel().getTimeOverlaps().nrConflicts(best[x], best[idx]);
                            else if (getStudent().getRequests().get(x) instanceof FreeTimeRequest)
                                bestTimeOverlaps += getModel().getTimeOverlaps().nrConflicts(((FreeTimeRequest) getStudent().getRequests().get(x)).createEnrollment(), best[idx]);
                        }
                        for (int x = 0; x < idx; x++) {
                            if (current[x] != null && current[x].getAssignments() != null)
                                currentTimeOverlaps += getModel().getTimeOverlaps().nrConflicts(current[x], current[idx]);
                            else if (getStudent().getRequests().get(x) instanceof FreeTimeRequest)
                                currentTimeOverlaps += getModel().getTimeOverlaps().nrConflicts(((FreeTimeRequest) getStudent().getRequests().get(x)).createEnrollment(), current[idx]);
                        }
                    }
                }
                for (int idx = 0; idx < current.length; idx++) {
                    if (best[idx] != null && best[idx].getAssignments() != null && best[idx].isCourseRequest()) {
                        bestTimeOverlaps += getModel().getTimeOverlaps().nrNotAvailableTimeConflicts(best[idx]);
                    }
                    if (current[idx] != null && current[idx].getAssignments() != null && current[idx].isCourseRequest()) {
                        currentTimeOverlaps += getModel().getTimeOverlaps().nrNotAvailableTimeConflicts(current[idx]);
                    }
                }
                if (currentTimeOverlaps < bestTimeOverlaps)
                    return -1;
                if (bestTimeOverlaps < currentTimeOverlaps)
                    return 1;
            }

            // 5. avoid distance conflicts
            if (getModel().getDistanceConflict() != null) {
                int bestDistanceConf = 0, currentDistanceConf = 0;
                for (int idx = 0; idx < current.length; idx++) {
                    if (best[idx] != null && best[idx].getAssignments() != null) {
                        bestDistanceConf += getModel().getDistanceConflict().nrConflicts(best[idx]);
                        for (int x = 0; x < idx; x++) {
                            if (best[x] != null && best[x].getAssignments() != null)
                                bestDistanceConf += getModel().getDistanceConflict().nrConflicts(best[x], best[idx]);
                        }
                    }
                    if (current[idx] != null && current[idx].getAssignments() != null) {
                        currentDistanceConf += getModel().getDistanceConflict().nrConflicts(current[idx]);
                        for (int x = 0; x < idx; x++) {
                            if (current[x] != null && current[x].getAssignments() != null)
                                currentDistanceConf += getModel().getDistanceConflict().nrConflicts(current[x], current[idx]);
                        }
                    }
                }
                if (currentDistanceConf < bestDistanceConf)
                    return -1;
                if (bestDistanceConf < currentDistanceConf)
                    return 1;
            }
        }

        // 6. avoid no-time and online sections (no-time first, online second)
        int bestNoTime = 0, currentNoTime = 0;
        int bestOnline = 0, currentOnline = 0;
        for (int idx = 0; idx < current.length; idx++) {
            if (best[idx] != null && best[idx].getAssignments() != null) {
                for (Section section : best[idx].getSections()) {
                    if (!section.hasTime())
                        bestNoTime++;
                    if (section.isOnline())
                        bestOnline++;
                }
                for (Section section : current[idx].getSections()) {
                    if (!section.hasTime())
                        currentNoTime++;
                    if (section.isOnline())
                        currentOnline++;
                }
            }
        }
        if (currentNoTime < bestNoTime)
            return -1;
        if (bestNoTime < currentNoTime)
            return 1;
        if (currentOnline < bestOnline)
            return -1;
        if (bestOnline < currentOnline)
            return 1;

        // 7. balance sections
        double bestUnavailableSize = 0.0, currentUnavailableSize = 0.0;
        int bestAltSectionsWithLimit = 0, currentAltSectionsWithLimit = 0;
        for (int idx = 0; idx < current.length; idx++) {
            if (best[idx] != null && best[idx].getAssignments() != null) {
                for (Section section : best[idx].getSections()) {
                    Subpart subpart = section.getSubpart();
                    // skip unlimited and single section subparts
                    if (subpart.getSections().size() <= 1 || subpart.getLimit() <= 0)
                        continue;
                    // average size
                    double averageSize = ((double) subpart.getLimit()) / subpart.getSections().size();
                    // section is below average
                    if (section.getLimit() < averageSize)
                        bestUnavailableSize += (averageSize - section.getLimit()) / averageSize;
                    bestAltSectionsWithLimit++;
                }
                for (Section section : current[idx].getSections()) {
                    Subpart subpart = section.getSubpart();
                    // skip unlimited and single section subparts
                    if (subpart.getSections().size() <= 1 || subpart.getLimit() <= 0)
                        continue;
                    // average size
                    double averageSize = ((double) subpart.getLimit()) / subpart.getSections().size();
                    // section is below average
                    if (section.getLimit() < averageSize)
                        currentUnavailableSize += (averageSize - section.getLimit()) / averageSize;
                    currentAltSectionsWithLimit++;
                }
            }
        }
        double bestUnavailableSizeFraction = (bestUnavailableSize > 0 ? bestUnavailableSize / bestAltSectionsWithLimit
                : 0.0);
        double currentUnavailableSizeFraction = (currentUnavailableSize > 0 ? currentUnavailableSize
                / currentAltSectionsWithLimit : 0.0);
        if (currentUnavailableSizeFraction < bestUnavailableSizeFraction)
            return -1;
        if (bestUnavailableSizeFraction < currentUnavailableSizeFraction)
            return 1;

        // 8. average penalty sections
        double bestPenalty = 0.0, currentPenalty = 0.0;
        for (int idx = 0; idx < current.length; idx++) {
            if (best[idx] != null && best[idx].getAssignments() != null) {
                for (Section section : best[idx].getSections())
                    bestPenalty += section.getPenalty() / best[idx].getSections().size();
                for (Section section : current[idx].getSections())
                    currentPenalty += section.getPenalty() / current[idx].getSections().size();
            }
        }
        if (currentPenalty < bestPenalty)
            return -1;
        if (bestPenalty < currentPenalty)
            return 1;

        return 0;
    }

    @Override
    public boolean canImprove(Assignment<Request, Enrollment> assignment, int maxIdx, Enrollment[] current,
            Enrollment[] best) {
        // 0. best priority & alternativity ignoring free time requests
        int alt = 0;
        boolean ft = false;
        boolean res = false;
        for (int idx = 0; idx < current.length; idx++) {
            if (isFreeTime(idx)) {
                ft = true;
                continue;
            }
            Request request = getRequest(idx);
            if (request instanceof CourseRequest && ((CourseRequest)request).hasReservations()) res = true;
            if (idx < maxIdx) {
                if (best[idx] != null) {
                    if (current[idx] == null)
                        return false; // higher priority request assigned
                    if (best[idx].getTruePriority() < current[idx].getTruePriority())
                        return false; // less alternative request assigned
                    if (best[idx].getTruePriority() > current[idx].getTruePriority())
                        return true; // less alternative request assigned
                    if (request.isAlternative())
                        alt--;
                } else {
                    if (current[idx] != null)
                        return true; // higher priority request assigned
                    if (!request.isAlternative())
                        alt++;
                }
            } else {
                if (best[idx] != null) {
                    if (best[idx].getTruePriority() > 0)
                        return true; // alternativity can be improved
                } else {
                    if (!request.isAlternative() || alt > 0)
                        return true; // priority can be improved
                }
            }
        }
        
        // 0.1. allowed, but not available sections
        int notAvailable = 0;
        for (int idx = 0; idx < current.length; idx++) {
            if (best[idx] != null && best[idx].getAssignments() != null && best[idx].getRequest() instanceof CourseRequest && best[idx].getReservation() != null && best[idx].getReservation().canAssignOverLimit()) {
                for (Section section: best[idx].getSections()) {
                    if (section.getLimit() == 0)
                        notAvailable ++;
                }
            }
            if (idx < maxIdx && current[idx] != null && current[idx].getAssignments() != null && current[idx].getRequest() instanceof CourseRequest && current[idx].getReservation() != null && current[idx].getReservation().canAssignOverLimit()) {
                for (Section section: current[idx].getSections()) {
                    if (section.getLimit() == 0)
                        notAvailable --;
                }
            }
        }
        if (notAvailable > 0) {
            return true;
        }

        // 0.5. avoid course time overlaps & unavailability overlaps
        if (getModel().getStudentQuality() != null) {
            int bestTimeOverlaps = 0, currentTimeOverlaps = 0;
            for (int idx = 0; idx < current.length; idx++) {
                if (best[idx] != null && best[idx].getRequest() instanceof CourseRequest) {
                    for (int x = 0; x < idx; x++) {
                        if (best[x] != null && best[x].getRequest() instanceof CourseRequest)
                            bestTimeOverlaps += getModel().getStudentQuality().penalty(StudentQuality.Type.CourseTimeOverlap, best[x], best[idx]);
                    }
                }
                if (current[idx] != null && idx < maxIdx && current[idx].getRequest() instanceof CourseRequest) {
                    for (int x = 0; x < idx; x++) {
                        if (current[x] != null && current[x].getRequest() instanceof CourseRequest)
                            currentTimeOverlaps += getModel().getStudentQuality().penalty(StudentQuality.Type.CourseTimeOverlap, current[x], current[idx]);
                    }
                }
            }
            for (int idx = 0; idx < current.length; idx++) {
                if (best[idx] != null && best[idx].getAssignments() != null && best[idx].isCourseRequest()) {
                    bestTimeOverlaps += getModel().getStudentQuality().penalty(StudentQuality.Type.Unavailability, best[idx]);
                }
                if (current[idx] != null && idx < maxIdx && current[idx].getAssignments() != null && current[idx].isCourseRequest()) {
                    currentTimeOverlaps += getModel().getStudentQuality().penalty(StudentQuality.Type.Unavailability, current[idx]);
                }
            }
            if (currentTimeOverlaps < bestTimeOverlaps)
                return true;
            if (bestTimeOverlaps < currentTimeOverlaps)
                return false;
        } else if (getModel().getTimeOverlaps() != null) {
            int bestTimeOverlaps = 0, currentTimeOverlaps = 0;
            for (int idx = 0; idx < current.length; idx++) {
                if (best[idx] != null && best[idx].getRequest() instanceof CourseRequest) {
                    for (int x = 0; x < idx; x++) {
                        if (best[x] != null && best[x].getRequest() instanceof CourseRequest)
                            bestTimeOverlaps += getModel().getTimeOverlaps().nrConflicts(best[x], best[idx]);
                    }
                }
                if (current[idx] != null && idx < maxIdx && current[idx].getRequest() instanceof CourseRequest) {
                    for (int x = 0; x < idx; x++) {
                        if (current[x] != null && current[x].getRequest() instanceof CourseRequest)
                            currentTimeOverlaps += getModel().getTimeOverlaps().nrConflicts(current[x], current[idx]);
                    }
                }
            }
            for (int idx = 0; idx < current.length; idx++) {
                if (best[idx] != null && best[idx].getAssignments() != null && best[idx].isCourseRequest()) {
                    bestTimeOverlaps += getModel().getTimeOverlaps().nrNotAvailableTimeConflicts(best[idx]);
                }
                if (current[idx] != null && idx < maxIdx && current[idx].getAssignments() != null && current[idx].isCourseRequest()) {
                    currentTimeOverlaps += getModel().getTimeOverlaps().nrNotAvailableTimeConflicts(current[idx]);
                }
            }
            if (currentTimeOverlaps < bestTimeOverlaps)
                return true;
            if (bestTimeOverlaps < currentTimeOverlaps)
                return false;
        }

        // 1. maximize number of penalties
        double bestPenalties = 0, currentPenalties = 0;
        for (int idx = 0; idx < current.length; idx++) {
            if (best[idx] != null) {
                for (Section section : best[idx].getSections())
                    bestPenalties += getModel().getOverExpected(assignment, best, idx, section, best[idx].getRequest());
            }
            if (current[idx] != null && idx < maxIdx) {
                for (Section section : current[idx].getSections())
                    currentPenalties += getModel().getOverExpected(assignment, current, idx, section, current[idx].getRequest());
            }
        }
        if (currentPenalties < bestPenalties)
            return true;
        if (bestPenalties < currentPenalties)
            return false;

        // 2. best priority & alternativity including free times
        if (ft) {
            alt = 0;
            for (int idx = 0; idx < current.length; idx++) {
                Request request = getStudent().getRequests().get(idx);
                if (idx < maxIdx) {
                    if (best[idx] != null) {
                        if (current[idx] == null)
                            return false; // higher priority request assigned
                        if (best[idx].getTruePriority() < current[idx].getTruePriority())
                            return false; // less alternative request assigned
                        if (best[idx].getTruePriority() > current[idx].getTruePriority())
                            return true; // less alternative request assigned
                        if (request.isAlternative())
                            alt--;
                    } else {
                        if (current[idx] != null)
                            return true; // higher priority request assigned
                        if (request instanceof CourseRequest && !request.isAlternative())
                            alt++;
                    }
                } else {
                    if (best[idx] != null) {
                        if (best[idx].getTruePriority() > 0)
                            return true; // alternativity can be improved
                    } else {
                        if (!request.isAlternative() || alt > 0)
                            return true; // priority can be improved
                    }
                }
            }
        }

        // 3. maximize selection
        int bestSelected = 0, currentSelected = 0;
        for (int idx = 0; idx < current.length; idx++) {
            if (best[idx] != null && best[idx].isCourseRequest()) {
                Set<Section> preferred = getPreferredSections(best[idx].getRequest());
                if (preferred != null && !preferred.isEmpty()) {
                    for (Section section : best[idx].getSections())
                        if (preferred.contains(section)) {
                            if (idx < maxIdx)
                                bestSelected++;
                        } else if (idx >= maxIdx)
                            bestSelected--;
                }
            }
            if (current[idx] != null && idx < maxIdx && current[idx].isCourseRequest()) {
                Set<Section> preferred = getPreferredSections(current[idx].getRequest());
                if (preferred != null && !preferred.isEmpty()) {
                    for (Section section : current[idx].getSections())
                        if (preferred.contains(section))
                            currentSelected++;
                }
            }
        }
        if (currentSelected > bestSelected)
            return true;
        if (bestSelected > currentSelected)
            return false;

        // 3.5 maximize preferences
        double bestSelectedConfigs = 0, currentSelectedConfigs = 0;
        double bestSelectedSections = 0, currentSelectedSections = 0;
        for (int idx = 0; idx < current.length; idx++) {
            if (best[idx] != null && best[idx].getAssignments() != null && best[idx].isCourseRequest()) {
                bestSelectedSections += best[idx].percentSelectedSameSection();
                bestSelectedConfigs += best[idx].percentSelectedSameConfig();
                if (idx >= maxIdx) {
                    bestSelectedSections -= 1.0;
                    bestSelectedConfigs -= 1.0;
                }
            }
            if (current[idx] != null && idx < maxIdx && current[idx].getAssignments() != null && current[idx].isCourseRequest()) {
                currentSelectedSections += current[idx].percentSelectedSameSection();
                currentSelectedConfigs += current[idx].percentSelectedSameConfig();
            }
        }
        if (0.3 * currentSelectedConfigs + 0.7 * currentSelectedSections > 0.3 * bestSelectedConfigs + 0.7 * bestSelectedSections) return true;
        if (0.3 * bestSelectedConfigs + 0.7 * bestSelectedSections > 0.3 * currentSelectedConfigs + 0.7 * currentSelectedSections) return false;
        
        // 3.9 maximize selection with penalization for not followed reservations
        if (res) {
            alt = 0;
            for (int idx = 0; idx < current.length; idx++) {
                Request request = getStudent().getRequests().get(idx);
                if (idx < maxIdx) {
                    if (best[idx] != null) {
                        if (current[idx] == null)
                            return false; // higher priority request assigned
                        if (best[idx].getAdjustedPriority() < current[idx].getAdjustedPriority())
                            return false; // less alternative request assigned
                        if (best[idx].getAdjustedPriority() > current[idx].getAdjustedPriority())
                            return true; // less alternative request assigned
                        if (request.isAlternative())
                            alt--;
                    } else {
                        if (current[idx] != null)
                            return true; // higher priority request assigned
                        if (request instanceof CourseRequest && !request.isAlternative())
                            alt++;
                    }
                } else {
                    if (best[idx] != null) {
                        if (best[idx].getTruePriority() > 0)
                            return true; // alternativity can be improved
                    } else {
                        if (!request.isAlternative() || alt > 0)
                            return true; // priority can be improved
                    }
                }
            }
        }
        
        // 3.95 avoid past sections
        double bestPast = 0.0, currentPast = 0.0;
        for (int idx = 0; idx < current.length; idx++) {
            if (best[idx] != null && best[idx].getAssignments() != null) {
                for (Section section : best[idx].getSections()) {
                    if (section.isPast())
                        bestPast += 1.0 / best[idx].getSections().size();
                }
            }
            if (current[idx] != null && idx < maxIdx && current[idx].getAssignments() != null) {
                for (Section section : current[idx].getSections()) {
                    if (section.isPast())
                        currentPast += 1.0 / current[idx].getSections().size();
                }
            }
        }
        if (Math.abs(currentPast - bestPast) > 0.0001) {
            if (currentPast < bestPast)
                return true;
            if (bestPast < currentPast)
                return false;
        }
        
        // 4-5. student quality
        if (getModel().getStudentQuality() != null) {
            double bestQuality = 0, currentQuality = 0;
            for (StudentQuality.Type type: StudentQuality.Type.values()) {
                for (int idx = 0; idx < current.length; idx++) {
                    if (best[idx] != null) {
                        bestQuality += iQalityWeights[type.ordinal()] * getModel().getStudentQuality().penalty(type, best[idx]);
                        for (int x = 0; x < idx; x++) {
                            if (best[x] != null)
                                bestQuality += iQalityWeights[type.ordinal()] * getModel().getStudentQuality().penalty(type, best[x], best[idx]);
                        }
                    }
                    if (current[idx] != null && idx < maxIdx) {
                        currentQuality += iQalityWeights[type.ordinal()] * getModel().getStudentQuality().penalty(type, current[idx]);
                        for (int x = 0; x < idx; x++) {
                            if (current[x] != null)
                                currentQuality += iQalityWeights[type.ordinal()] * getModel().getStudentQuality().penalty(type, current[x], current[idx]);
                        }
                    }
                }
            }
            if (currentQuality < bestQuality)
                return true;
            if (bestQuality < currentQuality)
                return false;
        } else {
            // 4. avoid time overlaps
            if (getModel().getTimeOverlaps() != null) {
                int bestTimeOverlaps = 0, currentTimeOverlaps = 0;
                for (int idx = 0; idx < current.length; idx++) {
                    if (best[idx] != null) {
                        for (int x = 0; x < idx; x++) {
                            if (best[x] != null)
                                bestTimeOverlaps += getModel().getTimeOverlaps().nrConflicts(best[x], best[idx]);
                            else if (getStudent().getRequests().get(x) instanceof FreeTimeRequest)
                                bestTimeOverlaps += getModel().getTimeOverlaps()
                                        .nrConflicts(
                                                ((FreeTimeRequest) getStudent().getRequests().get(x)).createEnrollment(),
                                                best[idx]);
                        }
                    }
                    if (current[idx] != null && idx < maxIdx) {
                        for (int x = 0; x < idx; x++) {
                            if (current[x] != null)
                                currentTimeOverlaps += getModel().getTimeOverlaps().nrConflicts(current[x], current[idx]);
                            else if (getStudent().getRequests().get(x) instanceof FreeTimeRequest)
                                currentTimeOverlaps += getModel().getTimeOverlaps().nrConflicts(
                                        ((FreeTimeRequest) getStudent().getRequests().get(x)).createEnrollment(),
                                        current[idx]);
                        }
                    }
                }
                for (int idx = 0; idx < current.length; idx++) {
                    if (best[idx] != null && best[idx].getAssignments() != null && best[idx].isCourseRequest()) {
                        bestTimeOverlaps += getModel().getTimeOverlaps().nrNotAvailableTimeConflicts(best[idx]);
                    }
                    if (current[idx] != null && idx < maxIdx && current[idx].getAssignments() != null && current[idx].isCourseRequest()) {
                        currentTimeOverlaps += getModel().getTimeOverlaps().nrNotAvailableTimeConflicts(current[idx]);
                    }
                }
                if (currentTimeOverlaps < bestTimeOverlaps)
                    return true;
                if (bestTimeOverlaps < currentTimeOverlaps)
                    return false;
            }

            // 5. avoid distance conflicts
            if (getModel().getDistanceConflict() != null) {
                int bestDistanceConf = 0, currentDistanceConf = 0;
                for (int idx = 0; idx < current.length; idx++) {
                    if (best[idx] != null) {
                        bestDistanceConf += getModel().getDistanceConflict().nrConflicts(best[idx]);
                        for (int x = 0; x < idx; x++) {
                            if (best[x] != null)
                                bestDistanceConf += getModel().getDistanceConflict().nrConflicts(best[x], best[idx]);
                        }
                    }
                    if (current[idx] != null && idx < maxIdx) {
                        currentDistanceConf += getModel().getDistanceConflict().nrConflicts(current[idx]);
                        for (int x = 0; x < idx; x++) {
                            if (current[x] != null)
                                currentDistanceConf += getModel().getDistanceConflict().nrConflicts(current[x],
                                        current[idx]);
                        }
                    }
                }
                if (currentDistanceConf < bestDistanceConf)
                    return true;
                if (bestDistanceConf < currentDistanceConf)
                    return false;
            }
        }

        // 6. avoid no-time and online sections (no-time first, online second)
        int bestNoTime = 0, currentNoTime = 0;
        int bestOnline = 0, currentOnline = 0;
        for (int idx = 0; idx < current.length; idx++) {
            if (best[idx] != null) {
                for (Section section : best[idx].getSections()) {
                    if (!section.hasTime())
                        bestNoTime++;
                    if (section.isOnline())
                        bestOnline++;
                }
            }
            if (current[idx] != null && idx < maxIdx) {
                for (Section section : current[idx].getSections()) {
                    if (!section.hasTime())
                        currentNoTime++;
                    if (section.isOnline())
                        currentOnline++;
                }
            }
        }
        if (currentNoTime < bestNoTime)
            return true;
        if (bestNoTime < currentNoTime)
            return false;
        if (currentOnline < bestOnline)
            return true;
        if (bestOnline < currentOnline)
            return false;

        // 7. balance sections
        double bestUnavailableSize = 0.0, currentUnavailableSize = 0.0;
        int bestAltSectionsWithLimit = 0, currentAltSectionsWithLimit = 0;
        for (int idx = 0; idx < current.length; idx++) {
            if (best[idx] != null) {
                for (Section section : best[idx].getSections()) {
                    Subpart subpart = section.getSubpart();
                    // skip unlimited and single section subparts
                    if (subpart.getSections().size() <= 1 || subpart.getLimit() <= 0)
                        continue;
                    // average size
                    double averageSize = ((double) subpart.getLimit()) / subpart.getSections().size();
                    // section is below average
                    if (section.getLimit() < averageSize)
                        bestUnavailableSize += (averageSize - section.getLimit()) / averageSize;
                    bestAltSectionsWithLimit++;
                }
            }
            if (current[idx] != null && idx < maxIdx) {
                for (Section section : current[idx].getSections()) {
                    Subpart subpart = section.getSubpart();
                    // skip unlimited and single section subparts
                    if (subpart.getSections().size() <= 1 || subpart.getLimit() <= 0)
                        continue;
                    // average size
                    double averageSize = ((double) subpart.getLimit()) / subpart.getSections().size();
                    // section is below average
                    if (section.getLimit() < averageSize)
                        currentUnavailableSize += (averageSize - section.getLimit()) / averageSize;
                    currentAltSectionsWithLimit++;
                }
            }
        }
        double bestUnavailableSizeFraction = (bestUnavailableSize > 0 ? bestUnavailableSize / bestAltSectionsWithLimit
                : 0.0);
        double currentUnavailableSizeFraction = (currentUnavailableSize > 0 ? currentUnavailableSize
                / currentAltSectionsWithLimit : 0.0);
        if (currentUnavailableSizeFraction < bestUnavailableSizeFraction)
            return true;
        if (bestUnavailableSizeFraction < currentUnavailableSizeFraction)
            return false;

        // 8. average penalty sections
        double bestPenalty = 0.0, currentPenalty = 0.0;
        for (int idx = 0; idx < current.length; idx++) {
            if (best[idx] != null) {
                for (Section section : best[idx].getSections())
                    bestPenalty += section.getPenalty() / best[idx].getSections().size();
                if (idx >= maxIdx && best[idx].isCourseRequest())
                    bestPenalty -= ((CourseRequest) best[idx].getRequest()).getMinPenalty();
            }
            if (current[idx] != null && idx < maxIdx) {
                for (Section section : current[idx].getSections())
                    currentPenalty += section.getPenalty() / current[idx].getSections().size();
            }
        }
        if (currentPenalty < bestPenalty)
            return true;
        if (bestPenalty < currentPenalty)
            return false;

        return true;
    }

    @Override
    public double getTotalWeight(Assignment<Request, Enrollment> assignment, Enrollment[] enrollemnts) {
        if (enrollemnts == null)
            return 0.0;
        double value = 0.0;
        for (int idx = 0; idx < enrollemnts.length; idx++) {
            if (enrollemnts[idx] != null)
                if (getModel().getStudentQuality() != null) {
                    value += getWeight(assignment, enrollemnts[idx], getStudentQualityConflicts(enrollemnts, idx));
                } else { 
                    value += getWeight(assignment, enrollemnts[idx], getDistanceConflicts(enrollemnts, idx), getTimeOverlappingConflicts(enrollemnts, idx));
                }
        }
        return value;
    }

    @Override
    public int compare(Assignment<Request, Enrollment> assignment, Enrollment e1, Enrollment e2) {
        // 1. alternativity
        if (e1.getTruePriority() < e2.getTruePriority())
            return -1;
        if (e1.getTruePriority() > e2.getTruePriority())
            return 1;
        
        // 1.5 not available sections
        int na1 = 0, na2 = 0;
        for (Section section: e1.getSections())
            if (section.getLimit() == 0) na1++;
        for (Section section: e2.getSections())
            if (section.getLimit() == 0) na2++;
        if (na1 < na2) return -1;
        if (na1 > na2) return 1;
        
        // 2. maximize number of penalties
        double p1 = 0, p2 = 0;
        for (Section section : e1.getSections())
            p1 += getModel().getOverExpected(assignment, section, e1.getRequest());
        for (Section section : e2.getSections())
            p2 += getModel().getOverExpected(assignment, section, e2.getRequest());
        if (p1 < p2)
            return -1;
        if (p2 < p1)
            return 1;

        // 3. maximize selection
        if (e1.isCourseRequest()) {
            Set<Section> preferred = getPreferredSections(e1.getRequest());
            if (preferred != null && !preferred.isEmpty()) {
                int s1 = 0, s2 = 0;
                for (Section section : e1.getSections())
                    if (preferred.contains(section))
                        s1++;
                for (Section section : e2.getSections())
                    if (preferred.contains(section))
                        s2++;
                if (s2 > s1)
                    return -1;
                if (s1 > s2)
                    return 1;
            }
        }
        
        // 3.5 maximize preferences
        if (e1.isCourseRequest()) {
            double s1 = 0.3 * e1.percentSelectedSameConfig() + 0.7 * e1.percentSelectedSameSection();
            double s2 = 0.3 * e2.percentSelectedSameConfig() + 0.7 * e2.percentSelectedSameSection();
            if (s1 > s2) return -1;
            if (s2 > s1) return 1;
        }
        
        // 3.9 maximize selection with penalization for not followed reservations
        if (e1.getAdjustedPriority() < e2.getAdjustedPriority())
            return -1;
        if (e1.getAdjustedPriority() > e2.getAdjustedPriority())
            return 1;
        
        // 3.95 avoid past sections
        double w1 = 0, w2 = 0;
        for (Section section : e1.getSections()) {
            if (section.isPast())
                w1 += 1.0 / e1.getSections().size();
        }
        for (Section section : e2.getSections()) {
            if (section.isPast())
                w2 += 1.0 / e2.getSections().size();
        }
        if (Math.abs(w1 - w2) > 0.0001) {
            if (w1 < w2)
                return -1;
            if (w2 < w1)
                return 1;
        }

        // 4. avoid time overlaps
        if (getTimesToAvoid() == null) {
            if (getModel().getStudentQuality() != null) {
                int o1 = getModel().getStudentQuality().penalty(StudentQuality.Type.FreeTimeOverlap, e1) + getModel().getStudentQuality().penalty(StudentQuality.Type.Unavailability, e1);
                int o2 = getModel().getStudentQuality().penalty(StudentQuality.Type.FreeTimeOverlap, e2) + getModel().getStudentQuality().penalty(StudentQuality.Type.Unavailability, e2);
                if (o1 < o2)
                    return -1;
                if (o2 < o1)
                    return 1;
            } else if (getModel().getTimeOverlaps() != null) {
                int o1 = getModel().getTimeOverlaps().nrFreeTimeConflicts(e1) + getModel().getTimeOverlaps().nrNotAvailableTimeConflicts(e1);
                int o2 = getModel().getTimeOverlaps().nrFreeTimeConflicts(e2) + getModel().getTimeOverlaps().nrNotAvailableTimeConflicts(e2);
                if (o1 < o2)
                    return -1;
                if (o2 < o1)
                    return 1;
            }
        } else {
            if (e1.getRequest().equals(e2.getRequest()) && e1.isCourseRequest()) {
                double o1 = 0.0, o2 = 0.0;
                for (Section s : e1.getSections()) {
                    if (s.getTime() != null)
                        for (TimeToAvoid avoid : getTimesToAvoid()) {
                            if (avoid.priority() > e1.getRequest().getPriority())
                                o1 += avoid.overlap(s.getTime());
                        }
                }
                for (Section s : e2.getSections()) {
                    if (s.getTime() != null)
                        for (TimeToAvoid avoid : getTimesToAvoid()) {
                            if (avoid.priority() > e2.getRequest().getPriority())
                                o2 += avoid.overlap(s.getTime());
                        }
                }
                if (o1 < o2)
                    return -1;
                if (o2 < o1)
                    return 1;
            }
        }

        // 5. avoid distance conflicts
        if (getModel().getDistanceConflict() != null) {
            int c1 = getModel().getDistanceConflict().nrConflicts(e1);
            int c2 = getModel().getDistanceConflict().nrConflicts(e2);
            if (c1 < c2)
                return -1;
            if (c2 < c1)
                return 1;
        }

        // 6. avoid no-time and online sections (no-time first, online second)
        int n1 = 0, n2 = 0;
        int o1 = 0, o2 = 0;
        for (Section section : e1.getSections()) {
            if (!section.hasTime())
                n1++;
            if (section.isOnline())
                o1++;
        }
        for (Section section : e2.getSections()) {
            if (!section.hasTime())
                n2++;
            if (section.isOnline())
                o2++;
        }
        if (n1 < n2)
            return -1;
        if (n2 < n1)
            return 1;
        if (o1 < o2)
            return -1;
        if (o2 < o1)
            return 1;

        // 7. balance sections
        double u1 = 0.0, u2 = 0.0;
        int a1 = 0, a2 = 0;
        for (Section section : e1.getSections()) {
            Subpart subpart = section.getSubpart();
            // skip unlimited and single section subparts
            if (subpart.getSections().size() <= 1 || subpart.getLimit() <= 0)
                continue;
            // average size
            double averageSize = ((double) subpart.getLimit()) / subpart.getSections().size();
            // section is below average
            if (section.getLimit() < averageSize)
                u1 += (averageSize - section.getLimit()) / averageSize;
            a1++;
        }
        for (Section section : e2.getSections()) {
            Subpart subpart = section.getSubpart();
            // skip unlimited and single section subparts
            if (subpart.getSections().size() <= 1 || subpart.getLimit() <= 0)
                continue;
            // average size
            double averageSize = ((double) subpart.getLimit()) / subpart.getSections().size();
            // section is below average
            if (section.getLimit() < averageSize)
                u2 += (averageSize - section.getLimit()) / averageSize;
            a2++;
        }
        double f1 = (u1 > 0 ? u1 / a1 : 0.0);
        double f2 = (u2 > 0 ? u2 / a2 : 0.0);
        if (f1 < f2)
            return -1;
        if (f2 < f1)
            return 1;

        // 8. average penalty sections
        double x1 = 0.0, x2 = 0.0;
        for (Section section : e1.getSections())
            x1 += section.getPenalty() / e1.getSections().size();
        for (Section section : e2.getSections())
            x2 += section.getPenalty() / e2.getSections().size();
        if (x1 < x2)
            return -1;
        if (x2 < x1)
            return 1;

        return 0;
    }

    /**
     * Time to be avoided.
     */
    public static class TimeToAvoid {
        private TimeLocation iTime;
        private double iPenalty;
        private int iPriority;

        public TimeToAvoid(TimeLocation time, int penalty, int priority) {
            iTime = time;
            iPenalty = penalty;
            iPriority = priority;
        }

        public int priority() {
            return iPriority;
        }

        public double overlap(TimeLocation time) {
            if (time.hasIntersection(iTime)) {
                return iPenalty * (time.nrSharedDays(iTime) * time.nrSharedHours(iTime))
                        / (iTime.getNrMeetings() * iTime.getLength());
            } else {
                return 0.0;
            }
        }

        @Override
        public String toString() {
            return iTime.getLongName(true) + " (" + iPriority + "/" + iPenalty + ")";
        }
    }
}