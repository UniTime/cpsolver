package org.cpsolver.instructor.model;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.instructor.constraints.InstructorConstraint;

public class TeachingAssignment extends Value<TeachingRequest, TeachingAssignment> {
    private Instructor iInstructor;
    private int iHashCode;
    private int iAttributePreference, iInstructorPreference, iCoursePreference, iTimePreference;

    public TeachingAssignment(TeachingRequest request, Instructor instructor, int attributePreference) {
        super(request, 0.0);
        iInstructor = instructor;
        iHashCode = request.hashCode() ^ instructor.hashCode();
        iTimePreference = instructor.getTimePreference(request).getPreferenceInt();
        iCoursePreference = instructor.getCoursePreference(request.getCourse()).getPreference();
        iInstructorPreference = request.getInstructorPreference(instructor).getPreference();
        iAttributePreference = attributePreference;
    }
    
    public TeachingAssignment(TeachingRequest request, Instructor instructor) {
        this(request, instructor, request.getAttributePreference(instructor).getPreferenceInt());
    }

    public Instructor getInstructor() {
        return iInstructor;
    }
    public InstructorConstraint getInstructorConstraint() {
        return getInstructor().getConstraint();
    }

    @Override
    public double toDouble(Assignment<TeachingRequest, TeachingAssignment> assignment) {
        double ret = 0.0;
        for (Criterion<TeachingRequest, TeachingAssignment> criterion : variable().getModel().getCriteria())
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
    
    public int getAttributePreference() {
        return iAttributePreference;
    }
    
    public int getTimePreference() {
        return iTimePreference;
    }
    
    public int getInstructorPreference() {
        return iInstructorPreference;
    }
    
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
        return getInstructor().getExternalId();
    }
}
