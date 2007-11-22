package net.sf.cpsolver.exam;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.reports.ExamAssignments;
import net.sf.cpsolver.exam.reports.ExamCourseSectionAssignments;
import net.sf.cpsolver.exam.reports.ExamInstructorConflicts;
import net.sf.cpsolver.exam.reports.ExamPeriodUsage;
import net.sf.cpsolver.exam.reports.ExamRoomSchedule;
import net.sf.cpsolver.exam.reports.ExamRoomSplit;
import net.sf.cpsolver.exam.reports.ExamStudentBackToBackConflicts;
import net.sf.cpsolver.exam.reports.ExamStudentConflicts;
import net.sf.cpsolver.exam.reports.ExamStudentConflictsPerExam;
import net.sf.cpsolver.exam.reports.ExamStudentDirectConflicts;
import net.sf.cpsolver.exam.reports.ExamStudentMoreTwoADay;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solution.SolutionListener;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * An examination timetabling test program. The follwoing steps are performed:
 * <ul>
 *  <li>Input properties are loaded
 *  <li>Input problem is loaded (General.Input property)
 *  <li>Problem is solved (using the given properties)
 *  <li>Solution is save (General.OutputFile property)
 * </ul>
 * <br><br>
 * Usage:
 * <code>
 * &nbsp;&nbsp;&nbsp;java -Xmx1024m -jar examtt-1.1.jar exam.properties input.xml output.xml
 * </code>
 * <br><br>
 * 
 * @version
 * ExamTT 1.1 (Examination Timetabling)<br>
 * Copyright (C) 2007 Tomas Muller<br>
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
    private static org.apache.log4j.Logger sLog = org.apache.log4j.Logger.getLogger(Test.class);
    
    /**
     * Setup log4j logging
     * @param logFile log file
     * @param debug true if debug messages should be logged (use -Ddebug=true to enable debug message)
     */
    public static void setupLogging(File logFile, boolean debug) {
        Logger root = Logger.getRootLogger();
        ConsoleAppender console = new ConsoleAppender(new PatternLayout("%m%n"));//%-5p %c{1}> %m%n
        console.setThreshold(Level.INFO);
        root.addAppender(console);
        if (logFile!=null) {
            try {
                FileAppender file = new FileAppender(
                        new PatternLayout("%d{dd-MMM-yy HH:mm:ss.SSS} [%t] %-5p %c{2}> %m%n"), 
                        logFile.getPath(), 
                        false);
                file.setThreshold(Level.DEBUG);
                root.addAppender(file);
            } catch (IOException e) {
                sLog.fatal("Unable to configure logging, reason: "+e.getMessage(), e);
            }
        }
        if (!debug) root.setLevel(Level.INFO);
    }
    
    public static class ShutdownHook extends Thread {
        Solver iSolver = null;
        public ShutdownHook(Solver solver) {
            setName("ShutdownHook");
            iSolver = solver;
        }
        public void run() {
            try {
                if (iSolver.isRunning()) iSolver.stopSolver();
                Solution solution = iSolver.lastSolution();
                Progress.removeInstance(solution.getModel());
                if (solution.getBestInfo()==null) {
                    sLog.error("No best solution found.");
                } else solution.restoreBest();
                
                sLog.info("Best solution:"+ToolBox.dict2string(solution.getExtendedInfo(),1));
                
                sLog.info("Best solution found after "+solution.getBestTime()+" seconds ("+solution.getBestIteration()+" iterations).");
                sLog.info("Number of assigned variables is "+solution.getModel().assignedVariables().size());
                sLog.info("Total value of the solution is "+solution.getModel().getTotalValue());
                
                File outFile = new File(iSolver.getProperties().getProperty("General.OutputFile",iSolver.getProperties().getProperty("General.Output")+File.separator+"solution.xml"));
                FileOutputStream fos = new FileOutputStream(outFile);
                (new XMLWriter(fos,OutputFormat.createPrettyPrint())).write(((ExamModel)solution.getModel()).save());
                fos.flush();fos.close();
                
                new ExamAssignments((ExamModel)solution.getModel()).report().
                    save(new File(outFile.getParentFile(),outFile.getName().substring(0,outFile.getName().lastIndexOf('.'))+".schdex.csv"));

                new ExamCourseSectionAssignments((ExamModel)solution.getModel()).report().
                    save(new File(outFile.getParentFile(),outFile.getName().substring(0,outFile.getName().lastIndexOf('.'))+".schdcs.csv"));

                new ExamStudentConflicts((ExamModel)solution.getModel()).report().
                    save(new File(outFile.getParentFile(),outFile.getName().substring(0,outFile.getName().lastIndexOf('.'))+".sconf.csv"));

                new ExamInstructorConflicts((ExamModel)solution.getModel()).report().
                    save(new File(outFile.getParentFile(),outFile.getName().substring(0,outFile.getName().lastIndexOf('.'))+".iconf.csv"));

                new ExamStudentConflictsPerExam((ExamModel)solution.getModel()).report().
                    save(new File(outFile.getParentFile(),outFile.getName().substring(0,outFile.getName().lastIndexOf('.'))+".sconfex.csv"));

                new ExamStudentDirectConflicts((ExamModel)solution.getModel()).report().
                    save(new File(outFile.getParentFile(),outFile.getName().substring(0,outFile.getName().lastIndexOf('.'))+".sdir.csv"));

                new ExamStudentBackToBackConflicts((ExamModel)solution.getModel()).report().
                    save(new File(outFile.getParentFile(),outFile.getName().substring(0,outFile.getName().lastIndexOf('.'))+".sbtb.csv"));

                new ExamStudentMoreTwoADay((ExamModel)solution.getModel()).report().
                    save(new File(outFile.getParentFile(),outFile.getName().substring(0,outFile.getName().lastIndexOf('.'))+".sm2d.csv"));

                new ExamPeriodUsage((ExamModel)solution.getModel()).report().
                    save(new File(outFile.getParentFile(),outFile.getName().substring(0,outFile.getName().lastIndexOf('.'))+".per.csv"));

                new ExamRoomSchedule((ExamModel)solution.getModel()).report().
                    save(new File(outFile.getParentFile(),outFile.getName().substring(0,outFile.getName().lastIndexOf('.'))+".schdr.csv"));

                new ExamRoomSplit((ExamModel)solution.getModel()).report().
                    save(new File(outFile.getParentFile(),outFile.getName().substring(0,outFile.getName().lastIndexOf('.'))+".rsplit.csv"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Main program
     * @param args problem property file, 
     * input file (optional, may be set by General.Input property), 
     * output file (optional, may be set by General.OutputFile property)
     */
    public static void main(String[] args) {
        try {
            DataProperties cfg = new DataProperties();
            cfg.setProperty("Termination.StopWhenComplete","false");
            cfg.setProperty("Termination.TimeOut","1800");
            cfg.setProperty("General.SaveBestUnassigned", "-1");
            cfg.setProperty("Neighbour.Class","net.sf.cpsolver.exam.heuristics.ExamNeighbourSelection");
            if (args.length>=1) {
                cfg.load(new FileInputStream(args[0]));
            }
            cfg.putAll(System.getProperties());
            
            File inputFile = new File("c:\\test\\exam\\exam1070.xml");
            if (args.length>=2) {
                inputFile = new File(args[1]);
            }
            cfg.setProperty("General.Input", inputFile.toString());
            
            String outName = inputFile.getName();
            if (outName.indexOf('.')>=0)
                outName = outName.substring(0,outName.lastIndexOf('.')) + "s.xml";
            File outFile = new File(inputFile.getParentFile(),outName);
            if (args.length>=3) {
                outFile = new File(args[2]);
                if (outFile.exists() && outFile.isDirectory())
                    outFile = new File(outFile, outName);
                if (!outFile.exists() && !outFile.getName().endsWith(".xml"))
                    outFile = new File(outFile, outName);
            }
            if (outFile.getParentFile()!=null) outFile.getParentFile().mkdirs();
            cfg.setProperty("General.OutputFile", outFile.toString());
            cfg.setProperty("General.Output", outFile.getParent());
            
            String logName = outFile.getName();
            if (logName.indexOf('.')>=0)
                logName = logName.substring(0,logName.lastIndexOf('.')) + ".log";
            setupLogging(new File(outFile.getParent(),logName), 
                    "true".equals(System.getProperty("debug","false")));
            
            ExamModel model = new ExamModel(cfg);
            
            Document document = (new SAXReader()).read(new File(cfg.getProperty("General.Input")));
            model.load(document);
            
            sLog.info("Loaded model: "+ToolBox.dict2string(model.getExtendedInfo(), 2));
            
            Solver solver = new Solver(cfg);
            solver.setInitalSolution(new Solution(model));
            
            solver.currentSolution().addSolutionListener(new SolutionListener() {
                public void solutionUpdated(Solution solution) {}
                public void getInfo(Solution solution, java.util.Dictionary info) {}
                public void getInfo(Solution solution, java.util.Dictionary info, java.util.Vector variables) {}
                public void bestCleared(Solution solution) {}
                public void bestSaved(Solution solution) {
                    ExamModel m = (ExamModel)solution.getModel();
                    if (sLog.isInfoEnabled()) {
                        sLog.info("**BEST["+solution.getIteration()+"]** V:"+m.assignedVariables().size()+"/"+m.variables().size()+
                                " - T:"+new DecimalFormat("0.00").format(m.getTotalValue())+
                                " ("+m+")");
                    }
                }
                public void bestRestored(Solution solution) {}
            });

            Runtime.getRuntime().addShutdownHook(new ShutdownHook(solver));
            
            solver.start();
            try {
                solver.getSolverThread().join();
            } catch (InterruptedException e) {}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
