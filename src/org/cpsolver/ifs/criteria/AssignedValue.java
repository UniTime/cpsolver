package org.cpsolver.ifs.criteria;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Simple Criterion: Sum of {@link Value#toDouble(Assignment)}. <br>
 * <br>
 * This criterion only counts a sum of values (see {@link Value#toDouble(Assignment)}) of the assigned variables.
 * It is an alternative to the default {@link Model#getTotalValue(Assignment)}.
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
 * @param <V> Variable
 * @param <T> Value
 */
public class AssignedValue<V extends Variable<V, T>, T extends Value<V, T>> extends AbstractCriterion<V, T>{
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 1.0;
    }

    @Override
    public double getValue(Assignment<V, T> assignment, T value, Set<T> conflicts) {
        double ret = value.toDouble(assignment);
        if (conflicts != null)
            for (T conflict: conflicts)
                ret -= conflict.toDouble(assignment);
        return ret;
    }

}
