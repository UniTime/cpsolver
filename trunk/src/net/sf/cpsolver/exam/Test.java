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
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solution.SolutionListener;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ToolBox;

public class Test {
    private static org.apache.log4j.Logger sLog = org.apache.log4j.Logger.getLogger(Test.class);
    
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

    public static void main(String[] args) {
        try {
            DataProperties cfg = new DataProperties();
            cfg.setProperty("Termination.StopWhenComplete","false");
            cfg.setProperty("Termination.TimeOut","1800");
            cfg.setProperty("General.SaveBestUnassigned", "-1");
            cfg.setProperty("General.Input","c:\\exam1070.xml");
            cfg.setProperty("General.OutputFile","c:\\exam1070s.xml");
            //cfg.setProperty("Extensions.Classes","net.sf.cpsolver.ifs.extension.ConflictStatistics");
            //cfg.setProperty("Neighbour.Class","net.sf.cpsolver.exam.heuristics.ExamConstruction");
            cfg.setProperty("Neighbour.Class","net.sf.cpsolver.exam.heuristics.ExamNeighbourSelection");
            if (args.length>=1) {
                cfg.load(new FileInputStream(args[0]));
            }
            cfg.putAll(System.getProperties());
            
            if (args.length>=2) {
                cfg.setProperty("General.Input", args[1]);
            }
            
            setupLogging(null, false);
            
            ExamModel model = new ExamModel(cfg);
            
            Document document = (new SAXReader()).read(new File(cfg.getProperty("General.Input")));
            model.load(document, false, false);
            
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

            
            solver.start();
            try {
                solver.getSolverThread().join();
            } catch (InterruptedException e) {}
            
            Solution solution = solver.lastSolution();
            Progress.removeInstance(solution.getModel());
            if (solution.getBestInfo()==null) {
                sLog.error("No best solution found.");
            } else solution.restoreBest();
            
            sLog.info("Best solution:"+ToolBox.dict2string(solution.getExtendedInfo(),1));
            
            sLog.info("Best solution found after "+solution.getBestTime()+" seconds ("+solution.getBestIteration()+" iterations).");
            sLog.info("Number of assigned variables is "+solution.getModel().assignedVariables().size());
            sLog.info("Total value of the solution is "+solution.getModel().getTotalValue());
            
            FileOutputStream fos = new FileOutputStream(new File(cfg.getProperty("General.OutputFile",cfg.getProperty("General.Output")+File.separator+"solution.xml")));
            (new XMLWriter(fos,OutputFormat.createPrettyPrint())).write(model.save(true,true));
            fos.flush();fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
