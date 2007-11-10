package net.sf.cpsolver.exam.neighbours;

import java.util.Iterator;
import java.util.Set;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPeriod;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamRoom;
import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;

public class ExamTimeMove implements NeighbourSelection {
    
    public ExamTimeMove(DataProperties properties) {
    }
    
    public void init(Solver solver) {}
    
    public Neighbour selectNeighbour(Solution solution) {
        ExamModel model = (ExamModel)solution.getModel();
        Exam exam = (Exam)ToolBox.random(model.variables());
        ExamPlacement placement = (ExamPlacement)exam.getAssignment();
        ExamPeriod period = (ExamPeriod)ToolBox.random(model.getPeriods());
        if (placement!=null && placement.getPeriod().equals(period)) return null;
        if (!exam.isAvailable(period)) return null;
        if (placement!=null) {
            boolean ok = true;
            for (Iterator i=placement.getRooms().iterator();i.hasNext();) {
                ExamRoom room = (ExamRoom)i.next();
                if (!room.isAvailable(period) || room.getPlacement(period)!=null) {
                    ok = false; break;
                }
            }
            if (ok)
                return new ExamSimpleNeighbour(new ExamPlacement(exam, period, placement.getRooms()));
        }
        Set rooms = exam.findRooms(period);
        if (rooms==null) return null;
        return new ExamSimpleNeighbour(new ExamPlacement(exam, period, rooms));
    }
}