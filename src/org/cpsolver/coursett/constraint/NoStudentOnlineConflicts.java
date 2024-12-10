package org.cpsolver.coursett.constraint;

import java.util.Set;

import org.cpsolver.coursett.criteria.additional.StudentOnlineConflict;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.RoomLocation;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.util.DataProperties;

/**
 * An experimental global constraints that prohibits cases where a student has an online and in-person
 * class on the same day. Online classes are identified by a regular expression matching the room name
 * and set in the General.OnlineRoom parameter (defaults to (?i)ONLINE|). Classes without a 
 * room are considered online when the General.OnlineRoom parameter matches a blank string.
 * If a class has multiple rooms, all rooms must be online for the class to be considered online. 
 * See {@link StudentOnlineConflict} criterion for a soft variant.
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
public class NoStudentOnlineConflicts extends GlobalConstraint<Lecture, Placement>{
    private String iOnlineRoom = null;
    
    @Override
    public void setModel(Model<Lecture, Placement> model) {
        super.setModel(model);
        if (model != null && model instanceof TimetableModel) {
            DataProperties config = ((TimetableModel)model).getProperties();
            iOnlineRoom = config.getProperty("General.OnlineRoom", "(?i)ONLINE|");
        }
    }

    @Override
    public void computeConflicts(Assignment<Lecture, Placement> assignment, Placement placement, Set<Placement> conflicts) {
        Lecture lecture = placement.variable();
        for (JenrlConstraint jenrl: lecture.jenrlConstraints()) {
            if (jenrl.getJenrl() > 0l) {
                Placement other = assignment.getValue(jenrl.another(lecture));
                if (isConsistent(placement, other))
                    conflicts.add(other);
            }
        }
    }
    
    @Override
    public boolean inConflict(Assignment<Lecture, Placement> assignment, Placement placement) {
        Lecture lecture = placement.variable();
        for (JenrlConstraint jenrl: lecture.jenrlConstraints()) {
            if (jenrl.getJenrl() > 0l && isConsistent(placement, assignment.getValue(jenrl.another(lecture))))
                return true;
        }
        return false;
    }

    @Override
    public boolean isConsistent(Placement p1, Placement p2) {
        if (p1 == null || p2 == null) {
            // at least one class is not assigned > not a problem
            return false;
        } else if (p1.getTimeLocation().shareDays(p2.getTimeLocation()) && p1.getTimeLocation().shareWeeks(p2.getTimeLocation())) {
            return isOnline(p1) != isOnline(p2);
        } else {
            // different days > not a problem
            return false;
        }
    }
    
    protected boolean isOnline(Placement p) {
        if (iOnlineRoom == null) return false;
        // no room -- General.OnlineRoom must allow for a blank string
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
    
    @Override
    public String getName() {
        return "No Student Online Conflicts";
    }
    
    @Override
    public String toString() {
        return "No Student Online Conflicts";
    }
}
