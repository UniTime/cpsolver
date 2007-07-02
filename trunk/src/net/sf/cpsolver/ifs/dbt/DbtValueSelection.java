package net.sf.cpsolver.ifs.dbt;


import java.util.*;

import net.sf.cpsolver.ifs.extension.*;
import net.sf.cpsolver.ifs.heuristics.*;
import net.sf.cpsolver.ifs.model.*;
import net.sf.cpsolver.ifs.solution.*;
import net.sf.cpsolver.ifs.solver.*;
import net.sf.cpsolver.ifs.util.*;

/**
 * Selection of a value for dynamic backtracking.
 * <br><br>
 * <li>Returns null if all values of the selected variable are nogood.
 * <li>Selected the best good value (according to the parameters) of the selected variable.
 * <br><br>
 * It is based on a weighted sum of several criteria.
 * <br><br>
 * This IFS solver value selection heuristics is to be used only in case of dynamic backtracking and it has the following parameters:
 * <br>
 * <table border='1'><tr><th>Parameter</th><th>Type</th><th>Comment</th></tr>
 * <tr><td>General.MPP</td><td>{@link Boolean}</td><td>Minimal Perturbation Problem</td></tr>
 * <tr><td>Value.MPPLimit</td><td>{@link Integer}</td><td>Limit on the number of perturbations (only in case of MPP, i.e., when General.MPP=true). MPP limit is decreased when a complete solution is found. If set to -1, it is no used</td></tr>
 * <tr><td>Value.InitialSelectionProb</td><td>{@link Double}</td><td>Probability of selection of initial value (only in case of MPP)</td></tr>
 * <tr><td>Value.WeightDeltaInitialAssignments</td><td>{@link Double}</td><td>Weight of difference in the number of assignments of initial values in case of selection of the value(only in case of MPP)</td></tr>
 * <tr><td>Value.RandomWalkProb</td><td>{@link Double}</td><td>Probability of random selection of a good value</td></tr>
 * <tr><td>Value.WeightNrAssignments</td><td>{@link Double}</td><td>Weight of the number of previous assignments of the value</td></tr>
 * <tr><td>Value.WeightValue</td><td>{@link Double}</td><td>Weight of the value itself (e.g., for minCSP)</td></tr>
 * </table>
 * <br>
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
 */
public class DbtValueSelection implements ValueSelection {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(GeneralValueSelection.class);
    private double iRandomWalkProb = 0.0;
    private double iInitialSelectionProb = 0.0;
    private int    iMPPLimit = -1;
    
    private double iWeightDeltaInitialAssignment = 0.0;
    private double iWeightNrAssignments = 0.5;
    private double iWeightValue = 0.0;
    
    private boolean iMPP = false;
    private DbtPropagation iProp = null;
    private ViolatedInitials iViolatedInitials = null;
    
    public DbtValueSelection(DataProperties properties) {
        iMPP = properties.getPropertyBoolean("General.MPP", false);
        
        if (iMPP) {
            iMPPLimit = properties.getPropertyInt("Value.MPPLimit", -1);
            iInitialSelectionProb = properties.getPropertyDouble("Value.InitialSelectionProb",0.75);
            iWeightDeltaInitialAssignment = properties.getPropertyDouble("Value.WeightDeltaInitialAssignments",0.0);
        }
        
        iRandomWalkProb = properties.getPropertyDouble("Value.RandomWalkProb",0.0);
        iWeightNrAssignments = properties.getPropertyDouble("Value.WeightNrAssignments",0.5);
        iWeightValue = properties.getPropertyDouble("Value.WeightValue", 0.0);
    }
    
    /** 
     * Heuristics initialization
     *
     * @see ValueSelection#init(Solver)
     */
    public void init(Solver solver) {
        for (Enumeration i = solver.getExtensions().elements(); i.hasMoreElements();) {
            Extension extension = (Extension) i.nextElement();
            
            if (extension instanceof DbtPropagation) {
                iProp = (DbtPropagation) extension;
            }
            if (extension instanceof ViolatedInitials) {
                iViolatedInitials = (ViolatedInitials) extension;
            }
        }
    }
    
