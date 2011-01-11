package net.sf.cpsolver.studentsct.report;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.DistanceMetric;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.extension.DistanceConflict;
import net.sf.cpsolver.studentsct.extension.DistanceConflict.Conflict;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * This class lists distance student conflicts in a {@link CSVFile} comma
 * separated text file. Two sections that are attended by the same student are
 * considered in a distance conflict if they are back-to-back taught in
 * locations that are two far away. See {@link DistanceConflict} for more
 * details. <br>
 * <br>
 * 
 * Each line represent a pair if courses that have one or more distance
 * conflicts in between (columns Course1, Course2), column NrStud displays the
 * number of student distance conflicts (weighted by requests weights), and
 * column AvgDist displays the average distance for all the distance conflicts
 * between these two courses. The column NoAlt is Y when every possible
 * enrollment of the first course is either overlapping or there is a distance
 * conflict with every possible enrollment of the second course (it is N
 * otherwise) and a column Reason which lists the sections that are involved in
 * a distance conflict.
 * 
 * <br>
 * <br>
 * 
 * Usage: new DistanceConflictTable(model),createTable(true, true).save(aFile);
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
public class DistanceConflictTable {
    private static org.apache.log4j.Logger sLog = org.apache.log4j.Logger.getLogger(DistanceConflictTable.class);
    private static DecimalFormat sDF = new DecimalFormat("0.000");

    private StudentSectioningModel iModel = null;
    private DistanceConflict iDC = null;
    private DistanceMetric iDM = null;

    /**
     * Constructor
     * 
     * @param model
     *            student sectioning model
     */
    public DistanceConflictTable(StudentSectioningModel model) {
        iModel = model;
        iDC = model.getDistanceConflict();
        if (iDC == null)
            iDC = new DistanceConflict(null, model.getProperties());
        iDM = new DistanceMetric(model.getProperties());
    }

    /** Return student sectioning model */
    public StudentSectioningModel getModel() {
        return iModel;
    }

    /**
     * True, if there is no pair of enrollments of r1 and r2 that is not in a
     * hard conflict and without a distance conflict
     */
    private boolean areInHardConfict(Request r1, Request r2) {
        for (Enrollment e1 : r1.values()) {
            for (Enrollment e2 : r2.values()) {
                if (!e1.isOverlapping(e2) && iDC.nrConflicts(e1, e2) == 0)
                    return false;
            }
        }
        return true;
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
        csv.setHeader(new CSVFile.CSVField[] { new CSVFile.CSVField("Course1"), new CSVFile.CSVField("Course2"),
                new CSVFile.CSVField("NrStud"), new CSVFile.CSVField("AvgDist"),
                new CSVFile.CSVField("NoAlt"), new CSVFile.CSVField("Reason") });
        Set<Conflict> confs = iDC.computeAllConflicts();
        HashMap<Course, HashMap<Course, Object[]>> distConfTable = new HashMap<Course, HashMap<Course, Object[]>>();
        for (Conflict conflict : confs) {
            if (conflict.getStudent().isDummy() && !includeLastLikeStudents)
                continue;
            if (!conflict.getStudent().isDummy() && !includeRealStudents)
                continue;
            Section s1 = conflict.getS1(), s2 = conflict.getS2();
            Course c1 = null, c2 = null;
            Request r1 = null, r2 = null;
            for (Request request : conflict.getStudent().getRequests()) {
                Enrollment enrollment = request.getAssignment();
                if (enrollment == null || !enrollment.isCourseRequest())
                    continue;
                if (c1 == null && enrollment.getAssignments().contains(s1)) {
                    c1 = enrollment.getConfig().getOffering().getCourse(conflict.getStudent());
                    r1 = request;
                }
                if (c2 == null && enrollment.getAssignments().contains(s2)) {
                    c2 = enrollment.getConfig().getOffering().getCourse(conflict.getStudent());
                    r2 = request;
                }
            }
            if (c1 == null) {
                sLog.error("Unable to find a course for " + s1);
                continue;
            }
            if (c2 == null) {
                sLog.error("Unable to find a course for " + s2);
                continue;
            }
            if (c1.getName().compareTo(c2.getName()) > 0) {
                Course x = c1;
                c1 = c2;
                c2 = x;
                Section y = s1;
                s1 = s2;
                s2 = y;
            }
            if (c1.equals(c2) && s1.getName().compareTo(s2.getName()) > 0) {
                Section y = s1;
                s1 = s2;
                s2 = y;
            }
            HashMap<Course, Object[]> firstCourseTable = distConfTable.get(c1);
            if (firstCourseTable == null) {
                firstCourseTable = new HashMap<Course, Object[]>();
                distConfTable.put(c1, firstCourseTable);
            }
            Object[] secondCourseTable = firstCourseTable.get(c2);
            double nrStud = (secondCourseTable == null ? 0.0 : ((Double) secondCourseTable[0]).doubleValue()) + 1.0;
            double dist = (secondCourseTable == null ? 0.0 : ((Double) secondCourseTable[1]).doubleValue()) + (conflict.getDistance(iDM));
            boolean hard = (secondCourseTable == null ? areInHardConfict(r1, r2) : ((Boolean) secondCourseTable[2]).booleanValue());
            HashSet<String> expl = (HashSet<String>) (secondCourseTable == null ? null : secondCourseTable[3]);
            if (expl == null)
                expl = new HashSet<String>();
            expl.add(s1.getSubpart().getName() + " " + s1.getTime().getLongName() + " "
                    + s1.getPlacement().getRoomName(",") + " vs " + s2.getSubpart().getName() + " "
                    + s2.getTime().getLongName() + " " + s2.getPlacement().getRoomName(","));
            firstCourseTable.put(c2, new Object[] { new Double(nrStud), new Double(dist), new Boolean(hard), expl });
        }
        for (Map.Entry<Course, HashMap<Course, Object[]>> entry : distConfTable.entrySet()) {
            Course c1 = entry.getKey();
            HashMap<Course, Object[]> firstCourseTable = entry.getValue();
            for (Map.Entry<Course, Object[]> entry2 : firstCourseTable.entrySet()) {
                Course c2 = entry2.getKey();
                Object[] secondCourseTable = entry2.getValue();
                HashSet<String> expl = (HashSet<String>) secondCourseTable[3];
                String explStr = "";
                for (Iterator<String> k = new TreeSet<String>(expl).iterator(); k.hasNext();)
                    explStr += k.next() + (k.hasNext() ? "\n" : "");
                double nrStud = ((Double) secondCourseTable[0]).doubleValue();
                double dist = ((Double) secondCourseTable[1]).doubleValue() / nrStud;
                csv.addLine(new CSVFile.CSVField[] { new CSVFile.CSVField(c1.getName()),
                        new CSVFile.CSVField(c2.getName()), new CSVFile.CSVField(sDF.format(nrStud)),
                        new CSVFile.CSVField(sDF.format(dist)),
                        new CSVFile.CSVField(((Boolean) secondCourseTable[2]).booleanValue() ? "Y" : "N"),
                        new CSVFile.CSVField(explStr) });
            }
        }
        if (csv.getLines() != null)
            Collections.sort(csv.getLines(), new Comparator<CSVFile.CSVLine>() {
                public int compare(CSVFile.CSVLine l1, CSVFile.CSVLine l2) {
                    // int cmp =
                    // l2.getField(4).toString().compareTo(l1.getField(4).toString());
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
