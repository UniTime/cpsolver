package net.sf.cpsolver.exam.heuristics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPeriodPlacement;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamRoomPlacement;
import net.sf.cpsolver.ifs.extension.ConflictStatistics;
import net.sf.cpsolver.ifs.extension.Extension;
import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.heuristics.ValueSelection;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.SimpleNeighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;

import org.apache.log4j.Logger;

/**
 * Tabu search algorithm. <br>
 * <br>
 * If used as {@link NeighbourSelection}, the most improving (re)assignment of a
 * value to a variable is returned (all variables and all their values are
 * enumerated). If there are more than one of such assignments, one is selected
 * randomly. A returned assignment can cause unassignment of other existing
 * assignments. The search is stopped (
 * {@link ExamTabuSearch#selectNeighbour(Solution)} returns null) after
 * TabuSearch.MaxIdle idle (not improving) iterations. <br>
 * <br>
 * If used as {@link ValueSelection}, the most improving (re)assignment of a
 * value to a given variable is returned (all values of the given variable are
 * enumerated). If there are more than one of such assignments, one is selected
 * randomly. A returned assignment can cause unassignment of other existing
 * assignments. <br>
 * <br>
 * To avoid cycling, a tabu is maintainded during the search. It is the list of
 * the last n selected values. A selection of a value that is present in the
 * tabu list is only allowed when it improves the best ever found solution. <br>
 * <br>
 * The minimum size of the tabu list is TabuSearch.MinSize, maximum size is
 * TabuSearch.MaxSize (tabu list is not used when both sizes are zero). The
 * current size of the tabu list starts at MinSize (and is reset to MinSize
 * every time a new best solution is found), it is increased by one up to the
 * MaxSize after TabuSearch.MaxIdle / (MaxSize - MinSize) non-improving
 * iterations. <br>
 * <br>
 * Conflict-based Statistics {@link ConflictStatistics} (CBS) can be used
 * instead of (or together with) tabu list, when CBS is used as a solver
 * extension.
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
public class ExamTabuSearch implements NeighbourSelection<Exam, ExamPlacement>, ValueSelection<Exam, ExamPlacement> {
    private static Logger sLog = Logger.getLogger(ExamTabuSearch.class);
    private ConflictStatistics<Exam, ExamPlacement> iStat = null;

    private long iFirstIteration = -1;
    private long iMaxIdleIterations = 10000;

    private int iTabuMinSize = 0;
    private int iTabuMaxSize = 0;
    private TabuList iTabu = null;

    private double iConflictWeight = 1000000;
    private double iValueWeight = 1;

    /**
     * <ul>
     * <li>TabuSearch.MaxIdle ... maximum number of idle iterations (default is
     * 10000)
     * <li>TabuSearch.MinSize ... minimum size of the tabu list
     * <li>TabuSearch.MaxSize ... maximum size of the tabu list
     * <li>Value.ValueWeight ... weight of a value (i.e.,
     * {@link Value#toDouble()})
     * <li>Value.ConflictWeight ... weight of a conflicting value (see
     * {@link Model#conflictValues(Value)}), it is also weighted by the past
     * occurrences when conflict-based statistics is used
     * </ul>
     */
    public ExamTabuSearch(DataProperties properties) throws Exception {
        iTabuMinSize = properties.getPropertyInt("TabuSearch.MinSize", iTabuMinSize);
        iTabuMaxSize = properties.getPropertyInt("TabuSearch.MaxSize", iTabuMaxSize);
        if (iTabuMaxSize > 0)
            iTabu = new TabuList(iTabuMinSize);
        iMaxIdleIterations = properties.getPropertyLong("TabuSearch.MaxIdle", iMaxIdleIterations);
        iConflictWeight = properties.getPropertyDouble("Value.ConflictWeight", iConflictWeight);
        iValueWeight = properties.getPropertyDouble("Value.ValueWeight", iValueWeight);
    }

    /** Initialization */
    @Override
    public void init(Solver<Exam, ExamPlacement> solver) {
        for (Extension<Exam, ExamPlacement> extension : solver.getExtensions()) {
            if (ConflictStatistics.class.isInstance(extension))
                iStat = (ConflictStatistics<Exam, ExamPlacement>) extension;
        }
    }

    /**
     * Neighbor selection
     */
    @Override
    public Neighbour<Exam, ExamPlacement> selectNeighbour(Solution<Exam, ExamPlacement> solution) {
        if (iFirstIteration < 0)
            iFirstIteration = solution.getIteration();
        long idle = solution.getIteration() - Math.max(iFirstIteration, solution.getBestIteration());
        if (idle > iMaxIdleIterations) {
            sLog.debug("  [tabu]    max idle iterations reached");
            iFirstIteration = -1;
            if (iTabu != null)
                iTabu.clear();
            return null;
        }
        if (iTabu != null && iTabuMaxSize > iTabuMinSize) {
            if (idle == 0) {
                iTabu.resize(iTabuMinSize);
            } else if (idle % (iMaxIdleIterations / (iTabuMaxSize - iTabuMinSize)) == 0) {
                iTabu.resize(Math.min(iTabuMaxSize, iTabu.size() + 1));
            }
        }

        boolean acceptConflicts = solution.getModel().getBestUnassignedVariables() > 0;
        ExamModel model = (ExamModel) solution.getModel();
        double bestEval = 0.0;
        List<ExamPlacement> best = null;
        for (Exam exam : model.variables()) {
            ExamPlacement assigned = exam.getAssignment();
            double assignedVal = (assigned == null ? iConflictWeight : iValueWeight * assigned.toDouble());
            for (ExamPeriodPlacement period : exam.getPeriodPlacements()) {
                Set<ExamRoomPlacement> rooms = exam.findBestAvailableRooms(period);
                if (rooms == null)
                    rooms = exam.findRoomsRandom(period, false);
                if (rooms == null)
                    continue;
                ExamPlacement value = new ExamPlacement(exam, period, rooms);
                if (value.equals(assigned))
                    continue;
                double eval = iValueWeight * value.toDouble() - assignedVal;
                if (acceptConflicts) {
                    Set<ExamPlacement> conflicts = model.conflictValues(value);
                    for (ExamPlacement conflict : conflicts) {
                        eval -= iValueWeight * conflict.toDouble();
                        eval += iConflictWeight
                                * (1.0 + (iStat == null ? 0.0 : iStat.countRemovals(solution.getIteration(), conflict,
                                        value)));
                    }
                } else {
                    if (model.inConflict(value))
                        continue;
                }
                if (iTabu != null && iTabu.contains(exam.getId() + ":" + value.getPeriod().getIndex())) {
                    int un = model.nrUnassignedVariables() - (assigned == null ? 0 : 1);
                    if (un > model.getBestUnassignedVariables())
                        continue;
                    if (un == model.getBestUnassignedVariables()
                            && model.getTotalValue() + eval >= solution.getBestValue())
                        continue;
                }
                if (best == null || bestEval > eval) {
                    if (best == null)
                        best = new ArrayList<ExamPlacement>();
                    else
                        best.clear();
                    best.add(value);
                    bestEval = eval;
                } else if (bestEval == eval) {
                    best.add(value);
                }
            }
        }

        if (best == null) {
            sLog.debug("  [tabu] --none--");
            iFirstIteration = -1;
            if (iTabu != null)
                iTabu.clear();
            return null;
        }
        ExamPlacement bestVal = ToolBox.random(best);

        if (sLog.isDebugEnabled()) {
            Set<ExamPlacement> conflicts = model.conflictValues(bestVal);
            double wconf = (iStat == null ? 0.0 : iStat.countRemovals(solution.getIteration(), conflicts, bestVal));
            sLog.debug("  [tabu] "
                    + bestVal
                    + " ("
                    + (bestVal.variable().getAssignment() == null ? "" : "was=" + bestVal.variable().getAssignment()
                            + ", ") + "val=" + bestEval
                    + (conflicts.isEmpty() ? "" : ", conf=" + (wconf + conflicts.size()) + "/" + conflicts) + ")");
        }

        if (iTabu != null)
            iTabu.add(bestVal.variable().getId() + ":" + bestVal.getPeriod().getIndex());

        return new SimpleNeighbour<Exam, ExamPlacement>(bestVal.variable(), bestVal);
    }

    /**
     * Value selection
     */
    @Override
    public ExamPlacement selectValue(Solution<Exam, ExamPlacement> solution, Exam exam) {
        if (iFirstIteration < 0)
            iFirstIteration = solution.getIteration();
        long idle = solution.getIteration() - Math.max(iFirstIteration, solution.getBestIteration());
        if (idle > iMaxIdleIterations) {
            sLog.debug("  [tabu]    max idle iterations reached");
            iFirstIteration = -1;
            if (iTabu != null)
                iTabu.clear();
            return null;
        }
        if (iTabu != null && iTabuMaxSize > iTabuMinSize) {
            if (idle == 0) {
                iTabu.resize(iTabuMinSize);
            } else if (idle % (iMaxIdleIterations / (iTabuMaxSize - iTabuMinSize)) == 0) {
                iTabu.resize(Math.min(iTabuMaxSize, iTabu.size() + 1));
            }
        }

        ExamModel model = (ExamModel) solution.getModel();
        double bestEval = 0.0;
        List<ExamPlacement> best = null;

        ExamPlacement assigned = exam.getAssignment();
        // double assignedVal =
        // (assigned==null?-iConflictWeight:iValueWeight*assigned.toDouble());
        double assignedVal = (assigned == null ? iConflictWeight : iValueWeight * assigned.toDouble());
        for (ExamPeriodPlacement period : exam.getPeriodPlacements()) {
            Set<ExamRoomPlacement> rooms = exam.findBestAvailableRooms(period);
            if (rooms == null)
                rooms = exam.findRoomsRandom(period, false);
            if (rooms == null) {
                sLog.info("Exam " + exam.getName() + " has no rooms for period " + period);
                continue;
            }
            ExamPlacement value = new ExamPlacement(exam, period, rooms);
            if (value.equals(assigned))
                continue;
            Set<ExamPlacement> conflicts = model.conflictValues(value);
            double eval = iValueWeight * value.toDouble() - assignedVal;
            for (ExamPlacement conflict : conflicts) {
                eval -= iValueWeight * conflict.toDouble();
                eval += iConflictWeight
                        * (1.0 + (iStat == null ? 0.0 : iStat.countRemovals(solution.getIteration(), conflict, value)));
            }
            if (iTabu != null && iTabu.contains(exam.getId() + ":" + value.getPeriod().getIndex())) {
                int un = model.nrUnassignedVariables() - (assigned == null ? 0 : 1);
                if (un > model.getBestUnassignedVariables())
                    continue;
                if (un == model.getBestUnassignedVariables() && model.getTotalValue() + eval >= solution.getBestValue())
                    continue;
            }
            if (best == null || bestEval > eval) {
                if (best == null)
                    best = new ArrayList<ExamPlacement>();
                else
                    best.clear();
                best.add(value);
                bestEval = eval;
            } else if (bestEval == eval) {
                best.add(value);
            }
        }

        if (best == null)
            return null;
        ExamPlacement bestVal = ToolBox.random(best);

        if (sLog.isDebugEnabled()) {
            Set<ExamPlacement> conflicts = model.conflictValues(bestVal);
            double wconf = (iStat == null ? 0.0 : iStat.countRemovals(solution.getIteration(), conflicts, bestVal));
            sLog.debug("  [tabu] "
                    + bestVal
                    + " ("
                    + (bestVal.variable().getAssignment() == null ? "" : "was=" + bestVal.variable().getAssignment()
                            + ", ") + "val=" + bestEval
                    + (conflicts.isEmpty() ? "" : ", conf=" + (wconf + conflicts.size()) + "/" + conflicts) + ")");
        }

        if (iTabu != null)
            iTabu.add(exam.getId() + ":" + bestVal.getPeriod().getIndex());

        return bestVal;
    }

    /** Tabu-list */
    private static class TabuList {
        private HashSet<TabuItem> iList = new HashSet<TabuItem>();
        private int iSize;
        private long iIteration = 0;

        public TabuList(int size) {
            iSize = size;
        }

        public Object add(Object object) {
            if (iSize == 0)
                return object;
            if (contains(object)) {
                iList.remove(new TabuItem(object, 0));
                iList.add(new TabuItem(object, iIteration++));
                return null;
            } else {
                Object oldest = null;
                if (iList.size() >= iSize)
                    oldest = removeOldest();
                iList.add(new TabuItem(object, iIteration++));
                return oldest;
            }
        }

        public void resize(int newSize) {
            iSize = newSize;
            while (iList.size() > newSize)
                removeOldest();
        }

        public boolean contains(Object object) {
            return iList.contains(new TabuItem(object, 0));
        }

        public void clear() {
            iList.clear();
        }

        public int size() {
            return iSize;
        }

        public Object removeOldest() {
            TabuItem oldest = null;
            for (TabuItem element : iList) {
                if (oldest == null || oldest.getIteration() > element.getIteration())
                    oldest = element;
            }
            if (oldest == null)
                return null;
            iList.remove(oldest);
            return oldest.getObject();
        }

        @Override
        public String toString() {
            return new TreeSet<TabuItem>(iList).toString();
        }
    }

    /** Tabu item (an item in {@link TabuList}) */
    private static class TabuItem implements Comparable<TabuItem> {
        private Object iObject;
        private long iIteration;

        public TabuItem(Object object, long iteration) {
            iObject = object;
            iIteration = iteration;
        }

        public Object getObject() {
            return iObject;
        }

        public long getIteration() {
            return iIteration;
        }

        @Override
        public boolean equals(Object object) {
            if (object == null || !(object instanceof TabuItem))
                return false;
            return getObject().equals(((TabuItem) object).getObject());
        }

        @Override
        public int hashCode() {
            return getObject().hashCode();
        }

        @Override
        public int compareTo(TabuItem o) {
            return Double.compare(getIteration(), o.getIteration());
        }

        @Override
        public String toString() {
            return getObject().toString();
        }
    }
}
