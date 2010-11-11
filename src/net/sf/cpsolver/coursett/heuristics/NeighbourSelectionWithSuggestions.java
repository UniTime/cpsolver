package net.sf.cpsolver.coursett.heuristics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.heuristics.StandardNeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.JProf;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Neighbour selection which does the standard time neighbour selection most of
 * the time, however, the very best neighbour is selected time to time (using
 * backtracking based search).
 * 
 * @see StandardNeighbourSelection
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
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
    private int iSuggestionTimeout = 500;
    private int iSuggestionDepth = 4;

    private Solution<Lecture, Placement> iSolution = null;
    private SuggestionNeighbour iSuggestionNeighbour = null;
    private TimetableComparator iCmp = null;
    private double iValue = 0;
    private int iNrAssigned = 0;

    public NeighbourSelectionWithSuggestions(DataProperties properties) throws Exception {
        super(properties);
        iSuggestionProbability = properties
                .getPropertyDouble("Neighbour.SuggestionProbability", iSuggestionProbability);
        iSuggestionProbabilityAllAssigned = properties.getPropertyDouble("Neighbour.SuggestionProbabilityAllAssigned",
                iSuggestionProbabilityAllAssigned);
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
        iCmp = (TimetableComparator) solver.getSolutionComparator();
    }

    @Override
    public Neighbour<Lecture, Placement> selectNeighbour(Solution<Lecture, Placement> solution) {
        Neighbour<Lecture, Placement> neighbour = null;
        if (solution.getModel().unassignedVariables().isEmpty()) {
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

    public synchronized Neighbour<Lecture, Placement> selectNeighbourWithSuggestions(
            Solution<Lecture, Placement> solution, Lecture lecture, int depth) {
        if (lecture == null)
            return null;

        iSolution = solution;
        iSuggestionNeighbour = null;
        iValue = iCmp.currentValue(solution);
        iNrAssigned = solution.getModel().assignedVariables().size();

        synchronized (solution) {
            // System.out.println("BEFORE BT ("+lecture.getName()+"): nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iCmp.currentValue(iSolution));

            List<Lecture> initialLectures = new ArrayList<Lecture>(1);
            initialLectures.add(lecture);
            backtrack(JProf.currentTimeMillis(), initialLectures, new ArrayList<Lecture>(),
                    new HashMap<Lecture, Placement>(), depth);

            // System.out.println("AFTER  BT ("+lecture.getName()+"): nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iCmp.currentValue(iSolution));
        }

        return iSuggestionNeighbour;
    }

    private boolean containsCommited(Collection<Placement> values) {
        if (((TimetableModel) iSolution.getModel()).hasConstantVariables()) {
            for (Placement placement : values) {
                Lecture lecture = placement.variable();
                if (lecture.isCommitted())
                    return true;
            }
        }
        return false;
    }

    private void backtrack(long startTime, List<Lecture> initialLectures, List<Lecture> resolvedLectures,
            HashMap<Lecture, Placement> conflictsToResolve, int depth) {
        int nrUnassigned = conflictsToResolve.size();
        if ((initialLectures == null || initialLectures.isEmpty()) && nrUnassigned == 0) {
            if (iSolution.getModel().assignedVariables().size() > iNrAssigned
                    || (iSolution.getModel().assignedVariables().size() == iNrAssigned && iValue > iCmp
                            .currentValue(iSolution))) {
                if (iSuggestionNeighbour == null || iSuggestionNeighbour.compareTo(iSolution) >= 0)
                    iSuggestionNeighbour = new SuggestionNeighbour(resolvedLectures);
            }
            return;
        }
        if (depth <= 0)
            return;
        if (iSuggestionTimeout > 0 && JProf.currentTimeMillis() - startTime > iSuggestionTimeout) {
            return;
        }
        for (Lecture lecture: initialLectures != null && !initialLectures.isEmpty() ? initialLectures : new ArrayList<Lecture>(conflictsToResolve.keySet())) {
            if (resolvedLectures.contains(lecture))
                continue;
            resolvedLectures.add(lecture);
            for (Placement placement : lecture.values()) {
                if (placement.equals(lecture.getAssignment()))
                    continue;
                if (placement.isHard())
                    continue;
                Set<Placement> conflicts = iSolution.getModel().conflictValues(placement);
                if (conflicts != null && (nrUnassigned + conflicts.size() > depth))
                    continue;
                if (conflicts != null && conflicts.contains(placement))
                    continue;
                if (containsCommited(conflicts))
                    continue;
                boolean containException = false;
                if (conflicts != null) {
                    for (Iterator<Placement> i = conflicts.iterator(); !containException && i.hasNext();) {
                        Placement c = i.next();
                        if (resolvedLectures.contains((c.variable()).getClassId()))
                            containException = true;
                    }
                }
                if (containException)
                    continue;
                Placement cur = lecture.getAssignment();
                if (conflicts != null) {
                    for (Iterator<Placement> i = conflicts.iterator(); !containException && i.hasNext();) {
                        Placement c = i.next();
                        c.variable().unassign(0);
                    }
                }
                if (cur != null)
                    cur.variable().unassign(0);
                for (Iterator<Placement> i = conflicts.iterator(); !containException && i.hasNext();) {
                    Placement c = i.next();
                    conflictsToResolve.put(c.variable(), c);
                }
                Placement resolvedConf = conflictsToResolve.remove(lecture);
                backtrack(startTime, null, resolvedLectures, conflictsToResolve, depth - 1);
                if (cur == null)
                    lecture.unassign(0);
                else
                    lecture.assign(0, cur);
                for (Iterator<Placement> i = conflicts.iterator(); i.hasNext();) {
                    Placement p = i.next();
                    p.variable().assign(0, p);
                    conflictsToResolve.remove(p.variable());
                }
                if (resolvedConf != null)
                    conflictsToResolve.put(lecture, resolvedConf);
            }
            resolvedLectures.remove(lecture);
        }
    }

    public class SuggestionNeighbour extends Neighbour<Lecture, Placement> {
        private double iValue = 0;
        private List<Placement> iDifferentAssignments = null;

        public SuggestionNeighbour(List<Lecture> resolvedLectures) {
            iValue = iCmp.currentValue(iSolution);
            iDifferentAssignments = new ArrayList<Placement>();
            for (Lecture lecture : resolvedLectures) {
                Placement p = lecture.getAssignment();
                iDifferentAssignments.add(p);
            }
        }

        @Override
        public double value() {
            return iValue;
        }

        @Override
        public void assign(long iteration) {
            // System.out.println("START ASSIGN: nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iCmp.currentValue(iSolution));
            // System.out.println("  "+this);
            for (Placement p : iDifferentAssignments) {
                if (p.variable().getAssignment() != null)
                    p.variable().unassign(iteration);
            }
            for (Placement p : iDifferentAssignments) {
                p.variable().assign(iteration, p);
            }
            // System.out.println("END ASSIGN: nrAssigned="+iSolution.getModel().assignedVariables().size()+",  value="+iCmp.currentValue(iSolution));
        }

        public int compareTo(Solution<Lecture, Placement> solution) {
            return Double.compare(iValue, iCmp.currentValue(solution));
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer("Suggestion{value=" + (iValue - iCmp.currentValue(iSolution)) + ": ");
            for (Iterator<Placement> e = iDifferentAssignments.iterator(); e.hasNext();) {
                Placement p = e.next();
                sb.append("\n    " + p.variable().getName() + " " + p.getName() + (e.hasNext() ? "," : ""));
            }
            sb.append("}");
            return sb.toString();
        }
    }
}
