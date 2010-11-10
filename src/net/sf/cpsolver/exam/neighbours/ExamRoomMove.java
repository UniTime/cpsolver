package net.sf.cpsolver.exam.neighbours;

import java.util.ArrayList;
import java.util.List;
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
 * Try to swap a room between two exams. An exam is selected randomly, a
 * different (available) room is randomly selected for the exam -- the exam is
 * assigned into the new room (if the room is used, it tries to swap the rooms
 * between the selected exam and the one that is using it). If an exam is
 * assigned into two or more rooms, only one room is swapped at a time. <br>
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
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 */
public class ExamRoomMove implements NeighbourSelection<Exam, ExamPlacement> {
    private boolean iCheckStudentConflicts = false;
    private boolean iCheckDistributionConstraints = true;

    /**
     * Constructor
     * 
     * @param properties
     *            problem properties
     */
    public ExamRoomMove(DataProperties properties) {
        iCheckStudentConflicts = properties.getPropertyBoolean("ExamRoomMove.CheckStudentConflicts",
                iCheckStudentConflicts);
        iCheckDistributionConstraints = properties.getPropertyBoolean("ExamRoomMove.CheckDistributionConstraints",
                iCheckDistributionConstraints);
    }

    /**
     * Initialization
     */
    public void init(Solver<Exam, ExamPlacement> solver) {
    }

    /**
     * Select an exam randomly, select an available period randomly (if it is
     * not assigned, from {@link Exam#getPeriodPlacements()}), select rooms
     * using {@link Exam#findRoomsRandom(ExamPeriodPlacement)}
     */
    public Neighbour<Exam, ExamPlacement> selectNeighbour(Solution<Exam, ExamPlacement> solution) {
        ExamModel model = (ExamModel) solution.getModel();
        Exam exam = ToolBox.random(model.variables());
        if (exam.getMaxRooms() <= 0)
            return null;
        ExamPlacement placement = exam.getAssignment();
        ExamPeriodPlacement period = (placement != null ? placement.getPeriodPlacement()
                : (ExamPeriodPlacement) ToolBox.random(exam.getPeriodPlacements()));
        if (iCheckStudentConflicts && placement == null && exam.countStudentConflicts(period) > 0)
            return null;
        if (iCheckDistributionConstraints && placement == null && !exam.checkDistributionConstraints(period))
            return null;
        Set<ExamRoomPlacement> rooms = (placement != null ? placement.getRoomPlacements() : exam
                .findBestAvailableRooms(period));
        if (rooms == null || rooms.isEmpty())
            return null;
        if (placement == null)
            placement = new ExamPlacement(exam, period, rooms);
        List<ExamRoomPlacement> roomVect = new ArrayList<ExamRoomPlacement>(rooms);
        int rx = ToolBox.random(roomVect.size());
        for (int r = 0; r < roomVect.size(); r++) {
            ExamRoomPlacement current = roomVect.get((r + rx) % roomVect.size());
            int mx = ToolBox.random(exam.getRoomPlacements().size());
            for (int m = 0; m < exam.getRoomPlacements().size(); m++) {
                ExamRoomPlacement swap = exam.getRoomPlacements().get((m + mx) % exam.getRoomPlacements().size());
                ExamRoomSwapNeighbour n = new ExamRoomSwapNeighbour(placement, current, swap);
                if (n.canDo())
                    return n;
            }
        }
        rooms = exam.findRoomsRandom(period);
        if (rooms == null)
            return null;
        return new ExamSimpleNeighbour(new ExamPlacement(exam, period, rooms));
    }
}