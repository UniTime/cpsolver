package org.cpsolver.studentsct.heuristics;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.heuristics.RoundRobinNeighbourSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.solver.SolverListener;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.studentsct.heuristics.selection.AssignInitialSelection;
import org.cpsolver.studentsct.heuristics.selection.BacktrackSelection;
import org.cpsolver.studentsct.heuristics.selection.BranchBoundSelection;
import org.cpsolver.studentsct.heuristics.selection.PriorityConstructionSelection;
import org.cpsolver.studentsct.heuristics.selection.RandomUnassignmentSelection;
import org.cpsolver.studentsct.heuristics.selection.ResectionIncompleteStudentsSelection;
import org.cpsolver.studentsct.heuristics.selection.ResectionUnassignedStudentsSelection;
import org.cpsolver.studentsct.heuristics.selection.RndUnProblStudSelection;
import org.cpsolver.studentsct.heuristics.selection.StandardSelection;
import org.cpsolver.studentsct.heuristics.selection.SwapStudentSelection;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;

/**
 * (Batch) student sectioning neighbour selection. It is based on
 * {@link RoundRobinNeighbourSelection}, the following steps are involved:
 * <ul>
 * <li>Phase 1: section all students using incremental branch &amp; bound (no
 * unassignments) ({@link BranchBoundSelection} is used)
 * <li>Phase 2: pick a student (one by one) with an incomplete schedule, try to
 * find an improvement ({@link SwapStudentSelection} is used)
 * <li>Phase 3: use standard value selection for some time (
 * {@link StandardSelection} is used)
 * <li>Phase 4: use backtrack neighbour selection ({@link BacktrackSelection} is
 * used)
 * <li>Phase 5: pick a student (one by one) with an incomplete schedule, try to
 * find an improvement, identify problematic students (
 * {@link SwapStudentSelection} is used)
 * <li>Phase 6: random unassignment of some problematic students (
 * {@link RndUnProblStudSelection} is used)
 * <li>Phase 7: resection incomplete students (
 * {@link ResectionIncompleteStudentsSelection} is used)
 * <li>Phase 8: resection of students that were unassigned in step 6 (
 * {@link ResectionUnassignedStudentsSelection} is used)
 * <li>Phase 9: pick a student (one by one) with an incomplete schedule, try to
 * find an improvement ({@link SwapStudentSelection} is used)
 * <li>Phase 10: use standard value selection for some time (
 * {@link StandardSelection} with {@link RouletteWheelRequestSelection} is used)
 * <li>Phase 11: pick a student (one by one) with an incomplete schedule, try to
 * find an improvement ({@link SwapStudentSelection} is used)
 * <li>Phase 12: use backtrack neighbour selection ({@link BacktrackSelection}
 * is used)
 * <li>Phase 13: random unassignment of some students (
 * {@link RandomUnassignmentSelection} is used)
 * </ul>
 * 
 * <br>
 * <br>
 * 
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

public class StudentSctNeighbourSelection extends RoundRobinNeighbourSelection<Request, Enrollment> implements SolverListener<Request, Enrollment> {
    private boolean iUseConstruction = false;
    private boolean iMPP = false;

    public StudentSctNeighbourSelection(DataProperties properties) throws Exception {
        super(properties);
        iUseConstruction = properties.getPropertyBoolean("Sectioning.UsePriorityConstruction", iUseConstruction);
        iMPP = properties.getPropertyBoolean("General.MPP", false);
    }

    @Override
    public void init(Solver<Request, Enrollment> solver) {
        super.init(solver);
        setup(solver);
        solver.setUpdateProgress(false);
        solver.addSolverListener(this);
    }

    public void setup(Solver<Request, Enrollment> solver) {
        if (iMPP)
            registerSelection(new AssignInitialSelection(solver.getProperties()));
        
        // Phase 1: section all students using incremental branch & bound (no
        // unassignments)
        registerSelection(iUseConstruction ?
                new PriorityConstructionSelection(solver.getProperties()) :
                new BranchBoundSelection(solver.getProperties()));

        // Phase 2: pick a student (one by one) with an incomplete schedule, try
        // to find an improvement
        registerSelection(new SwapStudentSelection(solver.getProperties()));

        // Phase 3: use standard value selection for some time
        registerSelection(new StandardSelection(solver.getProperties(), getVariableSelection(), getValueSelection()));

        // Phase 4: use backtrack neighbour selection
        registerSelection(new BacktrackSelection(solver.getProperties()));

        // Phase 5: pick a student (one by one) with an incomplete schedule, try
        // to find an improvement, identify problematic students
        SwapStudentSelection swapStudentSelection = new SwapStudentSelection(solver.getProperties());
        registerSelection(swapStudentSelection);

        // Phase 6: random unassignment of some problematic students
        registerSelection(new RndUnProblStudSelection(solver.getProperties(), swapStudentSelection));

        // Phase 7: resection incomplete students
        registerSelection(new ResectionIncompleteStudentsSelection(solver.getProperties()));

        // Phase 8: resection of students that were unassigned in step 6
        registerSelection(new ResectionUnassignedStudentsSelection(solver.getProperties()));

        // Phase 9: pick a student (one by one) with an incomplete schedule, try
        // to find an improvement
        registerSelection(new SwapStudentSelection(solver.getProperties()));

        // Phase 10: use standard value selection for some time
        registerSelection(new StandardSelection(solver.getProperties(), new RouletteWheelRequestSelection(solver.getProperties()), getValueSelection()));

        // Phase 11: pick a student (one by one) with an incomplete schedule,
        // try to find an improvement
        registerSelection(new SwapStudentSelection(solver.getProperties()));

        // Phase 12: use backtrack neighbour selection
        registerSelection(new BacktrackSelection(solver.getProperties()));

        // Phase 13: random unassignment of some students
        registerSelection(new RandomUnassignmentSelection(solver.getProperties()));
    }

    @Override
    public void changeSelection(int selectionIndex) {
        super.changeSelection(selectionIndex);
    }

    @Override
    public boolean variableSelected(Assignment<Request, Enrollment> assignment, long iteration, Request variable) {
        return true;
    }

    @Override
    public boolean valueSelected(Assignment<Request, Enrollment> assignment, long iteration, Request variable, Enrollment value) {
        return true;
    }

    @Override
    public boolean neighbourSelected(Assignment<Request, Enrollment> assignment, long iteration, Neighbour<Request, Enrollment> neighbour) {
        return true;
    }

    @Override
    public void neighbourFailed(Assignment<Request, Enrollment> assignment, long iteration, Neighbour<Request, Enrollment> neighbour) {
        NeighbourSelection<Request, Enrollment> selection = getSelection();
        if (neighbour instanceof BranchBoundSelection.BranchBoundNeighbour && selection instanceof BranchBoundSelection)
            ((BranchBoundSelection)selection).addStudent(((BranchBoundSelection.BranchBoundNeighbour)neighbour).getStudent());
    }

}
