package net.sf.cpsolver.studentsct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.constraint.SectionLimit;
import net.sf.cpsolver.studentsct.constraint.StudentConflict;
import net.sf.cpsolver.studentsct.heuristics.selection.BranchBoundSelection;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * Online student sectioning test (using {@link BranchBoundSelection}
 * selection). This class is used by the online student sectioning mock-up page. <br>
 * <br>
 * Usage: <code>
 * StudentSctBBTest test = new StudentSctBBTest(student); //student already has all his/her requests defined<br>
 * Solution sectioningSolution = test.getSolution(); //solution contains only one student (the given one) with his/her schedule<br>
 * Vector sectioningMessages = test.getMessages(); //sectioning messages (to be printed in the GUI).
 * </code>
 * 
 * <br>
 * <br>
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          <a href="http://muller.unitime.org">http://muller.unitime.org</a><br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 3 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */
public class StudentSctBBTest extends Model<Request, Enrollment> {
    private Student iStudent = null;
    private Solution<Request, Enrollment> iSolution = null;
    private long iTime;
    private boolean iTimeoutReached = false;

    /**
     * Constructor
     * 
     * @param student
     *            a student to be sectioned
     */
    public StudentSctBBTest(Student student) {
        iStudent = student;
        for (Request request : iStudent.getRequests())
            addVariable(request);
        addGlobalConstraint(new SectionLimit(new DataProperties()));
        addConstraint(new StudentConflict(student));
    }

    /** Return the given student */
    public Student getStudent() {
        return iStudent;
    }

    /**
     * Compute and return the sectioning solution. It contains only the given
     * student with his/her schedule
     */
    public Solution<Request, Enrollment> getSolution() {
        if (iSolution == null) {
            iSolution = new Solution<Request, Enrollment>(this);
            BranchBoundSelection.Selection selection = new BranchBoundSelection(new DataProperties())
                    .getSelection(getStudent());
            Neighbour<Request, Enrollment> neighbour = selection.select();
            if (neighbour != null)
                neighbour.assign(0);
            iTime = selection.getTime();
            iTimeoutReached = selection.isTimeoutReached();
        }
        return iSolution;
    }

