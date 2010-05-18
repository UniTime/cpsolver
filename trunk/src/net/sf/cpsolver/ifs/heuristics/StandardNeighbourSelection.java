package net.sf.cpsolver.ifs.heuristics;

import java.lang.reflect.Constructor;

import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.SimpleNeighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.solver.SolverListener;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Standard neighbour selection criterion. <br>
 * <br>
 * This criterion is using the provided variable and value selection criteria.
 * In each step, a variable is selected first using the
 * {@link VariableSelection}. Then, a value is selected to the selected
 * variable, using the {@link ValueSelection}. A {@link SimpleNeighbour}
 * containing the selected value is returned. <br>
 * <br>
 * Note: the use of neighbour select criteria extends the former implementation
 * of the IFS algorithm which was only able to use variable and value selection
 * criteria and therefore only one value was assigned in each iteration. <br>
 * <br>
 * Parameters: <br>
 * <table border='1'>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>Value.Class</td>
 * <td>{@link String}</td>
 * <td>Fully qualified class name of the value selection criterion (see
 * {@link ValueSelection}, e.g. {@link GeneralValueSelection})</td>
 * </tr>
 * <tr>
 * <td>Variable.Class</td>
 * <td>{@link String}</td>
 * <td>Fully qualified class name of the variable selection criterion (see
 * {@link VariableSelection}, e.g. {@link GeneralVariableSelection})</td>
 * </tr>
 * </table>
 * 
 * @see Solver
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 **/
public class StandardNeighbourSelection<V extends Variable<V, T>, T extends Value<V, T>> implements
        NeighbourSelection<V, T> {
    protected static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger
            .getLogger(StandardNeighbourSelection.class);

    private ValueSelection<V, T> iValueSelection = null;
    private VariableSelection<V, T> iVariableSelection = null;
    private Solver<V, T> iSolver = null;

    /** Sets value selection criterion */
    public void setValueSelection(ValueSelection<V, T> valueSelection) {
        iValueSelection = valueSelection;
    }

    /** Sets variable selection criterion */
    public void setVariableSelection(VariableSelection<V, T> variableSelection) {
        iVariableSelection = variableSelection;
    }

    /** Returns values selection criterion */
    public ValueSelection<V, T> getValueSelection() {
        return iValueSelection;
    }

    /** Returns variable selection criterion */
    public VariableSelection<V, T> getVariableSelection() {
        return iVariableSelection;
    }

    /**
     * Constructor
     * 
     * @param properties
     *            configuration
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public StandardNeighbourSelection(DataProperties properties) throws Exception {
        String valueSelectionClassName = properties.getProperty("Value.Class",
                "net.sf.cpsolver.ifs.heuristics.GeneralValueSelection");
        sLogger.info("Using " + valueSelectionClassName);
        Class<?> valueSelectionClass = Class.forName(valueSelectionClassName);
        Constructor<?> valueSelectionConstructor = valueSelectionClass
                .getConstructor(new Class[] { DataProperties.class });
        setValueSelection((ValueSelection<V, T>) valueSelectionConstructor.newInstance(new Object[] { properties }));

        String variableSelectionClassName = properties.getProperty("Variable.Class",
                "net.sf.cpsolver.ifs.heuristics.GeneralVariableSelection");
        sLogger.info("Using " + variableSelectionClassName);
        Class<?> variableSelectionClass = Class.forName(variableSelectionClassName);
        Constructor<?> variableSelectionConstructor = variableSelectionClass
                .getConstructor(new Class[] { DataProperties.class });
        setVariableSelection((VariableSelection<V, T>) variableSelectionConstructor
                .newInstance(new Object[] { properties }));
    }

    /**
     * Initialization -- methods
     * {@link net.sf.cpsolver.ifs.heuristics.VariableSelection#init(Solver)} and
     * {@link net.sf.cpsolver.ifs.heuristics.ValueSelection#init(Solver)} are
     * called.
     */
    public void init(Solver<V, T> solver) {
        getValueSelection().init(solver);
        getVariableSelection().init(solver);
        iSolver = solver;
    }

    /** Use the provided variable selection criterion to select a variable */
    public V selectVariable(Solution<V, T> solution) {
        // Variable selection
        V variable = getVariableSelection().selectVariable(solution);
        for (SolverListener<V, T> listener : iSolver.getSolverListeners())
            if (!listener.variableSelected(solution.getIteration(), variable))
                return null;
        if (variable == null)
            sLogger.debug("No variable selected.");

        if (variable != null && !variable.hasValues()) {
            sLogger.debug("Variable " + variable.getName() + " has no values.");
            return null;
        }
        return variable;
    }

    /**
     * Use the provided value selection criterion to select a value to the
     * selected variable
     */
    public T selectValue(Solution<V, T> solution, V variable) {
        // Value selection
        T value = getValueSelection().selectValue(solution, variable);
        for (SolverListener<V, T> listener : iSolver.getSolverListeners())
            if (!listener.valueSelected(solution.getIteration(), variable, value))
                return null;

        if (value == null) {
            sLogger.debug("No value selected for variable " + variable + ".");
        }
        return value;
    }

    /**
     * Select neighbour. A value is selected to the selected variable.
     */
    public Neighbour<V, T> selectNeighbour(Solution<V, T> solution) {
        V variable = selectVariable(solution);
        if (variable == null)
            return null;
        T value = selectValue(solution, variable);
        if (value == null)
            return null;
        return new SimpleNeighbour<V, T>(variable, value);
    }
}
