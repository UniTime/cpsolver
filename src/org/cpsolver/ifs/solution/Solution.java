package org.cpsolver.ifs.solution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.perturbations.PerturbationsCounter;
import org.cpsolver.ifs.solver.Solver;


/**
 * Generic solution. <br>
 * <br>
 * It consist from the model and information about current iteration and
 * solution time.
 * 
 * @see Model
 * @see org.cpsolver.ifs.solver.Solver
 * 
 * @version IFS 1.3 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
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
 * 
 * @param <V> Variable
 * @param <T> Value
 */
public class Solution<V extends Variable<V, T>, T extends Value<V, T>> {
    private static java.text.DecimalFormat sTimeFormat = new java.text.DecimalFormat("0.00", new java.text.DecimalFormatSymbols(Locale.US));

    private Model<V, T> iModel;
    private Assignment<V, T> iAssignment;
    private long iIteration = 0;
    private long iFailedIterations = 0;
    private double iTime = 0.0;

    private boolean iBestComplete = false;
    private Map<String, String> iBestInfo = null;
    private long iBestIteration = -1;
    private long iBestFailedIterations = -1;
    private double iBestTime = -1;
    private double iBestPerturbationsPenaly = -1.0;
    private int iBestIndex = -1;

    private List<SolutionListener<V, T>> iSolutionListeners = new ArrayList<SolutionListener<V, T>>();
    private PerturbationsCounter<V, T> iPerturbationsCounter = null;

    /** Constructor 
     * @param model problem model
     **/
    @Deprecated
    public Solution(Model<V, T> model) {
        this(model, model.getDefaultAssignment(), 0, 0.0);
    }
    
    /** Constructor 
     * @param model problem model
     * @param assignment current assignment
     **/
    public Solution(Model<V, T> model, Assignment<V, T> assignment) {
        this(model, assignment, 0, 0.0);
    }

    /** Constructor
     * @param model problem model
     * @param assignment current assignment
     * @param iteration current iteration
     * @param time current solver time
     **/
    public Solution(Model<V, T> model, Assignment<V, T> assignment, long iteration, double time) {
        iModel = model;
        iAssignment = assignment;
        iIteration = iteration;
        iTime = time;
    }

    /** Current iteration 
     * @return current iteration
     **/
    public long getIteration() {
        return iIteration;
    }
    
    /** Number of failed iterations (i.e., number of calls {@link Solution#update(double, boolean)} with false success)
     * @return number of failed iterations 
     **/
    public long getFailedIterations() {
        return iFailedIterations;
    }
    
    /** Number of failed iterations (i.e., number of calls {@link Solution#update(double, boolean)} with false success) in the best solution
     * @return number of failed iterations in the best solution
     **/
    public long getBestFailedIterations() {
        return (iBestIteration < 0 ? getFailedIterations() : iBestFailedIterations);
    }

    /** The model associated with the solution
     * @return problem model 
     **/
    public Model<V, T> getModel() {
        return iModel;
    }
    
    /** The assignment associated with this solution
     * @return current assignment
     **/
    public Assignment<V, T> getAssignment() {
        return iAssignment;
    }
    
    /** Set a new assignment 
     * @param assignment current assignment
     **/
    public void setAssignment(Assignment<V, T> assignment) {
        iAssignment = assignment;
    }

    /** Current solution time (time in seconds from the start of the solver) 
     * @return solver time
     **/
    public double getTime() {
        return iTime;
    }

    /** Update time, increment current iteration 
     * @param time updated solver time
     * @param success true if the last iteration was successful
     **/
    public void update(double time, boolean success) {
        iTime = time;
        iIteration++;
        if (!success) iFailedIterations ++;
        for (SolutionListener<V, T> listener : iSolutionListeners)
            listener.solutionUpdated(this);
    }
    
    /** Update time, increment current iteration 
     * @param time updated solver time
     **/
    public void update(double time) {
        update(time, true);
    }

