package org.cpsolver.coursett.criteria.additional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cpsolver.coursett.constraint.JenrlConstraint;
import org.cpsolver.coursett.criteria.StudentConflict;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.Student;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;

/**
 * Naive, yet effective approach for minimizing holes in student schedule. This criterion
 * is based on {@link StudentConflict} and it penalizes all cases where a student has
 * two classes taught on the same day that are not back-to-back. The penalisation
 * is based on the time distance between the two classes, computed in hours.
 * These penalties are weighted by Comparator.MinimizeStudentScheduleHolesWeight,
 * which defaults to 1/20 of the Comparator.StudentConflictWeight.
 * 
 * <br>
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
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
public class StudentMinimizeScheduleHoles extends StudentConflict {
    
    /**
     * Time distance between two placements in hours. The classes must be on the same days and weeks,
     * but not overlapping in time. If the two classes are taught on multiple days during the week, the
     * distance is also multiplied by the number of shared days of week.
     * @param p1 first placement
     * @param p2 second placement
     * @return distance between the two classes in hours
     */
    public static double btbDistance(Placement p1, Placement p2) {
        if (p1 == null || p2 == null) return 0.0;
        return btbDistance(p1.getTimeLocation(), p2.getTimeLocation());
    }
    
    private static double btbDistance(TimeLocation t1, TimeLocation t2) {
        if (!t1.shareDays(t2) || !t1.shareWeeks(t2) || t1.shareHours(t2)) return 0.0;
        int s1 = t1.getStartSlot(), e1 = s1 + t1.getLength();
        int s2 = t2.getStartSlot(), e2 = s2 + t2.getLength();
        if (e1 < s2) {
            return t1.nrSharedDays(t2) * (s2 - e1) / 12.0;
        } else if (e2 < s1) {
            return t1.nrSharedDays(t2) * (s1 - e2) / 12.0;
        }
        return 0.0;        
    }
    
    @Override
    public boolean inConflict(Placement p1, Placement p2) {
        return btbDistance(p1, p2) > 0.0;
    }
    
    @Override
    protected double jointEnrollment(JenrlConstraint jenrl, Placement p1, Placement p2) {
        return btbDistance(p1, p2) * jenrl.jenrl();
    }
    
    @Override
    protected double jointEnrollment(JenrlConstraint jenrl) {
        double max = 0;
        for (TimeLocation t1: jenrl.first().timeLocations())
            for (TimeLocation t2: jenrl.second().timeLocations()) {
                double distance = btbDistance(t1, t2);
                if (distance > max) max = distance;
            }
        return max;
    }
    
    @Override
    public boolean isApplicable(Lecture l1, Lecture l2) {
        return l1 != null && l2 != null && !ignore(l1, l2) && applicable(l1, l2);
    }
    
    @Override
    public void incJenrl(Assignment<Lecture, Placement> assignment, JenrlConstraint jenrl, double studentWeight, Double conflictPriority, Student student) {
        if (isApplicable(student, jenrl.first(), jenrl.second()))
            inc(assignment, studentWeight * btbDistance(assignment.getValue(jenrl.first()), assignment.getValue(jenrl.second())));
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.MinimizeStudentScheduleHolesWeight", 0.05 * config.getPropertyDouble("Comparator.StudentConflictWeight", 1.0));
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.MinimizeStudentScheduleHolesWeight";
    }

    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info) {
        super.getInfo(assignment, info);
        double total = 0;
        for (JenrlConstraint jenrl: ((TimetableModel)getModel()).getJenrlConstraints()) {
            if (!jenrl.isToBeIgnored())
                total += jenrl.jenrl();
        }
        info.put("Student class distance", sDoubleFormat.format(60.0 * getValue(assignment) / total) + " minutes");
    }
    
    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info, Collection<Lecture> variables) {
        super.getInfo(assignment, info, variables);
        Set<JenrlConstraint> jenrls = new HashSet<JenrlConstraint>();
        double distance = 0;
        double total = 0;
        for (Lecture lecture: variables) {
            for (JenrlConstraint jenrl: lecture.jenrlConstraints())
                if (jenrls.add(jenrl) && !jenrl.isToBeIgnored()) {
                    distance += jenrl.jenrl() * btbDistance(assignment.getValue(jenrl.first()), assignment.getValue(jenrl.second()));
                    total += jenrl.jenrl();
                }
        }
        info.put("Student class distance", sDoubleFormat.format(60.0 * distance / total) + " minutes");
    }

}