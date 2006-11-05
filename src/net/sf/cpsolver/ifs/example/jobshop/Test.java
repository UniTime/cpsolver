package net.sf.cpsolver.ifs.example.jobshop;

import java.io.*;
import java.util.*;

import net.sf.cpsolver.ifs.solution.*;
import net.sf.cpsolver.ifs.solver.*;
import net.sf.cpsolver.ifs.util.*;

/**
 * Test of Job Shop problem. It takes one argument -- property file with all the parameters.
 * <br><br>
 * Test's parameters:
 * <br>
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>General.Input</td><td>{@link String}</td><td>Input file describing the job shop problem</td></tr>
 * <tr><td>General.Output</td><td>{@link String}</td><td>Output folder where a log file and a solution (solution.txt) will be placed</td></tr>
 * </table>
 * <br>
 *
 * @version
 * IFS 1.0 (Iterative Forward Search)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@ktiml.mff.cuni.cz">muller@ktiml.mff.cuni.cz</a><br>
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
    private static java.text.DecimalFormat sDoubleFormat = new java.text.DecimalFormat("0.00",new java.text.DecimalFormatSymbols(Locale.US));
    private static java.text.SimpleDateFormat sDateFormat = new java.text.SimpleDateFormat("dd-MMM-yy_HHmmss",java.util.Locale.US);
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(Test.class);

    public static void test(DataProperties properties) {
        try {
            String inputFile = properties.getProperty("General.Input");
            JobShopModel model = JobShopModel.loadModel(inputFile);
            Solver s = new Solver(properties);
            s.setInitalSolution(model);
            s.start();
            s.getSolverThread().join();
            Solution best = s.currentSolution();
            best.restoreBest();
            sLogger.info("Best solution info:"+best.getInfo());
            sLogger.info("Best solution:"+model.toString());
            model.save(properties.getProperty("General.Output")+File.separator+"solution.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        try {
            Progress.getInstance().addProgressListener(new ProgressWriter(System.out));
            
            File inputCfg = new File(args[0]);
            DataProperties properties = ToolBox.loadProperties(inputCfg);
            String outDir = properties.getProperty("General.Output",".")+File.separator + inputCfg.getName().substring(0,inputCfg.getName().lastIndexOf('.'))+File.separator+sDateFormat.format(new Date());
            (new File(outDir)).mkdirs();
            properties.setProperty("General.Output", outDir.toString());
            ToolBox.configureLogging(outDir, null);
            test(properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
