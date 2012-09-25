package net.sf.cpsolver.exam.heuristics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamInstructor;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPeriodPlacement;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamRoomPlacement;
import net.sf.cpsolver.exam.model.ExamStudent;
import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.JProf;
import net.sf.cpsolver.ifs.util.Progress;

/**
 * Examination timetabling construction heuristics based on graph vertex coloring.
 * This approach is trying to find a (direct) conflict-free examination schedule using
 * a depth-first search, assigning periods to exams in a way that there is not student or
 * instructor direct conflict.
 * <br>
 * <br>
 * This heuristics works in following modes (defined by Exam.ColoringConstructionMode).
 * <ul>
 * <li>Greedy .. all exams are greedily assigned with periods (and best available rooms);
 *   Exams with smallest number of available periods (or highest number of connected exams
 *   if multiple) are assigned first. 
 * <li>ColorOnly .. all exams are assigned with periods using a depth-first search (ignoring
 *   all other constraints), this coloring is then extended to exam placements as much as possible
 * <li>Irredundant .. other constraints (distributions, rooms) are considered, however, to
 *   speedup the search redundant colorings are avoided -- this may not find a complete solution,
 *   especially when some periods are not swap-able
 * <li>Full .. all constraints are considered, a complete solution is guaranteed to be found, if
 *   it exists (and enough time is given)  
 * </ul>
 * <br>
 * Time to run can  be limited using Exam.ColoringConstructionTimeLimit parameter (double precision,
 * limit is in seconds, defaults to 5 minutes)
 * <br>
 * <br>
 * 
 * @version ExamTT 1.2 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2010 Tomas Muller<br>
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
public class ExamColoringConstruction implements NeighbourSelection<Exam, ExamPlacement> {
    private Progress iProgress;
    private double iT0;
    private double iTimeLimit = 300.0;
    private boolean iTimeLimitReached = false;
    private Solution<Exam, ExamPlacement> iSolution = null;
    private Mode iMode = Mode.Full;
    private Solver<Exam, ExamPlacement> iSolver;
    
    private static enum Mode {
        Greedy(true, false, true),
        ColorOnly(false, false, false),
        Irredundant(false, false, false),
        Full(false, true, true);
        boolean iGreedy, iRedundant, iConstraintCheck;
        private Mode(boolean gr, boolean r, boolean ch) { iGreedy = gr; iRedundant = r; iConstraintCheck = ch; }
        public boolean isGreedy() { return iGreedy; }
        public boolean isRedundant() { return iRedundant; }
        public boolean isConstraintCheck() { return iConstraintCheck; } 
    }
    
    public ExamColoringConstruction(DataProperties config) {
        iTimeLimit = config.getPropertyDouble("Exam.ColoringConstructionTimeLimit", iTimeLimit);
        iMode = Mode.valueOf(config.getProperty("Exam.ColoringConstructionMode", iMode.name()));
    }
    
    private boolean backTrack(int index, HashSet<Integer> colorsUsedSoFar, Collection<Vertex> vertices) {
        if (iTimeLimitReached || iSolver.isStop()) return false;
        if (JProf.currentTimeSec() - iT0 > iTimeLimit) {
            iTimeLimitReached = true;
            return false;
        }
        if (index == vertices.size()) return true;
        if (iProgress.getProgress() < index) {
            iProgress.setProgress(index);
            if (iMode.isConstraintCheck())
                iSolution.saveBest();
        }
        Vertex vertex = null;
        for (Vertex v: vertices) {
            if (v.color() >= 0) continue;
            if (vertex == null || v.compareTo(vertex) < 0) {
                vertex = v;
            }
        }
        if (colorsUsedSoFar != null) {
            for (int color: new TreeSet<Integer>(colorsUsedSoFar))
                if (vertex.colorize(color) && backTrack(1 + index, colorsUsedSoFar, vertices)) return true;
            for (int color: vertex.domain()) {
                if (colorsUsedSoFar.contains(color)) continue;
                if (vertex.colorize(color)) {
                    colorsUsedSoFar.add(color);
                    if (backTrack(1 + index, colorsUsedSoFar, vertices)) return true;
                    colorsUsedSoFar.remove(color);
                }
                break;
            }
        } else {
            for (int color: vertex.domain())
                if (vertex.colorize(color) && backTrack(1 + index, colorsUsedSoFar, vertices)) return true;
        }
        vertex.colorize(-1);
        return false;
    }

    @Override
    public void init(Solver<Exam, ExamPlacement> solver) {
        iProgress = Progress.getInstance(solver.currentSolution().getModel());
        iSolver = solver;
    }

    @Override
    public Neighbour<Exam, ExamPlacement> selectNeighbour(Solution<Exam, ExamPlacement> solution) {
        iSolution = solution;
        ExamModel model = (ExamModel)solution.getModel();
        // if (!model.assignedVariables().isEmpty()) return null;
        final HashMap<Exam, Vertex> vertices = new HashMap<Exam, Vertex>();
        for (Exam x: model.variables()) {
            vertices.put(x, new Vertex(x));
        }
        for (ExamStudent s: model.getStudents()) {
            for (Exam x: s.variables()) {
                for (Exam y: s.variables()) {
                    if (!x.equals(y)) {
                        vertices.get(x).neighbors().add(vertices.get(y));
                        vertices.get(y).neighbors().add(vertices.get(x));
                    }
                }
            }
        }
        for (ExamInstructor i: model.getInstructors()) {
            for (Exam x: i.variables()) {
                for (Exam y: i.variables()) {
                    if (!x.equals(y)) {
                        vertices.get(x).neighbors().add(vertices.get(y));
                        vertices.get(y).neighbors().add(vertices.get(x));
                    }
                }
            }
        }
        iProgress.setPhase("Graph coloring-based construction", vertices.size());
        iProgress.info("Looking for a conflict-free assignment using " + model.getPeriods().size() + " periods.");
        iT0 = JProf.currentTimeSec(); iTimeLimitReached = false;
        if (iMode.isGreedy()) {
            iProgress.info("Using greedy heuristics only (no backtracking)...");
        } else if (backTrack(0, (iMode.isRedundant() ? null : new HashSet<Integer>()), vertices.values())) {
            iProgress.info("Success!");
        } else if (iTimeLimitReached) {
            iProgress.info("There was no conflict-free schedule found during the given time.");
        } else if (iSolver.isStop()) {
            iProgress.info("Solver was stopped.");
        } else {
            if (iMode.isRedundant())
                iProgress.info("There is no conflict-free schedule!");
            else
                iProgress.info("Conflict-free schedule not found.");
        }
        if (iMode.isConstraintCheck())
            iSolution.restoreBest();
        HashSet<Vertex> remaning = new HashSet<Vertex>();
        for (Vertex v: vertices.values())
            if (v.color() < 0) remaning.add(v);
        remaining: while (!remaning.isEmpty()) {
            Vertex vertex = null;
            for (Vertex v: remaning)
                if (vertex == null || v.compareTo(vertex) < 0)
                    vertex = v;
            remaning.remove(vertex);
            for (int color: vertex.domain())
                if (vertex.colorize(color)) continue remaining;
        }
        if (!iMode.isConstraintCheck()) {
            return new Neighbour<Exam, ExamPlacement>() {
                @Override
                public void assign(long iteration) {
                    iProgress.info("Using graph coloring solution ...");
                    for (Vertex vertex: new TreeSet<Vertex>(vertices.values())) {
                        ExamPeriodPlacement period = vertex.period();
                        if (period == null || !vertex.exam().checkDistributionConstraints(period)) continue;
                        Set<ExamRoomPlacement> rooms = vertex.exam().findBestAvailableRooms(period);
                        if (rooms == null) continue;
                        vertex.exam().assign(iteration, new ExamPlacement(vertex.exam(), period, rooms));
                    }
                    HashSet<Vertex> unassigned = new HashSet<Vertex>();
                    for (Vertex vertex: vertices.values()) {
                        if (vertex.exam().getAssignment() == null) {
                            unassigned.add(vertex);
                            vertex.colorize(-1);
                        }
                    }
                    iSolution.saveBest();
                    iProgress.info("Extending ...");
                    unassigned: while (!unassigned.isEmpty()) {
                        Vertex vertex = null;
                        for (Vertex v: unassigned)
                            if (vertex == null || v.compareTo(vertex) < 0)
                                vertex = v;
                        unassigned.remove(vertex);
                        for (int color: vertex.domain()) {
                            if (!vertex.colorize(color)) continue;
                            ExamPeriodPlacement period = vertex.period(color);
                            if (period == null || !vertex.exam().checkDistributionConstraints(period)) continue;
                            Set<ExamRoomPlacement> rooms = vertex.exam().findBestAvailableRooms(period);
                            if (rooms == null) continue;
                            vertex.exam().assign(iteration, new ExamPlacement(vertex.exam(), period, rooms));
                            continue unassigned;
                        }
                        vertex.colorize(-1);
                    }
                    iSolution.saveBest();
                    iProgress.info("Construction done.");
                }
                @Override
                public double value() {
                    return 0;
                }
            };
        }
        return null;
    }

    /** Internal graph representation -- needed for domain caching */
    private class Vertex implements Comparable<Vertex> {
        private Exam iExam;
        private List<Vertex> iNeighbors = new ArrayList<Vertex>();
        private int iColor = -1;
        private HashMap<Integer, ExamPeriodPlacement> iDomain = new HashMap<Integer, ExamPeriodPlacement>();
        private HashMap<Integer, Vertex> iTaken = new HashMap<Integer, Vertex>();

        public Vertex(Exam exam) {
            iExam = exam;
            for (ExamPeriodPlacement period: exam.getPeriodPlacements())
                iDomain.put(period.getIndex(), period);
        }
        
        public List<Vertex> neighbors() { return iNeighbors; }
        
        public Set<Integer> domain() { return iDomain.keySet(); }
        
        public ExamPeriodPlacement period() { return (iColor < 0 ? null : iDomain.get(iColor)); }
        
        public ExamPeriodPlacement period(int color) { return (color < 0 ? null : iDomain.get(color)); }

        private boolean neighborColored(Vertex v, int color) {
            if (!iDomain.containsKey(color)) return true;
            if (iTaken.get(color) == null)
                iTaken.put(color, v);
            return iTaken.size() < iDomain.size();
        }
        
        private void neighborUncolored(Vertex v, int color) {
            if (!iDomain.containsKey(color)) return;
            if (v.equals(iTaken.get(color))) {
                for (Vertex w: neighbors()) {
                    if (w.equals(v)) continue;
                    if (w.color() == color) {
                        iTaken.put(color, w);
                        return;
                    }
                }
                iTaken.remove(color);
            }
        }
        
        public int color() { return iColor; }
        
        public boolean colorize(int color) {
            if (iColor == color) return true;
            ExamPlacement placement = null;
            if (color >= 0) {
                if (iTaken.get(color) != null || !iDomain.containsKey(color))
                    return false;
                if (iMode.isConstraintCheck()) {
                    ExamPeriodPlacement period = iDomain.get(color);
                    if (!iExam.checkDistributionConstraints(period)) return false;
                    Set<ExamRoomPlacement> rooms = iExam.findBestAvailableRooms(period);
                    if (rooms == null) return false;
                    placement = new ExamPlacement(iExam, period, rooms);
                }
            }
            if (iColor >= 0) {
                for (Vertex v: neighbors())
                    v.neighborUncolored(this, iColor);
            }
            boolean success = true;
            if (color >= 0) {
                for (Vertex v: neighbors()) {
                    if (!v.neighborColored(this, color)) {
                        success = false; break;
                    }
                }
            }
            if (success) {
                iColor = color;
                if (iMode.isConstraintCheck()) {
                    if (placement != null)
                        iExam.assign(0l, placement);
                    else
                        iExam.unassign(0l);
                }
            } else { // undo
                for (Vertex v: neighbors()) {
                    v.neighborUncolored(this, color);
                    if (iColor >= 0)
                        v.neighborColored(this, iColor);
                }
            }
            return success;
        }
        
        public int degree() {
            return iNeighbors.size();
        }
        
        public int available() {
            return iDomain.size() - iTaken.size();
        }
        
        public int degreeNotColored() {
            int ret = 0;
            for (Vertex v: neighbors())
                if (v.color() < 0) ret ++;
            return ret;
        }
        
        public Exam exam() { return iExam; }
        
        @Override
        public int compareTo(Vertex v) {
            if (available() < v.available()) return -1;
            if (v.available() < available()) return 1;
            if (degree() > v.degree()) return -1;
            if (v.degree() > degree()) return 1;        
            if (degreeNotColored() > v.degreeNotColored()) return -1;
            if (v.degreeNotColored() > degreeNotColored()) return 1;
            return Double.compare(exam().getId(), v.exam().getId());
        }
    }
}
