package net.sf.cpsolver.ifs.constant;


import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.util.ArrayList;
import net.sf.cpsolver.ifs.util.List;

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
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 * 
 */

public class ConstantModel<V extends Variable<V, T>, T extends Value<V, T>> extends Model<V, T> {
    private List<V> iConstantVariables = null;

    /** List of constant variables */
    public List<V> constantVariables() {
        return iConstantVariables;
    }

    /** True, if the model contains at least one constant variable. */
    public boolean hasConstantVariables() {
        return iConstantVariables != null && !iConstantVariables.isEmpty();
    }

    /** True, if the given variable is constant. */
    public boolean isConstant(V variable) {
        return (iConstantVariables != null && variable instanceof ConstantVariable && ((ConstantVariable) variable)
                .isConstant());
    }

    /** Adds a variable to the model */
    @Override
    public void addVariable(V variable) {
        if (variable instanceof ConstantVariable && ((ConstantVariable) variable).isConstant()) {
            if (iConstantVariables == null)
                iConstantVariables = new ArrayList<V>();
            variable.setModel(this);
            iConstantVariables.add(variable);
            if (variable.getAssignment() != null)
                variable.assign(0L, variable.getAssignment());
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
    public void beforeAssigned(long iteration, T value) {
        if (!isConstant(value.variable()))
            super.beforeAssigned(iteration, value);
    }

    /**
     * Called before a value is unassigned from its variable. Constant variables
     * are excluded from (re)assignment.
     */
    @Override
    public void beforeUnassigned(long iteration, T value) {
        if (!isConstant(value.variable()))
            super.beforeUnassigned(iteration, value);
    }

    /**
     * Called after a value is assigned to its variable. Constant variables are
     * excluded from (re)assignment.
     */
    @Override
    public void afterAssigned(long iteration, T value) {
        if (!isConstant(value.variable()))
            super.afterAssigned(iteration, value);
    }

    /**
     * Called after a value is unassigned from its variable. Constant variables
     * are excluded from (re)assignment.
     */
    @Override
    public void afterUnassigned(long iteration, T value) {
        if (!isConstant(value.variable()))
            super.afterUnassigned(iteration, value);
    }
}
