package org.cpsolver.studentsct.heuristics;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.extension.ConflictStatistics;
import org.cpsolver.ifs.extension.Extension;
import org.cpsolver.ifs.extension.MacPropagation;
import org.cpsolver.ifs.extension.ViolatedInitials;
import org.cpsolver.ifs.heuristics.GeneralValueSelection;
import org.cpsolver.ifs.heuristics.ValueSelection;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Student;


/**
 * Enrollment selection criterion. It is similar to
 * {@link GeneralValueSelection}, however, it is not allowed to assign a
 * enrollment to a dummy student {@link Student#isDummy()} that is conflicting
 * with an enrollment of a real student.
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
 */
public class EnrollmentSelection implements ValueSelection<Request, Enrollment> {
    private double iRandomWalkProb = 0.0;
    private double iInitialSelectionProb = 0.0;
    private double iGoodSelectionProb = 0.0;
    private int iMPPLimit = -1;

    private double iWeightDeltaInitialAssignment = 0.0;
    private double iWeightPotentialConflicts = 0.0;
    private double iWeightWeightedCoflicts = 0.0;
    private double iWeightCoflicts = 1.0;
    private double iWeightValue = 0.0;

    protected int iTabuSize = 0;
    protected List<Enrollment> iTabu = null;
    protected int iTabuPos = 0;

    private boolean iMPP = false;
    private ConflictStatistics<Request, Enrollment> iStat = null;
    private MacPropagation<Request, Enrollment> iProp = null;
    private ViolatedInitials<Request, Enrollment> iViolatedInitials = null;

    public EnrollmentSelection() {
    }

    /**
     * Constructor
     * 
     * @param properties
     *            input configuration
     */
    public EnrollmentSelection(DataProperties properties) {
        iMPP = properties.getPropertyBoolean("General.MPP", false);
        if (iMPP) {
            iMPPLimit = properties.getPropertyInt("Value.MPPLimit", -1);
            iInitialSelectionProb = properties.getPropertyDouble("Value.InitialSelectionProb", 0.75);
            iWeightDeltaInitialAssignment = properties.getPropertyDouble("Value.WeightDeltaInitialAssignments", 0.0);
        }
        iGoodSelectionProb = properties.getPropertyDouble("Value.GoodSelectionProb", 0.00);
        iWeightWeightedCoflicts = properties.getPropertyDouble("Value.WeightWeightedConflicts", 0.0);
        iWeightPotentialConflicts = properties.getPropertyDouble("Value.WeightPotentialConflicts", 0.0);

        iRandomWalkProb = properties.getPropertyDouble("Value.RandomWalkProb", 0.0);
        iWeightCoflicts = properties.getPropertyDouble("Value.WeightConflicts", 1.0);
        iWeightValue = properties.getPropertyDouble("Value.WeightValue", 0.0);
        iTabuSize = properties.getPropertyInt("Value.Tabu", 0);
        if (iTabuSize > 0)
            iTabu = new ArrayList<Enrollment>(iTabuSize);
    }

    /** Initialization */
    @Override
    public void init(Solver<Request, Enrollment> solver) {
        for (Extension<Request, Enrollment> extension : solver.getExtensions()) {
            if (ConflictStatistics.class.isInstance(extension))
                iStat = (ConflictStatistics<Request, Enrollment>) extension;
            if (MacPropagation.class.isInstance(extension))
                iProp = (MacPropagation<Request, Enrollment>) extension;
            if (ViolatedInitials.class.isInstance(extension))
                iViolatedInitials = (ViolatedInitials<Request, Enrollment>) extension;
        }
    }

    /** true, if it is allowed to assign given value 
     * @param assignment current assignment
     * @param value given value
     * @return true if it is allowed
     **/
    public boolean isAllowed(Assignment<Request, Enrollment> assignment, Enrollment value, AssignmentCheck<Request, Enrollment> test) {
        return isAllowed(assignment, value, null, test);
    }

    /** true, if it is allowed to assign given value 
     * @param assignment current assignment
     * @param value given value
     * @param conflicts conflicting assignments
     * @return true if it is allowed
     **/
    public boolean isAllowed(Assignment<Request, Enrollment> assignment, Enrollment value, Set<Enrollment> conflicts, AssignmentCheck<Request, Enrollment> test) {
        if (value == null)
            return true;
        StudentSectioningModel model = (StudentSectioningModel) value.variable().getModel();
        if (model.getNrLastLikeRequests(false) == 0 || model.getNrRealRequests(false) == 0) {
            // all students are dummy or all are real
            if (test != null) {
                // there is an assignment check >> check if all conflicts can be unassigned
                if (conflicts == null)
                    conflicts = value.variable().getModel().conflictValues(assignment, value);
                for (Enrollment conflict : conflicts)
                    if (!test.canUnassign(value, conflict, assignment)) return false;
            }
            return true;
        }
        Request request = value.variable();
        if (request.getStudent().isDummy()) {
            // dummy student cannot unassign real student
            if (conflicts == null)
                conflicts = value.variable().getModel().conflictValues(assignment, value);
            for (Enrollment conflict : conflicts) {
                if (!conflict.getRequest().getStudent().isDummy())
                    return false;
                if (test != null && !test.canUnassign(value, conflict, assignment))
                    return false;
            }
        } else {
            // real student
            if (conflicts == null)
                conflicts = value.variable().getModel().conflictValues(assignment, value);
            if (test == null) {
                // no assignment check >> legacy behavior
                if (conflicts.size() > (assignment.getValue(request) == null ? 1 : 0))
                    return false;
            } else {
                // there is an assignment check >> check if all conflicts can be unassigned
                for (Enrollment conflict : conflicts)
                    if (!test.canUnassign(value, conflict, assignment))
                        return false;
            }
        }
        return true;
    }

