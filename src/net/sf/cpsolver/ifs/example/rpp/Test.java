package net.sf.cpsolver.ifs.example.rpp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.JProf;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ProgressWriter;
import net.sf.cpsolver.ifs.util.PrologFile;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * RPP test. It takes one argument -- property file with all the parameters. It
 * allows to execute given number of tests. The problem is loaded from
 * prolog-based text files. For description of RPP problem see {@link RPPModel}. <br>
 * <br>
 * Description of the input problem files can be found at <a href='http://www.fi.muni.cz/~hanka/rpp/instances.html'>http://www.fi.muni.cz/~hanka/rpp/instances.html</a>.
 * Each input problem (e.g., gen22.pl) has the following structure:
 * <ul>
 * <code>
 * objects([<br>
 * &nbsp;&nbsp;object(<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;name( rect1 ),<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;size( [ 2, 1 ] ),<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;valid_positions( [ 0-38, 0-13 ] )<br>
 * &nbsp;&nbsp;),<br>
 * &nbsp;&nbsp;object(<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;name( rect2 ),<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;size( [ 2, 1 ] ),<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;valid_positions( [ 0-38, 0-13 ] )<br>
 * &nbsp;&nbsp;), <br>
 * ... <br>
 * &nbsp;&nbsp;object(<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;name( rect200 ),<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;size( [ 2, 1 ] ),<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;valid_positions( [ 0-38, 7-13 ] )<br>
 * &nbsp;&nbsp;)<br>
 * ] ). <br>
 * </code>
 * </ul>
 * Stating that the first rectangle (named rect1) has size 2x1 and its valid
 * position are with x between 0 and 38, y between 0 and 13, and so on. <br>
 * MPP instances contain an extra file with the solution (e.g., gen22.solution),
 * with the following structure
 * <ul>
 * <code>
 * assigned([[rect1X,[17]], [rect1Y,[5]], [rect2X,[24]], [rect2Y,[4]], ... [rect200X,[37]], [rect200Y,[10]]]).
 * </code>
 * </ul>
 * Which means that the first rectangle (named rect1) is to be placed at [17,5],
 * second at [24,4] and so on. <br>
 * There is also a file (e.g., gen22.mpp) describing which input placements are
 * to be prohibited (not that if the input placement is prohibited, it means
 * that also all values with the same X or Y coordinate are prohibited). It has
 * the following structure:
 * <ul>
 * <code>
 * perturbation( 1, 0, [] ).<br>
 * perturbation( 2, 0, [] ).<br> 
 * ...<br>
 * perturbation( 1, 2, [44,127] ).<br>
 * perturbation( 2, 2, [80,153] ).<br>
 * ...<br>
 * perturbation( 1, 4, [44,80,127,153] ).<br>
 * perturbation( 2, 4, [48,67,138,170] ). <br>
 * ...<br>
 * </code>
 * </ul>
 * Stating that for instance in the first test with 4 perturbations the
 * rectangles rect44, rect80, rect127 and rect153 will have their initial value
 * prohibited. <br>
 * <br>
 * Test's parameters: <br>
 * <table border='1'>
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
 * <td>RPP.NrTests</td>
 * <td>{@link Integer}</td>
 * <td>Number of tests to be executed for each input instance</td>
 * </tr>
 * <tr>
 * <td>Rpp.Min<br>
 * Rpp.Max<br>
 * Rpp.Step</td>
 * <td>{@link Integer}</td>
 * <td>In case of MPP: minimal, maximal number and increment of input
 * perturbations. An instance is loaded and tested for each given number of
 * input perturbations.</td>
 * </tr>
 * <tr>
 * <td>Rpp.Min<br>
 * Rpp.Max</td>
 * <td>{@link Integer}</td>
 * <td>In case of initial problem: minimal and maximal number of the input
 * problem. An instance is loaded and tested for each given number from RPP.Min
 * to RPP.Max.</td>
 * </tr>
 * <tr>
 * <td>Rpp.ProblemWidth<br>
 * Rpp.ProblemHeight</td>
 * <td>{@link Integer}</td>
 * <td>Width and height of the placement area.</td>
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
 * <ul>
 * <code>INCLUDE_REGEXP=general.ini;{rpp85|rpp90|rpp95|mpp22}.ini;{5min}.ini;{cbs|rw1|tabu20}.ini</code>
 * <br>
 * </ul>
 * where {a|b|c|...} means a selection of a, b, c, .. All possible combinations
 * are taken and for each of them an input configuration is combined from the
 * relevant files. So, for instance, the above example will result into the
 * following configurations:
 * <ul>
 * <li>general.ini;rpp85.ini;5min.ini;cbs.ini
 * <li>general.ini;rpp85.ini;5min.ini;rw1.ini
 * <li>general.ini;rpp85.ini;5min.ini;tabu20.ini
 * <li>general.ini;rpp90.ini;5min.ini;cbs.ini
 * <li>general.ini;rpp90.ini;5min.ini;rw1.ini
 * <li>general.ini;rpp90.ini;5min.ini;tabu20.ini
 * <li>general.ini;rpp95.ini;5min.ini;cbs.ini
 * <li>general.ini;rpp95.ini;5min.ini;rw1.ini
 * <li>general.ini;rpp95.ini;5min.ini;tabu20.ini
 * <li>general.ini;mpp22.ini;5min.ini;cbs.ini
 * <li>general.ini;mpp22.ini;5min.ini;rw1.ini
 * <li>general.ini;mpp22.ini;5min.ini;tabu20.ini
 * </ul>
 * To be able to distinguish such configuration a subfolder in General.Output
 * folder is created, its name is combined from the names which are in
 * parenthesis. So, for instance the first bunch of tests will output into the
 * folder:
 * <ul>
 * ${General.Output}\rpp85_5min_csb\25-Feb-05_191136
 * </ul>
 * If one parameter is defined in more than one configuration files (e.g. in
 * general.ini as well as cbs.ini) the one from the file more on the right is
 * taken. <br>
 * <br>
 * An example of the configurations:<br>
 * File<b> general.ini</b>
 * <ul>
 * <code>
 * #Default settings common for all configurations<br>
 * General.MPP=false<br>
 * General.InitialAssignment=false<br>
 * General.Output=output\\RPP\\IFS<br>
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
 * </code>
 * </ul>
 * <br>
 * File<b> rpp80.ini</b>
 * <ul>
 * <code>
 * #RPP instances with 200 objects and the placement area filled to 80% in average<br>
 * General.Input=input\\rpp\\80<br>
 * Rpp.ProblemWidth=40<br>
 * Rpp.ProblemHeight=14<br>
 * #Use 10 problem instances (this means problem files gen1.pl, gen2.pl,... gen10.pl will be taken), each run 10 times<br>
 * Rpp.Min=1<br>
 * Rpp.Max=10<br>
 * Rpp.NrTests=10<br>
 * </code>
 * </ul>
 * <br>
 * File<b> mpp22.ini</b>
 * <ul>
 * <code>
 * #RPP MPP instance 22 (with 200 objects and the placement area filled to 80% in average)<br>
 * #  files gen22.pl (input problem), gen22.solution (initial solution) and gen22.mpp (input perturbations) are to be taken<br>
 * General.Input=input\\rpp-mpp\\gen22<br>
 * Rpp.ProblemWidth=40<br>
 * Rpp.ProblemHeight=14<br>
 * # 0, 4, 8, .. 200 input perturbations to be used<br>
 * Rpp.Min=0<br>
 * Rpp.Max=200<br>
 * Rpp.Step=4<br>
 * </code>
 * </ul>
 * <br>
 * File<b> 5min.ini</b>
 * <ul>
 * <code>
 * #5 minute time limit for each run<br>
 * Termination.TimeOut=300<br>
 * </code>
 * </ul>
 * <br>
 * File<b> cbs.ini</b>
 * <ul>
 * <code>
 * #Use conflict-based statistics<br>
 * Extensions.Classes=net.sf.cpsolver.ifs.extension.ConflictStatistics<br>
 * Value.WeightWeightedConflicts=1.0<br>
 * </code>
 * </ul>
 * <br>
 * File<b> tabu20.ini</b>
 * <ul>
 * <code>
 * #Use tabu-list of the length 20<br>
 * Value.Tabu=20<br>
 * </code>
 * </ul>
 * <br>
 * File<b> rw1.ini</b>
 * <ul>
 * <code>
 * #Use 1% random walk selection<br>
 * Value.RandomWalkProb=0.01<br>
 * </code>
 * </ul>
 * <br>
 * 
 * @see RPPModel
 * @see net.sf.cpsolver.ifs.extension.ConflictStatistics
 * @see net.sf.cpsolver.ifs.heuristics.GeneralValueSelection
 * @see net.sf.cpsolver.ifs.heuristics.GeneralVariableSelection
 * @see net.sf.cpsolver.ifs.termination.GeneralTerminationCondition
 * @see net.sf.cpsolver.ifs.solution.GeneralSolutionComparator
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
public class Test {
    private static java.text.DecimalFormat sDoubleFormat = new java.text.DecimalFormat("0.00",
            new java.text.DecimalFormatSymbols(Locale.US));
    private static java.text.SimpleDateFormat sDateFormat = new java.text.SimpleDateFormat("dd-MMM-yy_HHmmss",
            java.util.Locale.US);
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(Test.class);

