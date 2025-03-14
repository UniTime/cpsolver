package org.cpsolver.ifs.example.jobshop;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;


/**
 * Machine constraint. <br>
 * <br>
 * Each machine contians a given set of operations (variables). A machine
 * constraint is satisfied, if all operations on it do not overlap in time.
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Iterative Forward Search)<br>
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

    /** Get machine number 
     * @return machine number
     **/
    public int getMachineNumber() {
        return iMachineNumber;
    }

    /**
     * Adds conflicting operations into the set of conflicts.
     */
    @Override
    public void computeConflicts(Assignment<Operation, Location> assignment, Location location, Set<Location> conflicts) {
        for (Operation o : assignedVariables(assignment)) {
            if (o.getOperationNumber() == location.variable().getOperationNumber() && o.getJobNumber() == location.variable().getJobNumber())
                continue;
            Location conf = assignment.getValue(o);
            if (conf.overlap(location))
                conflicts.add(conf);
        }
    }

    /**
     * True if there is an operation from the machine which violates with the
     * given assignment.
     */
    @Override
    public boolean inConflict(Assignment<Operation, Location> assignment, Location location) {
        for (Operation o : assignedVariables(assignment)) {
            if (o.getOperationNumber() == location.variable().getOperationNumber()
                    && o.getJobNumber() == location.variable().getJobNumber())
                continue;
            if (assignment.getValue(o).overlap(location))
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