    /** Value selection */
    @Override
    public Enrollment selectValue(Solution<Request, Enrollment> solution, Request selectedVariable) {
        return selectValue(solution, selectedVariable, null);
    }

    public Enrollment selectValue(Solution<Request, Enrollment> solution, Request selectedVariable, AssignmentCheck<Request, Enrollment> test) {
        Assignment<Request, Enrollment> assignment = solution.getAssignment();
        if (iMPP) {
            if (selectedVariable.getInitialAssignment() != null) {
                if (solution.getModel().unassignedVariables(assignment).isEmpty()) {
                    if (solution.getModel().perturbVariables(assignment).size() <= iMPPLimit)
                        iMPPLimit = solution.getModel().perturbVariables(assignment).size() - 1;
                }
                if (iMPPLimit >= 0 && solution.getModel().perturbVariables(assignment).size() > iMPPLimit) {
                    if (isAllowed(assignment, selectedVariable.getInitialAssignment(), test))
                        return selectedVariable.getInitialAssignment();
                }
                if (selectedVariable.getInitialAssignment() != null && ToolBox.random() <= iInitialSelectionProb) {
                    if (isAllowed(assignment, selectedVariable.getInitialAssignment(), test))
                        return selectedVariable.getInitialAssignment();
                }
            }
        }

        List<Enrollment> values = selectedVariable.values(assignment);
        if (ToolBox.random() <= iRandomWalkProb) {
            Enrollment value = ToolBox.random(values);
            if (isAllowed(assignment, value, test))
                return value;
        }
        if (iProp != null && assignment.getValue(selectedVariable) == null && ToolBox.random() <= iGoodSelectionProb) {
            Set<Enrollment> goodValues = iProp.goodValues(assignment, selectedVariable);
            if (!goodValues.isEmpty())
                values = new ArrayList<Enrollment>(goodValues);
        }
        if (values.size() == 1) {
            Enrollment value = values.get(0);
            if (isAllowed(assignment, value, test))
                return value;
            else
                return null;
        }

        List<Enrollment> bestValues = null;
        double bestWeightedSum = 0;

        for (Enrollment value : values) {
            if (iTabu != null && iTabu.contains(value))
                continue;
            if (assignment.getValue(selectedVariable) != null && assignment.getValue(selectedVariable).equals(value))
                continue;

            Set<Enrollment> conf = solution.getModel().conflictValues(assignment, value);
            if (conf.contains(value))
                continue;

            if (!isAllowed(assignment, value, conf, test))
                continue;

            double weightedConflicts = (iStat == null || iWeightWeightedCoflicts == 0.0 ? 0.0 : iStat.countRemovals(solution.getIteration(), conf, value));
            double potentialConflicts = (iStat == null || iWeightPotentialConflicts == 0.0 ? 0.0 : iStat.countPotentialConflicts(assignment, solution.getIteration(), value, 3));

            long deltaInitialAssignments = 0;
            if (iMPP && iWeightDeltaInitialAssignment != 0.0) {
                if (iViolatedInitials != null) {
                    Set<Enrollment> violations = iViolatedInitials.getViolatedInitials(value);
                    if (violations != null) {
                        for (Enrollment aValue : violations) {
                            if (assignment.getValue(aValue.variable()) == null || assignment.getValue(aValue.variable()).equals(aValue))
                                deltaInitialAssignments += 2;
                        }
                    }
                }
                for (Enrollment aValue : conf) {
                    if (aValue.variable().getInitialAssignment() != null)
                        deltaInitialAssignments--;
                }
                if (selectedVariable.getInitialAssignment() != null
                        && !selectedVariable.getInitialAssignment().equals(value)) {
                    deltaInitialAssignments++;
                }
                if (iMPPLimit >= 0 && (solution.getModel().perturbVariables(assignment).size() + deltaInitialAssignments) > iMPPLimit)
                    continue;
            }
            
            double val = value.toDouble(assignment);
            for (Enrollment c: conf)
                val -= c.toDouble(assignment);

            double weightedSum = (iWeightDeltaInitialAssignment * deltaInitialAssignments)
                    + (iWeightPotentialConflicts * potentialConflicts) + (iWeightWeightedCoflicts * weightedConflicts)
                    + (iWeightCoflicts * conf.size())
                    + (iWeightValue * val);

            if (bestValues == null || bestWeightedSum > weightedSum) {
                bestWeightedSum = weightedSum;
                if (bestValues == null)
                    bestValues = new ArrayList<Enrollment>();
                else
                    bestValues.clear();
                bestValues.add(value);
            } else {
                if (bestWeightedSum == weightedSum)
                    bestValues.add(value);
            }
        }

        Enrollment selectedValue = (bestValues == null ? null : ToolBox.random(bestValues));
        if (selectedValue == null)
            selectedValue = ToolBox.random(values);
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
