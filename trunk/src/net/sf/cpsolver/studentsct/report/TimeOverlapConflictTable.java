package net.sf.cpsolver.studentsct.report;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.extension.TimeOverlapsCounter;
import net.sf.cpsolver.studentsct.extension.TimeOverlapsCounter.Conflict;
import net.sf.cpsolver.studentsct.model.Assignment;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.FreeTimeRequest;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * This class lists time overlapping conflicts in a {@link CSVFile} comma
 * separated text file. Only sections that allow overlaps
 * (see {@link Assignment#isAllowOverlap()}) can overlap. See {@link TimeOverlapsCounter} for more
 * details. <br>
 * <br>
 * 
 * Each line represent a pair if classes that overlap in time and have one
 * or more students in common.
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
 *          Copyright (C) 2013 Tomas Muller<br>
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
public class TimeOverlapConflictTable implements StudentSectioningReport {
    private static DecimalFormat sDF1 = new DecimalFormat("0.####");
    private static DecimalFormat sDF2 = new DecimalFormat("0.0000");

    private StudentSectioningModel iModel = null;
    private TimeOverlapsCounter iTOC = null;

    /**
     * Constructor
     * 
     * @param model
     *            student sectioning model
     */
    public TimeOverlapConflictTable(StudentSectioningModel model) {
        iModel = model;
        iTOC = model.getTimeOverlaps();
        if (iTOC == null) {
            iTOC = new TimeOverlapsCounter(null, model.getProperties());
        }
    }

    /** Return student sectioning model */
    public StudentSectioningModel getModel() {
        return iModel;
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
    public CSVFile createTable(boolean includeLastLikeStudents, boolean includeRealStudents) {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVFile.CSVField[] { new CSVFile.CSVField("Course"), new CSVFile.CSVField("Total\nConflicts"),
                new CSVFile.CSVField("Class"), new CSVFile.CSVField("Meeting Time"),
                new CSVFile.CSVField("Time\nConflicts"), new CSVFile.CSVField("% of Total\nConflicts"),
                new CSVFile.CSVField("Conflicting\nClass"), new CSVFile.CSVField("Conflicting\nMeeting Time"),
                new CSVFile.CSVField("Overlap [min]"), new CSVFile.CSVField("Joined\nConflicts"), new CSVFile.CSVField("% of Total\nConflicts")
                });
        
        Set<Conflict> confs = new HashSet<Conflict>();
        for (Request r1 : getModel().variables()) {
            if (r1.getAssignment() == null || r1 instanceof FreeTimeRequest)
                continue;
            for (Request r2 : r1.getStudent().getRequests()) {
                if (r2 instanceof FreeTimeRequest) {
                    FreeTimeRequest ft = (FreeTimeRequest)r2;
                    confs.addAll(iTOC.conflicts(r1.getAssignment(), ft.createEnrollment()));
                } else if (r2.getAssignment() != null && r1.getId() < r2.getId()) {
                    confs.addAll(iTOC.conflicts(r1.getAssignment(), r2.getAssignment()));
                }
            }
        }
        
        HashMap<Course, Set<Long>> totals = new HashMap<Course, Set<Long>>();
        HashMap<CourseSection, Map<CourseSection, Double>> conflictingPairs = new HashMap<CourseSection, Map<CourseSection,Double>>();
        HashMap<CourseSection, Set<Long>> sectionOverlaps = new HashMap<CourseSection, Set<Long>>();        
        
        for (Conflict conflict : confs) {
            if (conflict.getStudent().isDummy() && !includeLastLikeStudents) continue;
            if (!conflict.getStudent().isDummy() && !includeRealStudents) continue;
            if (conflict.getR1() instanceof FreeTimeRequest || conflict.getR2() instanceof FreeTimeRequest) continue;
            Section s1 = (Section)conflict.getS1(), s2 = (Section)conflict.getS2();
            Request r1 = conflict.getR1();
            Course c1 = conflict.getR1().getAssignment().getCourse();
            Request r2 = conflict.getR2();
            Course c2 = conflict.getR2().getAssignment().getCourse();
            CourseSection a = new CourseSection(c1, s1);
            CourseSection b = new CourseSection(c2, s2);
            
            Set<Long> students = totals.get(c1);
            if (students == null) {
                students = new HashSet<Long>();
                totals.put(c1, students);
            }
            students.add(r1.getStudent().getId());
            students = totals.get(c2);
            if (students == null) {
                students = new HashSet<Long>();
                totals.put(c2, students);
            }
            students.add(r2.getStudent().getId());
            
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
                
                for (CourseSection other: new TreeSet<CourseSection>(pair.keySet())) {
                    List<CSVFile.CSVField> line = new ArrayList<CSVFile.CSVField>();
                    line.add(new CSVFile.CSVField(firstCourse && firstClass ? course.getName() : ""));
                    line.add(new CSVFile.CSVField(firstCourse && firstClass ? total.size() : ""));
                    
                    line.add(new CSVFile.CSVField(firstClass ? section.getSubpart().getName() + " " + section.getName(course.getId()): ""));
                    line.add(new CSVFile.CSVField(firstClass ? section.getTime() == null ? "" : section.getTime().getDayHeader() + " " + section.getTime().getStartTimeHeader() + " - " + section.getTime().getEndTimeHeader(): ""));
                        
                    line.add(new CSVFile.CSVField(firstClass && sectionOverlap != null ? String.valueOf(sectionOverlap.size()): ""));
                    line.add(new CSVFile.CSVField(firstClass && sectionOverlap != null ? sDF2.format(((double)sectionOverlap.size()) / total.size()) : ""));

                    line.add(new CSVFile.CSVField(other.getCourse().getName() + " " + other.getSection().getSubpart().getName() + " " + other.getSection().getName(other.getCourse().getId())));
                    line.add(new CSVFile.CSVField(other.getSection().getTime().getDayHeader() + " " + other.getSection().getTime().getStartTimeHeader() + " - " + other.getSection().getTime().getEndTimeHeader()));
                    
                    line.add(new CSVFile.CSVField(sDF1.format(5 * iTOC.share(section, other.getSection()))));
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

    @Override
    public CSVFile create(DataProperties properties) {
        return createTable(properties.getPropertyBoolean("lastlike", false), properties.getPropertyBoolean("real", true));
    }
}
