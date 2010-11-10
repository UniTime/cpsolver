package net.sf.cpsolver.ifs.solver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import net.sf.cpsolver.ifs.extension.ConflictStatistics;
import net.sf.cpsolver.ifs.extension.Extension;
import net.sf.cpsolver.ifs.extension.MacPropagation;
import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.heuristics.StandardNeighbourSelection;
import net.sf.cpsolver.ifs.heuristics.ValueSelection;
import net.sf.cpsolver.ifs.heuristics.VariableSelection;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.perturbations.DefaultPerturbationsCounter;
import net.sf.cpsolver.ifs.perturbations.PerturbationsCounter;
import net.sf.cpsolver.ifs.solution.GeneralSolutionComparator;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solution.SolutionComparator;
import net.sf.cpsolver.ifs.termination.GeneralTerminationCondition;
import net.sf.cpsolver.ifs.termination.TerminationCondition;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.JProf;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ToolBox;

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
 * value is selected (whatever �best� means), its assignment to the selected
 * variable may cause some hard conflicts with already assigned variables. Such
 * conflicting variables are removed from the solution and become unassigned.
 * Finally, the selected value is assigned to the selected variable. <br>
 * <br>
 * Algorithm schema:
 * <ul>
 * <code>
 * procedure net.sf.cpsolver.ifs(initial)  // initial solution is the parameter <br>
 * &nbsp;&nbsp;iteration = 0;         // iteration counter <br>
 * &nbsp;&nbsp;current = initial;     // current (partial) feasible solution <br>
 * &nbsp;&nbsp;best = initial;        // best solution <br>
 * &nbsp;&nbsp;while canContinue(current, iteration) do <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;iteration = iteration + 1; <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;variable = selectVariable(current); <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;value = selectValue(current, variable); <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;UNASSIGN(current,  CONFLICTING_VARIABLES(current, variable, value)); <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;ASSIGN(current, variable, value); <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;if better(current, best) then best = current; <br>
 * &nbsp;&nbsp;end while <br>
 * &nbsp;&nbsp;return best;<br>
 * end procedure <br>
 * </code>
 * </ul>
 * <br>
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
 * <ul>
 * <code>
 * DataProperties cfg = ToolBox.loadProperties(inputCfg); //input configuration<br>
 * Solver solver = new Solver(cfg);<br>
 * solver.setInitalSolution(model); //sets initial solution<br>
 * <br>
 * solver.start(); //server is executed in a thread<br>
 * <br>
 * try { //wait untill the server finishes<br>
 * &nbsp;&nbsp;solver.getSolverThread().join(); <br>
 * } catch (InterruptedException e) {} <br>
 * <br>
 * Solution solution = solver.lastSolution(); //last solution<br>
 * solution.restoreBest(); //restore best solution ever found<br>
 * </code>
 * </ul>
 * <br>
 * Solver's parameters: <br>
 * <table border='1'>
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
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
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

public class Solver<V extends Variable<V, T>, T extends Value<V, T>> {
    public static int THREAD_PRIORITY = 3;
    /** log */
    protected static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(Solver.class);
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
    private List<SolverListener<V, T>> iSolverListeners = new ArrayList<SolverListener<V, T>>();
    private int iSaveBestUnassigned = 0;

    private boolean iUpdateProgress = true;

    private Progress iProgress;

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

    private boolean iValueExtraUsed = false;
    private boolean iVariableExtraUsed = false;

    /** Sets termination condition */
    public void setTerminalCondition(TerminationCondition<V, T> terminationCondition) {
        iTerminationCondition = terminationCondition;
    }

    /** Sets solution comparator */
    public void setSolutionComparator(SolutionComparator<V, T> solutionComparator) {
        iSolutionComparator = solutionComparator;
    }

    /** Sets neighbour selection criterion */
    public void setNeighbourSelection(NeighbourSelection<V, T> neighbourSelection) {
        iNeighbourSelection = neighbourSelection;
    }

    /** Sets perturbation counter (minimal perturbation problem) */
    public void setPerturbationsCounter(PerturbationsCounter<V, T> perturbationsCounter) {
        iPerturbationsCounter = perturbationsCounter;
    }

