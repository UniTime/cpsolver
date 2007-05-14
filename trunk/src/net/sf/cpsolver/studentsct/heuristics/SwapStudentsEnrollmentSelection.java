package net.sf.cpsolver.studentsct.heuristics;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.heuristics.ValueSelection;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.multi.MultiValue;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

public class SwapStudentsEnrollmentSelection implements ValueSelection {
    private static Logger sLog = Logger.getLogger(SwapStudentsEnrollmentSelection.class); 
    private int iTimeout = 5000;
    private int iMaxValues = 100;
    public static boolean sDebug = false;

    public SwapStudentsEnrollmentSelection(DataProperties properties) {
        iTimeout = properties.getPropertyInt("Neighbour.SwapStudentsTimeout", iTimeout);
        iMaxValues = properties.getPropertyInt("Neighbour.SwapStudentsMaxValues", iMaxValues);
    }

    public void init(Solver solver) {}
    
    public Value selectValue(Solution solution, Variable selectedVariable) {
        Student student = (Student)selectedVariable;
        return new Selection(student).select();
    }
    
    public Selection getSelection(Student student) {
        return new Selection(student);
    }
    
    public class Selection {
        private Student iStudent;
        private long iT0, iT1;
        private boolean iTimeoutReached;
        private Enrollment iBestEnrollment;
        private double iBestValue;
        private HashSet iProblemStudents;
        
        public Selection(Student student) {
            iStudent = student;
        }
        
        public Value select() {
            if (sDebug) sLog.debug("select(S"+iStudent.getId()+")");
            iT0 = System.currentTimeMillis();
            iTimeoutReached = false;
            iBestEnrollment = null;
            iProblemStudents = new HashSet();
            for (Enumeration e=iStudent.getRequests().elements();e.hasMoreElements();) {
                if (iTimeout>0 && (System.currentTimeMillis()-iT0)>iTimeout) {
                    if (!iTimeoutReached) {
                        if (sDebug) sLog.debug("  -- timeout reached");
                        iTimeoutReached=true; 
                    }
                    break;
                }
                Request request = (Request)e.nextElement();
                if (request.getAssignment()!=null) continue;
                if (!iStudent.canAssign(request)) continue;
                if (sDebug) sLog.debug("  -- checking request "+request);
                Vector values = null;
                if (iMaxValues>0 && request instanceof CourseRequest) {
                    values = ((CourseRequest)request).computeRandomEnrollments(iMaxValues);
                } else values = request.values();
                for (Enumeration f=values.elements();f.hasMoreElements();) {
                    if (iTimeout>0 && (System.currentTimeMillis()-iT0)>iTimeout) {
                        if (!iTimeoutReached) {
                            if (sDebug) sLog.debug("  -- timeout reached");
                            iTimeoutReached=true; 
                        }
                        break;
                    }
                    Enrollment enrollment = (Enrollment)f.nextElement();
                    if (sDebug) sLog.debug("      -- enrollment "+enrollment);
                    Set conflicts = iStudent.getModel().conflictValues(enrollment);
                    if (conflicts.contains(enrollment)) continue;
                    double value = enrollment.toDouble();
                    boolean unresolvedConflict = false;
                    for (Iterator j=conflicts.iterator();j.hasNext();) {
                        Enrollment conflict = (Enrollment)j.next();
                        if (sDebug) sLog.debug("        -- conflict "+conflict);
                        Enrollment other = bestSwap(conflict, enrollment, iProblemStudents);
                        if (other==null) {
                            if (sDebug) sLog.debug("          -- unable to resolve");
                            unresolvedConflict = true; break;
                        }
                        if (sDebug) sLog.debug("          -- can be resolved by switching to "+other.getName());
                        value -= conflict.toDouble();
                        value += other.toDouble();
                    }
                    if (unresolvedConflict) continue;
                    if (iBestEnrollment==null || iBestEnrollment.toDouble()>value) {
                        iBestEnrollment = enrollment; iBestValue = value;
                    };
                }
            }
            iT1 = System.currentTimeMillis();
            if (sDebug) sLog.debug("  -- done, best enrollment is "+iBestEnrollment);
            if (iBestEnrollment==null) {
                if (iProblemStudents.isEmpty()) iProblemStudents.add(iStudent);
                if (sDebug) sLog.debug("  -- problem students are: "+iProblemStudents);
                return null;
            }
            if (sDebug) sLog.debug("  -- value "+iBestValue);
            Enrollment[] assignment = new Enrollment[iStudent.getRequests().size()];
            int idx = 0;
            for (Enumeration e=iStudent.getRequests().elements();e.hasMoreElements();) {
                Request request = (Request)e.nextElement();
                assignment[idx++] = (iBestEnrollment.getRequest().equals(request)?iBestEnrollment:(Enrollment)request.getAssignment());
            }
            return new MultiValue(iStudent, assignment);
        }
        
        public boolean isTimeoutReached() {
            return iTimeoutReached;
        }

        public long getTime() {
            return iT1 - iT0;
        }
        
        public Enrollment getBestEnrollment() {
            return iBestEnrollment;
        }
        
        public double getBestValue() {
            return iBestValue;
        }
        
        public Set getProblemStudents() {
            return iProblemStudents;
        }
        
    }

    /** Identify the best swap for the given student */
    public static Enrollment bestSwap(Enrollment conflict, Enrollment enrl, Set problematicStudents) {
        Enrollment bestEnrollment = null;
        for (Iterator i=conflict.getRequest().values().iterator();i.hasNext();) {
            Enrollment enrollment = (Enrollment)i.next();
            if (enrollment.equals(conflict)) continue;
            if (!enrl.isConsistent(enrollment)) continue;
            if (conflict.getStudent().getModel().conflictValues(enrollment).isEmpty()) {
                if (bestEnrollment==null || bestEnrollment.toDouble()>enrollment.toDouble())
                    bestEnrollment = enrollment;
            }
        }
        if (bestEnrollment==null && problematicStudents!=null) {
            boolean added = false;
            for (Iterator i=conflict.getRequest().values().iterator();i.hasNext();) {
                Enrollment enrollment = (Enrollment)i.next();
                if (enrollment.equals(conflict)) continue;
                if (!enrl.isConsistent(enrollment)) continue;
                Set conflicts = conflict.getStudent().getModel().conflictValues(enrollment);
                for (Iterator j=conflicts.iterator();j.hasNext();) {
                    Enrollment c = (Enrollment)j.next();
                    if (!enrl.getStudent().equals(c.getStudent()) && !conflict.getStudent().equals(c.getStudent()))
                        problematicStudents.add(c.getStudent());
                }
            }
            if (!added && !enrl.getStudent().equals(conflict.getStudent()))
                problematicStudents.add(conflict.getStudent());
        }
        return bestEnrollment;
    }
}
