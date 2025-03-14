package org.cpsolver.exam;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;


import org.cpsolver.exam.criteria.DistributionPenalty;
import org.cpsolver.exam.criteria.ExamRotationPenalty;
import org.cpsolver.exam.criteria.InstructorBackToBackConflicts;
import org.cpsolver.exam.criteria.InstructorDirectConflicts;
import org.cpsolver.exam.criteria.InstructorDistanceBackToBackConflicts;
import org.cpsolver.exam.criteria.InstructorMoreThan2ADayConflicts;
import org.cpsolver.exam.criteria.InstructorNotAvailableConflicts;
import org.cpsolver.exam.criteria.LargeExamsPenalty;
import org.cpsolver.exam.criteria.PeriodIndexPenalty;
import org.cpsolver.exam.criteria.PeriodPenalty;
import org.cpsolver.exam.criteria.PeriodSizePenalty;
import org.cpsolver.exam.criteria.PerturbationPenalty;
import org.cpsolver.exam.criteria.RoomPenalty;
import org.cpsolver.exam.criteria.RoomPerturbationPenalty;
import org.cpsolver.exam.criteria.RoomSizePenalty;
import org.cpsolver.exam.criteria.RoomSplitDistancePenalty;
import org.cpsolver.exam.criteria.RoomSplitPenalty;
import org.cpsolver.exam.criteria.StudentBackToBackConflicts;
import org.cpsolver.exam.criteria.StudentDirectConflicts;
import org.cpsolver.exam.criteria.StudentDistanceBackToBackConflicts;
import org.cpsolver.exam.criteria.StudentMoreThan2ADayConflicts;
import org.cpsolver.exam.criteria.StudentNotAvailableConflicts;
import org.cpsolver.exam.criteria.additional.DistanceToStronglyPreferredRoom;
import org.cpsolver.exam.criteria.additional.DistributionViolation;
import org.cpsolver.exam.criteria.additional.PeriodViolation;
import org.cpsolver.exam.criteria.additional.RoomViolation;
import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamInstructor;
import org.cpsolver.exam.model.ExamModel;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.exam.model.ExamStudent;
import org.cpsolver.exam.reports.ExamAssignments;
import org.cpsolver.exam.reports.ExamCourseSectionAssignments;
import org.cpsolver.exam.reports.ExamInstructorConflicts;
import org.cpsolver.exam.reports.ExamNbrMeetingsPerDay;
import org.cpsolver.exam.reports.ExamPeriodUsage;
import org.cpsolver.exam.reports.ExamRoomSchedule;
import org.cpsolver.exam.reports.ExamRoomSplit;
import org.cpsolver.exam.reports.ExamStudentBackToBackConflicts;
import org.cpsolver.exam.reports.ExamStudentConflicts;
import org.cpsolver.exam.reports.ExamStudentConflictsBySectionCourse;
import org.cpsolver.exam.reports.ExamStudentConflictsPerExam;
import org.cpsolver.exam.reports.ExamStudentDirectConflicts;
import org.cpsolver.exam.reports.ExamStudentMoreTwoADay;
import org.cpsolver.exam.split.ExamSplitter;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.DefaultParallelAssignment;
import org.cpsolver.ifs.assignment.DefaultSingleAssignment;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solution.SolutionListener;
import org.cpsolver.ifs.solver.ParallelSolver;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.ifs.util.ToolBox;
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
 * Usage:
 * <pre><code>java -Xmx1024m -jar examtt-1.1.jar exam.properties input.xml output.xml</code></pre>
 * <br>
 * 
 * @author  Tomas Muller
 * @version ExamTT 1.3 (Examination Timetabling)<br>
 *          Copyright (C) 2007 - 2014 Tomas Muller<br>
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
    private static org.apache.logging.log4j.Logger sLog = org.apache.logging.log4j.LogManager.getLogger(Test.class);
    private static java.text.DecimalFormat sDoubleFormat = new java.text.DecimalFormat("0.00", new java.text.DecimalFormatSymbols(Locale.US));

    /** Generate exam reports 
     * @param model problem model
     * @param assignment current assignment
     * @param outDir output folder
     * @param outName output file name prefix
     * @throws IOException may be thrown when writing fails
     **/
    public static void createReports(ExamModel model, Assignment<Exam, ExamPlacement> assignment, File outDir, String outName) throws IOException {
        new ExamAssignments(model).report(assignment).save(new File(outDir, outName + ".schdex.csv"));

        new ExamCourseSectionAssignments(model).report(assignment).save(new File(outDir, outName + ".schdcs.csv"));

        new ExamStudentConflicts(model).report(assignment).save(new File(outDir, outName + ".sconf.csv"));

        new ExamInstructorConflicts(model).report(assignment).save(new File(outDir, outName + ".iconf.csv"));

        new ExamStudentConflictsPerExam(model).report(assignment).save(new File(outDir, outName + ".sconfex.csv"));

        new ExamStudentDirectConflicts(model).report(assignment).save(new File(outDir, outName + ".sdir.csv"));

        new ExamStudentBackToBackConflicts(model).report(assignment).save(new File(outDir, outName + ".sbtb.csv"));

        new ExamStudentMoreTwoADay(model).report(assignment).save(new File(outDir, outName + ".sm2d.csv"));

        new ExamPeriodUsage(model).report(assignment).save(new File(outDir, outName + ".per.csv"));

        new ExamRoomSchedule(model).report(assignment).save(new File(outDir, outName + ".schdr.csv"));

        new ExamRoomSplit(model).report(assignment).save(new File(outDir, outName + ".rsplit.csv"));

        new ExamNbrMeetingsPerDay(model).report(assignment).save(new File(outDir, outName + ".distmpd.csv"));

        new ExamStudentConflictsBySectionCourse(model).report(assignment).save(new File(outDir, outName + ".sconfcs.csv"));
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
                sLog.info("Number of assigned variables is " + solution.getAssignment().nrAssignedVariables());
                sLog.info("Total value of the solution is " + solution.getModel().getTotalValue(solution.getAssignment()));

                File outFile = new File(iSolver.getProperties().getProperty("General.OutputFile",
                        iSolver.getProperties().getProperty("General.Output") + File.separator + "solution.xml"));
                FileOutputStream fos = new FileOutputStream(outFile);
                (new XMLWriter(fos, OutputFormat.createPrettyPrint())).write(((ExamModel) solution.getModel()).save(solution.getAssignment()));
                fos.flush();
                fos.close();

                if ("true".equals(System.getProperty("reports", "false")))
                    createReports((ExamModel) solution.getModel(), solution.getAssignment(), outFile.getParentFile(), outFile.getName()
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
            Assignment<Exam, ExamPlacement> assignment = solution.getAssignment();
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
                        + "," + model.getCriterion(PeriodPenalty.class).getBounds(assignment)[0]
                        + "," + model.getCriterion(RoomPenalty.class).getBounds(assignment)[0]
                        + "," + model.getCriterion(DistributionPenalty.class).getBounds(assignment)[0]
                        + ",," + df.format(rotation.averagePeriod(assignment)) + ","
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
                        + "," + model.getCriterion(PeriodPenalty.class).getBounds(assignment)[1]
                        + "," + model.getCriterion(RoomPenalty.class).getBounds(assignment)[1]
                        + "," + model.getCriterion(DistributionPenalty.class).getBounds(assignment)[1]
                        + ",," + rotation.nrAssignedExamsWithAvgPeriod(assignment) + ","
                        + ",,,"
                        + (largeSize >= 0 ? "," + model.getCriterion(LargeExamsPenalty.class).getBounds(assignment)[1] : "")
                        + (mpp ? ",," : "")
                        + (distStrPref == null ? "" : ",")
                        + (splitter == null ? "" : ",")
                        + (violPer == null ? "" : "," + model.getCriterion(PeriodViolation.class).getBounds(assignment)[1])
                        + (violRoom == null ? "" : "," + model.getCriterion(RoomViolation.class).getBounds(assignment)[1])
                        + (violDist == null ? "" : "," + model.getCriterion(DistributionViolation.class).getBounds(assignment)[1])
                        + ",,");
            }
            pw.println(ToolBox.getSeed()
                    + "," + model.getCriterion(StudentNotAvailableConflicts.class).getValue(assignment)
                    + "," + model.getCriterion(StudentDirectConflicts.class).getValue(assignment)
                    + "," + model.getCriterion(StudentMoreThan2ADayConflicts.class).getValue(assignment)
                    + "," + model.getCriterion(StudentBackToBackConflicts.class).getValue(assignment)
                    + (model.getBackToBackDistance() < 0 ? "" : "," + model.getCriterion(StudentDistanceBackToBackConflicts.class).getValue(assignment))
                    + "," + model.getCriterion(InstructorNotAvailableConflicts.class).getValue(assignment)
                    + "," + model.getCriterion(InstructorDirectConflicts.class).getValue(assignment)
                    + "," + model.getCriterion(InstructorMoreThan2ADayConflicts.class).getValue(assignment)
                    + "," + model.getCriterion(InstructorBackToBackConflicts.class).getValue(assignment)
                    + (model.getBackToBackDistance() < 0 ? "" : "," + model.getCriterion(InstructorDistanceBackToBackConflicts.class).getValue(assignment))
                    + "," + model.getCriterion(PeriodPenalty.class).getValue(assignment)
                    + "," + model.getCriterion(RoomPenalty.class).getValue(assignment)
                    + "," + model.getCriterion(DistributionPenalty.class).getValue(assignment)
                    + "," + df.format(model.getCriterion(PeriodIndexPenalty.class).getValue(assignment) / assignment.nrAssignedVariables())
                    + "," + df.format(Math.sqrt(rotation.getValue(assignment) / rotation.nrAssignedExamsWithAvgPeriod(assignment)) - 1)
                    + "," + df.format(model.getCriterion(PeriodSizePenalty.class).getValue(assignment) / assignment.nrAssignedVariables())
                    + "," + df.format(model.getCriterion(RoomSizePenalty.class).getValue(assignment) / assignment.nrAssignedVariables())
                    + "," + model.getCriterion(RoomSplitPenalty.class).getValue(assignment)
                    + "," + df.format(splitDistance.nrRoomSplits(assignment) <= 0 ? 0.0 : splitDistance.getValue(assignment) / splitDistance.nrRoomSplits(assignment))
                    + (largeSize >= 0 ? "," + model.getCriterion(LargeExamsPenalty.class).getValue(assignment) : "")
                    + (mpp ? "," + df.format(model.getCriterion(PerturbationPenalty.class).getValue(assignment) / assignment.nrAssignedVariables())
                           + "," + df.format(model.getCriterion(RoomPerturbationPenalty.class).getValue(assignment) / assignment.nrAssignedVariables()): "")
                    + (distStrPref == null ? "" : "," + df.format(distStrPref.getValue(assignment) / assignment.nrAssignedVariables()))
                    + (splitter == null ? "" : "," + df.format(splitter.getValue(assignment)))
                    + (violPer == null ? "" : "," + df.format(violPer.getValue(assignment)))
                    + (violRoom == null ? "" : "," + df.format(violRoom.getValue(assignment)))
                    + (violDist == null ? "" : "," + df.format(violDist.getValue(assignment)))
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
            cfg.setProperty("Neighbour.Class", "org.cpsolver.exam.heuristics.ExamNeighbourSelection");
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
            ToolBox.setupLogging(new File(outFile.getParent(), logName), "true".equals(System.getProperty("debug", "false")));

            ExamModel model = new ExamModel(cfg);

            Document document = (new SAXReader()).read(new File(cfg.getProperty("General.Input")));
            int nrSolvers = cfg.getPropertyInt("Parallel.NrSolvers", 1);
            Assignment<Exam, ExamPlacement> assignment = (nrSolvers <= 1 ? new DefaultSingleAssignment<Exam, ExamPlacement>() : new DefaultParallelAssignment<Exam, ExamPlacement>());
            model.load(document, assignment);

            Solver<Exam, ExamPlacement> solver = (nrSolvers == 1 ? new Solver<Exam, ExamPlacement>(cfg) : new ParallelSolver<Exam, ExamPlacement>(cfg));
            solver.setInitalSolution(new Solution<Exam, ExamPlacement>(model, assignment));

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
                    Assignment<Exam, ExamPlacement> a = solution.getAssignment();
                    if (sLog.isInfoEnabled()) {
                        sLog.info("**BEST[" + solution.getIteration() + "]** "
                                + (m.variables().size() > a.nrAssignedVariables() ? "V:" + a.nrAssignedVariables() + "/" + m.variables().size() + " - " : "") +
                                "T:" + new DecimalFormat("0.00").format(m.getTotalValue(a)) + " " + m.toString(a) +
                                (solution.getFailedIterations() > 0 ? ", F:" + sDoubleFormat.format(100.0 * solution.getFailedIterations() / solution.getIteration()) + "%" : ""));
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
