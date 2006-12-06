package net.sf.cpsolver.ifs.example.jobshop;

import java.util.*;

import net.sf.cpsolver.ifs.model.*;

/**
 * Job constraint.
 * <br><br>
 * Each job contians a given set of operations (variables).
 * A job constraint is satisfied, if all operations of the job do not overlap in time and are processed in the given order.
 *
 * @version
 * IFS 1.1 (Iterative Forward Search)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@ktiml.mff.cuni.cz">muller@ktiml.mff.cuni.cz</a><br>
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
public class Job extends Constraint {
    private int iJobNumber = 0;
    private int iDueTime = -1;
    
    /**
     * Constructor
     * @param jobNumber job number
     */
    public Job(int jobNumber) {
        super();
        iJobNumber = jobNumber;
    }
    
    /**
     * Set due time
     */
    public void setDueTime(int dueTime) { iDueTime = dueTime; }
    
    /**
     * Get due time
     */
    public int getDueTime() { return iDueTime; }
    
    /**
     * Get job number
     */
    public int getJobNumner() { return iJobNumber; }
    
    /**
     * Count job operations for the job (i.e., the number of variables in this constraint)
     */
    public int countOperations() { return variables().size(); }
    
    /**
     * Get operation of the given index (0..countOperations()-1)
     */
    public Operation getOperation(int opNumber) { return (Operation)variables().get(opNumber); }
    
    /**
     * Adds conflicting operations into the set of conflicts.
     */
    public void computeConflicts(Value value, Set conflicts) {
        Location location = (Location)value;
        Operation operation = (Operation)value.variable();
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            Operation o = (Operation)e.nextElement();
            if (o.getOperationNumber()==operation.getOperationNumber()) continue;
            Location l = (Location)o.getAssignment();
            if (o.getOperationNumber()<operation.getOperationNumber()) {
                if (!l.before(location)) conflicts.add(l);
            } else {
                if (!l.after(location)) conflicts.add(l);
            }
        }
    }
    
    /**
     * True if there is an operation from the same job which violates with the given assignment. 
     */
    public boolean inConflict(Value value) {
        Location location = (Location)value;
        Operation operation = (Operation)value.variable();
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            Operation o = (Operation)e.nextElement();
            if (o.getOperationNumber()==operation.getOperationNumber()) continue;
            Location l = (Location)o.getAssignment();
            if (o.getOperationNumber()<operation.getOperationNumber()) {
                if (!l.before(location)) return true;
            } else {
                if (!l.after(location)) return true;
            }
        }
        return false;
    }
    
    /**
     * True if the two assignments (placement of opeartions of the same job in time) violates each other.
     */
    public boolean isConsistent(Value value1, Value value2) {
        Location location1 = (Location)value1;
        Operation operation1 = (Operation)value1.variable();
        Location location2 = (Location)value2;
        Operation operation2 = (Operation)value2.variable();
        if (operation1.getOperationNumber()<operation2.getOperationNumber()) {
            if (location1.before(location2)) return true;
        } else {
            if (location2.before(location1)) return true;
        }
        return false;
    }

    /**
     * String representation -- for debuging and printing purposes
     */
    public String toString() { return "J"+iJobNumber; }
    /**
     * Name of the job (e.g. J10 where 10 is the job number)
     */
    public String getName() { return "J"+iJobNumber; }
}