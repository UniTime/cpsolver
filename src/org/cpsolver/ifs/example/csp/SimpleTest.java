package org.cpsolver.ifs.example.csp;

import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.ToolBox;

/**
 * Simple test of IFS CBS algorithm on random binary CSP problem
 * CSP(25,12,198/300,36/144).
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
public class SimpleTest {
    /**
     * run the test
     * @param args program arguments
     */
    public static void main(String[] args) {
        ToolBox.configureLogging();
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

        org.cpsolver.ifs.util.DataProperties cfg = new org.cpsolver.ifs.util.DataProperties();
        cfg.setProperty("Termination.Class", "org.cpsolver.ifs.termination.GeneralTerminationCondition");
        cfg.setProperty("Termination.StopWhenComplete", "false");
        cfg.setProperty("Termination.TimeOut", "60");
        cfg.setProperty("Comparator.Class", "org.cpsolver.ifs.solution.GeneralSolutionComparator");
        cfg.setProperty("Value.Class", "org.cpsolver.ifs.heuristics.GeneralValueSelection");
        cfg.setProperty("Value.WeightConflicts", "1");
        cfg.setProperty("Variable.Class", "org.cpsolver.ifs.heuristics.GeneralVariableSelection");
        cfg.setProperty("Extensions.Classes", "org.cpsolver.ifs.extension.ConflictStatistics");

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
        System.out.println("Number of assigned variables is " + solution.getAssignment().nrAssignedVariables());
        System.out.println("Total value of the solution is " + solution.getModel().getTotalValue(solution.getAssignment()));

        int idx = 1;
        for (CSPVariable v : solution.getModel().variables()) {
            CSPValue a = solution.getAssignment().getValue(v);
            if (a != null)
                System.out.println("Var" + (idx++) + "=" + a.toDouble());
        }
    }
}
