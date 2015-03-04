package org.cpsolver.coursett.constraint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
 * The MaxDays constraint limits the number of days of week during which the given set of classes are taught.<br>
 * It has one parameter: a maximal number of week days during which the given set of classes can be placed.<br>
 * Reference _MaxDays:2_ translates to a maximum number of 2 days a week<br>
 * 
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2015 Tomas Muller<br>
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
public class MaxDaysFlexibleConstraint extends FlexibleConstraint {
    private int iMaxDays;
    
    public MaxDaysFlexibleConstraint(Long id, String owner, String preference, String reference) {
        super(id, owner, preference, reference);     

        Matcher matcher = Pattern.compile(FlexibleConstraintType.MAX_DAYS.getPattern()).matcher(reference);
        if (matcher.find()) {
            iMaxDays = Integer.parseInt(matcher.group(2));
            iConstraintType = FlexibleConstraintType.MAX_DAYS;           
        }   
    }
    
    @Override
    public double getNrViolations(Assignment<Lecture, Placement> assignment, Set<Placement> conflicts, HashMap<Lecture, Placement> assignments) {
        return ((MaxDaysFlexibleConstraintContext)getContext(assignment)).nrViolations(assignments, conflicts);
    }

    @Override
    public void computeConflicts(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        if (!isHard()) return;
        
        MaxDaysFlexibleConstraintContext context = (MaxDaysFlexibleConstraintContext)getContext(assignment);
        while (context.nrDays(value, conflicts) > iMaxDays) {
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
        if (!isHard()) return false;
        return ((MaxDaysFlexibleConstraintContext)getContext(assignment)).nrDays(value, null) > iMaxDays;
    }
    
    @Override
    public FlexibleConstraintContext createAssignmentContext(Assignment<Lecture, Placement> assignment) {
        return new MaxDaysFlexibleConstraintContext(assignment);
    }
    
    public class MaxDaysFlexibleConstraintContext extends FlexibleConstraintContext {
        private Set<Lecture> iDayAssignments[] = null;
        
        @SuppressWarnings("unchecked")
        public MaxDaysFlexibleConstraintContext(Assignment<Lecture, Placement> assignment) {
            super();
            iDayAssignments = new Set[Constants.NR_DAYS];
            for (int i = 0; i < iDayAssignments.length; i++)
                iDayAssignments[i] = new HashSet<Lecture>();
            
            for (Lecture variable: variables()) {
                Placement value = assignment.getValue(variable);
                if (value != null) {
                    for (int i = 0; i < iDayAssignments.length; i++)
                        if ((value.getTimeLocation().getDayCode() & Constants.DAY_CODES[i]) != 0)
                            iDayAssignments[i].add(value.variable());
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
            for (int i = 0; i < iDayAssignments.length; i++)
                if ((value.getTimeLocation().getDayCode() & Constants.DAY_CODES[i]) != 0)
                    iDayAssignments[i].add(value.variable());
            updateCriterion(assignment);
        }
        
        @Override
        public void unassigned(Assignment<Lecture, Placement> assignment, Placement value) {
            for (int i = 0; i < iDayAssignments.length; i++)
                if ((value.getTimeLocation().getDayCode() & Constants.DAY_CODES[i]) != 0)
                    iDayAssignments[i].remove(value.variable());
            updateCriterion(assignment);
        }
        
        public int nrDays(Placement value, Set<Placement> conflicts) {
            int ret = 0;
            for (int i = 0; i < iDayAssignments.length; i++) {
                int cnt = iDayAssignments[i].size();
                if (value != null) {
                    if ((value.getTimeLocation().getDayCode() & Constants.DAY_CODES[i]) != 0) cnt ++;
                    if (iDayAssignments[i].contains(value.variable())) cnt --; 
                }
                if (conflicts != null) {
                    for (Placement conflict: conflicts) {
                        if (value != null && conflict.variable().equals(value.variable())) continue;
                        if (iDayAssignments[i].contains(conflict.variable())) cnt --;
                    }
                }
                if (cnt > 0) ret++;
            }
            return ret;
        }
        
        public Set<Lecture> candidates(Placement value, Set<Placement> conflicts) {
            int bestCnt = 0;
            List<Integer> bestWeeks = new ArrayList<Integer>();
            for (int i = 0; i < iDayAssignments.length; i++) {
                if ((value.getTimeLocation().getDayCode() & Constants.DAY_CODES[i]) != 0) continue;
                int cnt = iDayAssignments[i].size();
                if (iDayAssignments[i].contains(value.variable())) cnt --;
                for (Placement conflict: conflicts) {
                    if (conflict.variable().equals(value.variable())) continue;
                    if (iDayAssignments[i].contains(conflict.variable())) cnt --;
                }
                if (cnt <= 0) continue;
                if (bestWeeks.isEmpty() || bestCnt > cnt) {
                    bestWeeks.clear(); bestWeeks.add(i); bestCnt = cnt;
                } else if (bestCnt == cnt) {
                    bestWeeks.add(i);
                }
            }
            return bestWeeks.isEmpty() ? null : iDayAssignments[ToolBox.random(bestWeeks)];
        }
        
        public int nrViolations(HashMap<Lecture, Placement> assignments, Set<Placement> conflicts) {
            int days = 0;
            for (int i = 0; i < iDayAssignments.length; i++) {
                int cnt = iDayAssignments[i].size();
                if (assignments != null) {
                    for (Map.Entry<Lecture, Placement> entry: assignments.entrySet()) {
                        Placement assignment = entry.getValue();
                        if (assignment != null && (assignment.getTimeLocation().getDayCode() & Constants.DAY_CODES[i]) != 0) cnt ++;
                    }
                }
                if (conflicts != null)
                    for (Placement conflict: conflicts) {
                        if (assignments != null && assignments.containsKey(conflict.variable())) continue;
                        if (iDayAssignments[i].contains(conflict.variable())) cnt --;
                    }
                if (cnt > 0) days ++;
            }
            return (days <= iMaxDays ? 0 : days - iMaxDays);
        }
    
    }
}