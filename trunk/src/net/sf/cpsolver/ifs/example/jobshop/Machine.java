package net.sf.cpsolver.ifs.example.jobshop;

import java.util.*;

import net.sf.cpsolver.ifs.model.*;

/**
 * Machine constraint.
 * <br><br>
 * Each machine contians a given set of operations (variables).
 * A machine constraint is satisfied, if all operations on it do not overlap in time.
 *
 * @version
 * IFS 1.1 (Iterative Forward Search)<br>
 * Copyright (C) 2006 Tomas Muller<br>
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
public class Machine extends Constraint {
    private int iMachineNumber = -1;
    
    /**
     * Constructor 
     * @param machineNumber machine number
     */
    public Machine(int machineNumber) {
        super();
        iMachineNumber = machineNumber;
    }
    
    /** Get machine number */
    public int getMachineNumber() { return iMachineNumber; }
    
    
    /**
     * Adds conflicting operations into the set of conflicts.
     */
    public void computeConflicts(Value value, java.util.Set conflicts) {
        Location location = (Location)value;
        Operation operation = (Operation)value.variable();
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            Operation o = (Operation)e.nextElement();
            if (o.getOperationNumber()==operation.getOperationNumber() && o.getJobNumber()==operation.getJobNumber()) continue;
            Location l = (Location)o.getAssignment();
            if (l.overlap(location)) conflicts.add(l);
        }
    }
    
    /**
     * True if there is an operation from the machine which violates with the given assignment.
     */
    public boolean inConflict(Value value) {
        Location location = (Location)value;
        Operation operation = (Operation)value.variable();
        for (Enumeration e=assignedVariables().elements();e.hasMoreElements();) {
            Operation o = (Operation)e.nextElement();
            if (o.getOperationNumber()==operation.getOperationNumber() && o.getJobNumber()==operation.getJobNumber()) continue;
            Location l = (Location)o.getAssignment();
            if (l.overlap(location)) return true;
        }
        return false;
    }
    
    /**
     * True if the two assignments (placement of opeartions of the machine in time) violates each other.
     */
    public boolean isConsistent(Value value1, Value value2) {
        return !((Location)value1).overlap((Location)value2);
    }
    
    /** string representation -- for debuging and printing purposes */
    public String toString() { return getName(); }
    
   /**
     * Name of the machine (e.g. M10 where 10 is the machine number)
     */
     public String getName() { return "M"+iMachineNumber; }
}
