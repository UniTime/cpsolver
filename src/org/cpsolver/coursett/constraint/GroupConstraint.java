package org.cpsolver.coursett.constraint;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.criteria.DistributionPreferences;
import org.cpsolver.coursett.criteria.StudentConflict;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.coursett.model.TimeLocation.IntEnumeration;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.WeakeningConstraint;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.DistanceMetric;
import org.cpsolver.ifs.util.ToolBox;


/**
 * Group constraint. <br>
 * This constraint expresses relations between several classes, e.g., that two
 * sections of the same lecture can not be taught at the same time, or that some
 * classes have to be taught one immediately after another. It can be either
 * hard or soft. <br>
 * <br>
 * Following constraints are now supported:
 * <table border='1'><caption>Related Solver Parameters</caption>
 * <tr>
 * <th>Constraint</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>SAME_TIME</td>
 * <td>Same time: given classes have to be taught in the same hours. If the
 * classes are of different length, the smaller one cannot start before the
 * longer one and it cannot end after the longer one.</td>
 * </tr>
 * <tr>
 * <td>SAME_DAYS</td>
 * <td>Same days: given classes have to be taught in the same day. If the
 * classes are of different time patterns, the days of one class have to form a
 * subset of the days of the other class.</td>
 * </tr>
 * <tr>
 * <td>BTB</td>
 * <td>Back-to-back constraint: given classes have to be taught in the same room
 * and they have to follow one strictly after another.</td>
 * </tr>
 * <tr>
 * <td>BTB_TIME</td>
 * <td>Back-to-back constraint: given classes have to follow one strictly after
 * another, but they can be taught in different rooms.</td>
 * </tr>
 * <tr>
 * <td>DIFF_TIME</td>
 * <td>Different time: given classes cannot overlap in time.</td>
 * </tr>
 * <tr>
 * <td>NHB(1), NHB(1.5), NHB(2), ... NHB(8)</td>
 * <td>Number of hours between: between the given classes, the exact number of
 * hours have to be kept.</td>
 * </tr>
 * <tr>
 * <td>SAME_START</td>
 * <td>Same starting hour: given classes have to start in the same hour.</td>
 * </tr>
 * <tr>
 * <td>SAME_ROOM</td>
 * <td>Same room: given classes have to be placed in the same room.</td>
 * </tr>
 * <tr>
 * <td>NHB_GTE(1)</td>
 * <td>Greater than or equal to 1 hour between: between the given classes, the
 * number of hours have to be one or more.</td>
 * </tr>
 * <tr>
 * <td>NHB_LT(6)</td>
 * <td>Less than 6 hours between: between the given classes, the number of hours
 * have to be less than six.</td>
 * </tr>
 * </table>
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
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
public class GroupConstraint extends ConstraintWithContext<Lecture, Placement, GroupConstraint.GroupConstraintContext> {
    private Long iConstraintId;
    private int iPreference;
    private ConstraintTypeInterface iType;
    private boolean iIsRequired;
    private boolean iIsProhibited;
    private int iDayOfWeekOffset = 0;
    private boolean iPrecedenceConsiderDatePatterns = true;
    private boolean iPrecedenceSkipSameDatePatternCheck = true;
    private boolean iMaxNHoursADayConsiderDatePatterns = true;
    private boolean iMaxNHoursADayPrecideComputation = false;
    private int iForwardCheckMaxDepth = 2;
    private int iForwardCheckMaxDomainSize = 1000;
    private int iNrWorkDays = 5;
    private int iFirstWorkDay = 0;
    private String iOnlineRoom = null;
    
    /**
     * Group constraints that can be checked on pairs of classes (e.g., same room means any two classes are in the same room),
     * only need to implement this interface.
     */
    public static interface PairCheck {
        /**
         * Check whether the constraint is satisfied for the given two assignments (required / preferred case)
         * @param gc Calling group constraint 
         * @param plc1 First placement
         * @param plc2 Second placement
         * @return true if constraint is satisfied
         */
        public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2);
        /**
         * Check whether the constraint is satisfied for the given two assignments (prohibited / discouraged case)
         * @param gc Calling group constraint 
         * @param plc1 First placement
         * @param plc2 Second placement
         * @return true if constraint is satisfied
         */
        public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2);
    }
    
    /**
     * Group constraints that can be checked on pairs of classes (e.g., same room means any two classes are in the same room),
     * only need to implement this interface. Unlike {@link PairCheck}, this check is also given current assignment.
     */
    public static interface AssignmentPairCheck {
        /**
         * Check whether the constraint is satisfied for the given two assignments (required / preferred case)
         * @param assignment current assignment
         * @param gc Calling group constraint 
         * @param plc1 First placement
         * @param plc2 Second placement
         * @return true if constraint is satisfied
         */
        public boolean isSatisfied(Assignment<Lecture, Placement> assignment, GroupConstraint gc, Placement plc1, Placement plc2);
        /**
         * Check whether the constraint is satisfied for the given two assignments (prohibited / discouraged case)
         * @param assignment current assignment
         * @param gc Calling group constraint 
         * @param plc1 First placement
         * @param plc2 Second placement
         * @return true if constraint is satisfied
         */
        public boolean isViolated(Assignment<Lecture, Placement> assignment, GroupConstraint gc, Placement plc1, Placement plc2);
    }
    
    /**
     * Group constraints that can have parameters need to implement this interface instead of {@link AssignmentPairCheck} or {@link PairCheck}.
     */
    public static interface AssignmentParameterPairCheck<P> {
        /**
         * Check whether the constraint is satisfied for the given two assignments (required / preferred case)
         * @param assignment current assignment
         * @param parameter constraint dependent parameter(s)
         * @param gc Calling group constraint 
         * @param plc1 First placement
         * @param plc2 Second placement
         * @return true if constraint is satisfied
         */
        public boolean isSatisfied(Assignment<Lecture, Placement> assignment, P parameter, GroupConstraint gc, Placement plc1, Placement plc2);
        /**
         * Check whether the constraint is satisfied for the given two assignments (prohibited / discouraged case)
         * @param assignment current assignment
         * @param parameter constraint dependent parameter(s)
         * @param gc Calling group constraint 
         * @param plc1 First placement
         * @param plc2 Second placement
         * @return true if constraint is satisfied
         */
        public boolean isViolated(Assignment<Lecture, Placement> assignment, P parameter, GroupConstraint gc, Placement plc1, Placement plc2);
        
        /**
         * Create constraint type with the parameters taken from the provided reference
         * @param reference constraint reference, including parameter(s)
         * @param referenceRegExp reference regular expression defined on the constraint type
         * @return constraint type with the parameter
         */
        public ParametrizedConstraintType<P> create(String reference, String referenceRegExp);
    }
    
    /**
     * Group constraint building blocks (individual constraints that need more than {@link PairCheck})
     */
    public static enum Flag {
        /** Back-to-back constraint (sequence check) */
        BACK_TO_BACK,
        /** Can share room flag */
        CAN_SHARE_ROOM,
        /** Maximum hours a day (number of slots a day check) */
        MAX_HRS_DAY,
        /** Children cannot overlap */
        CH_NOTOVERLAP,
        /** Ignore student conflicts */
        IGNORE_STUDENTS,
        ;
        /** Bit number (to combine flags) */
        int flag() { return 1 << ordinal(); }
    }
    
    /**
     * Constraint type interface
     */
    public static interface ConstraintTypeInterface extends AssignmentPairCheck {
        /** Constraint type
         * @return constraint type
         */
        public ConstraintType type();
        
        /** Constraint reference
         * @return constraint reference
         **/
        public String reference();
        
        /** Constraint name
         * @return constraint name
         **/
        public String getName();
        
        /** Minimum (gap) parameter
         * @return minimum gap (first constraint parameter)
         **/
        public int getMin();
        
        /** Maximum (gap, hours a day) parameter 
         * @return maximum gap (second constraint parameter) 
         **/
        public int getMax();
        
        /** Flag check (true if contains given flag) 
         * @param f a flag to check
         * @return true if present
         **/
        public boolean is(Flag f);
    }
    
    /**
     * Constraint type with a parameter
     */
    public static class ParametrizedConstraintType<P> implements ConstraintTypeInterface {
        private String iReference;
        private ConstraintType iType;
        private Integer iMin = null, iMax = null;
        private String iName = null;
        private P iParameter;
        
        /**
         * Constructor
         * @param type constraint type
         * @param parameter parameter parsed from the reference using {@link AssignmentParameterPairCheck#create(String, String)}
         * @param reference constraint reference with parameters
         */
        public ParametrizedConstraintType(ConstraintType type, P parameter, String reference) {
            iType = type; iParameter = parameter; iReference = reference;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean isSatisfied(Assignment<Lecture, Placement> assignment, GroupConstraint gc, Placement plc1, Placement plc2) {
            return ((AssignmentParameterPairCheck<P>)iType.iAssignmentPairCheck).isSatisfied(assignment, iParameter, gc, plc1, plc2);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean isViolated(Assignment<Lecture, Placement> assignment, GroupConstraint gc, Placement plc1, Placement plc2) {
            return ((AssignmentParameterPairCheck<P>)iType.iAssignmentPairCheck).isViolated(assignment, iParameter, gc, plc1, plc2);
        }

        /**
         * Return constraint's parameter
         * @return constraint's parameter
         */
        public P getParameter() { return iParameter; }
        @Override
        public ConstraintType type() { return iType; }
        @Override
        public String reference() { return iReference; }
        @Override
        public String getName() { return (iName == null ? iType.getName() : iName); }
        @Override
        public int getMin() { return (iMin == null ? iType.getMin() : iMin); }
        @Override
        public int getMax() { return (iMax == null ? iType.getMax() : iMax); }
        @Override
        public boolean is(Flag f) { return iType.is(f); }
        public ParametrizedConstraintType<P> setMin(int min) { iMin = min; return this; }
        public ParametrizedConstraintType<P> setMax(int max) { iMax = max; return this; }
        public ParametrizedConstraintType<P> setName(String name) { iName = name; return this; }
    }
    
    /**
     * Group constraint type.
     */
    public static enum ConstraintType implements ConstraintTypeInterface {
        /**
         * Same Time: Given classes must be taught at the same time of day (independent of the actual day the classes meet).
         * For the classes of the same length, this is the same constraint as same start. For classes of different length,
         * the shorter one cannot start before, nor end after, the longer one.<BR>
         * When prohibited or (strongly) discouraged: one class may not meet on any day at a time of day that overlaps with
         * that of the other. For example, one class can not meet M 7:30 while the other meets F 7:30. Note the difference
         * here from the different time constraint that only prohibits the actual class meetings from overlapping.
         */
        SAME_TIME("SAME_TIME", "Same Time", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return sameHours(plc1.getTimeLocation().getStartSlot(), plc1.getTimeLocation().getLength(),
                        plc2.getTimeLocation().getStartSlot(), plc2.getTimeLocation().getLength());
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return !(plc1.getTimeLocation().shareHours(plc2.getTimeLocation()));
            }}),
        /**
         * Same Days: Given classes must be taught on the same days. In case of classes of different time patterns, a class
         * with fewer meetings must meet on a subset of the days used by the class with more meetings. For example, if one
         * class pattern is 3x50, all others given in the constraint can only be taught on Monday, Wednesday, or Friday.
         * For a 2x100 class MW, MF, WF is allowed but TTh is prohibited.<BR>
         * When prohibited or (strongly) discouraged: any pair of classes classes cannot be taught on the same days (cannot
         *  overlap in days). For instance, if one class is MFW, the second has to be TTh.
         */
        SAME_DAYS("SAME_DAYS", "Same Days", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray());
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return !plc1.getTimeLocation().shareDays(plc2.getTimeLocation());
            }}),
        /**
         * Back-To-Back &amp; Same Room: Classes must be offered in adjacent time segments and must be placed in the same room.
         * Given classes must also be taught on the same days.<BR>
         * When prohibited or (strongly) discouraged: classes cannot be back-to-back. There must be at least half-hour
         * between these classes, and they must be taught on the same days and in the same room.
         */
        BTB("BTB", "Back-To-Back & Same Room", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return
                    plc1.sameRooms(plc2) &&
                    sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray());
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return
                    plc1.sameRooms(plc2) &&
                    sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray());
            }}, Flag.BACK_TO_BACK),
        /**
         * Back-To-Back: Classes must be offered in adjacent time segments but may be placed in different rooms. Given classes
         * must also be taught on the same days.<BR>
         * When prohibited or (strongly) discouraged: no pair of classes can be taught back-to-back. They may not overlap in time,
         * but must be taught on the same days. This means that there must be at least half-hour between these classes. 
         */
        BTB_TIME("BTB_TIME", "Back-To-Back", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray());
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray());
            }}, Flag.BACK_TO_BACK),
        /**
         * Different Time: Given classes cannot overlap in time. They may be taught at the same time of day if they are on
         * different days. For instance, MF 7:30 is compatible with TTh 7:30.<BR>
         * When prohibited or (strongly) discouraged: every pair of classes in the constraint must overlap in time. 
         */
        DIFF_TIME("DIFF_TIME", "Different Time", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return !plc1.getTimeLocation().hasIntersection(plc2.getTimeLocation());
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return plc1.getTimeLocation().hasIntersection(plc2.getTimeLocation());
            }}),
        /**
         * 1 Hour Between: Given classes must have exactly 1 hour in between the end of one and the beginning of another.
         * As with the <i>back-to-back time</i> constraint, given classes must be taught on the same days.<BR>
         * When prohibited or (strongly) discouraged: classes can not have 1 hour in between. They may not overlap in time
         * but must be taught on the same days.
         */
        NHB_1("NHB(1)", "1 Hour Between", 10, 12, BTB_TIME.check(), Flag.BACK_TO_BACK),
        /**
         * 2 Hours Between: Given classes must have exactly 2 hours in between the end of one and the beginning of another.
         * As with the <i>back-to-back time</i> constraint, given classes must be taught on the same days.<BR>
         * When prohibited or (strongly) discouraged: classes can not have 2 hours in between. They may not overlap in time
         * but must be taught on the same days.
         */
        NHB_2("NHB(2)", "2 Hours Between", 20, 24, BTB_TIME.check(), Flag.BACK_TO_BACK),
        /**
         * 3 Hours Between: Given classes must have exactly 3 hours in between the end of one and the beginning of another.
         * As with the <i>back-to-back time</i> constraint, given classes must be taught on the same days.<BR>
         * When prohibited or (strongly) discouraged: classes can not have 3 hours in between. They may not overlap in time
         * but must be taught on the same days.
         */
        NHB_3("NHB(3)", "3 Hours Between", 30, 36, BTB_TIME.check(), Flag.BACK_TO_BACK),
        /**
         * 4 Hours Between: Given classes must have exactly 4 hours in between the end of one and the beginning of another.
         * As with the <i>back-to-back time</i> constraint, given classes must be taught on the same days.<BR>
         * When prohibited or (strongly) discouraged: classes can not have 4 hours in between. They may not overlap in time
         * but must be taught on the same days.
         */
        NHB_4("NHB(4)", "4 Hours Between", 40, 48, BTB_TIME.check(), Flag.BACK_TO_BACK),
        /**
         * 5 Hours Between: Given classes must have exactly 5 hours in between the end of one and the beginning of another.
         * As with the <i>back-to-back time</i> constraint, given classes must be taught on the same days.<BR>
         * When prohibited or (strongly) discouraged: classes can not have 5 hours in between. They may not overlap in time
         * but must be taught on the same days.
         */
        NHB_5("NHB(5)", "5 Hours Between", 50, 60, BTB_TIME.check(), Flag.BACK_TO_BACK),
        /**
         * 6 Hours Between: Given classes must have exactly 6 hours in between the end of one and the beginning of another.
         * As with the <i>back-to-back time</i> constraint, given classes must be taught on the same days.<BR>
         * When prohibited or (strongly) discouraged: classes can not have 6 hours in between. They may not overlap in time
         * but must be taught on the same days.
         */
        NHB_6("NHB(6)", "6 Hours Between", 60, 72, BTB_TIME.check(), Flag.BACK_TO_BACK),
        /**
         * 7 Hours Between: Given classes must have exactly 7 hours in between the end of one and the beginning of another.
         * As with the <i>back-to-back time</i> constraint, given classes must be taught on the same days.<BR>
         * When prohibited or (strongly) discouraged: classes can not have 7 hours in between. They may not overlap in time
         * but must be taught on the same days.
         */
        NHB_7("NHB(7)", "7 Hours Between", 70, 84, BTB_TIME.check(), Flag.BACK_TO_BACK),
        /**
         * 8 Hours Between: Given classes must have exactly 8 hours in between the end of one and the beginning of another.
         * As with the <i>back-to-back time</i> constraint, given classes must be taught on the same days.<BR>
         * When prohibited or (strongly) discouraged: classes can not have 8 hours in between. They may not overlap in time
         * but must be taught on the same days.
         */
        NHB_8("NHB(8)", "8 Hours Between", 80, 96, BTB_TIME.check(), Flag.BACK_TO_BACK),
        /**
         * Same Start Time: Given classes must start during the same half-hour period of a day (independent of the actual
         * day the classes meet). For instance, MW 7:30 is compatible with TTh 7:30 but not with MWF 8:00.<BR>
         * When prohibited or (strongly) discouraged: any pair of classes in the given constraint cannot start during the
         * same half-hour period of any day of the week.
         */
        SAME_START("SAME_START", "Same Start Time", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return
                    (plc1.getTimeLocation().getStartSlot() % Constants.SLOTS_PER_DAY) == 
                    (plc2.getTimeLocation().getStartSlot() % Constants.SLOTS_PER_DAY);
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return
                    (plc1.getTimeLocation().getStartSlot() % Constants.SLOTS_PER_DAY) != 
                    (plc2.getTimeLocation().getStartSlot() % Constants.SLOTS_PER_DAY);
            }}),
        /**
         * Same Room: Given classes must be taught in the same room.<BR>
         * When prohibited or (strongly) discouraged: any pair of classes in the constraint cannot be taught in the same room.
         */
        SAME_ROOM("SAME_ROOM", "Same Room", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return plc1.sameRooms(plc2);
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return !plc1.sameRooms(plc2);
            }}),
        /**
         * At Least 1 Hour Between: Given classes have to have 1 hour or more in between.<BR>
         * When prohibited or (strongly) discouraged: given classes have to have less than 1 hour in between.
         */
        NHB_GTE_1("NHB_GTE(1)", "At Least 1 Hour Between", 6, 288, BTB_TIME.check(), Flag.BACK_TO_BACK),
        /**
         * Less Than 6 Hours Between: Given classes must have less than 6 hours from end of first class to the beginning of
         * the next. Given classes must also be taught on the same days.<BR>
         * When prohibited or (strongly) discouraged: given classes must have 6 or more hours between. This constraint does
         * not carry over from classes taught at the end of one day to the beginning of the next.
         */
        NHB_LT_6("NHB_LT(6)", "Less Than 6 Hours Between", 0, 72, BTB_TIME.check(), Flag.BACK_TO_BACK),
        /**
         * 1.5 Hour Between: Given classes must have exactly 90 minutes in between the end of one and the beginning of another.
         * As with the <i>back-to-back time</i> constraint, given classes must be taught on the same days.<BR>
         * When prohibited or (strongly) discouraged: classes can not have 90 minutes in between. They may not overlap in time
         * but must be taught on the same days.
         */
        NHB_1_5("NHB(1.5)", "1.5 Hour Between", 15, 18, BTB_TIME.check(), Flag.BACK_TO_BACK),
        /**
         * 4.5 Hours Between: Given classes must have exactly 4.5 hours in between the end of one and the beginning of another.
         * As with the <i>back-to-back time</i> constraint, given classes must be taught on the same days.<BR>
         * When prohibited or (strongly) discouraged: classes can not have 4.5 hours in between. They may not overlap in time
         * but must be taught on the same days.
         */
        NHB_4_5("NHB(4.5)", "4.5 Hours Between", 45, 54, BTB_TIME.check(), Flag.BACK_TO_BACK),
        /**
         * Same Students: Given classes are treated as they are attended by the same students, i.e., they cannot overlap in time
         * and if they are back-to-back the assigned rooms cannot be too far (student limit is used).
         */
        SAME_STUDENTS("SAME_STUDENTS", "Same Students", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return !JenrlConstraint.isInConflict(plc1, plc2, ((TimetableModel)gc.getModel()).getDistanceMetric(), ((TimetableModel)gc.getModel()).getStudentWorkDayLimit());
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return true;
            }}),
        /**
         * Same Instructor: Given classes are treated as they are taught by the same instructor, i.e., they cannot overlap in time
         * and if they are back-to-back the assigned rooms cannot be too far (instructor limit is used).<BR>
         * If the constraint is required and the classes are back-to-back, discouraged and strongly discouraged distances between
         * assigned rooms are also considered.
         */
        SAME_INSTR("SAME_INSTR", "Same Instructor", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                TimeLocation t1 = plc1.getTimeLocation(), t2 = plc2.getTimeLocation();
                if (t1.shareDays(t2) && t1.shareWeeks(t2)) {
                    if (t1.shareHours(t2)) return false; // overlap
                    DistanceMetric m = ((TimetableModel)gc.getModel()).getDistanceMetric();
                    if ((t1.getStartSlot() + t1.getLength() == t2.getStartSlot() || t2.getStartSlot() + t2.getLength() == t1.getStartSlot())) {
                        if (Placement.getDistanceInMeters(m, plc1, plc2) > m.getInstructorProhibitedLimit())
                            return false;
                    } else if (m.doComputeDistanceConflictsBetweenNonBTBClasses()) {
                        if (t1.getStartSlot() + t1.getLength() < t2.getStartSlot() && 
                            Placement.getDistanceInMinutes(m, plc1, plc2) > t1.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t2.getStartSlot() - t1.getStartSlot() - t1.getLength()))
                            return false;
                        if (t2.getStartSlot() + t2.getLength() < t1.getStartSlot() &&
                            Placement.getDistanceInMinutes(m, plc1, plc2) > t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (t1.getStartSlot() - t2.getStartSlot() - t2.getLength()))
                            return false;
                    }
                }
                return true;
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return true;
            }}),
        /**
         * Can Share Room: Given classes can share the room (use the room in the same time) if the room is big enough.
         */
        CAN_SHARE_ROOM("CAN_SHARE_ROOM", "Can Share Room", Flag.CAN_SHARE_ROOM),
        /**
         * Precedence: Given classes have to be taught in the given order (the first meeting of the first class has to end before
         * the first meeting of the second class etc.)<BR>
         * When prohibited or (strongly) discouraged: classes have to be taught in the order reverse to the given one.
         */
        PRECEDENCE("PRECEDENCE", "Precedence", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return gc.isPrecedence(plc1, plc2, true, true);
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return gc.isPrecedence(plc1, plc2, false, true);
            }}),
        /**
         * Back-To-Back Day: Classes must be offered on adjacent days and may be placed in different rooms.<BR>
         * When prohibited or (strongly) discouraged: classes can not be taught on adjacent days. They also can not be taught
         * on the same days. This means that there must be at least one day between these classes.
         */
        BTB_DAY("BTB_DAY", "Back-To-Back Day", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return
                    !sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray()) &&
                    gc.isBackToBackDays(plc1.getTimeLocation(), plc2.getTimeLocation());
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return
                    !sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray()) &&
                    !gc.isBackToBackDays(plc1.getTimeLocation(), plc2.getTimeLocation());
            }}),
        /**
         * Meet Together: Given classes are meeting together (same as if the given classes require constraints Can Share Room,
         * Same Room, Same Time and Same Days all together).
         */
        MEET_WITH("MEET_WITH", "Meet Together", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return
                        plc1.sameRooms(plc2) &&
                        sameHours(plc1.getTimeLocation().getStartSlot(), plc1.getTimeLocation().getLength(),
                                plc2.getTimeLocation().getStartSlot(), plc2.getTimeLocation().getLength()) &&
                        sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray());
                        
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return true;
            }}, Flag.CAN_SHARE_ROOM),
        /**
         * More Than 1 Day Between: Given classes must have two or more days in between.<br>
         * When prohibited or (strongly) discouraged: given classes must be offered on adjacent days or with at most one day in between.
         */
        NDB_GT_1("NDB_GT_1", "More Than 1 Day Between", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return
                    !sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray()) &&
                    gc.isNrDaysBetweenGreaterThanOne(plc1.getTimeLocation(), plc2.getTimeLocation());
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return
                    !sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray()) &&
                    !gc.isNrDaysBetweenGreaterThanOne(plc1.getTimeLocation(), plc2.getTimeLocation());
            }}),
        /**
         * Children Cannot Overlap: If parent classes do not overlap in time, children classes can not overlap in time as well.<BR>
         * Note: This constraint only needs to be put on the parent classes. Preferred configurations are Required All Classes
         * or Pairwise (Strongly) Preferred.
         */
        CH_NOTOVERLAP("CH_NOTOVERLAP", "Children Cannot Overlap", new AssignmentPairCheck() {
            @Override
            public boolean isSatisfied(Assignment<Lecture, Placement> assignment, GroupConstraint gc, Placement plc1, Placement plc2) {
                return gc.isChildrenNotOverlap(assignment, plc1.variable(), plc1, plc2.variable(), plc2);
            }
            @Override
            public boolean isViolated(Assignment<Lecture, Placement> assignment, GroupConstraint gc, Placement plc1, Placement plc2) {
                return true;
            }}),
        /**
         * Next Day: The second class has to be placed on the following day of the first class (if the first class is on Friday,
         * second class have to be on Monday).<br>
         * When prohibited or (strongly) discouraged: The second class has to be placed on the previous day of the first class
         * (if the first class is on Monday, second class have to be on Friday).<br>
         * Note: This constraint works only between pairs of classes.
         */
        FOLLOWING_DAY("FOLLOWING_DAY", "Next Day", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return gc.isFollowingDay(plc1, plc2, true);
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return gc.isFollowingDay(plc1, plc2, false);
            }}),
        /**
         * Two Days After: The second class has to be placed two days after the first class (Monday &rarr; Wednesday, Tuesday &rarr; 
         * Thurday, Wednesday &rarr; Friday, Thursday &rarr; Monday, Friday &rarr; Tuesday).<br>
         * When prohibited or (strongly) discouraged: The second class has to be placed two days before the first class (Monday &rarr;
         * Thursday, Tuesday &rarr; Friday, Wednesday &rarr; Monday, Thursday &rarr; Tuesday, Friday &rarr; Wednesday).<br>
         * Note: This constraint works only between pairs of classes.
         */
        EVERY_OTHER_DAY("EVERY_OTHER_DAY", "Two Days After", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return gc.isEveryOtherDay(plc1, plc2, true);
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return gc.isEveryOtherDay(plc1, plc2, false);
            }}),
        /**
         * At Most 3 Hours A Day: Classes are to be placed in a way that there is no more than three hours in any day.
         */
       MAX_HRS_DAY_3("MAX_HRS_DAY(3)", "At Most 3 Hours A Day", 36, null, Flag.MAX_HRS_DAY),        
       /**
        * At Most 4 Hours A Day: Classes are to be placed in a way that there is no more than four hours in any day.
        */
       MAX_HRS_DAY_4("MAX_HRS_DAY(4)", "At Most 4 Hours A Day", 48, null, Flag.MAX_HRS_DAY),        
       /**
         * At Most 5 Hours A Day: Classes are to be placed in a way that there is no more than five hours in any day.
         */
       MAX_HRS_DAY_5("MAX_HRS_DAY(5)", "At Most 5 Hours A Day", 60, null, Flag.MAX_HRS_DAY),        
       /**
        * At Most 6 Hours A Day: Classes are to be placed in a way that there is no more than six hours in any day.
        */
       MAX_HRS_DAY_6("MAX_HRS_DAY(6)", "At Most 6 Hours A Day", 72, null, Flag.MAX_HRS_DAY),
       /**
        * At Most 7 Hours A Day: Classes are to be placed in a way that there is no more than seven hours in any day.
        */
       MAX_HRS_DAY_7("MAX_HRS_DAY(7)", "At Most 7 Hours A Day", 84, null, Flag.MAX_HRS_DAY),
       /**
        * At Most 8 Hours A Day: Classes are to be placed in a way that there is no more than eight hours in any day.
        */
       MAX_HRS_DAY_8("MAX_HRS_DAY(8)", "At Most 8 Hours A Day", 96, null, Flag.MAX_HRS_DAY),
       /**
        * At Most 9 Hours A Day: Classes are to be placed in a way that there is no more than nine hours in any day.
        */
       MAX_HRS_DAY_9("MAX_HRS_DAY(9)", "At Most 9 Hours A Day", 108, null, Flag.MAX_HRS_DAY),
       /**
        * At Most 10 Hours A Day: Classes are to be placed in a way that there is no more than ten hours in any day.
        */
       MAX_HRS_DAY_10("MAX_HRS_DAY(10)", "At Most 10 Hours A Day", 120, null, Flag.MAX_HRS_DAY),
        /**
         * At Most X Hours A Day: Classes are to be placed in a way that there is no more than given number of hours in any day.
         */
        MAX_HRS_DAY("MAX_HRS_DAY\\(([0-9\\.]+)\\)", "At Most N Hours A Day", new AssignmentParameterPairCheck<Integer>() {
            @Override
            public boolean isSatisfied(Assignment<Lecture, Placement> assignment, Integer parameter, GroupConstraint gc, Placement plc1, Placement plc2) {
                return true;
            }
            @Override
            public boolean isViolated(Assignment<Lecture, Placement> assignment, Integer parameter, GroupConstraint gc, Placement plc1, Placement plc2) {
                return true;
            }
            @Override
            public ParametrizedConstraintType<Integer> create(String reference, String regexp) {
                Matcher matcher = Pattern.compile(regexp).matcher(reference);
                if (matcher.find()) {
                    double hours = Double.parseDouble(matcher.group(1));
                    int slots = (int)Math.round(12.0 * hours);
                    return new ParametrizedConstraintType<Integer>(ConstraintType.MAX_HRS_DAY, slots, reference)
                            .setName("At Most " + matcher.group(1) + " Hours A Day")
                            .setMin(slots).setMax(slots);
                }
                return null;
            }}, Flag.MAX_HRS_DAY),
        /**
         * Given classes must be taught during the same weeks (i.e., must have the same date pattern).<br>
         * When prohibited or (strongly) discouraged: any two classes must have non overlapping date patterns.
         */
        SAME_WEEKS("SAME_WEEKS", "Same Weeks", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return plc1.getTimeLocation().getWeekCode().equals(plc2.getTimeLocation().getWeekCode());
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return !plc1.getTimeLocation().shareWeeks(plc2.getTimeLocation());
            }}),
        /**
         * Classes (of different courses) are to be attended by the same students. For instance,
         * if class A1 (of a course A) and class B1 (of a course B) are linked, a student requesting
         * both courses must attend A1 if and only if he also attends B1. This is a student sectioning
         * constraint that is interpreted as Same Students constraint during course timetabling.
         */
        LINKED_SECTIONS("LINKED_SECTIONS", "Linked Classes", SAME_STUDENTS.check()),
        /**
         * Back-To-Back Precedence: Given classes have to be taught in the given order, on the same days,
         * and in adjacent time segments.
         * When prohibited or (strongly) discouraged: Given classes have to be taught in the given order,
         * on the same days, but cannot be back-to-back.
         */
        BTB_PRECEDENCE("BTB_PRECEDENCE", "Back-To-Back Precedence", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return gc.isPrecedence(plc1, plc2, true, false) && sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray());
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return gc.isPrecedence(plc1, plc2, true, false) && sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray());
            }}, Flag.BACK_TO_BACK),   
            
        /**
         * Same Days-Time: Given classes must be taught at the same time of day and on the same days.
         * It is the combination of Same Days and Same Time distribution preferences.     
         * When prohibited or (strongly) discouraged: Any pair of classes classes cannot be taught on the same days
         * during the same time.
         */             
        SAME_DAYS_TIME("SAME_D_T", "Same Days-Time", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return sameHours(plc1.getTimeLocation().getStartSlot(), plc1.getTimeLocation().getLength(),
                        plc2.getTimeLocation().getStartSlot(), plc2.getTimeLocation().getLength()) &&
                        sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray());
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return !plc1.getTimeLocation().shareHours(plc2.getTimeLocation()) ||
                        !plc1.getTimeLocation().shareDays(plc2.getTimeLocation());
            }}),
        /**
         * Same Days-Room-Time: Given classes must be taught at the same time of day, on the same days and in the same room.
         * It is the combination of Same Days, Same Time and Same Room distribution preferences.
         * Note that this constraint is the same as Meet Together constraint, except it does not allow room sharing. In other words,
         * it is only useful when these classes are taught during non-overlapping date patterns.
         * When prohibited or (strongly) discouraged: Any pair of classes classes cannot be taught on the same days 
         * during the same time in the same room.
         */            
        SAME_DAYS_ROOM_TIME("SAME_D_R_T", "Same Days-Room-Time", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return sameHours(plc1.getTimeLocation().getStartSlot(), plc1.getTimeLocation().getLength(),
                        plc2.getTimeLocation().getStartSlot(), plc2.getTimeLocation().getLength()) &&
                        sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray()) &&
                        plc1.sameRooms(plc2);
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return !plc1.getTimeLocation().shareHours(plc2.getTimeLocation()) ||
                        !plc1.getTimeLocation().shareDays(plc2.getTimeLocation()) ||
                        !plc1.sameRooms(plc2);
            }}),
        /**
         * 6 Hour Work Day: Classes are to be placed in a way that there is no more than six hours between the start of the first class and the end of the class one on any day.
         */
        WORKDAY_6("WORKDAY(6)", "6 Hour Work Day", 72, new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                TimeLocation t1 = plc1.getTimeLocation(), t2 = plc2.getTimeLocation();
                if (t1 == null || t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) return true;
                return Math.max(t1.getStartSlot() + t1.getLength(), t2.getStartSlot() + t2.getLength()) - Math.min(t1.getStartSlot(), t2.getStartSlot()) <= gc.getType().getMax();
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) { return true; }
            }),
        /**
         * 7 Hour Work Day: Classes are to be placed in a way that there is no more than seven hours between the start of the first class and the end of the class one on any day.
         */
        WORKDAY_7("WORKDAY(7)", "7 Hour Work Day", 84, WORKDAY_6.check()),
        /**
         * 8 Hour Work Day: Classes are to be placed in a way that there is no more than eight hours between the start of the first class and the end of the class one on any day.
         */
        WORKDAY_8("WORKDAY(8)", "8 Hour Work Day", 96, WORKDAY_6.check()),
        /**
         * 9 Hour Work Day: Classes are to be placed in a way that there is no more than nine hours between the start of the first class and the end of the class one on any day.
         */
        WORKDAY_9("WORKDAY(9)", "9 Hour Work Day", 108, WORKDAY_6.check()),
        /**
         * 10 Hour Work Day: Classes are to be placed in a way that there is no more than ten hours between the start of the first class and the end of the class one on any day.
         */
        WORKDAY_10("WORKDAY(10)", "10 Hour Work Day", 120, WORKDAY_6.check()),
        /**
         * 11 Hour Work Day: Classes are to be placed in a way that there is no more than eleven hours between the start of the first class and the end of the class one on any day.
         */
        WORKDAY_11("WORKDAY(11)", "11 Hour Work Day", 132, WORKDAY_6.check()),
        /**
         * 12 Hour Work Day: Classes are to be placed in a way that there is no more than twelve hours between the start of the first class and the end of the class one on any day.
         */
        WORKDAY_12("WORKDAY(12)", "12 Hour Work Day", 144, WORKDAY_6.check()),
        /**
         * 4 Hour Work Day: Classes are to be placed in a way that there is no more than four hours between the start of the first class and the end of the class one on any day.
         */
        WORKDAY_4("WORKDAY(4)", "4 Hour Work Day", 48, WORKDAY_6.check()),
        /**
         * 5 Hour Work Day: Classes are to be placed in a way that there is no more than five hours between the start of the first class and the end of the class one on any day.
         */
        WORKDAY_5("WORKDAY(5)", "5 Hour Work Day", 60, WORKDAY_6.check()),
          /**
         * Work Day: Classes are to be placed in a way that there is no more than given number of hours between the start of the first class and the end of the class one on any day.
         */
        WORKDAY("WORKDAY\\(([0-9\\.]+)\\)", "Work Day", new AssignmentParameterPairCheck<Integer>() {
            @Override
            public boolean isSatisfied(Assignment<Lecture, Placement> assignment, Integer parameter, GroupConstraint gc, Placement plc1, Placement plc2) {
                TimeLocation t1 = plc1.getTimeLocation(), t2 = plc2.getTimeLocation();
                if (t1 == null || t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) return true;
                return Math.max(t1.getStartSlot() + t1.getLength(), t2.getStartSlot() + t2.getLength()) - Math.min(t1.getStartSlot(), t2.getStartSlot()) <= parameter;
            }
            @Override
            public boolean isViolated(Assignment<Lecture, Placement> assignment, Integer parameter, GroupConstraint gc, Placement plc1, Placement plc2) {
                return true;
            }
            @Override
            public ParametrizedConstraintType<Integer> create(String reference, String regexp) {
                Matcher matcher = Pattern.compile(regexp).matcher(reference);
                if (matcher.find()) {
                    double hours = Double.parseDouble(matcher.group(1));
                    int slots = (int)Math.round(12.0 * hours);
                    return new ParametrizedConstraintType<Integer>(ConstraintType.WORKDAY, slots, reference)
                            .setName(matcher.group(1) + " Hour Work Day").setMin(slots).setMax(slots);
                }
                return null;
            }}),
        /**
         * Meet Together &amp; Same Weeks: Given classes are meeting together (same as if the given classes require constraints Can Share Room,
         * Same Room, Same Time, Same Days and Same Weeks all together).
         */
        MEET_WITH_WEEKS("MEET_WITH_WEEKS", "Meet Together & Same Weeks", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return
                        plc1.sameRooms(plc2) &&
                        sameHours(plc1.getTimeLocation().getStartSlot(), plc1.getTimeLocation().getLength(), plc2.getTimeLocation().getStartSlot(), plc2.getTimeLocation().getLength()) &&
                        sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray()) &&
                        plc1.getTimeLocation().getWeekCode().equals(plc2.getTimeLocation().getWeekCode());
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return true;
            }}, Flag.CAN_SHARE_ROOM),
        MIN_GAP("MIN_GAP\\(([0-9\\.]+)\\)", "Mininal Gap Between Classes", new AssignmentParameterPairCheck<Integer>() {
            @Override
            public boolean isSatisfied(Assignment<Lecture, Placement> assignment, Integer parameter, GroupConstraint gc, Placement plc1, Placement plc2) {
                TimeLocation t1 = plc1.getTimeLocation(), t2 = plc2.getTimeLocation();
                if (t1 == null || t2 == null || !t1.shareDays(t2) || !t1.shareWeeks(t2)) return true;
                return t1.getStartSlot() + t1.getLength() + parameter <= t2.getStartSlot() ||
                        t2.getStartSlot() + t2.getLength() + parameter <= t1.getStartSlot();
            }
            @Override
            public boolean isViolated(Assignment<Lecture, Placement> assignment, Integer parameter, GroupConstraint gc, Placement plc1, Placement plc2) { return true; }
            @Override
            public ParametrizedConstraintType<Integer> create(String reference, String regexp) {
                Matcher matcher = Pattern.compile(regexp).matcher(reference);
                if (matcher.find()) {
                    double hours = Double.parseDouble(matcher.group(1));
                    int slots = (int)Math.round(12.0 * hours);
                    return new ParametrizedConstraintType<Integer>(ConstraintType.MIN_GAP, slots, reference)
                            .setName("At Least " + matcher.group(1) + " Hours Between Classes")
                            .setMin(slots).setMax(slots);
                }
                return null;
            }}),
        /**
         * Given classes must be taught on weeks that are back-to-back (the gap between the two assigned date patterns is less than a week).<br>
         * When prohibited or (strongly) discouraged: any two classes must have at least a week gap in between.
         */
        BTB_WEEKS("BTB_WEEKS", "Back-To-Back Weeks", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                if (gc.variables().size() <= 2) {
                    return gc.isBackToBackWeeks(plc1.getTimeLocation(), plc2.getTimeLocation());
                } else {
                    int totalWeeks = 0;
                    for (Lecture l: gc.variables())
                        totalWeeks += l.getMinWeeks();
                    return gc.isMaxWeekSpan(plc1.getTimeLocation(), plc2.getTimeLocation(), totalWeeks);
                }
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return gc.isNotBackToBackWeeks(plc1.getTimeLocation(), plc2.getTimeLocation());
            }}),
        /**
         * Given classes must be taught on weeks that are back-to-back and in the given order.<br>
         * When prohibited or (strongly) discouraged: given classes must be taught on weeks in the given order with at least one week between any two following classes.
         */
        FOLLOWING_WEEKS("FOLLOWING_WEEKS", "Following Weeks", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return gc.isFollowingWeeksBTB(plc1, plc2, true);
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return gc.isFollowingWeeksBTB(plc1, plc2, false);
            }}),
        /**
         * Given classes must be taught on the same dates. If one of the classes meets more often, the class meeting less often can only meet on the dates when the other class is meeting.<br>
         * When prohibited or (strongly) discouraged: given classes cannot be taught on the same days (there cannot be a date when both classes are meeting).<br>
         * Note: unlike with the same days/weeks constraint, this constraint consider individual meeting dates of both classes.
         */
        SAME_DATES("SAME_DATES", "Same Dates", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return gc.isSameDates(plc1.getTimeLocation(), plc2.getTimeLocation());
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return gc.isDifferentDates(plc1.getTimeLocation(), plc2.getTimeLocation());
            }}),
        /**
         * Same Days-Room-Start: Given classes must start at the same time of day, on the same days and in the same room.
         * It is the combination of Same Days, Same Start and Same Room distribution preferences.
         * When prohibited or (strongly) discouraged: Any pair of classes classes cannot be taught on the same days 
         * during the same time in the same room.
         */
        SAME_DAYS_ROOM_START("SAME_DAY_ROOM_START", "Same Days-Room-Start", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return (plc1.getTimeLocation().getStartSlot() % Constants.SLOTS_PER_DAY) == 
                        (plc2.getTimeLocation().getStartSlot() % Constants.SLOTS_PER_DAY) &&
                        sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray()) &&
                        plc1.sameRooms(plc2);
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return !plc1.getTimeLocation().shareHours(plc2.getTimeLocation()) ||
                        !plc1.getTimeLocation().shareDays(plc2.getTimeLocation()) ||
                        !plc1.sameRooms(plc2);
            }}),
        /**
         * Overnight: The constraint has two parameters: hours and distance in minutes. There is a problem when
         * an evening class is followed by a morning class the next day and the time in between is less then the
         * given number of hours, but only when the distance in minutes between them is greater than the
         * given number of minutes.
         */
        DAYBREAK("DAYBREAK\\(([0-9\\.]+),(-?[0-9]+)\\)", "Daybreak", new AssignmentParameterPairCheck<Integer[]>() {
            @Override
            public boolean isSatisfied(Assignment<Lecture, Placement> assignment, Integer[] param, GroupConstraint gc, Placement plc1, Placement plc2) {
                TimeLocation t1 = plc1.getTimeLocation();
                TimeLocation t2 = plc2.getTimeLocation();
                if (288 + t2.getStartSlot() - t1.getStartSlot() - t1.getLength() < gc.getType().getMin()) { // close to each other
                    if (gc.isNextDay(t1, t2)) { // next day
                        if (gc.getType().getMax() < 0) { // no distance check
                            return false;
                        } else {
                            DistanceMetric m = ((TimetableModel)gc.getModel()).getDistanceMetric();
                            if (Placement.getDistanceInMinutes(m, plc1, plc2) > gc.getType().getMax()) { // distance check
                                return false;
                            }
                        }
                    }
                } else if (288 + t1.getStartSlot() - t2.getStartSlot() - t2.getLength() < gc.getType().getMin()) { // close to each other, but the other way around
                    if (gc.isNextDay(t2, t1)) { // next day
                        if (gc.getType().getMax() < 0) { // no distance check
                            return false;
                        } else {
                            DistanceMetric m = ((TimetableModel)gc.getModel()).getDistanceMetric();
                            if (Placement.getDistanceInMinutes(m, plc2, plc1) > gc.getType().getMax()) { // distance check
                                return false;
                            }
                        }
                    }
                }
                return true;
            }
            @Override
            public boolean isViolated(Assignment<Lecture, Placement> assignment, Integer[] parameter, GroupConstraint gc, Placement plc1, Placement plc2) {
                return true;
            }
            @Override
            public ParametrizedConstraintType<Integer[]> create(String reference, String regexp) {
                Matcher matcher = Pattern.compile(regexp).matcher(reference);
                if (matcher.find()) {
                    double hours = Double.parseDouble(matcher.group(1));
                    int distanceSlots = (int)Math.round(12.0 * hours);
                    int distanceInMinutes = Integer.parseInt(matcher.group(2));
                    return new ParametrizedConstraintType<Integer[]>(ConstraintType.DAYBREAK, 
                            new Integer[] {distanceSlots, distanceInMinutes}, reference)
                            .setName("Daybreak of " + ( distanceSlots / 12.0) + " hours" + (distanceInMinutes >= 0 ? " when over " + distanceInMinutes + " mins": ""))
                            .setMin(distanceSlots).setMax(distanceInMinutes);
                }
                return null;
            }}),
            /**
             * Online/Offline Room: Given classes, if scheduled on the same day, must be all in the online room or
             * none of them can be in the online room. This means there is a conflict when two classes
             * are placed on the same day, but one is in online room and the other is not.
             */
            ONLINE_ROOM("ONLINE_ROOM", "Online/Offline Room", new PairCheck() {
                @Override
                public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                    TimeLocation t1 = plc1.getTimeLocation();
                    TimeLocation t2 = plc2.getTimeLocation();
                    if (t1.shareDays(t2) && t1.shareWeeks(t2)) {
                        return gc.isOnline(plc1) == gc.isOnline(plc2);
                    } else {
                        // different days > do not care
                        return true;
                    }
                }
                @Override
                public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) { return true; }
            }),
            /**
             * Same Days-Time-Weeks: The given classes must be taught at the same time of day, on the same days, and on the same weeks
             * (i.e., they must follow the same date pattern).
             * It is the combination of Same Days, Same Time, and Same Weeks distribution preferences.
             * When prohibited or (strongly) discouraged: Any pair of classes cannot be taught on the same days
             * during the same time and during overlapping date patterns. In other words, the given classes cannot overlap.
             */
            SAME_DATE_TIME_WEEKS("SAME_DTW", "Same Days-Time-Weeks", new PairCheck() {
                @Override
                public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                    return sameHours(plc1.getTimeLocation().getStartSlot(), plc1.getTimeLocation().getLength(),
                            plc2.getTimeLocation().getStartSlot(), plc2.getTimeLocation().getLength()) &&
                            sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray()) &&
                            plc1.getTimeLocation().getWeekCode().equals(plc2.getTimeLocation().getWeekCode());
                }
                @Override
                public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                    return !plc1.getTimeLocation().shareHours(plc2.getTimeLocation()) ||
                            !plc1.getTimeLocation().shareDays(plc2.getTimeLocation()) ||
                            !plc1.getTimeLocation().shareWeeks(plc2.getTimeLocation());
                }}),
            /**
             * Same Students w/o Distance: This distribution is the same as the Same Students distribution,
             * except there is no distance conflict checking and no work-day limit.
             * Also, the distribution gets ignored when there is the Ignore Student Conflicts distribution between the two classes.
             * This constraint can be used as the Same Student Constraint in the solver configuration, e.g., in the interactive mode,
             * replacing the Different Time constraint which does not ignore cases when there is the Ignore Student Conflicts between
             * two classes in a parent-child relation
             */
            SAME_STUD_NODST("SAME_STUD_NODST", "Same Students No Distance", new PairCheck() {
                @Override
                public boolean isSatisfied(GroupConstraint gc, Placement p1, Placement p2) {
                    return p1 == null || p2 == null || StudentConflict.ignore(p1.variable(), p2.variable()) || !StudentConflict.overlaps(p1, p2);
                }
                @Override
                public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                    return true;
                }}),
            /**
             * Different Time with Ignore Student Conflicts: Combination of two constraints, Different Time and
             * Ignore Student Conflicts. Given classes cannot overlap in time, replacing any student conflicts between
             * these classes.
             * When prohibited or (strongly) discouraged: every pair of classes in the constraint must overlap in time.
             * Still, student conflicts are ignored.
             */
            DIFF_TIME_IGN_STUDS("DIFF_TIME_IGN_STUDS", "Different Time + Ignore Student Conflicts", new PairCheck() {
                @Override
                public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                    return !plc1.getTimeLocation().hasIntersection(plc2.getTimeLocation());
                }
                @Override
                public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                    return plc1.getTimeLocation().hasIntersection(plc2.getTimeLocation());
                }}, Flag.IGNORE_STUDENTS),
            /**
             * Given classes must be taught on dates that are back-to-back and in the given order.<br>
             * When prohibited or (strongly) discouraged: given classes must be taught on dates in the given order with at least one day between any two following classes.<br>
             * The primary use for this constraint are for classes that meet only once. 
             * The constraint will fail if one of the meetings falls on a holiday and the other does not.
             */
            FOLLOWING_DATES("FOLLOWING_DATES", "Following Dates", new PairCheck() {
                @Override
                public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                    return gc.isFollowingDates(plc1, plc2, true);
                }
                @Override
                public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                    return gc.isFollowingDates(plc1, plc2, false);
                }}),
        ;
        
        String iReference, iName;
        int iFlag = 0;
        Flag[] iFlags = null;
        int iMin = 0, iMax = 0;
        PairCheck iCheck = null;
        AssignmentPairCheck iAssignmentCheck = null;
        AssignmentParameterPairCheck<?> iAssignmentPairCheck = null;
        ConstraintType(String reference, String name, Flag... flags) {
            iReference = reference;
            iName = name;
            iFlags = flags;
            for (Flag f: flags)
                iFlag |= f.flag();
        }
        ConstraintType(String reference, String name, PairCheck check, Flag... flags) {
            this(reference, name, flags);
            iCheck = check;
        }
        ConstraintType(String reference, String name, AssignmentPairCheck check, Flag... flags) {
            this(reference, name, flags);
            iAssignmentCheck = check;
        }
        ConstraintType(String reference, String name, int limit, PairCheck check, Flag... flags) {
            this(reference, name, check, flags);
            iMin = iMax = limit;
        }
        ConstraintType(String reference, String name, int min, int max, PairCheck check, Flag... flags) {
            this(reference, name, check, flags);
            iMin = min;
            iMax = max;
        }
        ConstraintType(String reference, String name, AssignmentParameterPairCheck<?> check, Flag... flags) {
            this(reference, name, flags);
            iAssignmentPairCheck = check;
        }
        
        /**
         * Constraint type
         * @return constraint type
         */
        @Override
        public ConstraintType type() { return this; }

        /** Constraint reference
         * @return constraint reference
         **/
        @Override
        public String reference() { return iReference; }
        
        /** Constraint name
         * @return constraint name
         **/
        @Override
        public String getName() { return iName; }
        
        /** Minimum (gap) parameter
         * @return minimum gap (first constraint parameter)
         **/
        @Override
        public int getMin() { return iMin; }
        
        /** Maximum (gap, hours a day) parameter 
         * @return maximum gap (second constraint parameter) 
         **/
        @Override
        public int getMax() { return iMax; }
        
        /** Flag check (true if contains given flag) 
         * @param f a flag to check
         * @return true if present
         **/
        @Override
        public boolean is(Flag f) { return (iFlag & f.flag()) != 0; }

        /** Constraint type from reference 
         * @param reference constraint reference
         * @return constraint of the reference
         * @deprecated use {@link GroupConstraint#getConstraintType(String)} instead
         **/
        @Deprecated
        public static ConstraintType get(String reference) {
            for (ConstraintType t: ConstraintType.values())
                if (t.reference().equals(reference)) return t;
            return null;
        }
        
        /** True if a required or preferred constraint is satisfied between a pair of placements 
         * @param assignment current assignment
         * @param gc current constraint
         * @param plc1 first placement
         * @param plc2 second placement
         * @return true if the two placements are consistent with the constraint if preferred or required 
         **/ 
        @Override
        public boolean isSatisfied(Assignment<Lecture, Placement> assignment, GroupConstraint gc, Placement plc1, Placement plc2) {
            if (iCheck != null && !iCheck.isSatisfied(gc, plc1, plc2))
                return false;
            if (iAssignmentCheck != null && assignment != null && !iAssignmentCheck.isSatisfied(assignment, gc, plc1, plc2))
                return false;
            return true;
        }
        
        /** True if a prohibited or discouraged constraint is satisfied between a pair of placements 
         * @param assignment current assignment
         * @param gc current constraint
         * @param plc1 first placement
         * @param plc2 second placement
         * @return true if the two placements are consistent with the constraint if discouraged or prohibited 
         **/ 
        @Override
        public boolean isViolated(Assignment<Lecture, Placement> assignment, GroupConstraint gc, Placement plc1, Placement plc2) { 
            if (iCheck != null && !iCheck.isViolated(gc, plc1, plc2))
                return false;
            if (iAssignmentCheck != null && assignment != null && !iAssignmentCheck.isViolated(assignment, gc, plc1, plc2))
                return false;
            return true;
        }
        /** Pair check */
        private PairCheck check() { return iCheck; }
    }
    
    /** Constraint type from reference 
     * @param reference constraint reference
     * @return constraint of the reference
     **/
    public static ConstraintTypeInterface getConstraintType(String reference) {
        for (ConstraintType t: ConstraintType.values()) {
            if (t.reference().equals(reference)) return t;
            if (t.iAssignmentPairCheck != null && reference.matches(t.reference()))
                return t.iAssignmentPairCheck.create(reference, t.reference());
        }
        return null;
    }    

    public GroupConstraint() {
    }
    
    @Override
    public void setModel(Model<Lecture, Placement> model) {
        super.setModel(model);
        if (model != null) {
            DataProperties config = ((TimetableModel)model).getProperties();
            iDayOfWeekOffset = config.getPropertyInt("DatePattern.DayOfWeekOffset", 0);
            iPrecedenceConsiderDatePatterns = config.getPropertyBoolean("Precedence.ConsiderDatePatterns", true);
            iPrecedenceSkipSameDatePatternCheck = config.getPropertyBoolean("Precedence.SkipSameDatePatternCheck", true);
            iForwardCheckMaxDepth = config.getPropertyInt("ForwardCheck.MaxDepth", iForwardCheckMaxDepth);
            iForwardCheckMaxDomainSize = config.getPropertyInt("ForwardCheck.MaxDomainSize", iForwardCheckMaxDomainSize);
            iMaxNHoursADayPrecideComputation = config.getPropertyBoolean("MaxNHoursADay.PreciseComputation", iMaxNHoursADayPrecideComputation);
            iMaxNHoursADayConsiderDatePatterns = config.getPropertyBoolean("MaxNHoursADay.ConsiderDatePatterns", iMaxNHoursADayConsiderDatePatterns);
            iNrWorkDays = (config.getPropertyInt("General.LastWorkDay", 4) - config.getPropertyInt("General.FirstWorkDay", 0) + 1);
            if (iNrWorkDays <= 0) iNrWorkDays += 7;
            if (iNrWorkDays > 7) iNrWorkDays -= 7;
            iFirstWorkDay = config.getPropertyInt("General.FirstWorkDay", 0);
            iOnlineRoom = config.getProperty("General.OnlineRoom", "(?i)ONLINE|");
        }
    }

    @Override
    public void addVariable(Lecture lecture) {
        if (!variables().contains(lecture))
            super.addVariable(lecture);
        if (getType().is(Flag.CH_NOTOVERLAP)) {
            if (lecture.getChildrenSubpartIds() != null) {
                for (Long subpartId: lecture.getChildrenSubpartIds()) {
                    for (Lecture ch : lecture.getChildren(subpartId)) {
                        if (!variables().contains(ch))
                            super.addVariable(ch);
                    }
                }
            }
        }
    }

    @Override
    public void removeVariable(Lecture lecture) {
        if (variables().contains(lecture))
            super.removeVariable(lecture);
        if (getType().is(Flag.CH_NOTOVERLAP)) {
            if (lecture.getChildrenSubpartIds() != null) {
                for (Long subpartId: lecture.getChildrenSubpartIds()) {
                    for (Lecture ch : lecture.getChildren(subpartId)) {
                        if (variables().contains(ch))
                            super.removeVariable(ch);
                    }
                }
            }
        }
    }

    /**
     * Constructor
     * 
     * @param id
     *            constraint id
     * @param type
     *            constraString type (e.g, {@link ConstraintType#SAME_TIME})
     * @param preference
     *            time preference ("R" for required, "P" for prohibited, "-2",
     *            "-1", "1", "2" for soft preference)
     */
    public GroupConstraint(Long id, ConstraintTypeInterface type, String preference) {
        iConstraintId = id;
        iType = type;
        iIsRequired = preference.equals(Constants.sPreferenceRequired);
        iIsProhibited = preference.equals(Constants.sPreferenceProhibited);
        iPreference = Constants.preference2preferenceLevel(preference);
    }

    /** Constraint id 
     * @return constraint unique id
     **/
    public Long getConstraintId() {
        return iConstraintId;
    }

    @Override
    public long getId() {
        return (iConstraintId == null ? -1 : iConstraintId.longValue());
    }
    
    /** Generated unique id 
     * @return generated unique id
     **/
    protected long getGeneratedId() {
        return iId;
    }

    /** Return constraint type (e.g, {@link ConstraintType#SAME_TIME}) 
     * @return constraint type
     **/
    public ConstraintTypeInterface getType() {
        return iType;
    }

    /**
     * Set constraint type
     * @param type constraint type
     */
    public void setType(ConstraintType type) {
        iType = type;
    }

    /** Is constraint required 
     * @return true if required
     **/
    public boolean isRequired() {
        return iIsRequired;
    }

    /** Is constraint prohibited 
     * @return true if prohibited
     **/
    public boolean isProhibited() {
        return iIsProhibited;
    }

    /**
     * Prolog reference: "R" for required, "P" for prohibited", "-2",.."2" for
     * preference
     * @return prolog preference
     */
    public String getPrologPreference() {
        return Constants.preferenceLevel2preference(iPreference);
    }

    @Override
    public boolean isConsistent(Placement value1, Placement value2) {
        if (!isHard())
            return true;
        if (!isSatisfiedPair(null, value1, value2))
            return false;
        if (getType().is(Flag.BACK_TO_BACK)) {
            HashMap<Lecture, Placement> assignments = new HashMap<Lecture, Placement>();
            assignments.put(value1.variable(), value1);
            assignments.put(value2.variable(), value2);
            if (!isSatisfiedSeq(null, assignments, null))
                return false;
        }
        if (getType().is(Flag.MAX_HRS_DAY)) {
            HashMap<Lecture, Placement> assignments = new HashMap<Lecture, Placement>();
            assignments.put(value1.variable(), value1);
            assignments.put(value2.variable(), value2);
            for (int dayCode: Constants.DAY_CODES) {
                if (iMaxNHoursADayPrecideComputation) {
                    for (IntEnumeration dates = value1.getTimeLocation().getDates(iDayOfWeekOffset); dates.hasMoreElements(); ) {
                        int date = dates.nextElement();
                        if (!value2.getTimeLocation().hasDate(date, iDayOfWeekOffset)) continue;
                        if (nrSlotsADay(null, date, assignments, null) > getType().getMax()) return false;
                    }
                } else if (iMaxNHoursADayConsiderDatePatterns) {
                    for (BitSet week: ((TimetableModel)getModel()).getWeeks()) {
                        if (!value1.getTimeLocation().shareWeeks(week) && !value2.getTimeLocation().shareWeeks(week)) continue;
                        if (nrSlotsADay(null, dayCode, week, assignments, null) > getType().getMax()) return false;
                    }
                } else {
                    if (nrSlotsADay(null, dayCode, null, assignments, null) > getType().getMax()) return false;
                }
            }
        }
        return true;
    }

    @Override
    public void computeConflicts(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        computeConflicts(assignment, value, conflicts, true);
    }
    
    @Override
    public void computeConflictsNoForwardCheck(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        computeConflicts(assignment, value, conflicts, false);
    }
    
    public void computeConflicts(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts, boolean fwdCheck) {
        if (!isHard())
            return;
        for (Lecture v : variables()) {
            if (v.equals(value.variable()))
                continue; // ignore this variable
            Placement p = assignment.getValue(v);
            if (p == null)
                continue; // there is an unassigned variable -- great, still a chance to get violated
            if (!isSatisfiedPair(assignment, p, value))
                conflicts.add(p);
        }
        if (getType().is(Flag.BACK_TO_BACK)) {
            HashMap<Lecture, Placement> assignments = new HashMap<Lecture, Placement>();
            assignments.put(value.variable(), value);
            if (!isSatisfiedSeq(assignment, assignments, conflicts))
                conflicts.add(value);
        }
        if (getType().is(Flag.MAX_HRS_DAY)) {
            HashMap<Lecture, Placement> assignments = new HashMap<Lecture, Placement>();
            assignments.put(value.variable(), value);
            for (int dayCode: Constants.DAY_CODES) {
                if (iMaxNHoursADayPrecideComputation) {
                    for (IntEnumeration dates = value.getTimeLocation().getDates(iDayOfWeekOffset); dates.hasMoreElements(); ) {
                        int date = dates.nextElement();
                        if (nrSlotsADay(assignment, date, assignments, conflicts) > getType().getMax()) {
                            List<Placement> adepts = new ArrayList<Placement>();
                            for (Lecture l: variables()) {
                                if (l.equals(value.variable()) || l.isConstant()) continue;
                                Placement p = assignment.getValue(l);
                                if (p == null || conflicts.contains(p) || p.getTimeLocation() == null) continue;
                                if (!p.getTimeLocation().hasDate(date, iDayOfWeekOffset)) continue;
                                adepts.add(p);
                            }
                            do {
                                if (adepts.isEmpty()) { conflicts.add(value); break; }
                                Placement conflict = ToolBox.random(adepts);
                                adepts.remove(conflict);
                                conflicts.add(conflict);
                            } while (nrSlotsADay(assignment, date, assignments, conflicts) > getType().getMax());
                        }
                    }
                } else if (iMaxNHoursADayConsiderDatePatterns) {
                    for (BitSet week: ((TimetableModel)getModel()).getWeeks()) {
                        if (!value.getTimeLocation().shareWeeks(week)) continue;
                        if (nrSlotsADay(assignment, dayCode, week, assignments, conflicts) > getType().getMax()) {
                            List<Placement> adepts = new ArrayList<Placement>();
                            for (Lecture l: variables()) {
                                if (l.equals(value.variable()) || l.isConstant()) continue;
                                Placement p = assignment.getValue(l);
                                if (p == null || conflicts.contains(p) || p.getTimeLocation() == null) continue;
                                if ((p.getTimeLocation().getDayCode() & dayCode) == 0 || !p.getTimeLocation().shareWeeks(week)) continue;
                                adepts.add(p);
                            }
                            do {
                                if (adepts.isEmpty()) { conflicts.add(value); break; }
                                Placement conflict = ToolBox.random(adepts);
                                adepts.remove(conflict);
                                conflicts.add(conflict);
                            } while (nrSlotsADay(assignment, dayCode, week, assignments, conflicts) > getType().getMax());
                        }
                    }
                } else {
                    if (nrSlotsADay(assignment, dayCode, null, assignments, conflicts) > getType().getMax()) {
                        List<Placement> adepts = new ArrayList<Placement>();
                        for (Lecture l: variables()) {
                            if (l.equals(value.variable()) || l.isConstant()) continue;
                            Placement p = assignment.getValue(l);
                            if (p == null || conflicts.contains(p) || p.getTimeLocation() == null) continue;
                            if ((p.getTimeLocation().getDayCode() & dayCode) == 0) continue;
                            adepts.add(p);
                        }
                        do {
                            if (adepts.isEmpty()) { conflicts.add(value); break; }
                            Placement conflict = ToolBox.random(adepts);
                            adepts.remove(conflict);
                            conflicts.add(conflict);
                        } while (nrSlotsADay(assignment, dayCode, null, assignments, conflicts) > getType().getMax());
                    }
                }
            }
        }
        
        // Forward checking
        if (fwdCheck) forwardCheck(assignment, value, conflicts, new HashSet<GroupConstraint>(), iForwardCheckMaxDepth - 1);
    }
    
    public void forwardCheck(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts, Set<GroupConstraint> ignore, int depth) {
        try {
            if (depth < 0) return;
            ignore.add(this);
            
            List<Placement> neededSize = null;
            
            for (Lecture lecture: variables()) {
                if (conflicts.contains(value)) break; // already conflicting

                if (lecture.equals(value.variable())) continue; // Skip this lecture
                Placement current = assignment.getValue(lecture);
                if (current != null) { // Has assignment, check whether it is conflicting
                    if (isSatisfiedPair(assignment, value, current)) {
                        // Increase needed size if the assignment is of the same room and overlapping in time
                        if (canShareRoom() && sameRoomAndOverlaps(value, current)) {
                            if (neededSize == null) neededSize = new ArrayList<Placement>(); 
                            neededSize.add(current);
                        }
                        continue;
                    }
                    conflicts.add(current);
                }
                
                // Look for supporting assignments assignment
                boolean shareRoomAndOverlaps = canShareRoom();
                Placement support = null;
                int nrSupports = 0;
                if (lecture.nrValues() >= iForwardCheckMaxDomainSize) {
                    // ignore variables with large domains
                    return;
                }
                List<Placement> values = lecture.values(assignment);
                if (values.isEmpty()) {
                    // ignore variables with empty domain
                    return;
                }
                for (Placement other: values) {
                    if (nrSupports < 2) {
                        if (isSatisfiedPair(assignment, value, other)) {
                            if (support == null) support = other;
                            nrSupports ++;
                            if (shareRoomAndOverlaps && !sameRoomAndOverlaps(value, other))
                                shareRoomAndOverlaps = false;
                        }
                    } else if (shareRoomAndOverlaps && !sameRoomAndOverlaps(value, other) && isSatisfiedPair(assignment, value, other)) {
                        shareRoomAndOverlaps = false;
                    }
                    if (nrSupports > 1 && !shareRoomAndOverlaps)
                        break;
                }
                
                // No supporting assignment -> fail
                if (nrSupports == 0) {
                    conflicts.add(value); // other class cannot be assigned with this value
                    return;
                }
                // Increase needed size if all supporters are of the same room and in overlapping times
                if (shareRoomAndOverlaps && support != null) {
                    if (neededSize == null) neededSize = new ArrayList<Placement>(); 
                    neededSize.add(support);
                }

                // Only one supporter -> propagate the new assignment over other hard constraints of the lecture
                if (nrSupports == 1) {
                    for (Constraint<Lecture, Placement> other: lecture.hardConstraints()) {
                        if (other instanceof WeakeningConstraint) continue;
                        if (other instanceof GroupConstraint) {
                            GroupConstraint gc = (GroupConstraint)other;
                            if (depth > 0 && !ignore.contains(gc))
                                gc.forwardCheck(assignment, support, conflicts, ignore, depth - 1);
                        } else {
                            other.computeConflicts(assignment, support, conflicts);
                        }
                    }
                    for (GlobalConstraint<Lecture, Placement> other: getModel().globalConstraints()) {
                        if (other instanceof WeakeningConstraint) continue;
                        other.computeConflicts(assignment, support, conflicts);
                    }

                    if (conflicts.contains(support))
                        conflicts.add(value);
                }
            }
            
            if (canShareRoom() && neededSize != null) {
                if (value.getRoomLocations() != null) {
                    for (RoomLocation room: value.getRoomLocations())
                        if (room.getRoomConstraint() != null && !room.getRoomConstraint().checkRoomSize(value, neededSize)) {
                            // room is too small to fit all meet with classes
                            conflicts.add(value);
                        }
                } else if (value.getRoomLocation() != null) {
                    RoomLocation room = value.getRoomLocation();
                    if (room.getRoomConstraint() != null && !room.getRoomConstraint().checkRoomSize(value, neededSize)) {
                        // room is too small to fit all meet with classes
                        conflicts.add(value);
                    }
                }
            }
        } finally {
            ignore.remove(this);
        }
    }

    @Override
    public boolean inConflict(Assignment<Lecture, Placement> assignment, Placement value) {
        if (!isHard())
            return false;
        for (Lecture v : variables()) {
            if (v.equals(value.variable()))
                continue; // ignore this variable
            Placement p = assignment.getValue(v);
            if (p == null)
                continue; // there is an unassigned variable -- great, still a chance to get violated
            if (!isSatisfiedPair(assignment, p, value))
                return true;
        }
        if (getType().is(Flag.BACK_TO_BACK)) {
            HashMap<Lecture, Placement> assignments = new HashMap<Lecture, Placement>();
            assignments.put(value.variable(), value);
            if (!isSatisfiedSeq(assignment, assignments, null))
                return true;
        }
        if (getType().is(Flag.MAX_HRS_DAY)) {
            HashMap<Lecture, Placement> assignments = new HashMap<Lecture, Placement>();
            assignments.put(value.variable(), value);
            for (int dayCode: Constants.DAY_CODES) {
                if (iMaxNHoursADayPrecideComputation) {
                    for (IntEnumeration dates = value.getTimeLocation().getDates(iDayOfWeekOffset); dates.hasMoreElements(); ) {
                        int date = dates.nextElement();
                        if (nrSlotsADay(assignment,date, assignments, null) > getType().getMax())
                            return true;
                    }
                } else if (iMaxNHoursADayConsiderDatePatterns) {
                    for (BitSet week: ((TimetableModel)getModel()).getWeeks()) {
                        if (!value.getTimeLocation().shareWeeks(week)) continue;
                        if (nrSlotsADay(assignment, dayCode, week, assignments, null) > getType().getMax())
                            return true;
                    }
                } else {
                    if (nrSlotsADay(assignment, dayCode, null, assignments, null) > getType().getMax()) return true;
                }
            }
        }
        
        if (!forwardCheck(assignment, value, new HashSet<GroupConstraint>(), iForwardCheckMaxDepth - 1)) return true;
        
        return false;
    }
    
    public boolean forwardCheck(Assignment<Lecture, Placement> assignment, Placement value, Set<GroupConstraint> ignore, int depth) {
        try {
            if (depth < 0) return true;
            ignore.add(this);
            
            int neededSize = value.variable().maxRoomUse();
            
            for (Lecture lecture: variables()) {
                if (lecture.equals(value.variable())) continue; // Skip this lecture
                Placement current = assignment.getValue(lecture);
                if (current != null) { // Has assignment, check whether it is conflicting
                    if (isSatisfiedPair(assignment, value, current)) {
                        // Increase needed size if the assignment is of the same room and overlapping in time
                        if (canShareRoom() && sameRoomAndOverlaps(value, current)) {
                            neededSize += lecture.maxRoomUse();
                        }
                        continue;
                    }
                    return false;
                }
                
                // Look for supporting assignments assignment
                boolean shareRoomAndOverlaps = canShareRoom();
                Placement support = null;
                int nrSupports = 0;
                if (lecture.nrValues() >= iForwardCheckMaxDomainSize) {
                    // ignore variables with large domains
                    return true;
                }
                List<Placement> values = lecture.values(assignment);
                if (values.isEmpty()) {
                    // ignore variables with empty domain
                    return true;
                }
                for (Placement other: lecture.values(assignment)) {
                    if (nrSupports < 2) {
                        if (isSatisfiedPair(assignment, value, other)) {
                            if (support == null) support = other;
                            nrSupports ++;
                            if (shareRoomAndOverlaps && !sameRoomAndOverlaps(value, other))
                                shareRoomAndOverlaps = false;
                        }
                    } else if (shareRoomAndOverlaps && !sameRoomAndOverlaps(value, other) && isSatisfiedPair(assignment, value, other)) {
                        shareRoomAndOverlaps = false;
                    }
                    if (nrSupports > 1 && !shareRoomAndOverlaps)
                        break;
                }

                // No supporting assignment -> fail
                if (nrSupports == 0) {
                    return false; // other class cannot be assigned with this value
                }
                // Increase needed size if all supporters are of the same room and in overlapping times
                if (shareRoomAndOverlaps) {
                    neededSize += lecture.maxRoomUse();
                }

                // Only one supporter -> propagate the new assignment over other hard constraints of the lecture
                if (nrSupports == 1) {
                    for (Constraint<Lecture, Placement> other: lecture.hardConstraints()) {
                        if (other instanceof WeakeningConstraint) continue;
                        if (other instanceof GroupConstraint) {
                            GroupConstraint gc = (GroupConstraint)other;
                            if (depth > 0 && !ignore.contains(gc) && !gc.forwardCheck(assignment, support, ignore, depth - 1)) return false;
                        } else {
                            if (other.inConflict(assignment, support)) return false;
                        }
                    }
                    for (GlobalConstraint<Lecture, Placement> other: getModel().globalConstraints()) {
                        if (other instanceof WeakeningConstraint) continue;
                        if (other.inConflict(assignment, support)) return false;
                    }
                }
            }
            
            if (canShareRoom() && neededSize > value.getRoomSize()) {
                // room is too small to fit all meet with classes
                return false;
            }
         
            return true;
        } finally {
            ignore.remove(this);
        }
    }

    /** Constraint preference (0 if prohibited or required) 
     * @return constraint preference (if soft)
     **/
    public int getPreference() {
        return iPreference;
    }

    /**
     * Current constraint preference (0 if prohibited or required, depends on
     * current satisfaction of the constraint)
     * @param assignment current assignment
     * @return current preference
     */
    public int getCurrentPreference(Assignment<Lecture, Placement> assignment) {
        if (isHard()) return 0; // no preference
        if (countAssignedVariables(assignment) < 2) return - Math.abs(iPreference); // not enough variable
        if (getType().is(Flag.MAX_HRS_DAY)) { // max hours a day
            int over = 0;
            for (int dayCode: Constants.DAY_CODES) {
                if (iMaxNHoursADayPrecideComputation) {
                    Set<Integer> allDates = new HashSet<Integer>();
                    for (Lecture v1 : variables()) {
                        Placement p1 = assignment.getValue(v1);
                        if (p1 == null) continue;
                        for (IntEnumeration dates = p1.getTimeLocation().getDates(iDayOfWeekOffset); dates.hasMoreElements(); ) {
                            int date = dates.nextElement();
                            if (allDates.add(date))
                                over += Math.max(0, nrSlotsADay(assignment, date, null, null) - getType().getMax());
                        }
                    }
                } else if (iMaxNHoursADayConsiderDatePatterns) {
                    for (BitSet week: ((TimetableModel)getModel()).getWeeks())
                        over += Math.max(0, nrSlotsADay(assignment, dayCode, week, null, null) - getType().getMax());
                } else {
                    over += Math.max(0, nrSlotsADay(assignment, dayCode, null, null, null) - getType().getMax());
                }
            }
            return (over > 0 ? Math.abs(iPreference) * over / 12 : - Math.abs(iPreference));
        }
        int nrViolatedPairs = 0;
        for (Lecture v1 : variables()) {
            Placement p1 = assignment.getValue(v1);
            if (p1 == null) continue;
            for (Lecture v2 : variables()) {
                Placement p2 = assignment.getValue(v2);
                if (p2 == null || v1.getId() >= v2.getId()) continue;
                if (!isSatisfiedPair(assignment, p1, p2)) nrViolatedPairs++;
            }
        }
        if (getType().is(Flag.BACK_TO_BACK)) {
            Set<Placement> conflicts = new HashSet<Placement>();
            if (isSatisfiedSeq(assignment, new HashMap<Lecture, Placement>(), conflicts))
                nrViolatedPairs += conflicts.size();
            else
                nrViolatedPairs = variables().size();
        }
        return (nrViolatedPairs > 0 ? Math.abs(iPreference) * nrViolatedPairs : - Math.abs(iPreference));
    }

    /** Current constraint preference change (if given placement is assigned) 
     * @param assignment current assignment
     * @param placement placement that is being considered
     * @return change in the current preference, if assigned 
     **/
    public int getCurrentPreference(Assignment<Lecture, Placement> assignment, Placement placement) {
        if (isHard()) return 0; // no preference
        if (countAssignedVariables(assignment) + (assignment.getValue(placement.variable()) == null ? 1 : 0) < 2) return 0; // not enough variable
        if (getType().is(Flag.MAX_HRS_DAY)) {
            HashMap<Lecture, Placement> assignments = new HashMap<Lecture, Placement>();
            assignments.put(placement.variable(), placement);
            HashMap<Lecture, Placement> unassignments = new HashMap<Lecture, Placement>();
            unassignments.put(placement.variable(), null);
            int after = 0;
            int before = 0;
            for (int dayCode: Constants.DAY_CODES) {
                if (iMaxNHoursADayPrecideComputation) {
                    for (IntEnumeration dates = placement.getTimeLocation().getDates(iDayOfWeekOffset); dates.hasMoreElements(); ) {
                        int date = dates.nextElement();
                        after += Math.max(0, nrSlotsADay(assignment, date, assignments, null) - getType().getMax());
                        before += Math.max(0, nrSlotsADay(assignment, date, unassignments, null) - getType().getMax());
                    }
                } else if (iMaxNHoursADayConsiderDatePatterns) {
                    for (BitSet week: ((TimetableModel)getModel()).getWeeks()) {
                        after += Math.max(0, nrSlotsADay(assignment, dayCode, week, assignments, null) - getType().getMax());
                        before += Math.max(0, nrSlotsADay(assignment, dayCode, week, unassignments, null) - getType().getMax());
                    }
                } else {
                    after += Math.max(0, nrSlotsADay(assignment, dayCode, null, assignments, null) - getType().getMax());
                    before += Math.max(0, nrSlotsADay(assignment, dayCode, null, unassignments, null) - getType().getMax());
                }
            }
            return (after > 0 ? Math.abs(iPreference) * after / 12 : - Math.abs(iPreference)) - (before > 0 ? Math.abs(iPreference) * before / 12 : - Math.abs(iPreference));
        }
        
        int nrViolatedPairsAfter = 0;
        int nrViolatedPairsBefore = 0;
        for (Lecture v1 : variables()) {
            for (Lecture v2 : variables()) {
                if (v1.getId() >= v2.getId()) continue;
                Placement p1 = (v1.equals(placement.variable()) ? null : assignment.getValue(v1));
                Placement p2 = (v2.equals(placement.variable()) ? null : assignment.getValue(v2));
                if (p1 != null && p2 != null && !isSatisfiedPair(assignment, p1, p2))
                    nrViolatedPairsBefore ++;
                if (v1.equals(placement.variable())) p1 = placement;
                if (v2.equals(placement.variable())) p2 = placement;
                if (p1 != null && p2 != null && !isSatisfiedPair(assignment, p1, p2))
                    nrViolatedPairsAfter ++;
            }
        }
        
        if (getType().is(Flag.BACK_TO_BACK)) {
            HashMap<Lecture, Placement> assignments = new HashMap<Lecture, Placement>();
            assignments.put(placement.variable(), placement);
            Set<Placement> conflicts = new HashSet<Placement>();
            if (isSatisfiedSeq(assignment, assignments, conflicts))
                nrViolatedPairsAfter += conflicts.size();
            else
                nrViolatedPairsAfter = variables().size();
            
            HashMap<Lecture, Placement> unassignments = new HashMap<Lecture, Placement>();
            unassignments.put(placement.variable(), null);
            Set<Placement> previous = new HashSet<Placement>();
            if (isSatisfiedSeq(assignment, unassignments, previous))
                nrViolatedPairsBefore += previous.size();
            else
                nrViolatedPairsBefore = variables().size();
        }
        
        return (nrViolatedPairsAfter > 0 ? Math.abs(iPreference) * nrViolatedPairsAfter : - Math.abs(iPreference)) -
                (nrViolatedPairsBefore > 0 ? Math.abs(iPreference) * nrViolatedPairsBefore : - Math.abs(iPreference));
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getName());
        sb.append(" between ");
        for (Iterator<Lecture> e = variables().iterator(); e.hasNext();) {
            Lecture v = e.next();
            sb.append(v.getName());
            if (e.hasNext())
                sb.append(", ");
        }
        return sb.toString();
    }

    @Override
    public boolean isHard() {
        return iIsRequired || iIsProhibited;
    }

    @Override
    public String getName() {
        return getType().getName();
    }


    private boolean isPrecedence(Placement p1, Placement p2, boolean firstGoesFirst, boolean considerDatePatterns) {
        int ord1 = variables().indexOf(p1.variable());
        int ord2 = variables().indexOf(p2.variable());
        TimeLocation t1 = null, t2 = null;
        if (ord1 < ord2) {
            if (firstGoesFirst) {
                t1 = p1.getTimeLocation();
                t2 = p2.getTimeLocation();
            } else {
                t2 = p1.getTimeLocation();
                t1 = p2.getTimeLocation();
            }
        } else {
            if (!firstGoesFirst) {
                t1 = p1.getTimeLocation();
                t2 = p2.getTimeLocation();
            } else {
                t2 = p1.getTimeLocation();
                t1 = p2.getTimeLocation();
            }
        }
        if (considerDatePatterns && iPrecedenceConsiderDatePatterns) {
            if (iPrecedenceSkipSameDatePatternCheck) {
                int m1 = t1.getFirstMeeting(iDayOfWeekOffset), m2 = t2.getFirstMeeting(iDayOfWeekOffset);
                if (m1 != m2) return m1 < m2;
            } else {
                boolean sameDatePattern = (t1.getDatePatternId() != null ? t1.getDatePatternId().equals(t2.getDatePatternId()) : t1.getWeekCode().equals(t2.getWeekCode()));
                if (!sameDatePattern) {
                    int m1 = t1.getFirstMeeting(iDayOfWeekOffset), m2 = t2.getFirstMeeting(iDayOfWeekOffset);
                    if (m1 != m2) return m1 < m2;
                }
            }
        }
        if (iFirstWorkDay != 0) {
            for (int i = 0; i < Constants.DAY_CODES.length; i++) {
                int idx = (i + iFirstWorkDay) % 7;
                boolean a = (t1.getDayCode() & Constants.DAY_CODES[idx]) != 0;
                boolean b = (t2.getDayCode() & Constants.DAY_CODES[idx]) != 0;
                if (b && !a) return false; // second start first
                if (a && !b) return true;  // first start first
                if (a && b) return t1.getStartSlot() + t1.getLength() <= t2.getStartSlot(); // same day: check times
            }
        }
        return t1.getStartSlots().nextElement() + t1.getLength() <= t2.getStartSlots().nextElement();
    }

    private boolean isBackToBackDays(TimeLocation t1, TimeLocation t2) {
        int f1 = -1, f2 = -1, e1 = -1, e2 = -1;
        for (int i = 0; i < Constants.DAY_CODES.length; i++) {
            int idx = (i + iFirstWorkDay) % 7;
            if ((t1.getDayCode() & Constants.DAY_CODES[idx]) != 0) {
                if (f1 < 0)
                    f1 = i;
                e1 = i;
            }
            if ((t2.getDayCode() & Constants.DAY_CODES[idx]) != 0) {
                if (f2 < 0)
                    f2 = i;
                e2 = i;
            }
        }
        return (e1 + 1 == f2) || (e2 + 1 == f1);
    }
    
    private boolean isNrDaysBetweenGreaterThanOne(TimeLocation t1, TimeLocation t2) {
        int f1 = -1, f2 = -1, e1 = -1, e2 = -1;
        for (int i = 0; i < Constants.DAY_CODES.length; i++) {
            int idx = (i + iFirstWorkDay) % 7;
            if ((t1.getDayCode() & Constants.DAY_CODES[idx]) != 0) {
                if (f1 < 0)
                    f1 = i;
                e1 = i;
            }
            if ((t2.getDayCode() & Constants.DAY_CODES[idx]) != 0) {
                if (f2 < 0)
                    f2 = i;
                e2 = i;
            }
        }
        return (e1 - f2 > 2) || (e2 - f1 > 2);
    }

    private boolean isFollowingDay(Placement p1, Placement p2, boolean firstGoesFirst) {
        int ord1 = variables().indexOf(p1.variable());
        int ord2 = variables().indexOf(p2.variable());
        TimeLocation t1 = null, t2 = null;
        if (ord1 < ord2) {
            if (firstGoesFirst) {
                t1 = p1.getTimeLocation();
                t2 = p2.getTimeLocation();
            } else {
                t2 = p1.getTimeLocation();
                t1 = p2.getTimeLocation();
            }
        } else {
            if (!firstGoesFirst) {
                t1 = p1.getTimeLocation();
                t2 = p2.getTimeLocation();
            } else {
                t2 = p1.getTimeLocation();
                t1 = p2.getTimeLocation();
            }
        }
        int f1 = -1, f2 = -1, e1 = -1;
        for (int i = 0; i < Constants.DAY_CODES.length; i++) {
            int idx = (i + iFirstWorkDay) % 7;
            if ((t1.getDayCode() & Constants.DAY_CODES[idx]) != 0) {
                if (f1 < 0)
                    f1 = i;
                e1 = i;
            }
            if ((t2.getDayCode() & Constants.DAY_CODES[idx]) != 0) {
                if (f2 < 0)
                    f2 = i;
            }
        }
        return ((e1 + 1) % iNrWorkDays == f2);
    }
    
    private boolean isNextDay(TimeLocation t1, TimeLocation t2) {
        if (iPrecedenceConsiderDatePatterns) {
            for (Enumeration<Integer> e = t1.getDates(iDayOfWeekOffset); e.hasMoreElements(); ) {
                Integer date = e.nextElement();
                if (t2.hasDate(date + 1, iDayOfWeekOffset)) return true;
            }
            return false;
        }
        for (int i = 0; i < Constants.DAY_CODES.length; i++) {
            int i1 = (i + iFirstWorkDay) % 7;
            int i2 = (1 + i1) % 7;
            boolean a = (t1.getDayCode() & Constants.DAY_CODES[i1]) != 0;
            boolean b = (t2.getDayCode() & Constants.DAY_CODES[i2]) != 0;
            if (a && b) return true;
        }
        return false;
    }

    private boolean isEveryOtherDay(Placement p1, Placement p2, boolean firstGoesFirst) {
        int ord1 = variables().indexOf(p1.variable());
        int ord2 = variables().indexOf(p2.variable());
        TimeLocation t1 = null, t2 = null;
        if (ord1 < ord2) {
            if (firstGoesFirst) {
                t1 = p1.getTimeLocation();
                t2 = p2.getTimeLocation();
            } else {
                t2 = p1.getTimeLocation();
                t1 = p2.getTimeLocation();
            }
        } else {
            if (!firstGoesFirst) {
                t1 = p1.getTimeLocation();
                t2 = p2.getTimeLocation();
            } else {
                t2 = p1.getTimeLocation();
                t1 = p2.getTimeLocation();
            }
        }
        int f1 = -1, f2 = -1, e1 = -1;
        for (int i = 0; i < Constants.DAY_CODES.length; i++) {
            int idx = (i + iFirstWorkDay) % 7;
            if ((t1.getDayCode() & Constants.DAY_CODES[idx]) != 0) {
                if (f1 < 0)
                    f1 = i;
                e1 = i;
            }
            if ((t2.getDayCode() & Constants.DAY_CODES[idx]) != 0) {
                if (f2 < 0)
                    f2 = i;
            }
        }
        return ((e1 + 2) % iNrWorkDays == f2);
    }

    private static boolean sameDays(int[] days1, int[] days2) {
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
    
    private static boolean sameRoomAndOverlaps(Placement p1, Placement p2) {
        return p1.shareRooms(p2) && p1.getTimeLocation() != null && p2.getTimeLocation() != null && p1.getTimeLocation().hasIntersection(p2.getTimeLocation());
    }

    private static boolean sameHours(int start1, int len1, int start2, int len2) {
        if (len1 > len2)
            return sameHours(start2, len2, start1, len1);
        start1 %= Constants.SLOTS_PER_DAY;
        start2 %= Constants.SLOTS_PER_DAY;
        return (start1 >= start2 && start1 + len1 <= start2 + len2);
    }
    
    private static boolean canFill(int totalGap, int gapMin, int gapMax, List<Set<Integer>> lengths) {
        if (gapMin <= totalGap && totalGap <= gapMax)
            return true;
        if (totalGap < 2 * gapMin)
            return false;
        for (int i = 0; i < lengths.size(); i++) {
            Set<Integer> length = lengths.get(i);
            lengths.remove(i);
            for (int gap = gapMin; gap <= gapMax; gap++)
                for (Integer l: length)
                    if (canFill(totalGap - gap - l, gapMin, gapMax, lengths))
                        return true;
            lengths.add(i, length);
        }
        return false;
    }

    private boolean isSatisfiedSeq(Assignment<Lecture, Placement> assignment, HashMap<Lecture, Placement> assignments, Set<Placement> conflicts) {
        if (conflicts == null)
            return isSatisfiedSeqCheck(assignment, assignments, conflicts);
        else {
            Set<Placement> bestConflicts = isSatisfiedRecursive(assignment, 0, assignments, conflicts,
                    new HashSet<Placement>(), null);
            if (bestConflicts == null)
                return false;
            conflicts.addAll(bestConflicts);
            return true;
        }
    }

    private Set<Placement> isSatisfiedRecursive(Assignment<Lecture, Placement> assignment, int idx, HashMap<Lecture, Placement> assignments,
            Set<Placement> conflicts, Set<Placement> newConflicts, Set<Placement> bestConflicts) {
        if (idx == variables().size() && newConflicts.isEmpty())
            return bestConflicts;
        if (isSatisfiedSeqCheck(assignment, assignments, conflicts)) {
            if (bestConflicts == null) {
                return new HashSet<Placement>(newConflicts);
            } else {
                int b = 0, n = 0;
                for (Placement value: assignments.values()) {
                    if (value != null && bestConflicts.contains(value)) b++;
                    if (value != null && newConflicts.contains(value)) n++;
                }
                if (n < b || (n == b && newConflicts.size() < bestConflicts.size()))
                    return new HashSet<Placement>(newConflicts);
            }
            return bestConflicts;
        }
        if (idx == variables().size())
            return bestConflicts;
        bestConflicts = isSatisfiedRecursive(assignment, idx + 1, assignments, conflicts, newConflicts,
                bestConflicts);
        Lecture lecture = variables().get(idx);
        //if (assignments != null && assignments.containsKey(lecture))
        //    return bestConflicts;
        Placement placement = null;
        if (assignments != null && assignments.containsKey(lecture))
            placement = assignments.get(lecture);
        else if (assignment != null)
            placement = assignment.getValue(lecture);
        if (placement == null)
            return bestConflicts;
        if (conflicts != null && conflicts.contains(placement))
            return bestConflicts;
        conflicts.add(placement);
        newConflicts.add(placement);
        bestConflicts = isSatisfiedRecursive(assignment, idx + 1, assignments, conflicts, newConflicts, bestConflicts);
        newConflicts.remove(placement);
        conflicts.remove(placement);
        return bestConflicts;
    }

    private boolean isSatisfiedSeqCheck(Assignment<Lecture, Placement> assignment, HashMap<Lecture, Placement> assignments, Set<Placement> conflicts) {
        if (!getType().is(Flag.BACK_TO_BACK)) return true;
        int gapMin = getType().getMin();
        int gapMax = getType().getMax();

        List<Set<Integer>> lengths = new ArrayList<Set<Integer>>();

        Placement[] res = new Placement[Constants.SLOTS_PER_DAY];
        for (int i = 0; i < Constants.SLOTS_PER_DAY; i++)
            res[i] = null;

        int nrLectures = 0;

        for (Lecture lecture : variables()) {
            Placement placement = null;
            if (assignments != null && assignments.containsKey(lecture))
                placement = assignments.get(lecture);
            else if (assignment != null)
                placement = assignment.getValue(lecture);
            if (placement == null) {
            	if (!lecture.timeLocations().isEmpty()) {
            	    Set<Integer> l = new HashSet<Integer>();
            	    for (TimeLocation time: lecture.timeLocations())
            	        l.add(time.getLength());
            	    lengths.add(l);
            	}
            } else if (conflicts != null && conflicts.contains(placement)) {
            	if (!lecture.timeLocations().isEmpty()) {
            	    Set<Integer> l = new HashSet<Integer>();
            	    for (TimeLocation time: lecture.timeLocations())
            	        l.add(time.getLength());
            	    lengths.add(l);
            	}
            } else {
                int pos = placement.getTimeLocation().getStartSlot();
                int length = placement.getTimeLocation().getLength();
                for (int j = pos; j < pos + length; j++) {
                    if (res[j] != null) {
                        if (conflicts == null)
                            return false;
                        if (!assignments.containsKey(lecture))
                            conflicts.add(placement);
                        else if (!assignments.containsKey(res[j].variable()))
                            conflicts.add(res[j]);
                    }
                }
                for (int j = pos; j < pos + length; j++)
                    res[j] = placement;
                nrLectures++;
            }
        }
        if (nrLectures <= 1)
            return true;

        if (iIsRequired || (!iIsProhibited && iPreference < 0)) {
            int i = 0;
            Placement p = res[i];
            while (p == null)
                p = res[++i];
            i = res[i].getTimeLocation().getStartSlot() + res[i].getTimeLocation().getLength();
            nrLectures--;
            while (nrLectures > 0) {
                int gap = 0;
                while (i < Constants.SLOTS_PER_DAY && res[i] == null) {
                    gap++;
                    i++;
                }
                if (i == Constants.SLOTS_PER_DAY)
                    break;
                if (!canFill(gap, gapMin, gapMax, lengths))
                    return false;
                p = res[i];
                i = res[i].getTimeLocation().getStartSlot() + res[i].getTimeLocation().getLength();
                nrLectures--;
            }
        } else if (iIsProhibited || (!iIsRequired && iPreference > 0)) {
            int i = 0;
            Placement p = res[i];
            while (p == null)
                p = res[++i];
            i = res[i].getTimeLocation().getStartSlot() + res[i].getTimeLocation().getLength();
            nrLectures--;
            while (nrLectures > 0) {
                int gap = 0;
                while (i < Constants.SLOTS_PER_DAY && res[i] == null) {
                    gap++;
                    i++;
                }
                if (i == Constants.SLOTS_PER_DAY)
                    break;
                if ((gapMin == 0 || !canFill(gap, 0, gapMin - 1, lengths))
                        && (gapMax >= Constants.SLOTS_PER_DAY || !canFill(gap, gapMax + 1, Constants.SLOTS_PER_DAY,
                                lengths))) {
                    return false;
                }
                p = res[i];
                i = res[i].getTimeLocation().getStartSlot() + res[i].getTimeLocation().getLength();
                nrLectures--;
            }
        }
        return true;
    }

    public boolean isSatisfied(Assignment<Lecture, Placement> assignment) {
        if (isHard()) return true;
        if (countAssignedVariables(assignment) < 2) return true;
        if (getPreference() == 0) return true;
        return isHard() || countAssignedVariables(assignment) < 2 || getPreference() == 0 || getCurrentPreference(assignment) < 0;
    }

    public boolean isChildrenNotOverlap(Assignment<Lecture, Placement> assignment, Lecture lec1, Placement plc1, Lecture lec2, Placement plc2) {
        if (lec1.getSchedulingSubpartId().equals(lec2.getSchedulingSubpartId())) {
            // same subpart
            boolean overlap = plc1.getTimeLocation().hasIntersection(plc2.getTimeLocation());

            if (overlap && lec1.getParent() != null && variables().contains(lec1.getParent())
                    && lec2.getParent() != null && variables().contains(lec2.getParent())) {
                // children overlaps
                Placement p1 = assignment.getValue(lec1.getParent());
                Placement p2 = assignment.getValue(lec2.getParent());
                // parents not overlap, but children do
                if (p1 != null && p2 != null && !p1.getTimeLocation().hasIntersection(p2.getTimeLocation()))
                    return false;
            }

            if (!overlap && lec1.getChildrenSubpartIds() != null && lec2.getChildrenSubpartIds() != null) {
                // parents not overlap
                for (Long subpartId: lec1.getChildrenSubpartIds()) {
                    for (Lecture c1 : lec1.getChildren(subpartId)) {
                        Placement p1 = assignment.getValue(c1);
                        if (p1 == null) continue;
                        for (Lecture c2 : lec2.getChildren(subpartId)) {
                            Placement p2 = assignment.getValue(c2);
                            if (p2 == null) continue;
                            if (!c1.getSchedulingSubpartId().equals(c2.getSchedulingSubpartId())) continue;
                            // parents not overlap, but children do
                            if (p1.getTimeLocation().hasIntersection(p2.getTimeLocation()))
                                return false;
                        }
                    }
                }
            }
        } else {
            // different subpart
        }
        return true;
    }

    public boolean isSatisfiedPair(Assignment<Lecture, Placement> assignment, Placement plc1, Placement plc2) {
        if (iIsRequired || (!iIsProhibited && iPreference <= 0))
            return getType().isSatisfied(assignment, this, plc1, plc2);
        else if (iIsProhibited || (!iIsRequired && iPreference > 0))
            return getType().isViolated(assignment, this, plc1, plc2);
        return true;
    }
    
    public boolean canShareRoom() {
        return getType().is(Flag.CAN_SHARE_ROOM);
    }
    
    protected int nrSlotsADay(Assignment<Lecture, Placement> assignment, int dayCode, BitSet week, HashMap<Lecture, Placement> assignments, Set<Placement> conflicts) {
        Set<Integer> slots = new HashSet<Integer>();
        for (Lecture lecture: variables()) {
            Placement placement = null;
            if (assignments != null && assignments.containsKey(lecture))
                placement = assignments.get(lecture);
            else if (assignment != null)
                placement = assignment.getValue(lecture);
            if (placement == null || placement.getTimeLocation() == null) continue;
            if (conflicts != null && conflicts.contains(placement)) continue;
            TimeLocation t = placement.getTimeLocation();
            if (t == null || (t.getDayCode() & dayCode) == 0 || (week != null && !t.shareWeeks(week))) continue;
            for (int i = 0; i < t.getLength(); i++)
                slots.add(i + t.getStartSlot());
        }
        return slots.size();
    }
    
    protected int nrSlotsADay(Assignment<Lecture, Placement> assignment, int date, HashMap<Lecture, Placement> assignments, Set<Placement> conflicts) {
        Set<Integer> slots = new HashSet<Integer>();
        for (Lecture lecture: variables()) {
            Placement placement = null;
            if (assignments != null && assignments.containsKey(lecture))
                placement = assignments.get(lecture);
            else if (assignment != null)
                placement = assignment.getValue(lecture);
            if (placement == null || placement.getTimeLocation() == null) continue;
            if (conflicts != null && conflicts.contains(placement)) continue;
            TimeLocation t = placement.getTimeLocation();
            if (t == null || !t.hasDate(date, iDayOfWeekOffset)) continue;
            for (int i = 0; i < t.getLength(); i++)
                slots.add(i + t.getStartSlot());
        }
        return slots.size();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof GroupConstraint)) return false;
        return getGeneratedId() == ((GroupConstraint) o).getGeneratedId();
    }
    
    @Override
    public GroupConstraintContext createAssignmentContext(Assignment<Lecture, Placement> assignment) {
        return new GroupConstraintContext(assignment);
    }

    public class GroupConstraintContext implements AssignmentConstraintContext<Lecture, Placement> {
        protected int iLastPreference = 0;
        
        public GroupConstraintContext(Assignment<Lecture, Placement> assignment) {
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
        
        protected void updateCriterion(Assignment<Lecture, Placement> assignment) {
            if (!isHard()) {
                getModel().getCriterion(DistributionPreferences.class).inc(assignment, -iLastPreference);
                iLastPreference = getCurrentPreference(assignment) + Math.abs(iPreference);
                getModel().getCriterion(DistributionPreferences.class).inc(assignment, iLastPreference);
            }
        }
        
        public int getPreference() { return iLastPreference; }
    }
    
    private boolean isBackToBackWeeks(TimeLocation t1, TimeLocation t2) {
        if (t1.shareWeeks(t2)) return false;
        int f1 = t1.getWeekCode().nextSetBit(0);
        int e1 = t1.getWeekCode().previousSetBit(t1.getWeekCode().size());
        int f2 = t2.getWeekCode().nextSetBit(0);
        int e2 = t2.getWeekCode().previousSetBit(t2.getWeekCode().size());
        if (e1 < f2) {
            return (f2 - e1) < 7;
        } else if (e2 < f1) {
            return (f1 - e2) < 7;
        }
        return false;
    }
    
    private boolean isMaxWeekSpan(TimeLocation t1, TimeLocation t2, int nrWeeks) {
        if (t1.shareWeeks(t2)) return false;
        if (isBackToBackWeeks(t1, t2)) return true;
        
        int f1 = t1.getWeekCode().nextSetBit(0);
        int e1 = t1.getWeekCode().previousSetBit(t1.getWeekCode().size());
        int f2 = t2.getWeekCode().nextSetBit(0);
        int e2 = t2.getWeekCode().previousSetBit(t2.getWeekCode().size());
        if (e1 < f2) {
            return (3 + e2 - f1) / 7 <= nrWeeks;
        } else if (e2 < f1) {
            return (3 + e1 - f2) / 7 <= nrWeeks;
        }
        return false;
    }
    
    private boolean isNotBackToBackWeeks(TimeLocation t1, TimeLocation t2) {
        if (t1.shareWeeks(t2)) return false;
        int f1 = t1.getWeekCode().nextSetBit(0);
        int e1 = t1.getWeekCode().previousSetBit(t1.getWeekCode().size());
        int f2 = t2.getWeekCode().nextSetBit(0);
        int e2 = t2.getWeekCode().previousSetBit(t2.getWeekCode().size());
        if (e1 < f2) {
            return (f2 - e1) >= 7;
        } else if (e2 < f1) {
            return (f1 - e2) >= 7;
        }
        return false;
    }
    
    private boolean isFollowingWeeksBTB(Placement p1, Placement p2, boolean btb) {
        int ord1 = variables().indexOf(p1.variable());
        int ord2 = variables().indexOf(p2.variable());
        TimeLocation t1, t2;
        boolean following = false;
        if (ord1 < ord2) {
            t1 = p1.getTimeLocation();
            t2 = p2.getTimeLocation();
            if (ord1 + 1 == ord2) following = true;
        } else {
            t2 = p1.getTimeLocation();
            t1 = p2.getTimeLocation();
            if (ord2 + 1 == ord1) following = true;
        }
        if (t1.shareWeeks(t2)) return false;
        int e1 = t1.getWeekCode().previousSetBit(t1.getWeekCode().size());
        int s2 = t2.getWeekCode().nextSetBit(0);
        if (e1 >= s2) return false;
        if (!btb) // not back-to-back: any two classes must be at least a week apart
            return (s2 - e1) >= 7;
        else if (following) // back-to-back and following classes: must be within a week
            return (s2 - e1) < 7;
        else // back-to-back and not following: just the order
            return true;
    }
    
    private boolean isDifferentDates(TimeLocation t1, TimeLocation t2) {
        if (!t1.shareDays(t2) || !t1.shareWeeks(t2)) return true;
        for (Enumeration<Integer> e = t1.getDates(iDayOfWeekOffset); e.hasMoreElements(); ) {
            Integer date = e.nextElement();
            if (t2.hasDate(date, iDayOfWeekOffset)) return false;
        }
        return true;
    }
    
    private boolean isSameDates(TimeLocation t1, TimeLocation t2) {
        if (!t1.shareDays(t2) || !t1.shareWeeks(t2)) return false;
        // t1 is meets less often
        if (t1.countDates(iDayOfWeekOffset) > t2.countDates(iDayOfWeekOffset)) {
            TimeLocation t = t1; t1 = t2; t2 = t;
        }
        for (Enumeration<Integer> e = t1.getDates(iDayOfWeekOffset); e.hasMoreElements(); ) {
            Integer date = e.nextElement();
            if (!t2.hasDate(date, iDayOfWeekOffset)) return false;
        }
        return true;
    }
    
    private int nextDate(BitSet weekCode, int dayCode, int nextDate) {
        while (true) {
            nextDate = weekCode.nextSetBit(1 + nextDate);
            if (nextDate < 0) return -1;
            int dow = (nextDate + iDayOfWeekOffset) % 7;
            if ((dayCode & Constants.DAY_CODES[dow]) != 0) return nextDate;
        }
    }
    
    private boolean isFollowingDates(Placement p1, Placement p2, boolean btb) {
        int ord1 = variables().indexOf(p1.variable());
        int ord2 = variables().indexOf(p2.variable());
        TimeLocation t1, t2;
        boolean following = false;
        if (ord1 < ord2) {
            t1 = p1.getTimeLocation();
            t2 = p2.getTimeLocation();
            if (ord1 + 1 == ord2) following = true;
        } else {
            t2 = p1.getTimeLocation();
            t1 = p2.getTimeLocation();
            if (ord2 + 1 == ord1) following = true;
        }
        int d1 = nextDate(t1.getWeekCode(), t1.getDayCode(), -1);
        int d2 = nextDate(t2.getWeekCode(), t2.getDayCode(), -1);
        while (d1 >= 0) {
            int d1next = nextDate(t1.getWeekCode(), t1.getDayCode(), d1);
            int d2next = nextDate(t2.getWeekCode(), t2.getDayCode(), d2);
            if (d1 >= d2) return false; // next date of p2 is before or on d1 (or there is no next date)
            // d1 < d2 (d1 before d2)
            if (d1next >= 0 && d2 >= d1next) return false; // p1 has a next date, but p2 date is not before that date
            // d1next < 0 || d2 < d1next (no next d1, or d2 before next d2
            if (!btb && d1 + 1 == d2) return false; // p2 is on a following day of p1
            // btb || d1 + 1 < d2 (back-to-back or at least one day gap in between)
            if (btb && following && d1 + 1 != d2) return false; // d2 is not on the next date from d1
            // !following || d1 + 1 == d2 (not following or on the next day)
            d1 = d1next; d2 = d2next;
        }
        // d1 < 0 (no next d1), check that there is also no d2
        return (d2 < 0); // both patterns have ended
    }
    
    protected boolean isOnline(Placement p) {
        // no room -- StudentConflict.OnlineRoom must allow for a blank string
        if (p.getNrRooms() == 0)
            return "".matches(iOnlineRoom);
        // one room -- room name must match StudentConflict.OnlineRoom
        if (p.getNrRooms() == 1)
            return (p.getRoomLocation().getName() != null && p.getRoomLocation().getName().matches(iOnlineRoom));
        // multiple rooms -- all rooms must match StudentConflict.OnlineRoom
        for (RoomLocation r: p.getRoomLocations())
            if (r.getName() == null || !r.getName().matches(iOnlineRoom)) return false;
        return true;
    }
}
