package net.sf.cpsolver.coursett.criteria;

import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimeLocation;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Student overlapping conflicts. This criterion counts student overlapping conflicts between classes.
 * An overlapping conflict occurs when two classes that are attended by the same student (or students)
 * are overlapping in time (i.e., {@link TimeLocation#hasIntersection(TimeLocation)} is true).
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
public class StudentOverlapConflict extends StudentConflict {
    
    @Override
    public boolean inConflict(Placement p1, Placement p2) {
        return !ignore(p1, p2) && overlaps(p1, p2);
    }
    
    @Override
    public boolean isApplicable(Lecture l1, Lecture l2) {
        return !ignore(l1, l2) && applicable(l1, l2); // all student conflicts (including committed)
    }

    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.StudentConflictWeight", 1.0);
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.NrStudConfsWeight";
    }
}
