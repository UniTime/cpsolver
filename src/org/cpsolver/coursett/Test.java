package org.cpsolver.coursett;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import org.cpsolver.coursett.constraint.DepartmentSpreadConstraint;
import org.cpsolver.coursett.constraint.GroupConstraint;
import org.cpsolver.coursett.constraint.InstructorConstraint;
import org.cpsolver.coursett.constraint.JenrlConstraint;
import org.cpsolver.coursett.constraint.RoomConstraint;
import org.cpsolver.coursett.constraint.SpreadConstraint;
import org.cpsolver.coursett.criteria.BackToBackInstructorPreferences;
import org.cpsolver.coursett.criteria.BrokenTimePatterns;
import org.cpsolver.coursett.criteria.DepartmentBalancingPenalty;
import org.cpsolver.coursett.criteria.DistributionPreferences;
import org.cpsolver.coursett.criteria.Perturbations;
import org.cpsolver.coursett.criteria.RoomPreferences;
import org.cpsolver.coursett.criteria.SameSubpartBalancingPenalty;
import org.cpsolver.coursett.criteria.StudentCommittedConflict;
import org.cpsolver.coursett.criteria.StudentConflict;
import org.cpsolver.coursett.criteria.StudentDistanceConflict;
import org.cpsolver.coursett.criteria.StudentHardConflict;
import org.cpsolver.coursett.criteria.TimePreferences;
import org.cpsolver.coursett.criteria.TooBigRooms;
import org.cpsolver.coursett.criteria.UselessHalfHours;
import org.cpsolver.coursett.heuristics.UniversalPerturbationsCounter;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.model.Student;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.DefaultParallelAssignment;
import org.cpsolver.ifs.assignment.DefaultSingleAssignment;
import org.cpsolver.ifs.extension.ConflictStatistics;
import org.cpsolver.ifs.extension.Extension;
import org.cpsolver.ifs.extension.MacPropagation;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solution.SolutionListener;
import org.cpsolver.ifs.solver.ParallelSolver;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.ifs.util.ProgressWriter;
import org.cpsolver.ifs.util.ToolBox;


