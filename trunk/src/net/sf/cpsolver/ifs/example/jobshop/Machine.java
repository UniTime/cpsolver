package net.sf.cpsolver.ifs.example.jobshop;

import java.util.*;

import net.sf.cpsolver.ifs.model.*;

/**
 * Machine constraint. <br>
 * <br>
 * Each machine contians a given set of operations (variables). A machine
 * constraint is satisfied, if all operations on it do not overlap in time.
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
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
public class Machine extends Constraint<Operation, Location> {
    private int iMachineNumber = -1;

    /**
     * Constructor
     * 
     * @param machineNumber
     *            machine number
     */
    public Machine(int machineNumber) {
        super();
        iMachineNumber = machineNumber;
    }

    /** Get machine number */
    public int getMachineNumber() {
        return iMachineNumber;
    }

    /**
     * Adds conflicting operations into the set of conflicts.
     */
    @Override
    public void computeConflicts(Location location, Set<Location> conflicts) {
        for (Operation o : assignedVariables()) {
            if (o.getOperationNumber() == location.variable().getOperationNumber()
                    && o.getJobNumber() == location.variable().getJobNumber())
                continue;
            if (o.getAssignment().overlap(location))
                conflicts.add(o.getAssignment());
        }
    }

    /**
     * True if there is an operation from the machine which violates with the
     * given assignment.
     */
    @Override
    public boolean inConflict(Location location) {
        for (Operation o : assignedVariables()) {
            if (o.getOperationNumber() == location.variable().getOperationNumber()
                    && o.getJobNumber() == location.variable().getJobNumber())
                continue;
            if (o.getAssignment().overlap(location))
                return true;
        }
        return false;
    }

    /**
     * True if the two assignments (placement of opeartions of the machine in
     * time) violates each other.
     */
    @Override
    public boolean isConsistent(Location value1, Location value2) {
        return !value1.overlap(value2);
    }

    /** string representation -- for debuging and printing purposes */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * Name of the machine (e.g. M10 where 10 is the machine number)
     */
    @Override
    public String getName() {
        return "M" + iMachineNumber;
    }
}
