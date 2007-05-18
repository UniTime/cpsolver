package net.sf.cpsolver.studentsct.heuristics.general;

import java.util.Vector;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.heuristics.StandardNeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;

public class RoundRobinNeighbourSelection extends StandardNeighbourSelection {
    private static Logger sLog = Logger.getLogger(RoundRobinNeighbourSelection.class);
    private int iSelectionIdx = -1;
    private Vector iSelections = new Vector();
    private Solver iSolver = null;
    
    public RoundRobinNeighbourSelection(DataProperties properties) throws Exception {
        super(properties);
    }
    
    public void registerSelection(NeighbourSelection selection) {
        iSelections.add(selection);
    }
    
    public void init(Solver solver) {
        super.init(solver);
        iSolver = solver;
    }
    
    public Neighbour selectNeighbour(Solution solution) {
        if (iSelectionIdx==-1) {
            iSelectionIdx = 0;
            ((NeighbourSelection)iSelections.elementAt(iSelectionIdx)).init(iSolver);
        }
        while (true) {
            NeighbourSelection selection = (NeighbourSelection)iSelections.elementAt(iSelectionIdx);
            Neighbour neighbour = selection.selectNeighbour(solution);
            if (neighbour!=null) return neighbour;
            iSelectionIdx = (1+iSelectionIdx) % iSelections.size();
            sLogger.debug("Phase changed to "+(iSelectionIdx+1));
            ((NeighbourSelection)iSelections.elementAt(iSelectionIdx)).init(iSolver);
        }
    }
}
