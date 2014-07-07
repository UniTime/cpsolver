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
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Simulated annealing. In each iteration, one of the given neighbourhoods is selected first,
 * then a neighbour is generated and it is accepted with probability
 * {@link SimulatedAnnealing#prob(double)}. The search is guided by the
 * temperature, which starts at <i>SimulatedAnnealing.InitialTemperature</i>.
 * After each <i>SimulatedAnnealing.TemperatureLength</i> iterations, the
 * temperature is reduced by <i>SimulatedAnnealing.CoolingRate</i>. If there was
 * no improvement in the past <i>SimulatedAnnealing.ReheatLengthCoef *
 * SimulatedAnnealing.TemperatureLength</i> iterations, the temperature is
 * increased by <i>SimulatedAnnealing.ReheatRate</i>. If there was no
 * improvement in the past <i>SimulatedAnnealing.RestoreBestLengthCoef *
 * SimulatedAnnealing.TemperatureLength</i> iterations, the best ever found
 * solution is restored. <br>
 * <br>
 * If <i>SimulatedAnnealing.StochasticHC</i> is true, the acceptance probability
 * is computed using stochastic hill climbing criterion, i.e.,
 * <code>1.0 / (1.0 + Math.exp(value/temperature))</code>, otherwise it is
 * cumputed using simlated annealing criterion, i.e.,
 * <code>(value<=0.0?1.0:Math.exp(-value/temperature))</code>. If
 * <i>SimulatedAnnealing.RelativeAcceptance</i> neighbour value
 * {@link Neighbour#value()} is taken as the value of the selected
 * neighbour (difference between the new and the current solution, if the
 * neighbour is accepted), otherwise the value is computed as the difference
 * between the value of the current solution if the neighbour is accepted and
 * the best ever found solution. <br>
 * <br>
 * Custom neighbours can be set using SimulatedAnnealing.Neighbours property that should
 * contain semicolon separated list of {@link NeighbourSelection}. By default, 
 * each neighbour selection is selected with the same probability (each has 1 point in
 * a roulette wheel selection). It can be changed by adding &nbsp;@n at the end
 * of the name of the class, for example:<br>
 * <code>
 * SimulatedAnnealing.Neighbours=net.sf.cpsolver.ifs.algorithms.neighbourhoods.RandomMove;net.sf.cpsolver.ifs.algorithms.neighbourhoods.RandomSwapMove@0.1
 * </code>
 * <br>
 * Selector RandomSwapMove is 10&times; less probable to be selected than other selectors.
 * When SimulatedAnnealing.Random is true, all selectors are selected with the same probability, ignoring these weights.
 * <br><br>
 * When SimulatedAnnealing.Update is true, {@link NeighbourSelector#update(Neighbour, long)} is called 
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
public class SimulatedAnnealing<V extends Variable<V, T>, T extends Value<V, T>> extends NeighbourSearch<V,T> {
    private DecimalFormat iDF5 = new DecimalFormat("0.00000");
    private DecimalFormat iDF10 = new DecimalFormat("0.0000000000");
    private double iInitialTemperature = 1.5;
    private double iCoolingRate = 0.95;
    private double iReheatRate = -1;
    private long iTemperatureLength = 25000;
    private long iReheatLength = 0;
    private long iRestoreBestLength = 0;
    private double iTemperature = 0.0;
    private double iReheatLengthCoef = 5.0;
    private double iRestoreBestLengthCoef = -1;
    private long iLastImprovingIter = 0;
    private long iLastReheatIter = 0;
    private long iLastCoolingIter = 0;
    private int iAcceptIter[] = new int[] { 0, 0, 0 };
    private boolean iStochasticHC = false;
    private int iMoves = 0;
    private double iAbsValue = 0;
    private double iBestValue = 0;

    private boolean iRelativeAcceptance = true;

    /**
     * Constructor. Following problem properties are considered:
     * <ul>
     * <li>SimulatedAnnealing.InitialTemperature ... initial temperature (default 1.5)
     * <li>SimulatedAnnealing.TemperatureLength ... temperature length (number of iterations between temperature decrements, default 25000)
     * <li>SimulatedAnnealing.CoolingRate ... temperature cooling rate (default 0.95)
     * <li>SimulatedAnnealing.ReheatLengthCoef ... temperature re-heat length coefficient (multiple of temperature length, default 5)
     * <li>SimulatedAnnealing.ReheatRate ... temperature re-heating rate (default (1/coolingRate)^(reheatLengthCoef*1.7))
     * <li>SimulatedAnnealing.RestoreBestLengthCoef ... restore best length coefficient (multiple of re-heat length, default reheatLengthCoef^2)
     * <li>SimulatedAnnealing.StochasticHC ... true for stochastic search acceptance criterion, false for simulated annealing acceptance (default false)
     * <li>SimulatedAnnealing.RelativeAcceptance ... true for relative acceptance (different between the new and the current solutions, if the neighbour is accepted), false for absolute acceptance (difference between the new and the best solutions, if the neighbour is accepted)
     * <li>SimulatedAnnealing.Neighbours ... semicolon separated list of classes implementing {@link NeighbourSelection}
     * <li>SimulatedAnnealing.AdditionalNeighbours ... semicolon separated list of classes implementing {@link NeighbourSelection}
     * <li>SimulatedAnnealing.Random ... when true, a neighbour selector is selected randomly
     * <li>SimulatedAnnealing.Update ... when true, a neighbour selector is selected using {@link NeighbourSelector#getPoints()} weights (roulette wheel selection)
     * </ul>
     * 
     * @param properties
     *            problem properties
     */
    public SimulatedAnnealing(DataProperties properties) {
        super(properties);
        iInitialTemperature = properties.getPropertyDouble(getParameterBaseName() + ".InitialTemperature", iInitialTemperature);
        iReheatRate = properties.getPropertyDouble(getParameterBaseName() + ".ReheatRate", iReheatRate);
        iCoolingRate = properties.getPropertyDouble(getParameterBaseName() + ".CoolingRate", iCoolingRate);
        iRelativeAcceptance = properties.getPropertyBoolean(getParameterBaseName() + ".RelativeAcceptance", iRelativeAcceptance);
        iStochasticHC = properties.getPropertyBoolean(getParameterBaseName() + ".StochasticHC", iStochasticHC);
        iTemperatureLength = properties.getPropertyLong(getParameterBaseName() + ".TemperatureLength", iTemperatureLength);
        iReheatLengthCoef = properties.getPropertyDouble(getParameterBaseName() + ".ReheatLengthCoef", iReheatLengthCoef);
        iRestoreBestLengthCoef = properties.getPropertyDouble(getParameterBaseName() + ".RestoreBestLengthCoef", iRestoreBestLengthCoef);
        if (iReheatRate < 0)
            iReheatRate = Math.pow(1 / iCoolingRate, iReheatLengthCoef * 1.7);
        if (iRestoreBestLengthCoef < 0)
            iRestoreBestLengthCoef = iReheatLengthCoef * iReheatLengthCoef;
    }
    
    /** Setup the temperature */
    @Override
    protected void activate(Solution<V, T> solution) {
        super.activate(solution);
        iTemperature = iInitialTemperature;
        iReheatLength = Math.round(iReheatLengthCoef * iTemperatureLength);
        iRestoreBestLength = Math.round(iRestoreBestLengthCoef * iTemperatureLength);
        iLastImprovingIter = iIter;
    }

    /**
     * Cool temperature
     */
    protected void cool(Solution<V, T> solution) {
        iTemperature *= iCoolingRate;
        iLog.info("Iter=" + iIter / 1000 + "k, NonImpIter=" + iDF2.format((iIter - iLastImprovingIter) / 1000.0)
                + "k, Speed=" + iDF2.format(1000.0 * iIter / (JProf.currentTimeMillis() - iT0)) + " it/s");
        iLog.info("Temperature decreased to " + iDF5.format(iTemperature) + " " + "(#moves=" + iMoves + ", rms(value)="
                + iDF2.format(Math.sqrt(iAbsValue / iMoves)) + ", " + "accept=-"
                + iDF2.format(100.0 * iAcceptIter[0] / iMoves) + "/"
                + iDF2.format(100.0 * iAcceptIter[1] / iMoves) + "/+"
                + iDF2.format(100.0 * iAcceptIter[2] / iMoves) + "%, "
                + (prob(-1) < 1.0 ? "p(-1)=" + iDF2.format(100.0 * prob(-1)) + "%, " : "") + 
                "p(+0.1)=" + iDF2.format(100.0 * prob(0.1)) + "%, " +
                "p(+1)=" + iDF2.format(100.0 * prob(1)) + "%, " +
                "p(+10)=" + iDF5.format(100.0 * prob(10)) + "%)");
        logNeibourStatus();
        iLastCoolingIter = iIter;
        iAcceptIter = new int[] { 0, 0, 0 };
        iMoves = 0;
        iAbsValue = 0;
    }

    /**
     * Reheat temperature
     */
    protected void reheat(Solution<V, T> solution) {
        iTemperature *= iReheatRate;
        iLog.info("Iter=" + iIter / 1000 + "k, NonImpIter=" + iDF2.format((iIter - iLastImprovingIter) / 1000.0)
                + "k, Speed=" + iDF2.format(1000.0 * iIter / (JProf.currentTimeMillis() - iT0)) + " it/s");
        iLog.info("Temperature increased to " + iDF5.format(iTemperature) + " "
                + (prob(-1) < 1.0 ? "p(-1)=" + iDF2.format(100.0 * prob(-1)) + "%, " : "") + "p(+1)="
                + iDF2.format(100.0 * prob(1)) + "%, " + "p(+10)=" + iDF5.format(100.0 * prob(10)) + "%, " + "p(+100)="
                + iDF10.format(100.0 * prob(100)) + "%)");
        logNeibourStatus();
        iLastReheatIter = iIter;
        iProgress.setPhase("Simulated Annealing [" + iDF2.format(iTemperature) + "]...");
    }

    /**
     * restore best ever found solution
     */
    protected void restoreBest(Solution<V, T> solution) {
        iLog.info("Best restored");
        iLastImprovingIter = iIter;
    }

    /**
     * Neighbour acceptance probability
     * 
     * @param value
     *            absolute or relative value of the proposed change (neighbour)
     * @return probability of acceptance of a change (neighbour)
     */
    protected double prob(double value) {
        if (iStochasticHC)
            return 1.0 / (1.0 + Math.exp(value / iTemperature));
        else
            return (value <= 0.0 ? 1.0 : Math.exp(-value / iTemperature));
    }

    /**
     * True if the given neighbour is to be be accepted
     * 
     * @param solution
     *            current solution
     * @param neighbour
     *            proposed move
     * @return true if generated random number is below
     *         {@link SimulatedAnnealing#prob(double)}
     */
    @Override
    protected boolean accept(Model<V, T> model, Neighbour<V, T> neighbour, double value, boolean lazy) {
        iMoves ++;
        iAbsValue += value * value;
        double prob = prob(iRelativeAcceptance ? value : (lazy ? model.getTotalValue() : value + model.getTotalValue()) - iBestValue);
        if (prob >= 1.0 || ToolBox.random() < prob) {
            iAcceptIter[value < 0.0 ? 0 : value > 0.0 ? 2 : 1]++;
            return true;
        }
        return false;
    }

    /**
     * Increment iteration counter, cool/reheat/restoreBest if necessary
     */
    @Override
    protected void incIteration(Solution<V, T> solution) {
        super.incIteration(solution);
        iIter++;
        if (iIter > iLastImprovingIter + iRestoreBestLength)
            restoreBest(solution);
        if (iIter > Math.max(iLastReheatIter, iLastImprovingIter) + iReheatLength)
            reheat(solution);
        if (iIter > iLastCoolingIter + iTemperatureLength)
            cool(solution);
        iProgress.setProgress(Math.round(100.0 * (iIter - Math.max(iLastReheatIter, iLastImprovingIter)) / iReheatLength));
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
        iLastImprovingIter = iIter;
    }

    @Override
    public String getParameterBaseName() { return "SimulatedAnnealing"; }
}
