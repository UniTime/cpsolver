package net.sf.cpsolver.coursett.constraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.cpsolver.coursett.Constants;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Minimize number of used groups of time within a set of classes. <br>
 * <br>
 * 
 * This constraint implements the following distribution/group constraints: <br>
 * <br>
 * 
 * MIN_GRUSE(10x1h) (Minimize Use Of 1h Groups)<br>
 * Minimize number of groups of time that are used by the given classes. The
 * time is spread into the following 10 groups of one hour: 7:30a-8:30a,
 * 8:30a-9:30a, 9:30a-10:30a, ... 4:30p-5:30p. <br>
 * <br>
 * 
 * MIN_GRUSE(5x2h) (Minimize Use Of 2h Groups)<br>
 * Minimize number of groups of time that are used by the given classes. The
 * time is spread into the following 5 groups of two hours: 7:30a-9:30a,
 * 9:30a-11:30a, 11:30a-1:30p, 1:30p-3:30p, 3:30p-5:30p. <br>
 * <br>
 * 
 * MIN_GRUSE(3x3h) (Minimize Use Of 3h Groups)<br>
 * Minimize number of groups of time that are used by the given classes. The
 * time is spread into the following 3 groups: 7:30a-10:30a, 10:30a-2:30p,
 * 2:30p-5:30p. <br>
 * <br>
 * 
 * MIN_GRUSE(2x5h) (Minimize Use Of 5h Groups)<br>
 * Minimize number of groups of time that are used by the given classes. The
 * time is spread into the following 2 groups: 7:30a-12:30a, 12:30a-5:30p.
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
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

public class MinimizeNumberOfUsedGroupsOfTime extends Constraint<Lecture, Placement> implements WeakeningConstraint {
    private int iUnassignmentsToWeaken = 250;
    private long iUnassignment = 0;
    private int iLimit = 1;
    private GroupOfTime iGroupsOfTime[];
    private HashSet<Placement> iUsage[];
    private boolean iEnabled = false;

    private String iName = null;

    public static GroupOfTime[] sGroups2of5h = new GroupOfTime[] {
            new GroupOfTime(Constants.time2slot(7, 30), Constants.time2slot(12, 30), Constants.DAY_CODE_ALL),
            new GroupOfTime(Constants.time2slot(12, 30), Constants.time2slot(17, 30), Constants.DAY_CODE_ALL), };
    public static GroupOfTime[] sGroups3of3h = new GroupOfTime[] {
            new GroupOfTime(Constants.time2slot(7, 30), Constants.time2slot(10, 30), Constants.DAY_CODE_ALL),
            new GroupOfTime(Constants.time2slot(10, 30), Constants.time2slot(14, 30), Constants.DAY_CODE_ALL),
            new GroupOfTime(Constants.time2slot(14, 30), Constants.time2slot(17, 30), Constants.DAY_CODE_ALL) };
    public static GroupOfTime[] sGroups5of2h = new GroupOfTime[] {
            new GroupOfTime(Constants.time2slot(7, 30), Constants.time2slot(9, 30), Constants.DAY_CODE_ALL),
            new GroupOfTime(Constants.time2slot(9, 30), Constants.time2slot(11, 30), Constants.DAY_CODE_ALL),
            new GroupOfTime(Constants.time2slot(11, 30), Constants.time2slot(13, 30), Constants.DAY_CODE_ALL),
            new GroupOfTime(Constants.time2slot(13, 30), Constants.time2slot(15, 30), Constants.DAY_CODE_ALL),
            new GroupOfTime(Constants.time2slot(15, 30), Constants.time2slot(17, 30), Constants.DAY_CODE_ALL) };
    public static GroupOfTime[] sGroups10of1h = new GroupOfTime[] {
            new GroupOfTime(Constants.time2slot(7, 30), Constants.time2slot(8, 30), Constants.DAY_CODE_ALL),
            new GroupOfTime(Constants.time2slot(8, 30), Constants.time2slot(9, 30), Constants.DAY_CODE_ALL),
            new GroupOfTime(Constants.time2slot(9, 30), Constants.time2slot(10, 30), Constants.DAY_CODE_ALL),
            new GroupOfTime(Constants.time2slot(10, 30), Constants.time2slot(11, 30), Constants.DAY_CODE_ALL),
            new GroupOfTime(Constants.time2slot(11, 30), Constants.time2slot(12, 30), Constants.DAY_CODE_ALL),
            new GroupOfTime(Constants.time2slot(12, 30), Constants.time2slot(13, 30), Constants.DAY_CODE_ALL),
            new GroupOfTime(Constants.time2slot(13, 30), Constants.time2slot(14, 30), Constants.DAY_CODE_ALL),
            new GroupOfTime(Constants.time2slot(14, 30), Constants.time2slot(15, 30), Constants.DAY_CODE_ALL),
            new GroupOfTime(Constants.time2slot(15, 30), Constants.time2slot(16, 30), Constants.DAY_CODE_ALL),
            new GroupOfTime(Constants.time2slot(16, 30), Constants.time2slot(17, 30), Constants.DAY_CODE_ALL) };