    /** Initialization 
     * @param solver current solver
     **/
    public void init(Solver<V, T> solver) {
        iIteration = 0;
        iFailedIterations = 0;
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
     * @return info table
     */
    public Map<String, String> getInfo() {
        Map<String, String> ret = getModel().getInfo(iAssignment);
        if (getPerturbationsCounter() != null)
            getPerturbationsCounter().getInfo(getAssignment(), getModel(), ret);
        ret.put("Time", sTimeFormat.format(getTime() / 60.0) + " min");
        ret.put("Iteration", getIteration() + (getFailedIterations() > 0 ? " (" + sTimeFormat.format(100.0 * getFailedIterations() / getIteration())+ "% failed)" : ""));
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
     * @return extended info table
     */
    public Map<String, String> getExtendedInfo() {
        Map<String, String> ret = getModel().getExtendedInfo(iAssignment);
        if (getPerturbationsCounter() != null)
            getPerturbationsCounter().getInfo(getAssignment(), getModel(), ret);
        ret.put("Time", sTimeFormat.format(getTime() / 60.0) + " min");
        ret.put("Iteration", getIteration() + (getFailedIterations() > 0 ? " (" + sTimeFormat.format(100.0 * getFailedIterations() / getIteration())+ "% failed)" : ""));
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
     * @param variables sub-problem
     * @return info table
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

    /** Info of the best ever found solution 
     * @return info table of the best solution
     **/
    public Map<String, String> getBestInfo() {
        return iBestInfo;
    }

    /** Iteration when the best ever found solution was found 
     * @return iteration of the best solution
     **/
    public long getBestIteration() {
        return (iBestIteration < 0 ? getIteration() : iBestIteration);
    }

    /** Solution time when the best ever found solution was found
     * @return solver time of the best solution
     **/
    public double getBestTime() {
        return (iBestTime < 0 ? getTime() : iBestTime);
    }

    /**
     * Returns true, if all variables of the best ever solution found are
     * assigned
     * @return true if the best solution has all the variables assigned
     */
    public boolean isBestComplete() {
        return iBestComplete;
    }
    
    /**
     * Index of the best assignment.
     * @return {@link Assignment#getIndex()} of the best saved solution
     */
    public int getBestIndex() {
        return iBestIndex;
    }

    /**
     * Total value of the best ever found solution -- sum of all assigned values
     * (see {@link Value#toDouble(Assignment)}).
     * @return value of the best solution
     */
    public double getBestValue() {
        return getModel().getBestValue();
    }

    /** Set total value of the best ever found solution 
     * @param bestValue value of the best solution
     **/
    public void setBestValue(double bestValue) {
        getModel().setBestValue(bestValue);
    }

    /**
     * Perturbation penalty of the best ever found solution (see
     * {@link PerturbationsCounter})
     * @return perturbation penalty of the best solution
     */
    public double getBestPerturbationsPenalty() {
        return iBestPerturbationsPenaly;
    }

    /** Returns perturbation counter 
     * @return perturbations counter
     **/
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
            iBestFailedIterations = 0;
            iBestComplete = false;
            iBestIndex = -1;
            iBestPerturbationsPenaly = -1.0;
            for (SolutionListener<V, T> listener : iSolutionListeners)
                listener.bestCleared(this);
        }
    }
    
    /** True if the solution is complete, i.e., all the variables are assigned 
     * @return true if all the variables are assigned
     **/
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
            iBestFailedIterations = getFailedIterations();
            iBestComplete = isComplete();
            iBestIndex = getAssignment().getIndex();
            iBestPerturbationsPenaly = (iPerturbationsCounter == null ? 0.0 : iPerturbationsCounter.getPerturbationPenalty(getAssignment(), getModel()));
            for (SolutionListener<V, T> listener : iSolutionListeners)
                listener.bestSaved(this);
            
            if (master != null) {
                master.iIteration = iIteration;
                master.iFailedIterations = iFailedIterations;
                master.iTime = iTime;
                master.iBestInfo = iBestInfo;
                master.iBestTime = iBestTime;
                master.iBestIteration = iBestIteration;
                master.iBestFailedIterations = iBestFailedIterations;
                master.iBestComplete = iBestComplete;
                master.iBestPerturbationsPenaly = iBestPerturbationsPenaly;
                master.iBestIndex = iBestIndex;
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
            iFailedIterations = iBestFailedIterations;
            for (SolutionListener<V, T> listener : iSolutionListeners)
                listener.bestRestored(this);
        }
    }

    /** Adds solution listener 
     * @param listener a solution listener
     **/
    public void addSolutionListener(SolutionListener<V, T> listener) {
        iSolutionListeners.add(listener);
    }

    /** Removes solution listener
     * @param listener a solution listener
     **/
    public void removeSolutionListener(SolutionListener<V, T> listener) {
        iSolutionListeners.remove(listener);
    }
    
    /** Registered of solution listeners
     * @return list of registered solution listener
     **/
    public List<SolutionListener<V, T>> getSolutionListeners() {
        return iSolutionListeners;
    }
}
