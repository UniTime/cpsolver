package net.sf.cpsolver.studentsct.heuristics;

import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.heuristics.general.RoundRobinNeighbourSelection;
import net.sf.cpsolver.studentsct.heuristics.selection.BacktrackSelection;
import net.sf.cpsolver.studentsct.heuristics.selection.BranchBoundSelection;
import net.sf.cpsolver.studentsct.heuristics.selection.RandomUnassignmentSelection;
import net.sf.cpsolver.studentsct.heuristics.selection.ResectionIncompleteStudentsSelection;
import net.sf.cpsolver.studentsct.heuristics.selection.RndUnProblStudSelection;
import net.sf.cpsolver.studentsct.heuristics.selection.StandardSelection;
import net.sf.cpsolver.studentsct.heuristics.selection.SwapStudentSelection;

/**
 * (Batch) student sectioning neighbour selection.
 * It is based on {@link RoundRobinNeighbourSelection}, the following steps are involved:
 * <ul>
 * <li>Phase 1: section all students using incremental branch & bound (no unassignments) ({@link BranchBoundSelection} is used)
 * <li>Phase 2: pick a student (one by one) with an incomplete schedule, try to find an improvement ({@link SwapStudentSelection} is used)
 * <li>Phase 3: use standard value selection for some time ({@link StandardSelection} is used)
 * <li>Phase 4: use backtrack neighbour selection ({@link BacktrackSelection} is used)
 * <li>Phase 5: pick a student (one by one) with an incomplete schedule, try to find an improvement, identify problematic students
 * ({@link SwapStudentSelection} is used)
 * <li>Phase 6: random unassignment of some problematic students ({@link RndUnProblStudSelection} is used)
 * <li>Phase 7: resection incomplete students ({@link ResectionIncompleteStudentsSelection} is used)
 * <li>Phase 8: use standard value selection for some time ({@link StandardSelection} with {@link RouletteWheelRequestSelection} is used)
 * <li>Phase 9: random unassignment of some students ({@link RandomUnassignmentSelection} is used)
 * </ul>
 * 
 * <br><br>
 * 
 * @version
 * StudentSct 1.1 (Student Sectioning)<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@ktiml.mff.cuni.cz">muller@ktiml.mff.cuni.cz</a><br>
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

public class StudentSctNeighbourSelection extends RoundRobinNeighbourSelection {

    public StudentSctNeighbourSelection(DataProperties properties) throws Exception {
        super(properties);
    }
    
    public void init(Solver solver) {
        super.init(solver);
        setup(solver);
    }

    public void setup(Solver solver) {
        //Phase 1: section all students using incremental branch & bound (no unassignments)
        registerSelection(new BranchBoundSelection(solver.getProperties()));

        //Phase 2: pick a student (one by one) with an incomplete schedule, try to find an improvement
        registerSelection(new SwapStudentSelection(solver.getProperties()));

        //Phase 3: use standard value selection for some time
        registerSelection(new StandardSelection(solver.getProperties(), getVariableSelection(), getValueSelection())); 

        //Phase 4: use backtrack neighbour selection
        registerSelection(new BacktrackSelection(solver.getProperties()));
        
        //Phase 5: pick a student (one by one) with an incomplete schedule, try to find an improvement, identify problematic students
        SwapStudentSelection swapStudentSelection = new SwapStudentSelection(solver.getProperties());
        registerSelection(swapStudentSelection);
            
        //Phase 6: random unassignment of some problematic students
        registerSelection(new RndUnProblStudSelection(solver.getProperties(), swapStudentSelection));
        
        //Phase 7: resection incomplete students 
        registerSelection(new ResectionIncompleteStudentsSelection(solver.getProperties()));
           
        //Phase 8: use standard value selection for some time
        registerSelection(new StandardSelection(solver.getProperties(), new RouletteWheelRequestSelection(solver.getProperties()), getValueSelection()));
        
        //Phase 9: random unassignment of some students
        registerSelection(new RandomUnassignmentSelection(solver.getProperties()));
    }
    
}
