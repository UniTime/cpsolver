package org.cpsolver.ifs.algorithms;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.cpsolver.ifs.algorithms.neighbourhoods.HillClimberSelection;
import org.cpsolver.ifs.algorithms.neighbourhoods.RandomMove;
import org.cpsolver.ifs.algorithms.neighbourhoods.RandomSwapMove;
import org.cpsolver.ifs.algorithms.neighbourhoods.SuggestionMove;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentContext;
import org.cpsolver.ifs.assignment.context.NeighbourSelectionWithContext;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.LazyNeighbour;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.model.LazyNeighbour.LazyNeighbourAcceptanceCriterion;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solution.SolutionListener;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.JProf;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.ifs.util.ToolBox;


/**
 * Base class for the search techniques like hill climber, great deluge, or simulated annealing.
 * It implements the {@link SolutionListener} and the variable neighbourhood selection.
 * 
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
 **/
public abstract class NeighbourSearch<V extends Variable<V, T>, T extends Value<V, T>> extends NeighbourSelectionWithContext<V, T, NeighbourSearch<V, T>.NeighbourSearchContext> implements LazyNeighbourAcceptanceCriterion<V, T>, SolutionListener<V, T> {
    private Logger iLog;
    protected DecimalFormat iDF2 = new DecimalFormat("0.00");
    
    private Progress iProgress = null;
    protected String iPhase = null;

    private List<NeighbourSelector<V, T>> iNeighbours = null;
    private boolean iRandomSelection = false;
    private boolean iUpdatePoints = false;
    private double iTotalBonus;
    private Solver<V, T> iSolver = null;

