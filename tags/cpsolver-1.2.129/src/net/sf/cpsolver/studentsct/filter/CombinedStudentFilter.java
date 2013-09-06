package net.sf.cpsolver.studentsct.filter;

import net.sf.cpsolver.studentsct.model.Student;

/**
 * This student filter combines two given student filters with logical operation
 * AND or OR.
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
public class CombinedStudentFilter implements StudentFilter {
    /** AND */
    public static final int OP_AND = 0;
    /** OR */
    public static final int OP_OR = 1;
    private StudentFilter iFirst, iSecond;
    private int iOp;

    /**
     * Constructor
     * 
     * @param first
     *            first filter
     * @param second
     *            second filter
     * @param op
     *            logical operation (either {@link CombinedStudentFilter#OP_AND}
     *            or {@link CombinedStudentFilter#OP_OR} )
     */
    public CombinedStudentFilter(StudentFilter first, StudentFilter second, int op) {
        iFirst = first;
        iSecond = second;
        iOp = op;
    }

    /**
     * A student is accepted if it is accepted by the first and/or the second
     * filter
     */
    @Override
    public boolean accept(Student student) {
        switch (iOp) {
            case OP_OR:
                return iFirst.accept(student) || iSecond.accept(student);
            case OP_AND:
            default:
                return iFirst.accept(student) && iSecond.accept(student);
        }
    }

}
