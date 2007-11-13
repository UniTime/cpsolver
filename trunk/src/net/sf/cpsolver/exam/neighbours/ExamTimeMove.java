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

/**
 * A new period is selected for a randomly selected exam. It tries to use the current
 * set of rooms, if it is possible (exam is assigned, rooms are available and
 * not used during the new period). Otherwise, rooms are selected using 
 * {@link Exam#findBestAvailableRooms(ExamPeriod)}. 
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
public class ExamTimeMove implements NeighbourSelection {
    private boolean iCheckStudentConflicts = false;
    
    /**
     * Constructor
     * @param properties problem properties
     */
    public ExamTimeMove(DataProperties properties) {
        iCheckStudentConflicts = properties.getPropertyBoolean("ExamTimeMove.CheckStudentConflicts", iCheckStudentConflicts);
    }
    
    /**
     * Initialization
     */
    public void init(Solver solver) {}
    
    /**
     * Select an exam randomly,
     * select an available period randomly (if it is not assigned), 
     * use rooms if possible, select rooms using {@link Exam#findBestAvailableRooms(ExamPeriod)} if not (exam is unassigned, a room is not available or used).
     */
    public Neighbour selectNeighbour(Solution solution) {
        ExamModel model = (ExamModel)solution.getModel();
        Exam exam = (Exam)ToolBox.random(model.variables());
        ExamPlacement placement = (ExamPlacement)exam.getAssignment();
        ExamPeriod period = (ExamPeriod)ToolBox.random(model.getPeriods());
        if (placement!=null && placement.getPeriod().equals(period)) return null;
        if (!exam.isAvailable(period)) return null;
        if (iCheckStudentConflicts && exam.countStudentConflicts(period)>0) return null;
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
        Set rooms = exam.findBestAvailableRooms(period);
        if (rooms==null) return null;
        return new ExamSimpleNeighbour(new ExamPlacement(exam, period, rooms));
    }
}