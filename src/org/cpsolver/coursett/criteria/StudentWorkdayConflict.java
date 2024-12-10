package org.cpsolver.coursett.criteria;

import java.util.Collection;
import java.util.Map;

import org.cpsolver.coursett.constraint.JenrlConstraint;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.coursett.model.TimetableModel;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;

/**
 * Student work-day conflicts. This criterion counts student work-day conflicts between classes.
 * A work-day conflict occurs when two classes that are attended by the same student (or students)
 * are placed on the same day (or days) in times that are too far a part. The combinations of classes
 * that share students are maintained by {@link JenrlConstraint}.
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
public class StudentWorkdayConflict extends StudentConflict {
    
    public int getStudentWorkDayLimit() {
        return (getModel() == null ? -1 : ((TimetableModel)getModel()).getStudentWorkDayLimit());
    }

    @Override
    public boolean inConflict(Placement p1, Placement p2) {
        return workday(getStudentWorkDayLimit(), p1, p2);
    }
    
    @Override
    public boolean isApplicable(Lecture l1, Lecture l2) {
        return l1 != null && l2 != null && !ignore(l1, l2) && applicable(l1, l2); // all student conflicts (including committed)
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return config.getPropertyDouble("Comparator.WorkDayStudentConflictWeight", 0.2);
    }
    
    @Override
    public String getPlacementSelectionWeightName() {
        return "Placement.NrWorkDayStudConfsWeight";
    }
    
    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info) {
        super.getInfo(assignment, info);
        double conf = getValue(assignment);
        if (conf > 0.0)
            info.put("Workday student conflicts", sDoubleFormat.format(conf));
    }
    
    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info, Collection<Lecture> variables) {
        super.getInfo(assignment, info, variables);
        double conf = getValue(assignment, variables);
        if (conf > 0.0)
            info.put("Workday student conflicts", sDoubleFormat.format(conf));
    }    
}
