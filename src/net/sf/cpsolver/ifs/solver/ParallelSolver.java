package net.sf.cpsolver.ifs.solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.cpsolver.ifs.assignment.Assignment;
import net.sf.cpsolver.ifs.assignment.DefaultParallelAssignment;
import net.sf.cpsolver.ifs.assignment.DefaultSingleAssignment;
import net.sf.cpsolver.ifs.assignment.InheritedAssignment;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solution.SolutionListener;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.JProf;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Multi-threaded solver. Instead of one, a given number of solver threads are created
 * (as defined by Parallel.NrSolvers property) and started in parallel. Each thread
 * works with its own assignment {@link DefaultParallelAssignment}, but the best solution
 * is shared among all of them.<br>
 * <br>
 * When {@link DefaultSingleAssignment} is given to the solver, only one solution is used.
 * A neighbour is assigned to this (shared) solution when it does not create any conflicts
 * outside of {@link Neighbour#assignments()}.
 * 
 * @see Solver
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2014 Tomas Muller<br>
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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 **/
public class ParallelSolver<V extends Variable<V, T>, T extends Value<V, T>> extends Solver<V, T> {
    private SynchronizationThread iSynchronizationThread = null;
    private int iNrFinished = 0;
    
    public ParallelSolver(DataProperties properties) {
        super(properties);
    }
    
    /** Starts solver */
    @Override
    public void start() {
        int nrSolvers = Math.abs(getProperties().getPropertyInt("Parallel.NrSolvers", 4));
        if (nrSolvers == 1) {
            super.start();
        } else {
            iSynchronizationThread = new SynchronizationThread(nrSolvers);
            iSynchronizationThread.setPriority(THREAD_PRIORITY);
            iSynchronizationThread.start();
        }
    }
    
    /** Returns solver's thread */
    @Override
    public Thread getSolverThread() {
        return iSynchronizationThread != null ? iSynchronizationThread : super.getSolverThread();
    }
    
    /**
     * Synchronization thread
     */
    protected class SynchronizationThread extends Thread {
        private int iNrSolvers;
        private List<SolverThread> iSolvers = new ArrayList<SolverThread>();
        
        SynchronizationThread(int nrSolvers) {
            iNrSolvers = nrSolvers;
        }
        
        @Override
        public void run() {
            iStop = false;
            iNrFinished = 0;
            setName("SolverSync");
            
            // Initialization
            iProgress = Progress.getInstance(currentSolution().getModel());
            iProgress.setStatus("Solving problem ...");
            iProgress.setPhase("Initializing solver");
            initSolver();
            onStart();
            
            double startTime = JProf.currentTimeSec();
            if (isUpdateProgress()) {
                if (currentSolution().getBestInfo() == null) {
                    iProgress.setPhase("Searching for initial solution ...", currentSolution().getModel().variables().size());
                } else {
                    iProgress.setPhase("Improving found solution ...");
                }
            }
            sLogger.info("Initial solution:" + ToolBox.dict2string(currentSolution().getInfo(), 2));
            if ((iSaveBestUnassigned < 0 || iSaveBestUnassigned >= currentSolution().getAssignment().nrUnassignedVariables(currentSolution().getModel())) && (currentSolution().getBestInfo() == null || getSolutionComparator().isBetterThanBestSolution(currentSolution()))) {
                if (currentSolution().getAssignment().nrAssignedVariables() == currentSolution().getModel().variables().size())
                    sLogger.info("Complete solution " + ToolBox.dict2string(currentSolution().getInfo(), 1) + " was found.");
                synchronized (currentSolution()) {
                    currentSolution().saveBest();
                }
            }

            if (currentSolution().getModel().variables().isEmpty()) {
                iProgress.error("Nothing to solve.");
                iStop = true;
            }
            
            if (!iStop) {
                for (int i = 0; i < iNrSolvers; i++) {
                    SolverThread thread = new SolverThread(i, startTime);
                    // thread.setPriority(THREAD_PRIORITY);
                    thread.setName("Solver-" + (1 + i));
                    thread.start();
                    iSolvers.add(thread);
                }
            }
            
            int prog = 9999;
            while (!iStop && iNrFinished < iNrSolvers) {
                try {
                    Thread.sleep(1000);
                    
                    // Increment progress bar
                    if (isUpdateProgress()) {
                        if (iCurrentSolution.getBestInfo() != null && currentSolution().getModel().getBestUnassignedVariables() == 0) {
                            prog++;
                            if (prog == 10000) {
                                iProgress.setPhase("Improving found solution ...");
                                prog = 0;
                            } else {
                                iProgress.setProgress(prog / 100);
                            }
                        } else if ((currentSolution().getBestInfo() == null || currentSolution().getModel().getBestUnassignedVariables() > 0) && (currentSolution().getAssignment().nrAssignedVariables() > iProgress.getProgress())) {
                            iProgress.setProgress(currentSolution().getAssignment().nrAssignedVariables());
                        }
                    }
                } catch (InterruptedException e) {}
            }
            
            boolean stop = iStop; iStop = true;
            for (SolverThread thread: iSolvers) {
                try {
                    thread.join();
                } catch (InterruptedException e) {}
            }
            
            // Finalization
            iLastSolution = iCurrentSolution;

            iProgress.setPhase("Done", 1);
            iProgress.incProgress();

            iSynchronizationThread = null;
            if (stop) {
                sLogger.debug("Solver stopped.");
                iProgress.setStatus("Solver stopped.");
                onStop();
            } else {
                sLogger.debug("Solver done.");
                iProgress.setStatus("Solver done.");
                onFinish();
            }
        }
    }
    
    /**
     * Create a solution that is to be used by a solver thread of the given index
     */
    protected Solution<V, T> createParallelSolution(int index) {
        Model<V, T> model = currentSolution().getModel();
        Assignment<V, T> assignment = new DefaultParallelAssignment<V, T>(index, currentSolution().getAssignment());
        Solution<V, T> solution = new Solution<V, T>(model, assignment);
        for (SolutionListener<V, T> listener: currentSolution().getSolutionListeners())
            solution.addSolutionListener(listener);
        return solution;
    }
    
    /**
     * Returns true if the solver works only with one solution (regardless the number of threads it is using)
     * @return true if the current solution is {@link DefaultSingleAssignment}
     */
    @Override
    public boolean hasSingleSolution() {
        return currentSolution().getAssignment() instanceof DefaultSingleAssignment;
    }
    
    /**
     * Solver thread
     */
    protected class SolverThread extends Thread {
        private double iStartTime;
        private int iIndex;
        
        public SolverThread(int index, double startTime) {
            iIndex = index;
            iStartTime = startTime;
        }
        
        @Override
        public void run() {
            try {
                boolean single = hasSingleSolution();
                Model<V, T> model = currentSolution().getModel();
                Solution<V, T> solution = (single ? currentSolution() : createParallelSolution(1 + iIndex));
                Assignment<V, T> assignment = solution.getAssignment();

                while (!iStop) {
                    // Break if cannot continue
                    if (!getTerminationCondition().canContinue(solution)) break;
                    
                    // Create a sub-solution if needed
                    Solution<V, T> current = (single ? new Solution<V, T>(model, new InheritedAssignment<V, T>(solution.getAssignment()), solution.getIteration(), solution.getTime()) : solution);

                    // Neighbour selection
                    Neighbour<V, T> neighbour = null;
                    try {
                        neighbour = getNeighbourSelection().selectNeighbour(current);
                    } catch (Exception e) {
                        sLogger.warn("Failed to select a neighbour: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()), e);
                    }
                    for (SolverListener<V, T> listener : iSolverListeners) {
                        if (!listener.neighbourSelected(assignment, solution.getIteration(), neighbour)) {
                            neighbour = null;
                            continue;
                        }
                    }

                    double time = JProf.currentTimeSec() - iStartTime;
                    if (neighbour == null) {
                        sLogger.debug("No neighbour selected.");
                        // still update the solution (increase iteration etc.)
                        solution.update(time, false);
                        continue;
                    }
                    
                    if (single) {
                        Map<V, T> assignments = null;
                        try {
                            assignments = neighbour.assignments();
                        } catch (UnsupportedOperationException e) {
                            sLogger.error("Failed to enumerate " + neighbour.getClass().getSimpleName(), e);
                        }
                        if (assignments == null) {
                            sLogger.debug("No assignments returned.");
                            // still update the solution (increase iteration etc.)
                            solution.update(time, false);
                            continue;
                        }
                        
                        // Assign selected value to the selected variable
                        synchronized (solution) {
                            Map<V, T> undo = new HashMap<V, T>();
                            for (V var: assignments.keySet())
                                undo.put(var, solution.getAssignment().unassign(solution.getIteration(), var));
                            boolean fail = false;
                            for (T val: assignments.values()) {
                                if (val == null) continue;
                                if (model.inConflict(solution.getAssignment(), val)) {
                                    fail = true; break;
                                }
                                solution.getAssignment().assign(solution.getIteration(), val);
                            }
                            if (fail) {
                                for (V var: undo.keySet())
                                    solution.getAssignment().unassign(solution.getIteration(), var);
                                for (T val: undo.values())
                                    if (val != null)
                                        solution.getAssignment().assign(solution.getIteration(), val);
                            }
                            solution.update(time, !fail);
                            if (fail) {
                                for (SolverListener<V, T> listener : iSolverListeners)
                                    listener.neighbourFailed(current.getAssignment(), solution.getIteration(), neighbour);
                                continue;
                            }

                            if ((iSaveBestUnassigned < 0 || iSaveBestUnassigned >= solution.getAssignment().nrUnassignedVariables(model)) && getSolutionComparator().isBetterThanBestSolution(solution)) {
                                solution.saveBest();
                            }
                        }
                    } else {
                        // Assign selected value to the selected variable
                        neighbour.assign(assignment, solution.getIteration());
                        solution.update(time);
                        
                        if ((iSaveBestUnassigned < 0 || iSaveBestUnassigned >= assignment.nrUnassignedVariables(model)) && getSolutionComparator().isBetterThanBestSolution(solution)) {
                            solution.saveBest(currentSolution());
                        }
                    }
                }

            } catch (Exception ex) {
                sLogger.error(ex.getMessage(), ex);
                iProgress.fatal(getName() + " failed, reason:" + ex.getMessage(), ex);
                if (iIndex == 0) {
                    iProgress.setStatus("Solver failed.");
                    onFailure();
                }
            }
            synchronized (currentSolution()) {
                iNrFinished ++;
            }
        }
        
    }
}
