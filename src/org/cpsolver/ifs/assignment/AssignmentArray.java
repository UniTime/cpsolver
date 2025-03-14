package org.cpsolver.ifs.assignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.cpsolver.ifs.assignment.context.AssignmentContextHolder;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;


/**
 * An assignment using array to store values of all the variables of the model.
 * {@link Variable#getIndex()} is used to index the array.
 * 
 * @see Assignment
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Iterative Forward Search)<br>
 *          Copyright (C) 2014 Tomas Muller<br>
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
 *          License along with this library; if not see <a href='http://www.gnu.org/licenses'>http://www.gnu.org/licenses</a>.
 * @param <V> Variable
 * @param <T> Value
 **/
public class AssignmentArray<V extends Variable<V, T>, T extends Value<V, T>> extends AssignmentAbstract<V, T> {
    protected Object[] iAssignments = new Object[1000];
    protected Long[] iIteration = new Long[1000];
    
    /** Creates an empty assignment 
     * @param contexts assignment context holder
     **/
    public AssignmentArray(AssignmentContextHolder<V, T> contexts) {
        super(contexts);
    }
    
    @Override
    public long getIteration(V variable) {
        try {
            Long it = iIteration[variable.getIndex()];
            return (it == null ? 0 : it);
        } catch (ArrayIndexOutOfBoundsException e) {
            return 0;
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected T getValueInternal(V variable) {
        try {
            return (T) iAssignments[variable.getIndex()];
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    @Override
    protected void setValueInternal(long iteration, V variable, T value) {
        if (iAssignments.length <= variable.getIndex()) {
            iAssignments = Arrays.copyOf(iAssignments, variable.getIndex() + 1000);
            iIteration = Arrays.copyOf(iIteration, variable.getIndex() + 1000);
        }
        if (value == null) {
            iAssignments[variable.getIndex()] = null;
            iIteration[variable.getIndex()] = null;
        } else {
            iAssignments[variable.getIndex()] = value;
            if (iteration > 0)
                iIteration[variable.getIndex()] = iteration;
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Collection<V> assignedVariables() {
        List<V> variables = new ArrayList<V>(iAssignments.length);
        for (Object value: iAssignments)
            if (value != null) variables.add(((T)value).variable());
        return variables;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Collection<T> assignedValues() {
        List<T> values = new ArrayList<T>(iAssignments.length);
        for (Object value: iAssignments)
            if (value != null) values.add((T)value);
        return values;
    }

    @Override
    public int nrAssignedVariables() {
        int ret = 0;
        for (Object value: iAssignments)
            if (value != null) ret ++;
        return ret;
    }
}
