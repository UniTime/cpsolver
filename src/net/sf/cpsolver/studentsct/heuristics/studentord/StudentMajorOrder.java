package net.sf.cpsolver.studentsct.heuristics.studentord;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.model.AcademicAreaCode;
import net.sf.cpsolver.studentsct.model.Student;

/** 
 * Return the given set of students ordered by their majors 
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
public class StudentMajorOrder implements StudentOrder, Comparator {
    private boolean iReverse = false;

    public StudentMajorOrder(DataProperties config) {
        iReverse = config.getPropertyBoolean("StudentMajorOrder.Reverse", iReverse);
    }

    /** Return the given set of students in a random order */
    public Vector order(Vector students) {
        Vector ret = new Vector(students);
        Collections.sort(ret, this);
        return ret;
    }
    
    public int compare(Object o1, Object o2) {
        Student s1 = (Student)o1;
        Student s2 = (Student)o2;
        int cmp = compareMajors(s1.getMajors(), s2.getMajors());
        if (cmp!=0) return (iReverse?-1:1)*cmp;
        return (iReverse?-1:1)*Double.compare(s1.getId(), s2.getId());
    }
    
    public int compareMajors(Vector m1, Vector m2) {
        if (m1.isEmpty()) {
            return m2.isEmpty()?0:-1; 
        } else if (m2.isEmpty()) return 1;
        return compareMajors((AcademicAreaCode)m1.firstElement(), (AcademicAreaCode)m2.firstElement());
    }
    
    public int compareMajors(AcademicAreaCode m1, AcademicAreaCode m2) {
        int cmp = (m1.getArea()==null?"":m1.getArea()).compareTo(m2.getArea()==null?"":m2.getArea());
        if (cmp!=0) return cmp;
        return (m1.getCode()==null?"":m1.getCode()).compareTo(m2.getCode()==null?"":m2.getCode());
    }
}
