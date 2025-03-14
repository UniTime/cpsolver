package org.cpsolver.studentsct.check;

import java.util.HashMap;

import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.SctAssignment;
import org.cpsolver.studentsct.model.Student;


/**
 * This class looks and reports cases when a student is enrolled into two
 * sections that are overlapping in time.
 * 
 * <br>
 * <br>
 * 
 * Usage: if (new OverlapCheck(model).check()) ...
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
public class OverlapCheck {
    private static org.apache.logging.log4j.Logger sLog = org.apache.logging.log4j.LogManager.getLogger(OverlapCheck.class);
    private StudentSectioningModel iModel;

    /**
     * Constructor
     * 
     * @param model
     *            student sectioning model
     */
    public OverlapCheck(StudentSectioningModel model) {
        iModel = model;
    }

    /** Return student sectioning model 
     * @return problem model
     **/
    public StudentSectioningModel getModel() {
        return iModel;
    }

    /**
     * Check for overlapping sections that are attended by the same student
     * @param a current assignment
     * @return false, if there is such a case
     */
    public boolean check(Assignment<Request, Enrollment> a) {
        sLog.info("Checking for overlaps...");
        boolean ret = true;
        for (Student student : getModel().getStudents()) {
            HashMap<TimeLocation, SctAssignment> times = new HashMap<TimeLocation, SctAssignment>();
            for (Request request : student.getRequests()) {
                Enrollment enrollment = a.getValue(request);
                if (enrollment == null)
                    continue;
                for (SctAssignment assignment : enrollment.getAssignments()) {
                    if (assignment.getTime() == null)
                        continue;
                    for (TimeLocation time: times.keySet()) {
                        if (time.hasIntersection(assignment.getTime())) {
                            sLog.error("Student " + student + " assignment " + assignment + " overlaps with "
                                    + times.get(time));
                            ret = false;
                        }
                    }
                    times.put(assignment.getTime(), assignment);
                }
            }
        }
        return ret;
    }

}
