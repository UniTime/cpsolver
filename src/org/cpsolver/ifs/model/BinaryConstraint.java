package org.cpsolver.ifs.model;

/**
 * Binary constraint. <br>
 * <br>
 * Extension of {@link Constraint} that links exactly two variables.
 * 
 * @see Variable
 * @see Constraint
 * @see Model
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
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 * @param <V> Variable
 * @param <T> Value
 */

public abstract class BinaryConstraint<V extends Variable<V, T>, T extends Value<V, T>> extends Constraint<V, T> {
    private V iFirst = null, iSecond = null;

    public BinaryConstraint() {
        super();
    }

    @Override
    public void addVariable(V var) {
        if (iFirst == null)
            iFirst = var;
        else
            iSecond = var;
        super.addVariable(var);
    }

    /** First variable 
     * @return first variable of the constraint 
     **/
    public V first() {
        return iFirst;
    }

    /** Second variable 
     * @return second variable of the constraint 
     **/
    public V second() {
        return iSecond;
    }

    /** True, id the given variable is the first one 
     * @param variable given variable
     * @return true if the given variable is the first variable of the constraint
     **/
    public boolean isFirst(V variable) {
        return variable.equals(first());
    }

    /**
     * Returns the variable out of the constraints variables which is different
     * from the given variable.
     * @param variable given variable
     * @return the other variable of the constraint
     */
    public V another(V variable) {
        return (first() != null && variable.equals(first()) ? second() : first());
    }
}
