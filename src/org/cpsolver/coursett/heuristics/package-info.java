/**
 * University Course Timetabling: Heuristics.
 * <br>
 * <br>
 * The quality of a solution is expressed as a weighted sum combining
 * soft time and classroom preferences, satisfied soft group constrains
 * and the total number of student conflicts. This allows us to express
 * the importance of different types of soft constraints. The following
 * weights are considered in the sum:
 * <ul>
 *         <li>
 *                 weight of a student conflict,
 *                 <li>
 *                         weight of a time preference of a placement,
 *                         <li>
 *                                 weight of a classroom preference of a placement,
 *                                 <li>
 *                                         weight of a preference of a satisfied soft group constraint,
 *                                         <li>
 *                                                 weight of a distance instructor preference of a placement (it is
 *                                                 discouraged if there are two subsequent classes taught by the
 *                                                 same instructor but placed in different buildings not far than
 *                                                 50 meters, strongly discouraged if the buildings are more than
 *                                                 50 meters but less than 200 meters far)
 *                                                 <li>
 *                                                         weight of the overall department balancing penalty (number of
 *                                                         the time units used over initial allowances summed over all
 *                                                         times and departments)
 *                                                         <li>
 *                                                                 weight of an useless half-hour (empty half-hour time segments
 *                                                                 between classes, such half-hours cannot be used since all
 *                                                                 events require at least one hour)
 *                                                                 <li>
 *                                                                         weight of a too large classrooms (weight for each classroom
 *                                                                         that has more than 50% excess seats)
 * </ul>
 * <br>
 * Note that preferences of all time, classroom and group soft
 * constraints go from -2 (strongly preferred) to 2 (strongly
 * discouraged). So, for instance, the value of the weighted sum is
 * increased when there is a discouraged time or room selected or a
 * discouraged group constraint satisfied. Therefore, if there are two
 * solutions, the better solution of them has the lower weighted sum of
 * the above criteria.
 * <br>
 * <br>
 * The termination condition stops the search when the solution is
 * complete and good enough (expressed as the number of perturbations and
 * the solution quality described above). It also allows for the solver
 * to be stopped by the user. Characteristics of the current and the best
 * achieved solution, describing the number of assigned variables, time
 * and classroom preferences, the total number of student conflicts,
 * etc., are visible to the user during the search.
 * <br>
 * <br>
 * The solution comparator prefers a more complete solution (with a
 * smaller number of unassigned variables) and a solution with a smaller
 * number of perturbations among solutions with the same number of
 * unassigned variables. If both solutions have the same number of
 * unassigned variables and perturbations, the solution of better quality
 * is selected.
 * <br>
 * <br>
 * If there are one or more variables unassigned, the variable selection
 * criterion picks one of them randomly. We have tried several approaches
 * using domain sizes, number of previous assignments, numbers of
 * constraints in which the variable participates, etc., but there was no
 * significant improvement in this timetabling problem towards the random
 * selection of an unassigned variable. The reason is, that it is easy to
 * go back when a wrong variable is picked - such a variable is
 * unassigned when there is a conflict with it in some of the subsequent
 * iterations.
 * <br>
 * <br>
 * When all variables are assigned, an evaluation is made for each
 * variable according to the above described weights. The variable with
 * the worst evaluation is selected. This variable promises the best
 * improvement in optimization.
 * <br>
 * <br>
 * We have implemented a hierarchical handling of the value selection
 * criteria. There are three levels of comparison. At each level a
 * weighted sum of the criteria described below is computed. Only
 * solutions with the smallest sum are considered in the next level. The
 * weights express how quickly a complete solution should be found. Only
 * hard constraints are satisfied in the first level sum. Distance from
 * the initial solution (MPP), and a weighting of major preferences
 * (including time, classroom requirements and student conflicts), are
 * considered in the next level. In the third level, other minor criteria
 * are considered. In general, a criterion can be used in more than one
 * level, e.g., with different weights.
 * <br>
 * <br>
 * The above sums order the values lexicographically: the best value
 * having the smallest first level sum, the smallest second level sum
 * among values with the smallest first level sum, and the smallest third
 * level sum among these values. As mentioned above, this allows
 * diversification between the importance of individual criteria.
 * <br>
 * <br>
 * Furthermore, the value selection heuristics also support some limits
 * (e.g., that all values with a first level sum smaller than a given
 * percentage Pth above the best value [typically 10%] will go to the
 * second level comparison and so on). This allows for the continued
 * feasibility of a value near to the best that may yet be much better in
 * the next level of comparison. If there is more than one solution after
 * these three levels of comparison, one is selected randomly. This
 * approach helped us to significantly improve the quality of the
 * resultant solutions.
 * <br>
 * <br>
 * In general, there can be more than three levels of these weighted
 * sums, however three of them seem to be sufficient for spreading
 * weights of various criteria for our problem.
 * <br>
 * <br>
 * The value selection heuristics also allow for random selection of a
 * value with a given probability (random walk, e.g., 2%) and, in the
 * case of MPP, to select the initial value (if it exists) with a given
 * probability (e.g., 70%).
 * <br>
 * <br>
 * Criteria used in the value selection heuristics can be divided into
 * two sets. Criteria in the first set are intended to generate a
 * complete assignment:
 * <ul>
 *         <li>
 *                 Number of hard conflicts
 *                 <li>
 *                         Number of hard conflicts, weighted by their previous occurrences
 *                         (see conflict-based statistics)
 * </ul>
 * Additional criteria allow better results to be achieved during
 * optimization:
 * <ul>
 *         <li>
 *                 Number of student conflicts caused by the value if it is assigned to
 *                 the variable
 *                 <li>
 *                         Soft time preference caused by a value if it is assigned to the
 *                         variable
 *                         <li>
 *                                 Soft classroom preference caused by a value if it is assigned to
 *                                 the variable (combination of the placement's building, room, and
 *                                 classroom equipment compared with preferences)
 *                                 <li>
 *                                         Preferences of satisfied soft group constraints caused by the
 *                                         value if it is assigned to the variable
 *                                         <li>
 *                                                 Difference in the number of assigned initial values if the value
 *                                                 is assigned to the variable: -1 if the value is initial, 0
 *                                                 otherwise, increased by the number of initial values assigned to
 *                                                 variables with hard conflicts with the value.
 *                                                 <li>
 *                                                         Distance instructor preference caused by a value if it is
 *                                                         assigned to the variable (together with the neighbour classes)
 *                                                         <li>
 *                                                                 Difference in department balancing penalty
 *                                                                 <li>
 *                                                                         Difference in the number of useless half-hours (number of
 *                                                                         empty half-hour time segments between classes that arise,
 *                                                                         minus those which disappear if the value is selected)
 *                                                                         <li>
 *                                                                                 Classroom is too big: 1 if the selected classroom has more
 *                                                                                 than 50% excess seats
 * </ul>
 * <br>
 * Let us emphasize that the criteria from the second group are needed
 * for optimization only, i.e., they are not needed to find a feasible
 * solution. Furthermore, assigning a different weight to a particular
 * criteria influences the value of the corresponding objective function.
 * 
 * @author  Tomas Muller
 * @version IFS 1.4 (Instructor Sectioning)<br>
 *          Copyright (C) 2024 Tomas Muller<br>
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
package org.cpsolver.coursett.heuristics;