package org.cpsolver.coursett.sectioning;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cpsolver.coursett.constraint.JenrlConstraint;
import org.cpsolver.coursett.criteria.StudentConflict;
import org.cpsolver.coursett.model.Configuration;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.Student;
import org.cpsolver.coursett.model.StudentGroup;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;

/**
 * Student swap move. Two students of the same offering are swapped between each other
 * or a student is given some other (alternative) enrollment into the given offering. 
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2017 Tomas Muller<br>
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
public class StudentSwap implements StudentMove {
    private TimetableModel iModel;
    private Student iFirstStudent, iSecondStudent;
    private Set<Lecture> iFirstLectures, iSecondLectures;
    private Configuration iFirstConfig, iSecondConfig;
    private boolean iAllowed = true;
    
    /**
     * Create a swap of two students of an offering. 
     */
    public StudentSwap(TimetableModel model, Assignment<Lecture, Placement> assignment, Student firstStudent, Student secondStudent, Long offeringId) {
        iModel = model;
        iFirstStudent = firstStudent; iSecondStudent = secondStudent;
        iAllowed = initSwap(assignment, offeringId);
    }
    
    /**
     * Move a student into some other lectures of the offering.  
     */
    public StudentSwap(TimetableModel model, Assignment<Lecture, Placement> assignment, Student student, Collection<Lecture> lectures) {
        iModel = model;
        iFirstStudent = student; iSecondStudent = null;
        iAllowed = initMove(assignment, lectures);
    }
    
    /**
     * Compute student swap and its validity
     */
    protected boolean initSwap(Assignment<Lecture, Placement> assignment, Long offeringId) {
        double w1 = iFirstStudent.getOfferingWeight(offeringId), w2 = iSecondStudent.getOfferingWeight(offeringId);
        iFirstLectures = new HashSet<Lecture>();
        for (Lecture lecture: iFirstStudent.getLectures()) {
            if (lecture.getConfiguration() == null) return false;
            if (lecture.getConfiguration().getOfferingId().equals(offeringId) && !iSecondStudent.getLectures().contains(lecture)) {
                iFirstLectures.add(lecture);
                if (iFirstConfig == null)
                    iFirstConfig = lecture.getConfiguration();
                if (!iSecondStudent.canEnroll(lecture) || !iFirstStudent.canUnenroll(lecture)) return false;
                if (w2 - w1 > 0.0 && lecture.nrWeightedStudents() - w1 + w2 > sEps + lecture.classLimit(assignment)) return false;
            }
        }
        iSecondLectures = new HashSet<Lecture>();
        for (Lecture lecture: iSecondStudent.getLectures()) {
            if (lecture.getConfiguration() == null) return false;
            if (lecture.getConfiguration().getOfferingId().equals(offeringId) && !iFirstStudent.getLectures().contains(lecture)) {
                iSecondLectures.add(lecture);
                if (iSecondConfig == null)
                    iSecondConfig = lecture.getConfiguration();
                if (!iFirstStudent.canEnroll(lecture) || !iSecondStudent.canUnenroll(lecture)) return false;
                if (w1 - w2 > 0.0 && lecture.nrWeightedStudents() - w2 + w1 > sEps + lecture.classLimit(assignment)) return false;
            }
        }
        return !iFirstLectures.isEmpty() && !iSecondLectures.isEmpty();
    }
    
    /**
     * Compute student move and its validity
     */
    private boolean initMove(Assignment<Lecture, Placement> assignment, Collection<Lecture> lectures) {
        double w = 0.0;
        iFirstLectures = new HashSet<Lecture>();
        iSecondLectures = new HashSet<Lecture>();
        for (Lecture lecture: lectures) {
            if (lecture.getConfiguration() == null) return false;
            if (!iFirstStudent.getLectures().contains(lecture)) {
                if (iSecondConfig == null) {
                    iSecondConfig = lecture.getConfiguration();
                    w = iFirstStudent.getOfferingWeight(iSecondConfig);
                }
                iSecondLectures.add(lecture);
                if (lecture.nrWeightedStudents() + w > sEps + lecture.classLimit(assignment)) return false;
            }
        }
        if (iSecondLectures.isEmpty()) return false;
        iFirstLectures = new HashSet<Lecture>();
        for (Lecture lecture: iFirstStudent.getLectures()) {
            if (lecture.getConfiguration() == null) return false;
            if (lecture.getConfiguration().getOfferingId().equals(iSecondConfig.getOfferingId()) && !lectures.contains(lecture)) {
                iFirstLectures.add(lecture);
                if (iFirstConfig == null) { iFirstConfig = lecture.getConfiguration(); }
                if (lecture.getClassLimitConstraint() != null && lecture.nrWeightedStudents() < sEps + lecture.minClassLimit()) return false;
            }
        }
        if (iFirstLectures.isEmpty()) return false;
        return true;
    }
    
    /**
     * Decrement {@link JenrlConstraint} between the given two classes by the given student
     */
    protected void decJenrl(Assignment<Lecture, Placement> assignment, Student student, Lecture l1, Lecture l2) {
        if (l1.equals(l2)) return;
        JenrlConstraint jenrl = l1.jenrlConstraint(l2);
        if (jenrl != null) {
            jenrl.decJenrl(assignment, student);
            /*
            if (jenrl.getNrStudents() == 0) {
                jenrl.getContext(assignment).unassigned(assignment, null);
                Object[] vars = jenrl.variables().toArray();
                for (int k = 0; k < vars.length; k++)
                    jenrl.removeVariable((Lecture) vars[k]);
                iModel.removeConstraint(jenrl);
            }
            */
        }
    }
    
    /**
     * Increment {@link JenrlConstraint} between the given two classes by the given student
     */
    protected void incJenrl(Assignment<Lecture, Placement> assignment, Student student, Lecture l1, Lecture l2) {
        if (l1.equals(l2)) return;
        JenrlConstraint jenrl = l1.jenrlConstraint(l2);
        if (jenrl == null) {
            jenrl = new JenrlConstraint();
            jenrl.addVariable(l1);
            jenrl.addVariable(l2);
            iModel.addConstraint(jenrl);
        }
        jenrl.incJenrl(assignment, student);
    }
    
    /**
     * Compute student conflict weigth between two classes.
     */
    public double getJenrConflictWeight(List<StudentConflict> criteria, Student student, Placement p1, Placement p2) {
        if (p1 == null || p2 == null) return 0.0;
        if (criteria == null) {
            if (JenrlConstraint.isInConflict(p1, p2, iModel.getDistanceMetric(), iModel.getStudentWorkDayLimit()))
                return student.getJenrlWeight(p1.variable(), p2.variable());
            return 0.0;
        }

        double weight = 0.0;
        for (StudentConflict sc: criteria)
            if (sc.isApplicable(student, p1.variable(), p2.variable()) && sc.inConflict(p1, p2))
                weight += sc.getWeight() * student.getJenrlWeight(p1.variable(), p2.variable());
        return weight;
    }
    
    @Override
    public double value(Assignment<Lecture, Placement> assignment) {
        return value(iModel == null ? null : iModel.getStudentConflictCriteria(), assignment);
    }
    
    private double groupValue(Student student, Student other, Lecture lecture) {
        if (student.getGroups().isEmpty()) return 0.0;
        double ret = 0.0;
        for (StudentGroup g: student.getGroups()) {
            ret += groupValue(g, student, other, lecture);
        }
        return ret;
    }
    
    private double groupValue(StudentGroup group, Student student, Student other, Lecture lecture) {
        int match = 0;
        for (Student s: lecture.students()) {
            if (s.equals(student) || s.equals(other)) continue;
            if (s.getGroups().contains(group)) match ++;
        }
        if (match > 0) {
            double total = group.countStudents(lecture.getConfiguration().getOfferingId());
            return 2.0 * match / (total * (total - 1.0)) / group.countOfferings();
        } else
            return 0.0;
    }
    
    private double groupValue(Student student, Student other, Configuration config, Set<Lecture> lectures) {
        double ret = 0.0;
        for (Lecture lecture: lectures)
            ret += groupValue(student, other, lecture);
        return ret / config.countSubparts();
    }
    
    @Override
    public double value(List<StudentConflict> criteria, Assignment<Lecture, Placement> assignment) {
        double delta = 0;
        for (Lecture l1: iFirstLectures) {
            Placement p1 = assignment.getValue(l1);
            if (p1 == null) continue;
            for (Lecture l2: iFirstStudent.getLectures()) {
                Placement p2 = assignment.getValue(l2);
                if (l1.equals(l2) || p2 == null) continue;
                if (iFirstLectures.contains(l2) && l1.getClassId().compareTo(l2.getClassId()) >= 0) continue;
                delta -= getJenrConflictWeight(criteria, iFirstStudent, p1, p2);
            }
            if (iSecondStudent != null) {
                for (Lecture l2: iSecondStudent.getLectures()) {
                    if (iSecondLectures.contains(l2)) continue;
                    Placement p2 = assignment.getValue(l2);
                    if (l1.equals(l2) || p2 == null) continue;
                    delta += getJenrConflictWeight(criteria, iSecondStudent, p1, p2);
                }
                for (Lecture l2: iFirstLectures) {
                    Placement p2 = assignment.getValue(l2);
                    if (l1.equals(l2) || p2 == null) continue;
                    if (l1.getClassId().compareTo(l2.getClassId()) >= 0) continue;
                    delta += getJenrConflictWeight(criteria, iSecondStudent, p1, p2);
                }
            }
        }
        for (Lecture l1: iSecondLectures) {
            Placement p1 = assignment.getValue(l1);
            if (p1 == null) continue;
            if (iSecondStudent != null) {
                for (Lecture l2: iSecondStudent.getLectures()) {
                    Placement p2 = assignment.getValue(l2);
                    if (l1.equals(l2) || p2 == null) continue;
                    if (iSecondLectures.contains(l2) && l1.getClassId().compareTo(l2.getClassId()) >= 0) continue;
                    delta -= getJenrConflictWeight(criteria, iSecondStudent, p1, p2);
                }            
            }
            for (Lecture l2: iFirstStudent.getLectures()) {
                if (iFirstLectures.contains(l2)) continue;
                Placement p2 = assignment.getValue(l2);
                if (l1.equals(l2) || p2 == null) continue;
                delta += getJenrConflictWeight(criteria, iFirstStudent, p1, p2);
            }
            for (Lecture l2: iSecondLectures) {
                Placement p2 = assignment.getValue(l2);
                if (l1.equals(l2) || p2 == null) continue;
                if (l1.getClassId().compareTo(l2.getClassId()) >= 0) continue;
                delta += getJenrConflictWeight(criteria, iFirstStudent, p1, p2);
            }
        }
        return delta;
    }

    @Override
    public void assign(Assignment<Lecture, Placement> assignment, long iteration) {
        for (Lecture l1 : iFirstLectures) {
            for (Lecture l2: iFirstStudent.getLectures()) {
                if (l1.equals(l2)) continue;
                if (iFirstLectures.contains(l2) && l1.getClassId().compareTo(l2.getClassId()) >= 0) continue;
                decJenrl(assignment, iFirstStudent, l1, l2);
            }
        }
        if (iSecondStudent != null) {
            for (Lecture l1 : iSecondLectures) {
                for (Lecture l2: iSecondStudent.getLectures()) {
                    if (l1.equals(l2)) continue;
                    if (iSecondLectures.contains(l2) && l1.getClassId().compareTo(l2.getClassId()) >= 0) continue;
                    decJenrl(assignment, iSecondStudent, l1, l2);
                }
            }
        }
        for (Lecture l1 : iFirstLectures) {
            l1.removeStudent(assignment, iFirstStudent);
            iFirstStudent.removeLecture(l1);
            if (iSecondStudent != null) {
                l1.addStudent(assignment, iSecondStudent);
                iSecondStudent.addLecture(l1);
            }
        }
        for (Lecture l1 : iSecondLectures) {
            if (iSecondStudent != null) {
                l1.removeStudent(assignment, iSecondStudent);
                iSecondStudent.removeLecture(l1);
            }
            l1.addStudent(assignment, iFirstStudent);
            iFirstStudent.addLecture(l1);
        }
        if (iSecondStudent != null) {
            for (Lecture l1 : iFirstLectures) {
                for (Lecture l2: iSecondStudent.getLectures()) {
                    if (l1.equals(l2)) continue;
                    if (iFirstLectures.contains(l2) && l1.getClassId().compareTo(l2.getClassId()) >= 0) continue;
                    incJenrl(assignment, iSecondStudent, l1, l2);
                }
            }
        }
        for (Lecture l1 : iSecondLectures) {
            for (Lecture l2: iFirstStudent.getLectures()) {
                if (l1.equals(l2)) continue;
                if (iSecondLectures.contains(l2) && l1.getClassId().compareTo(l2.getClassId()) >= 0) continue;
                incJenrl(assignment, iFirstStudent, l1, l2);
            }
        }
        if (!iFirstConfig.equals(iSecondConfig)) {
            iFirstStudent.removeConfiguration(iFirstConfig);
            iFirstStudent.addConfiguration(iSecondConfig);
            if (iSecondStudent != null) {
                iSecondStudent.removeConfiguration(iSecondConfig);
                iSecondStudent.addConfiguration(iFirstConfig);
            }
        }
    }

    @Override
    public boolean isAllowed() {
        return iAllowed;
    }
    
    @Override
    public Map<Lecture, Placement> assignments() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double group(List<StudentConflict> criteria, Assignment<Lecture, Placement> assignment) {
        double value = groupValue(iFirstStudent, iSecondStudent, iSecondConfig, iSecondLectures) - groupValue(iFirstStudent, iSecondStudent, iFirstConfig, iFirstLectures);
        if (iSecondStudent != null)
            value += groupValue(iSecondStudent, iFirstStudent, iFirstConfig, iFirstLectures) - groupValue(iSecondStudent, iFirstStudent, iSecondConfig, iSecondLectures);
        return value;
    }
    
    @Override
    public String toString() {
        return "StudentSwap{" + iFirstStudent.getId() + " " + iFirstStudent.getGroupNames() + "/" + iFirstLectures + "; " + (iSecondStudent == null ? "NULL" : iSecondStudent.getId() + " " + iSecondStudent.getGroupNames()) + "/" + iSecondLectures + "}"; 
    }
}
