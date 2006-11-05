package net.sf.cpsolver.ifs.solver;


import java.io.*;
import java.util.*;
import java.lang.reflect.*;

import net.sf.cpsolver.ifs.extension.*;
import net.sf.cpsolver.ifs.heuristics.*;
import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.perturbations.*;
import net.sf.cpsolver.ifs.solution.*;
import net.sf.cpsolver.ifs.termination.*;
import net.sf.cpsolver.ifs.util.*;

/**
 * IFS Solver.
 * <br><br>
 * The iterative forward search (IFS) algorithm is based on ideas of local search methods. However, in contrast to 
 * classical local search techniques, it operates over feasible, though not necessarily complete solutions. In such
 * a solution, some variables can be left unassigned. Still all hard constraints on assigned variables must be
 * satisfied. Similarly to backtracking based algorithms, this means that there are no violations of hard 
 * constraints.
 * <br><br>
 * This search works in iterations. During each step, an unassigned or assigned variable is initially selected. 
 * Typically an unassigned variable is chosen like in backtracking-based search. An assigned variable may be selected 
 * when all variables are assigned but the solution is not good enough (for example, when there are still many 
 * violations of soft constraints). Once a variable is selected, a value from its domain is chosen for assignment.
 * Even if the best value is selected (whatever “best” means), its assignment to the selected variable may cause some 
 * hard conflicts with already assigned variables. Such conflicting variables are removed from the solution and become 
 * unassigned. Finally, the selected value is assigned to the selected variable.
 * <br><br>
 * Algorithm schema:
 * <ul><code>
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
 * </ul><br>
 * The algorithm attempts to move from one (partial) feasible solution to another via repetitive assignment of a 
 * selected value to a selected variable. During this search, the feasibility of all hard constraints in each 
 * iteration step is enforced by unassigning the conflicting variables. The search is terminated when the requested 
 * solution is found or when there is a timeout, expressed e.g., as a maximal number of iterations or available 
 * time being reached. The best solution found is then returned.
 * <br><br>
 * The above algorithm schema is parameterized by several functions, namely:
 * <ul>
 * <li> the termination condition (function canContinue, see {@link TerminationCondition}),
 * <li> the solution comparator (function better, see {@link SolutionComparator}),
 * <li> the variable selection (function selectVariable, see {@link VariableSelection}) and
 * <li> the value selection (function selectValue, see {@link ValueSelection}).
 * </ul>
 * <br>
 * Usage:<ul><code>
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
 * </code></ul>
 * <br>
 * Solver's parameters:
 * <br>
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>General.SaveBestUnassigned</td><td>{@link Integer}</td><td>During the search, solution is saved when it is the best ever found solution and if the number of assigned variables is less or equal this parameter (if set to -1, the solution is always saved)</td></tr>
 * <tr><td>General.Seed</td><td>{@link Long}</td><td>If set, random number generator is initialized with this seed</td></tr>
 * <tr><td>General.SaveConfiguration</td><td>{@link Boolean}</td><td>If true, given configuration is stored into the output folder (during initialization of the solver, ${General.Output}/${General.ProblemName}.properties)</td></tr>
 * <tr><td>Solver.AutoConfigure</td><td>{@link Boolean}</td><td>If true, IFS Solver is configured according to the following parameters</td></tr>
 * <tr><td>Termination.Class</td><td>{@link String}</td><td>Fully qualified class name of the termination condition (see {@link TerminationCondition}, e.g. {@link GeneralTerminationCondition})</td></tr>
 * <tr><td>Comparator.Class</td><td>{@link String}</td><td>Fully qualified class name of the solution comparator (see {@link SolutionComparator}, e.g. {@link GeneralSolutionComparator})</td></tr>
 * <tr><td>Value.Class</td><td>{@link String}</td><td>Fully qualified class name of the value selection criterion (see {@link ValueSelection}, e.g. {@link GeneralValueSelection})</td></tr>
 * <tr><td>Variable.Class</td><td>{@link String}</td><td>Fully qualified class name of the variable selection criterion (see {@link VariableSelection}, e.g. {@link GeneralVariableSelection})</td></tr>
 * <tr><td>PerturbationCounter.Class</td><td>{@link String}</td><td>Fully qualified class name of the perturbation counter in case of solving minimal perturbation problem (see {@link PerturbationsCounter}, e.g. {@link DefaultPerturbationsCounter})</td></tr>
 * <tr><td>Extensions.Classes</td><td>{@link String}</td><td>Semi-colon separated list of fully qualified class names of IFS extensions (see {@link Extension}, e.g. {@link ConflictStatistics} or {@link MacPropagation})</td></tr>
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
 * @version
 * IFS 1.0 (Iterative Forward Search)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@ktiml.mff.cuni.cz">muller@ktiml.mff.cuni.cz</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
**/

