package org.cpsolver.ifs.solver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.locks.Lock;

import org.cpsolver.ifs.assignment.DefaultSingleAssignment;
import org.cpsolver.ifs.extension.ConflictStatistics;
import org.cpsolver.ifs.extension.Extension;
import org.cpsolver.ifs.extension.MacPropagation;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.heuristics.StandardNeighbourSelection;
import org.cpsolver.ifs.heuristics.ValueSelection;
import org.cpsolver.ifs.heuristics.VariableSelection;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.perturbations.DefaultPerturbationsCounter;
import org.cpsolver.ifs.perturbations.PerturbationsCounter;
import org.cpsolver.ifs.solution.GeneralSolutionComparator;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solution.SolutionComparator;
import org.cpsolver.ifs.termination.GeneralTerminationCondition;
import org.cpsolver.ifs.termination.TerminationCondition;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.JProf;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.ifs.util.ToolBox;


/**
 * IFS Solver. <br>
 * <br>
 * The iterative forward search (IFS) algorithm is based on ideas of local
 * search methods. However, in contrast to classical local search techniques, it
 * operates over feasible, though not necessarily complete solutions. In such a
 * solution, some variables can be left unassigned. Still all hard constraints
 * on assigned variables must be satisfied. Similarly to backtracking based
 * algorithms, this means that there are no violations of hard constraints. <br>
 * <br>
 * This search works in iterations. During each step, an unassigned or assigned
 * variable is initially selected. Typically an unassigned variable is chosen
 * like in backtracking-based search. An assigned variable may be selected when
 * all variables are assigned but the solution is not good enough (for example,
 * when there are still many violations of soft constraints). Once a variable is
 * selected, a value from its domain is chosen for assignment. Even if the best
 * value is selected (whatever "best" means), its assignment to the selected
 * variable may cause some hard conflicts with already assigned variables. Such
 * conflicting variables are removed from the solution and become unassigned.
 * Finally, the selected value is assigned to the selected variable. <br>
 * <br>
 * Algorithm schema:
 * <pre><code>
 * procedure org.cpsolver.ifs(initial)  // initial solution is the parameter
 * &nbsp;&nbsp;iteration = 0;         // iteration counter
 * &nbsp;&nbsp;current = initial;     // current (partial) feasible solution
 * &nbsp;&nbsp;best = initial;        // best solution
 * &nbsp;&nbsp;while canContinue(current, iteration) do
 * &nbsp;&nbsp;&nbsp;&nbsp;iteration = iteration + 1;
 * &nbsp;&nbsp;&nbsp;&nbsp;variable = selectVariable(current);
 * &nbsp;&nbsp;&nbsp;&nbsp;value = selectValue(current, variable);
 * &nbsp;&nbsp;&nbsp;&nbsp;UNASSIGN(current,  CONFLICTING_VARIABLES(current, variable, value));
 * &nbsp;&nbsp;&nbsp;&nbsp;ASSIGN(current, variable, value);
 * &nbsp;&nbsp;&nbsp;&nbsp;if better(current, best) then best = current;
 * &nbsp;&nbsp;end while
 * &nbsp;&nbsp;return best;
 * end procedure
 * </code></pre>
 * The algorithm attempts to move from one (partial) feasible solution to
 * another via repetitive assignment of a selected value to a selected variable.
 * During this search, the feasibility of all hard constraints in each iteration
 * step is enforced by unassigning the conflicting variables. The search is
 * terminated when the requested solution is found or when there is a timeout,
 * expressed e.g., as a maximal number of iterations or available time being
 * reached. The best solution found is then returned. <br>
 * <br>
 * The above algorithm schema is parameterized by several functions, namely:
 * <ul>
 * <li>the termination condition (function canContinue, see
 * {@link TerminationCondition}),
 * <li>the solution comparator (function better, see {@link SolutionComparator}
 * ),
 * <li>the neighbour selection (function selectNeighbour, see
 * {@link NeighbourSelection}) and
 * </ul>
 * <br>
 * Usage:
 * <pre><code>
 * DataProperties cfg = ToolBox.loadProperties(inputCfg); //input configuration
 * Solver solver = new Solver(cfg);
 * solver.setInitalSolution(model); //sets initial solution
 * 
 * solver.start(); //server is executed in a thread
 * 
 * try { //wait untill the server finishes
 * &nbsp;&nbsp;solver.getSolverThread().join(); 
 * } catch (InterruptedException e) {} 
 * 
 * Solution solution = solver.lastSolution(); //last solution
 * solution.restoreBest(); //restore best solution ever found
 * </code></pre>
 * Solver's parameters: <br>
 * <table border='1'><caption>Related Solver Parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>General.SaveBestUnassigned</td>
 * <td>{@link Integer}</td>
 * <td>During the search, solution is saved when it is the best ever found
 * solution and if the number of assigned variables is less or equal this
 * parameter (if set to -1, the solution is always saved)</td>
 * </tr>
 * <tr>
 * <td>General.Seed</td>
 * <td>{@link Long}</td>
 * <td>If set, random number generator is initialized with this seed</td>
 * </tr>
 * <tr>
 * <td>General.SaveConfiguration</td>
 * <td>{@link Boolean}</td>
 * <td>If true, given configuration is stored into the output folder (during
 * initialization of the solver,
 * ${General.Output}/${General.ProblemName}.properties)</td>
 * </tr>
 * <tr>
 * <td>Solver.AutoConfigure</td>
 * <td>{@link Boolean}</td>
 * <td>If true, IFS Solver is configured according to the following parameters</td>
 * </tr>
 * <tr>
 * <td>Termination.Class</td>
 * <td>{@link String}</td>
 * <td>Fully qualified class name of the termination condition (see
 * {@link TerminationCondition}, e.g. {@link GeneralTerminationCondition})</td>
 * </tr>
 * <tr>
 * <td>Comparator.Class</td>
 * <td>{@link String}</td>
 * <td>Fully qualified class name of the solution comparator (see
 * {@link SolutionComparator}, e.g. {@link GeneralSolutionComparator})</td>
 * </tr>
 * <tr>
 * <td>Neighbour.Class</td>
 * <td>{@link String}</td>
 * <td>Fully qualified class name of the neighbour selection criterion (see
 * {@link NeighbourSelection}, e.g. {@link StandardNeighbourSelection})</td>
 * </tr>
 * <tr>
 * <td>PerturbationCounter.Class</td>
 * <td>{@link String}</td>
 * <td>Fully qualified class name of the perturbation counter in case of solving
 * minimal perturbation problem (see {@link PerturbationsCounter}, e.g.
 * {@link DefaultPerturbationsCounter})</td>
 * </tr>
 * <tr>
 * <td>Extensions.Classes</td>
 * <td>{@link String}</td>
 * <td>Semi-colon separated list of fully qualified class names of IFS
 * extensions (see {@link Extension}, e.g. {@link ConflictStatistics} or
 * {@link MacPropagation})</td>
 * </tr>
 * </table>
 * 
 * @see SolverListener
 * @see Model
 * @see Solution
 * @see TerminationCondition
 * @see SolutionComparator
 * @see PerturbationsCounter
 * @see VariableSelection
 * @see ValueSelection
 * @see Extension
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
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
public class Solver<V extends Variable<V, T>, T extends Value<V, T>> {
    public static int THREAD_PRIORITY = 3;
    /** log */
    protected static org.apache.logging.log4j.Logger sLogger = org.apache.logging.log4j.LogManager.getLogger(Solver.class);
    /** current solution */
    protected Solution<V, T> iCurrentSolution = null;
    /** last solution (after IFS Solver finishes) */
    protected Solution<V, T> iLastSolution = null;

    /** solver is stopped */
    protected boolean iStop = false;

    /** solver thread */
    protected SolverThread iSolverThread = null;
    /** configuration */
    private DataProperties iProperties = null;

    private TerminationCondition<V, T> iTerminationCondition = null;
    private SolutionComparator<V, T> iSolutionComparator = null;
    private PerturbationsCounter<V, T> iPerturbationsCounter = null;
    private NeighbourSelection<V, T> iNeighbourSelection = null;
    private List<Extension<V, T>> iExtensions = new ArrayList<Extension<V, T>>();
    protected List<SolverListener<V, T>> iSolverListeners = new ArrayList<SolverListener<V, T>>();
    protected int iSaveBestUnassigned = 0;

    private boolean iUpdateProgress = true;

    protected Progress iProgress;

    /**
     * Constructor.
     * 
     * @param properties
     *            input configuration
     */
    public Solver(DataProperties properties) {
        iProperties = properties;
    }

    /** Dispose solver */
    public void dispose() {
        iExtensions.clear();
        iSolverListeners.clear();
        iTerminationCondition = null;
        iSolutionComparator = null;
        iPerturbationsCounter = null;
        iNeighbourSelection = null;
    }

    /** Sets termination condition
     * @param terminationCondition termination condition
     **/
    public void setTerminalCondition(TerminationCondition<V, T> terminationCondition) {
        iTerminationCondition = terminationCondition;
    }

    /** Sets solution comparator
     * @param solutionComparator solution comparator
     **/
    public void setSolutionComparator(SolutionComparator<V, T> solutionComparator) {
        iSolutionComparator = solutionComparator;
    }

    /** Sets neighbour selection criterion
     * @param neighbourSelection neighbour selection criterion
     **/
    public void setNeighbourSelection(NeighbourSelection<V, T> neighbourSelection) {
        iNeighbourSelection = neighbourSelection;
    }

    /** Sets perturbation counter (minimal perturbation problem)
     * @param perturbationsCounter perturbation counter
     **/
    public void setPerturbationsCounter(PerturbationsCounter<V, T> perturbationsCounter) {
        iPerturbationsCounter = perturbationsCounter;
    }

    /** Add an IFS extension
     * @param extension an extension
     **/
    public void addExtension(Extension<V, T> extension) {
        iExtensions.add(extension);
    }

    /** Returns termination condition
     * @return termination condition
     **/
    public TerminationCondition<V, T> getTerminationCondition() {
        return iTerminationCondition;
    }

    /** Returns solution comparator
     * @return solution comparator
     **/
    public SolutionComparator<V, T> getSolutionComparator() {
        return iSolutionComparator;
    }

    /** Returns neighbour selection criterion
     * @return neighbour selection criterion
     **/
    public NeighbourSelection<V, T> getNeighbourSelection() {
        return iNeighbourSelection;
    }

    /** Returns perturbation counter (minimal perturbation problem)
     * @return perturbation counter
     **/
    public PerturbationsCounter<V, T> getPerturbationsCounter() {
        return iPerturbationsCounter;
    }

    /** Returns list of all used extensions
     * @return list of all registered extensions
     **/
    public List<Extension<V, T>> getExtensions() {
        return iExtensions;
    }

    /** Adds a solver listener
     * @param listener solver listener
     **/
    public void addSolverListener(SolverListener<V, T> listener) {
        iSolverListeners.add(listener);
    }

    /** Removes a solver listener
     * @param listener solver listener
     **/
    public void removeSolverListener(SolverListener<V, T> listener) {
        iSolverListeners.remove(listener);
    }

    /** Registered solver listeners
     * @return list of all registered solver listeners
     **/
    public List<SolverListener<V, T>> getSolverListeners() {
        return iSolverListeners;
    }

    /** Returns configuration
     * @return solver configuration
     **/
    public DataProperties getProperties() {
        return iProperties;
    }

    /**
     * Automatic configuratin of the solver -- when Solver.AutoConfigure is true
     */
    @SuppressWarnings("unchecked")
    protected void autoConfigure() {
        try {
            boolean mpp = getProperties().getPropertyBoolean("General.MPP", false);

            String terminationConditionClassName = getProperties().getProperty(
                    "Termination.Class",
                    (mpp ? "org.cpsolver.ifs.termination.MPPTerminationCondition"
                            : "org.cpsolver.ifs.termination.GeneralTerminationCondition"));
            sLogger.info("Using " + terminationConditionClassName);
            Class<?> terminationConditionClass = Class.forName(terminationConditionClassName);
            Constructor<?> terminationConditionConstructor = terminationConditionClass
                    .getConstructor(new Class[] { DataProperties.class });
            setTerminalCondition((TerminationCondition<V, T>) terminationConditionConstructor
                    .newInstance(new Object[] { getProperties() }));

            String solutionComparatorClassName = getProperties().getProperty(
                    "Comparator.Class",
                    (mpp ? "org.cpsolver.ifs.solution.MPPSolutionComparator"
                            : "org.cpsolver.ifs.solution.GeneralSolutionComparator"));
            sLogger.info("Using " + solutionComparatorClassName);
            Class<?> solutionComparatorClass = Class.forName(solutionComparatorClassName);
            Constructor<?> solutionComparatorConstructor = solutionComparatorClass
                    .getConstructor(new Class[] { DataProperties.class });
            setSolutionComparator((SolutionComparator<V, T>) solutionComparatorConstructor
                    .newInstance(new Object[] { getProperties() }));

            String neighbourSelectionClassName = getProperties().getProperty("Neighbour.Class",
                    "org.cpsolver.ifs.heuristics.StandardNeighbourSelection");
            sLogger.info("Using " + neighbourSelectionClassName);
            Class<?> neighbourSelectionClass = Class.forName(neighbourSelectionClassName);
            Constructor<?> neighbourSelectionConstructor = neighbourSelectionClass
                    .getConstructor(new Class[] { DataProperties.class });
            setNeighbourSelection((NeighbourSelection<V, T>) neighbourSelectionConstructor
                    .newInstance(new Object[] { getProperties() }));

            String perturbationCounterClassName = getProperties().getProperty("PerturbationCounter.Class",
                    "org.cpsolver.ifs.perturbations.DefaultPerturbationsCounter");
            sLogger.info("Using " + perturbationCounterClassName);
            Class<?> perturbationCounterClass = Class.forName(perturbationCounterClassName);
            Constructor<?> perturbationCounterConstructor = perturbationCounterClass
                    .getConstructor(new Class[] { DataProperties.class });
            setPerturbationsCounter((PerturbationsCounter<V, T>) perturbationCounterConstructor
                    .newInstance(new Object[] { getProperties() }));

            for (Extension<V, T> extension : iExtensions) {
                extension.unregister(iCurrentSolution.getModel());
            }
            iExtensions.clear();
            String extensionClassNames = getProperties().getProperty("Extensions.Classes", null);
            if (extensionClassNames != null) {
                StringTokenizer extensionClassNameTokenizer = new StringTokenizer(extensionClassNames, ";");
                while (extensionClassNameTokenizer.hasMoreTokens()) {
                    String extensionClassName = extensionClassNameTokenizer.nextToken().trim();
                    if (extensionClassName.isEmpty()) continue;
                    sLogger.info("Using " + extensionClassName);
                    Class<?> extensionClass = Class.forName(extensionClassName);
                    Constructor<?> extensionConstructor = extensionClass.getConstructor(new Class[] { Solver.class,
                            DataProperties.class });
                    addExtension((Extension<V, T>) extensionConstructor.newInstance(new Object[] { this,
                            getProperties() }));
                }
            }
        } catch (Exception e) {
            sLogger.error("Unable to autoconfigure solver.", e);
        }
    }

    /** Clears best solution */
    public void clearBest() {
        if (iCurrentSolution != null)
            iCurrentSolution.clearBest();
    }

    /** Sets initial solution 
     * @param solution initial solution
     **/
    public void setInitalSolution(Solution<V, T> solution) {
        iCurrentSolution = solution;
        iLastSolution = null;
    }

    /** Sets initial solution 
     * @param model problem model
     **/
    public void setInitalSolution(Model<V, T> model) {
        setInitalSolution(new Solution<V, T>(model, new DefaultSingleAssignment<V, T>(), 0, 0));
    }

    /** Starts solver */
    public void start() {
        iSolverThread = new SolverThread();
        iSolverThread.setPriority(THREAD_PRIORITY);
        iSolverThread.start();
    }

    /** Returns solver's thread 
     * @return solver's thread
     **/
    public Thread getSolverThread() {
        return iSolverThread;
    }

    /** Initialization */
    public void init() {
    }

    /** True, when solver should update progress (see {@link Progress}) 
     * @return true if the solver should update process
     **/
    protected boolean isUpdateProgress() {
        return iUpdateProgress;
    }

    /** True, when solver should update progress (see {@link Progress})
     * @param updateProgress true if the solver should update process (default is true)
     **/
    public void setUpdateProgress(boolean updateProgress) {
        iUpdateProgress = updateProgress;
    }

    /** Last solution (when solver finishes) 
     * @return last solution
     **/
    public Solution<V, T> lastSolution() {
        return (iLastSolution == null ? iCurrentSolution : iLastSolution);
    }

    /** Current solution (during the search) 
     * @return current solution
     **/
    public Solution<V, T> currentSolution() {
        return iCurrentSolution;
    }

    public void initSolver() {
        long seed = getProperties().getPropertyLong("General.Seed", System.currentTimeMillis());
        ToolBox.setSeed(seed);

        iSaveBestUnassigned = getProperties().getPropertyInt("General.SaveBestUnassigned", 0);

        clearBest();
        if (iProperties.getPropertyBoolean("Solver.AutoConfigure", true)) {
            autoConfigure();
        }

        // register extensions
        for (Extension<V, T> extension : iExtensions) {
            extension.register(iCurrentSolution.getModel());
        }

        // register solution
        iCurrentSolution.init(Solver.this);

        // register and intialize neighbour selection
        getNeighbourSelection().init(Solver.this);

        // register and intialize perturbations counter
        if (getPerturbationsCounter() != null)
            getPerturbationsCounter().init(Solver.this);

        // save initial configuration
        if (iProperties.getPropertyBoolean("General.SaveConfiguration", false)) {
            FileOutputStream f = null;
            try {
                f = new FileOutputStream(iProperties.getProperty("General.Output") + File.separator
                        + iProperties.getProperty("General.ProblemName", "ifs") + ".properties");
                iProperties.store(f, iProperties.getProperty("General.ProblemNameLong", "Iterative Forward Search")
                        + "  -- configuration file");
                f.flush();
                f.close();
                f = null;
            } catch (Exception e) {
                sLogger.error("Unable to store configuration file :-(", e);
            } finally {
                try {
                    if (f != null)
                        f.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /** Stop running solver */
    public void stopSolver() {
        stopSolver(true);
    }
    
    /** Stop running solver 
     * @param join wait for the solver thread to finish
     **/
    public void stopSolver(boolean join) {
        if (getSolverThread() != null) {
            iStop = true;
            if (join) {
                try {
                    getSolverThread().join();
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    /** True, if the solver is running 
     * @return true if the solver is running
     **/
    public boolean isRunning() {
        return (getSolverThread() != null);
    }

    /** Called when the solver is stopped */
    protected void onStop() {
    }

    /** Called when the solver is started */
    protected void onStart() {
    }

    /** Called when the solver is finished */
    protected void onFinish() {
    }

    /** Called when the solver fails */
    protected void onFailure() {
    }

    /** Called in each iteration, after a neighbour is assigned 
     * @param startTime solver start time in seconds
     * @param solution current solution
     **/
    protected void onAssigned(double startTime, Solution<V, T> solution) {
    }
    
    /**
     * Returns true if the solver works only with one solution (regardless the number of threads it is using)
     * @return true
     */
    public boolean hasSingleSolution() {
        return currentSolution().getAssignment() instanceof DefaultSingleAssignment;
    }

    /** Solver thread */
    protected class SolverThread extends Thread {

        /** Solving rutine */
        @Override
        public void run() {
            try {
                iStop = false;
                // Sets thread name
                setName("Solver");

                // Initialization
                iProgress = Progress.getInstance(iCurrentSolution.getModel());
                iProgress.setStatus("Solving problem ...");
                iProgress.setPhase("Initializing solver");
                initSolver();
                onStart();

                double startTime = JProf.currentTimeSec();
                int timeout = getProperties().getPropertyInt("Termination.TimeOut", 1800);
                if (isUpdateProgress()) {
                    if (iCurrentSolution.getBestInfo() == null) {
                        iProgress.setPhase("Searching for initial solution ...", iCurrentSolution.getModel()
                                .variables().size());
                    } else {
                        iProgress.setPhase("Improving found solution ...");
                    }
                }
                sLogger.info("Initial solution:" + ToolBox.dict2string(iCurrentSolution.getExtendedInfo(), 1));
                if ((iSaveBestUnassigned < 0 || iSaveBestUnassigned >= iCurrentSolution.getAssignment().nrUnassignedVariables(iCurrentSolution.getModel()))
                        && (iCurrentSolution.getBestInfo() == null || getSolutionComparator().isBetterThanBestSolution(iCurrentSolution))) {
                    if (iCurrentSolution.getModel().variables().size() == iCurrentSolution.getAssignment().nrAssignedVariables())
                        sLogger.info("Complete solution " + ToolBox.dict2string(iCurrentSolution.getExtendedInfo(), 1) + " was found.");
                    iCurrentSolution.saveBest();
                }

                if (iCurrentSolution.getModel().variables().isEmpty()) {
                    iProgress.error("Nothing to solve.");
                    iStop = true;
                }

                // Iterations: until solver can continue
                while (!iStop && getTerminationCondition().canContinue(iCurrentSolution)) {
                    // Neighbour selection
                    Neighbour<V, T> neighbour = getNeighbourSelection().selectNeighbour(iCurrentSolution);
                    for (SolverListener<V, T> listener : iSolverListeners) {
                        if (!listener.neighbourSelected(iCurrentSolution.getAssignment(), iCurrentSolution.getIteration(), neighbour)) {
                            neighbour = null;
                            continue;
                        }
                    }
                    if (neighbour == null) {
                        sLogger.debug("No neighbour selected.");
                        // still update the solution (increase iteration etc.)
                        iCurrentSolution.update(JProf.currentTimeSec() - startTime, false);
                        continue;
                    }

                    // Assign selected value to the selected variable
                    Lock lock = iCurrentSolution.getLock().writeLock();
                    lock.lock();
                    try {
                        neighbour.assign(iCurrentSolution.getAssignment(), iCurrentSolution.getIteration());
                    } finally {
                        lock.unlock();
                    }
                    double time = JProf.currentTimeSec() - startTime;
                    iCurrentSolution.update(time);

                    onAssigned(startTime, iCurrentSolution);

                    // Check if the solution is the best ever found one
                    if ((iSaveBestUnassigned < 0 || iSaveBestUnassigned >= iCurrentSolution.getAssignment().nrUnassignedVariables(iCurrentSolution.getModel())) && (iCurrentSolution.getBestInfo() == null || getSolutionComparator().isBetterThanBestSolution(iCurrentSolution))) {
                        if (iCurrentSolution.getModel().variables().size() == iCurrentSolution.getAssignment().nrAssignedVariables()) {
                            iProgress.debug("Complete solution of value " + iCurrentSolution.getModel().getTotalValue(iCurrentSolution.getAssignment()) + " was found.");
                        }
                        iCurrentSolution.saveBest();
                    }

                    // Increment progress bar
                    if (isUpdateProgress()) {
                        if (iCurrentSolution.getBestInfo() != null && iCurrentSolution.getModel().getBestUnassignedVariables() == 0) {
                            if (!"Improving found solution ...".equals(iProgress.getPhase()))
                                iProgress.setPhase("Improving found solution ...");
                            iProgress.setProgress(Math.min(100, (int)Math.round(100 * time / timeout)));
                        } else if ((iCurrentSolution.getBestInfo() == null || iCurrentSolution.getModel().getBestUnassignedVariables() > 0) && (iCurrentSolution.getAssignment().nrAssignedVariables() > iProgress.getProgress())) {
                            iProgress.setProgress(iCurrentSolution.getAssignment().nrAssignedVariables());
                        }
                    }

                }

                // Finalization
                iLastSolution = iCurrentSolution;

                iProgress.setPhase("Done", 1);
                iProgress.incProgress();

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
                iProgress.fatal("Solver failed, reason:" + ex.getMessage(), ex);
                iProgress.setStatus("Solver failed.");
                onFailure();
            } finally {
                iSolverThread = null;
            }
        }
    }
    
    /** Return true if {@link Solver#stopSolver()} was called 
     * @return true if the solver is to be stopped
     **/
    public boolean isStop() {
        return iStop;
    }

}
