package org.cpsolver.ifs.algorithms;

import java.text.DecimalFormat;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.JProf;
import org.cpsolver.ifs.util.ToolBox;


/**
 * Simulated annealing. In each iteration, one of the given neighbourhoods is selected first,
 * then a neighbour is generated and it is accepted with probability
 * {@link SimulatedAnnealingContext#prob(double)}. The search is guided by the
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
 * <code>(value&lt;=0.0?1.0:Math.exp(-value/temperature))</code>. If
 * <i>SimulatedAnnealing.RelativeAcceptance</i> neighbour value
 * {@link Neighbour#value(Assignment)} is taken as the value of the selected
 * neighbour (difference between the new and the current solution, if the
 * neighbour is accepted), otherwise the value is computed as the difference
 * between the value of the current solution if the neighbour is accepted and
 * the best ever found solution. <br>
 * <br>
 * Custom neighbours can be set using SimulatedAnnealing.Neighbours property that should
 * contain semicolon separated list of {@link NeighbourSelection}. By default, 
 * each neighbour selection is selected with the same probability (each has 1 point in
 * a roulette wheel selection). It can be changed by adding &nbsp;@n at the end
 * of the name of the class, for example:
 * <pre><code>
 * SimulatedAnnealing.Neighbours=org.cpsolver.ifs.algorithms.neighbourhoods.RandomMove;org.cpsolver.ifs.algorithms.neighbourhoods.RandomSwapMove@0.1
 * </code></pre>
 * Selector RandomSwapMove is 10&times; less probable to be selected than other selectors.
 * When SimulatedAnnealing.Random is true, all selectors are selected with the same probability, ignoring these weights.
 * <br><br>
 * When SimulatedAnnealing.Update is true, {@link NeighbourSelector#update(Assignment, Neighbour, long)} is called 
 * after each iteration (on the selector that was used) and roulette wheel selection 
 * that is using {@link NeighbourSelector#getPoints()} is used to pick a selector in each iteration. 
 * See {@link NeighbourSelector} for more details. 
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
public class SimulatedAnnealing<V extends Variable<V, T>, T extends Value<V, T>> extends NeighbourSearch<V,T> {
    private DecimalFormat iDF5 = new DecimalFormat("0.00000");
    private DecimalFormat iDF10 = new DecimalFormat("0.0000000000");
    private double iInitialTemperature = -1;
    private double iMaximalTemperature = 1.5;
    private double iMinimalTemperature = 0.001;
    private double iCoolingRate = 0.95;
    private double iReheatRate = -1;
    private long iTemperatureLength = 25000;
    private double iReheatLengthCoef = 5.0;
    private double iRestoreBestLengthCoef = -1;
    private boolean iStochasticHC = false;
    private boolean iRelativeAcceptance = true;
    private Double[] iCoolingRateAdjusts = null;
    private int iTrainingValues = 10000;
    private double iTrainingProbability = 0.00001;
    private double iTimeBetweenCooldowns = 10.0;

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
        iMaximalTemperature = properties.getPropertyDouble(getParameterBaseName() + ".MaximalTemperature", (iInitialTemperature <= 0.0 ? -1.0 : iMaximalTemperature));
        iMinimalTemperature = properties.getPropertyDouble(getParameterBaseName() + ".MinimalTemperature", (iInitialTemperature <= 0.0 ? -1.0 : 0.0));
        iReheatRate = properties.getPropertyDouble(getParameterBaseName() + ".ReheatRate", iReheatRate);
        iCoolingRate = properties.getPropertyDouble(getParameterBaseName() + ".CoolingRate", iCoolingRate);
        iRelativeAcceptance = properties.getPropertyBoolean(getParameterBaseName() + ".RelativeAcceptance", iRelativeAcceptance);
        iStochasticHC = properties.getPropertyBoolean(getParameterBaseName() + ".StochasticHC", iStochasticHC);
        iTemperatureLength = properties.getPropertyLong(getParameterBaseName() + ".TemperatureLength", iTemperatureLength);
        iReheatLengthCoef = properties.getPropertyDouble(getParameterBaseName() + ".ReheatLengthCoef", iReheatLengthCoef);
        iRestoreBestLengthCoef = properties.getPropertyDouble(getParameterBaseName() + ".RestoreBestLengthCoef", iRestoreBestLengthCoef);
        iCoolingRateAdjusts = properties.getPropertyDoubleArry(getParameterBaseName() + ".CoolingRateAdjustments", null);
        iTrainingValues = properties.getPropertyInt(getParameterBaseName() + ".TrainingValues", iTrainingValues);
        iTrainingProbability = properties.getPropertyDouble(getParameterBaseName() + ".TrainingProbability", iTrainingProbability);
        iTimeBetweenCooldowns = properties.getPropertyDouble(getParameterBaseName() + ".TimeBetweenCooldowns", iTimeBetweenCooldowns);
        if (iReheatRate < 0)
            iReheatRate = Math.pow(1 / iCoolingRate, iReheatLengthCoef * 1.7);
        if (iRestoreBestLengthCoef < 0)
            iRestoreBestLengthCoef = iReheatLengthCoef * iReheatLengthCoef;
    }
    
    @Override
    public String getParameterBaseName() { return "SimulatedAnnealing"; }
    
    @Override
    public NeighbourSearchContext createAssignmentContext(Assignment<V, T> assignment) {
        return new SimulatedAnnealingContext();
    }
    
    public class SimulatedAnnealingContext extends NeighbourSearchContext {
        private double iTemperature = 0.0;
        private int iMoves = 0;
        private double iAbsValue = 0;
        private double iBestValue = 0;
        private long iLastImprovingIter = -1;
        private long iLastBestIter = -1;
        private long iLastReheatIter = 0;
        private long iLastCoolingIter = 0;
        private int iAcceptIter[] = new int[] { 0, 0, 0 };
        private long iReheatLength = 0;
        private long iRestoreBestLength = 0;
        private int iTrainingIterations = 0;
        private double iTrainingTotal = 0.0;

        /** Setup the temperature */
        @Override
        protected void activate(Solution<V, T> solution) {
            super.activate(solution);
            iTrainingTotal = 0.0; iTrainingIterations = 0;
            iTemperature = iInitialTemperature;
            iReheatLength = Math.round(iReheatLengthCoef * iTemperatureLength);
            iRestoreBestLength = Math.round(iRestoreBestLengthCoef * iTemperatureLength);
            iLastImprovingIter = -1;
            iLastBestIter = -1;
        }
        
        protected double getCoolingRate(int idx) {
            if (idx < 0 || iCoolingRateAdjusts == null || idx >= iCoolingRateAdjusts.length || iCoolingRateAdjusts[idx] == null) return iCoolingRate;
            return iCoolingRate * iCoolingRateAdjusts[idx];
        }
        
        /**
         * Set initial temperature based on the training period
         * @param solution current solution
         */
        protected void train(Solution<V, T> solution) {
            double value = iTrainingTotal / iTrainingIterations;
            if (iStochasticHC) {
                iInitialTemperature = value / Math.log(1.0 / 0.01 * iTrainingProbability - 1.0);
                if (iMaximalTemperature <= 0.0)
                    iMaximalTemperature = value / Math.log(1.0 / iTrainingProbability - 1.0);
                if (iMinimalTemperature < 0.0)
                    iMinimalTemperature = 0.1 / Math.log(1.0 / iTrainingProbability - 1.0);
            } else {
                iInitialTemperature = - value / Math.log(0.01 * iTrainingProbability);
                if (iMaximalTemperature <= 0.0)
                    iMaximalTemperature = - value / Math.log(iTrainingProbability);
                if (iMinimalTemperature < 0.0)
                    iMinimalTemperature = - 0.1 / Math.log(iTrainingProbability);
            }
            iTemperature = iInitialTemperature;
            info("Iter=" + iIter / 1000 + (iLastImprovingIter < 0 ? "" : "k, NonImpIter=" + iDF2.format((iIter - iLastImprovingIter) / 1000.0))
                    + "k, Speed=" + iDF2.format(1000.0 * iIter / (JProf.currentTimeMillis() - iT0)) + " it/s, " +
                    "Value=" + iDF2.format(solution.getModel().getTotalValue(solution.getAssignment())) +
                    ", Best=" + iDF2.format(solution.getBestValue()) +
                    " (" + iDF2.format(100.0 * solution.getModel().getTotalValue(solution.getAssignment()) / solution.getBestValue()) + " %)");
            info("Temperature set to " + iDF5.format(iTemperature) + " " + "(" + 
                    "p(+0.1)=" + iDF2.format(100.0 * prob(0.1)) + "%, " +
                    "p(+1)=" + iDF2.format(100.0 * prob(1)) + "%, " +
                    "p(+10)=" + iDF5.format(100.0 * prob(10)) + "%, " +
                    "p(+" + iDF2.format(value) + ")=" + iDF5.format(100.0 * prob(value)) + "%)");
            info("Maximal temperature set to " + iDF5.format(iMaximalTemperature) + " " + "(" + 
                    "p(+0.1)=" + iDF2.format(100.0 * prob(0.1, iMaximalTemperature)) + "%, " +
                    "p(+1)=" + iDF2.format(100.0 * prob(1, iMaximalTemperature)) + "%, " +
                    "p(+10)=" + iDF5.format(100.0 * prob(10, iMaximalTemperature)) + "%, " +
                    "p(+" + iDF2.format(value) + ")=" + iDF5.format(100.0 * prob(value, iMaximalTemperature)) + "%)");
            info("Minimal temperature set to " + iDF5.format(iMinimalTemperature) + " " + "(" + 
                    "p(+0.001)=" + iDF2.format(100.0 * prob(0.001, iMinimalTemperature)) + "%, " +
                    "p(+0.01)=" + iDF2.format(100.0 * prob(0.01, iMinimalTemperature)) + "%, " +
                    "p(+0.1)=" + iDF5.format(100.0 * prob(0.1, iMinimalTemperature)) + "%)");
            logNeibourStatus();
            double speed = ((double)iIter) / (JProf.currentTimeMillis() - iT0); // iterations per ms
            iTemperatureLength = Math.round(speed * iTimeBetweenCooldowns * 1000.0);
            iReheatLength = Math.round(iReheatLengthCoef * iTemperatureLength);
            iRestoreBestLength = Math.round(iRestoreBestLengthCoef * iTemperatureLength);
            info("Training speed was " + iDF2.format(1000.0 * speed) + " it/s, temperature length adjusted to " + iTemperatureLength + ".");
            iIter = 0; iT0 = JProf.currentTimeMillis();
            iLastImprovingIter = -1; iLastBestIter = 0; iBestValue = solution.getBestValue();
            iAcceptIter = new int[] { 0, 0, 0 };
            iMoves = 0;
            iAbsValue = 0;
        }

        /**
         * Cool temperature
         * @param solution current solution
         */
        protected void cool(Solution<V, T> solution) {
            info("Iter=" + iIter / 1000 + (iLastImprovingIter < 0 ? "" : "k, NonImpIter=" + iDF2.format((iIter - iLastImprovingIter) / 1000.0))
                    + "k, Speed=" + iDF2.format(1000.0 * iIter / (JProf.currentTimeMillis() - iT0)) + " it/s, " +
                    "Value=" + iDF2.format(solution.getModel().getTotalValue(solution.getAssignment())) +
                    ", Best=" + iDF2.format(solution.getBestValue()) +
                    " (" + iDF2.format(100.0 * solution.getModel().getTotalValue(solution.getAssignment()) / solution.getBestValue()) + " %), " +
                    "Step=" + iDF2.format(1.0 * (iIter - Math.max(iLastReheatIter, iLastImprovingIter)) / iTemperatureLength));
            if (iLastImprovingIter < 0 || iIter > iLastImprovingIter + iTemperatureLength) {
                // Do not update the temperature when solution was improved recently 
                iTemperature *= getCoolingRate(solution.getAssignment().getIndex() - 1);
                info("Temperature decreased to " + iDF5.format(iTemperature) + " " + "(#moves=" + iMoves + ", rms(value)="
                        + iDF2.format(Math.sqrt(iAbsValue / iMoves)) + ", " + "accept=-"
                        + iDF2.format(100.0 * iAcceptIter[0] / iMoves) + "/"
                        + iDF2.format(100.0 * iAcceptIter[1] / iMoves) + "/+"
                        + iDF2.format(100.0 * iAcceptIter[2] / iMoves) + "%, "
                        + (prob(-1) < 1.0 ? "p(-1)=" + iDF2.format(100.0 * prob(-1)) + "%, " : "") + 
                        "p(+0.1)=" + iDF2.format(100.0 * prob(0.1)) + "%, " +
                        "p(+1)=" + iDF5.format(100.0 * prob(1)) + "%, " +
                        "p(+10)=" + iDF10.format(100.0 * prob(10)) + "%)");
                iAbsValue = 0;
                iAcceptIter = new int[] { 0, 0, 0 };
                iMoves = 0;
            }
            logNeibourStatus();
            iLastCoolingIter = iIter;
        }

        /**
         * Reheat temperature
         * @param solution current solution
         */
        protected void reheat(Solution<V, T> solution) {
            iTemperature *= iReheatRate;
            info("Iter=" + iIter / 1000 + (iLastImprovingIter < 0 ? "" : "k, NonImpIter=" + iDF2.format((iIter - iLastImprovingIter) / 1000.0))
                    + "k, Speed=" + iDF2.format(1000.0 * iIter / (JProf.currentTimeMillis() - iT0)) + " it/s, " +
                    "Value=" + iDF2.format(solution.getModel().getTotalValue(solution.getAssignment())) +
                    ", Best=" + iDF2.format(solution.getBestValue()) +
                    " (" + iDF2.format(100.0 * solution.getModel().getTotalValue(solution.getAssignment()) / solution.getBestValue()) + " %)");
            info("Temperature increased to " + iDF5.format(iTemperature) + " "
                    + (prob(-1) < 1.0 ? "p(-1)=" + iDF2.format(100.0 * prob(-1)) + "%, " : "") + "p(+0.1)="
                    + iDF2.format(100.0 * prob(0.1)) + "%, " + "p(+1)=" + iDF5.format(10.0 * prob(1)) + "%, " + "p(+10)="
                    + iDF10.format(100.0 * prob(10)) + "%)");
            logNeibourStatus();
            iLastReheatIter = iIter;
            if (iTemperature > iMaximalTemperature) {
                // Max temperature reached -> restore best solution and stop re-heating
                restoreBest(solution);
                iLastImprovingIter = -1;
            }
            iBestValue = solution.getBestValue();
            setProgressPhase("Simulated Annealing [" + iDF5.format(iTemperature) + "]...");
        }

        /**
         * restore best ever found solution
         * @param solution current solution
         */
        protected void restoreBest(Solution<V, T> solution) {
            info("Best solution restored.");
            solution.restoreBest();
            iLastBestIter = iIter;
        }

        /**
         * Neighbour acceptance probability
         * 
         * @param value
         *            absolute or relative value of the proposed change (neighbour)
         * @return probability of acceptance of a change (neighbour)
         */
        protected double prob(double value) {
            return prob(value, iTemperature);
        }
        
        /**
         * Neighbour acceptance probability
         * 
         * @param value
         *            absolute or relative value of the proposed change (neighbour)
         * @param temp
         *      current temperature
         * @return probability of acceptance of a change (neighbour)
         */
        protected double prob(double value, double temp) {
            if (iStochasticHC)
                return 1.0 / (1.0 + Math.exp(value / temp));
            else
                return (value <= 0.0 ? 1.0 : Math.exp(-value / temp));
        }

        /**
         * True if the given neighbour is to be be accepted
         * 
         * @param assignment
         *            current assignment
         * @param neighbour
         *            proposed move
         * @return true if generated random number is below the generated probability
         */
        @Override
        protected boolean accept(Assignment<V, T> assignment, Model<V, T> model, Neighbour<V, T> neighbour, double value, boolean lazy) {
            iMoves ++;
            iAbsValue += value * value;
            double v = (iRelativeAcceptance ? value : (lazy ? model.getTotalValue(assignment) : value + model.getTotalValue(assignment)) - iBestValue);
            if (iTrainingIterations < iTrainingValues) {
                if (v <= 0.0) {
                    iAcceptIter[value < 0.0 ? 0 : value > 0.0 ? 2 : 1]++;
                    return true;
                } else {
                    iTrainingIterations ++; iTrainingTotal += v;
                }
                return false;
            }
            double prob = prob(v);
            if (v > 0) {
                iTrainingIterations ++; iTrainingTotal += v;
            }
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
            if (isMaster(solution)) {
                if (iInitialTemperature <= 0.0) {
                    if (iTrainingIterations < iTrainingValues) {
                        setProgress(Math.round(100.0 * iTrainingIterations / iTrainingValues));
                        return;
                    } else {
                        train(solution);
                    }
                }
                if (iLastBestIter >= 0 && iIter > iLastBestIter + iRestoreBestLength)
                    restoreBest(solution);
                if (iTemperature < iMinimalTemperature) { // Minimal temperature reached, start going up again
                    // If the current solution is more than 0.1% worse, restore best
                    if ((solution.getModel().getTotalValue(solution.getAssignment()) - solution.getBestValue()) / Math.abs(solution.getBestValue()) >= 0.001)
                        restoreBest(solution);
                    // Set last improving iteration so that the re-heating would continue
                    iLastImprovingIter = iIter;
                    // Do the first reheat
                    reheat(solution);
                } else if (iLastImprovingIter >= 0 && iIter > Math.max(iLastReheatIter, iLastImprovingIter) + iReheatLength) {
                    reheat(solution);
                } else if (iIter > iLastCoolingIter + iTemperatureLength) {
                    cool(solution);
                }
                setProgress(Math.round(100.0 * (iIter - Math.max(iLastReheatIter, iLastImprovingIter)) / iReheatLength));
            }
        }

        /**
         * Memorize the iteration when the last best solution was found.
         */
        @Override
        public void bestSaved(Solution<V, T> solution) {
            super.bestSaved(solution);
            if (iLastImprovingIter < 0 || Math.abs(iBestValue - solution.getBestValue()) / Math.max(Math.abs(iBestValue), Math.abs(solution.getBestValue())) >= 0.0001) {
                iLastImprovingIter = iIter;
                iLastBestIter = iIter;
                iBestValue = solution.getBestValue();
            }
        }
    }

}