    @SuppressWarnings("unchecked")
    public NeighbourSearch(DataProperties properties) {
        iPhase = getClass().getSimpleName().replaceAll("(?<=[^A-Z])([A-Z])"," $1");
        iLog = org.apache.logging.log4j.LogManager.getLogger(getClass());
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
     * Prints a message into the log
     * @param message a message to log
     */
    protected void info(String message) {
        iProgress.debug("[" + Thread.currentThread().getName() + "] " + iPhase + "> " + message);
    }
    
    /**
     * Set search progress
     * @param progress between 0 and 100
     */
    protected void setProgress(long progress) {
        // iProgress.setProgress(progress);
    }
    
    /**
     * Set search progress phase
     * @param phase a progress phase to set
     */
    protected void setProgressPhase(String phase) {
        iProgress.info("[" + Thread.currentThread().getName() + "] " + phase);
        // iProgress.setPhase(phase);
    }
    
    /**
     * Add neighbour selection
     * @param ns a selection
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
     * @param hcMode true if the search is a hill climber (worsening moves are always rejected)
     */
    protected void setHCMode(boolean hcMode) {
        for (NeighbourSelector<V,T> s: iNeighbours) {
            if (s.selection() instanceof HillClimberSelection)
                ((HillClimberSelection)s.selection()).setHcMode(hcMode);
        }
    }
    
    /**
     * Return list of neighbour selections
     * @return list of neighbour selections
     */
    protected List<? extends NeighbourSelection<V, T>> getNeighbours() {
        return iNeighbours;
    }

    @Override
    public void init(Solver<V, T> solver) {
        super.init(solver);
        iProgress = Progress.getInstance(solver.currentSolution().getModel());
        iSolver = solver;
        solver.currentSolution().addSolutionListener(this);
        // solver.setUpdateProgress(false);
        for (NeighbourSelection<V, T> neighbour: iNeighbours)
            neighbour.init(solver);
        iTotalBonus = 0;
        for (NeighbourSelector<V,T> s: iNeighbours) {
            s.init(solver);
            iTotalBonus += s.getBonus();
        }
    }
    
    /**
     * Generate and return next neighbour selection
     * @return next neighbour selection to use
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
     * @param solution current solution
     * @return generated neighbour
     */
    public Neighbour<V, T> generateMove(Solution<V, T> solution) {
        return nextNeighbourSelection().selectNeighbour(solution);
    }

    @Override
    public Neighbour<V, T> selectNeighbour(Solution<V, T> solution) {
        NeighbourSearchContext context = getContext(solution.getAssignment());
        context.activateIfNeeded(solution);
        while (context.canContinue(solution)) {
            if (iSolver != null && iSolver.isStop()) return null;
            context.incIteration(solution);
            Neighbour<V,T> n = generateMove(solution);
            if (n != null && accept(context, solution, n))
                return n;
        }
        context.deactivateIfNeeded(solution);
        return null;
    }
    
    /**
     * True if the generated move is to be accepted.
     * @param context search context
     * @param solution current solution
     * @param neighbour a generated move
     * @return true if the generated move should be assigned
     */
    protected boolean accept(NeighbourSearchContext context, Solution<V, T> solution, Neighbour<V, T> neighbour) {
        if (neighbour instanceof LazyNeighbour) {
            ((LazyNeighbour<V, T>)neighbour).setAcceptanceCriterion(this);
            return true;
        }
        return context.accept(solution.getAssignment(), solution.getModel(), neighbour, neighbour.value(solution.getAssignment()), false);
    }
    
    /** Accept lazy neighbour -- calling the acceptance criterion with lazy = true. */
    @Override
    public boolean accept(Assignment<V, T> assignment, LazyNeighbour<V, T> neighbour, double value) {
        return getContext(assignment).accept(assignment, neighbour.getModel(), neighbour, value, true);
    }

    /**
     * Parameter base name. This can be used to distinguish between parameters of different search algorithms.
     * @return solver parameter base name for this search technique
     */
    public abstract String getParameterBaseName();
    
    @Override
    public void bestSaved(Solution<V, T> solution) {
        getContext(solution.getAssignment()).bestSaved(solution);
    }

    @Override
    public void solutionUpdated(Solution<V, T> solution) {
        getContext(solution.getAssignment()).solutionUpdated(solution);
    }

    @Override
    public void getInfo(Solution<V, T> solution, Map<String, String> info) {
        getContext(solution.getAssignment()).getInfo(solution, info);
    }

    @Override
    public void getInfo(Solution<V, T> solution, Map<String, String> info, Collection<V> variables) {
        getContext(solution.getAssignment()).getInfo(solution, info, variables);
    }

    @Override
    public void bestCleared(Solution<V, T> solution) {
        getContext(solution.getAssignment()).bestCleared(solution);
    }

    @Override
    public void bestRestored(Solution<V, T> solution) {
        getContext(solution.getAssignment()).bestRestored(solution);
    }
    
    /**
     * In single solution multiple threads environments return true if the given solution is of the first thread
     * @param solution current solution
     * @return if the current thread is master (can alter bound etc.)
     */
    public boolean isMaster(Solution<V, T> solution) {
        return !hasContextOverride() || solution.getAssignment().getIndex() <= 1;
    }
    
    /**
     * Search context
     */
    public abstract class NeighbourSearchContext implements AssignmentContext, SolutionListener<V, T> {
        protected long iT0 = -1;
        protected int iIter = 0;

        /** Called just before the neighbourhood search is called for the first time. 
         * @param solution current solution
         **/
        protected void activate(Solution<V, T> solution) {
            iT0 = JProf.currentTimeMillis();
            iIter = 0;
            setProgressPhase(iPhase + "...");
        }
        
        private synchronized void activateIfNeeded(Solution<V, T> solution) {
            if (iT0 < 0) activate(solution);
        }
        
        /** Called when the search cannot continue, just before a null neighbour is returned 
         * @param solution current solution
         **/
        protected void deactivate(Solution<V, T> solution) {
            iT0 = -1;
        }
        
        private synchronized void deactivateIfNeeded(Solution<V, T> solution) {
            if (isMaster(solution)) deactivate(solution);
        }
        
        /**
         * Increment iteration counters etc.
         * @param solution current solution
         */
        protected void incIteration(Solution<V, T> solution) {
            iIter++;
        }

        /**
         * Running time in milliseconds (since the last call of activate)
         * @return running time
         */
        protected long getTimeMillis() { return JProf.currentTimeMillis() - iT0; }

        /**
         * Return false if the search is to be stopped. Null neighbour is returned when this method returns false.
         * @param solution current solution
         * @return true if can continue
         */
        protected boolean canContinue(Solution<V, T> solution) {
            return true;
        }
        
        /** Acceptance criterion. If lazy, current assignment already contains the given neighbour.  
         * @param assignment current assignment
         * @param model problem model
         * @param neighbour a generated move
         * @param value value of the generated move (i.e., its impact on the solution value)
         * @param lazy true if lazy move
         * @return true if the generated move is to be assigned 
         **/
        protected abstract boolean accept(Assignment<V, T> assignment, Model<V, T> model, Neighbour<V, T> neighbour, double value, boolean lazy);
        
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
}
