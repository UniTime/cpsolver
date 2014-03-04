package net.sf.cpsolver.ifs.algorithms;

import java.text.DecimalFormat;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.JProf;

/**
 * Great deluge. In each iteration, one of the given neighbourhoods is selected first,
 * then a neighbour is generated and it is accepted if the value of the new
 * solution is below certain bound. This bound is initialized to the
 * GreatDeluge.UpperBoundRate &times; value of the best solution ever found.
 * After each iteration, the bound is decreased by GreatDeluge.CoolRate (new
 * bound equals to old bound &times; GreatDeluge.CoolRate). If the bound gets
 * bellow GreatDeluge.LowerBoundRate &times; value of the best solution ever
 * found, it is changed back to GreatDeluge.UpperBoundRate &times; value of the
 * best solution ever found.
 * <br><br>
 * If there was no improvement found between the bounds, the new bounds are
 * changed to GreatDeluge.UpperBoundRate^2 and GreatDeluge.LowerBoundRate^2,
 * GreatDeluge.UpperBoundRate^3 and GreatDeluge.LowerBoundRate^3, etc. till
 * there is an improvement found. <br>
 * <br>
 * Custom neighbours can be set using GreatDeluge.Neighbours property that should
 * contain semicolon separated list of {@link NeighbourSelection}. By default, 
 * each neighbour selection is selected with the same probability (each has 1 point in
 * a roulette wheel selection). It can be changed by adding &nbsp;@n at the end
 * of the name of the class, for example:<br>
 * <code>
 * GreatDeluge.Neighbours=net.sf.cpsolver.ifs.algorithms.neighbourhoods.RandomMove;net.sf.cpsolver.ifs.algorithms.neighbourhoods.RandomSwapMove@0.1
 * </code>
 * <br>
 * Selector RandomSwapMove is 10&times; less probable to be selected than other selectors.
 * When GreatDeluge.Random is true, all selectors are selected with the same probability, ignoring these weights.
 * <br><br>
 * When GreatDeluge.Update is true, {@link NeighbourSelector#update(Neighbour, long)} is called 
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
public class GreatDeluge<V extends Variable<V, T>, T extends Value<V, T>> extends NeighbourSearch<V, T> {
    private DecimalFormat sDF2 = new DecimalFormat("0.00");
    private DecimalFormat sDF5 = new DecimalFormat("0.00000");
    private double iBound = 0.0;
    private double iCoolRate = 0.9999999;
    private double iUpperBound;
    private double iUpperBoundRate = 1.05;
    private double iLowerBoundRate = 0.95;
    private int iMoves = 0;
    private int iAcceptedMoves = 0;
    private int iNrIdle = 0;
    private long iLastImprovingIter = 0;
    private double iBestValue = 0;

    /**
     * Constructor. Following problem properties are considered:
     * <ul>
     * <li>GreatDeluge.CoolRate ... bound cooling rate (default 0.99999995)
     * <li>GreatDeluge.UpperBoundRate ... bound upper bound relative to best solution ever found (default 1.05)
     * <li>GreatDeluge.LowerBoundRate ... bound lower bound relative to best solution ever found (default 0.95)
     * <li>GreatDeluge.Neighbours ... semicolon separated list of classes implementing {@link NeighbourSelection}
     * <li>GreatDeluge.AdditionalNeighbours ... semicolon separated list of classes implementing {@link NeighbourSelection}
     * <li>GreatDeluge.Random ... when true, a neighbour selector is selected randomly
     * <li>GreatDeluge.Update ... when true, a neighbour selector is selected using {@link NeighbourSelector#getPoints()} weights (roulette wheel selection)
     * </ul>
     * 
     * @param properties
     *            problem properties
     */
    public GreatDeluge(DataProperties properties) {
        super(properties);
        iCoolRate = properties.getPropertyDouble(getParameterBaseName() + ".CoolRate", iCoolRate);
        iUpperBoundRate = properties.getPropertyDouble(getParameterBaseName() + ".UpperBoundRate", iUpperBoundRate);
        iLowerBoundRate = properties.getPropertyDouble(getParameterBaseName() + ".LowerBoundRate", iLowerBoundRate);
    }

    /** Accept the given neighbour if it does not worsen the current solution or when the new solution is below the bound */
    @Override
    protected boolean accept(Model<V, T> model, Neighbour<V, T> neighbour, double value, boolean lazy) {
        iMoves ++;
        if (value <= 0.0 || (lazy ? model.getTotalValue() : value + model.getTotalValue()) < iBound) {
            iAcceptedMoves ++;
            return true;
        }
        return false;
    }
    
    /** Setup the bound */
    @Override
    protected void activate(Solution<V, T> solution) {
        super.activate(solution);
        iNrIdle = 0;
        iLastImprovingIter = 0;
        iBound = (solution.getBestValue() > 0.0 ? iUpperBoundRate * solution.getBestValue() : solution.getBestValue() / iUpperBoundRate);
        iUpperBound = iBound;
    }
    
    /** Increment iteration count, update bound */
    @Override
    protected void incIteration(Solution<V, T> solution) {
        super.incIteration(solution);
        if (solution.getBestValue() >= 0.0)
            iBound *= iCoolRate;
        else
            iBound /= iCoolRate;
        if (iIter % 10000 == 0) {
            iLog.info("Iter=" + iIter / 1000 + "k, NonImpIter=" + sDF2.format((iIter - iLastImprovingIter) / 1000.0)
                    + "k, Speed=" + sDF2.format(1000.0 * iIter / (JProf.currentTimeMillis() - iT0)) + " it/s");
            iLog.info("Bound is " + sDF2.format(iBound) + ", " + "best value is " + sDF2.format(solution.getBestValue())
                    + " (" + sDF2.format(100.0 * iBound / solution.getBestValue()) + "%), " + "current value is "
                    + sDF2.format(solution.getModel().getTotalValue()) + " ("
                    + sDF2.format(100.0 * iBound / solution.getModel().getTotalValue()) + "%), " + "#idle=" + iNrIdle
                    + ", " + "Pacc=" + sDF5.format(100.0 * iAcceptedMoves / iMoves) + "%");
            logNeibourStatus();
            iAcceptedMoves = iMoves = 0;
        }
        double lowerBound = (solution.getBestValue() >= 0.0
                ? Math.pow(iLowerBoundRate, 1 + iNrIdle) * solution.getBestValue()
                : solution.getBestValue() / Math.pow(iLowerBoundRate, 1 + iNrIdle));
        if (iBound < lowerBound) {
            iNrIdle++;
            iBound = Math.max(solution.getBestValue() + 2.0,(solution.getBestValue() >= 0.0 ?
                    Math.pow(iUpperBoundRate, iNrIdle) * solution.getBestValue() :
                    solution.getBestValue() / Math.pow(iUpperBoundRate, iNrIdle)));
            iUpperBound = iBound;
            iProgress.setPhase("Great Deluge [" + (1 + iNrIdle) + "]...");
        }
        iProgress.setProgress(100 - Math.round(100.0 * (iBound - lowerBound) / (iUpperBound - lowerBound)));
        iMoves++;
    }
    
    /** Update last improving iteration count */
    @Override
    public void bestSaved(Solution<V, T> solution) {
        if (Math.abs(iBestValue - solution.getBestValue()) >= 1.0) {
            iLastImprovingIter = iIter;
            iNrIdle = 0;
            iBestValue = solution.getBestValue();
        }
    }

    @Override
    public String getParameterBaseName() { return "GreatDeluge"; }
}
