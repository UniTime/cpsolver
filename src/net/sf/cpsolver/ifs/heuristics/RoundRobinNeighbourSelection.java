package net.sf.cpsolver.ifs.heuristics;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * A round robin neighbour selection. Two or more {@link NeighbourSelection}
 * needs to be registered within the selection. This selection criterion takes
 * the registered neighbour selections one by one and performs
 * {@link NeighbourSelection#init(Solver)} and then it is using
 * {@link NeighbourSelection#selectNeighbour(Solution)} to select a neighbour.
 * When null is returned by the underlaying selection, next registered neighbour
 * selection is initialized and used for the following selection(s). If the last
 * registered selection returns null, the selection is returned to the first
 * registered neighbour selection (it is initialized before used again).
 * 
 * <br>
 * <br>
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 */

public class RoundRobinNeighbourSelection<V extends Variable<V, T>, T extends Value<V, T>> extends
        StandardNeighbourSelection<V, T> {
    private static Logger sLogger = Logger.getLogger(RoundRobinNeighbourSelection.class);
    private int iSelectionIdx = -1;
    private List<NeighbourSelection<V, T>> iSelections = new ArrayList<NeighbourSelection<V, T>>();
    private Solver<V, T> iSolver = null;

    /**
     * Constructor
     * 
     * @param properties
     *            configuration
     * @throws Exception
     */
    public RoundRobinNeighbourSelection(DataProperties properties) throws Exception {
        super(properties);
    }

    /** Register a neighbour selection */
    public void registerSelection(NeighbourSelection<V, T> selection) {
        iSelections.add(selection);
    }

    /** Initialization */
    @Override
    public void init(Solver<V, T> solver) {
        super.init(solver);
        iSolver = solver;
    }

    /**
     * Select neighbour. A first registered selections is initialized and used
     * until it returns null, then the second registered selections is
     * initialized and used and vice versa.
     */
    @Override
    public Neighbour<V, T> selectNeighbour(Solution<V, T> solution) {
        if (iSelectionIdx == -1) {
            iSelectionIdx = 0;
            iSelections.get(iSelectionIdx).init(iSolver);
        }
        while (true) {
            NeighbourSelection<V, T> selection = iSelections.get(iSelectionIdx);
            Neighbour<V, T> neighbour = selection.selectNeighbour(solution);
            if (neighbour != null)
                return neighbour;
            changeSelection(solution);
        }
    }

    /** Change selection */
    public void changeSelection(Solution<V, T> solution) {
        iSelectionIdx = (1 + iSelectionIdx) % iSelections.size();
        sLogger.debug("Phase changed to " + (iSelectionIdx + 1));
        iSelections.get(iSelectionIdx).init(iSolver);
    }
}
