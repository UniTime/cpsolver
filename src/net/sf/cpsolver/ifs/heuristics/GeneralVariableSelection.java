package net.sf.cpsolver.ifs.heuristics;


import java.util.*;

import net.sf.cpsolver.ifs.extension.*;
import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.solution.*;
import net.sf.cpsolver.ifs.solver.*;
import net.sf.cpsolver.ifs.util.*;

/**
 * General implementation of variable selection criterion.
 * <br><br>
 * In case that all variables are assigned, one of the variables is selected randomly. In case of MPP, 
 * the random selection is made among the variables which have not assigned initial values.
 * <br><br>
 * When there are unassigned variables, a variable is selected randomly among all 
 * unassigned variables (when Variable.RandomSelection is true) or the following roulette 
 * wheel selection takes place (MPP):<ul>
 * <li> one point for a variable with no initial assignment
 * <li> 3 * ( 1 + number of conflicts with the initial assignment) for a variable with an initial assignment
 * </ul>
 * <br>
 * If {@link MacPropagation} is used and Variable.UnassignWhenNoGood parameter is true, while
 * there is a variable with an empty domain: <ul>
 * <li> with Variable.UnassignWhenNoGoodRandomWalk probabilty an arbitrary assigned variable is selected
 * <li> otherwise, one variable with empty domain is picked, one of its original values is picked and 
 * one of the variables from the explanation of that value is then returned. If the explanation is 
 * empty, another variable and value is tried (up to ten times).
 * </ul>
 * <br>
 * Parameters:
 * <br>
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>Variable.RandomSelection</td><td>{@link Boolean}</td><td>if true, an unassigned variable is picked randomly</td></tr>
 * <tr><td>Variable.UnassignWhenNoGood</td><td>{@link Boolean}</td><td>if true and if {@link MacPropagation} is used: if there is a variable with empty domain, assigned variable (which is present in some explanation for a vairable with empty domain) is selected (for reassignment)</td></tr>
 * <tr><td>Variable.UnassignWhenNoGoodRandomWalk</td><td>{@link Double}</td><td>if Variable.UnassignWhenNoGood is true and if {@link MacPropagation} is used: if there is a variable with empty domain, with the given probability an arbitrary assigned variable is selected</td></tr>
 * </table>
 *
 * @see VariableSelection
 * @see Solver
 *
 * @version
 * IFS 1.1 (Iterative Forward Search)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 **/
public class GeneralVariableSelection implements VariableSelection {
    private boolean iUnassignWhenNotGood = false;
    private double iUnassignWhenNotGoodRandWalk = 0.02;
    private boolean iRandomSelection = true;
    
    private MacPropagation iProp = null;
    
    /** Constructor
     * @param properties input configuration
     */
    public GeneralVariableSelection(DataProperties properties) {
        iUnassignWhenNotGood = properties.getPropertyBoolean("Variable.UnassignWhenNoGood", iUnassignWhenNotGood);
        iUnassignWhenNotGoodRandWalk = properties.getPropertyDouble("Variable.UnassignWhenNoGoodRandomWalk", iUnassignWhenNotGoodRandWalk);
        iRandomSelection = properties.getPropertyBoolean("Variable.RandomSelection", iRandomSelection);
    }
    
    public GeneralVariableSelection() {
    }

    /** Initialization */
    public void init(Solver solver) {
        for (Enumeration i = solver.getExtensions().elements(); i.hasMoreElements(); ) {
            Extension extension = (Extension)i.nextElement();
            if (extension instanceof MacPropagation) iProp = (MacPropagation)extension;
        }
    }

    /** Variable selection */
    public Variable selectVariable(Solution solution) {
        if (solution.getModel().nrUnassignedVariables()==0) {
            if (!solution.getModel().perturbVariables().isEmpty())
                return (Variable)ToolBox.random(solution.getModel().perturbVariables());
            else
                return (Variable)ToolBox.random(solution.getModel().assignedVariables());
        } else {
            if (iProp != null && iUnassignWhenNotGood) {
                Vector noGoodVariables = new FastVector();
                for (Enumeration i1 = solution.getModel().unassignedVariables().elements(); i1.hasMoreElements();) {
                    Variable variable = (Variable)i1.nextElement();
                    if (iProp.goodValues(variable).isEmpty())
                        noGoodVariables.addElement(variable);
                }
                if (!noGoodVariables.isEmpty()) {
                    if (ToolBox.random() < iUnassignWhenNotGoodRandWalk)
                        return (Variable)ToolBox.random(solution.getModel().assignedVariables());
                    for (int attempt = 0; attempt < 10; attempt++) {
                        Variable noGoodVariable = (Variable)ToolBox.random(noGoodVariables);
                        Value noGoodValue = (Value)ToolBox.random(noGoodVariable.values());
                        Set noGood = iProp.noGood(noGoodValue);
                        if (noGood!=null && !noGood.isEmpty())
                            return ((Value)ToolBox.random(noGood)).variable();
                    }
                }
            }
            if (iRandomSelection)
                return (Variable)ToolBox.random(solution.getModel().unassignedVariables());
            Vector points = new FastVector();
            int totalPoints = 0;
            for (Enumeration i = solution.getModel().unassignedVariables().elements(); i.hasMoreElements(); ) {
                Variable variable = (Variable)i.nextElement();
                int pointsThisVariable = (variable.getInitialAssignment()!=null ? 3*(1+solution.getModel().conflictValues(variable.getInitialAssignment()).size()):1);
                totalPoints += pointsThisVariable;
                points.addElement(new Integer(totalPoints));
            }
            int rndPoints = ToolBox.random(totalPoints);
            Enumeration x = solution.getModel().unassignedVariables().elements();
            for (int i = 0; x.hasMoreElements() && i < points.size(); i++) {
                Variable variable = (Variable)x.nextElement();
                int tp = ((Integer)points.elementAt(i)).intValue();
                if (tp > rndPoints) return variable;
            }
            return (Variable)ToolBox.random(solution.getModel().unassignedVariables());
        }
    }
    
}
