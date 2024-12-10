package org.cpsolver.exam.heuristics;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamModel;
import org.cpsolver.exam.model.ExamPeriodPlacement;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.exam.model.ExamRoomPlacement;
import org.cpsolver.exam.neighbours.ExamSimpleNeighbour;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentContext;
import org.cpsolver.ifs.assignment.context.NeighbourSelectionWithContext;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.Progress;


/**
 * Initial solution construction heuristics. <br>
 * <br>
 * While there are exams that are still not assigned:
 * <ul>
 * <li>The worst unassigned exam is selected (using {@link Exam#compareTo(Exam)})
 * <li>The best period that does not break any hard constraint and for which
 * there is a room assignment available is selected together with the set the
 * best available rooms for the exam in the best period (computed using
 * {@link Exam#findBestAvailableRooms(Assignment, ExamPeriodPlacement)}).
 * </ul>
 * <br>
 * <br>
 * If problem property ExamConstruction.CheckLocalOptimality is true, local
 * (time) optimality is enforced at the end of this phase. During this
 * procedure, for each exam, it tries to change the period of the exam so that
 * the time cost is lower (see {@link ExamPlacement#getTimeCost(Assignment)}), but no hard
 * constraint is violated. The problem is considered locally optimal if there is
 * no such move. <br>
 * <br>
 * 
 * @author  Tomas Muller
 * @version ExamTT 1.3 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2014 Tomas Muller<br>
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
public class ExamConstruction extends NeighbourSelectionWithContext<Exam, ExamPlacement, ExamConstruction.Context> {
    private boolean iCheckLocalOptimality = false;
    private Progress iProgress = null;
    private boolean iActive = false;

    /**
     * Constructor
     * 
     * @param properties
     *            problem properties
     */
    public ExamConstruction(DataProperties properties) {
        iCheckLocalOptimality = properties.getPropertyBoolean("ExamConstruction.CheckLocalOptimality", iCheckLocalOptimality);
    }

    /**
     * Initialization
     */
    @Override
    public void init(Solver<Exam, ExamPlacement> solver) {
        super.init(solver);
        // solver.setUpdateProgress(false);
        iProgress = Progress.getInstance(solver.currentSolution().getModel());
        iActive = false;
    }

    /**
     * Find a new assignment of one of the assigned exams that improves the time
     * cost {@link ExamPlacement#getTimeCost(Assignment)} and for which there is a set of
     * available rooms {@link Exam#findBestAvailableRooms(Assignment, ExamPeriodPlacement)}.
     * Return null, if there is no such assignment (the problem is considered
     * locally optimal).
     * @param assignment current assignment
     * @param model problem model
     * @return a neighbour to assign or null if none was found
     */
    public Neighbour<Exam, ExamPlacement> checkLocalOptimality(Assignment<Exam, ExamPlacement> assignment, ExamModel model) {
        if (iCheckLocalOptimality) {
            Context context = getContext(assignment); 
            for (Exam exam : assignment.assignedVariables()) {
                ExamPlacement current = assignment.getValue(exam);
                if (current.getTimeCost(assignment) <= 0)
                    continue;
                ExamPlacement best = null;
                for (ExamPeriodPlacement period : exam.getPeriodPlacements()) {
                    if (exam.countStudentConflicts(assignment, period) > 0) {
                        if (context.assignments().contains(exam.getId() + ":" + period.getIndex()))
                            continue;
                    }
                    if (exam.countInstructorConflicts(assignment, period) > 0) {
                        if (context.assignments().contains(exam.getId() + ":" + period.getIndex()))
                            continue;
                    }
                    if (!exam.checkDistributionConstraints(assignment, period))
                        continue;
                    ExamPlacement placement = new ExamPlacement(exam, period, null);
                    if (best == null || best.getTimeCost(assignment) > placement.getTimeCost(assignment)) {
                        Set<ExamRoomPlacement> rooms = exam.findBestAvailableRooms(assignment, period);
                        if (rooms != null)
                            best = new ExamPlacement(exam, period, rooms);
                    }
                }
                if (best != null && best.getTimeCost(assignment) < current.getTimeCost(assignment))
                    return new ExamSimpleNeighbour(assignment, best);
            }
        }
        iActive = false;
        return null;
    }

    /**
     * Select a neighbour. While there are exams that are still not assigned:
     * <ul>
     * <li>The worst unassigned exam is selected (using
     * {@link Exam#compareTo(Exam)})
     * <li>The best period that does not break any hard constraint and for which
     * there is a room assignment available is selected together with the set
     * the best available rooms for the exam in the best period (computed using
     * {@link Exam#findBestAvailableRooms(Assignment, ExamPeriodPlacement)}).
     * </ul>
     * Return null when done (all variables are assigned and the problem is
     * locally optimal).
     */
    @Override
    public Neighbour<Exam, ExamPlacement> selectNeighbour(Solution<Exam, ExamPlacement> solution) {
        ExamModel model = (ExamModel) solution.getModel();
        Assignment<Exam, ExamPlacement> assignment = solution.getAssignment();
        Context context = getContext(assignment);
        if (!iActive) {
            iActive = true;
            // iProgress.setPhase("Construction ...", model.variables().size());
            iProgress.info("[" + Thread.currentThread().getName() + "] Construction ...");
        }
        // iProgress.setProgress(assignment.nrAssignedVariables());
        if (model.variables().size() - assignment.nrAssignedVariables() <= context.skip().size())
            return checkLocalOptimality(assignment, model);
        Exam bestExam = null;
        for (Exam exam : model.variables()) {
            if (assignment.getValue(exam) != null || context.skip().contains(exam))
                continue;
            if (bestExam == null || exam.compareTo(bestExam) < 0)
                bestExam = exam;
        }
        ExamPlacement best = null;
        for (ExamPeriodPlacement period : bestExam.getPeriodPlacements()) {
            if (bestExam.countStudentConflicts(assignment, period) > 0) {
                if (context.assignments().contains(bestExam.getId() + ":" + period.getIndex()))
                    continue;
            }
            if (bestExam.countInstructorConflicts(assignment, period) > 0) {
                if (context.assignments().contains(bestExam.getId() + ":" + period.getIndex()))
                    continue;
            }
            if (!bestExam.checkDistributionConstraints(assignment, period))
                continue;
            ExamPlacement placement = new ExamPlacement(bestExam, period, null);
            if (best == null || best.getTimeCost(assignment) > placement.getTimeCost(assignment)) {
                Set<ExamRoomPlacement> rooms = bestExam.findBestAvailableRooms(assignment, period);
                if (rooms != null)
                    best = new ExamPlacement(bestExam, period, rooms);
            }
        }
        if (best != null) {
            context.assignments().add(bestExam.getId() + ":" + best.getPeriod().getIndex());
            return new ExamSimpleNeighbour(assignment, best);
        } /*
           * else { for (Enumeration
           * e=bestExam.getPeriodPlacements().elements();e.hasMoreElements();) {
           * ExamPeriodPlacement period = (ExamPeriodPlacement)e.nextElement();
           * ExamPlacement placement = new ExamPlacement(bestExam, period,
           * null); if (best==null ||
           * best.getTimeCost()>placement.getTimeCost()) { Set rooms =
           * bestExam.findBestAvailableRooms(period); if (rooms!=null) best =
           * new ExamPlacement(bestExam, period, rooms); } } if (best!=null) {
           * bestExam.allowAllStudentConflicts(best.getPeriod());
           * iAssignments.add(bestExam.getId()+":"+best.getPeriod().getIndex());
           * return new ExamSimpleNeighbour(best); } }
           */
        if (!context.skip().contains(bestExam)) {
            context.skip().add(bestExam);
            return selectNeighbour(solution);
        }
        return checkLocalOptimality(assignment, model);
    }
    
    @Override
    public Context createAssignmentContext(Assignment<Exam, ExamPlacement> assignment) {
        return new Context();
    }

    public class Context implements AssignmentContext {
        private Set<String> iAssignments = Collections.synchronizedSet(new HashSet<String>());
        private Set<Exam> iSkip = Collections.synchronizedSet(new HashSet<Exam>());
        
        public Set<Exam> skip() { return iSkip; }
        
        public Set<String> assignments() { return iAssignments; }
    }
}