package net.sf.cpsolver.ifs.example.jobshop;

import net.sf.cpsolver.ifs.model.Value;

/**
 * Location of an operation. <br>
 * <br>
 * Each location has its start time.
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
public class Location extends Value<Operation, Location> {
    private int iStartTime = -1;

    /**
     * Constructor
     * 
     * @param op
     *            parent operation
     * @param startTime
     *            start time
     */
    public Location(Operation op, int startTime) {
        super(op);
        iStartTime = startTime;
    }

    /**
     * Get start time of the location
     */
    public int getStartTime() {
        return iStartTime;
    }

    /**
     * Get finishing time of the location (start time + operation processing
     * time)
     */
    public int getFinishingTime() {
        return iStartTime + (variable()).getProcessingTime() - 1;
    }

    /**
     * Start time of the location
     */
    @Override
    public double toDouble() {
        return iStartTime;
    }

    /**
     * String representation (operation name = start time)
     */
    @Override
    public String toString() {
        return variable().getName() + "=" + iStartTime;
    }

    /**
     * Name -- start time
     */
    @Override
    public String getName() {
        return String.valueOf(iStartTime);
    }

    /**
     * Returns true if overlap with the given location
     */
    public boolean overlap(Location anotherLocation) {
        if (getStartTime() + variable().getProcessingTime() <= anotherLocation.getStartTime())
            return false;
        if (anotherLocation.getStartTime() + anotherLocation.variable().getProcessingTime() <= getStartTime())
            return false;
        return true;
    }

    /**
     * Returnts true if before the given location
     */
    public boolean before(Location anotherLocation) {
        if (getStartTime() + variable().getProcessingTime() <= anotherLocation.getStartTime())
            return true;
        return false;
    }

    /**
     * Returnts true if after the given location
     */
    public boolean after(Location anotherLocation) {
        return anotherLocation.before(this);
    }
}
