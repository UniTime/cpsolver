package net.sf.cpsolver.studentsct.heuristics;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
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
    
    public StudentSctNeighbourSelection(DataProperties properties) {
    }
    
    public void init(Solver solver) {
        iStudents = new Vector(((StudentSectioningModel)solver.currentSolution().getModel()).getStudents());
        iStudentsEnumeration = iStudents.elements();
        iPhase = 1;
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
                    if (student.isComplete()) continue;
                    SwapStudentsEnrollmentSelection.Selection selection = new SwapStudentsEnrollmentSelection.Selection(student);
                    if (selection.select()!=null) {
                        iStudent = student;
                        return new N2(selection);
                    } else {
                        if (selection.getProblemStudents()!=null)
                            iProblemStudents.addAll(selection.getProblemStudents());
                    }
                }
                iPhase++;
                iStudentsEnumeration = new Vector(iProblemStudents).elements();
            }
            
            //Phase 3: unassignment of all problematic students
            if (iPhase==3) {
                while (iStudentsEnumeration.hasMoreElements()) {
                    Student student = (Student)iStudentsEnumeration.nextElement();
                    return new N3(student);
                }
                iPhase++;
                Collections.shuffle(iStudents);
                iStudentsEnumeration = iStudents.elements();
            }
            
            //Phase 4: resection incomplete students 
            if (iPhase==4) {
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
            }
            
            //Phase 5: random unassignment of some students
            if (iPhase==5) {
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
}
