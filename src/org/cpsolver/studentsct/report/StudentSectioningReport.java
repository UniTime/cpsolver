package org.cpsolver.studentsct.report;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.CSVFile;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.model.Course;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Student;

/**
 * Simple interface for student sectioning reports.
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
 *          Copyright (C) 2013 - 2014 Tomas Muller<br>
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
public interface StudentSectioningReport {
    public CSVFile create(Assignment<Request, Enrollment> assignment, DataProperties properties);
    
    /**
     * Report filtering interface
     */
    public interface Filter {
        /** Returns true if the given request matches the filter.
         * {@link #matches(Request, Enrollment)} is called with the current assignment.
         * */
        public boolean matches(Request r);
        /** Returns true if the given request with the given assignment matches the filter. */
        public boolean matches(Request r, Enrollment e);
        /** Returns true if the given student matches the filter.
         * This means that a student has a request that matches the filter. */
        public boolean matches(Student s);
        /** Returns true if the given course matches the filter.
         * This means that a student has a request for the course that matches the filter. */
        public boolean matches(Course c);
    }
}
