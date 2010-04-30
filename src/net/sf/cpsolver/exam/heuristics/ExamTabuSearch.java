package net.sf.cpsolver.exam.heuristics;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.log4j.Logger;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamPeriodPlacement;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.ifs.extension.ConflictStatistics;
import net.sf.cpsolver.ifs.extension.Extension;
import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.heuristics.ValueSelection;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.model.SimpleNeighbour;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * Tabu search algorithm. 
 * <br><br>
 * If used as {@link NeighbourSelection}, the most improving (re)assignment of a value to a variable
 * is returned (all variables and all their values are enumerated). If there are more than one of 
 * such assignments, one is selected randomly. A returned assignment can cause unassignment of
 * other existing assignments. The search is stopped ({@link ExamTabuSearch#selectNeighbour(Solution)} 
 * returns null) after TabuSearch.MaxIdle idle (not improving) iterations.
 * <br><br>
 * If used as {@link ValueSelection}, the most improving (re)assignment of a value to a given variable
 * is returned (all values of the given variable are enumerated). If there are more than one of 
 * such assignments, one is selected randomly. A returned assignment can cause unassignment of
 * other existing assignments.  
 * <br><br>
 * To avoid cycling, a tabu is maintainded during the search. It is the list of the last n
 * selected values. A selection of a value that is present in the tabu list is only allowed when it improves the 
 * best ever found solution.
 * <br><br>
 * The minimum size of the tabu list is TabuSearch.MinSize, maximum size is TabuSearch.MaxSize (tabu 
 * list is not used when both sizes are zero). The current size of the tabu list starts at
 * MinSize (and is reset to MinSize every time a new best solution is found), it is increased
 * by one up to the MaxSize after TabuSearch.MaxIdle / (MaxSize - MinSize) non-improving 
 * iterations.
 * <br><br>
 * Conflict-based Statistics {@link ConflictStatistics} (CBS) can be used instead of (or together with)
 * tabu list, when CBS is used as a solver extension.
 *  
 * @version
 * ExamTT 1.1 (Examination Timetabling)<br>
 * Copyright (C) 2008 Tomas Muller<br>
 * <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
public class ExamTabuSearch implements NeighbourSelection, ValueSelection {
    private static Logger sLog = Logger.getLogger(ExamTabuSearch.class);
    private ConflictStatistics iStat = null;

    private long iFirstIteration = -1;
    private long iMaxIdleIterations = 10000;

    private int iTabuMinSize = 0;
    private int iTabuMaxSize = 0;
    private TabuList iTabu = null;
    
    private double iConflictWeight = 1000000;
    private double iValueWeight = 1;
    
    /**
     * <ul>
     * <li>TabuSearch.MaxIdle ... maximum number of idle iterations (default is 10000)
     * <li>TabuSearch.MinSize ... minimum size of the tabu list
     * <li>TabuSearch.MaxSize ... maximum size of the tabu list
     * <li>Value.ValueWeight ... weight of a value (i.e., {@link Value#toDouble()})
     * <li>Value.ConflictWeight ... weight of a conflicting value (see {@link Model#conflictValues(Value)}), 
     * it is also weighted by the past occurrences when conflict-based statistics is used 
     * </ul>
     */
    public ExamTabuSearch(DataProperties properties) throws Exception {
        iTabuMinSize = properties.getPropertyInt("TabuSearch.MinSize", iTabuMinSize);
        iTabuMaxSize = properties.getPropertyInt("TabuSearch.MaxSize", iTabuMaxSize);
        if (iTabuMaxSize > 0) iTabu = new TabuList(iTabuMinSize);
        iMaxIdleIterations = properties.getPropertyLong("TabuSearch.MaxIdle", iMaxIdleIterations);
        iConflictWeight = properties.getPropertyDouble("Value.ConflictWeight", iConflictWeight);
        iValueWeight = properties.getPropertyDouble("Value.ValueWeight", iValueWeight);
    }
    
    /** Initialization */
    public void init(Solver solver) {
        for (Enumeration i = solver.getExtensions().elements(); i.hasMoreElements();) {
            Extension extension = (Extension)i.nextElement();
            if (extension instanceof ConflictStatistics)
                iStat = (ConflictStatistics)extension;
        }
    }
    
    /**
     * Neighbor selection 
     */
    public Neighbour selectNeighbour(Solution solution) {
        if (iFirstIteration<0)
            iFirstIteration = solution.getIteration();
        long idle = solution.getIteration()-Math.max(iFirstIteration,solution.getBestIteration()); 
        if (idle>iMaxIdleIterations) {
            sLog.debug("  [tabu]    max idle iterations reached");
            iFirstIteration=-1;
            if (iTabu!=null) iTabu.clear();
            return null;
        }
        if (iTabu!=null && iTabuMaxSize>iTabuMinSize) {
            if (idle==0) {
                iTabu.resize(iTabuMinSize);
            } else if (idle%(iMaxIdleIterations/(iTabuMaxSize-iTabuMinSize))==0) { 
                iTabu.resize(Math.min(iTabuMaxSize,iTabu.size()+1));
            }
        }
        
        boolean acceptConflicts = solution.getModel().getBestUnassignedVariables()>0;
        Model model = solution.getModel();
        double bestEval = 0.0;
        Vector best = null;
        for (Enumeration e=model.variables().elements();e.hasMoreElements();) {
            Exam exam = (Exam)e.nextElement();
            ExamPlacement assigned = (ExamPlacement)exam.getAssignment();
            double assignedVal = (assigned==null?iConflictWeight:iValueWeight*assigned.toDouble());
            for (Enumeration f=exam.getPeriodPlacements().elements();f.hasMoreElements();) {
                ExamPeriodPlacement period = (ExamPeriodPlacement)f.nextElement();
                Set rooms = exam.findBestAvailableRooms(period);
                if (rooms==null) rooms = exam.findRoomsRandom(period, false);
                if (rooms==null) continue;
                ExamPlacement value = new ExamPlacement(exam, period, rooms); 
                if (value.equals(assigned)) continue;
                double eval = iValueWeight*value.toDouble() - assignedVal;
                if (acceptConflicts) {
                    Set conflicts = model.conflictValues(value);
                    for (Iterator i=conflicts.iterator();i.hasNext();) {
                        Value conflict = (Value)i.next();
                        eval -= iValueWeight*conflict.toDouble();
                        eval += iConflictWeight * (1.0+(iStat==null?0.0:iStat.countRemovals(solution.getIteration(), conflict, value)));
                    }
                } else {
                    if (model.inConflict(value)) continue;
                }
                if (iTabu!=null && iTabu.contains(exam.getId()+":"+value.getPeriod().getIndex())) {
                    int un = model.nrUnassignedVariables()-(assigned==null?0:1);
                    if (un>model.getBestUnassignedVariables()) continue;
                    if (un==model.getBestUnassignedVariables() && model.getTotalValue()+eval>=solution.getBestValue()) continue;
                }
                if (best==null || bestEval>eval) {
                    if (best==null)
                        best = new Vector();
                    else
                        best.clear();
                    best.add(value);
                    bestEval = eval;
                } else if (bestEval==eval) {
                    best.add(value);
                }
            }
        }
        
        if (best==null) {
            sLog.debug("  [tabu] --none--");
            iFirstIteration=-1;
            if (iTabu!=null) iTabu.clear();
            return null;
        }
        ExamPlacement bestVal = (ExamPlacement)ToolBox.random(best);
        
        if (sLog.isDebugEnabled()) {
            Set conflicts = model.conflictValues(bestVal);
            double wconf = (iStat==null?0.0:iStat.countRemovals(solution.getIteration(), conflicts, bestVal));
            sLog.debug("  [tabu] "+bestVal+" ("+(bestVal.variable().getAssignment()==null?"":"was="+bestVal.variable().getAssignment()+", ")+"val="+bestEval+(conflicts.isEmpty()?"":", conf="+(wconf+conflicts.size())+"/"+conflicts)+")");
        }
        
        if (iTabu!=null) 
            iTabu.add(bestVal.variable().getId()+":"+bestVal.getPeriod().getIndex());

        return new SimpleNeighbour(bestVal.variable(),bestVal);        
    }
    
    /**
     * Value selection 
     */
    public Value selectValue(Solution solution, Variable variable) {
        if (iFirstIteration<0)
            iFirstIteration = solution.getIteration();
        long idle = solution.getIteration()-Math.max(iFirstIteration,solution.getBestIteration()); 
        if (idle>iMaxIdleIterations) {
            sLog.debug("  [tabu]    max idle iterations reached");
            iFirstIteration=-1;
            if (iTabu!=null) iTabu.clear();
            return null;
        }
        if (iTabu!=null && iTabuMaxSize>iTabuMinSize) {
            if (idle==0) {
                iTabu.resize(iTabuMinSize);
            } else if (idle%(iMaxIdleIterations/(iTabuMaxSize-iTabuMinSize))==0) { 
                iTabu.resize(Math.min(iTabuMaxSize,iTabu.size()+1));
            }
        }

        Model model = solution.getModel();
        double bestEval = 0.0;
        Vector best = null;

        Exam exam = (Exam)variable;
        ExamPlacement assigned = (ExamPlacement)variable.getAssignment();
        //double assignedVal = (assigned==null?-iConflictWeight:iValueWeight*assigned.toDouble());
        double assignedVal = (assigned==null?iConflictWeight:iValueWeight*assigned.toDouble());
        for (Enumeration f=exam.getPeriodPlacements().elements();f.hasMoreElements();) {
            ExamPeriodPlacement period = (ExamPeriodPlacement)f.nextElement();
            Set rooms = exam.findBestAvailableRooms(period);
            if (rooms==null) rooms = exam.findRoomsRandom(period, false);
            if (rooms==null) {
                sLog.info("Exam "+exam.getName()+" has no rooms for period "+period);
                continue;
            }
            ExamPlacement value = new ExamPlacement(exam, period, rooms); 
            if (value.equals(assigned)) continue;
            Set conflicts = model.conflictValues(value);
            double eval = iValueWeight*value.toDouble() - assignedVal;
            for (Iterator i=conflicts.iterator();i.hasNext();) {
                Value conflict = (Value)i.next();
                eval -= iValueWeight*conflict.toDouble();
                eval += iConflictWeight * (1.0+(iStat==null?0.0:iStat.countRemovals(solution.getIteration(), conflict, value)));
            }
            if (iTabu!=null && iTabu.contains(exam.getId()+":"+value.getPeriod().getIndex())) {
                int un = model.nrUnassignedVariables()-(assigned==null?0:1);
                if (un>model.getBestUnassignedVariables()) continue;
                if (un==model.getBestUnassignedVariables() && model.getTotalValue()+eval>=solution.getBestValue()) continue;
		    }
            if (best==null || bestEval>eval) {
                if (best==null)
                    best = new Vector();
                else
                    best.clear();
                best.add(value);
                bestEval = eval;
            } else if (bestEval==eval) {
                best.add(value);
            }
        }
        
        if (best==null) return null;
        ExamPlacement bestVal = (ExamPlacement)ToolBox.random(best);

        if (sLog.isDebugEnabled()) {
            Set conflicts = model.conflictValues(bestVal);
            double wconf = (iStat==null?0.0:iStat.countRemovals(solution.getIteration(), conflicts, bestVal));
            sLog.debug("  [tabu] "+bestVal+" ("+(bestVal.variable().getAssignment()==null?"":"was="+bestVal.variable().getAssignment()+", ")+"val="+bestEval+(conflicts.isEmpty()?"":", conf="+(wconf+conflicts.size())+"/"+conflicts)+")");
        }
        
        if (iTabu!=null) iTabu.add(exam.getId()+":"+bestVal.getPeriod().getIndex());
        
        return bestVal;
    }

    
    /** Tabu-list */
    private static class TabuList {
        private HashSet iList = new HashSet();
        private int iSize;
        private long iIteration = 0;
        
        public TabuList(int size) {
            iSize = size;
        }
        
        public Object add(Object object) {
            if (iSize==0) return object;
            if (contains(object)) {
                iList.remove(new TabuItem(object, 0));
                iList.add(new TabuItem(object, iIteration++));
                return null;
            } else {
                Object oldest = null;
                if (iList.size()>=iSize) oldest = removeOldest();
                iList.add(new TabuItem(object, iIteration++));
                return oldest;
            }
        }
        
        public void resize(int newSize) {
            iSize = newSize;
            while (iList.size()>newSize) removeOldest();
        }
        
        public boolean contains(Object object) {
            return iList.contains(new TabuItem(object,0));
        }
        
        public void clear() {
            iList.clear();
        }
        
        public int size() {
            return iSize;
        }
        
        public Object removeOldest() {
            TabuItem oldest = null;
            for (Iterator i=iList.iterator();i.hasNext();) {
                TabuItem element = (TabuItem)i.next();
                if (oldest==null || oldest.getIteration()>element.getIteration())
                    oldest = element;
            }
            if (oldest==null) return null;
            iList.remove(oldest);
            return oldest.getObject();
        }
        
        public String toString() {
            return new TreeSet(iList).toString();
        }
    }

    /** Tabu item (an item in {@link TabuList}) */
    private static class TabuItem implements Comparable {
        private Object iObject;
        private long iIteration;
        public TabuItem(Object object, long iteration) {
            iObject = object; iIteration = iteration;
        }
        public Object getObject() {
            return iObject;
        }
        public long getIteration() {
            return iIteration;
        }
        public boolean equals(Object object) {
            if (object==null || !(object instanceof TabuItem)) return false;
            return getObject().equals(((TabuItem)object).getObject());
        }
        public int hashCode() {
            return getObject().hashCode();
        }
        public int compareTo(Object o) {
            return Double.compare(getIteration(), ((TabuItem)o).getIteration());
        }
        public String toString() {
            return getObject().toString();
        }
    }
}