public class Solver {
    /** log */
    protected static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(Solver.class);
    /** current solution */
    protected Solution iCurrentSolution = null;
    /** last solution (after IFS Solver finishes) */
    protected Solution iLastSolution = null;
    
    /** solver status: done */
    protected boolean iDone = false;
    /** solver status: running */
    protected boolean iRunning = false;
    /** solver status: stopped */
    protected boolean iStop = false;
    
    /** solver thread */
    protected SolverThread iSolverThread = null;
    /** configuration */
    private DataProperties iProperties = null;
    
    private TerminationCondition iTerminationCondition = null;
    private SolutionComparator iSolutionComparator = null;
    private PerturbationsCounter iPerturbationsCounter = null;
    private ValueSelection iValueSelection = null;
    private VariableSelection iVariableSelection = null;
    private Vector iExtensions = new FastVector(5);
    private Vector iSolverListeners = new FastVector(5);
    private int iSaveBestUnassigned = 0;
    
    /** Constructor.
     * @param properties input configuration
     */
    public Solver(DataProperties properties) {
        iProperties = properties;
        
        long seed = properties.getPropertyLong( "General.Seed", System.currentTimeMillis());
        ToolBox.setSeed(seed);
        
        iSaveBestUnassigned = properties.getPropertyInt( "General.SaveBestUnassigned", 0);
        
        clearBest();
        if (iProperties.getPropertyBoolean("Solver.AutoConfigure",true)) {
            autoConfigure();
        }
    }
    
    private boolean iValueExtraUsed = false;
    private boolean iVariableExtraUsed = false;
    
    /** Sets termination condition */
    public void setTerminalCondition(TerminationCondition terminationCondition) {iTerminationCondition = terminationCondition; }
    /** Sets solution comparator */
    public void setSolutionComparator(SolutionComparator solutionComparator) {iSolutionComparator = solutionComparator; }
    /** Sets value selection criterion */
    public void setValueSelection(ValueSelection valueSelection) { iValueSelection = valueSelection; }
    /** Sets variable selection criterion */
    public void setVariableSelection(VariableSelection variableSelection) { iVariableSelection = variableSelection; }
    /** Sets perturbation counter (minimal perturbation problem) */
    public void setPerturbationsCounter(PerturbationsCounter perturbationsCounter) { iPerturbationsCounter = perturbationsCounter; }
    /** Add an IFS extension */
    public void addExtension(Extension extension) {
        if (extension.useValueExtra() && iValueExtraUsed) {
            sLogger.warn("Unable to add an extension "+extension+" -- value extra is already used.");
            return;
        }
        if (extension.useVariableExtra() && iVariableExtraUsed) {
            sLogger.warn("Unable to add extension "+extension+" -- variable extra is already used.");
            return;
        }
        iValueExtraUsed = iValueExtraUsed | extension.useValueExtra();
        iValueExtraUsed = iVariableExtraUsed | extension.useVariableExtra();
        iExtensions.addElement(extension);
    }
    
