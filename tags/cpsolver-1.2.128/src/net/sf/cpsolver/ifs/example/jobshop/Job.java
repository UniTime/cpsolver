package net.sf.cpsolver.ifs.example.jobshop;

import java.util.Set;

import net.sf.cpsolver.ifs.model.Constraint;

/**
 * Job constraint. <br>
 * <br>
 * Each job contians a given set of operations (variables). A job constraint is
 * satisfied, if all operations of the job do not overlap in time and are
 * processed in the given order.
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
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
public class Job extends Constraint<Operation, Location> {
    private int iJobNumber = 0;
    private int iDueTime = -1;

    /**
     * Constructor
     * 
     * @param jobNumber
     *            job number
     */
    public Job(int jobNumber) {
        super();
        iJobNumber = jobNumber;
    }

    /**
     * Set due time
     */
    public void setDueTime(int dueTime) {
        iDueTime = dueTime;
    }

    /**
     * Get due time
     */
    public int getDueTime() {
        return iDueTime;
    }

    /**
     * Get job number
     */
    public int getJobNumner() {
        return iJobNumber;
    }

    /**
     * Count job operations for the job (i.e., the number of variables in this
     * constraint)
     */
    public int countOperations() {
        return variables().size();
    }

    /**
     * Get operation of the given index (0..countOperations()-1)
     */
    public Operation getOperation(int opNumber) {
        return variables().get(opNumber);
    }

    /**
     * Adds conflicting operations into the set of conflicts.
     */
    @Override
    public void computeConflicts(Location location, Set<Location> conflicts) {
        for (Operation o : assignedVariables()) {
            if (o.getOperationNumber() == location.variable().getOperationNumber())
                continue;
            Location l = o.getAssignment();
            if (o.getOperationNumber() < location.variable().getOperationNumber()) {
                if (!l.before(location))
                    conflicts.add(l);
            } else {
                if (!l.after(location))
                    conflicts.add(l);
            }
        }
    }

    /**
     * True if there is an operation from the same job which violates with the
     * given assignment.
     */
    @Override
    public boolean inConflict(Location location) {
        for (Operation o : assignedVariables()) {
            if (o.getOperationNumber() == location.variable().getOperationNumber())
                continue;
            Location l = o.getAssignment();
            if (o.getOperationNumber() < location.variable().getOperationNumber()) {
                if (!l.before(location))
                    return true;
            } else {
                if (!l.after(location))
                    return true;
            }
        }
        return false;
    }

    /**
     * True if the two assignments (placement of opeartions of the same job in
     * time) violates each other.
     */
    @Override
    public boolean isConsistent(Location location1, Location location2) {
        Operation operation1 = location1.variable();
        Operation operation2 = location2.variable();
        if (operation1.getOperationNumber() < operation2.getOperationNumber()) {
            if (location1.before(location2))
                return true;
        } else {
            if (location2.before(location1))
                return true;
        }
        return false;
    }

    /**
     * String representation -- for debuging and printing purposes
     */
    @Override
    public String toString() {
        return "J" + iJobNumber;
    }

    /**
     * Name of the job (e.g. J10 where 10 is the job number)
     */
    @Override
    public String getName() {
        return "J" + iJobNumber;
    }
}