package net.sf.cpsolver.studentsct;

import java.util.Iterator;

import net.sf.cpsolver.studentsct.model.AcademicAreaCode;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * This student filter accepts only freshman students.  
 * 
 * @version
 * StudentSct 1.1 (Student Sectioning)<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

public class FreshmanStudentFilter implements StudentFilter {
    /** 
     * Accept student. Student is accepted if it is freshmen, i.e., academic area classification 
     * code is A, 01, or 02.
     **/
    public boolean accept(Student student) {
        for (Iterator i=student.getAcademicAreaClasiffications().iterator();i.hasNext();) {
            AcademicAreaCode aac = (AcademicAreaCode)i.next();
            if ("A".equals(aac.getCode())) return true; //First Year
            if ("01".equals(aac.getCode())) return true; //First Semester Freshman
            if ("02".equals(aac.getCode())) return true; //Second Semester Freshman
        }
        return false;
    }

}
