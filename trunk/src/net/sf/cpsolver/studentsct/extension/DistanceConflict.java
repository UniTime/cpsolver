package net.sf.cpsolver.studentsct.extension;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import net.sf.cpsolver.coursett.Constants;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.RoomLocation;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.ifs.assignment.Assignment;
import net.sf.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import net.sf.cpsolver.ifs.assignment.context.ExtensionWithContext;
import net.sf.cpsolver.ifs.model.ModelListener;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.DistanceMetric;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.StudentSectioningModel.StudentSectioningModelContext;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * This extension computes student distant conflicts. Two sections that are
 * attended by the same student are considered in a distance conflict if they
 * are back-to-back taught in locations that are two far away. This means that
 * the (walking) distance in minutes between the two classes are longer than
 * the break time of the earlier class. See {@link DistanceMetric} for more details.
 * 
 * @see TimeLocation
 * @see Placement
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
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */

public class DistanceConflict extends ExtensionWithContext<Request, Enrollment, DistanceConflict.DistanceConflictContext> implements ModelListener<Request, Enrollment> {
    private static Logger sLog = Logger.getLogger(DistanceConflict.class);
    /** Debug flag */
    public static boolean sDebug = false;
    private DistanceMetric iDistanceMetric = null;

    /**
     * Constructor. Beside of other thigs, this constructor also uses
     * {@link StudentSectioningModel#setDistanceConflict(DistanceConflict)} to
     * set the this instance to the model.
     * 
     * @param solver
     *            constraint solver
     * @param properties
     *            configuration
     */
    public DistanceConflict(Solver<Request, Enrollment> solver, DataProperties properties) {
        super(solver, properties);
        if (solver != null)
            ((StudentSectioningModel) solver.currentSolution().getModel()).setDistanceConflict(this);
        iDistanceMetric = new DistanceMetric(properties);
    }
    
    /**
     * Alternative constructor (for online student sectioning)
     * @param metrics distance metrics
     * @param properties configuration
     */
    public DistanceConflict(DistanceMetric metrics, DataProperties properties) {
        super(null, properties);
        iDistanceMetric = metrics;
    }

    @Override
    public String toString() {
        return "DistanceConstraint";
    }
    
    public DistanceMetric getDistanceMetric() {
        return iDistanceMetric;
    }
    
    
    private Map<Long, Map<Long, Integer>> iDistanceCache = new HashMap<Long, Map<Long,Integer>>();
    protected int getDistanceInMinutes(RoomLocation r1, RoomLocation r2) {
        if (r1.getId().compareTo(r2.getId()) > 0) return getDistanceInMinutes(r2, r1);
        if (r1.getId().equals(r2.getId()) || r1.getIgnoreTooFar() || r2.getIgnoreTooFar())
            return 0;
        if (r1.getPosX() == null || r1.getPosY() == null || r2.getPosX() == null || r2.getPosY() == null)
            return iDistanceMetric.getMaxTravelDistanceInMinutes();
        Map<Long, Integer> other2distance = iDistanceCache.get(r1.getId());
        if (other2distance == null) {
            other2distance = new HashMap<Long, Integer>();
            iDistanceCache.put(r1.getId(), other2distance);
        }
        Integer distance = other2distance.get(r2.getId());
        if (distance == null) {
            distance = iDistanceMetric.getDistanceInMinutes(r1.getId(), r1.getPosX(), r1.getPosY(), r2.getId(), r2.getPosX(), r2.getPosY());
            other2distance.put(r2.getId(), distance);    
        }
        return distance;
    }

