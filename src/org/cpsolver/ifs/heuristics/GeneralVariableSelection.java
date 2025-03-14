package org.cpsolver.ifs.heuristics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.cpsolver.ifs.extension.Extension;
import org.cpsolver.ifs.extension.MacPropagation;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;


/**
 * General implementation of variable selection criterion. <br>
 * <br>
 * In case that all variables are assigned, one of the variables is selected
 * randomly. In case of MPP, the random selection is made among the variables
 * which have not assigned initial values. <br>
 * <br>
 * When there are unassigned variables, a variable is selected randomly among
 * all unassigned variables (when Variable.RandomSelection is true) or the
 * following roulette wheel selection takes place (MPP):
 * <ul>
 * <li>one point for a variable with no initial assignment
 * <li>3 * ( 1 + number of conflicts with the initial assignment) for a variable
 * with an initial assignment
 * </ul>
 * <br>
 * If {@link MacPropagation} is used and Variable.UnassignWhenNoGood parameter
 * is true, while there is a variable with an empty domain:
 * <ul>
 * <li>with Variable.UnassignWhenNoGoodRandomWalk probabilty an arbitrary
 * assigned variable is selected
 * <li>otherwise, one variable with empty domain is picked, one of its original
 * values is picked and one of the variables from the explanation of that value
 * is then returned. If the explanation is empty, another variable and value is
 * tried (up to ten times).
 * </ul>
 * <br>
 * Parameters: <br>
 * <table border='1'><caption>Related Solver Parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>Variable.RandomSelection</td>
 * <td>{@link Boolean}</td>
 * <td>if true, an unassigned variable is picked randomly</td>
 * </tr>
 * <tr>
 * <td>Variable.UnassignWhenNoGood</td>
 * <td>{@link Boolean}</td>
 * <td>if true and if {@link MacPropagation} is used: if there is a variable
 * with empty domain, assigned variable (which is present in some explanation
 * for a vairable with empty domain) is selected (for reassignment)</td>
 * </tr>
 * <tr>
 * <td>Variable.UnassignWhenNoGoodRandomWalk</td>
 * <td>{@link Double}</td>
 * <td>if Variable.UnassignWhenNoGood is true and if {@link MacPropagation} is
 * used: if there is a variable with empty domain, with the given probability an
 * arbitrary assigned variable is selected</td>
 * </tr>
 * </table>
 * 
 * @see VariableSelection
 * @see Solver
 * 
 * @author  Tomas Muller
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
 *          License along with this library; if not see <a href='http://www.gnu.org/licenses'>http://www.gnu.org/licenses</a>.
 *
 * @param <V> Variable
 * @param <T> Value
 **/
public class GeneralVariableSelection<V extends Variable<V, T>, T extends Value<V, T>> implements VariableSelection<V, T> {
    private boolean iUnassignWhenNotGood = false;
    private double iUnassignWhenNotGoodRandWalk = 0.02;
    private boolean iRandomSelection = true;

    private MacPropagation<V, T> iProp = null;

    /**
     * Constructor
     * 
     * @param properties
     *            input configuration
     */
    public GeneralVariableSelection(DataProperties properties) {
        iUnassignWhenNotGood = properties.getPropertyBoolean("Variable.UnassignWhenNoGood", iUnassignWhenNotGood);
        iUnassignWhenNotGoodRandWalk = properties.getPropertyDouble("Variable.UnassignWhenNoGoodRandomWalk",
                iUnassignWhenNotGoodRandWalk);
        iRandomSelection = properties.getPropertyBoolean("Variable.RandomSelection", iRandomSelection);
    }

    public GeneralVariableSelection() {
    }

    /** Initialization */
    @Override
    public void init(Solver<V, T> solver) {
        for (Extension<V, T> extension : solver.getExtensions()) {
            if (MacPropagation.class.isInstance(extension))
                iProp = (MacPropagation<V, T>) extension;
        }
    }

    /** Variable selection */
    @Override
    public V selectVariable(Solution<V, T> solution) {
        if (solution.getModel().variables().size() == solution.getAssignment().nrAssignedVariables()) {
            if (!solution.getModel().perturbVariables(solution.getAssignment()).isEmpty())
                return ToolBox.random(solution.getModel().perturbVariables(solution.getAssignment()));
            else
                return ToolBox.random(solution.getAssignment().assignedVariables());
        } else {
            if (iProp != null && iUnassignWhenNotGood) {
                List<V> noGoodVariables = new ArrayList<V>();
                for (V variable : solution.getAssignment().unassignedVariables(solution.getModel())) {
                    if (iProp.goodValues(solution.getAssignment(), variable).isEmpty())
                        noGoodVariables.add(variable);
                }
                if (!noGoodVariables.isEmpty()) {
                    if (ToolBox.random() < iUnassignWhenNotGoodRandWalk)
                        return ToolBox.random(solution.getAssignment().assignedVariables());
                    for (int attempt = 0; attempt < 10; attempt++) {
                        V noGoodVariable = ToolBox.random(noGoodVariables);
                        T noGoodValue = ToolBox.random(noGoodVariable.values(solution.getAssignment()));
                        Set<T> noGood = iProp.noGood(solution.getAssignment(), noGoodValue);
                        if (noGood != null && !noGood.isEmpty())
                            return ToolBox.random(noGood).variable();
                    }
                }
            }
            if (iRandomSelection)
                return ToolBox.random(solution.getAssignment().unassignedVariables(solution.getModel()));
            List<Integer> points = new ArrayList<Integer>();
            int totalPoints = 0;
            for (V variable : solution.getAssignment().unassignedVariables(solution.getModel())) {
                int pointsThisVariable = (variable.getInitialAssignment() != null ? 3 * (1 + solution.getModel().conflictValues(solution.getAssignment(), variable.getInitialAssignment()).size()) : 1);
                totalPoints += pointsThisVariable;
                points.add(totalPoints);
            }
            int rndPoints = ToolBox.random(totalPoints);
            Iterator<V> x = solution.getAssignment().unassignedVariables(solution.getModel()).iterator();
            for (int i = 0; x.hasNext() && i < points.size(); i++) {
                V variable = x.next();
                int tp = points.get(i);
                if (tp > rndPoints)
                    return variable;
            }
            return ToolBox.random(solution.getAssignment().unassignedVariables(solution.getModel()));
        }
    }

}
