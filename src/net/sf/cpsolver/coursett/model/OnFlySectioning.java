package net.sf.cpsolver.coursett.model;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.ModelListener;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;

/**
 * On fly student sectioning.
 * <br><br>
 * In this mode, students are resectioned after each iteration, but only between classes that are affected by the iteration.
 * This slows down the solver, but it can dramatically improve results in the case when there is more stress put on student conflicts (e.g., Woebegon College example).
 * 
 * <br><br>
 * Parameters:
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>OnFlySectioning.Enabled</td><td>{@link Boolean}</td><td>Enable on fly sectioning (if enabled, students will be resectioned after each iteration)</td></tr>
 * <tr><td>OnFlySectioning.Recursive</td><td>{@link Boolean}</td><td>Recursively resection lectures affected by a student swap</td></tr>
 * <tr><td>OnFlySectioning.ConfigAsWell</td><td>{@link Boolean}</td><td>Resection students between configurations as well</td></tr>
 * </table>
 *
 * @version
 * CourseTT 1.1 (University Course Timetabling)<br>
 * Copyright (C) 2006 Tomas Muller<br>
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

public class OnFlySectioning implements ModelListener {
    private FinalSectioning iFinalSectioning;
    private boolean iRecursive = true;
    private boolean iConfigAsWell = false;
    
    /**
     * Constructor
     * @param model timetabling model
     */
    public OnFlySectioning(TimetableModel model) {
        iFinalSectioning = new FinalSectioning(model);
    }

    public void variableAdded(Variable variable) {}
    
    public void variableRemoved(Variable variable) {}
    
    public void constraintAdded(Constraint constraint) {}
    
    public void constraintRemoved(Constraint constraint) {}
    
    public void beforeAssigned(long iteration, Value value) {}

    public void beforeUnassigned(long iteration, Value value) {}
    
    /** 
     * {@link FinalSectioning#resection(Lecture, boolean, boolean)} is called when given iteration number 
     * is greater than zero. 
     */
    public void afterAssigned(long iteration, Value value) {
        if (iteration>0)
            iFinalSectioning.resection((Lecture)value.variable(), iRecursive, iConfigAsWell);
    }

    public void afterUnassigned(long iteration, Value value) {}

    /** 
     * Initialization
     */
    public boolean init(net.sf.cpsolver.ifs.solver.Solver solver) {
        iRecursive = solver.getProperties().getPropertyBoolean("OnFlySectioning.Recursive", true);
        iConfigAsWell = solver.getProperties().getPropertyBoolean("OnFlySectioning.ConfigAsWell", false);
        return true;
    }
}
