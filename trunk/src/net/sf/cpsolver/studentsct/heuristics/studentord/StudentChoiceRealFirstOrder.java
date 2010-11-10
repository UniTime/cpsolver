package net.sf.cpsolver.studentsct.heuristics.studentord;

import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * Return the given set of students in an order of average number of choices of
 * each student (students with more choices first), however, real student are
 * before last-like students. By default, the order is reversed (students with
 * less choices first).
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
public class StudentChoiceRealFirstOrder extends StudentChoiceOrder {

    public StudentChoiceRealFirstOrder(DataProperties config) {
        super(config);
    }

    public StudentChoiceRealFirstOrder() {
        super(new DataProperties());
        setReverse(true);
    }

    @Override
    public int compare(Student s1, Student s2) {
        if (s1.isDummy()) {
            if (!s2.isDummy())
                return 1;
        } else if (s2.isDummy())
            return -1;
        return super.compare(s1, s2);
    }

}
