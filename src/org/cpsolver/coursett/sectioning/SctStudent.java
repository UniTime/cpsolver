package org.cpsolver.coursett.sectioning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.cpsolver.coursett.constraint.JenrlConstraint;
import org.cpsolver.coursett.criteria.StudentConflict;
import org.cpsolver.coursett.model.Configuration;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.Student;

/**
 * A class wrapping a student, including an ordered set of possible enrollment into a given
 * course.
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
public class SctStudent implements Comparable<SctStudent> {
    private SctModel iModel;
    private Student iStudent;
    private List<SctEnrollment> iEnrollments = null;
    private double iTotalEnrollmentWeight = 0.0;
    private Double iOfferingWeight = null;
    private List<Lecture> iInstructing = null;
    
    /**
     * Constructor.
     * @param model student sectioning model
     * @param student student to represent
     */
    public SctStudent(SctModel model, Student student) {
        iStudent = student;
        iModel = model;
    }
    
    /**
     * Student sectioning model
     */
    public SctModel getModel() { return iModel; }
    
    /**
     * Student for which the possible enrollments are being computed
     */
    public Student getStudent() { return iStudent; }
    
    /**
     * Current enrollment of this student
     * @param checkInstructor if the student is also instructor and he/she is instructing this class, return classes he/she is instructing instead
     * @return current enrollment of this student into the given course
     */
    public SctEnrollment getCurrentEnrollment(boolean checkInstructor) {
        if (checkInstructor && isInstructing())
            return new SctEnrollment(-1, this, getInstructingLectures());
        List<Lecture> lectures = new ArrayList<Lecture>();
        for (Lecture lecture: getStudent().getLectures())
            if (getModel().getOfferingId().equals(lecture.getConfiguration().getOfferingId()))
                lectures.add(lecture);
        return new SctEnrollment(-1, this, lectures);
    }
    
    /**
     * List of lectures of the given course that the student is instructing (if he/she is also an instructor, using {@link Student#getInstructor()})
     * @return list of lectures of the given course that the student is instructing
     */
    public List<Lecture> getInstructingLectures() {
        if (iInstructing == null && getStudent().getInstructor() != null) {
            iInstructing = new ArrayList<Lecture>();
            for (Lecture lecture: getStudent().getInstructor().variables())
                if (getModel().getOfferingId().equals(lecture.getConfiguration().getOfferingId()))
                    iInstructing.add(lecture);
        }
        return iInstructing;
    }
    
    /**
     * Is student also an instructor of the given course?
     */
    public boolean isInstructing() {
        return getStudent().getInstructor() != null && !getInstructingLectures().isEmpty();
    }
    
    /**
     * Conflict weight of a lecture pair
     */
    public double getJenrConflictWeight(Lecture l1, Lecture l2) {
        Placement p1 = getModel().getAssignment().getValue(l1);
        Placement p2 = getModel().getAssignment().getValue(l2);
        if (p1 == null || p2 == null) return 0.0;
        if (getModel().getStudentConflictCriteria() == null) {
            if (JenrlConstraint.isInConflict(p1, p2, getModel().getTimetableModel().getDistanceMetric(), getModel().getTimetableModel().getStudentWorkDayLimit()))
                return getStudent().getJenrlWeight(l1, l2);
            return 0.0;
        }
        double weight = 0.0;
        for (StudentConflict sc: getModel().getStudentConflictCriteria())
            if (sc.isApplicable(getStudent(), p1.variable(), p2.variable()) && sc.inConflict(p1, p2))
                weight += sc.getWeight() * getStudent().getJenrlWeight(l1, l2);
        return weight;
    }
    
    /**
     * All scheduling subpart ids of a configuration.
     */
    private List<Long> getSubpartIds(Configuration configuration) {
        List<Long> subpartIds = new ArrayList<Long>();
        Queue<Lecture> queue = new LinkedList<Lecture>();
        for (Map.Entry<Long, Set<Lecture>> e: configuration.getTopLectures().entrySet()) {
            subpartIds.add(e.getKey());
            queue.add(e.getValue().iterator().next());
        }
        Lecture lecture = null;
        while ((lecture = queue.poll()) != null) {
            if (lecture.getChildren() != null)
                for (Map.Entry<Long, List<Lecture>> e: lecture.getChildren().entrySet()) {
                    subpartIds.add(e.getKey());
                    queue.add(e.getValue().iterator().next());
                }
        }
        return subpartIds;
    }
    
    /**
     * Compute all possible enrollments
     */
    private void computeEnrollments(Configuration configuration, Map<Long, Set<Lecture>> subparts, List<Long> subpartIds, Set<Lecture> enrollment, double conflictWeight) {
        if (enrollment.size() == subpartIds.size()) {
            iEnrollments.add(new SctEnrollment(iEnrollments.size(), this, enrollment, conflictWeight));
            iTotalEnrollmentWeight += conflictWeight;
        } else {
            Set<Lecture> lectures = subparts.get(subpartIds.get(enrollment.size()));
            for (Lecture lecture: lectures) {
                if (lecture.getParent() != null && !enrollment.contains(lecture.getParent())) continue;
                if (!getStudent().canEnroll(lecture)) continue;
                double delta = 0.0;
                for (Lecture other: getStudent().getLectures())
                    if (!configuration.getOfferingId().equals(other.getConfiguration().getOfferingId()))
                        delta += getJenrConflictWeight(lecture, other);
                for (Lecture other: enrollment)
                    delta += getJenrConflictWeight(lecture, other);
                enrollment.add(lecture);
                computeEnrollments(configuration, subparts, subpartIds, enrollment, conflictWeight + delta);
                enrollment.remove(lecture);
            }
        }
    }
    
    /**
     * Compute all possible enrollments
     */
    private void computeEnrollments() {
        iEnrollments = new ArrayList<SctEnrollment>();
        if (isInstructing()) {
            double conflictWeight = 0.0;
            for (Lecture lecture: getInstructingLectures()) {
                for (Lecture other: getStudent().getLectures())
                    if (!getModel().getOfferingId().equals(other.getConfiguration().getOfferingId()))
                        conflictWeight += getJenrConflictWeight(lecture, other);
            }
            iEnrollments.add(new SctEnrollment(0, this, getInstructingLectures(), conflictWeight));
            return;
        }
        for (Configuration configuration: getModel().getConfigurations()) {
            Map<Long, Set<Lecture>> subparts = getModel().getSubparts(configuration);
            List<Long> subpartIds = getSubpartIds(configuration);
            computeEnrollments(configuration, subparts, subpartIds, new HashSet<Lecture>(), 0.0);
        }
        Collections.sort(iEnrollments);
    }
    
    /**
     * Return all possible enrollments of the given student into the given course
     */
    public List<SctEnrollment> getEnrollments() {
        if (iEnrollments == null) computeEnrollments();
        return iEnrollments;
    }
    
    public List<SctEnrollment> getEnrollments(Comparator<SctEnrollment> cmp) {
        if (iEnrollments == null) computeEnrollments();
        if (cmp != null)
            Collections.sort(iEnrollments, cmp);
        return iEnrollments;
    }
    
    /**
     * Number of all possible enrollments of the given student into the given course
     */
    public int getNumberOfEnrollments() {
        if (iEnrollments == null) computeEnrollments();
        return iEnrollments.size();
    }
    
    /**
     * Average conflict weight
     */
    public double getAverageConflictWeight() {
        if (iEnrollments == null) computeEnrollments();
        return iTotalEnrollmentWeight / iEnrollments.size();
    }
    
    /**
     * Offering weight using {@link Student#getOfferingWeight(Long)}
     */
    public double getOfferingWeight() {
        if (iOfferingWeight == null)
            iOfferingWeight = getStudent().getOfferingWeight(getModel().getOfferingId());
        return iOfferingWeight;
    }
    
    /**
     * Compare two students using their curriculum information
     */
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

    /**
     * Compare two student, using average conflict weight, number of possible enrollments and their curriculum information.
     * Student that is harder to schedule goes first.
     */
    @Override
    public int compareTo(SctStudent s) {
        int cmp = Double.compare(s.getAverageConflictWeight(), getAverageConflictWeight());
        if (cmp != 0) return cmp;
        cmp = Double.compare(getNumberOfEnrollments(), s.getNumberOfEnrollments());
        if (cmp != 0) return cmp;
        return compare(getStudent(), s.getStudent());
    }
    
    @Override
    public String toString() {
        return getStudent() + "{enrls:" + getEnrollments().size() + (getEnrollments().isEmpty() ? "" : ", best:" + getEnrollments().get(0).getConflictWeight()) + ", avg:" + getAverageConflictWeight() + "}";
    }

}