    /** Add an IFS extension */
    public void addExtension(Extension<V, T> extension) {
        if (extension.useValueExtra() && iValueExtraUsed) {
            sLogger.warn("Unable to add an extension " + extension + " -- value extra is already used.");
            return;
        }
        if (extension.useVariableExtra() && iVariableExtraUsed) {
            sLogger.warn("Unable to add extension " + extension + " -- variable extra is already used.");
            return;
        }
        iValueExtraUsed = iValueExtraUsed | extension.useValueExtra();
        iValueExtraUsed = iVariableExtraUsed | extension.useVariableExtra();
        iExtensions.add(extension);
    }

    /** Returns termination condition */
    public TerminationCondition<V, T> getTerminationCondition() {
        return iTerminationCondition;
    }

    /** Returns solution comparator */
    public SolutionComparator<V, T> getSolutionComparator() {
        return iSolutionComparator;
    }

    /** Returns neighbour selection criterion */
    public NeighbourSelection<V, T> getNeighbourSelection() {
        return iNeighbourSelection;
    }

    /** Returns perturbation counter (minimal perturbation problem) */
    public PerturbationsCounter<V, T> getPerturbationsCounter() {
        return iPerturbationsCounter;
    }

    /** Returns list of all used extensions */
    public List<Extension<V, T>> getExtensions() {
        return iExtensions;
    }

    /** Adds a solver listener */
    public void addSolverListener(SolverListener<V, T> listener) {
        iSolverListeners.add(listener);
    }

    /** Removes a solver listener */
    public void removeSolverListener(SolverListener<V, T> listener) {
        iSolverListeners.remove(listener);
    }

    /** Registered solver listeners */
    public List<SolverListener<V, T>> getSolverListeners() {
        return iSolverListeners;
    }

