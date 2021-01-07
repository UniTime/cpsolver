package org.cpsolver.ifs.model;

import java.util.HashMap;
import java.util.Map;

import org.cpsolver.ifs.assignment.Assignment;


/**
 * Lazy swap of two variables. See {@link LazyNeighbour}.
 *  
 * @version IFS 1.3 (Iterative Forward Search)<br>
 * Copyright (C) 2013 - 2014 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * <a href="http://muller.unitime.org">http://muller.unitime.org</a><br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not see
 * <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 * 
 * @param <V> Variable 
 * @param <T> Value
 */
public class LazySwap<V extends Variable<V, T>, T extends Value<V, T>> extends LazyNeighbour<V, T> {
    private T iV1, iV2, iOldV1 = null, iOldV2 = null; 
    
    /**
     * Constructor
     * @param v1 first variable
     * @param v2 second variable
     */
    public LazySwap(T v1, T v2) {
        iV1 = v1;
        iV2 = v2;
    }
    
    /** Perform swap */
    @Override
    protected void doAssign(Assignment<V, T> assignment, long iteration) {
        iOldV1 = assignment.getValue(iV1.variable());
        iOldV2 = assignment.getValue(iV2.variable());
        if (iOldV1 != null) assignment.unassign(iteration, iV1.variable());
        if (iOldV2 != null) assignment.unassign(iteration, iV2.variable());
        assignment.assign(iteration, iV1);
        assignment.assign(iteration, iV2);
    }
    
    /** Undo the swap */
    @Override
    protected void undoAssign(Assignment<V, T> assignment, long iteration) {
        assignment.unassign(iteration, iV1.variable());
        assignment.unassign(iteration, iV2.variable());
        if (iOldV1 != null) assignment.assign(iteration, iOldV1);
        if (iOldV2 != null) assignment.assign(iteration, iOldV2);
    }
    /** Return problem model */
    @Override
    public Model<V,T> getModel() {
        return iV1.variable().getModel();
    }
    
    /** String representation */
    @Override
    public String toString() {
        return "Lazy "+iOldV1+" -> "+iV1+", "+iOldV2+" -> "+iV2;
    }

    @Override
    public Map<V, T> assignments() {
        Map<V, T> ret = new HashMap<V, T>();
        ret.put(iV1.variable(), iV1);
        ret.put(iV2.variable(), iV2);
        return ret;
    }

}