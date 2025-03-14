package org.cpsolver.coursett.sectioning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cpsolver.coursett.model.Lecture;

/**
 * A class wrapping an enrollment of a student into a course. Such an enrollment
 * consists of a list of classes and a conflict weight. That is a weighted sum
 * of the student conflicts (with other courses of the student) it creates.
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
public class SctEnrollment implements Comparable<SctEnrollment> {
    private int iId;
    private SctStudent iStudent;
    private List<Lecture> iLectures;
    private double iConflictWeight;
    
    /**
     * @param id unique id
     * @param student a student
     * @param lectures list of classes
     * @param conflictWeight conflict weight
     */
    public SctEnrollment(int id, SctStudent student, Collection<Lecture> lectures, double conflictWeight) {
        iId = id;
        iStudent = student;
        iLectures = new ArrayList<Lecture>(lectures);
        iConflictWeight = conflictWeight;
    }
    
    /**
     * @param id unique id
     * @param student a student
     * @param lectures list of classes
     */
    public SctEnrollment(int id, SctStudent student, Collection<Lecture> lectures) {
        iId = id;
        iStudent = student;
        iLectures = new ArrayList<Lecture>(lectures);
        iConflictWeight = 0;
        for (Lecture lecture: lectures) {
            for (Lecture other: student.getStudent().getLectures()) {
                if (!lecture.getConfiguration().getOfferingId().equals(other.getConfiguration().getOfferingId()))
                    iConflictWeight += student.getJenrConflictWeight(lecture, other);
            }
            for (Lecture other: lectures)
                if (lecture.getClassId().compareTo(other.getClassId()) < 0)
                    iConflictWeight += student.getJenrConflictWeight(lecture, other);
        }
    }
    
    /**
     * Student
     */
    public SctStudent getStudent() { return iStudent; }
    
    /**
     * List of classes of this enrollment
     */
    public List<Lecture> getLectures() { return iLectures; }
    
    /**
     * Overall conflict weight
     */
    public double getConflictWeight() { return iConflictWeight; }

    @Override
    public int compareTo(SctEnrollment o) {
        int cmp = Double.compare(getConflictWeight(), o.getConflictWeight());
        if (cmp != 0) return cmp;
        return Double.compare(iId, o.iId);
    }
    
    public Integer getId() { return iId; }
    
    @Override
    public String toString() {
        return iConflictWeight + "/" + iLectures;
    }
}