    private static RPPModel loadModel(int mWidth, int mHeight, List<PrologFile.Term> objects,
            List<PrologFile.Term> assigned, List<PrologFile.Term> perturbations, int perb, int test) {
        try {
            sLogger.debug("Loading model " + perb + "." + test + " ...");
            double startTime = JProf.currentTimeSec();
            RPPModel m = new RPPModel();
            ResourceConstraint c = new ResourceConstraint(mWidth, mHeight);
            m.addConstraint(c);
            for (PrologFile.Term object : objects.get(0).getContent().get(0).getContent()) {
                String name = object.elementAt(0).elementAt(0).getText();
                int width = object.elementAt(1).elementAt(0).elementAt(0).toInt();
                int height = object.elementAt(1).elementAt(0).elementAt(1).toInt();
                String xpos = object.elementAt(2).elementAt(0).elementAt(0).getText();
                String ypos = object.elementAt(2).elementAt(0).elementAt(1).getText();
                int xmin = Integer.parseInt(xpos.substring(0, xpos.indexOf('-')));
                int xmax = Integer.parseInt(xpos.substring(xpos.indexOf('-') + 1));
                int ymin = Integer.parseInt(ypos.substring(0, ypos.indexOf('-')));
                int ymax = Integer.parseInt(ypos.substring(ypos.indexOf('-') + 1));
                Rectangle r = new Rectangle(name, width, height, xmin, xmax, ymin, ymax, null);
                m.addVariable(r);
                c.addVariable(r);
            }
            for (Iterator<PrologFile.Term> i = assigned.get(0).elementAt(0).getContent().iterator(); i.hasNext();) {
                PrologFile.Term assignment = i.next();
                String name = assignment.elementAt(0).getText();
                name = name.substring(0, name.length() - 1);
                int x = assignment.elementAt(1).elementAt(0).toInt();
                assignment = i.next();
                int y = assignment.elementAt(1).elementAt(0).toInt();
                m.getRectangle(name).setInitialAssignment(new Location(m.getRectangle(name), x, y));
            }
            for (PrologFile.Term pert : perturbations) {
                if (test == pert.elementAt(0).toInt() && perb == pert.elementAt(1).toInt() && perb > 0) {
                    for (PrologFile.Term t : pert.elementAt(2).getContent()) {
                        int rec = t.toInt();
                        m.getRectangle("rect" + rec).setProhibited();
                    }
                }
            }
            sLogger.debug("Loaded in " + sDoubleFormat.format(JProf.currentTimeSec() - startTime) + " sec.");
            return m;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static RPPModel loadModel(int mWidth, int mHeight, List<PrologFile.Term> objects) {
        try {
            sLogger.debug("Loading model ...");
            double startTime = JProf.currentTimeSec();
            RPPModel m = new RPPModel();
            ResourceConstraint c = new ResourceConstraint(mWidth, mHeight);
            m.addConstraint(c);
            for (PrologFile.Term object : objects.get(0).getContent().get(0).getContent()) {
                String name = object.elementAt(0).elementAt(0).getText();
                int width = object.elementAt(1).elementAt(0).elementAt(0).toInt();
                int height = object.elementAt(1).elementAt(0).elementAt(1).toInt();
                String xpos = object.elementAt(2).elementAt(0).elementAt(0).getText();
                String ypos = object.elementAt(2).elementAt(0).elementAt(1).getText();
                int xmin = Integer.parseInt(xpos.substring(0, xpos.indexOf('-')));
                int xmax = Integer.parseInt(xpos.substring(xpos.indexOf('-') + 1));
                int ymin = Integer.parseInt(ypos.substring(0, ypos.indexOf('-')));
                int ymax = Integer.parseInt(ypos.substring(ypos.indexOf('-') + 1));
                Rectangle r = new Rectangle(name, width, height, xmin, xmax, ymin, ymax, null);
                m.addVariable(r);
                c.addVariable(r);
            }
            sLogger.debug("Loaded in " + sDoubleFormat.format(JProf.currentTimeSec() - startTime) + " sec.");
            return m;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void testMPP(DataProperties properties) {
        try {
            FileInputStream fis = new FileInputStream(properties.getProperty("General.Input") + ".pl");
            List<PrologFile.Term> v1 = PrologFile.readTermsFromStream(fis, "objects");
            fis.close();
            fis = new FileInputStream(properties.getProperty("General.Input") + ".solution");
            List<PrologFile.Term> v2 = PrologFile.readTermsFromStream(fis, "assigned");
            fis.close();
            fis = new FileInputStream(properties.getProperty("General.Input") + ".mpp");
            List<PrologFile.Term> v3 = PrologFile.readTermsFromStream(fis, "perturbation");
            fis.close();

            PrintWriter res = new PrintWriter(new FileWriter(properties.getProperty("General.Output") + File.separator
                    + "result.pl"));
            PrintWriter stat = new PrintWriter(new FileWriter(properties.getProperty("General.Output") + File.separator
                    + "stat.pl"));
            PrintWriter txt = new PrintWriter(new FileWriter(properties.getProperty("General.Output") + File.separator
                    + "stat.csv"));
            txt.println("pert;time[s];timeRMS;assigned;assignedRMS;perturbations;perturbationsRMS;iters;itersRMS");
            java.text.DecimalFormat nf = new java.text.DecimalFormat("0.000", new java.text.DecimalFormatSymbols(
                    Locale.US));
            int size = -1; // gen80_22_initial().getRectangles().size();
            int tests = properties.getPropertyInt("Rpp.NrTests", 10);
            int step = properties.getPropertyInt("Rpp.Step", 4);
            int min = properties.getPropertyInt("Rpp.Min", 0);
            int max = properties.getPropertyInt("Rpp.Max", -1);
            for (int i = min; size == -1 || i <= (max > 0 ? Math.min(max, size) : size); i += step) {
                double time = 0;
                long assigned = 0;
                long perturbation = 0;
                long iters = 0;
                double time2 = 0;
                long assigned2 = 0;
                long perturbation2 = 0;
                long iters2 = 0;
                for (int t = 1; t <= tests; t++) {
                    RPPModel m = loadModel(properties.getPropertyInt("Rpp.ProblemWidth", 40), properties
                            .getPropertyInt("Rpp.ProblemHeight", 14), v1, v2, v3, i, t);
                    if (size < 0)
                        size = m.variables().size();
                    Solver<Rectangle, Location> s = new Solver<Rectangle, Location>(properties);
                    s.setInitalSolution(m);
                    s.start();
                    s.getSolverThread().join();
                    Solution<Rectangle, Location> best = s.currentSolution();
                    best.restoreBest();
                    res.println("result(" + t + "," + i + "," + nf.format(best.getBestTime()) + ","
                            + (best.getModel().variables().size() - best.getModel().getBestUnassignedVariables()) + ","
                            + best.getBestIteration() + ",");
                    Collection<Rectangle> notPlaced = best.getModel().bestUnassignedVariables();
                    if (notPlaced == null)
                        notPlaced = new ArrayList<Rectangle>();
                    res.print("  unassigned(" + (2 * notPlaced.size()) + "/[");
                    for (Iterator<Rectangle> it = notPlaced.iterator(); it.hasNext();) {
                        Rectangle rect = it.next();
                        res.print(rect.getName() + "X," + rect.getName() + "Y" + (it.hasNext() ? "," : ""));
                    }
                    res.println("]),");
                    StringBuffer sb = new StringBuffer();
                    int perts = 0;
                    for (Rectangle rect : ((RPPModel) best.getModel()).variables()) {
                        if (rect.getBestAssignment() != null
                                && (rect.getInitialAssignment() == null || !rect.getBestAssignment().equals(
                                        rect.getInitialAssignment()))) {
                            sb.append(sb.length() == 0 ? "" : ",");
                            sb.append(rect.getName() + "X-" + (rect.getBestAssignment()).getX());
                            sb.append(sb.length() == 0 ? "" : ",");
                            sb.append(rect.getName() + "Y-" + (rect.getBestAssignment()).getY());
                            perts++;
                        }
                        if (rect.getBestAssignment() == null) {
                            perts++;
                        }
                    }
                    res.println("  perturbations(" + (2 * perts) + "/[" + sb + "])");
                    res.println(").");
                    res.flush();
                    iters += best.getBestIteration();
                    iters2 += (best.getBestIteration() * best.getBestIteration());
                    time += best.getBestTime();
                    time2 += (best.getBestTime() * best.getBestTime());
                    assigned += (best.getModel().variables().size() - best.getModel().getBestUnassignedVariables());
                    assigned2 += (best.getModel().variables().size() - best.getModel().getBestUnassignedVariables())
                            * (best.getModel().variables().size() - best.getModel().getBestUnassignedVariables());
                    perturbation += perts;
                    perturbation2 += perts * perts;
                }
                txt.println(i + ";" + nf.format(time / tests) + ";" + nf.format(ToolBox.rms(tests, time, time2)) + ";"
                        + nf.format(((double) assigned) / tests) + ";"
                        + nf.format(ToolBox.rms(tests, assigned, assigned2)) + ";"
                        + nf.format(((double) perturbation) / tests) + ";"
                        + nf.format(ToolBox.rms(tests, perturbation, perturbation2)) + ";"
                        + nf.format(((double) iters) / tests) + ";" + nf.format(ToolBox.rms(tests, iters, iters2)));
                txt.flush();
                stat.println("averages( initperturbations( " + i + " ), time( " + nf.format(time / tests)
                        + " ), assigned( " + nf.format(((double) assigned) / tests) + " ), perturbations( "
                        + nf.format(((double) perturbation) / tests) + " ) ).");
                stat.println("deviations( initperturbations( " + i + " ), time( "
                        + nf.format(ToolBox.rms(tests, time, time2)) + " ), assigned( "
                        + nf.format(ToolBox.rms(tests, assigned, assigned2)) + " ), perturbations( "
                        + nf.format(ToolBox.rms(tests, perturbation, perturbation2)) + " ) ).");
                stat.flush();
            }
            res.close();
            txt.close();
            stat.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void test(DataProperties properties) {
        try {
            int tests = properties.getPropertyInt("Rpp.NrTests", 10);
            int min = properties.getPropertyInt("Rpp.Min", 0);
            int max = properties.getPropertyInt("Rpp.Max", -1);
            PrintWriter res = new PrintWriter(new FileWriter(properties.getProperty("General.Output") + File.separator
                    + "result.pl"));
            PrintWriter stat = new PrintWriter(new FileWriter(properties.getProperty("General.Output") + File.separator
                    + "stat.pl"));
            PrintWriter txt = new PrintWriter(new FileWriter(properties.getProperty("General.Output") + File.separator
                    + "stat.csv"));
            txt.println("gen;time[s];timeRMS;assigned;assignedRMS;iters;itersRMS");
            java.text.DecimalFormat nf = new java.text.DecimalFormat("0.000", new java.text.DecimalFormatSymbols(
                    Locale.US));
            for (int genNr = min; genNr <= max; genNr++) {
                FileInputStream fis = new FileInputStream(properties.getProperty("General.Input") + File.separator
                        + "gen" + genNr + ".pl");
                List<PrologFile.Term> v1 = PrologFile.readTermsFromStream(fis, "objects");
                fis.close();
                double time = 0;
                long assigned = 0;
                long iters = 0;
                double time2 = 0;
                long assigned2 = 0;
                long iters2 = 0;
                for (int t = 1; t <= tests; t++) {
                    RPPModel m = loadModel(properties.getPropertyInt("Rpp.ProblemWidth", 40), properties
                            .getPropertyInt("Rpp.ProblemHeight", 14), v1);
                    Solver<Rectangle, Location> s = new Solver<Rectangle, Location>(properties);
                    s.setInitalSolution(m);
                    s.start();
                    s.getSolverThread().join();
                    Solution<Rectangle, Location> best = s.currentSolution();
                    best.restoreBest();
                    iters += best.getBestIteration();
                    iters2 += (best.getBestIteration() * best.getBestIteration());
                    time += best.getBestTime();
                    time2 += (best.getBestTime() * best.getBestTime());
                    assigned += (best.getModel().variables().size() - best.getModel().getBestUnassignedVariables());
                    assigned2 += (best.getModel().variables().size() - best.getModel().getBestUnassignedVariables())
                            * (best.getModel().variables().size() - best.getModel().getBestUnassignedVariables());
                    res.println("result(" + genNr + "," + t + "," + nf.format(best.getBestTime()) + ","
                            + (best.getModel().variables().size() - best.getModel().getBestUnassignedVariables()) + ","
                            + best.getBestIteration() + ",");
                    Collection<Rectangle> notPlaced = best.getModel().bestUnassignedVariables();
                    if (notPlaced == null)
                        notPlaced = new ArrayList<Rectangle>();
                    res.print("  unassigned(" + (2 * notPlaced.size()) + "/[");
                    for (Iterator<Rectangle> it = notPlaced.iterator(); it.hasNext();) {
                        Rectangle rect = it.next();
                        res.print(rect.getName() + "X," + rect.getName() + "Y" + (it.hasNext() ? "," : ""));
                    }
                    res.println("]),");
                    int perts = 0;
                    StringBuffer sb = new StringBuffer();
                    for (Rectangle rect : ((RPPModel) best.getModel()).variables()) {
                        if (rect.getBestAssignment() != null) {
                            sb.append(sb.length() == 0 ? "" : ",");
                            sb.append(rect.getName() + "X-" + (rect.getBestAssignment()).getX());
                            sb.append(sb.length() == 0 ? "" : ",");
                            sb.append(rect.getName() + "Y-" + (rect.getBestAssignment()).getY());
                            perts++;
                        }
                    }
                    res.println("  assigned(" + (2 * perts) + "/[" + sb + "])");
                    res.println(").");
                    res.flush();
                }
                txt.println(genNr + ";" + nf.format(time / tests) + ";" + nf.format(ToolBox.rms(tests, time, time2))
                        + ";" + nf.format(((double) assigned) / tests) + ";"
                        + nf.format(ToolBox.rms(tests, assigned, assigned2)) + ";"
                        + nf.format(((double) iters) / tests) + ";" + nf.format(ToolBox.rms(tests, iters, iters2)));
                txt.flush();
                stat.println("averages( problem( " + genNr + " ), time( " + nf.format(time / tests) + " ), assigned( "
                        + nf.format(((double) assigned) / tests) + " ) ).");
                stat.println("deviations( problem( " + genNr + " ), time( "
                        + nf.format(ToolBox.rms(tests, time, time2)) + " ), assigned( "
                        + nf.format(ToolBox.rms(tests, assigned, assigned2)) + " ) ).");
                stat.flush();
            }
            res.close();
            txt.close();
            stat.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                if ((new File(aFile)).exists())
                    is = new FileInputStream(aFile);
                if ((new File(inputCfg.getParent() + File.separator + aFile)).exists())
                    is = new FileInputStream(inputCfg.getParent() + File.separator + aFile);
                if (is == null)
                    System.err.println("Unable to find include file '" + aFile + "'.");
                properties.load(is);
                is.close();
            }
            String outDirTisTest = (outDir == null ? properties.getProperty("General.Output", ".") : outDir)
                    + File.separator + name + File.separator + sDateFormat.format(new Date());
            properties.setProperty("General.Output", outDirTisTest.toString());
            System.out.println("Output folder: " + properties.getProperty("General.Output"));
            (new File(outDirTisTest)).mkdirs();
            ToolBox.configureLogging(outDirTisTest, null);
            FileOutputStream fos = new FileOutputStream(outDirTisTest + File.separator + "rcsp.conf");
            properties.store(fos, "Random CSP problem configuration file");
            fos.flush();
            fos.close();
            boolean mpp = properties.getPropertyBoolean("General.MPP", true);
            if (mpp)
                testMPP(properties);
            else
                test(properties);
        }
    }

    /**
     * RPP test.
     * 
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {
        try {
            Progress.getInstance().addProgressListener(new ProgressWriter(System.out));

            File inputCfg = new File(args[0]);
            DataProperties properties = ToolBox.loadProperties(inputCfg);
            if (properties.getProperty("INCLUDE_REGEXP") != null) {
                if (args.length > 1)
                    properties.setProperty("General.Output", args[1]);
                test(inputCfg, null, null, properties.getProperty("INCLUDE_REGEXP"), (args.length > 1 ? args[1] : null));
            } else {
                String outDir = properties.getProperty("General.Output", ".") + File.separator
                        + inputCfg.getName().substring(0, inputCfg.getName().lastIndexOf('.')) + File.separator
                        + sDateFormat.format(new Date());
                if (args.length > 1)
                    outDir = args[1] + File.separator + (sDateFormat.format(new Date()));
                (new File(outDir)).mkdirs();
                properties.setProperty("General.Output", outDir.toString());
                System.out.println("Output folder: " + properties.getProperty("General.Output"));
                ToolBox.configureLogging(outDir, null);
                boolean mpp = properties.getPropertyBoolean("General.MPP", false);
                if (mpp)
                    testMPP(properties);
                else
                    test(properties);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
