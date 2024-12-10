package org.cpsolver.studentsct.report;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.TreeSet;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.CSVFile;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.model.Config;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Offering;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.RequestGroup;
import org.cpsolver.studentsct.model.Section;
import org.cpsolver.studentsct.model.Subpart;

/**
 * This reports lists all request groups (including course name and group name) and the current spreads.
 * For each group, the current average spread (see {@link RequestGroup#getAverageSpread(Assignment)})
 * is listed together with all the classes that the students of the group are enrolled into and their
 * spreads (see {@link RequestGroup#getSectionSpread(Assignment, Section)}).<br>
 * <br>
 * The average spread corresponds with the probability of two students of the group to attend the same section.
 * The section spread is a break down of the average spread by each section.<br>
 * <br>
 * 
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
public class RequestGroupTable extends AbstractStudentSectioningReport {
    private static DecimalFormat sDF = new DecimalFormat("0.000");
    
    /**
     * Constructor
     * 
     * @param model student sectioning model
     */
    public RequestGroupTable(StudentSectioningModel model) {
        super(model);
    }

    @Override
    public CSVFile createTable(Assignment<Request, Enrollment> assignment, DataProperties properties) {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVFile.CSVField[] {
                new CSVFile.CSVField("Group"),
                new CSVFile.CSVField("Course"),
                new CSVFile.CSVField("Total\nSpread"),
                new CSVFile.CSVField("Group\nEnrollment"),
                new CSVFile.CSVField("Class"),
                new CSVFile.CSVField("Meeting Time"),
                new CSVFile.CSVField("Class\nSpread"),
                new CSVFile.CSVField("Class\nEnrollment"),
                new CSVFile.CSVField("Class\nLimit")
                });
        
        TreeSet<RequestGroup> groups = new TreeSet<RequestGroup>(new Comparator<RequestGroup>() {
            @Override
            public int compare(RequestGroup g1, RequestGroup g2) {
                int cmp = g1.getName().compareTo(g2.getName());
                if (cmp != 0) return cmp;
                cmp = g1.getCourse().getName().compareTo(g2.getCourse().getName());
                if (cmp != 0) return cmp;
                if (g1.getId() < g2.getId()) return -1;
                if (g1.getId() > g2.getId()) return 1;
                return (g1.getCourse().getId() < g2.getCourse().getId() ? -1 : g1.getCourse().getId() > g2.getCourse().getId() ? 1 : 0);
            }
        });
        
        for (Offering offering: getModel().getOfferings()) {
            if (offering.isDummy()) continue;
            for (Course course: offering.getCourses())
                groups.addAll(course.getRequestGroups());
        }
        
        for (RequestGroup group: groups) {
            int nbrMatches = 0;
            for (CourseRequest cr: group.getRequests()) {
                if (matches(cr)) nbrMatches ++;
            }
            if (nbrMatches == 0) continue;
            double groupEnrollment = group.getEnrollmentWeight(assignment, null);
            double groupSpread = group.getAverageSpread(assignment);
            for (Config config: group.getCourse().getOffering().getConfigs())
                for (Subpart subpart: config.getSubparts())
                    for (Section section: subpart.getSections()) {
                        double s = group.getSectionWeight(assignment, section, null);
                        if (s > 0.00001) {
                            csv.addLine(new CSVFile.CSVField[] {
                                    new CSVFile.CSVField(group.getName()),
                                    new CSVFile.CSVField(group.getCourse().getName()),
                                    new CSVFile.CSVField(sDF.format(100.0 * groupSpread)),
                                    new CSVFile.CSVField(Math.round(groupEnrollment)),
                                    new CSVFile.CSVField(section.getSubpart().getName() + " " + section.getName(group.getCourse().getId())),
                                    new CSVFile.CSVField(section.getTime() == null ? "" : section.getTime().getDayHeader() + " " + section.getTime().getStartTimeHeader(isUseAmPm()) + " - " + section.getTime().getEndTimeHeader(isUseAmPm())),
                                    new CSVFile.CSVField(sDF.format(100.0 * group.getSectionSpread(assignment, section))),
                                    new CSVFile.CSVField(Math.round(group.getSectionWeight(assignment, section, null))),
                                    new CSVFile.CSVField(section.getLimit())
                            });
                        }
                    }
        }
        
        return csv;
    }

}
