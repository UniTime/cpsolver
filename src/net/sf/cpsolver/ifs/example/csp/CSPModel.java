package net.sf.cpsolver.ifs.example.csp;

import java.util.Iterator;
import java.util.Random;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Model;

/**
 * Random Binary CSP with uniform distribution. <br>
 * <br>
 * A random CSP is defined by a four-tuple (n, d, p1, p2), where n denotes the
 * number of variables and d denotes the domain size of each variable, p1 and p2
 * are two probabilities. They are used to generate randomly the binary
 * constraints among the variables. p1 represents the probability that a
 * constraint exists between two different variables and p2 represents the
 * probability that a pair of values in the domains of two variables connected
 * by a constraint are incompatible. <br>
 * <br>
 * We use a so called model B of Random CSP (n, d, n1, n2) where n1 =
 * p1*n*(n-1)/2 pairs of variables are randomly and uniformly selected and
 * binary constraints are posted between them. For each constraint, n2 = p1*d^2
 * randomly and uniformly selected pairs of values are picked as incompatible.
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
public class CSPModel extends Model<CSPVariable, CSPValue> {

    /**
     * Constructor
     * 
     * @param nrVariables
     *            number of variables in the problem
     * @param nrValues
     *            number of values of each variable
     * @param nrConstraints
     *            number of constraints in the problem
     * @param nrCompatiblePairs
     *            number of compatible pairs of values for every constraint
     * @param seed
     *            seed for random number generator (use
     *            {@link System#currentTimeMillis} if not bother)
     */
    public CSPModel(int nrVariables, int nrValues, int nrConstraints, int nrCompatiblePairs, long seed) {
        generate(nrVariables, nrValues, nrConstraints, nrCompatiblePairs, seed);
    }

    public CSPModel() {
    }

    private void swap(CSPVariable[][] allPairs, int first, int second) {
        CSPVariable[] a = allPairs[first];
        allPairs[first] = allPairs[second];
        allPairs[second] = a;
    }

    private void buildBinaryConstraintGraph(Random rnd) {
        int numberOfAllPairs = variables().size() * (variables().size() - 1) / 2;
        CSPVariable[][] allPairs = new CSPVariable[numberOfAllPairs][];
        int idx = 0;
        for (CSPVariable v1 : variables()) {
            for (CSPVariable v2 : variables()) {
                if (v1.getId() >= v2.getId())
                    continue;
                allPairs[idx++] = new CSPVariable[] { v1, v2 };
            }
        }
        idx = 0;
        for (Iterator<Constraint<CSPVariable, CSPValue>> i = constraints().iterator(); i.hasNext();) {
            CSPBinaryConstraint c = (CSPBinaryConstraint) i.next();
            swap(allPairs, idx, idx + (int) (rnd.nextDouble() * (numberOfAllPairs - idx)));
            c.addVariable(allPairs[idx][0]);
            c.addVariable(allPairs[idx][1]);
            c.init(rnd);
            idx++;
        }
    }

    private void generate(int nrVariables, int nrValues, int nrConstraints, int nrCompatiblePairs, long seed) {
        Random rnd = new Random(seed);

        for (int i = 0; i < nrVariables; i++) {
            CSPVariable var = new CSPVariable(i + 1, nrValues);
            addVariable(var);
        }

        for (int i = 0; i < nrConstraints; i++) {
            CSPBinaryConstraint c = new CSPBinaryConstraint(i + 1, nrCompatiblePairs);
            addConstraint(c);
        }

        buildBinaryConstraintGraph(rnd);
    }
}