    /**
     * Return a list of messages ({@link Message} objects) from the sectioning
     * of the given student
     */
    public List<Message> getMessages() {
        List<Message> ret = new ArrayList<Message>();
        ret.add(new Message(Message.sMsgLevelInfo, null, "<li>Solution found in " + iTime + " ms."));
        if (iTimeoutReached)
            ret.add(new Message(Message.sMsgLevelInfo, null,
                    "<li>Time out reached, solution optimality can not be guaranteed."));
        for (Request request : getStudent().getRequests()) {
            if (!request.isAlternative() && request.getAssignment() == null) {
                ret
                        .add(new Message(Message.sMsgLevelWarn, request,
                                "<li>Unable to enroll to "
                                        + request
                                        + ", "
                                        + (request instanceof CourseRequest ? ((CourseRequest) request).getCourses()
                                                .size() == 1 ? "course is" : "courses are" : "time is")
                                        + " not available."));
                Collection<Enrollment> values = (request instanceof CourseRequest ? (Collection<Enrollment>) ((CourseRequest) request)
                        .getAvaiableEnrollmentsSkipSameTime()
                        : request.computeEnrollments());
                for (Iterator<Enrollment> f = values.iterator(); f.hasNext();) {
                    Enrollment enrollment = f.next();
                    Set<Enrollment> conf = conflictValues(enrollment);
                    if (conf != null && !conf.isEmpty()) {
                        Enrollment conflict = conf.iterator().next();
                        if (conflict.equals(enrollment))
                            ret.add(new Message(Message.sMsgLevelInfo, request, "<ul>Assignment of "
                                    + enrollment.getName().replaceAll("\n", "<br>&nbsp;&nbsp;&nbsp;&nbsp;")
                                    + "<br> is not available."));
                        else
                            ret.add(new Message(Message.sMsgLevelInfo, request, "<ul>Assignment of "
                                    + enrollment.getName().replaceAll("\n", "<br>&nbsp;&nbsp;&nbsp;&nbsp;")
                                    + "<br> conflicts with "
                                    + conflict.getName().replaceAll("\n", "<br>&nbsp;&nbsp;&nbsp;&nbsp;") + "</ul>"));
                    }
                }
            }
            if (request instanceof CourseRequest && request.getAssignment() != null) {
                CourseRequest courseRequest = (CourseRequest) request;
                Enrollment enrollment = request.getAssignment();
                List<Enrollment> selectedEnrollments = courseRequest.getSelectedEnrollments(false);
                if (selectedEnrollments != null && !selectedEnrollments.isEmpty()
                        && !selectedEnrollments.contains(enrollment)) {
                    Course course = (courseRequest.getSelectedChoices().iterator().next()).getOffering().getCourse(
                            getStudent());
                    Enrollment selected = selectedEnrollments.get(0);
                    Set<Enrollment> conf = conflictValues(selected);
                    if (conf != null && !conf.isEmpty()) {
                        ret.add(new Message(Message.sMsgLevelWarn, request,
                                "<li>Unable to enroll selected enrollment for " + course.getName() + ", seleted "
                                        + (courseRequest.getSelectedChoices().size() == 1 ? "class is" : "classes are")
                                        + " conflicting with other choices."));
                        Enrollment conflict = conf.iterator().next();
                        if (conflict.equals(selected))
                            ret.add(new Message(Message.sMsgLevelInfo, request, "<ul>Assignment of "
                                    + selected.getName().replaceAll("\n", "<br>&nbsp;&nbsp;&nbsp;&nbsp;")
                                    + "<br> is not available."));
                        else
                            ret.add(new Message(Message.sMsgLevelInfo, request, "<ul>Assignment of "
                                    + selected.getName().replaceAll("\n", "<br>&nbsp;&nbsp;&nbsp;&nbsp;")
                                    + "<br> conflicts with "
                                    + conflict.getName().replaceAll("\n", "<br>&nbsp;&nbsp;&nbsp;&nbsp;") + "</ul>"));
                    } else {
                        ret.add(new Message(Message.sMsgLevelWarn, request,
                                "<li>Unable to enroll selected enrollment for " + course.getName() + "."));
                    }
                }
            }
        }
        return ret;
    }

    /** Sectioning message */
    public static class Message {
        /** Message levels */
        public static String[] sMsgLevels = { "INFO", "WARN", "ERROR" };
        /** Info message level */
        public static int sMsgLevelInfo = 0;
        /** Warning message level */
        public static int sMsgLevelWarn = 1;
        /** Error message level */
        public static int sMsgLevelError = 2;

        private int iLevel;
        private Request iRequest;
        private String iMessage;

        /**
         * Constructor
         * 
         * @param level
         *            message level (one of
         *            {@link StudentSctBBTest.Message#sMsgLevelInfo},
         *            {@link StudentSctBBTest.Message#sMsgLevelWarn}, and
         *            {@link StudentSctBBTest.Message#sMsgLevelError})
         * @param request
         *            related course / free time request
         * @param message
         *            a message
         */
        public Message(int level, Request request, String message) {
            iLevel = level;
            iRequest = request;
            iMessage = message;
        }

        /**
         * Message level (one of {@link StudentSctBBTest.Message#sMsgLevelInfo},
         * {@link StudentSctBBTest.Message#sMsgLevelWarn}, and
         * {@link StudentSctBBTest.Message#sMsgLevelError})
         */
        public int getLevel() {
            return iLevel;
        }

        /** Message level as string */
        public String getLevelString() {
            return sMsgLevels[iLevel];
        }

        /** Related course / free time request */
        public Request getRequest() {
            return iRequest;
        }

        /** Message */
        public String getMessage() {
            return iMessage;
        }

        /** String representation (message level: message) */
        @Override
        public String toString() {
            return getLevelString() + ":" + getMessage();
        }
    }
}
