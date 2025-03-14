/**
 * Simple Timetabling Problem.
 * <br>
 * <br>
 * Simplified model for timetabling problems consisting of a set of
 * resources, a set of activities, and a set of dependencies between the
 * activities. Time is divided into time slots with the same duration.
 * Every slot may have assigned a constraint, either hard or soft: a hard
 * constraint indicates that the slot is forbidden for any activity, a
 * soft constraint indicates that the slot is not preferred. We call
 * these constraints 'time preferences'. Every activity and every
 * resource may have assigned a set of time preferences, which indicate
 * forbidden and not preferred time slots.
 * <br>
 * <br>
 * Activity (which can be, for instance, directly mapped to a lecture) is
 * identified by its name. Every activity is described by its duration
 * (expressed as a number of time slots), by time preferences, and by a
 * set of resources. This set of resources determines which resources are
 * required by the activity. To model alternative as well as required
 * resources, we divide the set of resources into several subsets of
 * resource groups. Each group is either conjunctive or disjunctive: the
 * conjunctive group of resources means that the activity needs all the
 * resources from the group, the disjunctive group means that the
 * activity needs exactly one of the resources (we can choose from
 * several alternatives). An example can be a lecture, which will take
 * place in one of the possible classrooms and it will be taught for all
 * of the selected classes. Note that we do not need to model conjunctive
 * groups explicitly because we can use a set of disjunctive groups
 * containing exactly one resource instead (the set of required resources
 * can be described in a conjunctive normal form). However, usage of both
 * conjunctive and disjunctive groups simplifies modelling for the users.
 * <br>
 * <br>
 * Resource is also identified by its name and it is fully described by
 * time preferences. There is a hard condition that only one activity can
 * use the resource at the same time. For instance, such resource can
 * represent a teacher, a class, a classroom, or another special resource
 * at the lecture timetabling problem.
 * <br>
 * <br>
 * Finally, we need a mechanism for defining and handling direct
 * dependencies between the activities. It seems sufficient to use binary
 * dependencies only that define relationship between two activities. We
 * defined three temporal constraints: the activity finishes before
 * another activity, the activity finishes exactly at the time when the
 * second activity starts, and two activities run concurrently (they have
 * the same start time).
 * <br>
 * <br>
 * The solution of the problem defined by the above model is a timetable
 * where every scheduled activity has assigned its start time and a set
 * of reserved resources that are needed for its execution (the activity
 * is allocated to respective slots of the reserved resources). This
 * timetable must satisfy all the hard constraints, namely:
 * <ul>
 * <li>
 *      every scheduled activity has all the required resources reserved,
 *      i.e., all resources from the conjunctive groups and one resource
 *      from each disjunctive group of resources,
 * <li>
 *      two scheduled activities cannot use the same resource at the same
 *      time,
 * <li>
 *      no activity is scheduled into a time slot where the activity or
 *      some of its reserved resources has a hard constraint in the time
 *      preferences,
 * <li>
 *      all dependencies between the scheduled activities must be
 *      satisfied.
 * </ul>
 * Furthermore, we want to minimize the number of violated soft
 * constraints in the time preferences of resources and activities. We do
 * not formally express this objective function; these preferences will
 * be used as a guide during the search for the solution.
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
package org.cpsolver.ifs.example.tt;