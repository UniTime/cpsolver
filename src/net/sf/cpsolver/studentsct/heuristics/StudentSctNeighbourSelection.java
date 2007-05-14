package net.sf.cpsolver.studentsct.heuristics;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.ifs.heuristics.StandardNeighbourSelection;
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

public class StudentSctNeighbourSelection extends StandardNeighbourSelection {
    private int iSelectionIdx = -1;
    private Vector iSelections = new Vector();
    private RandomizedBacktrackNeighbourSelection iRBtNSel = null;
    private BranchBoundEnrollmentsSelection iBBEnSel = null;
    private SwapStudentsEnrollmentSelection iSwStEnSel = null;
    private HashSet iProblemStudents = new HashSet();
    
    public StudentSctNeighbourSelection(DataProperties properties) throws Exception {
        super(properties);
        iRBtNSel = new RandomizedBacktrackNeighbourSelection(properties);
        iBBEnSel = new BranchBoundEnrollmentsSelection(properties);
        iSwStEnSel = new SwapStudentsEnrollmentSelection(properties);
    }
    
    public void registerSelection(Selection selection) {
        iSelections.add(selection);
    }
    
    public void init(Solver solver) {
        super.init(solver);
        iRBtNSel.init(solver);
        iBBEnSel.init(solver);
        iSwStEnSel.init(solver);
        setup();
    }

    public Neighbour selectNeighbour(Solution solution) {
        if (iSelectionIdx==-1) {
            iSelectionIdx = 0;
            ((Selection)iSelections.elementAt(iSelectionIdx)).init(solution);
        }
        while (true) {
            Selection selection = (Selection)iSelections.elementAt(iSelectionIdx);
            Neighbour neighbour = selection.select(solution);
            if (neighbour!=null) return neighbour;
            iSelectionIdx = (1+iSelectionIdx) % iSelections.size();
            sLogger.debug("Phase changed to "+(iSelectionIdx+1));
            ((Selection)iSelections.elementAt(iSelectionIdx)).init(solution);
        }
    }
    
