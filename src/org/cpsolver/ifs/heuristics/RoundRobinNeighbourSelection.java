package org.cpsolver.ifs.heuristics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.cpsolver.ifs.model.InfoProvider;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.Progress;

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
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2014 Tomas Muller<br>
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
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 *
 * @param <V> Variable
 * @param <T> Value
 */
public class RoundRobinNeighbourSelection<V extends Variable<V, T>, T extends Value<V, T>> extends StandardNeighbourSelection<V, T> {
    protected static Logger sLogger = org.apache.logging.log4j.LogManager.getLogger(RoundRobinNeighbourSelection.class);
    private int iSelectionIdx = -1;
    private List<NeighbourSelection<V, T>> iSelections = new ArrayList<NeighbourSelection<V, T>>();
    protected Solver<V, T> iSolver = null;

    /**
     * Constructor
     * 
     * @param properties
     *            configuration
     * @throws Exception thrown when initialization fails
     */
    public RoundRobinNeighbourSelection(DataProperties properties) throws Exception {
        super(properties);
    }

    /** Register a neighbour selection 
     * @param selection a neighbour selection to include in the selection
     **/
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
        int nrChanges = 0;
        while (nrChanges <= iSelections.size()) {
            int selectionIndex = getSelectionIndex();
            NeighbourSelection<V, T> selection = iSelections.get(selectionIndex % iSelections.size());
            Neighbour<V, T> neighbour = selection.selectNeighbour(solution);
            if (neighbour != null)
                return neighbour;
            changeSelection(selectionIndex);
            nrChanges ++;
        }
        return null;
    }
    
    public int getSelectionIndex() {
        if (iSelectionIdx == -1) changeSelection(-1);
        iSolver.currentSolution().getLock().readLock().lock();
        try {
            return iSelectionIdx;
        } finally {
            iSolver.currentSolution().getLock().readLock().unlock();
        }
    }

    /** Change selection 
     * @param selectionIndex current selection index 
     **/
    @SuppressWarnings("unchecked")
    public void changeSelection(int selectionIndex) {
        iSolver.currentSolution().getLock().writeLock().lock();
        try {
            Progress progress = Progress.getInstance(iSolver.currentSolution().getModel());
            int newSelectionIndex = 1 + selectionIndex;
            if (newSelectionIndex <= iSelectionIdx) return; // already changed
            if (selectionIndex == -1 && iSelectionIdx >= 0) return; // already changed
            iSelectionIdx = newSelectionIndex;
            if (selectionIndex >= 0) {
                try {
                    NeighbourSelection<V, T> selection = iSelections.get(selectionIndex % iSelections.size());
                    if (selection instanceof InfoProvider) {
                        Map<String, String> info = new HashMap<String, String>();
                        ((InfoProvider<V, T>)selection).getInfo(iSolver.currentSolution().getAssignment(), info);
                        if (!info.isEmpty())
                            for (Map.Entry<String, String> e: info.entrySet())
                                progress.debug(e.getKey() + ": " + e.getValue());
                    }
                } catch (Exception e) {}
            }
            sLogger.info("Phase changed to " + ((newSelectionIndex % iSelections.size()) + 1));
            progress.debug(iSolver.currentSolution().toString());
            if (iSolver.currentSolution().getBestInfo() == null || iSolver.getSolutionComparator().isBetterThanBestSolution(iSolver.currentSolution()))
                iSolver.currentSolution().saveBest();
            iSelections.get(iSelectionIdx % iSelections.size()).init(iSolver);
        } finally {
            iSolver.currentSolution().getLock().writeLock().unlock();
        }
    }
    
    public NeighbourSelection<V, T> getSelection() {
        return iSelections.get(getSelectionIndex() % iSelections.size());
    }
}
