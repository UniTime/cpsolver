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
 * The MaxBreaks constraint limits the number of blocks of non back-to-back classes of an instructor on a day.<br>
 * It has two parameters: a maximal number of breaks and a minimal length of a break between two classes not to be considered in the same block.<br>
 * Reference _MaxBreaks:1:30_ translates to a maximum number of one break (two blocks) on a day of classes not more than 30 minutes a part.<br>
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
public class MaxBreaksFlexibleConstraint extends FlexibleConstraint implements WeakeningConstraint<Lecture, Placement> {
    private int iMaxBreakBetweenBTB;
    protected int iMaxBlocksOnADay;
    
    public MaxBreaksFlexibleConstraint(Long id, String owner, String preference, String reference) {
        super(id, owner, preference, reference);     

        Matcher matcher = Pattern.compile(FlexibleConstraintType.MAX_BREAKS.getPattern()).matcher(reference);
        if (matcher.find()) {                
            iMaxBlocksOnADay = 1 + Integer.parseInt(matcher.group(2));
            iMaxBreakBetweenBTB = Integer.parseInt(matcher.group(3)) / Constants.SLOT_LENGTH_MIN;
            iConstraintType = FlexibleConstraintType.MAX_BREAKS;           
        }   
    }

    public List<Block> getBlocks(Assignment<Lecture, Placement> assignment, int dayCode, Set<Placement> conflicts, Placement value, HashMap<Lecture, Placement> assignments, BitSet week) {     
        List<Placement> toBeSorted = new ArrayList<Placement>(getRelevantPlacements(assignment, dayCode, conflicts, value, assignments, week));
        Collections.sort(toBeSorted, new PlacementTimeComparator());  
        
        return mergeToBlocks(toBeSorted, iMaxBreakBetweenBTB);
    } 

    @Override
    public double getNrViolations(Assignment<Lecture, Placement> assignment, Set<Placement> conflicts, HashMap<Lecture, Placement> assignments) {
        int penalty = 0;
        // constraint is checked for every day in week
        for (int dayCode : Constants.DAY_CODES) {
            // constraint is checked for every week in semester (or for the whole semester)
            for (BitSet week : getWeeks()) {
                // each blocks contains placements which are BTB
                List<Block> blocks = getBlocks(assignment, dayCode, null, null, assignments, week);
                // too many blocks -> increase penalty
                if (blocks.size() > iMaxBlocksOnADay)
                    penalty += (blocks.size() - iMaxBlocksOnADay) * (blocks.size() - iMaxBlocksOnADay);
            }
        }
        return penalty;
    }

    @Override
    public void computeConflicts(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        if (!isHard()) return;
        
        if (((MaxBreaksFlexibleConstraintContext)getContext(assignment)).isWeak(value)) {
            for (Lecture v: variables())
                if (assignment.getValue(v) == null && !v.equals(value.variable())) {
                    // incomplete and week -- do not check for conflicts just yet
                    return;
                }
        }
        
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
                // each blocks contains placements which are BTB
                List<Block> blocks = getBlocks(assignment, dayCode, conflicts, value, null, week);
                while (blocks.size() > iMaxBlocksOnADay) {
                    // too many blocks -> identify adepts for unassignment
                    List<Block> adepts = new ArrayList<Block>(); int size = 0;
                    for (Block block: blocks) {
                        if (block.getPlacements().contains(value)) continue; // skip block containing given value
                        // select adepts of the smallest size
                        if (adepts.isEmpty() || size > block.getPlacements().size()) {
                            adepts.clear();
                            adepts.add(block);
                            size = block.getPlacements().size();
                        } else if (size == block.getPlacements().size()) {
                            adepts.add(block);
                        }
                    }
                    
                    // pick one randomly
                    Block block = ToolBox.random(adepts);
                    blocks.remove(block);
                    for (Placement conflict: block.getPlacements())
                        if (conflict.equals(assignment.getValue(conflict.variable())))
                            conflicts.add(conflict);
                }
            }
        }
    }

    @Override
    public void weaken(Assignment<Lecture, Placement> assignment) {
    }

    @Override
    public void weaken(Assignment<Lecture, Placement> assignment, Placement value) {
        if (isHard())
            ((MaxBreaksFlexibleConstraintContext)getContext(assignment)).weaken(value);
    }
    
    @Override
    public FlexibleConstraintContext createAssignmentContext(Assignment<Lecture, Placement> assignment) {
        return new MaxBreaksFlexibleConstraintContext(assignment);
    }

    public class MaxBreaksFlexibleConstraintContext extends FlexibleConstraintContext {
        private Map<Lecture, Placement> iWeakAssignment = new HashMap<Lecture, Placement>();
        
        public MaxBreaksFlexibleConstraintContext(Assignment<Lecture, Placement> assignment) {
            super(assignment);
        }

        @Override
        public void assigned(Assignment<Lecture, Placement> assignment, Placement value) {
            super.assigned(assignment, value);
            if (isHard())
                iWeakAssignment.remove(value.variable());
        }
        
        public void weaken(Placement value) {
            if (isHard())
                iWeakAssignment.put(value.variable(), value);
        }
        
        public boolean isWeak(Placement value) {
            return value.equals(iWeakAssignment.get(value.variable()));
        }
    }
    
}
