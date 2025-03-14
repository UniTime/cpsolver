package org.cpsolver.coursett.criteria.additional;

import java.util.Map;
import java.util.Set;

import org.cpsolver.coursett.criteria.TimetablingCriterion;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Cost for using room(s) that are too big. I.e., a difference between size of the assigned room and the size of the 
 * smallest room in which the class can be placed {@link Lecture#minRoomSize()}.
 * <br><br>
 * A weight for room size penalty can be set by problem property Comparator.RoomSizeWeight.
 * <br><br>
 * The difference function can be made polynomial by using Comparator.RoomSizeFactor parameter
 * (defaults to 1.05). The value of this criteria is then cubed by the power of this room
 * size factor. This is to be able to favor a room swap between two classes at the same time,
 * in which a smaller class takes a smaller room. To do this, set Comparator.RoomSizeFactor to
 * a number bigger than one that is close to one (e.g., 1.05).
 *   
 * <br>
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
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
public class RoomSizePenalty extends TimetablingCriterion {
    private double iRoomSizeFactor = 1.0;

    @Override
    public void configure(DataProperties properties) {   
        super.configure(properties);
        iRoomSizeFactor = properties.getPropertyDouble("Comparator.RoomSizeFactor", 1.05);
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.RoomSizeWeight", 0.001);
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.RoomSizeWeight";
    }
    
    @Override
    public double getValue(Assignment<Lecture, Placement> assignment, Placement value, Set<Placement> conflicts) {
        if (value.variable().getNrRooms() <= 0) return 0.0;
        double size = 0;
        if (value.getRoomLocation() != null)
            size = value.getRoomLocation().getRoomSize();
        else if (value.getRoomLocations() != null) {
            for (RoomLocation room: value.getRoomLocations())
                size += room.getRoomSize();
            size /= value.getNrRooms();
        }
        double diff = size - value.variable().minRoomSize();
        return (diff < 0 ? 0 : Math.pow(diff, iRoomSizeFactor));
    }
    
    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info) {
        if (getValue(assignment) != 0.0)
            info.put(getName(), sDoubleFormat.format(Math.pow(getValue(assignment) / getModel().nrAssignedVariables(assignment), 1.0 / iRoomSizeFactor)));
    }

}
