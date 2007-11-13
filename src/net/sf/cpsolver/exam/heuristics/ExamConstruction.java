package net.sf.cpsolver.exam.heuristics;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPeriod;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.neighbours.ExamSimpleNeighbour;
import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;


import org.apache.log4j.Logger;
/**
 * Initial solution construction heuristics.
 * <br><br>
 * While there are exams that are still not assigned:
 * <ul>
 * <li>The worst unassigned exam is selected (using {@link Exam#compareTo(Object)})
 * <li>The best period that does not break any hard constraint and
 * for which there is a room assignment available is selected together with 
 * the set the best available rooms for the exam in the best period
 * (computed using {@link Exam#findBestAvailableRooms(ExamPeriod)}).
 * </ul>
 * <br><br>
 * If problem property ExamConstruction.CheckLocalOptimality is true,
 * local (time) optimality is enforced at the end of this phase. During this
 * procedure, for each exam, it tries to change the period of the exam
 * so that the time cost is lower (see {@link ExamPlacement#getTimeCost()}),
 * but no hard constraint is violated. The problem is considered locally 
 * optimal if there is no such move. 
 * <br><br>
 * 
 * @version
 * ExamTT 1.1 (Examination Timetabling)<br>
 * Copyright (C) 2007 Tomas Muller<br>
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
public class ExamConstruction implements NeighbourSelection {
    private static Logger sLog = Logger.getLogger(ExamConstruction.class);
    private HashSet iAssignments = new HashSet();
    private boolean iCheckLocalOptimality = true;

    /**
     * Constructor
     * @param properties problem properties
     */
    public ExamConstruction(DataProperties properties) {
        iCheckLocalOptimality = properties.getPropertyBoolean("ExamConstruction.CheckLocalOptimality", iCheckLocalOptimality);
    }
    
    /**
     * Initialization
     */
    public void init(Solver solver) {
    }
    
    /**
     * Find a new assignment of one of the assigned exams that improves
     * the time cost {@link ExamPlacement#getTimeCost()} and for which 
     * there is a set of available rooms {@link Exam#findBestAvailableRooms(ExamPeriod)}.
     * Return null, if there is no such assignment (the problem is considered
     * locally optimal).
     */
    public Neighbour checkLocalOptimality(ExamModel model) {
        for (Enumeration e=model.assignedVariables().elements();e.hasMoreElements();) {
            Exam exam = (Exam)e.nextElement();
            ExamPlacement current = (ExamPlacement)exam.getAssignment(); 
            if (exam.hasPreAssignedPeriod()) continue;
            if (current.getTimeCost()<=0) continue;
            ExamPlacement best = null;
            for (Enumeration f=model.getPeriods().elements();f.hasMoreElements();) {
                ExamPeriod period = (ExamPeriod)f.nextElement();
                if (!exam.isAvailable(period)) continue;
                if (exam.countStudentConflicts(period)>0) {
                    if (iAssignments.contains(exam.getId()+":"+period.getIndex())) continue;
                    if (exam.hasStudentConflictWithPreAssigned(period)) continue;
                }
                ExamPlacement placement = new ExamPlacement(exam, period, null);
                if (best==null || best.getTimeCost()>placement.getTimeCost()) {
                    Set rooms = exam.findBestAvailableRooms(period);
                    if (rooms!=null)
                        best = new ExamPlacement(exam, period, rooms);
                }
            }
            if (best!=null && best.getTimeCost()<current.getTimeCost()) return new ExamSimpleNeighbour(best);
        }
        return null;
    }
    
    /**
     * Select a neighbour.
     * While there are exams that are still not assigned:
     * <ul>
     * <li>The worst unassigned exam is selected (using {@link Exam#compareTo(Object)})
     * <li>The best period that does not break any hard constraint and
     * for which there is a room assignment available is selected together with 
     * the set the best available rooms for the exam in the best period
     * (computed using {@link Exam#findBestAvailableRooms(ExamPeriod)}).
     * </ul>
     * Return null when done (all variables are assigned and the problem is locally optimal).
     */
    public Neighbour selectNeighbour(Solution solution) {
        ExamModel model = (ExamModel)solution.getModel();
        if (model.unassignedVariables().isEmpty()) return (iCheckLocalOptimality?checkLocalOptimality(model):null);
        Exam bestExam = null;
        for (Enumeration e=model.unassignedVariables().elements();e.hasMoreElements();) {
            Exam exam = (Exam)e.nextElement();
            if (bestExam==null || exam.compareTo(bestExam)<0)
                bestExam = exam;
        }
        if (sLog.isDebugEnabled()) sLog.debug("  -- "+bestExam);
        ExamPlacement best = null;
        for (Enumeration e=model.getPeriods().elements();e.hasMoreElements();) {
            ExamPeriod period = (ExamPeriod)e.nextElement();
            if (!bestExam.isAvailable(period)) continue;
            if (bestExam.countStudentConflicts(period)>0) {
                if (iAssignments.contains(bestExam.getId()+":"+period.getIndex())) continue;
                if (bestExam.hasStudentConflictWithPreAssigned(period)) continue;
            }
            ExamPlacement placement = new ExamPlacement(bestExam, period, null);
            if (best==null || best.getTimeCost()>placement.getTimeCost()) {
                Set rooms = bestExam.findBestAvailableRooms(period);
                if (rooms!=null)
                    best = new ExamPlacement(bestExam, period, rooms);
            }
        }
        if (sLog.isDebugEnabled()) sLog.debug("    -- "+best);
        if (best!=null) {
            if (sLog.isDebugEnabled()) { 
                Set conf = bestExam.getStudentConflicts(best.getPeriod());
                if (!conf.isEmpty())
                    sLog.debug("      -- conflicts with "+conf);
            }
            iAssignments.add(bestExam.getId()+":"+best.getPeriod().getIndex());
            return new ExamSimpleNeighbour(best);
        } else {
            for (Enumeration e=model.getPeriods().elements();e.hasMoreElements();) {
                ExamPeriod period = (ExamPeriod)e.nextElement();
                if (!bestExam.isAvailable(period)) continue;
                ExamPlacement placement = new ExamPlacement(bestExam, period, null);
                if (best==null || best.getTimeCost()>placement.getTimeCost()) {
                    Set rooms = bestExam.findBestAvailableRooms(period);
                    if (rooms!=null)
                        best = new ExamPlacement(bestExam, period, rooms);
                }
            }
            if (best!=null) {
                if (sLog.isDebugEnabled()) {
                    Set conf = bestExam.getStudentConflicts(best.getPeriod());
                    if (!conf.isEmpty())
                        sLog.debug("        -- allow all student conflicts between "+bestExam+" and "+conf);
                }
                bestExam.allowAllStudentConflicts(best.getPeriod());
                iAssignments.add(bestExam.getId()+":"+best.getPeriod().getIndex());
                return new ExamSimpleNeighbour(best);
            }
        }
        return (iCheckLocalOptimality?checkLocalOptimality(model):null);
    }
    
}