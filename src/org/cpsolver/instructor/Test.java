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

import org.apache.log4j.Logger;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.DefaultParallelAssignment;
import org.cpsolver.ifs.assignment.DefaultSingleAssignment;
import org.cpsolver.ifs.extension.ConflictStatistics;
import org.cpsolver.ifs.extension.Extension;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solution.SolutionListener;
import org.cpsolver.ifs.solver.ParallelSolver;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.instructor.constraints.InstructorConstraint;
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

public class Test extends InstructorSchedulingModel {
    private static Logger sLog = Logger.getLogger(Test.class);
    
    public Test(DataProperties properties) {
        super(properties);
    }

    protected boolean load(File inputFile, Assignment<TeachingRequest, TeachingAssignment> assignment) {
        try {
            Document document = (new SAXReader()).read(inputFile);
            return load(document, assignment);
        } catch (Exception e) {
            sLog.error("Failed to load model from " + inputFile + ": " + e.getMessage(), e);
            return false;
        }
    }
    
    protected void generateReports(File outputDir, Assignment<TeachingRequest, TeachingAssignment> assignment) throws IOException {
        PrintWriter out = new PrintWriter(new File(outputDir, "solution-assignments.csv"));
        out.println("Course,Section,Time,Room,Load,Student,Name,Instructor Pref,Course Pref,Attribute Pref,Time Pref,Back-To-Back,Different Lecture,Overlap [h]");
        double diffRoomWeight = getProperties().getPropertyDouble("BackToBack.DifferentRoomWeight", 0.8);
        double diffTypeWeight = getProperties().getPropertyDouble("BackToBack.DifferentTypeWeight", 0.5);
        for (TeachingRequest request : variables()) {
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
            out.print("," + new DecimalFormat("0.0").format(request.getLoad()));
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
        out.println("Student,Name,Preference,Not Available,Time Pref,Course Pref,Back-to-Back,Max Load,Assigned Load,Back-To-Back,Different Lecture,Overlap [h],1st Assignment,2nd Assignment, 3rd Assignment");
        for (Constraint<TeachingRequest, TeachingAssignment> constraint : constraints()) {
            if (constraint instanceof InstructorConstraint) {
                InstructorConstraint ic = (InstructorConstraint) constraint;
                Instructor instructor = ic.getInstructor();
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
                out.print("," + new DecimalFormat("0.0").format(instructor.getMaxLoad()));
                
                InstructorConstraint.Context context = ic.getContext(assignment);
                out.print("," + new DecimalFormat("0.0").format(context.getLoad()));
                out.print("," + (context.countBackToBackPercentage() == 0.0 ? "" : new DecimalFormat("0.0").format(100.0 * context.countBackToBackPercentage())));
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
        }
        out.flush();
        out.close();
    }
    
    protected void save(File outputDir, Assignment<TeachingRequest, TeachingAssignment> assignment) {
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
    
    public void execute() {
        int nrSolvers = getProperties().getPropertyInt("Parallel.NrSolvers", 1);
        Solver<TeachingRequest, TeachingAssignment> solver = (nrSolvers == 1 ? new Solver<TeachingRequest, TeachingAssignment>(getProperties()) : new ParallelSolver<TeachingRequest, TeachingAssignment>(getProperties()));
        
        Assignment<TeachingRequest, TeachingAssignment> assignment = (nrSolvers <= 1 ? new DefaultSingleAssignment<TeachingRequest, TeachingAssignment>() : new DefaultParallelAssignment<TeachingRequest, TeachingAssignment>());
        if (!load(new File(getProperties().getProperty("input", "input/solution.xml")), assignment))
            return;
        
        solver.setInitalSolution(new Solution<TeachingRequest, TeachingAssignment>(this, assignment));

        solver.currentSolution().addSolutionListener(new SolutionListener<TeachingRequest, TeachingAssignment>() {
            @Override
            public void solutionUpdated(Solution<TeachingRequest, TeachingAssignment> solution) {
            }

            @Override
            public void getInfo(Solution<TeachingRequest, TeachingAssignment> solution, Map<String, String> info) {
            }

            @Override
            public void getInfo(Solution<TeachingRequest, TeachingAssignment> solution, Map<String, String> info,
                    Collection<TeachingRequest> variables) {
            }

            @Override
            public void bestCleared(Solution<TeachingRequest, TeachingAssignment> solution) {
            }

            @Override
            public void bestSaved(Solution<TeachingRequest, TeachingAssignment> solution) {
                Model<TeachingRequest, TeachingAssignment> m = solution.getModel();
                Assignment<TeachingRequest, TeachingAssignment> a = solution.getAssignment();
                System.out.println("**BEST[" + solution.getIteration() + "]** " + m.toString(a));
            }

            @Override
            public void bestRestored(Solution<TeachingRequest, TeachingAssignment> solution) {
            }
        });

        solver.start();
        try {
            solver.getSolverThread().join();
        } catch (InterruptedException e) {
        }
        
        Solution<TeachingRequest, TeachingAssignment> solution = solver.lastSolution();
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
        
        ConflictStatistics<TeachingRequest, TeachingAssignment> cbs = null;
        for (Extension<TeachingRequest, TeachingAssignment> extension : solver.getExtensions()) {
            if (ConflictStatistics.class.isInstance(extension)) {
                cbs = (ConflictStatistics<TeachingRequest, TeachingAssignment>) extension;
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
