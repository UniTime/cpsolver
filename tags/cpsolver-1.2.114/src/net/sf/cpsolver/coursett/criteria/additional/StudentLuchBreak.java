package net.sf.cpsolver.coursett.criteria.additional;

import java.util.Collection;
import java.util.Map;

import net.sf.cpsolver.coursett.criteria.StudentConflict;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.ifs.solver.Solver;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Naive, yet effective approach for modeling student lunch breaks. This criterion
 * is based on {@link StudentConflict} and it creates a conflict whenever there are
 * two classes (that share students) overlapping with the lunch time which are one
 * after the other with a break in between smaller than the requested lunch break.
 * Lunch time is defined by StudentLunch.StartSlot and StudentLunch.EndStart
 * properties (default is 11:00 am - 1:30 pm), with lunch break of at least
 * StudentLunch.Length slots (default is 30 minutes). Such a conflict is weighted
 * by Comparator.StudentLunchWeight, which defaults to Comparator.StudentConflictWeight.
 * 
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

public class StudentLuchBreak extends StudentConflict {
    private int iLunchStart, iLunchEnd, iLunchLength; 
    
    @Override
    public boolean init(Solver<Lecture, Placement> solver) {
        iLunchStart = solver.getProperties().getPropertyInt("StudentLunch.StartSlot", (11 * 60) / 5);
        iLunchEnd = solver.getProperties().getPropertyInt("StudentLunch.EndStart", (13 * 60 + 30) / 5);
        iLunchLength = solver.getProperties().getPropertyInt("StudentLunch.Length", 30 / 5);
        return super.init(solver);
    }


    @Override
    public boolean isApplicable(Lecture l1, Lecture l2) {
        return applicable(l1, l2);
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.StudentLunchWeight", config.getPropertyDouble("Comparator.StudentConflictWeight", 1.0));
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.StudentLunchWeight";
    }

    public boolean nolunch(Placement p1, Placement p2) {
        if (p1 == null || p2 == null || overlaps(p1, p2)) return false;
        if (p1.variable().isCommitted() && p2.variable().isCommitted()) return false;
        TimeLocation t1 = p1.getTimeLocation(), t2 = p2.getTimeLocation();
        if (!t1.shareDays(t2) || !t1.shareWeeks(t2)) return false;
        int s1 = t1.getStartSlot(), s2 = t2.getStartSlot();
        int e1 = t1.getStartSlot() + t1.getNrSlotsPerMeeting(), e2 = t2.getStartSlot() + t2.getNrSlotsPerMeeting();
        if (e1 + iLunchLength > s2 && e2 + iLunchLength > s1 && e1 > iLunchStart && iLunchEnd > s1 && e2 > iLunchStart && iLunchEnd > s2)
            return true;
        return false;
    }
    
    @Override
    public boolean inConflict(Placement p1, Placement p2) {
        return nolunch(p1, p2);
    }
    
    @Override
    public void getInfo(Map<String, String> info) {
        super.getInfo(info);
        double conf = getValue();
        if (conf > 0.0)
            info.put("Student lunch conflicts", String.valueOf(Math.round(conf)));
    }
    
    @Override
    public void getInfo(Map<String, String> info, Collection<Lecture> variables) {
        super.getInfo(info, variables);
        double conf = getValue(variables);
        if (conf > 0.0)
            info.put("Student lunch conflicts", String.valueOf(Math.round(conf)));
    }

}
