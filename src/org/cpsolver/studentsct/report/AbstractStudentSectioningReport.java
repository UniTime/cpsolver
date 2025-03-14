package org.cpsolver.studentsct.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.CSVFile;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.Query;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Instructor;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Request.RequestPriority;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Student.BackToBackPreference;
import org.cpsolver.studentsct.model.Student.ModalityPreference;
import org.cpsolver.studentsct.reservation.UniversalOverride;

/**
 * Abstract student sectioning report. Adds filtering capabilities using the 
 * filter parameter. It also checks the lastlike and real parameters (whether to
 * include projected and real students respectively) and passes on the useAmPm parameter
 * as {@link #isUseAmPm()}. The filter replicates most of the capabilities available
 * in UniTime on the Batch Student Solver Dashboard page.
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
public abstract class AbstractStudentSectioningReport implements StudentSectioningReport, StudentSectioningReport.Filter {
    private StudentSectioningModel iModel = null;
    private Query iFilter = null;
    private String iUser = null;
    private Assignment<Request, Enrollment> iAssignment;
    private boolean iIncludeLastLike = false;
    private boolean iIncludeReal = true;
    private boolean iUseAmPm = true;

    public AbstractStudentSectioningReport(StudentSectioningModel model) {
        iModel = model;
    }

    /**
     * Returns the student sectioning model
     */
    public StudentSectioningModel getModel() {
        return iModel;
    }

    @Override
    public CSVFile create(Assignment<Request, Enrollment> assignment, DataProperties properties) {
        String filter = properties.getProperty("filter");
        if (filter != null && !filter.isEmpty())
            iFilter = new Query(filter);
        iUser = properties.getProperty("user");
        iIncludeLastLike = properties.getPropertyBoolean("lastlike", false);
        iIncludeReal = properties.getPropertyBoolean("real", true);
        iUseAmPm = properties.getPropertyBoolean("useAmPm", true);
        iAssignment = assignment;
        return createTable(assignment, properties);
    }

    @Override
    public boolean matches(Request r, Enrollment e) {
        if (iFilter == null)
            return true;
        if (r.getStudent().isDummy() && !iIncludeLastLike) return false;
        if (!r.getStudent().isDummy() && !iIncludeReal) return false;
        return iFilter.match(new RequestMatcher(r, e, iAssignment, iUser));
    }
    
    @Override
    public boolean matches(Request r) {
        if (iFilter == null)
            return true;
        if (r.getStudent().isDummy() && !iIncludeLastLike) return false;
        if (!r.getStudent().isDummy() && !iIncludeReal) return false;
        return iFilter.match(new RequestMatcher(r, iAssignment.getValue(r), iAssignment, iUser));
    }
    
    @Override
    public boolean matches(Course c) {
        if (iFilter == null) return true;
        return iFilter.match(new CourseMatcher(c));
    }
    
    @Override
    public boolean matches(Student student) {
        for (Request r: student.getRequests())
            if (matches(r)) return true;
        return false;
    }
    
    /**
     * Time display settings
     */
    public boolean isUseAmPm() { return iUseAmPm; }

    public abstract CSVFile createTable(Assignment<Request, Enrollment> assignment, DataProperties properties);

    public static class RequestMatcher extends UniversalOverride.StudentMatcher {
        private Request iRequest;
        private Enrollment iEnrollment;
        private String iUser;
        private Assignment<Request, Enrollment> iAssignment;

        public RequestMatcher(Request request, Enrollment enrollment, Assignment<Request, Enrollment> assignment, String user) {
            super(request.getStudent());
            iRequest = request;
            iEnrollment = enrollment;
            iAssignment = assignment;
            iUser = user;
        }

        public boolean isAssigned() {
            return iEnrollment != null;
        }

        public Enrollment enrollment() {
            return iEnrollment;
        }

        public Request request() {
            return iRequest;
        }

        public CourseRequest cr() {
            return iRequest instanceof CourseRequest ? (CourseRequest) iRequest : null;
        }
        
        public Course course() {
            if (enrollment() != null)
                return enrollment().getCourse();
            else if (request() instanceof CourseRequest)
                return ((CourseRequest) request()).getCourses().get(0);
            else
                return null;
        }

        @Override
        public boolean match(String attr, String term) {
            if (super.match(attr, term))
                return true;

            if ("assignment".equals(attr)) {
                if (eq("Assigned", term)) {
                    return isAssigned();
                } else if (eq("Reserved", term)) {
                    return isAssigned() && enrollment().getReservation() != null;
                } else if (eq("Not Assigned", term)) {
                    return !isAssigned() && !request().isAlternative();
                } else if (eq("Wait-Listed", term)) {
                    if (enrollment() == null)
                        return cr() != null && cr().isWaitlist();
                    else
                        return enrollment().isWaitlisted();
                } else if (eq("Critical", term)) {
                    return request().getRequestPriority() == RequestPriority.Critical;
                } else if (eq("Assigned Critical", term)) {
                    return request().getRequestPriority() == RequestPriority.Critical && isAssigned();
                } else if (eq("Not Assigned Critical", term)) {
                    return request().getRequestPriority() == RequestPriority.Critical && !isAssigned();
                } else if (eq("Vital", term)) {
                    return request().getRequestPriority() == RequestPriority.Vital;
                } else if (eq("Assigned Vital", term)) {
                    return request().getRequestPriority() == RequestPriority.Vital && isAssigned();
                } else if (eq("Not Assigned Vital", term)) {
                    return request().getRequestPriority() == RequestPriority.Vital && !isAssigned();
                } else if (eq("Visiting F2F", term)) {
                    return request().getRequestPriority() == RequestPriority.VisitingF2F;
                } else if (eq("Assigned Visiting F2F", term)) {
                    return request().getRequestPriority() == RequestPriority.VisitingF2F && isAssigned();
                } else if (eq("Not Assigned Visiting F2F", term)) {
                    return request().getRequestPriority() == RequestPriority.VisitingF2F && !isAssigned();
                } else if (eq("LC", term)) {
                    return request().getRequestPriority() == RequestPriority.LC;
                } else if (eq("Assigned LC", term)) {
                    return request().getRequestPriority() == RequestPriority.LC && isAssigned();
                } else if (eq("Not Assigned LC", term)) {
                    return request().getRequestPriority() == RequestPriority.LC && !isAssigned();
                } else if (eq("Important", term)) {
                    return request().getRequestPriority() == RequestPriority.Important;
                } else if (eq("Assigned Important", term)) {
                    return request().getRequestPriority() == RequestPriority.Important && isAssigned();
                } else if (eq("Not Assigned Important", term)) {
                    return request().getRequestPriority() == RequestPriority.Important && !isAssigned();
                } else if (eq("No-Subs", term) || eq("No-Substitutes", term)) {
                    return cr() != null && cr().isWaitlist();
                } else if (eq("Assigned No-Subs", term) || eq("Assigned  No-Substitutes", term)) {
                    return cr() != null && cr().isWaitlist() && isAssigned();
                } else if (eq("Not Assigned No-Subs", term) || eq("Not Assigned No-Substitutes", term)) {
                    return cr() != null && cr().isWaitlist() && !isAssigned();
                }
            }

            if ("assigned".equals(attr) || "scheduled".equals(attr)) {
                if (eq("true", term) || eq("1", term))
                    return isAssigned();
                else
                    return !isAssigned();
            }

            if ("waitlisted".equals(attr) || "waitlist".equals(attr)) {
                if (eq("true", term) || eq("1", term))
                    return !isAssigned() && cr() != null && cr().isWaitlist();
                else
                    return isAssigned() && cr() != null && cr().isWaitlist();
            }

            if ("no-substitutes".equals(attr) || "no-subs".equals(attr)) {
                if (eq("true", term) || eq("1", term))
                    return !isAssigned() && cr() != null && cr().isWaitlist();
                else
                    return isAssigned() && cr() != null && cr().isWaitlist();
            }

            if ("reservation".equals(attr) || "reserved".equals(attr)) {
                if (eq("true", term) || eq("1", term))
                    return isAssigned() && enrollment().getReservation() != null;
                else
                    return isAssigned() && enrollment().getReservation() == null;
            }

            if ("mode".equals(attr)) {
                if (eq("My Students", term)) {
                    if (iUser == null)
                        return false;
                    for (Instructor a : student().getAdvisors())
                        if (eq(a.getExternalId(), iUser))
                            return true;
                    return false;
                }
                return true;
            }

            if ("status".equals(attr)) {
                if ("default".equalsIgnoreCase(term) || "Not Set".equalsIgnoreCase(term))
                    return student().getStatus() == null;
                return like(student().getStatus(), term);
            }

            if ("credit".equals(attr)) {
                float min = 0, max = Float.MAX_VALUE;
                Credit prefix = Credit.eq;
                String number = term;
                if (number.startsWith("<=")) {
                    prefix = Credit.le;
                    number = number.substring(2);
                } else if (number.startsWith(">=")) {
                    prefix = Credit.ge;
                    number = number.substring(2);
                } else if (number.startsWith("<")) {
                    prefix = Credit.lt;
                    number = number.substring(1);
                } else if (number.startsWith(">")) {
                    prefix = Credit.gt;
                    number = number.substring(1);
                } else if (number.startsWith("=")) {
                    prefix = Credit.eq;
                    number = number.substring(1);
                }
                String im = null;
                try {
                    float a = Float.parseFloat(number);
                    switch (prefix) {
                        case eq:
                            min = max = a;
                            break; // = a
                        case le:
                            max = a;
                            break; // <= a
                        case ge:
                            min = a;
                            break; // >= a
                        case lt:
                            max = a - 1;
                            break; // < a
                        case gt:
                            min = a + 1;
                            break; // > a
                    }
                } catch (NumberFormatException e) {
                    Matcher m = Pattern.compile("([0-9]+\\.?[0-9]*)([^0-9\\.].*)").matcher(number);
                    if (m.matches()) {
                        float a = Float.parseFloat(m.group(1));
                        im = m.group(2).trim();
                        switch (prefix) {
                            case eq:
                                min = max = a;
                                break; // = a
                            case le:
                                max = a;
                                break; // <= a
                            case ge:
                                min = a;
                                break; // >= a
                            case lt:
                                max = a - 1;
                                break; // < a
                            case gt:
                                min = a + 1;
                                break; // > a
                        }
                    }
                }
                if (term.contains("..")) {
                    try {
                        String a = term.substring(0, term.indexOf('.'));
                        String b = term.substring(term.indexOf("..") + 2);
                        min = Float.parseFloat(a);
                        max = Float.parseFloat(b);
                    } catch (NumberFormatException e) {
                        Matcher m = Pattern.compile("([0-9]+\\.?[0-9]*)\\.\\.([0-9]+\\.?[0-9]*)([^0-9].*)")
                                .matcher(term);
                        if (m.matches()) {
                            min = Float.parseFloat(m.group(1));
                            max = Float.parseFloat(m.group(2));
                            im = m.group(3).trim();
                        }
                    }
                }
                float credit = 0;
                for (Request r : student().getRequests()) {
                    if (r instanceof CourseRequest) {
                        CourseRequest cr = (CourseRequest) r;
                        Enrollment e = iAssignment.getValue(cr);
                        if (e == null)
                            continue;
                        Config g = e.getConfig();
                        if (g != null) {
                            if ("!".equals(im) && g.getInstructionalMethodReference() != null)
                                continue;
                            if (im != null && !"!".equals(im)
                                    && !im.equalsIgnoreCase(g.getInstructionalMethodReference()))
                                continue;
                            if (g.hasCreditValue())
                                credit += g.getCreditValue();
                            else if (e.getCourse().hasCreditValue())
                                credit += e.getCourse().getCreditValue();
                        }
                    }
                }
                return min <= credit && credit <= max;
            }

            if ("rc".equals(attr) || "requested-credit".equals(attr)) {
                int min = 0, max = Integer.MAX_VALUE;
                Credit prefix = Credit.eq;
                String number = term;
                if (number.startsWith("<=")) {
                    prefix = Credit.le;
                    number = number.substring(2);
                } else if (number.startsWith(">=")) {
                    prefix = Credit.ge;
                    number = number.substring(2);
                } else if (number.startsWith("<")) {
                    prefix = Credit.lt;
                    number = number.substring(1);
                } else if (number.startsWith(">")) {
                    prefix = Credit.gt;
                    number = number.substring(1);
                } else if (number.startsWith("=")) {
                    prefix = Credit.eq;
                    number = number.substring(1);
                }
                try {
                    int a = Integer.parseInt(number);
                    switch (prefix) {
                        case eq:
                            min = max = a;
                            break; // = a
                        case le:
                            max = a;
                            break; // <= a
                        case ge:
                            min = a;
                            break; // >= a
                        case lt:
                            max = a - 1;
                            break; // < a
                        case gt:
                            min = a + 1;
                            break; // > a
                    }
                } catch (NumberFormatException e) {
                }
                if (term.contains("..")) {
                    try {
                        String a = term.substring(0, term.indexOf('.'));
                        String b = term.substring(term.indexOf("..") + 2);
                        min = Integer.parseInt(a);
                        max = Integer.parseInt(b);
                    } catch (NumberFormatException e) {
                    }
                }
                if (min == 0 && max == Integer.MAX_VALUE)
                    return true;
                float studentMinTot = 0f, studentMaxTot = 0f;
                int nrCoursesTot = 0;
                List<Float> minsTot = new ArrayList<Float>();
                List<Float> maxsTot = new ArrayList<Float>();
                for (Request r : student().getRequests()) {
                    if (r instanceof CourseRequest) {
                        CourseRequest cr = (CourseRequest) r;
                        Float minTot = null, maxTot = null;
                        for (Course c : cr.getCourses()) {
                            if (c.hasCreditValue()) {
                                if (minTot == null || minTot > c.getCreditValue())
                                    minTot = c.getCreditValue();
                                if (maxTot == null || maxTot < c.getCreditValue())
                                    maxTot = c.getCreditValue();
                            }
                        }
                        if (cr.isWaitlist()) {
                            if (minTot != null) {
                                studentMinTot += minTot;
                                studentMaxTot += maxTot;
                            }
                        } else {
                            if (minTot != null) {
                                minsTot.add(minTot);
                                maxsTot.add(maxTot);
                                if (!r.isAlternative())
                                    nrCoursesTot++;
                            }
                        }
                    }
                }
                Collections.sort(minsTot);
                Collections.sort(maxsTot);
                for (int i = 0; i < nrCoursesTot; i++) {
                    studentMinTot += minsTot.get(i);
                    studentMaxTot += maxsTot.get(maxsTot.size() - i - 1);
                }
                return min <= studentMaxTot && studentMinTot <= max;
            }

            if ("fc".equals(attr) || "first-choice-credit".equals(attr)) {
                int min = 0, max = Integer.MAX_VALUE;
                Credit prefix = Credit.eq;
                String number = term;
                if (number.startsWith("<=")) {
                    prefix = Credit.le;
                    number = number.substring(2);
                } else if (number.startsWith(">=")) {
                    prefix = Credit.ge;
                    number = number.substring(2);
                } else if (number.startsWith("<")) {
                    prefix = Credit.lt;
                    number = number.substring(1);
                } else if (number.startsWith(">")) {
                    prefix = Credit.gt;
                    number = number.substring(1);
                } else if (number.startsWith("=")) {
                    prefix = Credit.eq;
                    number = number.substring(1);
                }
                try {
                    int a = Integer.parseInt(number);
                    switch (prefix) {
                        case eq:
                            min = max = a;
                            break; // = a
                        case le:
                            max = a;
                            break; // <= a
                        case ge:
                            min = a;
                            break; // >= a
                        case lt:
                            max = a - 1;
                            break; // < a
                        case gt:
                            min = a + 1;
                            break; // > a
                    }
                } catch (NumberFormatException e) {
                }
                if (term.contains("..")) {
                    try {
                        String a = term.substring(0, term.indexOf('.'));
                        String b = term.substring(term.indexOf("..") + 2);
                        min = Integer.parseInt(a);
                        max = Integer.parseInt(b);
                    } catch (NumberFormatException e) {
                    }
                }
                if (min == 0 && max == Integer.MAX_VALUE)
                    return true;
                float credit = 0f;
                for (Request r : student().getRequests()) {
                    if (r instanceof CourseRequest) {
                        CourseRequest cr = (CourseRequest) r;
                        for (Course c : cr.getCourses()) {
                            if (c != null && c.hasCreditValue()) {
                                credit += c.getCreditValue();
                                break;
                            }
                        }
                    }
                }
                return min <= credit && credit <= max;
            }

            if ("rp".equals(attr)) {
                if ("subst".equalsIgnoreCase(term))
                    return request().isAlternative();
                int min = 0, max = Integer.MAX_VALUE;
                Credit prefix = Credit.eq;
                String number = term;
                if (number.startsWith("<=")) {
                    prefix = Credit.le;
                    number = number.substring(2);
                } else if (number.startsWith(">=")) {
                    prefix = Credit.ge;
                    number = number.substring(2);
                } else if (number.startsWith("<")) {
                    prefix = Credit.lt;
                    number = number.substring(1);
                } else if (number.startsWith(">")) {
                    prefix = Credit.gt;
                    number = number.substring(1);
                } else if (number.startsWith("=")) {
                    prefix = Credit.eq;
                    number = number.substring(1);
                }
                try {
                    int a = Integer.parseInt(number);
                    switch (prefix) {
                        case eq:
                            min = max = a;
                            break; // = a
                        case le:
                            max = a;
                            break; // <= a
                        case ge:
                            min = a;
                            break; // >= a
                        case lt:
                            max = a - 1;
                            break; // < a
                        case gt:
                            min = a + 1;
                            break; // > a
                    }
                } catch (NumberFormatException e) {
                }
                if (term.contains("..")) {
                    try {
                        String a = term.substring(0, term.indexOf('.'));
                        String b = term.substring(term.indexOf("..") + 2);
                        min = Integer.parseInt(a);
                        max = Integer.parseInt(b);
                    } catch (NumberFormatException e) {
                    }
                }
                if (min == 0 && max == Integer.MAX_VALUE)
                    return true;
                return !request().isAlternative() && min <= request().getPriority() + 1
                        && request().getPriority() + 1 <= max;
            }

            if ("choice".equals(attr) || "ch".equals(attr)) {
                if (cr() == null)
                    return false;
                int min = 0, max = Integer.MAX_VALUE;
                Credit prefix = Credit.eq;
                String number = term;
                if (number.startsWith("<=")) {
                    prefix = Credit.le;
                    number = number.substring(2);
                } else if (number.startsWith(">=")) {
                    prefix = Credit.ge;
                    number = number.substring(2);
                } else if (number.startsWith("<")) {
                    prefix = Credit.lt;
                    number = number.substring(1);
                } else if (number.startsWith(">")) {
                    prefix = Credit.gt;
                    number = number.substring(1);
                } else if (number.startsWith("=")) {
                    prefix = Credit.eq;
                    number = number.substring(1);
                }
                try {
                    int a = Integer.parseInt(number);
                    switch (prefix) {
                        case eq:
                            min = max = a;
                            break; // = a
                        case le:
                            max = a;
                            break; // <= a
                        case ge:
                            min = a;
                            break; // >= a
                        case lt:
                            max = a - 1;
                            break; // < a
                        case gt:
                            min = a + 1;
                            break; // > a
                    }
                } catch (NumberFormatException e) {
                }
                if (term.contains("..")) {
                    try {
                        String a = term.substring(0, term.indexOf('.'));
                        String b = term.substring(term.indexOf("..") + 2);
                        min = Integer.parseInt(a);
                        max = Integer.parseInt(b);
                    } catch (NumberFormatException e) {
                    }
                }
                if (min == 0 && max == Integer.MAX_VALUE)
                    return true;
                if (enrollment() != null) {
                    int choice = 1;
                    for (Course course : cr().getCourses()) {
                        if (course.equals(enrollment().getCourse())) {
                            return min <= choice && choice <= max;
                        }
                        choice++;
                    }
                    return false;
                } else if (!request().isAlternative()) {
                    int choice = cr().getCourses().size();
                    return min <= choice && choice <= max;
                } else {
                    return false;
                }
            }

            if ("btb".equals(attr)) {
                if ("prefer".equalsIgnoreCase(term) || "preferred".equalsIgnoreCase(term))
                    return student().getBackToBackPreference() == BackToBackPreference.BTB_PREFERRED;
                else if ("disc".equalsIgnoreCase(term) || "discouraged".equalsIgnoreCase(term))
                    return student().getBackToBackPreference() == BackToBackPreference.BTB_DISCOURAGED;
                else
                    return student().getBackToBackPreference() == BackToBackPreference.NO_PREFERENCE;
            }

            if ("online".equals(attr)) {
                if ("prefer".equalsIgnoreCase(term) || "preferred".equalsIgnoreCase(term))
                    return student().getModalityPreference() == ModalityPreference.ONLINE_PREFERRED;
                else if ("require".equalsIgnoreCase(term) || "required".equalsIgnoreCase(term))
                    return student().getModalityPreference() == ModalityPreference.ONLINE_REQUIRED;
                else if ("disc".equalsIgnoreCase(term) || "discouraged".equalsIgnoreCase(term))
                    return student().getModalityPreference() == ModalityPreference.ONILNE_DISCOURAGED;
                else if ("no".equalsIgnoreCase(term) || "no-preference".equalsIgnoreCase(term))
                    return student().getModalityPreference() == ModalityPreference.NO_PREFERENCE;
            }

            if ("online".equals(attr) || "face-to-face".equals(attr) || "f2f".equals(attr) || "no-time".equals(attr)
                    || "has-time".equals(attr)) {
                int min = 0, max = Integer.MAX_VALUE;
                Credit prefix = Credit.eq;
                String number = term;
                if (number.startsWith("<=")) {
                    prefix = Credit.le;
                    number = number.substring(2);
                } else if (number.startsWith(">=")) {
                    prefix = Credit.ge;
                    number = number.substring(2);
                } else if (number.startsWith("<")) {
                    prefix = Credit.lt;
                    number = number.substring(1);
                } else if (number.startsWith(">")) {
                    prefix = Credit.gt;
                    number = number.substring(1);
                } else if (number.startsWith("=")) {
                    prefix = Credit.eq;
                    number = number.substring(1);
                }
                boolean perc = false;
                if (number.endsWith("%")) {
                    perc = true;
                    number = number.substring(0, number.length() - 1).trim();
                }
                try {
                    int a = Integer.parseInt(number);
                    switch (prefix) {
                        case eq:
                            min = max = a;
                            break; // = a
                        case le:
                            max = a;
                            break; // <= a
                        case ge:
                            min = a;
                            break; // >= a
                        case lt:
                            max = a - 1;
                            break; // < a
                        case gt:
                            min = a + 1;
                            break; // > a
                    }
                } catch (NumberFormatException e) {
                }
                if (term.contains("..")) {
                    try {
                        String a = term.substring(0, term.indexOf('.'));
                        String b = term.substring(term.indexOf("..") + 2);
                        min = Integer.parseInt(a);
                        max = Integer.parseInt(b);
                    } catch (NumberFormatException e) {
                    }
                }
                if (min == 0 && max == Integer.MAX_VALUE)
                    return true;
                int match = 0, total = 0;
                for (Request r : student().getRequests()) {
                    if (r instanceof CourseRequest) {
                        CourseRequest cr = (CourseRequest) r;
                        Enrollment e = iAssignment.getValue(cr);
                        if (e == null)
                            continue;
                        for (Section section : enrollment().getSections()) {
                            if ("online".equals(attr) && section.isOnline())
                                match++;
                            else if (("face-to-face".equals(attr) || "f2f".equals(attr)) && !section.isOnline())
                                match++;
                            else if ("no-time".equals(attr)
                                    && (section.getTime() == null || section.getTime().getDayCode() == 0))
                                match++;
                            else if ("has-time".equals(attr) && section.getTime() != null
                                    && section.getTime().getDayCode() != 0)
                                match++;
                            total++;
                        }
                    }
                }
                if (total == 0)
                    return false;
                if (perc) {
                    double percentage = 100.0 * match / total;
                    return min <= percentage && percentage <= max;
                } else {
                    return min <= match && match <= max;
                }
            }

            if ("overlap".equals(attr)) {
                int min = 0, max = Integer.MAX_VALUE;
                Credit prefix = Credit.eq;
                String number = term;
                if (number.startsWith("<=")) {
                    prefix = Credit.le;
                    number = number.substring(2);
                } else if (number.startsWith(">=")) {
                    prefix = Credit.ge;
                    number = number.substring(2);
                } else if (number.startsWith("<")) {
                    prefix = Credit.lt;
                    number = number.substring(1);
                } else if (number.startsWith(">")) {
                    prefix = Credit.gt;
                    number = number.substring(1);
                } else if (number.startsWith("=")) {
                    prefix = Credit.eq;
                    number = number.substring(1);
                }
                try {
                    int a = Integer.parseInt(number);
                    switch (prefix) {
                        case eq:
                            min = max = a;
                            break; // = a
                        case le:
                            max = a;
                            break; // <= a
                        case ge:
                            min = a;
                            break; // >= a
                        case lt:
                            max = a - 1;
                            break; // < a
                        case gt:
                            min = a + 1;
                            break; // > a
                    }
                } catch (NumberFormatException e) {
                }
                if (term.contains("..")) {
                    try {
                        String a = term.substring(0, term.indexOf('.'));
                        String b = term.substring(term.indexOf("..") + 2);
                        min = Integer.parseInt(a);
                        max = Integer.parseInt(b);
                    } catch (NumberFormatException e) {
                    }
                }
                int share = 0;
                for (Request r : student().getRequests()) {
                    if (r instanceof CourseRequest) {
                        CourseRequest cr = (CourseRequest) r;
                        Enrollment e = iAssignment.getValue(cr);
                        if (e == null)
                            continue;
                        for (Section section : e.getSections()) {
                            if (section.getTime() == null)
                                continue;
                            for (Request q : student().getRequests()) {
                                if (q.equals(request()))
                                    continue;
                                Enrollment otherEnrollment = iAssignment.getValue(q);
                                if (otherEnrollment != null && otherEnrollment.getCourse() != null) {
                                    for (Section otherSection : otherEnrollment.getSections()) {
                                        if (otherSection.getTime() != null
                                                && otherSection.getTime().hasIntersection(section.getTime())) {
                                            share += 5 * section.getTime().nrSharedHours(otherSection.getTime())
                                                    * section.getTime().nrSharedDays(otherSection.getTime());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return min <= share && share <= max;
            }

            if ("prefer".equals(attr)) {
                if (cr() == null)
                    return false;
                if (eq("Any Preference", term))
                    return !cr().getSelectedChoices().isEmpty() || !cr().getRequiredChoices().isEmpty();
                if (eq("Met Preference", term) || eq("Unmet Preference", term)) {
                    if (enrollment() == null) {
                        if (eq("Unmet Preference", term))
                            return !cr().getSelectedChoices().isEmpty() || !cr().getRequiredChoices().isEmpty();
                        return false;
                    }
                    if (eq("Met Preference", term))
                        return enrollment().isSelected();
                    else
                        return !enrollment().isSelected();
                }
                return false;
            }

            if ("require".equals(attr)) {
                if (cr() == null)
                    return false;
                if (eq("Any Requirement", term)) {
                    return !cr().getRequiredChoices().isEmpty();
                }
                if (eq("Met Requirement", term)) {
                    return enrollment() != null && enrollment().isRequired();
                }
                if (eq("Unmet Requirement", term)) {
                    return enrollment() != null && !enrollment().isRequired();
                }
                return false;
            }

            if ("im".equals(attr)) {
                if (cr() == null)
                    return false;
                if (enrollment() == null) {
                    for (Course course : cr().getCourses()) {
                        for (Config config : course.getOffering().getConfigs()) {
                            if (term.equals(config.getInstructionalMethodReference()))
                                return true;
                        }
                        break;
                    }
                    return false;
                } else {
                    Config config = enrollment().getConfig();
                    if (config == null)
                        return false;
                    return term.equals(config.getInstructionalMethodReference());
                }
            }

            if (enrollment() != null && enrollment().getCourse() != null) {
                for (Section section : enrollment().getSections()) {
                    if (attr == null || attr.equals("crn") || attr.equals("id") || attr.equals("externalId")
                            || attr.equals("exid") || attr.equals("name")) {
                        if (section.getName(enrollment().getCourse().getId()) != null && section
                                .getName(enrollment().getCourse().getId()).toLowerCase().startsWith(term.toLowerCase()))
                            return true;
                    }
                    if (attr == null || attr.equals("day")) {
                        if (section.getTime() == null && term.equalsIgnoreCase("none"))
                            return true;
                        if (section.getTime() != null) {
                            int day = parseDay(term);
                            if (day > 0 && (section.getTime().getDayCode() & day) == day)
                                return true;
                        }
                    }
                    if (attr == null || attr.equals("time")) {
                        if (section.getTime() == null && term.equalsIgnoreCase("none"))
                            return true;
                        if (section.getTime() != null) {
                            int start = parseStart(term);
                            if (start >= 0 && section.getTime().getStartSlot() == start)
                                return true;
                        }
                    }
                    if (attr != null && attr.equals("before")) {
                        if (section.getTime() != null) {
                            int end = parseStart(term);
                            if (end >= 0 && section.getTime().getStartSlot() + section.getTime().getLength()
                                    - section.getTime().getBreakTime() / 5 <= end)
                                return true;
                        }
                    }
                    if (attr != null && attr.equals("after")) {
                        if (section.getTime() != null) {
                            int start = parseStart(term);
                            if (start >= 0 && section.getTime().getStartSlot() >= start)
                                return true;
                        }
                    }
                    if (attr == null || attr.equals("room")) {
                        if ((section.getRooms() == null || section.getRooms().isEmpty())
                                && term.equalsIgnoreCase("none"))
                            return true;
                        if (section.getRooms() != null) {
                            for (RoomLocation r : section.getRooms()) {
                                if (has(r.getName(), term))
                                    return true;
                            }
                        }
                    }
                    if (attr == null || attr.equals("instr") || attr.equals("instructor")) {
                        if (attr != null && section.getInstructors().isEmpty() && term.equalsIgnoreCase("none"))
                            return true;
                        for (Instructor instuctor : section.getInstructors()) {
                            if (has(instuctor.getName(), term) || eq(instuctor.getExternalId(), term))
                                return true;
                            if (instuctor.getEmail() != null) {
                                String email = instuctor.getEmail();
                                if (email.indexOf('@') >= 0)
                                    email = email.substring(0, email.indexOf('@'));
                                if (eq(email, term))
                                    return true;
                            }
                        }
                    }
                }
            }
            
            if (attr == null || "name".equals(attr) || "course".equals(attr)) {
                return course() != null && (course().getSubjectArea().equalsIgnoreCase(term) || course().getCourseNumber().equalsIgnoreCase(term) || (course().getSubjectArea() + " " + course().getCourseNumber()).equalsIgnoreCase(term));
            }
            if ("title".equals(attr)) {
                return course() != null && course().getTitle().toLowerCase().contains(term.toLowerCase());
            }
            if ("subject".equals(attr)) {
                return course() != null && course().getSubjectArea().equalsIgnoreCase(term);
            }
            if ("number".equals(attr)) {
                return course() != null && course().getCourseNumber().equalsIgnoreCase(term);
            }

            return false;
        }

        private boolean eq(String name, String term) {
            if (name == null)
                return false;
            return name.equalsIgnoreCase(term);
        }

        private boolean has(String name, String term) {
            if (name == null)
                return false;
            if (eq(name, term))
                return true;
            for (String t : name.split(" |,"))
                if (t.equalsIgnoreCase(term))
                    return true;
            return false;
        }

        private boolean like(String name, String term) {
            if (name == null)
                return false;
            if (term.indexOf('%') >= 0) {
                return name.matches("(?i)" + term.replaceAll("%", ".*"));
            } else {
                return name.equalsIgnoreCase(term);
            }
        }

        public static enum Credit {
            eq, lt, gt, le, ge
        }

        public static String DAY_NAMES_CHARS[] = new String[] { "M", "T", "W", "R", "F", "S", "X" };

        private int parseDay(String token) {
            int days = 0;
            boolean found = false;
            do {
                found = false;
                for (int i = 0; i < Constants.DAY_NAMES_SHORT.length; i++) {
                    if (token.toLowerCase().startsWith(Constants.DAY_NAMES_SHORT[i].toLowerCase())) {
                        days |= Constants.DAY_CODES[i];
                        token = token.substring(Constants.DAY_NAMES_SHORT[i].length());
                        while (token.startsWith(" "))
                            token = token.substring(1);
                        found = true;
                    }
                }
                for (int i = 0; i < DAY_NAMES_CHARS.length; i++) {
                    if (token.toLowerCase().startsWith(DAY_NAMES_CHARS[i].toLowerCase())) {
                        days |= Constants.DAY_CODES[i];
                        token = token.substring(DAY_NAMES_CHARS[i].length());
                        while (token.startsWith(" "))
                            token = token.substring(1);
                        found = true;
                    }
                }
            } while (found);
            return (token.isEmpty() ? days : 0);
        }

        private int parseStart(String token) {
            int startHour = 0, startMin = 0;
            String number = "";
            while (!token.isEmpty() && token.charAt(0) >= '0' && token.charAt(0) <= '9') {
                number += token.substring(0, 1);
                token = token.substring(1);
            }
            if (number.isEmpty())
                return -1;
            if (number.length() > 2) {
                startHour = Integer.parseInt(number) / 100;
                startMin = Integer.parseInt(number) % 100;
            } else {
                startHour = Integer.parseInt(number);
            }
            while (token.startsWith(" "))
                token = token.substring(1);
            if (token.startsWith(":")) {
                token = token.substring(1);
                while (token.startsWith(" "))
                    token = token.substring(1);
                number = "";
                while (!token.isEmpty() && token.charAt(0) >= '0' && token.charAt(0) <= '9') {
                    number += token.substring(0, 1);
                    token = token.substring(1);
                }
                if (number.isEmpty())
                    return -1;
                startMin = Integer.parseInt(number);
            }
            while (token.startsWith(" "))
                token = token.substring(1);
            boolean hasAmOrPm = false;
            if (token.toLowerCase().startsWith("am")) {
                token = token.substring(2);
                hasAmOrPm = true;
            }
            if (token.toLowerCase().startsWith("a")) {
                token = token.substring(1);
                hasAmOrPm = true;
            }
            if (token.toLowerCase().startsWith("pm")) {
                token = token.substring(2);
                hasAmOrPm = true;
                if (startHour < 12)
                    startHour += 12;
            }
            if (token.toLowerCase().startsWith("p")) {
                token = token.substring(1);
                hasAmOrPm = true;
                if (startHour < 12)
                    startHour += 12;
            }
            if (startHour < 7 && !hasAmOrPm)
                startHour += 12;
            if (startMin % 5 != 0)
                startMin = 5 * ((startMin + 2) / 5);
            if (startHour == 7 && startMin == 0 && !hasAmOrPm)
                startHour += 12;
            return (60 * startHour + startMin) / 5;
        }
    }
    
    public static class CourseMatcher implements Query.TermMatcher {
        private Course iCourse;
        
        public CourseMatcher(Course course) {
                iCourse = course;
        }

        public Course course() { return iCourse; }
        
        @Override
        public boolean match(String attr, String term) {
            if (attr == null || "name".equals(attr) || "course".equals(attr)) {
                return course() != null && (course().getSubjectArea().equalsIgnoreCase(term) || course().getCourseNumber().equalsIgnoreCase(term) || (course().getSubjectArea() + " " + course().getCourseNumber()).equalsIgnoreCase(term));
            }
            if ("title".equals(attr)) {
                return course() != null && course().getTitle().toLowerCase().contains(term.toLowerCase());
            }
            if ("subject".equals(attr)) {
                return course() != null && course().getSubjectArea().equalsIgnoreCase(term);
            }
            if ("number".equals(attr)) {
                return course() != null && course().getCourseNumber().equalsIgnoreCase(term);
            }
            return true;
        }
    }
}
