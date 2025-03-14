package org.cpsolver.instructor.model;

import java.util.BitSet;

import org.cpsolver.coursett.model.TimeLocation;

/**
 * Enrolled class to be used as an instructor unavailability.
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Instructor Sectioning)<br>
 *          Copyright (C) 2016 Tomas Muller<br>
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
public class EnrolledClass extends TimeLocation {
    private Long iCourseId;
    private Long iClassId;
    private String iCourse;
    private String iSection;
    private String iType;
    private String iExternalId;
    private String iRoom;
    private boolean iInstructor;
    
    /**
     * Constructor
     * @param courseId course unique id
     * @param classId class unique id
     * @param course course name
     * @param type instructional type
     * @param section section name
     * @param externalId external id
     * @param dayCode days (combination of 1 for Monday, 2 for Tuesday, ...)
     * @param startTime start slot
     * @param length number of slots
     * @param datePatternId date pattern unique id
     * @param datePatternName date pattern name
     * @param weekCode date pattern (binary string with 1 for each day when classes take place)
     * @param breakTime break time in minutes
     * @param room assigned room(s)
     * @param instructor true if the instructor is teaching the class (not being a student)
     */
    public EnrolledClass(Long courseId, Long classId, String course, String type, String section, String externalId, int dayCode, int startTime, int length, Long datePatternId, String datePatternName, BitSet weekCode, int breakTime, String room, boolean instructor) {
        super(dayCode, startTime, length, 0, 0.0, 0, datePatternId, datePatternName, weekCode, breakTime);
        iCourseId = courseId;
        iClassId = classId;
        iCourse = course;
        iType = type;
        iSection = section;
        iExternalId = externalId;
        iRoom = room;
        iInstructor = instructor;
    }

    /**
     * Unique id of the enrolled course
     * @return course unique id
     */
    public Long getCourseId() { return iCourseId; }
    /**
     * Unique id of the enrolled class
     * @return class unique id
     */
    public Long getClassId() { return iClassId; }
    /**
     * Name of the enrolled course
     * @return course name
     */
    public String getCourse() { return iCourse; }
    /**
     * Name of the enrolled class
     * @return section name
     */
    public String getSection() { return iSection; }
    /**
     * Name of the instructional type of the enrolled class
     * @return instructional type
     */
    public String getType() { return iType; }
    /**
     * External id of the enrolled class
     * @return external id
     */
    public String getExternalId() { return iExternalId; }
    /**
     * Room or rooms (comma separated) of the enrolled class
     * @return assigned room name
     */
    public String getRoom() { return iRoom; }
    /**
     * Role of the instructor
     * @return true if the instructor is teaching this class
     */
    public boolean isInstructor() { return iInstructor; }
    /**
     * Role of the instructor
     * @return true if the instructor is enrolled in this class as student
     */
    public boolean isStudent() { return !iInstructor; }
    
    @Override
    public String toString() {
        return getCourse() + " " + getSection() + " " + getLongName(true);
    }
}