    /** Returns termination condition */
    public TerminationCondition getTerminationCondition() { return iTerminationCondition; }
    /** Returns solution comparator */
    public SolutionComparator getSolutionComparator() { return iSolutionComparator; }
    /** Returns values selection criterion */
    public ValueSelection getValueSelection() { return iValueSelection; }
    /** Returns variable selection criterion */
    public VariableSelection getVariableSelection() { return iVariableSelection; }
    /** Returns perturbation counter (minimal perturbation problem) */
    public PerturbationsCounter getPerturbationsCounter() { return iPerturbationsCounter; }
    /** Returns list of all used extensions */
    public Vector getExtensions() { return iExtensions; }
    
    /** Adds a solver listener */
    public void addSolverListener(SolverListener listener) {
        iSolverListeners.addElement(listener);
    }
    /** Removes a solver listener */
    public void removeSolverListener(SolverListener listener) {
        iSolverListeners.removeElement(listener);
    }
    
    /** Returns configuration */
    public DataProperties getProperties() { return iProperties; }
    
    /** Automatic configuratin of the solver -- when Solver.AutoConfigure is true */
    protected void autoConfigure() {
        try {
            boolean mpp = getProperties().getPropertyBoolean("General.MPP", false);
            
            String terminationConditionClassName = getProperties().getProperty("Termination.Class",(mpp?"net.sf.cpsolver.ifs.termination.MPPTerminationCondition":"net.sf.cpsolver.ifs.termination.GeneralTerminationCondition"));
            sLogger.info("Using "+terminationConditionClassName);
            Class terminationConditionClass = Class.forName(terminationConditionClassName);
            Constructor terminationConditionConstructor = terminationConditionClass.getConstructor(new Class[]{DataProperties.class});
            setTerminalCondition((TerminationCondition)terminationConditionConstructor.newInstance(new Object[] {getProperties()}));
            
            String solutionComparatorClassName = getProperties().getProperty("Comparator.Class",(mpp?"net.sf.cpsolver.ifs.solution.MPPSolutionComparator":"net.sf.cpsolver.ifs.solution.GeneralSolutionComparator"));
            sLogger.info("Using "+solutionComparatorClassName);
            Class solutionComparatorClass = Class.forName(solutionComparatorClassName);
            Constructor solutionComparatorConstructor = solutionComparatorClass.getConstructor(new Class[]{DataProperties.class});
            setSolutionComparator((SolutionComparator)solutionComparatorConstructor.newInstance(new Object[] {getProperties()}));
            
            String valueSelectionClassName = getProperties().getProperty("Value.Class","net.sf.cpsolver.ifs.heuristics.GeneralValueSelection");
            sLogger.info("Using "+valueSelectionClassName);
            Class valueSelectionClass = Class.forName(valueSelectionClassName);
            Constructor valueSelectionConstructor = valueSelectionClass.getConstructor(new Class[]{DataProperties.class});
            setValueSelection((ValueSelection)valueSelectionConstructor.newInstance(new Object[] {getProperties()}));
            
            String variableSelectionClassName = getProperties().getProperty("Variable.Class","net.sf.cpsolver.ifs.heuristics.GeneralVariableSelection");
            sLogger.info("Using "+variableSelectionClassName);
            Class variableSelectionClass = Class.forName(variableSelectionClassName);
            Constructor variableSelectionConstructor = variableSelectionClass.getConstructor(new Class[]{DataProperties.class});
            setVariableSelection((VariableSelection)variableSelectionConstructor.newInstance(new Object[] {getProperties()}));
            
            String perturbationCounterClassName = getProperties().getProperty("PerturbationCounter.Class","net.sf.cpsolver.ifs.perturbations.DefaultPerturbationsCounter");
            sLogger.info("Using "+perturbationCounterClassName);
            Class perturbationCounterClass = Class.forName(perturbationCounterClassName);
            Constructor perturbationCounterConstructor = perturbationCounterClass.getConstructor(new Class[]{DataProperties.class});
            setPerturbationsCounter((PerturbationsCounter)perturbationCounterConstructor.newInstance(new Object[] {getProperties()}));
            
            String extensionClassNames = getProperties().getProperty("Extensions.Classes",null);
            if (extensionClassNames!=null) {
                StringTokenizer extensionClassNameTokenizer = new StringTokenizer(extensionClassNames,";");
                while (extensionClassNameTokenizer.hasMoreTokens()) {
                    String extensionClassName = extensionClassNameTokenizer.nextToken();
                    sLogger.info("Using "+extensionClassName);
                    Class extensionClass = Class.forName(extensionClassName);
                    Constructor extensionConstructor = extensionClass.getConstructor(new Class[]{Solver.class, DataProperties.class});
                    addExtension((Extension)extensionConstructor.newInstance(new Object[] {this, getProperties()}));
                }
            }
        } catch (Exception e) {
            sLogger.error("Unable to autoconfigure solver.",e);
        }
    }
    
