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

import net.sf.cpsolver.ifs.model.GlobalConstraint;
import net.sf.cpsolver.ifs.util.CSVFile;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.constraint.SectionLimit;
import net.sf.cpsolver.studentsct.model.Course;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.FreeTimeRequest;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * This class computes time and availability conflicts on classes in a {@link CSVFile} comma separated
 * text file. <br>
 * <br>
 * The first report (type OVERLAPS) shows time conflicts between pairs of classes. Each such enrollment
 * is given a weight of 1/n, where n is the number of available enrollments of the student into the course.
 * This 1/n is added to each class that is present in a conflict. These numbers are aggregated on
 * individual classes and on pairs of classes (that are in a time conflict).
 * <br>
 * The second report (type UNAVAILABILITIES) shows for each course how many students could not get into
 * the course because of the limit constraints. It considers all the not-conflicting, but unavailable enrollments
 * of a student into the course. For each such an enrollment 1/n is added to each class. So, in a way, the
 * Availability Conflicts column shows how much space is missing in each class. The Class Potential column
 * can be handy as well. If the class would be unlimited, this is the number of students (out of all the 
 * conflicting students) that can get into the class.
 * <br>
 * The last report (type OVERLAPS_AND_UNAVAILABILITIES) show the two reports together. It is possible that
 * there is a course where some students cannot get in because of availabilities (all not-conflicting enrollments
 * have no available space) as well as time conflicts (all available enrollments are conflicting with some other
 * classes the student has). 
 * <br>
 * <br>
 * 
 * Usage: new SectionConflictTable(model, type),createTable(true, true).save(aFile);
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
public class SectionConflictTable implements StudentSectioningReport {
    private static DecimalFormat sDF1 = new DecimalFormat("0.####");
    private static DecimalFormat sDF2 = new DecimalFormat("0.0000");

    private StudentSectioningModel iModel = null;
    private Type iType;
    private boolean iOverlapsAllEnrollments = true;
    
    /**
     * Report type
     */
    public static enum Type {
        /** Time conflicts */
        OVERLAPS(true, false),
        /** Availability conflicts */
        UNAVAILABILITIES(false, true),
        /** Both time and availability conflicts */
        OVERLAPS_AND_UNAVAILABILITIES(true, true),
        ;
        
        boolean iOveralps, iUnavailabilities;
        Type(boolean overlaps, boolean unavailabilities) {
            iOveralps = overlaps;
            iUnavailabilities = unavailabilities;
        }
        
        /** Has time conflicts */
        public boolean hasOverlaps() { return iOveralps; }
        
        /** Has availability conflicts */
        public boolean hasUnavailabilities() { return iUnavailabilities; }
    }

    /**
     * Constructor
     * 
     * @param model
     *            student sectioning model
     */
    public SectionConflictTable(StudentSectioningModel model, Type type) {
        iModel = model;
        iType = type;
    }
    
    public SectionConflictTable(StudentSectioningModel model) {
        this(model, Type.OVERLAPS_AND_UNAVAILABILITIES);
    }

    /** Return student sectioning model */
    public StudentSectioningModel getModel() {
        return iModel;
    }
    
