package org.cpsolver.ifs.algorithms;

import org.apache.logging.log4j.Logger;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentContext;
import org.cpsolver.ifs.assignment.context.NeighbourSelectionWithContext;
import org.cpsolver.ifs.heuristics.MaxIdleNeighbourSelection;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.heuristics.StandardNeighbourSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.JProf;
import org.cpsolver.ifs.util.Progress;


/**
 * Simple search neighbour selection. <br>
 * <br>
 * It consists of the following three phases:
 * <ul>
 * <li>Construction phase ({@link StandardNeighbourSelection} until all variables are assigned)
 * <li>Hill-climbing phase ({@link HillClimber} until the given number if idle iterations)
 * <li>Simulated annealing phase ({@link SimulatedAnnealing} until timeout is reached)
 * <li>Or great deluge phase (when Search.GreatDeluge is true, {@link GreatDeluge} until timeout is reached)
 * </ul>
 * <br>
 * <br>
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
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 * @param <V> Variable
 * @param <T> Value
 */
public class SimpleSearch<V extends Variable<V, T>, T extends Value<V, T>> extends NeighbourSelectionWithContext<V,T,SimpleSearch<V,T>.SimpleSearchContext> {
    private Logger iLog = org.apache.logging.log4j.LogManager.getLogger(SimpleSearch.class);
    private NeighbourSelection<V, T> iCon = null;
    private boolean iConstructionUntilComplete = false; 
    private NeighbourSelection<V, T> iStd = null;
    private SimulatedAnnealing<V, T> iSA = null;
    private HillClimber<V, T> iHC = null;
    private GreatDeluge<V, T> iGD = null;
    private boolean iUseGD = true;
    private Progress iProgress = null;
    private int iMaxIdleIterations = 1000;
    private boolean iAllowUnassignments = false;
    
    private int iTimeOut;
    private double iStartTime;

    /**
     * Constructor
     * @param properties problem configuration
     * @throws Exception thrown when initialization fails (e.g., a given class is not found)
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
            iHC = new StepCountingHillClimber<V, T>(properties);
        else
            iHC = new HillClimber<V, T>(properties);
        iGD = new GreatDeluge<V, T>(properties);
        iUseGD = properties.getPropertyBoolean("Search.GreatDeluge", iUseGD);
        iMaxIdleIterations = properties.getPropertyInt("Search.MaxIdleIterations", iMaxIdleIterations);
        if (iMaxIdleIterations >= 0) {
            iStd = new MaxIdleNeighbourSelection<V, T>(properties, iStd, iMaxIdleIterations);
            if (iCon != null && !iConstructionUntilComplete)
                iCon = new MaxIdleNeighbourSelection<V, T>(properties, iCon, iMaxIdleIterations);
        }
        iTimeOut = properties.getPropertyInt("Termination.TimeOut", 1800);
        iAllowUnassignments = properties.getPropertyBoolean("Suggestion.AllowUnassignments", iAllowUnassignments);
    }

    /**
     * Initialization
     */
    @Override
    public void init(Solver<V, T> solver) {
        super.init(solver);
        if (!solver.hasSingleSolution())
            iCon = new ParallelConstruction<V, T>(solver.getProperties(), iCon == null ? iStd : iCon);
        iStd.init(solver);
        if (iCon != null)
            iCon.init(solver);
        iSA.init(solver);
        iHC.init(solver);
        iGD.init(solver);
        iProgress = Progress.getInstance(solver.currentSolution().getModel());
        solver.setUpdateProgress(false);
        iStartTime = JProf.currentTimeSec();
    }
    
    protected void updateProgress(int phase, Solution<V, T> solution) {
        if (!iHC.isMaster(solution)) return;
        if (phase < 2) {
            if (!"Searching for initial solution ...".equals(iProgress.getPhase()))
                iProgress.setPhase("Searching for initial solution ...", solution.getModel().variables().size());
            if (solution.getModel().getBestUnassignedVariables() >= 0)
                iProgress.setProgress(solution.getModel().variables().size() - solution.getModel().getBestUnassignedVariables());
            else
                iProgress.setProgress(solution.getAssignment().nrAssignedVariables());
        } else {
            if (!"Improving found solution ...".equals(iProgress.getPhase()))
                iProgress.setPhase("Improving found solution ...", 1000l);
            double time = JProf.currentTimeSec() - iStartTime;
            iProgress.setProgress(Math.min(1000l, Math.round(1000 * time / iTimeOut)));
        }
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
    public Neighbour<V, T> selectNeighbour(Solution<V, T> solution) {
        SimpleSearchContext context = getContext(solution.getAssignment());
        updateProgress(context.getPhase(), solution);
        Neighbour<V, T> n = null;
        switch (context.getPhase()) {
            case -1:
                context.setPhase(0);
                iProgress.info("[" + Thread.currentThread().getName() + "] Construction...");
            case 0:
                if (iCon != null && solution.getModel().nrUnassignedVariables(solution.getAssignment()) > 0) {
                    n = iCon.selectNeighbour(solution);
                    if (n != null || iConstructionUntilComplete)
                        return n;
                }
                context.setPhase(1);
                iProgress.info("[" + Thread.currentThread().getName() + "] IFS...");
            case 1:
                if (iStd != null && solution.getModel().nrUnassignedVariables(solution.getAssignment()) > 0) {
                    n = iStd.selectNeighbour(solution);
                    if (n != null) return n;
                }
                context.setPhase(2);
                if (solution.getModel().getBestUnassignedVariables() >= 0 && solution.getModel().getBestUnassignedVariables() < solution.getAssignment().nrUnassignedVariables(solution.getModel()))
                    solution.restoreBest();
            case 2:
                if (iMaxIdleIterations < 0 && solution.getModel().nrUnassignedVariables(solution.getAssignment()) > 0) {
                    n = (iCon == null ? iStd : iCon).selectNeighbour(solution);
                    if (n != null) return n;
                }
                if (iMaxIdleIterations >= 0 && !iAllowUnassignments && solution.getModel().getBestUnassignedVariables() >= 0 && solution.getModel().getBestUnassignedVariables() < solution.getAssignment().nrUnassignedVariables(solution.getModel()))
                    solution.restoreBest();
                n = iHC.selectNeighbour(solution);
                if (n != null)
                    return n;
                context.setPhase(3);
            case 3:
                if (iMaxIdleIterations < 0 && solution.getModel().nrUnassignedVariables(solution.getAssignment()) > 0) {
                    n = (iCon == null ? iStd : iCon).selectNeighbour(solution);
                    if (n != null) return n;
                }
                if (iMaxIdleIterations >= 0 && !iAllowUnassignments && solution.getModel().getBestUnassignedVariables() >= 0 && solution.getModel().getBestUnassignedVariables() < solution.getAssignment().nrUnassignedVariables(solution.getModel()))
                    solution.restoreBest();
                if (iUseGD)
                    return iGD.selectNeighbour(solution);
                else
                    return iSA.selectNeighbour(solution);
            default:
                return null;
        }
    }

    @Override
    public SimpleSearchContext createAssignmentContext(Assignment<V, T> assignment) {
        return new SimpleSearchContext();
    }

    public class SimpleSearchContext implements AssignmentContext {
        private int iPhase = -1;
        
        public int getPhase() { return iPhase; }
        public void setPhase(int phase) { iPhase = phase; }
    }
}
