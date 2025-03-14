package org.cpsolver.studentsct.weights;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.DefaultSingleAssignment;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.extension.DistanceConflict;
import org.cpsolver.studentsct.extension.StudentQuality;
import org.cpsolver.studentsct.extension.StudentQuality.Conflict;
import org.cpsolver.studentsct.extension.TimeOverlapsCounter;
import org.cpsolver.studentsct.model.Choice;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Instructor;
import org.cpsolver.studentsct.model.Offering;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.RequestGroup;
import org.cpsolver.studentsct.model.SctAssignment;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Subpart;


/**
 * New weighting model. It tries to obey the following principles:
 * <ul>
 *      <li> Total student weight is between zero and one (one means student got the best schedule)
 *      <li> Weight of the given priority course is higher than sum of the remaining weights the student can get
 *      <li> First alternative is better than the following course
 *      <li> Second alternative is better than the second following course
 *      <li> Distance conflicts are considered secondary (priorities should be maximized first)
 *      <li> If alternative sections are otherwise equal, use the better balanced one
 * </ul>
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

public class PriorityStudentWeights implements StudentWeights {
    protected double iPriorityFactor = 0.5010;
    protected double iFirstAlternativeFactor = 0.5010;
    protected double iSecondAlternativeFactor = 0.2510;
    protected double iDistanceConflict = 0.0100;
    protected double iShortDistanceConflict = 0.1000;
    protected double iTimeOverlapFactor = 0.5000;
    protected double iTimeOverlapMaxLimit = 0.5000;
    protected boolean iLeftoverSpread = false;
    protected double iBalancingFactor = 0.0050;
    protected double iNoTimeFactor = 0.0100;
    protected double iOnlineFactor = 0.0100;
    protected double iPastFactor = 0.0000;
    protected double iReservationNotFollowedFactor = 0.1000;
    protected double iAlternativeRequestFactor = 0.1260;
    protected double iProjectedStudentWeight = 0.0100;
    protected boolean iMPP = false;
    protected double iPerturbationFactor = 0.100;
    protected double iSelectionFactor = 0.100;
    protected double iSameChoiceWeight = 0.900;
    protected double iSameTimeWeight = 0.700;
    protected double iSameConfigWeight = 0.500;
    protected double iGroupFactor = 0.100;
    protected double iGroupBestRatio = 0.95;
    protected double iGroupFillRatio = 0.05;
    protected boolean iAdditiveWeights = false;
    protected boolean iMaximizeAssignment = false;
    protected boolean iPreciseComparison = false;
    protected double[] iQalityWeights;
    protected boolean iImprovedBound = true;
    protected double iCriticalBoost = 1.0;
    protected double iPriortyBoost = 1.0;
    
    public PriorityStudentWeights(DataProperties config) {
        iPriorityFactor = config.getPropertyDouble("StudentWeights.Priority", iPriorityFactor);
        iFirstAlternativeFactor = config.getPropertyDouble("StudentWeights.FirstAlternative", iFirstAlternativeFactor);
        iSecondAlternativeFactor = config.getPropertyDouble("StudentWeights.SecondAlternative", iSecondAlternativeFactor);
        iDistanceConflict = config.getPropertyDouble("StudentWeights.DistanceConflict", iDistanceConflict);
        iShortDistanceConflict = config.getPropertyDouble("StudentWeights.ShortDistanceConflict", iShortDistanceConflict);
        iTimeOverlapFactor = config.getPropertyDouble("StudentWeights.TimeOverlapFactor", iTimeOverlapFactor);
        iTimeOverlapMaxLimit = config.getPropertyDouble("StudentWeights.TimeOverlapMaxLimit", iTimeOverlapMaxLimit);
        iLeftoverSpread = config.getPropertyBoolean("StudentWeights.LeftoverSpread", iLeftoverSpread);
        iBalancingFactor = config.getPropertyDouble("StudentWeights.BalancingFactor", iBalancingFactor);
        iAlternativeRequestFactor = config.getPropertyDouble("StudentWeights.AlternativeRequestFactor", iAlternativeRequestFactor);
        iProjectedStudentWeight = config.getPropertyDouble("StudentWeights.ProjectedStudentWeight", iProjectedStudentWeight);
        iMPP = config.getPropertyBoolean("General.MPP", false);
        iPerturbationFactor = config.getPropertyDouble("StudentWeights.Perturbation", iPerturbationFactor);
        iSelectionFactor = config.getPropertyDouble("StudentWeights.Selection", iSelectionFactor);
        iSameChoiceWeight = config.getPropertyDouble("StudentWeights.SameChoice", iSameChoiceWeight);
        iSameTimeWeight = config.getPropertyDouble("StudentWeights.SameTime", iSameTimeWeight);
        iSameConfigWeight = config.getPropertyDouble("StudentWeights.SameConfig", iSameConfigWeight);
        iGroupFactor = config.getPropertyDouble("StudentWeights.SameGroup", iGroupFactor);
        iGroupBestRatio = config.getPropertyDouble("StudentWeights.GroupBestRatio", iGroupBestRatio);
        iGroupFillRatio = config.getPropertyDouble("StudentWeights.GroupFillRatio", iGroupFillRatio);
        iNoTimeFactor = config.getPropertyDouble("StudentWeights.NoTimeFactor", iNoTimeFactor);
        iOnlineFactor = config.getPropertyDouble("StudentWeights.OnlineFactor", iOnlineFactor);
        iPastFactor = config.getPropertyDouble("StudentWeights.PastFactor", iPastFactor);
        iReservationNotFollowedFactor = config.getPropertyDouble("StudentWeights.ReservationNotFollowedFactor", iReservationNotFollowedFactor);
        iAdditiveWeights = config.getPropertyBoolean("StudentWeights.AdditiveWeights", iAdditiveWeights);
        iMaximizeAssignment = config.getPropertyBoolean("StudentWeights.MaximizeAssignment", iMaximizeAssignment);
        iPreciseComparison = config.getPropertyBoolean("StudentWeights.PreciseComparison", iPreciseComparison);
        iQalityWeights = new double[StudentQuality.Type.values().length];
        for (StudentQuality.Type type: StudentQuality.Type.values()) {
            iQalityWeights[type.ordinal()] = config.getPropertyDouble(type.getWeightName(), type.getWeightDefault());
        }
        iImprovedBound = config.getPropertyBoolean("StudentWeights.ImprovedBound", iImprovedBound);
        iPriortyBoost = config.getPropertyDouble("StudentWeights.PriortyBoost", 1.0);
        iCriticalBoost = config.getPropertyDouble("StudentWeights.CriticalBoost", 1.0);
    }
        
    public double getWeight(Request request) {
        if (request.getStudent().isDummy() && iProjectedStudentWeight >= 0.0) {
            double weight = iProjectedStudentWeight;
            if (request.isAlternative())
                weight *= iAlternativeRequestFactor;
            return weight;
        }
        double total = 1000000.0;
        int nrReq = request.getStudent().nrRequests();
        double remain = (iLeftoverSpread ? Math.floor(1000000.0 * Math.pow(iPriorityFactor, nrReq) / nrReq) : 0.0);
        for (int idx = 0; idx < request.getStudent().getRequests().size(); idx++) {
            Request r = request.getStudent().getRequests().get(idx);
            boolean last = (idx + 1 == request.getStudent().getRequests().size());
            boolean lastNotAlt = !r.isAlternative() && (last || request.getStudent().getRequests().get(1 + idx).isAlternative());
            double w = Math.ceil(iPriorityFactor * total) + remain;
            if (!iLeftoverSpread && lastNotAlt) {
                w = total;
            } else {
                total -= w;
            }
            if (r.equals(request)) {
                return w / 1000000.0;
            }
        }
        return 0.0;
    }

    public double getBoostedWeight(Request request) {
        double weight = getWeight(request);
        if (iPriortyBoost != 1.0) {
            Double boost = request.getStudent().getPriority().getBoost();
            if (boost != null)
                weight *= boost * iPriortyBoost;
        }
        if (iCriticalBoost != 1.0) {
            Double boost = request.getRequestPriority().getBoost();
            if (boost != null)
                weight *= boost * iCriticalBoost;
        }
        return weight;
    }
    
    public double getCachedWeight(Request request) {
        double[] cache = (double[])request.getExtra();
        if (cache == null) {
            double base = getBoostedWeight(request); 
            cache = new double[]{base, computeBound(base, request)};
            request.setExtra(cache);
        }
        return cache[0];
    }
    
    /**
     * Return how much the given enrollment is different from the initial enrollment
     * @param enrollment given enrollment
     * @return 0.0 when all the sections are the same, 1.0 when all the section are different (including different times)
     */
    protected double getDifference(Enrollment enrollment) {
        if (enrollment.getStudent().isDummy() || !enrollment.isCourseRequest()) return 1.0;
        Enrollment other = enrollment.getRequest().getInitialAssignment();
        if (other != null) {
            double similarSections = 0.0;
            if (enrollment.getConfig().equals(other.getConfig())) {
                // same configurations -- compare sections of matching subpart
                for (Section section: enrollment.getSections()) {
                    for (Section initial: other.getSections()) {
                        if (section.getSubpart().equals(initial.getSubpart())) {
                            if (section.equals(initial)) {
                                similarSections += 1.0;
                            } else if (section.sameChoice(initial)) {
                                similarSections += iSameChoiceWeight;
                            } else if (section.sameTime(initial)) {
                                similarSections += iSameTimeWeight;
                            }
                            break;
                        }
                    }
                }
            } else {
                // different configurations -- compare sections of matching itype
                for (Section section: enrollment.getSections()) {
                    for (Section initial: other.getSections()) {
                        if (section.sameChoice(initial)) {
                            similarSections += iSameChoiceWeight;
                            break;
                        } else if (section.sameInstructionalType(initial) && section.sameTime(initial)) {
                            similarSections += iSameTimeWeight;
                            break;
                        }
                    }
                }
            }
            return 1.0 - similarSections / enrollment.getAssignments().size();
        }
        return 1.0;
    }
    
    /**
     * Return how much the given enrollment is different from the selection (if any)
     * @param enrollment given enrollment
     * @return 0.0 when all the sections are the same, 1.0 when all the section are different (including different times)
     */
    public double getSelection(Enrollment enrollment) {
        if (enrollment.getStudent().isDummy()) return 1.0;
        if (enrollment.isCourseRequest()) {
            CourseRequest cr = (CourseRequest)enrollment.getRequest();
            if (!cr.getSelectedChoices().isEmpty()) {
                double similarSections = 0.0;
                for (Section section: enrollment.getSections()) {
                    double bestChoice = 0.0;
                    for (Choice ch: cr.getSelectedChoices()) {
                        if (bestChoice < 1.0 && ch.sameSection(section)) {
                            bestChoice = 1.0;
                        } else if (bestChoice < iSameChoiceWeight && ch.sameChoice(section)) {
                            bestChoice = iSameChoiceWeight;
                        } else if (bestChoice < iSameTimeWeight && ch.sameOffering(section) && ch.sameInstructionalType(section) && ch.sameTime(section)) {
                            bestChoice = iSameTimeWeight;
                        } else if (bestChoice < iSameConfigWeight && ch.sameConfiguration(section)) {
                            bestChoice = iSameConfigWeight;
                        }
                    }
                    similarSections += bestChoice;
                }
                return 1.0 - similarSections / enrollment.getAssignments().size();
            } else {
                return 1.0;
            }
        } else {
            return 1.0;
        }
    }
    

    @Override
    public double getBound(Request request) {
        double[] cache = (double[])request.getExtra();
        if (cache == null) {
            double base = getBoostedWeight(request); 
            cache = new double[]{base, computeBound(base, request)};
            request.setExtra(cache);
        }
        return cache[1];
    }
    
    protected double computeBound(double base, Request request) {
        if (!iImprovedBound) return base;
        if (iAdditiveWeights) {
            double weight = 0.0;
            if (request instanceof CourseRequest) {
                CourseRequest cr = (CourseRequest)request;
                if (iNoTimeFactor != 0.0 && !cr.getCourses().isEmpty()) {
                    weight += iNoTimeFactor * cr.getCourses().get(0).getArrHrsBound();
                }
                if (iOnlineFactor != 0.0 && !cr.getCourses().isEmpty()) {
                    weight += iOnlineFactor * cr.getCourses().get(0).getOnlineBound();
                }
                if (iPastFactor != 0.0 && !cr.getCourses().isEmpty()) {
                    weight += iPastFactor * cr.getCourses().get(0).getPastBound();
                }
                if (iMPP && cr.getInitialAssignment() == null) {
                    weight += iPerturbationFactor;
                }
                if (iSelectionFactor != 0.0 && cr.getSelectedChoices().isEmpty()) {
                    weight += iSelectionFactor;
                }
            }
            return round(base * (1.0 - weight));
        } else {
            double weight = base;
            if (request instanceof CourseRequest) {
                CourseRequest cr = (CourseRequest)request;
                if (iNoTimeFactor != 0.0 && !cr.getCourses().isEmpty()) {
                    weight *= (1.0 - iNoTimeFactor * cr.getCourses().get(0).getArrHrsBound());
                }
                if (iOnlineFactor != 0.0 && !cr.getCourses().isEmpty()) {
                    weight *= (1.0 - iOnlineFactor * cr.getCourses().get(0).getOnlineBound());
                }
                if (iPastFactor != 0.0 && !cr.getCourses().isEmpty()) {
                    weight *= (1.0 - iPastFactor * cr.getCourses().get(0).getPastBound());
                }
                if (iMPP && cr.getInitialAssignment() == null) {
                    weight *= (1.0 - iPerturbationFactor);
                }
                if (iSelectionFactor != 0.0 && cr.getSelectedChoices().isEmpty()) {
                    weight *= (1.0 - iSelectionFactor);
                }
            }
            return round(weight);
        }
    }
    
    protected double round(double value) {
        return Math.ceil(1000000.0 * value) / 1000000.0;
    }
    
    protected double getBaseWeight(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        double weight = getCachedWeight(enrollment.getRequest());
        switch (enrollment.getTruePriority()) {
            case 0: break;
            case 1: weight *= iFirstAlternativeFactor; break;
            case 2: weight *= iSecondAlternativeFactor; break;
            default:
                weight *= Math.pow(iFirstAlternativeFactor, enrollment.getTruePriority());
        }
        return weight;
    }
    
    @Override
    public double getWeight(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        if (iAdditiveWeights)
            return getWeightAdditive(assignment, enrollment);
        else
            return getWeightMultiplicative(assignment, enrollment);
    }
    
    public double getWeightMultiplicative(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        double weight = getBaseWeight(assignment, enrollment);
        if (enrollment.isCourseRequest() && iNoTimeFactor != 0.0) {
            int noTimeSections = 0, total = 0;
            for (Section section: enrollment.getSections()) {
                if (!section.hasTime()) noTimeSections ++;
                total ++;
            }
            if (noTimeSections > 0)
                weight *= (1.0 - iNoTimeFactor * noTimeSections / total);
        }
        if (enrollment.isCourseRequest() && iOnlineFactor != 0.0) {
            int onlineSections = 0, total = 0;
            for (Section section: enrollment.getSections()) {
                if (section.isOnline()) onlineSections ++;
                total ++;
            }
            if (onlineSections > 0)
                weight *= (1.0 - iOnlineFactor * onlineSections / total);
        }
        if (enrollment.isCourseRequest() && iPastFactor != 0.0) {
            int pastSections = 0, total = 0;
            for (Section section: enrollment.getSections()) {
                if (section.isPast()) pastSections ++;
                total ++;
            }
            if (pastSections > 0)
                weight *= (1.0 - iPastFactor * pastSections / total);
        }
        if (enrollment.getTruePriority() < enrollment.getPriority()) {
            weight *= (1.0 - iReservationNotFollowedFactor);
        }
        if (enrollment.isCourseRequest() && iBalancingFactor != 0.0) {
            double configUsed = enrollment.getConfig().getEnrollmentTotalWeight(assignment, enrollment.getRequest()) + enrollment.getRequest().getWeight();
            double disbalanced = 0;
            double total = 0;
            for (Section section: enrollment.getSections()) {
                Subpart subpart = section.getSubpart();
                if (subpart.getSections().size() <= 1) continue;
                double used = section.getEnrollmentTotalWeight(assignment, enrollment.getRequest()) + enrollment.getRequest().getWeight();
                // sections have limits -> desired size is section limit x (total enrollment / total limit)
                // unlimited sections -> desired size is total enrollment / number of sections
                double desired = (subpart.getLimit() > 0
                        ? section.getLimit() * (configUsed / subpart.getLimit())
                        : configUsed / subpart.getSections().size());
                if (used > desired)
                    disbalanced += Math.min(enrollment.getRequest().getWeight(), used - desired) / enrollment.getRequest().getWeight();
                else
                    disbalanced -= Math.min(enrollment.getRequest().getWeight(), desired - used) / enrollment.getRequest().getWeight();
                total ++;
            }
            if (disbalanced > 0)
                weight *= (1.0 - disbalanced / total * iBalancingFactor);
        }
        if (iMPP) {
            double difference = getDifference(enrollment);
            if (difference > 0.0)
                weight *= (1.0 - difference * iPerturbationFactor);
        }
        if (iSelectionFactor != 0.0) {
            double selection = getSelection(enrollment);
            if (selection > 0.0)
                weight *= (1.0 - selection * iSelectionFactor);
        }
        if (enrollment.isCourseRequest() && iGroupFactor != 0.0) {
            double sameGroup = 0.0; int groupCount = 0;
            for (RequestGroup g: ((CourseRequest)enrollment.getRequest()).getRequestGroups()) {
                if (g.getCourse().equals(enrollment.getCourse())) {
                    sameGroup += g.getEnrollmentSpread(assignment, enrollment, iGroupBestRatio, iGroupFillRatio);
                    groupCount ++;
                }
            }
            if (groupCount > 0) {
                double difference = 1.0 - sameGroup / groupCount;
                weight *= (1.0 - difference * iGroupFactor);
            }
        }
        return round(weight);
    }
    
    public double getWeightAdditive(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        double base = getBaseWeight(assignment, enrollment);
        double weight = 0.0;
        if (enrollment.isCourseRequest() && iNoTimeFactor != 0.0) {
            int noTimeSections = 0, total = 0;
            for (Section section: enrollment.getSections()) {
                if (!section.hasTime()) noTimeSections ++;
                total ++;
            }
            if (noTimeSections > 0)
                weight += iNoTimeFactor * noTimeSections / total;
        }
        if (enrollment.isCourseRequest() && iOnlineFactor != 0.0) {
            int onlineSections = 0, total = 0;
            for (Section section: enrollment.getSections()) {
                if (section.isOnline()) onlineSections ++;
                total ++;
            }
            if (onlineSections > 0)
                weight += iOnlineFactor * onlineSections / total;
        }
        if (enrollment.isCourseRequest() && iPastFactor != 0.0) {
            int pastSections = 0, total = 0;
            for (Section section: enrollment.getSections()) {
                if (section.isPast()) pastSections ++;
                total ++;
            }
            if (pastSections > 0)
                weight += iPastFactor * pastSections / total;
        }
        if (enrollment.getTruePriority() < enrollment.getPriority()) {
            weight += iReservationNotFollowedFactor;
        }
        if (enrollment.isCourseRequest() && iBalancingFactor != 0.0) {
            double configUsed = enrollment.getConfig().getEnrollmentTotalWeight(assignment, enrollment.getRequest()) + enrollment.getRequest().getWeight();
            double disbalanced = 0;
            double total = 0;
            for (Section section: enrollment.getSections()) {
                Subpart subpart = section.getSubpart();
                if (subpart.getSections().size() <= 1) continue;
                double used = section.getEnrollmentTotalWeight(assignment, enrollment.getRequest()) + enrollment.getRequest().getWeight();
                // sections have limits -> desired size is section limit x (total enrollment / total limit)
                // unlimited sections -> desired size is total enrollment / number of sections
                double desired = (subpart.getLimit() > 0
                        ? section.getLimit() * (configUsed / subpart.getLimit())
                        : configUsed / subpart.getSections().size());
                if (used > desired)
                    disbalanced += Math.min(enrollment.getRequest().getWeight(), used - desired) / enrollment.getRequest().getWeight();
                else
                    disbalanced -= Math.min(enrollment.getRequest().getWeight(), desired - used) / enrollment.getRequest().getWeight();
                total ++;
            }
            if (disbalanced > 0)
                weight += disbalanced / total * iBalancingFactor;
        }
        if (iMPP) {
            double difference = getDifference(enrollment);
            if (difference > 0.0)
                weight += difference * iPerturbationFactor;
        }
        if (iSelectionFactor != 0.0) {
            double selection = getSelection(enrollment);
            if (selection > 0.0)
                weight += selection * iSelectionFactor;
        }
        if (enrollment.isCourseRequest() && iGroupFactor != 0.0) {
            double sameGroup = 0.0; int groupCount = 0;
            for (RequestGroup g: ((CourseRequest)enrollment.getRequest()).getRequestGroups()) {
                if (g.getCourse().equals(enrollment.getCourse())) {
                    sameGroup += g.getEnrollmentSpread(assignment, enrollment, iGroupBestRatio, iGroupFillRatio);
                    groupCount ++;
                }
            }
            if (groupCount > 0) {
                double difference = 1.0 - sameGroup / groupCount;
                weight += difference * iGroupFactor;
            }
        }
        return round(base * (1.0 - weight));
    }
    
    @Override
    public double getDistanceConflictWeight(Assignment<Request, Enrollment> assignment, DistanceConflict.Conflict c) {
        if (iAdditiveWeights) {
            if (c.getR1().getPriority() < c.getR2().getPriority()) {
                return round(getBaseWeight(assignment, c.getE2()) * (c.getStudent().isNeedShortDistances() ? iShortDistanceConflict : iDistanceConflict));
            } else {
                return round(getBaseWeight(assignment, c.getE1()) * (c.getStudent().isNeedShortDistances() ? iShortDistanceConflict : iDistanceConflict));
            }
        } else {
            if (c.getR1().getPriority() < c.getR2().getPriority()) {
                return round(getWeightMultiplicative(assignment, c.getE2()) * (c.getStudent().isNeedShortDistances() ? iShortDistanceConflict : iDistanceConflict));
            } else {
                return round(getWeightMultiplicative(assignment, c.getE1()) * (c.getStudent().isNeedShortDistances() ? iShortDistanceConflict : iDistanceConflict));
            }
        }
    }
    
    @Override
    public double getTimeOverlapConflictWeight(Assignment<Request, Enrollment> assignment, Enrollment e, TimeOverlapsCounter.Conflict c) {
        if (e == null || e.getRequest() == null) return 0.0;
        double toc = Math.min(iTimeOverlapFactor * c.getShare() / e.getNrSlots(), iTimeOverlapMaxLimit);
        if (iAdditiveWeights) {
            return round(getBaseWeight(assignment, e) * toc);
        } else {
            return round(getWeightMultiplicative(assignment, e) * toc);
        }
    }
    
    @Override
    public double getWeight(Assignment<Request, Enrollment> assignment, Enrollment enrollment, Set<DistanceConflict.Conflict> distanceConflicts, Set<TimeOverlapsCounter.Conflict> timeOverlappingConflicts) {
        if (iAdditiveWeights) {
            double base = getBaseWeight(assignment, enrollment);
            double dc = 0.0;
            if (distanceConflicts != null) {
                for (DistanceConflict.Conflict c: distanceConflicts) {
                    Enrollment other = (c.getE1().equals(enrollment) ? c.getE2() : c.getE1());
                    if (other.getRequest().getPriority() <= enrollment.getRequest().getPriority())
                        dc += base * (c.getStudent().isNeedShortDistances() ? iShortDistanceConflict : iDistanceConflict);
                    else
                        dc += getBaseWeight(assignment, other) * (c.getStudent().isNeedShortDistances() ? iShortDistanceConflict : iDistanceConflict);
                }
            }
            double toc = 0.0;
            if (timeOverlappingConflicts != null) {
                for (TimeOverlapsCounter.Conflict c: timeOverlappingConflicts) {
                    toc += base * Math.min(iTimeOverlapFactor * c.getShare() / enrollment.getNrSlots(), iTimeOverlapMaxLimit);
                    Enrollment other = (c.getE1().equals(enrollment) ? c.getE2() : c.getE1());
                    if (other.getRequest() != null)
                        toc += getBaseWeight(assignment, other) * Math.min(iTimeOverlapFactor * c.getShare() / other.getNrSlots(), iTimeOverlapMaxLimit);
                }
            }
            return round(getWeight(assignment, enrollment) - dc - toc);
        } else {
            double base = getWeightMultiplicative(assignment, enrollment);
            double dc = 0.0;
            if (distanceConflicts != null) {
                for (DistanceConflict.Conflict c: distanceConflicts) {
                    Enrollment other = (c.getE1().equals(enrollment) ? c.getE2() : c.getE1());
                    if (other.getRequest().getPriority() <= enrollment.getRequest().getPriority())
                        dc += base * (c.getStudent().isNeedShortDistances() ? iShortDistanceConflict : iDistanceConflict);
                    else
                        dc += getWeightMultiplicative(assignment, other) * (c.getStudent().isNeedShortDistances() ? iShortDistanceConflict : iDistanceConflict);
                }
            }
            double toc = 0.0;
            if (timeOverlappingConflicts != null) {
                for (TimeOverlapsCounter.Conflict c: timeOverlappingConflicts) {
                    toc += base * Math.min(iTimeOverlapFactor * c.getShare() / enrollment.getNrSlots(), iTimeOverlapMaxLimit);
                    Enrollment other = (c.getE1().equals(enrollment) ? c.getE2() : c.getE1());
                    if (other.getRequest() != null)
                        toc += getWeightMultiplicative(assignment, other) * Math.min(iTimeOverlapFactor * c.getShare() / other.getNrSlots(), iTimeOverlapMaxLimit);
                }
            }
            return round(base - dc - toc);
        }
    }
    
    
    @Override
    public boolean isBetterThanBestSolution(Solution<Request, Enrollment> currentSolution) {
        if (currentSolution.getBestInfo() == null) return true;
        if (iMaximizeAssignment) {
            long acr = Math.round(((StudentSectioningModel)currentSolution.getModel()).getContext(currentSolution.getAssignment()).getAssignedCourseRequestWeight());
            long bcr = Math.round(((StudentSectioningModel)currentSolution.getModel()).getBestAssignedCourseRequestWeight());
            if (acr != bcr)
                return acr > bcr;
        }
        return ((StudentSectioningModel)currentSolution.getModel()).getTotalValue(currentSolution.getAssignment(), iPreciseComparison) < currentSolution.getBestValue();
    }
    
    @Override
    public boolean isFreeTimeAllowOverlaps() {
        return false;
    }
    
    /**
     * Test case -- run to see the weights for a few courses
     * @param args program arguments
     */
    public static void main(String[] args) {
        PriorityStudentWeights pw = new PriorityStudentWeights(new DataProperties());
        DecimalFormat df = new DecimalFormat("0.000000");
        Student s = new Student(0l);
        new CourseRequest(1l, 0, false, s, ToolBox.toList(
                new Course(1, "A", "1", new Offering(0, "A")),
                new Course(1, "A", "2", new Offering(0, "A")),
                new Course(1, "A", "3", new Offering(0, "A"))), false, null);
        new CourseRequest(2l, 1, false, s, ToolBox.toList(
                new Course(1, "B", "1", new Offering(0, "B")),
                new Course(1, "B", "2", new Offering(0, "B")),
                new Course(1, "B", "3", new Offering(0, "B"))), false, null);
        new CourseRequest(3l, 2, false, s, ToolBox.toList(
                new Course(1, "C", "1", new Offering(0, "C")),
                new Course(1, "C", "2", new Offering(0, "C")),
                new Course(1, "C", "3", new Offering(0, "C"))), false, null);
        new CourseRequest(4l, 3, false, s, ToolBox.toList(
                new Course(1, "D", "1", new Offering(0, "D")),
                new Course(1, "D", "2", new Offering(0, "D")),
                new Course(1, "D", "3", new Offering(0, "D"))), false, null);
        new CourseRequest(5l, 4, false, s, ToolBox.toList(
                new Course(1, "E", "1", new Offering(0, "E")),
                new Course(1, "E", "2", new Offering(0, "E")),
                new Course(1, "E", "3", new Offering(0, "E"))), false, null);
        new CourseRequest(6l, 5, true, s, ToolBox.toList(
                new Course(1, "F", "1", new Offering(0, "F")),
                new Course(1, "F", "2", new Offering(0, "F")),
                new Course(1, "F", "3", new Offering(0, "F"))), false, null);
        new CourseRequest(7l, 6, true, s, ToolBox.toList(
                new Course(1, "G", "1", new Offering(0, "G")),
                new Course(1, "G", "2", new Offering(0, "G")),
                new Course(1, "G", "3", new Offering(0, "G"))), false, null);
        
        Assignment<Request, Enrollment> assignment = new DefaultSingleAssignment<Request, Enrollment>();
        Placement p = new Placement(null, new TimeLocation(1, 90, 12, 0, 0, null, null, new BitSet(), 10), new ArrayList<RoomLocation>());
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<SctAssignment> sections = new HashSet<SctAssignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections, assignment);
                w[i] = pw.getWeight(assignment, e, null, null);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }

        System.out.println("With one distance conflict:");
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<SctAssignment> sections = new HashSet<SctAssignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections, assignment);
                Set<DistanceConflict.Conflict> dc = new HashSet<DistanceConflict.Conflict>();
                dc.add(new DistanceConflict.Conflict(s, e, (Section)sections.iterator().next(), e, (Section)sections.iterator().next()));
                w[i] = pw.getWeight(assignment, e, dc, null);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }

        System.out.println("With two distance conflicts:");
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<SctAssignment> sections = new HashSet<SctAssignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections, assignment);
                Set<DistanceConflict.Conflict> dc = new HashSet<DistanceConflict.Conflict>();
                dc.add(new DistanceConflict.Conflict(s, e, (Section)sections.iterator().next(), e, (Section)sections.iterator().next()));
                dc.add(new DistanceConflict.Conflict(s, e, (Section)sections.iterator().next(), e,
                        new Section(1, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null)));
                w[i] = pw.getWeight(assignment, e, dc, null);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }

        System.out.println("With 25% time overlapping conflict:");
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<SctAssignment> sections = new HashSet<SctAssignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections, assignment);
                Set<TimeOverlapsCounter.Conflict> toc = new HashSet<TimeOverlapsCounter.Conflict>();
                toc.add(new TimeOverlapsCounter.Conflict(s, 3, e, sections.iterator().next(), e, sections.iterator().next()));
                w[i] = pw.getWeight(assignment, e, null, toc);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }
        
        System.out.println("Disbalanced sections (by 2 / 10 students):");
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<SctAssignment> sections = new HashSet<SctAssignment>();
                Subpart x = new Subpart(0, "Lec", "Lec", cfg, null);
                Section a = new Section(0, 10, "x", x, p, null);
                new Section(1, 10, "y", x, p, null);
                sections.add(a);
                a.assigned(assignment, new Enrollment(s.getRequests().get(0), i, cfg, sections, assignment));
                a.assigned(assignment, new Enrollment(s.getRequests().get(0), i, cfg, sections, assignment));
                cfg.getContext(assignment).assigned(assignment, new Enrollment(s.getRequests().get(0), i, cfg, sections, assignment));
                cfg.getContext(assignment).assigned(assignment, new Enrollment(s.getRequests().get(0), i, cfg, sections, assignment));
                Enrollment e = new Enrollment(cr, i, cfg, sections, assignment);
                w[i] = pw.getWeight(assignment, e, null, null);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }
        
        System.out.println("Same choice sections:");
        pw.iMPP = true;
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<SctAssignment> sections = new HashSet<SctAssignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections, assignment);
                Set<SctAssignment> other = new HashSet<SctAssignment>();
                other.add(new Section(1, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null));
                cr.setInitialAssignment(new Enrollment(cr, i, cfg, other, assignment));
                w[i] = pw.getWeight(assignment, e, null, null);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }
        
        System.out.println("Same time sections:");
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<SctAssignment> sections = new HashSet<SctAssignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections, assignment);
                Set<SctAssignment> other = new HashSet<SctAssignment>();
                other.add(new Section(1, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null, new Instructor(1l, null, "Josef Novak", null)));
                cr.setInitialAssignment(new Enrollment(cr, i, cfg, other, assignment));
                w[i] = pw.getWeight(assignment, e, null, null);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }
        
        System.out.println("Different time sections:");
        Placement q = new Placement(null, new TimeLocation(1, 102, 12, 0, 0, null, null, new BitSet(), 10), new ArrayList<RoomLocation>());
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<SctAssignment> sections = new HashSet<SctAssignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections, assignment);
                Set<SctAssignment> other = new HashSet<SctAssignment>();
                other.add(new Section(1, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), q, null));
                cr.setInitialAssignment(new Enrollment(cr, i, cfg, other, assignment));
                w[i] = pw.getWeight(assignment, e, null, null);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }
        
        System.out.println("Two sections, one same choice, one same time:");
        for (Request r: s.getRequests()) {
            CourseRequest cr = (CourseRequest)r;
            double[] w = new double[] {0.0, 0.0, 0.0};
            for (int i = 0; i < cr.getCourses().size(); i++) {
                Config cfg = new Config(0l, -1, "", cr.getCourses().get(i).getOffering());
                Set<SctAssignment> sections = new HashSet<SctAssignment>();
                sections.add(new Section(0, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null));
                sections.add(new Section(1, 1, "y", new Subpart(1, "Rec", "Rec", cfg, null), p, null));
                Enrollment e = new Enrollment(cr, i, cfg, sections, assignment);
                Set<SctAssignment> other = new HashSet<SctAssignment>();
                other.add(new Section(2, 1, "x", new Subpart(0, "Lec", "Lec", cfg, null), p, null));
                other.add(new Section(3, 1, "y", new Subpart(1, "Rec", "Rec", cfg, null), p, null, new Instructor(1l, null, "Josef Novak", null)));
                cr.setInitialAssignment(new Enrollment(cr, i, cfg, other, assignment));
                w[i] = pw.getWeight(assignment, e, null, null);
            }
            System.out.println(cr + ": " + df.format(w[0]) + "  " + df.format(w[1]) + "  " + df.format(w[2]));
        }

    }

    @Override
    public double getWeight(Assignment<Request, Enrollment> assignment, Enrollment enrollment, Set<StudentQuality.Conflict> qualityConflicts) {
        if (iAdditiveWeights) {
            double base = getBaseWeight(assignment, enrollment);
            double penalty = 0.0;
            if (qualityConflicts != null) {
                for (StudentQuality.Conflict c: qualityConflicts) {
                    switch (c.getType().getType()) {
                        case REQUEST:
                            if (enrollment.isCourseRequest())
                                penalty += base * iQalityWeights[c.getType().ordinal()] * c.getWeight(enrollment);
                            break;
                        case BOTH:
                            Enrollment other = c.getOther(enrollment);
                            penalty += base * iQalityWeights[c.getType().ordinal()] * c.getWeight(enrollment);
                            penalty += getBaseWeight(assignment, other) * iQalityWeights[c.getType().ordinal()] * c.getWeight(other);
                            break;
                        case LOWER:
                            other = c.getOther(enrollment);
                            if (other.getRequest().getPriority() <= enrollment.getRequest().getPriority())
                                penalty += base * iQalityWeights[c.getType().ordinal()] * c.getWeight(enrollment);
                            else
                                penalty += getBaseWeight(assignment, other) * iQalityWeights[c.getType().ordinal()] * c.getWeight(other);
                            break;
                        case HIGHER:
                            other = c.getOther(enrollment);
                            if (other.getRequest().getPriority() >= enrollment.getRequest().getPriority())
                                penalty += base * iQalityWeights[c.getType().ordinal()] * c.getWeight(enrollment);
                            else
                                penalty += getBaseWeight(assignment, other) * iQalityWeights[c.getType().ordinal()] * c.getWeight(other);
                    }
                }
            }
            return round(getWeight(assignment, enrollment) - penalty);
        } else {
            double base = getWeightMultiplicative(assignment, enrollment);
            double penalty = 0.0;
            if (qualityConflicts != null) {
                for (StudentQuality.Conflict c: qualityConflicts) {
                    Enrollment other = c.getOther(enrollment);
                    switch (c.getType().getType()) {
                        case REQUEST:
                            if (enrollment.isCourseRequest())
                                penalty += base * iQalityWeights[c.getType().ordinal()] * c.getWeight(enrollment);
                            else if (other.isCourseRequest())
                                penalty += getWeightMultiplicative(assignment, other) * iQalityWeights[c.getType().ordinal()] * c.getWeight(other);
                            break;
                        case BOTH:
                            penalty += base * iQalityWeights[c.getType().ordinal()] * c.getWeight(enrollment);
                            if (other.getRequest() != null)
                                penalty += getWeightMultiplicative(assignment, other) * iQalityWeights[c.getType().ordinal()] * c.getWeight(other);
                            break;
                        case LOWER:
                            other = c.getOther(enrollment);
                            if (other.getRequest().getPriority() <= enrollment.getRequest().getPriority())
                                penalty += base * iQalityWeights[c.getType().ordinal()] * c.getWeight(enrollment);
                            else if (other.getRequest() != null)
                                penalty += getWeightMultiplicative(assignment, other) * iQalityWeights[c.getType().ordinal()] * c.getWeight(other);
                            break;
                        case HIGHER:
                            other = c.getOther(enrollment);
                            if (other.getRequest().getPriority() >= enrollment.getRequest().getPriority())
                                penalty += base * iQalityWeights[c.getType().ordinal()] * c.getWeight(enrollment);
                            else if (other.getRequest() != null)
                                penalty += getWeightMultiplicative(assignment, other) * iQalityWeights[c.getType().ordinal()] * c.getWeight(other);
                    }
                }
            }
            return round(base - penalty);
        }
    }

    @Override
    public double getStudentQualityConflictWeight(Assignment<Request, Enrollment> assignment, Enrollment enrollment, Conflict conflict) {
        switch (conflict.getType().getType()) {
            case BOTH:
                if (enrollment == null || enrollment.getRequest() == null) return 0.0;
                if (iAdditiveWeights) {
                    return round(getBaseWeight(assignment, enrollment) * iQalityWeights[conflict.getType().ordinal()] * conflict.getWeight(enrollment));
                } else {
                    return round(getWeightMultiplicative(assignment, enrollment) * iQalityWeights[conflict.getType().ordinal()] * conflict.getWeight(enrollment));
                }
            case REQUEST:
                if (enrollment == null || enrollment.getRequest() == null || !enrollment.isCourseRequest()) return 0.0;
                if (iAdditiveWeights) {
                    return round(getBaseWeight(assignment, enrollment) * iQalityWeights[conflict.getType().ordinal()] * conflict.getWeight(enrollment));
                } else {
                    return round(getWeightMultiplicative(assignment, enrollment) * iQalityWeights[conflict.getType().ordinal()] * conflict.getWeight(enrollment));
                }
            case LOWER:
                if (iAdditiveWeights) {
                    if (conflict.getR1().getPriority() < conflict.getR2().getPriority()) {
                        return round(getBaseWeight(assignment, conflict.getE2()) * iQalityWeights[conflict.getType().ordinal()] * conflict.getWeight(conflict.getE2()));
                    } else {
                        return round(getBaseWeight(assignment, conflict.getE1()) * iQalityWeights[conflict.getType().ordinal()] * conflict.getWeight(conflict.getE1()));
                    }
                } else {
                    if (conflict.getR1().getPriority() < conflict.getR2().getPriority()) {
                        return round(getWeightMultiplicative(assignment, conflict.getE2()) * iQalityWeights[conflict.getType().ordinal()] * conflict.getWeight(conflict.getE2()));
                    } else {
                        return round(getWeightMultiplicative(assignment, conflict.getE1()) * iQalityWeights[conflict.getType().ordinal()] * conflict.getWeight(conflict.getE1()));
                    }
                }
            case HIGHER:
                if (iAdditiveWeights) {
                    if (conflict.getR1().getPriority() > conflict.getR2().getPriority()) {
                        return round(getBaseWeight(assignment, conflict.getE2()) * iQalityWeights[conflict.getType().ordinal()] * conflict.getWeight(conflict.getE2()));
                    } else {
                        return round(getBaseWeight(assignment, conflict.getE1()) * iQalityWeights[conflict.getType().ordinal()] * conflict.getWeight(conflict.getE1()));
                    }
                } else {
                    if (conflict.getR1().getPriority() < conflict.getR2().getPriority()) {
                        return round(getWeightMultiplicative(assignment, conflict.getE2()) * iQalityWeights[conflict.getType().ordinal()] * conflict.getWeight(conflict.getE2()));
                    } else {
                        return round(getWeightMultiplicative(assignment, conflict.getE1()) * iQalityWeights[conflict.getType().ordinal()] * conflict.getWeight(conflict.getE1()));
                    }
                }
            default:
                return 0.0;
        }
    }
}
