package net.sf.cpsolver.coursett.custom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import net.sf.cpsolver.coursett.model.Configuration;
import net.sf.cpsolver.coursett.model.DefaultStudentSectioning;
import net.sf.cpsolver.coursett.model.InitialSectioning;
import net.sf.cpsolver.coursett.model.InitialSectioning.Group;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Student;
import net.sf.cpsolver.coursett.model.StudentSectioning;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.util.Progress;

/**
 * Deterministic implementation of the initial student sectioning. This class assign students to groups in
 * a deterministic way. Students are ordered by their academic information (curriculum) and unique ids and 
 * assigned in this order to the first available group (configuration or lecture). See {@link StudentSectioning}
 * and {@link DefaultStudentSectioning} for more details about sectioning students during course timetabling.
 * <br><br>
 * This deterministic sectioning can be enabled by setting the following parameter:<ul>
 * <code>StudentSectioning.Class=net.sf.cpsolver.coursett.custom.DeterministicStudentSectioning</code>
 * </ul>
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2014 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          <a href="http://muller.unitime.org">http://muller.unitime.org</a><br>
 *          This customization has been implemented for <a href='http://www.agh.pl'>AGH, Poland</a>.<br>
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
public class DeterministicStudentSectioning extends DefaultStudentSectioning {
    public DeterministicStudentSectioning(TimetableModel model) {
        super(model);
    }
    
    @Override
    protected Group[] studentsToConfigurations(Long offeringId, Collection<Student> students, Collection<Configuration> configurations) {
        DeterministicInitialSectioning sect = new DeterministicInitialSectioning(getProgress(), offeringId, configurations, students);
        return sect.getGroups();
    }
    
    @Override
    protected Group[] studentsToLectures(Long offeringId, Collection<Student> students, Collection<Lecture> lectures) {
        Set<Lecture> sortedLectures = new TreeSet<Lecture>(new Comparator<Lecture>() {
            @Override
            public int compare(Lecture l1, Lecture l2) {
                return l1.getClassId().compareTo(l2.getClassId());
            }
        });
        sortedLectures.addAll(lectures);
        DeterministicInitialSectioning sect = new DeterministicInitialSectioning(getProgress(), offeringId, sortedLectures, students);
        return sect.getGroups();
    }
    
    /**
     * No re-sectioning (final sectioning) during deterministic student sectioning.
     */
    @Override
    public boolean hasFinalSectioning() {
        return false;
    }

    /**
     * No re-sectioning (final sectioning) during deterministic student sectioning.
     */
    @Override
    public void switchStudents(TimetableModel model) {
    }

    /**
     * No re-sectioning (final sectioning) during deterministic student sectioning.
     */
    @Override
    public void resection(Lecture lecture, boolean recursive, boolean configAsWell) {
    }

    /**
     * Assign students to groups in a deterministic way, i.e., first student to first available group etc.
     * @author Tomas Muller
     */
    public class DeterministicInitialSectioning extends InitialSectioning implements Comparator<Student> {
        
        public DeterministicInitialSectioning(Progress progress, Long offeringId, Collection<?> lectureOrConfigurations, Collection<Student> students) {
            super(progress, offeringId, lectureOrConfigurations, students);
            
            // Sort students by their curriculum information first
            iStudents = new TreeSet<Student>(this); iStudents.addAll(students);
        }
        
        @Override
        protected void tweakSizes(double total) {
            double studentsWeight = 0;
            for (Student s : iStudents) {
                studentsWeight += s.getOfferingWeight(iOfferingId);
            }
            
            // if there is not enough space for the given students 
            if (studentsWeight > total) {
                if (total == 0) {
                    // all limits are zero -> spread students equally
                    for (int idx = 0; idx < iGroups.length; idx++)
                        iGroups[idx].setMaxSize(total / iGroups.length);
                } else {
                    // enlarge sections proportionally
                    double factor = studentsWeight / total;
                    for (int idx = 0; idx < iGroups.length; idx++) {
                        iGroups[idx].setMaxSize(factor * iGroups[idx].getMaxSize());
                        iGroups[idx].setMinSize(Math.min(iGroups[idx].getMinSize(), 0.9 * iGroups[idx].getMaxSize()));
                   }
                }
            }
        }
        
        @Override
        public Group[] getGroups() {
            // Assign already enrolled students first
            students: for (Iterator<Student> i = iStudents.iterator(); i.hasNext();) {
                Student student = i.next();
                for (int idx = 0; idx < iGroups.length; idx++) {
                    if (iGroups[idx].isEnrolled(student)) {
                        iGroups[idx].addStudent(student);
                        i.remove();
                        continue students;
                    }
                }
            }

            // For all other (not enrolled) students
            students: for (Student student : iStudents) {
                double studentWeight = student.getOfferingWeight(iOfferingId);
                
                // enroll into first available group with enough space
                for (int idx = 0; idx < iGroups.length; idx++) {
                    Group g = iGroups[idx];
                    if (!g.canEnroll(student) || g.size() >= g.getMaxSize()) continue;
                    // group found -> enroll and continue with the next student
                    g.addStudent(student);
                    continue students;
                }

                // disobey max size, but prefer sections with smallest excess
                Group group = null; int excess = 0;
                for (int idx = 0; idx < iGroups.length; idx++) {
                    Group g = iGroups[idx];
                    if (!g.canEnroll(student)) continue;
                    int ex = (int)Math.round(g.size() + studentWeight - g.getMaxSize());
                    if (group == null || ex < excess) {
                        group = g;
                        excess = ex;
                    }
                }

                if (group != null) {
                    group.addStudent(student);
                    continue;
                }

                // put the student into the first group
                getProgress().debug("Unable to find a valid section for student " + student.getId() + ", enrolling to " + (iGroups[0].getLecture() != null ? iGroups[0].getLecture().getName() : iGroups[0].getConfiguration().getConfigId().toString()));
                iGroups[0].addStudent(student);
            }

            // try to move students away from groups with an excess of more than a student
            for (int idx = 0; idx < iGroups.length; idx++) {
                Group group = iGroups[idx];
                if (group.size() > group.getMaxSize()) {
                    // for each student of a group that is not enrolled in it
                    for (Student student : new ArrayList<Student>(group.getStudents())) {
                        if (group.isEnrolled(student)) continue;
                        // excess of a fraction of a student is allowed
                        if (group.size() - student.getOfferingWeight(iOfferingId) < group.getMaxSize()) continue; 
                        // look for an available group with enough space
                        for (int x = 0; x < iGroups.length; x++) {
                            if (idx == x) continue;
                            if (!iGroups[x].canEnroll(student) || iGroups[x].size() >= iGroups[x].getMaxSize()) continue;
                            // group found, move the student away
                            group.removeStudent(student);
                            iGroups[x].addStudent(student);
                            break;
                        }
                        if (group.size() <= group.getMaxSize()) break;
                    }
                }
            }

            return iGroups;
        }

        /**
         * Sort students by their curriculum information and id
         */
        @Override
        public int compare(Student s1, Student s2) {
            int cmp = (s1.getCurriculum() == null ? "" : s1.getCurriculum()).compareToIgnoreCase(s2.getCurriculum() == null ? "" : s2.getCurriculum());
            if (cmp != 0) return cmp;
            cmp = (s1.getAcademicArea() == null ? "" : s1.getAcademicArea()).compareToIgnoreCase(s2.getAcademicArea() == null ? "" : s2.getAcademicArea());
            if (cmp != 0) return cmp;
            cmp = (s1.getMajor() == null ? "" : s1.getMajor()).compareToIgnoreCase(s2.getMajor() == null ? "" : s2.getMajor());
            if (cmp != 0) return cmp;
            cmp = (s1.getAcademicClassification() == null ? "" : s1.getAcademicClassification()).compareToIgnoreCase(s2.getAcademicClassification() == null ? "" : s2.getAcademicClassification());
            if (cmp != 0) return cmp;
            return s1.getId().compareTo(s2.getId());
        }
    }
}