    @SuppressWarnings("unchecked")
    public MinimizeNumberOfUsedGroupsOfTime(DataProperties config, String name, GroupOfTime[] groupsOfTime) {
        iGroupsOfTime = groupsOfTime;
        iUnassignmentsToWeaken = config.getPropertyInt("MinimizeNumberOfUsedGroupsOfTime.Unassignments2Weaken",
                iUnassignmentsToWeaken);
        iName = name;
        iUsage = new HashSet[iGroupsOfTime.length];
        for (int i = 0; i < iUsage.length; i++)
            iUsage[i] = new HashSet<Placement>();
    }

    public int currentUsage() {
        int ret = 0;
        for (int i = 0; i < iUsage.length; i++)
            if (!iUsage[i].isEmpty())
                ret++;
        return ret;
    }

    public void weaken() {
        iUnassignment++;
        if (iUnassignmentsToWeaken > 0 && iUnassignment % iUnassignmentsToWeaken == 0)
            iLimit++;
    }

    public boolean isOverLimit(Placement placement) {
        return getOverLimit(placement) > 0;
    }

    public int getOverLimit(Placement placement) {
        if (!iEnabled)
            return 0; // not enabled
        if (iUnassignmentsToWeaken == 0)
            return 0; // not working

        Lecture lecture = placement.variable();
        TimeLocation time = placement.getTimeLocation();

        if (lecture.isCommitted())
            return 0; // commited class

        int usage = 0;
        for (int i = 0; i < iGroupsOfTime.length; i++) {
            GroupOfTime groupOfTime = iGroupsOfTime[i];
            if (!iUsage[i].isEmpty() || groupOfTime.overlap(time))
                usage++;
        }

        return usage - iLimit;
    }

    public int estimateLimit() {
        int nrSlotsUsed = 0;
        int minSlotsUsed = 0;
        boolean firstLecture = true;
        for (Lecture lecture : variables()) {
            boolean first = true;
            int minSlotsUsedThisLecture = 0;
            for (TimeLocation time : lecture.timeLocations()) {
                int min = 0;
                for (int i = 0; i < iGroupsOfTime.length; i++) {
                    if (iGroupsOfTime[i].overlap(time))
                        min++;
                }
                if (first) {
                    nrSlotsUsed += time.getLength() * time.getNrMeetings();
                    minSlotsUsedThisLecture = min;
                    first = false;
                } else {
                    minSlotsUsedThisLecture = Math.min(minSlotsUsedThisLecture, min);
                }
            }
            if (firstLecture) {
                minSlotsUsed = minSlotsUsedThisLecture;
                firstLecture = false;
            } else {
                minSlotsUsed = Math.min(minSlotsUsed, minSlotsUsedThisLecture);
            }
        }
        return Math.max(Math.max(1, (int) Math.ceil(((double) nrSlotsUsed) / iGroupsOfTime[0].size())), minSlotsUsed);
    }

