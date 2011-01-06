package net.sf.cpsolver.studentsct.heuristics.selection;

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;

import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.Progress;
import net.sf.cpsolver.studentsct.StudentSectioningModel;
import net.sf.cpsolver.studentsct.heuristics.selection.BranchBoundSelection.BranchBoundNeighbour;
import net.sf.cpsolver.studentsct.heuristics.studentord.StudentChoiceRealFirstOrder;
import net.sf.cpsolver.studentsct.heuristics.studentord.StudentOrder;
import net.sf.cpsolver.studentsct.model.Enrollment;
import net.sf.cpsolver.studentsct.model.Request;
import net.sf.cpsolver.studentsct.model.Student;

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

public class PriorityConstructionSelection implements NeighbourSelection<Request, Enrollment> {
    private static org.apache.log4j.Logger sLog = org.apache.log4j.Logger.getLogger(PriorityConstructionSelection.class);
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
        iSkip = !solver.currentSolution().getModel().assignedVariables().isEmpty();
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
            Neighbour<Request, Enrollment> neighbour = iBranchBoundSelection.getSelection(student).select();
            if (neighbour != null)
                return neighbour;
        }
        return null;
    }
    
    /** Increment cycle */
    protected void nextCycle(Solution<Request, Enrollment> solution) {
        iCycle ++; iImproved = false;
        sLog.debug("Assigning up to " + iCycle + " requests...");
        
        StudentSectioningModel m = (StudentSectioningModel)solution.getModel();
        double tv = m.getTotalValue(true);
        sLog.debug("**CURR** " + solution.getModel().toString() + ", TM:" + sDF.format(solution.getTime() / 3600.0) + "h, " + 
                "TV:" + sDF.format(-tv) + " (" + sDF.format(-100.0 * tv / m.getStudents().size()) + "%)");
        
        iStudentsEnumeration = iStudents.iterator();
        Progress.getInstance(solution.getModel()).setPhase("Construction[" + iCycle + "]...", iStudents.size());
    }

    /**
     * Select neighbor. All students are taken, one by one in a random order.
     * For each student a branch & bound search is employed.
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
    public class ConstructionNeighbour extends Neighbour<Request, Enrollment>{
        private BranchBoundNeighbour iNeighbour;
        
        public ConstructionNeighbour(BranchBoundNeighbour neighbour) {
            iNeighbour = neighbour;
        }

        /**
         * Only assign given number of assignments (from the first priority down).
         * Mark the cycle as improving if there was enough assignments to do.
         */
        @Override
        public void assign(long iteration) {
            if (iCycle >= iMaxCycles) {
                iNeighbour.assign(iteration);
                return;
            }
            for (Request r: iNeighbour.getStudent().getRequests())
                if (r.getAssignment() != null) r.unassign(iteration);
            int n = iCycle;
            for (int i = 0; i < iNeighbour.getAssignment().length; i++) {
                if (iNeighbour.getAssignment()[i] != null) {
                    iNeighbour.getAssignment()[i].variable().assign(iteration, iNeighbour.getAssignment()[i]);
                    n --;
                }
                if (n == 0) {
                    iImproved = true; break;
                }
            }
        }

        @Override
        public double value() {
            return iNeighbour.value();
        }
        
        @Override
        public String toString() {
            int n = iCycle;
            StringBuffer sb = new StringBuffer("B&B[" + n + "]{ " + 
                    iNeighbour.getStudent() + " " + sDF.format(-value() * 100.0) + "%");
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
    }

    
}
