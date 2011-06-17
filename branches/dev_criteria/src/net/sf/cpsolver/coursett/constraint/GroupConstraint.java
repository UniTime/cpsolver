package net.sf.cpsolver.coursett.constraint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.coursett.Constants;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.GlobalConstraint;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Group constraint. <br>
 * This constraint expresses relations between several classes, e.g., that two
 * sections of the same lecture can not be taught at the same time, or that some
 * classes have to be taught one immediately after another. It can be either
 * hard or soft. <br>
 * <br>
 * Following constraints are now supported:
 * <table border='1'>
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
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
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

public class GroupConstraint extends Constraint<Lecture, Placement> {
    private Long iId;
    private int iPreference;
    private ConstraintType iType;
    private boolean iIsRequired;
    private boolean iIsProhibited;
    private int iLastPreference = 0;
    
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
        CH_NOTOVERLAP;
        /** Bit number (to combine flags) */
        int flag() { return 1 << ordinal(); }
    }
    
    /**
     * Group constraint type.
     */
    public static enum ConstraintType {
        /**
         * Same Rime: Given classes must be taught at the same time of day (independent of the actual day the classes meet).
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
         * Back-To-Back & Same Room: Classes must be offered in adjacent time segments and must be placed in the same room.
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
        NHB_1("NHB(1)", "1 Hour Between", 12, BTB_TIME.check(), Flag.BACK_TO_BACK),
        /**
         * 2 Hours Between: Given classes must have exactly 2 hours in between the end of one and the beginning of another.
         * As with the <i>back-to-back time</i> constraint, given classes must be taught on the same days.<BR>
         * When prohibited or (strongly) discouraged: classes can not have 2 hours in between. They may not overlap in time
         * but must be taught on the same days.
         */
        NHB_2("NHB(2)", "2 Hours Between", 24, BTB_TIME.check(), Flag.BACK_TO_BACK),
        /**
         * 3 Hours Between: Given classes must have exactly 3 hours in between the end of one and the beginning of another.
         * As with the <i>back-to-back time</i> constraint, given classes must be taught on the same days.<BR>
         * When prohibited or (strongly) discouraged: classes can not have 3 hours in between. They may not overlap in time
         * but must be taught on the same days.
         */
        NHB_3("NHB(3)", "3 Hours Between", 36, BTB_TIME.check(), Flag.BACK_TO_BACK),
        /**
         * 4 Hours Between: Given classes must have exactly 4 hours in between the end of one and the beginning of another.
         * As with the <i>back-to-back time</i> constraint, given classes must be taught on the same days.<BR>
         * When prohibited or (strongly) discouraged: classes can not have 4 hours in between. They may not overlap in time
         * but must be taught on the same days.
         */
        NHB_4("NHB(4)", "4 Hours Between", 48, BTB_TIME.check(), Flag.BACK_TO_BACK),
        /**
         * 5 Hours Between: Given classes must have exactly 5 hours in between the end of one and the beginning of another.
         * As with the <i>back-to-back time</i> constraint, given classes must be taught on the same days.<BR>
         * When prohibited or (strongly) discouraged: classes can not have 5 hours in between. They may not overlap in time
         * but must be taught on the same days.
         */
        NHB_5("NHB(5)", "5 Hours Between", 60, BTB_TIME.check(), Flag.BACK_TO_BACK),
        /**
         * 6 Hours Between: Given classes must have exactly 6 hours in between the end of one and the beginning of another.
         * As with the <i>back-to-back time</i> constraint, given classes must be taught on the same days.<BR>
         * When prohibited or (strongly) discouraged: classes can not have 6 hours in between. They may not overlap in time
         * but must be taught on the same days.
         */
        NHB_6("NHB(6)", "6 Hours Between", 72, BTB_TIME.check(), Flag.BACK_TO_BACK),
        /**
         * 7 Hours Between: Given classes must have exactly 7 hours in between the end of one and the beginning of another.
         * As with the <i>back-to-back time</i> constraint, given classes must be taught on the same days.<BR>
         * When prohibited or (strongly) discouraged: classes can not have 7 hours in between. They may not overlap in time
         * but must be taught on the same days.
         */
        NHB_7("NHB(7)", "7 Hours Between", 84, BTB_TIME.check(), Flag.BACK_TO_BACK),
        /**
         * 8 Hours Between: Given classes must have exactly 8 hours in between the end of one and the beginning of another.
         * As with the <i>back-to-back time</i> constraint, given classes must be taught on the same days.<BR>
         * When prohibited or (strongly) discouraged: classes can not have 8 hours in between. They may not overlap in time
         * but must be taught on the same days.
         */
        NHB_8("NHB(8)", "8 Hours Between", 96, BTB_TIME.check(), Flag.BACK_TO_BACK),
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
        NHB_LT_6("NHB_LT(6)", "Less Than 6 Hours Between", 0, 66, BTB_TIME.check(), Flag.BACK_TO_BACK),
        /**
         * 1.5 Hour Between: Given classes must have exactly 90 minutes in between the end of one and the beginning of another.
         * As with the <i>back-to-back time</i> constraint, given classes must be taught on the same days.<BR>
         * When prohibited or (strongly) discouraged: classes can not have 90 minutes in between. They may not overlap in time
         * but must be taught on the same days.
         */
        NHB_1_5("NHB(1.5)", "1.5 Hour Between", 18, BTB_TIME.check(), Flag.BACK_TO_BACK),
        /**
         * 4.5 Hours Between: Given classes must have exactly 4.5 hours in between the end of one and the beginning of another.
         * As with the <i>back-to-back time</i> constraint, given classes must be taught on the same days.<BR>
         * When prohibited or (strongly) discouraged: classes can not have 4.5 hours in between. They may not overlap in time
         * but must be taught on the same days.
         */
        NHB_4_5("NHB(4.5)", "4.5 Hours Between", 54, BTB_TIME.check(), Flag.BACK_TO_BACK),
        /**
         * Same Students: Given classes are treated as they are attended by the same students, i.e., they cannot overlap in time
         * and if they are back-to-back the assigned rooms cannot be too far (student limit is used).
         */
        SAME_STUDENTS("SAME_STUDENTS", "Same Students", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return !JenrlConstraint.isInConflict(plc1, plc2, ((TimetableModel)gc.getModel()).getDistanceMetric());
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
                    if (t1.shareHours(t2))
                        return false; // overlap
                    int s1 = t1.getStartSlot() % Constants.SLOTS_PER_DAY;
                    int s2 = t2.getStartSlot() % Constants.SLOTS_PER_DAY;
                    if (s1 + t1.getLength() == s2 || s2 + t2.getLength() == s1) { // back-to-back
                        TimetableModel m = (TimetableModel)gc.getModel();
                        double distance = Placement.getDistanceInMeters(m.getDistanceMetric(), plc1, plc2);
                        if (distance > m.getDistanceMetric().getInstructorProhibitedLimit())
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
        CAN_SHARE_ROOM("CAN_SHARE_ROOM", "Can Share Room", null, Flag.CAN_SHARE_ROOM),
        /**
         * Precedence: Given classes have to be taught in the given order (the first meeting of the first class has to end before
         * the first meeting of the second class etc.)<BR>
         * When prohibited or (strongly) discouraged: classes have to be taught in the order reverse to the given one.
         */
        PRECEDENCE("PRECEDENCE", "Precedence", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return gc.isPrecedence(plc1, plc2, true);
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return gc.isPrecedence(plc1, plc2, false);
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
                    isBackToBackDays(plc1.getTimeLocation(), plc2.getTimeLocation());
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return
                    !sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray()) &&
                    !isBackToBackDays(plc1.getTimeLocation(), plc2.getTimeLocation());
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
                    isNrDaysBetweenGreaterThanOne(plc1.getTimeLocation(), plc2.getTimeLocation());
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
                return
                    !sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray()) &&
                    !isNrDaysBetweenGreaterThanOne(plc1.getTimeLocation(), plc2.getTimeLocation());
            }}),
        /**
         * Children Cannot Overlap: If parent classes do not overlap in time, children classes can not overlap in time as well.<BR>
         * Note: This constraint only needs to be put on the parent classes. Preferred configurations are Required All Classes
         * or Pairwise (Strongly) Preferred.
         */
        CH_NOTOVERLAP("CH_NOTOVERLAP", "Children Cannot Overlap", new PairCheck() {
            @Override
            public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) {
                return gc.isChildrenNotOverlap(plc1.variable(), plc1, plc2.variable(), plc2);
            }
            @Override
            public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) {
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
        ;
        
        String iReference, iName;
        int iFlag = 0;
        Flag[] iFlags = null;
        int iMin = 0, iMax = 0;
        PairCheck iCheck = null;
        ConstraintType(String reference, String name, PairCheck check, Flag... flags) {
            iReference = reference;
            iName = name;
            iCheck = check;
            iFlags = flags;
            for (Flag f: flags)
                iFlag |= f.flag();
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
        
        /** Constraint reference */
        public String reference() { return iReference; }
        /** Constraint name */
        public String getName() { return iName; }
        /** Minimum (gap) parameter */
        public int getMin() { return iMin; }
        /** Maximum (gap, hours a day) parameter */
        public int getMax() { return iMax; }
        
        /** Flag check (true if contains given flag) */
        public boolean is(Flag f) { return (iFlag & f.flag()) != 0; }

        /** Constraint type from reference */
        public static ConstraintType get(String reference) {
            for (ConstraintType t: ConstraintType.values())
                if (t.reference().equals(reference)) return t;
            return null;
        }
        
        /** True if a required or preferred constraint is satisfied between a pair of placements */ 
        public boolean isSatisfied(GroupConstraint gc, Placement plc1, Placement plc2) { return (iCheck == null ? true : iCheck.isSatisfied(gc, plc1, plc2)); }
        /** True if a prohibited or discouraged constraint is satisfied between a pair of placements */ 
        public boolean isViolated(GroupConstraint gc, Placement plc1, Placement plc2) { return (iCheck == null ? true : iCheck.isViolated(gc, plc1, plc2)); }
        /** Pair check */
        private PairCheck check() { return iCheck; }
    }

    public GroupConstraint() {
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
    public GroupConstraint(Long id, ConstraintType type, String preference) {
        iId = id;
        iType = type;
        iIsRequired = preference.equals(Constants.sPreferenceRequired);
        iIsProhibited = preference.equals(Constants.sPreferenceProhibited);
        iPreference = Constants.preference2preferenceLevel(preference);
    }

    /** Constraint id */
    public Long getConstraintId() {
        return iId;
    }

    @Override
    public long getId() {
        return (iId == null ? -1 : iId.longValue());
    }

    /** ConstraString type (e.g, {@link ConstraintType#SAME_TIME}) */
    public ConstraintType getType() {
        return iType;
    }

    public void setType(ConstraintType type) {
        iType = type;
    }

    /** Is constraint required */
    public boolean isRequired() {
        return iIsRequired;
    }

    /** Is constraint prohibited */
    public boolean isProhibited() {
        return iIsProhibited;
    }

    /**
     * Prolog reference: "R" for required, "P" for prohibited", "-2",.."2" for
     * preference
     */
    public String getPrologPreference() {
        return Constants.preferenceLevel2preference(iPreference);
    }

    @Override
    public boolean isConsistent(Placement value1, Placement value2) {
        if (!isHard())
            return true;
        if (!isSatisfiedPair(value1, value2))
            return false;
        if (getType().is(Flag.BACK_TO_BACK)) {
            HashMap<Lecture, Placement> assignments = new HashMap<Lecture, Placement>();
            assignments.put(value1.variable(), value1);
            assignments.put(value2.variable(), value2);
            if (!isSatisfiedSeq(assignments, false, null))
                return false;
        }
        if (getType().is(Flag.MAX_HRS_DAY)) {
            HashMap<Lecture, Placement> assignments = new HashMap<Lecture, Placement>();
            assignments.put(value1.variable(), value1);
            assignments.put(value2.variable(), value2);
            for (int dayCode: Constants.DAY_CODES) {
                if (nrSlotsADay(dayCode, assignments, null) > getType().getMax())
                    return false;
            }
        }
        return true;
    }

    @Override
    public void computeConflicts(Placement value, Set<Placement> conflicts) {
        if (!isHard())
            return;
        for (Lecture v : variables()) {
            if (v.equals(value.variable()))
                continue; // ignore this variable
            if (v.getAssignment() == null)
                continue; // there is an unassigned variable -- great, still a
                          // chance to get violated
            if (!isSatisfiedPair(v.getAssignment(), value))
                conflicts.add(v.getAssignment());
        }
        if (getType().is(Flag.BACK_TO_BACK)) {
            HashMap<Lecture, Placement> assignments = new HashMap<Lecture, Placement>();
            assignments.put(value.variable(), value);
            if (!isSatisfiedSeq(assignments, true, conflicts))
                conflicts.add(value);
        }
        if (getType().is(Flag.MAX_HRS_DAY)) {
            HashMap<Lecture, Placement> assignments = new HashMap<Lecture, Placement>();
            assignments.put(value.variable(), value);
            for (int dayCode: Constants.DAY_CODES) {
                if (nrSlotsADay(dayCode, assignments, conflicts) > getType().getMax()) {
                    List<Placement> adepts = new ArrayList<Placement>();
                    for (Lecture lecture: assignedVariables()) {
                        if (conflicts.contains(lecture.getAssignment()) || lecture.equals(value.variable())) continue;
                        if (lecture.getAssignment().getTimeLocation() == null) continue;
                        if ((lecture.getAssignment().getTimeLocation().getDayCode() & dayCode) == 0) continue;
                        adepts.add(lecture.getAssignment());
                    }
                    do {
                        Placement conflict = ToolBox.random(adepts);
                        adepts.remove(conflict);
                        conflicts.add(conflict);
                    } while (!adepts.isEmpty() && nrSlotsADay(dayCode, assignments, conflicts) > getType().getMax());
                }
            }
        }
        
        if (iType == ConstraintType.MEET_WITH && !conflicts.contains(value)) {
            // Check the room size
            int neededSize = 0;
            for (Lecture lecture: variables())
                neededSize += lecture.maxRoomUse();
            if (neededSize > value.getRoomSize()) {
                conflicts.add(value); // room is too small to fit all meet with classes
                return;
            }
            for (Lecture lecture: variables()) {
                if (lecture.equals(value.variable())) continue; // Skip this lecture
                if (lecture.getAssignment() != null) { // Has assignment, check whether it is conflicting
                    Placement other = lecture.getAssignment();
                    if (other.sameRooms(value) && sameHours(value.getTimeLocation(), other.getTimeLocation()) &&
                        sameDays(value.getTimeLocation(), other.getTimeLocation()))
                        continue;
                    conflicts.add(lecture.getAssignment());
                }
                // Look for a matching assignment
                List<Placement> sameAssignments = new ArrayList<Placement>();
                for (Placement other: lecture.values()) {
                    if (other.sameRooms(value) && sameHours(value.getTimeLocation(), other.getTimeLocation()) &&
                            sameDays(value.getTimeLocation(), other.getTimeLocation())) {
                        sameAssignments.add(other);
                    }
                }
                // No matching assignment -> fail
                if (sameAssignments.isEmpty()) {
                    conflicts.add(value); // other meet with class cannot be assigned with this value
                    return;
                }
                // Propagate the new assignment over other hard constraints of the lecture
                if (sameAssignments.size() == 1) {
                    Placement sameAssignment = sameAssignments.get(0);
                    for (Constraint<Lecture, Placement> other: lecture.hardConstraints()) {
                        if (other.equals(this)) continue;
                        other.computeConflicts(sameAssignment, conflicts);
                        if (conflicts.contains(value)) return;
                        if (conflicts.contains(sameAssignment)) {
                            conflicts.add(value); return;
                        }
                    }
                    for (GlobalConstraint<Lecture, Placement> other: getModel().globalConstraints()) {
                        other.computeConflicts(sameAssignment, conflicts);
                        if (conflicts.contains(value)) return;
                        if (conflicts.contains(sameAssignment)) {
                            conflicts.add(value); return;
                        }
                    }   
                }
            }
        }
    }

    @Override
    public boolean inConflict(Placement value) {
        if (!isHard())
            return false;
        for (Lecture v : variables()) {
            if (v.equals(value.variable()))
                continue; // ignore this variable
            if (v.getAssignment() == null)
                continue; // there is an unassigned variable -- great, still a
                          // chance to get violated
            if (!isSatisfiedPair(v.getAssignment(), value))
                return true;
        }
        if (getType().is(Flag.BACK_TO_BACK)) {
            HashMap<Lecture, Placement> assignments = new HashMap<Lecture, Placement>();
            assignments.put(value.variable(), value);
            if (!isSatisfiedSeq(assignments, true, null))
                return true;
        }
        if (getType().is(Flag.MAX_HRS_DAY)) {
            HashMap<Lecture, Placement> assignments = new HashMap<Lecture, Placement>();
            assignments.put(value.variable(), value);
            for (int dayCode: Constants.DAY_CODES) {
                if (nrSlotsADay(dayCode, assignments, null) > getType().getMax())
                    return true;
            }
        }
        return false;
    }

    /** Constraint preference (0 if prohibited or reqired) */
    public int getPreference() {
        return iPreference;
    }

    /**
     * Current constraint preference (0 if prohibited or reqired, depends on
     * current satisfaction of the constraint)
     */
    public int getCurrentPreference() {
        if (isHard()) return 0; // no preference
        if (countAssignedVariables() < 2) return 0; // not enough variable
        if (getType().is(Flag.MAX_HRS_DAY)) { // max hours a day
            int over = 0;
            for (int dayCode: Constants.DAY_CODES)
                over += Math.max(0, nrSlotsADay(dayCode, null, null) - getType().getMax());
            return (over > 0 ? Math.abs(iPreference) * over / 12 : - Math.abs(iPreference));
        }
        int nrViolatedPairs = 0;
        for (Lecture v1 : variables()) {
            if (v1.getAssignment() == null) continue;
            for (Lecture v2 : variables()) {
                if (v2.getAssignment() == null || v1.getId() >= v2.getId()) continue;
                if (!isSatisfiedPair(v1.getAssignment(), v2.getAssignment())) nrViolatedPairs++;
            }
        }
        if (getType().is(Flag.BACK_TO_BACK)) {
            Set<Placement> conflicts = new HashSet<Placement>();
            if (!isSatisfiedSeq(new HashMap<Lecture, Placement>(), true, conflicts))
                nrViolatedPairs += conflicts.size();
        }
        return (nrViolatedPairs > 0 ? Math.abs(iPreference) * nrViolatedPairs : - Math.abs(iPreference));
    }

    /** Current constraint preference (if given placement is assigned) */
    public int getCurrentPreference(Placement placement) {
        if (getType().is(Flag.MAX_HRS_DAY)) {
            if (placement.getTimeLocation() == null) return 0;
            HashMap<Lecture, Placement> assignments = new HashMap<Lecture, Placement>();
            assignments.put(placement.variable(), placement);
            int over = 0;
            for (int dayCode: Constants.DAY_CODES)
                if ((placement.getTimeLocation().getDayCode() & dayCode) != 0)
                    over += Math.min(Math.max(0, nrSlotsADay(dayCode, assignments, null) - getType().getMax()), placement.getTimeLocation().getLength());
            return (over > 0 ? Math.abs(iPreference) * over / 12 : - Math.abs(iPreference));
        }
        int nrViolatedPairs = 0;
        for (Lecture v1 : variables()) {
            if (v1.getAssignment() == null || v1.equals(placement.variable())) continue;
            if (!isSatisfiedPair(v1.getAssignment(), placement)) nrViolatedPairs++;
        }
        if (getType().is(Flag.BACK_TO_BACK)) {
            HashMap<Lecture, Placement> assignment = new HashMap<Lecture, Placement>();
            assignment.put(placement.variable(), placement);
            Set<Placement> conflicts = new HashSet<Placement>();
            if (!isSatisfiedSeq(assignment, true, conflicts))
                nrViolatedPairs += conflicts.size();
        }
        return (nrViolatedPairs > 0 ? Math.abs(iPreference) * nrViolatedPairs : - Math.abs(iPreference));
    }

    @Override
    public void unassigned(long iteration, Placement value) {
        super.unassigned(iteration, value);
        if (iIsRequired || iIsProhibited)
            return;
        ((TimetableModel) getModel()).getGlobalGroupConstraintPreferenceCounter().dec(iLastPreference);
        iLastPreference = Math.min(0, getCurrentPreference());
        ((TimetableModel) getModel()).getGlobalGroupConstraintPreferenceCounter().inc(iLastPreference);
    }

    @Override
    public void assigned(long iteration, Placement value) {
        super.assigned(iteration, value);
        if (iIsRequired || iIsProhibited)
            return;
        ((TimetableModel) getModel()).getGlobalGroupConstraintPreferenceCounter().dec(iLastPreference);
        iLastPreference = Math.min(0, getCurrentPreference());
        ((TimetableModel) getModel()).getGlobalGroupConstraintPreferenceCounter().inc(iLastPreference);
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


    private boolean isPrecedence(Placement p1, Placement p2, boolean firstGoesFirst) {
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
        return t1.getStartSlots().nextElement() + t1.getLength() <= t2.getStartSlots().nextElement();
    }

    private static boolean isBackToBackDays(TimeLocation t1, TimeLocation t2) {
        int f1 = -1, f2 = -1, e1 = -1, e2 = -1;
        for (int i = 0; i < Constants.DAY_CODES.length; i++) {
            if ((t1.getDayCode() & Constants.DAY_CODES[i]) != 0) {
                if (f1 < 0)
                    f1 = i;
                e1 = i;
            }
            if ((t2.getDayCode() & Constants.DAY_CODES[i]) != 0) {
                if (f2 < 0)
                    f2 = i;
                e2 = i;
            }
        }
        return (e1 + 1 == f2) || (e2 + 1 == f1);
    }
    
    private static boolean isNrDaysBetweenGreaterThanOne(TimeLocation t1, TimeLocation t2) {
        int f1 = -1, f2 = -1, e1 = -1, e2 = -1;
        for (int i = 0; i < Constants.DAY_CODES.length; i++) {
            if ((t1.getDayCode() & Constants.DAY_CODES[i]) != 0) {
                if (f1 < 0)
                    f1 = i;
                e1 = i;
            }
            if ((t2.getDayCode() & Constants.DAY_CODES[i]) != 0) {
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
            if ((t1.getDayCode() & Constants.DAY_CODES[i]) != 0) {
                if (f1 < 0)
                    f1 = i;
                e1 = i;
            }
            if ((t2.getDayCode() & Constants.DAY_CODES[i]) != 0) {
                if (f2 < 0)
                    f2 = i;
            }
        }
        return ((e1 + 1) % Constants.NR_DAYS_WEEK == f2);
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
            if ((t1.getDayCode() & Constants.DAY_CODES[i]) != 0) {
                if (f1 < 0)
                    f1 = i;
                e1 = i;
            }
            if ((t2.getDayCode() & Constants.DAY_CODES[i]) != 0) {
                if (f2 < 0)
                    f2 = i;
            }
        }
        return ((e1 + 2) % Constants.NR_DAYS_WEEK == f2);
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
    
    private static boolean sameDays(TimeLocation t1, TimeLocation t2) {
        if (t1 == null || t2 == null) return false;
        return sameDays(t1.getDaysArray(), t2.getDaysArray());
    }

    private static boolean sameHours(int start1, int len1, int start2, int len2) {
        if (len1 > len2)
            return sameHours(start2, len2, start1, len1);
        start1 %= Constants.SLOTS_PER_DAY;
        start2 %= Constants.SLOTS_PER_DAY;
        return (start1 >= start2 && start1 + len1 <= start2 + len2);
    }
    
    private static boolean sameHours(TimeLocation t1, TimeLocation t2) {
        if (t1 == null || t2 == null) return false;
        return sameHours(t1.getStartSlot(), t1.getLength(), t2.getStartSlot(), t2.getLength());
    }

    private static boolean canFill(int totalGap, int gapMin, int gapMax, List<Integer> lengths) {
        if (gapMin <= totalGap && totalGap <= gapMax)
            return true;
        if (totalGap < 2 * gapMin)
            return false;
        for (int i = 0; i < lengths.size(); i++) {
            int length = lengths.get(i);
            lengths.remove(i);
            for (int gap = gapMin; gap <= gapMax; gap++)
                if (canFill(totalGap - gap - length, gapMin, gapMax, lengths))
                    return true;
            lengths.add(i, length);
        }
        return false;
    }

    private boolean isSatisfiedSeq(HashMap<Lecture, Placement> assignments, boolean considerCurrentAssignments,
            Set<Placement> conflicts) {
        if (conflicts == null)
            return isSatisfiedSeqCheck(assignments, considerCurrentAssignments, conflicts);
        else {
            Set<Placement> bestConflicts = isSatisfiedRecursive(0, assignments, considerCurrentAssignments, conflicts,
                    new HashSet<Placement>(), null);
            if (bestConflicts == null)
                return false;
            conflicts.addAll(bestConflicts);
            return true;
        }
    }

    private Set<Placement> isSatisfiedRecursive(int idx, HashMap<Lecture, Placement> assignments,
            boolean considerCurrentAssignments, Set<Placement> conflicts, Set<Placement> newConflicts,
            Set<Placement> bestConflicts) {
        if (idx == variables().size() && newConflicts.isEmpty())
            return bestConflicts;
        if (isSatisfiedSeqCheck(assignments, considerCurrentAssignments, conflicts)) {
            if (bestConflicts == null || bestConflicts.size() > newConflicts.size())
                return new HashSet<Placement>(newConflicts);
            return bestConflicts;
        }
        if (idx == variables().size())
            return bestConflicts;
        bestConflicts = isSatisfiedRecursive(idx + 1, assignments, considerCurrentAssignments, conflicts, newConflicts,
                bestConflicts);
        Lecture lecture = variables().get(idx);
        if (assignments != null && assignments.containsKey(lecture))
            return bestConflicts;
        Placement placement = (assignments == null ? null : assignments.get(lecture));
        if (placement == null && considerCurrentAssignments)
            placement = lecture.getAssignment();
        if (placement == null)
            return bestConflicts;
        if (conflicts != null && conflicts.contains(placement))
            return bestConflicts;
        conflicts.add(placement);
        newConflicts.add(placement);
        bestConflicts = isSatisfiedRecursive(idx + 1, assignments, considerCurrentAssignments, conflicts, newConflicts,
                bestConflicts);
        newConflicts.remove(placement);
        conflicts.remove(placement);
        return bestConflicts;
    }

    private boolean isSatisfiedSeqCheck(HashMap<Lecture, Placement> assignments, boolean considerCurrentAssignments,
            Set<Placement> conflicts) {
        if (!getType().is(Flag.BACK_TO_BACK)) return true;
        int gapMin = getType().getMin();
        int gapMax = getType().getMax();

        List<Integer> lengths = new ArrayList<Integer>();

        Placement[] res = new Placement[Constants.SLOTS_PER_DAY];
        for (int i = 0; i < Constants.SLOTS_PER_DAY; i++)
            res[i] = null;

        int nrLectures = 0;

        for (Lecture lecture : variables()) {
            Placement placement = (assignments == null ? null : assignments.get(lecture));
            if (placement == null && considerCurrentAssignments)
                placement = lecture.getAssignment();
            if (placement == null) {
                lengths.add(lecture.timeLocations().get(0).getLength());
            } else if (conflicts != null && conflicts.contains(placement)) {
                lengths.add(lecture.timeLocations().get(0).getLength());
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
            i += res[i].getTimeLocation().getLength();
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
                i += res[i].getTimeLocation().getLength();
                nrLectures--;
            }
        } else if (iIsProhibited || (!iIsRequired && iPreference > 0)) {
            int i = 0;
            Placement p = res[i];
            while (p == null)
                p = res[++i];
            i += res[i].getTimeLocation().getLength();
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
                i += res[i].getTimeLocation().getLength();
                nrLectures--;
            }
        }
        return true;
    }

    public boolean isSatisfied() {
        if (isHard()) return true;
        if (countAssignedVariables() < 2) return true;
        if (getPreference() == 0) return true;
        return isHard() || countAssignedVariables() < 2 || getPreference() == 0 || getCurrentPreference() < 0;
    }

    public boolean isChildrenNotOverlap(Lecture lec1, Placement plc1, Lecture lec2, Placement plc2) {
        if (lec1.getSchedulingSubpartId().equals(lec2.getSchedulingSubpartId())) {
            // same subpart
            boolean overlap = plc1.getTimeLocation().hasIntersection(plc2.getTimeLocation());

            if (overlap && lec1.getParent() != null && variables().contains(lec1.getParent())
                    && lec2.getParent() != null && variables().contains(lec2.getParent())) {
                // children overlaps
                Placement p1 = lec1.getParent().getAssignment();
                Placement p2 = lec2.getParent().getAssignment();
                // parents not overlap, but children do
                if (p1 != null && p2 != null && !p1.getTimeLocation().hasIntersection(p2.getTimeLocation()))
                    return false;
            }

            if (!overlap && lec1.getChildrenSubpartIds() != null && lec2.getChildrenSubpartIds() != null) {
                // parents not overlap
                for (Long subpartId: lec1.getChildrenSubpartIds()) {
                    for (Lecture c1 : lec1.getChildren(subpartId)) {
                        if (c1.getAssignment() == null)
                            continue;
                        for (Lecture c2 : lec2.getChildren(subpartId)) {
                            if (c2.getAssignment() == null)
                                continue;
                            if (!c1.getSchedulingSubpartId().equals(c2.getSchedulingSubpartId()))
                                continue;
                            Placement p1 = c1.getAssignment();
                            Placement p2 = c2.getAssignment();
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

    public boolean isSatisfiedPair(Placement plc1, Placement plc2) {
        if (iIsRequired || (!iIsProhibited && iPreference <= 0))
            return getType().isSatisfied(this, plc1, plc2);
        else if (iIsProhibited || (!iIsRequired && iPreference > 0))
            return getType().isViolated(this, plc1, plc2);
        return true;
    }
    
    public boolean canShareRoom() {
        return getType().is(Flag.CAN_SHARE_ROOM);
    }
    
    private int nrSlotsADay(int dayCode, HashMap<Lecture, Placement> assignments, Set<Placement> conflicts) {
        Set<Integer> slots = new HashSet<Integer>();
        for (Lecture lecture: variables()) {
            Placement placement = (assignments == null ? null : assignments.get(lecture));
            if (placement == null) {
                placement = lecture.getAssignment();
                if (conflicts != null && conflicts.contains(placement)) continue;
            }
            if (placement == null || placement.getTimeLocation() == null) continue;
            TimeLocation t = placement.getTimeLocation();
            if (t == null || (t.getDayCode() & dayCode) == 0) continue;
            for (int i = 0; i < t.getLength(); i++)
                slots.add(i + t.getStartSlot());
        }
        return slots.size();
    }

    
}
