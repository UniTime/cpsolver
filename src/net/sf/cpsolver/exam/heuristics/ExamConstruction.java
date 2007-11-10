package net.sf.cpsolver.exam.heuristics;

import java.util.Enumeration;
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

public class ExamConstruction implements NeighbourSelection {
    private static Logger sLog = Logger.getLogger(ExamConstruction.class);

    public ExamConstruction(DataProperties properties) {
    }
    
    public void init(Solver solver) {
    }
    
    public Neighbour checkLocalConsistency(ExamModel model) {
        for (Enumeration e=model.assignedVariables().elements();e.hasMoreElements();) {
            Exam exam = (Exam)e.nextElement();
            ExamPlacement current = (ExamPlacement)exam.getAssignment(); 
            if (exam.hasPreAssignedPeriod()) continue;
            if (current.getTimeCost()<=0) continue;
            ExamPlacement best = null;
            for (Enumeration f=model.getPeriods().elements();f.hasMoreElements();) {
                ExamPeriod period = (ExamPeriod)f.nextElement();
                if (!exam.isAvailable(period)) continue;
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
    
    public Neighbour selectNeighbour(Solution solution) {
        ExamModel model = (ExamModel)solution.getModel();
        if (model.unassignedVariables().isEmpty()) return null;//checkLocalConsistency(model);
        Exam bestExam = null;
        for (Enumeration e=model.unassignedVariables().elements();e.hasMoreElements();) {
            Exam exam = (Exam)e.nextElement();
            if (bestExam==null || exam.compareTo(bestExam)<0)
                bestExam = exam;
        }
        ExamPlacement best = null;
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
        if (best!=null) return new ExamSimpleNeighbour(best);
        return checkLocalConsistency(model);
    }
    
}