package org.cpsolver.ifs.assignment;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.context.DefaultSingleAssignmentContextHolder;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solver.Solver;


/**
 * An assignment using the old {@link Variable#getAssignment()} to store values of all the
 * variables of the model. Besides of that, a set of assigned variables is kept in memory.
 * It is fast, but there can be only one such assignment at a time.
 * Ideal for single threaded solvers. Also used as a default assignment, see
 * {@link Model#getDefaultAssignment()}. Used by {@link Solver} where there is only one
 * assignment kept in memory.
 * 
 * @see Assignment
 * @see Solver
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
public class DefaultSingleAssignment<V extends Variable<V, T>, T extends Value<V, T>> extends AssignmentAbstract<V, T> {
    private Set<V> iAssignedVariables = new HashSet<V>();

    public DefaultSingleAssignment() {
        super(new DefaultSingleAssignmentContextHolder<V,T>());
    }

    @Override
    @SuppressWarnings("deprecation")
    public long getIteration(V variable) {
        return variable.getLastIteration();
    }

    @Override
    public Collection<V> assignedVariables() {
        return iAssignedVariables;
    }

    @Override
    @SuppressWarnings("deprecation")
    protected T getValueInternal(V variable) {
        return variable.getAssignment();
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void setValueInternal(long iteration, V variable, T value) {
        variable.setAssignment(value);
        variable.setLastIteration(iteration);
        if (value == null)
            iAssignedVariables.remove(variable);
        else
            iAssignedVariables.add(variable);
    }
    
    @Override
    public int getIndex() {
        return 0;
    }
}
