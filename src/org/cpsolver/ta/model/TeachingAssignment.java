package org.cpsolver.ta.model;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ta.constraints.Student;

public class TeachingAssignment extends Value<TeachingRequest, TeachingAssignment> {
    private Student iStudent;
    private int iHashCode;

    public TeachingAssignment(TeachingRequest variable, Student student) {
        super(variable, 0.0);
        iStudent = student;
        iHashCode = (variable.getName() + "," + student.getStudentId()).hashCode();
    }

    public Student getStudent() {
        return iStudent;
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
        return variable().equals(a.variable()) && getStudent().equals(a.getStudent());
    }

    @Override
    public int hashCode() {
        return iHashCode;
    }

    @Override
    public String toString() {
        return variable().getName();
    }

    @Override
    public String getName() {
        return getStudent().getStudentId();
    }
}
