package org.cpsolver.ifs.assignment;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.context.InheritedAssignmentContextHolder;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;


/**
 * Optimistic inherited assignment. This assignment does not expect the parent assignment to change
 * much and it only remembers local changes made in this assignment. Use with caution. 
 * 
 * @see InheritedAssignment
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
public class OptimisticInheritedAssignment<V extends Variable<V, T>, T extends Value<V, T>> extends AssignmentAbstract<V, T> implements InheritedAssignment<V, T> {
    private Assignment<V, T> iParent;
    private Map<V, T> iAssignments = new LinkedHashMap<V, T>();
    private Set<V> iDirty = new HashSet<V>();
    private Map<V, Long> iIteration = new HashMap<V, Long>();
    private long iVersion = -1;

    public OptimisticInheritedAssignment(Solution<V, T> parent, int index) {
        // super(new InheritedAssignmentContextHolder<V, T>(index, parent.getIteration()), index, parent);
    	super(new InheritedAssignmentContextHolder<V, T>(index, parent.getIteration()));
        iParent = parent.getAssignment();
        iVersion = parent.getIteration();
    }
    
    @Override
    public long getIteration(V variable) {
        Long it = iIteration.get(variable);
        return (it != null ? it : iDirty.contains(variable) ? 0 : iParent.getIteration(variable));
    }

    @Override
    public Collection<V> assignedVariables() {
        Set<V> variables = new HashSet<V>(iParent.assignedVariables());
        variables.removeAll(iDirty);
        variables.addAll(iAssignments.keySet());
        return variables;
    }
    
    @Override
    public Collection<T> assignedValues() {
        Set<T> values = new HashSet<T>();
        for (T val: iParent.assignedValues()) {
            if (val != null && !iDirty.contains(val.variable()))
                values.add(val);
        }
        values.addAll(iAssignments.values());
        return values;
    }

    @Override
    public int nrAssignedVariables() {
        return iAssignments.size() + iParent.nrAssignedVariables() - iDirty.size();
    }
    
    @Override
    protected T getValueInternal(V variable) {
        T value = (iAssignments.isEmpty() ? null : iAssignments.get(variable));
        return (value != null ? value : iDirty.contains(variable) ? null : iParent.getValue(variable));
   }

    @Override
    protected void setValueInternal(long iteration, V variable, T value) {
        if (value == null) {
            iAssignments.remove(variable);
            iIteration.remove(variable);
            if (iParent.getValue(variable) != null)
                iDirty.add(variable);
        } else {
            iAssignments.put(variable, value);
            if (iteration > 0)
                iIteration.put(variable, iteration);
            iDirty.remove(variable);
        }
    }    
    
    @Override
    public Assignment<V, T> getParentAssignment() {
        return iParent;
    }
    
    @Override
    public long getVersion() {
        return iVersion;
    }
}
