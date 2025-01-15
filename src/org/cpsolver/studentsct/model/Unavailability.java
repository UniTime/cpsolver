package org.cpsolver.studentsct.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.ifs.assignment.Assignment;

/**
 * Representation of an unavailability. This is typically used when the student is
 * also an instructor and he/she is teaching during some time (and hence not available
 * for attending classes as a student). An unavailability can be marked as can overlap
 * in time, in which case the time overlap is allowed but the overlapping time is to
 * be minimized.<br>
 * <br>
 * 
 * @author  Tomas Muller
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2016 Tomas Muller<br>
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
public class Unavailability implements SctAssignment {
    private Section iSection;
    private Student iStudent;
    private boolean iAllowOverlap;
    private boolean iTeachingAssignment = true;
    private Long iCourseId;
    
    /**
     * Constructor
     * @param student student
     * @param section section that the student is teaching
     * @param canOverlap true if student can have classes during the time, but the overlapping time should be minimized
     */
    public Unavailability(Student student, Section section, boolean canOverlap) {
        iStudent = student;
        iSection = section;
        iAllowOverlap = canOverlap;
        iStudent.getUnavailabilities().add(this);
        iSection.getUnavailabilities().add(this);
    }
    
    /**
     * Student
     */
    public Student getStudent() { return iStudent; }
    
    /**
     * Section
     */
    public Section getSection() { return iSection; }
    
    /**
     * Optional course id (within the course of {@link Unavailability#getSection()}
     */
    public Long getCourseId() { return iCourseId; }
    
    /**
     * Optional course id (within the course of {@link Unavailability#getSection()}
     */
    public void setCourseId(Long courseId) { iCourseId = courseId; }
    
    /**
     * Is this a teaching assignment
     */
    public boolean isTeachingAssignment() { return iTeachingAssignment; }
    
    /**
     * Is this a teaching assignment, defaults to true
     */
    public void setTeachingAssignment(boolean ta) { iTeachingAssignment = ta; }
    
    /**
     * Course name taken from the {@link Unavailability#getSection()} and optional {@link Unavailability#getCourseId()}.
     * Name of the controlling course is used when no course id is set.
     */
    public String getCourseName() {
        if (getSection().getSubpart() == null) return "";
        Offering offering = getSection().getSubpart().getConfig().getOffering();
        if (getCourseId() != null)
            for (Course course: offering.getCourses())
                if (course.getId() == getCourseId())
                    return course.getName();
        return offering.getName();
    }
    
    /**
     * Section name using {@link Section#getName(long)} when the optional course id is provided, 
     * using {@link Section#getName()} otherwise. 
     */
    public String getSectionName() {
        if (getSection().getSubpart() == null) return getSection().getName();
        if (getCourseId() != null)
            return getSection().getSubpart().getName() + " " + getSection().getName(getCourseId());
        return getSection().getSubpart().getName() + " " +getSection().getName();
    }
    
    @Override
    public long getId() {
        return getSection().getId();
    }
    
    @Override
    public TimeLocation getTime() { return getSection().getTime(); }
    
    /**
     * Can student have classes during this unavailability? The overlapping time should be minimized in this case
     * (like with lower priority free time requests).
     */
    @Override
    public boolean isAllowOverlap() { return iAllowOverlap; }

    @Override
    public List<RoomLocation> getRooms() {
        return getSection().getRooms();
    }

    @Override
    public int getNrRooms() {
        return getSection().getNrRooms();
    }

    @Override
    public boolean isOverlapping(SctAssignment assignment) {
        if (isAllowOverlap() || assignment.isAllowOverlap()) return false;
        if (getTime() == null || assignment.getTime() == null) return false;
        if (assignment instanceof Section && getTime().hasIntersection(assignment.getTime())) return true;
        return false;
    }

    @Override
    public boolean isOverlapping(Set<? extends SctAssignment> assignments) {
        if (isAllowOverlap()) return false;
        if (getTime() == null) return false;
        for (SctAssignment assignment : assignments) {
            if (assignment.isAllowOverlap()) continue;
            if (assignment.getTime() == null) continue;
            if (assignment instanceof Section && getTime().hasIntersection(assignment.getTime())) return true;
        }
        return false;
    }

    @Override
    public void assigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
    }

    @Override
    public void unassigned(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
    }

    /**
     * Not used, always null
     */
    @Override
    public Set<Enrollment> getEnrollments(Assignment<Request, Enrollment> assignment) {
        return null;
    }

    @Override
    public int compareById(SctAssignment a) {
        if (a instanceof Unavailability) {
            return Long.valueOf(getId()).compareTo(((Unavailability)a).getId());
        } else {
            return 1;
        }
    }
    
    /**
     * Create dummy enrollment of this unavailability 
     * @return created enrollment (warning: the returned enrollment has no request)
     **/
    public Enrollment createEnrollment() {
        HashSet<SctAssignment> assignments = new HashSet<SctAssignment>();
        assignments.add(this);
        return new Enrollment(null, 0, null, assignments, null);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Unavailability)) return false;
        return getId() == ((Unavailability)o).getId();
    }
    
    @Override
    public int hashCode() {
        return (int) (getId() ^ (getId() >>> 32));
    }

    @Override
    public String toString() {
        if (getSection().getSubpart() == null) return getSection().getName();
        return getCourseName() + " " + getSectionName();
    }
}