    protected int getDistanceInMinutes(Placement p1, Placement p2) {
        if (p1.isMultiRoom()) {
            if (p2.isMultiRoom()) {
                int dist = 0;
                for (RoomLocation r1 : p1.getRoomLocations()) {
                    for (RoomLocation r2 : p2.getRoomLocations()) {
                        dist = Math.max(dist, getDistanceInMinutes(r1, r2));
                    }
                }
                return dist;
            } else {
                if (p2.getRoomLocation() == null)
                    return 0;
                int dist = 0;
                for (RoomLocation r1 : p1.getRoomLocations()) {
                    dist = Math.max(dist, getDistanceInMinutes(r1, p2.getRoomLocation()));
                }
                return dist;
            }
        } else if (p2.isMultiRoom()) {
            if (p1.getRoomLocation() == null)
                return 0;
            int dist = 0;
            for (RoomLocation r2 : p2.getRoomLocations()) {
                dist = Math.max(dist, getDistanceInMinutes(p1.getRoomLocation(), r2));
            }
            return dist;
        } else {
            if (p1.getRoomLocation() == null || p2.getRoomLocation() == null)
                return 0;
            return getDistanceInMinutes(p1.getRoomLocation(), p2.getRoomLocation());
        }
    }
    
    /**
     * Return true if the given two sections are in distance conflict. This
     * means that the sections are back-to-back and that they are placed in
     * locations that are two far.
     * 
     * @param s1
     *            a section
     * @param s2
     *            a section
     * @return true, if the given sections are in a distance conflict
     */
    public boolean inConflict(Section s1, Section s2) {
        if (s1.getPlacement() == null || s2.getPlacement() == null)
            return false;
        TimeLocation t1 = s1.getTime();
        TimeLocation t2 = s2.getTime();
        if (!t1.shareDays(t2) || !t1.shareWeeks(t2))
            return false;
        int a1 = t1.getStartSlot(), a2 = t2.getStartSlot();
        if (getDistanceMetric().doComputeDistanceConflictsBetweenNonBTBClasses()) {
            if (a1 + t1.getNrSlotsPerMeeting() <= a2) {
                int dist = getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                if (dist > t1.getBreakTime() + Constants.SLOT_LENGTH_MIN * (a2 - a1 - t1.getLength()))
                    return true;
            } else if (a2 + t2.getNrSlotsPerMeeting() <= a1) {
                int dist = getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                if (dist > t2.getBreakTime() + Constants.SLOT_LENGTH_MIN * (a1 - a2 - t2.getLength()))
                    return true;
            }
        } else {
            if (a1 + t1.getNrSlotsPerMeeting() == a2) {
                int dist = getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                if (dist > t1.getBreakTime())
                    return true;
            } else if (a2 + t2.getNrSlotsPerMeeting() == a1) {
                int dist = getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
                if (dist > t2.getBreakTime())
                    return true;
            }
        }
        return false;
    }

    /**
     * Return number of distance conflict of a (course) enrollment. It is the
     * number of pairs of assignments of the enrollment that are in a distance
     * conflict.
     * 
     * @param e1
     *            an enrollment
     * @return number of distance conflicts
     */
    public int nrConflicts(Enrollment e1) {
        if (!e1.isCourseRequest())
            return 0;
        int cnt = 0;
        for (Section s1 : e1.getSections()) {
            for (Section s2 : e1.getSections()) {
                if (s1.getId() < s2.getId() && inConflict(s1, s2))
                    cnt ++;
            }
        }
        return cnt;
    }

    /**
     * Return number of distance conflicts that are between two enrollments. It
     * is the number of pairs of assignments of these enrollments that are in a
     * distance conflict.
     * 
     * @param e1
     *            an enrollment
     * @param e2
     *            an enrollment
     * @return number of distance conflict between given enrollments
     */
    public int nrConflicts(Enrollment e1, Enrollment e2) {
        if (!e1.isCourseRequest() || !e2.isCourseRequest() || !e1.getStudent().equals(e2.getStudent()))
            return 0;
        int cnt = 0;
        for (Section s1 : e1.getSections()) {
            for (Section s2 : e2.getSections()) {
                if (inConflict(s1, s2))
                    cnt ++;
            }
        }
        return cnt;
    }

