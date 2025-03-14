package org.cpsolver.coursett.constraint;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.criteria.FlexibleConstraintCriterion;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;
import org.cpsolver.ifs.criteria.Criterion;


/**
 * Flexible constraint. <br>
 * This constraint expresses relations between several classes. Provides similar
 * functions as Group constraint. Unlike group constraint, Flexible constraint
 * is able to parse some of its parameters from its reference field<br>
 * 
 * @author  Matej Lukac
 * @version CourseTT 1.3 (University Course Timetabling)<br>
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
public abstract class FlexibleConstraint extends ConstraintWithContext<Lecture, Placement, FlexibleConstraint.FlexibleConstraintContext> {

    protected int iPreference;
    private boolean iIsRequired;   
    private String iOwner;
    
    // Constraint type
    protected FlexibleConstraintType iConstraintType = null;
        
    // A pattern the constraint was created from
    protected String iReference = "";   
    
    // Determines whether the constraint is checked for every week in the semester    
    protected List<BitSet> iWeeks = null;
    protected Integer iDayOfWeekOffset = null;
    protected Boolean iPreciseDateComputation = null;
    
    /**
     * Flexible constraint types
     * 
     */
    public static enum FlexibleConstraintType {
        /**
         * Given classes must be taught in a way there is a break between two blocks of classes. 
         */
        MAXBLOCK_BACKTOBACK("_(MaxBlock):([0-9]+):([0-9]+)_", MaxBlockFlexibleConstraint.class, "Block"),
        /**
         * There must be a break of a given length in a given time interval.
         */
        BREAK("_(Break):([0-9]+):([0-9]+):([0-9]+)_", BreakFlexibleConstraint.class, "Break"),
        /**
         * Limit number of breaks between adjacent classes on a day.
         */
        MAX_BREAKS("_(MaxBreaks):([0-9]+):([0-9]+)_", MaxBreaksFlexibleConstraint.class, "MaxBreaks"),
        /**
         * Limit number of weeks on which an a class can take place.
         */
        MAX_WEEKS("_(MaxWeeks):([0-9]+):([0-9]+)_", MaxWeeksFlexibleConstraint.class, "MaxWeeks"),
        /**
         * Limit number of days of a week. 
         */
        MAX_DAYS("_(MaxDays):([0-9]+)_", MaxDaysFlexibleConstraint.class, "MaxDays"),
        /**
         * Minimize free time of an instructor during a day (between the first and the last class).
         */
        MAX_HOLES("_(MaxHoles):([0-9]+)_", MaxHolesFlexibleConstraint.class, "MaxHoles"),
        /**
         * Limit number of half-days of a week. 
         */
        MAX_HALF_DAYS("_(MaxHalfDays):([0-9]+)_", MaxHalfDaysFlexibleConstraint.class, "MaxHalfDays"),
        /**
         * Limit number of consecutive days of a week. 
         */
        MAX_CONSECUTIVE_DAYS("_(MaxConsDays):([0-9]+)_", MaxConsecutiveDaysFlexibleConstraint.class, "MaxConsDays"),
        ;
        
        private String iPattern;
        private Class<? extends FlexibleConstraint> iImplementation;
        private String iName;
        FlexibleConstraintType(String pattern, Class<? extends FlexibleConstraint> implementation, String name) {
            iPattern = pattern; iImplementation = implementation; iName = name;
        }
        
        public String getPattern() { return iPattern; }
        
        public String getName() { return iName.replaceAll("(?<=[^A-Z])([A-Z])"," $1"); }

        public FlexibleConstraint create(Long id, String owner, String preference, String reference) throws IllegalArgumentException {
            try {
                return iImplementation.getConstructor(Long.class, String.class, String.class, String.class).newInstance(id, owner, preference, reference);
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
    }
    
    /**
     * Constructor
     * @param id unique id
     * @param owner identifier of distribution preference the constraint was created for
     * @param preference time preference ("R" for required, "P" for prohibited, "-2",
     *            "-1", "1", "2" for soft preference)   
     * @param reference parameters of the constraint in String form            
     */
    public FlexibleConstraint(Long id, String owner, String preference, String reference){
        super();                
        iId = id;
        iReference = reference;
        iPreference = Constants.preference2preferenceLevel(preference);
        iIsRequired = preference.equals(Constants.sPreferenceRequired);        
        iOwner = owner;                
    }
    

    /** 
     * Return current number of violations.
     * @param assignment current assignment
     * @param conflicts conflicting placements to be unassigned
     * @param assignments assigned placements 
     * @return the number of violations of the constraint during days and all weeks of the semester
     */
    public abstract double getNrViolations(Assignment<Lecture, Placement> assignment, Set<Placement> conflicts, HashMap<Lecture, Placement> assignments);

    
    /**
     * Return weeks of the term.
     * @return a list of bitsets (one for each week of the term) representing datePatterns or null if semester is whole semester is considered
     */
    public List<BitSet> getWeeks(){
        if (iWeeks == null){
            TimetableModel model = (TimetableModel) getModel();
            iWeeks = new ArrayList<BitSet>();

            boolean checkWeeks = model.getProperties().getPropertyBoolean("FlexibleConstraint.CheckWeeks", false);
            
            if (checkWeeks) {
                // get weeks method returns bitsets representing weeks during semester
                iWeeks = model.getWeeks();
            } else {
                // weeks are not considered, all placements are taken into consideration
                iWeeks.add(null);
            } 
        }  
          
        return iWeeks;
    }
    
    public int getDayOfWeekOffset() {
        if (iDayOfWeekOffset == null) {
            TimetableModel model = (TimetableModel) getModel();
            iDayOfWeekOffset = model.getProperties().getPropertyInt("DatePattern.DayOfWeekOffset", 0);
        }
        return iDayOfWeekOffset;
    }
    
    public boolean isPreciseDateComputation() {
        if (iPreciseDateComputation == null) {
            TimetableModel model = (TimetableModel) getModel();
            iPreciseDateComputation = model.getProperties().getPropertyBoolean("FlexibleConstraint.PreciseDateComputation", false);
        }
        return iPreciseDateComputation;
    }
    
    @Override
    public boolean isConsistent(Placement value1, Placement value2) {
        HashMap<Lecture, Placement> assignments = new HashMap<Lecture, Placement>();
        if (value1 != null)
            assignments.put(value1.variable(), value1);
        if (value2 != null)
            assignments.put(value2.variable(), value2);
        
        if (getNrViolations(null, null, assignments) != 0) return false;

        return super.isConsistent(value1, value2);
    }
    
    /**
     * Returns placements of variables assigned to this constraint with assignment which satisfy following conditions:
     * They must be taught in the day included in dayCode.
     * They cannot be included in conflicts
     * Their date pattern intersects with week
     * 
     * @param assignment current assignment
     * @param dayCode representation of days in week combination
     * @param conflicts placements to be unassigned
     * @param value placement to be assigned
     * @param assignments placements of variables
     * @param week bitset representing a date pattern
     * @return placements of variables assigned to this constraint with assignment which satisfy conditions above
     */
    protected Set<Placement> getRelevantPlacements(Assignment<Lecture, Placement> assignment, int dayCode, Set<Placement> conflicts, Placement value,
            HashMap<Lecture, Placement> assignments, BitSet week) {
        Set<Placement> placements = new HashSet<Placement>();
        
        for (Lecture lecture : variables()) {
            // lecture of the value is already assigned
            if(value != null && lecture.equals(value.variable()))continue;

            // lecture might not have assignment if it is present in assignments
            if (assignments != null && assignments.containsKey(lecture)) {
                Placement placement = assignments.get(lecture);
                if (placement != null && shareWeeksAndDay(placement.getTimeLocation(), week, dayCode))
                    placements.add(placement);
            } else if (assignment != null) {
                Placement placement = assignment.getValue(lecture);
                if (placement != null && (conflicts == null || !conflicts.contains(placement)) && shareWeeksAndDay(placement.getTimeLocation(), week, dayCode))
                    placements.add(placement);
            }
        }

        if (value == null || (conflicts != null && conflicts.contains(value))) {
            return placements;
        } 
        
        if (shareWeeksAndDay(value.getTimeLocation(), week, dayCode)) placements.add(value); 

        return placements;
    }
    
    /**
     * Used to determine the daycode and week of a timelocation
     * 
     * @param t timelocation 
     * @param week date pattern compared to timelocation
     * @param dayCode days compared to timelocation
     * @return true if TimeLocation matches the date pattern and days
     */
    protected boolean shareWeeksAndDay(TimeLocation t, BitSet week, int dayCode){
        if (isPreciseDateComputation()) return t.overlaps(dayCode, week, dayCode);
        
        boolean matchDay = (t.getDayCode() & dayCode) != 0;
        boolean matchWeek = (week == null || t.shareWeeks(week));                
        return matchDay && matchWeek;
    }
    
    /**
     * Creates a list of blocks from a placements sorted by startSlot
     * 
     * @param sorted list of placements sorted by startSlot
     * @param maxBreakBetweenBTB maximum number of free slots between BTB placements
     * @return groups of BTB placements as a list of blocks
     */
    protected List<Block> mergeToBlocks(List<Placement> sorted, int maxBreakBetweenBTB){
        List<Block> blocks = new ArrayList<Block>();
        // add placements to blocks
        for (int i = 0; i < sorted.size(); i++) {
            Placement placement = sorted.get(i);
            boolean added = false;
            // add placement to a suitable block
            for (int j = 0; j < blocks.size(); j++) {
                if (blocks.get(j).addPlacement(placement)) {
                    added = true;
                }
            }
            // create a new block if a lecture does not fit into any block
            if (!added) {
                Block block = new Block(maxBreakBetweenBTB);
                block.addPlacement(placement);
                blocks.add(block);
            }
        }   
        return blocks;
    }
    
    @Override
    public boolean isHard() {
        return iIsRequired;
    }    
    
    @Override
    public String getName() {
        return iOwner + ": " + iConstraintType.getName();
    }
    
    public FlexibleConstraintType getType(){
        return iConstraintType;
    }
    
    public String getReference() {        
        return iReference;
    }
    
    public String getOwner() {        
        return iOwner;
    }   
    
    /**
     * Prolog reference: "R" for required, "P" for prohibited", "-2",.."2" for
     * preference
     * @return prolog preference
     */
    public String getPrologPreference() {
        return Constants.preferenceLevel2preference(iPreference);
    }

    /**
     * Return the current preference of the flexible constraint, considering conflicts and new assignments.
     * Used to compute value for flexible constraint criterion.
     * @param assignment current assignment
     * @param conflicts conflicting assignment
     * @param assignments proposed assignments
     * @return the current preference of the flexible constraint
     */
    public double getCurrentPreference(Assignment<Lecture, Placement> assignment, Set<Placement> conflicts, HashMap<Lecture, Placement> assignments){
        if (isHard()) return 0;
        double pref = getNrViolations(assignment, conflicts, assignments);
        if(pref == 0){
            return - Math.abs(iPreference);
        }
        return Math.abs(iPreference) * pref;
    }

    /**
     * A block is a list of placements sorted by startSlot, which are BTB.
     * maxSlotsBetweenBackToBack determines the number of free slots between two BTB placements
     *
     */
    public class Block {
        
        // start slot of the block
        private int startSlotCurrentBlock = -1;        
        // end slot of the block
        private int endSlotCurrentBlock = -1;        
        // max number of slots between classes to be considered Back-To-Back; 4 slots default      
        private int maxSlotsBetweenBackToBack = 4;
        // the list of placements of this block
        private List<Placement> placements = new ArrayList<Placement>();
        
        public Block(int maxSlotsBetweenBTB){
            this.maxSlotsBetweenBackToBack = maxSlotsBetweenBTB;            
        }              
        
        /**
         * Adds placement to the block and updates block's attributes.
         * 
         * @param placement placement to be added to the block
         * @return true if the placement was successfully added to the block
         */
        public boolean addPlacement(Placement placement){   
            if (placement == null){
                return false;
            }
            
            TimeLocation t = placement.getTimeLocation();
            
            if (t == null){
                return false;
            }
            
            // if placements is empty, the block only contains currently added placement -> set start and end
            if (placements.isEmpty()){
                placements.add(placement);
                startSlotCurrentBlock = t.getStartSlot();
                endSlotCurrentBlock = t.getStartSlot() + t.getLength();
                return true;
            }
            
            // if startSlotCurrentBlock equals placement's start slot, add placement and adjust endSlotCurrentBlock
            if (t.getStartSlot() == startSlotCurrentBlock){
                placements.add(placement);
                int tEnd = t.getStartSlot() + t.getLength();
                if (tEnd > endSlotCurrentBlock){
                    endSlotCurrentBlock = tEnd;
                }
                return true;
            }      
            
            // if placement starts among endSlotCurrentBlock + modifier and startSlotCurrentBlock, add the placement
            if (endSlotCurrentBlock + maxSlotsBetweenBackToBack >= t.getStartSlot() && t.getStartSlot() >= startSlotCurrentBlock ){
                placements.add(placement);
                int tEnd = t.getStartSlot() + t.getLength();
                if (tEnd > endSlotCurrentBlock){
                    endSlotCurrentBlock = tEnd;
                }
                return true;
            }
            
            return false;
        }

        public boolean haveSameStartTime() {
            if (!placements.isEmpty()) {
                int startSlot = placements.get(0).getTimeLocation().getStartSlot();
                for (Placement p : getPlacements()) {
                    if (p.getTimeLocation().getStartSlot() != startSlot)
                        return false;
                }
            }
            return true;
        }
        
        public int getStartSlotCurrentBlock(){
            return startSlotCurrentBlock;
        }
        
        public int getEndSlotCurrentBlock(){
            return endSlotCurrentBlock;
        }
        
        public int getNbrPlacements(){
            return placements.size();
        }        
       
        public List<Placement> getPlacements(){
            return placements;
        }
        
        public int getLengthInSlots(){
            return endSlotCurrentBlock - startSlotCurrentBlock;
        }
        
        @Override
        public String toString(){
            return "[" + startSlotCurrentBlock + ", " + endSlotCurrentBlock + "]" + " PlacementsNbr: "+ getNbrPlacements();
        }          
    }
    
    /**
     * Placement comparator: earlier placement first, shorter placement first if both start at the same time.
     */
    protected static class PlacementTimeComparator implements Comparator<Placement> {
        @Override
        public int compare(Placement p1, Placement p2) {
            TimeLocation t1 = p1.getTimeLocation(), t2 = p2.getTimeLocation();
            // compare by start time (earlier first)
            if (t1.getStartSlot() < t2.getStartSlot())
                return -1;
            if (t1.getStartSlot() > t2.getStartSlot())
                return 1;
            // same start -> compare by length (shorter first)
            if (t1.getLength() < t2.getLength())
                return -1;
            if (t1.getLength() > t2.getLength())
                return 1;
            // fallback
            return 0;
        }
    }
    
    @Override
    public String toString() {
        return getName();
    }
    
    @Override
    public FlexibleConstraintContext createAssignmentContext(Assignment<Lecture, Placement> assignment) {
        return new FlexibleConstraintContext(assignment);
    }
    
    public class FlexibleConstraintContext implements AssignmentConstraintContext<Lecture, Placement> {
        protected double iLastPreference = 0;
        
        FlexibleConstraintContext() {}
        
        FlexibleConstraintContext(Assignment<Lecture, Placement> assignment) {
            updateCriterion(assignment);
        }

        @Override
        public void assigned(Assignment<Lecture, Placement> assignment, Placement value) {
            updateCriterion(assignment);
        }

        @Override
        public void unassigned(Assignment<Lecture, Placement> assignment, Placement value) {
            updateCriterion(assignment);
        }

        /**
         * Update value of FlexibleConstraintCriterion and number of violated FlexibleConstraints
         */
        protected void updateCriterion(Assignment<Lecture, Placement> assignment) {
            if (!isHard()) {
                Criterion<Lecture, Placement> criterion = getModel().getCriterion(FlexibleConstraintCriterion.class);
                if (criterion != null) {
                    criterion.inc(assignment, -iLastPreference);                
                    iLastPreference = getCurrentPreference(assignment, null, null);
                    criterion.inc(assignment, iLastPreference);  
                }
            }
        }
        
        public double getPreference() {
            return iLastPreference;
        }
    }
}
