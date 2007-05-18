package net.sf.cpsolver.studentsct.heuristics.selection;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.heuristics.ValueSelection;
import net.sf.cpsolver.ifs.heuristics.VariableSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.SimpleNeighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;

/**
 * Standard value selection for some time
 */

public class StandardSelection implements NeighbourSelection {
    private long iIteration = 0;
    private ValueSelection iValueSelection = null;
    private VariableSelection iVariableSelection = null;
    
    public StandardSelection(DataProperties properties, VariableSelection variableSelection, ValueSelection valueSelection) {
        iVariableSelection = variableSelection;
        iValueSelection = valueSelection;
    }
    
    public void init(Solver solver) {
        iIteration = solver.currentSolution().getIteration();
    }
    
    public Neighbour selectNeighbour(Solution solution) {
        if (solution.getModel().unassignedVariables().isEmpty() || solution.getIteration()>=iIteration+solution.getModel().countVariables()) return null;
        for (int i=0;i<10;i++) {
            Request request = (Request)iVariableSelection.selectVariable(solution);
            Enrollment enrollment = (request==null?null:(Enrollment)iValueSelection.selectValue(solution, request));
            if (enrollment!=null && !enrollment.variable().getModel().conflictValues(enrollment).contains(enrollment))
                return new SimpleNeighbour(request, enrollment);
        }
        return null;
    }

}
