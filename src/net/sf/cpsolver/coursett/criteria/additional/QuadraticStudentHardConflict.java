package net.sf.cpsolver.coursett.criteria.additional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.sf.cpsolver.coursett.constraint.JenrlConstraint;
import net.sf.cpsolver.coursett.criteria.StudentHardConflict;
import net.sf.cpsolver.coursett.model.Lecture;
import net.sf.cpsolver.coursett.model.Placement;
import net.sf.cpsolver.coursett.model.TimetableModel;
import net.sf.cpsolver.ifs.util.DataProperties;

/**
 * Quadratic student conflicts. Same as {@link StudentHardConflict}, however,
 * student joint enrollments are squared (1 conflict counts as 1, 2 as 4, 3 as 9, etc.).
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
public class QuadraticStudentHardConflict extends QuadraticStudentConflict {

    @Override
    public boolean inConflict(Placement p1, Placement p2) {
        return hard(p1, p2) && super.inConflict(p1, p2);
    }

    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.HardStudentConflictWeight", 5.0);
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.NrHardStudConfsWeight";
    }
    
    @Override
    public double[] getBounds() {
        double[] bounds = { 0.0, 0.0 };
        for (JenrlConstraint jenrl: ((TimetableModel)getModel()).getJenrlConstraints())
            if (hard(jenrl.first(), jenrl.second()))
                bounds[0] += jenrl(jenrl);
        return bounds;
    }
    
    @Override
    public double[] getBounds(Collection<Lecture> variables) {
        double[] bounds = { 0.0, 0.0 };
        Set<JenrlConstraint> constraints = new HashSet<JenrlConstraint>();
        for (Lecture lect: variables) {
            if (lect.getAssignment() == null) continue;
            for (JenrlConstraint jenrl: lect.jenrlConstraints()) {
                if (constraints.add(jenrl) && hard(jenrl.first(), jenrl.second()))
                    bounds[0] += jenrl(jenrl);
            }
        }
        return bounds;
    }

}
