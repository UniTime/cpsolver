package net.sf.cpsolver.studentsct.model;

/**
 * Representation of a course offering. A course offering contains id, subject
 * area, course number and an instructional offering. <br>
 * <br>
 * Each instructional offering (see {@link Offering}) is offered under one or
 * more course offerings.
 * 
 * <br>
 * <br>
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
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
public class Course {
    private long iId = -1;
    private String iSubjectArea = null;
    private String iCourseNumber = null;
    private Offering iOffering = null;
    private int iLimit = 0, iProjected = 0;

    /**
     * Constructor
     * 
     * @param id
     *            course offering unique id
     * @param subjectArea
     *            subject area (e.g., MA, CS, ENGL)
     * @param courseNumber
     *            course number under the given subject area
     * @param offering
     *            instructional offering which is offered under this course
     *            offering
     */
    public Course(long id, String subjectArea, String courseNumber, Offering offering) {
        iId = id;
        iSubjectArea = subjectArea;
        iCourseNumber = courseNumber;
        iOffering = offering;
        iOffering.getCourses().add(this);
    }

    /**
     * Constructor
     * 
     * @param id
     *            course offering unique id
     * @param subjectArea
     *            subject area (e.g., MA, CS, ENGL)
     * @param courseNumber
     *            course number under the given subject area
     * @param offering
     *            instructional offering which is offered under this course
     *            offering
     * @param limit
     *            course offering limit
     * @param projected
     *            projected demand
     */
    public Course(long id, String subjectArea, String courseNumber, Offering offering, int limit, int projected) {
        iId = id;
        iSubjectArea = subjectArea;
        iCourseNumber = courseNumber;
        iOffering = offering;
        iOffering.getCourses().add(this);
        iLimit = limit;
        iProjected = projected;
    }

    /** Course offering unique id */
    public long getId() {
        return iId;
    }

    /** Subject area */
    public String getSubjectArea() {
        return iSubjectArea;
    }

    /** Course number */
    public String getCourseNumber() {
        return iCourseNumber;
    }

    /** Course offering name: subject area + course number */
    public String getName() {
        return iSubjectArea + " " + iCourseNumber;
    }

    @Override
    public String toString() {
        return getName();
    }

    /** Instructional offering which is offered under this course offering. */
    public Offering getOffering() {
        return iOffering;
    }

    /** Course offering limit */
    public int getLimit() {
        return iLimit;
    }

    /** Set course offering limit */
    public void setLimit(int limit) {
        iLimit = limit;
    }

    /** Course offering projected number of students */
    public int getProjected() {
        return iProjected;
    }
}
