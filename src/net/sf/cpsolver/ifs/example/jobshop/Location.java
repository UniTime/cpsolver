package net.sf.cpsolver.ifs.example.jobshop;

import net.sf.cpsolver.ifs.model.*;

/**
 * Location of an operation.
 * <br><br>
 * Each location has its start time.
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
public class Location extends Value {
    private int iStartTime = -1;
    
    /**
     * Constructor
     * @param op parent operation
     * @param startTime start time
     */
    public Location(Operation op, int startTime) {
        super(op);
        iStartTime = startTime;
    }
    
    /**
     * Get start time of the location
     */
    public int getStartTime() { return iStartTime; }
    
    /**
     * Get finishing time of the location (start time + operation processing time)
     */
    public int getFinishingTime() { return iStartTime+((Operation)variable()).getProcessingTime()-1; }
    
    /**
     * Start time of the location
     */
    public double toDouble() { return iStartTime; }
    
    /**
     * String representation (operation name = start time)
     */
    public String toString() { return variable().getName()+"="+iStartTime; }
    
    /**
     * Name -- start time
     */
    public String getName() { return String.valueOf(iStartTime); }
    
    /**
     * Returns true if overlap with the given location
     */
    public boolean overlap(Location anotherLocation) {
        if (getStartTime()+((Operation)variable()).getProcessingTime()<=anotherLocation.getStartTime()) return false;
        if (anotherLocation.getStartTime()+((Operation)anotherLocation.variable()).getProcessingTime()<=getStartTime()) return false;
        return true;
    }
    
    /**
     * Returnts true if before the given location
     */
    public boolean before(Location anotherLocation) {
        if (getStartTime()+((Operation)variable()).getProcessingTime()<=anotherLocation.getStartTime()) return true;
        return false;
    }

    /**
     * Returnts true if after the given location
     */
    public boolean after(Location anotherLocation) {
        return anotherLocation.before(this);
    }
}
