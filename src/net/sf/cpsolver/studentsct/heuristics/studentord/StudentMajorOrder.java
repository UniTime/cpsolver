package net.sf.cpsolver.studentsct.heuristics.studentord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.model.AcademicAreaCode;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * Return the given set of students ordered by their majors
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
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
public class StudentMajorOrder implements StudentOrder, Comparator<Student> {
    private boolean iReverse = false;

    public StudentMajorOrder(DataProperties config) {
        iReverse = config.getPropertyBoolean("StudentMajorOrder.Reverse", iReverse);
    }

    /** Order the given list of students */
    public List<Student> order(List<Student> students) {
        List<Student> ret = new ArrayList<Student>(students);
        Collections.sort(ret, this);
        return ret;
    }

    public int compare(Student s1, Student s2) {
        int cmp = compareMajors(s1.getMajors(), s2.getMajors());
        if (cmp != 0)
            return (iReverse ? -1 : 1) * cmp;
        return (iReverse ? -1 : 1) * Double.compare(s1.getId(), s2.getId());
    }

    public int compareMajors(List<AcademicAreaCode> m1, List<AcademicAreaCode> m2) {
        if (m1.isEmpty()) {
            return m2.isEmpty() ? 0 : -1;
        } else if (m2.isEmpty())
            return 1;
        return compareMajors(m1.get(0), m2.get(0));
    }

    public int compareMajors(AcademicAreaCode m1, AcademicAreaCode m2) {
        int cmp = (m1.getArea() == null ? "" : m1.getArea()).compareTo(m2.getArea() == null ? "" : m2.getArea());
        if (cmp != 0)
            return cmp;
        return (m1.getCode() == null ? "" : m1.getCode()).compareTo(m2.getCode() == null ? "" : m2.getCode());
    }
}
