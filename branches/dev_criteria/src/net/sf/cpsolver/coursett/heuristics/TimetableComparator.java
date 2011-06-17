package net.sf.cpsolver.coursett.heuristics;

import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.ifs.criteria.Criterion;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.solution.GeneralSolutionComparator;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Timetable (solution) comparator. <br>
 * <br>
 * The quality of a solution is expressed as a weighted sum combining soft time
 * and classroom preferences, satisfied soft group constrains and the total
 * number of student conflicts. This allows us to express the importance of
 * different types of soft constraints. <br>
 * <br>
 * The solution comparator prefers a more complete solution (with a smaller
 * number of unassigned variables) and a solution with a smaller number of
 * perturbations among solutions with the same number of unassigned variables.
 * If both solutions have the same number of unassigned variables and
 * perturbations, the solution of better quality is selected. <br>
 * <br>
 * Parameters:
 * <table border='1'>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>Comparator.HardStudentConflictWeight</td>
 * <td>{@link Double}</td>
 * <td>Weight of hard student conflict (conflict between single-section classes)
 * </td>
 * </tr>
 * <tr>
 * <td>Comparator.StudentConflictWeight</td>
 * <td>{@link Double}</td>
 * <td>Weight of student conflict</td>
 * </tr>
 * <tr>
 * <td>Comparator.TimePreferenceWeight</td>
 * <td>{@link Double}</td>
 * <td>Time preferences weight</td>
 * </tr>
 * <tr>
 * <td>Comparator.ContrPreferenceWeight</td>
 * <td>{@link Double}</td>
 * <td>Group constraint preferences weight</td>
 * </tr>
 * <tr>
 * <td>Comparator.RoomPreferenceWeight</td>
 * <td>{@link Double}</td>
 * <td>Room preferences weight</td>
 * </tr>
 * <tr>
 * <td>Comparator.UselessSlotWeight</td>
 * <td>{@link Double}</td>
 * <td>Useless slots weight</td>
 * </tr>
 * <tr>
 * <td>Comparator.TooBigRoomWeight</td>
 * <td>{@link Double}</td>
 * <td>Too big room weight</td>
 * </tr>
 * <tr>
 * <td>Comparator.DistanceInstructorPreferenceWeight</td>
 * <td>{@link Double}</td>
 * <td>Distance (of the rooms of the back-to-back classes) based instructor
 * preferences weight</td>
 * </tr>
 * <tr>
 * <td>Comparator.PerturbationPenaltyWeight</td>
 * <td>{@link Double}</td>
 * <td>Perturbation penalty (see {@link UniversalPerturbationsCounter})</td>
 * </tr>
 * <tr>
 * <td>Comparator.DeptSpreadPenaltyWeight</td>
 * <td>{@link Double}</td>
 * <td>Department balancing penalty (see
 * {@link net.sf.cpsolver.coursett.constraint.DepartmentSpreadConstraint})</td>
 * </tr>
 * </table>
 * 
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
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
@Deprecated
public class TimetableComparator extends GeneralSolutionComparator<Lecture, Placement> {

    public TimetableComparator(DataProperties properties) {
        super(properties);
    }
    
    /** 
     * Use {@link Model#getTotalValue()} instead.
     */
    @Deprecated
    public double currentValue(Solution<Lecture, Placement> currentSolution) {
        double ret = 0.0;
        for (Criterion<Lecture, Placement> criterion: currentSolution.getModel().getCriteria())
            ret += criterion.getWeightedValue();
        return ret;
    }
    
    /** 
     * Use {@link Solution#getBestValue()} instead.
     */
    @Deprecated
    public double getBest(Solution<Lecture, Placement> currentSolution) {
        double ret = 0.0;
        for (Criterion<Lecture, Placement> criterion: currentSolution.getModel().getCriteria())
            ret += criterion.getWeightedBest();
        return ret;
    }
}
