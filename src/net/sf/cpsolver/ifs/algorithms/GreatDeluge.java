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
import net.sf.cpsolver.ifs.model.LazyNeighbour.LazyNeighbourAcceptanceCriterion;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solution.SolutionListener;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.JProf;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ToolBox;

import org.apache.log4j.Logger;

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
public class GreatDeluge<V extends Variable<V, T>, T extends Value<V, T>> implements NeighbourSelection<V, T>, SolutionListener<V, T>, LazyNeighbourAcceptanceCriterion<V, T> {
    private static Logger sLog = Logger.getLogger(GreatDeluge.class);
    private static DecimalFormat sDF2 = new DecimalFormat("0.00");
    private static DecimalFormat sDF5 = new DecimalFormat("0.00000");
    private double iBound = 0.0;
    private double iCoolRate = 0.9999995;
    private long iIter;
    private double iUpperBound;
    private double iUpperBoundRate = 1.05;
    private double iLowerBoundRate = 0.95;
    private int iMoves = 0;
    private int iAcceptedMoves = 0;
    private int iNrIdle = 0;
    private long iT0 = -1;
    private long iLastImprovingIter = 0;
    private double iBestValue = 0;
    private Progress iProgress = null;

    private List<NeighbourSelector<V, T>> iNeighbours = null;
    private boolean iRandomSelection = false;
    private boolean iUpdatePoints = false;
    private double iTotalBonus;

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
    @SuppressWarnings("unchecked")
    public GreatDeluge(DataProperties properties) {
        iCoolRate = properties.getPropertyDouble("GreatDeluge.CoolRate", iCoolRate);
        iUpperBoundRate = properties.getPropertyDouble("GreatDeluge.UpperBoundRate", iUpperBoundRate);
        iLowerBoundRate = properties.getPropertyDouble("GreatDeluge.LowerBoundRate", iLowerBoundRate);
        iRandomSelection = properties.getPropertyBoolean("GreatDeluge.Random", iRandomSelection);
        iUpdatePoints = properties.getPropertyBoolean("GreatDeluge.Update", iUpdatePoints);
        String neighbours = properties.getProperty("GreatDeluge.Neighbours",
                RandomMove.class.getName() + ";" + RandomSwapMove.class.getName() + "@0.01;" + SuggestionMove.class.getName() + "@0.01");
        neighbours += ";" + properties.getProperty("GreatDeluge.AdditionalNeighbours", "");
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
    
    /** Initialization */
    @Override
    public void init(Solver<V, T> solver) {
        iIter = -1;
        solver.currentSolution().addSolutionListener(this);
        for (NeighbourSelection<V, T> neighbour: iNeighbours)
            neighbour.init(solver);
        solver.setUpdateProgress(false);
        iProgress = Progress.getInstance(solver.currentSolution().getModel());
        iTotalBonus = 0;
        for (NeighbourSelector<V,T> s: iNeighbours) {
            s.init(solver);
            iTotalBonus += s.getBonus();
        }
    }

    /** Print some information */
    protected void info(Solution<V, T> solution) {
        sLog.info("Iter=" + iIter / 1000 + "k, NonImpIter=" + sDF2.format((iIter - iLastImprovingIter) / 1000.0)
                + "k, Speed=" + sDF2.format(1000.0 * iIter / (JProf.currentTimeMillis() - iT0)) + " it/s");
        sLog.info("Bound is " + sDF2.format(iBound) + ", " + "best value is " + sDF2.format(solution.getBestValue())
                + " (" + sDF2.format(100.0 * iBound / solution.getBestValue()) + "%), " + "current value is "
                + sDF2.format(solution.getModel().getTotalValue()) + " ("
                + sDF2.format(100.0 * iBound / solution.getModel().getTotalValue()) + "%), " + "#idle=" + iNrIdle
                + ", " + "Pacc=" + sDF5.format(100.0 * iAcceptedMoves / iMoves) + "%");
        if (iUpdatePoints)
            for (NeighbourSelector<V,T> ns: iNeighbours)
                sLog.info("  "+ns+" ("+sDF2.format(ns.getPoints())+" pts, "+sDF2.format(100.0*(iUpdatePoints?ns.getPoints():ns.getBonus())/totalPoints())+"%)");
        iAcceptedMoves = iMoves = 0;
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

    /** Accept neighbour */
    protected boolean accept(Solution<V, T> solution, Neighbour<V, T> neighbour) {
        if (neighbour instanceof LazyNeighbour) {
            ((LazyNeighbour<V, T>)neighbour).setAcceptanceCriterion(this);
            return true;
        }
        return (neighbour.value() <= 0 || solution.getModel().getTotalValue() + neighbour.value() <= iBound);
    }
    
    /** Accept lazy neighbour */
    @Override
    public boolean accept(LazyNeighbour<V, T> neighbour, double value) {
        return (value <= 0.0 || neighbour.getModel().getTotalValue() <= iBound);
    }

    /** Increment iteration count, update bound */
    protected void incIter(Solution<V, T> solution) {
        if (iIter < 0) {
            iIter = 0;
            iLastImprovingIter = 0;
            iT0 = JProf.currentTimeMillis();
            iBound = (solution.getBestValue() > 0.0 ? iUpperBoundRate * solution.getBestValue() : solution.getBestValue() / iUpperBoundRate);
            iUpperBound = iBound;
            iNrIdle = 0;
            iProgress.setPhase("Great deluge [" + (1 + iNrIdle) + "]...");
        } else {
            iIter++;
            if (solution.getBestValue() >= 0.0)
                iBound *= iCoolRate;
            else
                iBound /= iCoolRate;
        }
        if (iIter % 10000 == 0) {
            info(solution);
        }
        double lowerBound = (solution.getBestValue() >= 0.0 ? Math.pow(iLowerBoundRate, 1 + iNrIdle)
                * solution.getBestValue() : solution.getBestValue() / Math.pow(iLowerBoundRate, 1 + iNrIdle));
        if (iBound < lowerBound) {
            iNrIdle++;
            sLog.info(" -<[" + iNrIdle + "]>- ");
            iBound = Math.max(solution.getBestValue() + 2.0,(solution.getBestValue() >= 0.0 ?
                    Math.pow(iUpperBoundRate, iNrIdle) * solution.getBestValue() :
                    solution.getBestValue() / Math.pow(iUpperBoundRate, iNrIdle)));
            iUpperBound = iBound;
            iProgress.setPhase("Great deluge [" + (1 + iNrIdle) + "]...");
        }
        iProgress.setProgress(100 - Math.round(100.0 * (iBound - lowerBound) / (iUpperBound - lowerBound)));
    }

    /**
     * A neighbour is generated randomly untill an acceptable one is found.
     */
    @Override
    public Neighbour<V, T> selectNeighbour(Solution<V, T> solution) {
        Neighbour<V, T> neighbour = null;
        while ((neighbour = genMove(solution)) != null) {
            iMoves++;
            if (accept(solution, neighbour)) {
                iAcceptedMoves++;
                break;
            }
        }
        return (neighbour == null ? null : neighbour);
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
