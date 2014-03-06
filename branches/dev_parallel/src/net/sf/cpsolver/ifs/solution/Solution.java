package net.sf.cpsolver.ifs.solution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.sf.cpsolver.ifs.assignment.Assignment;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.perturbations.PerturbationsCounter;
import net.sf.cpsolver.ifs.solver.Solver;

/**
 * Generic solution. <br>
 * <br>
 * It consist from the model and information about current iteration and
 * solution time.
 * 
 * @see Model
 * @see net.sf.cpsolver.ifs.solver.Solver
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
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

public class Solution<V extends Variable<V, T>, T extends Value<V, T>> {
    private static java.text.DecimalFormat sTimeFormat = new java.text.DecimalFormat("0.00",
            new java.text.DecimalFormatSymbols(Locale.US));

    private Model<V, T> iModel;
    private Assignment<V, T> iAssignment;
    private long iIteration = 0;
    private double iTime = 0.0;

    private boolean iBestComplete = false;
    private Map<String, String> iBestInfo = null;
    private long iBestIteration = -1;
    private double iBestTime = -1;
    private double iBestPerturbationsPenaly = -1.0;

    private List<SolutionListener<V, T>> iSolutionListeners = new ArrayList<SolutionListener<V, T>>();
    private PerturbationsCounter<V, T> iPerturbationsCounter = null;

    /** Constructor */
    @Deprecated
    public Solution(Model<V, T> model) {
        this(model, model.getDefaultAssignment(), 0, 0.0);
    }
    
    /** Constructor */
    public Solution(Model<V, T> model, Assignment<V, T> assignment) {
        this(model, assignment, 0, 0.0);
    }

    /** Constructor */
    public Solution(Model<V, T> model, Assignment<V, T> assignment, long iteration, double time) {
        iModel = model;
        iAssignment = assignment;
        iIteration = iteration;
        iTime = time;
    }

    /** Current iteration */
    public long getIteration() {
        return iIteration;
    }

    /** The model associated with the solution */
    public Model<V, T> getModel() {
        return iModel;
    }
    
    /** The assignment associated with this solution */
    public Assignment<V, T> getAssignment() {
        return iAssignment;
    }
    
    /** Set a new assignment */
    public void setAssignment(Assignment<V, T> assignment) {
        iAssignment = assignment;
    }

    /** Current solution time (time in seconds from the start of the solver) */
    public double getTime() {
        return iTime;
    }

    /** Update time, increment current iteration */
    public void update(double time) {
        iTime = time;
        iIteration++;
        for (SolutionListener<V, T> listener : iSolutionListeners)
            listener.solutionUpdated(this);
    }

    /** Initialization */
    public void init(Solver<V, T> solver) {
        iIteration = 0;
        iTime = 0;
        if (iModel != null)
            iModel.init(solver);
        iPerturbationsCounter = solver.getPerturbationsCounter();
    }

    @Override
    public String toString() {
        return "Solution{\n  model=" + iModel + ",\n  iteration=" + iIteration + ",\n  time=" + iTime + "\n}";
    }

    /**
     * Solution information. It consists from info from the model which is
     * associated with the solution, time, iteration, speed and infos from all
     * solution listeners.
     */
    public Map<String, String> getInfo() {
        Map<String, String> ret = getModel().getInfo(iAssignment);
        if (getPerturbationsCounter() != null)
            getPerturbationsCounter().getInfo(getAssignment(), getModel(), ret);
        ret.put("Time", sTimeFormat.format(getTime() / 60.0) + " min");
        ret.put("Iteration", String.valueOf(getIteration()));
        if (getTime() > 0)
            ret.put("Speed", sTimeFormat.format((getIteration()) / getTime()) + " it/s");
        for (SolutionListener<V, T> listener : iSolutionListeners)
            listener.getInfo(this, ret);
        return ret;
    }

    /**
     * Extended solution information. Similar to {@link Solution#getInfo()}, but
     * some more information (that is more expensive to compute) might be added.
     * Also extended model information is added (see
     * {@link Model#getExtendedInfo(Assignment)}) into the resultant table.
     */
    public Map<String, String> getExtendedInfo() {
        Map<String, String> ret = getModel().getExtendedInfo(iAssignment);
        if (getPerturbationsCounter() != null)
            getPerturbationsCounter().getInfo(getAssignment(), getModel(), ret);
        ret.put("Time", sTimeFormat.format(getTime() / 60.0) + " min");
        ret.put("Iteration", String.valueOf(getIteration()));
        if (getTime() > 0)
            ret.put("Speed", sTimeFormat.format((getIteration()) / getTime()) + " it/s");
        for (SolutionListener<V, T> listener : iSolutionListeners)
            listener.getInfo(this, ret);
        return ret;
    }

    /**
     * Solution information. It consists from info from the model which is
     * associated with the solution, time, iteration, speed and infos from all
     * solution listeners. Only variables from the given set are included.
     */
    public Map<String, String> getInfo(Collection<V> variables) {
        Map<String, String> ret = getModel().getInfo(iAssignment, variables);
        if (getPerturbationsCounter() != null)
            getPerturbationsCounter().getInfo(getAssignment(), getModel(), ret, variables);
        ret.put("Time", sTimeFormat.format(getTime()) + " sec");
        ret.put("Iteration", String.valueOf(getIteration()));
        if (getTime() > 0)
            ret.put("Speed", sTimeFormat.format((getIteration()) / getTime()) + " it/s");
        for (SolutionListener<V, T> listener : iSolutionListeners)
            listener.getInfo(this, ret, variables);
        return ret;
    }

    /** Info of the best ever found solution */
    public Map<String, String> getBestInfo() {
        return iBestInfo;
    }

    /** Iteration when the best ever found solution was found */
    public long getBestIteration() {
        return (iBestIteration < 0 ? getIteration() : iBestIteration);
    }

    /** Solution time when the best ever found solution was found */
    public double getBestTime() {
        return (iBestTime < 0 ? getTime() : iBestTime);
    }

    /**
     * Returns true, if all variables of the best ever solution found are
     * assigned
     */
    public boolean isBestComplete() {
        return iBestComplete;
    }

    /**
     * Total value of the best ever found solution -- sum of all assigned values
     * (see {@link Value#toDouble()}).
     */
    public double getBestValue() {
        return getModel().getBestValue();
    }

    /** Set total value of the best ever found solution */
    public void setBestValue(double bestValue) {
        getModel().setBestValue(bestValue);
    }

    /**
     * Perturbation penalty of the best ever found solution (see
     * {@link PerturbationsCounter})
     */
    public double getBestPerturbationsPenalty() {
        return iBestPerturbationsPenaly;
    }

    /** Returns perturbation counter */
    public PerturbationsCounter<V, T> getPerturbationsCounter() {
        return iPerturbationsCounter;
    }

    /** Clear the best ever found solution */
    public void clearBest() {
        synchronized (getModel()) {
            getModel().clearBest();
            iBestInfo = null;
            iBestTime = -1;
            iBestIteration = -1;
            iBestComplete = false;
            iBestPerturbationsPenaly = -1.0;
            for (SolutionListener<V, T> listener : iSolutionListeners)
                listener.bestCleared(this);
        }
    }
    
    /** True if the solution is complete, i.e., all the variables are assigned */
    public boolean isComplete() {
        return getAssignment().nrAssignedVariables() == getModel().variables().size();
    }

    /**
     * Save the current solution as the best ever found solution (it also calls
     * {@link Model#saveBest(Assignment)})
     * @param master master solution into which information about the best solution are to be copied as well
     */
    public void saveBest(Solution<V, T> master) {
        synchronized (getModel()) {
            getModel().saveBest(iAssignment);
            iBestInfo = getInfo();
            iBestTime = getTime();
            iBestIteration = getIteration();
            iBestComplete = isComplete();
            iBestPerturbationsPenaly = (iPerturbationsCounter == null ? 0.0 : iPerturbationsCounter.getPerturbationPenalty(getAssignment(), getModel()));
            for (SolutionListener<V, T> listener : iSolutionListeners)
                listener.bestSaved(this);
            
            if (master != null) {
                master.iIteration = iIteration;
                master.iTime = iTime;
                master.iBestInfo = iBestInfo;
                master.iBestTime = iBestTime;
                master.iBestIteration = iBestIteration;
                master.iBestComplete = iBestComplete;
                master.iBestPerturbationsPenaly = iBestPerturbationsPenaly;
            }
        }
    }
    
    /**
     * Save the current solution as the best ever found solution (it also calls
     * {@link Model#saveBest(Assignment)})
     */
    public void saveBest() {
        saveBest(null);
    }

    /**
     * Restore the best ever found solution into the current solution (it also
     * calls {@link Model#restoreBest(Assignment)})
     */
    public void restoreBest() {
        synchronized (getModel()) {
            getModel().restoreBest(iAssignment);
            iTime = iBestTime;
            iIteration = iBestIteration;
            for (SolutionListener<V, T> listener : iSolutionListeners)
                listener.bestRestored(this);
        }
    }

    /** Adds solution listener */
    public void addSolutionListener(SolutionListener<V, T> listener) {
        iSolutionListeners.add(listener);
    }

    /** Removes solution listener */
    public void removeSolutionListener(SolutionListener<V, T> listener) {
        iSolutionListeners.remove(listener);
    }
    
    public List<SolutionListener<V, T>> getSolutionListeners() {
        return iSolutionListeners;
    }
}
