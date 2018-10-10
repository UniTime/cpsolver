package org.cpsolver.exam.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.cpsolver.exam.criteria.DistributionPenalty;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;


/**
 * Distribution binary constraint. <br>
 * <br>
 * The following binary distribution constraints are implemented
 * <ul>
 * <li>Same room
 * <li>Different room
 * <li>Same period
 * <li>Different period
 * <li>Precedence
 * <li>Same day
 * </ul>
 * <br>
 * <br>
 * 
 * @version ExamTT 1.3 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2014 Tomas Muller<br>
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
public class ExamDistributionConstraint extends ConstraintWithContext<Exam, ExamPlacement, ExamDistributionConstraint.Context> {
    /** Same room constraint type */
    public static final int sDistSameRoom = 0;
    /** Different room constraint type */
    public static final int sDistDifferentRoom = 1;
    /** Same period constraint type */
    public static final int sDistSamePeriod = 2;
    /** Different period constraint type */
    public static final int sDistDifferentPeriod = 3;
    /** Precedence constraint type */
    public static final int sDistPrecedence = 4;
    /** Precedence constraint type (reverse order) */
    public static final int sDistPrecedenceRev = 5;
    /** Same day constraint type */
    public static final int sDistSameDay = 6;
    /** Different day constraint type */
    public static final int sDistDifferentDay = 7;
    /** Distribution type name */
    public static final String[] sDistType = new String[] { "same-room", "different-room", "same-period",
            "different-period", "precedence", "precedence-rev", "same-day", "different-day"};
    private int iType = -1;
    private boolean iHard = true;
    private int iWeight = 0;

    /**
     * Constructor
     * 
     * @param id
     *            constraint unique id
     * @param type
     *            constraint type
     * @param hard
     *            true if the constraint is hard (cannot be violated)
     * @param weight
     *            if not hard, penalty for violation
     */
    public ExamDistributionConstraint(long id, int type, boolean hard, int weight) {
        iId = id;
        iType = type;
        iHard = hard;
        iWeight = weight;
    }

    /**
     * Constructor
     * 
     * @param id
     *            constraint unique id
     * @param type
     *            constraint type (EX_SAME_PREF, EX_SAME_ROOM, or EX_PRECEDENCE)
     * @param pref
     *            preference (R/P for required/prohibited, or -2, -1, 0, 1, 2
     *            for preference (from preferred to discouraged))
     */
    public ExamDistributionConstraint(long id, String type, String pref) {
        iId = id;
        boolean neg = "P".equals(pref) || "2".equals(pref) || "1".equals(pref);
        if ("EX_SAME_PER".equals(type)) {
            iType = (neg ? sDistDifferentPeriod : sDistSamePeriod);
        } else if ("EX_SAME_ROOM".equals(type)) {
            iType = (neg ? sDistDifferentRoom : sDistSameRoom);
        } else if ("EX_PRECEDENCE".equals(type)) {
            iType = (neg ? sDistPrecedenceRev : sDistPrecedence);
        } else if ("EX_SAME_DAY".equals(type)) {
            iType = (neg ? sDistDifferentDay : sDistSameDay);
        } else
            throw new RuntimeException("Unkown type " + type);
        if ("P".equals(pref) || "R".equals(pref))
            iHard = true;
        else {
            iHard = false;
            iWeight = Integer.parseInt(pref) * Integer.parseInt(pref);
        }
    }

    /**
     * Constructor
     * 
     * @param id
     *            constraint unique id
     * @param type
     *            constraint type name
     * @param hard true if the constraint is hard
     * @param weight constraint penalty if violated (for soft constraint)
     */
    public ExamDistributionConstraint(long id, String type, boolean hard, int weight) {
        iId = id;
        for (int i = 0; i < sDistType.length; i++)
            if (sDistType[i].equals(type))
                iType = i;
        if (iType < 0)
            throw new RuntimeException("Unknown type '" + type + "'.");
        iHard = hard;
        iWeight = weight;
    }

    /**
     * True if hard (must be satisfied), false for soft (should be satisfied)
     */
    @Override
    public boolean isHard() {
        return iHard;
    }

    /**
     * If not hard, penalty for violation
     * @return constraint penalty if violated
     */
    public int getWeight() {
        return iWeight;
    }

    /**
     * Constraint type
     * @return constraint type
     */
    public int getType() {
        return iType;
    }

    /**
     * Constraint type name
     * @return constraint type name (one of {@link ExamDistributionConstraint#sDistType})
     */
    public String getTypeString() {
        return sDistType[iType];
    }

    /**
     * String representation -- constraint type name (exam 1, exam 2)
     */
    @Override
    public String toString() {
        return getTypeString() + " (" + variables() + ")";
    }

    /**
     * Compute conflicts -- there is a conflict if the other variable is
     * assigned and
     * {@link ExamDistributionConstraint#check(ExamPlacement, ExamPlacement)} is
     * false
     */
    @Override
    public void computeConflicts(Assignment<Exam, ExamPlacement> assignment, ExamPlacement givenPlacement, Set<ExamPlacement> conflicts) {
        boolean before = true;
        for (Exam exam : variables()) {
            if (exam.equals(givenPlacement.variable())) {
                before = false;
                continue;
            }
            ExamPlacement placement = assignment.getValue(exam);
            if (placement == null)
                continue;
            if (!check(before ? placement : givenPlacement, before ? givenPlacement : placement))
                conflicts.add(placement);
        }
    }

    /**
     * Check for conflict -- there is a conflict if the other variable is
     * assigned and
     * {@link ExamDistributionConstraint#check(ExamPlacement, ExamPlacement)} is
     * false
     */
    @Override
    public boolean inConflict(Assignment<Exam, ExamPlacement> assignment, ExamPlacement givenPlacement) {
        boolean before = true;
        for (Exam exam : variables()) {
            if (exam.equals(givenPlacement.variable())) {
                before = false;
                continue;
            }
            ExamPlacement placement = assignment.getValue(exam);
            if (placement == null)
                continue;
            if (!check(before ? placement : givenPlacement, before ? givenPlacement : placement))
                return true;
        }
        return false;
    }

    /**
     * Consistency check --
     * {@link ExamDistributionConstraint#check(ExamPlacement, ExamPlacement)} is
     * called
     */
    @Override
    public boolean isConsistent(ExamPlacement first, ExamPlacement second) {
        boolean before = (variables().indexOf(first.variable()) < variables().indexOf(second.variable()));
        return check(before ? first : second, before ? second : first);
    }

    /**
     * Check assignments of the given exams
     * 
     * @param first
     *            assignment of the first exam
     * @param second
     *            assignment of the second exam
     * @return true, if the constraint is satisfied
     */
    public boolean check(ExamPlacement first, ExamPlacement second) {
        switch (getType()) {
            case sDistPrecedence:
                return first.getPeriod().getIndex() < second.getPeriod().getIndex();
            case sDistPrecedenceRev:
                return first.getPeriod().getIndex() > second.getPeriod().getIndex();
            case sDistSamePeriod:
                return first.getPeriod().getIndex() == second.getPeriod().getIndex();
            case sDistDifferentPeriod:
                return first.getPeriod().getIndex() != second.getPeriod().getIndex();
            case sDistSameRoom:
                return first.getRoomPlacements().containsAll(second.getRoomPlacements())
                        || second.getRoomPlacements().containsAll(first.getRoomPlacements());
            case sDistDifferentRoom:
                for (Iterator<ExamRoomPlacement> i = first.getRoomPlacements().iterator(); i.hasNext();)
                    if (second.getRoomPlacements().contains(i.next()))
                        return false;
                return true;
            case sDistSameDay:
                return first.getPeriod().getDay() == second.getPeriod().getDay();
            case sDistDifferentDay:
                return first.getPeriod().getDay() != second.getPeriod().getDay();
  
            default:
                return false;
        }
    }

    /**
     * Compare with other constraint for equality
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ExamDistributionConstraint))
            return false;
        ExamDistributionConstraint c = (ExamDistributionConstraint) o;
        return getType() == c.getType() && getId() == c.getId();
    }

    /**
     * Return true if this is hard constraint or this is a soft constraint
     * without any violation
     * @param assignment current assignment
     * @return true if the constraint is satisfied
     */
    public boolean isSatisfied(Assignment<Exam, ExamPlacement> assignment) {
        return isSatisfied(assignment, null);
    }

    /**
     * Return true if this is hard constraint or this is a soft constraint
     * without any violation
     * 
     * @param assignment current assignment
     * @param p
     *            exam assignment to be made
     * @return true if the constraint is satisfied
     */
    public boolean isSatisfied(Assignment<Exam, ExamPlacement> assignment, ExamPlacement p) {
        if (isHard())
            return true;
        switch (getType()) {
            case sDistPrecedence:
                ExamPeriod last = null;
                for (Exam exam : variables()) {
                    ExamPlacement placement = (p != null && exam.equals(p.variable()) ? p : assignment.getValue(exam));
                    if (placement == null)
                        continue;
                    if (last == null || last.getIndex() < placement.getPeriod().getIndex())
                        last = placement.getPeriod();
                    else
                        return false;
                }
                return true;
            case sDistPrecedenceRev:
                last = null;
                for (Exam exam : variables()) {
                    ExamPlacement placement = (p != null && exam.equals(p.variable()) ? p : assignment.getValue(exam));
                    if (placement == null)
                        continue;
                    if (last == null || last.getIndex() > placement.getPeriod().getIndex())
                        last = placement.getPeriod();
                    else
                        return false;
                }
                return true;
            case sDistSamePeriod:
                ExamPeriod period = null;
                for (Exam exam : variables()) {
                    ExamPlacement placement = (p != null && exam.equals(p.variable()) ? p : assignment.getValue(exam));
                    if (placement == null)
                        continue;
                    if (period == null)
                        period = placement.getPeriod();
                    else if (period.getIndex() != placement.getPeriod().getIndex())
                        return false;
                }
                return true;
            case sDistDifferentPeriod:
                HashSet<ExamPeriod> periods = new HashSet<ExamPeriod>();
                for (Exam exam : variables()) {
                    ExamPlacement placement = (p != null && exam.equals(p.variable()) ? p : assignment.getValue(exam));
                    if (placement == null)
                        continue;
                    if (!periods.add(placement.getPeriod()))
                        return false;
                }
                return true;
            case sDistSameRoom:
                Set<ExamRoomPlacement> rooms = null;
                for (Exam exam : variables()) {
                    ExamPlacement placement = (p != null && exam.equals(p.variable()) ? p : assignment.getValue(exam));
                    if (placement == null)
                        continue;
                    if (rooms == null)
                        rooms = placement.getRoomPlacements();
                    else if (!rooms.containsAll(placement.getRoomPlacements())
                            || !placement.getRoomPlacements().containsAll(rooms))
                        return false;
                }
                return true;
            case sDistDifferentRoom:
                HashSet<ExamRoomPlacement> allRooms = new HashSet<ExamRoomPlacement>();
                for (Exam exam : variables()) {
                    ExamPlacement placement = (p != null && exam.equals(p.variable()) ? p : assignment.getValue(exam));
                    if (placement == null)
                        continue;
                    for (ExamRoomPlacement room : placement.getRoomPlacements()) {
                        if (!allRooms.add(room))
                            return false;
                    }
                }
                return true;
            case sDistSameDay:
                ExamPeriod period1 = null;
                for (Exam exam : variables()) {
                    ExamPlacement placement = (p != null && exam.equals(p.variable()) ? p : assignment.getValue(exam));
                    if (placement == null)
                        continue;
                    if (period1 == null)
                        period1 = placement.getPeriod();
                    else if (period1.getDay() != placement.getPeriod().getDay())
                        return false;
                }
                return true;
            case sDistDifferentDay:
                HashSet<Integer> days = new HashSet<Integer>();
                for (Exam exam : variables()) {
                    ExamPlacement placement = (p != null && exam.equals(p.variable()) ? p : assignment.getValue(exam));
                    if (placement == null)
                        continue;
                    if (!days.add(placement.getPeriod().getDay()))
                        return false;
                }
                return true;

            default:
                return false;
        }
    }

    /** True if the constraint is related to rooms 
     * @return true if the constraint is related to room placement
     **/
    public boolean isRoomRelated() {
        return iType == sDistSameRoom || iType == sDistDifferentRoom;
    }

    /** True if the constraint is related to periods 
     * @return true if the constraint is related to period placement
     **/
    public boolean isPeriodRelated() {
        return !isRoomRelated();
    }
    
    @Override
    public Context createAssignmentContext(Assignment<Exam, ExamPlacement> assignment) {
        return new Context(assignment);
    }
    
    public class Context implements AssignmentConstraintContext<Exam, ExamPlacement> {
        private boolean iIsSatisfied;
        
        public Context(Assignment<Exam, ExamPlacement> assignment) {
            iIsSatisfied = isSatisfied(assignment);
            if (!iIsSatisfied)
                ((DistributionPenalty)getModel().getCriterion(DistributionPenalty.class)).inc(assignment, getWeight());
        }

        @Override
        public void assigned(Assignment<Exam, ExamPlacement> assignment, ExamPlacement placement) {
            if (!isHard() && iIsSatisfied != isSatisfied(assignment)) {
                iIsSatisfied = !iIsSatisfied;
                ((DistributionPenalty)getModel().getCriterion(DistributionPenalty.class)).inc(assignment, iIsSatisfied ? -getWeight() : getWeight());
            }
        }
        
        @Override
        public void unassigned(Assignment<Exam, ExamPlacement> assignment, ExamPlacement placement) {
            if (!isHard() && iIsSatisfied != isSatisfied(assignment)) {
                iIsSatisfied = !iIsSatisfied;
                ((DistributionPenalty)getModel().getCriterion(DistributionPenalty.class)).inc(assignment, iIsSatisfied ? -getWeight() : getWeight());
            }
        }
    }
}