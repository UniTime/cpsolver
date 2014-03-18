package net.sf.cpsolver.ifs.algorithms;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.algorithms.neighbourhoods.HillClimberSelection;
import net.sf.cpsolver.ifs.algorithms.neighbourhoods.RandomMove;
import net.sf.cpsolver.ifs.algorithms.neighbourhoods.RandomSwapMove;
import net.sf.cpsolver.ifs.algorithms.neighbourhoods.SuggestionMove;
import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.LazyNeighbour;
import net.sf.cpsolver.ifs.model.Model;
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

/**
 * Base class for the search techniques like hill climber, great deluge, or simulated annealing.
 * It implements the {@link SolutionListener} and the variable neighbourhood selection.
 * 
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
 **/
public abstract class NeighbourSearch<V extends Variable<V, T>, T extends Value<V, T>> implements NeighbourSelection<V, T>, SolutionListener<V, T>, LazyNeighbourAcceptanceCriterion<V, T> {
    protected Logger iLog;
    protected DecimalFormat iDF2 = new DecimalFormat("0.00");
    
    protected long iT0 = -1;
    protected int iIter = 0;
    protected String iPhase = null;
    protected Progress iProgress = null;
    
    private List<NeighbourSelector<V, T>> iNeighbours = null;
    private boolean iRandomSelection = false;
    private boolean iUpdatePoints = false;
    private double iTotalBonus;

    @SuppressWarnings("unchecked")
    public NeighbourSearch(DataProperties properties) {
        iPhase = getClass().getSimpleName().replaceAll("(?<=[^A-Z])([A-Z])"," $1");
        iLog = Logger.getLogger(getClass());
        iRandomSelection = properties.getPropertyBoolean(getParameterBaseName() + ".Random", iRandomSelection);
        iUpdatePoints = properties.getPropertyBoolean(getParameterBaseName() + ".Update", iUpdatePoints);
        String neighbours = properties.getProperty(getParameterBaseName() + ".Neighbours",
                RandomMove.class.getName() + ";" + RandomSwapMove.class.getName() + "@0.01;" + SuggestionMove.class.getName() + "@0.01");
        neighbours += ";" + properties.getProperty(getParameterBaseName() + ".AdditionalNeighbours", "");
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
                iLog.error("Unable to use " + neighbour + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Add neighbour selection
     * @param bonus execution bonus (more bonus means more executions of this neighbour selection, see {@link NeighbourSelector})
     */
    protected void addNeighbourSelection(NeighbourSelection<V,T> ns, double bonus) {
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
     * Set HC mode for all the neighbour selections that support the {@link HillClimberSelection} interface. 
     */
    protected void setHCMode(boolean hcMode) {
        for (NeighbourSelector<V,T> s: iNeighbours) {
            if (s.selection() instanceof HillClimberSelection)
                ((HillClimberSelection)s.selection()).setHcMode(hcMode);
        }
    }
    
    /**
     * Return list of neighbour selections
     * @return
     */
    protected List<? extends NeighbourSelection<V, T>> getNeighbours() {
        return iNeighbours;
    }

    @Override
    public void init(Solver<V, T> solver) {
        iProgress = Progress.getInstance(solver.currentSolution().getModel());
        iT0 = -1;
        solver.currentSolution().addSolutionListener(this);
        solver.setUpdateProgress(false);
        for (NeighbourSelection<V, T> neighbour: iNeighbours)
            neighbour.init(solver);
        solver.setUpdateProgress(false);
        iTotalBonus = 0;
        for (NeighbourSelector<V,T> s: iNeighbours) {
            s.init(solver);
            iTotalBonus += s.getBonus();
        }
    }
    
    /**
     * Generate and return next neighbour selection
     */
    protected NeighbourSelection<V,T> nextNeighbourSelection() {
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
        return ns;
    }
    
    /**
     * Log some information about neigbour selections once in a while
     */
    protected void logNeibourStatus() {
        if (iUpdatePoints)
            for (NeighbourSelector<V,T> ns: iNeighbours)
                iLog.info("  "+ns+" ("+iDF2.format(ns.getPoints())+" pts, "+iDF2.format(100.0*(iUpdatePoints?ns.getPoints():ns.getBonus())/totalPoints())+"%)");
    }

    /**
     * Generate a random move
     */
    public Neighbour<V, T> generateMove(Solution<V, T> solution) {
        return nextNeighbourSelection().selectNeighbour(solution);
    }

    @Override
    public Neighbour<V, T> selectNeighbour(Solution<V, T> solution) {
        if (iT0 < 0) activate(solution);
        while (canContinue(solution)) {
            incIteration(solution);
            Neighbour<V,T> n = generateMove(solution);
            if (n != null && accept(solution, n))
                return n;
        }
        deactivate(solution);
        return null;
    }
    
    /**
     * Return false if the search is to be stopped. Null neighbour is returned when this method returns false.
     */
    protected boolean canContinue(Solution<V, T> solution) {
        return true;
    }
    
    /**
     * Increment iteration counters etc.
     */
    protected void incIteration(Solution<V, T> solution) {
        iIter++;
    }

    /**
     * Running time in milliseconds (since the last call of activate)
     */
    protected long getTimeMillis() { return JProf.currentTimeMillis() - iT0; }
    
    /**
     * True if the generated move is to be accepted.
     */
    protected boolean accept(Solution<V, T> solution, Neighbour<V, T> neighbour) {
        if (neighbour instanceof LazyNeighbour) {
            ((LazyNeighbour<V, T>)neighbour).setAcceptanceCriterion(this);
            return true;
        }
        return accept(solution.getModel(), neighbour, neighbour.value(), false);
    }
    
    /** Accept lazy neighbour -- calling the acceptance criterion with lazy = true. */
    @Override
    public boolean accept(LazyNeighbour<V, T> neighbour, double value) {
        return accept(neighbour.getModel(), neighbour, value, true);
    }

    /** Acceptance criterion. If lazy, current assignment already contains the given neighbour.  */
    protected abstract boolean accept(Model<V, T> model, Neighbour<V, T> neighbour, double value, boolean lazy);
    
    /** Called just before the neighbourhood search is called for the first time. */
    protected void activate(Solution<V, T> solution) {
        iT0 = JProf.currentTimeMillis();
        iIter = 0;
        iProgress.setPhase(iPhase + "...");
    }
    
    /** Called when the search cannot continue, just before a null neighbour is returned */
    protected void deactivate(Solution<V, T> solution) {
        iT0 = -1;
    }

    /**
     * Parameter base name. This can be used to distinguish between parameters of different search algorithms.
     */
    public abstract String getParameterBaseName();
    
    @Override
    public void bestSaved(Solution<V, T> solution) {
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
