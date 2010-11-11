package net.sf.cpsolver.coursett.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import net.sf.cpsolver.ifs.util.Progress;

/**
 * Student initial sectioning (before a solver is started). <br>
 * <br>
 * Many course offerings consist of multiple classes, with students enrolled in
 * the course divided among them. These classes are often linked by a set of
 * constraints, namely:
 * <ul>
 * <li>Each class has a limit stating the maximum number of students who can be
 * enrolled in it.
 * <li>A student must be enrolled in exactly one class for each subpart of a
 * course.
 * <li>If two subparts of a course have a parent�child relationship, a student
 * enrolled in the parent class must also be enrolled in one of the child
 * classes.
 * </ul>
 * Moreover, some of the classes of an offering may be required or prohibited
 * for certain students, based on reservations that can be set on an offering, a
 * configuration, or a class. <br>
 * Before implementing the solver, an initial sectioning of students into
 * classes is processed. This sectioning is based on Carter�s homogeneous
 * sectioning and is intended to minimize future student conflicts. However, it
 * is still possible to improve on the number of student conflicts in the
 * solution. This can be accomplished by moving students between alternative
 * classes of the same course during or after the search (see
 * {@link FinalSectioning}).
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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 */
public class InitialSectioning {
    Collection<Student> iStudents = null;
    Group[] iGroups = null;
    Long iOfferingId = null;
    Progress iProgress = null;

    public InitialSectioning(Progress progress, Long offeringId, Collection<?> lectureOrConfigurations,
            Collection<Student> students) {
        iOfferingId = offeringId;
        iStudents = new HashSet<Student>(students);
        iProgress = progress;

        iGroups = new Group[lectureOrConfigurations.size()];

        int idx = 0;
        double total = 0;
        for (Iterator<?> i = lectureOrConfigurations.iterator(); i.hasNext(); idx++) {
            Object lectureOrConfiguration = i.next();
            if (lectureOrConfiguration instanceof Lecture) {
                Lecture lecture = (Lecture) lectureOrConfiguration;
                iGroups[idx] = new Group(lecture);
            } else {
                Configuration configuration = (Configuration) lectureOrConfiguration;
                iGroups[idx] = new Group(configuration);
            }
            total += iGroups[idx].getMaxSize();
        }

        if (total == 0) {
            for (idx = 0; idx < iGroups.length; idx++) {
                iGroups[idx].setMaxSize(1);
                total++;
            }
        }

        double studentsWeight = 0;
        for (Student s : iStudents) {
            studentsWeight += s.getOfferingWeight(iOfferingId);
        }

        double factor = studentsWeight / total;
        for (idx = 0; idx < iGroups.length; idx++) {
            iGroups[idx].setMaxSize(factor * iGroups[idx].getMaxSize());
            iGroups[idx].setMinSize(Math.min(iGroups[idx].getMinSize(), 0.9 * iGroups[idx].getMaxSize()));
        }

        progress.trace("Initial sectioning:");
        progress.trace("  going to section " + iStudents.size() + " into " + total + " seats");
        for (idx = 0; idx < iGroups.length; idx++)
            progress.trace("    " + (idx + 1) + ". group can accomodate <" + iGroups[idx].getMinSize() + ","
                    + iGroups[idx].getMaxSize() + "> students");
    }

    public void addStudent(Student student) {
        iStudents.add(student);
    }

    private boolean moveAwayOneStudent(Group group) {
        Group newGroup = null;
        Student movingStudent = null;
        double curDist = 0, newDist = 0;

        for (Student student : group.getStudents()) {
            if (group.isEnrolled(student))
                continue;
            double cd = group.getDistance(student);
            for (int x = 0; x < iGroups.length; x++) {
                if (group.equals(iGroups[x]))
                    continue;
                if (iGroups[x].size() > iGroups[x].getMaxSize())
                    continue;
                if (!iGroups[x].canEnroll(student))
                    continue;
                double nd = iGroups[x].getDistance(student);
                if (newGroup == null || newDist - curDist < nd - cd) {
                    newGroup = iGroups[x];
                    movingStudent = student;
                    curDist = cd;
                    newDist = nd;
                    if (newDist - curDist < 0.01)
                        break;
                }
            }
        }

        if (newGroup != null) {
            group.removeStudent(movingStudent);
            newGroup.addStudent(movingStudent);
            return true;
        }

        return false;
    }

