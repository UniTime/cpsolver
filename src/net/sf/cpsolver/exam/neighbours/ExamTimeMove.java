package net.sf.cpsolver.exam.neighbours;

import java.util.Iterator;
import java.util.Set;

import net.sf.cpsolver.exam.model.Exam;
import net.sf.cpsolver.exam.model.ExamModel;
import net.sf.cpsolver.exam.model.ExamPeriodPlacement;
import net.sf.cpsolver.exam.model.ExamPlacement;
import net.sf.cpsolver.exam.model.ExamRoomPlacement;
import net.sf.cpsolver.ifs.heuristics.NeighbourSelection;
import net.sf.cpsolver.ifs.model.Neighbour;
import net.sf.cpsolver.ifs.solution.Solution;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;
import net.sf.cpsolver.ifs.util.ToolBox;

/**
 * A new period is selected for a randomly selected exam. It tries to use the
 * current set of rooms, if it is possible (exam is assigned, rooms are
 * available and not used during the new period). Otherwise, rooms are selected
 * using {@link Exam#findBestAvailableRooms(ExamPeriodPlacement)}. <br>
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
public class ExamTimeMove implements NeighbourSelection<Exam,ExamPlacement> {
    private boolean iCheckStudentConflicts = false;
    private boolean iCheckDistributionConstraints = true;
    
    /**
     * Constructor
     * @param properties problem properties
     */
    public ExamTimeMove(DataProperties properties) {
        iCheckStudentConflicts = properties.getPropertyBoolean("ExamTimeMove.CheckStudentConflicts", iCheckStudentConflicts);
        iCheckDistributionConstraints = properties.getPropertyBoolean("ExamTimeMove.CheckDistributionConstraints", iCheckDistributionConstraints);
    }
    
    /**
     * Initialization
     */
    @Override
    public void init(Solver<Exam,ExamPlacement> solver) {}
    
    /**
     * Select an exam randomly,
     * select an available period randomly (if it is not assigned), 
     * use rooms if possible, select rooms using {@link Exam#findBestAvailableRooms(ExamPeriodPlacement)} if not (exam is unassigned, a room is not available or used).
     */
    @Override
    public Neighbour<Exam,ExamPlacement> selectNeighbour(Solution<Exam,ExamPlacement> solution) {
        ExamModel model = (ExamModel)solution.getModel();
        Exam exam = ToolBox.random(model.variables());
        ExamPlacement placement = exam.getAssignment();
        int px = ToolBox.random(exam.getPeriodPlacements().size());
        for (int p=0;p<exam.getPeriodPlacements().size();p++) {
            ExamPeriodPlacement period = exam.getPeriodPlacements().get((p+px)%exam.getPeriodPlacements().size());
            if (placement!=null && placement.getPeriod().equals(period)) continue;
            if (iCheckStudentConflicts && exam.countStudentConflicts(period)>0) continue;
            if (iCheckDistributionConstraints && !exam.checkDistributionConstraints(period)) continue;
            if (placement!=null) {
                boolean ok = true;
                for (Iterator<ExamRoomPlacement> i=placement.getRoomPlacements().iterator();i.hasNext();) {
                    ExamRoomPlacement room = i.next();
                    if (!room.isAvailable(period.getPeriod()) || room.getRoom().getPlacement(period.getPeriod())!=null) {
                        ok = false; break;
                    }
                }
                if (ok)
                    return new ExamSimpleNeighbour(new ExamPlacement(exam, period, placement.getRoomPlacements()));
            }
            Set<ExamRoomPlacement> rooms = exam.findBestAvailableRooms(period);
            if (rooms==null) continue;
            return new ExamSimpleNeighbour(new ExamPlacement(exam, period, rooms));
        }
        return null;
    }
}