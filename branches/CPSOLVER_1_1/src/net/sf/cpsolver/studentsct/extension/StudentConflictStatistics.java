package net.sf.cpsolver.studentsct.extension;

import net.sf.cpsolver.ifs.extension.ConflictStatistics;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * Same as {@link ConflictStatistics}, however, conflict with real students 
 * can be weighted differently than with last-like students.
 * 
 * <br><br>
 * Parameters:
 * <br>
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>StudentConflictStatistics.RealStudentWeight</td><td>{@link Double}</td><td>
 * Weight of a conflict with a real student ({@link Student#isDummy()} is false). 
 * </td></tr>
 * <tr><td>StudentConflictStatistics.RealStudentWeight</td><td>{@link Double}</td><td>
 * Weight of a conflict with a last-like student ({@link Student#isDummy()} is true). 
 * </td></tr>
 * </table>
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
public class StudentConflictStatistics extends ConflictStatistics {
    public double iRealStudentWeight = 2.0;
    public double iDummyStudentWeight = 0.5;

    public StudentConflictStatistics(Solver solver, DataProperties properties) {
        super (solver, properties);
        iRealStudentWeight = properties.getPropertyDouble("StudentConflictStatistics.RealStudentWeight", iRealStudentWeight);
        iDummyStudentWeight = properties.getPropertyDouble("StudentConflictStatistics.DummyStudentWeight", iDummyStudentWeight);
    }
    
    public double countRemovals(long iteration, Value conflictValue, Value value) {
        double ret = super.countRemovals(iteration, conflictValue, value);
        if (ret==0.0) return ret;
        Enrollment conflict = (Enrollment)conflictValue;
        /*
        Enrollment enrollment = (Enrollment)value;
        if (enrollment.getRequest().getStudent().isDummy()==conflict.getRequest().getStudent().isDummy())
            return ret;
        */
        return ret * (conflict.getRequest().getStudent().isDummy()?iDummyStudentWeight:iRealStudentWeight); 
    }
}
