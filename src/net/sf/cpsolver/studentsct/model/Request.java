package net.sf.cpsolver.studentsct.model;

import java.util.Iterator;
import java.util.Vector;

import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;

/**
 * Representation of a request of a student for a course(s) or a free time. This can be either {@link CourseRequest} or {@link FreeTimeRequest}. 
 * Each request contains id, priority, weight, and a student. A request can be also marked as alternative. 
 * <br><br>
 * For each student, all non-alternative requests should be satisfied (an enrollment is assigned to a request). If not, an alternative request can be 
 * assigned instead of a non-alternative course request. In the case when only one of two requests can be assigned, the one with the lowest priority 
 * is preferred. 
 * <br><br>
 * 
 * @version
 * StudentSct 1.1 (Student Sectioning)<br>
 * Copyright (C) 2007 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
public abstract class Request extends Variable implements Comparable {
    private long iId = -1;
    private int iPriority = 0;
    private boolean iAlternative = false;
    private Student iStudent = null;
    private double iWeight = 1.0;
    /** True means that method {@link Request#values()} will cache its results. */ 
    public static boolean sCacheValues = false;

    /** Constructor
     * @param id course/free time request unique id
     * @param priority request priority -- if there is a choice, request with lower priority is more preferred to be assigned
     * @param alternative true if the request is alternative (alternative request can be assigned instead of a non-alternative course requests, if it is left unassigned) 
     * @param student student to which this request belongs
     */
    public Request(long id, int priority, boolean alternative, Student student) {
        iId = id;
        iPriority = priority;
        iAlternative = alternative;
        iStudent = student;
        iStudent.getRequests().add(this);
    }
    
    /** Request id */
    public long getId() {
        return iId;
    }
    
    /** Request priority -- if there is a choice, request with lower priority is more preferred to be assigned */
    public int getPriority() {
        return iPriority;
    }
    
    /** True, if the request is alternative (alternative request can be assigned instead of a non-alternative course requests, if it is left unassigned) */
    public boolean isAlternative() {
        return iAlternative;
    }
    
    /** Student to which this request belongs */
    public Student getStudent() {
        return iStudent;
    }
    
    /** Compare to requests, non-alternative requests go first, otherwise use priority (a request with lower priority goes first) */ 
    public int compareTo(Object o) {
        if (o==null || !(o instanceof Request)) return -1;
        Request r = (Request)o;
        if (isAlternative()!=r.isAlternative())
            return (isAlternative()?1:-1);
        return Double.compare(getPriority(), r.getPriority());
    }
    
    /** Compute available enrollments */
    public abstract Vector computeEnrollments();
    
    /** Domain of this variable -- list of available enrollments. Method {@link Request#computeEnrollments()} is used. */
    public Vector values() {
        Vector values = super.values();
        if (values!=null) return values;
        values = computeEnrollments();
        if (sCacheValues) setValues(values);
        return values;
    }
    
    /** Assign given enrollment to this request. This method also calls {@link Assignment#assigned(Enrollment)} on for all the assignments of the enrollment. */
    public void assign(long iteration, Value value) {
        super.assign(iteration, value);
        Enrollment enrollment = (Enrollment)value;
        for (Iterator i=enrollment.getAssignments().iterator();i.hasNext();) {
            Assignment a = (Assignment)i.next();
            a.assigned(enrollment);
        }
    }

    /** Unassign currently assigned enrollment from this request. This method also calls {@link Assignment#unassigned(Enrollment)} on for all the assignments of the current enrollment. */
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
    
    /** Get bound, i.e., the value of the best possible enrollment */
    public abstract double getBound();
    
    /** Request weight, set by default to 1.0, defines the amount of space which will be taken in the section by this request. */ 
    public double getWeight() {
        return iWeight;
    }
    
    /** Set request weight. It defines the amount of space which will be taken in the section by this request. */
    public void setWeight(double weight) {
        iWeight = weight;
    }
}
