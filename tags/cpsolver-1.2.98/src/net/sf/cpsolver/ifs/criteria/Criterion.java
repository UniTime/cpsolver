package net.sf.cpsolver.ifs.criteria;

import java.util.Collection;
import java.util.Set;

import net.sf.cpsolver.ifs.model.InfoProvider;
import net.sf.cpsolver.ifs.model.ModelListener;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;

/**
 * Criterion. <br>
 * <br>
 * An optimization objective can be split into several (optimization) criteria
 * and modeled as a weighted sum of these. This makes the implementation of a particular problem
 * more versatile as it allows for an easier modification of the optimization objective.
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2011 Tomas Muller<br>
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
public interface Criterion<V extends Variable<V, T>, T extends Value<V, T>> extends ModelListener<V, T>, InfoProvider<V> {
    
    /** Current value of the criterion (optimization objective) */
    public double getValue();
    
    /** Weighted value of the objectives */
    public double getWeightedValue();
    
    /** Bounds (minimum and maximum) estimate for the value */
    public double[] getBounds();
    
    /** Weighted best value of the objective (value in the best solution). */
    public double getWeightedBest();

    /** Best value (value of the criterion in the best solution) */
    public double getBest();
    
    /** Weight of the criterion */
    public double getWeight();
    
    /** Weighted value of a proposed assignment (including hard conflicts) */
    public double getWeightedValue(T value, Set<T> conflicts);
    
    /** Value of a proposed assignment (including hard conflicts) */
    public double getValue(T value, Set<T> conflicts);
    
    /** Weighted value of a part of the problem (given by the collection of variables) */
    public double getWeightedValue(Collection<V> variables);
    
    /** Value of a part of the problem (given by the collection of variables) */
    public double getValue(Collection<V> variables);
    
    /** Value bounds (minimum and maximum) of the criterion on a part of the problem */
    public double[] getBounds(Collection<V> variables);
    
    /** Criterion name */
    public String getName();
    
    /** Outside update of the criterion (usefull when the criterion is driven by a set of constraints). */
    public void inc(double value);
    
    /** Notification that the current solution has been saved to the best. */
    public void bestSaved();

    /** Notification that the current solution has been restored from the best. */
    public void bestRestored();
}
