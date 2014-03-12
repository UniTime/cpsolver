package net.sf.cpsolver.studentsct.extension;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import net.sf.cpsolver.ifs.assignment.Assignment;
import net.sf.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import net.sf.cpsolver.ifs.assignment.context.ExtensionWithContext;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.StudentSectioningModel.StudentSectioningModelContext;
import net.sf.cpsolver.studentsct.model.SctAssignment;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.FreeTimeRequest;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Section;
import net.sf.cpsolver.studentsct.model.Student;

/**
 * This extension computes time overlaps. Only sections that allow overlaps
 * (see {@link SctAssignment#isAllowOverlap()}) can overlap. This class counts
 * how many overlapping slots there are so that this number can be minimized.
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

public class TimeOverlapsCounter extends ExtensionWithContext<Request, Enrollment, TimeOverlapsCounter.TimeOverlapsCounterContext> {
    private static Logger sLog = Logger.getLogger(TimeOverlapsCounter.class);
    /** Debug flag */
    public static boolean sDebug = false;

    /**
     * Constructor. Beside of other things, this constructor also uses
     * {@link StudentSectioningModel#setTimeOverlaps(TimeOverlapsCounter)} to
     * set the this instance to the model.
     * 
     * @param solver
     *            constraint solver
     * @param properties
     *            configuration
     */
    public TimeOverlapsCounter(Solver<Request, Enrollment> solver, DataProperties properties) {
        super(solver, properties);
        if (solver != null)
            ((StudentSectioningModel) solver.currentSolution().getModel()).setTimeOverlaps(this);
    }

    @Override
    public String toString() {
        return "TimeOverlaps";
    }

    /**
     * Return true if the given two assignments are overlapping.
     * 
     * @param a1
     *            an assignment
     * @param a2
     *            an assignment
     * @return true, if the given sections are in an overlapping conflict
     */
    public boolean inConflict(SctAssignment a1, SctAssignment a2) {
        if (a1.getTime() == null || a2.getTime() == null) return false;
        if (a1 instanceof Section && a2 instanceof Section && ((Section)a1).isToIgnoreStudentConflictsWith(a2.getId())) return false;
        return a1.getTime().hasIntersection(a2.getTime());
    }
    
    /**
     * If the two sections are overlapping, return the number of slots of the overlap.
     * 
     * @param a1
     *            an assignment
     * @param a2
     *            an assignment
     * @return the number of overlapping slots against the number of slots of the smallest section
     */
    public int share(SctAssignment a1, SctAssignment a2) {
        if (!inConflict(a1, a2)) return 0;
        return a1.getTime().nrSharedDays(a2.getTime()) * a1.getTime().nrSharedHours(a2.getTime());
    }


    /**
     * Return number of time overlapping conflicts that are between two enrollments. It
     * is the total share between pairs of assignments of these enrollments that are in a
     * time overlap.
     * 
     * @param e1
     *            an enrollment
     * @param e2
     *            an enrollment
     * @return number of time overlapping conflict between given enrollments
     */
    public int nrConflicts(Enrollment e1, Enrollment e2) {
        if (!e1.getStudent().equals(e2.getStudent())) return 0;
        if (e1.getRequest() instanceof FreeTimeRequest && e2.getRequest() instanceof FreeTimeRequest) return 0;
        int cnt = 0;
        for (SctAssignment s1 : e1.getAssignments()) {
            for (SctAssignment s2 : e2.getAssignments()) {
                if (inConflict(s1, s2))
                    cnt += share(s1, s2);
            }
        }
        return cnt;
    }

    /**
     * Return a set of time overlapping conflicts ({@link Conflict} objects) between
     * given (course) enrollments.
     * 
     * @param e1
     *            an enrollment
     * @param e2
     *            an enrollment
     * @return list of time overlapping conflicts that are between assignment of the
     *         given enrollments
     */
    public Set<Conflict> conflicts(Enrollment e1, Enrollment e2) {
        Set<Conflict> ret = new HashSet<Conflict>();
        if (!e1.getStudent().equals(e2.getStudent())) return ret;
        if (e1.getRequest() instanceof FreeTimeRequest && e2.getRequest() instanceof FreeTimeRequest) return ret;
        for (SctAssignment s1 : e1.getAssignments()) {
            for (SctAssignment s2 : e2.getAssignments()) {
                if (inConflict(s1, s2))
                    ret.add(new Conflict(e1.getStudent(), share(s1, s2), e1, s1, e2, s2));
            }
        }
        return ret;
    }

    /**
     * Total sum of all free time conflict of the given enrollment.
     */
    public int nrFreeTimeConflicts(Enrollment enrollment) {
        if (enrollment.getRequest() instanceof FreeTimeRequest) return 0;
        int cnt = 0;
        for (Request request : enrollment.getStudent().getRequests()) {
            if (request instanceof FreeTimeRequest) {
                FreeTimeRequest ft = (FreeTimeRequest)request;
                for (SctAssignment section: enrollment.getAssignments())
                    cnt += share(section, ft);
            }
        }
        return cnt;
    }
    
    /**
     * Return a set of free time conflict of the given enrollment.
     */
    public Set<Conflict> freeTimeConflicts(Enrollment enrollment) {
        Set<Conflict> ret = new HashSet<Conflict>();
        if (enrollment.getRequest() instanceof FreeTimeRequest) return ret;
        for (Request request : enrollment.getStudent().getRequests()) {
            if (request instanceof FreeTimeRequest) {
                FreeTimeRequest ft = (FreeTimeRequest)request;
                for (SctAssignment section: enrollment.getAssignments()) {
                    if (inConflict(section, ft))
                        ret.add(new Conflict(enrollment.getStudent(), share(section, ft), enrollment, section, ft.createEnrollment(), ft));
                }
            }
        }
        return ret;
    }

    /** Actual number of all time overlapping conflicts */
    public int getTotalNrConflicts(Assignment<Request, Enrollment> assignment) {
        return getContext(assignment).getTotalNrConflicts();
    }
    
    public void checkTotalNrConflicts(Assignment<Request, Enrollment> assignment) {
        getContext(assignment).checkTotalNrConflicts(assignment);
    }

    /**
     * Return a set of all time overlapping conflicts ({@link Conflict} objects).
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

    /** A representation of a time overlapping conflict */
    public static class Conflict {
        private int iShare;
        private Student iStudent;
        private SctAssignment iA1, iA2;
        private Enrollment iE1, iE2;
        private int iHashCode;

        /**
         * Constructor
         * 
         * @param student
         *            related student
         * @param a1
         *            first conflicting section
         * @param a2
         *            second conflicting section
         */
        public Conflict(Student student, int share, Enrollment e1, SctAssignment a1, Enrollment e2, SctAssignment a2) {
            iStudent = student;
            if (a1.compareById(a2) < 0 ) {
                iA1 = a1;
                iA2 = a2;
                iE1 = e1;
                iE2 = e2;
            } else {
                iA1 = a2;
                iA2 = a1;
                iE1 = e2;
                iE2 = e1;
            }
            iHashCode = (iStudent.getId() + ":" + iA1.getId() + ":" + iA2.getId()).hashCode();
            iShare = share;
        }

        /** Related student */
        public Student getStudent() {
            return iStudent;
        }

        /** First section */
        public SctAssignment getS1() {
            return iA1;
        }

        /** Second section */
        public SctAssignment getS2() {
            return iA2;
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

        /** The number of overlapping slots against the number of slots of the smallest section */
        public int getShare() {
            return iShare;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Conflict)) return false;
            Conflict c = (Conflict) o;
            return getStudent().equals(c.getStudent()) && getS1().equals(c.getS1()) && getS2().equals(c.getS2());
        }

        @Override
        public String toString() {
            return getStudent() + ": (s:" + getShare() + ") " + getS1() + " -- " + getS2();
        }
    }
    
    public class TimeOverlapsCounterContext implements AssignmentConstraintContext<Request, Enrollment> {
        private int iTotalNrConflicts = 0;
        private Set<Conflict> iAllConflicts = new HashSet<Conflict>();
        private Request iOldVariable = null;
        private Enrollment iUnassignedValue = null;

        public TimeOverlapsCounterContext(Assignment<Request, Enrollment> assignment) {
            iTotalNrConflicts = countTotalNrConflicts(assignment);
            if (sDebug)
                iAllConflicts = computeAllConflicts(assignment);
            StudentSectioningModelContext cx = ((StudentSectioningModel)getModel()).getContext(assignment);
            for (Conflict c: computeAllConflicts(assignment))
                cx.add(assignment, c);
        }

        /**
         * Called when a value is assigned to a variable. Internal number of
         * time overlapping conflicts is updated, see
         * {@link TimeOverlapsCounter#getTotalNrConflicts(Assignment)}.
         */
        @Override
        public void assigned(Assignment<Request, Enrollment> assignment, Enrollment value) {
            StudentSectioningModelContext cx = ((StudentSectioningModel)getModel()).getContext(assignment);
            for (Conflict c: allConflicts(assignment, value)) {
                iTotalNrConflicts += c.getShare();
                cx.add(assignment, c);
            }
            if (sDebug) {
                sLog.debug("A:" + value.variable() + " := " + value);
                int inc = nrAllConflicts(assignment, value);
                if (inc != 0) {
                    sLog.debug("-- TOC+" + inc + " A: " + value.variable() + " := " + value);
                    for (Conflict c: allConflicts(assignment, value)) {
                        sLog.debug("  -- " + c);
                        iAllConflicts.add(c);
                        inc -= c.getShare();
                    }
                    if (inc != 0) {
                        sLog.error("Different number of conflicts for the assigned value (difference: " + inc + ")!");
                    }
                }
            }
        }

        /**
         * Called when a value is unassigned from a variable. Internal number of
         * time overlapping conflicts is updated, see
         * {@link TimeOverlapsCounter#getTotalNrConflicts(Assignment)}.
         */
        @Override
        public void unassigned(Assignment<Request, Enrollment> assignment, Enrollment value) {
            StudentSectioningModelContext cx = ((StudentSectioningModel)getModel()).getContext(assignment);
            for (Conflict c: allConflicts(assignment, value)) {
                iTotalNrConflicts -= c.getShare();
                cx.remove(assignment, c);
            }
            if (sDebug) {
                sLog.debug("U:" + value.variable() + " := " + value);
                int dec = nrAllConflicts(assignment, value);
                if (dec != 0) {
                    sLog.debug("-- TOC-" + dec + " U: " + value.variable() + " := " + value);
                    for (Conflict c: allConflicts(assignment, value)) {
                        sLog.debug("  -- " + c);
                        iAllConflicts.remove(c);
                        dec -= c.getShare();
                    }
                    if (dec != 0) {
                        sLog.error("Different number of conflicts for the unassigned value (difference: " + dec + ")!");
                    }
                }
            }
        }
        
        /**
         * Called before a value is assigned to a variable.
         */
        public void beforeAssigned(Assignment<Request, Enrollment> assignment, long iteration, Enrollment value) {
            if (value != null) {
                Enrollment old = assignment.getValue(value.variable());
                if (old != null) {
                    iUnassignedValue = old;
                    unassigned(assignment, old);
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
            if (value != null) {
                assigned(assignment, value);
            }
        }

        /**
         * Called after a value is unassigned from a variable.
         */
        public void afterUnassigned(Assignment<Request, Enrollment> assignment, long iteration, Enrollment value) {
            if (value != null && !value.equals(iUnassignedValue)) {
                unassigned(assignment, value);
            }
        }
        
        /**
         * Return a set of all time overlapping conflicts ({@link Conflict} objects).
         */
        public Set<Conflict> getAllConflicts() {
            return iAllConflicts;
        }
        
        /** Actual number of all time overlapping conflicts */
        public int getTotalNrConflicts() {
            return iTotalNrConflicts;
        }
        
        public void checkTotalNrConflicts(Assignment<Request, Enrollment> assignment) {
            int total = countTotalNrConflicts(assignment);
            if (total != iTotalNrConflicts) {
                sLog.error("Number of conflicts does not match (actual: " + total + ", count: " + iTotalNrConflicts + ")!");
                iTotalNrConflicts = total;
                if (sDebug) {
                    Set<Conflict> conflicts = computeAllConflicts(assignment);
                    for (Conflict c: conflicts) {
                        if (!iAllConflicts.contains(c))
                            sLog.debug("  +add+ " + c);
                    }
                    for (Conflict c: iAllConflicts) {
                        if (!conflicts.contains(c))
                            sLog.debug("  -rem- " + c);
                    }
                    for (Conflict c: conflicts) {
                        for (Conflict d: iAllConflicts) {
                            if (c.equals(d) && c.getShare() != d.getShare()) {
                                sLog.debug("  -dif- " + c + " (other: " + d.getShare() + ")");
                            }
                        }
                    }                
                    iAllConflicts = conflicts;
                    // getSolver().stopSolver(false);
                }
            }
        }
        
        /**
         * Compute the actual number of all time overlapping conflicts. Should be equal to
         * {@link TimeOverlapsCounter#getTotalNrConflicts(Assignment)}.
         */
        public int countTotalNrConflicts(Assignment<Request, Enrollment> assignment) {
            int total = 0;
            for (Request r1 : getModel().variables()) {
                Enrollment e1 = assignment.getValue(r1);
                if (e1 == null || r1 instanceof FreeTimeRequest || r1.equals(iOldVariable))
                    continue;
                for (Request r2 : r1.getStudent().getRequests()) {
                    Enrollment e2 = assignment.getValue(r2);
                    if (r2 instanceof FreeTimeRequest) {
                        FreeTimeRequest ft = (FreeTimeRequest)r2;
                        total += nrConflicts(e1, ft.createEnrollment());
                    } else if (e2 != null && r1.getId() < r2.getId() && !r2.equals(iOldVariable)) {
                        total += nrConflicts(e1, e2);
                    }
                }
            }
            return total;
        }

        /**
         * Compute a set of all time overlapping conflicts ({@link Conflict} objects).
         */
        public Set<Conflict> computeAllConflicts(Assignment<Request, Enrollment> assignment) {
            Set<Conflict> ret = new HashSet<Conflict>();
            for (Request r1 : getModel().variables()) {
                Enrollment e1 = assignment.getValue(r1);
                if (e1 == null || r1 instanceof FreeTimeRequest || r1.equals(iOldVariable))
                    continue;
                for (Request r2 : r1.getStudent().getRequests()) {
                    Enrollment e2 = assignment.getValue(r2);
                    if (r2 instanceof FreeTimeRequest) {
                        FreeTimeRequest ft = (FreeTimeRequest)r2;
                        ret.addAll(conflicts(e1, ft.createEnrollment()));
                    } else if (e2 != null && r1.getId() < r2.getId() && !r2.equals(iOldVariable)) {
                        ret.addAll(conflicts(e1, e2));
                    }                    
                }
            }
            return ret;
        }

        /**
         * The set of all conflicts ({@link Conflict} objects) of the given
         * enrollment and other enrollments that are assigned to the same student.
         */
        public Set<Conflict> allConflicts(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
            Set<Conflict> ret = new HashSet<Conflict>();
            if (enrollment.getRequest() instanceof FreeTimeRequest) return ret;
            for (Request request : enrollment.getStudent().getRequests()) {
                if (request.equals(enrollment.getRequest())) continue;
                if (request instanceof FreeTimeRequest) {
                    FreeTimeRequest ft = (FreeTimeRequest)request;
                    ret.addAll(conflicts(enrollment, ft.createEnrollment()));
                    continue;
                } else if (assignment.getValue(request) != null && !request.equals(iOldVariable)) {
                    ret.addAll(conflicts(enrollment, assignment.getValue(request)));
                }
            }
            return ret;
        }
        
        /**
         * Total sum of all conflict of the given enrollment and other enrollments
         * that are assigned to the same student.
         */
        public int nrAllConflicts(Assignment<Request, Enrollment> assignment, Enrollment enrollment) {
            if (enrollment.getRequest() instanceof FreeTimeRequest) return 0;
            int cnt = 0;
            for (Request request : enrollment.getStudent().getRequests()) {
                if (request.equals(enrollment.getRequest())) continue;
                if (request instanceof FreeTimeRequest) {
                    FreeTimeRequest ft = (FreeTimeRequest)request;
                    cnt += nrConflicts(enrollment, ft.createEnrollment());
                } else if (assignment.getValue(request) != null && !request.equals(iOldVariable)) {
                    cnt += nrConflicts(enrollment, assignment.getValue(request));
                }
            }
            return cnt;
        }
    }

    @Override
    public TimeOverlapsCounterContext createAssignmentContext(Assignment<Request, Enrollment> assignment) {
        return new TimeOverlapsCounterContext(assignment);
    }
}
