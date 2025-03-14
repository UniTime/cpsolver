package org.cpsolver.coursett.constraint;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cpsolver.coursett.criteria.FlexibleConstraintCriterion;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.util.ToolBox;

/**
 * 
 * The MaxWeeks constraint limits the number of weeks during which the given set of classes are taught.<br>
 * It has two parameters: a maximal number of weeks during which the given set of classes can be placed
 * and a day code indicating what days of week are considered.<br>
 * Reference _MaxWeeks:3:6_ translates to a maximum number of 3 weeks, but only for classes that are placed on Fridays and Saturdays
 * (64 for Monday, 32 for Tuesday, 16 for Wednesday, 8 for Thursday, 4 for Friday, 2 for Saturday, and 1 for Sunday).
 * If the second parameter is zero, all days of week are considered.<br>
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2013 - 2014 Tomas Muller<br>
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
public class MaxWeeksFlexibleConstraint extends FlexibleConstraint {
    private int iMaxWeeks;
    private int iDayCode;
    
    public MaxWeeksFlexibleConstraint(Long id, String owner, String preference, String reference) {
        super(id, owner, preference, reference);     

        Matcher matcher = Pattern.compile(FlexibleConstraintType.MAX_WEEKS.getPattern()).matcher(reference);
        if (matcher.find()) {
            iMaxWeeks = Integer.parseInt(matcher.group(2));
            iDayCode = Integer.parseInt(matcher.group(3));
            iConstraintType = FlexibleConstraintType.MAX_WEEKS;           
        }   
    }
    
    public boolean isCorectDayOfWeek(Placement value) {
        return value != null && value.getTimeLocation() != null && (iDayCode == 0 || (iDayCode & value.getTimeLocation().getDayCode()) != 0);
    }

    @Override
    public double getNrViolations(Assignment<Lecture, Placement> assignment, Set<Placement> conflicts, HashMap<Lecture, Placement> assignments) {
        return ((MaxWeeksFlexibleConstraintContext)getContext(assignment)).nrViolations(assignments, conflicts);
    }

    @Override
    public void computeConflicts(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        if (!isHard() || !isCorectDayOfWeek(value)) return;
        
        MaxWeeksFlexibleConstraintContext context = (MaxWeeksFlexibleConstraintContext)getContext(assignment);
        while (context.nrWeeks(value, conflicts) > iMaxWeeks) {
            Set<Lecture> candidates = context.candidates(value, conflicts);
            if (candidates == null) return;
            for (Lecture candidate: candidates) {
                Placement conflict = assignment.getValue(candidate);
                if (conflict != null)
                    conflicts.add(conflict);
            }
        }
    }
    
    @Override
    public boolean inConflict(Assignment<Lecture, Placement> assignment, Placement value) {
        if (!isHard() || !isCorectDayOfWeek(value)) return false;
        return ((MaxWeeksFlexibleConstraintContext)getContext(assignment)).nrWeeks(value, null) > iMaxWeeks;
    }
    
    @Override
    public FlexibleConstraintContext createAssignmentContext(Assignment<Lecture, Placement> assignment) {
        return new MaxWeeksFlexibleConstraintContext(assignment);
    }

    public class MaxWeeksFlexibleConstraintContext extends FlexibleConstraintContext {
        private List<BitSet> iWeeks = null;
        private Set<Lecture> iWeekAssignments[] = null;
        
        @SuppressWarnings("unchecked")
        public MaxWeeksFlexibleConstraintContext(Assignment<Lecture, Placement> assignment) {
            super();
            iWeeks = ((TimetableModel)getModel()).getWeeks();
            iWeekAssignments = new Set[iWeeks.size()];
            for (int i = 0; i < iWeekAssignments.length; i++)
                iWeekAssignments[i] = new HashSet<Lecture>();
            
            for (Lecture variable: variables()) {
                Placement value = assignment.getValue(variable);
                if (value != null && isCorectDayOfWeek(value)) {
                    for (int i = 0; i < iWeeks.size(); i++)
                        if (value.getTimeLocation().shareWeeks(iWeeks.get(i)))
                            iWeekAssignments[i].add(value.variable());
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

        @Override
        public void assigned(Assignment<Lecture, Placement> assignment, Placement value) {
            if (isCorectDayOfWeek(value)) {
                for (int i = 0; i < iWeeks.size(); i++)
                    if (value.getTimeLocation().shareWeeks(iWeeks.get(i)))
                        iWeekAssignments[i].add(value.variable());
                updateCriterion(assignment);
            }
        }
        
        @Override
        public void unassigned(Assignment<Lecture, Placement> assignment, Placement value) {
            if (isCorectDayOfWeek(value)) {
                for (int i = 0; i < iWeeks.size(); i++)
                    if (value.getTimeLocation().shareWeeks(iWeeks.get(i)))
                        iWeekAssignments[i].remove(value.variable());
                updateCriterion(assignment);
            }
        }
        
        public int nrWeeks(Placement value, Set<Placement> conflicts) {
            int ret = 0;
            for (int i = 0; i < iWeeks.size(); i++) {
                BitSet w = iWeeks.get(i);
                int cnt = iWeekAssignments[i].size();
                if (value != null) {
                    if (value.getTimeLocation().shareWeeks(w)) cnt ++;
                    if (iWeekAssignments[i].contains(value.variable())) cnt --; 
                }
                if (conflicts != null) {
                    for (Placement conflict: conflicts) {
                        if (value != null && conflict.variable().equals(value.variable())) continue;
                        if (iWeekAssignments[i].contains(conflict.variable())) cnt --;
                    }
                }
                if (cnt > 0) ret++;
            }
            return ret;
        }
        
        public Set<Lecture> candidates(Placement value, Set<Placement> conflicts) {
            int bestCnt = 0;
            List<Integer> bestWeeks = new ArrayList<Integer>();
            for (int i = 0; i < iWeeks.size(); i++) {
                BitSet w = iWeeks.get(i);
                if (value.getTimeLocation().shareWeeks(w)) continue;
                int cnt = iWeekAssignments[i].size();
                if (iWeekAssignments[i].contains(value.variable())) cnt --;
                for (Placement conflict: conflicts) {
                    if (conflict.variable().equals(value.variable())) continue;
                    if (iWeekAssignments[i].contains(conflict.variable())) cnt --;
                }
                if (cnt <= 0) continue;
                if (bestWeeks.isEmpty() || bestCnt > cnt) {
                    bestWeeks.clear(); bestWeeks.add(i); bestCnt = cnt;
                } else if (bestCnt == cnt) {
                    bestWeeks.add(i);
                }
            }
            return bestWeeks.isEmpty() ? null : iWeekAssignments[ToolBox.random(bestWeeks)];
        }
        
        public int nrViolations(HashMap<Lecture, Placement> assignments, Set<Placement> conflicts) {
            int weeks = 0;
            for (int i = 0; i < iWeeks.size(); i++) {
                BitSet w = iWeeks.get(i);
                int cnt = iWeekAssignments[i].size();
                if (assignments != null) {
                    for (Map.Entry<Lecture, Placement> entry: assignments.entrySet()) {
                        if (isCorectDayOfWeek(entry.getValue()) && entry.getValue().getTimeLocation().shareWeeks(w)) cnt ++;
                        if (iWeekAssignments[i].contains(entry.getKey())) cnt --; 
                    }
                }
                if (conflicts != null)
                    for (Placement conflict: conflicts) {
                        if (assignments != null && assignments.containsKey(conflict.variable())) continue;
                        if (iWeekAssignments[i].contains(conflict.variable())) cnt --;
                    }
                if (cnt > 0) weeks ++;
            }
            return (weeks <= iMaxWeeks ? 0 : weeks - iMaxWeeks);
        }
    
    }
}
