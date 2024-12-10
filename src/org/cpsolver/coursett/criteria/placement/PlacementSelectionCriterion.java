package org.cpsolver.coursett.criteria.placement;

import java.util.Collection;
import java.util.Map;

import org.cpsolver.coursett.criteria.TimetablingCriterion;
import org.cpsolver.coursett.model.Lecture;
import org.cpsolver.coursett.model.Placement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.util.DataProperties;


/**
 * Abstract class for all criteria that are to be used only in the placement selection
 * (do not have any impact on the overall solution value). Such criterion is for instance
 * the number of hard conflict (values that have to be unassigned before a selected
 * value can be assigned for the problem to remain consistent). 
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
public abstract class PlacementSelectionCriterion extends TimetablingCriterion {

    public PlacementSelectionCriterion() {
        setValueUpdateType(ValueUpdateType.NoUpdate);
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 0.0;
    }
    
    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info) {
    }
    
    @Override
    public void getInfo(Assignment<Lecture, Placement> assignment, Map<String, String> info, Collection<Lecture> variables) {
    }

}
