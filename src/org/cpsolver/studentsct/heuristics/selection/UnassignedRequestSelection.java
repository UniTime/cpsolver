package org.cpsolver.studentsct.heuristics.selection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.cpsolver.ifs.heuristics.VariableSelection;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.studentsct.filter.StudentFilter;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.FreeTimeRequest;
import org.cpsolver.studentsct.model.Request;

public class UnassignedRequestSelection implements VariableSelection<Request, Enrollment>{
    protected int iNrRounds = 0;
    protected Queue<Request> iRequests = null;
    protected StudentFilter iFilter = null;
    
    @Override
    public void init(Solver<Request, Enrollment> solver) {
        iRequests = new LinkedList<Request>();
        iNrRounds = solver.getProperties().getPropertyInt("UnassignedRequestSelection.NrRounds", 1);
    }

    @Override
    public Request selectVariable(Solution<Request, Enrollment> solution) {
        return nextRequest(solution);
    }
    
    protected synchronized Request nextRequest(Solution<Request, Enrollment> solution) {
        if (iRequests.isEmpty() && iNrRounds > 0) {
            iNrRounds --;
            List<Request> variables = new ArrayList<Request>();
            for (Request r: solution.getModel().unassignedVariables(solution.getAssignment())) {
                if (r instanceof FreeTimeRequest) continue;
                if (iFilter == null || iFilter.accept(r.getStudent()))
                    variables.add(r);
            }
            Collections.shuffle(variables);
            iRequests.addAll(variables);
        }
        return iRequests.poll();
    }
    
    /**
     * Only consider students meeting the given filter.
     */
    public StudentFilter getFilter() { return iFilter; }
    
    /**
     * Only consider students meeting the given filter.
     */
    public UnassignedRequestSelection withFilter(StudentFilter filter) { iFilter = filter; return this; }
}