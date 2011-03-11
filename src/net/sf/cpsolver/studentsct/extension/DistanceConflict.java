package net.sf.cpsolver.studentsct.extension;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.RoomLocation;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.ifs.extension.Extension;
import net.sf.cpsolver.ifs.model.ModelListener;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.DistanceMetric;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
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

public class DistanceConflict extends Extension<Request, Enrollment> implements ModelListener<Request, Enrollment> {
    private static Logger sLog = Logger.getLogger(DistanceConflict.class);
    private Set<Conflict> iAllConflicts = new HashSet<Conflict>();
    /** Debug flag */
    public static boolean sDebug = false;
    private Request iOldVariable = null;
    private Enrollment iUnassignedValue = null;
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
     * Initialize extension
     */
    @Override
    public boolean init(Solver<Request, Enrollment> solver) {
        StudentSectioningModel m = (StudentSectioningModel)solver.currentSolution().getModel();
        iAllConflicts = computeAllConflicts();
        for (Conflict c: iAllConflicts)
            m.add(c);
        return true;
    }

    @Override
    public String toString() {
        return "DistanceConstraint";
    }
    
    
    private Map<Long, Map<Long, Integer>> iDistanceCache = new HashMap<Long, Map<Long,Integer>>();
    protected int getDistanceInMinutes(RoomLocation r1, RoomLocation r2) {
        if (r1.getId().compareTo(r2.getId()) > 0) return getDistanceInMinutes(r2, r1);
        if (r1.getId().equals(r2.getId()) || r1.getIgnoreTooFar() || r2.getIgnoreTooFar())
            return 0;
        if (r1.getPosX() == null || r1.getPosY() == null || r2.getPosX() == null || r2.getPosY() == null)
            return 60;
        Map<Long, Integer> other2distance = iDistanceCache.get(r1.getId());
        if (other2distance == null) {
            other2distance = new HashMap<Long, Integer>();
            iDistanceCache.put(r1.getId(), other2distance);
        }
        Integer distance = other2distance.get(r2.getId());
        if (distance == null) {
            distance = iDistanceMetric.getDistanceInMinutes(r1.getPosX(), r1.getPosY(), r2.getPosX(), r2.getPosY());
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
        if (a1 + t1.getNrSlotsPerMeeting() == a2) {
            int dist = getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
            if (dist > t1.getBreakTime())
                return true;
        } else if (a2 + t2.getNrSlotsPerMeeting() == a1) {
            int dist = getDistanceInMinutes(s1.getPlacement(), s2.getPlacement());
            if (dist > t2.getBreakTime())
                return true;
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
     * Total sum of all conflict of the given enrollment and other enrollments
     * that are assignmed to the same student.
     */
    public int nrAllConflicts(Enrollment enrollment) {
        if (!enrollment.isCourseRequest())
            return 0;
        int cnt = nrConflicts(enrollment);
        for (Request request : enrollment.getStudent().getRequests()) {
            if (request.equals(enrollment.getRequest()) || request.getAssignment() == null
                    || request.equals(iOldVariable))
                continue;
            cnt += nrConflicts(enrollment, request.getAssignment());
        }
        return cnt;
    }

    /**
     * The set of all conflicts ({@link Conflict} objects) of the given
     * enrollment and other enrollments that are assignmed to the same student.
     */
    public Set<Conflict> allConflicts(Enrollment enrollment) {
        Set<Conflict> ret = new HashSet<Conflict>();
        if (!enrollment.isCourseRequest())
            return ret;
        for (Request request : enrollment.getStudent().getRequests()) {
            if (request.equals(enrollment.getRequest()) || request.getAssignment() == null)
                continue;
            ret.addAll(conflicts(enrollment, request.getAssignment()));
        }
        return ret;
    }

    /**
     * Called when a value is assigned to a variable. Internal number of
     * distance conflicts is updated, see
     * {@link DistanceConflict#getTotalNrConflicts()}.
     */
    public void assigned(long iteration, Enrollment value) {
        StudentSectioningModel m = (StudentSectioningModel)value.variable().getModel();
        for (Conflict c: allConflicts(value)) {
            if (iAllConflicts.add(c))
                m.add(c);
        }
        if (sDebug) {
            sLog.debug("A:" + value.variable() + " := " + value);
            int inc = nrConflicts(value);
            if (inc != 0) {
                sLog.debug("-- DC+" + inc + " A: " + value.variable() + " := " + value);
                for (Iterator<Conflict> i = allConflicts(value).iterator(); i.hasNext();)
                    sLog.debug("  -- " + i.next());
            }
        }
    }

    /**
     * Called when a value is unassigned from a variable. Internal number of
     * distance conflicts is updated, see
     * {@link DistanceConflict#getTotalNrConflicts()}.
     */
    public void unassigned(long iteration, Enrollment value) {
        if (value.variable().equals(iOldVariable))
            return;
        StudentSectioningModel m = (StudentSectioningModel)value.variable().getModel();
        for (Conflict c: allConflicts(value)) {
            if (iAllConflicts.remove(c))
                m.remove(c);
        }
        if (sDebug) {
            sLog.debug("U:" + value.variable() + " := " + value);
            int dec = nrAllConflicts(value);
            if (dec != 0) {
                sLog.debug("-- DC+" + dec + " U: " + value.variable() + " := " + value);
                Set<Conflict> confs = allConflicts(value);
                for (Iterator<Conflict> i = confs.iterator(); i.hasNext();)
                    sLog.debug("  -- " + i.next());
            }
        }
    }

    /** Checks the counter counting all conflicts */
    public void checkAllConflicts() {
        Set<Conflict> allConfs = computeAllConflicts();
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
     * Compute the actual number of all distance conflicts. Should be equal to
     * {@link DistanceConflict#getTotalNrConflicts()}.
     */
    public int countTotalNrConflicts() {
        int total = 0;
        for (Request r1 : getModel().variables()) {
            if (r1.getAssignment() == null || !(r1 instanceof CourseRequest))
                continue;
            total += nrConflicts(r1.getAssignment());
            for (Request r2 : r1.getStudent().getRequests()) {
                if (r2.getAssignment() == null || r1.getId() >= r2.getId() || !(r2 instanceof CourseRequest))
                    continue;
                total += nrConflicts(r1.getAssignment(), r2.getAssignment());
            }
        }
        return total;
    }

    /**
     * Compute a set of all distance conflicts ({@link Conflict} objects).
     */
    public Set<Conflict> computeAllConflicts() {
        Set<Conflict> ret = new HashSet<Conflict>();
        for (Request r1 : getModel().variables()) {
            if (r1.getAssignment() == null || !(r1 instanceof CourseRequest))
                continue;
            ret.addAll(conflicts(r1.getAssignment()));
            for (Request r2 : r1.getStudent().getRequests()) {
                if (r2.getAssignment() == null || r1.getId() >= r2.getId() || !(r2 instanceof CourseRequest))
                    continue;
                ret.addAll(conflicts(r1.getAssignment(), r2.getAssignment()));
            }
        }
        return ret;
    }
    
    /**
     * Return a set of all distance conflicts ({@link Conflict} objects).
     */
    public Set<Conflict> getAllConflicts() {
        return iAllConflicts;
    }

    /**
     * Called before a value is assigned to a variable.
     */
    @Override
    public void beforeAssigned(long iteration, Enrollment value) {
        if (value != null) {
            if (value.variable().getAssignment() != null) {
                unassigned(iteration, value.variable().getAssignment());
                iUnassignedValue = value.variable().getAssignment();
            }
            iOldVariable = value.variable();
        }
    }

    /**
     * Called after a value is assigned to a variable.
     */
    @Override
    public void afterAssigned(long iteration, Enrollment value) {
        iOldVariable = null;
        iUnassignedValue = null;
        if (value != null)
            assigned(iteration, value);
    }

    /**
     * Called after a value is unassigned from a variable.
     */
    @Override
    public void afterUnassigned(long iteration, Enrollment value) {
        if (value != null && !value.equals(iUnassignedValue))
            unassigned(iteration, value);
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
}