    public boolean moveIntoOneStudent(Group group) {
        Group oldGroup = null;
        Student movingStudent = null;
        double curDist = 0, newDist = 0;

        for (int x = 0; x < iGroups.length; x++) {
            if (group.equals(iGroups[x]))
                continue;
            if (iGroups[x].size() <= iGroups[x].getMinSize())
                continue;
            for (Student student : iGroups[x].getStudents()) {
                if (!group.canEnroll(student))
                    continue;
                if (iGroups[x].isEnrolled(student))
                    continue;
                double cd = iGroups[x].getDistance(student);
                double nd = group.getDistance(student);
                if (oldGroup == null || newDist - curDist < nd - cd) {
                    oldGroup = iGroups[x];
                    movingStudent = student;
                    curDist = cd;
                    newDist = nd;
                    if (newDist - curDist < 0.01)
                        break;
                }
            }
        }

        if (oldGroup != null) {
            oldGroup.removeStudent(movingStudent);
            group.addStudent(movingStudent);
            return true;
        }

        return false;
    }

    public Group[] getGroups() {
        double minDist = 1.0 / iGroups.length;

        for (Iterator<Student> i = iStudents.iterator(); i.hasNext();) {
            Student student = i.next();
            Group group = null;
            for (int idx = 0; idx < iGroups.length; idx++) {
                if (iGroups[idx].isEnrolled(student)) {
                    group = iGroups[idx];
                    break;
                }
            }
            if (group != null) {
                group.addStudent(student);
                i.remove();
            }
        }

        for (Student student : iStudents) {
            double studentWeight = student.getOfferingWeight(iOfferingId);

            Group group = null;
            double dist = 0.0;
            for (int idx = 0; idx < iGroups.length; idx++) {
                Group g = iGroups[idx];
                if (!g.canEnroll(student))
                    continue;
                if (g.size() + studentWeight > g.getMaxSize())
                    continue;
                double d = g.getDistance(student);
                if (group == null || d < dist) {
                    group = g;
                    dist = d;
                    if (d < minDist)
                        break;
                }
            }

            if (group != null) {
                group.addStudent(student);
                continue;
            }

            // disobey max size
            for (int idx = 0; idx < iGroups.length; idx++) {
                Group g = iGroups[idx];
                if (!g.canEnroll(student))
                    continue;
                double d = g.getDistance(student);
                if (group == null || d < dist) {
                    group = g;
                    dist = d;
                }
            }

            if (group != null) {
                group.addStudent(student);
                continue;
            }

            // disobey max size & can enroll
            for (int idx = 0; idx < iGroups.length; idx++) {
                Group g = iGroups[idx];
                double d = g.getDistance(student);
                if (group == null || d < dist) {
                    group = g;
                    dist = d;
                }
            }

            iProgress.debug("Unable to find a valid section for student "
                    + student.getId()
                    + ", enrolling to "
                    + (group.getLecture() != null ? group.getLecture().getName() : group.getConfiguration()
                            .getConfigId().toString()));

            group.addStudent(student);
        }

        for (int idx = 0; idx < iGroups.length; idx++) {
            Group group = iGroups[idx];

            while (group.size() > group.getMaxSize()) {
                if (!moveAwayOneStudent(group))
                    break;
            }

            while (group.size() > group.getMaxSize()) {

                Group newGroup = null;
                Student movingStudent = null;

                for (Student student : group.getStudents()) {
                    if (group.isEnrolled(student))
                        continue;
                    for (int x = 0; x < iGroups.length; x++) {
                        if (idx == x)
                            continue;
                        if (!iGroups[x].canEnroll(student))
                            continue;
                        while (iGroups[x].size() + student.getOfferingWeight(iOfferingId) > iGroups[x].getMaxSize()) {
                            if (!moveAwayOneStudent(iGroups[x]))
                                break;
                        }
                        if (iGroups[x].size() + student.getOfferingWeight(iOfferingId) > iGroups[x].getMaxSize())
                            continue;
                        newGroup = iGroups[x];
                        movingStudent = student;
                        break;
                    }
                    if (newGroup != null)
                        break;
                }

                if (newGroup != null) {
                    group.removeStudent(movingStudent);
                    newGroup.addStudent(movingStudent);
                } else {
                    break;
                }
            }

            while (group.size() < group.getMinSize()) {
                if (!moveIntoOneStudent(group))
                    break;
            }
        }

        return iGroups;
    }

