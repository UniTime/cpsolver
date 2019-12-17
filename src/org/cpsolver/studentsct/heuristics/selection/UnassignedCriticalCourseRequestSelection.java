package org.cpsolver.studentsct.heuristics.selection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.cpsolver.ifs.heuristics.VariableSelection;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Request.RequestPriority;

public class UnassignedCriticalCourseRequestSelection implements VariableSelection<Request, Enrollment>{
    protected Queue<Request> iRequests = null;
    private RequestPriority iPriority = null;
    
    UnassignedCriticalCourseRequestSelection(RequestPriority priority) {
        iPriority = priority;
    }
    
    
    @Override
    public void init(Solver<Request, Enrollment> solver) {
        iRequests = new LinkedList<Request>();
    }

    @Override
    public Request selectVariable(Solution<Request, Enrollment> solution) {
        return nextRequest(solution);
    }
    
    protected synchronized Request nextRequest(Solution<Request, Enrollment> solution) {
        Request ret = iRequests.poll();
        if (ret == null) {
            List<Request> variables = new ArrayList<Request>();
            for (Request r: solution.getModel().unassignedVariables(solution.getAssignment()))
                if (iPriority.isCritical(r)) variables.add(r);
            Collections.shuffle(variables);
            iRequests.addAll(variables);
            ret = iRequests.poll();
        }
        return ret;
    }
}