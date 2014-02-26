package net.sf.cpsolver.ifs.model;

import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;

/**
 * Lazy neigbour (a change of the overall solution value is unknown before
 * the neighbour is assigned, it is possible to undo the neighbour instead). 
 * This neighbour is useful when it is 
 * two expensive to compute change of overall solution value before the 
 * variable is reassigned. It is possible to undo the neighbour instead.
 * Search strategy has to implement {@link LazyNeighbourAcceptanceCriterion}.
 *  
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2013 Tomas Muller<br>
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
public abstract class LazyNeighbour<V extends Variable<V, T>, T extends Value<V, T>> extends Neighbour<V,T> {
    private LazyNeighbourAcceptanceCriterion<V,T> iCriterion = null;
    
    /**
     * Set acceptance criterion (to be used by a search strategy before the 
     * neighbour is accepted, so that it can be undone if desired)  
     */
    public void setAcceptanceCriterion(LazyNeighbourAcceptanceCriterion<V,T> criterion) {
        iCriterion = criterion;
    }
    
    /**
     * Assign neighbour, check given acceptance criterion, and undo
     * assignment if the change is not accepted. 
     */
    @Override
    public void assign(long iteration) {
        double before = getModel().getTotalValue();
        doAssign(iteration);
        double after = getModel().getTotalValue();
        if (!iCriterion.accept(this, after - before)) undoAssign(iteration);
    }
    /**
     * Return -1 (neighbour is always accepted). The search strategy that
     * is using this neighbour must implement {@link LazyNeighbourAcceptanceCriterion}.
     */
    @Override
    public double value() { return -1; }
    
    /** Perform assignment */
    protected abstract void doAssign(long iteration);
    /** Undo assignment */
    protected abstract void undoAssign(long iteration);
    /** Return problem model (it is needed in order to be able to get
     * overall solution value before and after the assignment of this neighbour) */
    public abstract Model<V,T> getModel();
    
    /** Neighbour acceptance criterion interface (to be implemented
     * by search strategies that are using {@link LazyNeighbour}. 
     * It is also required to call {@link LazyNeighbour#setAcceptanceCriterion(LazyNeighbour.LazyNeighbourAcceptanceCriterion)}
     * before the neighbour is accepted by the search strategy. 
     */ 
    public static interface LazyNeighbourAcceptanceCriterion<V extends Variable<V, T>, T extends Value<V, T>> {
        /** True when the currently assigned neighbour should be accepted (false means
         * that the change will be undone
         * @param neighbour neighbour that was assigned
         * @param value change in overall solution value
         * @return true if the neighbour can be accepted (false to undo the assignment)
         */
        public boolean accept(LazyNeighbour<V,T> neighbour, double value);
    }
}