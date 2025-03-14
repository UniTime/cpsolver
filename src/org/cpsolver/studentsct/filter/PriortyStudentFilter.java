package org.cpsolver.studentsct.filter;

import org.cpsolver.studentsct.model.Student;
import org.cpsolver.studentsct.model.Student.StudentPriority;

/**
 * This student filter only accepts students that are flagged as priority students.
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
public class PriortyStudentFilter implements StudentFilter  {
    StudentPriority iPriority = StudentPriority.Priority;
    boolean iIncludeHigherPriority = true;
    
    public PriortyStudentFilter(StudentPriority priority, boolean includeHigherPriority) {
        iPriority = priority; iIncludeHigherPriority = includeHigherPriority;
    }

    @Override
    public boolean accept(Student student) {
        if (student.isDummy()) return false;
        if (iIncludeHigherPriority) {
            return iPriority.isSameOrHigher(student);
        } else {
            return iPriority.isSame(student);
        }
    }
    
    @Override
    public String getName() {
        return iPriority.name() + (iIncludeHigherPriority ? "+" : "");
    }

}
