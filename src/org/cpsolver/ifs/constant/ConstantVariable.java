package org.cpsolver.ifs.constant;

import org.cpsolver.ifs.model.Value;

/**
 * Extension of a variable with the possibility to have a constant value.
 * 
 * Such variables are excluded from the solver process, however, they can be
 * included in constraints.
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
 * @param <T> Value
 */
public interface ConstantVariable<T extends Value<?, T>> {
    /** True, if the variable is constant. 
     * @return true if constant
     **/
    public boolean isConstant();
    
    /** Return assignment if constant
     * @return constant value (if {@link ConstantVariable#isConstant()} is true)
     **/
    public T getConstantValue();
}
