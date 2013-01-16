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

import net.sf.cpsolver.exam.criteria.DistributionPenalty;
import net.sf.cpsolver.exam.criteria.ExamRotationPenalty;
import net.sf.cpsolver.exam.criteria.InstructorBackToBackConflicts;
import net.sf.cpsolver.exam.criteria.InstructorDirectConflicts;
import net.sf.cpsolver.exam.criteria.InstructorDistanceBackToBackConflicts;
import net.sf.cpsolver.exam.criteria.InstructorMoreThan2ADayConflicts;
import net.sf.cpsolver.exam.criteria.InstructorNotAvailableConflicts;
import net.sf.cpsolver.exam.criteria.LargeExamsPenalty;
import net.sf.cpsolver.exam.criteria.PeriodIndexPenalty;
import net.sf.cpsolver.exam.criteria.PeriodPenalty;
import net.sf.cpsolver.exam.criteria.PeriodSizePenalty;
import net.sf.cpsolver.exam.criteria.PerturbationPenalty;
import net.sf.cpsolver.exam.criteria.RoomPenalty;
import net.sf.cpsolver.exam.criteria.RoomPerturbationPenalty;
import net.sf.cpsolver.exam.criteria.RoomSizePenalty;
import net.sf.cpsolver.exam.criteria.RoomSplitDistancePenalty;
import net.sf.cpsolver.exam.criteria.RoomSplitPenalty;
import net.sf.cpsolver.exam.criteria.StudentBackToBackConflicts;
import net.sf.cpsolver.exam.criteria.StudentDirectConflicts;
import net.sf.cpsolver.exam.criteria.StudentDistanceBackToBackConflicts;
import net.sf.cpsolver.exam.criteria.StudentMoreThan2ADayConflicts;
import net.sf.cpsolver.exam.criteria.StudentNotAvailableConflicts;
import net.sf.cpsolver.exam.criteria.additional.DistanceToStronglyPreferredRoom;
import net.sf.cpsolver.exam.criteria.additional.DistributionViolation;
import net.sf.cpsolver.exam.criteria.additional.PeriodViolation;
import net.sf.cpsolver.exam.criteria.additional.RoomViolation;
import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamInstructor;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPlacement;
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
import net.sf.cpsolver.exam.split.ExamSplitter;
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
                addCSVLine(new File(outFile.getParentFile(), baseName + ".csv"), outFile.getName(), iSolver.getProperties().getProperty("General.Config"), solution);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void addCSVLine(File file, String instance, String config, Solution<Exam, ExamPlacement> solution) {
        try {
            ExamModel model = (ExamModel) solution.getModel();
            boolean ex = file.exists();
            PrintWriter pw = new PrintWriter(new FileWriter(file, true));
            boolean mpp = ((PerturbationPenalty)model.getCriterion(PerturbationPenalty.class)).isMPP();
            int largeSize = ((LargeExamsPenalty)model.getCriterion(LargeExamsPenalty.class)).getLargeSize();
            RoomSplitDistancePenalty splitDistance = (RoomSplitDistancePenalty)model.getCriterion(RoomSplitDistancePenalty.class);
            ExamSplitter splitter = (ExamSplitter)model.getCriterion(ExamSplitter.class);
            PeriodViolation violPer = (PeriodViolation)model.getCriterion(PeriodViolation.class);
            RoomViolation violRoom = (RoomViolation)model.getCriterion(RoomViolation.class);
            DistributionViolation violDist = (DistributionViolation)model.getCriterion(DistributionViolation.class);
            DistanceToStronglyPreferredRoom distStrPref = (DistanceToStronglyPreferredRoom)model.getCriterion(DistanceToStronglyPreferredRoom.class);
            ExamRotationPenalty rotation = (ExamRotationPenalty)model.getCriterion(ExamRotationPenalty.class);
            DecimalFormat df = new DecimalFormat("0.00");
            if (!ex) {
                pw.println("SEED"
                        + ",NA,DC,M2D,BTB" + (model.getBackToBackDistance() < 0 ? "" : ",dBTB")
                        + ",iNA,iDC,iM2D,iBTB" + (model.getBackToBackDistance() < 0 ? "" : ",diBTB")
                        + ",PP,RP,DP"
                        + ",PI,@P,PS" // Period Index, Rotation Penalty, Period Size
                        + ",RSz,RSp,RD" // Room Size, Room Split, Room Split Distance
                        + (largeSize >= 0 ? ",LP" : "")
                        + (mpp ? ",IP,IRP" : "")
                        + (distStrPref == null ? "" : ",@D")
                        + (splitter == null ? "" : ",XX")
                        + (violPer == null ? "" : ",!P")
                        + (violRoom == null ? "" : ",!R")
                        + (violDist == null ? "" : ",!D")
                        + ",INSTANCE,CONFIG");
                int nrStudentExams = 0;
                for (ExamStudent student : model.getStudents()) {
                    nrStudentExams += student.variables().size();
                }
                int nrInstructorExams = 0;
                for (ExamInstructor instructor : model.getInstructors()) {
                    nrInstructorExams += instructor.variables().size();
                }
                pw.println("MIN"
                        + ",#EX,#RM,#PER," + (model.getBackToBackDistance() < 0 ? "" : ",")
                        + ",#STD,#STDX,#INS,#INSX" + (model.getBackToBackDistance() < 0 ? "" : ",")
                        + "," + model.getCriterion(PeriodPenalty.class).getBounds()[0]
                        + "," + model.getCriterion(RoomPenalty.class).getBounds()[0]
                        + "," + model.getCriterion(DistributionPenalty.class).getBounds()[0]
                        + ",," + df.format(rotation.averagePeriod()) + ","
                        + ",,,"
                        + (largeSize >= 0 ? ",0" : "")
                        + (mpp ? ",," : "")
                        + (distStrPref == null ? "" : ",")
                        + (splitter == null ? "" : ",")
                        + (violPer == null ? "" : ",")
                        + (violRoom == null ? "" : ",")
                        + (violDist == null ? "" : ",")
                        + ",,");
                pw.println("MAX"
                        + "," + model.variables().size() + "," + model.getRooms().size() + "," + model.getPeriods().size() + "," + (model.getBackToBackDistance() < 0 ? "" : ",")
                        + "," + model.getStudents().size() + "," + nrStudentExams + "," + model.getInstructors().size() + "," + nrInstructorExams + (model.getBackToBackDistance() < 0 ? "" : ",")
                        + "," + model.getCriterion(PeriodPenalty.class).getBounds()[1]
                        + "," + model.getCriterion(RoomPenalty.class).getBounds()[1]
                        + "," + model.getCriterion(DistributionPenalty.class).getBounds()[1]
                        + ",," + rotation.nrAssignedExamsWithAvgPeriod() + ","
                        + ",,,"
                        + (largeSize >= 0 ? "," + model.getCriterion(LargeExamsPenalty.class).getBounds()[1] : "")
                        + (mpp ? ",," : "")
                        + (distStrPref == null ? "" : ",")
                        + (splitter == null ? "" : ",")
                        + (violPer == null ? "" : "," + model.getCriterion(PeriodViolation.class).getBounds()[1])
                        + (violRoom == null ? "" : "," + model.getCriterion(RoomViolation.class).getBounds()[1])
                        + (violDist == null ? "" : "," + model.getCriterion(DistributionViolation.class).getBounds()[1])
                        + ",,");
            }
            pw.println(ToolBox.getSeed()
                    + "," + model.getCriterion(StudentNotAvailableConflicts.class).getValue()
                    + "," + model.getCriterion(StudentDirectConflicts.class).getValue()
                    + "," + model.getCriterion(StudentMoreThan2ADayConflicts.class).getValue()
                    + "," + model.getCriterion(StudentBackToBackConflicts.class).getValue()
                    + (model.getBackToBackDistance() < 0 ? "" : "," + model.getCriterion(StudentDistanceBackToBackConflicts.class).getValue())
                    + "," + model.getCriterion(InstructorNotAvailableConflicts.class).getValue()
                    + "," + model.getCriterion(InstructorDirectConflicts.class).getValue()
                    + "," + model.getCriterion(InstructorMoreThan2ADayConflicts.class).getValue()
                    + "," + model.getCriterion(InstructorBackToBackConflicts.class).getValue()
                    + (model.getBackToBackDistance() < 0 ? "" : "," + model.getCriterion(InstructorDistanceBackToBackConflicts.class).getValue())
                    + "," + model.getCriterion(PeriodPenalty.class).getValue()
                    + "," + model.getCriterion(RoomPenalty.class).getValue()
                    + "," + model.getCriterion(DistributionPenalty.class).getValue()
                    + "," + df.format(model.getCriterion(PeriodIndexPenalty.class).getValue() / model.nrAssignedVariables())
                    + "," + df.format(Math.sqrt(rotation.getValue() / rotation.nrAssignedExamsWithAvgPeriod()) - 1)
                    + "," + df.format(model.getCriterion(PeriodSizePenalty.class).getValue() / model.nrAssignedVariables())
                    + "," + df.format(model.getCriterion(RoomSizePenalty.class).getValue() / model.nrAssignedVariables())
                    + "," + model.getCriterion(RoomSplitPenalty.class).getValue()
                    + "," + df.format(splitDistance.nrRoomSplits() <= 0 ? 0.0 : splitDistance.getValue() / splitDistance.nrRoomSplits())
                    + (largeSize >= 0 ? "," + model.getCriterion(LargeExamsPenalty.class).getValue() : "")
                    + (mpp ? "," + df.format(model.getCriterion(PerturbationPenalty.class).getValue() / model.nrAssignedVariables())
                           + "," + df.format(model.getCriterion(RoomPerturbationPenalty.class).getValue() / model.nrAssignedVariables()): "")
                    + (distStrPref == null ? "" : "," + df.format(distStrPref.getValue() / model.nrAssignedVariables()))
                    + (splitter == null ? "" : "," + df.format(splitter.getValue()))
                    + (violPer == null ? "" : "," + df.format(violPer.getValue()))
                    + (violRoom == null ? "" : "," + df.format(violRoom.getValue()))
                    + (violDist == null ? "" : "," + df.format(violDist.getValue()))
                    + "," + instance + "," + config);
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
                cfg.setProperty("General.Config", new File(args[0]).getName());
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

            Solver<Exam, ExamPlacement> solver = new Solver<Exam, ExamPlacement>(cfg);
            solver.setInitalSolution(new Solution<Exam, ExamPlacement>(model));

            solver.currentSolution().addSolutionListener(new SolutionListener<Exam, ExamPlacement>() {
                @Override
                public void solutionUpdated(Solution<Exam, ExamPlacement> solution) {
                }

                @Override
                public void getInfo(Solution<Exam, ExamPlacement> solution, Map<String, String> info) {
                }

                @Override
                public void getInfo(Solution<Exam, ExamPlacement> solution, Map<String, String> info,
                        Collection<Exam> variables) {
                }

                @Override
                public void bestCleared(Solution<Exam, ExamPlacement> solution) {
                }

                @Override
                public void bestSaved(Solution<Exam, ExamPlacement> solution) {
                    ExamModel m = (ExamModel) solution.getModel();
                    if (sLog.isInfoEnabled()) {
                        sLog.info("**BEST[" + solution.getIteration() + "]** "
                                + (m.nrUnassignedVariables() > 0 ? "V:" + m.nrAssignedVariables() + "/" + m.variables().size() + " - " : "") +
                                "T:" + new DecimalFormat("0.00").format(m.getTotalValue()) +
                                " " + m);
                    }
                }

                @Override
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
