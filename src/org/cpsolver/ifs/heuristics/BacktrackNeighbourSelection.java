package org.cpsolver.ifs.heuristics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;


import org.apache.logging.log4j.Logger;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentContext;
import org.cpsolver.ifs.constant.ConstantVariable;
import org.cpsolver.ifs.extension.ConflictStatistics;
import org.cpsolver.ifs.extension.Extension;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.JProf;

/**
 * Backtracking-based neighbour selection. A best neighbour that is found by a
 * backtracking-based algorithm within a limited depth from a selected variable
 * is returned. <br>
 * <br>
 * Parameters: <br>
 * <table border='1'><caption>Related Solver Parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>Neighbour.BackTrackTimeout</td>
 * <td>{@link Integer}</td>
 * <td>Timeout for each neighbour selection (in milliseconds).</td>
 * </tr>
 * <tr>
 * <td>Neighbour.BackTrackDepth</td>
 * <td>{@link Integer}</td>
 * <td>Limit of search depth.</td>
 * </tr>
 * </table>
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2014 Tomas Muller<br>
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
public class BacktrackNeighbourSelection<V extends Variable<V, T>, T extends Value<V, T>> extends StandardNeighbourSelection<V, T> {
    private ConflictStatistics<V, T> iStat = null;
    private static Logger sLog = org.apache.logging.log4j.LogManager.getLogger(BacktrackNeighbourSelection.class);
    private int iTimeout = 5000;
    private int iDepth = 4;
    private int iMaxIters = -1;
    protected BacktrackNeighbourSelectionContext iContext;

    /**
     * Constructor
     * 
     * @param properties
     *            configuration
     * @throws Exception thrown when initialization fails
     */
    public BacktrackNeighbourSelection(DataProperties properties) throws Exception {
        super(properties);
        iTimeout = properties.getPropertyInt("Neighbour.BackTrackTimeout", iTimeout);
        iDepth = properties.getPropertyInt("Neighbour.BackTrackDepth", iDepth);
        iMaxIters = properties.getPropertyInt("Neighbour.BackTrackMaxIters", iMaxIters);
    }

    /** Solver initialization */
    @Override
    public void init(Solver<V, T> solver) {
        super.init(solver);
        for (Extension<V, T> extension : solver.getExtensions()) {
            if (ConflictStatistics.class.isInstance(extension))
                iStat = (ConflictStatistics<V, T>) extension;
        }
    }

    /**
     * Select neighbour. The standard variable selection (see
     * {@link StandardNeighbourSelection#getVariableSelection()}) is used to
     * select a variable. A backtracking of a limited depth is than employed
     * from this variable. The best assignment found is returned (see
     * {@link BackTrackNeighbour}).
     **/
    @Override
    public Neighbour<V, T> selectNeighbour(Solution<V, T> solution) {
        return selectNeighbour(solution, getVariableSelection().selectVariable(solution));
    }

    /**
     * Select neighbour -- starts from the provided variable. A backtracking of
     * a limited depth is employed from the given variable. The best assignment
     * found is returned (see {@link BackTrackNeighbour}).
     * @param solution current solution
     * @param variable selected variable
     * @return a neighbour, null if not found
     **/
    public Neighbour<V, T> selectNeighbour(Solution<V, T> solution, V variable) {
        if (variable == null)
            return null;

        BacktrackNeighbourSelectionContext context = new BacktrackNeighbourSelectionContext(solution);
        selectNeighbour(solution, variable, context);
        return context.getBackTrackNeighbour();
    }
    
    protected void selectNeighbour(Solution<V, T> solution, V variable, BacktrackNeighbourSelectionContext context) {
        iContext = context;
        Lock lock = solution.getLock().writeLock();
        lock.lock();
        try {
            if (sLog.isDebugEnabled())
                sLog.debug("-- before BT (" + variable.getName() + "): nrAssigned=" + solution.getAssignment().nrAssignedVariables() + ",  value=" + solution.getModel().getTotalValue(solution.getAssignment()));

            List<V> variables2resolve = new ArrayList<V>(1);
            variables2resolve.add(variable);
            backtrack(context, variables2resolve, 0, iDepth);

            if (sLog.isDebugEnabled())
                sLog.debug("-- after  BT (" + variable.getName() + "): nrAssigned=" + solution.getAssignment().nrAssignedVariables() + ",  value=" + solution.getModel().getTotalValue(solution.getAssignment()));
        } finally {
            lock.unlock();
        }

        if (sLog.isDebugEnabled())
            sLog.debug("-- selected neighbour: " + context.getBackTrackNeighbour());
    }
    
    public BacktrackNeighbourSelectionContext getContext() {
        return iContext;
    }

    private boolean containsConstantValues(Collection<T> values) {
        for (T value : values) {
            if (value.variable() instanceof ConstantVariable && ((ConstantVariable<?>) value.variable()).isConstant())
                return true;
        }
        return false;
    }

    /** List of values of the given variable that will be considered 
     * @param context assignment context
     * @param variable given variable
     * @return values of the given variable that will be considered
     **/
    protected Iterator<T> values(BacktrackNeighbourSelectionContext context, V variable) {
        return variable.values(context.getAssignment()).iterator();
    }

    /** Check bound 
     * @param variables2resolve unassigned variables that are in conflict with the current solution
     * @param idx position in variables2resolve
     * @param depth current depth
     * @param value value to check
     * @param conflicts conflicting values
     * @return bound (best possible improvement)
     **/
    protected boolean checkBound(List<V> variables2resolve, int idx, int depth, T value, Set<T> conflicts) {
        int nrUnassigned = variables2resolve.size() - idx;
        if ((nrUnassigned + conflicts.size() > depth)) {
            if (sLog.isDebugEnabled())
                sLog.debug("        -- too deap");
            return false;
        }
        if (containsConstantValues(conflicts)) {
            if (sLog.isDebugEnabled())
                sLog.debug("        -- contains constants values");
            return false;
        }
        boolean containAssigned = false;
        for (Iterator<T> i = conflicts.iterator(); !containAssigned && i.hasNext();) {
            T conflict = i.next();
            int confIdx = variables2resolve.indexOf(conflict.variable());
            if (confIdx >= 0 && confIdx <= idx) {
                if (sLog.isDebugEnabled())
                    sLog.debug("        -- contains resolved variable " + conflict.variable());
                containAssigned = true;
            }
        }
        if (containAssigned)
            return false;
        return true;
    }

    /** Check whether backtrack can continue 
     * @param context assignment context
     * @param variables2resolve unassigned variables that are in conflict with the current solution
     * @param idx position in variables2resolve
     * @param depth current depth
     * @return true if the search can continue
     **/
    protected boolean canContinue(BacktrackNeighbourSelectionContext context, List<V> variables2resolve, int idx, int depth) {
        if (depth <= 0) {
            if (sLog.isDebugEnabled())
                sLog.debug("    -- depth reached");
            return false;
        }
        if (context.isTimeoutReached()) {
            if (sLog.isDebugEnabled())
                sLog.debug("    -- timeout reached");
            return false;
        }
        if (context.isMaxItersReached()) {
            if (sLog.isDebugEnabled())
                sLog.debug("    -- max number of iterations reached");
            return false;
        }
        return true;
    }

    protected boolean canContinueEvaluation(BacktrackNeighbourSelectionContext context) {
        return !context.isMaxItersReached() && !context.isTimeoutReached();
    }

    /** Backtracking 
     * @param context assignment context
     * @param variables2resolve unassigned variables that are in conflict with the current solution
     * @param idx position in variables2resolve
     * @param depth current depth
     **/
    protected void backtrack(BacktrackNeighbourSelectionContext context, List<V> variables2resolve, int idx, int depth) {
        if (sLog.isDebugEnabled())
            sLog.debug("  -- bt[" + depth + "]: " + idx + " of " + variables2resolve.size() + " " + variables2resolve);
        context.incIteration();
        int nrUnassigned = variables2resolve.size() - idx;
        if (nrUnassigned == 0) {
            context.saveBest(variables2resolve);
            return;
        }
        if (!canContinue(context, variables2resolve, idx, depth))
            return;
        V variable = variables2resolve.get(idx);
        if (sLog.isDebugEnabled())
            sLog.debug("    -- variable " + variable);
        for (Iterator<T> e = values(context, variable); canContinueEvaluation(context) && e.hasNext();) {
            T value = e.next();
            T current = context.getAssignment().getValue(variable);
            if (value.equals(current))
                continue;
            if (sLog.isDebugEnabled())
                sLog.debug("      -- value " + value);
            Set<T> conflicts = context.getModel().conflictValues(context.getAssignment(), value);
            if (sLog.isDebugEnabled())
                sLog.debug("      -- conflicts " + conflicts);
            if (!checkBound(variables2resolve, idx, depth, value, conflicts))
                continue;
            List<V> newVariables2resolve = new ArrayList<V>(variables2resolve);
            for (Iterator<T> i = conflicts.iterator(); i.hasNext();) {
                T conflict = i.next();
                context.getAssignment().unassign(0, conflict.variable());
                if (!newVariables2resolve.contains(conflict.variable()))
                    newVariables2resolve.add(conflict.variable());
            }
            if (current != null)
                context.getAssignment().unassign(0, current.variable());
            context.getAssignment().assign(0, value);
            backtrack(context, newVariables2resolve, idx + 1, depth - 1);
            if (current == null)
                context.getAssignment().unassign(0, variable);
            else
                context.getAssignment().assign(0, current);
            for (Iterator<T> i = conflicts.iterator(); i.hasNext();) {
                T conflict = i.next();
                context.getAssignment().assign(0, conflict); 
            }
        }
    }

    /** Backtracking neighbour */
    public class BackTrackNeighbour implements Neighbour<V, T> {
        private double iTotalValue = 0;
        private double iValue = 0;
        private List<T> iDifferentAssignments = null;
        private Model<V, T> iModel = null;

        /**
         * Constructor
         * 
         * @param context assignment context
         * @param resolvedVariables
         *            variables that has been changed
         */
        public BackTrackNeighbour(BacktrackNeighbourSelectionContext context, List<V> resolvedVariables) {
            iTotalValue = context.getModel().getTotalValue(context.getAssignment());
            iDifferentAssignments = new ArrayList<T>();
            for (V variable : resolvedVariables) {
                T value = variable.getAssignment(context.getAssignment());
                iDifferentAssignments.add(value);
            }
            iValue = iTotalValue - context.iValue;
            if (sLog.isDebugEnabled())
                iModel = context.getModel();
        }
        
        /**
         * Constructor
         * 
         * @param context assignment context
         * @param resolvedVariables
         *            variables that has been changed
         */
        public BackTrackNeighbour(BacktrackNeighbourSelectionContext context,
                @SuppressWarnings("unchecked") V... resolvedVariables) {
            iTotalValue = context.getModel().getTotalValue(context.getAssignment());
            iDifferentAssignments = new ArrayList<T>();
            for (V variable : resolvedVariables) {
                T value = variable.getAssignment(context.getAssignment());
                iDifferentAssignments.add(value);
            }
            iValue = iTotalValue - context.iValue;
            if (sLog.isDebugEnabled())
                iModel = context.getModel();
        }

        /** Neighbour value (solution total value if the neighbour is applied). 
         * @return value of the new solution
         **/
        public double getTotalValue() {
            return iTotalValue;
        }

        /**
         * Sum of values of variables from the neighbour that change their
         * values
         */
        @Override
        public double value(Assignment<V, T> assignment) {
            return iValue;
        }

        /** Neighbour assignments 
         * @return list of assignments in this neighbour
         **/
        public List<T> getAssignments() {
            return iDifferentAssignments;
        }

        /**
         * Assign the neighbour
         */
        @Override
        public void assign(Assignment<V, T> assignment, long iteration) {
            if (sLog.isDebugEnabled())
                sLog.debug("-- before assignment: nrAssigned=" + assignment.nrAssignedVariables() + ",  value=" + iModel.getTotalValue(assignment));
            if (sLog.isDebugEnabled())
                sLog.debug("  " + this);
            int idx = 0;
            for (Iterator<T> e = iDifferentAssignments.iterator(); e.hasNext(); idx++) {
                T p = e.next();
                T o = assignment.getValue(p.variable());
                if (o != null) {
                    if (idx > 0 && iStat != null)
                        iStat.variableUnassigned(iteration, o, iDifferentAssignments.get(0));
                    assignment.unassign(iteration, p.variable());
                }
            }
            for (T p : iDifferentAssignments) {
                assignment.assign(iteration, p);
            }
            if (sLog.isDebugEnabled())
                sLog.debug("-- after assignment: nrAssigned=" + assignment.nrAssignedVariables() + ",  value=" + iModel.getTotalValue(assignment));
        }

        /**
         * Compare two neighbours
         * @param solution current solution
         * @return comparison
         */
        public int compareTo(Solution<V, T> solution) {
            return Double.compare(iTotalValue, solution.getModel().getTotalValue(solution.getAssignment()));
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer("BT{value=" + iTotalValue + ": ");
            for (Iterator<T> e = iDifferentAssignments.iterator(); e.hasNext();) {
                T p = e.next();
                sb.append("\n    " + p.variable().getName() + " " + p.getName() + (e.hasNext() ? "," : ""));
            }
            sb.append("}");
            return sb.toString();
        }

        @Override
        public Map<V, T> assignments() {
            Map<V, T> ret = new HashMap<V, T>();
            for (T p : iDifferentAssignments)
                ret.put(p.variable(), p);
            return ret;
        }
    }

    /** Return maximal depth 
     * @return maximal search depth
     **/
    public int getDepth() {
        return iDepth;
    }

    /** Set maximal depth 
     * @param depth maximal search depth
     **/
    public void setDepth(int depth) {
        iDepth = depth;
    }

    /** Return time limit 
     * @return time limit 
     **/
    public int getTimeout() {
        return iTimeout;
    }

    /** Set time limit 
     * @param timeout time limit
     **/
    public void setTimeout(int timeout) {
        iTimeout = timeout;
    }

    /** Return maximal number of iterations 
     * @return maximal number of iterations
     **/
    public int getMaxIters() {
        return iMaxIters;
    }

    /** Set maximal number of iterations 
     * @param maxIters maximal number of iterations
     **/
    public void setMaxIters(int maxIters) {
        iMaxIters = maxIters;
    }
    
    public class BacktrackNeighbourSelectionContext implements AssignmentContext {
        private long iT0, iT1 = 0;
        private boolean iTimeoutReached = false;
        private int iMaxIters = -1, iNrIters = 0;
        protected Solution<V, T> iSolution = null;
        protected BackTrackNeighbour iBackTrackNeighbour = null;
        protected double iValue = 0;
        private int iNrAssigned = 0;
        private boolean iMaxItersReached = false;
        
        public BacktrackNeighbourSelectionContext(Solution<V, T> solution) {
            iSolution = solution;
            iBackTrackNeighbour = null;
            iValue = solution.getModel().getTotalValue(iSolution.getAssignment());
            iNrAssigned = iSolution.getAssignment().nrAssignedVariables();
            iT0 = JProf.currentTimeMillis();
            iNrIters = 0;
            iTimeoutReached = false;
            iMaxItersReached = false;
        }

        /** Time needed to find a neighbour (last call of selectNeighbour method) 
         * @return search time
         **/
        public long getTime() {
            if (iT1 == 0) return JProf.currentTimeMillis() - iT0;
            return iT1 - iT0;
        }

        /**
         * True, if timeout was reached during the last call of selectNeighbour
         * method
         * @return true if the timeout was reached
         */
        public boolean isTimeoutReached() {
            return iTimeoutReached;
        }

        /**
         * True, if the maximum number of iterations was reached by the last call of
         * selectNeighbour method
         * @return true if the maximum number of iterations was reached
         */
        public boolean isMaxItersReached() {
            return iMaxItersReached;
        }
        
        public BackTrackNeighbour getBackTrackNeighbour() { return iBackTrackNeighbour; }
        
        public void incIteration() {
            iT1 = JProf.currentTimeMillis();
            if (!iTimeoutReached && iTimeout > 0 && iT1 - iT0 > iTimeout)
                iTimeoutReached = true;
            if (!iMaxItersReached && iMaxIters > 0 && iNrIters++ > iMaxIters)
                iMaxItersReached = true;
        }
        
        public void saveBest(List<V> variables2resolve) {
            if (sLog.isDebugEnabled())
                sLog.debug("    -- all assigned");
            if (iSolution.getAssignment().nrAssignedVariables() > iNrAssigned || (iSolution.getAssignment().nrAssignedVariables() == iNrAssigned && iValue > iSolution.getModel().getTotalValue(iSolution.getAssignment()))) {
                if (sLog.isDebugEnabled())
                    sLog.debug("    -- better than current");
                if (iBackTrackNeighbour == null || iBackTrackNeighbour.compareTo(iSolution) >= 0) {
                    if (sLog.isDebugEnabled())
                        sLog.debug("      -- better than best");
                    iBackTrackNeighbour = new BackTrackNeighbour(this, variables2resolve);
                }
            }
        }
        
        public void saveBest(@SuppressWarnings("unchecked") V... variables2resolve) {
            if (sLog.isDebugEnabled())
                sLog.debug("    -- all assigned");
            if (iSolution.getAssignment().nrAssignedVariables() > iNrAssigned || (iSolution.getAssignment().nrAssignedVariables() == iNrAssigned && iValue > iSolution.getModel().getTotalValue(iSolution.getAssignment()))) {
                if (sLog.isDebugEnabled())
                    sLog.debug("    -- better than current");
                if (iBackTrackNeighbour == null || iBackTrackNeighbour.compareTo(iSolution) >= 0) {
                    if (sLog.isDebugEnabled())
                        sLog.debug("      -- better than best");
                    iBackTrackNeighbour = new BackTrackNeighbour(this, variables2resolve);
                }
            }
        }
        
        public Model<V, T> getModel() { return iSolution.getModel();}
        
        public Assignment<V, T> getAssignment() { return iSolution.getAssignment(); }
    }
}
