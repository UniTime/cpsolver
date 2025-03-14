package org.cpsolver.coursett.criteria.additional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cpsolver.coursett.constraint.JenrlConstraint;
import org.cpsolver.coursett.criteria.StudentConflict;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;

/**
 * An experimental criterion that tries to keep student all classes before or after the lunch period.
 * There is a conflict (penalized by Comparator.StudentOverLunchConflictWeight parameter) every time when
 * a student has two classes, one in the morning (starting before or at the lunch period) 
 * and one in the afternoon (starting after lunch period). When StudentConflict.OverLunchSameDayOnly is true,
 * only conflicts between classes that are on the same day are counted. The lunch period is defined by
 * StudentConflict.NoonSlot parameter (defaults to 144).
 * <br>
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2013 - 2014 Tomas Muller<br>
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
public class StudentOverLunchConflict extends StudentConflict {
    private int iNoonSlot = 144;
    private boolean iSameDay = true;
    
    @Override
    public void configure(DataProperties properties) {   
        super.configure(properties);
        iNoonSlot = properties.getPropertyInt("StudentConflict.NoonSlot", 144);
        iSameDay = properties.getPropertyBoolean("StudentConflict.OverLunchSameDayOnly", true);
    }
    
    /**
     * Are the two placements at the same day? True when the two placements share days and weeks.
     */
    public boolean shareDays(Placement p1, Placement p2) {
        return p1 != null && p2 != null && p1.getTimeLocation().shareDays(p2.getTimeLocation()) && p1.getTimeLocation().shareWeeks(p2.getTimeLocation());
    }
    
    /**
     * Is the given placement in the morning or in the afternoon?
     */
    public boolean isMorning(Placement placement) {
        return placement != null && placement.getTimeLocation().getStartSlot() <= iNoonSlot;
    }
    
    /**
     * There is a conflict when {@link StudentOverLunchConflict#isMorning(Placement)} differs for the two placements.
     * When parameter StudentConflict.OverLunchSameDayOnly is true, only conflicts that are on the same day
     * ({@link StudentOverLunchConflict#shareDays(Placement, Placement)} returns true) are counted.
     */
    @Override
    public boolean inConflict(Placement p1, Placement p2) {
        return p1 != null && p2 != null && isMorning(p1) != isMorning(p2) && (!iSameDay || shareDays(p1, p2));
    }
    
    @Override
    public boolean isApplicable(Lecture l1, Lecture l2) {
        return l1 != null && l2 != null && !ignore(l1, l2) && applicable(l1, l2);
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.StudentOverLunchConflictWeight", 0.1 * config.getPropertyDouble("Comparator.StudentConflictWeight", 1.0));
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.StudentOverLunchConflictWeight";
    }
    
    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info) {
        super.getInfo(assignment, info);
        double conf = getValue(assignment);
        if (conf > 0.0) {
            double total = 0;
            for (JenrlConstraint jenrl: ((TimetableModel)getModel()).getJenrlConstraints()) {
                if (!jenrl.isToBeIgnored()) {
                    total += jenrl.jenrl();
                }
            }
            info.put("Student over-lunch conflicts", getPerc(conf, 0.0, total) + "% (" + sDoubleFormat.format(conf) + " / " + sDoubleFormat.format(total) + ", weighted: " + sDoubleFormat.format(getWeightedValue(assignment)) + ")");
        }
    }
    
    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info, Collection<Lecture> variables) {
        super.getInfo(assignment, info, variables);
        double conf = getValue(assignment, variables);
        if (conf > 0.0) {
            Set<JenrlConstraint> jenrls = new HashSet<JenrlConstraint>();
            double total = 0;
            for (Lecture lecture: variables) {
                for (JenrlConstraint jenrl: lecture.jenrlConstraints())
                    if (jenrls.add(jenrl) && !jenrl.isToBeIgnored())
                        total += jenrl.jenrl();
            }
            info.put("Student over-lunch conflicts", getPerc(conf, 0.0, total) + "% (" + sDoubleFormat.format(conf) + ")");
        }
    }
}