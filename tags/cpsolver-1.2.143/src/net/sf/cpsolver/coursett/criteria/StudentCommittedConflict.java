package net.sf.cpsolver.coursett.criteria;

import java.util.Collection;
import java.util.Set;

import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Student committed conflicts. This criterion counts student conflicts between pairs of classes where
 * one the classes is committed (i.e., fixed in time and room, belonging to another problem).
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
public class StudentCommittedConflict extends StudentConflict {

    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.CommitedStudentConflictWeight", 1.0);
    }

    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.NrCommitedStudConfsWeight";
    }
    
    @Override
    public boolean isApplicable(Lecture l1, Lecture l2) {
        return !ignore(l1, l2) && committed(l1, l2); // only committed student conflicts
    }


    @Override
    public boolean inConflict(Placement p1, Placement p2) {
        return !ignore(p1, p2) && committed(p1, p2) && super.inConflict(p1, p2);
    }
        
    @Override
    public double[] getBounds(Collection<Lecture> variables) {
        double[] bounds = super.getBounds(variables);
        for (Lecture lecture: variables) {
            Double max = null;
            for (Placement placement: lecture.values()) {
                if (max == null) { max = new Double(lecture.getCommitedConflicts(placement)); continue; }
                max = Math.max(max, lecture.getCommitedConflicts(placement));
            }
            if (max != null) bounds[0] += max;
        }
        return bounds;
    }
    
    @Override
    public double getValue(Placement value, Set<Placement> conflicts) {
        double ret = super.getValue(value, conflicts);
        ret += value.variable().getCommitedConflicts(value);
        if (iIncludeConflicts && conflicts != null)
            for (Placement conflict: conflicts)
                ret -= value.variable().getCommitedConflicts(conflict);
        return ret;
    }
    
    @Override
    public double getValue(Collection<Lecture> variables) {
        double ret = super.getValue(variables);
        for (Lecture lect: variables)
            if (lect.getAssignment() != null)
                ret += lect.getCommitedConflicts(lect.getAssignment());
        return Math.round(ret);
    }
}
