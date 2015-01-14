package net.sf.cpsolver.ifs.algorithms;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Hill climber. In each iteration, one of the given neighbourhoods is selected first,
 * then a neighbour is generated and it is accepted if its value
 * {@link Neighbour#value()} is below or equal to zero. The search is
 * stopped after a given amount of idle iterations ( can be defined by problem
 * property HillClimber.MaxIdle). <br>
 * <br>
 * Custom neighbours can be set using HillClimber.Neighbours property that should
 * contain semicolon separated list of {@link NeighbourSelection}. By default, 
 * each neighbour selection is selected with the same probability (each has 1 point in
 * a roulette wheel selection). It can be changed by adding &nbsp;@n at the end
 * of the name of the class, for example:<br>
 * <code>
 * HillClimber.Neighbours=net.sf.cpsolver.ifs.algorithms.neighbourhoods.RandomMove;net.sf.cpsolver.ifs.algorithms.neighbourhoods.RandomSwapMove@0.1
 * </code>
 * <br>
 * Selector RandomSwapMove is 10&times; less probable to be selected than other selectors.
 * When HillClimber.Random is true, all selectors are selected with the same probability, ignoring these weights.
 * <br><br>
 * When HillClimber.Update is true, {@link NeighbourSelector#update(Neighbour, long)} is called 
 * after each iteration (on the selector that was used) and roulette wheel selection 
 * that is using {@link NeighbourSelector#getPoints()} is used to pick a selector in each iteration. 
 * See {@link NeighbourSelector} for more details. 
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
public class HillClimber<V extends Variable<V, T>, T extends Value<V, T>> extends NeighbourSearch<V, T> {
    protected int iMaxIdleIters = 10000;
    protected int iLastImprovingIter = 0;
    protected double iBestValue = 0;
    protected boolean iSetHCMode = false;

    /**
     * Constructor
     * <ul>
     * <li>HillClimber.MaxIdle ... maximum number of idle iterations (default is 200000)
     * <li>HillClimber.Neighbours ... semicolon separated list of classes implementing {@link NeighbourSelection}
     * <li>HillClimber.AdditionalNeighbours ... semicolon separated list of classes implementing {@link NeighbourSelection}
     * <li>HillClimber.Random ... when true, a neighbour selector is selected randomly
     * <li>HillClimber.Update ... when true, a neighbour selector is selected using {@link NeighbourSelector#getPoints()} weights (roulette wheel selection)
     * </ul>
     */
    public HillClimber(DataProperties properties) {
        super(properties);
        iMaxIdleIters = properties.getPropertyInt(getParameterBaseName() + ".MaxIdle", iMaxIdleIters);
        iSetHCMode = properties.getPropertyBoolean(getParameterBaseName() + ".SetHCMode", iSetHCMode);
    }
    
    /**
     * Set progress phase name
     */
    public void setPhase(String phase) {
        iPhase = phase;
    }

    /**
     * Initialization
     */
    @Override
    public void init(Solver<V, T> solver) {
        super.init(solver);
        if (iSetHCMode)
            setHCMode(true);
    }
    
    /**
     * Increase iteration counter
     */
    @Override
    protected void incIteration(Solution<V, T> solution) {
        super.incIteration(solution);
        if (iIter % 10000 == 0) {
            iLog.info("Iter=" + (iIter / 1000)+"k, NonImpIter=" + iDF2.format((iIter-iLastImprovingIter)/1000.0)+"k, Speed="+iDF2.format(1000.0*iIter/getTimeMillis())+" it/s");
            logNeibourStatus();
        }
        iProgress.setProgress(Math.round(100.0 * (iIter - iLastImprovingIter) / iMaxIdleIters));
    }
    
    /**
     * Stop the search after a given number of idle (not improving) iterations
     */
    @Override
    protected boolean canContinue(Solution<V, T> solution) {
        return iIter - iLastImprovingIter < iMaxIdleIters;
    }
    
    /**
     * Reset the idle iterations counter
     */
    @Override
    protected void activate(Solution<V, T> solution) {
        super.activate(solution);
        iLastImprovingIter = iIter;
    }
    
    /**
     * Memorize the iteration when the last best solution was found.
     */
    @Override
    public void bestSaved(Solution<V, T> solution) {
        if (Math.abs(iBestValue - solution.getBestValue()) >= 1.0) {
            iLastImprovingIter = iIter;
            iBestValue = solution.getBestValue();
        }
    }
    
    /**
     * Accept any move that does not worsen the solution (value <= 0)
     */
    @Override
    protected boolean accept(Model<V, T> model, Neighbour<V, T> neighbour, double value, boolean lazy) {
        return value <= 0;
    }

    /**
     * All parameters start with HillClimber base name, e.g., HillClimber.MaxIdle 
     */
    @Override
    public String getParameterBaseName() { return "HillClimber"; }
}
