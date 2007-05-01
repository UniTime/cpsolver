package net.sf.cpsolver.studentsct.heuristics;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.heuristics.ValueSelection;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.multi.MultiValue;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

public class SwapStudentsEnrollmentSelection implements ValueSelection {
    private static Logger sLog = Logger.getLogger(SwapStudentsEnrollmentSelection.class); 
    public static long sTimeOut = 5000;
    public static boolean sDebug = false;
    public static int sMaxValues = 100;

    public void init(Solver solver) {}
    
    public Value selectValue(Solution solution, Variable selectedVariable) {
        Student student = (Student)selectedVariable;
        return new Selection(student).select();
    }
    
    public static class Selection {
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
                if (sTimeOut>0 && (System.currentTimeMillis()-iT0)>sTimeOut) {
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
                
                for (Iterator i=ToolBox.subSet(request.values(),0,sMaxValues).iterator();i.hasNext();) {
                    if (sTimeOut>0 && (System.currentTimeMillis()-iT0)>sTimeOut) {
                        if (!iTimeoutReached) {
                            if (sDebug) sLog.debug("  -- timeout reached");
                            iTimeoutReached=true; 
                        }
                        break;
                    }
                    Enrollment enrollment = (Enrollment)i.next();
                    if (sDebug) sLog.debug("      -- enrollment "+enrollment);
                    Set conflicts = iStudent.getModel().conflictValues(enrollment);
                    double value = enrollment.toDouble();
                    boolean unresolvedConflict = false;
                    for (Iterator j=conflicts.iterator();j.hasNext();) {
                        Enrollment conflict = (Enrollment)j.next();
                        if (sDebug) sLog.debug("        -- conflict "+conflict);
                        Enrollment other = conflict.bestSwap(enrollment, iProblemStudents);
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
}
