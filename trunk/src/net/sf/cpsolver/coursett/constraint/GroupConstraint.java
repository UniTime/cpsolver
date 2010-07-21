package net.sf.cpsolver.coursett.constraint;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.coursett.Constants;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.model.Constraint;

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
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 */

public class GroupConstraint extends Constraint<Lecture, Placement> {
    private Long iId;
    private int iPreference;
    private String iType;
    private boolean iIsRequired;
    private boolean iIsProhibited;
    private int iLastPreference = 0;

    /**
     * Same time: given classes have to be taught in the same hours. If the
     * classes are of different length, the smaller one cannot start before the
     * longer one and it cannot end after the longer one.
     */
    public static String TYPE_SAME_TIME = "SAME_TIME";
    /**
     * Same days: given classes have to be taught in the same day. If the
     * classes are of different time patterns, the days of one class have to
     * form a subset of the days of the other class.
     */
    public static String TYPE_SAME_DAYS = "SAME_DAYS";
    /**
     * Back-to-back constraint: given classes have to be taught in the same room
     * and they have to follow one strictly after another.
     */
    public static String TYPE_BTB = "BTB";
    /**
     * Back-to-back constraint: given classes have to follow one strictly after
     * another, but they can be taught in different rooms.
     */
    public static String TYPE_BTB_TIME = "BTB_TIME";
    /** Different time: given classes cannot overlap in time. */
    public static String TYPE_DIFF_TIME = "DIFF_TIME";
    /**
     * One hour between: between the given classes, the exact number of hours
     * have to be kept.
     */
    public static String TYPE_NHB_1 = "NHB(1)";
    /**
     * Two hours between: between the given classes, the exact number of hours
     * have to be kept.
     */
    public static String TYPE_NHB_2 = "NHB(2)";
    /**
     * Three hours between: between the given classes, the exact number of hours
     * have to be kept.
     */
    public static String TYPE_NHB_3 = "NHB(3)";
    /**
     * Four hours between: between the given classes, the exact number of hours
     * have to be kept.
     */
    public static String TYPE_NHB_4 = "NHB(4)";
    /**
     * Five hours between: between the given classes, the exact number of hours
     * have to be kept.
     */
    public static String TYPE_NHB_5 = "NHB(5)";
    /**
     * Six hours between: between the given classes, the exact number of hours
     * have to be kept.
     */
    public static String TYPE_NHB_6 = "NHB(6)";
    /**
     * Seven hours between: between the given classes, the exact number of hours
     * have to be kept.
     */
    public static String TYPE_NHB_7 = "NHB(7)";
    /**
     * Eight hours between: between the given classes, the exact number of hours
     * have to be kept.
     */
    public static String TYPE_NHB_8 = "NHB(8)";
    /** Same room: given classes have to placed in the same room. */
    public static String TYPE_SAME_START = "SAME_START";
    /** Same room: given classes have to placed in the same room. */
    public static String TYPE_SAME_ROOM = "SAME_ROOM";
    /**
     * Greater than or equal to 1 hour between: between the given classes, the
     * number of hours have to be one or more.
     */
    public static String TYPE_NHB_GTE_1 = "NHB_GTE(1)";
    /**
     * Less than 6 hours between: between the given classes, the number of hours
     * have to be less than six.
     */
    public static String TYPE_NHB_LT_6 = "NHB_LT(6)";
    /**
     * One and half hour between: between the given classes, the exact number of
     * hours have to be kept.
     */
    public static String TYPE_NHB_1_5 = "NHB(1.5)";
    /**
     * Four and half hours between: between the given classes, the exact number
     * of hours have to be kept.
     */
    public static String TYPE_NHB_4_5 = "NHB(4.5)";
    public static String TYPE_SAME_STUDENTS = "SAME_STUDENTS";
    public static String TYPE_SAME_INSTR = "SAME_INSTR";
    public static String TYPE_CAN_SHARE_ROOM = "CAN_SHARE_ROOM";
    public static String TYPE_PRECEDENCE = "PRECEDENCE";
    public static String TYPE_BTB_DAY = "BTB_DAY";
    public static String TYPE_MEET_WITH = "MEET_WITH";
    public static String TYPE_NDB_GT_1 = "NDB_GT_1";
    public static String TYPE_CH_NOTOVERLAP = "CH_NOTOVERLAP";
    public static String TYPE_FOLLOWING_DAY = "FOLLOWING_DAY";
    public static String TYPE_EVERY_OTHER_DAY = "EVERY_OTHER_DAY";

