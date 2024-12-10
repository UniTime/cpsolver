package org.cpsolver.studentsct.report;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.CSVFile;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Request.RequestPriority;
import org.cpsolver.studentsct.model.Student;

/**
 * This reports lists critical courses and their assignments.<br>
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
public class CriticalCoursesTable extends AbstractStudentSectioningReport {
    /**
     * Constructor
     * 
     * @param model
     *            student sectioning model
     */
    public CriticalCoursesTable(StudentSectioningModel model) {
        super(model);
    }

    @Override
    public CSVFile createTable(Assignment<Request, Enrollment> assignment, DataProperties properties) {
        RequestPriority rp = RequestPriority.valueOf(properties.getProperty("priority", RequestPriority.Critical.name()));
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVFile.CSVField[] {
                new CSVFile.CSVField("__Student"),
                new CSVFile.CSVField("Student"),
                new CSVFile.CSVField("Priority"),
                new CSVFile.CSVField("Course"),
                new CSVFile.CSVField("1st Alt"),
                new CSVFile.CSVField("2nd Alt"),
                new CSVFile.CSVField("Enrolled"),
                new CSVFile.CSVField("Choice")
                });
        for (Student student: getModel().getStudents()) {
            if (student.isDummy()) continue;
            int priority = 0;
            for (Request r: student.getRequests()) {
                if (r instanceof CourseRequest) {
                    CourseRequest cr = (CourseRequest)r;
                    priority ++;
                    if (rp != cr.getRequestPriority() || cr.isAlternative()) continue;
                    Enrollment e = cr.getAssignment(assignment);
                    if (!matches(cr, e)) continue;
                    Course course = cr.getCourses().get(0);
                    Course alt1 = (cr.getCourses().size() < 2 ? null : cr.getCourses().get(1));
                    Course alt2 = (cr.getCourses().size() < 3 ? null : cr.getCourses().get(2));
                    Course enrolled = (e == null ? null : e.getCourse());
                    csv.addLine(new CSVFile.CSVField[] {
                            new CSVFile.CSVField(student.getId()),
                            new CSVFile.CSVField(student.getExternalId()),
                            new CSVFile.CSVField(priority),
                            new CSVFile.CSVField(course.getName()),
                            new CSVFile.CSVField(alt1 == null ? "" : alt1.getName()),
                            new CSVFile.CSVField(alt2 == null ? "" : alt2.getName()),
                            new CSVFile.CSVField(enrolled == null ? "" : enrolled.getName()),
                            new CSVFile.CSVField(enrolled == null ? "" : String.valueOf(cr.getCourses().indexOf(enrolled) + 1))
                    });
                }
            }
        }
        return csv;
    }
}