    /** 
     * Value selection
     *
     * @see ValueSelection#selectValue(Solution, Variable)
     */
    public Value selectValue(Solution solution, Variable selectedVariable) {
        Vector values = null;
        
        if (iProp != null) {
            values = new FastVector(iProp.goodValues(selectedVariable).size());
            for (Enumeration i1 = selectedVariable.values().elements(); i1.hasMoreElements();) {
                Value value = (Value) i1.nextElement();
                
                if (!iProp.isGood(value)) {
                    continue;
                }
                Collection conf = solution.getModel().conflictValues(value);
                
                if (!conf.isEmpty()) {
                    HashSet noGood = new HashSet(2 * conf.size());
                    
                    for (Iterator i2 = conf.iterator(); i2.hasNext();) {
                        noGood.add((Value) i2.next());
                    }
                    iProp.setNoGood(value, noGood);
                    sLogger.debug(value+" become nogood ("+noGood+")");
                } else {
                    if (!solution.isBestComplete() || solution.getBestValue()> solution.getModel().getTotalValue()+value.toDouble()) {
                        values.add(value);
                    }
                }
            }
        } else {
            values = new FastVector(selectedVariable.values().size());
            for (Enumeration i1 = selectedVariable.values().elements(); i1.hasMoreElements();) {
                Value value = (Value) i1.nextElement();
                
                if (solution.getModel().conflictValues(value).isEmpty()) {
                    if (solution.isBestComplete() && solution.getBestValue()>solution.getModel().getTotalValue()+value.toDouble()) {
                        values.add(value);
                    }
                }
            }
        }
        if (values.isEmpty()) {
            return null;
        }
        
        if (iMPP) {
            if (iMPPLimit>=0 && solution.isBestComplete() && solution.getBestPertirbations()>=0 && solution.getBestPertirbations() <= iMPPLimit) {
                iMPPLimit = solution.getBestPertirbations() - 1;
                sLogger.debug("MPP Limit decreased to "+iMPPLimit);
            }
            
            int nrPerts = solution.getModel().perturbVariables().size();
            
            if (iMPPLimit>=0 && iMPPLimit < nrPerts) {
                return null;
            }
            if (iMPPLimit>=0 && iMPPLimit==nrPerts && selectedVariable.getInitialAssignment() != null) {
                if (values.contains(selectedVariable.getInitialAssignment())) {
                    return selectedVariable.getInitialAssignment();
                } else {
                    return null;
                }
            }
            
            if (selectedVariable.getInitialAssignment() != null && ToolBox.random() <= iInitialSelectionProb) {
                if (values.contains(selectedVariable.getInitialAssignment())) {
                    return selectedVariable.getInitialAssignment();
                }
            }
        }
        
        if (values.size()==1) {
            return (Value) values.firstElement();
        }
        
        if (ToolBox.random() <= iRandomWalkProb) {
            return (Value) ToolBox.random(values);
        }
        
        Vector bestValues = null;
        double bestWeightedSum = 0;
        
        if (iWeightDeltaInitialAssignment==0.0 && iWeightNrAssignments==0.0 && iWeightValue==0.0) {
            return (Value) ToolBox.random(values);
        }
        
        for (Enumeration i1 = values.elements(); i1.hasMoreElements();) {
            Value value = (Value) i1.nextElement();
            
            long deltaInitialAssignments = 0;
            
            if (iWeightDeltaInitialAssignment != 0.0) {
                if (iViolatedInitials != null) {
                    Set violations = iViolatedInitials.getViolatedInitials(value);
                    
                    if (violations != null) {
                        for (Iterator it1 = violations.iterator(); it1.hasNext();) {
                            Value aValue = (Value) it1.next();
                            
                            if (aValue.variable().getAssignment()==null || aValue.variable().getAssignment().equals(aValue)) {
                                deltaInitialAssignments += 2;
                            }
                        }
                    }
                }
                if (selectedVariable.getInitialAssignment() != null && !selectedVariable.getInitialAssignment().equals(value)) {
                    deltaInitialAssignments++;
                }
                if (iMPPLimit>=0 && (solution.getModel().perturbVariables().size()+deltaInitialAssignments)>iMPPLimit) {
                    continue;
                }
            }
            
            double weightedSum = 
                  (iWeightDeltaInitialAssignment * deltaInitialAssignments)
                + (iWeightNrAssignments * value.countAssignments())
                + (iWeightValue * value.toDouble());
            
            if (bestValues==null || bestWeightedSum>weightedSum) {
                bestWeightedSum = weightedSum;
                if (bestValues==null) {
                    bestValues = new FastVector();
                } else {
                    bestValues.clear();
                }
                bestValues.addElement(value);
            } else if (bestWeightedSum==weightedSum) {
                bestValues.addElement(value);
            }
        }
        return (bestValues==null ? null : (Value) ToolBox.random(bestValues));
    }
}
