package net.sf.cpsolver.studentsct.heuristics;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.heuristics.ValueSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.SimpleNeighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

public class StudentSctNeighbourSelection implements NeighbourSelection {
    private Vector iStudents = null;
    private Enumeration iStudentsEnumeration = null;
    private int iPhase = 0;
    private Set iProblemStudents = new HashSet();
    private Student iStudent = null;
    private ValueSelection iValueSelection = null;
    private long iIteration = 0;
    
    public StudentSctNeighbourSelection(DataProperties properties) {
        try {
            String valueSelectionClassName = properties.getProperty("Value.Class","net.sf.cpsolver.ifs.heuristics.GeneralValueSelection");
            Class valueSelectionClass = Class.forName(valueSelectionClassName);
            Constructor valueSelectionConstructor = valueSelectionClass.getConstructor(new Class[]{DataProperties.class});
            iValueSelection = (ValueSelection)valueSelectionConstructor.newInstance(new Object[] {properties});
        } catch (Exception e) {
            new RuntimeException(e.getMessage(),e);
        }
    }
    
    public void init(Solver solver) {
        iStudents = new Vector(((StudentSectioningModel)solver.currentSolution().getModel()).getStudents());
        iStudentsEnumeration = iStudents.elements();
        iPhase = 1;
        iValueSelection.init(solver);
    }

