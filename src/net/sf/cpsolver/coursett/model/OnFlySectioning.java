package net.sf.cpsolver.coursett.model;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.ModelListener;
import net.sf.cpsolver.ifs.solver.Solver;

/**
 * On fly student sectioning. <br>
 * <br>
 * In this mode, students are resectioned after each iteration, but only between
 * classes that are affected by the iteration. This slows down the solver, but
 * it can dramatically improve results in the case when there is more stress put
 * on student conflicts (e.g., Woebegon College example).
 * 
 * <br>
 * <br>
 * Parameters:
 * <table border='1'>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>OnFlySectioning.Enabled</td>
 * <td>{@link Boolean}</td>
 * <td>Enable on fly sectioning (if enabled, students will be resectioned after
 * each iteration)</td>
 * </tr>
 * <tr>
 * <td>OnFlySectioning.Recursive</td>
 * <td>{@link Boolean}</td>
 * <td>Recursively resection lectures affected by a student swap</td>
 * </tr>
 * <tr>
 * <td>OnFlySectioning.ConfigAsWell</td>
 * <td>{@link Boolean}</td>
 * <td>Resection students between configurations as well</td>
 * </tr>
 * </table>
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
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

public class OnFlySectioning implements ModelListener<Lecture, Placement> {
    private FinalSectioning iFinalSectioning;
    private boolean iRecursive = true;
    private boolean iConfigAsWell = false;

    /**
     * Constructor
     * 
     * @param model
     *            timetabling model
     */
    public OnFlySectioning(TimetableModel model) {
        iFinalSectioning = new FinalSectioning(model);
    }

    public void variableAdded(Lecture variable) {
    }

    public void variableRemoved(Lecture variable) {
    }

    public void constraintAdded(Constraint<Lecture, Placement> constraint) {
    }

    public void constraintRemoved(Constraint<Lecture, Placement> constraint) {
    }

    public void beforeAssigned(long iteration, Placement value) {
    }

    public void beforeUnassigned(long iteration, Placement value) {
    }

    /**
     * {@link FinalSectioning#resection(Lecture, boolean, boolean)} is called
     * when given iteration number is greater than zero.
     */
    public void afterAssigned(long iteration, Placement value) {
        if (iteration > 0)
            iFinalSectioning.resection(value.variable(), iRecursive, iConfigAsWell);
    }

    public void afterUnassigned(long iteration, Placement value) {
    }

    /**
     * Initialization
     */
    public boolean init(Solver<Lecture, Placement> solver) {
        iRecursive = solver.getProperties().getPropertyBoolean("OnFlySectioning.Recursive", true);
        iConfigAsWell = solver.getProperties().getPropertyBoolean("OnFlySectioning.ConfigAsWell", false);
        return true;
    }
}
