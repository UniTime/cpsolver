package org.cpsolver.ifs.assignment;

import org.cpsolver.ifs.assignment.context.InheritedAssignmentContextHolder;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;

/**
 * Default inherited assignment. This assignment is based on {@link DefaultParallelAssignment} and creates
 * a full copy of the given assignment during its creation. This inherited assignment is slower than
 * {@link OptimisticInheritedAssignment}, but more reliable. Useful for small problems with a lot of constraints.
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
public class DefaultInheritedAssignment<V extends Variable<V, T>, T extends Value<V, T>> extends DefaultParallelAssignment<V, T> implements InheritedAssignment<V, T> {
    private Assignment<V, T> iParent;
    private long iVersion = -1;

    public DefaultInheritedAssignment(Solution<V, T> parent, int index) {
        super(new InheritedAssignmentContextHolder<V, T>(index, parent.getIteration()), index, parent);
        iParent = parent.getAssignment();
        iVersion = parent.getIteration();
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
