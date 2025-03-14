package org.cpsolver.studentsct.report;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.CSVFile;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.model.Choice;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Subpart;
import org.cpsolver.studentsct.reservation.Reservation;

/**
 * This reports lists information needed for additional reporting.<br>
 * <br>
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2015 Tomas Muller<br>
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
public class TableauReport extends AbstractStudentSectioningReport {

    /**
     * Constructor
     * 
     * @param model
     *            student sectioning model
     */
    public TableauReport(StudentSectioningModel model) {
        super(model);
    }

    @Override
    public CSVFile createTable(Assignment<Request, Enrollment> assignment, DataProperties properties) {
        CSVFile csv = new CSVFile();
        boolean simple = properties.getPropertyBoolean("simple", false);
        if (simple) {
            csv.setHeader(new CSVFile.CSVField[] {
                    new CSVFile.CSVField("__Student"),
                    new CSVFile.CSVField("Student"),
                    new CSVFile.CSVField("Course"),
                    new CSVFile.CSVField("Course Limit"),
                    new CSVFile.CSVField("Primary"),
                    new CSVFile.CSVField("Priority"),
                    new CSVFile.CSVField("Alternativity"),
                    new CSVFile.CSVField("Enrolled"),
                    new CSVFile.CSVField("Request Type")
                    });
        } else {
            csv.setHeader(new CSVFile.CSVField[] {
                    new CSVFile.CSVField("__Student"),
                    new CSVFile.CSVField("Student"),
                    new CSVFile.CSVField("Course"),
                    new CSVFile.CSVField("Course Limit"),
                    new CSVFile.CSVField("Controlling Course"),
                    new CSVFile.CSVField("Primary"),
                    new CSVFile.CSVField("Priority"),
                    new CSVFile.CSVField("Alternativity"),
                    new CSVFile.CSVField("Enrolled"),
                    new CSVFile.CSVField("Credits"),
                    new CSVFile.CSVField("Sections"),
                    new CSVFile.CSVField("Preferred Sections"),
                    new CSVFile.CSVField("Required Sections"),
                    new CSVFile.CSVField("Instructional Method"),
                    new CSVFile.CSVField("Preferred Instructional Methods"),
                    new CSVFile.CSVField("Required Instructional Methods"),
                    new CSVFile.CSVField("Request Type")
                    });
        }
        for (Student student: getModel().getStudents()) {
            if (student.isDummy()) continue;
            int regPriority = 1, altPriority = 1;
            for (Request r: student.getRequests()) {
                if (r instanceof CourseRequest) {
                    CourseRequest cr = (CourseRequest)r;
                    Enrollment e = cr.getAssignment(assignment);
                    if (!matches(r, e)) continue;
                    int primary = (cr.isAlternative() ? 0 : 1);
                    int priority = 0;
                    if (cr.isAlternative())
                        priority = altPriority++;
                    else
                        priority = regPriority++;
                    int alternativity = 0;
                    for (Course course: cr.getCourses()) {
                        int enrolled = (e != null && e.getCourse().equals(course) ? 1 : 0);
                        String sect = null;
                        if (e != null && e.getCourse().equals(course)) {
                            sect = "";
                            Set<String> added = new HashSet<String>();
                            for (Section s: e.getSections()) {
                                String x = s.getName(e.getCourse().getId());
                                if (x.indexOf('-') > 0) x = x.substring(0, x.indexOf('-'));
                                if (added.add(x)) sect += (sect.isEmpty() ? "" : ",") + x;
                            }
                        }
                        String imR = "", sctR = "";
                        Set<String> addedR = new HashSet<String>();
                        for (Choice ch: cr.getRequiredChoices()) {
                            if (course.getOffering().equals(ch.getOffering())) {
                                if (ch.getConfigId() != null) {
                                    for (Config cfg: ch.getOffering().getConfigs()) {
                                        if (ch.getConfigId().equals(cfg.getId())) {
                                            String im = cfg.getInstructionalMethodReference();
                                            if (im != null && addedR.add(im))
                                                imR += (imR.isEmpty() ? "" : ",") + im;            
                                        }
                                    }
                                }
                                if (ch.getSectionId() != null) {
                                    String x = ch.getOffering().getSection(ch.getSectionId()).getName(course.getId());
                                    if (x.indexOf('-') > 0) x = x.substring(0, x.indexOf('-'));
                                    if (addedR.add(x))
                                        sctR += (sctR.isEmpty() ? "" : ",") + x;
                                }
                            }
                        }
                        for (Reservation rs: cr.getReservations(course)) {
                            if (rs.mustBeUsed()) {
                                for (Map.Entry<Subpart, Set<Section>> ent: rs.getSections().entrySet()) {
                                    for (Section s: ent.getValue()) {
                                        String x = s.getName(course.getId());
                                        if (x.indexOf('-') > 0) x = x.substring(0, x.indexOf('-'));
                                        if (addedR.add(x))
                                            sctR += (sctR.isEmpty() ? "" : ",") + x;
                                    }
                                }
                                if (rs.getSections().isEmpty()) {
                                    for (Config cfg: rs.getConfigs()) {
                                        String im = cfg.getInstructionalMethodReference();
                                        if (im != null && addedR.add(im))
                                            imR += (imR.isEmpty() ? "" : ",") + im;
                                    }
                                }
                            }
                        }
                        String imP = "", sctP = "";
                        for (Choice ch: cr.getSelectedChoices()) {
                            Set<String> added = new HashSet<String>();
                            if (course.getOffering().equals(ch.getOffering())) {
                                if (ch.getConfigId() != null) {
                                    for (Config cfg: ch.getOffering().getConfigs()) {
                                        if (ch.getConfigId().equals(cfg.getId())) {
                                            String im = cfg.getInstructionalMethodReference();
                                            if (im != null && added.add(im))
                                                imP += (imP.isEmpty() ? "" : ",") + im;            
                                        }
                                    }
                                }
                                if (ch.getSectionId() != null) {
                                    String x = ch.getOffering().getSection(ch.getSectionId()).getName(course.getId());
                                    if (x.indexOf('-') > 0) x = x.substring(0, x.indexOf('-'));
                                    if (added.add(x))
                                        sctP += (sctP.isEmpty() ? "" : ",") + x;
                                }
                            }
                        }
                        if (simple)
                            csv.addLine(new CSVFile.CSVField[] {
                                    new CSVFile.CSVField(student.getId()),
                                    new CSVFile.CSVField(student.getExternalId()),
                                    new CSVFile.CSVField(course.getName()),
                                    new CSVFile.CSVField(course.getLimit() < 0 ? null : course.getLimit()),
                                    new CSVFile.CSVField(primary == 1 ? "Yes" : "No"),
                                    new CSVFile.CSVField(priority),
                                    new CSVFile.CSVField(alternativity),
                                    new CSVFile.CSVField(enrolled == 1 ? "Yes" : "No"),
                                    new CSVFile.CSVField(cr.getRequestPriority() == null ? "" : cr.getRequestPriority().name())
                            });
                        else
                            csv.addLine(new CSVFile.CSVField[] {
                                    new CSVFile.CSVField(student.getId()),
                                    new CSVFile.CSVField(student.getExternalId()),
                                    new CSVFile.CSVField(course.getName()),
                                    new CSVFile.CSVField(course.getLimit() < 0 ? null : course.getLimit()),
                                    new CSVFile.CSVField(course.getOffering().getCourses().size() <= 1 ? null : course.getOffering().getName()),
                                    new CSVFile.CSVField(primary == 1 ? "Yes" : "No"),
                                    new CSVFile.CSVField(priority),
                                    new CSVFile.CSVField(alternativity),
                                    new CSVFile.CSVField(enrolled == 1 ? "Yes" : "No"),
                                    new CSVFile.CSVField(enrolled == 1 ? e.getCredit() : course.getCreditValue() == null ? 0f : course.getCreditValue()),
                                    new CSVFile.CSVField(sect),
                                    new CSVFile.CSVField(sctP),
                                    new CSVFile.CSVField(sctR),
                                    new CSVFile.CSVField(e != null ? e.getConfig().getInstructionalMethodReference() : null),
                                    new CSVFile.CSVField(imP),
                                    new CSVFile.CSVField(imR),
                                    new CSVFile.CSVField(cr.getRequestPriority() == null ? "" : cr.getRequestPriority().name())
                            });
                        alternativity++;
                    }
                }
            }
        }
        return csv;
    }
}