    public Neighbour selectNeighbour(Solution solution) {
        while (true) {
            //Phase 0: not initialized
            if (iPhase==0)
                throw new RuntimeException("Neighbour selection not initialized.");
            
            //Phase 1: section all students using incremental branch & bound (no unassignments)
            if (iPhase==1) {
                while (iStudentsEnumeration.hasMoreElements()) {
                    Student student = (Student)iStudentsEnumeration.nextElement();
                    BranchBoundEnrollmentsSelection.Selection selection = new BranchBoundEnrollmentsSelection.Selection(student);
                    if (selection.select()!=null)
                        return new N1(selection);
                }
                iPhase++;
                Collections.shuffle(iStudents);
                iStudentsEnumeration = iStudents.elements();
                iProblemStudents.clear();
                iIteration = solution.getIteration();
            }
            
            //Phase 2: pick a student (one by one) with an incomplete schedule, try to find an improvement 
            if (iPhase==2) {
                if (iStudent!=null && !iStudent.isComplete()) {
                    SwapStudentsEnrollmentSelection.Selection selection = new SwapStudentsEnrollmentSelection.Selection(iStudent);
                    if (selection.select()!=null)
                        return new N2(selection);
                }
                iStudent = null;
                while (iStudentsEnumeration.hasMoreElements()) {
                    Student student = (Student)iStudentsEnumeration.nextElement();
                    if (student.isComplete() || student.nrAssignedRequests()==0) continue;
                    SwapStudentsEnrollmentSelection.Selection selection = new SwapStudentsEnrollmentSelection.Selection(student);
                    if (selection.select()!=null) {
                        iStudent = student;
                        return new N2(selection);
                    }
                }
                iPhase++;
                Collections.shuffle(iStudents);
                iStudentsEnumeration = iStudents.elements();
                iIteration = solution.getIteration();
            }
            
            
            //Phase 3: use standard value selection for some time
            if (iPhase==3) {
                if (solution.getModel().unassignedVariables().isEmpty() || solution.getIteration()>=iIteration+10*solution.getModel().countVariables()) {
                    iPhase++;
                } else {
                    for (int i=0;i<10;i++) {
                        Request request = (Request)ToolBox.random(solution.getModel().unassignedVariables());
                        Enrollment enrollment = (request==null?null:(Enrollment)iValueSelection.selectValue(solution, request));
                        if (enrollment!=null && !enrollment.variable().getModel().conflictValues(enrollment).contains(enrollment))
                            return new SimpleNeighbour(request, enrollment);
                    }
                    iPhase++;
                }
            }
            
            
            //Phase 4: pick a student (one by one) with an incomplete schedule, try to find an improvement, identify problematic students
            if (iPhase==4) {
                if (iStudent!=null && !iStudent.isComplete()) {
                    SwapStudentsEnrollmentSelection.Selection selection = new SwapStudentsEnrollmentSelection.Selection(iStudent);
                    if (selection.select()!=null)
                        return new N2(selection);
                    else 
                        iProblemStudents.addAll(selection.getProblemStudents());
                }
                iStudent = null;
                while (iStudentsEnumeration.hasMoreElements()) {
                    Student student = (Student)iStudentsEnumeration.nextElement();
                    if (student.isComplete() || student.nrAssignedRequests()==0) continue;
                    SwapStudentsEnrollmentSelection.Selection selection = new SwapStudentsEnrollmentSelection.Selection(student);
                    if (selection.select()!=null) {
                        iStudent = student;
                        return new N2(selection);
                    } else {
                        iProblemStudents.addAll(selection.getProblemStudents());
                    }
                }
                iPhase++;
                iStudentsEnumeration = new Vector(iProblemStudents).elements();
                iIteration = solution.getIteration();
            }
            
            //Phase 5: unassignment of all problematic students
            if (iPhase==5) {
                while (iStudentsEnumeration.hasMoreElements()) {
                    Student student = (Student)iStudentsEnumeration.nextElement();
                    return new N3(student);
                }
                iPhase++;
                Collections.shuffle(iStudents);
                iStudentsEnumeration = iStudents.elements();
                iIteration = solution.getIteration();
            }
            
            //Phase 6: resection incomplete students 
            if (iPhase==6) {
                while (iStudentsEnumeration.hasMoreElements()) {
                    Student student = (Student)iStudentsEnumeration.nextElement();
                    if (iProblemStudents.contains(student) || student.isComplete()) continue;
                    BranchBoundEnrollmentsSelection.Selection selection = new BranchBoundEnrollmentsSelection.Selection(student);
                    if (selection.select()!=null)
                        return new N1(selection);
                }
                iPhase++;
                Collections.shuffle(iStudents);
                iStudentsEnumeration = iStudents.elements();
                iIteration = solution.getIteration();
            }
            
            //Phase 7: use standard value selection for some time
            if (iPhase==7) {
                if (solution.getIteration()>=iIteration+10*solution.getModel().countVariables()) {
                    iPhase++;
                } else {
                    RouletteWheelSelection roulette = new RouletteWheelSelection();
                    for (Enumeration e=solution.getModel().variables().elements();e.hasMoreElements();) {
                        Request request = (Request)e.nextElement();
                        double points = 0;
                        if (request.getAssignment()==null)
                            points +=10;
                        else {
                            Enrollment enrollment = (Enrollment)request.getAssignment();
                            if (enrollment.toDouble()>request.getBound())
                                points +=1;
                        }
                        if (points>0)
                            roulette.add(request, points);
                    }
                    for (int i=0;i<10;i++) {
                        Request request = (Request)roulette.select();
                        Enrollment enrollment = (request==null?null:(Enrollment)iValueSelection.selectValue(solution, request));
                        if (enrollment!=null && !enrollment.variable().getModel().conflictValues(enrollment).contains(enrollment))
                            return new SimpleNeighbour(request, enrollment);
                    }
                    iPhase++;
                }
            }
            
            //Phase 8: random unassignment of some students
            if (iPhase==8) {
                if (Math.random()<0.5) {
                    Student student = (Student)ToolBox.random(iStudents);
                    return new N3(student);
                } else {
                    iPhase++;
                }
            }
            
            iPhase = 1;
        }
    }
    
    public static class N1 extends Neighbour {
        private BranchBoundEnrollmentsSelection.Selection iSelection = null;
        public N1(BranchBoundEnrollmentsSelection.Selection selection) {
            iSelection = selection;
        }
        
