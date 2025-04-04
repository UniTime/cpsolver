package org.cpsolver.studentsct;

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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.DefaultSingleAssignment;
import org.cpsolver.ifs.assignment.EmptyAssignment;
import org.cpsolver.ifs.heuristics.BacktrackNeighbourSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solution.SolutionListener;
import org.cpsolver.ifs.solver.ParallelSolver;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.solver.SolverListener;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.JProf;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.ifs.util.ProgressWriter;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.studentsct.check.CourseLimitCheck;
import org.cpsolver.studentsct.check.InevitableStudentConflicts;
import org.cpsolver.studentsct.check.OverlapCheck;
import org.cpsolver.studentsct.check.SectionLimitCheck;
import org.cpsolver.studentsct.extension.DistanceConflict;
import org.cpsolver.studentsct.extension.TimeOverlapsCounter;
import org.cpsolver.studentsct.filter.CombinedStudentFilter;
import org.cpsolver.studentsct.filter.FreshmanStudentFilter;
import org.cpsolver.studentsct.filter.RandomStudentFilter;
import org.cpsolver.studentsct.filter.ReverseStudentFilter;
import org.cpsolver.studentsct.filter.StudentFilter;
import org.cpsolver.studentsct.heuristics.StudentSctNeighbourSelection;
import org.cpsolver.studentsct.heuristics.selection.BranchBoundSelection;
import org.cpsolver.studentsct.heuristics.selection.OnlineSelection;
import org.cpsolver.studentsct.heuristics.selection.SwapStudentSelection;
import org.cpsolver.studentsct.heuristics.selection.BranchBoundSelection.BranchBoundNeighbour;
import org.cpsolver.studentsct.heuristics.studentord.StudentOrder;
import org.cpsolver.studentsct.heuristics.studentord.StudentRandomOrder;
import org.cpsolver.studentsct.model.AreaClassificationMajor;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Offering;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.report.CourseConflictTable;
import org.cpsolver.studentsct.report.DistanceConflictTable;
import org.cpsolver.studentsct.report.RequestGroupTable;
import org.cpsolver.studentsct.report.RequestPriorityTable;
import org.cpsolver.studentsct.report.SectionConflictTable;
import org.cpsolver.studentsct.report.SolutionStatsReport;
import org.cpsolver.studentsct.report.TableauReport;
import org.cpsolver.studentsct.report.TimeOverlapConflictTable;
import org.cpsolver.studentsct.report.UnbalancedSectionsTable;
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
 * <table border='1'><caption>Related Solver Parameters</caption>
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
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
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
    private static java.text.SimpleDateFormat sDateFormat = new java.text.SimpleDateFormat("yyMMdd_HHmmss",
            java.util.Locale.US);
    private static DecimalFormat sDF = new DecimalFormat("0.000");

    /** Load student sectioning model 
     * @param cfg solver configuration
     * @return loaded solution
     **/
    public static Solution<Request, Enrollment> load(DataProperties cfg) {
        StudentSectioningModel model = null;
        Assignment<Request, Enrollment> assignment = null;
        try {
            if (cfg.getProperty("Test.CombineStudents") == null) {
                model = new StudentSectioningModel(cfg);
                assignment = new DefaultSingleAssignment<Request, Enrollment>();
                new StudentSectioningXMLLoader(model, assignment).load();
            } else {
                Solution<Request, Enrollment> solution = combineStudents(cfg,
                        new File(cfg.getProperty("Test.CombineStudentsLastLike", cfg.getProperty("General.Input", "." + File.separator + "solution.xml"))),
                        new File(cfg.getProperty("Test.CombineStudents")));
                model = (StudentSectioningModel)solution.getModel();
                assignment = solution.getAssignment();
            }
            if (cfg.getProperty("Test.ExtraStudents") != null) {
                StudentSectioningXMLLoader extra = new StudentSectioningXMLLoader(model, assignment);
                extra.setInputFile(new File(cfg.getProperty("Test.ExtraStudents")));
                extra.setLoadOfferings(false);
                extra.setLoadStudents(true);
                extra.setStudentFilter(new ExtraStudentFilter(model));
                extra.load();
            }
            if (cfg.getProperty("Test.LastLikeCourseDemands") != null)
                loadLastLikeCourseDemandsXml(model, new File(cfg.getProperty("Test.LastLikeCourseDemands")));
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
        if (cfg.getPropertyBoolean("Debug.TimeOverlaps", false))
            TimeOverlapsCounter.sDebug = true;
        if (cfg.getProperty("CourseRequest.SameTimePrecise") != null)
            CourseRequest.sSameTimePrecise = cfg.getPropertyBoolean("CourseRequest.SameTimePrecise", false);
        Configurator.setLevel(BacktrackNeighbourSelection.class.getName(),
                cfg.getPropertyBoolean("Debug.BacktrackNeighbourSelection", false) ? Level.DEBUG : Level.INFO);
        if (cfg.getPropertyBoolean("Test.FixPriorities", false))
            fixPriorities(model);
        return new Solution<Request, Enrollment>(model, assignment);
    }

    /** Batch sectioning test 
     * @param cfg solver configuration
     * @return resultant solution
     **/
    public static Solution<Request, Enrollment> batchSectioning(DataProperties cfg) {
        Solution<Request, Enrollment> solution = load(cfg);
        if (solution == null)
            return null;
        StudentSectioningModel model = (StudentSectioningModel)solution.getModel();

        if (cfg.getPropertyBoolean("Test.ComputeSectioningInfo", true))
            model.clearOnlineSectioningInfos();
        
        Progress.getInstance(model).addProgressListener(new ProgressWriter(System.out));

        solve(solution, cfg);

        return solution;
    }

    /** Online sectioning test 
     * @param cfg solver configuration
     * @return resultant solution
     * @throws Exception thrown when the sectioning fails
     **/
    public static Solution<Request, Enrollment> onlineSectioning(DataProperties cfg) throws Exception {
        Solution<Request, Enrollment> solution = load(cfg);
        if (solution == null)
            return null;
        StudentSectioningModel model = (StudentSectioningModel)solution.getModel();
        Assignment<Request, Enrollment> assignment = solution.getAssignment();

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
            @SuppressWarnings("rawtypes")
            Class studentOrdClass = Class.forName(model.getProperties().getProperty("Test.StudentOrder", StudentRandomOrder.class.getName()));
            @SuppressWarnings("unchecked")
            StudentOrder studentOrd = (StudentOrder) studentOrdClass.getConstructor(new Class[] { DataProperties.class }).newInstance(new Object[] { model.getProperties() });
            students = studentOrd.order(model.getStudents());
        } catch (Exception e) {
            sLog.error("Unable to reorder students, reason: " + e.getMessage(), e);
        }
        
        ShutdownHook hook = new ShutdownHook(solver);
        Runtime.getRuntime().addShutdownHook(hook);

        for (Student student : students) {
            if (student.nrAssignedRequests(assignment) > 0)
                continue; // skip students with assigned courses (i.e., students
                          // already assigned by a batch sectioning process)
            sLog.info("Sectioning student: " + student);

            BranchBoundSelection.Selection selection = onlineSelection.getSelection(assignment, student);
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
                            for (Enrollment x : request.getAvaiableEnrollments(assignment)) {
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
                        double[] avEnrlMinMax = getMinMaxAvailableEnrollmentPenalty(assignment, request);
                        minAvEnrlPenalty += avEnrlMinMax[0];
                        maxAvEnrlPenalty += avEnrlMinMax[1];
                        totalPenalty += enrollment.getPenalty();
                        minPenalty += request.getMinPenalty();
                        maxPenalty += request.getMaxPenalty();
                        if (penalties != null) {
                            double[] avEnrlPrefMinMax = penalties.getMinMaxAvailableEnrollmentPenalty(assignment, enrollment.getRequest());
                            minAvEnrlPrefPenalty += avEnrlPrefMinMax[0];
                            maxAvEnrlPrefPenalty += avEnrlPrefMinMax[1];
                            totalPrefPenalty += penalties.getPenalty(enrollment);
                            minPrefPenalty += penalties.getMinPenalty(enrollment.getRequest());
                            maxPrefPenalty += penalties.getMaxPenalty(enrollment.getRequest());
                        }
                    }
                }
                neighbour.assign(assignment, solution.getIteration());
                sLog.info("Student " + student + " enrolls into " + neighbour);
                onlineSelection.updateSpace(assignment, student);
            } else {
                sLog.warn("No solution found.");
            }
            solution.update(JProf.currentTimeSec() - startTime);
            solution.saveBest();
        }

        if (chCourseRequests > 0)
            pw.println(sDF.format(((double) chChoices) / chCourseRequests));

        pw.flush();
        pw.close();
        
        HashMap<String, String> extra = new HashMap<String, String>();
        sLog.info("Overall penalty is " + getPerc(totalPenalty, minPenalty, maxPenalty) + "% ("
                + sDF.format(totalPenalty) + "/" + sDF.format(minPenalty) + ".." + sDF.format(maxPenalty) + ")");
        extra.put("Overall penalty", getPerc(totalPenalty, minPenalty, maxPenalty) + "% (" + sDF.format(totalPenalty)
                + "/" + sDF.format(minPenalty) + ".." + sDF.format(maxPenalty) + ")");
        extra.put("Overall available enrollment penalty", getPerc(totalPenalty, minAvEnrlPenalty, maxAvEnrlPenalty)
                + "% (" + sDF.format(totalPenalty) + "/" + sDF.format(minAvEnrlPenalty) + ".." + sDF.format(maxAvEnrlPenalty) + ")");
        if (onlineSelection.isUseStudentPrefPenalties()) {
            sLog.info("Overall preference penalty is " + getPerc(totalPrefPenalty, minPrefPenalty, maxPrefPenalty)
                    + "% (" + sDF.format(totalPrefPenalty) + "/" + sDF.format(minPrefPenalty) + ".." + sDF.format(maxPrefPenalty) + ")");
            extra.put("Overall preference penalty", getPerc(totalPrefPenalty, minPrefPenalty, maxPrefPenalty) + "% ("
                    + sDF.format(totalPrefPenalty) + "/" + sDF.format(minPrefPenalty) + ".." + sDF.format(maxPrefPenalty) + ")");
            extra.put("Overall preference available enrollment penalty", getPerc(totalPrefPenalty,
                    minAvEnrlPrefPenalty, maxAvEnrlPrefPenalty)
                    + "% (" + sDF.format(totalPrefPenalty) + "/" + sDF.format(minAvEnrlPrefPenalty) + ".." + sDF.format(maxAvEnrlPrefPenalty) + ")");
            extra.put("Average number of choices", sDF.format(((double) nrChoices) / nrCourseRequests) + " ("
                    + nrChoices + "/" + nrCourseRequests + ")");
            extra.put("Average number of enrollments", sDF.format(((double) nrEnrollments) / nrCourseRequests) + " ("
                    + nrEnrollments + "/" + nrCourseRequests + ")");
        }
        hook.setExtra(extra);

        return solution;
    }

    /**
     * Minimum and maximum enrollment penalty, i.e.,
     * {@link Enrollment#getPenalty()} of all enrollments
     * @param request a course request
     * @return minimum and maximum of the enrollment penalty
     */
    public static double[] getMinMaxEnrollmentPenalty(CourseRequest request) {
        List<Enrollment> enrollments = request.values(new EmptyAssignment<Request, Enrollment>());
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
     * @param assignment current assignment
     * @param request a course request
     * @return minimum and maximum of the available enrollment penalty
     */
    public static double[] getMinMaxAvailableEnrollmentPenalty(Assignment<Request, Enrollment> assignment, CourseRequest request) {
        List<Enrollment> enrollments = request.getAvaiableEnrollments(assignment);
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
     *            {@link StudentSectioningModel#computeOnlineSectioningInfos(Assignment)})
     * @param runChecks
     *            true, if checks {@link OverlapCheck} and
     *            {@link SectionLimitCheck} are to be performed as well
     */
    public static void printInfo(Solution<Request, Enrollment> solution, boolean computeTables, boolean computeSectInfos, boolean runChecks) {
        StudentSectioningModel model = (StudentSectioningModel) solution.getModel();

        if (computeTables) {
            if (solution.getModel().assignedVariables(solution.getAssignment()).size() > 0) {
                try {
                    DataProperties lastlike = new DataProperties();
                    lastlike.setProperty("lastlike", "true");
                    lastlike.setProperty("real", "false");
                    lastlike.setProperty("useAmPm", "true");
                    DataProperties real = new DataProperties();
                    real.setProperty("lastlike", "false");
                    real.setProperty("real", "true");
                    real.setProperty("useAmPm", "true");
                    
                    File outDir = new File(model.getProperties().getProperty("General.Output", "."));
                    outDir.mkdirs();
                    CourseConflictTable cct = new CourseConflictTable((StudentSectioningModel) solution.getModel());
                    cct.createTable(solution.getAssignment(), lastlike).save(new File(outDir, "conflicts-lastlike.csv"));
                    cct.createTable(solution.getAssignment(), real).save(new File(outDir, "conflicts-real.csv"));

                    DistanceConflictTable dct = new DistanceConflictTable((StudentSectioningModel) solution.getModel());
                    dct.createTable(solution.getAssignment(), lastlike).save(new File(outDir, "distances-lastlike.csv"));
                    dct.createTable(solution.getAssignment(), real).save(new File(outDir, "distances-real.csv"));
                    
                    SectionConflictTable sct = new SectionConflictTable((StudentSectioningModel) solution.getModel(), SectionConflictTable.Type.OVERLAPS);
                    sct.createTable(solution.getAssignment(), lastlike).save(new File(outDir, "time-conflicts-lastlike.csv"));
                    sct.createTable(solution.getAssignment(), real).save(new File(outDir, "time-conflicts-real.csv"));
                    
                    SectionConflictTable ust = new SectionConflictTable((StudentSectioningModel) solution.getModel(), SectionConflictTable.Type.UNAVAILABILITIES);
                    ust.createTable(solution.getAssignment(), lastlike).save(new File(outDir, "availability-conflicts-lastlike.csv"));
                    ust.createTable(solution.getAssignment(), real).save(new File(outDir, "availability-conflicts-real.csv"));
                    
                    SectionConflictTable ct = new SectionConflictTable((StudentSectioningModel) solution.getModel(), SectionConflictTable.Type.OVERLAPS_AND_UNAVAILABILITIES);
                    ct.createTable(solution.getAssignment(), lastlike).save(new File(outDir, "section-conflicts-lastlike.csv"));
                    ct.createTable(solution.getAssignment(), real).save(new File(outDir, "section-conflicts-real.csv"));
                    
                    UnbalancedSectionsTable ubt = new UnbalancedSectionsTable((StudentSectioningModel) solution.getModel());
                    ubt.createTable(solution.getAssignment(), lastlike).save(new File(outDir, "unbalanced-lastlike.csv"));
                    ubt.createTable(solution.getAssignment(), real).save(new File(outDir, "unbalanced-real.csv"));
                    
                    TimeOverlapConflictTable toc = new TimeOverlapConflictTable((StudentSectioningModel) solution.getModel());
                    toc.createTable(solution.getAssignment(), lastlike).save(new File(outDir, "time-overlaps-lastlike.csv"));
                    toc.createTable(solution.getAssignment(), real).save(new File(outDir, "time-overlaps-real.csv"));
                    
                    RequestGroupTable rqt = new RequestGroupTable((StudentSectioningModel) solution.getModel());
                    rqt.create(solution.getAssignment(), model.getProperties()).save(new File(outDir, "request-groups.csv"));
                    
                    RequestPriorityTable rpt = new RequestPriorityTable((StudentSectioningModel) solution.getModel());
                    rpt.create(solution.getAssignment(), model.getProperties()).save(new File(outDir, "request-priorities.csv"));
                    
                    TableauReport tr = new TableauReport((StudentSectioningModel) solution.getModel());
                    tr.create(solution.getAssignment(), model.getProperties()).save(new File(outDir, "tableau.csv"));
                    
                    SolutionStatsReport st = new SolutionStatsReport((StudentSectioningModel) solution.getModel());
                    st.create(solution.getAssignment(), model.getProperties()).save(new File(outDir, "stats.csv"));
                } catch (IOException e) {
                    sLog.error(e.getMessage(), e);
                }
            }

            solution.saveBest();
        }

        if (computeSectInfos)
            model.computeOnlineSectioningInfos(solution.getAssignment());

        if (runChecks) {
            try {
                if (model.getProperties().getPropertyBoolean("Test.InevitableStudentConflictsCheck", false)) {
                    InevitableStudentConflicts ch = new InevitableStudentConflicts(model);
                    if (!ch.check(solution.getAssignment()))
                        ch.getCSVFile().save(
                                new File(new File(model.getProperties().getProperty("General.Output", ".")),
                                        "inevitable-conflicts.csv"));
                }
            } catch (IOException e) {
                sLog.error(e.getMessage(), e);
            }
            new OverlapCheck(model).check(solution.getAssignment());
            new SectionLimitCheck(model).check(solution.getAssignment());
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

    /** Solve the student sectioning problem using IFS solver 
     * @param solution current solution
     * @param cfg solver configuration
     * @return resultant solution
     **/
    public static Solution<Request, Enrollment> solve(Solution<Request, Enrollment> solution, DataProperties cfg) {
        int nrSolvers = cfg.getPropertyInt("Parallel.NrSolvers", 1);
        Solver<Request, Enrollment> solver = (nrSolvers == 1 ? new Solver<Request, Enrollment>(cfg) : new ParallelSolver<Request, Enrollment>(cfg));
        solver.setInitalSolution(solution);
        if (cfg.getPropertyBoolean("Test.Verbose", false)) {
            solver.addSolverListener(new SolverListener<Request, Enrollment>() {
                @Override
                public boolean variableSelected(Assignment<Request, Enrollment> assignment, long iteration, Request variable) {
                    return true;
                }

                @Override
                public boolean valueSelected(Assignment<Request, Enrollment> assignment, long iteration, Request variable, Enrollment value) {
                    return true;
                }

                @Override
                public boolean neighbourSelected(Assignment<Request, Enrollment> assignment, long iteration, Neighbour<Request, Enrollment> neighbour) {
                    sLog.debug("Select[" + iteration + "]: " + neighbour);
                    return true;
                }

                @Override
                public void neighbourFailed(Assignment<Request, Enrollment> assignment, long iteration, Neighbour<Request, Enrollment> neighbour) {
                    sLog.debug("Failed[" + iteration + "]: " + neighbour);
                }
            });
        }
        solution.addSolutionListener(new TestSolutionListener());
        
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(solver));

        solver.start();
        try {
            solver.getSolverThread().join();
        } catch (InterruptedException e) {
        }

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
     * @param model problem model
     * @param xml an XML file
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
                            CourseRequest request = new CourseRequest(reqId++, priority++, false, student, courses, false, null);
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
                    Student student = students.get(Long.valueOf(studentId));
                    if (student == null) {
                        student = new Student(studentId);
                        if (lastLike)
                            student.setDummy(true);
                        students.put(Long.valueOf(studentId), student);
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
                                    courseRequest = new CourseRequest(reqId++, student.getRequests().size(), false, student, courses, false, null);
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
                        if (studentIds.add(Long.valueOf(newId)))
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
                @Override
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
                    try {
                        String line;
                        while ((line = in.readLine()) != null) {
                            if (line.length() < 55)
                                continue;
                            char code = line.charAt(12);
                            if (code == 'H' || code == 'T')
                                continue; // skip header and tail
                            if (code == 'D' || code == 'K')
                                continue; // skip delete nad cancel
                            long studentId = Long.parseLong(line.substring(2, 11));
                            Student student = students.get(Long.valueOf(studentId));
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
                            student.getAreaClassificationMajors().clear();
                            student.getAreaClassificationMinors().clear();
                            if (major.length() > 0)
                                student.getAreaClassificationMajors().add(new AreaClassificationMajor(area, clasf, major));
                            if (minor.length() > 0)
                                student.getAreaClassificationMajors().add(new AreaClassificationMajor(area, clasf, minor));
                        }
                    } finally {
                        in.close();
                    }
                }
            }
            int without = 0;
            for (Student student: students.values()) {
                if (student.getAreaClassificationMajors().isEmpty())
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
                @Override
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

    /** Save solution info as XML 
     * @param solution current solution
     * @param extra solution extra info
     * @param file file to write
     **/
    public static void saveInfoToXML(Solution<Request, Enrollment> solution, Map<String, String> extra, File file) {
        FileOutputStream fos = null;
        try {
            Document document = DocumentHelper.createDocument();
            document.addComment("Solution Info");

            Element root = document.addElement("info");
            TreeSet<Map.Entry<String, String>> entrySet = new TreeSet<Map.Entry<String, String>>(
                    new Comparator<Map.Entry<String, String>>() {
                        @Override
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
                if (!lastLikeIds.add(Long.valueOf(student.getId()))) {
                    sLog.error("Two last-like student with id " + student.getId());
                }
            } else {
                if (!realIds.add(Long.valueOf(student.getId()))) {
                    sLog.error("Two real student with id " + student.getId());
                }
            }
            for (Request request : student.getRequests()) {
                if (request instanceof CourseRequest) {
                    CourseRequest courseRequest = (CourseRequest) request;
                    Course course = courseRequest.getCourses().get(0);
                    Integer cnt = (student.isDummy() ? lastLike : real).get(course);
                    (student.isDummy() ? lastLike : real).put(course, Integer.valueOf(
                            (cnt == null ? 0 : cnt.intValue()) + 1));
                }
            }
        }
        for (Student student : new ArrayList<Student>(model.getStudents())) {
            if (student.isDummy() && realIds.contains(Long.valueOf(student.getId()))) {
                sLog.warn("There is both last-like and real student with id " + student.getId());
                long newId = -1;
                while (true) {
                    newId = 1 + (long) (999999999L * Math.random());
                    if (!realIds.contains(Long.valueOf(newId)) && !lastLikeIds.contains(Long.valueOf(newId)))
                        break;
                }
                lastLikeIds.remove(Long.valueOf(student.getId()));
                lastLikeIds.add(Long.valueOf(newId));
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

    /** Combine students from the provided two files 
     * @param cfg solver configuration
     * @param lastLikeStudentData a file containing last-like student data
     * @param realStudentData a file containing real student data
     * @return combined solution
     **/
    public static Solution<Request, Enrollment> combineStudents(DataProperties cfg, File lastLikeStudentData, File realStudentData) {
        try {
            RandomStudentFilter rnd = new RandomStudentFilter(1.0);

            StudentSectioningModel model = null;
            Assignment<Request, Enrollment> assignment = new DefaultSingleAssignment<Request, Enrollment>();

            for (StringTokenizer stk = new StringTokenizer(cfg.getProperty("Test.CombineAcceptProb", "1.0"), ","); stk.hasMoreTokens();) {
                double acceptProb = Double.parseDouble(stk.nextToken());
                sLog.info("Test.CombineAcceptProb=" + acceptProb);
                rnd.setProbability(acceptProb);

                StudentFilter batchFilter = new CombinedStudentFilter(new ReverseStudentFilter(
                        new FreshmanStudentFilter()), rnd, CombinedStudentFilter.OP_AND);

                model = new StudentSectioningModel(cfg);
                StudentSectioningXMLLoader loader = new StudentSectioningXMLLoader(model, assignment);
                loader.setLoadStudents(false);
                loader.load();

                StudentSectioningXMLLoader lastLikeLoader = new StudentSectioningXMLLoader(model, assignment);
                lastLikeLoader.setInputFile(lastLikeStudentData);
                lastLikeLoader.setLoadOfferings(false);
                lastLikeLoader.setLoadStudents(true);
                lastLikeLoader.load();

                StudentSectioningXMLLoader realLoader = new StudentSectioningXMLLoader(model, assignment);
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

            return model == null ? null : new Solution<Request, Enrollment>(model, assignment);

        } catch (Exception e) {
            sLog.error("Unable to combine students, reason: " + e.getMessage(), e);
            return null;
        }
    }
    
    /** Main 
     * @param args program arguments
     **/
    public static void main(String[] args) {
        try {
            DataProperties cfg = new DataProperties();
            cfg.setProperty("Termination.Class", "org.cpsolver.ifs.termination.GeneralTerminationCondition");
            cfg.setProperty("Termination.StopWhenComplete", "true");
            cfg.setProperty("Termination.TimeOut", "600");
            cfg.setProperty("Comparator.Class", "org.cpsolver.ifs.solution.GeneralSolutionComparator");
            cfg.setProperty("Value.Class", "org.cpsolver.studentsct.heuristics.EnrollmentSelection");// org.cpsolver.ifs.heuristics.GeneralValueSelection
            cfg.setProperty("Value.WeightConflicts", "1.0");
            cfg.setProperty("Value.WeightNrAssignments", "0.0");
            cfg.setProperty("Variable.Class", "org.cpsolver.ifs.heuristics.GeneralVariableSelection");
            cfg.setProperty("Neighbour.Class", "org.cpsolver.studentsct.heuristics.StudentSctNeighbourSelection");
            cfg.setProperty("General.SaveBestUnassigned", "0");
            cfg.setProperty("Extensions.Classes",
                    "org.cpsolver.ifs.extension.ConflictStatistics;org.cpsolver.studentsct.extension.DistanceConflict" +
                    ";org.cpsolver.studentsct.extension.TimeOverlapsCounter");
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

            File outDir = null;
            if (args.length >= 3) {
                outDir = new File(args[2], sDateFormat.format(new Date()));
            } else if (cfg.getProperty("General.Output") != null) {
                outDir = new File(cfg.getProperty("General.Output", "."), sDateFormat.format(new Date()));
            } else {
                outDir = new File(System.getProperty("user.home", ".") + File.separator + "Sectioning-Test" + File.separator + (sDateFormat.format(new Date())));
            }
            outDir.mkdirs();
            ToolBox.setupLogging(new File(outDir, "debug.log"), "true".equals(System.getProperty("debug", "false")));
            cfg.setProperty("General.Output", outDir.getAbsolutePath());

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
                iIds.add(Long.valueOf(student.getId()));
            }
        }

        @Override
        public boolean accept(Student student) {
            return !iIds.contains(Long.valueOf(student.getId()));
        }

        @Override
        public String getName() {
            return "Extra";
        }
    }

    public static class TestSolutionListener implements SolutionListener<Request, Enrollment> {
        @Override
        public void solutionUpdated(Solution<Request, Enrollment> solution) {
            StudentSectioningModel m = (StudentSectioningModel) solution.getModel();
            if (m.getTimeOverlaps() != null && TimeOverlapsCounter.sDebug)
                m.getTimeOverlaps().checkTotalNrConflicts(solution.getAssignment());
            if (m.getDistanceConflict() != null && DistanceConflict.sDebug)
                m.getDistanceConflict().checkAllConflicts(solution.getAssignment());
            if (m.getStudentQuality() != null && m.getStudentQuality().isDebug())
                m.getStudentQuality().checkTotalPenalty(solution.getAssignment());
        }

        @Override
        public void getInfo(Solution<Request, Enrollment> solution, Map<String, String> info) {
        }

        @Override
        public void getInfo(Solution<Request, Enrollment> solution, Map<String, String> info, Collection<Request> variables) {
        }

        @Override
        public void bestCleared(Solution<Request, Enrollment> solution) {
        }

        @Override
        public void bestSaved(Solution<Request, Enrollment> solution) {
            sLog.info("**BEST** " + ((StudentSectioningModel)solution.getModel()).toString(solution.getAssignment()) + ", TM:" + sDF.format(solution.getTime() / 3600.0) + "h" +
                    (solution.getFailedIterations() > 0 ? ", F:" + sDF.format(100.0 * solution.getFailedIterations() / solution.getIteration()) + "%" : ""));
        }

        @Override
        public void bestRestored(Solution<Request, Enrollment> solution) {
        }
    }
    
    private static class ShutdownHook extends Thread {
        Solver<Request, Enrollment> iSolver = null;
        Map<String, String> iExtra = null;

        private ShutdownHook(Solver<Request, Enrollment> solver) {
            setName("ShutdownHook");
            iSolver = solver;
        }
        
        void setExtra(Map<String, String> extra) { iExtra = extra; }
        
        @Override
        public void run() {
            try {
                if (iSolver.isRunning()) iSolver.stopSolver();
                Solution<Request, Enrollment> solution = iSolver.lastSolution();
                solution.restoreBest();
                DataProperties cfg = iSolver.getProperties();
                
                printInfo(solution,
                        cfg.getPropertyBoolean("Test.CreateReports", true),
                        cfg.getPropertyBoolean("Test.ComputeSectioningInfo", true),
                        cfg.getPropertyBoolean("Test.RunChecks", true));

                try {
                    new StudentSectioningXMLSaver(iSolver).save(new File(new File(cfg.getProperty("General.Output", ".")), "solution.xml"));
                } catch (Exception e) {
                    sLog.error("Unable to save solution, reason: " + e.getMessage(), e);
                }
                
                saveInfoToXML(solution, iExtra, new File(new File(cfg.getProperty("General.Output", ".")), "info.xml"));
                
                Progress.removeInstance(solution.getModel());
            } catch (Throwable t) {
                sLog.error("Test failed.", t);
            }
        }
    }

}
