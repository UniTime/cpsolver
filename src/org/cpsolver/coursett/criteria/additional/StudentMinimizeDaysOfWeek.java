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
 * Naive, yet effective approach for minimizing number of days in student schedule. This criterion
 * is based on {@link StudentConflict} and it penalizes all cases where a student has
 * two classes taught on different days.
 * These penalties are weighted by Comparator.MinimizeStudentScheduleDaysWeight,
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
public class StudentMinimizeDaysOfWeek extends StudentConflict {
    
    /**
     * Time distance between two placements in the number of days. The classes must be on the weeks.
     * If the two classes are taught on multiple days during the week, the
     * distance only count the different days.
     * @param p1 first placement
     * @param p2 second placement
     * @return number of different days
     */
    public static double dayDistance(Placement p1, Placement p2) {
        if (p1 == null || p2 == null) return 0;
        return dayDistance(p1.getTimeLocation(), p2.getTimeLocation());
    }
    
    private static double dayDistance(TimeLocation t1, TimeLocation t2) {
        if (!t1.shareWeeks(t2)) return 0.0;
        return 1.0 - ((double)t1.nrSharedDays(t2)) / Math.min(t1.getNrMeetings(), t2.getNrMeetings());
    }
    
    @Override
    public boolean inConflict(Placement p1, Placement p2) {
        return dayDistance(p1, p2) > 0.0;
    }
    
    @Override
    protected double jointEnrollment(JenrlConstraint jenrl, Placement p1, Placement p2) {
        return dayDistance(p1, p2) * jenrl.jenrl();
    }
    
    @Override
    protected double jointEnrollment(JenrlConstraint jenrl) {
        double max = 0;
        for (TimeLocation t1: jenrl.first().timeLocations())
            for (TimeLocation t2: jenrl.second().timeLocations()) {
                double distance = dayDistance(t1, t2);
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
            inc(assignment, studentWeight * dayDistance(assignment.getValue(jenrl.first()), assignment.getValue(jenrl.second())));
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.MinimizeStudentScheduleDaysWeight", 0.05 * config.getPropertyDouble("Comparator.StudentConflictWeight", 1.0));
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.MinimizeStudentScheduleDaysWeight";
    }

    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info) {
        super.getInfo(assignment, info);
        double total = 0;
        for (JenrlConstraint jenrl: ((TimetableModel)getModel()).getJenrlConstraints()) {
            if (!jenrl.isToBeIgnored())
                total += jenrl.jenrl();
        }
        info.put("Student different days", sDoubleFormat.format(getValue(assignment) / total));
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
                    distance += jenrl.jenrl() * dayDistance(assignment.getValue(jenrl.first()), assignment.getValue(jenrl.second()));
                    total += jenrl.jenrl();
                }
        }
        info.put("Student different days", sDoubleFormat.format(distance / total));
    }

}