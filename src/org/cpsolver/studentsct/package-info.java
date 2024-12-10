/**
 * <h1>
 *         Student Sectioning Solver.
 * </h1>
 * <br>
 * <br>
 * Student sectioning is often considered as a subproblem of course
 * timetabling. Once a timetable has been developed, the object is to
 * assign students to classes (i.e., individual sections of a course) in
 * order to minimize conflicts while respecting individual student course
 * requests and preferences along with various other constraints.
 * <br>
 * <br>
 * Before and during the construction of the timetable course demands are
 * collected from students. During this preregistration process, each
 * student can indicate the list of requested courses together with his
 * or her preferences. These preferences contain course priorities (order
 * of courses based on their importance for the student), alternative
 * course requests (each course request can have one or more alternative
 * courses), free time requests, wait-list preferences (if a student
 * cannot be enrolled into the course, e.g., because of the space
 * available, should he or she be assigned to the appropriate wait-list
 * for the course), and additional schedule distribution preferences.
 * <br>
 * <br>
 * <h2>
 *         Initial Sectioning
 * </h2>
 * During the construction of the course timetable, course demands of
 * already pre-registered students are considered. Since many students
 * are anticipated to register later in the process, projected course
 * demands are considered as well. These are deducted from the last-like
 * semester enrollments, e.g., Fall 2006 course enrollments are used to
 * predict Fall 2007 course demands. Minimization of potential student
 * conflicts is one of the optimization criteria of the timetabling
 * solver. Two classes are conflicting, i.e., they cannot be attended by
 * the same students, if they are overlapping in time or if they are
 * back-to-back (the second class starts just after the first ends) being
 * placed in rooms that are too far apart.
 * <br>
 * Before the course timetabling solver is started, an initial sectioning
 * of students into classes is processed. This sectioning is based on
 * Carters (Carter 2001) homogeneous sectioning and is intended to
 * minimize future student conflicts. However, it is still possible to
 * improve on the number of student conflicts in the solution. This is
 * accomplished by moving students between alternative classes of the
 * same course during or after the search for a timetabling solution.
 * <br>
 * <br>
 * <h2>
 *         Batch Sectioning
 * </h2>
 * After the course timetable for the whole university is constructed,
 * batch student sectioning process is executed. In this process, all
 * pre-registered students are assigned to specific sections (classes) of
 * courses in order to minimize conflicts as well as optimize preferences
 * provided by students. Additional constraints deducted from the course
 * structure as well as various reservations, that can be put on courses
 * or particular classes, are respected. Students that were not able to
 * get a requested course (or any of the provided alternatives) are
 * enrolled to the appropriate waitlists.
 * <br>
 * The batch student sectioning is also using the projected student
 * demands to compute the expected number of students in each class for
 * the following online phase, however, pre-registered students take
 * precedence before projected student demands. This means that a
 * pre-registered student cannot be bumped out a requested course because
 * of a projected student, but he or she may end up with a class which
 * does not prevent projected students to take the course as well. Based
 * on the computed solution, pre-registered students are assigned to
 * classes and wait-lists and the projected students demands are used to
 * identify space in each section that is to be reserved for students
 * that are not yet registered. This information is then used in the
 * online sectioning phase in order to direct already registered students
 * from sections that are expected to be taken by the future students.
 * <br>
 * <br>
 * <h2>
 *         Online Sectioning
 * </h2>
 * After the first student schedule is created, till the begining of the
 * semester, students can make changes in their schedules using the
 * online interface. During this phase, existing students are allowed to
 * remove themself from the requested courses or request additional
 * courses and a new sectioning solution is provided to them in realtime.
 * They can also change their class enrollments if there are other
 * classes of the course that are available or wait-list themselves on
 * classes that are not available. Wait-lists are automatically processed
 * as the space frees on courses and classes. Some changes in the course
 * timetable might occur as well, potentiality causing some re-sectioning
 * of existing students. New students are using the same interface as
 * existing students. They start with the course demands first, based on
 * which they are sectioned to courses in real-time, and then they can
 * continue as existing students.
 * <br>
 * As students submit schedule requests, each course is ranked in
 * priority order. During realtime sectioning of a student, the search
 * employs a backtracking process considering possible assignments
 * beginning with those classes associated with the students highest
 * priority course. As it evaluates each possible assignment, it compares
 * available space with the space expected to be taken by the future
 * students for each class. This difference between available space and
 * the expected need for each class is used to direct students away from
 * class assignments that would result in excess demand, however, in no
 * case is an eligible student blocked from scheduling a course offering
 * as a result of expected future demand. As students are assigned to
 * specific classes during the sectioning process, the expected demand
 * for each class is adjusted to reflect the assignment.
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
package org.cpsolver.studentsct;