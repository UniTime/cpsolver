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
    private Student iStudent = null;
    private Solution iSolution = null;
    private long iTime;
    private boolean iTimeoutReached = false;
    
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
        }
        return iSolution;
    }
    
    public Vector getMessages() {
        Vector ret = new Vector();
        ret.add(new Message(Message.sMsgLevelInfo,null,"<li>Solution found in "+iTime+" ms."));
        if (iTimeoutReached)
            ret.add(new Message(Message.sMsgLevelInfo,null,"<li>"+(BranchBoundEnrollmentsSelection.sTimeOut/1000)+" s time out reached, solution optimality can not be guaranteed."));
        for (Enumeration e=getStudent().getRequests().elements();e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (!request.isAlternative() && request.getAssignment()==null) {
                ret.add(new Message(Message.sMsgLevelWarn,request,"<li>Unable to enroll to "+request+", "+(request instanceof CourseRequest?((CourseRequest)request).getCourses().size()==1?"course is":"courses are":"time is")+" not available."));
                Collection values = (request instanceof CourseRequest ? (Collection)((CourseRequest)request).getAvaiableEnrollmentsSkipSameTime() : request.computeEnrollments());
                for (Iterator f=values.iterator();f.hasNext();) {
                    Enrollment enrollment = (Enrollment)f.next();
                    Set conf = conflictValues(enrollment);
                    if (conf!=null && !conf.isEmpty()) {
                        Enrollment conflict = (Enrollment)conf.iterator().next();
                        if (conflict.equals(enrollment))
                            ret.add(new Message(Message.sMsgLevelInfo,request,"<ul>Assignment of "+enrollment.getName().replaceAll("\n", "<br>&nbsp;&nbsp;&nbsp;&nbsp;")+"<br> is not available."));
                        else
                            ret.add(new Message(Message.sMsgLevelInfo,request,"<ul>Assignment of "+enrollment.getName().replaceAll("\n", "<br>&nbsp;&nbsp;&nbsp;&nbsp;")+"<br> conflicts with "+conflict.getName().replaceAll("\n", "<br>&nbsp;&nbsp;&nbsp;&nbsp;")+"</ul>"));
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
                        ret.add(new Message(Message.sMsgLevelWarn,request,"<li>Unable to enroll selected enrollment for "+course.getName()+", seleted "+(courseRequest.getSelectedChoices().size()==1?"class is":"classes are")+" conflicting with other choices."));
                        Enrollment conflict = (Enrollment)conf.iterator().next();
                        if (conflict.equals(selected))
                            ret.add(new Message(Message.sMsgLevelInfo,request,"<ul>Assignment of "+selected.getName().replaceAll("\n", "<br>&nbsp;&nbsp;&nbsp;&nbsp;")+"<br> is not available."));
                        else
                            ret.add(new Message(Message.sMsgLevelInfo,request,"<ul>Assignment of "+selected.getName().replaceAll("\n", "<br>&nbsp;&nbsp;&nbsp;&nbsp;")+"<br> conflicts with "+conflict.getName().replaceAll("\n", "<br>&nbsp;&nbsp;&nbsp;&nbsp;")+"</ul>"));
                    } else {
                        ret.add(new Message(Message.sMsgLevelWarn,request,"<li>Unable to enroll selected enrollment for "+course.getName()+"."));
                    }
                }
            }
        }
        return ret;
    }
    
    public static class Message {
        public static String[] sMsgLevels = { "INFO", "WARN", "ERROR" };
        public static int sMsgLevelInfo = 0;
        public static int sMsgLevelWarn = 1;
        public static int sMsgLevelError = 2;
        private int iLevel; 
        private Request iRequest;
        private String iMessage;
        public Message(int level, Request request, String message) {
            iLevel = level;
            iRequest = request;
            iMessage = message;
        }
        public int getLevel() {
            return iLevel;
        }
        public String getLevelString() {
            return sMsgLevels[iLevel];
        }
        public Request getRequest() {
            return iRequest;
        }
        public String getMessage() {
            return iMessage;
        }
        public String toString() {
            return getLevelString()+":"+getMessage();
        }
    }
}
