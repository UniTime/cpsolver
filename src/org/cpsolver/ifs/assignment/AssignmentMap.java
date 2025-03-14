package org.cpsolver.ifs.assignment;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.cpsolver.ifs.assignment.context.AssignmentContextHolder;
import org.cpsolver.ifs.assignment.context.AssignmentContextHolderMap;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;


/**
 * An assignment using a {@link HashMap} to store values of all the variables of the model.
 * This class is slower to store and retrieve a value than {@link AssignmentArray}, but 
 * faster in retrieving a collection of assigned variables and values (methods 
 * {@link Assignment#assignedVariables()} and {@link Assignment#assignedValues()}). 
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
public class AssignmentMap<V extends Variable<V, T>, T extends Value<V, T>> extends AssignmentAbstract<V, T> {
    private Map<V, T> iAssignments = new LinkedHashMap<V, T>();
    private Map<V, Long> iIteration = new HashMap<V, Long>();
    
    /** Creates an empty assignment 
     * @param contexts assignment context holder
     **/
    public AssignmentMap(AssignmentContextHolder<V, T> contexts) {
        super(contexts);
    }
    
    /** Creates an empty assignment */
    public AssignmentMap() {
        this(new AssignmentContextHolderMap<V, T>());
    }
    
    /** Creates a copy of an existing assignment
     * @param assignment current assignment
     **/
    public AssignmentMap(Assignment<V, T> assignment) {
        super(new AssignmentContextHolderMap<V, T>());
        for (T value: assignment.assignedValues())
            iAssignments.put(value.variable(), value);
    }

    
    @Override
    public long getIteration(V variable) {
        Long it = iIteration.get(variable);
        return (it == null ? 0 : it);
    }

    @Override
    public Collection<V> assignedVariables() {
        return iAssignments.keySet();
    }
    
    @Override
    public Collection<T> assignedValues() {
        return iAssignments.values();
    }

    @Override
    public int nrAssignedVariables() {
        return iAssignments.size();
    }
    
    @Override
    protected T getValueInternal(V variable) {
        return iAssignments.get(variable);
   }

    @Override
    protected void setValueInternal(long iteration, V variable, T value) {
        if (value == null) {
            iAssignments.remove(variable);
            iIteration.remove(variable);
        } else {
            iAssignments.put(variable, value);
            if (iteration > 0)
                iIteration.put(variable, iteration);
        }
    }
}