    public class Group {
        private ArrayList<Student> iStudents = new ArrayList<Student>();
        private Lecture iLecture = null;
        private Configuration iConfiguration = null;
        private Double iDist = null;
        private double iMinSize = 0, iMaxSize = 0;
        private HashMap<Student, Double> iDistCache = new HashMap<Student, Double>();
        private double iSize = 0.0;

        public Group(Lecture lecture) {
            iLecture = lecture;
            iMinSize = lecture.minClassLimit();
            iMaxSize = lecture.maxAchievableClassLimit();
        }

        public Group(Configuration configuration) {
            iConfiguration = configuration;
            iMinSize = iMaxSize = iConfiguration.getLimit();
        }

        public Configuration getConfiguration() {
            return iConfiguration;
        }

        public Lecture getLecture() {
            return iLecture;
        }

        public double getMinSize() {
            return iMinSize;
        }

        public double getMaxSize() {
            return iMaxSize;
        }

        public void setMinSize(double minSize) {
            iMinSize = minSize;
        }

        public void setMaxSize(double maxSize) {
            iMaxSize = maxSize;
        }

        public double getDistance() {
            if (iDist == null) {
                double dist = 0.0;
                double prob = 10.0 / iStudents.size();
                int cnt = 0;
                for (Student s1 : iStudents) {
                    if (Math.random() < prob) {
                        for (Student s2 : iStudents) {
                            if (s1.getId().compareTo(s2.getId()) <= 0)
                                continue;
                            if (Math.random() < prob) {
                                dist += s1.getDistance(s2);
                                cnt++;
                            }
                        }
                    }
                }
                iDist = new Double(dist / cnt);
            }
            return iDist.doubleValue();
        }

        public double getDistance(Student student) {
            Double cachedDist = iDistCache.get(student);
            if (cachedDist != null)
                return cachedDist.doubleValue();
            double dist = 0.0;
            double prob = 10.0 / iStudents.size();
            int cnt = 0;
            for (Student s : iStudents) {
                if (prob >= 1.0 || Math.random() < prob) {
                    dist += s.getDistance(student);
                    cnt++;
                }
            }
            iDistCache.put(student, new Double(dist / cnt));
            return dist / cnt;
        }

        public void addStudent(Student student) {
            iStudents.add(student);
            iSize += student.getOfferingWeight(iOfferingId);
            iDist = null;
            iDistCache.clear();
        }

        public void removeStudent(Student student) {
            iStudents.remove(student);
            iSize -= student.getOfferingWeight(iOfferingId);
            iDist = null;
            iDistCache.clear();
        }

        public List<Student> getStudents() {
            return iStudents;
        }

        public double size() {
            return iSize;
        }

        @Override
        public String toString() {
            return "{" + size() + "-" + getDistance() + "/" + getStudents() + "}";
        }

        public boolean canEnroll(Student student) {
            if (getLecture() != null) {
                return student.canEnroll(getLecture());
            } else {
                for (Long subpartId: getConfiguration().getTopSubpartIds()) {
                    boolean canEnrollThisSubpart = false;
                    for (Lecture lecture : getConfiguration().getTopLectures(subpartId)) {
                        if (student.canEnroll(lecture)) {
                            canEnrollThisSubpart = true;
                            break;
                        }
                    }
                    if (!canEnrollThisSubpart)
                        return false;
                }
                return true;
            }
        }

        public boolean isEnrolled(Student student) {
            if (getLecture() != null) {
                return student.getLectures().contains(getLecture());
            } else {
                for (Lecture lect : student.getLectures()) {
                    if (lect.getConfiguration().equals(getConfiguration()))
                        return true;
                }
                return false;
            }

        }
    }

