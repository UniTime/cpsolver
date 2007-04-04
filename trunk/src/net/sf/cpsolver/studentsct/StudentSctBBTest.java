package net.sf.cpsolver.studentsct;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.studentsct.constraint.SectionLimit;
import net.sf.cpsolver.studentsct.constraint.StudentConflict;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

public class StudentSctBBTest extends Model {
    private static Logger sLog = Logger.getLogger(StudentSctBBTest.class); 
    private Student iStudent = null;
    private Solution iSolution = null;
    private long iT0, iT1;
    
    public StudentSctBBTest(Student student) {
        iStudent = student;
        StudentConflict conflict = new StudentConflict();
        for (Enumeration e=iStudent.getRequests().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            conflict.addVariable(request);
            addVariable(request);
            addGlobalConstraint(new SectionLimit());
        }
        addConstraint(conflict);
    }
    
    public Student getStudent() {
        return iStudent;
    }
    
    public Solution getSolution() {
        if (iSolution==null) {
            iT0 = System.currentTimeMillis();
            iSolution = new Solution(this);
            backTrack(iSolution, 0);
            iSolution.restoreBest();
            iT1 = System.currentTimeMillis();
        }
        return iSolution;
    }
    
    public double getBound(Solution solution, int idx) {
        double bound = 0.0;
        int i=0, alt=0;
        for (Enumeration e=getStudent().getRequests().elements();e.hasMoreElements();i++) {
            Request r  = (Request)e.nextElement();
            if (i<idx) {
                if (r.getAssignment()!=null) bound += r.getAssignment().toDouble();
                if (r.isAlternative()) {
                    if (r.getAssignment()!=null || (r instanceof CourseRequest && ((CourseRequest)r).isWaitlist())) alt--;
                } else {
                    if (r instanceof CourseRequest && !((CourseRequest)r).isWaitlist() && r.getAssignment()==null) alt++;
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
    
    public void backTrack(Solution solution, int idx) {
        //sLog.debug("backTrack("+solution.getModel().assignedVariables().size()+"/"+solution.getModel().getTotalValue()+","+idx+")");
        if (solution.getBestInfo()!=null && getBound(solution,idx)>=solution.getBestValue()) return;
        if (idx==variables().size()) {
            if (solution.getModel().getTotalValue()<solution.getBestValue()) {
                sLog.debug("  -- best solution found "+solution.getModel().getInfo());
                solution.saveBest();
            }
        } else {
            Request request = (Request)variables().elementAt(idx);
            //sLog.debug("  -- request: "+request);
            Collection values = (request instanceof CourseRequest ? (Collection)((CourseRequest)request).getAvaiableEnrollmentsSkipSameTime() : request.computeEnrollments());
            //sLog.debug("  -- nrValues: "+values.size());
            if (request.getStudent().canAssign(request)) {
                for (Iterator i=values.iterator();i.hasNext();) {
                    Enrollment enrollment = (Enrollment)i.next();
                    //sLog.debug("    -- enrollment: "+enrollment);
                    Set conflicts = conflictValues(enrollment);
                    //sLog.debug("        -- conflicts: "+conflicts);
                    if (!conflicts.isEmpty()) continue;
                    request.assign(0, enrollment);
                    backTrack(solution, idx+1);
                    request.unassign(0);
                }
            }
            backTrack(solution, idx+1);
        }
    }
    
    public Vector getMessages() {
        Vector ret = new Vector();
        ret.add("INFO:Solution found in "+(iT1-iT0)+" ms.");
        for (Enumeration e=getStudent().getRequests().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (request.isAlternative()) continue;
            if (request.getAssignment()!=null) continue;
            ret.add("ERROR:Unable to enroll to "+request+", "+(request instanceof CourseRequest?((CourseRequest)request).getCourses().size()==1?"course is":"courses are":"time is")+" not available.");
        }
        return ret;
    }
}
