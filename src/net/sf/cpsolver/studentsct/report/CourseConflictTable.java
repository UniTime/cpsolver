package net.sf.cpsolver.studentsct.report;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.constraint.SectionLimit;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;

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
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 */

public class CourseConflictTable {
    private static org.apache.log4j.Logger sLog = org.apache.log4j.Logger.getLogger(CourseConflictTable.class);
    private static DecimalFormat sDF = new DecimalFormat("0.000");

    private StudentSectioningModel iModel = null;

    /**
     * Constructor
     * 
     * @param model
     *            student sectioning model
     */
    public CourseConflictTable(StudentSectioningModel model) {
        iModel = model;
    }

    /** Return student sectioning model */
    public StudentSectioningModel getModel() {
        return iModel;
    }

    /**
     * True, if there is no pair of enrollments of r1 and r2 that is not in a
     * hard conflict
     */
    private boolean areInHardConfict(Request r1, Request r2) {
        for (Enrollment e1 : r1.values()) {
            for (Enrollment e2 : r2.values()) {
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
    private Set<String> explanations(Enrollment enrl, Enrollment conflict) {
        Set<String> expl = new HashSet<String>();
        for (Section s1 : enrl.getSections()) {
            for (Section s2 : conflict.getSections()) {
                if (s1.isOverlapping(s2))
                    expl.add(s1.getSubpart().getName() + " " + s1.getTime().getLongName() + " vs "
                            + s2.getSubpart().getName() + " " + s2.getTime().getLongName());
            }
        }
        for (Section s1 : enrl.getSections()) {
            if (conflict.getAssignments().contains(s1)
                    && SectionLimit.getEnrollmentWeight(s1, enrl.getRequest()) > s1.getLimit()) {
                expl.add(s1.getSubpart().getName() + " n/a");
            }
        }
        return expl;
    }

    /**
     * Create report
     * 
     * @param includeLastLikeStudents
     *            true, if last-like students should be included (i.e.,
     *            {@link Student#isDummy()} is true)
     * @param includeRealStudents
     *            true, if real students should be included (i.e.,
     *            {@link Student#isDummy()} is false)
     * @return report as comma separated text file
     */
    @SuppressWarnings("unchecked")
    public CSVFile createTable(boolean includeLastLikeStudents, boolean includeRealStudents) {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVFile.CSVField[] { new CSVFile.CSVField("UnasgnCrs"), new CSVFile.CSVField("ConflCrs"),
                new CSVFile.CSVField("NrStud"), new CSVFile.CSVField("StudWeight"), new CSVFile.CSVField("NoAlt"),
                new CSVFile.CSVField("Reason") });
        Hashtable<Course, Hashtable<Course, Object[]>> unassignedCourseTable = new Hashtable<Course, Hashtable<Course, Object[]>>();
        for (Request request : new ArrayList<Request>(getModel().unassignedVariables())) {
            if (request.getStudent().isDummy() && !includeLastLikeStudents)
                continue;
            if (!request.getStudent().isDummy() && !includeRealStudents)
                continue;
            if (request instanceof CourseRequest) {
                CourseRequest courseRequest = (CourseRequest) request;
                if (courseRequest.getStudent().isComplete())
                    continue;

                List<Enrollment> values = courseRequest.values();
                SectionLimit limitConstraint = new SectionLimit(new DataProperties());
                List<Enrollment> availableValues = new ArrayList<Enrollment>(values.size());
                for (Enrollment enrollment : values) {
                    if (!limitConstraint.inConflict(enrollment))
                        availableValues.add(enrollment);
                }

                if (availableValues.isEmpty()) {
                    Course course = courseRequest.getCourses().get(0);
                    Hashtable<Course, Object[]> conflictCourseTable = unassignedCourseTable.get(course);
                    if (conflictCourseTable == null) {
                        conflictCourseTable = new Hashtable<Course, Object[]>();
                        unassignedCourseTable.put(course, conflictCourseTable);
                    }
                    Object[] weight = conflictCourseTable.get(course);
                    double nrStud = (weight == null ? 0.0 : ((Double) weight[0]).doubleValue()) + 1.0;
                    double nrStudW = (weight == null ? 0.0 : ((Double) weight[1]).doubleValue()) + request.getWeight();
                    boolean noAlt = (weight == null ? true : ((Boolean) weight[2]).booleanValue());
                    HashSet<String> expl = (weight == null ? new HashSet<String>() : (HashSet<String>) weight[3]);
                    expl.add(course.getName() + " n/a");
                    conflictCourseTable.put(course, new Object[] { new Double(nrStud), new Double(nrStudW),
                            new Boolean(noAlt), expl });
                }

                for (Enrollment enrollment : availableValues) {
                    Set<Enrollment> conflicts = getModel().conflictValues(enrollment);
                    if (conflicts.isEmpty()) {
                        sLog.warn("Request " + courseRequest + " of student " + courseRequest.getStudent()
                                + " not assigned, however, no conflicts were returned.");
                        courseRequest.assign(0, enrollment);
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
                        sLog.warn("Course not found for request " + courseRequest + " of student "
                                + courseRequest.getStudent() + ".");
                        continue;
                    }
                    Hashtable<Course, Object[]> conflictCourseTable = unassignedCourseTable.get(course);
                    if (conflictCourseTable == null) {
                        conflictCourseTable = new Hashtable<Course, Object[]>();
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
                            boolean noAlt = (weight == null ? areInHardConfict(request, conflict.getRequest())
                                    : ((Boolean) weight[2]).booleanValue());
                            HashSet<String> expl = (weight == null ? new HashSet<String>()
                                    : (HashSet<String>) weight[3]);
                            expl.addAll(explanations(enrollment, conflict));
                            conflictCourseTable.put(conflictCourse, new Object[] { new Double(nrStud),
                                    new Double(nrStudW), new Boolean(noAlt), expl });
                        }
                    }
                }
            }
        }
        for (Map.Entry<Course, Hashtable<Course, Object[]>> entry : unassignedCourseTable.entrySet()) {
            Course unassignedCourse = entry.getKey();
            Hashtable<Course, Object[]> conflictCourseTable = entry.getValue();
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
