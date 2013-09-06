package net.sf.cpsolver.coursett.criteria.additional;

import java.util.Collection;
import java.util.Map;

import net.sf.cpsolver.coursett.criteria.StudentConflict;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.ifs.criteria.Criterion;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Ignored student conflicts. This criterion counts student conflicts (both overlapping and distance) between classes
 * which are connected by a {@link IgnoredStudentConflict} constraint. This criterion was created mostly for debugging
 * as these student conflicts are to be ignored.
 * <br>
 * 
 * @version CourseTT 1.2 (University Course Timetabling)<br>
 *          Copyright (C) 2013 Tomas Muller<br>
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
public class IgnoredStudentConflict extends StudentConflict {
    
    @Override
    public boolean inConflict(Placement p1, Placement p2) {
        return ignore(p1, p2) && (overlaps(p1, p2) || distance(getMetrics(), p1, p2));
    }
    
    @Override
    public boolean isApplicable(Lecture l1, Lecture l2) {
        return ignore(l1, l2) && applicable(l1, l2);
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.IgnoredStudentConflictWeight", 0.0);
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.NrIgnoredStudConfsWeight";
    }
    
    @Override
    public void getInfo(Map<String, String> info) {
        super.getInfo(info);
        double conf = getValue();
        if (conf > 0.0) {
            Criterion<Lecture, Placement> c = getModel().getCriterion(IgnoredCommittedStudentConflict.class);
            double committed = (c == null ? 0.0 : c.getValue());
            info.put("Ignored student conflicts", sDoubleFormat.format(conf) + (committed > 0.0 ? " [committed: " + sDoubleFormat.format(committed) + "]" : ""));
        }
    }
    
    @Override
    public void getInfo(Map<String, String> info, Collection<Lecture> variables) {
        super.getInfo(info, variables);
        double conf = getValue(variables);
        if (conf > 0.0) {
            Criterion<Lecture, Placement> c = getModel().getCriterion(IgnoredCommittedStudentConflict.class);
            double committed = (c == null ? 0.0 : c.getValue(variables));
            info.put("Ignored student conflicts", sDoubleFormat.format(conf) + (committed > 0.0 ? " [committed: " + sDoubleFormat.format(committed) + "]" : ""));
        }
    }
}
