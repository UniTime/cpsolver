package org.cpsolver.coursett.constraint;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.criteria.FlexibleConstraintCriterion;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.util.ToolBox;

/**
 * 
 * The MaxConsecutiveDays constraint limits the number of consecutive days of week during which the given set of classes are taught.<br>
 * It has one parameter: a maximal number of consecutive days during which the given set of classes can be placed.<br>
 * Reference _MaxConsDays:2_ translates to a maximum number of 2 consecutive days a week (so for instance, Monday &amp; Tuesday,
 * Tuesday &amp; Wednesday, Wednesday &amp; Thursday, ...)<br>
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2022 Tomas Muller<br>
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
public class MaxConsecutiveDaysFlexibleConstraint extends FlexibleConstraint {
private int iMaxDays;
    
    public MaxConsecutiveDaysFlexibleConstraint(Long id, String owner, String preference, String reference) {
        super(id, owner, preference, reference);     

        Matcher matcher = Pattern.compile(FlexibleConstraintType.MAX_CONSECUTIVE_DAYS.getPattern()).matcher(reference);
        if (matcher.find()) {
            iMaxDays = Integer.parseInt(matcher.group(2));
            iConstraintType = FlexibleConstraintType.MAX_CONSECUTIVE_DAYS;           
        }
    }
    
    @Override
    public double getNrViolations(Assignment<Lecture, Placement> assignment, Set<Placement> conflicts, HashMap<Lecture, Placement> assignments) {
        return ((MaxConsecutiveDaysFlexibleConstraintContext)getContext(assignment)).nrViolations(assignments, conflicts);
    }

    @Override
    public void computeConflicts(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        if (!isHard()) return;
        
        MaxConsecutiveDaysFlexibleConstraintContext context = (MaxConsecutiveDaysFlexibleConstraintContext)getContext(assignment);
        while (context.nrDays(value, conflicts) > iMaxDays && !conflicts.contains(value)) {
            Set<Lecture> candidates = context.candidates(value, conflicts);
            if (candidates == null) {
                conflicts.add(value);
                return;
            }
            for (Lecture candidate: candidates) {
                Placement conflict = assignment.getValue(candidate);
                if (conflict != null)
                    conflicts.add(conflict);
            }
        }
    }
    
    @Override
    public boolean inConflict(Assignment<Lecture, Placement> assignment, Placement value) {
        if (!isHard()) return false;
        return ((MaxConsecutiveDaysFlexibleConstraintContext)getContext(assignment)).nrDays(value, null) > iMaxDays;
    }
    
    @Override
    public FlexibleConstraintContext createAssignmentContext(Assignment<Lecture, Placement> assignment) {
        return new MaxConsecutiveDaysFlexibleConstraintContext(assignment);
    }
    
    public class MaxConsecutiveDaysFlexibleConstraintContext extends FlexibleConstraintContext {
        private Map<BitSet, Set<Lecture>[]> iWeekDayAssignments = null;
        private Set<Lecture> iDayAssignments[] = null;
        
        public MaxConsecutiveDaysFlexibleConstraintContext(Assignment<Lecture, Placement> assignment) {
            super();
            for (BitSet week: getWeeks()) {
                Set<Lecture>[] dayAssignments = getDayAssignments(week);
                for (Lecture variable: variables()) {
                    Placement value = assignment.getValue(variable);
                    if (value != null) {
                        for (int i = 0; i < dayAssignments.length; i++)
                            if (overlaps(week, i, value))
                                dayAssignments[i].add(value.variable());
                    }
                }
            }
            if (!isHard()) {
                Criterion<Lecture, Placement> criterion = getModel().getCriterion(FlexibleConstraintCriterion.class);
                if (criterion != null) {
                    double pref = nrViolations(null, null);
                    if (pref == 0)
                        iLastPreference = -Math.abs(iPreference);
                    else 
                        iLastPreference = Math.abs(iPreference) * pref;
                    criterion.inc(assignment, iLastPreference);  
                }
            }
        }
        
        protected boolean overlaps(BitSet week, int dayOfWeek, Placement value) {
            if (value == null || value.getTimeLocation() == null) return false;
            if (isPreciseDateComputation())
                return value.getTimeLocation().hasDate(dayOfWeek, week, getDayOfWeekOffset());
            if (week != null && !value.getTimeLocation().getWeekCode().intersects(week)) return false;
            return (value.getTimeLocation().getDayCode() & Constants.DAY_CODES[dayOfWeek]) != 0;
        }
        
        @SuppressWarnings("unchecked")
        protected Set<Lecture>[] getDayAssignments(BitSet week) {
            if (week == null) {
                if (iDayAssignments == null) {
                    iDayAssignments = new Set[Constants.NR_DAYS];
                    for (int i = 0; i < iDayAssignments.length; i++)
                        iDayAssignments[i] = new HashSet<Lecture>();
                }
                return iDayAssignments;
            } else {
                if (iWeekDayAssignments == null)
                    iWeekDayAssignments = new HashMap<BitSet, Set<Lecture>[]>();
                Set<Lecture>[] dayAssignments = iWeekDayAssignments.get(week);
                if (dayAssignments == null) {
                    dayAssignments = new Set[Constants.NR_DAYS];
                    for (int i = 0; i < dayAssignments.length; i++)
                        dayAssignments[i] = new HashSet<Lecture>();
                    iWeekDayAssignments.put(week, dayAssignments);
                }
                return dayAssignments;
            }
        }

        @Override
        public void assigned(Assignment<Lecture, Placement> assignment, Placement value) {
            for (BitSet week: getWeeks()) {
                Set<Lecture>[] dayAssignments = getDayAssignments(week);
                for (int i = 0; i < dayAssignments.length; i++)
                    if (overlaps(week, i, value))
                        dayAssignments[i].add(value.variable());
            }
            updateCriterion(assignment);
        }
        
        @Override
        public void unassigned(Assignment<Lecture, Placement> assignment, Placement value) {
            for (BitSet week: getWeeks()) {
                Set<Lecture>[] dayAssignments = getDayAssignments(week);
                for (int i = 0; i < dayAssignments.length; i++)
                    if (overlaps(week, i, value))
                        dayAssignments[i].remove(value.variable());
            }
            updateCriterion(assignment);
        }
        
        public int nrDays(BitSet week, Placement value, Set<Placement> conflicts) {
            Set<Lecture>[] dayAssignments = getDayAssignments(week);
            Integer min = null, max = null;
            for (int i = 0; i < dayAssignments.length; i++) {
                int cnt = dayAssignments[i].size();
                if (value != null) {
                    if (overlaps(week, i, value)) cnt ++;
                    if (dayAssignments[i].contains(value.variable())) cnt --; 
                }
                if (conflicts != null) {
                    for (Placement conflict: conflicts) {
                        if (value != null && conflict.variable().equals(value.variable())) continue;
                        if (dayAssignments[i].contains(conflict.variable())) cnt --;
                    }
                }
                if (cnt > 0) {
                    if (min == null) min = i;
                    max = i;
                }
            }
            if (min == null) return 0;
            return max - min + 1;
        }
        
        public int nrDays(Placement value, Set<Placement> conflicts) {
            int ret = 0; 
            for (BitSet week: getWeeks()) {
                int days = nrDays(week, value, conflicts);
                if (days > ret) ret = days;
            }
            return ret;
        }
        
        public Set<Lecture> candidates(Placement value, Set<Placement> conflicts) {
            for (BitSet week: getWeeks()) {
                Set<Lecture>[] dayAssignments = getDayAssignments(week);
                Integer min = null, max = null;
                Integer minThisPl = null, maxThisPl = null;
                for (int i = 0; i < dayAssignments.length; i++) {
                    int cnt = dayAssignments[i].size();
                    if (overlaps(week, i, value)) {
                        if (minThisPl == null) minThisPl = i;
                        maxThisPl = i;
                        cnt ++;
                    }
                    if (dayAssignments[i].contains(value.variable())) cnt --;
                    for (Placement conflict: conflicts) {
                        if (conflict.variable().equals(value.variable())) continue;
                        if (dayAssignments[i].contains(conflict.variable())) cnt --;
                    }
                    if (cnt > 0) {
                        if (min == null) min = i;
                        max = i;
                    }
                }
                if (min == null || max - min + 1 <= iMaxDays) continue;
                if ((minThisPl == null || min < minThisPl) && (maxThisPl == null || maxThisPl < max)) {
                    if (ToolBox.random() <= 0.5)
                        return dayAssignments[min];
                    else
                        return dayAssignments[max];
                }
                if (minThisPl == null || min < minThisPl)
                    return dayAssignments[min];
                if (maxThisPl == null || maxThisPl < max)
                    return dayAssignments[max];
            }
            return null;
        }
        
        public int nrViolations(BitSet week, HashMap<Lecture, Placement> assignments, Set<Placement> conflicts) {
            Set<Lecture>[] dayAssignments = getDayAssignments(week);
            Integer min = null, max = null;
            for (int i = 0; i < dayAssignments.length; i++) {
                int cnt = dayAssignments[i].size();
                if (assignments != null) {
                    for (Map.Entry<Lecture, Placement> entry: assignments.entrySet()) {
                        Placement assignment = entry.getValue();
                        if (assignment != null && overlaps(week, i, assignment)) cnt ++;
                    } 
                }
                if (conflicts != null) {
                    for (Placement conflict: conflicts) {
                        if (assignments != null && assignments.containsKey(conflict.variable())) continue;
                        if (dayAssignments[i].contains(conflict.variable())) cnt --;
                    }
                }
                if (cnt > 0) {
                    if (min == null) min = i;
                    max = i;
                }
            }
            if (min == null) return 0;
            return (max - min + 1 <= iMaxDays ? 0 : max - min + 1 - iMaxDays);
        }
        
        public int nrViolations(HashMap<Lecture, Placement> assignments, Set<Placement> conflicts) {
            int ret = 0; 
            for (BitSet week: getWeeks()) {
                ret += nrViolations(week, assignments, conflicts);
            }
            return ret;
        }
    
    }
}
