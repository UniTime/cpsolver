package org.cpsolver.studentsct.report;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.CSVFile;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Student;

/**
 * This reports lists all the requested courses and their properties.<br>
 * <br>
 * Namely:<ul>
 * <li><b>Alternative</b> is 1 when the course was requested as an alternative (<b>Primary</b> is 0 or <b>Alternativity</b> is above 0)</li>
 * <li><b>Enrolled</b> is 1 when the student is enrolled in the course</li>
 * <li><b>Primary</b> is 1 when the request is from the Course Requests table, 0 when it is from the Alternate Course Requests table</li>
 * <li><b>Priority</b> is the request's priority</li>
 * <li><b>Alternativity</b> is the request's alternativity (0 for the primary course, 1 for the first alternative, 2 for the second alternative, etc.)</li>
 * </ul>
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
public class RequestPriorityTable extends AbstractStudentSectioningReport {
    /**
     * Constructor
     * 
     * @param model
     *            student sectioning model
     */
    public RequestPriorityTable(StudentSectioningModel model) {
        super(model);
    }
    
    @Override
    public CSVFile createTable(Assignment<Request, Enrollment> assignment, DataProperties properties) {
        CSVFile csv = new CSVFile();
        csv.setHeader(new CSVFile.CSVField[] {
                new CSVFile.CSVField("__Student"),
                new CSVFile.CSVField("Student"),
                new CSVFile.CSVField("Course"),
                new CSVFile.CSVField("Alternative"),
                new CSVFile.CSVField("Enrolled"),
                new CSVFile.CSVField("Primary"),
                new CSVFile.CSVField("Priority"),
                new CSVFile.CSVField("Alternativity")
                });
        for (Student student: getModel().getStudents()) {
            if (student.isDummy()) continue;
            int regPriority = 1, altPriority = 1;
            for (Request r: student.getRequests()) {
                if (r instanceof CourseRequest) {
                    CourseRequest cr = (CourseRequest)r;
                    Enrollment e = cr.getAssignment(assignment);
                    if (!matches(cr, e)) continue;
                    int primary = (cr.isAlternative() ? 0 : 1);
                    int priority = 0;
                    if (cr.isAlternative())
                        priority = altPriority++;
                    else
                        priority = regPriority++;
                    int alternativity = 0;
                    for (Course course: cr.getCourses()) {
                        int alternative = (primary == 0 || alternativity > 0 ? 1 : 0);
                        int enrolled = (e != null && e.getCourse().equals(course) ? 1 : 0);
                        csv.addLine(new CSVFile.CSVField[] {
                                new CSVFile.CSVField(student.getId()),
                                new CSVFile.CSVField(student.getExternalId()),
                                new CSVFile.CSVField(cr.getCourses().get(alternativity).getName()),
                                new CSVFile.CSVField(alternative),
                                new CSVFile.CSVField(enrolled),
                                new CSVFile.CSVField(primary),
                                new CSVFile.CSVField(priority),
                                new CSVFile.CSVField(alternativity)
                        });
                        alternativity++;
                    }
                }
            }
        }
        return csv;
    }
}
