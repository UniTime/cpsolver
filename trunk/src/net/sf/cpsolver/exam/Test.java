package net.sf.cpsolver.exam;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Map;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamDistributionConstraint;
import net.sf.cpsolver.exam.model.ExamInstructor;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPeriod;
import net.sf.cpsolver.exam.model.ExamPeriodPlacement;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamRoom;
import net.sf.cpsolver.exam.model.ExamRoomPlacement;
import net.sf.cpsolver.exam.model.ExamStudent;
import net.sf.cpsolver.exam.reports.ExamAssignments;
import net.sf.cpsolver.exam.reports.ExamCourseSectionAssignments;
import net.sf.cpsolver.exam.reports.ExamInstructorConflicts;
import net.sf.cpsolver.exam.reports.ExamNbrMeetingsPerDay;
import net.sf.cpsolver.exam.reports.ExamPeriodUsage;
import net.sf.cpsolver.exam.reports.ExamRoomSchedule;
import net.sf.cpsolver.exam.reports.ExamRoomSplit;
import net.sf.cpsolver.exam.reports.ExamStudentBackToBackConflicts;
import net.sf.cpsolver.exam.reports.ExamStudentConflicts;
import net.sf.cpsolver.exam.reports.ExamStudentConflictsBySectionCourse;
import net.sf.cpsolver.exam.reports.ExamStudentConflictsPerExam;
import net.sf.cpsolver.exam.reports.ExamStudentDirectConflicts;
import net.sf.cpsolver.exam.reports.ExamStudentMoreTwoADay;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solution.SolutionListener;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ToolBox;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/**
 * An examination timetabling test program. The following steps are performed:
 * <ul>
 * <li>Input properties are loaded
 * <li>Input problem is loaded (General.Input property)
 * <li>Problem is solved (using the given properties)
 * <li>Solution is save (General.OutputFile property)
 * </ul>
 * <br>
 * <br>
 * Usage: <code>
 * &nbsp;&nbsp;&nbsp;java -Xmx1024m -jar examtt-1.1.jar exam.properties input.xml output.xml
 * </code> <br>
 * <br>
 * 
 * @version ExamTT 1.2 (Examination Timetabling)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
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
    private static org.apache.log4j.Logger sLog = org.apache.log4j.Logger.getLogger(Test.class);

    /**
     * Setup log4j logging
     * 
     * @param logFile
     *            log file
     * @param debug
     *            true if debug messages should be logged (use -Ddebug=true to
     *            enable debug message)
     */
    public static void setupLogging(File logFile, boolean debug) {
        Logger root = Logger.getRootLogger();
        ConsoleAppender console = new ConsoleAppender(new PatternLayout("%m%n"));// %-5p
                                                                                 // %c{1}>
                                                                                 // %m%n
        console.setThreshold(Level.INFO);
        root.addAppender(console);
        if (logFile != null) {
            try {
                FileAppender file = new FileAppender(new PatternLayout(
                        "%d{dd-MMM-yy HH:mm:ss.SSS} [%t] %-5p %c{2}> %m%n"), logFile.getPath(), false);
                file.setThreshold(Level.DEBUG);
                root.addAppender(file);
            } catch (IOException e) {
                sLog.fatal("Unable to configure logging, reason: " + e.getMessage(), e);
            }
        }
        if (!debug)
            root.setLevel(Level.INFO);
    }

    /** Generate exam reports */
    public static void createReports(ExamModel model, File outDir, String outName) throws IOException {
        new ExamAssignments(model).report().save(new File(outDir, outName + ".schdex.csv"));

        new ExamCourseSectionAssignments(model).report().save(new File(outDir, outName + ".schdcs.csv"));

        new ExamStudentConflicts(model).report().save(new File(outDir, outName + ".sconf.csv"));

        new ExamInstructorConflicts(model).report().save(new File(outDir, outName + ".iconf.csv"));

        new ExamStudentConflictsPerExam(model).report().save(new File(outDir, outName + ".sconfex.csv"));

        new ExamStudentDirectConflicts(model).report().save(new File(outDir, outName + ".sdir.csv"));

        new ExamStudentBackToBackConflicts(model).report().save(new File(outDir, outName + ".sbtb.csv"));

        new ExamStudentMoreTwoADay(model).report().save(new File(outDir, outName + ".sm2d.csv"));

        new ExamPeriodUsage(model).report().save(new File(outDir, outName + ".per.csv"));

        new ExamRoomSchedule(model).report().save(new File(outDir, outName + ".schdr.csv"));

        new ExamRoomSplit(model).report().save(new File(outDir, outName + ".rsplit.csv"));

        new ExamNbrMeetingsPerDay(model).report().save(new File(outDir, outName + ".distmpd.csv"));

        new ExamStudentConflictsBySectionCourse(model).report().save(new File(outDir, outName + ".sconfcs.csv"));
    }

    public static class ShutdownHook extends Thread {
        Solver<Exam, ExamPlacement> iSolver = null;

        public ShutdownHook(Solver<Exam, ExamPlacement> solver) {
            setName("ShutdownHook");
            iSolver = solver;
        }

        @Override
        public void run() {
            try {
                if (iSolver.isRunning())
                    iSolver.stopSolver();
                Solution<Exam, ExamPlacement> solution = iSolver.lastSolution();
                Progress.removeInstance(solution.getModel());
                if (solution.getBestInfo() == null) {
                    sLog.error("No best solution found.");
                } else
                    solution.restoreBest();

                sLog.info("Best solution:" + ToolBox.dict2string(solution.getExtendedInfo(), 1));

                sLog.info("Best solution found after " + solution.getBestTime() + " seconds ("
                        + solution.getBestIteration() + " iterations).");
                sLog.info("Number of assigned variables is " + solution.getModel().nrAssignedVariables());
                sLog.info("Total value of the solution is " + solution.getModel().getTotalValue());

                File outFile = new File(iSolver.getProperties().getProperty("General.OutputFile",
                        iSolver.getProperties().getProperty("General.Output") + File.separator + "solution.xml"));
                FileOutputStream fos = new FileOutputStream(outFile);
                (new XMLWriter(fos, OutputFormat.createPrettyPrint())).write(((ExamModel) solution.getModel()).save());
                fos.flush();
                fos.close();

                if ("true".equals(System.getProperty("reports", "false")))
                    createReports((ExamModel) solution.getModel(), outFile.getParentFile(), outFile.getName()
                            .substring(0, outFile.getName().lastIndexOf('.')));

                String baseName = new File(iSolver.getProperties().getProperty("General.Input")).getName();
                if (baseName.indexOf('.') > 0)
                    baseName = baseName.substring(0, baseName.lastIndexOf('.'));
                addCSVLine(new File(outFile.getParentFile(), baseName + ".csv"), outFile.getName(), solution);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static int getMinPenalty(ExamRoom r) {
        int min = Integer.MAX_VALUE;
        for (ExamPeriod p : ((ExamModel) r.getModel()).getPeriods()) {
            if (r.isAvailable(p)) {
                min = Math.min(min, r.getPenalty(p));
            }
        }
        return min;
    }

    private static int getMaxPenalty(ExamRoom r) {
        int max = Integer.MIN_VALUE;
        for (ExamPeriod p : ((ExamModel) r.getModel()).getPeriods()) {
            if (r.isAvailable(p)) {
                max = Math.max(max, r.getPenalty(p));
            }
        }
        return max;
    }

    private static void addCSVLine(File file, String instance, Solution<Exam, ExamPlacement> solution) {
        try {
            ExamModel model = (ExamModel) solution.getModel();
            boolean ex = file.exists();
            PrintWriter pw = new PrintWriter(new FileWriter(file, true));
            if (!ex) {
                pw.println("SEED," + "DC,sM2D,BTB," + (model.getBackToBackDistance() < 0 ? "" : "dBTB,")
                        + "iDC,iM2D,iBTB," + (model.getBackToBackDistance() < 0 ? "" : "diBTB,")
                        + "PP,@P,RSz,RSp,RD,RP,DP," + (model.getLargeSize() >= 0 ? ",LP" : "")
                        + (model.isMPP() ? ",IP" : "") + ",INSTANCE");
                int minPeriodPenalty = 0, maxPeriodPenalty = 0;
                int minRoomPenalty = 0, maxRoomPenalty = 0;
                int nrLargeExams = 0;
                for (Exam exam : model.variables()) {
                    if (model.getLargeSize() >= 0 && exam.getSize() >= model.getLargeSize())
                        nrLargeExams++;
                    if (!exam.getPeriodPlacements().isEmpty()) {
                        int minPenalty = Integer.MAX_VALUE, maxPenalty = Integer.MIN_VALUE;
                        for (ExamPeriodPlacement periodPlacement : exam.getPeriodPlacements()) {
                            minPenalty = Math.min(minPenalty, periodPlacement.getPenalty());
                            maxPenalty = Math.max(maxPenalty, periodPlacement.getPenalty());
                        }
                        minPeriodPenalty += minPenalty;
                        maxPeriodPenalty += maxPenalty;
                    }
                    if (!exam.getRoomPlacements().isEmpty()) {
                        int minPenalty = Integer.MAX_VALUE, maxPenalty = Integer.MIN_VALUE;
                        for (ExamRoomPlacement roomPlacement : exam.getRoomPlacements()) {
                            minPenalty = Math.min(minPenalty, roomPlacement.getPenalty()
                                    + getMinPenalty(roomPlacement.getRoom()));
                            maxPenalty = Math.max(maxPenalty, roomPlacement.getPenalty()
                                    + getMaxPenalty(roomPlacement.getRoom()));
                        }
                        minRoomPenalty += minPenalty;
                        maxRoomPenalty += maxPenalty;
                    }
                }
                int maxDistributionPenalty = 0;
                for (ExamDistributionConstraint dc : model.getDistributionConstraints()) {
                    if (dc.isHard())
                        continue;
                    maxDistributionPenalty += dc.getWeight();
                }
                int nrStudentExams = 0;
                for (ExamStudent student : model.getStudents()) {
                    nrStudentExams += student.variables().size();
                }
                int nrInstructorExams = 0;
                for (ExamInstructor instructor : model.getInstructors()) {
                    nrInstructorExams += instructor.variables().size();
                }
                pw
                        .println("MIN," + "#EX,#RM,#PER," + (model.getBackToBackDistance() < 0 ? "" : ",")
                                + "#STD,#STDX,," + (model.getBackToBackDistance() < 0 ? "" : ",") + minPeriodPenalty
                                + ",#INS,#INSX,,," + minRoomPenalty + ",0," + (model.getLargeSize() >= 0 ? ",0" : "")
                                + (model.isMPP() ? "," : ""));
                pw.println("MAX," + model.variables().size() + "," + model.getRooms().size() + ","
                        + model.getPeriods().size() + "," + (model.getBackToBackDistance() < 0 ? "" : ",")
                        + model.getStudents().size() + "," + nrStudentExams + ",,"
                        + (model.getBackToBackDistance() < 0 ? "" : ",") + maxPeriodPenalty + ","
                        + model.getInstructors().size() + "," + nrInstructorExams + ",,," + maxRoomPenalty + ","
                        + maxDistributionPenalty + "," + (model.getLargeSize() >= 0 ? "," + nrLargeExams : "")
                        + (model.isMPP() ? "," : ""));
            }
            DecimalFormat df = new DecimalFormat("0.00");
            pw.println(ToolBox.getSeed()
                    + ","
                    + model.getNrDirectConflicts(false)
                    + ","
                    + model.getNrMoreThanTwoADayConflicts(false)
                    + ","
                    + model.getNrBackToBackConflicts(false)
                    + ","
                    + (model.getBackToBackDistance() < 0 ? "" : model.getNrDistanceBackToBackConflicts(false) + ",")
                    + model.getNrInstructorDirectConflicts(false)
                    + ","
                    + model.getNrInstructorMoreThanTwoADayConflicts(false)
                    + ","
                    + model.getNrInstructorBackToBackConflicts(false)
                    + ","
                    + (model.getBackToBackDistance() < 0 ? "" : model.getNrInstructorDistanceBackToBackConflicts(false)
                            + ",")
                    + model.getPeriodPenalty(false)
                    + ","
                    + model.getExamRotationPenalty(false)
                    + ","
                    + df.format(((double) model.getRoomSizePenalty(false)) / model.nrAssignedVariables())
                    + ","
                    + model.getRoomSplitPenalty(false)
                    + ","
                    + df.format(model.getRoomSplitDistancePenalty(false) / model.getNrRoomSplits(false))
                    + ","
                    + model.getRoomPenalty(false)
                    + ","
                    + model.getDistributionPenalty(false)
                    + (model.getLargeSize() >= 0 ? "," + model.getLargePenalty(false) : "")
                    + (model.isMPP() ? ","
                            + df.format(((double) model.getPerturbationPenalty(false)) / model.nrAssignedVariables())
                            : "") + "," + instance);
            pw.flush();
            pw.close();
        } catch (Exception e) {
            sLog.error("Unable to add CSV line to " + file, e);
        }
    }

    /**
     * Main program
     * 
     * @param args
     *            problem property file, input file (optional, may be set by
     *            General.Input property), output file (optional, may be set by
     *            General.OutputFile property)
     */
    public static void main(String[] args) {
        try {
            DataProperties cfg = new DataProperties();
            cfg.setProperty("Termination.StopWhenComplete", "false");
            cfg.setProperty("Termination.TimeOut", "1800");
            cfg.setProperty("General.SaveBestUnassigned", "-1");
            cfg.setProperty("Neighbour.Class", "net.sf.cpsolver.exam.heuristics.ExamNeighbourSelection");
            if (args.length >= 1) {
                cfg.load(new FileInputStream(args[0]));
            }
            cfg.putAll(System.getProperties());

            File inputFile = new File("c:\\test\\exam\\exam1070.xml");
            if (args.length >= 2) {
                inputFile = new File(args[1]);
            }
            ToolBox.setSeed(cfg.getPropertyLong("General.Seed", Math.round(Long.MAX_VALUE * Math.random())));

            cfg.setProperty("General.Input", inputFile.toString());

            String outName = inputFile.getName();
            if (outName.indexOf('.') >= 0)
                outName = outName.substring(0, outName.lastIndexOf('.')) + "s.xml";
            File outFile = new File(inputFile.getParentFile(), outName);
            if (args.length >= 3) {
                outFile = new File(args[2]);
                if (outFile.exists() && outFile.isDirectory())
                    outFile = new File(outFile, outName);
                if (!outFile.exists() && !outFile.getName().endsWith(".xml"))
                    outFile = new File(outFile, outName);
            }
            if (outFile.getParentFile() != null)
                outFile.getParentFile().mkdirs();
            cfg.setProperty("General.OutputFile", outFile.toString());
            cfg.setProperty("General.Output", outFile.getParent());

            String logName = outFile.getName();
            if (logName.indexOf('.') >= 0)
                logName = logName.substring(0, logName.lastIndexOf('.')) + ".log";
            setupLogging(new File(outFile.getParent(), logName), "true".equals(System.getProperty("debug", "false")));

            ExamModel model = new ExamModel(cfg);

            Document document = (new SAXReader()).read(new File(cfg.getProperty("General.Input")));
            model.load(document);

            sLog.info("Loaded model: " + ToolBox.dict2string(model.getExtendedInfo(), 2));

            Solver<Exam, ExamPlacement> solver = new Solver<Exam, ExamPlacement>(cfg);
            solver.setInitalSolution(new Solution<Exam, ExamPlacement>(model));

            solver.currentSolution().addSolutionListener(new SolutionListener<Exam, ExamPlacement>() {
                public void solutionUpdated(Solution<Exam, ExamPlacement> solution) {
                }

                public void getInfo(Solution<Exam, ExamPlacement> solution, Map<String, String> info) {
                }

                public void getInfo(Solution<Exam, ExamPlacement> solution, Map<String, String> info,
                        Collection<Exam> variables) {
                }

                public void bestCleared(Solution<Exam, ExamPlacement> solution) {
                }

                public void bestSaved(Solution<Exam, ExamPlacement> solution) {
                    ExamModel m = (ExamModel) solution.getModel();
                    if (sLog.isInfoEnabled()) {
                        sLog.info("**BEST["
                                + solution.getIteration()
                                + "]** "
                                + (m.nrUnassignedVariables() > 0 ? "V:" + m.nrAssignedVariables() + "/"
                                        + m.variables().size() + " - " : "") + "T:"
                                + new DecimalFormat("0.00").format(m.getTotalValue()) + " (" + m + ")");
                    }
                }

                public void bestRestored(Solution<Exam, ExamPlacement> solution) {
                }
            });

            Runtime.getRuntime().addShutdownHook(new ShutdownHook(solver));

            solver.start();
            try {
                solver.getSolverThread().join();
            } catch (InterruptedException e) {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
