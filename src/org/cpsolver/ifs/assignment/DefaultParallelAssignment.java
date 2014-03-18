package org.cpsolver.ifs.assignment;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.cpsolver.ifs.assignment.context.DefaultParallelAssignmentContextHolder;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solver.ParallelSolver;


/**
 * An assignment using the old {@link Variable#getExtra()} to store values of all the
 * variables of the model. Besides of that, a set of assigned variables is kept in memory.
 * Each extra contains an array of values, indexed by {@link Assignment#getIndex()}.
 * Useful for a small, fixed number of assignments. Used by the {@link ParallelSolver},
 * where there is one assignment for each thread. 
 * 
 * @see Assignment
 * @see ParallelSolver
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 **/
public class DefaultParallelAssignment <V extends Variable<V, T>, T extends Value<V, T>> extends AssignmentAbstract<V, T> {
    private Map<V, Long> iAssignedVariables = new HashMap<V, Long>();
    private int iIndex;

    public DefaultParallelAssignment(int threadIndex) {
        super(new DefaultParallelAssignmentContextHolder<V, T>(threadIndex));
        iIndex = threadIndex;
    }
    
    public DefaultParallelAssignment() {
        this(0);
    }
    
    public DefaultParallelAssignment(int threadIndex, Model<V, T> model, Assignment<V, T> assignment) {
        this(threadIndex);
        for (V variable: model.variables())
            setValueInternal(0, variable, assignment != null ? assignment.getValue(variable) : null);
    }

    @Override
    public long getIteration(V variable) {
        Long it = iAssignedVariables.get(variable);
        return (it == null ? 0 : it.longValue());
    }

    @Override
    public Collection<V> assignedVariables() {
        return iAssignedVariables.keySet();
    }
    
    @SuppressWarnings("unchecked")
    protected T[] getAssignments(V variable) {
        synchronized (variable) {
            T[] assignments = (T[])variable.getExtra();
            if (assignments == null) {
                assignments = (T[])new Value[Math.max(10, 1 + iIndex)];
                variable.setExtra(assignments);
            } else if (assignments.length <= iIndex) {
                assignments = Arrays.copyOf(assignments, 10 + iIndex);
                variable.setExtra(assignments);
            }
            return assignments;
        }
    }

    @Override
    protected T getValueInternal(V variable) {
        return getAssignments(variable)[iIndex];
    }
    
    @Override
    protected void setValueInternal(long iteration, V variable, T value) {
        getAssignments(variable)[iIndex] = value;
        if (value == null)
            iAssignedVariables.remove(variable);
        else
            iAssignedVariables.put(variable, iteration);
    }

    @Override
    public int getIndex() {
        return iIndex;
    }
}
