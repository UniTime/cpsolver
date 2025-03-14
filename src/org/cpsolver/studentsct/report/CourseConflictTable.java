package org.cpsolver.studentsct.report;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.ifs.util.CSVFile;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.constraint.ConfigLimit;
import org.cpsolver.studentsct.constraint.CourseLimit;
import org.cpsolver.studentsct.constraint.SectionLimit;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;


/**
 * This class lists conflicting courses in a {@link CSVFile} comma separated
 * text file. <br>
 * <br>
 * 
 * Each line represent a course that has some unassigned course requests (column
 * UnasgnCrs), course that was conflicting with that course (column ConflCrs),
 * and number of students with that conflict. So, for instance if there was a
 * student which cannot attend course A with weight 1.5 (e.g., 10 last-like
 * students projected to 15), and when A had two possible assignments for that
 * student, one conflicting with C (assigned to that student) and the other with
 * D, then 0.75 (1.5/2) was added to rows A, B and A, C. The column NoAlt is Y
 * when every possible enrollment of the first course is overlapping with every
 * possible enrollment of the second course (it is N otherwise) and a column
 * Reason which lists the overlapping sections.
 * 
 * <br>
 * <br>
 * 
 * Usage: new CourseConflictTable(model),createTable(true, true).save(aFile);
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

public class CourseConflictTable extends AbstractStudentSectioningReport {
    private static org.apache.logging.log4j.Logger sLog = org.apache.logging.log4j.LogManager.getLogger(CourseConflictTable.class);
    private static DecimalFormat sDF = new DecimalFormat("0.000");

    /**
     * Constructor
     * 
     * @param model
     *            student sectioning model
     */
    public CourseConflictTable(StudentSectioningModel model) {
        super(model);
    }

    /**
     * True, if there is no pair of enrollments of r1 and r2 that is not in a
     * hard conflict
     */
    private boolean areInHardConfict(Assignment<Request, Enrollment> assignment, Request r1, Request r2) {
        for (Enrollment e1 : r1.values(assignment)) {
            for (Enrollment e2 : r2.values(assignment)) {
                if (!e1.isOverlapping(e2))
                    return false;
            }
        }
        return true;
    }

    /**
     * Return a set of explanations (Strings) for conflicts between the given
     * enrollments
     * 
     * @param enrl
     *            an enrollment
     * @param conflict
     *            an enrollment conflicting with enrl
     * @return a set of explanations, (e.g., AB 101 Lec 1 MWF 7:30 - 8:20 vs AB
     *         201 Lec 1 F 7:30 - 9:20)
     */
    private Set<String> explanations(Assignment<Request, Enrollment> assignment, Enrollment enrl, Enrollment conflict, boolean useAmPm) {
        Set<String> expl = new HashSet<String>();
        for (Section s1 : enrl.getSections()) {
            for (Section s2 : conflict.getSections()) {
                if (s1.isOverlapping(s2))
                    expl.add(s1.getSubpart().getName() + " " + s1.getTime().getLongName(useAmPm) + " vs "
                            + s2.getSubpart().getName() + " " + s2.getTime().getLongName(useAmPm));
            }
        }
        for (Section s1 : enrl.getSections()) {
            if (conflict.getAssignments().contains(s1)
                    && SectionLimit.getEnrollmentWeight(assignment, s1, enrl.getRequest()) > s1.getLimit()) {
                expl.add(s1.getSubpart().getName() + " n/a");
            }
        }
        if (enrl.getConfig() != null && enrl.getConfig().equals(conflict.getConfig())) {
            if (ConfigLimit.getEnrollmentWeight(assignment, enrl.getConfig(), enrl.getRequest()) > enrl.getConfig().getLimit()) {
                expl.add(enrl.getConfig().getName() + " n/a");
            }
        }
        if (enrl.getCourse() != null && enrl.getCourse().equals(conflict.getCourse())) {
            if (CourseLimit.getEnrollmentWeight(assignment, enrl.getCourse(), enrl.getRequest()) > enrl.getCourse().getLimit()) {
                expl.add(enrl.getCourse().getName() + " n/a");
            }
        }
        return expl;
    }

    /**
     * Create report
     * 
     * @param assignment current assignment
     * @return report as comma separated text file
     */
    @SuppressWarnings("unchecked")
    @Override
    public CSVFile createTable(Assignment<Request, Enrollment> assignment, DataProperties properties) {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVFile.CSVField[] { new CSVFile.CSVField("UnasgnCrs"), new CSVFile.CSVField("ConflCrs"),
                new CSVFile.CSVField("NrStud"), new CSVFile.CSVField("StudWeight"), new CSVFile.CSVField("NoAlt"),
                new CSVFile.CSVField("Reason") });
        HashMap<Course, HashMap<Course, Object[]>> unassignedCourseTable = new HashMap<Course, HashMap<Course, Object[]>>();
        for (Request request : new ArrayList<Request>(getModel().unassignedVariables(assignment))) {
            if (!matches(request)) continue;
            if (request instanceof CourseRequest) {
                CourseRequest courseRequest = (CourseRequest) request;
                if (courseRequest.getStudent().isComplete(assignment))
                    continue;

                List<Enrollment> values = courseRequest.values(assignment);
                SectionLimit limitConstraint = null;
                for (GlobalConstraint<Request, Enrollment> c: getModel().globalConstraints()) {
                    if (c instanceof SectionLimit) {
                        limitConstraint = (SectionLimit)c;
                        break;
                    }
                }
                if (limitConstraint == null) {
                    limitConstraint = new SectionLimit(new DataProperties());
                    limitConstraint.setModel(getModel());
                }
                List<Enrollment> availableValues = new ArrayList<Enrollment>(values.size());
                for (Enrollment enrollment : values) {
                    if (!limitConstraint.inConflict(assignment, enrollment))
                        availableValues.add(enrollment);
                }

                if (availableValues.isEmpty()) {
                    Course course = courseRequest.getCourses().get(0);
                    HashMap<Course, Object[]> conflictCourseTable = unassignedCourseTable.get(course);
                    if (conflictCourseTable == null) {
                        conflictCourseTable = new HashMap<Course, Object[]>();
                        unassignedCourseTable.put(course, conflictCourseTable);
                    }
                    Object[] weight = conflictCourseTable.get(course);
                    double nrStud = (weight == null ? 0.0 : ((Double) weight[0]).doubleValue()) + 1.0;
                    double nrStudW = (weight == null ? 0.0 : ((Double) weight[1]).doubleValue()) + request.getWeight();
                    boolean noAlt = (weight == null ? true : ((Boolean) weight[2]).booleanValue());
                    HashSet<String> expl = (weight == null ? new HashSet<String>() : (HashSet<String>) weight[3]);
                    expl.add(course.getName() + " n/a");
                    conflictCourseTable.put(course, new Object[] { Double.valueOf(nrStud), Double.valueOf(nrStudW),
                            Boolean.valueOf(noAlt), expl });
                }

                for (Enrollment enrollment : availableValues) {
                    Set<Enrollment> conflicts = getModel().conflictValues(assignment, enrollment);
                    if (conflicts.isEmpty()) {
                        sLog.warn("Request " + courseRequest + " of student " + courseRequest.getStudent() + " not assigned, however, no conflicts were returned.");
                        assignment.assign(0, enrollment);
                        break;
                    }
                    Course course = null;
                    for (Course c : courseRequest.getCourses()) {
                        if (c.getOffering().equals(enrollment.getConfig().getOffering())) {
                            course = c;
                            break;
                        }
                    }
                    if (course == null) {
                        sLog.warn("Course not found for request " + courseRequest + " of student " + courseRequest.getStudent() + ".");
                        continue;
                    }
                    HashMap<Course, Object[]> conflictCourseTable = unassignedCourseTable.get(course);
                    if (conflictCourseTable == null) {
                        conflictCourseTable = new HashMap<Course, Object[]>();
                        unassignedCourseTable.put(course, conflictCourseTable);
                    }
                    for (Enrollment conflict : conflicts) {
                        if (conflict.variable() instanceof CourseRequest) {
                            CourseRequest conflictCourseRequest = (CourseRequest) conflict.variable();
                            Course conflictCourse = null;
                            for (Course c : conflictCourseRequest.getCourses()) {
                                if (c.getOffering().equals(conflict.getConfig().getOffering())) {
                                    conflictCourse = c;
                                    break;
                                }
                            }
                            if (conflictCourse == null) {
                                sLog.warn("Course not found for request " + conflictCourseRequest + " of student "
                                        + conflictCourseRequest.getStudent() + ".");
                                continue;
                            }
                            double weightThisConflict = request.getWeight() / availableValues.size() / conflicts.size();
                            double partThisConflict = 1.0 / availableValues.size() / conflicts.size();
                            Object[] weight = conflictCourseTable.get(conflictCourse);
                            double nrStud = (weight == null ? 0.0 : ((Double) weight[0]).doubleValue())
                                    + partThisConflict;
                            double nrStudW = (weight == null ? 0.0 : ((Double) weight[1]).doubleValue())
                                    + weightThisConflict;
                            boolean noAlt = (weight == null ? areInHardConfict(assignment, request, conflict.getRequest())
                                    : ((Boolean) weight[2]).booleanValue());
                            HashSet<String> expl = (weight == null ? new HashSet<String>()
                                    : (HashSet<String>) weight[3]);
                            expl.addAll(explanations(assignment, enrollment, conflict, isUseAmPm()));
                            conflictCourseTable.put(conflictCourse, new Object[] { Double.valueOf(nrStud),
                                    Double.valueOf(nrStudW), Boolean.valueOf(noAlt), expl });
                        }
                    }
                }
            }
        }
        for (Map.Entry<Course, HashMap<Course, Object[]>> entry : unassignedCourseTable.entrySet()) {
            Course unassignedCourse = entry.getKey();
            HashMap<Course, Object[]> conflictCourseTable = entry.getValue();
            for (Map.Entry<Course, Object[]> entry2 : conflictCourseTable.entrySet()) {
                Course conflictCourse = entry2.getKey();
                Object[] weight = entry2.getValue();
                HashSet<String> expl = (HashSet<String>) weight[3];
                String explStr = "";
                for (Iterator<String> k = new TreeSet<String>(expl).iterator(); k.hasNext();)
                    explStr += k.next() + (k.hasNext() ? "\n" : "");
                csv.addLine(new CSVFile.CSVField[] { new CSVFile.CSVField(unassignedCourse.getName()),
                        new CSVFile.CSVField(conflictCourse.getName()), new CSVFile.CSVField(sDF.format(weight[0])),
                        new CSVFile.CSVField(sDF.format(weight[1])),
                        new CSVFile.CSVField(((Boolean) weight[2]).booleanValue() ? "Y" : "N"),
                        new CSVFile.CSVField(explStr) });
            }
        }
        if (csv.getLines() != null)
            Collections.sort(csv.getLines(), new Comparator<CSVFile.CSVLine>() {
                @Override
                public int compare(CSVFile.CSVLine l1, CSVFile.CSVLine l2) {
                    // int cmp =
                    // l2.getField(3).toString().compareTo(l1.getField(3).toString());
                    // if (cmp!=0) return cmp;
                    int cmp = Double.compare(l2.getField(2).toDouble(), l1.getField(2).toDouble());
                    if (cmp != 0)
                        return cmp;
                    cmp = l1.getField(0).toString().compareTo(l2.getField(0).toString());
                    if (cmp != 0)
                        return cmp;
                    return l1.getField(1).toString().compareTo(l2.getField(1).toString());
                }
            });
        return csv;
    }
}
