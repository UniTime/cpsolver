package net.sf.cpsolver.ifs.criteria;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Abstract Criterion. <br>
 * <br>
 * An optimization objective can be split into several (optimization) criteria
 * and modeled as a weighted sum of these. This makes the implementation of a particular problem
 * more versatile as it allows for an easier modification of the optimization objective.
 * <br>
 * This class implements most of the {@link Criterion} except of the {@link Criterion#getValue(Value, Set)}.
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
public abstract class AbstractCriterion<V extends Variable<V, T>, T extends Value<V, T>> implements Criterion<V, T> {
    private Model<V, T> iModel;
    protected double iBest = 0.0, iValue = 0.0, iWeight = 0.0;
    private double[] iBounds = null;
    protected static java.text.DecimalFormat sDoubleFormat = new java.text.DecimalFormat("0.##",
            new java.text.DecimalFormatSymbols(Locale.US));
    protected static java.text.DecimalFormat sPercentFormat = new java.text.DecimalFormat("0.##",
            new java.text.DecimalFormatSymbols(Locale.US));
    protected boolean iDebug = false;
    
    /**
     * Defines how the overall value of the criterion should be automatically updated (using {@link Criterion#getValue(Value, Set)}).
     */
    protected static enum ValueUpdateType {
        /** Update is done before an unassignment (decrement) and before an assignment (increment). */
        BeforeUnassignedBeforeAssigned,
        /** Update is done after an unassignment (decrement) and before an assignment (increment). */
        AfterUnassignedBeforeAssigned,
        /** Update is done before an unassignment (decrement) and after an assignment (increment). */
        BeforeUnassignedAfterAssigned,
        /** Update is done after an unassignment (decrement) and after an assignment (increment). This is the default. */
        AfterUnassignedAfterAssigned,
        /** Criterion is to be updated manually (e.g., using {@link Criterion#inc(double)}). */
        NoUpdate
    }
    protected ValueUpdateType iValueUpdateType = ValueUpdateType.BeforeUnassignedAfterAssigned;

    /** Defines weight name (to be used to get the criterion weight from the configuration). */
    public String getWeightName() {
        return "Weight." + getClass().getName().substring(1 + getClass().getName().lastIndexOf('.'));
    }
    
    /** Defines default weight (when {@link AbstractCriterion#getWeightName()} parameter is not present in the criterion). */
    public double getWeightDefault(DataProperties config) {
        return 0.0;
    }

    @Override
    public boolean init(Solver<V, T> solver) {
        iModel = solver.currentSolution().getModel();
        iWeight = solver.getProperties().getPropertyDouble(getWeightName(), getWeightDefault(solver.getProperties()));
        iDebug = solver.getProperties().getPropertyBoolean(
                "Debug." + getClass().getName().substring(1 + getClass().getName().lastIndexOf('.')),
                solver.getProperties().getPropertyBoolean("Debug.Criterion", false));
        return true;
    }
    
    /** Returns current model */
    public Model<V, T> getModel() { return iModel; }
    
    @Override
    public double getValue() {
        return iValue;
    }
    
    @Override
    public double getBest() {
        return iBest;
    }
    
    @Override
    public double getValue(Collection<V> variables) {
        double ret = 0;
        for (V v: variables) {
            T t = v.getAssignment();
            if (t != null) ret += getValue(t, null);
        }
        return ret;
    }

    
    @Override
    public double getWeight() {
        return iWeight;
    }
    
    @Override
    public double getWeightedBest() {
        return getWeight() == 0.0 ? 0.0 : getWeight() * getBest();
    }
    
    @Override
    public double getWeightedValue() {
        return (getWeight() == 0.0 ? 0.0 : getWeight() * getValue());
    }
    
    @Override
    public double getWeightedValue(T value, Set<T> conflicts) {
        return (getWeight() == 0.0 ? 0.0 : getWeight() * getValue(value, conflicts));
    }
    
    @Override
    public double getWeightedValue(Collection<V> variables) {
        return (getWeight() == 0.0 ? 0.0 : getWeight() * getValue(variables));
    }

    /** Compute bounds (bounds are being cached by default). */
    protected double[] computeBounds() {
        return getBounds(new ArrayList<V>(getModel().variables()));
    }

    @Override
    public double[] getBounds() {
        if (iBounds == null) iBounds = computeBounds();
        return (iBounds == null ? new double[] {0.0, 0.0} : iBounds);
    }

    @Override
    public double[] getBounds(Collection<V> variables) {
        double[] bounds = new double[] { 0.0, 0.0 };
        for (V v: variables) {
            Double min = null, max = null;
            for (T t: v.values()) {
                double value = getValue(t, null);
                if (min == null) { min = value; max = value; continue; }
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
            if (min != null) {
                bounds[0] += min;
                bounds[1] += max;
            }
        }
        return bounds;
    }

    @Override
    public void beforeAssigned(long iteration, T value) {
        switch (iValueUpdateType) {
            case AfterUnassignedBeforeAssigned:
            case BeforeUnassignedBeforeAssigned:
                iValue += getValue(value, null);
        }
    }

    @Override
    public void afterAssigned(long iteration, T value) {
        switch (iValueUpdateType) {
            case AfterUnassignedAfterAssigned:
            case BeforeUnassignedAfterAssigned:
                iValue += getValue(value, null);
        }
    }

    @Override
    public void beforeUnassigned(long iteration, T value) {
        switch (iValueUpdateType) {
            case BeforeUnassignedAfterAssigned:
            case BeforeUnassignedBeforeAssigned:
                iValue -= getValue(value, null);
        }
    }

    @Override
    public void afterUnassigned(long iteration, T value) {
        switch (iValueUpdateType) {
            case AfterUnassignedAfterAssigned:
            case AfterUnassignedBeforeAssigned:
                iValue -= getValue(value, null);
        }
    }

    @Override
    public void bestSaved() {
        iBest = iValue;
    }

    @Override
    public void bestRestored() {
        iValue = iBest;
    }
    
    @Override
    public void inc(double value) {
        iValue += value;
    }   

    @Override
    public String getName() {
        return getClass().getName().substring(1 + getClass().getName().lastIndexOf('.')).replaceAll("(?<=[^A-Z])([A-Z])"," $1");
    }
    
    /** Clear bounds cache */
    protected void clearCache() {
        iBounds = null;
    }
    
    @Override
    public void variableAdded(V variable) {
        clearCache();
    }
    @Override
    public void variableRemoved(V variable) {
        clearCache();
    }
    @Override
    public void constraintAdded(Constraint<V, T> constraint) {
        clearCache();
    }
    @Override
    public void constraintRemoved(Constraint<V, T> constraint) {
        clearCache();
    }
    
    protected String getPerc(double value, double min, double max) {
        if (max == min)
            return sPercentFormat.format(100.0);
        return sPercentFormat.format(100.0 - 100.0 * (value - min) / (max - min));
    }

    protected String getPercRev(double value, double min, double max) {
        if (max == min)
            return sPercentFormat.format(0.0);
        return sPercentFormat.format(100.0 * (value - min) / (max - min));
    }

    @Override
    public void getInfo(Map<String, String> info) {
        if (iDebug) {
            double val = getValue(), w = getWeightedValue(), prec = getValue(getModel().variables());
            double[] bounds = getBounds();
            if (bounds[0] <= val && val <= bounds[1] && bounds[0] < bounds[1])
                info.put("[C] " + getName(),
                        getPerc(val, bounds[0], bounds[1]) + "% (value: " + sDoubleFormat.format(val) +
                        (prec != val ? ", precise:" + sDoubleFormat.format(prec) : "") +
                        ", weighted:" + sDoubleFormat.format(w) +
                        ", bounds: " + sDoubleFormat.format(bounds[0]) + "&hellip;" + sDoubleFormat.format(bounds[1]) + ")");
            else if (bounds[1] <= val && val <= bounds[0] && bounds[1] < bounds[0])
                info.put("[C] " + getName(),
                        getPercRev(val, bounds[1], bounds[0]) + "% (value: " + sDoubleFormat.format(val) +
                        (prec != val ? ", precise:" + sDoubleFormat.format(prec) : "") +
                        ", weighted:" + sDoubleFormat.format(w) +
                        ", bounds: " + sDoubleFormat.format(bounds[1]) + "&hellip;" + sDoubleFormat.format(bounds[0]) + ")");
            else if (bounds[0] != val || val != bounds[1])
                info.put("[C] " + getName(),
                        sDoubleFormat.format(val) + " (" +
                        (prec != val ? "precise:" + sDoubleFormat.format(prec) + ", ": "") +
                        "weighted:" + sDoubleFormat.format(w) +
                        (bounds[0] != bounds[1] ? ", bounds: " + sDoubleFormat.format(bounds[0]) + "&hellip;" + sDoubleFormat.format(bounds[1]) : "") +
                        ")");
        }
    }
    
    @Override
    public void getInfo(Map<String, String> info, Collection<V> variables) {
        if (iDebug) {
            double val = getValue(variables), w = getWeightedValue(variables);
            double[] bounds = getBounds(variables);
            if (bounds[0] <= val && val <= bounds[1])
                info.put("[C] " + getName(),
                        getPerc(val, bounds[0], bounds[1]) + "% (value: " + sDoubleFormat.format(val) +
                        ", weighted:" + sDoubleFormat.format(w) +
                        ", bounds: " + sDoubleFormat.format(bounds[0]) + "&hellip;" + sDoubleFormat.format(bounds[1]) + ")");
            else if (bounds[1] <= val && val <= bounds[0])
                info.put("[C] " + getName(),
                        getPercRev(val, bounds[1], bounds[0]) + "% (value: " + sDoubleFormat.format(val) +
                        ", weighted:" + sDoubleFormat.format(w) +
                        ", bounds: " + sDoubleFormat.format(bounds[1]) + "&hellip;" + sDoubleFormat.format(bounds[0]) + ")");
            else if (bounds[0] != val || val != bounds[1])
                info.put("[C] " + getName(),
                        sDoubleFormat.format(val) + " (weighted:" + sDoubleFormat.format(w) +
                        (bounds[0] != bounds[1] ? ", bounds: " + sDoubleFormat.format(bounds[0]) + "&hellip;" + sDoubleFormat.format(bounds[1]) : "") +
                        ")");
        }
    }
}
