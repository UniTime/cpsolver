package org.cpsolver.studentsct.heuristics.selection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;


import org.apache.log4j.Logger;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.JProf;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.heuristics.studentord.StudentChoiceRealFirstOrder;
import org.cpsolver.studentsct.heuristics.studentord.StudentOrder;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Student;

/**
 * Pick a student (one by one) with an incomplete schedule, try to find an
 * improvement, identify problematic students. <br>
 * <br>
 * For each request that does not have an assignment and that can be assined
 * (see {@link Student#canAssign(Assignment, Request)}) a randomly selected sub-domain is
 * visited. For every such enrollment, a set of conflicting enrollments is
 * computed and a possible student that can get an alternative enrollment is
 * identified (if there is any). For each such move a value (the cost of moving
 * the problematic student somewhere else) is computed and the best possible
 * move is selected at the end. If there is no such move, a set of problematic
 * students is identified, i.e., the students whose enrollments are preventing
 * this student to get a request. <br>
 * <br>
 * Each student can be selected for this swap move multiple times, but only if
 * there is still a request that can be resolved. At the end (when there is no
 * other neighbour), the set of all such problematic students can be returned
 * using the {@link ProblemStudentsProvider} interface. <br>
 * <br>
 * Parameters: <br>
 * <table border='1' summary='Related Solver Parameters'>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Comment</th>
 * </tr>
 * <tr>
 * <td>Neighbour.SwapStudentsTimeout</td>
 * <td>{@link Integer}</td>
 * <td>Timeout for each neighbour selection (in milliseconds).</td>
 * </tr>
 * <tr>
 * <td>Neighbour.SwapStudentsMaxValues</td>
 * <td>{@link Integer}</td>
 * <td>Limit for the number of considered values for each course request (see
 * {@link CourseRequest#computeRandomEnrollments(Assignment, int)}).</td>
 * </tr>
 * </table>
 * <br>
 * <br>
 * 
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2014 Tomas Muller<br>
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

public class SwapStudentSelection implements NeighbourSelection<Request, Enrollment>, ProblemStudentsProvider {
    private static Logger sLog = Logger.getLogger(SwapStudentSelection.class);
    private Set<Student> iProblemStudents = Collections.synchronizedSet(new HashSet<Student>());
    private Queue<Student> iStudents = null;
    private int iTimeout = 5000;
    private int iMaxValues = 100;
    public static boolean sDebug = false;
    protected StudentOrder iOrder = new StudentChoiceRealFirstOrder();

    /**
     * Constructor
     * 
     * @param properties
     *            configuration
     */
    public SwapStudentSelection(DataProperties properties) {
        iTimeout = properties.getPropertyInt("Neighbour.SwapStudentsTimeout", iTimeout);
        iMaxValues = properties.getPropertyInt("Neighbour.SwapStudentsMaxValues", iMaxValues);
        if (properties.getProperty("Neighbour.SwapStudentsOrder") != null) {
            try {
                iOrder = (StudentOrder) Class.forName(properties.getProperty("Neighbour.SwapStudentsOrder"))
                        .getConstructor(new Class[] { DataProperties.class }).newInstance(new Object[] { properties });
            } catch (Exception e) {
                sLog.error("Unable to set student order, reason:" + e.getMessage(), e);
            }
        }
    }

    /** Initialization */
    @Override
    public void init(Solver<Request, Enrollment> solver) {
        List<Student> students = iOrder.order(((StudentSectioningModel) solver.currentSolution().getModel()).getStudents());
        iStudents = new LinkedList<Student>(students);
        iProblemStudents.clear();
        Progress.getInstance(solver.currentSolution().getModel()).setPhase("Student swap...", students.size());
    }
    
    protected synchronized Student nextStudent() {
        return iStudents.poll();
    }
    
    protected synchronized void addStudent(Student student) {
        iStudents.add(student);
    }

    /**
     * For each student that does not have a complete schedule, try to find a
     * request and a student that can be moved out of an enrollment so that the
     * selected student can be assigned to the selected request.
     */
    @Override
    public Neighbour<Request, Enrollment> selectNeighbour(Solution<Request, Enrollment> solution) {
        Student student = null;
        while ((student = nextStudent()) != null) {
            Progress.getInstance(solution.getModel()).incProgress();
            if (student.isComplete(solution.getAssignment()) || student.nrAssignedRequests(solution.getAssignment()) == 0)
                continue;
            Selection selection = getSelection(solution.getAssignment(), student);
            Neighbour<Request, Enrollment> neighbour = selection.select();
            if (neighbour != null) {
                addStudent(student);
                return neighbour;
            } else
                iProblemStudents.addAll(selection.getProblemStudents());
        }
        return null;
    }

    /** List of problematic students */
    @Override
    public Set<Student> getProblemStudents() {
        return iProblemStudents;
    }

    /** Selection subclass for a student */
    public Selection getSelection(Assignment<Request, Enrollment> assignment, Student student) {
        return new Selection(student, assignment);
    }

    /** This class looks for a possible swap move for the given student */
    public class Selection {
        private Student iStudent;
        private long iT0, iT1;
        private boolean iTimeoutReached;
        private Enrollment iBestEnrollment;
        private double iBestValue;
        private Set<Student> iProblemStudents;
        private List<Enrollment> iBestSwaps;
        private Assignment<Request, Enrollment> iAssignment;

        /**
         * Constructor
         * 
         * @param student
         *            given student
         */
        public Selection(Student student, Assignment<Request, Enrollment> assignment) {
            iStudent = student;
            iAssignment = assignment;
        }

        /**
         * The actual selection
         */
        public SwapStudentNeighbour select() {
            if (sDebug)
                sLog.debug("select(S" + iStudent.getId() + ")");
            iT0 = JProf.currentTimeMillis();
            iTimeoutReached = false;
            iBestEnrollment = null;
            iProblemStudents = new HashSet<Student>();
            Double initialValue = null;
            for (Request request : iStudent.getRequests()) {
                if (initialValue == null)
                    initialValue = request.getModel().getTotalValue(iAssignment);
                if (iTimeout > 0 && (JProf.currentTimeMillis() - iT0) > iTimeout) {
                    if (!iTimeoutReached) {
                        if (sDebug)
                            sLog.debug("  -- timeout reached");
                        iTimeoutReached = true;
                    }
                    break;
                }
                if (iAssignment.getValue(request) != null)
                    continue;
                if (!iStudent.canAssign(iAssignment, request))
                    continue;
                if (sDebug)
                    sLog.debug("  -- checking request " + request);
                List<Enrollment> values = null;
                if (iMaxValues > 0 && request instanceof CourseRequest) {
                    values = ((CourseRequest) request).computeRandomEnrollments(iAssignment, iMaxValues);
                } else
                    values = request.values();
                for (Enrollment enrollment : values) {
                    if (iTimeout > 0 && (JProf.currentTimeMillis() - iT0) > iTimeout) {
                        if (!iTimeoutReached) {
                            if (sDebug)
                                sLog.debug("  -- timeout reached");
                            iTimeoutReached = true;
                        }
                        break;
                    }
                    if (sDebug)
                        sLog.debug("      -- enrollment " + enrollment);
                    Set<Enrollment> conflicts = enrollment.variable().getModel().conflictValues(iAssignment, enrollment);
                    if (conflicts.contains(enrollment))
                        continue;
                    
                    double bound = enrollment.toDouble(iAssignment);
                    for (Enrollment conflict: conflicts)
                        bound += conflict.variable().getBound();
                    if (iBestEnrollment != null && bound >= iBestValue)
                        continue;
                    
                    for (Enrollment conflict: conflicts)
                        iAssignment.unassign(0, conflict.variable());
                    iAssignment.assign(0, enrollment);
                    
                    boolean allResolved = true;
                    List<Enrollment> swaps = new ArrayList<Enrollment>(conflicts.size());
                    for (Enrollment conflict : conflicts) {
                        if (sDebug)
                            sLog.debug("        -- conflict " + conflict);
                        Enrollment other = bestSwap(iAssignment, conflict, enrollment, iProblemStudents);
                        if (other == null) {
                            if (sDebug)
                                sLog.debug("          -- unable to resolve");
                            allResolved = false;
                            break;
                        }
                        iAssignment.assign(0, other);
                        swaps.add(other);
                        if (sDebug)
                            sLog.debug("          -- can be resolved by switching to " + other.getName());
                    }
                    double value = request.getModel().getTotalValue(iAssignment) - initialValue;
                    
                    for (Enrollment other: swaps)
                        iAssignment.unassign(0, other.variable());
                    iAssignment.unassign(0, enrollment.variable());
                    for (Enrollment conflict: conflicts)
                        iAssignment.assign(0, conflict);
                    
                    if (allResolved && value <= 0.0 && (iBestEnrollment == null || iBestValue > value)) {
                        iBestEnrollment = enrollment;
                        iBestValue = value;
                        iBestSwaps = swaps;
                    }
                }
            }
            iT1 = JProf.currentTimeMillis();
            if (sDebug)
                sLog.debug("  -- done, best enrollment is " + iBestEnrollment);
            if (iBestEnrollment == null) {
                if (iProblemStudents.isEmpty())
                    iProblemStudents.add(iStudent);
                if (sDebug)
                    sLog.debug("  -- problem students are: " + iProblemStudents);
                return null;
            }
            if (sDebug)
                sLog.debug("  -- value " + iBestValue);
            Enrollment[] assignment = new Enrollment[iStudent.getRequests().size()];
            int idx = 0;
            for (Request request : iStudent.getRequests()) {
                assignment[idx++] = (iBestEnrollment.getRequest().equals(request) ? iBestEnrollment
                        : (Enrollment) request.getAssignment(iAssignment));
            }
            return new SwapStudentNeighbour(iBestValue, iBestEnrollment, iBestSwaps);
        }

        /** Was timeout reached during the selection */
        public boolean isTimeoutReached() {
            return iTimeoutReached;
        }

        /** Time spent in the last selection */
        public long getTime() {
            return iT1 - iT0;
        }

        /** The best enrollment found. */
        public Enrollment getBestEnrollment() {
            return iBestEnrollment;
        }

        /** Cost of the best enrollment found */
        public double getBestValue() {
            return iBestValue;
        }

        /** Set of problematic students computed in the last selection */
        public Set<Student> getProblemStudents() {
            return iProblemStudents;
        }

    }

    /**
     * Identify the best swap for the given student
     * 
     * @param conflict
     *            conflicting enrollment
     * @param enrl
     *            enrollment that is visited (to be assigned to the given
     *            student)
     * @param problematicStudents
     *            the current set of problematic students
     * @return best alternative enrollment for the student of the conflicting
     *         enrollment
     */
    public static Enrollment bestSwap(Assignment<Request, Enrollment> assignment, Enrollment conflict, Enrollment enrl, Set<Student> problematicStudents) {
        Enrollment bestEnrollment = null;
        double bestValue = 0;
        for (Enrollment enrollment : conflict.getRequest().values()) {
            if (conflict.variable().getModel().inConflict(assignment, enrollment))
                continue;
            double value = enrollment.toDouble(assignment);
            if (bestEnrollment == null || bestValue > value) {
                bestEnrollment = enrollment;
                bestValue = value;
            }
        }
        if (bestEnrollment == null && problematicStudents != null) {
            boolean added = false;
            for (Enrollment enrollment : conflict.getRequest().values()) {
                Set<Enrollment> conflicts = conflict.variable().getModel().conflictValues(assignment, enrollment);
                for (Enrollment c : conflicts) {
                    if (enrl.getStudent().isDummy() && !c.getStudent().isDummy())
                        continue;
                    if (enrl.getStudent().equals(c.getStudent()) || conflict.getStudent().equals(c.getStudent()))
                        continue;
                    problematicStudents.add(c.getStudent());
                }
            }
            if (!added && !enrl.getStudent().equals(conflict.getStudent()))
                problematicStudents.add(conflict.getStudent());
        }
        return bestEnrollment;
    }

    /** Neighbour that contains the swap */
    public static class SwapStudentNeighbour implements Neighbour<Request, Enrollment> {
        private double iValue;
        private Enrollment iEnrollment;
        private List<Enrollment> iSwaps;

        /**
         * Constructor
         * 
         * @param value
         *            cost of the move
         * @param enrollment
         *            the enrollment which is to be assigned to the given
         *            student
         * @param swaps
         *            enrollment swaps
         */
        public SwapStudentNeighbour(double value, Enrollment enrollment, List<Enrollment> swaps) {
            iValue = value;
            iEnrollment = enrollment;
            iSwaps = swaps;
        }

        @Override
        public double value(Assignment<Request, Enrollment> assignment) {
            return iValue;
        }

        /**
         * Perform the move. All the requeired swaps are identified and
         * performed as well.
         **/
        @Override
        public void assign(Assignment<Request, Enrollment> assignment, long iteration) {
            assignment.unassign(iteration, iEnrollment.variable());
            for (Enrollment swap : iSwaps) {
                assignment.unassign(iteration, swap.variable());
            }
            assignment.assign(iteration, iEnrollment);
            for (Enrollment swap : iSwaps) {
                assignment.assign(iteration, swap);
            }
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer("SwSt{");
            sb.append(" " + iEnrollment.getRequest().getStudent());
            sb.append(" (" + iValue + ")");
            sb.append("\n " + iEnrollment.getRequest());
            sb.append(" " + iEnrollment);
            for (Enrollment swap : iSwaps) {
                sb.append("\n " + swap.getRequest());
                sb.append(" -> " + swap);
            }
            sb.append("\n}");
            return sb.toString();
        }

        @Override
        public Map<Request, Enrollment> assignments() {
            Map<Request, Enrollment> ret = new HashMap<Request, Enrollment>();
            ret.put(iEnrollment.variable(), iEnrollment);
            for (Enrollment swap : iSwaps)
                ret.put(swap.variable(), swap);
            return ret;
        }
    }
}
