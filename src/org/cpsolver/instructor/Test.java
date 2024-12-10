package org.cpsolver.instructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.DefaultParallelAssignment;
import org.cpsolver.ifs.assignment.DefaultSingleAssignment;
import org.cpsolver.ifs.extension.ConflictStatistics;
import org.cpsolver.ifs.extension.Extension;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solution.SolutionListener;
import org.cpsolver.ifs.solver.ParallelSolver;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.instructor.model.Course;
import org.cpsolver.instructor.model.Instructor;
import org.cpsolver.instructor.model.InstructorSchedulingModel;
import org.cpsolver.instructor.model.Preference;
import org.cpsolver.instructor.model.Section;
import org.cpsolver.instructor.model.TeachingAssignment;
import org.cpsolver.instructor.model.TeachingRequest;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
/**
 * A main class for running of the instructor scheduling solver from command line. <br>
 * Instructor scheduling is a process of assigning instructors (typically teaching assistants) to classes
 * after the course timetabling and student scheduling is done.
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Instructor Sectioning)<br>
 *          Copyright (C) 2016 Tomas Muller<br>
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
public class Test extends InstructorSchedulingModel {
    private static Logger sLog = org.apache.logging.log4j.LogManager.getLogger(Test.class);
    
    /**
     * Constructor
     * @param properties data properties
     */
    public Test(DataProperties properties) {
        super(properties);
    }

    /**
     * Load input problem
     * @param inputFile input file (or folder)
     * @param assignment current assignments
     * @return true if the problem was successfully loaded in
     */
    protected boolean load(File inputFile, Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) {
        try {
            Document document = (new SAXReader()).read(inputFile);
            return load(document, assignment);
        } catch (Exception e) {
            sLog.error("Failed to load model from " + inputFile + ": " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Generate a few reports
     * @param outputDir output directory
     * @param assignment current assignments
     * @throws IOException when there is an IO error
     */
    protected void generateReports(File outputDir, Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) throws IOException {
        PrintWriter out = new PrintWriter(new File(outputDir, "solution-assignments.csv"));
        out.println("Course,Section,Time,Room,Load,Student,Name,Instructor Pref,Course Pref,Attribute Pref,Time Pref,Back-To-Back,Same-Days,Same-Room,Different Lecture,Overlap [h]");
        double diffRoomWeight = getProperties().getPropertyDouble("BackToBack.DifferentRoomWeight", 0.8);
        double diffTypeWeight = getProperties().getPropertyDouble("BackToBack.DifferentTypeWeight", 0.5);
        double diffRoomWeightSD = getProperties().getPropertyDouble("SameDays.DifferentRoomWeight", 0.8);
        double diffTypeWeightSD = getProperties().getPropertyDouble("SameDays.DifferentTypeWeight", 0.5);
        double diffTypeWeightSR = getProperties().getPropertyDouble("SameRoom.DifferentTypeWeight", 0.5);
        for (TeachingRequest.Variable request : variables()) {
            out.print(request.getCourse().getCourseName());
            String sect = "", time = "", room = "";
            for (Iterator<Section> i = request.getSections().iterator(); i.hasNext(); ) {
                Section section = i.next();
                sect += section.getSectionName();
                time += (section.getTime() == null ? "-" : section.getTime().getDayHeader() + " " + section.getTime().getStartTimeHeader(true));
                room += (section.getRoom() == null ? "-" : section.getRoom());
                if (i.hasNext()) { sect += ", "; time += ", "; room += ", "; }
            }
            out.print(",\"" + sect + "\",\"" + time + "\",\"" + room + "\"");
            out.print("," + new DecimalFormat("0.0").format(request.getRequest().getLoad()));
            TeachingAssignment ta = assignment.getValue(request);
            if (ta != null) {
                Instructor instructor = ta.getInstructor();
                out.print("," + instructor.getExternalId());
                out.print(",\"" + instructor.getName() + "\"");
                out.print("," + (ta.getInstructorPreference() == 0 ? "" : ta.getInstructorPreference()));
                out.print("," + (ta.getCoursePreference() == 0 ? "" : ta.getCoursePreference()));
                out.print("," + (ta.getAttributePreference() == 0 ? "" : ta.getAttributePreference()));
                out.print("," + (ta.getTimePreference() == 0 ? "" : ta.getTimePreference()));
                double b2b = instructor.countBackToBacks(assignment, ta, diffRoomWeight, diffTypeWeight);
                out.print("," + (b2b == 0.0 ? "" : new DecimalFormat("0.0").format(b2b)));
                double sd = instructor.countSameDays(assignment, ta, diffRoomWeightSD, diffTypeWeightSD);
                out.print("," + (sd == 0.0 ? "" : new DecimalFormat("0.0").format(sd)));
                double sr = instructor.countSameRooms(assignment, ta, diffTypeWeightSR);
                out.print("," + (sr == 0.0 ? "" : new DecimalFormat("0.0").format(sr)));
                double dl = instructor.differentLectures(assignment, ta);
                out.print("," + (dl == 0.0 ? "" : new DecimalFormat("0.0").format(dl)));
                double sh = instructor.share(assignment, ta);
                out.print("," + (sh == 0 ? "" : new DecimalFormat("0.0").format(sh / 12.0)));
            }
            out.println();
        }
        out.flush();
        out.close();

        out = new PrintWriter(new File(outputDir, "solution-students.csv"));
        out.println("Student,Name,Preference,Not Available,Time Pref,Course Pref,Back-to-Back,Same-Days,Same-Room,Max Load,Assigned Load,Back-To-Back,Same-Days,Same-Room,Different Lecture,Overlap [h],1st Assignment,2nd Assignment, 3rd Assignment");
        for (Instructor instructor: getInstructors()) {
            out.print(instructor.getExternalId());
            out.print(",\"" + instructor.getName() + "\"");
            out.print("," + (instructor.getPreference() == 0 ? "" : instructor.getPreference()));
            out.print(",\"" + instructor.getAvailable() + "\"");
            String timePref = "";
            for (Preference<TimeLocation> p: instructor.getTimePreferences()) {
                if (!p.isProhibited()) {
                    if (!timePref.isEmpty()) timePref += ", ";
                    timePref += p.getTarget().getLongName(true).trim() + ": " + (p.isRequired() ? "R" : p.isProhibited() ? "P" : p.getPreference());
                }
            }
            out.print(",\"" + timePref + "\"");
            String coursePref = "";
            for (Preference<Course> p: instructor.getCoursePreferences()) {
                if (!coursePref.isEmpty()) coursePref += ", ";
                coursePref += p.getTarget().getCourseName() + ": " + (p.isRequired() ? "R" : p.isProhibited() ? "P" : p.getPreference());
            }
            out.print(",\"" + coursePref + "\"");
            out.print("," + (instructor.getBackToBackPreference() == 0 ? "" : instructor.getBackToBackPreference()));
            out.print("," + (instructor.getSameDaysPreference() == 0 ? "" : instructor.getSameDaysPreference()));
            out.print("," + (instructor.getSameRoomPreference() == 0 ? "" : instructor.getSameRoomPreference()));
            out.print("," + new DecimalFormat("0.0").format(instructor.getMaxLoad()));
            
            Instructor.Context context = instructor.getContext(assignment);
            out.print("," + new DecimalFormat("0.0").format(context.getLoad()));
            out.print("," + (context.countBackToBackPercentage() == 0.0 ? "" : new DecimalFormat("0.0").format(100.0 * context.countBackToBackPercentage())));
            out.print("," + (context.countSameDaysPercentage() == 0.0 ? "" : new DecimalFormat("0.0").format(100.0 * context.countSameDaysPercentage())));
            out.print("," + (context.countSameRoomPercentage() == 0.0 ? "" : new DecimalFormat("0.0").format(100.0 * context.countSameRoomPercentage())));
            out.print("," + (context.countDifferentLectures() == 0.0 ? "" : new DecimalFormat("0.0").format(100.0 * context.countDifferentLectures())));
            out.print("," + (context.countTimeOverlaps() == 0.0 ? "" : new DecimalFormat("0.0").format(context.countTimeOverlaps() / 12.0)));
            for (TeachingAssignment ta : context.getAssignments()) {
                String sect = "";
                for (Iterator<Section> i = ta.variable().getSections().iterator(); i.hasNext(); ) {
                    Section section = i.next();
                    sect += section.getSectionName() + (section.getTime() == null ? "" : " " + section.getTime().getDayHeader() + " " + section.getTime().getStartTimeHeader(true));
                    if (i.hasNext()) sect += ", ";
                }
                out.print(",\"" + ta.variable().getCourse() + " " + sect + "\"");
            }
            out.println();
        }
        out.flush();
        out.close();
    }
    
    /**
     * Save the problem and the resulting assignment
     * @param outputDir output directory
     * @param assignment final assignment
     */
    protected void save(File outputDir, Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) {
        try {
            File outFile = new File(outputDir, "solution.xml");
            FileOutputStream fos = new FileOutputStream(outFile);
            try {
                (new XMLWriter(fos, OutputFormat.createPrettyPrint())).write(save(assignment));
                fos.flush();
            } finally {
                fos.close();
            }
        } catch (Exception e) {
            sLog.error("Failed to save solution: " + e.getMessage(), e);
        }
    }
    
    /**
     * Run the problem
     */
    public void execute() {
        int nrSolvers = getProperties().getPropertyInt("Parallel.NrSolvers", 1);
        Solver<TeachingRequest.Variable, TeachingAssignment> solver = (nrSolvers == 1 ? new Solver<TeachingRequest.Variable, TeachingAssignment>(getProperties()) : new ParallelSolver<TeachingRequest.Variable, TeachingAssignment>(getProperties()));
        
        Assignment<TeachingRequest.Variable, TeachingAssignment> assignment = (nrSolvers <= 1 ? new DefaultSingleAssignment<TeachingRequest.Variable, TeachingAssignment>() : new DefaultParallelAssignment<TeachingRequest.Variable, TeachingAssignment>());
        if (!load(new File(getProperties().getProperty("input", "input/solution.xml")), assignment))
            return;
        
        solver.setInitalSolution(new Solution<TeachingRequest.Variable, TeachingAssignment>(this, assignment));

        solver.currentSolution().addSolutionListener(new SolutionListener<TeachingRequest.Variable, TeachingAssignment>() {
            @Override
            public void solutionUpdated(Solution<TeachingRequest.Variable, TeachingAssignment> solution) {
            }

            @Override
            public void getInfo(Solution<TeachingRequest.Variable, TeachingAssignment> solution, Map<String, String> info) {
            }

            @Override
            public void getInfo(Solution<TeachingRequest.Variable, TeachingAssignment> solution, Map<String, String> info, Collection<TeachingRequest.Variable> variables) {
            }

            @Override
            public void bestCleared(Solution<TeachingRequest.Variable, TeachingAssignment> solution) {
            }

            @Override
            public void bestSaved(Solution<TeachingRequest.Variable, TeachingAssignment> solution) {
                Model<TeachingRequest.Variable, TeachingAssignment> m = solution.getModel();
                Assignment<TeachingRequest.Variable, TeachingAssignment> a = solution.getAssignment();
                System.out.println("**BEST[" + solution.getIteration() + "]** " + m.toString(a));
            }

            @Override
            public void bestRestored(Solution<TeachingRequest.Variable, TeachingAssignment> solution) {
            }
        });

        solver.start();
        try {
            solver.getSolverThread().join();
        } catch (InterruptedException e) {
        }
        
        Solution<TeachingRequest.Variable, TeachingAssignment> solution = solver.lastSolution();
        solution.restoreBest();

        sLog.info("Best solution found after " + solution.getBestTime() + " seconds (" + solution.getBestIteration() + " iterations).");
        sLog.info("Number of assigned variables is " + solution.getModel().assignedVariables(solution.getAssignment()).size());
        sLog.info("Total value of the solution is " + solution.getModel().getTotalValue(solution.getAssignment()));

        sLog.info("Info: " + ToolBox.dict2string(solution.getExtendedInfo(), 2));

        File outDir = new File(getProperties().getProperty("output", "output"));
        outDir.mkdirs();
        
        save(outDir, solution.getAssignment());
        
        try {
            generateReports(outDir, assignment);
        } catch (IOException e) {
            sLog.error("Failed to write reports: " + e.getMessage(), e);
        }
        
        ConflictStatistics<TeachingRequest.Variable, TeachingAssignment> cbs = null;
        for (Extension<TeachingRequest.Variable, TeachingAssignment> extension : solver.getExtensions()) {
            if (ConflictStatistics.class.isInstance(extension)) {
                cbs = (ConflictStatistics<TeachingRequest.Variable, TeachingAssignment>) extension;
            }
        }
        
        if (cbs != null) {
            PrintWriter out = null;
            try {
                out = new PrintWriter(new FileWriter(new File(outDir, "cbs.txt")));
                out.println(cbs.toString());
                out.flush(); out.close();
            } catch (IOException e) {
                sLog.error("Failed to write CBS: " + e.getMessage(), e);
            } finally {
                if (out != null) out.close();
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        ToolBox.configureLogging();

        DataProperties config = new DataProperties();
        if (System.getProperty("config") == null) {
            config.load(Test.class.getClass().getResourceAsStream("/org/cpsolver/instructor/default.properties"));
        } else {
            config.load(new FileInputStream(System.getProperty("config")));
        }
        config.putAll(System.getProperties());
        
        new Test(config).execute();
    }

}