    public void setup() {
        //Phase 1: section all students using incremental branch & bound (no unassignments)
        registerSelection(new Selection() {
            private Enumeration iStudentsEnumeration = null;
            private BranchBoundEnrollmentsSelection iBranchBoundEnrollmentsSelection = null;
            public void init(Solution solution) {
                Vector students = new Vector(((StudentSectioningModel)solution.getModel()).getStudents());
                Collections.shuffle(students);
                iStudentsEnumeration = students.elements();
            }
            public Neighbour select(Solution solution) {
                while (iStudentsEnumeration.hasMoreElements()) {
                    Student student = (Student)iStudentsEnumeration.nextElement();
                    BranchBoundEnrollmentsSelection.Selection selection = iBBEnSel.getSelection(student);
                    if (selection.select()!=null) return new N1(selection);
                }
                return null;
            }
        });
        
        //Phase 2: pick a student (one by one) with an incomplete schedule, try to find an improvement
        registerSelection(new Selection() {
            private Student iStudent = null;
            private Enumeration iStudentsEnumeration = null;
            public void init(Solution solution) {
                Vector students = new Vector(((StudentSectioningModel)solution.getModel()).getStudents());
                Collections.shuffle(students);
                iStudentsEnumeration = students.elements();
            }
            public Neighbour select(Solution solution) {
                if (iStudent!=null && !iStudent.isComplete()) {
                    SwapStudentsEnrollmentSelection.Selection selection = iSwStEnSel.getSelection(iStudent);
                    if (selection.select()!=null) return new N2(selection);
                }
                iStudent = null;
                while (iStudentsEnumeration.hasMoreElements()) {
                    Student student = (Student)iStudentsEnumeration.nextElement();
                    if (student.isComplete() || student.nrAssignedRequests()==0) continue;
                    SwapStudentsEnrollmentSelection.Selection selection = iSwStEnSel.getSelection(student);
                    if (selection.select()!=null) {
                        iStudent = student;
                        return new N2(selection);
                    }
                }
                return null;
            }
        });
        
        //Phase 3: use standard value selection for some time
        registerSelection(new Selection() {
            private long iIteration = 0;
            public void init(Solution solution) {
                iIteration = solution.getIteration();
            }
            public Neighbour select(Solution solution) {
                if (solution.getModel().unassignedVariables().isEmpty() || solution.getIteration()>=iIteration+solution.getModel().countVariables()) return null;
                for (int i=0;i<10;i++) {
                    Request request = (Request)ToolBox.random(solution.getModel().unassignedVariables());
                    Enrollment enrollment = (request==null?null:(Enrollment)getValueSelection().selectValue(solution, request));
                    if (enrollment!=null && !enrollment.variable().getModel().conflictValues(enrollment).contains(enrollment))
                        return new SimpleNeighbour(request, enrollment);
                }
                return null;
            }
        });

        //Phase 4: use backtrack neighbour selection
        registerSelection(new Selection() {
            private Enumeration iRequestEnumeration = null;
            public void init(Solution solution) {
                Vector unassigned = new Vector(solution.getModel().unassignedVariables());
                Collections.shuffle(unassigned);
                iRequestEnumeration = unassigned.elements();
            }
            public Neighbour select(Solution solution) {
                while (iRequestEnumeration.hasMoreElements()) {
                    Request request = (Request)iRequestEnumeration.nextElement();
                    Neighbour n = iRBtNSel.selectNeighbour(solution, request);
                    if (n!=null) return n;
                }
                return null;
            }
        });
        
        //Phase 5: pick a student (one by one) with an incomplete schedule, try to find an improvement, identify problematic students
        registerSelection(new Selection() {
            private Student iStudent = null;
            private Enumeration iStudentsEnumeration = null;
            public void init(Solution solution) {
                Vector students = new Vector(((StudentSectioningModel)solution.getModel()).getStudents());
                Collections.shuffle(students);
                iStudentsEnumeration = students.elements();
                iProblemStudents.clear();
            }
            public Neighbour select(Solution solution) {
                if (iStudent!=null && !iStudent.isComplete()) {
                    SwapStudentsEnrollmentSelection.Selection selection = iSwStEnSel.getSelection(iStudent);
                    if (selection.select()!=null) 
                        return new N2(selection);
                    else
                        iProblemStudents.addAll(selection.getProblemStudents());
                }
                iStudent = null;
                while (iStudentsEnumeration.hasMoreElements()) {
                    Student student = (Student)iStudentsEnumeration.nextElement();
                    if (student.isComplete() || student.nrAssignedRequests()==0) continue;
                    SwapStudentsEnrollmentSelection.Selection selection = iSwStEnSel.getSelection(student);
                    if (selection.select()!=null) {
                        iStudent = student;
                        return new N2(selection);
                    } else
                        iProblemStudents.addAll(selection.getProblemStudents());
                }
                return null;
            }
        });
            
        //Phase 6: random unassignment of some problematic students
        registerSelection(new Selection() {
            public void init(Solution solution) {
            }
            public Neighbour select(Solution solution) {
                if (!iProblemStudents.isEmpty() && Math.random()<0.9) {
                    Student student = (Student)ToolBox.random(iProblemStudents);
                    iProblemStudents.remove(student);
                    return new N3(student);
                }
                return null;
            }
        });
        
        //Phase 7: resection incomplete students 
        registerSelection(new Selection() {
            private Enumeration iStudentsEnumeration = null;
            public void init(Solution solution) {
                Vector students = new Vector(((StudentSectioningModel)solution.getModel()).getStudents());
                Collections.shuffle(students);
                iStudentsEnumeration = students.elements();
            }
            public Neighbour select(Solution solution) {
                while (iStudentsEnumeration.hasMoreElements()) {
                    Student student = (Student)iStudentsEnumeration.nextElement();
                    if (student.nrAssignedRequests()==0 || student.isComplete()) continue;
                    BranchBoundEnrollmentsSelection.Selection selection = iBBEnSel.getSelection(student);
                    if (selection.select()!=null)
                        return new N1(selection);
                }
                return null;
            }
        });
           
        //Phase 8: use standard value selection for some time
        registerSelection(new Selection() {
            private long iIteration = 0;
            public void init(Solution solution) {
                iIteration = solution.getIteration();
            }
            public Neighbour select(Solution solution) {
                if (solution.getIteration()>=iIteration+10*solution.getModel().unassignedVariables().size()) return null;
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
                    Enrollment enrollment = (request==null?null:(Enrollment)getValueSelection().selectValue(solution, request));
                    if (enrollment!=null && !enrollment.variable().getModel().conflictValues(enrollment).contains(enrollment))
                        return new SimpleNeighbour(request, enrollment);
                }
                return null;
            }
        });
        
        //Phase 9: random unassignment of some students
        registerSelection(new Selection() {
            private Vector iStudents = null;
            public void init(Solution solution) {
                iStudents = ((StudentSectioningModel)solution.getModel()).getStudents();
            }
            public Neighbour select(Solution solution) {
                if (Math.random()<0.5) {
                    Student student = (Student)ToolBox.random(iStudents);
                    return new N3(student);
                }
                return null;
            }
        });
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
                        sb.append("  -- "+enrollment);
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
                Enrollment switchEnrl = SwapStudentsEnrollmentSelection.bestSwap(conflict, enrollment, null);
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
            sb.append("\n "+iSelection.getBestEnrollment().getRequest());
            sb.append(" "+iSelection.getBestEnrollment());
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
    
    public static interface Selection {
        public Neighbour select(Solution solution); 
        public void init(Solution solutions);
    }
}
