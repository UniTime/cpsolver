package net.sf.cpsolver.ifs.assignment.context;

import net.sf.cpsolver.ifs.assignment.Assignment;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;

/**
 * A variable with an assignment context. In order to be able to hold multiple assignments in memory
 * it is desired for all the assignment dependent data a variable may need (to effectively enumerate
 * its objectives), to store these data in a separate class (implementing the 
 * {@link AssignmentContext} interface). This context is created by calling
 * {@link ConstraintWithContext#createAssignmentContext(Assignment)} and accessed by
 * {@link ConstraintWithContext#getContext(Assignment)}.
 * 
 * 
 * @see AssignmentContext
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
public abstract class VariableWithContext<V extends Variable<V, T>, T extends Value<V, T>, C extends AssignmentContext> extends Variable<V, T> implements HasAssignmentContext<V, T, C>, CanHoldContext {
    private AssignmentContextReference<V, T, C> iContextReference = null;
    private AssignmentContext[] iContext = null;
    
    /** Constructor */
    public VariableWithContext() {
        super();
    }

    /**
     * Constructor
     * @param initialValue initial value (minimal-perturbation problem)
     */
    public VariableWithContext(T initialValue) {
        super(initialValue);
    }
    
    @Override
    public void setModel(Model<V, T> model) {
        super.setModel(model);
        iContextReference = model.createReference(this);
    }
    
    /**
     * Returns an assignment context associated with this extension. If there is no 
     * assignment context associated with this extension yet, one is created using the
     * {@link ConstraintWithContext#createAssignmentContext(Assignment)} method. From that time on,
     * this context is kept with the assignment.
     * @param assignment given assignment
     * @return assignment context associated with this extension and the given assignment
     */
    @SuppressWarnings("unchecked")
    public C getContext(Assignment<V, T> assignment) {
        if (iContext != null && assignment.getIndex() >= 0 && assignment.getIndex() < iContext.length) {
            AssignmentContext c = iContext[assignment.getIndex()];
            if (c != null) return (C)c;
        }
        return assignment.getAssignmentContext(getAssignmentContextReference());
    }

    @Override
    public AssignmentContextReference<V, T, C> getAssignmentContextReference() { return iContextReference; }

    @Override
    public void setAssignmentContextReference(AssignmentContextReference<V, T, C> reference) { iContextReference = reference; }

    @Override
    public AssignmentContext[] getContext() { return iContext; }

    @Override
    public void setContext(AssignmentContext[] context) { iContext = context; }
}
