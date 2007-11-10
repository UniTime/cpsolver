package net.sf.cpsolver.exam.neighbours;

import java.util.Set;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPeriod;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;

public class ExamRoomMove implements NeighbourSelection {
    
    public ExamRoomMove(DataProperties properties) {
    }
    
    public void init(Solver solver) {}
    
    public Neighbour selectNeighbour(Solution solution) {
        ExamModel model = (ExamModel)solution.getModel();
        Exam exam = (Exam)ToolBox.random(model.variables());
        ExamPlacement placement = (ExamPlacement)exam.getAssignment();
        ExamPeriod period = (placement!=null?placement.getPeriod():(ExamPeriod)ToolBox.random(model.getPeriods()));
        Set rooms = exam.findRoomsRandom(period);
        if (rooms==null) return null;
        return new ExamSimpleNeighbour(new ExamPlacement(exam, period, rooms));
    }
}