package org.cpsolver.ifs.constant;


import java.util.ArrayList;
import java.util.List;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;


/**
 * Extension of the model with constant variables.
 * 
 * Such variables are excluded from the solver process, however, they can be
 * included in constraints. Such model can allow us to build a solution on top
 * of another solution (e.g., committed classes in the course timetabling).
 * 
 * Constant variable has to implement interface {@link ConstantVariable},
 * returning {@link ConstantVariable#isConstant()} true.
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
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
 * 
 * @param <V> Variable
 * @param <T> Value
 */
public class ConstantModel<V extends Variable<V, T>, T extends Value<V, T>> extends Model<V, T> {
    private List<V> iConstantVariables = null;

    /** List of constant variables 
     * @return all constants
     **/
    public List<V> constantVariables() {
        return iConstantVariables;
    }

    /** True, if the model contains at least one constant variable.
     * @return true, if there is at least one constant in this model
     **/
    public boolean hasConstantVariables() {
        return iConstantVariables != null && !iConstantVariables.isEmpty();
    }

    /** True, if the given variable is constant.
     * @param variable given variable
     * @return true if constant
     **/
    public boolean isConstant(V variable) {
        return (iConstantVariables != null && variable instanceof ConstantVariable && ((ConstantVariable<?>) variable).isConstant());
    }

    /** Adds a variable to the model */
    @Override
    public void addVariable(V variable) {
        if (variable instanceof ConstantVariable && ((ConstantVariable<?>) variable).isConstant()) {
            if (iConstantVariables == null)
                iConstantVariables = new ArrayList<V>();
            variable.setModel(this);
            iConstantVariables.add(variable);
        } else
            super.addVariable(variable);
    }

    /** Removes a variable from the model */
    @Override
    public void removeVariable(V variable) {
        if (isConstant(variable)) {
            variable.setModel(null);
            iConstantVariables.remove(variable);
        } else
            super.removeVariable(variable);
    }

    /**
     * Called before a value is assigned to its variable. Constant variables are
     * excluded from (re)assignment.
     */
    @Override
    public void beforeAssigned(Assignment<V, T> assignment, long iteration, T value) {
        if (!isConstant(value.variable()))
            super.beforeAssigned(assignment, iteration, value);
    }

    /**
     * Called before a value is unassigned from its variable. Constant variables
     * are excluded from (re)assignment.
     */
    @Override
    public void beforeUnassigned(Assignment<V, T> assignment, long iteration, T value) {
        if (!isConstant(value.variable()))
            super.beforeUnassigned(assignment, iteration, value);
    }

    /**
     * Called after a value is assigned to its variable. Constant variables are
     * excluded from (re)assignment.
     */
    @Override
    public void afterAssigned(Assignment<V, T> assignment, long iteration, T value) {
        if (!isConstant(value.variable()))
            super.afterAssigned(assignment, iteration, value);
    }

    /**
     * Called after a value is unassigned from its variable. Constant variables
     * are excluded from (re)assignment.
     */
    @Override
    public void afterUnassigned(Assignment<V, T> assignment, long iteration, T value) {
        if (!isConstant(value.variable()))
            super.afterUnassigned(assignment, iteration, value);
    }
}
