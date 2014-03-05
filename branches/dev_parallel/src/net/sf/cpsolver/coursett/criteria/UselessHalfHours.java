package net.sf.cpsolver.coursett.criteria;

import java.util.List;

import net.sf.cpsolver.coursett.Constants;
import net.sf.cpsolver.coursett.constraint.RoomConstraint;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Useless half-hours. This criterion counts cases when there is an empty half hour in a room.
 * Such half-hours should be generally avoided as usually any class takes more than half an hour.  
 * <br>
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2011 Tomas Muller<br>
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
public class UselessHalfHours extends BrokenTimePatterns {

    @Override
    public double getWeightDefault(DataProperties config) {
        return Constants.sPreferenceLevelStronglyDiscouraged * config.getPropertyDouble("Comparator.UselessSlotWeight", 0.1);
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.UselessSlotsWeight";
    }
    
    @Override
    protected double penalty(RoomConstraint rc) {
        return countUselessSlotsHalfHours(rc);
    }

    @Override
   protected double penalty(RoomConstraint rc, Placement value) {
        return countUselessSlotsHalfHours(rc, value);
    }
    
    private static boolean isEmpty(RoomConstraint rc, int slot, Placement placement) {
        List<Placement> assigned = rc.getResource(slot);
        return assigned.isEmpty() || (placement != null && assigned.size() == 1 && assigned.get(0).variable().equals(placement.variable()));
    }

    
    private static boolean isUselessBefore(RoomConstraint rc, int slot, Placement placement) {
        int s = slot % Constants.SLOTS_PER_DAY;
        if (s - 1 < 0 || s + 6 >= Constants.SLOTS_PER_DAY)
            return false;
        return (!isEmpty(rc, slot - 1, placement) &&
                isEmpty(rc, slot + 0, placement) &&
                isEmpty(rc, slot + 1, placement) &&
                isEmpty(rc, slot + 2, placement) &&
                isEmpty(rc, slot + 3, placement) &&
                isEmpty(rc, slot + 4, placement) &&
                isEmpty(rc, slot + 5, placement) &&
                isEmpty(rc, slot + 6, placement));
    }

    private static boolean isUselessAfter(RoomConstraint rc, int slot, Placement placement) {
        int s = slot % Constants.SLOTS_PER_DAY;
        if (s - 1 < 0 || s + 6 >= Constants.SLOTS_PER_DAY)
            return false;
        return (isEmpty(rc, slot - 1, placement) &&
                isEmpty(rc, slot + 0, placement) &&
                isEmpty(rc, slot + 1, placement) &&
                isEmpty(rc, slot + 2, placement) &&
                isEmpty(rc, slot + 3, placement) &&
                isEmpty(rc, slot + 4, placement) &&
                isEmpty(rc, slot + 5, placement) &&
                !isEmpty(rc, slot + 6, placement));
    }
    
    private static boolean isUseless(RoomConstraint rc, int slot, Placement placement) {
        int s = slot % Constants.SLOTS_PER_DAY;
        if (s - 1 < 0 || s + 6 >= Constants.SLOTS_PER_DAY)
            return false;
        return (!isEmpty(rc, slot - 1, placement) &&
                isEmpty(rc, slot + 0, placement) &&
                isEmpty(rc, slot + 1, placement) &&
                isEmpty(rc, slot + 2, placement) &&
                isEmpty(rc, slot + 3, placement) &&
                isEmpty(rc, slot + 4, placement) &&
                isEmpty(rc, slot + 5, placement) &&
                !isEmpty(rc, slot + 6, placement));
    }

    /** Number of useless half hours for this room */
    protected static int countUselessSlotsHalfHours(RoomConstraint rc, Placement placement) {
        int ret = 0;
        TimeLocation time = placement.getTimeLocation();
        int slot = time.getStartSlot() % Constants.SLOTS_PER_DAY;
        int days = time.getDayCode();
        for (int d = 0; d < Constants.NR_DAYS; d++) {
            if ((Constants.DAY_CODES[d] & days) == 0)
                continue;
            if (isUselessBefore(rc, d * Constants.SLOTS_PER_DAY + slot - 6, placement))
                ret ++;
            if (isUselessAfter(rc, d * Constants.SLOTS_PER_DAY + slot + time.getNrSlotsPerMeeting(), placement))
                ret ++;
            if (time.getNrSlotsPerMeeting() == 6 && isUseless(rc, d * Constants.SLOTS_PER_DAY + slot, placement))
                ret --;
        }
        return ret;
    }
    
    private static boolean isUseless(RoomConstraint rc, int slot) {
        int s = slot % Constants.SLOTS_PER_DAY;
        if (s - 1 < 0 || s + 6 >= Constants.SLOTS_PER_DAY)
            return false;
        return (!rc.getResource(slot - 1).isEmpty() &&
                rc.getResource(slot + 0).isEmpty() &&
                rc.getResource(slot + 1).isEmpty() &&
                rc.getResource(slot + 2).isEmpty() &&
                rc.getResource(slot + 3).isEmpty() &&
                rc.getResource(slot + 4).isEmpty() &&
                rc.getResource(slot + 5).isEmpty() &&
                !rc.getResource(slot + 6).isEmpty());
    }

    /** Number of useless slots for this room */
    public static int countUselessSlotsHalfHours(RoomConstraint rc) {
        int ret = 0;
        for (int d = 0; d < Constants.NR_DAYS; d++) {
            for (int s = 0; s < Constants.SLOTS_PER_DAY; s++) {
                int slot = d * Constants.SLOTS_PER_DAY + s;
                if (isUseless(rc, slot))
                    ret++;
            }
        }
        return ret;
    }
}
