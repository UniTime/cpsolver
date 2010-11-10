package net.sf.cpsolver.studentsct.heuristics.studentord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * Return the given set of students in a random order, however, all real
 * students before last-like students.
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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 */
public class StudentRandomRealFirstOrder implements StudentOrder {

    public StudentRandomRealFirstOrder(DataProperties config) {
    }

    public StudentRandomRealFirstOrder() {
    }

    /**
     * Return the given set of students in a random order, however, all real
     * students before last-like ({@link Student#isDummy()} is true) students.
     **/
    public List<Student> order(List<Student> students) {
        List<Student> real = new ArrayList<Student>(students.size());
        List<Student> dummy = new ArrayList<Student>(students.size());
        for (Student student : students) {
            if (student.isDummy())
                dummy.add(student);
            else
                real.add(student);
        }
        Collections.shuffle(dummy);
        Collections.shuffle(real);
        dummy.addAll(real);
        return dummy;
    }

}
