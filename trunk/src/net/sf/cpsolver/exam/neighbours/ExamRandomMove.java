package net.sf.cpsolver.exam.neighbours;

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
 * A period is selected randomly for a randomly selected exam. Rooms are
 * computed using {@link Exam#findBestAvailableRooms(ExamPeriodPlacement)}. <br>
 * <br>
 * 
 * @version ExamTT 1.2 (Examination Timetabling)<br>
 *          Copyright (C) 2008 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 */
public class ExamRandomMove implements NeighbourSelection<Exam, ExamPlacement> {
    private boolean iCheckStudentConflicts = false;
    private boolean iCheckDistributionConstraints = true;

    /**
     * Constructor
     * 
     * @param properties
     *            problem properties
     */
    public ExamRandomMove(DataProperties properties) {
        iCheckStudentConflicts = properties.getPropertyBoolean("ExamRandomMove.CheckStudentConflicts",
                iCheckStudentConflicts);
        iCheckDistributionConstraints = properties.getPropertyBoolean("ExamRandomMove.CheckDistributionConstraints",
                iCheckDistributionConstraints);
    }

    /**
     * Initialization
     */
    public void init(Solver<Exam, ExamPlacement> solver) {
    }

    /**
     * Select an exam randomly, select an available period randomly (from
     * {@link Exam#getPeriodPlacements()}), select rooms using
     * {@link Exam#findBestAvailableRooms(ExamPeriodPlacement)}.
     */
    public Neighbour<Exam, ExamPlacement> selectNeighbour(Solution<Exam, ExamPlacement> solution) {
        ExamModel model = (ExamModel) solution.getModel();
        Exam exam = ToolBox.random(model.variables());
        int px = ToolBox.random(exam.getPeriodPlacements().size());
        for (int p = 0; p < exam.getPeriodPlacements().size(); p++) {
            ExamPeriodPlacement period = exam.getPeriodPlacements().get((p + px) % exam.getPeriodPlacements().size());
            if (iCheckStudentConflicts && exam.countStudentConflicts(period) > 0)
                continue;
            if (iCheckDistributionConstraints && !exam.checkDistributionConstraints(period))
                continue;
            Set<ExamRoomPlacement> rooms = exam.findBestAvailableRooms(period);
            if (rooms == null)
                continue;
            return new ExamSimpleNeighbour(new ExamPlacement(exam, period, rooms));
        }
        return null;
    }
}