package org.cpsolver.studentsct.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.CSVFile;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.extension.StudentQuality;
import org.cpsolver.studentsct.extension.StudentQuality.Conflict;
import org.cpsolver.studentsct.extension.StudentQuality.Type;
import org.cpsolver.studentsct.model.AreaClassificationMajor;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Instructor;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.SctAssignment;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.StudentGroup;


/**
 * This class lists student accommodation conflicts in a {@link CSVFile} comma
 * separated text file. See {@link StudentQuality} for more
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
public class AccommodationConflictsTable extends AbstractStudentSectioningReport {
    private StudentQuality iSQ = null;
    private Type[] iTypes = new Type[] {
            Type.ShortDistance, Type.AccBackToBack, Type.AccBreaksBetweenClasses, Type.AccFreeTimeOverlap
    };

    /**
     * Constructor
     * 
     * @param model
     *            student sectioning model
     */
    public AccommodationConflictsTable(StudentSectioningModel model) {
        super(model);
        iSQ = model.getStudentQuality();
    }
    
    protected String rooms(SctAssignment section) {
        if (section.getNrRooms() == 0) return "";
        String ret = "";
        for (RoomLocation r: section.getRooms())
            ret += (ret.isEmpty() ? "" : ", ") + r.getName();
        return ret;
    }
    
    protected String curriculum(Student student) {
        String curriculum = "";
        for (AreaClassificationMajor acm: student.getAreaClassificationMajors())
                curriculum += (curriculum.isEmpty() ? "" : ", ") + acm.toString();
        return curriculum;
    }
    
    protected String group(Student student) {
        String group = "";
        Set<String> groups = new TreeSet<String>();
        for (StudentGroup g: student.getGroups())
                groups.add(g.getReference());
        for (String g: groups)
                group += (group.isEmpty() ? "" : ", ") + g;
        return group;           
    }
    
    protected String advisor(Student student) {
        String advisors = "";
        for (Instructor instructor: student.getAdvisors())
                advisors += (advisors.isEmpty() ? "" : ", ") + instructor.getName();
        return advisors;
    }

    /**
     * Create report
     * @param assignment current assignment
     * @return report as comma separated text file
     */
    @Override
    public CSVFile createTable(Assignment<Request, Enrollment> assignment, DataProperties properties) {
        if (iSQ == null) throw new IllegalArgumentException("Student Schedule Quality is not enabled.");

        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVFile.CSVField[] {
                new CSVFile.CSVField("__Student"),
                new CSVFile.CSVField("External Id"), new CSVFile.CSVField("Student Name"),
                new CSVFile.CSVField("Curriculum"), new CSVFile.CSVField("Group"), new CSVFile.CSVField("Advisor"),
                new CSVFile.CSVField("Type"),
                new CSVFile.CSVField("Course"), new CSVFile.CSVField("Class"), new CSVFile.CSVField("Meeting Time"), new CSVFile.CSVField("Room"),
                new CSVFile.CSVField("Conflicting\nCourse"), new CSVFile.CSVField("Conflicting\nClass"), new CSVFile.CSVField("Conflicting\nMeeting Time"), new CSVFile.CSVField("Conflicting\nRoom"),
                new CSVFile.CSVField("Penalty\nMinutes")
                });
        
        List<Conflict> confs = new ArrayList<Conflict>();
        for (Request r1 : getModel().variables()) {
            Enrollment e1 = assignment.getValue(r1);
            if (e1 == null || !(r1 instanceof CourseRequest))
                continue;
            for (StudentQuality.Type t: iTypes)
                confs.addAll(iSQ.conflicts(t, e1));
            for (Request r2 : r1.getStudent().getRequests()) {
                Enrollment e2 = assignment.getValue(r2);
                if (e2 == null || r1.getId() >= r2.getId() || !(r2 instanceof CourseRequest))
                    continue;
                for (StudentQuality.Type t: iTypes)
                    confs.addAll(iSQ.conflicts(t, e1, e2));
            }
        }
        Collections.sort(confs, new Comparator<Conflict>() {
            @Override
            public int compare(Conflict c1, Conflict c2) {
                int cmp = (c1.getStudent().getExternalId() == null ? "" : c1.getStudent().getExternalId()).compareTo(c2.getStudent().getExternalId() == null ? "" : c2.getStudent().getExternalId());
                if (cmp != 0) return cmp;
                cmp = c1.getStudent().compareTo(c2.getStudent());
                if (cmp != 0) return cmp;
                if (c1.getType() != c2.getType())
                    return Integer.compare(c1.getType().ordinal(), c2.getType().ordinal());
                cmp = c1.getE1().getCourse().getName().toString().compareTo(c2.getE1().getCourse().getName());
                if (cmp != 0) return cmp;
                return ((Section)c1.getS1()).getName(c1.getE1().getCourse().getId()).compareTo(((Section)c2.getS1()).getName(c2.getE1().getCourse().getId()));
            }
        });
        
        for (Conflict conflict : confs) {
            if (!matches(conflict.getR1(), conflict.getE1())) continue;
            if (conflict.getType() == Type.AccBackToBack) {
                boolean trueConflict = false;
                for (int i = 0; i < Constants.DAY_CODES.length; i++) {
                    if ((conflict.getS1().getTime().getDayCode() & Constants.DAY_CODES[i]) == 0 || (conflict.getS2().getTime().getDayCode() & Constants.DAY_CODES[i]) == 0) continue;
                    boolean inBetween = false;
                    for (Request r: conflict.getStudent().getRequests()) {
                        Enrollment e = r.getAssignment(assignment);
                        if (e == null) continue;
                        for (SctAssignment s: e.getAssignments()) {
                            if (s.getTime() == null) continue;
                            if ((s.getTime().getDayCode() & Constants.DAY_CODES[i]) == 0) continue;
                            if (!s.getTime().shareWeeks(conflict.getS1().getTime()) || !s.getTime().shareWeeks(conflict.getS2().getTime())) continue;
                            if (conflict.getS1().getTime().getStartSlot() + conflict.getS1().getTime().getLength() <= s.getTime().getStartSlot() &&
                                s.getTime().getStartSlot() + s.getTime().getLength() <= conflict.getS2().getTime().getStartSlot()) {
                                inBetween = true; break;
                            }
                            if (conflict.getS2().getTime().getStartSlot() + conflict.getS2().getTime().getLength() <= s.getTime().getStartSlot() &&
                                s.getTime().getStartSlot() + s.getTime().getLength() <= conflict.getS1().getTime().getStartSlot()) {
                                inBetween = true; break;
                            }
                        }
                    }
                    if (!inBetween) { trueConflict = true; break; }
                }
                if (!trueConflict) continue;
            }
            List<CSVFile.CSVField> line = new ArrayList<CSVFile.CSVField>();
            
            line.add(new CSVFile.CSVField(conflict.getStudent().getId()));
            line.add(new CSVFile.CSVField(conflict.getStudent().getExternalId()));
            line.add(new CSVFile.CSVField(conflict.getStudent().getName()));
            line.add(new CSVFile.CSVField(curriculum(conflict.getStudent())));
            line.add(new CSVFile.CSVField(group(conflict.getStudent())));
            line.add(new CSVFile.CSVField(advisor(conflict.getStudent())));
            switch (conflict.getType()) {
                case ShortDistance:
                    line.add(new CSVFile.CSVField(iSQ.getDistanceMetric().getShortDistanceAccommodationReference()));
                    break;
                case AccBackToBack:
                    line.add(new CSVFile.CSVField(iSQ.getStudentQualityContext().getBackToBackAccommodation()));
                    break;
                case AccBreaksBetweenClasses:
                    line.add(new CSVFile.CSVField(iSQ.getStudentQualityContext().getBreakBetweenClassesAccommodation()));
                    break;
                case AccFreeTimeOverlap:
                    line.add(new CSVFile.CSVField(iSQ.getStudentQualityContext().getFreeTimeAccommodation()));
                    break;
                default:
                    line.add(new CSVFile.CSVField(conflict.getType().getName()));
                    break;
            }
            line.add(new CSVFile.CSVField(conflict.getE1().getCourse().getName()));
            line.add(new CSVFile.CSVField(conflict.getS1() instanceof Section ? ((Section)conflict.getS1()).getName(conflict.getE1().getCourse().getId()) : ""));
            line.add(new CSVFile.CSVField(conflict.getS1().getTime() == null ? "" : conflict.getS1().getTime().getLongName(isUseAmPm())));
            line.add(new CSVFile.CSVField(rooms(conflict.getS1())));
            line.add(new CSVFile.CSVField(conflict.getE2().isCourseRequest() ? conflict.getE2().getCourse().getName() : "Free Time"));
            line.add(new CSVFile.CSVField(conflict.getS2() instanceof Section ? ((Section)conflict.getS2()).getName(conflict.getE2().getCourse().getId()) : ""));
            line.add(new CSVFile.CSVField(conflict.getS2().getTime() == null ? "" : conflict.getS2().getTime().getLongName(isUseAmPm())));
            line.add(new CSVFile.CSVField(rooms(conflict.getS2())));
            switch (conflict.getType()) {
                case AccFreeTimeOverlap:
                    line.add(new CSVFile.CSVField(5 * conflict.getPenalty()));
                    break;
                case ShortDistance:
                    line.add(new CSVFile.CSVField(iSQ.getStudentQualityContext().getDistanceInMinutes(((Section)conflict.getS1()).getPlacement(), ((Section)conflict.getS2()).getPlacement())));
                    break;
                case AccBackToBack:
                case AccBreaksBetweenClasses:
                    TimeLocation t1 = conflict.getS1().getTime();
                    TimeLocation t2 = conflict.getS2().getTime();
                    if (t1.getStartSlot() + t1.getNrSlotsPerMeeting() <= t2.getStartSlot()) {
                        int dist = t2.getStartSlot() - (t1.getStartSlot() + t1.getNrSlotsPerMeeting());
                        line.add(new CSVFile.CSVField(5 * dist + t1.getBreakTime()));
                    } else if (t2.getStartSlot() + t2.getNrSlotsPerMeeting() <= t1.getStartSlot()) {
                        int dist = t1.getStartSlot() - (t2.getStartSlot() + t2.getNrSlotsPerMeeting());
                        line.add(new CSVFile.CSVField(5 * dist + t2.getBreakTime()));
                    } else {
                        line.add(new CSVFile.CSVField(null));
                    }
                    break;
                default:
                    line.add(new CSVFile.CSVField(conflict.getPenalty()));
                    break;
            }
            csv.addLine(line);
        }
        
        return csv;
    }
}
