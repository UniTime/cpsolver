package org.cpsolver.ifs.example.csp;

import org.cpsolver.ifs.model.Value;

/**
 * CSP value.
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
 */
public class CSPValue extends Value<CSPVariable, CSPValue> {
    /**
     * Constructor
     * 
     * @param variable
     *            parent variable
     * @param value
     *            value (an integer between 0 .. number of values - 1 )
     */
    public CSPValue(CSPVariable variable, int value) {
        super(variable, Double.valueOf(value));
    }
    
    @Override
    public double toDouble() {
        return iValue;
    }

    @Override
    public String getName() {
        return String.valueOf(toDouble());
    }
}
