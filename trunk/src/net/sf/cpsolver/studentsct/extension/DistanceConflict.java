package net.sf.cpsolver.studentsct.extension;

import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;

import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.ifs.extension.Extension;
import net.sf.cpsolver.ifs.model.ModelListener;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * This extension computes student distant conflicts.
 * Two sections that are attended by the same student are considered in a 
 * distance conflict if they are back-to-back taught in locations
 * that are two far away. The allowed distance is provided by method
 * {@link DistanceConflict#getAllowedDistance(TimeLocation)}.
 * 
 * @see TimeLocation
 * @see Placement
 * 
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

public class DistanceConflict extends Extension implements ModelListener {
    private static Logger sLog = Logger.getLogger(DistanceConflict.class);
    private static DecimalFormat sDF = new DecimalFormat("0.000");
    private double iTotalNrConflicts = 0;
    private HashSet iAllConflicts = new HashSet();
    /** Debug flag */
    public static boolean sDebug = false;
    private Variable iOldVariable = null;
    
    /**
     * Constructor.
     * Beside of other thigs, this constructor also uses 
     * {@link StudentSectioningModel#setDistanceConflict(DistanceConflict)} to
     * set the this instance to the model. 
     * @param solver constraint solver
     * @param properties configuration
     */
    public DistanceConflict(Solver solver, DataProperties properties) {
        super(solver, properties);
        if (solver!=null)
            ((StudentSectioningModel)solver.currentSolution().getModel()).setDistanceConflict(this);
    }
    
    /** 
     * Initialize extension
     */
    public boolean init(Solver solver) {
        iTotalNrConflicts = countTotalNrConflicts();
        return true;
    }
    
    public String toString() {
        return "DistanceConstraint";
    }
    
    /**
     * Return true if the given two sections are in distance conflict.
     * This means that the sections are back-to-back and that they are
     * placed in locations that are two far. 
     * @param s1 a section
     * @param s2 a section
     * @return true, if the given sections are in a distance conflict
     */
    public boolean inConflict(Section s1, Section s2) {
        if (s1.getPlacement()==null || s2.getPlacement()==null) return false;
        TimeLocation t1 = s1.getTime();
        TimeLocation t2 = s2.getTime();
        if (!t1.shareDays(t2) || !t1.shareWeeks(t2)) return false;
        int a1 = t1.getStartSlot(), a2 = t2.getStartSlot();
        if (a1+t1.getNrSlotsPerMeeting()==a2) {
            double dist = Placement.getDistance(s1.getPlacement(), s2.getPlacement());
            if (dist>getAllowedDistance(t1)) return true;
        } else if (a2+t2.getNrSlotsPerMeeting()==a1) {
            double dist = Placement.getDistance(s1.getPlacement(), s2.getPlacement());
            if (dist>getAllowedDistance(t2)) return true;
        }
        return false;
    }
    
    /**
     * Return number of distance conflict of a (course) enrollment.
     * It is the number of pairs of assignments of the enrollment
     * that are in a distance conflict, weighted by the 
     * request's weight (see {@link Request#getWeight()}).
     * @param e1 an enrollment
     * @return number of distance conflicts
     */
    public double nrConflicts(Enrollment e1) {
        if (!e1.isCourseRequest()) return 0;
        double cnt = 0;
        for (Iterator i1=e1.getAssignments().iterator();i1.hasNext();) {
            Section s1 = (Section)i1.next();
            for (Iterator i2=e1.getAssignments().iterator();i2.hasNext();) {
                Section s2 = (Section)i2.next();
                if (s1.getId()<s2.getId() && inConflict(s1,s2)) cnt+=e1.getRequest().getWeight();
            }
        }
        return cnt;
    }
    
    /**
     * Return number of distance conflicts that are between two enrollments.
     * It is the number of pairs of assignments of these enrollments
     * that are in a distance conflict, weighted by the average 
     * (see {@link DistanceConflict#avg(double, double)}) of the requests'
     * weight (see {@link Request#getWeight()}).
     * @param e1 an enrollment
     * @param e2 an enrollment
     * @return number of distance conflict between given enrollments
     */
    public double nrConflicts(Enrollment e1, Enrollment e2) {
        if (!e1.isCourseRequest() || !e2.isCourseRequest() || !e1.getStudent().equals(e2.getStudent())) return 0;
        double cnt = 0;
        for (Iterator i1=e1.getAssignments().iterator();i1.hasNext();) {
            Section s1 = (Section)i1.next();
            for (Iterator i2=e2.getAssignments().iterator();i2.hasNext();) {
                Section s2 = (Section)i2.next();
                if (inConflict(s1,s2)) cnt+=avg(e1.getRequest().getWeight(),e2.getRequest().getWeight());
            }
        }
        return cnt;
    }
    
    /**
     * Return a set of distance conflicts ({@link Conflict} objects) of a (course) enrollment.
     * @param e1 an enrollment
     * @return list of distance conflicts that are between assignment of the given enrollment
     */
    public HashSet conflicts(Enrollment e1) {
        HashSet ret = new HashSet();
        if (!e1.isCourseRequest()) return ret;
        for (Iterator i1=e1.getAssignments().iterator();i1.hasNext();) {
            Section s1 = (Section)i1.next();
            for (Iterator i2=e1.getAssignments().iterator();i2.hasNext();) {
                Section s2 = (Section)i2.next();
                if (s1.getId()<s2.getId() && inConflict(s1,s2)) 
                    ret.add(new Conflict(e1.getRequest().getWeight(),e1.getStudent(),s1,s2));
            }
        }
        return ret;
    }
    
    /**
     * Return a set of distance conflicts ({@link Conflict} objects) between given (course) enrollments.
     * @param e1 an enrollment
     * @param e2 an enrollment
     * @return list of distance conflicts that are between assignment of the given enrollments
     */
    public HashSet conflicts(Enrollment e1, Enrollment e2) {
        HashSet ret = new HashSet();
        if (!e1.isCourseRequest() || !e2.isCourseRequest() || !e1.getStudent().equals(e2.getStudent())) return ret;
        for (Iterator i1=e1.getAssignments().iterator();i1.hasNext();) {
            Section s1 = (Section)i1.next();
            for (Iterator i2=e2.getAssignments().iterator();i2.hasNext();) {
                Section s2 = (Section)i2.next();
                if (inConflict(s1,s2))
                    ret.add(new Conflict(avg(e1.getRequest().getWeight(),e2.getRequest().getWeight()),e1.getStudent(),s1,s2));
            }
        }
        return ret;
    }

    /**
     * Allowed distance for the course that follows the given time assignment.
     * @param time a time assignment of the first of two sections that are back-to-back
     * @return the maximal allowed distance
     */
    public double getAllowedDistance(TimeLocation time) {
        if (time.getBreakTime()>=15) return 100.0;
        return 67.0;
    }
    
    /** 
     * Total sum of all conflict of the given enrollment and other enrollments that are assignmed to the same student.
     */
    public double nrAllConflicts(Enrollment enrollment) {
        if (!enrollment.isCourseRequest()) return 0;
        double cnt = nrConflicts(enrollment);
        for (Enumeration e=enrollment.getStudent().getRequests().elements(); e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (request.equals(enrollment.getRequest()) || request.getAssignment()==null || request.equals(iOldVariable)) continue;
            cnt += nrConflicts(enrollment, (Enrollment)request.getAssignment());
        }
        return cnt;
    }
    
    /** 
     * The set of all conflicts ({@link Conflict} objects) of the given enrollment and 
     * other enrollments that are assignmed to the same student.
     */
    public HashSet allConflicts(Enrollment enrollment) {
        HashSet ret = new HashSet();
        if (!enrollment.isCourseRequest()) return ret;
        for (Enumeration e=enrollment.getStudent().getRequests().elements(); e.hasMoreElements();) {
            Request request = (Request)e.nextElement();
            if (request.equals(enrollment.getRequest()) || request.getAssignment()==null) continue;
            ret.addAll(conflicts(enrollment, (Enrollment)request.getAssignment()));
        }
        return ret;
    }
    
    /**
     * Called when a value is assigned to a variable.
     * Internal number of distance conflicts is updated, see {@link DistanceConflict#getTotalNrConflicts()}.
     */
    public void assigned(long iteration, Value value) {
        double inc = nrAllConflicts((Enrollment)value);
        iTotalNrConflicts += inc;
        if (sDebug) {
            sLog.debug("A:"+value);
            HashSet allConfs = computeAllConflicts();
            double allConfWeight = 0.0;
            for (Iterator i=allConfs.iterator();i.hasNext();)
                allConfWeight += ((Conflict)i.next()).getWeight();
            if (Math.abs(iTotalNrConflicts-allConfWeight)>0.0001) {
                sLog.error("Different number of conflicts "+sDF.format(iTotalNrConflicts)+"!="+sDF.format(allConfWeight));
                for (Iterator i=allConfs.iterator();i.hasNext();) {
                    Conflict c = (Conflict)i.next();
                    if (!iAllConflicts.contains(c))
                        sLog.debug("  +add+ "+c);
                }
                for (Iterator i=iAllConflicts.iterator();i.hasNext();) {
                    Conflict c = (Conflict)i.next();
                    if (!allConfs.contains(c))
                        sLog.debug("  -rem- "+c);
                }
                iTotalNrConflicts = allConfWeight;
            }
            iAllConflicts = allConfs;
            if (inc!=0) {
                sLog.debug("-- DC+"+sDF.format(inc)+" A: "+value);
                HashSet confs = allConflicts((Enrollment)value);
                for (Iterator i=confs.iterator();i.hasNext();)
                    sLog.debug("  -- "+i.next());
            }
        }
    }
    
    /**
     * Called when a value is unassigned from a variable.
     * Internal number of distance conflicts is updated, see {@link DistanceConflict#getTotalNrConflicts()}.
     */
    public void unassigned(long iteration, Value value) {
        if (value.variable().equals(iOldVariable)) return;
        double dec = nrAllConflicts((Enrollment)value);
        iTotalNrConflicts -= dec;
        if (sDebug) {
            sLog.debug("U:"+value);
            if (dec!=0) {
                sLog.debug("-- DC-"+sDF.format(dec)+" U: "+value);
                HashSet confs = allConflicts((Enrollment)value);
                for (Iterator i=confs.iterator();i.hasNext();)
                    sLog.debug("  -- "+i.next());
            }
        }
    }
    
    /** Actual number of all distance conflicts */
    public double getTotalNrConflicts() {
        return iTotalNrConflicts;
    }
    
    /** 
     * Compute the actual number of all distance conflicts.
     * Should be equal to {@link DistanceConflict#getTotalNrConflicts()}.
     */
    public double countTotalNrConflicts() {
        double total = 0;
        for (Enumeration e=getModel().variables().elements();e.hasMoreElements();) {
            Request r1 = (Request)e.nextElement();
            if (r1.getAssignment()==null || !(r1 instanceof CourseRequest)) continue;
            total += nrConflicts((Enrollment)r1.getAssignment());
            for (Enumeration f=r1.getStudent().getRequests().elements();f.hasMoreElements();) {
                Request r2 = (Request)f.nextElement();
                if (r2.getAssignment()==null || r1.getId()>=r2.getId() || !(r2 instanceof CourseRequest)) continue;
                total += nrConflicts((Enrollment)r1.getAssignment(), (Enrollment)r2.getAssignment());
            }
        }
        return total;
    }
    
    /**
     * Compute a set of all distance conflicts ({@link Conflict} objects).
     */
    public HashSet computeAllConflicts() {
        HashSet ret = new HashSet();
        for (Enumeration e=getModel().variables().elements();e.hasMoreElements();) {
            Request r1 = (Request)e.nextElement();
            if (r1.getAssignment()==null || !(r1 instanceof CourseRequest)) continue;
            ret.addAll(conflicts((Enrollment)r1.getAssignment()));
            for (Enumeration f=r1.getStudent().getRequests().elements();f.hasMoreElements();) {
                Request r2 = (Request)f.nextElement();
                if (r2.getAssignment()==null || r1.getId()>=r2.getId() || !(r2 instanceof CourseRequest)) continue;
                ret.addAll(conflicts((Enrollment)r1.getAssignment(), (Enrollment)r2.getAssignment()));
            }
        }
        return ret;
    }
    
    /**
     * Quadratic average of two weights.
     */
    public double avg(double w1, double w2) {
        return Math.sqrt(w1*w2);
    }

    /**
     * Called before a value is assigned to a variable.
     */
    public void beforeAssigned(long iteration, Value value) {
        if (value!=null) {
            if (value.variable().getAssignment()!=null)
                unassigned(iteration, value.variable().getAssignment());
            iOldVariable=value.variable();
        }
    }
    
    /**
     * Called after a value is assigned to a variable.
     */
    public void afterAssigned(long iteration, Value value) {
        iOldVariable=null;
        if (value!=null) assigned(iteration, value);
    }

    /**
     * Called after a value is unassigned from a variable.
     */
    public void afterUnassigned(long iteration, Value value) {
        if (value!=null)
            unassigned(iteration, value);
    }
    
    /** A representation of a distance conflict */
    public class Conflict {
        private double iWeight;
        private Student iStudent;
        private Section iS1, iS2;
        private int iHashCode;
        
        /**
         * Constructor
         * @param weight conflict weight
         * @param student related student
         * @param s1 first conflicting section
         * @param s2 second conflicting section
         */
        public Conflict(double weight, Student student, Section s1, Section s2) {
            iWeight = weight;
            iStudent = student; 
            if (s1.getId()<s2.getId()) {
                iS1 = s1; iS2 = s2;
            } else {
                iS1 = s2; iS2 = s1;
            }
            iHashCode = (iStudent.getId()+":"+iS1.getId()+":"+iS2.getId()).hashCode();
        }
        
        /** Conflict weight */
        public double getWeight() {
            return iWeight;
        }
        
        /** Related student */
        public Student getStudent() {
            return iStudent;
        }
        
        /** First section */
        public Section getS1() {
            return iS1;
        }
        
        /** Second section */
        public Section getS2() {
            return iS2;
        }
        
        public int hashCode() {
            return iHashCode;
        }
        
        /** The distance between conflicting sections*/
        public double getDistance() {
            return Placement.getDistance(getS1().getPlacement(),getS2().getPlacement());
        }
        
        public boolean equals(Object o) {
            if (o==null || !(o instanceof Conflict))
                return false;
            Conflict c = (Conflict)o;
            return getStudent().getId()==c.getStudent().getId() && 
                getS1().getId()==c.getS1().getId() && 
                getS2().getId()==c.getS2().getId();
        }
        
        public String toString() {
            return getStudent()+": (w:"+sDF.format(getWeight())+",d:"+sDF.format(10.0*getDistance())+"m) "+getS1()+" -- "+getS2();
        }
    }
}