        public void assign(long iteration) {
            for (int i=0;i<iSelection.getBestAssignment().length;i++)
                if (iSelection.getBestAssignment()[i]!=null)
                    iSelection.getBestAssignment()[i].variable().assign(iteration, iSelection.getBestAssignment()[i]);
        }
        
        public String toString() {
            StringBuffer sb = new StringBuffer("N1{");
            Student student = null;
            for (int i=0;i<iSelection.getBestAssignment().length;i++) {
                if (iSelection.getBestAssignment()[i]!=null) {
                    student = iSelection.getBestAssignment()[i].getRequest().getStudent();
                    sb.append(" "+student);
                    sb.append(" ("+iSelection.getBestValue()+")");
                    break;
                }
            }
            if (student!=null) {
                int idx=0;
                for (Enumeration e=student.getRequests().elements();e.hasMoreElements();idx++) {
                    Request request = (Request)e.nextElement();
                    sb.append("\n"+request);
                    Enrollment enrollment = iSelection.getBestAssignment()[idx];
                    if (enrollment==null)
                        sb.append("  -- not assigned");
                    else
                        sb.append("  -- "+enrollment.getName());
                }
            }
            sb.append("\n}");
            return sb.toString();
        }
        
    }

    public static class N2 extends Neighbour {
        private SwapStudentsEnrollmentSelection.Selection iSelection = null;
        public N2(SwapStudentsEnrollmentSelection.Selection selection) {
            iSelection = selection;
        }
        
        public void assign(long iteration) {
            Enrollment enrollment = iSelection.getBestEnrollment();
            if (enrollment.variable().getAssignment()!=null)
                enrollment.variable().unassign(iteration);
            Set conflicts = enrollment.getStudent().getModel().conflictValues(enrollment);
            for (Iterator i=conflicts.iterator();i.hasNext();) {
                Enrollment conflict = (Enrollment)i.next();
                Enrollment switchEnrl = conflict.bestSwap(enrollment, null);
                conflict.variable().unassign(iteration);
                if (switchEnrl!=null)
                    switchEnrl.variable().assign(iteration, switchEnrl);
            }
            enrollment.variable().assign(iteration, enrollment);
        }
        
        public String toString() {
            StringBuffer sb = new StringBuffer("N2{");
            sb.append(" "+iSelection.getBestEnrollment().getRequest().getStudent());
            sb.append(" ("+iSelection.getBestValue()+")");
            sb.append("\n "+iSelection.getBestEnrollment().getRequest().getName());
            sb.append(" "+iSelection.getBestEnrollment().getName());
            sb.append("\n}");
            return sb.toString();
        }
    }

    public static class N3 extends Neighbour {
        private Student iStudent = null;
        public N3(Student student) {
            iStudent = student;
        }
        
        public void assign(long iteration) {
            for (Enumeration e=iStudent.getRequests().elements();e.hasMoreElements();) {
                Request request = (Request)e.nextElement();
                if (request.getAssignment()!=null)
                    request.unassign(iteration);
            }
        }
        
        public String toString() {
            StringBuffer sb = new StringBuffer("N3{");
            sb.append(" "+iStudent);
            sb.append(" }");
            return sb.toString();
        }
        
    }
    
    public static class RouletteWheelSelection {
        private Vector iAdepts = new Vector();
        private Vector iPoints = new Vector();
        private double iTotalPoints = 0;
        public void add(Object adept, double points) {
            iAdepts.add(adept); iPoints.add(new Double(points)); iTotalPoints+=points;
        }
        public Object select() {
            if (iAdepts.isEmpty()) return null;
            double rx = ToolBox.random()*iTotalPoints;
            int idx = 0;
            for (Enumeration e=iPoints.elements();e.hasMoreElements();idx++) {
                rx -= ((Double)e.nextElement()).doubleValue();
                if (rx<0) return iAdepts.elementAt(idx);
            }
            return iAdepts.lastElement();
        }
    }
}
