package net.sf.cpsolver.studentsct;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.studentsct.constraint.SectionLimit;
import net.sf.cpsolver.studentsct.constraint.StudentConflict;
import net.sf.cpsolver.studentsct.heuristics.BranchBoundEnrollmentsSelection;
import net.sf.cpsolver.studentsct.model.Choice;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

public class StudentSctBBTest extends Model {
    //private static Logger sLog = Logger.getLogger(StudentSctBBTest.class); 
    private Student iStudent = null;
    private Solution iSolution = null;
    private long iTime;
    //private static long sTimeOut = 5000;
    private boolean iTimeoutReached = false;
    //private static boolean sDebug = false;
    
    public StudentSctBBTest(Student student) {
        iStudent = student;
        StudentConflict conflict = new StudentConflict();
        for (Enumeration e=iStudent.getRequests().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            conflict.addVariable(request);
            addVariable(request);
        }
        addGlobalConstraint(new SectionLimit());
        addConstraint(conflict);
    }
    
    public Student getStudent() {
        return iStudent;
    }
    
    public Solution getSolution() {
        if (iSolution==null) {
            iSolution = new Solution(this);
            BranchBoundEnrollmentsSelection.Selection selection = new BranchBoundEnrollmentsSelection.Selection(getStudent());
            Value value = selection.select();
            if (value!=null)
                getStudent().assign(0, value);
            iTime = selection.getTime();
            iTimeoutReached = selection.isTimeoutReached();
            /*
            iT0 = System.currentTimeMillis();
            iTimeoutReached = false;
            iSolution = new Solution(this);
            backTrack(iSolution, 0);
            iSolution.restoreBest();
            iT1 = System.currentTimeMillis();
            */
        }
        return iSolution;
    }
    
    /*
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
        if ((System.currentTimeMillis()-iT0)>sTimeOut) {
            iTimeoutReached=true; return;
        }
        if (sDebug) sLog.debug("backTrack("+solution.getModel().assignedVariables().size()+"/"+solution.getModel().getTotalValue()+","+idx+")");
        if (solution.getBestInfo()!=null && getBound(solution,idx)>=solution.getBestValue()) return;
        if (idx==variables().size()) {
            if (solution.getModel().getTotalValue()<solution.getBestValue()) {
                if (sDebug) sLog.debug("  -- best solution found "+solution.getModel().getInfo());
                solution.saveBest();
            }
        } else {
            Request request = (Request)variables().elementAt(idx);
            if (sDebug) sLog.debug("  -- request: "+request);
            if (!request.getStudent().canAssign(request)) {
                backTrack(solution, idx+1);
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
                            Set conflicts = conflictValues(enrollment);
                            if (!conflicts.isEmpty()) continue;
                            if (sDebug) sLog.debug("      -- nonconflicting enrollment found: "+enrollment);
                            hasNoConflictValue = true;
                            request.assign(0, enrollment);
                            backTrack(solution, idx+1);
                            request.unassign(0);
                        }
                        if (hasNoConflictValue) return;
                    }
                }
                values = courseRequest.getAvaiableEnrollmentsSkipSameTime();
            } else {
                values = request.computeEnrollments();
            }
            boolean hasNoConflictValue = false;
            if (sDebug) sLog.debug("  -- nrValues: "+values.size());
            for (Iterator i=values.iterator();i.hasNext();) {
                Enrollment enrollment = (Enrollment)i.next();
                if (sDebug) sLog.debug("    -- enrollment: "+enrollment);
                Set conflicts = conflictValues(enrollment);
                if (sDebug) sLog.debug("        -- conflicts: "+conflicts);
                if (!conflicts.isEmpty()) continue;
                hasNoConflictValue = true;
                request.assign(0, enrollment);
                backTrack(solution, idx+1);
                request.unassign(0);
            }
            if (!hasNoConflictValue || request instanceof CourseRequest) backTrack(solution, idx+1);
        }
    }
    */
    
    public Vector getMessages() {
        Vector ret = new Vector();
        ret.add("INFO:<li>Solution found in "+iTime+" ms.");
        if (iTimeoutReached)
            ret.add("INFO:<li>"+(BranchBoundEnrollmentsSelection.sTimeOut/1000)+" s time out reached, solution optimality can not be guaranteed.");
        for (Enumeration e=getStudent().getRequests().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (!request.isAlternative() && request.getAssignment()==null) {
                ret.add("WARN:<li>Unable to enroll to "+request+", "+(request instanceof CourseRequest?((CourseRequest)request).getCourses().size()==1?"course is":"courses are":"time is")+" not available.");
                Collection values = (request instanceof CourseRequest ? (Collection)((CourseRequest)request).getAvaiableEnrollmentsSkipSameTime() : request.computeEnrollments());
                for (Iterator f=values.iterator();f.hasNext();) {
                    Enrollment enrollment = (Enrollment)f.next();
                    Set conf = conflictValues(enrollment);
                    if (conf!=null && !conf.isEmpty()) {
                        Enrollment conflict = (Enrollment)conf.iterator().next();
                        ret.add("INFO:<ul>Assignment of "+enrollment.getName().replaceAll("\n", "<br>&nbsp;&nbsp;&nbsp;&nbsp;")+"<br> conflicts with "+conflict.getName().replaceAll("\n", "<br>&nbsp;&nbsp;&nbsp;&nbsp;")+"</ul>");
                    }
                }
            }
            if (request instanceof CourseRequest && request.getAssignment()!=null) {
                CourseRequest courseRequest = (CourseRequest)request;
                Enrollment enrollment = (Enrollment)request.getAssignment();
                Vector selectedEnrollments = courseRequest.getSelectedEnrollments(false);
                if (selectedEnrollments!=null && !selectedEnrollments.isEmpty() && !selectedEnrollments.contains(enrollment)) {
                    Course course = ((Choice)courseRequest.getSelectedChoices().iterator().next()).getOffering().getCourse(getStudent());
                    Enrollment selected = (Enrollment)selectedEnrollments.firstElement();
                    Set conf = conflictValues(selected);
                    if (conf!=null && !conf.isEmpty()) {
                        ret.add("ERROR:<li>Unable to enroll selected enrollment for "+course.getName()+", seleted "+(courseRequest.getSelectedChoices().size()==1?"class is":"classes are")+" conflicting with other choices.");
                        Enrollment conflict = (Enrollment)conf.iterator().next();
                        ret.add("INFO:<ul>Assignment of "+selected.getName().replaceAll("\n", "<br>&nbsp;&nbsp;&nbsp;&nbsp;")+"<br> conflicts with "+conflict.getName().replaceAll("\n", "<br>&nbsp;&nbsp;&nbsp;&nbsp;")+"</ul>");
                    } else {
                        ret.add("ERROR:<li>Unable to enroll selected enrollment for "+course.getName()+".");
                    }
                }
            }
        }
        return ret;
    }
}
