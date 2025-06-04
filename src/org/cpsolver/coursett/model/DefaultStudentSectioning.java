package org.cpsolver.coursett.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.cpsolver.coursett.model.InitialSectioning.Group;
import org.cpsolver.coursett.sectioning.StudentSwapSectioning;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.InfoProvider;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.termination.TerminationCondition;
import org.cpsolver.ifs.util.Progress;


/**
 * Default implementation of the student sectioning functions needed within the course timetabling solver
 * consisting of {@link InitialSectioning} and {@link FinalSectioning}.
 * <br>
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
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
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
public class DefaultStudentSectioning implements StudentSectioning, InfoProvider<Lecture, Placement> {
    protected TimetableModel iModel = null;
    private Progress iProgress = null;
    protected FinalSectioning iFinalSectioning = null;
    protected boolean iMustFollowReservations = false;
    protected static java.text.DecimalFormat sDF2 = new java.text.DecimalFormat("0.00", new java.text.DecimalFormatSymbols(Locale.US));

    /**
     * Constructor
     * @param model problem model
     */
    public DefaultStudentSectioning(TimetableModel model) {
        iModel = model;
        iFinalSectioning = new FinalSectioning(model);
        iMustFollowReservations = model.getProperties().getPropertyBoolean("StudentSectioning.MustFollowReservations", false);
    }
    
    public Progress getProgress() {
        if (iProgress == null) iProgress = Progress.getInstance(iModel);
        return iProgress;
    }

    /**
     * Enroll students into the given offering during the initial data load using {@link InitialSectioning}.
     * @param offeringId instructional offering id
     * @param courseName course name
     * @param students list of students to be sectioned
     * @param configurations list of configurations the students are to be sectioned into
     */
    @Override
    public void initialSectioning(Assignment<Lecture, Placement> assignment, Long offeringId, String courseName, Collection<Student> students, Collection<Configuration> configurations) {
        if (students == null || students.isEmpty())
            return;
        if (configurations == null || configurations.isEmpty())
            return;
        if (configurations.size() == 1) {
            if (isMustFollowReservations()) {
                Collection<Student> availableStudents = new ArrayList<Student>(students.size());
                Configuration cfg = configurations.iterator().next();
                for (Student st : students) {
                    if (st.canEnroll(cfg)) {
                        availableStudents.add(st);
                        st.addConfiguration(cfg);
                    } else {
                        getProgress().debug("Unable to enroll student " + st.getId() + " in " + courseName);
                    }
                    for (Long subpartId: cfg.getTopSubpartIds()) {
                        initialSectioningLectures(assignment, offeringId, courseName, availableStudents, cfg.getTopLectures(subpartId));
                    }
                }
            } else {
                Configuration cfg = configurations.iterator().next();
                for (Student st : students) {
                    st.addConfiguration(cfg);
                }
                for (Long subpartId: cfg.getTopSubpartIds()) {
                    initialSectioningLectures(assignment, offeringId, courseName, students, cfg.getTopLectures(subpartId));
                }
            }
        } else {
            getProgress().trace("sectioning " + students.size() + " students of course " + courseName + " into " + configurations.size() + " configurations");
            Group[] studentsPerSection = studentsToConfigurations(offeringId, students, configurations);
            for (int i = 0; i < configurations.size(); i++) {
                Group group = studentsPerSection[i];
                getProgress().trace((i + 1) + ". configuration got " + group.getStudents().size() + " students (weighted=" + group.size() + ", cfgLimit=" + group.getConfiguration().getLimit() + ")");
                for (Student st : group.getStudents()) {
                    st.addConfiguration(group.getConfiguration());
                }
                for (Long subpartId: group.getConfiguration().getTopSubpartIds()) {
                    initialSectioningLectures(assignment, offeringId, courseName, group.getStudents(), group.getConfiguration().getTopLectures(subpartId));
                }
            }
            if (isMustFollowReservations()) {
                for (Student st : students) {
                    boolean hasConfig = false;
                    for (Configuration cfg: configurations)
                        if (st.getConfigurations().contains(cfg)) {
                            hasConfig = true;
                            break;
                        }
                    if (!hasConfig)
                        getProgress().debug("Unable to enroll student " + st.getId() + " in " + courseName);
                }
            }
        }
    }
    
    /**
     * Class label
     * @param lecture a class
     * @return class label including a link to be printed in the log
     */
    protected String getClassLabel(Lecture lecture) {
        return "<A href='classDetail.do?cid=" + lecture.getClassId() + "'>" + lecture.getName() + "</A>";
    }
    
    /**
     * Enroll students into the given classes during the initial data load using {@link InitialSectioning}.
     * @param assignment current assignment
     * @param offeringId instructional offering id
     * @param courseName course name
     * @param students list of students to be sectioned
     * @param lectures list of lectures the students are to be sectioned into
     */
    protected void initialSectioningLectures(Assignment<Lecture, Placement> assignment, Long offeringId, String courseName, Collection<Student> students, Collection<Lecture> lectures) {
        if (lectures == null || lectures.isEmpty())
            return;
        if (students == null || students.isEmpty())
            return;
        for (Lecture lecture : lectures) {
            if (lecture.classLimit(assignment) == 0 && !lecture.isCommitted())
                getProgress().warn("Class " + getClassLabel(lecture) + " has zero class limit.");
        }

        getProgress().trace("sectioning " + students.size() + " students of course " + courseName + " into " + lectures.size() + " sections");
        if (lectures.size() == 1) {
            Collection<Student> availableStudents = (isMustFollowReservations() ? new ArrayList<Student>(students.size()) : students);
            Lecture lect = lectures.iterator().next();
            for (Student st : students) {
                if (!st.canEnroll(lect)) {
                    getProgress().debug("Unable to enroll student " + st.getId() + " in class " + getClassLabel(lect));
                    if (isMustFollowReservations()) continue;
                }
                lect.addStudent(assignment, st);
                st.addLecture(lect);
                if (isMustFollowReservations()) availableStudents.add(st);
            }
            if (lect.hasAnyChildren()) {
                for (Long subpartId: lect.getChildrenSubpartIds()) {
                    List<Lecture> children = lect.getChildren(subpartId);
                    initialSectioningLectures(assignment, offeringId, lect.getName(), availableStudents, children);
                }
            }
        } else {
            Group[] studentsPerSection = studentsToLectures(offeringId, students, lectures);
            for (int i = 0; i < studentsPerSection.length; i++) {
                Group group = studentsPerSection[i];
                Lecture lect = group.getLecture();
                if (group.getStudents().isEmpty()) {
                    getProgress().trace("Lecture " + getClassLabel(lect) + " got no students (cl=" + lect.classLimit(assignment) + ")");
                    continue;
                }
                getProgress().trace("Lecture " + getClassLabel(lect) + " got " + group.getStudents().size() + " students (weighted=" + group.size() + ", classLimit=" + lect.classLimit(assignment) + ")");
                List<Student> studentsThisSection = group.getStudents();
                for (Student st : studentsThisSection) {
                    if (!st.canEnroll(lect)) {
                        if (isMustFollowReservations()) {
                            // should not really happen
                            getProgress().info("Unable to enroll student " + st.getId() + " in class " + getClassLabel(lect));
                            continue;
                        } else {
                            getProgress().debug("Unable to enroll student " + st.getId() + " in class " + getClassLabel(lect));
                        }
                    }
                    lect.addStudent(assignment, st);
                    st.addLecture(lect);
                }
                if (lect.hasAnyChildren()) {
                    for (Long subpartId: lect.getChildrenSubpartIds()) {
                        List<Lecture> children = lect.getChildren(subpartId);
                        initialSectioningLectures(assignment, offeringId, lect.getName(), studentsThisSection, children);
                    }
                }
            }
            if (isMustFollowReservations()) {
                for (Student st : students) {
                    boolean hasLecture = false;
                    for (Lecture lect: lectures)
                        if (st.getLectures().contains(lect)) {
                            hasLecture = true;
                            break;
                        }
                    if (!hasLecture)
                        getProgress().debug("Unable to enroll student " + st.getId() + " in " + courseName);
                }
            }
        }
    }
    
    /**
     * Section students into configurations. This method calls the actual initial sectioning {@link InitialSectioning#getGroups()}.
     * @param offeringId instructional offering id
     * @param students list of students to be sectioned
     * @param configurations list of configurations the students are to be sectioned into
     * @return list of {@link Group}
     */
    protected Group[] studentsToConfigurations(Long offeringId, Collection<Student> students, Collection<Configuration> configurations) {
        InitialSectioning sect = new InitialSectioning(getProgress(), offeringId, configurations, students);
        sect.setMustFollowReservations(isMustFollowReservations());
        return sect.getGroups();
    }
    
    /**
     * Section students into lectures. This method calls the actual initial sectioning {@link InitialSectioning#getGroups()}.
     * @param offeringId instructional offering id
     * @param students list of students to be sectioned
     * @param lectures list of lectures the students are to be sectioned into
     * @return list of {@link Group}
     */
    protected Group[] studentsToLectures(Long offeringId, Collection<Student> students, Collection<Lecture> lectures) {
        InitialSectioning sect = new InitialSectioning(getProgress(), offeringId, lectures, students);
        sect.setMustFollowReservations(isMustFollowReservations());
        return sect.getGroups();
    }
    
    /**
     * Return true if final student sectioning is implemented.
     */
    @Override
    public boolean hasFinalSectioning() {
        return true;
    }

    /**
     * Run student final sectioning (switching students between sections of the same
     * class in order to minimize overall number of student conflicts).
     */
    @Override
    public void switchStudents(Solution<Lecture, Placement> solution, TerminationCondition<Lecture, Placement> termination) {
        iFinalSectioning.execute(solution, termination);
    }
    
    /**
     * Perform sectioning on the given lecture
     * @param lecture given lecture
     * @param recursive recursively resection lectures affected by a student swap
     * @param configAsWell resection students between configurations as well
     **/
    @Override
    public void resection(Assignment<Lecture, Placement> assignment, Lecture lecture, boolean recursive, boolean configAsWell) {
        iFinalSectioning.resection(assignment, lecture, recursive, configAsWell);
    }
    
    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info) {
        if (!iModel.getStudentGroups().isEmpty())
            info.put("Student groups", sDF2.format(100.0 * StudentSwapSectioning.group(iModel) / iModel.getStudentGroups().size()) + "%");
    }

    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info, Collection<Lecture> variables) {
        if (!iModel.getStudentGroups().isEmpty())
            info.put("Student groups", sDF2.format(StudentSwapSectioning.gp(iModel, variables)) + "%");
    }
    
    /**
     * Must reservations be followed? When true, a student cannot be placed in a section where {@link Student#canEnroll(Lecture)} is false.
     * Defaults to false.
     */
    public boolean isMustFollowReservations() { return iMustFollowReservations; }
    /**
     * Must reservations be followed? When true, a student cannot be placed in a section where {@link Student#canEnroll(Lecture)} is false.
     * Defaults to false.
     */
    public void setMustFollowReservations(boolean mustFollow) { iMustFollowReservations = mustFollow; }
}
