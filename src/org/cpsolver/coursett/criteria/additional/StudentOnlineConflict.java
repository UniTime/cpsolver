package org.cpsolver.coursett.criteria.additional;

import java.util.Collection;
import java.util.Map;

import org.cpsolver.coursett.criteria.StudentConflict;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;

/**
 * An experimental criterion that tries to minimize cases where a student has an online and in-person
 * class on the same day. Online classes are identified by a regular expression matching the room name
 * and set in the General.OnlineRoom parameter (defaults to (?i)ONLINE|). Classes without a 
 * room are considered online when the General.OnlineRoom parameter matches a blank string.
 * If a class has multiple rooms, all rooms must be online for the class to be considered online. 
 * The criterion is weighted by the Comparator.StudentOnlineConflictWeight parameter, defaults
 * to one half of the Comparator.StudentConflictWeight.
 * <br>
 * 
 * @author  Tomas Muller
 * @version CourseTT 1.3 (University Course Timetabling)<br>
 *          Copyright (C) 2013 - 2023 Tomas Muller<br>
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
public class StudentOnlineConflict extends StudentConflict {
    private String iOnlineRoom = null;
    
    @Override
    public void configure(DataProperties properties) {   
        super.configure(properties);
        iOnlineRoom = properties.getProperty("StudentConflict.OnlineRoom",
                properties.getProperty("General.OnlineRoom", "(?i)ONLINE|"));
    }
    
    public boolean isOnline(Placement p) {
        // no room -- StudentConflict.OnlineRoom must allow for a blank string
        if (p.getNrRooms() == 0)
            return "".matches(iOnlineRoom);
        // one room -- room name must match StudentConflict.OnlineRoom
        if (p.getNrRooms() == 1)
            return (p.getRoomLocation().getName() != null && p.getRoomLocation().getName().matches(iOnlineRoom));
        // multiple rooms -- all rooms must match StudentConflict.OnlineRoom
        for (RoomLocation r: p.getRoomLocations())
            if (r.getName() == null || !r.getName().matches(iOnlineRoom)) return false;
        return true;
    }
    
    /**
     * Are the two placements at the same day? True when the two placements share days and weeks.
     */
    public boolean shareDays(Placement p1, Placement p2) {
        return p1 != null && p2 != null && p1.getTimeLocation().shareDays(p2.getTimeLocation()) && p1.getTimeLocation().shareWeeks(p2.getTimeLocation());
    }
    
    /**
     * There is a conflict when {@link StudentOnlineConflict#isOnline(Placement)} differs for the two placements
     * and they are placed on the same day ({@link StudentOnlineConflict#shareDays(Placement, Placement)} returns true).
     */
    @Override
    public boolean inConflict(Placement p1, Placement p2) {
        return p1 != null && p2 != null && isOnline(p1) != isOnline(p2) && shareDays(p1, p2);
    }
    
    @Override
    public boolean isApplicable(Lecture l1, Lecture l2) {
        return l1 != null && l2 != null && !ignore(l1, l2) && applicable(l1, l2);
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.StudentOnlineConflictWeight", 0.5 * config.getPropertyDouble("Comparator.StudentConflictWeight", 1.0));
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.StudentOnlineConflictWeight";
    }
    
    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info) {
        super.getInfo(assignment, info);
        double conf = getValue(assignment);
        if (conf > 0.0)
            info.put("Student online conflicts", sDoubleFormat.format(conf));
    }
    
    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info, Collection<Lecture> variables) {
        super.getInfo(assignment, info, variables);
        double conf = getValue(assignment, variables);
        if (conf > 0.0)
            info.put("Student online conflicts", sDoubleFormat.format(conf));
    }
}