    /**
     * Return a set of distance conflicts ({@link Conflict} objects) of a
     * (course) enrollment.
     * 
     * @param e1
     *            an enrollment
     * @return list of distance conflicts that are between assignment of the
     *         given enrollment
     */
    public Set<Conflict> conflicts(Enrollment e1) {
        Set<Conflict> ret = new HashSet<Conflict>();
        if (!e1.isCourseRequest())
            return ret;
        for (Section s1 : e1.getSections()) {
            for (Section s2 : e1.getSections()) {
                if (s1.getId() < s2.getId() && inConflict(s1, s2))
                    ret.add(new Conflict(e1.getStudent(), e1, s1, e1, s2));
            }
        }
        return ret;
    }

    /**
     * Return a set of distance conflicts ({@link Conflict} objects) between
     * given (course) enrollments.
     * 
     * @param e1
     *            an enrollment
     * @param e2
     *            an enrollment
     * @return list of distance conflicts that are between assignment of the
     *         given enrollments
     */
    public Set<Conflict> conflicts(Enrollment e1, Enrollment e2) {
        Set<Conflict> ret = new HashSet<Conflict>();
        if (!e1.isCourseRequest() || !e2.isCourseRequest() || !e1.getStudent().equals(e2.getStudent()))
            return ret;
        for (Section s1 : e1.getSections()) {
            for (Section s2 : e2.getSections()) {
                if (inConflict(s1, s2))
                    ret.add(new Conflict(e1.getStudent(), e1, s1, e2, s2));
            }
        }
        return ret;
    }

    /**
     * The set of all conflicts ({@link Conflict} objects) of the given
     * enrollment and other enrollments that are assignmed to the same student.
     */
    public Set<Conflict> allConflicts(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
        Set<Conflict> ret = conflicts(enrollment);
        if (!enrollment.isCourseRequest())
            return ret;
        for (Request request : enrollment.getStudent().getRequests()) {
            if (request.equals(enrollment.getRequest()) || assignment.getValue(request) == null)
                continue;
            ret.addAll(conflicts(enrollment, assignment.getValue(request)));
        }
        return ret;
    }

    /** Checks the counter counting all conflicts */
    public void checkAllConflicts(Assignment<Request, Enrollment> assignment) {
        getContext(assignment).checkAllConflicts(assignment);
    }
    
    /** Actual number of all distance conflicts */
    public int getTotalNrConflicts(Assignment<Request, Enrollment> assignment) {
        return getContext(assignment).getTotalNrConflicts();
    }

    /**
     * Compute the actual number of all distance conflicts. Should be equal to
     * {@link DistanceConflict#getTotalNrConflicts(Assignment)}.
     */
    public int countTotalNrConflicts(Assignment<Request, Enrollment> assignment) {
        int total = 0;
        for (Request r1 : getModel().variables()) {
            if (assignment.getValue(r1) == null || !(r1 instanceof CourseRequest))
                continue;
            Enrollment e1 = assignment.getValue(r1);
            total += nrConflicts(e1);
            for (Request r2 : r1.getStudent().getRequests()) {
                Enrollment e2 = assignment.getValue(r2);
                if (e2 == null || r1.getId() >= r2.getId() || !(r2 instanceof CourseRequest))
                    continue;
                total += nrConflicts(e1, e2);
            }
        }
        return total;
    }

    /**
     * Compute a set of all distance conflicts ({@link Conflict} objects).
     */
    public Set<Conflict> computeAllConflicts(Assignment<Request, Enrollment> assignment) {
        Set<Conflict> ret = new HashSet<Conflict>();
        for (Request r1 : getModel().variables()) {
            Enrollment e1 = assignment.getValue(r1);
            if (e1 == null || !(r1 instanceof CourseRequest))
                continue;
            ret.addAll(conflicts(e1));
            for (Request r2 : r1.getStudent().getRequests()) {
                Enrollment e2 = assignment.getValue(r2);
                if (e2 == null || r1.getId() >= r2.getId() || !(r2 instanceof CourseRequest))
                    continue;
                ret.addAll(conflicts(e1, e2));
            }
        }
        return ret;
    }
    
    /**
     * Return a set of all distance conflicts ({@link Conflict} objects).
     */
    public Set<Conflict> getAllConflicts(Assignment<Request, Enrollment> assignment) {
        return getContext(assignment).getAllConflicts();
    }

