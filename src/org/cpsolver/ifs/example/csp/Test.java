package org.cpsolver.ifs.example.csp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.StringTokenizer;

import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.ifs.util.ProgressWriter;
import org.cpsolver.ifs.util.ToolBox;


/**
 * Test of Structured CSP problems. It takes one argument -- property file with
 * all the parameters. It allows to execute given number of tests. It also
 * allows to define several configurations which will be executed. For instance
 * CSP(20,15,5%..95%,5..95%), 10 runs of each configuration. All such
 * configuration are processed in one run of Test class. <br>
 * <br>
 * In Structured CSP, variables are divided into several kernels (some variables
 * may remain ouside kernels). Different constraints (in density and tightnes)
 * are generated according to whether variables are from the same kernel or not. <br>
 * <br>
 * Test's parameters: <br>
 * <table border='1'><caption>Related Solver Parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>General.MPP</td>
 * <td>{@link String}</td>
 * <td>Minimal perturbation problem (if true), this mj. means that initial
 * assignment will be generated</td>
 * </tr>
 * <tr>
 * <td>CSP.Seed</td>
 * <td>{@link Long}</td>
 * <td>Random number generator seed, {@link System#currentTimeMillis()} is taken
 * if not present</td>
 * </tr>
 * <tr>
 * <td>CSP.ForceSolutionExistance</td>
 * <td>{@link Boolean}</td>
 * <td>If true, generated problem will always have at least one feasible
 * solution</td>
 * </tr>
 * <tr>
 * <td>CPS.NrTests</td>
 * <td>{@link Integer}</td>
 * <td>Number of tests (for each input configuration)</td>
 * </tr>
 * <tr>
 * <td>CSP.NrVariables</td>
 * <td>{@link Integer}</td>
 * <td>Number of variables</td>
 * </tr>
 * <tr>
 * <td>CSP.NrVariablesMin<br>
 * CSP.NrVariablesMax<br>
 * CSP.NrVariablesStep</td>
 * <td>{@link Integer}</td>
 * <td>Range of the number variables (a set of different configurations will be
 * generated)<br>
 * Use either CSP.NrVariables or these CSP.NrVariablesMin, CSP.NrVariablesMax,
 * CSP.NrVariablesStep</td>
 * </tr>
 * <tr>
 * <td>CSP.DomainSize</td>
 * <td>{@link Integer}</td>
 * <td>Number of values of every variable</td>
 * </tr>
 * <tr>
 * <td>CSP.DomainSizeRatio</td>
 * <td>{@link Double}</td>
 * <td>Number of values as a ration of the number of variables. This way we can
 * model for instance CSP(N,2N,p1,p2) problems with one configuration.<br>
 * Use either CSP.DomainSize or CSP.DomainSizeRatio</td>
 * </tr>
 * <tr>
 * <td>CSP.Tightness</td>
 * <td>{@link Double}</td>
 * <td>Tightness of constraints outside kernels</td>
 * </tr>
 * <tr>
 * <td>CSP.TightnessMin<br>
 * CSP.TightnessMax<br>
 * CSP.TightnessStep</td>
 * <td>{@link Double}</td>
 * <td>Tightness of constraints outside kernels given as a range &rarr; respective
 * configurations will be generated and tested</td>
 * </tr>
 * <tr>
 * <td>CSP.Density</td>
 * <td>{@link Double}</td>
 * <td>Density of constraints outside kernels</td>
 * </tr>
 * <tr>
 * <td>CSP.DensityMin<br>
 * CSP.DensityMax<br>
 * CSP.DensityStep</td>
 * <td>{@link Double}</td>
 * <td>Density of constraints outside kernels given as a range &rarr; respective
 * configurations will be generated and tested</td>
 * </tr>
 * <tr>
 * <td>CSP.NrKernels</td>
 * <td>{@link Integer}</td>
 * <td>Number of kernels (Structured CSP, use 0 for "normal" CSP)</td>
 * </tr>
 * <tr>
 * <td>CSP.KernelSize</td>
 * <td>{@link Integer}</td>
 * <td>Number of variables in each kernel</td>
 * </tr>
 * <tr>
 * <td>CSP.KernelTightness</td>
 * <td>{@link Double}</td>
 * <td>Tightness of constraints inside a kernel</td>
 * </tr>
 * <tr>
 * <td>CSP.KernelDensity</td>
 * <td>{@link Double}</td>
 * <td>Density of constraints inside a kernel</td>
 * </tr>
 * <tr>
 * <td>CSP.SameProblemEachStep</td>
 * <td>{@link Boolean}</td>
 * <td>If true, each configuration will start with the same seed</td>
 * </tr>
 * <tr>
 * <td>CSP.SameProblemEachTest</td>
 * <td>{@link Boolean}</td>
 * <td>If true, each test of the same configuration will start with the same
 * seed</td>
 * </tr>
 * <tr>
 * <td>General.Output</td>
 * <td>{@link String}</td>
 * <td>Output folder where a log file and tables with results. In order not to
 * overwrite the results if executed more than once, a subfolder with the name
 * taken from current date and time will be created in this folder and all
 * results will go to this subfolder.</td>
 * </tr>
 * </table>
 * <br>
 * <br>
 * Also, the configuration file can consist only from one parameter (named
 * INCLUDE_REGEXP) which is processed as a regular expression of semicolon
 * separated list of property files, for instance
 * <pre><code>INCLUDE_REGEXP=general.ini;{CSP(50,12,250,p2)|CSP(25,15,198,p2)}.ini;{std|opt}.ini;{10x1min}.ini;{cbs|rw1|tabu20}.ini</code></pre>
 * where {a|b|c|...} means a selection of a, b, c, .. All possible combinations
 * are taken and for each of them an input configuration is combined from the
 * relevant files. So, for instance, the above example will result into the
 * following configurations:
 * <ul>
 * <li>general.ini;CSP(50,12,250,p2).ini;std.ini;10x1min.ini;cbs.ini
 * <li>general.ini;CSP(50,12,250,p2).ini;std.ini;10x1min.ini;rw1.ini
 * <li>general.ini;CSP(50,12,250,p2).ini;std.ini;10x1min.ini;tabu20.ini
 * <li>general.ini;CSP(50,12,250,p2).ini;opt.ini;10x1min.ini;cbs.ini
 * <li>general.ini;CSP(50,12,250,p2).ini;opt.ini;10x1min.ini;rw1.ini
 * <li>general.ini;CSP(50,12,250,p2).ini;opt.ini;10x1min.ini;tabu20.ini
 * <li>general.ini;CSP(25,15,198,p2).ini;std.ini;10x1min.ini;cbs.ini
 * <li>general.ini;CSP(25,15,198,p2).ini;std.ini;10x1min.ini;rw1.ini
 * <li>general.ini;CSP(25,15,198,p2).ini;std.ini;10x1min.ini;tabu20.ini
 * <li>general.ini;CSP(25,15,198,p2).ini;opt.ini;10x1min.ini;cbs.ini
 * <li>general.ini;CSP(25,15,198,p2).ini;opt.ini;10x1min.ini;rw1.ini
 * <li>general.ini;CSP(25,15,198,p2).ini;opt.ini;10x1min.ini;tabu20.ini
 * </ul>
 * To be able to distinguish such configuration a subfolder in General.Output
 * folder is created, its name is combined from the names which are in
 * parenthesis. So, for instance the first bunch of tests will output into the
 * folder:
 * <pre><code>
 * ${General.Output}\CSP(50,12,250,p2)_std_10x1min_csb\25-Feb-05_191136
 * </code></pre>
 * If one parameter is defined in more than one configuration files (e.g. in
 * general.ini as well as cbs.ini) the one from the file more on the right is
 * taken. <br>
 * <br>
 * An example of the configurations:<br>
 * File<b> general.ini</b>
 * <pre><code>
 * #Default settings common for all configurations
 * General.MPP=false
 * General.InitialAssignment=false
 * General.Output=output\\RandomCSP\\IFS
 * 
 * #Value selection heuristics
 * Value.Class=org.cpsolver.ifs.heuristics.GeneralValueSelection
 * Value.WeightWeightedConflicts=0.0
 * Value.RandomWalkProb=0.0
 * Value.WeightConflicts=1.0
 * Value.WeightNrAssignments=0.0
 * Value.WeightValue=0.0
 * Value.Tabu=0
 * 
 * #Variable selection heuristics
 * Variable.Class=org.cpsolver.ifs.heuristics.GeneralVariableSelection
 * Variable.RandomSelection=true
 * 
 * #Termination condition
 * Termination.Class=org.cpsolver.ifs.termination.GeneralTerminationCondition
 * Termination.MaxIters=-1
 * Termination.TimeOut=-1
 * Termination.StopWhenComplete=true
 * 
 * #Solution comparator
 * Comparator.Class=org.cpsolver.ifs.solution.GeneralSolutionComparator
 * </code></pre>
 * File<b> CSP(50,12,250,p2).ini</b>
 * <pre><code>
 * #Sparse problem CSP(50,12,250/1225,p2)
 * CSP.NrVariables=50
 * CSP.DomainSize=12
 * CSP.Density=0.2
 * CSP.TightnessMin=0.10
 * CSP.TightnessMax=0.95
 * CSP.TightnessStep=0.02
 *  
 * CSP.Seed=780921
 * 
 * CSP.ForceSolutionExistance=false
 * CSP.SameProblemEachStep=false
 * CSP.SameProblemEachTest=false
 * 
 * CSP.NrKernels=0
 * </code></pre>
 * File<b> std.ini</b>
 * <pre><code>
 * #Standard problem
 * CSP.ForceSolutionExistance=false
 * </code></pre>
 * File<b> opt.ini</b>
 * <pre><code>
 * #Optimization problem (minCSP)
 * #Value selection: use weigh of a conflict, but when there are more than one value
 * #        with the same number of conflicts, use the one with lower value
 * Value.WeightValue=0.0001
 * Value.WeightConflicts=1.0
 * #Do not stop when a complete solution is found
 * Termination.StopWhenComplete=false
 * </code></pre>
 * File<b> 10x1min.ini</b>
 * <pre><code>
 * #For each configuration, execute 10 tests, each with 1 minute timeout
 * CPS.NrTests=10
 * Termination.TimeOut=60
 * </code></pre>
 * File<b> cbs.ini</b>
 * <pre><code>
 * #Use conflict-based statistics
 * Extensions.Classes=org.cpsolver.ifs.extension.ConflictStatistics
 * Value.WeightWeightedConflicts=1.0
 * </code></pre>
 * File<b> tabu20.ini</b>
 * <pre><code>
 * #Use tabu-list of the length 20
 * Value.Tabu=20
 * </code></pre>
 * File<b> rw1.ini</b>
 * <pre><code>
 * #Use 1% random walk selection
 * Value.RandomWalkProb=0.01
 * </code></pre>
 * 
 * @see StructuredCSPModel
 * @see org.cpsolver.ifs.extension.ConflictStatistics
 * @see org.cpsolver.ifs.heuristics.GeneralValueSelection
 * @see org.cpsolver.ifs.heuristics.GeneralVariableSelection
 * @see org.cpsolver.ifs.termination.GeneralTerminationCondition
 * @see org.cpsolver.ifs.solution.GeneralSolutionComparator
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
public class Test {
    private static java.text.DecimalFormat sDoubleFormat = new java.text.DecimalFormat("0.000",
            new java.text.DecimalFormatSymbols(Locale.US));
    private static java.text.SimpleDateFormat sDateFormat = new java.text.SimpleDateFormat("dd-MMM-yy_HHmmss",
            java.util.Locale.US);
    private static org.apache.logging.log4j.Logger sLogger = org.apache.logging.log4j.LogManager.getLogger(Test.class);

    private static void test(DataProperties properties) throws Exception {
        boolean sameProblemStep = properties.getPropertyBoolean("CSP.SameProblemEachStep", false);
        boolean sameProblemTest = properties.getPropertyBoolean("CSP.SameProblemEachTest", false);
        int nrVars = properties.getPropertyInt("CSP.NrVariables", 20);
        int nrKernels = properties.getPropertyInt("CSP.NrKernels", 2);
        int nrKernelVariables = properties.getPropertyInt("CSP.KernelSize", 8);
        int nrVariablesMin = properties.getPropertyInt("CSP.NrVariablesMin", nrVars);
        int nrVariablesMax = properties.getPropertyInt("CSP.NrVariablesMax", nrVars);
        int nrVariablesStep = properties.getPropertyInt("CSP.NrVariablesStep", 1);
        int nrValues = properties.getPropertyInt("CSP.DomainSize", 10);
        double nrValuesRatio = properties.getPropertyDouble("CSP.DomainSizeRatio", -1);
        float kernelTightness = properties.getPropertyFloat("CSP.KernelTightness", 0.097f);
        float kernelDensity = properties.getPropertyFloat("CSP.KernelDensity", 0.097f);
        float tightnessInit = properties.getPropertyFloat("CSP.Tightness", 0.4f);
        float tightnessMin = properties.getPropertyFloat("CSP.TightnessMin", tightnessInit);
        float tightnessMax = properties.getPropertyFloat("CSP.TightnessMax", tightnessInit) + 1e-6f;
        float tightnessStep = properties.getPropertyFloat("CSP.TightnessStep", 0.1f);
        float densityInit = properties.getPropertyFloat("CSP.Density", 0.4f);
        float densityMin = properties.getPropertyFloat("CSP.DensityMin", densityInit);
        float densityMax = properties.getPropertyFloat("CSP.DensityMax", densityInit) + 1e-6f;
        float densityStep = properties.getPropertyFloat("CSP.DensityStep", 0.1f);
        long seed = properties.getPropertyLong("CSP.Seed", System.currentTimeMillis());
        int nrTests = properties.getPropertyInt("CPS.NrTests", 10);
        boolean mpp = properties.getPropertyBoolean("General.MPP", false);
        PrintWriter logStat = new PrintWriter(new FileWriter(properties.getProperty("General.Output") + File.separator
                + "rcsp_" + nrVariablesMin + "_" + nrValues + ".csv"));
        PrintWriter logAvgStat = new PrintWriter(new FileWriter(properties.getProperty("General.Output")
                + File.separator + "avg_stat.csv"));
        PrintWriter log = new PrintWriter(new FileWriter(properties.getProperty("General.Output") + File.separator
                + "info.txt"));
        logStat
                .println("testNr;nrVars;nrVals;density[%];tightness[%];time[s];iters;speed[it/s];unassConstr;assigned;assigned[%]"
                        + (mpp ? ";perts;perts[%]" : "") + ";value;totalValue");
        logAvgStat
                .println("nrVars;nrVals;density[%];tightness[%];time[s];RMStime[s];iters;RMSiters;speed[it/s];unassConst;assigned;RMSassigned;assigned[%]"
                        + (mpp ? ";perts;RMSperts;perts[%]" : "") + ";value;RMSvalue;totalValue;RMStotalValue");
        System.out.println("Number of variables: " + nrVariablesMin + " .. " + nrVariablesMax + "  (step="
                + nrVariablesStep + ")");
        System.out.println("Density:             " + densityMin + " .. " + densityMax + "  (step=" + densityStep + ")");
        System.out.println("Tightness:           " + tightnessMin + " .. " + tightnessMax + "  (step=" + tightnessStep
                + ")");
        for (int nrVariables = nrVariablesMin; nrVariables <= nrVariablesMax; nrVariables += nrVariablesStep) {
            if (nrValuesRatio > 0.0)
                nrValues = (int) Math.round(nrValuesRatio * nrVariables);
            for (float density = densityMin; density <= densityMax; density += densityStep) {
                for (float tightness = tightnessMin; tightness <= tightnessMax; tightness += tightnessStep) {
                    log.println("CSP{#Var=" + nrVariables + ", #Val=" + nrValues + ", P(density)="
                            + sDoubleFormat.format(100.0 * density) + "%, P(tighness)="
                            + sDoubleFormat.format(100.0 * tightness) + ", " + nrKernels + "x Kernel{#Var="
                            + nrKernelVariables + ", P(density)=" + sDoubleFormat.format(100.0 * kernelDensity)
                            + "%, P(tighness)=" + sDoubleFormat.format(100.0 * kernelTightness) + "%}}");
                    double sumTime = 0;
                    double sumTime2 = 0;
                    int sumIters = 0;
                    int sumIters2 = 0;
                    int sumConfl = 0;
                    int sumAssign = 0;
                    int sumAssign2 = 0;
                    int sumPert = 0;
                    int sumPert2 = 0;
                    int sumVal = 0;
                    int sumVal2 = 0;
                    int sumTotalVal = 0;
                    int sumTotalVal2 = 0;
                    for (int test = 1; test <= nrTests; test++) {
                        log.println("  " + test + ". test");
                        log.flush();
                        properties.setProperty("CSP.NrVariables", String.valueOf(nrVariables));
                        properties.setProperty("CSP.Tightness", String.valueOf(tightness));
                        properties.setProperty("CSP.Density", String.valueOf(density));

                        long currentSeed = (seed * 1000000L)
                                + (1000 * (long) ((sameProblemStep ? densityMin : density) * 1000.0))
                                + ((long) ((sameProblemStep ? tightnessMin : tightness) * 1000.0));
                        currentSeed = (currentSeed * nrTests) + (sameProblemTest ? 0 : test - 1);

                        sLogger.debug("Seed: " + currentSeed);
                        StructuredCSPModel csp = new StructuredCSPModel(properties, currentSeed);

                        Solver<CSPVariable, CSPValue> s = new Solver<CSPVariable, CSPValue>(properties);
                        s.setInitalSolution(csp);
                        s.currentSolution().clearBest();
                        s.start();

                        try {
                            s.getSolverThread().join();
                        } catch (NullPointerException npe) {
                        }

                        if (s.lastSolution().getBestInfo() == null)
                            sLogger.error("No solution found :-(");
                        sLogger.debug("Last solution:" + s.lastSolution().getInfo());
                        Solution<CSPVariable, CSPValue> best = s.lastSolution();
                        sLogger.debug("Best solution:" + s.lastSolution().getBestInfo());
                        best.restoreBest();
                        int val = 0;
                        for (CSPValue v: best.getAssignment().assignedValues())
                            val += (int) v.toDouble();
                        int totalVal = val + (best.getModel().unassignedVariables(best.getAssignment()).size() * nrValues);
                        sLogger.debug("Last solution:" + best.getInfo());
                        logStat.println(test
                                + ";"
                                + nrVariables
                                + ";"
                                + nrValues
                                + ";"
                                + sDoubleFormat.format(density)
                                + ";"
                                + sDoubleFormat.format(tightness)
                                + ";"
                                + sDoubleFormat.format(best.getTime())
                                + ";"
                                + best.getIteration()
                                + ";"
                                + sDoubleFormat.format((best.getIteration()) / best.getTime())
                                + ";"
                                + best.getModel().unassignedHardConstraints(best.getAssignment()).size()
                                + ";"
                                + best.getModel().assignedVariables(best.getAssignment()).size()
                                + ";"
                                + sDoubleFormat.format(100.0 * best.getModel().assignedVariables(best.getAssignment()).size()
                                        / best.getModel().variables().size())
                                + (mpp ? ";"
                                        + (best.getModel().perturbVariables(best.getAssignment()).size() + best.getModel()
                                                .unassignedVariables(best.getAssignment()).size())
                                        + ";"
                                        + sDoubleFormat.format(100.0
                                                * (best.getModel().perturbVariables(best.getAssignment()).size() + best.getModel()
                                                        .unassignedVariables(best.getAssignment()).size())
                                                / best.getModel().variables().size()) : "") + ";" + val + ";"
                                + totalVal);
                        log.println("    seed:         " + currentSeed);
                        log.println("    constraints:  " + best.getModel().constraints().size());
                        for (Iterator<Constraint<CSPVariable, CSPValue>> i = best.getModel().constraints().iterator(); i
                                .hasNext();) {
                            CSPBinaryConstraint c = (CSPBinaryConstraint) i.next();
                            log.println("      " + c.getName() + " (" + c.first().getName() + ","
                                    + c.second().getName() + ")");
                            for (CSPValue v0 : c.first().values(best.getAssignment())) {
                                log.print("        ");
                                for (CSPValue v1 : c.second().values(best.getAssignment()))
                                    log.print(c.isConsistent(v0, v1) ? "1 " : "0 ");
                            }
                            log.println();
                        }
                        log.println("    time:         " + sDoubleFormat.format(best.getTime()) + " s");
                        log.println("    iteration:    " + best.getIteration());
                        log.println("    speed:        " + sDoubleFormat.format((best.getIteration()) / best.getTime())
                                + " it/s");
                        log.println("    assigned:     "
                                + best.getModel().assignedVariables(best.getAssignment()).size()
                                + " ("
                                + sDoubleFormat.format(100.0 * best.getModel().assignedVariables(best.getAssignment()).size()
                                        / best.getModel().variables().size()) + "%)");
                        log.println("    total value:  " + val);
                        if (mpp)
                            log.println("    perturbations:"
                                    + (best.getModel().perturbVariables(best.getAssignment()).size() + best.getModel()
                                            .unassignedVariables(best.getAssignment()).size())
                                    + " ("
                                    + sDoubleFormat
                                            .format(100.0
                                                    * (best.getModel().perturbVariables(best.getAssignment()).size() + best.getModel()
                                                            .unassignedVariables(best.getAssignment()).size())
                                                    / best.getModel().variables().size()) + "%)");
                        log.print("    solution:     ");
                        for (CSPVariable v : best.getModel().variables()) {
                            if (v.getBestAssignment() == null)
                                continue;
                            log.print(v.getName() + "=" + v.getBestAssignment().getName());
                            log.print(", ");
                        }
                        log.println();
                        sumTime += best.getTime();
                        sumTime2 += best.getTime() * best.getTime();
                        sumIters += best.getIteration();
                        sumIters2 += best.getIteration() * best.getIteration();
                        sumConfl += best.getModel().unassignedHardConstraints(best.getAssignment()).size();
                        sumAssign += best.getModel().assignedVariables(best.getAssignment()).size();
                        sumAssign2 += best.getModel().assignedVariables(best.getAssignment()).size()
                                * best.getModel().assignedVariables(best.getAssignment()).size();
                        sumVal += val;
                        sumVal2 += val * val;
                        sumTotalVal += totalVal;
                        sumTotalVal2 += totalVal * totalVal;
                        if (mpp) {
                            sumPert += (best.getModel().perturbVariables(best.getAssignment()).size() + best.getModel()
                                    .unassignedVariables(best.getAssignment()).size());
                            sumPert2 += (best.getModel().perturbVariables(best.getAssignment()).size() + best.getModel()
                                    .unassignedVariables(best.getAssignment()).size())
                                    * (best.getModel().perturbVariables(best.getAssignment()).size() + best.getModel()
                                            .unassignedVariables(best.getAssignment()).size());
                        }
                        log.flush();
                        logStat.flush();
                    }
                    logAvgStat.println(nrVariables
                            + ";"
                            + nrValues
                            + ";"
                            + sDoubleFormat.format(density)
                            + ";"
                            + sDoubleFormat.format(tightness)
                            + ";"
                            + sDoubleFormat.format(sumTime / nrTests)
                            + ";"
                            + sDoubleFormat.format(ToolBox.rms(nrTests, sumTime, sumTime2))
                            + ";"
                            + sDoubleFormat.format(((double) sumIters) / nrTests)
                            + ";"
                            + sDoubleFormat.format(ToolBox.rms(nrTests, sumIters, sumIters2))
                            + ";"
                            + sDoubleFormat.format((sumIters) / sumTime)
                            + ";"
                            + sDoubleFormat.format(((double) sumConfl) / nrTests)
                            + ";"
                            + sDoubleFormat.format(((double) sumAssign) / nrTests)
                            + ";"
                            + sDoubleFormat.format(ToolBox.rms(nrTests, sumAssign, sumAssign2))
                            + ";"
                            + sDoubleFormat.format(100.0 * (sumAssign) / (nrVariables * nrTests))
                            + (mpp ? ";" + sDoubleFormat.format(((double) sumPert) / nrTests) + ";"
                                    + sDoubleFormat.format(ToolBox.rms(nrTests, sumPert, sumPert2)) + ";"
                                    + sDoubleFormat.format(100.0 * (sumPert) / (nrVariables * nrTests)) : "")
                            + ";"
                            + sDoubleFormat.format(((double) sumVal) / (nrTests * nrVariables))
                            + ";"
                            + sDoubleFormat.format(ToolBox.rms(nrTests, (double) sumVal / nrVariables, (double) sumVal2
                                    / (nrVariables * nrVariables))) + ";"
                            + sDoubleFormat.format(((double) sumTotalVal) / nrTests) + ";"
                            + sDoubleFormat.format(ToolBox.rms(nrTests, sumTotalVal, sumTotalVal2)));
                    logAvgStat.flush();
                }
            }
        }
        log.flush();
        log.close();
        logStat.flush();
        logStat.close();
        logAvgStat.flush();
        logAvgStat.close();
    }

    private static void test(File inputCfg, String name, String include, String regexp, String outDir) throws Exception {
        if (regexp != null) {
            String incFile;

            if (regexp.indexOf(';') > 0) {
                incFile = regexp.substring(0, regexp.indexOf(';'));
                regexp = regexp.substring(regexp.indexOf(';') + 1);
            } else {
                incFile = regexp;
                regexp = null;
            }
            if (incFile.startsWith("[") && incFile.endsWith("]")) {
                test(inputCfg, name, include, regexp, outDir);
                incFile = incFile.substring(1, incFile.length() - 1);
            }
            if (incFile.indexOf('{') >= 0 && incFile.indexOf('}') >= 0) {
                String prefix = incFile.substring(0, incFile.indexOf('{'));
                StringTokenizer middle = new StringTokenizer(incFile.substring(incFile.indexOf('{') + 1, incFile
                        .indexOf('}')), "|");
                String sufix = incFile.substring(incFile.indexOf('}') + 1);

                while (middle.hasMoreTokens()) {
                    String m = middle.nextToken();

                    test(inputCfg, (name == null ? "" : name + "_") + m, (include == null ? "" : include + ";")
                            + prefix + m + sufix, regexp, outDir);
                }
            } else {
                test(inputCfg, name, (include == null ? "" : include + ";") + incFile, regexp, outDir);
            }
        } else {
            DataProperties properties = ToolBox.loadProperties(inputCfg);
            StringTokenizer inc = new StringTokenizer(include, ";");

            while (inc.hasMoreTokens()) {
                String aFile = inc.nextToken();

                System.out.println("  Loading included file '" + aFile + "' ... ");
                FileInputStream is = null;

                if ((new File(aFile)).exists()) {
                    is = new FileInputStream(aFile);
                }
                if ((new File(inputCfg.getParent() + File.separator + aFile)).exists()) {
                    is = new FileInputStream(inputCfg.getParent() + File.separator + aFile);
                }
                if (is == null) {
                    System.err.println("Unable to find include file '" + aFile + "'.");
                }
                properties.load(is);
                is.close();
            }
            String outDirThisTest = (outDir == null ? properties.getProperty("General.Output", ".") : outDir)
                    + File.separator + name + File.separator + sDateFormat.format(new Date());
            properties.setProperty("General.Output", outDirThisTest.toString());
            System.out.println("Output folder: " + properties.getProperty("General.Output"));
            (new File(outDirThisTest)).mkdirs();
            ToolBox.configureLogging(outDirThisTest, null);
            FileOutputStream fos = new FileOutputStream(outDirThisTest + File.separator + "rcsp.conf");

            properties.store(fos, "Random CSP problem configuration file");
            fos.flush();
            fos.close();
            test(properties);
        }
    }

    public static void main(String[] args) {
        try {
            Progress.getInstance().addProgressListener(new ProgressWriter(System.out));
            File inputCfg = new File(args[0]);
            DataProperties properties = ToolBox.loadProperties(inputCfg);
            if (properties.getProperty("INCLUDE_REGEXP") != null) {
                test(inputCfg, null, null, properties.getProperty("INCLUDE_REGEXP"), (args.length > 1 ? args[1] : null));
            } else {
                String outDir = properties.getProperty("General.Output", ".") + File.separator
                        + inputCfg.getName().substring(0, inputCfg.getName().lastIndexOf('.')) + File.separator
                        + sDateFormat.format(new Date());
                if (args.length > 1)
                    outDir = args[1] + File.separator + (sDateFormat.format(new Date()));
                properties.setProperty("General.Output", outDir.toString());
                System.out.println("Output folder: " + properties.getProperty("General.Output"));
                (new File(outDir)).mkdirs();
                ToolBox.configureLogging(outDir, null);
                test(properties);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