    public GroupConstraint() {
    }

    @Override
    public void addVariable(Lecture lecture) {
        if (!variables().contains(lecture))
            super.addVariable(lecture);
        if (isChildrenNotOverlap(getType())) {
            if (lecture.getChildrenSubpartIds() != null) {
                for (Enumeration<Long> e1 = lecture.getChildrenSubpartIds(); e1.hasMoreElements();) {
                    Long subpartId = e1.nextElement();
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
        if (isChildrenNotOverlap(getType())) {
            if (lecture.getChildrenSubpartIds() != null) {
                for (Enumeration<Long> e1 = lecture.getChildrenSubpartIds(); e1.hasMoreElements();) {
                    Long subpartId = e1.nextElement();
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
     *            constraString type (e.g, "SAME_TIME")
     * @param preference
     *            time preferent ("R" for required, "P" for prohibited, "-2",
     *            "-1", "1", "2" for soft preference)
     */
    public GroupConstraint(Long id, String type, String preference) {
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

    /** ConstraString type (e.g, {@link GroupConstraint#TYPE_SAME_TIME} */
    public String getType() {
        return iType;
    }

    public void setType(String type) {
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
        if (!isSatisfiedPair(value1.variable(), value1, value2.variable(), value2))
            return false;
        if (isBackToBack(getType())) {
            Hashtable<Lecture, Placement> assignments = new Hashtable<Lecture, Placement>();
            assignments.put(value1.variable(), value1);
            assignments.put(value2.variable(), value2);
            if (!isSatisfiedSeq(assignments, false, null))
                return false;
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
            if (!isSatisfiedPair(v, v.getAssignment(), value.variable(), value))
                conflicts.add(v.getAssignment());
        }
        Hashtable<Lecture, Placement> assignments = new Hashtable<Lecture, Placement>();
        assignments.put(value.variable(), value);
        if (!isSatisfiedSeq(assignments, true, conflicts))
            conflicts.add(value);
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
            if (!isSatisfiedPair(v, v.getAssignment(), value.variable(), value))
                return true;
        }
        Hashtable<Lecture, Placement> assignments = new Hashtable<Lecture, Placement>();
        assignments.put(value.variable(), value);
        return isSatisfiedSeq(assignments, true, null);
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
        if (isHard())
            return 0; // no preference
        if (countAssignedVariables() < 2)
            return 0;
        if (iPreference < 0) { // preference version (violated -> 0, satisfied
                               // -> preference)
            for (Lecture v1 : variables()) {
                if (v1.getAssignment() == null)
                    continue;
                for (Lecture v2 : variables()) {
                    if (v2.getAssignment() == null)
                        continue;
                    if (v1.equals(v2))
                        continue;
                    if (!isSatisfiedPair(v1, v1.getAssignment(), v2, v2.getAssignment()))
                        return 0;
                }
            }
            if (!isSatisfiedSeq(null, true, null))
                return 0;
            return iPreference;
        } else { // discouraged version (violated -> prefernce, satisfied -> 0)
            for (Lecture v1 : variables()) {
                if (v1.getAssignment() == null)
                    continue;
                for (Lecture v2 : variables()) {
                    if (v2.getAssignment() == null)
                        continue;
                    if (v1.equals(v2))
                        continue;
                    if (!isSatisfiedPair(v1, v1.getAssignment(), v2, v2.getAssignment()))
                        return iPreference;
                }
            }
            if (!isSatisfiedSeq(null, true, null))
                return iPreference;
            return 0;
        }
    }

    /** Current constraint preference (if given placement is assigned) */
    public int getCurrentPreference(Placement placement) {
        // if (isHard()) return 0; //no preference
        if (iPreference < 0) { // preference version
            for (Lecture v1 : variables()) {
                if (v1.getAssignment() == null)
                    continue;
                if (v1.equals(placement.variable()))
                    continue;
                if (!isSatisfiedPair(v1, v1.getAssignment(), placement.variable(), placement))
                    return 0;
            }
            if (isBackToBack(getType())) {
                Hashtable<Lecture, Placement> assignment = new Hashtable<Lecture, Placement>();
                assignment.put(placement.variable(), placement);
                if (!isSatisfiedSeq(assignment, true, null))
                    return 0;
            }
            return iPreference;
        } else { // discouraged version
            for (Lecture v1 : variables()) {
                if (v1.getAssignment() == null)
                    continue;
                if (v1.equals(placement.variable()))
                    continue;
                if (!isSatisfiedPair(v1, v1.getAssignment(), placement.variable(), placement))
                    return iPreference;
            }
            if (isBackToBack(getType())) {
                Hashtable<Lecture, Placement> assignments = new Hashtable<Lecture, Placement>();
                assignments.put(placement.variable(), placement);
                if (!isSatisfiedSeq(assignments, true, null))
                    return iPreference;
            }
            return 0;
        }
    }

    @Override
    public void unassigned(long iteration, Placement value) {
        super.unassigned(iteration, value);
        if (iIsRequired || iIsProhibited)
            return;
        ((TimetableModel) getModel()).getGlobalGroupConstraintPreferenceCounter().dec(iLastPreference);
        iLastPreference = getCurrentPreference();
        ((TimetableModel) getModel()).getGlobalGroupConstraintPreferenceCounter().inc(iLastPreference);
    }

    @Override
    public void assigned(long iteration, Placement value) {
        super.assigned(iteration, value);
        if (iIsRequired || iIsProhibited)
            return;
        ((TimetableModel) getModel()).getGlobalGroupConstraintPreferenceCounter().dec(iLastPreference);
        iLastPreference = getCurrentPreference();
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
        return getName(iType);
    }

    public static String getName(String type) {
        return type;
    }

    private static int getGapMin(String type) {
        if (type.equals(TYPE_BTB))
            return 0;
        else if (type.equals(TYPE_BTB_TIME))
            return 0;
        else if (type.equals(TYPE_NHB_1))
            return 6 * 2;
        else if (type.equals(TYPE_NHB_1_5))
            return 6 * 3;
        else if (type.equals(TYPE_NHB_2))
            return 6 * 4;
        else if (type.equals(TYPE_NHB_3))
            return 6 * 6;
        else if (type.equals(TYPE_NHB_4))
            return 6 * 8;
        else if (type.equals(TYPE_NHB_4_5))
            return 6 * 9;
        else if (type.equals(TYPE_NHB_5))
            return 6 * 10;
        else if (type.equals(TYPE_NHB_6))
            return 6 * 12;
        else if (type.equals(TYPE_NHB_7))
            return 6 * 14;
        else if (type.equals(TYPE_NHB_8))
            return 6 * 16;
        else if (type.equals(TYPE_NHB_GTE_1))
            return 6 * 1;
        else if (type.equals(TYPE_NHB_LT_6))
            return 0;
        return -1;
    }

    private static int getGapMax(String type) {
        if (type.equals(TYPE_BTB))
            return 0;
        else if (type.equals(TYPE_BTB_TIME))
            return 0;
        else if (type.equals(TYPE_NHB_1))
            return 6 * 2;
        else if (type.equals(TYPE_NHB_1_5))
            return 6 * 3;
        else if (type.equals(TYPE_NHB_2))
            return 6 * 4;
        else if (type.equals(TYPE_NHB_3))
            return 6 * 6;
        else if (type.equals(TYPE_NHB_4))
            return 6 * 8;
        else if (type.equals(TYPE_NHB_4_5))
            return 6 * 9;
        else if (type.equals(TYPE_NHB_5))
            return 6 * 10;
        else if (type.equals(TYPE_NHB_6))
            return 6 * 12;
        else if (type.equals(TYPE_NHB_7))
            return 6 * 14;
        else if (type.equals(TYPE_NHB_8))
            return 6 * 16;
        else if (type.equals(TYPE_NHB_GTE_1))
            return Constants.SLOTS_PER_DAY;
        else if (type.equals(TYPE_NHB_LT_6))
            return 6 * 11;
        return -1;
    }

    private static boolean isBackToBack(String type) {
        if (type.equals(TYPE_BTB))
            return true;
        if (type.equals(TYPE_BTB_TIME))
            return true;
        if (type.equals(TYPE_NHB_1))
            return true;
        if (type.equals(TYPE_NHB_1_5))
            return true;
        if (type.equals(TYPE_NHB_2))
            return true;
        if (type.equals(TYPE_NHB_3))
            return true;
        if (type.equals(TYPE_NHB_4))
            return true;
        if (type.equals(TYPE_NHB_4_5))
            return true;
        if (type.equals(TYPE_NHB_5))
            return true;
        if (type.equals(TYPE_NHB_6))
            return true;
        if (type.equals(TYPE_NHB_7))
            return true;
        if (type.equals(TYPE_NHB_8))
            return true;
        if (type.equals(TYPE_NHB_GTE_1))
            return true;
        if (type.equals(TYPE_NHB_LT_6))
            return true;
        return false;
    }

    private static boolean isBackToBackTime(String type) {
        if (type.equals(TYPE_BTB_TIME))
            return true;
        if (type.equals(TYPE_NHB_1))
            return true;
        if (type.equals(TYPE_NHB_1_5))
            return true;
        if (type.equals(TYPE_NHB_2))
            return true;
        if (type.equals(TYPE_NHB_3))
            return true;
        if (type.equals(TYPE_NHB_4))
            return true;
        if (type.equals(TYPE_NHB_4_5))
            return true;
        if (type.equals(TYPE_NHB_5))
            return true;
        if (type.equals(TYPE_NHB_6))
            return true;
        if (type.equals(TYPE_NHB_7))
            return true;
        if (type.equals(TYPE_NHB_8))
            return true;
        if (type.equals(TYPE_NHB_GTE_1))
            return true;
        if (type.equals(TYPE_NHB_LT_6))
            return true;
        return false;
    }

    private static boolean sameRoom(String type) {
        if (type.equals(TYPE_SAME_ROOM))
            return true;
        if (type.equals(TYPE_MEET_WITH))
            return true;
        return false;
    }

    private static boolean isPrecedence(String type) {
        if (type.equals(TYPE_PRECEDENCE))
            return true;
        return false;
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

    private static boolean sameStudents(String type) {
        if (type.equals(TYPE_SAME_STUDENTS))
            return true;
        return false;
    }

    private static boolean sameInstructor(String type) {
        if (type.equals(TYPE_SAME_INSTR))
            return true;
        return false;
    }

    public static boolean canShareRooms(String type) {
        if (type.equals(TYPE_CAN_SHARE_ROOM))
            return true;
        if (type.equals(TYPE_MEET_WITH))
            return true;
        return false;
    }

    private static boolean sameStartHour(String type) {
        return (type.equals(TYPE_SAME_START));
    }

    private static boolean sameHours(String type) {
        return (type.equals(TYPE_SAME_TIME) || type.equals(TYPE_MEET_WITH));
    }

    private static boolean sameDays(String type) {
        if (type.equals(TYPE_SAME_DAYS) || type.equals(TYPE_MEET_WITH))
            return true;
        return false;
    }

    private static boolean notOverlap(String type) {
        return (type.equals(TYPE_DIFF_TIME));
    }

    private static boolean isBackToBackDay(String type) {
        return (type.equals(TYPE_BTB_DAY));
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

    private static boolean isNrDaysBetweenGreaterThanOne(String type) {
        return (type.equals(TYPE_NDB_GT_1));
    }

    private static boolean isChildrenNotOverlap(String type) {
        return (type.equals(TYPE_CH_NOTOVERLAP));
    }

    private static boolean isFollowingDay(String type) {
        return (type.equals(TYPE_FOLLOWING_DAY));
    }

    private static boolean isEveryOtherDay(String type) {
        return (type.equals(TYPE_EVERY_OTHER_DAY));
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

    private static boolean sameHours(int start1, int len1, int start2, int len2) {
        if (len1 > len2)
            return sameHours(start2, len2, start1, len1);
        start1 %= Constants.SLOTS_PER_DAY;
        start2 %= Constants.SLOTS_PER_DAY;
        return (start1 >= start2 && start1 + len1 <= start2 + len2);
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

    private boolean isSatisfiedSeq(Hashtable<Lecture, Placement> assignments, boolean considerCurrentAssignments,
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

    private Set<Placement> isSatisfiedRecursive(int idx, Hashtable<Lecture, Placement> assignments,
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

    private boolean isSatisfiedSeqCheck(Hashtable<Lecture, Placement> assignments, boolean considerCurrentAssignments,
            Set<Placement> conflicts) {
        int gapMin = getGapMin(getType());
        int gapMax = getGapMax(getType());
        if (gapMin < 0 || gapMax < 0)
            return true;

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
        if (isHard())
            return true;
        if (countAssignedVariables() < 2)
            return true;
        return (getPreference() < 0 && getCurrentPreference() < 0) || getPreference() == 0
                || (getPreference() > 0 && getCurrentPreference() == 0);
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
                for (Enumeration<Long> e1 = lec1.getChildrenSubpartIds(); e1.hasMoreElements();) {
                    Long subpartId = e1.nextElement();
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

    private boolean isSatisfiedPair(Lecture lec1, Placement plc1, Lecture lec2, Placement plc2) {
        if (iIsRequired || (!iIsProhibited && iPreference < 0)) {
            if (sameRoom(getType()) || (isBackToBack(getType()) && !isBackToBackTime(getType()))) {
                if (!plc1.sameRooms(plc2))
                    return false;
            }
            if (sameStartHour(getType())) {
                if ((plc1.getTimeLocation().getStartSlot() % Constants.SLOTS_PER_DAY) != (plc2.getTimeLocation()
                        .getStartSlot() % Constants.SLOTS_PER_DAY))
                    return false;
            }
            if (sameHours(getType())) {
                if (!sameHours(plc1.getTimeLocation().getStartSlot(), plc1.getTimeLocation().getLength(), plc2
                        .getTimeLocation().getStartSlot(), plc2.getTimeLocation().getLength()))
                    return false;
            }
            if (sameDays(getType()) || isBackToBack(getType())) {
                if (!sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray()))
                    return false;
            }
            if (notOverlap(getType())) {
                if (plc1.getTimeLocation().hasIntersection(plc2.getTimeLocation()))
                    return false;
            }
            if (sameStudents(getType())) {
                if (JenrlConstraint.isInConflict(plc1, plc2, ((TimetableModel)getModel()).getDistanceMetric()))
                    return false;
            }
            if (sameInstructor(getType())) {
                TimeLocation t1 = plc1.getTimeLocation(), t2 = plc2.getTimeLocation();
                if (t1.shareDays(t2) && t1.shareWeeks(t2)) {
                    if (t1.shareHours(t2))
                        return false; // overlap
                    int s1 = t1.getStartSlot() % Constants.SLOTS_PER_DAY;
                    int s2 = t2.getStartSlot() % Constants.SLOTS_PER_DAY;
                    if (s1 + t1.getLength() == s2 || s2 + t2.getLength() == s1) { // back-to-back
                        TimetableModel m = (TimetableModel) getModel();
                        double distance = Placement.getDistanceInMeters(m.getDistanceMetric(), plc1, plc2);
                        if (distance > m.getDistanceMetric().getInstructorProhibitedLimit())
                            return false;
                    }
                }
            }
            if (isPrecedence(getType())) {
                return isPrecedence(plc1, plc2, true);
            }
            if (isBackToBackDay(getType())) {
                if (sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray()))
                    return false;
                return isBackToBackDays(plc1.getTimeLocation(), plc2.getTimeLocation());
            }
            if (isNrDaysBetweenGreaterThanOne(getType())) {
                if (sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray()))
                    return false;
                return isNrDaysBetweenGreaterThanOne(plc1.getTimeLocation(), plc2.getTimeLocation());
            }
            if (isChildrenNotOverlap(getType())) {
                return isChildrenNotOverlap(lec1, plc1, lec2, plc2);
            }
            if (isFollowingDay(getType())) {
                return isFollowingDay(plc1, plc2, true);
            }
            if (isEveryOtherDay(getType())) {
                return isEveryOtherDay(plc1, plc2, true);
            }
        } else if (iIsProhibited || (!iIsRequired && iPreference > 0)) {
            if (sameRoom(getType())) {
                if (plc1.sameRooms(plc2))
                    return false;
            }
            if (sameHours(getType())) {
                if (plc1.getTimeLocation().shareHours(plc2.getTimeLocation()))
                    return false;
            }
            if (sameStartHour(getType())) {
                if ((plc1.getTimeLocation().getStartSlot() % Constants.SLOTS_PER_DAY) == (plc2.getTimeLocation()
                        .getStartSlot() % Constants.SLOTS_PER_DAY))
                    return false;
            }
            if (sameDays(getType())) {
                if (plc1.getTimeLocation().shareDays(plc2.getTimeLocation()))
                    return false;
            }
            if (notOverlap(getType())) {
                if (!plc1.getTimeLocation().hasIntersection(plc2.getTimeLocation()))
                    return false;
            }
            if (isBackToBack(getType())) { // still same time
                if (!sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray()))
                    return false;
                if (!isBackToBackTime(getType())) { // still same room
                    if (!plc1.sameRooms(plc2))
                        return false;
                }
            }
            if (isPrecedence(getType())) {
                return isPrecedence(plc1, plc2, false);
            }
            if (isBackToBackDay(getType())) {
                if (sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray()))
                    return false;
                return !isBackToBackDays(plc1.getTimeLocation(), plc2.getTimeLocation());
            }
            if (isNrDaysBetweenGreaterThanOne(getType())) {
                if (sameDays(plc1.getTimeLocation().getDaysArray(), plc2.getTimeLocation().getDaysArray()))
                    return false;
                return !isNrDaysBetweenGreaterThanOne(plc1.getTimeLocation(), plc2.getTimeLocation());
            }
            if (isFollowingDay(getType())) {
                return isFollowingDay(plc1, plc2, false);
            }
            if (isEveryOtherDay(getType())) {
                return isEveryOtherDay(plc1, plc2, false);
            }
        }
        return true;
    }

    /*
     * public void getInfo(Dictionary info) { StringBuffer varNames = new
     * StringBuffer(); for (Enumeration
     * e=variables().elements();e.hasMoreElements();) { Variable variable =
     * (Variable)e.nextElement(); varNames.append(varNames.length()>0?", ":"");
     * varNames.append(variable.getName()); if (variable.getAssignment()!=null)
     * varNames.append(" "+variable.getAssignment().getName()); }
     * info.put("gc"+iId,
     * iType).getDescription()+" (pref="+getDescription()+" ("
     * +iIsRequired+"/"+iIsProhibited
     * +"/"+iPreference+")"+", current="+getCurrentPreference
     * ()+", vars=["+varNames+"])"); }
     */

    /*
     * public static class GroupConstraintTypes { private static Vector
     * sGroupConstraintTypes = null;
     * 
     * static { try { sGroupConstraintTypes = new Vector(); for (Iterator
     * i=DistributionType.findAll().iterator();i.hasNext();) { DistributionType
     * type = (DistributionType)i.next(); sGroupConstraintTypes.addElement(new
     * GroupConstraintType
     * (type.getRequirementId().intValue(),type.getReference()
     * ,type.getLabel())); } } catch (Exception e) {
     * sLogger.error(e.getMessage(),e); sGroupConstraintTypes.addElement(new
     * GroupConstraintType(1,"BTB","back-to-back"));
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(2,"BTB_TIME","back-to-back time"));
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(3,"SAME_TIME","same time"));
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(4,"SAME_DAYS","same days"));
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(5,"NHB(1)","1 hr between"));
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(6,"NHB(2)","2 hrs between"));
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(7,"NHB(3)","3 hrs between"));
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(8,"NHB(4)","4 hrs between"));
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(9,"NHB(5)","5 hrs between"));
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(10,"NHB(6)","6 hrs between"));
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(11,"NHB(7)","7 hrs between"));
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(12,"NHB(8)","8 hrs between"));
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(13,"DIFF_TIME","different time"));
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(14,"NHB(1.5)","1.5 hrs between"));
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(15,"NHB(4.5)","4.5 hrs between"));
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(16,"SAME_START","same start time"));
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(17,"SAME_ROOM","same room"));
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(18,"NHB_GTE(1)","greater than or equal to 1 hour between"
     * )); sGroupConstraintTypes.addElement(new
     * GroupConstraintType(19,"NHB_LT(6)","less than 6 hours between")); } if
     * (getGroupConstraintType("SAME_STUDENTS")==null)
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(20,"SAME_STUDENTS","same students")); if
     * (getGroupConstraintType("SAME_INSTR")==null)
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(21,"SAME_INSTR","same instructor")); if
     * (getGroupConstraintType("CAN_SHARE_ROOM")==null)
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(22,"CAN_SHARE_ROOM","can share rooms")); if
     * (getGroupConstraintType("PRECEDENCE")==null)
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(24,"PRECEDENCE","precedence")); if
     * (getGroupConstraintType("BTB_DAY")==null)
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(26,"BTB_DAY","back-to-back day")); if
     * (getGroupConstraintType("MIN_GRUSE(10x1h)")==null)
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(27,"MIN_GRUSE(10x1h)","minimize use of 1h groups"));
     * if (getGroupConstraintType("MIN_GRUSE(5x2h)")==null)
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(28,"MIN_GRUSE(5x2h)","minimize use of 2h groups"));
     * if (getGroupConstraintType("MIN_GRUSE(3x3h)")==null)
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(29,"MIN_GRUSE(3x3h)","minimize use of 3h groups"));
     * if (getGroupConstraintType("MIN_GRUSE(2x5h)")==null)
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(30,"MIN_GRUSE(2x5h)","minimize use of 5h groups"));
     * if (getGroupConstraintType("MEET_WITH")==null)
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(31,"MEET_WITH","meet together")); if
     * (getGroupConstraintType("NDB_GT_1")==null)
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(32,"NDB_GT_1",">1d btw")); if
     * (getGroupConstraintType("CH_NOTOVERLAP")==null)
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(33,"NDB_GT_1","ch no overlap")); if
     * (getGroupConstraintType("FOLLOWING_DAY")==null)
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(34,"FOLLOWING_DAY","following day")); if
     * (getGroupConstraintType("EVERY_OTHER_DAY")==null)
     * sGroupConstraintTypes.addElement(new
     * GroupConstraintType(35,"EVERY_OTHER_DAY","every other day")); }
     * 
     * public static Enumeration elements() { return
     * sGroupConstraintTypes.elements(); }
     * 
     * public static int size() { return sGroupConstraintTypes.size(); }
     * 
     * public static GroupConstraintType elementAt(int i) { return
     * (GroupConstraintType) sGroupConstraintTypes.elementAt(i); }
     * 
     * public static GroupConstraintType getGroupConstraintType(int id) { for
     * (Enumeration e=sGroupConstraintTypes.elements();e.hasMoreElements();) {
     * GroupConstraintType gc = (GroupConstraintType)e.nextElement(); if
     * (gc.getId()==id) return gc; } return null; }
     * 
     * public static GroupConstraintType getGroupConstraintType(String type) {
     * for (Enumeration e=sGroupConstraintTypes.elements();e.hasMoreElements();)
     * { GroupConstraintType gc = (GroupConstraintType)e.nextElement(); if
     * (gc.getType().equalsIgnoreCase(type)) return gc; } return null; }
     * 
     * public static class GroupConstraintType implements Comparable { int iId;
     * String iType; String iDescription;
     * 
     * private GroupConstraintType(int id, String type, String desc) { iId = id;
     * iType = type; iDescription = desc; }
     * 
     * public int getId() { return iId; } public String getType() { return
     * iType; } public String getDescription() { return iDescription; }
     * 
     * public boolean equals(Object o) { if (o==null || !(o instanceof
     * GroupConstraintType)) return false; return getId() ==
     * ((GroupConstraintType)o; }
     * 
     * public int compareTo(Object o) { if (o==null || !(o instanceof
     * GroupConstraintType)) return 0; GroupConstraintType g =
     * (GroupConstraintType)o; return getId() - ((GroupConstraintType)o; }
     * 
     * } }
     */
}