    /** Clears best solution */
    public void clearBest() {
        if (iCurrentSolution!=null) iCurrentSolution.clearBest();
    }
    
    /** Sets initial solution */
    public void setInitalSolution(Solution solution) {
        iCurrentSolution = solution;
    }
    
    /** Sets initial solution */
    public void setInitalSolution(Model model) {
        iCurrentSolution = new Solution(model, 0, 0);
    }
    
    /** Starts solver */
    public void start() {
        iSolverThread = new SolverThread();
        iSolverThread.start();
    }
    
    /** Returns solver's thread */
    public Thread getSolverThread() {
        return iSolverThread;
    }
    
    /** Initialization */
    public void init() {
        iStop = false;
        iDone = false;
        iRunning = false;
    }
    
    /** Last solution (when solver finishes) */
    public Solution lastSolution() { return (iLastSolution==null?iCurrentSolution:iLastSolution); }
    /** Current solution (during the search) */
    public Solution currentSolution() { return iCurrentSolution; }
    /** Solver status: solver is done */
    public boolean isDone() { return iDone; }
    /** Solver status: solver is running */
    public boolean isRunning() { return iRunning; }
    /** Stops the running solver */
    public void stopSolver() { iStop = true; };
    /** Solver status: solver is stopped */
    public boolean isStopped() { return iStop; }
    
    public void initSolver() {
        // Status
        iStop = false; iDone = false; iRunning = false;
        Progress.getInstance().setPhase("Initializing solver");
        
        // register extensions
        for (Enumeration i=iExtensions.elements(); i.hasMoreElements(); ) {
            Extension extension = (Extension)i.nextElement();
            extension.register(iCurrentSolution.getModel());
        }
        
        //register solution
        iCurrentSolution.init(Solver.this);
        
        //register and intialize value selection
        getValueSelection().init(Solver.this);
        
        //register and intialize value selection
        getVariableSelection().init(Solver.this);
        
        //register and intialize perturbations counter
        if (getPerturbationsCounter()!=null) getPerturbationsCounter().init(Solver.this);
        
        //save initial configuration
        if (iProperties.getPropertyBoolean("General.SaveConfiguration",false)) {
            try {
                FileOutputStream f = new FileOutputStream(iProperties.getProperty("General.Output")+File.separator+iProperties.getProperty("General.ProblemName","net.sf.cpsolver.ifs")+".properties");
                iProperties.store(f, iProperties.getProperty("General.ProblemNameLong","Iterative Forward Search")+"  -- configuration file");
                f.flush();f.close();
            } catch (Exception e) {
                sLogger.error("Unable to store configuration file :-(", e);
            }
        }
    }
    
