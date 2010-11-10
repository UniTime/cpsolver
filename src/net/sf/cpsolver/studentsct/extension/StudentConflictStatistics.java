package net.sf.cpsolver.studentsct.extension;

import net.sf.cpsolver.ifs.extension.ConflictStatistics;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * Same as {@link ConflictStatistics}, however, conflict with real students can
 * be weighted differently than with last-like students.
 * 
 * <br>
 * <br>
 * Parameters: <br>
 * <table border='1'>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>StudentConflictStatistics.RealStudentWeight</td>
 * <td>{@link Double}</td>
 * <td>
 * Weight of a conflict with a real student ({@link Student#isDummy()} is
 * false).</td>
 * </tr>
 * <tr>
 * <td>StudentConflictStatistics.RealStudentWeight</td>
 * <td>{@link Double}</td>
 * <td>
 * Weight of a conflict with a last-like student ({@link Student#isDummy()} is
 * true).</td>
 * </tr>
 * </table>
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
public class StudentConflictStatistics extends ConflictStatistics<Request, Enrollment> {
    public double iRealStudentWeight = 2.0;
    public double iDummyStudentWeight = 0.5;

    public StudentConflictStatistics(Solver<Request, Enrollment> solver, DataProperties properties) {
        super(solver, properties);
        iRealStudentWeight = properties.getPropertyDouble("StudentConflictStatistics.RealStudentWeight",
                iRealStudentWeight);
        iDummyStudentWeight = properties.getPropertyDouble("StudentConflictStatistics.DummyStudentWeight",
                iDummyStudentWeight);
    }

    @Override
    public double countRemovals(long iteration, Enrollment conflictValue, Enrollment value) {
        double ret = super.countRemovals(iteration, conflictValue, value);
        if (ret == 0.0)
            return ret;
        Enrollment conflict = conflictValue;
        /*
         * Enrollment enrollment = (Enrollment)value; if
         * (enrollment.getRequest()
         * .getStudent().isDummy()==conflict.getRequest()
         * .getStudent().isDummy()) return ret;
         */
        return ret * (conflict.getRequest().getStudent().isDummy() ? iDummyStudentWeight : iRealStudentWeight);
    }
}
