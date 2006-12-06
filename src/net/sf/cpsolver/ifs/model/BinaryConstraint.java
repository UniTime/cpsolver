package net.sf.cpsolver.ifs.model;

/**
 * Binary constraint.
 * <br><br>
 * Extension of {@link Constraint} that links exactly two variables.
 *
 * @see Variable
 * @see Constraint
 * @see Model
 *
 * @version
 * IFS 1.1 (Iterative Forward Search)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@ktiml.mff.cuni.cz">muller@ktiml.mff.cuni.cz</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

public abstract class BinaryConstraint extends Constraint {
    private Variable iFirst = null, iSecond = null;
    public BinaryConstraint() {
        super();
    }
    
    public void addVariable(Variable var) {
        if (iFirst == null)
            iFirst = (Variable)var;
        else
            iSecond = (Variable)var;
        super.addVariable(var);
    }
    
    /** First variable */
    public Variable first() {
        return iFirst;
    }
    
    /** Second variable */
    public Variable second() {
        return iSecond;
    }
    
    /** True, id the given variable is the first one */
    public boolean isFirst(Variable variable) {
        return variable.equals(first());
    }
    
    /** Returns the variable out of the constraints variables which is different from the given variable.*/
    public Variable another(Variable variable) {
        return (
                first() != null && variable.equals(first()) ? second() : first());
    }
}
