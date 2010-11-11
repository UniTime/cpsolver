package net.sf.cpsolver.ifs.example.csp;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.sf.cpsolver.ifs.model.Variable;

/**
 * CSP variable. <br>
 * <br>
 * This class only implements generation of variable's values (domain)
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
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
public class CSPVariable extends Variable<CSPVariable, CSPValue> {
    private int iKernelId = -1;

    /**
     * Constructor
     * 
     * @param domainSize
     *            number of values of the variable
     */
    public CSPVariable(int id, int domainSize) {
        this(id, domainSize, -1);
    }

    /**
     * Constructor
     * 
     * @param domainSize
     *            number of values of the variable
     * @param kernelId
     *            kernel id (for structured CSP)
     */
    public CSPVariable(int id, int domainSize, int kernelId) {
        super(null);
        iId = id;
        iKernelId = kernelId;
        setValues(computeValues(domainSize));
    }

    /** Get kernel id */
    public int getKernelId() {
        return iKernelId;
    }

    /**
     * Generate an intial value (for MPP and for forcing of existance of a
     * solution)
     */
    public void generateInitialValue(Random rnd) {
        CSPValue aValue = values().get((int) (rnd.nextFloat() * values().size()));
        setInitialAssignment(aValue);
    }

    private List<CSPValue> computeValues(int domainSize) {
        List<CSPValue> values = new ArrayList<CSPValue>();
        for (int i = 0; i < domainSize; i++) {
            CSPValue value = new CSPValue(this, i);
            values.add(value);
        }
        return values;
    }

    @Override
    public String getName() {
        return "V" + getId();
    }
}
