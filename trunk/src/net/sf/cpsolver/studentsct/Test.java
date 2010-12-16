package net.sf.cpsolver.studentsct;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import net.sf.cpsolver.ifs.heuristics.BacktrackNeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solution.SolutionListener;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.solver.SolverListener;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.JProf;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.check.CourseLimitCheck;
import net.sf.cpsolver.studentsct.check.InevitableStudentConflicts;
import net.sf.cpsolver.studentsct.check.OverlapCheck;
import net.sf.cpsolver.studentsct.check.SectionLimitCheck;
import net.sf.cpsolver.studentsct.extension.DistanceConflict;
import net.sf.cpsolver.studentsct.filter.CombinedStudentFilter;
import net.sf.cpsolver.studentsct.filter.FreshmanStudentFilter;
import net.sf.cpsolver.studentsct.filter.RandomStudentFilter;
import net.sf.cpsolver.studentsct.filter.ReverseStudentFilter;
import net.sf.cpsolver.studentsct.filter.StudentFilter;
import net.sf.cpsolver.studentsct.heuristics.StudentSctNeighbourSelection;
import net.sf.cpsolver.studentsct.heuristics.selection.BranchBoundSelection;
import net.sf.cpsolver.studentsct.heuristics.selection.OnlineSelection;
import net.sf.cpsolver.studentsct.heuristics.selection.SwapStudentSelection;
import net.sf.cpsolver.studentsct.heuristics.selection.BranchBoundSelection.BranchBoundNeighbour;
import net.sf.cpsolver.studentsct.heuristics.studentord.StudentOrder;
import net.sf.cpsolver.studentsct.heuristics.studentord.StudentRandomOrder;
import net.sf.cpsolver.studentsct.model.AcademicAreaCode;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Offering;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;
import net.sf.cpsolver.studentsct.report.CourseConflictTable;
import net.sf.cpsolver.studentsct.report.DistanceConflictTable;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/**
 * A main class for running of the student sectioning solver from command line. <br>
 * <br>
 * Usage:<br>
 * java -Xmx1024m -jar studentsct-1.1.jar config.properties [input_file]
 * [output_folder] [batch|online|simple]<br>
 * <br>
 * Modes:<br>
 * &nbsp;&nbsp;batch ... batch sectioning mode (default mode -- IFS solver with
 * {@link StudentSctNeighbourSelection} is used)<br>
 * &nbsp;&nbsp;online ... online sectioning mode (students are sectioned one by
 * one, sectioning info (expected/held space) is used)<br>
 * &nbsp;&nbsp;simple ... simple sectioning mode (students are sectioned one by
 * one, sectioning info is not used)<br>
 * See http://www.unitime.org for example configuration files and benchmark data
 * sets.<br>
 * <br>
 * 
 * The test does the following steps:
 * <ul>
 * <li>Provided property file is loaded (see {@link DataProperties}).
 * <li>Output folder is created (General.Output property) and logging is setup
 * (using log4j).
 * <li>Input data are loaded from the given XML file (calling
 * {@link StudentSectioningXMLLoader#load()}).
 * <li>Solver is executed (see {@link Solver}).
 * <li>Resultant solution is saved to an XML file (calling
 * {@link StudentSectioningXMLSaver#save()}.
 * </ul>
 * Also, a log and some reports (e.g., {@link CourseConflictTable} and
 * {@link DistanceConflictTable}) are created in the output folder.
 * 
 * <br>
 * <br>
 * Parameters:
 * <table border='1'>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>Test.LastLikeCourseDemands</td>
 * <td>{@link String}</td>
 * <td>Load last-like course demands from the given XML file (in the format that
 * is being used for last like course demand table in the timetabling
 * application)</td>
 * </tr>
 * <tr>
 * <td>Test.StudentInfos</td>
 * <td>{@link String}</td>
 * <td>Load last-like course demands from the given XML file (in the format that
 * is being used for last like course demand table in the timetabling
 * application)</td>
 * </tr>
 * <tr>
 * <td>Test.CrsReq</td>
 * <td>{@link String}</td>
 * <td>Load student requests from the given semi-colon separated list files (in
 * the format that is being used by the old MSF system)</td>
 * </tr>
 * <tr>
 * <td>Test.EtrChk</td>
 * <td>{@link String}</td>
 * <td>Load student information (academic area, classification, major, minor)
 * from the given semi-colon separated list files (in the format that is being
 * used by the old MSF system)</td>
 * </tr>
 * <tr>
 * <td>Sectioning.UseStudentPreferencePenalties</td>
 * <td>{@link Boolean}</td>
 * <td>If true, {@link StudentPreferencePenalties} are used (applicable only for
 * online sectioning)</td>
 * </tr>
 * <tr>
 * <td>Test.StudentOrder</td>
 * <td>{@link String}</td>
 * <td>A class that is used for ordering of students (must be an interface of
 * {@link StudentOrder}, default is {@link StudentRandomOrder}, not applicable
 * only for batch sectioning)</td>
 * </tr>
 * <tr>
 * <td>Test.CombineStudents</td>
 * <td>{@link File}</td>
 * <td>If provided, students are combined from the input file (last-like
 * students) and the provided file (real students). Real non-freshmen students
 * are taken from real data, last-like data are loaded on top of the real data
 * (all students, but weighted to occupy only the remaining space).</td>
 * </tr>
 * <tr>
 * <td>Test.CombineStudentsLastLike</td>
 * <td>{@link File}</td>
 * <td>If provided (together with Test.CombineStudents), students are combined
 * from the this file (last-like students) and Test.CombineStudents file (real
 * students). Real non-freshmen students are taken from real data, last-like
 * data are loaded on top of the real data (all students, but weighted to occupy
 * only the remaining space).</td>
 * </tr>
 * <tr>
 * <td>Test.CombineAcceptProb</td>
 * <td>{@link Double}</td>
 * <td>Used in combining students, probability of a non-freshmen real student to
 * be taken into the combined file (default is 1.0 -- all real non-freshmen
 * students are taken).</td>
 * </tr>
 * <tr>
 * <td>Test.FixPriorities</td>
 * <td>{@link Boolean}</td>
 * <td>If true, course/free time request priorities are corrected (to go from
 * zero, without holes or duplicates).</td>
 * </tr>
 * <tr>
 * <td>Test.ExtraStudents</td>
 * <td>{@link File}</td>
 * <td>If provided, students are loaded from the given file on top of the
 * students loaded from the ordinary input file (students with the same id are
 * skipped).</td>
 * </tr>
 * </table>
 * <br>
 * <br>
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
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
    private static java.text.SimpleDateFormat sDateFormat = new java.text.SimpleDateFormat("yyMMdd_HHmmss",
            java.util.Locale.US);
    private static DecimalFormat sDF = new DecimalFormat("0.000");

    /** Load student sectioning model */
    public static StudentSectioningModel loadModel(DataProperties cfg) {
        StudentSectioningModel model = null;
        try {
            if (cfg.getProperty("Test.CombineStudents") == null) {
                model = new StudentSectioningModel(cfg);
                new StudentSectioningXMLLoader(model).load();
            } else {
                model = combineStudents(cfg, new File(cfg.getProperty("Test.CombineStudentsLastLike", cfg.getProperty(
                        "General.Input", "." + File.separator + "solution.xml"))), new File(cfg
                        .getProperty("Test.CombineStudents")));
            }
            if (cfg.getProperty("Test.ExtraStudents") != null) {
                StudentSectioningXMLLoader extra = new StudentSectioningXMLLoader(model);
                extra.setInputFile(new File(cfg.getProperty("Test.ExtraStudents")));
                extra.setLoadOfferings(false);
                extra.setLoadStudents(true);
                extra.setStudentFilter(new ExtraStudentFilter(model));
                extra.load();
            }
            if (cfg.getProperty("Test.LastLikeCourseDemands") != null)
                loadLastLikeCourseDemandsXml(model, new File(cfg.getProperty("Test.LastLikeCourseDemands")));
            if (cfg.getProperty("Test.StudentInfos") != null)
                loadStudentInfoXml(model, new File(cfg.getProperty("Test.StudentInfos")));
            if (cfg.getProperty("Test.CrsReq") != null)
                loadCrsReqFiles(model, cfg.getProperty("Test.CrsReq"));
        } catch (Exception e) {
            sLog.error("Unable to load model, reason: " + e.getMessage(), e);
            return null;
        }
        if (cfg.getPropertyBoolean("Debug.DistanceConflict", false))
            DistanceConflict.sDebug = true;
        if (cfg.getPropertyBoolean("Debug.BranchBoundSelection", false))
            BranchBoundSelection.sDebug = true;
        if (cfg.getPropertyBoolean("Debug.SwapStudentsSelection", false))
            SwapStudentSelection.sDebug = true;
        if (cfg.getProperty("CourseRequest.SameTimePrecise") != null)
            CourseRequest.sSameTimePrecise = cfg.getPropertyBoolean("CourseRequest.SameTimePrecise", false);
        Logger.getLogger(BacktrackNeighbourSelection.class).setLevel(
                cfg.getPropertyBoolean("Debug.BacktrackNeighbourSelection", false) ? Level.DEBUG : Level.INFO);
        if (cfg.getPropertyBoolean("Test.FixPriorities", false))
            fixPriorities(model);
        return model;
    }

    /** Batch sectioning test */
    public static Solution<Request, Enrollment> batchSectioning(DataProperties cfg) {
        StudentSectioningModel model = loadModel(cfg);
        if (model == null)
            return null;

        if (cfg.getPropertyBoolean("Test.ComputeSectioningInfo", true))
            model.clearOnlineSectioningInfos();

        Solution<Request, Enrollment> solution = solveModel(model, cfg);

        printInfo(solution, cfg.getPropertyBoolean("Test.CreateReports", true), cfg.getPropertyBoolean(
                "Test.ComputeSectioningInfo", true), cfg.getPropertyBoolean("Test.RunChecks", true));

        try {
            Solver<Request, Enrollment> solver = new Solver<Request, Enrollment>(cfg);
            solver.setInitalSolution(solution);
            new StudentSectioningXMLSaver(solver).save(new File(new File(cfg.getProperty("General.Output", ".")),
                    "solution.xml"));
        } catch (Exception e) {
            sLog.error("Unable to save solution, reason: " + e.getMessage(), e);
        }

        saveInfoToXML(solution, null, new File(new File(cfg.getProperty("General.Output", ".")), "info.xml"));

        return solution;
    }

    /** Online sectioning test */
    @SuppressWarnings("unchecked")
    public static Solution<Request, Enrollment> onlineSectioning(DataProperties cfg) throws Exception {
        StudentSectioningModel model = loadModel(cfg);
        if (model == null)
            return null;

        Solution<Request, Enrollment> solution = new Solution<Request, Enrollment>(model, 0, 0);
        solution.addSolutionListener(new TestSolutionListener());
        double startTime = JProf.currentTimeSec();

        Solver<Request, Enrollment> solver = new Solver<Request, Enrollment>(cfg);
        solver.setInitalSolution(solution);
        solver.initSolver();

        OnlineSelection onlineSelection = new OnlineSelection(cfg);
        onlineSelection.init(solver);

        double totalPenalty = 0, minPenalty = 0, maxPenalty = 0;
        double minAvEnrlPenalty = 0, maxAvEnrlPenalty = 0;
        double totalPrefPenalty = 0, minPrefPenalty = 0, maxPrefPenalty = 0;
        double minAvEnrlPrefPenalty = 0, maxAvEnrlPrefPenalty = 0;
        int nrChoices = 0, nrEnrollments = 0, nrCourseRequests = 0;
        int chChoices = 0, chCourseRequests = 0, chStudents = 0;

        int choiceLimit = model.getProperties().getPropertyInt("Test.ChoicesLimit", -1);

        File outDir = new File(model.getProperties().getProperty("General.Output", "."));
        outDir.mkdirs();
        PrintWriter pw = new PrintWriter(new FileWriter(new File(outDir, "choices.csv")));

        List<Student> students = model.getStudents();
        try {
            Class studentOrdClass = Class.forName(model.getProperties().getProperty("Test.StudentOrder",
                    StudentRandomOrder.class.getName()));
            StudentOrder studentOrd = (StudentOrder) studentOrdClass.getConstructor(
                    new Class[] { DataProperties.class }).newInstance(new Object[] { model.getProperties() });
            students = studentOrd.order(model.getStudents());
        } catch (Exception e) {
            sLog.error("Unable to reorder students, reason: " + e.getMessage(), e);
        }

        for (Student student : students) {
            if (student.nrAssignedRequests() > 0)
                continue; // skip students with assigned courses (i.e., students
                          // already assigned by a batch sectioning process)
            sLog.info("Sectioning student: " + student);

            BranchBoundSelection.Selection selection = onlineSelection.getSelection(student);
            BranchBoundNeighbour neighbour = selection.select();
            if (neighbour != null) {
                StudentPreferencePenalties penalties = null;
                if (selection instanceof OnlineSelection.EpsilonSelection) {
                    OnlineSelection.EpsilonSelection epsSelection = (OnlineSelection.EpsilonSelection) selection;
                    penalties = epsSelection.getPenalties();
                    for (int i = 0; i < neighbour.getAssignment().length; i++) {
                        Request r = student.getRequests().get(i);
                        if (r instanceof CourseRequest) {
                            nrCourseRequests++;
                            chCourseRequests++;
                            int chChoicesThisRq = 0;
                            CourseRequest request = (CourseRequest) r;
                            for (Enrollment x : request.getAvaiableEnrollments()) {
                                nrEnrollments++;
                                if (epsSelection.isAllowed(i, x)) {
                                    nrChoices++;
                                    if (choiceLimit <= 0 || chChoicesThisRq < choiceLimit) {
                                        chChoices++;
                                        chChoicesThisRq++;
                                    }
                                }
                            }
                        }
                    }
                    chStudents++;
                    if (chStudents == 100) {
                        pw.println(sDF.format(((double) chChoices) / chCourseRequests));
                        pw.flush();
                        chStudents = 0;
                        chChoices = 0;
                        chCourseRequests = 0;
                    }
                }
                for (int i = 0; i < neighbour.getAssignment().length; i++) {
                    if (neighbour.getAssignment()[i] == null)
                        continue;
                    Enrollment enrollment = neighbour.getAssignment()[i];
                    if (enrollment.getRequest() instanceof CourseRequest) {
                        CourseRequest request = (CourseRequest) enrollment.getRequest();
                        double[] avEnrlMinMax = getMinMaxAvailableEnrollmentPenalty(request);
                        minAvEnrlPenalty += avEnrlMinMax[0];
                        maxAvEnrlPenalty += avEnrlMinMax[1];
                        totalPenalty += enrollment.getPenalty();
                        minPenalty += request.getMinPenalty();
                        maxPenalty += request.getMaxPenalty();
                        if (penalties != null) {
                            double[] avEnrlPrefMinMax = penalties.getMinMaxAvailableEnrollmentPenalty(enrollment
                                    .getRequest());
                            minAvEnrlPrefPenalty += avEnrlPrefMinMax[0];
                            maxAvEnrlPrefPenalty += avEnrlPrefMinMax[1];
                            totalPrefPenalty += penalties.getPenalty(enrollment);
                            minPrefPenalty += penalties.getMinPenalty(enrollment.getRequest());
                            maxPrefPenalty += penalties.getMaxPenalty(enrollment.getRequest());
                        }
                    }
                }
                neighbour.assign(solution.getIteration());
                sLog.info("Student " + student + " enrolls into " + neighbour);
                onlineSelection.updateSpace(student);
            } else {
                sLog.warn("No solution found.");
            }
            solution.update(JProf.currentTimeSec() - startTime);
        }

        if (chCourseRequests > 0)
            pw.println(sDF.format(((double) chChoices) / chCourseRequests));

        pw.flush();
        pw.close();

        solution.saveBest();

        printInfo(solution, cfg.getPropertyBoolean("Test.CreateReports", true), false, cfg.getPropertyBoolean(
                "Test.RunChecks", true));

        HashMap<String, String> extra = new HashMap<String, String>();
        sLog.info("Overall penalty is " + getPerc(totalPenalty, minPenalty, maxPenalty) + "% ("
                + sDF.format(totalPenalty) + "/" + sDF.format(minPenalty) + ".." + sDF.format(maxPenalty) + ")");
        extra.put("Overall penalty", getPerc(totalPenalty, minPenalty, maxPenalty) + "% (" + sDF.format(totalPenalty)
                + "/" + sDF.format(minPenalty) + ".." + sDF.format(maxPenalty) + ")");
        extra.put("Overall available enrollment penalty", getPerc(totalPenalty, minAvEnrlPenalty, maxAvEnrlPenalty)
                + "% (" + sDF.format(totalPenalty) + "/" + sDF.format(minAvEnrlPenalty) + ".."
                + sDF.format(maxAvEnrlPenalty) + ")");
        if (onlineSelection.isUseStudentPrefPenalties()) {
            sLog.info("Overall preference penalty is " + getPerc(totalPrefPenalty, minPrefPenalty, maxPrefPenalty)
                    + "% (" + sDF.format(totalPrefPenalty) + "/" + sDF.format(minPrefPenalty) + ".."
                    + sDF.format(maxPrefPenalty) + ")");
            extra.put("Overall preference penalty", getPerc(totalPrefPenalty, minPrefPenalty, maxPrefPenalty) + "% ("
                    + sDF.format(totalPrefPenalty) + "/" + sDF.format(minPrefPenalty) + ".."
                    + sDF.format(maxPrefPenalty) + ")");
            extra.put("Overall preference available enrollment penalty", getPerc(totalPrefPenalty,
                    minAvEnrlPrefPenalty, maxAvEnrlPrefPenalty)
                    + "% ("
                    + sDF.format(totalPrefPenalty)
                    + "/"
                    + sDF.format(minAvEnrlPrefPenalty)
                    + ".."
                    + sDF.format(maxAvEnrlPrefPenalty) + ")");
            extra.put("Average number of choices", sDF.format(((double) nrChoices) / nrCourseRequests) + " ("
                    + nrChoices + "/" + nrCourseRequests + ")");
            extra.put("Average number of enrollments", sDF.format(((double) nrEnrollments) / nrCourseRequests) + " ("
                    + nrEnrollments + "/" + nrCourseRequests + ")");
        }

        try {
            new StudentSectioningXMLSaver(solver).save(new File(new File(cfg.getProperty("General.Output", ".")),
                    "solution.xml"));
        } catch (Exception e) {
            sLog.error("Unable to save solution, reason: " + e.getMessage(), e);
        }

        saveInfoToXML(solution, extra, new File(new File(cfg.getProperty("General.Output", ".")), "info.xml"));

        return solution;
    }

    /**
     * Minimum and maximum enrollment penalty, i.e.,
     * {@link Enrollment#getPenalty()} of all enrollments
     */
    public static double[] getMinMaxEnrollmentPenalty(CourseRequest request) {
        List<Enrollment> enrollments = request.values();
        if (enrollments.isEmpty())
            return new double[] { 0, 0 };
        double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
        for (Enrollment enrollment : enrollments) {
            double penalty = enrollment.getPenalty();
            min = Math.min(min, penalty);
            max = Math.max(max, penalty);
        }
        return new double[] { min, max };
    }

    /**
     * Minimum and maximum available enrollment penalty, i.e.,
     * {@link Enrollment#getPenalty()} of all available enrollments
     */
    public static double[] getMinMaxAvailableEnrollmentPenalty(CourseRequest request) {
        List<Enrollment> enrollments = request.getAvaiableEnrollments();
        if (enrollments.isEmpty())
            return new double[] { 0, 0 };
        double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
        for (Enrollment enrollment : enrollments) {
            double penalty = enrollment.getPenalty();
            min = Math.min(min, penalty);
            max = Math.max(max, penalty);
        }
        return new double[] { min, max };
    }

    /**
     * Compute percentage
     * 
     * @param value
     *            current value
     * @param min
     *            minimal bound
     * @param max
     *            maximal bound
     * @return (value-min)/(max-min)
     */
    public static String getPerc(double value, double min, double max) {
        if (max == min)
            return sDF.format(100.0);
        return sDF.format(100.0 - 100.0 * (value - min) / (max - min));
    }

    /**
     * Print some information about the solution
     * 
     * @param solution
     *            given solution
     * @param computeTables
     *            true, if reports {@link CourseConflictTable} and
     *            {@link DistanceConflictTable} are to be computed as well
     * @param computeSectInfos
     *            true, if online sectioning infou is to be computed as well
     *            (see
     *            {@link StudentSectioningModel#computeOnlineSectioningInfos()})
     * @param runChecks
     *            true, if checks {@link OverlapCheck} and
     *            {@link SectionLimitCheck} are to be performed as well
     */
    public static void printInfo(Solution<Request, Enrollment> solution, boolean computeTables,
            boolean computeSectInfos, boolean runChecks) {
        StudentSectioningModel model = (StudentSectioningModel) solution.getModel();

        if (computeTables) {
            if (solution.getModel().assignedVariables().size() > 0) {
                try {
                    File outDir = new File(model.getProperties().getProperty("General.Output", "."));
                    outDir.mkdirs();
                    CourseConflictTable cct = new CourseConflictTable((StudentSectioningModel) solution.getModel());
                    cct.createTable(true, false).save(new File(outDir, "conflicts-lastlike.csv"));
                    cct.createTable(false, true).save(new File(outDir, "conflicts-real.csv"));

                    DistanceConflictTable dct = new DistanceConflictTable((StudentSectioningModel) solution.getModel());
                    dct.createTable(true, false).save(new File(outDir, "distances-lastlike.csv"));
                    dct.createTable(false, true).save(new File(outDir, "distances-real.csv"));
                } catch (IOException e) {
                    sLog.error(e.getMessage(), e);
                }
            }

            solution.saveBest();
        }

        if (computeSectInfos)
            model.computeOnlineSectioningInfos();

        if (runChecks) {
            try {
                if (model.getProperties().getPropertyBoolean("Test.InevitableStudentConflictsCheck", false)) {
                    InevitableStudentConflicts ch = new InevitableStudentConflicts(model);
                    if (!ch.check())
                        ch.getCSVFile().save(
                                new File(new File(model.getProperties().getProperty("General.Output", ".")),
                                        "inevitable-conflicts.csv"));
                }
            } catch (IOException e) {
                sLog.error(e.getMessage(), e);
            }
            new OverlapCheck(model).check();
            new SectionLimitCheck(model).check();
            try {
                CourseLimitCheck ch = new CourseLimitCheck(model);
                if (!ch.check())
                    ch.getCSVFile().save(
                            new File(new File(model.getProperties().getProperty("General.Output", ".")),
                                    "course-limits.csv"));
            } catch (IOException e) {
                sLog.error(e.getMessage(), e);
            }
        }

        sLog.info("Best solution found after " + solution.getBestTime() + " seconds (" + solution.getBestIteration()
                + " iterations).");
        sLog.info("Info: " + ToolBox.dict2string(solution.getExtendedInfo(), 2));
    }

    /** Solve the student sectioning problem using IFS solver */
    public static Solution<Request, Enrollment> solveModel(StudentSectioningModel model, DataProperties cfg) {
        Solver<Request, Enrollment> solver = new Solver<Request, Enrollment>(cfg);
        Solution<Request, Enrollment> solution = new Solution<Request, Enrollment>(model, 0, 0);
        solver.setInitalSolution(solution);
        if (cfg.getPropertyBoolean("Test.Verbose", false)) {
            solver.addSolverListener(new SolverListener<Request, Enrollment>() {
                public boolean variableSelected(long iteration, Request variable) {
                    return true;
                }

                public boolean valueSelected(long iteration, Request variable, Enrollment value) {
                    return true;
                }

                public boolean neighbourSelected(long iteration, Neighbour<Request, Enrollment> neighbour) {
                    sLog.debug("Select[" + iteration + "]: " + neighbour);
                    return true;
                }
            });
        }
        solution.addSolutionListener(new TestSolutionListener());

        solver.start();
        try {
            solver.getSolverThread().join();
        } catch (InterruptedException e) {
        }

        solution = solver.lastSolution();
        solution.restoreBest();

        printInfo(solution, false, false, false);

        return solution;
    }

    /**
     * Compute last-like student weight for the given course
     * 
     * @param course
     *            given course
     * @param real
     *            number of real students for the course
     * @param lastLike
     *            number of last-like students for the course
     * @return weight of a student request for the given course
     */
    public static double getLastLikeStudentWeight(Course course, int real, int lastLike) {
        int projected = course.getProjected();
        int limit = course.getLimit();
        if (course.getLimit() < 0) {
            sLog.debug("  -- Course " + course.getName() + " is unlimited.");
            return 1.0;
        }
        if (projected <= 0) {
            sLog.warn("  -- No projected demand for course " + course.getName() + ", using course limit (" + limit
                    + ")");
            projected = limit;
        } else if (limit < projected) {
            sLog.warn("  -- Projected number of students is over course limit for course " + course.getName() + " ("
                    + Math.round(projected) + ">" + limit + ")");
            projected = limit;
        }
        if (lastLike == 0) {
            sLog.warn("  -- No last like info for course " + course.getName());
            return 1.0;
        }
        double weight = ((double) Math.max(0, projected - real)) / lastLike;
        sLog.debug("  -- last like student weight for " + course.getName() + " is " + weight + " (lastLike=" + lastLike
                + ", real=" + real + ", projected=" + projected + ")");
        return weight;
    }

    /**
     * Load last-like students from an XML file (the one that is used to load
     * last like course demands table in the timetabling application)
     */
    public static void loadLastLikeCourseDemandsXml(StudentSectioningModel model, File xml) {
        try {
            Document document = (new SAXReader()).read(xml);
            Element root = document.getRootElement();
            HashMap<Course, List<Request>> requests = new HashMap<Course, List<Request>>();
            long reqId = 0;
            for (Iterator<?> i = root.elementIterator("student"); i.hasNext();) {
                Element studentEl = (Element) i.next();
                Student student = new Student(Long.parseLong(studentEl.attributeValue("externalId")));
                student.setDummy(true);
                int priority = 0;
                HashSet<Course> reqCourses = new HashSet<Course>();
                for (Iterator<?> j = studentEl.elementIterator("studentCourse"); j.hasNext();) {
                    Element courseEl = (Element) j.next();
                    String subjectArea = courseEl.attributeValue("subject");
                    String courseNbr = courseEl.attributeValue("courseNumber");
                    Course course = null;
                    offerings: for (Offering offering : model.getOfferings()) {
                        for (Course c : offering.getCourses()) {
                            if (c.getSubjectArea().equals(subjectArea) && c.getCourseNumber().equals(courseNbr)) {
                                course = c;
                                break offerings;
                            }
                        }
                    }
                    if (course == null && courseNbr.charAt(courseNbr.length() - 1) >= 'A'
                            && courseNbr.charAt(courseNbr.length() - 1) <= 'Z') {
                        String courseNbrNoSfx = courseNbr.substring(0, courseNbr.length() - 1);
                        offerings: for (Offering offering : model.getOfferings()) {
                            for (Course c : offering.getCourses()) {
                                if (c.getSubjectArea().equals(subjectArea)
                                        && c.getCourseNumber().equals(courseNbrNoSfx)) {
                                    course = c;
                                    break offerings;
                                }
                            }
                        }
                    }
                    if (course == null) {
                        sLog.warn("Course " + subjectArea + " " + courseNbr + " not found.");
                    } else {
                        if (!reqCourses.add(course)) {
                            sLog.warn("Course " + subjectArea + " " + courseNbr + " already requested.");
                        } else {
                            List<Course> courses = new ArrayList<Course>(1);
                            courses.add(course);
                            CourseRequest request = new CourseRequest(reqId++, priority++, false, student, courses,
                                    false);
                            List<Request> requestsThisCourse = requests.get(course);
                            if (requestsThisCourse == null) {
                                requestsThisCourse = new ArrayList<Request>();
                                requests.put(course, requestsThisCourse);
                            }
                            requestsThisCourse.add(request);
                        }
                    }
                }
                if (!student.getRequests().isEmpty())
                    model.addStudent(student);
            }
            for (Map.Entry<Course, List<Request>> entry : requests.entrySet()) {
                Course course = entry.getKey();
                List<Request> requestsThisCourse = entry.getValue();
                double weight = getLastLikeStudentWeight(course, 0, requestsThisCourse.size());
                for (Request request : requestsThisCourse) {
                    request.setWeight(weight);
                }
            }
        } catch (Exception e) {
            sLog.error(e.getMessage(), e);
        }
    }

    /**
     * Load course request from the given files (in the format being used by the
     * old MSF system)
     * 
     * @param model
     *            student sectioning model (with offerings loaded)
     * @param files
     *            semi-colon separated list of files to be loaded
     */
    public static void loadCrsReqFiles(StudentSectioningModel model, String files) {
        try {
            boolean lastLike = model.getProperties().getPropertyBoolean("Test.CrsReqIsLastLike", true);
            boolean shuffleIds = model.getProperties().getPropertyBoolean("Test.CrsReqShuffleStudentIds", true);
            boolean tryWithoutSuffix = model.getProperties().getPropertyBoolean("Test.CrsReqTryWithoutSuffix", false);
            HashMap<Long, Student> students = new HashMap<Long, Student>();
            long reqId = 0;
            for (StringTokenizer stk = new StringTokenizer(files, ";"); stk.hasMoreTokens();) {
                String file = stk.nextToken();
                sLog.debug("Loading " + file + " ...");
                BufferedReader in = new BufferedReader(new FileReader(file));
                String line;
                int lineIndex = 0;
                while ((line = in.readLine()) != null) {
                    lineIndex++;
                    if (line.length() <= 150)
                        continue;
                    char code = line.charAt(13);
                    if (code == 'H' || code == 'T')
                        continue; // skip header and tail
                    long studentId = Long.parseLong(line.substring(14, 23));
                    Student student = students.get(new Long(studentId));
                    if (student == null) {
                        student = new Student(studentId);
                        if (lastLike)
                            student.setDummy(true);
                        students.put(new Long(studentId), student);
                        sLog.debug("  -- loading student " + studentId + " ...");
                    } else
                        sLog.debug("  -- updating student " + studentId + " ...");
                    line = line.substring(150);
                    while (line.length() >= 20) {
                        String subjectArea = line.substring(0, 4).trim();
                        String courseNbr = line.substring(4, 8).trim();
                        if (subjectArea.length() == 0 || courseNbr.length() == 0) {
                            line = line.substring(20);
                            continue;
                        }
                        /*
                         * // UNUSED String instrSel = line.substring(8,10);
                         * //ZZ - Remove previous instructor selection char
                         * reqPDiv = line.charAt(10); //P - Personal preference;
                         * C - Conflict resolution; //0 - (Zero) used by program
                         * only, for change requests to reschedule division //
                         * (used to reschedule canceled division) String reqDiv
                         * = line.substring(11,13); //00 - Reschedule division
                         * String reqSect = line.substring(13,15); //Contains
                         * designator for designator-required courses String
                         * credit = line.substring(15,19); char nameRaise =
                         * line.charAt(19); //N - Name raise
                         */
                        char action = line.charAt(19); // A - Add; D - Drop; C -
                                                       // Change
                        sLog.debug("    -- requesting " + subjectArea + " " + courseNbr + " (action:" + action
                                + ") ...");
                        Course course = null;
                        offerings: for (Offering offering : model.getOfferings()) {
                            for (Course c : offering.getCourses()) {
                                if (c.getSubjectArea().equals(subjectArea) && c.getCourseNumber().equals(courseNbr)) {
                                    course = c;
                                    break offerings;
                                }
                            }
                        }
                        if (course == null && tryWithoutSuffix && courseNbr.charAt(courseNbr.length() - 1) >= 'A'
                                && courseNbr.charAt(courseNbr.length() - 1) <= 'Z') {
                            String courseNbrNoSfx = courseNbr.substring(0, courseNbr.length() - 1);
                            offerings: for (Offering offering : model.getOfferings()) {
                                for (Course c : offering.getCourses()) {
                                    if (c.getSubjectArea().equals(subjectArea)
                                            && c.getCourseNumber().equals(courseNbrNoSfx)) {
                                        course = c;
                                        break offerings;
                                    }
                                }
                            }
                        }
                        if (course == null) {
                            if (courseNbr.charAt(courseNbr.length() - 1) >= 'A'
                                    && courseNbr.charAt(courseNbr.length() - 1) <= 'Z') {
                            } else {
                                sLog.warn("      -- course " + subjectArea + " " + courseNbr + " not found (file "
                                        + file + ", line " + lineIndex + ")");
                            }
                        } else {
                            CourseRequest courseRequest = null;
                            for (Request request : student.getRequests()) {
                                if (request instanceof CourseRequest
                                        && ((CourseRequest) request).getCourses().contains(course)) {
                                    courseRequest = (CourseRequest) request;
                                    break;
                                }
                            }
                            if (action == 'A') {
                                if (courseRequest == null) {
                                    List<Course> courses = new ArrayList<Course>(1);
                                    courses.add(course);
                                    courseRequest = new CourseRequest(reqId++, student.getRequests().size(), false,
                                            student, courses, false);
                                } else {
                                    sLog.warn("      -- request for course " + course + " is already present");
                                }
                            } else if (action == 'D') {
                                if (courseRequest == null) {
                                    sLog.warn("      -- request for course " + course
                                            + " is not present -- cannot be dropped");
                                } else {
                                    student.getRequests().remove(courseRequest);
                                }
                            } else if (action == 'C') {
                                if (courseRequest == null) {
                                    sLog.warn("      -- request for course " + course
                                            + " is not present -- cannot be changed");
                                } else {
                                    // ?
                                }
                            } else {
                                sLog.warn("      -- unknown action " + action);
                            }
                        }
                        line = line.substring(20);
                    }
                }
                in.close();
            }
            HashMap<Course, List<Request>> requests = new HashMap<Course, List<Request>>();
            Set<Long> studentIds = new HashSet<Long>();
            for (Student student: students.values()) {
                if (!student.getRequests().isEmpty())
                    model.addStudent(student);
                if (shuffleIds) {
                    long newId = -1;
                    while (true) {
                        newId = 1 + (long) (999999999L * Math.random());
                        if (studentIds.add(new Long(newId)))
                            break;
                    }
                    student.setId(newId);
                }
                if (student.isDummy()) {
                    for (Request request : student.getRequests()) {
                        if (request instanceof CourseRequest) {
                            Course course = ((CourseRequest) request).getCourses().get(0);
                            List<Request> requestsThisCourse = requests.get(course);
                            if (requestsThisCourse == null) {
                                requestsThisCourse = new ArrayList<Request>();
                                requests.put(course, requestsThisCourse);
                            }
                            requestsThisCourse.add(request);
                        }
                    }
                }
            }
            Collections.sort(model.getStudents(), new Comparator<Student>() {
                public int compare(Student o1, Student o2) {
                    return Double.compare(o1.getId(), o2.getId());
                }
            });
            for (Map.Entry<Course, List<Request>> entry : requests.entrySet()) {
                Course course = entry.getKey();
                List<Request> requestsThisCourse = entry.getValue();
                double weight = getLastLikeStudentWeight(course, 0, requestsThisCourse.size());
                for (Request request : requestsThisCourse) {
                    request.setWeight(weight);
                }
            }
            if (model.getProperties().getProperty("Test.EtrChk") != null) {
                for (StringTokenizer stk = new StringTokenizer(model.getProperties().getProperty("Test.EtrChk"), ";"); stk
                        .hasMoreTokens();) {
                    String file = stk.nextToken();
                    sLog.debug("Loading " + file + " ...");
                    BufferedReader in = new BufferedReader(new FileReader(file));
                    String line;
                    int lineIndex = 0;
                    while ((line = in.readLine()) != null) {
                        lineIndex++;
                        if (line.length() < 55)
                            continue;
                        char code = line.charAt(12);
                        if (code == 'H' || code == 'T')
                            continue; // skip header and tail
                        if (code == 'D' || code == 'K')
                            continue; // skip delete nad cancel
                        long studentId = Long.parseLong(line.substring(2, 11));
                        Student student = students.get(new Long(studentId));
                        if (student == null) {
                            sLog.info("  -- student " + studentId + " not found");
                            continue;
                        }
                        sLog.info("  -- reading student " + studentId);
                        String area = line.substring(15, 18).trim();
                        if (area.length() == 0)
                            continue;
                        String clasf = line.substring(18, 20).trim();
                        String major = line.substring(21, 24).trim();
                        String minor = line.substring(24, 27).trim();
                        student.getAcademicAreaClasiffications().clear();
                        student.getMajors().clear();
                        student.getMinors().clear();
                        student.getAcademicAreaClasiffications().add(new AcademicAreaCode(area, clasf));
                        if (major.length() > 0)
                            student.getMajors().add(new AcademicAreaCode(area, major));
                        if (minor.length() > 0)
                            student.getMinors().add(new AcademicAreaCode(area, minor));
                    }
                }
            }
            int without = 0;
            for (Student student: students.values()) {
                if (student.getAcademicAreaClasiffications().isEmpty())
                    without++;
            }
            fixPriorities(model);
            sLog.info("Students without academic area: " + without);
        } catch (Exception e) {
            sLog.error(e.getMessage(), e);
        }
    }

    public static void fixPriorities(StudentSectioningModel model) {
        for (Student student : model.getStudents()) {
            Collections.sort(student.getRequests(), new Comparator<Request>() {
                public int compare(Request r1, Request r2) {
                    int cmp = Double.compare(r1.getPriority(), r2.getPriority());
                    if (cmp != 0)
                        return cmp;
                    return Double.compare(r1.getId(), r2.getId());
                }
            });
            int priority = 0;
            for (Request request : student.getRequests()) {
                if (priority != request.getPriority()) {
                    sLog.debug("Change priority of " + request + " to " + priority);
                    request.setPriority(priority);
                }
            }
        }
    }

    /** Load student infos from a given XML file. */
    public static void loadStudentInfoXml(StudentSectioningModel model, File xml) {
        try {
            sLog.info("Loading student infos from " + xml);
            Document document = (new SAXReader()).read(xml);
            Element root = document.getRootElement();
            HashMap<Long, Student> studentTable = new HashMap<Long, Student>();
            for (Student student : model.getStudents()) {
                studentTable.put(new Long(student.getId()), student);
            }
            for (Iterator<?> i = root.elementIterator("student"); i.hasNext();) {
                Element studentEl = (Element) i.next();
                Student student = studentTable.get(Long.valueOf(studentEl.attributeValue("externalId")));
                if (student == null) {
                    sLog.debug(" -- student " + studentEl.attributeValue("externalId") + " not found");
                    continue;
                }
                sLog.debug(" -- loading info for student " + student);
                student.getAcademicAreaClasiffications().clear();
                if (studentEl.element("studentAcadAreaClass") != null)
                    for (Iterator<?> j = studentEl.element("studentAcadAreaClass").elementIterator("acadAreaClass"); j
                            .hasNext();) {
                        Element studentAcadAreaClassElement = (Element) j.next();
                        student.getAcademicAreaClasiffications().add(
                                new AcademicAreaCode(studentAcadAreaClassElement.attributeValue("academicArea"),
                                        studentAcadAreaClassElement.attributeValue("academicClass")));
                    }
                sLog.debug("   -- acad areas classifs " + student.getAcademicAreaClasiffications());
                student.getMajors().clear();
                if (studentEl.element("studentMajors") != null)
                    for (Iterator<?> j = studentEl.element("studentMajors").elementIterator("major"); j.hasNext();) {
                        Element studentMajorElement = (Element) j.next();
                        student.getMajors().add(
                                new AcademicAreaCode(studentMajorElement.attributeValue("academicArea"),
                                        studentMajorElement.attributeValue("code")));
                    }
                sLog.debug("   -- majors " + student.getMajors());
                student.getMinors().clear();
                if (studentEl.element("studentMinors") != null)
                    for (Iterator<?> j = studentEl.element("studentMinors").elementIterator("minor"); j.hasNext();) {
                        Element studentMinorElement = (Element) j.next();
                        student.getMinors().add(
                                new AcademicAreaCode(studentMinorElement.attributeValue("academicArea", ""),
                                        studentMinorElement.attributeValue("code", "")));
                    }
                sLog.debug("   -- minors " + student.getMinors());
            }
        } catch (Exception e) {
            sLog.error(e.getMessage(), e);
        }
    }

    /** Save solution info as XML */
    public static void saveInfoToXML(Solution<Request, Enrollment> solution, HashMap<String, String> extra, File file) {
        FileOutputStream fos = null;
        try {
            Document document = DocumentHelper.createDocument();
            document.addComment("Solution Info");

            Element root = document.addElement("info");
            TreeSet<Map.Entry<String, String>> entrySet = new TreeSet<Map.Entry<String, String>>(
                    new Comparator<Map.Entry<String, String>>() {
                        public int compare(Map.Entry<String, String> e1, Map.Entry<String, String> e2) {
                            return e1.getKey().compareTo(e2.getKey());
                        }
                    });
            entrySet.addAll(solution.getExtendedInfo().entrySet());
            if (extra != null)
                entrySet.addAll(extra.entrySet());
            for (Map.Entry<String, String> entry : entrySet) {
                root.addElement("property").addAttribute("name", entry.getKey()).setText(entry.getValue());
            }

            fos = new FileOutputStream(file);
            (new XMLWriter(fos, OutputFormat.createPrettyPrint())).write(document);
            fos.flush();
            fos.close();
            fos = null;
        } catch (Exception e) {
            sLog.error("Unable to save info, reason: " + e.getMessage(), e);
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
            }
        }
    }

    private static void fixWeights(StudentSectioningModel model) {
        HashMap<Course, Integer> lastLike = new HashMap<Course, Integer>();
        HashMap<Course, Integer> real = new HashMap<Course, Integer>();
        HashSet<Long> lastLikeIds = new HashSet<Long>();
        HashSet<Long> realIds = new HashSet<Long>();
        for (Student student : model.getStudents()) {
            if (student.isDummy()) {
                if (!lastLikeIds.add(new Long(student.getId()))) {
                    sLog.error("Two last-like student with id " + student.getId());
                }
            } else {
                if (!realIds.add(new Long(student.getId()))) {
                    sLog.error("Two real student with id " + student.getId());
                }
            }
            for (Request request : student.getRequests()) {
                if (request instanceof CourseRequest) {
                    CourseRequest courseRequest = (CourseRequest) request;
                    Course course = courseRequest.getCourses().get(0);
                    Integer cnt = (student.isDummy() ? lastLike : real).get(course);
                    (student.isDummy() ? lastLike : real).put(course, new Integer(
                            (cnt == null ? 0 : cnt.intValue()) + 1));
                }
            }
        }
        for (Student student : new ArrayList<Student>(model.getStudents())) {
            if (student.isDummy() && realIds.contains(new Long(student.getId()))) {
                sLog.warn("There is both last-like and real student with id " + student.getId());
                long newId = -1;
                while (true) {
                    newId = 1 + (long) (999999999L * Math.random());
                    if (!realIds.contains(new Long(newId)) && !lastLikeIds.contains(new Long(newId)))
                        break;
                }
                lastLikeIds.remove(new Long(student.getId()));
                lastLikeIds.add(new Long(newId));
                student.setId(newId);
                sLog.warn("  -- last-like student id changed to " + student.getId());
            }
            for (Request request : new ArrayList<Request>(student.getRequests())) {
                if (!student.isDummy()) {
                    request.setWeight(1.0);
                    continue;
                }
                if (request instanceof CourseRequest) {
                    CourseRequest courseRequest = (CourseRequest) request;
                    Course course = courseRequest.getCourses().get(0);
                    Integer lastLikeCnt = lastLike.get(course);
                    Integer realCnt = real.get(course);
                    courseRequest.setWeight(getLastLikeStudentWeight(course, realCnt == null ? 0 : realCnt.intValue(),
                            lastLikeCnt == null ? 0 : lastLikeCnt.intValue()));
                } else
                    request.setWeight(1.0);
                if (request.getWeight() <= 0.0) {
                    model.removeVariable(request);
                    student.getRequests().remove(request);
                }
            }
            if (student.getRequests().isEmpty()) {
                model.getStudents().remove(student);
            }
        }
    }

    /** Combine students from the provided two files */
    public static StudentSectioningModel combineStudents(DataProperties cfg, File lastLikeStudentData,
            File realStudentData) {
        try {
            RandomStudentFilter rnd = new RandomStudentFilter(1.0);

            StudentSectioningModel model = null;

            for (StringTokenizer stk = new StringTokenizer(cfg.getProperty("Test.CombineAcceptProb", "1.0"), ","); stk
                    .hasMoreTokens();) {
                double acceptProb = Double.parseDouble(stk.nextToken());
                sLog.info("Test.CombineAcceptProb=" + acceptProb);
                rnd.setProbability(acceptProb);

                StudentFilter batchFilter = new CombinedStudentFilter(new ReverseStudentFilter(
                        new FreshmanStudentFilter()), rnd, CombinedStudentFilter.OP_AND);

                model = new StudentSectioningModel(cfg);
                StudentSectioningXMLLoader loader = new StudentSectioningXMLLoader(model);
                loader.setLoadStudents(false);
                loader.load();

                StudentSectioningXMLLoader lastLikeLoader = new StudentSectioningXMLLoader(model);
                lastLikeLoader.setInputFile(lastLikeStudentData);
                lastLikeLoader.setLoadOfferings(false);
                lastLikeLoader.setLoadStudents(true);
                lastLikeLoader.load();

                StudentSectioningXMLLoader realLoader = new StudentSectioningXMLLoader(model);
                realLoader.setInputFile(realStudentData);
                realLoader.setLoadOfferings(false);
                realLoader.setLoadStudents(true);
                realLoader.setStudentFilter(batchFilter);
                realLoader.load();

                fixWeights(model);

                fixPriorities(model);

                Solver<Request, Enrollment> solver = new Solver<Request, Enrollment>(model.getProperties());
                solver.setInitalSolution(model);
                new StudentSectioningXMLSaver(solver).save(new File(new File(model.getProperties().getProperty(
                        "General.Output", ".")), "solution-r" + ((int) (100.0 * acceptProb)) + ".xml"));

            }

            return model;

        } catch (Exception e) {
            sLog.error("Unable to combine students, reason: " + e.getMessage(), e);
            return null;
        }
    }

    /** Main */
    public static void main(String[] args) {
        try {
            DataProperties cfg = new DataProperties();
            cfg.setProperty("Termination.Class", "net.sf.cpsolver.ifs.termination.GeneralTerminationCondition");
            cfg.setProperty("Termination.StopWhenComplete", "true");
            cfg.setProperty("Termination.TimeOut", "600");
            cfg.setProperty("Comparator.Class", "net.sf.cpsolver.ifs.solution.GeneralSolutionComparator");
            cfg.setProperty("Value.Class", "net.sf.cpsolver.studentsct.heuristics.EnrollmentSelection");// net.sf.cpsolver.ifs.heuristics.GeneralValueSelection
            cfg.setProperty("Value.WeightConflicts", "1.0");
            cfg.setProperty("Value.WeightNrAssignments", "0.0");
            cfg.setProperty("Variable.Class", "net.sf.cpsolver.ifs.heuristics.GeneralVariableSelection");
            cfg.setProperty("Neighbour.Class", "net.sf.cpsolver.studentsct.heuristics.StudentSctNeighbourSelection");
            cfg.setProperty("General.SaveBestUnassigned", "-1");
            cfg
                    .setProperty("Extensions.Classes",
                            "net.sf.cpsolver.ifs.extension.ConflictStatistics;net.sf.cpsolver.studentsct.extension.DistanceConflict");
            cfg.setProperty("Data.Initiative", "puWestLafayetteTrdtn");
            cfg.setProperty("Data.Term", "Fal");
            cfg.setProperty("Data.Year", "2007");
            cfg.setProperty("General.Input", "pu-sectll-fal07-s.xml");
            if (args.length >= 1) {
                cfg.load(new FileInputStream(args[0]));
            }
            cfg.putAll(System.getProperties());

            if (args.length >= 2) {
                cfg.setProperty("General.Input", args[1]);
            }

            if (args.length >= 3) {
                File logFile = new File(ToolBox.configureLogging(args[2] + File.separator
                        + (sDateFormat.format(new Date())), cfg, false, false));
                cfg.setProperty("General.Output", logFile.getParentFile().getAbsolutePath());
            } else if (cfg.getProperty("General.Output") != null) {
                cfg.setProperty("General.Output", cfg.getProperty("General.Output", ".") + File.separator
                        + (sDateFormat.format(new Date())));
                ToolBox.configureLogging(cfg.getProperty("General.Output", "."), cfg, false, false);
            } else {
                ToolBox.configureLogging();
                cfg.setProperty("General.Output", System.getProperty("user.home", ".") + File.separator
                        + "Sectioning-Test" + File.separator + (sDateFormat.format(new Date())));
            }

            if (args.length >= 4 && "online".equals(args[3])) {
                onlineSectioning(cfg);
            } else if (args.length >= 4 && "simple".equals(args[3])) {
                cfg.setProperty("Sectioning.UseOnlinePenalties", "false");
                onlineSectioning(cfg);
            } else {
                batchSectioning(cfg);
            }
        } catch (Exception e) {
            sLog.error(e.getMessage(), e);
            e.printStackTrace();
        }
    }

    public static class ExtraStudentFilter implements StudentFilter {
        HashSet<Long> iIds = new HashSet<Long>();

        public ExtraStudentFilter(StudentSectioningModel model) {
            for (Student student : model.getStudents()) {
                iIds.add(new Long(student.getId()));
            }
        }

        public boolean accept(Student student) {
            return !iIds.contains(new Long(student.getId()));
        }
    }

    public static class TestSolutionListener implements SolutionListener<Request, Enrollment> {
        public void solutionUpdated(Solution<Request, Enrollment> solution) {
        }

        public void getInfo(Solution<Request, Enrollment> solution, Map<String, String> info) {
        }

        public void getInfo(Solution<Request, Enrollment> solution, Map<String, String> info, Collection<Request> variables) {
        }

        public void bestCleared(Solution<Request, Enrollment> solution) {
        }

        public void bestSaved(Solution<Request, Enrollment> solution) {
            StudentSectioningModel m = (StudentSectioningModel) solution.getModel();
            sLog.debug("**BEST** "
                    + (m.getNrRealStudents(false) > 0 ? "RRq:" + m.getNrAssignedRealRequests(false) + "/"
                            + m.getNrRealRequests(false) + ", " : "")
                    + (m.getNrLastLikeStudents(false) > 0 ? "DRq:" + m.getNrAssignedLastLikeRequests(false) + "/"
                            + m.getNrLastLikeRequests(false) + ", " : "")
                    + (m.getNrRealStudents(false) > 0 ? "RS:" + m.getNrCompleteRealStudents(false) + "/"
                            + m.getNrRealStudents(false) + ", " : "")
                    + (m.getNrLastLikeStudents(false) > 0 ? "DS:" + m.getNrCompleteLastLikeStudents(false) + "/"
                            + m.getNrLastLikeStudents(false) + ", " : "")
                    + "V:"
                    + sDF.format(m.getTotalValue())
                    + (m.getDistanceConflict() == null ? "" : ", DC:"
                            + sDF.format(m.getDistanceConflict().getTotalNrConflicts())) 
                    + ", %: " + sDF.format(-100.0 * m.getTotalValue() / m.getStudents().size()));
        }

        public void bestRestored(Solution<Request, Enrollment> solution) {
        }
    }
}