    private boolean canIgnore(Enrollment enrollment, Section section, List<Enrollment> other) {
        e: for (Enrollment e: other) {
            Section a = null;
            for (Section s: e.getSections()) {
                if (s.getSubpart().equals(section.getSubpart())) {
                    if (s.equals(section)) continue e;
                    a = s;
                } else if (!enrollment.getSections().contains(s))
                    continue e;
            }
            if (a == null) continue e;
            for (Request r: enrollment.getStudent().getRequests())
                if (!enrollment.getRequest().equals(r) && r.getAssignment() != null && r instanceof CourseRequest && !r.getAssignment().isAllowOverlap())
                    for (Section b: r.getAssignment().getSections())
                        if (!b.isAllowOverlap() && !b.isToIgnoreStudentConflictsWith(section.getId()) && b.getTime() != null && a.getTime() != null && !a.isAllowOverlap() && b.getTime().hasIntersection(a.getTime()))
                            continue e;
            return true;
        }
        return false;
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
        HashMap<Course, Map<Section, Double[]>> unavailabilities = new HashMap<Course, Map<Section,Double[]>>();
        HashMap<Course, Set<Long>> totals = new HashMap<Course, Set<Long>>();
        HashMap<CourseSection, Map<CourseSection, Double>> conflictingPairs = new HashMap<CourseSection, Map<CourseSection,Double>>();
        HashMap<CourseSection, Double> sectionOverlaps = new HashMap<CourseSection, Double>();        
        
        for (Request request : new ArrayList<Request>(getModel().unassignedVariables())) {
            if (request.getStudent().isDummy() && !includeLastLikeStudents) continue;
            if (!request.getStudent().isDummy() && !includeRealStudents) continue;
            if (request instanceof CourseRequest) {
                CourseRequest courseRequest = (CourseRequest) request;
                if (courseRequest.getStudent().isComplete()) continue;
                
                List<Enrollment> values = courseRequest.values();

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
                List<Enrollment> notAvailableValues = new ArrayList<Enrollment>(values.size());
                List<Enrollment> availableValues = new ArrayList<Enrollment>(values.size());
                for (Enrollment enrollment : values) {
                    if (limitConstraint.inConflict(enrollment))
                        notAvailableValues.add(enrollment);
                    else
                        availableValues.add(enrollment); 
                }
                
                if (!notAvailableValues.isEmpty() && iType.hasUnavailabilities()) {
                    List<Enrollment> notOverlappingEnrollments = new ArrayList<Enrollment>(values.size());
                    enrollments: for (Enrollment enrollment: notAvailableValues) {
                        for (Request other : request.getStudent().getRequests()) {
                            if (other.equals(request) || other.getAssignment() == null || other instanceof FreeTimeRequest) continue;
                            if (other.getAssignment().isOverlapping(enrollment)) continue enrollments;
                        }
                        // not overlapping
                        notOverlappingEnrollments.add(enrollment);
                    }
                    
                    if (notOverlappingEnrollments.isEmpty()  && availableValues.isEmpty() && iOverlapsAllEnrollments) {
                        double fraction = request.getWeight() / notAvailableValues.size();
                        Set<CourseSection> ones = new HashSet<CourseSection>();
                        for (Enrollment enrollment: notAvailableValues) {
                            boolean hasConflict = false;
                            for (Section s: enrollment.getSections()) {
                                if (s.getLimit() >= 0 && s.getEnrollmentWeight(request) + request.getWeight() > s.getLimit()) {
                                    hasConflict = true;
                                    break;
                                }
                            }
                            
                            Map<Section, Double[]> sections = unavailabilities.get(enrollment.getCourse());
                            if (sections == null) {
                                sections = new HashMap<Section, Double[]>();
                                unavailabilities.put(enrollment.getCourse(), sections);
                            }
                            for (Section s: enrollment.getSections()) {
                                if (hasConflict && s.getLimit() < 0 || s.getEnrollmentWeight(request) + request.getWeight() <= s.getLimit()) continue;
                                Double[] total = sections.get(s);
                                sections.put(s, new Double[] {
                                            fraction + (total == null ? 0.0 : total[0].doubleValue()),
                                            (total == null ? 0.0 : total[1].doubleValue())
                                        });
                                ones.add(new CourseSection(enrollment.getCourse(), s));
                            }
                            Set<Long> total = totals.get(enrollment.getCourse());
                            if (total == null) {
                                total = new HashSet<Long>();
                                totals.put(enrollment.getCourse(), total);
                            }
                            total.add(enrollment.getStudent().getId());
                        }
                    } else if (!notOverlappingEnrollments.isEmpty()) {
                        double fraction = request.getWeight() / notOverlappingEnrollments.size();
                        Set<CourseSection> ones = new HashSet<CourseSection>();
                        for (Enrollment enrollment: notOverlappingEnrollments) {
                            boolean hasConflict = false;
                            for (Section s: enrollment.getSections()) {
                                if (s.getLimit() >= 0 && s.getEnrollmentWeight(request) + request.getWeight() > s.getLimit()) {
                                    hasConflict = true;
                                    break;
                                }
                            }
                            
                            Map<Section, Double[]> sections = unavailabilities.get(enrollment.getCourse());
                            if (sections == null) {
                                sections = new HashMap<Section, Double[]>();
                                unavailabilities.put(enrollment.getCourse(), sections);
                            }
                            for (Section s: enrollment.getSections()) {
                                if (hasConflict && s.getLimit() < 0 || s.getEnrollmentWeight(request) + request.getWeight() <= s.getLimit()) continue;
                                Double[] total = sections.get(s);
                                sections.put(s, new Double[] {
                                            fraction + (total == null ? 0.0 : total[0].doubleValue()),
                                            (total == null ? 0.0 : total[1].doubleValue())
                                        });
                                ones.add(new CourseSection(enrollment.getCourse(), s));
                            }
                            Set<Long> total = totals.get(enrollment.getCourse());
                            if (total == null) {
                                total = new HashSet<Long>();
                                totals.put(enrollment.getCourse(), total);
                            }
                            total.add(enrollment.getStudent().getId());
                        }
                        for (CourseSection section: ones) {
                            Map<Section, Double[]> sections = unavailabilities.get(section.getCourse());
                            Double[] total = sections.get(section.getSection());
                            sections.put(section.getSection(), new Double[] {
                                    (total == null ? 0.0 : total[0].doubleValue()),
                                    request.getWeight() + (total == null ? 0.0 : total[1].doubleValue())
                                });
                        }                        
                    }
                }
                
                if (iOverlapsAllEnrollments)
                    availableValues = values;
                if (!availableValues.isEmpty() && iType.hasOverlaps()) {
                    List<Map<CourseSection, List<CourseSection>>> conflicts = new ArrayList<Map<CourseSection, List<CourseSection>>>();
                    for (Enrollment enrollment: availableValues) {
                        Map<CourseSection, List<CourseSection>> overlaps = new HashMap<CourseSection, List<CourseSection>>();
                        for (Request other : request.getStudent().getRequests()) {
                            if (other.equals(request) || other.getAssignment() == null || other instanceof FreeTimeRequest) continue;
                            Enrollment otherEnrollment = other.getAssignment();
                            if (enrollment.isOverlapping(otherEnrollment))
                                for (Section a: enrollment.getSections())
                                    for (Section b: other.getAssignment().getSections())
                                        if (a.getTime() != null && b.getTime() != null && !a.isAllowOverlap() && !b.isAllowOverlap() && !a.isToIgnoreStudentConflictsWith(b.getId()) && a.getTime().hasIntersection(b.getTime()) && !canIgnore(enrollment, a, availableValues)) {
                                            List<CourseSection> x = overlaps.get(new CourseSection(enrollment.getCourse(), a));
                                            if (x == null) { x = new ArrayList<CourseSection>(); overlaps.put(new CourseSection(enrollment.getCourse(), a), x); }
                                            x.add(new CourseSection(other.getAssignment().getCourse(), b));
                                        }
                        }
                        if (!overlaps.isEmpty()) {
                            conflicts.add(overlaps);
                            Set<Long> total = totals.get(enrollment.getCourse());
                            if (total == null) {
                                total = new HashSet<Long>();
                                totals.put(enrollment.getCourse(), total);
                            }
                            total.add(enrollment.getStudent().getId());
                        }
                    }
                    
                    double fraction = request.getWeight() / conflicts.size();
                    for (Map<CourseSection, List<CourseSection>> overlaps: conflicts) {
                        for (Map.Entry<CourseSection, List<CourseSection>> entry: overlaps.entrySet()) {
                            CourseSection a = entry.getKey();
                            Double total = sectionOverlaps.get(a);
                            sectionOverlaps.put(a, fraction + (total == null ? 0.0 : total.doubleValue()));
                            Map<CourseSection, Double> pair = conflictingPairs.get(a);
                            if (pair == null) {
                                pair = new HashMap<CourseSection, Double>();
                                conflictingPairs.put(a, pair);
                            }
                            for (CourseSection b: entry.getValue()) {
                                Double prev = pair.get(b);
                                pair.put(b, fraction + (prev == null ? 0.0 : prev.doubleValue()));
                            }
                        }
                    }
                }
            }
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
        
        CSVFile csv = new CSVFile();
        List<CSVFile.CSVField> headers = new ArrayList<CSVFile.CSVField>();
        headers.add(new CSVFile.CSVField("Course"));
        headers.add(new CSVFile.CSVField("Total\nConflicts"));
        if (iType.hasUnavailabilities()) {
            headers.add(new CSVFile.CSVField("Course\nEnrollment"));
            headers.add(new CSVFile.CSVField("Course\nLimit"));
        }
        headers.add(new CSVFile.CSVField("Class"));
        headers.add(new CSVFile.CSVField("Meeting Time"));
        if (iType.hasUnavailabilities()) {
            headers.add(new CSVFile.CSVField("Availability\nConflicts"));
            headers.add(new CSVFile.CSVField("% of Total\nConflicts"));
        }
        if (iType.hasOverlaps()) {
            headers.add(new CSVFile.CSVField("Time\nConflicts"));
            headers.add(new CSVFile.CSVField("% of Total\nConflicts"));
        }
        if (iType.hasUnavailabilities()) {
            headers.add(new CSVFile.CSVField("Class\nEnrollment"));
            headers.add(new CSVFile.CSVField("Class\nLimit"));
            if (!iType.hasOverlaps())
                headers.add(new CSVFile.CSVField("Class\nPotential"));
        }
        if (iType.hasOverlaps()) {
            headers.add(new CSVFile.CSVField("Conflicting\nClass"));
            headers.add(new CSVFile.CSVField("Conflicting\nMeeting Time"));
            headers.add(new CSVFile.CSVField("Joined\nConflicts"));
            headers.add(new CSVFile.CSVField("% of Total\nConflicts"));
        }
        csv.setHeader(headers);
        
        TreeSet<Course> courses = new TreeSet<Course>(courseComparator);
        courses.addAll(totals.keySet());
        
        for (Course course: courses) {
            Map<Section, Double[]> sectionUnavailability = unavailabilities.get(course);
            Set<Long> total = totals.get(course);
            
            TreeSet<Section> sections = new TreeSet<Section>(sectionComparator);
            if (sectionUnavailability != null)
                sections.addAll(sectionUnavailability.keySet());
            for (Map.Entry<CourseSection, Double> entry: sectionOverlaps.entrySet())
                if (course.equals(entry.getKey().getCourse()))
                    sections.add(entry.getKey().getSection());
            
            boolean firstCourse = true;
            for (Section section: sections) {
                Double[] sectionUnavailable = (sectionUnavailability == null ? null : sectionUnavailability.get(section));
                Double sectionOverlap = sectionOverlaps.get(new CourseSection(course, section));
                Map<CourseSection, Double> pair = conflictingPairs.get(new CourseSection(course, section));
                
                if (pair == null) {
                    List<CSVFile.CSVField> line = new ArrayList<CSVFile.CSVField>();
                    line.add(new CSVFile.CSVField(firstCourse ? course.getName() : ""));
                    line.add(new CSVFile.CSVField(firstCourse ? total.size() : ""));
                    if (iType.hasUnavailabilities()) {
                        line.add(new CSVFile.CSVField(firstCourse ? sDF1.format(course.getEnrollmentWeight(null)) : ""));
                        line.add(new CSVFile.CSVField(firstCourse ? course.getLimit() < 0 ? "" : String.valueOf(course.getLimit()) : ""));
                    }
                    
                    line.add(new CSVFile.CSVField(section.getSubpart().getName() + " " + section.getName(course.getId())));
                    line.add(new CSVFile.CSVField(section.getTime() == null ? "" : section.getTime().getDayHeader() + " " + section.getTime().getStartTimeHeader() + " - " + section.getTime().getEndTimeHeader()));
                    
                    if (iType.hasUnavailabilities()) {
                        line.add(new CSVFile.CSVField(sectionUnavailable != null ? sDF2.format(sectionUnavailable[0]) : ""));
                        line.add(new CSVFile.CSVField(sectionUnavailable != null ? sDF2.format(sectionUnavailable[0] / total.size()) : ""));
                    }
                    if (iType.hasOverlaps()) {
                        line.add(new CSVFile.CSVField(sectionOverlap != null ? sDF2.format(sectionOverlap) : ""));
                        line.add(new CSVFile.CSVField(sectionOverlap != null ? sDF2.format(sectionOverlap / total.size()) : ""));
                    }
                    if (iType.hasUnavailabilities()) {
                        line.add(new CSVFile.CSVField(sDF1.format(section.getEnrollmentWeight(null))));
                        line.add(new CSVFile.CSVField(section.getLimit() < 0 ? "" : String.valueOf(section.getLimit())));
                        if (!iType.hasOverlaps())
                            line.add(new CSVFile.CSVField(sectionUnavailable != null ? sDF1.format(sectionUnavailable[1]) : ""));
                    }
                    
                    csv.addLine(line);
                } else {
                    boolean firstClass = true;
                    for (CourseSection other: new TreeSet<CourseSection>(pair.keySet())) {
                        List<CSVFile.CSVField> line = new ArrayList<CSVFile.CSVField>();
                        line.add(new CSVFile.CSVField(firstCourse && firstClass ? course.getName() : ""));
                        line.add(new CSVFile.CSVField(firstCourse && firstClass ? total.size() : ""));
                        if (iType.hasUnavailabilities()) {
                            line.add(new CSVFile.CSVField(firstCourse && firstClass ? sDF1.format(course.getEnrollmentWeight(null)) : ""));
                            line.add(new CSVFile.CSVField(firstCourse && firstClass ? course.getLimit() < 0 ? "" : String.valueOf(course.getLimit()) : ""));
                        }
                        
                        line.add(new CSVFile.CSVField(firstClass ? section.getSubpart().getName() + " " + section.getName(course.getId()): ""));
                        line.add(new CSVFile.CSVField(firstClass ? section.getTime() == null ? "" : section.getTime().getDayHeader() + " " + section.getTime().getStartTimeHeader() + " - " + section.getTime().getEndTimeHeader(): ""));
                        
                        if (iType.hasUnavailabilities()) {
                            line.add(new CSVFile.CSVField(firstClass && sectionUnavailable != null ? sDF2.format(sectionUnavailable[0]): ""));
                            line.add(new CSVFile.CSVField(sectionUnavailable != null ? sDF2.format(sectionUnavailable[0] / total.size()) : ""));
                        }
                        line.add(new CSVFile.CSVField(firstClass && sectionOverlap != null ? sDF2.format(sectionOverlap): ""));
                        line.add(new CSVFile.CSVField(firstClass && sectionOverlap != null ? sDF2.format(sectionOverlap / total.size()) : ""));
                        if (iType.hasUnavailabilities()) {
                            line.add(new CSVFile.CSVField(firstClass ? sDF1.format(section.getEnrollmentWeight(null)): ""));
                            line.add(new CSVFile.CSVField(firstClass ? section.getLimit() < 0 ? "" : String.valueOf(section.getLimit()): ""));
                        }
                        
                        line.add(new CSVFile.CSVField(other.getCourse().getName() + " " + other.getSection().getSubpart().getName() + " " + other.getSection().getName(other.getCourse().getId())));
                        line.add(new CSVFile.CSVField(other.getSection().getTime().getDayHeader() + " " + other.getSection().getTime().getStartTimeHeader() + " - " + other.getSection().getTime().getEndTimeHeader()));
                        line.add(new CSVFile.CSVField(sDF2.format(pair.get(other))));
                        line.add(new CSVFile.CSVField(sDF2.format(pair.get(other) / total.size())));
                        
                        csv.addLine(line);
                        firstClass = false;
                    }                    
                }
                
                firstCourse = false;
            }
            
            csv.addLine();
        }
        return csv;
    }

    @Override
    public CSVFile create(DataProperties properties) {
        iType = Type.valueOf(properties.getProperty("type", iType.name()));
        iOverlapsAllEnrollments = properties.getPropertyBoolean("overlapsIncludeAll", true);
        return createTable(properties.getPropertyBoolean("lastlike", false), properties.getPropertyBoolean("real", true));
    }
}
