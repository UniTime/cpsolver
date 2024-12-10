package org.cpsolver.studentsct.filter;

import org.cpsolver.studentsct.StudentSectioningXMLLoader;
import org.cpsolver.studentsct.Test;
import org.cpsolver.studentsct.model.Student;

/**
 * Interface for filter students based on academic area classifications, majors,
 * or minors. This interface can be used by {@link StudentSectioningXMLLoader}
 * to load only subset of all students, and it is used by {@link Test} to
 * combine last-like and real students from two XML files.
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

public interface StudentFilter {
    /** Accept student 
     * @param student a student
     * @return true if the student is to be accepted (sectioned)*/
    public boolean accept(Student student);
    
    /**
     * Filter name
     */
    public String getName();
}
