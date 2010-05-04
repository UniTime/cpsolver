package net.sf.cpsolver.exam.model;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Value;

/**
 * Distribution binary constraint. 
 * <br><br>
 * The following binary distribution constraints are implemented
 * <ul>
 *  <li> Same room
 *  <li> Different room
 *  <li> Same period
 *  <li> Different period
 *  <li> Precedence
 * </ul>
 * <br><br>
 * 
 * @version
 * ExamTT 1.1 (Examination Timetabling)<br>
 * Copyright (C) 2008 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
public class ExamDistributionConstraint extends Constraint {
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
    /** Distribution type name */
    public static final String[] sDistType = new String[] {
        "same-room",
        "different-room",
        "same-period",
        "different-period",
        "precedence",
        "precedence-rev"
    };
    
    private int iType = -1;
    private boolean iHard = true;
    private int iWeight = 0;
    
    /**
     * Constructor
     * @param id constraint unique id
     * @param type constraint type
     * @param hard true if the constraint is hard (cannot be violated)
     * @param weight if not hard, penalty for violation
     */
    public ExamDistributionConstraint(long id, int type, boolean hard, int weight) {
        iId = id;
        iType = type;
        iHard = hard;
        iWeight = weight;
    }

    /**
     * Constructor
     * @param id constraint unique id
     * @param type constraint type (EX_SAME_PREF, EX_SAME_ROOM, or EX_PRECEDENCE)
     * @param pref preference (R/P for required/prohibited, or -2, -1, 0, 1, 2 for preference (from preferred to discouraged)) 
     */
    public ExamDistributionConstraint(long id, String type, String pref) {
        iId = id;
        boolean neg = "P".equals(pref) || "2".equals(pref) || "1".equals(pref);
        if ("EX_SAME_PER".equals(type)) {
            iType = (neg?sDistDifferentPeriod:sDistSamePeriod);
        } else if ("EX_SAME_ROOM".equals(type)) {
            iType = (neg?sDistDifferentRoom:sDistSameRoom);
        } else if ("EX_PRECEDENCE".equals(type)) {
            iType = (neg?sDistPrecedenceRev:sDistPrecedence);
        } else
            throw new RuntimeException("Unkown type "+type);
        if ("P".equals(pref) || "R".equals(pref))
            iHard = true;
        else {
            iHard = false;
            iWeight = Integer.parseInt(pref) * Integer.parseInt(pref);
        }
    }

    /**
     * Constructor
     * @param id constraint unique id
     * @param type constraint type name
     */
    public ExamDistributionConstraint(long id, String type, boolean hard, int weight) {
        iId = id;
        for (int i=0;i<sDistType.length;i++)
            if (sDistType[i].equals(type)) iType = i;
        if (iType<0)
            throw new RuntimeException("Unknown type '"+type+"'.");
        iHard = hard;
        iWeight = weight;
    }
    
    /**
     * True if hard (must be satisfied), false for soft (should be satisfied)
     */
    public boolean isHard() { return iHard; }
    
    /**
     * If not hard, penalty for violation
     */
    public int getWeight() { return iWeight; }
    
    /**
     * Constraint type
     */
    public int getType() {
        return iType;
    }

    /**
     * Constraint type name
     */
    public String getTypeString() {
        return sDistType[iType];
    }

    /**
     * String representation -- constraint type name (exam 1, exam 2)
     */
    public String toString() {
        return getTypeString()+" ("+variables()+")";
    }
    
    /**
     * Compute conflicts -- there is a conflict if the other variable is
     * assigned and {@link ExamDistributionConstraint#check(ExamPlacement, ExamPlacement)} is false
     */
    public void computeConflicts(Value value, Set conflicts) {
        boolean before = true;
        ExamPlacement givenPlacement = (ExamPlacement)value;
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            Exam exam = (Exam)e.nextElement();
            if (exam.equals(value.variable())) {
                before = false; continue;
            }
            ExamPlacement placement = (ExamPlacement)exam.getAssignment();
            if (placement==null) continue;
            if (!check(before?placement:givenPlacement, before?givenPlacement:placement))
                conflicts.add(placement);
        }
    }
    
    /**
     * Check for conflict -- there is a conflict if the other variable is
     * assigned and {@link ExamDistributionConstraint#check(ExamPlacement, ExamPlacement)} is false
     */
    public boolean inConflict(Value value) {
        boolean before = true;
        ExamPlacement givenPlacement = (ExamPlacement)value;
        for (Enumeration e=variables().elements();e.hasMoreElements();) {
            Exam exam = (Exam)e.nextElement();
            if (exam.equals(value.variable())) {
                before = false; continue;
            }
            ExamPlacement placement = (ExamPlacement)exam.getAssignment();
            if (placement==null) continue;
            if (!check(before?placement:givenPlacement, before?givenPlacement:placement)) return true;
        }
        return false;
    }
    
    /**
     * Consistency check -- {@link ExamDistributionConstraint#check(ExamPlacement, ExamPlacement)} is called
     */
    public boolean isConsistent(Value value1, Value value2) {
        ExamPlacement first = (ExamPlacement)value1;
        ExamPlacement second = (ExamPlacement)value2;
        boolean before = (variables().indexOf(value1.variable())<variables().indexOf(value2.variable()));
        return check(before?first:second, before?second:first);
    }
    
    /**
     * Check assignments of the given exams 
     * @param first assignment of the first exam 
     * @param second assignment of the  second exam
     * @return true, if the constraint is satisfied
     */
    public boolean check(ExamPlacement first, ExamPlacement second) {
        switch (getType()) {
        case sDistPrecedence :
            return first.getPeriod().getIndex()<second.getPeriod().getIndex();
        case sDistPrecedenceRev :
            return first.getPeriod().getIndex()>second.getPeriod().getIndex();
        case sDistSamePeriod :
            return first.getPeriod().getIndex()==second.getPeriod().getIndex();
        case sDistDifferentPeriod :
            return first.getPeriod().getIndex()!=second.getPeriod().getIndex();
        case sDistSameRoom :
            return first.getRoomPlacements().containsAll(second.getRoomPlacements()) || second.getRoomPlacements().containsAll(first.getRoomPlacements());
        case sDistDifferentRoom :
            for (Iterator i=first.getRoomPlacements().iterator();i.hasNext();) 
                if (second.getRoomPlacements().contains(i.next())) return false;
            return true;
        default :
            return false;
        }
    }
    
    /**
     * Compare with other constraint for equality
     */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof ExamDistributionConstraint)) return false;
        ExamDistributionConstraint c = (ExamDistributionConstraint)o;
        return getType()==c.getType() && getId()==c.getId();
    }
    
    /**
     * Return true if this is hard constraint or this is a soft constraint without any violation
     */
    public boolean isSatisfied() {
        return isSatisfied(null);
    }
    
    /**
     * Return true if this is hard constraint or this is a soft constraint without any violation
     * @param p exam assignment to be made
     */
    public boolean isSatisfied(ExamPlacement p) {
        if (isHard()) return true;
        switch (getType()) {
        case sDistPrecedence :
            ExamPeriod last = null;
            for (Enumeration e=variables().elements();e.hasMoreElements();) {
                Exam exam = (Exam)e.nextElement();
                ExamPlacement placement = (p!=null && exam.equals(p.variable())?p:(ExamPlacement)exam.getAssignment());
                if (placement==null) continue;
                if (last==null || last.getIndex()<placement.getPeriod().getIndex())
                    last = placement.getPeriod();
                else
                    return false;
            }
            return true;
        case sDistPrecedenceRev :
            last = null;
            for (Enumeration e=variables().elements();e.hasMoreElements();) {
                Exam exam = (Exam)e.nextElement();
                ExamPlacement placement = (p!=null && exam.equals(p.variable())?p:(ExamPlacement)exam.getAssignment());
                if (placement==null) continue;
                if (last==null || last.getIndex()>placement.getPeriod().getIndex())
                    last = placement.getPeriod();
                else
                    return false;
            }
            return true;
        case sDistSamePeriod :
            ExamPeriod period = null;
            for (Enumeration e=variables().elements();e.hasMoreElements();) {
                Exam exam = (Exam)e.nextElement();
                ExamPlacement placement = (p!=null && exam.equals(p.variable())?p:(ExamPlacement)exam.getAssignment());
                if (placement==null) continue;
                if (period==null)
                    period = placement.getPeriod();
                else if (period.getIndex()!=placement.getPeriod().getIndex())
                    return false;
            }
            return true;
        case sDistDifferentPeriod :
            HashSet periods = new HashSet();
            for (Enumeration e=variables().elements();e.hasMoreElements();) {
                Exam exam = (Exam)e.nextElement();
                ExamPlacement placement = (p!=null && exam.equals(p.variable())?p:(ExamPlacement)exam.getAssignment());
                if (placement==null) continue;
                if (!periods.add(placement.getPeriod())) return false;
            }
            return true;
        case sDistSameRoom :
            Set rooms = null;
            for (Enumeration e=variables().elements();e.hasMoreElements();) {
                Exam exam = (Exam)e.nextElement();
                ExamPlacement placement = (p!=null && exam.equals(p.variable())?p:(ExamPlacement)exam.getAssignment());
                if (placement==null) continue;
                if (rooms==null)
                    rooms = placement.getRoomPlacements();
                else if (!rooms.containsAll(placement.getRoomPlacements()) || !placement.getRoomPlacements().containsAll(rooms))
                    return false;
            }
            return true;
        case sDistDifferentRoom :
            HashSet allRooms = new HashSet();
            for (Enumeration e=variables().elements();e.hasMoreElements();) {
                Exam exam = (Exam)e.nextElement();
                ExamPlacement placement = (p!=null && exam.equals(p.variable())?p:(ExamPlacement)exam.getAssignment());
                if (placement==null) continue;
                for (Iterator i=placement.getRoomPlacements().iterator();i.hasNext();) {
                    ExamRoomPlacement room = (ExamRoomPlacement)i.next();
                    if (!allRooms.add(room)) return false;
                }
            }
            return true;
        default :
            return false;
        }
    }
    
    
    boolean iIsSatisfied = true;
    public void assigned(long iteration, Value value) {
        super.assigned(iteration, value);
        if (!isHard() && iIsSatisfied!=isSatisfied()) {
            iIsSatisfied = !iIsSatisfied;
            ((ExamModel)value.variable().getModel()).addDistributionPenalty(iIsSatisfied?-getWeight():getWeight());
        }
    }
    
    public void unassigned(long iteration, Value value) {
        super.unassigned(iteration, value);
        if (!isHard() && iIsSatisfied!=isSatisfied()) {
            iIsSatisfied = !iIsSatisfied;
            ((ExamModel)value.variable().getModel()).addDistributionPenalty(iIsSatisfied?-getWeight():getWeight());
        }
    }
    
    /** True if the constraint is related to rooms */
    public boolean isRoomRelated() {
        return iType==sDistSameRoom || iType==sDistDifferentRoom;
    }

    /** True if the constraint is related to periods */
    public boolean isPeriodRelated() {
        return !isRoomRelated();
    }
}
