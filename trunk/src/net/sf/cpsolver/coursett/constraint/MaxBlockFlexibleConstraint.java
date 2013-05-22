package net.sf.cpsolver.coursett.constraint;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.cpsolver.coursett.Constants;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * 
 * The MaxBlock constraint checks for too big blocks of back-to-back classes of an instructor.<br>
 * It has two parameters: a maximal length of a back-to-back block that is allowed and a minimal length of a break between two classes not to be considered in the same block.<br>
 * Reference _MaxBlock:120:30_ translates to a maximal block of at most 2 hours (120 minutes) with classes not more than 30 minutes a part.<br>
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2013 Matej Lukac<br>
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
public class MaxBlockFlexibleConstraint extends FlexibleConstraint {

    // max number of slots between to classes to be considered Back-To-Back
    private int iMaxBreakBetweenBTB; 
    // max length of a block of classes taught Back-To-Back
    private int iMaxBlockSlotsBTB;      
    
    /**
     * 
     * @param owner identifier of distribution preference the constraint was created for
     * @param preference time preference ("R" for required, "P" for prohibited, "-2",
     *            "-1", "1", "2" for soft preference)   
     * @param reference parameters of the constraint in String form            
     */
    public MaxBlockFlexibleConstraint(Long id, String owner, String preference, String reference) {
        super(id, owner, preference, reference);     
        
        Pattern pattern = null;
        Matcher matcher = null;   
        
        // recognize Break constraint        
        String patternString = "_(MaxBlock):([0-9]+):([0-9]+)_";
        pattern = Pattern.compile(patternString);
        matcher = pattern.matcher(reference);
        if (matcher.find()) {       
            iMaxBlockSlotsBTB = Integer.parseInt(matcher.group(2))/Constants.SLOT_LENGTH_MIN;
            iMaxBreakBetweenBTB = Integer.parseInt(matcher.group(3))/Constants.SLOT_LENGTH_MIN;             
            iConstraintType = FlexibleConstraint.FlexibleConstraintType.MAXBLOCK_BACKTOBACK;           
        }  
         
    }
    
    @Override
    public void computeConflicts(Placement value, Set<Placement> conflicts) {
        if (!isHard())
            return;       
        
        List<BitSet> weeks = getWeeks();
        
        // constraint is checked for every day in week
        for (int dayCode : Constants.DAY_CODES) {
            // constraint is checked for every week in semester (or for the whole semester)
            for (BitSet week : weeks) {
                boolean isProblem = false;
                do {
                    isProblem = false;
                    // each blocks contains placements which are BTB 
                    List<Block> blocks = getBlocks(dayCode, conflicts, value, null, week);
                    for (Block block : blocks) {
                        // if the block is not affected by the current placement, continue
                        if (!block.getPlacements().contains(value)){
                            continue;
                        }
                        Set<Placement> adepts = new HashSet<Placement>();                        
                        // if there is only 1 placement in block, the block cannot be shortened
                        // if placements of a block start at the same time, they intersect
                        // this solves problem when between 2 classes is required MEET_TOGETHER  
                        if (block.getNbrPlacements() == 1 || block.haveSameStartTime())
                            continue;
                        // if block is longer than maximum size, some of its placements are conflicts
                        if (block.getLengthInSlots() > iMaxBlockSlotsBTB) {
                            // classes from block are potential conflicts
                            adepts.addAll(block.getPlacements());                            
                            // do not set currently assigned value as conflict
                            adepts.remove(value);
                            isProblem = true;
                            // pick random placement
                            Placement conflict = ToolBox.random(adepts);
                            if (conflict != null) {
                                conflicts.add(conflict);
                            }
                        }
                    }
                } while (isProblem);
            }
        }
    }
    
    public List<Block> getBlocks(int dayCode, Set<Placement> conflicts, Placement value, HashMap<Lecture, Placement> assignments, BitSet week) {     
        List<Placement> toBeSorted = new ArrayList<Placement>(getRelevantPlacements(dayCode, conflicts, value, assignments, week));
        Collections.sort(toBeSorted, new PlacementTimeComparator());  
        
        return mergeToBlocks(toBeSorted, iMaxBreakBetweenBTB);
    } 

    @Override
    public double getNrViolations(Set<Placement> conflicts, HashMap<Lecture, Placement> assignments) {
        List<BitSet> weeks = getWeeks();

        int violatedBlocks = 0;
        for (int dayCode : Constants.DAY_CODES) {
            for (BitSet week : weeks) {
                List<Block> blocks = getBlocks(dayCode, null, null, assignments, week);
                for (Block block : blocks) {
                    if (block.getNbrPlacements() == 1 || block.haveSameStartTime())
                        continue;
                    // violated if there is a block containing more than one
                    // class longer than iMaxBlockSlotsBTB
                    if (block.getLengthInSlots() > iMaxBlockSlotsBTB) {
                        int blockLengthPenalty = block.getLengthInSlots() / iMaxBlockSlotsBTB;
                        violatedBlocks += blockLengthPenalty;
                    }
                }
            }
        }
        return violatedBlocks;
    }

}
