package net.sf.cpsolver.ifs.algorithms;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.cpsolver.ifs.algorithms.neighbourhoods.RandomMove;
import net.sf.cpsolver.ifs.algorithms.neighbourhoods.RandomSwapMove;
import net.sf.cpsolver.ifs.algorithms.neighbourhoods.SuggestionMove;
import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.LazyNeighbour;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.model.LazyNeighbour.LazyNeighbourAcceptanceCriterion;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solution.SolutionListener;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.JProf;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ToolBox;

import org.apache.log4j.Logger;

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
public class SimulatedAnnealing<V extends Variable<V, T>, T extends Value<V, T>> implements NeighbourSelection<V, T>, SolutionListener<V, T>, LazyNeighbourAcceptanceCriterion<V, T> {
    private static Logger sLog = Logger.getLogger(SimulatedAnnealing.class);
    private static DecimalFormat sDF2 = new DecimalFormat("0.00");
    private static DecimalFormat sDF5 = new DecimalFormat("0.00000");
    private static DecimalFormat sDF10 = new DecimalFormat("0.0000000000");
    private double iInitialTemperature = 1.5;
    private double iCoolingRate = 0.95;
    private double iReheatRate = -1;
    private long iTemperatureLength = 250000;
    private long iReheatLength = 0;
    private long iRestoreBestLength = 0;
    private double iTemperature = 0.0;
    private double iReheatLengthCoef = 5.0;
    private double iRestoreBestLengthCoef = -1;
    private long iIter = 0;
    private long iLastImprovingIter = 0;
    private long iLastReheatIter = 0;
    private long iLastCoolingIter = 0;
    private int iAcceptIter[] = new int[] { 0, 0, 0 };
    private boolean iStochasticHC = false;
    private int iMoves = 0;
    private double iAbsValue = 0;
    private long iT0 = -1;
    private double iBestValue = 0;
    private Progress iProgress = null;
    private boolean iActive;

    private List<NeighbourSelector<V, T>> iNeighbours = null;
    private boolean iRandomSelection = false;
    private boolean iUpdatePoints = false;
    private double iTotalBonus;

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
    @SuppressWarnings("unchecked")
    public SimulatedAnnealing(DataProperties properties) {
        iInitialTemperature = properties.getPropertyDouble("SimulatedAnnealing.InitialTemperature", iInitialTemperature);
        iReheatRate = properties.getPropertyDouble("SimulatedAnnealing.ReheatRate", iReheatRate);
        iCoolingRate = properties.getPropertyDouble("SimulatedAnnealing.CoolingRate", iCoolingRate);
        iRelativeAcceptance = properties.getPropertyBoolean("SimulatedAnnealing.RelativeAcceptance", iRelativeAcceptance);
        iStochasticHC = properties.getPropertyBoolean("SimulatedAnnealing.StochasticHC", iStochasticHC);
        iTemperatureLength = properties.getPropertyLong("SimulatedAnnealing.TemperatureLength", iTemperatureLength);
        iReheatLengthCoef = properties.getPropertyDouble("SimulatedAnnealing.ReheatLengthCoef", iReheatLengthCoef);
        iRestoreBestLengthCoef = properties.getPropertyDouble("SimulatedAnnealing.RestoreBestLengthCoef", iRestoreBestLengthCoef);
        if (iReheatRate < 0)
            iReheatRate = Math.pow(1 / iCoolingRate, iReheatLengthCoef * 1.7);
        if (iRestoreBestLengthCoef < 0)
            iRestoreBestLengthCoef = iReheatLengthCoef * iReheatLengthCoef;
        iRandomSelection = properties.getPropertyBoolean("SimulatedAnnealing.Random", iRandomSelection);
        iUpdatePoints = properties.getPropertyBoolean("SimulatedAnnealing.Update", iUpdatePoints);
        String neighbours = properties.getProperty("SimulatedAnnealing.Neighbours",
                RandomMove.class.getName() + ";" + RandomSwapMove.class.getName() + "@0.01;" + SuggestionMove.class.getName() + "@0.01");
        neighbours += ";" + properties.getProperty("SimulatedAnnealing.AdditionalNeighbours", "");
        iNeighbours = new ArrayList<NeighbourSelector<V,T>>();
        for (String neighbour: neighbours.split("\\;")) {
            if (neighbour == null || neighbour.isEmpty()) continue;
            try {
                double bonus = 1.0;
                if (neighbour.indexOf('@')>=0) {
                    bonus = Double.parseDouble(neighbour.substring(neighbour.indexOf('@') + 1));
                    neighbour = neighbour.substring(0, neighbour.indexOf('@'));
                }
                Class<NeighbourSelection<V, T>> clazz = (Class<NeighbourSelection<V, T>>)Class.forName(neighbour);
                NeighbourSelection<V, T> selection = clazz.getConstructor(DataProperties.class).newInstance(properties);
                addNeighbourSelection(selection, bonus);
            } catch (Exception e) {
                sLog.error("Unable to use " + neighbour + ": " + e.getMessage());
            }
        }
    }
    
