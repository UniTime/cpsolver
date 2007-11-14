package net.sf.cpsolver.exam.model;

import java.util.Iterator;
import java.util.Set;

import net.sf.cpsolver.ifs.model.BinaryConstraint;
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
 * Copyright (C) 2007 Tomas Muller<br>
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
public class ExamDistributionConstraint extends BinaryConstraint {
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
    /** Distribution type name */
    public static final String[] sDistType = new String[] {
        "same-room",
        "different-room",
        "same-period",
        "different-period",
        "precedence"
    };
    
    private int iType = -1;
    
    /**
     * Constructor
     * @param id constraint unique id
     * @param type constraint type
     */
    public ExamDistributionConstraint(long id, int type) {
        iId = id;
        iType = type;
    }

    /**
     * Constructor
     * @param id constraint unique id
     * @param type constraint type name
     */
    public ExamDistributionConstraint(long id, String type) {
        iId = id;
        for (int i=0;i<sDistType.length;i++)
            if (sDistType[i].equals(type)) iType = i;
        if (iType<0)
            throw new RuntimeException("Unknown type '"+type+"'.");
    }
    
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
        return getTypeString()+" ("+first().getId()+","+second().getId()+")";
    }
    
    /**
     * Compute conflicts -- there is a conflict if the other variable is
     * assigned and {@link ExamDistributionConstraint#check(ExamPlacement, ExamPlacement)} is false
     */
    public void computeConflicts(Value value, Set conflicts) {
        if (inConflict(value))
            conflicts.add(another(value.variable()).getAssignment());
    }
    
    /**
     * Check for conflict -- there is a conflict if the other variable is
     * assigned and {@link ExamDistributionConstraint#check(ExamPlacement, ExamPlacement)} is false
     */
    public boolean inConflict(Value value) {
        ExamPlacement first, second;
        if (value.variable().equals(first())) {
            first = (ExamPlacement)value;
            second = (ExamPlacement)second().getAssignment();
        } else {
            first = (ExamPlacement)first().getAssignment();
            second = (ExamPlacement)value;
        }
        return first!=null && second!=null && !check(first,second); 
    }
    
    /**
     * Consistency check -- {@link ExamDistributionConstraint#check(ExamPlacement, ExamPlacement)} is called
     */
    public boolean isConsistent(Value value1, Value value2) {
        ExamPlacement first, second;
        if (value1.variable().equals(first())) {
            first = (ExamPlacement)value1;
            second = (ExamPlacement)value2;
        } else {
            first = (ExamPlacement)value2;
            second = (ExamPlacement)value1;
        }
        return check(first, second);
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
        case sDistSamePeriod :
            return first.getPeriod().getIndex()==second.getPeriod().getIndex();
        case sDistDifferentPeriod :
            return first.getPeriod().getIndex()!=second.getPeriod().getIndex();
        case sDistSameRoom :
            return first.getRooms().containsAll(second.getRooms()) || second.getRooms().containsAll(first.getRooms());
        case sDistDifferentRoom :
            for (Iterator i=first.getRooms().iterator();i.hasNext();) 
                if (second.getRooms().contains(i.next())) return false;
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
        return getType()==c.getType() && first().equals(c.first()) && second().equals(c.second());
    }
}
