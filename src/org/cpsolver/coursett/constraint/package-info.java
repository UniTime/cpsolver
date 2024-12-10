/**
 * University Course Timetabling: Constraints.
 * <br>
 * <br>
 * There are two types of basic hard constraints: resource constraints
 * (expressing that only one course can be taught by an instructor or in
 * a particular room at the same time), and group constraints (expressing
 * relations between several classes, e.g., that two sections of the same
 * lecture can not be taught at the same time, or that some classes have
 * to be taught one immediately after another).
 * <br>
 * <br>
 * Except the constraints described above, there are several additional
 * constraints which came up during our work on this lecture timetabling
 * problem. These constraints were defined in order to make the
 * automatically computed timetable solution acceptable for users from
 * Purdue University.
 * <br>
 * <br>
 * First of all, if there are two classes placed one after another so
 * that there is no time slot in between (also called back-to-back
 * classes), distances between buildings need to be considered. The
 * general feeling is that different rooms in the same building are
 * always reasonable, moving to the building next door is to be
 * discouraged, a couple of buildings away strongly discouraged, and any
 * longer distance prohibited.
 * <br>
 * <br>
 * Each building has its location defined as a pair of coordinates [x,y].
 * The distance between two buildings is estimated by Euclides distance
 * in such two dimensional space, i.e., (dx^2 + dy^2)^(1/2) where dx and
 * dy are differences between x and y coordinates of the buildings. As
 * for instructors, two subsequent classes (where there is no empty slot
 * in between, called also back-to-back classes) are infeasible to teach
 * when such difference is more than 200 meters (hard constraint). The
 * other options (soft constraints) are:
 * <ul>
 *         <li>
 *                 if the distance is zero (same building), then no penalty,
 *                 <li>
 *                         if the distance is above zero, but not more than 50 meters, then
 *                         the placement is discouraged,
 *                         <li>
 *                                 if the distance is between 50 and 200 meters, the placement is
 *                                 strongly discouraged
 * </ul>
 * <br>
 * Our concern for distance between back-to-back classes for students is
 * different. Here it is simply a question of whether it is feasible for
 * students to get from one class to another during the 10-minute passing
 * period. At present, the distance between buildings not more than 670
 * meters is considered as an acceptable travel distance. For the
 * distance above 670 meters, the classes are considered as too far. If
 * there is a student attending both classes, it means a student conflict
 * (same as when these classes are overlapping in time).
 * <br>
 * <br>
 * Next, since the automatic solver tries to maximize the overall
 * accomplishment of soft time and room constraints (preferences), the
 * resultant timetable might be unacceptable for some departments. The
 * problem is that some departments define their time and room
 * preferences more strictly than others. The departments which have not
 * defined time and room preferences usually have most of their classes
 * taught in early morning or late evening hours. Therefore, we
 * introduced the departmental time and room preferences balancing
 * mechanism. The solver is trying to fulfill the time and room
 * preferences as well as to balance the used times between individual
 * departments. This means that each department should use each time unit
 * (half-hour, e.g., Monday 7:30 - 8:00) in a similar portion to the
 * other time units used by the department.
 * <br>
 * <br>
 * At first, for each department and time unit, there is a number stating
 * how many times each time unit can be used (i.e., how many placements
 * of all classes from the department can be placed over the time unit).
 * For instance, if there are two 1 hour x 2 days per week classes, the
 * time unit Wednesday 8:00 - 8:30 can be used four times, i.e., each of
 * these classes can be placed either on Monday-Wednesday or
 * Wednesday-Friday from 8:00 till 9:00. Than, an average fill factor is
 * computed for each department and time unit. It is a ratio between the
 * computed number of placements using the time unit and the total number
 * of placements of all classes from the department (it is sixty for the
 * above example with two classes, each class can be placed in thirty
 * different times if all possible times are allowed). So, this factor
 * states the overall usage of a time unit for a department. The reason
 * for computing such number is the fact that some times are used much
 * more than others (e.g., if the department has most of the classes in n
 * hours hour x 3 days per week, Tuesday and Thursday are used much less
 * than Monday, Wednesday and Friday). The initial allowance, which
 * states how many times each time unit can be used by a department is
 * computed from this maximal fill factor: it is increased by the given
 * percentage (20% is used in our tests) and rounded upwards to the first
 * integer number. The overall department balancing penalty of a solution
 * is the sum of overruns of this initial allowance over all time units
 * and departments. The intention is to keep this number as low as
 * possible.
 * <br>
 * <br>
 * Finally, since all of the classes are at least two time slots long (60
 * minutes), an empty time slot of a room which is surrounded by classes
 * on both sides (i.e., the room is not used for 30 minutes between two
 * consecutive classes) is considered useless if no other class can use
 * it. The number of such useless half-hours should be minimized. Also
 * the situation when a room is occupied by a class which is using less
 * than 2/3 of its seats is discouraged. Both these soft constraints are
 * considered much less important than all the constraints described
 * above.
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
package org.cpsolver.coursett.constraint;