    /**
     * Called before a value is assigned to a variable.
     */
    @Override
    public void beforeAssigned(Assignment<Request, Enrollment> assignment, long iteration, Enrollment value) {
        getContext(assignment).beforeAssigned(assignment, iteration, value);
    }

    /**
     * Called after a value is assigned to a variable.
     */
    @Override
    public void afterAssigned(Assignment<Request, Enrollment> assignment, long iteration, Enrollment value) {
        getContext(assignment).afterAssigned(assignment, iteration, value);
    }

    /**
     * Called after a value is unassigned from a variable.
     */
    @Override
    public void afterUnassigned(Assignment<Request, Enrollment> assignment, long iteration, Enrollment value) {
        getContext(assignment).afterUnassigned(assignment, iteration, value);
    }

    /** A representation of a distance conflict */
    public static class Conflict {
        private Student iStudent;
        private Section iS1, iS2;
        private Enrollment iE1, iE2;
        private int iHashCode;

        /**
         * Constructor
         * 
         * @param student
         *            related student
         * @param s1
         *            first conflicting section
         * @param s2
         *            second conflicting section
         */
        public Conflict(Student student, Enrollment e1, Section s1, Enrollment e2, Section s2) {
            iStudent = student;
            if (s1.getId() < s2.getId()) {
                iS1 = s1;
                iS2 = s2;
                iE1 = e1;
                iE2 = e2;
            } else {
                iS1 = s2;
                iS2 = s1;
                iE1 = e2;
                iE2 = e1;
            }
            iHashCode = (iStudent.getId() + ":" + iS1.getId() + ":" + iS2.getId()).hashCode();
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
        
        /** First request */
        public Request getR1() {
            return iE1.getRequest();
        }
        
        /** Second request */
        public Request getR2() {
            return iE2.getRequest();
        }
        
        /** First enrollment */
        public Enrollment getE1() {
            return iE1;
        }

        /** Second enrollment */
        public Enrollment getE2() {
            return iE2;
        }

        @Override
        public int hashCode() {
            return iHashCode;
        }

        /** The distance between conflicting sections */
        public double getDistance(DistanceMetric dm) {
            return Placement.getDistanceInMeters(dm, getS1().getPlacement(), getS2().getPlacement());
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Conflict)) return false;
            Conflict c = (Conflict) o;
            return getStudent().equals(c.getStudent()) && getS1().equals(c.getS1()) && getS2().equals(c.getS2());
        }

