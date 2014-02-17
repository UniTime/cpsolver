package net.sf.cpsolver.ifs.algorithms;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.algorithms.neighbourhoods.RandomMove;
import net.sf.cpsolver.ifs.algorithms.neighbourhoods.RandomSwapMove;
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
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.ifs.util.ToolBox;

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
public class HillClimber<V extends Variable<V, T>, T extends Value<V, T>> implements NeighbourSelection<V, T>, SolutionListener<V, T>, LazyNeighbourAcceptanceCriterion<V, T> {
    private static Logger sLog = Logger.getLogger(HillClimber.class);
    private static DecimalFormat sDF2 = new DecimalFormat("0.00");
   
    private List<NeighbourSelector<V, T>> iNeighbours = null;
    private int iMaxIdleIters = 10000;
    private int iLastImprovingIter = 0;
    private double iBestValue = 0;
    private int iIter = 0;
    private Progress iProgress = null;
    private boolean iActive;
    private String iName = "Hill climbing";
    private boolean iRandomSelection = false;
    private boolean iUpdatePoints = false;
    private double iTotalBonus;
    private long iT0 = -1;

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
        this(properties, "Hill Climbing");
    }

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
    @SuppressWarnings("unchecked")
    public HillClimber(DataProperties properties, String name) {
        iMaxIdleIters = properties.getPropertyInt("HillClimber.MaxIdle", iMaxIdleIters);
        iRandomSelection = properties.getPropertyBoolean("HillClimber.Random", iRandomSelection);
        iUpdatePoints = properties.getPropertyBoolean("HillClimber.Update", iUpdatePoints);
        String neighbours = properties.getProperty("HillClimber.Neighbours", RandomMove.class.getName() + ";" + RandomSwapMove.class.getName() + "@0.01");
        neighbours += ";" + properties.getProperty("HillClimber.AdditionalNeighbours", "");
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
        iName = name;
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
        solver.currentSolution().addSolutionListener(this);
        for (NeighbourSelection<V, T> neighbour: iNeighbours)
            neighbour.init(solver);
        solver.setUpdateProgress(false);
        iProgress = Progress.getInstance(solver.currentSolution().getModel());
        iActive = false;
        iTotalBonus = 0;
        for (NeighbourSelector<V,T> s: iNeighbours) {
            s.init(solver);
            if (s.selection() instanceof HillClimberSelection)
                ((HillClimberSelection)s.selection()).setHcMode(true);
            iTotalBonus += s.getBonus();
        }
    }

    /**
     * Select one of the given neighbourhoods randomly, select neighbour, return
     * it if its value is below or equal to zero (continue with the next
     * selection otherwise). Return null when the given number of idle
     * iterations is reached.
     */
    @Override
    public Neighbour<V, T> selectNeighbour(Solution<V, T> solution) {
        if (iT0 < 0) iT0 = System.currentTimeMillis();
        if (!iActive) {
            iProgress.setPhase(iName + "...");
            iActive = true;
        }
        while (true) {
            iIter++;
            if (iIter % 1000 == 0) {
                sLog.info("Iter="+iIter/1000+"k, NonImpIter="+sDF2.format((iIter-iLastImprovingIter)/1000.0)+"k, Speed="+sDF2.format(1000.0*iIter/(System.currentTimeMillis()-iT0))+" it/s");
                if (iUpdatePoints)
                    for (NeighbourSelector<V,T> ns: iNeighbours)
                        sLog.info("  "+ns+" ("+sDF2.format(ns.getPoints())+" pts, "+sDF2.format(100.0*(iUpdatePoints?ns.getPoints():ns.getBonus())/totalPoints())+"%)");
            }
            iProgress.setProgress(Math.round(100.0 * (iIter - iLastImprovingIter) / iMaxIdleIters));
            if (iIter - iLastImprovingIter >= iMaxIdleIters)
                break;
            NeighbourSelector<V,T> ns = null;
            if (iRandomSelection) {
                ns = ToolBox.random(iNeighbours);
            } else {
                double points = (ToolBox.random() * totalPoints());
                for (Iterator<NeighbourSelector<V,T>> i = iNeighbours.iterator(); i.hasNext(); ) {
                    ns = i.next();
                    points -= (iUpdatePoints ? ns.getPoints() : ns.getBonus());
                    if (points <= 0) break;
                }
            }
            Neighbour<V, T> n = ns.selectNeighbour(solution);
            if (n != null) {
                if (n instanceof LazyNeighbour) {
                    ((LazyNeighbour<V,T>)n).setAcceptanceCriterion(this);
                    return n;
                } else if (n.value() <= 0.0) return n;
            }
        }
        iIter = 0;
        iLastImprovingIter = 0;
        iActive = false;
        return null;
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

    /** Accept lazy neighbour */
    @Override
    public boolean accept(LazyNeighbour<V, T> neighbour, double value) {
        return value <= 0.0;
    }
    
    /**
     * This interface may be implemented by a {@link NeighbourSelection} to indicate that it is employed by a hill climber.
     *
     */
    public static interface HillClimberSelection {
        /**
         * True if employed by a hill climber, e.g., worsening moves may be skipped.
         */
        public void setHcMode(boolean hcMode);
    }
}
