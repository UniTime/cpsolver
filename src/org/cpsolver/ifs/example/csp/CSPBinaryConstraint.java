package org.cpsolver.ifs.example.csp;

import java.util.Random;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.BinaryConstraint;


/**
 * CSP binary constraint. <br>
 * <br>
 * This class only implements the generation of a binary CSP constraint and the
 * consistency check.
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
public class CSPBinaryConstraint extends BinaryConstraint<CSPVariable, CSPValue> {
    private boolean iIsConsistent[][] = null;
    private int iNrCompatiblePairs;

    /**
     * Constructor
     * @param id constraint id
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
        int numberOfAllPairs = first().values(null).size() * second().values(null).size();
        int[][] allPairs = new int[numberOfAllPairs][];
        int idx = 0;

        iIsConsistent = new boolean[first().values(null).size()][second().values(null).size()];

        for (CSPValue v1 : first().values(null)) {
            for (CSPValue v2 : second().values(null)) {
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
    public void computeConflicts(Assignment<CSPVariable, CSPValue> assignment, CSPValue aValue, Set<CSPValue> conflicts) {
        if (isFirst(aValue.variable())) {
            if (!isConsistent(aValue, assignment.getValue(second()))) {
                conflicts.add(assignment.getValue(second()));
            }
        } else {
            if (!isConsistent(assignment.getValue(first()), aValue)) {
                conflicts.add(assignment.getValue(first()));
            }
        }
    }

    @Override
    public String getName() {
        return "C" + getId();
    }
}