    /** Solver thread */
    protected class SolverThread extends Thread {
        /** Solving rutine */
        public void run() {
            try {
                // Sets thread name
                setName("Solver");
                
                // Initialization
                Progress.getInstance().setStatus("Solving");
                initSolver();
                iRunning = true;

                double startTime = JProf.currentTimeSec();
                if (iCurrentSolution.getBestInfo()==null) {
                    Progress.getInstance().setPhase("Searching for initial solution ...",iCurrentSolution.getModel().variables().size());
                } else {
                    Progress.getInstance().setPhase("Improving found solution ...");
                }
                long prog = 9999;
                sLogger.info("Initial solution:"+ToolBox.dict2string(iCurrentSolution.getInfo(),1));
                if ((iSaveBestUnassigned<0 || iSaveBestUnassigned>=iCurrentSolution.getModel().unassignedVariables().size()) && (iCurrentSolution.getBestInfo()==null || getSolutionComparator().isBetterThanBestSolution(iCurrentSolution))) {
                    if (iCurrentSolution.getModel().unassignedVariables().isEmpty())
                        sLogger.info("Complete solution "+ToolBox.dict2string(iCurrentSolution.getInfo(),1)+" was found.");
                    synchronized (iCurrentSolution) {
                        iCurrentSolution.saveBest();
                    }
                }
                
                // Iterations: until solver can continue
                while (!iStop && getTerminationCondition().canContinue(iCurrentSolution)) {
                    // Variable selection
                    Variable variable = getVariableSelection().selectVariable(iCurrentSolution);
                    for (Enumeration i=iSolverListeners.elements();i.hasMoreElements();)
                        if (!((SolverListener)i.nextElement()).variableSelected(iCurrentSolution.getIteration(), variable)) continue;
                    if (variable == null) {
                        sLogger.warn("No variable selected.");
                    }
                    if (variable != null && variable.values().isEmpty()) {
                        sLogger.error("Variable "+variable.getName()+" has no values.");
                        continue;
                    }
                    
                    // Value selection
                    Value value = getValueSelection().selectValue(iCurrentSolution, variable);
                    for (Enumeration i=iSolverListeners.elements();i.hasMoreElements();)
                        if (!((SolverListener)i.nextElement()).valueSelected(iCurrentSolution.getIteration(), variable, value)) continue;
                    if (variable == null) {
                        sLogger.warn("No value selected for variable "+variable+".");
                    }
                    
                    // Assign selected value to the selected variable
                    synchronized (iCurrentSolution) {
                        if (value!=null) variable.assign(iCurrentSolution.getIteration(), value); else variable.unassign(iCurrentSolution.getIteration());
                        iCurrentSolution.update(JProf.currentTimeSec()-startTime);
                    }
                    
                    // Check if the solution is the best ever found one
                    if ((iSaveBestUnassigned<0 || iSaveBestUnassigned>=iCurrentSolution.getModel().unassignedVariables().size()) && (iCurrentSolution.getBestInfo()==null || getSolutionComparator().isBetterThanBestSolution(iCurrentSolution))) {
                        if (iCurrentSolution.getModel().unassignedVariables().isEmpty())
                            sLogger.info("Complete solution "+ToolBox.dict2string(iCurrentSolution.getInfo(),1)+" was found.");
                        synchronized (iCurrentSolution) {
                            iCurrentSolution.saveBest();
                        }
                    }
                    
                    // Increment progress bar
                    if (iCurrentSolution.getBestInfo()!=null && iCurrentSolution.getModel().getBestUnassignedVariables()==0) {
                        prog++;
                        if (prog == 10000) {
                            Progress.getInstance().setPhase("Improving found solution ...");
                            prog=0;
                        } else {
                            Progress.getInstance().setProgress(prog/100);
                        }
                    } else if ((iCurrentSolution.getBestInfo()==null || iCurrentSolution.getModel().getBestUnassignedVariables()>0) && (iCurrentSolution.getModel().variables().size()-iCurrentSolution.getModel().unassignedVariables().size())>Progress.getInstance().getProgress()) {
                        Progress.getInstance().setProgress(iCurrentSolution.getModel().variables().size()-iCurrentSolution.getModel().unassignedVariables().size());
                    }
                    
                }
                
                // Finalization
                iLastSolution = iCurrentSolution;
                if (!iStop) {
                    Progress.getInstance().setPhase("Done",1); Progress.getInstance().incProgress();
                }
                
                //Update status
                iDone = true; iRunning = false; iSolverThread=null;
                
                sLogger.debug("Solver stoped.");
            } catch (Exception ex) {
                sLogger.error(ex.getMessage(),ex);
            }
        }
    }
    
}
