package org.cpsolver.coursett.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.cpsolver.coursett.constraint.InstructorConstraint;
import org.cpsolver.coursett.constraint.JenrlConstraint;


/**
 * Student.
 * 
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
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
public class Student implements Comparable<Student> {
    private static org.apache.log4j.Logger sLogger = org.apache.log4j.Logger.getLogger(Student.class);
    public static boolean USE_DISTANCE_CACHE = false;
    Long iStudentId = null;
    HashMap<Long, Double> iOfferings = new HashMap<Long, Double>();
    Set<Lecture> iLectures = new HashSet<Lecture>();
    Set<Configuration> iConfigurations = new HashSet<Configuration>();
    HashMap<Long, Set<Lecture>> iCanNotEnrollSections = null;
    HashMap<Student, Double> iDistanceCache = null;
    HashSet<Placement> iCommitedPlacements = null;
    private String iAcademicArea = null, iAcademicClassification = null, iMajor = null, iCurriculum = null;
    HashMap<Long, Double> iOfferingPriority = new HashMap<Long, Double>();
    private InstructorConstraint iInstructor = null;
    private Set<StudentGroup> iGroups = new HashSet<StudentGroup>();

    public Student(Long studentId) {
        iStudentId = studentId;
    }

    public void addOffering(Long offeringId, double weight, Double priority) {
        iOfferings.put(offeringId, weight);
        if (priority != null) iOfferingPriority.put(offeringId, priority);
    }
    
    public void addOffering(Long offeringId, double weight) {
        addOffering(offeringId, weight, null);
    }

    public Map<Long, Double> getOfferingsMap() {
        return iOfferings;
    }

    public Set<Long> getOfferings() {
        return iOfferings.keySet();
    }

    public boolean hasOffering(Long offeringId) {
        return iOfferings.containsKey(offeringId);
    }
    
    public InstructorConstraint getInstructor() { return iInstructor; }
    
    public void setInstructor(InstructorConstraint instructor) { iInstructor = instructor; }
    
    /**
     * Priority of an offering (for the student). Null if not used, or between
     * zero (no priority) and one (highest priority)
     * @param offeringId instructional offering unique id
     * @return student's priority
     */
    public Double getPriority(Long offeringId) {
        return offeringId == null ? null : iOfferingPriority.get(offeringId);
    }
    
    public Double getPriority(Configuration configuration) {
        return configuration == null ? null : getPriority(configuration.getOfferingId());
    }
    
    public Double getPriority(Lecture lecture) {
        return lecture == null ? null : getPriority(lecture.getConfiguration());
    }
    
    public Double getConflictingPriorty(Lecture l1, Lecture l2) {
        // Conflicting priority is the lower of the two priorities
        Double p1 = getPriority(l1);
        Double p2 = getPriority(l2);
        return p1 == null ? null : p2 == null ? null : Math.min(p1, p2);
    }

    public double getOfferingWeight(Configuration configuration) {
        if (configuration == null)
            return 1.0;
        return getOfferingWeight(configuration.getOfferingId());
    }

    public double getOfferingWeight(Long offeringId) {
        Double weight = iOfferings.get(offeringId);
        return (weight == null ? 0.0 : weight.doubleValue());
    }
    
    public boolean canUnenroll(Lecture lecture) {
        if (getInstructor() != null)
            return !getInstructor().variables().contains(lecture);
        return true;
    }

    public boolean canEnroll(Lecture lecture) {
        if (iCanNotEnrollSections != null) {
            Set<Lecture> canNotEnrollLectures = iCanNotEnrollSections.get(lecture.getConfiguration().getOfferingId());
            return canEnroll(canNotEnrollLectures, lecture, true);
        }
        return true;
    }

    private boolean canEnroll(Set<Lecture> canNotEnrollLectures, Lecture lecture, boolean checkParents) {
        if (canNotEnrollLectures == null)
            return true;
        if (canNotEnrollLectures.contains(lecture))
            return false;
        if (checkParents) {
            Lecture parent = lecture.getParent();
            while (parent != null) {
                if (canNotEnrollLectures.contains(parent))
                    return false;
                parent = parent.getParent();
            }
        }
        if (lecture.hasAnyChildren()) {
            for (Long subpartId: lecture.getChildrenSubpartIds()) {
                boolean canEnrollChild = false;
                for (Lecture childLecture : lecture.getChildren(subpartId)) {
                    if (canEnroll(canNotEnrollLectures, childLecture, false)) {
                        canEnrollChild = true;
                        break;
                    }
                }
                if (!canEnrollChild)
                    return false;
            }
        }
        return true;
    }

    public void addCanNotEnroll(Lecture lecture) {
        if (iCanNotEnrollSections == null)
            iCanNotEnrollSections = new HashMap<Long, Set<Lecture>>();
        if (lecture.getConfiguration() == null) {
            sLogger.warn("Student.addCanNotEnroll(" + lecture
                    + ") -- given lecture has no configuration associated with.");
            return;
        }
        Set<Lecture> canNotEnrollLectures = iCanNotEnrollSections.get(lecture.getConfiguration().getOfferingId());
        if (canNotEnrollLectures == null) {
            canNotEnrollLectures = new HashSet<Lecture>();
            iCanNotEnrollSections.put(lecture.getConfiguration().getOfferingId(), canNotEnrollLectures);
        }
        canNotEnrollLectures.add(lecture);
    }

    public void addCanNotEnroll(Long offeringId, Collection<Lecture> lectures) {
        if (lectures == null || lectures.isEmpty())
            return;
        if (iCanNotEnrollSections == null)
            iCanNotEnrollSections = new HashMap<Long, Set<Lecture>>();
        Set<Lecture> canNotEnrollLectures = iCanNotEnrollSections.get(offeringId);
        if (canNotEnrollLectures == null) {
            canNotEnrollLectures = new HashSet<Lecture>();
            iCanNotEnrollSections.put(offeringId, canNotEnrollLectures);
        }
        canNotEnrollLectures.addAll(lectures);
    }

    public Map<Long, Set<Lecture>> canNotEnrollSections() {
        return iCanNotEnrollSections;
    }

    public void addLecture(Lecture lecture) {
        iLectures.add(lecture);
    }

    public void removeLecture(Lecture lecture) {
        iLectures.remove(lecture);
    }

    public Set<Lecture> getLectures() {
        return iLectures;
    }

    public void addConfiguration(Configuration config) {
        if (config != null) iConfigurations.add(config);
    }

    public void removeConfiguration(Configuration config) {
        if (config != null) iConfigurations.remove(config);
    }

    public Set<Configuration> getConfigurations() {
        return iConfigurations;
    }

    public Long getId() {
        return iStudentId;
    }

    public double getDistance(Student student) {
        Double dist = (USE_DISTANCE_CACHE && iDistanceCache != null ? iDistanceCache.get(student) : null);
        if (dist == null) {
            if (!getGroups().isEmpty() || !student.getGroups().isEmpty()) {
                double total = 0.0f;
                double same = 0.0;
                for (StudentGroup g: getGroups()) {
                    total += g.getWeight();
                    if (student.hasGroup(g))
                        same += g.getWeight();
                }
                for (StudentGroup g: student.getGroups()) {
                    total += g.getWeight();
                }
                dist = (total - 2*same) / total;
            } else {
                int same = 0;
                for (Long o : getOfferings()) {
                    if (student.getOfferings().contains(o))
                        same++;
                }
                double all = student.getOfferings().size() + getOfferings().size();
                double dif = all - 2.0 * same;
                dist = new Double(dif / all);
            }
            if (USE_DISTANCE_CACHE) {
                if (iDistanceCache == null)
                    iDistanceCache = new HashMap<Student, Double>();
                iDistanceCache.put(student, dist);
            }
        }
        return dist.doubleValue();
    }

    public void clearDistanceCache() {
        if (USE_DISTANCE_CACHE && iDistanceCache != null)
            iDistanceCache.clear();
    }

    @Override
    public String toString() {
        return String.valueOf(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public int compareTo(Student s) {
        return getId().compareTo(s.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Student))
            return false;
        return getId().equals(((Student) o).getId());
    }

    public void addCommitedPlacement(Placement placement) {
        if (iCommitedPlacements == null)
            iCommitedPlacements = new HashSet<Placement>();
        iCommitedPlacements.add(placement);
    }

    public Set<Placement> getCommitedPlacements() {
        return iCommitedPlacements;
    }

    public Set<Placement> conflictPlacements(Placement placement) {
        if (iCommitedPlacements == null)
            return null;
        Set<Placement> ret = new HashSet<Placement>();
        Lecture lecture = placement.variable();
        for (Placement commitedPlacement : iCommitedPlacements) {
            Lecture commitedLecture = commitedPlacement.variable();
            if (lecture.getSchedulingSubpartId() != null
                    && lecture.getSchedulingSubpartId().equals(commitedLecture.getSchedulingSubpartId()))
                continue;
            if (lecture.isToIgnoreStudentConflictsWith(commitedLecture)) continue;
            if (JenrlConstraint.isInConflict(commitedPlacement, placement, ((TimetableModel)placement.variable().getModel()).getDistanceMetric(),
                    ((TimetableModel)placement.variable().getModel()).getStudentWorkDayLimit()))
                ret.add(commitedPlacement);
        }
        return ret;
    }

    public int countConflictPlacements(Placement placement) {
        Set<Placement> conflicts = conflictPlacements(placement);
        double w = getOfferingWeight((placement.variable()).getConfiguration());
        return (int) Math.round(conflicts == null ? 0 : avg(w, 1.0) * conflicts.size());
    }

    public double getJenrlWeight(Lecture l1, Lecture l2) {
        if (getInstructor() != null && (getInstructor().variables().contains(l1) || getInstructor().variables().contains(l2)))
            return 1.0;
        return avg(getOfferingWeight(l1.getConfiguration()), getOfferingWeight(l2.getConfiguration()));
    }

    public double avg(double w1, double w2) {
        return Math.sqrt(w1 * w2);
    }
    
    public String getAcademicArea() {
        return iAcademicArea;
    }
    
    public void setAcademicArea(String acadArea) {
        iAcademicArea = acadArea;
    }
    
    public String getAcademicClassification() {
        return iAcademicClassification;
    }
    
    public void setAcademicClassification(String acadClasf) {
        iAcademicClassification = acadClasf;
    }
    
    public String getMajor() {
        return iMajor;
    }
    
    public void setMajor(String major) {
        iMajor = major;
    }
    
    public String getCurriculum() {
        return iCurriculum;
    }
    
    public void setCurriculum(String curriculum) {
        iCurriculum = curriculum;
    }
    
    public void addGroup(StudentGroup group) {
        iGroups.add(group);
    }
    
    public Set<StudentGroup> getGroups() { return iGroups; }
    
    public boolean hasGroup(StudentGroup group) { return iGroups.contains(group); }
    
    public String getGroupNames() {
        if (iGroups.isEmpty()) return "";
        if (iGroups.size() == 1) return iGroups.iterator().next().getName();
        String ret = "";
        for (StudentGroup g: new TreeSet<StudentGroup>(iGroups))
            ret += (ret.isEmpty() ? "" : ", ") + g.getName();
        return ret;
    }
    
    public double getSameGroupWeight(Student other) {
        double ret = 0.0;
        for (StudentGroup group: iGroups)
            if (other.hasGroup(group) && group.getWeight() > ret) ret = group.getWeight();
        return ret;
    }
}
