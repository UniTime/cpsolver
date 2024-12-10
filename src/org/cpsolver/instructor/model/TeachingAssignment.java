package org.cpsolver.instructor.model;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.model.Value;

/**
 * Teaching assignment. An assignment of an instructor to a teaching request (a set of sections of a course).
 * A teaching assignment also contains the value of attribute, instructor, course, and time preferences. 
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
public class TeachingAssignment extends Value<TeachingRequest.Variable, TeachingAssignment> {
    private Instructor iInstructor;
    private int iHashCode;
    private int iAttributePreference, iInstructorPreference, iCoursePreference, iTimePreference;

    /**
     * Constructor
     * @param variable teaching request variable
     * @param instructor instructor (it is expected that {@link Instructor#canTeach(TeachingRequest)} is true and that {@link TeachingRequest#getAttributePreference(Instructor)} is not prohibited)
     * @param attributePreference attribute preference (value of {@link TeachingRequest#getAttributePreference(Instructor)})
     */
    public TeachingAssignment(TeachingRequest.Variable variable, Instructor instructor, int attributePreference) {
        super(variable);
        iInstructor = instructor;
        iHashCode = variable.hashCode() ^ instructor.hashCode();
        iTimePreference = instructor.getTimePreference(variable.getRequest()).getPreferenceInt();
        iCoursePreference = instructor.getCoursePreference(variable.getCourse()).getPreference();
        iInstructorPreference = variable.getRequest().getInstructorPreference(instructor).getPreference();
        iAttributePreference = attributePreference;
    }
    
    /**
     * Constructor
     * @param variable teaching request variable
     * @param instructor instructor (it is expected that {@link Instructor#canTeach(TeachingRequest)} is true and that {@link TeachingRequest#getAttributePreference(Instructor)} is not prohibited)
     */
    public TeachingAssignment(TeachingRequest.Variable variable, Instructor instructor) {
        this(variable, instructor, variable.getRequest().getAttributePreference(instructor).getPreferenceInt());
    }

    /**
     * Assigned instructor
     * @return assigned instructor
     */
    public Instructor getInstructor() {
        return iInstructor;
    }
    
    @Override
    public double toDouble(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) {
        double ret = 0.0;
        for (Criterion<TeachingRequest.Variable, TeachingAssignment> criterion : variable().getModel().getCriteria())
            ret += criterion.getWeightedValue(assignment, this, null);
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof TeachingAssignment))
            return false;
        TeachingAssignment a = (TeachingAssignment) o;
        return variable().equals(a.variable()) && getInstructor().equals(a.getInstructor());
    }
    
    /**
     * Attribute preference
     * @return {@link TeachingRequest#getAttributePreference(Instructor)}
     */
    public int getAttributePreference() {
        return iAttributePreference;
    }
    
    /**
     * Time preference
     * @return {@link Instructor#getTimePreference(TeachingRequest)}
     */
    public int getTimePreference() {
        return iTimePreference;
    }
    
    /**
     * Instructor preference
     * @return {@link TeachingRequest#getInstructorPreference(Instructor) }
     */
    public int getInstructorPreference() {
        return iInstructorPreference;
    }
    
    /**
     * Course preference
     * @return {@link Instructor#getCoursePreference(Course)} for {@link TeachingRequest#getCourse()}
     */
    public int getCoursePreference() {
        return iCoursePreference;
    }

    @Override
    public int hashCode() {
        return iHashCode;
    }

    @Override
    public String toString() {
        return variable().getName() + ": " + getInstructor().getName();
    }

    @Override
    public String getName() {
        return (getInstructor().hasName() ? getInstructor().getName() + (getInstructor().hasExternalId() ? " (" + getInstructor().getExternalId() + ")" : "") : getInstructor().getName());
    }
}
