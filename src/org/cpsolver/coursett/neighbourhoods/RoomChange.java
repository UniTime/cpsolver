package org.cpsolver.coursett.neighbourhoods;

import java.util.List;

import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.algorithms.neighbourhoods.RandomMove;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.SimpleNeighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;


/**
 * Try to assign a class with a new room. A class is selected randomly, a
 * different (available) room is randomly selected for the class -- the class is
 * assigned into the new room if there is no conflict.
 * <br>
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Iterative Forward Search)<br>
 *          Copyright (C) 2014 Tomas Muller<br>
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
public class RoomChange extends RandomMove<Lecture, Placement> {

    public RoomChange(DataProperties config) {
        super(config);
    }

    @Override
    public Neighbour<Lecture, Placement> selectNeighbour(Solution<Lecture, Placement> solution) {
        TimetableModel model = (TimetableModel)solution.getModel();
        Assignment<Lecture, Placement> assignment = solution.getAssignment();
        int varIdx = ToolBox.random(model.variables().size());
        for (int i = 0; i < model.variables().size(); i++) {
            Lecture lecture = model.variables().get((i + varIdx) % model.variables().size());
            Placement old = assignment.getValue(lecture);
            if (old == null || old.getNrRooms() != 1) continue;

            List<RoomLocation> values = lecture.roomLocations();
            if (values.isEmpty()) continue;

            int valIdx = ToolBox.random(values.size());
            for (int j = 0; j < values.size(); j++) {
                RoomLocation room = values.get((j + valIdx) % values.size());
                if (room.getPreference() > 50) continue;
                if (room.equals(old.getRoomLocation())) continue;
                
                Placement placement = new Placement(lecture, old.getTimeLocation(), room);
                if (placement.isValid() && !model.inConflict(assignment, placement)) {
                    SimpleNeighbour<Lecture, Placement> n = new SimpleNeighbour<Lecture, Placement>(lecture, placement);
                    if (!iHC || n.value(assignment) <= 0) return n;
                }
            }
        }
        return null;
    }
}
