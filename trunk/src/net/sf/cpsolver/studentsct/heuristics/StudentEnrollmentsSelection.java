package net.sf.cpsolver.studentsct.heuristics;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.heuristics.ValueSelection;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.multi.MultiValue;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

public class StudentEnrollmentsSelection implements ValueSelection {
    private static Logger sLog = Logger.getLogger(StudentEnrollmentsSelection.class); 
    public static long sTimeOut = 5000;
    public static boolean sDebug = false;

    public void init(Solver solver) {}
    
    public Value selectValue(Solution solution, Variable selectedVariable) {
        Student student = (Student)selectedVariable;
        return new Selection(student).select();
    }
    
    public static class Selection {
        private Student iStudent;
        private long iT0, iT1;
        private boolean iTimeoutReached;
        private Enrollment[] iAssignment, iBestAssignment;
        private double iBestValue;
        
        public Selection(Student student) {
            iStudent = student;
        }
        
        public Value select() {
            iT0 = System.currentTimeMillis();
            iTimeoutReached = false;
            iAssignment = new Enrollment[iStudent.getRequests().size()];
            iBestAssignment = null;
            iBestValue = 0;
            backTrack(0);
            iT1 = System.currentTimeMillis();
            if (iBestAssignment==null) return null;
            return new MultiValue(iStudent, iBestAssignment);
        }
        
        public boolean isTimeoutReached() {
            return iTimeoutReached;
        }
        
        public long getTime() {
            return iT1 - iT0;
        }
        
        public Enrollment[] getBestAssignment() {
            return iBestAssignment;
        }
        
        public double getBestValue() {
            return iBestValue;
        }
        
        public double getBound(int idx) {
            double bound = 0.0;
            int i=0, alt=0;
            for (Enumeration e=iStudent.getRequests().elements();e.hasMoreElements();i++) {
                Request r  = (Request)e.nextElement();
                if (i<idx) {
                    if (iAssignment[i]!=null) bound += iAssignment[i].toDouble();
                    if (r.isAlternative()) {
                        if (iAssignment[i]!=null || (r instanceof CourseRequest && ((CourseRequest)r).isWaitlist())) alt--;
                    } else {
                        if (r instanceof CourseRequest && !((CourseRequest)r).isWaitlist() && iAssignment[i]==null) alt++;
                    }
                } else {
                    if (!r.isAlternative())
                        bound += r.getBound();
                    else if (alt>0) {
                        bound += r.getBound(); alt--;
                    }
                }
            }
            return bound;
        }
        
        public double getValue() {
            double value = 0.0;
            for (int i=0;i<iAssignment.length;i++)
                if (iAssignment[i]!=null) value += iAssignment[i].toDouble();
            return value;
        }
        
        public void saveBest() {
            double value = 0.0;
            if (iBestAssignment==null)
                iBestAssignment = new Enrollment[iAssignment.length];
            for (int i=0;i<iAssignment.length;i++) {
                iBestAssignment[i] = iAssignment[i];
                if (iAssignment[i]!=null) value += iAssignment[i].toDouble();
            }
            iBestValue = value;
        }
        
        public Enrollment firstConflict(Enrollment enrollment) {
            Set conflicts = enrollment.variable().getModel().conflictValues(enrollment);
            if (conflicts!=null && !conflicts.isEmpty()) return (Enrollment)conflicts.iterator().next();
            for (int i=0;i<iAssignment.length;i++) {
                if (iAssignment[i]==null) continue;
                if (!iAssignment[i].isConsistent(enrollment)) return iAssignment[i];
            }
            return null;
        }
        
        public boolean canAssign(Request request, int idx) {
            if (!request.isAlternative() || iAssignment[idx]!=null) return true;
            int alt = 0;
            int i = 0;
            for (Enumeration e=iStudent.getRequests().elements();e.hasMoreElements();i++) {
                Request r  = (Request)e.nextElement();
                if (r.equals(request)) continue;
                if (r.isAlternative()) {
                    if (iAssignment[i]!=null || (r instanceof CourseRequest && ((CourseRequest)r).isWaitlist())) alt--;
                } else {
                    if (r instanceof CourseRequest && !((CourseRequest)r).isWaitlist() && iAssignment[i]==null) alt++;
                }
            }
            return (alt>0);
        }
        
        public int nrAssigned() {
            int assigned = 0;
            for (int i=0;i<iAssignment.length;i++)
                if (iAssignment[i]!=null) assigned++;            
            return assigned;
        }
        
        public void backTrack(int idx) {
            if (sTimeOut>0 && (System.currentTimeMillis()-iT0)>sTimeOut) {
                if (sDebug) sLog.debug("  -- timeout reached");
                iTimeoutReached=true; return;
            }
            if (sDebug) sLog.debug("backTrack("+nrAssigned()+"/"+getValue()+","+idx+")");
            if (iBestAssignment!=null && getBound(idx)>=iBestValue) return;
            if (idx==iAssignment.length) {
                if (getValue()<iBestValue) {
                    if (sDebug) sLog.debug("  -- best solution found "+nrAssigned()+"/"+getValue());
                    saveBest();
                }
            } else {
                Request request = (Request)iStudent.getRequests().elementAt(idx);
                if (sDebug) sLog.debug("  -- request: "+request);
                if (!canAssign(request, idx)) {
                    if (sDebug) sLog.debug("    -- cannot assign");
                    backTrack(idx+1);
                    return;
                }
                Collection values = null;
                if (request instanceof CourseRequest) {
                    CourseRequest courseRequest = (CourseRequest)request;
                    if (!courseRequest.getSelectedChoices().isEmpty()) {
                        if (sDebug) sLog.debug("    -- selection among selected enrollments");
                        values = courseRequest.getSelectedEnrollments(true);
                        if (values!=null && !values.isEmpty()) { 
                            boolean hasNoConflictValue = false;
                            for (Iterator i=values.iterator();i.hasNext();) {
                                Enrollment enrollment = (Enrollment)i.next();
                                if (firstConflict(enrollment)!=null) continue;
                                hasNoConflictValue = true;
                                if (sDebug) sLog.debug("      -- nonconflicting enrollment found: "+enrollment);
                                iAssignment[idx] = enrollment;
                                backTrack(idx+1);
                                iAssignment[idx] = null;
                            }
                            if (hasNoConflictValue) return;
                        }
                    }
                    values = courseRequest.getAvaiableEnrollmentsSkipSameTime();
                } else {
                    values = request.computeEnrollments();
                }
                if (sDebug) sLog.debug("  -- nrValues: "+values.size());
                boolean hasNoConflictValue = false;
                for (Iterator i=values.iterator();i.hasNext();) {
                    Enrollment enrollment = (Enrollment)i.next();
                    if (sDebug) sLog.debug("    -- enrollment: "+enrollment);
                    Enrollment conflict = firstConflict(enrollment);
                    if (conflict!=null) {
                        if (sDebug) sLog.debug("        -- in conflict with: "+conflict);
                        continue;
                    }
                    hasNoConflictValue = true;
                    iAssignment[idx] = enrollment;
                    backTrack(idx+1);
                    iAssignment[idx] = null;
                }
                if (!hasNoConflictValue || request instanceof CourseRequest) backTrack(idx+1);
            }
        }
    }
}
