package org.cpsolver.studentsct.online.selection;

import java.util.Hashtable;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.studentsct.extension.StudentQuality;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.FreeTimeRequest;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Subpart;
import org.cpsolver.studentsct.online.OnlineSectioningModel;

/**
* Equal weighting multi-criteria selection criterion. Much like the {@link OnlineSectioningCriterion}, but
* course request priorities are ignored. Most complete solution is preferred instead.
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
public class EqualWeightCriterion extends OnlineSectioningCriterion {

    public EqualWeightCriterion(Student student, OnlineSectioningModel model,
            Assignment<Request, Enrollment> assignment, Hashtable<CourseRequest, Set<Section>> preferredSections) {
        super(student, model, assignment, preferredSections);
    }

    @Override
    public int compare(Assignment<Request, Enrollment> assignment, Enrollment[] current, Enrollment[] best) {
        if (best == null)
            return -1;

        // 0. best number of assigned course requests (including alternativity &
        // priority)
        int currentAssignedCourseReq = 0, bestAssignedCourseReq = 0;
        int currentAssignedRequests = 0, bestAssignedRequests = 0;
        int currentAssignedPriority = 0, bestAssignedPriority = 0;
        int currentAssignedAlternativity = 0, bestAssignedAlternativity = 0;
        int currentNotFollowedReservations = 0, bestNotFollowedReservations = 0;
        for (int idx = 0; idx < current.length; idx++) {
            if (current[idx] != null && current[idx].getAssignments() != null) {
                currentAssignedRequests++;
                if (current[idx].isCourseRequest())
                    currentAssignedCourseReq++;
                currentAssignedPriority += current[idx].getTruePriority() * current[idx].getTruePriority();
                currentAssignedAlternativity += (current[idx].getRequest().isAlternative() ? 1 : 0);
                if (current[idx].getTruePriority() < current[idx].getPriority()) currentNotFollowedReservations ++;
            }
            if (best[idx] != null && best[idx].getAssignments() != null) {
                bestAssignedRequests++;
                if (best[idx].isCourseRequest())
                    bestAssignedCourseReq++;
                bestAssignedPriority += best[idx].getTruePriority() * best[idx].getTruePriority();
                bestAssignedAlternativity += (best[idx].getRequest().isAlternative() ? 1 : 0);
                if (best[idx].getTruePriority() < best[idx].getPriority()) bestNotFollowedReservations ++;
            }
        }
        if (currentAssignedCourseReq > bestAssignedCourseReq)
            return -1;
        if (bestAssignedCourseReq > currentAssignedCourseReq)
            return 1;
        if (currentAssignedPriority < bestAssignedPriority)
            return -1;
        if (bestAssignedPriority < currentAssignedPriority)
            return 1;
        if (currentAssignedAlternativity < bestAssignedAlternativity)
            return -1;
        if (bestAssignedAlternativity < currentAssignedAlternativity)
            return 1;

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

        // 0.5. avoid course overlaps & unavailabilities
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
                if (best[idx] != null && best[idx].getAssignments() != null && best[idx].getRequest() instanceof CourseRequest) {
                    for (int x = 0; x < idx; x++) {
                        if (best[x] != null && best[x].getAssignments() != null
                        		&& best[x].getRequest() instanceof CourseRequest)
                            bestTimeOverlaps += getModel().getTimeOverlaps().nrConflicts(best[x], best[idx]);
                    }
                }
                if (current[idx] != null && current[idx].getAssignments() != null && current[idx].getRequest() instanceof CourseRequest) {
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
            }
            if (current[idx] != null && current[idx].getAssignments() != null && current[idx].isCourseRequest()) {
                for (Section section : current[idx].getSections())
                    currentPenalties += getModel().getOverExpected(assignment, current, idx, section, current[idx].getRequest());
            }
        }
        if (currentPenalties < bestPenalties)
            return -1;
        if (bestPenalties < currentPenalties)
            return 1;

        // 2. best number of assigned requests (including free time requests)
        if (currentAssignedRequests > bestAssignedRequests)
            return -1;
        if (bestAssignedRequests > currentAssignedRequests)
            return 1;

        // 3. maximize selection
        int bestSelected = 0, currentSelected = 0;
        for (int idx = 0; idx < current.length; idx++) {
            Set<Section> preferred = getPreferredSections(getRequest(idx));
            if (preferred != null && !preferred.isEmpty()) {
                if (best[idx] != null && best[idx].getAssignments() != null && best[idx].isCourseRequest()) {
                    for (Section section : best[idx].getSections())
                        if (preferred.contains(section))
                            bestSelected++;
                }
                if (current[idx] != null && current[idx].getAssignments() != null && current[idx].isCourseRequest()) {
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
        
        // 3.9 minimize enrollments where the reservation is not followed
        if (currentNotFollowedReservations < bestNotFollowedReservations)
            return -1;
        if (bestNotFollowedReservations < currentNotFollowedReservations)
            return 1;
        
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
                    }
                    if (current[idx] != null && current[idx].getAssignments() != null) {
                        for (int x = 0; x < idx; x++) {
                            if (current[x] != null && current[x].getAssignments() != null)
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
                                currentDistanceConf += getModel().getDistanceConflict().nrConflicts(current[x],
                                        current[idx]);
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
            }
            if (current[idx] != null && current[idx].getAssignments() != null) {
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
            }
            if (current[idx] != null && current[idx].getAssignments() != null) {
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
            }
            if (current[idx] != null && current[idx].getAssignments() != null) {
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
        // 0. best number of assigned course requests (including alternativity &
        // priority)
        int currentAssignedCourseReq = 0, bestAssignedCourseReq = 0;
        int currentAssignedRequests = 0, bestAssignedRequests = 0;
        int currentAssignedPriority = 0, bestAssignedPriority = 0;
        int currentAssignedAlternativity = 0, bestAssignedAlternativity = 0;
        int currentNotFollowedReservations = 0, bestNotFollowedReservations = 0;
        int alt = 0;
        for (int idx = 0; idx < current.length; idx++) {
            if (idx < maxIdx) {
                if (current[idx] != null && current[idx].getAssignments() != null) {
                    currentAssignedRequests++;
                    if (current[idx].isCourseRequest())
                        currentAssignedCourseReq++;
                    currentAssignedPriority += current[idx].getTruePriority() * current[idx].getTruePriority();
                    currentAssignedAlternativity += (current[idx].getRequest().isAlternative() ? 1 : 0);
                    if (current[idx].getTruePriority() < current[idx].getPriority()) currentNotFollowedReservations ++;
                } else if (!isFreeTime(idx) && !getRequest(idx).isAlternative()) {
                    alt++;
                }
            } else {
                if (!getRequest(idx).isAlternative()) {
                    currentAssignedRequests++;
                    if (!isFreeTime(idx))
                        currentAssignedCourseReq++;
                } else if (alt > 0) {
                    currentAssignedRequests++;
                    currentAssignedCourseReq++;
                    alt--;
                    currentAssignedAlternativity++;
                }
            }
            if (best[idx] != null && best[idx].getAssignments() != null) {
                bestAssignedRequests++;
                if (best[idx].isCourseRequest())
                    bestAssignedCourseReq++;
                bestAssignedPriority += best[idx].getTruePriority() * best[idx].getTruePriority();
                bestAssignedAlternativity += (best[idx].getRequest().isAlternative() ? 1 : 0);
                if (best[idx].getTruePriority() < best[idx].getPriority()) bestNotFollowedReservations ++;
            }
        }
        if (currentAssignedCourseReq > bestAssignedCourseReq)
            return true;
        if (bestAssignedCourseReq > currentAssignedCourseReq)
            return false;
        if (currentAssignedPriority < bestAssignedPriority)
            return true;
        if (bestAssignedPriority < currentAssignedPriority)
            return false;
        if (currentAssignedAlternativity < bestAssignedAlternativity)
            return true;
        if (bestAssignedAlternativity < currentAssignedAlternativity)
            return false;

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

        // 0.5. avoid course time overlaps & unavailabilities
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

        // 2. best number of assigned requests (including free time requests)
        if (currentAssignedRequests > bestAssignedRequests)
            return true;
        if (bestAssignedRequests > currentAssignedRequests)
            return false;

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
        
        // 3.9 minimize enrollments where the reservation is not followed
        if (currentNotFollowedReservations < bestNotFollowedReservations)
            return true;
        if (bestNotFollowedReservations < currentNotFollowedReservations)
            return false;
        
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
        
        // 4-5. solution quality
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
                                bestTimeOverlaps += getModel().getTimeOverlaps().nrConflicts(((FreeTimeRequest) getStudent().getRequests().get(x)).createEnrollment(), best[idx]);
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
}
