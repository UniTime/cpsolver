package net.sf.cpsolver.studentsct;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
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
import net.sf.cpsolver.studentsct.heuristics.general.BacktrackNeighbourSelection;
import net.sf.cpsolver.studentsct.heuristics.selection.BranchBoundSelection;
import net.sf.cpsolver.studentsct.heuristics.selection.SwapStudentSelection;
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

public class Test {
    private static org.apache.log4j.Logger sLog = org.apache.log4j.Logger.getLogger(Test.class);
    private static java.text.SimpleDateFormat sDateFormat = new java.text.SimpleDateFormat("yyMMdd_HHmmss",java.util.Locale.US);
    private static DecimalFormat sDF = new DecimalFormat("0.000");
    
    public static StudentSectioningModel loadModel(DataProperties cfg) {
        StudentSectioningModel model = new StudentSectioningModel(cfg);
        try {
            new StudentSectioningXMLLoader(model).load();
            if (cfg.getProperty("Test.LastLikeCourseDemands")!=null)
                loadLastLikeCourseDemandsXml(model, new File(cfg.getProperty("Test.LastLikeCourseDemands")));
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

    public static Solution batchSectioning(DataProperties cfg) {
        StudentSectioningModel model = loadModel(cfg);
        if (model==null) return null;
        
        model.clearOnlineSectioningInfos();
        
        Solution solution = solveModel(model, cfg);
        
        if (solution.getModel().assignedVariables().size()>0) {
            try {
                File outDir = new File(cfg.getProperty("General.Output","."));
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
        
        ((StudentSectioningModel)solution.getModel()).computeOnlineSectioningInfos();
        
        new OverlapCheck((StudentSectioningModel)solution.getModel()).check();
        
        new SectionLimitCheck((StudentSectioningModel)solution.getModel()).check();
        
        try {
            Solver solver = new Solver(cfg);
            solver.setInitalSolution(solution);
            new StudentSectioningXMLSaver(solver).save(new File(new File(cfg.getProperty("General.Output",".")),"solution.xml"));
        } catch (Exception e) {
            sLog.error("Unable to save solution, reason: "+e.getMessage(),e);
        }
        
        return solution;
    }

    public static Solution onlineSectioning(DataProperties cfg) {
        StudentSectioningModel model = loadModel(cfg);
        if (model==null) return null;
        
        boolean usePenalties = cfg.getPropertyBoolean("Sectioning.UseOnlinePenalties", true);
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
        /*
        model.addModelListener(new ModelListener() {
            public void variableAdded(Variable variable) {}
            public void variableRemoved(Variable variable) {}
            public void constraintAdded(Constraint constraint) {}
            public void constraintRemoved(Constraint constraint) {}
            public void beforeAssigned(long iteration, Value value) {
                //sLog.debug("BEFORE_ASSIGN["+iteration+"] "+value);
                Enrollment enrollment = (Enrollment)value;
                StudentSectioningModel m = (StudentSectioningModel)enrollment.getRequest().getModel();
                if (m.getDistanceConstraint()!=null) {
                    m.getDistanceConstraint().setDebug(true);
                    m.getDistanceConstraint().nrAllConflicts(enrollment);
                    m.getDistanceConstraint().setDebug(false);
                }
            }
            public void beforeUnassigned(long iteration, Value value) {
                //sLog.debug("BEFORE_UNASSIGN["+iteration+"] "+value);
                Enrollment enrollment = (Enrollment)value;
                StudentSectioningModel m = (StudentSectioningModel)enrollment.getRequest().getModel();
                if (m.getDistanceConstraint()!=null) {
                    m.getDistanceConstraint().setDebug(true);
                    m.getDistanceConstraint().nrAllConflicts(enrollment);
                    m.getDistanceConstraint().setDebug(false);
                }
            }
            public void afterAssigned(long iteration, Value value) {
                sLog.debug("AFTER_ASSIGN["+iteration+"] "+value);
            }
            public void afterUnassigned(long iteration, Value value) {
                sLog.debug("AFTER_UNASSIGN["+iteration+"] "+value);
            }
            public boolean init(Solver solver) {
                return true;
            }
        });
        */
        double startTime = JProf.currentTimeSec();
        
        
        Solver solver = new Solver(cfg);
        solver.setInitalSolution(solution);
        solver.initSolver();

        BranchBoundSelection bbSelection = new BranchBoundSelection(cfg);
        bbSelection.init(solver);

        Vector students = new Vector(model.getStudents());
        Collections.shuffle(students);
        for (Enumeration e=students.elements();e.hasMoreElements();) {
            Student student = (Student)e.nextElement();
            sLog.info("Sectioning student: "+student);
            if (usePenalties) setPenalties(student);
            Neighbour neighbour = bbSelection.getSelection(student).select();
            if (neighbour!=null) {
                neighbour.assign(solution.getIteration());
                sLog.info("Solution: "+neighbour);
                if (usePenalties) updateSpace(student);
            } else {
                sLog.warn("No solution found.");
            }
            solution.update(JProf.currentTimeSec()-startTime);
            solution.saveBest();
        }
        
        try {
            File outDir = new File(cfg.getProperty("General.Output","."));
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
        
        solution.saveBest();

        new OverlapCheck((StudentSectioningModel)solution.getModel()).check();
        
        new SectionLimitCheck((StudentSectioningModel)solution.getModel()).check();

        try {
            new StudentSectioningXMLSaver(solver).save(new File(new File(cfg.getProperty("General.Output",".")),"solution.xml"));
        } catch (Exception e) {
            sLog.error("Unable to save solution, reason: "+e.getMessage(),e);
        }
        
        sLog.info("Best solution found after "+solution.getBestTime()+" seconds ("+solution.getBestIteration()+" iterations).");
        sLog.info("Number of assigned variables is "+solution.getModel().assignedVariables().size());
        sLog.info("Number of students with complete schedule is "+((StudentSectioningModel)solution.getModel()).nrComplete());
        sLog.info("Total value of the solution is "+solution.getModel().getTotalValue());
        sLog.info("Average unassigned priority "+sDF.format(((StudentSectioningModel)solution.getModel()).avgUnassignPriority()));
        sLog.info("Average number of requests "+sDF.format(((StudentSectioningModel)solution.getModel()).avgNrRequests()));
        sLog.info("Unassigned request weight "+sDF.format(((StudentSectioningModel)solution.getModel()).getUnassignedRequestWeight())+" / "+sDF.format(((StudentSectioningModel)solution.getModel()).getTotalRequestWeight()));
        sLog.info("Info: "+solution.getInfo());

        return solution;
    }
    
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
        }
    }

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
        /*
        solution.getModel().addModelListener(new ModelListener() {
            public void variableAdded(Variable variable) {}
            public void variableRemoved(Variable variable) {}
            public void constraintAdded(Constraint constraint) {}
            public void constraintRemoved(Constraint constraint) {}
            public void beforeAssigned(long iteration, Value value) {
                sLog.debug("BEFORE_ASSIGN["+iteration+"] "+value);
            }
            public void beforeUnassigned(long iteration, Value value) {
                sLog.debug("BEFORE_UNASSIGN["+iteration+"] "+value);
            }
            public void afterAssigned(long iteration, Value value) {
                sLog.debug("AFTER_ASSIGN["+iteration+"] "+value);
            }
            public void afterUnassigned(long iteration, Value value) {
                sLog.debug("AFTER_UNASSIGN["+iteration+"] "+value);
            }
            public boolean init(Solver solver) {
                return true;
            }
        });
        */
        
        solver.start();
        try {
            solver.getSolverThread().join();
        } catch (InterruptedException e) {}
        
        solution = solver.lastSolution();
        solution.restoreBest();
        
        sLog.info("Best solution found after "+solution.getBestTime()+" seconds ("+solution.getBestIteration()+" iterations).");
        sLog.info("Number of assigned variables is "+solution.getModel().assignedVariables().size());
        sLog.info("Number of students with complete schedule is "+((StudentSectioningModel)solution.getModel()).nrComplete());
        sLog.info("Total value of the solution is "+solution.getModel().getTotalValue());
        sLog.info("Average unassigned priority "+sDF.format(((StudentSectioningModel)solution.getModel()).avgUnassignPriority()));
        sLog.info("Average number of requests "+sDF.format(((StudentSectioningModel)solution.getModel()).avgNrRequests()));
        sLog.info("Unassigned request weight "+sDF.format(((StudentSectioningModel)solution.getModel()).getUnassignedRequestWeight())+" / "+sDF.format(((StudentSectioningModel)solution.getModel()).getTotalRequestWeight()));
        sLog.info("Info: "+solution.getInfo());

        return solution;
    }
    
    
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

    public static void main(String[] args) {
        try {
            /*
            if (args==null || args.length==0) {
                args = new String[] {"c:\\test\\test.cfg", "c:\\test\\pu-sectll-fal07-s.xml", "c:\\test\\log-test-online", "online"};
            }
            */
            
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
            cfg.setProperty("Data.Term","2007");
            cfg.setProperty("Data.Year","Fal");
            cfg.setProperty("General.Input","pu-sectll-fal07-s.xml");
            if (args.length>=1) {
                cfg.load(new FileInputStream(args[0]));
            }
            
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
            
            if (args.length>=4 && "online".equals(args[3]))
                onlineSectioning(cfg);
            else if (args.length>=4 && "simple".equals(args[3])) {
                cfg.setProperty("Sectioning.UseOnlinePenalties", "false");
                onlineSectioning(cfg);
            } else
                batchSectioning(cfg);
        } catch (Exception e) {
            sLog.error(e.getMessage(),e);
            e.printStackTrace();
        }
    }
}
