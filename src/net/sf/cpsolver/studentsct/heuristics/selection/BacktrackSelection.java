package net.sf.cpsolver.studentsct.heuristics.selection;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.heuristics.RandomizedBacktrackNeighbourSelection;
import net.sf.cpsolver.studentsct.model.Request;

/**
 * Use backtrack neighbour selection
 *
 */

public class BacktrackSelection implements NeighbourSelection {
    private RandomizedBacktrackNeighbourSelection iRBtNSel = null;
    private Enumeration iRequestEnumeration = null;

    public BacktrackSelection(DataProperties properties) {
    }

    public void init(Solver solver) {
        Vector unassigned = new Vector(solver.currentSolution().getModel().unassignedVariables());
        Collections.shuffle(unassigned);
        iRequestEnumeration = unassigned.elements();
        if (iRBtNSel==null) {
            try {
                iRBtNSel = new RandomizedBacktrackNeighbourSelection(solver.getProperties());
                iRBtNSel.init(solver);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(),e);
            }
        }
    }
    
    public Neighbour selectNeighbour(Solution solution) {
        while (iRequestEnumeration.hasMoreElements()) {
            Request request = (Request)iRequestEnumeration.nextElement();
            Neighbour n = iRBtNSel.selectNeighbour(solution, request);
            if (n!=null) return n;
        }
        return null;
    }
    
}
