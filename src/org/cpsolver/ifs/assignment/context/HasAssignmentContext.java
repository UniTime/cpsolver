package org.cpsolver.ifs.assignment.context;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.AbstractCriterion;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;

/**
 * An interface to be implemented by a class that need an assignment context associated with it.
 * I.e., for each existing assignment, there can be an instance of {@link AssignmentContext} available
 * to the class.<br><br>
 * 
 * The idea is that each class that needs to keep some assignment dependent data will implements
 * {@link HasAssignmentContext} interface and the data will be wrapped by this class.
 * The {@link HasAssignmentContext} will only contain a reference to this
 * assignment context, created by calling {@link Model#createReference(HasAssignmentContext)}
 * during its initialization. The assignment context can be than accessed by calling
 * {@link Assignment#getAssignmentContext(AssignmentContextReference)}.<br><br>
 * 
 * These assignment contexts are being held in memory by a class implementing the
 * {@link AssignmentContextHolder} interface. For constraints, criteria, extensions, and 
 * neighborhood selections an existing class implementing the context can be used, see
 * {@link ConstraintWithContext}, {@link AbstractCriterion}, {@link ExtensionWithContext},
 * and {@link NeighbourSelectionWithContext} respectively.<br><br>
 * 
 * For instance, when implementing {@link ConstraintWithContext}, only the method
 * {@link ConstraintWithContext#createAssignmentContext(Assignment)} needs to be implemented and the 
 * assignment context can be accessed within the constraint using the method
 * {@link ConstraintWithContext#getContext(Assignment)}.
 * 
 * @see AssignmentContextHolder
 * @see HasAssignmentContext
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
public interface HasAssignmentContext <V extends Variable<V, T>, T extends Value<V, T>, C extends AssignmentContext> {

    /**
     * Create a new assignment context for the given assignment. If there are already some variables assigned in
     * the value, the method should make sure that the context is appropriately initialized.
     * @param assignment an assignment for which there needs to be an assignment context
     * @return a new instance of the assignment context, filled according to the given assignment
     */
    public C createAssignmentContext(Assignment<V,T> assignment);
    
    /**
     * Returns an assignment context reference 
     * @return reference provided by the model by calling {@link Model#createReference(HasAssignmentContext)} during initialization
     */
    public AssignmentContextReference<V, T, C> getAssignmentContextReference();
    
    /**
     * Store an assignment context reference that was given for the class by the {@link Model#createReference(HasAssignmentContext)}.
     * @param reference reference provided by the model by calling {@link Model#createReference(HasAssignmentContext)} during initialization
     */
    public void setAssignmentContextReference(AssignmentContextReference<V, T, C> reference);
    
    /**
     * Returns an assignment context associated with this object. If there is no 
     * assignment context associated with this object yet, one is created using the
     * {@link ConstraintWithContext#createAssignmentContext(Assignment)} method.
     * @param assignment given assignment
     * @return assignment context associated with this object and the given assignment
     */
    public C getContext(Assignment<V, T> assignment);
}