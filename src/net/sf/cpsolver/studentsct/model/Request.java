package net.sf.cpsolver.studentsct.model;

import java.util.Iterator;
import java.util.Vector;

import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;

public abstract class Request extends Variable implements Comparable {
    private long iId = -1;
    private int iPriority = 0;
    private boolean iAlternative = false;
    private Student iStudent = null;
    private double iWeight = 1.0;
    
    public static boolean sCacheValues = false;

    public Request(long id, int priority, boolean alternative, Student student) {
        iId = id;
        iPriority = priority;
        iAlternative = alternative;
        iStudent = student;
        iStudent.getRequests().add(this);
    }
    
    public long getId() {
        return iId;
    }
    
    public int getPriority() {
        return iPriority;
    }
    
    public boolean isAlternative() {
        return iAlternative;
    }
    
    public Student getStudent() {
        return iStudent;
    }
    
    public int compareTo(Object o) {
        if (o==null || !(o instanceof Request)) return -1;
        Request r = (Request)o;
        if (isAlternative()!=r.isAlternative())
            return (isAlternative()?1:-1);
        return Double.compare(getPriority(), r.getPriority());
    }
    
    public abstract Vector computeEnrollments();
    
    public Vector values() {
        Vector values = super.values();
        if (values!=null) return values;
        values = computeEnrollments();
        if (sCacheValues) setValues(values);
        return values;
    }
    
    public void assign(long iteration, Value value) {
        super.assign(iteration, value);
        Enrollment enrollment = (Enrollment)value;
        for (Iterator i=enrollment.getAssignments().iterator();i.hasNext();) {
            Assignment a = (Assignment)i.next();
            a.assigned(enrollment);
        }
    }

    public void unassign(long iteration) {
        if (getAssignment()!=null) {
            Enrollment enrollment = (Enrollment)getAssignment();
            for (Iterator i=enrollment.getAssignments().iterator();i.hasNext();) {
                Assignment a = (Assignment)i.next();
                a.unassigned(enrollment);
            }
        }
        super.unassign(iteration);
    }
    
    public abstract double getBound();
    
    public double getWeight() {
        return iWeight;
    }
    
    public void setWeight(double weight) {
        iWeight = weight;
    }
}
