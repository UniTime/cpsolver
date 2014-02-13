package net.sf.cpsolver.coursett.model;

import java.util.Collection;

/**
 * Interface for student sectioning functions needed within the course timetabling solver.<br>
 * <br>
 * Many course offerings consist of multiple classes, with students enrolled in
 * the course divided among them. These classes are often linked by a set of
 * constraints, namely:
 * <ul>
 * <li>Each class has a limit stating the maximum number of students who can be
 * enrolled in it.
 * <li>A student must be enrolled in exactly one class for each subpart of a
 * course.
 * <li>If two subparts of a course have a parent-child relationship, a student
 * enrolled in the parent class must also be enrolled in one of the child
 * classes.
 * </ul>
 * Moreover, some of the classes of an offering may be required or prohibited
 * for certain students, based on reservations that can be set on an offering, a
 * configuration, or a class. <br>
 * While the data are loaded into the solver, an initial sectioning of students into
 * classes is processed (see {@link InitialSectioning}). However, it
 * is still possible to improve on the number of student conflicts in the
 * solution. This can be accomplished by moving students between alternative
 * classes of the same course during or after the search (see
 * {@link FinalSectioning}).
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2014 Tomas Muller<br>
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
public interface StudentSectioning {

    /**
     * Enroll students into the given offering during the initial data load.
     * @param offeringId instructional offering id
     * @param courseName course name
     * @param students list of students to be sectioned
     * @param configurations list of configurations the students are to be sectioned into
     */
    public void initialSectioning(Long offeringId, String courseName, Collection<Student> students, Collection<Configuration> configurations);
    
    /**
     * Return true if final student sectioning is implemented. 
     */
    public boolean hasFinalSectioning();
    
    /**
     * Run student final sectioning (switching students between sections of the same
     * class in order to minimize overall number of student conflicts).
     */
    public void switchStudents(TimetableModel model);
    
    /**
     * Perform sectioning on the given lecture
     * @param lecture given lecture
     * @param recursive recursively resection lectures affected by a student swap
     * @param configAsWell resection students between configurations as well
     **/
    public void resection(Lecture lecture, boolean recursive, boolean configAsWell);
}
