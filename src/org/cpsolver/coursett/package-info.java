/**
 * University Course Timetabling.
 * 
 * <br>
 * <br>
 * <h2>
 *         Problem Description
 * </h2>
 * Purdue is a large (39,000 students) public university with a broad
 * spectrum of programs at the undergraduate and graduate levels. In a
 * typical term there are 9,000 classes offered using 570 teaching
 * spaces. Approximately 259,000 individual student class requests must
 * be satisfied. The complete university timetabling problem is
 * decomposed into a series of subproblems to be solved at the academic
 * department level, where the resources required to provide instruction
 * are controlled. Several other special problems, where shared resources
 * or student interactions are of critical importance, are solved
 * institution wide. A major consideration in designing the system has
 * been supporting distributed construction of departmental timetables
 * while providing central coordination of the overall problem. This
 * reflects the distributed management of instructional resources across
 * multiple departments at the University.
 * <br>
 * <br>
 * Purdue University timetabling problem is naturally decomposed into
 * <ul>
 *         <li>
 *                 a centrally timetabled large lecture room problem (about 800 classes
 *                 being timetabled into 55 rooms with sizes up to 474 seats),
 *                 <li>
 *                         individually timetabled departmental problems (about 70 problems
 *                         with 10 to 500 classes),
 *                         <li>
 *                                 and a centrally timetabled computer laboratory problem (about 450
 *                                 classes timetabled into 36 rooms with 20 to 45 seats).
 * </ul>
 * The large lecture room problem consists of the largest classes on
 * campus that are attended by students from multiple departments. The
 * problem is also very dense. For instance, rooms are utilized on
 * average over 70% of the available time, and this rate increases with
 * room size (utilization is over 85% for all rooms above 100 seats and
 * about 97% for the four largest rooms having over 400 seats). Since
 * there are many interactions between this problem and the departmental
 * problems, the large lecture problem is solved first and the
 * departmental problems are solved on top of this solution.
 * <br>
 * <br>
 * On the opposite end of the spectrum, the computer laboratory problem
 * is solved at the very end of the process, on top of the large lecture
 * room and departmental problem solutions. It contains only small
 * classes, most of which have many sections (laboratories are normally
 * the smallest subparts of a course). A typical example is a course
 * having one large lecture class for 100 students, two departmental
 * recitations with 50 students each, and four computer laboratories of
 * 25 students.
 * <br>
 * <br>
 * The departmental problems are solved more or less concurrently. These
 * problems are usually quite independent of one another, occurring in
 * mostly different sets of rooms, with separate instructors and
 * students. However, there are some cases with higher levels of
 * interactions, particularly among students. In order to address these
 * situations, a concept referred to as "committing" solutions has been
 * introduced. Each user of the timetabling system (e.g., a departmental
 * schedule manager) can create and store multiple solutions. At the end
 * of the process a single solution must be selected and committed.
 * During the commit, all conflicts between the current solution and all
 * other solutions that have already been committed are checked and the
 * commit is successful only when there are no hard conflicts between
 * these solutions. Each problem being solved also automatically
 * considers all of the previously committed solutions. This means that a
 * room, an instructor, or a student is available at a particular time
 * only if that time is not already occupied in a commited solution for a
 * different problem. This approach can be beneficial, for instance, in a
 * case where there are two or more departments with many common
 * students. Here, the problems can be solved in an agreed upon order
 * (the second department will solve its problem after the first
 * department commits its solution). Moreover, if a room must be shared
 * by two departments, a room sharing matrix can be defined, stating the
 * times during the week that a room is available for each department to
 * use. Finally, there is also an option to combine two or more
 * individual problems and solve as one larger problem, considering all
 * of the relations between the problems in real time.

 * <br>
 * <br>
 * <h2>
 *         Model
 * </h2>
 * To minimize potential time conflicts, Purdue has historically
 * subscribed to a set of standard meeting patterns. With few exceptions,
 * 1 hour x 3 day per week classes meet on Monday, Wednesday, and Friday
 * at the half hour (7:30, 8:30, 9:30, ...). 1.5 hour x 2 day per week
 * classes meet on Tuesday and Thursday during set time blocks. 2 or 3
 * hours x 1day per week classes must also fit within specific blocks,
 * etc. Generally, all meetings of a class should be taught in the same
 * location. Such meeting patterns are of interest to the problem
 * solution as they allow easier changes between classes having the same
 * or similar meeting patterns.
 * <br>
 * <br>
 * Due to the set of standardized time patterns and administrative rules
 * enforced at the university, it is generally possible to represent all
 * meetings of a class by a single variable. This tying together of
 * meetings considerably simplifies the problem constraints. Most classes
 * have all meetings taught in the same room, by the same instructor, at
 * the same time of day. Only the day of week differs. Moreover, these
 * days and times are mapped together with the help of meeting patterns,
 * e.g., a 2 hours x 3 day per week class can be taught only on Monday,
 * Wednesday, Friday, beginning at 5 possible times. Or, for instance, a
 * 1 hour x 2 day per week class can be taught only on Monday-Wednesday,
 * Wednesday-Friday or Monday-Friday, beginning at 10 possible times.
 * <br>
 * <br>
 * In addition, all valid placements of a course in the timetable have a
 * one-to-one mapping with values in the variable's domain. This domain
 * can be seen as a subset of the Cartesian product of the possible
 * starting times, rooms, etc. for a class represented by these values.
 * Therefore, each value encodes the selected time pattern (some
 * alternatives may occur, e.g., 1.5 hour x 2 day per week may be an
 * alternative to 1 hour x 3 day per week), selected days (e.g., a two
 * meeting course can be taught in Monday-Wednesday, Tuesday-Thursday,
 * Wednesday-Friday), and possible starting times. A value also encodes
 * the instructor and selected meeting room. Each such placement also
 * encodes its preferences (soft constraints), combined from the
 * preference for time, room, building and the room's available
 * equipment. Only placements with valid times and rooms are present in a
 * class's domain. For example, when a computer (classroom equipment) is
 * required, only placements in a room containing a computer are present.
 * Also, only rooms large enough to accommodate all the enrolled students
 * can be present in valid class placements. Similarly, if a time slice
 * is prohibited, no placement containing this time slice is in the
 * class's domain.
 * <br>
 * <br>
 * As mentioned above, each value, besides encoding a class's placement
 * (time, room, instructor), also contains information about the
 * preference for the given time and room. Room preference is a
 * combination of preferences on the choice of building, room, and
 * classroom equipment. The second group of soft constraints is formed by
 * student requirements. Each student can enrol in several classes, so
 * the aim is to minimize the total number of student conflicts among
 * these classes. Such conflicts occur if the student cannot attend two
 * classes to which he or she has enrolled because these classes have
 * overlapping times. Finally, there are some group constraints
 * (additional relations between two or more classes). These may either
 * be hard (required or prohibited), or soft (preferred), similar to the
 * time and room preferences (from -2 to 2).
 * <br>
 * <br>
 * <h2>
 *         Constraints
 * </h2>
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
 * (same as when these classes are overlapping in time). The only
 * exeption is when the first meeting is 90 minutes long -- the
 * acceptable travel distance is 1000 meters, since there is for
 * 15-minute passing period.
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
 * Finally, since all of the classes are at least 60 minutes long, every
 * window of empty time slots of a room that is surrounded by classes on
 * both sides and that is less than 60 minutes long (i.e., the room is
 * not used for 30 minutes between two consecutive classes) is considered
 * useless if no other class can use it. The number of such useless hours
 * should be minimized. Also the situation when a room is occupied by a
 * class which is using less than 2/3 of its seats is discouraged. Both
 * these soft constraints are considered much less important than all the
 * constraints described above.
 * <br>
 * <br>
 * <h2>
 *         Student Sectioning
 * </h2>
 * Many course offerings consist of multiple classes, with students
 * enrolled in the course divided among them. These classes are often
 * linked by a set of constraints, namely:
 * <ul>
 *         <li>
 *                 Each class has a limit stating the maximum number of students who
 *                 can be enrolled in it.
 *                 <li>
 *                         A student must be enrolled in exactly one class for each subpart of
 *                         a course.
 *                         <li>
 *                                 If two subparts of a course have a parent-child relationship, a
 *                                 student enrolled in the parent class must also be enrolled in one
 *                                 of the child classes.
 * </ul>
 * <br>
 * Moreover, some of the classes of an offering may be required or
 * prohibited for certain students, based on reservations that can be set
 * on an offering, a configuration, or a class.
 * <br>
 * <br>
 * Before implementing the solver, an initial sectioning of students into
 * classes is processed. This sectioning is based on Carter's homogeneous
 * sectioning and is intended to minimize future student conflicts.
 * However, it is still possible to improve on the number of student
 * conflicts in the solution. This can be accomplished by moving students
 * between alternative classes of the same course during or after the
 * search.
 * <br>
 * <br>
 * In the current implementation, students are not re-sectioned during
 * the search, but a student re-sectioning algorithm is called after the
 * solver is finished or upon the user's request.
 * <br>
 * <br>
 * Since students are not re-sectioned during the timetabling search, the
 * computed number of student conflicts is really an upper bound on the
 * actual number that may exist afterward. To compensate for this during
 * the search, student conflicts between subparts with multiple classes
 * are weighted lower than conflicts between classes that meet at a
 * single time (i.e., having student conflicts that cannot be avoided by
 * re-sectioning). 
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
package org.cpsolver.coursett;