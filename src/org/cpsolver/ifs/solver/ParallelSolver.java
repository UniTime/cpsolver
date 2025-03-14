package org.cpsolver.ifs.solver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.DefaultParallelAssignment;
import org.cpsolver.ifs.assignment.DefaultSingleAssignment;
import org.cpsolver.ifs.assignment.context.CanHoldContext;
import org.cpsolver.ifs.model.LazyNeighbour;
import org.cpsolver.ifs.model.LazyNeighbour.LazyNeighbourAcceptanceCriterion;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solution.SolutionListener;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.JProf;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.ifs.util.ToolBox;


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
 * @author  Tomas Muller
 * @version IFS 1.3 (Iterative Forward Search)<br>
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
 *          License along with this library; if not see <a href='http://www.gnu.org/licenses'>http://www.gnu.org/licenses</a>.
 *
 * @param <V> Variable
 * @param <T> Value
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
        int nrSolvers = Math.min(Math.abs(getProperties().getPropertyInt("Parallel.NrSolvers", 4)), CanHoldContext.sMaxSize - 1);
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
    
    /** Sets initial solution */
    @Override
    public void setInitalSolution(Model<V, T> model) {
        int nrSolvers = Math.min(Math.abs(getProperties().getPropertyInt("Parallel.NrSolvers", 4)), CanHoldContext.sMaxSize - 1);
        boolean updateMasterSolution = getProperties().getPropertyBoolean("Parallel.UpdateMasterSolution", true);
        setInitalSolution(new Solution<V, T>(model, nrSolvers > 1 ? new DefaultParallelAssignment<V, T>(updateMasterSolution ? 1 : 0) : new DefaultSingleAssignment<V, T>(), 0, 0));
    }
    
    /**
     * Return a working (parallel) solution that contributed to the best solution last.
     * @return working solution
     */
    protected Solution<V, T> getWorkingSolution() {
        if (iSynchronizationThread != null && !hasSingleSolution()) {
            int idx = currentSolution().getBestIndex();
            if (idx < 0) idx = 0; // take the first thread solution if there was no best solution saved yet
            if (idx < iSynchronizationThread.iSolvers.size())
                return iSynchronizationThread.iSolvers.get(idx).iSolution;
        }
        return currentSolution();
    }
    
    /**
     * Synchronization thread
     */
    protected class SynchronizationThread extends Thread {
        private int iNrSolvers;
        private List<SolverThread> iSolvers = new ArrayList<SolverThread>();
        private AssignmentThread iAssignmentThread = null;
        
        SynchronizationThread(int nrSolvers) {
            iNrSolvers = nrSolvers;
        }
        
        @Override
        public void run() {
            try {
                iStop = false;
                iNrFinished = 0;
                setName("SolverSync");
                
                // Initialization
                iProgress = Progress.getInstance(currentSolution().getModel());
                iProgress.setStatus("Solving problem ...");
                iProgress.setPhase("Initializing solver");
                initSolver();
                onStart();
                
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
                    currentSolution().saveBest();
                }

                if (currentSolution().getModel().variables().isEmpty()) {
                    iProgress.error("Nothing to solve.");
                    iStop = true;
                }
                
                BlockingQueue<Neighbour<V, T>> queue = null;
                if (hasSingleSolution() && iNrSolvers > 1 && getProperties().getPropertyBoolean("ParallelSolver.SingleSolutionQueue", false))
                    queue = new ArrayBlockingQueue<Neighbour<V, T>>(2 * iNrSolvers);
                
                if (!iStop) {
                    for (int i = 1; i <= iNrSolvers; i++) {
                        SolverThread thread = new SolverThread(i, queue);
                        thread.setPriority(THREAD_PRIORITY);
                        thread.setName("Solver-" + i);
                        thread.start();
                        iSolvers.add(thread);
                    }
                }
                
                if (queue != null) {
                    iAssignmentThread = new AssignmentThread(queue);
                    iAssignmentThread.start();
                }
                
                int timeout = getProperties().getPropertyInt("Termination.TimeOut", 1800);
                double start = JProf.currentTimeSec();
                while (!iStop && iNrFinished < iNrSolvers) {
                    try {
                        Thread.sleep(1000);
                        double time = JProf.currentTimeSec() - start;
                        
                        // Increment progress bar
                        if (isUpdateProgress()) {
                            if (currentSolution().getBestInfo() != null && currentSolution().getModel().getBestUnassignedVariables() == 0) {
                                if (!"Improving found solution ...".equals(iProgress.getPhase()))
                                    iProgress.setPhase("Improving found solution ...");
                                iProgress.setProgress(Math.min(100, (int)Math.round(100 * time / timeout)));
                            } else if (currentSolution().getModel().getBestUnassignedVariables() > 0 && (currentSolution().getModel().countVariables() - currentSolution().getModel().getBestUnassignedVariables() > iProgress.getProgress())) {
                                iProgress.setProgress(currentSolution().getModel().countVariables() - currentSolution().getModel().getBestUnassignedVariables());
                            } else if (iSolvers.get(0).iAssignment.nrAssignedVariables() > iProgress.getProgress()) {
                                iProgress.setProgress(iSolvers.get(0).iAssignment.nrAssignedVariables());
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
                if (iAssignmentThread != null) {
                    try {
                        iAssignmentThread.join();
                    } catch (InterruptedException e) {}
                }
                
                // Finalization
                iLastSolution = iCurrentSolution;

                iProgress.setPhase("Done", 1);
                iProgress.incProgress();

                iStop = stop;
                if (iStop) {
                    sLogger.debug("Solver stopped.");
                    iProgress.setStatus("Solver stopped.");
                    onStop();
                } else {
                    sLogger.debug("Solver done.");
                    iProgress.setStatus("Solver done.");
                    onFinish();
                }
            } catch (Exception ex) {
                sLogger.error(ex.getMessage(), ex);
                iProgress.fatal("Solver synchronization failed, reason:" + ex.getMessage(), ex);
                iProgress.setStatus("Solver failed.");
                onFailure();
            } finally {
                iSynchronizationThread = null;
            }
        }
    }
    
    /**
     * Create a solution that is to be used by a solver thread of the given index
     * @param index solver thread index
     * @return new solution to work with
     */
    protected Solution<V, T> createParallelSolution(int index) {
        Model<V, T> model = iCurrentSolution.getModel();
        Assignment<V, T> assignment = new DefaultParallelAssignment<V, T>(index, model, iCurrentSolution.getAssignment());
        model.createAssignmentContexts(assignment, true);
        Solution<V, T> solution = new Solution<V, T>(model, assignment);
        for (SolutionListener<V, T> listener: iCurrentSolution.getSolutionListeners())
            solution.addSolutionListener(listener);
        return solution;
    }
    
    /**
     * Returns true if the solver works only with one solution (regardless the number of threads it is using)
     * @return true if the current solution is {@link DefaultSingleAssignment}
     */
    @Override
    public boolean hasSingleSolution() {
        return iCurrentSolution.getAssignment() instanceof DefaultSingleAssignment;
    }
    
    /**
     * Solver thread
     */
    protected class SolverThread extends Thread {
        private double iStartTime;
        private int iIndex;
        private boolean iSingle;
        private Model<V, T> iModel;
        private Solution<V, T> iSolution;
        private Assignment<V, T> iAssignment;
        private BlockingQueue<Neighbour<V, T>> iQueue;
        
        public SolverThread(int index, BlockingQueue<Neighbour<V, T>> queue) {
            iIndex = index;
            iSingle = hasSingleSolution();
            iModel = iCurrentSolution.getModel();
            iSolution = (iSingle || iCurrentSolution.getAssignment().getIndex() == index ? iCurrentSolution : createParallelSolution(iIndex));
            iAssignment = iSolution.getAssignment();
            iQueue = queue;
        }
        
        @Override
        public void run() {
            iStartTime = JProf.currentTimeSec();
            try {
                boolean neighbourCheck = getProperties().getPropertyBoolean("ParallelSolver.SingleSolutionNeighbourCheck", false);
                boolean tryLazyFirst = getProperties().getPropertyBoolean("ParallelSolver.SingleSolutionTryLazyFirst", false);
                
                while (!iStop) {
                    // Break if cannot continue
                    if (!getTerminationCondition().canContinue(iSolution)) break;
                    
                    // Create a sub-solution if needed
                    Solution<V, T> current = iSolution;
                    if (iSingle) {
                        current = new Solution<V, T>(iModel, iModel.createInheritedAssignment(iSolution, iIndex), iSolution.getIteration(), iSolution.getTime());
                        current.addSolutionListener(new SolutionListener<V, T>() {
                            @Override
                            public void solutionUpdated(Solution<V, T> solution) {
                            }

                            @Override
                            public void getInfo(Solution<V, T> solution, Map<String, String> info) {
                            }

                            @Override
                            public void getInfo(Solution<V, T> solution, Map<String, String> info, Collection<V> variables) {
                            }

                            @Override
                            public void bestCleared(Solution<V, T> solution) {
                            }

                            @Override
                            public void bestSaved(Solution<V, T> solution) {
                            }

                            @Override
                            public void bestRestored(Solution<V, T> solution) {
                                iSolution.restoreBest();
                            }
                        });
                    }

                    // Neighbour selection
                    Neighbour<V, T> neighbour = null;
                    try {
                        neighbour = getNeighbourSelection().selectNeighbour(current);
                    } catch (Exception e) {
                        sLogger.debug("Failed to select a neighbour: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()), e);
                    }
                    for (SolverListener<V, T> listener : iSolverListeners) {
                        if (!listener.neighbourSelected(iAssignment, iSolution.getIteration(), neighbour)) {
                            neighbour = null;
                            continue;
                        }
                    }

                    double time = JProf.currentTimeSec() - iStartTime;
                    if (neighbour == null) {
                        sLogger.debug("No neighbour selected.");
                        // still update the solution (increase iteration etc.)
                        iSolution.update(time, false);
                        continue;
                    }
                    
                    if (iSingle) {
                        if (iQueue != null) {
                            do {
                                if (iQueue.offer(neighbour, 1000, TimeUnit.MILLISECONDS)) break;
                            } while (!iStop && getTerminationCondition().canContinue(iSolution));
                            continue;
                        }
                        
                        Map<V, T> assignments = null;
                        try {
                            assignments = neighbour.assignments();
                        } catch (Exception e) {
                            sLogger.error("Failed to enumerate " + neighbour.getClass().getSimpleName(), e);
                        }
                        if (assignments == null) {
                            sLogger.debug("No assignments returned.");
                            // still update the solution (increase iteration etc.)
                            iSolution.update(time, false);
                            continue;
                        }
                        
                        if (tryLazyFirst && neighbour instanceof LazyNeighbour) {
                            LazyNeighbour<V, T> lazy = (LazyNeighbour<V, T>)neighbour;
                            double before = current.getModel().getTotalValue(current.getAssignment());
                            neighbour.assign(current.getAssignment(), current.getIteration());
                            double after = current.getModel().getTotalValue(current.getAssignment());
                            if (!lazy.getAcceptanceCriterion().accept(current.getAssignment(), lazy, after - before))
                                continue;
                        }
                        
                        // Assign selected value to the selected variable
                        Lock lock = iSolution.getLock().writeLock();
                        lock.lock();
                        try {
                            LazyNeighbourAcceptanceCriterion<V,T> lazy = null;
                            double before = 0, value = 0;
                            if (neighbour instanceof LazyNeighbour) {
                                before = iSolution.getModel().getTotalValue(iSolution.getAssignment());
                                lazy = ((LazyNeighbour<V, T>)neighbour).getAcceptanceCriterion();
                            } else if (neighbourCheck) {
                                before = iSolution.getModel().getTotalValue(iSolution.getAssignment());
                                value = neighbour.value(current.getAssignment());
                            }
                            Map<V, T> undo = new HashMap<V, T>();
                            for (Iterator<Map.Entry<V, T>> i = assignments.entrySet().iterator(); i.hasNext(); ) {
                                Map.Entry<V, T> e = i.next();
                                T cur = iSolution.getAssignment().getValue(e.getKey());
                                if (e.getValue() == null && cur == null) {
                                    i.remove();
                                } else if (cur != null && cur.equals(e.getValue())) {
                                    i.remove();
                                } else {
                                    undo.put(e.getKey(), iSolution.getAssignment().unassign(iSolution.getIteration(), e.getKey()));
                                }
                            }
                            boolean fail = false;
                            for (T val: assignments.values()) {
                                if (val == null) continue;
                                if (iModel.inConflict(iSolution.getAssignment(), val)) {
                                    fail = true; break;
                                }
                                iSolution.getAssignment().assign(iSolution.getIteration(), val);
                            }
                            if (!fail) {
                                if (lazy != null) {
                                    double after = iSolution.getModel().getTotalValue(iSolution.getAssignment());
                                    if (!lazy.accept(iSolution.getAssignment(), (LazyNeighbour<V, T>) neighbour, after - before))
                                        fail = true;
                                } else if (neighbourCheck) {
                                    double after = iSolution.getModel().getTotalValue(iSolution.getAssignment());
                                    if (before + value < after && before < after && !getSolutionComparator().isBetterThanBestSolution(iSolution))
                                        fail = true;
                                }
                            }
                            if (fail) {
                                for (V var: undo.keySet())
                                    iSolution.getAssignment().unassign(iSolution.getIteration(), var);
                                for (T val: undo.values())
                                    if (val != null)
                                        iSolution.getAssignment().assign(iSolution.getIteration(), val);
                            }
                            iSolution.update(time, !fail);
                            if (fail) {
                                for (SolverListener<V, T> listener : iSolverListeners)
                                    listener.neighbourFailed(current.getAssignment(), iSolution.getIteration(), neighbour);
                                continue;
                            }
                            
                            onAssigned(iStartTime, iSolution);

                            if ((iSaveBestUnassigned < 0 || iSaveBestUnassigned >= iSolution.getAssignment().nrUnassignedVariables(iModel)) && getSolutionComparator().isBetterThanBestSolution(iSolution)) {
                                iSolution.saveBest();
                            }
                        } finally {
                            lock.unlock();
                        }
                    } else {
                        // Assign selected value to the selected variable
                        Lock lock = iSolution.getLock().writeLock();
                        lock.lock();
                        try {
                            neighbour.assign(iAssignment, iSolution.getIteration());
                            iSolution.update(time, currentSolution());
                        } finally {
                            lock.unlock();
                        }

                        onAssigned(iStartTime, iSolution);
                        
                        if (iSaveBestUnassigned < 0 || iSaveBestUnassigned >= iAssignment.nrUnassignedVariables(iModel))
                            iSolution.saveBestIfImproving(currentSolution(), getSolutionComparator());
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
            Lock lock = currentSolution().getLock().writeLock();
            lock.lock();
            try {
                iNrFinished ++;
            } finally {
                lock.unlock();
            }
        }
        
    }
    
    /**
     * Solver thread
     */
    protected class AssignmentThread extends Thread {
        private double iStartTime;
        private Solution<V, T> iSolution;
        private BlockingQueue<Neighbour<V, T>> iQueue;
        
        public AssignmentThread(BlockingQueue<Neighbour<V, T>> queue) {
            setName("Assignment");
            setPriority(1 + THREAD_PRIORITY);
            iSolution = iCurrentSolution;
            iQueue = queue;
        }
        
        @Override
        public void run() {
            iStartTime = JProf.currentTimeSec();
            try {
                boolean neighbourCheck = getProperties().getPropertyBoolean("ParallelSolver.SingleSolutionNeighbourCheck", false);
                
                while (!iStop) {
                    // Break if cannot continue
                    if (!getTerminationCondition().canContinue(iSolution)) break;
                    
                    // Create a sub-solution if needed
                    Neighbour<V, T> neighbour = iQueue.poll(1000, TimeUnit.MILLISECONDS);
                    
                    if (neighbour == null) continue;

                    double time = JProf.currentTimeSec() - iStartTime;
                    
                    Map<V, T> assignments = null;
                    try {
                        assignments = neighbour.assignments();
                    } catch (Exception e) {
                        sLogger.error("Failed to enumerate " + neighbour.getClass().getSimpleName(), e);
                    }
                    if (assignments == null) {
                        sLogger.debug("No assignments returned.");
                        // still update the solution (increase iteration etc.)
                        iSolution.update(time, false);
                        continue;
                    }
                    
                    // Assign selected value to the selected variable
                    Lock lock = iSolution.getLock().writeLock();
                    lock.lock();
                    try {
                        LazyNeighbourAcceptanceCriterion<V,T> lazy = null;
                        double before = 0, value = 0;
                        if (neighbour instanceof LazyNeighbour) {
                            before = iSolution.getModel().getTotalValue(iSolution.getAssignment());
                            lazy = ((LazyNeighbour<V, T>)neighbour).getAcceptanceCriterion();
                        } else if (neighbourCheck) {
                            before = iSolution.getModel().getTotalValue(iSolution.getAssignment());
                            value = neighbour.value(iSolution.getAssignment());
                        }
                        Map<V, T> undo = new HashMap<V, T>();
                        for (V var: assignments.keySet())
                            undo.put(var, iSolution.getAssignment().unassign(iSolution.getIteration(), var));
                        boolean fail = false;
                        for (T val: assignments.values()) {
                            if (val == null) continue;
                            if (iSolution.getModel().inConflict(iSolution.getAssignment(), val)) {
                                fail = true; break;
                            }
                            iSolution.getAssignment().assign(iSolution.getIteration(), val);
                        }
                        if (!fail) {
                            if (lazy != null) {
                                double after = iSolution.getModel().getTotalValue(iSolution.getAssignment());
                                if (!lazy.accept(iSolution.getAssignment(), (LazyNeighbour<V, T>) neighbour, after - before))
                                    fail = true;
                            } else if (neighbourCheck) {
                                double after = iSolution.getModel().getTotalValue(iSolution.getAssignment());
                                if (before + value < after && before < after && !getSolutionComparator().isBetterThanBestSolution(iSolution))
                                    fail = true;
                            }
                        }
                        if (fail) {
                            for (V var: undo.keySet())
                                iSolution.getAssignment().unassign(iSolution.getIteration(), var);
                            for (T val: undo.values())
                                if (val != null)
                                    iSolution.getAssignment().assign(iSolution.getIteration(), val);
                        }
                        iSolution.update(time, !fail);
                        if (fail) {
                            for (SolverListener<V, T> listener : iSolverListeners)
                                listener.neighbourFailed(iSolution.getAssignment(), iSolution.getIteration(), neighbour);
                            continue;
                        }
                        
                        onAssigned(iStartTime, iSolution);

                        if ((iSaveBestUnassigned < 0 || iSaveBestUnassigned >= iSolution.getAssignment().nrUnassignedVariables(iSolution.getModel())) && getSolutionComparator().isBetterThanBestSolution(iSolution)) {
                            iSolution.saveBest();
                        }
                    } finally {
                        lock.unlock();
                    }
                }

            } catch (Exception ex) {
                sLogger.error(ex.getMessage(), ex);
                iProgress.fatal(getName() + " failed, reason:" + ex.getMessage(), ex);
            }
            Lock lock = currentSolution().getLock().writeLock();
            lock.lock();
            try {
                iNrFinished ++;
            } finally {
                lock.unlock();
            }
        }
        
    }

}
