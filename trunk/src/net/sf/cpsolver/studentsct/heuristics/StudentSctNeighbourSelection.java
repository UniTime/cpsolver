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
