package org.cpsolver.instructor.constraints;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.instructor.model.Instructor;
import org.cpsolver.instructor.model.Section;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;
import org.cpsolver.instructor.model.TeachingRequest.Variable;

/**
 * Group Constraint. This constraint implements many of the course timetabling group and flexible constraints.
 * See {@link ConstraintType} for the list, and the appropriate {@link org.cpsolver.coursett.constraint.GroupConstraint.ConstraintType}
 * or {@link org.cpsolver.coursett.constraint.FlexibleConstraint} constraint for their description.
 * Because the instructor - class assignments are dynamic, the constraints are implemented by a single group constraint.
 * 
 * @author  Tomas Muller
 * @version IFS 1.4 (Instructor Sectioning)<br>
 *          Copyright (C) 2024 Tomas Muller<br>
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
public class GroupConstraint extends GlobalConstraint<TeachingRequest.Variable, TeachingAssignment> {
    
    public GroupConstraint() {}
    
    @Override
    public void computeConflicts(Assignment<Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
        for (Distribution d: value.getInstructor().getDistributions()) {
            if (!d.isHard()) continue;
            d.getType().computeConflicts(d, assignment, value, conflicts);
        }
    }
    
    @Override
    public String getName() {
        return "Distribution";
    }
    
    @Override
    public String toString() {
        return getName();
    }
    
    /**
     * Wrapper class representing one distribution preference set on an instructor.
     */
    public static class Distribution {
        GroupConstraint.ConstraintTypeInterface iType;
        boolean iRequired = false;
        boolean iProhibited = false;
        int iPenalty = 0;
        
        public Distribution(GroupConstraint.ConstraintTypeInterface type, String preference) {
            iType = type;
            iRequired = Constants.sPreferenceRequired.equals(preference);
            iProhibited = Constants.sPreferenceProhibited.equals(preference);
            iPenalty = Constants.preference2preferenceLevel(preference);
        }
        
        public GroupConstraint.ConstraintTypeInterface getType() { return iType; }
        public int getPenalty() { return iPenalty; }
        public boolean isRequired() { return iRequired; }
        public boolean isProhibited() { return iProhibited; }
        public boolean isHard() { return isRequired() || isProhibited(); }
        public String getPreference() {
            if (isRequired()) return Constants.sPreferenceRequired;
            if (isProhibited()) return Constants.sPreferenceProhibited;
            return Constants.preferenceLevel2preference(getPenalty());
        }
        public boolean isPositive() {
            if (isRequired()) return true;
            if (isProhibited()) return false;
            return getPenalty() <= 0;
        }

        @SuppressWarnings("unchecked")
        public <P> P getParameter() {
            if (iType instanceof ParametrizedConstraintType)
                return ((ParametrizedConstraintType<P>)iType).getParameter();
            else
                return null;
        }
    }

    /**
     * Group constraint check interface
     */
    public static interface Check {
        public void computeConflicts(Distribution distribution, Assignment<Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts);
        public double getValue(Distribution distribution, Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Instructor instructor, TeachingAssignment value);
    }
    
    /**
     * Group constraint check interface for constraints that can be computed on individual class pairs.
     */
    public static abstract class PairCheck implements Check {
        public abstract boolean isSatisfied(Distribution distribution, Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Instructor instructor, Section s1, Section s2);
        public abstract boolean isViolated(Distribution distribution, Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Instructor instructor, Section s1, Section s2);
        @Override
        public double getValue(Distribution distribution, Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Instructor instructor, TeachingAssignment value) {
            int ret = 0;
            Set<TeachingAssignment> assignments = instructor.getContext(assignment).getAssignments();
            if (value == null)
                for (TeachingAssignment ta1: assignments) {
                    for (Section s1: ta1.variable().getSections()) {
                        for (TeachingAssignment ta2: instructor.getContext(assignment).getAssignments()) {
                            for (Section s2: ta2.variable().getSections()) {
                                if (s1.equals(s2)) continue;
                                if (distribution.isPositive()) {
                                    if (!isSatisfied(distribution, assignment, instructor, s1, s2)) ret ++;
                                } else {
                                    if (!isViolated(distribution, assignment, instructor, s1, s2)) ret ++;
                                }
                            }
                        }
                    }
                }
            else
                for (Section s1: value.variable().getSections()) {
                    for (TeachingAssignment ta2: assignments) {
                        for (Section s2: ta2.variable().getSections()) {
                            if (s1.equals(s2)) continue;
                            if (distribution.isPositive()) {
                                if (!isSatisfied(distribution, assignment, instructor, s1, s2)) ret ++;
                            } else {
                                if (!isViolated(distribution, assignment, instructor, s1, s2)) ret ++;
                            }
                        }
                    }
                }
            if (ret == 0) return 0.0;
            int n = assignments.size() + (value == null || assignment.getValue(value.variable()) != null ? 0 : 1);
            return (2.0 * ret) / (n * (n - 1));
        }
        @Override
        public void computeConflicts(Distribution distribution, Assignment<Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
            for (Section s1: value.variable().getSections()) {
                for (TeachingAssignment ta2: value.getInstructor().getContext(assignment).getAssignments()) {
                    for (Section s2: ta2.variable().getSections()) {
                        if (s1.equals(s2)) continue;
                        if (distribution.isPositive()) {
                            if (!isSatisfied(distribution, assignment, value.getInstructor(), s1, s2)) conflicts.add(ta2);
                        } else {
                            if (!isViolated(distribution, assignment, value.getInstructor(), s1, s2)) conflicts.add(ta2);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Simplified group constraint check interface for constraints that can be computed on individual class pairs.
     */
    public static abstract class SimpleCheck extends PairCheck {
        public abstract boolean isSatisfied(Distribution d, Section s1, Section s2);
        public boolean isViolated(Distribution d,  Section s1, Section s2) { return true; }
        @Override
        public boolean isSatisfied(Distribution distribution, Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Instructor instructor, Section s1, Section s2) {
            return isSatisfied(distribution, s1, s2);
        }
        @Override
        public boolean isViolated(Distribution distribution, Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Instructor instructor, Section s1, Section s2) {
            return isViolated(distribution, s1, s2);
        }
    }
    
    /**
     * Simplified group constraint check interface for time-only constraints that can be computed on individual class pairs.
     */
    public static abstract class SimpleTimeCheck extends SimpleCheck {
        public abstract boolean isSatisfied(Distribution d, TimeLocation t1, TimeLocation t2);
        public boolean isViolated(Distribution d, TimeLocation t1, TimeLocation t2) { return true; }
        @Override
        public boolean isSatisfied(Distribution d, Section s1, Section s2) {
            if (s1.getTime() == null || s2.getTime() == null) return true;
            return isSatisfied(d, s1.getTime(), s2.getTime());
        }
        @Override
        public boolean isViolated(Distribution d, Section s1, Section s2) {
            if (s1.getTime() == null || s2.getTime() == null) return true;
            return isViolated(d, s1.getTime(), s2.getTime());
        }
    }

    /**
     * Factory class for group constraints with parameters
     */
    public static interface ConstraintCreator<P> {
        public ParametrizedConstraintType<P> create(String reference, String referenceRegExp);
    }
    
    /**
     * Interface for a distribution constraint type, including its implementation
     */
    public static interface ConstraintTypeInterface extends Check {
        public ConstraintType type();
        public String reference();
        public String getName();
    }
    
    /**
     * Distribution constraint with parameters.
     */
    public static class ParametrizedConstraintType<P> implements ConstraintTypeInterface {
        private String iReference;
        private ConstraintType iType;
        private P iParameter;
        private String iName;
        
        public ParametrizedConstraintType(ConstraintType type, P parameter, String reference) {
            iType = type; iParameter = parameter; iReference = reference;
        }
        
        public ParametrizedConstraintType<P> setName(String name) { iName = name; return this; }
        
        @Override
        public double getValue(Distribution distribution, Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Instructor instructor, TeachingAssignment value) {
            return iType.check().getValue(distribution, assignment, instructor, value);
        }
        
        @Override
        public void computeConflicts(Distribution distribution, Assignment<Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
            iType.check().computeConflicts(distribution, assignment, value, conflicts);
        }

        public P getParameter() { return iParameter; }
        @Override
        public ConstraintType type() { return iType; }
        @Override
        public String reference() { return iReference; }
        @Override
        public String getName() { return (iName == null ? iType.getName() : iName); }
    }
    
    /**
     * Distribution types and their implementation
     */
    public static enum ConstraintType implements ConstraintTypeInterface {
        /**
         * Same Days: Given classes must be taught on the same days. In case of classes of different time patterns, a class
         * with fewer meetings must meet on a subset of the days used by the class with more meetings. For example, if one
         * class pattern is 3x50, all others given in the constraint can only be taught on Monday, Wednesday, or Friday.
         * For a 2x100 class MW, MF, WF is allowed but TTh is prohibited.<BR>
         * When prohibited or (strongly) discouraged: any pair of classes classes cannot be taught on the same days (cannot
         *  overlap in days). For instance, if one class is MFW, the second has to be TTh.
         */
        SAME_DAYS("SAME_DAYS", "Same Days", new SimpleTimeCheck() {
            @Override
            public boolean isSatisfied(Distribution d, TimeLocation t1, TimeLocation t2) {
                return sameDays(t1.getDaysArray(), t2.getDaysArray());
            }
            @Override
            public boolean isViolated(Distribution d, TimeLocation t1, TimeLocation t2) {
                return !t1.shareDays(t2);
            }
            private boolean sameDays(int[] days1, int[] days2) {
                if (days2.length < days1.length)
                    return sameDays(days2, days1);
                int i2 = 0;
                for (int i1 = 0; i1 < days1.length; i1++) {
                    int d1 = days1[i1];
                    while (true) {
                        if (i2 == days2.length)
                            return false;
                        int d2 = days2[i2];
                        if (d1 == d2)
                            break;
                        i2++;
                        if (i2 == days2.length)
                            return false;
                    }
                    i2++;
                }
                return true;
            }
            }),
        /**
         * Same Start Time: Given classes must start during the same half-hour period of a day (independent of the actual
         * day the classes meet). For instance, MW 7:30 is compatible with TTh 7:30 but not with MWF 8:00.<BR>
         * When prohibited or (strongly) discouraged: any pair of classes in the given constraint cannot start during the
         * same half-hour period of any day of the week.
         */
        SAME_START("SAME_START", "Same Start Time", new SimpleTimeCheck() {
            @Override
            public boolean isSatisfied(Distribution d, TimeLocation t1, TimeLocation t2) {
                return (t1.getStartSlot() % Constants.SLOTS_PER_DAY) ==  (t2.getStartSlot() % Constants.SLOTS_PER_DAY);
            }
            @Override
            public boolean isViolated(Distribution d, TimeLocation t1, TimeLocation t2) {
                return (t1.getStartSlot() % Constants.SLOTS_PER_DAY) != (t2.getStartSlot() % Constants.SLOTS_PER_DAY);
            }}),
        /**
         * Same Room: Given classes must be taught in the same room.<BR>
         * When prohibited or (strongly) discouraged: any pair of classes in the constraint cannot be taught in the same room.
         */
        SAME_ROOM("SAME_ROOM", "Same Room", new SimpleCheck() {
            @Override
            public boolean isSatisfied(Distribution d, Section s1, Section s2) {
                return s1.isSameRoom(s2);
            }
            @Override
            public boolean isViolated(Distribution d, Section s1, Section s2) {
                return !s1.isSameRoom(s2);
            }}),
        /**
         * At Most X Hours A Day: Classes are to be placed in a way that there is no more than given number of hours in any day.
         */
        MAX_HRS_DAY("MAX_HRS_DAY\\(([0-9\\.]+)\\)", "At Most N Hours A Day", new Check() {
            protected int nrSlotsADay(Set<TeachingAssignment> assignments, BitSet week, int dayCode, TeachingRequest.Variable variable, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
                Set<Integer> slots = new HashSet<Integer>();
                for (TeachingAssignment ta: assignments) {
                    if (variable != null && variable.equals(ta.variable())) continue;
                    if (conflicts != null && conflicts.contains(ta)) continue;
                    for (Section section: ta.variable().getSections()) {
                        TimeLocation t = section.getTime();
                        if (t == null || (t.getDayCode() & dayCode) == 0 || (week != null && !t.shareWeeks(week))) continue;
                        for (int i = 0; i < t.getLength(); i++)
                            slots.add(i + t.getStartSlot());
                    }
                }
                if (value != null) {
                    for (Section section: value.variable().getSections()) {
                        TimeLocation t = section.getTime();
                        if (t == null || (t.getDayCode() & dayCode) == 0 || (week != null && !t.shareWeeks(week))) continue;
                        for (int i = 0; i < t.getLength(); i++)
                            slots.add(i + t.getStartSlot());
                    }
                }
                return slots.size();
            }
            @Override
            public double getValue(Distribution d, Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Instructor instructor, TeachingAssignment value) {
                Integer max = d.getParameter();
                Set<TeachingAssignment> assignments = instructor.getContext(assignment).getAssignments();
                double over = 0.0;
                for (int dayCode: Constants.DAY_CODES) {
                    for (BitSet week: instructor.getModel().getWeeks()) {
                        if (value == null) {
                           over += Math.max(0, nrSlotsADay(assignments, week, dayCode, null, null, null) - max);
                        } else {
                            int before = Math.max(0, nrSlotsADay(assignments, week, dayCode, value.variable(), null, null) - max);
                            int after = Math.max(0, nrSlotsADay(assignments, week, dayCode, value.variable(), value, null) - max);
                            over += after - before;
                        }
                    }
                }
                return over/ (60.0 * instructor.getModel().getWeeks().size());
            }
            @Override
            public void computeConflicts(Distribution d, Assignment<Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
                Integer max = d.getParameter();
                Set<TeachingAssignment> assignments = value.getInstructor().getContext(assignment).getAssignments();
                for (int dayCode: Constants.DAY_CODES) {
                    for (BitSet week: value.getInstructor().getModel().getWeeks()) {
                        if (nrSlotsADay(assignments, week, dayCode, value.variable(), value, conflicts) > max) {
                            List<TeachingAssignment> adepts = new ArrayList<TeachingAssignment>();
                            for (TeachingAssignment ta: assignments) {
                                if (ta.variable().equals(value.variable())) continue;
                                if (conflicts.contains(ta)) continue;
                                boolean hasDate = false;
                                for (Section section: ta.variable().getSections()) {
                                    TimeLocation t = section.getTime();
                                    if (t != null && (t.getDayCode() & dayCode) != 0 && t.shareWeeks(week)) {
                                        hasDate = true;
                                        break;
                                    }
                                }
                                if (hasDate) adepts.add(ta);
                            }
                            do {
                                if (adepts.isEmpty()) { conflicts.add(value); break; }
                                TeachingAssignment conflict = ToolBox.random(adepts);
                                adepts.remove(conflict);
                                conflicts.add(conflict);
                            } while (nrSlotsADay(assignments, week, dayCode, value.variable(), value, conflicts) > max);
                        }
                    }
                }
            }}, new ConstraintCreator<Integer>() {
            @Override
            public ParametrizedConstraintType<Integer> create(String reference, String regexp) {
                Matcher matcher = Pattern.compile(regexp).matcher(reference);
                if (matcher.find()) {
                    double hours = Double.parseDouble(matcher.group(1));
                    int slots = (int)Math.round(12.0 * hours);
                    return new ParametrizedConstraintType<Integer>(ConstraintType.MAX_HRS_DAY, slots, reference)
                            .setName("At Most " + matcher.group(1) + " Hours A Day");
                }
                return null;
            }}),
        /**
         * Given classes must be taught during the same weeks (i.e., must have the same date pattern).<br>
         * When prohibited or (strongly) discouraged: any two classes must have non overlapping date patterns.
         */
        SAME_WEEKS("SAME_WEEKS", "Same Weeks", new SimpleTimeCheck() {
            @Override
            public boolean isSatisfied(Distribution d, TimeLocation t1, TimeLocation t2) {
                return t1.getWeekCode().equals(t2.getWeekCode());
            }
            @Override
            public boolean isViolated(Distribution d, TimeLocation t1, TimeLocation t2) {
                return !t1.shareWeeks(t2);
            }}),
      /**
       * Work Day: Classes are to be placed in a way that there is no more than given number of hours between the start of the first class and the end of the class one on any day.
       */
        WORKDAY("WORKDAY\\(([0-9\\.]+)\\)", "Work Day", new SimpleTimeCheck() {
            @Override
            public boolean isSatisfied(Distribution d, TimeLocation t1, TimeLocation t2) {
                if (t1 == null || t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) return true;
                Integer parameter = d.getParameter();
                return Math.max(t1.getStartSlot() + t1.getLength(), t2.getStartSlot() + t2.getLength()) - Math.min(t1.getStartSlot(), t2.getStartSlot()) <= parameter;
            }}, new ConstraintCreator<Integer>() {
                @Override
                public ParametrizedConstraintType<Integer> create(String reference, String regexp) {
                    Matcher matcher = Pattern.compile(regexp).matcher(reference);
                    if (matcher.find()) {
                        double hours = Double.parseDouble(matcher.group(1));
                        int slots = (int)Math.round(12.0 * hours);
                        return new ParametrizedConstraintType<Integer>(ConstraintType.WORKDAY, slots, reference)
                                .setName(matcher.group(1) + " Hour Work Day");
                    }
                    return null;
                }}),
        /**
         * Minimal gap between classes.
         */
        MIN_GAP("MIN_GAP\\(([0-9\\.]+)\\)", "Mininal Gap Between Classes", new SimpleTimeCheck() {
            @Override
            public boolean isSatisfied(Distribution d, TimeLocation t1, TimeLocation t2) {
                if (t1 == null || t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) return true;
                Integer parameter = d.getParameter();
                return t1.getStartSlot() + t1.getLength() + parameter <= t2.getStartSlot() ||
                        t2.getStartSlot() + t2.getLength() + parameter <= t1.getStartSlot();
            }}, new ConstraintCreator<Integer>() {
            @Override
            public ParametrizedConstraintType<Integer> create(String reference, String regexp) {
                Matcher matcher = Pattern.compile(regexp).matcher(reference);
                if (matcher.find()) {
                    double hours = Double.parseDouble(matcher.group(1));
                    int slots = (int)Math.round(12.0 * hours);
                    return new ParametrizedConstraintType<Integer>(ConstraintType.MIN_GAP, slots, reference)
                            .setName("At Least " + matcher.group(1) + " Hours Between Classes");
                }
                return null;
            }}),
        /**
         * Given classes must be taught in a way there is a break between two blocks of classes. 
         */
        MAX_BLOCK("_(MaxBlock):([0-9]+):([0-9]+)_", "Max Block", new Check() {
            @Override
            public double getValue(Distribution d, Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Instructor instructor, TeachingAssignment value) {
                int [] params = d.getParameter();
                int maxBlockSlotsBTB = params[0];
                int maxBreakBetweenBTB = params[1];
                double penalty = 0;
                // constraint is checked for every day in week
                for (int dayCode : Constants.DAY_CODES) {
                    // constraint is checked for every week in semester (or for the whole semester)
                    for (BitSet week : instructor.getModel().getWeeks()) {
                        // each blocks contains placements which are BTB
                        List<Block> blocks = getBlocks(assignment, instructor, dayCode, null, (value == null ? null : value.variable()), value, week,  maxBreakBetweenBTB);
                        for (Block block : blocks) {
                            // ignore single-start/signle-class blocks
                            if (block.getNbrPlacements() == 1 || block.haveSameStartTime()) continue;
                            // violated if there is a block containing more than one
                            // class longer than maxBlockSlotsBTB
                            if (block.getLengthInSlots() > maxBlockSlotsBTB) {
                                int blockLengthPenalty = block.getLengthInSlots() / maxBlockSlotsBTB;
                                penalty += blockLengthPenalty;
                            }
                        }
                    }
                }
                return penalty / (5.0 * instructor.getModel().getWeeks().size());
            }
            @Override
            public void computeConflicts(Distribution d, Assignment<Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
                int [] params = d.getParameter();
                int maxBlockSlotsBTB = params[0];
                int maxBreakBetweenBTB = params[1];
                List<BitSet> weeks = value.getInstructor().getModel().getWeeks();
                // constraint is checked for every day in week
                for (int dayCode : Constants.DAY_CODES) {
                    // constraint is checked for every week in semester (or for the whole semester)
                    for (BitSet week : weeks) {
                        boolean isProblem = false;
                        do {
                            isProblem = false;
                            // each blocks contains placements which are BTB 
                            List<Block> blocks = getBlocks(assignment, value.getInstructor(), dayCode, conflicts, value.variable(), value, week, maxBreakBetweenBTB);
                            for (Block block : blocks) {
                                // if the block is not affected by the current placement, continue
                                if (!block.contains(value)) continue;
                                Set<TeachingAssignment> adepts = new HashSet<TeachingAssignment>();                        
                                // if there is only 1 placement in block, the block cannot be shortened
                                // if placements of a block start at the same time, they intersect
                                // this solves problem when between 2 classes is required MEET_TOGETHER  
                                if (block.getNbrPlacements() == 1 || block.haveSameStartTime()) continue;
                                // if block is longer than maximum size, some of its placements are conflicts
                                if (block.getLengthInSlots() > maxBlockSlotsBTB) {
                                    // classes from block are potential conflicts
                                    for (TeachingAssignmentSection tas: block.getSections())
                                        adepts.add(tas.getTeachingAssignment());                     
                                    // do not set currently assigned value as conflict
                                    adepts.remove(value);
                                    isProblem = true;
                                    // pick random placement
                                    TeachingAssignment conflict = ToolBox.random(adepts);
                                    if (conflict != null) {
                                        conflicts.add(conflict);
                                    }
                                }
                            }
                        } while (isProblem);
                    }
                }
            }}, new ConstraintCreator<int[]>() {
            @Override
            public ParametrizedConstraintType<int[]> create(String reference, String regexp) {
                Matcher matcher = Pattern.compile(regexp).matcher(reference);
                if (matcher.find()) {
                    int maxBlockSlotsBTB = Integer.parseInt(matcher.group(2))/Constants.SLOT_LENGTH_MIN;
                    int maxBreakBetweenBTB = Integer.parseInt(matcher.group(3))/Constants.SLOT_LENGTH_MIN;
                    return new ParametrizedConstraintType<int[]>(ConstraintType.MAX_BLOCK, new int[] {maxBlockSlotsBTB, maxBreakBetweenBTB}, reference)
                            .setName("Max " + (maxBlockSlotsBTB / 12.0) + "h Blocks");
                }
                return null;
            }}),
        /**
         * Limit number of breaks between adjacent classes on a day.
         */
        MAX_BREAKS("_(MaxBreaks):([0-9]+):([0-9]+)_", "Max Breaks", new Check() {
            @Override
            public double getValue(Distribution d, Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Instructor instructor, TeachingAssignment value) {
                int [] params = d.getParameter();
                int maxBlocksOnADay = params[0];
                int maxBreakBetweenBTB = params[1];
                double penalty = 0;
                // constraint is checked for every day in week
                for (int dayCode : Constants.DAY_CODES) {
                    // constraint is checked for every week in semester (or for the whole semester)
                    for (BitSet week : instructor.getModel().getWeeks()) {
                        // each blocks contains placements which are BTB
                        List<Block> blocks = getBlocks(assignment, instructor, dayCode, null, (value == null ? null : value.variable()), value, week,  maxBreakBetweenBTB);
                        // too many blocks -> increase penalty
                        if (blocks.size() > maxBlocksOnADay)
                            penalty += (blocks.size() - maxBlocksOnADay);
                    }
                }
                return penalty / (5.0 * instructor.getModel().getWeeks().size());
            }
            @Override
            public void computeConflicts(Distribution d, Assignment<Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
                int [] params = d.getParameter();
                int maxBlocksOnADay = params[0];
                int maxBreakBetweenBTB = params[1];
                List<BitSet> weeks = value.getInstructor().getModel().getWeeks();
                // constraint is checked for every day in week
                for (int dayCode : Constants.DAY_CODES) {
                    // constraint is checked for every week in semester (or for the whole semester)
                    for (BitSet week : weeks) {
                     // each blocks contains placements which are BTB 
                        List<Block> blocks = getBlocks(assignment, value.getInstructor(), dayCode, conflicts, value.variable(), value, week, maxBreakBetweenBTB);
                        while (blocks.size() > maxBlocksOnADay) {
                            // too many blocks -> identify adepts for unassignment
                            List<Block> adepts = new ArrayList<Block>(); int size = 0;
                            for (Block block: blocks) {
                                if (block.contains(value)) continue; // skip block containing given value
                                // select adepts of the smallest size
                                if (adepts.isEmpty() || size > block.getSections().size()) {
                                    adepts.clear();
                                    adepts.add(block);
                                    size = block.getSections().size();
                                } else if (size == block.getSections().size()) {
                                    adepts.add(block);
                                }
                            }
                            // pick one randomly
                            Block block = ToolBox.random(adepts);
                            blocks.remove(block);
                            for (TeachingAssignmentSection tas: block.getSections())
                                if (tas.getTeachingAssignment().equals(assignment.getValue(tas.getTeachingAssignment().variable())))
                                    conflicts.add(tas.getTeachingAssignment());
                        }
                    }
                }
            }}, new ConstraintCreator<int[]>() {
            @Override
            public ParametrizedConstraintType<int[]> create(String reference, String regexp) {
                Matcher matcher = Pattern.compile(regexp).matcher(reference);
                if (matcher.find()) {
                    int maxBlocksOnADay = 1 + Integer.parseInt(matcher.group(2));
                    int maxBreakBetweenBTB = Integer.parseInt(matcher.group(3)) / Constants.SLOT_LENGTH_MIN;
                    return new ParametrizedConstraintType<int[]>(ConstraintType.MAX_BREAKS, new int[] {maxBlocksOnADay, maxBreakBetweenBTB}, reference)
                            .setName(maxBlocksOnADay == 1 ? "No Break" : maxBlocksOnADay == 2 ? "Max 1 Break" : "Max " + (maxBlocksOnADay - 1) + " Breaks");
                }
                return null;
            }}),
        /**
         * Limit number of days of a week. 
         */
        MAX_DAYS("_(MaxDays):([0-9]+)_", "Max Days", new Check() {
            protected boolean hasDay(BitSet week, int dayOfWeek, Section section) {
                if (section.getTime() == null || !section.getTime().getWeekCode().intersects(week)) return false;
                return (section.getTime().getDayCode() & Constants.DAY_CODES[dayOfWeek]) != 0;
            }
            protected boolean hasDay(BitSet week, int dayOfWeek, TeachingAssignment ta) {
                for (Section section: ta.variable().getSections())
                    if (hasDay(week, dayOfWeek, section)) return true;
                return false;
            }
            @Override
            public double getValue(Distribution d, Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Instructor instructor, TeachingAssignment value) {
                Integer maxDays  = d.getParameter();
                Set<TeachingAssignment> assignments = instructor.getContext(assignment).getAssignments();
                double penalty = 0;
                for (BitSet week: instructor.getModel().getWeeks()) {
                    Set<Integer> days = new HashSet<Integer>();
                    for (TeachingAssignment ta: assignments) {
                        if (value != null && value.variable().equals(ta.variable())) continue;
                        for (int day = 0; day < Constants.DAY_CODES.length; day++)
                            if (hasDay(week, day, ta))
                                days.add(day);
                    }
                    if (value != null) {
                        for (int day = 0; day < Constants.DAY_CODES.length; day++)
                            if (hasDay(week, day, value) && days.add(day) && days.size() > maxDays)
                                penalty += 1.0;
                    } else {
                        penalty += Math.max(0, days.size() - maxDays);
                    }
                }
                return penalty / (Math.max(1.0, 5.0 - maxDays) * instructor.getModel().getWeeks().size());
            }
            @Override
            public void computeConflicts(Distribution d, Assignment<Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
                Integer maxDays  = d.getParameter();
                Set<TeachingAssignment> assignments = value.getInstructor().getContext(assignment).getAssignments();
                for (BitSet week: value.getInstructor().getModel().getWeeks()) {
                    Set<Integer> selectedDays = new HashSet<Integer>();
                    for (int day = 0; day < Constants.DAY_CODES.length; day++)
                        if (hasDay(week, day, value))
                            selectedDays.add(day);
                    // selected value has no days -> next week
                    if (selectedDays.isEmpty()) continue;
                    // selected value is over -> it cannot be assigned
                    if (selectedDays.size() > maxDays) {
                        conflicts.add(value); continue;
                    }
                    // check other days
                    while (true) {
                        Set<Integer> otherDays = new HashSet<Integer>();
                        for (TeachingAssignment ta: assignments) {
                            if (value != null && value.variable().equals(ta.variable())) continue;
                            if (conflicts.contains(ta)) continue;
                            for (int day = 0; day < Constants.DAY_CODES.length; day++)
                                if (!selectedDays.contains(day) && hasDay(week, day, ta))
                                    otherDays.add(day);
                        }
                        if (otherDays.size() + selectedDays.size() <= maxDays) break;
                        int day = ToolBox.random(otherDays);
                        for (TeachingAssignment ta: assignments) {
                            if (value != null && value.variable().equals(ta.variable())) continue;
                            if (conflicts.contains(ta)) continue;
                            if (hasDay(week, day, ta))
                                conflicts.add(ta);
                        }
                    }
                }
            }}, new ConstraintCreator<Integer>() {
            @Override
            public ParametrizedConstraintType<Integer> create(String reference, String regexp) {
                Matcher matcher = Pattern.compile(regexp).matcher(reference);
                if (matcher.find()) {
                    int maxDays =  Integer.parseInt(matcher.group(2));
                    return new ParametrizedConstraintType<Integer>(ConstraintType.MAX_DAYS, maxDays, reference)
                            .setName(maxDays == 1 ? "Max 1 Day" : "Max " + maxDays + " Days");
                }
                return null;
            }}),
        /**
         * There must be a break of a given length in a given time interval.
         */
        BREAK("_(Break):([0-9]+):([0-9]+):([0-9]+)_", "Break", new Check() {
            @Override
            public double getValue(Distribution d, Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Instructor instructor, TeachingAssignment value) {
                int [] params = d.getParameter();
                int breakStart = params[0];
                int breakEnd = params[1];
                int breakLength = params[2];
                double penalty = 0;
                // constraint is checked for every day in week
                for (int dayCode : Constants.DAY_CODES) {
                    // constraint is checked for every week in semester (or for the whole semester)
                    for (BitSet week : instructor.getModel().getWeeks()) {
                        // each blocks contains placements which are BTB
                        List<Block> blocks = getBlocks(assignment, instructor, dayCode, null, (value == null ? null : value.variable()) ,value, week,  breakLength);
                        // too many blocks -> increase penalty
                        List<Block> matchingBlocks = new ArrayList<Block>();
                        for(Block block: blocks) {
                            // if block intersects with break interval, it will be used in conflict selection
                            if (block.getStartSlotCurrentBlock() <= breakEnd && block.getEndSlotCurrentBlock() >= breakStart)
                                matchingBlocks.add(block);          
                        }
                        int size = matchingBlocks.size();
                        if (size == 1) {
                            Block block = matchingBlocks.get(0);
                            // the matching block does not contain the given placement -> ignore
                            if (value != null && !block.contains(value)) continue;
                            // check whether the block leaves enough space for break
                            if (block.getStartSlotCurrentBlock() - breakStart >= breakLength || breakEnd - block.getEndSlotCurrentBlock() >= breakLength)
                                continue;
                            // if it doesn't
                            penalty += 1.0;
                        }   
                    }
                }
                return penalty / (5.0 * instructor.getModel().getWeeks().size());
            }
            @Override
            public void computeConflicts(Distribution d, Assignment<Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
                int [] params = d.getParameter();
                int breakStart = params[0];
                int breakEnd = params[1];
                int breakLength = params[2];
                // constraint is checked for every day in week
                for (int dayCode : Constants.DAY_CODES) {
                    // constraint is checked for every week in semester (or for the whole semester)
                    for (BitSet week : value.getInstructor().getModel().getWeeks()) {
                        while (true) {
                            // each blocks contains placements which are BTB
                            List<Block> blocks = getBlocks(assignment, value.getInstructor(), dayCode, conflicts, value.variable(), value, week,  breakLength);
                            // too many blocks -> increase penalty
                            List<Block> matchingBlocks = new ArrayList<Block>();
                            for(Block block: blocks) {
                                // if block intersects with break interval, it will be used in conflict selection
                                if (block.getStartSlotCurrentBlock() <= breakEnd && block.getEndSlotCurrentBlock() >= breakStart)
                                    matchingBlocks.add(block);          
                            }
                            int size = matchingBlocks.size();
                            if (size == 1) {
                                Block block = matchingBlocks.get(0);
                                // the matching block does not contain the given placement -> ignore
                                if (!block.contains(value)) break;
                                // check whether the block leaves enough space for break
                                if (block.getStartSlotCurrentBlock() - breakStart >= breakLength || breakEnd - block.getEndSlotCurrentBlock() >= breakLength)
                                    break;
                                // if it doesn't
                                List<TeachingAssignmentSection> adepts = new ArrayList<TeachingAssignmentSection>();
                                // every placement intersecting with break interval might be potential conflict
                                for (TeachingAssignmentSection p: block.getSections())
                                    if (p.getTime().getStartSlot() <= breakEnd && p.getTime().getStartSlot()+ p.getTime().getLength() >= breakStart)
                                        adepts.add(p);
                                if (adepts.size() > 0) {
                                    conflicts.add(ToolBox.random(adepts).getTeachingAssignment());
                                    continue;
                                }
                                break;
                            }
                        }   
                    }
                }
            }}, new ConstraintCreator<int[]>() {
            @Override
            public ParametrizedConstraintType<int[]> create(String reference, String regexp) {
                Matcher matcher = Pattern.compile(regexp).matcher(reference);
                if (matcher.find()) {
                    int breakStart = Integer.parseInt(matcher.group(2));
                    int breakEnd = Integer.parseInt(matcher.group(3)); 
                    int breakLength = Integer.parseInt(matcher.group(4))/Constants.SLOT_LENGTH_MIN;
                    return new ParametrizedConstraintType<int[]>(ConstraintType.BREAK, new int[] {breakStart, breakEnd, breakLength}, reference)
                            .setName((breakEnd * 5) + " Min Break");
                }
                return null;
            }}),
        /**
         * Limit number of weeks on which an a class can take place.
         */
        MAX_WEEKS("_(MaxWeeks):([0-9]+):([0-9]+)_", "Max Weeks", new Check() {
            @Override
            public double getValue(Distribution d, Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Instructor instructor, TeachingAssignment value) {
                int [] params = d.getParameter();
                int maxWeeks = params[0];
                int dayCode = params[1];
                if (value != null && !isCorectDayOfWeek(value, dayCode)) return 0.0;
                Set<TeachingAssignment> assignments = instructor.getContext(assignment).getAssignments();
                double penalty = 0;
                Set<BitSet> weeks = new HashSet<BitSet>();
                for (BitSet week: instructor.getModel().getWeeks()) {
                    for (TeachingAssignment ta: assignments) {
                        if (value != null && value.variable().equals(ta.variable())) continue;
                        if (isCorectDayAndWeek(ta, dayCode, week))
                            weeks.add(week);
                    }
                }
                if (value != null) {
                    for (BitSet week: instructor.getModel().getWeeks()) {
                        if (isCorectDayAndWeek(value, dayCode, week) && weeks.add(week) && weeks.size() > maxWeeks)
                            penalty += 1.0;
                    }
                } else {
                    penalty += Math.max(0.0, weeks.size() - maxWeeks);
                }
                return penalty / Math.max(1.0, instructor.getModel().getWeeks().size() - maxWeeks);
            }
            @Override
            public void computeConflicts(Distribution d, Assignment<Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
                int [] params = d.getParameter();
                int maxWeeks = params[0];
                int dayCode = params[1];
                if (!isCorectDayOfWeek(value, dayCode)) return;
                
                Set<BitSet> selectedWeeks = new HashSet<BitSet>();
                for (BitSet week: value.getInstructor().getModel().getWeeks()) {
                    if (isCorectDayAndWeek(value, dayCode, week))
                        selectedWeeks.add(week);
                }
                if (selectedWeeks.size() > maxWeeks) {
                    conflicts.add(value);
                    return;
                }
                Set<TeachingAssignment> assignments = value.getInstructor().getContext(assignment).getAssignments();
                while (true) {
                    Set<BitSet> otherWeeks = new HashSet<BitSet>();
                    for (BitSet week: value.getInstructor().getModel().getWeeks()) {
                        if (selectedWeeks.contains(week)) continue;
                        for (TeachingAssignment ta: assignments) {
                            if (value != null && value.variable().equals(ta.variable())) continue;
                            if (conflicts.contains(ta)) continue;
                            if (isCorectDayAndWeek(ta, dayCode, week))
                                otherWeeks.add(week);
                        }
                    }
                    if (otherWeeks.size() + selectedWeeks.size() <= maxWeeks) break;
                    BitSet week = ToolBox.random(otherWeeks);
                    for (TeachingAssignment ta: assignments) {
                        if (value != null && value.variable().equals(ta.variable())) continue;
                        if (conflicts.contains(ta)) continue;
                        if (isCorectDayAndWeek(ta, dayCode, week)) {
                            conflicts.add(ta);
                            break;
                        }
                    }
                }
            }}, new ConstraintCreator<int[]>() {
            @Override
            public ParametrizedConstraintType<int[]> create(String reference, String regexp) {
                Matcher matcher = Pattern.compile(regexp).matcher(reference);
                if (matcher.find()) {
                    int maxWeeks = Integer.parseInt(matcher.group(2));
                    int dayCode = Integer.parseInt(matcher.group(3));
                    return new ParametrizedConstraintType<int[]>(ConstraintType.MAX_WEEKS, new int[] {maxWeeks, dayCode}, reference)
                            .setName("Max " + maxWeeks + " Weeks");
                }
                return null;
            }}),
        /**
         * Minimize free time of an instructor during a day (between the first and the last class).
         */
        MAX_HOLES("_(MaxHoles):([0-9]+)_", "Max Holes", new Check() {
            int countHoles(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Instructor instructor,int dayCode, Set<TeachingAssignment> conflicts, TeachingRequest.Variable variable, TeachingAssignment value, BitSet week) {
                Set<TeachingAssignmentSection> placements = getRelevantPlacements(assignment, instructor, dayCode, conflicts, variable, value, week);
                int lastSlot = -1;
                int holes = 0;
                for (TeachingAssignmentSection placement: placements) {
                    if (lastSlot >= 0 && placement.getTime().getStartSlot() > lastSlot) {
                        holes += (placement.getTime().getStartSlot() - lastSlot);
                    }
                    lastSlot = Math.max(lastSlot, placement.getTime().getStartSlot() + placement.getTime().getLength());
                }
                return holes;
            }
            
            @Override
            public double getValue(Distribution d, Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Instructor instructor, TeachingAssignment value) {
                int [] params = d.getParameter();
                int maxHolesOnADay = params[0];
                double penalty = 0;
                for (int dayCode : Constants.DAY_CODES) {
                    for (BitSet week: instructor.getModel().getWeeks()) {
                        if (value != null && !isCorectDayAndWeek(value, dayCode, week)) continue;
                        if (value == null) {
                            penalty += Math.max(0, countHoles(assignment, instructor, dayCode, null, null, null, week) - maxHolesOnADay);
                        } else {
                            int before = Math.max(0, countHoles(assignment, instructor, dayCode, null, value.variable(), null, week) - maxHolesOnADay);
                            int after = Math.max(0, countHoles(assignment, instructor, dayCode, null, value.variable(), value, week) - maxHolesOnADay);
                            penalty += after - before;
                        }
                    }
                }
                return penalty / (60.0 * instructor.getModel().getWeeks().size());
            }
            @Override
            public void computeConflicts(Distribution d, Assignment<Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
                int [] params = d.getParameter();
                int maxHolesOnADay = params[0];
                for (int dayCode : Constants.DAY_CODES) {
                    for (BitSet week : value.getInstructor().getModel().getWeeks()) {
                        if (!isCorectDayAndWeek(value, dayCode, week)) continue;
                        int penalty = countHoles(assignment, value.getInstructor(), dayCode, conflicts, value.variable(), value, week);
                        while (penalty > maxHolesOnADay) {
                            List<TeachingAssignmentSection> adepts = new ArrayList<TeachingAssignmentSection>();
                            for (TeachingAssignmentSection placement: getRelevantPlacements(assignment, value.getInstructor(), dayCode, conflicts, value.variable(), value, week)) {
                                if (placement.getTeachingAssignment().equals(value)) continue; // skip given value
                                // check if removing placement would improve the penalty
                                Set<TeachingAssignment> test = new HashSet<TeachingAssignment>(conflicts); test.add(placement.getTeachingAssignment());
                                int newPenalty = countHoles(assignment, value.getInstructor(), dayCode, test, value.variable(), value, week);
                                if (newPenalty <= penalty)
                                    adepts.add(placement);
                            }
                            
                            // no adepts -> fail
                            if (adepts.isEmpty()) {
                                conflicts.add(value); return;
                            }
                            
                            // pick one randomly
                            conflicts.add(ToolBox.random(adepts).getTeachingAssignment());
                            penalty = countHoles(assignment, value.getInstructor(), dayCode, conflicts, value.variable(), value, week);
                        }
                    }
                }
            }}, new ConstraintCreator<int[]>() {
            @Override
            public ParametrizedConstraintType<int[]> create(String reference, String regexp) {
                Matcher matcher = Pattern.compile(regexp).matcher(reference);
                if (matcher.find()) {
                    int maxHolesOnADay = Integer.parseInt(matcher.group(2)) / Constants.SLOT_LENGTH_MIN;
                    return new ParametrizedConstraintType<int[]>(ConstraintType.MAX_HOLES, new int[] {maxHolesOnADay}, reference)
                            .setName("Max " + (maxHolesOnADay/12.0) + " Free Hours");
                }
                return null;
            }}),
        /**
         * Limit number of half-days of a week. 
         */
        MAX_HALF_DAYS("_(MaxHalfDays):([0-9]+)_", "Max Half-Days", new Check() {
            private Integer iNoonSlot = null; 
            protected boolean hasHalfDay(BitSet week, int dayOfWeek, Section section, boolean morning) {
                if (section.getTime() == null || !section.getTime().getWeekCode().intersects(week)) return false;
                if ((section.getTime().getDayCode() & Constants.DAY_CODES[dayOfWeek]) == 0) return false;
                if (morning)
                    return section.getTime().getStartSlot() < iNoonSlot;
                else
                    return section.getTime().getStartSlot() >= iNoonSlot;
            }
            protected boolean hasHalfDay(BitSet week, int dayOfWeek, TeachingAssignment ta, boolean morning) {
                for (Section section: ta.variable().getSections())
                    if (hasHalfDay(week, dayOfWeek, section, morning)) return true;
                return false;
            }
            
            @Override
            public double getValue(Distribution d, Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Instructor instructor, TeachingAssignment value) {
                if (iNoonSlot == null)
                    iNoonSlot = instructor.getModel().getProperties().getPropertyInteger("General.HalfDaySlot", 144);
                int [] params = d.getParameter();
                int maxHalfDays = params[0];
                double penalty = 0;
                Set<TeachingAssignment> assignments = instructor.getContext(assignment).getAssignments();
                for (BitSet week: instructor.getModel().getWeeks()) {
                    Set<Integer> mornings = new HashSet<Integer>();
                    Set<Integer> afternoons = new HashSet<Integer>();
                    for (TeachingAssignment ta: assignments) {
                        if (value != null && value.variable().equals(ta.variable())) continue;
                        for (int day = 0; day < Constants.DAY_CODES.length; day++) {
                            if (hasHalfDay(week, day, ta, true))
                                mornings.add(day);
                            if (hasHalfDay(week, day, ta, false))
                                afternoons.add(day);
                        }
                    }
                    if (value != null) {
                        for (int day = 0; day < Constants.DAY_CODES.length; day++) {
                            if (hasHalfDay(week, day, value, true) && mornings.add(day) && (mornings.size() + afternoons.size()) > maxHalfDays)
                                penalty += 1.0;
                            if (hasHalfDay(week, day, value, false) && afternoons.add(day) && (mornings.size() + afternoons.size()) > maxHalfDays)
                                penalty += 1.0;
                        }
                    } else {
                        penalty += Math.max(0, mornings.size() + afternoons.size() - maxHalfDays);
                    }
                }
                return penalty / (Math.max(1.0, 10.0 - maxHalfDays) * instructor.getModel().getWeeks().size());
            }
            @Override
            public void computeConflicts(Distribution d, Assignment<Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
                if (iNoonSlot == null)
                    iNoonSlot = value.getInstructor().getModel().getProperties().getPropertyInteger("General.HalfDaySlot", 144);
                int [] params = d.getParameter();
                int maxHalfDays = params[0];
                Set<TeachingAssignment> assignments = value.getInstructor().getContext(assignment).getAssignments();
                for (BitSet week: value.getInstructor().getModel().getWeeks()) {
                    Set<Integer> selectedHalfDays = new HashSet<Integer>();
                    for (int day = 0; day < Constants.DAY_CODES.length; day++) {
                        if (hasHalfDay(week, day, value, true))
                            selectedHalfDays.add(2 * day);
                        if (hasHalfDay(week, day, value, false))
                            selectedHalfDays.add(2 * day + 1);
                    }
                    // selected value has no days -> next week
                    if (selectedHalfDays.size() == 0) continue;
                    // selected value is over -> it cannot be assigned
                    if (selectedHalfDays.size() > maxHalfDays) {
                        conflicts.add(value); continue;
                    }
                    // check other days
                    while (true) {
                        Set<Integer> otherHalfDays = new HashSet<Integer>();
                        for (TeachingAssignment ta: assignments) {
                            if (value != null && value.variable().equals(ta.variable())) continue;
                            if (conflicts.contains(ta)) continue;
                            for (int day = 0; day < Constants.DAY_CODES.length; day++) {
                                if (!selectedHalfDays.contains(2 * day) && hasHalfDay(week, day, ta, true))
                                    otherHalfDays.add(2 * day);
                                if (!selectedHalfDays.contains(2 * day + 1) && hasHalfDay(week, day, ta, false))
                                    otherHalfDays.add(2 * day + 1);
                            }
                        }
                        if (selectedHalfDays.size() + otherHalfDays.size() <= maxHalfDays) break;
                        int halfday = ToolBox.random(otherHalfDays);
                        int day = halfday / 2;
                        boolean morning = ((halfday % 2) == 0);
                        for (TeachingAssignment ta: assignments) {
                            if (value != null && value.variable().equals(ta.variable())) continue;
                            if (conflicts.contains(ta)) continue;
                            if (hasHalfDay(week, day, ta, morning))
                                conflicts.add(ta);
                        }
                    }
                }
            }}, new ConstraintCreator<int[]>() {
            @Override
            public ParametrizedConstraintType<int[]> create(String reference, String regexp) {
                Matcher matcher = Pattern.compile(regexp).matcher(reference);
                if (matcher.find()) {
                    int maxHalfDays = Integer.parseInt(matcher.group(2));
                    return new ParametrizedConstraintType<int[]>(ConstraintType.MAX_HALF_DAYS, new int[] {maxHalfDays}, reference)
                            .setName("Max " + maxHalfDays + " Half-Days");
                }
                return null;
            }}),
        /**
         * Limit number of consecutive days of a week. 
         */
        MAX_CONSECUTIVE_DAYS("_(MaxConsDays):([0-9]+)_", "Max Consecutive Days", new Check() {
            protected boolean hasDay(BitSet week, int dayOfWeek, Section section) {
                if (section.getTime() == null || !section.getTime().getWeekCode().intersects(week)) return false;
                return (section.getTime().getDayCode() & Constants.DAY_CODES[dayOfWeek]) != 0;
            }
            protected boolean hasDay(BitSet week, int dayOfWeek, TeachingAssignment ta) {
                for (Section section: ta.variable().getSections())
                    if (hasDay(week, dayOfWeek, section)) return true;
                return false;
            }
            protected int countDays(TreeSet<Integer> days) {
                if (days.isEmpty()) return 0;
                return days.last() - days.first() + 1;
            }
            protected int countDays(TreeSet<Integer> days1, TreeSet<Integer> days2) {
                if (days1.isEmpty()) return countDays(days2);
                if (days2.isEmpty()) return countDays(days1);
                return Math.max(days1.last(), days2.last()) - Math.min(days1.first(), days2.first()) + 1;
            }
            @Override
            public double getValue(Distribution d, Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Instructor instructor, TeachingAssignment value) {
                int [] params = d.getParameter();
                int maxDays = params[0];
                double penalty = 0;
                Set<TeachingAssignment> assignments = instructor.getContext(assignment).getAssignments();
                for (BitSet week: instructor.getModel().getWeeks()) {
                    TreeSet<Integer> days = new TreeSet<Integer>();
                    for (TeachingAssignment ta: assignments) {
                        if (value != null && value.variable().equals(ta.variable())) continue;
                        for (int day = 0; day < Constants.DAY_CODES.length; day++)
                            if (hasDay(week, day, ta))
                                days.add(day);
                    }
                    if (value != null) {
                        int before = Math.max(0, countDays(days) - maxDays);
                        for (int day = 0; day < Constants.DAY_CODES.length; day++)
                            if (hasDay(week, day, value))
                                days.add(day);
                        int after = Math.max(0, countDays(days) - maxDays);
                        penalty += after - before;
                    } else {
                        penalty += Math.max(0, countDays(days) - maxDays);
                    }
                }
                return penalty / (Math.max(1.0, 5.0 - maxDays) * instructor.getModel().getWeeks().size());
            }
            @Override
            public void computeConflicts(Distribution d, Assignment<Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
                int [] params = d.getParameter();
                int maxDays = params[0];
                Set<TeachingAssignment> assignments = value.getInstructor().getContext(assignment).getAssignments();
                for (BitSet week: value.getInstructor().getModel().getWeeks()) {
                    TreeSet<Integer> selectedDays = new TreeSet<Integer>();
                    for (int day = 0; day < Constants.DAY_CODES.length; day++)
                        if (hasDay(week, day, value))
                            selectedDays.add(day);
                    // selected value has no days -> next week
                    if (selectedDays.isEmpty()) continue;
                    // selected value is over -> it cannot be assigned
                    if (countDays(selectedDays) > maxDays) {
                        conflicts.add(value); continue;
                    }
                    // check other days
                    while (true) {
                        TreeSet<Integer> otherDays = new TreeSet<Integer>();
                        for (TeachingAssignment ta: assignments) {
                            if (value != null && value.variable().equals(ta.variable())) continue;
                            if (conflicts.contains(ta)) continue;
                            for (int day = 0; day < Constants.DAY_CODES.length; day++)
                                if (!selectedDays.contains(day) && hasDay(week, day, ta))
                                    otherDays.add(day);
                        }
                        if (countDays(selectedDays, otherDays) <= maxDays) break;
                        int day = (ToolBox.random(1) == 0 ? otherDays.first() : otherDays.last());
                        for (TeachingAssignment ta: assignments) {
                            if (value != null && value.variable().equals(ta.variable())) continue;
                            if (conflicts.contains(ta)) continue;
                            if (hasDay(week, day, ta))
                                conflicts.add(ta);
                        }
                    }
                }
            }}, new ConstraintCreator<int[]>() {
            @Override
            public ParametrizedConstraintType<int[]> create(String reference, String regexp) {
                Matcher matcher = Pattern.compile(regexp).matcher(reference);
                if (matcher.find()) {
                    int maxDays = Integer.parseInt(matcher.group(2));
                    return new ParametrizedConstraintType<int[]>(ConstraintType.MAX_CONSECUTIVE_DAYS, new int[] {maxDays}, reference)
                            .setName("Max " + maxDays + " Consecutive Days");
                }
                return null;
            }}),
        ;
        
        private String iReference, iName;
        private Check iCheck = null;
        private ConstraintCreator<?> iCretor = null;
        
        ConstraintType(String reference, String name, Check check) {
            iReference = reference; iName = name; iCheck = check;
        }
        
        ConstraintType(String reference, String name, Check check, ConstraintCreator<?> creator) {
            iReference = reference; iName = name; iCheck = check; iCretor = creator;
        }
        
        @Override
        public ConstraintType type() { return this; }
        
        @Override
        public String reference() { return iReference; }
        
        @Override
        public String getName() { return iName; }

        @Override
        public double getValue(Distribution distribution, Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Instructor instructor, TeachingAssignment value) {
            return iCheck.getValue(distribution, assignment, instructor, value);
        }
        
        @Override
        public void computeConflicts(Distribution distribution, Assignment<Variable, TeachingAssignment> assignment, TeachingAssignment value, Set<TeachingAssignment> conflicts) {
            iCheck.computeConflicts(distribution, assignment, value, conflicts);
        }
        
        private Check check() { return iCheck; }
        private ConstraintCreator<?> creator() { return iCretor; }
    }
    
    /**
     * Create a constraint type from the provided reference. Returns null when there is no match.
     */
    public static ConstraintTypeInterface getConstraintType(String reference, String name) {
        for (ConstraintType t: ConstraintType.values()) {
            if (t.reference().equals(reference)) {
                if (name == null) return t;
                return new ParametrizedConstraintType<Integer>(t, null, reference).setName(name);
            }
            if (t.creator() != null && reference.matches(t.reference())) {
                if (name == null) 
                    return t.creator().create(reference, t.reference());
                else
                    return t.creator().create(reference, t.reference()).setName(name);
            }
        }
        return null;
    }
    
    /**
     * Teaching assignment and a section (from the assignment) combination.
     */
    static class TeachingAssignmentSection {
        private TeachingAssignment iTeachingAssignment;
        private Section iSection;
        
        TeachingAssignmentSection(TeachingAssignment ta, Section section) {
            iTeachingAssignment = ta;
            iSection = section;
        }
        
        public TeachingAssignment getTeachingAssignment() { return iTeachingAssignment; }
        public Section getSection() { return iSection; }
        public TimeLocation getTime() { return getSection().getTime(); }
        
        @Override
        public int hashCode() {
            return getTeachingAssignment().hashCode() ^ getSection().hashCode();
        }
        
        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof TeachingAssignmentSection)) return false;
            TeachingAssignmentSection tas = (TeachingAssignmentSection)o;
            return tas.getTeachingAssignment().equals(getTeachingAssignment()) && tas.getSection().equals(getSection());
        }
    }
    
    /**
     * Block of sections that go one after the other without any gap.
     */
    public static class Block {
        // start slot of the block
        private int startSlotCurrentBlock = -1;        
        // end slot of the block
        private int endSlotCurrentBlock = -1;        
        // max number of slots between classes to be considered Back-To-Back; 4 slots default      
        private int maxSlotsBetweenBackToBack = 4;
        // the list of placements of this block
        private List<TeachingAssignmentSection> sections = new ArrayList<TeachingAssignmentSection>();
        
        public Block(int maxSlotsBetweenBTB){
            this.maxSlotsBetweenBackToBack = maxSlotsBetweenBTB;            
        }              
        
        public boolean addSection(TeachingAssignmentSection section) {   
            if (section == null) return false;
            
            TimeLocation t = section.getTime();
            if (t == null) return false;
            
            // if placements is empty, the block only contains currently added placement -> set start and end
            if (sections.isEmpty()){
                sections.add(section);
                startSlotCurrentBlock = t.getStartSlot();
                endSlotCurrentBlock = t.getStartSlot() + t.getLength();
                return true;
            }
            
            // if startSlotCurrentBlock equals placement's start slot, add placement and adjust endSlotCurrentBlock
            if (t.getStartSlot() == startSlotCurrentBlock){
                sections.add(section);
                int tEnd = t.getStartSlot() + t.getLength();
                if (tEnd > endSlotCurrentBlock){
                    endSlotCurrentBlock = tEnd;
                }
                return true;
            }      
            
            // if placement starts among endSlotCurrentBlock + modifier and startSlotCurrentBlock, add the placement
            if (endSlotCurrentBlock + maxSlotsBetweenBackToBack >= t.getStartSlot() && t.getStartSlot() >= startSlotCurrentBlock ){
                sections.add(section);
                int tEnd = t.getStartSlot() + t.getLength();
                if (tEnd > endSlotCurrentBlock){
                    endSlotCurrentBlock = tEnd;
                }
                return true;
            }
            
            return false;
        }

        public boolean haveSameStartTime() {
            if (!sections.isEmpty()) {
                int startSlot = sections.get(0).getTime().getStartSlot();
                for (TeachingAssignmentSection p : getSections()) {
                    if (p.getTime().getStartSlot() != startSlot)
                        return false;
                }
            }
            return true;
        }
        
        public int getStartSlotCurrentBlock() {
            return startSlotCurrentBlock;
        }
        
        public int getEndSlotCurrentBlock() {
            return endSlotCurrentBlock;
        }
        
        public int getNbrPlacements() {
            return sections.size();
        }        
       
        public List<TeachingAssignmentSection> getSections() {
            return sections;
        }
        
        public int getLengthInSlots() {
            return endSlotCurrentBlock - startSlotCurrentBlock;
        }
        
        public boolean contains(TeachingAssignment ta) {
            for (TeachingAssignmentSection tas: getSections())
                if (tas.getTeachingAssignment().equals(ta)) return true;
            return false;
        }
        
        @Override
        public String toString(){
            return "[" + startSlotCurrentBlock + ", " + endSlotCurrentBlock + "]" + " PlacementsNbr: "+ getNbrPlacements();
        }          
    }
    
    /**
     * Returns true if the time overlaps with the provided week date pattern and days of the week.
     */
    protected static boolean shareWeeksAndDay(TimeLocation t, BitSet week, int dayCode){
        boolean matchDay = (t.getDayCode() & dayCode) != 0;
        boolean matchWeek = (week == null || t.shareWeeks(week));                
        return matchDay && matchWeek;
    }
    
    /**
     * Return teaching assignment + section combinations for the given instructor, considering the selected value and the provided conflicts.
     * The sections are sorted by time, and must meet during the given week and days of the week.
     */
    protected static Set<TeachingAssignmentSection> getRelevantPlacements(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Instructor instructor, int dayCode, Set<TeachingAssignment> conflicts, TeachingRequest.Variable variable, TeachingAssignment value, BitSet week) {
        Set<TeachingAssignmentSection> placements = new TreeSet<TeachingAssignmentSection>(new Comparator<TeachingAssignmentSection>() {
            @Override
            public int compare(TeachingAssignmentSection p1, TeachingAssignmentSection p2) {
                TimeLocation t1 = p1.getTime(), t2 = p2.getTime();
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
        });
        
        for (TeachingAssignment ta : instructor.getContext(assignment).getAssignments()) {
            // lecture of the value is already assigned
            if (variable != null && ta.variable().equals(variable)) continue;
            if (conflicts != null && conflicts.contains(ta)) continue;
            
            for (Section s: ta.variable().getSections()) {
                if (s.getTime() != null && shareWeeksAndDay(s.getTime(), week, dayCode))
                    placements.add(new TeachingAssignmentSection(ta, s));
            }
        }
        
        if (value != null) {
            for (Section s: value.variable().getSections()) {
                if (s.getTime() != null && shareWeeksAndDay(s.getTime(), week, dayCode))
                    placements.add(new TeachingAssignmentSection(value, s));
            }
        }

        return placements;
    }
    
    /**
     * Merge sorted teaching assignment + section combinations into block.
     */
    protected static List<Block> mergeToBlocks(Collection<TeachingAssignmentSection> sorted, int maxBreakBetweenBTB){
        List<Block> blocks = new ArrayList<Block>();
        // add placements to blocks
        for (TeachingAssignmentSection placement: sorted) {
            boolean added = false;
            // add placement to a suitable block
            for (int j = 0; j < blocks.size(); j++) {
                if (blocks.get(j).addSection(placement)) {
                    added = true;
                }
            }
            // create a new block if a lecture does not fit into any block
            if (!added) {
                Block block = new Block(maxBreakBetweenBTB);
                block.addSection(placement);
                blocks.add(block);
            }
        }   
        return blocks;
    }
    
    /**
     * Return blocks for an instructor, meeting the provided parameters.
     * Combining {@link GroupConstraint#getRelevantPlacements(Assignment, Instructor, int, Set, Variable, TeachingAssignment, BitSet)}
     * and {@link GroupConstraint#mergeToBlocks(Collection, int)}. 
     */
    protected static List<Block> getBlocks(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, Instructor instructor,int dayCode, Set<TeachingAssignment> conflicts, TeachingRequest.Variable variable, TeachingAssignment value, BitSet week, int maxBreakBetweenBTB) {     
        return mergeToBlocks(getRelevantPlacements(assignment, instructor, dayCode, conflicts, variable, value, week), maxBreakBetweenBTB);
    }
    
    /**
     * Check if the given assignment has a section on the given days of the week.
     */
    protected static boolean isCorectDayOfWeek(TeachingAssignment value, int dayCode) {
        if (value == null) return true;
        for (Section section: value.variable().getSections())
            if (section.getTime() != null && (dayCode == 0 || (dayCode & section.getTime().getDayCode()) != 0)) return true;
        return false;
    }
    
    /**
     * Check if the given assignment has a section on the given days of the week and weeks.
     */
    protected static boolean isCorectDayAndWeek(TeachingAssignment value, int dayCode, BitSet week) {
        if (value == null) return true;
        for (Section section: value.variable().getSections())
            if (section.getTime() != null && (dayCode == 0 || (dayCode & section.getTime().getDayCode()) != 0)
                && section.getTime().getWeekCode().intersects(week)) return true;
        return false;
    }
}
