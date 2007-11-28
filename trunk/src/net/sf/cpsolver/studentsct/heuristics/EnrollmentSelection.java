package net.sf.cpsolver.studentsct.heuristics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import net.sf.cpsolver.ifs.extension.ConflictStatistics;
import net.sf.cpsolver.ifs.extension.Extension;
import net.sf.cpsolver.ifs.extension.MacPropagation;
import net.sf.cpsolver.ifs.extension.ViolatedInitials;
import net.sf.cpsolver.ifs.heuristics.GeneralValueSelection;
import net.sf.cpsolver.ifs.heuristics.ValueSelection;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.FastVector;
import net.sf.cpsolver.ifs.util.ToolBox;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * Enrollment selection criterion. It is similar to {@link GeneralValueSelection}, 
 * however, it is not allowed to assign a enrollment to a dummy student {@link Student#isDummy()}
 * that is conflicting with an enrollment of a real student.
 * 
 * @version
 * StudentSct 1.1 (Student Sectioning)<br>
 * Copyright (C) 2007 Tomas Muller<br>
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
public class EnrollmentSelection implements ValueSelection {
    private double iRandomWalkProb = 0.0;
    private double iInitialSelectionProb = 0.0;
    private double iGoodSelectionProb = 0.0;
    private int iMPPLimit = -1;
    
    private double iWeightDeltaInitialAssignment = 0.0;
    private double iWeightPotentialConflicts = 0.0;
    private double iWeightWeightedCoflicts = 0.0;
    private double iWeightCoflicts = 1.0;
    private double iWeightNrAssignments = 0.5;
    private double iWeightValue = 0.0;
    
    protected int iTabuSize = 0;
    protected ArrayList iTabu = null;
    protected int iTabuPos = 0;
    
    private boolean iMPP = false;
    private ConflictStatistics iStat = null;
    private MacPropagation iProp = null;
    private ViolatedInitials iViolatedInitials = null;
    
    public EnrollmentSelection() {}
    
    /** Constructor
     * @param properties input configuration
     */
    public EnrollmentSelection(DataProperties properties) {
        iMPP = properties.getPropertyBoolean("General.MPP", false);
        if (iMPP) {
            iMPPLimit = properties.getPropertyInt("Value.MPPLimit", -1);
            iInitialSelectionProb = properties.getPropertyDouble("Value.InitialSelectionProb", 0.75);
            iWeightDeltaInitialAssignment = properties.getPropertyDouble("Value.WeightDeltaInitialAssignments", 0.0);
        }
        iGoodSelectionProb = properties.getPropertyDouble("Value.GoodSelectionProb", 0.00);
        iWeightWeightedCoflicts = properties.getPropertyDouble("Value.WeightWeightedConflicts", 1.0);
        iWeightPotentialConflicts = properties.getPropertyDouble("Value.WeightPotentialConflicts", 0.0);
        
        iRandomWalkProb = properties.getPropertyDouble("Value.RandomWalkProb", 0.0);
        iWeightCoflicts = properties.getPropertyDouble("Value.WeightConflicts", 1.0);
        iWeightNrAssignments = properties.getPropertyDouble("Value.WeightNrAssignments", 0.5);
        iWeightValue = properties.getPropertyDouble("Value.WeightValue", 0.0);
        iTabuSize = properties.getPropertyInt("Value.Tabu", 0);
        if (iTabuSize > 0)
            iTabu = new ArrayList(iTabuSize);
    }
    
    /** Initialization */
    public void init(Solver solver) {
        for (Enumeration i = solver.getExtensions().elements(); i.hasMoreElements();) {
            Extension extension = (Extension)i.nextElement();
            if (extension instanceof ConflictStatistics)
                iStat = (ConflictStatistics)extension;
            if (extension instanceof MacPropagation)
                iProp = (MacPropagation)extension;
            if (extension instanceof ViolatedInitials)
                iViolatedInitials = (ViolatedInitials)extension;
        }
    }
    
    /** true, if it is allowed to assign given value */
    public boolean isAllowed(Value value) {
        return isAllowed(value, null);
    }
    
    /** true, if it is allowed to assign given value */
    public boolean isAllowed(Value value, Collection conflicts) {
        if (value==null) return true;
        StudentSectioningModel model = (StudentSectioningModel)value.variable().getModel();
        if (model.getNrLastLikeRequests(false)==0 || model.getNrRealRequests(false)==0) return true;
        Request request = (Request)value.variable();
        if (!request.getStudent().isDummy()) return true;
        if (conflicts==null) conflicts = value.variable().getModel().conflictValues(value);
        for (Iterator i=conflicts.iterator();i.hasNext();) {
            Enrollment conflict = (Enrollment)i.next();
            if (!conflict.getRequest().getStudent().isDummy()) return false;
        }
        return true;
    }
    
    /** Value selecion */
    public Value selectValue(Solution solution, Variable selectedVariable) {
        if (iMPP) {
            if (selectedVariable.getInitialAssignment() != null) {
                if (solution.getModel().unassignedVariables().isEmpty()) {
                    if (solution.getModel().perturbVariables().size() <= iMPPLimit)
                        iMPPLimit = solution.getModel().perturbVariables().size() - 1;
                }
                if (iMPPLimit >= 0 && solution.getModel().perturbVariables().size() > iMPPLimit) {
                    if (isAllowed(selectedVariable.getInitialAssignment())) return selectedVariable.getInitialAssignment();
                }
                if (selectedVariable.getInitialAssignment() != null && ToolBox.random() <= iInitialSelectionProb) {
                    if (isAllowed(selectedVariable.getInitialAssignment())) return selectedVariable.getInitialAssignment();
                }
            }
        }
        
        Vector values = selectedVariable.values();
        if (ToolBox.random() <= iRandomWalkProb) {
            Value value = (Value)ToolBox.random(values);
            if (isAllowed(value)) return value; 
        }
        if (iProp != null && selectedVariable.getAssignment() == null && ToolBox.random() <= iGoodSelectionProb) {
            Collection goodValues = iProp.goodValues(selectedVariable);
            if (!goodValues.isEmpty()) values = new FastVector(goodValues);
        }
        if (values.size() == 1) {
            Value value = (Value)values.firstElement();
            if (isAllowed(value)) return value; 
            else return null;
        }
        
        Vector bestValues = null;
        double bestWeightedSum = 0;
        
        for (Enumeration i1 = values.elements(); i1.hasMoreElements();) {
            Value value = (Value)i1.nextElement();
            if (iTabu != null && iTabu.contains(value)) continue;
            if (selectedVariable.getAssignment() != null && selectedVariable.getAssignment().equals(value))
                continue;
            
            Collection conf = solution.getModel().conflictValues(value);
            if (conf.contains(value)) continue;
            
            if (!isAllowed(value, conf)) continue;
            
            double weightedConflicts = (iStat == null || iWeightWeightedCoflicts == 0.0 ? 0.0 : iStat.countRemovals(solution.getIteration(), conf, value));
            double potentialConflicts = (iStat == null || iWeightPotentialConflicts == 0.0 ? 0.0 : iStat.countPotentialConflicts(solution.getIteration(), value, 3));
            
            long deltaInitialAssignments = 0;
            if (iMPP && iWeightDeltaInitialAssignment != 0.0) {
                if (iViolatedInitials != null) {
                    Set violations = iViolatedInitials.getViolatedInitials(value);
                    if (violations != null) {
                        for (Iterator it1 = violations.iterator(); it1.hasNext();) {
                            Value aValue = (Value)it1.next();
                            if (aValue.variable().getAssignment() == null || aValue.variable().getAssignment().equals( aValue))
                                deltaInitialAssignments += 2;
                        }
                    }
                }
                for (Iterator it1 = conf.iterator(); it1.hasNext();) {
                    Value aValue = (Value)it1.next();
                    if (aValue.variable().getInitialAssignment() != null)
                        deltaInitialAssignments--;
                }
                if (selectedVariable.getInitialAssignment() != null && !selectedVariable.getInitialAssignment().equals(value)) {
                    deltaInitialAssignments++;
                }
                if (iMPPLimit >= 0 && (solution.getModel().perturbVariables().size() + deltaInitialAssignments) > iMPPLimit)
                    continue;
            }
            
            double weightedSum =
                    (iWeightDeltaInitialAssignment * deltaInitialAssignments)
                    + (iWeightPotentialConflicts * potentialConflicts)
                    + (iWeightWeightedCoflicts * weightedConflicts)
                    + (iWeightCoflicts * conf.size())
                    + (iWeightNrAssignments * value.countAssignments())
                    + (iWeightValue * value.toDouble());
            
            if (bestValues == null || bestWeightedSum > weightedSum) {
                bestWeightedSum = weightedSum;
                if (bestValues == null) bestValues = new FastVector();
                else bestValues.clear();
                bestValues.addElement(value);
            } else {
                if (bestWeightedSum == weightedSum)
                    bestValues.addElement(value);
            }
        }

        Value selectedValue = (Value)(bestValues==null?null:ToolBox.random(bestValues));
        if (selectedValue == null) selectedValue = (Value)ToolBox.random(values);
        if (iTabu != null) {
            if (iTabu.size() == iTabuPos)
                iTabu.add(selectedValue);
            else
                iTabu.set(iTabuPos, selectedValue);
            iTabuPos = (iTabuPos + 1) % iTabuSize;
        }
        return (bestValues == null ? null : selectedValue);
    }
    

}
