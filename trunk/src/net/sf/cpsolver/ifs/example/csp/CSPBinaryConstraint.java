package net.sf.cpsolver.ifs.example.csp;

import java.util.*;

import net.sf.cpsolver.ifs.model.*;

/**
 * CSP binary constraint. <br>
 * <br>
 * This class only implements the generation of a binary CSP constraint and the
 * consistency check.
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
 */
public class CSPBinaryConstraint extends BinaryConstraint<CSPVariable, CSPValue> {
    private boolean iIsConsistent[][] = null;
    private int iNrCompatiblePairs;

    /**
     * Constructor
     * 
     * @param nrCompatiblePairs
     *            number of compatible pairs of values in the constraint
     */
    public CSPBinaryConstraint(int id, int nrCompatiblePairs) {
        super();
        iId = id;
        iNrCompatiblePairs = nrCompatiblePairs;
    }

    private void swap(int[][] allPairs, int first, int second) {
        int[] a = allPairs[first];
        allPairs[first] = allPairs[second];
        allPairs[second] = a;
    }

    /**
     * Initializes the constraint. Randomly generates the given number of
     * compatible pairs of values.
     * 
     * @param rndNumGen
     *            random number generator
     */
    public void init(Random rndNumGen) {
        int numberOfAllPairs = first().values().size() * second().values().size();
        int[][] allPairs = new int[numberOfAllPairs][];
        int idx = 0;

        iIsConsistent = new boolean[first().values().size()][second().values().size()];

        for (CSPValue v1 : first().values()) {
            for (CSPValue v2 : second().values()) {
                iIsConsistent[(int) v1.toDouble()][(int) v2.toDouble()] = false;
                allPairs[idx++] = new int[] { (int) v1.toDouble(), (int) v2.toDouble() };
            }
        }

        for (int i = 0; i < iNrCompatiblePairs; i++) {
            swap(allPairs, i, i + (int) (rndNumGen.nextDouble() * (numberOfAllPairs - i)));
            iIsConsistent[allPairs[i][0]][allPairs[i][1]] = true;
        }
    }

    /**
     * True if the pair of given values is compatible.
     */
    @Override
    public boolean isConsistent(CSPValue value1, CSPValue value2) {
        if (value1 == null || value2 == null)
            return true;
        if (isFirst(value1.variable())) {
            return iIsConsistent[(int) value1.toDouble()][(int) value2.toDouble()];
        } else {
            return iIsConsistent[(int) value2.toDouble()][(int) value1.toDouble()];
        }
    }

    /**
     * Add the other variable to the set of conflicts, if it is not compatible
     * with the given value.
     */
    @Override
    public void computeConflicts(CSPValue aValue, Set<CSPValue> conflicts) {
        if (isFirst(aValue.variable())) {
            if (!isConsistent(aValue, second().getAssignment())) {
                conflicts.add(second().getAssignment());
            }
        } else {
            if (!isConsistent(first().getAssignment(), aValue)) {
                conflicts.add(first().getAssignment());
            }
        }
    }

    @Override
    public String getName() {
        return "C" + getId();
    }
}