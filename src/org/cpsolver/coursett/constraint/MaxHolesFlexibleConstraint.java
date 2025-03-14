package org.cpsolver.coursett.constraint;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.WeakeningConstraint;
import org.cpsolver.ifs.util.ToolBox;


/**
 * 
 * The MaxHoles constraint limits the number of free time (holes) for an instructor on a day.<br>
 * It has one parameter: a maximal amount of free time (holes) that an instructor have on a day in minutes.<br>
 * Reference _MaxHoles:120_ translates to a maximum number of two hours on a day (between the first and the last class on a day).<br>
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2013 - 2017 Tomas Muller<br>
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
public class MaxHolesFlexibleConstraint extends FlexibleConstraint implements WeakeningConstraint<Lecture, Placement> {
    private int iMaxHolesOnADay = 288;
    
    public MaxHolesFlexibleConstraint(Long id, String owner, String preference, String reference) {
        super(id, owner, preference, reference);     

        Matcher matcher = Pattern.compile(FlexibleConstraintType.MAX_HOLES.getPattern()).matcher(reference);
        if (matcher.find()) {                
            iMaxHolesOnADay = Integer.parseInt(matcher.group(2)) / Constants.SLOT_LENGTH_MIN;
            iConstraintType = FlexibleConstraintType.MAX_HOLES;           
        }
    }
    
    /**
     * Count number of holes (free slots) between the given classes on given day and week.
     * @param assignment current assignment
     * @param dayCode representation of days in week combination
     * @param conflicts placements to be unassigned
     * @param value placement to be assigned
     * @param assignments placements of variables
     * @param week bitset representing a date pattern
     * @return number of holes (free slots) that are over the limit
     */
    public int countHoles(Assignment<Lecture, Placement> assignment, int dayCode, Set<Placement> conflicts, Placement value, HashMap<Lecture, Placement> assignments, BitSet week) {
        List<Placement> placements = new ArrayList<Placement>(getRelevantPlacements(assignment, dayCode, conflicts, value, assignments, week));
        Collections.sort(placements, new PlacementTimeComparator());
        
        int lastSlot = -1;
        int holes = 0;
        for (Placement placement: placements) {
            if (lastSlot >= 0 && placement.getTimeLocation().getStartSlot() > lastSlot) {
                holes += (placement.getTimeLocation().getStartSlot() - lastSlot);
            }
            lastSlot = Math.max(lastSlot, placement.getTimeLocation().getStartSlot() + placement.getTimeLocation().getLength());
        }
        return holes;
    }

    /**
     * Count violations, that is weekly average free time that is over the limit in hours.
     * @param assignment current assignment
     * @param conflicts placements to be unassigned
     * @param assignments placements of variables
     * @return weekly average free time that is over the limit in hours
     */
    @Override
    public double getNrViolations(Assignment<Lecture, Placement> assignment, Set<Placement> conflicts, HashMap<Lecture, Placement> assignments) {
        double penalty = 0;
        // constraint is checked for every day in week
        for (int dayCode : Constants.DAY_CODES) {
            // constraint is checked for every week in semester (or for the whole semester)
            for (BitSet week : getWeeks()) {
                // count holes in the week and day
                int holes = countHoles(assignment, dayCode, conflicts, null, assignments, week);
                if (holes > iMaxHolesOnADay)
                    penalty += (holes - iMaxHolesOnADay);
            }
        }
        // return average holes in a week, in hours
        return penalty / (12.0 * getWeeks().size());
    }

    @Override
    public void computeConflicts(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        if (!isHard()) return;
        
        MaxHolesFlexibleConstraintContext context = (MaxHolesFlexibleConstraintContext)getContext(assignment);
        
        // constraint is checked for every day in week
        for (int dayCode : Constants.DAY_CODES) {
            if ((value.getTimeLocation().getDayCode() & dayCode) == 0) continue; // ignore other days
            // constraint is checked for every week in semester (or for the whole semester)
            for (BitSet week : getWeeks()) {
                if (isPreciseDateComputation()) {
                    if (!value.getTimeLocation().overlaps(dayCode, week, getDayOfWeekOffset())) continue;
                } else {
                    if (week != null && !week.intersects(value.getTimeLocation().getWeekCode())) continue; // ignore other weeks
                }
                // check penalty
                int penalty = countHoles(assignment, dayCode, conflicts, value, null, week);
                while (penalty > context.getMaxHoles(dayCode, week)) {
                    // too many holes -> identify adepts for unassignment
                    List<Placement> adepts = new ArrayList<Placement>();
                    for (Placement placement: getRelevantPlacements(assignment, dayCode, conflicts, value, null, week)) {
                        if (placement.equals(value)) continue; // skip given value
                        // check if removing placement would improve the penalty
                        HashMap<Lecture, Placement> assignments = new HashMap<Lecture, Placement>(); assignments.put(placement.variable(), null);
                        int newPenalty = countHoles(assignment, dayCode, conflicts, value, assignments, week);
                        if (newPenalty <= penalty)
                            adepts.add(placement);
                    }
                    
                    // no adepts -> fail
                    if (adepts.isEmpty()) {
                        conflicts.add(value); return;
                    }
                    
                    // pick one randomly
                    conflicts.add(ToolBox.random(adepts));
                    penalty = countHoles(assignment, dayCode, conflicts, value, null, week);
                }
            }
        }
    }

    @Override
    public void weaken(Assignment<Lecture, Placement> assignment) {
    }
    
    @Override
    public boolean isConsistent(Placement value1, Placement value2) {
        // there is no way to check without trying to put other placements in between
        return true;
    }

    @Override
    public void weaken(Assignment<Lecture, Placement> assignment, Placement value) {
        if (isHard())
            ((MaxHolesFlexibleConstraintContext)getContext(assignment)).weaken(assignment, value);
    }
    
    @Override
    public FlexibleConstraintContext createAssignmentContext(Assignment<Lecture, Placement> assignment) {
        return new MaxHolesFlexibleConstraintContext(assignment);
    }

    public class MaxHolesFlexibleConstraintContext extends FlexibleConstraintContext {
        private Map<Integer, Map<BitSet, Integer>> iMaxHoles = new HashMap<Integer, Map<BitSet, Integer>>();
        
        public MaxHolesFlexibleConstraintContext(Assignment<Lecture, Placement> assignment) {
            super(assignment);
        }

        public void weaken(Assignment<Lecture, Placement> assignment, Placement value) {
            if (!isHard()) return;
            for (int dayCode : Constants.DAY_CODES) {
                if ((value.getTimeLocation().getDayCode() & dayCode) == 0) continue; // ignore other days
                for (BitSet week : getWeeks()) {
                    if (week != null && !week.intersects(value.getTimeLocation().getWeekCode())) continue; // ignore other weeks
                    int penalty = countHoles(assignment, dayCode, null, value, null, week);
                    if (penalty > iMaxHolesOnADay) {
                        Map<BitSet, Integer> holes = iMaxHoles.get(dayCode);
                        if (holes == null) {
                            holes = new HashMap<BitSet, Integer>();
                            iMaxHoles.put(dayCode, holes);
                        }
                        Integer oldPenalty = holes.get(week);
                        if (oldPenalty != null && oldPenalty >= penalty) continue;
                        holes.put(week, penalty);
                    }
                }
            }
        }
        
        public int getMaxHoles(int dayCode, BitSet week) {
            Map<BitSet, Integer> holes = iMaxHoles.get(dayCode);
            if (holes == null) return iMaxHolesOnADay;
            Integer penalty = holes.get(week);
            if (penalty == null || penalty < iMaxHolesOnADay) return iMaxHolesOnADay;
            return penalty;
        }
    }
    
}
