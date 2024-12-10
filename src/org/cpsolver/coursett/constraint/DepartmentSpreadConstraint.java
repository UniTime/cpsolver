package org.cpsolver.coursett.constraint;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.criteria.DepartmentBalancingPenalty;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.util.DataProperties;

/**
 * Departmental ballancing constraint. <br>
 * <br>
 * The new implementation of the balancing times for departments works as
 * follows: Initially, there is a histogram for each department computed. For
 * each time slot, it says how many placements of all classes (of the
 * department) include this time slot. Each such placement has the weight of 1 /
 * number of placements of the class, so the total sum of all values in the
 * histogram (i.e., for all time slots) is equal to the total sum of half-hours
 * required by the given set of classes. <br>
 * <br>
 * On the other hand, each class splits its number of half-hours (e.g., 2x50 has
 * 4 half-hours, "4 points") into the time slots which it can occupy, according
 * to the frequencies of the utilization of each time slots (i.e., number of
 * placements containing the time slots divided by the number of all placements
 * of the class). <br>
 * <br>
 * For example, a histogram for department 1286:<pre><code>
 * 1: [0.10,0.00,0.10,0.00,0.10] &larr; 7:30 [Mon, Tue, Wed, Thu, Fri]
 * 2: [0.10,0.00,0.10,0.00,0.10] &larr; 8:00 [Mon, Tue, Wed, Thu, Fri]
 * 3: [0.35,0.62,0.48,0.62,0.10] ... and so on
 * 4: [0.35,0.62,0.48,0.62,0.10]
 * 5: [1.35,1.12,1.48,0.12,1.10]
 * 6: [1.35,1.12,1.48,0.12,1.10]
 * 7: [0.35,0.62,0.48,1.63,0.10]
 * 8: [0.35,0.62,0.48,1.63,0.10]
 * 9: [0.35,0.12,0.48,0.12,0.10]
 * 10:[0.35,0.12,0.48,0.12,0.10]
 * 11:[0.35,0.12,0.48,0.12,0.10]
 * 12:[0.35,0.12,0.48,0.12,0.10]
 * 13:[0.35,0.12,0.48,1.12,0.10]
 * 14:[0.35,0.12,0.48,1.12,0.10]
 * 15:[0.35,0.12,0.48,0.12,0.10]
 * 16:[0.35,0.12,0.48,0.12,0.10]
 * 17:[0.35,0.12,0.48,0.12,0.10]
 * 18:[0.35,0.12,0.48,0.12,0.10]
 * 19:[0.10,0.00,0.10,0.00,0.10]
 * 20:[0.10,0.00,0.10,0.00,0.10]
 * 21:[0.00,0.00,0.00,0.00,0.00]
 * </code></pre>
 * You can easily see, that the time slots which are prohibited for all of the
 * classes of the department have zero values, also some time slots are used
 * much often than the others. Note that there are no preferences involved in
 * this computation, only prohibited / not used times are less involved. <br>
 * <br>
 * The idea is to setup the initial limits for each of the time slots according
 * to the above values. The reason for doing so is to take the requirements
 * (time patterns, required/prohibited times) of all classes of the department
 * into account. For instance, take two classes A and B of type MWF 2x100 with
 * two available locations starting from 7:30 and 8:30. Note that the time slot
 * Monday 8:30-9:00 will be always used by both of the classes and for instance
 * the time slot Monday 7:30-8:00 (or Friday 9:30-10:00) can be used by none of
 * them, only one of them or both of them. From the balancing point of the view,
 * I believe it should be preferred to have one class starting from 7:30 and the
 * other one from 8:30. <br>
 * <br>
 * So, after the histogram is computed, its values are increased by the given
 * percentage (same reason as before, to allow some space between the actual
 * value and the limit, also not to make the limits for a department with 21
 * time slots less strict than a department with 20 time slots etc.). The
 * initial limits are than computed as these values rounded upwards (1.90
 * results in 2). <br>
 * <br>
 * Moreover, the value is increased only when the histogram value of a time slot
 * is below the following value: spread factor * (number of used time slots /
 * number of all time slots). Is assures a department of at least the given
 * percentage more time slots than required, but does not provide an additional
 * reward for above average use of time slots based on 'required' times. <br>
 * <br>
 * For example, the department 1286 will have the following limits (histogram
 * increased by 20% (i.e., each value is 20% higher) and rounded upwards):
 * <pre><code>
 * 1: [1,0,1,0,1]
 * 2: [1,0,1,0,1]
 * 3: [1,1,1,1,1]
 * 4: [1,1,1,1,1]
 * 5: [2,2,2,1,2]
 * 6: [2,2,2,1,2]
 * 7: [1,1,1,2,1]
 * 8: [1,1,1,2,1]
 * 9: [1,1,1,1,1]
 * 10:[1,1,1,1,1]
 * 11:[1,1,1,1,1]
 * 12:[1,1,1,1,1]
 * 13:[1,1,1,2,1]
 * 14:[1,1,1,2,1]
 * 15:[1,1,1,1,1]
 * 16:[1,1,1,1,1]
 * 17:[1,1,1,1,1]
 * 18:[1,1,1,1,1]
 * 19:[1,0,1,0,1]
 * 20:[1,0,1,0,1]
 * 21:[0,0,0,0,0]
 * </code></pre>
 * The maximal penalty (i.e., the maximal number of half-hours which can be used
 * above the pre-computed limits by a department) of a constraint is used.
 * Initially, the maximal penalty is set to zero. It is increased by one after
 * each time when the constraint causes the given number (e.g., 100) of
 * un-assignments. <br>
 * <br>
 * Also, the balancing penalty (the total number of half-hours over the initial
 * limits) is computed and it can be minimized during the search (soft
 * constraint). <br>
 * <br>
 * Parameters:
 * <table border='1'><caption>Related Solver Parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>DeptBalancing.SpreadFactor</td>
 * <td>{@link Double}</td>
 * <td>Initial allowance of the slots for a particular time (factor)<br>
 * Allowed slots = ROUND(SpreadFactor * (number of requested slots / number of
 * slots per day))</td>
 * </tr>
 * <tr>
 * <td>DeptBalancing.Unassignments2Weaken</td>
 * <td>{@link Integer}</td>
 * <td>Increase the initial allowance when it causes the given number of
 * unassignments</td>
 * </tr>
 * </table>
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
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
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */

public class DepartmentSpreadConstraint extends SpreadConstraint {
    private Long iDepartment = null;

    public DepartmentSpreadConstraint(DataProperties config, Long department, String name) {
        super(name,
                config.getPropertyDouble("DeptBalancing.SpreadFactor", 1.20),
                config.getPropertyInt("DeptBalancing.Unassignments2Weaken", 250),
                config.getPropertyBoolean("General.InteractiveMode", false),
                config.getPropertyInt("General.FirstDaySlot", Constants.DAY_SLOTS_FIRST),
                config.getPropertyInt("General.LastDaySlot", Constants.DAY_SLOTS_LAST),
                config.getPropertyInt("General.FirstWorkDay", 0),
                config.getPropertyInt("General.LastWorkDay", Constants.NR_DAYS_WEEK - 1)
                );
        iDepartment = department;
    }

    public Long getDepartmentId() {
        return iDepartment;
    }
    
    @Override
    protected Criterion<Lecture, Placement> getCriterion() {
        return getModel().getCriterion(DepartmentBalancingPenalty.class);
    }

    @Override
    public String toString() {
        return "Departmental Balancing for " + getName();
    }
}