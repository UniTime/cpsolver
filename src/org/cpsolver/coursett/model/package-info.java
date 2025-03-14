/**
 * University Course Timetabling: Model.
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
 * Wednesday, Friday, beginning at 5 possible times
 * <br>
 * <img src='https://muller.unitime.org/3x100_ex.png'
 *         alt='An example of time preferences for 2 hours x 3 days per week  class'>
 * <br>
 * Or, for instance, a 1 hour x 2 day per week class can be taught only
 * on Monday-Wednesday, Wednesday-Friday or Monday-Friday, beginning at
 * 10 possible times
 * <br>
 * <img src='https://muller.unitime.org/2x50_ex.png'
 *         alt='Fig. 3.2. An example of time preferences for 1 hour x 2 days per week class'>
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
package org.cpsolver.coursett.model;