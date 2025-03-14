package org.cpsolver.ifs.algorithms;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentContext;
import org.cpsolver.ifs.assignment.context.NeighbourSelectionWithContext;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.heuristics.StandardNeighbourSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.Progress;

/**
 * Meta-heuristic search neighbor selection. <br>
 * <br>
 * It consists of the following (up to four) phases:
 * <ul>
 * <li>Construction phase until the construction neighbor selection is not exhausted (i.e., null is returned as the next neighbor),
 * <li>Incomplete phase (e.g., {@link StandardNeighbourSelection}) until all variables are assigned (this heuristics is also used whenever the solution becomes incomplete, until it is complete again),
 * <li>Hill-climbing phase (e.g., {@link HillClimber}) until the given number if idle iterations (this phase is optional)
 * <li>Improvement phase (e.g., {@link GreatDeluge} or {@link SimulatedAnnealing}) until timeout is reached
 * </ul>
 * The search is based on the {@link SimpleSearch}, however each phase can be customized by providing the appropriate neighbor selection.
 * Also, a different selection can be used in each thread.
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
public class MetaHeuristicSearch<V extends Variable<V, T>, T extends Value<V, T>> extends NeighbourSelectionWithContext<V,T,MetaHeuristicSearch<V,T>.MetaHeuristicSearchContext> {
    private Logger iLog = org.apache.logging.log4j.LogManager.getLogger(SimpleSearch.class);
    private List<NeighbourSelection<V, T>> iConstructionPhase = new ArrayList<NeighbourSelection<V,T>>();
    private List<NeighbourSelection<V, T>> iIncompletePhase = new ArrayList<NeighbourSelection<V,T>>();
    private List<NeighbourSelection<V, T>> iHillClimberPhase = new ArrayList<NeighbourSelection<V,T>>();
    private List<NeighbourSelection<V, T>> iImprovementPhase = new ArrayList<NeighbourSelection<V,T>>();
    private Progress iProgress = null;

    /**
     * Constructor
     * <ul>
     * <li>MetaHeuristic.ConstructionClass ... construction heuristics (if needed)
     * <li>MetaHeuristic.IncompleteClass ... incomplete heuristics (e.g., {@link StandardNeighbourSelection})
     * <li>MetaHeuristic.HillClimberClass ... hill-climber heuristics (e.g., {@link HillClimber} or {@link StepCountingHillClimber})
     * <li>MetaHeuristic.ImprovementClass ... improvement heuristics (e.g., {@link SimulatedAnnealing} or {@link GreatDeluge})
     * </ul>
     * @param properties problem configuration
     */
    public MetaHeuristicSearch(DataProperties properties) {
        String constructions = properties.getProperty("MetaHeuristic.ConstructionClass"); 
        if (constructions != null) {
            for (String construction: constructions.split(",")) {
                try {
                    boolean pc = false;
                    if (construction.endsWith("@PC")) {
                        pc = true;
                        construction = construction.substring(0, construction.length() - 3);
                    }
                    if (construction.isEmpty() || "null".equalsIgnoreCase(construction)) {
                        iConstructionPhase.add(null);
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    Class<NeighbourSelection<V, T>> constructionClass = (Class<NeighbourSelection<V, T>>)Class.forName(construction);
                    NeighbourSelection<V, T> constructionSelection = constructionClass.getConstructor(DataProperties.class).newInstance(properties);
                    if (pc) {
                        constructionSelection = new ParallelConstruction<V, T>(properties, constructionSelection);
                    }
                    iConstructionPhase.add(constructionSelection);
                } catch (Exception e) {
                    iLog.error("Unable to use " + construction + ": " + e.getMessage());
                }
            }
        }
        String incompletes = properties.getProperty("MetaHeuristic.IncompleteClass"); 
        if (incompletes != null) {
            for (String incomplete: incompletes.split(",")) {
                try {
                    boolean pc = false;
                    if (incomplete.endsWith("@PC")) {
                        pc = true;
                        incomplete = incomplete.substring(0, incomplete.length() - 3);
                    }
                    if (incomplete.isEmpty() || "null".equalsIgnoreCase(incomplete)) {
                        iIncompletePhase.add(null);
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    Class<NeighbourSelection<V, T>> incompleteClass = (Class<NeighbourSelection<V, T>>)Class.forName(incomplete);
                    NeighbourSelection<V, T> incompleteSelection = incompleteClass.getConstructor(DataProperties.class).newInstance(properties);
                    if (pc) {
                        incompleteSelection = new ParallelConstruction<V, T>(properties, incompleteSelection);
                    }
                    iIncompletePhase.add(incompleteSelection);
                } catch (Exception e) {
                    iLog.error("Unable to use " + incomplete + ": " + e.getMessage());
                }
            }
        }
        String hillClimbers = properties.getProperty("MetaHeuristic.HillClimberClass"); 
        if (hillClimbers != null) {
            for (String hillClimber: hillClimbers.split(",")) {
                try {
                    if (hillClimber.isEmpty() || "null".equalsIgnoreCase(hillClimber)) {
                        iHillClimberPhase.add(null);
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    Class<NeighbourSelection<V, T>> hillClimberClass = (Class<NeighbourSelection<V, T>>)Class.forName(hillClimber);
                    NeighbourSelection<V, T> hillClimberSelection = hillClimberClass.getConstructor(DataProperties.class).newInstance(properties);
                    iHillClimberPhase.add(hillClimberSelection);
                } catch (Exception e) {
                    iLog.error("Unable to use " + hillClimber + ": " + e.getMessage());
                }
            }
        }
        String improvements = properties.getProperty("MetaHeuristic.ImprovementClass"); 
        if (improvements != null) {
            for (String improvement: improvements.split(",")) {
                try {
                    if (improvement.isEmpty() || "null".equalsIgnoreCase(improvement)) {
                        iImprovementPhase.add(null);
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    Class<NeighbourSelection<V, T>> improvementClass = (Class<NeighbourSelection<V, T>>)Class.forName(improvement);
                    NeighbourSelection<V, T> improvementSelection = improvementClass.getConstructor(DataProperties.class).newInstance(properties);
                    iImprovementPhase.add(improvementSelection);
                } catch (Exception e) {
                    iLog.error("Unable to use " + improvement + ": " + e.getMessage());
                }
            }
        }
    }
    
    private String getName(NeighbourSelection<V, T> selection) {
        return selection.getClass().getSimpleName().replaceAll("(?<=[^A-Z])([A-Z])"," $1");
    }

    /**
     * Initialization
     */
    @Override
    public void init(Solver<V, T> solver) {
        super.init(solver);
        for (NeighbourSelection<V, T> ns: iConstructionPhase)
            if (ns != null) ns.init(solver);
        for (NeighbourSelection<V, T> ns: iIncompletePhase)
            if (ns != null) ns.init(solver);
        for (NeighbourSelection<V, T> ns: iHillClimberPhase)
            if (ns != null) ns.init(solver);
        for (NeighbourSelection<V, T> ns: iImprovementPhase)
            if (ns != null) ns.init(solver);
        iProgress = Progress.getInstance(solver.currentSolution().getModel());
    }

    /**
     * Neighbour selection. It consists of the following phases:
     * <ul>
     * <li>Construction phase until the construction neighbor selection is not exhausted (i.e., null is returned as the next neighbor),
     * <li>Incomplete phase (e.g., {@link StandardNeighbourSelection}) until all variables are assigned (this heuristics is also used whenever the solution becomes incomplete, until it is complete again),
     * <li>Hill-climbing phase (e.g., {@link HillClimber}) until the given number if idle iterations (this phase is optional)
     * <li>Improvement phase (e.g., {@link GreatDeluge} or {@link SimulatedAnnealing}) until timeout is reached
     * </ul>
     */
    @SuppressWarnings("fallthrough")
    @Override
    public Neighbour<V, T> selectNeighbour(Solution<V, T> solution) {
        MetaHeuristicSearchContext context = getContext(solution.getAssignment());
        Neighbour<V, T> n = null;
        switch (context.getPhase()) {
            case -1:
                context.setPhase(0);
                iProgress.info("[" + Thread.currentThread().getName() + "] " + getName(context.getConstructionSelection()) + "...");
            case 0:
                if (solution.getModel().nrUnassignedVariables(solution.getAssignment()) > 0) {
                    n = context.getConstructionSelection().selectNeighbour(solution);
                    if (n != null)
                        return n;
                }
                context.setPhase(1);
                if (solution.getModel().nrUnassignedVariables(solution.getAssignment()) > 0)
                    iProgress.info("[" + Thread.currentThread().getName() + "] " + getName(context.getIncompleteSelection()) + "...");
            case 1:
                if (solution.getModel().nrUnassignedVariables(solution.getAssignment()) > 0) {
                    return context.getIncompleteSelection().selectNeighbour(solution);
                }
                context.setPhase(2);
                if (context.hasHillClimberSelection())
                    iProgress.info("[" + Thread.currentThread().getName() + "] " + getName(context.getHillClimberSelection()) + "...");
            case 2:
                if (solution.getModel().nrUnassignedVariables(solution.getAssignment()) > 0)
                    return context.getIncompleteSelection().selectNeighbour(solution);
                if (context.hasHillClimberSelection()) {
                    n = context.getHillClimberSelection().selectNeighbour(solution);
                    if (n != null) return n;
                }
                context.setPhase(3);
                iProgress.info("[" + Thread.currentThread().getName() + "] " + getName(context.getImprovementSelection()) + "...");
            case 3:
                if (solution.getModel().nrUnassignedVariables(solution.getAssignment()) > 0)
                    return context.getIncompleteSelection().selectNeighbour(solution);
                return context.getImprovementSelection().selectNeighbour(solution);
            default:
                return null;
        }
    }

    @Override
    public MetaHeuristicSearchContext createAssignmentContext(Assignment<V, T> assignment) {
        return new MetaHeuristicSearchContext(assignment.getIndex() - 1);
    }

    public class MetaHeuristicSearchContext implements AssignmentContext {
        private int iPhase = -1;
        private NeighbourSelection<V, T> iConstructionSelection = null;
        private NeighbourSelection<V, T> iIncompleteSelection = null;
        private NeighbourSelection<V, T> iHillClimberSelection = null;
        private NeighbourSelection<V, T> iImprovementSelection = null;
                
        public MetaHeuristicSearchContext(int index) {
            if (!iConstructionPhase.isEmpty()) {
                if (index < 0) iConstructionSelection = iConstructionPhase.get(0);
                iConstructionSelection = (index < 0 ? iConstructionPhase.get(0) : iConstructionPhase.get(index % iConstructionPhase.size()));
            }
            if (!iIncompletePhase.isEmpty()) {
                if (index < 0) iIncompleteSelection = iIncompletePhase.get(0);
                iIncompleteSelection = (index < 0 ? iIncompletePhase.get(0) : iIncompletePhase.get(index % iIncompletePhase.size()));
            }
            if (!iImprovementPhase.isEmpty()) {
                if (index < 0) iImprovementSelection = iImprovementPhase.get(0);
                iImprovementSelection = (index < 0 ? iImprovementPhase.get(0) : iImprovementPhase.get(index % iImprovementPhase.size()));
            }
            if (!iHillClimberPhase.isEmpty()) {
                if (index < 0) iHillClimberSelection = iHillClimberPhase.get(0);
                iHillClimberSelection = (index < 0 ? iHillClimberPhase.get(0) : iHillClimberPhase.get(index % iHillClimberPhase.size()));
            }
            if (iConstructionSelection == null) iConstructionSelection = iIncompleteSelection;
            if (iIncompleteSelection == null) iIncompleteSelection = iConstructionSelection;
            if (iImprovementSelection == null) iImprovementSelection = iIncompleteSelection;
            iLog.info("Using " + iConstructionSelection.getClass().getSimpleName() +
                    " > " + iIncompleteSelection.getClass().getSimpleName() + 
                    (iHillClimberSelection == null ? "" : " > " + iHillClimberSelection.getClass().getSimpleName()) +
                    " > " + iImprovementSelection.getClass().getSimpleName());
        }
        
        public NeighbourSelection<V, T> getConstructionSelection() {
            return iConstructionSelection;
        }
        
        public NeighbourSelection<V, T> getIncompleteSelection() {
            return iIncompleteSelection;
        }
        
        public NeighbourSelection<V, T> getHillClimberSelection() {
            return iHillClimberSelection;
        }
        
        public boolean hasHillClimberSelection() {
            return iHillClimberSelection != null;
        }

        public NeighbourSelection<V, T> getImprovementSelection() {
            return iImprovementSelection;
        }
        
        public int getPhase() { return iPhase; }
        public void setPhase(int phase) { iPhase = phase; }
    }
}