    /** Returns configuration */
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
                    (mpp ? "net.sf.cpsolver.ifs.termination.MPPTerminationCondition"
                            : "net.sf.cpsolver.ifs.termination.GeneralTerminationCondition"));
            sLogger.info("Using " + terminationConditionClassName);
            Class<?> terminationConditionClass = Class.forName(terminationConditionClassName);
            Constructor<?> terminationConditionConstructor = terminationConditionClass
                    .getConstructor(new Class[] { DataProperties.class });
            setTerminalCondition((TerminationCondition<V, T>) terminationConditionConstructor
                    .newInstance(new Object[] { getProperties() }));

            String solutionComparatorClassName = getProperties().getProperty(
                    "Comparator.Class",
                    (mpp ? "net.sf.cpsolver.ifs.solution.MPPSolutionComparator"
                            : "net.sf.cpsolver.ifs.solution.GeneralSolutionComparator"));
            sLogger.info("Using " + solutionComparatorClassName);
            Class<?> solutionComparatorClass = Class.forName(solutionComparatorClassName);
            Constructor<?> solutionComparatorConstructor = solutionComparatorClass
                    .getConstructor(new Class[] { DataProperties.class });
            setSolutionComparator((SolutionComparator<V, T>) solutionComparatorConstructor
                    .newInstance(new Object[] { getProperties() }));

            String neighbourSelectionClassName = getProperties().getProperty("Neighbour.Class",
                    "net.sf.cpsolver.ifs.heuristics.StandardNeighbourSelection");
            sLogger.info("Using " + neighbourSelectionClassName);
            Class<?> neighbourSelectionClass = Class.forName(neighbourSelectionClassName);
            Constructor<?> neighbourSelectionConstructor = neighbourSelectionClass
                    .getConstructor(new Class[] { DataProperties.class });
            setNeighbourSelection((NeighbourSelection<V, T>) neighbourSelectionConstructor
                    .newInstance(new Object[] { getProperties() }));

            String perturbationCounterClassName = getProperties().getProperty("PerturbationCounter.Class",
                    "net.sf.cpsolver.ifs.perturbations.DefaultPerturbationsCounter");
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
                    String extensionClassName = extensionClassNameTokenizer.nextToken();
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

    /** Sets initial solution */
    public void setInitalSolution(Solution<V, T> solution) {
        iCurrentSolution = solution;
        iLastSolution = null;
    }

    /** Sets initial solution */
    public void setInitalSolution(Model<V, T> model) {
        iCurrentSolution = new Solution<V, T>(model, 0, 0);
        iLastSolution = null;
    }

    /** Starts solver */
    public void start() {
        iSolverThread = new SolverThread();
        iSolverThread.setPriority(THREAD_PRIORITY);
        iSolverThread.start();
    }

    /** Returns solver's thread */
    public Thread getSolverThread() {
        return iSolverThread;
    }

    /** Initialization */
    public void init() {
    }

    /** True, when solver should update progress (see {@link Progress}) */
    private boolean isUpdateProgress() {
        return iUpdateProgress;
    }

    /** True, when solver should update progress (see {@link Progress}) */
    public void setUpdateProgress(boolean updateProgress) {
        iUpdateProgress = updateProgress;
    }

    /** Last solution (when solver finishes) */
    public Solution<V, T> lastSolution() {
        return (iLastSolution == null ? iCurrentSolution : iLastSolution);
    }

    /** Current solution (during the search) */
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
        if (getSolverThread() != null) {
            iStop = true;
            try {
                getSolverThread().join();
            } catch (InterruptedException ex) {
            }
        }
    }

    /** True, if the solver is running */
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

    /** Called in each iteration, after a neighbour is assigned */
    protected void onAssigned(double startTime) {
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
                if (isUpdateProgress()) {
                    if (iCurrentSolution.getBestInfo() == null) {
                        iProgress.setPhase("Searching for initial solution ...", iCurrentSolution.getModel()
                                .variables().size());
                    } else {
                        iProgress.setPhase("Improving found solution ...");
                    }
                }
                long prog = 9999;
                sLogger.info("Initial solution:" + ToolBox.dict2string(iCurrentSolution.getInfo(), 1));
                if ((iSaveBestUnassigned < 0 || iSaveBestUnassigned >= iCurrentSolution.getModel()
                        .nrUnassignedVariables())
                        && (iCurrentSolution.getBestInfo() == null || getSolutionComparator().isBetterThanBestSolution(
                                iCurrentSolution))) {
                    if (iCurrentSolution.getModel().nrUnassignedVariables() == 0)
                        sLogger.info("Complete solution " + ToolBox.dict2string(iCurrentSolution.getInfo(), 1)
                                + " was found.");
                    synchronized (iCurrentSolution) {
                        iCurrentSolution.saveBest();
                    }
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
                        if (!listener.neighbourSelected(iCurrentSolution.getIteration(), neighbour)) {
                            neighbour = null;
                            continue;
                        }
                    }
                    if (neighbour == null) {
                        sLogger.debug("No neighbour selected.");
                        synchronized (iCurrentSolution) { // still update the
                                                          // solution (increase
                                                          // iteration etc.)
                            iCurrentSolution.update(JProf.currentTimeSec() - startTime);
                        }
                        continue;
                    }

                    // Assign selected value to the selected variable
                    synchronized (iCurrentSolution) {
                        neighbour.assign(iCurrentSolution.getIteration());
                        iCurrentSolution.update(JProf.currentTimeSec() - startTime);
                    }

                    onAssigned(startTime);

                    // Check if the solution is the best ever found one
                    if ((iSaveBestUnassigned < 0 || iSaveBestUnassigned >= iCurrentSolution.getModel()
                            .nrUnassignedVariables())
                            && (iCurrentSolution.getBestInfo() == null || getSolutionComparator()
                                    .isBetterThanBestSolution(iCurrentSolution))) {
                        if (iCurrentSolution.getModel().nrUnassignedVariables() == 0) {
                            iProgress.debug("Complete solution of value " + iCurrentSolution.getModel().getTotalValue()
                                    + " was found.");
                        }
                        synchronized (iCurrentSolution) {
                            iCurrentSolution.saveBest();
                        }
                    }

                    // Increment progress bar
                    if (isUpdateProgress()) {
                        if (iCurrentSolution.getBestInfo() != null
                                && iCurrentSolution.getModel().getBestUnassignedVariables() == 0) {
                            prog++;
                            if (prog == 10000) {
                                iProgress.setPhase("Improving found solution ...");
                                prog = 0;
                            } else {
                                iProgress.setProgress(prog / 100);
                            }
                        } else if ((iCurrentSolution.getBestInfo() == null || iCurrentSolution.getModel()
                                .getBestUnassignedVariables() > 0)
                                && (iCurrentSolution.getModel().variables().size() - iCurrentSolution.getModel()
                                        .nrUnassignedVariables()) > iProgress.getProgress()) {
                            iProgress.setProgress(iCurrentSolution.getModel().variables().size()
                                    - iCurrentSolution.getModel().nrUnassignedVariables());
                        }
                    }

                }

                // Finalization
                iLastSolution = iCurrentSolution;

                iProgress.setPhase("Done", 1);
                iProgress.incProgress();

                iSolverThread = null;
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
            }
            iSolverThread = null;
        }
    }
    
    /** Return true if {@link Solver#stopSolver()} was called */
    public boolean isStop() {
        return iStop;
    }

}