    private void addNeighbourSelection(NeighbourSelection<V,T> ns, double bonus) {
        iNeighbours.add(new NeighbourSelector<V,T>(ns, bonus, iUpdatePoints));
    }
    
    private double totalPoints() {
        if (!iUpdatePoints) return iTotalBonus;
        double total = 0;
        for (NeighbourSelector<V,T> ns: iNeighbours)
            total += ns.getPoints();
        return total;
    }

    /**
     * Initialization
     */
    @Override
    public void init(Solver<V, T> solver) {
        iTemperature = iInitialTemperature;
        iReheatLength = Math.round(iReheatLengthCoef * iTemperatureLength);
        iRestoreBestLength = Math.round(iRestoreBestLengthCoef * iTemperatureLength);
        solver.currentSolution().addSolutionListener(this);
        for (NeighbourSelection<V, T> neighbour: iNeighbours)
            neighbour.init(solver);
        solver.setUpdateProgress(false);
        iProgress = Progress.getInstance(solver.currentSolution().getModel());
        iActive = false;
        iTotalBonus = 0;
        for (NeighbourSelector<V,T> s: iNeighbours) {
            s.init(solver);
            iTotalBonus += s.getBonus();
        }
    }

    /**
     * Cool temperature
     */
    protected void cool(Solution<V, T> solution) {
        iTemperature *= iCoolingRate;
        sLog.info("Iter=" + iIter / 1000 + "k, NonImpIter=" + sDF2.format((iIter - iLastImprovingIter) / 1000.0)
                + "k, Speed=" + sDF2.format(1000.0 * iIter / (JProf.currentTimeMillis() - iT0)) + " it/s");
        sLog.info("Temperature decreased to " + sDF5.format(iTemperature) + " " + "(#moves=" + iMoves + ", rms(value)="
                + sDF2.format(Math.sqrt(iAbsValue / iMoves)) + ", " + "accept=-"
                + sDF2.format(100.0 * iAcceptIter[0] / iTemperatureLength) + "/"
                + sDF2.format(100.0 * iAcceptIter[1] / iTemperatureLength) + "/+"
                + sDF2.format(100.0 * iAcceptIter[2] / iTemperatureLength) + "%, "
                + (prob(-1) < 1.0 ? "p(-1)=" + sDF2.format(100.0 * prob(-1)) + "%, " : "") + "p(+1)="
                + sDF2.format(100.0 * prob(1)) + "%, " + "p(+10)=" + sDF5.format(100.0 * prob(10)) + "%)");
        if (iUpdatePoints)
            for (NeighbourSelector<V,T> ns: iNeighbours)
                sLog.info("  "+ns+" ("+sDF2.format(ns.getPoints())+" pts, "+sDF2.format(100.0*(iUpdatePoints?ns.getPoints():ns.getBonus())/totalPoints())+"%)");
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
        sLog.info("Iter=" + iIter / 1000 + "k, NonImpIter=" + sDF2.format((iIter - iLastImprovingIter) / 1000.0)
                + "k, Speed=" + sDF2.format(1000.0 * iIter / (JProf.currentTimeMillis() - iT0)) + " it/s");
        sLog.info("Temperature increased to " + sDF5.format(iTemperature) + " "
                + (prob(-1) < 1.0 ? "p(-1)=" + sDF2.format(100.0 * prob(-1)) + "%, " : "") + "p(+1)="
                + sDF2.format(100.0 * prob(1)) + "%, " + "p(+10)=" + sDF5.format(100.0 * prob(10)) + "%, " + "p(+100)="
                + sDF10.format(100.0 * prob(100)) + "%)");
        if (iUpdatePoints)
            for (NeighbourSelector<V,T> ns: iNeighbours)
                sLog.info("  "+ns+" ("+sDF2.format(ns.getPoints())+" pts, "+sDF2.format(100.0*(iUpdatePoints?ns.getPoints():ns.getBonus())/totalPoints())+"%)");
        iLastReheatIter = iIter;
        iProgress.setPhase("Simulated Annealing [" + sDF2.format(iTemperature) + "]...");
    }

