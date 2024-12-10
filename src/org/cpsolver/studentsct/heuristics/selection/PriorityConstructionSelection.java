package org.cpsolver.studentsct.heuristics.selection;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.heuristics.selection.BranchBoundSelection.BranchBoundNeighbour;
import org.cpsolver.studentsct.heuristics.studentord.StudentChoiceRealFirstOrder;
import org.cpsolver.studentsct.heuristics.studentord.StudentOrder;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Student;


/**
 * This selection is very much like {@link BranchBoundSelection}, but it works in cycles
 * (over all the students) assigning only the first N priority courses. It starts with
 * N = 1 and increases it by one after each cycle. The selection ends when no student can 
 * get more requests assigned in a whole cycle. Run the selection only once (at the 
 * beginning), the selection falls back to {@link BranchBoundSelection} if there are already
 * some requests assigned at the time of initialization.
 * 
 * <br>
 * <br>
 * 
 * @author  Tomas Muller
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

public class PriorityConstructionSelection implements NeighbourSelection<Request, Enrollment> {
    private static org.apache.logging.log4j.Logger sLog = org.apache.logging.log4j.LogManager.getLogger(PriorityConstructionSelection.class);
    private static DecimalFormat sDF = new DecimalFormat("0.00");
    private int iCycle = 0;
    private int iMaxCycles = 7;
    private boolean iImproved = false;
    private boolean iSkip = false;
    private BranchBoundSelection iBranchBoundSelection = null;
    protected Iterator<Student> iStudentsEnumeration = null;
    protected StudentOrder iOrder = new StudentChoiceRealFirstOrder();
    protected List<Student> iStudents = null;

    /**
     * Constructor
     * 
     * @param properties
     *            configuration
     */
    public PriorityConstructionSelection(DataProperties properties) {
        iBranchBoundSelection = new BranchBoundSelection(properties);
        if (properties.getProperty("Neighbour.PriorityConstructionOrder") != null) {
            try {
                iOrder = (StudentOrder) Class.forName(properties.getProperty("Neighbour.PriorityConstructionOrder"))
                        .getConstructor(new Class[] { DataProperties.class }).newInstance(new Object[] { properties });
            } catch (Exception e) {
                sLog.error("Unable to set student order, reason:" + e.getMessage(), e);
            }
        }
        iMaxCycles = properties.getPropertyInteger("Neighbour.PriorityConstructionCycles", iMaxCycles);
    }
    
    /**
     * Initialize
     */
    @Override
    public void init(Solver<Request, Enrollment> solver) {
        iCycle = 1;
        iImproved = false;
        iSkip = !solver.currentSolution().getModel().assignedVariables(solver.currentSolution().getAssignment()).isEmpty();
        if (iSkip) {
            iBranchBoundSelection.init(solver);
        } else {
            iStudents = iOrder.order(((StudentSectioningModel) solver.currentSolution().getModel()).getStudents());
            iStudentsEnumeration = iStudents.iterator();
            iBranchBoundSelection.init(solver, "Construction[" + iCycle + "]...");
        }
    }
    
    /**
     * Find best solution for the next student using {@link BranchBoundSelection}.
     * @param solution current selection
     * @return generated neighbour
     */
    public Neighbour<Request, Enrollment> branchAndBound(Solution<Request, Enrollment> solution) {
        while (iStudentsEnumeration.hasNext()) {
            Student student = iStudentsEnumeration.next();
            Progress.getInstance(solution.getModel()).incProgress();
            /*
            if (student.nrRequests() < iCycle) {
                // not enough requests -> nothing to improve -> skip
                continue;
            }
            if (student.nrAssignedRequests() + 1 < iCycle) {
                // previous step cycle already did not improve the assignment -> skip
                continue;
            }
            */
            Neighbour<Request, Enrollment> neighbour = iBranchBoundSelection.getSelection(solution.getAssignment(), student).select();
            if (neighbour != null)
                return neighbour;
        }
        return null;
    }
    
    /** Increment cycle 
     * @param solution current solution
     **/
    protected void nextCycle(Solution<Request, Enrollment> solution) {
        iCycle ++; iImproved = false;
        sLog.debug("Assigning up to " + iCycle + " requests...");
        
        StudentSectioningModel m = (StudentSectioningModel)solution.getModel();
        double tv = m.getTotalValue(solution.getAssignment(), true);
        sLog.debug("**CURR** " + solution.getModel().toString() + ", TM:" + sDF.format(solution.getTime() / 3600.0) + "h, " + 
                "TV:" + sDF.format(-tv) + " (" + sDF.format(-100.0 * tv / m.getStudents().size()) + "%)");
        
        iStudentsEnumeration = iStudents.iterator();
        Progress.getInstance(solution.getModel()).setPhase("Construction[" + iCycle + "]...", iStudents.size());
    }

    /**
     * Select neighbor. All students are taken, one by one in a random order.
     * For each student a branch &amp; bound search is employed.
     */
    @Override
    public Neighbour<Request, Enrollment> selectNeighbour(Solution<Request, Enrollment> solution) {
        if (iSkip)
            return iBranchBoundSelection.selectNeighbour(solution);
        Neighbour<Request, Enrollment> n = branchAndBound(solution);
        if (n == null) {
            if (iCycle == iMaxCycles || !iImproved) return null;
            nextCycle(solution);
            n = branchAndBound(solution);
        }
        return (n == null ? null : new ConstructionNeighbour((BranchBoundNeighbour)n));
        
    }
    
    /**
     * Takes {@link BranchBoundNeighbour} but only assign the given
     * number of assignments, corresponding to the number of cycles.
     */
    public class ConstructionNeighbour implements Neighbour<Request, Enrollment>{
        private BranchBoundNeighbour iNeighbour;
        
        public ConstructionNeighbour(BranchBoundNeighbour neighbour) {
            iNeighbour = neighbour;
        }

        /**
         * Only assign given number of assignments (from the first priority down).
         * Mark the cycle as improving if there was enough assignments to do.
         */
        @Override
        public void assign(Assignment<Request, Enrollment> assignment, long iteration) {
            if (iCycle >= iMaxCycles) {
                iNeighbour.assign(assignment, iteration);
                return;
            }
            for (Request r: iNeighbour.getStudent().getRequests())
                assignment.unassign(iteration, r);
            int n = iCycle;
            for (int i = 0; i < iNeighbour.getAssignment().length; i++) {
                if (iNeighbour.getAssignment()[i] != null) {
                    assignment.assign(iteration, iNeighbour.getAssignment()[i]);
                    n --;
                }
                if (n == 0) {
                    iImproved = true; break;
                }
            }
        }

        @Override
        public double value(Assignment<Request, Enrollment> assignment) {
            return iNeighbour.value(assignment);
        }
        
        @Override
        public String toString() {
            int n = iCycle;
            StringBuffer sb = new StringBuffer("B&B[" + n + "]{ " + iNeighbour.getStudent() + " " + sDF.format(-value(null) * 100.0) + "%");
            int idx = 0;
            for (Iterator<Request> e = iNeighbour.getStudent().getRequests().iterator(); e.hasNext(); idx++) {
                Request request = e.next();
                sb.append("\n  " + request);
                Enrollment enrollment = iNeighbour.getAssignment()[idx];
                if (enrollment == null) {
                    sb.append("  -- not assigned");
                } else {
                    sb.append("  -- " + enrollment);
                    n --;
                }
                if (n == 0) break;
            }
            sb.append("\n}");
            return sb.toString();
        }

        @Override
        public Map<Request, Enrollment> assignments() {
            Map<Request, Enrollment> ret = new HashMap<Request, Enrollment>();
            if (iCycle >= iMaxCycles) {
                return iNeighbour.assignments();
            }
            for (Request r: iNeighbour.getStudent().getRequests())
                ret.put(r, null);
            int n = iCycle;
            for (int i = 0; i < iNeighbour.getAssignment().length; i++) {
                if (iNeighbour.getAssignment()[i] != null) {
                    ret.put(iNeighbour.getAssignment()[i].variable(), iNeighbour.getAssignment()[i]);
                    n --;
                }
                if (n == 0) {
                    iImproved = true; break;
                }
            }
            return ret;
        }
    }

    
}
