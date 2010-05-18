package net.sf.cpsolver.ifs.example.csp;

import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;

/**
 * Simple test of IFS CBS algorithm on random binary CSP problem
 * CSP(25,12,198/300,36/144).
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
public class SimpleTest {
    /**
     * run the test
     */
    public static void main(String[] args) {
        org.apache.log4j.BasicConfigurator.configure();
        int nrVariables = 25;
        int nrValues = 12;
        int nrConstraints = 198;
        double tigtness = 0.25;
        int nrAllPairs = nrValues * nrValues;
        int nrCompatiblePairs = (int) ((1.0 - tigtness) * nrAllPairs);
        long seed = System.currentTimeMillis();
        System.out.println("CSP(" + nrVariables + "," + nrValues + "," + nrConstraints + "/"
                + ((nrVariables * (nrVariables - 1)) / 2) + "," + (nrAllPairs - nrCompatiblePairs) + "/" + nrAllPairs
                + ")");

        net.sf.cpsolver.ifs.util.DataProperties cfg = new net.sf.cpsolver.ifs.util.DataProperties();
        cfg.setProperty("Termination.Class", "net.sf.cpsolver.ifs.termination.GeneralTerminationCondition");
        cfg.setProperty("Termination.StopWhenComplete", "true");
        cfg.setProperty("Termination.TimeOut", "60");
        cfg.setProperty("Comparator.Class", "net.sf.cpsolver.ifs.solution.GeneralSolutionComparator");
        cfg.setProperty("Value.Class", "net.sf.cpsolver.ifs.heuristics.GeneralValueSelection");
        cfg.setProperty("Value.WeightConflicts", "1");
        cfg.setProperty("Variable.Class", "net.sf.cpsolver.ifs.heuristics.GeneralVariableSelection");
        cfg.setProperty("Extensions.Classes", "net.sf.cpsolver.ifs.extension.ConflictStatistics");

        CSPModel model = new CSPModel(nrVariables, nrValues, nrConstraints, nrCompatiblePairs, seed);
        Solver<CSPVariable, CSPValue> solver = new Solver<CSPVariable, CSPValue>(cfg);
        solver.setInitalSolution(model);

        solver.start();
        try {
            solver.getSolverThread().join();
        } catch (InterruptedException e) {
        }

        Solution<CSPVariable, CSPValue> solution = solver.lastSolution();
        solution.restoreBest();

        System.out.println("Best solution found after " + solution.getBestTime() + " seconds ("
                + solution.getBestIteration() + " iterations).");
        System.out.println("Number of assigned variables is " + solution.getModel().assignedVariables().size());
        System.out.println("Total value of the solution is " + solution.getModel().getTotalValue());

        int idx = 1;
        for (CSPVariable v : ((CSPModel) solution.getModel()).variables()) {
            if (v.getAssignment() != null)
                System.out.println("Var" + (idx++) + "=" + v.getAssignment().toDouble());
        }
    }
}
