package org.cpsolver.ifs.example.jobshop;

import java.io.File;
import java.util.Date;

import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.ifs.util.ProgressWriter;
import org.cpsolver.ifs.util.ToolBox;


/**
 * Test of Job Shop problem. It takes one argument -- property file with all the
 * parameters. <br>
 * <br>
 * Test's parameters: <br>
 * <table border='1'><caption>Related Solver Parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>General.Input</td>
 * <td>{@link String}</td>
 * <td>Input file describing the job shop problem</td>
 * </tr>
 * <tr>
 * <td>General.Output</td>
 * <td>{@link String}</td>
 * <td>Output folder where a log file and a solution (solution.txt) will be
 * placed</td>
 * </tr>
 * </table>
 * <br>
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
    private static java.text.SimpleDateFormat sDateFormat = new java.text.SimpleDateFormat("dd-MMM-yy_HHmmss", java.util.Locale.US);
    private static org.apache.logging.log4j.Logger sLogger = org.apache.logging.log4j.LogManager.getLogger(Test.class);

    public static void test(DataProperties properties) {
        try {
            String inputFile = properties.getProperty("General.Input");
            JobShopModel model = JobShopModel.loadModel(inputFile);
            Solver<Operation, Location> s = new Solver<Operation, Location>(properties);
            s.setInitalSolution(model);
            s.start();
            s.getSolverThread().join();
            Solution<Operation, Location> best = s.currentSolution();
            best.restoreBest();
            sLogger.info("Best solution info:" + best.getInfo());
            sLogger.info("Best solution:" + model.toString());
            model.save(best.getAssignment(), properties.getProperty("General.Output") + File.separator + "solution.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Progress.getInstance().addProgressListener(new ProgressWriter(System.out));

            File inputCfg = new File(args[0]);
            DataProperties properties = ToolBox.loadProperties(inputCfg);
            String outDir = properties.getProperty("General.Output", ".") + File.separator + inputCfg.getName().substring(0, inputCfg.getName().lastIndexOf('.')) + File.separator + sDateFormat.format(new Date());
            (new File(outDir)).mkdirs();
            properties.setProperty("General.Output", outDir.toString());
            ToolBox.configureLogging(outDir, null);
            test(properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
