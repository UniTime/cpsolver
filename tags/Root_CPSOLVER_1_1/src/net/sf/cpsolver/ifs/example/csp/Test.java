package net.sf.cpsolver.ifs.example.csp;


import java.io.*;
import java.util.*;

import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.solution.*;
import net.sf.cpsolver.ifs.solver.*;
import net.sf.cpsolver.ifs.util.*;

/**
 * Test of Structured CSP problems. It takes one argument -- property file with all the parameters.
 * It allows to execute given number of tests. It also allows to define several configurations which will be executed. For instance CSP(20,15,5%..95%,5..95%), 10 runs of each configuration. All such configuration are processed in one run of Test class.
 * <br><br>
 * In Structured CSP, variables are divided into several kernels (some variables may remain ouside kernels). 
 * Different constraints (in density and tightnes) are generated according to whether variables are from the same kernel or not.
 * <br><br>
 * Test's parameters:
 * <br>
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>General.MPP</td><td>{@link String}</td><td>Minimal perturbation problem (if true), this mj. means that initial assignment will be generated</td></tr>
 * <tr><td>CSP.Seed</td><td>{@link Long}</td><td>Random number generator seed, {@link System#currentTimeMillis()} is taken if not present</td></tr>
 * <tr><td>CSP.ForceSolutionExistance</td><td>{@link Boolean}</td><td>If true, generated problem will always have at least one feasible solution</td></tr>
 * <tr><td>CPS.NrTests</td><td>{@link Integer}</td><td>Number of tests (for each input configuration)</td></tr>
 * <tr><td>CSP.NrVariables</td><td>{@link Integer}</td><td>Number of variables</td></tr>
 * <tr><td>CSP.NrVariablesMin<br>CSP.NrVariablesMax<br>CSP.NrVariablesStep</td><td>{@link Integer}</td><td>Range of the number variables (a set of different configurations will be generated)<br>Use either CSP.NrVariables or these CSP.NrVariablesMin, CSP.NrVariablesMax, CSP.NrVariablesStep</td></tr>
 * <tr><td>CSP.DomainSize</td><td>{@link Integer}</td><td>Number of values of every variable</td></tr>
 * <tr><td>CSP.DomainSizeRatio</td><td>{@link Double}</td><td>Number of values as a ration of the number of variables. This way we can model for instance CSP(N,2N,p1,p2) problems with one configuration.<br>Use either CSP.DomainSize or CSP.DomainSizeRatio</td></tr>
 * <tr><td>CSP.Tightness</td><td>{@link Double}</td><td>Tightness of constraints outside kernels</td></tr>
 * <tr><td>CSP.TightnessMin<br>CSP.TightnessMax<br>CSP.TightnessStep</td><td>{@link Double}</td><td>Tightness of constraints outside kernels given as a range -> respective configurations will be generated and tested</td></tr>
 * <tr><td>CSP.Density</td><td>{@link Double}</td><td>Density of constraints outside kernels</td></tr>
 * <tr><td>CSP.DensityMin<br>CSP.DensityMax<br>CSP.DensityStep</td><td>{@link Double}</td><td>Density of constraints outside kernels given as a range -> respective configurations will be generated and tested</td></tr>
 * <tr><td>CSP.NrKernels</td><td>{@link Integer}</td><td>Number of kernels (Structured CSP, use 0 for "normal" CSP)</td></tr>
 * <tr><td>CSP.KernelSize</td><td>{@link Integer}</td><td>Number of variables in each kernel</td></tr>
 * <tr><td>CSP.KernelTightness</td><td>{@link Double}</td><td>Tightness of constraints inside a kernel</td></tr>
 * <tr><td>CSP.KernelDensity</td><td>{@link Double}</td><td>Density of constraints inside a kernel</td></tr>
 * <tr><td>CSP.SameProblemEachStep</td><td>{@link Boolean}</td><td>If true, each configuration will start with the same seed</td></tr>
 * <tr><td>CSP.SameProblemEachTest</td><td>{@link Boolean}</td><td>If true, each test of the same configuration will start with the same seed</td></tr>
 * <tr><td>General.Output</td><td>{@link String}</td><td>Output folder where a log file and tables with results. In order not to overwrite the results if executed more than once, a subfolder with the name taken from current date and time will be created in this folder and all results will go to this subfolder.</td></tr>
 * </table>
 * <br><br>
 * Also, the configuration file can consist only from one parameter (named INCLUDE_REGEXP) which is processed as a regular expression of semicolon separated list of property files, for instance<ul>
 * <code>INCLUDE_REGEXP=general.ini;{CSP(50,12,250,p2)|CSP(25,15,198,p2)}.ini;{std|opt}.ini;{10x1min}.ini;{cbs|rw1|tabu20}.ini</code><br>
 * </ul>where {a|b|c|...} means a selection of a, b, c, .. All possible combinations are taken and for each of them an input configuration is combined from the relevant files. So, for instance, the above example will result into the following configurations:<ul>
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
 * </ul>To be able to distinguish such configuration a subfolder in General.Output folder is created, its name is combined from the names which are in parenthesis.
 * So, for instance the first bunch of tests will output into the folder:<ul>
 * ${General.Output}\CSP(50,12,250,p2)_std_10x1min_csb\25-Feb-05_191136
 * </ul>If one parameter is defined in more than one configuration files (e.g. in general.ini as well as cbs.ini) the one from the file more on the right is taken.
 * <br><br>
 * An example of the configurations:<br>
 * File<b> general.ini</b><ul><code>
 * #Default settings common for all configurations<br>
 * General.MPP=false<br>
 * General.InitialAssignment=false<br>
 * General.Output=output\\RandomCSP\\IFS<br>
 * <br>
 * #Value selection heuristics<br>
 * Value.Class=net.sf.cpsolver.ifs.heuristics.GeneralValueSelection<br>
 * Value.WeightWeightedConflicts=0.0<br>
 * Value.RandomWalkProb=0.0<br>
 * Value.WeightConflicts=1.0<br>
 * Value.WeightNrAssignments=0.0<br>
 * Value.WeightValue=0.0<br>
 * Value.Tabu=0<br>
 * <br>
 * #Variable selection heuristics<br>
 * Variable.Class=net.sf.cpsolver.ifs.heuristics.GeneralVariableSelection<br>
 * Variable.RandomSelection=true<br>
 * <br>
 * #Termination condition<br>
 * Termination.Class=net.sf.cpsolver.ifs.termination.GeneralTerminationCondition<br>
 * Termination.MaxIters=-1<br>
 * Termination.TimeOut=-1<br>
 * Termination.StopWhenComplete=true<br>
 * <br>
 * #Solution comparator<br>
 * Comparator.Class=net.sf.cpsolver.ifs.solution.GeneralSolutionComparator<br>
 * </code></ul><br>
 * File<b> CSP(50,12,250,p2).ini</b><ul><code>
 * #Sparse problem CSP(50,12,250/1225,p2)<br>
 * CSP.NrVariables=50<br>
 * CSP.DomainSize=12<br>
 * CSP.Density=0.2<br>
 * CSP.TightnessMin=0.10<br>
 * CSP.TightnessMax=0.95<br>
 * CSP.TightnessStep=0.02<br>
 * <br> 
 * CSP.Seed=780921<br>
 * <br>
 * CSP.ForceSolutionExistance=false<br>
 * CSP.SameProblemEachStep=false<br>
 * CSP.SameProblemEachTest=false<br>
 * <br>
 * CSP.NrKernels=0<br>
 * </code></ul><br>
 * File<b> std.ini</b><ul><code>
 * #Standard problem<br>
 * CSP.ForceSolutionExistance=false<br>
 * </code></ul><br>
 * File<b> opt.ini</b><ul><code>
 * #Optimization problem (minCSP)<br>
 * #Value selection: use weigh of a conflict, but when there are more than one value<br>
 * #        with the same number of conflicts, use the one with lower value<br>
 * Value.WeightValue=0.0001<br>
 * Value.WeightConflicts=1.0<br>
 * #Do not stop when a complete solution is found<br>
 * Termination.StopWhenComplete=false<br>
 * </code></ul><br>
 * File<b> 10x1min.ini</b><ul><code>
 * #For each configuration, execute 10 tests, each with 1 minute timeout<br>
 * CPS.NrTests=10<br>
 * Termination.TimeOut=60<br>
 * </code></ul><br>
 * File<b> cbs.ini</b><ul><code>
 * #Use conflict-based statistics<br>
 * Extensions.Classes=net.sf.cpsolver.ifs.extension.ConflictStatistics<br>
 * Value.WeightWeightedConflicts=1.0<br>
 * </code></ul><br>
 * File<b> tabu20.ini</b><ul><code>
 * #Use tabu-list of the length 20<br>
 * Value.Tabu=20<br>
 * </code></ul><br>
 * File<b> rw1.ini</b><ul><code>
 * #Use 1% random walk selection<br>
 * Value.RandomWalkProb=0.01<br>
 * </code></ul><br>
 *
 * @see StructuredCSPModel
 * @see net.sf.cpsolver.ifs.extension.ConflictStatistics
 * @see net.sf.cpsolver.ifs.heuristics.GeneralValueSelection
 * @see net.sf.cpsolver.ifs.heuristics.GeneralVariableSelection
 * @see net.sf.cpsolver.ifs.termination.GeneralTerminationCondition
 * @see net.sf.cpsolver.ifs.solution.GeneralSolutionComparator
 *
 * @version
 * IFS 1.1 (Iterative Forward Search)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
public class Test {
    private static java.text.DecimalFormat sDoubleFormat = new java.text.DecimalFormat("0.000", new java.text.DecimalFormatSymbols(Locale.US));
    private static java.text.SimpleDateFormat sDateFormat = new java.text.SimpleDateFormat("dd-MMM-yy_HHmmss", java.util.Locale.US);
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(Test.class);
    
    private static void test(DataProperties properties) throws Exception {
        boolean sameProblemStep = properties.getPropertyBoolean("CSP.SameProblemEachStep", false);
        boolean sameProblemTest = properties.getPropertyBoolean("CSP.SameProblemEachTest", false);
        int nrVars = properties.getPropertyInt("CSP.NrVariables",20);
        int nrKernels = properties.getPropertyInt("CSP.NrKernels", 2);
        int nrKernelVariables = properties.getPropertyInt("CSP.KernelSize", 8);
        int nrVariablesMin = properties.getPropertyInt("CSP.NrVariablesMin",nrVars);
        int nrVariablesMax = properties.getPropertyInt("CSP.NrVariablesMax",nrVars);
        int nrVariablesStep = properties.getPropertyInt("CSP.NrVariablesStep",1);
        int nrValues = properties.getPropertyInt("CSP.DomainSize",10);
        double nrValuesRatio = properties.getPropertyDouble("CSP.DomainSizeRatio",-1);
        float kernelTightness = properties.getPropertyFloat("CSP.KernelTightness", 0.097f);
        float kernelDensity = properties.getPropertyFloat("CSP.KernelDensity", 0.097f);
        float tightnessInit = properties.getPropertyFloat( "CSP.Tightness", 0.4f);
        float tightnessMin = properties.getPropertyFloat( "CSP.TightnessMin", tightnessInit);
        float tightnessMax = properties.getPropertyFloat( "CSP.TightnessMax", tightnessInit)+1e-6f;
        float tightnessStep = properties.getPropertyFloat( "CSP.TightnessStep", 0.1f);
        float densityInit = properties.getPropertyFloat( "CSP.Density", 0.4f);
        float densityMin = properties.getPropertyFloat( "CSP.DensityMin", densityInit);
        float densityMax = properties.getPropertyFloat( "CSP.DensityMax", densityInit)+1e-6f;
        float densityStep = properties.getPropertyFloat( "CSP.DensityStep", 0.1f);
        long seed = properties.getPropertyLong( "CSP.Seed", System.currentTimeMillis());
        int nrTests = properties.getPropertyInt("CPS.NrTests",10);
        boolean mpp = properties.getPropertyBoolean("General.MPP", false);
        PrintWriter logStat = new PrintWriter(new FileWriter(properties.getProperty("General.Output")+File.separator+"rcsp_"+nrVariablesMin+"_"+nrValues+".csv"));
        PrintWriter logAvgStat = new PrintWriter(new FileWriter(properties.getProperty("General.Output")+File.separator+"avg_stat.csv"));
        PrintWriter log = new PrintWriter(new FileWriter(properties.getProperty("General.Output")+File.separator+"info.txt"));
        logStat.println("testNr;nrVars;nrVals;density[%];tightness[%];time[s];iters;speed[it/s];unassConstr;assigned;assigned[%]"+(mpp?";perts;perts[%]":"")+";value;totalValue");
        logAvgStat.println("nrVars;nrVals;density[%];tightness[%];time[s];RMStime[s];iters;RMSiters;speed[it/s];unassConst;assigned;RMSassigned;assigned[%]"+(mpp?";perts;RMSperts;perts[%]":"")+";value;RMSvalue;totalValue;RMStotalValue");
        System.out.println("Number of variables: "+nrVariablesMin+" .. "+nrVariablesMax+"  (step="+nrVariablesStep+")");
        System.out.println("Density:             "+densityMin+" .. "+densityMax+"  (step="+densityStep+")");
        System.out.println("Tightness:           "+tightnessMin+" .. "+tightnessMax+"  (step="+tightnessStep+")");
        for (int nrVariables=nrVariablesMin;nrVariables<=nrVariablesMax;nrVariables+=nrVariablesStep) {
            if (nrValuesRatio>0.0) nrValues = (int)Math.round(nrValuesRatio*nrVariables);
            for (float density=densityMin;density<=densityMax;density+=densityStep) {
                for (float tightness=tightnessMin;tightness<=tightnessMax;tightness+=tightnessStep) {
                    log.println("CSP{#Var="+nrVariables+", #Val="+nrValues+", P(density)="+sDoubleFormat.format(100.0*density)+"%, P(tighness)="+sDoubleFormat.format(100.0*tightness)+", "+nrKernels+"x Kernel{#Var="+nrKernelVariables+", P(density)="+sDoubleFormat.format(100.0*kernelDensity)+"%, P(tighness)="+sDoubleFormat.format(100.0*kernelTightness)+"%}}");
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
                    double sumAssignPer = 0;
                    double sumPertPer = 0;
                    for (int test=1;test<=nrTests;test++) {
                        log.println("  "+test+". test");
                        log.flush();
                        properties.setProperty("CSP.NrVariables", String.valueOf(nrVariables));
                        properties.setProperty("CSP.Tightness", String.valueOf(tightness));
                        properties.setProperty("CSP.Density", String.valueOf(density));
                        
                        long currentSeed = (seed*1000000L) + (1000 * (long)((sameProblemStep?densityMin:density)*1000.0)) + ((long)((sameProblemStep?tightnessMin:tightness)*1000.0));
                        currentSeed = (currentSeed*nrTests) + (sameProblemTest?0:test-1);

                        sLogger.debug("Seed: "+currentSeed);
                        StructuredCSPModel csp = new StructuredCSPModel(properties, currentSeed);
                        
                        Solver s = new Solver(properties);
                        s.setInitalSolution(csp);
                        s.currentSolution().clearBest();
                        s.start();

                        try {
                            s.getSolverThread().join();
                        } catch (NullPointerException npe) {}
                        
                        if (s.lastSolution().getBestInfo()==null) sLogger.error("No solution found :-(");
                        sLogger.debug("Last solution:"+s.lastSolution().getInfo());
                        Solution best = s.lastSolution();
                        sLogger.debug("Best solution:"+s.lastSolution().getBestInfo());
                        best.restoreBest();
                        int val = 0;
                        for (Enumeration iv = best.getModel().assignedVariables().elements(); iv.hasMoreElements();)
                            val += (int)((Variable)iv.nextElement()).getAssignment().toDouble();
                        int totalVal = val + (best.getModel().unassignedVariables().size()*nrValues);
                        sLogger.debug("Last solution:"+best.getInfo());
                        logStat.println(test+";"+nrVariables+";"+nrValues+";"+sDoubleFormat.format(density)+";"+sDoubleFormat.format(tightness)+";"+ sDoubleFormat.format(best.getTime())+";"+best.getIteration()+";"+sDoubleFormat.format(((double)best.getIteration())/best.getTime())+";"+best.getModel().unassignedHardConstraints().size()+";"+best.getModel().assignedVariables().size()+";"+sDoubleFormat.format(100.0 * best.getModel().assignedVariables().size() / best.getModel().variables().size())+ (mpp?";"+(best.getModel().perturbVariables().size()+best.getModel().unassignedVariables().size())+";"+sDoubleFormat.format(100.0 * (best.getModel().perturbVariables().size()+best.getModel().unassignedVariables().size()) / best.getModel().variables().size()):"")+";"+val+";"+totalVal);
                        log.println("    seed:         "+currentSeed);
                        log.println("    constraints:  "+best.getModel().constraints().size());
                        for (Enumeration i=best.getModel().constraints().elements();i.hasMoreElements();) {
                            CSPBinaryConstraint c = (CSPBinaryConstraint)i.nextElement();
                            log.println("      "+c.getName()+" ("+c.first().getName()+","+c.second().getName()+")");
                            for (Enumeration a=c.first().values().elements();a.hasMoreElements();) {
                                Value v0 = (Value)a.nextElement();
                                log.print("        ");
                                for (Enumeration b=c.second().values().elements();b.hasMoreElements();) {
                                    Value v1 = (Value)b.nextElement();
                                    log.print(c.isConsistent(v0,v1)?"1 ":"0 ");
                        	}
                        	log.println();
                            }
                        }
                        log.println("    time:         "+sDoubleFormat.format(best.getTime())+" s");
                        log.println("    iteration:    "+best.getIteration());
                        log.println("    speed:        "+sDoubleFormat.format(((double)best.getIteration())/best.getTime())+" it/s");
                        log.println("    assigned:     "+best.getModel().assignedVariables().size()+" ("+sDoubleFormat.format(100.0 * best.getModel().assignedVariables().size() / best.getModel().variables().size())+"%)");
                        log.println("    total value:  "+val);
                        if (mpp) log.println("    perturbations:"+(best.getModel().perturbVariables().size()+best.getModel().unassignedVariables().size())+" ("+sDoubleFormat.format(100.0 * (best.getModel().perturbVariables().size()+best.getModel().unassignedVariables().size()) / best.getModel().variables().size())+"%)");
                        log.print("    solution:     ");
                        for (Enumeration i=best.getModel().variables().elements();i.hasMoreElements();) {
                            CSPVariable v = (CSPVariable)i.nextElement();
                            if (v.getBestAssignment()==null) continue;
                            log.print(v.getName()+"="+v.getBestAssignment().getName());
                            if (i.hasMoreElements()) log.print(", ");
                        }
                        log.println();
                        sumTime += best.getTime();
                        sumTime2 += best.getTime()*best.getTime();
                        sumIters += best.getIteration();
                        sumIters2 += best.getIteration()*best.getIteration();
                        sumConfl += best.getModel().unassignedHardConstraints().size();
                        sumAssign += best.getModel().assignedVariables().size();
                        sumAssign2 += best.getModel().assignedVariables().size()*best.getModel().assignedVariables().size();
                        sumAssignPer += 100.0*((double)best.getModel().assignedVariables().size()/((double)best.getModel().variables().size()));
                        sumVal += val;
                        sumVal2 += val * val;
                        sumTotalVal += totalVal;
                        sumTotalVal2 += totalVal * totalVal;
                        if (mpp) {
                            sumPert += (best.getModel().perturbVariables().size()+best.getModel().unassignedVariables().size());
                            sumPert2 += (best.getModel().perturbVariables().size()+best.getModel().unassignedVariables().size())*(best.getModel().perturbVariables().size()+best.getModel().unassignedVariables().size());
                            sumPertPer += 100.0 * (best.getModel().perturbVariables().size()+best.getModel().unassignedVariables().size()) / best.getModel().variables().size();
                        }
                        log.flush();
                        logStat.flush();
                    }
                    logAvgStat.println(nrVariables+";"+nrValues+";"+sDoubleFormat.format(density)+";"+sDoubleFormat.format(tightness)+";"+
                    sDoubleFormat.format(sumTime/nrTests)+";"+
                    sDoubleFormat.format(ToolBox.rms(nrTests,sumTime,sumTime2))+";"+
                    sDoubleFormat.format(((double)sumIters)/nrTests)+";"+
                    sDoubleFormat.format(ToolBox.rms(nrTests,(double)sumIters,(double)sumIters2))+";"+
                    sDoubleFormat.format(((double)sumIters)/sumTime)+";"+
                    sDoubleFormat.format(((double)sumConfl)/nrTests)+";"+
                    sDoubleFormat.format(((double)sumAssign)/nrTests)+";"+
                    sDoubleFormat.format(ToolBox.rms(nrTests,(double)sumAssign,(double)sumAssign2))+";"+
                    sDoubleFormat.format(100.0*((double)sumAssign)/(nrVariables*nrTests))+
                    (mpp?";"+
                    sDoubleFormat.format(((double)sumPert)/nrTests)+";"+
                    sDoubleFormat.format(ToolBox.rms(nrTests,(double)sumPert,(double)sumPert2))+";"+
                    sDoubleFormat.format(100.0*((double)sumPert)/(nrVariables*nrTests))
                    :"")+";"+
                    sDoubleFormat.format(((double)sumVal)/(nrTests*nrVariables))+";"+
                    sDoubleFormat.format(ToolBox.rms(nrTests,(double)sumVal/nrVariables,(double)sumVal2/(nrVariables*nrVariables)))+";"+
                    sDoubleFormat.format(((double)sumTotalVal)/nrTests)+";"+
                    sDoubleFormat.format(ToolBox.rms(nrTests,(double)sumTotalVal,(double)sumTotalVal2)));
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
                StringTokenizer middle = new StringTokenizer(incFile.substring(incFile.indexOf('{')+1,incFile.indexOf('}')),"|");
                String sufix = incFile.substring(incFile.indexOf('}') + 1);

                while (middle.hasMoreTokens()) {
                    String m = middle.nextToken();

                    test(inputCfg, (name==null?"":name+"_")+m, (include==null?"":include+";")+prefix+m+sufix, regexp, outDir);
                }
            } else {
                test(inputCfg, name, (include == null ? "" : include + ";") + incFile, regexp, outDir);
            }
        } else {
            DataProperties properties = ToolBox.loadProperties(inputCfg);
            StringTokenizer inc = new StringTokenizer(include, ";");

            while (inc.hasMoreTokens()) {
                String aFile = inc.nextToken();

                System.out.println("  Loading included file '" + aFile+ "' ... ");
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
            String outDirThisTest = (outDir==null?properties.getProperty("General.Output","."):outDir)+File.separator + name+File.separator+sDateFormat.format(new Date());
            properties.setProperty("General.Output", outDirThisTest.toString());
            System.out.println("Output folder: "+properties.getProperty("General.Output"));
            (new File(outDirThisTest)).mkdirs();
            ToolBox.configureLogging(outDirThisTest, null);
            FileOutputStream fos = new FileOutputStream(outDirThisTest + File.separator + "rcsp.conf");

            properties.store(fos, "Random CSP problem configuration file");
            fos.flush(); fos.close();
            test(properties);
        }
    }
    
    public static void main(String[] args) {
        try {
            Progress.getInstance().addProgressListener(new ProgressWriter(System.out));
            File inputCfg = new File(args[0]);
            DataProperties properties = ToolBox.loadProperties(inputCfg);
            if (properties.getProperty("INCLUDE_REGEXP") != null) {
                test(inputCfg, null, null, properties.getProperty("INCLUDE_REGEXP"), (args.length>1?args[1]:null));
            } else {
                String outDir = properties.getProperty("General.Output", ".") + File.separator + inputCfg.getName().substring(0, inputCfg.getName().lastIndexOf('.')) + File.separator + sDateFormat.format(new Date());
                if (args.length>1)
                    outDir = args[1]+File.separator+(sDateFormat.format(new Date()));
                properties.setProperty("General.Output", outDir.toString());
                System.out.println("Output folder: "+properties.getProperty("General.Output"));
                (new File(outDir)).mkdirs();
                ToolBox.configureLogging(outDir, null);
                test(properties);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
