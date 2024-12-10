package org.cpsolver.studentsct.extension;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.logging.log4j.Logger;
import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.CanInheritContext;
import org.cpsolver.ifs.assignment.context.ExtensionWithContext;
import org.cpsolver.ifs.model.InfoProvider;
import org.cpsolver.ifs.model.ModelListener;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.DistanceMetric;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.StudentSectioningModel.StudentSectioningModelContext;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.FreeTimeRequest;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.SctAssignment;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Student.BackToBackPreference;
import org.cpsolver.studentsct.model.Student.ModalityPreference;

import org.cpsolver.studentsct.model.Unavailability;

/**
 * This extension computes student schedule quality using various matrices.
 * It replaces {@link TimeOverlapsCounter} and {@link DistanceConflict} extensions.
 * Besides of time and distance conflicts, it also counts cases when a student
 * has a lunch break conflict, travel time during the day, it can prefer
 * or discourage student class back-to-back and cases when a student has more than
 * a given number of hours between the first and the last class on a day.
 * See {@link StudentQuality.Type} for more details.
 * 
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

public class StudentQuality extends ExtensionWithContext<Request, Enrollment, StudentQuality.StudentQualityContext> implements ModelListener<Request, Enrollment>, CanInheritContext<Request, Enrollment, StudentQuality.StudentQualityContext>, InfoProvider<Request, Enrollment> {
    private static Logger sLog = org.apache.logging.log4j.LogManager.getLogger(StudentQuality.class);
    private Context iContext;
    
    /**
     * Constructor
     * @param solver student scheduling solver
     * @param properties solver configuration
     */
    public StudentQuality(Solver<Request, Enrollment> solver, DataProperties properties) {
        super(solver, properties);
        if (solver != null) {
            StudentSectioningModel model = (StudentSectioningModel) solver.currentSolution().getModel(); 
            iContext = new Context(model.getDistanceMetric(), properties);
            model.setStudentQuality(this, false);
        } else {
            iContext = new Context(null, properties);
        }
    }
    
    /**
     * Constructor
     * @param metrics distance metric
     * @param properties solver configuration
     */
    public StudentQuality(DistanceMetric metrics, DataProperties properties) {
        super(null, properties);
        iContext = new Context(metrics, properties);
    }
    
    /**
     * Current distance metric
     * @return distance metric
     */
    public DistanceMetric getDistanceMetric() {
        return iContext.getDistanceMetric();
    }
    
    /**
     * Is debugging enabled
     * @return true when StudentQuality.Debug is true
     */
    public boolean isDebug() {
        return iContext.isDebug();
    }
    
    /**
     * Student quality context
     */
    public Context getStudentQualityContext() {
        return iContext;
    }
    
    /**
     * Weighting types 
     */
    public static enum WeightType {
        /** Penalty is incurred on the request with higher priority */
        HIGHER,
        /** Penalty is incurred on the request with lower priority */
        LOWER,
        /** Penalty is incurred on both requests */
        BOTH,
        /** Penalty is incurred on the course request (for conflicts between course request and a free time) */
        REQUEST,
        ;
    }
    
    /**
     * Measured student qualities
     *
     */
    public static enum Type {
        /** 
         * Time conflicts between two classes that is allowed. Time conflicts are penalized as shared time
         * between two course requests proportional to the time of each, capped at one half of the time.
         * This criterion is weighted by StudentWeights.TimeOverlapFactor, defaulting to 0.5.
         */
        CourseTimeOverlap(WeightType.BOTH, "StudentWeights.TimeOverlapFactor", 0.5000, new Quality(){
            @Override
            public boolean isApplicable(Context cx, Student student, Request r1, Request r2) {
                return r1 instanceof CourseRequest && r2 instanceof CourseRequest;
            }

            @Override
            public boolean inConflict(Context cx, SctAssignment a1, SctAssignment a2) {
                if (a1.getTime() == null || a2.getTime() == null) return false;
                if (((Section)a1).isToIgnoreStudentConflictsWith(a2.getId())) return false;
                return a1.getTime().hasIntersection(a2.getTime());
            }

            @Override
            public int penalty(Context cx, Student s, SctAssignment a1, SctAssignment a2) {
                if (!inConflict(cx, a1, a2)) return 0;
                return a1.getTime().nrSharedDays(a2.getTime()) * a1.getTime().nrSharedHours(a2.getTime());
            }
            
            @Override
            public Iterable<? extends SctAssignment> other(Context cx, Enrollment e) {
                return new Nothing();
            }

            @Override
            public double getWeight(Context cx, Conflict c, Enrollment e) {
                return Math.min(cx.getTimeOverlapMaxLimit() * c.getPenalty() / e.getNrSlots(), cx.getTimeOverlapMaxLimit());
            }
        }),
        /** 
         * Time conflict between class and a free time request. Free time conflicts are penalized as the time
         * of a course request overlapping with a free time proportional to the time of the request, capped at one half
         * of the time. This criterion is weighted by StudentWeights.TimeOverlapFactor, defaulting to 0.5.
         */
        FreeTimeOverlap(WeightType.REQUEST, "StudentWeights.TimeOverlapFactor", 0.5000, new Quality(){
            @Override
            public boolean isApplicable(Context cx, Student student, Request r1, Request r2) {
                return false;
            }

            @Override
            public boolean inConflict(Context cx, SctAssignment a1, SctAssignment a2) {
                if (a1.getTime() == null || a2.getTime() == null) return false;
                return a1.getTime().hasIntersection(a2.getTime());
            }

            @Override
            public int penalty(Context cx, Student s, SctAssignment a1, SctAssignment a2) {
                if (!inConflict(cx, a1, a2)) return 0;
                return a1.getTime().nrSharedDays(a2.getTime()) * a1.getTime().nrSharedHours(a2.getTime());
            }
            
            @Override
            public Iterable<? extends SctAssignment> other(Context cx, Enrollment e) {
                return (e.isCourseRequest() ? new FreeTimes(e.getStudent()) : new Nothing());
            }
            
            @Override
            public double getWeight(Context cx, Conflict c, Enrollment e) {
                return Math.min(cx.getTimeOverlapMaxLimit() * c.getPenalty() / c.getE1().getNrSlots(), cx.getTimeOverlapMaxLimit());
            }
        }),
        /** 
         * Student unavailability conflict. Time conflict between a class that the student is taking and a class that the student
         * is teaching (if time conflicts are allowed). Unavailability conflicts are penalized as the time
         * of a course request overlapping with an unavailability proportional to the time of the request, capped at one half
         * of the time. This criterion is weighted by StudentWeights.TimeOverlapFactor, defaulting to 0.5.
         */
        Unavailability(WeightType.REQUEST, "StudentWeights.TimeOverlapFactor", 0.5000, new Quality(){
            @Override
            public boolean isApplicable(Context cx, Student student, Request r1, Request r2) {
                return false;
            }

            @Override
            public boolean inConflict(Context cx, SctAssignment a1, SctAssignment a2) {
                if (a1.getTime() == null || a2.getTime() == null) return false;
                return a1.getTime().hasIntersection(a2.getTime());
            }

            @Override
            public int penalty(Context cx, Student s, SctAssignment a1, SctAssignment a2) {
                if (!inConflict(cx, a1, a2)) return 0;
                return a1.getTime().nrSharedDays(a2.getTime()) * a1.getTime().nrSharedHours(a2.getTime());
            }

            @Override
            public Iterable<? extends SctAssignment> other(Context cx, Enrollment e) {
                return (e.isCourseRequest() ? new Unavailabilities(e.getStudent()) : new Nothing());
            }
            
            @Override
            public double getWeight(Context cx, Conflict c, Enrollment e) {
                return Math.min(cx.getTimeOverlapMaxLimit() * c.getPenalty() / c.getE1().getNrSlots(), cx.getTimeOverlapMaxLimit());
            }
        }),
        /**
         * Distance conflict. When Distances.ComputeDistanceConflictsBetweenNonBTBClasses is set to false,
         * distance conflicts are only considered between back-to-back classes (break time of the first 
         * class is shorter than the distance in minutes between the two classes). When 
         * Distances.ComputeDistanceConflictsBetweenNonBTBClasses is set to true, the distance between the
         * two classes is also considered.
         * This criterion is weighted by StudentWeights.DistanceConflict, defaulting to 0.01.
         */
        Distance(WeightType.LOWER, "StudentWeights.DistanceConflict", 0.0100, new Quality(){
            @Override
            public boolean isApplicable(Context cx, Student student, Request r1, Request r2) {
                return r1 instanceof CourseRequest && r2 instanceof CourseRequest;
            }

            @Override
            public boolean inConflict(Context cx, SctAssignment sa1, SctAssignment sa2) {
                Section s1 = (Section) sa1;
                Section s2 = (Section) sa2;
                if (s1.getPlacement() == null || s2.getPlacement() == null)
                    return false;
                TimeLocation t1 = s1.getTime();
                TimeLocation t2 = s2.getTime();
                if (!t1.shareDays(t2) || !t1.shareWeeks(t2))
                    return false;
                int a1 = t1.getStartSlot(), a2 = t2.getStartSlot();
                if (cx.getDistanceMetric().doComputeDistanceConflictsBetweenNonBTBClasses()) {
                    if (a1 + t1.getNrSlotsPerMeeting() <= a2) {
                        int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                        if (dist > t1.getBreakTime() + Constants.SLOT_LENGTH_MIN * (a2 - a1 - t1.getLength()))
                            return true;
                    } else if (a2 + t2.getNrSlotsPerMeeting() <= a1) {
                        int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                        if (dist > t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (a1 - a2 - t2.getLength()))
                            return true;
                    }
                } else {
                    if (a1 + t1.getNrSlotsPerMeeting() == a2) {
                        int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                        if (dist > t1.getBreakTime())
                            return true;
                    } else if (a2 + t2.getNrSlotsPerMeeting() == a1) {
                        int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                        if (dist > t2.getBreakTime())
                            return true;
                    }
                }
                return false;
            }

            @Override
            public int penalty(Context cx, Student s, SctAssignment a1, SctAssignment a2) {
                return inConflict(cx, a1, a2) ? 1 : 0;
            }

            @Override
            public Iterable<? extends SctAssignment> other(Context cx, Enrollment e) {
                return new Nothing();
            }
            
            @Override
            public double getWeight(Context cx, Conflict c, Enrollment e) {
                return c.getPenalty();
            }
        }),
        /**
         * Short distance conflict. Similar to distance conflicts but for students that require short
         * distances. When Distances.ComputeDistanceConflictsBetweenNonBTBClasses is set to false,
         * distance conflicts are only considered between back-to-back classes (travel time between the
         * two classes is more than zero minutes). When 
         * Distances.ComputeDistanceConflictsBetweenNonBTBClasses is set to true, the distance between the
         * two classes is also considered (break time is also ignored).
         * This criterion is weighted by StudentWeights.ShortDistanceConflict, defaulting to 0.1.
         */
        ShortDistance(WeightType.LOWER, "StudentWeights.ShortDistanceConflict", 0.1000, new Quality(){
            @Override
            public boolean isApplicable(Context cx, Student student, Request r1, Request r2) {
                return student.isNeedShortDistances() && r1 instanceof CourseRequest && r2 instanceof CourseRequest;
            }

            @Override
            public boolean inConflict(Context cx, SctAssignment sa1, SctAssignment sa2) {
                Section s1 = (Section) sa1;
                Section s2 = (Section) sa2;
                if (s1.getPlacement() == null || s2.getPlacement() == null)
                    return false;
                TimeLocation t1 = s1.getTime();
                TimeLocation t2 = s2.getTime();
                if (!t1.shareDays(t2) || !t1.shareWeeks(t2))
                    return false;
                int a1 = t1.getStartSlot(), a2 = t2.getStartSlot();
                if (cx.getDistanceMetric().doComputeDistanceConflictsBetweenNonBTBClasses()) {
                    if (a1 + t1.getNrSlotsPerMeeting() <= a2) {
                        int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                        if (dist > Constants.SLOT_LENGTH_MIN * (a2 - a1 - t1.getLength()))
                            return true;
                    } else if (a2 + t2.getNrSlotsPerMeeting() <= a1) {
                        int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                        if (dist > Constants.SLOT_LENGTH_MIN * (a1 - a2 - t2.getLength()))
                            return true;
                    }
                } else {
                    if (a1 + t1.getNrSlotsPerMeeting() == a2) {
                        int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                        if (dist > 0) return true;
                    } else if (a2 + t2.getNrSlotsPerMeeting() == a1) {
                        int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                        if (dist > 0) return true;
                    }
                }
                return false;
            }

            @Override
            public int penalty(Context cx, Student s, SctAssignment a1, SctAssignment a2) {
                return inConflict(cx, a1, a2) ? 1 : 0;
            }

            @Override
            public Iterable<? extends SctAssignment> other(Context cx, Enrollment e) {
                return new Nothing();
            }
            
            @Override
            public double getWeight(Context cx, Conflict c, Enrollment e) {
                return c.getPenalty();
            }
        }),
        /**
         * Naive, yet effective approach for modeling student lunch breaks. It creates a conflict whenever there are
         * two classes (of a student) overlapping with the lunch time which are one after the other with a break in
         * between smaller than the requested lunch break. Lunch time is defined by StudentLunch.StartSlot and
         * StudentLunch.EndStart properties (default is 11:00 am - 1:30 pm), with lunch break of at least
         * StudentLunch.Length slots (default is 30 minutes). Such a conflict is weighted
         * by StudentWeights.LunchBreakFactor, which defaults to 0.005.
         */
        LunchBreak(WeightType.BOTH, "StudentWeights.LunchBreakFactor", 0.0050, new Quality() {
            @Override
            public boolean isApplicable(Context cx, Student student, Request r1, Request r2) {
                return r1 instanceof CourseRequest && r2 instanceof CourseRequest && !student.isDummy();
            }

            @Override
            public boolean inConflict(Context cx, SctAssignment a1, SctAssignment a2) {
                if (a1.getTime() == null || a2.getTime() == null) return false;
                if (((Section)a1).isToIgnoreStudentConflictsWith(a2.getId())) return false;
                if (a1.getTime().hasIntersection(a2.getTime())) return false;
                TimeLocation t1 = a1.getTime(), t2 = a2.getTime();
                if (!t1.shareDays(t2) || !t1.shareWeeks(t2)) return false;
                int s1 = t1.getStartSlot(), s2 = t2.getStartSlot();
                int e1 = t1.getStartSlot() + t1.getNrSlotsPerMeeting(), e2 = t2.getStartSlot() + t2.getNrSlotsPerMeeting();
                if (e1 + cx.getLunchLength() > s2 && e2 + cx.getLunchLength() > s1 && e1 > cx.getLunchStart() && cx.getLunchEnd() > s1 && e2 > cx.getLunchStart() && cx.getLunchEnd() > s2)
                    return true;
                return false;
            }

            @Override
            public int penalty(Context cx, Student s, SctAssignment a1, SctAssignment a2) {
                if (!inConflict(cx, a1, a2)) return 0;
                return a1.getTime().nrSharedDays(a2.getTime());
            }
            
            @Override
            public Iterable<? extends SctAssignment> other(Context cx, Enrollment e) {
                return new Nothing();
            }

            @Override
            public double getWeight(Context cx, Conflict c, Enrollment e) {
                return c.getPenalty();
            }
        }),
        /**
         * Naive, yet effective approach for modeling travel times. A conflict with the penalty
         * equal to the distance in minutes occurs when two classes are less than TravelTime.MaxTravelGap
         * time slots a part (defaults 1 hour), or when they are less then twice as long apart 
         * and the travel time is longer than the break time of the first class.
         * Such a conflict is weighted by StudentWeights.TravelTimeFactor, which defaults to 0.001.
         */
        TravelTime(WeightType.BOTH, "StudentWeights.TravelTimeFactor", 0.0010, new Quality() {
            @Override
            public boolean isApplicable(Context cx, Student student, Request r1, Request r2) {
                return r1 instanceof CourseRequest && r2 instanceof CourseRequest && !student.isDummy();
            }

            @Override
            public boolean inConflict(Context cx, SctAssignment sa1, SctAssignment sa2) {
                Section s1 = (Section) sa1;
                Section s2 = (Section) sa2;
                if (s1.getPlacement() == null || s2.getPlacement() == null)
                    return false;
                TimeLocation t1 = s1.getTime();
                TimeLocation t2 = s2.getTime();
                if (!t1.shareDays(t2) || !t1.shareWeeks(t2))
                    return false;
                int a1 = t1.getStartSlot(), a2 = t2.getStartSlot();
                if (a1 + t1.getNrSlotsPerMeeting() <= a2) {
                    int gap = a2 - (a1 + t1.getNrSlotsPerMeeting());
                    int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                    return (gap < cx.getMaxTravelGap() && dist > 0) || (gap < 2 * cx.getMaxTravelGap() && dist > t1.getBreakTime());
                } else if (a2 + t2.getNrSlotsPerMeeting() <= a1) {
                    int gap = a1 - (a2 + t2.getNrSlotsPerMeeting());
                    int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                    return (gap < cx.getMaxTravelGap() && dist > 0) || (gap < 2 * cx.getMaxTravelGap() && dist > t2.getBreakTime());
                }
                return false;
            }

            @Override
            public int penalty(Context cx, Student s, SctAssignment sa1, SctAssignment sa2) {
                Section s1 = (Section) sa1;
                Section s2 = (Section) sa2;
                if (s1.getPlacement() == null || s2.getPlacement() == null) return 0;
                TimeLocation t1 = s1.getTime();
                TimeLocation t2 = s2.getTime();
                if (!t1.shareDays(t2) || !t1.shareWeeks(t2)) return 0;
                int a1 = t1.getStartSlot(), a2 = t2.getStartSlot();
                if (a1 + t1.getNrSlotsPerMeeting() <= a2) {
                    int gap = a2 - (a1 + t1.getNrSlotsPerMeeting());
                    int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                    if ((gap < cx.getMaxTravelGap() && dist > 0) || (gap < 2 * cx.getMaxTravelGap() && dist > t1.getBreakTime()))
                        return dist;
                } else if (a2 + t2.getNrSlotsPerMeeting() <= a1) {
                    int gap = a1 - (a2 + t2.getNrSlotsPerMeeting());
                    int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                    if ((gap < cx.getMaxTravelGap() && dist > 0) || (gap < 2 * cx.getMaxTravelGap() && dist > t2.getBreakTime()))
                        return dist;
                }
                return 0;
            }
            
            @Override
            public Iterable<? extends SctAssignment> other(Context cx, Enrollment e) {
                return new Nothing();
            }

            @Override
            public double getWeight(Context cx, Conflict c, Enrollment e) {
                return c.getPenalty();
            }
        }),
        /**
         * A back-to-back conflict is there every time when a student has two classes that are
         * back-to-back or less than StudentWeights.BackToBackDistance time slots apart (defaults to 30 minutes).
         * Such a conflict is weighted by StudentWeights.BackToBackFactor, which
         * defaults to -0.0001 (these conflicts are preferred by default, trying to avoid schedule gaps).
         * NEW: Consider student's back-to-back preference. That is, students with no preference are ignored, and
         * students that discourage back-to-backs have a negative weight on the conflict.
         */
        BackToBack(WeightType.BOTH, "StudentWeights.BackToBackFactor", -0.0001, new Quality() {
            @Override
            public boolean isApplicable(Context cx, Student student, Request r1, Request r2) {
                return r1 instanceof CourseRequest && r2 instanceof CourseRequest && !student.isDummy() && 
                        (student.getBackToBackPreference() == BackToBackPreference.BTB_PREFERRED || student.getBackToBackPreference() == BackToBackPreference.BTB_DISCOURAGED);
            }

            @Override
            public boolean inConflict(Context cx, SctAssignment a1, SctAssignment a2) {
                TimeLocation t1 = a1.getTime();
                TimeLocation t2 = a2.getTime();
                if (t1 == null || t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) return false;
                if (t1.getStartSlot() + t1.getNrSlotsPerMeeting() <= t2.getStartSlot()) {
                    int dist = t2.getStartSlot() - (t1.getStartSlot() + t1.getNrSlotsPerMeeting());
                    return dist <= cx.getBackToBackDistance();
                } else if (t2.getStartSlot() + t2.getNrSlotsPerMeeting() <= t1.getStartSlot()) {
                    int dist = t1.getStartSlot() - (t2.getStartSlot() + t2.getNrSlotsPerMeeting());
                    return dist <= cx.getBackToBackDistance();
                }
                return false;
            }

            @Override
            public int penalty(Context cx, Student s, SctAssignment a1, SctAssignment a2) {
                if (!inConflict(cx, a1, a2)) return 0;
                if (s.getBackToBackPreference() == BackToBackPreference.BTB_PREFERRED)
                    return a1.getTime().nrSharedDays(a2.getTime());
                else if (s.getBackToBackPreference() == BackToBackPreference.BTB_DISCOURAGED)
                    return -a1.getTime().nrSharedDays(a2.getTime());
                else
                    return 0;
            }
            
            @Override
            public Iterable<? extends SctAssignment> other(Context cx, Enrollment e) {
                return new Nothing();
            }

            @Override
            public double getWeight(Context cx, Conflict c, Enrollment e) {
                return c.getPenalty();
            }
        }),
        /**
         * A work-day conflict is there every time when a student has two classes that are too
         * far apart. This means that the time between the start of the first class and the end
         * of the last class is more than WorkDay.WorkDayLimit (defaults to 6 hours). A penalty
         * of one is incurred for every hour started over this limit.
         * Such a conflict is weighted by StudentWeights.WorkDayFactor, which defaults to 0.01.
         */
        WorkDay(WeightType.BOTH, "StudentWeights.WorkDayFactor", 0.0100, new Quality() {
            @Override
            public boolean isApplicable(Context cx, Student student, Request r1, Request r2) {
                return r1 instanceof CourseRequest && r2 instanceof CourseRequest && !student.isDummy();
            }
            
            @Override
            public boolean inConflict(Context cx, SctAssignment a1, SctAssignment a2) {
                TimeLocation t1 = a1.getTime();
                TimeLocation t2 = a2.getTime();
                if (t1 == null || t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) return false;
                int dist = Math.max(t1.getStartSlot() + t1.getLength(), t2.getStartSlot() + t2.getLength()) - Math.min(t1.getStartSlot(), t2.getStartSlot());
                return dist > cx.getWorkDayLimit();
            }

            @Override
            public int penalty(Context cx, Student s, SctAssignment a1, SctAssignment a2) {
                TimeLocation t1 = a1.getTime();
                TimeLocation t2 = a2.getTime();
                if (t1 == null || t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) return 0;
                int dist = Math.max(t1.getStartSlot() + t1.getLength(), t2.getStartSlot() + t2.getLength()) - Math.min(t1.getStartSlot(), t2.getStartSlot());
                if (dist > cx.getWorkDayLimit())
                    return a1.getTime().nrSharedDays(a2.getTime()) * (dist - cx.getWorkDayLimit());
                else
                    return 0;
            }
            
            @Override
            public Iterable<? extends SctAssignment> other(Context cx, Enrollment e) {
                return new Nothing();
            }

            @Override
            public double getWeight(Context cx, Conflict c, Enrollment e) {
                return c.getPenalty() / 12.0;
            }
        }),
        TooEarly(WeightType.REQUEST, "StudentWeights.TooEarlyFactor", 0.0500, new Quality(){
            @Override
            public boolean isApplicable(Context cx, Student student, Request r1, Request r2) {
                return false;
            }

            @Override
            public boolean inConflict(Context cx, SctAssignment a1, SctAssignment a2) {
                if (a1.getTime() == null || a2.getTime() == null) return false;
                return a1.getTime().shareDays(a2.getTime()) && a1.getTime().shareHours(a2.getTime());
            }

            @Override
            public int penalty(Context cx, Student s, SctAssignment a1, SctAssignment a2) {
                if (!inConflict(cx, a1, a2)) return 0;
                return a1.getTime().nrSharedDays(a2.getTime()) * a1.getTime().nrSharedHours(a2.getTime());
            }
            
            @Override
            public Iterable<? extends SctAssignment> other(Context cx, Enrollment e) {
                return (e.isCourseRequest() && !e.getStudent().isDummy() ? new SingleTimeIterable(0, cx.getEarlySlot()) : new Nothing());
            }
            
            @Override
            public double getWeight(Context cx, Conflict c, Enrollment e) {
                return Math.min(cx.getTimeOverlapMaxLimit() * c.getPenalty() / c.getE1().getNrSlots(), cx.getTimeOverlapMaxLimit());
            }
        }),
        TooLate(WeightType.REQUEST, "StudentWeights.TooLateFactor", 0.0250, new Quality(){
            @Override
            public boolean isApplicable(Context cx, Student student, Request r1, Request r2) {
                return false;
            }

            @Override
            public boolean inConflict(Context cx, SctAssignment a1, SctAssignment a2) {
                if (a1.getTime() == null || a2.getTime() == null) return false;
                return a1.getTime().shareDays(a2.getTime()) && a1.getTime().shareHours(a2.getTime());
            }

            @Override
            public int penalty(Context cx, Student s, SctAssignment a1, SctAssignment a2) {
                if (!inConflict(cx, a1, a2)) return 0;
                return a1.getTime().nrSharedDays(a2.getTime()) * a1.getTime().nrSharedHours(a2.getTime());
            }
            
            @Override
            public Iterable<? extends SctAssignment> other(Context cx, Enrollment e) {
                return (e.isCourseRequest() && !e.getStudent().isDummy() ? new SingleTimeIterable(cx.getLateSlot(), 288) : new Nothing());
            }
            
            @Override
            public double getWeight(Context cx, Conflict c, Enrollment e) {
                return Math.min(cx.getTimeOverlapMaxLimit() * c.getPenalty() / c.getE1().getNrSlots(), cx.getTimeOverlapMaxLimit());
            }
        }),
        /**
         * There is a student modality preference conflict when a student that prefers online
         * gets a non-online class ({@link Section#isOnline()} is false) or when a student that
         * prefers non-online gets an online class (@{link Section#isOnline()} is true).
         * Such a conflict is weighted by StudentWeights.ModalityFactor, which defaults to 0.05.
         */
        Modality(WeightType.REQUEST, "StudentWeights.ModalityFactor", 0.0500, new Quality(){
            @Override
            public boolean isApplicable(Context cx, Student student, Request r1, Request r2) {
                return false;
            }

            @Override
            public boolean inConflict(Context cx, SctAssignment a1, SctAssignment a2) {
                return a1.equals(a2);
            }

            @Override
            public int penalty(Context cx, Student s, SctAssignment a1, SctAssignment a2) {
                return (inConflict(cx, a1, a2) ? 1 : 0);
            }
            
            @Override
            public Iterable<? extends SctAssignment> other(Context cx, Enrollment e) {
                if (!e.isCourseRequest() || e.getStudent().isDummy()) return new Nothing();
                if (e.getStudent().getModalityPreference() == ModalityPreference.ONLINE_PREFERRED)
                    return new Online(e, false); // face-to-face sections are conflicting
                else if (e.getStudent().getModalityPreference() == ModalityPreference.ONILNE_DISCOURAGED)
                    return new Online(e, true); // online sections are conflicting
                return new Nothing();
            }
            
            @Override
            public double getWeight(Context cx, Conflict c, Enrollment e) {
                return ((double) c.getPenalty()) / ((double) e.getSections().size());
            }
        }),
        /** 
         * DRC: Time conflict between class and a free time request (for students with FT accommodation).
         * Free time conflicts are penalized as the time of a course request overlapping with a free time
         * proportional to the time of the request, capped at one half of the time.
         * This criterion is weighted by Accommodations.FreeTimeOverlapFactor, defaulting to 0.5.
         */
        AccFreeTimeOverlap(WeightType.REQUEST, "Accommodations.FreeTimeOverlapFactor", 0.5000, new Quality(){
            @Override
            public boolean isApplicable(Context cx, Student student, Request r1, Request r2) {
                return false;
            }

            @Override
            public boolean inConflict(Context cx, SctAssignment a1, SctAssignment a2) {
                if (a1.getTime() == null || a2.getTime() == null) return false;
                return a1.getTime().hasIntersection(a2.getTime());
            }

            @Override
            public int penalty(Context cx, Student s, SctAssignment a1, SctAssignment a2) {
                if (!inConflict(cx, a1, a2)) return 0;
                return a1.getTime().nrSharedDays(a2.getTime()) * a1.getTime().nrSharedHours(a2.getTime());
            }
            
            @Override
            public Iterable<? extends SctAssignment> other(Context cx, Enrollment e) {
                if (!e.getStudent().hasAccommodation(cx.getFreeTimeAccommodation())) return new Nothing();
                return (e.isCourseRequest() ? new FreeTimes(e.getStudent()) : new Nothing());
            }
            
            @Override
            public double getWeight(Context cx, Conflict c, Enrollment e) {
                return Math.min(cx.getTimeOverlapMaxLimit() * c.getPenalty() / c.getE1().getNrSlots(), cx.getTimeOverlapMaxLimit());
            }
        }),
        /**
         * DRC: A back-to-back conflict (for students with BTB accommodation) is there every time when a student has two classes that are NOT
         * back-to-back or less than Accommodations.BackToBackDistance time slots apart (defaults to 30 minutes).
         * Such a conflict is weighted by Accommodations.BackToBackFactor, which defaults to 0.001
         */
        AccBackToBack(WeightType.BOTH, "Accommodations.BackToBackFactor", 0.001, new Quality() {
            @Override
            public boolean isApplicable(Context cx, Student student, Request r1, Request r2) {
                return r1 instanceof CourseRequest && r2 instanceof CourseRequest && !student.isDummy() && student.hasAccommodation(cx.getBackToBackAccommodation());
            }

            @Override
            public boolean inConflict(Context cx, SctAssignment a1, SctAssignment a2) {
                TimeLocation t1 = a1.getTime();
                TimeLocation t2 = a2.getTime();
                if (t1 == null || t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) return false;
                if (t1.getStartSlot() + t1.getNrSlotsPerMeeting() <= t2.getStartSlot()) {
                    int dist = t2.getStartSlot() - (t1.getStartSlot() + t1.getNrSlotsPerMeeting());
                    return dist > cx.getBackToBackDistance();
                } else if (t2.getStartSlot() + t2.getNrSlotsPerMeeting() <= t1.getStartSlot()) {
                    int dist = t1.getStartSlot() - (t2.getStartSlot() + t2.getNrSlotsPerMeeting());
                    return dist > cx.getBackToBackDistance();
                }
                return false;
            }

            @Override
            public int penalty(Context cx, Student s, SctAssignment a1, SctAssignment a2) {
                if (!inConflict(cx, a1, a2)) return 0;
                return a1.getTime().nrSharedDays(a2.getTime());
            }
            
            @Override
            public Iterable<? extends SctAssignment> other(Context cx, Enrollment e) {
                return new Nothing();
            }

            @Override
            public double getWeight(Context cx, Conflict c, Enrollment e) {
                return c.getPenalty();
            }
        }),
        /**
         * DRC: A not back-to-back conflict (for students with BBC accommodation) is there every time when a student has two classes that are
         * back-to-back or less than Accommodations.BackToBackDistance time slots apart (defaults to 30 minutes).
         * Such a conflict is weighted by Accommodations.BreaksBetweenClassesFactor, which defaults to 0.001.
         */
        AccBreaksBetweenClasses(WeightType.BOTH, "Accommodations.BreaksBetweenClassesFactor", 0.001, new Quality() {
            @Override
            public boolean isApplicable(Context cx, Student student, Request r1, Request r2) {
                return r1 instanceof CourseRequest && r2 instanceof CourseRequest && !student.isDummy() && student.hasAccommodation(cx.getBreakBetweenClassesAccommodation());
            }

            @Override
            public boolean inConflict(Context cx, SctAssignment a1, SctAssignment a2) {
                TimeLocation t1 = a1.getTime();
                TimeLocation t2 = a2.getTime();
                if (t1 == null || t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) return false;
                if (t1.getStartSlot() + t1.getNrSlotsPerMeeting() <= t2.getStartSlot()) {
                    int dist = t2.getStartSlot() - (t1.getStartSlot() + t1.getNrSlotsPerMeeting());
                    return dist <= cx.getBackToBackDistance();
                } else if (t2.getStartSlot() + t2.getNrSlotsPerMeeting() <= t1.getStartSlot()) {
                    int dist = t1.getStartSlot() - (t2.getStartSlot() + t2.getNrSlotsPerMeeting());
                    return dist <= cx.getBackToBackDistance();
                }
                return false;
            }

            @Override
            public int penalty(Context cx, Student s, SctAssignment a1, SctAssignment a2) {
                if (!inConflict(cx, a1, a2)) return 0;
                return a1.getTime().nrSharedDays(a2.getTime());
            }
            
            @Override
            public Iterable<? extends SctAssignment> other(Context cx, Enrollment e) {
                return new Nothing();
            }

            @Override
            public double getWeight(Context cx, Conflict c, Enrollment e) {
                return c.getPenalty();
            }
        }),
        /** 
         * Student unavailability distance conflict. Distance conflict between a class that the student is taking and a class that the student
         * is teaching or attending in a different session.
         * This criterion is weighted by StudentWeights.UnavailabilityDistanceConflict, defaulting to 0.1.
         */
        UnavailabilityDistance(WeightType.REQUEST, "StudentWeights.UnavailabilityDistanceConflict", 0.100, new Quality(){
            @Override
            public boolean isApplicable(Context cx, Student student, Request r1, Request r2) {
                return false;
            }
            
            @Override
            public boolean inConflict(Context cx, SctAssignment sa1, SctAssignment sa2) {
                Section s1 = (Section) sa1;
                Unavailability s2 = (Unavailability) sa2;
                if (s1.getPlacement() == null || s2.getTime() == null || s2.getNrRooms() == 0)
                    return false;
                TimeLocation t1 = s1.getTime();
                TimeLocation t2 = s2.getTime();
                if (!t1.shareDays(t2) || !t1.shareWeeks(t2))
                    return false;
                int a1 = t1.getStartSlot(), a2 = t2.getStartSlot();
                if (a1 + t1.getNrSlotsPerMeeting() <= a2) {
                    int dist = cx.getUnavailabilityDistanceInMinutes(s1.getPlacement(), s2);
                    if (dist > t1.getBreakTime() + Constants.SLOT_LENGTH_MIN * (a2 - a1 - t1.getLength()))
                        return true;
                } else if (a2 + t2.getNrSlotsPerMeeting() <= a1) {
                    int dist = cx.getUnavailabilityDistanceInMinutes(s1.getPlacement(), s2);
                    if (dist > t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (a1 - a2 - t2.getLength()))
                        return true;
                }
                return false;
            }

            @Override
            public int penalty(Context cx, Student s, SctAssignment a1, SctAssignment a2) {
                if (!inConflict(cx, a1, a2)) return 0;
                return a1.getTime().nrSharedDays(a2.getTime());
            }

            @Override
            public Iterable<? extends SctAssignment> other(Context cx, Enrollment e) {
                return (e.isCourseRequest() ? new Unavailabilities(e.getStudent()) : new Nothing());
            }
            
            @Override
            public double getWeight(Context cx, Conflict c, Enrollment e) {
                return c.getPenalty();
            }
        }),
        ;
        
        private WeightType iType;
        private Quality iQuality;
        private String iWeightName;
        private double iWeightDefault;
        Type(WeightType type, String weightName, double weightDefault, Quality quality) {
            iQuality = quality;
            iType = type;
            iWeightName = weightName;
            iWeightDefault = weightDefault;
        }
        
        
        public boolean isApplicable(Context cx, Student student, Request r1, Request r2) { return iQuality.isApplicable(cx, student, r1, r2); }
        public boolean inConflict(Context cx, SctAssignment a1, SctAssignment a2) { return iQuality.inConflict(cx, a1, a2); }
        public int penalty(Context cx, Student s, SctAssignment a1, SctAssignment a2) { return iQuality.penalty(cx, s, a1, a2); }
        public Iterable<? extends SctAssignment> other(Context cx, Enrollment e) { return iQuality.other(cx, e); }
        public double getWeight(Context cx, Conflict c, Enrollment e) { return iQuality.getWeight(cx, c, e); }
        public String getName() { return name().replaceAll("(?<=[^A-Z0-9])([A-Z0-9])"," $1"); }
        public String getAbbv() { return getName().replaceAll("[a-z ]",""); }
        public WeightType getType() { return iType; }
        public String getWeightName() { return iWeightName; }
        public double getWeightDefault() { return iWeightDefault; }
    }
    
    /**
     * Schedule quality interface
     */
    public static interface Quality {
        /**
         * Check if the metric is applicable for the given student, between the given two requests
         */
        public boolean isApplicable(Context cx, Student student, Request r1, Request r2);
        /**
         * When applicable, is there a conflict between two sections
         */
        public boolean inConflict(Context cx, SctAssignment a1, SctAssignment a2);
        /**
         * When in conflict, what is the penalisation
         */
        public int penalty(Context cx, Student s, SctAssignment a1, SctAssignment a2);
        /**
         * Enumerate other section assignments applicable for the given enrollment (e.g., student unavailabilities)
         */
        public Iterable<? extends SctAssignment> other(Context cx, Enrollment e);
        /**
         * Base weight of the given conflict and enrollment. Typically based on the {@link Conflict#getPenalty()}, but 
         * change to be between 0.0 and 1.0. For example, for time conflicts, a percentage of share is used. 
         */
        public double getWeight(Context cx, Conflict c, Enrollment e);
    }
    
    /**
     * Penalisation of the given type between two enrollments of a student.
     */
    public int penalty(Type type, Enrollment e1, Enrollment e2) {
        if (!e1.getStudent().equals(e2.getStudent()) || !type.isApplicable(iContext, e1.getStudent(), e1.getRequest(), e2.getRequest())) return 0;
        int cnt = 0;
        for (SctAssignment s1 : e1.getAssignments()) {
            for (SctAssignment s2 : e2.getAssignments()) {
                cnt += type.penalty(iContext, e1.getStudent(), s1, s2);
            }
        }
        return cnt;
    }
    
    /**
     * Conflicss of the given type between two enrollments of a student.
     */
    public Set<Conflict> conflicts(Type type, Enrollment e1, Enrollment e2) {
        Set<Conflict> ret = new HashSet<Conflict>();
        if (!e1.getStudent().equals(e2.getStudent()) || !type.isApplicable(iContext, e1.getStudent(), e1.getRequest(), e2.getRequest())) return ret;
        for (SctAssignment s1 : e1.getAssignments()) {
            for (SctAssignment s2 : e2.getAssignments()) {
                int penalty = type.penalty(iContext, e1.getStudent(), s1, s2);
                if (penalty != 0)
                    ret.add(new Conflict(e1.getStudent(), type, penalty, e1, s1, e2, s2));
            }
        }
        return ret;
    }
    
    /**
     * Conflicts of any type between two enrollments of a student.
     */
    public Set<Conflict> conflicts(Enrollment e1, Enrollment e2) {
        Set<Conflict> ret = new HashSet<Conflict>();
        for (Type type: iContext.getTypes()) {
            if (!e1.getStudent().equals(e2.getStudent()) || !type.isApplicable(iContext, e1.getStudent(), e1.getRequest(), e2.getRequest())) continue;
            for (SctAssignment s1 : e1.getAssignments()) {
                for (SctAssignment s2 : e2.getAssignments()) {
                    int penalty = type.penalty(iContext, e1.getStudent(), s1, s2);
                    if (penalty != 0)
                        ret.add(new Conflict(e1.getStudent(), type, penalty, e1, s1, e2, s2));
                }
            }
        }
        return ret;
    }
    
    /**
     * Conflicts of the given type between classes of a single enrollment (or with free times, unavailabilities, etc.)
     */
    public Set<Conflict> conflicts(Type type, Enrollment e1) {
        Set<Conflict> ret = new HashSet<Conflict>();
        boolean applicable = type.isApplicable(iContext, e1.getStudent(), e1.getRequest(), e1.getRequest()); 
        for (SctAssignment s1 : e1.getAssignments()) {
            if (applicable) {
                for (SctAssignment s2 : e1.getAssignments()) {
                    if (s1.getId() < s2.getId()) {
                        int penalty = type.penalty(iContext, e1.getStudent(), s1, s2);
                        if (penalty != 0)
                            ret.add(new Conflict(e1.getStudent(), type, penalty, e1, s1, e1, s2));
                    }
                }
            }
            for (SctAssignment s2: type.other(iContext, e1)) {
                int penalty = type.penalty(iContext, e1.getStudent(), s1, s2);
                if (penalty != 0)
                    ret.add(new Conflict(e1.getStudent(), type, penalty, e1, s1, s2));
            }
        }
        return ret;
    }
    
    /**
     * Conflicts of any type between classes of a single enrollment (or with free times, unavailabilities, etc.)
     */
    public Set<Conflict> conflicts(Enrollment e1) {
        Set<Conflict> ret = new HashSet<Conflict>();
        for (Type type: iContext.getTypes()) {
            boolean applicable = type.isApplicable(iContext, e1.getStudent(), e1.getRequest(), e1.getRequest()); 
            for (SctAssignment s1 : e1.getAssignments()) {
                if (applicable) {
                    for (SctAssignment s2 : e1.getAssignments()) {
                        if (s1.getId() < s2.getId()) {
                            int penalty = type.penalty(iContext, e1.getStudent(), s1, s2);
                            if (penalty != 0)
                                ret.add(new Conflict(e1.getStudent(), type, penalty, e1, s1, e1, s2));
                        }
                    }
                }
                for (SctAssignment s2: type.other(iContext, e1)) {
                    int penalty = type.penalty(iContext, e1.getStudent(), s1, s2);
                    if (penalty != 0)
                        ret.add(new Conflict(e1.getStudent(), type, penalty, e1, s1, s2));
                }
            }            
        }
        return ret;
    }
    
    /**
     * Penalty of given type between classes of a single enrollment (or with free times, unavailabilities, etc.)
     */
    public int penalty(Type type, Enrollment e1) {
        int penalty = 0;
        boolean applicable = type.isApplicable(iContext, e1.getStudent(), e1.getRequest(), e1.getRequest());
        for (SctAssignment s1 : e1.getAssignments()) {
            if (applicable) {
                for (SctAssignment s2 : e1.getAssignments()) {
                    if (s1.getId() < s2.getId()) {
                        penalty += type.penalty(iContext, e1.getStudent(), s1, s2);
                    }
                }
            }
            for (SctAssignment s2: type.other(iContext, e1)) {
                penalty += type.penalty(iContext, e1.getStudent(), s1, s2);
            }
        }
        return penalty;
    }
    
    /**
     * Check whether the given type is applicable for the student and the two requests.
     */
    public boolean isApplicable(Type type, Student student, Request r1, Request r2) {
        return type.isApplicable(iContext, student, r1, r2);
    }
  
    /**
     * Total penalisation of given type
     */
    public int getTotalPenalty(Type type, Assignment<Request, Enrollment> assignment) {
        return getContext(assignment).getTotalPenalty(type);
    }
    
    /**
     * Total penalisation of given types
     */
    public int getTotalPenalty(Assignment<Request, Enrollment> assignment, Type... types) {
        int ret = 0;
        for (Type type: types)
            ret += getContext(assignment).getTotalPenalty(type);
        return ret;
    }
    
    /**
     * Re-check total penalization for the given assignment 
     */
    public void checkTotalPenalty(Assignment<Request, Enrollment> assignment) {
        for (Type type: iContext.getTypes())
            checkTotalPenalty(type, assignment);
    }
    
    /**
     * Re-check total penalization for the given assignment and conflict type 
     */
    public void checkTotalPenalty(Type type, Assignment<Request, Enrollment> assignment) {
        getContext(assignment).checkTotalPenalty(type, assignment);
    }

    /**
     * All conflicts of the given type for the given assignment 
     */
    public Set<Conflict> getAllConflicts(Type type, Assignment<Request, Enrollment> assignment) {
        return getContext(assignment).getAllConflicts(type);
    }
    
    /**
     * All conflicts of the any type for the enrollment (including conflicts with other enrollments of the student)
     */
    public Set<Conflict> allConflicts(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        Set<Conflict> conflicts = new HashSet<Conflict>();
        for (Type t: iContext.getTypes()) {
            conflicts.addAll(conflicts(t, enrollment));
            for (Request request : enrollment.getStudent().getRequests()) {
                if (request.equals(enrollment.getRequest()) || assignment.getValue(request) == null) continue;
                conflicts.addAll(conflicts(t, enrollment, assignment.getValue(request)));
            }
        }
        return conflicts;
    }
    
    @Override
    public void beforeAssigned(Assignment<Request, Enrollment> assignment, long iteration, Enrollment value) {
        getContext(assignment).beforeAssigned(assignment, iteration, value);
    }

    @Override
    public void afterAssigned(Assignment<Request, Enrollment> assignment, long iteration, Enrollment value) {
        getContext(assignment).afterAssigned(assignment, iteration, value);
    }

    @Override
    public void afterUnassigned(Assignment<Request, Enrollment> assignment, long iteration, Enrollment value) {
        getContext(assignment).afterUnassigned(assignment, iteration, value);
    }
    
    /** A representation of a time overlapping conflict */
    public class Conflict {
        private Type iType;
        private int iPenalty;
        private Student iStudent;
        private SctAssignment iA1, iA2;
        private Enrollment iE1, iE2;
        private int iHashCode;

        /**
         * Constructor
         * 
         * @param student related student
         * @param type conflict type
         * @param penalty conflict penalization, e.g., the number of slots in common between the two conflicting sections
         * @param e1 first enrollment
         * @param a1 first conflicting section
         * @param e2 second enrollment
         * @param a2 second conflicting section
         */
        public Conflict(Student student, Type type, int penalty, Enrollment e1, SctAssignment a1, Enrollment e2, SctAssignment a2) {
            iStudent = student;
            if (a1.compareById(a2) < 0 ) {
                iA1 = a1;
                iA2 = a2;
                iE1 = e1;
                iE2 = e2;
            } else {
                iA1 = a2;
                iA2 = a1;
                iE1 = e2;
                iE2 = e1;
            }
            iHashCode = (iStudent.getId() + ":" + iA1.getId() + ":" + iA2.getId()).hashCode();
            iType = type;
            iPenalty = penalty;
        }
        
        public Conflict(Student student, Type type, int penalty, Enrollment e1, SctAssignment a1, SctAssignment a2) {
            this(student, type, penalty, e1, a1, a2 instanceof FreeTimeRequest ? ((FreeTimeRequest)a2).createEnrollment() : a2 instanceof Unavailability ? ((Unavailability)a2).createEnrollment() : e1, a2);
            
        }

        /** Related student
         * @return student
         **/
        public Student getStudent() {
            return iStudent;
        }

        /** First section
         * @return first section
         **/
        public SctAssignment getS1() {
            return iA1;
        }

        /** Second section
         * @return second section
         **/
        public SctAssignment getS2() {
            return iA2;
        }

        /** First request
         * @return first request
         **/
        public Request getR1() {
            return iE1.getRequest();
        }
        
        /** First request weight
         * @return first request weight
         **/
        public double getR1Weight() {
            return (iE1.getRequest() == null ? 0.0 : iE1.getRequest().getWeight());
        }
        
        /** Second request weight
         * @return second request weight
         **/
        public double getR2Weight() {
            return (iE2.getRequest() == null ? 0.0 : iE2.getRequest().getWeight());
        }
        
        /** Second request
         * @return second request
         **/
        public Request getR2() {
            return iE2.getRequest();
        }
        
        /** First enrollment
         * @return first enrollment
         **/
        public Enrollment getE1() {
            return iE1;
        }

        /** Second enrollment
         * @return second enrollment
         **/
        public Enrollment getE2() {
            return iE2;
        }
        
        @Override
        public int hashCode() {
            return iHashCode;
        }

        /** Conflict penalty, e.g., the number of overlapping slots against the number of slots of the smallest section
         * @return conflict penalty 
         **/
        public int getPenalty() {
            return iPenalty;
        }
        
        /** Other enrollment of the conflict */
        public Enrollment getOther(Enrollment enrollment) {
            return (getE1().getRequest().equals(enrollment.getRequest()) ? getE2() : getE1());
        }
        
        /** Weight of the conflict on the given enrollment */
        public double getWeight(Enrollment e) {
            return iType.getWeight(iContext, this, e);
        }
        
        /** Weight of the conflict on both enrollment (sum) */
        public double getWeight() {
            return (iType.getWeight(iContext, this, iE1) + iType.getWeight(iContext, this, iE2)) / 2.0;
        }
        
        /** Conflict type
         * @return conflict type;
         */
        public Type getType() {
            return iType;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Conflict)) return false;
            Conflict c = (Conflict) o;
            return getType() == c.getType() && getStudent().equals(c.getStudent()) && getS1().equals(c.getS1()) && getS2().equals(c.getS2());
        }

        @Override
        public String toString() {
            return getStudent() + ": (" + getType() + ", p:" + getPenalty() + ") " + getS1() + " -- " + getS2();
        }
    }
    
    /**
     * Context holding parameters and distance cache. See {@link Type} for the list of available parameters.
     */
    public static class Context {
        private List<Type> iTypes = null;
        private DistanceMetric iDistanceMetric = null;
        private boolean iDebug = false;
        protected double iTimeOverlapMaxLimit = 0.5000;
        private int iLunchStart, iLunchEnd, iLunchLength, iMaxTravelGap, iWorkDayLimit, iBackToBackDistance, iEarlySlot, iLateSlot, iAccBackToBackDistance;
        private String iFreeTimeAccommodation = "FT", iBackToBackAccommodation = "BTB", iBreakBetweenClassesAccommodation = "BBC";
        private ReentrantReadWriteLock iLock = new ReentrantReadWriteLock();
        private Integer iUnavailabilityMaxTravelTime = null; 
        private DistanceMetric iUnavailabilityDistanceMetric = null;
        
        public Context(DistanceMetric dm, DataProperties config) {
            iDistanceMetric = (dm == null ? new DistanceMetric(config) : dm);
            iDebug = config.getPropertyBoolean("StudentQuality.Debug", false);
            iTimeOverlapMaxLimit = config.getPropertyDouble("StudentWeights.TimeOverlapMaxLimit", iTimeOverlapMaxLimit);
            iLunchStart = config.getPropertyInt("StudentLunch.StartSlot", (11 * 60) / 5);
            iLunchEnd = config.getPropertyInt("StudentLunch.EndStart", (13 * 60) / 5);
            iLunchLength = config.getPropertyInt("StudentLunch.Length", 30 / 5);
            iMaxTravelGap = config.getPropertyInt("TravelTime.MaxTravelGap", 12);
            iWorkDayLimit = config.getPropertyInt("WorkDay.WorkDayLimit", 6 * 12);
            iBackToBackDistance = config.getPropertyInt("StudentWeights.BackToBackDistance", 6);
            iAccBackToBackDistance = config.getPropertyInt("Accommodations.BackToBackDistance", 6);
            iEarlySlot = config.getPropertyInt("WorkDay.EarlySlot", 102);
            iLateSlot = config.getPropertyInt("WorkDay.LateSlot", 210);
            iFreeTimeAccommodation = config.getProperty("Accommodations.FreeTimeReference", iFreeTimeAccommodation);
            iBackToBackAccommodation = config.getProperty("Accommodations.BackToBackReference", iBackToBackAccommodation);
            iBreakBetweenClassesAccommodation = config.getProperty("Accommodations.BreakBetweenClassesReference", iBreakBetweenClassesAccommodation);
            iTypes = new ArrayList<Type>();
            for (Type t: Type.values())
                if (config.getPropertyDouble(t.getWeightName(), t.getWeightDefault()) != 0.0)
                    iTypes.add(t);
            iUnavailabilityMaxTravelTime = config.getPropertyInteger("Distances.UnavailabilityMaxTravelTimeInMinutes", null);
            if (iUnavailabilityMaxTravelTime != null && iUnavailabilityMaxTravelTime != iDistanceMetric.getMaxTravelDistanceInMinutes()) {
                iUnavailabilityDistanceMetric = new DistanceMetric(iDistanceMetric);
                iUnavailabilityDistanceMetric.setMaxTravelDistanceInMinutes(iUnavailabilityMaxTravelTime);
                iUnavailabilityDistanceMetric.setComputeDistanceConflictsBetweenNonBTBClasses(true);
            }
        }
        
        public DistanceMetric getDistanceMetric() {
            return iDistanceMetric;
        }
        
        public DistanceMetric getUnavailabilityDistanceMetric() {
            return (iUnavailabilityDistanceMetric == null ? iDistanceMetric : iUnavailabilityDistanceMetric);
        }
        
        public boolean isDebug() { return iDebug; }
        
        public double getTimeOverlapMaxLimit() { return iTimeOverlapMaxLimit; }
        public int getLunchStart() { return iLunchStart; }
        public int getLunchEnd() { return iLunchEnd; }
        public int getLunchLength() { return iLunchLength; }
        public int getMaxTravelGap() { return iMaxTravelGap; }
        public int getWorkDayLimit() { return iWorkDayLimit; }
        public int getBackToBackDistance() { return iBackToBackDistance; }
        public int getAccBackToBackDistance() { return iAccBackToBackDistance; }
        public int getEarlySlot() { return iEarlySlot; }
        public int getLateSlot() { return iLateSlot; }
        public String getFreeTimeAccommodation() { return iFreeTimeAccommodation; }
        public String getBackToBackAccommodation() { return iBackToBackAccommodation; }
        public String getBreakBetweenClassesAccommodation() { return iBreakBetweenClassesAccommodation; }
        public List<Type> getTypes() { return iTypes; }
            
        private Map<Long, Map<Long, Integer>> iDistanceCache = new HashMap<Long, Map<Long,Integer>>();
        protected Integer getDistanceInMinutesFromCache(RoomLocation r1, RoomLocation r2) {
            ReadLock lock = iLock.readLock();
            lock.lock();
            try {
                Map<Long, Integer> other2distance = iDistanceCache.get(r1.getId());
                return other2distance == null ? null : other2distance.get(r2.getId());
            } finally {
                lock.unlock();
            }
        }
        
        protected void setDistanceInMinutesFromCache(RoomLocation r1, RoomLocation r2, Integer distance) {
            WriteLock lock = iLock.writeLock();
            lock.lock();
            try {
                Map<Long, Integer> other2distance = iDistanceCache.get(r1.getId());
                if (other2distance == null) {
                    other2distance = new HashMap<Long, Integer>();
                    iDistanceCache.put(r1.getId(), other2distance);
                }
                other2distance.put(r2.getId(), distance);
            } finally {
                lock.unlock();
            }
        }
        
        protected int getDistanceInMinutes(RoomLocation r1, RoomLocation r2) {
            if (r1.getId().compareTo(r2.getId()) > 0) return getDistanceInMinutes(r2, r1);
            if (r1.getId().equals(r2.getId()) || r1.getIgnoreTooFar() || r2.getIgnoreTooFar())
                return 0;
            if (r1.getPosX() == null || r1.getPosY() == null || r2.getPosX() == null || r2.getPosY() == null)
                return iDistanceMetric.getMaxTravelDistanceInMinutes();
            Integer distance = getDistanceInMinutesFromCache(r1, r2);
            if (distance == null) {
                distance = iDistanceMetric.getDistanceInMinutes(r1.getId(), r1.getPosX(), r1.getPosY(), r2.getId(), r2.getPosX(), r2.getPosY());
                setDistanceInMinutesFromCache(r1, r2, distance);
            }
            return distance;
        }

        public int getDistanceInMinutes(Placement p1, Placement p2) {
            if (p1.isMultiRoom()) {
                if (p2.isMultiRoom()) {
                    int dist = 0;
                    for (RoomLocation r1 : p1.getRoomLocations()) {
                        for (RoomLocation r2 : p2.getRoomLocations()) {
                            dist = Math.max(dist, getDistanceInMinutes(r1, r2));
                        }
                    }
                    return dist;
                } else {
                    if (p2.getRoomLocation() == null)
                        return 0;
                    int dist = 0;
                    for (RoomLocation r1 : p1.getRoomLocations()) {
                        dist = Math.max(dist, getDistanceInMinutes(r1, p2.getRoomLocation()));
                    }
                    return dist;
                }
            } else if (p2.isMultiRoom()) {
                if (p1.getRoomLocation() == null)
                    return 0;
                int dist = 0;
                for (RoomLocation r2 : p2.getRoomLocations()) {
                    dist = Math.max(dist, getDistanceInMinutes(p1.getRoomLocation(), r2));
                }
                return dist;
            } else {
                if (p1.getRoomLocation() == null || p2.getRoomLocation() == null)
                    return 0;
                return getDistanceInMinutes(p1.getRoomLocation(), p2.getRoomLocation());
            }
        }
        
        private Map<Long, Map<Long, Integer>> iUnavailabilityDistanceCache = new HashMap<Long, Map<Long,Integer>>();
        protected Integer getUnavailabilityDistanceInMinutesFromCache(RoomLocation r1, RoomLocation r2) {
            ReadLock lock = iLock.readLock();
            lock.lock();
            try {
                Map<Long, Integer> other2distance = iUnavailabilityDistanceCache.get(r1.getId());
                return other2distance == null ? null : other2distance.get(r2.getId());
            } finally {
                lock.unlock();
            }
        }
        
        protected void setUnavailabilityDistanceInMinutesFromCache(RoomLocation r1, RoomLocation r2, Integer distance) {
            WriteLock lock = iLock.writeLock();
            lock.lock();
            try {
                Map<Long, Integer> other2distance = iUnavailabilityDistanceCache.get(r1.getId());
                if (other2distance == null) {
                    other2distance = new HashMap<Long, Integer>();
                    iUnavailabilityDistanceCache.put(r1.getId(), other2distance);
                }
                other2distance.put(r2.getId(), distance);
            } finally {
                lock.unlock();
            }
        }
        
        protected int getUnavailabilityDistanceInMinutes(RoomLocation r1, RoomLocation r2) {
            if (iUnavailabilityDistanceMetric == null) return getDistanceInMinutes(r1, r2);
            if (r1.getId().compareTo(r2.getId()) > 0) return getUnavailabilityDistanceInMinutes(r2, r1);
            if (r1.getId().equals(r2.getId()) || r1.getIgnoreTooFar() || r2.getIgnoreTooFar())
                return 0;
            if (r1.getPosX() == null || r1.getPosY() == null || r2.getPosX() == null || r2.getPosY() == null)
                return iUnavailabilityDistanceMetric.getMaxTravelDistanceInMinutes();
            Integer distance = getUnavailabilityDistanceInMinutesFromCache(r1, r2);
            if (distance == null) {
                distance = iUnavailabilityDistanceMetric.getDistanceInMinutes(r1.getId(), r1.getPosX(), r1.getPosY(), r2.getId(), r2.getPosX(), r2.getPosY());
                setUnavailabilityDistanceInMinutesFromCache(r1, r2, distance);
            }
            return distance;
        }

        public int getUnavailabilityDistanceInMinutes(Placement p1, Unavailability p2) {
            if (p1.isMultiRoom()) {
                int dist = 0;
                for (RoomLocation r1 : p1.getRoomLocations()) {
                    for (RoomLocation r2 : p2.getRooms()) {
                        dist = Math.max(dist, getUnavailabilityDistanceInMinutes(r1, r2));
                    }
                }
                return dist;
            } else {
                if (p1.getRoomLocation() == null)
                    return 0;
                int dist = 0;
                for (RoomLocation r2 : p2.getRooms()) {
                    dist = Math.max(dist, getUnavailabilityDistanceInMinutes(p1.getRoomLocation(), r2));
                }
                return dist;
            }
        }      
    }
    
    /**
     * Assignment context
     */
    public class StudentQualityContext implements AssignmentConstraintContext<Request, Enrollment> {
        private int[] iTotalPenalty = null;
        private Set<Conflict>[] iAllConflicts = null;
        private Request iOldVariable = null;
        private Enrollment iUnassignedValue = null;

        @SuppressWarnings("unchecked")
        public StudentQualityContext(Assignment<Request, Enrollment> assignment) {
            iTotalPenalty = new int[Type.values().length];
            for (Type t: iContext.getTypes())
                iTotalPenalty[t.ordinal()] = countTotalPenalty(t, assignment);
            if (iContext.isDebug()) {
                iAllConflicts = new Set[Type.values().length];
                for (Type t: iContext.getTypes())
                    iAllConflicts[t.ordinal()] = computeAllConflicts(t, assignment);
            }
            StudentSectioningModelContext cx = ((StudentSectioningModel)getModel()).getContext(assignment);
            for (Type t: iContext.getTypes())
                for (Conflict c: computeAllConflicts(t, assignment)) cx.add(assignment, c);
        }
        
        @SuppressWarnings("unchecked")
        public StudentQualityContext(StudentQualityContext parent) {
            iTotalPenalty = new int[Type.values().length];
            for (Type t: iContext.getTypes())
                iTotalPenalty[t.ordinal()] = parent.iTotalPenalty[t.ordinal()];
            if (iContext.isDebug()) {
                iAllConflicts = new Set[Type.values().length];
                for (Type t: iContext.getTypes())
                    iAllConflicts[t.ordinal()] = new HashSet<Conflict>(parent.iAllConflicts[t.ordinal()]);
            }
        }

        @Override
        public void assigned(Assignment<Request, Enrollment> assignment, Enrollment value) {
            StudentSectioningModelContext cx = ((StudentSectioningModel)getModel()).getContext(assignment);
            for (Type type: iContext.getTypes()) {
                iTotalPenalty[type.ordinal()] += allPenalty(type, assignment, value);
                for (Conflict c: allConflicts(type, assignment, value))
                    cx.add(assignment, c);
            }
            if (iContext.isDebug()) {
                sLog.debug("A:" + value.variable() + " := " + value);
                for (Type type: iContext.getTypes()) {
                    int inc = allPenalty(type, assignment, value);
                    if (inc != 0) {
                        sLog.debug("-- " + type + " +" + inc + " A: " + value.variable() + " := " + value);
                        for (Conflict c: allConflicts(type, assignment, value)) {
                            sLog.debug("  -- " + c);
                            iAllConflicts[type.ordinal()].add(c);
                            inc -= c.getPenalty();
                        }
                        if (inc != 0) {
                            sLog.error(type + ": Different penalty for the assigned value (difference: " + inc + ")!");
                        }
                    }
                }
            }
        }

        /**
         * Called when a value is unassigned from a variable. Internal number of
         * time overlapping conflicts is updated, see
         * {@link TimeOverlapsCounter#getTotalNrConflicts(Assignment)}.
         */
        @Override
        public void unassigned(Assignment<Request, Enrollment> assignment, Enrollment value) {
            StudentSectioningModelContext cx = ((StudentSectioningModel)getModel()).getContext(assignment);
            for (Type type: iContext.getTypes()) {
                iTotalPenalty[type.ordinal()] -= allPenalty(type, assignment, value);
                for (Conflict c: allConflicts(type, assignment, value))
                    cx.remove(assignment, c);
            }
            if (iContext.isDebug()) {
                sLog.debug("U:" + value.variable() + " := " + value);
                for (Type type: iContext.getTypes()) {
                    int dec = allPenalty(type, assignment, value);
                    if (dec != 0) {
                        sLog.debug("--  " + type + " -" + dec + " U: " + value.variable() + " := " + value);
                        for (Conflict c: allConflicts(type, assignment, value)) {
                            sLog.debug("  -- " + c);
                            iAllConflicts[type.ordinal()].remove(c);
                            dec -= c.getPenalty();
                        }
                        if (dec != 0) {
                            sLog.error(type + ":Different penalty for the unassigned value (difference: " + dec + ")!");
                        }
                    }
                }
            }
        }
        
        /**
         * Called before a value is assigned to a variable.
         * @param assignment current assignment
         * @param iteration current iteration
         * @param value value to be assigned
         */
        public void beforeAssigned(Assignment<Request, Enrollment> assignment, long iteration, Enrollment value) {
            if (value != null) {
                Enrollment old = assignment.getValue(value.variable());
                if (old != null) {
                    iUnassignedValue = old;
                    unassigned(assignment, old);
                }
                iOldVariable = value.variable();
            }
        }

        /**
         * Called after a value is assigned to a variable.
         * @param assignment current assignment
         * @param iteration current iteration
         * @param value value that was assigned
         */
        public void afterAssigned(Assignment<Request, Enrollment> assignment, long iteration, Enrollment value) {
            iOldVariable = null;
            iUnassignedValue = null;
            if (value != null) {
                assigned(assignment, value);
            }
        }

        /**
         * Called after a value is unassigned from a variable.
         * @param assignment current assignment
         * @param iteration current iteration
         * @param value value that was unassigned
         */
        public void afterUnassigned(Assignment<Request, Enrollment> assignment, long iteration, Enrollment value) {
            if (value != null && !value.equals(iUnassignedValue)) {
                unassigned(assignment, value);
            }
        }
        
        public Set<Conflict> getAllConflicts(Type type) {
            return iAllConflicts[type.ordinal()];
        }
        
        public int getTotalPenalty(Type type) {
            return iTotalPenalty[type.ordinal()];
        }
        
        public void checkTotalPenalty(Type type, Assignment<Request, Enrollment> assignment) {
            int total = countTotalPenalty(type, assignment);
            if (total != iTotalPenalty[type.ordinal()]) {
                sLog.error(type + " penalty does not match for (actual: " + total + ", count: " + iTotalPenalty[type.ordinal()] + ")!");
                iTotalPenalty[type.ordinal()] = total;
                if (iContext.isDebug()) {
                    Set<Conflict> conflicts = computeAllConflicts(type, assignment);
                    for (Conflict c: conflicts) {
                        if (!iAllConflicts[type.ordinal()].contains(c))
                            sLog.debug("  +add+ " + c);
                    }
                    for (Conflict c: iAllConflicts[type.ordinal()]) {
                        if (!conflicts.contains(c))
                            sLog.debug("  -rem- " + c);
                    }
                    for (Conflict c: conflicts) {
                        for (Conflict d: iAllConflicts[type.ordinal()]) {
                            if (c.equals(d) && c.getPenalty() != d.getPenalty()) {
                                sLog.debug("  -dif- " + c + " (other: " + d.getPenalty() + ")");
                            }
                        }
                    }                
                    iAllConflicts[type.ordinal()] = conflicts;
                }
            }
        }
        
        public int countTotalPenalty(Type type, Assignment<Request, Enrollment> assignment) {
            int total = 0;
            for (Request r1 : getModel().variables()) {
                Enrollment e1 = assignment.getValue(r1);
                if (e1 == null || r1.equals(iOldVariable)) continue;
                for (Request r2 : r1.getStudent().getRequests()) {
                    Enrollment e2 = assignment.getValue(r2);
                    if (e2 != null && r1.getId() < r2.getId() && !r2.equals(iOldVariable)) {
                        if (type.isApplicable(iContext, r1.getStudent(), r1, r2))
                            total += penalty(type, e1, e2);
                    }
                }
                total += penalty(type, e1);
            }
            return total;
        }

        public Set<Conflict> computeAllConflicts(Type type, Assignment<Request, Enrollment> assignment) {
            Set<Conflict> ret = new HashSet<Conflict>();
            for (Request r1 : getModel().variables()) {
                Enrollment e1 = assignment.getValue(r1);
                if (e1 == null || r1.equals(iOldVariable)) continue;
                for (Request r2 : r1.getStudent().getRequests()) {
                    Enrollment e2 = assignment.getValue(r2);
                    if (e2 != null && r1.getId() < r2.getId() && !r2.equals(iOldVariable)) {
                        if (type.isApplicable(iContext, r1.getStudent(), r1, r2))
                            ret.addAll(conflicts(type, e1, e2));
                    }                    
                }
                ret.addAll(conflicts(type, e1));
            }
            return ret;
        }
        
        public Set<Conflict> allConflicts(Type type, Assignment<Request, Enrollment> assignment, Student student) {
            Set<Conflict> ret = new HashSet<Conflict>();
            for (Request r1 : student.getRequests()) {
                Enrollment e1 = assignment.getValue(r1);
                if (e1 == null) continue;
                for (Request r2 : student.getRequests()) {
                    Enrollment e2 = assignment.getValue(r2);
                    if (e2 != null && r1.getId() < r2.getId()) {
                        if (type.isApplicable(iContext, r1.getStudent(), r1, r2))
                            ret.addAll(conflicts(type, e1, e2));
                    }
                }
                ret.addAll(conflicts(type, e1));
            }
            return ret;
        }

        public Set<Conflict> allConflicts(Type type, Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
            Set<Conflict> ret = new HashSet<Conflict>();
            for (Request request : enrollment.getStudent().getRequests()) {
                if (request.equals(enrollment.getRequest())) continue;
                if (assignment.getValue(request) != null && !request.equals(iOldVariable)) {
                    ret.addAll(conflicts(type, enrollment, assignment.getValue(request)));
                }
            }
            ret.addAll(conflicts(type, enrollment));
            return ret;
        }
        
        public int allPenalty(Type type, Assignment<Request, Enrollment> assignment, Student student) {
            int penalty = 0;
            for (Request r1 : student.getRequests()) {
                Enrollment e1 = assignment.getValue(r1);
                if (e1 == null) continue;
                for (Request r2 : student.getRequests()) {
                    Enrollment e2 = assignment.getValue(r2);
                    if (e2 != null && r1.getId() < r2.getId()) {
                        if (type.isApplicable(iContext, r1.getStudent(), r1, r2))
                            penalty += penalty(type, e1, e2); 
                    }
                }
                penalty += penalty(type, e1);
            }
            return penalty;
        }
        
        public int allPenalty(Type type, Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
            int penalty = 0;
            for (Request request : enrollment.getStudent().getRequests()) {
                if (request.equals(enrollment.getRequest())) continue;
                if (assignment.getValue(request) != null && !request.equals(iOldVariable)) {
                    if (type.isApplicable(iContext, enrollment.getStudent(), enrollment.variable(), request))
                        penalty += penalty(type, enrollment, assignment.getValue(request));
                }
            }
            penalty += penalty(type, enrollment);
            return penalty;
        }
    }

    @Override
    public StudentQualityContext createAssignmentContext(Assignment<Request, Enrollment> assignment) {
        return new StudentQualityContext(assignment);
    }

    @Override
    public StudentQualityContext inheritAssignmentContext(Assignment<Request, Enrollment> assignment, StudentQualityContext parentContext) {
        return new StudentQualityContext(parentContext);
    }
    
    /** Empty iterator */
    public static class Nothing implements Iterable<SctAssignment> {
        @Override
        public Iterator<SctAssignment> iterator() {
            return new Iterator<SctAssignment>() {
                @Override
                public SctAssignment next() { return null; }
                @Override
                public boolean hasNext() { return false; }
                @Override
                public void remove() { throw new UnsupportedOperationException(); }
            };
        }
    }
    
    /** Unavailabilities of a student */
    public static class Unavailabilities implements Iterable<Unavailability> {
        private Student iStudent;
        public Unavailabilities(Student student) { iStudent = student; }
        @Override
        public Iterator<Unavailability> iterator() { return iStudent.getUnavailabilities().iterator(); }
    }
    
    private static class SingleTime implements SctAssignment {
        private TimeLocation iTime = null;
        
        public SingleTime(int start, int end) {
            iTime = new TimeLocation(0x7f, start, end-start, 0, 0.0, 0, null, null, new BitSet(), 0);
        }

        @Override
        public TimeLocation getTime() { return iTime; }
        @Override
        public List<RoomLocation> getRooms() { return null; }
        @Override
        public int getNrRooms() { return 0; }
        @Override
        public void assigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {}
        @Override
        public void unassigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {}
        @Override
        public Set<Enrollment> getEnrollments(Assignment<Request, Enrollment> assignment) { return null; }
        @Override
        public boolean isAllowOverlap() { return false; }
        @Override
        public long getId() { return -1;}
        @Override
        public int compareById(SctAssignment a) { return 0; }

        @Override
        public boolean isOverlapping(SctAssignment assignment) {
            return assignment.getTime() != null && getTime().shareDays(assignment.getTime()) && getTime().shareHours(assignment.getTime());
        }

        @Override
        public boolean isOverlapping(Set<? extends SctAssignment> assignments) {
            for (SctAssignment assignment : assignments) {
                if (isOverlapping(assignment)) return true;
            }
            return false;
        }
    }
    
    /** Early/late time */
    public static class SingleTimeIterable implements Iterable<SingleTime> {
        private SingleTime iTime = null;
        public SingleTimeIterable(int start, int end) {
            if (start < end)
                iTime = new SingleTime(start, end);
            
        }
        @Override
        public Iterator<SingleTime> iterator() {
            return new Iterator<SingleTime>() {
                @Override
                public SingleTime next() {
                    SingleTime ret = iTime; iTime = null; return ret;
                }
                @Override
                public boolean hasNext() { return iTime != null; }
                @Override
                public void remove() { throw new UnsupportedOperationException(); }
            };
        }
    }
    
    /** Free times of a student */
    public static class FreeTimes implements Iterable<FreeTimeRequest> {
        private Student iStudent;
        public FreeTimes(Student student) {
            iStudent = student;
        }
        
        @Override
        public Iterator<FreeTimeRequest> iterator() {
            return new Iterator<FreeTimeRequest>() {
                Iterator<Request> i = iStudent.getRequests().iterator();
                FreeTimeRequest next = null;
                boolean hasNext = nextFreeTime();
                
                private boolean nextFreeTime() {
                    while (i.hasNext()) {
                        Request r = i.next();
                        if (r instanceof FreeTimeRequest) {
                            next = (FreeTimeRequest)r;
                            return true;
                        }
                    }
                    return false;
                }
                
                @Override
                public FreeTimeRequest next() {
                    try {
                        return next;
                    } finally {
                        hasNext = nextFreeTime();
                    }
                }
                @Override
                public boolean hasNext() { return hasNext; }
                @Override
                public void remove() { throw new UnsupportedOperationException(); }
            };
        }
    }
    
    /** Online (or not-online) classes of an enrollment */
    public static class Online implements Iterable<Section> {
        private Enrollment iEnrollment;
        private boolean iOnline;
        public Online(Enrollment enrollment, boolean online) {
            iEnrollment = enrollment;
            iOnline = online;
        }
        
        protected boolean skip(Section section) {
            return iOnline != section.isOnline();
        }
        
        @Override
        public Iterator<Section> iterator() {
            return new Iterator<Section>() {
                Iterator<Section> i = iEnrollment.getSections().iterator();
                Section next = null;
                boolean hasNext = nextSection();
                
                private boolean nextSection() {
                    while (i.hasNext()) {
                        Section r = i.next();
                        if (!skip(r)) {
                            next = r;
                            return true;
                        }
                    }
                    return false;
                }
                
                @Override
                public Section next() {
                    try {
                        return next;
                    } finally {
                        hasNext = nextSection();
                    }
                }
                @Override
                public boolean hasNext() { return hasNext; }
                @Override
                public void remove() { throw new UnsupportedOperationException(); }
            };
        }
    }

    @Override
    public void getInfo(Assignment<Request, Enrollment> assignment, Map<String, String> info) {
        StudentQualityContext cx = getContext(assignment);
        if (iContext.isDebug())
            for (Type type: iContext.getTypes())
                info.put("[Schedule Quality] " + type.getName(), String.valueOf(cx.getTotalPenalty(type)));
    }

    @Override
    public void getInfo(Assignment<Request, Enrollment> assignment, Map<String, String> info, Collection<Request> variables) {
    }
    
    public String toString(Assignment<Request, Enrollment> assignment) {
        String ret = "";
        StudentQualityContext cx = getContext(assignment);
        for (Type type: iContext.getTypes()) {
            int p = cx.getTotalPenalty(type);
            if (p != 0) {
                ret += (ret.isEmpty() ? "" : ", ") + type.getAbbv() + ": " + p;
            }
        }
        return ret;
    }
    
    public boolean hasDistanceConflict(Student student, Section s1, Section s2) {
        if (student.isNeedShortDistances())
            return Type.ShortDistance.inConflict(iContext, s1, s2);
        else
            return Type.Distance.inConflict(iContext, s1, s2);
    }
}
