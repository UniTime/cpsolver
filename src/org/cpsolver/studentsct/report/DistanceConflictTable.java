package org.cpsolver.studentsct.report;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.CSVFile;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.DistanceMetric;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.extension.DistanceConflict;
import org.cpsolver.studentsct.extension.DistanceConflict.Conflict;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;


/**
 * This class lists distance student conflicts in a {@link CSVFile} comma
 * separated text file. Two sections that are attended by the same student are
 * considered in a distance conflict if they are back-to-back taught in
 * locations that are two far away. See {@link DistanceConflict} for more
 * details. <br>
 * <br>
 * 
 * Each line represent a pair if classes that are in a distance conflict and have
 * one or more students in common.
 * 
 * <br>
 * <br>
 * 
 * Usage: new DistanceConflictTable(model),createTable(true, true).save(aFile);
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
public class DistanceConflictTable extends AbstractStudentSectioningReport {
    private static org.apache.logging.log4j.Logger sLog = org.apache.logging.log4j.LogManager.getLogger(DistanceConflictTable.class);
    private static DecimalFormat sDF1 = new DecimalFormat("0.####");
    private static DecimalFormat sDF2 = new DecimalFormat("0.0000");

    private DistanceConflict iDC = null;
    private DistanceMetric iDM = null;

    /**
     * Constructor
     * 
     * @param model
     *            student sectioning model
     */
    public DistanceConflictTable(StudentSectioningModel model) {
        super(model);
        iDC = model.getDistanceConflict();
        if (iDC == null) {
            iDM = new DistanceMetric(model.getProperties());
            iDC = new DistanceConflict(iDM, model.getProperties());
        } else {
            iDM = iDC.getDistanceMetric();
        }
    }

    /**
     * Create report
     * 
     * @param assignment current assignment
     * @return report as comma separated text file
     */
    @Override
    public CSVFile createTable(Assignment<Request, Enrollment> assignment, DataProperties properties) {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVFile.CSVField[] { new CSVFile.CSVField("Course"), new CSVFile.CSVField("Total\nConflicts"),
                new CSVFile.CSVField("Class"), new CSVFile.CSVField("Meeting Time"), new CSVFile.CSVField("Room"),
                new CSVFile.CSVField("Distance\nConflicts"), new CSVFile.CSVField("% of Total\nConflicts"),
                new CSVFile.CSVField("Conflicting\nClass"), new CSVFile.CSVField("Conflicting\nMeeting Time"), new CSVFile.CSVField("Conflicting\nRoom"),
                new CSVFile.CSVField("Distance [m]"), new CSVFile.CSVField("Distance [min]"), new CSVFile.CSVField("Joined\nConflicts"), new CSVFile.CSVField("% of Total\nConflicts")
                });
        
        Set<Conflict> confs = new HashSet<Conflict>();
        for (Request r1 : getModel().variables()) {
            Enrollment e1 = assignment.getValue(r1);
            if (e1 == null || !(r1 instanceof CourseRequest))
                continue;
            confs.addAll(iDC.conflicts(e1));
            for (Request r2 : r1.getStudent().getRequests()) {
                Enrollment e2 = assignment.getValue(r2);
                if (e2 == null || r1.getId() >= r2.getId() || !(r2 instanceof CourseRequest))
                    continue;
                confs.addAll(iDC.conflicts(e1, e2));
            }
        }
        
        HashMap<Course, Set<Long>> totals = new HashMap<Course, Set<Long>>();
        HashMap<CourseSection, Map<CourseSection, Double>> conflictingPairs = new HashMap<CourseSection, Map<CourseSection,Double>>();
        HashMap<CourseSection, Set<Long>> sectionOverlaps = new HashMap<CourseSection, Set<Long>>();        
        
        for (Conflict conflict : confs) {
            if (!matches(conflict.getR1(), conflict.getE1())) continue;
            Section s1 = conflict.getS1(), s2 = conflict.getS2();
            Course c1 = null, c2 = null;
            Request r1 = null, r2 = null;
            for (Request request : conflict.getStudent().getRequests()) {
                Enrollment enrollment = assignment.getValue(request);
                if (enrollment == null || !enrollment.isCourseRequest()) continue;
                if (c1 == null && enrollment.getAssignments().contains(s1)) {
                    c1 = enrollment.getCourse();
                    r1 = request;
                    Set<Long> total = totals.get(enrollment.getCourse());
                    if (total == null) {
                        total = new HashSet<Long>();
                        totals.put(enrollment.getCourse(), total);
                    }
                    total.add(enrollment.getStudent().getId());
                }
                if (c2 == null && enrollment.getAssignments().contains(s2)) {
                    c2 = enrollment.getCourse();
                    r2 = request;
                    Set<Long> total = totals.get(enrollment.getCourse());
                    if (total == null) {
                        total = new HashSet<Long>();
                        totals.put(enrollment.getCourse(), total);
                    }
                    total.add(enrollment.getStudent().getId());
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
            CourseSection a = new CourseSection(c1, s1);
            CourseSection b = new CourseSection(c2, s2);
            
            Set<Long> total = sectionOverlaps.get(a);
            if (total == null) {
                total = new HashSet<Long>();
                sectionOverlaps.put(a, total);
            }
            total.add(r1.getStudent().getId());
            Map<CourseSection, Double> pair = conflictingPairs.get(a);
            if (pair == null) {
                pair = new HashMap<CourseSection, Double>();
                conflictingPairs.put(a, pair);
            }
            Double prev = pair.get(b);
            pair.put(b, r2.getWeight() + (prev == null ? 0.0 : prev.doubleValue()));
            
            total = sectionOverlaps.get(b);
            if (total == null) {
                total = new HashSet<Long>();
                sectionOverlaps.put(b, total);
            }
            total.add(r2.getStudent().getId());
            pair = conflictingPairs.get(b);
            if (pair == null) {
                pair = new HashMap<CourseSection, Double>();
                conflictingPairs.put(b, pair);
            }
            prev = pair.get(a);
            pair.put(a, r1.getWeight() + (prev == null ? 0.0 : prev.doubleValue()));
        }
        
        Comparator<Course> courseComparator = new Comparator<Course>() {
            @Override
            public int compare(Course a, Course b) {
                int cmp = a.getName().compareTo(b.getName());
                if (cmp != 0) return cmp;
                return a.getId() < b.getId() ? -1 : a.getId() == b.getId() ? 0 : 1;
            }
        };
        Comparator<Section> sectionComparator = new Comparator<Section>() {
            @Override
            public int compare(Section a, Section b) {
                int cmp = a.getSubpart().getConfig().getOffering().getName().compareTo(b.getSubpart().getConfig().getOffering().getName());
                if (cmp != 0) return cmp;
                cmp = a.getSubpart().getInstructionalType().compareTo(b.getSubpart().getInstructionalType());
                // if (cmp != 0) return cmp;
                // cmp = a.getName().compareTo(b.getName());
                if (cmp != 0) return cmp;
                return a.getId() < b.getId() ? -1 : a.getId() == b.getId() ? 0 : 1;
            }
        };
        
        TreeSet<Course> courses = new TreeSet<Course>(courseComparator);
        courses.addAll(totals.keySet());
        for (Course course: courses) {
            Set<Long> total = totals.get(course);
            
            TreeSet<Section> sections = new TreeSet<Section>(sectionComparator);
            for (Map.Entry<CourseSection, Set<Long>> entry: sectionOverlaps.entrySet())
                if (course.equals(entry.getKey().getCourse()))
                    sections.add(entry.getKey().getSection());
            
            boolean firstCourse = true;
            for (Section section: sections) {
                Set<Long> sectionOverlap = sectionOverlaps.get(new CourseSection(course, section));
                Map<CourseSection, Double> pair = conflictingPairs.get(new CourseSection(course, section));
                boolean firstClass = true;
                
                String rooms = "";
                if (section.getRooms() != null)
                    for (RoomLocation r: section.getRooms()) {
                        if (!rooms.isEmpty()) rooms += "\n";
                        rooms += r.getName();
                    }

                for (CourseSection other: new TreeSet<CourseSection>(pair.keySet())) {
                    List<CSVFile.CSVField> line = new ArrayList<CSVFile.CSVField>();
                    line.add(new CSVFile.CSVField(firstCourse && firstClass ? course.getName() : ""));
                    line.add(new CSVFile.CSVField(firstCourse && firstClass ? total.size() : ""));
                    
                    line.add(new CSVFile.CSVField(firstClass ? section.getSubpart().getName() + " " + section.getName(course.getId()): ""));
                    line.add(new CSVFile.CSVField(firstClass ? section.getTime() == null ? "" : section.getTime().getDayHeader() + " " + section.getTime().getStartTimeHeader(isUseAmPm()) + " - " + section.getTime().getEndTimeHeader(isUseAmPm()): ""));
                        
                    line.add(new CSVFile.CSVField(firstClass ? rooms : ""));
                    
                    line.add(new CSVFile.CSVField(firstClass && sectionOverlap != null ? String.valueOf(sectionOverlap.size()): ""));
                    line.add(new CSVFile.CSVField(firstClass && sectionOverlap != null ? sDF2.format(((double)sectionOverlap.size()) / total.size()) : ""));

                    line.add(new CSVFile.CSVField(other.getCourse().getName() + " " + other.getSection().getSubpart().getName() + " " + other.getSection().getName(other.getCourse().getId())));
                    line.add(new CSVFile.CSVField(other.getSection().getTime().getDayHeader() + " " + other.getSection().getTime().getStartTimeHeader(isUseAmPm()) + " - " + other.getSection().getTime().getEndTimeHeader(isUseAmPm())));
                    
                    String or = "";
                    if (other.getSection().getRooms() != null)
                        for (RoomLocation r: other.getSection().getRooms()) {
                            if (!or.isEmpty()) or += "\n";
                            or += r.getName();
                        }
                    line.add(new CSVFile.CSVField(or));
                    
                    line.add(new CSVFile.CSVField(sDF2.format(Placement.getDistanceInMeters(iDM, section.getPlacement(), other.getSection().getPlacement()))));
                    line.add(new CSVFile.CSVField(String.valueOf(Placement.getDistanceInMinutes(iDM, section.getPlacement(), other.getSection().getPlacement()))));
                    line.add(new CSVFile.CSVField(sDF1.format(pair.get(other))));
                    line.add(new CSVFile.CSVField(sDF2.format(pair.get(other) / total.size())));
                    
                    csv.addLine(line);
                    firstClass = false;
                }                    
                firstCourse = false;
            }
            
            csv.addLine();
        }
        
        
        return csv;
    }
}
