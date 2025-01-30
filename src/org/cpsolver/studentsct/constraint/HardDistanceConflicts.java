package org.cpsolver.studentsct.constraint;

import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.extension.StudentQuality;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.SctAssignment;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Unavailability;

/**
 * Hard distance conflicts constraint. This global constraint checks for distance conflicts
 * that should not be allowed. These are distance conflicts where the distance betweem the
 * two sections is longer than HardDistanceConflict.DistanceHardLimitInMinutes minutes (defaults to 60)
 * and the distance to travel between the two sections is longer than
 * HardDistanceConflict.AllowedDistanceInMinutes minutes (defaults to 30).
 * The constraint checks both pairs of sections that the student is to be enrolled in 
 * and distance conflicts with unavailabilities.
 * Hard distance conflicts are allowed between sections that allow for time conflicts.
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.4 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2025 Tomas Muller<br>
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
public class HardDistanceConflicts extends GlobalConstraint<Request, Enrollment> {
    /**
     * A given enrollment is conflicting, if there is a section that
     * is disabled and there is not a matching reservation that would allow for that.
     * 
     * @param enrollment {@link Enrollment} that is being considered
     * @param conflicts all computed conflicting requests are added into this set
     */
    @Override
    public void computeConflicts(Assignment<Request, Enrollment> assignment, Enrollment enrollment, Set<Enrollment> conflicts) {
        if (enrollment.variable().getModel() == null || !(enrollment.variable().getModel() instanceof StudentSectioningModel)) return;
        StudentSectioningModel model = (StudentSectioningModel)enrollment.variable().getModel();
        StudentQuality studentQuality = model.getStudentQuality();
        if (studentQuality == null) return;
        StudentQuality.Context cx = studentQuality.getStudentQualityContext();
        
        // no distance conflicts when overlaps are allowed by a reservation
        if (enrollment.getReservation() != null && enrollment.getReservation().isAllowOverlap()) return;
        
        // enrollment's student
        Student student = enrollment.getStudent();
        // no unavailabilities > no distance conflicts
        if (student.getUnavailabilities().isEmpty()) return;
        
        // enrollment's config
        Config config = enrollment.getConfig();

        // exclude free time requests
        if (config == null) return;
        
        // check for an unavailability distance conflict
        if (cx.getUnavailabilityDistanceMetric().isHardDistanceConflictsEnabled()) {
            for (Section s1: enrollment.getSections()) {
                // no time or no room > no conflict
                if (!s1.hasTime() || s1.getNrRooms() == 0 || s1.isAllowOverlap()) continue;
                for (Unavailability s2: student.getUnavailabilities()) {
                    // no time or no room > no conflict
                    if (s2.getTime() == null || s2.getNrRooms() == 0) continue;
                    TimeLocation t1 = s1.getTime();
                    TimeLocation t2 = s2.getTime();
                    // no shared day > no conflict
                    if (!t1.shareDays(t2) || !t1.shareWeeks(t2)) continue;
                    int a1 = t1.getStartSlot(), a2 = t2.getStartSlot();
                    if (a1 + t1.getNrSlotsPerMeeting() <= a2) {
                        int dist = cx.getUnavailabilityDistanceInMinutes(s1.getPlacement(), s2);
                        if (dist >= cx.getUnavailabilityDistanceMetric().getDistanceHardLimitInMinutes()
                                && dist > t1.getBreakTime() + Constants.SLOT_LENGTH_MIN * (a2 - a1 - t1.getLength()) + cx.getUnavailabilityDistanceMetric().getAllowedDistanceInMinutes()) {
                            conflicts.add(enrollment);
                            return;
                        }
                    } else if (a2 + t2.getNrSlotsPerMeeting() <= a1) {
                        int dist = cx.getUnavailabilityDistanceInMinutes(s1.getPlacement(), s2);
                        if (dist >= cx.getUnavailabilityDistanceMetric().getDistanceHardLimitInMinutes()
                                && dist > t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (a1 - a2 - t2.getLength()) + cx.getUnavailabilityDistanceMetric().getAllowedDistanceInMinutes()) {
                            conflicts.add(enrollment);
                            return;
                        }
                    }
                }
            }
        }
        
        // check for distance conflicts within the enrollment
        if (cx.getDistanceMetric().isHardDistanceConflictsEnabled()) {
            for (Section s1: enrollment.getSections()) {
                // no time or no room > no conflict
                if (!s1.hasTime() || s1.getNrRooms() == 0 || s1.isAllowOverlap()) continue;
                for (Section s2: enrollment.getSections()) {
                    if (s1.getId() < s2.getId()) {
                        // no time or no room > no conflict
                        if (!s2.hasTime() || s2.getNrRooms() == 0 || s2.isAllowOverlap() || s1.isToIgnoreStudentConflictsWith(s2.getId())) continue;
                        TimeLocation t1 = s1.getTime();
                        TimeLocation t2 = s2.getTime();
                        // no shared day > no conflict
                        if (!t1.shareDays(t2) || !t1.shareWeeks(t2)) continue;
                        int a1 = t1.getStartSlot(), a2 = t2.getStartSlot();
                        if (cx.getDistanceMetric().doComputeDistanceConflictsBetweenNonBTBClasses()) {
                            if (a1 + t1.getNrSlotsPerMeeting() <= a2) {
                                int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                                if (dist >= cx.getDistanceMetric().getDistanceHardLimitInMinutes()
                                        && dist > t1.getBreakTime() + Constants.SLOT_LENGTH_MIN * (a2 - a1 - t1.getLength()) + cx.getDistanceMetric().getAllowedDistanceInMinutes()) {
                                    conflicts.add(enrollment);
                                    return;
                                }
                            } else if (a2 + t2.getNrSlotsPerMeeting() <= a1) {
                                int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                                if (dist >= cx.getDistanceMetric().getDistanceHardLimitInMinutes()
                                        && dist > t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (a1 - a2 - t2.getLength()) + cx.getDistanceMetric().getAllowedDistanceInMinutes()) {
                                    conflicts.add(enrollment);
                                    return;
                                }
                            }
                        } else {
                            if (a1 + t1.getNrSlotsPerMeeting() == a2) {
                                int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                                if (dist >= cx.getDistanceMetric().getDistanceHardLimitInMinutes()
                                        && dist > t1.getBreakTime() + cx.getDistanceMetric().getAllowedDistanceInMinutes()) {
                                    conflicts.add(enrollment);
                                    return;
                                }
                            } else if (a2 + t2.getNrSlotsPerMeeting() == a1) {
                                int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                                if (dist >= cx.getDistanceMetric().getDistanceHardLimitInMinutes()
                                        && dist > t2.getBreakTime() + cx.getDistanceMetric().getAllowedDistanceInMinutes()) {
                                    conflicts.add(enrollment);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
            
            // check conflicts with other enrollments of the student
            other: for (Request other: student.getRequests()) {
                if (other.equals(enrollment.variable())) continue;
                Enrollment e2 = other.getAssignment(assignment);
                if (e2 == null || conflicts.contains(e2)) continue;
                if (e2.getReservation() != null && e2.getReservation().isAllowOverlap()) continue;
                for (Section s1: enrollment.getSections()) {
                    // no time or no room > no conflict
                    if (!s1.hasTime() || s1.getNrRooms() == 0 || s1.isAllowOverlap()) continue;
                    for (Section s2: e2.getSections()) {
                        // no time or no room > no conflict
                        if (!s2.hasTime() || s2.getNrRooms() == 0 || s2.isAllowOverlap() || s1.isToIgnoreStudentConflictsWith(s2.getId())) continue;
                        TimeLocation t1 = s1.getTime();
                        TimeLocation t2 = s2.getTime();
                        // no shared day > no conflict
                        if (!t1.shareDays(t2) || !t1.shareWeeks(t2)) continue;
                        int a1 = t1.getStartSlot(), a2 = t2.getStartSlot();
                        if (cx.getDistanceMetric().doComputeDistanceConflictsBetweenNonBTBClasses()) {
                            if (a1 + t1.getNrSlotsPerMeeting() <= a2) {
                                int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                                if (dist >= cx.getDistanceMetric().getDistanceHardLimitInMinutes()
                                        && dist > t1.getBreakTime() + Constants.SLOT_LENGTH_MIN * (a2 - a1 - t1.getLength()) + cx.getDistanceMetric().getAllowedDistanceInMinutes()) {
                                    conflicts.add(e2);
                                    continue other;
                                }
                            } else if (a2 + t2.getNrSlotsPerMeeting() <= a1) {
                                int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                                if (dist >= cx.getDistanceMetric().getDistanceHardLimitInMinutes()
                                        && dist > t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (a1 - a2 - t2.getLength()) + cx.getDistanceMetric().getAllowedDistanceInMinutes()) {
                                    conflicts.add(e2);
                                    continue other;
                                }
                            }
                        } else {
                            if (a1 + t1.getNrSlotsPerMeeting() == a2) {
                                int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                                if (dist >= cx.getDistanceMetric().getDistanceHardLimitInMinutes()
                                        && dist > t1.getBreakTime() + cx.getDistanceMetric().getAllowedDistanceInMinutes()) {
                                    conflicts.add(e2);
                                    continue other;
                                }
                            } else if (a2 + t2.getNrSlotsPerMeeting() == a1) {
                                int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                                if (dist >= cx.getDistanceMetric().getDistanceHardLimitInMinutes()
                                        && dist > t2.getBreakTime() + cx.getDistanceMetric().getAllowedDistanceInMinutes()) {
                                    conflicts.add(e2);
                                    continue other;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * A given enrollment is conflicting, if there is a section that
     * is disabled and there is not a matching reservation that would allow for that.
     * 
     * @param enrollment {@link Enrollment} that is being considered
     * @return true, if the enrollment does not follow a reservation that must be used 
     */
    @Override
    public boolean inConflict(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        if (enrollment.variable().getModel() == null || !(enrollment.variable().getModel() instanceof StudentSectioningModel)) return false;
        StudentSectioningModel model = (StudentSectioningModel)enrollment.variable().getModel();
        StudentQuality studentQuality = model.getStudentQuality();
        if (studentQuality == null) return false;
        StudentQuality.Context cx = studentQuality.getStudentQualityContext();
        
        // no distance conflicts when overlaps are allowed by a reservation
        if (enrollment.getReservation() != null && enrollment.getReservation().isAllowOverlap()) return false;
        
        // enrollment's student
        Student student = enrollment.getStudent();
        // no unavailabilities > no distance conflicts
        if (student.getUnavailabilities().isEmpty()) return false;
        
        // enrollment's config
        Config config = enrollment.getConfig();

        // exclude free time requests
        if (config == null) return false;
        
        // check for an unavailability distance conflict
        if (cx.getUnavailabilityDistanceMetric().isHardDistanceConflictsEnabled()) {
            for (Section s1: enrollment.getSections()) {
                // no time or no room > no conflict
                if (!s1.hasTime() || s1.getNrRooms() == 0 || s1.isAllowOverlap()) continue;
                for (Unavailability s2: student.getUnavailabilities()) {
                    // no time or no room > no conflict
                    if (s2.getTime() == null || s2.getNrRooms() == 0 || s2.isAllowOverlap()) continue;
                    TimeLocation t1 = s1.getTime();
                    TimeLocation t2 = s2.getTime();
                    // no shared day > no conflict
                    if (!t1.shareDays(t2) || !t1.shareWeeks(t2)) continue;
                    int a1 = t1.getStartSlot(), a2 = t2.getStartSlot();
                    if (a1 + t1.getNrSlotsPerMeeting() <= a2) {
                        int dist = cx.getUnavailabilityDistanceInMinutes(s1.getPlacement(), s2);
                        if (dist >= cx.getUnavailabilityDistanceMetric().getDistanceHardLimitInMinutes()
                                && dist > t1.getBreakTime() + Constants.SLOT_LENGTH_MIN * (a2 - a1 - t1.getLength()) + cx.getUnavailabilityDistanceMetric().getAllowedDistanceInMinutes())
                            return true;
                    } else if (a2 + t2.getNrSlotsPerMeeting() <= a1) {
                        int dist = cx.getUnavailabilityDistanceInMinutes(s1.getPlacement(), s2);
                        if (dist >= cx.getUnavailabilityDistanceMetric().getDistanceHardLimitInMinutes()
                                && dist > t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (a1 - a2 - t2.getLength()) + cx.getUnavailabilityDistanceMetric().getAllowedDistanceInMinutes())
                            return true;
                    }
                }
            }
        }
        
        // check for distance conflicts within the enrollment
        if (cx.getDistanceMetric().isHardDistanceConflictsEnabled()) {
            for (Section s1: enrollment.getSections()) {
                // no time or no room > no conflict
                if (!s1.hasTime() || s1.getNrRooms() == 0 || s1.isAllowOverlap()) continue;
                for (Section s2: enrollment.getSections()) {
                    if (s1.getId() < s2.getId()) {
                        // no time or no room > no conflict
                        if (!s2.hasTime() || s2.getNrRooms() == 0 || s2.isAllowOverlap() || s1.isToIgnoreStudentConflictsWith(s2.getId())) continue;
                        TimeLocation t1 = s1.getTime();
                        TimeLocation t2 = s2.getTime();
                        // no shared day > no conflict
                        if (!t1.shareDays(t2) || !t1.shareWeeks(t2)) continue;
                        int a1 = t1.getStartSlot(), a2 = t2.getStartSlot();
                        if (cx.getDistanceMetric().doComputeDistanceConflictsBetweenNonBTBClasses()) {
                            if (a1 + t1.getNrSlotsPerMeeting() <= a2) {
                                int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                                if (dist >= cx.getDistanceMetric().getDistanceHardLimitInMinutes() 
                                       && dist > t1.getBreakTime() + Constants.SLOT_LENGTH_MIN * (a2 - a1 - t1.getLength()) + cx.getDistanceMetric().getAllowedDistanceInMinutes())
                                    return true;
                            } else if (a2 + t2.getNrSlotsPerMeeting() <= a1) {
                                int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                                if (dist >= cx.getDistanceMetric().getDistanceHardLimitInMinutes()
                                        && dist > t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (a1 - a2 - t2.getLength()) + cx.getDistanceMetric().getAllowedDistanceInMinutes())
                                    return true;
                            }
                        } else {
                            if (a1 + t1.getNrSlotsPerMeeting() == a2) {
                                int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                                if (dist >= cx.getDistanceMetric().getDistanceHardLimitInMinutes()
                                        && dist > t1.getBreakTime() + cx.getDistanceMetric().getAllowedDistanceInMinutes())
                                    return true;
                            } else if (a2 + t2.getNrSlotsPerMeeting() == a1) {
                                int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                                if (dist >= cx.getDistanceMetric().getDistanceHardLimitInMinutes()
                                        && dist > t2.getBreakTime() + cx.getDistanceMetric().getAllowedDistanceInMinutes())
                                    return true;
                            }
                        }
                    }
                }
            }
            
            // check conflicts with other enrollments of the student
            for (Request other: student.getRequests()) {
                if (other.equals(enrollment.variable())) continue;
                Enrollment e2 = other.getAssignment(assignment);
                if (e2 == null) continue;
                if (e2.getReservation() != null && e2.getReservation().isAllowOverlap()) continue;
                for (Section s1: enrollment.getSections()) {
                    // no time or no room > no conflict
                    if (!s1.hasTime() || s1.getNrRooms() == 0 || s1.isAllowOverlap()) continue;
                    for (Section s2: e2.getSections()) {
                        // no time or no room > no conflict
                        if (!s2.hasTime() || s2.getNrRooms() == 0 || s2.isAllowOverlap() || s1.isToIgnoreStudentConflictsWith(s2.getId())) continue;
                        TimeLocation t1 = s1.getTime();
                        TimeLocation t2 = s2.getTime();
                        // no shared day > no conflict
                        if (!t1.shareDays(t2) || !t1.shareWeeks(t2)) continue;
                        int a1 = t1.getStartSlot(), a2 = t2.getStartSlot();
                        if (cx.getDistanceMetric().doComputeDistanceConflictsBetweenNonBTBClasses()) {
                            if (a1 + t1.getNrSlotsPerMeeting() <= a2) {
                                int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                                if (dist >= cx.getDistanceMetric().getDistanceHardLimitInMinutes() && dist > t1.getBreakTime() + Constants.SLOT_LENGTH_MIN * (a2 - a1 - t1.getLength()) + cx.getDistanceMetric().getAllowedDistanceInMinutes())
                                    return true;
                            } else if (a2 + t2.getNrSlotsPerMeeting() <= a1) {
                                int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                                if (dist >= cx.getDistanceMetric().getDistanceHardLimitInMinutes() && dist > t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (a1 - a2 - t2.getLength()) + cx.getDistanceMetric().getAllowedDistanceInMinutes())
                                    return true;
                            }
                        } else {
                            if (a1 + t1.getNrSlotsPerMeeting() == a2) {
                                int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                                if (dist >= cx.getDistanceMetric().getDistanceHardLimitInMinutes() && dist > t1.getBreakTime() + cx.getDistanceMetric().getAllowedDistanceInMinutes())
                                    return true;
                            } else if (a2 + t2.getNrSlotsPerMeeting() == a1) {
                                int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                                if (dist >= cx.getDistanceMetric().getDistanceHardLimitInMinutes() && dist > t2.getBreakTime() + cx.getDistanceMetric().getAllowedDistanceInMinutes())
                                    return true;
                            }
                        }
                        
                    }
                    
                }
            }
        }
        
        return false;
    }
    
    public static boolean inConflict(StudentQuality sq, Section s1, Unavailability s2) {
        if (sq == null || s1 == null || s2 == null) return false;
        if (s1.getPlacement() == null || s2.getTime() == null || s2.getNrRooms() == 0
                || s1.isAllowOverlap() || s2.isAllowOverlap()) return false;
        StudentQuality.Context cx = sq.getStudentQualityContext();
        if (!cx.getUnavailabilityDistanceMetric().isHardDistanceConflictsEnabled()) return false;
        TimeLocation t1 = s1.getTime();
        TimeLocation t2 = s2.getTime();
        if (!t1.shareDays(t2) || !t1.shareWeeks(t2))
            return false;
        int a1 = t1.getStartSlot(), a2 = t2.getStartSlot();
        if (a1 + t1.getNrSlotsPerMeeting() <= a2) {
            int dist = cx.getUnavailabilityDistanceInMinutes(s1.getPlacement(), s2);
            if (dist >= cx.getUnavailabilityDistanceMetric().getDistanceHardLimitInMinutes() &&
                    dist > t1.getBreakTime() + Constants.SLOT_LENGTH_MIN * (a2 - a1 - t1.getLength()) + cx.getUnavailabilityDistanceMetric().getAllowedDistanceInMinutes())
                return true;
        } else if (a2 + t2.getNrSlotsPerMeeting() <= a1) {
            int dist = cx.getUnavailabilityDistanceInMinutes(s1.getPlacement(), s2);
            if (dist >= cx.getUnavailabilityDistanceMetric().getDistanceHardLimitInMinutes() &&
                    dist > t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (a1 - a2 - t2.getLength()) + cx.getUnavailabilityDistanceMetric().getAllowedDistanceInMinutes())
                return true;
        }
        return false;
    }
    
    public static boolean inConflict(StudentQuality sq, Section s1, Section s2) {
        if (sq == null || s1 == null || s2 == null) return false;
        if (s1.getPlacement() == null || s2.getPlacement() == null
                || s1.isAllowOverlap() || s2.isAllowOverlap() || s1.isToIgnoreStudentConflictsWith(s2.getId())) return false;
        StudentQuality.Context cx = sq.getStudentQualityContext();
        if (!cx.getDistanceMetric().isHardDistanceConflictsEnabled()) return false;
        TimeLocation t1 = s1.getTime();
        TimeLocation t2 = s2.getTime();
        if (!t1.shareDays(t2) || !t1.shareWeeks(t2))
            return false;
        int a1 = t1.getStartSlot(), a2 = t2.getStartSlot();
        if (a1 + t1.getNrSlotsPerMeeting() <= a2) {
            int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
            if (dist >= cx.getDistanceMetric().getDistanceHardLimitInMinutes() &&
                    dist > t1.getBreakTime() + Constants.SLOT_LENGTH_MIN * (a2 - a1 - t1.getLength()) + cx.getDistanceMetric().getAllowedDistanceInMinutes())
                return true;
        } else if (a2 + t2.getNrSlotsPerMeeting() <= a1) {
            int dist = cx.getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
            if (dist >= cx.getDistanceMetric().getDistanceHardLimitInMinutes() &&
                    dist > t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (a1 - a2 - t2.getLength()) + cx.getDistanceMetric().getAllowedDistanceInMinutes())
                return true;
        }
        return false;
    }
    
    public static boolean inConflict(StudentQuality sq, SctAssignment s1, Enrollment e) {
        if (sq == null || s1 == null || e == null) return false;
        if (!sq.getStudentQualityContext().getDistanceMetric().isHardDistanceConflictsEnabled()) return false;
        if (e.getReservation() != null && e.getReservation().isAllowOverlap()) return false;
        if (s1 instanceof Section && e.getCourse() != null)
            for (SctAssignment s2: e.getAssignments())
                if (s2 instanceof Section && inConflict(sq, (Section)s1, (Section)s2)) return true;
        return false;
    }

    @Override
    public String toString() {
        return "DistanceConflict";
    }
}
