package net.sf.cpsolver.ifs.heuristics;

import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;

public interface NeighbourSelection {
    public void init(Solver solver);

    public Neighbour selectNeighbour(Solution solution);
	
}