    @Override
    public void computeConflicts(Placement placement, Set<Placement> conflicts) {
        int overLimit = getOverLimit(placement);
        if (overLimit > 0) {
            TimeLocation time = placement.getTimeLocation();

            List<List<Placement>> adepts = new ArrayList<List<Placement>>();
            for (int i = 0; i < iGroupsOfTime.length; i++) {
                GroupOfTime groupOfTime = iGroupsOfTime[i];
                HashSet<Placement> usage = iUsage[i];
                if (groupOfTime.overlap(time) || usage.isEmpty())
                    continue;
                boolean canUnassign = true;
                List<Placement> placementsToUnassign = new ArrayList<Placement>(usage.size());
                for (Placement p : usage) {
                    Lecture l = p.variable();
                    if (l.isCommitted()) {
                        canUnassign = false;
                        break;
                    }
                    if (!conflicts.contains(p))
                        placementsToUnassign.add(p);
                }
                if (!canUnassign)
                    continue;
                adepts.add(placementsToUnassign);
            }
            if (adepts.size() < overLimit) {
                conflicts.add(placement);
            } else {
                Collections.sort(adepts, new Comparator<List<Placement>>() {
                    public int compare(List<Placement> c1, List<Placement> c2) {
                        return Double.compare(c1.size(), c2.size());
                    }
                });
                for (int i = 0; i < overLimit; i++) {
                    conflicts.addAll(adepts.get(i));
                }
            }
        }
    }

    @Override
    public boolean inConflict(Placement placement) {
        return isOverLimit(placement);
    }

    @Override
    public boolean isConsistent(Placement value1, Placement value2) {
        return (isOverLimit(value1) || isOverLimit(value2));
    }

    @Override
    public void assigned(long iteration, Placement placement) {
        super.assigned(iteration, placement);
        TimeLocation time = placement.getTimeLocation();
        for (int i = 0; i < iGroupsOfTime.length; i++) {
            GroupOfTime groupOfTime = iGroupsOfTime[i];
            HashSet<Placement> usage = iUsage[i];
            if (groupOfTime.overlap(time))
                usage.add(placement);
        }
    }

    @Override
    public void unassigned(long iteration, Placement placement) {
        super.unassigned(iteration, placement);
        TimeLocation time = placement.getTimeLocation();
        for (int i = 0; i < iGroupsOfTime.length; i++) {
            GroupOfTime groupOfTime = iGroupsOfTime[i];
            HashSet<Placement> usage = iUsage[i];
            if (groupOfTime.overlap(time))
                usage.remove(placement);
        }
    }

    public String getConstraintName() {
        return "MIN_GRUSE(" + iName + ")";
    }

    @Override
    public String getName() {
        return "Minimize number of used groups of time (" + iName + ")";
    }

    public void setEnabled(boolean enabled) {
        iEnabled = enabled;
        iLimit = Math.max(currentUsage(), estimateLimit());
    }

    public boolean isEnabled() {
        return iEnabled;
    }

    private static class GroupOfTime {
        private int iStartSlot = 0;
        private int iEndSlot = 0;
        private int iDays = 0;

        public GroupOfTime(int startSlot, int endSlot, int days) {
            iStartSlot = startSlot;
            iEndSlot = endSlot;
            iDays = days;
        }

        public int getStartSlot() {
            return iStartSlot;
        }

        public int getEndSlot() {
            return iEndSlot;
        }

        public int getDays() {
            return iDays;
        }

        public int nrDays() {
            int ret = 0;
            for (int i = 0; i < Constants.DAY_CODES.length; i++) {
                if ((getDays() & Constants.DAY_CODES[i]) != 0)
                    ret++;
            }
            return ret;
        }

        public int size() {
            return (getEndSlot() - getStartSlot()) * nrDays();
        }

        public boolean overlap(TimeLocation timeLocation) {
            if ((timeLocation.getDayCode() & iDays) == 0)
                return false;
            int end = Math.min(iEndSlot, timeLocation.getStartSlot() + timeLocation.getLength());
            int start = Math.max(iStartSlot, timeLocation.getStartSlot());
            int nrSharedSlots = (end < start ? 0 : end - start);
            return (nrSharedSlots > 0);
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Minimize Use Of "
                + (Constants.SLOT_LENGTH_MIN * (iGroupsOfTime[0].getEndSlot() - iGroupsOfTime[0].getStartSlot()))
                + "min Groups between ");
        for (Iterator<Lecture> e = variables().iterator(); e.hasNext();) {
            Lecture v = e.next();
            sb.append(v.getName());
            if (e.hasNext())
                sb.append(", ");
        }
        return sb.toString();
    }

}
