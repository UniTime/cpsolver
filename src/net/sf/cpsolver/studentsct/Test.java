package net.sf.cpsolver.studentsct;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solution.SolutionListener;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.solver.SolverListener;
import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.constraint.SectionLimit;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;

public class Test {
    private static org.apache.log4j.Logger sLog = org.apache.log4j.Logger.getLogger(Test.class);
    private static DecimalFormat sDF = new DecimalFormat("0.000");

    public static void batchSectioning(DataProperties cfg) {
        StudentSectioningModel model = new StudentSectioningModel(cfg);
        try {
            new StudentSectioningXMLLoader(model).load();
        } catch (Exception e) {
            sLog.error("Unable to load model, reason: "+e.getMessage(), e);
            return;
        }
        
        Solution solution = solveModel(model, cfg);
        
        try {
            File outDir = new File(cfg.getProperty("General.Output","."));
            outDir.mkdirs();
            createCourseConflictTable((StudentSectioningModel)solution.getModel(), true, false).save(new File(outDir, "conflicts-lastlike.csv"));
            createCourseConflictTable((StudentSectioningModel)solution.getModel(), false, true).save(new File(outDir, "conflicts-real.csv"));
        } catch (IOException e) {
            sLog.error(e.getMessage(),e);
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
                sLog.debug("**BEST** V:"+m.assignedVariables().size()+"/"+m.variables().size()+" - S:"+m.nrComplete()+"/"+m.getStudents().size()+" - TV:"+sDF.format(m.getTotalValue()));
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
        
        try {
            new StudentSectioningXMLSaver(solver).save(new File(new File(cfg.getProperty("General.Output",".")),"solution.xml"));
        } catch (Exception e) {
            sLog.error("Unable to save solution, reason: "+e.getMessage(),e);
        }
        
        sLog.info("Best solution found after "+solution.getBestTime()+" seconds ("+solution.getBestIteration()+" iterations).");
        sLog.info("Number of assigned variables is "+solution.getModel().assignedVariables().size());
        sLog.info("Number of students with complete schedule is "+((StudentSectioningModel)solution.getModel()).nrComplete());
        sLog.info("Total value of the solution is "+solution.getModel().getTotalValue());
        sLog.info("Info: "+solution.getInfo());

        return solution;
    }
    
    public static CSVFile createCourseConflictTable(StudentSectioningModel model, boolean includeLastLikeStudents, boolean includeRealStudents) {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVFile.CSVField[] {
                new CSVFile.CSVField("UNASSIGNED"),
                new CSVFile.CSVField("CONFLICT"),
                new CSVFile.CSVField("NR_STUDENTS")
        });
        Hashtable unassignedCourseTable = new Hashtable();
        for (Enumeration e=model.unassignedVariables().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (request.getStudent().isDummy() && !includeLastLikeStudents) continue;
            if (!request.getStudent().isDummy() && !includeRealStudents) continue;
            if (request instanceof CourseRequest) {
                CourseRequest courseRequest = (CourseRequest)request;
                if (courseRequest.getStudent().isComplete()) continue;
                
                Vector values = courseRequest.values();
                SectionLimit limitConstraint = new SectionLimit();
                Vector availableValues = new Vector(values.size());
                for (Enumeration f=values.elements();f.hasMoreElements();) {
                    Enrollment enrollment = (Enrollment)f.nextElement();
                    if (!limitConstraint.inConflict(enrollment))
                        availableValues.addElement(enrollment);
                }
                
                if (availableValues.isEmpty()) {
                    Course course = (Course)courseRequest.getCourses().firstElement();
                    Hashtable conflictCourseTable = (Hashtable)unassignedCourseTable.get(course);
                    if (conflictCourseTable==null) {
                        conflictCourseTable = new Hashtable();
                        unassignedCourseTable.put(course, conflictCourseTable);
                    }
                    Double weight = (Double)conflictCourseTable.get(course);
                    conflictCourseTable.put(course, new Double((weight==null?0.0:weight.doubleValue())+courseRequest.getWeight()));
                }
                
                for (Enumeration f=availableValues.elements();f.hasMoreElements();) {
                    Enrollment enrollment = (Enrollment)f.nextElement();
                    Set conflicts = model.conflictValues(enrollment);
                    if (conflicts.isEmpty()) {
                        sLog.warn("Request "+courseRequest+" of student "+courseRequest.getStudent()+" not assigned, however, no conflicts were returned.");
                        courseRequest.assign(0, enrollment);
                        break;
                    }
                    Course course = null;
                    for (Enumeration g=courseRequest.getCourses().elements();g.hasMoreElements();) {
                        Course c = (Course)g.nextElement();
                        if (c.getOffering().equals(enrollment.getConfig().getOffering())) {
                            course = c; break;
                        }
                    }
                    if (course==null) {
                        sLog.warn("Course not found for request "+courseRequest+" of student "+courseRequest.getStudent()+".");
                        continue;
                    }
                    Hashtable conflictCourseTable = (Hashtable)unassignedCourseTable.get(course);
                    if (conflictCourseTable==null) {
                        conflictCourseTable = new Hashtable();
                        unassignedCourseTable.put(course, conflictCourseTable);
                    }
                    for (Iterator i=conflicts.iterator();i.hasNext();) {
                        Enrollment conflict = (Enrollment)i.next();
                        if (conflict.variable() instanceof CourseRequest) {
                            CourseRequest conflictCourseRequest = (CourseRequest)conflict.variable();
                            Course conflictCourse = null;
                            for (Enumeration g=conflictCourseRequest.getCourses().elements();g.hasMoreElements();) {
                                Course c = (Course)g.nextElement();
                                if (c.getOffering().equals(conflict.getConfig().getOffering())) {
                                    conflictCourse = c; break;
                                }
                            }
                            if (conflictCourse==null) {
                                sLog.warn("Course not found for request "+conflictCourseRequest+" of student "+conflictCourseRequest.getStudent()+".");
                                continue;
                            }
                            Double weight = (Double)conflictCourseTable.get(conflictCourse);
                            double weightThisConflict = courseRequest.getWeight() / availableValues.size() / conflicts.size();
                            conflictCourseTable.put(conflictCourse, new Double((weight==null?0.0:weight.doubleValue())+weightThisConflict));
                        }
                    }
                }
            }
        }
        for (Iterator i=unassignedCourseTable.entrySet().iterator();i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            Course unassignedCourse = (Course)entry.getKey();
            Hashtable conflictCourseTable = (Hashtable)entry.getValue();
            for (Iterator j=conflictCourseTable.entrySet().iterator();j.hasNext();) {
                Map.Entry entry2 = (Map.Entry)j.next();
                Course conflictCourse = (Course)entry2.getKey();
                double weight = ((Double)entry2.getValue()).doubleValue();
                csv.addLine(new CSVFile.CSVField[] {
                   new CSVFile.CSVField(unassignedCourse.getName()),
                   new CSVFile.CSVField(conflictCourse.getName()),
                   new CSVFile.CSVField(sDF.format(weight)),
                });
             }
        }
        return csv;
    }
    
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
            cfg.setProperty("Extensions.Classes","net.sf.cpsolver.ifs.extension.ConflictStatistics");
            cfg.setProperty("Data.Initiative","puWestLafayetteTrdtn");
            cfg.setProperty("Data.Term","2007");
            cfg.setProperty("Data.Year","Fal");
            cfg.setProperty("General.Input","c:\\test\\fal07ll.xml");
            if (args.length>=1) {
                cfg.load(new FileInputStream(args[0]));
            }
            
            if (args.length>=2) {
                cfg.setProperty("General.Input", args[1]);
            }

            if (args.length>=3) {
                File logFile = new File(ToolBox.configureLogging(args[2], null, true, false));
                cfg.setProperty("General.Output", logFile.getParentFile().getAbsolutePath());
            } else {
                ToolBox.configureLogging();
                cfg.setProperty("General.Output", System.getProperty("user.home", ".")+File.separator+"Sectioning-Test");
            }
            
            batchSectioning(cfg);
        } catch (Exception e) {
            sLog.error(e.getMessage(),e);
            e.printStackTrace();
        }
    }
}