    /**
     * restore best ever found solution
     */
    protected void restoreBest(Solution<V, T> solution) {
        sLog.info("Best restored");
        iLastImprovingIter = iIter;
    }

    /**
     * Generate neighbour -- select neighbourhood randomly, select neighbour
     */
    public Neighbour<V, T> genMove(Solution<V, T> solution) {
        while (true) {
            incIter(solution);
            NeighbourSelector<V,T> ns = null;
            if (iRandomSelection) {
                ns = ToolBox.random(iNeighbours);
            } else {
                double points = (ToolBox.random()*totalPoints());
                for (Iterator<NeighbourSelector<V,T>> i = iNeighbours.iterator(); i.hasNext(); ) {
                    ns = i.next();
                    points -= (iUpdatePoints?ns.getPoints():ns.getBonus());
                    if (points<=0) break;
                }
            }
            Neighbour<V, T> n = ns.selectNeighbour(solution);
            if (n != null)
                return n;
        }
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
     * True if the given neighboir is to be be accepted
     * 
     * @param solution
     *            current solution
     * @param neighbour
     *            proposed move
     * @return true if generated random number is below
     *         {@link SimulatedAnnealing#prob(double)}
     */
    protected boolean accept(Solution<V, T> solution, Neighbour<V, T> neighbour) {
        if (neighbour instanceof LazyNeighbour) {
            ((LazyNeighbour<V, T>)neighbour).setAcceptanceCriterion(this);
            return true;
        }
        double value = (iRelativeAcceptance ? neighbour.value() : solution.getModel().getTotalValue() + neighbour.value() - solution.getBestValue());
        double prob = prob(value);
        if (prob >= 1.0 || ToolBox.random() < prob) {
            iAcceptIter[neighbour.value() < 0.0 ? 0 : neighbour.value() > 0.0 ? 2 : 1]++;
            return true;
        }
        return false;
    }
    
    /** Accept lazy neighbour */
    @Override
    public boolean accept(LazyNeighbour<V, T> neighbour, double value) {
        double prob = prob(value);
        if (prob >= 1.0 || ToolBox.random() < prob) {
            iAcceptIter[value < 0.0 ? 0 : value > 0.0 ? 2 : 1]++;
            return true;
        }
        return false;
    }

    /**
     * Increment iteration counter, cool/reheat/restoreBest if necessary
     */
    protected void incIter(Solution<V, T> solution) {
        if (iT0 < 0)
            iT0 = JProf.currentTimeMillis();
        iIter++;
        if (iIter > iLastImprovingIter + iRestoreBestLength)
            restoreBest(solution);
        if (iIter > Math.max(iLastReheatIter, iLastImprovingIter) + iReheatLength)
            reheat(solution);
        if (iIter > iLastCoolingIter + iTemperatureLength)
            cool(solution);
        iProgress.setProgress(Math.round(100.0 * (iIter - Math.max(iLastReheatIter, iLastImprovingIter))
                / iReheatLength));
    }

    /**
     * Select neighbour -- generate a move
     * {@link SimulatedAnnealing#genMove(Solution)} until an acceptable
     * neighbour is found
     * {@link SimulatedAnnealing#accept(Solution, Neighbour)}, keep
     * increasing iteration {@link SimulatedAnnealing#incIter(Solution)}.
     */
    @Override
    public Neighbour<V, T> selectNeighbour(Solution<V, T> solution) {
        if (!iActive) {
            iProgress.setPhase("Simulated Annealing [" + sDF2.format(iTemperature) + "]...");
            iActive = true;
        }
        Neighbour<V, T> neighbour = null;
        while ((neighbour = genMove(solution)) != null) {
            iMoves++;
            iAbsValue += neighbour.value() * neighbour.value();
            if (accept(solution, neighbour))
                break;
        }
        if (neighbour == null)
            iActive = false;
        return (neighbour == null ? null : neighbour);
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
    public void bestRestored(Solution<V, T> solution) {
    }
}
