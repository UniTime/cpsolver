package net.sf.cpsolver.studentsct.heuristics.selection;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.studentsct.heuristics.studentord.StudentOrder;
import net.sf.cpsolver.studentsct.heuristics.studentord.StudentRandomOrder;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * Resection studends with empty schedule.
 * An extension of {@link BranchBoundSelection}, where only students that have
 * no enrollments ({@link Student#nrAssignedRequests()} is zero) are 
 * resectioned.
 *  
 * <br><br>
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

public class ResectionUnassignedStudentsSelection extends BranchBoundSelection {
    private static Logger sLog = Logger.getLogger(ResectionUnassignedStudentsSelection.class);

    public ResectionUnassignedStudentsSelection(DataProperties properties) {
        super(properties);
        iOrder = new StudentRandomOrder(properties);
        if (properties.getProperty("Neighbour.ResectionUnassignedStudentsOrder")!=null) {
            try {
                iOrder = (StudentOrder)Class.forName(properties.getProperty("Neighbour.ResectionUnassignedStudentsOrder")).
                    getConstructor(new Class[] {DataProperties.class}).
                    newInstance(new Object[] {properties});
            } catch (Exception e) {
                sLog.error("Unable to set student order, reason:"+e.getMessage(),e);
            }
        }
    }
    
    public void init(Solver solver) {
        init(solver, "Resection unassigned students...");
    }
    
    /**
     * Select neighbour. All students with an empty schedule are taken, 
     * one by one in a random order. For each student a branch & bound search is employed. 
     */
    public Neighbour selectNeighbour(Solution solution) {
        while (iStudentsEnumeration.hasMoreElements()) {
            Student student = (Student)iStudentsEnumeration.nextElement();
            Progress.getInstance(solution.getModel()).incProgress();
            if (student.nrAssignedRequests()==0 && !student.getRequests().isEmpty()) {
                Neighbour neighbour = getSelection(student).select();
                if (neighbour!=null) return neighbour;
            }
        }
        return null;
    }
    
}
