package net.sf.cpsolver.studentsct;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solution.SolutionListener;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.solver.SolverListener;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.JProf;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.check.OverlapCheck;
import net.sf.cpsolver.studentsct.check.SectionLimitCheck;
import net.sf.cpsolver.studentsct.extension.DistanceConflict;
import net.sf.cpsolver.studentsct.heuristics.StudentSctNeighbourSelection;
import net.sf.cpsolver.studentsct.heuristics.general.BacktrackNeighbourSelection;
import net.sf.cpsolver.studentsct.heuristics.selection.BranchBoundSelection;
import net.sf.cpsolver.studentsct.heuristics.selection.SwapStudentSelection;
import net.sf.cpsolver.studentsct.heuristics.studentord.StudentOrder;
import net.sf.cpsolver.studentsct.heuristics.studentord.StudentRandomOrder;
import net.sf.cpsolver.studentsct.model.AcademicAreaCode;
import net.sf.cpsolver.studentsct.model.Config;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Offering;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;
import net.sf.cpsolver.studentsct.model.Subpart;
import net.sf.cpsolver.studentsct.report.CourseConflictTable;
import net.sf.cpsolver.studentsct.report.DistanceConflictTable;

/**
 * A main class for running of the student sectioning solver from command line.
 * <br><br>
 * Usage:<br>
 * java -Xmx1024m -jar studentsct-1.1.jar config.properties [input_file] [output_folder] [batch|online|simple]<br>
 * <br>
 * Modes:<br>
 * &nbsp;&nbsp;batch ... batch sectioning mode (default mode -- IFS solver with {@link StudentSctNeighbourSelection} is used)<br>
 * &nbsp;&nbsp;online ... online sectioning mode (students are sectioned one by one, sectioning info (expected/held space) is used)<br>
 * &nbsp;&nbsp;simple ...  simple sectioning mode (students are sectioned one by one, sectioning info is not used)<br>
 * See http://www.unitime.org for example configuration files and banchmark data sets.<br><br>
 * 
 * The test does the following steps:<ul>
 * <li>Provided property file is loaded (see {@link DataProperties}).
 * <li>Output folder is created (General.Output property) and loggings is setup (using log4j).
 * <li>Input data are loaded from the given XML file (calling {@link StudentSectioningXMLLoader#load()}).
 * <li>Solver is executed (see {@link Solver}).
 * <li>Resultant solution is saved to an XML file (calling {@link StudentSectioningXMLSaver#save()}.
 * </ul>
 * Also, a log and some reports (e.g., {@link CourseConflictTable} and {@link DistanceConflictTable}) are created in the output folder. 
 *
 * <br><br>
 * Parameters:
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>Test.LastLikeCourseDemands</td><td>{@link String}</td><td>Load last-like course demands from the given XML file (in the format that is being used for last like course demand table in the timetabling application)</td></tr>
 * <tr><td>Test.StudentInfos</td><td>{@link String}</td><td>Load last-like course demands from the given XML file (in the format that is being used for last like course demand table in the timetabling application)</td></tr>
 * <tr><td>Test.CrsReq</td><td>{@link String}</td><td>Load student requests from the given semi-colon separated list files (in the format that is being used by the old MSF system)</td></tr>
 * <tr><td>Test.EtrChk</td><td>{@link String}</td><td>Load student information (academic area, classification, major, minor) from the given semi-colon separated list files (in the format that is being used by the old MSF system)</td></tr>
 * <tr><td>Sectioning.UseStudentPreferencePenalties</td><td>{@link Boolean}</td><td>If true, {@link StudentPreferencePenalties} are used (applicable only for online sectioning)</td></tr>
 * <tr><td>Test.StudentOrder</td><td>{@link String}</td><td>A class that is used for ordering of students (must be an interface of {@link StudentOrder}, default is {@link StudentRandomOrder}, not applicable only for batch sectioning)</td></tr>
 * </table>
 * <br><br>
 * 
 * @version
 * StudentSct 1.1 (Student Sectioning)<br>
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
    private static java.text.SimpleDateFormat sDateFormat = new java.text.SimpleDateFormat("yyMMdd_HHmmss",java.util.Locale.US);
    private static DecimalFormat sDF = new DecimalFormat("0.000");
    
    /** Load student sectioning model */
    public static StudentSectioningModel loadModel(DataProperties cfg) {
        StudentSectioningModel model = new StudentSectioningModel(cfg);
        try {
            new StudentSectioningXMLLoader(model).load();
            if (cfg.getProperty("Test.LastLikeCourseDemands")!=null)
                loadLastLikeCourseDemandsXml(model, new File(cfg.getProperty("Test.LastLikeCourseDemands")));
            if (cfg.getProperty("Test.StudentInfos")!=null)
                loadStudentInfoXml(model, new File(cfg.getProperty("Test.StudentInfos")));
            if (cfg.getProperty("Test.CrsReq")!=null)
                loadCrsReqFiles(model, cfg.getProperty("Test.CrsReq"));
        } catch (Exception e) {
            sLog.error("Unable to load model, reason: "+e.getMessage(), e);
            return null;
        }
        if (cfg.getPropertyBoolean("Debug.DistanceConflict",false))
            DistanceConflict.sDebug=true;
        if (cfg.getPropertyBoolean("Debug.BranchBoundEnrollmentsSelection",false))
            BranchBoundSelection.sDebug=true;
        if (cfg.getPropertyBoolean("Debug.SwapStudentsSelection",false))
            SwapStudentSelection.sDebug=true;
        if (cfg.getPropertyBoolean("Debug.BacktrackNeighbourSelection",false))
            BacktrackNeighbourSelection.sDebug=true;
        
        return model;
    }

    /** Batch sectioning test */
    public static Solution batchSectioning(DataProperties cfg) {
        StudentSectioningModel model = loadModel(cfg);
        if (model==null) return null;
        
        model.clearOnlineSectioningInfos();
        
        Solution solution = solveModel(model, cfg);
        
        printInfo(solution, true, true, true);
        
        try {
            Solver solver = new Solver(cfg);
            solver.setInitalSolution(solution);
            new StudentSectioningXMLSaver(solver).save(new File(new File(cfg.getProperty("General.Output",".")),"solution.xml"));
        } catch (Exception e) {
            sLog.error("Unable to save solution, reason: "+e.getMessage(),e);
        }
        
        return solution;
    }

    /** Online sectioning test */
    public static Solution onlineSectioning(DataProperties cfg) {
        StudentSectioningModel model = loadModel(cfg);
        if (model==null) return null;
        
        boolean usePenalties = cfg.getPropertyBoolean("Sectioning.UseOnlinePenalties", true);
        boolean useStudentPrefPenalties = cfg.getPropertyBoolean("Sectioning.UseStudentPreferencePenalties", false);
        Solution solution = new Solution(model,0,0);
        solution.addSolutionListener(new SolutionListener() {
            public void solutionUpdated(Solution solution) {}
            public void getInfo(Solution solution, java.util.Dictionary info) {}
            public void getInfo(Solution solution, java.util.Dictionary info, java.util.Vector variables) {}
            public void bestCleared(Solution solution) {}
            public void bestSaved(Solution solution) {
                StudentSectioningModel m = (StudentSectioningModel)solution.getModel();
                sLog.debug("**BEST** V:"+m.assignedVariables().size()+"/"+m.variables().size()+" - S:"+
                        m.nrComplete()+"/"+m.getStudents().size()+" - TV:"+sDF.format(m.getTotalValue())+
                        (m.getDistanceConflict()==null?"":" - DC:"+sDF.format(m.getDistanceConflict().getTotalNrConflicts())));
            }
            public void bestRestored(Solution solution) {}
        });
        double startTime = JProf.currentTimeSec();
        
        
        Solver solver = new Solver(cfg);
        solver.setInitalSolution(solution);
        solver.initSolver();

        BranchBoundSelection bbSelection = new BranchBoundSelection(cfg);
        bbSelection.init(solver);
        
        double totalPenalty = 0, minPenalty = 0, maxPenalty = 0;
        
        Vector students = model.getStudents();
        try {
            Class studentOrdClass = Class.forName(model.getProperties().getProperty("Test.StudentOrder", StudentRandomOrder.class.getName()));
            StudentOrder studentOrd = (StudentOrder)studentOrdClass.getConstructor(new Class[] {DataProperties.class}).newInstance(new Object[] {model.getProperties()});
            students = studentOrd.order(model.getStudents());
        } catch (Exception e) {
            sLog.error("Unable to reorder students, reason: "+e.getMessage(),e);
        }
        
        for (Enumeration e=students.elements();e.hasMoreElements();) {
            Student student = (Student)e.nextElement();
            sLog.info("Sectioning student: "+student);
            if (useStudentPrefPenalties)
                StudentPreferencePenalties.setPenalties(student, cfg.getPropertyInt("Sectioning.Distribution", StudentPreferencePenalties.sDistTypePreference));
            else if (usePenalties) 
                setPenalties(student);
            Neighbour neighbour = bbSelection.getSelection(student).select();
            if (neighbour!=null) {
                neighbour.assign(solution.getIteration());
                sLog.info("Student "+student+" enrolls into "+neighbour);
                if (usePenalties && !useStudentPrefPenalties) updateSpace(student);
            } else {
                sLog.warn("No solution found.");
            }
            solution.update(JProf.currentTimeSec()-startTime);
            solution.saveBest();
            totalPenalty += getPenalty(student);
            minPenalty += getMinPenaltyOfAssignedCourseRequests(student);
            maxPenalty += getMaxPenaltyOfAssignedCourseRequests(student);
        }
        
        printInfo(solution, true, false, true);
        
        sLog.info("Overall penalty is "+totalPenalty+" ("+getPerc(totalPenalty, minPenalty, maxPenalty)+")");
        
        try {
            new StudentSectioningXMLSaver(solver).save(new File(new File(cfg.getProperty("General.Output",".")),"solution.xml"));
        } catch (Exception e) {
            sLog.error("Unable to save solution, reason: "+e.getMessage(),e);
        }

        return solution;
    }
    
    /** Sum of penalties of sections into which a student is enrolled */
    public static double getPenalty(Student student) {
        double penalty = 0.0;
        for (Enumeration e=student.getRequests().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            Enrollment enrollment = (Enrollment)request.getAssignment();
            if (enrollment!=null)
                penalty += enrollment.getPenalty();
        }
        return penalty;
    }

    /** Minimal penalty of courses in which a student is enrolled */
    public static double getMinPenaltyOfAssignedCourseRequests(Student student) {
        double penalty = 0.0;
        for (Enumeration e=student.getRequests().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (request.getAssignment()!=null && request instanceof CourseRequest)
                penalty += ((CourseRequest)request).getMinPenalty(); 
        }
        return penalty;
    }

    /** Maximal penalty of courses in which a student is enrolled */
    public static double getMaxPenaltyOfAssignedCourseRequests(Student student) {
        double penalty = 0.0;
        for (Enumeration e=student.getRequests().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (request.getAssignment()!=null && request instanceof CourseRequest)
                penalty += ((CourseRequest)request).getMaxPenalty(); 
        }
        return penalty;
    }

    /** 
     * Compute percentage 
     * @param value current value
     * @param min minimal bound
     * @param max maximal bound
     * @return (value-min)/(max-min)
     */
    public static String getPerc(double value, double min, double max) {
        if (max==min) return sDF.format(100.0);
        return sDF.format(100.0 - 100.0*(value-min)/(max-min));
    }
    
    /**
     * Print some information about the solution
     * @param solution given solution
     * @param computeTables true, if reports {@link CourseConflictTable} and {@link DistanceConflictTable} are to be computed as well
     * @param computeSectInfos true, if online sectioning infou is to be computed as well (see {@link StudentSectioningModel#computeOnlineSectioningInfos()})
     * @param runChecks true, if checks {@link OverlapCheck} and {@link SectionLimitCheck} are to be performed as well
     */
    public static void printInfo(Solution solution, boolean computeTables, boolean computeSectInfos, boolean runChecks) {
        StudentSectioningModel model = (StudentSectioningModel)solution.getModel();

        if (computeTables) {
            if (solution.getModel().assignedVariables().size()>0) {
                try {
                    File outDir = new File(model.getProperties().getProperty("General.Output","."));
                    outDir.mkdirs();
                    CourseConflictTable cct = new CourseConflictTable((StudentSectioningModel)solution.getModel());
                    cct.createTable(true, false).save(new File(outDir, "conflicts-lastlike.csv"));
                    cct.createTable(false, true).save(new File(outDir, "conflicts-real.csv"));
                    
                    DistanceConflictTable dct = new DistanceConflictTable((StudentSectioningModel)solution.getModel());
                    dct.createTable(true, false).save(new File(outDir, "distances-lastlike.csv"));
                    dct.createTable(false, true).save(new File(outDir, "distances-real.csv"));
                } catch (IOException e) {
                    sLog.error(e.getMessage(),e);
                }
            }

            solution.saveBest();
        }
        
        if (computeSectInfos)
            model.computeOnlineSectioningInfos();
        
        if (runChecks) {
            new OverlapCheck(model).check();
            new SectionLimitCheck(model).check();
        }

        sLog.info("Best solution found after "+solution.getBestTime()+" seconds ("+solution.getBestIteration()+" iterations).");
        sLog.info("Number of assigned variables is "+solution.getModel().assignedVariables().size());
        sLog.info("Number of students with complete schedule is "+model.nrComplete());
        sLog.info("Total value of the solution is "+solution.getModel().getTotalValue());
        sLog.info("Average unassigned priority is "+sDF.format(model.avgUnassignPriority()));
        sLog.info("Average number of requests is "+sDF.format(model.avgNrRequests()));
        sLog.info("Unassigned request weight is "+sDF.format(model.getUnassignedRequestWeight())+" / "+sDF.format(model.getTotalRequestWeight()));
        sLog.info("Info: "+solution.getInfo());
    }
    
    /** Set online sectioning penalties to all sections of all courses of the given student */
    private static void setPenalties(Student student) {
        for (Enumeration e=student.getRequests().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (!(request instanceof CourseRequest)) continue;
            CourseRequest courseRequest = (CourseRequest)request;
            for (Enumeration f=courseRequest.getCourses().elements();f.hasMoreElements();) {
                Course course = (Course)f.nextElement();
                for (Enumeration g=course.getOffering().getConfigs().elements();g.hasMoreElements();) {
                    Config config = (Config)g.nextElement();
                    for (Enumeration h=config.getSubparts().elements();h.hasMoreElements();) {
                        Subpart subpart = (Subpart)h.nextElement();
                        for (Enumeration i=subpart.getSections().elements();i.hasMoreElements();) {
                            Section section = (Section)i.nextElement();
                            section.setPenalty(section.getOnlineSectioningPenalty());
                        }
                    }
                }
            }
            courseRequest.clearCache();
        }
    }

    /** Update online sectioning info after the given student is sectioned */
    private static void updateSpace(Student student) {
        for (Enumeration e=student.getRequests().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (!(request instanceof CourseRequest)) continue;
            CourseRequest courseRequest = (CourseRequest)request;
            if (courseRequest.getAssignment()==null) return; //not enrolled --> no update
            Enrollment enrollment = (Enrollment)courseRequest.getAssignment();
            for (Iterator i=enrollment.getAssignments().iterator();i.hasNext();) {
                Section section = (Section)i.next();
                section.setSpaceHeld(section.getSpaceHeld()-courseRequest.getWeight());
                //sLog.debug("  -- space held for "+section+" decreased by 1 (to "+section.getSpaceHeld()+")");
            }
            Vector feasibleEnrollments = new Vector();
            for (Enumeration g=courseRequest.values().elements();g.hasMoreElements();) {
                Enrollment enrl = (Enrollment)g.nextElement();
                boolean overlaps = false;
                for (Enumeration h=courseRequest.getStudent().getRequests().elements();h.hasMoreElements();) {
                    CourseRequest otherCourseRequest = (CourseRequest)h.nextElement();
                    if (otherCourseRequest.equals(courseRequest)) continue;
                    Enrollment otherErollment = (Enrollment)otherCourseRequest.getAssignment();
                    if (otherErollment==null) continue;
                    if (enrl.isOverlapping(otherErollment)) {
                        overlaps = true; break;
                    }
                }
                if (!overlaps)
                    feasibleEnrollments.add(enrl);
            }
            double decrement = courseRequest.getWeight() / feasibleEnrollments.size();
            for (Enumeration g=feasibleEnrollments.elements();g.hasMoreElements();) {
                Enrollment feasibleEnrollment = (Enrollment)g.nextElement();
                for (Iterator i=feasibleEnrollment.getAssignments().iterator();i.hasNext();) {
                    Section section = (Section)i.next();
                    section.setSpaceExpected(section.getSpaceExpected()-decrement);
                    //sLog.debug("  -- space expected for "+section+" decreased by "+decrement+" (to "+section.getSpaceExpected()+")");
                }
            }
        }
    }

    /** Solve the student sectioning problem using IFS solver */
    public static Solution solveModel(StudentSectioningModel model, DataProperties cfg) {
        Solver solver = new Solver(cfg);
        Solution solution = new Solution(model,0,0);
        solver.setInitalSolution(solution);
        solver.addSolverListener(new SolverListener() {
            public boolean variableSelected(long iteration, Variable variable) {
                return true;
            }
            public boolean valueSelected(long iteration, Variable variable, Value value) {
                return true;
            }
            public boolean neighbourSelected(long iteration, Neighbour neighbour) {
                sLog.debug("Select["+iteration+"]: "+neighbour);
                return true;
            }
        });
        solution.addSolutionListener(new SolutionListener() {
            public void solutionUpdated(Solution solution) {}
            public void getInfo(Solution solution, java.util.Dictionary info) {}
            public void getInfo(Solution solution, java.util.Dictionary info, java.util.Vector variables) {}
            public void bestCleared(Solution solution) {}
            public void bestSaved(Solution solution) {
                StudentSectioningModel m = (StudentSectioningModel)solution.getModel();
                sLog.debug("**BEST** V:"+m.assignedVariables().size()+"/"+m.variables().size()+" - S:"+
                        m.nrComplete()+"/"+m.getStudents().size()+" - TV:"+sDF.format(m.getTotalValue())+
                        (m.getDistanceConflict()==null?"":" - DC:"+sDF.format(m.getDistanceConflict().getTotalNrConflicts())));
            }
            public void bestRestored(Solution solution) {}
        });

        solver.start();
        try {
            solver.getSolverThread().join();
        } catch (InterruptedException e) {}
        
        solution = solver.lastSolution();
        solution.restoreBest();
        
        printInfo(solution, false, false, false);

        return solution;
    }
    
    
    /**
     *  Compute last-like student weight for the given course
     * @param course given course
     * @param lastLike number of last-like students for the course
     * @return weight of a student request for the given course
     */
    public static double getLastLikeStudentWeight(Course course, int lastLike) {
        int projected = course.getProjected();
        int limit = course.getLimit();
        if (projected<=0) {
            sLog.warn("  -- No projected demand for course "+course.getName()+", using course limit ("+limit+")");
            projected = limit;
        } else if (limit<projected) {
            sLog.warn("  -- Projected number of students is over course limit for course "+course.getName()+" ("+Math.round(projected)+">"+limit+")");
            projected = limit;
        }
        if (lastLike==0) {
            sLog.warn("  -- No last like info for course "+course.getName());
            return 1.0;
        }
        double weight = ((double)projected) / lastLike; 
        sLog.debug("  -- last like student weight for "+course.getName()+" is "+weight+" (lastLike="+lastLike+", projected="+projected+")");
        return weight;
    }
    
    
    /**
     * Load last-like students from an XML file (the one that is used to load last like course demands table 
     * in the timetabling application) 
     */
    public static void loadLastLikeCourseDemandsXml(StudentSectioningModel model, File xml) {
        try {
            Document document = (new SAXReader()).read(xml);
            Element root = document.getRootElement();
            Hashtable requests = new Hashtable();
            long reqId = 0;
            for (Iterator i=root.elementIterator("student");i.hasNext();) {
                Element studentEl = (Element)i.next();
                Student student = new Student(Long.parseLong(studentEl.attributeValue("externalId")));
                student.setDummy(true);
                int priority = 0;
                for (Iterator j=studentEl.elementIterator("studentCourse");j.hasNext();) {
                    Element courseEl = (Element)j.next();
                    String subjectArea = courseEl.attributeValue("subject");
                    String courseNbr = courseEl.attributeValue("courseNumber");
                    Course course = null;
                    for (Enumeration e=model.getOfferings().elements();course==null && e.hasMoreElements();) {
                        Offering offering = (Offering)e.nextElement();
                        for (Enumeration f=offering.getCourses().elements();course==null && f.hasMoreElements();) {
                            Course c = (Course)f.nextElement();
                            if (c.getSubjectArea().equals(subjectArea) && c.getCourseNumber().equals(courseNbr))
                                course = c;
                        }
                    }
                    if (course==null && courseNbr.charAt(courseNbr.length()-1)>='A' && courseNbr.charAt(courseNbr.length()-1)<='Z') {
                        String courseNbrNoSfx = courseNbr.substring(0, courseNbr.length()-1);
                        for (Enumeration e=model.getOfferings().elements();course==null && e.hasMoreElements();) {
                            Offering offering = (Offering)e.nextElement();
                            for (Enumeration f=offering.getCourses().elements();course==null && f.hasMoreElements();) {
                                Course c = (Course)f.nextElement();
                                if (c.getSubjectArea().equals(subjectArea) && c.getCourseNumber().equals(courseNbrNoSfx))
                                    course = c;
                            }
                        }
                    }
                    if (course==null) {
                        sLog.warn("Course "+subjectArea+" "+courseNbr+" not found.");
                    } else {
                        Vector courses = new Vector(1);
                        courses.add(course);
                        CourseRequest request = new CourseRequest(reqId++, priority++, false, student, courses, false);
                        Vector requestsThisCourse = (Vector)requests.get(course);
                        if (requestsThisCourse==null) {
                            requestsThisCourse = new Vector();
                            requests.put(course, requestsThisCourse);
                        }
                        requestsThisCourse.add(request);
                    }
                }
                if (!student.getRequests().isEmpty())
                    model.addStudent(student);
            }
            for (Iterator i=requests.entrySet().iterator();i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                Course course = (Course)entry.getKey();
                Vector requestsThisCourse = (Vector)entry.getValue();
                double weight = getLastLikeStudentWeight(course, requestsThisCourse.size());
                for (Enumeration e=requestsThisCourse.elements();e.hasMoreElements();) {
                    CourseRequest request = (CourseRequest)e.nextElement();
                    request.setWeight(weight);
                }
            }
        } catch (Exception e) {
            sLog.error(e.getMessage(),e);
        }
    }
    
    /**
     * Load course request from the given files (in the format being used by the old MSF system) 
     * @param model student sectioning model (with offerings loaded)
     * @param files semi-colon separated list of files to be loaded
     */
    public static void loadCrsReqFiles(StudentSectioningModel model, String files) {
        try {
            boolean lastLike = model.getProperties().getPropertyBoolean("Test.CrsReqIsLastLike", true);
            boolean shuffleIds = model.getProperties().getPropertyBoolean("Test.CrsReqShuffleStudentIds", true);
            boolean tryWithoutSuffix = model.getProperties().getPropertyBoolean("Test.CrsReqTryWithoutSuffix", false);
            Hashtable students = new Hashtable();
            long reqId = 0;
            for (StringTokenizer stk=new StringTokenizer(files,";");stk.hasMoreTokens();) {
                String file = stk.nextToken();
                sLog.debug("Loading "+file+" ...");
                BufferedReader in = new BufferedReader(new FileReader(file));
                String line; int lineIndex=0;
                while ((line=in.readLine())!=null) {
                    lineIndex++;
                    if (line.length()<=150) continue;
                    char code = line.charAt(13);
                    if (code=='H' || code=='T') continue; //skip header and tail
                    long studentId = Long.parseLong(line.substring(14,23));
                    Student student = (Student)students.get(new Long(studentId));
                    if (student==null) {
                        student = new Student(studentId);
                        if (lastLike) student.setDummy(true);
                        students.put(new Long(studentId), student);
                        sLog.debug("  -- loading student "+studentId+" ...");
                    } else
                        sLog.debug("  -- updating student "+studentId+" ...");
                    line = line.substring(150);
                    while (line.length()>=20) {
                        String subjectArea = line.substring(0,4).trim();
                        String courseNbr = line.substring(4,8).trim();
                        if (subjectArea.length()==0 || courseNbr.length()==0) {
                            line = line.substring(20);
                            continue;
                        }
                        String instrSel = line.substring(8,10); //ZZ - Remove previous instructor selection
                        char reqPDiv = line.charAt(10); //P - Personal preference; C - Conflict resolution; 
                                                        //0 - (Zero) used by program only, for change requests to reschedule division
                                                        //    (used to reschedule cancelled division)
                        String reqDiv = line.substring(11,13); //00 - Reschedule division
                        String reqSect = line.substring(13,15); //Contains designator for designator-required courses
                        String credit = line.substring(15,19);
                        char nameRaise = line.charAt(19); //N - Name raise
                        char action =line.charAt(19); //A - Add; D - Drop; C - Change 
                        sLog.debug("    -- requesting "+subjectArea+" "+courseNbr+" (action:"+action+") ...");
                        Course course = null;
                        for (Enumeration e=model.getOfferings().elements();course==null && e.hasMoreElements();) {
                            Offering offering = (Offering)e.nextElement();
                            for (Enumeration f=offering.getCourses().elements();course==null && f.hasMoreElements();) {
                                Course c = (Course)f.nextElement();
                                if (c.getSubjectArea().equals(subjectArea) && c.getCourseNumber().equals(courseNbr))
                                    course = c;;
                            }
                        }
                        if (course==null && tryWithoutSuffix && courseNbr.charAt(courseNbr.length()-1)>='A' && courseNbr.charAt(courseNbr.length()-1)<='Z') {
                            String courseNbrNoSfx = courseNbr.substring(0, courseNbr.length()-1);
                            for (Enumeration e=model.getOfferings().elements();course==null && e.hasMoreElements();) {
                                Offering offering = (Offering)e.nextElement();
                                for (Enumeration f=offering.getCourses().elements();course==null && f.hasMoreElements();) {
                                    Course c = (Course)f.nextElement();
                                    if (c.getSubjectArea().equals(subjectArea) && c.getCourseNumber().equals(courseNbrNoSfx))
                                        course = c;
                                }
                            }
                        }
                        if (course==null) {
                            if (courseNbr.charAt(courseNbr.length()-1)>='A' && courseNbr.charAt(courseNbr.length()-1)<='Z') {
                            } else {
                                sLog.warn("      -- course "+subjectArea+" "+courseNbr+" not found (file "+file+", line "+lineIndex+")");
                            }
                        } else {
                            CourseRequest courseRequest = null;
                            for (Enumeration e=student.getRequests().elements();courseRequest==null && e.hasMoreElements();) {
                                Request request = (Request)e.nextElement();
                                if (request instanceof CourseRequest && ((CourseRequest)request).getCourses().contains(course))
                                    courseRequest = (CourseRequest)request;
                            }
                            if (action=='A') {
                                if (courseRequest==null) {
                                    Vector courses = new Vector(1); courses.add(course);
                                    courseRequest = new CourseRequest(reqId++, student.getRequests().size(), false, student, courses, false);
                                } else {
                                    sLog.warn("      -- request for course "+course+" is already present"); 
                                }
                            } else if (action=='D') {
                                if (courseRequest==null) {
                                    sLog.warn("      -- request for course "+course+" is not present -- cannot be dropped");
                                } else {
                                    student.getRequests().remove(courseRequest);
                                }
                            } else if (action=='C') {
                                if (courseRequest==null) {
                                    sLog.warn("      -- request for course "+course+" is not present -- cannot be changed");
                                } else {
                                    //?
                                }
                            } else {
                                sLog.warn("      -- unknown action "+action);
                            }
                        }
                        line = line.substring(20);
                    }
                }
                in.close();
            }
            Hashtable requests = new Hashtable();
            HashSet studentIds = new HashSet();
            for (Enumeration e=students.elements();e.hasMoreElements();) {
                Student student = (Student)e.nextElement();
                if (!student.getRequests().isEmpty())
                    model.addStudent(student);
                if (shuffleIds) {
                    long newId = -1;
                    while (true) {
                        newId = 1+(long)(999999999L * Math.random());
                        if (studentIds.add(new Long(newId))) break;
                    }
                    student.setId(newId);
                }
                if (student.isDummy()) {
                    for (Enumeration f=student.getRequests().elements();f.hasMoreElements();) {
                        Request request = (Request)f.nextElement();
                        if (request instanceof CourseRequest) {
                            Course course = (Course)((CourseRequest)request).getCourses().firstElement();
                            Vector requestsThisCourse = (Vector)requests.get(course);
                            if (requestsThisCourse==null) {
                                requestsThisCourse = new Vector();
                                requests.put(course, requestsThisCourse);
                            }
                            requestsThisCourse.add(request);
                        }
                    }
                }
            }
            Collections.sort(model.getStudents(), new Comparator() {
                public int compare(Object o1, Object o2) {
                    return Double.compare(((Student)o1).getId(),((Student)o2).getId());
                }
            });
            for (Iterator i=requests.entrySet().iterator();i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                Course course = (Course)entry.getKey();
                Vector requestsThisCourse = (Vector)entry.getValue();
                double weight = getLastLikeStudentWeight(course, requestsThisCourse.size());
                for (Enumeration e=requestsThisCourse.elements();e.hasMoreElements();) {
                    CourseRequest request = (CourseRequest)e.nextElement();
                    request.setWeight(weight);
                }
            }
            if (model.getProperties().getProperty("Test.EtrChk")!=null) {
                for (StringTokenizer stk=new StringTokenizer(model.getProperties().getProperty("Test.EtrChk"),";");stk.hasMoreTokens();) {
                    String file = stk.nextToken();
                    sLog.debug("Loading "+file+" ...");
                    BufferedReader in = new BufferedReader(new FileReader(file));
                    String line; int lineIndex=0;
                    while ((line=in.readLine())!=null) {
                        lineIndex++;
                        if (line.length()<55) continue;
                        char code = line.charAt(12);
                        if (code=='H' || code=='T') continue; //skip header and tail
                        if (code=='D' || code=='K') continue; //skip delete nad cancel 
                        long studentId = Long.parseLong(line.substring(2,11));
                        Student student = (Student)students.get(new Long(studentId));
                        if (student==null) {
                            sLog.info("  -- student "+studentId+" not found");
                            continue;
                        } 
                        sLog.info("  -- reading student "+studentId);
                        String area = line.substring(15,18).trim();
                        if (area.length()==0) continue;
                        String clasf = line.substring(18,20).trim();
                        String major = line.substring(21, 24).trim();
                        String minor = line.substring(24, 27).trim();
                        student.getAcademicAreaClasiffications().clear(); student.getMajors().clear(); student.getMinors().clear();
                        student.getAcademicAreaClasiffications().add(new AcademicAreaCode(area, clasf));
                        if (major.length()>0) student.getMajors().add(new AcademicAreaCode(area, major));
                        if (minor.length()>0) student.getMinors().add(new AcademicAreaCode(area, minor));
                    }
                }
            }
            int without = 0;
            for (Enumeration e=students.elements();e.hasMoreElements();) {
                Student student = (Student)e.nextElement();
                if (student.getAcademicAreaClasiffications().isEmpty()) without++;
            }
            sLog.info("Students without academic area: "+without);
        } catch (Exception e) {
            sLog.error(e.getMessage(),e);
        }
    }
    
    /** Load student infos from a given XML file. */
    public static void loadStudentInfoXml(StudentSectioningModel model, File xml) {
        try {
            sLog.info("Loading student infos from "+xml);
            Document document = (new SAXReader()).read(xml);
            Element root = document.getRootElement();
            Hashtable requests = new Hashtable();
            long reqId = 0;
            Hashtable studentTable = new Hashtable();
            for (Enumeration e=model.getStudents().elements();e.hasMoreElements();) {
                Student student = (Student)e.nextElement();
                studentTable.put(new Long(student.getId()), student);
            }
            for (Iterator i=root.elementIterator("student");i.hasNext();) {
                Element studentEl = (Element)i.next();
                Student student = (Student)studentTable.get(Long.valueOf(studentEl.attributeValue("externalId")));
                if (student==null) {
                    sLog.debug(" -- student "+studentEl.attributeValue("externalId")+" not found");
                    continue;
                }
                sLog.debug(" -- loading info for student "+student);
                student.getAcademicAreaClasiffications().clear();
                if (studentEl.element("studentAcadAreaClass")!=null)
                    for (Iterator j=studentEl.element("studentAcadAreaClass").elementIterator("acadAreaClass");j.hasNext();) {
                        Element studentAcadAreaClassElement = (Element)j.next();
                        student.getAcademicAreaClasiffications().add(
                            new AcademicAreaCode(
                                    studentAcadAreaClassElement.attributeValue("academicArea"),
                                    studentAcadAreaClassElement.attributeValue("academicClass"))
                            );
                    }
                sLog.debug("   -- acad areas classifs "+student.getAcademicAreaClasiffications());
                student.getMajors().clear();
                if (studentEl.element("studentMajors")!=null)
                    for (Iterator j=studentEl.element("studentMajors").elementIterator("major");j.hasNext();) {
                        Element studentMajorElement = (Element)j.next();
                        student.getMajors().add(
                            new AcademicAreaCode(
                                    studentMajorElement.attributeValue("academicArea"),
                                    studentMajorElement.attributeValue("code"))
                            );
                    }
                sLog.debug("   -- majors "+student.getMajors());
                student.getMinors().clear();
                if (studentEl.element("studentMinors")!=null)
                    for (Iterator j=studentEl.element("studentMinors").elementIterator("minor");j.hasNext();) {
                        Element studentMinorElement = (Element)j.next();
                        student.getMinors().add(
                            new AcademicAreaCode(
                                    studentMinorElement.attributeValue("academicArea",""),
                                    studentMinorElement.attributeValue("code",""))
                            );
                    }
                sLog.debug("   -- minors "+student.getMinors());
            }
        } catch (Exception e) {
            sLog.error(e.getMessage(),e);
        }
    }
    
    /** Main */
    public static void main(String[] args) {
        try {
            DataProperties cfg = new DataProperties();
            cfg.setProperty("Termination.Class","net.sf.cpsolver.ifs.termination.GeneralTerminationCondition");
            cfg.setProperty("Termination.StopWhenComplete","true");
            cfg.setProperty("Termination.TimeOut","600");
            cfg.setProperty("Comparator.Class","net.sf.cpsolver.ifs.solution.GeneralSolutionComparator");
            cfg.setProperty("Value.Class","net.sf.cpsolver.ifs.heuristics.GeneralValueSelection");
            cfg.setProperty("Value.WeightConflicts", "1.0");
            cfg.setProperty("Value.WeightNrAssignments", "0.0");
            cfg.setProperty("Variable.Class","net.sf.cpsolver.ifs.heuristics.GeneralVariableSelection");
            cfg.setProperty("Neighbour.Class","net.sf.cpsolver.studentsct.heuristics.StudentSctNeighbourSelection");
            cfg.setProperty("General.SaveBestUnassigned", "-1");
            cfg.setProperty("Extensions.Classes","net.sf.cpsolver.ifs.extension.ConflictStatistics;net.sf.cpsolver.studentsct.extension.DistanceConflict");
            cfg.setProperty("Data.Initiative","puWestLafayetteTrdtn");
            cfg.setProperty("Data.Term","Fal");
            cfg.setProperty("Data.Year","2007");
            cfg.setProperty("General.Input","pu-sectll-fal07-s.xml");
            if (args.length>=1) {
                cfg.load(new FileInputStream(args[0]));
            }
            cfg.putAll(System.getProperties());
            
            if (args.length>=2) {
                cfg.setProperty("General.Input", args[1]);
            }

            if (args.length>=3) {
                File logFile = new File(ToolBox.configureLogging(args[2]+File.separator+(sDateFormat.format(new Date())), null, false, false));
                cfg.setProperty("General.Output", logFile.getParentFile().getAbsolutePath());
            } else if (cfg.getProperty("General.Output")!=null) {
                cfg.setProperty("General.Output", cfg.getProperty("General.Output",".")+File.separator+(sDateFormat.format(new Date())));
                File logFile = new File(ToolBox.configureLogging(cfg.getProperty("General.Output","."), null, false, false));
            } else {
                ToolBox.configureLogging();
                cfg.setProperty("General.Output", System.getProperty("user.home", ".")+File.separator+"Sectioning-Test"+File.separator+(sDateFormat.format(new Date())));
            }
            
            if (args.length>=4 && "online".equals(args[3])) {
                onlineSectioning(cfg);
            } else if (args.length>=4 && "simple".equals(args[3])) {
                cfg.setProperty("Sectioning.UseOnlinePenalties", "false");
                onlineSectioning(cfg);
            } else {
                batchSectioning(cfg);
            }
        } catch (Exception e) {
            sLog.error(e.getMessage(),e);
            e.printStackTrace();
        }
    }
}
