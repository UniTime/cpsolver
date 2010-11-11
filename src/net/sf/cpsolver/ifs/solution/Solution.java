package net.sf.cpsolver.ifs.solution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 */

public class Solution<V extends Variable<V, T>, T extends Value<V, T>> {
    private static java.text.DecimalFormat sTimeFormat = new java.text.DecimalFormat("0.00",
            new java.text.DecimalFormatSymbols(Locale.US));

    private Model<V, T> iModel;
    private long iIteration = 0;
    private double iTime = 0.0;

    private boolean iBestComplete = false;
    private Map<String, String> iBestInfo = null;
    private long iBestIteration = -1;
    private double iBestTime = -1;
    private double iBestPerturbationsPenaly = -1.0;
    private double iBestValue = 0;

    private List<SolutionListener<V, T>> iSolutionListeners = new ArrayList<SolutionListener<V, T>>();
    private PerturbationsCounter<V, T> iPerturbationsCounter = null;

    /** Constructor */
    public Solution(Model<V, T> model) {
        this(model, 0, 0.0);
    }

    /** Constructor */
    public Solution(Model<V, T> model, long iteration, double time) {
        iModel = model;
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
     * Solution information. It consits from info from the model which is
     * associated with the solution, time, iteration, speed and infos from all
     * solution listeners.
     */
    public Map<String, String> getInfo() {
        Map<String, String> ret = getModel().getInfo();
        if (getPerturbationsCounter() != null)
            getPerturbationsCounter().getInfo(ret, getModel());
        ret.put("Time", sTimeFormat.format(getTime()) + " sec");
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
     * {@link Model#getExtendedInfo()}) into the resultant table.
     */
    public Map<String, String> getExtendedInfo() {
        Map<String, String> ret = getModel().getExtendedInfo();
        if (getPerturbationsCounter() != null)
            getPerturbationsCounter().getInfo(ret, getModel());
        ret.put("Time", sTimeFormat.format(getTime()) + " sec");
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
        Map<String, String> ret = getModel().getInfo(variables);
        if (getPerturbationsCounter() != null)
            getPerturbationsCounter().getInfo(ret, getModel(), variables);
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
        return iBestValue;
    }

    /** Set total value of the best ever found solution */
    public void setBestValue(double bestValue) {
        iBestValue = bestValue;
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
        getModel().clearBest();
        iBestInfo = null;
        iBestTime = -1;
        iBestIteration = -1;
        iBestComplete = false;
        iBestValue = 0;
        iBestPerturbationsPenaly = -1.0;
        for (SolutionListener<V, T> listener : iSolutionListeners)
            listener.bestCleared(this);
    }

    /**
     * Save the current solution as the best ever found solution (it also calls
     * {@link Model#saveBest()})
     */
    public void saveBest() {
        getModel().saveBest();
        iBestInfo = getInfo();
        iBestTime = getTime();
        iBestIteration = getIteration();
        iBestComplete = getModel().nrUnassignedVariables() == 0;
        iBestValue = getModel().getTotalValue();
        iBestPerturbationsPenaly = (iPerturbationsCounter == null ? 0.0 : iPerturbationsCounter
                .getPerturbationPenalty(getModel()));
        for (SolutionListener<V, T> listener : iSolutionListeners)
            listener.bestSaved(this);
    }

    /**
     * Restore the best ever found solution into the current solution (it also
     * calls {@link Model#restoreBest()})
     */
    public void restoreBest() {
        if (iBestInfo == null)
            return;
        getModel().restoreBest();
        iTime = iBestTime;
        iIteration = iBestIteration;
        for (SolutionListener<V, T> listener : iSolutionListeners)
            listener.bestRestored(this);
    }

    /** Adds solution listner */
    public void addSolutionListener(SolutionListener<V, T> listener) {
        iSolutionListeners.add(listener);
    }

    /** Removes solution listener */
    public void removeSolutionListener(SolutionListener<V, T> listener) {
        iSolutionListeners.remove(listener);
    }
}
