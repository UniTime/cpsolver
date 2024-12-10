package org.cpsolver.coursett.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Student group.
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2016 Tomas Muller<br>
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
public class StudentGroup implements Comparable<StudentGroup> {
    private long iId;
    private String iName;
    private double iWeight;
    private List<Student> iStudents = new ArrayList<Student>();
    
    /**
     * Create a student group
     * @param id group unique id
     * @param weight group weight
     * @param name group name
     */
    public StudentGroup(long id, double weight, String name) {
        iId = id; iName = name; iWeight = weight;
    }
    
    /**
     * Returns student group id
     */
    public long getId() { return iId; }
    
    /**
     * Returns student group name
     */
    public String getName() { return iName; }
    
    /**
     * Returns student group weight
     */
    public double getWeight() { return iWeight; }
    
    /**
     * Return students of this group
     */
    public List<Student> getStudents() {
        return iStudents;
    }
    
    /**
     * Count students of this group that are requesting the given offering.
     * @param offeringId offering id
     * @return students with {@link Student#hasOffering(Long)} true
     */
    public int countStudents(Long offeringId) {
        int ret = 0;
        for (Student student: iStudents)
            if (student.hasOffering(offeringId)) ret++;
        return ret;
    }
    
    /**
     * Add student to this group
     * @param student a student to add
     */
    public void addStudent(Student student) {
        iStudents.add(student);
    }
    
    @Override
    public int hashCode() { return (int)(iId ^ (iId >>> 32)); }
    
    @Override
    public boolean equals(Object o) {
        return (o != null && o instanceof StudentGroup && getId() == ((StudentGroup)o).getId());
    }
    
    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int compareTo(StudentGroup g) {
        int cmp = getName().compareToIgnoreCase(g.getName());
        if (cmp != 0) return cmp;
        return (getId() < g.getId() ? -1 : getId() == g.getId() ? 0 : 1);
    }
    
    /**
     * Average enrollment weight of students of this group in the given offering
     */
    public double getAverageEnrollmentWeight(Long offeringId) {
        double total = 0.0; int count = 0;
        for (Student student: iStudents)
            if (student.hasOffering(offeringId)) {
                total += student.getOfferingWeight(offeringId);
                count ++;
            }
        return count == 0 ? 0.0 : total / count;
    }
    
    /**
     * Count offerings that students of this group have
     */
    public int countOfferings() {
        Set<Long> offeringIds = new HashSet<Long>();
        for (Student student: iStudents)
            offeringIds.addAll(student.getOfferings());
        return offeringIds.size();
    }
}