        @Override
        public String toString() {
            return getStudent() + ": " + getS1() + " -- " + getS2();
        }
    }
    
    public class DistanceConflictContext implements AssignmentConstraintContext<Request, Enrollment> {
        private Set<Conflict> iAllConflicts = new HashSet<Conflict>();
        private Request iOldVariable = null;
        private Enrollment iUnassignedValue = null;

        public DistanceConflictContext(Assignment<Request, Enrollment> assignment) {
            iAllConflicts = computeAllConflicts(assignment);
            StudentSectioningModelContext cx = ((StudentSectioningModel)getModel()).getContext(assignment);
            for (Conflict c: iAllConflicts)
                cx.add(assignment, c);
        }
        
        /**
         * Called before a value is assigned to a variable.
         */
        public void beforeAssigned(Assignment<Request, Enrollment> assignment, long iteration, Enrollment value) {
            if (value != null) {
                Enrollment old = assignment.getValue(value.variable());
                if (old != null) {
                    unassigned(assignment, old);
                    iUnassignedValue = old;
                }
                iOldVariable = value.variable();
            }
        }
        
        /**
         * Called after a value is assigned to a variable.
         */
        public void afterAssigned(Assignment<Request, Enrollment> assignment, long iteration, Enrollment value) {
            iOldVariable = null;
            iUnassignedValue = null;
            if (value != null)
                assigned(assignment, value);
        }
        
        /**
         * Called after a value is unassigned from a variable.
         */
        public void afterUnassigned(Assignment<Request, Enrollment> assignment, long iteration, Enrollment value) {
            if (value != null && !value.equals(iUnassignedValue))
                unassigned(assignment, value);
        }

        /**
         * Called when a value is assigned to a variable. Internal number of
         * distance conflicts is updated, see
         * {@link DistanceConflict#getTotalNrConflicts(Assignment)}.
         */
        @Override
        public void assigned(Assignment<Request, Enrollment> assignment, Enrollment value) {
            StudentSectioningModelContext cx = ((StudentSectioningModel)getModel()).getContext(assignment);
            for (Conflict c: allConflicts(assignment, value)) {
                if (iAllConflicts.add(c))
                    cx.add(assignment, c);
            }
            if (sDebug) {
                sLog.debug("A:" + value.variable() + " := " + value);
                int inc = nrConflicts(value);
                if (inc != 0) {
                    sLog.debug("-- DC+" + inc + " A: " + value.variable() + " := " + value);
                    for (Iterator<Conflict> i = allConflicts(assignment, value).iterator(); i.hasNext();)
                        sLog.debug("  -- " + i.next());
                }
            }
        }

        /**
         * Called when a value is unassigned from a variable. Internal number of
         * distance conflicts is updated, see
         * {@link DistanceConflict#getTotalNrConflicts(Assignment)}.
         */
        @Override
        public void unassigned(Assignment<Request, Enrollment> assignment, Enrollment value) {
            if (value.variable().equals(iOldVariable))
                return;
            StudentSectioningModelContext cx = ((StudentSectioningModel)getModel()).getContext(assignment);
            for (Conflict c: allConflicts(assignment, value)) {
                if (iAllConflicts.remove(c))
                    cx.remove(assignment, c);
            }
            if (sDebug) {
                sLog.debug("U:" + value.variable() + " := " + value);
                int dec = nrAllConflicts(assignment, value);
                if (dec != 0) {
                    sLog.debug("-- DC+" + dec + " U: " + value.variable() + " := " + value);
                    Set<Conflict> confs = allConflicts(assignment, value);
                    for (Iterator<Conflict> i = confs.iterator(); i.hasNext();)
                        sLog.debug("  -- " + i.next());
                }
            }
        }
        
        /** Checks the counter counting all conflicts */
        public void checkAllConflicts(Assignment<Request, Enrollment> assignment) {
            Set<Conflict> allConfs = computeAllConflicts(assignment);
            if (iAllConflicts.size() != allConfs.size()) {
                sLog.error("Different number of conflicts " + iAllConflicts.size() + "!=" + allConfs.size());
                for (Iterator<Conflict> i = allConfs.iterator(); i.hasNext();) {
                    Conflict c = i.next();
                    if (!iAllConflicts.contains(c))
                        sLog.debug("  +add+ " + c);
                }
                for (Iterator<Conflict> i = iAllConflicts.iterator(); i.hasNext();) {
                    Conflict c = i.next();
                    if (!allConfs.contains(c))
                        sLog.debug("  -rem- " + c);
                }
                iAllConflicts = allConfs;
            }
        }
        
        /** Actual number of all distance conflicts */
        public int getTotalNrConflicts() {
            return iAllConflicts.size();
        }
        
        /**
         * Return a set of all distance conflicts ({@link Conflict} objects).
         */
        public Set<Conflict> getAllConflicts() {
            return iAllConflicts;
        }
        
        /**
         * Total sum of all conflict of the given enrollment and other enrollments
         * that are assignmed to the same student.
         */
        public int nrAllConflicts(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
            if (!enrollment.isCourseRequest())
                return 0;
            int cnt = nrConflicts(enrollment);
            Request old = iOldVariable;
            for (Request request : enrollment.getStudent().getRequests()) {
                if (request.equals(enrollment.getRequest()) || assignment.getValue(request) == null || request.equals(old))
                    continue;
                cnt += nrConflicts(enrollment, assignment.getValue(request));
            }
            return cnt;
        }
    }

    @Override
    public DistanceConflictContext createAssignmentContext(Assignment<Request, Enrollment> assignment) {
        return new DistanceConflictContext(assignment);
    }
}
