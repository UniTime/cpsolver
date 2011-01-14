package net.sf.cpsolver.studentsct.heuristics.selection;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.ifs.extension.Extension;
import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.JProf;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.extension.DistanceConflict;
import net.sf.cpsolver.studentsct.extension.TimeOverlapsCounter;
import net.sf.cpsolver.studentsct.heuristics.studentord.StudentChoiceRealFirstOrder;
import net.sf.cpsolver.studentsct.heuristics.studentord.StudentOrder;
import net.sf.cpsolver.studentsct.model.CourseRequest;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.FreeTimeRequest;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

import org.apache.log4j.Logger;

/**
 * Section all students using incremental branch & bound (no unassignments). All
 * students are taken in a random order, for each student a branch & bound
 * algorithm is used to find his/her best schedule on top of all other existing
 * student schedules (no enrollment of a different student is unassigned).
 * 
 * <br>
 * <br>
 * Parameters: <br>
 * <table border='1'>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>Neighbour.BranchAndBoundTimeout</td>
 * <td>{@link Integer}</td>
 * <td>Timeout for each neighbour selection (in milliseconds).</td>
 * </tr>
 * <tr>
 * <td>Neighbour.BranchAndBoundMinimizePenalty</td>
 * <td>{@link Boolean}</td>
 * <td>If true, section penalties (instead of section values) are minimized:
 * overall penalty is minimized together with the maximization of the number of
 * assigned requests and minimization of distance conflicts -- this variant is
 * to better mimic the case when students can choose their sections (section
 * times).</td>
 * </tr>
 * </table>
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

public class BranchBoundSelection implements NeighbourSelection<Request, Enrollment> {
    private static Logger sLog = Logger.getLogger(BranchBoundSelection.class);
    private static DecimalFormat sDF = new DecimalFormat("0.00");
    protected int iTimeout = 10000;
    protected DistanceConflict iDistanceConflict = null;
    protected TimeOverlapsCounter iTimeOverlaps = null;
    protected StudentSectioningModel iModel = null;
    public static boolean sDebug = false;
    protected Iterator<Student> iStudentsEnumeration = null;
    protected boolean iMinimizePenalty = false;
    protected StudentOrder iOrder = new StudentChoiceRealFirstOrder();
    protected double iDistConfWeight = 1.0;

    /**
     * Constructor
     * 
     * @param properties
     *            configuration
     */
    public BranchBoundSelection(DataProperties properties) {
        iTimeout = properties.getPropertyInt("Neighbour.BranchAndBoundTimeout", iTimeout);
        iMinimizePenalty = properties.getPropertyBoolean("Neighbour.BranchAndBoundMinimizePenalty", iMinimizePenalty);
        if (iMinimizePenalty)
            sLog.info("Overall penalty is going to be minimized (together with the maximization of the number of assigned requests and minimization of distance conflicts).");
        if (properties.getProperty("Neighbour.BranchAndBoundOrder") != null) {
            try {
                iOrder = (StudentOrder) Class.forName(properties.getProperty("Neighbour.BranchAndBoundOrder"))
                        .getConstructor(new Class[] { DataProperties.class }).newInstance(new Object[] { properties });
            } catch (Exception e) {
                sLog.error("Unable to set student order, reason:" + e.getMessage(), e);
            }
        }
        iDistConfWeight = properties.getPropertyDouble("DistanceConflict.Weight", iDistConfWeight);
    }

    /**
     * Initialize
     */
    public void init(Solver<Request, Enrollment> solver, String name) {
        List<Student> students = iOrder.order(((StudentSectioningModel) solver.currentSolution().getModel())
                .getStudents());
        iStudentsEnumeration = students.iterator();
        if (iDistanceConflict == null)
            for (Extension<Request, Enrollment> ext : solver.getExtensions()) {
                if (ext instanceof DistanceConflict)
                    iDistanceConflict = (DistanceConflict) ext;
            }
        if (iTimeOverlaps == null)
            for (Extension<Request, Enrollment> ext : solver.getExtensions()) {
                if (ext instanceof TimeOverlapsCounter)
                    iTimeOverlaps = (TimeOverlapsCounter) ext;
            }
        iModel = (StudentSectioningModel)solver.currentSolution().getModel();
        Progress.getInstance(solver.currentSolution().getModel()).setPhase(name, students.size());
    }

    public void init(Solver<Request, Enrollment> solver) {
        init(solver, "Branch&bound...");
    }

    /**
     * Select neighbour. All students are taken, one by one in a random order.
     * For each student a branch & bound search is employed.
     */
    public Neighbour<Request, Enrollment> selectNeighbour(Solution<Request, Enrollment> solution) {
        while (iStudentsEnumeration.hasNext()) {
            Student student = iStudentsEnumeration.next();
            Progress.getInstance(solution.getModel()).incProgress();
            Neighbour<Request, Enrollment> neighbour = getSelection(student).select();
            if (neighbour != null)
                return neighbour;
        }
        return null;
    }

    /**
     * Branch & bound selection for a student
     */
    public Selection getSelection(Student student) {
        return new Selection(student);
    }

    /**
     * Branch & bound selection for a student
     */
    public class Selection {
        /** Student */
        protected Student iStudent;
        /** Start time */
        protected long iT0;
        /** End time */
        protected long iT1;
        /** Was timeout reached */
        protected boolean iTimeoutReached;
        /** Current assignment */
        protected Enrollment[] iAssignment;
        /** Best assignment */
        protected Enrollment[] iBestAssignment;
        /** Best value */
        protected double iBestValue;
        /** Value cache */
        protected HashMap<CourseRequest, List<Enrollment>> iValues;

        /**
         * Constructor
         * 
         * @param student
         *            selected student
         */
        public Selection(Student student) {
            iStudent = student;
        }

        /**
         * Execute branch & bound, return the best found schedule for the
         * selected student.
         */
        public BranchBoundNeighbour select() {
            iT0 = JProf.currentTimeMillis();
            iTimeoutReached = false;
            iAssignment = new Enrollment[iStudent.getRequests().size()];
            iBestAssignment = null;
            iBestValue = 0;
            
            int i = 0;
            for (Request r: iStudent.getRequests())
                iAssignment[i++] = r.getAssignment();
            saveBest();
            for (int j = 0; j < iAssignment.length; j++)
                iAssignment[j] = null;
            
            
            iValues = new HashMap<CourseRequest, List<Enrollment>>();
            backTrack(0);
            iT1 = JProf.currentTimeMillis();
            if (iBestAssignment == null)
                return null;
            return new BranchBoundNeighbour(iStudent, iBestValue, iBestAssignment);
        }

        /** Was timeout reached */
        public boolean isTimeoutReached() {
            return iTimeoutReached;
        }

        /** Time (in milliseconds) the branch & bound did run */
        public long getTime() {
            return iT1 - iT0;
        }

        /** Best schedule */
        public Enrollment[] getBestAssignment() {
            return iBestAssignment;
        }

        /** Value of the best schedule */
        public double getBestValue() {
            return iBestValue;
        }

        /** Number of requests assigned in the best schedule */
        public int getBestNrAssigned() {
            int nrAssigned = 0;
            for (int i = 0; i < iBestAssignment.length; i++)
                if (iBestAssignment[i] != null)
                    nrAssigned += (iBestAssignment[i].isCourseRequest() ? 10 : 1);
            return nrAssigned;
        }

        /** Bound for the number of assigned requests in the current schedule */
        public int getNrAssignedBound(int idx) {
            int bound = 0;
            int i = 0, alt = 0;
            for (Iterator<Request> e = iStudent.getRequests().iterator(); e.hasNext(); i++) {
                Request r = e.next();
                boolean cr = r instanceof CourseRequest;
                if (i < idx) {
                    if (iAssignment[i] != null)
                        bound += (cr ? 10 : 1);
                    if (r.isAlternative()) {
                        if (iAssignment[i] != null || (cr && ((CourseRequest) r).isWaitlist()))
                            alt--;
                    } else {
                        if (cr && !((CourseRequest) r).isWaitlist() && iAssignment[i] == null)
                            alt++;
                    }
                } else {
                    if (!r.isAlternative())
                        bound += (cr ? 10 : 1);
                    else if (alt > 0) {
                        bound += (cr ? 10 : 1);
                        alt--;
                    }
                }
            }
            return bound;
        }
        
        /** Use {@link Selection#getDistanceConflicts} instead */
        @Deprecated
        public int getNrDistanceConflicts(int idx) {
            return getDistanceConflicts(idx).size();
        }

        /**
         * Distance conflicts of idx-th assignment of the current
         * schedule
         */
        public Set<DistanceConflict.Conflict> getDistanceConflicts(int idx) {
            if (iDistanceConflict == null || iAssignment[idx] == null)
                return null;
            Set<DistanceConflict.Conflict> dist = iDistanceConflict.conflicts(iAssignment[idx]);
            for (int x = 0; x < idx; x++)
                if (iAssignment[x] != null)
                    dist.addAll(iDistanceConflict.conflicts(iAssignment[x], iAssignment[idx]));
            return dist;
        }
        
        /**
         * Time overlapping conflicts of idx-th assignment of the current
         * schedule
         */
        public Set<TimeOverlapsCounter.Conflict> getTimeOverlappingConflicts(int idx) {
            if (iTimeOverlaps == null || iAssignment[idx] == null)
                return null;
            Set<TimeOverlapsCounter.Conflict> overlaps = new HashSet<TimeOverlapsCounter.Conflict>();
            for (int x = 0; x < idx; x++)
                if (iAssignment[x] != null)
                    overlaps.addAll(iTimeOverlaps.conflicts(iAssignment[x], iAssignment[idx]));
                else if (iStudent.getRequests().get(x) instanceof FreeTimeRequest)
                    overlaps.addAll(iTimeOverlaps.conflicts(((FreeTimeRequest)iStudent.getRequests().get(x)).createEnrollment(), iAssignment[idx]));
            return overlaps;
        }
        
        /**
         * Weight of an assignment. Unlike {@link StudentSectioningModel#getWeight(Enrollment, Set, Set)}, only count this side of distance conflicts and time overlaps.
         **/
        protected double getWeight(Enrollment enrollment, Set<DistanceConflict.Conflict> distanceConflicts, Set<TimeOverlapsCounter.Conflict> timeOverlappingConflicts) {
            double weight = - iModel.getStudentWeights().getWeight(enrollment);
            if (distanceConflicts != null)
                for (DistanceConflict.Conflict c: distanceConflicts) {
                    Enrollment other = (c.getE1().equals(enrollment) ? c.getE2() : c.getE1());
                    if (other.getRequest().getPriority() <= enrollment.getRequest().getPriority())
                        weight += iModel.getStudentWeights().getDistanceConflictWeight(c);
                }
            if (timeOverlappingConflicts != null)
                for (TimeOverlapsCounter.Conflict c: timeOverlappingConflicts) {
                    weight += iModel.getStudentWeights().getTimeOverlapConflictWeight(enrollment, c);
                }
            return weight;
        }
        
        /** Return bound of a request */
        protected double getBound(Request r) {
            return r.getBound();
        }

        /** Bound for the current schedule */
        public double getBound(int idx) {
            double bound = 0.0;
            int i = 0, alt = 0;
            for (Iterator<Request> e = iStudent.getRequests().iterator(); e.hasNext(); i++) {
                Request r = e.next();
                if (i < idx) {
                    if (iAssignment[i] != null)
                        bound += getWeight(iAssignment[i], getDistanceConflicts(i), getTimeOverlappingConflicts(i));
                    if (r.isAlternative()) {
                        if (iAssignment[i] != null || (r instanceof CourseRequest && ((CourseRequest) r).isWaitlist()))
                            alt--;
                    } else {
                        if (r instanceof CourseRequest && !((CourseRequest) r).isWaitlist() && iAssignment[i] == null)
                            alt++;
                    }
                } else {
                    if (!r.isAlternative())
                        bound += getBound(r);
                    else if (alt > 0) {
                        bound += getBound(r);
                        alt--;
                    }
                }
            }
            return bound;
        }

        /** Value of the current schedule */
        public double getValue() {
            double value = 0.0;
            for (int i = 0; i < iAssignment.length; i++)
                if (iAssignment[i] != null)
                    value += getWeight(iAssignment[i], getDistanceConflicts(i), getTimeOverlappingConflicts(i));
            return value;
        }

        /** Assignment penalty */
        protected double getAssignmentPenalty(int i) {
            return iAssignment[i].getPenalty() + iDistConfWeight * getDistanceConflicts(i).size();
        }

        /** Penalty of the current schedule */
        public double getPenalty() {
            double bestPenalty = 0;
            for (int i = 0; i < iAssignment.length; i++)
                if (iAssignment[i] != null)
                    bestPenalty += getAssignmentPenalty(i);
            return bestPenalty;
        }

        /** Penalty bound of the current schedule */
        public double getPenaltyBound(int idx) {
            double bound = 0.0;
            int i = 0, alt = 0;
            for (Iterator<Request> e = iStudent.getRequests().iterator(); e.hasNext(); i++) {
                Request r = e.next();
                if (i < idx) {
                    if (iAssignment[i] != null)
                        bound += getAssignmentPenalty(i);
                    if (r.isAlternative()) {
                        if (iAssignment[i] != null || (r instanceof CourseRequest && ((CourseRequest) r).isWaitlist()))
                            alt--;
                    } else {
                        if (r instanceof CourseRequest && !((CourseRequest) r).isWaitlist() && iAssignment[i] == null)
                            alt++;
                    }
                } else {
                    if (!r.isAlternative()) {
                        if (r instanceof CourseRequest)
                            bound += ((CourseRequest) r).getMinPenalty();
                    } else if (alt > 0) {
                        if (r instanceof CourseRequest)
                            bound += ((CourseRequest) r).getMinPenalty();
                        alt--;
                    }
                }
            }
            return bound;
        }

        /** Save the current schedule as the best */
        public void saveBest() {
            if (iBestAssignment == null)
                iBestAssignment = new Enrollment[iAssignment.length];
            for (int i = 0; i < iAssignment.length; i++)
                iBestAssignment[i] = iAssignment[i];
            if (iMinimizePenalty)
                iBestValue = getPenalty();
            else
                iBestValue = getValue();
        }

        /** First conflicting enrollment */
        public Enrollment firstConflict(int idx, Enrollment enrollment) {
            Set<Enrollment> conflicts = enrollment.variable().getModel().conflictValues(enrollment);
            if (conflicts.contains(enrollment))
                return enrollment;
            if (!conflicts.isEmpty()) {
                for (Enrollment conflict : conflicts) {
                    if (!conflict.getStudent().equals(iStudent))
                        return conflict;
                }
            }
            for (int i = 0; i < iAssignment.length; i++) {
                if (iAssignment[i] == null)
                    continue;
                if (!iAssignment[i].isConsistent(enrollment))
                    return iAssignment[i];
            }
            return null;
        }

        /** True if the given request can be assigned */
        public boolean canAssign(Request request, int idx) {
            if (!request.isAlternative() || iAssignment[idx] != null)
                return true;
            int alt = 0;
            int i = 0;
            for (Iterator<Request> e = iStudent.getRequests().iterator(); e.hasNext(); i++) {
                Request r = e.next();
                if (r.equals(request))
                    continue;
                if (r.isAlternative()) {
                    if (iAssignment[i] != null || (r instanceof CourseRequest && ((CourseRequest) r).isWaitlist()))
                        alt--;
                } else {
                    if (r instanceof CourseRequest && !((CourseRequest) r).isWaitlist() && iAssignment[i] == null)
                        alt++;
                }
            }
            return (alt > 0);
        }

        /** Number of assigned requests in the current schedule */
        public int getNrAssigned() {
            int assigned = 0;
            for (int i = 0; i < iAssignment.length; i++)
                if (iAssignment[i] != null)
                    assigned += (iAssignment[i].isCourseRequest() ? 10 : 1);
            return assigned;
        }

        /** Returns true if the given request can be left unassigned */
        protected boolean canLeaveUnassigned(Request request) {
            return true;
        }
        
        /** Returns list of available enrollments for a course request */
        protected List<Enrollment> values(final CourseRequest request) {
            List<Enrollment> values = request.getAvaiableEnrollments();
            Collections.sort(values, new Comparator<Enrollment>() {
                
                private HashMap<Enrollment, Double> iValues = new HashMap<Enrollment, Double>();
                
                private Double value(Enrollment e) {
                    Double value = iValues.get(e);
                    if (value == null) {
                        value = iModel.getStudentWeights().getWeight(e,
                                        (iModel.getDistanceConflict() == null ? null : iModel.getDistanceConflict().conflicts(e)),
                                        (iModel.getTimeOverlaps() == null ? null : iModel.getTimeOverlaps().freeTimeConflicts(e)));
                        iValues.put(e, value);       
                    }
                    return value;
                }
                
                public int compare(Enrollment e1, Enrollment e2) {
                    if (e1.equals(request.getAssignment())) return -1;
                    if (e2.equals(request.getAssignment())) return -1;
                    return value(e2).compareTo(value(e1));
                }
                
            });
            return values;
        }

        /** branch & bound search */
        public void backTrack(int idx) {
            if (sDebug)
                sLog.debug("backTrack(" + getNrAssigned() + "/" + getValue() + "," + idx + ")");
            if (iTimeout > 0 && (JProf.currentTimeMillis() - iT0) > iTimeout) {
                if (sDebug)
                    sLog.debug("  -- timeout reached");
                iTimeoutReached = true;
                return;
            }
            if (iMinimizePenalty) {
                if (getBestAssignment() != null
                        && (getNrAssignedBound(idx) < getBestNrAssigned() || (getNrAssignedBound(idx) == getBestNrAssigned() && getPenaltyBound(idx) >= getBestValue()))) {
                    if (sDebug)
                        sLog.debug("  -- branch number of assigned " + getNrAssignedBound(idx) + "<"
                                + getBestNrAssigned() + ", or penalty " + getPenaltyBound(idx) + ">=" + getBestValue());
                    return;
                }
                if (idx == iAssignment.length) {
                    if (getBestAssignment() == null
                            || (getNrAssigned() > getBestNrAssigned() || (getNrAssigned() == getBestNrAssigned() && getPenalty() < getBestValue()))) {
                        if (sDebug)
                            sLog.debug("  -- best solution found " + getNrAssigned() + "/" + getPenalty());
                        saveBest();
                    }
                    return;
                }
            } else {
                if (getBestAssignment() != null && getBound(idx) >= getBestValue()) {
                    if (sDebug)
                        sLog.debug("  -- branch " + getBound(idx) + " >= " + getBestValue());
                    return;
                }
                if (idx == iAssignment.length) {
                    if (getBestAssignment() == null || getValue() < getBestValue()) {
                        if (sDebug)
                            sLog.debug("  -- best solution found " + getNrAssigned() + "/" + getValue());
                        saveBest();
                    }
                    return;
                }
            }

            Request request = iStudent.getRequests().get(idx);
            if (sDebug)
                sLog.debug("  -- request: " + request);
            if (!canAssign(request, idx)) {
                if (sDebug)
                    sLog.debug("    -- cannot assign");
                backTrack(idx + 1);
                return;
            }
            List<Enrollment> values = null;
            if (request instanceof CourseRequest) {
                CourseRequest courseRequest = (CourseRequest) request;
                if (!courseRequest.getSelectedChoices().isEmpty()) {
                    if (sDebug)
                        sLog.debug("    -- selection among selected enrollments");
                    values = courseRequest.getSelectedEnrollments(true);
                    if (values != null && !values.isEmpty()) {
                        boolean hasNoConflictValue = false;
                        for (Enrollment enrollment : values) {
                            if (firstConflict(idx, enrollment) != null)
                                continue;
                            hasNoConflictValue = true;
                            if (sDebug)
                                sLog.debug("      -- nonconflicting enrollment found: " + enrollment);
                            iAssignment[idx] = enrollment;
                            backTrack(idx + 1);
                            iAssignment[idx] = null;
                        }
                        if (hasNoConflictValue)
                            return;
                    }
                }
                values = iValues.get(courseRequest);
                if (values == null) {
                    values = values(courseRequest);
                    iValues.put(courseRequest, values);
                }
            } else {
                values = request.computeEnrollments();
            }
            if (sDebug) {
                sLog.debug("  -- nrValues: " + values.size());
                int vIdx = 1;
                for (Enrollment enrollment : values) {
                    if (sDebug)
                        sLog.debug("    -- [" + vIdx + "]: " + enrollment);
                }
            }
            boolean hasNoConflictValue = false;
            for (Enrollment enrollment : values) {
                if (sDebug)
                    sLog.debug("    -- enrollment: " + enrollment);
                Enrollment conflict = firstConflict(idx, enrollment);
                if (conflict != null) {
                    if (sDebug)
                        sLog.debug("        -- in conflict with: " + conflict);
                    continue;
                }
                hasNoConflictValue = true;
                iAssignment[idx] = enrollment;
                backTrack(idx + 1);
                iAssignment[idx] = null;
            }
            if (canLeaveUnassigned(request) && (!hasNoConflictValue || request instanceof CourseRequest))
                backTrack(idx + 1);
        }
    }

    /** Branch & bound neighbour -- a schedule of a student */
    public static class BranchBoundNeighbour extends Neighbour<Request, Enrollment> {
        private double iValue;
        private Enrollment[] iAssignment;
        private Student iStudent;

        /**
         * Constructor
         * 
         * @param value
         *            value of the schedule
         * @param assignment
         *            enrollments of student's requests
         */
        public BranchBoundNeighbour(Student student, double value, Enrollment[] assignment) {
            iValue = value;
            iAssignment = assignment;
            iStudent = student;
        }

        @Override
        public double value() {
            return iValue;
        }

        /** Assignment */
        public Enrollment[] getAssignment() {
            return iAssignment;
        }
        
        /** Student */
        public Student getStudent() {
            return iStudent;
        }

        /** Assign the schedule */
        @Override
        public void assign(long iteration) {
            for (Request r: iStudent.getRequests())
                if (r.getAssignment() != null) r.unassign(iteration);
            for (int i = 0; i < iAssignment.length; i++)
                if (iAssignment[i] != null)
                    iAssignment[i].variable().assign(iteration, iAssignment[i]);
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer("B&B{ " + iStudent + " " + sDF.format(-value() * 100.0) + "%");
            int idx = 0;
            for (Iterator<Request> e = iStudent.getRequests().iterator(); e.hasNext(); idx++) {
                Request request = e.next();
                sb.append("\n  " + request);
                Enrollment enrollment = iAssignment[idx];
                if (enrollment == null)
                    sb.append("  -- not assigned");
                else
                    sb.append("  -- " + enrollment);
            }
            sb.append("\n}");
            return sb.toString();
        }

    }
}
