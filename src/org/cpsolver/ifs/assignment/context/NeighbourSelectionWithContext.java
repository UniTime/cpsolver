package org.cpsolver.ifs.assignment.context;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solver.Solver;

/**
 * A neighborhood selection with an assignment context. In order to be able to hold multiple assignments in memory
 * it is desired for all the assignment dependent data a selection may need (to keep its current state),
 * to store these data in a separate class (implementing the 
 * {@link AssignmentContext} interface). This context is created by calling
 * {@link ConstraintWithContext#createAssignmentContext(Assignment)} and accessed by
 * {@link ConstraintWithContext#getContext(Assignment)}.
 * 
 * 
 * @see AssignmentContext
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
 * @param <C> Assignment Context
 **/
public abstract class NeighbourSelectionWithContext<V extends Variable<V, T>, T extends Value<V, T>, C extends AssignmentContext> implements NeighbourSelection<V, T>, HasAssignmentContext<V, T, C>, CanHoldContext {
    private AssignmentContextReference<V, T, C> iContextReference = null;
    private AssignmentContext[] iContext = new AssignmentContext[CanHoldContext.sMaxSize];
    protected C iContextOverride = null;

    @Override
    public void init(Solver<V, T> solver) {
        iContextReference = solver.currentSolution().getModel().createReference(this);
        if (isSingleContextSolver(solver))
            iContextOverride = createAssignmentContext(solver.currentSolution().getAssignment());
    }
    
    /**
     * Returns true if there should be only one context for this neighbourhood selection.
     * @param solver current solver
     * @return {@link Solver#hasSingleSolution()}
     */
    protected boolean isSingleContextSolver(Solver<V, T> solver) {
        return solver.hasSingleSolution();
    }
    
    /**
     * Returns an assignment context associated with this selection. If there is no 
     * assignment context associated with this selection yet, one is created using the
     * {@link ConstraintWithContext#createAssignmentContext(Assignment)} method. From that time on,
     * this context is kept with the assignment.
     * @param assignment given assignment
     * @return assignment context associated with this selection and the given assignment
     */
    @Override
    public C getContext(Assignment<V, T> assignment) {
        if (iContextOverride != null)
            return iContextOverride;
        return AssignmentContextHelper.getContext(this, assignment);
    }
    
    @Override
    public AssignmentContextReference<V, T, C> getAssignmentContextReference() { return iContextReference; }

    @Override
    public void setAssignmentContextReference(AssignmentContextReference<V, T, C> reference) { iContextReference = reference; }

    @Override
    public AssignmentContext[] getContext() { return iContext; }
    
    /**
     * Has context override
     * @return true if all threads are using the same context
     */
    public boolean hasContextOverride() {
        return iContextOverride != null;
    }
}
