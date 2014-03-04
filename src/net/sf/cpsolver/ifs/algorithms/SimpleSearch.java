package net.sf.cpsolver.ifs.algorithms;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.heuristics.StandardNeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.termination.TerminationCondition;
import net.sf.cpsolver.ifs.util.Callback;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.Progress;

/**
 * Simple search neighbour selection. <br>
 * <br>
 * It consists of the following three phases:
 * <ul>
 * <li>Construction phase ({@link StandardNeighbourSelection} until all variables are assigned)
 * <li>Hill-climbing phase ({@link HillClimber} until the given number if idle iterations)
 * <li>Simulated annealing phase ({@link SimulatedAnnealing} until timeout is reached)
 * <ul>
 * <li>Or great deluge phase (when Search.GreatDeluge is true, {@link GreatDeluge} until timeout is reached)
 * </ul>
 * <li>At the end (when {@link TerminationCondition#canContinue(Solution)} is false),
 * the search is finished with one sweep of final phase ({@link HillClimber} until the given number if idle iterations).
 * </ul>
 * <br>
 * <br>
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
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */
public class SimpleSearch<V extends Variable<V, T>, T extends Value<V, T>> implements NeighbourSelection<V, T>, TerminationCondition<V, T> {
    private Logger iLog = Logger.getLogger(SimpleSearch.class);
    private NeighbourSelection<V, T> iCon = null;
    private boolean iConstructionUntilComplete = false; 
    private StandardNeighbourSelection<V, T> iStd = null;
    private SimulatedAnnealing<V, T> iSA = null;
    private HillClimber<V, T> iHC = null;
    private HillClimber<V, T> iFin = null;
    private GreatDeluge<V, T> iGD = null;
    private int iPhase = -1;
    private boolean iUseGD = true;
    private Progress iProgress = null;
    private Callback iFinalPhaseFinished = null;
    private boolean iCanContinue = true;
    private TerminationCondition<V, T> iTerm = null;

    /**
     * Constructor
     * 
     * @param properties
     *            problem properties
     */
    public SimpleSearch(DataProperties properties) throws Exception {
        String construction = properties.getProperty("Construction.Class"); 
        if (construction != null) {
            try {
                @SuppressWarnings("unchecked")
                Class<NeighbourSelection<V, T>> constructionClass = (Class<NeighbourSelection<V, T>>)Class.forName(properties.getProperty("Construction.Class"));
                iCon = constructionClass.getConstructor(DataProperties.class).newInstance(properties);
                iConstructionUntilComplete = properties.getPropertyBoolean("Construction.UntilComplete", iConstructionUntilComplete);
            } catch (Exception e) {
                iLog.error("Unable to use " + construction + ": " + e.getMessage());
            }
        }
        iStd = new StandardNeighbourSelection<V, T>(properties);
        iSA = new SimulatedAnnealing<V, T>(properties);
        if (properties.getPropertyBoolean("Search.CountSteps", false))
            iHC = new StepCountingHillClimber<V, T>(properties, "Step Counting Hill Climbing");
        else
            iHC = new HillClimber<V, T>(properties);
        iFin = new HillClimber<V, T>(properties); iFin.setPhase("Finalization");
        iGD = new GreatDeluge<V, T>(properties);
        iUseGD = properties.getPropertyBoolean("Search.GreatDeluge", iUseGD);
    }

    /**
     * Initialization
     */
    @Override
    public void init(Solver<V, T> solver) {
        iStd.init(solver);
        if (iCon != null)
            iCon.init(solver);
        iSA.init(solver);
        iHC.init(solver);
        iFin.init(solver);
        iGD.init(solver);
        if (iTerm == null) {
            iTerm = solver.getTerminationCondition();
            solver.setTerminalCondition(this);
        }
        iCanContinue = true;
        iProgress = Progress.getInstance(solver.currentSolution().getModel());
    }

    /**
     * Neighbour selection. It consists of the following three phases:
     * <ul>
     * <li>Construction phase ({@link StandardNeighbourSelection} until all variables are assigned)
     * <li>Hill-climbing phase ({@link HillClimber} until the given number if idle iterations)
     * <li>Simulated annealing phase ({@link SimulatedAnnealing} until timeout is reached)
     * </ul>
     */
    @SuppressWarnings("fallthrough")
    @Override
    public synchronized Neighbour<V, T> selectNeighbour(Solution<V, T> solution) {
        Neighbour<V, T> n = null;
        if (!isFinalPhase() && !iTerm.canContinue(solution))
            setFinalPhase(null);
        switch (iPhase) {
            case -1:
                iPhase++;
                iLog.info("***** construction phase *****");
                if (solution.getModel().nrUnassignedVariables() > 0)
                    iProgress.setPhase("Searching for initial solution...", solution.getModel().variables().size());
            case 0:
                if (iCon != null && solution.getModel().nrUnassignedVariables() > 0) {
                    iProgress.setProgress(solution.getModel().variables().size() - solution.getModel().getBestUnassignedVariables());
                    n = iCon.selectNeighbour(solution);
                    if (n != null || iConstructionUntilComplete)
                        return n;
                }
                iPhase++;
                iLog.info("***** ifs phase *****");
            case 1:
                if (iStd != null && solution.getModel().nrUnassignedVariables() > 0) {
                    iProgress.setProgress(solution.getModel().variables().size() - solution.getModel().getBestUnassignedVariables());
                    return iStd.selectNeighbour(solution);
                }
                iPhase++;
                iLog.info("***** hill climbing phase *****");
            case 2:
                if (solution.getModel().nrUnassignedVariables() > 0)
                    return (iCon == null ? iStd : iCon).selectNeighbour(solution);
                n = iHC.selectNeighbour(solution);
                if (n != null)
                    return n;
                iPhase++;
                iLog.info("***** " + (iUseGD ? "great deluge" : "simulated annealing") + " phase *****");
            case 3:
                if (solution.getModel().nrUnassignedVariables() > 0)
                    return (iCon == null ? iStd : iCon).selectNeighbour(solution);
                if (iUseGD)
                    return iGD.selectNeighbour(solution);
                else
                    return iSA.selectNeighbour(solution);
            case 9999:
                n = iFin.selectNeighbour(solution);
                if (n != null)
                    return n;
                iPhase = -1;
                if (iFinalPhaseFinished != null)
                    iFinalPhaseFinished.execute();
                iCanContinue = false;
            default:
                return null;
        }
    }

    /**
     * Set final phase
     * 
     * @param finalPhaseFinished
     *            to be called when the final phase is finished
     **/
    public synchronized void setFinalPhase(Callback finalPhaseFinished) {
        iLog.info("***** final phase *****");
        iFinalPhaseFinished = finalPhaseFinished;
        iPhase = 9999;
    }

    /** Is final phase */
    public boolean isFinalPhase() {
        return iPhase == 9999;
    }

    /** Termination condition (i.e., has final phase finished) */
    @Override
    public boolean canContinue(Solution<V, T> currentSolution) {
        return iCanContinue;
    }

}
