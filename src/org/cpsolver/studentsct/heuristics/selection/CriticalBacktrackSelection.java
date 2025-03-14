package org.cpsolver.studentsct.heuristics.selection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.studentsct.heuristics.RandomizedBacktrackNeighbourSelection;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;
import org.cpsolver.studentsct.model.Request.RequestPriority;

/**
 * Use backtrack neighbour selection. For all unassigned variables (in a random
 * order) that are marked as critical, {@link RandomizedBacktrackNeighbourSelection} is being used.
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
public class CriticalBacktrackSelection extends BacktrackSelection {
    private RequestPriority iPriority;
    
    public CriticalBacktrackSelection(DataProperties properties, RequestPriority priority) {
        super(properties);
        iPriority = priority;
        iIncludeAssignedRequests = properties.getPropertyBoolean("Neighbour.IncludeCriticalAssignedRequests", iIncludeAssignedRequests);
    }
    
    public CriticalBacktrackSelection(DataProperties properties) {
        this(properties, RequestPriority.Critical);
    }
    
    @Override
    public void init(Solver<Request, Enrollment> solver, String name) {
        List<Request> variables = new ArrayList<Request>();
        for (Request r: (iIncludeAssignedRequests ? solver.currentSolution().getModel().variables() : solver.currentSolution().getModel().unassignedVariables(solver.currentSolution().getAssignment())))
            if (iPriority.isCritical(r)) variables.add(r);
        Collections.shuffle(variables);
        Collections.sort(variables, iRequestComparator);
        iRequests = new LinkedList<Request>(variables);
        if (iRBtNSel == null) {
            try {
                iRBtNSel = new RandomizedBacktrackNeighbourSelection(solver.getProperties());
                iRBtNSel.init(solver);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        Progress.getInstance(solver.currentSolution().getModel()).setPhase(iPriority.name() + " Backtracking" + (iFilter == null ? "" : " (" + iFilter.getName().toLowerCase() + " students)") + "...", variables.size());
    }

}
