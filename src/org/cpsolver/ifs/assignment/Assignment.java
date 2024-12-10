package org.cpsolver.ifs.assignment;

import java.util.Collection;

import org.cpsolver.ifs.assignment.context.AssignmentContext;
import org.cpsolver.ifs.assignment.context.AssignmentContextReference;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;
import org.cpsolver.ifs.assignment.context.NeighbourSelectionWithContext;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;


/**
 * An assignment of all the variable of a {@link Model}. This class decouples 
 * an assignment of variables (classes extending {@link Variable}) to their values
 * (classes extending {@link Value}) from the {@link Model}. This is needed for 
 * any kind of parallel computations, or in general, to be able to hold multiple
 * different assignments in memory.<br><br>  
 * 
 * This class also include translation of {@link AssignmentContextReference} to
 * {@link AssignmentContext}, so that each constraint, criterion, neighborhood selection
 * etc. can hold its own assignment dependent information. See {@link ConstraintWithContext} or
 * {@link NeighbourSelectionWithContext} for more details.
 * 
 * @see Variable
 * @see Value
 * @see Model
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
 **/
public interface Assignment<V extends Variable<V, T>, T extends Value<V, T>> {
    
    /**
     * Assignment index. Unique identification of the assignment, starting with zero.
     * This can be used to increase the speed of the search by storing individual values on the 
     * variables and an array (indexed by this number).
     * @return assignment index, -1 if not to be used
     **/
    public int getIndex();
    
    /**
     * Returns assignment of a variable, null if not assigned
     * Replacement for {@link Variable#getAssignment()}.
     * @param variable problem variable
     * @return currently assigned value
     **/
    public T getValue(V variable);
    
    /**
     * Returns iteration of the last assignment.
     * Replacement for {@link Variable#getLastIteration()}.
     * @param variable problem variable
     * @return iteration of the last assignment
     **/
    public long getIteration(V variable);
    
    /**
     * Assign the given value to its variable.
     * Replacement for {@link Variable#assign(int, Value)}.
     * @param iteration current iteration
     * @param value a new value to be assigned to variable {@link Value#variable()}.
     * @return previous assignment of the variable, null if it was not assigned
     **/
    public T assign(long iteration, T value);
    
    /**
     * Unassign the given variable.
     * Replacement for {@link Variable#unassign(int)}.
     * @param iteration current iteration
     * @param variable variable to be unassigned
     * @return previous assignment of the variable, null if it was not assigned
     **/
    public T unassign(long iteration, V variable);
    
    /**
     * Unassign the given variable, but only if the current assignment differs from the given value.
     * Replacement for {@link Variable#unassign(int)}.
     * @param iteration current iteration
     * @param variable variable to be unassigned
     * @param value target value
     * @return previous assignment of the variable, null if it was not assigned
     **/
    public T unassign(long iteration, V variable, T value);
    
    /**
     * Number of assigned variables in this assignment.
     * Replacement for {@link Model#nrAssignedVariables()}.
     * @return number of assigned variables in this assignment
     **/
    public int nrAssignedVariables();
    
    /**
     * The list of assigned variables in the assignment. That is all the variables that {@link Assignment#getValue(Variable)} is not null in this assignment.
     * Replacement for {@link Model#assignedVariables()}.
     * @return a collection of assigned variable in this assignment
     **/
    public Collection<V> assignedVariables();

    /**
     * The list of assigned values in the assignment. That is a collection of {@link Assignment#getValue(Variable)} for all assigned variables in this assignment.
     * @return a collection of assigned values in this assignment
     **/
    public Collection<T> assignedValues();
    
    /**
     * Number of assigned variables in the assignment.
     * Replacement for {@link Model#nrUnassignedVariables()}.
     * @param model existing model (the assignment does not keep track about all existing variables, that is what the {@link Model#variables()} is for)
     * @return number of not assigned variables in this assignment
     **/
    public int nrUnassignedVariables(Model<V, T> model);

    /**
     * The list of variables of the model that have no value in this assignment. That is all the variables of the model that {@link Assignment#getValue(Variable)} is null in this assignment.
     * Replacement for {@link Model#unassignedVariables()}
     * @param model existing model (the assignment does not keep track about all existing variables, that is what the {@link Model#variables()} is for)
     * @return a collection of all not assigned variables in this assignment
     **/
    public Collection<V> unassignedVariables(Model<V, T> model);

    /**
     * Assignment context for a reference. This can be used to keep assignment dependent computations (e.g., see {@link ConstraintWithContext}).
     * @param reference a reference (which can be stored within the model, e.g., as an instance variable of a constraint)
     * @param <C> assignment context type
     * @return an {@link AssignmentContext}
     **/
    public <C extends AssignmentContext> C getAssignmentContext(AssignmentContextReference<V, T, C> reference);
    
    /**
     * Clear an assignment context that is associated with the given a reference. If there is any created for the reference.
     * @param reference a reference (which can be stored within the model, e.g., as an instance variable of a constraint)
     * @param <C> assignment context type
     **/
    public <C extends AssignmentContext> void clearContext(AssignmentContextReference<V, T, C> reference);
}