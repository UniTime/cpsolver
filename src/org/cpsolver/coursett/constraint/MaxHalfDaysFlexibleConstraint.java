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

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.criteria.FlexibleConstraintCriterion;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.util.ToolBox;

/**
 * 
 * The MaxHalfDays constraint limits the number of half-days of week during which the given set of classes are taught.<br>
 * It has one parameter: a maximal number of week half-days during which the given set of classes can be placed.<br>
 * A day is split by noon (which can be changed using General.HalfDaySlot parameter). A class starting before noon is considered
 * a morning class (despite of its end), a class starting at noon or later is considered an afternoon class.<br>
 * Reference _MaxHalfDays:4_ translates to a maximum number of 4 half-days a week<br>
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2017 Tomas Muller<br>
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
public class MaxHalfDaysFlexibleConstraint extends FlexibleConstraint {
    private int iMaxHalfDays;
    private int iNoonSlot = 144;
    
    public MaxHalfDaysFlexibleConstraint(Long id, String owner, String preference, String reference) {
        super(id, owner, preference, reference);     

        Matcher matcher = Pattern.compile(FlexibleConstraintType.MAX_HALF_DAYS.getPattern()).matcher(reference);
        if (matcher.find()) {
            iMaxHalfDays = Integer.parseInt(matcher.group(2));
            iConstraintType = FlexibleConstraintType.MAX_HALF_DAYS;           
        }   
    }
    
    @Override
    public void setModel(Model<Lecture, Placement> model) {
        super.setModel(model);
        iNoonSlot = ((TimetableModel)model).getProperties().getPropertyInt("General.HalfDaySlot", iNoonSlot);
    }
    
    /**
     * Returns number of half-days in a day
     */
    protected int getNrHalfDays() {
        return 2;
    }
    
    /**
     * Returns index of the half day
     * @param time given time
     * @return 0 for morning, 1 for evening
     */
    protected int getHalfDay(TimeLocation time) {
        return (time.getStartSlot() < iNoonSlot ? 0 : 1);
    }
    
    @Override
    public double getNrViolations(Assignment<Lecture, Placement> assignment, Set<Placement> conflicts, HashMap<Lecture, Placement> assignments) {
        return ((MaxHalfDaysFlexibleConstraintContext)getContext(assignment)).nrViolations(assignments, conflicts);
    }

    @Override
    public void computeConflicts(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        if (!isHard()) return;
        
        MaxHalfDaysFlexibleConstraintContext context = (MaxHalfDaysFlexibleConstraintContext)getContext(assignment);
        while (context.nrHalfDays(value, conflicts) > iMaxHalfDays) {
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
        return ((MaxHalfDaysFlexibleConstraintContext)getContext(assignment)).nrHalfDays(value, null) > iMaxHalfDays;
    }
    
    @Override
    public FlexibleConstraintContext createAssignmentContext(Assignment<Lecture, Placement> assignment) {
        return new MaxHalfDaysFlexibleConstraintContext(assignment);
    }
    
    public class MaxHalfDaysFlexibleConstraintContext extends FlexibleConstraintContext {
        private Map<BitSet, Set<Lecture>[]> iHalfWeekDayAssignments = null;
        private Set<Lecture> iHalfDayAssignments[] = null;
        
        public MaxHalfDaysFlexibleConstraintContext(Assignment<Lecture, Placement> assignment) {
            super();
            for (BitSet week: getWeeks()) {
                Set<Lecture>[] halfDayAssignments = getHalfDayAssignments(week);
                for (Lecture variable: variables()) {
                    Placement value = assignment.getValue(variable);
                    if (value != null) {
                        for (int i = 0; i < Constants.DAY_CODES.length; i++)
                            if (hasDay(week, i, value))
                                halfDayAssignments[i * getNrHalfDays() + getHalfDay(value.getTimeLocation())].add(value.variable());
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

        protected boolean hasDay(BitSet week, int dayOfWeek, Placement value) {
            if (value == null || value.getTimeLocation() == null) return false;
            if (isPreciseDateComputation())
                return value.getTimeLocation().hasDate(dayOfWeek, week, getDayOfWeekOffset());
            if (week != null && !value.getTimeLocation().getWeekCode().intersects(week)) return false;
            return (value.getTimeLocation().getDayCode() & Constants.DAY_CODES[dayOfWeek]) != 0;
        }
        
        @SuppressWarnings("unchecked")
        protected Set<Lecture>[] getHalfDayAssignments(BitSet week) {
            if (week == null) {
                if (iHalfDayAssignments == null) {
                    iHalfDayAssignments = new Set[getNrHalfDays() * Constants.NR_DAYS];
                    for (int i = 0; i < iHalfDayAssignments.length; i++)
                        iHalfDayAssignments[i] = new HashSet<Lecture>();
                }
                return iHalfDayAssignments;
            } else {
                if (iHalfWeekDayAssignments == null)
                    iHalfWeekDayAssignments = new HashMap<BitSet, Set<Lecture>[]>();
                Set<Lecture>[] dayAssignments = iHalfWeekDayAssignments.get(week);
                if (dayAssignments == null) {
                    dayAssignments = new Set[getNrHalfDays() * Constants.NR_DAYS];
                    for (int i = 0; i < dayAssignments.length; i++)
                        dayAssignments[i] = new HashSet<Lecture>();
                    iHalfWeekDayAssignments.put(week, dayAssignments);
                }
                return dayAssignments;
            }
        }

        @Override
        public void assigned(Assignment<Lecture, Placement> assignment, Placement value) {
            for (BitSet week: getWeeks()) {
                Set<Lecture>[] halfDayAssignments = getHalfDayAssignments(week);
                for (int i = 0; i < Constants.DAY_CODES.length; i++)
                    if (hasDay(week, i, value))
                        halfDayAssignments[i * getNrHalfDays() + getHalfDay(value.getTimeLocation())].add(value.variable());
            }
            updateCriterion(assignment);
        }
        
        @Override
        public void unassigned(Assignment<Lecture, Placement> assignment, Placement value) {
            for (BitSet week: getWeeks()) {
                Set<Lecture>[] halfDayAssignments = getHalfDayAssignments(week);
                for (int i = 0; i < Constants.DAY_CODES.length; i++)
                    if (hasDay(week, i, value))
                        halfDayAssignments[i * getNrHalfDays() + getHalfDay(value.getTimeLocation())].remove(value.variable());
            }
            updateCriterion(assignment);
        }
        
        public int nrHalfDays(BitSet week, Placement value, Set<Placement> conflicts) {
            Set<Lecture>[] halfDayAssignments = getHalfDayAssignments(week);
            int ret = 0;
            for (int i = 0; i < Constants.DAY_CODES.length; i++) {
                for (int j = 0; j < getNrHalfDays(); j++) {
                    int idx = i * getNrHalfDays() + j;
                    int cnt = halfDayAssignments[idx].size();
                    if (value != null) {
                        if (hasDay(week, i, value) && j == getHalfDay(value.getTimeLocation())) cnt ++;
                        if (halfDayAssignments[idx].contains(value.variable())) cnt --; 
                    }
                    if (conflicts != null) {
                        for (Placement conflict: conflicts) {
                            if (value != null && conflict.variable().equals(value.variable())) continue;
                            if (halfDayAssignments[idx].contains(conflict.variable())) cnt --;
                        }
                    }
                    if (cnt > 0) ret++;                    
                }
            }
            return ret;
        }
        
        public int nrHalfDays(Placement value, Set<Placement> conflicts) {
            int ret = 0;
            for (BitSet week: getWeeks()) {
                int days = nrHalfDays(week, value, conflicts);
                if (days > ret) ret = days;
            }
            return ret;
        }
        
        public Set<Lecture> candidates(Placement value, Set<Placement> conflicts) {
            for (BitSet week: getWeeks()) {
                Set<Lecture>[] halfDayAssignments = getHalfDayAssignments(week);
                int bestCnt = 0;
                int nrHalfDays = 0;
                List<Integer> bestHalfDays = new ArrayList<Integer>();
                for (int i = 0; i < Constants.DAY_CODES.length; i++) {
                    for (int j = 0; j < getNrHalfDays(); j++) {
                        int idx = i * getNrHalfDays() + j;
                        if (hasDay(week, i, value) && j == getHalfDay(value.getTimeLocation())) {
                            nrHalfDays ++;
                            continue;
                        }
                        int cnt = halfDayAssignments[idx].size();
                        if (halfDayAssignments[idx].contains(value.variable())) cnt --;
                        for (Placement conflict: conflicts) {
                            if (conflict.variable().equals(value.variable())) continue;
                            if (halfDayAssignments[idx].contains(conflict.variable())) cnt --;
                        }
                        if (cnt <= 0) continue;
                        nrHalfDays ++;
                        if (bestHalfDays.isEmpty() || bestCnt > cnt) {
                            bestHalfDays.clear(); bestHalfDays.add(idx); bestCnt = cnt;
                        } else if (bestCnt == cnt) {
                            bestHalfDays.add(idx);
                        }
                    }
                }
                if (!bestHalfDays.isEmpty() && nrHalfDays > iMaxHalfDays) return halfDayAssignments[ToolBox.random(bestHalfDays)];
            }
            return null;
        }
        
        public int nrViolations(BitSet week,HashMap<Lecture, Placement> assignments, Set<Placement> conflicts) {
            Set<Lecture>[] halfDayAssignments = getHalfDayAssignments(week);
            int halfDays = 0;
            for (int i = 0; i < Constants.DAY_CODES.length; i++) {
                for (int j = 0; j < getNrHalfDays(); j++) {
                    int idx = i * getNrHalfDays() + j;
                    int cnt = halfDayAssignments[idx].size();
                    if (assignments != null) {
                        for (Map.Entry<Lecture, Placement> entry: assignments.entrySet()) {
                            Placement assignment = entry.getValue();
                            if (assignment != null && hasDay(week, i, assignment) && j == getHalfDay(assignment.getTimeLocation())) cnt ++;
                        }
                    }
                    if (conflicts != null)
                        for (Placement conflict: conflicts) {
                            if (assignments != null && assignments.containsKey(conflict.variable())) continue;
                            if (halfDayAssignments[idx].contains(conflict.variable())) cnt --;
                        }
                    if (cnt > 0) halfDays ++;
                }
            }
            return (halfDays <= iMaxHalfDays ? 0 : halfDays - iMaxHalfDays);
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