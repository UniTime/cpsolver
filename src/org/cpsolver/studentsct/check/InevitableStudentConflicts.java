package org.cpsolver.studentsct.check;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.CSVFile;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.FreeTimeRequest;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.SctAssignment;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;


/**
 * This class looks and reports all cases when a student cannot obtain a
 * complete schedule because of time assignments of the requested courses.
 * Course and section limits are not checked.
 * 
 * <br>
 * <br>
 * 
 * Usage:
 * <pre><code>
 * &nbsp;&nbsp;&nbsp;&nbsp; InevitableStudentConflicts ch = new InevitableStudentConflicts(model);
 * &nbsp;&nbsp;&nbsp;&nbsp; if (!ch.check()) ch.getCSVFile().save(new File("inevitable-conflicts.csv"));
 * </code></pre>
 * 
 * <br>
 * Parameters:
 * <table border='1'><caption>Related Solver Parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>InevitableStudentConflicts.DeleteInevitable</td>
 * <td>{@link Boolean}</td>
 * <td>
 * If true, for each no-good (the smallest set of requests of the same student
 * that cannot be assigned at the same time), the problematic request (i.e., the
 * request that was not assigned by {@link StudentCheck}) is removed from the
 * model.</td>
 * </tr>
 * </table>
 * 
 * <br>
 * <br>
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2014 Tomas Muller<br>
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
public class InevitableStudentConflicts {
    private static org.apache.logging.log4j.Logger sLog = org.apache.logging.log4j.LogManager.getLogger(InevitableStudentConflicts.class);
    private StudentSectioningModel iModel;
    private CSVFile iCSVFile = null;
    public static boolean sDebug = false;
    private boolean iDeleteInevitable;

    /**
     * Constructor
     * 
     * @param model
     *            student sectioning model
     */
    public InevitableStudentConflicts(StudentSectioningModel model) {
        iModel = model;
        iCSVFile = new CSVFile();
        iCSVFile.setHeader(new CSVFile.CSVField[] { new CSVFile.CSVField("NoGood"), new CSVFile.CSVField("NrStud"),
                new CSVFile.CSVField("StudWeight"), new CSVFile.CSVField("1. Course"),
                new CSVFile.CSVField("2. Course"), new CSVFile.CSVField("3. Course"),
                new CSVFile.CSVField("4. Course"), new CSVFile.CSVField("5. Course") });
        iDeleteInevitable = model.getProperties().getPropertyBoolean("InevitableStudentConflicts.DeleteInevitable",
                false);
    }

    /** Return student sectioning model 
     * @return problem model
     **/
    public StudentSectioningModel getModel() {
        return iModel;
    }

    /** Return report
     * @return generated report
     **/
    public CSVFile getCSVFile() {
        return iCSVFile;
    }

    /** Check model for inevitable student conflicts 
     * @param assignment current assignment
     * @return true if there are no inevitable student conflicts
     **/
    public boolean check(Assignment<Request, Enrollment> assignment) {
        sLog.info("Checking for inevitable student conflicts...");
        HashMap<TreeSet<Object>, Object[]> noGoods = new HashMap<TreeSet<Object>, Object[]>();
        long studentWithoutCompleteSchedule = 0;
        long inevitableRequests = 0;
        double inevitableRequestWeight = 0.0;
        long incompleteInevitableRequests = 0;
        double incompleteInevitableRequestWeight = 0.0;
        long total = 0;
        Comparator<Object> simpleCmp = new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                return o1.toString().compareTo(o2.toString());
            }
        };
        HashSet<Request> requests2remove = new HashSet<Request>();
        for (Student student : getModel().getStudents()) {
            sLog.debug("  Checking " + (++total) + ". student " + student + "...");
            if (student.isComplete(assignment)) {
                for (Request request : student.getRequests()) {
                    if (assignment.getValue(request) == null) {
                        inevitableRequests++;
                        inevitableRequestWeight += request.getWeight();
                    }
                }
            } else {
                StudentCheck ch = new StudentCheck(student.getRequests());
                ch.check(assignment);
                if (!ch.isBestComplete()) {
                    sLog.info("    Student " + student + " cannot have a complete schedule");
                    studentWithoutCompleteSchedule++;
                }
                int idx = 0;
                for (Iterator<Request> f = student.getRequests().iterator(); f.hasNext(); idx++) {
                    Request request = f.next();
                    Enrollment enrollment = ch.getBestAssignment()[idx];
                    if (enrollment == null) {
                        if (!ch.isBestComplete()) {
                            List<Request> noGood = noGood(assignment, student, ch, idx);
                            sLog.info("      Request " + request + " cannot be assigned");
                            for (Request r : noGood) {
                                sLog.debug("        " + r);
                                Collection<Enrollment> values = null;
                                if (r instanceof CourseRequest) {
                                    values = ((CourseRequest) r).getEnrollmentsSkipSameTime(assignment);
                                } else {
                                    values = request.computeEnrollments(assignment);
                                }
                                for (Enrollment en : values) {
                                    sLog.debug("          " + enrollment2string(en));
                                }
                            }
                            if (iDeleteInevitable) {
                                requests2remove.add(request); // noGood.lastElement()
                                sLog.info("        -- request " + request + " picked to be removed from the model");
                            }
                            TreeSet<Object> key = new TreeSet<Object>(simpleCmp);
                            for (Request r : noGood) {
                                if (r instanceof CourseRequest) {
                                    key.add(((CourseRequest) r).getCourses().get(0));
                                } else {
                                    key.add("Free " + ((FreeTimeRequest) r).getTime().getLongName(true));
                                }
                            }
                            Object[] counter = noGoods.get(key);
                            int ir = (counter == null ? 1 : ((Integer) counter[0]).intValue() + 1);
                            double irw = (counter == null ? 0.0 : ((Double) counter[1]).doubleValue())
                                    + request.getWeight();
                            noGoods.put(key, new Object[] { Integer.valueOf(ir), Double.valueOf(irw) });
                            if (ch.canAssign(request, idx)) {
                                incompleteInevitableRequests++;
                                incompleteInevitableRequestWeight += request.getWeight();
                            }
                        }
                        inevitableRequests++;
                        inevitableRequestWeight += request.getWeight();
                    }
                }
            }
        }
        for (Map.Entry<TreeSet<Object>, Object[]> entry : noGoods.entrySet()) {
            TreeSet<Object> noGood = entry.getKey();
            Object[] counter = entry.getValue();
            List<CSVFile.CSVField> fields = new ArrayList<CSVFile.CSVField>();
            String courseStr = "";
            for (Iterator<Object> j = noGood.iterator(); j.hasNext();) {
                Object x = j.next();
                if (x instanceof Course) {
                    Course course = (Course) x;
                    courseStr += course.getName();
                } else
                    courseStr += x.toString();
                if (j.hasNext())
                    courseStr += ", ";
            }
            fields.add(new CSVFile.CSVField(courseStr));
            fields.add(new CSVFile.CSVField(((Integer) counter[0]).intValue()));
            fields.add(new CSVFile.CSVField(((Double) counter[1]).doubleValue()));
            for (Iterator<Object> j = noGood.iterator(); j.hasNext();) {
                Object x = j.next();
                if (x instanceof Course) {
                    Course course = (Course) x;
                    List<Course> courses = new ArrayList<Course>(1);
                    courses.add(course);
                    CourseRequest cr = new CourseRequest(-1, 0, false, new Student(-1), courses, false, null);
                    String field = course.getName();
                    int idx = 0;
                    for (Iterator<Enrollment> k = cr.getEnrollmentsSkipSameTime(assignment).iterator(); k.hasNext();) {
                        if (idx++ > 20) {
                            field += "\n ...";
                            break;
                        } else {
                            field += "\n  " + enrollment2string(k.next());
                        }
                    }
                    fields.add(new CSVFile.CSVField(field));
                } else
                    fields.add(new CSVFile.CSVField(x.toString()));
            }
            iCSVFile.addLine(fields);
        }
        if (!requests2remove.isEmpty()) {
            for (Request request : requests2remove) {
                removeRequest(request);
            }
        }
        sLog.info("Students that can never obtain a complete schedule: " + studentWithoutCompleteSchedule);
        sLog.info("Inevitable student requests: " + inevitableRequests);
        sLog.info("Inevitable student request weight: " + inevitableRequestWeight);
        sLog.info("Inevitable student requests of students without a complete schedule: "
                + incompleteInevitableRequests);
        sLog.info("Inevitable student request weight of students without a complete schedule: "
                + incompleteInevitableRequestWeight);
        if (iCSVFile.getLines() != null)
            Collections.sort(iCSVFile.getLines(), new Comparator<CSVFile.CSVLine>() {
                @Override
                public int compare(CSVFile.CSVLine l1, CSVFile.CSVLine l2) {
                    int cmp = Double.compare(l2.getField(1).toDouble(), l1.getField(1).toDouble());
                    if (cmp != 0)
                        return cmp;
                    return l1.getField(0).toString().compareTo(l2.getField(0).toString());
                }
            });
        return (inevitableRequests == 0);
    }

    /** Remove given request from the model */
    private void removeRequest(Request request) {
        request.getStudent().getRequests().remove(request);
        for (Request r : request.getStudent().getRequests()) {
            if (r.getPriority() > request.getPriority())
                r.setPriority(r.getPriority() - 1);
        }
        iModel.removeVariable(request);
        if (request.getStudent().getRequests().isEmpty())
            iModel.getStudents().remove(request.getStudent());
    }

    /**
     * Convert given enrollment to a string (comma separated list of subpart
     * names and time assignments only)
     */
    private static String enrollment2string(Enrollment enrollment) {
        StringBuffer sb = new StringBuffer();
        Collection<SctAssignment> assignments = enrollment.getAssignments();
        if (enrollment.isCourseRequest()) {
            assignments = new TreeSet<SctAssignment>(new Comparator<SctAssignment>() {
                @Override
                public int compare(SctAssignment a1, SctAssignment a2) {
                    Section s1 = (Section) a1;
                    Section s2 = (Section) a2;
                    return s1.getSubpart().compareTo(s2.getSubpart());
                }
            });
            assignments.addAll(enrollment.getAssignments());
        }
        for (Iterator<? extends SctAssignment> i = assignments.iterator(); i.hasNext();) {
            SctAssignment a = i.next();
            if (a instanceof Section)
                sb.append(((Section) a).getSubpart().getName() + " ");
            if (a.getTime() != null)
                sb.append(a.getTime().getLongName(true));
            if (i.hasNext())
                sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * No-good set of requests
     * 
     * @param student
     *            student
     * @param ch
     *            student checked that failed to find a complete schedule
     * @param idx
     *            index of unassigned course in the best found schedule
     * @return the smallest set of requests that cannot be assigned all
     *         together, containing the request with the given index
     */
    private List<Request> noGood(Assignment<Request, Enrollment> assignment, Student student, StudentCheck ch, int idx) {
        List<Request> noGood = new ArrayList<Request>();
        Request rx = student.getRequests().get(idx);
        for (int i = 0; i < student.getRequests().size(); i++) {
            if (i == idx)
                noGood.add(rx);
            else if (ch.getBestAssignment()[i] != null)
                noGood.add(ch.getBestAssignment()[i].getRequest());
        }
        for (Request r : noGood) {
            if (r.equals(rx))
                continue;
            List<Request> newNoGood = new ArrayList<Request>(noGood);
            newNoGood.remove(r);
            StudentCheck chx = new StudentCheck(newNoGood);
            chx.check(assignment);
            if (!chx.isBestComplete())
                noGood = newNoGood;
        }
        return noGood;
    }

    /**
     * Use branch &amp; bound technique to find out whether a student can get a
     * complete schedule.
     */
    public static class StudentCheck {
        private List<Request> iRequests;
        private Enrollment[] iAssignment, iBestAssignment;
        private HashMap<Request, List<Enrollment>> iValues;
        private int iBestNrAssigned = 0;
        private boolean iBestComplete = false;

        /**
         * Constructor
         * 
         * @param requests
         *            course and free time requests of a student
         */
        public StudentCheck(List<Request> requests) {
            iRequests = requests;
        }

        /**
         * Execute branch &amp; bound, return the best found schedule for the
         * selected student.
         * @param assignment current assignment
         */
        public void check(Assignment<Request, Enrollment> assignment) {
            iAssignment = new Enrollment[iRequests.size()];
            iBestAssignment = null;
            iBestNrAssigned = 0;
            iBestComplete = false;
            iValues = new HashMap<Request, List<Enrollment>>();
            backTrack(assignment, 0);
        }

        /** Best schedule
         * @return best schedule
         **/
        public Enrollment[] getBestAssignment() {
            return iBestAssignment;
        }

        /** Number of requests assigned in the best schedule
         * @return number of requests assigned in the best schedule
         **/
        public int getBestNrAssigned() {
            return iBestNrAssigned;
        }

        /** Bound for the number of assigned requests in the current schedule
         * @param idx index of the request that is being assigned
         * @return bound for the number of assigned requests in the current schedule
         **/
        public int getNrAssignedBound(int idx) {
            int bound = 0;
            int i = 0, alt = 0;
            for (Request r : iRequests) {
                if (i < idx) {
                    if (iAssignment[i] != null)
                        bound++;
                    if (r.isAlternative()) {
                        if (iAssignment[i] != null || (r instanceof CourseRequest && ((CourseRequest) r).isWaitlist()))
                            alt--;
                    } else {
                        if (r instanceof CourseRequest && !((CourseRequest) r).isWaitlist() && iAssignment[i] == null)
                            alt++;
                    }
                } else {
                    if (!r.isAlternative())
                        bound++;
                    else if (alt > 0) {
                        bound++;
                        alt--;
                    }
                }
                i++;
            }
            return bound;
        }

        /** True when the best enrollment is complete 
         * @return true when the best enrollment is complete
         **/
        public boolean isBestComplete() {
            return iBestComplete;
        }

        /** Save the current schedule as the best */
        public void saveBest() {
            if (iBestAssignment == null)
                iBestAssignment = new Enrollment[iAssignment.length];
            iBestNrAssigned = 0;
            for (int i = 0; i < iAssignment.length; i++) {
                iBestAssignment[i] = iAssignment[i];
                if (iBestAssignment[i] != null)
                    iBestNrAssigned++;
            }
            int nrRequests = 0;
            int nrAssignedRequests = 0;
            int idx = 0;
            for (Request r : iRequests) {
                if (!(r instanceof CourseRequest)) {
                    idx++;
                    continue;
                }// ignore free times
                if (!r.isAlternative())
                    nrRequests++;
                if (iBestAssignment[idx] != null)
                    nrAssignedRequests++;
                idx++;
            }
            iBestComplete = (nrAssignedRequests == nrRequests);
        }

        /** First conflicting enrollment 
         * @param enrollment an enrollment
         * @return first conflicting enrollment with the given one
         **/
        public Enrollment firstConflict(Enrollment enrollment) {
            for (int i = 0; i < iAssignment.length; i++) {
                if (iAssignment[i] != null && iAssignment[i].isOverlapping(enrollment))
                    return iAssignment[i];
            }
            return null;
        }

        /** True if the given request can be assigned 
         * @param request given request
         * @param idx index of the request that is being assigned
         * @return true if the given request can be assigned */
        public boolean canAssign(Request request, int idx) {
            if (iAssignment[idx] != null)
                return true;
            int alt = 0;
            int i = 0;
            float credit = 0f;
            for (Iterator<Request> e = iRequests.iterator(); e.hasNext(); i++) {
                Request r = e.next();
                if (r.equals(request))
                    credit += r.getMinCredit();
                else if (iAssignment[i] != null)
                    credit += iAssignment[i].getCredit();
                if (r.equals(request))
                    continue;
                if (r.isAlternative()) {
                    if (iAssignment[i] != null || (r instanceof CourseRequest && ((CourseRequest) r).isWaitlist()))
                        alt--;
                } else {
                    if (r instanceof CourseRequest && !((CourseRequest) r).isWaitlist() && iAssignment[i] == null)
                        alt++;
                }
            }
            return (!request.isAlternative() || alt > 0) && (credit <= request.getStudent().getMaxCredit());
        }

        /** Number of assigned requests in the current schedule 
         * @return number of assigned requests in the current schedule
         **/
        public int getNrAssigned() {
            int assigned = 0;
            for (int i = 0; i < iAssignment.length; i++)
                if (iAssignment[i] != null)
                    assigned++;
            return assigned;
        }

        /** branch &amp; bound search 
         * @param assignment current assignment
         * @param idx index of the request that is being assigned
         **/
        public void backTrack(Assignment<Request, Enrollment> assignment, int idx) {
            if (sDebug)
                sLog.debug("BT[" + idx + "]:  -- assigned:" + getNrAssigned() + ", bound:" + getNrAssignedBound(idx)
                        + ", best:" + getBestNrAssigned());
            if (idx == iAssignment.length) {
                if (iBestAssignment == null || getNrAssigned() > getBestNrAssigned()) {
                    saveBest();
                    if (sDebug)
                        sLog.debug("BT[" + idx + "]:    -- BEST " + getBestNrAssigned());
                }
                return;
            } else if (isBestComplete() || getNrAssignedBound(idx) <= getBestNrAssigned()) {
                if (sDebug)
                    sLog
                            .debug("BT[" + idx + "]:    -- BOUND " + getNrAssignedBound(idx) + " <= "
                                    + getBestNrAssigned());
                return;
            }
            Request request = iRequests.get(idx);
            if (sDebug)
                sLog.debug("BT[" + idx + "]:    -- REQUEST " + request);
            if (!canAssign(request, idx)) {
                if (sDebug)
                    sLog.debug("BT[" + idx + "]:      -- CANNOT ASSIGN");
                backTrack(assignment, idx + 1);
                return;
            }
            List<Enrollment> values = null;
            if (request instanceof CourseRequest) {
                CourseRequest courseRequest = (CourseRequest) request;
                values = iValues.get(courseRequest);
                if (values == null) {
                    values = courseRequest.getEnrollmentsSkipSameTime(assignment);
                    iValues.put(courseRequest, values);
                }
            } else {
                values = request.computeEnrollments(assignment);
            }
            if (sDebug)
                sLog.debug("BT[" + idx + "]:    -- VALUES: " + values.size());
            boolean hasNoConflictValue = false;
            for (Iterator<Enrollment> i = values.iterator(); i.hasNext() && !isBestComplete();) {
                Enrollment enrollment = i.next();
                if (sDebug)
                    sLog.debug("BT[" + idx + "]:      -- " + enrollment2string(enrollment));
                Enrollment conflict = firstConflict(enrollment);
                if (conflict != null) {
                    if (sDebug)
                        sLog.debug("BT[" + idx + "]:        -- conflict with " + conflict.getRequest() + " "
                                + enrollment2string(conflict));
                    continue;
                }
                hasNoConflictValue = true;
                iAssignment[idx] = enrollment;
                backTrack(assignment, idx + 1);
                iAssignment[idx] = null;
            }
            if (!hasNoConflictValue || request instanceof CourseRequest) {
                if (sDebug)
                    sLog.debug("BT[" + idx + "]:      -- without assignment");
                backTrack(assignment, idx + 1);
            }
        }
    }

}
