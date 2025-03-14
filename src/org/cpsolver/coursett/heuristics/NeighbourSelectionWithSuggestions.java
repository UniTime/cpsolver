package org.cpsolver.coursett.heuristics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.StandardNeighbourSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.JProf;
import org.cpsolver.ifs.util.ToolBox;


/**
 * Neighbour selection which does the standard time neighbour selection most of
 * the time, however, the very best neighbour is selected time to time (using
 * backtracking based search).
 * 
 * @see StandardNeighbourSelection
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
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

public class NeighbourSelectionWithSuggestions extends StandardNeighbourSelection<Lecture, Placement> {
    private double iSuggestionProbability = 0.1;
    private double iSuggestionProbabilityAllAssigned = 0.5;
    protected int iSuggestionTimeout = 500;
    protected int iSuggestionDepth = 4;

    public NeighbourSelectionWithSuggestions(DataProperties properties) throws Exception {
        super(properties);
        iSuggestionProbability = properties.getPropertyDouble("Neighbour.SuggestionProbability", iSuggestionProbability);
        iSuggestionProbabilityAllAssigned = properties.getPropertyDouble("Neighbour.SuggestionProbabilityAllAssigned", iSuggestionProbabilityAllAssigned);
        iSuggestionTimeout = properties.getPropertyInt("Neighbour.SuggestionTimeout", iSuggestionTimeout);
        iSuggestionDepth = properties.getPropertyInt("Neighbour.SuggestionDepth", iSuggestionDepth);
    }

    public NeighbourSelectionWithSuggestions(Solver<Lecture, Placement> solver) throws Exception {
        this(solver.getProperties());
        init(solver);
    }

    @Override
    public void init(Solver<Lecture, Placement> solver) {
        super.init(solver);
    }

    @Override
    public Neighbour<Lecture, Placement> selectNeighbour(Solution<Lecture, Placement> solution) {
        Neighbour<Lecture, Placement> neighbour = null;
        if (solution.getModel().unassignedVariables(solution.getAssignment()).isEmpty()) {
            for (int d = iSuggestionDepth; d > 1; d--) {
                if (ToolBox.random() < Math.pow(iSuggestionProbabilityAllAssigned, d - 1)) {
                    neighbour = selectNeighbourWithSuggestions(solution, selectVariable(solution), d);
                    break;
                }
            }
        } else {
            for (int d = iSuggestionDepth; d > 1; d--) {
                if (ToolBox.random() < Math.pow(iSuggestionProbability, d - 1)) {
                    neighbour = selectNeighbourWithSuggestions(solution, selectVariable(solution), d);
                    break;
                }
            }
        }
        return (neighbour != null ? neighbour : super.selectNeighbour(solution));
    }

    public Neighbour<Lecture, Placement> selectNeighbourWithSuggestions(Solution<Lecture, Placement> solution, Lecture lecture, int depth) {
        if (lecture == null)
            return null;

        NeighbourSelectionWithSuggestionsContext context = new NeighbourSelectionWithSuggestionsContext(solution);

        Lock lock = solution.getLock().writeLock();
        lock.lock();
        try {
            // System.out.println("BEFORE BT ("+lecture.getName()+"): nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iCmp.currentValue(iSolution));

            List<Lecture> initialLectures = new ArrayList<Lecture>(1);
            initialLectures.add(lecture);
            backtrack(context, initialLectures, new HashMap<Lecture, Placement>(), new HashMap<Lecture, Placement>(), depth);

            // System.out.println("AFTER  BT ("+lecture.getName()+"): nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iCmp.currentValue(iSolution));
        } finally {
            lock.unlock();
        }

        return context.getSuggestionNeighbour();
    }

    private boolean containsCommited(NeighbourSelectionWithSuggestionsContext context, Collection<Placement> values) {
        if (context.getModel().hasConstantVariables()) {
            for (Placement placement : values) {
                Lecture lecture = placement.variable();
                if (lecture.isCommitted())
                    return true;
            }
        }
        return false;
    }

    private void backtrack(NeighbourSelectionWithSuggestionsContext context, List<Lecture> initialLectures, Map<Lecture, Placement> resolvedLectures, HashMap<Lecture, Placement> conflictsToResolve, int depth) {
        int nrUnassigned = conflictsToResolve.size();
        if ((initialLectures == null || initialLectures.isEmpty()) && nrUnassigned == 0) {
            context.setSuggestionNeighbourIfImproving(resolvedLectures);
            return;
        }
        if (depth <= 0 || context.checkTimeoutReached())
            return;
        
        Assignment<Lecture, Placement> assignment = context.getAssignment();
        for (Lecture lecture: initialLectures != null && !initialLectures.isEmpty() ? initialLectures : new ArrayList<Lecture>(conflictsToResolve.keySet())) {
            if (context.isTimeoutReached()) break;
            if (resolvedLectures.containsKey(lecture))
                continue;
            List<Placement> placements = lecture.values(assignment);
            int rnd = ToolBox.random(placements.size());
            placements: for (int idx = 0; idx < placements.size(); idx++) {
                Placement placement = placements.get((idx + rnd) % placements.size());
                if (context.isTimeoutReached()) break;
                Placement cur = assignment.getValue(lecture);
                if (placement.equals(cur))
                    continue;
                if (placement.isHard(assignment))
                    continue;
                Set<Placement> conflicts = context.getModel().conflictValues(assignment, placement);
                if (nrUnassigned + conflicts.size() > depth)
                    continue;
                if (conflicts.contains(placement))
                    continue;
                if (containsCommited(context, conflicts))
                    continue;
                for (Iterator<Placement> i = conflicts.iterator();i.hasNext();) {
                    Placement c = i.next();
                    if (resolvedLectures.containsKey(c.variable()))
                        continue placements;
                }
                for (Iterator<Placement> i = conflicts.iterator(); i.hasNext();) {
                    Placement c = i.next();
                    assignment.unassign(0, c.variable());
                }
                assignment.assign(0, placement);
                for (Iterator<Placement> i = conflicts.iterator(); i.hasNext();) {
                    Placement c = i.next();
                    conflictsToResolve.put(c.variable(), c);
                }
                Placement resolvedConf = conflictsToResolve.remove(lecture);
                resolvedLectures.put(lecture, placement);
                backtrack(context, null, resolvedLectures, conflictsToResolve, depth - 1);
                resolvedLectures.remove(lecture);
                if (cur == null)
                    assignment.unassign(0, lecture);
                else
                    assignment.assign(0, cur);
                for (Iterator<Placement> i = conflicts.iterator(); i.hasNext();) {
                    Placement p = i.next();
                    assignment.assign(0, p);
                    conflictsToResolve.remove(p.variable());
                }
                if (resolvedConf != null)
                    conflictsToResolve.put(lecture, resolvedConf);
            }
        }
    }

    public class SuggestionNeighbour implements Neighbour<Lecture, Placement> {
        private double iValue = 0;
        private List<Placement> iDifferentAssignments = null;

        public SuggestionNeighbour(Map<Lecture, Placement> resolvedLectures, double value) {
            iValue = value;
            iDifferentAssignments = new ArrayList<Placement>(resolvedLectures.values());
        }

        @Override
        public double value(Assignment<Lecture, Placement> assignment) {
            return iValue;
        }

        @Override
        public void assign(Assignment<Lecture, Placement> assignment, long iteration) {
            // System.out.println("START ASSIGN: nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iCmp.currentValue(iSolution));
            // System.out.println("  "+this);
            for (Placement p : iDifferentAssignments)
                assignment.unassign(iteration, p.variable());
            for (Placement p : iDifferentAssignments)
                assignment.assign(iteration, p);
            // System.out.println("END ASSIGN: nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iCmp.currentValue(iSolution));
        }

        public int compareTo(Solution<Lecture, Placement> solution) {
            return Double.compare(iValue, solution.getModel().getTotalValue(solution.getAssignment()));
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer("Suggestion{value=" + iValue + ": ");
            for (Iterator<Placement> e = iDifferentAssignments.iterator(); e.hasNext();) {
                Placement p = e.next();
                sb.append("\n    " + p.variable().getName() + " " + p.getName() + (e.hasNext() ? "," : ""));
            }
            sb.append("}");
            return sb.toString();
        }

        @Override
        public Map<Lecture, Placement> assignments() {
            Map<Lecture, Placement> ret = new HashMap<Lecture, Placement>();
            for (Placement p : iDifferentAssignments)
                ret.put(p.variable(), p);
            return ret;
        }
    }
    
    public class NeighbourSelectionWithSuggestionsContext {
        private Solution<Lecture, Placement> iSolution = null;
        private SuggestionNeighbour iSuggestionNeighbour = null;
        private double iValue = 0;
        private int iNrAssigned = 0;
        private boolean iTimeoutReached = false;
        private long iStartTime;
        
        public NeighbourSelectionWithSuggestionsContext(Solution<Lecture, Placement> solution) {
            iSolution = solution;
            iSuggestionNeighbour = null;
            iValue = solution.getModel().getTotalValue(solution.getAssignment());
            iNrAssigned = solution.getAssignment().nrAssignedVariables();
            iTimeoutReached = false;
            iStartTime = JProf.currentTimeMillis();
        }

        public SuggestionNeighbour getSuggestionNeighbour() { return iSuggestionNeighbour; }
        
        public boolean setSuggestionNeighbourIfImproving(Map<Lecture, Placement> assignment) {
            if (getAssignment().nrAssignedVariables() > getNrAssigned() || (getAssignment().nrAssignedVariables() == getNrAssigned() && getValue() > getModel().getTotalValue(getAssignment()))) {
                double value = getModel().getTotalValue(getAssignment());
                if (getSuggestionNeighbour() == null || getSuggestionNeighbour().value(getAssignment()) >= value) {
                    iSuggestionNeighbour = new SuggestionNeighbour(assignment, value);
                    return true;
                }
            }
            return false;
        }
        
        public Solution<Lecture, Placement> getSolution() { return iSolution; }
        public Assignment<Lecture, Placement> getAssignment() { return getSolution().getAssignment(); }
        public TimetableModel getModel() { return (TimetableModel)getSolution().getModel(); }
        public int getNrAssigned() { return iNrAssigned; }
        public double getValue() { return iValue; }
        
        public boolean isTimeoutReached() { return iTimeoutReached; }
        public boolean checkTimeoutReached() {
            if (iTimeoutReached) return true;
            if (iSuggestionTimeout > 0 && JProf.currentTimeMillis() - iStartTime > iSuggestionTimeout)
                iTimeoutReached = true;
            return iTimeoutReached;
        }
        public void setTimeoutReached(boolean timeoutReached) { iTimeoutReached = timeoutReached; }
    }
}
