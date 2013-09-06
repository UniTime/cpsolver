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
 * The Break constraint checks for instructor lunch break or a break in general in between the given classes.<br>
 * It has three parameters: a start and an end time of a window in which the break is required / preferred, and a minimal length of a break that is needed.<br>
 * Reference _Break:132:162:30_ translates to a break of at least 30 minutes between 11 am (slot 132) and 1:30 pm (slot 162).<br>
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
public class BreakFlexibleConstraint extends FlexibleConstraint {
    
    // LunchBreak constraint parameters
    private int iBreakStart;
    private int iBreakEnd;
    private int iBreakLength;

    /**
     * 
     * @param owner identifier of distribution preference the constraint was created for
     * @param preference time preference ("R" for required, "P" for prohibited, "-2",
     *            "-1", "1", "2" for soft preference)   
     * @param reference parameters of the constraint in String form            
     */
    public BreakFlexibleConstraint(Long id, String owner, String preference, String reference) {
        super(id, owner, preference, reference);     
        
        Pattern pattern = null;
        Matcher matcher = null;   
        
        // recognize Break constraint        
        String patternString = "_(Break):([0-9]+):([0-9]+):([0-9]+)_";
        pattern = Pattern.compile(patternString);
        matcher = pattern.matcher(reference);
        if (matcher.find()) {                
            iBreakStart = Integer.parseInt(matcher.group(2));
            iBreakEnd = Integer.parseInt(matcher.group(3)); 
            iBreakLength = Integer.parseInt(matcher.group(4))/Constants.SLOT_LENGTH_MIN; 
            iConstraintType = FlexibleConstraintType.BREAK;           
        }   
    }

    @Override
    public void computeConflicts(Placement value, Set<Placement> conflicts) {
        if (!isHard())
            return;        
        
        List<BitSet> weeks = getWeeks();
       
        // checks only placements in the break time
        if (value.getTimeLocation().getStartSlot() <= iBreakEnd
                && value.getTimeLocation().getStartSlot() + value.getTimeLocation().getLength() > iBreakStart) {
            
            for (int dayCode : Constants.DAY_CODES) {
                // checks only days affected by the placement
                if ((value.getTimeLocation().getDayCode() & dayCode) != 0) {             
                    // constraint is checked for every week in semester (or for the whole semester)
                    for (BitSet week : weeks) {
                        boolean isProblem = false;
                        do {
                            Set<Placement> adepts = new HashSet<Placement>();
                            // each blocks contains placements which are BTB
                            // placements are BTB if there is less time between them than the minimal break length
                            List<Block> blocks = getBreakBlocks(dayCode, conflicts, value, null, week);
                            // determine possible conflicts from blocks' placements
                            getAdeptsLunchBreak(blocks, adepts);
                            if (adepts.isEmpty())
                                isProblem = false;
                            // currently assigned value shouldn't be added to conflicts if possible 
                            if (adepts.size() >= 2)
                                adepts.remove(value);
                            // pick random placement
                            Placement conflict = ToolBox.random(adepts);
                            if (conflict != null) {
                                conflicts.add(conflict);
                            }
                        } while (isProblem);
                    }   
                }
            }            
        }
    }  
    
    /**
     * Creates a list of consecutive blocks with back-to-back classes.
     */
    public List<Block> getBreakBlocks(int dayCode, Set<Placement> conflicts, Placement value, HashMap<Lecture, Placement> assignments, BitSet week) {     
        
        List<Placement> toBeSorted = new ArrayList<Placement>(getRelevantPlacements(dayCode, conflicts, value, assignments, week));
        Collections.sort(toBeSorted, new PlacementTimeComparator());           
             
        return mergeToBlocks(toBeSorted, iBreakLength);
    }     
    
    
    
    /**
     * Method adds Placements from blocks to adepts if there is a possibility, that the placement caused constraint violation
     * 
     * @param blocks placements in 
     * @param adepts
     */
    private void getAdeptsLunchBreak(List<Block> blocks, Set<Placement> adepts) {
        List<Block> matchingBlocks = new ArrayList<Block>();
        for(Block block: blocks){
            // if block intersects with break interval, it will be used in conflict selection
            if (block.getStartSlotCurrentBlock() <= iBreakEnd && block.getEndSlotCurrentBlock() >= iBreakStart) matchingBlocks.add(block);          
        }
        int size = matchingBlocks.size();
        // if there is none block intersecting with break interval, the constraint is satisfied
        // if there are at least 2 blocks intersecting with break interval, the constraint is satisfied, 
        // because there must be a space between them, otherwise they would be in one block
        // if there is only one block intersecting with break interval, constraint might not be satisfied
        if (size == 1) {
            Block block = matchingBlocks.get(0);
            // check whether the block leaves enough space for break
            if (block.getStartSlotCurrentBlock() - iBreakStart >= iBreakLength || iBreakEnd - block.getEndSlotCurrentBlock() >= iBreakLength){
                return;
            // if it doesn't
            }else{
                // every placement intersecting with break interval might be potential conflict
                for (Placement p: block.getPlacements()){
                    if ( p.getTimeLocation().getStartSlot() <= iBreakEnd && p.getTimeLocation().getStartSlot()+ p.getTimeLocation().getLength() >= iBreakStart){
                        adepts.add(p);
                    }
                }                
            }
        }       
    }
    
    @Override
    public double getNrViolations(Set<Placement> conflicts, HashMap<Lecture, Placement> assignments){
        List<BitSet> weeks = getWeeks();

        int violatedDays = 0;
        for (int dayCode : Constants.DAY_CODES) {
            weekIteration: for (BitSet week : weeks) {
                Set<Placement> adepts = new HashSet<Placement>();
                List<Block> blocks = getBreakBlocks(dayCode, null, null, assignments, week);
                getAdeptsLunchBreak(blocks, adepts);
                if (!adepts.isEmpty())
                    violatedDays++;
                break weekIteration;
            }
        }
        return violatedDays;
    }
    
}