    public static void initialSectioningCfg(Progress p, Long offeringId, String courseName, Collection<Student> students,
            List<Configuration> configurations) {
        if (students == null || students.isEmpty())
            return;
        if (configurations == null || configurations.isEmpty())
            return;
        if (configurations.size() == 1) {
            Configuration cfg = configurations.get(0);
            for (Student st : students) {
                st.addConfiguration(cfg);
            }
            for (Long subpartId: cfg.getTopSubpartIds()) {
                initialSectioning(p, offeringId, courseName, students, cfg.getTopLectures(subpartId));
            }
        } else {
            p.trace("sectioning " + students.size() + " students of course " + courseName + " into "
                    + configurations.size() + " configurations");
            InitialSectioning sect = new InitialSectioning(p, offeringId, configurations, students);
            Group[] studentsPerSection = sect.getGroups();
            for (int i = 0; i < configurations.size(); i++) {
                Group group = studentsPerSection[i];
                p.trace((i + 1) + ". configuration got " + group.getStudents().size() + " students (weighted="
                        + group.size() + ", cfgLimit=" + group.getConfiguration().getLimit() + ")");
                for (Student st : group.getStudents()) {
                    st.addConfiguration(group.getConfiguration());
                }
                for (Long subpartId: group.getConfiguration().getTopSubpartIds()) {
                    initialSectioning(p, offeringId, courseName, group.getStudents(), group.getConfiguration()
                            .getTopLectures(subpartId));
                }
            }
        }
    }

    private static String getClassLabel(Lecture lecture) {
        return "<A href='classDetail.do?cid=" + lecture.getClassId() + "'>" + lecture.getName() + "</A>";
    }

    private static void initialSectioning(Progress p, Long offeringId, String parentName, Collection<Student> students,
            Collection<Lecture> lectures) {
        if (lectures == null || lectures.isEmpty())
            return;
        if (students == null || students.isEmpty())
            return;
        for (Lecture lecture : lectures) {
            if (lecture.classLimit() == 0 && !lecture.isCommitted())
                p.warn("Class " + getClassLabel(lecture) + " has zero class limit.");
        }

        p.trace("sectioning " + students.size() + " students of course " + parentName + " into " + lectures.size()
                + " sections");
        if (lectures.size() == 1) {
            Lecture lect = lectures.iterator().next();
            for (Student st : students) {
                if (!st.canEnroll(lect)) {
                    p.info("Unable to enroll student " + st.getId() + " in class " + getClassLabel(lect));
                }
                lect.addStudent(st);
                st.addLecture(lect);
            }
            if (lect.hasAnyChildren()) {
                for (Long subpartId: lect.getChildrenSubpartIds()) {
                    List<Lecture> children = lect.getChildren(subpartId);
                    initialSectioning(p, offeringId, lect.getName(), students, children);
                }
            }
        } else {
            InitialSectioning sect = new InitialSectioning(p, offeringId, lectures, students);
            Group[] studentsPerSection = sect.getGroups();
            for (int i = 0; i < studentsPerSection.length; i++) {
                Group group = studentsPerSection[i];
                Lecture lect = group.getLecture();
                if (group.getStudents().isEmpty()) {
                    p.trace("Lecture " + getClassLabel(lect) + " got no students (cl=" + lect.classLimit() + ")");
                    continue;
                }
                p.trace("Lecture " + getClassLabel(lect) + " got " + group.getStudents().size()
                        + " students (weighted=" + group.size() + ", classLimit=" + lect.classLimit() + ")");
                List<Student> studentsThisSection = group.getStudents();
                for (Student st : studentsThisSection) {
                    if (!st.canEnroll(lect)) {
                        p.info("Unable to enroll student " + st.getId() + " in class " + getClassLabel(lect));
                    }
                    lect.addStudent(st);
                    st.addLecture(lect);
                }
                if (lect.hasAnyChildren()) {
                    for (Long subpartId: lect.getChildrenSubpartIds()) {
                        List<Lecture> children = lect.getChildren(subpartId);
                        initialSectioning(p, offeringId, lect.getName(), studentsThisSection, children);
                    }
                }
            }
        }
    }

}