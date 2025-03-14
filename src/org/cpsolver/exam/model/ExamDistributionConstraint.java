package org.cpsolver.exam.model;

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
 * @author  Tomas Muller
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
    @Deprecated
    public static int sDistSameRoom = DistributionType.SameRoom.ordinal();
    private DistributionType iType = null;
    private boolean iHard = true;
    private int iWeight = 0;
    
    public static enum DistributionType {
        /** Same room constraint type */
        SameRoom("same-room", "EX_SAME_ROOM", false, new RoomCheck() {
            @Override
            public boolean isSatisfied(ExamPlacement first, ExamPlacement second) {
                return first.getRoomPlacements().containsAll(second.getRoomPlacements()) || second.getRoomPlacements().containsAll(first.getRoomPlacements());
            }
            @Override
            public boolean isSatisfied(ExamPlacement first, ExamRoomPlacement second) {
                return first.getRoomPlacements().contains(second);
            }}),
        /** Different room constraint type */
        DifferentRoom("different-room", "EX_SAME_ROOM", true, new RoomCheck() {
            @Override
            public boolean isSatisfied(ExamPlacement first, ExamPlacement second) {
                for (Iterator<ExamRoomPlacement> i = first.getRoomPlacements().iterator(); i.hasNext();)
                    if (second.getRoomPlacements().contains(i.next()))
                        return false;
                return true;
            }
            @Override
            public boolean isSatisfied(ExamPlacement first, ExamRoomPlacement second) {
                return !first.getRoomPlacements().contains(second);
            }}),
        /** Same period constraint type */
        SamePeriod("same-period", "EX_SAME_PER", false, new PeriodCheck() {
            @Override
            public boolean isSatisfied(ExamPlacement first, ExamPlacement second) {
                return first.getPeriod().getIndex() == second.getPeriod().getIndex();
            }
            @Override
            public boolean isSatisfied(ExamPeriod first, ExamPeriod second) {
                return first.getIndex() == second.getIndex();
            }}),
        /** Different period constraint type */
        DifferentPeriod("different-period", "EX_SAME_PER", true, new PeriodCheck() {
            @Override
            public boolean isSatisfied(ExamPlacement first, ExamPlacement second) {
                return first.getPeriod().getIndex() != second.getPeriod().getIndex();
            }
            @Override
            public boolean isSatisfied(ExamPeriod first, ExamPeriod second) {
                return first.getIndex() != second.getIndex();
            }}),
        /** Precedence constraint type */
        Precedence("precedence", "EX_PRECEDENCE", false, new PeriodCheck() {
            @Override
            public boolean isSatisfied(ExamPlacement first, ExamPlacement second) {
                return first.getPeriod().getIndex() < second.getPeriod().getIndex();
            }
            @Override
            public boolean isSatisfied(ExamPeriod first, ExamPeriod second) {
                return first.getIndex() < second.getIndex();
            }}),
        /** Precedence constraint type (reverse order) */
        PrecedenceRev("precedence-rev", "EX_PRECEDENCE", true, new PeriodCheck() {
            @Override
            public boolean isSatisfied(ExamPlacement first, ExamPlacement second) {
                return first.getPeriod().getIndex() > second.getPeriod().getIndex();
            }
            @Override
            public boolean isSatisfied(ExamPeriod first, ExamPeriod second) {
                return first.getIndex() > second.getIndex();
            }}),
        /** Same day constraint type */
        SameDay("same-day", "EX_SAME_DAY", false, new PeriodCheck() {
            @Override
            public boolean isSatisfied(ExamPlacement first, ExamPlacement second) {
                return first.getPeriod().getDay() == second.getPeriod().getDay();
            }
            @Override
            public boolean isSatisfied(ExamPeriod first, ExamPeriod second) {
                return first.getDay() == second.getDay();
            }}),
        /** Different day constraint type */
        DifferentDay("different-day", "EX_SAME_DAY", true, new PeriodCheck() {
            @Override
            public boolean isSatisfied(ExamPlacement first, ExamPlacement second) {
                return first.getPeriod().getDay() != second.getPeriod().getDay();
            }
            @Override
            public boolean isSatisfied(ExamPeriod first, ExamPeriod second) {
                return first.getDay() != second.getDay();
            }}),
        ;
        private String iReference;
        private String iUniTimeReference;
        private boolean iUniTimeNegative;
        private PairCheck iCheck;
        private DistributionType(String reference, String utReference, boolean utNegative, PairCheck check) {
            iReference = reference;
            iUniTimeReference = utReference;
            iUniTimeNegative = utNegative;
            iCheck = check;
        }
        
        public String getReference() { return iReference; }
        public boolean isSatisfied(ExamPlacement first, ExamPlacement second) {
            return iCheck.isSatisfied(first, second);
        }
        public boolean isRoomRelated() { return iCheck instanceof RoomCheck; }
        public boolean isPeriodRelated() { return iCheck instanceof PeriodCheck; }
        public boolean isSatisfied(ExamPeriod first, ExamPeriod second) {
            if (iCheck instanceof PeriodCheck)
                return ((PeriodCheck)iCheck).isSatisfied(first, second);
            else
                return true;
        }
        public boolean isSatisfied(ExamPlacement first, ExamRoomPlacement second) {
            if (iCheck instanceof RoomCheck)
                return ((RoomCheck)iCheck).isSatisfied(first, second);
            else
                return true;
        }
        public String getUniTimeReference() { return iUniTimeReference; }
        public boolean isUniTimeNegative() { return iUniTimeNegative; }
    }
    
    public static interface PairCheck {
        public boolean isSatisfied(ExamPlacement first, ExamPlacement second);
    }
    
    public static interface PeriodCheck extends PairCheck {
        public boolean isSatisfied(ExamPeriod first, ExamPeriod second);
    }
    
    public static interface RoomCheck extends PairCheck {
        public boolean isSatisfied(ExamPlacement first, ExamRoomPlacement second);
    }

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
    public ExamDistributionConstraint(long id, DistributionType type, boolean hard, int weight) {
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
     *            constraint type
     * @param hard
     *            true if the constraint is hard (cannot be violated)
     * @param weight
     *            if not hard, penalty for violation
     * @deprecated use {@link ExamDistributionConstraint#ExamDistributionConstraint(long, DistributionType, boolean, int)}
     */
    @Deprecated
    public ExamDistributionConstraint(long id, int type, boolean hard, int weight) {
        iId = id;
        iType = DistributionType.values()[type];
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
        for (DistributionType t: DistributionType.values()) {
            if (t.getUniTimeReference().equals(type) && t.isUniTimeNegative() == neg) {
                iType = t;
                break;
            }
        }
        if (iType == null)
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
        for (DistributionType t: DistributionType.values()) {
            if (t.getReference().equals(type)) {
                iType = t; break;
            }
        }
        if (iType == null)
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
        return iType.ordinal() - 1;
    }
    
    public DistributionType getDistributionType() {
        return iType;
    }

    /**
     * Constraint type name
     * @return constraint type name (one of {@link DistributionType#getReference()})
     */
    public String getTypeString() {
        return iType.getReference();
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
            if (!iType.isSatisfied(before ? placement : givenPlacement, before ? givenPlacement : placement))
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
            if (!iType.isSatisfied(before ? placement : givenPlacement, before ? givenPlacement : placement))
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
        return iType.isSatisfied(before ? first : second, before ? second : first);
    }

    /**
     * Check assignments of the given exams
     * 
     * @param first
     *            assignment of the first exam
     * @param second
     *            assignment of the second exam
     * @return true, if the constraint is satisfied
     * @deprecated use {@link DistributionType#isSatisfied(ExamPlacement, ExamPlacement)}
     */
    @Deprecated
    public boolean check(ExamPlacement first, ExamPlacement second) {
        return iType.isSatisfied(first, second);
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
        return countViolations(assignment, p) == 0;
    }
    
    /**
     * Return number of violated pairs for a soft constraint and the given placement
     * 
     * @param assignment current assignment
     * @param p
     *            exam assignment to be made
     * @return number of examination pairs violated
     */
    public int countViolations(Assignment<Exam, ExamPlacement> assignment, ExamPlacement p) {
        if (isHard()) return 0;
        if (p == null) return countViolations(assignment);
        if (countAssignedVariables(assignment) + (assignment.getValue(p.variable()) == null ? 1 : 0) < 2) return 0; // not enough variables
        
        int nrViolatedPairs = 0;
        boolean before = true;
        for (Exam other: variables()) {
            if (other.equals(p.variable())) {
                before = false;
                continue;
            }
            ExamPlacement otherPlacement = assignment.getValue(other);
            if (otherPlacement == null) continue;
            if (before) {
                if (!iType.isSatisfied(otherPlacement, p)) nrViolatedPairs ++;
            } else {
                if (!iType.isSatisfied(p, otherPlacement)) nrViolatedPairs ++;
            }
        }
        return nrViolatedPairs;
    }
    
    /**
     * Return number of all violated pairs for a soft constraint
     * 
     * @param assignment current assignment
     * @return number of examination pairs violated
     */
    public int countViolations(Assignment<Exam, ExamPlacement> assignment) {
        if (isHard()) return 0;
        int violations = 0;
        for (int i = 0; i < variables().size() - 1; i++) {
            ExamPlacement first = assignment.getValue(variables().get(i));
            if (first == null) continue;
            for (int j = i + 1; j < variables().size(); j++) {
                ExamPlacement second = assignment.getValue(variables().get(j));
                if (second == null) continue;
                if (!iType.isSatisfied(first, second)) violations ++;
            }
        }
        return violations;
    }

    /** True if the constraint is related to rooms 
     * @return true if the constraint is related to room placement
     **/
    public boolean isRoomRelated() {
        return iType.isRoomRelated();
    }

    /** True if the constraint is related to periods 
     * @return true if the constraint is related to period placement
     **/
    public boolean isPeriodRelated() {
        return iType.isPeriodRelated();
    }
    
    @Override
    public Context createAssignmentContext(Assignment<Exam, ExamPlacement> assignment) {
        return new Context(assignment);
    }
    
    public class Context implements AssignmentConstraintContext<Exam, ExamPlacement> {
        private int iViolations = 0;
        
        public Context(Assignment<Exam, ExamPlacement> assignment) {
            updateCriterion(assignment);
        }

        @Override
        public void assigned(Assignment<Exam, ExamPlacement> assignment, ExamPlacement placement) {
            updateCriterion(assignment);
        }
        
        @Override
        public void unassigned(Assignment<Exam, ExamPlacement> assignment, ExamPlacement placement) {
            updateCriterion(assignment);
        }
        
        protected void updateCriterion(Assignment<Exam, ExamPlacement> assignment) {
            if (!isHard()) {
                ((DistributionPenalty)getModel().getCriterion(DistributionPenalty.class)).inc(assignment, -iViolations * getWeight(), ExamDistributionConstraint.this);
                iViolations = countViolations(assignment);
                ((DistributionPenalty)getModel().getCriterion(DistributionPenalty.class)).inc(assignment, +iViolations * getWeight(), ExamDistributionConstraint.this);
            }
        }
        
        public int getViolations() { return iViolations; }
    }
}