/**
 * A main class for running of the solver from command line. <br>
 * <br>
 * Usage:<br>
 * java -Xmx1024m -jar coursett1.1.jar config.properties [input_file]
 * [output_folder]<br>
 * <br>
 * See http://www.unitime.org for example configuration files and banchmark data
 * sets.<br>
 * <br>
 * 
 * The test does the following steps:
 * <ul>
 * <li>Provided property file is loaded (see {@link DataProperties}).
 * <li>Output folder is created (General.Output property) and loggings is setup
 * (using log4j).
 * <li>Input data are loaded (calling {@link TimetableLoader#load()}).
 * <li>Solver is executed (see {@link Solver}).
 * <li>Resultant solution is saved (calling {@link TimetableSaver#save()}, when
 * General.Save property is set to true.
 * </ul>
 * Also, a log and a CSV (comma separated text file) is created in the output
 * folder.
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
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

public class Test implements SolutionListener<Lecture, Placement> {
    private static java.text.SimpleDateFormat sDateFormat = new java.text.SimpleDateFormat("yyMMdd_HHmmss",
            java.util.Locale.US);
    private static java.text.DecimalFormat sDoubleFormat = new java.text.DecimalFormat("0.000",
            new java.text.DecimalFormatSymbols(Locale.US));
    private static org.apache.logging.log4j.Logger sLogger = org.apache.logging.log4j.LogManager.getLogger(Test.class);

    private PrintWriter iCSVFile = null;

    private MacPropagation<Lecture, Placement> iProp = null;
    private ConflictStatistics<Lecture, Placement> iStat = null;
    private int iLastNotified = -1;

    private boolean initialized = false;
    private Solver<Lecture, Placement> iSolver = null;

    /** Current version 
     * @return version string
     **/
    public static String getVersionString() {
        return "IFS Timetable Solver v" + Constants.getVersion() + " build" + Constants.getBuildNumber() + ", "
                + Constants.getReleaseDate();
    }

    /** Solver initialization 
     * @param solver current solver
     **/
    public void init(Solver<Lecture, Placement> solver) {
        iSolver = solver;
        solver.currentSolution().addSolutionListener(this);
    }

    /**
     * Return name of the class that is used for loading the data. This class
     * needs to extend class {@link TimetableLoader}. It can be also defined in
     * configuration, using TimetableLoader property.
     **/
    private String getTimetableLoaderClass(DataProperties properties) {
        String loader = properties.getProperty("TimetableLoader");
        if (loader != null)
            return loader;
        if (properties.getPropertyInt("General.InputVersion", -1) >= 0)
            return "org.unitime.timetable.solver.TimetableDatabaseLoader";
        else
            return "org.cpsolver.coursett.TimetableXMLLoader";
    }

    /**
     * Return name of the class that is used for loading the data. This class
     * needs to extend class {@link TimetableSaver}. It can be also defined in
     * configuration, using TimetableSaver property.
     **/
    private String getTimetableSaverClass(DataProperties properties) {
        String saver = properties.getProperty("TimetableSaver");
        if (saver != null)
            return saver;
        if (properties.getPropertyInt("General.InputVersion", -1) >= 0)
            return "org.unitime.timetable.solver.TimetableDatabaseSaver";
        else
            return "org.cpsolver.coursett.TimetableXMLSaver";
    }

    /**
     * Solver Test
     * 
     * @param args
     *            command line arguments
     */
    public Test(String[] args) {
        try {
            DataProperties properties = ToolBox.loadProperties(new java.io.File(args[0]));
            properties.putAll(System.getProperties());
            properties.setProperty("General.Output", properties.getProperty("General.Output", ".") + File.separator + sDateFormat.format(new Date()));
            if (args.length > 1)
                properties.setProperty("General.Input", args[1]);
            if (args.length > 2)
                properties.setProperty("General.Output", args[2] + File.separator + (sDateFormat.format(new Date())));
            System.out.println("Output folder: " + properties.getProperty("General.Output"));
            File outDir = new File(properties.getProperty("General.Output", "."));
            outDir.mkdirs();
            ToolBox.setupLogging(new File(outDir, "debug.log"), "true".equals(System.getProperty("debug", "false")));

            TimetableModel model = new TimetableModel(properties);
            int nrSolvers = properties.getPropertyInt("Parallel.NrSolvers", 1);
            Assignment<Lecture, Placement> assignment = (nrSolvers <= 1 ? new DefaultSingleAssignment<Lecture, Placement>() : new DefaultParallelAssignment<Lecture, Placement>());
            Progress.getInstance(model).addProgressListener(new ProgressWriter(System.out));
            Solver<Lecture, Placement> solver = (nrSolvers == 1 ? new Solver<Lecture, Placement>(properties) : new ParallelSolver<Lecture, Placement>(properties));

            TimetableLoader loader = null;
            try {
                loader = (TimetableLoader) Class.forName(getTimetableLoaderClass(properties))
                        .getConstructor(new Class[] { TimetableModel.class, Assignment.class }).newInstance(new Object[] { model, assignment });
            } catch (ClassNotFoundException e) {
                System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
                loader = new TimetableXMLLoader(model, assignment);
            }
            loader.load();

            solver.setInitalSolution(new Solution<Lecture, Placement>(model, assignment));
            init(solver);

            iCSVFile = new PrintWriter(new FileWriter(outDir.toString() + File.separator + "stat.csv"));
            String colSeparator = ";";
            iCSVFile.println("Assigned"
                    + colSeparator
                    + "Assigned[%]"
                    + colSeparator
                    + "Time[min]"
                    + colSeparator
                    + "Iter"
                    + colSeparator
                    + "IterYield[%]"
                    + colSeparator
                    + "Speed[it/s]"
                    + colSeparator
                    + "AddedPert"
                    + colSeparator
                    + "AddedPert[%]"
                    + colSeparator
                    + "HardStudentConf"
                    + colSeparator
                    + "StudentConf"
                    + colSeparator
                    + "DistStudentConf"
                    + colSeparator
                    + "CommitStudentConf"
                    + colSeparator
                    + "TimePref"
                    + colSeparator
                    + "RoomPref"
                    + colSeparator
                    + "DistInstrPref"
                    + colSeparator
                    + "GrConstPref"
                    + colSeparator
                    + "UselessHalfHours"
                    + colSeparator
                    + "BrokenTimePat"
                    + colSeparator
                    + "TooBigRooms"
                    + (iProp != null ? colSeparator + "GoodVars" + colSeparator + "GoodVars[%]" + colSeparator
                            + "GoodVals" + colSeparator + "GoodVals[%]" : ""));
            iCSVFile.flush();
            
            Runtime.getRuntime().addShutdownHook(new ShutdownHook(solver));

            solver.start();
            try {
                solver.getSolverThread().join();
            } catch (InterruptedException e) {
            }
        } catch (Throwable t) {
            sLogger.error("Test failed.", t);
        }
    }

    public static void main(String[] args) {
        new Test(args);
    }

    @Override
    public void bestCleared(Solution<Lecture, Placement> solution) {
    }

    @Override
    public void bestRestored(Solution<Lecture, Placement> solution) {
    }

    @Override
    public void bestSaved(Solution<Lecture, Placement> solution) {
        notify(solution);
        if (sLogger.isInfoEnabled())
            sLogger.info("**BEST[" + solution.getIteration() + "]** " + ((TimetableModel)solution.getModel()).toString(solution.getAssignment()) +
                    (solution.getFailedIterations() > 0 ? ", F:" + sDoubleFormat.format(100.0 * solution.getFailedIterations() / solution.getIteration()) + "%" : ""));
    }

    @Override
    public void getInfo(Solution<Lecture, Placement> solution, Map<String, String> info) {
    }

    @Override
    public void getInfo(Solution<Lecture, Placement> solution, Map<String, String> info, Collection<Lecture> variables) {
    }

    @Override
    public void solutionUpdated(Solution<Lecture, Placement> solution) {
        if (!initialized) {
            for (Extension<Lecture, Placement> extension : iSolver.getExtensions()) {
                if (MacPropagation.class.isInstance(extension))
                    iProp = (MacPropagation<Lecture, Placement>) extension;
                if (ConflictStatistics.class.isInstance(extension)) {
                    iStat = (ConflictStatistics<Lecture, Placement>) extension;
                }
            }
        }
    }

    /** Add a line into the output CSV file when a enw best solution is found. 
     * @param solution current solution
     **/
    public void notify(Solution<Lecture, Placement> solution) {
        String colSeparator = ";";
        Assignment<Lecture, Placement> assignment = solution.getAssignment();
        if (assignment.nrAssignedVariables() < solution.getModel().countVariables() && iLastNotified == assignment.nrAssignedVariables())
            return;
        iLastNotified = assignment.nrAssignedVariables();
        if (iCSVFile != null) {
            TimetableModel model = (TimetableModel) solution.getModel();
            iCSVFile.print(model.variables().size() - model.nrUnassignedVariables(assignment));
            iCSVFile.print(colSeparator);
            iCSVFile.print(sDoubleFormat.format(100.0 * assignment.nrAssignedVariables() / model.variables().size()));
            iCSVFile.print(colSeparator);
            iCSVFile.print(sDoubleFormat.format((solution.getTime()) / 60.0));
            iCSVFile.print(colSeparator);
            iCSVFile.print(solution.getIteration());
            iCSVFile.print(colSeparator);
            iCSVFile.print(sDoubleFormat.format(100.0 * assignment.nrAssignedVariables() / solution.getIteration()));
            iCSVFile.print(colSeparator);
            iCSVFile.print(sDoubleFormat.format((solution.getIteration()) / solution.getTime()));
            iCSVFile.print(colSeparator);
            iCSVFile.print(model.perturbVariables(assignment).size());
            iCSVFile.print(colSeparator);
            iCSVFile.print(sDoubleFormat.format(100.0 * model.perturbVariables(assignment).size() / model.variables().size()));
            iCSVFile.print(colSeparator);
            iCSVFile.print(Math.round(solution.getModel().getCriterion(StudentHardConflict.class).getValue(assignment)));
            iCSVFile.print(colSeparator);
            iCSVFile.print(Math.round(solution.getModel().getCriterion(StudentConflict.class).getValue(assignment)));
            iCSVFile.print(colSeparator);
            iCSVFile.print(Math.round(solution.getModel().getCriterion(StudentDistanceConflict.class).getValue(assignment)));
            iCSVFile.print(colSeparator);
            iCSVFile.print(Math.round(solution.getModel().getCriterion(StudentCommittedConflict.class).getValue(assignment)));
            iCSVFile.print(colSeparator);
            iCSVFile.print(sDoubleFormat.format(solution.getModel().getCriterion(TimePreferences.class).getValue(assignment)));
            iCSVFile.print(colSeparator);
            iCSVFile.print(Math.round(solution.getModel().getCriterion(RoomPreferences.class).getValue(assignment)));
            iCSVFile.print(colSeparator);
            iCSVFile.print(Math.round(solution.getModel().getCriterion(BackToBackInstructorPreferences.class).getValue(assignment)));
            iCSVFile.print(colSeparator);
            iCSVFile.print(Math.round(solution.getModel().getCriterion(DistributionPreferences.class).getValue(assignment)));
            iCSVFile.print(colSeparator);
            iCSVFile.print(Math.round(solution.getModel().getCriterion(UselessHalfHours.class).getValue(assignment)));
            iCSVFile.print(colSeparator);
            iCSVFile.print(Math.round(solution.getModel().getCriterion(BrokenTimePatterns.class).getValue(assignment)));
            iCSVFile.print(colSeparator);
            iCSVFile.print(Math.round(solution.getModel().getCriterion(TooBigRooms.class).getValue(assignment)));
            if (iProp != null) {
                if (solution.getModel().nrUnassignedVariables(assignment) > 0) {
                    int goodVariables = 0;
                    long goodValues = 0;
                    long allValues = 0;
                    for (Lecture variable : ((TimetableModel) solution.getModel()).unassignedVariables(assignment)) {
                        goodValues += iProp.goodValues(assignment, variable).size();
                        allValues += variable.values(solution.getAssignment()).size();
                        if (!iProp.goodValues(assignment, variable).isEmpty())
                            goodVariables++;
                    }
                    iCSVFile.print(colSeparator);
                    iCSVFile.print(goodVariables);
                    iCSVFile.print(colSeparator);
                    iCSVFile.print(sDoubleFormat.format(100.0 * goodVariables / solution.getModel().nrUnassignedVariables(assignment)));
                    iCSVFile.print(colSeparator);
                    iCSVFile.print(goodValues);
                    iCSVFile.print(colSeparator);
                    iCSVFile.print(sDoubleFormat.format(100.0 * goodValues / allValues));
                } else {
                    iCSVFile.print(colSeparator);
                    iCSVFile.print(colSeparator);
                    iCSVFile.print(colSeparator);
                    iCSVFile.print(colSeparator);
                }
            }
            iCSVFile.println();
            iCSVFile.flush();
        }
    }

    /** Print room utilization 
     * @param pw writer
     * @param model problem model
     * @param assignment current assignment
     **/
    public static void printRoomInfo(PrintWriter pw, TimetableModel model, Assignment<Lecture, Placement> assignment) {
        pw.println("Room info:");
        pw.println("id, name, size, used_day, used_total");
        int firstDaySlot = model.getProperties().getPropertyInt("General.FirstDaySlot", Constants.DAY_SLOTS_FIRST);
        int lastDaySlot = model.getProperties().getPropertyInt("General.LastDaySlot", Constants.DAY_SLOTS_LAST);
        int firstWorkDay = model.getProperties().getPropertyInt("General.FirstWorkDay", 0);
        int lastWorkDay = model.getProperties().getPropertyInt("General.LastWorkDay", Constants.NR_DAYS_WEEK - 1);
        if (lastWorkDay < firstWorkDay) lastWorkDay += 7;
        for (RoomConstraint rc : model.getRoomConstraints()) {
            int used_day = 0;
            int used_total = 0;
            for (int day = firstWorkDay; day <= lastWorkDay; day++) {
                for (int time = firstDaySlot; time <= lastDaySlot; time++) {
                    if (!rc.getContext(assignment).getPlacements((day % 7) * Constants.SLOTS_PER_DAY + time).isEmpty())
                        used_day++;
                }
            }
            for (int day = 0; day < Constants.DAY_CODES.length; day++) {
                for (int time = 0; time < Constants.SLOTS_PER_DAY; time++) {
                    if (!rc.getContext(assignment).getPlacements((day % 7) * Constants.SLOTS_PER_DAY + time).isEmpty())
                        used_total++;
                }
            }
            pw.println(rc.getResourceId() + "," + rc.getName() + "," + rc.getCapacity() + "," + used_day + "," + used_total);
        }
    }

    /** Class information 
     * @param pw writer
     * @param model problem model
     **/
    public static void printClassInfo(PrintWriter pw, TimetableModel model) {
        pw.println("Class info:");
        pw.println("id, name, min_class_limit, max_class_limit, room2limit_ratio, half_hours");
        for (Lecture lecture : model.variables()) {
            if (lecture.timeLocations().isEmpty()) {
                pw.println(lecture.getClassId() + "," + lecture.getName() + "," + lecture.minClassLimit() + ","
                        + lecture.maxClassLimit() + "," + lecture.roomToLimitRatio() + ","
                        + "NO TIMES");
                sLogger.error(lecture.getName() + " has no times.");
                continue;
            }
            TimeLocation time = lecture.timeLocations().get(0);
            pw.println(lecture.getClassId() + "," + lecture.getName() + "," + lecture.minClassLimit() + ","
                    + lecture.maxClassLimit() + "," + lecture.roomToLimitRatio() + ","
                    + (time.getNrSlotsPerMeeting() * time.getNrMeetings()));
        }
    }

    /** Create info.txt with some more information about the problem 
     * @param solution current solution
     * @throws IOException an exception that may be thrown
     **/
    public static void printSomeStuff(Solution<Lecture, Placement> solution) throws IOException {
        TimetableModel model = (TimetableModel) solution.getModel();
        Assignment<Lecture, Placement> assignment = solution.getAssignment();
        File outDir = new File(model.getProperties().getProperty("General.Output", "."));
        PrintWriter pw = new PrintWriter(new FileWriter(outDir.toString() + File.separator + "info.txt"));
        PrintWriter pwi = new PrintWriter(new FileWriter(outDir.toString() + File.separator + "info.csv"));
        String name = new File(model.getProperties().getProperty("General.Input")).getName();
        pwi.println("Instance," + name.substring(0, name.lastIndexOf('.')));
        pw.println("Solution info: " + ToolBox.dict2string(solution.getInfo(), 1));
        pw.println("Bounds: " + ToolBox.dict2string(model.getBounds(assignment), 1));
        Map<String, String> info = solution.getInfo();
        for (String key : new TreeSet<String>(info.keySet())) {
            if (key.equals("Memory usage"))
                continue;
            if (key.equals("Iteration"))
                continue;
            if (key.equals("Time"))
                continue;
            String value = info.get(key);
            if (value.indexOf(' ') > 0)
                value = value.substring(0, value.indexOf(' '));
            pwi.println(key + "," + value);
        }
        printRoomInfo(pw, model, assignment);
        printClassInfo(pw, model);
        long nrValues = 0;
        long nrTimes = 0;
        long nrRooms = 0;
        double totalMaxNormTimePref = 0.0;
        double totalMinNormTimePref = 0.0;
        double totalNormTimePref = 0.0;
        int totalMaxRoomPref = 0;
        int totalMinRoomPref = 0;
        int totalRoomPref = 0;
        long nrStudentEnrls = 0;
        long nrInevitableStudentConflicts = 0;
        long nrJenrls = 0;
        int nrHalfHours = 0;
        int nrMeetings = 0;
        int totalMinLimit = 0;
        int totalMaxLimit = 0;
        long nrReqRooms = 0;
        int nrSingleValueVariables = 0;
        int nrSingleTimeVariables = 0;
        int nrSingleRoomVariables = 0;
        long totalAvailableMinRoomSize = 0;
        long totalAvailableMaxRoomSize = 0;
        long totalRoomSize = 0;
        long nrOneOrMoreRoomVariables = 0;
        long nrOneRoomVariables = 0;
        HashSet<Student> students = new HashSet<Student>();
        HashSet<Long> offerings = new HashSet<Long>();
        HashSet<Long> configs = new HashSet<Long>();
        HashSet<Long> subparts = new HashSet<Long>();
        int[] sizeLimits = new int[] { 0, 25, 50, 75, 100, 150, 200, 400 };
        int[] nrRoomsOfSize = new int[sizeLimits.length];
        int[] minRoomOfSize = new int[sizeLimits.length];
        int[] maxRoomOfSize = new int[sizeLimits.length];
        int[] totalUsedSlots = new int[sizeLimits.length];
        int[] totalUsedSeats = new int[sizeLimits.length];
        int[] totalUsedSeats2 = new int[sizeLimits.length];
        int firstDaySlot = model.getProperties().getPropertyInt("General.FirstDaySlot", Constants.DAY_SLOTS_FIRST);
        int lastDaySlot = model.getProperties().getPropertyInt("General.LastDaySlot", Constants.DAY_SLOTS_LAST);
        int firstWorkDay = model.getProperties().getPropertyInt("General.FirstWorkDay", 0);
        int lastWorkDay = model.getProperties().getPropertyInt("General.LastWorkDay", Constants.NR_DAYS_WEEK - 1);
        if (lastWorkDay < firstWorkDay) lastWorkDay += 7;
        for (Lecture lect : model.variables()) {
            if (lect.getConfiguration() != null) {
                offerings.add(lect.getConfiguration().getOfferingId());
                configs.add(lect.getConfiguration().getConfigId());
            }
            subparts.add(lect.getSchedulingSubpartId());
            nrStudentEnrls += (lect.students() == null ? 0 : lect.students().size());
            students.addAll(lect.students());
            nrValues += lect.values(solution.getAssignment()).size();
            nrReqRooms += lect.getNrRooms();
            for (RoomLocation room: lect.roomLocations())
                if (room.getMinPreference() < Constants.sPreferenceLevelProhibited / 2)
                    nrRooms++;
            for (TimeLocation time: lect.timeLocations())
                if (time.getPreference() < Constants.sPreferenceLevelProhibited / 2)
                    nrTimes ++;
            totalMinLimit += lect.minClassLimit();
            totalMaxLimit += lect.maxClassLimit();
            if (!lect.values(solution.getAssignment()).isEmpty()) {
                Placement p = lect.values(solution.getAssignment()).get(0);
                nrMeetings += p.getTimeLocation().getNrMeetings();
                nrHalfHours += p.getTimeLocation().getNrMeetings() * p.getTimeLocation().getNrSlotsPerMeeting();
                totalMaxNormTimePref += lect.getMinMaxTimePreference()[1];
                totalMinNormTimePref += lect.getMinMaxTimePreference()[0];
                totalNormTimePref += Math.abs(lect.getMinMaxTimePreference()[1] - lect.getMinMaxTimePreference()[0]);
                totalMaxRoomPref += lect.getMinMaxRoomPreference()[1];
                totalMinRoomPref += lect.getMinMaxRoomPreference()[0];
                totalRoomPref += Math.abs(lect.getMinMaxRoomPreference()[1] - lect.getMinMaxRoomPreference()[0]);
                TimeLocation time = p.getTimeLocation();
                boolean hasRoomConstraint = false;
                for (RoomLocation roomLocation : lect.roomLocations()) {
                    if (roomLocation.getRoomConstraint().getConstraint())
                        hasRoomConstraint = true;
                }
                if (hasRoomConstraint && lect.getNrRooms() > 0) {
                    for (int d = firstWorkDay; d <= lastWorkDay; d++) {
                        if ((time.getDayCode() & Constants.DAY_CODES[d % 7]) == 0)
                            continue;
                        for (int t = Math.max(time.getStartSlot(), firstDaySlot); t <= Math.min(time.getStartSlot() + time.getLength() - 1, lastDaySlot); t++) {
                            for (int l = 0; l < sizeLimits.length; l++) {
                                if (sizeLimits[l] <= lect.minRoomSize()) {
                                    totalUsedSlots[l] += lect.getNrRooms();
                                    totalUsedSeats[l] += lect.classLimit(assignment);
                                    totalUsedSeats2[l] += lect.minRoomSize() * lect.getNrRooms();
                                }
                            }
                        }
                    }
                }
            }
            if (lect.values(solution.getAssignment()).size() == 1) {
                nrSingleValueVariables++;
            }
            if (lect.timeLocations().size() == 1) {
                nrSingleTimeVariables++;
            }
            if (lect.roomLocations().size() == 1) {
                nrSingleRoomVariables++;
            }
            if (lect.getNrRooms() == 1) {
                nrOneRoomVariables++;
            }
            if (lect.getNrRooms() > 0) {
                nrOneOrMoreRoomVariables++;
            }
            if (!lect.roomLocations().isEmpty()) {
                int minRoomSize = Integer.MAX_VALUE;
                int maxRoomSize = Integer.MIN_VALUE;
                for (RoomLocation rl : lect.roomLocations()) {
                    minRoomSize = Math.min(minRoomSize, rl.getRoomSize());
                    maxRoomSize = Math.max(maxRoomSize, rl.getRoomSize());
                    totalRoomSize += rl.getRoomSize();
                }
                totalAvailableMinRoomSize += minRoomSize;
                totalAvailableMaxRoomSize += maxRoomSize;
            }
        }
        for (JenrlConstraint jenrl : model.getJenrlConstraints()) {
            nrJenrls += jenrl.getJenrl();
            if ((jenrl.first()).timeLocations().size() == 1 && (jenrl.second()).timeLocations().size() == 1) {
                TimeLocation t1 = jenrl.first().timeLocations().get(0);
                TimeLocation t2 = jenrl.second().timeLocations().get(0);
                if (t1.hasIntersection(t2)) {
                    nrInevitableStudentConflicts += jenrl.getJenrl();
                    pw.println("Inevitable " + jenrl.getJenrl() + " student conflicts between " + jenrl.first() + " "
                            + t1 + " and " + jenrl.second() + " " + t2);
                } else if (jenrl.first().values(solution.getAssignment()).size() == 1 && jenrl.second().values(solution.getAssignment()).size() == 1) {
                    Placement p1 = jenrl.first().values(solution.getAssignment()).get(0);
                    Placement p2 = jenrl.second().values(solution.getAssignment()).get(0);
                    if (JenrlConstraint.isInConflict(p1, p2, ((TimetableModel)p1.variable().getModel()).getDistanceMetric(), ((TimetableModel)p1.variable().getModel()).getStudentWorkDayLimit())) {
                        nrInevitableStudentConflicts += jenrl.getJenrl();
                        pw.println("Inevitable " + jenrl.getJenrl()
                                + (p1.getTimeLocation().hasIntersection(p2.getTimeLocation()) ? "" : " distance")
                                + " student conflicts between " + p1 + " and " + p2);
                    }
                }
            }
        }
        int totalCommitedPlacements = 0;
        for (Student student : students) {
            if (student.getCommitedPlacements() != null)
                totalCommitedPlacements += student.getCommitedPlacements().size();
        }
        pw.println("Total number of classes: " + model.variables().size());
        pwi.println("Number of classes," + model.variables().size());
        pw.println("Total number of instructional offerings: " + offerings.size() + " ("
                + sDoubleFormat.format(100.0 * offerings.size() / model.variables().size()) + "%)");
        // pwi.println("Number of instructional offerings,"+offerings.size());
        pw.println("Total number of configurations: " + configs.size() + " ("
                + sDoubleFormat.format(100.0 * configs.size() / model.variables().size()) + "%)");
        pw.println("Total number of scheduling subparts: " + subparts.size() + " ("
                + sDoubleFormat.format(100.0 * subparts.size() / model.variables().size()) + "%)");
        // pwi.println("Number of scheduling subparts,"+subparts.size());
        pw.println("Average number classes per subpart: "
                + sDoubleFormat.format(1.0 * model.variables().size() / subparts.size()));
        pwi.println("Avg. classes per instruction,"
                + sDoubleFormat.format(1.0 * model.variables().size() / subparts.size()));
        pw.println("Average number classes per config: "
                + sDoubleFormat.format(1.0 * model.variables().size() / configs.size()));
        pw.println("Average number classes per offering: "
                + sDoubleFormat.format(1.0 * model.variables().size() / offerings.size()));
        pw.println("Total number of classes with only one value: " + nrSingleValueVariables + " ("
                + sDoubleFormat.format(100.0 * nrSingleValueVariables / model.variables().size()) + "%)");
        pw.println("Total number of classes with only one time: " + nrSingleTimeVariables + " ("
                + sDoubleFormat.format(100.0 * nrSingleTimeVariables / model.variables().size()) + "%)");
        pw.println("Total number of classes with only one room: " + nrSingleRoomVariables + " ("
                + sDoubleFormat.format(100.0 * nrSingleRoomVariables / model.variables().size()) + "%)");
        pwi.println("Classes with single value," + nrSingleValueVariables);
        // pwi.println("Classes with only one time/room,"+nrSingleTimeVariables+"/"+nrSingleRoomVariables);
        pw.println("Total number of classes requesting no room: "
                + (model.variables().size() - nrOneOrMoreRoomVariables)
                + " ("
                + sDoubleFormat.format(100.0 * (model.variables().size() - nrOneOrMoreRoomVariables)
                        / model.variables().size()) + "%)");
        pw.println("Total number of classes requesting one room: " + nrOneRoomVariables + " ("
                + sDoubleFormat.format(100.0 * nrOneRoomVariables / model.variables().size()) + "%)");
        pw.println("Total number of classes requesting one or more rooms: " + nrOneOrMoreRoomVariables + " ("
                + sDoubleFormat.format(100.0 * nrOneOrMoreRoomVariables / model.variables().size()) + "%)");
        // pwi.println("% classes requesting no room,"+sDoubleFormat.format(100.0*(model.variables().size()-nrOneOrMoreRoomVariables)/model.variables().size())+"%");
        // pwi.println("% classes requesting one room,"+sDoubleFormat.format(100.0*nrOneRoomVariables/model.variables().size())+"%");
        // pwi.println("% classes requesting two or more rooms,"+sDoubleFormat.format(100.0*(nrOneOrMoreRoomVariables-nrOneRoomVariables)/model.variables().size())+"%");
        pw.println("Average number of requested rooms: "
                + sDoubleFormat.format(1.0 * nrReqRooms / model.variables().size()));
        pw.println("Average minimal class limit: "
                + sDoubleFormat.format(1.0 * totalMinLimit / model.variables().size()));
        pw.println("Average maximal class limit: "
                + sDoubleFormat.format(1.0 * totalMaxLimit / model.variables().size()));
        // pwi.println("Average class limit,"+sDoubleFormat.format(1.0*(totalMinLimit+totalMaxLimit)/(2*model.variables().size())));
        pw.println("Average number of placements: " + sDoubleFormat.format(1.0 * nrValues / model.variables().size()));
        // pwi.println("Average domain size,"+sDoubleFormat.format(1.0*nrValues/model.variables().size()));
        pwi.println("Avg. domain size," + sDoubleFormat.format(1.0 * nrValues / model.variables().size()));
        pw.println("Average number of time locations: "
                + sDoubleFormat.format(1.0 * nrTimes / model.variables().size()));
        pwi.println("Avg. number of avail. times/rooms,"
                + sDoubleFormat.format(1.0 * nrTimes / model.variables().size()) + "/"
                + sDoubleFormat.format(1.0 * nrRooms / model.variables().size()));
        pw.println("Average number of room locations: "
                + sDoubleFormat.format(1.0 * nrRooms / model.variables().size()));
        pw.println("Average minimal requested room size: "
                + sDoubleFormat.format(1.0 * totalAvailableMinRoomSize / nrOneOrMoreRoomVariables));
        pw.println("Average maximal requested room size: "
                + sDoubleFormat.format(1.0 * totalAvailableMaxRoomSize / nrOneOrMoreRoomVariables));
        pw.println("Average requested room sizes: " + sDoubleFormat.format(1.0 * totalRoomSize / nrRooms));
        pwi.println("Average requested room size," + sDoubleFormat.format(1.0 * totalRoomSize / nrRooms));
        pw.println("Average maximum normalized time preference: "
                + sDoubleFormat.format(totalMaxNormTimePref / model.variables().size()));
        pw.println("Average minimum normalized time preference: "
                + sDoubleFormat.format(totalMinNormTimePref / model.variables().size()));
        pw.println("Average normalized time preference,"
                + sDoubleFormat.format(totalNormTimePref / model.variables().size()));
        pw.println("Average maximum room preferences: "
                + sDoubleFormat.format(1.0 * totalMaxRoomPref / nrOneOrMoreRoomVariables));
        pw.println("Average minimum room preferences: "
                + sDoubleFormat.format(1.0 * totalMinRoomPref / nrOneOrMoreRoomVariables));
        pw.println("Average room preferences," + sDoubleFormat.format(1.0 * totalRoomPref / nrOneOrMoreRoomVariables));
        pw.println("Total number of students:" + students.size());
        pwi.println("Number of students," + students.size());
        pwi.println("Number of inevitable student conflicts," + nrInevitableStudentConflicts);
        pw.println("Total amount of student enrollments: " + nrStudentEnrls);
        pwi.println("Number of student enrollments," + nrStudentEnrls);
        pw.println("Total amount of joined enrollments: " + nrJenrls);
        pwi.println("Number of joint student enrollments," + nrJenrls);
        pw.println("Average number of students: "
                + sDoubleFormat.format(1.0 * students.size() / model.variables().size()));
        pw.println("Average number of enrollemnts (per student): "
                + sDoubleFormat.format(1.0 * nrStudentEnrls / students.size()));
        pwi.println("Avg. number of classes per student,"
                + sDoubleFormat.format(1.0 * nrStudentEnrls / students.size()));
        pwi.println("Avg. number of committed classes per student,"
                + sDoubleFormat.format(1.0 * totalCommitedPlacements / students.size()));
        pw.println("Total amount of inevitable student conflicts: " + nrInevitableStudentConflicts + " ("
                + sDoubleFormat.format(100.0 * nrInevitableStudentConflicts / nrStudentEnrls) + "%)");
        pw.println("Average number of meetings (per class): "
                + sDoubleFormat.format(1.0 * nrMeetings / model.variables().size()));
        pw.println("Average number of hours per class: "
                + sDoubleFormat.format(1.0 * nrHalfHours / model.variables().size() / 12.0));
        pwi.println("Avg. number of meetings per class,"
                + sDoubleFormat.format(1.0 * nrMeetings / model.variables().size()));
        pwi.println("Avg. number of hours per class,"
                + sDoubleFormat.format(1.0 * nrHalfHours / model.variables().size() / 12.0));
        int minRoomSize = Integer.MAX_VALUE;
        int maxRoomSize = Integer.MIN_VALUE;
        int nrDistancePairs = 0;
        double maxRoomDistance = Double.MIN_VALUE;
        double totalRoomDistance = 0.0;
        int[] totalAvailableSlots = new int[sizeLimits.length];
        int[] totalAvailableSeats = new int[sizeLimits.length];
        int nrOfRooms = 0;
        totalRoomSize = 0;
        for (RoomConstraint rc : model.getRoomConstraints()) {
            if (rc.variables().isEmpty()) continue;
            nrOfRooms++;
            minRoomSize = Math.min(minRoomSize, rc.getCapacity());
            maxRoomSize = Math.max(maxRoomSize, rc.getCapacity());
            for (int l = 0; l < sizeLimits.length; l++) {
                if (sizeLimits[l] <= rc.getCapacity()
                        && (l + 1 == sizeLimits.length || rc.getCapacity() < sizeLimits[l + 1])) {
                    nrRoomsOfSize[l]++;
                    if (minRoomOfSize[l] == 0)
                        minRoomOfSize[l] = rc.getCapacity();
                    else
                        minRoomOfSize[l] = Math.min(minRoomOfSize[l], rc.getCapacity());
                    if (maxRoomOfSize[l] == 0)
                        maxRoomOfSize[l] = rc.getCapacity();
                    else
                        maxRoomOfSize[l] = Math.max(maxRoomOfSize[l], rc.getCapacity());
                }
            }
            totalRoomSize += rc.getCapacity();
            if (rc.getPosX() != null && rc.getPosY() != null) {
                for (RoomConstraint rc2 : model.getRoomConstraints()) {
                    if (rc2.getResourceId().compareTo(rc.getResourceId()) > 0 && rc2.getPosX() != null && rc2.getPosY() != null) {
                        double distance = ((TimetableModel)solution.getModel()).getDistanceMetric().getDistanceInMinutes(rc.getId(), rc.getPosX(), rc.getPosY(), rc2.getId(), rc2.getPosX(), rc2.getPosY());
                        totalRoomDistance += distance;
                        nrDistancePairs++;
                        maxRoomDistance = Math.max(maxRoomDistance, distance);
                    }
                }
            }
            for (int d = firstWorkDay; d <= lastWorkDay; d++) {
                for (int t = firstDaySlot; t <= lastDaySlot; t++) {
                    if (rc.isAvailable((d % 7) * Constants.SLOTS_PER_DAY + t)) {
                        for (int l = 0; l < sizeLimits.length; l++) {
                            if (sizeLimits[l] <= rc.getCapacity()) {
                                totalAvailableSlots[l]++;
                                totalAvailableSeats[l] += rc.getCapacity();
                            }
                        }
                    }
                }
            }
        }
        pw.println("Total number of rooms: " + nrOfRooms);
        pwi.println("Number of rooms," + nrOfRooms);
        pw.println("Minimal room size: " + minRoomSize);
        pw.println("Maximal room size: " + maxRoomSize);
        pwi.println("Room size min/max," + minRoomSize + "/" + maxRoomSize);
        pw.println("Average room size: "
                + sDoubleFormat.format(1.0 * totalRoomSize / model.getRoomConstraints().size()));
        pw.println("Maximal distance between two rooms: " + sDoubleFormat.format(maxRoomDistance));
        pw.println("Average distance between two rooms: "
                + sDoubleFormat.format(totalRoomDistance / nrDistancePairs));
        pwi.println("Average distance between two rooms [min],"
                + sDoubleFormat.format(totalRoomDistance / nrDistancePairs));
        pwi.println("Maximal distance between two rooms [min]," + sDoubleFormat.format(maxRoomDistance));
        for (int l = 0; l < sizeLimits.length; l++) {// sizeLimits.length;l++) {
            pwi.println("\"Room frequency (size>=" + sizeLimits[l] + ", used/avaiable times)\","
                    + sDoubleFormat.format(100.0 * totalUsedSlots[l] / totalAvailableSlots[l]) + "%");
            pwi.println("\"Room utilization (size>=" + sizeLimits[l] + ", used/available seats)\","
                    + sDoubleFormat.format(100.0 * totalUsedSeats[l] / totalAvailableSeats[l]) + "%");
            pwi.println("\"Number of rooms (size>=" + sizeLimits[l] + ")\"," + nrRoomsOfSize[l]);
            pwi.println("\"Min/max room size (size>=" + sizeLimits[l] + ")\"," + minRoomOfSize[l] + "-"
                    + maxRoomOfSize[l]);
            // pwi.println("\"Room utilization (size>="+sizeLimits[l]+", minRoomSize)\","+sDoubleFormat.format(100.0*totalUsedSeats2[l]/totalAvailableSeats[l])+"%");
        }
        pw.println("Average hours available: "
                + sDoubleFormat.format(1.0 * totalAvailableSlots[0] / nrOfRooms / 12.0));
        int totalInstructedClasses = 0;
        for (InstructorConstraint ic : model.getInstructorConstraints()) {
            totalInstructedClasses += ic.variables().size();
        }
        pw.println("Total number of instructors: " + model.getInstructorConstraints().size());
        pwi.println("Number of instructors," + model.getInstructorConstraints().size());
        pw.println("Total class-instructor assignments: " + totalInstructedClasses + " ("
                + sDoubleFormat.format(100.0 * totalInstructedClasses / model.variables().size()) + "%)");
        pwi.println("Number of class-instructor assignments," + totalInstructedClasses);
        pw.println("Average classes per instructor: "
                + sDoubleFormat.format(1.0 * totalInstructedClasses / model.getInstructorConstraints().size()));
        pwi.println("Average classes per instructor,"
                + sDoubleFormat.format(1.0 * totalInstructedClasses / model.getInstructorConstraints().size()));
        // pw.println("Average hours available: "+sDoubleFormat.format(1.0*totalAvailableSlots/model.getInstructorConstraints().size()/12.0));
        // pwi.println("Instructor availability [h],"+sDoubleFormat.format(1.0*totalAvailableSlots/model.getInstructorConstraints().size()/12.0));
        int nrGroupConstraints = model.getGroupConstraints().size() + model.getSpreadConstraints().size();
        int nrHardGroupConstraints = 0;
        int nrVarsInGroupConstraints = 0;
        for (GroupConstraint gc : model.getGroupConstraints()) {
            if (gc.isHard())
                nrHardGroupConstraints++;
            nrVarsInGroupConstraints += gc.variables().size();
        }
        for (SpreadConstraint sc : model.getSpreadConstraints()) {
            nrVarsInGroupConstraints += sc.variables().size();
        }
        pw.println("Total number of group constraints: " + nrGroupConstraints + " ("
                + sDoubleFormat.format(100.0 * nrGroupConstraints / model.variables().size()) + "%)");
        // pwi.println("Number of group constraints,"+nrGroupConstraints);
        pw.println("Total number of hard group constraints: " + nrHardGroupConstraints + " ("
                + sDoubleFormat.format(100.0 * nrHardGroupConstraints / model.variables().size()) + "%)");
        // pwi.println("Number of hard group constraints,"+nrHardGroupConstraints);
        pw.println("Average classes per group constraint: "
                + sDoubleFormat.format(1.0 * nrVarsInGroupConstraints / nrGroupConstraints));
        // pwi.println("Average classes per group constraint,"+sDoubleFormat.format(1.0*nrVarsInGroupConstraints/nrGroupConstraints));
        pwi.println("Avg. number distribution constraints per class,"
                + sDoubleFormat.format(1.0 * nrVarsInGroupConstraints / model.variables().size()));
        pwi.println("Joint enrollment constraints," + model.getJenrlConstraints().size());
        pw.flush();
        pw.close();
        pwi.flush();
        pwi.close();
    }

    public static void saveOutputCSV(Solution<Lecture, Placement> s, File file) {
        try {
            DecimalFormat dx = new DecimalFormat("000");
            PrintWriter w = new PrintWriter(new FileWriter(file));
            TimetableModel m = (TimetableModel) s.getModel();
            int firstDaySlot = m.getProperties().getPropertyInt("General.FirstDaySlot", Constants.DAY_SLOTS_FIRST);
            int lastDaySlot = m.getProperties().getPropertyInt("General.LastDaySlot", Constants.DAY_SLOTS_LAST);
            int firstWorkDay = m.getProperties().getPropertyInt("General.FirstWorkDay", 0);
            int lastWorkDay = m.getProperties().getPropertyInt("General.LastWorkDay", Constants.NR_DAYS_WEEK - 1);
            if (lastWorkDay < firstWorkDay) lastWorkDay += 7;
            Assignment<Lecture, Placement> a = s.getAssignment();
            int idx = 1;
            w.println("000." + dx.format(idx++) + " Assigned variables," + a.nrAssignedVariables());
            w.println("000." + dx.format(idx++) + " Time [sec]," + sDoubleFormat.format(s.getBestTime()));
            w.println("000." + dx.format(idx++) + " Hard student conflicts," + Math.round(m.getCriterion(StudentHardConflict.class).getValue(a)));
            if (m.getProperties().getPropertyBoolean("General.UseDistanceConstraints", true))
                w.println("000." + dx.format(idx++) + " Distance student conf.," + Math.round(m.getCriterion(StudentDistanceConflict.class).getValue(a)));
            w.println("000." + dx.format(idx++) + " Student conflicts," + Math.round(m.getCriterion(StudentConflict.class).getValue(a)));
            w.println("000." + dx.format(idx++) + " Committed student conflicts," + Math.round(m.getCriterion(StudentCommittedConflict.class).getValue(a)));
            w.println("000." + dx.format(idx++) + " All Student conflicts,"
                    + Math.round(m.getCriterion(StudentConflict.class).getValue(a) + m.getCriterion(StudentCommittedConflict.class).getValue(a)));
            w.println("000." + dx.format(idx++) + " Time preferences,"
                    + sDoubleFormat.format( m.getCriterion(TimePreferences.class).getValue(a)));
            w.println("000." + dx.format(idx++) + " Room preferences," + Math.round(m.getCriterion(RoomPreferences.class).getValue(a)));
            w.println("000." + dx.format(idx++) + " Useless half-hours," + Math.round(m.getCriterion(UselessHalfHours.class).getValue(a)));
            w.println("000." + dx.format(idx++) + " Broken time patterns," + Math.round(m.getCriterion(BrokenTimePatterns.class).getValue(a)));
            w.println("000." + dx.format(idx++) + " Too big room," + Math.round(m.getCriterion(TooBigRooms.class).getValue(a)));
            w.println("000." + dx.format(idx++) + " Distribution preferences," + sDoubleFormat.format(m.getCriterion(DistributionPreferences.class).getValue(a)));
            if (m.getProperties().getPropertyBoolean("General.UseDistanceConstraints", true))
                w.println("000." + dx.format(idx++) + " Back-to-back instructor pref.," + Math.round(m.getCriterion(BackToBackInstructorPreferences.class).getValue(a)));
            if (m.getProperties().getPropertyBoolean("General.DeptBalancing", true)) {
                w.println("000." + dx.format(idx++) + " Dept. balancing penalty," + sDoubleFormat.format(m.getCriterion(DepartmentBalancingPenalty.class).getValue(a)));
            }
            w.println("000." + dx.format(idx++) + " Same subpart balancing penalty," + sDoubleFormat.format(m.getCriterion(SameSubpartBalancingPenalty.class).getValue(a)));
            if (m.getProperties().getPropertyBoolean("General.MPP", false)) {
                Map<String, Double> mppInfo = ((UniversalPerturbationsCounter)((Perturbations)m.getCriterion(Perturbations.class)).getPerturbationsCounter()).getCompactInfo(a, m, false, false);
                int pidx = 51;
                w.println("000." + dx.format(pidx++) + " Perturbation penalty," + sDoubleFormat.format(m.getCriterion(Perturbations.class).getValue(a)));
                w.println("000." + dx.format(pidx++) + " Additional perturbations," + m.perturbVariables(a).size());
                int nrPert = 0, nrStudentPert = 0;
                for (Lecture lecture : m.variables()) {
                    if (lecture.getInitialAssignment() != null)
                        continue;
                    nrPert++;
                    nrStudentPert += lecture.classLimit(a);
                }
                w.println("000." + dx.format(pidx++) + " Given perturbations," + nrPert);
                w.println("000." + dx.format(pidx++) + " Given student perturbations," + nrStudentPert);
                for (String key : new TreeSet<String>(mppInfo.keySet())) {
                    Double value = mppInfo.get(key);
                    w.println("000." + dx.format(pidx++) + " " + key + "," + sDoubleFormat.format(value));
                }
            }
            HashSet<Student> students = new HashSet<Student>();
            int enrls = 0;
            int minRoomPref = 0, maxRoomPref = 0;
            int minGrPref = 0, maxGrPref = 0;
            int minTimePref = 0, maxTimePref = 0;
            int worstInstrPref = 0;
            HashSet<Constraint<Lecture, Placement>> used = new HashSet<Constraint<Lecture, Placement>>();
            for (Lecture lecture : m.variables()) {
                enrls += (lecture.students() == null ? 0 : lecture.students().size());
                students.addAll(lecture.students());

                int[] minMaxRoomPref = lecture.getMinMaxRoomPreference();
                maxRoomPref += minMaxRoomPref[1] - minMaxRoomPref[0];

                double[] minMaxTimePref = lecture.getMinMaxTimePreference();
                maxTimePref += minMaxTimePref[1] - minMaxTimePref[0];
                for (Constraint<Lecture, Placement> c : lecture.constraints()) {
                    if (!used.add(c))
                        continue;

                    if (c instanceof InstructorConstraint) {
                        InstructorConstraint ic = (InstructorConstraint) c;
                        worstInstrPref += ic.getWorstPreference();
                    }

                    if (c instanceof GroupConstraint) {
                        GroupConstraint gc = (GroupConstraint) c;
                        if (gc.isHard())
                            continue;
                        maxGrPref += Math.abs(gc.getPreference()) * (1 + (gc.variables().size() * (gc.variables().size() - 1)) / 2);
                    }
                }
            }
            int totalCommitedPlacements = 0;
            for (Student student : students) {
                if (student.getCommitedPlacements() != null)
                    totalCommitedPlacements += student.getCommitedPlacements().size();
            }
            HashMap<Long, List<Lecture>> subs = new HashMap<Long, List<Lecture>>();
            for (Lecture lecture : m.variables()) {
                if (lecture.isCommitted() || lecture.getScheduler() == null)
                    continue;
                List<Lecture> vars = subs.get(lecture.getScheduler());
                if (vars == null) {
                    vars = new ArrayList<Lecture>();
                    subs.put(lecture.getScheduler(), vars);
                }
                vars.add(lecture);
            }
            int bidx = 101;
            w.println("000." + dx.format(bidx++) + " Assigned variables max," + m.variables().size());
            w.println("000." + dx.format(bidx++) + " Student enrollments," + enrls);
            w.println("000." + dx.format(bidx++) + " Student commited enrollments," + totalCommitedPlacements);
            w.println("000." + dx.format(bidx++) + " All student enrollments," + (enrls + totalCommitedPlacements));
            w.println("000." + dx.format(bidx++) + " Time preferences min," + minTimePref);
            w.println("000." + dx.format(bidx++) + " Time preferences max," + maxTimePref);
            w.println("000." + dx.format(bidx++) + " Room preferences min," + minRoomPref);
            w.println("000." + dx.format(bidx++) + " Room preferences max," + maxRoomPref);
            w.println("000." + dx.format(bidx++) + " Useless half-hours max," +
                    (Constants.sPreferenceLevelStronglyDiscouraged * m.getRoomConstraints().size() * (lastDaySlot - firstDaySlot + 1) * (lastWorkDay - firstWorkDay + 1)));
            w.println("000." + dx.format(bidx++) + " Too big room max," + (Constants.sPreferenceLevelStronglyDiscouraged * m.variables().size()));
            w.println("000." + dx.format(bidx++) + " Distribution preferences min," + minGrPref);
            w.println("000." + dx.format(bidx++) + " Distribution preferences max," + maxGrPref);
            w.println("000." + dx.format(bidx++) + " Back-to-back instructor pref max," + worstInstrPref);
            TooBigRooms tbr = (TooBigRooms)m.getCriterion(TooBigRooms.class);
            for (Long scheduler: new TreeSet<Long>(subs.keySet())) {
                List<Lecture> vars = subs.get(scheduler);
                idx = 001;
                bidx = 101;
                int nrAssg = 0;
                enrls = 0;
                int roomPref = 0;
                minRoomPref = 0;
                maxRoomPref = 0;
                double timePref = 0;
                minTimePref = 0;
                maxTimePref = 0;
                double grPref = 0;
                minGrPref = 0;
                maxGrPref = 0;
                long allSC = 0, hardSC = 0, distSC = 0;
                int instPref = 0;
                worstInstrPref = 0;
                int spreadPen = 0, deptSpreadPen = 0;
                int tooBigRooms = 0;
                int rcs = 0, uselessSlots = 0;
                used = new HashSet<Constraint<Lecture, Placement>>();
                for (Lecture lecture : vars) {
                    if (lecture.isCommitted())
                        continue;
                    enrls += lecture.students().size();
                    Placement placement = a.getValue(lecture);
                    if (placement != null) {
                        nrAssg++;
                    }

                    int[] minMaxRoomPref = lecture.getMinMaxRoomPreference();
                    minRoomPref += minMaxRoomPref[0];
                    maxRoomPref += minMaxRoomPref[1];

                    double[] minMaxTimePref = lecture.getMinMaxTimePreference();
                    minTimePref += minMaxTimePref[0];
                    maxTimePref += minMaxTimePref[1];

                    if (placement != null) {
                        roomPref += placement.getRoomPreference();
                        timePref += placement.getTimeLocation().getNormalizedPreference();
                        if (tbr != null) tooBigRooms += tbr.getPreference(placement);
                    }

                    for (Constraint<Lecture, Placement> c : lecture.constraints()) {
                        if (!used.add(c))
                            continue;

                        if (c instanceof InstructorConstraint) {
                            InstructorConstraint ic = (InstructorConstraint) c;
                            instPref += ic.getPreference(a);
                            worstInstrPref += ic.getWorstPreference();
                        }

                        if (c instanceof DepartmentSpreadConstraint) {
                            DepartmentSpreadConstraint dsc = (DepartmentSpreadConstraint) c;
                            deptSpreadPen += dsc.getPenalty(a);
                        } else if (c instanceof SpreadConstraint) {
                            SpreadConstraint sc = (SpreadConstraint) c;
                            spreadPen += sc.getPenalty(a);
                        }

                        if (c instanceof GroupConstraint) {
                            GroupConstraint gc = (GroupConstraint) c;
                            if (gc.isHard())
                                continue;
                            minGrPref -= Math.abs(gc.getPreference());
                            maxGrPref += 0;
                            grPref += Math.min(0, gc.getCurrentPreference(a));
                            // minGrPref += Math.min(gc.getPreference(), 0);
                            // maxGrPref += Math.max(gc.getPreference(), 0);
                            // grPref += gc.getCurrentPreference();
                        }

                        if (c instanceof JenrlConstraint) {
                            JenrlConstraint jc = (JenrlConstraint) c;
                            if (!jc.isInConflict(a) || !jc.isOfTheSameProblem())
                                continue;
                            Lecture l1 = jc.first();
                            Lecture l2 = jc.second();
                            allSC += jc.getJenrl();
                            if (l1.areStudentConflictsHard(l2))
                                hardSC += jc.getJenrl();
                            Placement p1 = a.getValue(l1);
                            Placement p2 = a.getValue(l2);
                            if (!p1.getTimeLocation().hasIntersection(p2.getTimeLocation()))
                                distSC += jc.getJenrl();
                        }

                        if (c instanceof RoomConstraint) {
                            RoomConstraint rc = (RoomConstraint) c;
                            uselessSlots += UselessHalfHours.countUselessSlotsHalfHours(rc.getContext(a)) + BrokenTimePatterns.countUselessSlotsBrokenTimePatterns(rc.getContext(a));
                            rcs++;
                        }
                    }
                }
                w.println(dx.format(scheduler) + "." + dx.format(idx++) + " Assigned variables," + nrAssg);
                w.println(dx.format(scheduler) + "." + dx.format(bidx++) + " Assigned variables max," + vars.size());
                w.println(dx.format(scheduler) + "." + dx.format(idx++) + " Hard student conflicts," + hardSC);
                w.println(dx.format(scheduler) + "." + dx.format(bidx++) + " Student enrollments," + enrls);
                if (m.getProperties().getPropertyBoolean("General.UseDistanceConstraints", true))
                    w.println(dx.format(scheduler) + "." + dx.format(idx++) + " Distance student conf.," + distSC);
                w.println(dx.format(scheduler) + "." + dx.format(idx++) + " Student conflicts," + allSC);
                w.println(dx.format(scheduler) + "." + dx.format(idx++) + " Time preferences," + timePref);
                w.println(dx.format(scheduler) + "." + dx.format(bidx++) + " Time preferences min," + minTimePref);
                w.println(dx.format(scheduler) + "." + dx.format(bidx++) + " Time preferences max," + maxTimePref);
                w.println(dx.format(scheduler) + "." + dx.format(idx++) + " Room preferences," + roomPref);
                w.println(dx.format(scheduler) + "." + dx.format(bidx++) + " Room preferences min," + minRoomPref);
                w.println(dx.format(scheduler) + "." + dx.format(bidx++) + " Room preferences max," + maxRoomPref);
                w.println(dx.format(scheduler) + "." + dx.format(idx++) + " Useless half-hours," + uselessSlots);
                w.println(dx.format(scheduler) + "." + dx.format(bidx++) + " Useless half-hours max," +
                        (Constants.sPreferenceLevelStronglyDiscouraged * rcs * (lastDaySlot - firstDaySlot + 1) * (lastWorkDay - firstWorkDay + 1)));
                w.println(dx.format(scheduler) + "." + dx.format(idx++) + " Too big room," + tooBigRooms);
                w.println(dx.format(scheduler) + "." + dx.format(bidx++) + " Too big room max," + (Constants.sPreferenceLevelStronglyDiscouraged * vars.size()));
                w.println(dx.format(scheduler) + "." + dx.format(idx++) + " Distribution preferences," + grPref);
                w.println(dx.format(scheduler) + "." + dx.format(bidx++) + " Distribution preferences min," + minGrPref);
                w.println(dx.format(scheduler) + "." + dx.format(bidx++) + " Distribution preferences max," + maxGrPref);
                if (m.getProperties().getPropertyBoolean("General.UseDistanceConstraints", true))
                    w.println(dx.format(scheduler) + "." + dx.format(idx++) + " Back-to-back instructor pref," + instPref);
                w.println(dx.format(scheduler) + "." + dx.format(bidx++) + " Back-to-back instructor pref max," + worstInstrPref);
                if (m.getProperties().getPropertyBoolean("General.DeptBalancing", true)) {
                    w.println(dx.format(scheduler) + "." + dx.format(idx++) + " Department balancing penalty," + sDoubleFormat.format((deptSpreadPen) / 12.0));
                }
                w.println(dx.format(scheduler) + "." + dx.format(idx++) + " Same subpart balancing penalty," + sDoubleFormat.format((spreadPen) / 12.0));
            }
            w.flush();
            w.close();
        } catch (java.io.IOException io) {
            sLogger.error(io.getMessage(), io);
        }
    }
    
    private class ShutdownHook extends Thread {
        Solver<Lecture, Placement> iSolver = null;

        private ShutdownHook(Solver<Lecture, Placement> solver) {
            setName("ShutdownHook");
            iSolver = solver;
        }
        
        @Override
        public void run() {
            try {
                if (iSolver.isRunning()) iSolver.stopSolver();
                Solution<Lecture, Placement> solution = iSolver.lastSolution();
                long lastIt = solution.getIteration();
                double lastTime = solution.getTime();
                DataProperties properties = iSolver.getProperties();
                TimetableModel model = (TimetableModel) solution.getModel();
                File outDir = new File(properties.getProperty("General.Output", "."));

                if (solution.getBestInfo() != null) {
                    Solution<Lecture, Placement> bestSolution = solution;// .cloneBest();
                    sLogger.info("Last solution: " + ToolBox.dict2string(bestSolution.getExtendedInfo(), 1));
                    sLogger.info("Best solution (before restore): " + ToolBox.dict2string(bestSolution.getBestInfo(), 1));
                    bestSolution.restoreBest();
                    sLogger.info("Best solution: " + ToolBox.dict2string(bestSolution.getExtendedInfo(), 1));
                    if (properties.getPropertyBoolean("General.SwitchStudents", true))
                        ((TimetableModel) bestSolution.getModel()).switchStudents(bestSolution.getAssignment());
                    sLogger.info("Best solution: " + ToolBox.dict2string(bestSolution.getExtendedInfo(), 1));
                    saveOutputCSV(bestSolution, new File(outDir, "output.csv"));

                    printSomeStuff(bestSolution);

                    if (properties.getPropertyBoolean("General.Save", true)) {
                        TimetableSaver saver = null;
                        try {
                            saver = (TimetableSaver) Class.forName(getTimetableSaverClass(properties))
                                .getConstructor(new Class[] { Solver.class }).newInstance(new Object[] { iSolver });
                        } catch (ClassNotFoundException e) {
                            System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
                            saver = new TimetableXMLSaver(iSolver);
                        }
                        if ((saver instanceof TimetableXMLSaver) && properties.getProperty("General.SolutionFile") != null)
                            ((TimetableXMLSaver) saver).save(new File(properties.getProperty("General.SolutionFile")));
                        else
                            saver.save();
                    }
                } else {
                    sLogger.info("Last solution:" + ToolBox.dict2string(solution.getExtendedInfo(), 1));
                }

                iCSVFile.close();

                sLogger.info("Total number of done iteration steps:" + lastIt);
                sLogger.info("Achieved speed: " + sDoubleFormat.format(lastIt / lastTime) + " iterations/second");
                
                PrintWriter out = new PrintWriter(new FileWriter(new File(outDir, "solver.html")));
                out.println("<html><title>Save log</title><body>");
                out.println(Progress.getInstance(model).getHtmlLog(Progress.MSGLEVEL_TRACE, true));
                out.println("</html>");
                out.flush();
                out.close();
                Progress.removeInstance(model);

                if (iStat != null) {
                    PrintWriter cbs = new PrintWriter(new FileWriter(new File(outDir, "cbs.txt")));
                    cbs.println(iStat.toString());
                    cbs.flush(); cbs.close();
                }

                System.out.println("Unassigned variables: " + model.nrUnassignedVariables(solution.getAssignment()));
            } catch (Throwable t) {
                sLogger.error("Test failed.", t);
            }
        }
    }
}
